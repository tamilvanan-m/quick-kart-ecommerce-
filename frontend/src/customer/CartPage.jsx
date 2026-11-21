import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Trash2, Plus, Minus, Tag, ArrowRight, ShoppingBag } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { catalogApi } from '../services/catalogApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { formatCurrency, resolveImageUrl } from '../utils/format';

export default function CartPage() {
  const { isAuthenticated } = useAuth();
  const { cart, updateCartItem, removeCartItem, applyCoupon, removeCoupon } = useCart();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [couponInput, setCouponInput] = useState('');
  const [publicCoupons, setPublicCoupons] = useState([]);
  const [variantMap, setVariantMap] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    catalogApi.getPublicCoupons().then((res) => setPublicCoupons(res || [])).catch(() => {});
  }, []);

  useEffect(() => {
    // Load variants for products in cart so users can switch size directly in bag
    const ids = [...new Set((cart.items || []).map((i) => i.productId))];
    ids.forEach((pid) => {
      if (!variantMap[pid]) {
        catalogApi.getProduct(String(pid)).then((prod) => {
          setVariantMap((prev) => ({ ...prev, [pid]: prod.variants || [] }));
        }).catch(() => {});
      }
    });
  }, [cart.items]);

  if (!isAuthenticated) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <ShoppingBag className="w-10 h-10 mx-auto text-neutral-400" />
        <h1 className="mt-4 text-2xl font-bold">Your Shopping Bag</h1>
        <p className="mt-2 text-xs text-neutral-500">Sign in to view your saved bag items across devices.</p>
        <Link
          to="/login"
          className="inline-block mt-6 bg-[#111111] text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest"
        >
          Sign In
        </Link>
      </div>
    );
  }

  const items = cart.items || [];
  const hasSoldOutItems = items.some((i) => i.soldOut || i.availableStock < i.quantity);

  const handleQtyChange = async (item, nextQty) => {
    if (nextQty < 1) return;
    setBusy(true);
    try {
      await updateCartItem(item.id, { quantity: nextQty });
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleSizeChange = async (item, newVariantId) => {
    setBusy(true);
    try {
      await updateCartItem(item.id, { newVariantId: Number(newVariantId), quantity: item.quantity });
      showToast('Size updated');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleApplyCoupon = async (e) => {
    e.preventDefault();
    if (!couponInput.trim()) return;
    setBusy(true);
    try {
      await applyCoupon(couponInput.trim());
      showToast('Coupon applied');
      setCouponInput('');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <ShoppingBag className="w-10 h-10 mx-auto text-neutral-400" />
        <h1 className="mt-4 text-2xl font-bold">Your Bag is Empty</h1>
        <p className="mt-2 text-xs text-neutral-500">Explore our latest arrivals and seasonal collections.</p>
        <Link
          to="/shop"
          className="inline-block mt-6 bg-[#111111] text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest"
        >
          Shop Collection
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-10">
      <h1 className="text-2xl font-bold tracking-tight uppercase pb-6 border-b border-[#E5E5E5]">
        Shopping Bag ({cart.totalItems})
      </h1>

      <div className="mt-8 grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Left: Items */}
        <div className="lg:col-span-8 space-y-4">
          {items.map((item) => {
            const productVariants = variantMap[item.productId] || [];
            return (
              <div key={item.id} className="border border-[#E5E5E5] p-4 flex gap-4 bg-white">
                <Link to={`/product/${item.productSlug}`} className="w-24 sm:w-28 aspect-[3/4] bg-[#F5F5F5] shrink-0">
                  <img
                    src={resolveImageUrl(item.imageUrl)}
                    alt={item.productName}
                    className="w-full h-full object-cover"
                  />
                </Link>

                <div className="flex-1 min-w-0 flex flex-col justify-between">
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="text-[11px] font-bold uppercase tracking-wider text-neutral-500">
                          {item.brandName}
                        </p>
                        <Link to={`/product/${item.productSlug}`} className="text-sm font-semibold hover:underline">
                          {item.productName}
                        </Link>
                      </div>
                      <button
                        type="button"
                        onClick={() => removeCartItem(item.id)}
                        className="text-neutral-400 hover:text-red-700 p-1"
                        aria-label="Remove item"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="mt-2 flex flex-wrap items-center gap-3 text-xs">
                      <span className="text-neutral-500">Colour: {item.color}</span>
                      <div className="flex items-center gap-1.5">
                        <span className="text-neutral-500">Size:</span>
                        {productVariants.length > 0 ? (
                          <select
                            value={item.variantId}
                            disabled={busy}
                            onChange={(e) => handleSizeChange(item, e.target.value)}
                            className="border border-[#D4D4D4] px-2 py-1 text-xs bg-white outline-none focus:border-[#111111]"
                          >
                            {productVariants.map((v) => (
                              <option
                                key={v.id}
                                value={v.id}
                                disabled={!v.active || v.availableQuantity <= 0}
                              >
                                {v.size} {v.availableQuantity <= 0 ? '(Sold Out)' : ''}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <span className="font-semibold">{item.size}</span>
                        )}
                      </div>
                    </div>

                    {item.soldOut && (
                      <p className="mt-2 text-xs font-semibold text-red-700">
                        This item is currently SOLD OUT. Remove it before proceeding.
                      </p>
                    )}
                    {!item.soldOut && item.availableStock < item.quantity && (
                      <p className="mt-2 text-xs font-semibold text-red-700">
                        Only {item.availableStock} left in stock. Please reduce quantity.
                      </p>
                    )}
                  </div>

                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                    <div className="inline-flex items-center border border-[#D4D4D4]">
                      <button
                        type="button"
                        disabled={busy || item.quantity <= 1}
                        onClick={() => handleQtyChange(item, item.quantity - 1)}
                        className="px-2.5 py-1.5 hover:bg-[#F5F5F5] disabled:opacity-40"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="px-3.5 text-xs font-semibold">{item.quantity}</span>
                      <button
                        type="button"
                        disabled={busy || item.quantity >= item.availableStock}
                        onClick={() => handleQtyChange(item, item.quantity + 1)}
                        className="px-2.5 py-1.5 hover:bg-[#F5F5F5] disabled:opacity-40"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <div className="text-right">
                      <span className="text-sm font-bold">{formatCurrency(item.lineTotal)}</span>
                      {Number(item.originalPrice) > Number(item.unitPrice) && (
                        <span className="ml-2 text-xs text-neutral-400 line-through">
                          {formatCurrency(Number(item.originalPrice) * item.quantity)}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Right: Summary & Coupon */}
        <div className="lg:col-span-4 space-y-5">
          {/* Coupon Box */}
          <div className="border border-[#E5E5E5] p-5 bg-[#FAFAFA]">
            <h3 className="text-xs font-bold uppercase tracking-widest flex items-center gap-2">
              <Tag className="w-3.5 h-3.5" /> Apply Coupon
            </h3>
            {cart.couponCode ? (
              <div className="mt-3 flex items-center justify-between bg-white border border-[#111111] px-3 py-2 text-xs">
                <span>
                  Applied: <strong>{cart.couponCode}</strong> (-{formatCurrency(cart.couponDiscount)})
                </span>
                <button
                  type="button"
                  onClick={() => removeCoupon()}
                  className="text-red-700 font-semibold hover:underline"
                >
                  Remove
                </button>
              </div>
            ) : (
              <form onSubmit={handleApplyCoupon} className="mt-3 flex gap-2">
                <input
                  type="text"
                  value={couponInput}
                  onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                  placeholder="Enter code (e.g. WELCOME10)"
                  className="flex-1 bg-white border border-[#D4D4D4] px-3 py-2 text-xs uppercase outline-none focus:border-[#111111]"
                />
                <button
                  type="submit"
                  disabled={busy}
                  className="bg-[#111111] text-white px-4 py-2 text-xs font-bold uppercase tracking-wider"
                >
                  Apply
                </button>
              </form>
            )}

            {publicCoupons.length > 0 && !cart.couponCode && (
              <div className="mt-3 space-y-1.5">
                {publicCoupons.slice(0, 2).map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setCouponInput(c.code)}
                    className="block w-full text-left text-[11px] text-neutral-600 hover:text-[#111111]"
                  >
                    Tap to use <strong>{c.code}</strong> — {c.description}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Price Details */}
          <div className="border border-[#E5E5E5] p-5 bg-white space-y-3 text-xs">
            <h3 className="font-bold uppercase tracking-widest pb-3 border-b border-[#E5E5E5]">
              Price Summary
            </h3>
            <div className="flex justify-between">
              <span className="text-neutral-600">Total MRP</span>
              <span>{formatCurrency(cart.mrpTotal)}</span>
            </div>
            <div className="flex justify-between text-emerald-700">
              <span>Bag Discount</span>
              <span>- {formatCurrency(cart.productSavings)}</span>
            </div>
            {Number(cart.couponDiscount) > 0 && (
              <div className="flex justify-between text-emerald-700">
                <span>Coupon ({cart.couponCode})</span>
                <span>- {formatCurrency(cart.couponDiscount)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-neutral-600">Delivery Charges</span>
              <span>
                {Number(cart.deliveryCharge) === 0 ? (
                  <span className="text-emerald-700 font-semibold">FREE</span>
                ) : (
                  formatCurrency(cart.deliveryCharge)
                )}
              </span>
            </div>
            <div className="pt-3 border-t border-[#E5E5E5] flex justify-between text-base font-bold">
              <span>Total Amount</span>
              <span>{formatCurrency(cart.finalAmount)}</span>
            </div>

            <button
              type="button"
              disabled={hasSoldOutItems}
              onClick={() => navigate('/checkout')}
              className="mt-4 w-full bg-[#111111] text-white py-4 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:bg-neutral-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              Proceed to Checkout <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
