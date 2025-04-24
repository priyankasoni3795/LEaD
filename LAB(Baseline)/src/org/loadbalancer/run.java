package org.loadbalancer;

import java.util.ArrayList;

public class run {
    public static void main(String[] args) {
        ArrayList<FogNode> fogNodes = new ArrayList<FogNode>(){{
            add(new FogNode(new Coordinates(50, 100)));
            add(new FogNode(new Coordinates(100, 200)));
            add(new FogNode(new Coordinates(150, 50)));
        }};
        ArrayList<IoTDevice> iotDevices = new ArrayList<IoTDevice>(){{
            add(new IoTDevice(new Coordinates(50, 150)));
            add(new IoTDevice(new Coordinates(100, 50)));
            add(new IoTDevice(new Coordinates(100, 100)));
            add(new IoTDevice(new Coordinates(150, 200)));
            add(new IoTDevice(new Coordinates(200, 50)));
        }};
        LoadBalancer lb = new LoadBalancer(fogNodes, iotDevices);
        lb.Run();
    }
}

