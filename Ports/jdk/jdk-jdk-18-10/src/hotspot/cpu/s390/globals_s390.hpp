/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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

#ifndef CPU_S390_GLOBALS_S390_HPP
#define CPU_S390_GLOBALS_S390_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Sets the default values for platform dependent flags used by the runtime system.
// (see globals.hpp)

define_pd_global(bool,  ImplicitNullChecks,          true);  // Generate code for implicit null checks.
define_pd_global(bool,  TrapBasedNullChecks,         true);
define_pd_global(bool,  UncommonNullCast,            true);  // Uncommon-trap NULLs passed to check cast.

define_pd_global(uintx, CodeCacheSegmentSize,        256);
// This shall be at least 32 for proper branch target alignment.
// Ideally, this is 256 (cache line size). This keeps code end data
// on separate lines. But we reduced it to 64 since 256 increased
// code size significantly by padding nops between IVC and second UEP.
define_pd_global(intx,  CodeEntryAlignment,          64);
define_pd_global(intx,  OptoLoopAlignment,           2);
define_pd_global(intx,  InlineFrequencyCount,        100);
define_pd_global(intx,  InlineSmallCode,             2000);

#define DEFAULT_STACK_YELLOW_PAGES   (2)
#define DEFAULT_STACK_RED_PAGES      (1)
// Java_java_net_SocketOutputStream_socketWrite0() uses a 64k buffer on the
// stack. To pass stack overflow tests we need 20 shadow pages.
#define DEFAULT_STACK_SHADOW_PAGES   (20 DEBUG_ONLY(+4))
#define DEFAULT_STACK_RESERVED_PAGES (1)

#define MIN_STACK_YELLOW_PAGES     DEFAULT_STACK_YELLOW_PAGES
#define MIN_STACK_RED_PAGES        DEFAULT_STACK_RED_PAGES
#define MIN_STACK_SHADOW_PAGES     DEFAULT_STACK_SHADOW_PAGES
#define MIN_STACK_RESERVED_PAGES   (0)

define_pd_global(intx,  StackYellowPages,            DEFAULT_STACK_YELLOW_PAGES);
define_pd_global(intx,  StackRedPages,               DEFAULT_STACK_RED_PAGES);
define_pd_global(intx,  StackShadowPages,            DEFAULT_STACK_SHADOW_PAGES);
define_pd_global(intx,  StackReservedPages,          DEFAULT_STACK_RESERVED_PAGES);

define_pd_global(bool, RewriteBytecodes,     true);
define_pd_global(bool, RewriteFrequentPairs, true);

define_pd_global(bool, PreserveFramePointer, false);

define_pd_global(uintx, TypeProfileLevel, 111);

define_pd_global(bool, CompactStrings, true);

// 8146801 (Short Array Allocation): No performance work done here yet.
define_pd_global(intx, InitArrayShortSize, 1*BytesPerLong);

#define ARCH_FLAGS(develop,                                                   \
                   product,                                                   \
                   notproduct,                                                \
                   range,                                                     \
                   constraint)                                                \
                                                                              \
  /* Reoptimize code-sequences of calls at runtime, e.g. replace an */        \
  /* indirect call by a direct call.                                */        \
  product(bool, ReoptimizeCallSequences, true, DIAGNOSTIC,                    \
          "Reoptimize code-sequences of calls at runtime.")                   \
                                                                              \
  product(bool, UseByteReverseInstruction, true, DIAGNOSTIC,                  \
          "Use byte reverse instruction.")                                    \
                                                                              \
  product(bool, ExpandLoadingBaseDecode, true, DIAGNOSTIC,                    \
          "Expand the assembler instruction required to load the base from "  \
          "DecodeN nodes during matching.")                                   \
  product(bool, ExpandLoadingBaseDecode_NN, true, DIAGNOSTIC,                 \
          "Expand the assembler instruction required to load the base from "  \
          "DecodeN_NN nodes during matching.")                                \
  product(bool, ExpandLoadingBaseEncode, true, DIAGNOSTIC,                    \
          "Expand the assembler instruction required to load the base from "  \
          "EncodeP nodes during matching.")                                   \
  product(bool, ExpandLoadingBaseEncode_NN, true, DIAGNOSTIC,                 \
          "Expand the assembler instruction required to load the base from "  \
          "EncodeP_NN nodes during matching.")                                \
                                                                              \
  /* Seems to pay off with 2 pages already. */                                \
  product(size_t, MVCLEThreshold, +2*(4*K), DIAGNOSTIC,                       \
          "Threshold above which page-aligned MVCLE copy/init is used.")      \
                                                                              \
  product(bool, PreferLAoverADD, false, DIAGNOSTIC,                           \
          "Use LA/LAY instructions over ADD instructions (z/Architecture).")  \
                                                                              \
  develop(bool, ZapEmptyStackFields, false, "Write 0x0101... to empty stack"  \
          " fields. Use this to ease stack debugging.")                       \
                                                                              \
  product(bool, TraceTraps, false, DIAGNOSTIC,                                \
          "Trace all traps the signal handler handles.")

// end of ARCH_FLAGS

#endif // CPU_S390_GLOBALS_S390_HPP
