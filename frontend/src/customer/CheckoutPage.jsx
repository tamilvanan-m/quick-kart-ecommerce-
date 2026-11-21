import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ShieldCheck, CreditCard, Banknote, Plus, CheckCircle2 } from 'lucide-react';
import { customerApi } from '../services/customerApi';
import { getApiErrorMessage } from '../services/api';
import { useCart } from '../context/CartContext';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';
import { formatCurrency } from '../utils/format';

export default function CheckoutPage() {
  const { cart, refreshCartAndWishlist } = useCart();
  const { settings } = useStore();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState({
    fullName: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    addressType: 'HOME',
    isDefault: true,
  });
  const [paymentMethod, setPaymentMethod] = useState(settings.razorpayEnabled ? 'RAZORPAY' : 'COD');
  const [notes, setNotes] = useState('');
  const [placingOrder, setPlacingOrder] = useState(false);

  // Simulated Razorpay test mode modal state
  const [pendingPaymentOrder, setPendingPaymentOrder] = useState(null);
  const [verifying, setVerifying] = useState(false);

  useEffect(() => {
    customerApi.getAddresses().then((list) => {
      setAddresses(list || []);
      if (list && list.length > 0) {
        const def = list.find((a) => a.isDefault) || list[0];
        setSelectedAddressId(def.id);
      } else {
        setUseNewAddress(true);
      }
    }).catch(() => {});
  }, []);

  if (!cart.items || cart.items.length === 0) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <h2 className="text-2xl font-bold">Your Bag is Empty</h2>
        <p className="mt-2 text-xs text-neutral-500">Add items before proceeding to checkout.</p>
        <Link to="/shop" className="inline-block mt-6 bg-[#111111] text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest">
          Browse Shop
        </Link>
      </div>
    );
  }

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    if (!useNewAddress && !selectedAddressId) {
      showToast('Please select or add a delivery address.', 'error');
      return;
    }

    setPlacingOrder(true);
    try {
      const payload = {
        addressId: useNewAddress ? null : selectedAddressId,
        newAddress: useNewAddress ? newAddress : null,
        paymentMethod,
        couponCode: cart.couponCode || null,
        notes,
      };
      const createdOrder = await customerApi.placeOrder(payload);

      if (paymentMethod === 'COD') {
        await refreshCartAndWishlist();
        showToast('Order placed successfully!');
        navigate(`/orders/${createdOrder.id}`);
        return;
      }

      // Razorpay online flow
      const pd = createdOrder.paymentDetails;
      if (pd && !pd.testMode && window.Razorpay) {
        const rzp = new window.Razorpay({
          key: pd.keyId,
          amount: Math.round(Number(pd.amount) * 100),
          currency: pd.currency || 'INR',
          name: settings.storeName || 'Quick kart',
          description: `Order ${createdOrder.orderNumber}`,
          order_id: pd.gatewayOrderId,
          handler: async (response) => {
            await completeRazorpayVerify({
              orderId: createdOrder.id,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              simulatedSuccess: true,
            });
          },
          modal: {
            ondismiss: async () => {
              await completeRazorpayVerify({
                orderId: createdOrder.id,
                razorpayOrderId: pd.gatewayOrderId,
                razorpayPaymentId: 'cancelled',
                razorpaySignature: '',
                simulatedSuccess: false,
              });
            },
          },
        });
        rzp.open();
      } else {
        // Test Mode simulation modal
        setPendingPaymentOrder(createdOrder);
      }
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setPlacingOrder(false);
    }
  };

  const completeRazorpayVerify = async (verifyPayload) => {
    setVerifying(true);
    try {
      const confirmed = await customerApi.verifyPayment(verifyPayload);
      setPendingPaymentOrder(null);
      await refreshCartAndWishlist();
      showToast('Payment verified & order confirmed!');
      navigate(`/orders/${confirmed.id}`);
    } catch (err) {
      setPendingPaymentOrder(null);
      showToast(getApiErrorMessage(err, 'Payment failed or cancelled.'), 'error');
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-10">
      <h1 className="text-2xl font-bold tracking-tight uppercase pb-6 border-b border-[#E5E5E5]">
        Checkout
      </h1>

      <form onSubmit={handlePlaceOrder} className="mt-8 grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Left: Shipping & Payment */}
        <div className="lg:col-span-8 space-y-8">
          {/* Step 1: Delivery Address */}
          <div className="border border-[#E5E5E5] p-6 bg-white">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-bold uppercase tracking-wider">1. Delivery Address</h2>
              {addresses.length > 0 && (
                <button
                  type="button"
                  onClick={() => setUseNewAddress((v) => !v)}
                  className="text-xs font-semibold underline flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" />
                  {useNewAddress ? 'Use Saved Address' : 'Add New Address'}
                </button>
              )}
            </div>

            {!useNewAddress && addresses.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {addresses.map((addr) => (
                  <label
                    key={addr.id}
                    className={`border p-4 cursor-pointer text-xs space-y-1 transition-colors ${
                      selectedAddressId === addr.id
                        ? 'border-[#111111] bg-[#FAFAFA]'
                        : 'border-[#E5E5E5] hover:border-neutral-400'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold">{addr.fullName}</span>
                      <input
                        type="radio"
                        name="selectedAddr"
                        checked={selectedAddressId === addr.id}
                        onChange={() => setSelectedAddressId(addr.id)}
                        className="accent-[#111111]"
                      />
                    </div>
                    <p className="text-neutral-600">{addr.addressLine1}</p>
                    {addr.addressLine2 && <p className="text-neutral-600">{addr.addressLine2}</p>}
                    <p className="text-neutral-600">
                      {addr.city}, {addr.state} - {addr.postalCode}
                    </p>
                    <p className="font-medium pt-1">Phone: {addr.phone}</p>
                  </label>
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <input
                  required
                  placeholder="Recipient Full Name *"
                  value={newAddress.fullName}
                  onChange={(e) => setNewAddress({ ...newAddress, fullName: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  required
                  placeholder="Mobile Number *"
                  value={newAddress.phone}
                  onChange={(e) => setNewAddress({ ...newAddress, phone: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  required
                  placeholder="Address Line 1 (House No, Street) *"
                  value={newAddress.addressLine1}
                  onChange={(e) => setNewAddress({ ...newAddress, addressLine1: e.target.value })}
                  className="sm:col-span-2 border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  placeholder="Address Line 2 (Landmark, Optional)"
                  value={newAddress.addressLine2}
                  onChange={(e) => setNewAddress({ ...newAddress, addressLine2: e.target.value })}
                  className="sm:col-span-2 border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  required
                  placeholder="City *"
                  value={newAddress.city}
                  onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  required
                  placeholder="State *"
                  value={newAddress.state}
                  onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <input
                  required
                  placeholder="Postal PIN Code *"
                  value={newAddress.postalCode}
                  onChange={(e) => setNewAddress({ ...newAddress, postalCode: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
                />
                <select
                  value={newAddress.addressType}
                  onChange={(e) => setNewAddress({ ...newAddress, addressType: e.target.value })}
                  className="border border-[#D4D4D4] px-3 py-2.5 bg-white outline-none focus:border-[#111111]"
                >
                  <option value="HOME">Home</option>
                  <option value="WORK">Work</option>
                </select>
              </div>
            )}
          </div>

          {/* Step 2: Payment Method */}
          <div className="border border-[#E5E5E5] p-6 bg-white">
            <h2 className="text-sm font-bold uppercase tracking-wider mb-4">2. Payment Method</h2>
            <div className="space-y-3 text-xs">
              {settings.razorpayEnabled && (
                <label
                  className={`flex items-center justify-between border p-4 cursor-pointer ${
                    paymentMethod === 'RAZORPAY' ? 'border-[#111111] bg-[#FAFAFA]' : 'border-[#E5E5E5]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <CreditCard className="w-4 h-4" />
                    <div>
                      <div className="font-bold">Razorpay Online Payment (UPI / Cards / NetBanking)</div>
                      <div className="text-neutral-500 mt-0.5">Encrypted checkout with instant order confirmation</div>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="paymentMethod"
                    checked={paymentMethod === 'RAZORPAY'}
                    onChange={() => setPaymentMethod('RAZORPAY')}
                    className="accent-[#111111]"
                  />
                </label>
              )}

              {settings.codEnabled && (
                <label
                  className={`flex items-center justify-between border p-4 cursor-pointer ${
                    paymentMethod === 'COD' ? 'border-[#111111] bg-[#FAFAFA]' : 'border-[#E5E5E5]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Banknote className="w-4 h-4" />
                    <div>
                      <div className="font-bold">Cash on Delivery (COD)</div>
                      <div className="text-neutral-500 mt-0.5">Pay via cash or UPI when your order arrives</div>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="paymentMethod"
                    checked={paymentMethod === 'COD'}
                    onChange={() => setPaymentMethod('COD')}
                    className="accent-[#111111]"
                  />
                </label>
              )}
            </div>

            <div className="mt-4">
              <label className="block text-[11px] font-medium mb-1 text-neutral-600">
                Delivery Instructions (Optional)
              </label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g., Leave package with building security"
                className="w-full border border-[#D4D4D4] px-3 py-2 text-xs outline-none focus:border-[#111111]"
              />
            </div>
          </div>
        </div>

        {/* Right: Order Summary */}
        <div className="lg:col-span-4">
          <div className="border border-[#E5E5E5] p-6 bg-white space-y-4 text-xs">
            <h3 className="font-bold uppercase tracking-widest pb-3 border-b border-[#E5E5E5]">
              Order Summary ({cart.totalItems} items)
            </h3>

            <div className="space-y-3 max-h-60 overflow-y-auto pr-1">
              {cart.items.map((item) => (
                <div key={item.id} className="flex justify-between gap-3">
                  <div className="min-w-0">
                    <div className="font-medium truncate">{item.productName}</div>
                    <div className="text-[11px] text-neutral-500">
                      Size: {item.size} • Qty: {item.quantity}
                    </div>
                  </div>
                  <div className="font-semibold shrink-0">{formatCurrency(item.lineTotal)}</div>
                </div>
              ))}
            </div>

            <div className="pt-3 border-t border-[#E5E5E5] space-y-2">
              <div className="flex justify-between">
                <span className="text-neutral-600">Subtotal</span>
                <span>{formatCurrency(cart.subtotal)}</span>
              </div>
              {Number(cart.couponDiscount) > 0 && (
                <div className="flex justify-between text-emerald-700">
                  <span>Coupon ({cart.couponCode})</span>
                  <span>- {formatCurrency(cart.couponDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-neutral-600">Delivery</span>
                <span>{Number(cart.deliveryCharge) === 0 ? 'FREE' : formatCurrency(cart.deliveryCharge)}</span>
              </div>
              <div className="pt-3 border-t border-[#E5E5E5] flex justify-between text-base font-bold">
                <span>Payable Total</span>
                <span>{formatCurrency(cart.finalAmount)}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={placingOrder}
              className="w-full bg-[#111111] text-white py-4 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
            >
              {placingOrder
                ? 'Validating Stock...'
                : paymentMethod === 'RAZORPAY'
                ? `Pay ${formatCurrency(cart.finalAmount)}`
                : 'Confirm COD Order'}
            </button>

            <p className="text-[11px] text-neutral-500 flex items-center justify-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5" /> Stock &amp; prices verified securely on server
            </p>
          </div>
        </div>
      </form>

      {/* Razorpay Test Mode Modal */}
      {pendingPaymentOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white border border-[#111111] max-w-md w-full p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-[#E5E5E5] pb-3">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-widest bg-[#111111] text-white px-2 py-0.5">
                  RAZORPAY TEST MODE
                </span>
                <h3 className="text-lg font-bold mt-2">{settings.storeName || 'Quick kart'} Checkout</h3>
              </div>
              <div className="text-right">
                <div className="text-[11px] text-neutral-500">Amount</div>
                <div className="text-lg font-bold">{formatCurrency(pendingPaymentOrder.totalAmount)}</div>
              </div>
            </div>

            <div className="text-xs text-neutral-600 space-y-1.5 bg-[#FAFAFA] p-4 border border-[#E5E5E5]">
              <div>
                Order Reference: <strong>{pendingPaymentOrder.orderNumber}</strong>
              </div>
              <div>
                Gateway Order ID: <strong>{pendingPaymentOrder.paymentDetails?.gatewayOrderId}</strong>
              </div>
              <p className="pt-1 text-neutral-500">
                Stock is currently reserved for this transaction. Simulate a successful or failed test payment below:
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <button
                type="button"
                disabled={verifying}
                onClick={() =>
                  completeRazorpayVerify({
                    orderId: pendingPaymentOrder.id,
                    razorpayOrderId: pendingPaymentOrder.paymentDetails?.gatewayOrderId,
                    razorpayPaymentId: 'pay_test_' + Date.now(),
                    razorpaySignature: 'test_sig',
                    simulatedSuccess: true,
                  })
                }
                className="flex-1 bg-[#111111] text-white py-3 px-4 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-1.5 hover:bg-neutral-800"
              >
                <CheckCircle2 className="w-4 h-4" /> Complete Test Payment
              </button>

              <button
                type="button"
                disabled={verifying}
                onClick={() =>
                  completeRazorpayVerify({
                    orderId: pendingPaymentOrder.id,
                    razorpayOrderId: pendingPaymentOrder.paymentDetails?.gatewayOrderId,
                    razorpayPaymentId: 'pay_failed',
                    razorpaySignature: '',
                    simulatedSuccess: false,
                  })
                }
                className="border border-red-300 text-red-700 py-3 px-4 text-xs font-bold uppercase tracking-widest hover:bg-red-50"
              >
                Simulate Failure
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
