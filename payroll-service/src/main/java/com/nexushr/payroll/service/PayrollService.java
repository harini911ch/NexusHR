package com.nexushr.payroll.service;

import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.payroll.dto.*;
import com.nexushr.payroll.entity.PayrollRecord;
import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.SalaryStructure;
import com.nexushr.payroll.repository.PayrollRecordRepository;
import com.nexushr.payroll.repository.PayrollRunRepository;
import com.nexushr.payroll.repository.SalaryStructureRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PayrollService {

    private final SalaryStructureRepository salaryRepository;
    private final PayrollRecordRepository payrollRepository;
    private final PayrollRunRepository runRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PayrollService(
        SalaryStructureRepository salaryRepository,
        PayrollRecordRepository payrollRepository,
        PayrollRunRepository runRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.salaryRepository = salaryRepository;
        this.payrollRepository = payrollRepository;
        this.runRepository = runRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SalaryStructure createSalaryStructure(SalaryStructureRequest request) {
        if (salaryRepository.findByEmployeeId(request.employeeId()).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Salary structure already exists for this employee");
        }

        SalaryStructure salary = new SalaryStructure();
        salary.setEmployeeId(request.employeeId());
        salary.setEmployeeCode(request.employeeCode().trim());
        salary.setEmployeeEmail(normalizeEmail(request.employeeEmail()));
        salary.setBasicSalary(money(request.basicSalary()));
        salary.setHousingAllowance(money(request.housingAllowance()));
        salary.setTransportAllowance(money(request.transportAllowance()));
        salary.setOtherAllowance(money(request.otherAllowance()));
        salary.setTaxPercentage(request.taxPercentage().setScale(2, RoundingMode.HALF_UP));
        salary.setOtherDeduction(money(request.otherDeduction()));
        salary.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        salary.setEffectiveFrom(request.effectiveFrom());

        return salaryRepository.save(salary);
    }

    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> salaryStructures() {
        return salaryRepository.findAll().stream()
            .map(SalaryStructureResponse::from)
            .toList();
    }

    @Transactional
    public PayrollRecord process(PayrollProcessRequest request, String actor) {
        SalaryStructure salary = salaryRepository.findByEmployeeId(request.employeeId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Salary structure not found"));
        ensureNoExistingPayslip(salary.getEmployeeId(), request.month(), request.year());
        if (salary.getEffectiveFrom().isAfter(
            LocalDate.of(request.year(), request.month(), 1).withDayOfMonth(
                LocalDate.of(request.year(), request.month(), 1).lengthOfMonth()
            )
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Salary structure is not effective for this period");
        }
        PayrollRecord record = calculate(salary, request.month(), request.year(), null);
        record.setProcessedBy(normalizeEmail(actor));
        return payrollRepository.save(record);
    }

    @Transactional
    public PayrollRunResponse createRun(PayrollRunRequest request, String actor) {
        if (runRepository.existsByMonthAndYear(request.month(), request.year())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A payroll run already exists for this period");
        }

        LocalDate periodEnd = LocalDate.of(request.year(), request.month(), 1)
            .withDayOfMonth(LocalDate.of(request.year(), request.month(), 1).lengthOfMonth());
        List<SalaryStructure> effectiveSalaries = salaryRepository.findAll().stream()
            .filter(salary -> !salary.getEffectiveFrom().isAfter(periodEnd))
            .toList();
        if (effectiveSalaries.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "No effective salary structures exist for this period");
        }
        for (SalaryStructure salary : effectiveSalaries) {
            ensureNoExistingPayslip(salary.getEmployeeId(), request.month(), request.year());
        }

        PayrollRun run = runRepository.save(new PayrollRun(
            request.month(), request.year(), normalizeEmail(actor)
        ));
        List<PayrollRecord> records = effectiveSalaries.stream()
            .map(salary -> calculate(salary, request.month(), request.year(), run.getId()))
            .toList();
        payrollRepository.saveAll(records);
        return PayrollRunResponse.from(run, records.size());
    }

    @Transactional
    public PayrollRunResponse approveRun(UUID id, String reviewerEmail) {
        PayrollRun run = runRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Payroll run not found"));
        if (run.getCreatedBy().equalsIgnoreCase(normalizeEmail(reviewerEmail))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "The payroll run creator cannot approve the same run");
        }
        List<PayrollRecord> records = payrollRepository
            .findByPayrollRunIdOrderByEmployeeCodeAsc(id);
        if (records.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Payroll run has no payslips to approve");
        }
        try {
            run.approve(normalizeEmail(reviewerEmail));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        records.forEach(record -> record.setPaymentStatus("APPROVED"));
        payrollRepository.saveAll(records);
        PayrollRun saved = runRepository.save(run);
        String period = java.time.YearMonth.of(run.getYear(), run.getMonth()).toString();
        records.forEach(record -> eventPublisher.publishEvent(new NotificationEvent(
            record.getEmployeeEmail(),
            "Payroll approved",
            "Your payroll for " + period + " has been approved.",
            "PAYROLL",
            "PAYROLL_RUN",
            run.getId().toString()
        )));
        return PayrollRunResponse.from(saved, records.size());
    }

    @Transactional
    public PayslipResponse approvePayslip(UUID id, String reviewerEmail) {
        PayrollRecord record = payrollRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Payslip not found"));
        if (record.getPayrollRunId() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Payslips belonging to a payroll run must be approved together");
        }
        if (!"PENDING_APPROVAL".equals(record.getPaymentStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Only pending payslips can be approved");
        }
        if (record.getProcessedBy() != null &&
            record.getProcessedBy().equalsIgnoreCase(normalizeEmail(reviewerEmail))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "The payroll processor cannot approve the same payslip");
        }
        record.setPaymentStatus("APPROVED");
        return PayslipResponse.from(payrollRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> runs() {
        return runRepository.findAll().stream()
            .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
            .map(run -> PayrollRunResponse.from(
                run, payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(run.getId()).size()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getRun(UUID id) {
        PayrollRun run = findRun(id);
        int count = payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(id).size();
        return PayrollRunResponse.from(run, count);
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getRunPayslips(UUID id) {
        findRun(id);
        return payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(id).stream()
            .map(PayslipResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getMyPayslips(String email) {
        return payrollRepository
            .findByEmployeeEmailIgnoreCaseOrderByPayrollYearDescPayrollMonthDesc(
                normalizeEmail(email)
            ).stream().map(PayslipResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(UUID id) {
        return payrollRepository.findById(id)
            .map(PayslipResponse::from)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Payslip not found"));
    }

    @Transactional(readOnly = true)
    public String exportRunCsv(UUID id) {
        findRun(id);
        StringBuilder csv = new StringBuilder(
            "employee_code,employee_email,month,year,basic_salary,allowances,gross_salary,"
                + "tax_deduction,other_deduction,total_deductions,net_salary,currency,status\r\n"
        );
        payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(id).forEach(record ->
            csv.append(csv(record.getEmployeeCode())).append(',')
                .append(csv(record.getEmployeeEmail())).append(',')
                .append(record.getPayrollMonth()).append(',')
                .append(record.getPayrollYear()).append(',')
                .append(record.getBasicSalary()).append(',')
                .append(record.getTotalAllowances()).append(',')
                .append(record.getGrossSalary()).append(',')
                .append(record.getTaxDeduction()).append(',')
                .append(record.getOtherDeduction()).append(',')
                .append(record.getTotalDeductions()).append(',')
                .append(record.getNetSalary()).append(',')
                .append(csv(record.getCurrency())).append(',')
                .append(csv(record.getPaymentStatus())).append("\r\n")
        );
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportRunXlsx(UUID id) {
        findRun(id);
        List<PayrollRecord> records =
            payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(id);
            SXSSFWorkbook workbook = new SXSSFWorkbook(100);
            try (workbook; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll");
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            String[] headers = {
                "Employee code", "Employee email", "Month", "Year", "Basic salary",
                "Allowances", "Gross salary", "Tax deduction", "Other deduction",
                "Total deductions", "Net salary", "Currency", "Status"
            };
            Row header = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }

            for (int index = 0; index < records.size(); index++) {
                PayrollRecord record = records.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(record.getEmployeeCode());
                row.createCell(1).setCellValue(record.getEmployeeEmail());
                row.createCell(2).setCellValue(record.getPayrollMonth());
                row.createCell(3).setCellValue(record.getPayrollYear());
                setMoneyCell(row, 4, record.getBasicSalary(), moneyStyle);
                setMoneyCell(row, 5, record.getTotalAllowances(), moneyStyle);
                setMoneyCell(row, 6, record.getGrossSalary(), moneyStyle);
                setMoneyCell(row, 7, record.getTaxDeduction(), moneyStyle);
                setMoneyCell(row, 8, record.getOtherDeduction(), moneyStyle);
                setMoneyCell(row, 9, record.getTotalDeductions(), moneyStyle);
                setMoneyCell(row, 10, record.getNetSalary(), moneyStyle);
                row.createCell(11).setCellValue(record.getCurrency());
                row.createCell(12).setCellValue(record.getPaymentStatus());
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(
                0, records.size(), 0, headers.length - 1
            ));
            int[] widths = {18, 30, 10, 10, 16, 16, 16, 16, 16, 18, 16, 12, 20};
            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, widths[column] * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the payroll XLSX export", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportRunPdf(UUID id) {
        PayrollRun run = findRun(id);
        List<PayrollRecord> records =
            payrollRepository.findByPayrollRunIdOrderByEmployeeCodeAsc(id);
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            int pageCount = Math.max(1, (records.size() + 24) / 25);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 11);
                    content.newLineAtOffset(45, 750);
                    content.showText("NexusHR Payroll Run " + run.getMonth() + "/" + run.getYear()
                        + " - " + run.getStatus());
                    content.newLineAtOffset(0, -22);
                    content.setFont(font, 8);
                    content.showText("Employee | Gross | Tax | Other deductions | Net | Currency | Status");
                    int start = pageIndex * 25;
                    int end = Math.min(start + 25, records.size());
                    for (int index = start; index < end; index++) {
                        PayrollRecord record = records.get(index);
                        content.newLineAtOffset(0, -24);
                        content.showText(pdfText(
                            record.getEmployeeCode() + " | " + record.getGrossSalary()
                                + " | " + record.getTaxDeduction() + " | "
                                + record.getOtherDeduction() + " | " + record.getNetSalary()
                                + " | " + record.getCurrency() + " | "
                                + record.getPaymentStatus()
                        ));
                    }
                    content.endText();
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the payroll PDF export", exception);
        }
    }

    private PayrollRecord calculate(
        SalaryStructure salary, int month, int year, UUID runId
    ) {
        BigDecimal allowances = money(salary.getHousingAllowance()
            .add(salary.getTransportAllowance())
            .add(salary.getOtherAllowance()));
        BigDecimal gross = money(salary.getBasicSalary().add(allowances));
        BigDecimal tax = money(gross.multiply(salary.getTaxPercentage())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        BigDecimal other = money(salary.getOtherDeduction());
        BigDecimal deductions = money(tax.add(other));
        BigDecimal net = money(gross.subtract(deductions));
        if (net.signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Deductions cannot exceed gross salary");
        }

        PayrollRecord record = new PayrollRecord();
        record.setPayrollRunId(runId);
        record.setEmployeeId(salary.getEmployeeId());
        record.setEmployeeCode(salary.getEmployeeCode());
        record.setEmployeeEmail(salary.getEmployeeEmail());
        record.setPayrollMonth(month);
        record.setPayrollYear(year);
        record.setBasicSalary(money(salary.getBasicSalary()));
        record.setTotalAllowances(allowances);
        record.setGrossSalary(gross);
        record.setTaxDeduction(tax);
        record.setOtherDeduction(other);
        record.setTotalDeductions(deductions);
        record.setNetSalary(net);
        record.setCurrency(salary.getCurrency());
        record.setPaymentStatus("PENDING_APPROVAL");
        return record;
    }

    private void ensureNoExistingPayslip(UUID employeeId, int month, int year) {
        if (payrollRepository.findByEmployeeIdAndPayrollMonthAndPayrollYear(
            employeeId, month, year
        ).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A payslip already exists for this employee and period");
        }
    }

    private PayrollRun findRun(UUID id) {
        return runRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Payroll run not found"));
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }

        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void setMoneyCell(
        Row row, int column, BigDecimal value, CellStyle style
    ) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static String pdfText(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?")
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)");
    }
}
