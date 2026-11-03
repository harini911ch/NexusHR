import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import {
  Activity, Bell, CalendarDays, ChartNoAxesCombined,
  ChevronRight, CircleDollarSign, ClipboardList, LayoutDashboard, LogOut,
  Menu, ShieldCheck, Sparkles, Users, X, LoaderCircle,
} from 'lucide-react'
import { api } from './lib/api'
import './App.css'

const ActionWorkspace = lazy(() => import('./ActionWorkspace'))
const EmployeeDocuments = lazy(() => import('./EmployeeDocuments'))
const EmployeeInvitations = lazy(() => import('./EmployeeInvitations'))
const EmployeeLifecycleApprovals = lazy(() => import('./EmployeeLifecycleApprovals'))
const PayrollRunExports = lazy(() => import('./PayrollRunExports'))
const WorkforceCharts = lazy(() => import('./WorkforceCharts'))

type User = { id?: string; userId?: string; fullName: string; email: string; role: string }
type ModuleKey = 'overview' | 'employees' | 'attendance' | 'payroll' | 'performance' | 'insights' | 'notifications'
type DataResult = { label: string; value: unknown; error?: string; status?: number }
type UnknownRecord = Record<string, unknown>

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

const navigation: { key: ModuleKey; label: string; icon: typeof LayoutDashboard }[] = [
  { key: 'overview', label: 'Overview', icon: LayoutDashboard },
  { key: 'employees', label: 'Employees', icon: Users },
  { key: 'attendance', label: 'Attendance & Leave', icon: CalendarDays },
  { key: 'payroll', label: 'Payroll', icon: CircleDollarSign },
  { key: 'performance', label: 'Performance', icon: ChartNoAxesCombined },
  { key: 'insights', label: 'AI Insights', icon: Sparkles },
  { key: 'notifications', label: 'Notifications', icon: Bell },
]

const titles: Record<ModuleKey, string> = {
  overview: 'Overview',
  employees: 'Employee lifecycle',
  attendance: 'Attendance & leave',
  payroll: 'Payroll management',
  performance: 'Performance management',
  insights: 'Workforce intelligence',
  notifications: 'Notifications',
}

function errorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as {
      response?: { status?: number; data?: unknown }
    }).response
    const data = isRecord(response?.data) ? response.data : {}
    for (const key of ['detail', 'message', 'error', 'title']) {
      if (typeof data[key] === 'string' && data[key]) return data[key]
    }
    return response?.status
      ? `Request failed (HTTP ${response.status}). Please try again.`
      : 'Request failed. Please try again.'
  }
  return error instanceof Error ? error.message : 'Unable to connect to the server.'
}

function Login({ onLogin }: { onLogin: (user: User) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [resetRequested, setResetRequested] = useState(false)
  const [requestingReset, setRequestingReset] = useState(false)

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (requestingReset) {
        await api.post('/auth/password-reset/request', { email })
        setResetRequested(true)
        return
      }
      const response = await api.post('/auth/login', { email, password })
      const data = response.data
      onLogin(
        { userId: data.userId, fullName: data.fullName, email: data.email, role: data.role },
      )
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-visual">
        <div className="brand brand-light"><span className="brand-mark"><Activity size={22} /></span><span>Nexus<span>HR</span></span></div>
        <div className="visual-copy">
          <span className="eyebrow">PEOPLE. PERFORMANCE. POSSIBILITY.</span>
          <h1>People are your<br /><em>greatest</em> advantage.</h1>
          <p>A connected workspace for smarter workforce management and better employee experiences.</p>
          <div className="visual-stats">
            <div><strong>01</strong><span>One connected<br />workforce</span></div>
            <div><strong>8</strong><span>Room to grow<br />together</span></div>
          </div>
        </div>
        <div className="visual-footer">NexusHR · Enterprise workforce intelligence</div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <div className="mobile-brand brand"><span className="brand-mark"><Activity size={20} /></span><span>Nexus<span>HR</span></span></div>
          <span className="eyebrow">{requestingReset ? 'ACCOUNT RECOVERY' : 'WELCOME BACK'}</span>
          <h2>{requestingReset ? 'Reset your password' : 'Sign in to your workspace'}</h2>
          <p className="muted">{requestingReset ? 'Enter your work email and we will send reset instructions if the account exists.' : 'Enter your work credentials to continue.'}</p>
          <form onSubmit={submit} className="login-form">
            <label htmlFor="email">Work email</label>
            <input id="email" type="email" autoComplete="username" placeholder="you@company.com" value={email} onChange={e => setEmail(e.target.value)} required />
            {!requestingReset && <>
              <div className="password-label"><label htmlFor="password">Password</label><button type="button" className="text-button" onClick={() => { setError(''); setResetRequested(false); setRequestingReset(true) }}>Forgot password?</button></div>
              <input id="password" type="password" autoComplete="current-password" placeholder="Enter your password" value={password} onChange={e => setPassword(e.target.value)} required />
            </>}
            {error && <div className="error-box">{error}</div>}
            {resetRequested && <div className="success-box" role="status">If the account exists, password reset instructions will be sent.</div>}
            <button className="primary-button login-button" disabled={busy || resetRequested}>
              {busy ? <><LoaderCircle className="spin" size={18} /> {requestingReset ? 'Sending...' : 'Signing in...'}</> : <>{requestingReset ? 'Send reset instructions' : 'Sign in'} <ChevronRight size={18} /></>}
            </button>
          </form>
          {requestingReset && <button type="button" className="text-button auth-back-button" onClick={() => { setRequestingReset(false); setResetRequested(false); setError('') }}>Back to sign in</button>}
          {!requestingReset && <a className="text-button auth-register-link" href="/register">Have an invitation? Register</a>}
          <div className="secure-note"><ShieldCheck size={17} /> Secure access protected by NexusHR authentication</div>
        </div>
        <div className="login-bottom">(c) 2026 NexusHR. Workforce intelligence platform.</div>
      </section>
    </main>
  )
}

function Registration({ onRegister, onBack }: {
  onRegister: (user: User) => void
  onBack: () => void
}) {
  const [invitationToken, setInvitationToken] = useState(
    () => new URLSearchParams(window.location.hash.slice(1)).get('token') || '',
  )
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState(
    () => new URLSearchParams(window.location.search).get('email') || '',
  )
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (password !== confirmation) {
      setError('The passwords do not match.')
      return
    }
    setBusy(true)
    try {
      const response = await api.post('/auth/register', {
        fullName,
        email,
        password,
        invitationToken,
      })
      window.history.replaceState(null, '', '/')
      onRegister({
        userId: response.data.userId,
        fullName: response.data.fullName,
        email: response.data.email,
        role: response.data.role,
      })
    } catch (registrationError) {
      setError(errorMessage(registrationError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel reset-panel">
        <div className="login-form-wrap">
          <div className="mobile-brand brand"><span className="brand-mark"><Activity size={20} /></span><span>Nexus<span>HR</span></span></div>
          <span className="eyebrow">EMPLOYEE REGISTRATION</span>
          <h2>Create your account</h2>
          <p className="muted">Register using the work email and secure invitation link provided by your HR team.</p>
          <form onSubmit={event => void submit(event)} className="login-form">
            <label htmlFor="register-name">Full name</label>
            <input id="register-name" autoComplete="name" maxLength={120} value={fullName} onChange={event => setFullName(event.target.value)} required />
            <label htmlFor="register-email">Work email</label>
            <input id="register-email" type="email" autoComplete="email" maxLength={180} value={email} onChange={event => setEmail(event.target.value)} required />
            <label htmlFor="register-password">Password</label>
            <input id="register-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} required />
            <label htmlFor="register-confirmation">Confirm password</label>
            <input id="register-confirmation" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={confirmation} onChange={event => setConfirmation(event.target.value)} required />
            {!invitationToken && <>
              <label htmlFor="register-token">Invitation token</label>
              <textarea id="register-token" rows={3} value={invitationToken} onChange={event => setInvitationToken(event.target.value.trim())} required />
            </>}
            {error && <div className="error-box" role="alert">{error}</div>}
            <button className="primary-button login-button" disabled={busy}>
              {busy ? <><LoaderCircle className="spin" size={18} /> Creating account...</> : <>Create account <ChevronRight size={18} /></>}
            </button>
          </form>
          <button type="button" className="text-button auth-back-button" onClick={onBack}>Back to sign in</button>
          <div className="secure-note"><ShieldCheck size={17} /> Registration requires an authorized invitation</div>
        </div>
      </section>
    </main>
  )
}

function PasswordReset({ onBack }: { onBack: () => void }) {
  const [token] = useState(
    () => new URLSearchParams(window.location.hash.slice(1)).get('token') || '',
  )
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)

  useEffect(() => {
    if (window.location.hash) {
      window.history.replaceState(null, '', window.location.pathname)
    }
  }, [])

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (password !== confirmation) {
      setError('The passwords do not match.')
      return
    }
    setBusy(true)
    try {
      await api.post('/auth/password-reset/complete', { token, newPassword: password })
      setComplete(true)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel reset-panel">
        <div className="login-form-wrap">
          <div className="mobile-brand brand"><span className="brand-mark"><Activity size={20} /></span><span>Nexus<span>HR</span></span></div>
          <span className="eyebrow">SECURE ACCOUNT RECOVERY</span>
          <h2>{complete ? 'Password updated' : 'Choose a new password'}</h2>
          {complete ? (
            <>
              <p className="muted">Your password has been changed. Sign in with your new password.</p>
              <button className="primary-button login-button" onClick={onBack}>Back to sign in <ChevronRight size={18} /></button>
            </>
          ) : !token ? (
            <>
              <p className="muted">This reset link is invalid or incomplete. Request a new one from the sign-in page.</p>
              <button className="primary-button login-button" onClick={onBack}>Back to sign in <ChevronRight size={18} /></button>
            </>
          ) : (
            <>
              <p className="muted">Use a password with at least 8 characters. Reset links are time-limited.</p>
              <form onSubmit={submit} className="login-form">
                <label htmlFor="new-password">New password</label>
                <input id="new-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} required />
                <label htmlFor="confirm-password">Confirm new password</label>
                <input id="confirm-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={confirmation} onChange={event => setConfirmation(event.target.value)} required />
                {error && <div className="error-box" role="alert">{error}</div>}
                <button className="primary-button login-button" disabled={busy}>
                  {busy ? <><LoaderCircle className="spin" size={18} /> Updating...</> : <>Update password <ChevronRight size={18} /></>}
                </button>
              </form>
            </>
          )}
          <div className="secure-note"><ShieldCheck size={17} /> Reset links are single-use and time-limited</div>
        </div>
      </section>
    </main>
  )
}

const hiddenFields = /password|token|secret|credential|hash|authorization/i

function formatLabel(key: string) {
  return key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/[_-]/g, ' ').replace(/\b\w/g, char => char.toUpperCase())
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'object') return Array.isArray(value) ? `${value.length} records` : 'Details available'
  return String(value)
}

function metricValue(results: DataResult[], label: string) {
  const result = results.find(item => item.label === label)
  if (!result || result.error || result.value == null) return '—'
  if (Array.isArray(result.value)) return String(result.value.length)
  if (isRecord(result.value)) {
    if (label === 'Employee directory' && 'totalElements' in result.value) {
      return String(result.value.totalElements)
    }
    if (label === 'Workforce analytics' && 'totalEmployees' in result.value) {
      return String(result.value.totalEmployees)
    }
    return 'Available'
  }
  return '—'
}

function DataValue({ value }: { value: unknown }) {
  if (value === null || value === undefined) return <div className="friendly-empty">No data available</div>

  if (Array.isArray(value)) {
    if (value.length === 0) return <div className="friendly-empty">No records found</div>

    if (value.every(item => item === null || typeof item !== 'object')) {
      return <div className="value-list">{value.map((item, index) => <div className="value-list-item" key={index}>{formatValue(item)}</div>)}</div>
    }

    const records = value.filter((item): item is Record<string, unknown> => item !== null && typeof item === 'object' && !Array.isArray(item))
    const columns = [...new Set(records.flatMap(item => Object.keys(item)))].filter(key => !hiddenFields.test(key))

    if (columns.length === 0) return <div className="friendly-empty">No displayable records found</div>

    return (
      <div className="records-table-wrap">
        <table className="records-table">
          <thead><tr>{columns.map(column => <th key={column}>{formatLabel(column)}</th>)}</tr></thead>
          <tbody>
            {records.map((record, index) => (
              <tr key={String(record.id ?? record.employeeId ?? index)}>
                {columns.map(column => <td key={column}>{formatValue(record[column])}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
        <div className="record-count">{records.length} records</div>
      </div>
    )
  }

  if (typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>).filter(([key]) => !hiddenFields.test(key))
    if (entries.length === 0) return <div className="friendly-empty">No displayable information</div>
    return <div className="profile-details">{entries.map(([key, item]) => <div className="profile-detail" key={key}><span>{formatLabel(key)}</span><strong>{formatValue(item)}</strong></div>)}</div>
  }

  return <div className="single-value">{formatValue(value)}</div>
}

function DataPanel({
  result,
  onCompleteProfile,
}: {
  result: DataResult
  onCompleteProfile?: () => void
}) {
  const profileMissing = result.label === 'My employee profile' && result.status === 404
  return (
    <section className="data-panel">
      <div className="panel-heading">
        <h3>{result.label}</h3>
        <span className={result.error ? 'status status-error' : 'status'}>
          {profileMissing ? 'Setup needed' : result.error ? 'Unavailable' : 'Connected'}
        </span>
      </div>
      {result.error ? (
        <>
          <p className="panel-error">
            {profileMissing
              ? 'Your account does not have an employee profile yet.'
              : result.error}
          </p>
          {profileMissing && onCompleteProfile && (
            <button type="button" className="refresh-button" onClick={onCompleteProfile}>
              Complete my employee profile
            </button>
          )}
        </>
      ) : <DataValue value={result.value} />}
    </section>
  )
}
function App() {
  const [user, setUser] = useState<User | null>(null)
  const [pathname, setPathname] = useState(window.location.pathname)
  const [restoringSession, setRestoringSession] = useState(
    !['/reset-password', '/register'].includes(window.location.pathname),
  )
  const [active, setActive] = useState<ModuleKey>('overview')
  const [results, setResults] = useState<DataResult[]>([])
  const [loading, setLoading] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [notice, setNotice] = useState('')

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
      setUser(null)
      setNotice('')
    } catch (error) {
      setNotice(`Sign-out failed: ${errorMessage(error)}`)
    }
  }, [])

  const login = (profile: User) => {
    setUser(profile)
    setActive('overview')
  }

  const returnToLogin = () => {
    window.history.pushState(null, '', '/')
    setPathname('/')
  }

  useEffect(() => {
    const handlePopState = () => setPathname(window.location.pathname)
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  useEffect(() => {
    if (pathname === '/reset-password' || pathname === '/register') return
    let active = true
    api.get('/auth/me').then(response => {
      const profile = response.data as User
      if (active) setUser(profile)
    }).catch(() => {
      if (active) setUser(null)
    }).finally(() => {
      if (active) setRestoringSession(false)
    })
    return () => { active = false }
  }, [pathname])

  useEffect(() => {
    if (!user) return

    const apiBase = new URL(
      import.meta.env.VITE_API_BASE_URL || 'http://localhost:8091/api',
      window.location.origin,
    )
    const socketUrl = new URL('/ws/notifications', apiBase.origin)
    socketUrl.protocol = socketUrl.protocol === 'https:' ? 'wss:' : 'ws:'
    const socket = new WebSocket(socketUrl)

    socket.onmessage = event => {
      try {
        const notification = JSON.parse(event.data) as Record<string, unknown>
        if (typeof notification.id !== 'string' || typeof notification.title !== 'string') {
          setNotice('A live notification update was incomplete. Refresh to reload notifications.')
          return
        }
        setResults(current => current.map(result => {
          if (result.label === 'My notifications' && Array.isArray(result.value)) {
            const exists = result.value.some(
              item => typeof item === 'object' && item !== null && 'id' in item && item.id === notification.id,
            )
            return exists ? result : { ...result, value: [notification, ...result.value] }
          }
          if (result.label === 'Unread notification count' &&
              typeof result.value === 'object' && result.value !== null &&
              'unreadCount' in result.value && typeof result.value.unreadCount === 'number') {
            return {
              ...result,
              value: { ...result.value, unreadCount: result.value.unreadCount + 1 },
            }
          }
          return result
        }))
        setNotice(`New notification: ${notification.title}`)
      } catch {
        setNotice('A live notification update could not be decoded. Refresh to reload notifications.')
      }
    }

    socket.onerror = () => {
      setNotice('Real-time notifications are unavailable. Use Refresh data to reload.')
    }

    return () => socket.close()
  }, [user])

  const loadModule = useCallback(async (module: ModuleKey) => {
    if (!user) return
    setLoading(true)
    setResults([])
    setNotice('')
    const requests: { label: string; url: string }[] = []

    const isHr = ['SUPER_ADMIN', 'HR_ADMIN', 'HR_MANAGER'].includes(user.role)
    const canViewDirectory = isHr || ['MANAGER', 'DEPARTMENT_MANAGER'].includes(user.role)

    if (module === 'overview' || module === 'employees') {
      if (canViewDirectory) {
        requests.push({ label: 'Employee directory', url: '/employees' })
        if (module === 'overview') {
          requests.push(
            { label: 'My employee profile', url: '/employees/me' },
            { label: 'Workforce analytics', url: '/employees/analytics' },
          )
        }
      } else {
        requests.push({ label: 'My employee profile', url: '/employees/me' })
      }
    }
    if (module === 'overview') {
      requests.push(
        { label: 'My attendance records', url: '/attendance/me' },
        { label: 'My leave requests', url: '/leaves/me' },
        { label: 'My payslips', url: '/payroll/me' },
        { label: 'My goals', url: '/performance/goals/me' },
      )
    }
    if (module === 'attendance') {
      requests.push(
        { label: 'My attendance records', url: '/attendance/me' },
        { label: 'My leave requests', url: '/leaves/me' },
        { label: 'Leave balance', url: `/leaves/balance?year=${new Date().getFullYear()}` },
      )
    }
    if (module === 'payroll') {
      requests.push({ label: 'My payslips', url: '/payroll/me' })
      if (isHr || user.role === 'PAYROLL_ADMIN') {
        requests.push(
          { label: 'Salary structures', url: '/payroll/salary-structures' },
          { label: 'Payroll runs', url: '/payroll/runs' },
        )
      }
    }
    if (module === 'performance') {
      requests.push(
        { label: 'My goals', url: '/performance/goals/me' },
        { label: 'My reviews', url: '/performance/reviews/me' },
        { label: 'Feedback invitations', url: '/performance/feedback-invitations/me' },
        { label: 'Performance scorecard', url: '/performance/scorecard/me' },
      )
    }
    if (module === 'notifications') {
      requests.push(
        { label: 'My notifications', url: '/notifications/me' },
        { label: 'Unread notification count', url: '/notifications/unread-count' },
      )
    }
    if (module === 'insights') {
      setResults([])
      setNotice('')
      setLoading(false)
      return
    }

    const settled = await Promise.all(requests.map(async request => {
      try {
        const response = await api.get(request.url)
        return { label: request.label, value: response.data } as DataResult
      } catch (error) {
        const status = typeof error === 'object' && error !== null && 'response' in error
          ? (error as { response?: { status?: number } }).response?.status
          : undefined
        return {
          label: request.label,
          value: null,
          error: errorMessage(error),
          status,
        } as DataResult
      }
    }))
    setResults(settled)
    setLoading(false)
  }, [user])

  useEffect(() => {
    if (!user) return
    const timer = window.setTimeout(() => void loadModule(active), 0)
    return () => window.clearTimeout(timer)
  }, [user, active, loadModule])

  if (restoringSession) {
    return <main className="login-page"><p className="muted">Restoring your secure session...</p></main>
  }
  if (pathname === '/reset-password') return <PasswordReset onBack={returnToLogin} />
  if (pathname === '/register') {
    return <Registration onBack={returnToLogin} onRegister={profile => {
      setUser(profile)
      setActive('overview')
      window.history.pushState(null, '', '/')
      setPathname('/')
    }} />
  }
  if (!user) return <Login onLogin={login} />

  const isHr = ['SUPER_ADMIN', 'HR_ADMIN', 'HR_MANAGER'].includes(user.role)
  const canViewDirectory = isHr || ['MANAGER', 'DEPARTMENT_MANAGER'].includes(user.role)
  const profileMissing = results.some(
    result => result.label === 'My employee profile' && result.status === 404,
  )
  const activeNav = navigation.find(item => item.key === active)
  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <div className="brand"><span className="brand-mark"><Activity size={21} /></span><span>Nexus<span>HR</span></span></div>
          <button className="icon-button close-menu" onClick={() => setMenuOpen(false)} aria-label="Close menu"><X size={20} /></button>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <div className="workspace-card"><div className="workspace-avatar">N</div><div><strong>NexusHR</strong><small>Enterprise workspace</small></div><ChevronRight size={15} /></div>
        <div className="nav-label">MENU</div>
        <nav className="main-nav">
          {navigation.filter(item => {
            const role = user.role
            if (role === 'SUPER_ADMIN' || role === 'HR_ADMIN' || role === 'HR_MANAGER') return true
            if (role === 'MANAGER' || role === 'DEPARTMENT_MANAGER') return ['overview', 'employees', 'attendance', 'performance', 'notifications'].includes(item.key)
            if (role === 'PAYROLL_ADMIN') return ['overview', 'payroll', 'notifications'].includes(item.key)
            if (role === 'EMPLOYEE') return ['overview', 'employees', 'attendance', 'payroll', 'performance', 'notifications'].includes(item.key)
            return item.key === 'overview'
          }).map(item => {
            const Icon = item.icon
            return <button key={item.key} className={`nav-item ${active === item.key ? 'active' : ''}`} onClick={() => { setActive(item.key); setMenuOpen(false) }}><Icon size={19} /><span>{item.key === 'employees' && user.role === 'EMPLOYEE' ? 'My Profile' : item.label}</span>{item.key === 'notifications' && <i className="nav-dot" />}</button>
          })}
        </nav>
        <div className="sidebar-bottom">
          {isHr && <div className="help-card"><div className="help-icon"><Sparkles size={17} /></div><strong>Workforce insights</strong><p>Turn people data into better decisions.</p><button onClick={() => setActive('insights')}>Explore insights <ChevronRight size={14} /></button></div>}
          <div className="sidebar-user"><div className="sidebar-user-info"><div className="user-avatar">{user.fullName?.charAt(0)?.toUpperCase() || 'U'}</div><div className="user-details"><strong>{user.fullName}</strong><small>{user.role?.replaceAll('_', ' ')}</small></div></div><button className="signout-button" onClick={() => { void logout() }}><LogOut size={16} /><span>Sign Out</span></button></div>
        </div>
      </aside>

      {menuOpen && <button className="mobile-overlay" aria-label="Close navigation" onClick={() => setMenuOpen(false)} />}
      <main className="main-area">
        <header className="topbar">
          <div className="topbar-left"><button className="icon-button menu-toggle" onClick={() => setMenuOpen(true)} aria-label="Open menu"><Menu size={21} /></button><span className="breadcrumb">Workspace</span><ChevronRight size={15} /><strong>{activeNav?.label}</strong></div>
          <div className="topbar-actions"><div className="top-date">{new Date().toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })}</div><button className="icon-button notification-button" onClick={() => setActive('notifications')} aria-label="Notifications"><Bell size={19} /></button><div className="top-user"><div className="user-avatar small">{user.fullName?.charAt(0)?.toUpperCase() || 'U'}</div><span>{user.fullName}</span></div></div>
        </header>

        <div className="page-content">
          <div className="page-heading">
            <div><div className="eyebrow">NEXUSHR WORKSPACE</div><h1>{titles[active]}</h1><p>{active === 'overview' ? `Welcome back, ${user.fullName?.split(' ')[0] || 'there'}. Here's your ${isHr ? 'workforce' : 'personal'} workspace at a glance.` : `Manage and review your ${titles[active].toLowerCase()} information.`}</p></div>
            <button className="refresh-button" onClick={() => void loadModule(active)} disabled={loading}><Activity size={16} className={loading ? 'spin' : ''} /> Refresh data</button>
          </div>

          {active === 'overview' && <div className="welcome-banner"><div><span className="banner-tag">YOUR WORKSPACE</span><h2>Build a better workplace,<br />one insight at a time.</h2><p>Manage people, support performance, and keep your workforce connected.</p></div><div className="banner-art"><div className="art-circle"><Users size={45} /></div><div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" /></div></div>}

          <div className="metric-grid">
            <button type="button" className="metric-card" onClick={() => setActive('employees')} aria-label="Open employee management">
              <div className="metric-icon blue"><Users size={20} /></div><span>Employee management</span><strong>{isHr || user.role === 'MANAGER' || user.role === 'DEPARTMENT_MANAGER' ? metricValue(results, 'Employee directory') : metricValue(results, 'My employee profile')}</strong><small>{isHr || user.role === 'MANAGER' || user.role === 'DEPARTMENT_MANAGER' ? 'Employees in your accessible directory' : 'My employee profile'}</small>
            </button>
            <button type="button" className="metric-card" onClick={() => setActive('attendance')} aria-label="Open attendance and leave">
              <div className="metric-icon green"><CalendarDays size={20} /></div><span>Attendance & leave</span><strong>{metricValue(results, 'My attendance records')}</strong><small>My attendance records</small>
            </button>
            <button type="button" className="metric-card" onClick={() => setActive('payroll')} aria-label="Open payroll">
              <div className="metric-icon orange"><CircleDollarSign size={20} /></div><span>Payroll</span><strong>{metricValue(results, 'My payslips')}</strong><small>My payslips</small>
            </button>
            <button type="button" className="metric-card" onClick={() => setActive('performance')} aria-label="Open performance">
              <div className="metric-icon violet"><ChartNoAxesCombined size={20} /></div><span>Performance</span><strong>{metricValue(results, 'My goals')}</strong><small>My performance goals</small>
            </button>
          </div>

          <Suspense fallback={<div className="loading-state"><LoaderCircle className="spin" size={25} /><span>Loading workspace...</span></div>}>
            <ActionWorkspace
              module={active}
              role={user.role}
              onSuccess={() => void loadModule(active)}
              profileMissing={active === 'employees' && profileMissing}
            />
          </Suspense>
          {active === 'employees' && (isHr || user.role === 'EMPLOYEE') &&
            <Suspense fallback={<div className="friendly-empty">Loading lifecycle requests...</div>}>
              <EmployeeLifecycleApprovals role={user.role} />
            </Suspense>}
          {active === 'employees' && (isHr || user.role === 'EMPLOYEE') &&
            <Suspense fallback={<div className="friendly-empty">Loading employee documents...</div>}>
              <EmployeeDocuments role={user.role} />
            </Suspense>}
          {active === 'employees' && ['SUPER_ADMIN', 'HR_ADMIN'].includes(user.role) &&
            <Suspense fallback={<div className="friendly-empty">Loading invitation form...</div>}>
              <EmployeeInvitations isSuperAdmin={user.role === 'SUPER_ADMIN'} />
            </Suspense>}
          {active === 'payroll' && (isHr || user.role === 'PAYROLL_ADMIN') &&
            <Suspense fallback={<div className="friendly-empty">Loading payroll exports...</div>}>
              <PayrollRunExports />
            </Suspense>}

          {notice && <div className="info-banner"><Sparkles size={19} /><p>{notice}</p></div>}
          {active === 'insights' && <div className="insight-grid"><div className="insight-card"><Sparkles size={22} /><h3>Attrition indicator</h3><p>Assess submitted attendance, engagement, performance, and tenure signals.</p></div><div className="insight-card"><Activity size={22} /><h3>Skill gap analysis</h3><p>Compare current skills with the skills required for a target role.</p></div><div className="insight-card"><ChartNoAxesCombined size={22} /><h3>Engagement score</h3><p>Calculate a weighted engagement indicator from submitted metrics.</p></div></div>}

          <div className="section-heading"><div><h2>{active === 'overview' ? 'Workspace data' : `${titles[active]} data`}</h2><p>Live information retrieved from your NexusHR services.</p></div><span className="live-indicator"><i /> LIVE API</span></div>
          {active === 'overview' && canViewDirectory && <Suspense fallback={<div className="friendly-empty">Loading workforce charts...</div>}>
            <WorkforceCharts value={results.find(item => item.label === 'Workforce analytics')?.value} />
          </Suspense>}
          {loading ? <div className="loading-state"><LoaderCircle className="spin" size={25} /><span>Loading workspace data...</span></div> : results.length > 0 ? <div className={`data-grid ${active === 'employees' ? 'data-grid-wide' : ''}`}>{results.map((result, index) => <DataPanel key={`${result.label}-${index}`} result={result} onCompleteProfile={profileMissing ? () => setActive('employees') : undefined} />)}</div> : active !== 'insights' ? <div className="empty-state"><ClipboardList size={30} /><h3>No data to display</h3><p>There are no records available for this section yet.</p></div> : null}

          <footer className="page-footer"><span>(c) 2026 NexusHR</span><span>Enterprise HR & Workforce Intelligence</span><span className="footer-secure"><ShieldCheck size={14} /> Secure workspace</span></footer>
        </div>
      </main>
    </div>
  )
}

export default App
