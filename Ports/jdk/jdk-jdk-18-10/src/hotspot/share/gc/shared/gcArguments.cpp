/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, Red Hat, Inc. and/or its affiliates.
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

#include "precompiled.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/gcArguments.hpp"
#include "logging/log.hpp"
#include "runtime/arguments.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/macros.hpp"

size_t HeapAlignment = 0;
size_t SpaceAlignment = 0;

void GCArguments::initialize() {
  if (FullGCALot && FLAG_IS_DEFAULT(MarkSweepAlwaysCompactCount)) {
    MarkSweepAlwaysCompactCount = 1;  // Move objects every gc.
  }

  if (!UseParallelGC && FLAG_IS_DEFAULT(ScavengeBeforeFullGC)) {
    FLAG_SET_DEFAULT(ScavengeBeforeFullGC, false);
  }

  if (GCTimeLimit == 100) {
    // Turn off gc-overhead-limit-exceeded checks
    FLAG_SET_DEFAULT(UseGCOverheadLimit, false);
  }

  if (MinHeapFreeRatio == 100) {
    // Keeping the heap 100% free is hard ;-) so limit it to 99%.
    FLAG_SET_ERGO(MinHeapFreeRatio, 99);
  }

  if (!ClassUnloading) {
    // If class unloading is disabled, also disable concurrent class unloading.
    FLAG_SET_CMDLINE(ClassUnloadingWithConcurrentMark, false);
  }
}

void GCArguments::initialize_heap_sizes() {
  initialize_alignments();
  initialize_heap_flags_and_sizes();
  initialize_size_info();
}

size_t GCArguments::compute_heap_alignment() {
  // The card marking array and the offset arrays for old generations are
  // committed in os pages as well. Make sure they are entirely full (to
  // avoid partial page problems), e.g. if 512 bytes heap corresponds to 1
  // byte entry and the os page size is 4096, the maximum heap size should
  // be 512*4096 = 2MB aligned.

  size_t alignment = CardTableRS::ct_max_alignment_constraint();

  if (UseLargePages) {
      // In presence of large pages we have to make sure that our
      // alignment is large page aware.
      alignment = lcm(os::large_page_size(), alignment);
  }

  return alignment;
}

#ifdef ASSERT
void GCArguments::assert_flags() {
  assert(InitialHeapSize <= MaxHeapSize, "Ergonomics decided on incompatible initial and maximum heap sizes");
  assert(InitialHeapSize % HeapAlignment == 0, "InitialHeapSize alignment");
  assert(MaxHeapSize % HeapAlignment == 0, "MaxHeapSize alignment");
}

void GCArguments::assert_size_info() {
  assert(MaxHeapSize >= MinHeapSize, "Ergonomics decided on incompatible minimum and maximum heap sizes");
  assert(InitialHeapSize >= MinHeapSize, "Ergonomics decided on incompatible initial and minimum heap sizes");
  assert(MaxHeapSize >= InitialHeapSize, "Ergonomics decided on incompatible initial and maximum heap sizes");
  assert(MinHeapSize % HeapAlignment == 0, "MinHeapSize alignment");
  assert(InitialHeapSize % HeapAlignment == 0, "InitialHeapSize alignment");
  assert(MaxHeapSize % HeapAlignment == 0, "MaxHeapSize alignment");
}
#endif // ASSERT

void GCArguments::initialize_size_info() {
  log_debug(gc, heap)("Minimum heap " SIZE_FORMAT "  Initial heap " SIZE_FORMAT "  Maximum heap " SIZE_FORMAT,
                      MinHeapSize, InitialHeapSize, MaxHeapSize);

  DEBUG_ONLY(assert_size_info();)
}

void GCArguments::initialize_heap_flags_and_sizes() {
  assert(SpaceAlignment != 0, "Space alignment not set up properly");
  assert(HeapAlignment != 0, "Heap alignment not set up properly");
  assert(HeapAlignment >= SpaceAlignment,
         "HeapAlignment: " SIZE_FORMAT " less than SpaceAlignment: " SIZE_FORMAT,
         HeapAlignment, SpaceAlignment);
  assert(HeapAlignment % SpaceAlignment == 0,
         "HeapAlignment: " SIZE_FORMAT " not aligned by SpaceAlignment: " SIZE_FORMAT,
         HeapAlignment, SpaceAlignment);

  if (FLAG_IS_CMDLINE(MaxHeapSize)) {
    if (FLAG_IS_CMDLINE(InitialHeapSize) && InitialHeapSize > MaxHeapSize) {
      vm_exit_during_initialization("Initial heap size set to a larger value than the maximum heap size");
    }
    if (FLAG_IS_CMDLINE(MinHeapSize) && MaxHeapSize < MinHeapSize) {
      vm_exit_during_initialization("Incompatible minimum and maximum heap sizes specified");
    }
  }

  // Check heap parameter properties
  if (MaxHeapSize < 2 * M) {
    vm_exit_during_initialization("Too small maximum heap");
  }
  if (InitialHeapSize < M) {
    vm_exit_during_initialization("Too small initial heap");
  }
  if (MinHeapSize < M) {
    vm_exit_during_initialization("Too small minimum heap");
  }

  // User inputs from -Xmx and -Xms must be aligned
  // Write back to flags if the values changed
  if (!is_aligned(MinHeapSize, HeapAlignment)) {
    FLAG_SET_ERGO(MinHeapSize, align_up(MinHeapSize, HeapAlignment));
  }
  if (!is_aligned(InitialHeapSize, HeapAlignment)) {
    FLAG_SET_ERGO(InitialHeapSize, align_up(InitialHeapSize, HeapAlignment));
  }
  if (!is_aligned(MaxHeapSize, HeapAlignment)) {
    FLAG_SET_ERGO(MaxHeapSize, align_up(MaxHeapSize, HeapAlignment));
  }

  if (FLAG_IS_CMDLINE(InitialHeapSize) && FLAG_IS_CMDLINE(MinHeapSize) &&
      InitialHeapSize < MinHeapSize) {
    vm_exit_during_initialization("Incompatible minimum and initial heap sizes specified");
  }

  if (!FLAG_IS_DEFAULT(InitialHeapSize) && InitialHeapSize > MaxHeapSize) {
    FLAG_SET_ERGO(MaxHeapSize, InitialHeapSize);
  } else if (!FLAG_IS_DEFAULT(MaxHeapSize) && InitialHeapSize > MaxHeapSize) {
    FLAG_SET_ERGO(InitialHeapSize, MaxHeapSize);
    if (InitialHeapSize < MinHeapSize) {
      FLAG_SET_ERGO(MinHeapSize, InitialHeapSize);
    }
  }

  if (FLAG_IS_DEFAULT(SoftMaxHeapSize)) {
    FLAG_SET_ERGO(SoftMaxHeapSize, MaxHeapSize);
  }

  FLAG_SET_ERGO(MinHeapDeltaBytes, align_up(MinHeapDeltaBytes, SpaceAlignment));

  DEBUG_ONLY(assert_flags();)
}

size_t GCArguments::heap_virtual_to_physical_ratio() {
  return 1;
}
