/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_POSIX_SIGNALS_POSIX_HPP
#define OS_POSIX_SIGNALS_POSIX_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;
class Thread;
class OSThread;

class PosixSignals : public AllStatic {

public:

  // Signal number used to suspend/resume a thread
  static int SR_signum;

  static int init();
  // The platform dependent parts of the central hotspot signal handler.
  // Returns true if the signal had been recognized and handled, false if not. If true, caller should
  // return from signal handling.
  static bool pd_hotspot_signal_handler(int sig, siginfo_t* info, ucontext_t* uc, JavaThread* thread);

  static bool is_sig_ignored(int sig);

  static void hotspot_sigmask(Thread* thread);

  static void print_signal_handler(outputStream* st, int sig, char* buf, size_t buflen);

  // Suspend-resume
  static bool do_suspend(OSThread* osthread);
  static void do_resume(OSThread* osthread);

  // For signal-chaining
  static bool chained_handler(int sig, siginfo_t* siginfo, void* context);

  // Unblock all signals whose delivery cannot be deferred and which, if they happen
  //  while delivery is blocked, would cause crashes or hangs (see JDK-8252533).
  static void unblock_error_signals();
};

#endif // OS_POSIX_SIGNALS_POSIX_HPP
