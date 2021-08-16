/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4214513 8245036
 * @summary Passing a negative offset or length,
 *          or passing a combination of offset and length too big
 *          for readFully must throw IndexOutOfBoundsException.
 */

import java.io.*;

public class ReadFully {

    private static final void testNegativeOffset() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, -1, buffer.length);
            throw new RuntimeException("Test testNegativeOffset() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testNegativeLength() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, 0, -1);
            throw new RuntimeException("Test testNegativeLength() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testNegativeOffsetZeroLength() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, -1, 0);
            throw new RuntimeException("Test testNegativeOffsetZeroLength() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testBigOffsetLength1() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, 0, buffer.length + 1);
            throw new RuntimeException("Test testBigOffsetLength1() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testBigOffsetLength2() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, 1, buffer.length);
            throw new RuntimeException("Test testBigOffsetLength2() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testBigOffsetLength3() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, buffer.length, 1);
            throw new RuntimeException("Test testBigOffsetLength3() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final void testBigOffsetLength4() throws Exception {
        File file = new File(System.getProperty("test.src"),
                "ReadFully.java");
        try (FileInputStream in = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(in);) {
            byte[] buffer = new byte[100];
            dis.readFully(buffer, buffer.length + 1, 0);
            throw new RuntimeException("Test testBigOffsetLength4() failed");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    public static final void main(String[] args) throws Exception {
        testNegativeOffset();
        testNegativeLength();
        testNegativeOffsetZeroLength();
        testBigOffsetLength1();
        testBigOffsetLength2();
        testBigOffsetLength3();
        testBigOffsetLength4();
    }

}
