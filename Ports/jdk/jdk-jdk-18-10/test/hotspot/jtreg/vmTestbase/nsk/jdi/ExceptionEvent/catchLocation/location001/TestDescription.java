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
 * @modules jdk.jdi/com.sun.tools.jdi:+open java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/ExceptionEvent/catchLocation/location001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    This test exercises
 *      com.sun.jdi.event.ExceptionEvent.catchLocation() method.
 *    The test checks the following assertion:
 *       - method ExceptionEvent.catchLocation() returns for the caught
 *         Throwable object a correct Location reference to the first code
 *         index in the appropriate catch clause of enclosing try statement.
 *    A debugger class - nsk.jdi.ExceptionEvent.catchLocation.location001  ;
 *    a debuggee class - nsk.jdi.ExceptionEvent.catchLocation.location001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger launches and connects to debuggee, waits for it started, and
 *    creates the ExceptionRequest with true <notifyCaught> and <notifyUncaught>
 *    parameters, which is disabled by default.
 *    The debugger defines separate <EventHandler> thread for listening received
 *    events, cleans EventQueue, starts <EventHandler>, enables ExceptionRequest,
 *    sends debuggee command <GO> to force it to throw exception, and waits for
 *    notification that all exceptions are thrown.
 *    Upon receiving <GO> command from debugger, the debuggee raises several
 *    exceptions and errors. All these exceptions and errors are enclosed in
 *    separate try-catch blocks. Debuggee verifies that all exceptions are
 *    really thrown and notifies debugger by sending command <DONE> or <ERROR>.
 *    Each received event is handled by separate <EventHandler> thread.
 *    For each ExceptionEvent assertions of the test are verified.
 *    After <DONE> command received from debuggee, debugger notifies <EventHandler>
 *    that all checked exceptions have been thrown and waits for <EventHandler>
 *    receives all expected events. If not all events received for WAITIME interval,
 *    debugger interrupts <EventHandler> thread and complains about an error.
 *    Finally, debugger disables event request, sends debuggee command <QUIT),
 *    cleans EventQueue, waits for deduggee terminates, and exits.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *    Test fixed due to bug:
 *        4455653 VMDisconnectedException on resume
 *    The test was fixed due to the following bug:
 *        4740123 Wrong exception catch location in ExceptionEvent
 *    Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ExceptionEvent.catchLocation.location001
 *        nsk.jdi.ExceptionEvent.catchLocation.location001a
 * @run main/othervm
 *      nsk.jdi.ExceptionEvent.catchLocation.location001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

