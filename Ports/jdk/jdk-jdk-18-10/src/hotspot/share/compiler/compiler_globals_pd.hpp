/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILER_GLOBALS_PD_HPP
#define SHARE_COMPILER_COMPILER_GLOBALS_PD_HPP

// Platform-specific Default values for VM flags used by the compiler.
//
// Note: for historical reasons, some of these flags are declared in globals.hpp.
// E.g., BackgroundCompilation. Such declarations should be moved to this
// file instead.

#include "runtime/globals_shared.hpp"
#ifdef COMPILER1
#include "c1/c1_globals_pd.hpp"
#endif // COMPILER1
#ifdef COMPILER2
#include "opto/c2_globals_pd.hpp"
#endif // COMPILER2

// JVMCI has no platform-specific global definitions
//#if INCLUDE_JVMCI
//#include "jvmci/jvmci_globals_pd.hpp"
//#endif

#if !defined(COMPILER1) && !defined(COMPILER2) && !INCLUDE_JVMCI
define_pd_global(bool, BackgroundCompilation,        false);
define_pd_global(bool, CICompileOSR,                 false);
define_pd_global(bool, UseTypeProfile,               false);
define_pd_global(bool, UseOnStackReplacement,        false);
define_pd_global(bool, InlineIntrinsics,             false);
define_pd_global(bool, PreferInterpreterNativeStubs, true);
define_pd_global(bool, ProfileInterpreter,           false);
define_pd_global(bool, ProfileTraps,                 false);
define_pd_global(bool, TieredCompilation,            false);

define_pd_global(intx, CompileThreshold,             0);

define_pd_global(intx,   OnStackReplacePercentage,   0);
define_pd_global(size_t, NewSizeThreadIncrease,      4*K);
define_pd_global(bool,   InlineClassNatives,         true);
define_pd_global(bool,   InlineUnsafeOps,            true);
define_pd_global(uintx,  InitialCodeCacheSize,       160*K);
define_pd_global(uintx,  ReservedCodeCacheSize,      32*M);
define_pd_global(uintx,  NonProfiledCodeHeapSize,    0);
define_pd_global(uintx,  ProfiledCodeHeapSize,       0);
define_pd_global(uintx,  NonNMethodCodeHeapSize,     32*M);

define_pd_global(uintx,  CodeCacheExpansionSize,     32*K);
define_pd_global(uintx,  CodeCacheMinBlockLength,    1);
define_pd_global(uintx,  CodeCacheMinimumUseSpace,   200*K);
define_pd_global(bool, NeverActAsServerClassMachine, true);
define_pd_global(uint64_t,MaxRAM,                    1ULL*G);
#define CI_COMPILER_COUNT 0
#else

#if COMPILER2_OR_JVMCI
#define CI_COMPILER_COUNT 2
#else
#define CI_COMPILER_COUNT 1
#endif // COMPILER2_OR_JVMCI

#endif // no compilers

#endif // SHARE_COMPILER_COMPILER_GLOBALS_PD_HPP
