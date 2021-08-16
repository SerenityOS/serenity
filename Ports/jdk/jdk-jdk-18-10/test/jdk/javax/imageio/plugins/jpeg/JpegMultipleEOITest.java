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
 * @bug     8152672
 * @summary When jpeg file has more than one set of EOI-SOI markers,
 *          test verifies whether we calculate EOI markers of all images
 *          properly skipping EOI markers present in application headers.
 * @run     main JpegMultipleEOITest
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class JpegMultipleEOITest {
    public static void main (String[] args) throws IOException {
        Iterator readers = ImageIO.getImageReadersByFormatName("JPEG");
        ImageReader reader = null;
        while(readers.hasNext()) {
            reader = (ImageReader)readers.next();
            if(reader.canReadRaster()) {
                break;
            }
        }

        if (reader != null) {
            String fileName = "JpegMultipleEOI.jpg";
            String sep = System.getProperty("file.separator");
            String dir = System.getProperty("test.src", ".");
            String filePath = dir+sep+fileName;
            System.out.println("Test file: " + filePath);
            File imageFile = new File(filePath);
            ImageInputStream stream = ImageIO.
                createImageInputStream(imageFile);
            reader.setInput(stream);
            int pageNum = 1;
            try {
                // read width of image index 1
                reader.getWidth(pageNum + reader.getMinIndex());
            } catch (IndexOutOfBoundsException e) {
                /*
                 * do nothing, we are supposed to get IndexOutofBoundsException
                 * as number of image is 1 and we are trying to get width of
                 * second image. But we should not see IIOException with
                 * message "Not a JPEG file: starts with 0xff 0xe2"
                 */
            }
        }
    }
}
