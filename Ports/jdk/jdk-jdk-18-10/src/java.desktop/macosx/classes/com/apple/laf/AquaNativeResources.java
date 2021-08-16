/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.apple.laf;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.plaf.UIResource;

import com.apple.laf.AquaUtils.RecyclableSingleton;

@SuppressWarnings("removal")
public class AquaNativeResources {
    static {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("osxui");
                    return null;
                }
            });
    }

    // TODO: removing CColorPaint for now
    @SuppressWarnings("serial") // JDK implementation class
    static class CColorPaintUIResource extends Color/*CColorPaint*/ implements UIResource {
        // The color passed to this MUST be a retained NSColor, and the CColorPaintUIResource
        //  takes ownership of that retain.
        public CColorPaintUIResource(long color, int r, int g, int b, int a) {
            super(r, g, b, a);
            //super(color, r, g, b, a);
        }
    }

    private static final RecyclableSingleton<Color> sBackgroundColor = new RecyclableSingleton<Color>() {
        @Override
        protected Color getInstance() {
            final long backgroundID = getWindowBackgroundColor();
            return new CColorPaintUIResource(backgroundID, 0xEE, 0xEE, 0xEE, 0xFF);
        }
    };
    private static native long getWindowBackgroundColor();
    public static Color getWindowBackgroundColorUIResource() {
        return sBackgroundColor.get();
    }

    static BufferedImage getRadioButtonSizerImage() {
        final BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics g = img.getGraphics();
        g.setColor(Color.pink);
        g.fillRect(0, 0, 20, 20);
        g.dispose();

        return img;
    }
}
