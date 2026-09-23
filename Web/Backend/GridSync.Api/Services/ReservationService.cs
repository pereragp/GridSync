using System.Security.Cryptography;
using GridSync.Api.Data;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using MongoDB.Bson;
using MongoDB.Driver;

namespace GridSync.Api.Services;

public class ReservationService
{
    private readonly MongoDbContext _db;

    public ReservationService(MongoDbContext db)
    {
        _db = db;
    }

    public async Task<EnergyReservation> GetByIdAsync(string userId, string reservationId, bool staffAccess)
    {
        var reservation = await FindRequiredAsync(reservationId);

        if (!staffAccess && reservation.ProsumerId != userId)
        {
            throw new KeyNotFoundException("Reservation not found.");
        }

        return reservation;
    }

    public async Task<List<EnergyReservation>> GetHistoryAsync(string prosumerId, string? status)
    {
        await ExpirePastReservationsAsync();

        var filter = Builders<EnergyReservation>.Filter.Eq(reservation => reservation.ProsumerId, prosumerId) &
                     Builders<EnergyReservation>.Filter.Lt(reservation => reservation.SlotEnd, DateTime.UtcNow);

        if (!string.IsNullOrWhiteSpace(status))
        {
            filter &= Builders<EnergyReservation>.Filter.Eq(
                reservation => reservation.Status,
                status.Trim());
        }

        return await _db.EnergyReservations
            .Find(filter)
            .SortByDescending(reservation => reservation.SlotStart)
            .ToListAsync();
    }

    public async Task<List<EnergyReservation>> GetForStaffAsync(string? status)
    {
        await ExpirePastReservationsAsync();

        var filter = Builders<EnergyReservation>.Filter.Empty;
        if (!string.IsNullOrWhiteSpace(status))
        {
            filter = Builders<EnergyReservation>.Filter.Eq(
                reservation => reservation.Status,
                status.Trim());
        }

        return await _db.EnergyReservations
            .Find(filter)
            .SortBy(reservation => reservation.SlotStart)
            .ToListAsync();
    }

    public async Task<EnergyReservation> ReviewAsync(
        string staffId,
        string reservationId,
        bool approve,
        ReviewReservationRequest request)
    {
        var reservation = await FindRequiredAsync(reservationId);

        if (reservation.Status != ReservationStatus.Pending)
        {
            throw new InvalidOperationException("Only pending reservations can be approved or rejected.");
        }

        if (approve)
        {
            reservation.Status = ReservationStatus.Approved;
        }
        else
        {
            if (string.IsNullOrWhiteSpace(request.Reason))
            {
                throw new InvalidOperationException("A rejection reason is required.");
            }

            reservation.Status = ReservationStatus.Rejected;
            reservation.RejectionReason = request.Reason.Trim();
            reservation.RejectedAt = DateTime.UtcNow;
            reservation.RejectedBy = staffId;
        }

        reservation.UpdatedAt = DateTime.UtcNow;
        var result = await _db.EnergyReservations.ReplaceOneAsync(
            candidate => candidate.Id == reservation.Id && candidate.Status == ReservationStatus.Pending,
            reservation);

        if (result.ModifiedCount != 1)
        {
            throw new InvalidOperationException("Reservation status has already changed.");
        }

        if (!approve)
        {
            await ReleaseSlotAsync(reservation.SlotId);
        }

        return reservation;
    }

    public async Task<EnergyReservation> UpdateAsync(
        string prosumerId,
        string reservationId,
        UpdateReservationRequest request)
    {
        var reservation = await FindOwnedAsync(prosumerId, reservationId);

        if (reservation.Status != ReservationStatus.Pending)
        {
            throw new InvalidOperationException("Only pending reservations can be updated.");
        }

        if (reservation.SlotStart - DateTime.UtcNow < TimeSpan.FromHours(12))
        {
            throw new InvalidOperationException("Reservations can only be updated at least 12 hours before the slot starts.");
        }

        if (request.ReservationType is not (ReservationTypes.Charging or ReservationTypes.DropOff))
        {
            throw new InvalidOperationException("ReservationType must be Charging or DropOff.");
        }

        reservation.ReservationType = request.ReservationType;
        reservation.UpdatedAt = DateTime.UtcNow;
        await _db.EnergyReservations.ReplaceOneAsync(
            candidate => candidate.Id == reservation.Id && candidate.ProsumerId == prosumerId,
            reservation);

        return reservation;
    }

    public async Task<EnergyReservation> CancelAsync(
        string prosumerId,
        string reservationId,
        CancelReservationRequest request)
    {
        var reservation = await FindOwnedAsync(prosumerId, reservationId);

        if (reservation.Status is ReservationStatus.Cancelled or ReservationStatus.Completed or ReservationStatus.Rejected)
        {
            throw new InvalidOperationException("Reservation cannot be cancelled in its current status.");
        }

        if (reservation.SlotStart - DateTime.UtcNow < TimeSpan.FromHours(12))
        {
            throw new InvalidOperationException("Reservations can only be cancelled at least 12 hours before the slot starts.");
        }

        reservation.Status = ReservationStatus.Cancelled;
        reservation.CancellationReason = string.IsNullOrWhiteSpace(request.Reason)
            ? null
            : request.Reason.Trim();
        reservation.CancelledAt = DateTime.UtcNow;
        reservation.UpdatedAt = DateTime.UtcNow;

        var result = await _db.EnergyReservations.ReplaceOneAsync(
            candidate => candidate.Id == reservation.Id &&
                         candidate.ProsumerId == prosumerId &&
                         candidate.Status != ReservationStatus.Cancelled,
            reservation);

        if (result.ModifiedCount != 1)
        {
            throw new InvalidOperationException("Reservation has already been cancelled.");
        }

        await ReleaseSlotAsync(reservation.SlotId);

        return reservation;
    }

    public async Task<EnergyReservation> CreateAsync(string prosumerId, CreateReservationRequest request)
    {
        if (!ObjectId.TryParse(prosumerId, out _))
        {
            throw new InvalidOperationException("The authenticated user id is invalid.");
        }

        if (!ObjectId.TryParse(request.SlotId, out _))
        {
            throw new InvalidOperationException("SlotId must be a valid id.");
        }

        if (request.ReservationType is not (ReservationTypes.Charging or ReservationTypes.DropOff))
        {
            throw new InvalidOperationException("ReservationType must be Charging or DropOff.");
        }

        var prosumer = await _db.Users
            .Find(user => user.Id == prosumerId)
            .FirstOrDefaultAsync();

        if (prosumer is null)
        {
            throw new KeyNotFoundException("Prosumer not found.");
        }

        if (prosumer.Role != UserRoles.Prosumer)
        {
            throw new InvalidOperationException("Only prosumers can create reservations.");
        }

        if (prosumer.Status != UserStatus.Active)
        {
            throw new InvalidOperationException("Only active prosumers can create reservations.");
        }

        var slot = await _db.EnergyBookingSlots
            .Find(candidate => candidate.Id == request.SlotId)
            .FirstOrDefaultAsync();

        if (slot is null)
        {
            throw new KeyNotFoundException("Booking slot not found.");
        }

        if (slot.SlotStart <= DateTime.UtcNow)
        {
            throw new InvalidOperationException("Booking slot has already started.");
        }

        if (slot.SlotStart > DateTime.UtcNow.AddDays(7))
        {
            throw new InvalidOperationException("Reservations can only be created within the next 7 days.");
        }

        if (slot.SlotEnd <= slot.SlotStart)
        {
            throw new InvalidOperationException("Booking slot has an invalid time range.");
        }

        var station = await _db.SolarStations
            .Find(candidate => candidate.Id == slot.StationId)
            .FirstOrDefaultAsync();

        if (station is null)
        {
            throw new KeyNotFoundException("Solar station not found.");
        }

        if (station.Status != StationStatus.Active)
        {
            throw new InvalidOperationException("Solar station is inactive.");
        }

        var hasExistingReservation = await _db.EnergyReservations.Find(reservation =>
            reservation.SlotId == slot.Id &&
            reservation.ProsumerId == prosumerId &&
            reservation.Status != ReservationStatus.Cancelled &&
            reservation.Status != ReservationStatus.Rejected &&
            reservation.Status != ReservationStatus.Expired).AnyAsync();

        if (hasExistingReservation)
        {
            throw new InvalidOperationException("You already have a reservation for this slot.");
        }

        var claimedSlot = await _db.EnergyBookingSlots.FindOneAndUpdateAsync(
            candidate => candidate.Id == slot.Id &&
                         candidate.Status == SlotStatus.Available &&
                         candidate.ReservedCount < candidate.MaxReservations,
            Builders<EnergyBookingSlot>.Update
                .Inc(candidate => candidate.ReservedCount, 1)
                .Set(candidate => candidate.UpdatedAt, DateTime.UtcNow),
            new FindOneAndUpdateOptions<EnergyBookingSlot>
            {
                ReturnDocument = ReturnDocument.After
            });

        if (claimedSlot is null)
        {
            throw new InvalidOperationException("Booking slot is no longer available.");
        }

        var reservation = new EnergyReservation
        {
            ReservationCode = GenerateReservationCode(),
            ProsumerId = prosumer.Id,
            ProsumerNic = prosumer.Nic ?? string.Empty,
            StationId = station.Id,
            SlotId = claimedSlot.Id,
            StationName = station.Name,
            SlotStart = claimedSlot.SlotStart,
            SlotEnd = claimedSlot.SlotEnd,
            ReservationType = request.ReservationType,
            EnergyKwh = claimedSlot.EnergyKwh,
            Status = ReservationStatus.Pending,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        try
        {
            await _db.EnergyReservations.InsertOneAsync(reservation);
            await UpdateSlotStatusAsync(claimedSlot);
            return reservation;
        }
        catch
        {
            await _db.EnergyBookingSlots.UpdateOneAsync(
                candidate => candidate.Id == claimedSlot.Id && candidate.ReservedCount > 0,
                Builders<EnergyBookingSlot>.Update
                    .Inc(candidate => candidate.ReservedCount, -1)
                    .Set(candidate => candidate.Status, SlotStatus.Available)
                    .Set(candidate => candidate.UpdatedAt, DateTime.UtcNow));
            throw;
        }
    }

    private async Task UpdateSlotStatusAsync(EnergyBookingSlot slot)
    {
        if (slot.ReservedCount >= slot.MaxReservations)
        {
            await _db.EnergyBookingSlots.UpdateOneAsync(
                candidate => candidate.Id == slot.Id,
                Builders<EnergyBookingSlot>.Update
                    .Set(candidate => candidate.Status, SlotStatus.FullyBooked)
                    .Set(candidate => candidate.UpdatedAt, DateTime.UtcNow));
        }
    }

    private async Task ReleaseSlotAsync(string slotId)
    {
        await _db.EnergyBookingSlots.UpdateOneAsync(
            candidate => candidate.Id == slotId && candidate.ReservedCount > 0,
            Builders<EnergyBookingSlot>.Update
                .Inc(candidate => candidate.ReservedCount, -1)
                .Set(candidate => candidate.Status, SlotStatus.Available)
                .Set(candidate => candidate.UpdatedAt, DateTime.UtcNow));
    }

    private async Task ExpirePastReservationsAsync()
    {
        var now = DateTime.UtcNow;
        await _db.EnergyReservations.UpdateManyAsync(
            reservation => reservation.SlotEnd <= now &&
                           (reservation.Status == ReservationStatus.Pending ||
                            reservation.Status == ReservationStatus.Approved),
            Builders<EnergyReservation>.Update
                .Set(reservation => reservation.Status, ReservationStatus.Expired)
                .Set(reservation => reservation.UpdatedAt, now));
    }

    private async Task<EnergyReservation> FindRequiredAsync(string reservationId)
    {
        if (!ObjectId.TryParse(reservationId, out _))
        {
            throw new InvalidOperationException("Reservation id must be a valid id.");
        }

        var reservation = await _db.EnergyReservations
            .Find(candidate => candidate.Id == reservationId)
            .FirstOrDefaultAsync();

        if (reservation is null)
        {
            throw new KeyNotFoundException("Reservation not found.");
        }

        return reservation;
    }

    private async Task<EnergyReservation> FindOwnedAsync(string prosumerId, string reservationId)
    {
        if (!ObjectId.TryParse(reservationId, out _))
        {
            throw new InvalidOperationException("Reservation id must be a valid id.");
        }

        var reservation = await _db.EnergyReservations
            .Find(candidate => candidate.Id == reservationId && candidate.ProsumerId == prosumerId)
            .FirstOrDefaultAsync();

        if (reservation is null)
        {
            throw new KeyNotFoundException("Reservation not found.");
        }

        return reservation;
    }

    private static string GenerateReservationCode()
    {
        return $"RSV-{DateTime.UtcNow:yyyyMMdd}-{RandomNumberGenerator.GetInt32(100000, 1000000)}";
    }
}
