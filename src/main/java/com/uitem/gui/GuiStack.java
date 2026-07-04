package com.uitem.gui;

import java.util.Stack;

/**
 * Navigation stack for GUI.
 * Allows the back button to return to the previous GUI instead of closing entirely.
 */
public class GuiStack {

    private static final Stack<Runnable> history = new Stack<>();

    /**
     * Push the current GUI onto the stack before opening a new GUI.
     */
    public static void push(Runnable parentOpener) {
        history.push(parentOpener);
    }

    /**
     * Go back to the previous GUI.
     * Returns true if there is a GUI to go back to.
     */
    public static void pop() {
        if (!history.isEmpty()) {
            history.pop().run();
        }
    }

    /**
     * Clear the entire history (when the player exits completely)
     */
    public static void clear() {
        history.clear();
    }

    /**
     * Check whether the stack is currently empty
     */
    public static boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Get the stack size
     */
    public static int size() {
        return history.size();
    }
}
