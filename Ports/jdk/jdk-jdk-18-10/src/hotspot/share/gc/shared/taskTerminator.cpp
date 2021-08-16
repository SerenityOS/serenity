/*
 * Copyright (c) 2018, 2020, Red Hat, Inc. All rights reserved.
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/taskqueue.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"

TaskTerminator::DelayContext::DelayContext() {
  _yield_count = 0;
  reset_hard_spin_information();
}

void TaskTerminator::DelayContext::reset_hard_spin_information() {
  _hard_spin_count = 0;
  _hard_spin_limit = WorkStealingHardSpins >> WorkStealingSpinToYieldRatio;
}

bool TaskTerminator::DelayContext::needs_sleep() const {
  return _yield_count >= WorkStealingYieldsBeforeSleep;
}

void TaskTerminator::DelayContext::do_step() {
  assert(_yield_count < WorkStealingYieldsBeforeSleep, "Number of yields too large");
  // Each spin iteration is counted as a yield for purposes of
  // deciding when to sleep.
  _yield_count++;
  // Periodically yield instead of spinning after WorkStealingSpinToYieldRatio
  // spins.
  if (_hard_spin_count > WorkStealingSpinToYieldRatio) {
    os::naked_yield();
    reset_hard_spin_information();
  } else {
    // Hard spin this time
    for (uint j = 0; j < _hard_spin_limit; j++) {
      SpinPause();
    }
    _hard_spin_count++;
    // Increase the hard spinning period but only up to a limit.
    _hard_spin_limit = MIN2(2 * _hard_spin_limit,
                            (uint) WorkStealingHardSpins);
  }
}

TaskTerminator::TaskTerminator(uint n_threads, TaskQueueSetSuper* queue_set) :
  _n_threads(n_threads),
  _queue_set(queue_set),
  _offered_termination(0),
  _blocker(Mutex::leaf, "TaskTerminator", false, Monitor::_safepoint_check_never),
  _spin_master(NULL) { }

TaskTerminator::~TaskTerminator() {
  if (_offered_termination != 0) {
    assert(_offered_termination == _n_threads, "Must be terminated or aborted");
  }

  assert(_spin_master == NULL, "Should have been reset");
}

#ifdef ASSERT
void TaskTerminator::assert_queue_set_empty() const {
  _queue_set->assert_empty();
}
#endif

void TaskTerminator::reset_for_reuse() {
  if (_offered_termination != 0) {
    assert(_offered_termination == _n_threads,
           "Only %u of %u threads offered termination", _offered_termination, _n_threads);
    assert(_spin_master == NULL, "Leftover spin master " PTR_FORMAT, p2i(_spin_master));
    _offered_termination = 0;
  }
}

void TaskTerminator::reset_for_reuse(uint n_threads) {
  reset_for_reuse();
  _n_threads = n_threads;
}

bool TaskTerminator::exit_termination(size_t tasks, TerminatorTerminator* terminator) {
  return tasks > 0 || (terminator != NULL && terminator->should_exit_termination());
}

size_t TaskTerminator::tasks_in_queue_set() const {
  return _queue_set->tasks();
}

void TaskTerminator::prepare_for_return(Thread* this_thread, size_t tasks) {
  assert(_blocker.is_locked(), "must be");
  assert(_blocker.owned_by_self(), "must be");
  assert(_offered_termination >= 1, "must be");

  if (_spin_master == this_thread) {
    _spin_master = NULL;
  }

  if (tasks >= _offered_termination - 1) {
    _blocker.notify_all();
  } else {
    for (; tasks > 1; tasks--) {
      _blocker.notify();
    }
  }
}

bool TaskTerminator::offer_termination(TerminatorTerminator* terminator) {
  assert(_n_threads > 0, "Initialization is incorrect");
  assert(_offered_termination < _n_threads, "Invariant");

  // Single worker, done
  if (_n_threads == 1) {
    _offered_termination = 1;
    assert_queue_set_empty();
    return true;
  }

  Thread* the_thread = Thread::current();

  MonitorLocker x(&_blocker, Mutex::_no_safepoint_check_flag);
  _offered_termination++;

  if (_offered_termination == _n_threads) {
    prepare_for_return(the_thread);
    assert_queue_set_empty();
    return true;
  }

  for (;;) {
    if (_spin_master == NULL) {
      _spin_master = the_thread;
      DelayContext delay_context;

      while (!delay_context.needs_sleep()) {
        size_t tasks;
        bool should_exit_termination;
        {
          MutexUnlocker y(&_blocker, Mutex::_no_safepoint_check_flag);
          delay_context.do_step();
          // Intentionally read the number of tasks outside the mutex since this
          // is potentially a long operation making the locked section long.
          tasks = tasks_in_queue_set();
          should_exit_termination = exit_termination(tasks, terminator);
        }
        // Immediately check exit conditions after re-acquiring the lock.
        if (_offered_termination == _n_threads) {
          prepare_for_return(the_thread);
          assert_queue_set_empty();
          return true;
        } else if (should_exit_termination) {
          prepare_for_return(the_thread, tasks);
          _offered_termination--;
          return false;
        }
      }
      // Give up spin master before sleeping.
      _spin_master = NULL;
    }
    bool timed_out = x.wait(WorkStealingSleepMillis);

    // Immediately check exit conditions after re-acquiring the lock.
    if (_offered_termination == _n_threads) {
      prepare_for_return(the_thread);
      assert_queue_set_empty();
      return true;
    } else if (!timed_out) {
      // We were woken up. Don't bother waking up more tasks.
      prepare_for_return(the_thread, 0);
      _offered_termination--;
      return false;
    } else {
      size_t tasks = tasks_in_queue_set();
      if (exit_termination(tasks, terminator)) {
        prepare_for_return(the_thread, tasks);
        _offered_termination--;
        return false;
      }
    }
  }
}
