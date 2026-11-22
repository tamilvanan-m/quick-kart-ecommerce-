import React, { useState, useEffect } from 'react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';

export default function AdminSettingsPage() {
  const { showToast } = useToast();
  const { refreshStoreData } = useStore();
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApi.getSettings().then((res) => setForm(res));
  }, []);

  if (!form) {
    return <div className="py-12 text-center text-xs text-neutral-500">Loading store settings...</div>;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await adminApi.updateSettings({
        ...form,
        deliveryCharge: Number(form.deliveryCharge),
        freeDeliveryThreshold: Number(form.freeDeliveryThreshold),
        returnPolicyDays: Number(form.returnPolicyDays),
      });
      await refreshStoreData();
      showToast('Store settings saved and published!');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Store Settings</h1>
        <p className="text-xs text-neutral-500 mt-0.5">
          Updates to store name, announcement bar, delivery thresholds, and payment options apply immediately.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white border border-[#E5E5E5] p-6 space-y-5 text-xs">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block font-semibold mb-1">Store Name *</label>
            <input required value={form.storeName} onChange={(e) => setForm({ ...form, storeName: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Support Email *</label>
            <input required type="email" value={form.contactEmail} onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Contact Phone</label>
            <input value={form.contactPhone || ''} onChange={(e) => setForm({ ...form, contactPhone: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">GST Number</label>
            <input value={form.gstNumber || ''} onChange={(e) => setForm({ ...form, gstNumber: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div className="sm:col-span-2">
            <label className="block font-semibold mb-1">Business Address</label>
            <input value={form.businessAddress || ''} onChange={(e) => setForm({ ...form, businessAddress: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div className="sm:col-span-2">
            <label className="block font-semibold mb-1">Top Store Announcement Bar</label>
            <input value={form.storeAnnouncement || ''} onChange={(e) => setForm({ ...form, storeAnnouncement: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 border-t border-[#F0F0F0]">
          <div>
            <label className="block font-semibold mb-1">Delivery Charge (₹)</label>
            <input type="number" step="0.01" min="0" value={form.deliveryCharge} onChange={(e) => setForm({ ...form, deliveryCharge: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Free Delivery Above (₹)</label>
            <input type="number" step="0.01" min="0" value={form.freeDeliveryThreshold} onChange={(e) => setForm({ ...form, freeDeliveryThreshold: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Return Window (Days)</label>
            <input type="number" min="0" value={form.returnPolicyDays} onChange={(e) => setForm({ ...form, returnPolicyDays: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none" />
          </div>
        </div>

        <div>
          <label className="block font-semibold mb-1">Return Policy Summary</label>
          <textarea rows={2} value={form.returnPolicyText || ''} onChange={(e) => setForm({ ...form, returnPolicyText: e.target.value })} className="w-full border border-[#D4D4D4] p-3 outline-none" />
        </div>

        <div className="flex flex-wrap gap-6 pt-2">
          <label className="flex items-center gap-2 font-medium cursor-pointer">
            <input type="checkbox" checked={Boolean(form.razorpayEnabled)} onChange={(e) => setForm({ ...form, razorpayEnabled: e.target.checked })} className="accent-[#111111]" />
            <span>Enable Razorpay Online Payment</span>
          </label>
          <label className="flex items-center gap-2 font-medium cursor-pointer">
            <input type="checkbox" checked={Boolean(form.codEnabled)} onChange={(e) => setForm({ ...form, codEnabled: e.target.checked })} className="accent-[#111111]" />
            <span>Enable Cash on Delivery (COD)</span>
          </label>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="bg-[#111111] text-white px-7 py-3 font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
        >
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
      </form>
    </div>
  );
}
