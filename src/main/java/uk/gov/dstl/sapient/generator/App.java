package uk.gov.dstl.sapient.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.dstl.sapient.generator.config.GeneratorConfig;
import uk.gov.dstl.sapient.generator.config.MovementConfig;
import uk.gov.dstl.sapient.generator.config.ObjectConfig;
import uk.gov.dstl.sapient.generator.engine.*;
import uk.gov.dstl.sapient.generator.generator.DetectionReportGenerator;
import uk.gov.dstl.sapient.generator.generator.IdGenerator;
import uk.gov.dstl.sapient.generator.output.JsonFileWriter;
import uk.gov.dstl.sapientmsg.bsiflex335v2.SapientMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.*;

public class App {

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "./config.json";

        try {
            GeneratorConfig config = loadConfig(configPath);
            run(config);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static GeneratorConfig loadConfig(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GeneratorConfig config = mapper.readValue(new File(path), GeneratorConfig.class);
        System.out.println("Loaded configuration from: " + path);
        System.out.println("  Node ID: " + config.getNodeId());
        System.out.println("  Output directory: " + config.getOutputDirectory());
        System.out.println("  Objects: " + config.getObjects().size());
        return config;
    }

    private static void run(GeneratorConfig config) throws IOException, InterruptedException {
        if (config.getObjects() == null || config.getObjects().isEmpty()) {
            throw new IllegalArgumentException("Configuration must define at least one object");
        }

        JsonFileWriter writer = new JsonFileWriter(Path.of(config.getOutputDirectory()));
        writer.ensureDirectoryExists();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(config.getObjects().size());
        long startTimeMs = System.currentTimeMillis();

        System.out.println("Starting detection generation...");

        for (ObjectConfig objConfig : config.getObjects()) {
            MovementEngine engine = createMovementEngine(objConfig.getMovement());
            DetectionReportGenerator generator = new DetectionReportGenerator(config, objConfig, engine);

            if (objConfig.getUpdateFrequencyHz() <= 0) {
                throw new IllegalArgumentException(
                        "updateFrequencyHz must be positive for object: " + objConfig.getName());
            }
            long periodMs = (long) (1000.0 / objConfig.getUpdateFrequencyHz());

            executor.scheduleAtFixedRate(() -> {
                try {
                    double elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0;
                    SapientMessage message = generator.generate(elapsedSeconds);

                    String timestamp = Instant.now().toString();
                    String reportId = message.getDetectionReport().getReportId();
                    writer.write(message, timestamp, generator.getObjectName(), reportId);

                    System.out.printf("[%s] Generated detection for %s (report: %s)%n",
                            timestamp, generator.getObjectName(), reportId);
                } catch (Exception e) {
                    System.err.printf("Error generating detection for %s: %s%n",
                            generator.getObjectName(), e.getMessage());
                }
            }, 0, periodMs, TimeUnit.MILLISECONDS);

            System.out.printf("  Scheduled %s: pattern=%s, frequency=%.1f Hz, period=%d ms%n",
                    objConfig.getName(), objConfig.getMovement().getPattern(),
                    objConfig.getUpdateFrequencyHz(), periodMs);
        }

        // Shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Stopped.");
        }));

        System.out.println("Running. Press Ctrl+C to stop.");

        // Block main thread
        Thread.currentThread().join();
    }

    private static MovementEngine createMovementEngine(MovementConfig movementConfig) {
        return switch (movementConfig.getPattern().toUpperCase()) {
            case "ORBIT" -> new OrbitMovement(movementConfig);
            case "FIGURE_8" -> new Figure8Movement(movementConfig);
            case "STATIONARY" -> new StationaryMovement(movementConfig);
            default -> throw new IllegalArgumentException(
                    "Unknown movement pattern: " + movementConfig.getPattern());
        };
    }
}
