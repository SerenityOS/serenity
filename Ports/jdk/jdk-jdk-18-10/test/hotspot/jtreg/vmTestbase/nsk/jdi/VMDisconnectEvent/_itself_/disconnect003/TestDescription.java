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
 * @summary converted from VM Testbase nsk/jdi/VMDisconnectEvent/_itself_/disconnect003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises com.sun.jdi.event.VMDisconnectEvent interface.
 *    The test checks the following assertion:
 *        - VMDisconnectEvent is received by debugger after invocation
 *          com.sun.jdi.VirtualMachine.exit(int) method for target VM.
 *        - VMDeathEvent is received without creating any EventRequest.
 *    A debugger class - nsk.jdi.VMDisconnectEvent._itself_.disconnect003  ;
 *    a debuggee class - nsk.jdi.VMDisconnectEvent._itself_.disconnect003a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger starts special thread <EventHandler> for listening events
 *    delivered from debuggee.
 *    Then the debugger invokes VirtualMachine.exit() method.
 *    The debugger switches <EventHandler> to to listen the event during
 *    the time specified by <waittime> parameter.
 *    If the debugger receives VMDisconnectEvent, it checks all assertions
 *    of the test. Since debugee may been terminated this time,
 *    debugger carefully handles VMDisconnectedException.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *    Test fixed due to bug:
 *      4455653 VMDisconnectedException on resume
 *    Test was fixed according to test bug:
 *      4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *      - initial IOPipe synchronization is used before starting event handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VMDisconnectEvent._itself_.disconnect003
 *        nsk.jdi.VMDisconnectEvent._itself_.disconnect003a
 * @run main/othervm
 *      nsk.jdi.VMDisconnectEvent._itself_.disconnect003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

