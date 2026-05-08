package de.uni_passau.fim.se2.sbse.suite_generation.examples;

import java.util.ArrayList;
import java.util.List;

/*
 * Features that will be tested during the grading process of the assignment:
 * - setting public fields
 * - invoking constructors
 * - invoking dynamic methods
 * - branch distance computation of if-else statements
 * - branch distance computation of for-loops and while-loops loops
 * - parameters: primitive types + wrapper types for primitives
 */
public class Feature {

    public String s = null;
    public Integer publicValue;
    private Integer value;
    private List<String> logger;

    public Feature(Integer value, String s) {
        this.logger = new ArrayList<>();
        this.value = value;
        this.s = s;
    }

    public Feature(Integer value) {
        logger = new ArrayList<>();
        this.value = value;
    }

    public Feature(int intValue, int value2) {
        this.value = intValue + value2;
    }

    public Feature(SimpleExample example) {
        this.value = 23;
    }

    public Feature() {
        logger = new ArrayList<>();
    }

    public String doStuff(String s, Integer i, Boolean b, Double d, Float f, Short r) {
        if (value != null && value > 0) {
            return "dings" + i + b + d + f + r;
        } else if (publicValue != null && publicValue < 65) {
            return "blub";
        } else {
            return null;
        }
    }

    public Integer foo(Integer counter) {
        Integer c = 0;
        while (counter != null && counter <= 4) {
            c++;
            counter++;
        }
        return c;
    }

    public void doNothing() {
        // this is empty!
    }

    public void caller(int i, float x, boolean b) {
        if (publicValue != null && i < 42 && i < publicValue) {
            call1(i);
        } else {
            call2(i);
        }

        if (x > 22 && b) {
            List<String> list = new ArrayList<>();
            for (int z = 0; z < x; z++) {
                list.add("n" + z);
            }
        }
    }

    private void call1(int x) {
        String a = "";
        for (int z = Math.abs(x); z < 60; z++) {
            a += "a";
        }
    }

    private void call2(double y) {
        List<Boolean> bools = new ArrayList<>();
        for (int j = (int) y / 2; j < 80; j++) {
            bools.add(y == j);
            if (bools.size() > 40) {
                break;
            }
        }
        if (s != null) {
            bools.add(false);
        }
    }

    public int simpleIf(int x) {
        if (x < 2) {
            return 0;
        }
        return 4;
    }

    public void seeWhatHappens(int x) {
        if (logger == null) {
            logger = new ArrayList<>();
        }

        if (x > -1022) {
            logger.add("Not quite there yet");
        }

        logger.add("You did it");
    }
}
