package com.nexushr.employee.service;

import com.nexushr.employee.dto.*;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.entity.Designation;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.DesignationRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    public OrganizationService(
        DepartmentRepository departmentRepository,
        DesignationRepository designationRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> departments(boolean includeInactive) {
        return departmentRepository.findAll(Sort.by("name")).stream()
            .filter(department -> includeInactive || department.isActive())
            .map(DepartmentResponse::from)
            .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Department already exists"
            );
        }
        Department parent = request.parentDepartmentId() == null
            ? null : findDepartment(request.parentDepartmentId());
        if (parent != null && !parent.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Parent department must be active"
            );
        }
        Department department = new Department(
            name, trimToNull(request.description()), normalizeEmail(request.managerEmail()),
            parent == null ? null : parent.getId()
        );
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        Department department = findDepartment(id);
        String name = request.name().trim();
        departmentRepository.findByNameIgnoreCase(name)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Department already exists"
                );
            });

        UUID parentId = request.parentDepartmentId();
        if (id.equals(parentId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A department cannot be its own parent"
            );
        }
        validateNoCycle(id, parentId);
        Department parent = parentId == null ? null : findDepartment(parentId);
        if (parent != null && !parent.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Parent department must be active"
            );
        }

        department.update(
            name,
            trimToNull(request.description()),
            normalizeEmail(request.managerEmail()),
            parentId,
            request.active() == null ? department.isActive() : request.active()
        );
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> designations(UUID departmentId, boolean includeInactive) {
        return designationRepository.findAll(Sort.by("title")).stream()
            .filter(designation -> departmentId == null ||
                designation.getDepartmentId().equals(departmentId))
            .filter(designation -> includeInactive || designation.isActive())
            .map(DesignationResponse::from)
            .toList();
    }

    @Transactional
    public DesignationResponse createDesignation(DesignationRequest request) {
        Department department = findDepartment(request.departmentId());
        if (!department.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Designation department must be active"
            );
        }
        if (designationRepository.findByTitleIgnoreCaseAndDepartmentId(
            request.title().trim(), request.departmentId()).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Designation already exists in this department"
            );
        }
        Designation designation = new Designation(
            request.title().trim(),
            department.getId(),
            trimToNull(request.description())
        );
        return DesignationResponse.from(designationRepository.save(designation));
    }

    @Transactional
    public DesignationResponse updateDesignation(UUID id, DesignationRequest request) {
        Designation designation = designationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Designation not found"
            ));
        Department department = findDepartment(request.departmentId());
        designationRepository.findByTitleIgnoreCaseAndDepartmentId(
            request.title().trim(), department.getId())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Designation already exists in this department"
                );
            });
        designation.update(
            request.title().trim(),
            department.getId(),
            trimToNull(request.description()),
            request.active() == null ? designation.isActive() : request.active()
        );
        return DesignationResponse.from(designationRepository.save(designation));
    }

    private void validateNoCycle(UUID departmentId, UUID candidateParentId) {
        Set<UUID> visited = new HashSet<>();
        UUID cursor = candidateParentId;
        while (cursor != null) {
            if (!visited.add(cursor) || cursor.equals(departmentId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Department hierarchy cannot contain a cycle"
                );
            }
            cursor = findDepartment(cursor).getParentDepartmentId();
        }
    }

    private Department findDepartment(UUID id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Department not found"
            ));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
