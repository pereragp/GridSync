import { useEffect, useState } from "react";
import { getUser, updateUser } from "../api/users";
import { useAuth } from "../context/AuthContext";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";

export default function Profile() {
  const { user } = useAuth();
  const [form, setForm] = useState({ fullName: "", phone: "", address: "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getUser(user.userId);
        if (!cancelled) {
          setForm({
            fullName: data.fullName || "",
            phone: data.phone || "",
            address: data.address || "",
          });
        }
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load profile");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user.userId]);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await updateUser(user.userId, form);
      setSuccess("Profile updated.");
    } catch (err) {
      setError(err.message || "Update failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg">
      <PageHeader title="Profile" subtitle={`${user.email} · ${user.role} · ${user.status}`} />
      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Full name">
          <input className={inputClass} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
        </Field>
        <Field label="Phone">
          <input className={inputClass} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required />
        </Field>
        <Field label="Address">
          <input className={inputClass} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
        <button className={btnPrimary} disabled={loading}>
          {loading ? "Saving…" : "Save changes"}
        </button>
      </form>
    </div>
  );
}
