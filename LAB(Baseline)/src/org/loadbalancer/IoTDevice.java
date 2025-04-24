package org.loadbalancer;

import java.security.SecureRandom;

public class IoTDevice {
    protected double flowArrivalRate = 0.5; // per second
    protected double trafficSize = 5e4; // 0.05 Mbit
    protected double computingSize = 7e6; // cpu cycles
    protected double computationDemand = 400e6; // 300 - 480 million cycles
    protected double transmissionPower = 0.5; // 0.5 W
    protected double deadline = 30; // 30s - 60s
    protected Coordinates coords;
    public IoTDevice(Coordinates coords) {
        this.coords = coords;
        this.computationDemand = new SecureRandom().nextDouble(400e6, 420e6);
    }
    protected double energyConsumption(double uplinkTime) {
        return this.transmissionPower * uplinkTime;
    }
}
