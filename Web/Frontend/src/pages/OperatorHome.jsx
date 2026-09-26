import { useEffect, useState } from 'react';
import { getReservationDashboardStats } from '../api/reservations';
import { Alert, PageHeader, cardClass } from '../components/ui';
import { useAuth } from '../context/AuthContext';

export default function OperatorHome() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    getReservationDashboardStats()
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err.message || 'Could not load dashboard statistics');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <PageHeader
        title={`Welcome, ${user.fullName}`}
        subtitle='Grid Operator console. Station and booking tools will appear here next.'
      />
      {error ? <Alert>{error}</Alert> : null}
      <section className='mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3'>
        <StatCard
          label='Pending reservations'
          value={stats?.pendingReservations}
        />
        <StatCard
          label='Approved upcoming'
          value={stats?.approvedUpcomingReservations}
        />
        <StatCard
          label='Completed transfers'
          value={stats?.completedTransfers}
        />
        <StatCard label='Rejected' value={stats?.rejectedReservations} />
        <StatCard label='Cancelled' value={stats?.cancelledReservations} />
        <StatCard label='Expired' value={stats?.expiredReservations} />
      </section>
      <div className={cardClass}>
        <p className='text-sm text-slate-600'>
          You are signed in as <strong>{user.role}</strong>. Use Profile or
          Password in the top nav for account settings.
        </p>
      </div>
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className='rounded-lg border border-slate-200 bg-white p-5 shadow-sm'>
      <p className='text-sm text-slate-600'>{label}</p>
      <p className='mt-2 text-3xl font-semibold text-teal-800'>
        {value ?? '...'}
      </p>
    </div>
  );
}
