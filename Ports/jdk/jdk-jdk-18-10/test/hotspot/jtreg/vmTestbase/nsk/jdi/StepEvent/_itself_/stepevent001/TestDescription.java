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
 * @summary converted from VM Testbase nsk/jdi/StepEvent/_itself_/stepevent001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises com.sun.jdi.event.StepEvent interface.
 *    The test checks the following assertions:
 *        - StepEvent is received by debugger for requested
 *          location if this location is reached by some executing thread
 *          in debugged VM;
 *        - received StepEvent has proper references to:
 *            debugged VM,
 *            executed thread,
 *            related StepRequest,
 *            checked location.
 *        - StepEvent is generated before the code at its location
 *          is executed.
 *    A debugger class - nsk.jdi.StepEvent._itself_.stepevent001  ;
 *    a debuggee class - nsk.jdi.StepEvent._itself_.stepevent001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger launches and connects to debuggee using the default launching
 *    connector. The debugger creates auxiliary BreakpointRequest on location in
 *    <foo> method of <stepevent001a> class where <counter> field is assigned
 *    to zero. The <counter> field in incremented on next lines of this method
 *    until exit. Also debugger creates but disables the StepRequest to receive
 *    StepEvent with size equals to STEP_LINE and depth equals to STEP_OVER
 *    The debugger starts <EventHandler> thread for listening events delivered
 *    from debuggee, sends command <GO> to debuggee and waits for command <DONE>.
 *    Upon receiving <GO> command from debugger, the debuggee invokes <foo()>
 *    method and, after method has been invoked, sends command <DONE> to debugger.
 *    When BreakpointEvent is received by <EventHandler>, it enables
 *    StepRequest.
 *    For each StepEvent received by <EventHandler>, it compares all refencies
 *    of this event ( <method>, <thread>, <request>, <virtualMachine> )
 *    with expected values. The current value of <counter> field reference
 *    is compared with expected value.
 *    When StepEvent for last checked line is received, <EventHadler>
 *    disables StepRequest to prevent further generation of StepEvents.
 *    When <DONE> command received from debuggee, debugger notifies
 *    <EventHandler> that method is invoked and waits for <EventHandler>
 *    receives all expected events. If not all events received for
 *    WAITIME period, debugger interrupts <EventHandler> thread and
 *    complains about an error.
 *    Finally, debugger disables all requests, sends debuggee command <QUIT),
 *    cleans events queue, waits for deduggee terminates, and exits.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *    Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4463674 TEST_BUG: some JDI tests are timing dependent
 *    Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StepEvent._itself_.stepevent001
 *        nsk.jdi.StepEvent._itself_.stepevent001a
 * @run main/othervm
 *      nsk.jdi.StepEvent._itself_.stepevent001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

