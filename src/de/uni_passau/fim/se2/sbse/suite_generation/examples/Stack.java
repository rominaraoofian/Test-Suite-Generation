package de.uni_passau.fim.se2.sbse.suite_generation.examples;

public class Stack {
    private Integer[] elements;
    private int top;
    private int capacity;
    public boolean allowResize;

    // Creates a new stack with the given capacity.
    public Stack(int capacity) {
        if (capacity < 1) {
            capacity = 5;
        }
        this.capacity = capacity;
        elements = new Integer[capacity];
        top = 0;
    }

    // Factory method.
    public static Stack factory() {
        return new Stack(5);
    }

    // Pushes the given element onto the stack.
    public void push(Integer x) {
        if (x == null) {
            return;
        }
        elements[top++] = x;
        if (top == capacity)
            resize();
    }

    // Doubles the length of the internal backing array.
    private void resize() {
        capacity = capacity * 2;
        Integer[] elements = new Integer[capacity];
        System.arraycopy(this.elements, 0, elements, 0, this.elements.length);
        this.elements = elements;
    }

    // Retrieves the top element without removing it from the stack.
    public Integer top() {
        if (isEmpty()) return null;
        else return elements[top - 1];
    }

    // Returns and removes the top element.
    public Integer pop() {
        if (isEmpty()) return null;
        else return elements[--top];
    }

    // Tells whether the stack is empty.
    public boolean isEmpty() {
        return top < 1;
    }

    public int size() {
        return top;
    }


    public boolean stackEquals(Stack o) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.size() != this.size()) return false;
        for (int i = 0; i < size(); i++) {
            if (!o.elements[i].equals(this.elements[i])) {
                return false;
            }
        }
        return true;
    }

}
