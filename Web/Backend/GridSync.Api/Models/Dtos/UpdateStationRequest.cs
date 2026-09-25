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
    public string? Address { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double CapacityKw { get; set; }
    public double CapacityKwh { get; set; }
    public int AvailableBatterySlots { get; set; }
}