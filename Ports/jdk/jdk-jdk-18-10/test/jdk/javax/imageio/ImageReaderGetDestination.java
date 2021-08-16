/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4449211
 * @summary Checks that ImageReader.getDestination throws correct exceptions
 */

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

public class ImageReaderGetDestination {

    public static void main(String argv[]) {
        Vector imageTypes = new Vector();
        boolean gotIAE1 = false;
        boolean gotIAE2 = false;
        boolean gotIAE3 = false;
        boolean gotIAE4 = false;

        try {
            DummyImageReaderImpl.getDestination(null, null, 5, 10);
        } catch (IllegalArgumentException iae) {
            gotIAE1 = true;
        } catch (Throwable ee) {
            System.out.println("Unexpected exception 1:");
            ee.printStackTrace();
        }
        if (!gotIAE1) {
            throw new RuntimeException("Failed to get IAE #1!");
        }

        try {
            DummyImageReaderImpl.getDestination(null, imageTypes.iterator(),
                                                5, 10);
        } catch (IllegalArgumentException iae) {
            gotIAE2 = true;
        } catch (Throwable ee) {
            System.out.println("Unexpected exception 2:");
            ee.printStackTrace();
        }
        if (!gotIAE2) {
            throw new RuntimeException("Failed to get IAE #2!");
        }

        imageTypes.add("abc");
        try {
            DummyImageReaderImpl.getDestination(null, imageTypes.iterator(),
                                                5, 10);
        } catch (IllegalArgumentException iae) {
            gotIAE3 = true;
        } catch (Throwable ee) {
            System.out.println("Unexpected exception 3:");
            ee.printStackTrace();
        }
        if (!gotIAE3) {
            throw new RuntimeException("Failed to get IAE #3!");
        }

        imageTypes.clear();
        ImageTypeSpecifier its = ImageTypeSpecifier.createFromBufferedImageType
            (BufferedImage.TYPE_INT_RGB);
        imageTypes.add(its);
        try {
            DummyImageReaderImpl.getDestination(null,
                                                imageTypes.iterator(),
                                                Integer.MAX_VALUE,
                                                Integer.MAX_VALUE);
        } catch (IllegalArgumentException iae) {
            gotIAE4 = true;
        } catch (Throwable ee) {
            System.out.println("Unexpected exception 4: ");
            ee.printStackTrace();
        }
        if (!gotIAE4) {
            throw new RuntimeException("Failed to get IAE #4!");
        }
    }

    public static class DummyImageReaderImpl extends ImageReader {
        public DummyImageReaderImpl(ImageReaderSpi originatingProvider) {
            super(originatingProvider);
        }
        public static BufferedImage getDestination(ImageReadParam param,
                                                   Iterator imageTypes,
                                                   int width,
                                                   int height)
          throws IIOException {
            return ImageReader.getDestination(param, imageTypes, width, height);
        }
        public int getNumImages(boolean allowSearch) throws IOException {return 1;}
        public int getWidth(int imageIndex) throws IOException {return 1;}
        public int getHeight(int imageIndex) throws IOException {return 1;}
        public Iterator getImageTypes(int imageIndex)
          throws IOException {return null;}
        public IIOMetadata getStreamMetadata() throws IOException {return null;}
        public IIOMetadata getImageMetadata(int imageIndex)
          throws IOException {return null;}
        public BufferedImage read(int imageIndex, ImageReadParam param)
          throws IOException {return null;}
    }
}
