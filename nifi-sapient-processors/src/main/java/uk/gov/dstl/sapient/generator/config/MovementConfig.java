package uk.gov.dstl.sapient.generator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovementConfig {
    private String pattern;
    private CenterPoint centerPoint;
    private double radiusM;
    private double speedMps;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public CenterPoint getCenterPoint() { return centerPoint; }
    public void setCenterPoint(CenterPoint centerPoint) { this.centerPoint = centerPoint; }

    public double getRadiusM() { return radiusM; }
    public void setRadiusM(double radiusM) { this.radiusM = radiusM; }

    public double getSpeedMps() { return speedMps; }
    public void setSpeedMps(double speedMps) { this.speedMps = speedMps; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CenterPoint {
        private double latitude;
        private double longitude;
        private double altitude;

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public double getAltitude() { return altitude; }
        public void setAltitude(double altitude) { this.altitude = altitude; }
    }
}
