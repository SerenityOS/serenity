/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;

/**
 * @test
 * @bug     4952954 8183349
 * @summary abortFlag must be cleared for every ImageWriter.write operation
 * @run     main WriteAfterAbort
 */
public final class WriteAfterAbort implements IIOWriteProgressListener {

    private volatile boolean abortFlag = true;
    private volatile boolean isAbortCalled;
    private volatile boolean isCompleteCalled;
    private volatile boolean isProgressCalled;
    private volatile boolean isStartedCalled;
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static FileOutputStream fos;
    private static File file;

    private void test(final ImageWriter writer) throws IOException {
        try {
            // Image initialization
            final BufferedImage imageWrite =
                    new BufferedImage(WIDTH, HEIGHT, TYPE_BYTE_BINARY);
            final Graphics2D g = imageWrite.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.dispose();

            // File initialization
            file = File.createTempFile("temp", ".img");
            fos = new SkipWriteOnAbortOutputStream(file);
            final ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(this);

            // This write will be aborted, and file will not be touched
            writer.write(imageWrite);
            if (!isStartedCalled) {
                throw new RuntimeException("Started should be called");
            }
            if (!isProgressCalled) {
                throw new RuntimeException("Progress should be called");
            }
            if (!isAbortCalled) {
                throw new RuntimeException("Abort should be called");
            }
            if (isCompleteCalled) {
                throw new RuntimeException("Complete should not be called");
            }
            // Flush aborted data
            ios.flush();

            /*
             * This write should be completed successfully and the file should
             * contain correct image data.
             */
            abortFlag = false;
            isAbortCalled = false;
            isCompleteCalled = false;
            isProgressCalled = false;
            isStartedCalled = false;
            writer.write(imageWrite);

            if (!isStartedCalled) {
                throw new RuntimeException("Started should be called");
            }
            if (!isProgressCalled) {
                throw new RuntimeException("Progress should be called");
            }
            if (isAbortCalled) {
                throw new RuntimeException("Abort should not be called");
            }
            if (!isCompleteCalled) {
                throw new RuntimeException("Complete should be called");
            }
            ios.close();

            // Validates content of the file.
            final BufferedImage imageRead = ImageIO.read(file);
            for (int x = 0; x < WIDTH; ++x) {
                for (int y = 0; y < HEIGHT; ++y) {
                    if (imageRead.getRGB(x, y) != imageWrite.getRGB(x, y)) {
                        throw new RuntimeException("Test failed.");
                    }
                }
            }
        } finally {
            writer.dispose();
            if (file != null) {
                if (fos != null) {
                    fos.close();
                }
                Files.delete(file.toPath());
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        final Iterator<ImageWriterSpi> iter = registry.getServiceProviders(
                ImageWriterSpi.class, provider -> true, true);

        // Validates all supported ImageWriters
        int numFailures = 0;
        while (iter.hasNext()) {
            final WriteAfterAbort writeAfterAbort = new WriteAfterAbort();
            final ImageWriter writer = iter.next().createWriterInstance();
            System.out.println("ImageWriter = " + writer);
            try {
                writeAfterAbort.test(writer);
            } catch (Exception e) {
                System.err.println("Test failed for \""
                    + writer.getOriginatingProvider().getFormatNames()[0]
                    + "\" format.");
                numFailures++;
            }
        }
        if (numFailures == 0) {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed.");
        }
    }

    // Callbacks

    @Override
    public void imageComplete(ImageWriter source) {
        isCompleteCalled = true;
    }

    @Override
    public void imageProgress(ImageWriter source, float percentageDone) {
        isProgressCalled = true;
        if (percentageDone > 50 && abortFlag) {
            source.abort();
        }
    }

    @Override
    public void imageStarted(ImageWriter source, int imageIndex) {
        isStartedCalled = true;
    }

    @Override
    public void writeAborted(final ImageWriter source) {
        isAbortCalled = true;
    }

    @Override
    public void thumbnailComplete(ImageWriter source) {
    }

    @Override
    public void thumbnailProgress(ImageWriter source, float percentageDone) {
    }

    @Override
    public void thumbnailStarted(ImageWriter source, int imageIndex,
                                 int thumbnailIndex) {
    }

    /**
     * We need to skip writes on abort, because content of the file after abort
     * is undefined.
     */
    private class SkipWriteOnAbortOutputStream extends FileOutputStream {

        SkipWriteOnAbortOutputStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void write(int b) throws IOException {
            if (!abortFlag) {
                super.write(b);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (!abortFlag) {
                super.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!abortFlag) {
                super.write(b, off, len);
            }
        }
    }
}

