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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/hotswap/tc09x002.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 3 - Hot Swap
 *         Test case:      TC9
 *         Description:    Breakpoints updated correctly
 *         Steps:          1.Set breakpoint at lines 36 and 39
 *                            (printing 1 and 4)
 *                         2.Debug Main
 *                            X. Stops on line 36
 *                         3.Delete line 37
 *                         4.Smart Swap
 *                            X. Breakpoints still set and valid at 36
 *                            and 38
 *                         5.Resume
 *                            X. Stops on line 38
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger sets breakpoints at
 *     the 47th and 49th line (method method_C).
 *     After the first breakpoint is reached, debugger redefines debugee
 *     deleting 48th line and attempts to set another breakpoint at 49th line.
 *     Because line number info is not available after redefining, it is
 *     expected IndexOutOfBoundsException will be thrown.
 * COMMENTS:
 *     Last step from scenarion description (stopping on line 38) is wrong
 *     of because JVMDI spec (Redefine Classes ) says:"...All breakpoints
 *     in the class are cleared."
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *     Test updated to wait for debugee VM exit:
 *     - standard method Debugee.endDebugee() is used instead of final Debugee.resume()
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.hotswap.tc09x002
 *        nsk.jdi.BScenarios.hotswap.tc09x002a
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.BScenarios.hotswap.tc09x002
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

