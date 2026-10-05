// -------------------------------------------------------------
// File: CancelReservationRequest.cs
// Project: GridSync.Api
// Description: Request body when a prosumer cancels a reservation.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class CancelReservationRequest
{
    public string? Reason { get; set; }
}
