// -------------------------------------------------------------
// File: ForgotPasswordRequest.cs
// Project: GridSync.Api
// Description: Request body to start password reset.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ForgotPasswordRequest
{
    public string Email { get; set; } = string.Empty;
}
