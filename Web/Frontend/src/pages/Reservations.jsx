import { useCallback, useEffect, useState } from 'react';
import {
  cancelReservation,
  createReservation,
  getReservationHistory,
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

const statuses = ['', 'Pending', 'Approved', 'Rejected', 'Cancelled'];

export default function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [status, setStatus] = useState('');
  const [form, setForm] = useState({ slotId: '', reservationType: 'Charging' });
  const [editing, setEditing] = useState({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState('');

  const load = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      setReservations((await getReservationHistory(status)) || []);
    } catch (err) {
      setError(err.message || 'Failed to load reservations');
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    let cancelled = false;
    getReservationHistory(status)
      .then((data) => {
        if (!cancelled) setReservations(data || []);
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
          Enter the booking slot ID supplied by the grid operator.
        </p>
        <form
          onSubmit={onCreate}
          className='grid gap-3 md:grid-cols-[1fr_12rem_auto] md:items-end'
        >
          <Field label='Booking slot ID'>
            <input
              className={inputClass}
              value={form.slotId}
              onChange={(e) => setForm({ ...form, slotId: e.target.value })}
              placeholder='MongoDB slot ID'
              required
            />
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

      <section className={cardClass}>
        <div className='mb-4 flex flex-wrap items-center justify-between gap-3'>
          <h2 className='text-lg font-medium'>Reservation history</h2>
          <select
            className={`${inputClass} w-auto`}
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
        </div>
        {loading ? <p className='text-sm text-slate-500'>Loading…</p> : null}
        {!loading && reservations.length === 0 ? (
          <p className='text-sm text-slate-500'>
            No reservations found for this filter.
          </p>
        ) : null}
        <div className='space-y-3'>
          {reservations.map((reservation) => (
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
    </article>
  );
}

function StatusBadge({ status }) {
  const colors = {
    Pending: 'bg-amber-50 text-amber-800',
    Approved: 'bg-teal-50 text-teal-800',
    Rejected: 'bg-red-50 text-red-800',
    Cancelled: 'bg-slate-200 text-slate-700',
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
