using System.ComponentModel.DataAnnotations;

namespace GridSync.Api.Models.Dtos;

public class VerifyReservationQrRequest
{
    [Required]
    public string QrPayload { get; set; } = string.Empty;
}
