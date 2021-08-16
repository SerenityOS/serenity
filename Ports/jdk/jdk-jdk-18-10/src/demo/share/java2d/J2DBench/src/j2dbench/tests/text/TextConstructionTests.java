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
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.Bidi;
import java.text.CharacterIterator;
import java.util.Map;

import j2dbench.Group;
import j2dbench.Result;
import j2dbench.TestEnvironment;

public abstract class TextConstructionTests extends TextTests {
    static Group constructroot;
    static Group constructtestroot;

    public static void init() {
      // don't even bother with this at all if we don't have Java2 APIs.
      if (hasGraphics2D) {
        constructroot = new Group(textroot, "construction", "Construction Benchmarks");
        constructtestroot = new Group(constructroot, "tests", "Construction Tests");

        new GVFromFontString();
        new GVFromFontChars();
        new GVFromFontCI();
        new GVFromFontGlyphs();
        new GVFromFontLayout();
        //  new GVClone(); // not public API!

        new TLFromFont();
        new TLFromMap();
        /*
        new TLFromACI();
        new TLClone();
        new TLJustify();
        new TLFromLBM();
        */
      }
    }

    public TextConstructionTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    public static class TCContext extends G2DContext {
        char[] chars1;
        CharacterIterator ci;
        GlyphVector gv;
        int[] glyphs;
        int flags;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            chars1 = new char[chars.length + 2];
            System.arraycopy(chars, 0, chars1, 1, chars.length);
            ci = new ArrayCI(chars1, 1, chars.length);
            gv = font.createGlyphVector(frc, text);
            glyphs = gv.getGlyphCodes(0, gv.getNumGlyphs(), null);
            flags = Bidi.requiresBidi(chars, 0, chars.length)
                ? Font.LAYOUT_LEFT_TO_RIGHT
                : Font.LAYOUT_RIGHT_TO_LEFT;
        }
    }

    public Context createContext() {
        return new TCContext();
    }

    public static class GVFromFontString extends TextConstructionTests {
        public GVFromFontString() {
            super(constructtestroot, "gvfromfontstring", "Call Font.createGlyphVector(FRC, String)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            GlyphVector gv;
            do {
                gv = font.createGlyphVector(frc, text);
            } while (--numReps >= 0);
        }
    }

    public static class GVFromFontChars extends TextConstructionTests {
        public GVFromFontChars() {
            super(constructtestroot, "gvfromfontchars", "Call Font.createGlyphVector(FRC, char[])");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final char[] chars = tcctx.chars;
            final FontRenderContext frc = tcctx.frc;
            GlyphVector gv;
            do {
                gv = font.createGlyphVector(frc, chars);
            } while (--numReps >= 0);
        }
    }

    public static class GVFromFontCI extends TextConstructionTests {
        public GVFromFontCI() {
            super(constructtestroot, "gvfromfontci", "Call Font.createGlyphVector(FRC, CharacterIterator)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final CharacterIterator ci = tcctx.ci;
            final FontRenderContext frc = tcctx.frc;
            GlyphVector gv;
            do {
                gv = font.createGlyphVector(frc, ci);
            } while (--numReps >= 0);
        }
    }

    public static class GVFromFontGlyphs extends TextConstructionTests {
        public GVFromFontGlyphs() {
            super(constructtestroot, "gvfromfontglyphs", "Call Font.createGlyphVector(FRC, int[])");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final int[] glyphs = tcctx.glyphs;
            final FontRenderContext frc = tcctx.frc;
            GlyphVector gv;
            do {
                gv = font.createGlyphVector(frc, glyphs);
            } while (--numReps >= 0);
        }
    }

    public static class GVFromFontLayout extends TextConstructionTests {
        public GVFromFontLayout() {
            super(constructtestroot, "gvfromfontlayout", "Call Font.layoutGlyphVector(...)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final char[] chars = tcctx.chars1;
            final int start = 1;
            final int limit = chars.length - 1;
            final FontRenderContext frc = tcctx.frc;
            final int flags = tcctx.flags;
            GlyphVector gv;
            do {
                gv = font.layoutGlyphVector(frc, chars, start, limit, flags);
            } while (--numReps >= 0);
        }
    }

    /*
     * my bad, clone is not public in GlyphVector!

    public static class GVClone extends TextConstructionTests {
        public GVClone() {
            super(constructtestroot, "gvclone", "Call GV.clone");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final GlyphVector origGV = tcctx.gv;
            GlyphVector gv;
            do {
                gv = (GlyphVector)origGV.clone();
            } while (--numReps >= 0);
        }
    }
    */

    public static class TLFromFont extends TextConstructionTests {
        public TLFromFont() {
            super(constructtestroot, "tlfromfont", "TextLayout(String, Font, FRC)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            TextLayout tl;
            do {
                tl = new TextLayout(text, font, frc);
            } while (--numReps >= 0);
        }
    }

    public static class TLMapContext extends G2DContext {
        Map map;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            map = (Map)env.getModifier(tlmapList);
        }

    }

    public static class TLFromMap extends TextConstructionTests {
        public TLFromMap() {
            super(constructtestroot, "tlfrommap", "TextLayout(String, Map, FRC)");
        }

        public Context createContext() {
            return new TLMapContext();
        }

        public void runTest(Object ctx, int numReps) {
            TLMapContext tlmctx = (TLMapContext)ctx;
            final String text = tlmctx.text;
            final FontRenderContext frc = tlmctx.frc;
            final Map map = tlmctx.map;
            TextLayout tl;
            do {
                tl = new TextLayout(text, map, frc);
            } while (--numReps >= 0);
        }
    }

    public static class ACIContext extends G2DContext {
        AttributedCharacterIterator aci;

        public void init(TestEnvironment env, Result results) {
            super.init(env, results);

            AttributedString astr = new AttributedString(text);

        }
    }

    public class TLFromACI extends TextConstructionTests {
        public TLFromACI() {
            super(constructtestroot, "tlfromaci", "TextLayout(ACI, FRC)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            TextLayout tl;
            do {
                tl = new TextLayout(text, font, frc);
            } while (--numReps >= 0);
        }
    }

    public class TLClone extends TextConstructionTests {
        public TLClone() {
            super(constructtestroot, "tlclone", "call TextLayout.clone()");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            TextLayout tl;
            do {
                tl = new TextLayout(text, font, frc);
            } while (--numReps >= 0);
        }
    }

    public class TLJustify extends TextConstructionTests {
        public TLJustify() {
            super(constructtestroot, "tljustify", "call TextLayout.getJustifiedLayout(...)");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            TextLayout tl;
            do {
                tl = new TextLayout(text, font, frc);
            } while (--numReps >= 0);
        }
    }

    public class TLFromLBM extends TextConstructionTests {
        public TLFromLBM() {
            super(constructtestroot, "tlfromlbm", "call LineBreakMeasurer.next()");
        }

        public void runTest(Object ctx, int numReps) {
            TCContext tcctx = (TCContext)ctx;
            final Font font = tcctx.font;
            final String text = tcctx.text;
            final FontRenderContext frc = tcctx.frc;
            TextLayout tl;
            do {
                tl = new TextLayout(text, font, frc);
            } while (--numReps >= 0);
        }
    }

    public static final class ArrayCI implements CharacterIterator {
        char[] chars;
        int off;
        int max;
        int pos;

        ArrayCI(char[] chars, int off, int len) {
            if (off < 0 || len < 0 || (len > 0 && (chars == null || chars.length - off < len))) {
                throw new InternalError("bad ArrayCI params");
            }
            this.chars = chars;
            this.off = off;
            this.max = off + len;
            this.pos = off;
        }

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     * @return the first character in the text, or DONE if the text is empty
     * @see #getBeginIndex()
     */
        public char first() {
            if (max > off) {
                return chars[pos = off];
            }
            return DONE;
        }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     * @return the last character in the text, or DONE if the text is empty
     * @see #getEndIndex()
     */
        public char last() {
            if (max > off) {
                return chars[pos = max - 1];
            }
            pos = max;
            return DONE;
        }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     * @return the character at the current position or DONE if the current
     * position is off the end of the text.
     * @see #getIndex()
     */
        public char current() {
            return pos == max ? DONE : chars[pos];
        }


    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of DONE is returned.
     * @return the character at the new position or DONE if the new
     * position is off the end of the text range.
     */
        public char next() {
            if (++pos < max) {
                return chars[pos];
            }
            pos = max;
            return DONE;
        }


    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of DONE is returned.
     * @return the character at the new position or DONE if the current
     * position is equal to getBeginIndex().
     */
        public char previous() {
            if (--pos >= off) {
                return chars[pos];
            }
            pos = off;
            return DONE;
        }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     */
        public char setIndex(int position) {
            if (position < off || position > max) {
                throw new IllegalArgumentException("pos: " + position + " off: " + off + " len: " + (max - off));
            }
            return ((pos = position) < max) ? chars[position] : DONE;
        }

    /**
     * Returns the start index of the text.
     * @return the index at which the text begins.
     */
        public int getBeginIndex() {
            return off;
        }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     * @return the index after the last character in the text
     */
        public int getEndIndex() {
            return max;
        }

    /**
     * Returns the current index.
     * @return the current index.
     */
        public int getIndex() {
            return pos;
        }

    /**
     * Create a copy of this iterator
     * @return A copy of this
     */
        public Object clone() {
            try {
                return super.clone();
            }
            catch (Exception e) {
                return new InternalError();
            }
        }
    }
}
