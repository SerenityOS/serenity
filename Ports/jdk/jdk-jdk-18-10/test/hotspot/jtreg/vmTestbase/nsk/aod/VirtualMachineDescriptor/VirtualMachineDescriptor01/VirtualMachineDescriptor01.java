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
 * @summary converted from VM Testbase nsk/aod/VirtualMachineDescriptor/VirtualMachineDescriptor01.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This is sanity test for class VirtualMachineDescriptor.
 *     Test checks that its methods toString(), displayName(), id(), provider() return
 *     non-null values and also test checks semantics of the method VirtualMachineDescriptor.equals(Object obj).
 *     Tested VirtualMachineDescriptors are obtained using VirtualMachine.list()
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachineDescriptor.VirtualMachineDescriptor01.VirtualMachineDescriptor01
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.VirtualMachineDescriptor.VirtualMachineDescriptor01;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

/*
 * Basic sanity checks for class com.sun.tools.attach.VirtualMachineDescriptor
 */
public class VirtualMachineDescriptor01 extends AODTestRunner {

    public VirtualMachineDescriptor01(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        String currentVMId = getCurrentVMId();

        VirtualMachineDescriptor currentVMDesc = null;
        VirtualMachineDescriptor targetVMDesc = null;

        for (VirtualMachineDescriptor vmDescriptor : VirtualMachine.list()) {
            log.display("VirtualMachineDescriptor: " + vmDescriptor);
            log.display("VirtualMachineDescriptor.displayName(): " + vmDescriptor.displayName());
            log.display("VirtualMachineDescriptor.id(): " + vmDescriptor.id());
            log.display("VirtualMachineDescriptor.provider(): " + vmDescriptor.provider());

            TestUtils.assertNotNull(vmDescriptor.toString(), "VirtualMachineDescriptor.toString() returns null");
            TestUtils.assertNotNull(vmDescriptor.displayName(), "VirtualMachineDescriptor.displayName() returns null");
            TestUtils.assertNotNull(vmDescriptor.id(), "VirtualMachineDescriptor.id() returns null");
            TestUtils.assertNotNull(vmDescriptor.provider(), "VirtualMachineDescriptor.provider() returns null");

            TestUtils.assertTrue(AttachProvider.providers().contains(vmDescriptor.provider()),
                    "AttachProvider.providers() doesn't contain provider '" + vmDescriptor.provider() + "'");

            if (vmDescriptor.id().equals(currentVMId)) {
                currentVMDesc = vmDescriptor;
            } else if (vmDescriptor.id().equals(targetVMId)) {
                targetVMDesc = vmDescriptor;
            }
        }

        TestUtils.assertNotNull(currentVMDesc, "VirtualMachine.list() didn't return descriptor for the current VM");
        TestUtils.assertNotNull(targetVMDesc, "VirtualMachine.list() didn't return descriptor for VM with id '" + targetVMId + "'");

        TestUtils.assertTrue(!currentVMDesc.equals(targetVMDesc),
                "VirtualMachineDescriptor.equals() returns 'true' for '" + currentVMDesc + "' and '" + targetVMDesc + "'");
        TestUtils.assertTrue(currentVMDesc.hashCode() != targetVMDesc.hashCode(),
                "VirtualMachineDescriptor.hashCode() returns the same value (" + currentVMDesc.hashCode() + ")" + " for '"
                + currentVMDesc + "' and '" + targetVMDesc + "'");

        VirtualMachine targetVM = VirtualMachine.attach(targetVMDesc);

        try {
            // create another VirtualMachineDescriptor for target VM
            VirtualMachineDescriptor targetVMDesc2 = new VirtualMachineDescriptor(targetVM.provider(), targetVM.id());

            TestUtils.assertEquals(targetVMDesc, targetVMDesc2,
                    "VirtualMachineDescriptor.equals() returns 'false' for '" + targetVMDesc + "' and '" + targetVMDesc2 + "'");

            TestUtils.assertEquals(targetVMDesc.hashCode(), targetVMDesc2.hashCode(),
                    "VirtualMachineDescriptor.hashCode() returns different values for '" + targetVMDesc + "' and '" + targetVMDesc2 + "'");
        } finally {
            targetVM.detach();
        }
    }

    public static void main(String[] args) {
        new VirtualMachineDescriptor01(args).runTest();
    }
}
