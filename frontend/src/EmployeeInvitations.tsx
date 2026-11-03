import { useState } from 'react'
import { Check, Copy, Send } from 'lucide-react'
import { api } from './lib/api'

type Role =
  | 'SUPER_ADMIN'
  | 'HR_ADMIN'
  | 'HR_MANAGER'
  | 'DEPARTMENT_MANAGER'
  | 'MANAGER'
  | 'EMPLOYEE'
  | 'PAYROLL_ADMIN'

type Invitation = {
  email: string
  role: Role
  invitationToken: string
  expiresAt: string
}

const employeeRoles: Role[] = [
  'EMPLOYEE',
  'MANAGER',
  'DEPARTMENT_MANAGER',
  'HR_MANAGER',
]

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as {
      response?: { status?: number; data?: Record<string, unknown> }
    }).response
    const data = response?.data
    for (const key of ['detail', 'message', 'error', 'title']) {
      if (typeof data?.[key] === 'string') return data[key]
    }
    return response?.status
      ? `Invitation failed (HTTP ${response.status}). Please try again.`
      : 'Unable to create the invitation. Please try again.'
  }
  return error instanceof Error ? error.message : 'Unable to connect to the server.'
}

export default function EmployeeInvitations({ isSuperAdmin }: { isSuperAdmin: boolean }) {
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('EMPLOYEE')
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  const roles: Role[] = isSuperAdmin
    ? ['EMPLOYEE', 'MANAGER', 'DEPARTMENT_MANAGER', 'HR_MANAGER', 'HR_ADMIN', 'PAYROLL_ADMIN', 'SUPER_ADMIN']
    : employeeRoles

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setInvitation(null)
    setCopied(false)
    try {
      const response = await api.post<Invitation>('/auth/invitations', { email, role })
      setInvitation(response.data)
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  function invitationLink(value: Invitation) {
    const url = new URL('/register', window.location.origin)
    url.searchParams.set('email', value.email)
    url.hash = new URLSearchParams({ token: value.invitationToken }).toString()
    return url.toString()
  }

  async function copyInvitationLink() {
    if (!invitation) return
    try {
      await navigator.clipboard.writeText(invitationLink(invitation))
      setCopied(true)
      setError('')
    } catch {
      setError('Could not copy automatically. Select and copy the invitation link below.')
    }
  }

  return (
    <section className="employee-invitations">
      <div className="employee-invitations-heading">
        <div>
          <h2>Invite a new employee</h2>
          <p>Create a secure, single-use registration link. Share it directly with the intended employee.</p>
        </div>
        <Send size={20} aria-hidden="true" />
      </div>
      <form className="invitation-form" onSubmit={event => void submit(event)}>
        <label>
          Work email
          <input
            type="email"
            autoComplete="email"
            maxLength={180}
            value={email}
            onChange={event => setEmail(event.target.value)}
            placeholder="new.employee@company.com"
            required
          />
        </label>
        <label>
          Account role
          <select value={role} onChange={event => setRole(event.target.value as Role)}>
            {roles.map(item => (
              <option key={item} value={item}>{item.replaceAll('_', ' ')}</option>
            ))}
          </select>
        </label>
        <button className="primary-button" type="submit" disabled={busy}>
          {busy ? 'Creating invitation...' : 'Create invitation'}
        </button>
      </form>
      {error && <p className="invitation-error" role="alert">{error}</p>}
      {invitation && (
        <div className="invitation-result" role="status">
          <div className="invitation-result-heading">
            <Check size={17} />
            <strong>Invitation created for {invitation.email}</strong>
          </div>
          <p>Role: {invitation.role.replaceAll('_', ' ')}. Expires {new Date(invitation.expiresAt).toLocaleString()}.</p>
          <label htmlFor="employee-invitation-link">Registration link</label>
          <div className="invitation-link-row">
            <input id="employee-invitation-link" readOnly value={invitationLink(invitation)} onFocus={event => event.currentTarget.select()} />
            <button type="button" className="refresh-button" onClick={() => void copyInvitationLink()}>
              <Copy size={15} /> {copied ? 'Copied' : 'Copy link'}
            </button>
          </div>
          <small>This link contains a private, single-use invitation token. Share it only with the intended recipient.</small>
        </div>
      )}
    </section>
  )
}
