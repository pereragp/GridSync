// -------------------------------------------------------------
// File: ReservationsController.cs
// Project: GridSync.Api
// Description: HTTP endpoints for energy reservation lifecycle.
// -------------------------------------------------------------

using System.Security.Claims;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using GridSync.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GridSync.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ReservationsController : ControllerBase
{
    private readonly ReservationService _reservationService;

    public ReservationsController(ReservationService reservationService)
    {
        // Inject reservation business logic.
        _reservationService = reservationService;
    }

    /// <summary>
    /// GET /api/reservations/{id}
    /// </summary>
    [HttpGet("{id:length(24)}")]
    public async Task<IActionResult> GetById(string id)
    {
        // Return one reservation for prosumer or operator.
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(userId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        var staffAccess = User.IsInRole(UserRoles.GridOperator);
        try
        {
            var reservation = await _reservationService.GetByIdAsync(userId, id, staffAccess);
            return Ok(reservation);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// GET /api/reservations/history
    /// </summary>
    [HttpGet("history")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> GetHistory([FromQuery] string? status)
    {
        // List past reservations for the signed-in prosumer.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        var reservations = await _reservationService.GetHistoryAsync(prosumerId, status);
        return Ok(reservations);
    }

    /// <summary>
    /// GET /api/reservations/upcoming
    /// </summary>
    [HttpGet("upcoming")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> GetUpcoming([FromQuery] string? status)
    {
        // List upcoming reservations for the signed-in prosumer.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        var reservations = await _reservationService.GetUpcomingAsync(prosumerId, status);
        return Ok(reservations);
    }

    /// <summary>
    /// GET /api/reservations/manage?status=Pending
    /// </summary>
    [HttpGet("manage")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Manage([FromQuery] string? status)
    {
        // Operator view of all reservations.
        var reservations = await _reservationService.GetForStaffAsync(status);
        return Ok(reservations);
    }

    /// <summary>
    /// GET /api/reservations/slots — available slots for a new prosumer reservation.
    /// </summary>
    [HttpGet("slots")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> GetAvailableSlots()
    {
        // Batteries open for new prosumer bookings.
        var slots = await _reservationService.GetAvailableSlotsAsync();
        return Ok(slots);
    }

    /// <summary>
    /// POST /api/reservations
    /// </summary>
    [HttpPost]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> Create([FromBody] CreateReservationRequest request)
    {
        // Create a pending reservation for the authenticated prosumer.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        try
        {
            var reservation = await _reservationService.CreateAsync(prosumerId, request);
            return Created($"/api/reservations/{reservation.Id}", reservation);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// PUT /api/reservations/{id}
    /// </summary>
    [HttpPut("{id}")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationRequest request)
    {
        // Update owned pending reservation details.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        try
        {
            var reservation = await _reservationService.UpdateAsync(prosumerId, id, request);
            return Ok(reservation);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// POST /api/reservations/{id}/cancel
    /// </summary>
    [HttpPost("{id}/cancel")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationRequest? request)
    {
        // Cancel owned reservation with optional reason.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        try
        {
            var reservation = await _reservationService.CancelAsync(
                prosumerId,
                id,
                request ?? new CancelReservationRequest());
            return Ok(reservation);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// POST /api/reservations/{id}/approve
    /// </summary>
    [HttpPost("{id}/approve")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Approve(string id)
    {
        // Approve pending reservation via shared review handler.
        return await Review(id, true, new ReviewReservationRequest());
    }

    /// <summary>
    /// POST /api/reservations/{id}/reject
    /// </summary>
    [HttpPost("{id}/reject")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Reject(string id, [FromBody] ReviewReservationRequest request)
    {
        // Reject pending reservation via shared review handler.
        return await Review(id, false, request);
    }

    /// <summary>
    /// POST /api/reservations/verify-qr
    /// </summary>
    [HttpPost("verify-qr")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> VerifyQr([FromBody] VerifyReservationQrRequest request)
    {
        // Validate scanned QR before station transfer.
        try
        {
            var result = await _reservationService.VerifyQrAsync(request.QrPayload);
            return Ok(result);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// POST /api/reservations/{id}/complete
    /// </summary>
    [HttpPost("{id}/complete")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Complete(string id)
    {
        // Complete approved reservation at the station.
        var operatorId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(operatorId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        try
        {
            return Ok(await _reservationService.CompleteAsync(operatorId, id));
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>GET /api/reservations/dashboard-stats — operator operational counts.</summary>
    [HttpGet("dashboard-stats")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> DashboardStats()
    {
        // Operator reservation status counts.
        return Ok(await _reservationService.GetDashboardStatsAsync());
    }

    /// <summary>GET /api/reservations/prosumer-dashboard — pending and active counts.</summary>
    [HttpGet("prosumer-dashboard")]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> ProsumerDashboard()
    {
        // Prosumer pending and active counts.
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
            return Unauthorized(new { message = "Authenticated user id is missing." });

        return Ok(await _reservationService.GetProsumerDashboardStatsAsync(prosumerId));
    }

    /// <summary>GET /api/reservations/search?status=&amp;stationId=&amp;from=&amp;to=&amp;q=</summary>
    [HttpGet("search")]
    public async Task<IActionResult> Search(
        [FromQuery] string? status,
        [FromQuery] string? stationId,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to,
        [FromQuery] string? q)
    {
        // Search reservations with role-scoped filters.
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized(new { message = "Authenticated user id is missing." });

        var staffAccess = User.IsInRole(UserRoles.GridOperator);
        try
        {
            var results = await _reservationService.SearchAsync(
                userId, staffAccess, status, stationId, from, to, q);
            return Ok(results);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    private async Task<IActionResult> Review(string id, bool approve, ReviewReservationRequest request)
    {
        // Approve or reject a pending reservation as staff.
        var staffId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(staffId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        try
        {
            var reservation = await _reservationService.ReviewAsync(staffId, id, approve, request);
            return Ok(reservation);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
