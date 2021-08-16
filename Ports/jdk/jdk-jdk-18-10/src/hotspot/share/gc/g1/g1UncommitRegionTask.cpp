/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1UncommitRegionTask.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "runtime/globals.hpp"
#include "utilities/ticks.hpp"

G1UncommitRegionTask* G1UncommitRegionTask::_instance = NULL;

G1UncommitRegionTask::G1UncommitRegionTask() :
    G1ServiceTask("G1 Uncommit Region Task"),
    _active(false),
    _summary_duration(),
    _summary_region_count(0) { }

void G1UncommitRegionTask::initialize() {
  assert(_instance == NULL, "Already initialized");
  _instance = new G1UncommitRegionTask();

  // Register the task with the service thread. This will automatically
  // schedule the task so we change the state to active.
  _instance->set_active(true);
  G1CollectedHeap::heap()->service_thread()->register_task(_instance);
}

G1UncommitRegionTask* G1UncommitRegionTask::instance() {
  if (_instance == NULL) {
    initialize();
  }
  return _instance;
}

void G1UncommitRegionTask::enqueue() {
  assert_at_safepoint_on_vm_thread();

  G1UncommitRegionTask* uncommit_task = instance();
  if (!uncommit_task->is_active()) {
    // Change state to active and schedule using UncommitInitialDelayMs.
    uncommit_task->set_active(true);
    G1CollectedHeap::heap()->service_thread()->schedule_task(uncommit_task, UncommitInitialDelayMs);
  }
}

bool G1UncommitRegionTask::is_active() {
  return _active;
}

void G1UncommitRegionTask::set_active(bool state) {
  assert(_active != state, "Must do a state change");
  // There is no need to guard _active with a lock since the places where it
  // is updated can never run in parallel. The state is set to true only in
  // a safepoint and it is set to false while running on the service thread
  // joined with the suspendible thread set.
  _active = state;
}

void G1UncommitRegionTask::report_execution(Tickspan time, uint regions) {
  _summary_region_count += regions;
  _summary_duration += time;

  log_trace(gc, heap)("Concurrent Uncommit: " SIZE_FORMAT "%s, %u regions, %1.3fms",
                      byte_size_in_proper_unit(regions * HeapRegion::GrainBytes),
                      proper_unit_for_byte_size(regions * HeapRegion::GrainBytes),
                      regions,
                      time.seconds() * 1000);
}

void G1UncommitRegionTask::report_summary() {
  log_debug(gc, heap)("Concurrent Uncommit Summary: " SIZE_FORMAT "%s, %u regions, %1.3fms",
                      byte_size_in_proper_unit(_summary_region_count * HeapRegion::GrainBytes),
                      proper_unit_for_byte_size(_summary_region_count * HeapRegion::GrainBytes),
                      _summary_region_count,
                      _summary_duration.seconds() * 1000);
}

void G1UncommitRegionTask::clear_summary() {
  _summary_duration = Tickspan();
  _summary_region_count = 0;
}

void G1UncommitRegionTask::execute() {
  assert(_active, "Must be active");

  // Translate the size limit into a number of regions. This cannot be a
  // compile time constant because G1HeapRegionSize is set ergonomically.
  static const uint region_limit = (uint) (UncommitSizeLimit / G1HeapRegionSize);

  // Prevent from running during a GC pause.
  SuspendibleThreadSetJoiner sts;
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  Ticks start = Ticks::now();
  uint uncommit_count = g1h->uncommit_regions(region_limit);
  Tickspan uncommit_time = (Ticks::now() - start);

  if (uncommit_count > 0) {
    report_execution(uncommit_time, uncommit_count);
  }

  // Reschedule if there are more regions to uncommit, otherwise
  // change state to inactive.
  if (g1h->has_uncommittable_regions()) {
    // Delay to avoid starving application.
    schedule(UncommitTaskDelayMs);
  } else {
    // Nothing more to do, change state and report a summary.
    set_active(false);
    report_summary();
    clear_summary();
  }
}
