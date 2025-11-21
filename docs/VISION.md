# Vision & Philosophy

## The Problem: The "Excel Gap"

In almost every enterprise, there is a massive gap between **Business Reality** and **Engineering Reality**.

*   **The Business** runs on Excel. Analysts, actuaries, and finance teams model the world in spreadsheets because it is the most flexible programming environment ever created. They iterate fast, handle complex logic, and understand the domain perfectly.
*   **The Engineering** runs on Java/Python/Go. To deploy a business model to production (e.g., a pricing API), developers typically take the Excel file and "translate" it into code.

### The Consequences
1.  **Translation Errors**: The developer misinterprets `IF(AND(A1>0, B1<5), ...)` and introduces a bug.
2.  **Latency**: Every time the analyst updates the model, the developer has to rewrite the code. This cycle takes weeks.
3.  **Black Boxes**: The business team can no longer verify the logic because they can't read Java.

## The HyperCell Solution: "Excel as Code"

HyperCell believes you shouldn't rewrite the model. You should **run the model**.

HyperCell provides the infrastructure to treat an Excel workbook exactly like a compiled binary. It loads the logic, understands the dependencies, and executes it with the speed and reliability of native Java code.

### Why not just use Apache POI?
Apache POI is incredible, but it is primarily an **I/O library**. It is designed to read and write files. While it has a formula evaluator, it is generally:
*   **Slow**: Designed for cell-by-cell evaluation, not massive throughput.
*   **Heavy**: Carries the full weight of the XML file structure.
*   **Not Serializable**: You can't easily "freeze" a POI workbook and ship it to a Redis cache or a Spark worker.

### Why HyperCell is Different
HyperCell is a **Calculation Engine first**. 
1.  **Graph-Based**: We parse the formulas into a Directed Acyclic Graph (DAG). When you change input `A1`, we only recalculate the cells that depend on `A1`.
2.  **Stateless/Stateful Separation**: We separate the *Model Definition* (the graph) from the *Data* (the values). This allows you to keep one "Compiled Graph" in memory and run thousands of concurrent calculations against it.
3.  **Headless**: There is no UI. There are no fonts, colors, or merged cells in our graph. Only logic.

## Design Principles

1.  **Correctness First**: `SUM(A1:A10)` must yield the exact same result as Excel.
2.  **Zero I/O during Calculation**: Once the model is hydrated, no disk access occurs. Everything is RAM.
3.  **Dependency Injection**: The engine should not know about databases or APIs. It should ask a `FunctionRegistry` for help. This keeps the core pure and portable.
