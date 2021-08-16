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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/hotswap/tc10x002.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 3 - Hot Swap
 *         Test case:      TC10
 *         Description:    Changing data type
 *         Steps:          1.Set breakpoint on line 20
 *                         2.Debug Main
 *                            X. Stops on line 20
 *                         3.Change line 16 to boolean i = true;
 *                         4.Smart Swap
 *                         5.Resume
 *                            X. Prints "true" as last line of output
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger sets breakpoint at
 *     the 40th line (method runIt).
 *     Note: the runIt method was added into the test, to allow invoking
 *     ThreadReference.popFrame(). Current frame can not be reset because of
 *     there is no previous frame.
 *     After the breakpoint is reached, debugger redefines debugee changing
 *     type of local variable, pops current frame, sets a new breakpoint at
 *     37th line and resumes debugee.
 *     When the second breakpoint hits, debugger requests value of the local
 *     variable "i" and compares got value with expected value.
 *     In this case expected value is the new value after redefinition.
 * COMMENTS:
 *     Step 5 of Borland's scenario is wrong due to redefineClasses spec says:
 *     "The redefined method will be used on new invokes. If resetting these
 *     frames is desired, use ThreadReference.popFrames() with
 *     Method.isObsolete()."
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *     Test updated to wait for debugee VM exit:
 *     - standard method Debugee.endDebugee() is used instead of final Debugee.resume()
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.hotswap.tc10x002
 *        nsk.jdi.BScenarios.hotswap.tc10x002a
 *
 * @comment make sure tc10x002a is compiled with full debug info
 * @clean nsk.jdi.BScenarios.hotswap.tc10x002a
 * @compile -g:lines,source,vars ../tc10x002a.java
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.BScenarios.hotswap.tc10x002
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

