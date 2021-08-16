/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalCounter.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "utilities/singleWriterSynchronizer.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

class SingleWriterSynchronizerTestReader : public JavaTestThread {
  SingleWriterSynchronizer* _synchronizer;
  volatile uintx* _synchronized_value;
  volatile int* _continue_running;

  static const uint reader_iterations = 10;

public:
  SingleWriterSynchronizerTestReader(Semaphore* post,
                                     SingleWriterSynchronizer* synchronizer,
                                     volatile uintx* synchronized_value,
                                     volatile int* continue_running) :
    JavaTestThread(post),
    _synchronizer(synchronizer),
    _synchronized_value(synchronized_value),
    _continue_running(continue_running)
  {}

  virtual void main_run() {
    size_t iterations = 0;
    size_t values_changed = 0;
    while (Atomic::load_acquire(_continue_running) != 0) {
      { ThreadBlockInVM tbiv(this); } // Safepoint check outside critical section.
      ++iterations;
      SingleWriterSynchronizer::CriticalSection cs(_synchronizer);
      uintx value = Atomic::load_acquire(_synchronized_value);
      uintx new_value = value;
      for (uint i = 0; i < reader_iterations; ++i) {
        new_value = Atomic::load_acquire(_synchronized_value);
        // A reader can see either the value it first read after
        // entering the critical section, or that value + 1.  No other
        // values are possible.
        if (value != new_value) {
          ASSERT_EQ((value + 1), new_value);
        }
      }
      if (value != new_value) {
        ++values_changed;
      }
    }
    tty->print_cr("reader iterations: " SIZE_FORMAT ", changes: " SIZE_FORMAT,
                  iterations, values_changed);
  }
};

class SingleWriterSynchronizerTestWriter : public JavaTestThread {
  SingleWriterSynchronizer* _synchronizer;
  volatile uintx* _synchronized_value;
  volatile int* _continue_running;

public:
  SingleWriterSynchronizerTestWriter(Semaphore* post,
                                     SingleWriterSynchronizer* synchronizer,
                                     volatile uintx* synchronized_value,
                                     volatile int* continue_running) :
    JavaTestThread(post),
    _synchronizer(synchronizer),
    _synchronized_value(synchronized_value),
    _continue_running(continue_running)
  {}

  virtual void main_run() {
    while (Atomic::load_acquire(_continue_running) != 0) {
      ++*_synchronized_value;
      _synchronizer->synchronize();
      { ThreadBlockInVM tbiv(this); } // Safepoint check.
    }
    tty->print_cr("writer iterations: " UINTX_FORMAT, *_synchronized_value);
  }
};

const uint nreaders = 5;
const uint milliseconds_to_run = 1000;

TEST_VM(TestSingleWriterSynchronizer, stress) {
  Semaphore post;
  SingleWriterSynchronizer synchronizer;
  volatile uintx synchronized_value = 0;
  volatile int continue_running = 1;

  JavaTestThread* readers[nreaders] = {};
  for (uint i = 0; i < nreaders; ++i) {
    readers[i] = new SingleWriterSynchronizerTestReader(&post,
                                                        &synchronizer,
                                                        &synchronized_value,
                                                        &continue_running);
    readers[i]->doit();
  }

  JavaTestThread* writer =
    new SingleWriterSynchronizerTestWriter(&post,
                                           &synchronizer,
                                           &synchronized_value,
                                           &continue_running);

  writer->doit();

  tty->print_cr("Stressing synchronizer for %u ms", milliseconds_to_run);
  JavaThread* cur = JavaThread::current();
  {
    ThreadInVMfromNative invm(cur);
    cur->sleep(milliseconds_to_run);
  }
  continue_running = 0;
  for (uint i = 0; i < nreaders + 1; ++i) {
    post.wait();
  }
}
