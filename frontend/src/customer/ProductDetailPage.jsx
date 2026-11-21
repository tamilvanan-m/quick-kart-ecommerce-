import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Heart, ShoppingBag, Star, Truck, ShieldCheck, Check } from 'lucide-react';
import { catalogApi } from '../services/catalogApi';
import { customerApi } from '../services/customerApi';
import { getApiErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';
import { formatCurrency, formatDate, resolveImageUrl } from '../utils/format';

export default function ProductDetailPage() {
  const { idOrSlug } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { addToCart, toggleWishlist, isInWishlist } = useCart();
  const { settings } = useStore();
  const { showToast } = useToast();

  const [product, setProduct] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [reviewForm, setReviewForm] = useState({ rating: 5, title: '', comment: '' });
  const [submittingReview, setSubmittingReview] = useState(false);
  const [addingToBag, setAddingToBag] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      try {
        const prod = await catalogApi.getProduct(idOrSlug);
        if (!active) return;
        setProduct(prod);
        setSelectedImage(0);
        const firstInStock = prod.variants?.find((v) => v.active && v.availableQuantity > 0) || prod.variants?.[0] || null;
        setSelectedVariant(firstInStock);

        const revRes = await catalogApi.getProductReviews(prod.id, 0, 10);
        if (active) setReviews(revRes?.content || []);
      } catch {
        if (active) setProduct(null);
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [idOrSlug]);

  if (loading) {
    return (
      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-16 text-center text-sm text-neutral-500">
        Loading product details...
      </div>
    );
  }

  if (!product) {
    return (
      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-20 text-center">
        <h2 className="text-2xl font-bold">Product Not Found</h2>
        <p className="mt-2 text-sm text-neutral-500">This item may have been removed or is currently unavailable.</p>
        <Link to="/shop" className="inline-block mt-6 bg-[#111111] text-white px-6 py-3 text-xs font-bold uppercase tracking-widest">
          Continue Shopping
        </Link>
      </div>
    );
  }

  const images = product.images?.length > 0 ? product.images : [{ imageUrl: product.primaryImageUrl }];
  const productSoldOut = Boolean(product.soldOut) || product.status === 'OUT_OF_STOCK' || product.totalAvailableStock <= 0;
  const variantStock = selectedVariant?.availableQuantity ?? 0;
  const isSelectedUnavailable = productSoldOut || !selectedVariant || variantStock <= 0;
  const activePrice = selectedVariant?.priceOverride || product.sellingPrice;
  const wishlisted = isInWishlist(product.id);

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      showToast('Please sign in to add items to your bag.', 'error');
      navigate('/login');
      return;
    }
    if (isSelectedUnavailable) {
      showToast('This size/item is currently SOLD OUT.', 'error');
      return;
    }
    setAddingToBag(true);
    try {
      await addToCart(product.id, selectedVariant.id, 1);
      showToast(`Added ${product.name} (${selectedVariant.size}) to your bag`);
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setAddingToBag(false);
    }
  };

  const handleWishlist = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      const added = await toggleWishlist(product.id);
      showToast(added ? 'Saved to wishlist' : 'Removed from wishlist');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setSubmittingReview(true);
    try {
      const created = await customerApi.submitReview(product.id, reviewForm);
      setReviews((prev) => [created, ...prev]);
      setReviewForm({ rating: 5, title: '', comment: '' });
      showToast('Thank you! Your verified review has been published.');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    } finally {
      setSubmittingReview(false);
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-8 sm:py-12">
      {/* Breadcrumb */}
      <div className="text-xs text-neutral-500 mb-6 flex items-center gap-2">
        <Link to="/" className="hover:text-[#111111]">Home</Link>
        <span>/</span>
        <Link to={`/shop?gender=${product.gender}`} className="hover:text-[#111111]">{product.gender}</Link>
        <span>/</span>
        <Link to={`/shop?category=${product.categorySlug}`} className="hover:text-[#111111]">{product.categoryName}</Link>
        <span>/</span>
        <span className="text-[#111111] font-medium truncate">{product.name}</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Image Gallery */}
        <div className="lg:col-span-7 flex flex-col-reverse sm:flex-row gap-4">
          {images.length > 1 && (
            <div className="flex sm:flex-col gap-3 overflow-x-auto sm:w-20 shrink-0">
              {images.map((img, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setSelectedImage(idx)}
                  className={`w-16 sm:w-20 aspect-[3/4] border overflow-hidden shrink-0 ${
                    idx === selectedImage ? 'border-[#111111]' : 'border-[#E5E5E5] opacity-70 hover:opacity-100'
                  }`}
                >
                  <img
                    src={resolveImageUrl(img.imageUrl)}
                    alt={`${product.name} ${idx + 1}`}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}

          <div className="relative flex-1 aspect-[3/4] bg-[#F5F5F5] border border-[#E5E5E5] overflow-hidden">
            <img
              src={resolveImageUrl(images[selectedImage]?.imageUrl)}
              alt={product.name}
              className={`w-full h-full object-cover ${productSoldOut ? 'opacity-75 grayscale-[30%]' : ''}`}
            />
            {productSoldOut && (
              <div className="absolute top-4 left-4 bg-[#111111] text-white px-4 py-1.5 text-xs font-bold uppercase tracking-widest">
                SOLD OUT
              </div>
            )}
          </div>
        </div>

        {/* Product Info */}
        <div className="lg:col-span-5">
          <div className="text-xs font-bold uppercase tracking-[0.18em] text-neutral-500">
            {product.brandName}
          </div>
          <h1 className="mt-1.5 text-2xl sm:text-3xl font-bold tracking-tight text-[#111111]">
            {product.name}
          </h1>

          {/* Rating Pill */}
          <div className="mt-3 flex items-center gap-3">
            {Number(product.ratingAvg) > 0 ? (
              <span className="inline-flex items-center gap-1.5 border border-[#E5E5E5] px-2.5 py-1 text-xs font-semibold">
                {Number(product.ratingAvg).toFixed(1)} <Star className="w-3.5 h-3.5 fill-[#111111]" />
                <span className="text-neutral-400">|</span>
                <span className="font-normal text-neutral-600">{product.reviewCount} Reviews</span>
              </span>
            ) : (
              <span className="text-xs text-neutral-500">New Season Release</span>
            )}
            <span className="text-xs text-neutral-400">SKU: {selectedVariant?.sku || product.sku}</span>
          </div>

          {/* Pricing */}
          <div className="mt-5 pb-5 border-b border-[#E5E5E5]">
            <div className="flex items-baseline gap-3">
              <span className="text-2xl font-bold text-[#111111]">{formatCurrency(activePrice)}</span>
              {Number(product.originalPrice) > Number(activePrice) && (
                <>
                  <span className="text-base text-neutral-400 line-through">
                    {formatCurrency(product.originalPrice)}
                  </span>
                  <span className="text-sm font-semibold text-red-700">
                    ({product.discountPercentage}% OFF)
                  </span>
                </>
              )}
            </div>
            <p className="text-[11px] text-neutral-500 mt-1">Inclusive of all taxes</p>
          </div>

          {/* Variant Color & Size Selector */}
          <div className="mt-6 space-y-5">
            {selectedVariant && (
              <div>
                <div className="text-xs font-bold uppercase tracking-wider mb-2">
                  Colour: <span className="font-normal text-neutral-600">{selectedVariant.color}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {Array.from(new Map((product.variants || []).map((v) => [v.color, v])).values()).map((cv) => (
                    <button
                      key={cv.color}
                      type="button"
                      onClick={() => setSelectedVariant(cv)}
                      className={`flex items-center gap-2 px-3 py-1.5 border text-xs ${
                        selectedVariant.color === cv.color
                          ? 'border-[#111111] font-semibold'
                          : 'border-[#E5E5E5] text-neutral-600'
                      }`}
                    >
                      {cv.colorHex && (
                        <span
                          className="w-3.5 h-3.5 rounded-full border border-neutral-300"
                          style={{ backgroundColor: cv.colorHex }}
                        />
                      )}
                      {cv.color}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between mb-2.5">
                <span className="text-xs font-bold uppercase tracking-wider">Select Size</span>
                {selectedVariant && (
                  <span
                    className={`text-xs font-medium ${
                      variantStock <= 0
                        ? 'text-red-700'
                        : variantStock <= 5
                        ? 'text-amber-700'
                        : 'text-emerald-700'
                    }`}
                  >
                    {variantStock <= 0
                      ? 'Out of Stock'
                      : variantStock <= 5
                      ? `Only ${variantStock} left in stock`
                      : `In Stock (${variantStock} available)`}
                  </span>
                )}
              </div>

              <div className="flex flex-wrap gap-2.5">
                {(product.variants || [])
                  .filter((v) => !selectedVariant || v.color === selectedVariant.color)
                  .map((v) => {
                    const out = !v.active || v.availableQuantity <= 0;
                    const active = selectedVariant?.id === v.id;
                    return (
                      <button
                        key={v.id}
                        type="button"
                        onClick={() => setSelectedVariant(v)}
                        className={`min-w-[52px] px-3.5 py-2.5 border text-xs font-semibold transition-colors relative ${
                          active
                            ? 'bg-[#111111] text-white border-[#111111]'
                            : out
                            ? 'bg-[#FAFAFA] text-neutral-400 border-[#E5E5E5] line-through'
                            : 'bg-white text-[#111111] border-[#D4D4D4] hover:border-[#111111]'
                        }`}
                      >
                        {v.size}
                      </button>
                    );
                  })}
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="mt-7 flex flex-col sm:flex-row gap-3">
            <button
              type="button"
              disabled={isSelectedUnavailable || addingToBag}
              onClick={handleAddToCart}
              className={`flex-1 py-4 px-6 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 transition-colors ${
                isSelectedUnavailable
                  ? 'bg-neutral-200 text-neutral-500 cursor-not-allowed'
                  : 'bg-[#111111] text-white hover:bg-neutral-800'
              }`}
            >
              <ShoppingBag className="w-4 h-4" />
              {productSoldOut || variantStock <= 0
                ? 'SOLD OUT'
                : addingToBag
                ? 'Adding...'
                : 'Add to Bag'}
            </button>

            <button
              type="button"
              onClick={handleWishlist}
              className="py-4 px-6 border border-[#111111] text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-[#F5F5F5] transition-colors"
            >
              <Heart className={`w-4 h-4 ${wishlisted ? 'fill-[#111111]' : ''}`} />
              {wishlisted ? 'Wishlisted' : 'Wishlist'}
            </button>
          </div>

          {/* Delivery & Description */}
          <div className="mt-8 border-t border-[#E5E5E5] pt-6 space-y-5 text-xs">
            <div className="space-y-2.5 bg-[#FAFAFA] p-4 border border-[#E5E5E5]">
              <div className="flex items-center gap-2.5">
                <Truck className="w-4 h-4 text-[#111111]" />
                <span>
                  Estimated delivery within <strong>3–5 business days</strong>. Free delivery above{' '}
                  {formatCurrency(settings.freeDeliveryThreshold || 999)}.
                </span>
              </div>
              <div className="flex items-center gap-2.5">
                <ShieldCheck className="w-4 h-4 text-[#111111]" />
                <span>{settings.returnPolicyText || `Easy ${settings.returnPolicyDays || 14}-day returns & exchanges.`}</span>
              </div>
            </div>

            <div>
              <h3 className="font-bold uppercase tracking-wider text-[11px] mb-2">Product Details</h3>
              <p className="text-neutral-700 leading-relaxed">{product.description}</p>
            </div>

            {product.materialCare && (
              <div>
                <h3 className="font-bold uppercase tracking-wider text-[11px] mb-1.5">Material &amp; Care</h3>
                <p className="text-neutral-600">{product.materialCare}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Customer Reviews Section */}
      <section className="mt-16 pt-12 border-t border-[#E5E5E5] grid grid-cols-1 lg:grid-cols-12 gap-10">
        <div className="lg:col-span-5">
          <h2 className="text-lg font-bold uppercase tracking-wider">Customer Reviews</h2>
          <p className="text-xs text-neutral-500 mt-1">
            Only verified buyers who purchased this item can submit a review.
          </p>

          <form onSubmit={handleSubmitReview} className="mt-6 border border-[#E5E5E5] p-5 space-y-4 bg-[#FAFAFA]">
            <h3 className="text-xs font-bold uppercase tracking-wider">Write a Review</h3>
            <div>
              <label className="block text-[11px] font-medium mb-1">Rating</label>
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setReviewForm({ ...reviewForm, rating: star })}
                    className={`p-1.5 border ${
                      reviewForm.rating >= star ? 'border-[#111111] bg-[#111111] text-white' : 'border-[#D4D4D4] bg-white'
                    }`}
                  >
                    <Star className="w-3.5 h-3.5 fill-current" />
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-[11px] font-medium mb-1">Headline</label>
              <input
                type="text"
                value={reviewForm.title}
                onChange={(e) => setReviewForm({ ...reviewForm, title: e.target.value })}
                placeholder="Summary of fit & fabric"
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none focus:border-[#111111]"
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium mb-1">Review</label>
              <textarea
                rows={3}
                required
                value={reviewForm.comment}
                onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                placeholder="Share your experience with sizing, fabric quality, and finish..."
                className="w-full border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none focus:border-[#111111]"
              />
            </div>
            <button
              type="submit"
              disabled={submittingReview}
              className="bg-[#111111] text-white px-5 py-2.5 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
            >
              {submittingReview ? 'Submitting...' : 'Submit Review'}
            </button>
          </form>
        </div>

        <div className="lg:col-span-7 space-y-4">
          {reviews.length === 0 ? (
            <div className="border border-[#E5E5E5] p-8 text-center text-xs text-neutral-500">
              No reviews yet for this product. Be the first verified customer to review it!
            </div>
          ) : (
            reviews.map((rev) => (
              <div key={rev.id} className="border border-[#E5E5E5] p-5 bg-white">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1">
                    {[...Array(5)].map((_, idx) => (
                      <Star
                        key={idx}
                        className={`w-3.5 h-3.5 ${
                          idx < rev.rating ? 'fill-[#111111] text-[#111111]' : 'text-neutral-300'
                        }`}
                      />
                    ))}
                    {rev.title && <span className="ml-2 text-xs font-bold">{rev.title}</span>}
                  </div>
                  <span className="text-[11px] text-neutral-400">{formatDate(rev.createdAt)}</span>
                </div>
                <p className="mt-2.5 text-xs text-neutral-700 leading-relaxed">{rev.comment}</p>
                <div className="mt-3 flex items-center gap-2 text-[11px] text-neutral-500">
                  <span className="font-semibold text-[#111111]">{rev.userName}</span>
                  {rev.verifiedPurchase && (
                    <span className="inline-flex items-center gap-1 text-emerald-700 font-medium">
                      <Check className="w-3 h-3" /> Verified Buyer
                    </span>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
