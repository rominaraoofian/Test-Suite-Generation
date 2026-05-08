package de.uni_passau.fim.se2.sbse.suite_generation.examples;

public class SimpleExample {

    private Integer index;

    public SimpleExample(Integer index) {
        this.index = index;
    }

    public void doStuff(int i, double x) {
        double t = i + x;
    }

    public void doFoo(Integer x, String s, double d, boolean b) {
        if (x != null) {
            double dd = x + d;
        }
    }

    public int simple(int x) {
        if (x < 3) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "SimpleExample";
    }
}
