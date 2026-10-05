import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { jsPDF } from "jspdf";
import { QRCodeCanvas } from "qrcode.react";
import {
  cancelReservation,
  createReservation,
  getAvailableBookingSlots,
  getReservationHistory,
  getUpcomingReservations,
  updateReservation,
} from "../api/reservations";
import {
  Alert,
  Field,
  btnPrimary,
  btnSecondary,
  cardClass,
  inputClass,
} from "../components/ui";

const statuses = [
  "",
  "Pending",
  "Approved",
  "Rejected",
  "Cancelled",
  "Completed",
  "Expired",
];
const reservationTypes = ["", "Charging", "DropOff"];

function defaultVisitWindow() {
  const start = new Date();
  start.setHours(start.getHours() + 24, 0, 0, 0);
  const end = new Date(start);
  end.setHours(end.getHours() + 1);
  return {
    slotStart: toLocalInput(start),
    slotEnd: toLocalInput(end),
  };
}

function toLocalInput(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toIsoUtc(localValue) {
  if (!localValue) return null;
  return new Date(localValue).toISOString();
}

function emptyCreateForm() {
  const visit = defaultVisitWindow();
  return {
    stationId: "",
    slotId: "",
    reservationType: "Charging",
    energyKwh: "",
    slotStart: visit.slotStart,
    slotEnd: visit.slotEnd,
  };
}

export default function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [upcomingReservations, setUpcomingReservations] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const [reservationType, setReservationType] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [form, setForm] = useState(emptyCreateForm);
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState({});
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState("");
  const createPanelRef = useRef(null);

  const loadSlots = useCallback(async () => {
    try {
      setAvailableSlots((await getAvailableBookingSlots()) || []);
    } catch (err) {
      setError(err.message || "Failed to load batteries");
    }
  }, []);

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [history, upcoming] = await Promise.all([
        getReservationHistory(status),
        getUpcomingReservations(status),
      ]);
      setReservations(history || []);
      setUpcomingReservations(upcoming || []);
    } catch (err) {
      setError(err.message || "Failed to load reservations");
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    loadSlots();
  }, [loadSlots]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (showCreate && createPanelRef.current) {
      createPanelRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [showCreate]);

  const stations = useMemo(() => {
    const byId = new Map();
    for (const slot of availableSlots) {
      if (!slot.stationId) continue;
      const existing = byId.get(slot.stationId);
      if (existing) {
        existing.batteryCount += 1;
        existing.chargeAvail += Number(slot.availableChargingKwh) || 0;
        existing.dropOffAvail += Number(slot.availableDropOffKwh) || 0;
      } else {
        byId.set(slot.stationId, {
          id: slot.stationId,
          name: slot.stationName || "Unknown station",
          batteryCount: 1,
          chargeAvail: Number(slot.availableChargingKwh) || 0,
          dropOffAvail: Number(slot.availableDropOffKwh) || 0,
        });
      }
    }
    return [...byId.values()].sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { sensitivity: "base" }),
    );
  }, [availableSlots]);

  const batteriesForStation = useMemo(() => {
    if (!form.stationId) return [];
    return availableSlots
      .filter((s) => s.stationId === form.stationId)
      .sort((a, b) => (a.batteryIndex ?? 0) - (b.batteryIndex ?? 0));
  }, [availableSlots, form.stationId]);

  const selectedSlot = useMemo(
    () => batteriesForStation.find((s) => s.id === form.slotId) || null,
    [batteriesForStation, form.slotId],
  );

  const availableForType = selectedSlot
    ? form.reservationType === "DropOff"
      ? selectedSlot.availableDropOffKwh
      : selectedSlot.availableChargingKwh
    : 0;

  const stats = useMemo(() => {
    const pending = upcomingReservations.filter(
      (r) => r.status === "Pending",
    ).length;
    const approved = upcomingReservations.filter(
      (r) => r.status === "Approved",
    ).length;
    return {
      upcoming: upcomingReservations.length,
      pending,
      approved,
      history: reservations.length,
    };
  }, [upcomingReservations, reservations]);

  const normalizedSearch = search.trim().toLowerCase();
  function matchesFilters(reservation) {
    const matchesSearch =
      !normalizedSearch ||
      [reservation.reservationCode, reservation.stationName]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalizedSearch));
    const matchesType =
      !reservationType || reservation.reservationType === reservationType;
    const slotDate = reservation.slotStart
      ? reservation.slotStart.slice(0, 10)
      : "";
    const matchesFromDate = !fromDate || (slotDate && slotDate >= fromDate);
    const matchesToDate = !toDate || (slotDate && slotDate <= toDate);

    return matchesSearch && matchesType && matchesFromDate && matchesToDate;
  }

  const filteredUpcomingReservations =
    upcomingReservations.filter(matchesFilters);
  const filteredReservations = reservations.filter(matchesFilters);

  function clearFeedback() {
    setError("");
    setMessage("");
  }

  function openCreate() {
    clearFeedback();
    setForm(emptyCreateForm());
    setShowCreate(true);
  }

  function closeCreate() {
    setShowCreate(false);
    setForm(emptyCreateForm());
  }

  async function onCreate(e) {
    e.preventDefault();
    clearFeedback();

    const energy = Number(form.energyKwh);
    if (!Number.isFinite(energy) || energy <= 0) {
      setError("Enter a valid energy amount in kWh.");
      return;
    }
    if (energy > availableForType) {
      setError(
        `Requested ${energy} kWh exceeds available ${availableForType.toFixed(2)} kWh for ${form.reservationType}.`,
      );
      return;
    }

    setSubmitting(true);
    try {
      await createReservation({
        slotId: form.slotId,
        reservationType: form.reservationType,
        energyKwh: energy,
        slotStart: toIsoUtc(form.slotStart),
        slotEnd: toIsoUtc(form.slotEnd),
      });
      setMessage("Reservation created and is pending review.");
      setForm(emptyCreateForm());
      setShowCreate(false);
      await Promise.all([load(), loadSlots()]);
    } catch (err) {
      setError(err.message || "Failed to create reservation");
    } finally {
      setSubmitting(false);
    }
  }

  async function onUpdate(reservation) {
    clearFeedback();
    const draft =
      editing[reservation.id] ||
      {
        reservationType: reservation.reservationType,
        energyKwh: String(reservation.energyKwh ?? ""),
        slotStart: toLocalInput(new Date(reservation.slotStart)),
        slotEnd: toLocalInput(new Date(reservation.slotEnd)),
      };

    const energy = Number(draft.energyKwh);
    if (!Number.isFinite(energy) || energy <= 0) {
      setError("Enter a valid energy amount in kWh.");
      return;
    }

    setActionId(reservation.id);
    try {
      await updateReservation(reservation.id, {
        reservationType: draft.reservationType,
        energyKwh: energy,
        slotStart: toIsoUtc(draft.slotStart),
        slotEnd: toIsoUtc(draft.slotEnd),
      });
      setMessage(`Reservation ${reservation.reservationCode} updated.`);
      await Promise.all([load(), loadSlots()]);
    } catch (err) {
      setError(err.message || "Failed to update reservation");
    } finally {
      setActionId("");
    }
  }

  async function onCancel(reservation) {
    const reason = window.prompt("Optional cancellation reason:", "");
    if (reason === null) return;

    clearFeedback();
    setActionId(reservation.id);
    try {
      await cancelReservation(reservation.id, reason);
      setMessage(`Reservation ${reservation.reservationCode} cancelled.`);
      await Promise.all([load(), loadSlots()]);
    } catch (err) {
      setError(err.message || "Failed to cancel reservation");
    } finally {
      setActionId("");
    }
  }

  return (
    <div className="space-y-6">
      {/* Dashboard header */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-grid-900 sm:text-3xl">
            Reservations
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Track upcoming visits and past bookings. Create a new Charging or
            Drop-off reservation when you are ready.
          </p>
        </div>
        <button
          type="button"
          onClick={() => (showCreate ? closeCreate() : openCreate())}
          className={showCreate ? btnSecondary : btnPrimary}
        >
          {showCreate ? "Close form" : "Create reservation"}
        </button>
      </div>

      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type="success">{message}</Alert> : null}

      {/* Stats */}
      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Upcoming" value={stats.upcoming} />
        <StatCard label="Pending review" value={stats.pending} />
        <StatCard label="Approved" value={stats.approved} accent />
        <StatCard label="History" value={stats.history} />
      </section>

      {/* Create panel (toggled) */}
      {showCreate ? (
        <section
          ref={createPanelRef}
          className={`${cardClass} border-grid-200 ring-1 ring-grid-500/10`}
        >
          <div className="mb-4">
            <h2 className="text-lg font-semibold text-grid-900">
              Create reservation
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              Choose a station, then an available battery. Set type, energy, and
              a visit window within the next 7 days.
            </p>
          </div>
          <form onSubmit={onCreate} className="space-y-6">
            {/* Step 1 — Station */}
            <div>
              <div className="mb-3 flex items-end justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                    Step 1
                  </p>
                  <h3 className="text-base font-semibold text-grid-900">
                    Choose a station
                  </h3>
                </div>
                {form.stationId ? (
                  <button
                    type="button"
                    className="text-sm font-medium text-grid-700 hover:text-grid-900"
                    onClick={() =>
                      setForm({ ...form, stationId: "", slotId: "" })
                    }
                  >
                    Clear
                  </button>
                ) : null}
              </div>
              {stations.length === 0 ? (
                <p className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
                  No stations with available batteries right now.
                </p>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {stations.map((station) => {
                    const selected = form.stationId === station.id;
                    return (
                      <button
                        key={station.id}
                        type="button"
                        onClick={() =>
                          setForm({
                            ...form,
                            stationId: station.id,
                            slotId: "",
                          })
                        }
                        className={`rounded-xl border p-4 text-left transition ${
                          selected
                            ? "border-grid-600 bg-grid-50 ring-2 ring-grid-500/25"
                            : "border-slate-200 bg-white hover:border-grid-300 hover:bg-grid-50/40"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <p className="font-semibold text-grid-900">
                            {station.name}
                          </p>
                          {selected ? (
                            <span className="rounded-md bg-grid-700 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-white">
                              Selected
                            </span>
                          ) : null}
                        </div>
                        <p className="mt-2 text-sm text-slate-600">
                          {station.batteryCount}{" "}
                          {station.batteryCount === 1 ? "battery" : "batteries"}{" "}
                          available
                        </p>
                        <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
                          <span>
                            Charge{" "}
                            <strong className="text-slate-700">
                              {station.chargeAvail.toFixed(1)} kWh
                            </strong>
                          </span>
                          <span>
                            Drop-off{" "}
                            <strong className="text-slate-700">
                              {station.dropOffAvail.toFixed(1)} kWh
                            </strong>
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Step 2 — Battery */}
            <div>
              <div className="mb-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                  Step 2
                </p>
                <h3 className="text-base font-semibold text-grid-900">
                  Choose a battery
                </h3>
              </div>
              {!form.stationId ? (
                <p className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
                  Select a station to see its batteries.
                </p>
              ) : batteriesForStation.length === 0 ? (
                <p className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
                  No batteries available at this station.
                </p>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {batteriesForStation.map((slot) => {
                    const selected = form.slotId === slot.id;
                    const capacity = Number(slot.capacityKwh) || 0;
                    const actual = Number(slot.actualEnergyKwh) || 0;
                    const fillPct =
                      capacity > 0
                        ? Math.min(100, Math.round((actual / capacity) * 100))
                        : 0;
                    return (
                      <button
                        key={slot.id}
                        type="button"
                        onClick={() =>
                          setForm({ ...form, slotId: slot.id })
                        }
                        className={`rounded-xl border p-4 text-left transition ${
                          selected
                            ? "border-grid-600 bg-grid-50 ring-2 ring-grid-500/25"
                            : "border-slate-200 bg-white hover:border-grid-300 hover:bg-grid-50/40"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <p className="font-semibold text-grid-900">
                            Battery #{slot.batteryIndex}
                          </p>
                          {selected ? (
                            <span className="rounded-md bg-grid-700 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-white">
                              Selected
                            </span>
                          ) : null}
                        </div>
                        <div className="mt-3">
                          <div className="mb-1 flex justify-between text-xs text-slate-500">
                            <span>Stored</span>
                            <span>
                              {actual.toFixed(1)} / {capacity.toFixed(1)} kWh
                            </span>
                          </div>
                          <div className="h-2 overflow-hidden rounded-full bg-slate-200">
                            <div
                              className="h-full rounded-full bg-grid-600 transition-all"
                              style={{ width: `${fillPct}%` }}
                            />
                          </div>
                        </div>
                        <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                          <div className="rounded-lg bg-white/80 px-2.5 py-2 ring-1 ring-slate-200/80">
                            <p className="text-slate-500">Charge avail</p>
                            <p className="mt-0.5 font-semibold text-grid-800">
                              {Number(slot.availableChargingKwh).toFixed(1)} kWh
                            </p>
                          </div>
                          <div className="rounded-lg bg-white/80 px-2.5 py-2 ring-1 ring-slate-200/80">
                            <p className="text-slate-500">Drop-off avail</p>
                            <p className="mt-0.5 font-semibold text-grid-800">
                              {Number(slot.availableDropOffKwh).toFixed(1)} kWh
                            </p>
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Step 3 — Details */}
            <div>
              <div className="mb-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                  Step 3
                </p>
                <h3 className="text-base font-semibold text-grid-900">
                  Reservation details
                </h3>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <Field label="Reservation type">
                  <select
                    className={inputClass}
                    value={form.reservationType}
                    onChange={(e) =>
                      setForm({ ...form, reservationType: e.target.value })
                    }
                  >
                    <option value="Charging">
                      Charging (deposit into battery)
                    </option>
                    <option value="DropOff">
                      Drop-off (withdraw stored energy)
                    </option>
                  </select>
                </Field>
                <Field
                  label={`Energy (kWh)${
                    selectedSlot
                      ? ` — up to ${Number(availableForType).toFixed(2)} available`
                      : ""
                  }`}
                >
                  <input
                    className={inputClass}
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.energyKwh}
                    onChange={(e) =>
                      setForm({ ...form, energyKwh: e.target.value })
                    }
                    required
                    placeholder="e.g. 5"
                    disabled={!form.slotId}
                  />
                </Field>
                <Field label="Visit start">
                  <input
                    className={inputClass}
                    type="datetime-local"
                    value={form.slotStart}
                    onChange={(e) =>
                      setForm({ ...form, slotStart: e.target.value })
                    }
                    required
                  />
                </Field>
                <Field label="Visit end">
                  <input
                    className={inputClass}
                    type="datetime-local"
                    value={form.slotEnd}
                    onChange={(e) =>
                      setForm({ ...form, slotEnd: e.target.value })
                    }
                    required
                  />
                </Field>
              </div>
            </div>

            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button
                className={btnPrimary}
                disabled={submitting || !form.stationId || !form.slotId}
              >
                {submitting ? "Creating…" : "Submit reservation"}
              </button>
              <button
                type="button"
                className={btnSecondary}
                onClick={closeCreate}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </section>
      ) : null}

      {/* Shared filters */}
      <section className={`${cardClass}`}>
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.12em] text-slate-500">
          Filters
        </h2>
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Search</span>
            <input
              className={inputClass}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Code or station"
            />
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Status</span>
            <select
              className={inputClass}
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              {statuses.map((value) => (
                <option key={value} value={value}>
                  {value || "All statuses"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Type</span>
            <select
              className={inputClass}
              value={reservationType}
              onChange={(e) => setReservationType(e.target.value)}
            >
              {reservationTypes.map((value) => (
                <option key={value} value={value}>
                  {value === "DropOff" ? "Drop-off" : value || "All types"}
                </option>
              ))}
            </select>
          </label>
          <div className="grid grid-cols-2 gap-2">
            <label className="text-sm">
              <span className="mb-1 block font-medium text-slate-700">From</span>
              <input
                className={inputClass}
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </label>
            <label className="text-sm">
              <span className="mb-1 block font-medium text-slate-700">To</span>
              <input
                className={inputClass}
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </label>
          </div>
        </div>
      </section>

      {/* Upcoming + history side by side */}
      <div className="grid gap-6 lg:grid-cols-2 lg:items-start">
        <section className={`${cardClass} flex min-h-0 flex-col`}>
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-grid-900">
                Upcoming reservations
              </h2>
              <p className="mt-0.5 text-sm text-slate-500">
                Showing {filteredUpcomingReservations.length} of{" "}
                {upcomingReservations.length}
              </p>
            </div>
          </div>
          {loading ? <p className="text-sm text-slate-500">Loading…</p> : null}
          {!loading && filteredUpcomingReservations.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center">
              <p className="text-sm text-slate-500">
                No upcoming reservations for this filter.
              </p>
              <button
                type="button"
                onClick={openCreate}
                className={`${btnPrimary} mt-4`}
              >
                Create reservation
              </button>
            </div>
          ) : null}
          <div className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
            {filteredUpcomingReservations.map((reservation) => (
              <ReservationItem
                key={reservation.id}
                reservation={reservation}
                draft={editing[reservation.id]}
                onDraftChange={(draft) =>
                  setEditing({ ...editing, [reservation.id]: draft })
                }
                onUpdate={() => onUpdate(reservation)}
                onCancel={() => onCancel(reservation)}
                busy={actionId === reservation.id}
              />
            ))}
          </div>
        </section>

        <section className={`${cardClass} flex min-h-0 flex-col`}>
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-grid-900">
                Booking history
              </h2>
              <p className="mt-0.5 text-sm text-slate-500">
                Showing {filteredReservations.length} of {reservations.length}
              </p>
            </div>
          </div>
          {!loading && filteredReservations.length === 0 ? (
            <p className="text-sm text-slate-500">
              No past reservations found for this filter.
            </p>
          ) : null}
          <div className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
            {filteredReservations.map((reservation) => (
              <ReservationItem
                key={reservation.id}
                reservation={reservation}
                draft={editing[reservation.id]}
                onDraftChange={(draft) =>
                  setEditing({ ...editing, [reservation.id]: draft })
                }
                onUpdate={() => onUpdate(reservation)}
                onCancel={() => onCancel(reservation)}
                busy={actionId === reservation.id}
              />
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function StatCard({ label, value, accent = false }) {
  return (
    <div
      className={`rounded-2xl border p-4 ${
        accent
          ? "border-grid-200 bg-grid-50"
          : "border-slate-200 bg-white"
      }`}
    >
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
        {label}
      </p>
      <p className="mt-2 text-3xl font-semibold tracking-tight text-grid-900">
        {value}
      </p>
    </div>
  );
}

function ReservationItem({
  reservation,
  draft,
  onDraftChange,
  onUpdate,
  onCancel,
  busy,
}) {
  const [showQr, setShowQr] = useState(false);
  const editable = reservation.status === "Pending";
  const activeDraft =
    draft ||
    (editable
      ? {
          reservationType: reservation.reservationType,
          energyKwh: String(reservation.energyKwh ?? ""),
          slotStart: toLocalInput(new Date(reservation.slotStart)),
          slotEnd: toLocalInput(new Date(reservation.slotEnd)),
        }
      : null);

  const hasQr =
    reservation.status === "Approved" && Boolean(reservation.qrPayload);

  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50/40 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium text-slate-900">
            {reservation.reservationCode || reservation.id}
          </p>
          <p className="text-sm text-slate-600">
            {reservation.stationName || "Station unavailable"} ·{" "}
            {reservation.reservationType === "DropOff"
              ? "Drop-off"
              : reservation.reservationType}{" "}
            · {reservation.energyKwh} kWh
          </p>
          <p className="text-sm text-slate-600">
            {formatDate(reservation.slotStart)} –{" "}
            {formatDate(reservation.slotEnd)}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={reservation.status} />
          {hasQr ? (
            <button
              type="button"
              className={btnSecondary}
              onClick={() => setShowQr((open) => !open)}
            >
              {showQr ? "Hide QR" : "Show QR"}
            </button>
          ) : null}
        </div>
      </div>

      {editable && activeDraft ? (
        <div className="mt-3 grid gap-3 md:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Type</span>
            <select
              className={inputClass}
              value={activeDraft.reservationType}
              onChange={(e) =>
                onDraftChange({
                  ...activeDraft,
                  reservationType: e.target.value,
                })
              }
              disabled={busy}
            >
              <option value="Charging">Charging</option>
              <option value="DropOff">Drop-off</option>
            </select>
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">
              Energy (kWh)
            </span>
            <input
              className={inputClass}
              type="number"
              min="0.01"
              step="0.01"
              value={activeDraft.energyKwh}
              onChange={(e) =>
                onDraftChange({ ...activeDraft, energyKwh: e.target.value })
              }
              disabled={busy}
            />
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Start</span>
            <input
              className={inputClass}
              type="datetime-local"
              value={activeDraft.slotStart}
              onChange={(e) =>
                onDraftChange({ ...activeDraft, slotStart: e.target.value })
              }
              disabled={busy}
            />
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">End</span>
            <input
              className={inputClass}
              type="datetime-local"
              value={activeDraft.slotEnd}
              onChange={(e) =>
                onDraftChange({ ...activeDraft, slotEnd: e.target.value })
              }
              disabled={busy}
            />
          </label>
          <div className="flex flex-wrap gap-2 md:col-span-2 lg:col-span-4">
            <button
              type="button"
              className={btnSecondary}
              onClick={onUpdate}
              disabled={busy}
            >
              Save changes
            </button>
            <button
              type="button"
              className="inline-flex items-center justify-center rounded-md border border-red-200 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50 disabled:opacity-60"
              onClick={onCancel}
              disabled={busy}
            >
              Cancel reservation
            </button>
          </div>
        </div>
      ) : null}

      {reservation.status === "Rejected" && reservation.rejectionReason ? (
        <p className="mt-2 text-sm text-red-700">
          Reason: {reservation.rejectionReason}
        </p>
      ) : null}
      {reservation.status === "Cancelled" && reservation.cancellationReason ? (
        <p className="mt-2 text-sm text-slate-600">
          Cancellation reason: {reservation.cancellationReason}
        </p>
      ) : null}
      {hasQr && showQr ? (
        <div className="mt-4 flex flex-wrap items-center gap-4 rounded-md border border-teal-100 bg-teal-50 p-3">
          <QRCodeCanvas
            id={`reservation-qr-${reservation.id}`}
            value={reservation.qrPayload}
            size={144}
            includeMargin
            aria-label={`QR code for ${reservation.reservationCode || "reservation"}`}
          />
          <div className="text-sm text-teal-900">
            <p className="font-medium">Transaction QR code</p>
            <p className="mt-1 text-teal-800">
              Show this code to the grid operator at the station. Energy is
              applied only after the QR is scanned and completed.
            </p>
            {reservation.qrGeneratedAt ? (
              <p className="mt-1 text-xs text-teal-700">
                Generated {formatDate(reservation.qrGeneratedAt)}
              </p>
            ) : null}
            <button
              type="button"
              className={`${btnSecondary} mt-3`}
              onClick={() => downloadReservationPdf(reservation)}
            >
              Download PDF
            </button>
          </div>
        </div>
      ) : null}
    </article>
  );
}

function StatusBadge({ status }) {
  const colors = {
    Pending: "bg-amber-50 text-amber-800",
    Approved: "bg-teal-50 text-teal-800",
    Rejected: "bg-red-50 text-red-800",
    Cancelled: "bg-slate-200 text-slate-700",
    Completed: "bg-grid-100 text-grid-800",
    Expired: "bg-slate-100 text-slate-700",
  };
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-medium ${colors[status] || "bg-slate-100 text-slate-700"}`}
    >
      {status}
    </span>
  );
}

function formatDate(value) {
  if (!value) return "Unknown time";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function downloadReservationPdf(reservation) {
  const qrCanvas = document.getElementById(`reservation-qr-${reservation.id}`);
  if (!qrCanvas) return;

  const code = reservation.reservationCode || reservation.id;
  const documentFile = new jsPDF();
  documentFile.setFontSize(20);
  documentFile.text("GridSync Reservation", 20, 24);
  documentFile.setFontSize(12);
  documentFile.text(`Reservation: ${code}`, 20, 38);
  documentFile.text(
    `Station: ${reservation.stationName || "Unavailable"}`,
    20,
    48,
  );
  documentFile.text(`Type: ${reservation.reservationType}`, 20, 58);
  documentFile.text(`Energy: ${reservation.energyKwh} kWh`, 20, 68);
  documentFile.text(`Start: ${formatDate(reservation.slotStart)}`, 20, 78);
  documentFile.text(`End: ${formatDate(reservation.slotEnd)}`, 20, 88);
  documentFile.text("Present this QR code to the grid operator.", 20, 104);
  documentFile.addImage(
    qrCanvas.toDataURL("image/png"),
    "PNG",
    20,
    114,
    55,
    55,
  );
  documentFile.save(`${code}.pdf`);
}
