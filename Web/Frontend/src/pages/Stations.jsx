import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import AlertMessage from "../components/AlertMessage";
import PageBleedHero from "../components/PageBleedHero";
import { deactivateStation, getStations, reactivateStation } from "../api/stations";
import { useAuth } from "../context/AuthContext";
import { useFeedback } from "../context/FeedbackContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=2000&q=80";

const STATUS_FILTERS = [
  { value: "All", label: "All" },
  { value: "Active", label: "Active" },
  { value: "Inactive", label: "Inactive" },
];

export default function Stations() {
  const { user } = useAuth();
  const { notify, confirm } = useFeedback();
  const isBackoffice = user.role === "Backoffice";
  const isOperator = user.role === "GridOperator";
  const consoleLabel = isBackoffice
    ? "Backoffice console"
    : isOperator
      ? "Grid Operator"
      : "Prosumer portal";
  const consoleBlurb = isBackoffice
    ? "Register solar microgrid nodes, set capacity, and maintain operational schedules across the network."
    : isOperator
      ? "View station schedules and update battery availability — close or reopen batteries for trading."
      : "Browse hubs and battery availability before booking Charging or Drop-off energy.";
  const pageTitle = isOperator
    ? "Battery stations"
    : isBackoffice
      ? "Microgrid nodes"
      : "Solar stations";
  const listSubtitle = isOperator
    ? "Open a station to view its schedule and manage battery availability."
    : isBackoffice
      ? "Create hubs, edit capacity and location, update schedules, or deactivate nodes when needed."
      : "Search by name, code, or description. Open a station to view details.";
  const openLabel = isOperator
    ? "Manage batteries"
    : isBackoffice
      ? "Manage node"
      : "Open";

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
      0,
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
    <div>
      <PageBleedHero
        image={HERO_IMAGE}
        imageAlt="Solar panels in a microgrid field"
        eyebrow={consoleLabel}
        title={pageTitle}
        subtitle={consoleBlurb}
        actions={
          isBackoffice ? (
            <Link
              to="/stations/new"
              className="inline-flex items-center justify-center rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-50"
            >
              Create station
            </Link>
          ) : null
        }
      />

      <div className="space-y-6">
        <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label="Total stations" value={stats.total} />
          <StatCard label="Active" value={stats.active} tone="grid" />
          <StatCard label="Inactive" value={stats.inactive} />
          <StatCard label="Battery slots" value={stats.totalSlots} tone="amber" />
        </section>

        {error ? (
          <AlertMessage
            type="error"
            title="Could not load stations"
            onDismiss={() => setError("")}
          >
            {error}
          </AlertMessage>
        ) : null}

        <section className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                Filter stations
              </p>
              <p className="mt-0.5 text-sm text-slate-600">{listSubtitle}</p>
            </div>
            <button
              type="button"
              onClick={load}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-sm font-semibold text-slate-700 transition hover:border-grid-300 hover:bg-grid-50 hover:text-grid-800 disabled:opacity-60"
            >
              <RefreshIcon spinning={loading} />
              Refresh
            </button>
          </div>

          <div className="mt-4 flex gap-2 overflow-x-auto pb-1">
            {STATUS_FILTERS.map((filter) => {
              const active = statusFilter === filter.value;
              return (
                <button
                  key={filter.value}
                  type="button"
                  onClick={() => setStatusFilter(filter.value)}
                  className={`shrink-0 rounded-xl px-3.5 py-2 text-sm font-semibold transition ${
                    active
                      ? "bg-grid-700 text-white shadow-sm shadow-grid-900/10"
                      : "bg-slate-100 text-slate-600 hover:bg-slate-200/80 hover:text-slate-800"
                  }`}
                >
                  {filter.label}
                </button>
              );
            })}
          </div>

          <div className="relative mt-4">
            <span className="pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-slate-400">
              <SearchIcon />
            </span>
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by name, code, or description…"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-2.5 pl-10 pr-3.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-grid-500 focus:bg-white focus:ring-2 focus:ring-grid-500/15"
            />
          </div>
        </section>

        <section>
          <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
            <div>
              <h2 className="font-display text-xl font-semibold text-grid-900">
                {isOperator
                  ? "Your stations"
                  : isBackoffice
                    ? "Registered nodes"
                    : "All stations"}
              </h2>
              <p className="mt-0.5 text-sm text-slate-500">
                {loading
                  ? "Loading…"
                  : `${filtered.length} of ${stations.length} station${
                      stations.length === 1 ? "" : "s"
                    }`}
                {query.trim() ? ` matching “${query.trim()}”` : ""}
              </p>
            </div>
          </div>

          {loading ? (
            <div className="grid gap-3 sm:grid-cols-2">
              {[0, 1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="h-44 animate-pulse rounded-2xl border border-slate-100 bg-slate-100/80"
                />
              ))}
            </div>
          ) : null}

          {!loading && filtered.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-14 text-center shadow-sm">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-grid-50 text-grid-700">
                <StationIcon />
              </div>
              <p className="mt-4 text-base font-semibold text-grid-900">
                No stations match
              </p>
              <p className="mx-auto mt-1 max-w-sm text-sm text-slate-500">
                {query.trim()
                  ? "Try another search, or clear filters to see the full list."
                  : isBackoffice
                    ? "Register a microgrid hub to get started."
                    : "No stations are available for this filter yet."}
              </p>
              <div className="mt-4 flex flex-wrap justify-center gap-3">
                {query.trim() || statusFilter !== "All" ? (
                  <button
                    type="button"
                    onClick={() => {
                      setQuery("");
                      setStatusFilter("All");
                    }}
                    className="text-sm font-semibold text-grid-700 hover:underline"
                  >
                    Clear filters
                  </button>
                ) : null}
                {isBackoffice ? (
                  <Link
                    to="/stations/new"
                    className="text-sm font-semibold text-grid-700 hover:underline"
                  >
                    Create station
                  </Link>
                ) : null}
              </div>
            </div>
          ) : null}

          {!loading && filtered.length > 0 ? (
            <ul className="grid gap-3 sm:grid-cols-2">
              {filtered.map((station) => (
                <li key={station.id}>
                  <StationCard
                    station={station}
                    openLabel={openLabel}
                    busy={busyId === station.id}
                    isBackoffice={isBackoffice}
                    onDeactivate={() => onDeactivate(station)}
                    onReactivate={() => onReactivate(station)}
                  />
                </li>
              ))}
            </ul>
          ) : null}
        </section>
      </div>
    </div>
  );
}

function StationCard({
  station,
  openLabel,
  busy,
  isBackoffice,
  onDeactivate,
  onReactivate,
}) {
  const active = station.status === "Active";

  return (
    <article className="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm transition hover:border-grid-200 hover:shadow-md hover:shadow-grid-900/5">
      <div className="flex flex-1 gap-0">
        <div
          className={`w-1 shrink-0 ${active ? "bg-grid-500" : "bg-slate-300"}`}
        />
        <div className="flex min-w-0 flex-1 flex-col p-4 sm:p-5">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-wide text-grid-700">
                {station.stationCode}
              </p>
              <h3 className="mt-0.5 truncate font-semibold tracking-tight text-slate-900">
                {station.name}
              </h3>
            </div>
            <StatusBadge status={station.status} />
          </div>

          {station.description ? (
            <p className="mt-2 line-clamp-2 text-sm text-slate-500">
              {station.description}
            </p>
          ) : null}

          <div className="mt-3 grid grid-cols-2 gap-2">
            <Meta
              label="Total capacity"
              value={`${station.totalCapacityKwh ?? "—"} kWh`}
            />
            <Meta
              label="Per battery"
              value={`${station.batteryCapacityKwh ?? "—"} kWh`}
            />
            <Meta
              label="Battery slots"
              value={String(station.availableBatterySlots ?? "—")}
            />
            <Meta
              label="Hours"
              value={`${station.schedule?.openTime || "—"}–${station.schedule?.closeTime || "—"}`}
            />
          </div>

          <p className="mt-3 text-xs text-slate-500">
            <span className="font-medium text-slate-600">Days</span>
            <span className="mx-1.5 text-slate-300">·</span>
            {(station.schedule?.workingDays || []).join(", ") || "—"}
          </p>

          <div className="mt-auto flex flex-wrap gap-2 border-t border-slate-100 pt-4">
            <Link
              to={`/stations/${station.id}`}
              className="inline-flex items-center justify-center rounded-xl bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800"
            >
              {openLabel}
            </Link>
            {isBackoffice && active ? (
              <button
                type="button"
                disabled={busy}
                onClick={onDeactivate}
                className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
              >
                {busy ? "Working…" : "Deactivate"}
              </button>
            ) : null}
            {isBackoffice && !active ? (
              <button
                type="button"
                disabled={busy}
                onClick={onReactivate}
                className="inline-flex items-center justify-center rounded-xl border border-grid-200 bg-grid-50 px-4 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-100 disabled:opacity-60"
              >
                {busy ? "Working…" : "Reactivate"}
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </article>
  );
}

function Meta({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className="mt-0.5 truncate text-sm font-medium text-slate-900">
        {value}
      </p>
    </div>
  );
}

function StatCard({ label, value, tone = "default" }) {
  const tones = {
    default: "border-slate-200 bg-white",
    grid: "border-grid-200/80 bg-grid-50/80",
    amber: "border-amber-200/80 bg-amber-50/80",
  };
  const values = {
    default: "text-grid-900",
    grid: "text-grid-900",
    amber: "text-amber-950",
  };
  return (
    <div className={`rounded-2xl border p-4 shadow-sm ${tones[tone]}`}>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p
        className={`mt-2 text-3xl font-semibold tracking-tight ${values[tone]}`}
      >
        {loadingSafe(value)}
      </p>
    </div>
  );
}

function loadingSafe(value) {
  return value ?? "…";
}

function StatusBadge({ status }) {
  const map = {
    Active: "bg-grid-100 text-grid-800 ring-1 ring-grid-200",
    Inactive: "bg-slate-200 text-slate-700 ring-1 ring-slate-300/80",
  };
  return (
    <span
      className={`inline-flex shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
        map[status] || "bg-slate-100 text-slate-700"
      }`}
    >
      {status}
    </span>
  );
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z"
        clipRule="evenodd"
      />
    </svg>
  );
}

function RefreshIcon({ spinning }) {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="currentColor"
      className={`h-4 w-4 ${spinning ? "animate-spin" : ""}`}
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H4.39a.75.75 0 00-.75.75v3.842a.75.75 0 001.5 0v-2.26l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.388zm-10.624-2.85a5.5 5.5 0 019.201-2.466l.312.311H11.77a.75.75 0 000 1.5h3.842a.75.75 0 00.75-.75V3.328a.75.75 0 00-1.5 0V5.59l-.31-.31A7 7 0 003.838 8.416a.75.75 0 001.45.388z"
        clipRule="evenodd"
      />
    </svg>
  );
}

function StationIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5" aria-hidden>
      <path d="M3.5 3A1.5 1.5 0 002 4.5v3A1.5 1.5 0 003.5 9h3A1.5 1.5 0 008 7.5v-3A1.5 1.5 0 006.5 3h-3zM12.5 3A1.5 1.5 0 0011 4.5v3A1.5 1.5 0 0012.5 9h3A1.5 1.5 0 0017 7.5v-3A1.5 1.5 0 0015.5 3h-3zM3.5 11A1.5 1.5 0 002 12.5v3A1.5 1.5 0 003.5 17h3A1.5 1.5 0 008 15.5v-3A1.5 1.5 0 006.5 11h-3zM12.5 11a1.5 1.5 0 00-1.5 1.5v3a1.5 1.5 0 001.5 1.5h3a1.5 1.5 0 001.5-1.5v-3a1.5 1.5 0 00-1.5-1.5h-3z" />
    </svg>
  );
}
