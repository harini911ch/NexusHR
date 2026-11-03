import { useEffect, useMemo, useState } from 'react'
import { Download, FileUp, LoaderCircle, ShieldCheck } from 'lucide-react'
import { api } from './lib/api'

type EmployeeOption = { id: string; fullName: string; employeeCode: string }
type EmployeeDocument = {
  id: string
  employeeId: string
  fileName: string
  contentType: string
  fileSize: number
  uploadedAt: string
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

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function EmployeeDocuments({ role }: { role: string }) {
  const isHr = ['SUPER_ADMIN', 'HR_ADMIN', 'HR_MANAGER'].includes(role)
  const [employees, setEmployees] = useState<EmployeeOption[]>([])
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(isHr ? '' : 'me')
  const [documents, setDocuments] = useState<EmployeeDocument[]>([])
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const documentsPath = useMemo(() => {
    if (isHr && !selectedEmployeeId) return null
    return selectedEmployeeId === 'me'
      ? '/employees/me/documents'
      : `/employees/${encodeURIComponent(selectedEmployeeId)}/documents`
  }, [isHr, selectedEmployeeId])

  useEffect(() => {
    if (!isHr) return
    let active = true
    api.get('/employees', { params: { page: 0, size: 100, sortBy: 'fullName' } })
      .then(response => {
        const content = response.data?.content
        if (active && Array.isArray(content)) {
          const eligibleEmployees = content.filter((item: unknown): item is EmployeeOption =>
            typeof item === 'object' && item !== null &&
            'id' in item && typeof item.id === 'string' &&
            'fullName' in item && typeof item.fullName === 'string' &&
            'employeeCode' in item && typeof item.employeeCode === 'string')
          setEmployees(eligibleEmployees)
          setLoading(false)
        }
      })
      .catch(err => {
        if (active) {
          setError(getErrorMessage(err))
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [isHr])

  useEffect(() => {
    let active = true
    if (!documentsPath) {
      return () => { active = false }
    }
    api.get(documentsPath)
      .then(response => {
        if (active) {
          setDocuments(Array.isArray(response.data) ? response.data : [])
          setError('')
        }
      })
      .catch(err => {
        if (active) {
          setDocuments([])
          setError(getErrorMessage(err))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [documentsPath])

  async function upload(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) {
      setError('Select a PDF, PNG, or JPEG file to upload.')
      return
    }
    if (!documentsPath) {
      setError('Select an employee record before uploading a document.')
      return
    }

    setBusy(true)
    setError('')
    setMessage('')
    const body = new FormData()
    body.append('file', file)
    try {
      await api.post(documentsPath, body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setFile(null)
      const input = document.getElementById('employee-document-file')
      if (input instanceof HTMLInputElement) input.value = ''
      setMessage('Document uploaded securely.')
      const response = await api.get(documentsPath)
      setDocuments(Array.isArray(response.data) ? response.data : [])
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function download(item: EmployeeDocument) {
    setBusy(true)
    setError('')
    try {
      const downloadPath = `${documentsPath}/${encodeURIComponent(item.id)}/download`
      const response = await api.get(downloadPath, { responseType: 'blob' })
      const objectUrl = URL.createObjectURL(response.data as Blob)
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = item.fileName
      link.click()
      URL.revokeObjectURL(objectUrl)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="employee-documents" aria-labelledby="employee-documents-heading">
      <div className="employee-documents-heading">
        <div>
          <h2 id="employee-documents-heading">Secure employee documents</h2>
          <p>Private files are limited to the employee and HR administrators. PDF, PNG, and JPEG; maximum 10 MB.</p>
        </div>
        <ShieldCheck size={21} aria-hidden="true" />
      </div>

      {isHr && <label className="document-employee-select">
        Employee record
        <select
          value={selectedEmployeeId}
          onChange={event => {
            setSelectedEmployeeId(event.target.value)
            setDocuments([])
            setLoading(true)
            setError('')
            setMessage('')
          }}
        >
          <option value="" disabled>Select an employee</option>
          {employees.map(employee => (
            <option value={employee.id} key={employee.id}>
              {employee.fullName} · {employee.employeeCode}
            </option>
          ))}
        </select>
      </label>}

      <form className="employee-document-upload" onSubmit={event => void upload(event)}>
        <label htmlFor="employee-document-file">Choose a document</label>
        <input
          id="employee-document-file"
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
          onChange={event => setFile(event.target.files?.[0] || null)}
        />
        <button type="submit" className="primary-button" disabled={busy || !file || !documentsPath}>
          {busy ? <LoaderCircle className="spin" size={17} /> : <FileUp size={17} />}
          Upload document
        </button>
      </form>

      {message && <p className="document-message" role="status">{message}</p>}
      {error && <p className="document-error" role="alert">{error}</p>}

      {loading ? (
        <div className="documents-loading"><LoaderCircle className="spin" size={18} /> Loading documents...</div>
      ) : documents.length === 0 ? (
        <div className="documents-empty">No documents have been uploaded for this employee.</div>
      ) : (
        <ul className="employee-document-list">
          {documents.map(item => (
            <li key={item.id}>
              <div>
                <strong>{item.fileName}</strong>
                <span>{formatSize(item.fileSize)} · {new Date(item.uploadedAt).toLocaleDateString()}</span>
              </div>
              <button type="button" className="document-download" onClick={() => void download(item)} disabled={busy}>
                <Download size={16} /> Download
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
