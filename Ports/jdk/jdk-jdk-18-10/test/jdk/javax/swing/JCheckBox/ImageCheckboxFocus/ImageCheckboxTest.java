/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JCheckBox;

/*
 * @test
 * @key headful
 * @bug 8216358
 * @summary [macos] The focus is invisible when tab to "Image Radio Buttons" and "Image CheckBoxes"
 * @library ../../regtesthelpers/
 * @build Util
 * @run main ImageCheckboxTest
 */

public class ImageCheckboxTest {
    public static void main(String[] args) throws Exception {
        new ImageCheckboxTest().performTest();
    }

    public void performTest() throws Exception {
        BufferedImage imageNoFocus = new BufferedImage(100, 50,
                BufferedImage.TYPE_INT_ARGB);
        BufferedImage imageFocus = new BufferedImage(100, 50,
                BufferedImage.TYPE_INT_ARGB);

        CustomCheckBox checkbox = new CustomCheckBox("Test", new MyIcon(Color.GREEN));
        checkbox.setSize(100, 50);
        checkbox.setFocused(false);
        checkbox.paint(imageNoFocus.createGraphics());
        checkbox.setFocused(true);
        checkbox.paint(imageFocus.createGraphics());

        if (Util.compareBufferedImages(imageFocus, imageNoFocus)) {
            ImageIO.write(imageFocus, "png", new File("imageFocus.png"));
            ImageIO.write(imageNoFocus, "png", new File("imageNoFocus.png"));
            throw new Exception("Changing focus is not visualized");
        }
    }

    class MyIcon implements Icon {
        Color color;
        public MyIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color old = g.getColor();
            g.setColor(color);
            g.fillArc(x+2, y+2, 12, 12, 0, 360);
            g.setColor(old);
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }

    class CustomCheckBox extends JCheckBox {
        public CustomCheckBox(String label, Icon icon) {
            super(label, icon);
        }

        private boolean focused = false;
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean hasFocus() {
            return focused;
        }
    }
}
