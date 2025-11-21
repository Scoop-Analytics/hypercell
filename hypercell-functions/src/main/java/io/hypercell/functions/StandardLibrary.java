package io.hypercell.functions;

import io.hypercell.api.FunctionRegistry;
import io.hypercell.functions.math.SumFunction;

public class StandardLibrary {
    public static void register(FunctionRegistry registry) {
        registry.register("SUM", new SumFunction());
    }
}
