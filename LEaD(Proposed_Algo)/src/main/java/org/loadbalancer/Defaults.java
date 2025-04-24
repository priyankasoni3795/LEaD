package org.loadbalancer;

public final class Defaults {
    /**
     * @see BaseStation#capacity
     */
    static final int BS_QUEUE_CAPACITY = 200;
    /**
     * @see BaseStation.Communicator#noise
     */
    static final double BS_NOISE_POWER = 1e-10;
    /**
     * @see BaseStation.Communicator#bandwidth
     */
    static final double BS_BANDWIDTH = 10e6;
    /**
     * @see BaseStation.Communicator#power
     */
    static final double BS_TRANSMISSION_POWER = 1;
    /**
     * @see Task#input
     */
    static final double TASK_INPUT_SIZE = 300e3;
    /**
     * @see Task#output
     */
    static final double TASK_OUTPUT_SIZE = 10e3;
    /**
     * @see Application#deadline
     */
    static final int APP_DEADLINE = 30;
    /**
     * @see Application#size
     */
    static final double APP_COMPUTING_SIZE = 0.05e6;
    /**
     * @see Application#quota
     */
    static final int APP_QUOTA = 1;
    /**
     * Both transmission power and computation power?
     *
     * @see IotDevice#power
     * @see IotDevice.Communicator#power
     */
    static final double IOT_POWER = 0.5;
    /**
     * @see IotDevice#rate
     */
    static final double IOT_ARDUINO_FREQUENCY = 16e6;
    /**
     * @see IotDevice.Communicator#arrivalRate
     */
    static final double IOT_FLOW_ARRIVAL_RATE = 0.5;
    /**
     * @see IotDevice.Communicator#trafficSize
     */
    static final double IOT_TRAFFIC_SIZE = 0.05e6;
    /**
     * @see IotDevice.Communicator#distance
     */
    static final double IOT_DISTANCE = 100;
    /**
     * Should be at least as large as {@link #FOG_QUOTA}.
     *
     * @see FogNode#queueCapacity
     */
    static final int FOG_QUEUE_CAPACITY = 120;
    /**
     * @see FogNode.Executor#power
     */
    static final double FOG_COMPUTATION_POWER = 0.4;
    /**
     * @see FogNode.Executor#rate
     */
    static final double FOG_COMPUTATION_RATE = 6e9;
    /**
     * @see FogNode.Executor#quota
     */
    static final int FOG_QUOTA = 6;
    /**
     * @see FogNode.Executor#capacity
     */
    static final double FOG_COMPUTING_CAPACITY = 6e9;

    /**
     * Prevent instantiation by defining a private constructor.
     */
    private Defaults() {
        final String summary = "cannot be instantiated";
        final String detail = "Defaults is a constants class";
        final String message = String.format("%s:%s.", summary, detail);
        throw new AssertionError(message);
    }
}
