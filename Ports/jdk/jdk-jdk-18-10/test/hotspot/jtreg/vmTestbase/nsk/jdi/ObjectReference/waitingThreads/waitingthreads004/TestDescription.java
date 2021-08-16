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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/waitingThreads/waitingthreads004.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks an following assertion of
 *     com.sun.jdi.ObjectReference.waitingThreads method spec:
 *        Returns a List containing a ThreadReference for each thread currently
 *        waiting for this object's monitor.
 *     There are two test cases:
 *        - An object with no waiting threads.
 *          A list with zero size is expected to be returned by the method.
 *        - An object with threads waiting in Object.wait(long) method.
 *          The debugger checks with expected results:
 *            - a size of returned list of ThreadReferences,
 *            - the names of thread references,
 *            - whether the thread reference has the same contented monitor object
 *              as checked one.
 *     The debugger program - nsk.jdi.ObjectReference.waitingThreads.waitingthreads004;
 *     the debuggee program - nsk.jdi.ObjectReference.waitingThreads.waitingthreads004a.
 *     Communication details between the debugger and the debuggee:
 *        Using nsk.jdi.share classes, the debugger connects to debuggee program running
 *        on another VM and establishes a communication pipe with the debuggee. The pipe
 *        is used in bi-directional way by sending and receiving special commands between
 *        the debugger and debuggee for synchronization
 *     In case of error the test produces the exit code 97 and a corresponding error
 *     message(s). Otherwise, the test is passed and produces the exit code 95 and
 *     no message.
 * COMMENTS:
 *  4883502 TEST_BUG: waiting Threads testcases not predictable
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.waitingThreads.waitingthreads004
 *        nsk.jdi.ObjectReference.waitingThreads.waitingthreads004a
 * @run main/othervm
 *      nsk.jdi.ObjectReference.waitingThreads.waitingthreads004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

