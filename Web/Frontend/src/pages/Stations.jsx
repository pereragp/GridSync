import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import AlertMessage from "../components/AlertMessage";
import { deactivateStation, getStations, reactivateStation } from "../api/stations";
import { useAuth } from "../context/AuthContext";
import { useFeedback } from "../context/FeedbackContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=1800&q=80";

export default function Stations() {
  const { user } = useAuth();
  const { notify, confirm } = useFeedback();
  const isBackoffice = user.role === "Backoffice";

  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("All");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const data = await getStations();
      setStations(data || []);
    } catch (err) {
      setError(err.message || "Failed to load stations");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const stats = useMemo(() => {
    const active = stations.filter((s) => s.status === "Active").length;
    const inactive = stations.filter((s) => s.status === "Inactive").length;
    const totalSlots = stations.reduce(
      (sum, s) => sum + (s.availableBatterySlots || 0),
      0
    );
    return { total: stations.length, active, inactive, totalSlots };
  }, [stations]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return stations.filter((s) => {
      if (statusFilter !== "All" && s.status !== statusFilter) return false;
      if (!q) return true;
      return (
        s.name?.toLowerCase().includes(q) ||
        s.stationCode?.toLowerCase().includes(q) ||
        s.description?.toLowerCase().includes(q)
      );
    });
  }, [stations, query, statusFilter]);

  async function onDeactivate(station) {
    const ok = await confirm({
      title: `Deactivate ${station.name}?`,
      message:
        "Deactivation is blocked if active energy reservations exist on this node. Resolve those first if needed.",
      confirmLabel: "Deactivate",
      tone: "danger",
    });
    if (!ok) return;

    setError("");
    setBusyId(station.id);
    try {
      await deactivateStation(station.id);
      notify.success(`Deactivated ${station.name}`);
      await load();
    } catch (err) {
      notify.error(err.message || "Deactivate failed");
    } finally {
      setBusyId("");
    }
  }

  async function onReactivate(station) {
    const ok = await confirm({
      title: `Reactivate ${station.name}?`,
      message:
        "This node will become Active again and available for schedules and bookings.",
      confirmLabel: "Reactivate",
    });
    if (!ok) return;

    setError("");
    setBusyId(station.id);
    try {
      await reactivateStation(station.id);
      notify.success(`Reactivated ${station.name}`);
      await load();
    } catch (err) {
      notify.error(err.message || "Reactivate failed");
    } finally {
      setBusyId("");
    }
  }

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Solar panels in a microgrid field"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/45" />
        </div>

        <div className="relative z-10 flex flex-col gap-6 px-6 py-10 sm:px-10 sm:py-12 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
              {isBackoffice ? "Backoffice console" : "Operator console"}
            </p>
            <h1 className="mt-3 font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
              Solar stations
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
              {isBackoffice
                ? "Register microgrid hubs, update capacity, and deactivate nodes across the GridSync network."
                : "Monitor station capacity and update operating schedules and battery slots."}
            </p>
          </div>

          {isBackoffice ? (
            <Link
              to="/stations/new"
              className="inline-flex items-center justify-center rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 shadow-lg transition hover:bg-grid-50"
            >
              Create station
            </Link>
          ) : null}
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total stations" value={stats.total} />
        <StatCard label="Active" value={stats.active} />
        <StatCard label="Inactive" value={stats.inactive} />
        <StatCard label="Battery slots" value={stats.totalSlots} accent />
      </section>

      {error ? (
        <AlertMessage type="error" title="Could not load stations" onDismiss={() => setError("")}>
          {error}
        </AlertMessage>
      ) : null}

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-5 py-4">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-grid-900">All stations</h2>
              <p className="text-sm text-slate-600">
                Search by name, code, or description. Open a station to edit details or schedule.
              </p>
            </div>
            <p className="text-sm text-slate-500">
              Showing{" "}
              <span className="font-semibold text-grid-800">{filtered.length}</span> of{" "}
              {stations.length}
            </p>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <input
              type="search"
              placeholder="Search name, code, or description"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
            />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
            >
              <option value="All">All statuses</option>
              <option value="Active">Active</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>
        </div>

        <div className="p-5">
          {loading ? (
            <p className="text-sm text-slate-500">Loading stations…</p>
          ) : filtered.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
              No stations match your filters.
              {isBackoffice ? (
                <>
                  {" "}
                  <Link to="/stations/new" className="font-semibold text-grid-700 hover:underline">
                    Create one
                  </Link>
                </>
              ) : null}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <th className="pb-3 pr-4 font-semibold">Station</th>
                    <th className="pb-3 pr-4 font-semibold">Capacity</th>
                    <th className="pb-3 pr-4 font-semibold">Slots</th>
                    <th className="pb-3 pr-4 font-semibold">Schedule</th>
                    <th className="pb-3 pr-4 font-semibold">Status</th>
                    <th className="pb-3 font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((s) => (
                    <tr key={s.id} className="border-b border-slate-100 align-top last:border-0">
                      <td className="py-3.5 pr-4">
                        <p className="font-medium text-slate-900">{s.name}</p>
                        <p className="text-xs font-medium text-grid-700">{s.stationCode}</p>
                        {s.description ? (
                          <p className="mt-0.5 line-clamp-1 text-xs text-slate-500">
                            {s.description}
                          </p>
                        ) : null}
                      </td>
                      <td className="py-3.5 pr-4 text-slate-700">
                        <p>{s.totalCapacityKwh} kWh total</p>
                        <p className="text-xs text-slate-500">
                          {s.batteryCapacityKwh} kWh per battery
                        </p>
                      </td>
                      <td className="py-3.5 pr-4 text-slate-700">
                        {s.availableBatterySlots}
                      </td>
                      <td className="py-3.5 pr-4 text-slate-700">
                        <p>
                          {s.schedule?.openTime} – {s.schedule?.closeTime}
                        </p>
                        <p className="text-xs text-slate-500">
                          {(s.schedule?.workingDays || []).join(", ") || "—"}
                        </p>
                      </td>
                      <td className="py-3.5 pr-4">
                        <StatusBadge status={s.status} />
                      </td>
                      <td className="py-3.5">
                        <div className="flex flex-wrap gap-2">
                          <Link
                            to={`/stations/${s.id}`}
                            className="rounded-lg bg-grid-700 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-grid-800"
                          >
                            Open
                          </Link>
                          {isBackoffice && s.status === "Active" ? (
                            <button
                              type="button"
                              disabled={busyId === s.id}
                              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
                              onClick={() => onDeactivate(s)}
                            >
                              {busyId === s.id ? "Working…" : "Deactivate"}
                            </button>
                          ) : null}
                          {isBackoffice && s.status === "Inactive" ? (
                            <button
                              type="button"
                              disabled={busyId === s.id}
                              className="rounded-lg border border-grid-300 bg-grid-50 px-3 py-1.5 text-sm font-medium text-grid-800 transition hover:bg-grid-100 disabled:opacity-60"
                              onClick={() => onReactivate(s)}
                            >
                              {busyId === s.id ? "Working…" : "Reactivate"}
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value, accent = false }) {
  return (
    <div
      className={`rounded-2xl border p-4 shadow-sm ${
        accent ? "border-amber-200 bg-amber-50" : "border-slate-200 bg-white"
      }`}
    >
      <p
        className={`text-xs font-semibold uppercase tracking-wide ${
          accent ? "text-amber-800" : "text-slate-500"
        }`}
      >
        {label}
      </p>
      <p
        className={`mt-2 text-3xl font-semibold tracking-tight ${
          accent ? "text-amber-950" : "text-grid-900"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function StatusBadge({ status }) {
  const map = {
    Active: "bg-grid-100 text-grid-800",
    Inactive: "bg-slate-200 text-slate-700",
  };
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
        map[status] || "bg-slate-100"
      }`}
    >
      {status}
    </span>
  );
}
