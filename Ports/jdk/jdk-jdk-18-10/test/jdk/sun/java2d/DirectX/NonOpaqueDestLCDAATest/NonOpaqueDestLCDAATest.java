/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6728834 6749060 8198613
 * @summary Tests that LCD AA text rendering works properly with destinations
 * being VolatileImage of all transparency types
 * @author Dmitri.Trembovetski: area=Graphics
 * @run main/manual/othervm -Dsun.java2d.d3d=false NonOpaqueDestLCDAATest
 * @run main/manual/othervm NonOpaqueDestLCDAATest
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import static java.awt.Transparency.*;

public class NonOpaqueDestLCDAATest extends JFrame implements ActionListener {
    private static volatile boolean passed = true;
    private static CountDownLatch complete = new CountDownLatch(1);

    public NonOpaqueDestLCDAATest() {
        JTextArea desc = new JTextArea();
        desc.setText(
            "\n  Instructions: the three text strings below should appear" +
            "  readable, without smudges or misshapen bold glyphs.\n" +
            "  You may need a magnifier to notice some bad colorfringing in "+
            "  in SW Translucent case, especially in vertical stems.\n\n"+
            "  Basically text rendered to TRANSLUCENT destination should look"+
            "  similar to one rendered to OPAQUE - it may differ in whether or" +
            "  not it's LCD, but it should look 'correct'\n\n"+
            "If the text looks fine the test PASSED otherwise it FAILED.\n");
        desc.setEditable(false);
        desc.setBackground(Color.black);
        desc.setForeground(Color.green);
        add("North", desc);
        JPanel renderPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                render(g, getWidth(), getHeight());
            }
        };
        renderPanel.setPreferredSize(new Dimension(1024, 650));
        renderPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                images = null;
            }
        });
        add("Center", renderPanel);

        JButton passedBtn = new JButton("Passed");
        JButton failedBtn = new JButton("Failed");
        passedBtn.addActionListener(this);
        failedBtn.addActionListener(this);
        JPanel p = new JPanel();
        p.add(passedBtn);
        p.add(failedBtn);
        add("South", p);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                complete.countDown();
            }
        });
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void render(Graphics g, int w, int h) {
        initImages(w, h);

        g.setColor(new Color(0xAD, 0xD8, 0xE6));
        g.fillRect(0, 0, w, h);

        Graphics2D g2d = (Graphics2D) g.create();
        for (Image im : images) {
            g2d.drawImage(im, 0, 0, null);
            g2d.translate(0, im.getHeight(null));
        }
    }

    String tr[] = { "OPAQUE", "BITMASK", "TRANSLUCENT" };
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Passed")) {
            passed = true;
            System.out.println("Test Passed");
        } else if (e.getActionCommand().equals("Failed")) {
            System.out.println("Test Failed");
            for (int i = 0; i < images.length; i++) {
                String f = "NonOpaqueDestLCDAATest_"+tr[i];
                try {
                    if (images[i] instanceof VolatileImage) {
                        f += "_vi.png";
                        ImageIO.write(((VolatileImage)images[i]).
                                getSnapshot(), "png", new File(f));
                    } else {
                        f += "_bi.png";
                        ImageIO.write((BufferedImage)images[i],
                                       "png", new File(f));
                    }
                    System.out.printf("Dumped %s image to %s\n", tr[i], f);
                } catch (Throwable t) {}
            }
            passed = false;
        }
        dispose();
        complete.countDown();
    }

    static void clear(Graphics2D  g, int type, int w, int h) {
        Graphics2D gg = (Graphics2D) g.create();
        if (type > OPAQUE) {
            gg.setColor(new Color(0, 0, 0, 0));
            gg.setComposite(AlphaComposite.Src);
        } else {
            gg.setColor(new Color(0xAD, 0xD8, 0xE6));
        }
        gg.fillRect(0, 0, w, h);
    }

    private void render(Image im, int type, String s) {
        Graphics2D g2d = (Graphics2D) im.getGraphics();
        clear(g2d, type, im.getWidth(null), im.getHeight(null));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        Font f = new Font("Dialog", Font.BOLD, 40);// g2d.getFont().deriveFont(32.0f);
        g2d.setColor(Color.white);
        g2d.setFont(g2d.getFont().deriveFont(36.0f));
        g2d.drawString(s, 10, im.getHeight(null) / 2);
    }

    Image images[];
    private void initImages(int w, int h) {
        if (images == null) {
            images = new Image[6];
            GraphicsConfiguration gc = getGraphicsConfiguration();
            for (int i = OPAQUE; i <= TRANSLUCENT; i++) {
                VolatileImage vi =
                    gc.createCompatibleVolatileImage(w,h/images.length,i);
                images[i-1] = vi;
                vi.validate(gc);
                String s = "LCD AA Text rendered to " + tr[i - 1] + " HW destination";
                render(vi, i, s);

                s = "LCD AA Text rendered to " + tr[i - 1] + " SW destination";
                images[i-1+3] = gc.createCompatibleImage(w, h/images.length, i);
                render(images[i-1+3], i, s);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                NonOpaqueDestLCDAATest t = new NonOpaqueDestLCDAATest();
                t.pack();
                t.setVisible(true);
            }
        });

        complete.await();
        if (!passed) {
            throw new RuntimeException("Test Failed!");
        }
    }
}
