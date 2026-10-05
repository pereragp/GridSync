// -------------------------------------------------------------
// File: BookingSlotService.cs
// Project: GridSync.Api
// Description: List, update notes, close, and delete station batteries.
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
        // Wire MongoDB context for battery slots.
        _db = db;
    }

    /// <summary>Lists batteries, optionally filtered by station and status.</summary>
    public async Task<List<BookingSlotResponse>> GetAllAsync(string? stationId, string? status)
    {
        // List batteries with optional station and status filters.
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
            .SortBy(s => s.StationId)
            .ThenBy(s => s.BatteryIndex)
            .ToListAsync();

        return await MapWithStationNamesAsync(slots);
    }

    /// <summary>Returns one battery by id.</summary>
    public async Task<BookingSlotResponse> GetByIdAsync(string id)
    {
        // Load one battery and include station name.
        var slot = await FindRequiredAsync(id);
        var stationName = await ResolveStationNameAsync(slot.StationId);
        return ToResponse(slot, stationName);
    }

    /// <summary>Updates battery notes only (capacity is owned by the station).</summary>
    public async Task<BookingSlotResponse> UpdateAsync(string id, UpdateBookingSlotRequest request)
    {
        // Update notes on a non-closed battery.
        var slot = await FindRequiredAsync(id);

        if (slot.Status == SlotStatus.Closed)
            throw new InvalidOperationException("Closed batteries cannot be updated.");

        slot.Notes = request.Notes?.Trim();
        slot.UpdatedAt = DateTime.UtcNow;

        await _db.EnergyBookingSlots.ReplaceOneAsync(s => s.Id == slot.Id, slot);
        return ToResponse(slot, await ResolveStationNameAsync(slot.StationId));
    }

    /// <summary>Marks a battery as Closed so it cannot be booked.</summary>
    public async Task<BookingSlotResponse> CloseAsync(string id)
    {
        // Close battery when no reserves or active bookings remain.
        var slot = await FindRequiredAsync(id);

        if (slot.Status == SlotStatus.Closed)
            throw new InvalidOperationException("Battery is already closed.");

        if (slot.ReservedChargingKwh > 0 || slot.ReservedDropOffKwh > 0)
            throw new InvalidOperationException("Cannot close a battery with reserved energy.");

        var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
        var active = await _db.EnergyReservations.CountDocumentsAsync(
            r => r.SlotId == id && activeStatuses.Contains(r.Status));

        if (active > 0)
            throw new InvalidOperationException("Cannot close a battery with active reservations.");

        slot.Status = SlotStatus.Closed;
        slot.UpdatedAt = DateTime.UtcNow;
        await _db.EnergyBookingSlots.ReplaceOneAsync(s => s.Id == slot.Id, slot);
        return ToResponse(slot, await ResolveStationNameAsync(slot.StationId));
    }

    /// <summary>Reopens a closed battery for booking.</summary>
    public async Task<BookingSlotResponse> ReopenAsync(string id)
    {
        // Reopen a closed battery on an active station.
        var slot = await FindRequiredAsync(id);

        if (slot.Status == SlotStatus.Available)
            throw new InvalidOperationException("Battery is already available.");

        var station = await _db.SolarStations.Find(s => s.Id == slot.StationId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Station not found.");

        if (station.Status != StationStatus.Active)
            throw new InvalidOperationException("Cannot reopen a battery on an inactive station.");

        slot.Status = SlotStatus.Available;
        slot.UpdatedAt = DateTime.UtcNow;
        await _db.EnergyBookingSlots.ReplaceOneAsync(s => s.Id == slot.Id, slot);
        return ToResponse(slot, station.Name);
    }

    /// <summary>Deletes a battery with no reservations or reserved energy.</summary>
    public async Task DeleteAsync(string id)
    {
        // Remove battery when no linked active reservations.
        var slot = await FindRequiredAsync(id);

        if (slot.ReservedChargingKwh > 0 || slot.ReservedDropOffKwh > 0)
            throw new InvalidOperationException("Cannot delete a battery with reserved energy.");

        var activeStatuses = new[] { ReservationStatus.Pending, ReservationStatus.Approved };
        var linked = await _db.EnergyReservations.CountDocumentsAsync(
            r => r.SlotId == id && activeStatuses.Contains(r.Status));

        if (linked > 0)
            throw new InvalidOperationException("Cannot delete a battery with active reservations.");

        await _db.EnergyBookingSlots.DeleteOneAsync(s => s.Id == id);
    }

    private async Task<EnergyBookingSlot> FindRequiredAsync(string id)
    {
        // Load battery by id or throw not found.
        if (!ObjectId.TryParse(id, out _))
            throw new KeyNotFoundException($"Battery '{id}' was not found.");

        return await _db.EnergyBookingSlots.Find(s => s.Id == id).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException($"Battery '{id}' was not found.");
    }

    private async Task<string> ResolveStationNameAsync(string stationId)
    {
        // Look up station display name (empty if missing).
        var station = await _db.SolarStations.Find(s => s.Id == stationId).FirstOrDefaultAsync();
        return station?.Name ?? string.Empty;
    }

    private async Task<List<BookingSlotResponse>> MapWithStationNamesAsync(List<EnergyBookingSlot> slots)
    {
        // Batch-resolve station names for slot DTOs.
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

    /// <summary>Map battery entity to API response DTO.</summary>
    private static BookingSlotResponse ToResponse(EnergyBookingSlot slot, string stationName) => new()
    {
        Id = slot.Id,
        StationId = slot.StationId,
        StationName = stationName,
        BatteryIndex = slot.BatteryIndex,
        CapacityKwh = slot.CapacityKwh,
        ActualEnergyKwh = slot.ActualEnergyKwh,
        ReservedChargingKwh = slot.ReservedChargingKwh,
        ReservedDropOffKwh = slot.ReservedDropOffKwh,
        AvailableChargingKwh = Math.Max(slot.CapacityKwh - slot.ActualEnergyKwh - slot.ReservedChargingKwh, 0),
        AvailableDropOffKwh = Math.Max(slot.ActualEnergyKwh - slot.ReservedDropOffKwh, 0),
        Status = slot.Status,
        Notes = slot.Notes,
        CreatedBy = slot.CreatedBy,
        CreatedAt = slot.CreatedAt,
        UpdatedAt = slot.UpdatedAt
    };
}
