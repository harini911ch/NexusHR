import { useEffect, useState } from 'react'
import { Activity, CheckCircle2, LoaderCircle, Send } from 'lucide-react'
import { api } from './lib/api'

type ModuleKey = 'overview' | 'employees' | 'attendance' | 'payroll' | 'performance' | 'insights' | 'notifications'

type Field = {
  name: string
  label: string
  type?: string
  required?: boolean
  placeholder?: string
  options?: string[]
  help?: string
}

type Action = {
  title: string
  description: string
  method: 'POST' | 'PUT'
  endpoint: string
  fields: Field[]
  success: string
}

type Props = {
  module: ModuleKey
  role: string
  onSuccess: () => void
  profileMissing: boolean
}

const actions: Partial<Record<ModuleKey, Action[]>> = {
  employees: [{
    title: 'Complete my employee profile',
    description: 'Creates the profile for the currently signed-in account.',
    method: 'POST',
    endpoint: '/employees/me',
    success: 'Employee profile saved.',
    fields: [
      { name: 'fullName', label: 'Full name', required: true },
      { name: 'department', label: 'Department', required: true },
      { name: 'jobTitle', label: 'Job title', required: true },
      { name: 'employmentType', label: 'Employment type', type: 'select', options: ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN'], required: true },
      { name: 'hireDate', label: 'Hire date', type: 'date', required: true },
      { name: 'phoneNumber', label: 'Phone number' },
    ],
  }],

  attendance: [
    {
      title: 'Check in',
      description: 'Record your attendance for the current day.',
      method: 'POST',
      endpoint: '/attendance/check-in',
      success: 'Check-in recorded.',
      fields: [],
    },
    {
      title: 'Check out',
      description: 'Record the end of your workday.',
      method: 'POST',
      endpoint: '/attendance/check-out',
      success: 'Check-out recorded.',
      fields: [],
    },
    {
      title: 'Apply for leave',
      description: 'Submit a leave request for HR or your manager to review.',
      method: 'POST',
      endpoint: '/leaves',
      success: 'Leave request submitted.',
      fields: [
        { name: 'leaveType', label: 'Leave type', type: 'select', options: ['CASUAL', 'SICK', 'EARNED', 'UNPAID'], required: true },
        { name: 'startDate', label: 'Start date', type: 'date', required: true },
        { name: 'endDate', label: 'End date', type: 'date', required: true },
        { name: 'reason', label: 'Reason', type: 'textarea', required: true },
      ],
    },
    {
      title: 'Review a leave request',
      description: 'Approve or reject a pending leave request. HR Admin or an authorized manager can review.',
      method: 'PUT',
      endpoint: '/leaves/{id}/review',
      success: 'Leave request reviewed.',
      fields: [
        { name: 'id', label: 'Leave request UUID', required: true },
        { name: 'decision', label: 'Decision', type: 'select', options: ['APPROVED', 'REJECTED'], required: true },
        { name: 'comment', label: 'Review comment', type: 'textarea' },
      ],
    },
  ],

  payroll: [
    {
      title: 'Create salary structure',
      description: "Set an employee's salary components before processing payroll. Only one structure can exist per employee.",
      method: 'POST',
      endpoint: '/payroll/salary-structures',
      success: 'Salary structure created.',
      fields: [
        { name: 'employeeId', label: 'Employee UUID', required: true },
        { name: 'employeeCode', label: 'Employee code', required: true },
        { name: 'employeeEmail', label: 'Employee email', type: 'email', required: true },
        { name: 'basicSalary', label: 'Basic salary', type: 'number', required: true },
        { name: 'housingAllowance', label: 'Housing allowance', type: 'number', placeholder: '0' },
        { name: 'transportAllowance', label: 'Transport allowance', type: 'number', placeholder: '0' },
        { name: 'otherAllowance', label: 'Other allowance', type: 'number', placeholder: '0' },
        { name: 'taxPercentage', label: 'Tax percentage', type: 'number', placeholder: '0' },
        { name: 'otherDeduction', label: 'Other deduction', type: 'number', placeholder: '0' },
        { name: 'currency', label: 'Currency', required: true, placeholder: 'INR' },
        { name: 'effectiveFrom', label: 'Effective from', type: 'date', required: true },
      ],
    },
    {
      title: 'Process payroll',
      description: 'Calculate one employee payslip for the selected period. A different payroll administrator must approve it before it is treated as approved.',
      method: 'POST',
      endpoint: '/payroll/process',
      success: 'Payroll processing request completed.',
      fields: [
        { name: 'employeeId', label: 'Employee UUID', required: true },
        { name: 'month', label: 'Month (1-12)', type: 'number', required: true, placeholder: '10' },
        { name: 'year', label: 'Year', type: 'number', required: true, placeholder: '2026' },
      ],
    },
    {
      title: 'Create payroll run',
      description: 'Prepare pending payslips for every effective salary structure in a period. A different authorized reviewer must approve the complete run.',
      method: 'POST',
      endpoint: '/payroll/runs',
      success: 'Payroll run prepared and awaiting approval.',
      fields: [
        { name: 'month', label: 'Month (1–12)', type: 'number', required: true, placeholder: '10' },
        { name: 'year', label: 'Year', type: 'number', required: true, placeholder: '2026' },
      ],
    },
    {
      title: 'Approve payroll run',
      description: 'Approve all pending payslips in an existing run. The creator cannot approve their own run.',
      method: 'PUT',
      endpoint: '/payroll/runs/{id}/approve',
      success: 'Payroll run approved.',
      fields: [
        { name: 'id', label: 'Payroll run UUID', required: true },
      ],
    },
  ],

  performance: [
    {
      title: 'Create a performance goal',
      description: 'Add a measurable goal to your performance plan.',
      method: 'POST',
      endpoint: '/performance/goals',
      success: 'Performance goal created.',
      fields: [
        { name: 'title', label: 'Goal title', required: true },
        { name: 'description', label: 'Description', type: 'textarea' },
        { name: 'category', label: 'Category', required: true, placeholder: 'Technical, Delivery, Learning...' },
        { name: 'targetDate', label: 'Target date', type: 'date', required: true },
      ],
    },
    {
      title: 'Update goal progress',
      description: 'Update progress and status for one of your goals.',
      method: 'PUT',
      endpoint: '/performance/goals/{id}/progress',
      success: 'Goal progress updated.',
      fields: [
        { name: 'id', label: 'Goal UUID', required: true },
        { name: 'progress', label: 'Progress (0-100)', type: 'number', required: true },
        { name: 'status', label: 'Goal status', type: 'select', options: ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD'], required: true },
      ],
    },
    {
      title: 'Create employee review',
      description: 'HR or a manager can create a review for an employee.',
      method: 'POST',
      endpoint: '/performance/reviews',
      success: 'Performance review created.',
      fields: [
        { name: 'employeeEmail', label: 'Employee email', type: 'email', required: true },
        { name: 'reviewPeriod', label: 'Review period', required: true, placeholder: 'Q4 2026' },
      ],
    },
    {
      title: 'Invite a 360-degree reviewer',
      description: 'Invite an employee to submit feedback for a review. Only invited employees can respond.',
      method: 'POST',
      endpoint: '/performance/reviews/{id}/feedback-invitations',
      success: 'Feedback invitation created.',
      fields: [
        { name: 'id', label: 'Review UUID', required: true },
        { name: 'inviteeEmail', label: 'Reviewer email', type: 'email', required: true },
        { name: 'relationship', label: 'Relationship', type: 'select', options: ['PEER', 'MANAGER', 'DIRECT_REPORT', 'COLLEAGUE'], required: true },
      ],
    },
    {
      title: 'Submit self-assessment',
      description: 'Submit your assessment for an existing review.',
      method: 'PUT',
      endpoint: '/performance/reviews/{id}/self-assessment',
      success: 'Self-assessment submitted.',
      fields: [
        { name: 'id', label: 'Review UUID', required: true },
        { name: 'rating', label: 'Rating (1-5)', type: 'number', required: true },
        { name: 'assessment', label: 'Your assessment', type: 'textarea', required: true },
      ],
    },
    {
      title: 'Submit manager assessment',
      description: 'Submit a manager assessment for an existing review.',
      method: 'PUT',
      endpoint: '/performance/reviews/{id}/manager-assessment',
      success: 'Manager assessment submitted.',
      fields: [
        { name: 'id', label: 'Review UUID', required: true },
        { name: 'rating', label: 'Rating (1-5)', type: 'number', required: true },
        { name: 'strengths', label: 'Strengths', type: 'textarea', required: true },
        { name: 'improvementAreas', label: 'Improvement areas', type: 'textarea', required: true },
        { name: 'comments', label: 'Comments', type: 'textarea', required: true },
      ],
    },
    {
      title: 'Submit 360-degree feedback',
      description: 'Submit feedback when invited to an existing review.',
      method: 'POST',
      endpoint: '/performance/reviews/{id}/feedback',
      success: 'Feedback submitted.',
      fields: [
        { name: 'id', label: 'Review UUID', required: true },
        { name: 'rating', label: 'Rating (1-5)', type: 'number', required: true },
        { name: 'comments', label: 'Feedback comments', type: 'textarea', required: true },
      ],
    },
  ],

  insights: [
    {
      title: 'Attrition risk indicator',
      description: 'Rule-based indicator using submitted workforce signals. It is not a validated prediction.',
      method: 'POST',
      endpoint: '/insights/attrition',
      success: 'Attrition indicator generated.',
      fields: [
        { name: 'employeeEmail', label: 'Employee email', type: 'email', required: true },
        { name: 'attendanceRate', label: 'Attendance rate (0-100)', type: 'number', required: true },
        { name: 'engagementScore', label: 'Engagement score (0-100)', type: 'number', required: true },
        { name: 'performanceRating', label: 'Performance rating (0-5)', type: 'number', required: true },
        { name: 'tenureMonths', label: 'Tenure (months)', type: 'number', required: true },
        { name: 'recentAbsences', label: 'Recent absences', type: 'number', required: true },
        { name: 'monthsSinceLastPromotion', label: 'Months since last promotion', type: 'number', required: true },
      ],
    },
    {
      title: 'Skill gap analysis',
      description: "Compare an employee's current skills with the skills required for a target role.",
      method: 'POST',
      endpoint: '/insights/skill-gaps',
      success: 'Skill gap analysis generated.',
      fields: [
        { name: 'employeeEmail', label: 'Employee email', type: 'email', required: true },
        { name: 'targetRole', label: 'Target role', required: true },
        { name: 'currentSkills', label: 'Current skills (comma-separated)', required: true, placeholder: 'Java, Spring Boot, SQL' },
        { name: 'requiredSkills', label: 'Required skills (comma-separated)', required: true, placeholder: 'Java, Spring Boot, Docker' },
      ],
    },
    {
      title: 'Engagement score',
      description: 'Calculate a weighted engagement indicator from submitted metrics.',
      method: 'POST',
      endpoint: '/insights/engagement',
      success: 'Engagement indicator generated.',
      fields: [
        { name: 'employeeEmail', label: 'Employee email', type: 'email', required: true },
        { name: 'attendanceRate', label: 'Attendance rate (0-100)', type: 'number', required: true },
        { name: 'feedbackRating', label: 'Feedback rating (1-5)', type: 'number', required: true },
        { name: 'managerRating', label: 'Manager rating (1-5)', type: 'number', required: true },
        { name: 'goalProgress', label: 'Goal progress (0-100)', type: 'number', required: true },
      ],
    },
  ],

  notifications: [
    {
      title: 'Send a notification',
      description: 'Send an in-app notification to an employee.',
      method: 'POST',
      endpoint: '/notifications',
      success: 'Notification created.',
      fields: [
        { name: 'recipientEmail', label: 'Recipient email', type: 'email', required: true },
        { name: 'title', label: 'Title', required: true },
        { name: 'message', label: 'Message', type: 'textarea', required: true },
        { name: 'type', label: 'Type', type: 'select', options: ['GENERAL', 'LEAVE', 'PAYROLL', 'PERFORMANCE', 'ATTENDANCE'], required: true },
        { name: 'relatedEntityType', label: 'Related entity type (optional)' },
        { name: 'relatedEntityId', label: 'Related entity ID (optional)' },
      ],
    },
    {
      title: 'Mark notification as read',
      description: 'Mark one of your notifications as read using its notification UUID.',
      method: 'PUT',
      endpoint: '/notifications/{id}/read',
      success: 'Notification marked as read.',
      fields: [
        { name: 'id', label: 'Notification UUID', required: true },
      ],
    },
  ],
}

function ActionCard({ action, onSuccess }: { action: Action; onSuccess: () => void }) {
  const [values, setValues] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [insightResult, setInsightResult] = useState<Record<string, unknown> | null>(null)

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')

    const payload: Record<string, unknown> = {}

    for (const field of action.fields) {
      const value = values[field.name]?.trim() ?? ''
      if (!value) continue

      if (field.name === 'currentSkills' || field.name === 'requiredSkills') {
        payload[field.name] = value.split(',').map(item => item.trim()).filter(Boolean)
      } else if (field.type === 'number') {
        payload[field.name] = Number(value)
      } else {
        payload[field.name] = value
      }
    }

    const id = String(payload.id ?? '')
    delete payload.id
    const endpoint = action.endpoint.replace('{id}', encodeURIComponent(id))

    try {
      const response = await (action.method === 'POST'
        ? api.post(endpoint, payload)
        : api.put(endpoint, payload))

      if (action.endpoint.startsWith('/insights/')) {
        setInsightResult(response.data as Record<string, unknown>)
      }

      setMessage(action.success)
      setValues({})
      onSuccess()
    } catch (err) {
      const e = err as {
        response?: { data?: { message?: string; error?: string; detail?: string } }
        message?: string
      }
      setError(
        e.response?.data?.message ||
        e.response?.data?.error ||
        e.response?.data?.detail ||
        e.message ||
        'Request failed.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="action-card">
      <div className="action-card-heading">
        <div>
          <h3>{action.title}</h3>
        </div>
        <Activity size={19} />
      </div>

      <p>{action.description}</p>

      <form className="action-form" onSubmit={submit}>
        {action.fields.map(field => (
          <label className="action-field" key={field.name}>
            <span>{field.label}{field.required ? ' *' : ''}</span>

            {field.type === 'textarea' ? (
              <textarea
                required={field.required}
                value={values[field.name] || ''}
                placeholder={field.placeholder}
                onChange={e => setValues(v => ({ ...v, [field.name]: e.target.value }))}
                rows={3}
              />
            ) : field.type === 'select' ? (
              <select
                required={field.required}
                value={values[field.name] || ''}
                onChange={e => setValues(v => ({ ...v, [field.name]: e.target.value }))}
              >
                <option value="">Choose...</option>
                {field.options?.map(option => (
                  <option key={option} value={option}>{option.replaceAll('_', ' ')}</option>
                ))}
              </select>
            ) : (
              <input
                required={field.required}
                type={field.type || 'text'}
                min={field.type === 'number'
                  ? field.name === 'rating' || field.name === 'feedbackRating' || field.name === 'managerRating'
                    ? 1 : 0
                  : undefined}
                max={field.name === 'rating' || field.name === 'feedbackRating' || field.name === 'managerRating'
                  ? 5
                  : field.name === 'month'
                    ? 12
                    : field.name === 'year'
                      ? 2100
                      : field.name === 'progress' || field.name === 'attendanceRate' || field.name === 'engagementScore' || field.name === 'goalProgress' || field.name === 'taxPercentage'
                        ? 100
                        : undefined}
                placeholder={field.placeholder}
                value={values[field.name] || ''}
                onChange={e => setValues(v => ({ ...v, [field.name]: e.target.value }))}
              />
            )}
          </label>
        ))}

        <button className="primary-button action-submit" type="submit" disabled={busy}>
          {busy
            ? <><LoaderCircle className="spin" size={16} /> Submitting...</>
            : <><Send size={15} /> {action.fields.length ? 'Submit request' : 'Confirm action'}</>}
        </button>
      </form>

      {message && <div className="action-success"><CheckCircle2 size={17} />{message}</div>}
      {error && <div className="action-error">{error}</div>}
      {insightResult && (
        <div className="insight-result">
          <h4>Analysis results</h4>
          <div className="insight-result-grid">
            {Object.entries(insightResult).map(([key, value]) => (
              <div className="insight-result-item" key={key}>
                <span>{key.replace(/([A-Z])/g, ' $1').replace(/^./, char => char.toUpperCase())}</span>
                {Array.isArray(value) ? (
                  value.length ? (
                    <ul>{value.map((item, index) => <li key={index}>{String(item)}</li>)}</ul>
                  ) : <strong>None</strong>
                ) : (
                  <strong>{value == null ? '—' : String(value)}</strong>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

const personalActionTitles = [
  'Check in',
  'Check out',
  'Apply for leave',
  'Create a performance goal',
  'Update goal progress',
  'Submit self-assessment',
  'Submit 360-degree feedback',
  'Mark notification as read',
]

type PendingLeave = {
  id: string
  employeeEmail: string
  leaveType: string
  startDate: string
  endDate: string
  totalDays: number
  reason: string
  status: string
}

function PendingLeaveRequests({ onSuccess }: { onSuccess: () => void }) {
  const [requests, setRequests] = useState<PendingLeave[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState('')
  const [comments, setComments] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function loadPending() {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/leaves/pending')
      setRequests(Array.isArray(response.data) ? response.data : [])
    } catch (err) {
      const e = err as {
        response?: { data?: { message?: string; error?: string } }
        message?: string
      }
      setError(e.response?.data?.message || e.response?.data?.error || e.message || 'Unable to load pending leave requests.')
    } finally {
      setLoading(false)
    }
  }

  async function review(id: string, decision: 'APPROVED' | 'REJECTED') {
    setBusyId(id)
    setError('')
    setMessage('')
    try {
      await api.put(`/leaves/${encodeURIComponent(id)}/review`, {
        decision,
        comment: comments[id]?.trim() || '',
      })
      setMessage("Leave request " + decision.toLowerCase() + " successfully.")
      setComments(current => {
        const next = { ...current }
        delete next[id]
        return next
      })
      await loadPending()
      onSuccess()
    } catch (err) {
      const e = err as {
        response?: { data?: { message?: string; error?: string; detail?: string } }
        message?: string
      }
      setError(e.response?.data?.message || e.response?.data?.error || e.response?.data?.detail || e.message || 'Unable to review this leave request.')
    } finally {
      setBusyId('')
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadPending(), 0)
    return () => window.clearTimeout(timer)
  }, [])

  return (
    <section className="pending-leaves">
      <div className="pending-leaves-heading">
        <div>
          <h3>Pending leave requests</h3>
          <p>Review leave applications submitted by employees.</p>
        </div>
        <button type="button" className="refresh-button" onClick={() => void loadPending()} disabled={loading}>
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {message && <div className="action-success">{message}</div>}
      {error && <div className="action-error">{error}</div>}

      {loading ? (
        <div className="friendly-empty">Loading pending requests...</div>
      ) : requests.length === 0 ? (
        <div className="friendly-empty">There are no pending leave requests.</div>
      ) : (
        <div className="pending-leave-grid">
          {requests.map(request => (
            <article className="pending-leave-card" key={request.id}>
              <div className="pending-leave-top">
                <strong>{request.leaveType?.replaceAll('_', ' ') || 'Leave'}</strong>
                <span>{request.totalDays} day(s)</span>
              </div>
              <div className="pending-leave-details">
                <div><span>Employee</span><strong>{request.employeeEmail}</strong></div>
                <div><span>From</span><strong>{request.startDate}</strong></div>
                <div><span>To</span><strong>{request.endDate}</strong></div>
                <div className="pending-leave-reason"><span>Reason</span><strong>{request.reason}</strong></div>
              </div>
              <label className="action-field pending-comment">
                <span>Review comment (optional)</span>
                <textarea
                  rows={2}
                  maxLength={1000}
                  value={comments[request.id] || ''}
                  onChange={event => setComments(current => ({ ...current, [request.id]: event.target.value }))}
                  placeholder="Add a comment for the employee..."
                />
              </label>
              <div className="pending-leave-actions">
                <button type="button" className="pending-reject" disabled={busyId === request.id} onClick={() => void review(request.id, 'REJECTED')}>Reject</button>
                <button type="button" className="pending-approve" disabled={busyId === request.id} onClick={() => void review(request.id, 'APPROVED')}>
                  {busyId === request.id ? 'Submitting...' : 'Approve'}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
export default function ActionWorkspace({ module, role, onSuccess, profileMissing }: Props) {
  const items = actions[module] || []
  const isHr = ['SUPER_ADMIN', 'HR_ADMIN', 'HR_MANAGER'].includes(role)
  const isManager = role === 'MANAGER' || role === 'DEPARTMENT_MANAGER'
  const isPayroll = role === 'PAYROLL_ADMIN'

  const visibleItems = items.filter(action => {
    if (action.title === 'Complete my employee profile') {
      return profileMissing && !isHr && !isManager && !isPayroll
    }
    if (personalActionTitles.includes(action.title)) return true

    if (action.title === 'Review a leave request') return false
    if (action.title === 'Create employee review') return isHr
    if (action.title === 'Invite a 360-degree reviewer') return isHr || isManager
    if (action.title === 'Submit manager assessment') return isHr || isManager
    if (action.title === 'Create salary structure' || action.title === 'Process payroll' ||
        action.title === 'Create payroll run' || action.title === 'Approve payroll run') return isHr || isPayroll
    if (action.title === 'Attrition risk indicator' || action.title === 'Skill gap analysis' || action.title === 'Engagement score') return isHr
    if (action.title === 'Send a notification') return isHr

    return false
  })

  const canReviewLeaves = module === 'attendance' && isHr

  if (visibleItems.length === 0 && !canReviewLeaves) return null

  return (
    <section className="action-workspace">
      <div className="section-heading action-section-heading">
        <div>
          <h2>Actions & workflows</h2>
          <p>Complete the tasks available to your workspace role.</p>
        </div>
        <span className="live-indicator"><i /> API CONNECTED</span>
      </div>

      {canReviewLeaves && <PendingLeaveRequests onSuccess={onSuccess} />}

      <div className="action-grid">
        {visibleItems.map(action => (
          <ActionCard key={action.endpoint} action={action} onSuccess={onSuccess} />
        ))}
      </div>
    </section>
  )
}
