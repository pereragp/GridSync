using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

public class User
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfDefault]
    public string Id { get; set; } = null!;

    /// <summary>Required for Prosumer; null/empty for staff. NIC is the business key.</summary>
    [BsonElement("nic")]
    [BsonIgnoreIfNull]
    public string? Nic { get; set; }

    [BsonElement("fullName")]
    public string FullName { get; set; } = string.Empty;

    [BsonElement("email")]
    public string Email { get; set; } = string.Empty;

    [BsonElement("phone")]
    public string Phone { get; set; } = string.Empty;

    [BsonElement("passwordHash")]
    public string PasswordHash { get; set; } = string.Empty;

    /// <summary>Backoffice | GridOperator | Prosumer</summary>
    [BsonElement("role")]
    public string Role { get; set; } = string.Empty;

    /// <summary>Pending | Active | Deactivated</summary>
    [BsonElement("status")]
    public string Status { get; set; } = UserStatus.Pending;

    [BsonElement("address")]
    [BsonIgnoreIfNull]
    public string? Address { get; set; }

    [BsonElement("deactivationRequestedAt")]
    [BsonIgnoreIfNull]
    public DateTime? DeactivationRequestedAt { get; set; }

    [BsonElement("deactivatedAt")]
    [BsonIgnoreIfNull]
    public DateTime? DeactivatedAt { get; set; }

    [BsonElement("reactivatedAt")]
    [BsonIgnoreIfNull]
    public DateTime? ReactivatedAt { get; set; }

    [BsonElement("reactivatedBy")]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfNull]
    public string? ReactivatedBy { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public static class UserRoles
{
    public const string Backoffice = "Backoffice";
    public const string GridOperator = "GridOperator";
    public const string Prosumer = "Prosumer";
}

public static class UserStatus
{
    public const string Pending = "Pending";
    public const string Active = "Active";
    public const string Deactivated = "Deactivated";
}
