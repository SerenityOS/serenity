/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6286578
 * @summary Test verifies that RGB images does not convert to gray-scaled if
 *          default image metadata is used
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class RGBImageTest {

    Color[] usedColors = {
        Color.red, Color.green, Color.blue, Color.yellow,
        Color.cyan, Color.magenta, Color.white, Color.black };

    BufferedImage src = null;
    int dx= 20;
    int height = 100;

    protected BufferedImage getSrc() {
        if (src == null) {
            src = new BufferedImage(dx * usedColors.length, height,
                                    BufferedImage.TYPE_INT_RGB);
            Graphics g = src.createGraphics();
            for (int i = 0; i < usedColors.length; i++) {
                g.setColor(usedColors[i]);
                g.fillRect(dx * i,  0, dx, height);
            }
        }
        return src;
    }

    protected void doTest() throws IOException {
        BufferedImage biSrc = getSrc();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("GIF").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        IIOMetadata imageMetadata =
            writer.getDefaultImageMetadata(new ImageTypeSpecifier(biSrc), writeParam);

        IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(writeParam);

        IIOImage iioImg = new IIOImage(biSrc, null, imageMetadata);
        writer.write(streamMetadata, iioImg, writeParam);
        ios.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        ImageReader reader = ImageIO.getImageReader(writer);
        reader.setInput(iis);
        BufferedImage dst = reader.read(0);

        // do test
        int x = dx / 2;
        int y = height / 2;

        for (int i = 0; i < usedColors.length; i++) {
            int dstRgb = dst.getRGB(x, y);
            System.out.println("dstColor: " + Integer.toHexString(dstRgb));
            int srcRgb = usedColors[i].getRGB();
            System.out.println("srcColor: " + Integer.toHexString(srcRgb));
            if (dstRgb != srcRgb) {
                throw new RuntimeException("wrong color " + i + ": " + Integer.toHexString(dstRgb));
            }
            x += dx;
        }

    }

    public static void main(String[] args) throws IOException {
        RGBImageTest t = new RGBImageTest();
        t.doTest();
    }
}
