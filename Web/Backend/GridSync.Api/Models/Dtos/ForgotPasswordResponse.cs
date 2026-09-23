// -------------------------------------------------------------
// File: ForgotPasswordResponse.cs
// Project: GridSync.Api
// Description: Response after requesting password reset.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ForgotPasswordResponse
{
    public string Message { get; set; } = string.Empty;

    /// <summary>
    /// Returned for API/Swagger testing when no email service is configured.
    /// Clients in production should receive this via email instead.
    /// </summary>
    public string? ResetToken { get; set; }

    public DateTime? ExpiresAt { get; set; }
}
