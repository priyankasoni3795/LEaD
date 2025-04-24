package org.loadbalancer.test;

import org.loadbalancer.*;
import java.util.ArrayList;

public class Circular {
    protected double ringSize = Globals.range / 4.0;
    public Circular(int size) {
        this.size = size;
    }
    private double shift = Math.PI / 10;
    protected ArrayList<FogNode> fogNodes = new ArrayList<>() {{
        add(new FogNode(ShiftCircular(ringSize * 1.0, Math.PI * 1.0 / 1.0)));
        add(new FogNode(ShiftCircular(ringSize * 2.0, Math.PI * 1.0 / 3.0)));
        add(new FogNode(ShiftCircular(ringSize * 2.0, Math.PI * 5.0 / 3.0)));
        add(new FogNode(ShiftCircular(ringSize * 3.0, Math.PI * 0.0 / 4.0)));
        add(new FogNode(ShiftCircular(ringSize * 3.0, Math.PI * 2.0 / 4.0)));
        add(new FogNode(ShiftCircular(ringSize * 3.0, Math.PI * 4.0 / 4.0)));
        add(new FogNode(ShiftCircular(ringSize * 3.0, Math.PI * 6.0 / 4.0)));
    }};
    protected int size;
    protected void Run() {
        ArrayList<IoTDevice> iotDevices = this.generateIoTDevices();
        LoadBalancer lb = new LoadBalancer(this.fogNodes, iotDevices);
        lb.Run();
    }
    protected ArrayList<IoTDevice> generateIoTDevices() {
        ArrayList<IoTDevice> devices = new ArrayList<>();
        double per_ring = this.size / 24.0;
        double unit = ringSize / per_ring;
        for (double i = 1; i*unit < 3*ringSize; i++) {
            double r = i * unit;
            devices.add(new IoTDevice(new Coordinates(0, r)));
            devices.add(new IoTDevice(new Coordinates(0, -r)));
            devices.add(new IoTDevice(new Coordinates(r, 0)));
            devices.add(new IoTDevice(new Coordinates(-r, 0)));
        }
        return devices;
    }
    private Coordinates ShiftCircular(double r, double theta) {
        theta = theta + shift;
        return new Coordinates(r * Math.cos(theta), r * Math.sin(theta));
    }
}
