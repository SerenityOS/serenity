/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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


/*
 * (C) Copyright IBM Corp. 2003, All Rights Reserved.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

package j2dbench.tests.text;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.Bidi;
import java.util.ArrayList;

import j2dbench.Group;
import j2dbench.Result;
import j2dbench.TestEnvironment;

public abstract class TextMeasureTests extends TextTests {
    static Group measureroot;
    static Group measuretestroot;

    public static void init() {
        measureroot = new Group(textroot, "Measuring", "Measuring Benchmarks");
        measuretestroot = new Group(measureroot, "tests", "Measuring Tests");

        new StringWidth();
        new StringBounds();
        new CharsWidth();
        new CharsBounds();
        new FontCanDisplay();

        if (hasGraphics2D) {
            new GVWidth();
            new GVLogicalBounds();
            new GVVisualBounds();
            new GVPixelBounds();
            new GVOutline();
            new GVGlyphLogicalBounds();
            new GVGlyphVisualBounds();
            new GVGlyphPixelBounds();
            new GVGlyphOutline();
            new GVGlyphTransform();
            new GVGlyphMetrics();

            new TLAdvance();
            new TLAscent();
            new TLBounds();
            new TLGetCaretInfo();
            new TLGetNextHit();
            new TLGetCaretShape();
            new TLGetLogicalHighlightShape();
            new TLHitTest();
            new TLOutline();

        /*
            new FontLineMetrics();
            new FontStringBounds();
        */
        }
    }

    public TextMeasureTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    static class SWContext extends TextContext {
        FontMetrics fm;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);
            fm = graphics.getFontMetrics(font);
        }
    }

    public Context createContext() {
        return new SWContext();
    }

    public static class StringWidth extends TextMeasureTests {
        public StringWidth() {
            super(measuretestroot, "stringWidth", "Measuring String Width");
        }

        public void runTest(Object ctx, int numReps) {
            SWContext swctx = (SWContext)ctx;
            String text = swctx.text;
            FontMetrics fm = swctx.fm;
            int wid = 0;
            do {
                wid += fm.stringWidth(text);
            } while (--numReps >= 0);
        }
    }

    public static class StringBounds extends TextMeasureTests {
        public StringBounds() {
            super(measuretestroot, "stringBounds", "Measuring String Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            SWContext swctx = (SWContext)ctx;
            String text = swctx.text;
            FontMetrics fm = swctx.fm;
            int wid = 0;
            Rectangle r = null;
            do {
                r = null;
                int dx = fm.stringWidth(text);
                int dy = fm.getAscent() + fm.getDescent() + fm.getLeading();
                int x = 0;
                int y = -fm.getAscent();
                r = new Rectangle(x, y, dx, dy);
            } while (--numReps >= 0);
        }
    }

    public static class CharsWidth extends TextMeasureTests {
        public CharsWidth() {
            super(measuretestroot, "charsWidth", "Measuring Chars Width");
        }

        public void runTest(Object ctx, int numReps) {
            SWContext swctx = (SWContext)ctx;
            FontMetrics fm = swctx.fm;
            char[] chars = swctx.chars;
            int wid = 0;
            do {
                wid += fm.charsWidth(chars, 0, chars.length);
            } while (--numReps >= 0);
        }
    }

    public static class CharsBounds extends TextMeasureTests {
        public CharsBounds() {
            super(measuretestroot, "charsBounds", "Measuring Chars Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            SWContext swctx = (SWContext)ctx;
            FontMetrics fm = swctx.fm;
            char[] chars = swctx.chars;
            int wid = 0;
            Rectangle r = null;
            do {
                r = null;
                int dx = fm.charsWidth(chars, 0, chars.length);
                int dy = fm.getAscent() + fm.getDescent() + fm.getLeading();
                int x = 0;
                int y = -fm.getAscent();
                r = new Rectangle(x, y, dx, dy);
            } while (--numReps >= 0);
        }
    }

    public static class FontCanDisplay extends TextMeasureTests {
        public FontCanDisplay() {
            super(measuretestroot, "fontcandisplay", "Font canDisplay(char)");
        }

        public void runTest(Object ctx, int numReps) {
            Font font = ((TextContext)ctx).font;
            boolean b = false;
            do {
                for (int i = 0; i < 0x10000; i += 0x64) {
                    b ^= font.canDisplay((char)i);
                }
            } while (--numReps >= 0);
        }
    }

    public static class GVContext extends G2DContext {
        GlyphVector gv;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            int flags = Font.LAYOUT_LEFT_TO_RIGHT;
            if (Bidi.requiresBidi(chars, 0, chars.length)) { // assume rtl
                flags = Font.LAYOUT_RIGHT_TO_LEFT;
            }
            gv = font.layoutGlyphVector(frc, chars, 0, chars.length, flags);

            // gv options
        }
    }

    public abstract static class GVMeasureTest extends TextMeasureTests {
        protected GVMeasureTest(Group parent, String nodeName, String description) {
            super(parent, nodeName, description);
        }

        public Context createContext() {
            return new GVContext();
        }
    }

    public static class GVWidth extends GVMeasureTest {
        public GVWidth() {
            super(measuretestroot, "gvWidth", "Measuring GV Width");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            double wid = 0;
            do {
                wid += gv.getGlyphPosition(gv.getNumGlyphs()).getX();
            } while (--numReps >= 0);
        }
    }

    public static class GVLogicalBounds extends GVMeasureTest {
        public GVLogicalBounds() {
            super(measuretestroot, "gvLogicalBounds", "Measuring GV Logical Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Rectangle2D r;
            do {
                r = gv.getLogicalBounds();
            } while (--numReps >= 0);
        }
    }

    public static class GVVisualBounds extends GVMeasureTest {
        public GVVisualBounds() {
            super(measuretestroot, "gvVisualBounds", "Measuring GV Visual Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Rectangle2D r;
            do {
                r = gv.getVisualBounds();
            } while (--numReps >= 0);
        }
    }

    public static class GVPixelBounds extends GVMeasureTest {
        public GVPixelBounds() {
            super(measuretestroot, "gvPixelBounds", "Measuring GV Pixel Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Rectangle2D r;
            do {
                r = gv.getPixelBounds(null, 0, 0); // !!! add opt to provide different frc?
            } while (--numReps >= 0);
        }
    }

    public static class GVOutline extends GVMeasureTest {
        public GVOutline() {
            super(measuretestroot, "gvOutline", "Getting GV Outline");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Shape s;
            do {
                s = gv.getOutline();
            } while (--numReps >= 0);
        }
    }

    public static class GVGlyphLogicalBounds extends GVMeasureTest {
        public GVGlyphLogicalBounds() {
            super(measuretestroot, "gvGlyphLogicalBounds", "Measuring GV Glyph Logical Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Shape s;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    s = gv.getGlyphLogicalBounds(i);
                }
            } while (--numReps >= 0);
        }
    }

    public static class GVGlyphVisualBounds extends GVMeasureTest {
        public GVGlyphVisualBounds() {
            super(measuretestroot, "gvGlyphVisualBounds", "Measuring GV Glyph Visual Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Shape s;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    s = gv.getGlyphVisualBounds(i);
                }
            } while (--numReps >= 0);
        }
    }


    public static class GVGlyphPixelBounds extends GVMeasureTest {
        public GVGlyphPixelBounds() {
            super(measuretestroot, "gvGlyphPixelBounds", "Measuring GV Glyph Pixel Bounds");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Rectangle2D r;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    r = gv.getGlyphPixelBounds(i, null, 0, 0); // !!! add opt to provide different frc?
                }
            } while (--numReps >= 0);
        }
    }

    public static class GVGlyphOutline extends GVMeasureTest {
        public GVGlyphOutline() {
            super(measuretestroot, "gvGlyphOutline", "Getting GV Glyph Outline");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            Shape s;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    s = gv.getGlyphOutline(i);
                }
            } while (--numReps >= 0);
        }
    }

    public static class GVGlyphTransform extends GVMeasureTest {
        public GVGlyphTransform() {
            super(measuretestroot, "gvGlyphTransform", "Getting GV Glyph Transform");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            AffineTransform tx;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    tx = gv.getGlyphTransform(i);
                }
            } while (--numReps >= 0);
        }
    }

    public static class GVGlyphMetrics extends GVMeasureTest {
        public GVGlyphMetrics() {
            super(measuretestroot, "gvGlyphMetrics", "Getting GV Glyph Metrics");
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            GlyphVector gv = gvctx.gv;
            GlyphMetrics gm;
            do {
                for (int i = 0, e = gv.getNumGlyphs(); i < e; ++i) {
                    gm = gv.getGlyphMetrics(i);
                }
            } while (--numReps >= 0);
        }
    }

    public static class TLContext extends G2DContext {
        TextLayout tl;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            // need more tl options here
            tl = new TextLayout(text, font, frc);
        }
    }

    public abstract static class TLMeasureTest extends TextMeasureTests {
        protected TLMeasureTest(Group parent, String nodeName, String description) {
            super(parent, nodeName, description);
        }

        public Context createContext() {
            return new TLContext();
        }
    }

    public static class TLAdvance extends TLMeasureTest {
        public TLAdvance() {
            super(measuretestroot, "tlAdvance", "Measuring TL advance");
        }

        public void runTest(Object ctx, int numReps) {
            TLContext tlctx = (TLContext)ctx;
            TextLayout tl = tlctx.tl;
            double wid = 0;
            do {
                wid += tl.getAdvance();
            } while (--numReps >= 0);
        }
    }

    public static class TLAscent extends TLMeasureTest {
        public TLAscent() {
            super(measuretestroot, "tlAscent", "Measuring TL ascent");
        }

        public void runTest(Object ctx, int numReps) {
            TLContext tlctx = (TLContext)ctx;
            TextLayout tl = tlctx.tl;
            float ht = 0;
            do {
                ht += tl.getAscent();
            } while (--numReps >= 0);
        }
    }

    public static class TLBounds extends TLMeasureTest {
        public TLBounds() {
            super(measuretestroot, "tlBounds", "Measuring TL advance");
        }

        public void runTest(Object ctx, int numReps) {
            TLContext tlctx = (TLContext)ctx;
            TextLayout tl = tlctx.tl;
            Rectangle2D r;
            do {
                r = tl.getBounds();
            } while (--numReps >= 0);
        }
    }

    static class TLExContext extends TLContext {
        TextHitInfo[] hits;
        Rectangle2D lb;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            ArrayList list = new ArrayList(text.length() * 2 + 2);
            TextHitInfo hit = TextHitInfo.trailing(-1);
            do {
                list.add(hit);
                hit = tl.getNextRightHit(hit);
            } while (hit != null);
            hits = (TextHitInfo[])list.toArray(new TextHitInfo[list.size()]);

            lb = tl.getBounds();
            lb.setRect(lb.getMinX() - 10, lb.getMinY(), lb.getWidth() + 20, lb.getHeight());
        }
    }

    public abstract static class TLExtendedMeasureTest extends TLMeasureTest {
        protected TLExtendedMeasureTest(Group parent, String nodeName, String description) {
            super(parent, nodeName, description);
        }

        public Context createContext() {
            return new TLExContext();
        }
    }

    public static class TLGetCaretInfo extends TLExtendedMeasureTest {
        public TLGetCaretInfo() {
            super(measuretestroot, "tlGetCaretInfo", "Measuring TL caret info");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            TextHitInfo[] hits = tlctx.hits;
            do {
                for (int i = 0; i < hits.length; ++i) {
                    tl.getCaretInfo(hits[i]);
                }
            } while (--numReps >= 0);
        }
    }

    public static class TLGetNextHit extends TLExtendedMeasureTest {
        public TLGetNextHit() {
            super(measuretestroot, "tlGetNextHit", "Measuring TL getNextRight/LeftHit");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            TextHitInfo[] hits = tlctx.hits;
            TextHitInfo hit;
            do {
                for (int i = 0; i < hits.length; ++i) {
                    hit = tl.getNextLeftHit(hits[i]);
                }
            } while (--numReps >= 0);
        }
    }

    public static class TLGetCaretShape extends TLExtendedMeasureTest {
        public TLGetCaretShape() {
            super(measuretestroot, "tlGetCaretShape", "Measuring TL getCaretShape");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            TextHitInfo[] hits = tlctx.hits;
            Shape s;
            do {
                for (int i = 0; i < hits.length; ++i) {
                    s = tl.getCaretShape(hits[i]);
                }
            } while (--numReps >= 0);
        }
    }

    public static class TLGetLogicalHighlightShape extends TLExtendedMeasureTest {
        public TLGetLogicalHighlightShape() {
            super(measuretestroot, "tlGetLogicalHighlightShape", "Measuring TL getLogicalHighlightShape");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            int len = tlctx.text.length();
            Rectangle2D lb = tlctx.lb;
            Shape s;
            if (len < 3) {
                do {
                    s = tl.getLogicalHighlightShape(0, len, lb);
                } while (--numReps >= 0);
            } else {
                do {
                    for (int i = 3; i < len; ++i) {
                        s = tl.getLogicalHighlightShape(i-3, i, lb);
                    }
                } while (--numReps >= 0);
            }
        }
    }

    public static class TLGetVisualHighlightShape extends TLExtendedMeasureTest {
        public TLGetVisualHighlightShape() {
            super(measuretestroot, "tlGetVisualHighlightShape", "Measuring TL getVisualHighlightShape");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            TextHitInfo[] hits = tlctx.hits;
            Rectangle2D lb = tlctx.lb;
            Shape s;
            if (hits.length < 3) {
                do {
                    s = tl.getVisualHighlightShape(hits[0], hits[hits.length - 1], lb);
                } while (--numReps >= 0);
            } else {
                do {
                    for (int i = 3; i < hits.length; ++i) {
                        s = tl.getVisualHighlightShape(hits[i-3], hits[i], lb);
                    }
                } while (--numReps >= 0);
            }
        }
    }

    public static class TLHitTest extends TLExtendedMeasureTest {
        public TLHitTest() {
            super(measuretestroot, "tlHitTest", "Measuring TL hitTest");
        }

        public void runTest(Object ctx, int numReps) {
            TLExContext tlctx = (TLExContext)ctx;
            TextLayout tl = tlctx.tl;
            int numhits = tlctx.hits.length;
            Rectangle2D lb = tlctx.lb;
            TextHitInfo hit;
            for (int i = 0; i <= numhits; ++i) {
                float x = (float)(lb.getMinX() + lb.getWidth() * i / numhits);
                float y = (float)(lb.getMinY() + lb.getHeight() * i / numhits);
                hit = tl.hitTestChar(x, y, lb);
            }
        }
    }

    public static class TLOutline extends TLMeasureTest {
        public TLOutline() {
            super(measuretestroot, "tlOutline", "Measuring TL outline");
        }

        public void runTest(Object ctx, int numReps) {
            TLContext tlctx = (TLContext)ctx;
            TextLayout tl = tlctx.tl;
            Shape s;
            do {
                s = tl.getOutline(null);
            } while (--numReps >= 0);
        }
    }
}
