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

    public double CapacityKw { get; set; }
    public double CapacityKwh { get; set; }
    public int AvailableBatterySlots { get; set; }

    // The full schedule object — reuse the existing model class directly
    public StationSchedule Schedule { get; set; } = new();

    // "Active" or "Inactive"
    public string Status { get; set; } = string.Empty;

    public string? CreatedBy { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}