using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

/// <summary>
/// One physical battery at a solar station.
/// Old time-window fields are ignored so legacy Mongo documents still load.
/// </summary>
[BsonIgnoreExtraElements]
public class EnergyBookingSlot
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    /// <summary>1-based battery number within the station.</summary>
    [BsonElement("batteryIndex")]
    public int BatteryIndex { get; set; }

    /// <summary>Max energy this battery can hold (kWh).</summary>
    [BsonElement("capacityKwh")]
    public double CapacityKwh { get; set; }

    /// <summary>Energy physically stored after completed transfers (kWh).</summary>
    [BsonElement("actualEnergyKwh")]
    public double ActualEnergyKwh { get; set; }

    /// <summary>kWh soft-locked by Pending/Approved Charging reservations.</summary>
    [BsonElement("reservedChargingKwh")]
    public double ReservedChargingKwh { get; set; }

    /// <summary>kWh soft-locked by Pending/Approved DropOff reservations (free space).</summary>
    [BsonElement("reservedDropOffKwh")]
    public double ReservedDropOffKwh { get; set; }

    /// <summary>Available | Closed</summary>
    [BsonElement("status")]
    public string Status { get; set; } = SlotStatus.Available;

    [BsonElement("notes")]
    [BsonIgnoreIfNull]
    public string? Notes { get; set; }

    [BsonElement("createdBy")]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfNull]
    public string? CreatedBy { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public static class SlotStatus
{
    public const string Available = "Available";
    public const string Closed = "Closed";
}
