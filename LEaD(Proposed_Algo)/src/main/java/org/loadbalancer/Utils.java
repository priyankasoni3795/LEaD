package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;

public class Utils {
    public static final String LOGGER_NAME = "org.loadbalancer";
    /**
     * The criteria weight for the fog nodes obtained using AHP matrix with
     * (Energy, Latency, Computation Demand) = (1, 4, 5).
     */
    public static final double[] fogCriteriaWeights = {0.664, 0.230, 0.103};
    /**
     * The criteria weight for the iot devices obtained using AHP matrix with
     * (Energy, Latency, Deadline) = (1, 5, 3).
     */
    public static final double[] iotCriteriaWeights = {1.929, 0.616, 0.442};
    /**
     * Maximum computing load threshold.
     */
    public static final double COMPUTING_THRESHOLD = 0.99;
    /**
     * Maximum traffic load threshold.
     */
    public static final double TRAFFIC_THRESHOLD = 0.99;
    public static final double DEMAND_UNIT = 1e6;
    /**
     * The margin that determines whether a task executes locally
     * or on the fog node.
     * - local execution: 210 - 300
     * - fog execution: 301 - 480
     */
    public static final double DEMAND_DETERMINER = 300 * DEMAND_UNIT;
    /**
     * The static loss due to wave properties in decibel ({@value} dB).
     */
    private static final double STATIC_LOSS = 38.02;
    static final Collector<
            // T, A, R
            String, StringJoiner, StringJoiner> commaCollector = Collector.of(
            // supplier, accumulator, combiner
            Utils::commaJoiner, StringJoiner::add, StringJoiner::merge);

    /**
     * @param power Transmission power of IoT device
     * @param gain  Uplink channel gain
     * @param noise Noise power
     * @return Signal-to-Noise ratio
     */
    public static double snr(double power, double gain, double noise) {
        return (power * gain) / noise;
    }

    /**
     * @param distance Distance from transmitter to receiver
     * @return Path loss
     */
    public static double pathLoss(double distance) {
        double dynamicLoss = 20 * Math.log10(distance);
        return STATIC_LOSS + dynamicLoss;
    }

    /**
     * @param distance Distance from transmitter to receiver
     * @return Channel Gain
     */
    public static double channelGain(double distance) {
        final double staticLoss = 0.00015776112;
        double dSquared = distance * distance;
        return staticLoss / dSquared;
    }

    /**
     * @param bandwidth Bandwidth
     * @param snr       Signal-to-Noise ratio
     * @return Data Rate (in Hz)
     */
    public static double dataRate(double bandwidth, double snr) {
        double logE = Math.log(1 + snr);
        double term = logE / Math.log(2);
        return bandwidth * term;
    }

    /**
     * @param size Size of data to be transmitted
     * @param rate Data Rate of transmission
     * @return Transmission Time
     */
    public static double transmissionTime(double size, double rate) {
        return size / rate;
    }

    /**
     * @param values  criteria weights
     * @param weights Criteria weights ({@link #fogCriteriaWeights} or {@link #iotCriteriaWeights})
     * @return numeric preference (larger the better)
     */
    public static double preference(double[] values, double[] weights) {
        assert values.length == weights.length : "Values and Criteria weights do not match";
        double pref = 0.0;
        int size = values.length; // == weights.length
        for (int i = 0; i < size; i++) {
            pref += values[i] * weights[i];
        }
        return pref;
    }

    /**
     * @param app Application whose waiting time is to be calculated.
     * @param fog FogNode on which application is assumed to be executed.
     * @return Total waiting time, i.e. wait at base station queue
     * (traffic) and wait at fog node queue (execution).
     */
    static double waitingTime(Application app, FogNode fog) {
        IotDevice device = app.parent;
        double bsQueue = device.comm.waitingTime();
        double fogQueue = app.waitingTime(fog);
        return bsQueue + fogQueue;
    }

    /**
     * @param list List of items having an id.
     * @return String representation of the list.
     */
    static String join(ArrayList<? extends Identifiable> list) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Identifiable item : list) {
            joiner.add(String.format("'%s'", item.id()));
        }
        return joiner.toString();
    }

    static String log(String msg, LogPair... pairs) {
        StringJoiner sj = Arrays.stream(pairs)
                // get string from pair
                .map(LogPair::toString)
                // accumulate into joiner
                .collect(commaCollector);
        StringJoiner joiner = new StringJoiner(";");
        joiner.add(msg);
        joiner = joiner.merge(sj);
        return joiner.toString();
    }

    private static StringJoiner commaJoiner() {
        return new StringJoiner(",");
    }

    public record LogPair(String key, Object value) {
        @Override
        public String toString() {
            return String.format("%s=%s", this.key, this.value);
        }
    }
}
