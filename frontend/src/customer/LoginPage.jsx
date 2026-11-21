import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';

export default function LoginPage() {
  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const [form, setForm] = useState({ email: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await login(form.email, form.password);
      showToast('Welcome back to Quick kart');
      navigate(from, { replace: true });
    } catch (err) {
      showToast(getApiErrorMessage(err, 'Invalid email or password'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-16 sm:py-20">
      <div className="border border-[#E5E5E5] p-8 bg-white">
        <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-neutral-500">Account</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight">Sign In to Quick kart</h1>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Email Address</label>
            <input
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="you@example.com"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="font-semibold uppercase tracking-wider">Password</label>
              <Link to="/forgot-password" className="text-[11px] underline text-neutral-500 hover:text-[#111111]">
                Forgot password?
              </Link>
            </div>
            <input
              type="password"
              required
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
            {submitting ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div className="mt-6 pt-5 border-t border-[#F0F0F0] text-center text-xs text-neutral-600">
          New to Quick kart?{' '}
          <Link to="/register" className="font-bold text-[#111111] underline">
            Create an Account
          </Link>
        </div>
      </div>
    </div>
  );
}
