// -------------------------------------------------------------
// File: BookingSlotResponse.cs
// Project: GridSync.Api
// Description: DTO returned for station battery slot details.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

/// <summary>Battery slot returned to staff clients.</summary>
public class BookingSlotResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string StationName { get; set; } = string.Empty;
    public int BatteryIndex { get; set; }
    public double CapacityKwh { get; set; }
    public double ActualEnergyKwh { get; set; }
    public double ReservedChargingKwh { get; set; }
    public double ReservedDropOffKwh { get; set; }
    public double AvailableChargingKwh { get; set; }
    public double AvailableDropOffKwh { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public string? CreatedBy { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
