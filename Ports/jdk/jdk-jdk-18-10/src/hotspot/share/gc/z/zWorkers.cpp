/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zThread.hpp"
#include "gc/z/zWorkers.hpp"
#include "runtime/java.hpp"

class ZWorkersInitializeTask : public AbstractGangTask {
private:
  const uint     _nworkers;
  uint           _started;
  ZConditionLock _lock;

public:
  ZWorkersInitializeTask(uint nworkers) :
      AbstractGangTask("ZWorkersInitializeTask"),
      _nworkers(nworkers),
      _started(0),
      _lock() {}

  virtual void work(uint worker_id) {
    // Register as worker
    ZThread::set_worker();

    // Wait for all threads to start
    ZLocker<ZConditionLock> locker(&_lock);
    if (++_started == _nworkers) {
      // All threads started
      _lock.notify_all();
    } else {
      while (_started != _nworkers) {
        _lock.wait();
      }
    }
  }
};

ZWorkers::ZWorkers() :
    _workers("ZWorker",
             UseDynamicNumberOfGCThreads ? ConcGCThreads : MAX2(ConcGCThreads, ParallelGCThreads),
             true /* are_GC_task_threads */,
             true /* are_ConcurrentGC_threads */) {

  if (UseDynamicNumberOfGCThreads) {
    log_info_p(gc, init)("GC Workers: %u (dynamic)", _workers.total_workers());
  } else {
    log_info_p(gc, init)("GC Workers: %u/%u (static)", ConcGCThreads, _workers.total_workers());
  }

  // Initialize worker threads
  _workers.initialize_workers();
  _workers.update_active_workers(_workers.total_workers());
  if (_workers.active_workers() != _workers.total_workers()) {
    vm_exit_during_initialization("Failed to create ZWorkers");
  }

  // Execute task to register threads as workers
  ZWorkersInitializeTask task(_workers.total_workers());
  _workers.run_task(&task);
}

uint ZWorkers::active_workers() const {
  return _workers.active_workers();
}

void ZWorkers::set_active_workers(uint nworkers) {
  log_info(gc, task)("Using %u workers", nworkers);
  _workers.update_active_workers(nworkers);
}

void ZWorkers::run(ZTask* task) {
  log_debug(gc, task)("Executing Task: %s, Active Workers: %u", task->name(), active_workers());
  ZStatWorkers::at_start();
  _workers.run_task(task->gang_task());
  ZStatWorkers::at_end();
}

void ZWorkers::run_all(ZTask* task) {
  // Save number of active workers
  const uint prev_active_workers = _workers.active_workers();

  // Execute task using all workers
  _workers.update_active_workers(_workers.total_workers());
  log_debug(gc, task)("Executing Task: %s, Active Workers: %u", task->name(), active_workers());
  _workers.run_task(task->gang_task());

  // Restore number of active workers
  _workers.update_active_workers(prev_active_workers);
}

void ZWorkers::threads_do(ThreadClosure* tc) const {
  _workers.threads_do(tc);
}
