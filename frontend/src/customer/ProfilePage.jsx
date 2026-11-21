import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Trash2, Plus, MapPin, User, KeyRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authApi, customerApi } from '../services/customerApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';

export default function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const { showToast } = useToast();

  const [profileForm, setProfileForm] = useState({ fullName: '', phone: '' });
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '' });
  const [addresses, setAddresses] = useState([]);
  const [showAddAddress, setShowAddAddress] = useState(false);
  const [addrForm, setAddrForm] = useState({
    fullName: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    addressType: 'HOME',
    isDefault: false,
  });

  useEffect(() => {
    if (user) {
      setProfileForm({ fullName: user.fullName || '', phone: user.phone || '' });
    }
    customerApi.getAddresses().then((res) => setAddresses(res || [])).catch(() => {});
  }, [user]);

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    try {
      await authApi.updateProfile(profileForm);
      await refreshProfile();
      showToast('Profile updated');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    try {
      await authApi.changePassword(pwForm);
      setPwForm({ currentPassword: '', newPassword: '' });
      showToast('Password updated successfully');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleAddAddress = async (e) => {
    e.preventDefault();
    try {
      const created = await customerApi.createAddress(addrForm);
      setAddresses((prev) => [created, ...prev]);
      setShowAddAddress(false);
      setAddrForm({
        fullName: '',
        phone: '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'India',
        addressType: 'HOME',
        isDefault: false,
      });
      showToast('Address added');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleDeleteAddress = async (id) => {
    try {
      await customerApi.deleteAddress(id);
      setAddresses((prev) => prev.filter((a) => a.id !== id));
      showToast('Address removed');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 space-y-10">
      <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-[#E5E5E5]">
        <div>
          <h1 className="text-2xl font-bold uppercase tracking-tight">My Account</h1>
          <p className="text-xs text-neutral-500 mt-1">{user?.email}</p>
        </div>
        <div className="flex gap-3 text-xs">
          <Link to="/orders" className="border border-[#111111] px-4 py-2 font-semibold uppercase tracking-wider hover:bg-[#111111] hover:text-white">
            Order History
          </Link>
          <Link to="/wishlist" className="border border-[#E5E5E5] px-4 py-2 font-semibold uppercase tracking-wider hover:border-[#111111]">
            Wishlist
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Personal Info */}
        <form onSubmit={handleSaveProfile} className="border border-[#E5E5E5] p-6 bg-white space-y-4">
          <h2 className="text-xs font-bold uppercase tracking-widest flex items-center gap-2">
            <User className="w-4 h-4" /> Personal Information
          </h2>
          <div className="space-y-3 text-xs">
            <div>
              <label className="block font-medium mb-1">Full Name</label>
              <input
                required
                value={profileForm.fullName}
                onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
            <div>
              <label className="block font-medium mb-1">Email</label>
              <input
                disabled
                value={user?.email || ''}
                className="w-full border border-[#E5E5E5] bg-[#F5F5F5] px-3 py-2.5 text-neutral-500"
              />
            </div>
            <div>
              <label className="block font-medium mb-1">Phone Number</label>
              <input
                value={profileForm.phone}
                onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
          </div>
          <button
            type="submit"
            className="bg-[#111111] text-white px-5 py-2.5 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800"
          >
            Save Profile
          </button>
        </form>

        {/* Change Password */}
        <form onSubmit={handleChangePassword} className="border border-[#E5E5E5] p-6 bg-white space-y-4">
          <h2 className="text-xs font-bold uppercase tracking-widest flex items-center gap-2">
            <KeyRound className="w-4 h-4" /> Account Security
          </h2>
          <div className="space-y-3 text-xs">
            <div>
              <label className="block font-medium mb-1">Current Password</label>
              <input
                type="password"
                required
                value={pwForm.currentPassword}
                onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
            <div>
              <label className="block font-medium mb-1">New Password</label>
              <input
                type="password"
                required
                minLength={6}
                value={pwForm.newPassword}
                onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
                className="w-full border border-[#D4D4D4] px-3 py-2.5 outline-none focus:border-[#111111]"
              />
            </div>
          </div>
          <button
            type="submit"
            className="border border-[#111111] px-5 py-2.5 text-xs font-bold uppercase tracking-widest hover:bg-[#111111] hover:text-white"
          >
            Update Password
          </button>
        </form>
      </div>

      {/* Saved Addresses */}
      <div className="border border-[#E5E5E5] p-6 bg-white">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-xs font-bold uppercase tracking-widest flex items-center gap-2">
            <MapPin className="w-4 h-4" /> Saved Addresses
          </h2>
          <button
            type="button"
            onClick={() => setShowAddAddress((v) => !v)}
            className="border border-[#111111] px-3.5 py-2 text-xs font-bold uppercase tracking-wider flex items-center gap-1 hover:bg-[#111111] hover:text-white"
          >
            <Plus className="w-3.5 h-3.5" /> Add Address
          </button>
        </div>

        {showAddAddress && (
          <form onSubmit={handleAddAddress} className="mb-6 border border-[#E5E5E5] bg-[#FAFAFA] p-5 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <input required placeholder="Full Name *" value={addrForm.fullName} onChange={(e) => setAddrForm({ ...addrForm, fullName: e.target.value })} className="border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input required placeholder="Phone *" value={addrForm.phone} onChange={(e) => setAddrForm({ ...addrForm, phone: e.target.value })} className="border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input required placeholder="Address Line 1 *" value={addrForm.addressLine1} onChange={(e) => setAddrForm({ ...addrForm, addressLine1: e.target.value })} className="sm:col-span-2 border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input placeholder="Address Line 2" value={addrForm.addressLine2} onChange={(e) => setAddrForm({ ...addrForm, addressLine2: e.target.value })} className="sm:col-span-2 border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input required placeholder="City *" value={addrForm.city} onChange={(e) => setAddrForm({ ...addrForm, city: e.target.value })} className="border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input required placeholder="State *" value={addrForm.state} onChange={(e) => setAddrForm({ ...addrForm, state: e.target.value })} className="border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <input required placeholder="PIN Code *" value={addrForm.postalCode} onChange={(e) => setAddrForm({ ...addrForm, postalCode: e.target.value })} className="border border-[#D4D4D4] bg-white px-3 py-2 outline-none" />
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={addrForm.isDefault} onChange={(e) => setAddrForm({ ...addrForm, isDefault: e.target.checked })} className="accent-[#111111]" />
              <span>Set as default address</span>
            </label>
            <div className="sm:col-span-2 flex justify-end gap-2">
              <button type="button" onClick={() => setShowAddAddress(false)} className="px-4 py-2 border border-[#D4D4D4] bg-white">Cancel</button>
              <button type="submit" className="bg-[#111111] text-white px-5 py-2 font-bold uppercase tracking-wider">Save Address</button>
            </div>
          </form>
        )}

        {addresses.length === 0 ? (
          <p className="text-xs text-neutral-500">No saved addresses yet.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            {addresses.map((addr) => (
              <div key={addr.id} className="border border-[#E5E5E5] p-4 flex justify-between items-start gap-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold">{addr.fullName}</span>
                    <span className="text-[10px] uppercase tracking-wider border border-[#D4D4D4] px-1.5 py-0.5">
                      {addr.addressType}
                    </span>
                    {addr.isDefault && (
                      <span className="text-[10px] uppercase tracking-wider bg-[#111111] text-white px-1.5 py-0.5">
                        Default
                      </span>
                    )}
                  </div>
                  <p className="text-neutral-600">{addr.addressLine1}</p>
                  {addr.addressLine2 && <p className="text-neutral-600">{addr.addressLine2}</p>}
                  <p className="text-neutral-600">
                    {addr.city}, {addr.state} - {addr.postalCode}
                  </p>
                  <p className="font-medium">Phone: {addr.phone}</p>
                </div>
                <button
                  type="button"
                  onClick={() => handleDeleteAddress(addr.id)}
                  className="text-neutral-400 hover:text-red-700"
                  aria-label="Delete address"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
