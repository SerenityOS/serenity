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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/redefineClasses/redefineclasses008.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test against the method com.sun.jdi.VirtualMachine.redefineClasses()
 *     and checks up the following assertion:
 *        "If canUnrestrictedlyRedefineClasses() is false, attempting to
 *         change method modifiers will throw UnsupportedOperationException
 *         exception"
 *     The test consists of the following files:
 *         redefineclasses008.java             - debugger
 *         redefineclasses008a.java            - initial debuggee
 *         newclassXX/redefineclasses008a.java - redefining debuggees
 *     Initial debugee-class has three dummy methods with different
 *     kind of accessibility: public, protected and private (see COMMENTS)
 *     This test performs the following cases:
 *       1. newclass01 - adding <static> modifiers to public method
 *       2. newclass02 - adding <static> modifiers to 3 methods
 *       3. newclass03 - adding <final> modifiers to public method
 *       4. newclass04 - adding <final> modifiers to 3 methods
 *       5. newclass05 - adding <synchronized> modifiers to public method
 *       6. newclass06 - adding <synchronized> modifiers to 3 methods
 *     The test checks two different cases for suspended debugee and not
 *     suspended one.
 * COMMENTS:
 *     This test looks like redefinclasses07  and differs from redefinclasses07
 *     that default access (package private) is excluded from examintation.
 *     There are no differs except the one. It was observed, count and
 *     accessibility of the methods affect the test behaviour.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.redefineClasses.redefineclasses008
 *        nsk.jdi.VirtualMachine.redefineClasses.redefineclasses008a
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass01 newclass02 newclass03 newclass04 newclass05 newclass06
 *
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.redefineClasses.redefineclasses008
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */
