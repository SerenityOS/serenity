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
 * @summary converted from VM Testbase nsk/jdi/ClassType/setValue/setvalue006.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method:
 *         com.sun.jdi.ClassType.setValue()
 *     does not throw ClassNotLoadedException - when a debugger part of
 *     the test attempts to set null value to the debuggee field which
 *     type has not yet been loaded through the appropriate class loader.
 *     The test works as follows. The debuggee part has two static
 *     fields: "dummySFld" of non-loaded type "DummyType" and
 *     "finDummySFld" of non-loaded type "FinDummyType".
 *     Debugger part tries to provoke the exception by setting null values
 *     to these fields. The test makes sure that appropriate class has not
 *     been loaded by the debuggee VM through the JDI method
 *     VirtualMachine.classesByName() which should return matching list
 *     of loaded classes only.
 * COMMENTS
 *     The test was fixed due to the following bug:
 *     4735268 TEST_BUG: some jdi tests expect ClassNotLoadedException for
 *             null values
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassType.setValue.setvalue006
 *        nsk.jdi.ClassType.setValue.setvalue006t
 * @run main/othervm
 *      nsk.jdi.ClassType.setValue.setvalue006
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

