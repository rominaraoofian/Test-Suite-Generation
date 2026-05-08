package de.uni_passau.fim.se2.sbse.suite_generation.algorithms;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.ChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.FitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranch;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.StoppingCondition;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;

import java.util.*;

public class MyRandomSearch<C extends Chromosome<C>> implements GeneticAlgorithm<C> {

    private final ChromosomeGenerator<C> chromosomeGenerator;
    private final List<FitnessFunction<C>> fitnessFunctions;
    private final StoppingCondition stoppingCondition;
    private final Map<Integer, C> archive = new HashMap<>();
    private final List<IBranch> branches;

    public MyRandomSearch(
            ChromosomeGenerator<C> generator,
            List<FitnessFunction<C>> fitnessFunctions,
            StoppingCondition stoppingCondition,
            List<IBranch> branches) {
        this.chromosomeGenerator = generator;
        this.fitnessFunctions = fitnessFunctions;
        this.stoppingCondition = stoppingCondition;
        this.branches = branches;
    }

    @Override
    public List<C> findSolution() {
        notifySearchStarted();
        while (searchCanContinue()) {
            C candidate = chromosomeGenerator.get();
            for (int i = 0; i < fitnessFunctions.size(); i++) {
                FitnessFunction<C> fitnessFunction = fitnessFunctions.get(i);
                double distance = fitnessFunction.applyAsDouble(candidate);
                int branchId = branches.get(i).getId();
                if (distance == 0.0) {
                    if (!archive.containsKey(branchId)) {
                        archive.put(branchId, candidate.copy());
                    }
                    else{
                        if (candidate.getStatements().size() < archive.get(branchId).getStatements().size()) {
                            archive.put(branchId, candidate.copy());
                        }
                    }
                }
            }

            notifyFitnessEvaluation();
        }
//        List<C> archiveList = new ArrayList<>(archive.values());
//        return archiveList;
        return new ArrayList<>(new HashSet<>(archive.values()));
    }

    @Override
    public StoppingCondition getStoppingCondition() {
        return stoppingCondition;
    }

}
