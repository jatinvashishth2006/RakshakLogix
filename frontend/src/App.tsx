import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import {
  Activity,
  AlertTriangle,
  Boxes,
  CheckCircle,
  ChevronRight,
  ClipboardList,
  LogOut,
  PackageCheck,
  Radio,
  RefreshCw,
  Shield,
  Wrench,
} from 'lucide-react';
import { api } from './api';

type User = { name: string; email: string; role: string };
type Item = {
  id: number;
  itemId: string;
  name: string;
  category: string;
  availableQuantity: number;
  minimumStockThreshold: number;
  unitOfMeasure: string;
  storageLocation?: string;
  status: string;
};
type Request = {
  id: number;
  requestId: string;
  requestingUnit: string;
  itemName: string;
  requestedQuantity: number;
  priority: string;
  status: string;
};

const nav = [
  { id: 'overview', label: 'Overview', icon: Activity },
  { id: 'inventory', label: 'Inventory', icon: Boxes },
  { id: 'requests', label: 'Supply requests', icon: ClipboardList },
];

const emptyItem = {
  itemId: '',
  name: '',
  category: 'General Equipment',
  availableQuantity: 0,
  minimumStockThreshold: 0,
  unitOfMeasure: 'units',
  description: '',
  storageLocation: '',
  supplier: '',
};

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('rakshak-token'));
  const [user, setUser] = useState<User | null>(null);
  const [page, setPage] = useState('overview');
  const [summary, setSummary] = useState<Record<string, number> | null>(null);
  const [items, setItems] = useState<Item[]>([]);
  const [requests, setRequests] = useState<Request[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');
  const [showItem, setShowItem] = useState(false);
  const [form, setForm] = useState(emptyItem);

  const refresh = async () => {
    if (!token) return;
    setBusy(true);
    setError('');
    try {
      const [me, dashboard, inventory, supplyRequests] = await Promise.all([
        api.get('/auth/me'),
        api.get('/dashboard/summary'),
        api.get('/inventory'),
        api.get('/requests'),
      ]);
      setUser(me.data);
      setSummary(dashboard.data);
      setItems(inventory.data);
      setRequests(supplyRequests.data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Could not reach the API. Start the backend and check MySQL is available.');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void refresh();
  }, [token]);

  const login = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    const values = new FormData(e.currentTarget);
    try {
      const { data } = await api.post('/auth/login', {
        email: values.get('email'),
        password: values.get('password'),
      });
      localStorage.setItem('rakshak-token', data.token);
      setToken(data.token);
      setUser(data.user);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Check credentials and backend status.');
    }
  };

  const logout = () => {
    localStorage.removeItem('rakshak-token');
    setToken(null);
    setUser(null);
  };

  const saveItem = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/inventory', form);
      setShowItem(false);
      setForm(emptyItem);
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Unable to save inventory item.');
    }
  };

  const stockAction = async (item: Item, type: 'receive' | 'issue') => {
    const input = window.prompt(`${type === 'receive' ? 'Receive' : 'Issue'} quantity for ${item.name}`);
    if (!input) return;
    const quantity = Number(input);
    if (!Number.isInteger(quantity) || quantity < 1) {
      setError('Enter a positive whole-number quantity.');
      return;
    }
    try {
      await api.post(`/inventory/${item.id}/${type}`, { quantity });
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Stock update failed.');
    }
  };

  const processRequest = async (request: Request, action: 'approve' | 'reject') => {
    try {
      await api.post(`/requests/${request.id}/${action}`, {});
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Request update failed.');
    }
  };

  const filtered = useMemo(
    () => items.filter((item) => `${item.name} ${item.itemId} ${item.category}`.toLowerCase().includes(search.toLowerCase())),
    [items, search],
  );

  if (!token) {
    return (
      <main className="login">
        <div className="login-card">
          <div className="brand">
            <div className="crest"><Shield size={25} /></div>
            <div><strong>RAKSHAKLOGIX</strong><small>INTELLIGENCE. READINESS. RESOURCE MANAGEMENT.</small></div>
          </div>
          <p className="eyebrow">LOGISTICS COMMAND PLATFORM</p>
          <h1>Welcome back</h1>
          <p className="muted">Sign in to your synthetic logistics workspace.</p>
          <form onSubmit={login}>
            <label>Email<input name="email" type="email" required defaultValue="admin@rakshaklogix.local" placeholder="name@example.com" /></label>
            <label>Password<input name="password" type="password" required defaultValue="Admin@123" /></label>
            <button className="primary wide">Secure sign in <ChevronRight size={17} /></button>
          </form>
          {error && <div className="error"><AlertTriangle size={17} />{error}</div>}
          <div className="login-hint">Demo: admin@rakshaklogix.local / Admin@123</div>
        </div>
        <div className="login-art">
          <div className="terrain" />
          <span className="live-dot">DEMO ENVIRONMENT • SYNTHETIC DATA</span>
          <h2>Readiness through<br /><em>resource intelligence.</em></h2>
          <p>A unified view of supply, stock and service readiness.</p>
        </div>
      </main>
    );
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand smallbrand"><div className="crest"><Shield size={21} /></div><strong>RAKSHAK<br />LOGIX</strong></div>
        <div className="nav-label">COMMAND</div>
        {nav.map((entry) => (
          <button key={entry.id} className={`nav-item ${page === entry.id ? 'active' : ''}`} onClick={() => setPage(entry.id)}>
            <entry.icon size={18} />{entry.label}
          </button>
        ))}
        <div className="side-bottom">
          <div className="synthetic"><Radio size={16} /> Synthetic demo data</div>
          <button className="nav-item" onClick={logout}><LogOut size={18} />Sign out</button>
        </div>
      </aside>

      <main className="main">
        <header>
          <div><div className="eyebrow">OPERATIONS / {page.toUpperCase()}</div><h1>{page === 'overview' ? 'Readiness overview' : page === 'inventory' ? 'Inventory management' : 'Supply requests'}</h1></div>
          <div className="header-actions">
            <span className="online"><i /> API connected</span>
            <button className="icon-btn" onClick={() => void refresh()} title="Refresh"><RefreshCw size={17} /></button>
            <div className="avatar">{user?.name?.slice(0, 1) || 'U'}</div>
            <div className="identity"><b>{user?.name || 'User'}</b><small>{user?.role?.replace('_', ' ')}</small></div>
          </div>
        </header>
        {error && <div className="error banner"><AlertTriangle size={18} />{error}<button onClick={() => setError('')}>Dismiss</button></div>}
        {busy && <div className="loading"><span />Syncing live data from backend…</div>}

        {page === 'overview' && <>
          <section className="welcome">
            <div><div className="eyebrow light">COMMAND DASHBOARD · DEMO INSTANCE</div><h2>Logistics readiness<br />at a glance.</h2><p>Live inventory and request data from your local RakshakLogix API.</p></div>
            <div className="welcome-mark"><PackageCheck size={68} /><span>READINESS<br />INTELLIGENCE</span></div>
          </section>
          <section className="kpis">
            {([
              ['Inventory items', summary?.inventoryItems ?? '—', Boxes],
              ['Available stock', summary?.availableStock ?? '—', PackageCheck],
              ['Low stock items', summary?.lowStockItems ?? '—', AlertTriangle],
              ['Pending requests', summary?.pendingRequests ?? '—', ClipboardList],
              ['Under maintenance', summary?.equipmentUnderMaintenance ?? '—', Wrench],
              ['Upcoming service', summary?.upcomingMaintenance ?? '—', CheckCircle],
            ] as const).map(([label, value, Icon]) => (
              <article className="kpi" key={label}><div className="kpi-icon"><Icon size={19} /></div><small>{label}</small><strong>{value}</strong><span>Current database totals</span></article>
            ))}
          </section>
          <section className="content-grid">
            <article className="panel">
              <div className="panel-head"><div><div className="eyebrow">SUPPLY POSITION</div><h3>Inventory snapshot</h3></div><button className="text-button" onClick={() => setPage('inventory')}>Open inventory <ChevronRight size={15} /></button></div>
              <div className="table-wrap"><table><thead><tr><th>Item</th><th>Category</th><th>Available</th><th>Position</th></tr></thead><tbody>
                {items.slice(0, 5).map((item) => <tr key={item.id}><td><b>{item.name}</b><small>{item.itemId}</small></td><td>{item.category}</td><td>{item.availableQuantity} {item.unitOfMeasure}</td><td><span className={`badge ${item.status === 'LOW_STOCK' ? 'warn' : 'good'}`}>{item.status === 'LOW_STOCK' ? 'Low stock' : 'Available'}</span></td></tr>)}
              </tbody></table>{items.length === 0 && <div className="empty">No inventory records yet. Add one from Inventory.</div>}</div>
            </article>
            <article className="panel">
              <div className="panel-head"><div><div className="eyebrow">ACTION QUEUE</div><h3>Recent requests</h3></div><ClipboardList size={18} /></div>
              {requests.slice(0, 4).map((request) => <div className="request-row" key={request.id}><div className="request-icon"><ClipboardList size={17} /></div><div><b>{request.itemName}</b><small>{request.requestId} · {request.requestingUnit}</small></div><span className={`badge ${request.status === 'Pending' ? 'warn' : 'good'}`}>{request.status}</span></div>)}
              {requests.length === 0 && <div className="empty">No supply requests recorded.</div>}
            </article>
          </section>
        </>}

        {page === 'inventory' && <section className="panel full-panel">
          <div className="panel-head"><div><div className="eyebrow">STOCK CONTROL</div><h3>Inventory records</h3></div><button className="primary" onClick={() => setShowItem(true)}>Add inventory item <ChevronRight size={16} /></button></div>
          <div className="table-tools"><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search item, ID or category…" /><span>{filtered.length} records</span></div>
          <div className="table-wrap"><table><thead><tr><th>Item ID</th><th>Item name</th><th>Category</th><th>Stock</th><th>Minimum</th><th>Status</th><th>Actions</th></tr></thead><tbody>
            {filtered.map((item) => <tr key={item.id}><td className="mono">{item.itemId}</td><td><b>{item.name}</b><small>{item.storageLocation || 'Location not set'}</small></td><td>{item.category}</td><td>{item.availableQuantity} {item.unitOfMeasure}</td><td>{item.minimumStockThreshold}</td><td><span className={`badge ${item.status === 'LOW_STOCK' ? 'warn' : 'good'}`}>{item.status}</span></td><td className="actions"><button onClick={() => void stockAction(item, 'receive')}>Receive</button><button onClick={() => void stockAction(item, 'issue')}>Issue</button></td></tr>)}
          </tbody></table>{filtered.length === 0 && <div className="empty">No matching inventory records.</div>}</div>
        </section>}

        {page === 'requests' && <section className="panel full-panel">
          <div className="panel-head"><div><div className="eyebrow">REVIEW WORKFLOW</div><h3>Supply requests</h3></div><span className="badge warn">{requests.filter((request) => request.status === 'Pending').length} pending</span></div>
          <div className="table-wrap"><table><thead><tr><th>Request</th><th>Unit</th><th>Requested item</th><th>Quantity</th><th>Priority</th><th>Status</th><th>Review</th></tr></thead><tbody>
            {requests.map((request) => <tr key={request.id}><td className="mono">{request.requestId}</td><td>{request.requestingUnit}</td><td>{request.itemName}</td><td>{request.requestedQuantity}</td><td>{request.priority}</td><td><span className={`badge ${request.status === 'Pending' ? 'warn' : 'good'}`}>{request.status}</span></td><td>{request.status === 'Pending' && <div className="actions"><button onClick={() => void processRequest(request, 'approve')}>Approve</button><button onClick={() => void processRequest(request, 'reject')}>Reject</button></div>}</td></tr>)}
          </tbody></table>{requests.length === 0 && <div className="empty">No requests yet.</div>}</div>
        </section>}

        <footer>RAKSHAKLOGIX <span>Prototype · Synthetic non-operational data only</span><span>Human review required for all recommendations</span></footer>
      </main>

      {showItem && <div className="modal-backdrop" onClick={() => setShowItem(false)}>
        <div className="modal" onClick={(e) => e.stopPropagation()}>
          <div className="panel-head"><div><div className="eyebrow">STOCK CONTROL</div><h3>Add inventory item</h3></div><button className="icon-btn" onClick={() => setShowItem(false)}>×</button></div>
          <form className="item-form" onSubmit={saveItem}>
            {([
              ['itemId', 'Item identifier'], ['name', 'Item name'], ['category', 'Category'],
              ['availableQuantity', 'Available quantity'], ['minimumStockThreshold', 'Minimum threshold'],
              ['unitOfMeasure', 'Unit of measure'], ['storageLocation', 'Storage location'], ['supplier', 'Supplier'],
            ] as const).map(([key, label]) => <label key={key}>{label}<input required={['itemId', 'name', 'category', 'unitOfMeasure'].includes(key)} type={key === 'availableQuantity' || key === 'minimumStockThreshold' ? 'number' : 'text'} min={key === 'availableQuantity' || key === 'minimumStockThreshold' ? 0 : undefined} value={form[key]} onChange={(e) => setForm({ ...form, [key]: key === 'availableQuantity' || key === 'minimumStockThreshold' ? Number(e.target.value) : e.target.value })} /></label>)}
            <label className="span2">Description<textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
            <div className="span2 form-actions"><button type="button" className="secondary" onClick={() => setShowItem(false)}>Cancel</button><button className="primary">Save item</button></div>
          </form>
        </div>
      </div>}
    </div>
  );
}
