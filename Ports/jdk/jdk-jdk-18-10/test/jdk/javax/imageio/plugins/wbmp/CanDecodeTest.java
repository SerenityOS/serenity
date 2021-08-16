/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5101862
 * @summary Test verifies that SPI of WBMP image reader
 *           does not claims to be able to decode QT movies,
 *           tga images, or ico files.
 * @run     main CanDecodeTest
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class CanDecodeTest {

    public static void main(String[] args) throws IOException {
        ImageReader r =
                ImageIO.getImageReadersByFormatName("WBMP").next();
        ImageReaderSpi spi = r.getOriginatingProvider();

        Vector<TestCase> tests = getTestCases();
        for (TestCase t : tests) {
            t.doTest(spi);
        }
        System.out.println("Test passed.");
    }

    private static Vector<TestCase> getTestCases() {
        Vector<TestCase> v = new Vector<TestCase>(4);
        v.add(new TestCase("wbmp", new byte[]{(byte) 0x00, (byte) 0x00,
                    (byte) 0x60, (byte) 0x14}, 244, true));
        v.add(new TestCase("mov", new byte[]{(byte) 0x00, (byte) 0x00,
                    (byte) 0x07, (byte) 0xb5, (byte) 0x6d}, 82397, false));
        v.add(new TestCase("tga", new byte[]{(byte) 0x00, (byte) 0x00,
                    (byte) 0x0a, (byte) 0x00}, 39693, false));
        v.add(new TestCase("ico", new byte[]{(byte) 0x00, (byte) 0x00,
                    (byte) 0x01, (byte) 0x00}, 1078, false));
        return v;
    }

    private static class TestCase {

        private String title;
        private byte[] header;
        private int dataLength;
        private boolean canDecode;

        public TestCase(String title, byte[] header,
                int dataLength, boolean canDecode) {
            this.title = title;
            this.dataLength = dataLength;
            this.header = header.clone();
            this.canDecode = canDecode;

        }

        public void doTest(ImageReaderSpi spi) throws IOException {
            System.out.println("Test for " + title +
                    (canDecode ? " (can decode)" : " (can't decode)"));
            System.out.print("As a stream...");
            ImageInputStream iis =
                    ImageIO.createImageInputStream(getDataStream());

            if (spi.canDecodeInput(iis) != canDecode) {
                throw new RuntimeException("Test failed: wrong decideion " +
                        "for stream data");
            }
            System.out.println("OK");

            System.out.print("As a file...");
            iis = ImageIO.createImageInputStream(getDataFile());
            if (spi.canDecodeInput(iis) != canDecode) {
                throw new RuntimeException("Test failed: wrong decideion " +
                        "for file data");
            }
            System.out.println("OK");
        }

        private byte[] getData() {
            byte[] data = new byte[dataLength];
            Arrays.fill(data, (byte) 0);
            System.arraycopy(header, 0, data, 0, header.length);

            return data;
        }
        public InputStream getDataStream() {
            return new ByteArrayInputStream(getData());
        }

        public File getDataFile() throws IOException {
            File f = File.createTempFile("wbmp_", "." + title, new File("."));
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(getData());
            fos.flush();
            fos.close();

            return f;
        }
    }
}
