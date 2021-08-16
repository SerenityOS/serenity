/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JApplet;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/* @test
 * @bug 8032667
 * @summary [macosx] Components cannot be rendered in HiDPI to BufferedImage
 * @run applet/manual=yesno bug8032667.html
 */
public class bug8032667 extends JApplet {

    static final int scale = 2;
    static final int width = 130;
    static final int height = 50;
    static final int scaledWidth = scale * width;
    static final int scaledHeight = scale * height;

    @Override
    public void init() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                final Image image1 = getImage(getCheckBox("Deselected", false));
                final Image image2 = getImage(getCheckBox("Selected", true));

                Canvas canvas = new Canvas() {

                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        g.drawImage(image1, 0, 0, scaledWidth, scaledHeight, this);
                        g.drawImage(image2, 0, scaledHeight + 5,
                                scaledWidth, scaledHeight, this);
                    }
                };

                getContentPane().add(canvas, BorderLayout.CENTER);
            }
        });
    }

    static JCheckBox getCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setSelected(selected);
        checkBox.setSize(new Dimension(width, height));
        return checkBox;
    }

    static Image getImage(JComponent component) {
        final BufferedImage image = new BufferedImage(
                scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = image.getGraphics();
        ((Graphics2D) g).scale(scale, scale);
        component.paint(g);
        g.dispose();

        return image;
    }
}
