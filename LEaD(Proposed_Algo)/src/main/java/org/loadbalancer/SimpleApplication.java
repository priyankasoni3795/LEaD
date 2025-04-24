package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Formatter;
import java.util.logging.Level;

/**
 * SimpleApplication is a kind of Application that consists of single
 * independent task. So, the execution may happen in the following ways:
 * <ul>
 *     <li>local execution</li>
 *     <li>fog node execution</li>
 * </ul>
 */
public class SimpleApplication extends Application {
    /**
     * The single independent task that makes up the SimpleApplication.
     */
    final Task task;

    private SimpleApplication(
            // Mandatory parameters
            Task task, IotDevice parent,
            // Application parameters
            String id, int deadline, int quota, double size) {
        super(id, parent, deadline, size, quota);
        this.task = task;
    }

    @Override
    double latency(FogNode fog) {
        IotDevice device = this.parent;
        Task task = this.task;
        boolean isLocal = this.task.isLocal();
        int cases = isLocal ? 0 : 1;
        String logMessage = "app latency";
        double latency;
        if (isLocal) {
            /* local execution */
            latency = task.localX(device); // local exec
            logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("app", this.id), new Utils.LogPair("device", this.parent.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", cases), new Utils.LogPair("localX", latency)));
            return latency;
        } else {
            /* fog execution */
            double uplink = task.inputUplink(device); // upload input
            double downlink = task.downlink(device);  // download output
            double fogX = task.fogX(fog);             // fog exec
            double wait = Utils.waitingTime(this, fog);
            latency = uplink + downlink + fogX + wait;
            logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("app", this.id), new Utils.LogPair("device", this.parent.id), new Utils.LogPair("case", cases), new Utils.LogPair("fog", fog.id), new Utils.LogPair("uplink", uplink), new Utils.LogPair("downlink", downlink), new Utils.LogPair("fogX", fogX), new Utils.LogPair("wait", wait), new Utils.LogPair("latency", latency)));
            return latency;
        }
    }

    @Override
    double energy(FogNode fog) {
        IotDevice device = this.parent;
        Task task = this.task;
        String logMessage = "app energy";
        boolean isLocal = this.task.isLocal();
        int cases = isLocal ? 0 : 1;
        double energy;
        if (isLocal) {
            energy = device.power * task.localX(device);
            logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("app", this.id), new Utils.LogPair("device", this.parent.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("case", cases), new Utils.LogPair("energy", energy)));
            return energy;
        } else {
            double trans = task.inputUplink(device);
            trans += task.downlink(device);
            double transmission = device.comm.power * trans;
            double execution = fog.exec.power * task.fogX(fog);
            energy = transmission + execution;
            logger.log(Level.INFO, Utils.log(logMessage, new Utils.LogPair("app", this.id), new Utils.LogPair("device", this.parent.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("trans", transmission), new Utils.LogPair("exec", execution), new Utils.LogPair("energy", energy)));
            return energy;
        }
    }

    @Override
    double computationDemand() {
        return this.task.demand;
    }

    @Override
    void appendKeys(Formatter fmt) {
        fmt.format(",task:%s", this.task);
    }

    public static Builder Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected SimpleApplication object", new IllegalStateException());
        }
        String id = null;
        int deadline = 0;
        int quota = 0;
        double size = 0;
        Task task = null;
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
                case "task":
                    task = Task.Parse(parser);
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        if (task == null) {
            throw new JsonParseException(parser, "missing required 'task' field", new IllegalArgumentException());
        }
        Builder b = new Config(id, task);
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
        private final Task task;

        public Config(String id, Task task) {
            super(id);
            this.task = task;
        }

        @Override
        Application build(IotDevice parent) {
            return new SimpleApplication(
                    // mandatory parameters
                    this.task, parent,
                    // Application parameters
                    this.id, this.deadline, this.quota, this.size);
        }
    }
}
