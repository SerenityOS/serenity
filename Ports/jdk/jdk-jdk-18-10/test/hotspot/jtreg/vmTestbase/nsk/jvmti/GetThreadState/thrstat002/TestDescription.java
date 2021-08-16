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
 * @summary converted from VM Testbase nsk/jvmti/GetThreadState/thrstat002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, quarantine]
 * VM Testbase comments: 6260469
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMDI function GetThreadState.  Java program launches
 *     a thread and for various thread states calls Thread.suspend()/resume()
 *     methods or JVMDI functions SuspendThread/ResumeThread. Then native method
 *     checkStatus is invoked. This method calls GetThreadState and checks if
 *     the returned values are correct and JVMTI_THREAD_STATE_SUSPENDED bit
 *     is set (or clear after resume).
 *     The thread statuses are:
 *       - JVMTI_THREAD_STATE_RUNNABLE
 *               if thread is runnable
 *       - JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER
 *               if thread is waiting to enter sync. block
 *       - JVMTI_THREAD_STATE_IN_OBJECT_WAIT
 *               if thread is waiting by object.wait()
 *     Failing criteria for the test are:
 *       - values returned by GetThreadState are not the same as expected;
 *       - failure of used JVMTI functions.
 * COMMENTS
 *     Converted the test to use GetThreadState instead of GetThreadStatus.
 *     Fixed according to 4387521 and 4427103 bugs.
 *     Fixed according to 4463667 bug.
 *     Fixed according to 4669812 bug.
 *     Ported from JVMDI.
 *     Fixed according to 4925857 bug:
 *       - rearranged synchronization of tested thread
 *       - enhanced descripton
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:thrstat002 nsk.jvmti.GetThreadState.thrstat002 5
 */

