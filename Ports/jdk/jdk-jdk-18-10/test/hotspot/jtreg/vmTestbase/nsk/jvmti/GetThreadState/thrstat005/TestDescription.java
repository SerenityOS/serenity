/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/GetThreadState/thrstat005.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test verifies that the new hierarchical flags returned by GetThreadState()
 *     are properly set in various thread states as requested in bug #5041847.
 *     Flags being tested are:
 *     JVMTI_THREAD_STATE_ALIVE
 *     JVMTI_THREAD_STATE_TERMINATED
 *     JVMTI_THREAD_STATE_RUNNABLE
 *     JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER
 *     JVMTI_THREAD_STATE_WAITING
 *     JVMTI_THREAD_STATE_WAITING_INDEFINITELY
 *     JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT
 *     JVMTI_THREAD_STATE_SLEEPING
 *     JVMTI_THREAD_STATE_IN_OBJECT_WAIT
 *     JVMTI_THREAD_STATE_PARKED
 *     The state is checked in the following test cases:
 *     - A new thread is created
 *     - Thread is running (doing some computations)
 *     - Thread is blocked on a monitor (synchronized (...) { ... })
 *     - Thread is waiting in wait(timeout)
 *     - Thread is waiting in wait() w/o a timeout
 *     - Thread is parked using LockSupport.park()
 *     - Thread is parked using LockSupport.parkUntil()
 *     - Thread is in Thread.sleep()
 *     - Thread has terminated
 *     For more information see bugs #5041847, #4980307 and J2SE 5.0+ JVMTI spec.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:thrstat005 nsk.jvmti.GetThreadState.thrstat005
 */

