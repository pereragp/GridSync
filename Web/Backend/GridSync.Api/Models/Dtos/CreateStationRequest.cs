// -------------------------------------------------------------
// File: CreateStationRequest.cs
// Project: GridSync.Api
// Description: DTO for creating a new solar microgrid station.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class CreateStationRequest
{
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    /// <summary>Energy capacity of one battery (kWh).</summary>
    public double BatteryCapacityKwh { get; set; }
    public int AvailableBatterySlots { get; set; }
    public string? OpenTime { get; set; }
    public string? CloseTime { get; set; }
    public List<string>? WorkingDays { get; set; }
}
