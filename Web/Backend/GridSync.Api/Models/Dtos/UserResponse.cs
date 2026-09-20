// -------------------------------------------------------------
// File: UserResponse.cs
// Project: GridSync.Api
// Description: Safe user payload for API responses (no password).
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class UserResponse
{
    public string Id { get; set; } = string.Empty;
    public string? Nic { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public string? Address { get; set; }
    public DateTime? DeactivationRequestedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
