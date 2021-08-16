/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 *
 * (C) Copyright IBM Corp. 1999-2003 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

/*
 * GlyphLayout is used to process a run of text into a run of run of
 * glyphs, optionally with position and char mapping info.
 *
 * The text has already been processed for numeric shaping and bidi.
 * The run of text that layout works on has a single bidi level.  It
 * also has a single font/style.  Some operations need context to work
 * on (shaping, script resolution) so context for the text run text is
 * provided.  It is assumed that the text array contains sufficient
 * context, and the offset and count delimit the portion of the text
 * that needs to actually be processed.
 *
 * The font might be a composite font.  Layout generally requires
 * tables from a single physical font to operate, and so it must
 * resolve the 'single' font run into runs of physical fonts.
 *
 * Some characters are supported by several fonts of a composite, and
 * in order to properly emulate the glyph substitution behavior of a
 * single physical font, these characters might need to be mapped to
 * different physical fonts.  The script code that is assigned
 * characters normally considered 'common script' can be used to
 * resolve which physical font to use for these characters. The input
 * to the char to glyph mapper (which assigns physical fonts as it
 * processes the glyphs) should include the script code, and the
 * mapper should operate on runs of a single script.
 *
 * To perform layout, call get() to get a new (or reuse an old)
 * GlyphLayout, call layout on it, then call done(GlyphLayout) when
 * finished.  There's no particular problem if you don't call done,
 * but it assists in reuse of the GlyphLayout.
 */

package sun.font;

import java.lang.ref.SoftReference;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Character.*;

public final class GlyphLayout {
    // data for glyph vector
    private GVData _gvdata;

    // cached glyph layout data for reuse
    private static volatile GlyphLayout cache;  // reusable

    private LayoutEngineFactory _lef;  // set when get is called, unset when done is called
    private TextRecord _textRecord;    // the text we're working on, used by iterators
    private ScriptRun _scriptRuns;     // iterator over script runs
    private FontRunIterator _fontRuns; // iterator over physical fonts in a composite
    private int _ercount;
    private ArrayList<EngineRecord> _erecords;
    private Point2D.Float _pt;
    private FontStrikeDesc _sd;
    private float[] _mat;
    private float ptSize;
    private int _typo_flags;
    private int _offset;

    public static final class LayoutEngineKey {
        private Font2D font;
        private int script;
        private int lang;

        LayoutEngineKey() {
        }

        LayoutEngineKey(Font2D font, int script, int lang) {
            init(font, script, lang);
        }

        void init(Font2D font, int script, int lang) {
            this.font = font;
            this.script = script;
            this.lang = lang;
        }

        LayoutEngineKey copy() {
            return new LayoutEngineKey(font, script, lang);
        }

        Font2D font() {
            return font;
        }

        int script() {
            return script;
        }

        int lang() {
            return lang;
        }

        public boolean equals(Object rhs) {
            if (this == rhs) return true;
            if (rhs == null) return false;
            try {
                LayoutEngineKey that = (LayoutEngineKey)rhs;
                return this.script == that.script &&
                       this.lang == that.lang &&
                       this.font.equals(that.font);
            }
            catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return script ^ lang ^ font.hashCode();
        }
    }

    public static interface LayoutEngineFactory {
        /**
         * Given a font, script, and language, determine a layout engine to use.
         */
        public LayoutEngine getEngine(Font2D font, int script, int lang);

        /**
         * Given a key, determine a layout engine to use.
         */
        public LayoutEngine getEngine(LayoutEngineKey key);
    }

    public static interface LayoutEngine {
        /**
         * Given a strike descriptor, text, rtl flag, and starting point, append information about
         * glyphs, positions, and character indices to the glyphvector data, and advance the point.
         *
         * If the GVData does not have room for the glyphs, throws an IndexOutOfBoundsException and
         * leave pt and the gvdata unchanged.
         */
        public void layout(FontStrikeDesc sd, float[] mat, float ptSize, int gmask,
                           int baseIndex, TextRecord text, int typo_flags, Point2D.Float pt, GVData data);
    }

    /**
     * Return a new instance of GlyphLayout, using the provided layout engine factory.
     * If null, the system layout engine factory will be used.
     */
    public static GlyphLayout get(LayoutEngineFactory lef) {
        if (lef == null) {
            lef = SunLayoutEngine.instance();
        }
        GlyphLayout result = null;
        synchronized(GlyphLayout.class) {
            if (cache != null) {
                result = cache;
                cache = null;
            }
        }
        if (result == null) {
            result = new GlyphLayout();
        }
        result._lef = lef;
        return result;
    }

    /**
     * Return the old instance of GlyphLayout when you are done.  This enables reuse
     * of GlyphLayout objects.
     */
    public static void done(GlyphLayout gl) {
        gl._lef = null;
        cache = gl; // object reference assignment is thread safe, it says here...
    }

    private static final class SDCache {
        public Font key_font;
        public FontRenderContext key_frc;

        public AffineTransform dtx;
        public AffineTransform gtx;
        public Point2D.Float delta;
        public FontStrikeDesc sd;

        private SDCache(Font font, FontRenderContext frc) {
            key_font = font;
            key_frc = frc;

            // !!! add getVectorTransform and hasVectorTransform to frc?  then
            // we could just skip this work...

            dtx = frc.getTransform();
            dtx.setTransform(dtx.getScaleX(), dtx.getShearY(),
                             dtx.getShearX(), dtx.getScaleY(),
                             0, 0);

            float ptSize = font.getSize2D();
            if (font.isTransformed()) {
                gtx = font.getTransform();
                gtx.scale(ptSize, ptSize);
                delta = new Point2D.Float((float)gtx.getTranslateX(),
                                          (float)gtx.getTranslateY());
                gtx.setTransform(gtx.getScaleX(), gtx.getShearY(),
                                 gtx.getShearX(), gtx.getScaleY(),
                                 0, 0);
                gtx.preConcatenate(dtx);
            } else {
                delta = ZERO_DELTA;
                gtx = new AffineTransform(dtx);
                gtx.scale(ptSize, ptSize);
            }

            /* Similar logic to that used in SunGraphics2D.checkFontInfo().
             * Whether a grey (AA) strike is needed is size dependent if
             * AA mode is 'gasp'.
             */
            int aa =
                FontStrikeDesc.getAAHintIntVal(frc.getAntiAliasingHint(),
                                               FontUtilities.getFont2D(font),
                                               (int)Math.abs(ptSize));
            int fm = FontStrikeDesc.getFMHintIntVal
                (frc.getFractionalMetricsHint());
            sd = new FontStrikeDesc(dtx, gtx, font.getStyle(), aa, fm);
        }

        private static final Point2D.Float ZERO_DELTA = new Point2D.Float();

        private static
            SoftReference<ConcurrentHashMap<SDKey, SDCache>> cacheRef;

        private static final class SDKey {
            private final Font font;
            private final FontRenderContext frc;
            private final int hash;

            SDKey(Font font, FontRenderContext frc) {
                this.font = font;
                this.frc = frc;
                this.hash = font.hashCode() ^ frc.hashCode();
            }

            public int hashCode() {
                return hash;
            }

            public boolean equals(Object o) {
                try {
                    SDKey rhs = (SDKey)o;
                    return
                        hash == rhs.hash &&
                        font.equals(rhs.font) &&
                        frc.equals(rhs.frc);
                }
                catch (ClassCastException e) {
                }
                return false;
            }
        }

        public static SDCache get(Font font, FontRenderContext frc) {

            // It is possible a translation component will be in the FRC.
            // It doesn't affect us except adversely as we would consider
            // FRC's which are really the same to be different. If we
            // detect a translation component, then we need to exclude it
            // by creating a new transform which excludes the translation.
            if (frc.isTransformed()) {
                AffineTransform transform = frc.getTransform();
                if (transform.getTranslateX() != 0 ||
                    transform.getTranslateY() != 0) {
                    transform = new AffineTransform(transform.getScaleX(),
                                                    transform.getShearY(),
                                                    transform.getShearX(),
                                                    transform.getScaleY(),
                                                    0, 0);
                    frc = new FontRenderContext(transform,
                                                frc.getAntiAliasingHint(),
                                                frc.getFractionalMetricsHint()
                                                );
                }
            }

            SDKey key = new SDKey(font, frc); // garbage, yuck...
            ConcurrentHashMap<SDKey, SDCache> cache = null;
            SDCache res = null;
            if (cacheRef != null) {
                cache = cacheRef.get();
                if (cache != null) {
                    res = cache.get(key);
                }
            }
            if (res == null) {
                res = new SDCache(font, frc);
                if (cache == null) {
                    cache = new ConcurrentHashMap<SDKey, SDCache>(10);
                    cacheRef = new
                       SoftReference<ConcurrentHashMap<SDKey, SDCache>>(cache);
                } else if (cache.size() >= 512) {
                    cache.clear();
                }
                cache.put(key, res);
            }
            return res;
        }
    }

    /**
     * Create a glyph vector.
     * @param font the font to use
     * @param frc the font render context
     * @param text the text, including optional context before start and after start + count
     * @param offset the start of the text to lay out
     * @param count the length of the text to lay out
     * @param flags bidi and context flags {@see #java.awt.Font}
     * @param result a StandardGlyphVector to modify, can be null
     * @return the layed out glyphvector, if result was passed in, it is returned
     */
    public StandardGlyphVector layout(Font font, FontRenderContext frc,
                                      char[] text, int offset, int count,
                                      int flags, StandardGlyphVector result)
    {
        if (text == null || offset < 0 || count < 0 || (count > text.length - offset)) {
            throw new IllegalArgumentException();
        }

        init(count);

        // need to set after init
        // go through the back door for this
        if (font.hasLayoutAttributes()) {
            AttributeValues values = ((AttributeMap)font.getAttributes()).getValues();
            if (values.getKerning() != 0) _typo_flags |= 0x1;
            if (values.getLigatures() != 0) _typo_flags |= 0x2;
        }

        _offset = offset;

        // use cache now - can we use the strike cache for this?

        SDCache txinfo = SDCache.get(font, frc);
        _mat[0] = (float)txinfo.gtx.getScaleX();
        _mat[1] = (float)txinfo.gtx.getShearY();
        _mat[2] = (float)txinfo.gtx.getShearX();
        _mat[3] = (float)txinfo.gtx.getScaleY();
        _pt.setLocation(txinfo.delta);
        ptSize = font.getSize2D();

        int lim = offset + count;

        int min = 0;
        int max = text.length;
        if (flags != 0) {
            if ((flags & Font.LAYOUT_RIGHT_TO_LEFT) != 0) {
              _typo_flags |= 0x80000000; // RTL
            }

            if ((flags & Font.LAYOUT_NO_START_CONTEXT) != 0) {
                min = offset;
            }

            if ((flags & Font.LAYOUT_NO_LIMIT_CONTEXT) != 0) {
                max = lim;
            }
        }

        int lang = -1; // default for now

        Font2D font2D = FontUtilities.getFont2D(font);
        if (font2D instanceof FontSubstitution) {
            font2D = ((FontSubstitution)font2D).getCompositeFont2D();
        }

        _textRecord.init(text, offset, lim, min, max);
        int start = offset;
        if (font2D instanceof CompositeFont) {
            _scriptRuns.init(text, offset, count); // ??? how to handle 'common' chars
            _fontRuns.init((CompositeFont)font2D, text, offset, lim);
            while (_scriptRuns.next()) {
                int limit = _scriptRuns.getScriptLimit();
                int script = _scriptRuns.getScriptCode();
                while (_fontRuns.next(script, limit)) {
                    Font2D pfont = _fontRuns.getFont();
                    /* layout can't deal with NativeFont instances. The
                     * native font is assumed to know of a suitable non-native
                     * substitute font. This currently works because
                     * its consistent with the way NativeFonts delegate
                     * in other cases too.
                     */
                    if (pfont instanceof NativeFont) {
                        pfont = ((NativeFont)pfont).getDelegateFont();
                    }
                    int gmask = _fontRuns.getGlyphMask();
                    int pos = _fontRuns.getPos();
                    nextEngineRecord(start, pos, script, lang, pfont, gmask);
                    start = pos;
                }
            }
        } else {
            _scriptRuns.init(text, offset, count); // ??? don't worry about 'common' chars
            while (_scriptRuns.next()) {
                int limit = _scriptRuns.getScriptLimit();
                int script = _scriptRuns.getScriptCode();
                nextEngineRecord(start, limit, script, lang, font2D, 0);
                start = limit;
            }
        }

        int ix = 0;
        int stop = _ercount;
        int dir = 1;

        if (_typo_flags < 0) { // RTL
            ix = stop - 1;
            stop = -1;
            dir = -1;
        }

        //        _sd.init(dtx, gtx, font.getStyle(), frc.isAntiAliased(), frc.usesFractionalMetrics());
        _sd = txinfo.sd;
        for (;ix != stop; ix += dir) {
            EngineRecord er = _erecords.get(ix);
            for (;;) {
                try {
                    er.layout();
                    break;
                }
                catch (IndexOutOfBoundsException e) {
                    if (_gvdata._count >=0) {
                        _gvdata.grow();
                    }
                }
            }
            // Break out of the outer for loop if layout fails.
            if (_gvdata._count < 0) {
                break;
            }
        }

        // If layout fails (negative glyph count) create an un-laid out GV instead.
        // ie default positions. This will be a lot better than the alternative of
        // a complete blank layout.
        StandardGlyphVector gv;
        if (_gvdata._count < 0) {
            gv = new StandardGlyphVector(font, text, offset, count, frc);
            if (FontUtilities.debugFonts()) {
               FontUtilities.logWarning("OpenType layout failed on font: " + font);
            }
        } else {
            gv = _gvdata.createGlyphVector(font, frc, result);
        }
        //        System.err.println("Layout returns: " + gv);
        return gv;
    }

    //
    // private methods
    //

    private GlyphLayout() {
        this._gvdata = new GVData();
        this._textRecord = new TextRecord();
        this._scriptRuns = new ScriptRun();
        this._fontRuns = new FontRunIterator();
        this._erecords = new ArrayList<>(10);
        this._pt = new Point2D.Float();
        this._sd = new FontStrikeDesc();
        this._mat = new float[4];
    }

    private void init(int capacity) {
        this._typo_flags = 0;
        this._ercount = 0;
        this._gvdata.init(capacity);
    }

    private void nextEngineRecord(int start, int limit, int script, int lang, Font2D font, int gmask) {
        EngineRecord er = null;
        if (_ercount == _erecords.size()) {
            er = new EngineRecord();
            _erecords.add(er);
        } else {
            er = _erecords.get(_ercount);
        }
        er.init(start, limit, font, script, lang, gmask);
        ++_ercount;
    }

    /**
     * Storage for layout to build glyph vector data, then generate a real GlyphVector
     */
    public static final class GVData {
        public int _count; // number of glyphs, >= number of chars
        public int _flags;
        public int[] _glyphs;
        public float[] _positions;
        public int[] _indices;

        private static final int UNINITIALIZED_FLAGS = -1;

        public void init(int size) {
            _count = 0;
            _flags = UNINITIALIZED_FLAGS;

            if (_glyphs == null || _glyphs.length < size) {
                if (size < 20) {
                    size = 20;
                }
                _glyphs = new int[size];
                _positions = new float[size * 2 + 2];
                _indices = new int[size];
            }
        }

        public void grow() {
            grow(_glyphs.length / 4); // always grows because min length is 20
        }

        public void grow(int delta) {
            int size = _glyphs.length + delta;
            int[] nglyphs = new int[size];
            System.arraycopy(_glyphs, 0, nglyphs, 0, _count);
            _glyphs = nglyphs;

            float[] npositions = new float[size * 2 + 2];
            System.arraycopy(_positions, 0, npositions, 0, _count * 2 + 2);
            _positions = npositions;

            int[] nindices = new int[size];
            System.arraycopy(_indices, 0, nindices, 0, _count);
            _indices = nindices;
        }

        public StandardGlyphVector createGlyphVector(Font font, FontRenderContext frc, StandardGlyphVector result) {

            // !!! default initialization until we let layout engines do it
            if (_flags == UNINITIALIZED_FLAGS) {
                _flags = 0;

                if (_count > 1) { // if only 1 glyph assume LTR
                    boolean ltr = true;
                    boolean rtl = true;

                    int rtlix = _count; // rtl index
                    for (int i = 0; i < _count && (ltr || rtl); ++i) {
                        int cx = _indices[i];

                        ltr = ltr && (cx == i);
                        rtl = rtl && (cx == --rtlix);
                    }

                    if (rtl) _flags |= GlyphVector.FLAG_RUN_RTL;
                    if (!rtl && !ltr) _flags |= GlyphVector.FLAG_COMPLEX_GLYPHS;
                }

                // !!! layout engines need to tell us whether they performed
                // position adjustments. currently they don't tell us, so
                // we must assume they did
                _flags |= GlyphVector.FLAG_HAS_POSITION_ADJUSTMENTS;
            }

            int[] glyphs = new int[_count];
            System.arraycopy(_glyphs, 0, glyphs, 0, _count);

            float[] positions = null;
            if ((_flags & GlyphVector.FLAG_HAS_POSITION_ADJUSTMENTS) != 0) {
                positions = new float[_count * 2 + 2];
                System.arraycopy(_positions, 0, positions, 0, positions.length);
            }

            int[] indices = null;
            if ((_flags & GlyphVector.FLAG_COMPLEX_GLYPHS) != 0) {
                indices = new int[_count];
                System.arraycopy(_indices, 0, indices, 0, _count);
            }

            if (result == null) {
                result = new StandardGlyphVector(font, frc, glyphs, positions, indices, _flags);
            } else {
                result.initGlyphVector(font, frc, glyphs, positions, indices, _flags);
            }

            return result;
        }
    }

    /**
     * Utility class to keep track of script runs, which may have to be reordered rtl when we're
     * finished.
     */
    private final class EngineRecord {
        private int start;
        private int limit;
        private int gmask;
        private int eflags;
        private LayoutEngineKey key;
        private LayoutEngine engine;

        EngineRecord() {
            key = new LayoutEngineKey();
        }

        void init(int start, int limit, Font2D font, int script, int lang, int gmask) {
            this.start = start;
            this.limit = limit;
            this.gmask = gmask;
            this.key.init(font, script, lang);
            this.eflags = 0;

            // only request canonical substitution if we have combining marks
            for (int i = start; i < limit; ++i) {
                int ch = _textRecord.text[i];
                if (isHighSurrogate((char)ch) &&
                    i < limit - 1 &&
                    isLowSurrogate(_textRecord.text[i+1])) {
                    // rare case
                    ch = toCodePoint((char)ch,_textRecord.text[++i]); // inc
                }
                int gc = getType(ch);
                if (gc == NON_SPACING_MARK ||
                    gc == ENCLOSING_MARK ||
                    gc == COMBINING_SPACING_MARK) { // could do range test also

                    this.eflags = 0x4;
                    break;
                }
            }

            this.engine = _lef.getEngine(key); // flags?
        }

        void layout() {
            _textRecord.start = start;
            _textRecord.limit = limit;
            engine.layout(_sd, _mat, ptSize, gmask, start - _offset, _textRecord,
                          _typo_flags | eflags, _pt, _gvdata);
        }
    }
}
