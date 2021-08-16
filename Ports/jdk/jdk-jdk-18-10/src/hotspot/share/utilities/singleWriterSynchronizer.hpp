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

#ifndef SHARE_UTILITIES_SINGLEWRITERSYNCHRONIZER_HPP
#define SHARE_UTILITIES_SINGLEWRITERSYNCHRONIZER_HPP

#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Synchronization primitive inspired by RCU.
//
// Any number of threads may enter critical sections associated with a
// synchronizer object.  One (at a time) other thread may wait for the
// completion of all critical sections for the synchronizer object
// that were extant when the wait was initiated.  Usage is that there
// is some state that can be accessed either before or after some
// change.  An accessing thread performs the access within a critical
// section.  A writer thread performs the state change, and then waits
// for critical sections to complete, thereby ensuring there are no
// threads in a critical section that might have seen the old state.
//
// Generally, GlobalCounter should be used instead of this class, as
// GlobalCounter has measurably better performance and doesn't have
// the single writer at a time restriction.  Use this only in
// situations where GlobalCounter won't work for some reason.
class SingleWriterSynchronizer {
  volatile uint _enter;
  volatile uint _exit[2];
  volatile uint _waiting_for;
  Semaphore _wakeup;

  DEBUG_ONLY(volatile uint _writers;)

  NONCOPYABLE(SingleWriterSynchronizer);

public:
  SingleWriterSynchronizer();

  // Enter a critical section for this synchronizer.  Entering a
  // critical section never blocks.  While in a critical section, a
  // thread should avoid blocking, or even take a long time.  In
  // particular, a thread must never safepoint while in a critical
  // section.
  // Precondition: The current thread must not already be in a
  // critical section for this synchronizer.
  inline uint enter();

  // Exit a critical section for this synchronizer.
  // Precondition: enter_value must be the result of the corresponding
  // enter() for the critical section.
  inline void exit(uint enter_value);

  // Wait until all threads currently in a critical section for this
  // synchronizer have exited their critical section.  Threads that
  // enter a critical section after the synchronization has started
  // are not considered in the wait.
  // Precondition: No other thread may be synchronizing on this
  // synchronizer.
  void synchronize();

  // RAII class for managing enter/exit pairs.
  class CriticalSection;
};

inline uint SingleWriterSynchronizer::enter() {
  return Atomic::add(&_enter, 2u);
}

inline void SingleWriterSynchronizer::exit(uint enter_value) {
  uint exit_value = Atomic::add(&_exit[enter_value & 1], 2u);
  // If this exit completes a synchronize request, wakeup possibly
  // waiting synchronizer.  Read of _waiting_for must follow the _exit
  // update.
  if (exit_value == _waiting_for) {
    _wakeup.signal();
  }
}

class SingleWriterSynchronizer::CriticalSection : public StackObj {
  SingleWriterSynchronizer* _synchronizer;
  uint _enter_value;

public:
  // Enter synchronizer's critical section.
  explicit CriticalSection(SingleWriterSynchronizer* synchronizer) :
    _synchronizer(synchronizer),
    _enter_value(synchronizer->enter())
  {}

  // Exit synchronizer's critical section.
  ~CriticalSection() {
    _synchronizer->exit(_enter_value);
  }
};

#endif // SHARE_UTILITIES_SINGLEWRITERSYNCHRONIZER_HPP
