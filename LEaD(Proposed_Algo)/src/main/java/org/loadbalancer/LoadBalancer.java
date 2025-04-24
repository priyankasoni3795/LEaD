package org.loadbalancer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;

public class LoadBalancer {
    /**
     * List of base stations involved in the load balancing topology.
     */
    final ArrayList<BaseStation> stations;

    /**
     * @param stations List of stations in the load balancing topology.
     */
    public LoadBalancer(ArrayList<BaseStation> stations) {
        this.stations = stations;
    }

    /**
     * Perform load balancing for the given topology.
     */
    Output Do() {
        ArrayList<Output> outputs = new ArrayList<>(this.stations.size());
        for (BaseStation bs : this.stations) {
            bs.loadBalance();
            outputs.add(bs.output());
        }
        return Output.Join(outputs);
    }

    public static LoadBalancer Parse(JsonParser parser) throws IOException {
        if (!parser.hasToken(JsonToken.START_ARRAY)) {
            throw new JsonParseException(parser, "expected BaseStation list", new IllegalStateException());
        }
        ArrayList<BaseStation> list = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            list.add(BaseStation.Parse(parser));
        }
        return new LoadBalancer(list);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("{stations:%s", Utils.join(this.stations));
        fmt.format("}");
        return fmt.toString();
    }
}
