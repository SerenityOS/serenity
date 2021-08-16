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

/**
 * @test
 * @bug     8204187
 * @run     main TestWriteARGBJPEG
 * @summary verify JPEG Alpha support is as reported.
 */

import java.io.IOException;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class TestWriteARGBJPEG {
    public static void main(String args[]) throws IOException {

        BufferedImage bi =
            new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // There should be no exception from the next line
    // which internally should be relying on the canEncodeImage
    // method which we'll also test directly.
    boolean ret = ImageIO.write(bi, "jpeg", baos);
    System.out.println("ImageIO.write(..) returned " + ret);

    ImageTypeSpecifier its = new ImageTypeSpecifier(bi);
    Iterator<ImageWriter> writers = ImageIO.getImageWriters(its, "jpeg");
    boolean hasWriter = writers.hasNext();
    // If this can't write it, an exception will be thrown.
    if (writers.hasNext()) {
        System.out.println("A writer was found.");
        ImageWriter iw = writers.next();
        MemoryCacheImageOutputStream mos =
            new MemoryCacheImageOutputStream(baos);
        iw.setOutput(mos);
        iw.write(bi);
    }

    // Now Let's also ask the default JPEG writer's SPI if it
    // can write an ARGB image.
    ImageWriter iw = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriterSpi iwSpi = iw.getOriginatingProvider();
    boolean canEncode = iwSpi.canEncodeImage(bi);
    System.out.println("SPI canEncodeImage returned " + canEncode);

    // Now let's see if it is telling the truth.
    try {
        MemoryCacheImageOutputStream mos =
            new MemoryCacheImageOutputStream(baos);
        iw.setOutput(mos);
        iw.write(bi);
    } catch (IOException e) {
         if (canEncode) {
           throw e;
         }
    }
  }
}
