// -------------------------------------------------------------
// File: UserService.cs
// Project: GridSync.Api
// Description: User/prosumer account business logic (FAT service).
// -------------------------------------------------------------

using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class UserService
{
    private readonly MongoDbContext _db;

    public UserService(MongoDbContext db)
    {
        // Inject MongoDB context for Users collection access.
        _db = db;
    }

    /// <summary>
    /// Creates a Backoffice or Grid Operator staff account (Active immediately).
    /// </summary>
    public async Task<UserResponse> CreateStaffUserAsync(CreateStaffUserRequest request)
    {
        // Only allow staff roles here.
        if (request.Role is not (UserRoles.Backoffice or UserRoles.GridOperator))
        {
            throw new InvalidOperationException("Role must be Backoffice or GridOperator.");
        }

        await EnsureEmailUniqueAsync(request.Email);

        PasswordRules.EnsureValid(request.Password);

        var user = new User
        {
            FullName = request.FullName.Trim(),
            Email = request.Email.Trim().ToLowerInvariant(),
            Phone = request.Phone.Trim(),
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            Role = request.Role,
            Status = UserStatus.Active,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _db.Users.InsertOneAsync(user);
        return ToResponse(user);
    }

    /// <summary>
    /// Registers a solar prosumer using NIC as the business primary key (Pending until approved).
    /// </summary>
    public async Task<UserResponse> RegisterProsumerAsync(RegisterProsumerRequest request)
    {
        // Validate NIC presence.
        if (string.IsNullOrWhiteSpace(request.Nic))
        {
            throw new InvalidOperationException("NIC is required for prosumer registration.");
        }

        var nic = request.Nic.Trim();
        await EnsureEmailUniqueAsync(request.Email);
        await EnsureNicUniqueAsync(nic);
        PasswordRules.EnsureValid(request.Password);

        var user = new User
        {
            Nic = nic,
            FullName = request.FullName.Trim(),
            Email = request.Email.Trim().ToLowerInvariant(),
            Phone = request.Phone.Trim(),
            Address = request.Address?.Trim(),
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            Role = UserRoles.Prosumer,
            Status = UserStatus.Pending,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _db.Users.InsertOneAsync(user);
        return ToResponse(user);
    }

    /// <summary>
    /// Returns all users (for Backoffice administration).
    /// </summary>
    public async Task<List<UserResponse>> GetAllAsync()
    {
        // Return all users for administration.
        var users = await _db.Users.Find(_ => true).ToListAsync();
        return users.Select(ToResponse).ToList();
    }

    /// <summary>
    /// Returns a single user by MongoDB id.
    /// </summary>
    public async Task<UserResponse> GetByIdAsync(string id)
    {
        // Load one user by MongoDB id.
        var user = await FindRequiredAsync(id);
        return ToResponse(user);
    }

    /// <summary>
    /// Lists prosumer accounts waiting for Backoffice activation.
    /// </summary>
    public async Task<List<UserResponse>> GetPendingAsync()
    {
        // List prosumers awaiting Backoffice approval.
        var users = await _db.Users
            .Find(u => u.Role == UserRoles.Prosumer && u.Status == UserStatus.Pending)
            .ToListAsync();
        return users.Select(ToResponse).ToList();
    }

    /// <summary>
    /// Approves a pending prosumer (Pending -> Active).
    /// </summary>
    public async Task<UserResponse> ApprovePendingAsync(string id)
    {
        // Activate a pending prosumer account.
        var user = await FindRequiredAsync(id);

        if (user.Role != UserRoles.Prosumer || user.Status != UserStatus.Pending)
        {
            throw new InvalidOperationException("Only pending prosumer accounts can be approved.");
        }

        user.Status = UserStatus.Active;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
        return ToResponse(user);
    }

    /// <summary>
    /// Updates basic profile fields for a user.
    /// </summary>
    public async Task<UserResponse> UpdateAsync(string id, UpdateUserRequest request)
    {
        // Update profile fields on an existing user.
        var user = await FindRequiredAsync(id);

        user.FullName = request.FullName.Trim();
        user.Phone = request.Phone.Trim();
        user.Address = request.Address?.Trim();
        user.UpdatedAt = DateTime.UtcNow;

        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
        return ToResponse(user);
    }

    /// <summary>
    /// Prosumer requests deactivation (does not fully deactivate until processed).
    /// </summary>
    public async Task<UserResponse> RequestDeactivationAsync(string id)
    {
        // Record prosumer deactivation request timestamp.
        var user = await FindRequiredAsync(id);

        if (user.Role != UserRoles.Prosumer)
        {
            throw new InvalidOperationException("Only prosumers can request deactivation this way.");
        }

        if (user.Status == UserStatus.Deactivated)
        {
            throw new InvalidOperationException("Account is already deactivated.");
        }

        user.DeactivationRequestedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
        return ToResponse(user);
    }

    /// <summary>
    /// Backoffice deactivates an account.
    /// </summary>
    public async Task<UserResponse> DeactivateAsync(string id)
    {
        // Backoffice sets account status to deactivated.
        var user = await FindRequiredAsync(id);

        if (user.Status == UserStatus.Deactivated)
        {
            throw new InvalidOperationException("Account is already deactivated.");
        }

        user.Status = UserStatus.Deactivated;
        user.DeactivatedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
        return ToResponse(user);
    }

    /// <summary>
    /// Backoffice-only reactivation of a deactivated account.
    /// </summary>
    public async Task<UserResponse> ReactivateAsync(string id, string backofficeUserId)
    {
        // Restore deactivated account and record auditor id.
        var user = await FindRequiredAsync(id);

        if (user.Status != UserStatus.Deactivated)
        {
            throw new InvalidOperationException("Only deactivated accounts can be reactivated.");
        }

        user.Status = UserStatus.Active;
        user.ReactivatedAt = DateTime.UtcNow;
        user.ReactivatedBy = backofficeUserId;
        user.DeactivationRequestedAt = null;
        user.UpdatedAt = DateTime.UtcNow;

        await _db.Users.ReplaceOneAsync(u => u.Id == user.Id, user);
        return ToResponse(user);
    }

    /// <summary>
    /// Loads a user or throws if missing.
    /// </summary>
    private async Task<User> FindRequiredAsync(string id)
    {
        // Load user or throw when missing.
        var user = await _db.Users.Find(u => u.Id == id).FirstOrDefaultAsync();
        if (user is null)
        {
            throw new KeyNotFoundException("User not found.");
        }
        return user;
    }

    /// <summary>
    /// Ensures email is not already registered.
    /// </summary>
    private async Task EnsureEmailUniqueAsync(string email)
    {
        // Reject duplicate normalized email addresses.
        var normalized = email.Trim().ToLowerInvariant();
        var exists = await _db.Users.Find(u => u.Email == normalized).AnyAsync();
        if (exists)
        {
            throw new InvalidOperationException("Email is already registered.");
        }
    }

    /// <summary>
    /// Ensures NIC is not already registered.
    /// </summary>
    private async Task EnsureNicUniqueAsync(string nic)
    {
        // Reject duplicate prosumer NIC values.
        var exists = await _db.Users.Find(u => u.Nic == nic).AnyAsync();
        if (exists)
        {
            throw new InvalidOperationException("NIC is already registered.");
        }
    }

    /// <summary>
    /// Maps entity to safe response DTO.
    /// </summary>
    private static UserResponse ToResponse(User user) => new()
    {
        Id = user.Id,
        Nic = user.Nic,
        FullName = user.FullName,
        Email = user.Email,
        Phone = user.Phone,
        Role = user.Role,
        Status = user.Status,
        Address = user.Address,
        DeactivationRequestedAt = user.DeactivationRequestedAt,
        CreatedAt = user.CreatedAt,
        UpdatedAt = user.UpdatedAt
    };
}
