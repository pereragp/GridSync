// -------------------------------------------------------------
// File: LoginRequest.cs
// Project: GridSync.Api
// Description: DTO for user login credentials.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class LoginRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}
