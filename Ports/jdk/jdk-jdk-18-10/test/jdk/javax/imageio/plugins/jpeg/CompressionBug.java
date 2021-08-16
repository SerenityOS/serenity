/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4415068 4622201
 * @summary Tests if the JPEG writer responds to the compression quality setting
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class CompressionBug {

    public CompressionBug() throws IOException {
        File fileHighComp = File.createTempFile("CompressionHigh", ".jpg");
        File fileLowComp = File.createTempFile("CompressionLow", ".jpg");

        fileHighComp.deleteOnExit();
        fileLowComp.deleteOnExit();

        ImageOutputStream iosHighComp =
            ImageIO.createImageOutputStream(fileHighComp);
        ImageOutputStream iosLowComp =
            ImageIO.createImageOutputStream(fileLowComp);

        int width = 100;
        int height = 100;
        BufferedImage bi =
            new BufferedImage(width, height,
                              BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            Color c = new Color(r.nextInt(256),
                                r.nextInt(256),
                                r.nextInt(256));
            int x = r.nextInt(width);
            int y = r.nextInt(height);
            int w = r.nextInt(width - x);
            int h = r.nextInt(height - y);
            g.setColor(c);
            g.fillRect(x, y, w, h);
        }

        ImageTypeSpecifier typeSpecifier =
            new ImageTypeSpecifier(bi.getColorModel(),
                                   bi.getSampleModel());

        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWriters(typeSpecifier,"jpeg");
        while (iter.hasNext()) {
            writer = (ImageWriter)iter.next();
            break;
        }

        IIOImage iioImg = new IIOImage(bi, null, null);
        ImageWriteParam wParam = writer.getDefaultWriteParam();
        wParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        // write the highly compressed image (a compression quality setting of
        // 0.1f means low visual quality and small file size)
        wParam.setCompressionQuality(0.1f);
        writer.setOutput(iosHighComp);
        writer.write(null, iioImg, wParam);

        // write the somewhat compressed image (a compression quality setting
        // of 0.9f means high visual quality and large file size)
        wParam.setCompressionQuality(0.9f);
        writer.setOutput(iosLowComp);
        writer.write(null, iioImg, wParam);

        long sizeOfFileLowComp = fileLowComp.length();
        long sizeOfFileHighComp = fileHighComp.length();

        // the highly compressed image file should have a smaller file size
        // than the image file with low compression; throw an exception if
        // this isn't the case
        if (sizeOfFileLowComp < sizeOfFileHighComp) {
            throw new RuntimeException("Lower compression quality did not " +
                                       "reduce file size!");
        }
    }

    public static void main(String args[]) throws IOException {
        new CompressionBug();
    }
}
