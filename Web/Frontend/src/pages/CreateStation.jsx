import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createStation } from "../api/stations";
import AlertMessage from "../components/AlertMessage";
import LocationPicker from "../components/LocationPicker";
import { useFeedback } from "../context/FeedbackContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1508514177221-188b1cf16e9d?auto=format&fit=crop&w=1800&q=80";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

const emptyForm = {
  name: "",
  description: "",
  latitude: "",
  longitude: "",
  batteryCapacityKwh: "",
  availableBatterySlots: "0",
  openTime: "08:00",
  closeTime: "18:00",
  workingDays: ["Mon", "Tue", "Wed", "Thu", "Fri"],
};

export default function CreateStation() {
  const navigate = useNavigate();
  const { notify } = useFeedback();
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function toggleDay(day) {
    setForm((prev) => {
      const has = prev.workingDays.includes(day);
      return {
        ...prev,
        workingDays: has
          ? prev.workingDays.filter((d) => d !== day)
          : [...prev.workingDays, day],
      };
    });
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");

    if (form.workingDays.length === 0) {
      setError("Select at least one working day.");
      return;
    }

    if (form.latitude === "" || form.longitude === "") {
      setError("Select a location on the map.");
      return;
    }

    setLoading(true);
    try {
      const station = await createStation({
        name: form.name.trim(),
        description: form.description.trim() || null,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
        batteryCapacityKwh: Number(form.batteryCapacityKwh),
        availableBatterySlots: Number(form.availableBatterySlots),
        openTime: form.openTime,
        closeTime: form.closeTime,
        workingDays: form.workingDays,
      });
      notify.success(`${station.name} created successfully.`);
      navigate(`/stations/${station.id}`, { replace: true });
    } catch (err) {
      const msg = err.message || "Create failed";
      setError(msg);
      notify.error(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Solar installation under clear sky"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/40" />
        </div>

        <div className="relative z-10 flex flex-col gap-4 px-6 py-10 sm:px-10 sm:py-12 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
              Backoffice console
            </p>
            <h1 className="mt-3 font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
              Create solar station
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
              Register a new microgrid hub with location, capacity, battery slots,
              and operating hours. A unique station code is generated automatically.
            </p>
          </div>
          <Link
            to="/stations"
            className="inline-flex items-center justify-center rounded-xl border border-white/25 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
          >
            Back to stations
          </Link>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-6">
          <h2 className="text-xl font-semibold tracking-tight text-grid-900">
            Station details
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            New stations start as <span className="font-semibold text-grid-800">Active</span>.
          </p>
        </div>

        {error ? (
          <div className="mb-4">
            <AlertMessage type="error" title="Could not create station" onDismiss={() => setError("")}>
              {error}
            </AlertMessage>
          </div>
        ) : null}

        <form onSubmit={onSubmit} className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm sm:col-span-2">
              <span className="mb-1.5 block font-medium text-slate-700">Name</span>
              <input
                className={inputClass}
                value={form.name}
                onChange={(e) => set("name", e.target.value)}
                placeholder="e.g. Nugegoda Solar Hub"
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
                    value={form.description}
                    onChange={(e) => set("description", e.target.value)}
                    placeholder="Optional notes about this node"
                  />
                </label>

                <div className="sm:col-span-2">
                  <p className="mb-1.5 text-sm font-medium text-slate-700">
                    Location on map
                  </p>
                  <LocationPicker
                    latitude={form.latitude}
                    longitude={form.longitude}
                    onChange={({ latitude, longitude }) => {
                      setForm((prev) => ({
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
                    value={form.batteryCapacityKwh}
                    onChange={(e) => set("batteryCapacityKwh", e.target.value)}
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
                    value={form.availableBatterySlots}
                    onChange={(e) => set("availableBatterySlots", e.target.value)}
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
                      form.batteryCapacityKwh !== "" && form.availableBatterySlots !== ""
                        ? `${Number(form.availableBatterySlots) * Number(form.batteryCapacityKwh)} kWh`
                        : "—"
                    }
                  />
                  <span className="mt-1 block text-xs text-slate-500">
                    Calculated as battery slots × kWh per battery
                  </span>
                </label>

                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Open time</span>
                  <input
                    className={inputClass}
                    type="time"
                    value={form.openTime}
                    onChange={(e) => set("openTime", e.target.value)}
                    required
                  />
                </label>

                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Close time</span>
                  <input
                    className={inputClass}
                    type="time"
                    value={form.closeTime}
                    onChange={(e) => set("closeTime", e.target.value)}
                    required
                  />
                </label>
              </div>

              <div>
                <p className="mb-2 text-sm font-medium text-slate-700">Working days</p>
                <div className="flex flex-wrap gap-2">
                  {DAYS.map((day) => {
                    const active = form.workingDays.includes(day);
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

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              type="submit"
              disabled={loading}
              className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Creating station…" : "Create station"}
            </button>
            <Link
              to="/stations"
              className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            >
              Cancel
            </Link>
          </div>
        </form>
      </section>
    </div>
  );
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20";
