package org.loadbalancer;

public class Coordinates {
    protected double x;
    protected double y;
    public Coordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public double distanceFrom(Coordinates other) {
        double x = this.x - other.x;
        double y = this.y - other.y;
        return Math.sqrt(x*x + y*y);
    }
    public static Coordinates Polar(double r, double theta) {
        return new Coordinates(r * Math.cos(theta), r * Math.sin(theta));
    }
    public String toString() {
        return String.format("(%f, %f)", this.x, this.y);
    }
}
