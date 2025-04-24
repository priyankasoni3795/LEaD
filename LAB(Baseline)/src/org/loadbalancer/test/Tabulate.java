package org.loadbalancer.test;

import org.loadbalancer.*;
import java.util.ArrayList;

public class Tabulate {
    public static int iotGap = 20; // 25 metres
    public static int fogGap = 80; // 75 metres
    protected int size;
    public Tabulate(int size) {
        this.size = size;
    }
    protected void Run() {
        ArrayList<FogNode> fogNodes = this.generateFogNodes();
        ArrayList<IoTDevice> iotDevices = this.generateIoTDevices();
        LoadBalancer lb = new LoadBalancer(fogNodes, iotDevices);
        lb.Run();
    }
    protected ArrayList<IoTDevice> generateIoTDevices() {
        ArrayList<IoTDevice> list = new ArrayList<>();
        int max = this.size;
        int gap = iotGap;
        int start = 0;
        for (int i = 0; i < max; i++) {
            for (int j = 0; j < max; j++) {
                list.add(new IoTDevice(new Coordinates(start + i*gap, start + j*gap)));
            }
        }
        return list;
    }
    protected ArrayList<FogNode> generateFogNodes() {
        ArrayList<FogNode> list = new ArrayList<>();
        int max = Math.ceilDiv(this.size - 1, 4);
        int gap = fogGap;
        int start = 10;
        for (int i = 0; i < max; i++) {
            for (int j = 0; j < max; j++) {
                list.add(new FogNode(new Coordinates(start + i*gap, start + j*gap)));
            }
        }
        return list;
    }
}
