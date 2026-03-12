# SAPIENT Detection Generator — NiFi Processor

An Apache NiFi 2.6 processor (NAR) that generates simulated [BSI Flex 335 v2.0](https://www.bsigroup.com/en-GB/insights-and-media/insights/brochures/bsi-flex-335/) SAPIENT `DetectionReport` messages as JSON FlowFiles. Configure tracked objects with movement patterns (ORBIT, FIGURE_8, STATIONARY) and the processor generates SAPIENT-compliant detection reports at each NiFi scheduling trigger.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running Tests](#running-tests)
- [Deploying to NiFi](#deploying-to-nifi)
- [Processor Reference](#processor-reference)
  - [Properties](#properties)
  - [Relationships](#relationships)
  - [FlowFile Attributes](#flowfile-attributes)
- [Configuration JSON Schema](#configuration-json-schema)
  - [Global Settings](#global-settings)
  - [Object Definitions](#object-definitions)
  - [Movement Patterns](#movement-patterns)
  - [Classification](#classification)
- [Output Format](#output-format)
- [Architecture Overview](#architecture-overview)
- [User Guide](#user-guide)
  - [Quick Start](#quick-start)
  - [Example NiFi Flow](#example-nifi-flow)
  - [Creating Custom Scenarios](#creating-custom-scenarios)
  - [Tips and Troubleshooting](#tips-and-troubleshooting)

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **Java JDK** | 21+ | [Eclipse Temurin](https://adoptium.net/) recommended |
| **Apache Maven** | 3.9+ | [Download](https://maven.apache.org/download.cgi) |
| **Apache NiFi** | 2.6.0+ | [Download](https://nifi.apache.org/download/) |

Verify your environment:

```bash
java -version    # Should show 21.x or higher
mvn --version    # Should show 3.9.x or higher
```

> **Windows note**: If you have multiple Java versions installed, set `JAVA_HOME` to your JDK 21 installation:
> ```cmd
> set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot
> ```

## Building

From the project root directory:

```bash
# Full build: compile protos, compile Java, run tests, package NAR
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests
```

The build produces:
1. **Processor JAR**: `nifi-sapient-processors/target/nifi-sapient-processors-1.0.0-SNAPSHOT.jar`
2. **NAR file**: `nifi-sapient-nar/target/nifi-sapient-nar-1.0.0-SNAPSHOT.nar` — this is what you deploy to NiFi

The build process:
1. Downloads and runs `protoc` 3.25.5 to compile `.proto` schemas into Java classes
2. Compiles all Java source and test files
3. Runs unit tests (31 tests across 4 test classes)
4. Packages everything into a NAR (NiFi Archive) via the `nifi-nar-maven-plugin`

## Running Tests

```bash
# Run all tests
mvn test

# Run only processor tests
mvn test -pl nifi-sapient-processors -Dtest=SapientDetectionProcessorTest

# Run only movement engine tests
mvn test -pl nifi-sapient-processors -Dtest=OrbitMovementTest,Figure8MovementTest

# Run a specific test method
mvn test -pl nifi-sapient-processors -Dtest=OrbitMovementTest#testVelocityMagnitudeMatchesSpeed
```

**Test classes**:
| Class | Tests | What it validates |
|-------|-------|-------------------|
| `SapientDetectionProcessorTest` | 8 | NiFi processor lifecycle, FlowFile output, config validation |
| `OrbitMovementTest` | 6 | Circular orbit positions, velocity magnitude, full orbit return |
| `Figure8MovementTest` | 5 | Figure-8 positions, center crossing, radius validation |
| `DetectionReportGeneratorTest` | 7 | Protobuf message correctness, deterministic IDs, classification |
| `IdGeneratorTest` | 5 | ULID stability, uniqueness, format |

## Deploying to NiFi

1. Build the NAR:
   ```bash
   mvn clean install
   ```

2. Copy the NAR to NiFi's extensions directory:
   ```bash
   cp nifi-sapient-nar/target/nifi-sapient-nar-1.0.0-SNAPSHOT.nar $NIFI_HOME/extensions/
   ```

3. Restart NiFi (or wait for auto-detection if configured):
   ```bash
   $NIFI_HOME/bin/nifi.sh restart
   ```

4. In the NiFi UI, add a new processor and search for **"SapientDetection"**.

## Processor Reference

### Properties

| Property | Required | Description |
|----------|----------|-------------|
| **Configuration JSON** | Yes | Full JSON configuration defining the sensor node, destination, and tracked objects. See [Configuration JSON Schema](#configuration-json-schema). |

### Relationships

| Relationship | Description |
|--------------|-------------|
| **success** | Successfully generated SAPIENT DetectionReport JSON FlowFiles. One FlowFile per configured object per trigger. |
| **failure** | FlowFile created when generation fails for a specific object. Contains error details in attributes. |

### FlowFile Attributes

Each success FlowFile includes:

| Attribute | Description |
|-----------|-------------|
| `mime.type` | Always `application/json` |
| `sapient.object.name` | Object name from config (e.g., `"UAV-1"`) |
| `sapient.object.id` | Deterministic ULID for the object |
| `sapient.report.id` | Unique ULID for this specific detection |

### Scheduling

Use NiFi's built-in scheduling to control detection frequency:
- **Run Schedule**: `1 sec` = 1 detection per object per second
- **Run Schedule**: `500 ms` = 2 detections per object per second
- **Concurrent Tasks**: Keep at `1` (processor tracks elapsed time internally)

> **Note**: The `updateFrequencyHz` field in config is not used for scheduling in NiFi mode — NiFi's run schedule controls the trigger rate instead.

---

## Configuration JSON Schema

Paste this JSON into the **Configuration JSON** processor property.

### Global Settings

```json
{
  "nodeId": "cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b",
  "destinationId": "a8654cdf-4328-47de-81fa-c495589e30c1",
  "outputDirectory": "./output",
  "utmZone": "30U",
  "objects": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nodeId` | UUID string | Yes | SAPIENT node ID for the simulated sensor. |
| `destinationId` | UUID string | Yes | Destination node ID (e.g., fusion node). |
| `outputDirectory` | String | No | Not used in NiFi mode (retained for backwards compatibility). |
| `utmZone` | String | Yes | UTM zone included in location data (e.g., `"30U"`). |
| `objects` | Array | Yes | List of tracked objects. At least one required. |

### Object Definitions

```json
{
  "name": "UAV-1",
  "updateFrequencyHz": 1.0,
  "classification": { ... },
  "movement": { ... }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Unique object name. Seeds deterministic object ID generation. |
| `updateFrequencyHz` | Number | No | Not used for scheduling in NiFi (retained for config compatibility). |
| `classification` | Object | Yes | How this object is classified in detection reports. |
| `movement` | Object | Yes | Movement pattern and parameters. |

### Movement Patterns

```json
"movement": {
  "pattern": "ORBIT",
  "centerPoint": {
    "latitude": 51.1739726374,
    "longitude": -1.82237671048,
    "altitude": 788.0
  },
  "radiusM": 500.0,
  "speedMps": 15.0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pattern` | String | Yes | `"STATIONARY"`, `"ORBIT"`, or `"FIGURE_8"` |
| `centerPoint.latitude` | Number | Yes | Center latitude in decimal degrees |
| `centerPoint.longitude` | Number | Yes | Center longitude in decimal degrees |
| `centerPoint.altitude` | Number | Yes | Altitude in meters |
| `radiusM` | Number | Yes | Movement radius in meters. Must be > 0 for ORBIT/FIGURE_8 |
| `speedMps` | Number | Yes | Speed in m/s. Determines angular velocity for moving patterns |

#### STATIONARY
Fixed position, zero velocity. Useful for stationary sensors, parked vehicles.

#### ORBIT
Circular path around center: `metersEast = radius × cos(ω × t)`, `metersNorth = radius × sin(ω × t)` where `ω = speed / radius`.

#### FIGURE_8
Lissajous figure-8: `metersEast = radius × sin(ω × t)`, `metersNorth = radius × sin(2 × ω × t)`.

> All movement math is performed in meters and converted to lat/lng deltas using the WGS-84 approximation.

### Classification

```json
"classification": {
  "type": "Aircraft",
  "confidence": 0.95,
  "subClasses": [
    { "type": "UAV", "confidence": 0.9, "level": 1 }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | Yes | Top-level classification (e.g., `"Aircraft"`, `"Human"`, `"Vehicle"`) |
| `confidence` | Number | Yes | Confidence 0.0–1.0. Also used as `detectionConfidence` |
| `subClasses` | Array | No | Hierarchical sub-classifications |

---

## Output Format

Each FlowFile contains a SAPIENT `SapientMessage` with a `DetectionReport` payload, serialized using Protobuf's `JsonFormat.printer()` for exact BSI Flex 335 v2.0 compliance.

```json
{
  "timestamp": "2026-03-12T10:39:31.965793100Z",
  "nodeId": "cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b",
  "destinationId": "a8654cdf-4328-47de-81fa-c495589e30c1",
  "detectionReport": {
    "reportId": "01KKGT2MN7CDX8KHT2PSS6DJTA",
    "objectId": "4SB39V874PZJED71ZNSC38VBC9",
    "taskId": "350A1B5YA4PGS7P6PPFJ3X40G2",
    "state": "Active",
    "location": { "x": 51.17, "y": -1.82, "z": 788.0, "..." },
    "enuVelocity": { "eastRate": -0.007, "northRate": 14.999, "upRate": 0.0 },
    "predictionLocation": { "..." },
    "classification": [{ "type": "Aircraft", "confidence": 0.95, "..." }],
    "behaviour": [{ "type": "tracking", "confidence": 0.9 }]
  }
}
```

---

## Architecture Overview

```
sapient-detection-generator-parent/          (root aggregator POM)
├── nifi-sapient-processors/                 (processor JAR module)
│   └── src/main/java/uk/gov/dstl/sapient/generator/
│       ├── processor/
│       │   └── SapientDetectionProcessor.java   NiFi processor entry point
│       ├── config/
│       │   ├── GeneratorConfig.java             Root config POJO
│       │   ├── ObjectConfig.java                Per-object settings
│       │   ├── MovementConfig.java              Movement pattern config
│       │   └── ClassificationConfig.java        Classification config
│       ├── engine/
│       │   ├── MovementEngine.java              Interface
│       │   ├── OrbitMovement.java               Circular orbit
│       │   ├── Figure8Movement.java             Lissajous figure-8
│       │   └── StationaryMovement.java          Fixed position
│       └── generator/
│           ├── DetectionReportGenerator.java     Builds protobuf messages
│           └── IdGenerator.java                  ULID generation
├── nifi-sapient-nar/                        (NAR packaging module)
└── config.json                              (sample configuration)
```

**Key design decisions**:
- **Protobuf-first serialization**: `JsonFormat.printer().print(message)` ensures JSON matches BSI Flex 335 v2.0
- **Deterministic object IDs**: Object names are SHA-256 hashed to ULIDs for stable tracking
- **Meter-based physics**: Movement in meters, converted to lat/lng for realistic paths
- **NAR classloader isolation**: All dependencies (protobuf, Jackson, ULID) bundle inside the NAR

---

## User Guide

### Quick Start

1. **Build** the project:
   ```bash
   mvn clean install
   ```

2. **Deploy** the NAR to NiFi:
   ```bash
   cp nifi-sapient-nar/target/nifi-sapient-nar-1.0.0-SNAPSHOT.nar $NIFI_HOME/extensions/
   ```

3. **Restart** NiFi.

4. In the NiFi UI:
   - Add a **SapientDetectionProcessor**
   - Paste the contents of `config.json` into the **Configuration JSON** property
   - Set run schedule (e.g., `1 sec`)
   - Connect the **success** relationship to a downstream processor (e.g., PutFile, PublishKafka)
   - Start the processor

### Example NiFi Flow

```
[SapientDetectionProcessor] → success → [PutFile]
                            → failure → [LogAttribute]
```

For Kafka integration:
```
[SapientDetectionProcessor] → success → [PublishKafka]
```

For network transport:
```
[SapientDetectionProcessor] → success → [PutTCP]
```

### Creating Custom Scenarios

#### Multi-UAV surveillance

Paste this into the Configuration JSON property:

```json
{
  "nodeId": "your-sensor-uuid",
  "destinationId": "your-fusion-node-uuid",
  "utmZone": "30U",
  "objects": [
    {
      "name": "Patrol-Alpha",
      "updateFrequencyHz": 1.0,
      "classification": { "type": "Aircraft", "confidence": 0.98, "subClasses": [{ "type": "UAV", "confidence": 0.95, "level": 1 }] },
      "movement": { "pattern": "ORBIT", "centerPoint": { "latitude": 51.5074, "longitude": -0.1278, "altitude": 120.0 }, "radiusM": 200.0, "speedMps": 12.0 }
    },
    {
      "name": "Patrol-Bravo",
      "updateFrequencyHz": 1.0,
      "classification": { "type": "Aircraft", "confidence": 0.98, "subClasses": [{ "type": "UAV", "confidence": 0.95, "level": 1 }] },
      "movement": { "pattern": "FIGURE_8", "centerPoint": { "latitude": 51.508, "longitude": -0.129, "altitude": 150.0 }, "radiusM": 300.0, "speedMps": 10.0 }
    }
  ]
}
```

### Tips and Troubleshooting

| Issue | Solution |
|-------|----------|
| Processor won't start: "Failed to parse Configuration JSON" | Check your JSON syntax. Paste it into a JSON validator first. |
| "Configuration must define at least one object" | The `objects` array is empty or missing. |
| "radiusM must be positive for ORBIT pattern" | ORBIT and FIGURE_8 need `radiusM` > 0. Use STATIONARY for fixed objects. |
| No FlowFiles appearing | Check the processor is running and scheduled. Verify the success relationship is connected. |
| NAR not loading in NiFi | Ensure NiFi 2.6+ and Java 21+. Check `nifi-app.log` for classloading errors. |
| Want to use different Java version | NiFi 2.6 requires Java 21. Earlier NiFi versions are not compatible with this NAR. |
