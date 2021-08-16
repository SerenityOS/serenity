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

#ifndef CPU_X86_GLOBALS_X86_HPP
#define CPU_X86_GLOBALS_X86_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Sets the default values for platform dependent flags used by the runtime system.
// (see globals.hpp)

define_pd_global(bool, ImplicitNullChecks,       true);  // Generate code for implicit null checks
define_pd_global(bool, TrapBasedNullChecks,      false); // Not needed on x86.
define_pd_global(bool, UncommonNullCast,         true);  // Uncommon-trap NULLs passed to check cast

define_pd_global(uintx, CodeCacheSegmentSize,    64 COMPILER1_AND_COMPILER2_PRESENT(+64)); // Tiered compilation has large code-entry alignment.
// See 4827828 for this change. There is no globals_core_i486.hpp. I can't
// assign a different value for C2 without touching a number of files. Use
// #ifdef to minimize the change as it's late in Mantis. -- FIXME.
// c1 doesn't have this problem because the fix to 4858033 assures us
// the the vep is aligned at CodeEntryAlignment whereas c2 only aligns
// the uep and the vep doesn't get real alignment but just slops on by
// only assured that the entry instruction meets the 5 byte size requirement.
#if COMPILER2_OR_JVMCI
define_pd_global(intx, CodeEntryAlignment,       32);
#else
define_pd_global(intx, CodeEntryAlignment,       16);
#endif // COMPILER2_OR_JVMCI
define_pd_global(intx, OptoLoopAlignment,        16);
define_pd_global(intx, InlineFrequencyCount,     100);
define_pd_global(intx, InlineSmallCode,          1000);

#define DEFAULT_STACK_YELLOW_PAGES (NOT_WINDOWS(2) WINDOWS_ONLY(3))
#define DEFAULT_STACK_RED_PAGES (1)
#define DEFAULT_STACK_RESERVED_PAGES (NOT_WINDOWS(1) WINDOWS_ONLY(0))

#define MIN_STACK_YELLOW_PAGES DEFAULT_STACK_YELLOW_PAGES
#define MIN_STACK_RED_PAGES DEFAULT_STACK_RED_PAGES
#define MIN_STACK_RESERVED_PAGES (0)

#ifdef _LP64
// Java_java_net_SocketOutputStream_socketWrite0() uses a 64k buffer on the
// stack if compiled for unix and LP64. To pass stack overflow tests we need
// 20 shadow pages.
#define DEFAULT_STACK_SHADOW_PAGES (NOT_WIN64(20) WIN64_ONLY(7) DEBUG_ONLY(+2))
// For those clients that do not use write socket, we allow
// the min range value to be below that of the default
#define MIN_STACK_SHADOW_PAGES (NOT_WIN64(10) WIN64_ONLY(7) DEBUG_ONLY(+2))
#else
#define DEFAULT_STACK_SHADOW_PAGES (4 DEBUG_ONLY(+5))
#define MIN_STACK_SHADOW_PAGES DEFAULT_STACK_SHADOW_PAGES
#endif // _LP64

define_pd_global(intx, StackYellowPages, DEFAULT_STACK_YELLOW_PAGES);
define_pd_global(intx, StackRedPages, DEFAULT_STACK_RED_PAGES);
define_pd_global(intx, StackShadowPages, DEFAULT_STACK_SHADOW_PAGES);
define_pd_global(intx, StackReservedPages, DEFAULT_STACK_RESERVED_PAGES);

define_pd_global(bool, RewriteBytecodes,     true);
define_pd_global(bool, RewriteFrequentPairs, true);

define_pd_global(uintx, TypeProfileLevel, 111);

define_pd_global(bool, CompactStrings, true);

define_pd_global(bool, PreserveFramePointer, false);

define_pd_global(intx, InitArrayShortSize, 8*BytesPerLong);

#define ARCH_FLAGS(develop,                                                 \
                   product,                                                 \
                   notproduct,                                              \
                   range,                                                   \
                   constraint)                                              \
                                                                            \
  develop(bool, IEEEPrecision, true,                                        \
          "Enables IEEE precision (for INTEL only)")                        \
                                                                            \
  product(bool, UseStoreImmI16, true,                                       \
          "Use store immediate 16-bits value instruction on x86")           \
                                                                            \
  product(intx, UseSSE, 99,                                                 \
          "Highest supported SSE instructions set on x86/x64")              \
          range(0, 99)                                                      \
                                                                            \
  product(intx, UseAVX, 3,                                                  \
          "Highest supported AVX instructions set on x86/x64")              \
          range(0, 99)                                                      \
                                                                            \
  product(bool, UseKNLSetting, false, DIAGNOSTIC,                           \
          "Control whether Knights platform setting should be used")        \
                                                                            \
  product(bool, UseCLMUL, false,                                            \
          "Control whether CLMUL instructions can be used on x86/x64")      \
                                                                            \
  product(bool, UseIncDec, true, DIAGNOSTIC,                                \
          "Use INC, DEC instructions on x86")                               \
                                                                            \
  product(bool, UseNewLongLShift, false,                                    \
          "Use optimized bitwise shift left")                               \
                                                                            \
  product(bool, UseAddressNop, false,                                       \
          "Use '0F 1F [addr]' NOP instructions on x86 cpus")                \
                                                                            \
  product(bool, UseXmmLoadAndClearUpper, true,                              \
          "Load low part of XMM register and clear upper part")             \
                                                                            \
  product(bool, UseXmmRegToRegMoveAll, false,                               \
          "Copy all XMM register bits when moving value between registers") \
                                                                            \
  product(bool, UseXmmI2D, false,                                           \
          "Use SSE2 CVTDQ2PD instruction to convert Integer to Double")     \
                                                                            \
  product(bool, UseXmmI2F, false,                                           \
          "Use SSE2 CVTDQ2PS instruction to convert Integer to Float")      \
                                                                            \
  product(bool, UseUnalignedLoadStores, false,                              \
          "Use SSE2 MOVDQU instruction for Arraycopy")                      \
                                                                            \
  product(bool, UseXMMForObjInit, false,                                    \
          "Use XMM/YMM MOVDQU instruction for Object Initialization")       \
                                                                            \
  product(bool, UseFastStosb, false,                                        \
          "Use fast-string operation for zeroing: rep stosb")               \
                                                                            \
  /* Use Restricted Transactional Memory for lock eliding */                \
  product(bool, UseRTMLocking, false,                                       \
          "Enable RTM lock eliding for inflated locks in compiled code")    \
                                                                            \
  product(bool, UseRTMForStackLocks, false, EXPERIMENTAL,                   \
          "Enable RTM lock eliding for stack locks in compiled code")       \
                                                                            \
  product(bool, UseRTMDeopt, false,                                         \
          "Perform deopt and recompilation based on RTM abort ratio")       \
                                                                            \
  product(int, RTMRetryCount, 5,                                            \
          "Number of RTM retries on lock abort or busy")                    \
          range(0, max_jint)                                                \
                                                                            \
  product(int, RTMSpinLoopCount, 100, EXPERIMENTAL,                         \
          "Spin count for lock to become free before RTM retry")            \
          range(0, max_jint)                                                \
                                                                            \
  product(int, RTMAbortThreshold, 1000, EXPERIMENTAL,                       \
          "Calculate abort ratio after this number of aborts")              \
          range(0, max_jint)                                                \
                                                                            \
  product(int, RTMLockingThreshold, 10000, EXPERIMENTAL,                    \
          "Lock count at which to do RTM lock eliding without "             \
          "abort ratio calculation")                                        \
          range(0, max_jint)                                                \
                                                                            \
  product(int, RTMAbortRatio, 50, EXPERIMENTAL,                             \
          "Lock abort ratio at which to stop use RTM lock eliding")         \
          range(0, 100) /* natural range */                                 \
                                                                            \
  product(int, RTMTotalCountIncrRate, 64, EXPERIMENTAL,                     \
          "Increment total RTM attempted lock count once every n times")    \
          range(1, max_jint)                                                \
          constraint(RTMTotalCountIncrRateConstraintFunc,AfterErgo)         \
                                                                            \
  product(intx, RTMLockingCalculationDelay, 0, EXPERIMENTAL,                \
          "Number of milliseconds to wait before start calculating aborts " \
          "for RTM locking")                                                \
                                                                            \
  product(bool, UseRTMXendForLockBusy, true, EXPERIMENTAL,                  \
          "Use RTM Xend instead of Xabort when lock busy")                  \
                                                                            \
  /* assembler */                                                           \
  product(bool, UseCountLeadingZerosInstruction, false,                     \
          "Use count leading zeros instruction")                            \
                                                                            \
  product(bool, UseCountTrailingZerosInstruction, false,                    \
          "Use count trailing zeros instruction")                           \
                                                                            \
  product(bool, UseSSE42Intrinsics, false,                                  \
          "SSE4.2 versions of intrinsics")                                  \
                                                                            \
  product(bool, UseBMI1Instructions, false,                                 \
          "Use BMI1 instructions")                                          \
                                                                            \
  product(bool, UseBMI2Instructions, false,                                 \
          "Use BMI2 instructions")                                          \
                                                                            \
  product(bool, UseLibmIntrinsic, true, DIAGNOSTIC,                         \
          "Use Libm Intrinsics")                                            \
                                                                            \
  /* Minimum array size in bytes to use AVX512 intrinsics */                \
  /* for copy, inflate and fill which don't bail out early based on any */  \
  /* condition. When this value is set to zero compare operations like */   \
  /* compare, vectorizedMismatch, compress can also use AVX512 intrinsics.*/\
  product(int, AVX3Threshold, 4096, DIAGNOSTIC,                             \
             "Minimum array size in bytes to use AVX512 intrinsics"         \
             "for copy, inflate and fill. When this value is set as zero"   \
             "compare operations can also use AVX512 intrinsics.")          \
             range(0, max_jint)                                             \
             constraint(AVX3ThresholdConstraintFunc,AfterErgo)              \
                                                                            \
  product(bool, IntelJccErratumMitigation, true, DIAGNOSTIC,                \
             "Turn off JVM mitigations related to Intel micro code "        \
             "mitigations for the Intel JCC erratum")

// end of ARCH_FLAGS

#endif // CPU_X86_GLOBALS_X86_HPP
