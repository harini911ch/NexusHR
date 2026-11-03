package com.nexushr.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsWithCookieSecurityFiltersConfigured() {
        assertNotNull(applicationContext.getBean(CookieAuthenticationFilter.class));
        assertNotNull(applicationContext.getBean(CookieCsrfOriginFilter.class));
    }
}
