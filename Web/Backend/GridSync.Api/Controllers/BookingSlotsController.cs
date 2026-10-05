// -------------------------------------------------------------
// File: BookingSlotsController.cs
// Project: GridSync.Api
// Description: HTTP endpoints for physical battery slot management (UC07).
// -------------------------------------------------------------

using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using GridSync.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GridSync.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = UserRoles.Backoffice + "," + UserRoles.GridOperator)]
public class BookingSlotsController : ControllerBase
{
    private readonly BookingSlotService _bookingSlotService;

    public BookingSlotsController(BookingSlotService bookingSlotService)
    {
        // Inject battery slot service.
        _bookingSlotService = bookingSlotService;
    }

    /// <summary>GET /api/bookingslots?stationId=&amp;status=</summary>
    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? stationId, [FromQuery] string? status)
    {
        // List batteries with optional filters.
        try
        {
            return Ok(await _bookingSlotService.GetAllAsync(stationId, status));
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>GET /api/bookingslots/{id}</summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(string id)
    {
        // Return one battery by id.
        try
        {
            return Ok(await _bookingSlotService.GetByIdAsync(id));
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    /// <summary>PUT /api/bookingslots/{id} — update battery notes.</summary>
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateBookingSlotRequest request)
    {
        // Update battery notes.
        try
        {
            return Ok(await _bookingSlotService.UpdateAsync(id, request));
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

    /// <summary>POST /api/bookingslots/{id}/close — stop further bookings.</summary>
    [HttpPost("{id}/close")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Close(string id)
    {
        // Mark battery closed to new bookings.
        try
        {
            return Ok(await _bookingSlotService.CloseAsync(id));
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

    /// <summary>POST /api/bookingslots/{id}/reopen — allow bookings again.</summary>
    [HttpPost("{id}/reopen")]
    [Authorize(Roles = UserRoles.GridOperator)]
    public async Task<IActionResult> Reopen(string id)
    {
        // Reopen a closed battery.
        try
        {
            return Ok(await _bookingSlotService.ReopenAsync(id));
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

    /// <summary>DELETE /api/bookingslots/{id}</summary>
    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(string id)
    {
        // Delete battery when service rules allow.
        try
        {
            await _bookingSlotService.DeleteAsync(id);
            return NoContent();
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { message = ex.Message });
        }
    }
}
