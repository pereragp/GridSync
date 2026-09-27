// -------------------------------------------------------------
// File: BookingSlotService.cs
// Description: Create, list, update, close, and delete energy booking slots.
// -------------------------------------------------------------

using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using MongoDB.Bson;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class BookingSlotService
{
    private readonly MongoDbContext _db;

    public BookingSlotService(MongoDbContext db)
    {
        _db = db;
    }

    /// <summary>Creates a bookable time window on an active station.</summary>
    public async Task<BookingSlotResponse> CreateAsync(CreateBookingSlotRequest request, string createdByUserId)
    {
        ValidateWindow(request.SlotStart, request.SlotEnd, request.EnergyKwh, request.MaxReservations);

        if (!ObjectId.TryParse(request.StationId, out _))
            throw new InvalidOperationException("StationId must be a valid id.");

        var station = await _db.SolarStations
            .Find(s => s.Id == request.StationId)
            .FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException($"Station '{request.StationId}' was not found.");

        if (station.Status != StationStatus.Active)
            throw new InvalidOperationException("Slots can only be created on active stations.");

        var slot = new EnergyBookingSlot
        {
            StationId = request.StationId,
            SlotStart = EnsureUtc(request.SlotStart),
            SlotEnd = EnsureUtc(request.SlotEnd),
            EnergyKwh = request.EnergyKwh,
            MaxReservations = request.MaxReservations,
            ReservedCount = 0,
            Status = SlotStatus.Available,
            Notes = request.Notes?.Trim(),
            CreatedBy = createdByUserId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _db.EnergyBookingSlots.InsertOneAsync(slot);
        return ToResponse(slot, station.Name);
    }

    /// <summary>Lists slots, optionally filtered by station and status.</summary>
    public async Task<List<BookingSlotResponse>> GetAllAsync(string? stationId, string? status)
    {
        var filter = Builders<EnergyBookingSlot>.Filter.Empty;

        if (!string.IsNullOrWhiteSpace(stationId))
        {
            if (!ObjectId.TryParse(stationId, out _))
                throw new InvalidOperationException("StationId must be a valid id.");
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId.Trim());
        }

        if (!string.IsNullOrWhiteSpace(status))
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(s => s.Status, status.Trim());

        var slots = await _db.EnergyBookingSlots
            .Find(filter)
            .SortBy(s => s.SlotStart)
            .ToListAsync();

        return await MapWithStationNamesAsync(slots);
    }

    /// <summary>Returns one slot by id.</summary>
    public async Task<BookingSlotResponse> GetByIdAsync(string id)
    {
        var slot = await FindRequiredAsync(id);
        var stationName = await ResolveStationNameAsync(slot.StationId);
        return ToResponse(slot, stationName);
    }

    /// <summary>Updates slot timing and capacity when no reservations are held.</summary>
    public async Task<BookingSlotResponse> UpdateAsync(string id, UpdateBookingSlotRequest request)
    {
        ValidateWindow(request.SlotStart, request.SlotEnd, request.EnergyKwh, request.MaxReservations);

        var slot = await FindRequiredAsync(id);

        if (slot.ReservedCount > 0)
            throw new InvalidOperationException("Cannot update a slot that already has reservations.");

        if (slot.Status == SlotStatus.Closed)
            throw new InvalidOperationException("Closed slots cannot be updated. Create a new slot instead.");

        if (request.MaxReservations < slot.ReservedCount)
            throw new InvalidOperationException("MaxReservations cannot be less than the current reserved count.");

        slot.SlotStart = EnsureUtc(request.SlotStart);
        slot.SlotEnd = EnsureUtc(request.SlotEnd);
        slot.EnergyKwh = request.EnergyKwh;
        slot.MaxReservations = request.MaxReservations;
        slot.Notes = request.Notes?.Trim();
        slot.Status = SlotStatus.Available;
        slot.UpdatedAt = DateTime.UtcNow;

        await _db.EnergyBookingSlots.ReplaceOneAsync(s => s.Id == slot.Id, slot);
        return ToResponse(slot, await ResolveStationNameAsync(slot.StationId));
    }

    /// <summary>Marks a slot as Closed so it can no longer be booked.</summary>
    public async Task<BookingSlotResponse> CloseAsync(string id)
    {
        var slot = await FindRequiredAsync(id);

        if (slot.Status == SlotStatus.Closed)
            throw new InvalidOperationException("Slot is already closed.");

        slot.Status = SlotStatus.Closed;
        slot.UpdatedAt = DateTime.UtcNow;
        await _db.EnergyBookingSlots.ReplaceOneAsync(s => s.Id == slot.Id, slot);
        return ToResponse(slot, await ResolveStationNameAsync(slot.StationId));
    }

    /// <summary>Deletes a slot that has no reservations.</summary>
    public async Task DeleteAsync(string id)
    {
        var slot = await FindRequiredAsync(id);

        if (slot.ReservedCount > 0)
            throw new InvalidOperationException("Cannot delete a slot that has reservations.");

        var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
        var linked = await _db.EnergyReservations.CountDocumentsAsync(
            r => r.SlotId == id && activeStatuses.Contains(r.Status));

        if (linked > 0)
            throw new InvalidOperationException("Cannot delete a slot with active reservations.");

        await _db.EnergyBookingSlots.DeleteOneAsync(s => s.Id == id);
    }

    private async Task<EnergyBookingSlot> FindRequiredAsync(string id)
    {
        if (!ObjectId.TryParse(id, out _))
            throw new KeyNotFoundException($"Slot '{id}' was not found.");

        return await _db.EnergyBookingSlots.Find(s => s.Id == id).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException($"Slot '{id}' was not found.");
    }

    private async Task<string> ResolveStationNameAsync(string stationId)
    {
        var station = await _db.SolarStations.Find(s => s.Id == stationId).FirstOrDefaultAsync();
        return station?.Name ?? string.Empty;
    }

    private async Task<List<BookingSlotResponse>> MapWithStationNamesAsync(List<EnergyBookingSlot> slots)
    {
        if (slots.Count == 0)
            return [];

        var stationIds = slots.Select(s => s.StationId).Distinct().ToList();
        var stations = await _db.SolarStations
            .Find(s => stationIds.Contains(s.Id))
            .ToListAsync();
        var names = stations.ToDictionary(s => s.Id, s => s.Name);

        return slots
            .Select(s => ToResponse(s, names.GetValueOrDefault(s.StationId, string.Empty)))
            .ToList();
    }

    private static void ValidateWindow(DateTime start, DateTime end, double energyKwh, int maxReservations)
    {
        start = EnsureUtc(start);
        end = EnsureUtc(end);

        if (end <= start)
            throw new InvalidOperationException("SlotEnd must be after SlotStart.");

        if (start <= DateTime.UtcNow)
            throw new InvalidOperationException("SlotStart must be in the future.");

        if (energyKwh <= 0)
            throw new InvalidOperationException("EnergyKwh must be greater than zero.");

        if (maxReservations < 1)
            throw new InvalidOperationException("MaxReservations must be at least 1.");
    }

    private static DateTime EnsureUtc(DateTime value) =>
        value.Kind == DateTimeKind.Utc ? value : DateTime.SpecifyKind(value, DateTimeKind.Utc);

    private static BookingSlotResponse ToResponse(EnergyBookingSlot slot, string stationName) => new()
    {
        Id = slot.Id,
        StationId = slot.StationId,
        StationName = stationName,
        SlotStart = slot.SlotStart,
        SlotEnd = slot.SlotEnd,
        EnergyKwh = slot.EnergyKwh,
        MaxReservations = slot.MaxReservations,
        ReservedCount = slot.ReservedCount,
        AvailableReservations = Math.Max(slot.MaxReservations - slot.ReservedCount, 0),
        Status = slot.Status,
        Notes = slot.Notes,
        CreatedBy = slot.CreatedBy,
        CreatedAt = slot.CreatedAt,
        UpdatedAt = slot.UpdatedAt
    };
}
