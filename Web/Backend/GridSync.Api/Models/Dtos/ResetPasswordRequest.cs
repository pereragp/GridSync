// -------------------------------------------------------------
// File: ResetPasswordRequest.cs
// Project: GridSync.Api
// Description: Request body to set a new password using reset token.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ResetPasswordRequest
{
    public string Email { get; set; } = string.Empty;
    public string ResetToken { get; set; } = string.Empty;
    public string NewPassword { get; set; } = string.Empty;
}
