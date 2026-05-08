package de.uni_passau.fim.se2.sbse.suite_generation.crossover;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Pair;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MyCrossover<C extends Chromosome<C>> implements Crossover<MyChromosome> {

    private final Random random = Randomness.random();

    @Override
    public Pair<MyChromosome> apply(MyChromosome parent1, MyChromosome parent2) {
        List<Statement> parent1Statements = parent1.getStatements();
        List<Statement> parent2Statements = parent2.getStatements();
        List<Statement> offspring1Statements;
        List<Statement> offspring2Statements;
        int point = random.nextInt(Math.min(parent1Statements.size(), parent2Statements.size()));
        if (point <= 1){
            return Pair.of(parent1.copy(), parent2.copy());
        }
        int pivot = random.nextInt(point - 1) + 1;
        offspring1Statements = new ArrayList<>(parent1Statements.subList(0, pivot));
        offspring1Statements.addAll(parent2Statements.subList(pivot, parent2Statements.size()));
        offspring2Statements = new ArrayList<>(parent2Statements.subList(0, pivot));
        offspring2Statements.addAll(parent1Statements.subList(pivot, parent1Statements.size()));

        while (offspring1Statements.size() > 50) {
            offspring1Statements.removeLast();
        }
        while (offspring2Statements.size() > 50) {
            offspring2Statements.removeLast();
        }

        MyChromosome offspring1Chromosome = new MyChromosome(offspring1Statements, parent1.getMutation(), parent1.getCrossover());
        MyChromosome offspring2Chromosome = new MyChromosome(offspring2Statements, parent2.getMutation(), parent2.getCrossover());
        return Pair.of(offspring1Chromosome, offspring2Chromosome);
    }


}