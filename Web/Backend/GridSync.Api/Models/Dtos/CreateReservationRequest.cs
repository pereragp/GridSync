using System.ComponentModel.DataAnnotations;
using GridSync.Api.Models;

namespace GridSync.Api.Models.Dtos;

public class CreateReservationRequest
{
    [Required]
    public string SlotId { get; set; } = string.Empty;

    /// <summary>Charging | DropOff</summary>
    public string ReservationType { get; set; } = ReservationTypes.Charging;

    /// <summary>Requested energy amount in kWh.</summary>
    [Range(0.01, double.MaxValue)]
    public double EnergyKwh { get; set; }

    /// <summary>Visit window start (UTC).</summary>
    public DateTime SlotStart { get; set; }

    /// <summary>Visit window end (UTC).</summary>
    public DateTime SlotEnd { get; set; }
}
