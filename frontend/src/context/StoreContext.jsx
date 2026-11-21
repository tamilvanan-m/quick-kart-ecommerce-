import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { catalogApi } from '../services/catalogApi';

const StoreContext = createContext(null);

export function StoreProvider({ children }) {
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [settings, setSettings] = useState({
    storeName: 'Quick kart',
    contactEmail: 'support@quickkart.com',
    deliveryCharge: 79,
    freeDeliveryThreshold: 999,
    returnPolicyDays: 14,
    codEnabled: true,
    razorpayEnabled: true,
  });
  const [loading, setLoading] = useState(true);

  const refreshStoreData = useCallback(async () => {
    try {
      const [cats, brs, st] = await Promise.all([
        catalogApi.getCategories(),
        catalogApi.getBrands(),
        catalogApi.getStoreSettings(),
      ]);
      setCategories(cats || []);
      setBrands(brs || []);
      if (st) setSettings(st);
    } catch {
      // Keep graceful defaults if backend is unreachable temporarily
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshStoreData();
  }, [refreshStoreData]);

  return (
    <StoreContext.Provider value={{ categories, brands, settings, loading, refreshStoreData }}>
      {children}
    </StoreContext.Provider>
  );
}

export const useStore = () => useContext(StoreContext);
