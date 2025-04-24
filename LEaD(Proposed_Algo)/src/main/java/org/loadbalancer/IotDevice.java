package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Formatter;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IotDevice implements Identifiable {
    private static final Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
    /**
     * ID to uniquely identify the iot device.
     */
    public final String id;
    /**
     * Computation power of IotDevice (0.1 - 1 W).
     *
     * @see IotDevice.Communicator#power
     * @see FogNode.Executor#power
     */
    final double power;
    /**
     * Computation rate or Arduino frequency (local execution) (16 MHz).
     *
     * @see FogNode.Executor#rate
     */
    final double rate;
    /**
     * The base station to which {@code this} IotDevice is assigned to.
     */
    final BaseStation assignedBS;
    /**
     * The application that runs on this IoT Device.
     */
    final Application application;
    /**
     * Communicator of the IotDevice handles all the traffic-related stuff.
     */
    final Communicator comm;

    public IotDevice(String id, BaseStation bs, Application.Builder app, Config cfg) {
        this.id = id;
        this.power = cfg.cPower;
        this.rate = cfg.rate;
        this.assignedBS = bs;
        this.comm = new Communicator(cfg.arrival, cfg.traffic, cfg.tPower, cfg.distance);
        this.application = app.build(this);
    }

    public static Function<BaseStation, IotDevice> Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected IotDevice object", new IllegalStateException());
        }
        String id = null;
        Application.Builder app = null;
        Config config = new Config();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id":
                    id = parser.getText();
                    break;
                case "computation_power":
                    config.cPower = parser.getDoubleValue();
                    break;
                case "computation_rate":
                    config.rate = parser.getDoubleValue();
                    break;
                case "arrival_rate":
                    config.arrival = parser.getDoubleValue();
                    break;
                case "traffic_size":
                    config.traffic = parser.getDoubleValue();
                    break;
                case "distance":
                    config.distance = parser.getDoubleValue();
                    break;
                case "transmission_power":
                    config.tPower = parser.getDoubleValue();
                    break;
                case "simple_app":
                    app = SimpleApplication.Parse(parser);
                    break;
                case "compound_app":
                    app = CompoundApplication.Parse(parser);
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        if (app == null) {
            throw new JsonParseException(parser, "missing required 'simple_app' or 'compound_app' fields", new IllegalArgumentException());
        }
        Application.Builder finalApp = app;
        String finalId = id;
        return bs -> new IotDevice(finalId, bs, finalApp, config);
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
        fmt.format(",computation_power:%e", this.power);
        fmt.format(",computing_rate:%e", this.rate);
        fmt.format(",arrival_rate:%e", this.comm.arrivalRate);
        fmt.format(",traffic_size:%e", this.comm.trafficSize);
        fmt.format(",distance:%e", this.comm.distance);
        fmt.format(",transmission_power:%e", this.comm.power);
        fmt.format("}");
        return fmt.toString();
    }

    public static class Config {
        public double cPower = Defaults.IOT_POWER;
        public double rate = Defaults.IOT_ARDUINO_FREQUENCY;
        public double arrival = Defaults.IOT_FLOW_ARRIVAL_RATE;
        public double traffic = Defaults.IOT_TRAFFIC_SIZE;
        public double distance = Defaults.IOT_DISTANCE;
        public double tPower = Defaults.IOT_POWER;
    }

    public class Communicator {
        /**
         * Flow arrival rate per unit area at the location of the IoT
         * device (0.5 flows/second).
         *
         * @see Defaults#IOT_FLOW_ARRIVAL_RATE
         */
        final double arrivalRate;
        /**
         * The average traffic size of flow at the location of the IoT
         * device (0.05 Mbit).
         *
         * @see Defaults#IOT_TRAFFIC_SIZE
         */
        final double trafficSize;
        /**
         * Maximum transmission power of IoT device (0.1 - 1 W).
         *
         * @see BaseStation.Communicator#power
         * @see Defaults#IOT_POWER
         */
        final double power;
        /**
         * The distance of the IoT device from the assigned base station.
         * Default value is presumed to be {@value} metres.
         *
         * @see Defaults#IOT_DISTANCE
         */
        final double distance;

        private Communicator(double arrivalRate, double trafficSize, double power, double distance) {
            this.arrivalRate = arrivalRate;
            this.trafficSize = trafficSize;
            this.power = power;
            this.distance = distance;
        }

        /**
         * @return Traffic load density exerted on the assigned base station by this
         * IoT device.
         */
        double load() {
            double rate = this.uplinkDataRate();
            double arrival = this.arrivalRate;
            double traffic = this.trafficSize;
            double load = (arrival * traffic) / rate;
            logger.log(Level.CONFIG, Utils.log("device traffic load", new Utils.LogPair("device", IotDevice.this.id), new Utils.LogPair("bs", IotDevice.this.assignedBS.id), new Utils.LogPair("data-rate", rate), new Utils.LogPair("flow-arrival", arrival), new Utils.LogPair("traffic-size", traffic), new Utils.LogPair("traffic-load", load)));
            return load;
        }

        /**
         * <pre>
         * s = l / r.
         * t = l / r*(1 - ρ).
         * w = s - t = ρ*l / r*(1 - ρ).
         *
         * l: {@link #trafficSize}
         * r: {@link #uplinkDataRate() dataRate}
         * ρ: {@link BaseStation.Communicator#loadDensity() loadDensity}
         * s: {@code serviceTime}
         * t: {@code deliveryTime}
         * w: {@code waitingTime}
         * </pre>
         *
         * @return The average waiting time at the base station for
         * {@code this} IotDevice.
         * @see Application#waitingTime(FogNode)
         */
        double waitingTime() {
            double traffic = this.trafficSize;
            double rate = this.uplinkDataRate();
            double rho = IotDevice.this.assignedBS.comm.loadDensity();
            double numerator = rho * traffic;
            double denominator = rate * (1 - rho);
            double wait = numerator / denominator;
            logger.log(Level.CONFIG, Utils.log("device comm waiting time", new Utils.LogPair("device", IotDevice.this.id), new Utils.LogPair("bs", IotDevice.this.assignedBS.id), new Utils.LogPair("traffic-size", traffic), new Utils.LogPair("rho", rho), new Utils.LogPair("data-rate", rate), new Utils.LogPair("wait-time", wait)));
            return wait;
        }

        /**
         * @return Uplink Data Rate of the IotDevice towards the assigned
         * BaseStation.
         */
        double uplinkDataRate() {
            return this.dataRate(this.power);
        }

        /**
         * @return Downlink data rate of transmitting data from base station
         * to the IoT Device.
         */
        double downlinkDataRate() {
            BaseStation.Communicator bs = IotDevice.this.assignedBS.comm;
            return this.dataRate(bs.power);
        }

        /**
         * @param power Transmission power
         * @return Data Rate of transmission between the IotDevice and BaseStation.
         */
        private double dataRate(double power) {
            BaseStation.Communicator bs = IotDevice.this.assignedBS.comm;
            double gain = Utils.channelGain(this.distance);
            double noise = bs.noise;
            double bandwidth = bs.bandwidth;
            double snr = Utils.snr(power, gain, noise);
            double rate = Utils.dataRate(bandwidth, snr);
            logger.log(Level.CONFIG, Utils.log("device data rate", new Utils.LogPair("device", IotDevice.this.id), new Utils.LogPair("bs", IotDevice.this.assignedBS.id), new Utils.LogPair("power", power), new Utils.LogPair("distance", this.distance), new Utils.LogPair("channel-gain", gain), new Utils.LogPair("noise-power", noise), new Utils.LogPair("bandwidth", bandwidth), new Utils.LogPair("snr", snr), new Utils.LogPair("data-rate", rate)));
            return rate;
        }
    }
}
