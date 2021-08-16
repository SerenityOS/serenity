/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4393174
 * @summary Checks that ImageIO.write(..., ..., File) truncates the file
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

public class ImageIOWriteFile {

    public static void main(String[] args) {
        long length0 = -1L;
        long length1 = -1L;

        try {
            BufferedImage bi =
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

            File outFile = File.createTempFile("imageiowritefile", ".tmp");

            // Write image to an empty file
            outFile.delete();
            ImageIO.write(bi, "png", outFile);
            length0 = outFile.length();

            // Write a larger file full of junk
            outFile.delete();
            FileOutputStream fos = new FileOutputStream(outFile);
            for (int i = 0; i < length0*2; i++) {
                fos.write(1);
            }
            fos.close();

            // Write image again
            ImageIO.write(bi, "png", outFile);
            length1 = outFile.length();

            outFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected exception!");
        }

        if (length0 == 0) {
            throw new RuntimeException("File length is zero!");
        }
        if (length1 != length0) {
            throw new RuntimeException("File length changed!");
        }
    }
}
