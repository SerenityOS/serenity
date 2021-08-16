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
 * @summary converted from VM Testbase nsk/jvmti/GetThreadState/thrstat001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function GetThreadState.  Java program
 *     launchs thread and 3 times calls a native method checkStatus. This
 *     method calls GetThreadState and checks if the returned value is
 *     correct.
 *     The test exercises JVMTI function GetThreadState.  Java program
 *     launches a thread and 3 times calls a native method checkStatus.
 *     This method calls GetThreadState and checks if the returned value is:
 *       - JVMTI_THREAD_STATE_RUNNABLE
 *               if thread is runnable
 *       - JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER
 *               if thread is waiting to enter sync. block
 *       - JVMTI_THREAD_STATE_IN_OBJECT_WAIT
 *               if thread is waiting by object.wait()
 *     The test also enables JVMPI_EVENT_METHOD_ENTRY and JVMPI_EVENT_METHOD_EXIT
 *     events and checks if GetThreadState returns JVMPI_THREAD_RUNNABLE for
 *     their threads.
 *     Failing criteria for the test are:
 *       - value returned by GetThreadState does not match expected value;
 *       - failures of used JVMTI functions.
 * COMMENTS
 *     Converted the test to use GetThreadState instead of GetThreadStatus.
 *     The test was updated to be more precise in its thread state transitions.
 *     Fixed according to 4387521 bug.
 *     To fix bug 4463667,
 *         1) two code fragments with "Thread.sleep(500);" are replaced
 *            with following ones:
 *                 Object obj = new Object();
 *                 *
 *                 *
 *                 synchronized (obj) {
 *                     obj.wait(500);
 *                 }
 *         2) extra waiting time
 *                 synchronized (obj) {
 *                     obj.wait(500);
 *                 }
 *            is added to get waiting time certainly after "contendCount"
 *            is set to 1.
 *     Fixed according to 4669812 bug.
 *     Ported from JVMDI.
 *     Fixed according to 4925857 bug:
 *       - rearranged synchronization of tested thread
 *       - enhanced descripton
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:thrstat001 nsk.jvmti.GetThreadState.thrstat001
 */

