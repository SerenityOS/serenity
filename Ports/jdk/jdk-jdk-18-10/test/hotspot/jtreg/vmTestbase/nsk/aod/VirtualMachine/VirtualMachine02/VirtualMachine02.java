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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine02.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     Test checks following methods:
 *         - VirtualMachine.attach(String id) (test tries to attach to the VM running test
 *         and to the another VM started by this test)
 *         - VirtualMachine.attach(VirtualMachineDescriptor vmd) (test tries to attach to the VM running test
 *         and to the another VM started by this test)
 *         -  VirtualMachine.detach() (test checks that after detaching operations on VirtualMachine throw IOException)
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -Djdk.attach.allowAttachSelf
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine02.VirtualMachine02
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.VirtualMachine.VirtualMachine02;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

import java.io.IOException;

/*
 * Test checks following methods:
 *      -  VirtualMachine.attach(String) (test tries to attach to current and to another VM)
 *      -  VirtualMachine.attach(VirtualMachineDescriptor) (test tries to attach to current and to another VM)
 *      -  VirtualMachine.detach() (test checks that after detaching operations on VirtualMachine
 *      throw IOException)
 */
public class VirtualMachine02 extends AODTestRunner {

    public VirtualMachine02(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        log.display("Executing test for current VM");
        String currentVMId = getCurrentVMId();
        doTest(currentVMId);
        log.display("");

        log.display("Executing test for another VM (id = " + targetVMId + ")");
        doTest(targetVMId);
    }

    void doTest(String targetVMId) throws Throwable {
        VirtualMachine vm;

        log.display("Trying to attach using VirtualMachine(\"" + targetVMId + "\")");
        vm = VirtualMachine.attach(targetVMId);
        log.display("Attached: " + vm);
        checkDetach(vm);

        log.display("Trying to attach using VirtualMachine(VirtualMachineDescriptor)");
        AttachProvider provider;
        TestUtils.assertTrue(AttachProvider.providers().size() > 0, "AttachProvider.providers() returns empty list");
        provider = AttachProvider.providers().get(0);
        log.display("Create VirtualMachineDescriptor using provider '" + provider + "'");
        VirtualMachineDescriptor vmDescriptor = new VirtualMachineDescriptor(provider, targetVMId);
        vm = VirtualMachine.attach(vmDescriptor);
        log.display("Attached: " + vm);
        TestUtils.assertEquals(vm.provider(), provider, "vm.provider() returns unexpected value: " + vm.provider());
        checkDetach(vm);
    }

    void checkDetach(VirtualMachine vm) throws Throwable {
        log.display("Detaching from " + vm);
        vm.detach();

        try {
            vm.getSystemProperties();
            TestUtils.testFailed("Expected IOException wasn't thrown");
        } catch (IOException e) {
            // expected exception
        }
        try {
            vm.getAgentProperties();
            TestUtils.testFailed("Expected IOException wasn't thrown");
        } catch (IOException e) {
            // expected exception
        }
        try {
            vm.loadAgent("agent");
            TestUtils.testFailed("Expected IOException wasn't thrown");
        } catch (IOException e) {
            // expected exception
        }
        try {
            vm.loadAgentLibrary("agent");
            TestUtils.testFailed("Expected IOException wasn't thrown");
        } catch (IOException e) {
            // expected exception
        }
        try {
            vm.loadAgentPath("agent");
            TestUtils.testFailed("Expected IOException wasn't thrown");
        } catch (IOException e) {
            // expected exception
        }

        // shouldn't throw exception
        log.display("Trying to call detach one more time for " + vm);
        vm.detach();
    }

    public static void main(String[] args) {
        new VirtualMachine02(args).runTest();
    }
}
