/*
* Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.swingset3.demos.colorchooser;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.*;

import static com.sun.swingset3.demos.colorchooser.BezierAnimationPanel.BezierColor.*;

/**
 * BezierAnimationPanel
 *
 * @author Jim Graham
 * @author Jeff Dinkins (removed dynamic setting changes, made swing friendly)
 * @version 1.16 11/17/05
 */
public class BezierAnimationPanel extends JPanel implements Runnable {
    public static enum BezierColor {
        BACKGROUND, OUTER, GRADIENT_A, GRADIENT_B
    }

    private final Map<BezierColor, Color> colors = new Hashtable<BezierColor, Color>();

    private GradientPaint gradient = null;

    private static final int NUMPTS = 6;

    private final float[] animpts = new float[NUMPTS * 2];

    private final float[] deltas = new float[NUMPTS * 2];

    private BufferedImage img;

    private Thread anim;

    private final Object lock = new Object();

    /**
     * BezierAnimationPanel Constructor
     */
    public BezierAnimationPanel() {
        setOpaque(true);

        colors.put(BACKGROUND, new Color(0, 0, 153));
        colors.put(OUTER, new Color(255, 255, 255));
        colors.put(GRADIENT_A, new Color(255, 0, 101));
        colors.put(GRADIENT_B, new Color(255, 255, 0));

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if (isShowing()) {
                    start();
                } else {
                    stop();
                }
            }
        });
    }

    public Color getBezierColor(BezierColor bezierColor) {
        return colors.get(bezierColor);
    }

    public void setBezierColor(BezierColor bezierColor, Color value) {
        if (value != null) {
            colors.put(bezierColor, value);
        }
    }

    public void start() {
        Dimension size = getSize();
        for (int i = 0; i < animpts.length; i += 2) {
            animpts[i] = (float) (Math.random() * size.width);
            animpts[i + 1] = (float) (Math.random() * size.height);
            deltas[i] = (float) (Math.random() * 4.0 + 2.0);
            deltas[i + 1] = (float) (Math.random() * 4.0 + 2.0);
            if (animpts[i] > size.width / 6.0f) {
                deltas[i] = -deltas[i];
            }
            if (animpts[i + 1] > size.height / 6.0f) {
                deltas[i + 1] = -deltas[i + 1];
            }
        }
        anim = new Thread(this);
        anim.setPriority(Thread.MIN_PRIORITY);
        anim.start();
    }

    public synchronized void stop() {
        anim = null;
        notify();
    }

    private static void animate(float[] pts, float[] deltas, int index, int limit) {
        float newpt = pts[index] + deltas[index];
        if (newpt <= 0) {
            newpt = -newpt;
            deltas[index] = (float) (Math.random() * 3.0 + 2.0);
        } else if (newpt >= (float) limit) {
            newpt = 2.0f * limit - newpt;
            deltas[index] = -(float) (Math.random() * 3.0 + 2.0);
        }
        pts[index] = newpt;
    }

    public void run() {
        Thread me = Thread.currentThread();
        while (getSize().width <= 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }

        Graphics2D g2d = null;
        Graphics2D bufferG2D = null;
        BasicStroke solid = new BasicStroke(9.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 9.0f);
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        int rule = AlphaComposite.SRC_OVER;
        AlphaComposite opaque = AlphaComposite.SrcOver;
        AlphaComposite blend = AlphaComposite.getInstance(rule, 0.9f);
        AlphaComposite set = AlphaComposite.Src;
        Dimension oldSize = getSize();
        Shape clippath = null;
        while (anim == me) {
            Dimension size = getSize();
            if (size.width != oldSize.width || size.height != oldSize.height) {
                img = null;
                clippath = null;
                if (bufferG2D != null) {
                    bufferG2D.dispose();
                    bufferG2D = null;
                }
            }
            oldSize = size;

            if (img == null) {
                img = (BufferedImage) createImage(size.width, size.height);
            }

            if (bufferG2D == null) {
                bufferG2D = img.createGraphics();
                bufferG2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
                bufferG2D.setClip(clippath);
            }
            g2d = bufferG2D;

            float[] ctrlpts;
            for (int i = 0; i < animpts.length; i += 2) {
                animate(animpts, deltas, i, size.width);
                animate(animpts, deltas, i + 1, size.height);
            }
            ctrlpts = animpts;
            int len = ctrlpts.length;
            gp.reset();
            float prevx = ctrlpts[len - 2];
            float prevy = ctrlpts[len - 1];
            float curx = ctrlpts[0];
            float cury = ctrlpts[1];
            float midx = (curx + prevx) / 2.0f;
            float midy = (cury + prevy) / 2.0f;
            gp.moveTo(midx, midy);
            for (int i = 2; i <= ctrlpts.length; i += 2) {
                float x1 = (midx + curx) / 2.0f;
                float y1 = (midy + cury) / 2.0f;
                prevx = curx;
                prevy = cury;
                if (i < ctrlpts.length) {
                    curx = ctrlpts[i];
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

            synchronized (lock) {
                g2d.setComposite(set);
                g2d.setBackground(getBezierColor(BACKGROUND));
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                // g2d.clearRect(bounds.x-5, bounds.y-5, bounds.x + bounds.width
                // + 5, bounds.y + bounds.height + 5);
                g2d.clearRect(0, 0, getWidth(), getHeight());

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBezierColor(OUTER));
                g2d.setComposite(opaque);
                g2d.setStroke(solid);
                g2d.draw(gp);
                g2d.setPaint(gradient);

                Rectangle bounds = gp.getBounds();

                gradient = new GradientPaint(bounds.x, bounds.y, getBezierColor(GRADIENT_A), bounds.x + bounds.width,
                        bounds.y + bounds.height, getBezierColor(GRADIENT_B), true);

                g2d.setComposite(blend);
                g2d.fill(gp);
            }

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        repaint();
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (g2d != null) {
            g2d.dispose();
        }
    }

    public void paint(Graphics g) {
        synchronized (lock) {
            Graphics2D g2d = (Graphics2D) g;
            if (img != null) {
                g2d.setComposite(AlphaComposite.Src);
                g2d.drawImage(img, null, 0, 0);
            }
        }
    }
}
