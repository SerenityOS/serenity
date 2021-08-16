/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_WORKERMANAGER_HPP
#define SHARE_GC_SHARED_WORKERMANAGER_HPP

#include "gc/shared/gc_globals.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"
#include "utilities/globalDefinitions.hpp"

class WorkerManager : public AllStatic {
 public:
  // Create additional workers as needed.
  //   active_workers - number of workers being requested for an upcoming
  // parallel task.
  //   total_workers - total number of workers.  This is the maximum
  // number possible.
  //   created_workers - number of workers already created.  This maybe
  // less than, equal to, or greater than active workers.  If greater than
  // or equal to active_workers, nothing is done.
  //   worker_type - type of thread.
  //   initializing - true if this is called to get the initial number of
  // GC workers.
  // If initializing is true, do a vm exit if the workers cannot be created.
  // The initializing = true case is for JVM start up and failing to
  // create all the worker at start should considered a problem so exit.
  // If initializing = false, there are already some number of worker
  // threads and a failure would not be optimal but should not be fatal.
  static uint add_workers (WorkGang* workers,
                           uint active_workers,
                           uint total_workers,
                           uint created_workers,
                           os::ThreadType worker_type,
                           bool initializing);

  // Log (at trace level) a change in the number of created workers.
  static void log_worker_creation(WorkGang* workers,
                                  uint previous_created_workers,
                                  uint active_workers,
                                  uint created_workers,
                                  bool initializing);
};

uint WorkerManager::add_workers(WorkGang* workers,
                                uint active_workers,
                                uint total_workers,
                                uint created_workers,
                                os::ThreadType worker_type,
                                bool initializing) {
  uint start = created_workers;
  uint end = MIN2(active_workers, total_workers);
  for (uint worker_id = start; worker_id < end; worker_id += 1) {
    WorkerThread* new_worker = NULL;
    if (initializing || !InjectGCWorkerCreationFailure) {
      new_worker = workers->install_worker(worker_id);
    }
    if (new_worker == NULL || !os::create_thread(new_worker, worker_type)) {
      log_trace(gc, task)("WorkerManager::add_workers() : "
                          "creation failed due to failed allocation of native %s",
                          new_worker == NULL ? "memory" : "thread");
      delete new_worker;
      if (initializing) {
        vm_exit_out_of_memory(0, OOM_MALLOC_ERROR, "Cannot create worker GC thread. Out of system resources.");
      }
      break;
    }
    created_workers++;
    os::start_thread(new_worker);
  }

  log_trace(gc, task)("WorkerManager::add_workers() : "
                      "created_workers: %u", created_workers);

  return created_workers;
}

void WorkerManager::log_worker_creation(WorkGang* workers,
                                        uint previous_created_workers,
                                        uint active_workers,
                                        uint created_workers,
                                        bool initializing) {
  if (previous_created_workers < created_workers) {
    const char* initializing_msg = initializing ? "Adding initial" : "Creating additional";
    log_trace(gc, task)("%s %s(s) previously created workers %u active workers %u total created workers %u",
                        initializing_msg, workers->group_name(), previous_created_workers, active_workers, created_workers);
  }
}

#endif // SHARE_GC_SHARED_WORKERMANAGER_HPP
