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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/redefineClasses/redefineclasses002.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test against the method com.sun.jdi.VirtualMachine.redefineClasses()
 *     and checks up the following assertion:
 *         The redefined methods will be used on new invokes.
 *         If resetting these frames is desired, use
 *         ThreadReference.popFrames(StackFrame) with Method.isObsolete().
 *     The test consists of the following files:
 *         redefineclasses002.java            - debugger
 *         redefineclasses002a.java           - initial debuggee
 *         newclass/redefineclasses002a.java  - redefining debuggee
 *     This test performs the following steps:
 *         1. Setting breakpoint at line of method, which will be redefined.
 *            This method has a local variable <testedVar> and the breakpoint
 *            is placed after it's initializing.
 *         2. When breakpoint event is arrived, Debugger requests initial
 *            value of <testedVar>
 *         3. While VM suspended Debugger redefines the tested method.
 *            It is added new line, after execution point. That line changes
 *            value of <testedVar>, different from initial-value.
 *         4. While debugee remains suspending, debuger tries to get value of
 *            <testedVar>. In this case AbsentInformationException is expected.
 *         5. Popping the active frame and trying to get value again.
 *            It is expected, method will be not found in active stack frames.
 *         6. Setting breakpoint at line after the new one and resuming debugee.
 *         7. Trying to get new value of <testedVar>. It is expected the last
 *            value will be different from initial-value.
 *     The test should be compiled with "-g" option, so cfg-file redefines
 *     JAVA_OPTS variable.
 * COMMENTS:
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.redefineClasses.redefineclasses002
 *        nsk.jdi.VirtualMachine.redefineClasses.redefineclasses002a
 *
 * @comment make sure redefineclasses002a is compiled with full debug info
 * @clean nsk.jdi.VirtualMachine.redefineClasses.redefineclasses002a
 * @compile -g:lines,source,vars ../redefineclasses002a.java
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.redefineClasses.redefineclasses002
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

