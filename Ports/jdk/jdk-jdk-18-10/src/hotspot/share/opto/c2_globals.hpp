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

#ifndef SHARE_OPTO_C2_GLOBALS_HPP
#define SHARE_OPTO_C2_GLOBALS_HPP

#include "opto/c2_globals_pd.hpp"
#include "runtime/globals_shared.hpp"
#include "utilities/macros.hpp"

//
// Defines all globals flags used by the server compiler.
//

#define C2_FLAGS(develop,                                                   \
                 develop_pd,                                                \
                 product,                                                   \
                 product_pd,                                                \
                 notproduct,                                                \
                 range,                                                     \
                 constraint)                                                \
                                                                            \
  product(bool, StressLCM, false, DIAGNOSTIC,                               \
          "Randomize instruction scheduling in LCM")                        \
                                                                            \
  product(bool, StressGCM, false, DIAGNOSTIC,                               \
          "Randomize instruction scheduling in GCM")                        \
                                                                            \
  product(bool, StressIGVN, false, DIAGNOSTIC,                              \
          "Randomize worklist traversal in IGVN")                           \
                                                                            \
  product(bool, StressCCP, false, DIAGNOSTIC,                               \
          "Randomize worklist traversal in CCP")                            \
                                                                            \
  product(uint, StressSeed, 0, DIAGNOSTIC,                                  \
          "Seed for randomized stress testing (if unset, a random one is "  \
          "generated). The seed is recorded in the compilation log, if "    \
          "available.")                                                     \
          range(0, max_juint)                                               \
                                                                            \
  develop(bool, StressMethodHandleLinkerInlining, false,                    \
          "Stress inlining through method handle linkers")                  \
                                                                            \
  develop(intx, OptoPrologueNops, 0,                                        \
          "Insert this many extra nop instructions "                        \
          "in the prologue of every nmethod")                               \
          range(0, 128)                                                     \
                                                                            \
  product_pd(intx, InteriorEntryAlignment,                                  \
          "Code alignment for interior entry points "                       \
          "in generated code (in bytes)")                                   \
          constraint(InteriorEntryAlignmentConstraintFunc, AfterErgo)       \
                                                                            \
  product(intx, MaxLoopPad, (OptoLoopAlignment-1),                          \
          "Align a loop if padding size in bytes is less or equal to this " \
          "value")                                                          \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxVectorSize, 64,                                          \
          "Max vector size in bytes, "                                      \
          "actual size could be less depending on elements type")           \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, ArrayOperationPartialInlineSize, 0, DIAGNOSTIC,             \
          "Partial inline size used for small array operations"             \
          "(e.g. copy,cmp) acceleration.")                                  \
          range(0, 64)                                                      \
                                                                            \
  product(bool, AlignVector, true,                                          \
          "Perform vector store/load alignment in loop")                    \
                                                                            \
  product(intx, NumberOfLoopInstrToAlign, 4,                                \
          "Number of first instructions in a loop to align")                \
          range(0, max_jint)                                                \
                                                                            \
  notproduct(intx, IndexSetWatch, 0,                                        \
          "Trace all operations on this IndexSet (-1 means all, 0 none)")   \
          range(-1, 0)                                                      \
                                                                            \
  develop(intx, OptoNodeListSize, 4,                                        \
          "Starting allocation size of Node_List data structures")          \
          range(1, max_jint)                                                \
                                                                            \
  develop(intx, OptoBlockListSize, 8,                                       \
          "Starting allocation size of Block_List data structures")         \
          range(1, max_jint)                                                \
                                                                            \
  develop(intx, OptoPeepholeAt, -1,                                         \
          "Apply peephole optimizations to this peephole rule")             \
                                                                            \
  notproduct(bool, PrintIdeal, false,                                       \
          "Print ideal graph before code generation")                       \
                                                                            \
  notproduct(uintx, PrintIdealIndentThreshold, 0,                           \
          "A depth threshold of ideal graph. Indentation is disabled "      \
          "when users attempt to dump an ideal graph deeper than it.")      \
                                                                            \
  notproduct(bool, PrintOpto, false,                                        \
          "Print compiler2 attempts")                                       \
                                                                            \
  notproduct(bool, PrintOptoInlining, false,                                \
          "Print compiler2 inlining decisions")                             \
                                                                            \
  notproduct(bool, VerifyIdealNodeCount, false,                             \
          "Verify that tracked dead ideal node count is accurate")          \
                                                                            \
  notproduct(bool, PrintIdealNodeCount, false,                              \
          "Print liveness counts of ideal nodes")                           \
                                                                            \
  product_pd(bool, IdealizeClearArrayNode, DIAGNOSTIC,                      \
          "Replace ClearArrayNode by subgraph of basic operations.")        \
                                                                            \
  develop(bool, OptoBreakpoint, false,                                      \
          "insert breakpoint at method entry")                              \
                                                                            \
  notproduct(bool, OptoBreakpointOSR, false,                                \
          "insert breakpoint at osr method entry")                          \
                                                                            \
  notproduct(intx, BreakAtNode, 0,                                          \
          "Break at construction of this Node (either _idx or _debug_idx)") \
                                                                            \
  notproduct(bool, OptoBreakpointC2R, false,                                \
          "insert breakpoint at runtime stub entry")                        \
                                                                            \
  notproduct(bool, OptoNoExecute, false,                                    \
          "Attempt to parse and compile but do not execute generated code") \
                                                                            \
  notproduct(bool, PrintOptoStatistics, false,                              \
          "Print New compiler statistics")                                  \
                                                                            \
  product(bool, PrintOptoAssembly, false, DIAGNOSTIC,                       \
          "Print New compiler assembly output")                             \
                                                                            \
  develop_pd(bool, OptoPeephole,                                            \
          "Apply peephole optimizations after register allocation")         \
                                                                            \
  notproduct(bool, PrintFrameConverterAssembly, false,                      \
          "Print New compiler assembly output for frame converters")        \
                                                                            \
  notproduct(bool, PrintParseStatistics, false,                             \
          "Print nodes, transforms and new values made per bytecode parsed")\
                                                                            \
  notproduct(bool, PrintOptoPeephole, false,                                \
          "Print New compiler peephole replacements")                       \
                                                                            \
  develop(bool, PrintCFGBlockFreq, false,                                   \
          "Print CFG block freqencies")                                     \
                                                                            \
  develop(bool, TraceOptoParse, false,                                      \
          "Trace bytecode parse and control-flow merge")                    \
                                                                            \
  product_pd(intx,  LoopUnrollLimit,                                        \
          "Unroll loop bodies with node count less than this")              \
          range(0, max_jint / 4)                                            \
                                                                            \
  product_pd(intx, LoopPercentProfileLimit,                                 \
             "Unroll loop bodies with % node count of profile limit")       \
             range(10, 100)                                                 \
                                                                            \
  product(intx,  LoopMaxUnroll, 16,                                         \
          "Maximum number of unrolls for main loop")                        \
          range(0, max_jint)                                                \
                                                                            \
  product_pd(bool,  SuperWordLoopUnrollAnalysis,                            \
           "Map number of unrolls for main loop via "                       \
           "Superword Level Parallelism analysis")                          \
                                                                            \
  product(bool, PostLoopMultiversioning, false, EXPERIMENTAL,               \
           "Multi versioned post loops to eliminate range checks")          \
                                                                            \
  notproduct(bool, TraceSuperWordLoopUnrollAnalysis, false,                 \
          "Trace what Superword Level Parallelism analysis applies")        \
                                                                            \
  product(bool, UseVectorMacroLogic, true, DIAGNOSTIC,                      \
          "Use ternary macro logic instructions")                           \
                                                                            \
  product(intx,  LoopUnrollMin, 4,                                          \
          "Minimum number of unroll loop bodies before checking progress"   \
          "of rounds of unroll,optimize,..")                                \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, UseSubwordForMaxVector, true,                               \
          "Use Subword Analysis to set maximum vector size")                \
                                                                            \
  product(bool, UseVectorCmov, false,                                       \
          "Use Vectorized Cmov")                                            \
                                                                            \
  develop(intx, UnrollLimitForProfileCheck, 1,                              \
          "Don't use profile_trip_cnt() to restrict unrolling until "       \
          "unrolling would push the number of unrolled iterations above "   \
          "UnrollLimitForProfileCheck. A higher value allows more "         \
          "unrolling. Zero acts as a very large value." )                   \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, MultiArrayExpandLimit, 6,                                   \
          "Maximum number of individual allocations in an inline-expanded " \
          "multianewarray instruction")                                     \
          range(0, max_jint)                                                \
                                                                            \
  notproduct(bool, TraceProfileTripCount, false,                            \
          "Trace profile loop trip count information")                      \
                                                                            \
  product(bool, UseCountedLoopSafepoints, false,                            \
          "Force counted loops to keep a safepoint")                        \
                                                                            \
  product(bool, UseLoopPredicate, true,                                     \
          "Generate a predicate to select fast/slow loop versions")         \
                                                                            \
  develop(bool, TraceLoopPredicate, false,                                  \
          "Trace generation of loop predicates")                            \
                                                                            \
  develop(bool, TraceLoopOpts, false,                                       \
          "Trace executed loop optimizations")                              \
                                                                            \
  develop(bool, TraceLoopLimitCheck, false,                                 \
          "Trace generation of loop limits checks")                         \
                                                                            \
  develop(bool, TraceRangeLimitCheck, false,                                \
          "Trace additional overflow checks in RCE")                        \
                                                                            \
  /* OptimizeFill not yet supported on PowerPC. */                          \
  product(bool, OptimizeFill, true PPC64_ONLY(&& false),                    \
          "convert fill/copy loops into intrinsic")                         \
                                                                            \
  develop(bool, TraceOptimizeFill, false,                                   \
          "print detailed information about fill conversion")               \
                                                                            \
  develop(bool, OptoCoalesce, true,                                         \
          "Use Conservative Copy Coalescing in the Register Allocator")     \
                                                                            \
  develop(bool, UseUniqueSubclasses, true,                                  \
          "Narrow an abstract reference to the unique concrete subclass")   \
                                                                            \
  product(intx, TrackedInitializationLimit, 50,                             \
          "When initializing fields, track up to this many words")          \
          range(0, 65535)                                                   \
                                                                            \
  product(bool, ReduceFieldZeroing, true,                                   \
          "When initializing fields, try to avoid needless zeroing")        \
                                                                            \
  product(bool, ReduceInitialCardMarks, true,                               \
          "When initializing fields, try to avoid needless card marks")     \
                                                                            \
  product(bool, ReduceBulkZeroing, true,                                    \
          "When bulk-initializing, try to avoid needless zeroing")          \
                                                                            \
  product(bool, UseFPUForSpilling, false,                                   \
          "Spill integer registers to FPU instead of stack when possible")  \
                                                                            \
  develop_pd(intx, RegisterCostAreaRatio,                                   \
          "Spill selection in reg allocator: scale area by (X/64K) before " \
          "adding cost")                                                    \
                                                                            \
  develop_pd(bool, UseCISCSpill,                                            \
          "Use ADLC supplied cisc instructions during allocation")          \
                                                                            \
  notproduct(bool, VerifyGraphEdges , false,                                \
          "Verify Bi-directional Edges")                                    \
                                                                            \
  notproduct(bool, VerifyDUIterators, true,                                 \
          "Verify the safety of all iterations of Bi-directional Edges")    \
                                                                            \
  notproduct(bool, VerifyHashTableKeys, true,                               \
          "Verify the immutability of keys in the VN hash tables")          \
                                                                            \
  notproduct(bool, VerifyRegisterAllocator , false,                         \
          "Verify Register Allocator")                                      \
                                                                            \
  develop(intx, FLOATPRESSURE, -1,                                          \
          "Number of float LRG's that constitute high register pressure."   \
          "-1: means the threshold is determined by number of available "   \
          "float register for allocation")                                  \
          range(-1, max_jint)                                               \
                                                                            \
  develop(intx, INTPRESSURE, -1,                                            \
          "Number of integer LRG's that constitute high register pressure." \
          "-1: means the threshold is determined by number of available "   \
          "integer register for allocation")                                \
          range(-1, max_jint)                                               \
                                                                            \
  notproduct(bool, TraceOptoPipelining, false,                              \
          "Trace pipelining information")                                   \
                                                                            \
  notproduct(bool, TraceOptoOutput, false,                                  \
          "Trace pipelining information")                                   \
                                                                            \
  product_pd(bool, OptoScheduling,                                          \
          "Instruction Scheduling after register allocation")               \
                                                                            \
  product_pd(bool, OptoRegScheduling,                                       \
          "Instruction Scheduling before register allocation for pressure") \
                                                                            \
  product(bool, PartialPeelLoop, true,                                      \
          "Partial peel (rotate) loops")                                    \
                                                                            \
  product(intx, PartialPeelNewPhiDelta, 0,                                  \
          "Additional phis that can be created by partial peeling")         \
          range(0, max_jint)                                                \
                                                                            \
  notproduct(bool, TracePartialPeeling, false,                              \
          "Trace partial peeling (loop rotation) information")              \
                                                                            \
  product(bool, PartialPeelAtUnsignedTests, true,                           \
          "Partial peel at unsigned tests if no signed test exists")        \
                                                                            \
  product(bool, ReassociateInvariants, true,                                \
          "Enable reassociation of expressions with loop invariants.")      \
                                                                            \
  product(bool, LoopUnswitching, true,                                      \
          "Enable loop unswitching (a form of invariant test hoisting)")    \
                                                                            \
  notproduct(bool, TraceLoopUnswitching, false,                             \
          "Trace loop unswitching")                                         \
                                                                            \
  product(bool, AllowVectorizeOnDemand, true,                               \
          "Globally supress vectorization set in VectorizeMethod")          \
                                                                            \
  product(bool, UseSuperWord, true,                                         \
          "Transform scalar operations into superword operations")          \
                                                                            \
  develop(bool, SuperWordRTDepCheck, false,                                 \
          "Enable runtime dependency checks.")                              \
                                                                            \
  product(bool, SuperWordReductions, true,                                  \
          "Enable reductions support in superword.")                        \
                                                                            \
  product(bool, UseCMoveUnconditionally, false,                             \
          "Use CMove (scalar and vector) ignoring profitability test.")     \
                                                                            \
  product(bool, DoReserveCopyInSuperWord, true,                             \
          "Create reserve copy of graph in SuperWord.")                     \
                                                                            \
  notproduct(bool, TraceSuperWord, false,                                   \
          "Trace superword transforms")                                     \
                                                                            \
  notproduct(bool, TraceNewVectors, false,                                  \
          "Trace creation of Vector nodes")                                 \
                                                                            \
  product_pd(bool, OptoBundling,                                            \
          "Generate nops to fill i-cache lines")                            \
                                                                            \
  product_pd(intx, ConditionalMoveLimit,                                    \
          "Limit of ops to make speculative when using CMOVE")              \
          range(0, max_jint)                                                \
                                                                            \
  notproduct(bool, PrintIdealGraph, false,                                  \
          "Print ideal graph to XML file / network interface. "             \
          "By default attempts to connect to the visualizer on a socket.")  \
                                                                            \
  notproduct(intx, PrintIdealGraphLevel, 0,                                 \
          "Level of detail of the ideal graph printout. "                   \
          "System-wide value, -1=printing is disabled, "                    \
          "0=print nothing except IGVPrintLevel directives, "               \
          "4=all details printed. "                                         \
          "Level of detail of printouts can be set on a per-method level "  \
          "as well by using CompileCommand=option.")                        \
          range(-1, 4)                                                      \
                                                                            \
  notproduct(intx, PrintIdealGraphPort, 4444,                               \
          "Ideal graph printer to network port")                            \
          range(0, SHRT_MAX)                                                \
                                                                            \
  notproduct(ccstr, PrintIdealGraphAddress, "127.0.0.1",                    \
          "IP address to connect to visualizer")                            \
                                                                            \
  notproduct(ccstr, PrintIdealGraphFile, NULL,                              \
          "File to dump ideal graph to.  If set overrides the "             \
          "use of the network")                                             \
                                                                            \
  product(bool, UseBimorphicInlining, true,                                 \
          "Profiling based inlining for two receivers")                     \
                                                                            \
  product(bool, UseOnlyInlinedBimorphic, true,                              \
          "Don't use BimorphicInlining if can't inline a second method")    \
                                                                            \
  develop(bool, SubsumeLoads, true,                                         \
          "Attempt to compile while subsuming loads into machine "          \
          "instructions.")                                                  \
                                                                            \
  develop(bool, StressRecompilation, false,                                 \
          "Recompile each compiled method without subsuming loads "         \
          "or escape analysis.")                                            \
                                                                            \
  develop(intx, ImplicitNullCheckThreshold, 3,                              \
          "Don't do implicit null checks if NPE's in a method exceeds "     \
          "limit")                                                          \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, LoopOptsCount, 43,                                          \
          "Set level of loop optimization for tier 1 compiles")             \
          range(5, 43)                                                      \
                                                                            \
  /* controls for heat-based inlining */                                    \
                                                                            \
  develop(intx, NodeCountInliningCutoff, 18000,                             \
          "If parser node generation exceeds limit stop inlining")          \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxNodeLimit, 80000,                                        \
          "Maximum number of nodes")                                        \
          range(1000, max_jint / 3)                                         \
                                                                            \
  product(intx, NodeLimitFudgeFactor, 2000,                                 \
          "Fudge Factor for certain optimizations")                         \
          constraint(NodeLimitFudgeFactorConstraintFunc, AfterErgo)         \
                                                                            \
  product(bool, UseJumpTables, true,                                        \
          "Use JumpTables instead of a binary search tree for switches")    \
                                                                            \
  product(bool, UseDivMod, true,                                            \
          "Use combined DivMod instruction if available")                   \
                                                                            \
  product_pd(intx, MinJumpTableSize,                                        \
          "Minimum number of targets in a generated jump table")            \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, MaxJumpTableSize, 65000,                                    \
          "Maximum number of targets in a generated jump table")            \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, MaxJumpTableSparseness, 5,                                  \
          "Maximum sparseness for jumptables")                              \
          range(0, max_intx / 4)                                            \
                                                                            \
  product(bool, EliminateLocks, true,                                       \
          "Coarsen locks when possible")                                    \
                                                                            \
  product(bool, EliminateNestedLocks, true,                                 \
          "Eliminate nested locks of the same object when possible")        \
                                                                            \
  notproduct(bool, PrintLockStatistics, false,                              \
          "Print precise statistics on the dynamic lock usage")             \
                                                                            \
  product(bool, PrintPreciseRTMLockingStatistics, false, DIAGNOSTIC,        \
          "Print per-lock-site statistics of rtm locking in JVM")           \
                                                                            \
  notproduct(bool, PrintEliminateLocks, false,                              \
          "Print out when locks are eliminated")                            \
                                                                            \
  product(bool, EliminateAutoBox, true,                                     \
          "Control optimizations for autobox elimination")                  \
                                                                            \
  product(intx, AutoBoxCacheMax, 128,                                       \
          "Sets max value cached by the java.lang.Integer autobox cache")   \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, AggressiveUnboxing, true, DIAGNOSTIC,                       \
          "Control optimizations for aggressive boxing elimination")        \
                                                                            \
  develop(bool, TracePostallocExpand, false, "Trace expanding nodes after"  \
          " register allocation.")                                          \
                                                                            \
  product(bool, DoEscapeAnalysis, true,                                     \
          "Perform escape analysis")                                        \
                                                                            \
  product(double, EscapeAnalysisTimeout, 20. DEBUG_ONLY(+40.),              \
          "Abort EA when it reaches time limit (in sec)")                   \
          range(0, DBL_MAX)                                                 \
                                                                            \
  develop(bool, ExitEscapeAnalysisOnTimeout, true,                          \
          "Exit or throw assert in EA when it reaches time limit")          \
                                                                            \
  notproduct(bool, PrintEscapeAnalysis, false,                              \
          "Print the results of escape analysis")                           \
                                                                            \
  product(bool, EliminateAllocations, true,                                 \
          "Use escape analysis to eliminate allocations")                   \
                                                                            \
  notproduct(bool, PrintEliminateAllocations, false,                        \
          "Print out when allocations are eliminated")                      \
                                                                            \
  product(intx, EliminateAllocationArraySizeLimit, 64,                      \
          "Array size (number of elements) limit for scalar replacement")   \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, EliminateAllocationFieldsLimit, 512, DIAGNOSTIC,            \
          "Number of fields in instance limit for scalar replacement")      \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, OptimizePtrCompare, true,                                   \
          "Use escape analysis to optimize pointers compare")               \
                                                                            \
  notproduct(bool, PrintOptimizePtrCompare, false,                          \
          "Print information about optimized pointers compare")             \
                                                                            \
  notproduct(bool, VerifyConnectionGraph , true,                            \
          "Verify Connection Graph construction in Escape Analysis")        \
                                                                            \
  product(bool, OptimizeStringConcat, true,                                 \
          "Optimize the construction of Strings by StringBuilder")          \
                                                                            \
  notproduct(bool, PrintOptimizeStringConcat, false,                        \
          "Print information about transformations performed on Strings")   \
                                                                            \
  product(intx, ValueSearchLimit, 1000,                                     \
          "Recursion limit in PhaseMacroExpand::value_from_mem_phi")        \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxLabelRootDepth, 1100,                                    \
          "Maximum times call Label_Root to prevent stack overflow")        \
          range(100, max_jint)                                              \
                                                                            \
  product(intx, DominatorSearchLimit, 1000, DIAGNOSTIC,                     \
          "Iterations limit in Node::dominates")                            \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, BlockLayoutByFrequency, true,                               \
          "Use edge frequencies to drive block ordering")                   \
                                                                            \
  product(intx, BlockLayoutMinDiamondPercentage, 20,                        \
          "Miniumum %% of a successor (predecessor) for which block "       \
          "layout a will allow a fork (join) in a single chain")            \
          range(0, 100)                                                     \
                                                                            \
  product(bool, BlockLayoutRotateLoops, true,                               \
          "Allow back branches to be fall throughs in the block layout")    \
                                                                            \
  product(bool, InlineReflectionGetCallerClass, true, DIAGNOSTIC,           \
          "inline sun.reflect.Reflection.getCallerClass(), known to be "    \
          "part of base library DLL")                                       \
                                                                            \
  product(bool, InlineObjectCopy, true, DIAGNOSTIC,                         \
          "inline Object.clone and Arrays.copyOf[Range] intrinsics")        \
                                                                            \
  product(bool, SpecialStringCompareTo, true, DIAGNOSTIC,                   \
          "special version of string compareTo")                            \
                                                                            \
  product(bool, SpecialStringIndexOf, true, DIAGNOSTIC,                     \
          "special version of string indexOf")                              \
                                                                            \
  product(bool, SpecialStringEquals, true, DIAGNOSTIC,                      \
          "special version of string equals")                               \
                                                                            \
  product(bool, SpecialArraysEquals, true, DIAGNOSTIC,                      \
          "special version of Arrays.equals(char[],char[])")                \
                                                                            \
  product(bool, SpecialEncodeISOArray, true, DIAGNOSTIC,                    \
          "special version of ISO_8859_1$Encoder.encodeISOArray")           \
                                                                            \
  develop(bool, BailoutToInterpreterForThrows, false,                       \
          "Compiled methods which throws/catches exceptions will be "       \
          "deopt and intp.")                                                \
                                                                            \
  develop(bool, ConvertCmpD2CmpF, true,                                     \
          "Convert cmpD to cmpF when one input is constant in float range") \
                                                                            \
  develop(bool, ConvertFloat2IntClipping, true,                             \
          "Convert float2int clipping idiom to integer clipping")           \
                                                                            \
  develop(bool, MonomorphicArrayCheck, true,                                \
          "Uncommon-trap array store checks that require full type check")  \
                                                                            \
  notproduct(bool, TracePhaseCCP, false,                                    \
          "Print progress during Conditional Constant Propagation")         \
                                                                            \
  develop(bool, PrintDominators, false,                                     \
          "Print out dominator trees for GVN")                              \
                                                                            \
  product(bool, TraceSpilling, false, DIAGNOSTIC,                           \
          "Trace spilling")                                                 \
                                                                            \
  product(bool, TraceTypeProfile, false, DIAGNOSTIC,                        \
          "Trace type profile")                                             \
                                                                            \
  develop(bool, PoisonOSREntry, true,                                       \
           "Detect abnormal calls to OSR code")                             \
                                                                            \
  develop(bool, SoftMatchFailure, trueInProduct,                            \
          "If the DFA fails to match a node, print a message and bail out") \
                                                                            \
  develop(bool, InlineAccessors, true,                                      \
          "inline accessor methods (get/set)")                              \
                                                                            \
  product(intx, TypeProfileMajorReceiverPercent, 90,                        \
          "% of major receiver type to all profiled receivers")             \
          range(0, 100)                                                     \
                                                                            \
  product(bool, PrintIntrinsics, false, DIAGNOSTIC,                         \
          "prints attempted and successful inlining of intrinsics")         \
                                                                            \
  develop(bool, StressReflectiveCode, false,                                \
          "Use inexact types at allocations, etc., to test reflection")     \
                                                                            \
  product(bool, DebugInlinedCalls, true, DIAGNOSTIC,                        \
         "If false, restricts profiled locations to the root method only")  \
                                                                            \
  notproduct(bool, VerifyLoopOptimizations, false,                          \
          "verify major loop optimizations")                                \
                                                                            \
  product(bool, ProfileDynamicTypes, true, DIAGNOSTIC,                      \
          "do extra type profiling and use it more aggressively")           \
                                                                            \
  develop(bool, TraceIterativeGVN, false,                                   \
          "Print progress during Iterative Global Value Numbering")         \
                                                                            \
  develop(bool, VerifyIterativeGVN, false,                                  \
          "Verify Def-Use modifications during sparse Iterative Global "    \
          "Value Numbering")                                                \
                                                                            \
  notproduct(bool, TraceCISCSpill, false,                                   \
          "Trace allocators use of cisc spillable instructions")            \
                                                                            \
  product(bool, SplitIfBlocks, true,                                        \
          "Clone compares and control flow through merge points to fold "   \
          "some branches")                                                  \
                                                                            \
  develop(intx, FreqCountInvocations,  1,                                   \
          "Scaling factor for branch frequencies (deprecated)")             \
          range(1, max_intx)                                                \
                                                                            \
  product(intx, AliasLevel,     3,                                          \
          "0 for no aliasing, 1 for oop/field/static/array split, "         \
          "2 for class split, 3 for unique instances")                      \
          range(0, 3)                                                       \
          constraint(AliasLevelConstraintFunc,AfterErgo)                    \
                                                                            \
  develop(bool, VerifyAliases, false,                                       \
          "perform extra checks on the results of alias analysis")          \
                                                                            \
  product(intx, MaxInlineLevel, 15,                                         \
          "maximum number of nested calls that are inlined by high tier "   \
          "compiler")                                                       \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxRecursiveInlineLevel, 1,                                 \
          "maximum number of nested recursive calls that are inlined by "   \
          "high tier compiler")                                             \
          range(0, max_jint)                                                \
                                                                            \
  product_pd(intx, InlineSmallCode,                                         \
          "Only inline already compiled methods if their code size is "     \
          "less than this")                                                 \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxInlineSize, 35,                                          \
          "The maximum bytecode size of a method to be inlined by high "    \
          "tier compiler")                                                  \
          range(0, max_jint)                                                \
                                                                            \
  product_pd(intx, FreqInlineSize,                                          \
          "The maximum bytecode size of a frequent method to be inlined")   \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxTrivialSize, 6,                                          \
          "The maximum bytecode size of a trivial method to be inlined by " \
          "high tier compiler")                                             \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, IncrementalInline, true,                                    \
          "do post parse inlining")                                         \
                                                                            \
  product(bool, IncrementalInlineMH, true, DIAGNOSTIC,                      \
          "do post parse inlining of method handle calls")                  \
                                                                            \
  product(bool, IncrementalInlineVirtual, true, DIAGNOSTIC,                 \
          "do post parse inlining of virtual calls")                        \
                                                                            \
  develop(bool, AlwaysIncrementalInline, false,                             \
          "do all inlining incrementally")                                  \
                                                                            \
  product(bool, IncrementalInlineForceCleanup, false, DIAGNOSTIC,           \
          "do cleanup after every iteration of incremental inlining")       \
                                                                            \
  product(intx, LiveNodeCountInliningCutoff, 40000,                         \
          "max number of live nodes in a method")                           \
          range(0, max_juint / 8)                                           \
                                                                            \
  product(bool, OptimizeExpensiveOps, true, DIAGNOSTIC,                     \
          "Find best control for expensive operations")                     \
                                                                            \
  product(bool, UseMathExactIntrinsics, true, DIAGNOSTIC,                   \
          "Enables intrinsification of various java.lang.Math functions")   \
                                                                            \
  product(bool, UseCharacterCompareIntrinsics, false, DIAGNOSTIC,           \
          "Enables intrinsification of java.lang.Character functions")      \
                                                                            \
  product(bool, UseMultiplyToLenIntrinsic, false, DIAGNOSTIC,               \
          "Enables intrinsification of BigInteger.multiplyToLen()")         \
                                                                            \
  product(bool, UseSquareToLenIntrinsic, false, DIAGNOSTIC,                 \
          "Enables intrinsification of BigInteger.squareToLen()")           \
                                                                            \
  product(bool, UseMulAddIntrinsic, false, DIAGNOSTIC,                      \
          "Enables intrinsification of BigInteger.mulAdd()")                \
                                                                            \
  product(bool, UseMontgomeryMultiplyIntrinsic, false, DIAGNOSTIC,          \
          "Enables intrinsification of BigInteger.montgomeryMultiply()")    \
                                                                            \
  product(bool, UseMontgomerySquareIntrinsic, false, DIAGNOSTIC,            \
          "Enables intrinsification of BigInteger.montgomerySquare()")      \
                                                                            \
  product(bool, EnableVectorSupport, false, EXPERIMENTAL,                   \
          "Enables VectorSupport intrinsics")                               \
                                                                            \
  product(bool, EnableVectorReboxing, false, EXPERIMENTAL,                  \
          "Enables reboxing of vectors")                                    \
                                                                            \
  product(bool, EnableVectorAggressiveReboxing, false, EXPERIMENTAL,        \
          "Enables aggressive reboxing of vectors")                         \
                                                                            \
  product(bool, UseVectorStubs, false, EXPERIMENTAL,                        \
          "Use stubs for vector transcendental operations")                 \
                                                                            \
  product(bool, UseTypeSpeculation, true,                                   \
          "Speculatively propagate types from profiles")                    \
                                                                            \
  product(bool, UseInlineDepthForSpeculativeTypes, true, DIAGNOSTIC,        \
          "Carry inline depth of profile point with speculative type "      \
          "and give priority to profiling from lower inline depth")         \
                                                                            \
  product_pd(bool, TrapBasedRangeChecks,                                    \
          "Generate code for range checks that uses a cmp and trap "        \
          "instruction raising SIGTRAP. Used on PPC64.")                    \
                                                                            \
  product(intx, ArrayCopyLoadStoreMaxElem, 8,                               \
          "Maximum number of arraycopy elements inlined as a sequence of"   \
          "loads/stores")                                                   \
          range(0, max_intx)                                                \
                                                                            \
  develop(bool, StressArrayCopyMacroNode, false,                            \
          "Perform ArrayCopy load/store replacement during IGVN only")      \
                                                                            \
  develop(bool, RenumberLiveNodes, true,                                    \
          "Renumber live nodes")                                            \
                                                                            \
  product(uintx, LoopStripMiningIter, 0,                                    \
          "Number of iterations in strip mined loop")                       \
          range(0, max_juint)                                               \
          constraint(LoopStripMiningIterConstraintFunc, AfterErgo)          \
                                                                            \
  product(uintx, LoopStripMiningIterShortLoop, 0,                           \
          "Loop with fewer iterations are not strip mined")                 \
          range(0, max_juint)                                               \
                                                                            \
  product(bool, UseProfiledLoopPredicate, true,                             \
          "Move predicates out of loops based on profiling data")           \
                                                                            \
  product(bool, ExpandSubTypeCheckAtParseTime, false, DIAGNOSTIC,           \
          "Do not use subtype check macro node")                            \
                                                                            \
  develop(uintx, StressLongCountedLoop, 0,                                  \
          "if > 0, convert int counted loops to long counted loops"         \
          "to stress handling of long counted loops: run inner loop"        \
          "for at most jint_max / StressLongCountedLoop")                   \
          range(0, max_juint)                                               \
                                                                            \
  product(bool, VerifyReceiverTypes, trueInDebug, DIAGNOSTIC,               \
          "Verify receiver types at runtime")                               \

// end of C2_FLAGS

DECLARE_FLAGS(C2_FLAGS)

#endif // SHARE_OPTO_C2_GLOBALS_HPP
