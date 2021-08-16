/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_GLOBALS_ARM_HPP
#define CPU_ARM_GLOBALS_ARM_HPP

//
// Sets the default values for platform dependent flags used by the runtime system.
// (see globals.hpp)
//

define_pd_global(bool,  ImplicitNullChecks,       true);  // Generate code for implicit null checks
define_pd_global(bool,  UncommonNullCast,         true);  // Uncommon-trap NULLs past to check cast
define_pd_global(bool,  TrapBasedNullChecks,      false); // Not needed

define_pd_global(uintx, CodeCacheSegmentSize, 64 COMPILER1_AND_COMPILER2_PRESENT(+64)); // Tiered compilation has large code-entry alignment.
define_pd_global(intx,  CodeEntryAlignment,       16);
define_pd_global(intx,  OptoLoopAlignment,        16);

#define DEFAULT_STACK_YELLOW_PAGES (2)
#define DEFAULT_STACK_RED_PAGES (1)
#define DEFAULT_STACK_SHADOW_PAGES (5 DEBUG_ONLY(+1))
#define DEFAULT_STACK_RESERVED_PAGES (0)

#define MIN_STACK_YELLOW_PAGES DEFAULT_STACK_YELLOW_PAGES
#define MIN_STACK_RED_PAGES    DEFAULT_STACK_RED_PAGES
#define MIN_STACK_SHADOW_PAGES DEFAULT_STACK_SHADOW_PAGES
#define MIN_STACK_RESERVED_PAGES (0)

define_pd_global(intx,  StackYellowPages,         DEFAULT_STACK_YELLOW_PAGES);
define_pd_global(intx,  StackRedPages,            DEFAULT_STACK_RED_PAGES);
define_pd_global(intx,  StackShadowPages,         DEFAULT_STACK_SHADOW_PAGES);
define_pd_global(intx,  StackReservedPages,       DEFAULT_STACK_RESERVED_PAGES);

define_pd_global(intx,  InlineFrequencyCount,     50);
#if  defined(COMPILER1) || defined(COMPILER2)
define_pd_global(intx,  InlineSmallCode,          1500);
#endif

define_pd_global(bool,  RewriteBytecodes,         true);
define_pd_global(bool,  RewriteFrequentPairs,     true);

define_pd_global(bool,  PreserveFramePointer,     false);

define_pd_global(uintx, TypeProfileLevel, 0);

// No performance work done here yet.
define_pd_global(bool, CompactStrings, false);

define_pd_global(intx, InitArrayShortSize, 8*BytesPerLong);

#define ARCH_FLAGS(develop,     \
                   product,     \
                   notproduct,  \
                   range,       \
                   constraint)

// end of ARCH_FLAGS

#endif // CPU_ARM_GLOBALS_ARM_HPP
