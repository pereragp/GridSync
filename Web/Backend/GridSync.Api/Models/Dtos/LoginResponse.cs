// -------------------------------------------------------------
// File: LoginResponse.cs
// Project: GridSync.Api
// Description: DTO returned after successful authentication.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class LoginResponse
{
    public string Token { get; set; } = string.Empty;
    public string TokenType { get; set; } = "Bearer";
    public DateTime ExpiresAt { get; set; }
    public string UserId { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public string? Nic { get; set; }
}
