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

import java.io.*;
import java.awt.*;
import java.awt.font.*;
import java.util.*;

/**
 * Shows (top) with kerning, (middle) without, (bottom) also without.
 *
 * @bug 7017324
 */
public class KernCrash extends Frame {
    private static Font font0;
    private static Font font1;
    private static Font font2;

    public static void main(String[] args) throws Exception {
        HashMap attrs = new HashMap();
        font0 = Font.createFont(Font.TRUETYPE_FONT, new File("Vera.ttf"));
        System.out.println("using " + font0);
        attrs.put(TextAttribute.SIZE, new Float(58f));
        font1 = font0.deriveFont(attrs);
        attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        font2 = font0.deriveFont(attrs);

        KernCrash f = new KernCrash();
        f.setTitle("Kerning Crash");
        f.setSize(600, 300);
        f.setForeground(Color.black);
        f.show();
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout("text", font2, frc);
        layout.draw(g2, 10, 150);

        String s = "WAVATastic";
        TextLayout layout2 = new TextLayout(s, font1, frc);
        layout2.draw(g2, 10, 200);
        TextLayout layout3 = new TextLayout(s, font2, frc);
        layout3.draw(g2, 10, 100);
    }
}
