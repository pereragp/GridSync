import { useCallback, useEffect, useState } from 'react';
import { jsPDF } from 'jspdf';
import { QRCodeCanvas } from 'qrcode.react';
import {
  cancelReservation,
  createReservation,
  getAvailableBookingSlots,
  getReservationHistory,
  getUpcomingReservations,
  updateReservation,
} from '../api/reservations';
import {
  Alert,
  Field,
  PageHeader,
  btnPrimary,
  btnSecondary,
  cardClass,
  inputClass,
} from '../components/ui';

const statuses = [
  '',
  'Pending',
  'Approved',
  'Rejected',
  'Cancelled',
  'Expired',
];
const reservationTypes = ['', 'Charging', 'DropOff'];

export default function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [upcomingReservations, setUpcomingReservations] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [reservationType, setReservationType] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [form, setForm] = useState({ slotId: '', reservationType: 'Charging' });
  const [editing, setEditing] = useState({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState('');

  useEffect(() => {
    let cancelled = false;
    getAvailableBookingSlots()
      .then((data) => {
        if (!cancelled) setAvailableSlots(data || []);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err.message || 'Failed to load available slots');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const load = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      const [history, upcoming] = await Promise.all([
        getReservationHistory(status),
        getUpcomingReservations(status),
      ]);
      setReservations(history || []);
      setUpcomingReservations(upcoming || []);
    } catch (err) {
      setError(err.message || 'Failed to load reservations');
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      getReservationHistory(status),
      getUpcomingReservations(status),
    ])
      .then(([history, upcoming]) => {
        if (!cancelled) {
          setReservations(history || []);
          setUpcomingReservations(upcoming || []);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load reservations');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [status]);

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
      : '';
    const matchesFromDate = !fromDate || (slotDate && slotDate >= fromDate);
    const matchesToDate = !toDate || (slotDate && slotDate <= toDate);

    return matchesSearch && matchesType && matchesFromDate && matchesToDate;
  }

  const filteredUpcomingReservations =
    upcomingReservations.filter(matchesFilters);
  const filteredReservations = reservations.filter(matchesFilters);

  function clearFeedback() {
    setError('');
    setMessage('');
  }

  async function onCreate(e) {
    e.preventDefault();
    clearFeedback();
    setSubmitting(true);
    try {
      await createReservation(form);
      setMessage('Reservation created and is pending review.');
      setForm({ slotId: '', reservationType: 'Charging' });
      await load();
    } catch (err) {
      setError(err.message || 'Failed to create reservation');
    } finally {
      setSubmitting(false);
    }
  }

  async function onUpdate(reservation) {
    clearFeedback();
    setActionId(reservation.id);
    try {
      await updateReservation(reservation.id, {
        reservationType: editing[reservation.id],
      });
      setMessage(`Reservation ${reservation.reservationCode} updated.`);
      await load();
    } catch (err) {
      setError(err.message || 'Failed to update reservation');
    } finally {
      setActionId('');
    }
  }

  async function onCancel(reservation) {
    const reason = window.prompt('Optional cancellation reason:', '');
    if (reason === null) return;

    clearFeedback();
    setActionId(reservation.id);
    try {
      await cancelReservation(reservation.id, reason);
      setMessage(`Reservation ${reservation.reservationCode} cancelled.`);
      await load();
    } catch (err) {
      setError(err.message || 'Failed to cancel reservation');
    } finally {
      setActionId('');
    }
  }

  return (
    <div>
      <PageHeader
        title='Reservations'
        subtitle='Review your reservation history or submit a booking for an available slot.'
      />
      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type='success'>{message}</Alert> : null}

      <section className={`${cardClass} mb-6`}>
        <h2 className='mb-1 text-lg font-medium'>Create reservation</h2>
        <p className='mb-4 text-sm text-slate-600'>
          Choose an available booking slot within the next 7 days.
        </p>
        <form
          onSubmit={onCreate}
          className='grid gap-3 md:grid-cols-[1fr_12rem_auto] md:items-end'
        >
          <Field label='Available booking slot'>
            <select
              className={inputClass}
              value={form.slotId}
              onChange={(e) => setForm({ ...form, slotId: e.target.value })}
              required
              disabled={availableSlots.length === 0}
            >
              <option value=''>
                {availableSlots.length ? 'Select a slot' : 'No available slots'}
              </option>
              {availableSlots.map((slot) => (
                <option key={slot.id} value={slot.id}>
                  {slot.stationName} - {formatDate(slot.slotStart)} (
                  {slot.energyKwh} kWh, {slot.availableReservations} available)
                </option>
              ))}
            </select>
          </Field>
          <Field label='Reservation type'>
            <select
              className={inputClass}
              value={form.reservationType}
              onChange={(e) =>
                setForm({ ...form, reservationType: e.target.value })
              }
            >
              <option value='Charging'>Charging</option>
              <option value='DropOff'>Drop-off</option>
            </select>
          </Field>
          <button className={`${btnPrimary} mb-3`} disabled={submitting}>
            {submitting ? 'Creating…' : 'Create reservation'}
          </button>
        </form>
      </section>

      <section className={`${cardClass} mb-6`}>
        <div className='mb-4 flex flex-wrap items-center justify-between gap-3'>
          <h2 className='text-lg font-medium'>Upcoming reservations</h2>
          <span className='text-sm text-slate-500'>
            Showing {filteredUpcomingReservations.length} of{' '}
            {upcomingReservations.length}
          </span>
        </div>
        <div className='mb-4 grid gap-3 md:grid-cols-2 lg:grid-cols-4'>
          <label className='text-sm'>
            <span className='mb-1 block font-medium text-slate-700'>
              Search
            </span>
            <input
              className={inputClass}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder='Code or station'
            />
          </label>
          <label className='text-sm'>
            <span className='mb-1 block font-medium text-slate-700'>
              Status
            </span>
            <select
              className={inputClass}
              value={status}
              onChange={(e) => {
                setLoading(true);
                setStatus(e.target.value);
              }}
            >
              {statuses.map((value) => (
                <option key={value} value={value}>
                  {value || 'All statuses'}
                </option>
              ))}
            </select>
          </label>
          <label className='text-sm'>
            <span className='mb-1 block font-medium text-slate-700'>Type</span>
            <select
              className={inputClass}
              value={reservationType}
              onChange={(e) => setReservationType(e.target.value)}
            >
              {reservationTypes.map((value) => (
                <option key={value} value={value}>
                  {value === 'DropOff' ? 'Drop-off' : value || 'All types'}
                </option>
              ))}
            </select>
          </label>
          <div className='grid grid-cols-2 gap-2'>
            <label className='text-sm'>
              <span className='mb-1 block font-medium text-slate-700'>
                From
              </span>
              <input
                className={inputClass}
                type='date'
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </label>
            <label className='text-sm'>
              <span className='mb-1 block font-medium text-slate-700'>To</span>
              <input
                className={inputClass}
                type='date'
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </label>
          </div>
        </div>
        {loading ? <p className='text-sm text-slate-500'>Loading…</p> : null}
        {!loading && filteredUpcomingReservations.length === 0 ? (
          <p className='text-sm text-slate-500'>
            No upcoming reservations found for this filter.
          </p>
        ) : null}
        <div className='space-y-3'>
          {filteredUpcomingReservations.map((reservation) => (
            <ReservationItem
              key={reservation.id}
              reservation={reservation}
              value={editing[reservation.id] ?? reservation.reservationType}
              onChange={(value) =>
                setEditing({ ...editing, [reservation.id]: value })
              }
              onUpdate={() => onUpdate(reservation)}
              onCancel={() => onCancel(reservation)}
              busy={actionId === reservation.id}
            />
          ))}
        </div>
      </section>

      <section className={cardClass}>
        <div className='mb-4 flex flex-wrap items-center justify-between gap-3'>
          <h2 className='text-lg font-medium'>Booking history</h2>
          <span className='text-sm text-slate-500'>
            Showing {filteredReservations.length} of {reservations.length}
          </span>
        </div>
        {!loading && filteredReservations.length === 0 ? (
          <p className='text-sm text-slate-500'>
            No past reservations found for this filter.
          </p>
        ) : null}
        <div className='space-y-3'>
          {filteredReservations.map((reservation) => (
            <ReservationItem
              key={reservation.id}
              reservation={reservation}
              value={editing[reservation.id] ?? reservation.reservationType}
              onChange={(value) =>
                setEditing({ ...editing, [reservation.id]: value })
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
  value,
  onChange,
  onUpdate,
  onCancel,
  busy,
}) {
  const editable = reservation.status === 'Pending';
  return (
    <article className='border-t border-slate-200 pt-3 first:border-t-0 first:pt-0'>
      <div className='flex flex-wrap items-start justify-between gap-3'>
        <div>
          <p className='font-medium text-slate-900'>
            {reservation.reservationCode || reservation.id}
          </p>
          <p className='text-sm text-slate-600'>
            {reservation.stationName || 'Station unavailable'}
          </p>
          <p className='text-sm text-slate-600'>
            {formatDate(reservation.slotStart)} -{' '}
            {formatDate(reservation.slotEnd)}
          </p>
        </div>
        <StatusBadge status={reservation.status} />
      </div>
      <div className='mt-3 flex flex-wrap items-end gap-3'>
        <label className='text-sm'>
          <span className='mb-1 block font-medium text-slate-700'>Type</span>
          <select
            className={`${inputClass} w-36`}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            disabled={!editable || busy}
          >
            <option value='Charging'>Charging</option>
            <option value='DropOff'>Drop-off</option>
          </select>
        </label>
        {editable ? (
          <>
            <button
              type='button'
              className={btnSecondary}
              onClick={onUpdate}
              disabled={busy}
            >
              Save type
            </button>
            <button
              type='button'
              className='inline-flex items-center justify-center rounded-md border border-red-200 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50 disabled:opacity-60'
              onClick={onCancel}
              disabled={busy}
            >
              Cancel reservation
            </button>
          </>
        ) : null}
      </div>
      {reservation.status === 'Rejected' && reservation.rejectionReason ? (
        <p className='mt-2 text-sm text-red-700'>
          Reason: {reservation.rejectionReason}
        </p>
      ) : null}
      {reservation.status === 'Cancelled' && reservation.cancellationReason ? (
        <p className='mt-2 text-sm text-slate-600'>
          Cancellation reason: {reservation.cancellationReason}
        </p>
      ) : null}
      {reservation.status === 'Approved' && reservation.qrPayload ? (
        <div className='mt-4 flex flex-wrap items-center gap-4 rounded-md border border-teal-100 bg-teal-50 p-3'>
          <QRCodeCanvas
            id={`reservation-qr-${reservation.id}`}
            value={reservation.qrPayload}
            size={144}
            includeMargin
            aria-label={`QR code for ${reservation.reservationCode || 'reservation'}`}
          />
          <div className='text-sm text-teal-900'>
            <p className='font-medium'>Transaction QR code</p>
            <p className='mt-1 text-teal-800'>
              Show this code to the grid operator at the station.
            </p>
            {reservation.qrGeneratedAt ? (
              <p className='mt-1 text-xs text-teal-700'>
                Generated {formatDate(reservation.qrGeneratedAt)}
              </p>
            ) : null}
            <button
              type='button'
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
    Pending: 'bg-amber-50 text-amber-800',
    Approved: 'bg-teal-50 text-teal-800',
    Rejected: 'bg-red-50 text-red-800',
    Cancelled: 'bg-slate-200 text-slate-700',
    Expired: 'bg-slate-100 text-slate-700',
  };
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-medium ${colors[status] || 'bg-slate-100 text-slate-700'}`}
    >
      {status}
    </span>
  );
}

function formatDate(value) {
  if (!value) return 'Unknown time';
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function downloadReservationPdf(reservation) {
  const qrCanvas = document.getElementById(`reservation-qr-${reservation.id}`);
  if (!qrCanvas) return;

  const code = reservation.reservationCode || reservation.id;
  const documentFile = new jsPDF();
  documentFile.setFontSize(20);
  documentFile.text('GridSync Reservation', 20, 24);
  documentFile.setFontSize(12);
  documentFile.text(`Reservation: ${code}`, 20, 38);
  documentFile.text(
    `Station: ${reservation.stationName || 'Unavailable'}`,
    20,
    48,
  );
  documentFile.text(`Type: ${reservation.reservationType}`, 20, 58);
  documentFile.text(`Energy: ${reservation.energyKwh} kWh`, 20, 68);
  documentFile.text(`Start: ${formatDate(reservation.slotStart)}`, 20, 78);
  documentFile.text(`End: ${formatDate(reservation.slotEnd)}`, 20, 88);
  documentFile.text('Present this QR code to the grid operator.', 20, 104);
  documentFile.addImage(
    qrCanvas.toDataURL('image/png'),
    'PNG',
    20,
    114,
    55,
    55,
  );
  documentFile.save(`${code}.pdf`);
}
