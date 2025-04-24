package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FogController implements Authenticator.Verifiable, Identifiable {
    private final static Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
    /**
     * ID to uniquely identify the fog node controller.
     */
    public final String id;
    /**
     * SecretPart of the FNC to secure pairwise communications.
     */
    private final Authenticator.SecretPart secretPart;
    /**
     * List of fog nodes among whom tasks need to be scheduled.
     */
    ArrayList<FogNode> fogs;
    private boolean calcDone = false;
    private double energy = 0;
    private double latency = 0;
    private ArrayList<Application> outages;

    /**
     * @param id   Unique ID of the FNC.
     * @param fogs List of fog nodes load balanced by the FNC.
     */
    public FogController(String id, ArrayList<FogNode> fogs) {
        this.id = id;
        this.fogs = fogs;
        this.secretPart = Authenticator.generate();
    }

    /**
     * @return Average of computing load of every fog node under the FNC.
     */
    double averageLoad() {
        ArrayList<FogNode> fogs = this.fogs;
        double sum = 0;
        for (FogNode fog : fogs) {
            sum += fog.exec.loadDensity();
        }
        double avg = sum / fogs.size();
        StringJoiner joiner = fogs.stream().map(fog -> String.format("{id='%s',load=%e}", fog.id, fog.exec.loadDensity())).collect(Utils.commaCollector);
        logger.log(Level.CONFIG, Utils.log("fnc avg load", new Utils.LogPair("fnc", this.id), new Utils.LogPair("avg-load", avg), new Utils.LogPair("loads", joiner.toString())));
        return avg;
    }

    /**
     * @param app Incoming iot device application requesting to be executed.
     * @return true if app handled successfully, false if it were rejected.
     */
    boolean handleApp(Application app) {
        ArrayList<FogNode> list = this.fogs;
        list.sort(Comparator.comparingDouble(app::preference));
        StringJoiner joiner = list.stream().map(fog -> String.format("{id:'%s',pref:%e}", fog.id, app.preference(fog))).collect(Utils.commaCollector);
        logger.log(Level.CONFIG, Utils.log("sort fogs", new Utils.LogPair("fnc", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("fogs", joiner.toString())));
        for (FogNode fog : list) {
            boolean ok = this.authenticate(fog);
            if (!ok) {
                Authenticator.SecretPart self = this.secretPart();
                Authenticator.SecretPart other = fog.secretPart();
                logger.log(Level.WARNING, Utils.log("authentication failed", new Utils.LogPair("fnc", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("secrets", String.format("[%s,%s]", self, other))));
                continue;
            }
            double load = fog.exec.loadDensity();
            if (load >= Utils.COMPUTING_THRESHOLD) {
                logger.log(Level.WARNING, Utils.log("computing overload", new Utils.LogPair("fnc", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("load", load)));
                continue;
            }
            if (fog.isQueueFull()) {
                logger.log(Level.WARNING, Utils.log("queue full", new Utils.LogPair("fnc", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("fog", fog.id)));
                continue;
            }
            PriorityQueue<Application> queue = fog.queue;
            logger.log(Level.INFO, Utils.log("queue app", new Utils.LogPair("fnc", this.id), new Utils.LogPair("app", app.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("queue-size", queue.size()), new Utils.LogPair("queue-cap", fog.queueCapacity)));
            queue.add(app);
            return true; // success
        }
        return false; // reject
    }

    void calculateOutput() {
        this.calcDone = true;
        this.outages = new ArrayList<>();
        for (FogNode fog : this.fogs) {
            PriorityQueue<Application> queue = fog.queue;
            ArrayList<Application> list = new ArrayList<>(queue);
            list.sort(queue.comparator());
            StringJoiner joiner = list.stream().map(app -> String.format("{app:'%s',pref:%e", app.id, fog.preference(app))).collect(Utils.commaCollector);
            logger.log(Level.CONFIG, Utils.log("queue into sorted list", new Utils.LogPair("fnc", this.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("apps", joiner.toString())));
            for (Application app : list) {
                this.energy += app.energy(fog);
                double latency = app.latency(fog);
                int deadline = app.deadline;
                if (latency <= deadline) {
                    this.latency += latency;
                } else {
                    logger.log(Level.WARNING, Utils.log("app deadline exceeded", new Utils.LogPair("fnc", this.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("app", app.id), new Utils.LogPair("latency", latency), new Utils.LogPair("deadline", deadline)));
                    this.outages.add(app);
                }
            }
        }
    }

    double energy() {
        if (!this.calcDone) {
            this.calculateOutput();
        }
        return this.energy;
    }

    double latency() {
        if (!this.calcDone) {
            this.calculateOutput();
        }
        return this.latency;
    }

    ArrayList<Application> outages() {
        if (!this.calcDone) {
            this.calculateOutput();
        }
        return this.outages;
    }

    double resourceUtilisation() {
        List<FogNode> list = this.fogs;
        double util = 0.0;
        for (FogNode fog : list) {
            util += fog.exec.resourceUtilisation();
        }
        double avg = util / list.size();
        logger.fine(Utils.log("avg resource util", new Utils.LogPair("avg", util / list.size())));
        return avg;
    }

    public static FogController Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw new JsonParseException(parser, "expected FogController object", new IllegalStateException());
        }
        ArrayList<FogNode> fogNodes = new ArrayList<>();
        String id = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id":
                    id = parser.getText();
                    break;
                case "fogs":
                    if (!parser.hasToken(JsonToken.START_ARRAY)) {
                        throw new JsonParseException(parser, "expected FogNode list", new IllegalStateException());
                    }
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        fogNodes.add(FogNode.Parse(parser));
                    }
                    break;
                default:
                    throw new JsonParseException(parser, "unexpected field: "+field);
            }
        }
        if (id == null) {
            throw new JsonParseException(parser, "missing required 'id' field", new IllegalArgumentException());
        }
        return new FogController(id, fogNodes);
    }

    /**
     * @param fog Fog Node to secure communications with.
     * @return Whether authentication with the fog node successful or not.
     */
    private boolean authenticate(FogNode fog) {
        return Authenticator.verify(this, fog);
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
        fmt.format("}");
        return fmt.toString();
    }
}
