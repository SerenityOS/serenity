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
package java2d.demos.Paint;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import javax.swing.JComboBox;


/**
 * GradientPaint animation.
 */
@SuppressWarnings("serial")
public class GradAnim extends AnimatingControlsSurface {

    private static final int BASIC_GRADIENT = 0;
    private static final int LINEAR_GRADIENT = 1;
    private static final int RADIAL_GRADIENT = 2;
    private static final int FOCUS_GRADIENT = 3;
    private static final int MAX_HUE = 256 * 6;
    private animval x1, y1, x2, y2;
    private int hue = (int) (Math.random() * MAX_HUE);
    private int gradientType;

    public GradAnim() {
        setBackground(Color.white);
        setControls(new Component[] { new DemoControls(this) });
        x1 = new animval(0, 300, 2, 10);
        y1 = new animval(0, 300, 2, 10);
        x2 = new animval(0, 300, 2, 10);
        y2 = new animval(0, 300, 2, 10);
        gradientType = BASIC_GRADIENT;
    }

    @Override
    public void reset(int w, int h) {
        x1.newlimits(0, w);
        y1.newlimits(0, h);
        x2.newlimits(0, w);
        y2.newlimits(0, h);
    }

    @Override
    public void step(int w, int h) {
        x1.anim();
        y1.anim();
        x2.anim();
        y2.anim();
        hue = (hue + (int) (Math.random() * 10)) % MAX_HUE;
    }

    public static Color getColor(int hue) {
        int leg = (hue / 256) % 6;
        int step = (hue % 256) * 2;
        int falling = (step < 256) ? 255 : 511 - step;
        int rising = (step < 256) ? step : 255;
        int r, g, b;
        r = g = b = 0;
        switch (leg) {
            case 0:
                r = 255;
                break;
            case 1:
                r = falling;
                g = rising;
                break;
            case 2:
                g = 255;
                break;
            case 3:
                g = falling;
                b = rising;
                break;
            case 4:
                b = 255;
                break;
            case 5:
                b = falling;
                r = rising;
                break;
        }
        return new Color(r, g, b);
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        float fx1 = x1.getFlt();
        float fy1 = y1.getFlt();
        float fx2 = x2.getFlt();
        float fy2 = y2.getFlt();

        if ((fx1 == fx2) && (fy1 == fy2)) {
            // just to prevent the points from being coincident
            fx2++;
            fy2++;
        }

        Color c1 = getColor(hue);
        Color c2 = getColor(hue + 256 * 3);
        Paint gp;

        switch (gradientType) {
            case BASIC_GRADIENT:
            default:
                gp = new GradientPaint(fx1, fy1, c1,
                        fx2, fy2, c2,
                        true);
                break;
            case LINEAR_GRADIENT: {
                float[] fractions = new float[] { 0.0f, 0.2f, 1.0f };
                Color c3 = getColor(hue + 256 * 2);
                Color[] colors = new Color[] { c1, c2, c3 };
                gp = new LinearGradientPaint(fx1, fy1,
                        fx2, fy2,
                        fractions, colors,
                        CycleMethod.REFLECT);
            }
            break;

            case RADIAL_GRADIENT: {
                float[] fractions = { 0.0f, 0.2f, 0.8f, 1.0f };
                Color c3 = getColor(hue + 256 * 2);
                Color c4 = getColor(hue + 256 * 4);
                Color[] colors = new Color[] { c1, c2, c3, c4 };
                float radius = (float) Point2D.distance(fx1, fy1, fx2, fy2);
                gp = new RadialGradientPaint(fx1, fy1, radius,
                        fractions, colors,
                        CycleMethod.REFLECT);
            }
            break;

            case FOCUS_GRADIENT: {
                float[] fractions = { 0.0f, 0.2f, 0.8f, 1.0f };
                Color c3 = getColor(hue + 256 * 4);
                Color c4 = getColor(hue + 256 * 2);
                Color[] colors = new Color[] { c1, c2, c3, c4 };
                float radius = (float) Point2D.distance(fx1, fy1, fx2, fy2);
                float max = Math.max(w, h);
                // This function will map the smallest radius to
                // max/10 when the points are next to each other,
                // max when the points are max distance apart,
                // and >max when they are further apart (in which
                // case the focus clipping code in RGP will clip
                // the focus to be inside the radius).
                radius = max * (((radius / max) * 0.9f) + 0.1f);
                gp = new RadialGradientPaint(fx2, fy2, radius,
                        fx1, fy1,
                        fractions, colors,
                        CycleMethod.REPEAT);
            }
            break;
        }
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Color.yellow);
        g2.drawLine(x1.getInt(), y1.getInt(), x2.getInt(), y2.getInt());
    }


    public final class animval {

        float curval;
        float lowval;
        float highval;
        float currate;
        float lowrate;
        float highrate;

        public animval(int lowval, int highval,
                int lowrate, int highrate) {
            this.lowval = lowval;
            this.highval = highval;
            this.lowrate = lowrate;
            this.highrate = highrate;
            this.curval = randval(lowval, highval);
            this.currate = randval(lowrate, highrate);
        }

        public float randval(float low, float high) {
            return (float) (low + Math.random() * (high - low));
        }

        public float getFlt() {
            return curval;
        }

        public int getInt() {
            return (int) curval;
        }

        public void anim() {
            curval += currate;
            clip();
        }

        public void clip() {
            if (curval > highval) {
                curval = highval - (curval - highval);
                if (curval < lowval) {
                    curval = highval;
                }
                currate = -randval(lowrate, highrate);
            } else if (curval < lowval) {
                curval = lowval + (lowval - curval);
                if (curval > highval) {
                    curval = lowval;
                }
                currate = randval(lowrate, highrate);
            }
        }

        public void newlimits(int lowval, int highval) {
            this.lowval = lowval;
            this.highval = highval;
            clip();
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new GradAnim());
    }


    class DemoControls extends CustomControls implements ActionListener {

        GradAnim demo;
        JComboBox<String> combo;

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(GradAnim demo) {
            super(demo.name);
            this.demo = demo;
            combo = new JComboBox<>();
            combo.addActionListener(this);
            combo.addItem("2-color GradientPaint");
            combo.addItem("3-color LinearGradientPaint");
            combo.addItem("4-color RadialGradientPaint");
            combo.addItem("4-color RadialGradientPaint with focus");
            combo.setSelectedIndex(0);
            add(combo);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int index = combo.getSelectedIndex();
            if (index >= 0) {
                demo.gradientType = index;
            }
            if (!demo.animating.running()) {
                demo.repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 41);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    combo.setSelectedIndex(i);
                    try {
                        Thread.sleep(4444);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            thread = null;
        }
    }
}
