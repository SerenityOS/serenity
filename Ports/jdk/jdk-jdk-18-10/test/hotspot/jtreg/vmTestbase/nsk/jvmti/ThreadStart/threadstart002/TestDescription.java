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
 * @summary converted from VM Testbase nsk/jvmti/ThreadStart/threadstart002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This is a regression test for the following bug:
 *         4432884 jdbx does not work with jdk 1.3.1 starting with
 *                 rc1 build 19 onwards
 *     The test runs a debugger agent in a separate thread that operates
 *     on behalf of other threads, so when it gets a ThreadStart event,
 *     the debugger agent suspends the new thread and then
 *     calls jni_DeleteGlobalRef with a jnienv * for that new thread.
 *     Then the test resumes the new thread and checks the thread
 *     suspend status.
 * COMMENTS
 *     The test reproduces the bug on Solsparc with JDK 1.3.1-b19
 *     with the following output:
 *     java version "1.3.1-rc1"
 *     Java(TM) 2 Runtime Environment, Standard Edition (build 1.3.1-rc1-b19)
 *     Java HotSpot(TM) Client VM (build 1.3.1-rc1-b19, interpreted mode)
 *     >>> debug agent created
 *     >>> thread 0: Signal Dispatcher
 *     >>> Signal Dispatcher suspended ...
 *     >>> ... resumed
 *     >>> agent: threadStatus=ffffffff, suspendStatus=1
 *     "Signal Dispatcher" did not resume
 *     FATAL ERROR in native method: could not recover
 *     Exit Code: 1
 *     Fixed according to 4668512 bug,
 *     Ported from JVMDI.
 *     Modified due to fix of the RFE
 *     5001769 TEST_RFE: remove usage of deprecated GetThreadStatus function
 *     Fixed according to 6221885 test bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:threadstart002 nsk.jvmti.ThreadStart.threadstart002 5
 */

