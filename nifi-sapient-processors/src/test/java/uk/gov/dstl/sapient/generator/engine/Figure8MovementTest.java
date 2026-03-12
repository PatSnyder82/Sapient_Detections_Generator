package uk.gov.dstl.sapient.generator.engine;

import org.junit.Test;
import uk.gov.dstl.sapient.generator.config.MovementConfig;

import static org.junit.Assert.*;

public class Figure8MovementTest {

    private MovementConfig makeConfig(double lat, double lng, double alt, double radiusM, double speedMps) {
        MovementConfig config = new MovementConfig();
        config.setPattern("FIGURE_8");
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
    public void testPositionAtTimeZeroIsCenter() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        Figure8Movement fig8 = new Figure8Movement(config);

        double[] pos = fig8.getPosition(0.0);
        // At t=0: sin(0)=0 for both East and North → position is at center
        assertEquals("Latitude at t=0 should be center", 51.0, pos[0], 1e-9);
        assertEquals("Longitude at t=0 should be center", -1.0, pos[1], 1e-9);
        assertEquals("Altitude should be constant", 100.0, pos[2], 0.001);
    }

    @Test
    public void testPositionChangesOverTime() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        Figure8Movement fig8 = new Figure8Movement(config);

        double[] pos0 = fig8.getPosition(0.0);
        double[] pos5 = fig8.getPosition(5.0);

        assertFalse("Position should change over time",
                pos0[0] == pos5[0] && pos0[1] == pos5[1]);
    }

    @Test
    public void testFigure8CrossesCenterMultipleTimes() {
        double radiusM = 500.0;
        double speedMps = 15.0;
        double omega = speedMps / radiusM;
        // sin(2*omega*t) = 0 when 2*omega*t = n*pi → t = n*pi/(2*omega)
        double tCrossing = Math.PI / (2.0 * omega);

        MovementConfig config = makeConfig(51.0, -1.0, 100.0, radiusM, speedMps);
        Figure8Movement fig8 = new Figure8Movement(config);

        double[] posCrossing = fig8.getPosition(tCrossing);
        // North component should be near zero (crossing)
        assertEquals("North (lat) should be near center at crossing", 51.0, posCrossing[0], 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroRadiusThrowsException() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 0.0, 15.0);
        new Figure8Movement(config);
    }

    @Test
    public void testVelocityIsNonZeroWhenMoving() {
        MovementConfig config = makeConfig(51.0, -1.0, 100.0, 500.0, 15.0);
        Figure8Movement fig8 = new Figure8Movement(config);

        double[] vel = fig8.getVelocity(0.0);
        double magnitude = Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]);
        assertTrue("Velocity should be non-zero", magnitude > 0);
    }
}
