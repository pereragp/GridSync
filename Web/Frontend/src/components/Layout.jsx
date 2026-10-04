import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import BrandLogo from './BrandLogo';

export default function Layout() {
  const { user, logout, homePathFor } = useAuth();
  const year = new Date().getFullYear();
  const isOperator = user.role === 'GridOperator';
  const isBackoffice = user.role === 'Backoffice';

  return (
    <div className='flex min-h-dvh flex-col bg-grid-50 text-slate-900'>
      <div className='h-1 w-full bg-gradient-to-r from-grid-700 via-grid-500 to-emerald-400' />

      <header className='sticky top-0 z-30 border-b border-grid-100/90 bg-white/95 shadow-sm shadow-grid-900/5 backdrop-blur-md'>
        <div className='mx-auto flex max-w-6xl flex-wrap items-center gap-3 px-4 py-3.5 sm:gap-8 sm:px-6'>
          <div className='flex shrink-0 items-center gap-3'>
            <Link
              to={homePathFor(user.role)}
              className='group flex items-center gap-3 transition opacity-100 hover:opacity-90'
              aria-label='GridSync home'
            >
              <BrandLogo variant='header' />
            </Link>
          </div>

          <nav className='ml-2 flex min-w-0 flex-1 flex-wrap items-center gap-1 text-sm sm:ml-4 sm:gap-1'>
            {user.role === 'Backoffice' && (
              <>
                <NavLink className={navClass} to='/backoffice' end>
                  <NavIconHome />
                  Home
                </NavLink>
                <NavLink className={navClass} to='/backoffice/users'>
                  <NavIconUsers />
                  Users
                </NavLink>
                <NavLink className={navClass} to='/stations'>
                  <NavIconStations />
                  Stations
                </NavLink>
                <NavLink className={navClass} to='/backoffice/staff/new'>
                  <NavIconStaff />
                  Staff
                </NavLink>
                <NavLink className={navClass} to='/profile'>
                  <NavIconProfile />
                  Profile
                </NavLink>
              </>
            )}

            {isOperator && (
              <>
                <NavLink className={navClass} to='/operator' end>
                  <NavIconHome />
                  Home
                </NavLink>
                <NavLink className={navClass} to='/operator/reservations'>
                  <NavIconBookings />
                  Bookings
                </NavLink>
                <NavLink className={navClass} to='/stations'>
                  <NavIconStations />
                  Stations
                </NavLink>
                <NavLink className={navClass} to='/profile'>
                  <NavIconProfile />
                  Profile
                </NavLink>
              </>
            )}

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
                  {user.role === 'GridOperator' ? 'Grid Operator' : user.role}
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
              {isOperator ? (
                <>
                  <li>
                    <Link className='hover:text-white' to='/operator'>
                      Home
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/operator/reservations'>
                      Bookings
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/stations'>
                      Stations
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/profile'>
                      Profile
                    </Link>
                  </li>
                </>
              ) : isBackoffice ? (
                <>
                  <li>
                    <Link className='hover:text-white' to='/backoffice'>
                      Home
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/backoffice/users'>
                      Users
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/stations'>
                      Stations
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/backoffice/staff/new'>
                      Staff
                    </Link>
                  </li>
                  <li>
                    <Link className='hover:text-white' to='/profile'>
                      Profile
                    </Link>
                  </li>
                </>
              ) : (
                <>
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
                </>
              )}
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
                Role ·{' '}
                <span className='text-white'>
                  {user.role === 'GridOperator' ? 'Grid Operator' : user.role}
                </span>
              </li>
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
    'inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 transition',
    isActive
      ? 'bg-grid-100 font-semibold text-grid-800'
      : 'text-slate-600 hover:bg-grid-50 hover:text-grid-800',
  ].join(' ');
}

function NavIconHome() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path d='M10.707 2.293a1 1 0 00-1.414 0l-7 7A1 1 0 003 10.707V17a1 1 0 001 1h4a1 1 0 001-1v-3a1 1 0 011-1h2a1 1 0 011 1v3a1 1 0 001 1h4a1 1 0 001-1v-6.293a1 1 0 00.293-.707l-7-7z' />
    </svg>
  );
}

function NavIconStations() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path d='M3.5 3A1.5 1.5 0 002 4.5v3A1.5 1.5 0 003.5 9h3A1.5 1.5 0 008 7.5v-3A1.5 1.5 0 006.5 3h-3zM12.5 3A1.5 1.5 0 0011 4.5v3A1.5 1.5 0 0012.5 9h3A1.5 1.5 0 0017 7.5v-3A1.5 1.5 0 0015.5 3h-3zM3.5 11A1.5 1.5 0 002 12.5v3A1.5 1.5 0 003.5 17h3A1.5 1.5 0 008 15.5v-3A1.5 1.5 0 006.5 11h-3zM12.5 11a1.5 1.5 0 00-1.5 1.5v3a1.5 1.5 0 001.5 1.5h3a1.5 1.5 0 001.5-1.5v-3a1.5 1.5 0 00-1.5-1.5h-3z' />
    </svg>
  );
}

function NavIconUsers() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path d='M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z' />
    </svg>
  );
}

function NavIconStaff() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path d='M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a5 5 0 00-5 5v1h10v-1a5 5 0 00-5-5zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z' />
    </svg>
  );
}

function NavIconBookings() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path
        fillRule='evenodd'
        d='M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z'
        clipRule='evenodd'
      />
    </svg>
  );
}

function NavIconProfile() {
  return (
    <svg viewBox='0 0 20 20' fill='currentColor' className='h-3.5 w-3.5' aria-hidden>
      <path
        fillRule='evenodd'
        d='M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z'
        clipRule='evenodd'
      />
    </svg>
  );
}
