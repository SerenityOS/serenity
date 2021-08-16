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
  * @bug     6294607
  * @summary Test verifies whether ImageWriteParam.getCompressionTypes()
  *          returns any duplicate compression type for ImageIO plugins.
  * @run     main ImageCompressionTypesTest
  */

import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

public class ImageCompressionTypesTest {

    static ImageWriter writer = null;

    public ImageCompressionTypesTest(String format) {
        Iterator it = ImageIO.getImageWritersByFormatName(format);
        while (it.hasNext()) {
            writer = (ImageWriter) it.next();
            break;
        }
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        System.out.println("Checking compression types for : " + format);
        String compTypes[] = param.getCompressionTypes();
        if (compTypes.length > 1) {
            for (int i = 0; i < compTypes.length; i++) {
                for (int j = i + 1; j < compTypes.length; j++) {
                    if (compTypes[i].equalsIgnoreCase(compTypes[j])) {
                        throw new RuntimeException("Duplicate compression"
                                + " type exists for image format " + format);
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        final String[] formats = {"bmp", "png", "gif", "jpg", "tiff"};
        for (String format : formats) {
            new ImageCompressionTypesTest(format);
        }
    }
}

