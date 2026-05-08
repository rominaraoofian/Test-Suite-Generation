package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyAssignmentStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyMethodStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.BranchTracer;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyChromosome extends Chromosome<MyChromosome> {
     private final List<Statement> statements;
    public MyChromosome(List<Statement> statements) {
        super();
        this.statements = statements;
    }
     public MyChromosome(List<Statement> statements, Mutation<MyChromosome> mutation, Crossover<MyChromosome> crossover){
         super(mutation, crossover);
         this.statements = statements;
     }

    @Override
    public MyChromosome self() {
        return this;
    }

     @Override
    public  List<Statement> getStatements(){
         return statements;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof MyChromosome other2))
            return false;
        return Objects.equals(this.getStatements(), other2.getStatements());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getStatements());
    }

    @Override
    public Map<Integer, Double> call() {
        BranchTracer branchTracer = BranchTracer.getInstance();
        branchTracer.clear();
        Object actualRuntimeInstance = null;

        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = statements.get(i);

            try {
                if (i == 0) {
                    stmt.run();
                    if (stmt instanceof MyConstructorStatement constr) {
                        actualRuntimeInstance = constr.getResult();
                    }
                } else {
                    // For all other statements, inject the actual instance first
                    if (stmt instanceof MyMethodStatement methodStmt) {
                        methodStmt.setTarget(actualRuntimeInstance);
                    } else if (stmt instanceof MyAssignmentStatement assignStmt) {
                        assignStmt.setTarget(actualRuntimeInstance);
                    }
                    stmt.run();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error during test execution: " + e.getMessage());
            }
        }

        return branchTracer.getDistances();
    }

    @Override
    public MyChromosome copy() {
        List<Statement> statementsCopy = new ArrayList<>(this.statements);
        return new MyChromosome(statementsCopy, getMutation(), getCrossover());
    }

}
