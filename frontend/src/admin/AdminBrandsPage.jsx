import React, { useState, useEffect } from 'react';
import { Plus, Pencil, Trash2, Upload } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';
import { resolveImageUrl } from '../utils/format';

export default function AdminBrandsPage() {
  const { showToast } = useToast();
  const { refreshStoreData } = useStore();
  const [brands, setBrands] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ name: '', description: '', logoUrl: '', active: true });

  const load = async () => {
    const res = await adminApi.getBrands();
    setBrands(res || []);
  };

  useEffect(() => {
    load();
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await adminApi.uploadImage(file, 'brands');
      setForm((prev) => ({ ...prev, logoUrl: res.url }));
      showToast('Brand logo uploaded');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await adminApi.updateBrand(editingId, form);
        showToast('Brand updated');
      } else {
        await adminApi.createBrand(form);
        showToast('Brand created');
      }
      setEditingId(null);
      setForm({ name: '', description: '', logoUrl: '', active: true });
      await load();
      await refreshStoreData();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this brand?')) return;
    try {
      await adminApi.deleteBrand(id);
      showToast('Brand deleted');
      await load();
      await refreshStoreData();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Brands</h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <form onSubmit={handleSubmit} className="lg:col-span-4 bg-white border border-[#E5E5E5] p-5 space-y-3.5 text-xs h-fit">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2">
            {editingId ? 'Edit Brand' : 'Add Brand'}
          </h2>
          <div>
            <label className="block font-semibold mb-1">Brand Name *</label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full border border-[#D4D4D4] px-3 py-2 outline-none focus:border-[#111111]"
            />
          </div>
          <div>
            <label className="block font-semibold mb-1">Description</label>
            <textarea
              rows={2}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full border border-[#D4D4D4] p-2.5 outline-none focus:border-[#111111]"
            />
          </div>
          <div>
            <label className="block font-semibold mb-1">Logo URL</label>
            <div className="flex gap-2">
              <input
                value={form.logoUrl}
                onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
                className="flex-1 border border-[#D4D4D4] px-3 py-2 outline-none"
              />
              <label className="border border-[#111111] px-3 py-2 cursor-pointer flex items-center">
                <Upload className="w-3.5 h-3.5" />
                <input type="file" accept="image/*" onChange={handleUpload} className="hidden" />
              </label>
            </div>
          </div>
          <label className="flex items-center gap-2 cursor-pointer font-medium">
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
              <Plus className="w-3.5 h-3.5" /> {editingId ? 'Save' : 'Add Brand'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null);
                  setForm({ name: '', description: '', logoUrl: '', active: true });
                }}
                className="px-4 py-2 border border-[#D4D4D4]"
              >
                Cancel
              </button>
            )}
          </div>
        </form>

        <div className="lg:col-span-8 bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Brand</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {brands.map((b) => (
                <tr key={b.id} className="border-b border-[#F0F0F0]">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-3">
                      {b.logoUrl && (
                        <img src={resolveImageUrl(b.logoUrl)} alt={b.name} className="w-8 h-8 object-contain bg-[#F5F5F5]" />
                      )}
                      <div>
                        <div className="font-semibold">{b.name}</div>
                        <div className="text-[11px] text-neutral-500 line-clamp-1">{b.description}</div>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-3">
                    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${b.active ? 'border-emerald-300 text-emerald-800 bg-emerald-50' : 'border-neutral-300 text-neutral-500'}`}>
                      {b.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="inline-flex gap-2">
                      <button
                        onClick={() => {
                          setEditingId(b.id);
                          setForm({ name: b.name, description: b.description || '', logoUrl: b.logoUrl || '', active: Boolean(b.active) });
                        }}
                        className="p-1.5 border border-[#D4D4D4] hover:border-[#111111]"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(b.id)}
                        className="p-1.5 border border-[#D4D4D4] hover:border-red-700 hover:text-red-700"
                      >
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
