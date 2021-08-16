/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package renderperf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Robot;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class RenderPerfLCDTest {
    private static HashSet<String> ignoredTests = new HashSet<>();

    private final static int N = 1000;
    private final static float WIDTH = 800;
    private final static float HEIGHT = 800;
    private final static float R = 25;
    private final static int BW = 50;
    private final static int BH = 50;
    private final static int COUNT = 300;
    private final static int DELAY = 10;
    private final static int RESOLUTION = 5;
    private final static int COLOR_TOLERANCE = 10;
    private final static int MAX_MEASURE_TIME = 5000;


    interface Configurable {
        void configure(Graphics2D g2d);
    }

    interface Renderable {
        void setup(Graphics2D g2d);
        void render(Graphics2D g2d);
        void update();
    }

    static class Particles {
        private float[] bx;
        private float[] by;
        private float[] vx;
        private float[] vy;
        private float r;
        private int n;

        private float x0;
        private float y0;
        private float width;
        private float height;

        Particles(int n, float r, float x0, float y0, float width, float height) {
            bx = new float[n];
            by = new float[n];
            vx = new float[n];
            vy = new float[n];
            this.n = n;
            this.r = r;
            this.x0 = x0;
            this.y0 = y0;
            this.width = width;
            this.height = height;
            for (int i = 0; i < n; i++) {
                bx[i] = (float) (x0 + r + 0.1 + Math.random() * (width - 2 * r - 0.2 - x0));
                by[i] = (float) (y0 + r + 0.1 + Math.random() * (height - 2 * r - 0.2 - y0));
                vx[i] = 0.1f * (float) (Math.random() * 2 * r - r);
                vy[i] = 0.1f * (float) (Math.random() * 2 * r - r);
            }

        }

        void render(Graphics2D g2d, ParticleRenderer renderer) {
            for (int i = 0; i < n; i++) {
                renderer.render(g2d, i, bx, by, vx, vy);
            }
        }

        void update() {
            for (int i = 0; i < n; i++) {
                bx[i] += vx[i];
                if (bx[i] + r > width || bx[i] - r < x0) vx[i] = -vx[i];
                by[i] += vy[i];
                if (by[i] + r > height || by[i] - r < y0) vy[i] = -vy[i];
            }

        }

    }

    ParticleRenderable createPR(ParticleRenderer renderer) {
        return new ParticleRenderable(renderer);
    }

    static class ParticleRenderable implements Renderable {
        ParticleRenderer renderer;
        Configurable configure;

        ParticleRenderable(ParticleRenderer renderer, Configurable configure) {
            this.renderer = renderer;
            this.configure = configure;
        }

        ParticleRenderable(ParticleRenderer renderer) {
            this(renderer, null);
        }

        @Override
        public void setup(Graphics2D g2d) {
            if (configure != null) configure.configure(g2d);
        }

        @Override
        public void render(Graphics2D g2d) {
            balls.render(g2d, renderer);
        }

        @Override
        public void update() {
            balls.update();
        }

        public ParticleRenderable configure(Configurable configure) {
            this.configure = configure;
            return this;
        }
    }

    interface ParticleRenderer {
        void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy);

    }

    static class WhiteTextParticleRenderer implements ParticleRenderer {
        float r;

        WhiteTextParticleRenderer(float r) {
            this.r = r;
        }

        void setPaint(Graphics2D g2d, int id) {
            g2d.setColor(Color.WHITE);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            setPaint(g2d, id);
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)(y[id] - r));
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)y[id]);
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)(y[id] + r));
        }
    }

    static class TextParticleRenderer extends WhiteTextParticleRenderer {
        Color[] colors;

        float r;

        TextParticleRenderer(int n, float r) {
            super(r);
            colors = new Color[n];
            this.r = r;
            for (int i = 0; i < n; i++) {
                colors[i] = new Color((float) Math.random(),
                        (float) Math.random(), (float) Math.random());
            }
        }

        void setPaint(Graphics2D g2d, int id) {
            g2d.setColor(colors[id % colors.length]);
        }
    }

    static class PerfMeter {
        private String name;
        private int frame = 0;

        private JPanel panel;

        private long time;
        private double execTime = 0;
        private Color expColor = Color.RED;
        AtomicBoolean waiting = new AtomicBoolean(false);
        private double fps;

        PerfMeter(String name) {
            this.name = name;
        }

        PerfMeter exec(final Renderable renderable) throws Exception {
            final CountDownLatch latch = new CountDownLatch(COUNT);
            final CountDownLatch latchFrame = new CountDownLatch(1);
            final long endTime = System.currentTimeMillis() + MAX_MEASURE_TIME;

            final Frame f = new Frame();
            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    latchFrame.countDown();
                }
            });

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {

                    panel = new JPanel()
                    {
                        @Override
                        protected void paintComponent(Graphics g) {

                            super.paintComponent(g);
                            time = System.nanoTime();
                            Graphics2D g2d = (Graphics2D) g.create();
                            renderable.setup(g2d);
                            renderable.render(g2d);
                            g2d.setColor(expColor);
                            g.fillRect(0, 0, BW, BH);
                        }
                    };

                    panel.setPreferredSize(new Dimension((int)(WIDTH + BW), (int)(HEIGHT + BH)));
                    panel.setBackground(Color.BLACK);
                    f.add(panel);
                    //f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    f.pack();
                    f.setVisible(true);
                }
            });
            Robot robot = new Robot();

            Timer timer = new Timer(DELAY, e -> {

                if (waiting.compareAndSet(false, true)) {
                    Color c = robot.getPixelColor(
                            panel.getTopLevelAncestor().getX() + panel.getTopLevelAncestor().getInsets().left + BW / 2,
                            panel.getTopLevelAncestor().getY() + panel.getTopLevelAncestor().getInsets().top + BW / 2);
                    if (isAlmostEqual(c, Color.BLUE)) {
                        expColor = Color.RED;
                    } else {
                        expColor = Color.BLUE;
                    }
                    renderable.update();
                    panel.getParent().repaint();

                } else {
                    while (!isAlmostEqual(
                            robot.getPixelColor(
                                    panel.getTopLevelAncestor().getX() + panel.getTopLevelAncestor().getInsets().left + BW/2,
                                    panel.getTopLevelAncestor().getY() + panel.getTopLevelAncestor().getInsets().top + BH/2),
                            expColor))
                    {
                        try {
                            Thread.sleep(RESOLUTION);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    time = System.nanoTime() - time;
                    execTime += time;
                    frame++;
                    waiting.set(false);
                }

                if (System.currentTimeMillis() < endTime) {
                    latch.countDown();
                } else {
                    while(latch.getCount() > 0) latch.countDown();
                }
            });
            timer.start();
            latch.await();
            SwingUtilities.invokeAndWait(() -> {
                timer.stop();
                f.setVisible(false);
                f.dispose();
            });

            latchFrame.await();
            if (execTime != 0 && frame != 0) {
                fps = 1e9 / (execTime / frame);
            } else {
                fps = 0;
            }

            return this;
        }

        private void report() {
            System.err.println(name + " : " + String.format("%.2f FPS", fps));
        }

        private boolean isAlmostEqual(Color c1, Color c2) {
            return Math.abs(c1.getRed() - c2.getRed()) < COLOR_TOLERANCE ||
                    Math.abs(c1.getGreen() - c2.getGreen()) < COLOR_TOLERANCE ||
                    Math.abs(c1.getBlue() - c2.getBlue()) < COLOR_TOLERANCE;

        }
    }

    private static final Particles balls = new Particles(N, R, BW, BH, WIDTH, HEIGHT);
    private static final ParticleRenderer textRenderer = new TextParticleRenderer(N, R);

    private static final Configurable TextLCD = (Graphics2D g2d) ->
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

    public void testTextBubblesLCD() throws Exception {
        (new PerfMeter("TextLCD")).exec(createPR(textRenderer).configure(TextLCD)).report();
    }

    public static void main(String[] args)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        RenderPerfLCDTest test = new RenderPerfLCDTest();

        if (args.length > 0) {
            for (String testCase : args) {
                Method m = RenderPerfLCDTest.class.getDeclaredMethod(testCase);
                m.invoke(test);
            }
        } else {
            Method[] methods = RenderPerfLCDTest.class.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("test") && !ignoredTests.contains(m.getName())) {
                    m.invoke(test);
                }
            }
        }
    }
}
