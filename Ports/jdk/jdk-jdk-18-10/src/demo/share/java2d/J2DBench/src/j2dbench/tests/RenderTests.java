/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench.tests;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.AlphaComposite;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.JComponent;

import j2dbench.Group;
import j2dbench.Node;
import j2dbench.Option;
import j2dbench.TestEnvironment;

public abstract class RenderTests extends GraphicsTests {
    static Group renderroot;
    static Group renderoptroot;
    static Group rendertestroot;
    static Group rendershaperoot;

    static Option paintList;
    static Option doAntialias;
    static Option doAlphaColors;
    static Option sizeList;
    static Option strokeList;

    static final int NUM_RANDOMCOLORS = 4096;
    static final int NUM_RANDOMCOLORMASK = (NUM_RANDOMCOLORS - 1);
    static Color[] randAlphaColors;
    static Color[] randOpaqueColors;

    static {
        randOpaqueColors = new Color[NUM_RANDOMCOLORS];
        randAlphaColors = new Color[NUM_RANDOMCOLORS];
        for (int i = 0; i < NUM_RANDOMCOLORS; i++) {
            int r = (int) (Math.random() * 255);
            int g = (int) (Math.random() * 255);
            int b = (int) (Math.random() * 255);
            randOpaqueColors[i] = new Color(r, g, b);
            randAlphaColors[i] = makeAlphaColor(randOpaqueColors[i], 32);
        }
    }

    static boolean hasMultiGradient;

    static {
        try {
            hasMultiGradient = (MultipleGradientPaint.class != null);
        } catch (NoClassDefFoundError e) {
        }
    }

    public static void init() {
        renderroot = new Group(graphicsroot, "render", "Rendering Benchmarks");
        renderoptroot = new Group(renderroot, "opts", "Rendering Options");
        rendertestroot = new Group(renderroot, "tests", "Rendering Tests");

        ArrayList paintStrs = new ArrayList();
        ArrayList paintDescs = new ArrayList();
        paintStrs.add("single");
        paintDescs.add("Single Color");
        paintStrs.add("random");
        paintDescs.add("Random Color");
        if (hasGraphics2D) {
            paintStrs.add("gradient2");
            paintDescs.add("2-color GradientPaint");
            if (hasMultiGradient) {
                paintStrs.add("linear2");
                paintDescs.add("2-color LinearGradientPaint");
                paintStrs.add("linear3");
                paintDescs.add("3-color LinearGradientPaint");
                paintStrs.add("radial2");
                paintDescs.add("2-color RadialGradientPaint");
                paintStrs.add("radial3");
                paintDescs.add("3-color RadialGradientPaint");
            }
            paintStrs.add("texture20");
            paintDescs.add("20x20 TexturePaint");
            paintStrs.add("texture32");
            paintDescs.add("32x32 TexturePaint");
        }
        String[] paintStrArr = new String[paintStrs.size()];
        paintStrArr = (String[])paintStrs.toArray(paintStrArr);
        String[] paintDescArr = new String[paintDescs.size()];
        paintDescArr = (String[])paintDescs.toArray(paintDescArr);
        paintList =
            new Option.ObjectList(renderoptroot,
                                  "paint", "Paint Type",
                                  paintStrArr, paintStrArr,
                                  paintStrArr, paintDescArr,
                                  0x1);
        ((Option.ObjectList) paintList).setNumRows(5);

        // add special RandomColorOpt for backwards compatibility with
        // older options files
        new RandomColorOpt();

        if (hasGraphics2D) {
            doAlphaColors =
                new Option.Toggle(renderoptroot, "alphacolor",
                                  "Set the alpha of the paint to 0.125",
                                  Option.Toggle.Off);
            doAntialias =
                new Option.Toggle(renderoptroot, "antialias",
                                  "Render shapes antialiased",
                                  Option.Toggle.Off);
            String[] strokeStrings = {
                "width0",
                "width1",
                "width5",
                "width20",
                "dash0_5",
                "dash1_5",
                "dash5_20",
                "dash20_50",
            };
            String[] strokeDescriptions = {
                "Solid Thin lines",
                "Solid Width 1 lines",
                "Solid Width 5 lines",
                "Solid Width 20 lines",
                "Dashed Thin lines",
                "Dashed Width 1 lines",
                "Dashed Width 5 lines",
                "Dashed Width 20 lines",
            };
            BasicStroke[] strokeObjects = {
                new BasicStroke(0f),
                new BasicStroke(1f),
                new BasicStroke(5f),
                new BasicStroke(20f),
                new BasicStroke(0f, BasicStroke.CAP_SQUARE,
                                BasicStroke.JOIN_MITER, 10f,
                                new float[] { 5f, 5f }, 0f),
                new BasicStroke(1f, BasicStroke.CAP_SQUARE,
                                BasicStroke.JOIN_MITER, 10f,
                                new float[] { 5f, 5f }, 0f),
                new BasicStroke(5f, BasicStroke.CAP_SQUARE,
                                BasicStroke.JOIN_MITER, 10f,
                                new float[] { 20f, 20f }, 0f),
                new BasicStroke(20f, BasicStroke.CAP_SQUARE,
                                BasicStroke.JOIN_MITER, 10f,
                                new float[] { 50f, 50f }, 0f),
            };
            strokeList =
                new Option.ObjectList(renderoptroot,
                                      "stroke", "Stroke Type",
                                      strokeStrings, strokeObjects,
                                      strokeStrings, strokeDescriptions,
                                      0x2);
            ((Option.ObjectList) strokeList).setNumRows(4);
        }

        new DrawDiagonalLines();
        new DrawHorizontalLines();
        new DrawVerticalLines();
        new FillRects();
        new DrawRects();
        new FillOvals();
        new DrawOvals();
        new FillPolys();
        new DrawPolys();

        if (hasGraphics2D) {
            rendershaperoot = new Group(rendertestroot, "shape",
                                        "Shape Rendering Tests");

            new FillCubics();
            new DrawCubics();
            new FillEllipse2Ds();
            new DrawEllipse2Ds();
        }
    }

    /**
     * This "virtual Node" implementation is here to maintain backward
     * compatibility with older J2DBench releases, specifically those
     * options files that were created before we added the gradient/texture
     * paint options in JDK 6.  This class will translate the color settings
     * from the old "randomcolor" option into the new "paint" option.
     */
    private static class RandomColorOpt extends Node {
        public RandomColorOpt() {
            super(renderoptroot, "randomcolor",
                  "Use random colors for each shape");
        }

        public JComponent getJComponent() {
            return null;
        }

        public void restoreDefault() {
            // no-op
        }

        public void write(PrintWriter pw) {
            // no-op (the random/single choice will be saved as part of
            // the new "paint" option added to J2DBench in JDK 6)
        }

        public String setOption(String key, String value) {
            String opts;
            if (value.equals("On")) {
                opts = "random";
            } else if (value.equals("Off")) {
                opts = "single";
            } else if (value.equals("Both")) {
                opts = "random,single";
            } else {
                return "Bad value";
            }
            return ((Option.ObjectList)paintList).setValueFromString(opts);
        }
    }

    public static class Context extends GraphicsTests.Context {
        int colorindex;
        Color[] colorlist;
    }

    public RenderTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
        addDependencies(renderoptroot, true);
    }

    public GraphicsTests.Context createContext() {
        return new RenderTests.Context();
    }

    public void initContext(TestEnvironment env, GraphicsTests.Context ctx) {
        super.initContext(env, ctx);
        RenderTests.Context rctx = (RenderTests.Context) ctx;
        boolean alphacolor;

        if (hasGraphics2D) {
            Graphics2D g2d = (Graphics2D) rctx.graphics;
            if (env.isEnabled(doAntialias)) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);
            }
            alphacolor = env.isEnabled(doAlphaColors);
            g2d.setStroke((Stroke) env.getModifier(strokeList));
        } else {
            alphacolor = false;
        }

        String paint = (String)env.getModifier(paintList);
        if (paint.equals("single")) {
            Color c = Color.darkGray;
            if (alphacolor) {
                c = makeAlphaColor(c, 32);
            }
            rctx.graphics.setColor(c);
        } else if (paint.equals("random")) {
            rctx.colorlist = alphacolor ? randAlphaColors : randOpaqueColors;
        } else if (paint.equals("gradient2")) {
            Color[] colors = makeGradientColors(2, alphacolor);
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(new GradientPaint(0.0f, 0.0f, colors[0],
                                           10.0f, 10.0f, colors[1], true));
        } else if (paint.equals("linear2")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeLinear(2, alphacolor));
        } else if (paint.equals("linear3")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeLinear(3, alphacolor));
        } else if (paint.equals("radial2")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeRadial(2, alphacolor));
        } else if (paint.equals("radial3")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeRadial(3, alphacolor));
        } else if (paint.equals("texture20")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeTexturePaint(20, alphacolor));
        } else if (paint.equals("texture32")) {
            Graphics2D g2d = (Graphics2D)rctx.graphics;
            g2d.setPaint(makeTexturePaint(32, alphacolor));
        } else {
            throw new InternalError("Invalid paint mode");
        }
    }

    private Color[] makeGradientColors(int numColors, boolean alpha) {
        Color[] colors = new Color[] {Color.red, Color.blue,
                                      Color.green, Color.yellow};
        Color[] ret = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            ret[i] = alpha ? makeAlphaColor(colors[i], 32) : colors[i];
        }
        return ret;
    }

    private LinearGradientPaint makeLinear(int numColors, boolean alpha) {
        float interval = 1.0f / (numColors - 1);
        float[] fractions = new float[numColors];
        for (int i = 0; i < fractions.length; i++) {
            fractions[i] = i * interval;
        }
        Color[] colors = makeGradientColors(numColors, alpha);
        return new LinearGradientPaint(0.0f, 0.0f,
                                       10.0f, 10.0f,
                                       fractions, colors,
                                       CycleMethod.REFLECT);
    }

    private RadialGradientPaint makeRadial(int numColors, boolean alpha) {
        float interval = 1.0f / (numColors - 1);
        float[] fractions = new float[numColors];
        for (int i = 0; i < fractions.length; i++) {
            fractions[i] = i * interval;
        }
        Color[] colors = makeGradientColors(numColors, alpha);
        return new RadialGradientPaint(0.0f, 0.0f, 10.0f,
                                       fractions, colors,
                                       CycleMethod.REFLECT);
    }

    private TexturePaint makeTexturePaint(int size, boolean alpha) {
        int s2 = size / 2;
        int type =
            alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage img = new BufferedImage(size, size, type);
        Color[] colors = makeGradientColors(4, alpha);
        Graphics2D g2d = img.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(colors[0]);
        g2d.fillRect(0, 0, s2, s2);
        g2d.setColor(colors[1]);
        g2d.fillRect(s2, 0, s2, s2);
        g2d.setColor(colors[3]);
        g2d.fillRect(0, s2, s2, s2);
        g2d.setColor(colors[2]);
        g2d.fillRect(s2, s2, s2, s2);
        g2d.dispose();
        Rectangle2D bounds = new Rectangle2D.Float(0, 0, size, size);
        return new TexturePaint(img, bounds);
    }

    public static class DrawDiagonalLines extends RenderTests {
        public DrawDiagonalLines() {
            super(rendertestroot, "drawLine", "Draw Diagonal Lines");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            return Math.max(ctx.outdim.width, ctx.outdim.height);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x + size, y + size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x + size, y + size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class DrawHorizontalLines extends RenderTests {
        public DrawHorizontalLines() {
            super(rendertestroot, "drawLineHoriz",
                  "Draw Horizontal Lines");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            return ctx.outdim.width;
        }

        public Dimension getOutputSize(int w, int h) {
            return new Dimension(w, 1);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x + size, y);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x + size, y);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class DrawVerticalLines extends RenderTests {
        public DrawVerticalLines() {
            super(rendertestroot, "drawLineVert",
                  "Draw Vertical Lines");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            return ctx.outdim.height;
        }

        public Dimension getOutputSize(int w, int h) {
            return new Dimension(1, h);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x, y + size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawLine(x, y, x, y + size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class FillRects extends RenderTests {
        public FillRects() {
            super(rendertestroot, "fillRect", "Fill Rectangles");
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.fillRect(x, y, size, size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.fillRect(x, y, size, size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class DrawRects extends RenderTests {
        public DrawRects() {
            super(rendertestroot, "drawRect", "Draw Rectangles");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            int w = ctx.outdim.width;
            int h = ctx.outdim.height;
            if (w < 2 || h < 2) {
                // If one dimension is less than 2 then there is no
                // gap in the middle, so we get a solid filled rectangle.
                return w * h;
            }
            return (w * 2) + ((h - 2) * 2);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawRect(x, y, size, size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawRect(x, y, size, size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class FillOvals extends RenderTests {
        public FillOvals() {
            super(rendertestroot, "fillOval", "Fill Ellipses");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            // Approximated
            double xaxis = ctx.outdim.width / 2.0;
            double yaxis = ctx.outdim.height / 2.0;
            return (int) (xaxis * yaxis * Math.PI);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.fillOval(x, y, size, size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.fillOval(x, y, size, size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class DrawOvals extends RenderTests {
        public DrawOvals() {
            super(rendertestroot, "drawOval", "Draw Ellipses");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            /*
             * Approximation: We figured that the vertical chord connecting
             * the +45 deg and -45 deg points on the ellipse is about
             * height/sqrt(2) pixels long.  Likewise, the horizontal chord
             * connecting the +45 deg and +135 deg points on the ellipse is
             * about width/sqrt(2) pixels long.  Each of these chords has
             * a parallel on the opposite side of the respective axis (there
             * are two horizontal chords and two vertical chords).  Altogether
             * this gives a reasonable approximation of the total number of
             * pixels touched by the ellipse, so we have:
             *     2*(w/sqrt(2)) + 2*(h/sqrt(2))
             *  == (2/sqrt(2))*(w+h)
             *  == (sqrt(2))*(w+h)
             */
            return (int)(Math.sqrt(2.0)*(ctx.outdim.width+ctx.outdim.height));
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            if (rctx.animate) {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawOval(x, y, size, size);
                    if ((x -= 3) < 0) x += rctx.maxX;
                    if ((y -= 1) < 0) y += rctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    if (rCArray != null) {
                        g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                    }
                    g.drawOval(x, y, size, size);
                } while (--numReps > 0);
            }
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class FillPolys extends RenderTests {
        public FillPolys() {
            super(rendertestroot, "fillPoly", "Fill Hexagonal Polygons");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            /*
             * The polygon is a hexagon inscribed inside the square but
             * missing a triangle at each of the four corners of size
             * (w/4) by (h/2).
             *
             * Putting 2 of these triangles together gives a rectangle
             * of size (w/4) by (h/2).
             *
             * Putting 2 of these rectangles together gives a total
             * missing rectangle size of (w/2) by (h/2).
             *
             * Thus, exactly one quarter of the whole square is not
             * touched by the filled polygon.
             */
            int size = ctx.outdim.width * ctx.outdim.height;
            return size - (size / 4);
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            int size = rctx.size;
            int x = rctx.initX;
            int y = rctx.initY;
            int[] hexaX = new int[6];
            int[] hexaY = new int[6];
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            do {
                hexaX[0] = x;
                hexaX[1] = hexaX[5] = x+size/4;
                hexaX[2] = hexaX[4] = x+size-size/4;
                hexaX[3] = x+size;
                hexaY[1] = hexaY[2] = y;
                hexaY[0] = hexaY[3] = y+size/2;
                hexaY[4] = hexaY[5] = y+size;

                if (rCArray != null) {
                    g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                g.fillPolygon(hexaX, hexaY, 6);
                if ((x -= 3) < 0) x += rctx.maxX;
                if ((y -= 1) < 0) y += rctx.maxY;
            } while (--numReps > 0);
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class DrawPolys extends RenderTests {
        public DrawPolys() {
            super(rendertestroot, "drawPoly", "Draw Hexagonal Polygons");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            /*
             * The two horizontal segments have exactly two pixels per column.
             * Since the diagonals are more vertical than horizontal, using
             * h*2 would be a good way to count the pixels in those sections.
             * We then have to figure out the size of the remainder of the
             * horizontal lines at top and bottom to get the answer:
             *
             *     (diagonals less endpoints)*2 + (horizontals)*2
             *
             *  or:
             *
             *     (h-2)*2 + ((x+w-1-w/4)-(x+w/4)+1)*2
             *
             *  since (w == h == size), we then have:
             *
             *     (size - size/4 - 1) * 4
             */
            int size = ctx.size;
            if (size <= 1) {
                return 1;
            } else {
                return (size - (size / 4) - 1) * 4;
            }
        }

        public void runTest(Object ctx, int numReps) {
            RenderTests.Context rctx = (RenderTests.Context) ctx;
            // subtract 1 to account for the fact that lines are drawn to
            // and including the final coordinate...
            int size = rctx.size - 1;
            int x = rctx.initX;
            int y = rctx.initY;
            int[] hexaX = new int[6];
            int[] hexaY = new int[6];
            Graphics g = rctx.graphics;
            g.translate(rctx.orgX, rctx.orgY);
            Color[] rCArray = rctx.colorlist;
            int ci = rctx.colorindex;
            do {
                hexaX[0] = x;
                hexaX[1] = hexaX[5] = x+size/4;
                hexaX[2] = hexaX[4] = x+size-size/4;
                hexaX[3] = x+size;
                hexaY[1] = hexaY[2] = y;
                hexaY[0] = hexaY[3] = y+size/2;
                hexaY[4] = hexaY[5] = y+size;

                if (rCArray != null) {
                    g.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                g.drawPolygon(hexaX, hexaY, 6);
                if ((x -= 3) < 0) x += rctx.maxX;
                if ((y -= 1) < 0) y += rctx.maxY;
            } while (--numReps > 0);
            rctx.colorindex = ci;
            g.translate(-rctx.orgX, -rctx.orgY);
        }
    }

    public static class FillCubics extends RenderTests {
        static final double relTmax = .5 - Math.sqrt(3) / 6;
        static final double relYmax = ((6*relTmax - 9)*relTmax + 3)*relTmax;

        public FillCubics() {
            super(rendershaperoot, "fillCubic", "Fill Bezier Curves");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            /*
             * The cubic only touches 2 quadrants in the square, thus
             * at least half of the square is unfilled.  The integrals
             * to figure out the exact area are not trivial so for the
             * other 2 quadrants, I'm going to guess that the cubic only
             * encloses somewhere between 1/2 and 3/4ths of the pixels
             * in those quadrants - we will say 5/8ths.  Thus only
             * 5/16ths of the total square is filled.
             */
            // Note: 2x2 ends up hitting exactly 1 pixel...
            int size = ctx.size;
            if (size < 2) size = 2;
            return size * size * 5 / 16;
        }

        public static class Context extends RenderTests.Context {
            CubicCurve2D curve = new CubicCurve2D.Float();
        }

        public GraphicsTests.Context createContext() {
            return new FillCubics.Context();
        }

        public void runTest(Object ctx, int numReps) {
            FillCubics.Context cctx = (FillCubics.Context) ctx;
            int size = cctx.size;
            // Note: 2x2 ends up hitting exactly 1 pixel...
            if (size < 2) size = 2;
            int x = cctx.initX;
            int y = cctx.initY;
            int cpoffset = (int) (size/relYmax/2);
            CubicCurve2D curve = cctx.curve;
            Graphics2D g2d = (Graphics2D) cctx.graphics;
            g2d.translate(cctx.orgX, cctx.orgY);
            Color[] rCArray = cctx.colorlist;
            int ci = cctx.colorindex;
            do {
                curve.setCurve(x, y+size/2.0,
                               x+size/2.0, y+size/2.0-cpoffset,
                               x+size/2.0, y+size/2.0+cpoffset,
                               x+size, y+size/2.0);

                if (rCArray != null) {
                    g2d.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                g2d.fill(curve);
                if ((x -= 3) < 0) x += cctx.maxX;
                if ((y -= 1) < 0) y += cctx.maxY;
            } while (--numReps > 0);
            cctx.colorindex = ci;
            g2d.translate(-cctx.orgX, -cctx.orgY);
        }
    }

    public static class DrawCubics extends RenderTests {
        static final double relTmax = .5 - Math.sqrt(3) / 6;
        static final double relYmax = ((6*relTmax - 9)*relTmax + 3)*relTmax;

        public DrawCubics() {
            super(rendershaperoot, "drawCubic", "Draw Bezier Curves");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            // Gross approximation
            int size = ctx.size;
            if (size < 2) size = 2;
            return size;
        }

        public static class Context extends RenderTests.Context {
            CubicCurve2D curve = new CubicCurve2D.Float();
        }

        public GraphicsTests.Context createContext() {
            return new DrawCubics.Context();
        }

        public void runTest(Object ctx, int numReps) {
            DrawCubics.Context cctx = (DrawCubics.Context) ctx;
            int size = cctx.size;
            // Note: 2x2 ends up hitting exactly 1 pixel...
            if (size < 2) size = 2;
            int x = cctx.initX;
            int y = cctx.initY;
            int cpoffset = (int) (size/relYmax/2);
            CubicCurve2D curve = cctx.curve;
            Graphics2D g2d = (Graphics2D) cctx.graphics;
            g2d.translate(cctx.orgX, cctx.orgY);
            Color[] rCArray = cctx.colorlist;
            int ci = cctx.colorindex;
            do {
                curve.setCurve(x, y+size/2.0,
                               x+size/2.0, y+size/2.0-cpoffset,
                               x+size/2.0, y+size/2.0+cpoffset,
                               x+size, y+size/2.0);

                if (rCArray != null) {
                    g2d.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                g2d.draw(curve);
                if ((x -= 3) < 0) x += cctx.maxX;
                if ((y -= 1) < 0) y += cctx.maxY;
            } while (--numReps > 0);
            cctx.colorindex = ci;
            g2d.translate(-cctx.orgX, -cctx.orgY);
        }
    }

    public static class FillEllipse2Ds extends RenderTests {
        public FillEllipse2Ds() {
            super(rendershaperoot, "fillEllipse2D", "Fill Ellipse2Ds");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            // Approximated (copied from FillOvals.pixelsTouched())
            double xaxis = ctx.outdim.width / 2.0;
            double yaxis = ctx.outdim.height / 2.0;
            return (int) (xaxis * yaxis * Math.PI);
        }

        public static class Context extends RenderTests.Context {
            Ellipse2D ellipse = new Ellipse2D.Float();
        }

        public GraphicsTests.Context createContext() {
            return new FillEllipse2Ds.Context();
        }

        public void runTest(Object ctx, int numReps) {
            FillEllipse2Ds.Context cctx = (FillEllipse2Ds.Context) ctx;
            int size = cctx.size;
            int x = cctx.initX;
            int y = cctx.initY;
            Ellipse2D ellipse = cctx.ellipse;
            Graphics2D g2d = (Graphics2D) cctx.graphics;
            g2d.translate(cctx.orgX, cctx.orgY);
            Color[] rCArray = cctx.colorlist;
            int ci = cctx.colorindex;
            do {
                if (rCArray != null) {
                    g2d.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                ellipse.setFrame(x, y, size, size);
                g2d.fill(ellipse);
                if ((x -= 3) < 0) x += cctx.maxX;
                if ((y -= 1) < 0) y += cctx.maxY;
            } while (--numReps > 0);
            cctx.colorindex = ci;
            g2d.translate(-cctx.orgX, -cctx.orgY);
        }
    }

    public static class DrawEllipse2Ds extends RenderTests {
        public DrawEllipse2Ds() {
            super(rendershaperoot, "drawEllipse2D", "Draw Ellipse2Ds");
        }

        public int pixelsTouched(GraphicsTests.Context ctx) {
            // Approximated (copied from DrawOvals.pixelsTouched())
            return (int)(Math.sqrt(2.0)*(ctx.outdim.width+ctx.outdim.height));
        }

        public static class Context extends RenderTests.Context {
            Ellipse2D ellipse = new Ellipse2D.Float();
        }

        public GraphicsTests.Context createContext() {
            return new DrawEllipse2Ds.Context();
        }

        public void runTest(Object ctx, int numReps) {
            DrawEllipse2Ds.Context cctx = (DrawEllipse2Ds.Context) ctx;
            int size = cctx.size;
            int x = cctx.initX;
            int y = cctx.initY;
            Ellipse2D ellipse = cctx.ellipse;
            Graphics2D g2d = (Graphics2D) cctx.graphics;
            g2d.translate(cctx.orgX, cctx.orgY);
            Color[] rCArray = cctx.colorlist;
            int ci = cctx.colorindex;
            do {
                if (rCArray != null) {
                    g2d.setColor(rCArray[ci++ & NUM_RANDOMCOLORMASK]);
                }
                ellipse.setFrame(x, y, size, size);
                g2d.draw(ellipse);
                if ((x -= 3) < 0) x += cctx.maxX;
                if ((y -= 1) < 0) y += cctx.maxY;
            } while (--numReps > 0);
            cctx.colorindex = ci;
            g2d.translate(-cctx.orgX, -cctx.orgY);
        }
    }
}
