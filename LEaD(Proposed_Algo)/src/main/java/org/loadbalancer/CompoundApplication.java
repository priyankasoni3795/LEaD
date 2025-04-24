package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.Formatter;
import java.util.logging.Level;

/**
 * CompoundApplication is a kind of Application that consists of an independent
 * and a dependent task. So, the execution may happen in the following ways:
 * <ul>
 *     <li>{@code 01} (local, fog)</li>
 *     <li>{@code 00} (local, local)</li>
 *     <li>{@code 10} (fog, local)</li>
 *     <li>{@code 11} (fog, fog)</li>
 * </ul>
 */
public class CompoundApplication extends Application {
    /**
     * The independent task of this application.
     */
    final Task independent;
    /**
     * A task of this application that depends on {@link #independent}.
     */
    final Task dependent;

    private CompoundApplication(
            // Mandatory parameters
            Task independent, Task dependent, IotDevice parent,
            // Application parameters
            String id, int deadline, int quota, double size) {
        super(id, parent, deadline, size, quota);
        this.independent = independent;
        this.dependent = dependent;
    }

    private static double logEnergy(String msg, double trans, double exec, Utils.LogPair... pairs) {
        double energy = trans + exec;
        ArrayUtils.addAll(pairs,                    //
                new Utils.LogPair("trans", trans),  //
                new Utils.LogPair("exec", exec),    //
                new Utils.LogPair("energy", energy) //
        );
        logger.log(Level.INFO, Utils.log(msg, pairs));
        return energy;
    }

    @Override
    double latency(FogNode fog) {
        IotDevice device = this.parent;
        Task one = this.independent;
        Task two = this.dependent;
        boolean oneLocal = one.isLocal();
        boolean twoLocal = two.isLocal();
        int cases = (oneLocal ? 0 : 1) | (twoLocal ? 0 : 2);
        String logMessage = "app latency";
        double latency;
        if (oneLocal) {
            if (twoLocal) {
                double localX1 = one.localX(device);
                double uplink1 = one.outputUplink(device);
                double downlink2 = two.downlink(device);
                double localX2 = two.localX(device);
                latency = localX1 + uplink1 + downlink2 + localX2;
                logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", Integer.toBinaryString(cases)), new Utils.LogPair("localX1", localX1), new Utils.LogPair("uplink1", uplink1), new Utils.LogPair("downlink2", downlink2), new Utils.LogPair("localX2", localX2), new Utils.LogPair("latency", latency)));
                return latency;
            } else {
                double localX1 = one.localX(device);
                double uplink1 = one.outputUplink(device);
                double uplink2 = two.inputUplink(device);
                double fogX2 = two.fogX(fog);
                double wait = Utils.waitingTime(this, fog);
                latency = localX1 + uplink1 + uplink2 + fogX2 + wait;
                logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", Integer.toBinaryString(cases)), new Utils.LogPair("localX1", localX1), new Utils.LogPair("uplink1", uplink1), new Utils.LogPair("uplink2", uplink2), new Utils.LogPair("fogX2", fogX2), new Utils.LogPair("wait", wait), new Utils.LogPair("latency", latency)));
                return latency;
            }
        } else {
            if (twoLocal) {
                double uplink1 = one.inputUplink(device);
                double fogX1 = one.fogX(fog);
                double downlink1 = one.downlink(device);
                double localX2 = two.localX(device);
                double wait = Utils.waitingTime(this, fog);
                latency = uplink1 + fogX1 + downlink1 + localX2 + wait;
                logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", Integer.toBinaryString(cases)), new Utils.LogPair("uplink1", uplink1), new Utils.LogPair("fogX1", fogX1), new Utils.LogPair("downlink1", downlink1), new Utils.LogPair("localX2", localX2), new Utils.LogPair("wait", wait), new Utils.LogPair("latency", latency)));
                return latency;
            } else {
                double uplink1 = one.inputUplink(device);
                double uplink2 = two.inputUplink(device);
                double fogX1 = one.fogX(fog);
                double fogX2 = two.fogX(fog);
                double wait = Utils.waitingTime(this, fog);
                latency = uplink1 + uplink2 + fogX1 + fogX2 + wait;
                logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", Integer.toBinaryString(cases)), new Utils.LogPair("uplink1", uplink1), new Utils.LogPair("uplink2", uplink2), new Utils.LogPair("fogX1", fogX1), new Utils.LogPair("fogX2", fogX2), new Utils.LogPair("wait", wait), new Utils.LogPair("latency", latency)));
                return latency;
            }
        }
    }

    @Override
    double energy(FogNode fog) {
        IotDevice device = this.parent;
        Task one = this.independent;
        Task two = this.dependent;
        boolean oneLocal = one.isLocal();
        boolean twoLocal = two.isLocal();
        int cases = (oneLocal ? 0 : 1) | (twoLocal ? 0 : 2);
        String logMessage = "app energy";
        double energy, trans, execution;
        if (oneLocal) {
            trans = one.outputUplink(device);
            execution = device.comm.power * one.localX(device);
            if (twoLocal) {
                trans += one.downlink(device);
                trans = device.comm.power * trans;
                execution += device.comm.power * two.localX(device);
            } else {
                trans += two.inputUplink(device);
                trans = device.comm.power * trans;
                execution += fog.exec.power * two.fogX(fog);
            }
        } else {
            trans = one.inputUplink(device);
            execution = fog.exec.power * one.fogX(fog);
            if (twoLocal) {
                trans += one.downlink(device);
                trans = device.comm.power * trans;
                execution += device.power * two.localX(device);
            } else {
                trans += two.inputUplink(device);
                trans = device.comm.power * trans;
                execution += fog.exec.power * two.fogX(fog);
            }
        }
        energy = trans + execution;
        logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", Integer.toBinaryString(cases)), new Utils.LogPair("trans", trans), new Utils.LogPair("exec", execution), new Utils.LogPair("energy", energy)));
        return energy;
    }

    @Override
    double computationDemand() {
        double one = this.independent.demand;
        double two = this.dependent.demand;
        return Math.max(one, two);
    }

    @Override
    void appendKeys(Formatter fmt) {
        fmt.format(",independent:%s", this.independent);
        fmt.format(",dependent:%s", this.dependent);
    }

    public static Builder Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected CompoundApplication object", new IllegalStateException());
        }
        String id = null;
        int deadline = 0;
        int quota = 0;
        double size = 0;
        Task independent = null;
        Task dependent = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id":
                    id = parser.getText();
                    break;
                case "deadline":
                    deadline = parser.getIntValue();
                    break;
                case "quota":
                    quota = parser.getIntValue();
                    break;
                case "size":
                    size = parser.getDoubleValue();
                    break;
                case "dept":
                    dependent = Task.Parse(parser);
                    break;
                case "indept":
                    independent = Task.Parse(parser);
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        if (dependent == null) {
            throw new JsonParseException(parser, "missing required 'dept' field", new IllegalArgumentException());
        }
        if (independent == null) {
            throw new JsonParseException(parser, "missing required 'indept' field", new IllegalArgumentException());
        }
        Builder b = new Config(id, independent, dependent);
        if (deadline != 0) {
            b.deadline = deadline;
        }
        if (quota != 0) {
            b.quota = quota;
        }
        if (size != 0) {
            b.size = size;
        }
        return b;
    }

    public static class Config extends Application.Builder {
        private final Task independent;
        private final Task dependent;

        public Config(String id, Task independent, Task dependent) {
            super(id);
            this.independent = independent;
            this.dependent = dependent;
        }

        @Override
        Application build(IotDevice parent) {
            return new CompoundApplication(
                    // mandatory parameters
                    this.independent, this.dependent, parent,
                    // Application parameters
                    this.id, this.deadline, this.quota, this.size);
        }
    }
}
