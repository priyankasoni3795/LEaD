package org.loadbalancer.test;

public class MainRunner {
    public static void main(String[] args) {
        int fog = 7;
        int iot = 1000;
        int[] radii = {6, 10, 14, 18, 22};
        for (int r : radii) {
            new FarCircular(fog, iot, r).Run();
            new FarCircular(fog, iot, r).Run();
            new FarCircular(fog, iot, r).Run();
        }
    }
}
