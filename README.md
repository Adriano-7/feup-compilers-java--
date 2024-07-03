# Java-- Compiler

This project implements a compiler for Java--, a subset of Java with some additional features. The compiler performs lexical and syntactic analysis, semantic analysis, generates OLLIR (Object-Oriented Low-Level Intermediate Representation) code, and finally produces Jasmin assembly code that can be converted to Java bytecode.

## Project Structure

The compiler is developed in three main checkpoints:

1. Lexical and Syntactic Analysis, Symbol Table Generation
2. Semantic Analysis, Initial OLLIR and Jasmin Generation
3. Complete OLLIR and Jasmin Generation, Optimizations

## Features

- Lexical and syntactic analysis using ANTLR
- Symbol table generation
- Semantic analysis
- OLLIR code generation
- Jasmin code generation
- Support for basic Java-- constructs including:
  - Class declarations
  - Method declarations
  - Variable declarations
  - Arithmetic operations
  - Method invocations
  - Conditional statements (if-else)
  - Loops (while)
  - Arrays and varargs

## Contributors

- Adriano Machado – 202105352
- Alexandre Correia – 202007042
- Clarisse Carvalho – 202008444
