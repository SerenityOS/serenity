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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/mirrorOf_char/mirrorof_char001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     VirtualMachine.
 *     The test checks up that a result of the method
 *     com.sun.jdi.VirtualMachine.mirrorOf(char value)
 *     complies with the following requirements for CharValue:
 *     public char value()
 *     Returns this CharValue as a char.
 *     Returns: the char mirrored by this object.
 *     public boolean equals(java.lang.Object obj)
 *     Compares the specified Object with this CharValue for equality.
 *     Returns: true if the Object is a CharValue and if applying "=="
 *     to the two mirrored primitives would evaluate to true;
 *     false otherwise.
 *     public int hashCode()
 *     Returns the hash code value for this CharValue.
 *     Returns: the integer hash code
 *     The test works as follows:
 *     A debugger program - nsk.jdi.VirtualMachine.mirrorOf_char.mirrorof_char001;
 *     a debuggee program - nsk.jdi.VirtualMachine.mirrorOf_char.mirrorof_char001a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM,
 *     establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies
 *     via the pipe. Upon getting reply,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and compares the data got to the data expected.
 *     In case of mismatch the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.mirrorOf_char.mirrorof_char001
 *        nsk.jdi.VirtualMachine.mirrorOf_char.mirrorof_char001a
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.mirrorOf_char.mirrorof_char001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

