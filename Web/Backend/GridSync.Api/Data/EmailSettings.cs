// -------------------------------------------------------------
// File: EmailSettings.cs
// Project: GridSync.Api
// Description: Gmail SMTP settings for outbound email (password reset).
// -------------------------------------------------------------

namespace GridSync.Api.Data;

public class EmailSettings
{
    public bool Enabled { get; set; }

    public string SmtpHost { get; set; } = "smtp.gmail.com";

    public int SmtpPort { get; set; } = 587;

    public string FromEmail { get; set; } = string.Empty;

    public string FromName { get; set; } = "GridSync";

    /// <summary>Gmail App Password (not the normal Gmail login password).</summary>
    public string AppPassword { get; set; } = string.Empty;

    /// <summary>Frontend origin used to build reset links, e.g. http://localhost:5173</summary>
    public string FrontendBaseUrl { get; set; } = "http://localhost:5173";
}
