/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8033980
 * @summary verify serialization compatibility for XMLGregorianCalendar and Duration
 * @run main SerializationTest read
 */

import java.io.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * use "read" to test compatibility
 * SerializationTest read
 *
 * use "write" to create test files
 * SerializationTest write javaVersion
 * where javaVersion is 6, 7, 8, or 9
 *
 * @author huizhe.wang@oracle.com</a>
 */
public class SerializationTest {

    final String FILENAME_CAL = "_XMLGregorianCalendar.ser";
    final String FILENAME_DURATION = "_Duration.ser";
    String filePath;

    {
        filePath = System.getProperty("test.src");
        if (filePath == null) {
            //current directory
            filePath = System.getProperty("user.dir");
        }
        filePath += File.separator;
    }
    final String EXPECTED_CAL = "0001-01-01T00:00:00.0000000-05:00";
    final String EXPECTED_DURATION = "P1Y1M1DT1H1M1S";
    static String[] JDK = {"JDK6", "JDK7", "JDK8", "JDK9"};

    public static void main(String[] args) {
        SerializationTest test = new SerializationTest();

        if (args[0].equalsIgnoreCase("read")) {
            test.testReadCal();
            test.testReadDuration();
            test.report();
        } else {
            int ver = Integer.valueOf(args[1]).intValue();
            test.createTestFile(JDK[ver - 6]);
        }

    }

    public void testReadCal() {
        try {
            for (String javaVersion : JDK) {
                XMLGregorianCalendar d1 = (XMLGregorianCalendar) fromFile(
                        javaVersion + FILENAME_CAL);
                if (!d1.toString().equalsIgnoreCase(EXPECTED_CAL)) {
                    fail("Java version: " + javaVersion
                            + "\nExpected: " + EXPECTED_CAL
                            + "\nActual: " + d1.toString());
                } else {
                    success("testReadCal: read " + javaVersion + " serialized file, passed.");
                }
            }
        } catch (ClassNotFoundException ex) {
            fail("testReadCal: " + ex.getMessage());
        } catch (IOException ex) {
            fail("testReadCal: " + ex.getMessage());
        }
    }

    public void testReadDuration() {
        try {
            for (String javaVersion : JDK) {
                Duration d1 = (Duration) fromFile(
                        javaVersion + FILENAME_DURATION);
                if (!d1.toString().equalsIgnoreCase(EXPECTED_DURATION)) {
                    fail("Java version: " + javaVersion
                            + "\nExpected: " + EXPECTED_DURATION
                            + "\nActual: " + d1.toString());
                } else {
                    success("testReadDuration: read " + javaVersion + " serialized file, passed.");
                }
            }
        } catch (ClassNotFoundException ex) {
            fail("testReadDuration: " + ex.getMessage());
        } catch (IOException ex) {
            fail("testReadDuration: " + ex.getMessage());
        }
    }

    /**
     * Create test files
     *
     * @param javaVersion JDK version
     */
    public void createTestFile(String javaVersion) {
        try {
            DatatypeFactory dtf = DatatypeFactory.newInstance();
            XMLGregorianCalendar c = dtf.newXMLGregorianCalendar(EXPECTED_CAL);
            Duration d = dtf.newDuration(EXPECTED_DURATION);
            toFile((Serializable) c, filePath + javaVersion + FILENAME_CAL);
            toFile((Serializable) d, filePath + javaVersion + FILENAME_DURATION);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Read the object from a file.
     */
    private static Object fromFile(String filePath) throws IOException,
            ClassNotFoundException {
        InputStream streamIn = SerializationTest.class.getResourceAsStream(
            filePath);
        ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
        Object o = objectinputstream.readObject();
        return o;
    }

    /**
     * Write the object to a file.
     */
    private static void toFile(Serializable o, String filePath) throws IOException {
        FileOutputStream fout = new FileOutputStream(filePath, true);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(o);
        oos.close();
    }

    static String errMessage;
    int passed = 0, failed = 0;

    void fail(String errMsg) {
        if (errMessage == null) {
            errMessage = errMsg;
        } else {
            errMessage = errMessage + "\n" + errMsg;
        }
        failed++;
    }

    void success(String msg) {
        passed++;
        System.out.println(msg);
    }

    public void report() {

        System.out.println("\nNumber of tests passed: " + passed);
        System.out.println("Number of tests failed: " + failed + "\n");

        if (errMessage != null) {
            throw new RuntimeException(errMessage);
        }
    }
}
