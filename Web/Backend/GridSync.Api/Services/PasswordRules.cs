// -------------------------------------------------------------
// File: PasswordRules.cs
// Project: GridSync.Api
// Description: Shared password strength validation for auth flows.
// -------------------------------------------------------------

using System.Text.RegularExpressions;

namespace GridSync.Api.Services;

public static class PasswordRules
{
    /// <summary>
    /// Ensures password meets minimum security rules.
    /// </summary>
    public static void EnsureValid(string password)
    {
        // Enforce minimum length, letter, and digit rules.
        if (string.IsNullOrWhiteSpace(password) || password.Length < 8)
        {
            throw new InvalidOperationException("Password must be at least 8 characters long.");
        }

        if (!Regex.IsMatch(password, "[A-Za-z]"))
        {
            throw new InvalidOperationException("Password must contain at least one letter.");
        }

        if (!Regex.IsMatch(password, "[0-9]"))
        {
            throw new InvalidOperationException("Password must contain at least one number.");
        }
    }
}
