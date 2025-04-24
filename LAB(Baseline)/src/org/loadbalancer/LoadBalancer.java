package org.loadbalancer;

import java.util.ArrayList;

public class LoadBalancer {
    public ArrayList<FogNode> fogNodes;
    public ArrayList<IoTDevice> iotDevices;
    public ArrayList<ArrayList<IoTFogPair>> pairs;
    public ArrayList<ArrayList<Boolean>> association;
    public ArrayList<ArrayList<Boolean>> intmd_assoc;
    public LoadBalancer(ArrayList<FogNode> fogNodes, ArrayList<IoTDevice> iotDevices) {
        this.fogNodes = fogNodes;
        this.iotDevices = iotDevices;
        this.pairs = new ArrayList<>();
        this.association = new ArrayList<>();
        this.intmd_assoc = new ArrayList<>();
        for (FogNode fogNode: this.fogNodes) {
            ArrayList<IoTFogPair> pairs = new ArrayList<>();
            ArrayList<Boolean> assoc = new ArrayList<>();
            ArrayList<Boolean> intmd = new ArrayList<>();
            for (IoTDevice iotDevice: this.iotDevices) {
                pairs.add(new IoTFogPair(iotDevice, fogNode));
                assoc.add(false);
                intmd.add(false);
            }
            this.pairs.add(pairs);
            this.association.add(assoc);
            this.intmd_assoc.add(intmd);
        }
    }
    protected void initAssociation() throws Exception {
        for (int i = 0; i < this.iotDevices.size(); i++) {
            Coordinates iotDevice = this.iotDevices.get(i).coords;
            int closest = -1;
            double minDistance = Globals.range + 1;
            for (int j = 0; j < this.fogNodes.size(); j++) {
                Coordinates fogNode = this.fogNodes.get(j).coords;
                double distance = fogNode.distanceFrom(iotDevice);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = j;
                }
            }
            if (minDistance > Globals.range) {
                throw new Exception("IoT device not associated with any fog node");
            }
            this.intmd_assoc.get(closest).set(i, true);
        }
    }
    protected void farAssociation() throws Exception {
        for (int i = 0; i < this.iotDevices.size(); i++) {
            Coordinates iotDevice = this.iotDevices.get(i).coords;
            int farthest = -1;
            double maxDistance = 0;
            for (int j = 0; j < this.fogNodes.size(); j++) {
                Coordinates fogNode = this.fogNodes.get(j).coords;
                double distance = fogNode.distanceFrom(iotDevice);
                if (distance > maxDistance && distance <= Globals.range) {
                    maxDistance = distance;
                    farthest = j;
                }
            }
            if (farthest == -1) {
                throw new Exception("IoT device not associated with any fog node");
            }
            this.intmd_assoc.get(farthest).set(i, true);
        }
    }
    protected void calculateLoads() {
        for (int i = 0; i < this.fogNodes.size(); i++) {
            double trafficLoad = 0;
            double computingLoad = 0;
            for (int j = 0; j < this.iotDevices.size(); j++) {
                if (this.intmd_assoc.get(i).get(j)) {
                    IoTFogPair pair = this.pairs.get(i).get(j);
                    trafficLoad += pair.trafficLoadDensity();
                    computingLoad += pair.computingLoadDensity();
                }
            }
            FogNode fogNode = this.fogNodes.get(i);
            fogNode.trafficLoad = trafficLoad;
            fogNode.computingLoad = computingLoad;
        }
    }
    protected void BSSideAlgorithm() {
        for (int i = 0; i < this.pairs.size(); i++) {
            FogNode fogNode = this.fogNodes.get(i);
            double rho = 0;
            double rhoCap = 0;
            for (int j = 0; j < this.pairs.get(i).size(); j++) {
                IoTFogPair pair = this.pairs.get(i).get(j);
                if (this.intmd_assoc.get(i).get(j)) {
                    rho += pair.trafficLoadDensity();
                    rhoCap += pair.computingLoadDensity();
                }
            }
            fogNode.trafficLoad = rho;
            fogNode.computingLoad = rhoCap;
        }
    }
    protected void IoTSideAlgorithm() {
        for (int i = 0; i < this.iotDevices.size(); i++) {
            IoTDevice iotDevice = this.iotDevices.get(i);
            int pkx = -1;
            double maxValue = 0;
            for (int j = 0; j < this.fogNodes.size(); j++) {
                double value = getCrPhi(j, i, iotDevice);
                if (value > maxValue) {
                    maxValue = value;
                    pkx = j;
                }
            }
            this.association.get(pkx).set(i, true);
        }
    }
    public void Run() {
        try {
//            this.initAssociation(); // highly optimized
            this.farAssociation();
        } catch (Exception error) {
            error.printStackTrace();
            return;
        }
        this.BSSideAlgorithm();
        this.IoTSideAlgorithm();
        Output output = this.output();
        System.out.println(output);
    }
    private void assoc_average() {
        for(int i = 0; i < this.fogNodes.size(); i++) {
            for(int j = 0; j < this.iotDevices.size(); j++) {
                double sum = 0.0;
                if (this.association.get(i).get(j)) {
                    sum += 1 - Globals.beta;
                }
                if (this.intmd_assoc.get(i).get(j)) {
                    sum += Globals.beta;
                }
                this.intmd_assoc.get(i).set(j, sum >= 0.5);
            }
        }
    }
    private double getCrPhi(int i, int j, IoTDevice iotDevice) {
        FogNode fogNode = this.fogNodes.get(i);
        IoTFogPair pair = this.pairs.get(i).get(j);
        double diff = 1 - fogNode.trafficLoad;
        double diffCap = 1 - fogNode.computingLoad;
        double sq = diff * diff;
        double sqCap = diffCap * diffCap;
        double left = Globals.computingCapacity * iotDevice.trafficSize * sqCap;
        double right = pair.dataRate * iotDevice.computingSize * sq;
        double phi = (sq * sqCap) / (left + right);
        return Globals.computingCapacity * pair.dataRate * phi;
    }
    protected Output output() {
        double latency = 0.0;
        double energy = 0.0;
        int outages = 0;
        double epsilon = 0.0;
        double step = Globals.delta();
        for (int i = 0; i < this.iotDevices.size(); i++) {
            for (int j = 0; j < this.fogNodes.size(); j++) {
                if (this.association.get(j).get(i)) {
                    // System.out.printf("i%d <-> f%d\n", i+1, j+1); // association
                    IoTFogPair pair = pairs.get(j).get(i);
                    energy += pair.energyConsumption();
                    double localLatency = pair.latency();
                    if (localLatency+epsilon > pair.iotDevice.deadline) {
                        outages++;
                    }
                    latency += localLatency;
                }
                epsilon += step;
            }
        }
        return new Output(latency, energy, outages);
    }
}
