import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../services/customerApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';

export default function ForgotPasswordPage() {
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [step, setStep] = useState('request');
  const [email, setEmail] = useState('');
  const [resetForm, setResetForm] = useState({ token: '', newPassword: '' });
  const [busy, setBusy] = useState(false);

  const handleRequest = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await authApi.forgotPassword(email);
      showToast(res.message);
      setStep('reset');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await authApi.resetPassword(resetForm);
      showToast(res.message);
      navigate('/login');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-16 sm:py-20">
      <div className="border border-[#E5E5E5] p-8 bg-white">
        <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-neutral-500">Account Recovery</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight">
          {step === 'request' ? 'Forgot Password' : 'Reset Password'}
        </h1>

        {step === 'request' ? (
          <form onSubmit={handleRequest} className="mt-6 space-y-4 text-xs">
            <p className="text-neutral-600">
              Enter your registered email address and we will issue a password reset token.
            </p>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
            <button
              type="submit"
              disabled={busy}
              className="w-full bg-[#111111] text-white py-3.5 text-xs font-bold uppercase tracking-widest"
            >
              {busy ? 'Sending...' : 'Send Reset Instructions'}
            </button>
            <button
              type="button"
              onClick={() => setStep('reset')}
              className="w-full text-center text-[11px] underline text-neutral-500 pt-1"
            >
              Already have a reset token?
            </button>
          </form>
        ) : (
          <form onSubmit={handleReset} className="mt-6 space-y-4 text-xs">
            <input
              required
              value={resetForm.token}
              onChange={(e) => setResetForm({ ...resetForm, token: e.target.value })}
              placeholder="Paste reset token"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
            <input
              type="password"
              required
              minLength={6}
              value={resetForm.newPassword}
              onChange={(e) => setResetForm({ ...resetForm, newPassword: e.target.value })}
              placeholder="New password (min 6 chars)"
              className="w-full border border-[#D4D4D4] px-3.5 py-3 outline-none focus:border-[#111111]"
            />
            <button
              type="submit"
              disabled={busy}
              className="w-full bg-[#111111] text-white py-3.5 text-xs font-bold uppercase tracking-widest"
            >
              {busy ? 'Resetting...' : 'Reset Password'}
            </button>
          </form>
        )}

        <div className="mt-6 pt-5 border-t border-[#F0F0F0] text-center text-xs">
          <Link to="/login" className="underline font-semibold">
            Back to Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
