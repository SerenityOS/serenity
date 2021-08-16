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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/entryCount/entrycount002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test checks the following assertion of
 *   com.sun.jdi.ObjectReference.entryCount method spec:
 *      Returns the number times this object's monitor has been entered
 *      by the current owning thread.
 *   The debugger program - nsk.jdi.ObjectReference.entryCount.entrycount002;
 *   the debuggee program - nsk.jdi.ObjectReference.entryCount.entrycount002a;
 *   The test works as follows:
 *   Using nsk.jdi.share classes, the debugger connects to the debuggee.
 *   The debuggee invokes syncronized foo method of entrycount002aLock class.
 *   The foo method is recursive : invokes itself with the argument substracted 1
 *   until 0 is reached. The debugger creates MethodEntryRequest filtered
 *   to class of foo method. Each time the foo is invoked, the debuggee is
 *   suspended. Upon receiving next MethodEntryEvent, the debugger calls
 *   entryCount method for the debuggee's field of entrycount002aLock type.
 *   The returned value must be equal to the expected one - the level of
 *   recursion.
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
 * @build nsk.jdi.ObjectReference.entryCount.entrycount002
 *        nsk.jdi.ObjectReference.entryCount.entrycount002a
 * @run main/othervm
 *      nsk.jdi.ObjectReference.entryCount.entrycount002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

