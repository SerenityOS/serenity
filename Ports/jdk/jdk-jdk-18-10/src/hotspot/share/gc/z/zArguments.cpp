/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/z/zAddressSpaceLimit.hpp"
#include "gc/z/zArguments.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zHeuristics.hpp"
#include "gc/shared/gcArguments.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"

void ZArguments::initialize_alignments() {
  SpaceAlignment = ZGranuleSize;
  HeapAlignment = SpaceAlignment;
}

void ZArguments::initialize() {
  GCArguments::initialize();

  // Check mark stack size
  const size_t mark_stack_space_limit = ZAddressSpaceLimit::mark_stack();
  if (ZMarkStackSpaceLimit > mark_stack_space_limit) {
    if (!FLAG_IS_DEFAULT(ZMarkStackSpaceLimit)) {
      vm_exit_during_initialization("ZMarkStackSpaceLimit too large for limited address space");
    }
    FLAG_SET_DEFAULT(ZMarkStackSpaceLimit, mark_stack_space_limit);
  }

  // Enable NUMA by default
  if (FLAG_IS_DEFAULT(UseNUMA)) {
    FLAG_SET_DEFAULT(UseNUMA, true);
  }

  // Select number of parallel threads
  if (FLAG_IS_DEFAULT(ParallelGCThreads)) {
    FLAG_SET_DEFAULT(ParallelGCThreads, ZHeuristics::nparallel_workers());
  }

  if (ParallelGCThreads == 0) {
    vm_exit_during_initialization("The flag -XX:+UseZGC can not be combined with -XX:ParallelGCThreads=0");
  }

  // Select number of concurrent threads
  if (FLAG_IS_DEFAULT(ConcGCThreads)) {
    FLAG_SET_DEFAULT(ConcGCThreads, ZHeuristics::nconcurrent_workers());
  }

  if (ConcGCThreads == 0) {
    vm_exit_during_initialization("The flag -XX:+UseZGC can not be combined with -XX:ConcGCThreads=0");
  }

  // The heuristics used when UseDynamicNumberOfGCThreads is
  // enabled defaults to using a ZAllocationSpikeTolerance of 1.
  if (UseDynamicNumberOfGCThreads && FLAG_IS_DEFAULT(ZAllocationSpikeTolerance)) {
    FLAG_SET_DEFAULT(ZAllocationSpikeTolerance, 1);
  }

#ifdef COMPILER2
  // Enable loop strip mining by default
  if (FLAG_IS_DEFAULT(UseCountedLoopSafepoints)) {
    FLAG_SET_DEFAULT(UseCountedLoopSafepoints, true);
    if (FLAG_IS_DEFAULT(LoopStripMiningIter)) {
      FLAG_SET_DEFAULT(LoopStripMiningIter, 1000);
    }
  }
#endif

  // CompressedOops not supported
  FLAG_SET_DEFAULT(UseCompressedOops, false);

  // Verification before startup and after exit not (yet) supported
  FLAG_SET_DEFAULT(VerifyDuringStartup, false);
  FLAG_SET_DEFAULT(VerifyBeforeExit, false);

  if (VerifyBeforeGC || VerifyDuringGC || VerifyAfterGC) {
    FLAG_SET_DEFAULT(ZVerifyRoots, true);
    FLAG_SET_DEFAULT(ZVerifyObjects, true);
  }
}

size_t ZArguments::heap_virtual_to_physical_ratio() {
  return ZHeapViews * ZVirtualToPhysicalRatio;
}

size_t ZArguments::conservative_max_heap_alignment() {
  return 0;
}

CollectedHeap* ZArguments::create_heap() {
  return new ZCollectedHeap();
}

bool ZArguments::is_supported() const {
  return is_os_supported();
}
