/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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

#ifndef CPU_ZERO_GLOBALS_ZERO_HPP
#define CPU_ZERO_GLOBALS_ZERO_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Set the default values for platform dependent flags used by the
// runtime system.  See globals.hpp for details of what they do.

define_pd_global(bool,  ImplicitNullChecks,   true);
define_pd_global(bool,  TrapBasedNullChecks,  false);
define_pd_global(bool,  UncommonNullCast,     true);

define_pd_global(uintx, CodeCacheSegmentSize, 64 COMPILER1_AND_COMPILER2_PRESENT(+64)); // Tiered compilation has large code-entry alignment.
define_pd_global(intx,  CodeEntryAlignment,   32);
define_pd_global(intx,  OptoLoopAlignment,    16);
define_pd_global(intx,  InlineFrequencyCount, 100);
define_pd_global(intx,  InlineSmallCode,      1000);

// not used, but must satisfy following constraints:
// 1.) <VALUE> must be in the allowed range for intx *and*
// 2.) <VALUE> % BytesPerLong == 0 so as to not
//     violate the constraint verifier on JVM start-up.
define_pd_global(intx,  InitArrayShortSize,   0);

#define DEFAULT_STACK_YELLOW_PAGES (2)
#define DEFAULT_STACK_RED_PAGES (1)
#define DEFAULT_STACK_SHADOW_PAGES (5 LP64_ONLY(+1) DEBUG_ONLY(+3))
#define DEFAULT_STACK_RESERVED_PAGES (0)

#define MIN_STACK_YELLOW_PAGES DEFAULT_STACK_YELLOW_PAGES
#define MIN_STACK_RED_PAGES DEFAULT_STACK_RED_PAGES
#define MIN_STACK_SHADOW_PAGES DEFAULT_STACK_SHADOW_PAGES
#define MIN_STACK_RESERVED_PAGES (0)

define_pd_global(intx,  StackYellowPages,     DEFAULT_STACK_YELLOW_PAGES);
define_pd_global(intx,  StackRedPages,        DEFAULT_STACK_RED_PAGES);
define_pd_global(intx,  StackShadowPages,     DEFAULT_STACK_SHADOW_PAGES);
define_pd_global(intx,  StackReservedPages,   DEFAULT_STACK_RESERVED_PAGES);

define_pd_global(bool,  RewriteBytecodes,     true);
define_pd_global(bool,  RewriteFrequentPairs, true);

define_pd_global(uintx, TypeProfileLevel, 0);

define_pd_global(bool, PreserveFramePointer, false);

// No performance work done here yet.
define_pd_global(bool, CompactStrings, false);

#define ARCH_FLAGS(develop,                                                 \
                   product,                                                 \
                   notproduct,                                              \
                   range,                                                   \
                   constraint)                                              \
                                                                            \
  product(bool, UseFastEmptyMethods, true,                                  \
          "Use fast method entry code for empty methods")                   \
                                                                            \
  product(bool, UseFastAccessorMethods, true,                               \
          "Use fast method entry code for accessor methods")

// end of ARCH_FLAGS

#endif // CPU_ZERO_GLOBALS_ZERO_HPP
