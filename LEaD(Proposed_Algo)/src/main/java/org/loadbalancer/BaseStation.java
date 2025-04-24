package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseStation implements Identifiable {
    private final static Logger logger = Logger.getLogger(Utils.LOGGER_NAME);

    /**
     * ID to uniquely identify the base station.
     */
    public final String id;
    /**
     * Maximum capacity of the base station {@link #queue}.
     *
     * @see Defaults#BS_QUEUE_CAPACITY
     * @see #queue
     */
    final int capacity;
    /**
     * List of rejected tasks (apps).
     */
    final ArrayList<Application> rejected;
    /**
     * Queue of applications (tasks) waiting to be scheduled by the FNC
     * to execute on a fog node.
     *
     * @see #capacity
     */
    final ArrayDeque<Application> queue;
    /**
     * List of iot devices associated with the base station.
     */
    final ArrayList<IotDevice> devices;
    /**
     * Fog Node Controller with the base station that schedules iot device
     * applications (tasks) to execute on fog nodes.
     */
    final FogController fnc;
    /**
     * Communicator of the base station handles all the traffic
     * related stuff (loads, etc.).
     */
    final Communicator comm;

    public BaseStation(
            // mandatory parameters
            String id, ArrayList<IotDevice> list, FogController fnc,
            // optional parameters
            Config cfg) {
        this.id = id;
        this.capacity = cfg.capacity;
        this.devices = list;
        this.fnc = fnc;
        this.rejected = new ArrayList<>();
        this.queue = new ArrayDeque<>();
        this.comm = new Communicator(cfg.bandwidth, cfg.noise, cfg.power);
    }

    /**
     * Entrypoint for load balancing the iot device apps (and tasks)
     * on the fog nodes through FNC.
     */
    public void loadBalance() {
        for (IotDevice device : this.devices) {
            Application app = device.application;
            boolean ok = this.handleApp(app);
            if (!ok) {
                logger.log(Level.INFO, Utils.log("reject app", new Utils.LogPair("bs", this.id), new Utils.LogPair("iot", device.id), new Utils.LogPair("app", app.id)));
                this.rejected.add(app);
            }
        }
    }

    /**
     * @param app Incoming iot device application requesting to be executed.
     * @return true if app handled successfully, false if it were rejected.
     */
    boolean handleApp(Application app) {
        double load = this.fnc.averageLoad();
        if (load < Utils.COMPUTING_THRESHOLD) {
            logger.log(Level.INFO, Utils.log("forward app", new Utils.LogPair("bs", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("load", load)));
            return this.fnc.handleApp(app); // forward
        }
        if (!this.isQueueFull()) {
            ArrayDeque<Application> queue = this.queue;
            logger.log(Level.INFO, Utils.log("queue app", new Utils.LogPair("bs", this.id), new Utils.LogPair("size", queue.size()), new Utils.LogPair("capacity", this.capacity)));
            queue.add(app);
            return true; // success
        }
        logger.log(Level.WARNING, Utils.log("reject app", new Utils.LogPair("bs", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("reason", "queue is full")));
        return false; // reject
    }

    /**
     * @return whether {@link #queue} is full or not.
     */
    boolean isQueueFull() {
        return this.capacity <= this.queue.size();
    }

    public Output output() {
        FogController fnc = this.fnc;
        fnc.calculateOutput();
        return new Output(fnc.energy(), fnc.latency(), fnc.outages(), this.rejected, fnc.resourceUtilisation());
    }

    public static BaseStation Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected BaseStation object", new IllegalStateException());
        }
        String id = null;
        FogController fnc = null;
        ArrayList<Function<BaseStation, IotDevice>> list = new ArrayList<>();
        Config config = new Config();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id":
                    id = parser.getText();
                    break;
                case "queue_capacity":
                    config.capacity = parser.getIntValue();
                    break;
                case "noise_power":
                    config.noise = parser.getDoubleValue();
                    break;
                case "bandwidth":
                    config.bandwidth = parser.getDoubleValue();
                    break;
                case "transmission_power":
                    config.power = parser.getDoubleValue();
                    break;
                case "fnc":
                    fnc = FogController.Parse(parser);
                    break;
                case "devices":
                    if (!parser.hasToken(JsonToken.START_ARRAY)) {
                        throw new JsonParseException(parser, "expected IotDevice list", new IllegalStateException());
                    }
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        list.add(IotDevice.Parse(parser));
                    }
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        if (fnc == null) {
            throw new JsonParseException(parser, "missing required 'fnc' field", new IllegalArgumentException());
        }
        ArrayList<IotDevice> devices = new ArrayList<>();
        BaseStation bs = new BaseStation(id, devices, fnc, config);
        for (Function<BaseStation, IotDevice> fn: list) {
            devices.add(fn.apply(bs));
        }
        return bs;
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
        fmt.format(",capacity:%d", this.capacity);
        fmt.format(",noise:%e", this.comm.noise);
        fmt.format(",bandwidth:%e", this.comm.bandwidth);
        fmt.format(",power:%e", this.comm.power);
        fmt.format(",fnc:%s", this.fnc);
        fmt.format(",devices:%s", Utils.join(this.devices));
        fmt.format("}");
        return fmt.toString();
    }

    public static class Config {
        public int capacity = Defaults.BS_QUEUE_CAPACITY;
        public double noise = Defaults.BS_NOISE_POWER;
        public double bandwidth = Defaults.BS_BANDWIDTH;
        public double power = Defaults.BS_TRANSMISSION_POWER;
    }

    /**
     * Communicator groups together all the traffic related things of a
     * base station.
     */
    public class Communicator {
        /**
         * Bandwidth (10 MHz).
         *
         * @see Defaults#BS_BANDWIDTH
         */
        final double bandwidth;
        /**
         * Noise power (10<sup>-10</sup> W).
         *
         * @see Defaults#BS_NOISE_POWER
         */
        final double noise;
        /**
         * Transmission power of the base station (1 - 2 W).
         *
         * @see IotDevice.Communicator#power
         * @see Defaults#BS_TRANSMISSION_POWER
         */
        final double power;

        private Communicator(double bandwidth, double noise, double power) {
            this.bandwidth = bandwidth;
            this.noise = noise;
            this.power = power;
        }

        /**
         * This behaviour simulates the queueing at the base station.
         *
         * @return Traffic load density at the base station.
         */
        double loadDensity() {
            ArrayDeque<Application> queue = BaseStation.this.queue;
            double sum = 0.0;
            for (Application app : queue) {
                sum += app.parent.comm.load();
            }
            StringJoiner joiner = queue.stream().map(app -> String.format("{id='%s',load=%e}", app.parent.id, app.parent.comm.load())).collect(Utils.commaCollector);
            logger.log(Level.CONFIG, Utils.log("bs traffic load", new Utils.LogPair("bs", BaseStation.this.id), new Utils.LogPair("load", sum), new Utils.LogPair("loads", joiner.toString())));
            return sum;
        }
    }
}
