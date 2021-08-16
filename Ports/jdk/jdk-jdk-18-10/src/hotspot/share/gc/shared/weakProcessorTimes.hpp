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

#ifndef SHARE_GC_SHARED_WEAKPROCESSORTIMES_HPP
#define SHARE_GC_SHARED_WEAKPROCESSORTIMES_HPP

#include "gc/shared/oopStorageSet.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ticks.hpp"

template<typename T> class WorkerDataArray;

class WeakProcessorTimes {
  enum {
    DeadItems,
    TotalItems
  };
  uint _max_threads;
  uint _active_workers;

  // Total time for weak processor.
  double _total_time_sec;

  // Per-worker times and linked items.
  WorkerDataArray<double>* _worker_data[EnumRange<OopStorageSet::WeakId>().size()];
  WorkerDataArray<double>* worker_data(OopStorageSet::WeakId id) const;

  void log_summary(OopStorageSet::WeakId id, uint indent) const;
  template <typename T>
  void log_details(WorkerDataArray<T>* data, uint indent) const;

public:
  WeakProcessorTimes(uint max_threads);
  ~WeakProcessorTimes();

  uint max_threads() const;
  uint active_workers() const;
  void set_active_workers(uint n);

  double total_time_sec() const;
  double worker_time_sec(uint worker_id, OopStorageSet::WeakId id) const;

  void record_total_time_sec(double time_sec);
  void record_worker_time_sec(uint worker_id,
                              OopStorageSet::WeakId id,
                              double time_sec);
  void record_worker_items(uint worker_id,
                           OopStorageSet::WeakId id,
                           size_t num_dead,
                           size_t num_total);

  void reset();

  void log_total(uint indent = 0) const;
  void log_subtotals(uint indent = 0) const;
};

// Record total weak processor time and worker count in times.
// Does nothing if times is NULL.
class WeakProcessorTimeTracker : StackObj {
  WeakProcessorTimes* _times;
  Ticks _start_time;

public:
  WeakProcessorTimeTracker(WeakProcessorTimes* times);
  ~WeakProcessorTimeTracker();
};

// Record time contribution for the current thread.
// Does nothing if times is NULL.
class WeakProcessorParTimeTracker : StackObj {
  WeakProcessorTimes* _times;
  OopStorageSet::WeakId _storage_id;
  uint _worker_id;
  Ticks _start_time;

public:
  // For tracking possibly parallel times (even if processed by
  // only one thread).
  // Precondition: worker_id < times->max_threads().
  WeakProcessorParTimeTracker(WeakProcessorTimes* times,
                              OopStorageSet::WeakId storage_id,
                              uint worker_id);

  ~WeakProcessorParTimeTracker();
};

#endif // SHARE_GC_SHARED_WEAKPROCESSORTIMES_HPP
