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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;

import j2dbench.Group;
import j2dbench.Result;
import j2dbench.TestEnvironment;

public abstract class TextRenderTests extends TextTests {
    static Group renderroot;
    static Group rendertestroot;

    public static void init() {
        renderroot = new Group(textroot, "Rendering", "Rendering Benchmarks");
        rendertestroot = new Group(renderroot, "tests", "Rendering Tests");

        new DrawStrings();
        new DrawChars();
        new DrawBytes();

        if (hasGraphics2D) {
            new DrawGlyphVectors();
            new DrawTextLayouts();
        }
    }

    public TextRenderTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    public static class DrawStrings extends TextRenderTests {
        public DrawStrings() {
            super(rendertestroot, "drawString", "Drawing Strings");
        }

        public void runTest(Object ctx, int numReps) {
            TextContext tctx = (TextContext)ctx;
            Graphics g = tctx.graphics;
            g.setFont(tctx.font);
            String text = tctx.text;
            do {
                g.drawString(text, 40, 40);
            } while (--numReps >= 0);
        }
    }

    public static class DrawChars extends TextRenderTests {
        public DrawChars() {
            super(rendertestroot, "drawChars", "Drawing Char Arrays");
        }

        public void runTest(Object ctx, int numReps) {
            TextContext tctx = (TextContext)ctx;
            Graphics g = tctx.graphics;
            char[] chars = tctx.chars;
            g.setFont(tctx.font);
            do {
                g.drawChars(chars, 0, chars.length, 40, 40);
            } while (--numReps >= 0);
        }
    }

    public static class DrawBytes extends TextRenderTests {
        public DrawBytes() {
            super(rendertestroot, "drawBytes", "Drawing Byte Arrays");
        }

        public void runTest(Object ctx, int numReps) {
            TextContext tctx = (TextContext)ctx;
            Graphics g = tctx.graphics;
            g.setFont(tctx.font);
            try {
                byte[] bytes = tctx.text.getBytes("ASCII"); // only good for english
                do {
                    g.drawBytes(bytes, 0, bytes.length, 40, 40);
                } while (--numReps >= 0);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class GVContext extends G2DContext {
        GlyphVector gv;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);
            gv = font.createGlyphVector(frc, text);
        }
    }

    public static class DrawGlyphVectors extends TextRenderTests {
        public DrawGlyphVectors() {
            super(rendertestroot, "drawGlyphVectors", "Drawing GlyphVectors");
        }

        public Context createContext() {
            return new GVContext();
        }

        public void runTest(Object ctx, int numReps) {
            GVContext gvctx = (GVContext)ctx;
            Graphics2D g2d = gvctx.g2d;
            GlyphVector gv = gvctx.gv;
            do {
                g2d.drawGlyphVector(gv, 40, 40);
            } while (--numReps >= 0);
        }
    }

    public static class TLContext extends G2DContext {
        TextLayout tl;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);
            tl = new TextLayout(text, font, frc);
        }
    }

    public static class DrawTextLayouts extends TextRenderTests {
        public DrawTextLayouts() {
            super(rendertestroot, "drawTextLayout", "Drawing TextLayouts");
        }

        public Context createContext() {
            return new TLContext();
        }

        public void runTest(Object ctx, int numReps) {
            TLContext tlctx = (TLContext)ctx;
            Graphics2D g2d = tlctx.g2d;
            TextLayout tl = tlctx.tl;
            do {
                tl.draw(g2d, 40, 40);
            } while (--numReps >= 0);
        }
    }
}
