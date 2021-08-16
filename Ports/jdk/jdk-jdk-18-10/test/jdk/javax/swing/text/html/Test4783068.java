/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 4783068
 * @summary Disabled components should render grayed-out HTML
 * @author Peter Zhelezniakov
 * @run main Test4783068
*/

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class Test4783068 {
    final static Color TEST_COLOR = Color.WHITE;

    final static String html = "<html>" +
                  "This is a <font color='red'>colored</font> <b>text</b>" +
                  "<p>with a <a href='http://ru.sun.com'>link</a>" +
                  "<ul><li>an unordered<li>list</ul>" +
                  "<ol><li>and an ordered<li>list</ol>" +
                  "</html>";


    void test() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            throw new Error("Cannot set Metal LAF");
        }
        // Render text using background color
        UIManager.put("textInactiveText", TEST_COLOR);

        test(new JLabel(html));
        test(new JButton(html));

        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setDisabledTextColor(TEST_COLOR);
        test(pane);
    }

    void test(JComponent c) {
        c.setEnabled(false);
        c.setOpaque(true);
        c.setBackground(TEST_COLOR);
        c.setBorder(null);
        Dimension size = c.getPreferredSize();
        c.setBounds(0, 0, size.width, size.height);

        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        c.paint(image.getGraphics());

        int rgb = TEST_COLOR.getRGB();
        for (int i = 0; i < size.height; i++) {
            for (int j = 0; j < size.width; j++) {
                if (image.getRGB(j, i) != rgb) {
                    throw new RuntimeException(
                            String.format("Color mismatch at [%d, %d]", j, i));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                new Test4783068().test();
            }
        });
    }
}
