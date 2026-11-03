package com.nexushr.employee.dto;

import java.util.List;

public record EmployeeAnalyticsResponse(
    long totalEmployees,
    List<StatusCount> statusBreakdown,
    List<DepartmentCount> departmentBreakdown
) {
    public record StatusCount(String status, long count) {}
    public record DepartmentCount(String department, long count) {}
}
