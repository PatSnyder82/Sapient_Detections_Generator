package uk.gov.dstl.sapient.generator.engine;

import uk.gov.dstl.sapient.generator.config.MovementConfig;

public class StationaryMovement implements MovementEngine {

    private final double latitude;
    private final double longitude;
    private final double altitude;

    public StationaryMovement(MovementConfig config) {
        MovementConfig.CenterPoint cp = config.getCenterPoint();
        this.latitude = cp.getLatitude();
        this.longitude = cp.getLongitude();
        this.altitude = cp.getAltitude();
    }

    @Override
    public double[] getPosition(double elapsedSeconds) {
        return new double[]{latitude, longitude, altitude};
    }

    @Override
    public double[] getVelocity(double elapsedSeconds) {
        return new double[]{0.0, 0.0, 0.0};
    }
}
