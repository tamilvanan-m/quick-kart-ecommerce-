import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Pencil, Trash2, Search } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import Pagination from '../components/Pagination';
import { formatCurrency, resolveImageUrl } from '../utils/format';

export default function AdminProductsPage() {
  const { showToast } = useToast();
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadProducts = async () => {
    setLoading(true);
    try {
      const params = { page, size: 12 };
      if (q.trim()) params.q = q.trim();
      if (status) params.status = status;
      const res = await adminApi.getProducts(params);
      setData(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, [page, status]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadProducts();
  };

  const handleDelete = async (id) => {
    try {
      await adminApi.deleteProduct(id);
      setConfirmDelete(null);
      showToast('Product removed from storefront.');
      loadProducts();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Products ({data.totalElements})</h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Changes to product prices, images, or status reflect immediately on the customer storefront.
          </p>
        </div>
        <Link
          to="/admin/products/new"
          className="bg-[#111111] text-white px-5 py-2.5 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 hover:bg-neutral-800"
        >
          <Plus className="w-4 h-4" /> Add New Product
        </Link>
      </div>

      {/* Filters */}
      <div className="bg-white border border-[#E5E5E5] p-4 flex flex-wrap gap-3 items-center justify-between">
        <form onSubmit={handleSearch} className="flex gap-2 flex-1 min-w-[240px] max-w-md">
          <div className="relative flex-1">
            <Search className="w-3.5 h-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search by product name, SKU, or brand..."
              className="w-full border border-[#D4D4D4] pl-8 pr-3 py-2 text-xs outline-none focus:border-[#111111]"
            />
          </div>
          <button type="submit" className="bg-[#111111] text-white px-4 py-2 text-xs font-bold uppercase">
            Search
          </button>
        </form>

        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          className="border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none focus:border-[#111111]"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="OUT_OF_STOCK">Sold Out (0 Stock)</option>
          <option value="INACTIVE">Inactive (Hidden)</option>
          <option value="DRAFT">Draft</option>
        </select>
      </div>

      {/* Table */}
      <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
        {loading ? (
          <div className="p-10 text-center text-xs text-neutral-500">Loading products...</div>
        ) : data.content.length === 0 ? (
          <div className="p-12 text-center text-xs text-neutral-500">No products found.</div>
        ) : (
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase tracking-wider text-neutral-500">
                <th className="py-3 px-4">Product</th>
                <th className="py-3 px-3">Category / Brand</th>
                <th className="py-3 px-3">Price</th>
                <th className="py-3 px-3">Total Stock</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((p) => (
                <tr key={p.id} className="border-b border-[#F0F0F0] hover:bg-[#FAFAFA]">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-3">
                      <img
                        src={resolveImageUrl(p.primaryImageUrl)}
                        alt={p.name}
                        className="w-11 h-14 object-cover bg-[#F5F5F5] border border-[#E5E5E5] shrink-0"
                      />
                      <div>
                        <div className="font-semibold text-[#111111]">{p.name}</div>
                        <div className="text-[11px] text-neutral-500">SKU: {p.sku} • {p.gender}</div>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-3">
                    <div>{p.categoryName}</div>
                    <div className="text-[11px] text-neutral-500">{p.brandName}</div>
                  </td>
                  <td className="py-3 px-3">
                    <div className="font-bold">{formatCurrency(p.sellingPrice)}</div>
                    {Number(p.originalPrice) > Number(p.sellingPrice) && (
                      <div className="text-[11px] text-neutral-400 line-through">
                        {formatCurrency(p.originalPrice)}
                      </div>
                    )}
                  </td>
                  <td className="py-3 px-3">
                    <span className={`font-bold ${p.totalAvailableStock <= 0 ? 'text-red-700' : ''}`}>
                      {p.totalAvailableStock} units
                    </span>
                    <div className="text-[11px] text-neutral-500">{p.variants?.length || 0} variants</div>
                  </td>
                  <td className="py-3 px-3">
                    <span
                      className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${
                        p.status === 'ACTIVE'
                          ? 'border-emerald-300 bg-emerald-50 text-emerald-800'
                          : p.status === 'OUT_OF_STOCK'
                          ? 'border-red-300 bg-red-50 text-red-800'
                          : 'border-neutral-300 bg-neutral-100 text-neutral-700'
                      }`}
                    >
                      {p.status === 'OUT_OF_STOCK' ? 'SOLD OUT' : p.status}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="inline-flex items-center gap-2">
                      <Link
                        to={`/admin/products/${p.id}/edit`}
                        className="p-1.5 border border-[#D4D4D4] hover:border-[#111111]"
                        title="Edit Product"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </Link>
                      <button
                        type="button"
                        onClick={() => setConfirmDelete(p)}
                        className="p-1.5 border border-[#D4D4D4] hover:border-red-700 hover:text-red-700"
                        title="Delete Product"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />

      {confirmDelete && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white border border-[#111111] max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold uppercase">Confirm Deletion</h3>
            <p className="text-xs text-neutral-600">
              Are you sure you want to delete <strong>{confirmDelete.name}</strong>? If this product has customer order history, it will be safely archived while keeping past order invoices intact.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setConfirmDelete(null)}
                className="px-4 py-2 text-xs border border-[#D4D4D4]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleDelete(confirmDelete.id)}
                className="px-5 py-2 text-xs font-bold uppercase bg-red-700 text-white"
              >
                Delete Product
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
