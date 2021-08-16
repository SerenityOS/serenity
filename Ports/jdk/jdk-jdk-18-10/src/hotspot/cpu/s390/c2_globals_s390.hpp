/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_S390_C2_GLOBALS_S390_HPP
#define CPU_S390_C2_GLOBALS_S390_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Sets the default values for platform dependent flags used by the server compiler.
// (see c2_globals.hpp).

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
define_pd_global(intx, ConditionalMoveLimit,         4);
define_pd_global(intx, FreqInlineSize,               175);
// 10 prevents spill-split-recycle sanity check in JVM2008.xml.transform.
define_pd_global(intx, InteriorEntryAlignment,       2);
define_pd_global(size_t, NewSizeThreadIncrease,      ScaleForWordSize(4*K));
define_pd_global(intx, RegisterCostAreaRatio,        12000);
define_pd_global(intx, LoopUnrollLimit,              60);
define_pd_global(intx, LoopPercentProfileLimit,      10);
define_pd_global(intx, MinJumpTableSize,             18);

// Peephole and CISC spilling both break the graph, and so make the
// scheduler sick.
define_pd_global(bool, OptoPeephole,                 false);
define_pd_global(bool, UseCISCSpill,                 true);
define_pd_global(bool, OptoBundling,                 false);
define_pd_global(bool, OptoScheduling,               false);
define_pd_global(bool, OptoRegScheduling,            false);
define_pd_global(bool, SuperWordLoopUnrollAnalysis,  false);
// On s390x, we can clear the array with a single instruction,
// so don't idealize it.
define_pd_global(bool, IdealizeClearArrayNode,       false);

// InitialCodeCacheSize derived from specjbb2000 run.
define_pd_global(uintx, InitialCodeCacheSize,        2048*K); // Integral multiple of CodeCacheExpansionSize
define_pd_global(uintx, ReservedCodeCacheSize,       48*M);
define_pd_global(uintx, NonProfiledCodeHeapSize,     21*M);
define_pd_global(uintx, ProfiledCodeHeapSize,        22*M);
define_pd_global(uintx, NonNMethodCodeHeapSize,      5*M);
define_pd_global(uintx, CodeCacheExpansionSize,      64*K);

// Ergonomics related flags
define_pd_global(uint64_t, MaxRAM,                   128ULL*G);
define_pd_global(uintx, CodeCacheMinBlockLength,     4);
define_pd_global(uintx, CodeCacheMinimumUseSpace,    400*K);

define_pd_global(bool,  TrapBasedRangeChecks,        false); // Not needed on z/Architecture.

// Ergonomics related flags
define_pd_global(bool, NeverActAsServerClassMachine, false);

#endif // CPU_S390_C2_GLOBALS_S390_HPP
