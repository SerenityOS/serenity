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
 * @summary converted from VM Testbase nsk/jdi/Argument/isValid/isvalid002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Connector.Argument.
 *     The test checks up that a result of the method
 *     com.sun.jdi.connect.Connector.Argument.isValid()
 *     complies with its specification, that is,
 *     "Returns: true if the value is valid to be used in setValue(String)"
 *     in case when Argument implements BooleanArgument
 *     and its parameter is a null-string .
 *     The test works as follows:
 *     - Virtual Machine Manager is invoked.
 *     - First Connector.BooleanArgument object is searched among
 *     Arguments of Connectors.
 *     If no the object is found out the test exits
 *     with the return value = 95 and a warning message.
 *     - To a Connector.Argument argument object
 *     the following check is applied:
 *     String sNull = null;
 *     IF    argument.isValid(sNull)
 *     THEN  an error detected
 *     ELSE  the check passed
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 * To fix the bug 4491137,
 * the test is corrected to comply with
 * the updated specification in jdi-overview.html:
 *     Any method which takes a Object as an parameter
 *     will throw NullPointerException if
 *     null is passed directly or indirectly --
 *     unless null is explicitly mentioned as a valid parameter.
 * From now, the test regards throwing NullPointerException
 * as the correct reaction to the null-parameter.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Argument.isValid.isvalid002
 * @run main/othervm
 *      nsk.jdi.Argument.isValid.isvalid002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

