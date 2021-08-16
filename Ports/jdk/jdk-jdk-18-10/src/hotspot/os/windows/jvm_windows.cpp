/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/osThread.hpp"

#include <signal.h>

JVM_LEAF(void*, JVM_GetThreadInterruptEvent())
  return Thread::current()->osthread()->interrupt_event();
JVM_END

// sun.misc.Signal ///////////////////////////////////////////////////////////
// Signal code is mostly copied from classic vm, signals_md.c   1.4 98/08/23
/*
 * This function is included primarily as a debugging aid. If Java is
 * running in a console window, then pressing <CTRL-BREAK> will cause
 * the current state of all active threads and monitors to be written
 * to the console window.
 */

JVM_ENTRY_NO_ENV(void*, JVM_RegisterSignal(jint sig, void* handler))
  // Copied from classic vm
  // signals_md.c       1.4 98/08/23
  void* newHandler = handler == (void *)2
                   ? os::user_handler()
                   : handler;
  switch (sig) {
   case SIGFPE:
     return (void *)-1; /* already used by VM */
   case SIGBREAK:
     if (!ReduceSignalUsage) return (void *)-1;

    /* The following signals are used for Shutdown Hooks support. However, if
       ReduceSignalUsage (-Xrs) is set, Shutdown Hooks must be invoked via
       System.exit(), Java is not allowed to use these signals, and the the
       user is allowed to set his own _native_ handler for these signals and
       invoke System.exit() as needed. Terminator.setup() is avoiding
       registration of these signals when -Xrs is present. */
   case SHUTDOWN1_SIGNAL:
   case SHUTDOWN2_SIGNAL:
     if (ReduceSignalUsage) return (void*)-1;
  }

  void* oldHandler = os::signal(sig, newHandler);
  if (oldHandler == os::user_handler()) {
      return (void *)2;
  } else {
      return oldHandler;
  }
JVM_END


JVM_ENTRY_NO_ENV(jboolean, JVM_RaiseSignal(jint sig))
  if (ReduceSignalUsage) {
    // do not allow SHUTDOWN1_SIGNAL,SHUTDOWN2_SIGNAL,BREAK_SIGNAL
    // to be raised when ReduceSignalUsage is set, since no handler
    // for them is actually registered in JVM or via JVM_RegisterSignal.
    if (sig == SHUTDOWN1_SIGNAL || sig == SHUTDOWN2_SIGNAL ||
        sig == SIGBREAK) {
      return JNI_FALSE;
    }
  }
  os::signal_raise(sig);
  return JNI_TRUE;
JVM_END


