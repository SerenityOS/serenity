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

#ifndef CPU_X86_C2_GLOBALS_X86_HPP
#define CPU_X86_C2_GLOBALS_X86_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Sets the default values for platform dependent flags used by the server compiler.
// (see c2_globals.hpp).  Alpha-sorted.
define_pd_global(bool, BackgroundCompilation,        true);
define_pd_global(bool, CICompileOSR,                 true);
define_pd_global(bool, InlineIntrinsics,             true);
define_pd_global(bool, PreferInterpreterNativeStubs, false);
define_pd_global(bool, ProfileTraps,                 true);
define_pd_global(bool, UseOnStackReplacement,        true);
define_pd_global(bool, ProfileInterpreter,           true);
define_pd_global(bool, TieredCompilation,            COMPILER1_PRESENT(true) NOT_COMPILER1(false));
define_pd_global(intx, CompileThreshold,             10000);

define_pd_global(intx, OnStackReplacePercentage,     140);
define_pd_global(intx, ConditionalMoveLimit,         3);
define_pd_global(intx, FreqInlineSize,               325);
define_pd_global(intx, MinJumpTableSize,             10);
define_pd_global(intx, LoopPercentProfileLimit,      30);
#ifdef AMD64
define_pd_global(intx,  InteriorEntryAlignment,      16);
define_pd_global(size_t, NewSizeThreadIncrease,     ScaleForWordSize(4*K));
define_pd_global(intx,  LoopUnrollLimit,             60);
// InitialCodeCacheSize derived from specjbb2000 run.
define_pd_global(uintx, InitialCodeCacheSize,        2496*K); // Integral multiple of CodeCacheExpansionSize
define_pd_global(uintx, CodeCacheExpansionSize,      64*K);

// Ergonomics related flags
define_pd_global(uint64_t, MaxRAM,                   128ULL*G);
#else
define_pd_global(intx,  InteriorEntryAlignment,      4);
define_pd_global(size_t, NewSizeThreadIncrease,      4*K);
define_pd_global(intx,  LoopUnrollLimit,             50);     // Design center runs on 1.3.1
// InitialCodeCacheSize derived from specjbb2000 run.
define_pd_global(uintx, InitialCodeCacheSize,        2304*K); // Integral multiple of CodeCacheExpansionSize
define_pd_global(uintx, CodeCacheExpansionSize,      32*K);

// Ergonomics related flags
define_pd_global(uint64_t, MaxRAM,                   4ULL*G);
#endif // AMD64
define_pd_global(intx, RegisterCostAreaRatio,        16000);

// Peephole and CISC spilling both break the graph, and so makes the
// scheduler sick.
define_pd_global(bool, OptoPeephole,                 true);
define_pd_global(bool, UseCISCSpill,                 true);
define_pd_global(bool, OptoScheduling,               false);
define_pd_global(bool, OptoBundling,                 false);
define_pd_global(bool, OptoRegScheduling,            true);
define_pd_global(bool, SuperWordLoopUnrollAnalysis,  true);
define_pd_global(bool, IdealizeClearArrayNode,       true);

define_pd_global(uintx, ReservedCodeCacheSize,       48*M);
define_pd_global(uintx, NonProfiledCodeHeapSize,     21*M);
define_pd_global(uintx, ProfiledCodeHeapSize,        22*M);
define_pd_global(uintx, NonNMethodCodeHeapSize,      5*M );
define_pd_global(uintx, CodeCacheMinBlockLength,     6);
define_pd_global(uintx, CodeCacheMinimumUseSpace,    400*K);

define_pd_global(bool,  TrapBasedRangeChecks,        false); // Not needed on x86.

// Ergonomics related flags
define_pd_global(bool, NeverActAsServerClassMachine, false);

#endif // CPU_X86_C2_GLOBALS_X86_HPP
