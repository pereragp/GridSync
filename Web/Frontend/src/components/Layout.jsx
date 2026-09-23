import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { user, logout, homePathFor } = useAuth();

  return (
    <div className='min-h-screen bg-slate-100 text-slate-900'>
      <header className='border-b border-slate-200 bg-white'>
        <div className='mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3'>
          <Link
            to={homePathFor(user.role)}
            className='text-lg font-semibold tracking-tight text-teal-800'
          >
            GridSync
          </Link>
          <nav className='flex flex-wrap items-center gap-3 text-sm'>
            {user.role === 'Backoffice' && (
              <>
                <NavLink className={navClass} to='/backoffice'>
                  Users
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
              </>
            )}
            {user.role === 'Prosumer' && (
              <NavLink className={navClass} to='/reservations'>
                Reservations
              </NavLink>
            )}
            <NavLink className={navClass} to='/profile'>
              Profile
            </NavLink>
            <NavLink className={navClass} to='/change-password'>
              Password
            </NavLink>
            <span className='rounded-full bg-teal-50 px-2.5 py-1 text-xs font-medium text-teal-800'>
              {user.role}
            </span>
            <button
              type='button'
              onClick={() => logout()}
              className='rounded-md bg-slate-800 px-3 py-1.5 text-white hover:bg-slate-700'
            >
              Logout
            </button>
          </nav>
        </div>
      </header>
      <main className='mx-auto max-w-6xl px-4 py-8'>
        <Outlet />
      </main>
    </div>
  );
}

function navClass({ isActive }) {
  return isActive
    ? 'font-medium text-teal-700'
    : 'text-slate-600 hover:text-teal-700';
}
