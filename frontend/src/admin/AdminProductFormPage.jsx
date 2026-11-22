import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Upload, Plus, Trash2, ArrowLeft } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { resolveImageUrl } from '../utils/format';

export default function AdminProductFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [imageUrlInput, setImageUrlInput] = useState('');

  const [form, setForm] = useState({
    name: '',
    sku: '',
    brandId: '',
    categoryId: '',
    gender: 'MEN',
    description: '',
    materialCare: '',
    originalPrice: '',
    sellingPrice: '',
    status: 'ACTIVE',
    featured: false,
    newArrival: true,
    bestSeller: false,
    trending: false,
    images: [],
    variants: [
      { size: 'S', color: 'Black', colorHex: '#111111', stockQuantity: 10, lowStockThreshold: 5, active: true },
      { size: 'M', color: 'Black', colorHex: '#111111', stockQuantity: 15, lowStockThreshold: 5, active: true },
      { size: 'L', color: 'Black', colorHex: '#111111', stockQuantity: 12, lowStockThreshold: 5, active: true },
    ],
  });

  useEffect(() => {
    Promise.all([adminApi.getCategories(), adminApi.getBrands()]).then(([cats, brs]) => {
      setCategories(cats || []);
      setBrands(brs || []);
      if (!isEdit && cats?.length > 0 && brs?.length > 0) {
        setForm((prev) => ({
          ...prev,
          categoryId: prev.categoryId || cats.find((c) => c.parentId)?.id || cats[0].id,
          brandId: prev.brandId || brs[0].id,
        }));
      }
    });
  }, [isEdit]);

  useEffect(() => {
    if (!isEdit) return;
    adminApi.getProduct(id).then((p) => {
      setForm({
        name: p.name,
        sku: p.sku,
        brandId: p.brandId,
        categoryId: p.categoryId,
        gender: p.gender,
        description: p.description,
        materialCare: p.materialCare || '',
        originalPrice: p.originalPrice,
        sellingPrice: p.sellingPrice,
        status: p.status,
        featured: Boolean(p.featured),
        newArrival: Boolean(p.newArrival),
        bestSeller: Boolean(p.bestSeller),
        trending: Boolean(p.trending),
        images: (p.images || []).map((img) => ({
          imageUrl: img.imageUrl,
          publicId: img.publicId,
          isPrimary: img.isPrimary,
          displayOrder: img.displayOrder,
        })),
        variants: (p.variants || []).map((v) => ({
          id: v.id,
          sku: v.sku,
          size: v.size,
          color: v.color,
          colorHex: v.colorHex || '#111111',
          stockQuantity: v.stockQuantity,
          lowStockThreshold: v.lowStockThreshold,
          active: v.active,
        })),
      });
    });
  }, [id, isEdit]);

  const handleFileUpload = async (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    setUploading(true);
    try {
      for (const file of files) {
        const res = await adminApi.uploadImage(file, 'products');
        setForm((prev) => ({
          ...prev,
          images: [
            ...prev.images,
            {
              imageUrl: res.url,
              publicId: res.publicId,
              isPrimary: prev.images.length === 0,
              displayOrder: prev.images.length,
            },
          ],
        }));
      }
      showToast('Image(s) uploaded');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setUploading(false);
    }
  };

  const handleAddImageUrl = () => {
    if (!imageUrlInput.trim()) return;
    setForm((prev) => ({
      ...prev,
      images: [
        ...prev.images,
        {
          imageUrl: imageUrlInput.trim(),
          isPrimary: prev.images.length === 0,
          displayOrder: prev.images.length,
        },
      ],
    }));
    setImageUrlInput('');
  };

  const setPrimaryImage = (idx) => {
    setForm((prev) => ({
      ...prev,
      images: prev.images.map((img, i) => ({ ...img, isPrimary: i === idx })),
    }));
  };

  const removeImage = (idx) => {
    setForm((prev) => {
      const next = prev.images.filter((_, i) => i !== idx);
      if (next.length > 0 && !next.some((i) => i.isPrimary)) {
        next[0].isPrimary = true;
      }
      return { ...prev, images: next };
    });
  };

  const updateVariant = (idx, field, val) => {
    setForm((prev) => ({
      ...prev,
      variants: prev.variants.map((v, i) => (i === idx ? { ...v, [field]: val } : v)),
    }));
  };

  const addVariantRow = () => {
    setForm((prev) => ({
      ...prev,
      variants: [
        ...prev.variants,
        { size: 'XL', color: 'Black', colorHex: '#111111', stockQuantity: 10, lowStockThreshold: 5, active: true },
      ],
    }));
  };

  const removeVariantRow = (idx) => {
    if (form.variants.length <= 1) {
      showToast('At least one size/color variant is required.', 'error');
      return;
    }
    setForm((prev) => ({
      ...prev,
      variants: prev.variants.filter((_, i) => i !== idx),
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        brandId: Number(form.brandId),
        categoryId: Number(form.categoryId),
        originalPrice: Number(form.originalPrice),
        sellingPrice: Number(form.sellingPrice),
        variants: form.variants.map((v) => ({
          ...v,
          stockQuantity: Number(v.stockQuantity || 0),
          lowStockThreshold: Number(v.lowStockThreshold || 5),
        })),
      };

      if (isEdit) {
        await adminApi.updateProduct(id, payload);
        showToast('Product updated on storefront!');
      } else {
        await adminApi.createProduct(payload);
        showToast('New product published to Quick kart!');
      }
      navigate('/admin/products');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const discountPreview =
    Number(form.originalPrice) > 0 && Number(form.sellingPrice) > 0
      ? Math.max(0, Math.round(((Number(form.originalPrice) - Number(form.sellingPrice)) / Number(form.originalPrice)) * 100))
      : 0;

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <Link to="/admin/products" className="inline-flex items-center gap-1.5 text-xs font-semibold hover:underline mb-3">
          <ArrowLeft className="w-3.5 h-3.5" /> Back to Products
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">
          {isEdit ? 'Edit Product' : 'Add New Product'}
        </h1>
        <p className="text-xs text-neutral-500 mt-0.5">
          Fill in product details, pricing, images, and stock per size/colour.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6 text-xs">
        {/* Basic Info */}
        <div className="bg-white border border-[#E5E5E5] p-6 space-y-4">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2.5">
            1. Product Information
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block font-semibold mb-1">Product Title *</label>
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g., Tailored Linen Casual Shirt"
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>

            <div>
              <label className="block font-semibold mb-1">Brand *</label>
              <select
                required
                value={form.brandId}
                onChange={(e) => setForm({ ...form, brandId: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2.5 outline-none focus:border-[#111111]"
              >
                {brands.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold mb-1">Category *</label>
              <select
                required
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2.5 outline-none focus:border-[#111111]"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentName ? `${c.parentName} — ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold mb-1">Target Audience / Gender *</label>
              <select
                value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2.5 outline-none focus:border-[#111111]"
              >
                <option value="MEN">Men</option>
                <option value="WOMEN">Women</option>
                <option value="KIDS">Kids</option>
                <option value="UNISEX">Unisex</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold mb-1">SKU (Auto-generated if blank)</label>
              <input
                value={form.sku}
                onChange={(e) => setForm({ ...form, sku: e.target.value.toUpperCase() })}
                placeholder="e.g., QK-SH-205"
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold mb-1">Product Description *</label>
              <textarea
                required
                rows={3}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Describe fabric weight, fit, collar/sleeve details..."
                className="w-full border border-[#D4D4D4] p-3 outline-none focus:border-[#111111]"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold mb-1">Material &amp; Wash Care</label>
              <input
                value={form.materialCare}
                onChange={(e) => setForm({ ...form, materialCare: e.target.value })}
                placeholder="e.g., 100% Organic Cotton • Machine wash cold"
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
          </div>
        </div>

        {/* Pricing & Merchandising */}
        <div className="bg-white border border-[#E5E5E5] p-6 space-y-4">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2.5">
            2. Pricing &amp; Visibility
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block font-semibold mb-1">Original Price (MRP ₹) *</label>
              <input
                type="number"
                step="0.01"
                min="1"
                required
                value={form.originalPrice}
                onChange={(e) => setForm({ ...form, originalPrice: e.target.value })}
                placeholder="1499"
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
            <div>
              <label className="block font-semibold mb-1">Selling Price (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="1"
                required
                value={form.sellingPrice}
                onChange={(e) => setForm({ ...form, sellingPrice: e.target.value })}
                placeholder="999"
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
            <div>
              <label className="block font-semibold mb-1">Product Status</label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2.5 outline-none focus:border-[#111111]"
              >
                <option value="ACTIVE">ACTIVE (Visible &amp; Purchasable)</option>
                <option value="OUT_OF_STOCK">OUT OF STOCK</option>
                <option value="INACTIVE">INACTIVE (Hidden from Store)</option>
                <option value="DRAFT">DRAFT</option>
              </select>
            </div>
          </div>
          <div className="text-[11px] text-neutral-500">
            Discount calculated automatically: <strong>{discountPreview}% OFF</strong>
          </div>

          <div className="flex flex-wrap gap-5 pt-2">
            {[
              ['featured', 'Featured Product'],
              ['newArrival', 'New Arrival'],
              ['bestSeller', 'Best Seller'],
              ['trending', 'Trending Now'],
            ].map(([field, label]) => (
              <label key={field} className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="checkbox"
                  checked={form[field]}
                  onChange={(e) => setForm({ ...form, [field]: e.target.checked })}
                  className="accent-[#111111]"
                />
                <span>{label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Images */}
        <div className="bg-white border border-[#E5E5E5] p-6 space-y-4">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2.5">
            3. Product Photography
          </h2>

          <div className="flex flex-wrap gap-3 items-center">
            <label className="inline-flex items-center gap-2 bg-[#111111] text-white px-4 py-2.5 font-bold uppercase tracking-wider cursor-pointer hover:bg-neutral-800">
              <Upload className="w-3.5 h-3.5" />
              {uploading ? 'Uploading...' : 'Upload Image File'}
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={handleFileUpload}
                className="hidden"
              />
            </label>

            <div className="flex-1 min-w-[240px] flex gap-2">
              <input
                value={imageUrlInput}
                onChange={(e) => setImageUrlInput(e.target.value)}
                placeholder="Or paste external image URL..."
                className="flex-1 border border-[#D4D4D4] px-3 py-2 outline-none focus:border-[#111111]"
              />
              <button
                type="button"
                onClick={handleAddImageUrl}
                className="border border-[#111111] px-4 py-2 font-bold uppercase"
              >
                Add URL
              </button>
            </div>
          </div>

          {form.images.length > 0 && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-2">
              {form.images.map((img, idx) => (
                <div key={idx} className={`border p-2 space-y-2 ${img.isPrimary ? 'border-[#111111]' : 'border-[#E5E5E5]'}`}>
                  <img
                    src={resolveImageUrl(img.imageUrl)}
                    alt="Preview"
                    className="w-full aspect-[3/4] object-cover bg-[#F5F5F5]"
                  />
                  <div className="flex items-center justify-between text-[11px]">
                    <button
                      type="button"
                      onClick={() => setPrimaryImage(idx)}
                      className={img.isPrimary ? 'font-bold text-[#111111]' : 'text-neutral-500 underline'}
                    >
                      {img.isPrimary ? 'Primary Image' : 'Set Primary'}
                    </button>
                    <button
                      type="button"
                      onClick={() => removeImage(idx)}
                      className="text-red-700 hover:underline"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Variants & Stock */}
        <div className="bg-white border border-[#E5E5E5] p-6 space-y-4">
          <div className="flex items-center justify-between border-b border-[#F0F0F0] pb-2.5">
            <h2 className="font-bold uppercase tracking-widest">4. Size, Colour &amp; Stock Variants</h2>
            <button
              type="button"
              onClick={addVariantRow}
              className="border border-[#111111] px-3 py-1.5 font-bold uppercase flex items-center gap-1"
            >
              <Plus className="w-3.5 h-3.5" /> Add Size Row
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#E5E5E5] text-[11px] uppercase text-neutral-500">
                  <th className="py-2 pr-2">Size *</th>
                  <th className="py-2 pr-2">Colour *</th>
                  <th className="py-2 pr-2">Swatch</th>
                  <th className="py-2 pr-2">Stock Qty *</th>
                  <th className="py-2 pr-2">Low-Stock Alert</th>
                  <th className="py-2 text-right">Remove</th>
                </tr>
              </thead>
              <tbody>
                {form.variants.map((v, idx) => (
                  <tr key={idx} className="border-b border-[#F5F5F5]">
                    <td className="py-2 pr-2">
                      <input
                        required
                        value={v.size}
                        onChange={(e) => updateVariant(idx, 'size', e.target.value)}
                        placeholder="S / M / L / 32"
                        className="w-24 border border-[#D4D4D4] px-2.5 py-1.5 outline-none"
                      />
                    </td>
                    <td className="py-2 pr-2">
                      <input
                        required
                        value={v.color}
                        onChange={(e) => updateVariant(idx, 'color', e.target.value)}
                        placeholder="Jet Black"
                        className="w-32 border border-[#D4D4D4] px-2.5 py-1.5 outline-none"
                      />
                    </td>
                    <td className="py-2 pr-2">
                      <input
                        type="color"
                        value={v.colorHex || '#111111'}
                        onChange={(e) => updateVariant(idx, 'colorHex', e.target.value)}
                        className="w-9 h-8 border border-[#D4D4D4] p-0.5 cursor-pointer"
                      />
                    </td>
                    <td className="py-2 pr-2">
                      <input
                        type="number"
                        min="0"
                        required
                        value={v.stockQuantity}
                        onChange={(e) => updateVariant(idx, 'stockQuantity', e.target.value)}
                        className="w-24 border border-[#D4D4D4] px-2.5 py-1.5 outline-none"
                      />
                    </td>
                    <td className="py-2 pr-2">
                      <input
                        type="number"
                        min="0"
                        value={v.lowStockThreshold}
                        onChange={(e) => updateVariant(idx, 'lowStockThreshold', e.target.value)}
                        className="w-20 border border-[#D4D4D4] px-2.5 py-1.5 outline-none"
                      />
                    </td>
                    <td className="py-2 text-right">
                      <button
                        type="button"
                        onClick={() => removeVariantRow(idx)}
                        className="text-neutral-400 hover:text-red-700 p-1"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="text-[11px] text-neutral-500">
            Tip: If all variant stocks are set to <strong>0</strong>, the product automatically marks itself <strong>SOLD OUT</strong> on the website.
          </p>
        </div>

        <div className="flex justify-end gap-3">
          <Link to="/admin/products" className="px-6 py-3 border border-[#D4D4D4] bg-white font-bold uppercase tracking-wider">
            Cancel
          </Link>
          <button
            type="submit"
            disabled={saving}
            className="bg-[#111111] text-white px-8 py-3 font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
          >
            {saving ? 'Saving...' : isEdit ? 'Save Product Changes' : 'Publish Product'}
          </button>
        </div>
      </form>
    </div>
  );
}
