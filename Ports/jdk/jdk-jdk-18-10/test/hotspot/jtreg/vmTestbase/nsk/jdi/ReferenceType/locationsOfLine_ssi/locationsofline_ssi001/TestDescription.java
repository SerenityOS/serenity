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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/locationsOfLine_ssi/locationsofline_ssi001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ReferenceType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ReferenceType.locationsOfLine(String,String,int)
 *     complies with its spec:
 *     public List locationsOfLine(String stratum, String sourceName, int lineNumber)
 *                      throws AbsentInformationException
 *      Returns a List containing all Location objects that
 *      map to the given line number.
 *      For arrays (ArrayType) and primitive classes,
 *      the returned list is always empty.
 *      For interfaces (InterfaceType), the returned list will be non-empty only
 *      if the interface has executable code in its class initialization
 *      at the specified line number. An empty list will be returned
 *      if there is no executable code at the specified line number.
 *      Returned list is for the specified stratum
 *      (see Location for a description of strata).
 *      Parameters: stratum - the stratum to use for comparing line number and
 *                            source name, or null to use the defaultStratum().
 *                  sourceName - the source name containing the line number, or
 *                               null to match all source names
 *                  lineNumber - the line number
 *      Returns: a List of all Location objects that map to the given line.
 *      Throws: AbsentInformationException -
 *              if there is no line number information for this class.
 *              Or if sourceName is non-null and source name information is not present.
 *              ClassNotPreparedException -
 *              if this class not yet been prepared.
 *     The test checks up that for each line in the List returned
 *     by the method ReferenceType.allLineLocations(),
 *     the method locationsOfLine(defaultStratum,null,lineNumber)
 *     returns non-empty List object in which each object is a Location object.
 *     Not throwing AbsentInformationException is checked up as well.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001;
 *     the debuggee program - nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001a.
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
 * @build nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001
 *        nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001a
 *
 * @comment make sure locationsofline_ssi001a is compiled with full debug info
 * @clean nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001a
 * @compile -g:lines,source,vars ../locationsofline_ssi001a.java
 *
 * @run main/othervm
 *      nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsofline_ssi001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

