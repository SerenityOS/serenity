/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILER_GLOBALS_HPP
#define SHARE_COMPILER_COMPILER_GLOBALS_HPP

#include "compiler/compiler_globals_pd.hpp"
#include "runtime/globals_shared.hpp"
#ifdef COMPILER1
#include "c1/c1_globals.hpp"
#endif // COMPILER1
#ifdef COMPILER2
#include "opto/c2_globals.hpp"
#endif // COMPILER2
#if INCLUDE_JVMCI
#include "jvmci/jvmci_globals.hpp"
#endif

// TODO -- currently, even if all JIT compilers are disabled, the following flags
// are still available in HotSpot. This should eventually be fixed ...

#define COMPILER_FLAGS(develop,                                             \
                       develop_pd,                                          \
                       product,                                             \
                       product_pd,                                          \
                       notproduct,                                          \
                       range,                                               \
                       constraint)                                          \
                                                                            \
  /* compiler interface */                                                  \
                                                                            \
  develop(bool, CIPrintCompilerName, false,                                 \
          "when CIPrint is active, print the name of the active compiler")  \
                                                                            \
  product(bool, CIPrintCompileQueue, false, DIAGNOSTIC,                     \
          "display the contents of the compile queue whenever a "           \
          "compilation is enqueued")                                        \
                                                                            \
  develop(bool, CIPrintRequests, false,                                     \
          "display every request for compilation")                          \
                                                                            \
  product(bool, CITime, false,                                              \
          "collect timing information for compilation")                     \
                                                                            \
  develop(bool, CITimeVerbose, false,                                       \
          "be more verbose in compilation timings")                         \
                                                                            \
  develop(bool, CITimeEach, false,                                          \
          "display timing information after each successful compilation")   \
                                                                            \
  develop(bool, CICountOSR, false,                                          \
          "use a separate counter when assigning ids to osr compilations")  \
                                                                            \
  develop(bool, CICountNative, false,                                       \
          "use a separate counter when assigning ids to native "            \
          "compilations")                                                   \
                                                                            \
  develop(bool, CICompileNatives, true,                                     \
          "compile native methods if supported by the compiler")            \
                                                                            \
  develop_pd(bool, CICompileOSR,                                            \
          "compile on stack replacement methods if supported by the "       \
          "compiler")                                                       \
                                                                            \
  develop(bool, CIPrintMethodCodes, false,                                  \
          "print method bytecodes of the compiled code")                    \
                                                                            \
  develop(bool, CIPrintTypeFlow, false,                                     \
          "print the results of ciTypeFlow analysis")                       \
                                                                            \
  develop(bool, CITraceTypeFlow, false,                                     \
          "detailed per-bytecode tracing of ciTypeFlow analysis")           \
                                                                            \
  product(bool, CICompilerCountPerCPU, false,                               \
          "1 compiler thread for log(N CPUs)")                              \
                                                                            \
  notproduct(intx, CICrashAt, -1,                                           \
          "id of compilation to trigger assert in compiler thread for "     \
          "the purpose of testing, e.g. generation of replay data")         \
                                                                            \
  notproduct(bool, CIObjectFactoryVerify, false,                            \
          "enable potentially expensive verification in ciObjectFactory")   \
                                                                            \
  develop(intx, CIStart, 0,                                                 \
          "The id of the first compilation to permit")                      \
                                                                            \
  develop(intx, CIStop, max_jint,                                           \
          "The id of the last compilation to permit")                       \
                                                                            \
  develop(intx, CIStartOSR, 0,                                              \
          "The id of the first osr compilation to permit "                  \
          "(CICountOSR must be on)")                                        \
                                                                            \
  develop(intx, CIStopOSR, max_jint,                                        \
          "The id of the last osr compilation to permit "                   \
          "(CICountOSR must be on)")                                        \
                                                                            \
  develop(intx, CIBreakAtOSR, -1,                                           \
          "The id of osr compilation to break at")                          \
                                                                            \
  develop(intx, CIBreakAt, -1,                                              \
          "The id of compilation to break at")                              \
                                                                            \
  /* recompilation */                                                       \
                                                                            \
  product(double, CompileThresholdScaling, 1.0,                             \
          "Factor to control when first compilation happens "               \
          "(both with and without tiered compilation): "                    \
          "values greater than 1.0 delay counter overflow, "                \
          "values between 0 and 1.0 rush counter overflow, "                \
          "value of 1.0 leaves compilation thresholds unchanged "           \
          "value of 0.0 is equivalent to -Xint. "                           \
          ""                                                                \
          "Flag can be set as per-method option. "                          \
          "If a value is specified for a method, compilation thresholds "   \
          "for that method are scaled by both the value of the global flag "\
          "and the value of the per-method flag.")                          \
          range(0.0, DBL_MAX)                                               \
                                                                            \
  product(intx, Tier0InvokeNotifyFreqLog, 7,                                \
          "Interpreter (tier 0) invocation notification frequency")         \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier2InvokeNotifyFreqLog, 11,                               \
          "C1 without MDO (tier 2) invocation notification frequency")      \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier3InvokeNotifyFreqLog, 10,                               \
          "C1 with MDO profiling (tier 3) invocation notification "         \
          "frequency")                                                      \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier23InlineeNotifyFreqLog, 20,                             \
          "Inlinee invocation (tiers 2 and 3) notification frequency")      \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier0BackedgeNotifyFreqLog, 10,                             \
          "Interpreter (tier 0) invocation notification frequency")         \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier2BackedgeNotifyFreqLog, 14,                             \
          "C1 without MDO (tier 2) invocation notification frequency")      \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier3BackedgeNotifyFreqLog, 13,                             \
          "C1 with MDO profiling (tier 3) invocation notification "         \
          "frequency")                                                      \
          range(0, 30)                                                      \
                                                                            \
  product(intx, Tier2CompileThreshold, 0,                                   \
          "threshold at which tier 2 compilation is invoked")               \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier2BackEdgeThreshold, 0,                                  \
          "Back edge threshold at which tier 2 compilation is invoked")     \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3InvocationThreshold, 200,                              \
          "Compile if number of method invocations crosses this "           \
          "threshold")                                                      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3MinInvocationThreshold, 100,                           \
          "Minimum invocation to compile at tier 3")                        \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3CompileThreshold, 2000,                                \
          "Threshold at which tier 3 compilation is invoked (invocation "   \
          "minimum must be satisfied)")                                     \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3BackEdgeThreshold,  60000,                             \
          "Back edge threshold at which tier 3 OSR compilation is invoked") \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier4InvocationThreshold, 5000,                             \
          "Compile if number of method invocations crosses this "           \
          "threshold")                                                      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier4MinInvocationThreshold, 600,                           \
          "Minimum invocation to compile at tier 4")                        \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier4CompileThreshold, 15000,                               \
          "Threshold at which tier 4 compilation is invoked (invocation "   \
          "minimum must be satisfied)")                                     \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier4BackEdgeThreshold, 40000,                              \
          "Back edge threshold at which tier 4 OSR compilation is invoked") \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier0Delay, 20, DIAGNOSTIC,                                 \
          "If C2 queue size grows over this amount per compiler thread "    \
          "do not start profiling in the interpreter")                      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3DelayOn, 5,                                            \
          "If C2 queue size grows over this amount per compiler thread "    \
          "stop compiling at tier 3 and start compiling at tier 2")         \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3DelayOff, 2,                                           \
          "If C2 queue size is less than this amount per compiler thread "  \
          "allow methods compiled at tier 2 transition to tier 3")          \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier3LoadFeedback, 5,                                       \
          "Tier 3 thresholds will increase twofold when C1 queue size "     \
          "reaches this amount per compiler thread")                        \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, Tier4LoadFeedback, 3,                                       \
          "Tier 4 thresholds will increase twofold when C2 queue size "     \
          "reaches this amount per compiler thread")                        \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, TieredCompileTaskTimeout, 50,                               \
          "Kill compile task if method was not used within "                \
          "given timeout in milliseconds")                                  \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, TieredStopAtLevel, 4,                                       \
          "Stop at given compilation level")                                \
          range(0, 4)                                                       \
                                                                            \
  product(intx, Tier0ProfilingStartPercentage, 200,                         \
          "Start profiling in interpreter if the counters exceed the "      \
          "specified percentage of tier 3 thresholds (tier 4 thresholds "   \
          "with CompilationMode=high-only|high-only-quick-internal)")       \
          range(0, max_jint)                                                \
                                                                            \
  product(uintx, IncreaseFirstTierCompileThresholdAt, 50,                   \
          "Increase the compile threshold for C1 compilation if the code "  \
          "cache is filled by the specified percentage")                    \
          range(0, 99)                                                      \
                                                                            \
  product(intx, TieredRateUpdateMinTime, 1,                                 \
          "Minimum rate sampling interval (in milliseconds)")               \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, TieredRateUpdateMaxTime, 25,                                \
          "Maximum rate sampling interval (in milliseconds)")               \
          range(0, max_intx)                                                \
                                                                            \
  product(ccstr, CompilationMode, "default",                                \
          "Compilation modes: "                                             \
          "default: normal tiered compilation; "                            \
          "quick-only: C1-only mode; "                                      \
          "high-only: C2/JVMCI-only mode; "                                 \
          "high-only-quick-internal: C2/JVMCI-only mode, "                  \
          "with JVMCI compiler compiled with C1.")                          \
                                                                            \
  product(bool, PrintTieredEvents, false,                                   \
          "Print tiered events notifications")                              \
                                                                            \
  product_pd(intx, OnStackReplacePercentage,                                \
          "NON_TIERED number of method invocations/branches (expressed as " \
          "% of CompileThreshold) before (re-)compiling OSR code")          \
          constraint(OnStackReplacePercentageConstraintFunc, AfterErgo)     \
                                                                            \
  product(intx, InterpreterProfilePercentage, 33,                           \
          "NON_TIERED number of method invocations/branches (expressed as " \
          "% of CompileThreshold) before profiling in the interpreter")     \
          range(0, 100)                                                     \
                                                                            \
  /* compiler directives */                                                 \
                                                                            \
  product(ccstrlist, CompileOnly, "",                                       \
          "List of methods (pkg/class.name) to restrict compilation to")    \
                                                                            \
  product(ccstr, CompileCommandFile, NULL,                                  \
          "Read compiler commands from this file [.hotspot_compiler]")      \
                                                                            \
  product(ccstr, CompilerDirectivesFile, NULL, DIAGNOSTIC,                  \
          "Read compiler directives from this file")                        \
                                                                            \
  product(ccstrlist, CompileCommand, "",                                    \
          "Prepend to .hotspot_compiler; e.g. log,java/lang/String.<init>") \
                                                                            \
  develop(bool, ReplayCompiles, false,                                      \
          "Enable replay of compilations from ReplayDataFile")              \
                                                                            \
  product(ccstr, ReplayDataFile, NULL,                                      \
          "File containing compilation replay information"                  \
          "[default: ./replay_pid%p.log] (%p replaced with pid)")           \
                                                                            \
  product(ccstr, InlineDataFile, NULL,                                      \
          "File containing inlining replay information"                     \
          "[default: ./inline_pid%p.log] (%p replaced with pid)")           \
                                                                            \
  develop(intx, ReplaySuppressInitializers, 2,                              \
          "Control handling of class initialization during replay: "        \
          "0 - don't do anything special; "                                 \
          "1 - treat all class initializers as empty; "                     \
          "2 - treat class initializers for application classes as empty; " \
          "3 - allow all class initializers to run during bootstrap but "   \
          "    pretend they are empty after starting replay")               \
          range(0, 3)                                                       \
                                                                            \
  develop(bool, ReplayIgnoreInitErrors, false,                              \
          "Ignore exceptions thrown during initialization for replay")      \
                                                                            \
  product(bool, DumpReplayDataOnError, true,                                \
          "Record replay data for crashing compiler threads")               \
                                                                            \
  product(bool, CompilerDirectivesIgnoreCompileCommands, false, DIAGNOSTIC, \
             "Disable backwards compatibility for compile commands.")       \
                                                                            \
  product(bool, CompilerDirectivesPrint, false, DIAGNOSTIC,                 \
             "Print compiler directives on installation.")                  \
                                                                            \
  product(int,  CompilerDirectivesLimit, 50, DIAGNOSTIC,                    \
             "Limit on number of compiler directives.")                     \
                                                                            \
  /* Bytecode escape analysis estimation. */                                \
                                                                            \
  product(bool, EstimateArgEscape, true,                                    \
          "Analyze bytecodes to estimate escape state of arguments")        \
                                                                            \
  product(intx, BCEATraceLevel, 0,                                          \
          "How much tracing to do of bytecode escape analysis estimates "   \
          "(0-3)")                                                          \
          range(0, 3)                                                       \
                                                                            \
  product(intx, MaxBCEAEstimateLevel, 5,                                    \
          "Maximum number of nested calls that are analyzed by BC EA")      \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MaxBCEAEstimateSize, 150,                                   \
          "Maximum bytecode size of a method to be analyzed by BC EA")      \
          range(0, max_jint)                                                \
                                                                            \
  /* misc compiler flags */                                                 \
                                                                            \
  product(bool, AbortVMOnCompilationFailure, false, DIAGNOSTIC,             \
          "Abort VM when method had failed to compile.")                    \
                                                                            \
  develop(intx, OSROnlyBCI, -1,                                             \
          "OSR only at this bci.  Negative values mean exclude that bci")   \
                                                                            \
  develop(intx, DesiredMethodLimit,  8000,                                  \
          "The desired maximum method size (in bytecodes) after inlining")  \
                                                                            \
  product(bool, DontCompileHugeMethods, true,                               \
          "Do not compile methods > HugeMethodLimit")                       \
                                                                            \
  develop(intx, HugeMethodLimit,  8000,                                     \
          "Don't compile methods larger than this if "                      \
          "+DontCompileHugeMethods")                                        \
                                                                            \

// end of COMPILER_FLAGS

DECLARE_FLAGS(COMPILER_FLAGS)

#endif // SHARE_COMPILER_COMPILER_GLOBALS_HPP
