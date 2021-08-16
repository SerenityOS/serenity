/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/aod/AttachProvider/AttachProvider01.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test checks that method AttachProvider.listVirtualMachines() returns
 *     VirtualMachineDescriptors for 2 VMs started by this test.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -XX:+UsePerfData
 *      nsk.aod.AttachProvider.AttachProvider01.AttachProvider01
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.AttachProvider.AttachProvider01;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

import java.util.List;

/*
 * Test checks method AttachProvider.listVirtualMachines()
 * (test checks that collection returned by AttachProvider.listVirtualMachines() contains current VM
 * and another VM started by this test)
 */
public class AttachProvider01 extends AODTestRunner {

    public AttachProvider01(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) {
        String currentVMId = getCurrentVMId();

        for (AttachProvider provider : AttachProvider.providers()) {
            log.display("Checking AttachProvider.listVirtualMachines() (provider: " + provider + ")");
            checkList(provider.listVirtualMachines(), currentVMId, targetVMId);
        }
    }

    private void checkList(List<VirtualMachineDescriptor> vmDescriptors, String currentVMId, String targetVMId) {
        VirtualMachineDescriptor currentVMDesc = null;
        VirtualMachineDescriptor targetVMDesc = null;

        for (VirtualMachineDescriptor vmDescriptor : VirtualMachine.list()) {
            log.display("VirtualMachineDescriptor: " + vmDescriptor);

            if (vmDescriptor.id().equals(currentVMId)) {
                currentVMDesc = vmDescriptor;
            } else if (vmDescriptor.id().equals(targetVMId)) {
                targetVMDesc = vmDescriptor;
            }
        }

        TestUtils.assertNotNull(currentVMDesc, "VirtualMachine.list() didn't return descriptor for the current VM");
        TestUtils.assertNotNull(targetVMDesc, "VirtualMachine.list() didn't return descriptor for VM with id '" +
                targetVMId + "'");
    }

    public static void main(String[] args) {
        new AttachProvider01(args).runTest();
    }
}
