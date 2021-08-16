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
 * @summary converted from VM Testbase nsk/aod/AttachProvider/AttachProvider02.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test tries to attach to the VM started by this test using 2 methods:
 *         - AttachProvider.attachVirtualMachine(String id)
 *         - AttachProvider.attachVirtualMachine(VirtualMachineDescriptor vmd)
 *     After attaching test tries to use created VirtualMachine object (tries to call VirtualMachine.getSystemProperties()).
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -Djdk.attach.allowAttachSelf
 *      -XX:+UsePerfData
 *      nsk.aod.AttachProvider.AttachProvider02.AttachProvider02
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.AttachProvider.AttachProvider02;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

import java.util.Properties;

/*
 * Test checks following methods:
 *      - AttachProvider.attachVirtualMachine(String id)
 *      - AttachProvider.attachVirtualMachine(VirtualMachineDescriptor vmd)
 */
public class AttachProvider02 extends AODTestRunner {

    public AttachProvider02(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        TestUtils.assertTrue(AttachProvider.providers().size() > 0, "Method AttachProvider.providers() returns empty collection");

        String currentVMId = getCurrentVMId();

        for (AttachProvider provider : AttachProvider.providers()) {
            log.display("Provider: " + provider);
            log.display("Provider.name(): " + provider.name());
            log.display("Provider.type(): " + provider.type());

            TestUtils.assertNotNull(provider.name(), "Provider.name() returns null");
            TestUtils.assertNotNull(provider.type(), "Provider.type() returns null");

            tryAttach(provider, currentVMId, false);
            tryAttach(provider, currentVMId, true);

            tryAttach(provider, targetVMId, false);
            tryAttach(provider, targetVMId, true);
        }
    }

    void tryAttach(AttachProvider provider, String vmId, boolean useVMDescriptor) throws Throwable {
        log.display("Attaching to vm " + vmId + " using " +
                (useVMDescriptor ? "VirtualMachineDescriptor " : "VM id"));

        VirtualMachine vm;

        if (useVMDescriptor) {
            vm = provider.attachVirtualMachine(new VirtualMachineDescriptor(provider, vmId));
        } else {
            vm = provider.attachVirtualMachine(vmId);
        }

        try {
            log.display("Attached to vm: " + vm);
            TestUtils.assertEquals(vm.id(), vmId, "VirtualMachine.id() returns unexpected value for attached vm: " + vm.id());

            // try to use created VirtualMachine
            log.display("Trying to call VirtualMachine.getSystemProperties()");
            Properties properties = vm.getSystemProperties();
            TestUtils.assertNotNull(properties, "VirtualMachine.getSystemProperties() returns null");

            TestUtils.assertTrue(properties.size() > 0, "VirtualMachine.getSystemProperties() returns empty collection");
        } finally {
            vm.detach();
        }
    }

    public static void main(String[] args) {
        new AttachProvider02(args).runTest();
    }
}
