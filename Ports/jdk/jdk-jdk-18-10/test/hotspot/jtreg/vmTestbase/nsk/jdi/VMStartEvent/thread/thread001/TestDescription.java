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
 * @summary converted from VM Testbase nsk/jdi/VMStartEvent/thread/thread001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test exercises
 *     com.sun.jdi.event.VMStartEvent.thread() method.
 *   The test checks the following assertions:
 *     - VMStartEvent is received without creating any EventRequest,
 *     - VMStarEvent.thread() returns valid ThreadReference
 *        to the thread in target VM,
 *    A debugger class - nsk.jdi.VMStartEvent.thread.thread001  ;
 *    a debuggee class - nsk.jdi.VMStartEvent.thread.thread001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger starts special thread <EventHandler> for listening events
 *    delivered from debuggee.
 *    The debugger resumes debuggee and waits for command <READY>.
 *    Upon starting debuggee sends command <READY> and waits for command <QUIT>
 *    from debugger and completes upon receiving.
 *    The debugger switches <EventHandler> to listen the event during
 *    the time specified by <waittime> parameter.
 *    If the debugger receives VMStartEvent, it checks all assertions
 *    of the test.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *    Test fixed due to bug:
 *      4455653 VMDisconnectedException on resume
 *    Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *      - standard method Debugee.endDebugee() is used instead of cleaning event queue
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VMStartEvent.thread.thread001
 *        nsk.jdi.VMStartEvent.thread.thread001a
 * @run main/othervm
 *      nsk.jdi.VMStartEvent.thread.thread001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

