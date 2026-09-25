// -------------------------------------------------------------
// File: UpdateStationScheduleRequest.cs
// Project: GridSync.Api
// Description: DTO for updating a station's operating schedule and battery slots.
// -------------------------------------------------------------

namespace GridSync.Api.Models.Dtos;

public class UpdateStationScheduleRequest
{
    // Time strings in "HH:mm" format, e.g. "08:00", "18:30"
    public string OpenTime { get; set; } = "08:00";
    public string CloseTime { get; set; } = "18:00";

    // Day abbreviations: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    public List<string> WorkingDays { get; set; } = new();

    // Grid Operators can update available battery slots from here
    public int AvailableBatterySlots { get; set; }
}