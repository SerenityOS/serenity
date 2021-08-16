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
 * @summary converted from VM Testbase nsk/jdi/BreakpointEvent/_itself_/breakpoint001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises com.sun.jdi.event.BreakpointEvent interface.
 *    The test checks the following assertions:
 *        - BreakpointEvent is received by debugger for requested
 *          location if this location is reached by some executing thread
 *          in debugged VM;
 *        - received BreakpointEvent has proper references to:
 *            debugged VM,
 *            executed thread,
 *            related BreakpointRequest,
 *            checked location.
 *        - BreakpointEvent is generated before the code at its location
 *          is executed.
 *    A debugger class - nsk.jdi.BreakpointEvent._itself_.breakpoint001  ;
 *    a debuggee class - nsk.jdi.BreakpointEvent._itself_.breakpoint001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger launches and connects to debuggee using the default launching
 *    connector. Afer command <READY> is received from debugge, the debugger
 *    creates  BreakpointRequest on location in <foo> method of <breakpoint001a>
 *    class where <counter> field is modified.
 *    The debugger cleans EventQueue, starts separate <EventHandler> thread
 *    for listening events, enables breakpoint request, sends debuggee command
 *    <GO> and waits for <DONE> in reply.
 *    Upon receiving <GO> command from debugger, the debuggee invokes <foo()>
 *    method. This method updates <counter> field of <breakpoint001a> class.
 *    For each BreakpointEvent received by <EventHandler>, all refencies of this
 *    event ( <method>, <thread>, <request>, <virtualMachine> ) are compared
 *    with expected values. The current value of <counter> field reference
 *    is compared with expected value.
 *    After <DONE> command received from debuggee, debugger notifies
 *    <EventHandler> that method is invoked and waits for <EventHandler>
 *    receives all expected events. If not all events received for
 *    WAITTIME interval, debugger interrupts <EventHandler> thread and
 *    complains about an error.
 *    Finally, debugger disables checked request, sends debuggee command <QUIT),
 *    cleans events queue, waits for deduggee terminates, and exits.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *        Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4433805 EventSet.resume() for VMStartEvent resumes thread suspended
 *                by breakpoint request
 *        Standard method Debugee.endDebugee() is used instead of cleaning
 *        event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BreakpointEvent._itself_.breakpoint001
 *        nsk.jdi.BreakpointEvent._itself_.breakpoint001a
 * @run main/othervm
 *      nsk.jdi.BreakpointEvent._itself_.breakpoint001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

