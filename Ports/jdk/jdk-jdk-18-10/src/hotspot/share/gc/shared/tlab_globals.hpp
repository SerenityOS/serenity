/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_TLAB_GLOBALS_HPP
#define SHARE_GC_SHARED_TLAB_GLOBALS_HPP

#include "runtime/globals_shared.hpp"

#define TLAB_FLAGS(develop,                                                 \
                   develop_pd,                                              \
                   product,                                                 \
                   product_pd,                                              \
                   notproduct,                                              \
                   range,                                                   \
                   constraint)                                              \
                                                                            \
  /* Thread Local Allocation Buffer */                                      \
                                                                            \
  product(bool, UseTLAB, true,                                              \
          "Use thread-local object allocation")                             \
                                                                            \
  product(bool, ResizeTLAB, true,                                           \
          "Dynamically resize TLAB size for threads")                       \
                                                                            \
  product(bool, ZeroTLAB, false,                                            \
          "Zero out the newly created TLAB")                                \
                                                                            \
  product(bool, TLABStats, true,                                            \
          "Provide more detailed and expensive TLAB statistics.")           \
                                                                            \
  product(size_t, MinTLABSize, 2*K,                                         \
          "Minimum allowed TLAB size (in bytes)")                           \
          range(1, max_uintx/2)                                             \
          constraint(MinTLABSizeConstraintFunc,AfterMemoryInit)             \
                                                                            \
  product(size_t, TLABSize, 0,                                              \
          "Starting TLAB size (in bytes); zero means set ergonomically")    \
          constraint(TLABSizeConstraintFunc,AfterMemoryInit)                \
                                                                            \
  product(size_t, YoungPLABSize, 4096,                                      \
          "Size of young gen promotion LAB's (in HeapWords)")               \
          constraint(YoungPLABSizeConstraintFunc,AfterMemoryInit)           \
                                                                            \
  product(size_t, OldPLABSize, 1024,                                        \
          "Size of old gen promotion LAB's (in HeapWords)")                 \
          constraint(OldPLABSizeConstraintFunc,AfterMemoryInit)             \
                                                                            \
  product(uintx, TLABAllocationWeight, 35,                                  \
          "Allocation averaging weight")                                    \
          range(0, 100)                                                     \
                                                                            \
  /* Limit the lower bound of this flag to 1 as it is used  */              \
  /* in a division expression.                              */              \
  product(uintx, TLABWasteTargetPercent, 1,                                 \
          "Percentage of Eden that can be wasted")                          \
          range(1, 100)                                                     \
                                                                            \
  product(uintx, TLABRefillWasteFraction,    64,                            \
          "Maximum TLAB waste at a refill (internal fragmentation)")        \
          range(1, max_juint)                                               \
                                                                            \
  product(uintx, TLABWasteIncrement,    4,                                  \
          "Increment allowed waste at slow allocation")                     \
          range(0, max_jint)                                                \
          constraint(TLABWasteIncrementConstraintFunc,AfterMemoryInit)      \
                                                                            \

// end of TLAB_FLAGS

DECLARE_FLAGS(TLAB_FLAGS)

#endif // SHARE_GC_SHARED_TLAB_GLOBALS_HPP
