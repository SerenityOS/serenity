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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine03.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     Test checks semantics of the method VirtualMachine.equals(Object obj).
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.DummyTargetApplication
 * @run main/othervm
 *      -Djdk.attach.allowAttachSelf
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine03.VirtualMachine03
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.DummyTargetApplication
 */

package nsk.aod.VirtualMachine.VirtualMachine03;

import com.sun.tools.attach.VirtualMachine;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

/*
 * Test checks method VirtualMachine.equals(Object)
 */
public class VirtualMachine03 extends AODTestRunner {

    public VirtualMachine03(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        String currentVMId = getCurrentVMId();

        VirtualMachine vm1 = VirtualMachine.attach(currentVMId);
        try {
            VirtualMachine vm2 = VirtualMachine.attach(targetVMId);
            try {
                TestUtils.assertEquals(vm1.id(), currentVMId, "vm.id() returns unexpected value: " + vm1.id());
                TestUtils.assertEquals(vm2.id(), targetVMId, "vm.id() returns unexpected value: " + vm2.id());

                TestUtils.assertTrue(!vm1.equals(vm2), vm1 + ".equals(" + vm2 + ") returns 'true'");

                checkVM(vm1);
                checkVM(vm2);
            } finally {
                vm2.detach();
            }
        } finally {
            vm1.detach();
        }
    }

    void checkVM(VirtualMachine vm1) throws Throwable {
        TestUtils.assertEquals(vm1, vm1, "vm.equals(itself) returns 'false'");

        // create one more VirtualMachine object for the same VM
        VirtualMachine vm2 = VirtualMachine.attach(vm1.id());
        try {
            TestUtils.assertEquals(vm1, vm2, vm1 + ".equals(" + vm2 + ") returns 'false'");
            TestUtils.assertTrue(vm1.hashCode() == vm2.hashCode(), "vm.hashCode() returns different values for " + vm1 + " and " + vm2);
            TestUtils.assertEquals(vm1.provider(), vm2.provider(), "vm.provider() returns non-equals objects for " + vm1 + " and " + vm2);
        } finally {
            vm2.detach();
        }

        TestUtils.assertTrue(!vm1.equals(""), "vm.equals(String) returns 'true'");

        TestUtils.assertTrue(!vm1.equals(null), "vm.equals(null) returns 'true'");
    }

    public static void main(String[] args) {
        new VirtualMachine03(args).runTest();
    }
}
