// -------------------------------------------------------------
// File: UpdateUserRequest.cs
// Project: GridSync.Api
// Description: DTO for updating profile / account fields.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class UpdateUserRequest
{
    public string FullName { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string? Address { get; set; }
}
