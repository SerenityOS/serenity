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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/methods/methods006.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  A test for methods() method of ReferenceType interface.
 *  The test checks if the method returns methods declared in
 *  mirrored enum type, methods automatically generated
 *  by javac for enums, i.e. values, valueOf and default
 *  constructor.
 *  The test consists of a debugger program (methods006.java)
 *  and debuggee application (methods006a.java).
 *  Package name is nsk.jdi.ReferenceType.methods .
 *  The test works as follows.
 *  The debugger uses nsk.jdi.share framework classes to
 *  establish connection with debuggee. The debugger and debuggee
 *  synchronize with each other using special commands over
 *  communication channel provided by framework classes.
 *  Upon receiving the signal of readiness from debuggee,
 *  the debugger calls methods() method for each field
 *  of enum type declared in methods006a class. The result list
 *  list must contain all expected methods.
 * COMMENTS:
 *  5029502 TEST_BUG: jdi tests against enum should not use abstract
 *          modifier
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.methods.methods006
 *        nsk.jdi.ReferenceType.methods.methods006a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.methods.methods006
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

