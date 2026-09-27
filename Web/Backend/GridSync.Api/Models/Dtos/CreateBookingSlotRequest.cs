namespace GridSync.Api.Models.Dtos;

/// <summary>Request body for creating an energy booking slot.</summary>
public class CreateBookingSlotRequest
{
    public string StationId { get; set; } = string.Empty;
    public DateTime SlotStart { get; set; }
    public DateTime SlotEnd { get; set; }
    public double EnergyKwh { get; set; }
    public int MaxReservations { get; set; } = 1;
    public string? Notes { get; set; }
}
