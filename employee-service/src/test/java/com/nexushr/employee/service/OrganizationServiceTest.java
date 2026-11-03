package com.nexushr.employee.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.nexushr.employee.dto.DepartmentRequest;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.DesignationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private DesignationRepository designationRepository;

    @Test
    void omittedActiveValueDoesNotReactivateInactiveDepartment() {
        OrganizationService service =
            new OrganizationService(departmentRepository, designationRepository);
        Department department = new Department("Engineering", null, null, null);
        department.update("Engineering", null, null, null, false);
        UUID id = department.getId();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(departmentRepository.findByNameIgnoreCase("Engineering"))
            .thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        var response = service.updateDepartment(
            id, new DepartmentRequest(" Engineering ", null, null, null, null)
        );

        assertFalse(response.active());
    }
}
