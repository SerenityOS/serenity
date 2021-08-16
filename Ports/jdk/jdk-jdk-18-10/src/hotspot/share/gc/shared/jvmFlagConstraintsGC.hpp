/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_JVMFLAGCONSTRAINTSGC_HPP
#define SHARE_GC_SHARED_JVMFLAGCONSTRAINTSGC_HPP

#include "runtime/flags/jvmFlag.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_G1GC
#include "gc/g1/jvmFlagConstraintsG1.hpp"
#endif
#if INCLUDE_PARALLELGC
#include "gc/parallel/jvmFlagConstraintsParallel.hpp"
#endif

/*
 * Here we have GC arguments constraints functions, which are called automatically
 * whenever flag's value changes. If the constraint fails the function should return
 * an appropriate error value.
 */
#define SHARED_GC_CONSTRAINTS(f)                               \
 f(uint,   ParallelGCThreadsConstraintFunc)                    \
 f(size_t, YoungPLABSizeConstraintFunc)                        \
 f(size_t, OldPLABSizeConstraintFunc)                          \
 f(uintx,  MinHeapFreeRatioConstraintFunc)                     \
 f(uintx,  MaxHeapFreeRatioConstraintFunc)                     \
 f(intx,   SoftRefLRUPolicyMSPerMBConstraintFunc)              \
 f(size_t, MarkStackSizeConstraintFunc)                        \
 f(uintx,  MinMetaspaceFreeRatioConstraintFunc)                \
 f(uintx,  MaxMetaspaceFreeRatioConstraintFunc)                \
 f(uintx,  InitialTenuringThresholdConstraintFunc)             \
 f(uintx,  MaxTenuringThresholdConstraintFunc)                 \
                                                               \
 f(uintx,  MaxGCPauseMillisConstraintFunc)                     \
 f(uintx,  GCPauseIntervalMillisConstraintFunc)                \
 f(size_t, MinHeapSizeConstraintFunc)                          \
 f(size_t, InitialHeapSizeConstraintFunc)                      \
 f(size_t, MaxHeapSizeConstraintFunc)                          \
 f(size_t, SoftMaxHeapSizeConstraintFunc)                      \
 f(size_t, HeapBaseMinAddressConstraintFunc)                   \
 f(size_t, NewSizeConstraintFunc)                              \
 f(size_t, MinTLABSizeConstraintFunc)                          \
 f(size_t, TLABSizeConstraintFunc)                             \
 f(uintx,  TLABWasteIncrementConstraintFunc)                   \
 f(uintx,  SurvivorRatioConstraintFunc)                        \
 f(size_t, MetaspaceSizeConstraintFunc)                        \
 f(size_t, MaxMetaspaceSizeConstraintFunc)

SHARED_GC_CONSTRAINTS(DECLARE_CONSTRAINT)

JVMFlag::Error MaxPLABSizeBounds(const char* name, size_t value, bool verbose);

#define GC_CONSTRAINTS(f)                      \
  SHARED_GC_CONSTRAINTS(f)                     \
  G1GC_ONLY(G1_GC_CONSTRAINTS(f))              \
  PARALLELGC_ONLY(PARALLEL_GC_CONSTRAINTS(f))

#endif // SHARE_GC_SHARED_JVMFLAGCONSTRAINTSGC_HPP
