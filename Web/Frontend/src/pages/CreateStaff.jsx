import { useState } from "react";
import { createStaff } from "../api/users";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";

export default function CreateStaff() {
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    password: "",
    role: "GridOperator",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      const user = await createStaff(form);
      setSuccess(`Created ${user.role}: ${user.fullName} (${user.email})`);
      setForm({ fullName: "", email: "", phone: "", password: "", role: "GridOperator" });
    } catch (err) {
      setError(err.message || "Create failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg">
      <PageHeader title="Create staff user" subtitle="Backoffice or Grid Operator accounts." />
      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Full name">
          <input className={inputClass} value={form.fullName} onChange={(e) => set("fullName", e.target.value)} required />
        </Field>
        <Field label="Email">
          <input className={inputClass} type="email" value={form.email} onChange={(e) => set("email", e.target.value)} required />
        </Field>
        <Field label="Phone">
          <input className={inputClass} value={form.phone} onChange={(e) => set("phone", e.target.value)} required />
        </Field>
        <Field label="Role">
          <select className={inputClass} value={form.role} onChange={(e) => set("role", e.target.value)}>
            <option value="Backoffice">Backoffice</option>
            <option value="GridOperator">GridOperator</option>
          </select>
        </Field>
        <Field label="Password">
          <input className={inputClass} type="password" value={form.password} onChange={(e) => set("password", e.target.value)} required />
        </Field>
        <button className={btnPrimary} disabled={loading}>
          {loading ? "Creating…" : "Create user"}
        </button>
      </form>
    </div>
  );
}
