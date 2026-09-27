import { useCallback, useEffect, useMemo, useState } from "react";
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
  PageHeader,
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
  const [editing, setEditing] = useState({});
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState("");

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

  const stations = useMemo(() => {
    const byId = new Map();
    for (const slot of availableSlots) {
      if (!slot.stationId || byId.has(slot.stationId)) continue;
      byId.set(slot.stationId, {
        id: slot.stationId,
        name: slot.stationName || "Unknown station",
      });
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
    <div>
      <PageHeader
        title="Reservations"
        subtitle="Book Charging or Drop-off energy against a station battery, then track approvals and QR codes."
      />
      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type="success">{message}</Alert> : null}

      <section className={`${cardClass} mb-6`}>
        <h2 className="mb-1 text-lg font-medium">Create reservation</h2>
        <p className="mb-4 text-sm text-slate-600">
          Choose a station, then pick an available battery. Select Charging
          (deposit energy) or Drop-off (withdraw energy), enter kWh, and set your
          visit window within the next 7 days.
        </p>
        <form onSubmit={onCreate} className="grid gap-3 md:grid-cols-2">
          <Field label="Station">
            <select
              className={inputClass}
              value={form.stationId}
              onChange={(e) =>
                setForm({
                  ...form,
                  stationId: e.target.value,
                  slotId: "",
                })
              }
              required
              disabled={stations.length === 0}
            >
              <option value="">
                {stations.length
                  ? "Select a station"
                  : "No stations with available batteries"}
              </option>
              {stations.map((station) => (
                <option key={station.id} value={station.id}>
                  {station.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Battery">
            <select
              className={inputClass}
              value={form.slotId}
              onChange={(e) => setForm({ ...form, slotId: e.target.value })}
              required
              disabled={!form.stationId || batteriesForStation.length === 0}
            >
              <option value="">
                {!form.stationId
                  ? "Select a station first"
                  : batteriesForStation.length
                    ? "Select a battery"
                    : "No batteries available at this station"}
              </option>
              {batteriesForStation.map((slot) => (
                <option key={slot.id} value={slot.id}>
                  Battery #{slot.batteryIndex} · Cap {slot.capacityKwh} kWh ·
                  Charge avail {Number(slot.availableChargingKwh).toFixed(1)} ·
                  Drop-off avail {Number(slot.availableDropOffKwh).toFixed(1)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Reservation type">
            <select
              className={inputClass}
              value={form.reservationType}
              onChange={(e) =>
                setForm({ ...form, reservationType: e.target.value })
              }
            >
              <option value="Charging">Charging (deposit into battery)</option>
              <option value="DropOff">Drop-off (withdraw stored energy)</option>
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
              onChange={(e) => setForm({ ...form, energyKwh: e.target.value })}
              required
              placeholder="e.g. 5"
            />
          </Field>
          {selectedSlot ? (
            <div className="mb-3 self-end rounded-lg border border-grid-100 bg-grid-50 px-3 py-2 text-sm text-slate-700 md:col-span-2">
              <p className="font-medium text-grid-900">
                {selectedSlot.stationName} · Battery #{selectedSlot.batteryIndex}
              </p>
              <p>
                Actual stored:{" "}
                <strong>{Number(selectedSlot.actualEnergyKwh).toFixed(2)} kWh</strong>
                {" · "}
                Capacity:{" "}
                <strong>{Number(selectedSlot.capacityKwh).toFixed(2)} kWh</strong>
              </p>
            </div>
          ) : null}
          <Field label="Visit start">
            <input
              className={inputClass}
              type="datetime-local"
              value={form.slotStart}
              onChange={(e) => setForm({ ...form, slotStart: e.target.value })}
              required
            />
          </Field>
          <Field label="Visit end">
            <input
              className={inputClass}
              type="datetime-local"
              value={form.slotEnd}
              onChange={(e) => setForm({ ...form, slotEnd: e.target.value })}
              required
            />
          </Field>
          <div className="md:col-span-2">
            <button className={btnPrimary} disabled={submitting}>
              {submitting ? "Creating…" : "Create reservation"}
            </button>
          </div>
        </form>
      </section>

      <section className={`${cardClass} mb-6`}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium">Upcoming reservations</h2>
          <span className="text-sm text-slate-500">
            Showing {filteredUpcomingReservations.length} of{" "}
            {upcomingReservations.length}
          </span>
        </div>
        <div className="mb-4 grid gap-3 md:grid-cols-2 lg:grid-cols-4">
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
        {loading ? <p className="text-sm text-slate-500">Loading…</p> : null}
        {!loading && filteredUpcomingReservations.length === 0 ? (
          <p className="text-sm text-slate-500">
            No upcoming reservations found for this filter.
          </p>
        ) : null}
        <div className="space-y-3">
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

      <section className={cardClass}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium">Booking history</h2>
          <span className="text-sm text-slate-500">
            Showing {filteredReservations.length} of {reservations.length}
          </span>
        </div>
        {!loading && filteredReservations.length === 0 ? (
          <p className="text-sm text-slate-500">
            No past reservations found for this filter.
          </p>
        ) : null}
        <div className="space-y-3">
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

  return (
    <article className="border-t border-slate-200 pt-3 first:border-t-0 first:pt-0">
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
        <StatusBadge status={reservation.status} />
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
      {reservation.status === "Approved" && reservation.qrPayload ? (
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
