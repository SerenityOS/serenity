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
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import javax.swing.*;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import static java.awt.Color.*;


/**
 * Animated intersection clipping of lines, an image and a textured rectangle.
 */
@SuppressWarnings("serial")
public class Intersection extends AnimatingControlsSurface {

    private static final int HEIGHT_DECREASE = 0;
    private static final int HEIGHT_INCREASE = 1;
    private static final int  WIDTH_DECREASE = 2;
    private static final int  WIDTH_INCREASE = 3;

    private int xx, yy, ww, hh;
    private int direction = HEIGHT_DECREASE;
    private int angdeg;
    private Shape textshape;
    private double sw, sh;
    private GeneralPath ovals;
    private Rectangle2D rectshape;
    protected boolean doIntersection = true;
    protected boolean doOvals = true;
    protected boolean doText;
    protected boolean threeSixty;


    public Intersection() {
        setBackground(WHITE);
        setControls(new Component[] { new DemoControls(this) });
    }


    @Override
    public void reset(int w, int h) {
        xx = yy = 0;
        ww = w-1; hh = h;
        direction = HEIGHT_DECREASE;
        angdeg = 0;
        FontRenderContext frc = new FontRenderContext(null, true, false);
        Font f = new Font(Font.SERIF, Font.BOLD,32);
        TextLayout tl = new TextLayout("J2D", f, frc);
        sw = tl.getBounds().getWidth();
        sh = tl.getBounds().getHeight();
        int size = Math.min(w, h);
        double sx = (size-40)/sw;
        double sy = (size-100)/sh;
        AffineTransform Tx = AffineTransform.getScaleInstance(sx, sy);
        textshape = tl.getOutline(Tx);
        rectshape = textshape.getBounds();
        sw = rectshape.getWidth();
        sh = rectshape.getHeight();
        ovals = new GeneralPath();
        ovals.append(new Ellipse2D.Double(  10,   10, 20, 20), false);
        ovals.append(new Ellipse2D.Double(w-30,   10, 20, 20), false);
        ovals.append(new Ellipse2D.Double(  10, h-30, 20, 20), false);
        ovals.append(new Ellipse2D.Double(w-30, h-30, 20, 20), false);
    }


    @Override
    public void step(int w, int h) {
        if (direction == HEIGHT_DECREASE) {
            yy+=2; hh-=4;
            if (yy >= h/2) {
                direction = HEIGHT_INCREASE;
            }
        } else if (direction == HEIGHT_INCREASE) {
            yy-=2; hh+=4;
            if (yy <= 0) {
                direction = WIDTH_DECREASE;
                hh = h-1; yy = 0;
            }
        }
        if (direction == WIDTH_DECREASE) {
            xx+=2; ww-=4;
            if (xx >= w/2) {
                direction = WIDTH_INCREASE;
            }
        } else if (direction == WIDTH_INCREASE) {
            xx-=2; ww+=4;
            if (xx <= 0) {
                direction = HEIGHT_DECREASE;
                ww = w-1; xx = 0;
            }
        }
        if ((angdeg += 5) == 360) {
            angdeg = 0;
            threeSixty = true;
        }
    }


    @Override
    public void render(int w, int h, Graphics2D g2) {

        Rectangle rect = new Rectangle(xx, yy, ww, hh);

        AffineTransform Tx = new AffineTransform();
        Tx.rotate(Math.toRadians(angdeg),w/2,h/2);
        Tx.translate(w/2-sw/2, sh+(h-sh)/2);

        GeneralPath path = new GeneralPath();
        if (doOvals) {
            path.append(ovals, false);
        }
        if (doText) {
            path.append(Tx.createTransformedShape(textshape), false);
        } else {
            path.append(Tx.createTransformedShape(rectshape), false);
        }

        if (doIntersection) {
            g2.clip(rect);
            g2.clip(path);
        }

        g2.setColor(GREEN);
        g2.fill(rect);

        g2.setClip(new Rectangle(0, 0, w, h));

        g2.setColor(LIGHT_GRAY);
        g2.draw(rect);
        g2.setColor(BLACK);
        g2.draw(path);
    }


    public static void main(String[] argv) {
        createDemoFrame(new Intersection());
    }


    static final class DemoControls extends CustomControls implements ActionListener {

        Intersection demo;
        JToolBar toolbar;

        public DemoControls(Intersection demo) {
            super(demo.name);
            this.demo = demo;
            add(toolbar = new JToolBar());
            toolbar.setFloatable(false);
            addTool("Intersect", true );
            addTool("Text",      false);
            addTool("Ovals",     true );
        }


        public void addTool(String str, boolean state) {
            JToggleButton b = (JToggleButton) toolbar.add(new JToggleButton(str));
            b.setFocusPainted(false);
            b.setSelected(state);
            b.addActionListener(this);
            int width = b.getPreferredSize().width;
            Dimension prefSize = new Dimension(width, 21);
            b.setPreferredSize(prefSize);
            b.setMaximumSize(  prefSize);
            b.setMinimumSize(  prefSize);
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            JToggleButton b = (JToggleButton) e.getSource();
            if (b.getText().equals("Intersect")) {
                demo.doIntersection = b.isSelected();
            } else if (b.getText().equals("Ovals")) {
                demo.doOvals = b.isSelected();
            } else if (b.getText().equals("Text")) {
                demo.doText = b.isSelected();
            }
            if (!demo.animating.running()) {
                demo.repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200,40);
        }


        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                if (demo.threeSixty) {
                    ((AbstractButton) toolbar.getComponentAtIndex(1)).doClick();
                    demo.threeSixty = false;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { return; }
            }
            thread = null;
        }
    } // End DemoControls
} // End Intersection
