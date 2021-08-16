/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4843895
 * @summary Tests that we handle raster with non-zero minX and minY correctly
 * @modules java.desktop/com.sun.imageio.plugins.jpeg
 */

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class RasterWithMinXTest {

    public static void main(String[] args) {
        String format = "jpeg";

        // Set output file.
        ImageOutputStream output = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        // Create image.
        BufferedImage bi = new BufferedImage(256, 256,
                                             BufferedImage.TYPE_3BYTE_BGR);

        // Populate image.
        int[] rgbArray = new int[256];
        for(int i = 0; i < 256; i++) {
            Arrays.fill(rgbArray, i);
            bi.setRGB(0, i, 256, 1, rgbArray, 0, 256);
        }

        // create translated raster in order to get non-zero minX and minY
        WritableRaster r = (WritableRaster)bi.getRaster().createTranslatedChild(64,64);

        Iterator i =  ImageIO.getImageWritersByFormatName(format);
        ImageWriter iw = null;
        while(i.hasNext() && iw == null) {
            Object o = i.next();
            if (o instanceof com.sun.imageio.plugins.jpeg.JPEGImageWriter) {
                iw = (ImageWriter)o;
            }
        }
        if (iw == null) {
            throw new RuntimeException("No available image writer");
        }

         ImageWriteParam iwp = iw.getDefaultWriteParam();
         IIOMetadata metadata = iw.getDefaultImageMetadata(new ImageTypeSpecifier(bi.getColorModel(), r.getSampleModel()), iwp);

         IIOImage img = new IIOImage(r, null, metadata);

         iw.setOutput(output);
         try {
             iw.write(img);
         } catch (RasterFormatException e) {
             e.printStackTrace();
             throw new RuntimeException("RasterException occurs. Test Failed!");
         } catch (Exception ex) {
             ex.printStackTrace();
             throw new RuntimeException("Unexpected Exception");
         }

         // test case of theImageWriteParam with non-null sourceRegion
         iwp.setSourceRegion(new Rectangle(32,32,192,192));
         metadata = iw.getDefaultImageMetadata(new ImageTypeSpecifier(bi.getColorModel(), r.getSampleModel()), iwp);
         try {
             iw.write(metadata, img, iwp);
         } catch (RasterFormatException e) {
             e.printStackTrace();
             throw new RuntimeException("SetSourceRegion causes the RasterException. Test Failed!");
         } catch (Exception ex) {
             ex.printStackTrace();
             throw new RuntimeException("Unexpected Exception");
         }

    }
}
