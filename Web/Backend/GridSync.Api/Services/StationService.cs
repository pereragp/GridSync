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
        // Validate that per-battery capacity is positive and slots are non-negative
        if (request.BatteryCapacityKwh <= 0)
            throw new InvalidOperationException("Battery capacity (kWh) must be greater than zero.");

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
            BatteryCapacityKwh = request.BatteryCapacityKwh,
            AvailableBatterySlots = request.AvailableBatterySlots,
            TotalCapacityKwh = ComputeTotalCapacityKwh(request.AvailableBatterySlots, request.BatteryCapacityKwh),
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

        // One EnergyBookingSlot document per physical battery
        await CreateBatteriesAsync(station, createdByUserId);

        return ToResponse(station);
    }

    //-----------------------------------------
    // GET ALL stations
    public async Task<List<StationResponse>> GetAllAsync()
    {
        // Return every solar station as a response DTO.
        var stations = await _db.SolarStations.Find(_ => true).ToListAsync();
        return stations.Select(ToResponse).ToList();
    }

    //------------------------------------------------------
    // GET BY ID — return one station or throw if not found
    public async Task<StationResponse> GetByIdAsync(string id)
    {
        // Load one station by id.
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
        if (request.BatteryCapacityKwh <= 0)
            throw new InvalidOperationException("Battery capacity (kWh) must be greater than zero.");

        if (request.Latitude < -90 || request.Latitude > 90)
            throw new InvalidOperationException("Latitude must be between -90 and 90.");

        if (request.Longitude < -180 || request.Longitude > 180)
            throw new InvalidOperationException("Longitude must be between -180 and 180.");

        if (request.AvailableBatterySlots < 0)
            throw new InvalidOperationException("Battery slots cannot be negative.");

        var previousBatteryCount = station.AvailableBatterySlots;
        var previousCapacity = station.BatteryCapacityKwh;

        // Apply changes to the loaded station
        station.Name = request.Name.Trim();
        station.Description = request.Description?.Trim();
        station.Location = new GeoLocation
        {
            Type = "Point",
            Coordinates = new[] { request.Longitude, request.Latitude }
        };
        station.BatteryCapacityKwh = request.BatteryCapacityKwh;
        station.AvailableBatterySlots = request.AvailableBatterySlots;
        station.TotalCapacityKwh = ComputeTotalCapacityKwh(request.AvailableBatterySlots, request.BatteryCapacityKwh);
        station.UpdatedAt = DateTime.UtcNow;

        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);

        await SyncBatteriesAsync(station, previousBatteryCount, previousCapacity, station.CreatedBy);

        return ToResponse(station);
    }

    // -------------------------------------------------------------------------
    // UPDATE SCHEDULE — GridOperator updates schedule and battery slots only 
    public async Task<StationResponse> UpdateScheduleAsync(string id, UpdateStationScheduleRequest request)
    {
        // Update open/close hours, working days, and slot count.
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

        var previousBatteryCount = station.AvailableBatterySlots;

        // Apply only schedule-related changes; recalculate total when slots change
        station.Schedule.OpenTime = request.OpenTime;
        station.Schedule.CloseTime = request.CloseTime;
        station.Schedule.WorkingDays = request.WorkingDays;
        station.AvailableBatterySlots = request.AvailableBatterySlots;
        station.TotalCapacityKwh = ComputeTotalCapacityKwh(request.AvailableBatterySlots, station.BatteryCapacityKwh);
        station.UpdatedAt = DateTime.UtcNow;
        
        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);

        await SyncBatteriesAsync(station, previousBatteryCount, station.BatteryCapacityKwh, station.CreatedBy);

        return ToResponse(station);
    }

    /// <summary>Returns active stations within radiusKm of the given coordinates.</summary>
    public async Task<List<NearbyStationResponse>> GetNearbyAsync(double latitude, double longitude, double radiusKm)
    {
        // Filter active stations within radius using Haversine distance.
        if (latitude < -90 || latitude > 90)
            throw new InvalidOperationException("Latitude must be between -90 and 90.");

        if (longitude < -180 || longitude > 180)
            throw new InvalidOperationException("Longitude must be between -180 and 180.");

        if (radiusKm <= 0 || radiusKm > 500)
            throw new InvalidOperationException("RadiusKm must be between 0 and 500.");

        var stations = await _db.SolarStations
            .Find(s => s.Status == StationStatus.Active)
            .ToListAsync();

        return stations
            .Select(s =>
            {
                var lat = s.Location.Coordinates[1];
                var lng = s.Location.Coordinates[0];
                var distance = HaversineKm(latitude, longitude, lat, lng);
                var response = ToNearbyResponse(s, distance);
                return response;
            })
            .Where(s => s.DistanceKm <= radiusKm)
            .OrderBy(s => s.DistanceKm)
            .ToList();
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

    //------------------------------------------
    // REACTIVATE an inactive node
    public async Task<StationResponse> ReactivateAsync(string id)
    {
        var station = await FindRequiredAsync(id);

        if (station.Status == StationStatus.Active)
            throw new InvalidOperationException("Station is already active.");

        if (station.Status != StationStatus.Inactive)
            throw new InvalidOperationException("Only inactive stations can be reactivated.");

        station.Status = StationStatus.Active;
        station.UpdatedAt = DateTime.UtcNow;
        await _db.SolarStations.ReplaceOneAsync(s => s.Id == station.Id, station);

        return ToResponse(station);
    }

    //---------------
    // Helpers
    //---------------

    /// <summary>Creates N battery documents for a newly created station.</summary>
    private async Task CreateBatteriesAsync(SolarStation station, string? createdByUserId)
    {
        // Insert one booking slot document per physical battery.
        if (station.AvailableBatterySlots <= 0)
            return;

        var now = DateTime.UtcNow;
        var batteries = Enumerable.Range(1, station.AvailableBatterySlots)
            .Select(index => new EnergyBookingSlot
            {
                StationId = station.Id,
                BatteryIndex = index,
                CapacityKwh = station.BatteryCapacityKwh,
                ActualEnergyKwh = 0,
                ReservedChargingKwh = 0,
                ReservedDropOffKwh = 0,
                Status = SlotStatus.Available,
                CreatedBy = createdByUserId,
                CreatedAt = now,
                UpdatedAt = now
            })
            .ToList();

        await _db.EnergyBookingSlots.InsertManyAsync(batteries);
    }

    /// <summary>
    /// Syncs battery documents when station battery count or capacity changes.
    /// </summary>
    private async Task SyncBatteriesAsync(
        SolarStation station,
        int _previousBatteryCount,
        double previousCapacityKwh,
        string? createdByUserId)
    {
        // Add, remove, or resize batteries when station config changes.
        var batteries = await _db.EnergyBookingSlots
            .Find(b => b.StationId == station.Id)
            .SortBy(b => b.BatteryIndex)
            .ToListAsync();

        var newCount = station.AvailableBatterySlots;
        var currentCount = batteries.Count;

        if (newCount > currentCount)
        {
            var startIndex = currentCount == 0
                ? 1
                : batteries.Max(b => b.BatteryIndex) + 1;
            var toAdd = newCount - currentCount;
            var now = DateTime.UtcNow;
            var extras = Enumerable.Range(0, toAdd)
                .Select(offset => new EnergyBookingSlot
                {
                    StationId = station.Id,
                    BatteryIndex = startIndex + offset,
                    CapacityKwh = station.BatteryCapacityKwh,
                    ActualEnergyKwh = 0,
                    ReservedChargingKwh = 0,
                    ReservedDropOffKwh = 0,
                    Status = SlotStatus.Available,
                    CreatedBy = createdByUserId,
                    CreatedAt = now,
                    UpdatedAt = now
                })
                .ToList();
            await _db.EnergyBookingSlots.InsertManyAsync(extras);
            batteries.AddRange(extras);
        }
        else if (newCount < currentCount)
        {
            var removeCount = currentCount - newCount;
            var candidates = batteries
                .OrderByDescending(b => b.BatteryIndex)
                .Take(removeCount)
                .ToList();

            foreach (var battery in candidates)
            {
                if (battery.ReservedChargingKwh > 0 || battery.ReservedDropOffKwh > 0)
                    throw new InvalidOperationException(
                        $"Cannot reduce battery count: battery {battery.BatteryIndex} still has reserved energy.");

                var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
                var active = await _db.EnergyReservations.CountDocumentsAsync(
                    r => r.SlotId == battery.Id && activeStatuses.Contains(r.Status));

                if (active > 0)
                    throw new InvalidOperationException(
                        $"Cannot reduce battery count: battery {battery.BatteryIndex} has active reservations.");
            }

            var ids = candidates.Select(b => b.Id).ToList();
            await _db.EnergyBookingSlots.DeleteManyAsync(b => ids.Contains(b.Id));
            batteries = batteries.Where(b => !ids.Contains(b.Id)).ToList();
        }

        if (Math.Abs(station.BatteryCapacityKwh - previousCapacityKwh) > 0.0001)
        {
            var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
            foreach (var battery in batteries)
            {
                var active = await _db.EnergyReservations.CountDocumentsAsync(
                    r => r.SlotId == battery.Id && activeStatuses.Contains(r.Status));

                if (active > 0)
                    throw new InvalidOperationException(
                        "Cannot change battery capacity while batteries have active reservations.");

                if (battery.ActualEnergyKwh > station.BatteryCapacityKwh)
                    throw new InvalidOperationException(
                        $"Cannot set capacity below actual energy on battery {battery.BatteryIndex}.");
            }

            await _db.EnergyBookingSlots.UpdateManyAsync(
                b => b.StationId == station.Id,
                Builders<EnergyBookingSlot>.Update
                    .Set(b => b.CapacityKwh, station.BatteryCapacityKwh)
                    .Set(b => b.UpdatedAt, DateTime.UtcNow));
        }
    }

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
        // Loop until a unique SGH- code is found.
        string code;
        do
        {
            code = "SGH-" + Guid.NewGuid().ToString("N")[..6].ToUpper();
        }
        while (await _db.SolarStations.Find(s => s.StationCode == code).AnyAsync());

        return code;
    }

    /// <summary>Great-circle distance in km between two WGS84 points.</summary>
    private static double HaversineKm(double lat1, double lon1, double lat2, double lon2)
    {
        // Compute great-circle distance in kilometers.
        const double earthRadiusKm = 6371.0;
        var dLat = DegreesToRadians(lat2 - lat1);
        var dLon = DegreesToRadians(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(DegreesToRadians(lat1)) * Math.Cos(DegreesToRadians(lat2)) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        return earthRadiusKm * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
    }

    /// <summary>Convert degrees to radians.</summary>
    private static double DegreesToRadians(double degrees) => degrees * Math.PI / 180.0;

    /// <summary>Total capacity = battery slots × kWh per battery.</summary>
    private static double ComputeTotalCapacityKwh(int availableBatterySlots, double batteryCapacityKwh) =>
        availableBatterySlots * batteryCapacityKwh;

    /// <summary>Map station entity to API response DTO.</summary>
    private static StationResponse ToResponse(SolarStation s) => new()
    {
        Id = s.Id,
        StationCode = s.StationCode,
        Name = s.Name,
        Description = s.Description,
        // GeoJSON: index 0 = longitude, index 1 = latitude
        Longitude = s.Location.Coordinates[0],
        Latitude = s.Location.Coordinates[1],
        BatteryCapacityKwh = s.BatteryCapacityKwh,
        AvailableBatterySlots = s.AvailableBatterySlots,
        TotalCapacityKwh = ComputeTotalCapacityKwh(s.AvailableBatterySlots, s.BatteryCapacityKwh),
        Schedule = s.Schedule,
        Status = s.Status,
        CreatedBy = s.CreatedBy,
        CreatedAt = s.CreatedAt,
        UpdatedAt = s.UpdatedAt
    };

    /// <summary>Map station entity plus distance for nearby search.</summary>
    private static NearbyStationResponse ToNearbyResponse(SolarStation s, double distanceKm)
    {
        // Map station to nearby DTO with rounded distance.
        var baseResponse = ToResponse(s);
        return new NearbyStationResponse
        {
            Id = baseResponse.Id,
            StationCode = baseResponse.StationCode,
            Name = baseResponse.Name,
            Description = baseResponse.Description,
            Longitude = baseResponse.Longitude,
            Latitude = baseResponse.Latitude,
            BatteryCapacityKwh = baseResponse.BatteryCapacityKwh,
            AvailableBatterySlots = baseResponse.AvailableBatterySlots,
            TotalCapacityKwh = baseResponse.TotalCapacityKwh,
            Schedule = baseResponse.Schedule,
            Status = baseResponse.Status,
            CreatedBy = baseResponse.CreatedBy,
            CreatedAt = baseResponse.CreatedAt,
            UpdatedAt = baseResponse.UpdatedAt,
            DistanceKm = Math.Round(distanceKm, 2)
        };
    }

}


