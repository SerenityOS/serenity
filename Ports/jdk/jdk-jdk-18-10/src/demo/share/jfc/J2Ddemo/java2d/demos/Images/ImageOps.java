/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java2d.demos.Images;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.RescaleOp;
import java2d.ControlsSurface;
import java2d.CustomControls;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Images drawn using operators such as ConvolveOp LowPass & Sharpen,
 * LookupOp and RescaleOp.
 */
@SuppressWarnings("serial")
public class ImageOps extends ControlsSurface implements ChangeListener {

    protected JSlider slider1, slider2;
    private static final String[] imgName = { "bld.jpg", "boat.png" };
    private static final BufferedImage[] img = new BufferedImage[imgName.length];
    private static final String[] opsName = {
        "Threshold", "RescaleOp", "Invert", "Yellow Invert", "3x3 Blur",
        "3x3 Sharpen", "3x3 Edge", "5x5 Edge" };
    private static final BufferedImageOp[] biop =
            new BufferedImageOp[opsName.length];
    private static int rescaleFactor = 128;
    private static float rescaleOffset = 0;
    private static final int low = 100, high = 200;
    private int opsIndex, imgIndex;

    static {
        thresholdOp(low, high);
        int i = 1;
        biop[i++] = new RescaleOp(1.0f, 0, null);
        byte[] invert = new byte[256];
        byte[] ordered = new byte[256];
        for (int j = 0; j < 256; j++) {
            invert[j] = (byte) (256 - j);
            ordered[j] = (byte) j;
        }
        biop[i++] = new LookupOp(new ByteLookupTable(0, invert), null);
        byte[][] yellowInvert = new byte[][] { invert, invert, ordered };
        biop[i++] = new LookupOp(new ByteLookupTable(0, yellowInvert), null);
        int[][] dim = { { 3, 3 }, { 3, 3 }, { 3, 3 }, { 5, 5 } };
        float[][] data = { { 0.1f, 0.1f, 0.1f, // 3x3 blur
                0.1f, 0.2f, 0.1f,
                0.1f, 0.1f, 0.1f },
            { -1.0f, -1.0f, -1.0f, // 3x3 sharpen
                -1.0f, 9.0f, -1.0f,
                -1.0f, -1.0f, -1.0f },
            { 0.f, -1.f, 0.f, // 3x3 edge
                -1.f, 5.f, -1.f,
                0.f, -1.f, 0.f },
            { -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, // 5x5 edge
                -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 24.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f, -1.0f, -1.0f } };
        for (int j = 0; j < data.length; j++, i++) {
            biop[i] = new ConvolveOp(new Kernel(dim[j][0], dim[j][1], data[j]));
        }
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public ImageOps() {
        setDoubleBuffered(true);
        setBackground(Color.white);
        for (int i = 0; i < imgName.length; i++) {
            Image image = getImage(imgName[i]);
            int iw = image.getWidth(this);
            int ih = image.getHeight(this);
            img[i] = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
            img[i].createGraphics().drawImage(image, 0, 0, null);
        }
        slider1 = new JSlider(SwingConstants.VERTICAL, 0, 255, low);
        slider1.setPreferredSize(new Dimension(15, 100));
        slider1.addChangeListener(this);
        slider2 = new JSlider(SwingConstants.VERTICAL, 0, 255, high);
        slider2.setPreferredSize(new Dimension(15, 100));
        slider2.addChangeListener(this);
        setControls(new Component[] { new DemoControls(this), slider1, slider2 });
        setConstraints(new String[] {
                    BorderLayout.NORTH, BorderLayout.WEST, BorderLayout.EAST });
    }

    public static void thresholdOp(int low, int high) {
        byte[] threshold = new byte[256];
        for (int j = 0; j < 256; j++) {
            if (j > high) {
                threshold[j] = (byte) 255;
            } else if (j < low) {
                threshold[j] = (byte) 0;
            } else {
                threshold[j] = (byte) j;
            }
        }
        biop[0] = new LookupOp(new ByteLookupTable(0, threshold), null);
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        int iw = img[imgIndex].getWidth(null);
        int ih = img[imgIndex].getHeight(null);
        AffineTransform oldXform = g2.getTransform();
        g2.scale(((double) w) / iw, ((double) h) / ih);
        g2.drawImage(img[imgIndex], biop[opsIndex], 0, 0);
        g2.setTransform(oldXform);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource().equals(slider1)) {
            if (opsIndex == 0) {
                thresholdOp(slider1.getValue(), high);
            } else {
                rescaleFactor = slider1.getValue();
                biop[1] = new RescaleOp(rescaleFactor / 128.0f, rescaleOffset,
                        null);
            }
        } else {
            if (opsIndex == 0) {
                thresholdOp(low, slider2.getValue());
            } else {
                rescaleOffset = slider2.getValue();
                biop[1] = new RescaleOp(rescaleFactor / 128.0f, rescaleOffset,
                        null);
            }

        }
        repaint();
    }

    public static void main(String[] s) {
        createDemoFrame(new ImageOps());
    }


    static class DemoControls extends CustomControls implements ActionListener {

        ImageOps demo;
        JComboBox<String> imgCombo, opsCombo;
        Font font = new Font(Font.SERIF, Font.PLAIN, 10);

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(ImageOps demo) {
            super(demo.name);
            this.demo = demo;
            add(imgCombo = new JComboBox<>());
            imgCombo.setFont(font);
            for (String name : ImageOps.imgName) {
                imgCombo.addItem(name);
            }
            imgCombo.addActionListener(this);
            add(opsCombo = new JComboBox<>());
            opsCombo.setFont(font);
            for (String name : ImageOps.opsName) {
                opsCombo.addItem(name);
            }
            opsCombo.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(opsCombo)) {
                demo.opsIndex = opsCombo.getSelectedIndex();
                if (demo.opsIndex == 0) {
                    demo.slider1.setValue(ImageOps.low);
                    demo.slider2.setValue(ImageOps.high);
                    demo.slider1.setEnabled(true);
                    demo.slider2.setEnabled(true);
                } else if (demo.opsIndex == 1) {
                    demo.slider1.setValue(ImageOps.rescaleFactor);
                    demo.slider2.setValue((int) ImageOps.rescaleOffset);
                    demo.slider1.setEnabled(true);
                    demo.slider2.setEnabled(true);
                } else {
                    demo.slider1.setEnabled(false);
                    demo.slider2.setEnabled(false);
                }
            } else if (e.getSource().equals(imgCombo)) {
                demo.imgIndex = imgCombo.getSelectedIndex();
            }
            demo.repaint(10);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 39);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            try {
                Thread.sleep(1111);
            } catch (Exception e) {
                return;
            }
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 0; i < ImageOps.imgName.length; i++) {
                    imgCombo.setSelectedIndex(i);
                    for (int j = 0; j < ImageOps.opsName.length; j++) {
                        opsCombo.setSelectedIndex(j);
                        if (j <= 1) {
                            for (int k = 50; k <= 200; k += 10) {
                                demo.slider1.setValue(k);
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            }
                        }
                        try {
                            Thread.sleep(4444);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }
            thread = null;
        }
    } // End DemoControls
} // End ImageOps

