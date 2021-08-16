/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6630702 8198613
 * @summary Tests that scrolling after paint() is performed correctly.
 *          This is really only applicable to Vista
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm SwingOnScreenScrollingTest
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SwingOnScreenScrollingTest extends JPanel {

    static JScrollPane pane;
    static SwingOnScreenScrollingTest test;

    public SwingOnScreenScrollingTest() {
    }

    public static void main(String[] args) {
        int size = GraphicsEnvironment.
            getLocalGraphicsEnvironment().
                getDefaultScreenDevice().
                    getDefaultConfiguration().getColorModel().getPixelSize();
        if (size < 16) {
            System.err.println("<16 bit display mode detected. Test PASSED");
            return;
        }

        final JFrame f = new JFrame("SwingOnScreenScrollingTest");
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    test = new SwingOnScreenScrollingTest();
                    pane = new JScrollPane(test);
                    f.add(pane);
                    f.pack();
                    f.setSize(100, 200);
                    f.setVisible(true);
                }
            });
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                BufferedImage bi;
                bi = new BufferedImage(100, 300,
                                       BufferedImage.TYPE_INT_RGB);
                Graphics gg = bi.getGraphics();
                test.paint(gg);
                for (int y = 80; y < 200; y +=10) {
                    test.scrollRectToVisible(new Rectangle(0, y, 100, 100));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                Point p = pane.getViewport().getLocationOnScreen();
                Robot r = null;
                try {
                    r = new Robot();
                } catch (AWTException ex) {
                    throw new RuntimeException(ex);
                }
                bi = r.createScreenCapture(new Rectangle(p.x+5, p.y+5, 30, 30));
                for (int y = 0; y < bi.getHeight(); y++) {
                    for (int x = 0; x < bi.getHeight(); x++) {
                        int rgb = bi.getRGB(x, y);
                        if (bi.getRGB(x, y) != Color.red.getRGB()) {
                            System.err.printf("Test Failed at (%d,%d) c=0x%x\n",
                                              x, y, rgb);
                            try {
                                String name =
                                    "SwingOnScreenScrollingTest_out.png";
                                ImageIO.write(bi, "png", new File(name));
                                System.err.println("Wrote grabbed image to "+
                                                   name);
                            } catch (Throwable ex) {}
                            throw new RuntimeException("Test failed");
                        }
                    }
                }
                System.out.println("Test PASSED.");
                f.dispose();
            }
        });
    }

    protected void paintComponent(Graphics g) {
        g.setColor(Color.green);
        g.fillRect(0, 0, getWidth(), 100);
        g.setColor(Color.red);
        g.fillRect(0, 100, getWidth(), getHeight()-100);
    }

    public Dimension getPreferredSize() {
        return new Dimension(100, 300);
    }
}
