/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "runtime/init.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/task.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"

int PeriodicTask::_num_tasks = 0;
PeriodicTask* PeriodicTask::_tasks[PeriodicTask::max_tasks];

void PeriodicTask::real_time_tick(int delay_time) {
  assert(Thread::current()->is_Watcher_thread(), "must be WatcherThread");

  // The WatcherThread does not participate in the safepoint protocol
  // for the PeriodicTask_lock because it is not a JavaThread.
  MutexLocker ml(PeriodicTask_lock, Mutex::_no_safepoint_check_flag);
  int orig_num_tasks = _num_tasks;

  for(int index = 0; index < _num_tasks; index++) {
    _tasks[index]->execute_if_pending(delay_time);
    if (_num_tasks < orig_num_tasks) { // task dis-enrolled itself
      index--;  // re-do current slot as it has changed
      orig_num_tasks = _num_tasks;
    }
  }
}

int PeriodicTask::time_to_wait() {
  assert(PeriodicTask_lock->owned_by_self(), "PeriodicTask_lock required");

  if (_num_tasks == 0) {
    return 0; // sleep until shutdown or a task is enrolled
  }

  int delay = _tasks[0]->time_to_next_interval();
  for (int index = 1; index < _num_tasks; index++) {
    delay = MIN2(delay, _tasks[index]->time_to_next_interval());
  }
  return delay;
}


PeriodicTask::PeriodicTask(size_t interval_time) :
  _counter(0), _interval((int) interval_time) {
  // Sanity check the interval time
  assert(_interval >= PeriodicTask::min_interval &&
         _interval %  PeriodicTask::interval_gran == 0,
              "improper PeriodicTask interval time");
}

PeriodicTask::~PeriodicTask() {
  // This PeriodicTask may have already been disenrolled by a call
  // to disenroll() before the PeriodicTask was deleted.
  disenroll();
}

// enroll the current PeriodicTask
void PeriodicTask::enroll() {
  // Follow normal safepoint aware lock enter protocol if the caller does
  // not already own the PeriodicTask_lock. Otherwise, we don't try to
  // enter it again because VM internal Mutexes do not support recursion.
  //
  MutexLocker ml(PeriodicTask_lock->owned_by_self() ? NULL : PeriodicTask_lock);

  if (_num_tasks == PeriodicTask::max_tasks) {
    fatal("Overflow in PeriodicTask table");
  } else {
    _tasks[_num_tasks++] = this;
  }

  WatcherThread* thread = WatcherThread::watcher_thread();
  if (thread != NULL) {
    thread->unpark();
  } else {
    WatcherThread::start();
  }
}

// disenroll the current PeriodicTask
void PeriodicTask::disenroll() {
  // Follow normal safepoint aware lock enter protocol if the caller does
  // not already own the PeriodicTask_lock. Otherwise, we don't try to
  // enter it again because VM internal Mutexes do not support recursion.
  //
  MutexLocker ml(PeriodicTask_lock->owned_by_self() ? NULL : PeriodicTask_lock);

  int index;
  for(index = 0; index < _num_tasks && _tasks[index] != this; index++)
    ;

  if (index == _num_tasks) {
    return;
  }

  _num_tasks--;

  for (; index < _num_tasks; index++) {
    _tasks[index] = _tasks[index+1];
  }
}
