import React, { useState, useEffect } from 'react';
import { UserPlus } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import { formatDateTime } from '../utils/format';

export default function AdminStaffPage() {
  const { showToast } = useToast();
  const [staff, setStaff] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '', role: 'STAFF' });

  const load = async () => {
    const [s, logs] = await Promise.all([adminApi.getStaff(), adminApi.getAuditLogs(0, 20)]);
    setStaff(s || []);
    setAuditLogs(logs?.content || []);
  };

  useEffect(() => {
    load();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await adminApi.createStaff(form);
      showToast('Staff account created');
      setForm({ fullName: '', email: '', password: '', phone: '', role: 'STAFF' });
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  const handleToggleActive = async (member) => {
    try {
      await adminApi.updateStaff(member.id, { active: !member.active });
      showToast('Staff account updated');
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Staff Management &amp; Audit Trail</h1>
        <p className="text-xs text-neutral-500 mt-0.5">Owner-only workspace to create store admins/staff and review activity logs.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <form onSubmit={handleCreate} className="lg:col-span-4 bg-white border border-[#E5E5E5] p-5 space-y-3.5 text-xs h-fit">
          <h2 className="font-bold uppercase tracking-widest border-b border-[#F0F0F0] pb-2 flex items-center gap-2">
            <UserPlus className="w-4 h-4" /> Add Store Staff
          </h2>
          <input required placeholder="Full Name *" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          <input required type="email" placeholder="Staff Email *" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          <input required type="password" minLength={6} placeholder="Initial Password *" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          <input placeholder="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="w-full border border-[#D4D4D4] px-3 py-2 outline-none" />
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} className="w-full border border-[#D4D4D4] bg-white px-3 py-2 outline-none">
            <option value="STAFF">STAFF (Operations &amp; Inventory)</option>
            <option value="ADMIN">ADMIN (Full Store Operations)</option>
          </select>
          <button type="submit" className="w-full bg-[#111111] text-white py-2.5 font-bold uppercase tracking-wider">
            Create Staff Account
          </button>
        </form>

        <div className="lg:col-span-8 bg-white border border-[#E5E5E5] overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
                <th className="py-3 px-4">Staff Member</th>
                <th className="py-3 px-3">Role</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {staff.map((m) => (
                <tr key={m.id} className="border-b border-[#F0F0F0]">
                  <td className="py-3 px-4">
                    <div className="font-semibold">{m.fullName}</div>
                    <div className="text-[11px] text-neutral-500">{m.email}</div>
                  </td>
                  <td className="py-3 px-3 font-bold">{m.roles?.join(', ')}</td>
                  <td className="py-3 px-3">
                    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${m.active ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-red-300 bg-red-50 text-red-800'}`}>
                      {m.active ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    {!m.roles?.includes('OWNER') && (
                      <button
                        onClick={() => handleToggleActive(m)}
                        className="border border-[#D4D4D4] px-3 py-1 text-[11px] font-semibold hover:border-[#111111]"
                      >
                        {m.active ? 'Deactivate' : 'Activate'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Audit Logs */}
      <div className="bg-white border border-[#E5E5E5] p-6">
        <h2 className="text-xs font-bold uppercase tracking-widest mb-4">Recent Activity Audit Logs</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] text-[11px] uppercase text-neutral-500">
                <th className="py-2 pr-3">Actor</th>
                <th className="py-2 pr-3">Action</th>
                <th className="py-2 pr-3">Entity</th>
                <th className="py-2 pr-3">Details</th>
                <th className="py-2">Time</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.map((log) => (
                <tr key={log.id} className="border-b border-[#F5F5F5]">
                  <td className="py-2 pr-3 font-medium">{log.userEmail || 'System'}</td>
                  <td className="py-2 pr-3 font-bold">{log.action}</td>
                  <td className="py-2 pr-3">{log.entityType} #{log.entityId}</td>
                  <td className="py-2 pr-3 text-neutral-600">{log.details}</td>
                  <td className="py-2 text-neutral-500">{formatDateTime(log.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
