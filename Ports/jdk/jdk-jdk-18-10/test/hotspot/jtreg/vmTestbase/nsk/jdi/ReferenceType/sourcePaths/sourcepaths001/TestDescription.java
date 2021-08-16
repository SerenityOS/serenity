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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/sourcePaths/sourcepaths001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ReferenceType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ReferenceType.sourcePaths()
 *     complies with its spec:
 *     public List sourcePaths(String stratum)
 *                  throws AbsentInformationException
 *      Gets the paths to the source corresponding to the declaration of this type.
 *      Interpretation of these paths is the responsibility of
 *      the source repository mechanism.
 *      The returned paths are for the specified stratum
 *      (see Location for a description of strata).
 *      In the reference implementation, for strata
 *      which do not explicitly specify source path
 *      (the Java programming language stratum never does),
 *      the returned strings are the sourceNames(String) prefixed by
 *      the package name of this ReferenceType converted to
 *      a platform dependent path. For example, on a Windows platform,
 *      java.lang.Thread would return a List containing one element:
 *      "java\lang\Thread.java".
 *      Parameters: stratum - The stratum to retrieve information from or
 *                            null for the declaring type's default stratum.
 *      Returns: a List of String objects each representing a source path
 *      Throws:  AbsentInformationException -
 *               if the source names are not known.
 *               For arrays (ArrayType) and primitive classes,
 *               AbsentInformationException is always thrown.
 *      The case for testing debuggee's sourcecode file with a predefined name
 *      and a predefined package name.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ReferenceType.sourcePaths.sourcepaths001;
 *     the debuggee program - nsk.jdi.ReferenceType.sourcePaths.sourcepaths001a.
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
 * @build nsk.jdi.ReferenceType.sourcePaths.sourcepaths001
 *        nsk.jdi.ReferenceType.sourcePaths.sourcepaths001a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.sourcePaths.sourcepaths001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

