package de.uni_passau.fim.se2.sbse.suite_generation.algorithms;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.ChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.FitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranch;
import de.uni_passau.fim.se2.sbse.suite_generation.selection.Selection;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.StoppingCondition;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Pair;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;

import java.util.*;
import java.util.Random;

public class MyMOSA<C extends Chromosome<C>> implements GeneticAlgorithm<C> {

    private final ChromosomeGenerator<C> chromosomeGenerator;
    private final List<FitnessFunction<C>> fitnessFunctions;
    private final StoppingCondition stoppingCondition;
    private final Selection<C> selection;
    private final Map<Integer, C> archive = new HashMap<>();
    private final List<IBranch> branches;
    private final int populationSize;
    private final Random random = Randomness.random(); // also can pass it by constructor

    public MyMOSA(
            ChromosomeGenerator<C> generator,
            List<FitnessFunction<C>> fitnessFunctions,
            StoppingCondition stoppingCondition,
            Selection<C> selection,
            List<IBranch> branches,
            int populationSize) {
        this.chromosomeGenerator = generator;
        this.fitnessFunctions = fitnessFunctions;
        this.stoppingCondition = stoppingCondition;
        this.selection = selection;
        this.branches = branches;
        this.populationSize = populationSize;
    }

    @Override
    public List<C> findSolution() {
        notifySearchStarted();
        List<C> population = getInitialPopulation();
        while (searchCanContinue()) {
            List<C> offspring = generateOffspring(population);
            List<C> union = new ArrayList<>(population);
            union.addAll(offspring);
            updateArchive(union);
            population = preferenceSorting(union);
        }

        return new ArrayList<>(new HashSet<>(archive.values()));
    }

    private List<C> getInitialPopulation() {
        List<C> initialPopulation = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            initialPopulation.add(chromosomeGenerator.get());
        }
        updateArchive(initialPopulation);
        return initialPopulation;
    }

    private List<C> generateOffspring(List<C> population) {
        List<C> offspringPopulation = new ArrayList<>();
        while (offspringPopulation.size() < populationSize) {
            C parent1 = selection.apply(population);
            C parent2 = selection.apply(population);
            C offspring1;
            C offspring2;
            if (Randomness.random().nextDouble() < 0.75) {
                Pair<C> pair = parent1.crossover(parent2);
                offspring1 = pair.getFst();
                offspring2 = pair.getSnd();
            } else {
                offspring1 = parent1.copy();
                offspring2 = parent2.copy();
            }
            offspring1 = offspring1.mutate();
            offspring2 = offspring2.mutate();
            offspringPopulation.add(offspring1);
            if (offspringPopulation.size() < populationSize) {
                offspringPopulation.add(offspring2);
            }
        }
        return offspringPopulation;
    }

    private void updateArchive(List<C> population) {
        for (C chromosome : population) {
            if (!searchCanContinue()) {
                break;
            }
            chromosome.call();
            notifyFitnessEvaluation();

            for (int i = 0; i < fitnessFunctions.size(); i++) {
                int branchId = branches.get(i).getId();
                double score = fitnessFunctions.get(i).applyAsDouble(chromosome);
                C archived = archive.get(branchId);

                if (archived == null || score < fitnessFunctions.get(i).applyAsDouble(archived)) {
                    archive.put(branchId, (C) chromosome.copy());
                } else if (score == 0.0 && chromosome.size() < archived.size()) {
                    archive.put(branchId, (C) chromosome.copy());
                }
            }
        }
    }

    private List<C> preferenceSorting(List<C> union) {
        List<FitnessFunction<C>> uncovered = new ArrayList<>();
        for (int i = 0; i < fitnessFunctions.size(); i++) {
            int branchId = branches.get(i).getId();
            if (!archive.containsKey(branchId) || fitnessFunctions.get(i).applyAsDouble(archive.get(branchId)) > 0.0) {
                uncovered.add(fitnessFunctions.get(i));
            }
        }
        Set<C> f0 = new LinkedHashSet<>();
        for (FitnessFunction<C> goal : uncovered) {
            C best = union.get(0);
            double minFit = Double.MAX_VALUE;
            for (C chromosome : union) {
                double fit = goal.applyAsDouble(chromosome);
                if (fit < minFit) {
                    minFit = fit;
                    best = chromosome;
                }
            }
            f0.add(best);
        }

        List<C> nextPopulation = new ArrayList<>(f0);
        if (nextPopulation.size() >= populationSize) {
            return new ArrayList<>(nextPopulation).subList(0, populationSize);
        }

        List<C> remaining = new ArrayList<>(union);
        remaining.removeAll(f0);

        if (!remaining.isEmpty()) {
            List<List<C>> fronts = fastNonDominatedSort(remaining, uncovered);
            for (List<C> front : fronts) {
                if (nextPopulation.size() + front.size() <= populationSize) {
                    nextPopulation.addAll(front);
                } else {
                    calculateSubvectorDominanceAndSort(front, remaining, uncovered);
                    for (C c : front) {
                        if (nextPopulation.size() < populationSize) nextPopulation.add(c);
                    }
                }
                if (nextPopulation.size() >= populationSize){
                    break;
                }
            }
            while (nextPopulation.size() < populationSize) {
                nextPopulation.add(chromosomeGenerator.get());
            }
        }
        return nextPopulation;
    }


    private List<List<C>> fastNonDominatedSort(List<C> population, List<FitnessFunction<C>> objectives) {
        Map<C, List<C>> dominatedBy = new HashMap<>();
        Map<C, Integer> dominationCount = new HashMap<>();
        List<List<C>> fronts = new ArrayList<>();
        fronts.add(new ArrayList<>());

        for (C p : population) {
            dominatedBy.put(p, new ArrayList<>());
            dominationCount.put(p, 0);

            for (C q : population) {
                if (dominates(p, q, objectives)) {
                    dominatedBy.get(p).add(q);
                } else if (dominates(q, p, objectives)) {
                    dominationCount.put(p, dominationCount.get(p) + 1);
                }
            }

            if (dominationCount.get(p) == 0) {
                fronts.get(0).add(p);
            }
        }

        int i = 0;
        while (i < fronts.size() && !fronts.get(i).isEmpty()) {
            List<C> nextFront = new ArrayList<>();
            for (C p : fronts.get(i)) {
                for (C q : dominatedBy.get(p)) {
                    dominationCount.put(q, dominationCount.get(q) - 1);
                    if (dominationCount.get(q) == 0) {
                        nextFront.add(q);
                    }
                }
            }
            i++;
            if (!nextFront.isEmpty()) fronts.add(nextFront);
        }
        return fronts;
    }

    private boolean dominates(C p, C q, List<FitnessFunction<C>> objectives) {
        boolean better = false;
        for (FitnessFunction<C> fitnessFunction : objectives) {
            double fitnessP = fitnessFunction.applyAsDouble(p);
            double fitnessQ = fitnessFunction.applyAsDouble(q);
            if (fitnessP > fitnessQ) {
                return false;
            }
            if (fitnessP < fitnessQ){
                better = true;
            }
        }
        return better;
    }

    private void calculateSubvectorDominanceAndSort(List<C> front, List<C> population, List<FitnessFunction<C>> objectives) {
        Map<C, Integer> density = new HashMap<>();
        for (C p : front) {
            int strength = 0;
            for (C q : population) {
                if (dominates(q, p, objectives)) {
                    strength++;
                }
            }
            density.put(p, strength);
        }
        front.sort(Comparator.comparingInt(density::get));
    }

    @Override
    public StoppingCondition getStoppingCondition() {
        return stoppingCondition;
    }
}