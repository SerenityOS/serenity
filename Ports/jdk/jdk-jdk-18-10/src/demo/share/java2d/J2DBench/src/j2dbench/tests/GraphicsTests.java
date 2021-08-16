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
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;

import j2dbench.Destinations;
import j2dbench.Group;
import j2dbench.Option;
import j2dbench.Result;
import j2dbench.Test;
import j2dbench.TestEnvironment;

public abstract class GraphicsTests extends Test {
    public static boolean hasGraphics2D;

    static {
        try {
            hasGraphics2D = (Graphics2D.class != null);
        } catch (NoClassDefFoundError e) {
        }
    }

    static Color makeAlphaColor(Color opaque, int alpha) {
        try {
            opaque = new Color(opaque.getRed(),
                               opaque.getGreen(),
                               opaque.getBlue(),
                               alpha);
        } catch (NoSuchMethodError e) {
        }
        return opaque;
    }

    static Group graphicsroot;
    static Group groptroot;

    static Option animList;
    static Option sizeList;
    static Option compRules;
    static Option transforms;
    static Option doExtraAlpha;
    static Option doXor;
    static Option doClipping;
    static Option renderHint;
    // REMIND: transform, etc.

    public static void init() {
        graphicsroot = new Group("graphics", "Graphics Benchmarks");
        graphicsroot.setTabbed();

        groptroot = new Group(graphicsroot, "opts", "General Graphics Options");

        animList = new Option.IntList(groptroot, "anim",
                                      "Movement of rendering position",
                                      new int[] {0, 1, 2},
                                      new String[] {
                                          "static", "slide", "bounce",
                                      },
                                      new String[] {
                                          "No movement",
                                          "Shift horizontal alignment",
                                          "Bounce around window",
                                      }, 0x4);

        sizeList = new Option.IntList(groptroot, "sizes",
                                      "Size of Operations to perform",
                                      new int[] {1, 20, 100, 250, 1000, 4000},
                                      new String[] {
                                          "1x1", "20x20", "100x100", "250x250",
                                          "1000x1000", "4000x4000",
                                      },
                                      new String[] {
                                          "Tiny Shapes (1x1)",
                                          "Small Shapes (20x20)",
                                          "Medium Shapes (100x100)",
                                          "Large Shapes (250x250)",
                                          "X-Large Shapes (1000x1000)",
                                          "Huge Shapes (4000x4000)",
                                      }, 0xa);
        if (hasGraphics2D) {
            String[] rulenames = {
                "Clear",
                "Src",
                "Dst",
                "SrcOver",
                "DstOver",
                "SrcIn",
                "DstIn",
                "SrcOut",
                "DstOut",
                "SrcAtop",
                "DstAtop",
                "Xor",
            };
            String[] ruledescs = new String[rulenames.length];
            Object[] rules = new Object[rulenames.length];
            int j = 0;
            int defrule = 0;
            for (int i = 0; i < rulenames.length; i++) {
                String rulename = rulenames[i];
                try {
                    Field f = AlphaComposite.class.getField(rulename);
                    rules[j] = f.get(null);
                } catch (NoSuchFieldException nsfe) {
                    continue;
                } catch (IllegalAccessException iae) {
                    continue;
                }
                if (rules[j] == AlphaComposite.SrcOver) {
                    defrule = j;
                }
                rulenames[j] = rulename;
                String suffix;
                if (rulename.startsWith("Src")) {
                    suffix = rulename.substring(3);
                    rulename = "Source";
                } else if (rulename.startsWith("Dst")) {
                    suffix = rulename.substring(3);
                    rulename = "Dest";
                } else {
                    suffix = "";
                }
                if (suffix.length() > 0) {
                    suffix = " "+suffix;
                }
                ruledescs[j] = rulename+suffix;
                j++;
            }
            compRules =
                new Option.ObjectList(groptroot, "alpharule",
                                      "AlphaComposite Rule",
                                      j, rulenames, rules, rulenames,
                                      ruledescs, (1 << defrule));
            ((Option.ObjectList) compRules).setNumRows(4);

            Transform[] xforms = {
                Identity.instance,
                FTranslate.instance,
                Scale2x2.instance,
                Rotate15.instance,
                ShearX.instance,
                ShearY.instance,
            };
            String[] xformnames = new String[xforms.length];
            String[] xformdescs = new String[xforms.length];
            for (int i = 0; i < xforms.length; i++) {
                xformnames[i] = xforms[i].getShortName();
                xformdescs[i] = xforms[i].getDescription();
            }
            transforms =
                new Option.ObjectList(groptroot, "transform",
                                      "Affine Transform",
                                      xforms.length,
                                      xformnames, xforms, xformnames,
                                      xformdescs, 0x1);
            ((Option.ObjectList) transforms).setNumRows(3);

            doExtraAlpha =
                new Option.Toggle(groptroot, "extraalpha",
                                  "Render with an \"extra alpha\" of 0.125",
                                  Option.Toggle.Off);
            doXor =
                new Option.Toggle(groptroot, "xormode",
                                  "Render in XOR mode", Option.Toggle.Off);
            doClipping =
                new Option.Toggle(groptroot, "clip",
                                  "Render through a complex clip shape",
                                  Option.Toggle.Off);
            String[] rhintnames = {
                "Default", "Speed", "Quality",
            };
            renderHint =
                new Option.ObjectList(groptroot, "renderhint",
                                      "Rendering Hint",
                                      rhintnames, new Object[] {
                                          RenderingHints.VALUE_RENDER_DEFAULT,
                                          RenderingHints.VALUE_RENDER_SPEED,
                                          RenderingHints.VALUE_RENDER_QUALITY,
                                      }, rhintnames, rhintnames, 1);
        }
    }

    public static class Context {
        Graphics graphics;
        Dimension outdim;
        boolean animate;
        int size;
        int orgX, orgY;
        int initX, initY;
        int maxX, maxY;
        double pixscale;
    }

    public GraphicsTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
        addDependency(Destinations.destroot);
        addDependencies(groptroot, false);
    }

    public Object initTest(TestEnvironment env, Result result) {
        Context ctx = createContext();
        initContext(env, ctx);
        result.setUnits((int) (ctx.pixscale * pixelsTouched(ctx)));
        result.setUnitName("pixel");
        return ctx;
    }

    public int pixelsTouched(Context ctx) {
        return ctx.outdim.width * ctx.outdim.height;
    }

    public Context createContext() {
        return new Context();
    }

    public Dimension getOutputSize(int w, int h) {
        return new Dimension(w, h);
    }

    public void initContext(TestEnvironment env, Context ctx) {
        ctx.graphics = env.getGraphics();
        int w = env.getWidth();
        int h = env.getHeight();
        ctx.size = env.getIntValue(sizeList);
        ctx.outdim = getOutputSize(ctx.size, ctx.size);
        ctx.pixscale = 1.0;
        if (hasGraphics2D) {
            Graphics2D g2d = (Graphics2D) ctx.graphics;
            AlphaComposite ac = (AlphaComposite) env.getModifier(compRules);
            if (env.isEnabled(doExtraAlpha)) {
                ac = AlphaComposite.getInstance(ac.getRule(), 0.125f);
            }
            g2d.setComposite(ac);
            if (env.isEnabled(doXor)) {
                g2d.setXORMode(Color.white);
            }
            if (env.isEnabled(doClipping)) {
                Polygon p = new Polygon();
                p.addPoint(0, 0);
                p.addPoint(w, 0);
                p.addPoint(0, h);
                p.addPoint(w, h);
                p.addPoint(0, 0);
                g2d.clip(p);
            }
            Transform tx = (Transform) env.getModifier(transforms);
            Dimension envdim = new Dimension(w, h);
            tx.init(g2d, ctx, envdim);
            w = envdim.width;
            h = envdim.height;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                 env.getModifier(renderHint));
        }
        switch (env.getIntValue(animList)) {
        case 0:
            ctx.animate = false;
            ctx.maxX = 3;
            ctx.maxY = 1;
            ctx.orgX = (w - ctx.outdim.width) / 2;
            ctx.orgY = (h - ctx.outdim.height) / 2;
            break;
        case 1:
            ctx.animate = true;
            ctx.maxX = Math.max(Math.min(32, w - ctx.outdim.width), 3);
            ctx.maxY = 1;
            ctx.orgX = (w - ctx.outdim.width - ctx.maxX) / 2;
            ctx.orgY = (h - ctx.outdim.height) / 2;
            break;
        case 2:
            ctx.animate = true;
            ctx.maxX = (w - ctx.outdim.width) + 1;
            ctx.maxY = (h - ctx.outdim.height) + 1;
            ctx.maxX = adjustWidth(ctx.maxX, ctx.maxY);
            ctx.maxX = Math.max(ctx.maxX, 3);
            ctx.maxY = Math.max(ctx.maxY, 1);
            // ctx.orgX = ctx.orgY = 0;
            break;
        }
        ctx.initX = ctx.maxX / 2;
        ctx.initY = ctx.maxY / 2;
    }

    public void cleanupTest(TestEnvironment env, Object ctx) {
        Graphics graphics = ((Context) ctx).graphics;
        graphics.dispose();
        ((Context) ctx).graphics = null;
    }

    public abstract static class Transform {
        public abstract String getShortName();
        public abstract String getDescription();
        public abstract void init(Graphics2D g2d, Context ctx, Dimension dim);

        public static double scaleForPoint(AffineTransform at,
                                           double xorig, double yorig,
                                           double x, double y,
                                           int w, int h)
        {
            Point2D.Double ptd = new Point2D.Double(x, y);
            at.transform(ptd, ptd);
            x = ptd.getX();
            y = ptd.getY();
            double scale = 1.0;
            if (x < 0) {
                scale = Math.min(scale, xorig / (xorig - x));
            } else if (x > w) {
                scale = Math.min(scale, (w - xorig) / (x - xorig));
            }
            if (y < 0) {
                scale = Math.min(scale, yorig / (yorig - y));
            } else if (y > h) {
                scale = Math.min(scale, (h - yorig) / (y - yorig));
            }
            return scale;
        }

        public static Dimension scaleForTransform(AffineTransform at,
                                                  Dimension dim)
        {
            int w = dim.width;
            int h = dim.height;
            Point2D.Double ptd = new Point2D.Double(0, 0);
            at.transform(ptd, ptd);
            double ox = ptd.getX();
            double oy = ptd.getY();
            if (ox < 0 || ox > w || oy < 0 || oy > h) {
                throw new InternalError("origin outside destination");
            }
            double scalex = scaleForPoint(at, ox, oy, w, h, w, h);
            double scaley = scalex;
            scalex = Math.min(scaleForPoint(at, ox, oy, w, 0, w, h), scalex);
            scaley = Math.min(scaleForPoint(at, ox, oy, 0, h, w, h), scaley);
            if (scalex < 0 || scaley < 0) {
                throw new InternalError("could not fit dims to transform");
            }
            return new Dimension((int) Math.floor(w * scalex),
                                 (int) Math.floor(h * scaley));
        }
    }

    public static class Identity extends Transform {
        public static final Identity instance = new Identity();

        private Identity() {}

        public String getShortName() {
            return "ident";
        }

        public String getDescription() {
            return "Identity";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
        }
    }

    public static class FTranslate extends Transform {
        public static final FTranslate instance = new FTranslate();

        private FTranslate() {}

        public String getShortName() {
            return "ftrans";
        }

        public String getDescription() {
            return "FTranslate 1.5";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
            int w = dim.width;
            int h = dim.height;
            AffineTransform at = new AffineTransform();
            at.translate(1.5, 1.5);
            g2d.transform(at);
            dim.setSize(w-3, h-3);
        }
    }

    public static class Scale2x2 extends Transform {
        public static final Scale2x2 instance = new Scale2x2();

        private Scale2x2() {}

        public String getShortName() {
            return "scale2x2";
        }

        public String getDescription() {
            return "Scale 2x by 2x";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
            int w = dim.width;
            int h = dim.height;
            AffineTransform at = new AffineTransform();
            at.scale(2.0, 2.0);
            g2d.transform(at);
            dim.setSize(w/2, h/2);
            ctx.pixscale = 4;
        }
    }

    public static class Rotate15 extends Transform {
        public static final Rotate15 instance = new Rotate15();

        private Rotate15() {}

        public String getShortName() {
            return "rot15";
        }

        public String getDescription() {
            return "Rotate 15 degrees";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
            int w = dim.width;
            int h = dim.height;
            double theta = Math.toRadians(15);
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            double xsize = sin * h + cos * w;
            double ysize = sin * w + cos * h;
            double scale = Math.min(w / xsize, h / ysize);
            xsize *= scale;
            ysize *= scale;
            AffineTransform at = new AffineTransform();
            at.translate((w - xsize) / 2.0, (h - ysize) / 2.0);
            at.translate(sin * h * scale, 0.0);
            at.rotate(theta);
            g2d.transform(at);
            dim.setSize(scaleForTransform(at, dim));
        }
    }

    public static class ShearX extends Transform {
        public static final ShearX instance = new ShearX();

        private ShearX() {}

        public String getShortName() {
            return "shearx";
        }

        public String getDescription() {
            return "Shear X to the right";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
            int w = dim.width;
            int h = dim.height;
            AffineTransform at = new AffineTransform();
            at.translate(0.0, (h - (w*h)/(w + h*0.1)) / 2);
            at.shear(0.1, 0.0);
            g2d.transform(at);
            dim.setSize(scaleForTransform(at, dim));
        }
    }

    public static class ShearY extends Transform {
        public static final ShearY instance = new ShearY();

        private ShearY() {}

        public String getShortName() {
            return "sheary";
        }

        public String getDescription() {
            return "Shear Y down";
        }

        public void init(Graphics2D g2d, Context ctx, Dimension dim) {
            int w = dim.width;
            int h = dim.height;
            AffineTransform at = new AffineTransform();
            at.translate((w - (w*h)/(h + w*0.1)) / 2, 0.0);
            at.shear(0.0, 0.1);
            g2d.transform(at);
            dim.setSize(scaleForTransform(at, dim));
        }
    }
}
