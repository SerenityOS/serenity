/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/workerPolicy.hpp"
#include "logging/log.hpp"
#include "memory/universe.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"

uint WorkerPolicy::_parallel_worker_threads = 0;
bool WorkerPolicy::_parallel_worker_threads_initialized = false;

uint WorkerPolicy::nof_parallel_worker_threads(uint num,
                                               uint den,
                                               uint switch_pt) {
  if (FLAG_IS_DEFAULT(ParallelGCThreads)) {
    assert(ParallelGCThreads == 0, "Default ParallelGCThreads is not 0");
    uint threads;
    // For very large machines, there are diminishing returns
    // for large numbers of worker threads.  Instead of
    // hogging the whole system, use a fraction of the workers for every
    // processor after the first 8.  For example, on a 72 cpu machine
    // and a chosen fraction of 5/8
    // use 8 + (72 - 8) * (5/8) == 48 worker threads.
    uint ncpus = (uint) os::initial_active_processor_count();
    threads = (ncpus <= switch_pt) ?
              ncpus :
              (switch_pt + ((ncpus - switch_pt) * num) / den);
#ifndef _LP64
    // On 32-bit binaries the virtual address space available to the JVM
    // is usually limited to 2-3 GB (depends on the platform).
    // Do not use up address space with too many threads (stacks and per-thread
    // data). Note that x86 apps running on Win64 have 2 stacks per thread.
    // GC may more generally scale down threads by max heap size (etc), but the
    // consequences of over-provisioning threads are higher on 32-bit JVMS,
    // so add hard limit here:
    threads = MIN2(threads, (2 * switch_pt));
#endif
    return threads;
  } else {
    return ParallelGCThreads;
  }
}

uint WorkerPolicy::calc_parallel_worker_threads() {
  uint den = VM_Version::parallel_worker_threads_denominator();
  return nof_parallel_worker_threads(5, den, 8);
}

uint WorkerPolicy::parallel_worker_threads() {
  if (!_parallel_worker_threads_initialized) {
    if (FLAG_IS_DEFAULT(ParallelGCThreads)) {
      _parallel_worker_threads = WorkerPolicy::calc_parallel_worker_threads();
    } else {
      _parallel_worker_threads = ParallelGCThreads;
    }
    _parallel_worker_threads_initialized = true;
  }
  return _parallel_worker_threads;
}

//  If the number of GC threads was set on the command line, use it.
//  Else
//    Calculate the number of GC threads based on the number of Java threads.
//    Calculate the number of GC threads based on the size of the heap.
//    Use the larger.
uint WorkerPolicy::calc_default_active_workers(uintx total_workers,
                                               const uintx min_workers,
                                               uintx active_workers,
                                               uintx application_workers) {
  // If the user has specifically set the number of GC threads, use them.

  // If the user has turned off using a dynamic number of GC threads
  // or the users has requested a specific number, set the active
  // number of workers to all the workers.

  uintx new_active_workers = total_workers;
  uintx prev_active_workers = active_workers;
  uintx active_workers_by_JT = 0;
  uintx active_workers_by_heap_size = 0;

  // Always use at least min_workers but use up to
  // GCThreadsPerJavaThreads * application threads.
  active_workers_by_JT =
    MAX2((uintx) GCWorkersPerJavaThread * application_workers,
         min_workers);

  // Choose a number of GC threads based on the current size
  // of the heap.  This may be complicated because the size of
  // the heap depends on factors such as the throughput goal.
  // Still a large heap should be collected by more GC threads.
  active_workers_by_heap_size =
    MAX2((size_t) 2U, Universe::heap()->capacity() / HeapSizePerGCThread);

  uintx max_active_workers =
    MAX2(active_workers_by_JT, active_workers_by_heap_size);

  new_active_workers = MIN2(max_active_workers, (uintx) total_workers);

  // Increase GC workers instantly but decrease them more
  // slowly.
  if (new_active_workers < prev_active_workers) {
    new_active_workers =
      MAX2(min_workers, (prev_active_workers + new_active_workers) / 2);
  }

  // Check once more that the number of workers is within the limits.
  assert(min_workers <= total_workers, "Minimum workers not consistent with total workers");
  assert(new_active_workers >= min_workers, "Minimum workers not observed");
  assert(new_active_workers <= total_workers, "Total workers not observed");

  log_trace(gc, task)("WorkerPolicy::calc_default_active_workers() : "
    "active_workers(): " UINTX_FORMAT "  new_active_workers: " UINTX_FORMAT "  "
    "prev_active_workers: " UINTX_FORMAT "\n"
    " active_workers_by_JT: " UINTX_FORMAT "  active_workers_by_heap_size: " UINTX_FORMAT,
    active_workers, new_active_workers, prev_active_workers,
    active_workers_by_JT, active_workers_by_heap_size);
  assert(new_active_workers > 0, "Always need at least 1");
  return new_active_workers;
}

uint WorkerPolicy::calc_active_workers(uintx total_workers,
                                       uintx active_workers,
                                       uintx application_workers) {
  // If the user has specifically set the number of GC threads, use them.

  // If the user has turned off using a dynamic number of GC threads
  // or the users has requested a specific number, set the active
  // number of workers to all the workers.

  uint new_active_workers;
  if (!UseDynamicNumberOfGCThreads || !FLAG_IS_DEFAULT(ParallelGCThreads)) {
    new_active_workers = total_workers;
  } else {
    uintx min_workers = (total_workers == 1) ? 1 : 2;
    new_active_workers = calc_default_active_workers(total_workers,
                                                     min_workers,
                                                     active_workers,
                                                     application_workers);
  }
  assert(new_active_workers > 0, "Always need at least 1");
  return new_active_workers;
}

uint WorkerPolicy::calc_active_conc_workers(uintx total_workers,
                                            uintx active_workers,
                                            uintx application_workers) {
  if (!UseDynamicNumberOfGCThreads || !FLAG_IS_DEFAULT(ConcGCThreads)) {
    return ConcGCThreads;
  } else {
    uint no_of_gc_threads = calc_default_active_workers(total_workers,
                                                        1, /* Minimum number of workers */
                                                        active_workers,
                                                        application_workers);
    return no_of_gc_threads;
  }
}
