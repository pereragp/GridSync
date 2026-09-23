// -------------------------------------------------------------
// File: ChangePasswordRequest.cs
// Project: GridSync.Api
// Description: Request body for logged-in users to change password.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ChangePasswordRequest
{
    public string CurrentPassword { get; set; } = string.Empty;
    public string NewPassword { get; set; } = string.Empty;
}
