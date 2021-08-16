/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6967419
 * @summary Test verifies that when we force stop PNG writing to
 *          ImageOutputStream, it should not cause IndexOutOfBoundException.
 * @run     main PngForceStopWritingTest
 */

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

public class PngForceStopWritingTest {

    public static void main(String[] args) throws IOException {

        OutputStream outputStream = new NullOutputStream();
        ImageOutputStream imageOutputStream =
            ImageIO.createImageOutputStream(outputStream);
        try {
            ImageIO.write(createImage(2048),"PNG", imageOutputStream);
        } catch (IOException e) {
            imageOutputStream.close();
        }
    }

    private static BufferedImage createImage(int size) {

        BufferedImage image = new
            BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, Color.blue, size, size, Color.red));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return image;
    }

    static class NullOutputStream extends OutputStream {
        long count = 0;
        @Override
        public void write(int b) throws IOException {
            count++;
            if (count > 30000L) {
                throw new IOException("Force stop image writing");
            }
        }
    }
}
