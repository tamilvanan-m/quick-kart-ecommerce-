import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Check } from 'lucide-react';
import { customerApi } from '../services/customerApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { formatCurrency, formatDate, formatDateTime, ORDER_STATUS_STEPS, ORDER_STATUS_LABEL, resolveImageUrl } from '../utils/format';

export default function OrderDetailPage() {
  const { id } = useParams();
  const { showToast } = useToast();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [cancelReason, setCancelReason] = useState('');
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [returnForm, setReturnForm] = useState({ reason: 'Size did not fit', comments: '' });
  const [showReturnModal, setShowReturnModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const loadOrder = async () => {
    setLoading(true);
    try {
      const res = await customerApi.getMyOrder(id);
      setOrder(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrder();
  }, [id]);

  if (loading) {
    return <div className="max-w-5xl mx-auto px-4 py-16 text-center text-xs text-neutral-500">Loading order...</div>;
  }
  if (!order) {
    return <div className="max-w-5xl mx-auto px-4 py-16 text-center text-sm">Order not found.</div>;
  }

  const currentStepIndex = ORDER_STATUS_STEPS.indexOf(order.orderStatus);
  const canCancel = ['PLACED', 'CONFIRMED', 'PACKED'].includes(order.orderStatus);
  const canReturn = order.orderStatus === 'DELIVERED' && !order.returnRequest;

  const handleCancel = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const updated = await customerApi.cancelOrder(order.id, cancelReason || 'Changed mind');
      setOrder(updated);
      setShowCancelModal(false);
      showToast('Order cancelled and stock restored.');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReturn = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await customerApi.requestReturn(order.id, returnForm);
      await loadOrder();
      setShowReturnModal(false);
      showToast('Return request submitted.');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 space-y-8">
      <div>
        <Link to="/orders" className="inline-flex items-center gap-1.5 text-xs font-semibold hover:underline mb-4">
          <ArrowLeft className="w-3.5 h-3.5" /> Back to My Orders
        </Link>
        <div className="flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-[#E5E5E5]">
          <div>
            <h1 className="text-2xl font-bold">{order.orderNumber}</h1>
            <p className="text-xs text-neutral-500 mt-1">
              Placed on {formatDateTime(order.createdAt)} • {order.paymentMethod} ({order.paymentStatus})
            </p>
          </div>
          <div className="flex gap-3">
            {canCancel && (
              <button
                type="button"
                onClick={() => setShowCancelModal(true)}
                className="border border-red-300 text-red-700 px-4 py-2 text-xs font-bold uppercase tracking-wider hover:bg-red-50"
              >
                Cancel Order
              </button>
            )}
            {canReturn && (
              <button
                type="button"
                onClick={() => setShowReturnModal(true)}
                className="border border-[#111111] px-4 py-2 text-xs font-bold uppercase tracking-wider hover:bg-[#111111] hover:text-white"
              >
                Request Return
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Order Tracking Progress */}
      <div className="border border-[#E5E5E5] p-6 bg-white">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-6">
          <h2 className="text-xs font-bold uppercase tracking-widest">Order Tracking</h2>
          <div className="text-xs text-neutral-600">
            {order.trackingNumber && <span className="mr-4">Tracking #: <strong>{order.trackingNumber}</strong></span>}
            {order.estimatedDeliveryDate && order.orderStatus !== 'CANCELLED' && (
              <span>Est. Delivery: <strong>{formatDate(order.estimatedDeliveryDate)}</strong></span>
            )}
          </div>
        </div>

        {['CANCELLED', 'RETURN_REQUESTED', 'RETURNED'].includes(order.orderStatus) ? (
          <div className="p-4 border border-[#E5E5E5] bg-[#FAFAFA] text-xs">
            <div className="font-bold uppercase tracking-wider">
              Status: {ORDER_STATUS_LABEL[order.orderStatus]}
            </div>
            {order.cancellationReason && (
              <p className="mt-1 text-neutral-600">Reason: {order.cancellationReason}</p>
            )}
            {order.returnRequest && (
              <p className="mt-1 text-neutral-600">
                Return Status: <strong>{order.returnRequest.status}</strong> — {order.returnRequest.reason}
              </p>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-6 gap-4">
            {ORDER_STATUS_STEPS.map((step, idx) => {
              const completed = currentStepIndex >= idx;
              return (
                <div key={step} className="flex flex-col items-start sm:items-center text-left sm:text-center">
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center border text-xs font-bold ${
                      completed
                        ? 'bg-[#111111] text-white border-[#111111]'
                        : 'bg-white text-neutral-400 border-[#D4D4D4]'
                    }`}
                  >
                    {completed ? <Check className="w-3.5 h-3.5" /> : idx + 1}
                  </div>
                  <span className={`mt-2 text-[11px] font-semibold ${completed ? 'text-[#111111]' : 'text-neutral-400'}`}>
                    {ORDER_STATUS_LABEL[step]}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Items + Shipping / Payment */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 border border-[#E5E5E5] p-6 bg-white space-y-4">
          <h2 className="text-xs font-bold uppercase tracking-widest pb-3 border-b border-[#F0F0F0]">
            Ordered Items
          </h2>
          {order.items?.map((item) => (
            <div key={item.id} className="flex items-center gap-4 pb-4 border-b border-[#F5F5F5] last:border-b-0 last:pb-0">
              <img
                src={resolveImageUrl(item.imageUrl)}
                alt={item.productName}
                className="w-16 h-20 object-cover bg-[#F5F5F5] border border-[#E5E5E5]"
              />
              <div className="flex-1 min-w-0 text-xs">
                <div className="text-[11px] uppercase tracking-wider text-neutral-500">{item.brandName}</div>
                <Link to={`/product/${item.productId}`} className="font-semibold text-sm hover:underline">
                  {item.productName}
                </Link>
                <div className="text-neutral-500 mt-1">
                  Size: {item.size} • Colour: {item.color} • Qty: {item.quantity}
                </div>
              </div>
              <div className="text-sm font-bold">{formatCurrency(item.totalPrice)}</div>
            </div>
          ))}
        </div>

        <div className="lg:col-span-4 space-y-5">
          <div className="border border-[#E5E5E5] p-5 bg-white text-xs space-y-1.5">
            <h3 className="font-bold uppercase tracking-widest pb-2 border-b border-[#F0F0F0]">
              Shipping Address
            </h3>
            <p className="font-semibold pt-1">{order.shippingFullName}</p>
            <p className="text-neutral-600">{order.shippingAddressLine1}</p>
            {order.shippingAddressLine2 && <p className="text-neutral-600">{order.shippingAddressLine2}</p>}
            <p className="text-neutral-600">
              {order.shippingCity}, {order.shippingState} - {order.shippingPostalCode}
            </p>
            <p className="text-neutral-600">Phone: {order.shippingPhone}</p>
          </div>

          <div className="border border-[#E5E5E5] p-5 bg-white text-xs space-y-2">
            <h3 className="font-bold uppercase tracking-widest pb-2 border-b border-[#F0F0F0]">
              Payment Summary
            </h3>
            <div className="flex justify-between">
              <span className="text-neutral-600">Subtotal</span>
              <span>{formatCurrency(order.subtotal)}</span>
            </div>
            {Number(order.discountAmount) > 0 && (
              <div className="flex justify-between text-emerald-700">
                <span>Coupon Discount</span>
                <span>- {formatCurrency(order.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-neutral-600">Delivery</span>
              <span>{Number(order.deliveryCharge) === 0 ? 'FREE' : formatCurrency(order.deliveryCharge)}</span>
            </div>
            <div className="pt-2 border-t border-[#E5E5E5] flex justify-between text-sm font-bold">
              <span>Total Paid / Payable</span>
              <span>{formatCurrency(order.totalAmount)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleCancel} className="bg-white border border-[#111111] max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold uppercase tracking-wider">Cancel Order</h3>
            <p className="text-xs text-neutral-600">
              Are you sure you want to cancel order <strong>{order.orderNumber}</strong>?
            </p>
            <textarea
              required
              rows={3}
              placeholder="Reason for cancellation..."
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              className="w-full border border-[#D4D4D4] p-3 text-xs outline-none focus:border-[#111111]"
            />
            <div className="flex justify-end gap-3">
              <button type="button" onClick={() => setShowCancelModal(false)} className="px-4 py-2 text-xs border border-[#D4D4D4]">
                Keep Order
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="bg-red-700 text-white px-5 py-2 text-xs font-bold uppercase tracking-wider"
              >
                {submitting ? 'Cancelling...' : 'Confirm Cancel'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Return Modal */}
      {showReturnModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleReturn} className="bg-white border border-[#111111] max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold uppercase tracking-wider">Request Return</h3>
            <select
              value={returnForm.reason}
              onChange={(e) => setReturnForm({ ...returnForm, reason: e.target.value })}
              className="w-full border border-[#D4D4D4] px-3 py-2 text-xs bg-white outline-none focus:border-[#111111]"
            >
              <option>Size did not fit</option>
              <option>Product damaged or defective</option>
              <option>Wrong item delivered</option>
              <option>Quality not as expected</option>
            </select>
            <textarea
              rows={3}
              placeholder="Additional comments..."
              value={returnForm.comments}
              onChange={(e) => setReturnForm({ ...returnForm, comments: e.target.value })}
              className="w-full border border-[#D4D4D4] p-3 text-xs outline-none focus:border-[#111111]"
            />
            <div className="flex justify-end gap-3">
              <button type="button" onClick={() => setShowReturnModal(false)} className="px-4 py-2 text-xs border border-[#D4D4D4]">
                Close
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="bg-[#111111] text-white px-5 py-2 text-xs font-bold uppercase tracking-wider"
              >
                {submitting ? 'Submitting...' : 'Submit Request'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
