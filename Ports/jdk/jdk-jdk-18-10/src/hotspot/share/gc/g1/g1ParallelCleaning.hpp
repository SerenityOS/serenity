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

#ifndef SHARE_GC_G1_G1PARALLELCLEANING_HPP
#define SHARE_GC_G1_G1PARALLELCLEANING_HPP

#include "gc/shared/parallelCleaning.hpp"

#if INCLUDE_JVMCI
class JVMCICleaningTask : public StackObj {
  volatile int       _cleaning_claimed;

public:
  JVMCICleaningTask();
  // Clean JVMCI metadata handles.
  void work(bool unloading_occurred);

private:
  bool claim_cleaning_task();
};
#endif

// Do cleanup of some weakly held data in the same parallel task.
// Assumes a non-moving context.
class G1ParallelCleaningTask : public AbstractGangTask {
private:
  bool                    _unloading_occurred;
  CodeCacheUnloadingTask  _code_cache_task;
#if INCLUDE_JVMCI
  JVMCICleaningTask       _jvmci_cleaning_task;
#endif
  KlassCleaningTask       _klass_cleaning_task;

public:
  // The constructor is run in the VMThread.
  G1ParallelCleaningTask(BoolObjectClosure* is_alive,
                         uint num_workers,
                         bool unloading_occurred);

  void work(uint worker_id);
};

#endif // SHARE_GC_G1_G1PARALLELCLEANING_HPP
