// -------------------------------------------------------------
// File: StationService.cs
// Project: GridSync.Api
// Description: Business logic for microgrid node management 
// -------------------------------------------------------------

using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class StationService
{
    private readonly MongoDbContext _db;

    // Constructor — ASP.NET automatically "injects" MongoDbContext here
    public StationService(MongoDbContext db)
    {
        // Store the injected db context so all methods can use it
        _db = db;
    }

    //------------------------------------------
    // CREATE new solar hub
    public async Task<StationResponse> CreateAsync(CreateStationRequest request, string createdByUserId)
    {
        // Validate that capacity values are positive numbers
        if (request.CapacityKw <= 0 || request.CapacityKwh <= 0)
            throw new InvalidOperationException("Capacity values must be greater than zero.");
        
        if (request.Latitude < -90 || request.Latitude > 90)
            throw new InvalidOperationException("Latitude must be between -90 and 90.");

        if (request.Longitude < -180 || request.Longitude > 180)
            throw new InvalidOperationException("Longitude must be between -180 and 180.");

        if (request.AvailableBatterySlots < 0)
            throw new InvalidOperationException("Battery slots cannot be negative.");

        // Auto-generate a unique station code
        var stationCode = await GenerateUniqueStationCodeAsync();

        // Build the station entity to store in Mongo
        var station = new SolarStation
        {
            StationCode = stationCode,
            Name = request.Name.Trim(),
            Description = request.Description?.Trim(),
            Location = new GeoLocation {
                Type = "Point",
                Coordinates = new[] { request.Longitude, request.Latitude }
            },
            CapacityKw = request.CapacityKw,
            CapacityKwh = request.CapacityKwh,
            AvailableBatterySlots = request.AvailableBatterySlots,
            Schedule = new StationSchedule{
                // Use provided values or fall back to sensible defaults
                OpenTime = request.OpenTime ?? "08:00",
                CloseTime = request.CloseTime ?? "18:00",
                WorkingDays = request.WorkingDays ?? new List<string> { "Mon", "Tue", "Wed", "Thu", "Fri" }
            },
            Status = StationStatus.Active,
            CreatedBy = createdByUserId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Save to MongoDB
        await _db.SolarStations.InsertOneAsync(station);

        // Map to response DTO and return
        return ToResponse(station);
    }

    //-----------------------------------------
    // GET ALL stations
    public async Task<List<StationResponse>> GetAllAsync()
    {
        var stations = await _db.SolarStations.Find(_ => true).ToListAsync();
        return stations.Select(ToResponse).ToList();
    }

    //------------------------------------------------------
    // GET BY ID — return one station or throw if not found
    public async Task<StationResponse> GetByIdAsync(string id)
    {
        var station = await FindRequiredAsync(id);
        return ToResponse(station);
    }

    //------------------------------------------------------
    // UPDATE station details
    public async Task<StationResponse> UpdateAsync(string id, UpdateStationRequest request)
    {
        // Load exsisting station details
        var station = await FindRequiredAsync(id);

        // Validations
        if (request.CapacityKw <= 0 || request.CapacityKwh <= 0)
            throw new InvalidOperationException("Capacity values must be greater than zero.");

        if (request.Latitude < -90 || request.Latitude > 90)
            throw new InvalidOperationException("Latitude must be between -90 and 90.");

        if (request.Longitude < -180 || request.Longitude > 180)
            throw new InvalidOperationException("Longitude must be between -180 and 180.");

        if (request.AvailableBatterySlots < 0)
            throw new InvalidOperationException("Battery slots cannot be negative.");

        // Apply changes to the loaded station
        station.Name = request.Name.Trim();
        station.Description = request.Description?.Trim();
        station.Location = new GeoLocation
        {
            Type = "Point",
            Coordinates = new[] { request.Longitude, request.Latitude }
        };
        station.CapacityKw = request.CapacityKw;
        station.CapacityKwh = request.CapacityKwh;
        station.AvailableBatterySlots = request.AvailableBatterySlots;
        station.UpdatedAt = DateTime.UtcNow;

        // ReplaceOneAsync replaces the whole document in MongoDB
        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);

        return ToResponse(station);
    }

    // -------------------------------------------------------------------------
    // UPDATE SCHEDULE — GridOperator updates schedule and battery slots only 
    public async Task<StationResponse> UpdateScheduleAsync(string id, UpdateStationScheduleRequest request)
    {
        var station = await FindRequiredAsync(id);

        // Validate time strings: parse "HH:mm" format
        if (!TimeOnly.TryParse(request.OpenTime, out var open))
            throw new InvalidOperationException("OpenTime must be in HH:mm format (e.g. 08:00).");

        if (!TimeOnly.TryParse(request.CloseTime, out var close))
            throw new InvalidOperationException("CloseTime must be in HH:mm format (e.g. 18:00).");

        if (open >= close)
            throw new InvalidOperationException("OpenTime must be earlier than CloseTime.");

        // Validate working days — only allow known abbreviations
        var validDays = new[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        var invalidDays = request.WorkingDays.Except(validDays).ToList();

        if (invalidDays.Count > 0)
            throw new InvalidOperationException($"Invalid day(s): {string.Join(", ", invalidDays)}. Use Mon, Tue, Wed, Thu, Fri, Sat, Sun.");

        if (request.AvailableBatterySlots < 0)
            throw new InvalidOperationException("Battery slots cannot be negative.");

        // Apply only schedule-related changes
        station.Schedule.OpenTime = request.OpenTime;
        station.Schedule.CloseTime = request.CloseTime;
        station.Schedule.WorkingDays = request.WorkingDays;
        station.AvailableBatterySlots = request.AvailableBatterySlots;
        station.UpdatedAt = DateTime.UtcNow;
        
        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);
        return ToResponse(station);
    }

    //------------------------------------------
    // DEACTIVATE a node (blocked if active reservations exist)
    public async Task<StationResponse> DeactivateAsync(string id)
    {
        var station = await FindRequiredAsync(id);

        if (station.Status == StationStatus.Inactive)
            throw new InvalidOperationException("Station is already inactive.");

        // Check for active reservations (Pending or Approved)
        // These are reservations that haven't been completed/cancelled yet
        var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
        var activeCount = await _db.EnergyReservations.CountDocumentsAsync(r => r.StationId == id && activeStatuses.Contains(r.Status));

        // If any active reservations exist, BLOCK the deactivation
        if (activeCount > 0)
            throw new InvalidOperationException(
                $"Cannot deactivate: {activeCount} active energy reservation(s) exist on this node. " +
                "Resolve all reservations first.");

        station.Status = StationStatus.Inactive;
        station.UpdatedAt = DateTime.UtcNow;
        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);

        return ToResponse(station);
    }

    //---------------
    // Helpers
    //---------------

    /// <summary>
    /// Loads a station from the database or throws if it doesn't exist.
    /// </summary>
    private async Task<SolarStation> FindRequiredAsync(string id)
    {
        var station = await _db.SolarStations.Find(s => s.Id == id).FirstOrDefaultAsync();

        // FirstOrDefaultAsync returns null if not found — we throw instead
        if( station is null)
            throw new KeyNotFoundException($"Station with id: '{id}' was not found.");

        return station;
    }

    /// <summary>
    /// Generates a unique code like "SGH-A1B2C3" for a new station.
    /// </summary>
    private async Task<string> GenerateUniqueStationCodeAsync()
    {
        string code;
        do
        {
            code = "SGH-" + Guid.NewGuid().ToString("N")[..6].ToUpper();
        }
        while (await _db.SolarStations.Find(s => s.StationCode == code).AnyAsync());

        return code;
    }

    /// <summary>
    /// Maps a SolarStation database entity to a StationResponse DTO.
    /// </summary>
    private static StationResponse ToResponse(SolarStation s) => new()
    {
        Id = s.Id,
        StationCode = s.StationCode,
        Name = s.Name,
        Description = s.Description,
        // GeoJSON: index 0 = longitude, index 1 = latitude
        Longitude = s.Location.Coordinates[0],
        Latitude = s.Location.Coordinates[1],
        CapacityKw = s.CapacityKw,
        CapacityKwh = s.CapacityKwh,
        AvailableBatterySlots = s.AvailableBatterySlots,
        Schedule = s.Schedule,
        Status = s.Status,
        CreatedBy = s.CreatedBy,
        CreatedAt = s.CreatedAt,
        UpdatedAt = s.UpdatedAt
    };

}


