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
package java2d.demos.Clipping;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import static java.lang.Math.random;
import static java.awt.Color.*;


/**
 * Animated clipping of an image & composited shapes.
 */
@SuppressWarnings("serial")
public class ClipAnim extends AnimatingControlsSurface {

    private static Image dimg, cimg;
    static TexturePaint texturePaint;

    static {
        BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D big = bi.createGraphics();
        big.setBackground(YELLOW);
        big.clearRect(0, 0, 5, 5);
        big.setColor(RED);
        big.fillRect(0, 0, 3, 3);
        texturePaint = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));
    }
    private AnimVal[] animval = new AnimVal[3];
    protected boolean doObjects = true;
    private Font originalFont = new Font(Font.SERIF, Font.PLAIN, 12);
    private Font font;
    private GradientPaint gradient;
    private int strX, strY;
    private int dukeX, dukeY, dukeWidth, dukeHeight;

    public ClipAnim() {
        cimg = getImage("clouds.jpg");
        dimg = getImage("duke.png");
        setBackground(WHITE);
        animval[0] = new AnimVal(true);
        animval[1] = new AnimVal(false);
        animval[2] = new AnimVal(false);
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void reset(int w, int h) {
        for (AnimVal a : animval) {
            a.reset(w, h);
        }
        gradient = new GradientPaint(0, h / 2, RED, w * .4f, h * .9f, YELLOW);
        double scale = 0.4;
        dukeHeight = (int) (scale * h);
        dukeWidth = (int) (dimg.getWidth(this) * scale * h / dimg.getHeight(this));
        dukeX = (int) (w * .25 - dukeWidth / 2);
        dukeY = (int) (h * .25 - dukeHeight / 2);
        FontMetrics fm = getFontMetrics(originalFont);
        double sw = fm.stringWidth("CLIPPING");
        double sh = fm.getAscent() + fm.getDescent();
        double sx = (w / 2 - 30) / sw;
        double sy = (h / 2 - 30) / sh;
        AffineTransform Tx = AffineTransform.getScaleInstance(sx, sy);
        font = originalFont.deriveFont(Tx);
        fm = getFontMetrics(font);
        strX = (int) (w * .75 - fm.stringWidth("CLIPPING") / 2);
        strY = (int) (h * .72 + fm.getAscent() / 2);
    }

    @Override
    public void step(int w, int h) {
        for (AnimVal a : animval) {
            if (a.isSelected) {
                a.step(w, h);
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        GeneralPath p1 = new GeneralPath();
        GeneralPath p2 = new GeneralPath();

        for (AnimVal a : animval) {
            if (a.isSelected) {
                double x = a.x;
                double y = a.y;
                double ew = a.ew;
                double eh = a.eh;
                p1.append(new Ellipse2D.Double(x, y, ew, eh), false);
                p2.append(new Rectangle2D.Double(x + 5, y + 5, ew - 10, eh - 10),
                        false);
            }
        }
        if (animval[0].isSelected || animval[1].isSelected
                || animval[2].isSelected) {
            g2.setClip(p1);
            g2.clip(p2);
        }

        if (doObjects) {
            int w2 = w / 2;
            int h2 = h / 2;
            g2.drawImage(cimg, 0, 0, w2, h2, null);
            g2.drawImage(dimg, dukeX, dukeY, dukeWidth, dukeHeight, null);

            g2.setPaint(texturePaint);
            g2.fillRect(w2, 0, w2, h2);

            g2.setPaint(gradient);
            g2.fillRect(0, h2, w2, h2);

            g2.setColor(LIGHT_GRAY);
            g2.fillRect(w2, h2, w2, h2);
            g2.setColor(RED);
            g2.drawOval(w2, h2, w2 - 1, h2 - 1);
            g2.setFont(font);
            g2.drawString("CLIPPING", strX, strY);
        } else {
            g2.setColor(LIGHT_GRAY);
            g2.fillRect(0, 0, w, h);
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new ClipAnim());
    }


    public class AnimVal {

        double ix = 5.0;
        double iy = 3.0;
        double iw = 5.0;
        double ih = 3.0;
        double x, y;
        double ew, eh;   // ellipse width & height
        boolean isSelected;

        public AnimVal(boolean isSelected) {
            this.isSelected = isSelected;
        }

        public void step(int w, int h) {
            x += ix;
            y += iy;
            ew += iw;
            eh += ih;

            if (ew > w / 2) {
                ew = w / 2;
                iw = random() * -w / 16 - 1;
            }
            if (ew < w / 8) {
                ew = w / 8;
                iw = random() * w / 16 + 1;
            }
            if (eh > h / 2) {
                eh = h / 2;
                ih = random() * -h / 16 - 1;
            }
            if (eh < h / 8) {
                eh = h / 8;
                ih = random() * h / 16 + 1;
            }

            if ((x + ew) > w) {
                x = (w - ew) - 1;
                ix = random() * -w / 32 - 1;
            }
            if ((y + eh) > h) {
                y = (h - eh) - 2;
                iy = random() * -h / 32 - 1;
            }
            if (x < 0) {
                x = 2;
                ix = random() * w / 32 + 1;
            }
            if (y < 0) {
                y = 2;
                iy = random() * h / 32 + 1;
            }
        }

        public void reset(int w, int h) {
            x = random() * w;
            y = random() * h;
            ew = (random() * w) / 2;
            eh = (random() * h) / 2;
        }
    }


    static final class DemoControls extends CustomControls implements
            ActionListener {

        ClipAnim demo;
        JToolBar toolbar;

        public DemoControls(ClipAnim demo) {
            super(demo.name);
            this.demo = demo;
            add(toolbar = new JToolBar());
            toolbar.setFloatable(false);
            addTool("Objects", true);
            addTool("Clip1", true);
            addTool("Clip2", false);
            addTool("Clip3", false);
        }

        public void addTool(String str, boolean state) {
            JToggleButton b =
                    (JToggleButton) toolbar.add(new JToggleButton(str));
            b.setFocusPainted(false);
            b.setSelected(state);
            b.addActionListener(this);
            int width = b.getPreferredSize().width;
            Dimension prefSize = new Dimension(width, 21);
            b.setPreferredSize(prefSize);
            b.setMaximumSize(prefSize);
            b.setMinimumSize(prefSize);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JToggleButton b = (JToggleButton) e.getSource();
            if (b.getText().equals("Objects")) {
                demo.doObjects = b.isSelected();
            } else if (b.getText().equals("Clip1")) {
                demo.animval[0].isSelected = b.isSelected();
            } else if (b.getText().equals("Clip2")) {
                demo.animval[1].isSelected = b.isSelected();
            } else if (b.getText().equals("Clip3")) {
                demo.animval[2].isSelected = b.isSelected();
            }
            if (!demo.animating.running()) {
                demo.repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 40);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
            ((AbstractButton) toolbar.getComponentAtIndex(2)).doClick();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
            if (getSize().width > 400) {
                ((AbstractButton) toolbar.getComponentAtIndex(3)).doClick();
            }
            thread = null;
        }
    } // End DemoControls
} // End ClipAnim

