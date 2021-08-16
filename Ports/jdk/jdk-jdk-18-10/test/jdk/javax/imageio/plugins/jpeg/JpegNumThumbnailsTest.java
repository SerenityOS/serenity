/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4954348
 * @summary Checks whether JpegImageWriter returns -1 as recommended by the
 *          specification when getNumThumbnailsSupported method is invoked
 *          with insufficient data.
 * @run main JpegNumThumbnailsTest
 */
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class JpegNumThumbnailsTest {

    public static void main(String args[]) {
        // Test variables.
        Iterator<ImageWriter> iterWriter = null;
        ImageWriter jpgWriter = null;
        IIOMetadata imgMetadata = null;
        BufferedImage testImage = null;
        ImageTypeSpecifier imgType = null;
        int numThumbnails = 0;

        iterWriter = ImageIO.getImageWritersByFormatName("JPEG");
        if (iterWriter.hasNext()) {
            try {
                // JpegImageWriter requires either image type or image metadata
                // to determine the number of thumbnails that could be
                // supported. Hence we test for all possible input combinations
                // and observe the result.
                jpgWriter = iterWriter.next();
                testImage = new BufferedImage(32, 32, TYPE_INT_RGB);
                imgType = ImageTypeSpecifier.createFromRenderedImage(testImage);
                imgMetadata = jpgWriter.getDefaultImageMetadata(imgType, null);

                // Observe the result with insufficient data.
                numThumbnails = jpgWriter.getNumThumbnailsSupported(null,
                                        null, null, null);
                if (numThumbnails != -1) {
                    reportException("Incorrect number of thumbnails returned.");
                }

                // Observe the result with valid image type.
                numThumbnails = jpgWriter.getNumThumbnailsSupported(imgType,
                                        null, null, null);
                if (numThumbnails != Integer.MAX_VALUE) {
                    reportException("Incorrect number of thumbnails returned.");
                }

                // Observe the result with valid image metadata.
                numThumbnails = jpgWriter.getNumThumbnailsSupported(null,
                                        null, null, imgMetadata);
                if (numThumbnails != Integer.MAX_VALUE) {
                    reportException("Incorrect number of thumbnails returned.");
                }

                // Observe the result with valid image type and metadata.
                numThumbnails = jpgWriter.getNumThumbnailsSupported(imgType,
                                        null, null, imgMetadata);
                if (numThumbnails != Integer.MAX_VALUE) {
                    reportException("Incorrect number of thumbnails returned.");
                }
            } finally {
                // Dispose the writer
                jpgWriter.dispose();
            }
        }
    }

    private static void reportException(String message) {
        // Report a runtime exception with the required message.
        throw new RuntimeException("Test Failed. " + message);
    }
}
