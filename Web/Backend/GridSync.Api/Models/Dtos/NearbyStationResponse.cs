namespace GridSync.Api.Models.Dtos;

/// <summary>Station with distance from a map query point.</summary>
public class NearbyStationResponse : StationResponse
{
    /// <summary>Distance from the query coordinates in kilometres.</summary>
    public double DistanceKm { get; set; }
}
