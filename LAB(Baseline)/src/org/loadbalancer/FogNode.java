package org.loadbalancer;

public class FogNode {
    protected double bandwidth = 1e7; // 10 MHz
    protected double computationPower = 0.55; // 0.35 - 0.55 W
    protected double transmissionPower = 1; // 1 - 2 W
    protected double trafficLoad;
    protected double computingLoad;
// TODO: convert them into function to get (mu --- micron)
//    protected double communicationLatencyRatio;
//    protected double computationLatencyRatio;
    protected Coordinates coords;
    public FogNode(Coordinates coords) {
        this.coords = coords;
    }
    protected double communicationLatencyRatio() {
        double rho = this.trafficLoad;
        return rho / (1 - rho);
    }
    protected double computationLatencyRatio() {
        double rhoCap = this.computingLoad;
        return rhoCap / (1 - rhoCap);
    }
    protected double energyConsumption(double downlinkTime, double serviceTime, double compTime) {
        double comm = this.transmissionPower * (downlinkTime + serviceTime);
        double comp = this.computationPower * compTime;
        return comm + comp;
    }
}
