package org.loadbalancer;

public record Output(double latency, double energy, int outages) {
    @Override
    public String toString() {
        return String.format("%.5f,%.5f,%d", latency, energy, outages);
    }
}
