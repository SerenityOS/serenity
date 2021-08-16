/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6271221 8145584
 * @summary ask a text layout for its pixel bounds, then render it and
 * compute the actual pixel bounds when rendering-- the actual pixel bounds
 * must be contained within the bounds reported by the text layout, or there
 * will be an exception.
 */

/*
 * Copyright 2005 IBM Corp.  All Rights Reserved.
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;

import static java.awt.Font.*;
import static java.awt.font.GraphicAttribute.*;
import static java.awt.font.ShapeGraphicAttribute.*;
import static java.awt.font.TextAttribute.*;

public class TestGetPixelBounds {
    static final boolean DEBUG;
    static final boolean DUMP;
    static {
        String dbg = null;
        String dmp = null;
        try {
            dbg = System.getProperty("DEBUG");
            dmp = System.getProperty("DUMP");
        }
        catch (SecurityException e) {
            dbg = dmp = null;
        }

        DEBUG = dbg != null;
        DUMP = dmp != null;
    }

    public static void main(String[] args) {
        float x = 0;
        float y = 0;
        boolean rotate = false;
        boolean underline = false;
        boolean graphic = false;
        String text = "Ping";
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.startsWith("-x")) {
                x = Float.parseFloat(args[++i]);
            } else if (arg.startsWith("-y")) {
                y = Float.parseFloat(args[++i]);
            } else if (arg.startsWith("-r")) {
                rotate = true;
            } else if (arg.startsWith("-u")) {
                underline = true;
            } else if (arg.startsWith("-g")) {
                graphic = true;
            } else if (arg.startsWith("-t")) {
                text = args[++i];
            }
        }
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Map<TextAttribute, Object> m = new HashMap<TextAttribute, Object>();
        m.put(FAMILY, SANS_SERIF);
        m.put(SIZE, 16);
        if (underline) {
            m.put(UNDERLINE, UNDERLINE_ON);
        }
        if (rotate) {
            m.put(TRANSFORM, AffineTransform.getRotateInstance(Math.PI/4));
        }
        Font font = Font.getFont(m);

        AttributedString as;
        if (graphic) {
            GraphicAttribute sga = new ShapeGraphicAttribute(
                new Ellipse2D.Float(0,-10,10,20), TOP_ALIGNMENT, STROKE);
            as = new AttributedString(text + '*' + text, m);
            as.addAttribute(CHAR_REPLACEMENT, sga, text.length(), text.length() + 1);
        } else {
            as = new AttributedString(text, m);
        }
        TextLayout tl = new TextLayout(as.getIterator(), frc);
        System.out.println("tl bounds: " + tl.getBounds());
        System.out.println("tl compute: " + computeLayoutBounds(tl, x, y, frc));
        System.out.println("tl pixel bounds: " + tl.getPixelBounds(frc, x, y));
        System.out.println("   again int off: " + tl.getPixelBounds(frc, x+2, y - 2));
        System.out.println("   again frac off: " + tl.getPixelBounds(frc, x+.5f, y + .2f));
        System.out.println("   again frc: " + tl.getPixelBounds(
            new FontRenderContext(AffineTransform.getScaleInstance(100, 100), true, true), x, y));
        System.out.println("   again int off: " + tl.getPixelBounds(frc, x-2, y+2));

        GlyphVector gv = font.createGlyphVector(frc, text);
        System.out.println("gv bounds: " + gv.getPixelBounds(frc, x, y));
        System.out.println("gv compute: " + computeLayoutBounds(gv, x, y, frc));

        if (!tl.getPixelBounds(frc, x, y).contains(computeLayoutBounds(tl, x, y, frc))) {
            throw new RuntimeException("error, tl.bounds does not contain computed bounds");
        }
    }

    static Rectangle computeLayoutBounds(TextLayout tl, float x, float y, FontRenderContext frc) {
        Rectangle bounds = tl.getBounds().getBounds();
        BufferedImage im = new BufferedImage(bounds.width + 4, bounds.height + 4,
                                             BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = im.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, im.getWidth(), im.getHeight());

        float fx = (float)Math.IEEEremainder(x,1);
        float fy = (float)Math.IEEEremainder(y,1);
        g2d.setColor(Color.BLACK);
        tl.draw(g2d, fx + 2 - bounds.x, fy + 2 - bounds.y);

        Rectangle r = computePixelBounds(im);
        r.x += (int)Math.floor(x) - 2 + bounds.x;
        r.y += (int)Math.floor(y) - 2 + bounds.y;

        return r;
    }

    static Rectangle computeLayoutBounds(GlyphVector gv, float x, float y, FontRenderContext frc) {
        Rectangle bounds = gv.getVisualBounds().getBounds();
        BufferedImage im = new BufferedImage(bounds.width + 4, bounds.height + 4,
                                             BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = im.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, im.getWidth(), im.getHeight());

        float fx = (float)Math.IEEEremainder(x,1);
        float fy = (float)Math.IEEEremainder(y,1);
        g2d.setColor(Color.BLACK);
        g2d.drawGlyphVector(gv, fx + 2 - bounds.x, fy + 2 - bounds.y);

        Rectangle r = computePixelBounds(im);
        r.x += (int)Math.floor(x) - 2 + bounds.x;
        r.y += (int)Math.floor(y) - 2 + bounds.y;

        return r;
    }

    static Rectangle computePixelBounds(BufferedImage im) {
        int w = im.getWidth();
        int h = im.getHeight();

        Formatter fmt = DEBUG ? new Formatter(System.err) : null;

        // dump
        if (DUMP && DEBUG) {
            fmt.format("    ");
            for (int j = 0; j < w; ++j) {
                fmt.format("%2d", j);
            }
            for (int i = 0; i < h; ++i) {
                fmt.format("\n[%2d] ", i);
                for (int j = 0; j < w; ++j) {
                    fmt.format("%c ", im.getRGB(j, i) == -1 ? ' ' : '*');
                }
            }
            fmt.format("\n");
        }

        int l = -1, t = -1, r = w, b = h;

        {
            // get top
            int[] buf = new int[w];
        loop:
            while (++t < h) {
                im.getRGB(0, t, buf.length, 1, buf, 0, w); // w ignored
                for (int i = 0; i < buf.length; i++) {
                    if (buf[i] != -1) {
                        if (DEBUG) fmt.format("top pixel at %d,%d = 0x%08x\n", i, t, buf[i]);
                        break loop;
                    }
                }
            }
            if (DEBUG) fmt.format("t: %d\n", t);
        }

        // get bottom
        {
            int[] buf = new int[w];
        loop:
            while (--b > t) {
                im.getRGB(0, b, buf.length, 1, buf, 0, w); // w ignored
                for (int i = 0; i < buf.length; ++i) {
                    if (buf[i] != -1) {
                        if (DEBUG) fmt.format("bottom pixel at %d,%d = 0x%08x\n", i, b, buf[i]);
                        break loop;
                    }
                }
            }
            ++b;
            if (DEBUG) fmt.format("b: %d\n", b);
        }

        // get left
        {
        loop:
            while (++l < r) {
                for (int i = t; i < b; ++i) {
                    int v = im.getRGB(l, i);
                    if (v != -1) {
                        if (DEBUG) fmt.format("left pixel at %d,%d = 0x%08x\n", l, i, v);
                        break loop;
                    }
                }
            }
            if (DEBUG) fmt.format("l: %d\n", l);
        }

        // get right
        {
        loop:
            while (--r > l) {
                for (int i = t; i < b; ++i) {
                    int v = im.getRGB(r, i);
                    if (v != -1) {
                        if (DEBUG) fmt.format("right pixel at %d,%d = 0x%08x\n", r, i, v);
                        break loop;
                    }
                }
            }
            ++r;
            if (DEBUG) fmt.format("r: %d\n", r);
        }

        return new Rectangle(l, t, r-l, b-t);
    }
}
