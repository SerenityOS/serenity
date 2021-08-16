/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_TASK_HPP
#define SHARE_RUNTIME_TASK_HPP

#include "memory/allocation.hpp"
#include "runtime/timer.hpp"

// A PeriodicTask has the sole purpose of executing its task
// function with regular intervals.
// Usage:
//   PeriodicTask pf(10);
//   pf.enroll();
//   ...
//   pf.disenroll();

class PeriodicTask: public CHeapObj<mtInternal> {
 public:
  // Useful constants.
  // The interval constants are used to ensure the declared interval
  // is appropriate;  it must be between min_interval and max_interval,
  // and have a granularity of interval_gran (all in millis).
  enum { max_tasks     = 10,       // Max number of periodic tasks in system
         interval_gran = 10,
         min_interval  = 10,
         max_interval  = 10000 };

  static int num_tasks()   { return _num_tasks; }

 private:
  int _counter;
  const int _interval;

  static int _num_tasks;
  static PeriodicTask* _tasks[PeriodicTask::max_tasks];
  // Can only be called by the WatcherThread
  static void real_time_tick(int delay_time);

  // Only the WatcherThread can cause us to execute PeriodicTasks
  friend class WatcherThread;
 public:
  PeriodicTask(size_t interval_time); // interval is in milliseconds of elapsed time
  virtual ~PeriodicTask();

  // Make the task active
  // For dynamic enrollment at the time T, the task will execute somewhere
  // between T and T + interval_time.
  void enroll();

  // Make the task deactive
  void disenroll();

  void execute_if_pending(int delay_time) {
    // make sure we don't overflow
    jlong tmp = (jlong) _counter + (jlong) delay_time;

    if (tmp >= (jlong) _interval) {
      _counter = 0;
      task();
    } else {
      _counter += delay_time;
    }
  }

  // Returns how long (time in milliseconds) before the next time we should
  // execute this task.
  int time_to_next_interval() const {
    assert(_interval > _counter,  "task counter greater than interval?");
    return _interval - _counter;
  }

  // Calculate when the next periodic task will fire.
  // Called by the WatcherThread's run method.
  // Requires the PeriodicTask_lock.
  static int time_to_wait();

  // The task to perform at each period
  virtual void task() = 0;
};

#endif // SHARE_RUNTIME_TASK_HPP
