/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8041565
 * @summary Tests the limits imposed on the domain name part of an
 *          ObjectName instance
 * @author Jaroslav Bachorik
 * @modules java.management/javax.management:open
 * @run main CompressedStorageTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class CompressedStorageTest {
    private static Method setDomainLengthM;
    private static Field compressedStorageFld;

    private static int DOMAIN_PATTERN;
    private static int PROPLIST_PATTERN;
    private static int PROPVAL_PATTERN;

    private static Method setDomainPattern;
    private static Method setPropertyListPattern;
    private static Method setPropertyValuePattern;


    static {
        try {
            Class<?> clz = ObjectName.class;
            setDomainLengthM = clz.getDeclaredMethod("setDomainLength", int.class);
            setDomainLengthM.setAccessible(true);

            compressedStorageFld = clz.getDeclaredField("_compressed_storage");
            compressedStorageFld.setAccessible(true);

            setDomainPattern = clz.getDeclaredMethod("setDomainPattern", boolean.class);
            setDomainPattern.setAccessible(true);
            setPropertyListPattern = clz.getDeclaredMethod("setPropertyListPattern", boolean.class);
            setPropertyListPattern.setAccessible(true);
            setPropertyValuePattern = clz.getDeclaredMethod("setPropertyValuePattern", boolean.class);
            setPropertyValuePattern.setAccessible(true);

            DOMAIN_PATTERN = getStaticIntFld("DOMAIN_PATTERN");
            PROPLIST_PATTERN = getStaticIntFld("PROPLIST_PATTERN");
            PROPVAL_PATTERN = getStaticIntFld("PROPVAL_PATTERN");

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) throws Exception {
        testZeroLength();
        testNegativeLength();
        testMaxLength();

        testSetDomainPattern();
        testSetPropertyListPattern();
        testSetPropertyValuePattern();
    }

    private static ObjectName getObjectName()
    throws MalformedObjectNameException {
        return new ObjectName("domain", "key", "value");
    }

    /**
     * Test for accepting 0 being passed as argument to
     * {@linkplain ObjectName#setDomainLength(int)}.
     *
     */
    private static void testZeroLength() throws Exception {
        setDomainNameLength(0);
    }

    /**
     * Test for rejecting negative value being passed as argument to
     * {@linkplain ObjectName#setDomainLength(int)}.
     */
    private static void testNegativeLength() throws Exception {
        try {
            setDomainNameLength(-1);
        } catch (MalformedObjectNameException e) {
            return;
        }
        fail("Allowing negative domain name length");
    }

    /**
     * Test for rejecting value exceeding the maximum allowed length
     * being passed as argument to {@linkplain ObjectName#setDomainLength(int)}.
     */
    private static void testMaxLength() throws Exception {
        try {
            setDomainNameLength(Integer.MAX_VALUE / 4 + 1);
        } catch (MalformedObjectNameException e) {
            return;
        }
        fail("Maximum domain name length is not respected");
    }

    /**
     * Tests that calling {@linkplain ObjectName#setDomainPattern(boolean)}
     * results in setting correct bits in {@linkplain ObjectName#_compressed_storage}.
     */
    private static void testSetDomainPattern() throws Exception {
        ObjectName on = getObjectName();

        checkMask(DOMAIN_PATTERN, setDomainPattern, on);
    }

    /**
     * Tests that calling {@linkplain ObjectName#setPropertyListPattern(boolean)}
     * results in setting correct bits in {@linkplain ObjectName#_compressed_storage}.
     */
    private static void testSetPropertyListPattern() throws Exception {
        ObjectName on = getObjectName();

        checkMask(PROPLIST_PATTERN, setPropertyListPattern, on);
    }

    /**
     * Tests that calling {@linkplain ObjectName#setPropertyValuePattern(boolean)}
     * results in setting correct bits in {@linkplain ObjectName#_compressed_storage}.
     */
    private static void testSetPropertyValuePattern() throws Exception {
        ObjectName on = getObjectName();

        checkMask(PROPVAL_PATTERN, setPropertyValuePattern, on);
    }

    /**
     * Helper method to call {@linkplain ObjectName#setDomainLength(int)}
     * method via reflection.
     * @param len The domain name length
     * @throws MalformedObjectNameException Propagated from
     *           {@linkplain ObjectName#setDomainLength(int)} invocation.
     */
    private static void setDomainNameLength(int len)
    throws MalformedObjectNameException {
        try {
            setDomainLengthM.invoke(getObjectName(), len);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MalformedObjectNameException) {
                throw (MalformedObjectNameException)cause;
            }
            throw new Error(cause);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new Error(e);
        }
    }

    /**
     * Helper method to assert that a particular boolean setter affects only
     * a particular bit in the {@linkplain ObjectName#_compressed_storage} field.
     * @param mask bitmask for storing the boolean value
     * @param setter setter method reference
     * @param on {@linkplain ObjectName} instance
     */
    private static void checkMask(int mask, Method setter, ObjectName on)
    throws Exception {
        int valBefore = compressedStorageFld.getInt(on);
        setter.invoke(on, true);
        int valAfter = compressedStorageFld.getInt(on);

        checkMask(mask, valAfter ^ valBefore);

        valBefore = valAfter;
        setter.invoke(on, false);
        valAfter = compressedStorageFld.getInt(on);

        checkMask(mask, valAfter ^ valBefore);
    }

    /**
     * Compare the changed bits with the given mask.
     * @param mask bitmask
     * @param val the changed bits; may be 0 if there was no change
     */
    private static void checkMask(int mask, int val) {
        if (val != 0 && val != mask) {
            fail("Invalid mask: expecting '" +
                    Integer.toBinaryString(mask) + "' , received '" +
                    Integer.toBinaryString(val) + "'");
        }
    }

    /**
     * Helper method to obtain the value of a static field via reflection.
     * @param name static field name
     * @return static field value
     */
    private static int getStaticIntFld(String name) throws Exception {
        Field fld = ObjectName.class.getDeclaredField(name);
        fld.setAccessible(true);

        return fld.getInt(null);
    }

    private static void fail(String msg) {
        throw new Error(msg);
    }
}
