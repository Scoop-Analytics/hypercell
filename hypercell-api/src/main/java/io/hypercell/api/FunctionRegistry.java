package io.hypercell.api;

public interface FunctionRegistry {
    Function getFunction(String name);
    void register(String name, Function function);
}
