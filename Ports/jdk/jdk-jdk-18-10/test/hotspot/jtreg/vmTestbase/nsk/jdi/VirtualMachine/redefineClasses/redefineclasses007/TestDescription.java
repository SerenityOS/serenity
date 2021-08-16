/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/redefineClasses/redefineclasses007.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test against the method com.sun.jdi.VirtualMachine.redefineClasses()
 *     and checks up the following assertion:
 *        "If canUnrestrictedlyRedefineClasses() is false, attempting to
 *         change method modifiers will throw UnsupportedOperationException
 *         exception"
 *     The test consists of the following files:
 *         redefineclasses007.java             - debugger
 *         redefineclasses007a.java            - initial debuggee
 *         newclassXX/redefineclasses007a.java - redefining debuggees
 *     Initial debugee-class has four dummy methods with different
 *     kind of accessibility: public, protected, private and
 *     default access (package private).
 *     This test performs the following cases:
 *       1. newclass01 - adding <static> modifiers to public method
 *       2. newclass02 - adding <static> modifiers to 4 methods
 *       3. newclass03 - adding <final> modifiers to public method
 *       4. newclass04 - adding <final> modifiers to 4 methods
 *       5. newclass05 - adding <synchronized> modifiers to public method
 *       6. newclass06 - adding <synchronized> modifiers to 4 methods
 *     The test checks two different cases for suspended debugee and not
 *     suspended one.
 * COMMENTS:
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.redefineClasses.redefineclasses007
 *        nsk.jdi.VirtualMachine.redefineClasses.redefineclasses007a
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass01 newclass02 newclass03 newclass04 newclass05 newclass06
 *
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.redefineClasses.redefineclasses007
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */
