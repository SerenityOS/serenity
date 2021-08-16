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
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/StepEvent/_itself_/stepevent002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises com.sun.jdi.event.StepEvent interface.
 *    The test checks the following assertions:
 *        - if debugger creates StepRequest with depth equals to
 *          STEP_INTO, then for every received StepEvent frame count
 *          of thead's stack is increased every time.
 *    A debugger class - nsk.jdi.StepEvent._itself_.stepevent002  ;
 *    a debuggee class - nsk.jdi.StepEvent._itself_.stepevent002a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger launches and connects to debuggee using the default launching
 *    connector. The debugger creates the StepRequest to receive StepEvent
 *    with size equals to STEP_LINE and depth equals to STEP_INTO.
 *    The debugger clears event queue and starts <EventHandler> thread
 *    to handle received events asynchronously. After <eventHandler> started
 *    debugger enables Steprequest and sends debuggee command <GO> to
 *    force it to invoke checked method in checked thread.
 *    Upon receiving <GO> command from debugger, the debuggee permits
 *    started thread to invokes <foo()> recursively method. The <foo()>
 *    method of is written at a single line to avoid generating
 *    step events of line changing while execution. When thread finished
 *    invoking <foo()> method it stopped and debuggee sends debugger
 *    command <DONE>.
 *    If <EventHandler> in debugger receives StepEvent, it compares all
 *    refencies of this event ( <request>, <virtualMachine>, <method>, <thread> )
 *    with expected values. It is checked that current count of frame of
 *    checked thread is more or equal to count of frame at the moment of
 *    previous step event.
 *    When debugger receives command <DONE> from debuggee it notifies <EventHandler>
 *    that all metod invokations in the checked thread done and waits for
 *    <Eventhandler> finishes. <EventThread> finishes only if all expected events
 *    received. If no all events received for WAITTIME period, debugger interrupts
 *    <EventHandler>, checks number of received events, and complains about an error.
 *    Finally, debugger sends debuggee command to quit, clears event queue,
 *    waits for debugee finished, and exits.
 *    The test fails if any of this checks failed.
 * COMMENTS
 *    Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4463674: TEST_BUG: some JDI tests are timing dependent
 *    Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StepEvent._itself_.stepevent002
 *        nsk.jdi.StepEvent._itself_.stepevent002a
 * @run main/othervm
 *      nsk.jdi.StepEvent._itself_.stepevent002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

