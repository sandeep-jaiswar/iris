package org.jaiswarsecurities.replayengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify how Spring Boot binds property values to enums.
 */
class PropertyBindingTest {

    @Test
    void testEnumPropertyBinding() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("replay.source.type", "minio");
        
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        
        ReplayProperties replayProperties = binder.bind("replay", ReplayProperties.class).get();
        
        assertEquals(ReplayProperties.Source.SourceType.MINIO, replayProperties.getSource().getType());
    }
    
    @Test
    void testEnumPropertyBindingWithLocalFile() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("replay.source.type", "local-file");
        
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        
        ReplayProperties replayProperties = binder.bind("replay", ReplayProperties.class).get();
        
        // This should work with Spring Boot's flexible binding
        assertEquals(ReplayProperties.Source.SourceType.LOCAL_FILE, replayProperties.getSource().getType());
    }
}