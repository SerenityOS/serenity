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

/*
 * @test
 * @bug     8163258
 * @summary Test verifies that when we create our own ImageReaderSpi
 *          implementaion with MIMEType or FileSuffix as null, it should
 *          not throw NullPointerException when we call
 *          ImageIO.getReaderMIMETypes() or ImageIO.getReaderFileSuffixes().
 * @run     main GetReaderWriterInfoNullTest
 */

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

class TestImageReaderSpi extends ImageReaderSpi {

    public TestImageReaderSpi(String[] FORMATNAMES, String[] SUFFIXES,
                              String[] MIMETYPES) {
        super("J Duke",          // vendor
              "1.0",             // version
              FORMATNAMES,       // format names
              SUFFIXES,          // file suffixes
              MIMETYPES,         // mimetypes
              "readTest.TestImageReader",    // reader class name
              new Class<?>[] { ImageInputStream.class }, // input types
              null,              // writer class names.
              true,              // supports native metadata,
              null,              // [no] native stream metadata format
              null,              // [no] native stream metadata class
              null,              // [no] native extra stream metadata format
              null,              // [no] native extra stream metadata class
              true,              // supports standard metadata,
              null,              // metadata format name,
              null,              // metadata format class name
              null,              // [no] extra image metadata format
              null               // [no] extra image metadata format class
         );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescription(Locale locale) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageReader createReaderInstance(Object extension)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

class TestImageReader extends ImageReader {

    public TestImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
public class GetReaderWriterInfoNullTest {
    static final String[] FORMATNAMES = {"readTest"};
    static final String[] SUFFIXES = {"readTest"};
    static final String[] MIMETYPES = {"readTest"};
    public static void main (String[] args) throws IIOException {
        // Verify getReaderMIMETypes() behavior by keeping MIMEType as null.
        TestImageReaderSpi mimeNullReadSpi =
                new TestImageReaderSpi(FORMATNAMES, SUFFIXES, null);
        IIORegistry.getDefaultInstance().
                registerServiceProvider(mimeNullReadSpi);
        ImageIO.getReaderMIMETypes();
        IIORegistry.getDefaultInstance().
                deregisterServiceProvider(mimeNullReadSpi);

        /*
         * Verify getReaderFileSuffixes() behavior by keeping
         * file suffix as null.
         */
        TestImageReaderSpi suffixNullReadSpi =
                new TestImageReaderSpi(FORMATNAMES, null, MIMETYPES);
        IIORegistry.getDefaultInstance().
                registerServiceProvider(suffixNullReadSpi);
        ImageIO.getReaderFileSuffixes();
        IIORegistry.getDefaultInstance().
                deregisterServiceProvider(suffixNullReadSpi);
    }
}

