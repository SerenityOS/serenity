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

#ifndef CPU_ARM_C1_GLOBALS_ARM_HPP
#define CPU_ARM_C1_GLOBALS_ARM_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

//
// Sets the default values for platform dependent flags used by the client compiler.
// (see c1_globals.hpp)
//

#ifndef COMPILER2 // avoid duplicated definitions, favoring C2 version
define_pd_global(bool, BackgroundCompilation,        true );
define_pd_global(bool, InlineIntrinsics,             true );
define_pd_global(bool, PreferInterpreterNativeStubs, false);
define_pd_global(bool, ProfileTraps,                 false);
define_pd_global(bool, UseOnStackReplacement,        true );
define_pd_global(bool, TieredCompilation,            false);
define_pd_global(intx, CompileThreshold,             1500 );

define_pd_global(intx, OnStackReplacePercentage,     933  );
define_pd_global(size_t, NewSizeThreadIncrease,      4*K  );
define_pd_global(size_t, InitialCodeCacheSize,       160*K);
define_pd_global(size_t, ReservedCodeCacheSize,      32*M );
define_pd_global(size_t, NonProfiledCodeHeapSize,    13*M );
define_pd_global(size_t, ProfiledCodeHeapSize,       14*M );
define_pd_global(size_t, NonNMethodCodeHeapSize,     5*M  );
define_pd_global(bool, ProfileInterpreter,           false);
define_pd_global(size_t, CodeCacheExpansionSize,     32*K );
define_pd_global(uintx, CodeCacheMinBlockLength,     1);
define_pd_global(size_t, CodeCacheMinimumUseSpace,   400*K);
define_pd_global(bool, NeverActAsServerClassMachine, true);
define_pd_global(uint64_t, MaxRAM,                   1ULL*G);
define_pd_global(bool, CICompileOSR,                 true );
#endif // COMPILER2
define_pd_global(bool, UseTypeProfile,               false);

define_pd_global(bool, OptimizeSinglePrecision,      true);
define_pd_global(bool, CSEArrayLength,               true);
define_pd_global(bool, TwoOperandLIRForm,            false);

#endif // CPU_ARM_C1_GLOBALS_ARM_HPP
