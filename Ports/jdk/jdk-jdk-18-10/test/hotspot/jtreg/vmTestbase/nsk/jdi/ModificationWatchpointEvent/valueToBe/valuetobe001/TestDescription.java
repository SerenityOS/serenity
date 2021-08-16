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
 * @summary converted from VM Testbase nsk/jdi/ModificationWatchpointEvent/valueToBe/valuetobe001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    This test exercises the
 *      com.sun.jdi.event.ModificationWatchpointEvent.valueToBe() method.
 *    The test checks the following assertion:
 *      If any field of loaded class of target VM is assigned to new value
 *      which is not equal to previous value, then Value reference
 *      returned by ModificationWatchpointEvent.valueToBe() is not equal
 *      to one returned by Watchpoint.valueCurrent().
 *    A debugger class - nsk.jdi.ModificationWatchpointEvent.valueToBe.valuetobe001  ;
 *    a debuggee class - nsk.jdi.ModificationWatchpointEvent.valueToBe.valuetobe001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates ModificationWatchpointRequests to the fields defined
 *    in debuggee class.
 *    The debugger starts <Eventhandler> thread for listening events delivered
 *    from debuggee.
 *    Upon receiving <GO> command from debugger, the debuggee initializes its
 *    fields and sends command <DONE>.
 *    The debugger switches to <EventHandler> to listen the event during the
 *    time  specified by <waittime> parameter. Then debugger shuts down debuggee
 *    by <QUIT> command.
 *    If debugger receives ModificationWatchpointEvent, it compares old and new
 *    values of field referenced in the event. The test fails if any of this
 *    these values are equal.
 *    The test also fails if no ModificationWatchpointEvent was received.
 * COMMENTS
 *    Test fixed due to bug:
 *        4455653 VMDisconnectedException on resume
 * ----------
 *    I. To fix the bug 4505870, the following is done in file valuetobe001a.java:
 *    - assignments values to the fields lS0, lP0, lU0, lR0, lT0, and lV0
 *      are added (new lines 164-169).
 *    II. Several improvments:
 *    debugger:
 *    - line 129: check value is in requestsCount;
 *    - old line 39 removed;
 *    - check for all expected events received (lines 233-248) removed.
 *    debuggee:
 *    - line 25 removed.
 * ----------
 *    Standard method Debugee.endDebugee() is used instead of cleaning event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ModificationWatchpointEvent.valueToBe.valuetobe001
 *        nsk.jdi.ModificationWatchpointEvent.valueToBe.valuetobe001a
 * @run main/othervm
 *      nsk.jdi.ModificationWatchpointEvent.valueToBe.valuetobe001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

