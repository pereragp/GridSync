namespace GridSync.Api.Models.Dtos;

/// <summary>Request body for updating an energy booking slot.</summary>
public class UpdateBookingSlotRequest
{
    public DateTime SlotStart { get; set; }
    public DateTime SlotEnd { get; set; }
    public double EnergyKwh { get; set; }
    public int MaxReservations { get; set; }
    public string? Notes { get; set; }
}
