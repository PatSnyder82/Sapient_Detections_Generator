# SAPIENT Detection Generator

A Maven-based Java application that generates simulated [BSI Flex 335 v2.0](https://www.bsigroup.com/en-GB/insights-and-media/insights/brochures/bsi-flex-335/) SAPIENT `DetectionReport` messages. It reads a JSON configuration file, simulates persistent object tracks with realistic movement patterns, and writes each detection as an individual JSON file for inspection, validation, and integration testing.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running](#running)
- [Configuration Guide](#configuration-guide)
  - [Global Settings](#global-settings)
  - [Object Definitions](#object-definitions)
  - [Movement Patterns](#movement-patterns)
  - [Classification](#classification)
- [Output Format](#output-format)
- [Architecture Overview](#architecture-overview)
- [User Guide](#user-guide)
  - [Quick Start](#quick-start)
  - [Creating Custom Scenarios](#creating-custom-scenarios)
  - [Tips and Troubleshooting](#tips-and-troubleshooting)

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **Java JDK** | 17+ | [Eclipse Temurin](https://adoptium.net/) recommended |
| **Apache Maven** | 3.9+ | [Download](https://maven.apache.org/download.cgi) |

Verify your environment:

```bash
java -version    # Should show 17.x or higher
mvn --version    # Should show 3.9.x or higher
```

> **Windows note**: If you have multiple Java versions installed, set `JAVA_HOME` to point to your JDK 17 installation before running Maven:
> ```cmd
> set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot
> ```

## Building

From the project root directory:

```bash
# Compile protobuf schemas and Java sources
mvn clean compile

# Build executable fat JAR (includes all dependencies)
mvn clean package -DskipTests
```

The build process:
1. Downloads and runs `protoc` 3.25.5 to compile the `.proto` schemas into Java classes
2. Compiles all Java source files
3. Packages everything into a single executable JAR via the Maven Shade plugin

The output JAR is located at: `target/sapient-detection-generator-1.0.0-SNAPSHOT.jar`

## Running

```bash
# Run with default config (./config.json)
java -jar target/sapient-detection-generator-1.0.0-SNAPSHOT.jar

# Run with a custom config file
java -jar target/sapient-detection-generator-1.0.0-SNAPSHOT.jar path/to/my-config.json
```

The application runs continuously, generating detection files at the configured frequencies. Press **Ctrl+C** to stop gracefully.

Example console output:

```
Loaded configuration from: ./config.json
  Node ID: cb09bb6a-5b83-4dd8-88f1-2db8ea6d656b
  Output directory: ./output
  Objects: 3
Starting detection generation...
  Scheduled UAV-1: pattern=ORBIT, frequency=1.0 Hz, period=1000 ms
  Scheduled Person-1: pattern=FIGURE_8, frequency=0.5 Hz, period=2000 ms
  Scheduled Vehicle-1: pattern=STATIONARY, frequency=2.0 Hz, period=500 ms
Running. Press Ctrl+C to stop.
[2026-03-12T10:39:32.045Z] Generated detection for UAV-1 (report: 01KKGT2MN7CDX8KHT2PSS6DJTA)
[2026-03-12T10:39:32.045Z] Generated detection for Vehicle-1 (report: 01KKGT2MN7CDX8KHT2PSS6DJTB)
```

---

## Configuration Guide

The application is driven by a single `config.json` file. Below is a complete reference.

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
| `nodeId` | UUID string | Yes | The SAPIENT node ID included in every generated message. Identifies the simulated sensor. |
| `destinationId` | UUID string | Yes | The destination node ID (e.g., a fusion node or C2 system). |
| `outputDirectory` | File path | Yes | Directory where JSON files are written. Created automatically if it does not exist. |
| `utmZone` | String | Yes | UTM zone identifier included in location data (e.g., `"30U"` for southern England). |
| `objects` | Array | Yes | List of object definitions to simulate. At least one is required. |

### Object Definitions

Each entry in the `objects` array defines a tracked object:

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
| `name` | String | Yes | Unique object name. Used as a seed for deterministic ID generation — the same name always produces the same object ID, ensuring stable tracks across restarts. Also used in output file names. |
| `updateFrequencyHz` | Number | Yes | How often detections are generated, in Hz. `1.0` = once per second, `0.5` = once every 2 seconds, `2.0` = twice per second. Must be positive. |
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
| `pattern` | String | Yes | Movement type: `"STATIONARY"`, `"ORBIT"`, or `"FIGURE_8"`. |
| `centerPoint.latitude` | Number | Yes | Center latitude in decimal degrees. |
| `centerPoint.longitude` | Number | Yes | Center longitude in decimal degrees. |
| `centerPoint.altitude` | Number | Yes | Altitude in meters above datum. |
| `radiusM` | Number | Yes | Radius of movement in meters. Must be > 0 for ORBIT and FIGURE_8. Use `0.0` for STATIONARY. |
| `speedMps` | Number | Yes | Speed in meters per second. Determines angular velocity for moving patterns. Use `0.0` for STATIONARY. |

#### STATIONARY

The object remains at the center point. Velocity is zero. Useful for simulating fixed sensors, parked vehicles, or stationary persons.

#### ORBIT

The object moves in a circular path around the center point.

```
Position:
  metersEast  = radius × cos(ω × t)
  metersNorth = radius × sin(ω × t)

  where ω (angular velocity) = speedMps / radiusM
```

The angular velocity is derived from the linear speed and radius. For example, a UAV at 15 m/s with a 500 m radius completes one full orbit in about 209 seconds (~3.5 minutes).

#### FIGURE_8

The object traces a figure-8 (Lissajous curve) centered on the center point.

```
Position:
  metersEast  = radius × sin(ω × t)
  metersNorth = radius × sin(2 × ω × t)

  where ω (angular velocity) = speedMps / radiusM
```

This creates a smooth, self-crossing pattern. The lobes of the figure-8 extend `radiusM` meters from center in each direction.

> **Movement math**: All movement calculations are performed in meters for linear accuracy. Meter offsets are converted to latitude/longitude deltas using the WGS-84 approximation (111,320 m/degree latitude, adjusted by cos(latitude) for longitude).

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
| `type` | String | Yes | Top-level classification (e.g., `"Aircraft"`, `"Human"`, `"Vehicle"`). |
| `confidence` | Number | Yes | Classification confidence from 0.0 to 1.0. Also used as `detectionConfidence`. |
| `subClasses` | Array | No | Hierarchical sub-classifications. |
| `subClasses[].type` | String | Yes | Sub-class type (e.g., `"UAV"`, `"Male"`, `"Car"`). |
| `subClasses[].confidence` | Number | Yes | Sub-class confidence from 0.0 to 1.0. |
| `subClasses[].level` | Integer | Yes | Hierarchy level (1 = first sub-class, 2 = sub-sub-class, etc.). |

---

## Output Format

Each generated detection is saved as an individual JSON file in the output directory.

### File Naming

```
{Timestamp}_{ObjectName}_{ReportID}.json
```

Example: `2026-03-12T10-39-32-045243100Z_UAV-1_01KKGT2MN7CDX8KHT2PSS6DJTA.json`

- **Timestamp**: ISO 8601 UTC with colons and dots replaced by dashes for filesystem compatibility.
- **ObjectName**: From the config `name` field.
- **ReportID**: Unique ULID generated for each detection.

### JSON Structure

Each file contains a SAPIENT `SapientMessage` with a `DetectionReport` payload, serialized using the Protobuf `JsonFormat.printer()`. This ensures the JSON structure exactly matches the BSI Flex 335 v2.0 standard.

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
    "location": {
      "x": 51.17397479334674,
      "y": -1.8152126628142993,
      "z": 788.0,
      "xError": 5.0,
      "yError": 5.0,
      "zError": 5.0,
      "coordinateSystem": "LOCATION_COORDINATE_SYSTEM_UTM_M",
      "datum": "LOCATION_DATUM_WGS84_E",
      "utmZone": "30U"
    },
    "detectionConfidence": 0.95,
    "predictionLocation": {
      "location": { "..." },
      "predictedTimestamp": "2026-03-12T10:39:36.965793100Z"
    },
    "classification": [{ "type": "Aircraft", "confidence": 0.95, "subClass": [{ "..." }] }],
    "behaviour": [{ "type": "tracking", "confidence": 0.9 }],
    "enuVelocity": {
      "eastRate": -0.0072,
      "northRate": 14.9999,
      "upRate": 0.0
    }
  }
}
```

### Key Fields

| Field | Description |
|-------|-------------|
| `reportId` | Unique ULID — different for every generated file. |
| `objectId` | Deterministic ULID — same for all reports of a given object name. Enables stable track visualization. |
| `taskId` | Deterministic ULID derived from the object name. |
| `location` | Current position with error estimates and coordinate metadata. |
| `predictionLocation` | Predicted position 5 seconds in the future, based on current velocity. |
| `enuVelocity` | East-North-Up velocity vector in meters per second. |

---

## Architecture Overview

```
uk.gov.dstl.sapient.generator
├── App.java                          Entry point, config loading, scheduler
├── config/
│   ├── GeneratorConfig.java          Root config POJO (Jackson)
│   ├── ObjectConfig.java             Per-object settings
│   ├── MovementConfig.java           Pattern, center point, radius, speed
│   └── ClassificationConfig.java     Type, confidence, sub-classes
├── engine/
│   ├── MovementEngine.java           Interface: getPosition, getVelocity
│   ├── StationaryMovement.java       Fixed position, zero velocity
│   ├── OrbitMovement.java            Circular orbit in meters → lat/lng
│   └── Figure8Movement.java          Lissajous figure-8 in meters → lat/lng
├── generator/
│   ├── DetectionReportGenerator.java Builds protobuf SapientMessage
│   └── IdGenerator.java              Deterministic + random ULID generation
└── output/
    └── JsonFileWriter.java           JsonFormat.printer() + file I/O
```

**Key design decisions**:

- **Protobuf-first serialization**: `JsonFormat.printer().print(message)` is used instead of Jackson/Gson for the SAPIENT output, ensuring the JSON exactly matches the BSI Flex 335 v2.0 schema (enum names, timestamp format, oneof handling).
- **Deterministic object IDs**: Object names are hashed (SHA-256) to produce deterministic ULIDs, so `"UAV-1"` always gets the same object ID across restarts.
- **Meter-based physics**: Movement is calculated in meters, then converted to lat/lng deltas, ensuring smooth circular and figure-8 paths regardless of geographic location.

---

## User Guide

### Quick Start

1. **Build** the project:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run** with the included sample config:
   ```bash
   java -jar target/sapient-detection-generator-1.0.0-SNAPSHOT.jar
   ```

3. **Observe** JSON files appearing in the `./output/` directory. Each file is a complete SAPIENT detection message.

4. **Stop** the generator with **Ctrl+C**.

### Creating Custom Scenarios

#### Example: Surveillance scenario with multiple UAVs

```json
{
  "nodeId": "your-sensor-uuid-here",
  "destinationId": "your-fusion-node-uuid-here",
  "outputDirectory": "./surveillance-output",
  "utmZone": "30U",
  "objects": [
    {
      "name": "Patrol-UAV-Alpha",
      "updateFrequencyHz": 2.0,
      "classification": {
        "type": "Aircraft",
        "confidence": 0.98,
        "subClasses": [{ "type": "UAV", "confidence": 0.95, "level": 1 }]
      },
      "movement": {
        "pattern": "ORBIT",
        "centerPoint": { "latitude": 51.5074, "longitude": -0.1278, "altitude": 120.0 },
        "radiusM": 200.0,
        "speedMps": 12.0
      }
    },
    {
      "name": "Patrol-UAV-Bravo",
      "updateFrequencyHz": 2.0,
      "classification": {
        "type": "Aircraft",
        "confidence": 0.98,
        "subClasses": [{ "type": "UAV", "confidence": 0.95, "level": 1 }]
      },
      "movement": {
        "pattern": "FIGURE_8",
        "centerPoint": { "latitude": 51.5080, "longitude": -0.1290, "altitude": 150.0 },
        "radiusM": 300.0,
        "speedMps": 10.0
      }
    }
  ]
}
```

#### Frequency guidelines

| Scenario | Recommended Hz | Notes |
|----------|---------------|-------|
| Fast-moving aircraft | 2.0–5.0 | High update rate for track accuracy |
| Walking person | 0.5–1.0 | Slower updates sufficient |
| Stationary vehicle | 0.1–0.5 | Low rate saves disk space |
| Stress testing | 10.0+ | Generates many files quickly |

### Tips and Troubleshooting

| Issue | Solution |
|-------|----------|
| `Fatal error: Configuration must define at least one object` | Your `objects` array in `config.json` is empty. Add at least one object. |
| `radiusM must be positive for ORBIT pattern` | ORBIT and FIGURE_8 patterns require `radiusM` > 0. Use STATIONARY for fixed objects. |
| `updateFrequencyHz must be positive` | Set a positive number (e.g., `1.0`). |
| Build fails with `java.lang.UnsupportedClassVersionError` | You are running with Java < 17. Set `JAVA_HOME` to a JDK 17+ installation. |
| Output directory fills up quickly | Reduce `updateFrequencyHz` or periodically clean the output directory. At 1 Hz per object, 3 objects produce ~10,800 files/hour. |
| Object positions don't change | Check that the movement `pattern` is `"ORBIT"` or `"FIGURE_8"`, not `"STATIONARY"`. |
| Want different object IDs on restart | Object IDs are deterministic from the `name` field. Change the name to get a different ID. |
