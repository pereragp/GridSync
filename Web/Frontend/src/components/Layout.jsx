import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import BrandLogo from './BrandLogo';

export default function Layout() {
  const { user, logout, homePathFor } = useAuth();
  const year = new Date().getFullYear();

  return (
    <div className='flex min-h-dvh flex-col bg-grid-50 text-slate-900'>
      {/* Top sustainability accent */}
      <div className='h-1 w-full bg-gradient-to-r from-grid-700 via-grid-500 to-emerald-400' />

      <header className='sticky top-0 z-30 border-b border-grid-100/90 bg-white/95 shadow-sm shadow-grid-900/5 backdrop-blur-md'>
        <div className='mx-auto flex max-w-6xl flex-wrap items-center gap-3 px-4 py-3.5 sm:gap-10 sm:px-6'>
          <div className='flex shrink-0 items-center gap-3'>
            <Link
              to={homePathFor(user.role)}
              className='group flex items-center gap-3 transition opacity-100 hover:opacity-90'
              aria-label='GridSync home'
            >
              <BrandLogo variant='header' />
            </Link>
          </div>

          <nav className='ml-2 flex min-w-0 flex-1 flex-wrap items-center gap-1 text-sm sm:ml-4 sm:gap-1.5'>
            {user.role === 'Backoffice' && (
              <>
                <NavLink className={navClass} to='/backoffice'>
                  Users
                </NavLink>
                <NavLink className={navClass} to='/backoffice/reservations'>
                  Reservations
                </NavLink>
                <NavLink className={navClass} to="/stations">
                  Stations
                </NavLink>
                <NavLink className={navClass} to='/backoffice/staff/new'>
                  Create staff
                </NavLink>
              </>
            )}
            {user.role === 'GridOperator' && (
              <>
                <NavLink className={navClass} to='/operator'>
                  Home
                </NavLink>
                <NavLink className={navClass} to='/operator/reservations'>
                  Reservations
                </NavLink>
                <NavLink className={navClass} to='/operator/qr-scanner'>
                  Verify QR
                </NavLink>
              </>
            )}
            {user.role === 'Prosumer' && (
              <>
                <NavLink className={navClass} to='/prosumer'>
                  Dashboard
                </NavLink>
                <NavLink className={navClass} to='/reservations'>
                  Reservations
                </NavLink>
                <NavLink className={navClass} to="/stations">
                  Stations
                </NavLink>
              </>
            )}
            <NavLink className={navClass} to='/profile'>
              Profile
            </NavLink>
            <NavLink className={navClass} to='/change-password'>
              Password
            </NavLink>
          </nav>

          <div className='ml-auto flex shrink-0 items-center gap-2'>
            <div className='flex items-center gap-2 rounded-full border border-grid-100 bg-grid-50 py-1 pl-1 pr-2.5'>
              <span className='flex h-7 w-7 items-center justify-center rounded-full bg-grid-700 text-[11px] font-semibold text-white'>
                {initials(user.fullName)}
              </span>
              <div className='hidden leading-tight sm:block'>
                <p className='max-w-[9rem] truncate text-xs font-semibold text-grid-900'>
                  {user.fullName}
                </p>
                <p className='text-[10px] font-medium uppercase tracking-wide text-grid-600'>
                  {user.role}
                </p>
              </div>
            </div>

            <button
              type='button'
              onClick={() => logout()}
              title='Logout'
              aria-label='Logout'
              className='inline-flex h-9 w-9 items-center justify-center rounded-lg border border-grid-200 bg-white text-grid-800 transition hover:border-grid-400 hover:bg-grid-50'
            >
              <LogoutIcon />
            </button>
          </div>
        </div>
      </header>

      <main className='mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6'>
        <Outlet />
      </main>

      <footer className='mt-auto border-t border-grid-800/20 bg-grid-900 text-grid-100'>
        <div className='mx-auto grid max-w-6xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-3'>
          <div>
            <BrandLogo variant='footer' className='brightness-110' />
            <p className='mt-3 max-w-xs text-sm leading-relaxed text-grid-100/70'>
              Synchronizing solar microgrids, energy slots, and prosumer trading
              for a cleaner grid.
            </p>
          </div>

          <div>
            <p className='text-xs font-semibold uppercase tracking-[0.18em] text-grid-100/50'>
              Workspace
            </p>
            <ul className='mt-3 space-y-2 text-sm text-grid-100/80'>
              <li>
                <Link className='hover:text-white' to={homePathFor(user.role)}>
                  Dashboard
                </Link>
              </li>
              <li>
                <Link className='hover:text-white' to='/profile'>
                  Profile
                </Link>
              </li>
              <li>
                <Link className='hover:text-white' to='/change-password'>
                  Security
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <p className='text-xs font-semibold uppercase tracking-[0.18em] text-grid-100/50'>
              Session
            </p>
            <ul className='mt-3 space-y-2 text-sm text-grid-100/80'>
              <li>
                Signed in as <span className='text-white'>{user.email}</span>
              </li>
              <li>
                Role · <span className='text-white'>{user.role}</span>
              </li>
              <li>JWT-secured console</li>
            </ul>
          </div>
        </div>

        <div className='border-t border-white/10'>
          <div className='mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-4 text-xs text-grid-100/55 sm:px-6'>
            <p>© {year} GridSync · Smart Solar Microgrid Trading System</p>
            <p>Renewable energy operations platform</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function initials(name = '') {
  return (
    name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() || '')
      .join('') || 'GS'
  );
}

function LogoutIcon() {
  return (
    <svg
      viewBox='0 0 20 20'
      fill='currentColor'
      className='h-4 w-4'
      aria-hidden
    >
      <path
        fillRule='evenodd'
        d='M3 4.25A2.25 2.25 0 015.25 2h5.5A2.25 2.25 0 0113 4.25v2a.75.75 0 01-1.5 0v-2a.75.75 0 00-.75-.75h-5.5a.75.75 0 00-.75.75v11.5c0 .414.336.75.75.75h5.5a.75.75 0 00.75-.75v-2a.75.75 0 011.5 0v2A2.25 2.25 0 0110.75 18h-5.5A2.25 2.25 0 013 15.75V4.25z'
        clipRule='evenodd'
      />
      <path
        fillRule='evenodd'
        d='M6 10a.75.75 0 01.75-.75h9.69l-1.22-1.22a.75.75 0 111.06-1.06l2.5 2.5a.75.75 0 010 1.06l-2.5 2.5a.75.75 0 11-1.06-1.06l1.22-1.22H6.75A.75.75 0 016 10z'
        clipRule='evenodd'
      />
    </svg>
  );
}

function navClass({ isActive }) {
  return [
    'rounded-lg px-2.5 py-1.5 transition',
    isActive
      ? 'bg-grid-100 font-semibold text-grid-800'
      : 'text-slate-600 hover:bg-grid-50 hover:text-grid-800',
  ].join(' ');
}
