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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine01.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     Test checks that methods of VirtualMachine throws expected exceptions
 *     in following cases:
 *         - VirtualMachine.attach(null) should throw NullPointerException
 *         - VirtualMachine.attach(<invalid vm id>) should throw AttachNotSupportedException
 *         - VirtualMachine.loadAgent(null), VirtualMachine.loadAgentLibrary(null),
 *         VirtualMachine.loadAgentPath(null) should throw NullPointerException
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine01.VirtualMachine01
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.VirtualMachine.VirtualMachine01;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

/*
 * Test provokes exception which should be thrown by VirtualMachine methods:
 *      - VirtualMachine.attach(null) should throw NullPointerException
 *      - VirtualMachine.attach(<invalid vm id>) should throw AttachNotSupportedException
 *      - VirtualMachine.loadAgent(null), VirtualMachine.loadAgentLibrary(null),
 *      VirtualMachine.loadAgentPath(null) should throw NullPointerException
 */
public class VirtualMachine01 extends AODTestRunner {

    VirtualMachine01(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        try {
            log.display("VirtualMachine.attach((String)null)");
            VirtualMachine.attach((String) null);
            TestUtils.testFailed("NullPointerException wasn't thrown");
        } catch (NullPointerException e) {
            log.display("Expected exception: " + e);
        }

        try {
            log.display("VirtualMachine.attach((VirtualMachineDescriptor)null)");
            VirtualMachine.attach((VirtualMachineDescriptor) null);
            TestUtils.testFailed("NullPointerException wasn't thrown");
        } catch (NullPointerException e) {
            log.display("Expected exception: " + e);
        }

        final String invalidVMId = "InvalidID";

        try {
            log.display("VirtualMachine.attach(" + invalidVMId + ")");
            VirtualMachine.attach(invalidVMId);
            TestUtils.testFailed("AttachNotSupportedException wasn't thrown");
        } catch (AttachNotSupportedException e) {
            log.display("Expected exception: " + e);
        }

        try {
            TestUtils.assertTrue(AttachProvider.providers().size() > 0, "AttachProvider.providers() returns empty list");
            log.display("Create VirtualMachineDescriptor using provider '" + AttachProvider.providers().get(0) + "'");
            VirtualMachineDescriptor vmd = new VirtualMachineDescriptor(AttachProvider.providers().get(0), invalidVMId);
            log.display("VirtualMachine.attach(new VirtualMachineDescriptor(provider, " + invalidVMId + "))");
            VirtualMachine.attach(vmd);
            TestUtils.testFailed("AttachNotSupportedException wasn't thrown");
        } catch (AttachNotSupportedException e) {
            log.display("Expected exception: " + e);
        }

        // create VirtualMachine object VM to check non-static methods

        VirtualMachine vm = VirtualMachine.attach(targetVMId);
        try {
            try {
                log.display("VirtualMachine.loadAgent(null)");
                vm.loadAgent(null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

            try {
                log.display("VirtualMachine.loadAgent(null, null)");
                vm.loadAgent(null, null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

            try {
                log.display("VirtualMachine.loadAgentLibrary(null)");
                vm.loadAgentLibrary(null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

            try {
                log.display("VirtualMachine.loadAgentLibrary(null, null)");
                vm.loadAgentLibrary(null, null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

            try {
                log.display("VirtualMachine.loadAgentPath(null)");
                vm.loadAgentPath(null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

            try {
                log.display("VirtualMachine.loadAgentPath(null, null)");
                vm.loadAgentPath(null, null);
                TestUtils.testFailed("NullPointerException wasn't thrown");
            } catch (NullPointerException e) {
                log.display("Expected exception: " + e);
            }

        } finally {
            vm.detach();
        }
    }

    public static void main(String[] args) {
        new VirtualMachine01(args).runTest();
    }

}
