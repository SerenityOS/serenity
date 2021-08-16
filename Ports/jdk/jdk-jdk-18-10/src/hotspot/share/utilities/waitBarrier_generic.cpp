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
 *
 */

#include "precompiled.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "utilities/waitBarrier_generic.hpp"
#include "utilities/spinYield.hpp"

void GenericWaitBarrier::arm(int barrier_tag) {
  assert(_barrier_tag == 0, "Already armed");
  assert(_waiters == 0, "We left a thread hanging");
  _barrier_tag = barrier_tag;
  _waiters = 0;
  OrderAccess::fence();
}

int GenericWaitBarrier::wake_if_needed() {
  assert(_barrier_tag == 0, "Not disarmed");
  int w = _waiters;
  if (w == 0) {
    // Load of _barrier_threads in caller must not pass the load of _waiters.
    OrderAccess::loadload();
    return 0;
  }
  assert(w > 0, "Bad counting");
  // We need an exact count which never goes below zero,
  // otherwise the semaphore may be signalled too many times.
  if (Atomic::cmpxchg(&_waiters, w, w - 1) == w) {
    _sem_barrier.signal();
    return w - 1;
  }
  return w;
}

void GenericWaitBarrier::disarm() {
  assert(_barrier_tag != 0, "Not armed");
  _barrier_tag = 0;
  // Loads of _barrier_threads/_waiters must not float above disarm store and
  // disarm store must not sink below.
  OrderAccess::fence();
  int left;
  SpinYield sp;
  do {
    left = GenericWaitBarrier::wake_if_needed();
    if (left == 0 && _barrier_threads > 0) {
      // There is no thread to wake but we still have barrier threads.
      sp.wait();
    }
    // We must loop here until there are no waiters or potential waiters.
  } while (left > 0 || _barrier_threads > 0);
  // API specifies disarm() must provide a trailing fence.
  OrderAccess::fence();
}

void GenericWaitBarrier::wait(int barrier_tag) {
  assert(barrier_tag != 0, "Trying to wait on disarmed value");
  if (barrier_tag != _barrier_tag) {
    // API specifies wait() must provide a trailing fence.
    OrderAccess::fence();
    return;
  }
  Atomic::add(&_barrier_threads, 1);
  if (barrier_tag != 0 && barrier_tag == _barrier_tag) {
    Atomic::add(&_waiters, 1);
    _sem_barrier.wait();
    // We help out with posting, but we need to do so before we decrement the
    // _barrier_threads otherwise we might wake threads up in next wait.
    GenericWaitBarrier::wake_if_needed();
  }
  Atomic::add(&_barrier_threads, -1);
}
