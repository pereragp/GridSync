using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

public class EnergyBookingSlot
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("slotStart")]
    public DateTime SlotStart { get; set; }

    [BsonElement("slotEnd")]
    public DateTime SlotEnd { get; set; }

    [BsonElement("energyKwh")]
    public double EnergyKwh { get; set; }

    [BsonElement("maxReservations")]
    public int MaxReservations { get; set; } = 1;

    [BsonElement("reservedCount")]
    public int ReservedCount { get; set; }

    /// <summary>Available | FullyBooked | Closed</summary>
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
    public const string FullyBooked = "FullyBooked";
    public const string Closed = "Closed";
}
