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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/multithrd/tc01x001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 2 - Breakpoints (multiple threads)
 *         Test case:      TC1
 *         Description:    Line breakpoint & step
 *         Steps:          1.Set breakpoint on line 32
 *                         2.Debug Main
 *                           X. Stops on line 32
 *                         3.Run | Step over
 *                           X. Steps to line 33
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger sets breakpoint at
 *     the 69th line (method "foo").
 *     After the breakpoint is reached, debugger creates "step over" request
 *     and resumes debugee. For every hit event debugger checks line number
 *     of one's location. It should be 69th line for Breakpoint or 70th line
 *     for Step.
 *     In case, when at least one event doesn't arrive during waittime
 *     interval or line number of event is wrong, test fails.
 * COMMENTS:
 *     The testcase intermittently hangs under RightCross. It looks like a
 *     JBuilder bug.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.multithrd.tc01x001
 *        nsk.jdi.BScenarios.multithrd.tc01x001a
 * @run main/othervm
 *      nsk.jdi.BScenarios.multithrd.tc01x001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

