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

#include "precompiled.hpp"
#include "gc/g1/g1ServiceThread.hpp"
#include "logging/log.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/timer.hpp"
#include "runtime/os.hpp"

G1SentinelTask::G1SentinelTask() : G1ServiceTask("Sentinel Task") {
  set_time(max_jlong);
  set_next(this);
}

void G1SentinelTask::execute() {
  guarantee(false, "Sentinel service task should never be executed.");
}

G1ServiceThread::G1ServiceThread() :
    ConcurrentGCThread(),
    _monitor(Mutex::leaf,
             "G1ServiceThread monitor",
             true,
             Monitor::_safepoint_check_never),
    _task_queue() {
  set_name("G1 Service");
  create_and_start();
}

void G1ServiceThread::register_task(G1ServiceTask* task, jlong delay_ms) {
  guarantee(!task->is_registered(), "Task already registered");
  guarantee(task->next() == NULL, "Task already in queue");

  // Make sure the service thread is still up and running, there is a race
  // during shutdown where the service thread has been stopped, but other
  // GC threads might still be running and trying to add tasks.
  if (has_terminated()) {
    log_debug(gc, task)("G1 Service Thread (%s) (terminated)", task->name());
    return;
  }

  log_debug(gc, task)("G1 Service Thread (%s) (register)", task->name());

  // Associate the task with the service thread.
  task->set_service_thread(this);

  // Schedule the task to run after the given delay. The service will be
  // notified to check if this task is first in the queue.
  schedule_task(task, delay_ms);
}

void G1ServiceThread::schedule(G1ServiceTask* task, jlong delay_ms, bool notify) {
  guarantee(task->is_registered(), "Must be registered before scheduled");
  guarantee(task->next() == NULL, "Task already in queue");

  // Schedule task by setting the task time and adding it to queue.
  jlong delay = TimeHelper::millis_to_counter(delay_ms);
  task->set_time(os::elapsed_counter() + delay);

  MonitorLocker ml(&_monitor, Mutex::_no_safepoint_check_flag);
  _task_queue.add_ordered(task);
  if (notify) {
    ml.notify();
  }

  log_trace(gc, task)("G1 Service Thread (%s) (schedule) @%1.3fs",
                      task->name(), TimeHelper::counter_to_seconds(task->time()));
}

void G1ServiceThread::schedule_task(G1ServiceTask* task, jlong delay_ms) {
  schedule(task, delay_ms, true /* notify */);
}

G1ServiceTask* G1ServiceThread::wait_for_task() {
  MonitorLocker ml(&_monitor, Mutex::_no_safepoint_check_flag);
  while (!should_terminate()) {
    if (_task_queue.is_empty()) {
      log_trace(gc, task)("G1 Service Thread (wait for new tasks)");
      ml.wait();
    } else {
      G1ServiceTask* task = _task_queue.front();
      jlong scheduled = task->time();
      jlong now = os::elapsed_counter();
      if (scheduled <= now) {
        _task_queue.remove_front();
        return task;
      } else {
        // Round up to try not to wake up early, and to avoid round down to
        // zero (which has special meaning of wait forever) by conversion.
        double delay = ceil(TimeHelper::counter_to_millis(scheduled - now));
        log_trace(gc, task)("G1 Service Thread (wait %1.3fs)", (delay / 1000.0));
        int64_t delay_ms = static_cast<int64_t>(delay);
        assert(delay_ms > 0, "invariant");
        ml.wait(delay_ms);
      }
    }
  }
  return nullptr;               // Return nullptr when terminating.
}

void G1ServiceThread::run_task(G1ServiceTask* task) {
  jlong start = os::elapsed_counter();
  double vstart = os::elapsedVTime();

  assert(task->time() <= start,
         "task run early: " JLONG_FORMAT " > " JLONG_FORMAT,
         task->time(), start);
  log_debug(gc, task, start)("G1 Service Thread (%s) (run %1.3fms after schedule)",
                             task->name(),
                             TimeHelper::counter_to_millis(start - task->time()));

  task->execute();

  log_debug(gc, task)("G1 Service Thread (%s) (run: %1.3fms) (cpu: %1.3fms)",
                      task->name(),
                      TimeHelper::counter_to_millis(os::elapsed_counter() - start),
                      (os::elapsedVTime() - vstart) * MILLIUNITS);
}

void G1ServiceThread::run_service() {
  while (true) {
    G1ServiceTask* task = wait_for_task();
    if (task == nullptr) break;
    run_task(task);
  }
  assert(should_terminate(), "invariant");
  log_debug(gc, task)("G1 Service Thread (stopping)");
}

void G1ServiceThread::stop_service() {
  MonitorLocker ml(&_monitor, Mutex::_no_safepoint_check_flag);
  ml.notify();
}

G1ServiceTask::G1ServiceTask(const char* name) :
  _time(),
  _name(name),
  _next(NULL),
  _service_thread(NULL) { }

void G1ServiceTask::set_service_thread(G1ServiceThread* thread) {
  _service_thread = thread;
}

bool G1ServiceTask::is_registered() {
  return _service_thread != NULL;
}

void G1ServiceTask::schedule(jlong delay_ms) {
  assert(Thread::current() == _service_thread,
         "Can only be used when already running on the service thread");
  // No need to notify, since we *are* the service thread.
  _service_thread->schedule(this, delay_ms, false /* notify */);
}

const char* G1ServiceTask::name() {
  return _name;
}

void G1ServiceTask::set_time(jlong time) {
  assert(_next == NULL, "Not allowed to update time while in queue");
  _time = time;
}

jlong G1ServiceTask::time() {
  return _time;
}

void G1ServiceTask::set_next(G1ServiceTask* next) {
  _next = next;
}

G1ServiceTask* G1ServiceTask::next() {
  return _next;
}

G1ServiceTaskQueue::G1ServiceTaskQueue() : _sentinel() { }

void G1ServiceTaskQueue::remove_front() {
  verify_task_queue();

  G1ServiceTask* task = _sentinel.next();
  _sentinel.set_next(task->next());
  task->set_next(NULL);
}

G1ServiceTask* G1ServiceTaskQueue::front() {
  verify_task_queue();
  return _sentinel.next();
}

bool G1ServiceTaskQueue::is_empty() {
  return &_sentinel == _sentinel.next();
}

void G1ServiceTaskQueue::add_ordered(G1ServiceTask* task) {
  assert(task != NULL, "not a valid task");
  assert(task->next() == NULL, "invariant");
  assert(task->time() != max_jlong, "invalid time for task");

  G1ServiceTask* current = &_sentinel;
  while (task->time() >= current->next()->time()) {
    assert(task != current, "Task should only be added once.");
    current = current->next();
  }

  // Update the links.
  task->set_next(current->next());
  current->set_next(task);

  verify_task_queue();
}

#ifdef ASSERT
void G1ServiceTaskQueue::verify_task_queue() {
  G1ServiceTask* cur = _sentinel.next();

  assert(cur != &_sentinel, "Should never try to verify empty queue");
  while (cur != &_sentinel) {
    G1ServiceTask* next = cur->next();
    assert(cur->time() <= next->time(),
           "Tasks out of order, prev: %s (%1.3fs), next: %s (%1.3fs)",
           cur->name(), TimeHelper::counter_to_seconds(cur->time()), next->name(), TimeHelper::counter_to_seconds(next->time()));

    assert(cur != next, "Invariant");
    cur = next;
  }
}
#endif
