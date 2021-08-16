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
 * @summary converted from VM Testbase nsk/jdi/BooleanArgument/setValue/setvalue002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The second test for the implementation of
 *     a BooleanArgument object.
 *     The test checks up that results of the method
 *     com.sun.jdi.connect.Connector.BooleanArgument.setValue()
 *     complies with specification.
 *     The test works as follows:
 *     - Virtual Machine Manager is invoked.
 *     - First BooleanArgument is searched among Connectors.
 *     If no a BooleanArgument is found out the test exits with
 *     the return value = 95 and a warning message.
 *     - Under assumption that method booleanValue() works correctly (!!),
 *     to the value of the BooleanArgument founded,
 *     which may not have been set or may have an invalid value,
 *     the check is applied:
 *         setValue(false);  booleanValue() must return false
 *     In case of the check results in a wrong value,
 *     the test produces the return value 97 and
 *     a corresponding error message.
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BooleanArgument.setValue.setvalue002
 * @run main/othervm
 *      nsk.jdi.BooleanArgument.setValue.setvalue002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

