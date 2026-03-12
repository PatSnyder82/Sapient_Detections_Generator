package uk.gov.dstl.sapient.generator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import uk.gov.dstl.sapient.generator.config.GeneratorConfig;
import uk.gov.dstl.sapient.generator.config.MovementConfig;
import uk.gov.dstl.sapient.generator.config.ObjectConfig;
import uk.gov.dstl.sapient.generator.engine.*;
import uk.gov.dstl.sapient.generator.generator.DetectionReportGenerator;
import uk.gov.dstl.sapientmsg.bsiflex335v2.SapientMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Tags({"sapient", "detection", "BSI Flex 335", "simulation", "protobuf"})
@CapabilityDescription("Generates simulated SAPIENT DetectionReport messages (BSI Flex 335 v2.0) as JSON FlowFiles. "
        + "Configure tracked objects with movement patterns (ORBIT, FIGURE_8, STATIONARY) via a JSON configuration property. "
        + "Each trigger generates one detection per configured object.")
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
public class SapientDetectionProcessor extends AbstractProcessor {

    static final PropertyDescriptor CONFIGURATION_JSON = new PropertyDescriptor.Builder()
            .name("Configuration JSON")
            .displayName("Configuration JSON")
            .description("JSON configuration defining nodeId, destinationId, utmZone, and tracked objects "
                    + "with their movement patterns and classifications. See README for schema.")
            .required(true)
            .addValidator(Validator.VALID) // validated at @OnScheduled for better error messages
            .build();

    static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully generated SAPIENT DetectionReport JSON")
            .build();

    static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to generate detection report")
            .build();

    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private volatile List<DetectionReportGenerator> generators;
    private volatile long startTimeMs;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(CONFIGURATION_JSON);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(REL_SUCCESS, REL_FAILURE);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        String configJson = context.getProperty(CONFIGURATION_JSON).getValue();
        try {
            GeneratorConfig config = OBJECT_MAPPER.readValue(configJson, GeneratorConfig.class);
            validateConfig(config);

            List<DetectionReportGenerator> gens = new ArrayList<>();
            for (ObjectConfig objConfig : config.getObjects()) {
                MovementEngine engine = createMovementEngine(objConfig.getMovement());
                gens.add(new DetectionReportGenerator(config, objConfig, engine));
            }
            this.generators = gens;
            this.startTimeMs = System.currentTimeMillis();
            getLogger().info("Initialized {} object generators for node {}",
                    gens.size(), config.getNodeId());
        } catch (Exception e) {
            throw new ProcessException("Failed to parse Configuration JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        double elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0;

        for (DetectionReportGenerator generator : generators) {
            FlowFile flowFile = null;
            try {
                SapientMessage message = generator.generate(elapsedSeconds);
                String json = JSON_PRINTER.print(message);
                String reportId = message.getDetectionReport().getReportId();

                flowFile = session.create();
                flowFile = session.write(flowFile, out ->
                        out.write(json.getBytes(StandardCharsets.UTF_8)));
                flowFile = session.putAttribute(flowFile, "mime.type", "application/json");
                flowFile = session.putAttribute(flowFile, "sapient.object.name", generator.getObjectName());
                flowFile = session.putAttribute(flowFile, "sapient.object.id", generator.getObjectId());
                flowFile = session.putAttribute(flowFile, "sapient.report.id", reportId);

                session.transfer(flowFile, REL_SUCCESS);
            } catch (Exception e) {
                getLogger().error("Failed to generate detection for {}: {}",
                        generator.getObjectName(), e.getMessage(), e);
                if (flowFile != null) {
                    session.remove(flowFile);
                }
                FlowFile errorFlowFile = session.create();
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                errorFlowFile = session.putAttribute(errorFlowFile, "sapient.error", errorMsg);
                errorFlowFile = session.putAttribute(errorFlowFile, "sapient.object.name", generator.getObjectName());
                session.transfer(errorFlowFile, REL_FAILURE);
            }
        }
    }

    private void validateConfig(GeneratorConfig config) {
        if (config.getObjects() == null || config.getObjects().isEmpty()) {
            throw new IllegalArgumentException("Configuration must define at least one object");
        }
        if (config.getNodeId() == null || config.getNodeId().isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (config.getDestinationId() == null || config.getDestinationId().isBlank()) {
            throw new IllegalArgumentException("destinationId is required");
        }
        for (int i = 0; i < config.getObjects().size(); i++) {
            ObjectConfig obj = config.getObjects().get(i);
            if (obj.getMovement() == null) {
                throw new IllegalArgumentException("Object at index " + i + " is missing 'movement' configuration");
            }
            if (obj.getMovement().getPattern() == null || obj.getMovement().getPattern().isBlank()) {
                throw new IllegalArgumentException("Object at index " + i + " is missing 'movement.pattern'");
            }
        }
    }

    private MovementEngine createMovementEngine(MovementConfig movementConfig) {
        return switch (movementConfig.getPattern().toUpperCase()) {
            case "ORBIT" -> new OrbitMovement(movementConfig);
            case "FIGURE_8" -> new Figure8Movement(movementConfig);
            case "STATIONARY" -> new StationaryMovement(movementConfig);
            default -> throw new IllegalArgumentException(
                    "Unknown movement pattern: " + movementConfig.getPattern());
        };
    }
}
