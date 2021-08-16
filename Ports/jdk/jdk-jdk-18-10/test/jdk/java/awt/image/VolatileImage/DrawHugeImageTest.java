/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     8040617 8198613
 * @summary Test verifies that an attempt to get an accelerated copy of
 *          a huge buffered image does not result in an OOME.
 *
 * @run     main DrawHugeImageTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class DrawHugeImageTest {
    // we have to render the BI source several times in order
    // to get an accelerated copy to be used.
    static {
        System.setProperty("sun.java2d.accthreshold", "1");
    }
    private static final int max_rendering_count = 5;

    private static final Color srcColor = Color.red;
    private static final Color dstColor = Color.blue;

    public static void main(String[] args) {
        BufferedImage src = createSrc();

        VolatileImage dst = createDst();
        System.out.println("Dst: " + dst);
        boolean status;
        int count = max_rendering_count;

        do {
            System.out.println("render image: " + (max_rendering_count - count));
            status = render(src, dst);

        } while (status && count-- > 0);

        if (!status || count > 0) {
            throw new RuntimeException("Test failed: " + count);
        }
    }

    private static boolean render(BufferedImage src, VolatileImage dst) {
        int cnt = 5;
        do {
            Graphics2D g = dst.createGraphics();
            g.setColor(dstColor);
            g.fillRect(0, 0, dst.getWidth(), dst.getHeight());
            g.drawImage(src, 0, 0, null);
            g.dispose();
        } while (dst.contentsLost() && (--cnt > 0));

        if (cnt == 0) {
            System.err.println("Test failed: unable to render to volatile destination");
            return false;
        }

        BufferedImage s = dst.getSnapshot();

        return s.getRGB(1,1) == srcColor.getRGB();
    }

    private static BufferedImage createSrc() {
        final int w = 20000;
        final int h = 5;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.setColor(srcColor);
        g.fillRect(0, 0, w, h);
        g.dispose();

        return img;
    }

    private static VolatileImage createDst() {
        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        return gc.createCompatibleVolatileImage(200, 200);
    }
}
