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
#include "memory/iterator.hpp"
#include "runtime/atomic.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.inline.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/globalCounter.hpp"
#include "utilities/spinYield.hpp"

GlobalCounter::PaddedCounter GlobalCounter::_global_counter;

class GlobalCounter::CounterThreadCheck : public ThreadClosure {
 private:
  uintx _gbl_cnt;
 public:
  CounterThreadCheck(uintx gbl_cnt) : _gbl_cnt(gbl_cnt) {}
  void do_thread(Thread* thread) {
    SpinYield yield;
    // Loops on this thread until it has exited the critical read section.
    while(true) {
      uintx cnt = Atomic::load_acquire(thread->get_rcu_counter());
      // This checks if the thread's counter is active. And if so is the counter
      // for a pre-existing reader (belongs to this grace period). A pre-existing
      // reader will have a lower counter than the global counter version for this
      // generation. If the counter is larger than the global counter version this
      //  is a new reader and we can continue.
      if (((cnt & COUNTER_ACTIVE) != 0) && (cnt - _gbl_cnt) > (max_uintx / 2)) {
        yield.wait();
      } else {
        break;
      }
    }
  }
};

void GlobalCounter::write_synchronize() {
  assert((*Thread::current()->get_rcu_counter() & COUNTER_ACTIVE) == 0x0, "must be outside a critcal section");
  // Atomic::add must provide fence since we have storeload dependency.
  uintx gbl_cnt = Atomic::add(&_global_counter._counter, COUNTER_INCREMENT);

  // Do all RCU threads.
  CounterThreadCheck ctc(gbl_cnt);
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *thread = jtiwh.next(); ) {
    ctc.do_thread(thread);
  }
  for (NonJavaThread::Iterator njti; !njti.end(); njti.step()) {
    ctc.do_thread(njti.current());
  }
}
