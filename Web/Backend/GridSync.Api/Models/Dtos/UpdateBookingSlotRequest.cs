// -------------------------------------------------------------
// File: UpdateBookingSlotRequest.cs
// Project: GridSync.Api
// Description: Request body to update battery notes or metadata.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

/// <summary>Optional notes when closing or updating battery metadata.</summary>
public class UpdateBookingSlotRequest
{
    public string? Notes { get; set; }
}
