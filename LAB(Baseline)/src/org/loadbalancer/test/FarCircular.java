package org.loadbalancer.test;

import org.loadbalancer.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class FarCircular {
    protected int fn;
    protected int devices;
    protected double fogRadius = 10;
    public FarCircular(int fn, int devices) {
        this.fn = fn;
        this.devices = devices;
    }
    public FarCircular(int fn, int devices, int radius) {
        this.fn = fn;
        this.devices = devices;
        this.fogRadius = radius;
    }
    protected void Run() {
        LoadBalancer lb = new LoadBalancer(this.fogNodes(), this.ioTDevices());
        lb.Run();
    }
    protected ArrayList<FogNode> fogNodes() {
        ArrayList<FogNode> list = new ArrayList<>();
        Random rng = new SecureRandom();
        for (int i = 0; i < this.fn; i++) {
            double radius = rng.nextDouble(this.fogRadius);
            double theta = rng.nextDouble(2 * Math.PI);
            list.add(new FogNode(Coordinates.Polar(radius, theta)));
        }
        return list;
    }
    protected ArrayList<IoTDevice> ioTDevices() {
        ArrayList<IoTDevice> list = new ArrayList<>();
        double unit = 2 * Math.PI / this.devices;
        for (int i = 0; i < this.devices; i++) {
            double radius = Globals.range - this.fogRadius;
            list.add(new IoTDevice(Coordinates.Polar(radius, unit*i)));
        }
        return list;
    }
}
