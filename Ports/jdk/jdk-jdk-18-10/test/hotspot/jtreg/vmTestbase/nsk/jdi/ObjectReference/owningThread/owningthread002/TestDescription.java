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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/owningThread/owningthread002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test checks the following assertion of
 *   com.sun.jdi.ObjectReference.owningThread method spec:
 *      Returns an ThreadReference for the thread, if any,
 *      which currently owns this object's monitor.
 *   The debugger program - nsk.jdi.ObjectReference.owningThread.owningthread002;
 *   the debuggee program - nsk.jdi.ObjectReference.owningThread.owningthread002a;
 *   The test works as follows:
 *   Using nsk.jdi.share classes, the debugger connects to the debuggee.
 *   The debuggee's main threads starts a number of auxiliary threads.
 *   Each of them invokes syncronized foo method of owningthread002aLock class.
 *   The debugger creates MethodEntryRequest filtered to class of foo method.
 *   Each time the foo is invoked, the debuggee is suspended. Upon receiving next
 *   MethodEntryEvent, the debugger calls owningThread method for the debuggee's field
 *   of owningthread002aLock type. The returned ThreadReference must have expected name
 *   and be equal to the thread reference returned by thread method of received
 *   MethodEntryEvent .
 *   In case of error the test produces the return value 97 and a corresponding
 *   error message(s). Otherwise, the test is passed and produces the return
 *   value 95 and no message.
 * COMMENTS:
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - remove waiting for VMStartEvent after launching debuggee VM
 *     Test fixed according to test bug:
 *     4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *     - using standard Debugee.makeBreakpoint() method for setting breakpoint
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.owningThread.owningthread002
 *        nsk.jdi.ObjectReference.owningThread.owningthread002a
 * @run main/othervm
 *      nsk.jdi.ObjectReference.owningThread.owningthread002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

