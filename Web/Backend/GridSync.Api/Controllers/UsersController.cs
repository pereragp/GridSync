// -------------------------------------------------------------
// File: UsersController.cs
// Project: GridSync.Api
// Description: HTTP endpoints for staff/prosumer account management.
// -------------------------------------------------------------

using GridSync.Api.Models;
using GridSync.Api.Models.Dtos;
using GridSync.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GridSync.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UsersController : ControllerBase
{
    private readonly UserService _userService;

    public UsersController(UserService userService)
    {
        // Inject user service (all rules live in the service).
        _userService = userService;
    }

    /// <summary>
    /// POST /api/users/staff — create Backoffice or GridOperator.
    /// AllowAnonymous so the first admin can be seeded; lock this down later if needed.
    /// </summary>
    [HttpPost("staff")]
    [AllowAnonymous]
    public async Task<IActionResult> CreateStaff([FromBody] CreateStaffUserRequest request)
    {
        // Create staff account via service.
        try
        {
            var user = await _userService.CreateStaffUserAsync(request);
            return CreatedAtAction(nameof(GetById), new { id = user.Id }, user);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// POST /api/users/prosumers/register — prosumer self-registration.
    /// </summary>
    [HttpPost("prosumers/register")]
    [AllowAnonymous]
    public async Task<IActionResult> RegisterProsumer([FromBody] RegisterProsumerRequest request)
    {
        // Register prosumer as Pending.
        try
        {
            var user = await _userService.RegisterProsumerAsync(request);
            return CreatedAtAction(nameof(GetById), new { id = user.Id }, user);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// GET /api/users
    /// </summary>
    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        // Return all users.
        var users = await _userService.GetAllAsync();
        return Ok(users);
    }

    /// <summary>
    /// GET /api/users/pending — pending prosumer activations.
    /// </summary>
    [HttpGet("pending")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> GetPending()
    {
        // List pending prosumers for Backoffice.
        var users = await _userService.GetPendingAsync();
        return Ok(users);
    }

    /// <summary>
    /// GET /api/users/{id}
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(string id)
    {
        // Fetch one user by id.
        try
        {
            var user = await _userService.GetByIdAsync(id);
            return Ok(user);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    /// <summary>
    /// PUT /api/users/{id}
    /// </summary>
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateUserRequest request)
    {
        // Update profile fields.
        try
        {
            var user = await _userService.UpdateAsync(id, request);
            return Ok(user);
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
    /// POST /api/users/{id}/approve — approve pending prosumer.
    /// </summary>
    [HttpPost("{id}/approve")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Approve(string id)
    {
        // Backoffice approves pending activation.
        try
        {
            var user = await _userService.ApprovePendingAsync(id);
            return Ok(user);
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
    /// POST /api/users/{id}/request-deactivation
    /// </summary>
    [HttpPost("{id}/request-deactivation")]
    public async Task<IActionResult> RequestDeactivation(string id)
    {
        // Prosumer requests deactivation.
        try
        {
            var user = await _userService.RequestDeactivationAsync(id);
            return Ok(user);
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
    /// POST /api/users/{id}/deactivate
    /// </summary>
    [HttpPost("{id}/deactivate")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Deactivate(string id)
    {
        // Backoffice deactivates account.
        try
        {
            var user = await _userService.DeactivateAsync(id);
            return Ok(user);
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
    /// POST /api/users/{id}/reactivate?backofficeUserId=...
    /// </summary>
    [HttpPost("{id}/reactivate")]
    [Authorize(Roles = UserRoles.Backoffice)]
    public async Task<IActionResult> Reactivate(string id, [FromQuery] string backofficeUserId)
    {
        // Only Backoffice may reactivate (caller id passed for audit).
        if (string.IsNullOrWhiteSpace(backofficeUserId))
        {
            return BadRequest(new { message = "backofficeUserId query parameter is required." });
        }

        try
        {
            var user = await _userService.ReactivateAsync(id, backofficeUserId);
            return Ok(user);
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
