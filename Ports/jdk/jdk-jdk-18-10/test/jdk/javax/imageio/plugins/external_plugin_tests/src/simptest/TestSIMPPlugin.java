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

package simptest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

public class TestSIMPPlugin {

    static byte[] simpData = { (byte)'S', (byte)'I', (byte)'M', (byte)'P',
                               1, 1, 0, 0, 0};

    public static void main(String args[]) throws Exception {
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("simp");
        ImageReader simpReader = null;
        if (readers.hasNext()) {
            simpReader = readers.next();
            System.out.println("reader="+simpReader);
        }
        if (simpReader == null) {
            throw new RuntimeException("Reader not found.");
        }

        ImageReaderSpi spi = simpReader.getOriginatingProvider();
        IIOMetadataFormat spiFmt =
            spi.getImageMetadataFormat("simp_metadata_1.0");
        System.out.println("fmt from SPI=" + spiFmt);

        ByteArrayInputStream bais = new ByteArrayInputStream(simpData);
        ImageInputStream iis = new MemoryCacheImageInputStream(bais);
        simpReader.setInput(iis);
        BufferedImage bi = simpReader.read(0);
        System.out.println(bi);
        IIOMetadata metadata = simpReader.getImageMetadata(0);
        System.out.println("Image metadata="+metadata);
        IIOMetadataFormat format =
            metadata.getMetadataFormat("simp_metadata_1.0");
        System.out.println("Image metadata format="+format);
        if (format == null) {
            throw new RuntimeException("MetadataFormat not found.");
        }
    }
}
