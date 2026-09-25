namespace GridSync.Api.Models.Dtos;

public class AvailableBookingSlotResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string StationName { get; set; } = string.Empty;
    public DateTime SlotStart { get; set; }
    public DateTime SlotEnd { get; set; }
    public double EnergyKwh { get; set; }
    public int AvailableReservations { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? Notes { get; set; }
}
