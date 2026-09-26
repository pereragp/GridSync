namespace GridSync.Api.Models.Dtos;

public class ReservationDashboardStatsResponse
{
    public long PendingReservations { get; set; }
    public long ApprovedUpcomingReservations { get; set; }
    public long CompletedTransfers { get; set; }
    public long RejectedReservations { get; set; }
    public long CancelledReservations { get; set; }
    public long ExpiredReservations { get; set; }
}
