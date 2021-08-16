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

/**
 * @test
 * @bug     8022632
 * @summary Test verifies that SPI of WBMP image reader
 *           restores the stream position if an IOException
 *           occurs during processing of image header.
 * @run     main StreamResetTest
 */


import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStreamImpl;

public class StreamResetTest {

    public static void main(String[] args) {
        IOException expectedException = null;
        TestStream iis = new TestStream();

        ImageReader wbmp = ImageIO.getImageReadersByFormatName("WBMP").next();
        if (wbmp == null) {
            System.out.println("No WBMP reader: skip the test");
            return;
        }

        ImageReaderSpi spi = wbmp.getOriginatingProvider();

        iis.checkPosition();

        try {
            spi.canDecodeInput(iis);
        } catch (IOException e) {
            expectedException = e;
        }

        if (expectedException == null) {
            throw new RuntimeException("Test FAILED: stream was not used");
        }

        iis.checkPosition();

        System.out.println("Test PASSED");

    }

    private static class TestStream extends ImageInputStreamImpl {
        private final int errorPos = 1;

        @Override
        public int read() throws IOException {
            if (streamPos == errorPos) {
                throw new IOException("Test exception");
            }
            streamPos++;

            return 0x03;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            streamPos += len;
            return len;
        }

        public void checkPosition() {
            if (streamPos != 0) {
                throw new RuntimeException("Test FAILED");
            }
        }
    }
}
