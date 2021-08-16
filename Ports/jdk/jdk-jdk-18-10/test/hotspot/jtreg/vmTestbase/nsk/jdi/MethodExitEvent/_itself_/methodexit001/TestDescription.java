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
 * @summary converted from VM Testbase nsk/jdi/MethodExitEvent/_itself_/methodexit001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises
 *        com.sun.jdi.event.MethodExitEvent.method() method.
 *    The test checks the following assertions:
 *        - MethodExitEvent is not received by debugger if the method
 *          terminated with thrown exception in debugged VM;
 *    A debugger class - nsk.jdi.MethodExitEvent._itself_.methodexit001  ;
 *    a debuggee class - nsk.jdi.MethodExitEvent._itself_.methodexit001a .
 *    The test uses supporting nsk/share/jdi classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates MethodEntryRequest filtered to <method001a> class.
 *    Also it sets two breakpoint in debuggee before and after invoking
 *    checked method <foo>.
 *    The debugger starts <EventHandler> thread for listening events delivered
 *    from debuggee.
 *    Upon receiving <GO> command from debugger, the debuggee recurcively
 *    invokes <foo()> method. This method throws user-defined uncatched exception
 *    <methodexit001e>.
 *    The debugger switches to <EventHandler> to listen the event during the
 *    time specified by <waittime> parameter. When BreakpointEvent for the first
 *    breakpoint is received, MethodEntryRequest becomes enabled. <EventHandler>
 *    checks all received MetodEntryEvents until second breakpoint is reached
 *    and MethodEntryRequest becomes disabled.
 *    If debugger receives MethodEntryEvent for the checked method it
 *    complains an error message.
 *    Finally, debugger sends debuggee command QUIT and checks number of recieved
 *    events.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *    Test fixed due to bugs:
 *      4455653 VMDisconnectedException on resume
 *      4433805 EventSet.resume() for VMStartEvent resumes thread suspended by breakpoint request
 *    Standard method Debugee.endDebugee() is used instead of cleaning event queue on debuggee VM exit.
 *    Test fixed according to test bug:
 *      4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *      - using standard Debugee.setBreakpoint() method for setting breakpoint
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.MethodExitEvent._itself_.methodexit001
 *        nsk.jdi.MethodExitEvent._itself_.methodexit001a
 * @run main/othervm
 *      nsk.jdi.MethodExitEvent._itself_.methodexit001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

