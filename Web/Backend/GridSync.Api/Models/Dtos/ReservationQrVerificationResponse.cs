namespace GridSync.Api.Models.Dtos;

public class ReservationQrVerificationResponse
{
    public string ReservationId { get; set; } = string.Empty;
    public string ReservationCode { get; set; } = string.Empty;
    public string ProsumerNic { get; set; } = string.Empty;
    public string? StationName { get; set; }
    public DateTime SlotStart { get; set; }
    public DateTime SlotEnd { get; set; }
    public string ReservationType { get; set; } = string.Empty;
    public double EnergyKwh { get; set; }
    public string Status { get; set; } = string.Empty;
}
