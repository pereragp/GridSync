// -------------------------------------------------------------
// File: GmailEmailService.cs
// Project: GridSync.Api
// Description: Sends email through Gmail SMTP using an App Password.
// -------------------------------------------------------------

using GridSync.Api.Data;
using MailKit.Net.Smtp;
using MailKit.Security;
using Microsoft.Extensions.Options;
using MimeKit;

namespace GridSync.Api.Services;

public class GmailEmailService : IEmailService
{
    private readonly EmailSettings _settings;
    private readonly ILogger<GmailEmailService> _logger;

    public GmailEmailService(IOptions<EmailSettings> options, ILogger<GmailEmailService> logger)
    {
        // Load Gmail SMTP settings from configuration.
        _settings = options.Value;
        _logger = logger;
    }

    /// <summary>True when Gmail SMTP settings are complete.</summary>
    public bool IsConfigured =>
        _settings.Enabled
        && !string.IsNullOrWhiteSpace(_settings.FromEmail)
        && !string.IsNullOrWhiteSpace(_settings.AppPassword);

    /// <summary>
    /// Sends an HTML email via smtp.gmail.com.
    /// </summary>
    public async Task SendAsync(string toEmail, string subject, string htmlBody, CancellationToken cancellationToken = default)
    {
        // Send HTML message via Gmail SMTP.
        if (!IsConfigured)
        {
            throw new InvalidOperationException("Gmail email is not configured. Set EmailSettings in .env.");
        }

        var message = new MimeMessage();
        message.From.Add(new MailboxAddress(_settings.FromName, _settings.FromEmail));
        message.To.Add(MailboxAddress.Parse(toEmail));
        message.Subject = subject;
        message.Body = new TextPart("html") { Text = htmlBody };

        using var client = new SmtpClient();
        await client.ConnectAsync(_settings.SmtpHost, _settings.SmtpPort, SecureSocketOptions.StartTls, cancellationToken);
        await client.AuthenticateAsync(_settings.FromEmail, _settings.AppPassword, cancellationToken);
        await client.SendAsync(message, cancellationToken);
        await client.DisconnectAsync(true, cancellationToken);

        _logger.LogInformation("Password-related email sent to {Email}", toEmail);
    }
}
