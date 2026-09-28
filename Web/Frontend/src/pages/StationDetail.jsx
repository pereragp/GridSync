import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  closeBookingSlot,
  getBookingSlots,
  reopenBookingSlot,
} from "../api/bookingSlots";
import {
  deactivateStation,
  getStation,
  reactivateStation,
  updateStation,
  updateStationSchedule,
} from "../api/stations";
import AlertMessage from "../components/AlertMessage";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";
import { useFeedback } from "../context/FeedbackContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=1800&q=80";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

export default function StationDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const { notify, confirm } = useFeedback();
  const isBackoffice = user.role === "Backoffice";
  const canEditSchedule = isBackoffice;
  const canManageBatteries = user.role === "GridOperator";

  const [station, setStation] = useState(null);
  const [batteries, setBatteries] = useState([]);
  const [batteryBusyId, setBatteryBusyId] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingDetails, setSavingDetails] = useState(false);
  const [savingSchedule, setSavingSchedule] = useState(false);
  const [statusBusy, setStatusBusy] = useState(false);

  const [details, setDetails] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [activeTab, setActiveTab] = useState("details");

  const loadBatteries = useCallback(async () => {
    try {
      setBatteries((await getBookingSlots(id)) || []);
    } catch {
      setBatteries([]);
    }
  }, [id]);

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const data = await getStation(id);
      setStation(data);
      setDetails({
        name: data.name || "",
        description: data.description || "",
        latitude: String(data.latitude ?? ""),
        longitude: String(data.longitude ?? ""),
        batteryCapacityKwh: String(data.batteryCapacityKwh ?? ""),
        availableBatterySlots: String(data.availableBatterySlots ?? 0),
      });
      setSchedule({
        openTime: data.schedule?.openTime || "08:00",
        closeTime: data.schedule?.closeTime || "18:00",
        workingDays: [...(data.schedule?.workingDays || [])],
        availableBatterySlots: String(data.availableBatterySlots ?? 0),
      });
      await loadBatteries();
    } catch (err) {
      setError(err.message || "Failed to load station");
      setStation(null);
    } finally {
      setLoading(false);
    }
  }, [id, loadBatteries]);

  useEffect(() => {
    load();
  }, [load]);

  function setDetail(key, value) {
    setDetails((prev) => ({ ...prev, [key]: value }));
  }

  function setSched(key, value) {
    setSchedule((prev) => ({ ...prev, [key]: value }));
  }

  function toggleDay(day) {
    setSchedule((prev) => {
      const has = prev.workingDays.includes(day);
      return {
        ...prev,
        workingDays: has
          ? prev.workingDays.filter((d) => d !== day)
          : [...prev.workingDays, day],
      };
    });
  }

  async function onSaveDetails(e) {
    e.preventDefault();
    setError("");
    setSavingDetails(true);
    try {
      const updated = await updateStation(id, {
        name: details.name.trim(),
        description: details.description.trim() || null,
        latitude: Number(details.latitude),
        longitude: Number(details.longitude),
        batteryCapacityKwh: Number(details.batteryCapacityKwh),
        availableBatterySlots: Number(details.availableBatterySlots),
      });
      setStation(updated);
      setSchedule((prev) => ({
        ...prev,
        availableBatterySlots: String(updated.availableBatterySlots ?? 0),
      }));
      notify.success("Station details updated.");
      await loadBatteries();
    } catch (err) {
      notify.error(err.message || "Update failed");
    } finally {
      setSavingDetails(false);
    }
  }

  async function onSaveSchedule(e) {
    e.preventDefault();
    setError("");

    if (schedule.workingDays.length === 0) {
      notify.warning("Select at least one working day.");
      return;
    }

    setSavingSchedule(true);
    try {
      const updated = await updateStationSchedule(id, {
        openTime: schedule.openTime,
        closeTime: schedule.closeTime,
        workingDays: schedule.workingDays,
        availableBatterySlots: Number(schedule.availableBatterySlots),
      });
      setStation(updated);
      setDetails((prev) =>
        prev
          ? {
              ...prev,
              availableBatterySlots: String(updated.availableBatterySlots ?? 0),
            }
          : prev
      );
      notify.success("Schedule and battery slots updated.");
      await loadBatteries();
    } catch (err) {
      notify.error(err.message || "Schedule update failed");
    } finally {
      setSavingSchedule(false);
    }
  }

  async function onToggleBattery(battery) {
    setBatteryBusyId(battery.id);
    try {
      if (battery.status === "Closed") {
        await reopenBookingSlot(battery.id);
        notify.success(`Battery #${battery.batteryIndex} reopened.`);
      } else {
        await closeBookingSlot(battery.id);
        notify.success(`Battery #${battery.batteryIndex} closed.`);
      }
      await loadBatteries();
    } catch (err) {
      notify.error(err.message || "Battery update failed");
    } finally {
      setBatteryBusyId("");
    }
  }

  const batterySummary = useMemo(() => {
    return batteries.reduce(
      (acc, battery) => {
        acc.total += 1;
        if (battery.status === "Closed") acc.closed += 1;
        else acc.open += 1;
        acc.capacity += Number(battery.capacityKwh) || 0;
        acc.stored += Number(battery.actualEnergyKwh) || 0;
        acc.chargeAvail += Number(battery.availableChargingKwh) || 0;
        acc.dropAvail += Number(battery.availableDropOffKwh) || 0;
        return acc;
      },
      {
        total: 0,
        open: 0,
        closed: 0,
        capacity: 0,
        stored: 0,
        chargeAvail: 0,
        dropAvail: 0,
      },
    );
  }, [batteries]);

  async function onDeactivate() {
    const ok = await confirm({
      title: `Deactivate ${station?.name}?`,
      message:
        "Deactivation is blocked if active energy reservations exist on this node. Resolve those first if needed.",
      confirmLabel: "Deactivate",
      tone: "danger",
    });
    if (!ok) return;

    setError("");
    setStatusBusy(true);
    try {
      const updated = await deactivateStation(id);
      setStation(updated);
      notify.success(`Deactivated ${updated.name}`);
    } catch (err) {
      notify.error(err.message || "Deactivate failed");
    } finally {
      setStatusBusy(false);
    }
  }

  async function onReactivate() {
    const ok = await confirm({
      title: `Reactivate ${station?.name}?`,
      message:
        "This node will become Active again and available for schedules and bookings.",
      confirmLabel: "Reactivate",
    });
    if (!ok) return;

    setError("");
    setStatusBusy(true);
    try {
      const updated = await reactivateStation(id);
      setStation(updated);
      notify.success(`Reactivated ${updated.name}`);
    } catch (err) {
      notify.error(err.message || "Reactivate failed");
    } finally {
      setStatusBusy(false);
    }
  }

  if (loading) {
    return <p className="text-sm text-slate-500">Loading station…</p>;
  }

  if (!station || !details || !schedule) {
    return (
      <div className="space-y-4">
        <AlertMessage type="error" title="Station unavailable">
          {error || "Station not found."}
        </AlertMessage>
        <Link to="/stations" className="text-sm font-semibold text-grid-700 hover:underline">
          Back to stations
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Renewable energy landscape"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/40" />
        </div>

        <div className="relative z-10 px-6 py-8 sm:px-10 sm:py-10">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
            {station.stationCode}
          </p>
          <h1 className="mt-3 font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
            {station.name}
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
            {station.totalCapacityKwh} kWh total · {station.batteryCapacityKwh}{" "}
            kWh/battery · {station.availableBatterySlots} slots
          </p>
          <div className="mt-3">
            <StatusBadge status={station.status} light />
          </div>

          <nav
            className="mt-8 flex flex-wrap gap-1 rounded-xl bg-black/20 p-1.5 backdrop-blur-sm"
            aria-label="Station sections"
          >
            {STATION_TABS.map((tab) => {
              const selected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`rounded-lg px-3.5 py-2 text-sm font-semibold transition sm:px-4 ${
                    selected
                      ? "bg-white text-grid-900 shadow-sm"
                      : "text-white/80 hover:bg-white/10 hover:text-white"
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </nav>
        </div>
      </section>

      {error ? (
        <AlertMessage type="error" title="Something went wrong" onDismiss={() => setError("")}>
          {error}
        </AlertMessage>
      ) : null}

      {activeTab === "details" ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="mb-6">
            <h2 className="text-xl font-semibold tracking-tight text-grid-900">
              Station details
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {isBackoffice
                ? "Update name, location, and capacity for this microgrid node."
                : user.role === "GridOperator"
                  ? "Station identity and capacity are managed by Backoffice."
                  : "Core station information."}
            </p>
          </div>

          {isBackoffice ? (
            <form onSubmit={onSaveDetails} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-slate-700">Name</span>
                  <input
                    className={inputClass}
                    value={details.name}
                    onChange={(e) => setDetail("name", e.target.value)}
                    required
                  />
                </label>
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Description
                  </span>
                  <textarea
                    className={inputClass}
                    rows={2}
                    value={details.description}
                    onChange={(e) => setDetail("description", e.target.value)}
                  />
                </label>
                <div className="sm:col-span-2">
                  <p className="mb-1.5 text-sm font-medium text-slate-700">
                    Location on map
                  </p>
                  <LocationPicker
                    latitude={details.latitude}
                    longitude={details.longitude}
                    onChange={({ latitude, longitude }) => {
                      setDetails((prev) => ({
                        ...prev,
                        latitude,
                        longitude,
                      }));
                    }}
                  />
                </div>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Battery capacity (kWh per battery)
                  </span>
                  <input
                    className={inputClass}
                    type="number"
                    min="0"
                    step="any"
                    value={details.batteryCapacityKwh}
                    onChange={(e) => setDetail("batteryCapacityKwh", e.target.value)}
                    required
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Battery slots available
                  </span>
                  <input
                    className={inputClass}
                    type="number"
                    min="0"
                    step="1"
                    value={details.availableBatterySlots}
                    onChange={(e) =>
                      setDetail("availableBatterySlots", e.target.value)
                    }
                    required
                  />
                </label>
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Total capacity (kWh)
                  </span>
                  <input
                    className={`${inputClass} bg-slate-50 text-slate-600`}
                    type="text"
                    readOnly
                    value={
                      details.batteryCapacityKwh !== "" &&
                      details.availableBatterySlots !== ""
                        ? `${Number(details.availableBatterySlots) * Number(details.batteryCapacityKwh)} kWh`
                        : "—"
                    }
                  />
                  <span className="mt-1 block text-xs text-slate-500">
                    Calculated as battery slots × kWh per battery
                  </span>
                </label>
              </div>
              <button
                type="submit"
                disabled={savingDetails || station.status === "Inactive"}
                className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {savingDetails ? "Saving…" : "Save details"}
              </button>
            </form>
          ) : (
            <div className="space-y-4">
              <dl className="grid gap-4 text-sm sm:grid-cols-2">
                <Info label="Description" value={station.description || "—"} wide />
                <Info
                  label="Battery capacity (kWh per battery)"
                  value={station.batteryCapacityKwh}
                />
                <Info label="Battery slots" value={station.availableBatterySlots} />
                <Info label="Total capacity (kWh)" value={station.totalCapacityKwh} />
              </dl>
              <div>
                <p className="mb-1.5 text-sm font-medium text-slate-700">Location</p>
                <LocationPicker
                  latitude={station.latitude}
                  longitude={station.longitude}
                  readOnly
                  heightClass="h-56"
                />
              </div>
            </div>
          )}

          <div className="mt-8 grid gap-4 border-t border-slate-100 pt-6 sm:grid-cols-2">
            <div className="rounded-xl border border-grid-100 bg-grid-50/70 p-4">
              <p className="text-sm font-semibold text-grid-900">Metadata</p>
              <dl className="mt-3 space-y-2 text-xs text-slate-600">
                <div className="flex justify-between gap-2">
                  <dt>Created</dt>
                  <dd className="font-medium text-slate-800">
                    {formatDate(station.createdAt)}
                  </dd>
                </div>
                <div className="flex justify-between gap-2">
                  <dt>Updated</dt>
                  <dd className="font-medium text-slate-800">
                    {formatDate(station.updatedAt)}
                  </dd>
                </div>
              </dl>
            </div>

            {isBackoffice ? (
              <div className="rounded-xl border border-slate-200 bg-slate-50/80 p-4">
                <p className="text-sm font-semibold text-slate-900">Node lifecycle</p>
                <p className="mt-1 text-xs text-slate-600">
                  Deactivation is blocked while active energy reservations exist.
                </p>
                <div className="mt-3">
                  {station.status === "Active" ? (
                    <button
                      type="button"
                      disabled={statusBusy}
                      onClick={onDeactivate}
                      className="rounded-lg border border-red-200 bg-white px-3 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-60"
                    >
                      {statusBusy ? "Working…" : "Deactivate node"}
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={statusBusy}
                      onClick={onReactivate}
                      className="rounded-lg bg-grid-700 px-3 py-2 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
                    >
                      {statusBusy ? "Working…" : "Reactivate node"}
                    </button>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        </section>
      ) : null}

      {activeTab === "schedule" ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="mb-6">
            <h2 className="text-xl font-semibold tracking-tight text-grid-900">
              Schedule & slots
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {canEditSchedule
                ? "Adjust hours, working days, and available battery slots."
                : "Current operating schedule set by Backoffice for this node."}
            </p>
          </div>

          {canEditSchedule ? (
            <form onSubmit={onSaveSchedule} className="mx-auto max-w-xl space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Open time
                  </span>
                  <input
                    className={inputClass}
                    type="time"
                    value={schedule.openTime}
                    onChange={(e) => setSched("openTime", e.target.value)}
                    required
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">
                    Close time
                  </span>
                  <input
                    className={inputClass}
                    type="time"
                    value={schedule.closeTime}
                    onChange={(e) => setSched("closeTime", e.target.value)}
                    required
                  />
                </label>
              </div>
              <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-slate-700">
                  Battery slots
                </span>
                <input
                  className={inputClass}
                  type="number"
                  min="0"
                  step="1"
                  value={schedule.availableBatterySlots}
                  onChange={(e) =>
                    setSched("availableBatterySlots", e.target.value)
                  }
                  required
                />
              </label>
              <div>
                <p className="mb-2 text-sm font-medium text-slate-700">
                  Working days
                </p>
                <div className="flex flex-wrap gap-2">
                  {DAYS.map((day) => {
                    const active = schedule.workingDays.includes(day);
                    return (
                      <button
                        key={day}
                        type="button"
                        onClick={() => toggleDay(day)}
                        className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition ${
                          active
                            ? "border-grid-600 bg-grid-50 text-grid-900 ring-2 ring-grid-500/20"
                            : "border-slate-200 bg-white text-slate-600 hover:border-grid-300"
                        }`}
                      >
                        {day}
                      </button>
                    );
                  })}
                </div>
              </div>
              <button
                type="submit"
                disabled={savingSchedule || station.status === "Inactive"}
                className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {savingSchedule ? "Saving…" : "Save schedule"}
              </button>
            </form>
          ) : (
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="rounded-2xl border border-grid-100 bg-grid-50/80 p-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Operating hours
                </p>
                <p className="mt-2 text-2xl font-semibold tracking-tight text-grid-900">
                  {station.schedule?.openTime || "—"} –{" "}
                  {station.schedule?.closeTime || "—"}
                </p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Battery slots
                </p>
                <p className="mt-2 text-2xl font-semibold tracking-tight text-grid-900">
                  {station.availableBatterySlots}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  Configured by Backoffice
                </p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-5 sm:col-span-1">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Working days
                </p>
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {DAYS.map((day) => {
                    const active = (station.schedule?.workingDays || []).includes(
                      day,
                    );
                    return (
                      <span
                        key={day}
                        className={`rounded-lg px-2.5 py-1 text-xs font-semibold ${
                          active
                            ? "bg-grid-700 text-white"
                            : "bg-slate-100 text-slate-400"
                        }`}
                      >
                        {day}
                      </span>
                    );
                  })}
                </div>
              </div>
            </div>
          )}
        </section>
      ) : null}

      {activeTab === "batteries" ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                Battery management
              </h2>
              <p className="mt-1 max-w-2xl text-sm text-slate-600">
                {user.role === "GridOperator"
                  ? "Close a battery to stop new bookings on it, or reopen when ready. Charging uses free space; Drop-off withdraws stored energy after a completed transfer."
                  : "Each card shows stored energy, free charging space, drop-off capacity, and status. Grid Operators close or reopen batteries for trading."}
              </p>
            </div>
            <button
              type="button"
              onClick={loadBatteries}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            >
              Refresh
            </button>
          </div>

          {batteries.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
              No battery records yet. Creating or updating the station battery
              count will create them.
            </div>
          ) : (
            <>
              <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <BatterySummary
                  label="Batteries"
                  value={batterySummary.total}
                  hint={`${batterySummary.open} open · ${batterySummary.closed} closed`}
                />
                <BatterySummary
                  label="Stored energy"
                  value={`${batterySummary.stored.toFixed(1)} kWh`}
                  hint={`of ${batterySummary.capacity.toFixed(1)} kWh capacity`}
                  tone="grid"
                />
                <BatterySummary
                  label="Charge space"
                  value={`${batterySummary.chargeAvail.toFixed(1)} kWh`}
                  hint="Free capacity for Charging"
                  tone="amber"
                />
                <BatterySummary
                  label="Drop-off pool"
                  value={`${batterySummary.dropAvail.toFixed(1)} kWh`}
                  hint="Stored energy available to withdraw"
                  tone="sky"
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {batteries.map((battery) => (
                  <BatteryCard
                    key={battery.id}
                    battery={battery}
                    busy={batteryBusyId === battery.id}
                    canToggle={canManageBatteries}
                    onToggle={() => onToggleBattery(battery)}
                  />
                ))}
              </div>
            </>
          )}
        </section>
      ) : null}
    </div>
  );
}

const STATION_TABS = [
  { id: "details", label: "Station details" },
  { id: "schedule", label: "Schedule & slots" },
  { id: "batteries", label: "Battery management" },
];

function BatteryCard({ battery, busy, canToggle, onToggle }) {
  const capacity = Number(battery.capacityKwh) || 0;
  const actual = Number(battery.actualEnergyKwh) || 0;
  const chargeAvail = Number(battery.availableChargingKwh) || 0;
  const dropAvail = Number(battery.availableDropOffKwh) || 0;
  const fillPct = capacity > 0 ? Math.min(100, Math.round((actual / capacity) * 100)) : 0;
  const closed = battery.status === "Closed";

  return (
    <article
      className={`group relative flex flex-col overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:shadow-md ${
        closed
          ? "border-slate-200 opacity-90"
          : "border-grid-100 hover:border-grid-200"
      }`}
    >
      <div
        className={`h-1 w-full ${
          closed
            ? "bg-slate-300"
            : fillPct >= 85
              ? "bg-emerald-500"
              : fillPct >= 40
                ? "bg-grid-500"
                : "bg-amber-400"
        }`}
      />

      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`relative flex h-12 w-12 items-center justify-center rounded-xl ${
                closed ? "bg-slate-100 text-slate-500" : "bg-grid-50 text-grid-800"
              }`}
            >
              <BatteryGlyph fillPct={closed ? 0 : fillPct} />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">
                Battery {battery.batteryIndex}
              </p>
              <p className="text-xs text-slate-500">
                {capacity.toFixed(1)} kWh capacity
              </p>
            </div>
          </div>
          <span
            className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${
              closed
                ? "bg-slate-200 text-slate-700"
                : "bg-grid-100 text-grid-800"
            }`}
          >
            {closed ? "Closed" : "Available"}
          </span>
        </div>

        <div className="mt-5">
          <div className="mb-1.5 flex items-end justify-between gap-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Stored
            </p>
            <p className="text-sm font-semibold text-grid-900">
              {actual.toFixed(2)}{" "}
              <span className="text-xs font-medium text-slate-500">
                kWh · {fillPct}%
              </span>
            </p>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                closed
                  ? "bg-slate-400"
                  : fillPct >= 85
                    ? "bg-emerald-500"
                    : fillPct >= 40
                      ? "bg-grid-600"
                      : "bg-amber-400"
              }`}
              style={{ width: `${fillPct}%` }}
            />
          </div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-2">
          <MeterChip
            label="Charging space"
            value={`${chargeAvail.toFixed(2)} kWh`}
            pct={capacity > 0 ? Math.min(100, (chargeAvail / capacity) * 100) : 0}
            tone="amber"
          />
          <MeterChip
            label="Drop-off avail"
            value={`${dropAvail.toFixed(2)} kWh`}
            pct={capacity > 0 ? Math.min(100, (dropAvail / capacity) * 100) : 0}
            tone="sky"
          />
        </div>

        {canToggle ? (
          <button
            type="button"
            disabled={busy}
            onClick={onToggle}
            className={`mt-5 w-full rounded-xl px-3 py-2.5 text-sm font-semibold transition disabled:opacity-60 ${
              closed
                ? "bg-grid-700 text-white hover:bg-grid-800"
                : "border border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50"
            }`}
          >
            {busy ? "Updating…" : closed ? "Reopen battery" : "Close battery"}
          </button>
        ) : null}
      </div>
    </article>
  );
}

function MeterChip({ label, value, pct, tone }) {
  const tones = {
    amber: {
      wrap: "border-amber-100 bg-amber-50/80",
      bar: "bg-amber-400",
      track: "bg-amber-100",
      label: "text-amber-900/70",
      value: "text-amber-950",
    },
    sky: {
      wrap: "border-sky-100 bg-sky-50/80",
      bar: "bg-sky-500",
      track: "bg-sky-100",
      label: "text-sky-900/70",
      value: "text-sky-950",
    },
  };
  const t = tones[tone] || tones.amber;

  return (
    <div className={`rounded-xl border px-3 py-2.5 ${t.wrap}`}>
      <p className={`text-[10px] font-semibold uppercase tracking-wide ${t.label}`}>
        {label}
      </p>
      <p className={`mt-0.5 text-sm font-semibold ${t.value}`}>{value}</p>
      <div className={`mt-2 h-1 overflow-hidden rounded-full ${t.track}`}>
        <div
          className={`h-full rounded-full ${t.bar}`}
          style={{ width: `${Math.max(2, pct)}%` }}
        />
      </div>
    </div>
  );
}

function BatterySummary({ label, value, hint, tone = "default" }) {
  const tones = {
    default: "border-slate-200 bg-slate-50/80",
    grid: "border-grid-100 bg-grid-50",
    amber: "border-amber-100 bg-amber-50/70",
    sky: "border-sky-100 bg-sky-50/70",
  };
  return (
    <div className={`rounded-2xl border px-4 py-3 ${tones[tone] || tones.default}`}>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className="mt-1 text-xl font-semibold tracking-tight text-grid-900">
        {value}
      </p>
      {hint ? <p className="mt-0.5 text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}

function BatteryGlyph({ fillPct }) {
  return (
    <svg viewBox="0 0 24 24" className="h-6 w-6" aria-hidden>
      <rect
        x="6"
        y="4"
        width="12"
        height="16"
        rx="2"
        className="fill-current opacity-15"
      />
      <rect x="9" y="2" width="6" height="2" rx="0.5" className="fill-current opacity-40" />
      <rect
        x="7.5"
        y={18 - (12 * fillPct) / 100}
        width="9"
        height={(12 * fillPct) / 100}
        rx="1"
        className="fill-current"
      />
    </svg>
  );
}

function Info({ label, value, wide = false }) {
  return (
    <div className={wide ? "sm:col-span-2" : undefined}>
      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </dt>
      <dd className="mt-1 text-slate-800">{value}</dd>
    </div>
  );
}

function StatusBadge({ status, light = false }) {
  if (light) {
    return (
      <span className="inline-flex rounded-full border border-white/30 bg-white/15 px-2.5 py-0.5 text-xs font-semibold text-white">
        {status}
      </span>
    );
  }
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

function formatDate(value) {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20";
