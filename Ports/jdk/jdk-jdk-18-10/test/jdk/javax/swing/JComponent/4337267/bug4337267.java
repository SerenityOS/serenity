/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4337267
 * @summary test that numeric shaping works in Swing components
 * @author Sergey Groznyh
 * @run main bug4337267
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class bug4337267 {
    TestJPanel p1, p2;
    TestBufferedImage i1, i2;
    JComponent[] printq;
    static JFrame window;
    static boolean testFailed = false;

    String shaped =
            "000 (E) 111 (A) \u0641\u0642\u0643 \u0662\u0662\u0662 (E) 333";
    String text = "000 (E) 111 (A) \u0641\u0642\u0643 222 (E) 333";

    void run() {
        initUI();
        testTextComponent();
        testNonTextComponentHTML();
        testNonTextComponentPlain();

    }

    void initUI() {
        window = new JFrame("bug4337267");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(800, 600);
        Component content = createContentPane();
        window.add(content);
        window.setVisible(true);
    }

    void fail(String message) {
        testFailed = true;
        throw new RuntimeException(message);
    }

    void assertEquals(Object o1, Object o2) {
        if ((o1 == null) && (o2 != null)) {
            fail("Expected null, got " + o2);
        } else if ((o1 != null) && (o2 == null)) {
            fail("Expected " + o1 + ", got null");
        } else if (!o1.equals(o2)) {
            fail("Expected " + o1 + ", got " + o2);
        }
    }

    void testTextComponent() {
        System.out.println("testTextComponent:");
        JTextArea area1 = new JTextArea();
        injectComponent(p1, area1, false);
        area1.setText(shaped);
        JTextArea area2 = new JTextArea();
        injectComponent(p2, area2, true);
        area2.setText(text);
        window.repaint();
        printq = new JComponent[] { area1, area2 };
        printComponent(printq[0], i1);
        printComponent(printq[1], i2);
        assertEquals(p1.image, p2.image);
        assertEquals(i1, i2);
    }

    void testNonTextComponentHTML() {
        System.out.println("testNonTextComponentHTML:");
        JLabel label1 = new JLabel();
        injectComponent(p1, label1, false);
        label1.setText("<html>" + shaped);
        JLabel label2 = new JLabel();
        injectComponent(p2, label2, true);
        label2.setText("<html>" + text);
        window.repaint();
        printq = new JComponent[] { label1, label2 };
        printComponent(printq[0], i1);
        printComponent(printq[1], i2);
        assertEquals(p1.image, p2.image);
        assertEquals(i1, i2);
    }

    void testNonTextComponentPlain() {
        System.out.println("testNonTextComponentPlain:");
        JLabel label1 = new JLabel();
        injectComponent(p1, label1, false);
        label1.setText(shaped);
        JLabel label2 = new JLabel();
        injectComponent(p2, label2, true);
        label2.setText(text);
        window.repaint();
        printq = new JComponent[] { label1, label2 };
        printComponent(printq[0], i1);
        printComponent(printq[1], i2);
        assertEquals(p1.image, p2.image);
        assertEquals(i1, i2);
    }

    void setShaping(JComponent c) {
        c.putClientProperty(TextAttribute.NUMERIC_SHAPING,
                    NumericShaper.getContextualShaper(NumericShaper.ARABIC));
    }

    void injectComponent(JComponent p, JComponent c, boolean shape) {
        if (shape) {
            setShaping(c);
        }
        p.removeAll();
        p.add(c);
    }

    void printComponent(JComponent c, TestBufferedImage i) {
        Graphics g = i.getGraphics();
        g.setColor(c.getBackground());
        g.fillRect(0, 0, i.getWidth(), i.getHeight());
        c.print(g);
    }

    Component createContentPane() {
        Dimension size = new Dimension(500, 100);
        i1 = new TestBufferedImage(size.width, size.height,
                                                BufferedImage.TYPE_INT_ARGB);
        i2 = new TestBufferedImage(size.width, size.height,
                                                BufferedImage.TYPE_INT_ARGB);
        p1 = new TestJPanel();
        p1.setPreferredSize(size);
        p2 = new TestJPanel();
        p2.setPreferredSize(size);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(p1);
        panel.add(p2);

        return panel;
    }

    static class TestBufferedImage extends BufferedImage {
        int MAX_GLITCHES = 0;

        TestBufferedImage(int width, int height, int imageType) {
            super(width, height, imageType);
        }

        @Override
        public boolean equals(Object other) {
            if (! (other instanceof TestBufferedImage)) {
                return false;
            }
            TestBufferedImage image2 = (TestBufferedImage) other;
            int width = getWidth();
            int height = getHeight();
            if ((image2.getWidth() != width) || (image2.getHeight() != height)) {
                return false;
            }
            int glitches = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb1 = getRGB(x, y);
                    int rgb2 = image2.getRGB(x, y);
                    if (rgb1 != rgb2) {
                        //System.out.println(x+" "+y+" "+rgb1+" "+rgb2);
                        glitches++;
                    }
                }
            }
            return glitches <= MAX_GLITCHES;
        }
    }

    static class TestJPanel extends JPanel {
        TestBufferedImage image = createImage(new Dimension(1, 1));

        TestBufferedImage createImage(Dimension d) {
            return new TestBufferedImage(d.width, d.height,
                                                BufferedImage.TYPE_INT_ARGB);
        }

        public void setPreferredSize(Dimension size) {
            super.setPreferredSize(size);
            image = createImage(size);
        }

        public void paint(Graphics g) {
            Graphics g0 = image.getGraphics();
            super.paint(g0);
            g.drawImage(image, 0, 0, this);
        } }



    public static void main(String[] args) throws Exception {
        try {
            final bug4337267 test = new bug4337267();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.run();
                }
            });

            if (testFailed) {
                throw new RuntimeException("FAIL");
            }

            System.out.println("OK");
        } finally {
            if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
        }
    }
}
