package uk.gov.dstl.sapient.generator.engine;

import uk.gov.dstl.sapient.generator.config.MovementConfig;

/**
 * Figure-8 (Lissajous) pattern. Math in UTM meters, output in lat/lng.
 * X_m = radius * sin(ω * t), Y_m = radius * sin(2 * ω * t)
 */
public class Figure8Movement implements MovementEngine {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private final double centerLat;
    private final double centerLng;
    private final double altitude;
    private final double radiusM;
    private final double angularVelocity; // rad/s
    private final double metersPerDegreeLng;

    public Figure8Movement(MovementConfig config) {
        MovementConfig.CenterPoint cp = config.getCenterPoint();
        this.centerLat = cp.getLatitude();
        this.centerLng = cp.getLongitude();
        this.altitude = cp.getAltitude();
        this.radiusM = config.getRadiusM();
        if (radiusM <= 0) {
            throw new IllegalArgumentException("radiusM must be positive for FIGURE_8 pattern, got: " + radiusM);
        }
        this.angularVelocity = config.getSpeedMps() / radiusM;
        this.metersPerDegreeLng = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(centerLat));
    }

    @Override
    public double[] getPosition(double elapsedSeconds) {
        double angle = angularVelocity * elapsedSeconds;
        double metersEast = radiusM * Math.sin(angle);
        double metersNorth = radiusM * Math.sin(2.0 * angle);

        double lat = centerLat + (metersNorth / METERS_PER_DEGREE_LAT);
        double lng = centerLng + (metersEast / metersPerDegreeLng);
        return new double[]{lat, lng, altitude};
    }

    @Override
    public double[] getVelocity(double elapsedSeconds) {
        // Analytical derivative of position
        double angle = angularVelocity * elapsedSeconds;
        double eastRate = radiusM * angularVelocity * Math.cos(angle);
        double northRate = 2.0 * radiusM * angularVelocity * Math.cos(2.0 * angle);
        return new double[]{eastRate, northRate, 0.0};
    }
}
