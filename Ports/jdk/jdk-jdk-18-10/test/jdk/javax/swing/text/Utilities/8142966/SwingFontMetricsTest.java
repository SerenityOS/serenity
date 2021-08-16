/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.*;
import java.awt.Toolkit;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8142966 8199529
 * @summary Wrong cursor position in text components on HiDPI display
 * @run main/othervm -Dsun.java2d.uiScale=2 SwingFontMetricsTest
 */
public class SwingFontMetricsTest {

    private static final String LOWER_CASE_TEXT = "the quick brown fox jumps over the lazy dog";
    private static final String UPPER_CASE_TEXT = LOWER_CASE_TEXT.toUpperCase();
    private static final String TEXT = LOWER_CASE_TEXT + UPPER_CASE_TEXT;
    private static boolean passed = false;
    private static CountDownLatch latch = new CountDownLatch(1);
    private static Object aaHint = null;

    public static void main(String[] args) throws Exception {
        Map map = (Map)Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        aaHint = map.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        if (aaHint == null) {
            aaHint = VALUE_TEXT_ANTIALIAS_DEFAULT;
        }

        SwingUtilities.invokeAndWait(SwingFontMetricsTest::createAndShowGUI);
        latch.await(5, TimeUnit.SECONDS);

        if (!passed) {
            throw new RuntimeException("Test Failed!");
        }
    }

    private static void createAndShowGUI() {
        final JFrame frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel(TEXT) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Font font = getFont();
                Graphics2D g2d = (Graphics2D)g;
                int width1 = getFontMetrics(font).stringWidth(TEXT);
                // Set the same AA hint that the built-in Swing L&Fs set.
                g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, aaHint);
                int width2 = g.getFontMetrics(font).stringWidth(TEXT);
                passed = (width1 == width2);
                latch.countDown();
                frame.dispose();
            }
        };
        frame.add(label);
        frame.setVisible(true);
    }
}
