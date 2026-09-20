// -------------------------------------------------------------
// File: AuthService.cs
// Project: GridSync.Api
// Description: Authentication business logic (FAT service layer).
// -------------------------------------------------------------

using System.Text;
using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class AuthService
{
    private readonly MongoDbContext _db;

    public AuthService(MongoDbContext db)
    {
        // Inject MongoDB context for user lookup.
        _db = db;
    }

    /// <summary>
    /// Validates credentials and returns role-aware login payload.
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

        // Pending prosumers may log in with limited access (clients can route accordingly).
        return new LoginResponse
        {
            Token = CreateSimpleToken(user),
            UserId = user.Id,
            FullName = user.FullName,
            Email = user.Email,
            Role = user.Role,
            Status = user.Status,
            Nic = user.Nic
        };
    }

    /// <summary>
    /// Builds a lightweight opaque token for clients (replace with JWT later if needed).
    /// </summary>
    private static string CreateSimpleToken(User user)
    {
        // Encode user id + role + timestamp as a simple bearer token.
        var raw = $"{user.Id}:{user.Role}:{DateTime.UtcNow.Ticks}";
        return Convert.ToBase64String(Encoding.UTF8.GetBytes(raw));
    }
}
