package de.uni_passau.fim.se2.sbse.suite_generation.mutation;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;



public class MyMutation  implements Mutation<MyChromosome> {
    private final Random random = Randomness.random();
    private  MyChromosomeGenerator generator;

    public MyMutation (MyChromosomeGenerator generator) {
        this.generator = generator;
    }
    @Override
    public MyChromosome apply(MyChromosome parent) {
        List<Statement> parentStatements = parent.getStatements();
        double pMutate = 1.0 / Math.max(1, parentStatements.size());
        List <Statement> mutatedStatements = new ArrayList<>();
        mutatedStatements.add(parentStatements.getFirst());
        for (int i = 1; i < parentStatements.size(); i++) {
            if (random.nextDouble() >= pMutate) {
                mutatedStatements.add(parentStatements.get(i));
            }
        }
        if (mutatedStatements.size() <= 1) {
            mutatedStatements.clear();
            mutatedStatements.addAll(parentStatements);
        }
        Object target = ((MyConstructorStatement) mutatedStatements.getFirst()).getResult();
        for (int i = 1; i < mutatedStatements.size(); i++) {
            if (random.nextDouble() < pMutate) {
                Statement newStatement = generateRandomMethodOrField(target);
                if (newStatement != null)
                    mutatedStatements.set(i, newStatement);
            }
        }
        double alpha = 1.0 / 3.0;
        int count = 1;
        while (random.nextDouble() < Math.pow(alpha, count) && mutatedStatements.size() < 50) {
            Statement newStatement = generateRandomMethodOrField(target);
            if (newStatement != null) {
                mutatedStatements.add(newStatement);
            }
            count++;
        }
        return new MyChromosome(mutatedStatements, parent.getMutation(), parent.getCrossover());
    }

    private Statement generateRandomMethodOrField(Object target){
        if (random.nextBoolean()) {
            return generator.generateRandomMethodCall(target);
        } else {
            return generator.generateRandomFieldAssignment(target);
        }
    }
    public void setGenerator(MyChromosomeGenerator chromosomeGenerator) {
        generator = chromosomeGenerator;
    }
}


