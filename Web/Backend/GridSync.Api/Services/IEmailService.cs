// -------------------------------------------------------------
// File: IEmailService.cs
// Project: GridSync.Api
// Description: Abstraction for sending emails from the API.
// -------------------------------------------------------------

namespace GridSync.Api.Services;

public interface IEmailService
{
    bool IsConfigured { get; }

    Task SendAsync(string toEmail, string subject, string htmlBody, CancellationToken cancellationToken = default);
}
