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
 * @summary converted from VM Testbase nsk/jdi/Accessible/modifiers/modifiers002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  A test for isEnum() method of Accessible interface.
 *  The test checks if the method returns int status combined
 *  as from following flags for any mirrored enum type:
 *    - ACC_FINAL (0x0010) for any enum type because each enum types
 *      is final,
 *    - ACC_SUPER (0x0020 for any enum type due to
 *      JVMS 4.1 The ClassFile Structure
 *    - ACC_PUBLIC (0x0001) if public modifier is declared,
 *    - ACC_ABSTRACT (0x0400) if abstract modifier is declared.
 *  The test consists of a debugger program (modifiers002.java)
 *  and debuggee application (modifiers002a.java).
 *  Package name is nsk.jdi.Accessible.modifiers .
 *  The test works as follows.
 *  The debugger uses nsk.jdi.share framework classes to
 *  establish connection with debuggee. The debugger and debuggee
 *  synchronize with each other using special commands over
 *  communication channel provided by framework classes.
 *  Upon receiving the signal of readiness from debuggee,
 *  the debugger calls modifiers() method for each field
 *  of enum type declared in modifiers002a class.
 *  The result is bitwise compared with expected flags.
 * COMMENTS:
 *  5029502 TEST_BUG: jdi tests against enum should not use abstract
 *          modifier
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Accessible.modifiers.modifiers002
 *        nsk.jdi.Accessible.modifiers.modifiers002a
 * @run main/othervm
 *      nsk.jdi.Accessible.modifiers.modifiers002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

