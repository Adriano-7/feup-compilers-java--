# Java-- Compiler (Jmm)

> **Project**
> <br />
> Course Unit: [Compilers](https://sigarra.up.pt/feup/pt/ucurr_geral.ficha_uc_view?pv_ocorrencia_id=520331) (Compiladores), 3rd year
> <br />
> Course: Informatics and Computing Engineering
> <br />
> Faculty: **FEUP** (Faculty of Engineering of the University of Porto)
> <br />
> Project evaluation: **17**/20

---

## Project Goals

The goal of this project was to develop a complete compiler for **Java--**, a subset of the Java language. The compiler transforms Java-- source code into Java Bytecode (via Jasmin assembly), passing through several intermediate representations and performing rigorous semantic validation.

**The different stages:**
- **Parser:** Lexical and Syntactic analysis using ANTLR4.
- **Semantic Analysis:** Symbol table generation and type checking.
- **OLLIR:** Generation of an Intermediate Representation (Object-Oriented Low-Level Intermediate Representation).
- **Backend:** Conversion of OLLIR into Jasmin assembly and final JVM bytecode.

## Technical Approach

### 1. Syntactic & Lexical Analysis
We used **ANTLR4** to define the grammar for Java--. The parser handles standard Java constructs such as class declarations, inheritance (`extends`), method overloading, and complex expressions.
- **Error Recovery:** The parser includes basic error recovery mechanisms to identify syntax errors (like missing semicolons) without crashing the entire compilation process.
- **AST Generation:** The resulting Concrete Syntax Tree (CST) is mapped to a cleaner Abstract Syntax Tree (AST) for easier processing in subsequent stages.

### 2. Semantic Analysis
This stage ensures the code follows the logic and type rules of Java--.
- **Symbol Table:** We implemented a multi-scope symbol table to track class fields, local variables, and method signatures (including parameters and return types).
- **Type Checking:** Validates that operations are performed on compatible types (e.g., preventing `int + boolean`) and that method calls match their declarations.
- **Advanced Checks:** 
    - Initialization of variables before use.
    - Verification of `this` usage (prevention in `static main`).
    - Array access validation (index must be `int`).
    - Varargs support as the last parameter of a method.

### 3. OLLIR Generation
To bridge the gap between a high-level AST and low-level Bytecode, we implemented a visitor that generates **OLLIR**.
- **Code Linearization:** Complex nested expressions (e.g., `a = b + c * d`) are broken down into temporary variables to simplify the instruction set.
- **Control Flow:** `if-else` and `while` loops are translated into conditional jumps and labels.
- **Method Invocations:** Distinguishes between `invokestatic`, `invokevirtual`, and `invokespecial` based on the caller's context.

### 4. Code Generation (Jasmin)
The final stage converts OLLIR into **Jasmin assembly**.
- **Stack Management:** We calculate the required `.limit stack` and `.limit locals` for each method.
- **Instruction Mapping:** OLLIR instructions are mapped to their corresponding JVM instructions (e.g., `iadd`, `aload`, `istore`).
- **Optimizations:** We implemented instruction selection (e.g., using `iconst_0` instead of `bipush 0`) and efficient register allocation.

## Project Features

- [x] Full Java-- Support (Classes, Inheritance, Arrays)
- [x] Semantic Analysis & Type Checking
- [x] OLLIR Intermediate Representation
- [x] Jasmin Code Generation
- [x] Varargs support
- [x] Detailed error reporting with line/column information

## Running the Compiler

**Compile the project:**
```bash
./gradlew build
```

**Run the compiler on a source file:**
```bash
./jmm input.txt
```

**Run tests:**
```bash
./gradlew test
```

## Tech Stack

Java, ANTLR4, OLLIR, Jasmin, JUnit 5, Gradle

## Team

- Adriano Machado - 202105352
- Alexandre Correia - 202007042
- Clarisse Carvalho - 202008444
