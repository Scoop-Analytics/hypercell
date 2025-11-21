# HyperCell Architecture

HyperCell is designed as a multi-stage pipeline: **Hydration -> Compilation -> Execution**.

## 1. The Grid Model (`hypercell-core`)

The foundation is a set of simple, optimized data structures that represent the spreadsheet state.

*   **`MemWorkbook`**: The root container. Holds multiple sheets and shared state (fonts, styles).
*   **`MemSheet`**: A sparse grid of cells. Optimized for random access.
*   **`MemCell`**: The atomic unit. Can hold:
    *   **Value**: String, Number, Boolean.
    *   **Formula**: The raw string (e.g., `=SUM(A1:A10)`).
    *   **Compiled Expression**: The executable tree derived from the formula.

## 2. The Formula Engine (`hypercell-formula`)

We use **ANTLR4** to parse Excel formulas.

*   **Grammar (`HyperCellExpression.g4`)**: A complete definition of the Excel formula syntax, including complex ranges (`A1:B5`), intersections, unions, and function calls.
*   **Parser**: Converts a string formula into an Abstract Syntax Tree (AST).

## 3. The Compilation Phase (`Compile.java`)

When a workbook is loaded (Hydrated), HyperCell iterates through every formula cell:
1.  **Parse**: Uses ANTLR to generate the AST.
2.  **Build Dependency Graph**: As it parses `=A1+B1`, it registers the current cell as a "dependent" of `A1` and `B1`.
3.  **Compile to Expression**: The AST is converted into a tree of `Expression` objects (e.g., `SumFunction`, `CellReference`, `NumberConstant`).

## 4. The Execution Phase

Calculation is triggered when a cell value changes.

1.  **Dirty Marking**: When input `A1` changes, we traverse the dependency graph to mark all downstream cells (`B1`, `C1`) as "Dirty".
2.  **Evaluation**: We re-evaluate dirty cells in topological order.
3.  **Lazy vs. Eager**: HyperCell supports both modes.
    *   **Eager**: Calculate everything immediately (good for APIs).
    *   **Lazy**: Calculate only when requested (good for huge models).

## 5. The Plugin System (`hypercell-api`)

HyperCell is "hermetic"—it knows nothing about the outside world. It talks to the world via interfaces:

*   **`EvaluationContext`**: "I need the value of range 'GlobalVariables!A1:A10'. Can you find it?"
*   **`FunctionRegistry`**: "The user called `=MY_DB_LOOKUP(123)`. Do you have a handler for that?"

This allows you to inject:
*   **Database Lookups**: A custom function that queries SQL.
*   **API Calls**: A function that hits a REST endpoint.
*   **Machine Learning**: A function that runs an inference model.
