import {
  Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'

type StatusCount = { status: string; count: number }
type DepartmentCount = { department: string; count: number }

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function statusCounts(value: unknown): StatusCount[] {
  if (!Array.isArray(value)) return []
  return value.filter((row): row is StatusCount =>
    isRecord(row) &&
    typeof row.status === 'string' &&
    typeof row.count === 'number')
}

function departmentCounts(value: unknown): DepartmentCount[] {
  if (!Array.isArray(value)) return []
  return value.filter((row): row is DepartmentCount =>
    isRecord(row) &&
    typeof row.department === 'string' &&
    typeof row.count === 'number')
}

export default function WorkforceCharts({ value }: { value: unknown }) {
  if (!isRecord(value)) return null
  const statuses = statusCounts(value.statusBreakdown)
  const departments = departmentCounts(value.departmentBreakdown)

  return (
    <div className="workforce-chart-grid">
      <section className="workforce-chart-panel">
        <h3>Employees by status</h3>
        {statuses.length === 0
          ? <div className="friendly-empty">No workforce status data yet.</div>
          : <ResponsiveContainer width="100%" height={240}>
              <BarChart data={statuses} margin={{ top: 8, right: 12, bottom: 4, left: -16 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="status" tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#3268d5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>}
      </section>
      <section className="workforce-chart-panel">
        <h3>Employees by department</h3>
        {departments.length === 0
          ? <div className="friendly-empty">No department data yet.</div>
          : <ResponsiveContainer width="100%" height={240}>
              <BarChart data={departments} margin={{ top: 8, right: 12, bottom: 4, left: -16 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="department" tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#4a9c88" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>}
      </section>
    </div>
  )
}
