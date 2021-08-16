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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;

import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import javax.imageio.IIOImage;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * @test
 * @bug 8144245
 * @summary Ensure aborting write works properly for a TIFF sequence.
 */
public final class WriteToSequenceAfterAbort implements IIOWriteProgressListener {

    private volatile boolean abortFlag = true;
    private volatile boolean isAbortCalled;
    private volatile boolean isCompleteCalled;
    private volatile boolean isProgressCalled;
    private volatile boolean isStartedCalled;
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final int NUM_TILES_XY = 3;

    private class TiledImage implements RenderedImage {
        private final BufferedImage tile;
        private final BufferedImage image;
        private final int numXTiles, numYTiles;
        private boolean isImageInitialized = false;

        TiledImage(BufferedImage tile, int numXTiles, int numYTiles) {
            this.tile = tile;
            this.numXTiles = numXTiles;
            this.numYTiles = numYTiles;
            image = new BufferedImage(getWidth(), getHeight(), tile.getType());
        }

        @Override
        public Vector<RenderedImage> getSources() {
            return null;
        }

        @Override
        public Object getProperty(String string) {
            return java.awt.Image.UndefinedProperty;
        }

        @Override
        public String[] getPropertyNames() {
            return new String[0];
        }

        @Override
        public ColorModel getColorModel() {
            return tile.getColorModel();
        }

        @Override
        public SampleModel getSampleModel() {
            return tile.getSampleModel();
        }

        @Override
        public int getWidth() {
            return numXTiles*tile.getWidth();
        }

        @Override
        public int getHeight() {
            return numYTiles*tile.getHeight();
        }

        @Override
        public int getMinX() {
            return 0;
        }

        @Override
        public int getMinY() {
            return 0;
        }

        @Override
        public int getNumXTiles() {
            return numXTiles;
        }

        @Override
        public int getNumYTiles() {
            return numYTiles;
        }

        @Override
        public int getMinTileX() {
            return 0;
        }

        @Override
        public int getMinTileY() {
            return 0;
        }

        @Override
        public int getTileWidth() {
            return tile.getWidth();
        }

        @Override
        public int getTileHeight() {
            return tile.getHeight();
        }

        @Override
        public int getTileGridXOffset() {
            return 0;
        }

        @Override
        public int getTileGridYOffset() {
            return 0;
        }

        @Override
        public Raster getTile(int x, int y) {
            WritableRaster r = tile.getRaster();
            return r.createWritableTranslatedChild(x*tile.getWidth(),
                y*tile.getHeight());
        }

        @Override
        public Raster getData() {
            return getAsBufferedImage().getData();
        }

        @Override
        public Raster getData(Rectangle r) {
            return getAsBufferedImage().getData(r);
        }

        @Override
        public WritableRaster copyData(WritableRaster wr) {
            return getAsBufferedImage().copyData(wr);
        }

        public BufferedImage getAsBufferedImage() {
            synchronized (image) {
                if (!isImageInitialized) {
                    int tx0 = getMinTileX(), ty0 = getMinTileY();
                    int txN = tx0 + getNumXTiles(), tyN = ty0 + getNumYTiles();
                    for (int j = ty0; j < tyN; j++) {
                        for (int i = tx0; i < txN; i++) {
                            image.setData(getTile(i, j));
                        }
                    }
                }
                isImageInitialized = true;
            }
            return image;
        }
    }

    private void test(final ImageWriter writer) throws IOException {
        String suffix = writer.getOriginatingProvider().getFileSuffixes()[0];

        // Image initialization
        BufferedImage imageUpperLeft =
                new BufferedImage(WIDTH, HEIGHT, TYPE_BYTE_BINARY);
        Graphics2D g = imageUpperLeft.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH/2, HEIGHT/2);
        g.dispose();
        BufferedImage imageLowerRight =
                new BufferedImage(WIDTH, HEIGHT, TYPE_BYTE_BINARY);
        g = imageLowerRight.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(WIDTH/2, HEIGHT/2, WIDTH/2, HEIGHT/2);
        g.dispose();
        TiledImage[] images = new TiledImage[] {
            new TiledImage(imageUpperLeft, NUM_TILES_XY, NUM_TILES_XY),
            new TiledImage(imageUpperLeft, NUM_TILES_XY, NUM_TILES_XY),
            new TiledImage(imageLowerRight, NUM_TILES_XY, NUM_TILES_XY),
            new TiledImage(imageLowerRight, NUM_TILES_XY, NUM_TILES_XY)
        };

        // File initialization
        File file = File.createTempFile("temp", "." + suffix);
        file.deleteOnExit();
        FileOutputStream fos = new SkipWriteOnAbortOutputStream(file);
        ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
        writer.setOutput(ios);
        writer.addIIOWriteProgressListener(this);

        writer.prepareWriteSequence(null);
        boolean[] abortions = new boolean[] {true, false, true, false};
        for (int i = 0; i < 4; i++) {
            abortFlag = abortions[i];
            isAbortCalled = false;
            isCompleteCalled = false;
            isProgressCalled = false;
            isStartedCalled = false;

            TiledImage image = images[i];
            if (abortFlag) {
                // This write will be aborted, and file will not be touched
                writer.writeToSequence(new IIOImage(image, null, null), null);
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
            } else {
                // This write should be completed successfully and the file should
                // contain correct image data.
                writer.writeToSequence(new IIOImage(image, null, null), null);
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
            }
        }

        writer.endWriteSequence();
        writer.dispose();
        ios.close();

        // Validates content of the file.
        ImageReader reader = ImageIO.getImageReader(writer);
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        reader.setInput(iis);
        for (int i = 0; i < 2; i++) {
            System.out.println("Testing image " + i);
            BufferedImage imageRead = reader.read(i);
            BufferedImage imageWrite = images[2 * i].getAsBufferedImage();
            for (int x = 0; x < WIDTH; ++x) {
                for (int y = 0; y < HEIGHT; ++y) {
                    if (imageRead.getRGB(x, y) != imageWrite.getRGB(x, y)) {
                        throw new RuntimeException("Test failed for image " + i);
                    }
                }
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        WriteToSequenceAfterAbort writeAfterAbort = new WriteToSequenceAfterAbort();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("TIFF").next();
        writeAfterAbort.test(writer);
        System.out.println("Test passed.");
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

