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

/*
   @test
   @key headful
   @bug 8040328
   @summary JSlider has wrong preferred size with Synth LAF
   @author Semyon Sadetsky
 */

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.io.ByteArrayInputStream;

public class bug8040328 {
    private static String synthXml = "<synth>" +
            " <style id=\"all\">" +
            " <font name=\"Segoe UI\" size=\"12\"/>" +
            " </style>" +
            " <bind style=\"all\" type=\"REGION\" key=\".*\"/>" +
            " <style id=\"slider\">" +
            " <insets top=\"10\" left=\"5\" bottom=\"10\" right=\"5\"/>" +
            " </style>" +
            " <bind style=\"slider\" type=\"region\" key=\"Slider\"/>" +
            "</synth>";

    public static void main(String[] args) throws Exception {
        SynthLookAndFeel lookAndFeel = new SynthLookAndFeel();
        lookAndFeel.load(new ByteArrayInputStream(synthXml.getBytes("UTF8")),
                bug8040328.class);
        UIManager.setLookAndFeel(lookAndFeel);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final JFrame frame = new JFrame();
                try {
                    frame.setUndecorated(true);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setVisible(true);
                    test(frame);
                } finally {
                    frame.dispose();
                }
            }
        });
        System.out.println("ok");
    }

    static void test(JFrame frame) {
        JSlider hslider = new JSlider(JSlider.HORIZONTAL);
        hslider.setBackground(Color.DARK_GRAY);
        frame.getContentPane().add(hslider, BorderLayout.CENTER);
        frame.getContentPane().setBackground(Color.CYAN);
        frame.pack();
        Insets insets = hslider.getInsets();
        if (hslider.getWidth() != 200 + insets.left + insets.right) {
            throw new RuntimeException(
                    "Horizontal slider width is wrong " + hslider.getWidth());
        }
        if (hslider.getHeight() != hslider.getMinimumSize().height) {
            throw new RuntimeException(
                    "Horizontal slider height is wrong " + hslider.getHeight());
        }
        frame.getContentPane().remove(hslider);

        JSlider vslider = new JSlider(JSlider.VERTICAL);
        frame.getContentPane().add(vslider);
        frame.pack();
        insets = vslider.getInsets();
        if (vslider.getWidth() != vslider.getMinimumSize().width) {
            throw new RuntimeException(
                    "Verical slider width is wrong " + vslider.getWidth());
        }
        if (vslider.getHeight() != 200 + insets.top + insets.bottom) {
            throw new RuntimeException(
                    "Verical slider height is wrong " + vslider.getHeight());
        }
    }
}
