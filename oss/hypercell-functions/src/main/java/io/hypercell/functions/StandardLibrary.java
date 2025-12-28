package io.hypercell.functions;

import io.hypercell.api.FunctionRegistry;
import io.hypercell.api.Function;
import io.hypercell.functions.math.SumFunction;
import java.util.Map;
import java.util.HashMap;

public class StandardLibrary implements FunctionRegistry {
    private Map<String, Function> functions = new HashMap<>();

    public StandardLibrary() {
        register("SUM", new SumFunction());
    }

    public static void register(FunctionRegistry registry) {
        registry.register("SUM", new SumFunction());
    }

    @Override
    public void register(String name, Function function) {
        functions.put(name, function);
    }

    @Override
    public Function getFunction(String name) {
        return functions.get(name);
    }
}
