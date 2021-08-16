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

package jdk.test.lib.hexdump;

import org.testng.annotations.BeforeClass;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @summary Test StreamDump utility
 * @library /test/lib
 * @build jdk.test.lib.hexdump.StreamDump
 * @run testng jdk.test.lib.hexdump.StreamDumpTest
 */

/**
 * Test of the formatter is fairly coarse, formatting several
 * sample classes and spot checking the result string for key strings.
 */
@Test
public class StreamDumpTest {

    private final static Path workDir = Path.of(".");

    private final static String classpath = System.getProperty("test.class.path", ".");

    private final static String testJDK = System.getProperty("test.jdk");

    private final static String testSRC = System.getProperty("test.src", ".");

    private final static String serializedListPath = createTmpSer();

    /**
     * Create a file containing an example serialized list.
     * @return the path to the file.
     */
    private static String createTmpSer() {
        try {
            Object[] objs = {genList()};
            byte[] bytes = serializeObjects(objs);   // A serialized List
            Path path = Files.createTempFile(workDir, "list", ".ser");
            Files.write(path, bytes);
            return path.toString();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Arguments lists to be passed when invoking StreamDump.
     * Arg list and the expected exit status, stdout line count, and stderr line count.
     * @return array of argument list arrays.
     */
    @DataProvider(name = "serializables")
    Object[][] serializables() {
        return new Object[][] {
                {new String[]{testSRC + "/openssl.p12.pem"},
                        0, 126, 0},
                {new String[]{"--formatter", "jdk.test.lib.hexdump.ASN1Formatter", testSRC + "/openssl.p12.pem"},
                        0, 126, 0},
                {new String[]{serializedListPath},
                        0, 19, 0},
                {new String[]{"--formatter", "jdk.test.lib.hexdump.ObjectStreamPrinter", serializedListPath},
                        0, 19, 0},
                {new String[]{},
                        1, 2, 0},    // no file arguments
                {new String[]{"--formatter"},
                        1, 2, 0},       // --formatter option requires a class name
                {new String[]{"-formatter", "jdk.test.lib.hexdump.ObjectStreamPrinter"},
                        1, 2, 0},       // options start with double "--"
        };
    }


    /**
     * Test the main method (without launching a separate process)
     * passing a file name as a parameter.
     * Each file should be formatted to stdout with no exceptions
     * @throws IOException if an I/O exception occurs
     */
    @Test(dataProvider="serializables")
    static void testStreamDump(String[] args, int expectedStatus, int expectedStdout, int expectedStderr) throws IOException {
        List<String> argList = new ArrayList<>();
        argList.add(testJDK + "/bin/" + "java");
        argList.add("-classpath");
        argList.add(classpath);
        argList.add("jdk.test.lib.hexdump.StreamDump");
        argList.addAll(Arrays.asList(args));

        Path stdoutPath = Files.createTempFile(workDir, "stdout", ".log");
        Path stderrPath = Files.createTempFile(workDir, "stderr", ".log");

        ProcessBuilder pb = new ProcessBuilder(argList);
        pb.redirectOutput(stdoutPath.toFile());
        pb.redirectOutput(stdoutPath.toFile());

        System.out.println("args: " + argList);
        Process p = pb.start();
        try {
            int actualStatus = p.waitFor();
            fileCheck(stdoutPath, expectedStdout);
            fileCheck(stderrPath, expectedStderr);
            assertEquals(actualStatus, expectedStatus, "Unexpected exit status");
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Check that the file exists and contains the expected number of lines.
     * @param path a file path
     * @param expectedLines the number of lines expected
     * @throws IOException if an I/O exception occurs
     */
    static void fileCheck(Path path, int expectedLines) throws IOException {
        long actualLines = Files.newBufferedReader(path).lines().count();
        if (actualLines != expectedLines) {
            System.out.printf("%s: lines %d, expected: %d%n", path, actualLines, expectedLines);
            System.out.println("---Begin---");
            Files.newBufferedReader(path).lines().forEach(s -> System.out.println(s));
            System.out.println("----End----");
        }
        assertEquals(actualLines, expectedLines, "Unexpected line count");
    }

    /**
     * Serialize multiple objects to a single stream and return a byte array.
     *
     * @param obj an array of Objects to serialize
     * @return a byte array with the serilized objects.
     * @throws IOException if an I/O exception occurs
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



}
