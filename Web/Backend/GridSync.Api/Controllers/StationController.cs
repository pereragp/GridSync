// -------------------------------------------------------------
// File: StationsController.cs
// Project: GridSync.Api
// Description: HTTP endpoints for microgrid node management.
// -------------------------------------------------------------

using System.Security.Claims;
using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using GridSync.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GridSync.Api.Controllers;

[ApiController]             
[Route("api/[controller]")] // URL will be /api/stations 
[Authorize]                 
public class StationsController : ControllerBase 
{
    private readonly StationService _stationService;

    public StationsController(StationService stationService)
    {
        // ASP.NET injects the service here automatically
        _stationService = stationService;
    }

    /// <summary>
    /// POST /api/stations — Backoffice creates a new solar hub
    /// </summary>
    [HttpPost("create")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Create([FromBody] CreateStationRequest request)
    {
        var createdBy = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;

        try{
            var station = await _stationService.CreateAsync(request, createdBy);
            return CreatedAtAction(nameof(GetById), new { id = station.Id }, station);
        }
        catch (InvalidOperationException ex){
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// GET /api/stations 
    /// </summary>
    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var stations = await _stationService.GetAllAsync();
        return Ok(stations);
    }

    /// <summary>
    /// GET /api/stations/{id} — fetch a single station by id
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(string id)
    {
        try
        {
            var station = await _stationService.GetByIdAsync(id);
            return Ok(station);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    /// <summary>
    /// PUT /api/stations/{id} — Backoffice does a full update of station details.
    /// </summary>
    [HttpPut("{id}")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateStationRequest request)
    {
        try
        {
            var station = await _stationService.UpdateAsync(id, request);
            return Ok(station);
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
    /// PATCH /api/stations/{id}/schedule — Backoffice or GridOperator updates schedule + slots.
    /// </summary>
    [HttpPatch("{id}/schedule")]
    [Authorize(Roles = $"{UserRoles.Backoffice},{UserRoles.GridOperator}")]
    public async Task<IActionResult> UpdateSchedule(string id, [FromBody] UpdateStationScheduleRequest request)
    {
        try
        {
            var station = await _stationService.UpdateScheduleAsync(id, request);
            return Ok(station);
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
    /// POST /api/stations/{id}/deactivate — Backoffice deactivates a node.
    /// </summary>
    [HttpPost("{id}/deactivate")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Deactivate(string id)
    {
        try
        {
            var station = await _stationService.DeactivateAsync(id);
            return Ok(station);
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