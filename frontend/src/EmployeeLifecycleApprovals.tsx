import { useCallback, useEffect, useState } from 'react'
import { Check, LoaderCircle, RotateCcw, UserRoundX, X } from 'lucide-react'
import { api } from './lib/api'

type LifecycleRequest = {
  id: string
  employeeId: string
  action: 'ONBOARDING' | 'OFFBOARDING'
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  requestedBy: string
  reason: string
  reviewedBy?: string
  reviewNote?: string
  requestedAt: string
}

type EmployeeOption = {
  id: string
  fullName: string
  employeeCode: string
  employmentStatus: string
}

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as {
      response?: {
        status?: number
        data?: { message?: string; error?: string; detail?: string; title?: string }
      }
    }).response
    return response?.data?.message || response?.data?.detail || response?.data?.error ||
      response?.data?.title ||
      (response?.status ? `Request failed (HTTP ${response.status}). Please try again.` : 'Request failed. Please try again.')
  }
  return error instanceof Error ? error.message : 'Unable to connect to the server.'
}

export default function EmployeeLifecycleApprovals({ role }: { role: string }) {
  const isHr = ['SUPER_ADMIN', 'HR_ADMIN', 'HR_MANAGER'].includes(role)
  const [mine, setMine] = useState<LifecycleRequest[]>([])
  const [pending, setPending] = useState<LifecycleRequest[]>([])
  const [employees, setEmployees] = useState<EmployeeOption[]>([])
  const [employeeId, setEmployeeId] = useState('')
  const [reason, setReason] = useState('')
  const [reviewNotes, setReviewNotes] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const reload = useCallback(async () => {
    const calls = isHr
      ? [
          api.get('/employees/lifecycle-requests/pending'),
          api.get('/employees', { params: { page: 0, size: 100, sortBy: 'fullName' } }),
        ]
      : [api.get('/employees/me/lifecycle-requests')]
    const results = await Promise.allSettled(calls)
    if (isHr) {
      setMine([])
      if (results[0]?.status === 'fulfilled') {
        setPending(Array.isArray(results[0].value.data) ? results[0].value.data : [])
      } else if (results[0]?.status === 'rejected') {
        setError(getErrorMessage(results[0].reason))
      }
      if (results[1]?.status === 'fulfilled') {
        const content = results[1].value.data?.content
        if (Array.isArray(content)) {
          setEmployees(content.filter((item: unknown): item is EmployeeOption =>
            typeof item === 'object' && item !== null &&
            'id' in item && typeof item.id === 'string' &&
            'fullName' in item && typeof item.fullName === 'string' &&
            'employeeCode' in item && typeof item.employeeCode === 'string' &&
            'employmentStatus' in item && typeof item.employmentStatus === 'string'))
        }
      } else if (results[1]?.status === 'rejected') {
        setError(getErrorMessage(results[1].reason))
      }
    } else if (results[0]?.status === 'fulfilled') {
      setMine(Array.isArray(results[0].value.data) ? results[0].value.data : [])
    } else if (results[0]?.status === 'rejected') {
      setError(getErrorMessage(results[0].reason))
    }
    setLoading(false)
  }, [isHr])

  useEffect(() => {
    const timer = window.setTimeout(() => void reload(), 0)
    return () => window.clearTimeout(timer)
  }, [reload])

  async function submitOffboarding(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy('request')
    setError('')
    setMessage('')
    try {
      const path = isHr
        ? `/employees/${encodeURIComponent(employeeId)}/lifecycle-requests`
        : '/employees/me/lifecycle-requests'
      const body = isHr ? { action: 'OFFBOARDING', reason } : { reason }
      await api.post(path, body)
      setReason('')
      setMessage('Offboarding request submitted for approval.')
      await reload()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy('')
    }
  }

  async function decide(requestId: string, approved: boolean) {
    setBusy(requestId)
    setError('')
    setMessage('')
    try {
      await api.post(
        `/employees/lifecycle-requests/${encodeURIComponent(requestId)}/decision`,
        { approved, reviewNote: reviewNotes[requestId] || '' },
      )
      setMessage(approved ? 'Lifecycle request approved.' : 'Lifecycle request rejected.')
      setReviewNotes(current => ({ ...current, [requestId]: '' }))
      await reload()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy('')
    }
  }

  const employeeName = (id: string) => {
    const employee = employees.find(item => item.id === id)
    return employee
      ? `${employee.fullName} · ${employee.employeeCode}`
      : id
  }

  return (
    <section className="lifecycle-approvals" aria-labelledby="lifecycle-approvals-heading">
      <div className="lifecycle-heading">
        <div>
          <h2 id="lifecycle-approvals-heading">Onboarding & offboarding approvals</h2>
          <p>Employee lifecycle changes remain pending until an authorized reviewer acts.</p>
        </div>
        <RotateCcw size={20} aria-hidden="true" />
      </div>

      <form className="lifecycle-request-form" onSubmit={event => void submitOffboarding(event)}>
        {isHr && <label>
          Employee
          <select value={employeeId} onChange={event => setEmployeeId(event.target.value)} required>
            <option value="">Select an employee</option>
            {employees.filter(employee =>
              ['ACTIVE', 'ON_LEAVE'].includes(employee.employmentStatus)).map(employee => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName} · {employee.employeeCode}
                </option>
              ))}
          </select>
        </label>}
        {!isHr && <p>Submit a request to begin the offboarding review. Your account remains active until approval.</p>}
        <label>
          Reason
          <textarea value={reason} onChange={event => setReason(event.target.value)} maxLength={1000} required rows={2} />
        </label>
        <button className="primary-button" type="submit" disabled={busy !== '' || (isHr && !employeeId)}>
          {busy === 'request' ? <LoaderCircle className="spin" size={16} /> : <UserRoundX size={16} />}
          Request offboarding
        </button>
      </form>

      {message && <p className="lifecycle-message" role="status">{message}</p>}
      {error && <p className="lifecycle-error" role="alert">{error}</p>}

      {loading ? (
        <div className="lifecycle-empty"><LoaderCircle className="spin" size={17} /> Loading requests...</div>
      ) : (
        <>
          {isHr && <div className="lifecycle-section">
            <h3>Pending approvals <span>{pending.length}</span></h3>
            {pending.length === 0
              ? <div className="lifecycle-empty">No lifecycle requests are awaiting review.</div>
              : <ul className="lifecycle-list">
                  {pending.map(request => (
                    <li key={request.id}>
                      <div className="lifecycle-request-details">
                        <strong>{request.action} · {employeeName(request.employeeId)}</strong>
                        <span>Requested by {request.requestedBy} on {new Date(request.requestedAt).toLocaleDateString()}</span>
                        <p>{request.reason}</p>
                        <textarea
                          aria-label={`Review note for ${employeeName(request.employeeId)}`}
                          placeholder="A reason is required to reject"
                          maxLength={1000}
                          rows={2}
                          value={reviewNotes[request.id] || ''}
                          onChange={event => setReviewNotes(current => ({ ...current, [request.id]: event.target.value }))}
                        />
                      </div>
                      <div className="lifecycle-actions">
                        <button type="button" onClick={() => void decide(request.id, true)} disabled={busy !== ''}>
                          {busy === request.id ? <LoaderCircle className="spin" size={15} /> : <Check size={15} />}
                          Approve
                        </button>
                        <button type="button" onClick={() => void decide(request.id, false)} disabled={busy !== '' || !(reviewNotes[request.id] || '').trim()}>
                          <X size={15} /> Reject
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>}
          </div>}

          {!isHr && <div className="lifecycle-section">
            <h3>My lifecycle requests</h3>
            {mine.length === 0
              ? <div className="lifecycle-empty">No onboarding or offboarding requests yet.</div>
              : <ul className="lifecycle-list">
                  {mine.map(request => (
                    <li key={request.id}>
                      <div className="lifecycle-request-details">
                        <strong>{request.action} · {request.status}</strong>
                        <span>Submitted {new Date(request.requestedAt).toLocaleDateString()}</span>
                        <p>{request.reason}</p>
                        {request.reviewNote && <span>Reviewer note: {request.reviewNote}</span>}
                      </div>
                    </li>
                  ))}
                </ul>}
          </div>}
        </>
      )}
    </section>
  )
}
