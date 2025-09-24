package org.jaiswarsecurities.chipmunkgenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaiswarsecurities.chipmunkgenerator.service.ChipmunkFileGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the Chipmunk File Generator.
 * Generates production-ready chipmunk files containing Trade, Market Data, and FX events
 * in Protobuf format and uploads them to MinIO.
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {
    "org.jaiswarsecurities.chipmunkgenerator",
    "org.jaiswarsecurities.awsconfig"
})
@RequiredArgsConstructor
public class ChipmunkGeneratorApplication implements CommandLineRunner {

    private final ChipmunkFileGenerator chipmunkFileGenerator;

    public static void main(String[] args) {
        SpringApplication.run(ChipmunkGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Chipmunk File Generator...");
        
        // Parse command line arguments if needed
        String fileName = args.length > 0 ? args[0] : "sample-events-" + System.currentTimeMillis() + ".chip";
        int eventCount = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
        
        log.info("Generating chipmunk file: {} with {} events", fileName, eventCount);
        
        String uploadedPath = chipmunkFileGenerator.generateAndUploadChipmunkFile(fileName, eventCount);
        
        log.info("Successfully generated and uploaded chipmunk file to: {}", uploadedPath);
        log.info("Chipmunk File Generator completed successfully!");
    }
}