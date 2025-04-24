package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Formatter;

public class Task {
    /**
     * Output size of the task (10 - 20 Kb).
     *
     * @see Defaults#TASK_OUTPUT_SIZE
     */
    final double output;
    /**
     * Input size of the task (300 - 600 Kb).
     *
     * @see Defaults#TASK_INPUT_SIZE
     */
    final double input;
    /**
     * The computation demand of the task (210 - 480 million CPU cycles).
     */
    final double demand;

    public Task(double demand, Config cfg) {
        this.demand = demand;
        this.input = cfg.input;
        this.output = cfg.output;
    }

    /**
     * @return Determine whether the task has to execute locally or on the fog node.
     */
    boolean isLocal() {
        return this.demand <= Utils.DEMAND_DETERMINER;
    }

    /**
     * @return Time taken to execute the task locally.
     */
    double localX(IotDevice device) {
        return this.demand / device.rate;
    }

    /**
     * @return Time taken to execute the task on fog node.
     */
    double fogX(FogNode fog) {
        return this.demand / fog.exec.computationCapacity();
    }

    /**
     * @return Time taken to send the task input to the fog node.
     */
    double inputUplink(IotDevice device) {
        double dataRate = device.comm.uplinkDataRate();
        return Utils.transmissionTime(this.input, dataRate);
    }

    /**
     * @return Time taken to send the task output to the fog node.
     */
    double outputUplink(IotDevice device) {
        double dataRate = device.comm.uplinkDataRate();
        return Utils.transmissionTime(this.output, dataRate);
    }

    /**
     * @return Time taken to receive the output from the fog node.
     */
    double downlink(IotDevice device) {
        double dataRate = device.comm.downlinkDataRate();
        return Utils.transmissionTime(this.output, dataRate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("{demand:%e", this.demand);
        fmt.format(",output:%e", this.output);
        fmt.format(",input:%e", this.input);
        fmt.format("}");
        return fmt.toString();
    }

    public static Task Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected Task object", new IllegalStateException());
        }
        Config config = new Config();
        double demand = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "input":
                    config.input = parser.getDoubleValue();
                    break;
                case "output":
                    config.output = parser.getDoubleValue();
                    break;
                case "demand":
                    demand = parser.getDoubleValue();
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (demand == 0) {
            throw new JsonParseException(parser, "missing required field 'demand'", new IllegalArgumentException());
        }
        return new Task(demand, config);
    }

    public static class Config {
        public double input = Defaults.TASK_INPUT_SIZE;
        public double output = Defaults.TASK_OUTPUT_SIZE;
    }
}
