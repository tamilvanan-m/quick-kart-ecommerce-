import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';

export default function AdminLoginPage() {
  const { login, logout } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const user = await login(form.email, form.password);
      const allowed = user?.roles?.some((r) => ['OWNER', 'ADMIN', 'STAFF'].includes(r));
      if (!allowed) {
        logout();
        showToast('Access denied.', 'error');
        navigate('/', { replace: true });
        return;
      }
      showToast('Signed in to Store Admin');
      navigate('/admin', { replace: true });
    } catch (err) {
      showToast(getApiErrorMessage(err, 'Invalid credentials'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F5F5] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full bg-white border border-[#E5E5E5] p-8">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-neutral-500">
          <ShieldCheck className="w-4 h-4 text-[#111111]" /> Quick kart Management
        </div>
        <h1 className="mt-2 text-2xl font-bold tracking-tight">Store Administration</h1>
        <p className="mt-1 text-xs text-neutral-500">
          Authorized personnel only.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Email</label>
            <input
              type="email"
              required
              autoComplete="username"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="Enter authorized email"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Password</label>
            <input
              type="password"
              required
              autoComplete="current-password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Enter password"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-[#111111] text-white py-3.5 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
          >
            {submitting ? 'Verifying...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
}
