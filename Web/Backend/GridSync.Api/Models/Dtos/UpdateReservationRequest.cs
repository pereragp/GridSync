// -------------------------------------------------------------
// File: UpdateReservationRequest.cs
// Project: GridSync.Api
// Description: Request body for prosumer to update a pending reservation.
// -------------------------------------------------------------

using System.ComponentModel.DataAnnotations;
using GridSync.Api.Models;

namespace GridSync.Api.Models.Dtos;

public class UpdateReservationRequest
{
    [Required]
    public string ReservationType { get; set; } = ReservationTypes.Charging;

    [Range(0.01, double.MaxValue)]
    public double EnergyKwh { get; set; }

    public DateTime SlotStart { get; set; }

    public DateTime SlotEnd { get; set; }
}
