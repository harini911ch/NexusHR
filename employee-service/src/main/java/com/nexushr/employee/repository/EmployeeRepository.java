package com.nexushr.employee.repository;

import com.nexushr.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>,
        JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee where employee.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") UUID id);
    boolean existsByEmailIgnoreCase(String email);
    List<Employee> findByEmploymentStatus(String employmentStatus);
    Page<Employee> findByManagerEmailIgnoreCase(String managerEmail, Pageable pageable);

    @Query("""
        select e.employmentStatus as status, count(e) as total
        from Employee e group by e.employmentStatus order by e.employmentStatus
        """)
    List<StatusCount> countByStatus();

    @Query("""
        select e.employmentStatus as status, count(e) as total
        from Employee e
        where lower(e.managerEmail) = lower(:managerEmail)
        group by e.employmentStatus order by e.employmentStatus
        """)
    List<StatusCount> countDirectReportsByStatus(@Param("managerEmail") String managerEmail);

    @Query("""
        select e.employmentStatus as status, count(e) as total
        from Employee e where e.departmentId in :departmentIds
        group by e.employmentStatus order by e.employmentStatus
        """)
    List<StatusCount> countByDepartmentsStatus(
        @Param("departmentIds") List<UUID> departmentIds
    );

    @Query("""
        select e.departmentId as departmentId, count(e) as total
        from Employee e group by e.departmentId order by e.departmentId
        """)
    List<DepartmentCount> countByDepartment();

    @Query("""
        select e.departmentId as departmentId, count(e) as total
        from Employee e
        where lower(e.managerEmail) = lower(:managerEmail)
        group by e.departmentId order by e.departmentId
        """)
    List<DepartmentCount> countDirectReportsByDepartment(
        @Param("managerEmail") String managerEmail
    );

    @Query("""
        select e.departmentId as departmentId, count(e) as total
        from Employee e where e.departmentId in :departmentIds
        group by e.departmentId order by e.departmentId
        """)
    List<DepartmentCount> countByDepartments(
        @Param("departmentIds") List<UUID> departmentIds
    );

    interface StatusCount {
        String getStatus();
        long getTotal();
    }

    interface DepartmentCount {
        UUID getDepartmentId();
        long getTotal();
    }
}