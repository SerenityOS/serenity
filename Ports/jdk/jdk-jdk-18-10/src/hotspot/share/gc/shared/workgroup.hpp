/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_WORKGROUP_HPP
#define SHARE_GC_SHARED_WORKGROUP_HPP

#include "memory/allocation.hpp"
#include "metaprogramming/enableIf.hpp"
#include "metaprogramming/logical.hpp"
#include "runtime/globals.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/thread.hpp"
#include "gc/shared/gcId.hpp"
#include "logging/log.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// Task class hierarchy:
//   AbstractGangTask
//
// Gang/Group class hierarchy:
//   WorkGang
//
// Worker class hierarchy:
//   GangWorker (subclass of WorkerThread)

// Forward declarations of classes defined here

class GangWorker;
class Semaphore;
class ThreadClosure;
class GangTaskDispatcher;

// An abstract task to be worked on by a gang.
// You subclass this to supply your own work() method
class AbstractGangTask : public CHeapObj<mtInternal> {
  const char* _name;
  const uint _gc_id;

 public:
  explicit AbstractGangTask(const char* name) :
    _name(name),
    _gc_id(GCId::current_or_undefined())
  {}

  // The abstract work method.
  // The argument tells you which member of the gang you are.
  virtual void work(uint worker_id) = 0;

  // Debugging accessor for the name.
  const char* name() const { return _name; }
  const uint gc_id() const { return _gc_id; }
};

struct WorkData {
  AbstractGangTask* _task;
  uint              _worker_id;
  WorkData(AbstractGangTask* task, uint worker_id) : _task(task), _worker_id(worker_id) {}
};

// The work gang is the collection of workers to execute tasks.
// The number of workers run for a task is "_active_workers"
// while "_total_workers" is the number of available workers.
class WorkGang : public CHeapObj<mtInternal> {
  // The array of worker threads for this gang.
  GangWorker** _workers;
  // The count of the number of workers in the gang.
  uint _total_workers;
  // The currently active workers in this gang.
  uint _active_workers;
  // The count of created workers in the gang.
  uint _created_workers;
  // Printing support.
  const char* _name;

  // Initialize only instance data.
  const bool _are_GC_task_threads;
  const bool _are_ConcurrentGC_threads;

  // To get access to the GangTaskDispatcher instance.
  friend class GangWorker;
  GangTaskDispatcher* const _dispatcher;

  GangTaskDispatcher* dispatcher() const { return _dispatcher; }

  void set_thread(uint worker_id, GangWorker* worker) {
    _workers[worker_id] = worker;
  }

  // Add GC workers when _created_workers < _active_workers; otherwise, no-op.
  // If there's no memory/thread allocation failure, _created_worker is
  // adjusted to match _active_workers (_created_worker == _active_workers).
  void add_workers(bool initializing);

  GangWorker* allocate_worker(uint which);

 public:
  WorkGang(const char* name, uint workers, bool are_GC_task_threads, bool are_ConcurrentGC_threads);

  ~WorkGang();

  // Initialize workers in the gang.  Return true if initialization succeeded.
  void initialize_workers();

  bool are_GC_task_threads()      const { return _are_GC_task_threads; }
  bool are_ConcurrentGC_threads() const { return _are_ConcurrentGC_threads; }

  uint total_workers() const { return _total_workers; }

  uint created_workers() const {
    return _created_workers;
  }

  uint active_workers() const {
    assert(_active_workers != 0, "zero active workers");
    assert(_active_workers <= _total_workers,
           "_active_workers: %u > _total_workers: %u", _active_workers, _total_workers);
    return _active_workers;
  }

  uint update_active_workers(uint v) {
    assert(v <= _total_workers,
           "Trying to set more workers active than there are");
    assert(v != 0, "Trying to set active workers to 0");
    _active_workers = v;
    add_workers(false /* initializing */);
    log_trace(gc, task)("%s: using %d out of %d workers", name(), _active_workers, _total_workers);
    return _active_workers;
  }

  // Return the Ith worker.
  GangWorker* worker(uint i) const;

  // Base name (without worker id #) of threads.
  const char* group_name() { return name(); }

  void threads_do(ThreadClosure* tc) const;

  // Create a GC worker and install it into the work gang.
  virtual GangWorker* install_worker(uint which);

  // Debugging.
  const char* name() const { return _name; }

  // Run a task using the current active number of workers, returns when the task is done.
  void run_task(AbstractGangTask* task);

  // Run a task with the given number of workers, returns
  // when the task is done. The number of workers must be at most the number of
  // active workers.  Additional workers may be created if an insufficient
  // number currently exists. If the add_foreground_work flag is true, the current thread
  // is used to run the task too.
  void run_task(AbstractGangTask* task, uint num_workers, bool add_foreground_work = false);
};

// Temporarily try to set the number of active workers.
// It's not guaranteed that it succeeds, and users need to
// query the number of active workers.
class WithUpdatedActiveWorkers : public StackObj {
private:
  WorkGang* const _gang;
  const uint              _old_active_workers;

public:
  WithUpdatedActiveWorkers(WorkGang* gang, uint requested_num_workers) :
      _gang(gang),
      _old_active_workers(gang->active_workers()) {
    uint capped_num_workers = MIN2(requested_num_workers, gang->total_workers());
    gang->update_active_workers(capped_num_workers);
  }

  ~WithUpdatedActiveWorkers() {
    _gang->update_active_workers(_old_active_workers);
  }
};

// Several instances of this class run in parallel as workers for a gang.
class GangWorker: public WorkerThread {
private:
  WorkGang* _gang;

  void initialize();
  void loop();

  WorkGang* gang() const { return _gang; }

  WorkData wait_for_task();
  void run_task(WorkData work);
  void signal_task_done();

protected:
  // The only real method: run a task for the gang.
  void run() override;

public:
  GangWorker(WorkGang* gang, uint id);

  // Predicate for Thread
  bool is_GC_task_thread() const override { return gang()->are_GC_task_threads(); }
  bool is_ConcurrentGC_thread() const override { return gang()->are_ConcurrentGC_threads(); }

  // Printing
  const char* type_name() const override { return "GCTaskThread"; }
};

// A class that acts as a synchronisation barrier. Workers enter
// the barrier and must wait until all other workers have entered
// before any of them may leave.

class WorkGangBarrierSync : public StackObj {
protected:
  Monitor _monitor;
  uint    _n_workers;
  uint    _n_completed;
  bool    _should_reset;
  bool    _aborted;

  Monitor* monitor()        { return &_monitor; }
  uint     n_workers()      { return _n_workers; }
  uint     n_completed()    { return _n_completed; }
  bool     should_reset()   { return _should_reset; }
  bool     aborted()        { return _aborted; }

  void     zero_completed() { _n_completed = 0; }
  void     inc_completed()  { _n_completed++; }
  void     set_aborted()    { _aborted = true; }
  void     set_should_reset(bool v) { _should_reset = v; }

public:
  WorkGangBarrierSync();
  WorkGangBarrierSync(uint n_workers, const char* name);

  // Set the number of workers that will use the barrier.
  // Must be called before any of the workers start running.
  void set_n_workers(uint n_workers);

  // Enter the barrier. A worker that enters the barrier will
  // not be allowed to leave until all other threads have
  // also entered the barrier or the barrier is aborted.
  // Returns false if the barrier was aborted.
  bool enter();

  // Aborts the barrier and wakes up any threads waiting for
  // the barrier to complete. The barrier will remain in the
  // aborted state until the next call to set_n_workers().
  void abort();
};

// A class to manage claiming of subtasks within a group of tasks.  The
// subtasks will be identified by integer indices, usually elements of an
// enumeration type.

class SubTasksDone: public CHeapObj<mtInternal> {
  volatile bool* _tasks;
  uint _n_tasks;

  // make sure verification logic is run exactly once to avoid duplicate assertion failures
  DEBUG_ONLY(volatile bool _verification_done = false;)
  void all_tasks_claimed_impl(uint skipped[], size_t skipped_size) NOT_DEBUG_RETURN;

  NONCOPYABLE(SubTasksDone);

public:
  // Initializes "this" to a state in which there are "n" tasks to be
  // processed, none of the which are originally claimed.
  SubTasksDone(uint n);

  // Attempt to claim the task "t", returning true if successful,
  // false if it has already been claimed.  The task "t" is required
  // to be within the range of "this".
  bool try_claim_task(uint t);

  // The calling thread asserts that it has attempted to claim all the tasks
  // that it will try to claim.  Tasks that are meant to be skipped must be
  // explicitly passed as extra arguments. Every thread in the parallel task
  // must execute this.
  template<typename T0, typename... Ts,
          ENABLE_IF(Conjunction<std::is_same<T0, Ts>...>::value)>
  void all_tasks_claimed(T0 first_skipped, Ts... more_skipped) {
    static_assert(std::is_convertible<T0, uint>::value, "not convertible");
    uint skipped[] = { static_cast<uint>(first_skipped), static_cast<uint>(more_skipped)... };
    all_tasks_claimed_impl(skipped, ARRAY_SIZE(skipped));
  }
  // if there are no skipped tasks.
  void all_tasks_claimed() {
    all_tasks_claimed_impl(nullptr, 0);
  }

  // Destructor.
  ~SubTasksDone();
};

// As above, but for sequential tasks, i.e. instead of claiming
// sub-tasks from a set (possibly an enumeration), claim sub-tasks
// in sequential order. This is ideal for claiming dynamically
// partitioned tasks (like striding in the parallel remembered
// set scanning).

class SequentialSubTasksDone : public CHeapObj<mtInternal> {

  uint _num_tasks;     // Total number of tasks available.
  volatile uint _num_claimed;   // Number of tasks claimed.

  NONCOPYABLE(SequentialSubTasksDone);

public:
  SequentialSubTasksDone(uint num_tasks) : _num_tasks(num_tasks), _num_claimed(0) { }
  ~SequentialSubTasksDone() {
    // Claiming may try to claim more tasks than there are.
    assert(_num_claimed >= _num_tasks, "Claimed %u tasks of %u", _num_claimed, _num_tasks);
  }

  // Attempt to claim the next unclaimed task in the sequence,
  // returning true if successful, with t set to the index of the
  // claimed task. Returns false if there are no more unclaimed tasks
  // in the sequence. In this case t is undefined.
  bool try_claim_task(uint& t);
};

#endif // SHARE_GC_SHARED_WORKGROUP_HPP
