using System.ComponentModel.DataAnnotations;

namespace GridSync.Api.Models.Dtos;

public class UpdateReservationRequest
{
    [Required]
    public string ReservationType { get; set; } = ReservationTypes.Charging;
}
