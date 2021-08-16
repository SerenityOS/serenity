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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.Objects;


import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/* @test
 * @run testng/othervm  MOFilterTest
 *
 * @summary Test MarshalledObject applies ObjectInputFilter
 */
@Test
public class MOFilterTest {

    /**
     * Two cases are tested.
     * The filter = null and a filter set to verify the calls to the filter.
     * @return array objects with test parameters for each test case
     */
    @DataProvider(name = "FilterCases")
    public static Object[][] filterCases() {
        return new Object[][] {
                {true},     // run the test with the filter
                {false},    // run the test without the filter

        };
    }

    /**
     * Test that MarshalledObject inherits the ObjectInputFilter from
     * the stream it was deserialized from.
     */
    @Test(dataProvider="FilterCases")
    static void delegatesToMO(boolean withFilter) {
        try {
            Serializable testobj = Integer.valueOf(5);
            MarshalledObject<Serializable> mo = new MarshalledObject<>(testobj);
            Assert.assertEquals(mo.get(), testobj, "MarshalledObject.get returned a non-equals test object");

            byte[] bytes = writeObjects(mo);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {

                CountingFilter filter1 = new CountingFilter();
                ois.setObjectInputFilter(withFilter ? filter1 : null);
                MarshalledObject<?> actualMO = (MarshalledObject<?>)ois.readObject();
                int count = filter1.getCount();

                actualMO.get();
                int expectedCount = withFilter ? count + 2 : count;
                int actualCount = filter1.getCount();
                Assert.assertEquals(actualCount, expectedCount, "filter called wrong number of times during get()");
            }
        } catch (IOException ioe) {
            Assert.fail("Unexpected IOException", ioe);
        } catch (ClassNotFoundException cnf) {
            Assert.fail("Deserializing", cnf);
        }
    }

    /**
     * Write objects and return a byte array with the bytes.
     *
     * @param objects zero or more objects to serialize
     * @return the byte array of the serialized objects
     * @throws IOException if an exception occurs
     */
    static byte[] writeObjects(Object... objects)  throws IOException {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object o : objects) {
                oos.writeObject(o);
            }
            bytes = baos.toByteArray();
        }
        return bytes;
    }


    static class CountingFilter implements ObjectInputFilter {

        private int count;      // count of calls to the filter

        CountingFilter() {
            count = 0;
        }

        int getCount() {
            return count;
        }

        /**
         * Filter that rejects class Integer and allows others
         *
         * @param filterInfo access to the class, arrayLength, etc.
         * @return {@code STATUS.REJECTED}
         */
        public ObjectInputFilter.Status checkInput(FilterInfo filterInfo) {
            count++;
            return ObjectInputFilter.Status.ALLOWED;
        }
    }

}
