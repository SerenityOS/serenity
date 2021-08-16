/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 *
 */

/*
 *
 * (C) Copyright IBM Corp. 2003 - All Rights Reserved
 */

package sun.font;

/**
 * Iterates over runs of fonts in a CompositeFont, optionally taking script runs into account.
 */
public final class FontRunIterator {
    CompositeFont font;
    char[] text;
    int start;
    int limit;

    CompositeGlyphMapper mapper; // handy cache

    int slot = -1;
    int pos;

    public void init(CompositeFont font, char[] text, int start, int limit) {
        if (font == null || text == null || start < 0 || limit < start || limit > text.length) {
            throw new IllegalArgumentException();
        }

        this.font = font;
        this.text = text;
        this.start = start;
        this.limit = limit;

        this.mapper = (CompositeGlyphMapper)font.getMapper();
        this.slot = -1;
        this.pos = start;
    }

    public PhysicalFont getFont() {
        return slot == -1 ? null : font.getSlotFont(slot);
    }

    public int getGlyphMask() {
        return slot << 24;
    }

    public int getPos() {
        return pos;
    }

    /*
     * characters that are in the 'common' script become part of the
     * surrounding script run.  we want to fetch these from the same font
     * used to get surrounding characters, where possible.  but we don't
     * want to force non-common characters to come from other than their
     * standard font.
     *
     * what we really want to do is this:
     * 1) fetch a code point from the text.
     * 2) get its 'native' script code
     * 3) determine its 'resolved' script code
     * 4) if its native script is COMMON, and its resolved script is the same as the previous
     *    code point's, then see if the previous font supports this code point.  if so, use it.
     * 5) otherwise resolve the font as usual
     * 6) break the run when either the physical font or the resolved script changes.
     *
     * problems: we optimize latin-1 and cjk text assuming a fixed
     * width for each character.  since latin-1 digits and punctuation
     * are common, following this algorithm they will change to match
     * the fonts used for the preceding text, and potentially change metrics.
     *
     * this also seems to have the potential for changing arbitrary runs of text, e.g.
     * any number of digits and spaces can change depending on the preceding (or following!)
     * non-COMMON character's font assignment.  this is not good.
     *
     * since the goal is to enable layout to be performed using as few physical fonts as
     * possible, and the primary cause of switching fonts is to handle spaces, perhaps
     * we should just special-case spaces and assign them from the current font, whatever
     * it may be.
     *
     * One could also argue that the job of the composite font is to assign physical fonts
     * to text runs, however it wishes.  we don't necessarily have to provide script info
     * to let it do this.  it can determine based on whatever.  so having a special 'next'
     * function that takes script (and limit) is redundant.  It can fetch the script again
     * if need be.
     *
     * both this and the script iterator are turning char sequences into code point
     * sequences.  maybe it would be better to feed a single code point into each iterator-- push
     * the data instead of pull it?
     */

    public boolean next(int script, int lim) {
        if (pos == lim) {
            return false;
        }

        int ch = nextCodePoint(lim);
        int sl = mapper.charToGlyph(ch) & CompositeGlyphMapper.SLOTMASK;
        slot = sl >>> 24;
        while ((ch = nextCodePoint(lim)) != DONE && (mapper.charToGlyph(ch) & CompositeGlyphMapper.SLOTMASK) == sl);
        pushback(ch);

        return true;
    }

    public boolean next() {
        return next(Script.COMMON, limit);
    }

    static final int SURROGATE_START = 0x10000;
    static final int LEAD_START = 0xd800;
    static final int LEAD_LIMIT = 0xdc00;
    static final int TAIL_START = 0xdc00;
    static final int TAIL_LIMIT = 0xe000;
    static final int LEAD_SURROGATE_SHIFT = 10;
    static final int SURROGATE_OFFSET = SURROGATE_START - (LEAD_START << LEAD_SURROGATE_SHIFT) - TAIL_START;

    static final int DONE = -1;

    int nextCodePoint() {
        return nextCodePoint(limit);
    }

    int nextCodePoint(int lim) {
        if (pos >= lim) {
            return DONE;
        }
        int ch = text[pos++];
        if (ch >= LEAD_START && ch < LEAD_LIMIT && pos < lim) {
            int nch = text[pos];
            if (nch >= TAIL_START && nch < TAIL_LIMIT) {
                ++pos;
                ch = (ch << LEAD_SURROGATE_SHIFT) + nch + SURROGATE_OFFSET;
            }
        }
        return ch;
    }

    void pushback(int ch) {
        if (ch >= 0) {
            if (ch >= 0x10000) {
                pos -= 2;
            } else {
                pos -= 1;
            }
        }
    }
}
