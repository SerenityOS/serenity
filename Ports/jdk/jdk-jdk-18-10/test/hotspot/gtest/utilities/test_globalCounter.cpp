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
#include "threadHelper.inline.hpp"

#define GOOD_VALUE 1337
#define BAD_VALUE  4711

struct TestData {
  long test_value;
};

class RCUReaderThread : public JavaTestThread {
public:
  static volatile bool _exit;
  volatile TestData** _test;
  Semaphore* _wrt_start;
  RCUReaderThread(Semaphore* post, volatile TestData** test, Semaphore* wrt_start)
    : JavaTestThread(post), _test(test), _wrt_start(wrt_start) {};
  virtual ~RCUReaderThread(){}
  void main_run() {
    _wrt_start->signal();
    while (!_exit) {
      GlobalCounter::CSContext cs_context = GlobalCounter::critical_section_begin(this);
      volatile TestData* test = Atomic::load_acquire(_test);
      long value = Atomic::load_acquire(&test->test_value);
      ASSERT_EQ(value, GOOD_VALUE);
      GlobalCounter::critical_section_end(this, cs_context);
      {
        GlobalCounter::CriticalSection cs(this);
        volatile TestData* test = Atomic::load_acquire(_test);
        long value = Atomic::load_acquire(&test->test_value);
        ASSERT_EQ(value, GOOD_VALUE);
      }
    }
  }
};

volatile bool RCUReaderThread::_exit = false;

class RCUWriterThread : public JavaTestThread {
public:
  RCUWriterThread(Semaphore* post) : JavaTestThread(post) {
  };
  virtual ~RCUWriterThread(){}
  void main_run() {
    static const int NUMBER_OF_READERS = 4;
    Semaphore post;
    Semaphore wrt_start;
    volatile TestData* test = NULL;

    RCUReaderThread* reader1 = new RCUReaderThread(&post, &test, &wrt_start);
    RCUReaderThread* reader2 = new RCUReaderThread(&post, &test, &wrt_start);
    RCUReaderThread* reader3 = new RCUReaderThread(&post, &test, &wrt_start);
    RCUReaderThread* reader4 = new RCUReaderThread(&post, &test, &wrt_start);

    TestData* tmp = new TestData();
    tmp->test_value = GOOD_VALUE;
    Atomic::release_store_fence(&test, tmp);

    reader1->doit();
    reader2->doit();
    reader3->doit();
    reader4->doit();

    int nw = NUMBER_OF_READERS;
    while (nw > 0) {
      wrt_start.wait();
      --nw;
    }
    jlong stop_ms = os::javaTimeMillis() + 1000; // 1 seconds max test time
    for (int i = 0; i < 100000 && stop_ms > os::javaTimeMillis(); i++) {
      volatile TestData* free_tmp = test;
      tmp = new TestData();
      tmp->test_value = GOOD_VALUE;
      Atomic::release_store(&test, tmp);
      GlobalCounter::write_synchronize();
      free_tmp->test_value = BAD_VALUE;
      delete free_tmp;
    }
    RCUReaderThread::_exit = true;
    for (int i = 0; i < NUMBER_OF_READERS; i++) {
      post.wait();
    }
  }
};

TEST_VM(GlobalCounter, critical_section) {
  RCUReaderThread::_exit = false;
  mt_test_doer<RCUWriterThread>();
}
