/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7019861
 *
 * @summary Verifies that the last scanline isn't skipped when doing
 *          antialiased rendering.
 *
 * @run     main Test7019861
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static java.awt.RenderingHints.*;

public class Test7019861 {

    public static void main(String[] argv) throws Exception {
        BufferedImage im = getWhiteImage(30, 30);
        Graphics2D g2 = (Graphics2D)im.getGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        g2.setStroke(new BasicStroke(10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g2.setBackground(Color.white);
        g2.setColor(Color.black);

        Path2D p = getPath(0, 0, 20);
        g2.draw(p);

        if (!(new Color(im.getRGB(20, 19))).equals(Color.black)) {
            throw new Exception("This pixel should be black");
        }
    }

    private static Path2D getPath(int x, int y, int len) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.quadTo(x + len, y, x + len, y + len);
        return p;
    }

    private static BufferedImage getWhiteImage(int w, int h) {
        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final int[] white = new int[w * h];
        Arrays.fill(white, 0xffffff);
        ret.setRGB(0, 0, w, h, white, 0, w);
        return ret;
    }
}
