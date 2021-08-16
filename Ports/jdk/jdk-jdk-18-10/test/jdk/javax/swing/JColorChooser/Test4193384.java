/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 4193384 4200976
 * @summary Tests the color conversions and the preview panel foreground color
 * @author Mark Davidson
 */

import java.awt.Color;
import javax.swing.JColorChooser;
import javax.swing.JLabel;

public class Test4193384 {
    public static void main(String[] args) {
        test(new Color[] {
                new Color(11, 12, 13),
                new Color(204, 0, 204),
                new Color(0, 51, 51)
        });
    }

    private static void test(Color[] colors) {
        JLabel label = new JLabel("Preview Panel"); // NON-NLS: simple label

        JColorChooser chooser = new JColorChooser();
        chooser.setPreviewPanel(label);

        float[] hsb = new float[3];
        for (int i = 0; i < colors.length; i++) {
            Color color = colors[i];
            // Make sure sure that there wasn't a regression
            // in java.awt.Color and the conversion methods
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
            if (!color.equals(Color.getHSBColor(hsb[0], hsb[1], hsb[2]))) {
                throw new Error("color conversion is failed");
            }
            // 4193384 regression test
            if (!color.equals(new JColorChooser(color).getColor())) {
                throw new Error("constructor sets incorrect initial color");
            }
            // 4200976 regression test
            chooser.setColor(color);
            if (!color.equals(label.getForeground())) {
                throw new Error("a custom preview panel doesn't handle colors");
            }
        }
    }
}
