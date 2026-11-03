package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO leave_balances
            (id, employee_email, leave_type_id, balance_year, granted_days, used_days, updated_at)
        VALUES
            (gen_random_uuid(), lower(:email), :typeId, :year, :allowance, 0, CURRENT_TIMESTAMP)
        ON CONFLICT (employee_email, leave_type_id, balance_year) DO NOTHING
        """, nativeQuery = true)
    void insertIfMissing(
        @Param("email") String email,
        @Param("typeId") UUID typeId,
        @Param("year") int year,
        @Param("allowance") int allowance
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select balance from LeaveBalance balance
        where lower(balance.employeeEmail) = lower(:email)
          and balance.leaveTypeId = :typeId and balance.year = :year
        """)
    Optional<LeaveBalance> findForUpdate(
        @Param("email") String email,
        @Param("typeId") UUID typeId,
        @Param("year") int year
    );

    List<LeaveBalance> findByEmployeeEmailIgnoreCaseAndYearOrderByLeaveTypeId(
        String employeeEmail, int year
    );

    @Modifying
    @Query("""
        update LeaveBalance balance set balance.grantedDays = :allowance
        where balance.leaveTypeId = :typeId and balance.usedDays <= :allowance
        """)
    int updateGrant(@Param("typeId") UUID typeId, @Param("allowance") int allowance);

    long countByLeaveTypeId(UUID leaveTypeId);
}
