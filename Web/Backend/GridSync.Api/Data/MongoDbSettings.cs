// -------------------------------------------------------------
// File: MongoDbSettings.cs
// Project: GridSync.Api
// Description: Configuration for MongoDB connection and database name.
// -------------------------------------------------------------

namespace GridSync.Api.Data;

public class MongoDbSettings
{
    public string ConnectionString { get; set; } = string.Empty;
    public string DatabaseName { get; set; } = string.Empty;
}