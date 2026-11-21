import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';

export default function RegisterPage() {
  const { register } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      showToast('Passwords do not match.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        password: form.password,
      });
      showToast('Account created! Welcome to Quick kart.');
      navigate(from, { replace: true });
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-14 sm:py-20">
      <div className="border border-[#E5E5E5] p-8 bg-white">
        <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-neutral-500">Join Quick kart</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight">Create Customer Account</h1>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Full Name *</label>
            <input
              required
              minLength={2}
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              placeholder="Your full name"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Email Address *</label>
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
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Phone Number</label>
            <input
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              placeholder="+91 98765 43210"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Password *</label>
            <input
              type="password"
              required
              minLength={6}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Minimum 6 characters"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider mb-1.5">Confirm Password *</label>
            <input
              type="password"
              required
              minLength={6}
              value={form.confirmPassword}
              onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              placeholder="Re-enter password"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-[#111111] text-white py-3.5 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
          >
            {submitting ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>

        <div className="mt-6 pt-5 border-t border-[#F0F0F0] text-center text-xs text-neutral-600">
          Already have an account?{' '}
          <Link to="/login" className="font-bold text-[#111111] underline">
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
