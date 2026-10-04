namespace GridSync.Api.Models.Dtos;

/// <summary>Battery shown to prosumers for Charging / DropOff booking.</summary>
public class AvailableBookingSlotResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string StationName { get; set; } = string.Empty;
    public int BatteryIndex { get; set; }
    public double CapacityKwh { get; set; }
    public double ActualEnergyKwh { get; set; }
    public double AvailableChargingKwh { get; set; }
    public double AvailableDropOffKwh { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? Notes { get; set; }

    /// <summary>Station open time (HH:mm, station local / Asia/Colombo).</summary>
    public string OpenTime { get; set; } = "08:00";

    /// <summary>Station close time (HH:mm, station local / Asia/Colombo).</summary>
    public string CloseTime { get; set; } = "18:00";

    /// <summary>Working day abbreviations (Mon…Sun).</summary>
    public List<string> WorkingDays { get; set; } = [];
}
