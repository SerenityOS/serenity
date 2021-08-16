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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/instanceCounts/instancecounts002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 * This scenario in particular cover the situation described in CR 6376715.
 *      The test scenario is following:
 *      - the debugger gets the debuggee running on another JavaVM and
 *        establishes a pipe with the debuggee program
 *      - upon receiving corresponding command from the debugger process the debuggee
 *        loads set of classes and creates the number of class instances
 *      - the debugger process check that instanceCounts returns correct number
 *      - the debuggee process delete previously created objects
 *        (make them unreachable)
 *      - the debugger process checks that instanceCounts returns 0 and no
 *        com.sun.jdi.ObjectCollectedException is thrown
 * Test execute class nsk.jdi.VirtualMachine.instanceCounts.instancecounts001.instancecounts001 with parameter -forceGC
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.instanceCounts.instancecounts001.instancecounts001
 *        nsk.share.jdi.TestClass1
 *        nsk.share.jdi.TestClass2
 *        nsk.share.jdi.TestInterfaceImplementer1
 * @run main/othervm/native
 *      nsk.jdi.VirtualMachine.instanceCounts.instancecounts001.instancecounts001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 *      -forceGC
 */

