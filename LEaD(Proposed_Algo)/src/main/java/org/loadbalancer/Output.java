package org.loadbalancer;

import java.util.ArrayList;
import java.util.Formatter;

public record Output(double energy, double latency, ArrayList<Application> outages, ArrayList<Application> rejected, double utilisation) {
    public static <T extends Iterable<Output>> Output Join(T iterable) {
        double energy = 0;
        double latency = 0;
        double total = 0;
        double count = 0;
        ArrayList<Application> outages = new ArrayList<>();
        ArrayList<Application> rejected = new ArrayList<>();
        for (Output output : iterable) {
            energy += output.energy;
            latency += output.latency;
            outages.addAll(output.outages);
            rejected.addAll(output.rejected);
            total = output.utilisation;
            count++;
        }
        return new Output(energy, latency, outages, rejected, total/count);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("{energy:%e", this.energy);
        fmt.format(",latency:%e", this.latency);
        fmt.format(",outages:%s", Utils.join(this.outages));
        fmt.format(",rejected:%s", Utils.join(this.rejected));
        fmt.format(",utilised:%f%%", this.utilisation * 100);
        fmt.format("}");
        return fmt.toString();
    }
}
