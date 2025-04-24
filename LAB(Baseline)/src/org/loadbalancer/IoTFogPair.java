package org.loadbalancer;

public class IoTFogPair {
    /* TODO: make dataRate member memo cached */
    protected double dataRate;
    protected IoTDevice iotDevice;
    protected FogNode fogNode;
    public IoTFogPair(IoTDevice iotDevice, FogNode fogNode) {
        this.iotDevice = iotDevice;
        this.fogNode = fogNode;
        this.dataRate = this.dataRate(iotDevice.transmissionPower);
    }
    protected double channelGain() {
        double numerator = 1.5776e-4; // TODO: constant (static final?)
        Coordinates fogNode = this.fogNode.coords;
        Coordinates iotDevice = this.iotDevice.coords;
        double distance = iotDevice.distanceFrom(fogNode);
        if (distance > Globals.range) {
            return 0;
        }
        double denominator = distance * distance;
        return numerator / denominator;
    }
    protected double signalToNoiseRatio(double power) {
        double noise = Globals.noisePower;
        double gain = this.channelGain();
        return (power * gain) / noise;
    }
    protected double dataRate(double power) {
        double bandwidth = this.fogNode.bandwidth;
        double snr = this.signalToNoiseRatio(power);
        return bandwidth * Math.log10(1 + snr);
    }
    protected double trafficLoadDensity() {
        IoTDevice ioTDevice = this.iotDevice;
        return (iotDevice.flowArrivalRate * iotDevice.trafficSize) / this.dataRate;
    }
    protected double computingLoadDensity() {
        IoTDevice ioTDevice = this.iotDevice;
        return (iotDevice.flowArrivalRate * iotDevice.computingSize) / Globals.computingCapacity;
    }
    protected double energyConsumption() {
        IoTDevice ioTDevice = this.iotDevice;
        FogNode fogNode = this.fogNode;
        double serviceTime = ioTDevice.trafficSize / this.dataRate(ioTDevice.transmissionPower);
        double compTime = ioTDevice.computationDemand / Globals.computingCapacity;
        double uplinkTime = Globals.inputSize / this.dataRate(ioTDevice.transmissionPower);
        double downlinkTime = Globals.outputSize / this.dataRate(fogNode.transmissionPower);
//        return iotDevice.energyConsumption(uplinkTime) + fogNode.energyConsumption(uplinkTime, downlinkTime, serviceTime, compTime);
        double iotEnergy = ioTDevice.energyConsumption(uplinkTime);
        double fogEnergy = fogNode.energyConsumption(downlinkTime, serviceTime, compTime);
//        System.out.printf("uplink: %f; downlink: %f; service: %f; comp: %f;", uplinkTime, downlinkTime, serviceTime, compTime);
//        System.out.printf("iot energy: %f; fog energy: %f;\n", iotEnergy, fogEnergy);
        double energy = iotEnergy + fogEnergy;
//        Globals.logger.info(String.format("pair energy consumption;data-rate=%e", this.dataRate));
        return energy;
    }
    protected double latency() {
        IoTDevice device = this.iotDevice;
        double capacity = Globals.computingCapacity;
        double computation = device.computationDemand / capacity;
        double rate = this.dataRate(device.transmissionPower);
        double uplink = Globals.inputSize / rate;
        double downlink = Globals.outputSize / rate;
        double rho = this.trafficLoadDensity();
        double rhoCap = this.computingLoadDensity();
        double commWaiting = (rho * device.trafficSize) / (rate * (1 - rho));
        double compWaiting = (rhoCap * device.computingSize) / (capacity * (1-rhoCap));
        return computation + uplink + downlink + commWaiting + compWaiting;
    }
}
