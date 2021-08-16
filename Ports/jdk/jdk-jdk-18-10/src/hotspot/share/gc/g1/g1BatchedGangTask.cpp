/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "gc/g1/g1BatchedGangTask.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1GCParPhaseTimesTracker.hpp"
#include "runtime/atomic.hpp"
#include "utilities/growableArray.hpp"

void G1AbstractSubTask::record_work_item(uint worker_id, uint index, size_t count) {
  G1CollectedHeap* g1h = G1CollectedHeap::heap();
  g1h->phase_times()->record_thread_work_item(_tag, worker_id, count, index);
}

const char* G1AbstractSubTask::name() const {
  G1CollectedHeap* g1h = G1CollectedHeap::heap();
  return g1h->phase_times()->phase_name(_tag);
}

bool G1BatchedGangTask::try_claim_serial_task(int& task) {
  task = Atomic::fetch_and_add(&_num_serial_tasks_done, 1);
  return task < _serial_tasks.length();
}

void G1BatchedGangTask::add_serial_task(G1AbstractSubTask* task) {
  assert(task != nullptr, "must be");
  _serial_tasks.push(task);
}

void G1BatchedGangTask::add_parallel_task(G1AbstractSubTask* task) {
  assert(task != nullptr, "must be");
  _parallel_tasks.push(task);
}

G1BatchedGangTask::G1BatchedGangTask(const char* name, G1GCPhaseTimes* phase_times) :
  AbstractGangTask(name),
  _num_serial_tasks_done(0),
  _phase_times(phase_times),
  _serial_tasks(),
  _parallel_tasks() {
}

uint G1BatchedGangTask::num_workers_estimate() const {
  double sum = 0.0;
  for (G1AbstractSubTask* task : _serial_tasks) {
    sum += task->worker_cost();
  }
  for (G1AbstractSubTask* task : _parallel_tasks) {
    sum += task->worker_cost();
  }
  return ceil(sum);
}

void G1BatchedGangTask::set_max_workers(uint max_workers) {
  for (G1AbstractSubTask* task : _serial_tasks) {
    task->set_max_workers(max_workers);
  }
  for (G1AbstractSubTask* task : _parallel_tasks) {
    task->set_max_workers(max_workers);
  }
}

void G1BatchedGangTask::work(uint worker_id) {
  int t = 0;
  while (try_claim_serial_task(t)) {
    G1AbstractSubTask* task = _serial_tasks.at(t);
    G1GCParPhaseTimesTracker x(_phase_times, task->tag(), worker_id);
    task->do_work(worker_id);
  }
  for (G1AbstractSubTask* task : _parallel_tasks) {
    G1GCParPhaseTimesTracker x(_phase_times, task->tag(), worker_id);
    task->do_work(worker_id);
  }
}

G1BatchedGangTask::~G1BatchedGangTask() {
  assert(Atomic::load(&_num_serial_tasks_done) >= _serial_tasks.length(),
         "Only %d tasks of %d claimed", Atomic::load(&_num_serial_tasks_done), _serial_tasks.length());

  for (G1AbstractSubTask* task : _parallel_tasks) {
    delete task;
  }
  for (G1AbstractSubTask* task : _serial_tasks) {
    delete task;
  }
}
