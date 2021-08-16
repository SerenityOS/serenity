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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import sun.font.Font2DHandle;
import sun.font.Font2D;
import sun.font.FontScaler;
import sun.font.Type1Font;

/**
 * @bug 8132985
 * @summary Tests to verify Type1 Font scaler dispose crashes
 * @modules java.desktop/sun.font
 */
public class FontDisposeTest
{
    public static void main(String[] args) throws Exception
    {
        // The bug only happens with Type 1 fonts. The Ghostscript font files
        // should be commonly available. From distro pacakge or
        //  ftp://ftp.gnu.org/gnu/ghostscript/gnu-gs-fonts-other-6.0.tar.gz
        // Pass pfa/pfb font file as argument
        String path = args[0];

        // Load
        InputStream stream = new FileInputStream(path);
        Font font = Font.createFont(Font.TYPE1_FONT,stream);

        // Ensure native bits have been generated
        BufferedImage img = new BufferedImage(100,100,
                                 BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        FontRenderContext frc = g2d.getFontRenderContext();

        font.getLineMetrics("derp",frc);

        // Force disposal -
        // System.gc() is not sufficient.
        Field font2DHandleField = Font.class.getDeclaredField("font2DHandle");
        font2DHandleField.setAccessible(true);
        sun.font.Font2DHandle font2DHandle =
                      (sun.font.Font2DHandle)font2DHandleField.get(font);

        sun.font.Font2D font2D = font2DHandle.font2D;
        sun.font.Type1Font type1Font = (sun.font.Type1Font)font2D;

        Method getScalerMethod =
        sun.font.Type1Font.class.getDeclaredMethod("getScaler");
        getScalerMethod.setAccessible(true);
        sun.font.FontScaler scaler =
                  (sun.font.FontScaler)getScalerMethod.invoke(type1Font);

        // dispose should not crash due to double free
        scaler.dispose();
    }
}
