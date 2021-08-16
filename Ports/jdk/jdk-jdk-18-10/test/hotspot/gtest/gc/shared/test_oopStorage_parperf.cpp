/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/workgroup.hpp"
#include "logging/log.hpp"
#include "logging/logConfiguration.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/ostream.hpp"
#include "utilities/ticks.hpp"

#include "unittest.hpp"

// This "test" doesn't really verify much.  Rather, it's mostly a
// microbenchmark for OopStorage parallel iteration.  It executes
// parallel iteration with varying numbers of threads on an storage
// object containing a large number of entries, and logs some stats
// about the distribution and performance of the iteration.

const uint _max_workers = 10;
static uint _num_workers = 0;
const size_t _storage_entries = 1000000;

class OopStorageParIterPerf : public ::testing::Test {
public:
  OopStorageParIterPerf();
  ~OopStorageParIterPerf();

  WorkGang* workers() const;

  class VM_ParStateTime;
  class Task;
  class Closure;

  Tickspan run_task(Task* task, uint nthreads);
  void show_task(const Task* task, Tickspan duration, uint nthreads);
  void run_test(uint nthreads);

  static WorkGang* _workers;

  OopStorage _storage;
  oop* _entries[_storage_entries];
};

WorkGang* OopStorageParIterPerf::_workers = NULL;

WorkGang* OopStorageParIterPerf::workers() const {
  if (_workers == NULL) {
    WorkGang* wg = new WorkGang("OopStorageParIterPerf workers",
                                _num_workers,
                                false,
                                false);
    wg->initialize_workers();
    wg->update_active_workers(_num_workers);
    _workers = wg;
  }
  return _workers;
}

OopStorageParIterPerf::OopStorageParIterPerf() :
  _storage("Test Storage", mtGC)
{
  for (size_t i = 0; i < _storage_entries; ++i) {
    _entries[i] = _storage.allocate();
  }
  _num_workers = MIN2(_max_workers, (uint)os::processor_count());
}

OopStorageParIterPerf::~OopStorageParIterPerf() {
  _storage.release(_entries, ARRAY_SIZE(_entries));
}

class OopStorageParIterPerf::VM_ParStateTime : public VM_GTestExecuteAtSafepoint {
public:
  VM_ParStateTime(WorkGang* workers, AbstractGangTask* task, uint nthreads) :
    _workers(workers), _task(task), _nthreads(nthreads)
  {}

  void doit() {
    _workers->run_task(_task, _nthreads);
  }

private:
  WorkGang* _workers;
  AbstractGangTask* _task;
  uint _nthreads;
};

class OopStorageParIterPerf::Task : public AbstractGangTask {
  typedef OopStorage::ParState<false, false> StateType;

  Tickspan* _worker_times;
  StateType _state;
  OopClosure* _closure;

public:
  Task(OopStorage* storage, OopClosure* closure, uint nthreads) :
    AbstractGangTask("OopStorageParIterPerf::Task"),
    _worker_times(NULL),
    _state(storage, nthreads),
    _closure(closure)
  {
    Tickspan* wtimes = NEW_C_HEAP_ARRAY(Tickspan, _num_workers, mtInternal);
    for (uint i = 0; i < _num_workers; ++i) {
      new (&wtimes[i]) Tickspan();
    }
    _worker_times = wtimes;
  }

  ~Task() {
    FREE_C_HEAP_ARRAY(Tickspan, _worker_times);
  }

  virtual void work(uint worker_id) {
    Ticks start_time = Ticks::now();
    _state.oops_do(_closure);
    _worker_times[worker_id] = Ticks::now() - start_time;
  }

  const Tickspan* worker_times() const { return _worker_times; }
};

class OopStorageParIterPerf::Closure : public OopClosure {
public:
  virtual void do_oop(oop* p) { guarantee(*p == NULL, "expected NULL"); }
  virtual void do_oop(narrowOop* p) { ShouldNotReachHere(); }
};

Tickspan OopStorageParIterPerf::run_task(Task* task, uint nthreads) {
  tty->print_cr("Running test with %u threads", nthreads);
  VM_ParStateTime op(workers(), task, nthreads);
  ThreadInVMfromNative invm(JavaThread::current());
  Ticks start_time = Ticks::now();
  VMThread::execute(&op);
  return Ticks::now() - start_time;
}

void OopStorageParIterPerf::show_task(const Task* task, Tickspan duration, uint nthreads) {
  tty->print_cr("Run test with %u threads: " JLONG_FORMAT, nthreads, duration.value());
  const Tickspan* wtimes = task->worker_times();
  for (uint i = 0; i < _num_workers; ++i) {
    if (wtimes[i] != Tickspan()) {
      tty->print_cr("  %u: " JLONG_FORMAT, i, wtimes[i].value());
    }
  }
  tty->cr();
}

void OopStorageParIterPerf::run_test(uint nthreads) {
  if (nthreads <= _num_workers) {
    SCOPED_TRACE(err_msg("Running test with %u threads", nthreads).buffer());
    Closure closure;
    Task task(&_storage, &closure, nthreads);
    Tickspan t = run_task(&task, nthreads);
    show_task(&task, t, nthreads);
  }
}

TEST_VM_F(OopStorageParIterPerf, test) {
  // Enable additional interesting logging.
#define TEST_TAGS oopstorage, blocks, stats
  // There isn't an obvious way to capture the old log level so it
  // can be restored here, so just use Warning as the "default".
  LogLevelType old_level = LogLevel::Warning;
  if (log_is_enabled(Debug, TEST_TAGS)) {
    old_level = LogLevel::Debug;
  } else if (log_is_enabled(Info, TEST_TAGS)) {
    old_level = LogLevel::Info;
  }
  bool debug_enabled = old_level == LogLevel::Debug;
  if (!debug_enabled) {
    LogConfiguration::configure_stdout(LogLevel::Debug, true, LOG_TAGS(TEST_TAGS));
  }

  run_test(1);
  run_test(2);
  run_test(3);
  run_test(4);
  run_test(6);
  run_test(8);
  run_test(10);

  if (!debug_enabled) {
    LogConfiguration::configure_stdout(old_level, true, LOG_TAGS(TEST_TAGS));
  }
}
