/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.VolatileImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * @test
 * @bug 4917097 8019816 8198411
 * @key headful
 * @summary 1.4.1 REGRESSION: BasicStroke Dashes don't show when scale * line width = 1.0
 * @run main/othervm -Dsun.java2d.uiScale=1 DashScaleMinWidth
 */
public final class DashScaleMinWidth  {

    public static void main(final String[] args) {
        BufferedImage img = new BufferedImage(200, 40, TYPE_INT_ARGB);
        draw(img);
        validate(img);

        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
        if (gc.getColorModel() instanceof IndexColorModel) {
            System.err.println("Skipping VolatileImage because of IndexColorModel");
            return;
        }

        VolatileImage vi = gc.createCompatibleVolatileImage(200, 40);
        BufferedImage snapshot;
        int attempt = 0;
        while (true) {
            if (++attempt > 10) {
                throw new RuntimeException("Too many attempts: " + attempt);
            }
            vi.validate(gc);
            draw(vi);
            snapshot = vi.getSnapshot();
            if (!vi.contentsLost()) {
                break;
            }
        }
        validate(snapshot);
    }

    private static void draw(final Image img) {
        float[] dashes = {200.0f, 200.0f};
        BasicStroke bs = new BasicStroke(20.0f,
                                         BasicStroke.CAP_BUTT,
                                         BasicStroke.JOIN_MITER,
                                         1.0f,
                                         dashes,
                                         0.0f);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 40);
        Line2D line = new Line2D.Double(400, 400, 3600, 400);
        g.setColor(Color.BLACK);
        g.scale(0.05, 0.05);
        g.setStroke(bs);
        g.draw(line);
        g.dispose();
    }

    private static void validate(final BufferedImage img) {
        int white = Color.white.getRGB();
        int point = img.getRGB(35, 20);   // point in the gap
        if (point != white) {
            throw new RuntimeException("Line should be dashed");
        }
    }
}
