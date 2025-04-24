package org.loadbalancer;

import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Application implements Identifiable {
    final static Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
    /**
     * ID to uniquely identify the application.
     */
    public final String id;
    /**
     * Number of seconds of deadline for the execution of the task (30 - 60s).
     *
     * @see Defaults#APP_DEADLINE
     */
    final int deadline;
    /**
     * Average computing size of the task (0.05 Mbit).
     *
     * @see Defaults#APP_COMPUTING_SIZE
     */
    final double size;
    /**
     * How many quota it consumes on the fog node.
     *
     * @see FogNode.Executor#quota
     * @see Defaults#APP_QUOTA
     */
    final int quota;
    /**
     * The parent IoT device of which this application is a part of.
     */
    final IotDevice parent;

    /**
     * @see SimpleApplication
     * @see CompoundApplication
     */
    Application(String id, IotDevice parent, int deadline, double size, int quota) {
        this.id = id;
        this.parent = parent;
        this.deadline = deadline;
        this.size = size;
        this.quota = quota;
    }

    /**
     * Compute the total latency in transmission (if necessary) and execution.
     *
     * @param fog The FogNode on which the application is assumed to execute.
     * @return Total latency of task execution and offloading.
     */
    abstract double latency(FogNode fog);

    /**
     * Compute the total energy for transmission from (or to) IoT device
     * (if necessary) and execution of the task.
     *
     * @param fog FogNode on which application is assumed to execute.
     * @return Total energy consumed for task execution and offloading.
     */
    abstract double energy(FogNode fog);

    /**
     * @return The maximum of computation demands of all tasks involved
     * in the application.
     */
    abstract double computationDemand();

    /**
     * @param fog FogNode on which application is assumed to be executed.
     * @return Computing load density exerted on the assigned fog node by
     * this application.
     */
    double computingLoad(FogNode fog) {
        double numerator = this.parent.comm.arrivalRate * this.size;
        double load = numerator / fog.exec.computationCapacity();
        logger.log(Level.CONFIG, Utils.log("app computing load on fog", new Utils.LogPair("app", this.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("load", load)));
        return load;
    }

    /**
     * <pre>
     * w = ρ*ν / C*(1 - ρ).
     *
     * ρ: {@link FogNode.Executor#loadDensity() loadDensity}
     * ν: {@link #size}
     * C: {@link FogNode.Executor#computationCapacity() computingCapacity}
     * w: {@code waitingTime}
     * </pre>
     *
     * @param fog FogNode on which application is assumed to be executed.
     * @return The average waiting time for the application tasks at the
     * assigned fog node.
     * @see IotDevice.Communicator#waitingTime()
     */
    double waitingTime(FogNode fog) {
        double rhoCap = fog.exec.loadDensity();
        double numerator = rhoCap * this.size;
        double denominator = fog.exec.computationCapacity() * (1 - rhoCap);
        double time = numerator / denominator;
        logger.log(Level.CONFIG, Utils.log("app wait time at fog", new Utils.LogPair("app", this.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("rho-cap", rhoCap), new Utils.LogPair("wait-time", time)));
        return time;
    }


    /**
     * @param fog Different fog nodes for this given application evaluated
     *            under given criteria.
     * @return Preference of fog for this given application based on
     * criteria values (Energy, Latency, Deadline).
     * @see Utils#iotCriteriaWeights
     */
    double preference(FogNode fog) {
        double energy = this.energy(fog);
        double latency = this.latency(fog);
        double deadline = this.deadline;
        double[] values = {energy, latency, deadline};
        double preference = Utils.preference(values, Utils.iotCriteriaWeights);
        logger.log(Level.CONFIG, Utils.log("evaluate criteria preference", new Utils.LogPair("app", this.id), new Utils.LogPair("fog", fog.id), new Utils.LogPair("energy", energy), new Utils.LogPair("latency", latency), new Utils.LogPair("deadline", deadline), new Utils.LogPair("preference", preference)));
        return preference;
    }

    void appendKeys(Formatter fmt) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("{id:'%s'", this.id);
        fmt.format(",deadline:%d", this.deadline);
        fmt.format(",quota:%d", this.quota);
        fmt.format(",size:%e", this.size);
        this.appendKeys(fmt);
        fmt.format("}");
        return fmt.toString();
    }


    @Override
    public String id() {
        return this.id;
    }

    public abstract static class Builder {
        final String id;
        public int deadline = Defaults.APP_DEADLINE;
        public int quota = Defaults.APP_QUOTA;
        public double size = Defaults.APP_COMPUTING_SIZE;

        Builder(String id) {
            this.id = id;
        }

        abstract Application build(IotDevice parent);
    }
}
