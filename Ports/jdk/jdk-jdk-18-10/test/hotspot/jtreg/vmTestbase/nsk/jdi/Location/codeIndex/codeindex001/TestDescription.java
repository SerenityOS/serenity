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
 * @summary converted from VM Testbase nsk/jdi/Location/codeIndex/codeindex001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Location.
 *     The test checks up that a result of the method
 *     com.sun.jdi.Location.codeIndex()
 *     complies with its spec:
 *     public long codeIndex()
 *     Gets the code position within this location's method.
 *     Returns: the long representing the position within the method or -1
 *              if location is within a native method.
 *     when no native methods are tested.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.Location.codeIndex.codeindex 00;
 *     the debuggee program - nsk.jdi.Location.codeIndex.codeindex 00a.
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
 * @build nsk.jdi.Location.codeIndex.codeindex001
 *        nsk.jdi.Location.codeIndex.codeindex001a
 *
 * @comment make sure codeindex001a is compiled with full debug info
 * @clean nsk.jdi.Location.codeIndex.codeindex001a
 * @compile -g:lines,source,vars ../codeindex001a.java
 *
 * @run main/othervm
 *      nsk.jdi.Location.codeIndex.codeindex001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

