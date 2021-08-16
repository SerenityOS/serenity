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
 * @bug 4432615
 * @summary Tests progressive writing in the PNG encoder
 * @modules java.desktop/com.sun.imageio.plugins.png
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class WriteProgressive {

    public static void main(String[] args) throws IOException {
        Iterator witer = ImageIO.getImageWritersByFormatName("png");
        ImageWriter w = (ImageWriter)witer.next();

        File f = File.createTempFile("WriteProgressive", ".png");
        ImageOutputStream ios = ImageIO.createImageOutputStream(f);
        w.setOutput(ios);

        BufferedImage bi = new BufferedImage(100, 100,
                                             BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bi.createGraphics();
        Random r = new Random(10);
        for (int i = 0; i < 10000; i++) {
            Color c =
                new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256));
            g.setColor(c);
            g.fillRect(r.nextInt(100), r.nextInt(100), 1, 1);
        }

        IIOImage iioimage = new IIOImage(bi, null, null);

        ImageWriteParam param = w.getDefaultWriteParam();
        param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

        try {
            w.write(null, iioimage, param);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Got NPE during write!");
        }

        ios.close();

        BufferedImage bi2 = ImageIO.read(f);
        f.delete();

        ImageCompare.compare(bi, bi2);
    }
}
