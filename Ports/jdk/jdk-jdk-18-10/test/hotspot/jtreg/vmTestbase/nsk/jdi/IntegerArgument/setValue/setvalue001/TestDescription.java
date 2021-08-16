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
 * @summary converted from VM Testbase nsk/jdi/IntegerArgument/setValue/setvalue001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Connector.IntegerArgument.
 *     The test checks up that a result of the methods
 *     IntegerArgument.setValue(int value) and
 *     Argument.setValue(String value)
 *     complies with the following requirements:
 *       "Sets the value of the argument."
 *     when Argument is IntegerArgument.
 *     The testing values are as follows:
 *         min()
 *         max()
 *         min()+1
 *         min()-1>
 *         max()+1
 *     The test works as follows:
 *     - Virtual Machine Manager is invoked.
 *     - First Connector.IntegerArgument object is searched among
 *     Arguments of Connectors.
 *     If no the intArgument is found out the test exits
 *     with the return value = 95 and a warning message.
 *     - To the above values the following check is applied:
 *         intArgument.setValue(int i);
 *         intArgument.setValue(int i1);
 *         intArgument.setValue(int i);
 *         intArgument.setValue(intArgument.stringValueOf(i1));
 *         intArgument.setValue(intArgument.stringValueOf(i));
 *         intArgument.setValue(int i1);
 *         intArgument.setValue(intArgument.stringValueOf(i));
 *         intArgument.setValue(intArgument.stringValueOf(i1));
 *     In all checks the value returned by followed intArgument.intValue()
 *     must be equal to int i1.
 *     In case of inequality of the values
 *     the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.IntegerArgument.setValue.setvalue001
 * @run main/othervm
 *      nsk.jdi.IntegerArgument.setValue.setvalue001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

