/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.hexdump;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @summary Check ObjectStreamPrinter formatting
 * @library /test/lib
 * @run testng/othervm -DDEBUG=true jdk.test.lib.hexdump.ObjectStreamPrinterTest
 */

/**
 * Test of the formatter is fairly coarse, formatting several
 * sample classes and spot checking the result string for key strings.
 */
@Test
public class ObjectStreamPrinterTest {

    // Override with (-DDEBUG=true) to see all the output
    private static boolean DEBUG = Boolean.getBoolean("DEBUG");

    @DataProvider(name = "serializables")
    Object[][] serializables() {
        return new Object[][]{
                {new Object[]{"abc", "def"}, 0, 0, 2},
                {new Object[]{0, 1}, 2, 2, 0},
                {new Object[]{TimeUnit.DAYS, TimeUnit.SECONDS}, 2, 0, 2},
                {new Object[]{List.of("one", "two", "three")}, 1, 1, 3},
                {new Object[]{genList()}, 1, 1, 2},
                {new Object[]{genMap()}, 1, 1, 5},
                {new Object[]{genProxy()}, 5, 2, 9},
                {new Object[]{new char[]{'x', 'y', 'z'},
                        new byte[]{0x61, 0x62, 0x63}, new int[]{4, 5, 6},
                        new float[]{1.0f, 2.0f, 3.1415927f},
                        new boolean[]{true, false, true},
                        new Object[]{"first", 3, 3.14159f}}, 9, 2, 1},
                { new Object[] {new XYPair(3, 5)}, 1, 1, 0},
        };
    }

    @DataProvider(name = "SingleObjects")
    Object[][] sources() {
        return new Object[][]{
                {"A Simple", new A(), 1, 1, 0},
                {"BNoDefaultRO has no call to defaultReadObject", new BNoDefaultRO(), 2, 1, 1},
                {"BDefaultRO has call to defaultReadObject", new BDefaultRO(), 2, 1, 1},
                {"CNoDefaultRO extends BNoDefaultRO with no fields", new CNoDefaultRO(), 3, 1, 3},
                {"CDefaultRO extends BDefaultRO with no fields", new CDefaultRO(), 3, 1, 3},
        };
    }


    /**
     * Checks the output of serializing an object, using HexPrinter
     * with an ObjectStreamPrinter formatter, and spot checking the number of
     * class descriptors, objects, and strings.
     *
     * @param objs an array of objects
     * @param descriptors the expected count of class descriptors
     * @param objects the expected count of objects
     * @param strings the expected count of strings
     * @throws IOException if any I/O exception occurs
     */
    @Test(dataProvider = "serializables")
    public void testFormat(Object[] objs, int descriptors, int objects, int strings) throws IOException {
        byte[] bytes = serializeObjects(objs);

        String result = HexPrinter.simple()
                .formatter(ObjectStreamPrinter.formatter())
                .toString(bytes);
        if (DEBUG)
            System.out.println(result);
        expectStrings(result, "CLASSDESC #", descriptors);
        expectStrings(result, "OBJ #", objects);
        expectStrings(result, "STRING #", strings);
    }

    /**
     * Checks the output of serializing an object, using an ObjectStreamPrinter formatter,
     * and spot checking the number of class descriptors, objects, and strings.
     *
     * @param objs an array of objects
     * @param descriptors the expected count of class descriptors
     * @param objects the expected count of objects
     * @param strings the expected count of strings
     * @throws IOException if any I/O exception occurs
     */
    @Test(dataProvider = "serializables", enabled=true)
    static void standAlonePrinter(Object[] objs, int descriptors, int objects, int strings) throws IOException{
        byte[] bytes = serializeObjects(objs);
        StringBuilder sb = new StringBuilder();

        try (InputStream in = new ByteArrayInputStream(bytes);
             DataInputStream din = new DataInputStream(in)) {
            var p = ObjectStreamPrinter.formatter();
            String s;
            while (!(s = p.annotate(din)).isEmpty()) {
                sb.append(s);
            }
        } catch (EOFException eof) {
            // Done
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        String result = sb.toString();
        if (DEBUG)
            System.out.println(result);
        expectStrings(result, "CLASSDESC #", descriptors);
        expectStrings(result, "OBJ #", objects);
        expectStrings(result, "STRING #", strings);
    }

    /**
     * Checks the output of serializing an object, using HexPrinter
     * with an ObjectStreamPrinter formatter, and spot checking the number of
     * class descriptors, objects, and strings.
     *
     * @param label a label string for the object being serialized
     * @param o an object
     * @param descriptors the expected count of class descriptors
     * @param objects the expected count of objects
     * @param strings the expected count of strings
     * @throws IOException if any I/O exception occurs
     */
    @Test(dataProvider = "SingleObjects")
    static void singleObjects(String label, Object o, int descriptors, int objects, int strings) throws IOException {
        if (DEBUG)
            System.out.println("Case: " + label);
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(boas)) {
            os.writeObject(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = boas.toByteArray();
        String result = HexPrinter.simple()
                .formatter(ObjectStreamPrinter.formatter(), "// ", 120)
                .toString(bytes);
        if (DEBUG)
            System.out.println(result);
        expectStrings(result, "CLASSDESC #", descriptors);
        expectStrings(result, "OBJ #", objects);
        expectStrings(result, "STRING #", strings);
    }

    /**
     * A specific test case for (TC_LONGSTRING) of a stream
     * containing a very long (0x10000) character string.
     *
     * @throws IOException if any I/O exception occurs
     */
    @Test
    static void longString() throws IOException {
        String large = " 123456789abcedf".repeat(0x1000);

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(boas)) {
            os.writeObject(large);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = boas.toByteArray();
        String result = HexPrinter.simple()
                .formatter(ObjectStreamPrinter.formatter(), "// ", 16 * 8 - 1)
                .toString(bytes);
        long lineCount = result.lines().count();
        assertEquals(4610, lineCount, "too many/few lines in result");
        if (DEBUG || lineCount != 4610) {
            // Show first few lines
            int off = 0;
            for (int c = 0; c < 4; c++)
                off = result.indexOf('\n', off + 1);
            System.out.println(result.substring(0, off));
            System.out.println("...");
        }
    }

    /**
     * Test the main method (without launching a separate process)
     * passing a file name as a parameter.
     * Each file should be formatted to stdout with no exceptions
     * @throws IOException if an I/O exception occurs
     */
    @Test
    static void testMain() throws IOException {
        Object[] objs = {genList()};
        byte[] bytes = serializeObjects(objs);   // A serialized List
        Path p = Path.of("scratch.tmp");
        Files.write(p, bytes);
        String[] args = {p.toString()};
        ObjectStreamPrinter.main(args);     // invoke main with the file name
    }

    /**
     * Serialize multiple objects to a single stream and return a byte array.
     * @param obj an array of Objects to serialize
     * @return a byte array with the serilized objects.
     * @throws IOException
     */
    private static byte[] serializeObjects(Object[] obj) throws IOException {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object o : obj)
                oos.writeObject(o);
            oos.flush();
            bytes = baos.toByteArray();
        }
        return bytes;
    }

    /**
     * Checks if the result string contains a number of key strings.
     * If not, it asserts an exception.
     * @param result the result string of formatting
     * @param key a key string to count
     * @param expectedCount the expected count of strings
     */
    static void expectStrings(String result, String key, int expectedCount) {
        int count = 0;
        for (int i = result.indexOf(key); i >= 0; i = result.indexOf(key, i + 1)) {
            count++;
        }
        assertEquals(count, expectedCount, "Occurrences of " + key);
    }

    public static List<String> genList() {
        List<String> l = new ArrayList<>();
        l.add("abc");
        l.add("def");
        return l;
    }

    public static Map<String, String> genMap() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");
        map.put("2.2", "Two");
        return map;
    }

    public static Object genProxy() {
        InvocationHandler h = (InvocationHandler & Serializable) (Object proxy, Method method, Object[] args) -> null;
        Class<?>[] intf = new Class<?>[]{Serializable.class, DataInput.class, DataOutput.class};
        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), intf, h);
    }

    static class A implements Serializable {
        private static final long serialVersionUID = 1L;
        int aIntValue = 1;
    }
    static class BNoDefaultRO extends A {
        private static final long serialVersionUID = 2L;
        private void writeObject(ObjectOutputStream os) throws IOException {
            os.writeInt(32);
            os.writeObject("bbb");
        }
        private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
            is.readInt();
            is.readObject();
        }
    }

    static class BDefaultRO extends A {
        private static final long serialVersionUID = 3L;
        private long bLongValue = 65535L;
        private void writeObject(ObjectOutputStream os) throws IOException {
            os.defaultWriteObject();
            os.writeInt(32);
            os.writeObject("bbb");
        }
        private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
            is.defaultReadObject();
            is.readInt();
            is.readObject();
        }
    }
    static class CNoDefaultRO extends BNoDefaultRO {
        private static final long serialVersionUID = 4L;
        int cIntValue = Integer.MIN_VALUE;
        String cString = "ccc";
    }
    static class CDefaultRO extends BDefaultRO {
        private static final long serialVersionUID = 5L;
        int cIntvalue = Integer.MIN_VALUE;
        String cString = "ccc";
    }

    static class XYPair implements Serializable {
        private static final long serialVersionUID = 6L;
        private int x;
        private int y;
        XYPair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
