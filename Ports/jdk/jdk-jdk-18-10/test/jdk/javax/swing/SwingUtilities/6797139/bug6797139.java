/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 6797139
 * @author Alexander Potochkin
 * @summary tests that JButton's text is not incorrectly truncated
 */
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.image.BufferedImage;

public class bug6797139 {

    private static void createGui() {
        JButton b = new JButton("Probably");
        b.setUI(new BasicButtonUI() {
            protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
                super.paintText(g, b, textRect, text);
                if (text.endsWith("...")) {
                    throw new RuntimeException("Text is truncated!");
                }
            }
        });
        b.setSize(b.getPreferredSize());
        BufferedImage image = new BufferedImage(b.getWidth(), b.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        b.paint(g);
        g.dispose();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createGui();
            }
        });
    }
}
