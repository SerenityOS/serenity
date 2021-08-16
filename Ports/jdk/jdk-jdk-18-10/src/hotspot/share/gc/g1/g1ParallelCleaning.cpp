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
 *
 */

#include "precompiled.hpp"

#include "gc/g1/g1ParallelCleaning.hpp"
#include "runtime/atomic.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmci.hpp"
#endif

#if INCLUDE_JVMCI
JVMCICleaningTask::JVMCICleaningTask() :
  _cleaning_claimed(0) {
}

bool JVMCICleaningTask::claim_cleaning_task() {
  if (_cleaning_claimed) {
    return false;
  }

  return Atomic::cmpxchg(&_cleaning_claimed, 0, 1) == 0;
}

void JVMCICleaningTask::work(bool unloading_occurred) {
  // One worker will clean JVMCI metadata handles.
  if (unloading_occurred && EnableJVMCI && claim_cleaning_task()) {
    JVMCI::do_unloading(unloading_occurred);
  }
}
#endif // INCLUDE_JVMCI

G1ParallelCleaningTask::G1ParallelCleaningTask(BoolObjectClosure* is_alive,
                                               uint num_workers,
                                               bool unloading_occurred) :
  AbstractGangTask("G1 Parallel Cleaning"),
  _unloading_occurred(unloading_occurred),
  _code_cache_task(num_workers, is_alive, unloading_occurred),
  JVMCI_ONLY(_jvmci_cleaning_task() COMMA)
  _klass_cleaning_task() {
}

// The parallel work done by all worker threads.
void G1ParallelCleaningTask::work(uint worker_id) {
  // Clean JVMCI metadata handles.
  // Execute this task first because it is serial task.
  JVMCI_ONLY(_jvmci_cleaning_task.work(_unloading_occurred);)

  // Do first pass of code cache cleaning.
  _code_cache_task.work(worker_id);

  // Clean all klasses that were not unloaded.
  // The weak metadata in klass doesn't need to be
  // processed if there was no unloading.
  if (_unloading_occurred) {
    _klass_cleaning_task.work();
  }
}
