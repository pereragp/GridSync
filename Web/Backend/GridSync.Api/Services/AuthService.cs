// -------------------------------------------------------------
// File: AuthService.cs
// Project: GridSync.Api
// Description: Authentication business logic (FAT service layer).
// -------------------------------------------------------------

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class AuthService
{
    private readonly MongoDbContext _db;
    private readonly JwtSettings _jwt;
    private readonly EmailSettings _email;
    private readonly IEmailService _emailService;

    public AuthService(
        MongoDbContext db,
        IOptions<JwtSettings> jwtOptions,
        IOptions<EmailSettings> emailOptions,
        IEmailService emailService)
    {
        // Inject MongoDB, JWT, and email services.
        _db = db;
        _jwt = jwtOptions.Value;
        _email = emailOptions.Value;
        _emailService = emailService;
    }

    /// <summary>
    /// Validates credentials and returns a signed JWT with role claims.
    /// </summary>
    public async Task<LoginResponse> LoginAsync(LoginRequest request)
    {
        // Normalize email and find matching user (emails stored lowercase).
        var email = request.Email.Trim().ToLowerInvariant();
        var user = await _db.Users
            .Find(u => u.Email == email)
            .FirstOrDefaultAsync();

        if (user is null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
        {
            throw new InvalidOperationException("Invalid email or password.");
        }

        // Block deactivated accounts from signing in.
        if (user.Status == UserStatus.Deactivated)
        {
            throw new InvalidOperationException("Account is deactivated. Contact Backoffice to reactivate.");
        }

        var expiresAt = DateTime.UtcNow.AddMinutes(_jwt.ExpiryMinutes);
        var token = CreateJwtToken(user, expiresAt);

        return new LoginResponse
        {
            Token = token,
            TokenType = "Bearer",
            ExpiresAt = expiresAt,
            UserId = user.Id,
            FullName = user.FullName,
            Email = user.Email,
            Role = user.Role,
            Status = user.Status,
            Nic = user.Nic
        };
    }

    /// <summary>
    /// Revokes the current JWT so it cannot be reused (server-side logout).
    /// </summary>
    public async Task LogoutAsync(ClaimsPrincipal principal)
    {
        // Read jti and expiry from the authenticated token.
        var jti = principal.FindFirstValue(JwtRegisteredClaimNames.Jti)
            ?? principal.FindFirstValue("jti");
        var userId = principal.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? principal.FindFirstValue(JwtRegisteredClaimNames.Sub);

        if (string.IsNullOrWhiteSpace(jti) || string.IsNullOrWhiteSpace(userId))
        {
            throw new InvalidOperationException("Invalid token for logout.");
        }

        var expClaim = principal.FindFirstValue(JwtRegisteredClaimNames.Exp);
        var expiresAt = DateTime.UtcNow.AddMinutes(_jwt.ExpiryMinutes);
        if (long.TryParse(expClaim, out var expSeconds))
        {
            expiresAt = DateTimeOffset.FromUnixTimeSeconds(expSeconds).UtcDateTime;
        }

        // Skip if already revoked.
        var alreadyRevoked = await _db.RevokedTokens.Find(t => t.Jti == jti).AnyAsync();
        if (alreadyRevoked)
        {
            return;
        }

        await _db.RevokedTokens.InsertOneAsync(new RevokedToken
        {
            Jti = jti,
            UserId = userId,
            ExpiresAt = expiresAt,
            RevokedAt = DateTime.UtcNow
        });
    }

    /// <summary>
    /// Starts password reset, emails a reset link via Gmail when configured.
    /// </summary>
    public async Task<ForgotPasswordResponse> ForgotPasswordAsync(ForgotPasswordRequest request)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        var user = await _db.Users.Find(u => u.Email == email).FirstOrDefaultAsync();

        // Do not reveal whether the email exists in the message.
        if (user is null || user.Status == UserStatus.Deactivated)
        {
            return new ForgotPasswordResponse
            {
                Message = "If an account exists for that email, a password reset link has been sent."
            };
        }

        var resetToken = Convert.ToBase64String(RandomNumberGenerator.GetBytes(32));
        var expiresAt = DateTime.UtcNow.AddMinutes(30);

        user.PasswordResetTokenHash = BCrypt.Net.BCrypt.HashPassword(resetToken);
        user.PasswordResetExpiresAt = expiresAt;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);

        var resetLink =
            $"{_email.FrontendBaseUrl.TrimEnd('/')}/reset-password" +
            $"?email={Uri.EscapeDataString(email)}&token={Uri.EscapeDataString(resetToken)}";

        var response = new ForgotPasswordResponse
        {
            Message = "If an account exists for that email, a password reset link has been sent.",
            ExpiresAt = expiresAt
        };

        if (_emailService.IsConfigured)
        {
            var html = $"""
                <p>Hello {System.Net.WebUtility.HtmlEncode(user.FullName)},</p>
                <p>We received a request to reset your GridSync password.</p>
                <p><a href="{resetLink}">Click here to reset your password</a></p>
                <p>This link expires at {expiresAt:u} (UTC).</p>
                <p>If you did not request this, you can ignore this email.</p>
                """;

            await _emailService.SendAsync(email, "GridSync password reset", html);
            response.Message = "If an account exists for that email, a password reset link has been sent to your inbox.";
        }
        else
        {
            // Fallback for local/Swagger testing when Gmail is not configured.
            response.Message = "Email is not configured. Use the reset token below (development fallback).";
            response.ResetToken = resetToken;
        }

        return response;
    }

    /// <summary>
    /// Sets a new password using a valid reset token.
    /// </summary>
    public async Task ResetPasswordAsync(ResetPasswordRequest request)
    {
        PasswordRules.EnsureValid(request.NewPassword);

        var email = request.Email.Trim().ToLowerInvariant();
        var user = await _db.Users.Find(u => u.Email == email).FirstOrDefaultAsync();

        if (user is null
            || string.IsNullOrWhiteSpace(user.PasswordResetTokenHash)
            || user.PasswordResetExpiresAt is null
            || user.PasswordResetExpiresAt < DateTime.UtcNow
            || !BCrypt.Net.BCrypt.Verify(request.ResetToken, user.PasswordResetTokenHash))
        {
            throw new InvalidOperationException("Invalid or expired reset token.");
        }

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.PasswordResetTokenHash = null;
        user.PasswordResetExpiresAt = null;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
    }

    /// <summary>
    /// Changes password for the currently authenticated user.
    /// </summary>
    public async Task ChangePasswordAsync(string userId, ChangePasswordRequest request)
    {
        PasswordRules.EnsureValid(request.NewPassword);

        var user = await _db.Users.Find(u => u.Id == userId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("User not found.");

        if (!BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.PasswordHash))
        {
            throw new InvalidOperationException("Current password is incorrect.");
        }

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
    }

    /// <summary>
    /// Returns true when the JWT id was logged out / revoked.
    /// </summary>
    public async Task<bool> IsTokenRevokedAsync(string jti)
    {
        return await _db.RevokedTokens.Find(t => t.Jti == jti).AnyAsync();
    }

    /// <summary>
    /// Creates a signed JWT containing user id, email, role, status, NIC, and jti.
    /// </summary>
    private string CreateJwtToken(User user, DateTime expiresAt)
    {
        var jti = Guid.NewGuid().ToString("N");

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id),
            new(JwtRegisteredClaimNames.Email, user.Email),
            new(JwtRegisteredClaimNames.Jti, jti),
            new(ClaimTypes.NameIdentifier, user.Id),
            new(ClaimTypes.Email, user.Email),
            new(ClaimTypes.Name, user.FullName),
            new(ClaimTypes.Role, user.Role),
            new("status", user.Status)
        };

        if (!string.IsNullOrWhiteSpace(user.Nic))
        {
            claims.Add(new Claim("nic", user.Nic));
        }

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_jwt.Key));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _jwt.Issuer,
            audience: _jwt.Audience,
            claims: claims,
            expires: expiresAt,
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
