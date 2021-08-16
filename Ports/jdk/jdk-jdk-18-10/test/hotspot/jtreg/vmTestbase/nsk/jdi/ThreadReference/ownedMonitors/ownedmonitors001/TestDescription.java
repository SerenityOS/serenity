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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitors/ownedmonitors001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up that an implementation of the method
 *     com.sun.jdi.ThreadReference.ownedMonitors() conforms with its spec.
 *     The test checks an assertion:
 *       Returns a List containing an ObjectReference for each monitor owned by the thread.
 *       A monitor is owned by a thread if it has been entered (via the synchronized statement
 *       or entry into a synchronized method) and has not been relinquished through Object.wait(long).
 *     There are two test cases:
 *       - for a thread with no owned monitors,
 *       - for a thread with owned monitors, entered via the synchronized statement.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001;
 *     the debuggee program - nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001a.
 *     Communication details between the debugger and the debuggee:
 *        Using nsk.jdi.share classes, the debugger connects to debuggee program running
 *        on another VM and establishes a communication pipe with the debuggee. The pipe
 *        is used in bi-directional way by sending and receiving special commands between
 *        the debugger and debuggee for synchronization
 *     In case of error the test produces the exit code 97 and a corresponding error
 *     message(s). Otherwise, the test is passed and produces the exit code 95 and
 *     no message.
 * COMMENTS:
 *     4769563 NSK ownedmonitors001 sends an incorrect test-sync mesg
 *     5009132 nsk/jdi/ThreadReference/ownedMonitors/ownedmonitors001 monitor count fail
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001
 *        nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

