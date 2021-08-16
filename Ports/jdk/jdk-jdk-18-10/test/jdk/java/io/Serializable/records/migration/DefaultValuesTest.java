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
 * @library /test/lib
 * @modules jdk.compiler
 * @compile AssignableFrom.java Point.java DefaultValues.java SuperStreamFields.java
 * @run testng DefaultValuesTest
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.*;
import static java.lang.System.out;
import static org.testng.Assert.*;

/**
 * Basic test to check that default primitive / reference values are
 * presented to the record's canonical constructor, for fields not in
 * the stream.
 */
public class DefaultValuesTest extends AbstractTest {

    /**
     * Returns a stream of bytes for the given class, uid, flags. The
     * stream will have no stream field values.
     */
    static byte[] byteStreamFor(String className, long uid, byte flags)
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(className);
        dos.writeLong(uid);
        dos.writeByte(flags);
        dos.writeShort(0);             // number of fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.close();
        return baos.toByteArray();
    }

    /** Basic test with a simple Point. */
    @Test
    public void testPoint() throws Exception {
        out.println("--- testPoint ");

        byte[] bytes = byteStreamFor("PointImpl", 5L, (byte)SC_SERIALIZABLE);

        Point point = deserializeAsPlain(bytes);
        out.println("deserialized: " + point);
        assertEquals(point.x(), 0);
        assertEquals(point.y(), 0);

        point = deserializeAsRecord(bytes);
        out.println("deserialized: " + point);
        assertEquals(point.x(), 0);
        assertEquals(point.y(), 0);
    }

    // ---

    static class Defaults {  // default values
        static boolean bool;
        static byte by;
        static char ch;
        static short sh;
        static int i;
        static long l;
        static float f;
        static double d;
        static String str;
        static Object obj;
        static int[] ia;
        static Object[] oa;
    }

    /** A more comprehensive test with all primitives, reference, and array. */
    @Test
    public void testAllDefaults() throws Exception {
        out.println("--- testAllDefaults ");

        Point point = newPlainPoint(43, 98);
        DefaultValues o = newPlainDefaultValues(point);
        out.println("serialize   :" + o);
        byte[] bytes = serialize(o);
        DefaultValues o1 = deserializeAsRecord(bytes);
        out.println("deserialized: " + o1);

        assertEquals(o1.point().x(), point.x());   // sanity
        assertEquals(o1.point().y(), point.y());   // sanity
        assertTrue(o1.bool() == Defaults.bool);
        assertTrue(o1.by() == Defaults.by);
        assertTrue(o1.ch() == Defaults.ch);
        assertTrue(o1.sh() == Defaults.sh);
        assertTrue(o1.i() == Defaults.i);
        assertTrue(o1.l() == Defaults.l);
        assertTrue(o1.f() == Defaults.f);
        assertTrue(o1.d() == Defaults.d);
        assertTrue(o1.str() == Defaults.str);
        assertTrue(o1.obj() == Defaults.obj);
        assertTrue(o1.ia() == Defaults.ia);
        assertTrue(o1.oa() == Defaults.oa);
    }
}
