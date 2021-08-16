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
#include "gc/g1/g1ServiceThread.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/os.hpp"
#include "utilities/autoRestore.hpp"
#include "unittest.hpp"

class CheckTask : public G1ServiceTask {
  int _execution_count;
  bool _reschedule;

public:
  CheckTask(const char* name) :
      G1ServiceTask(name),
      _execution_count(0),
      _reschedule(true) { }
  virtual void execute() {
    _execution_count++;
    if (_reschedule) {
      schedule(100);
    }
  }

  int execution_count() { return _execution_count;}
  void set_reschedule(bool reschedule) { _reschedule = reschedule; }
};

static void stop_service_thread(G1ServiceThread* thread) {
  ThreadInVMfromNative tvn(JavaThread::current());
  thread->stop();
}

// Test that a task that is added during runtime gets run.
TEST_VM(G1ServiceThread, test_add) {
  // Create thread and let it start.
  G1ServiceThread* st = new G1ServiceThread();
  os::naked_short_sleep(500);

  CheckTask ct("AddAndRun");
  st->register_task(&ct);

  // Give CheckTask time to run.
  os::naked_short_sleep(500);
  stop_service_thread(st);

  ASSERT_GT(ct.execution_count(), 0);
}

// Test that a task that is added while the service thread is
// waiting gets run in a timely manner.
TEST_VM(G1ServiceThread, test_add_while_waiting) {
  // Make sure default tasks use long intervals so that the service thread
  // is doing a long wait for the next execution.
  AutoModifyRestore<uintx> f1(G1PeriodicGCInterval, 100000);
  AutoModifyRestore<uintx> f2(G1ConcRefinementServiceIntervalMillis, 100000);

  // Create thread and let it start.
  G1ServiceThread* st = new G1ServiceThread();
  os::naked_short_sleep(500);

  // Register a new task that should run right away.
  CheckTask ct("AddWhileWaiting");
  st->register_task(&ct);

  // Give CheckTask time to run.
  os::naked_short_sleep(500);
  stop_service_thread(st);

  ASSERT_GT(ct.execution_count(), 0);
}

// Test that a task with negative timeout is not rescheduled.
TEST_VM(G1ServiceThread, test_add_run_once) {
  // Create thread and let it start.
  G1ServiceThread* st = new G1ServiceThread();
  os::naked_short_sleep(500);

  // Set reschedule to false to only run once.
  CheckTask ct("AddRunOnce");
  ct.set_reschedule(false);
  st->register_task(&ct);

  // Give CheckTask time to run.
  os::naked_short_sleep(500);
  stop_service_thread(st);

  // Should be exactly 1 since negative timeout should
  // prevent rescheduling.
  ASSERT_EQ(ct.execution_count(), 1);
}

class TestTask : public G1ServiceTask {
  jlong _delay_ms;
public:
  TestTask(jlong delay) :
      G1ServiceTask("TestTask"),
      _delay_ms(delay) {
    set_time(delay);
  }
  virtual void execute() {}
  void update_time(jlong now, int multiplier) {
    set_time(now + (_delay_ms * multiplier));
  }
};

TEST_VM(G1ServiceTaskQueue, add_ordered) {
  G1ServiceTaskQueue queue;

  int num_test_tasks = 5;
  for (int i = 1; i <= num_test_tasks; i++) {
    // Create tasks with different timeout.
    TestTask* task = new TestTask(100 * i);
    queue.add_ordered(task);
  }

  // Now fake a run-loop, that reschedules the tasks using a
  // random multiplier.
  for (jlong now = 0; now < 1000000; now++) {
    // Random multiplier is at least 1 to ensure progress.
    int multiplier = 1 + os::random() % 10;
    while (queue.front()->time() < now) {
      TestTask* task = (TestTask*) queue.front();
      queue.remove_front();
      // Update delay multiplier.
      task->execute();
      task->update_time(now, multiplier);
      // All additions will verify that the queue is sorted.
      queue.add_ordered(task);
    }
  }

  while (!queue.is_empty()) {
    G1ServiceTask* task = queue.front();
    queue.remove_front();
    delete task;
  }
}

#ifdef ASSERT
TEST_VM_ASSERT_MSG(G1ServiceTaskQueue, remove_from_empty,
    ".*Should never try to verify empty queue") {
  G1ServiceTaskQueue queue;
  queue.remove_front();
}

TEST_VM_ASSERT_MSG(G1ServiceTaskQueue, get_from_empty,
    ".*Should never try to verify empty queue") {
  G1ServiceTaskQueue queue;
  queue.front();
}

TEST_VM_ASSERT_MSG(G1ServiceTaskQueue, set_time_in_queue,
    ".*Not allowed to update time while in queue") {
  G1ServiceTaskQueue queue;
  TestTask a(100);
  queue.add_ordered(&a);
  // Not allowed to update time while in queue.
  a.update_time(500, 1);
}

#endif
