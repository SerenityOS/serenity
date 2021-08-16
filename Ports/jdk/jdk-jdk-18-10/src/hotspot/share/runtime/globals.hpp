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

#ifndef SHARE_RUNTIME_GLOBALS_HPP
#define SHARE_RUNTIME_GLOBALS_HPP

#include "compiler/compiler_globals_pd.hpp"
#include "runtime/globals_shared.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include CPU_HEADER(globals)
#include OS_HEADER(globals)
#include OS_CPU_HEADER(globals)

// develop flags are settable / visible only during development and are constant in the PRODUCT version
// product flags are always settable / visible
// notproduct flags are settable / visible only during development and are not declared in the PRODUCT version
// develop_pd/product_pd flags are the same as develop/product, except that their default values
// are specified in platform-dependent header files.

// Flags must be declared with the following number of parameters:
// non-pd flags:
//    (type, name, default_value, doc), or
//    (type, name, default_value, extra_attrs, doc)
// pd flags:
//    (type, name, doc), or
//    (type, name, extra_attrs, doc)

// A flag must be declared with one of the following types:
// bool, int, uint, intx, uintx, size_t, ccstr, ccstrlist, double, or uint64_t.
// The type "ccstr" and "ccstrlist" are an alias for "const char*" and is used
// only in this file, because the macrology requires single-token type names.

// The optional extra_attrs parameter may have one of the following values:
// DIAGNOSTIC, EXPERIMENTAL, or MANAGEABLE. Currently extra_attrs can be used
// only with product/product_pd flags.
//
// DIAGNOSTIC options are not meant for VM tuning or for product modes.
//    They are to be used for VM quality assurance or field diagnosis
//    of VM bugs.  They are hidden so that users will not be encouraged to
//    try them as if they were VM ordinary execution options.  However, they
//    are available in the product version of the VM.  Under instruction
//    from support engineers, VM customers can turn them on to collect
//    diagnostic information about VM problems.  To use a VM diagnostic
//    option, you must first specify +UnlockDiagnosticVMOptions.
//    (This master switch also affects the behavior of -Xprintflags.)
//
// EXPERIMENTAL flags are in support of features that are not
//    part of the officially supported product, but are available
//    for experimenting with. They could, for example, be performance
//    features that may not have undergone full or rigorous QA, but which may
//    help performance in some cases and released for experimentation
//    by the community of users and developers. This flag also allows one to
//    be able to build a fully supported product that nonetheless also
//    ships with some unsupported, lightly tested, experimental features.
//    Like the UnlockDiagnosticVMOptions flag above, there is a corresponding
//    UnlockExperimentalVMOptions flag, which allows the control and
//    modification of the experimental flags.
//
// Nota bene: neither diagnostic nor experimental options should be used casually,
//    and they are not supported on production loads, except under explicit
//    direction from support engineers.
//
// MANAGEABLE flags are writeable external product flags.
//    They are dynamically writeable through the JDK management interface
//    (com.sun.management.HotSpotDiagnosticMXBean API) and also through JConsole.
//    These flags are external exported interface (see CCC).  The list of
//    manageable flags can be queried programmatically through the management
//    interface.
//
//    A flag can be made as "manageable" only if
//    - the flag is defined in a CCC as an external exported interface.
//    - the VM implementation supports dynamic setting of the flag.
//      This implies that the VM must *always* query the flag variable
//      and not reuse state related to the flag state at any given time.
//    - you want the flag to be queried programmatically by the customers.
//

//
// range is a macro that will expand to min and max arguments for range
//    checking code if provided - see jvmFlagLimit.hpp
//
// constraint is a macro that will expand to custom function call
//    for constraint checking if provided - see jvmFlagLimit.hpp

// Default and minimum StringTable and SymbolTable size values
// Must be powers of 2
const size_t defaultStringTableSize = NOT_LP64(1024) LP64_ONLY(65536);
const size_t minimumStringTableSize = 128;
const size_t defaultSymbolTableSize = 32768; // 2^15
const size_t minimumSymbolTableSize = 1024;

#ifdef _LP64
#define LP64_RUNTIME_FLAGS(develop,                                         \
                           develop_pd,                                      \
                           product,                                         \
                           product_pd,                                      \
                           notproduct,                                      \
                           range,                                           \
                           constraint)                                      \
                                                                            \
  product(bool, UseCompressedOops, false,                                   \
          "Use 32-bit object references in 64-bit VM. "                     \
          "lp64_product means flag is always constant in 32 bit VM")        \
                                                                            \
  product(bool, UseCompressedClassPointers, false,                          \
          "Use 32-bit class pointers in 64-bit VM. "                        \
          "lp64_product means flag is always constant in 32 bit VM")        \
                                                                            \
  product(intx, ObjectAlignmentInBytes, 8,                                  \
          "Default object alignment in bytes, 8 is minimum")                \
          range(8, 256)                                                     \
          constraint(ObjectAlignmentInBytesConstraintFunc, AtParse)

#else
// !_LP64

#define LP64_RUNTIME_FLAGS(develop,                                         \
                           develop_pd,                                      \
                           product,                                         \
                           product_pd,                                      \
                           notproduct,                                      \
                           range,                                           \
                           constraint)
const bool UseCompressedOops = false;
const bool UseCompressedClassPointers = false;
const intx ObjectAlignmentInBytes = 8;

#endif // _LP64

#define RUNTIME_FLAGS(develop,                                              \
                      develop_pd,                                           \
                      product,                                              \
                      product_pd,                                           \
                      notproduct,                                           \
                      range,                                                \
                      constraint)                                           \
                                                                            \
  notproduct(bool, CheckCompressedOops, true,                               \
          "Generate checks in encoding/decoding code in debug VM")          \
                                                                            \
  product(uintx, HeapSearchSteps, 3 PPC64_ONLY(+17),                        \
          "Heap allocation steps through preferred address regions to find" \
          " where it can allocate the heap. Number of steps to take per "   \
          "region.")                                                        \
          range(1, max_uintx)                                               \
                                                                            \
  product(uint, HandshakeTimeout, 0, DIAGNOSTIC,                            \
          "If nonzero set a timeout in milliseconds for handshakes")        \
                                                                            \
  product(bool, AlwaysSafeConstructors, false, EXPERIMENTAL,                \
          "Force safe construction, as if all fields are final.")           \
                                                                            \
  product(bool, UnlockDiagnosticVMOptions, trueInDebug, DIAGNOSTIC,         \
          "Enable normal processing of flags relating to field diagnostics")\
                                                                            \
  product(bool, UnlockExperimentalVMOptions, false, EXPERIMENTAL,           \
          "Enable normal processing of flags relating to experimental "     \
          "features")                                                       \
                                                                            \
  product(bool, JavaMonitorsInStackTrace, true,                             \
          "Print information about Java monitor locks when the stacks are " \
          "dumped")                                                         \
                                                                            \
  product_pd(bool, UseLargePages,                                           \
          "Use large page memory")                                          \
                                                                            \
  product_pd(bool, UseLargePagesIndividualAllocation,                       \
          "Allocate large pages individually for better affinity")          \
                                                                            \
  develop(bool, LargePagesIndividualAllocationInjectError, false,           \
          "Fail large pages individual allocation")                         \
                                                                            \
  product(bool, UseNUMA, false,                                             \
          "Use NUMA if available")                                          \
                                                                            \
  product(bool, UseNUMAInterleaving, false,                                 \
          "Interleave memory across NUMA nodes if available")               \
                                                                            \
  product(size_t, NUMAInterleaveGranularity, 2*M,                           \
          "Granularity to use for NUMA interleaving on Windows OS")         \
          constraint(NUMAInterleaveGranularityConstraintFunc, AtParse)      \
                                                                            \
  product(uintx, NUMAChunkResizeWeight, 20,                                 \
          "Percentage (0-100) used to weight the current sample when "      \
          "computing exponentially decaying average for "                   \
          "AdaptiveNUMAChunkSizing")                                        \
          range(0, 100)                                                     \
                                                                            \
  product(size_t, NUMASpaceResizeRate, 1*G,                                 \
          "Do not reallocate more than this amount per collection")         \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, UseAdaptiveNUMAChunkSizing, true,                           \
          "Enable adaptive chunk sizing for NUMA")                          \
                                                                            \
  product(bool, NUMAStats, false,                                           \
          "Print NUMA stats in detailed heap information")                  \
                                                                            \
  product(uintx, NUMAPageScanRate, 256,                                     \
          "Maximum number of pages to include in the page scan procedure")  \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, UseAES, false,                                              \
          "Control whether AES instructions are used when available")       \
                                                                            \
  product(bool, UseFMA, false,                                              \
          "Control whether FMA instructions are used when available")       \
                                                                            \
  product(bool, UseSHA, false,                                              \
          "Control whether SHA instructions are used when available")       \
                                                                            \
  product(bool, UseGHASHIntrinsics, false, DIAGNOSTIC,                      \
          "Use intrinsics for GHASH versions of crypto")                    \
                                                                            \
  product(bool, UseBASE64Intrinsics, false,                                 \
          "Use intrinsics for java.util.Base64")                            \
                                                                            \
  product(size_t, LargePageSizeInBytes, 0,                                  \
          "Maximum large page size used (0 will use the default large "     \
          "page size for the environment as the maximum)")                  \
          range(0, max_uintx)                                               \
                                                                            \
  product(size_t, LargePageHeapSizeThreshold, 128*M,                        \
          "Use large pages if maximum heap is at least this big")           \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, ForceTimeHighResolution, false,                             \
          "Using high time resolution (for Win32 only)")                    \
                                                                            \
  develop(bool, TracePcPatching, false,                                     \
          "Trace usage of frame::patch_pc")                                 \
                                                                            \
  develop(bool, TraceRelocator, false,                                      \
          "Trace the bytecode relocator")                                   \
                                                                            \
                                                                            \
  product(bool, SafepointALot, false, DIAGNOSTIC,                           \
          "Generate a lot of safepoints. This works with "                  \
          "GuaranteedSafepointInterval")                                    \
                                                                            \
  product(bool, HandshakeALot, false, DIAGNOSTIC,                           \
          "Generate a lot of handshakes. This works with "                  \
          "GuaranteedSafepointInterval")                                    \
                                                                            \
  product_pd(bool, BackgroundCompilation,                                   \
          "A thread requesting compilation is not blocked during "          \
          "compilation")                                                    \
                                                                            \
  product(bool, MethodFlushing, true,                                       \
          "Reclamation of zombie and not-entrant methods")                  \
                                                                            \
  develop(bool, VerifyStack, false,                                         \
          "Verify stack of each thread when it is entering a runtime call") \
                                                                            \
  product(bool, ForceUnreachable, false, DIAGNOSTIC,                        \
          "Make all non code cache addresses to be unreachable by "         \
          "forcing use of 64bit literal fixups")                            \
                                                                            \
  develop(bool, TraceDerivedPointers, false,                                \
          "Trace traversal of derived pointers on stack")                   \
                                                                            \
  notproduct(bool, TraceCodeBlobStacks, false,                              \
          "Trace stack-walk of codeblobs")                                  \
                                                                            \
  notproduct(bool, PrintRewrites, false,                                    \
          "Print methods that are being rewritten")                         \
                                                                            \
  product(bool, UseInlineCaches, true,                                      \
          "Use Inline Caches for virtual calls ")                           \
                                                                            \
  product(bool, InlineArrayCopy, true, DIAGNOSTIC,                          \
          "Inline arraycopy native that is known to be part of "            \
          "base library DLL")                                               \
                                                                            \
  product(bool, InlineObjectHash, true, DIAGNOSTIC,                         \
          "Inline Object::hashCode() native that is known to be part "      \
          "of base library DLL")                                            \
                                                                            \
  product(bool, InlineNatives, true, DIAGNOSTIC,                            \
          "Inline natives that are known to be part of base library DLL")   \
                                                                            \
  product(bool, InlineMathNatives, true, DIAGNOSTIC,                        \
          "Inline SinD, CosD, etc.")                                        \
                                                                            \
  product(bool, InlineClassNatives, true, DIAGNOSTIC,                       \
          "Inline Class.isInstance, etc")                                   \
                                                                            \
  product(bool, InlineThreadNatives, true, DIAGNOSTIC,                      \
          "Inline Thread.currentThread, etc")                               \
                                                                            \
  product(bool, InlineUnsafeOps, true, DIAGNOSTIC,                          \
          "Inline memory ops (native methods) from Unsafe")                 \
                                                                            \
  product(bool, CriticalJNINatives, false,                                  \
          "(Deprecated) Check for critical JNI entry points")               \
                                                                            \
  product(bool, UseAESIntrinsics, false, DIAGNOSTIC,                        \
          "Use intrinsics for AES versions of crypto")                      \
                                                                            \
  product(bool, UseAESCTRIntrinsics, false, DIAGNOSTIC,                     \
          "Use intrinsics for the paralleled version of AES/CTR crypto")    \
                                                                            \
  product(bool, UseMD5Intrinsics, false, DIAGNOSTIC,                        \
          "Use intrinsics for MD5 crypto hash function")                    \
                                                                            \
  product(bool, UseSHA1Intrinsics, false, DIAGNOSTIC,                       \
          "Use intrinsics for SHA-1 crypto hash function. "                 \
          "Requires that UseSHA is enabled.")                               \
                                                                            \
  product(bool, UseSHA256Intrinsics, false, DIAGNOSTIC,                     \
          "Use intrinsics for SHA-224 and SHA-256 crypto hash functions. "  \
          "Requires that UseSHA is enabled.")                               \
                                                                            \
  product(bool, UseSHA512Intrinsics, false, DIAGNOSTIC,                     \
          "Use intrinsics for SHA-384 and SHA-512 crypto hash functions. "  \
          "Requires that UseSHA is enabled.")                               \
                                                                            \
  product(bool, UseSHA3Intrinsics, false, DIAGNOSTIC,                       \
          "Use intrinsics for SHA3 crypto hash function. "                  \
          "Requires that UseSHA is enabled.")                               \
                                                                            \
  product(bool, UseCRC32Intrinsics, false, DIAGNOSTIC,                      \
          "use intrinsics for java.util.zip.CRC32")                         \
                                                                            \
  product(bool, UseCRC32CIntrinsics, false, DIAGNOSTIC,                     \
          "use intrinsics for java.util.zip.CRC32C")                        \
                                                                            \
  product(bool, UseAdler32Intrinsics, false, DIAGNOSTIC,                    \
          "use intrinsics for java.util.zip.Adler32")                       \
                                                                            \
  product(bool, UseVectorizedMismatchIntrinsic, false, DIAGNOSTIC,          \
          "Enables intrinsification of ArraysSupport.vectorizedMismatch()") \
                                                                            \
  product(bool, UseCopySignIntrinsic, false, DIAGNOSTIC,                    \
          "Enables intrinsification of Math.copySign")                      \
                                                                            \
  product(bool, UseSignumIntrinsic, false, DIAGNOSTIC,                      \
          "Enables intrinsification of Math.signum")                        \
                                                                            \
  product(ccstrlist, DisableIntrinsic, "", DIAGNOSTIC,                      \
         "do not expand intrinsics whose (internal) names appear here")     \
         constraint(DisableIntrinsicConstraintFunc,AfterErgo)               \
                                                                            \
  product(ccstrlist, ControlIntrinsic, "", DIAGNOSTIC,                      \
         "Control intrinsics using a list of +/- (internal) names, "        \
         "separated by commas")                                             \
         constraint(ControlIntrinsicConstraintFunc,AfterErgo)               \
                                                                            \
  develop(bool, TraceCallFixup, false,                                      \
          "Trace all call fixups")                                          \
                                                                            \
  develop(bool, DeoptimizeALot, false,                                      \
          "Deoptimize at every exit from the runtime system")               \
                                                                            \
  notproduct(ccstrlist, DeoptimizeOnlyAt, "",                               \
          "A comma separated list of bcis to deoptimize at")                \
                                                                            \
  develop(bool, DeoptimizeRandom, false,                                    \
          "Deoptimize random frames on random exit from the runtime system")\
                                                                            \
  notproduct(bool, ZombieALot, false,                                       \
          "Create zombies (non-entrant) at exit from the runtime system")   \
                                                                            \
  notproduct(bool, WalkStackALot, false,                                    \
          "Trace stack (no print) at every exit from the runtime system")   \
                                                                            \
  develop(bool, DeoptimizeObjectsALot, false,                               \
          "For testing purposes concurrent threads revert optimizations "   \
          "based on escape analysis at intervals given with "               \
          "DeoptimizeObjectsALotInterval=n. The thread count is given "     \
          "with DeoptimizeObjectsALotThreadCountSingle and "                \
          "DeoptimizeObjectsALotThreadCountAll.")                           \
                                                                            \
  develop(uint64_t, DeoptimizeObjectsALotInterval, 5,                       \
          "Interval for DeoptimizeObjectsALot.")                            \
          range(0, max_jlong)                                               \
                                                                            \
  develop(int, DeoptimizeObjectsALotThreadCountSingle, 1,                   \
          "The number of threads that revert optimizations based on "       \
          "escape analysis for a single thread if DeoptimizeObjectsALot "   \
          "is enabled. The target thread is selected round robin." )        \
          range(0, max_jint)                                                \
                                                                            \
  develop(int, DeoptimizeObjectsALotThreadCountAll, 1,                      \
          "The number of threads that revert optimizations based on "       \
          "escape analysis for all threads if DeoptimizeObjectsALot "       \
          "is enabled." )                                                   \
          range(0, max_jint)                                                \
                                                                            \
  notproduct(bool, VerifyLastFrame, false,                                  \
          "Verify oops on last frame on entry to VM")                       \
                                                                            \
  product(bool, SafepointTimeout, false,                                    \
          "Time out and warn or fail after SafepointTimeoutDelay "          \
          "milliseconds if failed to reach safepoint")                      \
                                                                            \
  product(bool, AbortVMOnSafepointTimeout, false, DIAGNOSTIC,               \
          "Abort upon failure to reach safepoint (see SafepointTimeout)")   \
                                                                            \
  product(bool, AbortVMOnVMOperationTimeout, false, DIAGNOSTIC,             \
          "Abort upon failure to complete VM operation promptly")           \
                                                                            \
  product(intx, AbortVMOnVMOperationTimeoutDelay, 1000, DIAGNOSTIC,         \
          "Delay in milliseconds for option AbortVMOnVMOperationTimeout")   \
          range(0, max_intx)                                                \
                                                                            \
  product(bool, MaxFDLimit, true,                                           \
          "Bump the number of file descriptors to maximum (Unix only)")     \
                                                                            \
  product(bool, LogEvents, true, DIAGNOSTIC,                                \
          "Enable the various ring buffer event logs")                      \
                                                                            \
  product(uintx, LogEventsBufferEntries, 20, DIAGNOSTIC,                    \
          "Number of ring buffer event logs")                               \
          range(1, NOT_LP64(1*K) LP64_ONLY(1*M))                            \
                                                                            \
  product(bool, BytecodeVerificationRemote, true, DIAGNOSTIC,               \
          "Enable the Java bytecode verifier for remote classes")           \
                                                                            \
  product(bool, BytecodeVerificationLocal, false, DIAGNOSTIC,               \
          "Enable the Java bytecode verifier for local classes")            \
                                                                            \
  develop(bool, VerifyStackAtCalls, false,                                  \
          "Verify that the stack pointer is unchanged after calls")         \
                                                                            \
  develop(bool, TraceJavaAssertions, false,                                 \
          "Trace java language assertions")                                 \
                                                                            \
  notproduct(bool, VerifyCodeCache, false,                                  \
          "Verify code cache on memory allocation/deallocation")            \
                                                                            \
  develop(bool, UseMallocOnly, false,                                       \
          "Use only malloc/free for allocation (no resource area/arena). "  \
          "Used to help diagnose memory stomping bugs.")                    \
                                                                            \
  develop(bool, ZapResourceArea, trueInDebug,                               \
          "Zap freed resource/arena space with 0xABABABAB")                 \
                                                                            \
  notproduct(bool, ZapVMHandleArea, trueInDebug,                            \
          "Zap freed VM handle space with 0xBCBCBCBC")                      \
                                                                            \
  notproduct(bool, ZapStackSegments, trueInDebug,                           \
          "Zap allocated/freed stack segments with 0xFADFADED")             \
                                                                            \
  develop(bool, ZapUnusedHeapArea, trueInDebug,                             \
          "Zap unused heap space with 0xBAADBABE")                          \
                                                                            \
  develop(bool, CheckZapUnusedHeapArea, false,                              \
          "Check zapping of unused heap space")                             \
                                                                            \
  develop(bool, ZapFillerObjects, trueInDebug,                              \
          "Zap filler objects with 0xDEAFBABE")                             \
                                                                            \
  product(bool, ExecutingUnitTests, false,                                  \
          "Whether the JVM is running unit tests or not")                   \
                                                                            \
  develop(uintx, ErrorHandlerTest, 0,                                       \
          "If > 0, provokes an error after VM initialization; the value "   \
          "determines which error to provoke. See controlled_crash() "      \
          "in vmError.cpp.")                                                \
          range(0, 17)                                                      \
                                                                            \
  develop(uintx, TestCrashInErrorHandler, 0,                                \
          "If > 0, provokes an error inside VM error handler (a secondary " \
          "crash). see controlled_crash() in vmError.cpp")                  \
          range(0, 17)                                                      \
                                                                            \
  develop(bool, TestSafeFetchInErrorHandler, false   ,                      \
          "If true, tests SafeFetch inside error handler.")                 \
                                                                            \
  develop(bool, TestUnresponsiveErrorHandler, false,                        \
          "If true, simulates an unresponsive error handler.")              \
                                                                            \
  develop(bool, Verbose, false,                                             \
          "Print additional debugging information from other modes")        \
                                                                            \
  develop(bool, PrintMiscellaneous, false,                                  \
          "Print uncategorized debugging information (requires +Verbose)")  \
                                                                            \
  develop(bool, WizardMode, false,                                          \
          "Print much more debugging information")                          \
                                                                            \
  product(bool, ShowMessageBoxOnError, false,                               \
          "Keep process alive on VM fatal error")                           \
                                                                            \
  product(bool, CreateCoredumpOnCrash, true,                                \
          "Create core/mini dump on VM fatal error")                        \
                                                                            \
  product(uint64_t, ErrorLogTimeout, 2 * 60,                                \
          "Timeout, in seconds, to limit the time spent on writing an "     \
          "error log in case of a crash.")                                  \
          range(0, (uint64_t)max_jlong/1000)                                \
                                                                            \
  product(bool, SuppressFatalErrorMessage, false,                           \
          "Report NO fatal error message (avoid deadlock)")                 \
                                                                            \
  product(ccstrlist, OnError, "",                                           \
          "Run user-defined commands on fatal error; see VMError.cpp "      \
          "for examples")                                                   \
                                                                            \
  product(ccstrlist, OnOutOfMemoryError, "",                                \
          "Run user-defined commands on first java.lang.OutOfMemoryError "  \
          "thrown from JVM")                                                \
                                                                            \
  product(bool, HeapDumpBeforeFullGC, false, MANAGEABLE,                    \
          "Dump heap to file before any major stop-the-world GC")           \
                                                                            \
  product(bool, HeapDumpAfterFullGC, false, MANAGEABLE,                     \
          "Dump heap to file after any major stop-the-world GC")            \
                                                                            \
  product(bool, HeapDumpOnOutOfMemoryError, false, MANAGEABLE,              \
          "Dump heap to file when java.lang.OutOfMemoryError is thrown "    \
          "from JVM")                                                       \
                                                                            \
  product(ccstr, HeapDumpPath, NULL, MANAGEABLE,                            \
          "When HeapDumpOnOutOfMemoryError is on, the path (filename or "   \
          "directory) of the dump file (defaults to java_pid<pid>.hprof "   \
          "in the working directory)")                                      \
                                                                            \
  product(intx, HeapDumpGzipLevel, 0, MANAGEABLE,                           \
          "When HeapDumpOnOutOfMemoryError is on, the gzip compression "    \
          "level of the dump file. 0 (the default) disables gzip "          \
          "compression. Otherwise the level must be between 1 and 9.")      \
          range(0, 9)                                                       \
                                                                            \
  product(ccstr, NativeMemoryTracking, "off",                               \
          "Native memory tracking options")                                 \
                                                                            \
  product(bool, PrintNMTStatistics, false, DIAGNOSTIC,                      \
          "Print native memory tracking summary data if it is on")          \
                                                                            \
  product(bool, LogCompilation, false, DIAGNOSTIC,                          \
          "Log compilation activity in detail to LogFile")                  \
                                                                            \
  product(bool, PrintCompilation, false,                                    \
          "Print compilations")                                             \
                                                                            \
  product(intx, RepeatCompilation, 0, DIAGNOSTIC,                           \
          "Repeat compilation without installing code (number of times)")   \
          range(0, max_jint)                                                \
                                                                            \
  product(bool, PrintExtendedThreadInfo, false,                             \
          "Print more information in thread dump")                          \
                                                                            \
  product(intx, ScavengeRootsInCode, 2, DIAGNOSTIC,                         \
          "0: do not allow scavengable oops in the code cache; "            \
          "1: allow scavenging from the code cache; "                       \
          "2: emit as many constants as the compiler can see")              \
          range(0, 2)                                                       \
                                                                            \
  product(bool, AlwaysRestoreFPU, false,                                    \
          "Restore the FPU control word after every JNI call (expensive)")  \
                                                                            \
  product(bool, PrintCompilation2, false, DIAGNOSTIC,                       \
          "Print additional statistics per compilation")                    \
                                                                            \
  product(bool, PrintAdapterHandlers, false, DIAGNOSTIC,                    \
          "Print code generated for i2c/c2i adapters")                      \
                                                                            \
  product(bool, VerifyAdapterCalls, trueInDebug, DIAGNOSTIC,                \
          "Verify that i2c/c2i adapters are called properly")               \
                                                                            \
  develop(bool, VerifyAdapterSharing, false,                                \
          "Verify that the code for shared adapters is the equivalent")     \
                                                                            \
  product(bool, PrintAssembly, false, DIAGNOSTIC,                           \
          "Print assembly code (using external disassembler.so)")           \
                                                                            \
  product(ccstr, PrintAssemblyOptions, NULL, DIAGNOSTIC,                    \
          "Print options string passed to disassembler.so")                 \
                                                                            \
  notproduct(bool, PrintNMethodStatistics, false,                           \
          "Print a summary statistic for the generated nmethods")           \
                                                                            \
  product(bool, PrintNMethods, false, DIAGNOSTIC,                           \
          "Print assembly code for nmethods when generated")                \
                                                                            \
  product(bool, PrintNativeNMethods, false, DIAGNOSTIC,                     \
          "Print assembly code for native nmethods when generated")         \
                                                                            \
  develop(bool, PrintDebugInfo, false,                                      \
          "Print debug information for all nmethods when generated")        \
                                                                            \
  develop(bool, PrintRelocations, false,                                    \
          "Print relocation information for all nmethods when generated")   \
                                                                            \
  develop(bool, PrintDependencies, false,                                   \
          "Print dependency information for all nmethods when generated")   \
                                                                            \
  develop(bool, PrintExceptionHandlers, false,                              \
          "Print exception handler tables for all nmethods when generated") \
                                                                            \
  develop(bool, StressCompiledExceptionHandlers, false,                     \
          "Exercise compiled exception handlers")                           \
                                                                            \
  develop(bool, InterceptOSException, false,                                \
          "Start debugger when an implicit OS (e.g. NULL) "                 \
          "exception happens")                                              \
                                                                            \
  product(bool, PrintCodeCache, false,                                      \
          "Print the code cache memory usage when exiting")                 \
                                                                            \
  develop(bool, PrintCodeCache2, false,                                     \
          "Print detailed usage information on the code cache when exiting")\
                                                                            \
  product(bool, PrintCodeCacheOnCompilation, false,                         \
          "Print the code cache memory usage each time a method is "        \
          "compiled")                                                       \
                                                                            \
  product(bool, PrintCodeHeapAnalytics, false, DIAGNOSTIC,                  \
          "Print code heap usage statistics on exit and on full condition") \
                                                                            \
  product(bool, PrintStubCode, false, DIAGNOSTIC,                           \
          "Print generated stub code")                                      \
                                                                            \
  product(bool, StackTraceInThrowable, true,                                \
          "Collect backtrace in throwable when exception happens")          \
                                                                            \
  product(bool, OmitStackTraceInFastThrow, true,                            \
          "Omit backtraces for some 'hot' exceptions in optimized code")    \
                                                                            \
  product(bool, ShowCodeDetailsInExceptionMessages, true, MANAGEABLE,       \
          "Show exception messages from RuntimeExceptions that contain "    \
          "snippets of the failing code. Disable this to improve privacy.") \
                                                                            \
  product(bool, PrintWarnings, true,                                        \
          "Print JVM warnings to output stream")                            \
                                                                            \
  product(bool, RegisterFinalizersAtInit, true,                             \
          "Register finalizable objects at end of Object.<init> or "        \
          "after allocation")                                               \
                                                                            \
  develop(bool, RegisterReferences, true,                                   \
          "Tell whether the VM should register soft/weak/final/phantom "    \
          "references")                                                     \
                                                                            \
  develop(bool, PrintCodeCacheExtension, false,                             \
          "Print extension of code cache")                                  \
                                                                            \
  develop(bool, UsePrivilegedStack, true,                                   \
          "Enable the security JVM functions")                              \
                                                                            \
  product(bool, ClassUnloading, true,                                       \
          "Do unloading of classes")                                        \
                                                                            \
  product(bool, ClassUnloadingWithConcurrentMark, true,                     \
          "Do unloading of classes with a concurrent marking cycle")        \
                                                                            \
  notproduct(bool, PrintSystemDictionaryAtExit, false,                      \
          "Print the system dictionary at exit")                            \
                                                                            \
  notproduct(bool, PrintClassLoaderDataGraphAtExit, false,                  \
          "Print the class loader data graph at exit")                      \
                                                                            \
  product(bool, DynamicallyResizeSystemDictionaries, true, DIAGNOSTIC,      \
          "Dynamically resize system dictionaries as needed")               \
                                                                            \
  product(bool, AllowParallelDefineClass, false,                            \
          "Allow parallel defineClass requests for class loaders "          \
          "registering as parallel capable")                                \
                                                                            \
  product_pd(bool, DontYieldALot,                                           \
          "Throw away obvious excess yield calls")                          \
                                                                            \
  product(bool, DisablePrimordialThreadGuardPages, false, EXPERIMENTAL,     \
               "Disable the use of stack guard pages if the JVM is loaded " \
               "on the primordial process thread")                          \
                                                                            \
  /* notice: the max range value here is max_jint, not max_intx  */         \
  /* because of overflow issue                                   */         \
  product(intx, AsyncDeflationInterval, 250, DIAGNOSTIC,                    \
          "Async deflate idle monitors every so many milliseconds when "    \
          "MonitorUsedDeflationThreshold is exceeded (0 is off).")          \
          range(0, max_jint)                                                \
                                                                            \
  product(size_t, AvgMonitorsPerThreadEstimate, 1024, DIAGNOSTIC,           \
          "Used to estimate a variable ceiling based on number of threads " \
          "for use with MonitorUsedDeflationThreshold (0 is off).")         \
          range(0, max_uintx)                                               \
                                                                            \
  /* notice: the max range value here is max_jint, not max_intx  */         \
  /* because of overflow issue                                   */         \
  product(intx, MonitorDeflationMax, 1000000, DIAGNOSTIC,                   \
          "The maximum number of monitors to deflate, unlink and delete "   \
          "at one time (minimum is 1024).")                      \
          range(1024, max_jint)                                             \
                                                                            \
  product(intx, MonitorUsedDeflationThreshold, 90, DIAGNOSTIC,              \
          "Percentage of used monitors before triggering deflation (0 is "  \
          "off). The check is performed on GuaranteedSafepointInterval "    \
          "or AsyncDeflationInterval.")                                     \
          range(0, 100)                                                     \
                                                                            \
  product(uintx, NoAsyncDeflationProgressMax, 3, DIAGNOSTIC,                \
          "Max number of no progress async deflation attempts to tolerate " \
          "before adjusting the in_use_list_ceiling up (0 is off).")        \
          range(0, max_uintx)                                               \
                                                                            \
  product(intx, hashCode, 5, EXPERIMENTAL,                                  \
               "(Unstable) select hashCode generation algorithm")           \
                                                                            \
  product(bool, FilterSpuriousWakeups, true,                                \
          "(Deprecated) When true prevents OS-level spurious, or premature,"\
          " wakeups from Object.wait (Ignored for Windows)")                \
                                                                            \
  product(bool, ReduceSignalUsage, false,                                   \
          "Reduce the use of OS signals in Java and/or the VM")             \
                                                                            \
  develop(bool, LoadLineNumberTables, true,                                 \
          "Tell whether the class file parser loads line number tables")    \
                                                                            \
  develop(bool, LoadLocalVariableTables, true,                              \
          "Tell whether the class file parser loads local variable tables") \
                                                                            \
  develop(bool, LoadLocalVariableTypeTables, true,                          \
          "Tell whether the class file parser loads local variable type"    \
          "tables")                                                         \
                                                                            \
  product(bool, AllowUserSignalHandlers, false,                             \
          "Application will install primary signal handlers for the JVM "   \
          "(Unix only)")                                                    \
                                                                            \
  product(bool, UseSignalChaining, true,                                    \
          "Use signal-chaining to invoke signal handlers installed "        \
          "by the application (Unix only)")                                 \
                                                                            \
  product(bool, RestoreMXCSROnJNICalls, false,                              \
          "Restore MXCSR when returning from JNI calls")                    \
                                                                            \
  product(bool, CheckJNICalls, false,                                       \
          "Verify all arguments to JNI calls")                              \
                                                                            \
  product(bool, UseFastJNIAccessors, true,                                  \
          "Use optimized versions of Get<Primitive>Field")                  \
                                                                            \
  product(intx, MaxJNILocalCapacity, 65536,                                 \
          "Maximum allowable local JNI handle capacity to "                 \
          "EnsureLocalCapacity() and PushLocalFrame(), "                    \
          "where <= 0 is unlimited, default: 65536")                        \
          range(min_intx, max_intx)                                         \
                                                                            \
  product(bool, EagerXrunInit, false,                                       \
          "Eagerly initialize -Xrun libraries; allows startup profiling, "  \
          "but not all -Xrun libraries may support the state of the VM "    \
          "at this time")                                                   \
                                                                            \
  product(bool, PreserveAllAnnotations, false,                              \
          "Preserve RuntimeInvisibleAnnotations as well "                   \
          "as RuntimeVisibleAnnotations")                                   \
                                                                            \
  develop(uintx, PreallocatedOutOfMemoryErrorCount, 4,                      \
          "Number of OutOfMemoryErrors preallocated with backtrace")        \
                                                                            \
  product(bool, UseXMMForArrayCopy, false,                                  \
          "Use SSE2 MOVQ instruction for Arraycopy")                        \
                                                                            \
  notproduct(bool, PrintFieldLayout, false,                                 \
          "Print field layout for each class")                              \
                                                                            \
  /* Need to limit the extent of the padding to reasonable size.          */\
  /* 8K is well beyond the reasonable HW cache line size, even with       */\
  /* aggressive prefetching, while still leaving the room for segregating */\
  /* among the distinct pages.                                            */\
  product(intx, ContendedPaddingWidth, 128,                                 \
          "How many bytes to pad the fields/classes marked @Contended with")\
          range(0, 8192)                                                    \
          constraint(ContendedPaddingWidthConstraintFunc,AfterErgo)         \
                                                                            \
  product(bool, EnableContended, true,                                      \
          "Enable @Contended annotation support")                           \
                                                                            \
  product(bool, RestrictContended, true,                                    \
          "Restrict @Contended to trusted classes")                         \
                                                                            \
  product(intx, DiagnoseSyncOnValueBasedClasses, 0, DIAGNOSTIC,             \
             "Detect and take action upon identifying synchronization on "  \
             "value based classes. Modes: "                                 \
             "0: off; "                                                     \
             "1: exit with fatal error; "                                   \
             "2: log message to stdout. Output file can be specified with " \
             "   -Xlog:valuebasedclasses. If JFR is running it will "       \
             "   also generate JFR events.")                                \
             range(0, 2)                                                    \
                                                                            \
  product(bool, ExitOnOutOfMemoryError, false,                              \
          "JVM exits on the first occurrence of an out-of-memory error "    \
          "thrown from JVM")                                                \
                                                                            \
  product(bool, CrashOnOutOfMemoryError, false,                             \
          "JVM aborts, producing an error log and core/mini dump, on the "  \
          "first occurrence of an out-of-memory error thrown from JVM")     \
                                                                            \
  /* tracing */                                                             \
                                                                            \
  develop(bool, StressRewriter, false,                                      \
          "Stress linktime bytecode rewriting")                             \
                                                                            \
  product(ccstr, TraceJVMTI, NULL,                                          \
          "Trace flags for JVMTI functions and events")                     \
                                                                            \
  product(bool, StressLdcRewrite, false, DIAGNOSTIC,                        \
          "Force ldc -> ldc_w rewrite during RedefineClasses. "             \
          "This option can change an EMCP method into an obsolete method "  \
          "and can affect tests that expect specific methods to be EMCP. "  \
          "This option should be used with caution.")                       \
                                                                            \
  product(bool, AllowRedefinitionToAddDeleteMethods, false,                 \
          "(Deprecated) Allow redefinition to add and delete private "      \
          "static or final methods for compatibility with old releases")    \
                                                                            \
  develop(bool, TraceBytecodes, false,                                      \
          "Trace bytecode execution")                                       \
                                                                            \
  develop(bool, TraceICs, false,                                            \
          "Trace inline cache changes")                                     \
                                                                            \
  notproduct(bool, TraceInvocationCounterOverflow, false,                   \
          "Trace method invocation counter overflow")                       \
                                                                            \
  develop(bool, TraceInlineCacheClearing, false,                            \
          "Trace clearing of inline caches in nmethods")                    \
                                                                            \
  develop(bool, TraceDependencies, false,                                   \
          "Trace dependencies")                                             \
                                                                            \
  develop(bool, VerifyDependencies, trueInDebug,                            \
          "Exercise and verify the compilation dependency mechanism")       \
                                                                            \
  develop(bool, TraceNewOopMapGeneration, false,                            \
          "Trace OopMapGeneration")                                         \
                                                                            \
  develop(bool, TraceNewOopMapGenerationDetailed, false,                    \
          "Trace OopMapGeneration: print detailed cell states")             \
                                                                            \
  develop(bool, TimeOopMap, false,                                          \
          "Time calls to GenerateOopMap::compute_map() in sum")             \
                                                                            \
  develop(bool, TimeOopMap2, false,                                         \
          "Time calls to GenerateOopMap::compute_map() individually")       \
                                                                            \
  develop(bool, TraceOopMapRewrites, false,                                 \
          "Trace rewriting of methods during oop map generation")           \
                                                                            \
  develop(bool, TraceICBuffer, false,                                       \
          "Trace usage of IC buffer")                                       \
                                                                            \
  develop(bool, TraceCompiledIC, false,                                     \
          "Trace changes of compiled IC")                                   \
                                                                            \
  develop(bool, FLSVerifyDictionary, false,                                 \
          "Do lots of (expensive) FLS dictionary verification")             \
                                                                            \
                                                                            \
  notproduct(bool, CheckMemoryInitialization, false,                        \
          "Check memory initialization")                                    \
                                                                            \
  product(uintx, ProcessDistributionStride, 4,                              \
          "Stride through processors when distributing processes")          \
          range(0, max_juint)                                               \
                                                                            \
  develop(bool, TraceFinalizerRegistration, false,                          \
          "Trace registration of final references")                         \
                                                                            \
  product(bool, IgnoreEmptyClassPaths, false,                               \
          "Ignore empty path elements in -classpath")                       \
                                                                            \
  product(bool, PrintHeapAtSIGBREAK, true,                                  \
          "Print heap layout in response to SIGBREAK")                      \
                                                                            \
  product(bool, PrintClassHistogram, false, MANAGEABLE,                     \
          "Print a histogram of class instances")                           \
                                                                            \
  product(double, ObjectCountCutOffPercent, 0.5, EXPERIMENTAL,              \
          "The percentage of the used heap that the instances of a class "  \
          "must occupy for the class to generate a trace event")            \
          range(0.0, 100.0)                                                 \
                                                                            \
  /* JVMTI heap profiling */                                                \
                                                                            \
  product(bool, VerifyBeforeIteration, false, DIAGNOSTIC,                   \
          "Verify memory system before JVMTI iteration")                    \
                                                                            \
  /* compiler */                                                            \
                                                                            \
  /* notice: the max range value here is max_jint, not max_intx  */         \
  /* because of overflow issue                                   */         \
  product(intx, CICompilerCount, CI_COMPILER_COUNT,                         \
          "Number of compiler threads to run")                              \
          range(0, max_jint)                                                \
          constraint(CICompilerCountConstraintFunc, AfterErgo)              \
                                                                            \
  product(bool, UseDynamicNumberOfCompilerThreads, true,                    \
          "Dynamically choose the number of parallel compiler threads")     \
                                                                            \
  product(bool, ReduceNumberOfCompilerThreads, true, DIAGNOSTIC,            \
             "Reduce the number of parallel compiler threads when they "    \
             "are not used")                                                \
                                                                            \
  product(bool, TraceCompilerThreads, false, DIAGNOSTIC,                    \
             "Trace creation and removal of compiler threads")              \
                                                                            \
  develop(bool, InjectCompilerCreationFailure, false,                       \
          "Inject thread creation failures for "                            \
          "UseDynamicNumberOfCompilerThreads")                              \
                                                                            \
  develop(bool, GenerateSynchronizationCode, true,                          \
          "generate locking/unlocking code for synchronized methods and "   \
          "monitors")                                                       \
                                                                            \
  develop(bool, GenerateRangeChecks, true,                                  \
          "Generate range checks for array accesses")                       \
                                                                            \
  product_pd(bool, ImplicitNullChecks, DIAGNOSTIC,                          \
          "Generate code for implicit null checks")                         \
                                                                            \
  product_pd(bool, TrapBasedNullChecks,                                     \
          "Generate code for null checks that uses a cmp and trap "         \
          "instruction raising SIGTRAP.  This is only used if an access to" \
          "null (+offset) will not raise a SIGSEGV, i.e.,"                  \
          "ImplicitNullChecks don't work (PPC64).")                         \
                                                                            \
  product(bool, EnableThreadSMRExtraValidityChecks, true, DIAGNOSTIC,       \
             "Enable Thread SMR extra validity checks")                     \
                                                                            \
  product(bool, EnableThreadSMRStatistics, trueInDebug, DIAGNOSTIC,         \
             "Enable Thread SMR Statistics")                                \
                                                                            \
  product(bool, UseNotificationThread, true,                                \
          "Use Notification Thread")                                        \
                                                                            \
  product(bool, Inline, true,                                               \
          "Enable inlining")                                                \
                                                                            \
  product(bool, ClipInlining, true,                                         \
          "Clip inlining if aggregate method exceeds DesiredMethodLimit")   \
                                                                            \
  develop(bool, UseCHA, true,                                               \
          "Enable CHA")                                                     \
                                                                            \
  product(bool, UseVtableBasedCHA, true,  DIAGNOSTIC,                       \
          "Use vtable information during CHA")                              \
                                                                            \
  product(bool, UseTypeProfile, true,                                       \
          "Check interpreter profile for historically monomorphic calls")   \
                                                                            \
  product(bool, PrintInlining, false, DIAGNOSTIC,                           \
          "Print inlining optimizations")                                   \
                                                                            \
  product(bool, UsePopCountInstruction, false,                              \
          "Use population count instruction")                               \
                                                                            \
  develop(bool, EagerInitialization, false,                                 \
          "Eagerly initialize classes if possible")                         \
                                                                            \
  product(bool, LogTouchedMethods, false, DIAGNOSTIC,                       \
          "Log methods which have been ever touched in runtime")            \
                                                                            \
  product(bool, PrintTouchedMethodsAtExit, false, DIAGNOSTIC,               \
          "Print all methods that have been ever touched in runtime")       \
                                                                            \
  develop(bool, TraceMethodReplacement, false,                              \
          "Print when methods are replaced do to recompilation")            \
                                                                            \
  develop(bool, PrintMethodFlushing, false,                                 \
          "Print the nmethods being flushed")                               \
                                                                            \
  product(bool, PrintMethodFlushingStatistics, false, DIAGNOSTIC,           \
          "print statistics about method flushing")                         \
                                                                            \
  product(intx, HotMethodDetectionLimit, 100000, DIAGNOSTIC,                \
          "Number of compiled code invocations after which "                \
          "the method is considered as hot by the flusher")                 \
          range(1, max_jint)                                                \
                                                                            \
  product(intx, MinPassesBeforeFlush, 10, DIAGNOSTIC,                       \
          "Minimum number of sweeper passes before an nmethod "             \
          "can be flushed")                                                 \
          range(0, max_intx)                                                \
                                                                            \
  product(bool, UseCodeAging, true,                                         \
          "Insert counter to detect warm methods")                          \
                                                                            \
  product(bool, StressCodeAging, false, DIAGNOSTIC,                         \
          "Start with counters compiled in")                                \
                                                                            \
  develop(bool, StressCodeBuffers, false,                                   \
          "Exercise code buffer expansion and other rare state changes")    \
                                                                            \
  product(bool, DebugNonSafepoints, trueInDebug, DIAGNOSTIC,                \
          "Generate extra debugging information for non-safepoints in "     \
          "nmethods")                                                       \
                                                                            \
  product(bool, PrintVMOptions, false,                                      \
          "Print flags that appeared on the command line")                  \
                                                                            \
  product(bool, IgnoreUnrecognizedVMOptions, false,                         \
          "Ignore unrecognized VM options")                                 \
                                                                            \
  product(bool, PrintCommandLineFlags, false,                               \
          "Print flags specified on command line or set by ergonomics")     \
                                                                            \
  product(bool, PrintFlagsInitial, false,                                   \
          "Print all VM flags before argument processing and exit VM")      \
                                                                            \
  product(bool, PrintFlagsFinal, false,                                     \
          "Print all VM flags after argument and ergonomic processing")     \
                                                                            \
  notproduct(bool, PrintFlagsWithComments, false,                           \
          "Print all VM flags with default values and descriptions and "    \
          "exit")                                                           \
                                                                            \
  product(bool, PrintFlagsRanges, false,                                    \
          "Print VM flags and their ranges")                                \
                                                                            \
  product(bool, SerializeVMOutput, true, DIAGNOSTIC,                        \
          "Use a mutex to serialize output to tty and LogFile")             \
                                                                            \
  product(bool, DisplayVMOutput, true, DIAGNOSTIC,                          \
          "Display all VM output on the tty, independently of LogVMOutput") \
                                                                            \
  product(bool, LogVMOutput, false, DIAGNOSTIC,                             \
          "Save VM output to LogFile")                                      \
                                                                            \
  product(ccstr, LogFile, NULL, DIAGNOSTIC,                                 \
          "If LogVMOutput or LogCompilation is on, save VM output to "      \
          "this file [default: ./hotspot_pid%p.log] (%p replaced with pid)")\
                                                                            \
  product(ccstr, ErrorFile, NULL,                                           \
          "If an error occurs, save the error data to this file "           \
          "[default: ./hs_err_pid%p.log] (%p replaced with pid)")           \
                                                                            \
  product(bool, ExtensiveErrorReports,                                      \
          PRODUCT_ONLY(false) NOT_PRODUCT(true),                            \
          "Error reports are more extensive.")                              \
                                                                            \
  product(bool, DisplayVMOutputToStderr, false,                             \
          "If DisplayVMOutput is true, display all VM output to stderr")    \
                                                                            \
  product(bool, DisplayVMOutputToStdout, false,                             \
          "If DisplayVMOutput is true, display all VM output to stdout")    \
                                                                            \
  product(bool, ErrorFileToStderr, false,                                   \
          "If true, error data is printed to stderr instead of a file")     \
                                                                            \
  product(bool, ErrorFileToStdout, false,                                   \
          "If true, error data is printed to stdout instead of a file")     \
                                                                            \
  product(bool, UseHeavyMonitors, false,                                    \
          "use heavyweight instead of lightweight Java monitors")           \
                                                                            \
  product(bool, PrintStringTableStatistics, false,                          \
          "print statistics about the StringTable and SymbolTable")         \
                                                                            \
  product(bool, VerifyStringTableAtExit, false, DIAGNOSTIC,                 \
          "verify StringTable contents at exit")                            \
                                                                            \
  notproduct(bool, PrintSymbolTableSizeHistogram, false,                    \
          "print histogram of the symbol table")                            \
                                                                            \
  product(ccstr, AbortVMOnException, NULL, DIAGNOSTIC,                      \
          "Call fatal if this exception is thrown.  Example: "              \
          "java -XX:AbortVMOnException=java.lang.NullPointerException Foo") \
                                                                            \
  product(ccstr, AbortVMOnExceptionMessage, NULL, DIAGNOSTIC,               \
          "Call fatal if the exception pointed by AbortVMOnException "      \
          "has this message")                                               \
                                                                            \
  develop(bool, DebugVtables, false,                                        \
          "add debugging code to vtable dispatch")                          \
                                                                            \
  notproduct(bool, PrintVtableStats, false,                                 \
          "print vtables stats at end of run")                              \
                                                                            \
  develop(bool, TraceCreateZombies, false,                                  \
          "trace creation of zombie nmethods")                              \
                                                                            \
  product(bool, RangeCheckElimination, true,                                \
          "Eliminate range checks")                                         \
                                                                            \
  develop_pd(bool, UncommonNullCast,                                        \
          "track occurrences of null in casts; adjust compiler tactics")    \
                                                                            \
  develop(bool, TypeProfileCasts,  true,                                    \
          "treat casts like calls for purposes of type profiling")          \
                                                                            \
  develop(bool, TraceLivenessGen, false,                                    \
          "Trace the generation of liveness analysis information")          \
                                                                            \
  notproduct(bool, TraceLivenessQuery, false,                               \
          "Trace queries of liveness analysis information")                 \
                                                                            \
  notproduct(bool, CollectIndexSetStatistics, false,                        \
          "Collect information about IndexSets")                            \
                                                                            \
  develop(intx, FastAllocateSizeLimit, 128*K,                               \
          /* Note:  This value is zero mod 1<<13 for a cheap sparc set. */  \
          "Inline allocations larger than this in doublewords must go slow")\
                                                                            \
  product_pd(bool, CompactStrings,                                          \
          "Enable Strings to use single byte chars in backing store")       \
                                                                            \
  product_pd(uintx, TypeProfileLevel,                                       \
          "=XYZ, with Z: Type profiling of arguments at call; "             \
                     "Y: Type profiling of return value at call; "          \
                     "X: Type profiling of parameters to methods; "         \
          "X, Y and Z in 0=off ; 1=jsr292 only; 2=all methods")             \
          constraint(TypeProfileLevelConstraintFunc, AfterErgo)             \
                                                                            \
  product(intx, TypeProfileArgsLimit,     2,                                \
          "max number of call arguments to consider for type profiling")    \
          range(0, 16)                                                      \
                                                                            \
  product(intx, TypeProfileParmsLimit,    2,                                \
          "max number of incoming parameters to consider for type profiling"\
          ", -1 for all")                                                   \
          range(-1, 64)                                                     \
                                                                            \
  /* statistics */                                                          \
  develop(bool, CountCompiledCalls, false,                                  \
          "Count method invocations")                                       \
                                                                            \
  notproduct(bool, ICMissHistogram, false,                                  \
          "Produce histogram of IC misses")                                 \
                                                                            \
  /* interpreter */                                                         \
  product_pd(bool, RewriteBytecodes,                                        \
          "Allow rewriting of bytecodes (bytecodes are not immutable)")     \
                                                                            \
  product_pd(bool, RewriteFrequentPairs,                                    \
          "Rewrite frequently used bytecode pairs into a single bytecode")  \
                                                                            \
  product(bool, PrintInterpreter, false, DIAGNOSTIC,                        \
          "Print the generated interpreter code")                           \
                                                                            \
  product(bool, UseInterpreter, true,                                       \
          "Use interpreter for non-compiled methods")                       \
                                                                            \
  develop(bool, UseFastSignatureHandlers, true,                             \
          "Use fast signature handlers for native calls")                   \
                                                                            \
  product(bool, UseLoopCounter, true,                                       \
          "Increment invocation counter on backward branch")                \
                                                                            \
  product_pd(bool, UseOnStackReplacement,                                   \
          "Use on stack replacement, calls runtime if invoc. counter "      \
          "overflows in loop")                                              \
                                                                            \
  notproduct(bool, TraceOnStackReplacement, false,                          \
          "Trace on stack replacement")                                     \
                                                                            \
  product_pd(bool, PreferInterpreterNativeStubs,                            \
          "Use always interpreter stubs for native methods invoked via "    \
          "interpreter")                                                    \
                                                                            \
  develop(bool, CountBytecodes, false,                                      \
          "Count number of bytecodes executed")                             \
                                                                            \
  develop(bool, PrintBytecodeHistogram, false,                              \
          "Print histogram of the executed bytecodes")                      \
                                                                            \
  develop(bool, PrintBytecodePairHistogram, false,                          \
          "Print histogram of the executed bytecode pairs")                 \
                                                                            \
  product(bool, PrintSignatureHandlers, false, DIAGNOSTIC,                  \
          "Print code generated for native method signature handlers")      \
                                                                            \
  develop(bool, VerifyOops, false,                                          \
          "Do plausibility checks for oops")                                \
                                                                            \
  develop(bool, CheckUnhandledOops, false,                                  \
          "Check for unhandled oops in VM code")                            \
                                                                            \
  develop(bool, VerifyJNIFields, trueInDebug,                               \
          "Verify jfieldIDs for instance fields")                           \
                                                                            \
  notproduct(bool, VerifyJNIEnvThread, false,                               \
          "Verify JNIEnv.thread == Thread::current() when entering VM "     \
          "from JNI")                                                       \
                                                                            \
  develop(bool, VerifyFPU, false,                                           \
          "Verify FPU state (check for NaN's, etc.)")                       \
                                                                            \
  develop(bool, VerifyThread, false,                                        \
          "Watch the thread register for corruption (SPARC only)")          \
                                                                            \
  develop(bool, VerifyActivationFrameSize, false,                           \
          "Verify that activation frame didn't become smaller than its "    \
          "minimal size")                                                   \
                                                                            \
  develop(bool, TraceFrequencyInlining, false,                              \
          "Trace frequency based inlining")                                 \
                                                                            \
  develop_pd(bool, InlineIntrinsics,                                        \
          "Inline intrinsics that can be statically resolved")              \
                                                                            \
  product_pd(bool, ProfileInterpreter,                                      \
          "Profile at the bytecode level during interpretation")            \
                                                                            \
  develop_pd(bool, ProfileTraps,                                            \
          "Profile deoptimization traps at the bytecode level")             \
                                                                            \
  product(intx, ProfileMaturityPercentage, 20,                              \
          "number of method invocations/branches (expressed as % of "       \
          "CompileThreshold) before using the method's profile")            \
          range(0, 100)                                                     \
                                                                            \
  product(bool, PrintMethodData, false, DIAGNOSTIC,                         \
          "Print the results of +ProfileInterpreter at end of run")         \
                                                                            \
  develop(bool, VerifyDataPointer, trueInDebug,                             \
          "Verify the method data pointer during interpreter profiling")    \
                                                                            \
  notproduct(bool, CrashGCForDumpingJavaThread, false,                      \
          "Manually make GC thread crash then dump java stack trace;  "     \
          "Test only")                                                      \
                                                                            \
  /* compilation */                                                         \
  product(bool, UseCompiler, true,                                          \
          "Use Just-In-Time compilation")                                   \
                                                                            \
  product(bool, UseCounterDecay, true,                                      \
          "Adjust recompilation counters")                                  \
                                                                            \
  develop(intx, CounterHalfLifeTime,    30,                                 \
          "Half-life time of invocation counters (in seconds)")             \
                                                                            \
  develop(intx, CounterDecayMinIntervalLength,   500,                       \
          "The minimum interval (in milliseconds) between invocation of "   \
          "CounterDecay")                                                   \
                                                                            \
  product(bool, AlwaysCompileLoopMethods, false,                            \
          "When using recompilation, never interpret methods "              \
          "containing loops")                                               \
                                                                            \
  product(intx,  AllocatePrefetchStyle, 1,                                  \
          "0 = no prefetch, "                                               \
          "1 = generate prefetch instructions for each allocation, "        \
          "2 = use TLAB watermark to gate allocation prefetch, "            \
          "3 = generate one prefetch instruction per cache line")           \
          range(0, 3)                                                       \
                                                                            \
  product(intx,  AllocatePrefetchDistance, -1,                              \
          "Distance to prefetch ahead of allocation pointer. "              \
          "-1: use system-specific value (automatically determined")        \
          constraint(AllocatePrefetchDistanceConstraintFunc,AfterMemoryInit)\
                                                                            \
  product(intx,  AllocatePrefetchLines, 3,                                  \
          "Number of lines to prefetch ahead of array allocation pointer")  \
          range(1, 64)                                                      \
                                                                            \
  product(intx,  AllocateInstancePrefetchLines, 1,                          \
          "Number of lines to prefetch ahead of instance allocation "       \
          "pointer")                                                        \
          range(1, 64)                                                      \
                                                                            \
  product(intx,  AllocatePrefetchStepSize, 16,                              \
          "Step size in bytes of sequential prefetch instructions")         \
          range(1, 512)                                                     \
          constraint(AllocatePrefetchStepSizeConstraintFunc,AfterMemoryInit)\
                                                                            \
  product(intx,  AllocatePrefetchInstr, 0,                                  \
          "Select instruction to prefetch ahead of allocation pointer")     \
          constraint(AllocatePrefetchInstrConstraintFunc, AfterMemoryInit)  \
                                                                            \
  /* deoptimization */                                                      \
  develop(bool, TraceDeoptimization, false,                                 \
          "Trace deoptimization")                                           \
                                                                            \
  develop(bool, PrintDeoptimizationDetails, false,                          \
          "Print more information about deoptimization")                    \
                                                                            \
  develop(bool, DebugDeoptimization, false,                                 \
          "Tracing various information while debugging deoptimization")     \
                                                                            \
  product(intx, SelfDestructTimer, 0,                                       \
          "Will cause VM to terminate after a given time (in minutes) "     \
          "(0 means off)")                                                  \
          range(0, max_intx)                                                \
                                                                            \
  product(intx, MaxJavaStackTraceDepth, 1024,                               \
          "The maximum number of lines in the stack trace for Java "        \
          "exceptions (0 means all)")                                       \
          range(0, max_jint/2)                                              \
                                                                            \
  /* notice: the max range value here is max_jint, not max_intx  */         \
  /* because of overflow issue                                   */         \
  product(intx, GuaranteedSafepointInterval, 1000, DIAGNOSTIC,              \
          "Guarantee a safepoint (at least) every so many milliseconds "    \
          "(0 means none)")                                                 \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, SafepointTimeoutDelay, 10000,                               \
          "Delay in milliseconds for option SafepointTimeout")              \
          range(0, max_intx LP64_ONLY(/MICROUNITS))                         \
                                                                            \
  product(intx, NmethodSweepActivity, 10,                                   \
          "Removes cold nmethods from code cache if > 0. Higher values "    \
          "result in more aggressive sweeping")                             \
          range(0, 2000)                                                    \
                                                                            \
  notproduct(bool, LogSweeper, false,                                       \
          "Keep a ring buffer of sweeper activity")                         \
                                                                            \
  notproduct(intx, SweeperLogEntries, 1024,                                 \
          "Number of records in the ring buffer of sweeper activity")       \
                                                                            \
  develop(intx, MallocCatchPtr, -1,                                         \
          "Hit breakpoint when mallocing/freeing this pointer")             \
                                                                            \
  notproduct(ccstrlist, SuppressErrorAt, "",                                \
          "List of assertions (file:line) to muzzle")                       \
                                                                            \
  develop(intx, StackPrintLimit, 100,                                       \
          "number of stack frames to print in VM-level stack dump")         \
                                                                            \
  notproduct(intx, MaxElementPrintSize, 256,                                \
          "maximum number of elements to print")                            \
                                                                            \
  notproduct(intx, MaxSubklassPrintSize, 4,                                 \
          "maximum number of subklasses to print when printing klass")      \
                                                                            \
  develop(intx, MaxForceInlineLevel, 100,                                   \
          "maximum number of nested calls that are forced for inlining "    \
          "(using CompileCommand or marked w/ @ForceInline)")               \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, MinInliningThreshold, 250,                                  \
          "The minimum invocation count a method needs to have to be "      \
          "inlined")                                                        \
          range(0, max_jint)                                                \
                                                                            \
  develop(intx, MethodHistogramCutoff, 100,                                 \
          "The cutoff value for method invocation histogram (+CountCalls)") \
                                                                            \
  develop(intx, DontYieldALotInterval,    10,                               \
          "Interval between which yields will be dropped (milliseconds)")   \
                                                                            \
  notproduct(intx, DeoptimizeALotInterval,     5,                           \
          "Number of exits until DeoptimizeALot kicks in")                  \
                                                                            \
  notproduct(intx, ZombieALotInterval,     5,                               \
          "Number of exits until ZombieALot kicks in")                      \
                                                                            \
  product(uintx, MallocMaxTestWords,     0, DIAGNOSTIC,                     \
          "If non-zero, maximum number of words that malloc/realloc can "   \
          "allocate (for testing only)")                                    \
          range(0, max_uintx)                                               \
                                                                            \
  product(intx, TypeProfileWidth, 2,                                        \
          "Number of receiver types to record in call/cast profile")        \
          range(0, 8)                                                       \
                                                                            \
  develop(intx, BciProfileWidth,      2,                                    \
          "Number of return bci's to record in ret profile")                \
                                                                            \
  product(intx, PerMethodRecompilationCutoff, 400,                          \
          "After recompiling N times, stay in the interpreter (-1=>'Inf')") \
          range(-1, max_intx)                                               \
                                                                            \
  product(intx, PerBytecodeRecompilationCutoff, 200,                        \
          "Per-BCI limit on repeated recompilation (-1=>'Inf')")            \
          range(-1, max_intx)                                               \
                                                                            \
  product(intx, PerMethodTrapLimit,  100,                                   \
          "Limit on traps (of one kind) in a method (includes inlines)")    \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, PerMethodSpecTrapLimit,  5000, EXPERIMENTAL,                \
          "Limit on speculative traps (of one kind) in a method "           \
          "(includes inlines)")                                             \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, PerBytecodeTrapLimit,  4,                                   \
          "Limit on traps (of one kind) at a particular BCI")               \
          range(0, max_jint)                                                \
                                                                            \
  product(intx, SpecTrapLimitExtraEntries,  3, EXPERIMENTAL,                \
          "Extra method data trap entries for speculation")                 \
                                                                            \
  develop(intx, InlineFrequencyRatio,    20,                                \
          "Ratio of call site execution to caller method invocation")       \
          range(0, max_jint)                                                \
                                                                            \
  product_pd(intx, InlineFrequencyCount, DIAGNOSTIC,                        \
          "Count of call site execution necessary to trigger frequent "     \
          "inlining")                                                       \
          range(0, max_jint)                                                \
                                                                            \
  develop(intx, InlineThrowCount,    50,                                    \
          "Force inlining of interpreted methods that throw this often")    \
          range(0, max_jint)                                                \
                                                                            \
  develop(intx, InlineThrowMaxSize,   200,                                  \
          "Force inlining of throwing methods smaller than this")           \
          range(0, max_jint)                                                \
                                                                            \
  product(size_t, MetaspaceSize, NOT_LP64(16 * M) LP64_ONLY(21 * M),        \
          "Initial threshold (in bytes) at which a garbage collection "     \
          "is done to reduce Metaspace usage")                              \
          constraint(MetaspaceSizeConstraintFunc,AfterErgo)                 \
                                                                            \
  product(size_t, MaxMetaspaceSize, max_uintx,                              \
          "Maximum size of Metaspaces (in bytes)")                          \
          constraint(MaxMetaspaceSizeConstraintFunc,AfterErgo)              \
                                                                            \
  product(size_t, CompressedClassSpaceSize, 1*G,                            \
          "Maximum size of class area in Metaspace when compressed "        \
          "class pointers are used")                                        \
          range(1*M, 3*G)                                                   \
                                                                            \
  develop(size_t, CompressedClassSpaceBaseAddress, 0,                       \
          "Force the class space to be allocated at this address or "       \
          "fails VM initialization (requires -Xshare=off.")                 \
                                                                            \
  product(ccstr, MetaspaceReclaimPolicy, "balanced",                        \
          "options: balanced, aggressive, none")                            \
                                                                            \
  product(bool, PrintMetaspaceStatisticsAtExit, false, DIAGNOSTIC,          \
          "Print metaspace statistics upon VM exit.")                       \
                                                                            \
  product(bool, MetaspaceGuardAllocations, false, DIAGNOSTIC,               \
          "Metapace allocations are guarded.")                              \
                                                                            \
  product(bool, MetaspaceHandleDeallocations, true, DIAGNOSTIC,             \
          "Switch off Metapace deallocation handling.")                     \
                                                                            \
  product(uintx, MinHeapFreeRatio, 40, MANAGEABLE,                          \
          "The minimum percentage of heap free after GC to avoid expansion."\
          " For most GCs this applies to the old generation. In G1 and"     \
          " ParallelGC it applies to the whole heap.")                      \
          range(0, 100)                                                     \
          constraint(MinHeapFreeRatioConstraintFunc,AfterErgo)              \
                                                                            \
  product(uintx, MaxHeapFreeRatio, 70, MANAGEABLE,                          \
          "The maximum percentage of heap free after GC to avoid shrinking."\
          " For most GCs this applies to the old generation. In G1 and"     \
          " ParallelGC it applies to the whole heap.")                      \
          range(0, 100)                                                     \
          constraint(MaxHeapFreeRatioConstraintFunc,AfterErgo)              \
                                                                            \
  product(bool, ShrinkHeapInSteps, true,                                    \
          "When disabled, informs the GC to shrink the java heap directly"  \
          " to the target size at the next full GC rather than requiring"   \
          " smaller steps during multiple full GCs.")                       \
                                                                            \
  product(intx, SoftRefLRUPolicyMSPerMB, 1000,                              \
          "Number of milliseconds per MB of free space in the heap")        \
          range(0, max_intx)                                                \
          constraint(SoftRefLRUPolicyMSPerMBConstraintFunc,AfterMemoryInit) \
                                                                            \
  product(size_t, MinHeapDeltaBytes, ScaleForWordSize(128*K),               \
          "The minimum change in heap space due to GC (in bytes)")          \
          range(0, max_uintx)                                               \
                                                                            \
  product(size_t, MinMetaspaceExpansion, ScaleForWordSize(256*K),           \
          "The minimum expansion of Metaspace (in bytes)")                  \
          range(0, max_uintx)                                               \
                                                                            \
  product(uintx, MaxMetaspaceFreeRatio,    70,                              \
          "The maximum percentage of Metaspace free after GC to avoid "     \
          "shrinking")                                                      \
          range(0, 100)                                                     \
          constraint(MaxMetaspaceFreeRatioConstraintFunc,AfterErgo)         \
                                                                            \
  product(uintx, MinMetaspaceFreeRatio,    40,                              \
          "The minimum percentage of Metaspace free after GC to avoid "     \
          "expansion")                                                      \
          range(0, 99)                                                      \
          constraint(MinMetaspaceFreeRatioConstraintFunc,AfterErgo)         \
                                                                            \
  product(size_t, MaxMetaspaceExpansion, ScaleForWordSize(4*M),             \
          "The maximum expansion of Metaspace without full GC (in bytes)")  \
          range(0, max_uintx)                                               \
                                                                            \
  /* stack parameters */                                                    \
  product_pd(intx, StackYellowPages,                                        \
          "Number of yellow zone (recoverable overflows) pages of size "    \
          "4KB. If pages are bigger yellow zone is aligned up.")            \
          range(MIN_STACK_YELLOW_PAGES, (DEFAULT_STACK_YELLOW_PAGES+5))     \
                                                                            \
  product_pd(intx, StackRedPages,                                           \
          "Number of red zone (unrecoverable overflows) pages of size "     \
          "4KB. If pages are bigger red zone is aligned up.")               \
          range(MIN_STACK_RED_PAGES, (DEFAULT_STACK_RED_PAGES+2))           \
                                                                            \
  product_pd(intx, StackReservedPages,                                      \
          "Number of reserved zone (reserved to annotated methods) pages"   \
          " of size 4KB. If pages are bigger reserved zone is aligned up.") \
          range(MIN_STACK_RESERVED_PAGES, (DEFAULT_STACK_RESERVED_PAGES+10))\
                                                                            \
  product(bool, RestrictReservedStack, true,                                \
          "Restrict @ReservedStackAccess to trusted classes")               \
                                                                            \
  /* greater stack shadow pages can't generate instruction to bang stack */ \
  product_pd(intx, StackShadowPages,                                        \
          "Number of shadow zone (for overflow checking) pages of size "    \
          "4KB. If pages are bigger shadow zone is aligned up. "            \
          "This should exceed the depth of the VM and native call stack.")  \
          range(MIN_STACK_SHADOW_PAGES, (DEFAULT_STACK_SHADOW_PAGES+30))    \
                                                                            \
  product_pd(intx, ThreadStackSize,                                         \
          "Thread Stack Size (in Kbytes)")                                  \
          range(0, 1 * M)                                                   \
                                                                            \
  product_pd(intx, VMThreadStackSize,                                       \
          "Non-Java Thread Stack Size (in Kbytes)")                         \
          range(0, max_intx/(1 * K))                                        \
                                                                            \
  product_pd(intx, CompilerThreadStackSize,                                 \
          "Compiler Thread Stack Size (in Kbytes)")                         \
          range(0, max_intx/(1 * K))                                        \
                                                                            \
  develop_pd(size_t, JVMInvokeMethodSlack,                                  \
          "Stack space (bytes) required for JVM_InvokeMethod to complete")  \
                                                                            \
  /* code cache parameters                                    */            \
  develop_pd(uintx, CodeCacheSegmentSize,                                   \
          "Code cache segment size (in bytes) - smallest unit of "          \
          "allocation")                                                     \
          range(1, 1024)                                                    \
          constraint(CodeCacheSegmentSizeConstraintFunc, AfterErgo)         \
                                                                            \
  develop_pd(intx, CodeEntryAlignment,                                      \
          "Code entry alignment for generated code (in bytes)")             \
          constraint(CodeEntryAlignmentConstraintFunc, AfterErgo)           \
                                                                            \
  product_pd(intx, OptoLoopAlignment,                                       \
          "Align inner loops to zero relative to this modulus")             \
          range(1, 16)                                                      \
          constraint(OptoLoopAlignmentConstraintFunc, AfterErgo)            \
                                                                            \
  product_pd(uintx, InitialCodeCacheSize,                                   \
          "Initial code cache size (in bytes)")                             \
          constraint(VMPageSizeConstraintFunc, AtParse)                     \
                                                                            \
  develop_pd(uintx, CodeCacheMinimumUseSpace,                               \
          "Minimum code cache size (in bytes) required to start VM.")       \
          range(0, max_uintx)                                               \
                                                                            \
  product(bool, SegmentedCodeCache, false,                                  \
          "Use a segmented code cache")                                     \
                                                                            \
  product_pd(uintx, ReservedCodeCacheSize,                                  \
          "Reserved code cache size (in bytes) - maximum code cache size")  \
          constraint(VMPageSizeConstraintFunc, AtParse)                     \
                                                                            \
  product_pd(uintx, NonProfiledCodeHeapSize,                                \
          "Size of code heap with non-profiled methods (in bytes)")         \
          range(0, max_uintx)                                               \
                                                                            \
  product_pd(uintx, ProfiledCodeHeapSize,                                   \
          "Size of code heap with profiled methods (in bytes)")             \
          range(0, max_uintx)                                               \
                                                                            \
  product_pd(uintx, NonNMethodCodeHeapSize,                                 \
          "Size of code heap with non-nmethods (in bytes)")                 \
          constraint(VMPageSizeConstraintFunc, AtParse)                     \
                                                                            \
  product_pd(uintx, CodeCacheExpansionSize,                                 \
          "Code cache expansion size (in bytes)")                           \
          range(32*K, max_uintx)                                            \
                                                                            \
  product_pd(uintx, CodeCacheMinBlockLength, DIAGNOSTIC,                    \
          "Minimum number of segments in a code cache block")               \
          range(1, 100)                                                     \
                                                                            \
  notproduct(bool, ExitOnFullCodeCache, false,                              \
          "Exit the VM if we fill the code cache")                          \
                                                                            \
  product(bool, UseCodeCacheFlushing, true,                                 \
          "Remove cold/old nmethods from the code cache")                   \
                                                                            \
  product(double, SweeperThreshold, 0.5,                                    \
          "Threshold controlling when code cache sweeper is invoked."       \
          "Value is percentage of ReservedCodeCacheSize.")                  \
          range(0.0, 100.0)                                                 \
                                                                            \
  product(uintx, StartAggressiveSweepingAt, 10,                             \
          "Start aggressive sweeping if X[%] of the code cache is free."    \
          "Segmented code cache: X[%] of the non-profiled heap."            \
          "Non-segmented code cache: X[%] of the total code cache")         \
          range(0, 100)                                                     \
                                                                            \
  /* interpreter debugging */                                               \
  develop(intx, BinarySwitchThreshold, 5,                                   \
          "Minimal number of lookupswitch entries for rewriting to binary " \
          "switch")                                                         \
                                                                            \
  develop(intx, StopInterpreterAt, 0,                                       \
          "Stop interpreter execution at specified bytecode number")        \
                                                                            \
  develop(intx, TraceBytecodesAt, 0,                                        \
          "Trace bytecodes starting with specified bytecode number")        \
                                                                            \
  /* Priorities */                                                          \
  product_pd(bool, UseThreadPriorities,  "Use native thread priorities")    \
                                                                            \
  product(intx, ThreadPriorityPolicy, 0,                                    \
          "0 : Normal.                                                     "\
          "    VM chooses priorities that are appropriate for normal       "\
          "    applications.                                               "\
          "    On Windows applications are allowed to use higher native    "\
          "    priorities. However, with ThreadPriorityPolicy=0, VM will   "\
          "    not use the highest possible native priority,               "\
          "    THREAD_PRIORITY_TIME_CRITICAL, as it may interfere with     "\
          "    system threads. On Linux thread priorities are ignored      "\
          "    because the OS does not support static priority in          "\
          "    SCHED_OTHER scheduling class which is the only choice for   "\
          "    non-root, non-realtime applications.                        "\
          "1 : Aggressive.                                                 "\
          "    Java thread priorities map over to the entire range of      "\
          "    native thread priorities. Higher Java thread priorities map "\
          "    to higher native thread priorities. This policy should be   "\
          "    used with care, as sometimes it can cause performance       "\
          "    degradation in the application and/or the entire system. On "\
          "    Linux/BSD/macOS this policy requires root privilege or an   "\
          "    extended capability.")                                       \
          range(0, 1)                                                       \
                                                                            \
  product(bool, ThreadPriorityVerbose, false,                               \
          "Print priority changes")                                         \
                                                                            \
  product(intx, CompilerThreadPriority, -1,                                 \
          "The native priority at which compiler threads should run "       \
          "(-1 means no change)")                                           \
          range(min_jint, max_jint)                                         \
                                                                            \
  product(intx, VMThreadPriority, -1,                                       \
          "The native priority at which the VM thread should run "          \
          "(-1 means no change)")                                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority1_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority2_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority3_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority4_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority5_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority6_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority7_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority8_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority9_To_OSPriority, -1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(intx, JavaPriority10_To_OSPriority,-1,                            \
          "Map Java priorities to OS priorities")                           \
          range(-1, 127)                                                    \
                                                                            \
  product(bool, UseCriticalJavaThreadPriority, false, EXPERIMENTAL,         \
          "Java thread priority 10 maps to critical scheduling priority")   \
                                                                            \
  product(bool, UseCriticalCompilerThreadPriority, false, EXPERIMENTAL,     \
          "Compiler thread(s) run at critical scheduling priority")         \
                                                                            \
  develop(intx, NewCodeParameter,      0,                                   \
          "Testing Only: Create a dedicated integer parameter before "      \
          "putback")                                                        \
                                                                            \
  /* new oopmap storage allocation */                                       \
  develop(intx, MinOopMapAllocation,     8,                                 \
          "Minimum number of OopMap entries in an OopMapSet")               \
                                                                            \
  /* recompilation */                                                       \
  product_pd(intx, CompileThreshold,                                        \
          "number of interpreted method invocations before (re-)compiling") \
          constraint(CompileThresholdConstraintFunc, AfterErgo)             \
                                                                            \
  product_pd(bool, TieredCompilation,                                       \
          "Enable tiered compilation")                                      \
                                                                            \
  /* Properties for Java libraries  */                                      \
                                                                            \
  product(uint64_t, MaxDirectMemorySize, 0,                                 \
          "Maximum total size of NIO direct-buffer allocations")            \
          range(0, max_jlong)                                               \
                                                                            \
  /* Flags used for temporary code during development  */                   \
                                                                            \
  product(bool, UseNewCode, false, DIAGNOSTIC,                              \
          "Testing Only: Use the new version while testing")                \
                                                                            \
  product(bool, UseNewCode2, false, DIAGNOSTIC,                             \
          "Testing Only: Use the new version while testing")                \
                                                                            \
  product(bool, UseNewCode3, false, DIAGNOSTIC,                             \
          "Testing Only: Use the new version while testing")                \
                                                                            \
  notproduct(bool, UseDebuggerErgo, false,                                  \
          "Debugging Only: Adjust the VM to be more debugger-friendly. "    \
          "Turns on the other UseDebuggerErgo* flags")                      \
                                                                            \
  notproduct(bool, UseDebuggerErgo1, false,                                 \
          "Debugging Only: Enable workarounds for debugger induced "        \
          "os::processor_id() >= os::processor_count() problems")           \
                                                                            \
  notproduct(bool, UseDebuggerErgo2, false,                                 \
          "Debugging Only: Limit the number of spawned JVM threads")        \
                                                                            \
  notproduct(bool, EnableJVMTIStackDepthAsserts, true,                      \
          "Enable JVMTI asserts related to stack depth checks")             \
                                                                            \
  /* flags for performance data collection */                               \
                                                                            \
  product(bool, UsePerfData, true,                                          \
          "Flag to disable jvmstat instrumentation for performance testing "\
          "and problem isolation purposes")                                 \
                                                                            \
  product(bool, PerfDataSaveToFile, false,                                  \
          "Save PerfData memory to hsperfdata_<pid> file on exit")          \
                                                                            \
  product(ccstr, PerfDataSaveFile, NULL,                                    \
          "Save PerfData memory to the specified absolute pathname. "       \
          "The string %p in the file name (if present) "                    \
          "will be replaced by pid")                                        \
                                                                            \
  product(intx, PerfDataSamplingInterval, 50,                               \
          "Data sampling interval (in milliseconds)")                       \
          range(PeriodicTask::min_interval, max_jint)                       \
          constraint(PerfDataSamplingIntervalFunc, AfterErgo)               \
                                                                            \
  product(bool, PerfDisableSharedMem, false,                                \
          "Store performance data in standard memory")                      \
                                                                            \
  product(intx, PerfDataMemorySize, 32*K,                                   \
          "Size of performance data memory region. Will be rounded "        \
          "up to a multiple of the native os page size.")                   \
          range(128, 32*64*K)                                               \
                                                                            \
  product(intx, PerfMaxStringConstLength, 1024,                             \
          "Maximum PerfStringConstant string length before truncation")     \
          range(32, 32*K)                                                   \
                                                                            \
  product(bool, PerfAllowAtExitRegistration, false,                         \
          "Allow registration of atexit() methods")                         \
                                                                            \
  product(bool, PerfBypassFileSystemCheck, false,                           \
          "Bypass Win32 file system criteria checks (Windows Only)")        \
                                                                            \
  product(intx, UnguardOnExecutionViolation, 0,                             \
          "Unguard page and retry on no-execute fault (Win32 only) "        \
          "0=off, 1=conservative, 2=aggressive")                            \
          range(0, 2)                                                       \
                                                                            \
  /* Serviceability Support */                                              \
                                                                            \
  product(bool, ManagementServer, false,                                    \
          "Create JMX Management Server")                                   \
                                                                            \
  product(bool, DisableAttachMechanism, false,                              \
          "Disable mechanism that allows tools to attach to this VM")       \
                                                                            \
  product(bool, StartAttachListener, false,                                 \
          "Always start Attach Listener at VM startup")                     \
                                                                            \
  product(bool, EnableDynamicAgentLoading, true,                            \
          "Allow tools to load agents with the attach mechanism")           \
                                                                            \
  product(bool, PrintConcurrentLocks, false, MANAGEABLE,                    \
          "Print java.util.concurrent locks in thread dump")                \
                                                                            \
  /* Shared spaces */                                                       \
                                                                            \
  product(bool, UseSharedSpaces, true,                                      \
          "Use shared spaces for metadata")                                 \
                                                                            \
  product(bool, VerifySharedSpaces, false,                                  \
          "Verify integrity of shared spaces")                              \
                                                                            \
  product(bool, RequireSharedSpaces, false,                                 \
          "Require shared spaces for metadata")                             \
                                                                            \
  product(bool, DumpSharedSpaces, false,                                    \
          "Special mode: JVM reads a class list, loads classes, builds "    \
          "shared spaces, and dumps the shared spaces to a file to be "     \
          "used in future JVM runs")                                        \
                                                                            \
  product(bool, DynamicDumpSharedSpaces, false,                             \
          "Dynamic archive")                                                \
                                                                            \
  product(bool, RecordDynamicDumpInfo, false,                               \
          "Record class info for jcmd VM.cds dynamic_dump")                 \
                                                                            \
  product(bool, PrintSharedArchiveAndExit, false,                           \
          "Print shared archive file contents")                             \
                                                                            \
  product(bool, PrintSharedDictionary, false,                               \
          "If PrintSharedArchiveAndExit is true, also print the shared "    \
          "dictionary")                                                     \
                                                                            \
  product(size_t, SharedBaseAddress, LP64_ONLY(32*G)                        \
          NOT_LP64(LINUX_ONLY(2*G) NOT_LINUX(0)),                           \
          "Address to allocate shared memory region for class data")        \
          range(0, SIZE_MAX)                                                \
                                                                            \
  product(ccstr, SharedArchiveConfigFile, NULL,                             \
          "Data to add to the CDS archive file")                            \
                                                                            \
  product(uintx, SharedSymbolTableBucketSize, 4,                            \
          "Average number of symbols per bucket in shared table")           \
          range(2, 246)                                                     \
                                                                            \
  product(bool, AllowArchivingWithJavaAgent, false, DIAGNOSTIC,             \
          "Allow Java agent to be run with CDS dumping")                    \
                                                                            \
  product(bool, PrintMethodHandleStubs, false, DIAGNOSTIC,                  \
          "Print generated stub code for method handles")                   \
                                                                            \
  product(bool, VerifyMethodHandles, trueInDebug, DIAGNOSTIC,               \
          "perform extra checks when constructing method handles")          \
                                                                            \
  product(bool, ShowHiddenFrames, false, DIAGNOSTIC,                        \
          "show method handle implementation frames (usually hidden)")      \
                                                                            \
  product(bool, TrustFinalNonStaticFields, false, EXPERIMENTAL,             \
          "trust final non-static declarations for constant folding")       \
                                                                            \
  product(bool, FoldStableValues, true, DIAGNOSTIC,                         \
          "Optimize loads from stable fields (marked w/ @Stable)")          \
                                                                            \
  product(int, UseBootstrapCallInfo, 1, DIAGNOSTIC,                         \
          "0: when resolving InDy or ConDy, force all BSM arguments to be " \
          "resolved before the bootstrap method is called; 1: when a BSM "  \
          "that may accept a BootstrapCallInfo is detected, use that API "  \
          "to pass BSM arguments, which allows the BSM to delay their "     \
          "resolution; 2+: stress test the BCI API by calling more BSMs "   \
          "via that API, instead of with the eagerly-resolved array.")      \
                                                                            \
  product(bool, PauseAtStartup,      false, DIAGNOSTIC,                     \
          "Causes the VM to pause at startup time and wait for the pause "  \
          "file to be removed (default: ./vm.paused.<pid>)")                \
                                                                            \
  product(ccstr, PauseAtStartupFile, NULL, DIAGNOSTIC,                      \
          "The file to create and for whose removal to await when pausing " \
          "at startup. (default: ./vm.paused.<pid>)")                       \
                                                                            \
  product(bool, PauseAtExit, false, DIAGNOSTIC,                             \
          "Pause and wait for keypress on exit if a debugger is attached")  \
                                                                            \
  product(bool, ExtendedDTraceProbes,    false,                             \
          "Enable performance-impacting dtrace probes")                     \
                                                                            \
  product(bool, DTraceMethodProbes, false,                                  \
          "Enable dtrace probes for method-entry and method-exit")          \
                                                                            \
  product(bool, DTraceAllocProbes, false,                                   \
          "Enable dtrace probes for object allocation")                     \
                                                                            \
  product(bool, DTraceMonitorProbes, false,                                 \
          "Enable dtrace probes for monitor events")                        \
                                                                            \
  product(bool, RelaxAccessControlCheck, false,                             \
          "Relax the access control checks in the verifier")                \
                                                                            \
  product(uintx, StringTableSize, defaultStringTableSize,                   \
          "Number of buckets in the interned String table "                 \
          "(will be rounded to nearest higher power of 2)")                 \
          range(minimumStringTableSize, 16777216ul /* 2^24 */)              \
                                                                            \
  product(uintx, SymbolTableSize, defaultSymbolTableSize, EXPERIMENTAL,     \
          "Number of buckets in the JVM internal Symbol table")             \
          range(minimumSymbolTableSize, 16777216ul /* 2^24 */)              \
                                                                            \
  product(bool, UseStringDeduplication, false,                              \
          "Use string deduplication")                                       \
                                                                            \
  product(uint, StringDeduplicationAgeThreshold, 3,                         \
          "A string must reach this age (or be promoted to an old region) " \
          "to be considered for deduplication")                             \
          range(1, markWord::max_age)                                       \
                                                                            \
  product(size_t, StringDeduplicationInitialTableSize, 500, EXPERIMENTAL,   \
          "Approximate initial number of buckets in the table")             \
          range(1, 1 * G)                                                   \
                                                                            \
  product(double, StringDeduplicationGrowTableLoad, 14.0, EXPERIMENTAL,     \
          "Entries per bucket above which the table should be expanded")    \
          range(0.1, 1000.0)                                                \
                                                                            \
  product(double, StringDeduplicationShrinkTableLoad, 1.0, EXPERIMENTAL,    \
          "Entries per bucket below which the table should be shrunk")      \
          range(0.01, 100.0)                                                \
                                                                            \
  product(double, StringDeduplicationTargetTableLoad, 7.0, EXPERIMENTAL,    \
          "Desired entries per bucket when resizing the table")             \
          range(0.01, 1000.0)                                               \
                                                                            \
  product(size_t, StringDeduplicationCleanupDeadMinimum, 100, EXPERIMENTAL, \
          "Minimum number of dead table entries for cleaning the table")    \
                                                                            \
  product(int, StringDeduplicationCleanupDeadPercent, 5, EXPERIMENTAL,      \
          "Minimum percentage of dead table entries for cleaning the table") \
          range(1, 100)                                                     \
                                                                            \
  product(bool, StringDeduplicationResizeALot, false, DIAGNOSTIC,           \
          "Force more frequent table resizing")                             \
                                                                            \
  product(uint64_t, StringDeduplicationHashSeed, 0, DIAGNOSTIC,             \
          "Seed for the table hashing function; 0 requests computed seed")  \
                                                                            \
  product(bool, WhiteBoxAPI, false, DIAGNOSTIC,                             \
          "Enable internal testing APIs")                                   \
                                                                            \
  product(ccstr, DumpLoadedClassList, NULL,                                 \
          "Dump the names all loaded classes, that could be stored into "   \
          "the CDS archive, in the specified file")                         \
                                                                            \
  product(ccstr, SharedClassListFile, NULL,                                 \
          "Override the default CDS class list")                            \
                                                                            \
  product(ccstr, SharedArchiveFile, NULL,                                   \
          "Override the default location of the CDS archive file")          \
                                                                            \
  product(ccstr, ArchiveClassesAtExit, NULL,                                \
          "The path and name of the dynamic archive file")                  \
                                                                            \
  product(ccstr, ExtraSharedClassListFile, NULL,                            \
          "Extra classlist for building the CDS archive file")              \
                                                                            \
  product(intx, ArchiveRelocationMode, 0, DIAGNOSTIC,                       \
           "(0) first map at preferred address, and if "                    \
           "unsuccessful, map at alternative address (default); "           \
           "(1) always map at alternative address; "                        \
           "(2) always map at preferred address, and if unsuccessful, "     \
           "do not map the archive")                                        \
           range(0, 2)                                                      \
                                                                            \
  product(size_t, ArrayAllocatorMallocLimit, (size_t)-1, EXPERIMENTAL,      \
          "Allocation less than this value will be allocated "              \
          "using malloc. Larger allocations will use mmap.")                \
                                                                            \
  product(bool, AlwaysAtomicAccesses, false, EXPERIMENTAL,                  \
          "Accesses to all variables should always be atomic")              \
                                                                            \
  product(bool, UseUnalignedAccesses, false, DIAGNOSTIC,                    \
          "Use unaligned memory accesses in Unsafe")                        \
                                                                            \
  product_pd(bool, PreserveFramePointer,                                    \
             "Use the FP register for holding the frame pointer "           \
             "and not as a general purpose register.")                      \
                                                                            \
  product(size_t, AsyncLogBufferSize, 2*M,                                  \
          "Memory budget (in bytes) for the buffer of Asynchronous "        \
          "Logging (-Xlog:async).")                                         \
          range(100*K, 50*M)                                                \
                                                                            \
  product(bool, CheckIntrinsics, true, DIAGNOSTIC,                          \
             "When a class C is loaded, check that "                        \
             "(1) all intrinsics defined by the VM for class C are present "\
             "in the loaded class file and are marked with the "            \
             "@IntrinsicCandidate annotation, that "                        \
             "(2) there is an intrinsic registered for all loaded methods " \
             "that are annotated with the @IntrinsicCandidate annotation, " \
             "and that "                                                    \
             "(3) no orphan methods exist for class C (i.e., methods for "  \
             "which the VM declares an intrinsic but that are not declared "\
             "in the loaded class C. "                                      \
             "Check (3) is available only in debug builds.")                \
                                                                            \
  product_pd(intx, InitArrayShortSize, DIAGNOSTIC,                          \
          "Threshold small size (in bytes) for clearing arrays. "           \
          "Anything this size or smaller may get converted to discrete "    \
          "scalar stores.")                                                 \
          range(0, max_intx)                                                \
          constraint(InitArrayShortSizeConstraintFunc, AfterErgo)           \
                                                                            \
  product(ccstr, AllocateHeapAt, NULL,                                      \
          "Path to the directory where a temporary file will be created "   \
          "to use as the backing store for Java Heap.")                     \
                                                                            \
  develop(int, VerifyMetaspaceInterval, DEBUG_ONLY(500) NOT_DEBUG(0),       \
               "Run periodic metaspace verifications (0 - none, "           \
               "1 - always, >1 every nth interval)")                        \
                                                                            \
  product(bool, ShowRegistersOnAssert, true, DIAGNOSTIC,                    \
          "On internal errors, include registers in error report.")         \
                                                                            \
  product(bool, UseSwitchProfiling, true, DIAGNOSTIC,                       \
          "leverage profiling for table/lookup switch")                     \
                                                                            \
  develop(bool, TraceMemoryWriteback, false,                                \
          "Trace memory writeback operations")                              \
                                                                            \
  JFR_ONLY(product(bool, FlightRecorder, false,                             \
          "(Deprecated) Enable Flight Recorder"))                           \
                                                                            \
  JFR_ONLY(product(ccstr, FlightRecorderOptions, NULL,                      \
          "Flight Recorder options"))                                       \
                                                                            \
  JFR_ONLY(product(ccstr, StartFlightRecording, NULL,                       \
          "Start flight recording with options"))                           \
                                                                            \
  product(bool, UseFastUnorderedTimeStamps, false, EXPERIMENTAL,            \
          "Use platform unstable time where supported for timestamps only") \
                                                                            \
  product(bool, UseEmptySlotsInSupers, true,                                \
                "Allow allocating fields in empty slots of super-classes")  \
                                                                            \
  product(bool, DeoptimizeNMethodBarriersALot, false, DIAGNOSTIC,           \
                "Make nmethod barriers deoptimise a lot.")                  \
                                                                            \
  develop(bool, VerifyCrossModifyFence,                                     \
          false AARCH64_ONLY(DEBUG_ONLY(||true)),                           \
             "Mark all threads after a safepoint, and clear on a modify "   \
             "fence. Add cleanliness checks.")                              \
                                                                            \
  develop(bool, TraceOptimizedUpcallStubs, false,                              \
                "Trace optimized upcall stub generation")                      \

// end of RUNTIME_FLAGS

DECLARE_FLAGS(LP64_RUNTIME_FLAGS)
DECLARE_ARCH_FLAGS(ARCH_FLAGS)
DECLARE_FLAGS(RUNTIME_FLAGS)
DECLARE_FLAGS(RUNTIME_OS_FLAGS)

#endif // SHARE_RUNTIME_GLOBALS_HPP
