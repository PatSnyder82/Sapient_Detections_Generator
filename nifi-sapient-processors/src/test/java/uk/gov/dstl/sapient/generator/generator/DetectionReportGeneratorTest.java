package uk.gov.dstl.sapient.generator.generator;

import org.junit.Test;
import uk.gov.dstl.sapient.generator.config.*;
import uk.gov.dstl.sapient.generator.engine.StationaryMovement;
import uk.gov.dstl.sapientmsg.bsiflex335v2.SapientMessage;

import java.util.List;

import static org.junit.Assert.*;

public class DetectionReportGeneratorTest {

    private GeneratorConfig makeGlobalConfig() {
        GeneratorConfig config = new GeneratorConfig();
        config.setNodeId("test-node-id");
        config.setDestinationId("test-dest-id");
        config.setUtmZone("30U");
        config.setOutputDirectory("./output");
        return config;
    }

    private ObjectConfig makeObjectConfig(String name) {
        ObjectConfig obj = new ObjectConfig();
        obj.setName(name);
        obj.setUpdateFrequencyHz(1.0);

        ClassificationConfig cls = new ClassificationConfig();
        cls.setType("Vehicle");
        cls.setConfidence(0.85);
        ClassificationConfig.SubClassConfig sub = new ClassificationConfig.SubClassConfig();
        sub.setType("Car");
        sub.setConfidence(0.7);
        sub.setLevel(1);
        cls.setSubClasses(List.of(sub));
        obj.setClassification(cls);

        MovementConfig mov = new MovementConfig();
        mov.setPattern("STATIONARY");
        mov.setRadiusM(0.0);
        mov.setSpeedMps(0.0);
        MovementConfig.CenterPoint cp = new MovementConfig.CenterPoint();
        cp.setLatitude(51.0);
        cp.setLongitude(-1.0);
        cp.setAltitude(100.0);
        mov.setCenterPoint(cp);
        obj.setMovement(mov);

        return obj;
    }

    @Test
    public void testGeneratedMessageContainsCorrectNodeId() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj = makeObjectConfig("TestVehicle");
        StationaryMovement engine = new StationaryMovement(obj.getMovement());
        DetectionReportGenerator gen = new DetectionReportGenerator(global, obj, engine);

        SapientMessage msg = gen.generate(0.0);

        assertEquals("test-node-id", msg.getNodeId());
        assertEquals("test-dest-id", msg.getDestinationId());
    }

    @Test
    public void testGeneratedMessageHasDetectionReport() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj = makeObjectConfig("TestVehicle");
        StationaryMovement engine = new StationaryMovement(obj.getMovement());
        DetectionReportGenerator gen = new DetectionReportGenerator(global, obj, engine);

        SapientMessage msg = gen.generate(0.0);

        assertTrue("Should have detection report", msg.hasDetectionReport());
        assertNotNull("Report ID should not be null", msg.getDetectionReport().getReportId());
        assertFalse("Report ID should not be empty", msg.getDetectionReport().getReportId().isEmpty());
    }

    @Test
    public void testObjectIdIsDeterministic() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj1 = makeObjectConfig("SameName");
        ObjectConfig obj2 = makeObjectConfig("SameName");
        StationaryMovement engine1 = new StationaryMovement(obj1.getMovement());
        StationaryMovement engine2 = new StationaryMovement(obj2.getMovement());
        DetectionReportGenerator gen1 = new DetectionReportGenerator(global, obj1, engine1);
        DetectionReportGenerator gen2 = new DetectionReportGenerator(global, obj2, engine2);

        assertEquals("Same name should produce same object ID", gen1.getObjectId(), gen2.getObjectId());
    }

    @Test
    public void testDifferentNamesProduceDifferentObjectIds() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj1 = makeObjectConfig("Alpha");
        ObjectConfig obj2 = makeObjectConfig("Bravo");
        StationaryMovement engine1 = new StationaryMovement(obj1.getMovement());
        StationaryMovement engine2 = new StationaryMovement(obj2.getMovement());
        DetectionReportGenerator gen1 = new DetectionReportGenerator(global, obj1, engine1);
        DetectionReportGenerator gen2 = new DetectionReportGenerator(global, obj2, engine2);

        assertNotEquals("Different names should produce different object IDs",
                gen1.getObjectId(), gen2.getObjectId());
    }

    @Test
    public void testReportIdChangesPerGeneration() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj = makeObjectConfig("TestVehicle");
        StationaryMovement engine = new StationaryMovement(obj.getMovement());
        DetectionReportGenerator gen = new DetectionReportGenerator(global, obj, engine);

        SapientMessage msg1 = gen.generate(0.0);
        SapientMessage msg2 = gen.generate(1.0);

        assertNotEquals("Report IDs should differ between generations",
                msg1.getDetectionReport().getReportId(),
                msg2.getDetectionReport().getReportId());
    }

    @Test
    public void testClassificationIsIncluded() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj = makeObjectConfig("TestVehicle");
        StationaryMovement engine = new StationaryMovement(obj.getMovement());
        DetectionReportGenerator gen = new DetectionReportGenerator(global, obj, engine);

        SapientMessage msg = gen.generate(0.0);

        assertEquals(1, msg.getDetectionReport().getClassificationCount());
        assertEquals("Vehicle", msg.getDetectionReport().getClassification(0).getType());
        assertEquals(1, msg.getDetectionReport().getClassification(0).getSubClassCount());
        assertEquals("Car", msg.getDetectionReport().getClassification(0).getSubClass(0).getType());
    }

    @Test
    public void testLocationHasCorrectCoordinateSystem() {
        GeneratorConfig global = makeGlobalConfig();
        ObjectConfig obj = makeObjectConfig("TestVehicle");
        StationaryMovement engine = new StationaryMovement(obj.getMovement());
        DetectionReportGenerator gen = new DetectionReportGenerator(global, obj, engine);

        SapientMessage msg = gen.generate(0.0);

        assertEquals("30U", msg.getDetectionReport().getLocation().getUtmZone());
    }
}
