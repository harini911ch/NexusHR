import { useEffect, useState } from 'react'
import { Download, FileSpreadsheet, FileText, LoaderCircle } from 'lucide-react'
import { api } from './lib/api'

type PayrollRun = {
  id: string
  month: number
  year: number
  status: string
  payslipCount: number
}

function isPayrollRun(value: unknown): value is PayrollRun {
  if (typeof value !== 'object' || value === null) return false
  const run = value as Record<string, unknown>
  return typeof run.id === 'string' &&
    typeof run.month === 'number' &&
    typeof run.year === 'number' &&
    typeof run.status === 'string' &&
    typeof run.payslipCount === 'number'
}

function messageFrom(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message || response?.data?.error || 'Request failed. Please try again.'
  }
  return error instanceof Error ? error.message : 'Unable to load payroll exports.'
}

export default function PayrollRunExports() {
  const [runs, setRuns] = useState<PayrollRun[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')

  async function loadRuns() {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/payroll/runs')
      setRuns(Array.isArray(response.data) ? response.data.filter(isPayrollRun) : [])
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }

  async function download(run: PayrollRun, format: 'csv' | 'xlsx' | 'pdf') {
    const key = `${run.id}-${format}`
    setBusy(key)
    setError('')
    try {
      const response = await api.get(
        `/payroll/runs/${encodeURIComponent(run.id)}/export.${format}`,
        { responseType: 'blob' },
      )
      const url = URL.createObjectURL(response.data as Blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `payroll-run-${run.year}-${String(run.month).padStart(2, '0')}.${format}`
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setBusy('')
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadRuns(), 0)
    return () => window.clearTimeout(timer)
  }, [])

  return (
    <section className="payroll-exports">
      <div className="payroll-exports-heading">
        <div>
          <h3>Payroll run exports</h3>
          <p>Download period reports as an Excel workbook, CSV, or PDF.</p>
        </div>
        <button type="button" className="refresh-button" onClick={() => void loadRuns()} disabled={loading}>
          {loading ? <LoaderCircle className="spin" size={16} /> : 'Refresh'}
        </button>
      </div>

      {error && <div className="action-error">{error}</div>}
      {loading ? (
        <div className="friendly-empty">Loading payroll runs...</div>
      ) : runs.length === 0 ? (
        <div className="friendly-empty">No payroll runs are available to export.</div>
      ) : (
        <div className="payroll-run-list">
          {runs.map(run => (
            <article className="payroll-run-row" key={run.id}>
              <div>
                <strong>{new Date(run.year, run.month - 1).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</strong>
                <span>{run.payslipCount} payslip(s) · {run.status.replaceAll('_', ' ')}</span>
              </div>
              <div className="payroll-run-actions">
                <button type="button" onClick={() => void download(run, 'csv')} disabled={busy !== ''}>
                  {busy === `${run.id}-csv` ? <LoaderCircle className="spin" size={15} /> : <Download size={15} />}
                  CSV
                </button>
                <button type="button" onClick={() => void download(run, 'xlsx')} disabled={busy !== ''}>
                  {busy === `${run.id}-xlsx` ? <LoaderCircle className="spin" size={15} /> : <FileSpreadsheet size={15} />}
                  XLSX
                </button>
                <button type="button" onClick={() => void download(run, 'pdf')} disabled={busy !== ''}>
                  {busy === `${run.id}-pdf` ? <LoaderCircle className="spin" size={15} /> : <FileText size={15} />}
                  PDF
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
