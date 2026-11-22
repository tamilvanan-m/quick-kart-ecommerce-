import React, { useState, useEffect } from 'react';
import { Search, Eye } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import Pagination from '../components/Pagination';
import { formatCurrency, formatDateTime, ORDER_STATUS_LABEL } from '../utils/format';

const ALL_STATUSES = [
  'PLACED',
  'CONFIRMED',
  'PACKED',
  'SHIPPED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
  'RETURN_REQUESTED',
  'RETURNED',
];

export default function AdminOrdersPage() {
  const { showToast } = useToast();
  const [tab, setTab] = useState('orders');
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [returns, setReturns] = useState([]);
  const [q, setQ] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [paymentFilter, setPaymentFilter] = useState('');
  const [page, setPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [statusForm, setStatusForm] = useState({ status: '', trackingNumber: '', notes: '' });

  const loadOrders = async () => {
    const params = { page, size: 12 };
    if (q.trim()) params.q = q.trim();
    if (statusFilter) params.orderStatus = statusFilter;
    if (paymentFilter) params.paymentStatus = paymentFilter;
    const res = await adminApi.getOrders(params);
    setData(res);
  };

  const loadReturns = async () => {
    const res = await adminApi.getReturns();
    setReturns(res || []);
  };

  useEffect(() => {
    if (tab === 'orders') loadOrders();
    else loadReturns();
  }, [tab, page, statusFilter, paymentFilter]);

  const openOrderModal = (order) => {
    setSelectedOrder(order);
    setStatusForm({
      status: order.orderStatus,
      trackingNumber: order.trackingNumber || '',
      notes: order.notes || '',
    });
  };

  const handleUpdateStatus = async (e) => {
    e.preventDefault();
    try {
      await adminApi.updateOrderStatus(selectedOrder.id, statusForm);
      showToast('Order status updated');
      setSelectedOrder(null);
      loadOrders();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleProcessReturn = async (returnId, status) => {
    try {
      await adminApi.processReturn(returnId, { status, adminNotes: `Processed by admin as ${status}` });
      showToast(`Return marked as ${status}`);
      loadReturns();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Orders &amp; Returns</h1>

      <div className="flex gap-2 border-b border-[#E5E5E5]">
        <button
          type="button"
          onClick={() => setTab('orders')}
          className={`px-4 py-2.5 text-xs font-bold uppercase tracking-wider border-b-2 ${
            tab === 'orders' ? 'border-[#111111] text-[#111111]' : 'border-transparent text-neutral-500'
          }`}
        >
          Customer Orders
        </button>
        <button
          type="button"
          onClick={() => setTab('returns')}
          className={`px-4 py-2.5 text-xs font-bold uppercase tracking-wider border-b-2 ${
            tab === 'returns' ? 'border-[#111111] text-[#111111]' : 'border-transparent text-neutral-500'
          }`}
        >
          Return Requests
        </button>
      </div>

      {tab === 'orders' ? (
        <>
          <div className="bg-white border border-[#E5E5E5] p-4 flex flex-wrap gap-3 items-center">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                setPage(0);
                loadOrders();
              }}
              className="flex gap-2 flex-1 min-w-[220px]"
            >
              <div className="relative flex-1">
                <Search className="w-3.5 h-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  placeholder="Order #, customer name or email..."
                  className="w-full border border-[#D4D4D4] pl-8 pr-3 py-2 text-xs outline-none focus:border-[#111111]"
                />
              </div>
              <button type="submit" className="bg-[#111111] text-white px-4 py-2 text-xs font-bold uppercase">
                Search
              </button>
            </form>

            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none"
            >
              <option value="">All Order Statuses</option>
              {ALL_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {ORDER_STATUS_LABEL[s]}
                </option>
              ))}
            </select>

            <select
              value={paymentFilter}
              onChange={(e) => {
                setPaymentFilter(e.target.value);
                setPage(0);
              }}
              className="border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none"
            >
              <option value="">All Payment Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="PAID">Paid</option>
              <option value="FAILED">Failed</option>
              <option value="REFUNDED">Refunded</option>
            </select>
          </div>

          <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                  <th className="py-3 px-4">Order #</th>
                  <th className="py-3 px-3">Customer</th>
                  <th className="py-3 px-3">Date</th>
                  <th className="py-3 px-3">Payment</th>
                  <th className="py-3 px-3">Order Status</th>
                  <th className="py-3 px-3">Total</th>
                  <th className="py-3 px-4 text-right">Manage</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((o) => (
                  <tr key={o.id} className="border-b border-[#F0F0F0] hover:bg-[#FAFAFA]">
                    <td className="py-3 px-4 font-bold">{o.orderNumber}</td>
                    <td className="py-3 px-3">
                      <div className="font-medium">{o.shippingFullName}</div>
                      <div className="text-[11px] text-neutral-500">{o.customerEmail}</div>
                    </td>
                    <td className="py-3 px-3 text-neutral-500">{formatDateTime(o.createdAt)}</td>
                    <td className="py-3 px-3">
                      <div>{o.paymentMethod}</div>
                      <div className="text-[11px] font-semibold">{o.paymentStatus}</div>
                    </td>
                    <td className="py-3 px-3">
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase border border-[#D4D4D4]">
                        {ORDER_STATUS_LABEL[o.orderStatus]}
                      </span>
                    </td>
                    <td className="py-3 px-3 font-bold">{formatCurrency(o.totalAmount)}</td>
                    <td className="py-3 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => openOrderModal(o)}
                        className="inline-flex items-center gap-1 border border-[#111111] px-3 py-1.5 text-[11px] font-bold uppercase hover:bg-[#111111] hover:text-white"
                      >
                        <Eye className="w-3.5 h-3.5" /> Manage
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      ) : (
        <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Order #</th>
                <th className="py-3 px-3">Customer</th>
                <th className="py-3 px-3">Reason</th>
                <th className="py-3 px-3">Refund Amount</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {returns.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-neutral-500">
                    No return requests submitted.
                  </td>
                </tr>
              ) : (
                returns.map((r) => (
                  <tr key={r.id} className="border-b border-[#F0F0F0]">
                    <td className="py-3 px-4 font-bold">{r.orderNumber}</td>
                    <td className="py-3 px-3">{r.customerName}</td>
                    <td className="py-3 px-3">
                      <div className="font-medium">{r.reason}</div>
                      {r.comments && <div className="text-[11px] text-neutral-500">{r.comments}</div>}
                    </td>
                    <td className="py-3 px-3 font-bold">{formatCurrency(r.refundAmount)}</td>
                    <td className="py-3 px-3 font-semibold">{r.status}</td>
                    <td className="py-3 px-4 text-right space-x-2">
                      {r.status === 'REQUESTED' && (
                        <>
                          <button onClick={() => handleProcessReturn(r.id, 'APPROVED')} className="px-2.5 py-1 bg-[#111111] text-white text-[11px] font-bold uppercase">
                            Approve
                          </button>
                          <button onClick={() => handleProcessReturn(r.id, 'REJECTED')} className="px-2.5 py-1 border border-red-300 text-red-700 text-[11px] font-bold uppercase">
                            Reject
                          </button>
                        </>
                      )}
                      {r.status === 'APPROVED' && (
                        <button onClick={() => handleProcessReturn(r.id, 'REFUNDED')} className="px-2.5 py-1 bg-emerald-700 text-white text-[11px] font-bold uppercase">
                          Refund &amp; Restock
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Order Detail & Status Modal */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white border border-[#111111] max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 space-y-5 text-xs">
            <div className="flex items-center justify-between border-b border-[#E5E5E5] pb-3">
              <div>
                <h3 className="text-lg font-bold">{selectedOrder.orderNumber}</h3>
                <p className="text-neutral-500">
                  {selectedOrder.shippingFullName} ({selectedOrder.shippingPhone}) • {selectedOrder.shippingCity}, {selectedOrder.shippingState}
                </p>
              </div>
              <button onClick={() => setSelectedOrder(null)} className="text-sm font-bold px-2 py-1 border">
                Close
              </button>
            </div>

            <div className="space-y-2">
              <h4 className="font-bold uppercase tracking-wider text-[11px]">Ordered Items</h4>
              {selectedOrder.items?.map((item) => (
                <div key={item.id} className="flex justify-between border-b border-[#F5F5F5] py-2">
                  <div>
                    <span className="font-semibold">{item.productName}</span> — Size {item.size}, {item.color} (x{item.quantity})
                  </div>
                  <div className="font-bold">{formatCurrency(item.totalPrice)}</div>
                </div>
              ))}
              <div className="flex justify-between pt-2 text-sm font-bold">
                <span>Order Total</span>
                <span>{formatCurrency(selectedOrder.totalAmount)}</span>
              </div>
            </div>

            <form onSubmit={handleUpdateStatus} className="border-t border-[#E5E5E5] pt-4 space-y-3">
              <h4 className="font-bold uppercase tracking-wider text-[11px]">Update Fulfillment Status</h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold mb-1">Order Status</label>
                  <select
                    value={statusForm.status}
                    onChange={(e) => setStatusForm({ ...statusForm, status: e.target.value })}
                    className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none"
                  >
                    {ALL_STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {ORDER_STATUS_LABEL[s]}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-semibold mb-1">Tracking Number</label>
                  <input
                    value={statusForm.trackingNumber}
                    onChange={(e) => setStatusForm({ ...statusForm, trackingNumber: e.target.value })}
                    placeholder="e.g., QK8392011"
                    className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
                  />
                </div>
              </div>
              <div>
                <label className="block font-semibold mb-1">Notes</label>
                <input
                  value={statusForm.notes}
                  onChange={(e) => setStatusForm({ ...statusForm, notes: e.target.value })}
                  className="w-full border border-[#D4D4D4] px-3 py-2 outline-none"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button type="submit" className="bg-[#111111] text-white px-6 py-2.5 font-bold uppercase tracking-wider">
                  Update Order
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
