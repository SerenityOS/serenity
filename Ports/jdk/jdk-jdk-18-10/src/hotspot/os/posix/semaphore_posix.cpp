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
#ifndef __APPLE__
#include "runtime/os.hpp"
// POSIX unnamed semaphores are not supported on OS X.
#include "semaphore_posix.hpp"
#include <semaphore.h>

#define check_with_errno(check_type, cond, msg)                             \
  do {                                                                      \
    int err = errno;                                                        \
    check_type(cond, "%s; error='%s' (errno=%s)", msg, os::strerror(err),   \
               os::errno_name(err));                                        \
} while (false)

#define assert_with_errno(cond, msg)    check_with_errno(assert, cond, msg)
#define guarantee_with_errno(cond, msg) check_with_errno(guarantee, cond, msg)

PosixSemaphore::PosixSemaphore(uint value) {
  int ret = sem_init(&_semaphore, 0, value);

  guarantee_with_errno(ret == 0, "Failed to initialize semaphore");
}

PosixSemaphore::~PosixSemaphore() {
  int ret = sem_destroy(&_semaphore);
  assert_with_errno(ret == 0, "sem_destroy failed");
}

void PosixSemaphore::signal(uint count) {
  for (uint i = 0; i < count; i++) {
    int ret = sem_post(&_semaphore);

    assert_with_errno(ret == 0, "sem_post failed");
  }
}

void PosixSemaphore::wait() {
  int ret;

  do {
    ret = sem_wait(&_semaphore);
  } while (ret != 0 && errno == EINTR);

  assert_with_errno(ret == 0, "sem_wait failed");
}

bool PosixSemaphore::trywait() {
  int ret;

  do {
    ret = sem_trywait(&_semaphore);
  } while (ret != 0 && errno == EINTR);

  assert_with_errno(ret == 0 || errno == EAGAIN, "trywait failed");

  return ret == 0;
}

bool PosixSemaphore::timedwait(int64_t millis) {
  struct timespec ts;
  os::Posix::to_RTC_abstime(&ts, millis);
  return timedwait(ts);
}

bool PosixSemaphore::timedwait(struct timespec ts) {
  while (true) {
    int result = sem_timedwait(&_semaphore, &ts);
    if (result == 0) {
      return true;
    } else if (errno == EINTR) {
      continue;
    } else if (errno == ETIMEDOUT) {
      return false;
    } else {
      assert_with_errno(false, "timedwait failed");
      return false;
    }
  }
}
#endif // __APPLE__

