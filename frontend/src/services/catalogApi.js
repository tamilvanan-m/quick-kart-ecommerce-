import api from './api';

export const catalogApi = {
  getProducts: (params) => api.get('/products', { params }).then((r) => r.data),
  getProduct: (idOrSlug) => api.get(`/products/${idOrSlug}`).then((r) => r.data),
  searchSuggestions: (q) => api.get('/products/suggestions', { params: { q } }).then((r) => r.data),
  getCategories: () => api.get('/categories').then((r) => r.data),
  getFlatCategories: () => api.get('/categories/flat').then((r) => r.data),
  getBrands: () => api.get('/brands').then((r) => r.data),
  getBanners: (placement) => api.get('/banners', { params: placement ? { placement } : {} }).then((r) => r.data),
  getStoreSettings: () => api.get('/store/settings').then((r) => r.data),
  getPublicCoupons: () => api.get('/coupons/public').then((r) => r.data),
  getProductReviews: (productId, page = 0, size = 10) =>
    api.get(`/reviews/product/${productId}`, { params: { page, size } }).then((r) => r.data),
  getFeaturedReviews: () => api.get('/reviews/featured').then((r) => r.data),
  subscribeNewsletter: (email) => api.post('/newsletter/subscribe', { email }).then((r) => r.data),
};
