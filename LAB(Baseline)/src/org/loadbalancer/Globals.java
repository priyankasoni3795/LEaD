package org.loadbalancer;

import java.security.SecureRandom;
import java.util.logging.Logger;

public class Globals {
    public static final Logger logger = Logger.getLogger("");
    public static double noisePower = 1e-9; // -100dB
    public static double range = 1000; // metres
    public static double computingCapacity = 1.2e8; // 0.12 GHz
//    public static double computingCapacity = 6e9; // 6 GHz
    public static double beta = 0.4;
    public static int n_iterations = 1;
    public static double maxTrafficLoad = 0.99;
    public static double maxComputingLoad = 0.99;
    public static double inputSize = 4e5; // 400 kbit
    public static double outputSize = 15e3; // 15 kbit
    public static double delta() {
        return new SecureRandom().nextDouble(0.003725, 0.00375); // 1000
//        return new SecureRandom().nextDouble(0.00495, 0.005); // 750
//        return new SecureRandom().nextDouble(0.00749, 0.00751);   // 500
//        return new SecureRandom().nextDouble(0.0151, 0.0152);   // 250
    }
}
