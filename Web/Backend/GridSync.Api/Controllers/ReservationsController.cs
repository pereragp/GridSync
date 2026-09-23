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
        _reservationService = reservationService;
    }

    /// <summary>
    /// GET /api/reservations/{id}
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(string id)
    {
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(userId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        var staffAccess = User.IsInRole(UserRoles.Backoffice) || User.IsInRole(UserRoles.GridOperator);
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
        var prosumerId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(prosumerId))
        {
            return Unauthorized(new { message = "Authenticated user id is missing." });
        }

        var reservations = await _reservationService.GetHistoryAsync(prosumerId, status);
        return Ok(reservations);
    }

    /// <summary>
    /// GET /api/reservations/manage?status=Pending
    /// </summary>
    [HttpGet("manage")]
    [Authorize(Roles = UserRoles.Backoffice + "," + UserRoles.GridOperator)]
    public async Task<IActionResult> Manage([FromQuery] string? status)
    {
        var reservations = await _reservationService.GetForStaffAsync(status);
        return Ok(reservations);
    }

    /// <summary>
    /// POST /api/reservations
    /// </summary>
    [HttpPost]
    [Authorize(Roles = UserRoles.Prosumer)]
    public async Task<IActionResult> Create([FromBody] CreateReservationRequest request)
    {
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
    [Authorize(Roles = UserRoles.Backoffice + "," + UserRoles.GridOperator)]
    public async Task<IActionResult> Approve(string id)
    {
        return await Review(id, true, new ReviewReservationRequest());
    }

    /// <summary>
    /// POST /api/reservations/{id}/reject
    /// </summary>
    [HttpPost("{id}/reject")]
    [Authorize(Roles = UserRoles.Backoffice + "," + UserRoles.GridOperator)]
    public async Task<IActionResult> Reject(string id, [FromBody] ReviewReservationRequest request)
    {
        return await Review(id, false, request);
    }

    private async Task<IActionResult> Review(string id, bool approve, ReviewReservationRequest request)
    {
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
