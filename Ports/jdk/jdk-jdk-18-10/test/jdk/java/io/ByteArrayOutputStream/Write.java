/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4017158 8180410
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng Write
 * @summary Check for correct implementation of ByteArrayInputStream.write
 * @key randomness
 */

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.RandomFactory;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Write {
    private static void doBoundsTest(byte[] b, int off, int len,
                                     ByteArrayOutputStream baos)
        throws Exception {
        if (b != null) {
            System.out.println("ByteArrayOutStream.write: b.length = " +
                               b.length + " off = " + off + " len = " + len);
        } else{
            System.out.println("ByteArrayOutStream.write: b is null off = " +
                               off + " len = " + len);
        }

        try {
            baos.write(b, off, len);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException is thrown: OKAY");
        } catch (NullPointerException e) {
            System.out.println("NullPointerException is thrown: OKAY");
        } catch (Throwable e){
            throw new RuntimeException("Unexpected Exception is thrown", e);
        }

        if (b != null) {
            System.out.println("ByteArrayOutStream.writeBytes: b.length = " +
                               b.length);
        } else{
            System.out.println("ByteArrayOutStream.writeBytes: b is null");
        }

        try {
            baos.writeBytes(b);
        } catch (NullPointerException e) {
            System.out.println("NullPointerException is thrown: OKAY");
        } catch (Throwable e){
            throw new RuntimeException("Unexpected Exception is thrown", e);
        }
    }

    @Test
    public static void boundsTest() throws Exception {
        byte array1[] = {1 , 2 , 3 , 4 , 5};     // Simple array

        //Create new ByteArrayOutputStream object
        ByteArrayOutputStream y1 = new ByteArrayOutputStream(5);

        doBoundsTest(array1, 0, Integer.MAX_VALUE , y1);
        doBoundsTest(array1, 0, array1.length+100, y1);
        doBoundsTest(array1, -1, 2, y1);
        doBoundsTest(array1, 0, -1, y1);
        doBoundsTest(null, 0, 2, y1);
    }

    @Test
    public static void writeTest() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Random rnd = RandomFactory.getRandom();
        final int size = 17 + rnd.nextInt(128);

        byte[] b = new byte[size];
        rnd.nextBytes(b);

        int off1 = rnd.nextInt(size / 4) + 1;
        int len1 = Math.min(rnd.nextInt(size / 4) + 1, size - off1);
        int off2 = rnd.nextInt(size / 2) + 1;
        int len2 = Math.min(rnd.nextInt(size / 2) + 1, size - off2);

        System.out.format("size: %d, off1: %d, len1: %d, off2: %d, len2: %d%n",
            size, off1, len1, off2, len2);

        baos.write(b, off1, len1);
        byte[] b1 = baos.toByteArray();
        assertEquals(b1.length, len1, "Array length test 1 failed.");
        assertEquals(b1, Arrays.copyOfRange(b, off1, off1 + len1),
            "Array equality test 1 failed.");

        baos.write(b, off2, len2);
        byte[] b2 = baos.toByteArray();
        assertEquals(b2.length, len1 + len2, "Array length test 2 failed.");
        assertEquals(Arrays.copyOfRange(b2, 0, len1),
             Arrays.copyOfRange(b, off1, off1 + len1),
            "Array equality test 2A failed.");
        assertEquals(Arrays.copyOfRange(b2, len1, len1 + len2),
            Arrays.copyOfRange(b, off2, off2 + len2),
            "Array equality test 2B failed.");

        baos.writeBytes(b);
        byte[] b3 = baos.toByteArray();
        int len3 = len1 + len2 + b.length;
        if (b3.length != len1 + len2 + b.length) {
            throw new RuntimeException("Array length test 3 failed.");
        }
        assertEquals(b3.length, len3, "Array length test 3 failed.");
        assertEquals(Arrays.copyOfRange(b3, 0, len1),
             Arrays.copyOfRange(b, off1, off1 + len1),
            "Array equality test 3A failed.");
        assertEquals(Arrays.copyOfRange(b3, len1, len1 + len2),
            Arrays.copyOfRange(b, off2, off2 + len2),
            "Array equality test 3B failed.");
        assertEquals(Arrays.copyOfRange(b3, len1 + len2, len3), b,
            "Array equality test 3C failed.");
    }
}
