# WALA - an interprocedural dataflow framework for Java/Javascript

By: Garvita Allabadi (garvita4)

## Introduction

The T. J. Watson Libraries for Analysis (WALA) provide static analysis capabilities for Java bytecode and related languages and for JavaScript. It was initially developed at IBM T.J. Watson Research Center and was made open source in 2006 under Eclipse Public License.

## Core WALA Features

WALA features include:

1. Java type system and class hierarchy analysis
2. Source language framework supporting Java and JavaScript
3. Interprocedural dataflow analysis (RHS solver)
4. Context-sensitive tabulation-based slicer
5. Pointer analysis and call graph construction
6. SSA-based register-transfer language IR
7. General framework for iterative dataflow
8. General analysis utilities and data structures
9. A bytecode instrumentation library (Shrike)

## Data Structes and Algorithms in WALA

WALA supports different kinds of data structures and algorithms such as:
1. BitSets datastructures such as BitVector, OffSetBitVector, MutableSparseIntSet. 
2. Graph Representations and algorithms such as DominanceFrontiers, Dominators, CFG, SSACFG, ExplodedControlFlowGraph (where each basic block corresponds to exactly one instruction). 
3. Algorithms for data flow analysis such as FixPoint Data Solvers and Pointer analysis   

WALA IR encodes the instructions and control flow of a particular method. The IR is an immutable control-flow graph of instructions in SSA form. It also provides different IR utilities such as getting DefUse, type inference etc. 
IR Example: SSAGetInstruction in WALA is analogous to the load instruction in LLVM and SSAPutInstruction in WALA is analogous to the store instruction in LLVM.

## Writing a new Analysis in WALA

The general sequence of steps to be followed while writing a new analysis in WALA is as follows:

1. Build a ClassHierarchy; that is, read the program source (e.g. bytecode) into memory, and parse some basic information regarding the types represented. A ClassHierarchy object represents a universe of code to analyze.
2. Build a CallGraph; that is, perform pointer analysis with on-the-fly call graph construction to resolve the targets of dynamic dispatch calls, and build a graph of CGNode objects representing the possible program calling structure.
3. Perfom some analysis over the resulting call graph using WALA IR. 


## Using WALA to write the Reaching Definitions Analysis

We will use the following example:

**Jave Code:**
```java

public class Dataflow {
    
    static int f;
    static int g;
    
    public static void test()
    {
        f = 4;
        g = 3;
        if (f == 5){
            g = 2;
        }
        else{
            g = 7;
        }
    }
}
```


**WALA generates the following CFG for the above code:**
```
CFG:
BB0[-1..-2]
    -> BB1
BB1[0..6]
    -> BB3
    -> BB2
BB2[7..9]
    -> BB4
BB3[10..11]
    -> BB4
BB4[12..12]
    -> BB5
BB5[-1..-2]
```

**WALA generates the following IR for the above code:**

```
BB0
BB1
1   putstatic < Application, Ldataflow/StaticDataflow, f, <Primordial,I> > = v2:#4(line 26)
3   putstatic < Application, Ldataflow/StaticDataflow, g, <Primordial,I> > = v3:#3(line 27)
4   v4 = getstatic < Application, Ldataflow/StaticDataflow, f, <Primordial,I> >(line 28)
6   conditional branch(ne, to iindex=10) v4,v5:#5(line 28)
BB2
8   putstatic < Application, Ldataflow/StaticDataflow, g, <Primordial,I> > = v7:#2(line 29)
9   goto (from iindex= 9 to iindex = 12)     (line 29)
BB3
11   putstatic < Application, Ldataflow/StaticDataflow, g, <Primordial,I> > = v6:#7(line 31)
BB4
12   return                                  (line 33)
BB5
```

## Implementation

We define a BitVectorFramework, BitVectorSolver for the Reaching Definition Analysis. For this we need to define our kill and gen set computations, the transfer funtion and the meet operator for the analysis. 

The complete implementation with a unit test is located here: [https://github.com/allabadigarvita/wala](https://github.com/allabadigarvita/wala)


## Results 

For the above example, the In sets and Out sets are computed as follows: 

```
BB: 0
In[Empty]
Out[Empty]
BB: 1
In[Empty]
Out{ 0 }
BB: 2
In{ 0 }
Out{ 0 2 }
BB: 3
In{ 0 }
Out{ 0 3 }
BB: 4
In{ 0 2 3 }
Out{ 0 2 3 }
BB: 5
In{ 0 2 3 }
Out{ 0 2 3 }
```
