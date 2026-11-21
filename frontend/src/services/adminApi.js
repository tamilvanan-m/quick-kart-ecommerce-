import api from './api';

export const adminApi = {
  // Dashboard & Reports
  getDashboardStats: (days = 14) => api.get('/admin/dashboard/stats', { params: { days } }).then((r) => r.data),
  getRevenueSeries: (days = 30) => api.get('/admin/dashboard/revenue', { params: { days } }).then((r) => r.data),
  exportOrdersCsv: () =>
    api.get('/admin/reports/orders/export', { responseType: 'blob' }).then((r) => r.data),

  // Products
  getProducts: (params) => api.get('/admin/products', { params }).then((r) => r.data),
  getProduct: (id) => api.get(`/admin/products/${id}`).then((r) => r.data),
  createProduct: (payload) => api.post('/admin/products', payload).then((r) => r.data),
  updateProduct: (id, payload) => api.put(`/admin/products/${id}`, payload).then((r) => r.data),
  deleteProduct: (id) => api.delete(`/admin/products/${id}`).then((r) => r.data),

  // Image Upload
  uploadImage: (file, folder = 'products') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', folder);
    return api
      .post('/admin/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then((r) => r.data);
  },

  // Categories
  getCategories: () => api.get('/admin/categories').then((r) => r.data),
  createCategory: (payload) => api.post('/admin/categories', payload).then((r) => r.data),
  updateCategory: (id, payload) => api.put(`/admin/categories/${id}`, payload).then((r) => r.data),
  deleteCategory: (id) => api.delete(`/admin/categories/${id}`).then((r) => r.data),

  // Brands
  getBrands: () => api.get('/admin/brands').then((r) => r.data),
  createBrand: (payload) => api.post('/admin/brands', payload).then((r) => r.data),
  updateBrand: (id, payload) => api.put(`/admin/brands/${id}`, payload).then((r) => r.data),
  deleteBrand: (id) => api.delete(`/admin/brands/${id}`).then((r) => r.data),

  // Inventory
  getInventory: () => api.get('/admin/inventory').then((r) => r.data),
  getLowStock: () => api.get('/admin/inventory/low-stock').then((r) => r.data),
  updateVariantStock: (variantId, payload) => api.put(`/admin/inventory/${variantId}`, payload).then((r) => r.data),
  getInventoryTransactions: (page = 0, size = 20) =>
    api.get('/admin/inventory/transactions', { params: { page, size } }).then((r) => r.data),

  // Orders & Returns
  getOrders: (params) => api.get('/admin/orders', { params }).then((r) => r.data),
  getOrder: (id) => api.get(`/admin/orders/${id}`).then((r) => r.data),
  updateOrderStatus: (id, payload) => api.put(`/admin/orders/${id}/status`, payload).then((r) => r.data),
  getReturns: () => api.get('/admin/returns').then((r) => r.data),
  processReturn: (id, payload) => api.put(`/admin/returns/${id}`, payload).then((r) => r.data),

  // Customers & Staff
  getCustomers: (params) => api.get('/admin/customers', { params }).then((r) => r.data),
  toggleCustomerStatus: (id, active) =>
    api.put(`/admin/customers/${id}/status`, null, { params: { active } }).then((r) => r.data),
  getStaff: () => api.get('/admin/staff').then((r) => r.data),
  createStaff: (payload) => api.post('/admin/staff', payload).then((r) => r.data),
  updateStaff: (id, payload) => api.put(`/admin/staff/${id}`, payload).then((r) => r.data),

  // Coupons
  getCoupons: () => api.get('/admin/coupons').then((r) => r.data),
  createCoupon: (payload) => api.post('/admin/coupons', payload).then((r) => r.data),
  updateCoupon: (id, payload) => api.put(`/admin/coupons/${id}`, payload).then((r) => r.data),
  deleteCoupon: (id) => api.delete(`/admin/coupons/${id}`).then((r) => r.data),

  // Banners
  getBanners: () => api.get('/admin/banners').then((r) => r.data),
  createBanner: (payload) => api.post('/admin/banners', payload).then((r) => r.data),
  updateBanner: (id, payload) => api.put(`/admin/banners/${id}`, payload).then((r) => r.data),
  deleteBanner: (id) => api.delete(`/admin/banners/${id}`).then((r) => r.data),

  // Reviews
  getReviews: (page = 0, size = 15) => api.get('/admin/reviews', { params: { page, size } }).then((r) => r.data),
  toggleReviewApproval: (id, approved) =>
    api.put(`/admin/reviews/${id}/approval`, null, { params: { approved } }).then((r) => r.data),
  deleteReview: (id) => api.delete(`/admin/reviews/${id}`).then((r) => r.data),

  // Settings & Audit Logs
  getSettings: () => api.get('/admin/settings').then((r) => r.data),
  updateSettings: (payload) => api.put('/admin/settings', payload).then((r) => r.data),
  getAuditLogs: (page = 0, size = 25) => api.get('/admin/audit-logs', { params: { page, size } }).then((r) => r.data),
};
