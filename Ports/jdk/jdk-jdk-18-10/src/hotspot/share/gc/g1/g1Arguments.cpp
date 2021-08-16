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
#include "gc/g1/g1Arguments.hpp"
#include "gc/g1/g1CardSet.hpp"
#include "gc/g1/g1CardSetContainers.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1HeapVerifier.hpp"
#include "gc/g1/heapRegion.hpp"
#include "gc/g1/heapRegionRemSet.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/gcArguments.hpp"
#include "gc/shared/workerPolicy.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"

static size_t calculate_heap_alignment(size_t space_alignment) {
  size_t card_table_alignment = CardTableRS::ct_max_alignment_constraint();
  size_t page_size = UseLargePages ? os::large_page_size() : os::vm_page_size();
  return MAX3(card_table_alignment, space_alignment, page_size);
}

void G1Arguments::initialize_alignments() {
  // Set up the region size and associated fields.
  //
  // There is a circular dependency here. We base the region size on the heap
  // size, but the heap size should be aligned with the region size. To get
  // around this we use the unaligned values for the heap.
  HeapRegion::setup_heap_region_size(MaxHeapSize);

  SpaceAlignment = HeapRegion::GrainBytes;
  HeapAlignment = calculate_heap_alignment(SpaceAlignment);

  // We need to initialize card set configuration as soon as heap region size is
  // known as it depends on it and is used really early.
  initialize_card_set_configuration();
  // Needs remembered set initialization as the ergonomics are based
  // on it.
  if (FLAG_IS_DEFAULT(G1EagerReclaimRemSetThreshold)) {
    FLAG_SET_ERGO(G1EagerReclaimRemSetThreshold, G1RemSetArrayOfCardsEntries);
  }
}

size_t G1Arguments::conservative_max_heap_alignment() {
  return HeapRegion::max_region_size();
}

void G1Arguments::initialize_verification_types() {
  if (strlen(VerifyGCType) > 0) {
    const char delimiter[] = " ,\n";
    size_t length = strlen(VerifyGCType);
    char* type_list = NEW_C_HEAP_ARRAY(char, length + 1, mtInternal);
    strncpy(type_list, VerifyGCType, length + 1);
    char* save_ptr;

    char* token = strtok_r(type_list, delimiter, &save_ptr);
    while (token != NULL) {
      parse_verification_type(token);
      token = strtok_r(NULL, delimiter, &save_ptr);
    }
    FREE_C_HEAP_ARRAY(char, type_list);
  }
}

void G1Arguments::parse_verification_type(const char* type) {
  if (strcmp(type, "young-normal") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyYoungNormal);
  } else if (strcmp(type, "concurrent-start") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyConcurrentStart);
  } else if (strcmp(type, "mixed") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyMixed);
  } else if (strcmp(type, "young-evac-fail") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyYoungEvacFail);
  } else if (strcmp(type, "remark") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyRemark);
  } else if (strcmp(type, "cleanup") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyCleanup);
  } else if (strcmp(type, "full") == 0) {
    G1HeapVerifier::enable_verification_type(G1HeapVerifier::G1VerifyFull);
  } else {
    log_warning(gc, verify)("VerifyGCType: '%s' is unknown. Available types are: "
                            "young-normal, young-evac-fail, concurrent-start, mixed, remark, cleanup and full", type);
  }
}

// Returns the maximum number of workers to be used in a concurrent
// phase based on the number of GC workers being used in a STW
// phase.
static uint scale_concurrent_worker_threads(uint num_gc_workers) {
  return MAX2((num_gc_workers + 2) / 4, 1U);
}

void G1Arguments::initialize_mark_stack_size() {
  if (FLAG_IS_DEFAULT(MarkStackSize)) {
    size_t mark_stack_size = MIN2(MarkStackSizeMax,
                                  MAX2(MarkStackSize, (size_t)ConcGCThreads * TASKQUEUE_SIZE));
    FLAG_SET_ERGO(MarkStackSize, mark_stack_size);
  }

  log_trace(gc)("MarkStackSize: %uk  MarkStackSizeMax: %uk", (uint)(MarkStackSize / K), (uint)(MarkStackSizeMax / K));
}


void G1Arguments::initialize_card_set_configuration() {
  assert(HeapRegion::LogOfHRGrainBytes != 0, "not initialized");
  // Array of Cards card set container globals.
  const int LOG_M = 20;
  uint region_size_log_mb = (uint)MAX2(HeapRegion::LogOfHRGrainBytes - LOG_M, 0);

  if (FLAG_IS_DEFAULT(G1RemSetArrayOfCardsEntries)) {
    uint num_cards_in_inline_ptr = G1CardSetConfiguration::num_cards_in_inline_ptr(HeapRegion::LogOfHRGrainBytes - CardTable::card_shift);
    FLAG_SET_ERGO(G1RemSetArrayOfCardsEntries, MAX2(num_cards_in_inline_ptr * 2,
                                                    G1RemSetArrayOfCardsEntriesBase * (1u << (region_size_log_mb + 1))));
  }

  // Round to next 8 byte boundary for array to maximize space usage.
  size_t const cur_size = G1CardSetArray::size_in_bytes(G1RemSetArrayOfCardsEntries);
  FLAG_SET_ERGO(G1RemSetArrayOfCardsEntries,
                G1RemSetArrayOfCardsEntries + (uint)(align_up(cur_size, G1CardSetAllocOptions::BufferAlignment) - cur_size) / sizeof(G1CardSetArray::EntryDataType));

  // Howl card set container globals.
  if (FLAG_IS_DEFAULT(G1RemSetHowlNumBuckets)) {
    FLAG_SET_ERGO(G1RemSetHowlNumBuckets, G1CardSetHowl::num_buckets(HeapRegion::CardsPerRegion,
                                                                     G1RemSetArrayOfCardsEntries,
                                                                     G1RemSetHowlMaxNumBuckets));
  }

  if (FLAG_IS_DEFAULT(G1RemSetHowlMaxNumBuckets)) {
    FLAG_SET_ERGO(G1RemSetHowlMaxNumBuckets, MAX2(G1RemSetHowlMaxNumBuckets, G1RemSetHowlNumBuckets));
  } else if (G1RemSetHowlMaxNumBuckets < G1RemSetHowlNumBuckets) {
    FormatBuffer<> buf("Maximum Howl card set container bucket size %u smaller than requested bucket size %u",
                       G1RemSetHowlMaxNumBuckets, G1RemSetHowlNumBuckets);
    vm_exit_during_initialization(buf);
  }
}

void G1Arguments::initialize() {
  GCArguments::initialize();
  assert(UseG1GC, "Error");
  FLAG_SET_DEFAULT(ParallelGCThreads, WorkerPolicy::parallel_worker_threads());
  if (ParallelGCThreads == 0) {
    assert(!FLAG_IS_DEFAULT(ParallelGCThreads), "The default value for ParallelGCThreads should not be 0.");
    vm_exit_during_initialization("The flag -XX:+UseG1GC can not be combined with -XX:ParallelGCThreads=0", NULL);
  }

  // When dumping the CDS archive we want to reduce fragmentation by
  // triggering a full collection. To get as low fragmentation as
  // possible we only use one worker thread.
  if (DumpSharedSpaces) {
    FLAG_SET_ERGO(ParallelGCThreads, 1);
  }

  if (FLAG_IS_DEFAULT(G1ConcRefinementThreads)) {
    FLAG_SET_ERGO(G1ConcRefinementThreads, ParallelGCThreads);
  }

  if (FLAG_IS_DEFAULT(ConcGCThreads) || ConcGCThreads == 0) {
    // Calculate the number of concurrent worker threads by scaling
    // the number of parallel GC threads.
    uint marking_thread_num = scale_concurrent_worker_threads(ParallelGCThreads);
    FLAG_SET_ERGO(ConcGCThreads, marking_thread_num);
  }

  if (FLAG_IS_DEFAULT(GCTimeRatio) || GCTimeRatio == 0) {
    // In G1, we want the default GC overhead goal to be higher than
    // it is for PS, or the heap might be expanded too aggressively.
    // We set it here to ~8%.
    FLAG_SET_DEFAULT(GCTimeRatio, 12);
  }

  // Below, we might need to calculate the pause time interval based on
  // the pause target. When we do so we are going to give G1 maximum
  // flexibility and allow it to do pauses when it needs to. So, we'll
  // arrange that the pause interval to be pause time target + 1 to
  // ensure that a) the pause time target is maximized with respect to
  // the pause interval and b) we maintain the invariant that pause
  // time target < pause interval. If the user does not want this
  // maximum flexibility, they will have to set the pause interval
  // explicitly.

  if (FLAG_IS_DEFAULT(MaxGCPauseMillis)) {
    // The default pause time target in G1 is 200ms
    FLAG_SET_DEFAULT(MaxGCPauseMillis, 200);
  }

  // Then, if the interval parameter was not set, set it according to
  // the pause time target (this will also deal with the case when the
  // pause time target is the default value).
  if (FLAG_IS_DEFAULT(GCPauseIntervalMillis)) {
    FLAG_SET_DEFAULT(GCPauseIntervalMillis, MaxGCPauseMillis + 1);
  }

  if (FLAG_IS_DEFAULT(ParallelRefProcEnabled) && ParallelGCThreads > 1) {
    FLAG_SET_DEFAULT(ParallelRefProcEnabled, true);
  }

  // By default do not let the target stack size to be more than 1/4 of the entries
  if (FLAG_IS_DEFAULT(GCDrainStackTargetSize)) {
    FLAG_SET_ERGO(GCDrainStackTargetSize, MIN2(GCDrainStackTargetSize, (uintx)TASKQUEUE_SIZE / 4));
  }

#ifdef COMPILER2
  // Enable loop strip mining to offer better pause time guarantees
  if (FLAG_IS_DEFAULT(UseCountedLoopSafepoints)) {
    FLAG_SET_DEFAULT(UseCountedLoopSafepoints, true);
    if (FLAG_IS_DEFAULT(LoopStripMiningIter)) {
      FLAG_SET_DEFAULT(LoopStripMiningIter, 1000);
    }
  }
#endif

  initialize_mark_stack_size();
  initialize_verification_types();

  // Verify that the maximum parallelism isn't too high to eventually overflow
  // the refcount in G1CardSetContainer.
  uint max_parallel_refinement_threads = G1ConcRefinementThreads + G1DirtyCardQueueSet::num_par_ids();
  uint const divisor = 3;  // Safe divisor; we increment by 2 for each claim, but there is a small initial value.
  if (max_parallel_refinement_threads > UINTPTR_MAX / divisor) {
    vm_exit_during_initialization("Too large parallelism for remembered sets.");
  }
}

void G1Arguments::initialize_heap_flags_and_sizes() {
  GCArguments::initialize_heap_flags_and_sizes();
}

CollectedHeap* G1Arguments::create_heap() {
  return new G1CollectedHeap();
}

size_t G1Arguments::heap_reserved_size_bytes() {
  return MaxHeapSize;
}

