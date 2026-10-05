// -------------------------------------------------------------
// File: IEmailService.cs
// Project: GridSync.Api
// Description: Abstraction for sending emails from the API.
// -------------------------------------------------------------

namespace GridSync.Api.Services;

public interface IEmailService
{
    /// <summary>True when SMTP credentials are present and email is enabled.</summary>
    bool IsConfigured { get; }

    /// <summary>Send an HTML email to the given address.</summary>
    Task SendAsync(string toEmail, string subject, string htmlBody, CancellationToken cancellationToken = default);
}
