/*
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
 */

#include "precompiled.hpp"
#include "runtime/mutex.hpp"
#include "utilities/filterQueue.inline.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"

// EXPECT_EQ(cht_get_copy(cht, thr, stl2), val2) << "Get did not find value.";

static bool match_all(uintptr_t val) {
  return true;
}

static bool match_1(uintptr_t val) {
  return 1 == val;
}

static bool match_2(uintptr_t val) {
  return 2 == val;
}

static bool match_3(uintptr_t val) {
  return 3 == val;
}

static bool match_4(uintptr_t val) {
  return 4 == val;
}

static bool match_even(uintptr_t val) {
  return (val & 0x1) == 0x0;
}

static void is_empty(FilterQueue<uintptr_t>& queue) {
  EXPECT_EQ(queue.is_empty(), true) << "Must be empty.";
  EXPECT_EQ(queue.contains(match_1), false) << "Must be empty.";
  EXPECT_EQ(queue.contains(match_all), false) << "Must be empty.";
  EXPECT_EQ(queue.peek(match_1), (uintptr_t)0) << "Must be empty.";
  EXPECT_EQ(queue.peek(match_all), (uintptr_t)0) << "Must be empty.";
  EXPECT_EQ(queue.pop(match_all), (uintptr_t)0) << "Must be empty.";
}

TEST_VM(FilterQueue, one) {
  FilterQueue<uintptr_t> queue;
  is_empty(queue);
  queue.push(1);
  EXPECT_EQ(queue.is_empty(), false) << "Must be not empty.";
  EXPECT_EQ(queue.contains(match_1), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_all), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_even), false) << "Must not contain a value.";
  EXPECT_EQ(queue.peek(match_1), (uintptr_t)1) << "Must match 1.";
  EXPECT_NE(queue.peek(match_all), (uintptr_t)0) << "Must contain a value.";
  EXPECT_EQ(queue.peek(match_even), (uintptr_t)0) << "Must not contain a value.";
  EXPECT_EQ(queue.pop(match_all), (uintptr_t)1) << "Must not be empty.";
  is_empty(queue);
}

TEST_VM(FilterQueue, two) {
  FilterQueue<uintptr_t> queue;

  queue.push(1);
  queue.push(2);

  EXPECT_EQ(queue.is_empty(), false) << "Must be not empty.";
  EXPECT_EQ(queue.contains(match_1), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_2), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_all), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_even), true) << "Must contain a value.";

  EXPECT_EQ(queue.peek(match_1), (uintptr_t)1) << "Must contain a value.";
  EXPECT_EQ(queue.peek(match_2), (uintptr_t)2) << "Must contain a value.";
  EXPECT_NE(queue.peek(match_all), (uintptr_t)0) << "Must contain a value.";
  EXPECT_NE(queue.peek(match_even), (uintptr_t)0) << "Must contain a value.";

  EXPECT_EQ(queue.pop(match_all), (uintptr_t)1) << "Must not be empty.";

  EXPECT_EQ(queue.is_empty(), false) << "Must be not empty.";
  EXPECT_EQ(queue.contains(match_1), false) << "Must not contain a value.";
  EXPECT_EQ(queue.contains(match_2), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_all), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_even), true) << "Must contain a value.";

  EXPECT_EQ(queue.peek(match_1), (uintptr_t)0) << "Must not contain a value.";
  EXPECT_EQ(queue.peek(match_2), (uintptr_t)2) << "Must contain a value.";
  EXPECT_NE(queue.peek(match_all), (uintptr_t)0) << "Must contain a value.";
  EXPECT_NE(queue.peek(match_even), (uintptr_t)0) << "Must contain a value.";

  queue.push(3);

  EXPECT_EQ(queue.peek(match_even), (uintptr_t)2) << "Must not be empty.";
  EXPECT_EQ(queue.pop(match_even), (uintptr_t)2) << "Must not be empty.";

  queue.push(2);

  EXPECT_EQ(queue.pop(match_even), (uintptr_t)2) << "Must not be empty.";

  EXPECT_EQ(queue.is_empty(), false) << "Must be not empty.";
  EXPECT_EQ(queue.contains(match_3), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_2), false) << "Must not contain a value.";
  EXPECT_EQ(queue.contains(match_all), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_even), false) << "Must not contain a value.";

  EXPECT_EQ(queue.peek(match_3), (uintptr_t)3) << "Must contain a value.";
  EXPECT_EQ(queue.peek(match_2), (uintptr_t)0) << "Must be empty.";
  EXPECT_EQ(queue.peek(match_all), (uintptr_t)3) << "Must contain a value.";
  EXPECT_EQ(queue.peek(match_even), (uintptr_t)0) << "Must be empty.";

  EXPECT_EQ(queue.pop(match_even), (uintptr_t)0) << "Must be empty.";
  EXPECT_EQ(queue.pop(match_all), (uintptr_t)3) << "Must not be empty.";

  is_empty(queue);
}

TEST_VM(FilterQueue, three) {
  FilterQueue<uintptr_t> queue;

  queue.push(1);
  queue.push(2);
  queue.push(3);

  EXPECT_EQ(queue.is_empty(), false) << "Must be not empty.";
  EXPECT_EQ(queue.contains(match_1), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_2), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_3), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_4), false) << "Must not contain a value.";

  EXPECT_EQ(queue.contains(match_all), true) << "Must contain a value.";
  EXPECT_EQ(queue.contains(match_even), true) << "Must contain a value.";

  EXPECT_EQ(queue.peek(match_even), (uintptr_t)2) << "Must not be empty.";
  EXPECT_EQ(queue.peek(match_all), (uintptr_t)1) << "Must not be empty.";

  EXPECT_EQ(queue.pop(match_even), (uintptr_t)2) << "Must not be empty.";
  EXPECT_EQ(queue.pop(match_even), (uintptr_t)0) << "Must be empty.";
  EXPECT_EQ(queue.pop(match_all), (uintptr_t)1) << "Must not be empty.";
  EXPECT_EQ(queue.pop(match_all), (uintptr_t)3) << "Must not be empty.";

  is_empty(queue);
}

class FilterQueueTestThread : public JavaTestThread {
  FilterQueue<uintptr_t>* _fq;
  Mutex* _lock;
  uintptr_t _val;
  uintptr_t _pop;
public:
  FilterQueueTestThread(Semaphore* post, FilterQueue<uintptr_t>* fq, Mutex* lock, uintptr_t val, uintptr_t pop)
    : JavaTestThread(post), _fq(fq), _lock(lock), _val(val), _pop(pop) {
  }
  virtual void main_run() {
    for (int i = 0; i < 1000; i++) {
      for (int j = 0; j < 10; j++) {
        _fq->push(_val);
      }
      {
        do {
          MutexLocker ml(_lock, Mutex::_no_safepoint_check_flag);
          if (_fq->contains(*this) != 0) {
            break;
          }
        } while (true);
      }
      for (int j = 0; j < 10; j++) {
        MutexLocker ml(_lock, Mutex::_no_safepoint_check_flag);
        while (_fq->peek(*this) == 0) {}
        while (_fq->pop(*this) == 0) {}
      }
    }
  }
  bool operator()(uintptr_t val) {
    return val == _pop;
  }
};

TEST_VM(FilterQueue, stress) {
  FilterQueue<uintptr_t> queue;
  Mutex lock(Mutex::leaf, "Test Lock", true, Mutex::_safepoint_check_never);
  static const int nthreads = 4;
  Semaphore post;
  FilterQueueTestThread* threads[nthreads] = {};
  for (int i = 0; i < nthreads; ++i) {
    threads[i] = new FilterQueueTestThread(&post, &queue, &lock, i + 1, i + 2 > nthreads ? 1 : i + 2);
    threads[i]->doit();
  }
  for (uint i = 0; i < nthreads; ++i) {
    post.wait();
  }
  EXPECT_EQ(queue.is_empty(), true) << "Must be empty.";
}
