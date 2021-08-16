/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_C2_GLOBALS_ARM_HPP
#define CPU_ARM_C2_GLOBALS_ARM_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

//
// Sets the default values for platform dependent flags used by the server compiler.
// (see c2_globals.hpp).  Alpha-sorted.

define_pd_global(bool, BackgroundCompilation,        true);
define_pd_global(bool, CICompileOSR,                 true);
define_pd_global(bool, InlineIntrinsics,             true);
define_pd_global(bool, PreferInterpreterNativeStubs, false);
define_pd_global(bool, ProfileTraps,                 true);
define_pd_global(bool, UseOnStackReplacement,        true);
define_pd_global(bool, ProfileInterpreter,           true);
define_pd_global(bool, TieredCompilation,            false);
define_pd_global(intx, CompileThreshold,             10000);

define_pd_global(intx, OnStackReplacePercentage,     140);
define_pd_global(intx, ConditionalMoveLimit,         4);
// C2 gets to use all the float/double registers
define_pd_global(intx, FreqInlineSize,               175);
define_pd_global(intx, InteriorEntryAlignment,       16);  // = CodeEntryAlignment
define_pd_global(size_t, NewSizeThreadIncrease,      ScaleForWordSize(4*K));
// The default setting 16/16 seems to work best.
// (For _228_jack 16/16 is 2% better than 4/4, 16/4, 32/32, 32/16, or 16/32.)
//define_pd_global(intx, OptoLoopAlignment,            16);  // = 4*wordSize
define_pd_global(intx, RegisterCostAreaRatio,        16000);
define_pd_global(intx, LoopUnrollLimit,              60); // Design center runs on 1.3.1
define_pd_global(intx, LoopPercentProfileLimit,      10);
define_pd_global(intx, MinJumpTableSize,             16);

// Peephole and CISC spilling both break the graph, and so makes the
// scheduler sick.
define_pd_global(bool, OptoPeephole,                 false);
define_pd_global(bool, UseCISCSpill,                 false);
define_pd_global(bool, OptoBundling,                 false);
define_pd_global(bool, OptoScheduling,               true);
define_pd_global(bool, OptoRegScheduling,            false);
define_pd_global(bool, SuperWordLoopUnrollAnalysis,  false);
define_pd_global(bool, IdealizeClearArrayNode,       true);

#ifdef _LP64
// We need to make sure that all generated code is within
// 2 gigs of the libjvm.so runtime routines so we can use
// the faster "call" instruction rather than the expensive
// sequence of instructions to load a 64 bit pointer.
//
// InitialCodeCacheSize derived from specjbb2000 run.
define_pd_global(size_t, InitialCodeCacheSize,       2048*K); // Integral multiple of CodeCacheExpansionSize
define_pd_global(size_t, ReservedCodeCacheSize,      48*M);
define_pd_global(size_t, NonProfiledCodeHeapSize,    21*M);
define_pd_global(size_t, ProfiledCodeHeapSize,       22*M);
define_pd_global(size_t, NonNMethodCodeHeapSize,     5*M );
define_pd_global(size_t, CodeCacheExpansionSize,     64*K);

// Ergonomics related flags
define_pd_global(uint64_t, MaxRAM,                   128ULL*G);
#else
// InitialCodeCacheSize derived from specjbb2000 run.
define_pd_global(size_t, InitialCodeCacheSize,       1536*K); // Integral multiple of CodeCacheExpansionSize
define_pd_global(size_t, ReservedCodeCacheSize,      32*M);
define_pd_global(size_t, NonProfiledCodeHeapSize,    13*M);
define_pd_global(size_t, ProfiledCodeHeapSize,       14*M);
define_pd_global(size_t, NonNMethodCodeHeapSize,     5*M );
define_pd_global(size_t, CodeCacheExpansionSize,     32*K);
// Ergonomics related flags
define_pd_global(uint64_t, MaxRAM,                   4ULL*G);
#endif
define_pd_global(uintx, CodeCacheMinBlockLength,     6);
define_pd_global(size_t, CodeCacheMinimumUseSpace,   400*K);

define_pd_global(bool,  TrapBasedRangeChecks,        false); // Not needed

// Ergonomics related flags
define_pd_global(bool, NeverActAsServerClassMachine, false);

#endif // CPU_ARM_C2_GLOBALS_ARM_HPP
