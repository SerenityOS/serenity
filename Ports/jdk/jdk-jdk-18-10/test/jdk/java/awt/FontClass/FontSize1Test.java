/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216965
 * @summary verify no crash when rendering size 1 fonts
 */

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

public class FontSize1Test {

    static final String text = "abcdefghijklmnopqrstuvwxyz";

    public static void main(String[] args) {

        BufferedImage bi =
            new BufferedImage(100, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        Font af[] =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        for (Font f : af) {
            System.out.println("Looking at font " + f);
            g2d.setFont(f);
            g2d.getFontMetrics().getWidths();
            g2d.drawString(text, 50, 10);
        }
        g2d.dispose();
    }

}
