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

#ifndef SHARE_UTILITIES_WAITBARRIER_HPP
#define SHARE_UTILITIES_WAITBARRIER_HPP

#include "memory/allocation.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/waitBarrier_generic.hpp"

#if defined(LINUX)
#include "waitBarrier_linux.hpp"
typedef LinuxWaitBarrier WaitBarrierDefault;
#else
typedef GenericWaitBarrier WaitBarrierDefault;
#endif

// Platform independent WaitBarrier API.
// An armed WaitBarrier prevents threads from advancing until the threads are
// woken by calling disarm(). The barrier is armed by setting a non-zero value
// - the tag. When the WaitBarrier is created, a thread is designated the owner
// and is the thread that should arm and disarm the WaitBarrier. In debug builds
// this is enforced.
//
// Expected Usage:
//  - Arming thread:
//     tag = ...;  // non-zero value
//     barrier.arm(tag);
//     <publish tag>
//     <work>
//     barrier.disarm();
//
//    - After arm(tag) returns any thread calling wait(tag) will block.
//    - Calling disarm() guarantees any thread calling or that has wait(tag) will
//      return. Either they will see the WaitBarrier as disarmed or they will be
//      unblocked and eligible to execute again when disarm() returns.
//    - After calling disarm() the barrier is ready to be re-armed with a new tag.
//      (may not be re-armed with last used tag)
//
//  - Waiting threads
//     wait(tag); // don't execute following code unless 'safe'
//     <work>
//
//    - A call to wait(tag) will block if the barrier is armed with the value
//      'tag'; else it will return immediately.
//    - A blocked thread is eligible to execute again once the barrier is
//      disarmed when disarm() has been called.
//
// It is a usage error to:
//  - call arm on a barrier that is already armed
//  - call disarm on a barrier that is not armed
//  - arm with the same tag as last used
// Usage errors are checked in debug builds but may be ignored otherwise.
//
// A primary goal of the WaitBarrier implementation is to wake all waiting
// threads as fast, and as concurrently, as possible.
//
template <typename WaitBarrierImpl>
class WaitBarrierType : public CHeapObj<mtInternal> {
  WaitBarrierImpl _impl;

  NONCOPYABLE(WaitBarrierType);

#ifdef ASSERT
  int _last_arm_tag;
  Thread* _owner;
#endif

 public:
  WaitBarrierType(Thread* owner) : _impl() {
#ifdef ASSERT
    _last_arm_tag = 0;
    _owner = owner;
#endif
  }
  ~WaitBarrierType() {}

  // Returns implementation description.
  const char* description()    { return _impl.description(); }

  // Guarantees any thread calling wait() with same tag will be blocked.
  // Provides a trailing fence.
  void arm(int barrier_tag) {
#ifdef ASSERT
    assert(_last_arm_tag != barrier_tag, "Re-arming with same tag");
    _last_arm_tag = barrier_tag;
    assert(_owner == Thread::current(), "Not owner thread");
#endif
    _impl.arm(barrier_tag);
  }

  // Guarantees any thread that called wait() will be awake when it returns.
  // Provides a trailing fence.
  void disarm() {
    assert(_owner == Thread::current(), "Not owner thread");
    _impl.disarm();
  }

  // Guarantees not to return until disarm() is called,
  // if called with currently armed tag (otherwise returns immediately).
  // Implementations must guarantee no spurious wakeups.
  // Provides a trailing fence.
  void wait(int barrier_tag) {
    assert(_owner != Thread::current(), "Trying to wait with owner thread");
    _impl.wait(barrier_tag);
  }
};

typedef WaitBarrierType<WaitBarrierDefault> WaitBarrier;

#endif // SHARE_UTILITIES_WAITBARRIER_HPP
