// -------------------------------------------------------------
// File: ReservationCompletionResponse.cs
// Project: GridSync.Api
// Description: Response after operator completes a reservation transfer.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ReservationCompletionResponse
{
    public string ReservationId { get; set; } = string.Empty;
    public string ReservationCode { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public DateTime CompletedAt { get; set; }
    public string CompletedBy { get; set; } = string.Empty;
}
