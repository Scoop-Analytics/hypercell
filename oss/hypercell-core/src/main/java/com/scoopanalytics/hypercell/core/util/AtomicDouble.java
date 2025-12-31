package com.scoopanalytics.hypercell.core.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleUnaryOperator;

/**
 * A thread-safe double value that supports atomic operations.
 * This is a lightweight replacement for Guava's AtomicDouble.
 */
public class AtomicDouble extends Number {
    private final AtomicLong bits;

    public AtomicDouble() {
        this(0.0);
    }

    public AtomicDouble(double initialValue) {
        bits = new AtomicLong(Double.doubleToLongBits(initialValue));
    }

    public double get() {
        return Double.longBitsToDouble(bits.get());
    }

    public void set(double newValue) {
        bits.set(Double.doubleToLongBits(newValue));
    }

    public double addAndGet(double delta) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = currentVal + delta;
            long next = Double.doubleToLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return nextVal;
            }
        }
    }

    public double getAndAdd(double delta) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = currentVal + delta;
            long next = Double.doubleToLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return currentVal;
            }
        }
    }

    public double updateAndGet(DoubleUnaryOperator updateFunction) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = updateFunction.applyAsDouble(currentVal);
            long next = Double.doubleToLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return nextVal;
            }
        }
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return get();
    }
}
