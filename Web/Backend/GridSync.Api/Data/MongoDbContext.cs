// -------------------------------------------------------------
// File: MongoDbContext.cs
// Project: GridSync.Api
// Description: MongoDB database and collection accessors.
// -------------------------------------------------------------

using GridSync.Api.Models;
using Microsoft.Extensions.Options;
using MongoDB.Driver;

namespace GridSync.Api.Data;

public class MongoDbContext
{
    private readonly IMongoDatabase _database;

    /// <summary>Connect using configured connection string and database name.</summary>
    public MongoDbContext(IOptions<MongoDbSettings> settings)
    {
        // Open MongoDB client and bind database.
        var client = new MongoClient(settings.Value.ConnectionString);
        _database = client.GetDatabase(settings.Value.DatabaseName);
    }

    public IMongoDatabase Database => _database;

    public IMongoCollection<User> Users =>
        _database.GetCollection<User>("Users");

    public IMongoCollection<SolarStation> SolarStations =>
        _database.GetCollection<SolarStation>("SolarStationInfo");

    public IMongoCollection<EnergyBookingSlot> EnergyBookingSlots =>
        _database.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");

    public IMongoCollection<EnergyReservation> EnergyReservations =>
        _database.GetCollection<EnergyReservation>("EnergyReservations");

    public IMongoCollection<RevokedToken> RevokedTokens =>
        _database.GetCollection<RevokedToken>("RevokedTokens");

    /// <summary>Generic accessor for an arbitrary collection name.</summary>
    public IMongoCollection<T> GetCollection<T>(string name) =>
        _database.GetCollection<T>(name);
}
