import api from './api';

export const authApi = {
  login: (payload) => api.post('/auth/login', payload).then((r) => r.data),
  register: (payload) => api.post('/auth/register', payload).then((r) => r.data),
  me: () => api.get('/auth/me').then((r) => r.data),
  updateProfile: (payload) => api.put('/auth/me', payload).then((r) => r.data),
  changePassword: (payload) => api.post('/auth/change-password', payload).then((r) => r.data),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }).then((r) => r.data),
  resetPassword: (payload) => api.post('/auth/reset-password', payload).then((r) => r.data),
};

export const customerApi = {
  // Cart
  getCart: () => api.get('/cart').then((r) => r.data),
  addToCart: (productId, variantId, quantity = 1) =>
    api.post('/cart/items', { productId, variantId, quantity }).then((r) => r.data),
  updateCartItem: (itemId, payload) => api.put(`/cart/items/${itemId}`, payload).then((r) => r.data),
  removeCartItem: (itemId) => api.delete(`/cart/items/${itemId}`).then((r) => r.data),
  applyCoupon: (code) => api.post('/cart/coupon', { code }).then((r) => r.data),
  removeCoupon: () => api.delete('/cart/coupon').then((r) => r.data),

  // Wishlist
  getWishlist: () => api.get('/wishlist').then((r) => r.data),
  addToWishlist: (productId) => api.post(`/wishlist/${productId}`).then((r) => r.data),
  removeFromWishlist: (productId) => api.delete(`/wishlist/${productId}`).then((r) => r.data),
  moveWishlistToCart: (productId, variantId) =>
    api.post(`/wishlist/${productId}/move-to-cart`, null, { params: variantId ? { variantId } : {} }).then((r) => r.data),

  // Addresses
  getAddresses: () => api.get('/addresses').then((r) => r.data),
  createAddress: (payload) => api.post('/addresses', payload).then((r) => r.data),
  updateAddress: (id, payload) => api.put(`/addresses/${id}`, payload).then((r) => r.data),
  deleteAddress: (id) => api.delete(`/addresses/${id}`).then((r) => r.data),

  // Orders & Payments
  placeOrder: (payload) => api.post('/orders', payload).then((r) => r.data),
  verifyPayment: (payload) => api.post('/payments/verify', payload).then((r) => r.data),
  getMyOrders: (page = 0, size = 10) => api.get('/orders/my-orders', { params: { page, size } }).then((r) => r.data),
  getMyOrder: (id) => api.get(`/orders/${id}`).then((r) => r.data),
  cancelOrder: (id, reason) => api.post(`/orders/${id}/cancel`, { reason }).then((r) => r.data),
  requestReturn: (id, payload) => api.post(`/orders/${id}/return`, payload).then((r) => r.data),

  // Reviews
  submitReview: (productId, payload) => api.post(`/reviews/product/${productId}`, payload).then((r) => r.data),
};
