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

import java.io.IOException;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class SIMPImageReaderSpi extends ImageReaderSpi {

    private static final String[] FORMATNAMES = {"simp, SIMP"};
    private static final String[] SUFFIXES = {"simp"};
    private static final String[] MIMETYPES = {"image/simp"};

    public SIMPImageReaderSpi() {
        super("J Duke",                   // vendor
              "1.0",                      // version
               FORMATNAMES,               // format names
               SUFFIXES,                  // file suffixes
               MIMETYPES,                 // mimetypes
               "simp.SIMPImageReader",    // reader class name
               new Class<?>[] { ImageInputStream.class }, // input types
               null,              // writer class names. TBD.
               true,              // supports native metadata,
               null,              // [no] native stream metadata format
               null,              // [no] native stream metadata class
               null,              // [no] native extra stream metadata format
               null,              // [no] native extra stream metadata class
               true,              // supports standard metadata,
               "simp_metadata_1.0",       // metadata format name,
               "simp.SIMPMetadataFormat", // metadata format class name
               null,              // [no] extra image metadata format
               null               // [no] extra image metadata format class
         );
         System.out.println("INIT SIMPImageReaderSpi");
    }

    public String getDescription(Locale locale) {
        return "SIMPle Image Format Reader";
    }

    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }
        ImageInputStream stream = (ImageInputStream)source;

        stream.mark();
        try {
             byte[] sig = new byte[4];
             stream.read(sig);
             return sig[0]=='S' && sig[1]=='I' && sig[2]=='M' && sig[3]=='P';
        } finally {
            stream.reset();
        }
    }

    public ImageReader createReaderInstance(Object extension)
        throws IIOException {
        return new SIMPImageReader(this);
    }
}
