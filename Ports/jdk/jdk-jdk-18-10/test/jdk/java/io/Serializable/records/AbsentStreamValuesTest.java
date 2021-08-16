/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Checks that the appropriate default value is given to the canonical ctr
 * @run testng AbsentStreamValuesTest
 * @run testng/othervm/java.security.policy=empty_security.policy AbsentStreamValuesTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.*;
import static java.lang.System.out;
import static org.testng.Assert.*;

/**
 * Basic test to check that default primitive / reference values are presented
 * to the record's canonical constructor, for fields not in the stream.
 */
public class AbsentStreamValuesTest {

    record R01(boolean  x) implements Serializable { }
    record R02(byte     x) implements Serializable { }
    record R03(short    x) implements Serializable { }
    record R04(char     x) implements Serializable { }
    record R05(int      x) implements Serializable { }
    record R06(long     x) implements Serializable { }
    record R07(float    x) implements Serializable { }
    record R08(double   x) implements Serializable { }
    record R09(Object   x) implements Serializable { }
    record R10(String   x) implements Serializable { }
    record R11(int[]    x) implements Serializable { }
    record R12(Object[] x) implements Serializable { }
    record R13(R12      x) implements Serializable { }
    record R14(R13[]    x) implements Serializable { }

    @DataProvider(name = "recordTypeAndExpectedValue")
    public Object[][] recordTypeAndExpectedValue() {
        return new Object[][] {
                new Object[] { R01.class, false    },
                new Object[] { R02.class, (byte)0  },
                new Object[] { R03.class, (short)0 },
                new Object[] { R04.class, '\u0000' },
                new Object[] { R05.class, 0        },
                new Object[] { R06.class, 0L       },
                new Object[] { R07.class, 0.0f     },
                new Object[] { R08.class, 0.0d     },
                new Object[] { R09.class, null     },
                new Object[] { R10.class, null     },
                new Object[] { R11.class, null     },
                new Object[] { R12.class, null     },
                new Object[] { R13.class, null     },
                new Object[] { R14.class, null     },
        };
    }

    @Test(dataProvider = "recordTypeAndExpectedValue")
    public void testWithDifferentTypes(Class<?> clazz, Object expectedXValue)
        throws Exception
    {
        out.println("\n---");
        assert clazz.isRecord();
        byte[] bytes = minimalByteStreamFor(clazz.getName());

        Object obj = deserialize(bytes);
        out.println("deserialized: " + obj);
        Object actualXValue = clazz.getDeclaredMethod("x").invoke(obj);
        assertEquals(actualXValue, expectedXValue);
    }

    // --- all together

    record R15(boolean a, byte b, short c, char d, int e, long f, float g, double h, Object i, String j, long[] k, Object[] l)
        implements Serializable { }

    @Test
    public void testWithAllTogether() throws Exception {
        out.println("\n---");
        byte[] bytes = minimalByteStreamFor(R15.class.getName());

        R15 obj = (R15)deserialize(bytes);
        out.println("deserialized: " + obj);
        assertEquals(obj.a, false);
        assertEquals(obj.b, 0);
        assertEquals(obj.c, 0);
        assertEquals(obj.d, '\u0000');
        assertEquals(obj.e, 0);
        assertEquals(obj.f, 0l);
        assertEquals(obj.g, 0f);
        assertEquals(obj.h, 0d);
        assertEquals(obj.i, null);
        assertEquals(obj.j, null);
        assertEquals(obj.k, null);
        assertEquals(obj.l, null);
    }

    // --- generic type

    record R16<T, U>(T t, U u) implements Serializable { }

    @Test
    public void testGenericType() throws Exception {
        out.println("\n---");
        byte[] bytes = minimalByteStreamFor(R16.class.getName());

        R16 obj = (R16)deserialize(bytes);
        out.println("deserialized: " + obj);
        assertEquals(obj.t, null);
        assertEquals(obj.u, null);
    }

    // --- infra

    /**
     * Returns the serial bytes for the given class name. The stream
     * will have no stream field values.
     */
    static byte[] minimalByteStreamFor(String className) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(className);
        dos.writeLong(0L);
        dos.writeByte(SC_SERIALIZABLE);
        dos.writeShort(0);             // number of fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }
}
