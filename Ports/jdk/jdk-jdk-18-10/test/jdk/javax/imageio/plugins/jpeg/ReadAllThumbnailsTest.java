/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4958271 8160943
 * @summary This test verifies that ImageReader.readAll() method is able to read
 *          all the thumbnails present in the input source without throwing any
 *          exception while reading through Jpeg data.
 * @run     main ReadAllThumbnailsTest
 */

import java.util.Iterator;
import java.io.File;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ReadAllThumbnailsTest {

    public ReadAllThumbnailsTest() {

        try {
            ImageReader reader = null;

            String fileName = "thumbimg.jpg";
            String sep = System.getProperty("file.separator");
            String dir = System.getProperty("test.src", ".");
            String filePath = dir+sep+fileName;
            System.out.println("Test file: " + filePath);
            File f = new File(filePath);

            ImageInputStream iis = ImageIO.createImageInputStream(f);
            Iterator readerIt = ImageIO.getImageReaders(iis);
            if (readerIt.hasNext()) {
                reader = (ImageReader) readerIt.next();
            }

            if (reader == null) {
                error("FAIL: Reader is not available for reading a " +
                      "JPG image with thumbnails. Test Aborted !!");
            }

            reader.setInput(iis);

            if (!reader.readerSupportsThumbnails()) {
                error("FAIL: JPG Reader fails to support thumbnails."
                        + " Test aborted !!");
            }

            int numThumbnails = reader.getNumThumbnails(0);
            if (numThumbnails <= 0) {
                error(" FAIL: Reader.getNumThumbnails() returns 0 when the " +
                      "input image contains some thumbnails");
            }
            IIOImage iioImg = reader.readAll(0, null);
            int thumbnailsRead = iioImg.getNumThumbnails();

            if (numThumbnails == thumbnailsRead) {
                System.out.println("PASS: Thumbnails are read properly by"
                        + " ImageReader.readAll(index, readParam) ");
            } else {
                error("FAIL: Some of the thumbnails are not read" +
                      " from the input source when calling" +
                      " ImageReader.readAll(index, readParam) ");
            }

            iis = ImageIO.createImageInputStream(f);
            reader.setInput(iis);

            iioImg = null;
            Iterator imgIter = reader.readAll(null);
            if (imgIter.hasNext()) {
                iioImg = (IIOImage) imgIter.next();
                thumbnailsRead = iioImg.getNumThumbnails();

                if (numThumbnails == thumbnailsRead) {
                    System.out.println("PASS: Thumbnails are read properly by"
                            + " ImageReader.readAll(Iter)");
                } else {
                    error("FAIL: Some of the thumbnails are not read " +
                          "from the input source when calling"
                            + " ImageReader.readAll(Iter)");
                }
            } else {
                error("FAIL: ImageReader.readAll(Iter) fails to read the image"
                        + " & thumbnails from the input source");
            }

        } catch (Exception e) {
            error(" FAIL: The following exception is thrown by " +
                  "ImageReader.readAll() method when input source contains " +
                  "some thumbnails. Exception: " + e.toString());
        }

    }

    public final void error(String mesg) {
        throw new RuntimeException(mesg);
    }

    public static void main(String args[]) {
        ReadAllThumbnailsTest test = new ReadAllThumbnailsTest();
    }
}
