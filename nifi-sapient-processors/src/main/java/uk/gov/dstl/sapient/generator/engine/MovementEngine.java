package uk.gov.dstl.sapient.generator.engine;

/**
 * Interface for computing object positions and velocities over time.
 * All internal math is in meters; output is in lat/lng degrees.
 */
public interface MovementEngine {

    /** Returns [latitude, longitude, altitude] at the given elapsed time. */
    double[] getPosition(double elapsedSeconds);

    /** Returns [eastRate, northRate, upRate] in m/s at the given elapsed time. */
    double[] getVelocity(double elapsedSeconds);

    /** Returns predicted [latitude, longitude, altitude] at elapsed + lookahead. */
    default double[] getPredictedPosition(double elapsedSeconds, double lookaheadSeconds) {
        return getPosition(elapsedSeconds + lookaheadSeconds);
    }
}
