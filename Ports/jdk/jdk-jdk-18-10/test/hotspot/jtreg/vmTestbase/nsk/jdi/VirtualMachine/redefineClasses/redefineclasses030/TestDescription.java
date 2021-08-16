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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/redefineClasses/redefineclasses030.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test against the method com.sun.jdi.VirtualMachine.redefineClasses()
 *     and checks up the following assertion:
 *         "If a "verifier" detects that a class, though well formed, contains
 *         an internal inconsistency or security problem, VerifyError will be
 *         thrown."
 *     The test consists of the following files:
 *       redefineclasses030.java             - debugger
 *       redefineclasses030a.java            - debuggee
 *       redefineclasses030b.java            - an initial redefined class
 *       newclassXX/redefineclasses030b.jasm - redefining debuggee's class
 *     This test performs the following cases:
 *      1. newclass01 - reducing stack size
 *      2. newclass02 - pushing a local variable with wrong index
 *     The test checks two different cases for suspended debugee and not
 *     suspended one.
 *     When canRedefineClasses() is false, the test is considered as passed
 *     and completes it's execution.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment compile newclassXX/*.jasm to bin/newclassXX and make sure the classes
 *          aren't in test.class.path
 * @compile newclass01/redefineclasses030b.jasm
 * @run driver
 *      nsk.jdi.ExtraClassesInstaller
 *      newclass01 nsk/jdi/VirtualMachine/redefineClasses/redefineclasses030b.class
 * @clean nsk.jdi.VirtualMachine.redefineClasses.redefineclasses030b
 * @compile newclass02/redefineclasses030b.jasm
 * @run driver
 *      nsk.jdi.ExtraClassesInstaller
 *      newclass02 nsk/jdi/VirtualMachine/redefineClasses/redefineclasses030b.class
 * @clean nsk.jdi.VirtualMachine.redefineClasses.redefineclasses030b
 *
 * @build nsk.jdi.VirtualMachine.redefineClasses.redefineclasses030
 *        nsk.jdi.VirtualMachine.redefineClasses.redefineclasses030a
 *
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.redefineClasses.redefineclasses030
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

