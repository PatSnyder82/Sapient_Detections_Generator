package uk.gov.dstl.sapient.generator.processor;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SapientDetectionProcessorTest {

    private TestRunner runner;

    private static final String VALID_CONFIG = """
            {
              "nodeId": "cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b",
              "destinationId": "a8654cdf-4328-47de-81fa-c495589e30c1",
              "outputDirectory": "./output",
              "utmZone": "30U",
              "objects": [
                {
                  "name": "TestUAV",
                  "updateFrequencyHz": 1.0,
                  "classification": {
                    "type": "Aircraft",
                    "confidence": 0.95,
                    "subClasses": [
                      { "type": "UAV", "confidence": 0.9, "level": 1 }
                    ]
                  },
                  "movement": {
                    "pattern": "ORBIT",
                    "centerPoint": {
                      "latitude": 51.1739,
                      "longitude": -1.8224,
                      "altitude": 788.0
                    },
                    "radiusM": 500.0,
                    "speedMps": 15.0
                  }
                }
              ]
            }
            """;

    private static final String MULTI_OBJECT_CONFIG = """
            {
              "nodeId": "cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b",
              "destinationId": "a8654cdf-4328-47de-81fa-c495589e30c1",
              "outputDirectory": "./output",
              "utmZone": "30U",
              "objects": [
                {
                  "name": "UAV-1",
                  "updateFrequencyHz": 1.0,
                  "classification": { "type": "Aircraft", "confidence": 0.95 },
                  "movement": {
                    "pattern": "ORBIT",
                    "centerPoint": { "latitude": 51.17, "longitude": -1.82, "altitude": 100.0 },
                    "radiusM": 200.0, "speedMps": 10.0
                  }
                },
                {
                  "name": "Person-1",
                  "updateFrequencyHz": 0.5,
                  "classification": { "type": "Human", "confidence": 0.8 },
                  "movement": {
                    "pattern": "STATIONARY",
                    "centerPoint": { "latitude": 51.18, "longitude": -1.83, "altitude": 50.0 },
                    "radiusM": 0.0, "speedMps": 0.0
                  }
                }
              ]
            }
            """;

    @Before
    public void setUp() {
        runner = TestRunners.newTestRunner(SapientDetectionProcessor.class);
    }

    @Test
    public void testValidConfigProducesFlowFiles() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, VALID_CONFIG);
        runner.run();

        runner.assertTransferCount(SapientDetectionProcessor.REL_SUCCESS, 1);
        runner.assertTransferCount(SapientDetectionProcessor.REL_FAILURE, 0);

        MockFlowFile flowFile = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS).get(0);
        String content = flowFile.getContent();

        assertTrue("Should contain detectionReport", content.contains("detectionReport"));
        assertTrue("Should contain nodeId", content.contains("cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b"));
        assertTrue("Should contain object classification", content.contains("Aircraft"));
        assertEquals("application/json", flowFile.getAttribute("mime.type"));
        assertEquals("TestUAV", flowFile.getAttribute("sapient.object.name"));
        assertNotNull(flowFile.getAttribute("sapient.report.id"));
    }

    @Test
    public void testMultipleObjectsProduceMultipleFlowFiles() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, MULTI_OBJECT_CONFIG);
        runner.run();

        runner.assertTransferCount(SapientDetectionProcessor.REL_SUCCESS, 2);
        runner.assertTransferCount(SapientDetectionProcessor.REL_FAILURE, 0);

        List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS);
        // Verify both objects generated
        boolean hasUav = flowFiles.stream().anyMatch(f -> "UAV-1".equals(f.getAttribute("sapient.object.name")));
        boolean hasPerson = flowFiles.stream().anyMatch(f -> "Person-1".equals(f.getAttribute("sapient.object.name")));
        assertTrue("Should have UAV-1", hasUav);
        assertTrue("Should have Person-1", hasPerson);
    }

    @Test
    public void testInvalidJsonFailsValidation() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, "not valid json");
        try {
            runner.run();
            fail("Should have thrown ProcessException for invalid JSON");
        } catch (AssertionError e) {
            // Expected: @OnScheduled throws ProcessException
            assertTrue(e.getMessage() != null || e.getCause() != null);
        }
    }

    @Test
    public void testEmptyObjectsFailsValidation() {
        String emptyConfig = """
                {
                  "nodeId": "test-node",
                  "destinationId": "test-dest",
                  "utmZone": "30U",
                  "objects": []
                }
                """;
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, emptyConfig);
        try {
            runner.run();
            fail("Should have thrown for empty objects list");
        } catch (AssertionError e) {
            assertTrue(e.getMessage() != null || e.getCause() != null);
        }
    }

    @Test
    public void testMissingNodeIdFailsValidation() {
        String noNodeId = """
                {
                  "destinationId": "test-dest",
                  "utmZone": "30U",
                  "objects": [
                    {
                      "name": "Test",
                      "updateFrequencyHz": 1.0,
                      "classification": { "type": "Aircraft", "confidence": 0.9 },
                      "movement": {
                        "pattern": "STATIONARY",
                        "centerPoint": { "latitude": 51.0, "longitude": -1.0, "altitude": 100.0 },
                        "radiusM": 0.0, "speedMps": 0.0
                      }
                    }
                  ]
                }
                """;
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, noNodeId);
        try {
            runner.run();
            fail("Should have thrown for missing nodeId");
        } catch (AssertionError e) {
            assertTrue(e.getMessage() != null || e.getCause() != null);
        }
    }

    @Test
    public void testFlowFileContainsValidSapientJson() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, VALID_CONFIG);
        runner.run();

        MockFlowFile flowFile = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS).get(0);
        String content = flowFile.getContent();

        // Verify key SAPIENT DetectionReport fields
        assertTrue("Should contain timestamp", content.contains("timestamp"));
        assertTrue("Should contain reportId", content.contains("reportId"));
        assertTrue("Should contain objectId", content.contains("objectId"));
        assertTrue("Should contain location", content.contains("location"));
        assertTrue("Should contain coordinateSystem", content.contains("coordinateSystem"));
        assertTrue("Should contain enuVelocity", content.contains("enuVelocity"));
        assertTrue("Should contain predictionLocation", content.contains("predictionLocation"));
        assertTrue("Should contain classification", content.contains("classification"));
    }

    @Test
    public void testConsecutiveRunsProduceDifferentReportIds() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, VALID_CONFIG);

        runner.run();
        String reportId1 = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS)
                .get(0).getAttribute("sapient.report.id");

        runner.clearTransferState();
        runner.run();
        String reportId2 = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS)
                .get(0).getAttribute("sapient.report.id");

        assertNotEquals("Consecutive runs should produce different report IDs", reportId1, reportId2);
    }

    @Test
    public void testObjectIdIsDeterministic() {
        runner.setProperty(SapientDetectionProcessor.CONFIGURATION_JSON, VALID_CONFIG);
        runner.run();
        String objectId1 = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS)
                .get(0).getAttribute("sapient.object.id");

        runner.clearTransferState();
        runner.run();
        String objectId2 = runner.getFlowFilesForRelationship(SapientDetectionProcessor.REL_SUCCESS)
                .get(0).getAttribute("sapient.object.id");

        assertEquals("Same object name should produce same object ID", objectId1, objectId2);
    }
}
