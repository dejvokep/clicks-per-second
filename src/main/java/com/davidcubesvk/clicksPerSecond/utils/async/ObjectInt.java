package com.davidcubesvk.clicksPerSecond.utils.async;

/**
 * Stores an integer value (like {@link Integer} or <code>int</code>, except this class is initialized only once, so even if declared as final, you are still able to change the stored value).
 */
public class ObjectInt {

    //Current value
    private int value;

    /**
     * Sets a start value for this instance.
     *
     * @param initValue the start value
     */
    public ObjectInt(int initValue) {
        this.value = initValue;
    }

    /**
     * Changes the stored value by the specified value in operation <code>stored = stored + x</code>.
     *
     * @param x the value to apply to the currently stored value
     */
    public void change(int x) {
        this.value += x;
    }

    /**
     * Returns the stored value.
     *
     * @return the stored value
     */
    public int get() {
        return value;
    }
}
