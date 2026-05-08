# Test Suite Generation using MOSA and Random Search

## Overview
This project focuses on automated **test case generation** for a Java Class Under Test (CUT) using search-based optimization techniques. The goal is to construct a test suite that achieves strong **branch coverage** without relying on manually written test cases.

Two algorithms are implemented and compared:

- **Random Search**
- **MOSA (Many-Objective Sorting Algorithm)**

---

## Problem Setting
Unlike test minimization or prioritization, this project starts with no predefined test suite. The system must **generate test cases from scratch** that effectively explore different execution paths of the program.

Each branch in the CUT is treated as a separate objective, making this a **many-objective optimization problem**.

---

## Solution Approaches

### Random Search
A baseline strategy that generates test cases randomly and retains those that achieve higher branch coverage. It provides a simple reference point for evaluating more advanced techniques.

### MOSA
MOSA is an extension of evolutionary multi-objective optimization designed for software testing problems with many objectives.

Key ideas include:
- Prioritizing uncovered branches
- Maintaining an archive of high-quality test cases
- Using a preference-based selection strategy to guide search
- Handling many objectives more effectively than standard dominance methods

---

## Test Case Representation
Each candidate solution represents a test case composed of Java statements:

- Begins with a constructor call of the CUT
- Includes method calls or field assignments
- Uses primitive types, Strings, or null references
- Limited to a maximum length of 50 statements

This structure ensures syntactically valid and executable test cases.

---

## Fitness Evaluation
Test quality is evaluated using **branch distance**, which measures how close a test case is to satisfying a branch condition.

It considers predicates such as:
- `x < y`
- `x == y`
- `!(x ≤ y)`

Distances are normalized and aggregated to guide the search toward unexecuted branches.
