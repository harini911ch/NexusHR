package com.nexushr.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.nexushr.payroll.dto.PayrollProcessRequest;
import com.nexushr.payroll.dto.PayrollRunRequest;
import com.nexushr.payroll.entity.PayrollRecord;
import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.SalaryStructure;
import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.payroll.repository.PayrollRecordRepository;
import com.nexushr.payroll.repository.PayrollRunRepository;
import com.nexushr.payroll.repository.SalaryStructureRepository;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private SalaryStructureRepository salaryRepository;
    @Mock private PayrollRecordRepository payrollRepository;
    @Mock private PayrollRunRepository runRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PayrollService service;

    @BeforeEach
    void setUp() {
        service = new PayrollService(
            salaryRepository, payrollRepository, runRepository, eventPublisher
        );
    }

    @Test
    void processCalculatesTransparentRoundedAmountsAndRequiresApproval() {
        UUID employeeId = UUID.randomUUID();
        SalaryStructure salary = salary(employeeId);
        when(salaryRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(salary));
        when(payrollRepository.findByEmployeeIdAndPayrollMonthAndPayrollYear(employeeId, 5, 2026))
            .thenReturn(Optional.empty());
        when(payrollRepository.save(any(PayrollRecord.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PayrollRecord payslip = service.process(
            new PayrollProcessRequest(employeeId, 5, 2026), "payroll@example.com"
        );

        assertEquals(new BigDecimal("1120.00"), payslip.getGrossSalary());
        assertEquals(new BigDecimal("112.00"), payslip.getTaxDeduction());
        assertEquals(new BigDecimal("5.00"), payslip.getOtherDeduction());
        assertEquals(new BigDecimal("1003.00"), payslip.getNetSalary());
        assertEquals("PENDING_APPROVAL", payslip.getPaymentStatus());
        assertEquals("payroll@example.com", payslip.getProcessedBy());
    }

    @Test
    void runIncludesEffectiveSalaryAndRequiresDifferentReviewer() {
        UUID employeeId = UUID.randomUUID();
        SalaryStructure salary = salary(employeeId);
        when(runRepository.existsByMonthAndYear(5, 2026)).thenReturn(false);
        when(salaryRepository.findAll()).thenReturn(List.of(salary));
        when(payrollRepository.findByEmployeeIdAndPayrollMonthAndPayrollYear(employeeId, 5, 2026))
            .thenReturn(Optional.empty());
        when(runRepository.save(any(PayrollRun.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(payrollRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var run = service.createRun(new PayrollRunRequest(5, 2026), "payroll@example.com");
        assertEquals(1, run.payslipCount());
        assertEquals("PENDING_APPROVAL", run.status());

        assertThrows(ResponseStatusException.class,
            () -> service.approveRun(run.id(), "PAYROLL@example.com"));

        PayrollRun entity = new PayrollRun(5, 2026, "payroll@example.com");
        when(runRepository.findByIdForUpdate(run.id())).thenReturn(Optional.of(entity));
        PayrollRecord record = new PayrollRecord();
        record.setEmployeeEmail("employee@example.com");
        when(payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(run.id()))
            .thenReturn(List.of(record));
        when(runRepository.save(entity)).thenReturn(entity);
        when(payrollRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var approved = service.approveRun(run.id(), "reviewer@example.com");

        assertEquals("APPROVED", approved.status());
        assertEquals("reviewer@example.com", approved.approvedBy());
        verify(payrollRepository, times(2)).saveAll(any());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void xlsxExportContainsNativeNumericPayrollCells() throws Exception {
        UUID runId = UUID.randomUUID();
        PayrollRun run = new PayrollRun(5, 2026, "payroll@example.com");
        PayrollRecord record = new PayrollRecord();
        record.setEmployeeCode("EMP-001");
        record.setEmployeeEmail("employee@example.com");
        record.setPayrollMonth(5);
        record.setPayrollYear(2026);
        record.setBasicSalary(new BigDecimal("1000.00"));
        record.setTotalAllowances(new BigDecimal("120.00"));
        record.setGrossSalary(new BigDecimal("1120.00"));
        record.setTaxDeduction(new BigDecimal("112.00"));
        record.setOtherDeduction(new BigDecimal("5.00"));
        record.setTotalDeductions(new BigDecimal("117.00"));
        record.setNetSalary(new BigDecimal("1003.00"));
        record.setCurrency("USD");
        record.setPaymentStatus("APPROVED");
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(runId))
            .thenReturn(List.of(record));

        byte[] export = service.exportRunXlsx(runId);

        assertTrue(export.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export))) {
            var sheet = workbook.getSheet("Payroll");
            assertEquals("Employee code", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("EMP-001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(1000d, sheet.getRow(1).getCell(4).getNumericCellValue());
            assertEquals("USD", sheet.getRow(1).getCell(11).getStringCellValue());
            assertEquals("APPROVED", sheet.getRow(1).getCell(12).getStringCellValue());
            assertTrue(sheet.getPaneInformation().isFreezePane());
        }
    }

    private static SalaryStructure salary(UUID employeeId) {
        SalaryStructure salary = new SalaryStructure();
        salary.setEmployeeId(employeeId);
        salary.setEmployeeCode("EMP-001");
        salary.setEmployeeEmail("employee@example.com");
        salary.setBasicSalary(new BigDecimal("1000"));
        salary.setHousingAllowance(new BigDecimal("50"));
        salary.setTransportAllowance(new BigDecimal("25"));
        salary.setOtherAllowance(new BigDecimal("45"));
        salary.setTaxPercentage(new BigDecimal("10"));
        salary.setOtherDeduction(new BigDecimal("5"));
        salary.setCurrency("USD");
        salary.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return salary;
    }
}
