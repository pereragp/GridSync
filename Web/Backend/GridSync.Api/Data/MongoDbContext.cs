using GridSync.Api.Models;
using Microsoft.Extensions.Options;
using MongoDB.Driver;

namespace GridSync.Api.Data;

public class MongoDbContext
{
    private readonly IMongoDatabase _database;

    public MongoDbContext(IOptions<MongoDbSettings> settings)
    {
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

    public IMongoCollection<T> GetCollection<T>(string name) =>
        _database.GetCollection<T>(name);
}
