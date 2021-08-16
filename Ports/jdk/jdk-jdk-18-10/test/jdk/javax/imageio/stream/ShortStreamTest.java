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

/**
 * @test
 * @bug     8074954
 * @summary Test verifies that an IOException is triggered if input stream
 *          does not contain enough data to read a multi-byte type.
 *
 * @run     main ShortStreamTest
 */

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ShortStreamTest {
    public static void main(String[] args) throws IOException {
        TestCase[]  tests = createTests();

        for (TestCase t : tests) {
            t.test();
        }
    }

    private static abstract class TestCase {
        abstract void testRead(ImageInputStream iis) throws IOException;

        public void test() {
            boolean gotException = false;

            ImageInputStream iis = createShortStream();

            try {
                testRead(iis);
            } catch (IOException e) {
                e.printStackTrace(System.out);
                gotException = true;
            }

            if (!gotException) {
                throw new RuntimeException("Test failed.");
            }
            System.out.println("Test PASSED");
        }
    }


    private static ImageInputStream createShortStream() {
        try {
            byte[] integerTestArray = new byte[] { 80 };
            ByteArrayInputStream bais = new ByteArrayInputStream(integerTestArray);

            return ImageIO.createImageInputStream(bais);
        } catch (IOException e) {
            return null;
        }
    }

    private static TestCase[] createTests() {
        return new TestCase[]{
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readInt();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readShort();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readDouble();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readFloat();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readLong();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readUnsignedInt();
                    }
                },
                new TestCase() {
                    @Override
                    void testRead(ImageInputStream iis) throws IOException {
                        iis.readUnsignedShort();
                    }
                }
        };
    }
}
