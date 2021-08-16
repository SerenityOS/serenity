/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "utilities/spinYield.hpp"
#include "utilities/waitBarrier.hpp"
#include "threadHelper.inline.hpp"

static volatile int wait_tag = 0;
static volatile int valid_value = 0;

template <typename WaitBarrierImpl>
class WBThread : public JavaTestThread {
public:
  static volatile bool _exit;
  WaitBarrierType<WaitBarrierImpl>* _wait_barrier;
  Semaphore* _wrt_start;
  volatile int _on_barrier;

  WBThread(Semaphore* post, WaitBarrierType<WaitBarrierImpl>* wb, Semaphore* wrt_start)
    : JavaTestThread(post), _wait_barrier(wb), _wrt_start(wrt_start) {};
  virtual ~WBThread(){}
  void main_run() {
    _wrt_start->signal();
    int vv, tag;
    // Similar to how a JavaThread would stop in a safepoint.
    while (!_exit) {
      // Load the published tag.
      tag = Atomic::load_acquire(&wait_tag);
      // Publish the tag this thread is going to wait for.
      Atomic::release_store(&_on_barrier, tag);
      if (_on_barrier == 0) {
        SpinPause();
        continue;
      }
      OrderAccess::storeload(); // Loads in WB must not float up.
      // Wait until we are woken.
      _wait_barrier->wait(tag);
      // Verify that we do not see an invalid value.
      vv = Atomic::load_acquire(&valid_value);
      ASSERT_EQ((vv & 0x1), 0);
      Atomic::release_store(&_on_barrier, 0);
    }
  }
};

template <typename WaitBarrierImpl>
volatile bool WBThread<WaitBarrierImpl>::_exit = false;

template <typename WaitBarrierImpl>
class WBArmerThread : public JavaTestThread {
public:
  WBArmerThread(Semaphore* post) : JavaTestThread(post) {
  };
  virtual ~WBArmerThread(){}
  void main_run() {
    static const int NUMBER_OF_READERS = 4;
    Semaphore post;
    Semaphore wrt_start;
    WaitBarrierType<WaitBarrierImpl> wb(this);

    WBThread<WaitBarrierImpl>* reader1 = new WBThread<WaitBarrierImpl>(&post, &wb, &wrt_start);
    WBThread<WaitBarrierImpl>* reader2 = new WBThread<WaitBarrierImpl>(&post, &wb, &wrt_start);
    WBThread<WaitBarrierImpl>* reader3 = new WBThread<WaitBarrierImpl>(&post, &wb, &wrt_start);
    WBThread<WaitBarrierImpl>* reader4 = new WBThread<WaitBarrierImpl>(&post, &wb, &wrt_start);

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
    int next_tag = 1;
    // Similar to how the VM thread would use a WaitBarrier in a safepoint.
    while (stop_ms > os::javaTimeMillis()) {
      // Arm next tag.
      wb.arm(next_tag);
      // Publish tag.
      Atomic::release_store_fence(&wait_tag, next_tag);

      // Wait until threads picked up new tag.
      while (reader1->_on_barrier != wait_tag ||
             reader2->_on_barrier != wait_tag ||
             reader3->_on_barrier != wait_tag ||
             reader4->_on_barrier != wait_tag) {
        SpinPause();
      }

      // Set an invalid value.
      Atomic::release_store(&valid_value, valid_value + 1); // odd
      os::naked_yield();
      // Set a valid value.
      Atomic::release_store(&valid_value, valid_value + 1); // even
      // Publish inactive tag.
      Atomic::release_store_fence(&wait_tag, 0); // Stores in WB must not float up.
      wb.disarm();

      // Wait until threads done valid_value verification.
      while (reader1->_on_barrier != 0 ||
             reader2->_on_barrier != 0 ||
             reader3->_on_barrier != 0 ||
             reader4->_on_barrier != 0) {
        SpinPause();
      }
      ++next_tag;
    }
    WBThread<WaitBarrierImpl>::_exit = true;
    for (int i = 0; i < NUMBER_OF_READERS; i++) {
      post.wait();
    }
  }
};

TEST_VM(WaitBarrier, default_wb) {
  WBThread<WaitBarrierDefault>::_exit = false;
  mt_test_doer<WBArmerThread<WaitBarrierDefault> >();
}

#if defined(LINUX)
TEST_VM(WaitBarrier, generic_wb) {
  WBThread<GenericWaitBarrier>::_exit = false;
  mt_test_doer<WBArmerThread<GenericWaitBarrier> >();
}
#endif
