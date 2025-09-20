package org.jaiswarsecurities.replayengine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test for the Replay Engine Application.
 * Verifies that the Spring Boot context loads successfully.
 */
@SpringBootTest(classes = ReplayEngineApplication.class)
@ActiveProfiles("test")
class ReplayEngineApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context
        // can be loaded successfully with all beans configured
    }
}
