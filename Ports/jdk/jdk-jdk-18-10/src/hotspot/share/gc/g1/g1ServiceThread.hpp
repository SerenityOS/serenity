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
 *
 */

#ifndef SHARE_GC_G1_G1SERVICETHREAD_HPP
#define SHARE_GC_G1_G1SERVICETHREAD_HPP

#include "gc/shared/concurrentGCThread.hpp"
#include "runtime/mutex.hpp"

class G1ServiceTaskQueue;
class G1ServiceThread;

class G1ServiceTask : public CHeapObj<mtGC> {
  friend class G1ServiceTaskQueue;
  friend class G1ServiceThread;

  // The next absolute time this task should be executed.
  jlong _time;
  // Name of the task.
  const char* _name;
  // Next task in the task queue.
  G1ServiceTask* _next;
  // The service thread this task is registered with.
  G1ServiceThread* _service_thread;

  void set_service_thread(G1ServiceThread* thread);
  bool is_registered();

public:
  G1ServiceTask(const char* name);

  jlong time();
  const char* name();
  G1ServiceTask* next();

  // Do the actual work for the task. To get added back to the
  // execution queue a task can call schedule(delay_ms).
  virtual void execute() = 0;

protected:
  // Schedule the task on the associated service thread using
  // the provided delay in milliseconds. Can only be used when
  // currently running on the service thread.
  void schedule(jlong delay_ms);

  // These setters are protected for use by testing and the
  // sentinel task only.
  void set_time(jlong time);
  void set_next(G1ServiceTask* next);
};

class G1SentinelTask : public G1ServiceTask {
public:
  G1SentinelTask();
  virtual void execute();
};

class G1ServiceTaskQueue {
  // The sentinel task is the entry point of this priority queue holding the
  // service tasks. The queue is ordered by the time the tasks are scheduled
  // to run. To simplify list management the sentinel task has its time set
  // to max_jlong, guaranteeing it to be the last task in the queue.
  G1SentinelTask _sentinel;

  // Verify that the queue is ordered.
  void verify_task_queue() NOT_DEBUG_RETURN;
public:
  G1ServiceTaskQueue();

  // precondition: !is_empty().
  G1ServiceTask* front();
  // precondition: !is_empty().
  void remove_front();
  void add_ordered(G1ServiceTask* task);
  bool is_empty();
};

// The G1ServiceThread is used to periodically do a number of different tasks:
//   - re-assess the validity of the prediction for the
//     remembered set lengths of the young generation.
//   - check if a periodic GC should be scheduled.
class G1ServiceThread: public ConcurrentGCThread {
  friend class G1ServiceTask;
  // The monitor is used to ensure thread safety for the task queue
  // and allow other threads to signal the service thread to wake up.
  Monitor _monitor;
  G1ServiceTaskQueue _task_queue;

  void run_service();
  void stop_service();

  // Return the next ready task, waiting until a task is ready.
  // Instead returns nullptr if termination requested.
  G1ServiceTask* wait_for_task();

  void run_task(G1ServiceTask* task);

  // Helper used by both schedule_task() and G1ServiceTask::schedule()
  // to schedule a registered task to run after the given delay.
  void schedule(G1ServiceTask* task, jlong delay, bool notify);

public:
  G1ServiceThread();

  // Register a task with the service thread. The task is guaranteed not to run
  // until at least `delay_ms` has passed. If no delay is specified or the
  // delay is 0, the task will run in the earliest time possible.
  void register_task(G1ServiceTask* task, jlong delay_ms = 0);

  // Schedule an already-registered task to run in at least `delay_ms` time,
  // and notify the service thread.
  void schedule_task(G1ServiceTask* task, jlong delay_ms);
};

#endif // SHARE_GC_G1_G1SERVICETHREAD_HPP
