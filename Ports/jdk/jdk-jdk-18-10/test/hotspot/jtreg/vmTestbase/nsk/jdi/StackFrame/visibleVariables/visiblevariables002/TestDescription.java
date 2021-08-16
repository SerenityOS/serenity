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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/visibleVariables/visiblevariables002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     StackFrame.
 *     The test checks up that a result of the method
 *     com.sun.jdi.StackFrame.visibleVariables()
 *     complies with its spec:
 *     public java.util.List visibleVariables()
 *                                 throws AbsentInformationException
 *     Returns a list containing each LocalVariable that
 *     can be accessed from this frame's location.
 *         Visibility is based on the code index of the current instruction of
 *     this StackFrame. Each variable has a range of byte code indices in which
 *     it is accessible. If this stack frame's method matches
 *     this variable's method and if the code index of this StackFrame is within
 *     the variable's byte code range, the variable is visible.
 *         A variable's byte code range is at least as large as the scope of that
 *     variable, but can continue beyond the end of the scope under certain
 *     circumstances:
 *        the compiler/VM does not immediately reuse the variable's slot.
 *        the compiler/VM is implemented to report the extended range that
 *        would result from the item above.
 *     The advantage of an extended range is that variables from recently
 *     exited scopes may remain available for examination (this is especially
 *     useful for loop indices). If, as a result of the extensions above,
 *     the current frame location is contained within the range of
 *     multiple local variables of the same name, the variable with
 *     the highest-starting range is chosen for the returned list.
 *     Returns: the list of LocalVariable objects currently visible.
 *     Throws: AbsentInformationException -
 *             if there is no line number information for this method.
 *             InvalidStackFrameException -
 *             if this stack frame has become invalid.
 *             Once the frame's thread is resumed,
 *             the stack frame is no longer valid.
 *     when a tested program is prepared with no full information,
 *     hence, AbsentInformationException is expected to happen.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.StackFrame.visibleVariables.visiblevariables002;
 *     the debuggee program - nsk.jdi.StackFrame.visibleVariables.visiblevariables002a.
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
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.visibleVariables.visiblevariables002
 *        nsk.jdi.StackFrame.visibleVariables.visiblevariables002a
 * @run main/othervm
 *      nsk.jdi.StackFrame.visibleVariables.visiblevariables002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

