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
 * @summary converted from VM Testbase nsk/jdi/ClassType/invokeMethod/invokemethod009.
 * VM Testbase keywords: [jpda, jdi, quarantine]
 * VM Testbase comments: 4698670
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method
 *         com.sun.jdi.ClassType.invokeMethod()
 *     properly throws ClassNotLoadedException - if any argument type
 *     has not yet been loaded through the appropriate class loader.
 *     The test works as follows. Debugger part of the test invokes
 *     debuggee methods "dummyMeth", "finDummyMeth" with arguments of
 *     non-loaded reference types "DummyType", "FinDummyType".
 *     The test makes sure that class has not been loaded by the debuggee
 *     VM through the JDI method VirtualMachine.classesByName() which
 *     should return list of loaded classes only.
 * COMMENTS
 *
 * @modules jdk.jdi/com.sun.tools.jdi:open
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassType.invokeMethod.invokemethod009
 *        nsk.jdi.ClassType.invokeMethod.invokemethod009t
 * @run main/othervm
 *      nsk.jdi.ClassType.invokeMethod.invokemethod009
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

