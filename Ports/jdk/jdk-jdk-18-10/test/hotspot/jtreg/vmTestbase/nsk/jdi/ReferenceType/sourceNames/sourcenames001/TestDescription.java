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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/sourceNames/sourcenames001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ReferenceType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ReferenceType.sourceNames()
 *     complies with its spec:
 *     public List sourceNames(String stratum)
 *                  throws AbsentInformationException
 *      Gets the identifying names for all the source corresponding to
 *      the declaration of this type. Interpretation of these names is the
 *      responsibility of the source repository mechanism.
 *      The returned names are for the specified stratum
 *      (see Location for a description of strata).
 *      In the reference implementation, when using the Java programming language stratum,
 *      the returned List contains one element:
 *      a String which is the unqualified name of the source file
 *      containing the declaration of this type.
 *      In other strata the returned source names are
 *      all the source names defined for that stratum.
 *      Parameters: stratum - The stratum to retrieve information from or
 *                            null for the declaring type's default stratum.
 *      Returns: a List of String objects each representing a source name
 *      Throws: AbsentInformationException -
 *              if the source names are not known.
 *              For arrays (ArrayType) and primitive classes,
 *              AbsentInformationException is always thrown.
 *     The case for testing includes a Class type in a debuggee, and
 *     debuggee's sourcecode file with a predefined name.
 *     No AbsentInformationException is expected to be thrown.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ReferenceType.sourceNames.sourcenames001;
 *     the debuggee program - nsk.jdi.ReferenceType.sourceNames.sourcenames001a.
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
 * @build nsk.jdi.ReferenceType.sourceNames.sourcenames001
 *        nsk.jdi.ReferenceType.sourceNames.sourcenames001a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.sourceNames.sourcenames001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

