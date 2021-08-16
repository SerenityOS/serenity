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
 * @summary converted from VM Testbase nsk/jdi/EventQueue/remove_l/remove_l005.
 * VM Testbase keywords: [jpda, jdi, quarantine]
 * VM Testbase comments: 8068225
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the method
 *         com.sun.jdi.event.EventQueue.remove(long)
 *     for boundary value of parameters.
 *     Test check up this method for the following arguments:
 *     Long.MIN_VALUE, -1 and Long.MAX_VALUE.
 *     IllegalArgumentException is expected for negative arguments.
 * COMMENTS:
 *     4777928 TEST_BUG: two jdi tests should expect IllegalArgumentException
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - implicit handling VMStartEvent was removed from the debugger part of the test
 *     - BreakpointEvent is used instead for testing EventQueue.remove()
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventQueue.remove_l.remove_l005
 *        nsk.jdi.EventQueue.remove_l.remove_l005a
 * @run main/othervm
 *      nsk.jdi.EventQueue.remove_l.remove_l005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

