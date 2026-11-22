import React, { useState, useEffect } from 'react';
import { Plus, Pencil, Trash2, Upload } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';
import { resolveImageUrl } from '../utils/format';

export default function AdminCategoriesPage() {
  const { showToast } = useToast();
  const { refreshStoreData } = useStore();

  const [categories, setCategories] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    name: '',
    description: '',
    imageUrl: '',
    parentId: '',
    genderScope: 'MEN',
    displayOrder: 0,
    active: true,
  });

  const load = async () => {
    const res = await adminApi.getCategories();
    setCategories(res || []);
  };

  useEffect(() => {
    load();
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await adminApi.uploadImage(file, 'categories');
      setForm((prev) => ({ ...prev, imageUrl: res.url }));
      showToast('Category image uploaded');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...form,
        parentId: form.parentId ? Number(form.parentId) : null,
        displayOrder: Number(form.displayOrder || 0),
      };
      if (editingId) {
        await adminApi.updateCategory(editingId, payload);
        showToast('Category updated');
      } else {
        await adminApi.createCategory(payload);
        showToast('Category created and published to navigation');
      }
      resetForm();
      await load();
      await refreshStoreData();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleEdit = (c) => {
    setEditingId(c.id);
    setForm({
      name: c.name,
      description: c.description || '',
      imageUrl: c.imageUrl || '',
      parentId: c.parentId || '',
      genderScope: c.genderScope || 'MEN',
      displayOrder: c.displayOrder || 0,
      active: Boolean(c.active),
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category?')) return;
    try {
      await adminApi.deleteCategory(id);
      showToast('Category deleted');
      await load();
      await refreshStoreData();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const resetForm = () => {
    setEditingId(null);
    setForm({ name: '', description: '', imageUrl: '', parentId: '', genderScope: 'MEN', displayOrder: 0, active: true });
  };

  const rootCategories = categories.filter((c) => !c.parentId);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Categories</h1>
        <p className="text-xs text-neutral-500 mt-0.5">
          Categories added or edited here update the customer storefront navigation immediately.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Form */}
        <form onSubmit={handleSubmit} className="lg:col-span-4 bg-white border border-[#E5E5E5] p-5 space-y-3.5 text-xs h-fit">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2">
            {editingId ? 'Edit Category' : 'Create Category'}
          </h2>
          <div>
            <label className="block font-semibold mb-1">Category Name *</label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="e.g., Linen Shirts"
              className="w-full border border-[#D4D4D4] px-3 py-2 outline-none focus:border-[#111111]"
            />
          </div>
          <div>
            <label className="block font-semibold mb-1">Parent Category</label>
            <select
              value={form.parentId}
              onChange={(e) => setForm({ ...form, parentId: e.target.value })}
              className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none focus:border-[#111111]"
            >
              <option value="">Top-Level Navigation Category</option>
              {rootCategories.map((rc) => (
                <option key={rc.id} value={rc.id}>
                  {rc.name}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold mb-1">Department Scope</label>
              <select
                value={form.genderScope}
                onChange={(e) => setForm({ ...form, genderScope: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none"
              >
                <option value="MEN">MEN</option>
                <option value="WOMEN">WOMEN</option>
                <option value="KIDS">KIDS</option>
                <option value="FOOTWEAR">FOOTWEAR</option>
                <option value="ACCESSORIES">ACCESSORIES</option>
                <option value="BEAUTY">BEAUTY</option>
              </select>
            </div>
            <div>
              <label className="block font-semibold mb-1">Display Order</label>
              <input
                type="number"
                value={form.displayOrder}
                onChange={(e) => setForm({ ...form, displayOrder: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
              />
            </div>
          </div>
          <div>
            <label className="block font-semibold mb-1">Category Image</label>
            <div className="flex gap-2">
              <input
                value={form.imageUrl}
                onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                placeholder="Image URL"
                className="flex-1 border border-[#D4D4D4] px-3 py-2 outline-none"
              />
              <label className="border border-[#111111] px-3 py-2 cursor-pointer flex items-center gap-1 font-semibold">
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
            <span>Active on storefront</span>
          </label>
          <div className="flex gap-2 pt-2">
            <button type="submit" className="flex-1 bg-[#111111] text-white py-2.5 font-bold uppercase tracking-wider flex items-center justify-center gap-1">
              <Plus className="w-3.5 h-3.5" /> {editingId ? 'Save' : 'Create'}
            </button>
            {editingId && (
              <button type="button" onClick={resetForm} className="px-4 py-2.5 border border-[#D4D4D4]">
                Cancel
              </button>
            )}
          </div>
        </form>

        {/* List */}
        <div className="lg:col-span-8 bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-3">Parent / Scope</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id} className="border-b border-[#F0F0F0]">
                  <td className="py-3 px-4 flex items-center gap-3">
                    {c.imageUrl && (
                      <img src={resolveImageUrl(c.imageUrl)} alt={c.name} className="w-9 h-9 object-cover bg-[#F5F5F5]" />
                    )}
                    <div>
                      <div className="font-semibold">{c.name}</div>
                      <div className="text-[11px] text-neutral-400">/{c.slug}</div>
                    </div>
                  </td>
                  <td className="py-3 px-3">
                    <div>{c.parentName || 'Top Level'}</div>
                    <div className="text-[11px] text-neutral-500">{c.genderScope}</div>
                  </td>
                  <td className="py-3 px-3">
                    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${c.active ? 'border-emerald-300 text-emerald-800 bg-emerald-50' : 'border-neutral-300 text-neutral-500'}`}>
                      {c.active ? 'Active' : 'Hidden'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="inline-flex gap-2">
                      <button onClick={() => handleEdit(c)} className="p-1.5 border border-[#D4D4D4] hover:border-[#111111]">
                        <Pencil className="w-3.5 h-3.5" />
                      </button>
                      <button onClick={() => handleDelete(c.id)} className="p-1.5 border border-[#D4D4D4] hover:border-red-700 hover:text-red-700">
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
