/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Security;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/* @test
 * @build CheckInputOrderTest SerialFilterTest
 * @run testng/othervm CheckInputOrderTest
 *
 * @summary Test that when both global filter and specific filter are set,
 *          global filter will not affect specific filter.
 */

public class CheckInputOrderTest implements Serializable {
    private static final long serialVersionUID = 12345678901L;

    @DataProvider(name="Patterns")
    Object[][] patterns() {
        return new Object[][] {
                new Object[] { SerialFilterTest.genTestObject("maxarray=1", true), "java.**;java.lang.*;java.lang.Long;maxarray=0", false },
                new Object[] { SerialFilterTest.genTestObject("maxarray=1", true), "java.**;java.lang.*;java.lang.Long", true },
                new Object[] { Long.MAX_VALUE, "java.**;java.lang.*;java.lang.Long;maxdepth=0", false },
                new Object[] { Long.MAX_VALUE, "java.**;java.lang.*;java.lang.Long;maxbytes=0", false },
                new Object[] { Long.MAX_VALUE, "java.**;java.lang.*;java.lang.Long;maxrefs=0", false },

                new Object[] { Long.MAX_VALUE, "java.**;java.lang.*;java.lang.Long", true },

                new Object[] { Long.MAX_VALUE, "!java.**;java.lang.*;java.lang.Long", false },
                new Object[] { Long.MAX_VALUE, "java.**;!java.lang.*;java.lang.Long", true },

                new Object[] { Long.MAX_VALUE, "!java.lang.*;java.**;java.lang.Long", false },
                new Object[] { Long.MAX_VALUE, "java.lang.*;!java.**;java.lang.Long", true },

                new Object[] { Long.MAX_VALUE, "!java.lang.Long;java.**;java.lang.*", false },
                new Object[] { Long.MAX_VALUE, "java.lang.Long;java.**;!java.lang.*", true },

                new Object[] { Long.MAX_VALUE, "java.lang.Long;!java.**;java.lang.*", false },
                new Object[] { Long.MAX_VALUE, "java.lang.Long;java.lang.Number;!java.**;java.lang.*", true },
        };
    }

    /**
     * Test:
     *   "global filter reject" + "specific ObjectInputStream filter is empty" => should reject
     *   "global filter reject" + "specific ObjectInputStream filter allow"    => should allow
     */
    @Test(dataProvider="Patterns")
    public void testRejectedInGlobal(Object toDeserialized, String pattern, boolean allowed) throws Exception {
        byte[] bytes = SerialFilterTest.writeObjects(toDeserialized);
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(pattern);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(filter);
            Object o = ois.readObject();
            assertTrue(allowed, "filter should have thrown an exception");
        } catch (InvalidClassException ice) {
            assertFalse(allowed, "filter should have thrown an exception");
        }
    }
}
