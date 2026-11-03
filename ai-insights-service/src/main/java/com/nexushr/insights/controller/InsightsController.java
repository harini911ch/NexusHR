package com.nexushr.insights.controller;

import com.nexushr.insights.dto.*;
import com.nexushr.insights.service.InsightsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER')")
public class InsightsController {
    private final InsightsService service;

    public InsightsController(InsightsService service) {
        this.service = service;
    }

    @PostMapping("/attrition")
    public Map<String, Object> attrition(@Valid @RequestBody AttritionRequest request) {
        return service.attrition(request);
    }

    @PostMapping("/skill-gaps")
    public Map<String, Object> skillGaps(@Valid @RequestBody SkillGapRequest request) {
        return service.skillGaps(request);
    }

    @PostMapping("/engagement")
    public Map<String, Object> engagement(@Valid @RequestBody EngagementRequest request) {
        return service.engagement(request);
    }
}