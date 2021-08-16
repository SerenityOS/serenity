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
package java2d.demos.Transforms;


import static java.awt.Color.BLACK;
import static java.awt.Color.ORANGE;
import static java.awt.Color.WHITE;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;


/**
 * Scaling or Shearing or Rotating an image & rectangle.
 */
@SuppressWarnings("serial")
public class SelectTx extends AnimatingControlsSurface {

    protected static final int RIGHT = 0;
    private static final int LEFT = 1;
    private static final int XMIDDLE = 2;
    private static final int DOWN = 3;
    private static final int UP = 4;
    private static final int YMIDDLE = 5;
    private static final int XupYup = 6;
    private static final int XdownYdown = 7;
    private static final String[] title = { "Scale", "Shear", "Rotate" };
    protected static final int SCALE = 0;
    protected static final int SHEAR = 1;
    protected static final int ROTATE = 2;
    private Image original;
    private int iw, ih;
    protected int transformType = SHEAR;
    protected double sx, sy;
    protected double angdeg;
    protected int direction = RIGHT;
    protected int transformToggle;

    @SuppressWarnings("LeakingThisInConstructor")
    public SelectTx() {
        setBackground(WHITE);
        original = getImage("painting.png");
        iw = original.getWidth(this);
        ih = original.getHeight(this);
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void reset(int w, int h) {

        iw = w > 3 ? w / 3 : 1;
        ih = h > 3 ? h / 3 : 1;

        if (transformType == SCALE) {
            direction = RIGHT;
            sx = sy = 1.0;
        } else if (transformType == SHEAR) {
            direction = RIGHT;
            sx = sy = 0;
        } else {
            angdeg = 0;
        }
    }

    @Override
    public void step(int w, int h) {
        int rw = iw + 10;
        int rh = ih + 10;

        if (transformType == SCALE && direction == RIGHT) {
            sx += .05;
            if (w * .5 - iw * .5 + rw * sx + 10 > w) {
                direction = DOWN;
            }
        } else if (transformType == SCALE && direction == DOWN) {
            sy += .05;
            if (h * .5 - ih * .5 + rh * sy + 20 > h) {
                direction = LEFT;
            }
        } else if (transformType == SCALE && direction == LEFT) {
            sx -= .05;
            if (rw * sx - 10 <= -(w * .5 - iw * .5)) {
                direction = UP;
            }
        } else if (transformType == SCALE && direction == UP) {
            sy -= .05;
            if (rh * sy - 20 <= -(h * .5 - ih * .5)) {
                direction = RIGHT;
                transformToggle = SHEAR;
            }
        }

        if (transformType == SHEAR && direction == RIGHT) {
            sx += .05;
            if (rw + 2 * rh * sx + 20 > w) {
                direction = LEFT;
                sx -= .1;
            }
        } else if (transformType == SHEAR && direction == LEFT) {
            sx -= .05;
            if (rw - 2 * rh * sx + 20 > w) {
                direction = XMIDDLE;
            }
        } else if (transformType == SHEAR && direction == XMIDDLE) {
            sx += .05;
            if (sx > 0) {
                direction = DOWN;
                sx = 0;
            }
        } else if (transformType == SHEAR && direction == DOWN) {
            sy -= .05;
            if (rh - 2 * rw * sy + 20 > h) {
                direction = UP;
                sy += .1;
            }
        } else if (transformType == SHEAR && direction == UP) {
            sy += .05;
            if (rh + 2 * rw * sy + 20 > h) {
                direction = YMIDDLE;
            }
        } else if (transformType == SHEAR && direction == YMIDDLE) {
            sy -= .05;
            if (sy < 0) {
                direction = XupYup;
                sy = 0;
            }
        } else if (transformType == SHEAR && direction == XupYup) {
            sx += .05;
            sy += .05;
            if (rw + 2 * rh * sx + 30 > w || rh + 2 * rw * sy + 30 > h) {
                direction = XdownYdown;
            }
        } else if (transformType == SHEAR && direction == XdownYdown) {
            sy -= .05;
            sx -= .05;
            if (sy < 0) {
                direction = RIGHT;
                sx = sy = 0.0;
                transformToggle = ROTATE;
            }
        }

        if (transformType == ROTATE) {
            angdeg += 5;
            if (angdeg == 360) {
                angdeg = 0;
                transformToggle = SCALE;
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        Font font = g2.getFont();
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout tl = new TextLayout(title[transformType], font, frc);
        g2.setColor(BLACK);
        tl.draw(g2, (float) (w / 2 - tl.getBounds().getWidth() / 2),
                (tl.getAscent() + tl.getDescent()));

        if (transformType == ROTATE) {
            String s = Double.toString(angdeg);
            g2.drawString("angdeg=" + s, 2, h - 4);
        } else {
            String s = Double.toString(sx);
            s = (s.length() < 5) ? s : s.substring(0, 5);
            TextLayout tlsx = new TextLayout("sx=" + s, font, frc);
            tlsx.draw(g2, 2, h - 4);

            s = Double.toString(sy);
            s = (s.length() < 5) ? s : s.substring(0, 5);
            g2.drawString("sy=" + s, (int) (tlsx.getBounds().getWidth() + 4), h
                    - 4);
        }

        if (transformType == SCALE) {
            g2.translate(w / 2 - iw / 2, h / 2 - ih / 2);
            g2.scale(sx, sy);
        } else if (transformType == SHEAR) {
            g2.translate(w / 2 - iw / 2, h / 2 - ih / 2);
            g2.shear(sx, sy);
        } else {
            g2.rotate(Math.toRadians(angdeg), w / 2, h / 2);
            g2.translate(w / 2 - iw / 2, h / 2 - ih / 2);
        }

        g2.setColor(ORANGE);
        g2.fillRect(0, 0, iw + 10, ih + 10);
        g2.drawImage(original, 5, 5, iw, ih, ORANGE, this);

    }

    public static void main(String[] argv) {
        createDemoFrame(new SelectTx());
    }


    static final class DemoControls extends CustomControls implements
            ActionListener {

        SelectTx demo;
        JToolBar toolbar;

        public DemoControls(SelectTx demo) {
            super(demo.name);
            this.demo = demo;
            add(toolbar = new JToolBar());
            toolbar.setFloatable(false);
            addTool("Scale", false);
            addTool("Shear", true);
            addTool("Rotate", false);
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
            for (int i = 0; i < toolbar.getComponentCount(); i++) {
                JToggleButton b = (JToggleButton) toolbar.getComponentAtIndex(i);
                b.setSelected(false);
            }
            JToggleButton b = (JToggleButton) e.getSource();
            b.setSelected(true);
            if (b.getText().equals("Scale")) {
                demo.transformType = SelectTx.SCALE;
                demo.direction = SelectTx.RIGHT;
                demo.sx = demo.sy = 1;
            } else if (b.getText().equals("Shear")) {
                demo.transformType = SelectTx.SHEAR;
                demo.direction = SelectTx.RIGHT;
                demo.sx = demo.sy = 0;
            } else if (b.getText().equals("Rotate")) {
                demo.transformType = SelectTx.ROTATE;
                demo.angdeg = 0;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 39);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            demo.transformToggle = demo.transformType;
            while (thread == me) {
                try {
                    Thread.sleep(222);
                } catch (InterruptedException e) {
                    return;
                }
                if (demo.transformToggle != demo.transformType) {
                    ((AbstractButton) toolbar.getComponentAtIndex(
                            demo.transformToggle)).doClick();
                }
            }
            thread = null;
        }
    } // End DemoControls class
} // End SelectTx class

