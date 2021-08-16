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
 * @summary converted from VM Testbase nsk/jdi/Method/arguments/arguments001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Method.
 *     The test checks up that a result of the method
 *     com.sun.jdi.Method.arguments()
 *     complies with its spec:
 *     public java.util.List arguments()
 *                          throws AbsentInformationException
 *      Returns a list containing each LocalVariable that is declared as
 *      an argument of this method.
 *      Returns: the list of LocalVariable arguments.
 *               If there are no arguments, a zero-length list is returned.
 *      Throws: AbsentInformationException -
 *              if there is no variable information for this method.
 *              NativeMethodException -
 *              if this operation is attempted for a native method.
 *     when a debugged program is compiled with the '-g' option,
 *     hence, no AbsentInformationException is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.Method.arguments.arguments001;
 *     the debuggee program - nsk.jdi.Method.arguments.arguments001a.
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
 * --------------
 * Fixing bug 4453137:
 * To comply with the latest Merlin specification
 * which differs the Ladybird and early Merlin specifications,
 * the test is corrected as follows:
 * - for native method,
 *   the check on throwing AbsentInformationException is performed.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.arguments.arguments001
 *        nsk.jdi.Method.arguments.arguments001a
 *
 * @comment make sure arguments001a is compiled with full debug info
 * @clean nsk.jdi.Method.arguments.arguments001a
 * @compile -g:lines,source,vars ../arguments001a.java
 *
 * @run main/othervm
 *      nsk.jdi.Method.arguments.arguments001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

