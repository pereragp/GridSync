// -------------------------------------------------------------
// File: UpdateStationRequest.cs
// Project: GridSync.Api
// Description: DTO for updating core details of a solar station.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class UpdateStationRequest
{
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    /// <summary>Energy capacity of one battery (kWh).</summary>
    public double BatteryCapacityKwh { get; set; }
    public int AvailableBatterySlots { get; set; }
}
