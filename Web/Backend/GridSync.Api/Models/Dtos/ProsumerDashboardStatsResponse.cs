namespace GridSync.Api.Models.Dtos;

/// <summary>Pending and active booking counts for a prosumer.</summary>
public class ProsumerDashboardStatsResponse
{
    public long PendingReservations { get; set; }
    public long ActiveReservations { get; set; }
}
