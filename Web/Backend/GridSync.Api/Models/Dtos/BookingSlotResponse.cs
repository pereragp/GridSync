namespace GridSync.Api.Models.Dtos;

/// <summary>Booking slot returned to clients.</summary>
public class BookingSlotResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string StationName { get; set; } = string.Empty;
    public DateTime SlotStart { get; set; }
    public DateTime SlotEnd { get; set; }
    public double EnergyKwh { get; set; }
    public int MaxReservations { get; set; }
    public int ReservedCount { get; set; }
    public int AvailableReservations { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public string? CreatedBy { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
