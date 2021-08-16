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
 * @summary converted from VM Testbase nsk/jdi/SelectedArgument/choices/choices001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Connector.SelectedArgument.
 *     The test checks up that a result of the method
 *     com.sun.jdi.connect.Connector.SelectedArgument.choices()
 *     complies with its specification, in particular,
 *     a returned value is a non-empty "List of String".
 *     Values of Strings are not checked up.
 *     The test works as follows:
 *     - Virtual Machine Manager is invoked.
 *     - First Connector.SelectedArgument object is searched among
 *     Arguments of Connectors.
 *     If no the argument is found out the test exits
 *     with the return value = 95 and a warning message.
 *     - Using the method argument.choices(),
 *     the list of possible values is obtained.
 *     IF   the list is empty
 *     THEN an error detected
 *     - each member of the list is checked up whether
 *     it is a string or not.
 *     IF   the member is not a string
 *     THEN an error detected
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 * Since current version of VMM doesn't implement SelectedArgument
 * only this is detected but other checks are impossible.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.SelectedArgument.choices.choices001
 * @run main/othervm
 *      nsk.jdi.SelectedArgument.choices.choices001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

