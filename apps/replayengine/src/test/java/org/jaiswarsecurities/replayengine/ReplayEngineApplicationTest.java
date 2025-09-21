package org.jaiswarsecurities.replayengine;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.jaiswarsecurities.replayengine.service.ChipmunkReader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test for the Replay Engine Application.
 * Verifies that the Spring Boot context loads successfully.
 */
@SpringBootTest(
    classes = ReplayEngineApplication.class,
    properties = {
        "replay.source.type=local-file",
        "replay.source.local.file-path=/tmp/test-file.json",
        "replay.checkpoint.enabled=false",
        "management.endpoints.web.exposure.include=health,info"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
class ReplayEngineApplicationTest {

    @MockBean
    private KafkaProducer<String, String> kafkaProducer;
    
    @MockBean
    private ChipmunkReader chipmunkReader;

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context
        // can be loaded successfully with all beans configured.
        // Both KafkaProducer and ChipmunkReader are mocked to avoid dependency issues in test environment.
    }
}
