import React, { useState, useEffect } from 'react';
import { Plus, Pencil, Trash2, Upload } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { resolveImageUrl } from '../utils/format';

export default function AdminBannersPage() {
  const { showToast } = useToast();
  const [banners, setBanners] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    title: '',
    subtitle: '',
    description: '',
    imageUrl: '',
    linkUrl: '/shop',
    ctaText: 'SHOP NOW',
    placement: 'HERO',
    displayOrder: 1,
    active: true,
  });

  const load = async () => {
    const res = await adminApi.getBanners();
    setBanners(res || []);
  };

  useEffect(() => {
    load();
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await adminApi.uploadImage(file, 'banners');
      setForm((prev) => ({ ...prev, imageUrl: res.url }));
      showToast('Banner image uploaded');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ...form, displayOrder: Number(form.displayOrder || 0) };
      if (editingId) {
        await adminApi.updateBanner(editingId, payload);
        showToast('Banner updated on homepage');
      } else {
        await adminApi.createBanner(payload);
        showToast('New banner published to homepage!');
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
      title: '',
      subtitle: '',
      description: '',
      imageUrl: '',
      linkUrl: '/shop',
      ctaText: 'SHOP NOW',
      placement: 'HERO',
      displayOrder: 1,
      active: true,
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this banner?')) return;
    try {
      await adminApi.deleteBanner(id);
      showToast('Banner deleted');
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Homepage Banners</h1>
        <p className="text-xs text-neutral-500 mt-0.5">
          Upload hero or promotional banners — active banners automatically appear on the customer homepage.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <form onSubmit={handleSubmit} className="lg:col-span-4 bg-white border border-[#E5E5E5] p-5 space-y-3.5 text-xs h-fit">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2">
            {editingId ? 'Edit Banner' : 'Add Banner'}
          </h2>
          <div>
            <label className="block font-semibold mb-1">Banner Title *</label>
            <input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g., WINTER KNITWEAR EDIT" className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Subtitle / Eyebrow</label>
            <input value={form.subtitle} onChange={(e) => setForm({ ...form, subtitle: e.target.value })} placeholder="SEASONAL DROP" className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Description</label>
            <textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="w-full border border-[#D4D4D4] p-2.5 outline-none" />
          </div>
          <div>
            <label className="block font-semibold mb-1">Banner Image *</label>
            <div className="flex gap-2">
              <input required value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="Paste image URL or upload..." className="flex-1 border border-[#D4D4D4] px-3 py-2 outline-none" />
              <label className="border border-[#111111] px-3 py-2 cursor-pointer flex items-center">
                <Upload className="w-3.5 h-3.5" />
                <input type="file" accept="image/*" onChange={handleUpload} className="hidden" />
              </label>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Placement</label>
              <select value={form.placement} onChange={(e) => setForm({ ...form, placement: e.target.value })} className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none">
                <option value="HERO">Hero Banner</option>
                <option value="PROMO">Promo Strip</option>
              </select>
            </div>
            <div>
              <label className="block font-semibold mb-1">Order</label>
              <input type="number" value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Destination Link</label>
              <input value={form.linkUrl} onChange={(e) => setForm({ ...form, linkUrl: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
            </div>
            <div>
              <label className="block font-semibold mb-1">Button Text</label>
              <input value={form.ctaText} onChange={(e) => setForm({ ...form, ctaText: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
            </div>
          </div>
          <label className="flex items-center gap-2 font-medium cursor-pointer">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} className="accent-[#111111]" />
            <span>Active on Homepage</span>
          </label>
          <div className="flex gap-2">
            <button type="submit" className="flex-1 bg-[#111111] text-white py-2.5 font-bold uppercase tracking-wider flex items-center justify-center gap-1">
              <Plus className="w-3.5 h-3.5" /> {editingId ? 'Save' : 'Publish Banner'}
            </button>
            {editingId && (
              <button type="button" onClick={resetForm} className="px-4 py-2 border border-[#D4D4D4]">
                Cancel
              </button>
            )}
          </div>
        </form>

        <div className="lg:col-span-8 space-y-4">
          {banners.map((b) => (
            <div key={b.id} className="bg-white border border-[#E5E5E5] p-4 flex flex-col sm:flex-row gap-4">
              <img src={resolveImageUrl(b.imageUrl)} alt={b.title} className="w-full sm:w-48 h-28 object-cover bg-[#F5F5F5]" />
              <div className="flex-1 text-xs">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 border border-[#D4D4D4]">
                    {b.placement} • #{b.displayOrder}
                  </span>
                  <span className={`text-[10px] font-bold uppercase px-2 py-0.5 border ${b.active ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-neutral-300 text-neutral-500'}`}>
                    {b.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <h3 className="text-sm font-bold mt-2">{b.title}</h3>
                {b.description && <p className="text-neutral-500 mt-1 line-clamp-2">{b.description}</p>}
                <div className="mt-3 flex items-center justify-between">
                  <span className="text-neutral-400">Link: {b.linkUrl}</span>
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        setEditingId(b.id);
                        setForm({
                          title: b.title,
                          subtitle: b.subtitle || '',
                          description: b.description || '',
                          imageUrl: b.imageUrl,
                          linkUrl: b.linkUrl || '/shop',
                          ctaText: b.ctaText || 'SHOP NOW',
                          placement: b.placement,
                          displayOrder: b.displayOrder,
                          active: Boolean(b.active),
                        });
                      }}
                      className="p-1.5 border border-[#D4D4D4]"
                    >
                      <Pencil className="w-3.5 h-3.5" />
                    </button>
                    <button onClick={() => handleDelete(b.id)} className="p-1.5 border border-[#D4D4D4] hover:text-red-700">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
