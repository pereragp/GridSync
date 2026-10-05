// -------------------------------------------------------------
// File: VerifyReservationQrRequest.cs
// Project: GridSync.Api
// Description: Request body containing a reservation QR payload.
// -------------------------------------------------------------

using System.ComponentModel.DataAnnotations;

namespace GridSync.Api.Models.Dtos;

public class VerifyReservationQrRequest
{
    [Required]
    public string QrPayload { get; set; } = string.Empty;
}
