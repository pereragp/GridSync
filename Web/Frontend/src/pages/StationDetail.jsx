import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  deactivateStation,
  getStation,
  reactivateStation,
  updateStation,
  updateStationSchedule,
} from "../api/stations";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=1800&q=80";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

export default function StationDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const isBackoffice = user.role === "Backoffice";
  const canEditSchedule =
    user.role === "Backoffice" || user.role === "GridOperator";

  const [station, setStation] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingDetails, setSavingDetails] = useState(false);
  const [savingSchedule, setSavingSchedule] = useState(false);
  const [statusBusy, setStatusBusy] = useState(false);

  const [details, setDetails] = useState(null);
  const [schedule, setSchedule] = useState(null);

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
    } catch (err) {
      setError(err.message || "Failed to load station");
      setStation(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

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
    setMessage("");
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
      setMessage("Station details updated.");
    } catch (err) {
      setError(err.message || "Update failed");
    } finally {
      setSavingDetails(false);
    }
  }

  async function onSaveSchedule(e) {
    e.preventDefault();
    setMessage("");
    setError("");

    if (schedule.workingDays.length === 0) {
      setError("Select at least one working day.");
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
      setMessage("Schedule and battery slots updated.");
    } catch (err) {
      setError(err.message || "Schedule update failed");
    } finally {
      setSavingSchedule(false);
    }
  }

  async function onDeactivate() {
    if (
      !window.confirm(
        `Deactivate ${station?.name}? Blocked if active reservations exist.`
      )
    ) {
      return;
    }
    setMessage("");
    setError("");
    setStatusBusy(true);
    try {
      const updated = await deactivateStation(id);
      setStation(updated);
      setMessage(`Deactivated ${updated.name}`);
    } catch (err) {
      setError(err.message || "Deactivate failed");
    } finally {
      setStatusBusy(false);
    }
  }

  async function onReactivate() {
    if (!window.confirm(`Reactivate ${station?.name}?`)) {
      return;
    }
    setMessage("");
    setError("");
    setStatusBusy(true);
    try {
      const updated = await reactivateStation(id);
      setStation(updated);
      setMessage(`Reactivated ${updated.name}`);
    } catch (err) {
      setError(err.message || "Reactivate failed");
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
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          {error || "Station not found."}
        </div>
        <Link to="/stations" className="text-sm font-semibold text-grid-700 hover:underline">
          Back to stations
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Renewable energy landscape"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/40" />
        </div>

        <div className="relative z-10 flex flex-col gap-4 px-6 py-10 sm:px-10 sm:py-12 lg:flex-row lg:items-end lg:justify-between">
          <div>
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
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/stations"
              className="inline-flex items-center justify-center rounded-xl border border-white/25 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
            >
              All stations
            </Link>
            {isBackoffice && station.status === "Active" ? (
              <button
                type="button"
                disabled={statusBusy}
                onClick={onDeactivate}
                className="inline-flex items-center justify-center rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 shadow-lg transition hover:bg-grid-50 disabled:opacity-60"
              >
                {statusBusy ? "Working…" : "Deactivate"}
              </button>
            ) : null}
            {isBackoffice && station.status === "Inactive" ? (
              <button
                type="button"
                disabled={statusBusy}
                onClick={onReactivate}
                className="inline-flex items-center justify-center rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 shadow-lg transition hover:bg-grid-50 disabled:opacity-60"
              >
                {statusBusy ? "Working…" : "Reactivate"}
              </button>
            ) : null}
          </div>
        </div>
      </section>

      {error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          {error}
        </div>
      ) : null}
      {message ? (
        <div className="rounded-xl border border-grid-200 bg-grid-50 px-4 py-3 text-sm text-grid-800">
          {message}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-12">
        {/* Details — Backoffice only edit; others read-only summary */}
        <section className="lg:col-span-7">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <div className="mb-6">
              <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                Station details
              </h2>
              <p className="mt-1 text-sm text-slate-600">
                {isBackoffice
                  ? "Update name, location, and capacity. Schedule is managed separately."
                  : "Core station information (read-only for operators)."}
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
                <dl className="grid gap-4 sm:grid-cols-2 text-sm">
                  <Info label="Description" value={station.description || "—"} wide />
                  <Info
                    label="Battery capacity (kWh per battery)"
                    value={station.batteryCapacityKwh}
                  />
                  <Info label="Battery slots" value={station.availableBatterySlots} />
                  <Info
                    label="Total capacity (kWh)"
                    value={station.totalCapacityKwh}
                  />
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
          </div>
        </section>

        {/* Schedule — Backoffice + GridOperator */}
        <section className="lg:col-span-5">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <div className="mb-6">
              <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                Schedule & slots
              </h2>
              <p className="mt-1 text-sm text-slate-600">
                {canEditSchedule
                  ? "Operators and Backoffice can adjust hours and available battery slots."
                  : "Operating hours for this node."}
              </p>
            </div>

            {canEditSchedule ? (
              <form onSubmit={onSaveSchedule} className="space-y-4">
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Open time</span>
                  <input
                    className={inputClass}
                    type="time"
                    value={schedule.openTime}
                    onChange={(e) => setSched("openTime", e.target.value)}
                    required
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Close time</span>
                  <input
                    className={inputClass}
                    type="time"
                    value={schedule.closeTime}
                    onChange={(e) => setSched("closeTime", e.target.value)}
                    required
                  />
                </label>
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
                  <p className="mb-2 text-sm font-medium text-slate-700">Working days</p>
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
              <dl className="space-y-3 text-sm">
                <Info
                  label="Hours"
                  value={`${station.schedule?.openTime} – ${station.schedule?.closeTime}`}
                />
                <Info
                  label="Days"
                  value={(station.schedule?.workingDays || []).join(", ") || "—"}
                />
              </dl>
            )}
          </div>

          <div className="mt-4 rounded-2xl border border-grid-100 bg-gradient-to-br from-grid-50 to-white p-5">
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
        </section>
      </div>
    </div>
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
