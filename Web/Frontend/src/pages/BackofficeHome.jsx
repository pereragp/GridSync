import { useCallback, useEffect, useState } from "react";
import {
  approveUser,
  deactivateUser,
  getPendingUsers,
  getUsers,
  reactivateUser,
} from "../api/users";
import { useAuth } from "../context/AuthContext";
import { Alert, PageHeader, btnSecondary, cardClass } from "../components/ui";

export default function BackofficeHome() {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [pending, setPending] = useState([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

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

  async function runAction(action, successText) {
    setMessage("");
    setError("");
    try {
      await action();
      setMessage(successText);
      await load();
    } catch (err) {
      setError(err.message || "Action failed");
    }
  }

  return (
    <div>
      <PageHeader title="User management" subtitle="Approve pending prosumers and manage account status." />
      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type="success">{message}</Alert> : null}

      <section className={`${cardClass} mb-6`}>
        <h2 className="mb-3 text-lg font-medium">Pending activations ({pending.length})</h2>
        {loading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : pending.length === 0 ? (
          <p className="text-sm text-slate-500">No pending prosumers.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-slate-200 text-slate-500">
                <tr>
                  <th className="py-2 pr-3">Name</th>
                  <th className="py-2 pr-3">NIC</th>
                  <th className="py-2 pr-3">Email</th>
                  <th className="py-2">Action</th>
                </tr>
              </thead>
              <tbody>
                {pending.map((u) => (
                  <tr key={u.id} className="border-b border-slate-100">
                    <td className="py-2 pr-3">{u.fullName}</td>
                    <td className="py-2 pr-3">{u.nic || "—"}</td>
                    <td className="py-2 pr-3">{u.email}</td>
                    <td className="py-2">
                      <button
                        type="button"
                        className={btnSecondary}
                        onClick={() => runAction(() => approveUser(u.id), `Approved ${u.fullName}`)}
                      >
                        Approve
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className={cardClass}>
        <h2 className="mb-3 text-lg font-medium">All users ({users.length})</h2>
        {loading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-slate-200 text-slate-500">
                <tr>
                  <th className="py-2 pr-3">Name</th>
                  <th className="py-2 pr-3">Role</th>
                  <th className="py-2 pr-3">Status</th>
                  <th className="py-2 pr-3">Email</th>
                  <th className="py-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-slate-100 align-top">
                    <td className="py-2 pr-3">
                      <div>{u.fullName}</div>
                      {u.nic ? <div className="text-xs text-slate-500">NIC {u.nic}</div> : null}
                    </td>
                    <td className="py-2 pr-3">{u.role}</td>
                    <td className="py-2 pr-3">
                      <StatusBadge status={u.status} />
                      {u.deactivationRequestedAt ? (
                        <div className="mt-1 text-xs text-amber-700">Deactivation requested</div>
                      ) : null}
                    </td>
                    <td className="py-2 pr-3">{u.email}</td>
                    <td className="py-2">
                      <div className="flex flex-wrap gap-2">
                        {u.status !== "Deactivated" && u.id !== user.userId ? (
                          <button
                            type="button"
                            className={btnSecondary}
                            onClick={() =>
                              runAction(() => deactivateUser(u.id), `Deactivated ${u.fullName}`)
                            }
                          >
                            Deactivate
                          </button>
                        ) : null}
                        {u.status === "Deactivated" ? (
                          <button
                            type="button"
                            className={btnSecondary}
                            onClick={() =>
                              runAction(
                                () => reactivateUser(u.id, user.userId),
                                `Reactivated ${u.fullName}`
                              )
                            }
                          >
                            Reactivate
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function StatusBadge({ status }) {
  const map = {
    Active: "bg-teal-50 text-teal-800",
    Pending: "bg-amber-50 text-amber-800",
    Deactivated: "bg-slate-200 text-slate-700",
  };
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${map[status] || "bg-slate-100"}`}>
      {status}
    </span>
  );
}
