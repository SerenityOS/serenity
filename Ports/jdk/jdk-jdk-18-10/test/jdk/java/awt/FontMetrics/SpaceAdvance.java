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
 * @bug 8227662
 */

import java.awt.Font;
import java.awt.FontMetrics ;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SpaceAdvance {
    public static void main(String[] args) throws Exception {

        BufferedImage bi = new BufferedImage(1,1,1);
        Graphics2D g2d = bi.createGraphics();
        Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
        if (!font.canDisplay(' ')) {
            return;
        }
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        if (fm.charWidth(' ') == 0) {
            throw new RuntimeException("Space has char width of 0");
        }
    }
}
