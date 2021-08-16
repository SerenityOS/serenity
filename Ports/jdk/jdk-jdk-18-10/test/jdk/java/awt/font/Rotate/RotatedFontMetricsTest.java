/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test    RotatedFontMetricsTest
 * @bug     8139178
 * @summary This test verifies that rotation does not affect font metrics.
 * @run     main RotatedFontMetricsTest
 */

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class RotatedFontMetricsTest {
    static final int FONT_SIZE = Integer.getInteger("font.size", 20);

    public static void main(String ... args) {
        Font font = new Font(Font.DIALOG, Font.PLAIN, FONT_SIZE);
        Graphics2D g2d = createGraphics();

        FontMetrics ref = null;
        RuntimeException failure = null;
        for (int a = 0; a < 360; a += 15) {
            Graphics2D g = (Graphics2D)g2d.create();
            g.rotate(Math.toRadians(a));
            FontMetrics m = g.getFontMetrics(font);
            g.dispose();

            boolean status = true;
            if (ref == null) {
                ref = m;
            } else {
                status = ref.getAscent() == m.getAscent() &&
                        ref.getDescent() == m.getDescent() &&
                        ref.getLeading() == m.getLeading() &&
                        ref.getMaxAdvance() == m.getMaxAdvance();
            }

            System.out.printf("Metrics a%d, d%d, l%d, m%d (%d) %s\n",
                    m.getAscent(), m.getDescent(), m.getLeading(), m.getMaxAdvance(),
                    (int)a, status ? "OK" : "FAIL");

            if (!status && failure == null) {
                failure = new RuntimeException("Font metrics differ for angle " + a);
            }
        }
        if (failure != null) {
            throw failure;
        }
        System.out.println("done");
    }

    private static Graphics2D createGraphics() {
        BufferedImage dst = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        return dst.createGraphics();
    }
}
