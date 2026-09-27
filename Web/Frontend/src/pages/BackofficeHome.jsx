import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  approveUser,
  deactivateUser,
  getPendingUsers,
  getUsers,
  reactivateUser,
} from "../api/users";
import { useAuth } from "../context/AuthContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=1800&q=80";

export default function BackofficeHome() {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [pending, setPending] = useState([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [all, pend] = await Promise.all([getUsers(), getPendingUsers()]);
      setUsers(all || []);
      setPending(pend || []);
    } catch (err) {
      setError(err.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const stats = useMemo(() => {
    const active = users.filter((u) => u.status === "Active").length;
    const deactivated = users.filter((u) => u.status === "Deactivated").length;
    const prosumers = users.filter((u) => u.role === "Prosumer").length;
    return {
      total: users.length,
      pending: pending.length,
      active,
      deactivated,
      prosumers,
    };
  }, [users, pending]);

  const filteredUsers = useMemo(() => {
    const q = query.trim().toLowerCase();
    return users.filter((u) => {
      if (roleFilter !== "All" && u.role !== roleFilter) return false;
      if (statusFilter !== "All" && u.status !== statusFilter) return false;
      if (!q) return true;
      return (
        u.fullName?.toLowerCase().includes(q) ||
        u.email?.toLowerCase().includes(q) ||
        u.nic?.toLowerCase().includes(q)
      );
    });
  }, [users, query, roleFilter, statusFilter]);

  async function runAction(id, action, successText) {
    setMessage("");
    setError("");
    setBusyId(id);
    try {
      await action();
      setMessage(successText);
      await load();
    } catch (err) {
      setError(err.message || "Action failed");
    } finally {
      setBusyId("");
    }
  }

  return (
    <div className="space-y-8">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Wind turbines and renewable energy landscape"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/45" />
        </div>

        <div className="relative z-10 flex flex-col gap-6 px-6 py-10 sm:px-10 sm:py-12 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
              Backoffice console
            </p>
            <h1 className="mt-3 font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
              User administration
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
              Approve pending prosumers, manage staff accounts, and control
              activation status across the GridSync network.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Link
              to="/stations"
              className="inline-flex items-center justify-center rounded-xl border border-white/25 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
            >
              Manage stations
            </Link>
            <Link
              to="/backoffice/staff/new"
              className="inline-flex items-center justify-center rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 shadow-lg transition hover:bg-grid-50"
            >
              Create staff user
            </Link>
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total users" value={stats.total} />
        <StatCard label="Pending approvals" value={stats.pending} accent />
        <StatCard label="Active accounts" value={stats.active} />
        <StatCard label="Prosumers" value={stats.prosumers} />
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

      {/* Pending */}
      <section className="overflow-hidden rounded-2xl border border-amber-200/80 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-amber-100 bg-amber-50/80 px-5 py-4">
          <div>
            <h2 className="text-lg font-semibold text-amber-950">
              Pending activations
            </h2>
            <p className="text-sm text-amber-900/70">
              Prosumer registrations waiting for Backoffice approval.
            </p>
          </div>
          <span className="rounded-full bg-amber-200/80 px-3 py-1 text-xs font-semibold text-amber-950">
            {pending.length} waiting
          </span>
        </div>

        <div className="p-5">
          {loading ? (
            <p className="text-sm text-slate-500">Loading pending accounts…</p>
          ) : pending.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
              No pending prosumer activations right now.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <th className="pb-3 pr-4 font-semibold">Prosumer</th>
                    <th className="pb-3 pr-4 font-semibold">NIC</th>
                    <th className="pb-3 pr-4 font-semibold">Email</th>
                    <th className="pb-3 font-semibold">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {pending.map((u) => (
                    <tr key={u.id} className="border-b border-slate-100 last:border-0">
                      <td className="py-3.5 pr-4">
                        <p className="font-medium text-slate-900">{u.fullName}</p>
                        <p className="text-xs text-slate-500">Registered account</p>
                      </td>
                      <td className="py-3.5 pr-4 text-slate-700">{u.nic || "—"}</td>
                      <td className="py-3.5 pr-4 text-slate-700">{u.email}</td>
                      <td className="py-3.5">
                        <button
                          type="button"
                          disabled={busyId === u.id}
                          className="rounded-lg bg-grid-700 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
                          onClick={() =>
                            runAction(u.id, () => approveUser(u.id), `Approved ${u.fullName}`)
                          }
                        >
                          {busyId === u.id ? "Approving…" : "Approve"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      {/* All users */}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-5 py-4">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-grid-900">All users</h2>
              <p className="text-sm text-slate-600">
                Search, filter, deactivate, or reactivate accounts.
              </p>
            </div>
            <p className="text-sm text-slate-500">
              Showing <span className="font-semibold text-grid-800">{filteredUsers.length}</span> of{" "}
              {users.length}
            </p>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <input
              type="search"
              placeholder="Search name, email, or NIC"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
            />
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
            >
              <option value="All">All roles</option>
              <option value="Backoffice">Backoffice</option>
              <option value="GridOperator">GridOperator</option>
              <option value="Prosumer">Prosumer</option>
            </select>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
            >
              <option value="All">All statuses</option>
              <option value="Active">Active</option>
              <option value="Pending">Pending</option>
              <option value="Deactivated">Deactivated</option>
            </select>
          </div>
        </div>

        <div className="p-5">
          {loading ? (
            <p className="text-sm text-slate-500">Loading users…</p>
          ) : filteredUsers.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
              No users match your filters.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <th className="pb-3 pr-4 font-semibold">User</th>
                    <th className="pb-3 pr-4 font-semibold">Role</th>
                    <th className="pb-3 pr-4 font-semibold">Status</th>
                    <th className="pb-3 pr-4 font-semibold">Contact</th>
                    <th className="pb-3 font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((u) => (
                    <tr key={u.id} className="border-b border-slate-100 align-top last:border-0">
                      <td className="py-3.5 pr-4">
                        <div className="flex items-center gap-3">
                          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-grid-100 text-xs font-semibold text-grid-800">
                            {initials(u.fullName)}
                          </span>
                          <div>
                            <p className="font-medium text-slate-900">{u.fullName}</p>
                            {u.nic ? (
                              <p className="text-xs text-slate-500">NIC {u.nic}</p>
                            ) : (
                              <p className="text-xs text-slate-400">Staff account</p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 pr-4">
                        <RoleBadge role={u.role} />
                      </td>
                      <td className="py-3.5 pr-4">
                        <StatusBadge status={u.status} />
                        {u.deactivationRequestedAt ? (
                          <p className="mt-1 text-xs font-medium text-amber-700">
                            Deactivation requested
                          </p>
                        ) : null}
                      </td>
                      <td className="py-3.5 pr-4">
                        <p className="text-slate-700">{u.email}</p>
                        <p className="text-xs text-slate-500">{u.phone || "—"}</p>
                      </td>
                      <td className="py-3.5">
                        <div className="flex flex-wrap gap-2">
                          {u.status !== "Deactivated" && u.id !== user.userId ? (
                            <button
                              type="button"
                              disabled={busyId === u.id}
                              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
                              onClick={() =>
                                runAction(
                                  u.id,
                                  () => deactivateUser(u.id),
                                  `Deactivated ${u.fullName}`
                                )
                              }
                            >
                              {busyId === u.id ? "Working…" : "Deactivate"}
                            </button>
                          ) : null}
                          {u.status === "Deactivated" ? (
                            <button
                              type="button"
                              disabled={busyId === u.id}
                              className="rounded-lg bg-grid-700 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
                              onClick={() =>
                                runAction(
                                  u.id,
                                  () => reactivateUser(u.id, user.userId),
                                  `Reactivated ${u.fullName}`
                                )
                              }
                            >
                              {busyId === u.id ? "Working…" : "Reactivate"}
                            </button>
                          ) : null}
                          {u.id === user.userId ? (
                            <span className="self-center text-xs text-slate-400">You</span>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value, accent = false }) {
  return (
    <div
      className={`rounded-2xl border p-4 shadow-sm ${
        accent
          ? "border-amber-200 bg-amber-50"
          : "border-slate-200 bg-white"
      }`}
    >
      <p className={`text-xs font-semibold uppercase tracking-wide ${accent ? "text-amber-800" : "text-slate-500"}`}>
        {label}
      </p>
      <p className={`mt-2 text-3xl font-semibold tracking-tight ${accent ? "text-amber-950" : "text-grid-900"}`}>
        {value}
      </p>
    </div>
  );
}

function StatusBadge({ status }) {
  const map = {
    Active: "bg-grid-100 text-grid-800",
    Pending: "bg-amber-100 text-amber-900",
    Deactivated: "bg-slate-200 text-slate-700",
  };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[status] || "bg-slate-100"}`}>
      {status}
    </span>
  );
}

function RoleBadge({ role }) {
  const map = {
    Backoffice: "bg-grid-800 text-white",
    GridOperator: "bg-emerald-700 text-white",
    Prosumer: "bg-grid-100 text-grid-800",
  };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[role] || "bg-slate-100"}`}>
      {role}
    </span>
  );
}

function initials(name = "") {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() || "")
      .join("") || "U"
  );
}
