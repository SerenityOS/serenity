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
 * @bug     7027667 7023591 7037091
 *
 * @summary Verifies that aa clipped rectangles are drawn, not filled.
 *
 * @run     main Test7027667
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import static java.awt.RenderingHints.*;

public class Test7027667 {
    public static void main(String[] args) throws Exception {
        BufferedImage bImg = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) bImg.getGraphics();
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2d.setClip(new Ellipse2D.Double(0, 0, 100, 100));
        g2d.drawRect(10, 10, 100, 100);
        if (new Color(bImg.getRGB(50, 50)).equals(Color.white)) {
            throw new Exception("Rectangle should be drawn, not filled");
        }
    }
}
