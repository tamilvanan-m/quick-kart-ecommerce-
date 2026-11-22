import React, { useState, useEffect } from 'react';
import { Save } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { formatDateTime } from '../utils/format';

export default function AdminInventoryPage() {
  const { showToast } = useToast();
  const [tab, setTab] = useState('all');
  const [items, setItems] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [edits, setEdits] = useState({});
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      if (tab === 'history') {
        const res = await adminApi.getInventoryTransactions(0, 30);
        setTransactions(res?.content || []);
      } else {
        const res = tab === 'low' ? await adminApi.getLowStock() : await adminApi.getInventory();
        setItems(res || []);
        const map = {};
        (res || []).forEach((it) => {
          map[it.variantId] = it.quantity;
        });
        setEdits(map);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [tab]);

  const handleSaveStock = async (item) => {
    const qty = Number(edits[item.variantId]);
    if (Number.isNaN(qty) || qty < 0) {
      showToast('Quantity must be 0 or greater.', 'error');
      return;
    }
    try {
      await adminApi.updateVariantStock(item.variantId, {
        quantity: qty,
        notes: 'Admin quick stock update',
      });
      showToast(
        qty === 0
          ? `Stock set to 0 for ${item.productName} (${item.size}). Product auto-updates if all sizes are 0.`
          : `Updated stock to ${qty} for ${item.productName} (${item.size}).`
      );
      loadData();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Automatic Inventory Control</h1>
        <p className="text-xs text-neutral-500 mt-0.5">
          Setting stock to 0 automatically marks a product <strong>SOLD OUT</strong>. Restocking automatically restores availability.
        </p>
      </div>

      <div className="flex gap-2 border-b border-[#E5E5E5]">
        {[
          ['all', 'All Variant Stock'],
          ['low', 'Low & Out-of-Stock Alerts'],
          ['history', 'Stock Movement Log'],
        ].map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`px-4 py-2.5 text-xs font-bold uppercase tracking-wider border-b-2 transition-colors ${
              tab === key ? 'border-[#111111] text-[#111111]' : 'border-transparent text-neutral-500 hover:text-[#111111]'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="py-12 text-center text-xs text-neutral-500">Loading inventory...</div>
      ) : tab === 'history' ? (
        <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Product / SKU</th>
                <th className="py-3 px-3">Type</th>
                <th className="py-3 px-3">Change</th>
                <th className="py-3 px-3">Prev &rarr; New</th>
                <th className="py-3 px-3">Notes</th>
                <th className="py-3 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((tx) => (
                <tr key={tx.id} className="border-b border-[#F0F0F0]">
                  <td className="py-2.5 px-4">
                    <div className="font-semibold">{tx.productName}</div>
                    <div className="text-[11px] text-neutral-500">{tx.variantSku}</div>
                  </td>
                  <td className="py-2.5 px-3 font-medium">{tx.transactionType}</td>
                  <td className={`py-2.5 px-3 font-bold ${tx.changeAmount < 0 ? 'text-red-700' : 'text-emerald-700'}`}>
                    {tx.changeAmount > 0 ? `+${tx.changeAmount}` : tx.changeAmount}
                  </td>
                  <td className="py-2.5 px-3">
                    {tx.previousQuantity} &rarr; <strong>{tx.newQuantity}</strong>
                  </td>
                  <td className="py-2.5 px-3 text-neutral-600">{tx.notes}</td>
                  <td className="py-2.5 px-4 text-neutral-500">{formatDateTime(tx.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Product &amp; Variant</th>
                <th className="py-3 px-3">Size / Colour</th>
                <th className="py-3 px-3">Reserved</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-3">Stock Quantity</th>
                <th className="py-3 px-4 text-right">Update</th>
              </tr>
            </thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.variantId} className="border-b border-[#F0F0F0]">
                  <td className="py-3 px-4">
                    <div className="font-semibold">{it.productName}</div>
                    <div className="text-[11px] text-neutral-500">
                      {it.brandName} • {it.variantSku}
                    </div>
                  </td>
                  <td className="py-3 px-3">
                    <span className="font-bold">{it.size}</span> / {it.color}
                  </td>
                  <td className="py-3 px-3">{it.reservedQuantity}</td>
                  <td className="py-3 px-3">
                    {it.outOfStock ? (
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase bg-red-50 text-red-700 border border-red-300">
                        SOLD OUT
                      </span>
                    ) : it.lowStock ? (
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase bg-amber-50 text-amber-800 border border-amber-300">
                        LOW STOCK
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase bg-emerald-50 text-emerald-800 border border-emerald-300">
                        IN STOCK
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-3">
                    <input
                      type="number"
                      min="0"
                      value={edits[it.variantId] ?? it.quantity}
                      onChange={(e) => setEdits({ ...edits, [it.variantId]: e.target.value })}
                      className="w-24 border border-[#D4D4D4] px-2.5 py-1.5 outline-none focus:border-[#111111]"
                    />
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      type="button"
                      onClick={() => handleSaveStock(it)}
                      className="inline-flex items-center gap-1 bg-[#111111] text-white px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-wider hover:bg-neutral-800"
                    >
                      <Save className="w-3 h-3" /> Save
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
