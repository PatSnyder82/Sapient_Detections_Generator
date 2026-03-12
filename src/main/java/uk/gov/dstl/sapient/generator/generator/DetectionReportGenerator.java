package uk.gov.dstl.sapient.generator.generator;

import com.google.protobuf.Timestamp;
import uk.gov.dstl.sapient.generator.config.ClassificationConfig;
import uk.gov.dstl.sapient.generator.config.GeneratorConfig;
import uk.gov.dstl.sapient.generator.config.ObjectConfig;
import uk.gov.dstl.sapient.generator.engine.MovementEngine;
import uk.gov.dstl.sapientmsg.bsiflex335v2.*;

import java.time.Instant;
import java.util.List;

public class DetectionReportGenerator {

    private static final double PREDICTION_LOOKAHEAD_SECONDS = 5.0;

    private final GeneratorConfig globalConfig;
    private final ObjectConfig objectConfig;
    private final MovementEngine movementEngine;
    private final String objectId;
    private final String taskId;

    public DetectionReportGenerator(GeneratorConfig globalConfig, ObjectConfig objectConfig,
                                    MovementEngine movementEngine) {
        this.globalConfig = globalConfig;
        this.objectConfig = objectConfig;
        this.movementEngine = movementEngine;
        this.objectId = IdGenerator.deterministicUlid(objectConfig.getName());
        this.taskId = IdGenerator.deterministicUlid("task-" + objectConfig.getName());
    }

    public SapientMessage generate(double elapsedSeconds) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        String reportId = IdGenerator.randomUlid();

        double[] position = movementEngine.getPosition(elapsedSeconds);
        double[] velocity = movementEngine.getVelocity(elapsedSeconds);
        double[] predicted = movementEngine.getPredictedPosition(elapsedSeconds, PREDICTION_LOOKAHEAD_SECONDS);

        Instant predictedTime = now.plusSeconds((long) PREDICTION_LOOKAHEAD_SECONDS);
        Timestamp predictedTimestamp = Timestamp.newBuilder()
                .setSeconds(predictedTime.getEpochSecond())
                .setNanos(predictedTime.getNano())
                .build();

        Location location = buildLocation(position);
        Location predictionLoc = buildLocation(predicted);

        DetectionReport.PredictedLocation predictionLocation = DetectionReport.PredictedLocation.newBuilder()
                .setLocation(predictionLoc)
                .setPredictedTimestamp(predictedTimestamp)
                .build();

        ENUVelocity enuVelocity = ENUVelocity.newBuilder()
                .setEastRate(velocity[0])
                .setNorthRate(velocity[1])
                .setUpRate(velocity[2])
                .build();

        DetectionReport.Builder reportBuilder = DetectionReport.newBuilder()
                .setReportId(reportId)
                .setObjectId(objectId)
                .setTaskId(taskId)
                .setState("Active")
                .setLocation(location)
                .setDetectionConfidence((float) objectConfig.getClassification().getConfidence())
                .setPredictionLocation(predictionLocation)
                .setEnuVelocity(enuVelocity);

        addClassification(reportBuilder);

        reportBuilder.addBehaviour(DetectionReport.Behaviour.newBuilder()
                .setType("tracking")
                .setConfidence(0.9f)
                .build());

        return SapientMessage.newBuilder()
                .setTimestamp(timestamp)
                .setNodeId(globalConfig.getNodeId())
                .setDestinationId(globalConfig.getDestinationId())
                .setDetectionReport(reportBuilder.build())
                .build();
    }

    private Location buildLocation(double[] position) {
        return Location.newBuilder()
                .setX(position[0])
                .setY(position[1])
                .setZ(position[2])
                .setXError(5.0)
                .setYError(5.0)
                .setZError(5.0)
                .setCoordinateSystem(LocationCoordinateSystem.LOCATION_COORDINATE_SYSTEM_UTM_M)
                .setDatum(LocationDatum.LOCATION_DATUM_WGS84_E)
                .setUtmZone(globalConfig.getUtmZone())
                .build();
    }

    private void addClassification(DetectionReport.Builder reportBuilder) {
        ClassificationConfig classConfig = objectConfig.getClassification();
        DetectionReport.DetectionReportClassification.Builder classBuilder =
                DetectionReport.DetectionReportClassification.newBuilder()
                        .setType(classConfig.getType())
                        .setConfidence((float) classConfig.getConfidence());

        List<ClassificationConfig.SubClassConfig> subClasses = classConfig.getSubClasses();
        if (subClasses != null) {
            for (ClassificationConfig.SubClassConfig sub : subClasses) {
                classBuilder.addSubClass(DetectionReport.SubClass.newBuilder()
                        .setType(sub.getType())
                        .setConfidence((float) sub.getConfidence())
                        .setLevel(sub.getLevel())
                        .build());
            }
        }

        reportBuilder.addClassification(classBuilder.build());
    }

    public String getObjectName() {
        return objectConfig.getName();
    }

    public String getObjectId() {
        return objectId;
    }
}
