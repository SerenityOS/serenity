/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zCPU.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zHeuristics.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"

void ZHeuristics::set_medium_page_size() {
  // Set ZPageSizeMedium so that a medium page occupies at most 3.125% of the
  // max heap size. ZPageSizeMedium is initially set to 0, which means medium
  // pages are effectively disabled. It is adjusted only if ZPageSizeMedium
  // becomes larger than ZPageSizeSmall.
  const size_t min = ZGranuleSize;
  const size_t max = ZGranuleSize * 16;
  const size_t unclamped = MaxHeapSize * 0.03125;
  const size_t clamped = clamp(unclamped, min, max);
  const size_t size = round_down_power_of_2(clamped);

  if (size > ZPageSizeSmall) {
    // Enable medium pages
    ZPageSizeMedium             = size;
    ZPageSizeMediumShift        = log2i_exact(ZPageSizeMedium);
    ZObjectSizeLimitMedium      = ZPageSizeMedium / 8;
    ZObjectAlignmentMediumShift = (int)ZPageSizeMediumShift - 13;
    ZObjectAlignmentMedium      = 1 << ZObjectAlignmentMediumShift;
  }
}

size_t ZHeuristics::relocation_headroom() {
  // Calculate headroom needed to avoid in-place relocation. Each worker will try
  // to allocate a small page, and all workers will share a single medium page.
  const uint nworkers = UseDynamicNumberOfGCThreads ? ConcGCThreads : MAX2(ConcGCThreads, ParallelGCThreads);
  return (nworkers * ZPageSizeSmall) + ZPageSizeMedium;
}

bool ZHeuristics::use_per_cpu_shared_small_pages() {
  // Use per-CPU shared small pages only if these pages occupy at most 3.125%
  // of the max heap size. Otherwise fall back to using a single shared small
  // page. This is useful when using small heaps on large machines.
  const size_t per_cpu_share = (MaxHeapSize * 0.03125) / ZCPU::count();
  return per_cpu_share >= ZPageSizeSmall;
}

static uint nworkers_based_on_ncpus(double cpu_share_in_percent) {
  return ceil(os::initial_active_processor_count() * cpu_share_in_percent / 100.0);
}

static uint nworkers_based_on_heap_size(double heap_share_in_percent) {
  const int nworkers = (MaxHeapSize * (heap_share_in_percent / 100.0)) / ZPageSizeSmall;
  return MAX2(nworkers, 1);
}

static uint nworkers(double cpu_share_in_percent) {
  // Cap number of workers so that they don't use more than 2% of the max heap
  // during relocation. This is useful when using small heaps on large machines.
  return MIN2(nworkers_based_on_ncpus(cpu_share_in_percent),
              nworkers_based_on_heap_size(2.0));
}

uint ZHeuristics::nparallel_workers() {
  // Use 60% of the CPUs, rounded up. We would like to use as many threads as
  // possible to increase parallelism. However, using a thread count that is
  // close to the number of processors tends to lead to over-provisioning and
  // scheduling latency issues. Using 60% of the active processors appears to
  // be a fairly good balance.
  return nworkers(60.0);
}

uint ZHeuristics::nconcurrent_workers() {
  // The number of concurrent threads we would like to use heavily depends
  // on the type of workload we are running. Using too many threads will have
  // a negative impact on the application throughput, while using too few
  // threads will prolong the GC-cycle and we then risk being out-run by the
  // application. When in dynamic mode, use up to 25% of the active processors.
  //  When in non-dynamic mode, use 12.5% of the active processors.
  return nworkers(UseDynamicNumberOfGCThreads ? 25.0 : 12.5);
}
