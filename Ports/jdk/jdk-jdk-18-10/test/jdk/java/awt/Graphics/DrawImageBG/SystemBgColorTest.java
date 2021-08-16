/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4614845
 * @summary Test drawImage(bgcolor) gets correct RGB from SystemColor objects.
 * @run     main SystemBgColorTest
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

public class SystemBgColorTest {
    public static final int TESTW = 10;
    public static final int TESTH = 10;

    static SystemColor systemColorObjects [] = {
        SystemColor.desktop,
        SystemColor.activeCaption,
        SystemColor.activeCaptionText,
        SystemColor.activeCaptionBorder,
        SystemColor.inactiveCaption,
        SystemColor.inactiveCaptionText,
        SystemColor.inactiveCaptionBorder,
        SystemColor.window,
        SystemColor.windowBorder,
        SystemColor.windowText,
        SystemColor.menu,
        SystemColor.menuText,
        SystemColor.text,
        SystemColor.textText,
        SystemColor.textHighlight,
        SystemColor.textHighlightText,
        SystemColor.textInactiveText,
        SystemColor.control,
        SystemColor.controlText,
        SystemColor.controlHighlight,
        SystemColor.controlLtHighlight,
        SystemColor.controlShadow,
        SystemColor.controlDkShadow,
        SystemColor.scrollbar,
        SystemColor.info,
        SystemColor.infoText
    };

    static boolean counterrors;
    static int errcount;

    public static void error(String problem) {
        if (counterrors) {
            errcount++;
        } else {
            throw new RuntimeException(problem);
        }
    }

    public static void main(String argv[]) {
        counterrors = (argv.length > 0);
        test(BufferedImage.TYPE_INT_ARGB);
        test(BufferedImage.TYPE_INT_RGB);
        if (errcount > 0) {
            throw new RuntimeException(errcount+" errors");
        }
    }

    static int cmap[] = {
        0x00000000,
        0xffffffff,
    };

    public static void test(int dsttype) {
        BufferedImage src =
            new BufferedImage(TESTW, TESTH, BufferedImage.TYPE_INT_ARGB);
        test(src, dsttype);
        IndexColorModel icm = new IndexColorModel(8, 2, cmap, 0, true, 0,
                                                  DataBuffer.TYPE_BYTE);
        src = new BufferedImage(TESTW, TESTH,
                                BufferedImage.TYPE_BYTE_INDEXED, icm);
        test(src, dsttype);
    }

    public static void test(Image src, int dsttype) {
        BufferedImage dst =
            new BufferedImage(TESTW, TESTH, dsttype);
        for (int i = 0; i < systemColorObjects.length; i++) {
            test(src, dst, systemColorObjects[i]);
        }
    }

    public static void test(Image src, BufferedImage dst, Color bg) {
        Graphics2D g = (Graphics2D) dst.getGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.white);
        g.fillRect(0, 0, TESTW, TESTH);
        g.drawImage(src, 0, 0, bg, null);
        int dstRGB = dst.getRGB(0, 0);
        int bgRGB = bg.getRGB();
        if (!dst.getColorModel().hasAlpha()) {
            bgRGB |= 0xFF000000;
        }
        if (dstRGB != bgRGB) {
            System.err.println("Actual: " + Integer.toHexString(dstRGB));
            System.err.println("Expected: " + Integer.toHexString(bgRGB));
            error("bad bg pixel for: " + bg);
        }
    }
}