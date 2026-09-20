// -------------------------------------------------------------
// File: RegisterProsumerRequest.cs
// Project: GridSync.Api
// Description: DTO for prosumer self-registration (NIC primary key).
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class RegisterProsumerRequest
{
    public string Nic { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string? Address { get; set; }
}
