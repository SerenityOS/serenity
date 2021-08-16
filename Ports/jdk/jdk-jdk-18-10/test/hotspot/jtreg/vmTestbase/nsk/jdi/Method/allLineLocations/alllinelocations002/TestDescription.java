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
 * @summary converted from VM Testbase nsk/jdi/Method/allLineLocations/alllinelocations002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Method.
 *     The test checks up that a result of the method
 *     com.sun.jdi.Method.allLineLocations()
 *     complies with its spec:
 *     public java.util.List allLineLocations()
 *                                 throws AbsentInformationException
 *     Returns the beginning Location objects for each executable source line in
 *     this method. Each location maps a source line to a range of code indices.
 *     The beginning of the range can be determined through Location.codeIndex().
 *     The returned list is ordered by code index (from low to high).
 *     The returned list may contain multiple locations for
 *     a particular line number, if the compiler and/or VM has mapped that line to
 *     two or more disjoint code index ranges.
 *     If the method is native or abstract, an empty list is returned.
 *     Returns: a List of all source line Location objects.
 *     Throws: AbsentInformationException -
 *             if there is no line number information for this
 *             (non-native, non-abstract) method.
 *     when a tested method is non-native, non-abstract and
 *     when no AbsentInformationException is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.Method.allLineLocations.alllinelocations002;
 *     the debuggee program - nsk.jdi.Method.allLineLocations.alllinelocations002a.
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
 *     In general, this test is potentially option depended
 *     since its spec states:
 *     Throws: AbsentInformationException -
 *             if there is no line number information for this
 *             (non-native, non-abstract) method.
 *     However, unlike some tests that need to be compiled with the option
 *         JAVAC_OPTS=-g
 *     (method.arguments(), method.variables() and method.variablesByName)
 *     at the date of implementing the test,
 *     javac prepares full information for the test without the option.
 *     Nonetheless, the alllinelocations002.cfg file contains the option
 *     in order to avoid possible failures in future in case of any changes
 *     in javac.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.allLineLocations.alllinelocations002
 *        nsk.jdi.Method.allLineLocations.alllinelocations002a
 *
 * @comment make sure alllinelocations002a is compiled with full debug info
 * @clean nsk.jdi.Method.allLineLocations.alllinelocations002a
 * @compile -g:lines,source,vars ../alllinelocations002a.java
 *
 * @run main/othervm
 *      nsk.jdi.Method.allLineLocations.alllinelocations002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

