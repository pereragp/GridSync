// -------------------------------------------------------------
// File: ReviewReservationRequest.cs
// Project: GridSync.Api
// Description: Request body when staff approve or reject a reservation.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class ReviewReservationRequest
{
    public string? Reason { get; set; }
}
