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
 * @bug     8152966
 * @summary Verify that casting of TIFFDirectory to TIFFIFD has been fixed.
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;


import javax.imageio.plugins.tiff.*;


public class IFDTest {

    private ImageWriter getTIFFWriter() {

        java.util.Iterator<ImageWriter> writers =
            ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new RuntimeException("No readers available for TIFF format");
        }
        return writers.next();
    }

    private void writeImage() throws Exception {

        File file = File.createTempFile("IFDTest", "tif", new File("."));
        file.deleteOnExit();

        OutputStream s = new BufferedOutputStream(
            new FileOutputStream("test.tiff"));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(s)) {

            ImageWriter writer = getTIFFWriter();
            writer.setOutput(ios);

            BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
            Graphics g = img.getGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 20, 20);
            g.dispose();

            IIOMetadata metadata = writer.getDefaultImageMetadata(
                new ImageTypeSpecifier(img), writer.getDefaultWriteParam());

            TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

            int type = TIFFTag.TIFF_IFD_POINTER;
            int nTag = ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER;
            TIFFTag tag = new TIFFTag("Exif IFD", nTag, 1 << type);
            TIFFTagSet sets[] = {ExifTIFFTagSet.getInstance()};

            TIFFField f = new TIFFField(tag, type, 42L, new TIFFDirectory(sets, tag));
            dir.addTIFFField(f);

            writer.write(new IIOImage(img, null, dir.getAsMetadata()));

            ios.flush();
            writer.dispose();
        } finally {
            s.close();
            file.delete();
        }
    }


    public static void main(String[] args) throws Exception {
        (new IFDTest()).writeImage();
    }
}
