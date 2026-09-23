// -------------------------------------------------------------
// File: AuthService.cs
// Project: GridSync.Api
// Description: Authentication business logic (FAT service layer).
// -------------------------------------------------------------

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
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

    public AuthService(MongoDbContext db, IOptions<JwtSettings> jwtOptions)
    {
        // Inject MongoDB context and JWT settings.
        _db = db;
        _jwt = jwtOptions.Value;
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

        // Pending prosumers may log in; clients use Status/Role for routing.
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
    /// Creates a signed JWT containing user id, email, role, status, and NIC.
    /// </summary>
    private string CreateJwtToken(User user, DateTime expiresAt)
    {
        // Build claims used by [Authorize] and role checks.
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id),
            new(JwtRegisteredClaimNames.Email, user.Email),
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
