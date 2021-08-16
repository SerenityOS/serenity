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
 * @summary converted from VM Testbase nsk/jdi/Scenarios/invokeMethod/popframes001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     Debuggee's part contains a tested class (class B) and debugger exercises
 *     method runIt() of this class by the following steps:
 *     1. On ClassPrepareEvent of class B, MethodExitRequest is created and
 *        debugger waits MethodExitEvent for <clinit> to be shure the static
 *        members of class B have been initialized
 *     2. After getting MethodExitEvent for <clinit>,
 *          - debugger creates MethodEntryRequest
 *          - invokes the tested method (method "runIt") by calling
 *            com.sun.jdi.ClassType.invokeMethod().
 *        This invoking occurs in special thread of debugger's part so that
 *        debugger can process the events of the target VM.
 *     3. When getting MethodEntryEvent from the invoked method (method "runIt"),
 *        debugger tries to pop current frame (with 0 index).
 *     The test passes when the above steps have been successfuly executed.
 * COMMENTS
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *     Test updated to wait for debugee VM exit:
 *     - standard method Debugee.endDebugee() is used instead of final Debugee.resume()
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Scenarios.invokeMethod.popframes001
 *        nsk.jdi.Scenarios.invokeMethod.popframes001a
 * @run main/othervm
 *      nsk.jdi.Scenarios.invokeMethod.popframes001
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

