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
 * @bug 4954545
 * @summary This test verifies whether the ImageWriter implementations throw
 *          IllegalArgumentException when a null image is passed to the write
 *          method. This is tested for all the image writers available with the
 *          IIORegistry.
 */

import java.io.FileOutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public class WriteNullImageTest {

    public WriteNullImageTest() {
        boolean testFailed = false;
        String failMsg = "FAIL: IllegalArgumentException is not thrown by the " +
            "ImageWriter when the image passed to the write() method is " +
            "null, for the image formats: ";

        try {
            IIORegistry reg = IIORegistry.getDefaultInstance();
            ImageWriter writer = null;
            Iterator writerSpiIter = reg.getServiceProviders(ImageWriterSpi.class, true);

            while (writerSpiIter.hasNext()) {
                ImageWriterSpi writerSpi = (ImageWriterSpi) writerSpiIter.next();
                writer = writerSpi.createWriterInstance();
                String names[] = writerSpi.getFormatNames();

                FileOutputStream fos = new FileOutputStream("temp");
                ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
                writer.setOutput(ios);

                try {
                    writer.write(null, null, null);
                } catch (IllegalArgumentException iae) {
                    System.out.println("PASS: Expected exception is thrown when null img is passed " +
                                       "to the write method, for the image format: " + names[0]);
                    System.out.println("\n");
                } catch (Exception e) {
                    testFailed = true;
                    failMsg = failMsg + names[0] + ", ";
                }
            }

        } catch (Exception e) {
            testFailed = true;
            throw new RuntimeException("Test Failed. Exception thrown: " + e.toString());
        }
        if (testFailed) {
            failMsg = failMsg.substring(0, failMsg.lastIndexOf(","));
            throw new RuntimeException(failMsg);
        }
    }

    public static void main(String args[]) {
        WriteNullImageTest test = new WriteNullImageTest();
    }
}
