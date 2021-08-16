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
 * @summary converted from VM Testbase nsk/jdi/Method/locationsOfLine_ssi/locationsofline_ssi001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Method.
 *     The test checks up that a result of the method
 *     com.sun.jdi.Method.locationsOfLine(String,String,int)
 *     complies with its spec:
 *     public List locationsOfLine(String stratum, String sourceName, int lineNumber)
 *                      throws AbsentInformationException
 *      Returns a List containing all Location objects that map to
 *      the given line number and source name.
 *      Returns a list containing each Location that maps to the given line.
 *      The returned list will contain a location for each disjoint range of
 *      code indices that have been assigned to the given line by the compiler
 *      and/or VM. Each returned location corresponds to the beginning of
 *      this range. An empty list will be returned if
 *      there is no executable code at the specified line number;
 *      specifically, native and abstract methods will always return an empty list.
 *      Returned list is for the specified stratum
 *      (see Location for a description of strata).
 *      Parameters: stratum - the stratum to use for comparing line number and
 *                            source name, or null to use the default stratum
 *                  sourceName - the source name containing the line number, or
 *                               null to match all source names
 *                  lineNumber - the line number
 *      Returns: a List of Location objects that map to the given line number.
 *      Throws: AbsentInformationException -
 *              if there is no line number information for this method.
 *              Or if sourceName is non-null
 *              and source name information is not present.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001;
 *     the debuggee program - nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001a.
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
 *     The option
 *         JAVAC_OPTS=-g
 *     is put in the locationsofline002.cfg file.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001
 *        nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001a
 *
 * @comment make sure locationsofline_ssi001a is compiled with full debug info
 * @clean nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001a
 * @compile -g:lines,source,vars ../locationsofline_ssi001a.java
 *
 * @run main/othervm
 *      nsk.jdi.Method.locationsOfLine_ssi.locationsofline_ssi001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

