// -------------------------------------------------------------
// File: ReservationService.cs
// Project: GridSync.Api
// Description: Business logic for energy reservations (FAT service).
// -------------------------------------------------------------

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

    /// <summary>Inject MongoDB context.</summary>
    public ReservationService(MongoDbContext db)
    {
        // Wire MongoDB context for reservation data.
        _db = db;
    }

    /// <summary>Load one reservation; prosumers only see their own.</summary>
    public async Task<EnergyReservation> GetByIdAsync(string userId, string reservationId, bool staffAccess)
    {
        // Load reservation and enforce prosumer ownership unless staff.
        var reservation = await FindRequiredAsync(reservationId);

        if (!staffAccess && reservation.ProsumerId != userId)
            throw new KeyNotFoundException("Reservation not found.");

        return reservation;
    }

    /// <summary>Past reservations for a prosumer (optionally by status).</summary>
    public async Task<List<EnergyReservation>> GetHistoryAsync(string prosumerId, string? status)
    {
        // Past slots for this prosumer, optionally filtered by status.
        await ExpirePastReservationsAsync();

        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerId, prosumerId);

        if (!string.IsNullOrWhiteSpace(status))
            filter &= Builders<EnergyReservation>.Filter.Eq(r => r.Status, status.Trim());

        return await _db.EnergyReservations
            .Find(filter & Builders<EnergyReservation>.Filter.Lte(r => r.SlotEnd, DateTime.UtcNow))
            .SortByDescending(r => r.SlotStart)
            .ToListAsync();
    }

    /// <summary>Upcoming reservations for a prosumer.</summary>
    public async Task<List<EnergyReservation>> GetUpcomingAsync(string prosumerId, string? status)
    {
        // Future slots for this prosumer, optionally filtered by status.
        await ExpirePastReservationsAsync();

        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerId, prosumerId) &
                     Builders<EnergyReservation>.Filter.Gt(r => r.SlotEnd, DateTime.UtcNow);

        if (!string.IsNullOrWhiteSpace(status))
            filter &= Builders<EnergyReservation>.Filter.Eq(r => r.Status, status.Trim());

        return await _db.EnergyReservations
            .Find(filter)
            .SortBy(r => r.SlotStart)
            .ToListAsync();
    }

    /// <summary>All reservations for grid-operator manage view.</summary>
    public async Task<List<EnergyReservation>> GetForStaffAsync(string? status)
    {
        // All reservations for operators, optionally filtered by status.
        await ExpirePastReservationsAsync();

        var filter = Builders<EnergyReservation>.Filter.Empty;
        if (!string.IsNullOrWhiteSpace(status))
            filter = Builders<EnergyReservation>.Filter.Eq(r => r.Status, status.Trim());

        return await _db.EnergyReservations
            .Find(filter)
            .SortBy(r => r.SlotStart)
            .ToListAsync();
    }

    /// <summary>Lists open batteries on active stations with Charging/DropOff availability.</summary>
    public async Task<List<AvailableBookingSlotResponse>> GetAvailableSlotsAsync()
    {
        // List bookable batteries on active stations with availability figures.
        var slots = await _db.EnergyBookingSlots
            .Find(slot => slot.Status == SlotStatus.Available)
            .SortBy(slot => slot.StationId)
            .ThenBy(slot => slot.BatteryIndex)
            .ToListAsync();

        if (slots.Count == 0)
            return [];

        var stationIds = slots.Select(s => s.StationId).Distinct().ToList();
        var stations = await _db.SolarStations
            .Find(s => stationIds.Contains(s.Id) && s.Status == StationStatus.Active)
            .ToListAsync();
        var stationById = stations.ToDictionary(s => s.Id);

        return slots
            .Where(s => stationById.ContainsKey(s.StationId))
            .Select(s =>
            {
                var station = stationById[s.StationId];
                var schedule = station.Schedule ?? new StationSchedule();
                return new AvailableBookingSlotResponse
                {
                    Id = s.Id,
                    StationId = s.StationId,
                    StationName = station.Name,
                    BatteryIndex = s.BatteryIndex,
                    CapacityKwh = s.CapacityKwh,
                    ActualEnergyKwh = s.ActualEnergyKwh,
                    AvailableChargingKwh = AvailableCharging(s),
                    AvailableDropOffKwh = AvailableDropOff(s),
                    Status = s.Status,
                    Notes = s.Notes,
                    OpenTime = schedule.OpenTime,
                    CloseTime = schedule.CloseTime,
                    WorkingDays = schedule.WorkingDays ?? [],
                };
            })
            .ToList();
    }

    /// <summary>Approve or reject a pending reservation.</summary>
    public async Task<EnergyReservation> ReviewAsync(
        string staffId,
        string reservationId,
        bool approve,
        ReviewReservationRequest request)
    {
        // Approve (QR) or reject a pending reservation.
        var reservation = await FindRequiredAsync(reservationId);

        if (reservation.Status != ReservationStatus.Pending)
            throw new InvalidOperationException("Only pending reservations can be approved or rejected.");

        if (approve)
        {
            reservation.Status = ReservationStatus.Approved;
            reservation.QrPayload = GenerateQrPayload(reservation);
            reservation.QrGeneratedAt = DateTime.UtcNow;
        }
        else
        {
            if (string.IsNullOrWhiteSpace(request.Reason))
                throw new InvalidOperationException("A rejection reason is required.");

            reservation.Status = ReservationStatus.Rejected;
            reservation.RejectionReason = request.Reason.Trim();
            reservation.RejectedAt = DateTime.UtcNow;
            reservation.RejectedBy = staffId;
        }

        reservation.UpdatedAt = DateTime.UtcNow;
        var result = await _db.EnergyReservations.ReplaceOneAsync(
            c => c.Id == reservation.Id && c.Status == ReservationStatus.Pending,
            reservation);

        if (result.ModifiedCount != 1)
            throw new InvalidOperationException("Reservation status has already changed.");

        if (!approve)
            await ReleaseReservedEnergyAsync(reservation);

        return reservation;
    }

    /// <summary>Validate a reservation QR payload for transfer.</summary>
    public async Task<ReservationQrVerificationResponse> VerifyQrAsync(string qrPayload)
    {
        // Look up an approved reservation by its QR payload.
        if (string.IsNullOrWhiteSpace(qrPayload))
            throw new InvalidOperationException("QR payload is required.");

        var reservation = await _db.EnergyReservations
            .Find(c => c.QrPayload == qrPayload.Trim())
            .FirstOrDefaultAsync();

        if (reservation is null)
            throw new KeyNotFoundException("QR code is not recognized.");

        if (reservation.Status != ReservationStatus.Approved)
            throw new InvalidOperationException(
                $"Reservation is not valid for transfer because its status is {reservation.Status}.");

        return new ReservationQrVerificationResponse
        {
            ReservationId = reservation.Id,
            ReservationCode = reservation.ReservationCode,
            ProsumerNic = reservation.ProsumerNic,
            StationName = reservation.StationName,
            SlotStart = reservation.SlotStart,
            SlotEnd = reservation.SlotEnd,
            ReservationType = reservation.ReservationType,
            EnergyKwh = reservation.EnergyKwh,
            Status = reservation.Status
        };
    }

    /// <summary>Complete an approved reservation and update battery stock.</summary>
    public async Task<ReservationCompletionResponse> CompleteAsync(string operatorId, string reservationId)
    {
        // Mark approved reservation completed and apply battery inventory.
        var reservation = await FindRequiredAsync(reservationId);
        if (reservation.Status != ReservationStatus.Approved)
            throw new InvalidOperationException(
                $"Only approved reservations can be completed. Current status: {reservation.Status}.");

        if (string.IsNullOrWhiteSpace(reservation.QrPayload))
            throw new InvalidOperationException("Reservation does not have a QR code.");

        await ApplyCompletionInventoryAsync(reservation);

        var completedAt = DateTime.UtcNow;
        reservation.Status = ReservationStatus.Completed;
        reservation.CompletedAt = completedAt;
        reservation.CompletedBy = operatorId;
        reservation.UpdatedAt = completedAt;

        var result = await _db.EnergyReservations.ReplaceOneAsync(
            c => c.Id == reservation.Id && c.Status == ReservationStatus.Approved,
            reservation);

        if (result.ModifiedCount != 1)
            throw new InvalidOperationException("Reservation status has already changed.");

        return new ReservationCompletionResponse
        {
            ReservationId = reservation.Id,
            ReservationCode = reservation.ReservationCode,
            Status = reservation.Status,
            CompletedAt = completedAt,
            CompletedBy = operatorId
        };
    }

    /// <summary>Operator dashboard reservation counts.</summary>
    public async Task<ReservationDashboardStatsResponse> GetDashboardStatsAsync()
    {
        // Aggregate reservation counts for operator dashboard.
        await ExpirePastReservationsAsync();
        var now = DateTime.UtcNow;
        var reservations = _db.EnergyReservations;

        return new ReservationDashboardStatsResponse
        {
            PendingReservations = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Pending),
            ApprovedUpcomingReservations = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Approved && r.SlotEnd > now),
            CompletedTransfers = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Completed),
            RejectedReservations = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Rejected),
            CancelledReservations = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Cancelled),
            ExpiredReservations = await reservations.CountDocumentsAsync(
                r => r.Status == ReservationStatus.Expired)
        };
    }

    /// <summary>Prosumer pending and active counts.</summary>
    public async Task<ProsumerDashboardStatsResponse> GetProsumerDashboardStatsAsync(string prosumerId)
    {
        // Pending and active counts for one prosumer.
        await ExpirePastReservationsAsync();
        var now = DateTime.UtcNow;
        var reservations = _db.EnergyReservations;

        return new ProsumerDashboardStatsResponse
        {
            PendingReservations = await reservations.CountDocumentsAsync(
                r => r.ProsumerId == prosumerId && r.Status == ReservationStatus.Pending),
            ActiveReservations = await reservations.CountDocumentsAsync(
                r => r.ProsumerId == prosumerId &&
                     r.Status == ReservationStatus.Approved &&
                     r.SlotEnd > now)
        };
    }

    /// <summary>Search reservations by status, station, date, or text.</summary>
    public async Task<List<EnergyReservation>> SearchAsync(
        string userId,
        bool staffAccess,
        string? status,
        string? stationId,
        DateTime? from,
        DateTime? to,
        string? q)
    {
        // Search reservations by status, station, date, or text.
        await ExpirePastReservationsAsync();

        var filter = staffAccess
            ? Builders<EnergyReservation>.Filter.Empty
            : Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerId, userId);

        if (!string.IsNullOrWhiteSpace(status))
            filter &= Builders<EnergyReservation>.Filter.Eq(r => r.Status, status.Trim());

        if (!string.IsNullOrWhiteSpace(stationId))
        {
            if (!ObjectId.TryParse(stationId, out _))
                throw new InvalidOperationException("StationId must be a valid id.");
            filter &= Builders<EnergyReservation>.Filter.Eq(r => r.StationId, stationId.Trim());
        }

        if (from.HasValue)
        {
            var fromUtc = EnsureUtc(from.Value);
            filter &= Builders<EnergyReservation>.Filter.Gte(r => r.SlotStart, fromUtc);
        }

        if (to.HasValue)
        {
            var toUtc = EnsureUtc(to.Value);
            filter &= Builders<EnergyReservation>.Filter.Lte(r => r.SlotStart, toUtc);
        }

        if (!string.IsNullOrWhiteSpace(q))
        {
            var term = q.Trim();
            filter &= Builders<EnergyReservation>.Filter.Or(
                Builders<EnergyReservation>.Filter.Regex(
                    r => r.ReservationCode, new BsonRegularExpression(term, "i")),
                Builders<EnergyReservation>.Filter.Regex(
                    r => r.StationName!, new BsonRegularExpression(term, "i")),
                Builders<EnergyReservation>.Filter.Regex(
                    r => r.ProsumerNic, new BsonRegularExpression(term, "i")));
        }

        return await _db.EnergyReservations
            .Find(filter)
            .SortByDescending(r => r.SlotStart)
            .ToListAsync();
    }

    /// <summary>Prosumer updates a pending reservation (≥12h before start).</summary>
    public async Task<EnergyReservation> UpdateAsync(
        string prosumerId,
        string reservationId,
        UpdateReservationRequest request)
    {
        // Prosumer updates a pending reservation (≥12h before start).
        var reservation = await FindOwnedAsync(prosumerId, reservationId);

        if (reservation.Status != ReservationStatus.Pending)
            throw new InvalidOperationException("Only pending reservations can be updated.");

        if (reservation.SlotStart - DateTime.UtcNow < TimeSpan.FromHours(12))
            throw new InvalidOperationException(
                "Reservations can only be updated at least 12 hours before the slot starts.");

        ValidateVisitWindow(request.SlotStart, request.SlotEnd);
        ValidateEnergyAndType(request.ReservationType, request.EnergyKwh);

        var station = await _db.SolarStations.Find(s => s.Id == reservation.StationId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Solar station not found.");

        var slotStart = EnsureUtc(request.SlotStart);
        var slotEnd = EnsureUtc(request.SlotEnd);
        ValidateAgainstStationSchedule(station, slotStart, slotEnd);

        // Release old reserved amount, then reserve the new amount.
        await ReleaseReservedEnergyAsync(reservation);

        try
        {
            await ReserveEnergyAsync(reservation.SlotId, request.ReservationType, request.EnergyKwh);
        }
        catch
        {
            await ReserveEnergyAsync(reservation.SlotId, reservation.ReservationType, reservation.EnergyKwh);
            throw;
        }

        reservation.ReservationType = request.ReservationType;
        reservation.EnergyKwh = request.EnergyKwh;
        reservation.SlotStart = slotStart;
        reservation.SlotEnd = slotEnd;
        reservation.UpdatedAt = DateTime.UtcNow;

        await _db.EnergyReservations.ReplaceOneAsync(
            c => c.Id == reservation.Id && c.ProsumerId == prosumerId,
            reservation);

        return reservation;
    }

    /// <summary>Prosumer cancels a reservation (≥12h before start).</summary>
    public async Task<EnergyReservation> CancelAsync(
        string prosumerId,
        string reservationId,
        CancelReservationRequest request)
    {
        // Prosumer cancels a reservation (≥12h before start).
        var reservation = await FindOwnedAsync(prosumerId, reservationId);

        if (reservation.Status is ReservationStatus.Cancelled or ReservationStatus.Completed
            or ReservationStatus.Rejected)
            throw new InvalidOperationException("Reservation cannot be cancelled in its current status.");

        if (reservation.SlotStart - DateTime.UtcNow < TimeSpan.FromHours(12))
            throw new InvalidOperationException(
                "Reservations can only be cancelled at least 12 hours before the slot starts.");

        reservation.Status = ReservationStatus.Cancelled;
        reservation.CancellationReason = string.IsNullOrWhiteSpace(request.Reason)
            ? null
            : request.Reason.Trim();
        reservation.CancelledAt = DateTime.UtcNow;
        reservation.UpdatedAt = DateTime.UtcNow;

        var result = await _db.EnergyReservations.ReplaceOneAsync(
            c => c.Id == reservation.Id &&
                 c.ProsumerId == prosumerId &&
                 c.Status != ReservationStatus.Cancelled,
            reservation);

        if (result.ModifiedCount != 1)
            throw new InvalidOperationException("Reservation has already been cancelled.");

        await ReleaseReservedEnergyAsync(reservation);

        return reservation;
    }

    /// <summary>Create a pending Charging or DropOff reservation.</summary>
    public async Task<EnergyReservation> CreateAsync(string prosumerId, CreateReservationRequest request)
    {
        // Create a pending Charging or DropOff reservation.
        if (!ObjectId.TryParse(prosumerId, out _))
            throw new InvalidOperationException("The authenticated user id is invalid.");

        if (!ObjectId.TryParse(request.SlotId, out _))
            throw new InvalidOperationException("SlotId must be a valid id.");

        ValidateEnergyAndType(request.ReservationType, request.EnergyKwh);
        ValidateVisitWindow(request.SlotStart, request.SlotEnd);

        var slotStart = EnsureUtc(request.SlotStart);
        var slotEnd = EnsureUtc(request.SlotEnd);

        if (slotStart <= DateTime.UtcNow)
            throw new InvalidOperationException("Visit start must be in the future.");

        if (slotStart > DateTime.UtcNow.AddDays(7))
            throw new InvalidOperationException("Reservations can only be created within the next 7 days.");

        var prosumer = await _db.Users.Find(u => u.Id == prosumerId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Prosumer not found.");

        if (prosumer.Role != UserRoles.Prosumer)
            throw new InvalidOperationException("Only prosumers can create reservations.");

        if (prosumer.Status != UserStatus.Active)
            throw new InvalidOperationException("Only active prosumers can create reservations.");

        var battery = await _db.EnergyBookingSlots.Find(s => s.Id == request.SlotId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Battery slot not found.");

        if (battery.Status != SlotStatus.Available)
            throw new InvalidOperationException("Battery is closed and cannot be booked.");

        var station = await _db.SolarStations.Find(s => s.Id == battery.StationId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Solar station not found.");

        if (station.Status != StationStatus.Active)
            throw new InvalidOperationException("Solar station is inactive.");

        ValidateAgainstStationSchedule(station, slotStart, slotEnd);

        var hasOverlap = await _db.EnergyReservations.Find(r =>
            r.SlotId == battery.Id &&
            r.ProsumerId == prosumerId &&
            r.Status != ReservationStatus.Cancelled &&
            r.Status != ReservationStatus.Rejected &&
            r.Status != ReservationStatus.Expired &&
            r.Status != ReservationStatus.Completed &&
            r.SlotStart < slotEnd &&
            r.SlotEnd > slotStart).AnyAsync();

        if (hasOverlap)
            throw new InvalidOperationException("You already have a reservation overlapping this visit window.");

        await ReserveEnergyAsync(battery.Id, request.ReservationType, request.EnergyKwh);

        var reservation = new EnergyReservation
        {
            ReservationCode = GenerateReservationCode(),
            ProsumerId = prosumer.Id,
            ProsumerNic = prosumer.Nic ?? string.Empty,
            StationId = station.Id,
            SlotId = battery.Id,
            StationName = station.Name,
            SlotStart = slotStart,
            SlotEnd = slotEnd,
            ReservationType = request.ReservationType,
            EnergyKwh = request.EnergyKwh,
            Status = ReservationStatus.Pending,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        try
        {
            await _db.EnergyReservations.InsertOneAsync(reservation);
            return reservation;
        }
        catch
        {
            await ReleaseReservedEnergyAsync(reservation);
            throw;
        }
    }

    /// <summary>Atomically reserves kWh on a battery for Charging or DropOff.</summary>
    private async Task ReserveEnergyAsync(string slotId, string reservationType, double energyKwh)
    {
        // Atomically soft-lock kWh on a battery.
        FilterDefinition<EnergyBookingSlot> filter;
        UpdateDefinition<EnergyBookingSlot> update;

        if (reservationType == ReservationTypes.Charging)
        {
            // Charging deposits energy: request <= free space (Capacity - Actual - ReservedCharging)
            filter = Builders<EnergyBookingSlot>.Filter.Where(s =>
                s.Id == slotId &&
                s.Status == SlotStatus.Available &&
                s.CapacityKwh - s.ActualEnergyKwh - s.ReservedChargingKwh >= energyKwh);

            update = Builders<EnergyBookingSlot>.Update
                .Inc(s => s.ReservedChargingKwh, energyKwh)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);
        }
        else
        {
            // Drop-off withdraws energy: request <= stored energy not already reserved
            filter = Builders<EnergyBookingSlot>.Filter.Where(s =>
                s.Id == slotId &&
                s.Status == SlotStatus.Available &&
                s.ActualEnergyKwh - s.ReservedDropOffKwh >= energyKwh);

            update = Builders<EnergyBookingSlot>.Update
                .Inc(s => s.ReservedDropOffKwh, energyKwh)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);
        }

        var claimed = await _db.EnergyBookingSlots.FindOneAndUpdateAsync(
            filter,
            update,
            new FindOneAndUpdateOptions<EnergyBookingSlot> { ReturnDocument = ReturnDocument.After });

        if (claimed is null)
        {
            var reason = reservationType == ReservationTypes.Charging
                ? "Not enough free capacity remaining on this battery for charging."
                : "Not enough actual energy available on this battery for drop-off.";
            throw new InvalidOperationException(reason);
        }
    }

    /// <summary>Releases reserved kWh without changing ActualEnergyKwh.</summary>
    private async Task ReleaseReservedEnergyAsync(EnergyReservation reservation)
    {
        // Release soft-locked kWh without changing actual energy.
        if (reservation.EnergyKwh <= 0)
            return;

        if (reservation.ReservationType == ReservationTypes.Charging)
        {
            await _db.EnergyBookingSlots.UpdateOneAsync(
                s => s.Id == reservation.SlotId && s.ReservedChargingKwh >= reservation.EnergyKwh,
                Builders<EnergyBookingSlot>.Update
                    .Inc(s => s.ReservedChargingKwh, -reservation.EnergyKwh)
                    .Set(s => s.UpdatedAt, DateTime.UtcNow));
        }
        else if (reservation.ReservationType == ReservationTypes.DropOff)
        {
            await _db.EnergyBookingSlots.UpdateOneAsync(
                s => s.Id == reservation.SlotId && s.ReservedDropOffKwh >= reservation.EnergyKwh,
                Builders<EnergyBookingSlot>.Update
                    .Inc(s => s.ReservedDropOffKwh, -reservation.EnergyKwh)
                    .Set(s => s.UpdatedAt, DateTime.UtcNow));
        }
    }

    /// <summary>
    /// On QR complete: Charging adds actual energy; Drop-off removes it.
    /// Reserved pools are cleared for the completed amount.
    /// </summary>
    private async Task ApplyCompletionInventoryAsync(EnergyReservation reservation)
    {
        // Apply Charging deposit or DropOff withdrawal on complete.
        FilterDefinition<EnergyBookingSlot> filter;
        UpdateDefinition<EnergyBookingSlot> update;

        if (reservation.ReservationType == ReservationTypes.Charging)
        {
            // Deposit into battery: free space must still cover the reserved amount
            filter = Builders<EnergyBookingSlot>.Filter.Where(s =>
                s.Id == reservation.SlotId &&
                s.ActualEnergyKwh + reservation.EnergyKwh <= s.CapacityKwh &&
                s.ReservedChargingKwh >= reservation.EnergyKwh);

            update = Builders<EnergyBookingSlot>.Update
                .Inc(s => s.ActualEnergyKwh, reservation.EnergyKwh)
                .Inc(s => s.ReservedChargingKwh, -reservation.EnergyKwh)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);
        }
        else
        {
            // Withdraw from battery: actual energy must cover the reserved amount
            filter = Builders<EnergyBookingSlot>.Filter.Where(s =>
                s.Id == reservation.SlotId &&
                s.ActualEnergyKwh >= reservation.EnergyKwh &&
                s.ReservedDropOffKwh >= reservation.EnergyKwh);

            update = Builders<EnergyBookingSlot>.Update
                .Inc(s => s.ActualEnergyKwh, -reservation.EnergyKwh)
                .Inc(s => s.ReservedDropOffKwh, -reservation.EnergyKwh)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);
        }

        var updated = await _db.EnergyBookingSlots.FindOneAndUpdateAsync(
            filter,
            update,
            new FindOneAndUpdateOptions<EnergyBookingSlot> { ReturnDocument = ReturnDocument.After });

        if (updated is null)
        {
            var reason = reservation.ReservationType == ReservationTypes.Charging
                ? "Cannot complete charging: battery does not have enough free capacity."
                : "Cannot complete drop-off: battery does not hold enough actual energy.";
            throw new InvalidOperationException(reason);
        }
    }

    /// <summary>Mark past Pending/Approved reservations as Expired.</summary>
    private async Task ExpirePastReservationsAsync()
    {
        // Mark past Pending/Approved reservations as Expired.
        var now = DateTime.UtcNow;
        var expired = await _db.EnergyReservations
            .Find(r => r.SlotEnd <= now &&
                       (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved))
            .ToListAsync();

        foreach (var reservation in expired)
        {
            var result = await _db.EnergyReservations.UpdateOneAsync(
                r => r.Id == reservation.Id &&
                     (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved),
                Builders<EnergyReservation>.Update
                    .Set(r => r.Status, ReservationStatus.Expired)
                    .Set(r => r.UpdatedAt, now));

            if (result.ModifiedCount == 1)
                await ReleaseReservedEnergyAsync(reservation);
        }
    }

    /// <summary>Load reservation by id or throw.</summary>
    private async Task<EnergyReservation> FindRequiredAsync(string reservationId)
    {
        // Load reservation by id or throw.
        if (!ObjectId.TryParse(reservationId, out _))
            throw new InvalidOperationException("Reservation id must be a valid id.");

        return await _db.EnergyReservations.Find(c => c.Id == reservationId).FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Reservation not found.");
    }

    /// <summary>Load reservation owned by prosumer or throw.</summary>
    private async Task<EnergyReservation> FindOwnedAsync(string prosumerId, string reservationId)
    {
        // Load reservation owned by prosumer or throw.
        if (!ObjectId.TryParse(reservationId, out _))
            throw new InvalidOperationException("Reservation id must be a valid id.");

        return await _db.EnergyReservations
            .Find(c => c.Id == reservationId && c.ProsumerId == prosumerId)
            .FirstOrDefaultAsync()
            ?? throw new KeyNotFoundException("Reservation not found.");
    }

    /// <summary>Ensure type is Charging/DropOff and energy > 0.</summary>
    private static void ValidateEnergyAndType(string reservationType, double energyKwh)
    {
        // Ensure type is Charging/DropOff and energy > 0.
        if (reservationType is not (ReservationTypes.Charging or ReservationTypes.DropOff))
            throw new InvalidOperationException("ReservationType must be Charging or DropOff.");

        if (energyKwh <= 0)
            throw new InvalidOperationException("EnergyKwh must be greater than zero.");
    }

    /// <summary>Ensure visit end is after start.</summary>
    private static void ValidateVisitWindow(DateTime start, DateTime end)
    {
        // Ensure visit end is after start.
        start = EnsureUtc(start);
        end = EnsureUtc(end);
        if (end <= start)
            throw new InvalidOperationException("SlotEnd must be after SlotStart.");
    }

    /// <summary>
    /// Visit window must fall on a working day and inside open/close hours
    /// (station wall-clock time in Asia/Colombo / Sri Lanka Standard Time).
    /// </summary>
    private static void ValidateAgainstStationSchedule(
        SolarStation station,
        DateTime slotStartUtc,
        DateTime slotEndUtc)
    {
        // Ensure visit fits station working hours.
        var schedule = station.Schedule ?? new StationSchedule();
        if (!TimeOnly.TryParse(schedule.OpenTime, out var open))
            throw new InvalidOperationException("Station open time is invalid.");
        if (!TimeOnly.TryParse(schedule.CloseTime, out var close))
            throw new InvalidOperationException("Station close time is invalid.");
        if (open >= close)
            throw new InvalidOperationException("Station schedule is invalid (open must be before close).");

        var workingDays = (schedule.WorkingDays ?? [])
            .Where(d => !string.IsNullOrWhiteSpace(d))
            .Select(d => d.Trim())
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        if (workingDays.Count == 0)
            throw new InvalidOperationException("Station has no working days configured.");

        var zone = ResolveStationTimeZone();
        var startLocal = TimeZoneInfo.ConvertTimeFromUtc(EnsureUtc(slotStartUtc), zone);
        var endLocal = TimeZoneInfo.ConvertTimeFromUtc(EnsureUtc(slotEndUtc), zone);

        if (startLocal.Date != endLocal.Date)
        {
            throw new InvalidOperationException(
                "Visit window must stay on a single station working day " +
                $"(hours {schedule.OpenTime}–{schedule.CloseTime}).");
        }

        var dayKey = ToDayAbbreviation(startLocal.DayOfWeek);
        if (!workingDays.Contains(dayKey))
        {
            var days = string.Join(", ", workingDays.OrderBy(DaySortKey));
            throw new InvalidOperationException(
                $"Station is closed on {dayKey}. Working days: {days}.");
        }

        var startTime = TimeOnly.FromDateTime(startLocal);
        var endTime = TimeOnly.FromDateTime(endLocal);

        if (startTime < open || endTime > close)
        {
            throw new InvalidOperationException(
                $"Visit must be within station hours {schedule.OpenTime}–{schedule.CloseTime} " +
                $"(requested {startTime:HH:mm}–{endTime:HH:mm}).");
        }
    }

    /// <summary>Resolve Asia/Colombo (or Sri Lanka) time zone.</summary>
    private static TimeZoneInfo ResolveStationTimeZone()
    {
        // Resolve Asia/Colombo (or Sri Lanka) time zone.
        foreach (var id in new[] { "Asia/Colombo", "Sri Lanka Standard Time" })
        {
            try
            {
                return TimeZoneInfo.FindSystemTimeZoneById(id);
            }
            catch (TimeZoneNotFoundException)
            {
            }
            catch (InvalidTimeZoneException)
            {
            }
        }

        // Fallback: fixed UTC+05:30 (Sri Lanka).
        return TimeZoneInfo.CreateCustomTimeZone(
            "GridSync-Colombo",
            TimeSpan.FromHours(5.5),
            "Sri Lanka",
            "Sri Lanka");
    }

    /// <summary>Map DayOfWeek to Mon..Sun abbreviation.</summary>
    private static string ToDayAbbreviation(DayOfWeek day) => day switch
    {
        DayOfWeek.Monday => "Mon",
        DayOfWeek.Tuesday => "Tue",
        DayOfWeek.Wednesday => "Wed",
        DayOfWeek.Thursday => "Thu",
        DayOfWeek.Friday => "Fri",
        DayOfWeek.Saturday => "Sat",
        DayOfWeek.Sunday => "Sun",
        _ => day.ToString()[..3],
    };

    /// <summary>Sort key for working-day abbreviations.</summary>
    private static int DaySortKey(string day) => day.ToLowerInvariant() switch
    {
        "mon" => 1,
        "tue" => 2,
        "wed" => 3,
        "thu" => 4,
        "fri" => 5,
        "sat" => 6,
        "sun" => 7,
        _ => 99,
    };

    /// <summary>Free space still available to charge into the battery.</summary>
    private static double AvailableCharging(EnergyBookingSlot s) =>
        Math.Max(s.CapacityKwh - s.ActualEnergyKwh - s.ReservedChargingKwh, 0);

    /// <summary>Stored energy available to drop off / withdraw (excludes pending drop-off reserves).</summary>
    private static double AvailableDropOff(EnergyBookingSlot s) =>
        Math.Max(s.ActualEnergyKwh - s.ReservedDropOffKwh, 0);

    /// <summary>Treat DateTime as UTC.</summary>
    private static DateTime EnsureUtc(DateTime value) =>
        value.Kind == DateTimeKind.Utc ? value : DateTime.SpecifyKind(value, DateTimeKind.Utc);

    /// <summary>Generate a unique RSV-yyyyMMdd-###### code.</summary>
    private static string GenerateReservationCode() =>
        $"RSV-{DateTime.UtcNow:yyyyMMdd}-{RandomNumberGenerator.GetInt32(100000, 1000000)}";

    /// <summary>Build a one-time QR payload for an approved reservation.</summary>
    private static string GenerateQrPayload(EnergyReservation reservation)
    {
        // Build a one-time QR payload for an approved reservation.
        var token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(32))
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
        return $"gridsync:v1:{reservation.Id}:{token}";
    }
}
