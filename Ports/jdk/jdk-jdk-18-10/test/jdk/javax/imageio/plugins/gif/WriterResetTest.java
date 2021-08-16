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
 * @bug 6275251
 * @summary Verifies that GIF image writer throws IllegalStateException if
 *          assigned output stream was cleared by reset() method
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class WriterResetTest {
    public static void main(String[] args) throws IOException {
        ImageWriter w = ImageIO.getImageWritersByFormatName("GIF").next();
        if (w == null) {
            throw new RuntimeException("No writers available!");
        }

        ByteArrayOutputStream baos =
            new ByteArrayOutputStream();

        ImageOutputStream ios =
            ImageIO.createImageOutputStream(baos);

        w.setOutput(ios);

        BufferedImage img = createTestImage();

        try {
            w.reset();
            w.write(img);
        } catch (IllegalStateException e) {
            System.out.println("Test passed");
        } catch (Throwable e) {
            throw new RuntimeException("Test failed", e);
        }
    }

    private static BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.black);
        g.fillRect(20, 20, 60, 60);

        return img;
    }
}
