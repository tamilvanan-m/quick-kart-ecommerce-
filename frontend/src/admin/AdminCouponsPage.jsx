import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Pencil } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { formatCurrency, formatDate } from '../utils/format';

export default function AdminCouponsPage() {
  const { showToast } = useToast();
  const [coupons, setCoupons] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    code: '',
    description: '',
    discountType: 'PERCENTAGE',
    discountValue: '',
    minPurchaseAmount: '0',
    maxDiscountAmount: '',
    expiryDate: '',
    usageLimit: '',
    active: true,
  });

  const load = async () => {
    const res = await adminApi.getCoupons();
    setCoupons(res || []);
  };

  useEffect(() => {
    load();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...form,
        discountValue: Number(form.discountValue),
        minPurchaseAmount: Number(form.minPurchaseAmount || 0),
        maxDiscountAmount: form.maxDiscountAmount ? Number(form.maxDiscountAmount) : null,
        usageLimit: form.usageLimit ? Number(form.usageLimit) : null,
        expiryDate: form.expiryDate ? `${form.expiryDate}T23:59:59` : null,
      };
      if (editingId) {
        await adminApi.updateCoupon(editingId, payload);
        showToast('Coupon updated');
      } else {
        await adminApi.createCoupon(payload);
        showToast('Coupon created');
      }
      resetForm();
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const resetForm = () => {
    setEditingId(null);
    setForm({
      code: '',
      description: '',
      discountType: 'PERCENTAGE',
      discountValue: '',
      minPurchaseAmount: '0',
      maxDiscountAmount: '',
      expiryDate: '',
      usageLimit: '',
      active: true,
    });
  };

  const handleEdit = (c) => {
    setEditingId(c.id);
    setForm({
      code: c.code,
      description: c.description || '',
      discountType: c.discountType,
      discountValue: c.discountValue,
      minPurchaseAmount: c.minPurchaseAmount || '0',
      maxDiscountAmount: c.maxDiscountAmount || '',
      expiryDate: c.expiryDate ? c.expiryDate.slice(0, 10) : '',
      usageLimit: c.usageLimit || '',
      active: Boolean(c.active),
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this coupon?')) return;
    try {
      await adminApi.deleteCoupon(id);
      showToast('Coupon deleted');
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Coupons &amp; Promo Codes</h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <form onSubmit={handleSubmit} className="lg:col-span-4 bg-white border border-[#E5E5E5] p-5 space-y-3.5 text-xs h-fit">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2">
            {editingId ? 'Edit Coupon' : 'Create Coupon'}
          </h2>
          <div>
            <label className="block font-semibold mb-1">Coupon Code *</label>
            <input
              required
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
              placeholder="e.g., SAVE15"
              className="w-full border border-[#D4D4D4] px-3 py-2 uppercase outline-none"
            />
          </div>
          <div>
            <label className="block font-semibold mb-1">Description</label>
            <input
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="15% off on orders above ₹1,499"
              className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Discount Type</label>
              <select
                value={form.discountType}
                onChange={(e) => setForm({ ...form, discountType: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none"
              >
                <option value="PERCENTAGE">Percentage (%)</option>
                <option value="FIXED">Fixed Amount (₹)</option>
              </select>
            </div>
            <div>
              <label className="block font-semibold mb-1">Value *</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.discountValue}
                onChange={(e) => setForm({ ...form, discountValue: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Min Spend (₹)</label>
              <input
                type="number"
                value={form.minPurchaseAmount}
                onChange={(e) => setForm({ ...form, minPurchaseAmount: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
            <div>
              <label className="block font-semibold mb-1">Max Discount (₹)</label>
              <input
                type="number"
                value={form.maxDiscountAmount}
                onChange={(e) => setForm({ ...form, maxDiscountAmount: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Expiry Date</label>
              <input
                type="date"
                value={form.expiryDate}
                onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
            <div>
              <label className="block font-semibold mb-1">Usage Limit</label>
              <input
                type="number"
                value={form.usageLimit}
                onChange={(e) => setForm({ ...form, usageLimit: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
          </div>
          <label className="flex items-center gap-2 font-medium cursor-pointer">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })}
              className="accent-[#111111]"
            />
            <span>Active</span>
          </label>
          <div className="flex gap-2">
            <button type="submit" className="flex-1 bg-[#111111] text-white py-2.5 font-bold uppercase tracking-wider flex items-center justify-center gap-1">
              <Plus className="w-3.5 h-3.5" /> {editingId ? 'Save' : 'Create Coupon'}
            </button>
            {editingId && (
              <button type="button" onClick={resetForm} className="px-4 py-2 border border-[#D4D4D4]">
                Cancel
              </button>
            )}
          </div>
        </form>

        <div className="lg:col-span-8 bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Code</th>
                <th className="py-3 px-3">Discount</th>
                <th className="py-3 px-3">Min Spend</th>
                <th className="py-3 px-3">Used / Limit</th>
                <th className="py-3 px-3">Expiry</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {coupons.map((c) => (
                <tr key={c.id} className="border-b border-[#F0F0F0]">
                  <td className="py-3 px-4">
                    <div className="font-bold tracking-wider">{c.code}</div>
                    <div className="text-[11px] text-neutral-500">{c.description}</div>
                  </td>
                  <td className="py-3 px-3 font-semibold">
                    {c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : formatCurrency(c.discountValue)}
                  </td>
                  <td className="py-3 px-3">{formatCurrency(c.minPurchaseAmount)}</td>
                  <td className="py-3 px-3">
                    {c.usedCount} / {c.usageLimit || '∞'}
                  </td>
                  <td className="py-3 px-3 text-neutral-500">{c.expiryDate ? formatDate(c.expiryDate) : 'No expiry'}</td>
                  <td className="py-3 px-4 text-right">
                    <div className="inline-flex gap-2">
                      <button onClick={() => handleEdit(c)} className="p-1.5 border border-[#D4D4D4]">
                        <Pencil className="w-3.5 h-3.5" />
                      </button>
                      <button onClick={() => handleDelete(c.id)} className="p-1.5 border border-[#D4D4D4] hover:text-red-700">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
