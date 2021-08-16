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
package java2d.demos.Arcs_Curves;


import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


/**
 * Animated Bezier Curve with controls for different draw & fill paints.
 */
@SuppressWarnings("serial")
public class BezierAnim extends AnimatingControlsSurface {

    private static final int NUMPTS = 6;
    protected BasicStroke solid = new BasicStroke(10.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    protected BasicStroke dashed = new BasicStroke(10.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[] { 5 },
            0);
    private float[] animpts = new float[NUMPTS * 2];
    private float[] deltas = new float[NUMPTS * 2];
    protected Paint fillPaint, drawPaint;
    protected boolean doFill = true;
    protected boolean doDraw = true;
    protected GradientPaint gradient;
    protected BasicStroke stroke;

    public BezierAnim() {
        setBackground(WHITE);
        gradient = new GradientPaint(0, 0, RED, 200, 200, YELLOW);
        fillPaint = gradient;
        drawPaint = BLUE;
        stroke = solid;
        setControls(new Component[] { new DemoControls(this) });
    }

    public void animate(float[] pts, float[] deltas, int index, int limit) {
        float newpt = pts[index] + deltas[index];
        if (newpt <= 0) {
            newpt = -newpt;
            deltas[index] = (float) (Math.random() * 4.0 + 2.0);
        } else if (newpt >= limit) {
            newpt = 2.0f * limit - newpt;
            deltas[index] = -(float) (Math.random() * 4.0 + 2.0);
        }
        pts[index] = newpt;
    }

    @Override
    public void reset(int w, int h) {
        for (int i = 0; i < animpts.length; i += 2) {
            animpts[i + 0] = (float) (Math.random() * w);
            animpts[i + 1] = (float) (Math.random() * h);
            deltas[i + 0] = (float) (Math.random() * 6.0 + 4.0);
            deltas[i + 1] = (float) (Math.random() * 6.0 + 4.0);
            if (animpts[i + 0] > w / 2.0f) {
                deltas[i + 0] = -deltas[i + 0];
            }
            if (animpts[i + 1] > h / 2.0f) {
                deltas[i + 1] = -deltas[i + 1];
            }
        }
        gradient = new GradientPaint(0, 0, RED, w * .7f, h * .7f, YELLOW);
    }

    @Override
    public void step(int w, int h) {
        for (int i = 0; i < animpts.length; i += 2) {
            animate(animpts, deltas, i + 0, w);
            animate(animpts, deltas, i + 1, h);
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        float[] ctrlpts = animpts;
        int len = ctrlpts.length;
        float prevx = ctrlpts[len - 2];
        float prevy = ctrlpts[len - 1];
        float curx = ctrlpts[0];
        float cury = ctrlpts[1];
        float midx = (curx + prevx) / 2.0f;
        float midy = (cury + prevy) / 2.0f;
        GeneralPath gp = new GeneralPath(Path2D.WIND_NON_ZERO);
        gp.moveTo(midx, midy);
        for (int i = 2; i <= ctrlpts.length; i += 2) {
            float x1 = (midx + curx) / 2.0f;
            float y1 = (midy + cury) / 2.0f;
            prevx = curx;
            prevy = cury;
            if (i < ctrlpts.length) {
                curx = ctrlpts[i + 0];
                cury = ctrlpts[i + 1];
            } else {
                curx = ctrlpts[0];
                cury = ctrlpts[1];
            }
            midx = (curx + prevx) / 2.0f;
            midy = (cury + prevy) / 2.0f;
            float x2 = (prevx + midx) / 2.0f;
            float y2 = (prevy + midy) / 2.0f;
            gp.curveTo(x1, y1, x2, y2, midx, midy);
        }
        gp.closePath();
        if (doDraw) {
            g2.setPaint(drawPaint);
            g2.setStroke(stroke);
            g2.draw(gp);
        }
        if (doFill) {
            if (fillPaint instanceof GradientPaint) {
                fillPaint = gradient;
            }
            g2.setPaint(fillPaint);
            g2.fill(gp);
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new BezierAnim());
    }


    static class DemoControls extends CustomControls implements ActionListener {

        static final TexturePaint tp1, tp2;

        static {
            BufferedImage bi = new BufferedImage(2, 1,
                    BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, 0xff00ff00);
            bi.setRGB(1, 0, 0xffff0000);
            tp1 = new TexturePaint(bi, new Rectangle(0, 0, 2, 1));
            bi = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, 0xff0000ff);
            bi.setRGB(1, 0, 0xffff0000);
            tp2 = new TexturePaint(bi, new Rectangle(0, 0, 2, 1));
        }
        BezierAnim demo;
        static Paint[] drawPaints = { new Color(0, 0, 0, 0), BLUE, new Color(0,
            0, 255, 126),
            BLUE, tp2 };
        static String[] drawName = { "No Draw", "Blue", "Blue w/ Alpha",
            "Blue Dash", "Texture" };
        static Paint[] fillPaints = { new Color(0, 0, 0, 0), GREEN, new Color(0,
            255, 0, 126),
            tp1, new GradientPaint(0, 0, RED, 30, 30, YELLOW) };
        String[] fillName = { "No Fill", "Green", "Green w/ Alpha", "Texture",
            "Gradient" };
        JMenu fillMenu, drawMenu;
        JMenuItem[] fillMI = new JMenuItem[fillPaints.length];
        JMenuItem[] drawMI = new JMenuItem[drawPaints.length];
        PaintedIcon[] fillIcons = new PaintedIcon[fillPaints.length];
        PaintedIcon[] drawIcons = new PaintedIcon[drawPaints.length];
        Font font = new Font(Font.SERIF, Font.PLAIN, 10);

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(BezierAnim demo) {
            super(demo.name);
            this.demo = demo;

            JMenuBar drawMenuBar = new JMenuBar();
            add(drawMenuBar);

            JMenuBar fillMenuBar = new JMenuBar();
            add(fillMenuBar);

            drawMenu = drawMenuBar.add(new JMenu("Draw Choice"));
            drawMenu.setFont(font);

            for (int i = 0; i < drawPaints.length; i++) {
                drawIcons[i] = new PaintedIcon(drawPaints[i]);
                drawMI[i] = drawMenu.add(new JMenuItem(drawName[i]));
                drawMI[i].setFont(font);
                drawMI[i].setIcon(drawIcons[i]);
                drawMI[i].addActionListener(this);
            }
            drawMenu.setIcon(drawIcons[1]);

            fillMenu = fillMenuBar.add(new JMenu("Fill Choice"));
            fillMenu.setFont(font);
            for (int i = 0; i < fillPaints.length; i++) {
                fillIcons[i] = new PaintedIcon(fillPaints[i]);
                fillMI[i] = fillMenu.add(new JMenuItem(fillName[i]));
                fillMI[i].setFont(font);
                fillMI[i].setIcon(fillIcons[i]);
                fillMI[i].addActionListener(this);
            }
            fillMenu.setIcon(fillIcons[fillPaints.length - 1]);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object obj = e.getSource();
            for (int i = 0; i < fillPaints.length; i++) {
                if (obj.equals(fillMI[i])) {
                    demo.doFill = true;
                    demo.fillPaint = fillPaints[i];
                    fillMenu.setIcon(fillIcons[i]);
                    break;
                }
            }
            for (int i = 0; i < drawPaints.length; i++) {
                if (obj.equals(drawMI[i])) {
                    demo.doDraw = true;
                    demo.drawPaint = drawPaints[i];
                    if (((JMenuItem) obj).getText().endsWith("Dash")) {
                        demo.stroke = demo.dashed;
                    } else {
                        demo.stroke = demo.solid;
                    }
                    drawMenu.setIcon(drawIcons[i]);
                    break;
                }
            }
            if (obj.equals(fillMI[0])) {
                demo.doFill = false;
            } else if (obj.equals(drawMI[0])) {
                demo.doDraw = false;
            }
            if (!demo.animating.running()) {
                demo.repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 36);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (JMenuItem dmi : drawMI) {
                    dmi.doClick();
                    for (JMenuItem fmi : fillMI) {
                        fmi.doClick();
                        try {
                            Thread.sleep(3000 + (long) (Math.random() * 3000));
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
            thread = null;
        }


        static class PaintedIcon implements Icon {

            Paint paint;

            public PaintedIcon(Paint p) {
                this.paint = p;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(paint);
                g2.fillRect(x, y, getIconWidth(), getIconHeight());
                g2.setColor(GRAY);
                g2.draw3DRect(x, y, getIconWidth() - 1, getIconHeight() - 1,
                        true);
            }

            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        } // End PaintedIcon class
    } // End DemoControls class
} // End BezierAnim class

