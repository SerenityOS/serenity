/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
#include "cds/metaspaceShared.hpp"
#include "runtime/os.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "signals_posix.hpp"
#include "utilities/debug.hpp"
#include "utilities/vmError.hpp"

#include <sys/types.h>
#include <sys/wait.h>

#ifdef LINUX
#include <sys/syscall.h>
#include <unistd.h>
#endif
#ifdef AIX
#include <unistd.h>
#endif
#ifdef BSD
#include <sys/syscall.h>
#include <unistd.h>
#endif


// Needed for cancelable steps.
static volatile pthread_t reporter_thread_id;

void VMError::reporting_started() {
  // record pthread id of reporter thread.
  reporter_thread_id = ::pthread_self();
}

void VMError::interrupt_reporting_thread() {
  // We misuse SIGILL here, but it does not really matter. We need
  //  a signal which is handled by crash_handler and not likely to
  //  occurr during error reporting itself.
  ::pthread_kill(reporter_thread_id, SIGILL);
}

static void crash_handler(int sig, siginfo_t* info, void* ucVoid) {

  PosixSignals::unblock_error_signals();

  // support safefetch faults in error handling
  ucontext_t* const uc = (ucontext_t*) ucVoid;
  address pc = (uc != NULL) ? os::Posix::ucontext_get_pc(uc) : NULL;

  // Correct pc for SIGILL, SIGFPE (see JDK-8176872)
  if (sig == SIGILL || sig == SIGFPE) {
    pc = (address) info->si_addr;
  }

  // Needed to make it possible to call SafeFetch.. APIs in error handling.
  if (uc && pc && StubRoutines::is_safefetch_fault(pc)) {
    os::Posix::ucontext_set_pc(uc, StubRoutines::continuation_for_safefetch_fault(pc));
    return;
  }

  // Needed because asserts may happen in error handling too.
#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
  if ((sig == SIGSEGV || sig == SIGBUS) && info != NULL && info->si_addr == g_assert_poison) {
    if (handle_assert_poison_fault(ucVoid, info->si_addr)) {
      return;
    }
  }
#endif // CAN_SHOW_REGISTERS_ON_ASSERT

  VMError::report_and_die(NULL, sig, pc, info, ucVoid);
}

const void* VMError::crash_handler_address = CAST_FROM_FN_PTR(void *, crash_handler);

void VMError::install_secondary_signal_handler() {
  static const int signals_to_handle[] = {
    SIGSEGV, SIGBUS, SIGILL, SIGFPE, SIGTRAP,
    0 // end
  };
  for (int i = 0; signals_to_handle[i] != 0; i++) {
    os::signal(signals_to_handle[i], CAST_FROM_FN_PTR(void *, crash_handler));
  }
}

// Write a hint to the stream in case siginfo relates to a segv/bus error
// and the offending address points into CDS archive.
void VMError::check_failing_cds_access(outputStream* st, const void* siginfo) {
#if INCLUDE_CDS
  if (siginfo && UseSharedSpaces) {
    const siginfo_t* const si = (siginfo_t*)siginfo;
    if (si->si_signo == SIGBUS || si->si_signo == SIGSEGV) {
      const void* const fault_addr = si->si_addr;
      if (fault_addr != NULL) {
        if (MetaspaceShared::is_in_shared_metaspace(fault_addr)) {
          st->print("Error accessing class data sharing archive. "
            "Mapped file inaccessible during execution, possible disk/network problem.");
        }
      }
    }
  }
#endif
}
