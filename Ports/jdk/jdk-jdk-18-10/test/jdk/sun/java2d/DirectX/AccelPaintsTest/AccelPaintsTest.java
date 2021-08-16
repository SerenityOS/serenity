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
 * @bug 6659345 8198613
 * @summary Tests that various paints work correctly when preceeded by a
 * textured operaiton.
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm AccelPaintsTest
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AccelPaintsTest extends JPanel {
    BufferedImage bi =
        new BufferedImage(80, 100, BufferedImage.TYPE_INT_ARGB_PRE);

    RadialGradientPaint rgp =
        new RadialGradientPaint(100, 100, 100, new float[] {0f, 0.2f, 0.6f, 1f},
            new Color[] { Color.red,
                          Color.yellow,
                          Color.blue,
                          Color.green},
            CycleMethod.REFLECT);
    LinearGradientPaint lgp =
            new LinearGradientPaint(30, 30, 120, 130, new float[] {0f, 0.2f, 0.6f, 1f},
            new Color[] {Color.red,
                         Color.yellow,
                         Color.blue,
                         Color.green});
    GradientPaint gp =
            new GradientPaint(30, 30, Color.red, 120, 130, Color.yellow, true);

    TexturePaint tp =
            new TexturePaint(bi, new Rectangle2D.Float(30, 30, 120, 130));


    public AccelPaintsTest() {
        Graphics g = bi.getGraphics();
        g.setColor(Color.blue);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());

        setPreferredSize(new Dimension(250, 4*120));
    }

    private void renderWithPaint(Graphics2D g2d, Paint p) {
        g2d.drawImage(bi, 130, 30, null);

        g2d.setPaint(p);
        g2d.fillRect(30, 30, 80, 100);
    }

    private void render(Graphics2D g2d) {
        renderWithPaint(g2d, rgp);
        g2d.translate(0, 100);

        renderWithPaint(g2d, lgp);
        g2d.translate(0, 100);

        renderWithPaint(g2d, gp);
        g2d.translate(0, 100);

        renderWithPaint(g2d, tp);
        g2d.translate(0, 100);
    }

    private void test() {
        GraphicsConfiguration gc =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
        if (gc.getColorModel().getPixelSize() < 16) {
            System.out.println("<16 bit depth detected, test passed");
            return;
        }

        VolatileImage vi =
            gc.createCompatibleVolatileImage(250, 4*120, Transparency.OPAQUE);
        BufferedImage res;
        do {
            vi.validate(gc);
            Graphics2D g2d = vi.createGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, vi.getWidth(), vi.getHeight());

            render(g2d);

            res = vi.getSnapshot();
        } while (vi.contentsLost());

        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                if (res.getRGB(x, y) == Color.black.getRGB()) {
                    System.err.printf("Test FAILED: found black at %d,%d\n",
                                      x, y);
                    try {
                        String fileName = "AccelPaintsTest.png";
                        ImageIO.write(res, "png", new File(fileName));
                        System.err.println("Dumped rendering to " + fileName);
                    } catch (IOException e) {}
                    throw new RuntimeException("Test FAILED: found black");
                }
            }
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;

        render(g2d);
    }

    public static void main(String[] args)
        throws InterruptedException, InvocationTargetException
    {

        if (args.length > 0 && args[0].equals("-show")) {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame f = new JFrame("RadialGradientTest");
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    AccelPaintsTest t = new AccelPaintsTest();
                    f.add(t);
                    f.pack();
                    f.setVisible(true);
                }
            });
        } else {
            AccelPaintsTest t = new AccelPaintsTest();
            t.test();
            System.out.println("Test Passed.");
        }
    }
}
