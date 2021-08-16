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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/getValue/getvalue002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     StackFrame.
 *     The test checks up that a result of the method
 *     com.sun.jdi.StackFrame.getValue()
 *     complies with its spec:
 *     public Value getValue(LocalVariable variable)
 *     Gets the Value of a LocalVariable in this frame.
 *     The variable must be valid for this frame's method and visible
 *     according to the rules described in visibleVariables().
 *     Parameters: variable - the LocalVariable to be accessed
 *     Returns:   the Value of the instance field.
 *     Throws: java.lang.IllegalArgumentException -
 *             if the variable is either invalid for this frame's method or
 *             not visible.
 *             InvalidStackFrameException -
 *             if this stack frame has become invalid.
 *             Once the frame's thread is resumed,
 *             the stack frame is no longer valid.
 *             VMMismatchException -
 *             if a Mirror argument and this mirror do not belong to
 *             the same VirtualMachine.
 *     when a tested program is prepared with full information (see COMMENTS),
 *     hence, AbsentInformationException is not expected to happen.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.StackFrame.getValue.getvalue002;
 *     the debuggee program - nsk.jdi.StackFrame.getValue.getvalue002a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM,
 *     establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies
 *     via the pipe. Upon getting reply,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and compares the data got to the data expected.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     This test is option depended,
 *     that is its .cfg file contains the option
 *         JAVAC_OPTS=-g
 *     because at the date of preparing the test
 *     javac prepared full information for the test only
 *     been invoked with the option.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.getValue.getvalue002
 *        nsk.jdi.StackFrame.getValue.getvalue002a
 *
 * @comment make sure getvalue002a is compiled with full debug info
 * @clean nsk.jdi.StackFrame.getValue.getvalue002a
 * @compile -g:lines,source,vars ../getvalue002a.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame.getValue.getvalue002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

