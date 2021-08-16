/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_WEAKPROCESSOR_INLINE_HPP
#define SHARE_GC_SHARED_WEAKPROCESSOR_INLINE_HPP

#include "gc/shared/weakProcessor.hpp"

#include "classfile/stringTable.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/weakProcessorTimes.hpp"
#include "gc/shared/workgroup.hpp"
#include "prims/resolvedMethodTable.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"

class BoolObjectClosure;
class OopClosure;

template <typename IsAlive, typename KeepAlive>
class WeakProcessor::CountingClosure : public Closure {
  IsAlive* _is_alive;
  KeepAlive* _keep_alive;
  size_t _old_dead;
  size_t _new_dead;
  size_t _live;

public:
  CountingClosure(IsAlive* is_alive, KeepAlive* keep_alive) :
    _is_alive(is_alive),
    _keep_alive(keep_alive),
    _old_dead(0),
    _new_dead(0),
    _live(0)
  {}

  void do_oop(oop* p) {
    oop obj = *p;
    if (obj == NULL) {
      ++_old_dead;
    } else if (_is_alive->do_object_b(obj)) {
      _keep_alive->do_oop(p);
      ++_live;
    } else {
      *p = NULL;
      ++_new_dead;
    }
  }

  size_t dead() const { return _old_dead + _new_dead; }
  size_t new_dead() const { return _new_dead; }
  size_t total() const { return dead() + _live; }
};

template<typename IsAlive, typename KeepAlive>
void WeakProcessor::Task::work(uint worker_id,
                               IsAlive* is_alive,
                               KeepAlive* keep_alive) {
  assert(worker_id < _nworkers,
         "worker_id (%u) exceeds task's configured workers (%u)",
         worker_id, _nworkers);

  for (auto id : EnumRange<OopStorageSet::WeakId>()) {
    CountingClosure<IsAlive, KeepAlive> cl(is_alive, keep_alive);
    WeakProcessorParTimeTracker pt(_times, id, worker_id);
    StorageState* cur_state = _storage_states.par_state(id);
    assert(cur_state->storage() == OopStorageSet::storage(id), "invariant");
    cur_state->oops_do(&cl);
    cur_state->increment_num_dead(cl.dead());
    if (_times != NULL) {
      _times->record_worker_items(worker_id, id, cl.new_dead(), cl.total());
    }
  }
}

class WeakProcessor::GangTask : public AbstractGangTask {
  Task _task;
  BoolObjectClosure* _is_alive;
  OopClosure* _keep_alive;
  void (*_erased_do_work)(GangTask* task, uint worker_id);

  template<typename IsAlive, typename KeepAlive>
  static void erased_do_work(GangTask* task, uint worker_id) {
    task->_task.work(worker_id,
                     static_cast<IsAlive*>(task->_is_alive),
                     static_cast<KeepAlive*>(task->_keep_alive));
  }

public:
  template<typename IsAlive, typename KeepAlive>
  GangTask(const char* name,
           IsAlive* is_alive,
           KeepAlive* keep_alive,
           WeakProcessorTimes* times,
           uint nworkers) :
    AbstractGangTask(name),
    _task(times, nworkers),
    _is_alive(is_alive),
    _keep_alive(keep_alive),
    _erased_do_work(&erased_do_work<IsAlive, KeepAlive>)
  {}

  virtual void work(uint worker_id);
  void report_num_dead() { _task.report_num_dead(); }
};

template<typename IsAlive, typename KeepAlive>
void WeakProcessor::weak_oops_do(WorkGang* workers,
                                 IsAlive* is_alive,
                                 KeepAlive* keep_alive,
                                 WeakProcessorTimes* times) {
  WeakProcessorTimeTracker tt(times);

  uint nworkers = ergo_workers(MIN2(workers->total_workers(),
                                    times->max_threads()));

  GangTask task("Weak Processor", is_alive, keep_alive, times, nworkers);
  workers->run_task(&task, nworkers);
  task.report_num_dead();
}

template<typename IsAlive, typename KeepAlive>
void WeakProcessor::weak_oops_do(WorkGang* workers,
                                 IsAlive* is_alive,
                                 KeepAlive* keep_alive,
                                 uint indent_log) {
  uint nworkers = ergo_workers(workers->total_workers());
  WeakProcessorTimes times(nworkers);
  weak_oops_do(workers, is_alive, keep_alive, &times);
  times.log_subtotals(indent_log); // Caller logs total if desired.
}

#endif // SHARE_GC_SHARED_WEAKPROCESSOR_INLINE_HPP
