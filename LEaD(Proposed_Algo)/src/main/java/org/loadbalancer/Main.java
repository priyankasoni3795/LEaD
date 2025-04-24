package org.loadbalancer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        setupLogger();
        try {
            JsonParser parser = new JsonFactory().createParser(System.in);
            parser.nextToken();
            LoadBalancer lb = LoadBalancer.Parse(parser);
            Output output = lb.Do();
            System.out.printf("%.5f,%.5f,",output.latency(), output.energy());
            System.out.printf("%d,%d,%.2f%%\n", output.outages().size(), output.rejected().size(), output.utilisation() * 100);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    private static void setupLogger() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.OFF);
    }
}
