// -------------------------------------------------------------
// File: RevokedToken.cs
// Project: GridSync.Api
// Description: Stores JWT IDs (jti) that were logged out until expiry.
// -------------------------------------------------------------

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace GridSync.Api.Models;

public class RevokedToken
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    [BsonIgnoreIfDefault]
    public string Id { get; set; } = null!;

    [BsonElement("jti")]
    public string Jti { get; set; } = string.Empty;

    [BsonElement("userId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string UserId { get; set; } = string.Empty;

    [BsonElement("expiresAt")]
    public DateTime ExpiresAt { get; set; }

    [BsonElement("revokedAt")]
    public DateTime RevokedAt { get; set; } = DateTime.UtcNow;
}
