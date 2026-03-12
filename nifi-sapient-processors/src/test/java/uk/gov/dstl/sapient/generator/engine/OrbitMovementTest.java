package uk.gov.dstl.sapient.generator.engine;

import org.junit.Test;
import uk.gov.dstl.sapient.generator.config.MovementConfig;

import static org.junit.Assert.*;

public class OrbitMovementTest {

    private MovementConfig makeConfig(double lat, double lng, double alt, double radiusM, double speedMps) {
        MovementConfig config = new MovementConfig();
        config.setPattern("ORBIT");
        config.setRadiusM(radiusM);
        config.setSpeedMps(speedMps);
        MovementConfig.CenterPoint cp = new MovementConfig.CenterPoint();
        cp.setLatitude(lat);
        cp.setLongitude(lng);
        cp.setAltitude(alt);
        config.setCenterPoint(cp);
        return config;
    }

    @Test
    public void testPositionAtTimeZeroIsAtRadiusEast() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        OrbitMovement orbit = new OrbitMovement(config);

        double[] pos = orbit.getPosition(0.0);
        // At t=0: cos(0)=1, sin(0)=0 → offset is radiusM east, 0 north
        // Latitude should be center (no north offset)
        assertEquals("Latitude at t=0 should be near center", 51.0, pos[0], 0.001);
        // Longitude should be offset east by ~500m
        assertTrue("Longitude at t=0 should be greater than center", pos[1] > -1.0);
        assertEquals("Altitude should be constant", 100.0, pos[2], 0.001);
    }

    @Test
    public void testPositionChangesOverTime() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        OrbitMovement orbit = new OrbitMovement(config);

        double[] pos0 = orbit.getPosition(0.0);
        double[] pos10 = orbit.getPosition(10.0);

        assertFalse("Position should change over time",
                pos0[0] == pos10[0] && pos0[1] == pos10[1]);
    }

    @Test
    public void testVelocityMagnitudeMatchesSpeed() {
        double speedMps = 15.0;
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, speedMps);
        OrbitMovement orbit = new OrbitMovement(config);

        double[] vel = orbit.getVelocity(0.0);
        double magnitude = Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]);
        assertEquals("Velocity magnitude should equal speed", speedMps, magnitude, 0.01);
    }

    @Test
    public void testFullOrbitReturnsToStart() {
        double radiusM = 500.0;
        double speedMps = 15.0;
        double period = 2.0 * Math.PI * radiusM / speedMps;

        MovementConfig config = makeConfig(51.0, -1.0, 100.0, radiusM, speedMps);
        OrbitMovement orbit = new OrbitMovement(config);

        double[] posStart = orbit.getPosition(0.0);
        double[] posEnd = orbit.getPosition(period);

        assertEquals("Lat should return to start", posStart[0], posEnd[0], 1e-9);
        assertEquals("Lng should return to start", posStart[1], posEnd[1], 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroRadiusThrowsException() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 0.0, 15.0);
        new OrbitMovement(config);
    }

    @Test
    public void testPredictedPositionUsesLookahead() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        OrbitMovement orbit = new OrbitMovement(config);

        double[] predicted = orbit.getPredictedPosition(10.0, 5.0);
        double[] actual = orbit.getPosition(15.0);

        assertEquals("Predicted should equal position at t+lookahead", actual[0], predicted[0], 1e-12);
        assertEquals("Predicted should equal position at t+lookahead", actual[1], predicted[1], 1e-12);
    }
}
