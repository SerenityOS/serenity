/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.sun.imageio.plugins.tiff;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

public class TIFFImageReaderSpi extends ImageReaderSpi {

    private boolean registered = false;

    public TIFFImageReaderSpi() {
        super("Oracle Corporation",
              "1.0",
              new String[] {"tif", "TIF", "tiff", "TIFF"},
              new String[] {"tif", "tiff"},
              new String[] {"image/tiff"},
              "com.sun.imageio.plugins.tiff.TIFFImageReader",
              new Class<?>[] { ImageInputStream.class },
              new String[] {"com.sun.imageio.plugins.tiff.TIFFImageWriterSpi"},
              false,
              TIFFStreamMetadata.NATIVE_METADATA_FORMAT_NAME,
              "com.sun.imageio.plugins.tiff.TIFFStreamMetadataFormat",
              null, null,
              true,
              TIFFImageMetadata.NATIVE_METADATA_FORMAT_NAME,
              "com.sun.imageio.plugins.tiff.TIFFImageMetadataFormat",
              null, null
              );
    }

    public String getDescription(Locale locale) {
        return "Standard TIFF image reader";
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream)input;
        byte[] b = new byte[4];
        stream.mark();
        stream.readFully(b);
        stream.reset();

        return ((b[0] == (byte)0x49 && b[1] == (byte)0x49 &&
                 b[2] == (byte)0x2a && b[3] == (byte)0x00) ||
                (b[0] == (byte)0x4d && b[1] == (byte)0x4d &&
                 b[2] == (byte)0x00 && b[3] == (byte)0x2a));
    }

    public ImageReader createReaderInstance(Object extension) {
        return new TIFFImageReader(this);
    }

    public void onRegistration(ServiceRegistry registry,
                               Class<?> category) {
        if (registered) {
            return;
        }

        registered = true;
    }
}
