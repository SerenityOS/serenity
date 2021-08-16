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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Robot;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class RenderPerfTest {
    private static HashSet<String> ignoredTests = new HashSet<>();

    static {
        ignoredTests.add("testWiredBoxAA");
    }

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

    static class FlatParticleRenderer implements ParticleRenderer {
        Color[] colors;
        float r;

        FlatParticleRenderer(int n, float r) {
            colors = new Color[n];
            this.r = r;
            for (int i = 0; i < n; i++) {
                colors[i] = new Color((float) Math.random(),
                        (float) Math.random(), (float) Math.random());
            }
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            g2d.fillOval((int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r));
        }

    }

    static class ClipFlatParticleRenderer extends FlatParticleRenderer {

        ClipFlatParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            if ((id % 10) == 0) {
                g2d.setColor(colors[id % colors.length]);
                g2d.setClip(new Ellipse2D.Double((int) (x[id] - r), (int) (y[id] - r), (int) (2 * r), (int) (2 * r)));
                g2d.fillRect((int) (x[id] - 2 * r), (int) (y[id] - 2 * r), (int) (4 * r), (int) (4 * r));
            }
        }

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

    static class LargeTextParticleRenderer extends TextParticleRenderer {

        LargeTextParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            setPaint(g2d, id);
            Font font = new Font("LucidaGrande", Font.PLAIN, 32);
            g2d.setFont(font);
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)(y[id] - r));
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)y[id]);
            g2d.drawString("The quick brown fox jumps over the lazy dog",
                    (int)(x[id] - r), (int)(y[id] + r));
        }
    }

    static class FlatOvalRotParticleRenderer extends FlatParticleRenderer {


        FlatOvalRotParticleRenderer(int n, float r) {
            super(n, r);
        }

        void setPaint(Graphics2D g2d, int id) {
            g2d.setColor(colors[id % colors.length]);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            setPaint(g2d, id);
            if (Math.abs(vx[id] + vy[id]) > 0.001) {
                AffineTransform t = (AffineTransform) g2d.getTransform().clone();
                double l = vx[id] / Math.sqrt(vx[id] * vx[id] + vy[id] * vy[id]);
                if (vy[id] < 0) {
                    l = -l;
                }
                g2d.translate(x[id], y[id]);
                g2d.rotate(Math.acos(l));
                g2d.fillOval(-(int)r, (int)(-0.5*r), (int) (2 * r), (int)r);
                g2d.setTransform(t);
            } else {
                g2d.fillOval((int)(x[id] - r), (int)(y[id] - 0.5*r),
                        (int) (2 * r), (int) r);
            }
        }
    }

    static class LinGradOvalRotParticleRenderer extends FlatOvalRotParticleRenderer {


        LinGradOvalRotParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        void setPaint(Graphics2D g2d, int id) {
            Point2D start = new Point2D.Double(- r,  - 0.5*r);
            Point2D end = new Point2D.Double( 2 * r, r);
            float[] dist = {0.0f, 1.0f};
            Color[] cls = {colors[id %colors.length], colors[(colors.length - id) %colors.length]};
            LinearGradientPaint p =
                    new LinearGradientPaint(start, end, dist, cls);
            g2d.setPaint(p);
        }
    }

    static class LinGrad3OvalRotParticleRenderer extends FlatOvalRotParticleRenderer {


        LinGrad3OvalRotParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        void setPaint(Graphics2D g2d, int id) {
            Point2D start = new Point2D.Double(- r,  - 0.5*r);
            Point2D end = new Point2D.Double( 2 * r, r);
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] cls = {
                colors[id %colors.length],
                colors[(colors.length - id) %colors.length],
                colors[(id*5) %colors.length]};
            LinearGradientPaint p =
                new LinearGradientPaint(start, end, dist, cls);
            g2d.setPaint(p);
        }
    }

    static class RadGrad3OvalRotParticleRenderer extends FlatOvalRotParticleRenderer {


        RadGrad3OvalRotParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        void setPaint(Graphics2D g2d, int id) {
            Point2D start = new Point2D.Double();
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] cls = {
                colors[id %colors.length],
                colors[(colors.length - id) %colors.length],
                colors[(id*5) %colors.length]};
            RadialGradientPaint p =
                new RadialGradientPaint(start, r, dist, cls);
            g2d.setPaint(p);
        }
    }

    static class FlatBoxParticleRenderer extends FlatParticleRenderer {


        FlatBoxParticleRenderer(int n, float r) {
            super(n, r);
        }
        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            g2d.fillRect((int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r));

        }

    }

    static class ClipFlatBoxParticleRenderer extends FlatParticleRenderer {


        ClipFlatBoxParticleRenderer(int n, float r) {
            super(n, r);
        }
        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            if ((id % 10) == 0) {
                g2d.setColor(colors[id % colors.length]);
                g2d.setClip((int) (x[id] - r), (int) (y[id] - r), (int) (2 * r), (int) (2 * r));
                g2d.fillRect((int) (x[id] - 2 * r), (int) (y[id] - 2 * r), (int) (4 * r), (int) (4 * r));
            }
        }
    }

    static class ImgParticleRenderer extends FlatParticleRenderer {
        BufferedImage dukeImg;

        ImgParticleRenderer(int n, float r) {
            super(n, r);
            try {
                dukeImg = ImageIO.read(
                        Objects.requireNonNull(
                                RenderPerfTest.class.getClassLoader().getResourceAsStream(
                                        "renderperf/images/duke.png")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            g2d.drawImage(dukeImg, (int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r), null);
        }

    }

    static class FlatBoxRotParticleRenderer extends FlatParticleRenderer {


        FlatBoxRotParticleRenderer(int n, float r) {
            super(n, r);
        }
        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            if (Math.abs(vx[id] + vy[id]) > 0.001) {
                AffineTransform t = (AffineTransform) g2d.getTransform().clone();
                double l = vx[id] / Math.sqrt(vx[id] * vx[id] + vy[id] * vy[id]);
                if (vy[id] < 0) {
                    l = -l;
                }
                g2d.translate(x[id], y[id]);
                g2d.rotate(Math.acos(l));
                g2d.fillRect(-(int)r, -(int)r, (int) (2 * r), (int) (2 * r));
                g2d.setTransform(t);
            } else {
                g2d.fillRect((int)(x[id] - r), (int)(y[id] - r),
                        (int) (2 * r), (int) (2 * r));
            }
        }
    }

    static class WiredParticleRenderer extends FlatParticleRenderer {


        WiredParticleRenderer(int n, float r) {
            super(n, r);
        }
        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            g2d.drawOval((int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r));
        }

    }
    static class WiredBoxParticleRenderer extends FlatParticleRenderer {

        WiredBoxParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.setColor(colors[id % colors.length]);
            g2d.drawRect((int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r));
        }

    }
    static class SegParticleRenderer extends FlatParticleRenderer {

        SegParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            double v = Math.sqrt(vx[id]*vx[id]+vy[id]*vy[id]);
            float nvx = (float) (vx[id]/v);
            float nvy = (float) (vy[id]/v);
            g2d.setColor(colors[id % colors.length]);
            g2d.drawLine((int)(x[id] - r*nvx), (int)(y[id] - r*nvy),
                    (int)(x[id] + 2*r*nvx), (int)(y[id] + 2*r*nvy));
        }

    }


    static class WiredQuadParticleRenderer extends FlatParticleRenderer {

        WiredQuadParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            if (id > 2 && (id % 3) == 0) {
                g2d.setColor(colors[id % colors.length]);
                g2d.draw(new QuadCurve2D.Float(x[id-3], y[id-3], x[id-2], y[id-2], x[id-1], y[id-1]));
            }

        }
    }

    static class FlatQuadParticleRenderer extends FlatParticleRenderer {

        FlatQuadParticleRenderer(int n, float r) {
            super(n, r);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            if (id > 2 && (id % 3) == 0) {
                g2d.setColor(colors[id % colors.length]);
                g2d.fill(new QuadCurve2D.Float(x[id-3], y[id-3], x[id-2], y[id-2], x[id-1], y[id-1]));
            }

        }
    }

    static class BlitImageParticleRenderer extends FlatParticleRenderer {
        BufferedImage image;

        BlitImageParticleRenderer(int n, float r, BufferedImage img) {
            super(n, r);
            image = img;
            fill(image);
        }

        @Override
        public void render(Graphics2D g2d, int id, float[] x, float[] y, float[] vx, float[] vy) {
            g2d.drawImage(image, (int)(x[id] - r), (int)(y[id] - r), (int)(2*r), (int)(2*r), null);
        }

        private static void fill(final Image image) {
            final Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setComposite(AlphaComposite.Src);
            for (int i = 0; i < image.getHeight(null); ++i) {
                graphics.setColor(new Color(i, 0, 0));
                graphics.fillRect(0, i, image.getWidth(null), 1);
            }
            graphics.dispose();
        }

    }

    static class SwBlitImageParticleRenderer extends BlitImageParticleRenderer {

        SwBlitImageParticleRenderer(int n, float r, final int type) {
            super(n, r, makeUnmanagedBI(type));
        }

        private static BufferedImage makeUnmanagedBI(final int type) {
            final BufferedImage bi = new BufferedImage(17, 33, type);
            final DataBuffer db = bi.getRaster().getDataBuffer();
            if (db instanceof DataBufferInt) {
                ((DataBufferInt) db).getData();
            } else if (db instanceof DataBufferShort) {
                ((DataBufferShort) db).getData();
            } else if (db instanceof DataBufferByte) {
                ((DataBufferByte) db).getData();
            }
            bi.setAccelerationPriority(0.0f);
            return bi;
        }
    }

    static class SurfaceBlitImageParticleRenderer extends BlitImageParticleRenderer {

        SurfaceBlitImageParticleRenderer(int n, float r, final int type) {
            super(n, r, makeManagedBI(type));
        }

        private static BufferedImage makeManagedBI(final int type) {
            final BufferedImage bi = new BufferedImage(17, 33, type);
            bi.setAccelerationPriority(1.0f);
            return bi;
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

            final JFrame f = new JFrame();
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
                    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
    private static final ParticleRenderer flatRenderer = new FlatParticleRenderer(N, R);
    private static final ParticleRenderer clipFlatRenderer = new ClipFlatParticleRenderer(N, R);
    private static final ParticleRenderer flatOvalRotRenderer = new FlatOvalRotParticleRenderer(N, R);
    private static final ParticleRenderer flatBoxRenderer = new FlatBoxParticleRenderer(N, R);
    private static final ParticleRenderer clipFlatBoxParticleRenderer = new ClipFlatBoxParticleRenderer(N, R);
    private static final ParticleRenderer flatBoxRotRenderer = new FlatBoxRotParticleRenderer(N, R);
    private static final ParticleRenderer linGradOvalRotRenderer = new LinGradOvalRotParticleRenderer(N, R);
    private static final ParticleRenderer linGrad3OvalRotRenderer = new LinGrad3OvalRotParticleRenderer(N, R);
    private static final ParticleRenderer radGrad3OvalRotRenderer = new RadGrad3OvalRotParticleRenderer(N, R);
    private static final ParticleRenderer wiredRenderer = new WiredParticleRenderer(N, R);
    private static final ParticleRenderer wiredBoxRenderer = new WiredBoxParticleRenderer(N, R);
    private static final ParticleRenderer segRenderer = new SegParticleRenderer(N, R);
    private static final ParticleRenderer flatQuadRenderer = new FlatQuadParticleRenderer(N, R);
    private static final ParticleRenderer wiredQuadRenderer = new WiredQuadParticleRenderer(N, R);
    private static final ParticleRenderer imgRenderer = new ImgParticleRenderer(N, R);
    private static final ParticleRenderer textRenderer = new TextParticleRenderer(N, R);
    private static final ParticleRenderer largeTextRenderer = new LargeTextParticleRenderer(N, R);
    private static final ParticleRenderer whiteTextRenderer = new WhiteTextParticleRenderer(R);
    private static final ParticleRenderer argbSwBlitImageRenderer = new SwBlitImageParticleRenderer(N, R, BufferedImage.TYPE_INT_ARGB);
    private static final ParticleRenderer bgrSwBlitImageRenderer = new SwBlitImageParticleRenderer(N, R, BufferedImage.TYPE_INT_BGR);
    private static final ParticleRenderer argbSurfaceBlitImageRenderer = new SurfaceBlitImageParticleRenderer(N, R, BufferedImage.TYPE_INT_ARGB);
    private static final ParticleRenderer bgrSurfaceBlitImageRenderer = new SurfaceBlitImageParticleRenderer(N, R, BufferedImage.TYPE_INT_BGR);

    private static final Configurable AA = (Graphics2D g2d) ->
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

    private static final Configurable TextLCD = (Graphics2D g2d) ->
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

    private static final Configurable TextAA = (Graphics2D g2d) ->
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    private static final Configurable XORMode = (Graphics2D g2d) ->
        {g2d.setXORMode(Color.WHITE);};

    private static final Configurable XORModeLCDText = (Graphics2D g2d) ->
        {g2d.setXORMode(Color.WHITE);
         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);};


    public void testFlatOval() throws Exception {
        (new PerfMeter("FlatOval")).exec(createPR(flatRenderer)).report();
    }

    public void testFlatOvalAA() throws Exception {
        (new PerfMeter("FlatOvalAA")).exec(createPR(flatRenderer).configure(AA)).report();
    }

    public void testClipFlatOval() throws Exception {
        (new PerfMeter("ClipFlatOval")).exec(createPR(clipFlatRenderer)).report();
    }

    public void testClipFlatOvalAA() throws Exception {
        (new PerfMeter("ClipFlatOvalAA")).exec(createPR(clipFlatRenderer).configure(AA)).report();
    }

    public void testFlatBox() throws Exception {
        (new PerfMeter("FlatBox")).exec(createPR(flatBoxRenderer)).report();
    }

    public void testFlatBoxAA() throws Exception {
        (new PerfMeter("FlatBoxAA")).exec(createPR(flatBoxRenderer).configure(AA)).report();
    }

    public void testClipFlatBox() throws Exception {
        (new PerfMeter("ClipFlatBox")).exec(createPR(clipFlatBoxParticleRenderer)).report();
    }

    public void testClipFlatBoxAA() throws Exception {
        (new PerfMeter("ClipFlatBoxAA")).exec(createPR(clipFlatBoxParticleRenderer).configure(AA)).report();
    }

    public void testImage() throws Exception {
        (new PerfMeter("Image")).exec(createPR(imgRenderer)).report();
    }

    public void testImageAA() throws Exception {
        (new PerfMeter("ImageAA")).exec(createPR(imgRenderer).configure(AA)).report();
    }

    public void testRotatedBox() throws Exception {
        (new PerfMeter("RotatedBox")).exec(createPR(flatBoxRotRenderer)).report();
    }

    public void testRotatedBoxAA() throws Exception {
        (new PerfMeter("RotatedBoxAA")).exec(createPR(flatBoxRotRenderer).configure(AA)).report();
    }

    public void testRotatedOval() throws Exception {
        (new PerfMeter("RotatedOval")).exec(createPR(flatOvalRotRenderer)).report();
    }

    public void testRotatedOvalAA() throws Exception {
        (new PerfMeter("RotatedOvalAA")).exec(createPR(flatOvalRotRenderer).configure(AA)).report();
    }

    public void testLinGrad3RotatedOval() throws Exception {
        (new PerfMeter("LinGrad3RotatedOval")).exec(createPR(linGrad3OvalRotRenderer)).report();
    }

    public void testLinGrad3RotatedOvalAA() throws Exception {
        (new PerfMeter("LinGrad3RotatedOvalAA")).exec(createPR(linGrad3OvalRotRenderer).configure(AA)).report();
    }

    public void testRadGrad3RotatedOval() throws Exception {
        (new PerfMeter("RadGrad3RotatedOval")).exec(createPR(radGrad3OvalRotRenderer)).report();
    }

    public void testRadGrad3RotatedOvalAA() throws Exception {
        (new PerfMeter("RadGrad3RotatedOvalAA")).exec(createPR(radGrad3OvalRotRenderer).configure(AA)).report();
    }

    public void testLinGradRotatedOval() throws Exception {
        (new PerfMeter("LinGradRotatedOval")).exec(createPR(linGradOvalRotRenderer)).report();
    }

    public void testLinGradRotatedOvalAA() throws Exception {
        (new PerfMeter("LinGradRotatedOvalAA")).exec(createPR(linGradOvalRotRenderer).configure(AA)).report();
    }

    public void testWiredBubbles() throws Exception {
        (new PerfMeter("WiredBubbles")).exec(createPR(wiredRenderer)).report();
    }

    public void testWiredBubblesAA() throws Exception {
        (new PerfMeter("WiredBubblesAA")).exec(createPR(wiredRenderer).configure(AA)).report();
    }

    public void testWiredBox() throws Exception {
        (new PerfMeter("WiredBox")).exec(createPR(wiredBoxRenderer)).report();
    }

    public void testWiredBoxAA() throws Exception {
        (new PerfMeter("WiredBoxAA")).exec(createPR(wiredBoxRenderer).configure(AA)).report();
    }

    public void testLines() throws Exception {
        (new PerfMeter("Lines")).exec(createPR(segRenderer)).report();
    }

    public void testLinesAA() throws Exception {
        (new PerfMeter("LinesAA")).exec(createPR(segRenderer).configure(AA)).report();
    }

    public void testFlatQuad() throws Exception {
        (new PerfMeter("FlatQuad")).exec(createPR(flatQuadRenderer)).report();
    }

    public void testFlatQuadAA() throws Exception {
        (new PerfMeter("FlatQuadAA")).exec(createPR(flatQuadRenderer).configure(AA)).report();
    }

    public void testWiredQuad() throws Exception {
        (new PerfMeter("WiredQuad")).exec(createPR(wiredQuadRenderer)).report();
    }

    public void testWiredQuadAA() throws Exception {
        (new PerfMeter("WiredQuadAA")).exec(createPR(wiredQuadRenderer).configure(AA)).report();
    }

    public void testTextNoAA() throws Exception {
        (new PerfMeter("TextNoAA")).exec(createPR(textRenderer)).report();
    }

    public void testTextLCD() throws Exception {
        (new PerfMeter("TextLCD")).exec(createPR(textRenderer).configure(TextLCD)).report();
    }

    public void testTextGray() throws Exception {
        (new PerfMeter("TextGray")).exec(createPR(textRenderer).configure(TextAA)).report();
    }

    public void testLargeTextNoAA() throws Exception {
        (new PerfMeter("LargeTextNoAA")).exec(createPR(largeTextRenderer)).report();
    }

    public void testLargeTextLCD() throws Exception {
        (new PerfMeter("LargeTextLCD")).exec(createPR(largeTextRenderer).configure(TextLCD)).report();
    }

    public void testLargeTextGray() throws Exception {
        (new PerfMeter("LargeTextGray")).exec(createPR(largeTextRenderer).configure(TextAA)).report();
    }
    public void testWhiteTextNoAA() throws Exception {
        (new PerfMeter("WhiteTextNoAA")).exec(createPR(whiteTextRenderer)).report();
    }

    public void testWhiteTextLCD() throws Exception {
        (new PerfMeter("WhiteTextLCD")).exec(createPR(whiteTextRenderer).configure(TextLCD)).report();
    }

    public void testWhiteTextGray() throws Exception {
        (new PerfMeter("WhiteTextGray")).exec(createPR(whiteTextRenderer).configure(TextAA)).report();
    }

    public void testArgbSwBlitImage() throws Exception {
        (new PerfMeter("ArgbSwBlitImage")).exec(createPR(argbSwBlitImageRenderer)).report();
    }

    public void testBgrSwBlitImage() throws Exception {
        (new PerfMeter("BgrSwBlitImage")).exec(createPR(bgrSwBlitImageRenderer)).report();
    }

    public void testArgbSurfaceBlitImage() throws Exception {
        (new PerfMeter("ArgbSurfaceBlitImageRenderer")).exec(createPR(argbSurfaceBlitImageRenderer)).report();
    }

    public void testBgrSurfaceBlitImage() throws Exception {
        (new PerfMeter("BgrSurfaceBlitImage")).exec(createPR(bgrSurfaceBlitImageRenderer)).report();
    }

    public void testFlatOval_XOR() throws Exception {
        (new PerfMeter("FlatOval_XOR")).exec(createPR(flatRenderer).configure(XORMode)).report();
    }

    public void testRotatedBox_XOR() throws Exception {
        (new PerfMeter("RotatedBox_XOR")).exec(createPR(flatBoxRotRenderer).configure(XORMode)).report();
    }

    public void testLines_XOR() throws Exception {
        (new PerfMeter("Lines_XOR")).exec(createPR(segRenderer).configure(XORMode)).report();
    }

    public void testImage_XOR() throws Exception {
        (new PerfMeter("Image_XOR")).exec(createPR(imgRenderer).configure(XORMode)).report();
    }

    public void testTextNoAA_XOR() throws Exception {
        (new PerfMeter("TextNoAA_XOR")).exec(createPR(textRenderer).configure(XORMode)).report();
    }

    public void testTextLCD_XOR() throws Exception {
        (new PerfMeter("TextLCD_XOR")).exec(createPR(textRenderer).configure(XORModeLCDText)).report();
    }

    public static void main(String[] args)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        RenderPerfTest test = new RenderPerfTest();

        if (args.length > 0) {
            for (String testCase : args) {
                Method m = RenderPerfTest.class.getDeclaredMethod("test" + testCase);
                m.invoke(test);
            }
        } else {
            Method[] methods = RenderPerfTest.class.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("test") && !ignoredTests.contains(m.getName())) {
                    m.invoke(test);
                }
            }
        }
    }
}
