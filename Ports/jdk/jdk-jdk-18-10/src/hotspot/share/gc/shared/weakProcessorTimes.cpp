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
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/weakProcessor.hpp"
#include "gc/shared/weakProcessorTimes.hpp"
#include "gc/shared/workerDataArray.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ticks.hpp"

const double uninitialized_time = -1.0;

#ifdef ASSERT
static bool is_initialized_time(double t) { return t >= 0.0; }
#endif // ASSERT


WeakProcessorTimes::WeakProcessorTimes(uint max_threads) :
  _max_threads(max_threads),
  _active_workers(0),
  _total_time_sec(uninitialized_time),
  _worker_data()
{
  assert(_max_threads > 0, "max_threads must not be zero");

  WorkerDataArray<double>** wpt = _worker_data;
  for (auto id : EnumRange<OopStorageSet::WeakId>()) {
    assert(size_t(wpt - _worker_data) < ARRAY_SIZE(_worker_data), "invariant");
    const char* description = OopStorageSet::storage(id)->name();
    *wpt = new WorkerDataArray<double>(NULL, description, _max_threads);
    (*wpt)->create_thread_work_items("Dead", DeadItems);
    (*wpt)->create_thread_work_items("Total", TotalItems);
    wpt++;
  }
  assert(size_t(wpt - _worker_data) == ARRAY_SIZE(_worker_data), "invariant");
}

WeakProcessorTimes::~WeakProcessorTimes() {
  for (size_t i = 0; i < ARRAY_SIZE(_worker_data); ++i) {
    delete _worker_data[i];
  }
}

uint WeakProcessorTimes::max_threads() const { return _max_threads; }

uint WeakProcessorTimes::active_workers() const {
  assert(_active_workers != 0, "active workers not set");
  return _active_workers;
}

void WeakProcessorTimes::set_active_workers(uint n) {
  assert(_active_workers == 0, "active workers already set");
  assert(n > 0, "active workers must be non-zero");
  assert(n <= _max_threads, "active workers must not exceed max threads");
  _active_workers = n;
}

void WeakProcessorTimes::reset() {
  _active_workers = 0;
  _total_time_sec = uninitialized_time;
  for (size_t i = 0; i < ARRAY_SIZE(_worker_data); ++i) {
    _worker_data[i]->reset();
  }
}

double WeakProcessorTimes::total_time_sec() const {
  assert(is_initialized_time(_total_time_sec), "Total time not set");
  return _total_time_sec;
}

void WeakProcessorTimes::record_total_time_sec(double time_sec) {
  assert(!is_initialized_time(_total_time_sec), "Already set total time");
  _total_time_sec = time_sec;
}

WorkerDataArray<double>* WeakProcessorTimes::worker_data(OopStorageSet::WeakId id) const {
  size_t index = EnumRange<OopStorageSet::WeakId>().index(id);
  assert(index < ARRAY_SIZE(_worker_data), "invalid phase");
  return _worker_data[index];
}

double WeakProcessorTimes::worker_time_sec(uint worker_id,
                                           OopStorageSet::WeakId id) const {
  assert(worker_id < active_workers(),
         "invalid worker id %u for %u", worker_id, active_workers());
  return worker_data(id)->get(worker_id);
}

void WeakProcessorTimes::record_worker_time_sec(uint worker_id,
                                                OopStorageSet::WeakId id,
                                                double time_sec) {
  worker_data(id)->set(worker_id, time_sec);
}

void WeakProcessorTimes::record_worker_items(uint worker_id,
                                             OopStorageSet::WeakId id,
                                             size_t num_dead,
                                             size_t num_total) {
  WorkerDataArray<double>* data = worker_data(id);
  data->set_or_add_thread_work_item(worker_id, num_dead, DeadItems);
  data->set_or_add_thread_work_item(worker_id, num_total, TotalItems);
}

static double elapsed_time_sec(Ticks start_time, Ticks end_time) {
  return (end_time - start_time).seconds();
}

WeakProcessorTimeTracker::WeakProcessorTimeTracker(WeakProcessorTimes* times) :
  _times(times),
  _start_time(Ticks::now())
{}

WeakProcessorTimeTracker::~WeakProcessorTimeTracker() {
  if (_times != NULL) {
    Ticks end_time = Ticks::now();
    _times->record_total_time_sec(elapsed_time_sec(_start_time, end_time));
  }
}

WeakProcessorParTimeTracker::WeakProcessorParTimeTracker(WeakProcessorTimes* times,
                                                         OopStorageSet::WeakId storage_id,
                                                         uint worker_id) :
  _times(times),
  _storage_id(storage_id),
  _worker_id(worker_id),
  _start_time(Ticks::now())
{
  assert(_times == NULL || worker_id < _times->active_workers(),
         "Invalid worker_id %u", worker_id);
}


WeakProcessorParTimeTracker::~WeakProcessorParTimeTracker() {
  if (_times != NULL) {
    double time_sec = elapsed_time_sec(_start_time, Ticks::now());
    _times->record_worker_time_sec(_worker_id, _storage_id, time_sec);
  }
}

//////////////////////////////////////////////////////////////////////////////
// Printing times

const char* const indents[] = {"", "  ", "    ", "      ", "        "};
const size_t max_indents_index = ARRAY_SIZE(indents) - 1;

static const char* indent_str(size_t i) {
  return indents[MIN2(i, max_indents_index)];
}

#define TIME_FORMAT "%.1lfms"

void WeakProcessorTimes::log_summary(OopStorageSet::WeakId id, uint indent) const {
  LogTarget(Debug, gc, phases) lt;
  LogStream ls(lt);
  ls.print("%s", indents[indent]);
  worker_data(id)->print_summary_on(&ls, true);
  log_details(worker_data(id), indent + 1);

  for (uint i = 0; i < worker_data(id)->MaxThreadWorkItems; i++) {
    WorkerDataArray<size_t>* work_items = worker_data(id)->thread_work_items(i);
    if (work_items != NULL) {
      ls.print("%s", indents[indent + 1]);
      work_items->print_summary_on(&ls, true);
      log_details(work_items, indent + 1);
    }
  }
}

template <typename T>
void WeakProcessorTimes::log_details(WorkerDataArray<T>* data, uint indent) const {
  LogTarget(Trace, gc, phases) lt;
  if (lt.is_enabled()) {
    LogStream ls(lt);
    ls.print("%s", indents[indent]);
    data->print_details_on(&ls);
  }
}

void WeakProcessorTimes::log_subtotals(uint indent) const {
  if (log_is_enabled(Debug, gc, phases)) {
    for (auto id : EnumRange<OopStorageSet::WeakId>()) {
      log_summary(id, indent);
    }
  }
}

void WeakProcessorTimes::log_total(uint indent) const {
  log_debug(gc, phases)("%s%s: " TIME_FORMAT,
                        indent_str(indent),
                        "Weak Processing",
                        total_time_sec() * MILLIUNITS);
}
