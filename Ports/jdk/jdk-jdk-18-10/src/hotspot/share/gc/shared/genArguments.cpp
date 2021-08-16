/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/genArguments.hpp"
#include "gc/shared/generation.hpp"
#include "logging/log.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

size_t MinNewSize = 0;

size_t MinOldSize = 0;
size_t MaxOldSize = 0;

size_t GenAlignment = 0;

size_t GenArguments::conservative_max_heap_alignment() { return (size_t)Generation::GenGrain; }

static size_t young_gen_size_lower_bound() {
  // The young generation must be aligned and have room for eden + two survivors
  return align_up(3 * SpaceAlignment, GenAlignment);
}

static size_t old_gen_size_lower_bound() {
  return align_up(SpaceAlignment, GenAlignment);
}

size_t GenArguments::scale_by_NewRatio_aligned(size_t base_size, size_t alignment) {
  return align_down_bounded(base_size / (NewRatio + 1), alignment);
}

static size_t bound_minus_alignment(size_t desired_size,
                                    size_t maximum_size,
                                    size_t alignment) {
  size_t max_minus = maximum_size - alignment;
  return desired_size < max_minus ? desired_size : max_minus;
}

void GenArguments::initialize_alignments() {
  SpaceAlignment = GenAlignment = (size_t)Generation::GenGrain;
  HeapAlignment = compute_heap_alignment();
}

void GenArguments::initialize_heap_flags_and_sizes() {
  GCArguments::initialize_heap_flags_and_sizes();

  assert(GenAlignment != 0, "Generation alignment not set up properly");
  assert(HeapAlignment >= GenAlignment,
         "HeapAlignment: " SIZE_FORMAT " less than GenAlignment: " SIZE_FORMAT,
         HeapAlignment, GenAlignment);
  assert(GenAlignment % SpaceAlignment == 0,
         "GenAlignment: " SIZE_FORMAT " not aligned by SpaceAlignment: " SIZE_FORMAT,
         GenAlignment, SpaceAlignment);
  assert(HeapAlignment % GenAlignment == 0,
         "HeapAlignment: " SIZE_FORMAT " not aligned by GenAlignment: " SIZE_FORMAT,
         HeapAlignment, GenAlignment);

  // All generational heaps have a young gen; handle those flags here

  // Make sure the heap is large enough for two generations
  size_t smallest_new_size = young_gen_size_lower_bound();
  size_t smallest_heap_size = align_up(smallest_new_size + old_gen_size_lower_bound(),
                                       HeapAlignment);
  if (MaxHeapSize < smallest_heap_size) {
    FLAG_SET_ERGO(MaxHeapSize, smallest_heap_size);
  }
  // If needed, synchronize MinHeapSize size and InitialHeapSize
  if (MinHeapSize < smallest_heap_size) {
    FLAG_SET_ERGO(MinHeapSize, smallest_heap_size);
    if (InitialHeapSize < MinHeapSize) {
      FLAG_SET_ERGO(InitialHeapSize, smallest_heap_size);
    }
  }

  // Make sure NewSize allows an old generation to fit even if set on the command line
  if (FLAG_IS_CMDLINE(NewSize) && NewSize >= InitialHeapSize) {
    log_warning(gc, ergo)("NewSize was set larger than initial heap size, will use initial heap size.");
    FLAG_SET_ERGO(NewSize, bound_minus_alignment(NewSize, InitialHeapSize, GenAlignment));
  }

  // Now take the actual NewSize into account. We will silently increase NewSize
  // if the user specified a smaller or unaligned value.
  size_t bounded_new_size = bound_minus_alignment(NewSize, MaxHeapSize, GenAlignment);
  bounded_new_size = MAX2(smallest_new_size, align_down(bounded_new_size, GenAlignment));
  if (bounded_new_size != NewSize) {
    FLAG_SET_ERGO(NewSize, bounded_new_size);
  }
  MinNewSize = smallest_new_size;

  if (!FLAG_IS_DEFAULT(MaxNewSize)) {
    if (MaxNewSize >= MaxHeapSize) {
      // Make sure there is room for an old generation
      size_t smaller_max_new_size = MaxHeapSize - GenAlignment;
      if (FLAG_IS_CMDLINE(MaxNewSize)) {
        log_warning(gc, ergo)("MaxNewSize (" SIZE_FORMAT "k) is equal to or greater than the entire "
                              "heap (" SIZE_FORMAT "k).  A new max generation size of " SIZE_FORMAT "k will be used.",
                              MaxNewSize/K, MaxHeapSize/K, smaller_max_new_size/K);
      }
      FLAG_SET_ERGO(MaxNewSize, smaller_max_new_size);
      if (NewSize > MaxNewSize) {
        FLAG_SET_ERGO(NewSize, MaxNewSize);
      }
    } else if (MaxNewSize < NewSize) {
      FLAG_SET_ERGO(MaxNewSize, NewSize);
    } else if (!is_aligned(MaxNewSize, GenAlignment)) {
      FLAG_SET_ERGO(MaxNewSize, align_down(MaxNewSize, GenAlignment));
    }
  }

  if (NewSize > MaxNewSize) {
    // At this point this should only happen if the user specifies a large NewSize and/or
    // a small (but not too small) MaxNewSize.
    if (FLAG_IS_CMDLINE(MaxNewSize)) {
      log_warning(gc, ergo)("NewSize (" SIZE_FORMAT "k) is greater than the MaxNewSize (" SIZE_FORMAT "k). "
                            "A new max generation size of " SIZE_FORMAT "k will be used.",
                            NewSize/K, MaxNewSize/K, NewSize/K);
    }
    FLAG_SET_ERGO(MaxNewSize, NewSize);
  }

  if (SurvivorRatio < 1 || NewRatio < 1) {
    vm_exit_during_initialization("Invalid young gen ratio specified");
  }

  if (OldSize < old_gen_size_lower_bound()) {
    FLAG_SET_ERGO(OldSize, old_gen_size_lower_bound());
  }
  if (!is_aligned(OldSize, GenAlignment)) {
    FLAG_SET_ERGO(OldSize, align_down(OldSize, GenAlignment));
  }

  if (FLAG_IS_CMDLINE(OldSize) && FLAG_IS_DEFAULT(MaxHeapSize)) {
    // NewRatio will be used later to set the young generation size so we use
    // it to calculate how big the heap should be based on the requested OldSize
    // and NewRatio.
    assert(NewRatio > 0, "NewRatio should have been set up earlier");
    size_t calculated_heapsize = (OldSize / NewRatio) * (NewRatio + 1);

    calculated_heapsize = align_up(calculated_heapsize, HeapAlignment);
    FLAG_SET_ERGO(MaxHeapSize, calculated_heapsize);
    FLAG_SET_ERGO(InitialHeapSize, calculated_heapsize);
  }

  // Adjust NewSize and OldSize or MaxHeapSize to match each other
  if (NewSize + OldSize > MaxHeapSize) {
    if (FLAG_IS_CMDLINE(MaxHeapSize)) {
      // Somebody has set a maximum heap size with the intention that we should not
      // exceed it. Adjust New/OldSize as necessary.
      size_t calculated_size = NewSize + OldSize;
      double shrink_factor = (double) MaxHeapSize / calculated_size;
      size_t smaller_new_size = align_down((size_t)(NewSize * shrink_factor), GenAlignment);
      FLAG_SET_ERGO(NewSize, MAX2(young_gen_size_lower_bound(), smaller_new_size));

      // OldSize is already aligned because above we aligned MaxHeapSize to
      // HeapAlignment, and we just made sure that NewSize is aligned to
      // GenAlignment. In initialize_flags() we verified that HeapAlignment
      // is a multiple of GenAlignment.
      FLAG_SET_ERGO(OldSize, MaxHeapSize - NewSize);
    } else {
      FLAG_SET_ERGO(MaxHeapSize, align_up(NewSize + OldSize, HeapAlignment));
    }
  }

  // Update NewSize, if possible, to avoid sizing the young gen too small when only
  // OldSize is set on the command line.
  if (FLAG_IS_CMDLINE(OldSize) && !FLAG_IS_CMDLINE(NewSize)) {
    if (OldSize < InitialHeapSize) {
      size_t new_size = InitialHeapSize - OldSize;
      if (new_size >= MinNewSize && new_size <= MaxNewSize) {
        FLAG_SET_ERGO(NewSize, new_size);
      }
    }
  }

  DEBUG_ONLY(assert_flags();)
}

// Values set on the command line win over any ergonomically
// set command line parameters.
// Ergonomic choice of parameters are done before this
// method is called.  Values for command line parameters such as NewSize
// and MaxNewSize feed those ergonomic choices into this method.
// This method makes the final generation sizings consistent with
// themselves and with overall heap sizings.
// In the absence of explicitly set command line flags, policies
// such as the use of NewRatio are used to size the generation.

// Minimum sizes of the generations may be different than
// the initial sizes.  An inconsistency is permitted here
// in the total size that can be specified explicitly by
// command line specification of OldSize and NewSize and
// also a command line specification of -Xms.  Issue a warning
// but allow the values to pass.
void GenArguments::initialize_size_info() {
  GCArguments::initialize_size_info();

  size_t max_young_size = MaxNewSize;

  // Determine maximum size of the young generation.

  if (FLAG_IS_DEFAULT(MaxNewSize)) {
    max_young_size = scale_by_NewRatio_aligned(MaxHeapSize, GenAlignment);
    // Bound the maximum size by NewSize below (since it historically
    // would have been NewSize and because the NewRatio calculation could
    // yield a size that is too small) and bound it by MaxNewSize above.
    // Ergonomics plays here by previously calculating the desired
    // NewSize and MaxNewSize.
    max_young_size = clamp(max_young_size, NewSize, MaxNewSize);
  }

  // Given the maximum young size, determine the initial and
  // minimum young sizes.
  size_t initial_young_size = NewSize;

  if (MaxHeapSize == InitialHeapSize) {
    // The maximum and initial heap sizes are the same so the generation's
    // initial size must be the same as it maximum size. Use NewSize as the
    // size if set on command line.
    max_young_size = FLAG_IS_CMDLINE(NewSize) ? NewSize : max_young_size;
    initial_young_size = max_young_size;

    // Also update the minimum size if min == initial == max.
    if (MaxHeapSize == MinHeapSize) {
      MinNewSize = max_young_size;
    }
  } else {
    if (FLAG_IS_CMDLINE(NewSize)) {
      // If NewSize is set on the command line, we should use it as
      // the initial size, but make sure it is within the heap bounds.
      initial_young_size =
        MIN2(max_young_size, bound_minus_alignment(NewSize, InitialHeapSize, GenAlignment));
      MinNewSize = bound_minus_alignment(initial_young_size, MinHeapSize, GenAlignment);
    } else {
      // For the case where NewSize is not set on the command line, use
      // NewRatio to size the initial generation size. Use the current
      // NewSize as the floor, because if NewRatio is overly large, the resulting
      // size can be too small.
      initial_young_size =
        clamp(scale_by_NewRatio_aligned(InitialHeapSize, GenAlignment), NewSize, max_young_size);
    }
  }

  log_trace(gc, heap)("1: Minimum young " SIZE_FORMAT "  Initial young " SIZE_FORMAT "  Maximum young " SIZE_FORMAT,
                      MinNewSize, initial_young_size, max_young_size);

  // At this point the minimum, initial and maximum sizes
  // of the overall heap and of the young generation have been determined.
  // The maximum old size can be determined from the maximum young
  // and maximum heap size since no explicit flags exist
  // for setting the old generation maximum.
  MaxOldSize = MAX2(MaxHeapSize - max_young_size, GenAlignment);

  size_t initial_old_size = OldSize;

  // If no explicit command line flag has been set for the
  // old generation size, use what is left.
  if (!FLAG_IS_CMDLINE(OldSize)) {
    // The user has not specified any value but the ergonomics
    // may have chosen a value (which may or may not be consistent
    // with the overall heap size).  In either case make
    // the minimum, maximum and initial sizes consistent
    // with the young sizes and the overall heap sizes.
    MinOldSize = GenAlignment;
    initial_old_size = clamp(InitialHeapSize - initial_young_size, MinOldSize, MaxOldSize);
    // MaxOldSize has already been made consistent above.
  } else {
    // OldSize has been explicitly set on the command line. Use it
    // for the initial size but make sure the minimum allow a young
    // generation to fit as well.
    // If the user has explicitly set an OldSize that is inconsistent
    // with other command line flags, issue a warning.
    // The generation minimums and the overall heap minimum should
    // be within one generation alignment.
    if (initial_old_size > MaxOldSize) {
      log_warning(gc, ergo)("Inconsistency between maximum heap size and maximum "
                            "generation sizes: using maximum heap = " SIZE_FORMAT
                            ", -XX:OldSize flag is being ignored",
                            MaxHeapSize);
      initial_old_size = MaxOldSize;
    }

    MinOldSize = MIN2(initial_old_size, MinHeapSize - MinNewSize);
  }

  // The initial generation sizes should match the initial heap size,
  // if not issue a warning and resize the generations. This behavior
  // differs from JDK8 where the generation sizes have higher priority
  // than the initial heap size.
  if ((initial_old_size + initial_young_size) != InitialHeapSize) {
    log_warning(gc, ergo)("Inconsistency between generation sizes and heap size, resizing "
                          "the generations to fit the heap.");

    size_t desired_young_size = InitialHeapSize - initial_old_size;
    if (InitialHeapSize < initial_old_size) {
      // Old want all memory, use minimum for young and rest for old
      initial_young_size = MinNewSize;
      initial_old_size = InitialHeapSize - MinNewSize;
    } else if (desired_young_size > max_young_size) {
      // Need to increase both young and old generation
      initial_young_size = max_young_size;
      initial_old_size = InitialHeapSize - max_young_size;
    } else if (desired_young_size < MinNewSize) {
      // Need to decrease both young and old generation
      initial_young_size = MinNewSize;
      initial_old_size = InitialHeapSize - MinNewSize;
    } else {
      // The young generation boundaries allow us to only update the
      // young generation.
      initial_young_size = desired_young_size;
    }

    log_trace(gc, heap)("2: Minimum young " SIZE_FORMAT "  Initial young " SIZE_FORMAT "  Maximum young " SIZE_FORMAT,
                        MinNewSize, initial_young_size, max_young_size);
  }

  // Write back to flags if necessary.
  if (NewSize != initial_young_size) {
    FLAG_SET_ERGO(NewSize, initial_young_size);
  }

  if (MaxNewSize != max_young_size) {
    FLAG_SET_ERGO(MaxNewSize, max_young_size);
  }

  if (OldSize != initial_old_size) {
    FLAG_SET_ERGO(OldSize, initial_old_size);
  }

  log_trace(gc, heap)("Minimum old " SIZE_FORMAT "  Initial old " SIZE_FORMAT "  Maximum old " SIZE_FORMAT,
                      MinOldSize, OldSize, MaxOldSize);

  DEBUG_ONLY(assert_size_info();)
}

#ifdef ASSERT
void GenArguments::assert_flags() {
  GCArguments::assert_flags();
  assert(NewSize >= MinNewSize, "Ergonomics decided on a too small young gen size");
  assert(NewSize <= MaxNewSize, "Ergonomics decided on incompatible initial and maximum young gen sizes");
  assert(FLAG_IS_DEFAULT(MaxNewSize) || MaxNewSize < MaxHeapSize, "Ergonomics decided on incompatible maximum young gen and heap sizes");
  assert(NewSize % GenAlignment == 0, "NewSize alignment");
  assert(FLAG_IS_DEFAULT(MaxNewSize) || MaxNewSize % GenAlignment == 0, "MaxNewSize alignment");
  assert(OldSize + NewSize <= MaxHeapSize, "Ergonomics decided on incompatible generation and heap sizes");
  assert(OldSize % GenAlignment == 0, "OldSize alignment");
}

void GenArguments::assert_size_info() {
  GCArguments::assert_size_info();
  // GenArguments::initialize_size_info may update the MaxNewSize
  assert(MaxNewSize < MaxHeapSize, "Ergonomics decided on incompatible maximum young and heap sizes");
  assert(MinNewSize <= NewSize, "Ergonomics decided on incompatible minimum and initial young gen sizes");
  assert(NewSize <= MaxNewSize, "Ergonomics decided on incompatible initial and maximum young gen sizes");
  assert(MinNewSize % GenAlignment == 0, "_min_young_size alignment");
  assert(NewSize % GenAlignment == 0, "_initial_young_size alignment");
  assert(MaxNewSize % GenAlignment == 0, "MaxNewSize alignment");
  assert(MinNewSize <= bound_minus_alignment(MinNewSize, MinHeapSize, GenAlignment),
      "Ergonomics made minimum young generation larger than minimum heap");
  assert(NewSize <=  bound_minus_alignment(NewSize, InitialHeapSize, GenAlignment),
      "Ergonomics made initial young generation larger than initial heap");
  assert(MaxNewSize <= bound_minus_alignment(MaxNewSize, MaxHeapSize, GenAlignment),
      "Ergonomics made maximum young generation lager than maximum heap");
  assert(MinOldSize <= OldSize, "Ergonomics decided on incompatible minimum and initial old gen sizes");
  assert(OldSize <= MaxOldSize, "Ergonomics decided on incompatible initial and maximum old gen sizes");
  assert(MaxOldSize % GenAlignment == 0, "MaxOldSize alignment");
  assert(OldSize % GenAlignment == 0, "OldSize alignment");
  assert(MaxHeapSize <= (MaxNewSize + MaxOldSize), "Total maximum heap sizes must be sum of generation maximum sizes");
  assert(MinNewSize + MinOldSize <= MinHeapSize, "Minimum generation sizes exceed minimum heap size");
  assert(NewSize + OldSize == InitialHeapSize, "Initial generation sizes should match initial heap size");
  assert(MaxNewSize + MaxOldSize == MaxHeapSize, "Maximum generation sizes should match maximum heap size");
}
#endif // ASSERT
