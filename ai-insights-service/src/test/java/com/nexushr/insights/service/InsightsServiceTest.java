package com.nexushr.insights.service;

import com.nexushr.insights.dto.*;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InsightsServiceTest {
    private final InsightsService service = new InsightsService();

    @Test
    void attritionReturnsExplainableHighRiskIndicators() {
        var request = new AttritionRequest(
            "person@example.com", 70.0, 30.0, 2.0, 30, 7, 30
        );

        Map<String, Object> result = service.attrition(request);

        assertEquals("HIGH", result.get("riskLevel"));
        assertEquals(100, result.get("riskScore"));
        assertFalse(((java.util.List<?>) result.get("factors")).isEmpty());
    }

    @Test
    void skillGapIdentifiesMissingSkillsCaseInsensitively() {
        var request = new SkillGapRequest(
            "person@example.com",
            "Java Developer",
            Set.of("Java", "SQL"),
            Set.of("java", "SQL", "Spring Boot")
        );

        Map<String, Object> result = service.skillGaps(request);

        assertEquals(67, result.get("coveragePercentage"));
        assertEquals(1, ((java.util.List<?>) result.get("skillGaps")).size());
    }

    @Test
    void engagementScoreIsBoundedAndClassified() {
        var request = new EngagementRequest(
            "person@example.com", 90.0, 4.0, 4.0, 80.0
        );

        Map<String, Object> result = service.engagement(request);

        assertEquals(83, result.get("engagementScore"));
        assertEquals("HIGH", result.get("engagementLevel"));
    }
}