/*
 * Copyright (c) 2018, 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shared/gcArguments.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "gc/shared/workerPolicy.hpp"
#include "gc/shenandoah/shenandoahArguments.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "utilities/defaultStream.hpp"

void ShenandoahArguments::initialize() {
#if !(defined AARCH64 || defined AMD64 || defined IA32)
  vm_exit_during_initialization("Shenandoah GC is not supported on this platform.");
#endif

#if 0 // leave this block as stepping stone for future platforms
  log_warning(gc)("Shenandoah GC is not fully supported on this platform:");
  log_warning(gc)("  concurrent modes are not supported, only STW cycles are enabled;");
  log_warning(gc)("  arch-specific barrier code is not implemented, disabling barriers;");

  FLAG_SET_DEFAULT(ShenandoahGCHeuristics,           "passive");

  FLAG_SET_DEFAULT(ShenandoahSATBBarrier,            false);
  FLAG_SET_DEFAULT(ShenandoahLoadRefBarrier,         false);
  FLAG_SET_DEFAULT(ShenandoahIUBarrier,              false);
  FLAG_SET_DEFAULT(ShenandoahCASBarrier,             false);
  FLAG_SET_DEFAULT(ShenandoahCloneBarrier,           false);

  FLAG_SET_DEFAULT(ShenandoahVerifyOptoBarriers,     false);
#endif
  if (UseLargePages) {
    size_t large_page_size = os::large_page_size();
    if ((align_up(MaxHeapSize, large_page_size) / large_page_size) < ShenandoahHeapRegion::MIN_NUM_REGIONS) {
      warning("Large pages size (" SIZE_FORMAT "K) is too large to afford page-sized regions, disabling uncommit",
              os::large_page_size() / K);
      FLAG_SET_DEFAULT(ShenandoahUncommit, false);
    }
  }

  // Enable NUMA by default. While Shenandoah is not NUMA-aware, enabling NUMA makes
  // storage allocation code NUMA-aware.
  if (FLAG_IS_DEFAULT(UseNUMA)) {
    FLAG_SET_DEFAULT(UseNUMA, true);
  }

  // Set up default number of concurrent threads. We want to have cycles complete fast
  // enough, but we also do not want to steal too much CPU from the concurrently running
  // application. Using 1/4 of available threads for concurrent GC seems a good
  // compromise here.
  bool ergo_conc = FLAG_IS_DEFAULT(ConcGCThreads);
  if (ergo_conc) {
    FLAG_SET_DEFAULT(ConcGCThreads, MAX2(1, os::initial_active_processor_count() / 4));
  }

  if (ConcGCThreads == 0) {
    vm_exit_during_initialization("Shenandoah expects ConcGCThreads > 0, check -XX:ConcGCThreads=#");
  }

  // Set up default number of parallel threads. We want to have decent pauses performance
  // which would use parallel threads, but we also do not want to do too many threads
  // that will overwhelm the OS scheduler. Using 1/2 of available threads seems to be a fair
  // compromise here. Due to implementation constraints, it should not be lower than
  // the number of concurrent threads.
  bool ergo_parallel = FLAG_IS_DEFAULT(ParallelGCThreads);
  if (ergo_parallel) {
    FLAG_SET_DEFAULT(ParallelGCThreads, MAX2(1, os::initial_active_processor_count() / 2));
  }

  if (ParallelGCThreads == 0) {
    vm_exit_during_initialization("Shenandoah expects ParallelGCThreads > 0, check -XX:ParallelGCThreads=#");
  }

  // Make sure ergonomic decisions do not break the thread count invariants.
  // This may happen when user overrides one of the flags, but not the other.
  // When that happens, we want to adjust the setting that was set ergonomically.
  if (ParallelGCThreads < ConcGCThreads) {
    if (ergo_conc && !ergo_parallel) {
      FLAG_SET_DEFAULT(ConcGCThreads, ParallelGCThreads);
    } else if (!ergo_conc && ergo_parallel) {
      FLAG_SET_DEFAULT(ParallelGCThreads, ConcGCThreads);
    } else if (ergo_conc && ergo_parallel) {
      // Should not happen, check the ergonomic computation above. Fail with relevant error.
      vm_exit_during_initialization("Shenandoah thread count ergonomic error");
    } else {
      // User settings error, report and ask user to rectify.
      vm_exit_during_initialization("Shenandoah expects ConcGCThreads <= ParallelGCThreads, check -XX:ParallelGCThreads, -XX:ConcGCThreads");
    }
  }

  if (ShenandoahRegionSampling && FLAG_IS_DEFAULT(PerfDataMemorySize)) {
    // When sampling is enabled, max out the PerfData memory to get more
    // Shenandoah data in, including Matrix.
    FLAG_SET_DEFAULT(PerfDataMemorySize, 2048*K);
  }

#ifdef COMPILER2
  // Shenandoah cares more about pause times, rather than raw throughput.
  if (FLAG_IS_DEFAULT(UseCountedLoopSafepoints)) {
    FLAG_SET_DEFAULT(UseCountedLoopSafepoints, true);
    if (FLAG_IS_DEFAULT(LoopStripMiningIter)) {
      FLAG_SET_DEFAULT(LoopStripMiningIter, 1000);
    }
  }
#ifdef ASSERT
  // C2 barrier verification is only reliable when all default barriers are enabled
  if (ShenandoahVerifyOptoBarriers &&
          (!FLAG_IS_DEFAULT(ShenandoahSATBBarrier)            ||
           !FLAG_IS_DEFAULT(ShenandoahLoadRefBarrier)         ||
           !FLAG_IS_DEFAULT(ShenandoahIUBarrier)              ||
           !FLAG_IS_DEFAULT(ShenandoahCASBarrier)             ||
           !FLAG_IS_DEFAULT(ShenandoahCloneBarrier)
          )) {
    warning("Unusual barrier configuration, disabling C2 barrier verification");
    FLAG_SET_DEFAULT(ShenandoahVerifyOptoBarriers, false);
  }
#else
  guarantee(!ShenandoahVerifyOptoBarriers, "Should be disabled");
#endif // ASSERT
#endif // COMPILER2

  // Record more information about previous cycles for improved debugging pleasure
  if (FLAG_IS_DEFAULT(LogEventsBufferEntries)) {
    FLAG_SET_DEFAULT(LogEventsBufferEntries, 250);
  }

  if ((InitialHeapSize == MaxHeapSize) && ShenandoahUncommit) {
    log_info(gc)("Min heap equals to max heap, disabling ShenandoahUncommit");
    FLAG_SET_DEFAULT(ShenandoahUncommit, false);
  }

  // If class unloading is disabled, no unloading for concurrent cycles as well.
  if (!ClassUnloading) {
    FLAG_SET_DEFAULT(ClassUnloadingWithConcurrentMark, false);
  }

  // TLAB sizing policy makes resizing decisions before each GC cycle. It averages
  // historical data, assigning more recent data the weight according to TLABAllocationWeight.
  // Current default is good for generational collectors that run frequent young GCs.
  // With Shenandoah, GC cycles are much less frequent, so we need we need sizing policy
  // to converge faster over smaller number of resizing decisions.
  if (FLAG_IS_DEFAULT(TLABAllocationWeight)) {
    FLAG_SET_DEFAULT(TLABAllocationWeight, 90);
  }
}

size_t ShenandoahArguments::conservative_max_heap_alignment() {
  size_t align = ShenandoahMaxRegionSize;
  if (UseLargePages) {
    align = MAX2(align, os::large_page_size());
  }
  return align;
}

void ShenandoahArguments::initialize_alignments() {
  // Need to setup sizes early to get correct alignments.
  MaxHeapSize = ShenandoahHeapRegion::setup_sizes(MaxHeapSize);

  // This is expected by our algorithm for ShenandoahHeap::heap_region_containing().
  size_t align = ShenandoahHeapRegion::region_size_bytes();
  if (UseLargePages) {
    align = MAX2(align, os::large_page_size());
  }
  SpaceAlignment = align;
  HeapAlignment = align;
}

CollectedHeap* ShenandoahArguments::create_heap() {
  return new ShenandoahHeap(new ShenandoahCollectorPolicy());
}
