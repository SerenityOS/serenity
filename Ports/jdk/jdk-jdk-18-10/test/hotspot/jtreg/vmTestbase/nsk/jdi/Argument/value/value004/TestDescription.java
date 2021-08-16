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
 * @summary converted from VM Testbase nsk/jdi/Argument/value/value004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * ================================================
 * DESCRIPTION:
 *         The test is aimed on the control of:
 *         Interface:      com.sun.jdi.connect.Connector.Argument
 *         Method:         public java.lang.String value()
 *         Assertion:      "Returns the current value of the argument."
 * ================================================
 * COMMENTS:
 *         The test aims on the concrete Sun's JDI
 *         reference implementations. It uses
 *         com.sun.jdi.RawCommandLineLaunch connector and its
 *         "command" argument.
 *         The test sets up the new "command" argument
 *         value and then checks that new value remains previously
 *         after connection establishing with debugee VM and
 *         after debugee VM finishing.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - debuggee wrapper is used to handle VMStartEvent
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Argument.value.value004
 *        nsk.jdi.Argument.value.value004a
 * @run main/othervm
 *      nsk.jdi.Argument.value.value004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

