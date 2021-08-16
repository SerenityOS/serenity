/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.image.BufferedImage;

/*
 * @test
 * @summary Check that GraphicsEnvironment methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessGraphicsEnvironment
 */

public class HeadlessGraphicsEnvironment {
    public static void main(String args[]) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (!GraphicsEnvironment.isHeadless())
            throw new RuntimeException("GraphicsEnvironment.isHeadless says it's not headless mode when it is");

        boolean exceptions = false;
        try {
            GraphicsDevice[] gdl = ge.getScreenDevices();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when excepted");

        exceptions = false;
        try {
            GraphicsDevice gdl = ge.getDefaultScreenDevice();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when excepted");

        Graphics2D gd = ge.createGraphics(new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR));

        for (Font font : ge.getAllFonts()) {
            for (float j = 8; j < 17; j++) {
                Font f1 = font.deriveFont(Font.PLAIN, j);
                Font f2 = font.deriveFont(Font.BOLD, j);
                Font f3 = font.deriveFont(Font.ITALIC, j);
                Font f4 = font.deriveFont(Font.BOLD | Font.ITALIC, j);

                f1.hasUniformLineMetrics();
                f2.hasUniformLineMetrics();
                f3.hasUniformLineMetrics();
                f4.hasUniformLineMetrics();
            }
        }

        String[] fNames = ge.getAvailableFontFamilyNames();
    }
}
