// -------------------------------------------------------------
// File: EnergyReservation.cs
// Project: GridSync.Api
// Description: Mongo model for a Charging or DropOff energy reservation.
// -------------------------------------------------------------

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonElement("reservationCode")]
    public string ReservationCode { get; set; } = string.Empty;

    [BsonElement("prosumerId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string ProsumerId { get; set; } = string.Empty;

    [BsonElement("prosumerNic")]
    public string ProsumerNic { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("slotId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string SlotId { get; set; } = string.Empty;

    [BsonElement("stationName")]
    [BsonIgnoreIfNull]
    public string? StationName { get; set; }

    [BsonElement("slotStart")]
    public DateTime SlotStart { get; set; }

    [BsonElement("slotEnd")]
    public DateTime SlotEnd { get; set; }

    /// <summary>DropOff | Charging</summary>
    [BsonElement("reservationType")]
    public string ReservationType { get; set; } = ReservationTypes.Charging;

    [BsonElement("energyKwh")]
    public double EnergyKwh { get; set; }

    /// <summary>Pending | Approved | Rejected | Cancelled | Completed | Expired</summary>
    [BsonElement("status")]
    public string Status { get; set; } = ReservationStatus.Pending;

    [BsonElement("qrPayload")]
    [BsonIgnoreIfNull]
    public string? QrPayload { get; set; }

    [BsonElement("qrGeneratedAt")]
    [BsonIgnoreIfNull]
    public DateTime? QrGeneratedAt { get; set; }

    [BsonElement("completedAt")]
    [BsonIgnoreIfNull]
    public DateTime? CompletedAt { get; set; }

    [BsonElement("completedBy")]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfNull]
    public string? CompletedBy { get; set; }

    [BsonElement("cancellationReason")]
    [BsonIgnoreIfNull]
    public string? CancellationReason { get; set; }

    [BsonElement("cancelledAt")]
    [BsonIgnoreIfNull]
    public DateTime? CancelledAt { get; set; }

    [BsonElement("rejectionReason")]
    [BsonIgnoreIfNull]
    public string? RejectionReason { get; set; }

    [BsonElement("rejectedAt")]
    [BsonIgnoreIfNull]
    public DateTime? RejectedAt { get; set; }

    [BsonElement("rejectedBy")]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfNull]
    public string? RejectedBy { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public static class ReservationStatus
{
    public const string Pending = "Pending";
    public const string Approved = "Approved";
    public const string Rejected = "Rejected";
    public const string Cancelled = "Cancelled";
    public const string Completed = "Completed";
    public const string Expired = "Expired";
}

public static class ReservationTypes
{
    public const string DropOff = "DropOff";
    public const string Charging = "Charging";
}
