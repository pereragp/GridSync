using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

public class SolarStation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonElement("stationCode")]
    public string StationCode { get; set; } = string.Empty;

    [BsonElement("name")]
    public string Name { get; set; } = string.Empty;

    [BsonElement("description")]
    [BsonIgnoreIfNull]
    public string? Description { get; set; }

    [BsonElement("location")]
    public GeoLocation Location { get; set; } = new();

    [BsonElement("address")]
    [BsonIgnoreIfNull]
    public string? Address { get; set; }

    [BsonElement("capacityKw")]
    public double CapacityKw { get; set; }

    [BsonElement("capacityKwh")]
    public double CapacityKwh { get; set; }

    [BsonElement("availableBatterySlots")]
    public int AvailableBatterySlots { get; set; }

    [BsonElement("schedule")]
    public StationSchedule Schedule { get; set; } = new();

    /// <summary>Active | Inactive</summary>
    [BsonElement("status")]
    public string Status { get; set; } = StationStatus.Active;

    [BsonElement("createdBy")]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfNull]
    public string? CreatedBy { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public class GeoLocation
{
    /// <summary>GeoJSON type — always "Point" for maps.</summary>
    [BsonElement("type")]
    public string Type { get; set; } = "Point";

    /// <summary>[longitude, latitude] — GeoJSON order.</summary>
    [BsonElement("coordinates")]
    public double[] Coordinates { get; set; } = new double[2];
}

public class StationSchedule
{
    [BsonElement("openTime")]
    public string OpenTime { get; set; } = "08:00";

    [BsonElement("closeTime")]
    public string CloseTime { get; set; } = "18:00";

    [BsonElement("workingDays")]
    public List<string> WorkingDays { get; set; } = new()
    {
        "Mon", "Tue", "Wed", "Thu", "Fri"
    };
}

public static class StationStatus
{
    public const string Active = "Active";
    public const string Inactive = "Inactive";
}
