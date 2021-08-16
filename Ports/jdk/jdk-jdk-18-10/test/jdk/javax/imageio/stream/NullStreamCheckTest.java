/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8044289
 * @summary Test verifies that when some of the read() and write() methods
 *          are not able to get stream from createImageInputStream() and
 *          createImageOutputStream() are we doing null check for stream
 *          and throwing IOException as per specification.
 * @run     main NullStreamCheckTest
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageOutputStreamSpi;

public class NullStreamCheckTest {

    // get ImageIORegistry default instance.
    private static final IIORegistry localRegistry = IIORegistry.
            getDefaultInstance();
    // stream variables needed for input and output.
    static LocalOutputStream outputStream = new LocalOutputStream();
    static LocalInputStream inputStream = new LocalInputStream();

    static final int width = 50, height = 50;

    // input and output BufferedImage needed while read and write.
    static BufferedImage inputImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

    /* if we catch expected IOException message return
     * false otherwise return true.
     */
    private static boolean verifyOutputExceptionMessage(IOException ex) {
        String message = ex.getMessage();
        return (!message.equals("Can't create an ImageOutputStream!"));
    }

    /* if we catch expected IOException message return
     * false otherwise return true.
     */
    private static boolean verifyInputExceptionMessage(IOException ex) {
        String message = ex.getMessage();
        return (!message.equals("Can't create an ImageInputStream!"));
    }

    private static void verifyFileWrite() throws IOException {
        File outputTestFile = File.createTempFile("outputTestFile", ".png");
        try {
            ImageIO.write(inputImage, "png", outputTestFile);
        } catch (IOException ex) {
            if (verifyOutputExceptionMessage(ex))
                throw ex;
        } finally {
            outputTestFile.delete();
        }
    }

    private static void verifyStreamWrite() throws IOException {
        try {
            ImageIO.write(inputImage, "png", outputStream);
        } catch (IOException ex) {
            if (verifyOutputExceptionMessage(ex))
                throw ex;
        } finally {
            try {
                outputStream.close();
            } catch (IOException ex) {
                throw ex;
            }
        }
    }

    private static void verifyFileRead() throws IOException {
        File inputTestFile = File.createTempFile("inputTestFile", ".png");
        try {
            ImageIO.read(inputTestFile);
        } catch (IOException ex) {
            if (verifyInputExceptionMessage(ex))
                throw ex;
        } finally {
            inputTestFile.delete();
        }
    }

    private static void verifyStreamRead() throws IOException {
        try {
            ImageIO.read(inputStream);
        } catch (IOException ex) {
            if (verifyInputExceptionMessage(ex))
                throw ex;
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                throw ex;
            }
        }
    }

    private static void verifyUrlRead() throws IOException {
        URL url;
        File inputTestUrlFile = File.createTempFile("inputTestFile", ".png");
        try {
            try {
                url = inputTestUrlFile.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw ex;
            }

            try {
                ImageIO.read(url);
            } catch (IOException ex) {
                if (verifyInputExceptionMessage(ex))
                    throw ex;
            }
        } finally {
            inputTestUrlFile.delete();
        }
    }

    public static void main(String[] args) throws IOException,
                                                  MalformedURLException {

        /* deregister ImageOutputStreamSpi so that we creatImageOutputStream
         * returns null while writing.
         */
        localRegistry.deregisterAll(ImageOutputStreamSpi.class);
        /* verify possible ImageIO.write() scenario's for null stream output
         * from createImageOutputStream() API in ImageIO class.
         */
        verifyFileWrite();
        verifyStreamWrite();

        /* deregister ImageInputStreamSpi so that we creatImageInputStream
         * returns null while reading.
         */
        localRegistry.deregisterAll(ImageInputStreamSpi.class);
        /* verify possible ImageIO.read() scenario's for null stream output
         * from createImageInputStream API in ImageIO class.
         */
        verifyFileRead();
        verifyStreamRead();
        verifyUrlRead();
    }

    static class LocalOutputStream extends OutputStream {

        @Override
        public void write(int i) throws IOException {
        }
    }

    static class LocalInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return 0;
        }
    }
}
