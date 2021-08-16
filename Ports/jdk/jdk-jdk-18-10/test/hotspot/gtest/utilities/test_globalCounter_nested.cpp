/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "utilities/globalCounter.hpp"
#include "utilities/globalCounter.inline.hpp"
#include "utilities/spinYield.hpp"
#include "threadHelper.inline.hpp"

enum NestedTestState {
  START,
  START_WAIT,
  OUTER_ENTERED,
  INNER_ENTERED,
  INNER_EXITED,
  OUTER_EXITED,
  SYNCHRONIZING,
  SYNCHRONIZED
};

class RCUNestedThread : public JavaTestThread {
  volatile NestedTestState _state;
  volatile bool _proceed;

protected:
  RCUNestedThread(Semaphore* post) :
    JavaTestThread(post),
    _state(START),
    _proceed(false)
  {}

  ~RCUNestedThread() {}

  void set_state(NestedTestState new_state) {
    Atomic::release_store(&_state, new_state);
  }

  void wait_with_state(NestedTestState new_state) {
    SpinYield spinner;
    Atomic::release_store(&_state, new_state);
    while (!Atomic::load_acquire(&_proceed)) {
      spinner.wait();
    }
    Atomic::release_store(&_proceed, false);
  }

public:
  NestedTestState state() const {
    return Atomic::load_acquire(&_state);
  }

  void wait_for_state(NestedTestState goal) {
    SpinYield spinner;
    while (state() != goal) {
      spinner.wait();
    }
  }

  void proceed() {
    Atomic::release_store(&_proceed, true);
  }
};

class RCUNestedReaderThread : public RCUNestedThread {
public:
  RCUNestedReaderThread(Semaphore* post) :
    RCUNestedThread(post)
  {}

  virtual void main_run();
};

void RCUNestedReaderThread::main_run() {
  wait_with_state(START_WAIT);
  {
    GlobalCounter::CriticalSection outer(Thread::current());
    wait_with_state(OUTER_ENTERED);
    {
      GlobalCounter::CriticalSection inner(Thread::current());
      wait_with_state(INNER_ENTERED);
    }
    wait_with_state(INNER_EXITED);
  }
  wait_with_state(OUTER_EXITED);
}


class RCUNestedWriterThread : public RCUNestedThread {
public:
  RCUNestedWriterThread(Semaphore* post) :
    RCUNestedThread(post)
  {}

  virtual void main_run();
};

void RCUNestedWriterThread::main_run() {
  wait_with_state(START_WAIT);
  set_state(SYNCHRONIZING);
  GlobalCounter::write_synchronize();
  wait_with_state(SYNCHRONIZED);
}

TEST_VM(GlobalCounter, nested_critical_section) {
  Semaphore post;
  RCUNestedReaderThread* reader = new RCUNestedReaderThread(&post);
  RCUNestedWriterThread* outer = new RCUNestedWriterThread(&post);
  RCUNestedWriterThread* inner = new RCUNestedWriterThread(&post);

  reader->doit();
  outer->doit();
  inner->doit();

  reader->wait_for_state(START_WAIT);
  outer->wait_for_state(START_WAIT);
  inner->wait_for_state(START_WAIT);
  EXPECT_EQ(START_WAIT, reader->state());
  EXPECT_EQ(START_WAIT, outer->state());
  EXPECT_EQ(START_WAIT, inner->state());

  reader->proceed();
  reader->wait_for_state(OUTER_ENTERED);
  EXPECT_EQ(OUTER_ENTERED, reader->state());
  EXPECT_EQ(START_WAIT, outer->state());
  EXPECT_EQ(START_WAIT, inner->state());

  outer->proceed();
  outer->wait_for_state(SYNCHRONIZING);
  EXPECT_EQ(OUTER_ENTERED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(START_WAIT, inner->state());

  os::naked_short_sleep(100);   // Give outer time in synchronization.
  EXPECT_EQ(OUTER_ENTERED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(START_WAIT, inner->state());

  reader->proceed();
  reader->wait_for_state(INNER_ENTERED);
  EXPECT_EQ(INNER_ENTERED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(START_WAIT, inner->state());

  inner->proceed();
  inner->wait_for_state(SYNCHRONIZING);
  EXPECT_EQ(INNER_ENTERED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(SYNCHRONIZING, inner->state());

  os::naked_short_sleep(100); // Give writers time in synchronization.
  EXPECT_EQ(INNER_ENTERED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(SYNCHRONIZING, inner->state());

  reader->proceed();
  reader->wait_for_state(INNER_EXITED);
  // inner does *not* complete synchronization here.
  EXPECT_EQ(INNER_EXITED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(SYNCHRONIZING, inner->state());

  os::naked_short_sleep(100); // Give writers more time in synchronization.
  EXPECT_EQ(INNER_EXITED, reader->state());
  EXPECT_EQ(SYNCHRONIZING, outer->state());
  EXPECT_EQ(SYNCHRONIZING, inner->state());

  reader->proceed();
  reader->wait_for_state(OUTER_EXITED);
  // Both inner and outer can synchronize now.
  outer->wait_for_state(SYNCHRONIZED);
  inner->wait_for_state(SYNCHRONIZED);
  EXPECT_EQ(OUTER_EXITED, reader->state());
  EXPECT_EQ(SYNCHRONIZED, outer->state());
  EXPECT_EQ(SYNCHRONIZED, inner->state());

  // Wait for reader, outer, and inner to complete.
  reader->proceed();
  outer->proceed();
  inner->proceed();
  for (uint i = 0; i < 3; ++i) {
    post.wait();
  }
}
