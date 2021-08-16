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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/hotswap/tc05x001.
 * VM Testbase keywords: [jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 3 - Hot Swap
 *         Test case:      TC5
 *         Description:    After point of execution, same method - stepping
 *         Steps:          1.Set breakpoint at line 24 (call to b()
 *                            from a())
 *                         2.Debug Main
 *                         3.Insert as next line after point of
 *                            execution: System.err.println("foo");
 *                         4.Smart Swap
 *                         5.F7 to step into
 *                            X. Steps into method b()
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger sets breakpoint at
 *     the 38th line (method method_A).
 *     After the breakpoint is reached, debugger redefines debugee adding
 *     a new line into method_A, creates StepRequest and resumes debugee.
 *     When the location of the current StepEvent is in method_B, created
 *     StepRequest is disabled.
 *     Working of debugger and debugee is synchronized via IOPipe channel.
 * COMMENTS:
 *     The scenario mentions debugee crashes on 1.4.0-b91.
 *     The test differs from tc05x002 by that it works via IOPipe
 *     channel and the crash is reproduced.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.hotswap.tc05x001
 *        nsk.jdi.BScenarios.hotswap.tc05x001a
 *
 * @comment make sure tc05x001a is compiled with full debug info
 * @clean nsk.jdi.BScenarios.hotswap.tc05x001a
 * @compile -g:lines,source,vars ../tc05x001a.java
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.BScenarios.hotswap.tc05x001
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

