/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcId.hpp"
#include "gc/shared/workgroup.hpp"
#include "gc/shared/workerManager.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "runtime/semaphore.hpp"
#include "runtime/thread.inline.hpp"

static void run_foreground_task_if_needed(AbstractGangTask* task, uint num_workers,
                                          bool add_foreground_work) {
  if (add_foreground_work) {
    log_develop_trace(gc, workgang)("Running work gang: %s task: %s worker: foreground",
      Thread::current()->name(), task->name());
    task->work(num_workers);
    log_develop_trace(gc, workgang)("Finished work gang: %s task: %s worker: foreground "
      "thread: " PTR_FORMAT, Thread::current()->name(), task->name(), p2i(Thread::current()));
  }
}

// WorkGang dispatcher implemented with semaphores.
//
// Semaphores don't require the worker threads to re-claim the lock when they wake up.
// This helps lowering the latency when starting and stopping the worker threads.
class GangTaskDispatcher : public CHeapObj<mtGC> {
  // The task currently being dispatched to the GangWorkers.
  AbstractGangTask* _task;

  volatile uint _started;
  volatile uint _not_finished;

  // Semaphore used to start the GangWorkers.
  Semaphore* _start_semaphore;
  // Semaphore used to notify the coordinator that all workers are done.
  Semaphore* _end_semaphore;

public:
  GangTaskDispatcher() :
      _task(NULL),
      _started(0),
      _not_finished(0),
      _start_semaphore(new Semaphore()),
      _end_semaphore(new Semaphore())
{ }

  ~GangTaskDispatcher() {
    delete _start_semaphore;
    delete _end_semaphore;
  }

  // Coordinator API.

  // Distributes the task out to num_workers workers.
  // Returns when the task has been completed by all workers.
  void coordinator_execute_on_workers(AbstractGangTask* task, uint num_workers, bool add_foreground_work) {
    // No workers are allowed to read the state variables until they have been signaled.
    _task         = task;
    _not_finished = num_workers;

    // Dispatch 'num_workers' number of tasks.
    _start_semaphore->signal(num_workers);

    run_foreground_task_if_needed(task, num_workers, add_foreground_work);

    // Wait for the last worker to signal the coordinator.
    _end_semaphore->wait();

    // No workers are allowed to read the state variables after the coordinator has been signaled.
    assert(_not_finished == 0, "%d not finished workers?", _not_finished);
    _task    = NULL;
    _started = 0;

  }

  // Worker API.

  // Waits for a task to become available to the worker.
  // Returns when the worker has been assigned a task.
  WorkData worker_wait_for_task() {
    // Wait for the coordinator to dispatch a task.
    _start_semaphore->wait();

    uint num_started = Atomic::add(&_started, 1u);

    // Subtract one to get a zero-indexed worker id.
    uint worker_id = num_started - 1;

    return WorkData(_task, worker_id);
  }

  // Signal to the coordinator that the worker is done with the assigned task.
  void worker_done_with_task() {
    // Mark that the worker is done with the task.
    // The worker is not allowed to read the state variables after this line.
    uint not_finished = Atomic::sub(&_not_finished, 1u);

    // The last worker signals to the coordinator that all work is completed.
    if (not_finished == 0) {
      _end_semaphore->signal();
    }
  }
};
// Definitions of WorkGang methods.

WorkGang::WorkGang(const char* name, uint workers, bool are_GC_task_threads, bool are_ConcurrentGC_threads) :
    _workers(NULL),
    _total_workers(workers),
    _active_workers(UseDynamicNumberOfGCThreads ? 1U : workers),
    _created_workers(0),
    _name(name),
    _are_GC_task_threads(are_GC_task_threads),
    _are_ConcurrentGC_threads(are_ConcurrentGC_threads),
    _dispatcher(new GangTaskDispatcher())
  { }

WorkGang::~WorkGang() {
  delete _dispatcher;
}

// The current implementation will exit if the allocation
// of any worker fails.
void WorkGang::initialize_workers() {
  log_develop_trace(gc, workgang)("Constructing work gang %s with %u threads", name(), total_workers());
  _workers = NEW_C_HEAP_ARRAY(GangWorker*, total_workers(), mtInternal);
  add_workers(true);
}


GangWorker* WorkGang::install_worker(uint worker_id) {
  GangWorker* new_worker = allocate_worker(worker_id);
  set_thread(worker_id, new_worker);
  return new_worker;
}

void WorkGang::add_workers(bool initializing) {
  os::ThreadType worker_type;
  if (are_ConcurrentGC_threads()) {
    worker_type = os::cgc_thread;
  } else {
    worker_type = os::pgc_thread;
  }
  uint previous_created_workers = _created_workers;

  _created_workers = WorkerManager::add_workers(this,
                                                _active_workers,
                                                _total_workers,
                                                _created_workers,
                                                worker_type,
                                                initializing);
  _active_workers = MIN2(_created_workers, _active_workers);

  WorkerManager::log_worker_creation(this, previous_created_workers, _active_workers, _created_workers, initializing);
}

GangWorker* WorkGang::worker(uint i) const {
  // Array index bounds checking.
  GangWorker* result = NULL;
  assert(_workers != NULL, "No workers for indexing");
  assert(i < total_workers(), "Worker index out of bounds");
  result = _workers[i];
  assert(result != NULL, "Indexing to null worker");
  return result;
}

void WorkGang::threads_do(ThreadClosure* tc) const {
  assert(tc != NULL, "Null ThreadClosure");
  uint workers = created_workers();
  for (uint i = 0; i < workers; i++) {
    tc->do_thread(worker(i));
  }
}

GangWorker* WorkGang::allocate_worker(uint worker_id) {
  return new GangWorker(this, worker_id);
}

void WorkGang::run_task(AbstractGangTask* task) {
  run_task(task, active_workers());
}

void WorkGang::run_task(AbstractGangTask* task, uint num_workers, bool add_foreground_work) {
  guarantee(num_workers <= total_workers(),
            "Trying to execute task %s with %u workers which is more than the amount of total workers %u.",
            task->name(), num_workers, total_workers());
  guarantee(num_workers > 0, "Trying to execute task %s with zero workers", task->name());
  uint old_num_workers = _active_workers;
  update_active_workers(num_workers);
  _dispatcher->coordinator_execute_on_workers(task, num_workers, add_foreground_work);
  update_active_workers(old_num_workers);
}

GangWorker::GangWorker(WorkGang* gang, uint id) {
  _gang = gang;
  set_id(id);
  set_name("%s#%d", gang->name(), id);
}

void GangWorker::run() {
  initialize();
  loop();
}

void GangWorker::initialize() {
  assert(_gang != NULL, "No gang to run in");
  os::set_priority(this, NearMaxPriority);
  log_develop_trace(gc, workgang)("Running gang worker for gang %s id %u", gang()->name(), id());
  assert(!Thread::current()->is_VM_thread(), "VM thread should not be part"
         " of a work gang");
}

WorkData GangWorker::wait_for_task() {
  return gang()->dispatcher()->worker_wait_for_task();
}

void GangWorker::signal_task_done() {
  gang()->dispatcher()->worker_done_with_task();
}

void GangWorker::run_task(WorkData data) {
  GCIdMark gc_id_mark(data._task->gc_id());
  log_develop_trace(gc, workgang)("Running work gang: %s task: %s worker: %u", name(), data._task->name(), data._worker_id);

  data._task->work(data._worker_id);

  log_develop_trace(gc, workgang)("Finished work gang: %s task: %s worker: %u thread: " PTR_FORMAT,
                                  name(), data._task->name(), data._worker_id, p2i(Thread::current()));
}

void GangWorker::loop() {
  while (true) {
    WorkData data = wait_for_task();

    run_task(data);

    signal_task_done();
  }
}

// *** WorkGangBarrierSync

WorkGangBarrierSync::WorkGangBarrierSync()
  : _monitor(Mutex::safepoint, "work gang barrier sync", true,
             Monitor::_safepoint_check_never),
    _n_workers(0), _n_completed(0), _should_reset(false), _aborted(false) {
}

WorkGangBarrierSync::WorkGangBarrierSync(uint n_workers, const char* name)
  : _monitor(Mutex::safepoint, name, true, Monitor::_safepoint_check_never),
    _n_workers(n_workers), _n_completed(0), _should_reset(false), _aborted(false) {
}

void WorkGangBarrierSync::set_n_workers(uint n_workers) {
  _n_workers    = n_workers;
  _n_completed  = 0;
  _should_reset = false;
  _aborted      = false;
}

bool WorkGangBarrierSync::enter() {
  MonitorLocker ml(monitor(), Mutex::_no_safepoint_check_flag);
  if (should_reset()) {
    // The should_reset() was set and we are the first worker to enter
    // the sync barrier. We will zero the n_completed() count which
    // effectively resets the barrier.
    zero_completed();
    set_should_reset(false);
  }
  inc_completed();
  if (n_completed() == n_workers()) {
    // At this point we would like to reset the barrier to be ready in
    // case it is used again. However, we cannot set n_completed() to
    // 0, even after the notify_all(), given that some other workers
    // might still be waiting for n_completed() to become ==
    // n_workers(). So, if we set n_completed() to 0, those workers
    // will get stuck (as they will wake up, see that n_completed() !=
    // n_workers() and go back to sleep). Instead, we raise the
    // should_reset() flag and the barrier will be reset the first
    // time a worker enters it again.
    set_should_reset(true);
    ml.notify_all();
  } else {
    while (n_completed() != n_workers() && !aborted()) {
      ml.wait();
    }
  }
  return !aborted();
}

void WorkGangBarrierSync::abort() {
  MutexLocker x(monitor(), Mutex::_no_safepoint_check_flag);
  set_aborted();
  monitor()->notify_all();
}

// SubTasksDone functions.

SubTasksDone::SubTasksDone(uint n) :
  _tasks(NULL), _n_tasks(n) {
  _tasks = NEW_C_HEAP_ARRAY(bool, n, mtInternal);
  for (uint i = 0; i < _n_tasks; i++) {
    _tasks[i] = false;
  }
}

#ifdef ASSERT
void SubTasksDone::all_tasks_claimed_impl(uint skipped[], size_t skipped_size) {
  if (Atomic::cmpxchg(&_verification_done, false, true)) {
    // another thread has done the verification
    return;
  }
  // all non-skipped tasks are claimed
  for (uint i = 0; i < _n_tasks; ++i) {
    if (!_tasks[i]) {
      auto is_skipped = false;
      for (size_t j = 0; j < skipped_size; ++j) {
        if (i == skipped[j]) {
          is_skipped = true;
          break;
        }
      }
      assert(is_skipped, "%d not claimed.", i);
    }
  }
  // all skipped tasks are *not* claimed
  for (size_t i = 0; i < skipped_size; ++i) {
    auto task_index = skipped[i];
    assert(task_index < _n_tasks, "Array in range.");
    assert(!_tasks[task_index], "%d is both claimed and skipped.", task_index);
  }
}
#endif

bool SubTasksDone::try_claim_task(uint t) {
  assert(t < _n_tasks, "bad task id.");
  return !_tasks[t] && !Atomic::cmpxchg(&_tasks[t], false, true);
}

SubTasksDone::~SubTasksDone() {
  assert(_verification_done, "all_tasks_claimed must have been called.");
  FREE_C_HEAP_ARRAY(bool, _tasks);
}

// *** SequentialSubTasksDone

bool SequentialSubTasksDone::try_claim_task(uint& t) {
  t = _num_claimed;
  if (t < _num_tasks) {
    t = Atomic::add(&_num_claimed, 1u) - 1;
  }
  return t < _num_tasks;
}
