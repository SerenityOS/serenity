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
 *
 */

#include "precompiled.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"
#include "utilities/singleWriterSynchronizer.hpp"
#include "utilities/macros.hpp"

SingleWriterSynchronizer::SingleWriterSynchronizer() :
  _enter(0),
  _exit(),
  // The initial value of 1 for _waiting_for puts it on the inactive
  // track, so no thread exiting a critical section will match it.
  _waiting_for(1),
  _wakeup()
  DEBUG_ONLY(COMMA _writers(0))
{}

// Wait until all threads that entered a critical section before
// synchronization have exited that critical section.
void SingleWriterSynchronizer::synchronize() {
  // Side-effect in assert balanced by debug-only dec at end.
  assert(Atomic::add(&_writers, 1u) == 1u, "multiple writers");
  // We don't know anything about the muxing between this invocation
  // and invocations in other threads.  We must start with the latest
  // _enter polarity, else we could clobber the wrong _exit value on
  // the first iteration.  So fence to ensure everything here follows
  // whatever muxing was used.
  OrderAccess::fence();
  uint value = _enter;
  // (1) Determine the old and new exit counters, based on the
  // polarity (bit0 value) of the on-entry enter counter.
  volatile uint* new_ptr = &_exit[(value + 1) & 1];
  // (2) Change the in-use exit counter to the new counter, by adding
  // 1 to the enter counter (flipping the polarity), meanwhile
  // "simultaneously" initializing the new exit counter to that enter
  // value.  Note: The new exit counter is not being used by read
  // operations until this change of _enter succeeds.
  uint old;
  do {
    old = value;
    *new_ptr = ++value;
    value = Atomic::cmpxchg(&_enter, old, value);
  } while (old != value);
  // Critical sections entered before we changed the polarity will use
  // the old exit counter.  Critical sections entered after the change
  // will use the new exit counter.
  volatile uint* old_ptr = &_exit[old & 1];
  assert(old_ptr != new_ptr, "invariant");
  // (3) Inform threads in in-progress critical sections that there is
  // a pending synchronize waiting.  The thread that completes the
  // request (_exit value == old) will signal the _wakeup semaphore to
  // allow us to proceed.
  _waiting_for = old;
  // Write of _waiting_for must precede read of _exit and associated
  // conditional semaphore wait.  If they were re-ordered then a
  // critical section exit could miss the wakeup request, failing to
  // signal us while we're waiting.
  OrderAccess::fence();
  // (4) Wait for all the critical sections started before the change
  // to complete, e.g. for the value of old_ptr to catch up with old.
  // Loop because there could be pending wakeups unrelated to this
  // synchronize request.
  while (old != Atomic::load_acquire(old_ptr)) {
    _wakeup.wait();
  }
  // (5) Drain any pending wakeups. A critical section exit may have
  // completed our request and seen our _waiting_for before we checked
  // for completion.  There are also possible (though rare) spurious
  // wakeup signals in the timing gap between changing the _enter
  // polarity and setting _waiting_for.  Enough of any of those could
  // lead to semaphore overflow.  This doesn't guarantee no unrelated
  // wakeups for the next wait, but prevents unbounded accumulation.
  while (_wakeup.trywait()) {}
  DEBUG_ONLY(Atomic::dec(&_writers);)
}
