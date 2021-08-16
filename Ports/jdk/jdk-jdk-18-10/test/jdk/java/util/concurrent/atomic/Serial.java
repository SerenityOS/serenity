/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8026344
 * @summary Exercise classes in j.u.c.atomic that use serialization proxies
 */

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * Basic test to exercise the j.u.c.atomic classes that use serialization
 * proxies.
 */
public class Serial {

    public static void main(String[] args) {
        testDoubleAdder();
        testDoubleAccumulator();
        testLongAdder();
        testLongAccumulator();
    }

    static void testDoubleAdder() {
        DoubleAdder a = new DoubleAdder();
        a.add(20.1d);
        DoubleAdder result = echo(a);
        if (result.doubleValue() != a.doubleValue())
            throw new RuntimeException("Unexpected doubleValue");

        checkSerialClassName(a, "java.util.concurrent.atomic.DoubleAdder$SerializationProxy");
    }

    static void testDoubleAccumulator() {
        DoubleBinaryOperator plus = (DoubleBinaryOperator & Serializable) (x, y) -> x + y;
        DoubleAccumulator a = new DoubleAccumulator(plus, 13.9d);
        a.accumulate(17.5d);
        DoubleAccumulator result = echo(a);
        if (result.get() != a.get())
            throw new RuntimeException("Unexpected value");
        a.reset();
        result.reset();
        if (result.get() != a.get())
            throw new RuntimeException("Unexpected value after reset");

        checkSerialClassName(a, "java.util.concurrent.atomic.DoubleAccumulator$SerializationProxy");
    }

    static void testLongAdder() {
        LongAdder a = new LongAdder();
        a.add(45);
        LongAdder result = echo(a);
        if (result.longValue() != a.longValue())
            throw new RuntimeException("Unexpected longValue");

        checkSerialClassName(a, "java.util.concurrent.atomic.LongAdder$SerializationProxy");
    }

    static void testLongAccumulator() {
        LongBinaryOperator plus = (LongBinaryOperator & Serializable) (x, y) -> x + y;
        LongAccumulator a = new LongAccumulator(plus, -2);
        a.accumulate(34);
        LongAccumulator result = echo(a);
        if (result.get() != a.get())
            throw new RuntimeException("Unexpected value");
        a.reset();
        result.reset();
        if (result.get() != a.get())
            throw new RuntimeException("Unexpected value after reset");

        checkSerialClassName(a, "java.util.concurrent.atomic.LongAccumulator$SerializationProxy");
    }

    /**
     * Serialize the given object, returning the reconstituted object.
     */
    @SuppressWarnings("unchecked")
    static <T extends Serializable> T echo(T obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed: " + e);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deserialization failed: " + e);
        }
    }

    /**
     * Checks that the given object serializes to the expected class.
     */
    static void checkSerialClassName(Serializable obj, String expected) {
        String cn = serialClassName(obj);
        if (!cn.equals(expected))
            throw new RuntimeException(obj.getClass() + " serialized as " + cn
                + ", expected " + expected);
    }

    /**
     * Returns the class name that the given object serializes as.
     */
    static String serialClassName(Serializable obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed: " + e);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (DataInputStream dis = new DataInputStream(in)) {
            dis.readShort();      // STREAM_MAGIC
            dis.readShort();      // STREAM_VERSION
            dis.readByte();       // TC_OBJECT
            dis.readByte();       // TC_CLASSDESC
            return dis.readUTF(); // className
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
