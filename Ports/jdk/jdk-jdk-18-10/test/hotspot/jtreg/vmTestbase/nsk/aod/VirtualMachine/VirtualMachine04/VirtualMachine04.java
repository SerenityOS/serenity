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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine04.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     Test checks following methods:
 *         - VirtualMachine.getSystemProperties() (test checks that returned properties contains
 *         expected property and tested method returns properties whose key and value is a String)
 *         - VirtualMachine.getAgentProperties() (test checks that method returns properties whose
 *         key and value is a String)
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.aod.VirtualMachine.VirtualMachine04.VM04Target
 * @run main/othervm
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine04.VirtualMachine04
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.aod.VirtualMachine.VirtualMachine04.VM04Target
 */

package nsk.aod.VirtualMachine.VirtualMachine04;

import com.sun.tools.attach.VirtualMachine;
import nsk.share.TestBug;
import nsk.share.aod.AODTestRunner;
import nsk.share.test.TestUtils;

import java.util.Properties;

/*
 * Test checks following methods:
 *      - VirtualMachine.getSystemProperties()
 *      - VirtualMachine.getAgentProperties()
 */
public class VirtualMachine04 extends AODTestRunner {
    static final String SIGNAL_CHANGE_PROPERTY = "change_property";
    static final String SIGNAL_PROPERTY_CHANGED = "property_changed";

    public VirtualMachine04(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        VirtualMachine vm = VirtualMachine.attach(targetVMId);

        try {
            checkSystemProperties(vm, VM04Target.TEST_PROPERTY_KEY, VM04Target.TEST_PROPERTY_VALUE);

            log.display("Sending signal " + SIGNAL_CHANGE_PROPERTY);
            pipe.println(SIGNAL_CHANGE_PROPERTY);
            String signal = pipe.readln();
            log.display("Received signal " + signal);
            if (!signal.equals(SIGNAL_PROPERTY_CHANGED)) {
                throw new TestBug("Unexpected signal received: " + signal);
            }
            checkSystemProperties(vm, VM04Target.TEST_PROPERTY_KEY, VM04Target.CHANGED_TEST_PROPERTY_VALUE);

            Properties properties = vm.getAgentProperties();
            System.out.println("VirtualMachine.getAgentProperties(): " + properties);
            checkProperties(properties);
        } finally {
            vm.detach();
        }
    }

    void checkSystemProperties(VirtualMachine vm,
                               String propertyToCheck,
                               String expectedPropertyValue) throws Throwable {

        Properties properties = vm.getSystemProperties();
        System.out.println("VirtualMachine.getSystemProperties(): " + properties);
        checkProperties(properties);

        String checkedPropertyValue = properties.getProperty(propertyToCheck);
        TestUtils.assertNotNull(checkedPropertyValue, "Properties doesn't contain property '" + propertyToCheck + "'");
        TestUtils.assertEquals(checkedPropertyValue, expectedPropertyValue,
                "Unexpected value of the property '" + propertyToCheck + "': " + checkedPropertyValue + ", expected value is '" + expectedPropertyValue + "'");
    }

    /*
     * Check following spec clause: VirtualMachine.getSystemProperties() and
     * VirtualMachine.getAgentProperties() return the properties whose key and value is a String
     */
    void checkProperties(Properties properties) {
        TestUtils.assertNotNull(properties, "Method returns null Properties");

        for (Object key : properties.keySet()) {
            Object value = properties.get(key);
            log.display("Value of '" + key + "' = " + value);

            TestUtils.assertTrue(key instanceof String, "Property key isn't String: " + key.getClass().getName());
            TestUtils.assertTrue(value instanceof String, "Property value isn't String: " + value.getClass().getName());
        }
    }

    public static void main(String[] args) {
        new VirtualMachine04(args).runTest();
    }
}
