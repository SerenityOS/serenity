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
 * @bug 4893464
 * @summary Tests bmp writer behavior with different compression modes
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

public class CompressionModeTest {

    public static void main(String args[]) {
        int[] iModes = { ImageWriteParam.MODE_DISABLED,
                         ImageWriteParam.MODE_EXPLICIT,
                         ImageWriteParam.MODE_COPY_FROM_METADATA,
                         ImageWriteParam.MODE_DEFAULT };

        String[] strModes = { "ImageWriteParam.MODE_DISABLED",
                              "ImageWriteParam.MODE_EXPLICIT",
                              "ImageWriteParam.MODE_COPY_FROM_METADATA",
                              "ImageWriteParam.MODE_DEFAULT" };

        for(int i=0; i<iModes.length; i++) {
            System.out.println("Test compression mode "+strModes[i]);
            doTest(iModes[i]);
        }
    }

    private static void doTest(int mode) {
        String fileFormat = "bmp";
        try {
            ImageWriter iw = (ImageWriter)ImageIO.getImageWritersBySuffix(fileFormat).next();
            if(iw == null) {
                throw new RuntimeException("No available image writer for "
                                           + fileFormat
                                           + " Test failed.");
            }

            File file = new File("image." + fileFormat);
            ImageOutputStream ios = ImageIO.createImageOutputStream(file);
            iw.setOutput(ios);

            BufferedImage bimg = new BufferedImage(100,
                                                   100, BufferedImage.TYPE_INT_RGB);
            Graphics g = bimg.getGraphics();
            g.setColor(Color.green);
            g.fillRect(0,0,100,100);

            ImageWriteParam param = iw.getDefaultWriteParam();

            param.setCompressionMode(mode);

            IIOMetadata meta = iw.getDefaultImageMetadata(new ImageTypeSpecifier(bimg),
                                                          param);

            IIOImage iioImg = new IIOImage(bimg, null, meta);
            iw.write(null, iioImg, param);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed.");
        }
    }
}
