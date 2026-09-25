using System.ComponentModel.DataAnnotations;

namespace GridSync.Api.Models.Dtos;

public class CreateReservationRequest
{
    [Required]
    public string SlotId { get; set; } = string.Empty;

    public string ReservationType { get; set; } = ReservationTypes.Charging;
}
