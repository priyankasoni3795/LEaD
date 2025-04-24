package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FogNode implements Authenticator.Verifiable, Identifiable {
    private final static Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
    /**
     * ID to uniquely identify the fog node.
     */
    public final String id;
    /**
     * Maximum capacity of the fog node {@link #queue}. Default value
     * is presumed to be {@value}.
     *
     * @see Defaults#FOG_QUEUE_CAPACITY
     */
    final int queueCapacity;
    /**
     * Executor of the fog node, which handles all the task running stuff.
     */
    final Executor exec;
    /**
     * Queue of applications (task) waiting to be executed by the fog node.
     */
    final PriorityQueue<Application> queue;
    /**
     * SecretPart of the fog node to secure communications with FNC.
     */
    private final Authenticator.SecretPart secretPart;

    public FogNode(String id, Config cfg) {
        this.id = id;
        this.queueCapacity = cfg.queueCapacity;
        this.exec = new Executor(cfg.power, cfg.rate, cfg.quota, cfg.capacity);
        this.secretPart = Authenticator.generate();
        Comparator<Application> cmp = Comparator.comparingDouble(this::preference);
        this.queue = new PriorityQueue<>(this.queueCapacity, cmp);
    }

    /**
     * @return whether {@link #queue} is full or not.
     */
    boolean isQueueFull() {
        return this.queueCapacity <= this.queue.size();
    }

    /**
     * @param app Different applications for this given fog node evaluated
     *            under given criteria.
     * @return Preference of app for this given fog node based on criteria
     * values (Latency, Energy, Computation Demand).
     * @see Utils#fogCriteriaWeights
     */
    double preference(Application app) {
        double energy = app.energy(this);
        double latency = app.latency(this);
        double demand = app.computationDemand();
        double[] values = {latency, energy, demand};
        double preference = Utils.preference(values, Utils.fogCriteriaWeights);
        logger.log(Level.CONFIG, Utils.log("pref of app on fog", new Utils.LogPair("fog", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("latency", latency), new Utils.LogPair("energy", energy), new Utils.LogPair("demand", demand), new Utils.LogPair("preference", preference)));
        return preference;
    }

    @Override
    public Authenticator.SecretPart secretPart() {
        return this.secretPart;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("{id:'%s'", this.id);
        fmt.format(",queueCapacity:%d", this.queueCapacity);
        fmt.format(",quota:%d", this.exec.quota);
        fmt.format(",power:%e", this.exec.power);
        fmt.format(",rate:%e", this.exec.rate);
        fmt.format(",capacity:%e", this.exec.capacity);
        fmt.format("}");
        return fmt.toString();
    }

    public static FogNode Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected FogNode object", new IllegalStateException());
        }
        Config config = new Config();
        String id = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id":
                    id = parser.getText();
                    break;
                case "queue_capacity":
                    config.queueCapacity = parser.getIntValue();
                    break;
                case "quota":
                    config.quota = parser.getIntValue();
                    break;
                case "power":
                    config.power = parser.getDoubleValue();
                    break;
                case "rate":
                    config.rate = parser.getDoubleValue();
                    break;
                case "capacity":
                    config.capacity = parser.getDoubleValue();
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        return new FogNode(id, config);
    }

    public static class Config {
        public int queueCapacity = Defaults.FOG_QUEUE_CAPACITY;
        public int quota = Defaults.FOG_QUOTA;
        public double power = Defaults.FOG_COMPUTATION_POWER;
        public double rate = Defaults.FOG_COMPUTATION_RATE;
        public double capacity = Defaults.FOG_COMPUTING_CAPACITY;
    }

    /**
     * Executor handles all the execution related stuff of the fog node.
     */
    public class Executor {
        /**
         * Computation power of fog node (0.35 - 0.55 W).
         *
         * @see IotDevice#power
         * @see Defaults#FOG_COMPUTATION_POWER
         */
        final double power;
        /**
         * Computation rate of fog node (6 - 10 GHz).
         *
         * @see IotDevice#rate
         * @see Defaults#FOG_COMPUTATION_RATE
         */
        final double rate;
        /**
         * Number of CPU cores (execution quotas) in the fog node.
         *
         * @see Application#quota
         * @see Defaults#FOG_QUOTA
         */
        final int quota;
        /**
         * Fog computing capacity (6 - 10 GHz).
         *
         * @see Defaults#FOG_COMPUTING_CAPACITY
         */
        final double capacity;

        private Executor(double power, double rate, int quota, double capacity) {
            this.power = power;
            this.rate = rate;
            this.quota = quota;
            this.capacity = capacity;
        }

        /**
         * <pre>
         * C<sub>j</sub> = {@link #capacity} / {@link #quota} = 6GHz / 50.
         * </pre>
         *
         * @return Fog Node VRU Capacity (C<sub>j</sub>).
         */
        double computationCapacity() {
            return this.capacity / this.quota;
        }

        /**
         * This behaviour simulates the queueing at the fog node.
         *
         * @return Computing load density at the fog node.
         */
        protected double loadDensity() {
            double sum = 0.0;
            FogNode fog = FogNode.this;
            Queue<Application> queue = fog.queue;
            for (Application app : queue) {
                sum += app.computingLoad(fog);
            }
            StringJoiner joiner = queue.stream().map(app -> String.format("{id='%s',load=%e", app.id, app.computingLoad(fog))).collect(Utils.commaCollector);
            logger.log(Level.CONFIG, Utils.log("fog computing load", new Utils.LogPair("fog", fog.id), new Utils.LogPair("sum", sum), new Utils.LogPair("loads", joiner.toString())));
            return sum;
        }

        /**
         * resource utilisation = # VRUs used / fog total # VRUs
         *
         * @return FogNode resource utilisation as a fraction
         */
        protected double resourceUtilisation() {
            double size = FogNode.this.queue.size();
            double quota = this.quota;
            logger.fine(Utils.log("fog node resource util", new Utils.LogPair("fog", FogNode.this.id()), new Utils.LogPair("queue-size", FogNode.this.queue.size()), new Utils.LogPair("quota", this.quota)));
            return Math.min(1, size/quota);
        }
    }
}
