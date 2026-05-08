package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Decision;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.objectweb.asm.Opcodes.*;

class BranchTracerTest {

    private BranchTracer tracer;
    private final int trueID = 1;
    private final int falseID = 2;

    @BeforeEach
    void setUp() {
        tracer = BranchTracer.getInstance();
        tracer.clear();
    }

    @Test
    void getInstanceShouldReturnSameInstance() {
        BranchTracer instance2 = BranchTracer.getInstance();
        assertSame(tracer, instance2);
    }

    @Test
    void passedBranchUnaryIntShouldComputeCorrectDistances() {
        tracer.passedBranch(5, IFEQ, trueID, falseID);
        assertEquals(5.0, tracer.getDistances().get(trueID));
        assertEquals(0.0, tracer.getDistances().get(falseID));
        tracer.clear();
        tracer.passedBranch(2, IFLT, trueID, falseID);
        assertEquals(3.0, tracer.getDistances().get(trueID));
        assertEquals(0.0, tracer.getDistances().get(falseID));
    }

    @Test
    void passedBranchBinaryIntShouldComputeCorrectDistances() {
        tracer.passedBranch(15, 10, IF_ICMPLT, trueID, falseID);
        assertEquals(6.0, tracer.getDistances().get(trueID));
        assertEquals(0.0, tracer.getDistances().get(falseID));
    }

    @Test
    void passedBranchUnaryObjectShouldComputeCorrectDistances() {
        tracer.passedBranch(new Object(), IFNULL, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        assertEquals(0.0, tracer.getDistances().get(falseID));
    }

    @Test
    void testBinaryIntOpcodes() {
        tracer.passedBranch(10, 20, IF_ICMPEQ, trueID, falseID);
        assertEquals(10.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(10, 10, IF_ICMPNE, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(5, 10, IF_ICMPGE, trueID, falseID);
        assertEquals(5.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(5, 5, IF_ICMPGT, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(10, 5, IF_ICMPLE, trueID, falseID);
        assertEquals(5.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(5, 10, IF_ICMPGT, trueID, falseID);
        assertEquals(6.0, tracer.getDistances().get(trueID));
    }


    @Test
    void testUnaryIntOpcodes() {
        tracer.passedBranch(0, IFNE, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(5, IFLE, trueID, falseID);
        assertEquals(5.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(-2, IFGT, trueID, falseID);
        assertEquals(3.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(-1, IFGE, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(0, IFGE, trueID, falseID);
        assertEquals(0.0, tracer.getDistances().get(trueID));
    }

    @Test
    void passedBranchRootShouldSetDistanceToZero() {
        tracer.passedBranch(100);
        assertEquals(0.0, tracer.getDistances().get(100));
    }

    @Test
    void traceBranchDistanceShouldOnlyUpdateIfImproved() {
        tracer.passedBranch(10, IFEQ, trueID, falseID);
        assertEquals(10.0, tracer.getDistances().get(trueID));
        tracer.passedBranch(20, IFEQ, trueID, falseID);
        assertEquals(10.0, tracer.getDistances().get(trueID));
        tracer.passedBranch(5, IFEQ, trueID, falseID);
        assertEquals(5.0, tracer.getDistances().get(trueID));
    }

    @Test
    void testObjectComparisons() {
        Object o1 = new Object();
        Object o2 = new Object();
        tracer.passedBranch(null, IFNONNULL, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(o1, o2, IF_ACMPEQ, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
        tracer.clear();
        tracer.passedBranch(o1, o1, IF_ACMPNE, trueID, falseID);
        assertEquals(1.0, tracer.getDistances().get(trueID));
    }


    @Test
    void passedBranchShouldThrowOnUnknownOpcode() {
        assertThrows(IllegalArgumentException.class, () -> tracer.passedBranch(0, -1, trueID, falseID));
    }

    @Test
    void testInvalidOpcodesShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> tracer.passedBranch(0, -1, trueID, falseID));
    }
}