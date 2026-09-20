// -------------------------------------------------------------
// File: CreateStaffUserRequest.cs
// Project: GridSync.Api
// Description: DTO to create Backoffice or GridOperator users.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class CreateStaffUserRequest
{
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;

    /// <summary>Backoffice or GridOperator only.</summary>
    public string Role { get; set; } = string.Empty;
}
