/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.security.AccessControlException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/* @test
 * @build FilterWithSecurityManagerTest SerialFilterTest
 * @run testng/othervm FilterWithSecurityManagerTest
 * @run testng/othervm/policy=security.policy.without.globalFilter
 *          -Djava.security.manager=default FilterWithSecurityManagerTest
 * @run testng/othervm/policy=security.policy
 *          -Djava.security.manager=default
 *          -Djdk.serialFilter=java.lang.Integer FilterWithSecurityManagerTest
 *
 * @summary Test that setting specific filter is checked by security manager,
 *          setting process-wide filter is checked by security manager.
 */

@Test
public class FilterWithSecurityManagerTest {

    byte[] bytes;
    boolean setSecurityManager;
    ObjectInputFilter filter;

    @BeforeClass
    @SuppressWarnings("removal")
    public void setup() throws Exception {
        setSecurityManager = System.getSecurityManager() != null;
        Object toDeserialized = Long.MAX_VALUE;
        bytes = SerialFilterTest.writeObjects(toDeserialized);
        filter = ObjectInputFilter.Config.createFilter("java.lang.Long");
    }

    /**
     * Test that setting process-wide filter is checked by security manager.
     */
    @Test
    @SuppressWarnings("removal")
    public void testGlobalFilter() {
        ObjectInputFilter global = ObjectInputFilter.Config.getSerialFilter();

        try  {
            ObjectInputFilter.Config.setSerialFilter(filter);
            assertFalse(setSecurityManager,
                    "When SecurityManager exists, without "
                    + "java.io.SerializablePermission(serialFilter) "
                    + "IllegalStateException should be thrown");
        } catch (AccessControlException ex) {
            assertTrue(setSecurityManager);
            assertTrue(ex.getMessage().contains("java.io.SerializablePermission"));
            assertTrue(ex.getMessage().contains("serialFilter"));
        } catch (IllegalStateException ise) {
            // ISE should occur only if global filter already set
            Assert.assertNotNull(global, "Global filter should be non-null");
        }
    }

    /**
     * Test that setting specific filter is checked by security manager.
     */
    @Test(dependsOnMethods = { "testGlobalFilter" })
    @SuppressWarnings("removal")
    public void testSpecificFilter() throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(filter);
            Object o = ois.readObject();
        } catch (AccessControlException ex) {
            assertTrue(setSecurityManager);
            assertTrue(ex.getMessage().contains("java.io.SerializablePermission"));
            assertTrue(ex.getMessage().contains("serialFilter"));
        }
    }
}
