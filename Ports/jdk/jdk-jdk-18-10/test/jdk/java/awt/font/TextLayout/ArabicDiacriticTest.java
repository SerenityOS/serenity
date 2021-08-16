/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary verify Arab Diacritic Positioning
 * @bug 8168759 8248352
 */

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ArabicDiacriticTest {

    static final String SAMPLE =
     "\u0627\u0644\u0639\u064e\u0631\u064e\u0628\u0650\u064a\u064e\u0651\u0629";

    static final String STR1 = "\u0644\u0639\u064e\u0629";
    static final String STR2 = "\u0644\u0639\u0629";

    static final String FONT = "DejaVu Sans";

    public static void main(String[] args) throws Exception {
        if ((args.length > 0) && (args[0].equals("-show"))) {
            showText(); // for a human
        }
        measureText(); // for the test harness
    }

    static void showText() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            JLabel label = new JLabel(SAMPLE);
            Font font = new Font(FONT, Font.PLAIN, 36);
            label.setFont(font);
            frame.setLayout(new GridLayout(3,1));
            frame.add(label);
            label = new JLabel(STR1);
            label.setFont(font);
            frame.add(label);
            label = new JLabel(STR2);
            label.setFont(font);
            frame.add(label);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static void measureText() {
        Font font = new Font(FONT, Font.PLAIN, 36);
        if (!font.getFamily(Locale.ENGLISH).equals(FONT)) {
            return;
        }
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout tl1 = new TextLayout(STR1, font, frc);
        TextLayout tl2 = new TextLayout(STR2, font, frc);
        Rectangle r1 = tl1.getPixelBounds(frc, 0f, 0f);
        Rectangle r2 = tl2.getPixelBounds(frc, 0f, 0f);
        if (r1.height > r2.height) {
            System.out.println(font);
            System.out.println(r1);
            System.out.println(r2);
            throw new RuntimeException("BAD BOUNDS");
        }
    }
}
