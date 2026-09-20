// -------------------------------------------------------------
// File: AuthController.cs
// Project: GridSync.Api
// Description: HTTP endpoints for authentication (thin controller).
// -------------------------------------------------------------

using GridSync.Api.Models.Dtos;
using GridSync.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace GridSync.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly AuthService _authService;

    public AuthController(AuthService authService)
    {
        // Inject auth service (business logic stays in service).
        _authService = authService;
    }

    /// <summary>
    /// POST /api/auth/login
    /// </summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        // Delegate credential checks to the FAT service layer.
        try
        {
            var result = await _authService.LoginAsync(request);
            return Ok(result);
        }
        catch (InvalidOperationException ex)
        {
            return Unauthorized(new { message = ex.Message });
        }
    }
}
