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
 * @bug 6328154 6962082
 * @summary ensure that ascii, and latin-1 text without combining marks, both layout faster
 *  than latin-1 text with combining marks.  The presumption is then that the canonical
 *  GSUB table is being run only on the latter and not on either of the former.
 */

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static java.awt.Font.*;

public class CombiningPerf {
    private static Font font;
    private static FontRenderContext frc;

    public static void main(String[] args) throws Exception {
        System.err.println("start");

        GraphicsEnvironment.getLocalGraphicsEnvironment();

        font = new Font("Lucida Sans Regular", PLAIN, 12);
        frc = new FontRenderContext(null, false, false);

        String ascii = "the characters are critical noodles?";
        String french = "l'aper\u00e7u caract\u00e8re one \u00e9t\u00e9 cr\u00e9\u00e9s";
        String frenchX = "l'aper\u00e7u caracte\u0300re one e\u0301te\u0301 ere\u0301e\u0301s";

        // warmup
        for (int i = 0; i < 100; ++i) {
            TextLayout tl = new TextLayout(french, font, frc);
            tl = new TextLayout(ascii, font, frc);
            tl = new TextLayout(frenchX, font, frc);
        }
        /**/
        long atime = test(ascii);
        System.err.println("atime: " + (atime/1000000.0) + " length: " + ascii.length());

        long ftime = test(french);
        System.err.println("ftime: " + (ftime/1000000.0) + " length: " + french.length());

        long xtime = test(frenchX);
        System.err.println("xtime: " + (xtime/1000000.0) + " length: " + frenchX.length());

        long limit = xtime * 2 / 3;
        if (atime > limit || ftime > limit) {
            throw new Exception("took too long");
        }
        /**/
    }

    private static long test(String text) {
        long start = System.nanoTime();
        for (int i = 0; i < 2000; ++i) {
            TextLayout tl = new TextLayout(text, font, frc);
        }
        return System.nanoTime() - start;
    }
}
