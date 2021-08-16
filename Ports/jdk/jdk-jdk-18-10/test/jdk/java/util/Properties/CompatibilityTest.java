/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.testng.Assert;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8252354
 * @run testng CompatibilityTest
 * @summary Verify compatibility.
 */
public class CompatibilityTest {
    @DataProvider(name = "entries")
    public Object[][] getEntries() throws IOException {
        return new Object[][]{
            {8, 238923},
            {1.1, 1.1},
            {new Object(), "Value"},
            {"Key", new Object()},
        };
    }

    /**
     * Verifies that a ClassCastException is thrown as specified by the
     * {@code storeToXML} method.
     * @param key the key
     * @param value the value
     * @throws IOException
     */
    @Test(dataProvider = "entries")
    void testThrows(Object key, Object value) throws IOException {
        Assert.assertThrows(ClassCastException.class, () -> storeToXML(key, value));
    }

    void storeToXML(Object key, Object value) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Properties pr = new Properties();
        pr.put(key, value);
        pr.storeToXML(os, "Test", "UTF-8");
    }
}
