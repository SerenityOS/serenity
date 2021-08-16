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
 * @summary converted from VM Testbase nsk/jdi/ClassType/invokeMethod/invokemethod015.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test for invokeMethod method of ClassType interface.
 *   The test checks if static values() method returns mirrors of enum
 *   constants when it was invoked via checked method for any mirrored
 *   enum type. The values() method is automatically implemented in any
 *   enum type.
 *   The debugger class - nsk.jdi.ClassType.invokeMethod.ivokemethod015;
 *   the debuggee class - nsk.jdi.ClassType.invokeMethod.ivokemethod015a;
 *   The test works as follows. At first, preliminary phase:
 *    - the debugger connects to the debuggee using nsk.jdi.share classes;
 *    - the debugger waits for the VMStartEvent and requested ClassPrepareEvent for
 *      debuggee class.
 *   At second, test specific phase, each action is performed for every test case:
 *    - the debugger waits for requested BreakpointEvent which is set on a line
 *      of debuggee's methodForCommunication() method. This breakpoint allows the
 *      debugger to obtain ThreadReference mirror of debuggee's main thread;
 *    - after getting the BreakpointEvent, the debugger finds mirrors of
 *      invokemethod015aEnum enum type and of values() method;
 *    - then the debugger calls invokeMethod with found mirrors as parameters
 *      and check if returned list is of correct size and contains ObjectRefences
 *      for every declared enum constant.
 *   In case of error the test produces the exit code 97 and a corresponding
 *   error message(s). Otherwise, the test is passed with exit code 95.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassType.invokeMethod.invokemethod015
 *        nsk.jdi.ClassType.invokeMethod.invokemethod015a
 * @run main/othervm
 *      nsk.jdi.ClassType.invokeMethod.invokemethod015
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

