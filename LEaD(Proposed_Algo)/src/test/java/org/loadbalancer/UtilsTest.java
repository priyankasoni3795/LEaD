package org.loadbalancer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class UtilsTest {
    /**
     *
     * @param expected  `want` value
     * @param actual    `got` value
     * @param precision Number of decimal digits of precision in its scientific notation
     * @param message   Failure message
     */
    private static void assertEquals(double expected, double actual, int precision, String message) {
        expected = roundOff(expected, precision);
        actual = roundOff(actual, precision);
        Assertions.assertEquals(expected, actual, message);
    }
    private static void assertEquals(double expected, double actual, int precision) {
        String message = String.format("expected (%e) !~ actual (%e)", expected, actual);
        assertEquals(expected, actual, precision, message);
    }
    private static double roundOff(double value, int digits) {
        double exponent = Math.floor(Math.log10(value));
        double multiplier = Math.pow(10.0, digits - exponent);
        long rounded = Math.round(value * multiplier);
        return rounded / multiplier;
    }
    @Test
    void testSNR() {
        double got = Utils.snr(0.5, 1.577e-8, 1e-10);
        double want = 78.85;
        assertEquals(want, got, 3);
    }
    @Test
    void testPathLoss() {
        double got = Utils.pathLoss(100);
        double want = 78.02;
        assertEquals(want, got, 3);
    }
    @Test
    void testChannelGain() {
        double got = Utils.channelGain(100);
        double want = 1.5776e-8;
        assertEquals(want, got, 3);
    }
    @Test
    void testUplinkDataRate() {
        double got = Utils.dataRate(10e6, 78.85);
        double want = 63.19e6;
        assertEquals(want, got, 3);
    }
    @Test
    void testDownlinkDataRate() {
        double snr = 236.6416;
        double got = Utils.dataRate(10e6, snr);
        double want = 78.926e6;
        assertEquals(want, got, 4);
    }
}
