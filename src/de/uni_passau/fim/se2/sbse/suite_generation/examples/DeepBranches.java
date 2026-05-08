package de.uni_passau.fim.se2.sbse.suite_generation.examples;

public class DeepBranches {

    public void deepIn(int a, int b, int c, int d, int e) {
        boolean done = false;
        if (a < 20) {
            if (b > 50) {
                if (c < a) {
                    if (d < a && d > e) {
                        if (e == 0) {
                            done = true;
                        }
                    }
                }
            }
        }
    }

    public void hard(int a, int b, int c, int d) {
        boolean done = false;
        if (a == 1) {
            if (b == 2) {
                if (c == 4) {
                    if (d == 5) {
                        done = true;
                    }
                }
            }
        }
    }
}
