/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_C1_C1_GLOBALS_HPP
#define SHARE_C1_C1_GLOBALS_HPP

#include "c1/c1_globals_pd.hpp"
#include "runtime/globals_shared.hpp"
#include "utilities/macros.hpp"
//
// Declare all global flags used by the client compiler.
//
#define C1_FLAGS(develop,                                                   \
                 develop_pd,                                                \
                 product,                                                   \
                 product_pd,                                                \
                 notproduct,                                                \
                 range,                                                     \
                 constraint)                                                \
                                                                            \
  /* Printing */                                                            \
  notproduct(bool, PrintC1Statistics, false,                                \
          "Print Compiler1 statistics" )                                    \
                                                                            \
  notproduct(bool, PrintInitialBlockList, false,                            \
          "Print block list of BlockListBuilder")                           \
                                                                            \
  notproduct(bool, PrintCFG, false,                                         \
          "Print control flow graph after each change")                     \
                                                                            \
  notproduct(bool, PrintCFG0, false,                                        \
          "Print control flow graph after construction")                    \
                                                                            \
  notproduct(bool, PrintCFG1, false,                                        \
          "Print control flow graph after optimizations")                   \
                                                                            \
  notproduct(bool, PrintCFG2, false,                                        \
          "Print control flow graph before code generation")                \
                                                                            \
  notproduct(bool, PrintIRDuringConstruction, false,                        \
          "Print IR as it's being constructed (helpful for debugging frontend)")\
                                                                            \
  notproduct(bool, PrintPhiFunctions, false,                                \
          "Print phi functions when they are created and simplified")       \
                                                                            \
  notproduct(bool, PrintIR, false,                                          \
          "Print full intermediate representation after each change")       \
                                                                            \
  notproduct(bool, PrintIR0, false,                                         \
          "Print full intermediate representation after construction")      \
                                                                            \
  notproduct(bool, PrintIR1, false,                                         \
          "Print full intermediate representation after optimizations")     \
                                                                            \
  notproduct(bool, PrintIR2, false,                                         \
          "Print full intermediate representation before code generation")  \
                                                                            \
  notproduct(bool, PrintSimpleStubs, false,                                 \
          "Print SimpleStubs")                                              \
                                                                            \
  /* C1 optimizations */                                                    \
                                                                            \
  develop(bool, UseC1Optimizations, true,                                   \
          "Turn on C1 optimizations")                                       \
                                                                            \
  develop(bool, SelectivePhiFunctions, true,                                \
          "create phi functions at loop headers only when necessary")       \
                                                                            \
  develop(bool, OptimizeIfOps, true,                                        \
          "Optimize multiple IfOps")                                        \
                                                                            \
  develop(bool, DoCEE, true,                                                \
          "Do Conditional Expression Elimination to simplify CFG")          \
                                                                            \
  develop(bool, PrintCEE, false,                                            \
          "Print Conditional Expression Elimination")                       \
                                                                            \
  develop(bool, UseLocalValueNumbering, true,                               \
          "Use Local Value Numbering (embedded in GraphBuilder)")           \
                                                                            \
  develop(bool, UseGlobalValueNumbering, true,                              \
          "Use Global Value Numbering (separate phase)")                    \
                                                                            \
  product(bool, UseLoopInvariantCodeMotion, true,                           \
          "Simple loop invariant code motion for short loops during GVN")   \
                                                                            \
  develop(bool, TracePredicateFailedTraps, false,                           \
          "trace runtime traps caused by predicate failure")                \
                                                                            \
  develop(bool, StressLoopInvariantCodeMotion, false,                       \
          "stress loop invariant code motion")                              \
                                                                            \
  develop(bool, TraceRangeCheckElimination, false,                          \
          "Trace Range Check Elimination")                                  \
                                                                            \
  develop(bool, AssertRangeCheckElimination, false,                         \
          "Assert Range Check Elimination")                                 \
                                                                            \
  develop(bool, StressRangeCheckElimination, false,                         \
          "stress Range Check Elimination")                                 \
                                                                            \
  develop(bool, PrintValueNumbering, false,                                 \
          "Print Value Numbering")                                          \
                                                                            \
  product(intx, ValueMapInitialSize, 11,                                    \
          "Initial size of a value map")                                    \
          range(1, NOT_LP64(1*K) LP64_ONLY(32*K))                           \
                                                                            \
  product(intx, ValueMapMaxLoopSize, 8,                                     \
          "maximum size of a loop optimized by global value numbering")     \
          range(0, 128)                                                     \
                                                                            \
  develop(bool, EliminateBlocks, true,                                      \
          "Eliminate unneccessary basic blocks")                            \
                                                                            \
  develop(bool, PrintBlockElimination, false,                               \
          "Print basic block elimination")                                  \
                                                                            \
  develop(bool, EliminateNullChecks, true,                                  \
          "Eliminate unneccessary null checks")                             \
                                                                            \
  develop(bool, PrintNullCheckElimination, false,                           \
          "Print null check elimination")                                   \
                                                                            \
  develop(bool, EliminateFieldAccess, true,                                 \
          "Optimize field loads and stores")                                \
                                                                            \
  develop(bool, InlineMethodsWithExceptionHandlers, true,                   \
          "Inline methods containing exception handlers "                   \
          "(NOTE: does not work with current backend)")                     \
                                                                            \
  product(bool, InlineSynchronizedMethods, true,                            \
          "Inline synchronized methods")                                    \
                                                                            \
  develop(bool, CanonicalizeNodes, true,                                    \
          "Canonicalize graph nodes")                                       \
                                                                            \
  develop(bool, PrintCanonicalization, false,                               \
          "Print graph node canonicalization")                              \
                                                                            \
  develop(bool, UseTableRanges, true,                                       \
          "Faster versions of lookup table using ranges")                   \
                                                                            \
  product(intx, C1MaxInlineSize, 35,                                        \
          "The maximum bytecode size of a method to be inlined by C1")      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, C1MaxTrivialSize, 6,                                        \
          "The maximum bytecode size of a trivial method to be inlined by " \
          "C1")                                                             \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, C1MaxInlineLevel, 9,                                        \
          "The maximum number of nested calls that are inlined by C1")      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, C1MaxRecursiveInlineLevel, 1,                               \
          "maximum number of nested recursive calls that are inlined by C1")\
          range(0, max_jint)                                                \
                                                                            \
  product(intx, C1InlineStackLimit, 10,                                     \
          "inlining only allowed for methods which don't exceed this "      \
          "number of expression stack and local slots")                     \
          range(0, max_jint)                                                \
                                                                            \
  develop(intx, NestedInliningSizeRatio, 90,                                \
          "Percentage of prev. allowed inline size in recursive inlining")  \
          range(0, 100)                                                     \
                                                                            \
  notproduct(bool, PrintIRWithLIR, false,                                   \
          "Print IR instructions with generated LIR")                       \
                                                                            \
  notproduct(bool, PrintLIRWithAssembly, false,                             \
          "Show LIR instruction with generated assembly")                   \
                                                                            \
  develop(bool, CommentedAssembly, trueInDebug,                             \
          "Show extra info in PrintNMethods output")                        \
                                                                            \
  develop(bool, LIRTraceExecution, false,                                   \
          "add LIR code which logs the execution of blocks")                \
                                                                            \
  develop_pd(bool, CSEArrayLength,                                          \
          "Create separate nodes for length in array accesses")             \
                                                                            \
  develop_pd(bool, TwoOperandLIRForm,                                       \
          "true if LIR requires src1 and dst to match in binary LIR ops")   \
                                                                            \
  develop(intx, TraceLinearScanLevel, 0,                                    \
          "Debug levels for the linear scan allocator")                     \
          range(0, 4)                                                       \
                                                                            \
  develop(bool, StressLinearScan, false,                                    \
          "scramble block order used by LinearScan (stress test)")          \
                                                                            \
  product(bool, TimeLinearScan, false,                                      \
          "detailed timing of LinearScan phases")                           \
                                                                            \
  develop(bool, TimeEachLinearScan, false,                                  \
          "print detailed timing of each LinearScan run")                   \
                                                                            \
  develop(bool, CountLinearScan, false,                                     \
          "collect statistic counters during LinearScan")                   \
                                                                            \
  /* C1 variable */                                                         \
                                                                            \
  develop(bool, C1Breakpoint, false,                                        \
          "Sets a breakpoint at entry of each compiled method")             \
                                                                            \
  develop(bool, ImplicitDiv0Checks, true,                                   \
          "Use implicit division by zero checks")                           \
                                                                            \
  develop(bool, PinAllInstructions, false,                                  \
          "All instructions are pinned")                                    \
                                                                            \
  develop(bool, UseFastNewInstance, true,                                   \
          "Use fast inlined instance allocation")                           \
                                                                            \
  develop(bool, UseFastNewTypeArray, true,                                  \
          "Use fast inlined type array allocation")                         \
                                                                            \
  develop(bool, UseFastNewObjectArray, true,                                \
          "Use fast inlined object array allocation")                       \
                                                                            \
  develop(bool, UseFastLocking, true,                                       \
          "Use fast inlined locking code")                                  \
                                                                            \
  develop(bool, UseSlowPath, false,                                         \
          "For debugging: test slow cases by always using them")            \
                                                                            \
  develop(bool, GenerateArrayStoreCheck, true,                              \
          "Generates code for array store checks")                          \
                                                                            \
  develop(bool, DeoptC1, true,                                              \
          "Use deoptimization in C1")                                       \
                                                                            \
  develop(bool, PrintBailouts, false,                                       \
          "Print bailout and its reason")                                   \
                                                                            \
  develop(bool, TracePatching, false,                                       \
         "Trace patching of field access on uninitialized classes")         \
                                                                            \
  develop(bool, PatchALot, false,                                           \
          "Marks all fields as having unloaded classes")                    \
                                                                            \
  develop(bool, PrintNotLoaded, false,                                      \
          "Prints where classes are not loaded during code generation")     \
                                                                            \
  develop(bool, PrintLIR, false,                                            \
          "print low-level IR")                                             \
                                                                            \
  develop(bool, BailoutAfterHIR, false,                                     \
          "bailout of compilation after building of HIR")                   \
                                                                            \
  develop(bool, BailoutAfterLIR, false,                                     \
          "bailout of compilation after building of LIR")                   \
                                                                            \
  develop(bool, BailoutOnExceptionHandlers, false,                          \
          "bailout of compilation for methods with exception handlers")     \
                                                                            \
  develop(bool, InstallMethods, true,                                       \
          "Install methods at the end of successful compilations")          \
                                                                            \
  develop(intx, NMethodSizeLimit, (64*K)*wordSize,                          \
          "Maximum size of a compiled method.")                             \
          range(0, max_jint)                                                \
                                                                            \
  develop(bool, TraceFPUStack, false,                                       \
          "Trace emulation of the FPU stack (intel only)")                  \
                                                                            \
  develop(bool, TraceFPURegisterUsage, false,                               \
          "Trace usage of FPU registers at start of blocks (intel only)")   \
                                                                            \
  develop(intx, InstructionCountCutoff, 37000,                              \
          "If GraphBuilder adds this many instructions, bails out")         \
          range(0, max_jint)                                                \
                                                                            \
  develop(bool, ComputeExactFPURegisterUsage, true,                         \
          "Compute additional live set for fpu registers to simplify fpu stack merge (Intel only)") \
                                                                            \
  product(bool, C1ProfileCalls, true,                                       \
          "Profile calls when generating code for updating MDOs")           \
                                                                            \
  product(bool, C1ProfileVirtualCalls, true,                                \
          "Profile virtual calls when generating code for updating MDOs")   \
                                                                            \
  product(bool, C1ProfileInlinedCalls, true,                                \
          "Profile inlined calls when generating code for updating MDOs")   \
                                                                            \
  product(bool, C1ProfileBranches, true,                                    \
          "Profile branches when generating code for updating MDOs")        \
                                                                            \
  product(bool, C1ProfileCheckcasts, true,                                  \
          "Profile checkcasts when generating code for updating MDOs")      \
                                                                            \
  product(bool, C1OptimizeVirtualCallProfiling, true,                       \
          "Use CHA and exact type results at call sites when updating MDOs")\
                                                                            \
  product(bool, C1UpdateMethodData, true,                                   \
          "Update MethodData*s in Tier1-generated code")                    \
                                                                            \
  develop(bool, PrintCFGToFile, false,                                      \
          "print control flow graph to a separate file during compilation")

// end of C1_FLAGS

DECLARE_FLAGS(C1_FLAGS)

#endif // SHARE_C1_C1_GLOBALS_HPP
