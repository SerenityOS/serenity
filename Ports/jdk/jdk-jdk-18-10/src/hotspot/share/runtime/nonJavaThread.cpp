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
#include "jvm_io.h"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/gcId.hpp"
#include "runtime/atomic.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/osThread.hpp"
#include "runtime/task.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/singleWriterSynchronizer.hpp"
#include "utilities/vmError.hpp"

#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

// List of all NonJavaThreads and safe iteration over that list.

class NonJavaThread::List {
public:
  NonJavaThread* volatile _head;
  SingleWriterSynchronizer _protect;

  List() : _head(NULL), _protect() {}
};

NonJavaThread::List NonJavaThread::_the_list;

NonJavaThread::Iterator::Iterator() :
  _protect_enter(_the_list._protect.enter()),
  _current(Atomic::load_acquire(&_the_list._head))
{}

NonJavaThread::Iterator::~Iterator() {
  _the_list._protect.exit(_protect_enter);
}

void NonJavaThread::Iterator::step() {
  assert(!end(), "precondition");
  _current = Atomic::load_acquire(&_current->_next);
}

NonJavaThread::NonJavaThread() : Thread(), _next(NULL) {
  assert(BarrierSet::barrier_set() != NULL, "NonJavaThread created too soon!");
}

NonJavaThread::~NonJavaThread() { }

void NonJavaThread::add_to_the_list() {
  MutexLocker ml(NonJavaThreadsList_lock, Mutex::_no_safepoint_check_flag);
  // Initialize BarrierSet-related data before adding to list.
  BarrierSet::barrier_set()->on_thread_attach(this);
  Atomic::release_store(&_next, _the_list._head);
  Atomic::release_store(&_the_list._head, this);
}

void NonJavaThread::remove_from_the_list() {
  {
    MutexLocker ml(NonJavaThreadsList_lock, Mutex::_no_safepoint_check_flag);
    // Cleanup BarrierSet-related data before removing from list.
    BarrierSet::barrier_set()->on_thread_detach(this);
    NonJavaThread* volatile* p = &_the_list._head;
    for (NonJavaThread* t = *p; t != NULL; p = &t->_next, t = *p) {
      if (t == this) {
        *p = _next;
        break;
      }
    }
  }
  // Wait for any in-progress iterators.  Concurrent synchronize is not
  // allowed, so do it while holding a dedicated lock.  Outside and distinct
  // from NJTList_lock in case an iteration attempts to lock it.
  MutexLocker ml(NonJavaThreadsListSync_lock, Mutex::_no_safepoint_check_flag);
  _the_list._protect.synchronize();
  _next = NULL;                 // Safe to drop the link now.
}

void NonJavaThread::pre_run() {
  add_to_the_list();

  // This is slightly odd in that NamedThread is a subclass, but
  // in fact name() is defined in Thread
  assert(this->name() != NULL, "thread name was not set before it was started");
  this->set_native_thread_name(this->name());
}

void NonJavaThread::post_run() {
  JFR_ONLY(Jfr::on_thread_exit(this);)
  remove_from_the_list();
  unregister_thread_stack_with_NMT();
  // Ensure thread-local-storage is cleared before termination.
  Thread::clear_thread_current();
  osthread()->set_state(ZOMBIE);
}

// NamedThread --  non-JavaThread subclasses with multiple
// uniquely named instances should derive from this.
NamedThread::NamedThread() :
  NonJavaThread(),
  _name(NULL),
  _processed_thread(NULL),
  _gc_id(GCId::undefined())
{}

NamedThread::~NamedThread() {
  FREE_C_HEAP_ARRAY(char, _name);
}

void NamedThread::set_name(const char* format, ...) {
  guarantee(_name == NULL, "Only get to set name once.");
  _name = NEW_C_HEAP_ARRAY(char, max_name_len, mtThread);
  va_list ap;
  va_start(ap, format);
  jio_vsnprintf(_name, max_name_len, format, ap);
  va_end(ap);
}

void NamedThread::print_on(outputStream* st) const {
  st->print("\"%s\" ", name());
  Thread::print_on(st);
  st->cr();
}


// ======= WatcherThread ========

// The watcher thread exists to simulate timer interrupts.  It should
// be replaced by an abstraction over whatever native support for
// timer interrupts exists on the platform.

WatcherThread* WatcherThread::_watcher_thread   = NULL;
bool WatcherThread::_startable = false;
volatile bool  WatcherThread::_should_terminate = false;

WatcherThread::WatcherThread() : NonJavaThread() {
  assert(watcher_thread() == NULL, "we can only allocate one WatcherThread");
  if (os::create_thread(this, os::watcher_thread)) {
    _watcher_thread = this;

    // Set the watcher thread to the highest OS priority which should not be
    // used, unless a Java thread with priority java.lang.Thread.MAX_PRIORITY
    // is created. The only normal thread using this priority is the reference
    // handler thread, which runs for very short intervals only.
    // If the VMThread's priority is not lower than the WatcherThread profiling
    // will be inaccurate.
    os::set_priority(this, MaxPriority);
    os::start_thread(this);
  }
}

int WatcherThread::sleep() const {
  // The WatcherThread does not participate in the safepoint protocol
  // for the PeriodicTask_lock because it is not a JavaThread.
  MonitorLocker ml(PeriodicTask_lock, Mutex::_no_safepoint_check_flag);

  if (_should_terminate) {
    // check for termination before we do any housekeeping or wait
    return 0;  // we did not sleep.
  }

  // remaining will be zero if there are no tasks,
  // causing the WatcherThread to sleep until a task is
  // enrolled
  int remaining = PeriodicTask::time_to_wait();
  int time_slept = 0;

  // we expect this to timeout - we only ever get unparked when
  // we should terminate or when a new task has been enrolled
  OSThreadWaitState osts(this->osthread(), false /* not Object.wait() */);

  jlong time_before_loop = os::javaTimeNanos();

  while (true) {
    bool timedout = ml.wait(remaining);
    jlong now = os::javaTimeNanos();

    if (remaining == 0) {
      // if we didn't have any tasks we could have waited for a long time
      // consider the time_slept zero and reset time_before_loop
      time_slept = 0;
      time_before_loop = now;
    } else {
      // need to recalculate since we might have new tasks in _tasks
      time_slept = (int) ((now - time_before_loop) / 1000000);
    }

    // Change to task list or spurious wakeup of some kind
    if (timedout || _should_terminate) {
      break;
    }

    remaining = PeriodicTask::time_to_wait();
    if (remaining == 0) {
      // Last task was just disenrolled so loop around and wait until
      // another task gets enrolled
      continue;
    }

    remaining -= time_slept;
    if (remaining <= 0) {
      break;
    }
  }

  return time_slept;
}

void WatcherThread::run() {
  assert(this == watcher_thread(), "just checking");

  this->set_active_handles(JNIHandleBlock::allocate_block());
  while (true) {
    assert(watcher_thread() == Thread::current(), "thread consistency check");
    assert(watcher_thread() == this, "thread consistency check");

    // Calculate how long it'll be until the next PeriodicTask work
    // should be done, and sleep that amount of time.
    int time_waited = sleep();

    if (VMError::is_error_reported()) {
      // A fatal error has happened, the error handler(VMError::report_and_die)
      // should abort JVM after creating an error log file. However in some
      // rare cases, the error handler itself might deadlock. Here periodically
      // check for error reporting timeouts, and if it happens, just proceed to
      // abort the VM.

      // This code is in WatcherThread because WatcherThread wakes up
      // periodically so the fatal error handler doesn't need to do anything;
      // also because the WatcherThread is less likely to crash than other
      // threads.

      for (;;) {
        // Note: we use naked sleep in this loop because we want to avoid using
        // any kind of VM infrastructure which may be broken at this point.
        if (VMError::check_timeout()) {
          // We hit error reporting timeout. Error reporting was interrupted and
          // will be wrapping things up now (closing files etc). Give it some more
          // time, then quit the VM.
          os::naked_short_sleep(200);
          // Print a message to stderr.
          fdStream err(defaultStream::output_fd());
          err.print_raw_cr("# [ timer expired, abort... ]");
          // skip atexit/vm_exit/vm_abort hooks
          os::die();
        }

        // Wait a second, then recheck for timeout.
        os::naked_short_sleep(999);
      }
    }

    if (_should_terminate) {
      // check for termination before posting the next tick
      break;
    }

    PeriodicTask::real_time_tick(time_waited);
  }

  // Signal that it is terminated
  {
    MutexLocker mu(Terminator_lock, Mutex::_no_safepoint_check_flag);
    _watcher_thread = NULL;
    Terminator_lock->notify_all();
  }
}

void WatcherThread::start() {
  assert(PeriodicTask_lock->owned_by_self(), "PeriodicTask_lock required");

  if (watcher_thread() == NULL && _startable) {
    _should_terminate = false;
    // Create the single instance of WatcherThread
    new WatcherThread();
  }
}

void WatcherThread::make_startable() {
  assert(PeriodicTask_lock->owned_by_self(), "PeriodicTask_lock required");
  _startable = true;
}

void WatcherThread::stop() {
  {
    // Follow normal safepoint aware lock enter protocol since the
    // WatcherThread is stopped by another JavaThread.
    MutexLocker ml(PeriodicTask_lock);
    _should_terminate = true;

    WatcherThread* watcher = watcher_thread();
    if (watcher != NULL) {
      // unpark the WatcherThread so it can see that it should terminate
      watcher->unpark();
    }
  }

  MonitorLocker mu(Terminator_lock);

  while (watcher_thread() != NULL) {
    // This wait should make safepoint checks and wait without a timeout.
    mu.wait(0);
  }
}

void WatcherThread::unpark() {
  assert(PeriodicTask_lock->owned_by_self(), "PeriodicTask_lock required");
  PeriodicTask_lock->notify();
}

void WatcherThread::print_on(outputStream* st) const {
  st->print("\"%s\" ", name());
  Thread::print_on(st);
  st->cr();
}

