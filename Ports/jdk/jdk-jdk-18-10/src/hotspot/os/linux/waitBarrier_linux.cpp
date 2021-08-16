/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "waitBarrier_linux.hpp"
#include <sys/syscall.h>
#include <linux/futex.h>

#define check_with_errno(check_type, cond, msg)                             \
  do {                                                                      \
    int err = errno;                                                        \
    check_type(cond, "%s: error='%s' (errno=%s)", msg, os::strerror(err),   \
               os::errno_name(err));                                        \
} while (false)

#define guarantee_with_errno(cond, msg) check_with_errno(guarantee, cond, msg)

static int futex(volatile int *addr, int futex_op, int op_arg) {
  return syscall(SYS_futex, addr, futex_op, op_arg, NULL, NULL, 0);
}

void LinuxWaitBarrier::arm(int barrier_tag) {
  assert(_futex_barrier == 0, "Should not be already armed: "
         "_futex_barrier=%d", _futex_barrier);
  _futex_barrier = barrier_tag;
  OrderAccess::fence();
}

void LinuxWaitBarrier::disarm() {
  assert(_futex_barrier != 0, "Should be armed/non-zero.");
  _futex_barrier = 0;
  int s = futex(&_futex_barrier,
                FUTEX_WAKE_PRIVATE,
                INT_MAX /* wake a max of this many threads */);
  guarantee_with_errno(s > -1, "futex FUTEX_WAKE failed");
}

void LinuxWaitBarrier::wait(int barrier_tag) {
  assert(barrier_tag != 0, "Trying to wait on disarmed value");
  if (barrier_tag == 0 ||
      barrier_tag != _futex_barrier) {
    OrderAccess::fence();
    return;
  }
  do {
    int s = futex(&_futex_barrier,
                  FUTEX_WAIT_PRIVATE,
                  barrier_tag /* should be this tag */);
    guarantee_with_errno((s == 0) ||
                         (s == -1 && errno == EAGAIN) ||
                         (s == -1 && errno == EINTR),
                         "futex FUTEX_WAIT failed");
    // Return value 0: woken up, but re-check in case of spurious wakeup.
    // Error EINTR: woken by signal, so re-check and re-wait if necessary.
    // Error EAGAIN: we are already disarmed and so will pass the check.
  } while (barrier_tag == _futex_barrier);
}
