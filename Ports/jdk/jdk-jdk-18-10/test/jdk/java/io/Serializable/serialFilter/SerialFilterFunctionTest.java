/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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


import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.FilterInfo;
import java.util.function.Predicate;

import static java.io.ObjectInputFilter.Status;
import static java.io.ObjectInputFilter.Status.ALLOWED;
import static java.io.ObjectInputFilter.Status.REJECTED;
import static java.io.ObjectInputFilter.Status.UNDECIDED;

/* @test
 * @run testng/othervm -Djava.util.logging.config.file=${test.src}/logging.properties
 *                      SerialFilterFunctionTest
 * @summary ObjectInputFilter.Config Function Tests
 */
@Test
public class SerialFilterFunctionTest {

    @Test
    void testMerge() {
        Status[] cases = Status.values();
        FilterInfo info = new SerialInfo(Object.class);
        for (Status st1 : cases) {
            ObjectInputFilter filter1 = getFilter(st1);
            for (Status st2 : cases) {
                ObjectInputFilter filter2 = getFilter(st2);
                ObjectInputFilter f = ObjectInputFilter.merge(filter1, filter2);
                Status r = f.checkInput(info);
                Assert.assertEquals(merge(st1, st2), r, "merge");
            }
            Assert.assertSame(ObjectInputFilter.merge(filter1, null), filter1, "merge with null fail");
            Assert.assertThrows(NullPointerException.class, () -> ObjectInputFilter.merge(null, filter1));
        }
    }

    /**
     * Return REJECTED if either is REJECTED; otherwise return ALLOWED if either is ALLOWED, else UNDECIDED.
     * @param status a status
     * @param otherStatus another status
     * @return REJECTED if either is REJECTED; otherwise return ALLOWED if either is ALLOWED, else UNDECIDED
     */
    private Status merge(Status status, Status otherStatus) {
        if (REJECTED.equals(status) || REJECTED.equals(otherStatus))
            return REJECTED;

        if (ALLOWED.equals(status)  || ALLOWED.equals(otherStatus))
            return ALLOWED;

        return UNDECIDED;
    }

    /**
     * Return a predicate mapping Class<?> to a boolean that returns true if the argument is Integer.class.
     * @return a predicate mapping Class<?> to a boolean that returns true if the argument is Integer.class
     */
    static Predicate<Class<?>> isInteger() {
        return (cl) -> cl.equals(Integer.class);
    }

    @DataProvider(name = "AllowPredicateCases")
    static Object[][] allowPredicateCases() {
        return new Object[][]{
                { Integer.class, isInteger(), REJECTED, ALLOWED},
                { Double.class, isInteger(), REJECTED, REJECTED},
                { null, isInteger(), REJECTED, UNDECIDED},      // no class -> UNDECIDED
                { Double.class, isInteger(), null, null},       // NPE
                { Double.class, null, REJECTED, null},          // NPE
        };
    }

    @Test(dataProvider = "AllowPredicateCases")
    void testAllowPredicates(Class<?> clazz, Predicate<Class<?>> predicate, Status otherStatus, Status expected) {
        ObjectInputFilter.FilterInfo info = new SerialInfo(clazz);
        if (predicate == null || expected == null) {
            Assert.assertThrows(NullPointerException.class, () -> ObjectInputFilter.allowFilter(predicate, expected));
        } else {
            Assert.assertEquals(ObjectInputFilter.allowFilter(predicate, otherStatus).checkInput(info),
                    expected, "Predicate result");
        }
    }

    @DataProvider(name = "RejectPredicateCases")
    static Object[][] rejectPredicateCases() {
        return new Object[][]{
                { Integer.class, isInteger(), REJECTED, REJECTED},
                { Double.class, isInteger(), ALLOWED, ALLOWED},
                { null, isInteger(), REJECTED, UNDECIDED},      // no class -> UNDECIDED
                { Double.class, isInteger(), null, null},         // NPE
                { Double.class, null, UNDECIDED, null},    // NPE
        };
    }

    @Test(dataProvider = "RejectPredicateCases")
    void testRejectPredicates(Class<?> clazz, Predicate<Class<?>> predicate, Status otherStatus, Status expected) {
        ObjectInputFilter.FilterInfo info = new SerialInfo(clazz);
        if (predicate == null || expected == null) {
            Assert.assertThrows(NullPointerException.class, () -> ObjectInputFilter.allowFilter(predicate, expected));
        } else {
            Assert.assertEquals(ObjectInputFilter.rejectFilter(predicate, otherStatus)
                    .checkInput(info), expected, "Predicate result");
        }
    }

    @Test
    void testRejectUndecided() {
        FilterInfo info = new SerialInfo(Object.class); // an info structure, unused

        ObjectInputFilter undecided = getFilter(UNDECIDED);
        Assert.assertEquals(ObjectInputFilter.rejectUndecidedClass(undecided).checkInput(info), REJECTED, "undecided -> rejected");
        ObjectInputFilter allowed = getFilter(ALLOWED);
        Assert.assertEquals(ObjectInputFilter.rejectUndecidedClass(allowed).checkInput(info), ALLOWED, "allowed -> rejected");
        ObjectInputFilter rejected = getFilter(REJECTED);
        Assert.assertEquals(ObjectInputFilter.rejectUndecidedClass(rejected).checkInput(info), REJECTED, "rejected -> rejected");

        // Specific cases of Classes the result in allowed, rejected, and undecided status
        ObjectInputFilter numberFilter = ObjectInputFilter.Config.createFilter("java.lang.Integer;!java.lang.Double");
        Object[] testObjs = {
                Integer.valueOf(1),         // Integer is allowed -> allowed
                new Integer[1],             // Integer is allowed -> allowed
                new Integer[0][0][0],       // Integer is allowed -> allowed
                Long.valueOf(2),            // Long is undecided -> rejected
                new Long[1],                // Long is undecided -> rejected
                new Long[0][0][0],          // Long is undecided -> rejected
                Double.valueOf(2.0d),       // Double is rejected -> rejected
                new Double[1],              // Double is rejected -> rejected
                new Double[0][0][0],        // Double is rejected -> rejected
                new int[1],                 // int is primitive undecided -> undecided
                new int[1][1][1],           // int is primitive undecided -> undecided
                };

        for (Object obj : testObjs) {
            Class<?> clazz = obj.getClass();
            info = new SerialInfo(clazz);
            Status rawSt = numberFilter.checkInput(info);
            Status st = ObjectInputFilter.rejectUndecidedClass(numberFilter).checkInput(info);
            if (UNDECIDED.equals(rawSt)) {
                while (clazz.isArray())
                    clazz = clazz.getComponentType();
                Status expected = (clazz.isPrimitive()) ? UNDECIDED : REJECTED;
                Assert.assertEquals(st, expected, "Wrong status for class: " + obj.getClass());
            } else {
                Assert.assertEquals(rawSt, st, "raw filter and rejectUndecided filter disagree");
            }
        }
    }

    /**
     * Returns an ObjectInputFilter that returns the requested Status.
     * @param status a Status, may be null
     * @return  an ObjectInputFilter that returns the requested Status
     */
    private static ObjectInputFilter getFilter(ObjectInputFilter.Status status) {
        return (info) -> status;
    }

    /**
     * FilterInfo instance with a specific class.
     */
    static class SerialInfo implements ObjectInputFilter.FilterInfo {
        private final Class<?> clazz;

        SerialInfo(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> serialClass() {
            return clazz;
        }

        @Override
        public long arrayLength() {
            return 0;
        }

        @Override
        public long depth() {
            return 0;
        }

        @Override
        public long references() {
            return 0;
        }

        @Override
        public long streamBytes() {
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("serialClass: " + serialClass());
            sb.append(", arrayLength: " + arrayLength());
            sb.append(", depth: " + depth());
            sb.append(", references: " + references());
            sb.append(", streamBytes: " + streamBytes());
            return sb.toString();
        }
    }
}
