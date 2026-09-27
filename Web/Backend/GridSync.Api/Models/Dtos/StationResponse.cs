// -------------------------------------------------------------
// File: StationResponse.cs
// Project: GridSync.Api
// Description: DTO returned to clients for solar station data.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class StationResponse
{
    public string Id { get; set; } = string.Empty;

    // Auto-generated unique code
    public string StationCode { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }

    public double Latitude { get; set; }
    public double Longitude { get; set; }

    /// <summary>Energy capacity of one battery (kWh).</summary>
    public double BatteryCapacityKwh { get; set; }

    public int AvailableBatterySlots { get; set; }

    /// <summary>Total station storage = slots × batteryCapacityKwh.</summary>
    public double TotalCapacityKwh { get; set; }

    // The full schedule object — reuse the existing model class directly
    public StationSchedule Schedule { get; set; } = new();

    // "Active" or "Inactive"
    public string Status { get; set; } = string.Empty;

    public string? CreatedBy { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
