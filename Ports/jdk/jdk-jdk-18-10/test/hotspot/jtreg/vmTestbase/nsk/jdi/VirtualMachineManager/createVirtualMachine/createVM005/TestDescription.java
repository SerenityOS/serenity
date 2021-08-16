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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM005.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM005 test:
 *     The test for the
 *     virtualMachineManager.createVirtualMachine(Connection) method.
 *     The test checks up that createVirtualMachine(Connection) method
 *     creates the VirtualMachine properly.
 *     After the VirtualMachine is created the test checks that:
 *       - VirtualMachine.process() method returns the null as
 *         the specification says about it.
 *       - VMStartEvent should be received for specified timeout;
 *       - after VirtualMachine.resume() the Process should finish
 *         with expected exit status;
 * COMMENTS:
 *     Test fixed according to test RFE:
 *     4943437 TEST_RFE: createVM002 does not honor DEBUGEE_VM_* setting
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachineManager.createVirtualMachine.createVM005
 *        nsk.jdi.VirtualMachineManager.createVirtualMachine.CreateVM005_TargetVM
 * @run main/othervm
 *      nsk.jdi.VirtualMachineManager.createVirtualMachine.createVM005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

