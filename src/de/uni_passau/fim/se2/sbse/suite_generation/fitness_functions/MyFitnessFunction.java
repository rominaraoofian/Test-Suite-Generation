package de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.BranchTracer;

import java.util.Map;

public class MyFitnessFunction implements FitnessFunction<MyChromosome>{

    private final int branchId;

    public MyFitnessFunction(int targetBranchId) {
        this.branchId = targetBranchId;
    }

    @Override
    public double applyAsDouble(MyChromosome chromosome) {
        Map<Integer, Double> distances = chromosome.call();
        if (distances.containsKey(branchId)) {
            double distance = distances.get(branchId);
            return distance / (distance + 1.0);
        }
        return 2.0;
    }

    @Override
    public boolean isMinimizing(){
        return true;
    }
}
