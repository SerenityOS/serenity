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

package simp;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
  * A simple image format which has no compression and is a
  * header with bytes 'SIMP', two signed bytes for width and height,
  * and then unpadded 3 byte per pixel data representing RGB assumed
  * to be in sRGB. The signed byte implies a maximum size of 127x127 pixels.
  * Trailing data is ignored but there must be at least
  * 3*width*height bytes of data following the simple 6 byte header.
  */
public class SIMPImageReader extends ImageReader {

    private ImageInputStream stream = null;
    private byte width = -1, height = -1;
    SIMPMetadata metadata = null;
    byte[] imageData = null;

    public SIMPImageReader(ImageReaderSpi originatingProvider) {
       super(originatingProvider);
    }

    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        stream = (ImageInputStream) input;
    }

    private void checkState(int imageIndex) throws IOException {
        if (stream == null) {
            throw new IllegalStateException("input not set.");
        }
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("index != 0");
        }
        if (width==-1) {
            byte[] sig = new byte[4];
            stream.reset();
            stream.read(sig);
            boolean ok = sig[0]=='S' && sig[1]=='I' &&
                         sig[2]=='M' && sig[3]=='P';
            if (!ok) {
                throw new IIOException("Not a SIMP image");
            }
            width = stream.readByte();
            height = stream.readByte();
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("bad image size");
        }
        metadata = new SIMPMetadata(width, height);
    }

    public int getWidth(int imageIndex) throws IOException {
        checkState(imageIndex);
        return width;
    }

    public int getHeight(int imageIndex) throws IOException {
        checkState(imageIndex);
        return height;
    }

     public int getNumImages(boolean allowSearch) throws IOException {
        checkState(0);
        return 1;
    }

     public IIOMetadata getStreamMetadata() throws IOException {
        return null;
     }

     public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        checkState(imageIndex);
        return metadata;
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
        throws IOException {

        checkState(imageIndex);
        BufferedImage bi =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ArrayList<ImageTypeSpecifier> list = new ArrayList<>(1);
        list.add(new ImageTypeSpecifier(bi));
        return list.iterator();
    }

    public BufferedImage read(int imageIndex, ImageReadParam param)
                            throws IOException {
        checkState(imageIndex);
        int len = 3*width*height;
        byte[] imageData = new byte[len];
        // The following is not efficient and is skipping all the
        // progress updates, and ignoring the ImageReadParam, which
        // it should not, but it is all we need for this test.
        stream.readFully(imageData, 0, len);
        BufferedImage bi =
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int off = 0;
        for (int h=0;h<height;h++) {
            int rgb = imageData[off]++ << 16 |
                      imageData[off++] <<  8 | imageData[off++];
            for (int w=0;w<width;w++) {
                bi.setRGB(w, h, rgb);
            }
        }
        return bi;
    }
}
