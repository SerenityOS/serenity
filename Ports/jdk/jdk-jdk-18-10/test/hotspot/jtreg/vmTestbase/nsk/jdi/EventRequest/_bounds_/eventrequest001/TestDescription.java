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
 * @summary converted from VM Testbase nsk/jdi/EventRequest/_bounds_/eventrequest001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the methods of com.sun.jdi.EventRequest:
 *         1. putProperty(Object, Object)
 *         2. getProperty(Object)
 *         3. setSuspendPolicy(int)
 *     to correctly work for boundary values of argument.
 *     The test performs the next testcases:
 *         1. putProperty(null, String) - in this case it is expected
 *            property <null> with value of <String> will be created.
 *            No exception must be thrown.
 *         2. getProperty(null) - value of <String> from the previous
 *            case is expected as return value. No exception must be thrown.
 *         3. setSuspendPolicy(int) is checked for the next parameters:
 *            Integer.MIN_VALUE, -1, Integer.MAX_VALUE. IllegalArgumentException
 *            is expected.
 * COMMENTS:
 *     4777928 TEST_BUG: two jdi tests should expect IllegalArgumentException
 *     Test fixed according to test bug:
 *     4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *     - using standard Debugee.setBreakpoint() method
 *     - adjusting source line numbers used for setting breakpoint
 *     Test fixed according to test bug:
 *     4862601 TEST_BUG: JDI: Unexpected exception was thrown for argument: -2147483648
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequest._bounds_.eventrequest001
 *        nsk.jdi.EventRequest._bounds_.eventrequest001a
 * @run main/othervm
 *      nsk.jdi.EventRequest._bounds_.eventrequest001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

