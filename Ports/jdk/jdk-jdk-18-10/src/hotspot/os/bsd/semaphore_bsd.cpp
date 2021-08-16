/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "semaphore_bsd.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"

#include <semaphore.h>

#ifdef __APPLE__
// OS X doesn't support unnamed POSIX semaphores, so the implementation in os_posix.cpp can't be used.

static const char* sem_init_strerror(kern_return_t value) {
  switch (value) {
    case KERN_INVALID_ARGUMENT:  return "Invalid argument";
    case KERN_RESOURCE_SHORTAGE: return "Resource shortage";
    default:                     return "Unknown";
  }
}

OSXSemaphore::OSXSemaphore(uint value) {
  kern_return_t ret = semaphore_create(mach_task_self(), &_semaphore, SYNC_POLICY_FIFO, value);

  guarantee(ret == KERN_SUCCESS, "Failed to create semaphore: %s", sem_init_strerror(ret));
}

OSXSemaphore::~OSXSemaphore() {
  semaphore_destroy(mach_task_self(), _semaphore);
}

void OSXSemaphore::signal(uint count) {
  for (uint i = 0; i < count; i++) {
    kern_return_t ret = semaphore_signal(_semaphore);

    assert(ret == KERN_SUCCESS, "Failed to signal semaphore");
  }
}

void OSXSemaphore::wait() {
  kern_return_t ret;
  while ((ret = semaphore_wait(_semaphore)) == KERN_ABORTED) {
    // Semaphore was interrupted. Retry.
  }
  assert(ret == KERN_SUCCESS, "Failed to wait on semaphore");
}

bool OSXSemaphore::trywait() {
  return timedwait(0);
}

bool OSXSemaphore::timedwait(int64_t millis) {
  kern_return_t kr = KERN_ABORTED;

  // kernel semaphores take a relative timeout
  mach_timespec_t waitspec;
  int secs = millis / MILLIUNITS;
  int nsecs = millis_to_nanos(millis % MILLIUNITS);
  waitspec.tv_sec = secs;
  waitspec.tv_nsec = nsecs;

  int64_t starttime = os::javaTimeNanos();

  kr = semaphore_timedwait(_semaphore, waitspec);
  while (kr == KERN_ABORTED) {
    // reduce the timout and try again
    int64_t totalwait = millis_to_nanos(millis);
    int64_t current = os::javaTimeNanos();
    int64_t passedtime = current - starttime;

    if (passedtime >= totalwait) {
      waitspec.tv_sec = 0;
      waitspec.tv_nsec = 0;
    } else {
      int64_t waittime = totalwait - (current - starttime);
      waitspec.tv_sec = waittime / NANOSECS_PER_SEC;
      waitspec.tv_nsec = waittime % NANOSECS_PER_SEC;
    }

    kr = semaphore_timedwait(_semaphore, waitspec);
  }

  return kr == KERN_SUCCESS;
}
#endif // __APPLE__
