/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/thread.inline.hpp"
#include "runtime/threadCritical.hpp"

// OS-includes here
# include <windows.h>
# include <winbase.h>

//
// See threadCritical.hpp for details of this class.
//

static INIT_ONCE initialized = INIT_ONCE_STATIC_INIT;
static int lock_count = 0;
static HANDLE lock_event;
static DWORD lock_owner = 0;

//
// Note that Microsoft's critical region code contains a race
// condition, and is not suitable for use. A thread holding the
// critical section cannot safely suspend a thread attempting
// to enter the critical region. The failure mode is that both
// threads are permanently suspended.
//
// I experiemented with the use of ordinary windows mutex objects
// and found them ~30 times slower than the critical region code.
//

static BOOL WINAPI initialize(PINIT_ONCE InitOnce, PVOID Parameter, PVOID *Context) {
  lock_event = CreateEvent(NULL, false, true, NULL);
  assert(lock_event != NULL, "unexpected return value from CreateEvent");
  return true;
}

ThreadCritical::ThreadCritical() {
  InitOnceExecuteOnce(&initialized, &initialize, NULL, NULL);

  DWORD current_thread = GetCurrentThreadId();
  if (lock_owner != current_thread) {
    // Grab the lock before doing anything.
    DWORD ret = WaitForSingleObject(lock_event,  INFINITE);
    assert(ret == WAIT_OBJECT_0, "unexpected return value from WaitForSingleObject");
    lock_owner = current_thread;
  }
  // Atomicity isn't required. Bump the recursion count.
  lock_count++;
}

ThreadCritical::~ThreadCritical() {
  assert(lock_owner == GetCurrentThreadId(), "unlock attempt by wrong thread");
  assert(lock_count >= 0, "Attempt to unlock when already unlocked");

  lock_count--;
  if (lock_count == 0) {
    // We're going to unlock
    lock_owner = 0;
    // No lost wakeups, lock_event stays signaled until reset.
    DWORD ret = SetEvent(lock_event);
    assert(ret != 0, "unexpected return value from SetEvent");
  }
}
