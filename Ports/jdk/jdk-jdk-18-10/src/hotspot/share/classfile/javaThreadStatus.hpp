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

#ifndef SHARE_CLASSFILE_JAVATHREADSTATUS_HPP
#define SHARE_CLASSFILE_JAVATHREADSTATUS_HPP

#include "jvmtifiles/jvmti.h"

// Java Thread Status for JVMTI and M&M use.
// This thread status info is saved in threadStatus field of
// java.lang.Thread java class.
enum class JavaThreadStatus : int {
  NEW                      = 0,
  RUNNABLE                 = JVMTI_THREAD_STATE_ALIVE +          // runnable / running
                             JVMTI_THREAD_STATE_RUNNABLE,
  SLEEPING                 = JVMTI_THREAD_STATE_ALIVE +          // Thread.sleep()
                             JVMTI_THREAD_STATE_WAITING +
                             JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT +
                             JVMTI_THREAD_STATE_SLEEPING,
  IN_OBJECT_WAIT           = JVMTI_THREAD_STATE_ALIVE +          // Object.wait()
                             JVMTI_THREAD_STATE_WAITING +
                             JVMTI_THREAD_STATE_WAITING_INDEFINITELY +
                             JVMTI_THREAD_STATE_IN_OBJECT_WAIT,
  IN_OBJECT_WAIT_TIMED     = JVMTI_THREAD_STATE_ALIVE +          // Object.wait(long)
                             JVMTI_THREAD_STATE_WAITING +
                             JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT +
                             JVMTI_THREAD_STATE_IN_OBJECT_WAIT,
  PARKED                   = JVMTI_THREAD_STATE_ALIVE +          // LockSupport.park()
                             JVMTI_THREAD_STATE_WAITING +
                             JVMTI_THREAD_STATE_WAITING_INDEFINITELY +
                             JVMTI_THREAD_STATE_PARKED,
  PARKED_TIMED             = JVMTI_THREAD_STATE_ALIVE +          // LockSupport.park(long)
                             JVMTI_THREAD_STATE_WAITING +
                             JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT +
                             JVMTI_THREAD_STATE_PARKED,
  BLOCKED_ON_MONITOR_ENTER = JVMTI_THREAD_STATE_ALIVE +          // (re-)entering a synchronization block
                             JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
  TERMINATED               = JVMTI_THREAD_STATE_TERMINATED
};

#endif // SHARE_CLASSFILE_JAVATHREADSTATUS_HPP
