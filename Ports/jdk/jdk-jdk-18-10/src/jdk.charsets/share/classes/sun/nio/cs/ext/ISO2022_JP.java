/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import sun.nio.cs.DelegatableDecoder;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.Surrogate;
import sun.nio.cs.US_ASCII;
import sun.nio.cs.*;
import static sun.nio.cs.CharsetMapping.*;

/*
 * Implementation notes:
 *
 * (1)"Standard based" (ASCII, JIS_X_0201 and JIS_X_0208) ISO2022-JP charset
 * is provided by the base implementation of this class.
 *
 * Three Microsoft ISO2022-JP variants, MS50220, MS50221 and MSISO2022JP
 * are provided via subclasses.
 *
 * (2)MS50220 and MS50221 are assumed to work the same way as Microsoft
 * CP50220 and CP50221's 7-bit implementation works by using CP5022X
 * specific JIS0208 and JIS0212 mapping tables (generated via Microsoft's
 * MultiByteToWideChar/WideCharToMultiByte APIs). The only difference
 * between these 2 classes is that MS50220 does not support singlebyte
 * halfwidth kana (Uff61-Uff9f) shiftin mechanism when "encoding", instead
 * these halfwidth kana characters are converted to their fullwidth JIS0208
 * counterparts.
 *
 * The difference between the standard JIS_X_0208 and JIS_X_0212 mappings
 * and the CP50220/50221 specific are
 *
 * 0208 mapping:
 *              1)0x213d <-> U2015 (compared to U2014)
 *              2)One way mappings for 5 characters below
 *                u2225 (ms) -> 0x2142 <-> u2016 (jis)
 *                uff0d (ms) -> 0x215d <-> u2212 (jis)
 *                uffe0 (ms) -> 0x2171 <-> u00a2 (jis)
 *                uffe1 (ms) -> 0x2172 <-> u00a3 (jis)
 *                uffe2 (ms) -> 0x224c <-> u00ac (jis)
 *                //should consider 0xff5e -> 0x2141 <-> U301c?
 *              3)NEC Row13 0x2d21-0x2d79
 *              4)85-94 ku <-> UE000,UE3AB (includes NEC selected
 *                IBM kanji in 89-92ku)
 *              5)UFF61-UFF9f -> Fullwidth 0208 KANA
 *
 * 0212 mapping:
 *              1)0x2237 <-> UFF5E (Fullwidth Tilde)
 *              2)0x2271 <-> U2116 (Numero Sign)
 *              3)85-94 ku <-> UE3AC - UE757
 *
 * (3)MSISO2022JP uses a JIS0208 mapping generated from MS932DB.b2c
 * and MS932DB.c2b by converting the SJIS codepoints back to their
 * JIS0208 counterparts. With the exception of
 *
 * (a)Codepoints with a resulting JIS0208 codepoints beyond 0x7e00 are
 *    dropped (this includs the IBM Extended Kanji/Non-kanji from 0x9321
 *    to 0x972c)
 * (b)The Unicode codepoints that the IBM Extended Kanji/Non-kanji are
 *    mapped to (in MS932) are mapped back to NEC selected IBM Kanji/
 *    Non-kanji area at 0x7921-0x7c7e.
 *
 * Compared to JIS_X_0208 mapping, this MS932 based mapping has
 *
 * (a)different mappings for 7 JIS codepoints
 *        0x213d <-> U2015
 *        0x2141 <-> UFF5E
 *        0x2142 <-> U2225
 *        0x215d <-> Uff0d
 *        0x2171 <-> Uffe0
 *        0x2172 <-> Uffe1
 *        0x224c <-> Uffe2
 * (b)added one-way c2b mappings for
 *        U00b8 -> 0x2124
 *        U00b7 -> 0x2126
 *        U00af -> 0x2131
 *        U00ab -> 0x2263
 *        U00bb -> 0x2264
 *        U3094 -> 0x2574
 *        U00b5 -> 0x264c
 * (c)NEC Row 13
 * (d)NEC selected IBM extended Kanji/Non-kanji
 *    These codepoints are mapped to the same Unicode codepoints as
 *    the MS932 does, while MS50220/50221 maps them to the Unicode
 *    private area.
 *
 * # There is also an interesting difference when compared to MS5022X
 *   0208 mapping for JIS codepoint "0x2D60", MS932 maps it to U301d
 *   but MS5022X maps it to U301e, obvious MS5022X is wrong, but...
 */

public class ISO2022_JP
    extends Charset
    implements HistoricallyNamedCharset
{
    private static final int ASCII = 0;                 // ESC ( B
    private static final int JISX0201_1976 = 1;         // ESC ( J
    private static final int JISX0208_1978 = 2;         // ESC $ @
    private static final int JISX0208_1983 = 3;         // ESC $ B
    private static final int JISX0212_1990 = 4;         // ESC $ ( D
    private static final int JISX0201_1976_KANA = 5;    // ESC ( I
    private static final int SHIFTOUT = 6;

    private static final int ESC = 0x1b;
    private static final int SO = 0x0e;
    private static final int SI = 0x0f;

    public ISO2022_JP() {
        super("ISO-2022-JP",
              ExtendedCharsets.aliasesFor("ISO-2022-JP"));
    }

    protected ISO2022_JP(String canonicalName,
                         String[] aliases) {
        super(canonicalName, aliases);
    }

    public String historicalName() {
        return "ISO2022JP";
    }

    public boolean contains(Charset cs) {
        return ((cs instanceof JIS_X_0201)
                || (cs instanceof US_ASCII)
                || (cs instanceof JIS_X_0208)
                || (cs instanceof ISO2022_JP));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    protected boolean doSBKANA() {
        return true;
    }

    static class Decoder extends CharsetDecoder
        implements DelegatableDecoder {

        static final DoubleByte.Decoder DEC0208 =
            (DoubleByte.Decoder)new JIS_X_0208().newDecoder();

        private int currentState;
        private int previousState;

        private final DoubleByte.Decoder dec0208;
        private final DoubleByte.Decoder dec0212;

        private Decoder(Charset cs) {
            this(cs, DEC0208, null);
        }

        protected Decoder(Charset cs,
                          DoubleByte.Decoder dec0208,
                          DoubleByte.Decoder dec0212) {
            super(cs, 0.5f, 1.0f);
            this.dec0208 = dec0208;
            this.dec0212 = dec0212;
            currentState = ASCII;
            previousState = ASCII;
        }

        public void implReset() {
            currentState = ASCII;
            previousState = ASCII;
        }

        private CoderResult decodeArrayLoop(ByteBuffer src,
                                            CharBuffer dst)
        {
            int inputSize;
            int b1, b2, b3, b4;
            char c;
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    b1 = sa[sp] & 0xff;
                    inputSize = 1;
                    if ((b1 & 0x80) != 0) {
                        return CoderResult.malformedForLength(inputSize);
                    }
                    if (b1 == ESC || b1 == SO || b1 == SI) {
                        if (b1 == ESC) {
                            if (sp + inputSize + 2 > sl)
                                return CoderResult.UNDERFLOW;
                            b2 = sa[sp + inputSize++] & 0xff;
                            if (b2 == '(') {
                                b3 = sa[sp + inputSize++] & 0xff;
                                if (b3 == 'B'){
                                    currentState = ASCII;
                                } else if (b3 == 'J'){
                                    currentState = JISX0201_1976;
                                } else if (b3 == 'I'){
                                    currentState = JISX0201_1976_KANA;
                                } else {
                                    return CoderResult.malformedForLength(inputSize);
                                }
                            } else if (b2 == '$'){
                                b3 = sa[sp + inputSize++] & 0xff;
                                if (b3 == '@'){
                                    currentState = JISX0208_1978;
                                } else if (b3 == 'B'){
                                    currentState = JISX0208_1983;
                                } else if (b3 == '(' && dec0212 != null) {
                                    if (sp + inputSize + 1 > sl)
                                        return CoderResult.UNDERFLOW;
                                    b4 = sa[sp + inputSize++] & 0xff;
                                    if (b4 == 'D') {
                                        currentState = JISX0212_1990;
                                    } else {
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else {
                                    return CoderResult.malformedForLength(inputSize);
                                }
                            } else {
                                return CoderResult.malformedForLength(inputSize);
                            }
                        } else if (b1 == SO) {
                            previousState = currentState;
                            currentState = SHIFTOUT;
                        } else if (b1 == SI) {
                            currentState = previousState;
                        }
                        sp += inputSize;
                        continue;
                    }
                    if (dp + 1 > dl)
                        return CoderResult.OVERFLOW;

                    switch (currentState){
                        case ASCII:
                            da[dp++] = (char)(b1 & 0xff);
                            break;
                        case JISX0201_1976:
                          switch (b1) {
                              case 0x5c:  // Yen/tilde substitution
                                da[dp++] = '\u00a5';
                                break;
                              case 0x7e:
                                da[dp++] = '\u203e';
                                break;
                              default:
                                da[dp++] = (char)b1;
                                break;
                            }
                            break;
                        case JISX0208_1978:
                        case JISX0208_1983:
                            if (sp + inputSize + 1 > sl)
                                return CoderResult.UNDERFLOW;
                            b2 = sa[sp + inputSize++] & 0xff;
                            c = dec0208.decodeDouble(b1,b2);
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(inputSize);
                            da[dp++] = c;
                            break;
                        case JISX0212_1990:
                            if (sp + inputSize + 1 > sl)
                                return CoderResult.UNDERFLOW;
                            b2 = sa[sp + inputSize++] & 0xff;
                            c = dec0212.decodeDouble(b1,b2);
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(inputSize);
                            da[dp++] = c;
                            break;
                        case JISX0201_1976_KANA:
                        case SHIFTOUT:
                            if (b1 > 0x5f) {
                                return CoderResult.malformedForLength(inputSize);
                            }
                            da[dp++] = (char)(b1 + 0xff40);
                            break;
                    }
                    sp += inputSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();
            int b1, b2, b3, b4;
            char c;
            int inputSize = 0;
            try {
                while (src.hasRemaining()) {
                    b1 = src.get() & 0xff;
                    inputSize = 1;
                    if ((b1 & 0x80) != 0)
                        return CoderResult.malformedForLength(inputSize);
                    if (b1 == ESC || b1 == SO || b1 == SI) {
                        if (b1 == ESC) {  // ESC
                            if (src.remaining() < 2)
                                return CoderResult.UNDERFLOW;
                            b2 = src.get() & 0xff;
                            inputSize++;
                            if (b2 == '(') {
                                b3 = src.get() & 0xff;
                                inputSize++;
                                if (b3 == 'B'){
                                    currentState = ASCII;
                                } else if (b3 == 'J'){
                                    currentState = JISX0201_1976;
                                } else if (b3 == 'I'){
                                    currentState = JISX0201_1976_KANA;
                                } else {
                                   return CoderResult.malformedForLength(inputSize);
                                }
                            } else if (b2 == '$'){
                                b3 = src.get() & 0xff;
                                inputSize++;
                                if (b3 == '@'){
                                    currentState = JISX0208_1978;
                                } else if (b3 == 'B'){
                                    currentState = JISX0208_1983;
                                } else if (b3 == '(' && dec0212 != null) {
                                    if (!src.hasRemaining())
                                        return CoderResult.UNDERFLOW;
                                    b4 = src.get() & 0xff;
                                    inputSize++;
                                    if (b4 == 'D') {
                                        currentState = JISX0212_1990;
                                    } else {
                                        return CoderResult.malformedForLength(inputSize);
                                    }
                                } else {
                                    return CoderResult.malformedForLength(inputSize);
                                }
                            } else {
                                return CoderResult.malformedForLength(inputSize);
                            }
                        } else if (b1 == SO) {
                            previousState = currentState;
                            currentState = SHIFTOUT;
                        } else if (b1 == SI) { // shift back in
                            currentState = previousState;
                        }
                        mark += inputSize;
                        continue;
                    }
                    if (!dst.hasRemaining())
                        return CoderResult.OVERFLOW;

                    switch (currentState){
                        case ASCII:
                            dst.put((char)(b1 & 0xff));
                            break;
                        case JISX0201_1976:
                            switch (b1) {
                              case 0x5c:  // Yen/tilde substitution
                                dst.put('\u00a5');
                                break;
                              case 0x7e:
                                dst.put('\u203e');
                                break;
                              default:
                                dst.put((char)b1);
                                break;
                            }
                            break;
                        case JISX0208_1978:
                        case JISX0208_1983:
                            if (!src.hasRemaining())
                                return CoderResult.UNDERFLOW;
                            b2 = src.get() & 0xff;
                            inputSize++;
                            c = dec0208.decodeDouble(b1,b2);
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(inputSize);
                            dst.put(c);
                            break;
                        case JISX0212_1990:
                            if (!src.hasRemaining())
                                return CoderResult.UNDERFLOW;
                            b2 = src.get() & 0xff;
                            inputSize++;
                            c = dec0212.decodeDouble(b1,b2);
                            if (c == UNMAPPABLE_DECODING)
                                return CoderResult.unmappableForLength(inputSize);
                            dst.put(c);
                            break;
                        case JISX0201_1976_KANA:
                        case SHIFTOUT:
                            if (b1 > 0x5f) {
                                return CoderResult.malformedForLength(inputSize);
                            }
                            dst.put((char)(b1 + 0xff40));
                            break;
                    }
                    mark += inputSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        // Make some protected methods public for use by JISAutoDetect
        public CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        public CoderResult implFlush(CharBuffer out) {
            return super.implFlush(out);
        }
    }

    static class Encoder extends CharsetEncoder {

        static final DoubleByte.Encoder ENC0208 =
            (DoubleByte.Encoder)new JIS_X_0208().newEncoder();

        private static final byte[] repl = { (byte)0x21, (byte)0x29 };
        private int currentMode = ASCII;
        private int replaceMode = JISX0208_1983;
        private final DoubleByte.Encoder enc0208;
        private final DoubleByte.Encoder enc0212;
        private final boolean doSBKANA;

        private Encoder(Charset cs) {
            this(cs, ENC0208, null, true);
        }

        Encoder(Charset cs,
                DoubleByte.Encoder enc0208,
                DoubleByte.Encoder enc0212,
                boolean doSBKANA) {
            super(cs, 4.0f, (enc0212 != null)? 9.0f : 8.0f, repl);
            this.enc0208 = enc0208;
            this.enc0212 = enc0212;
            this.doSBKANA = doSBKANA;
        }

        protected int encodeSingle(char inputChar) {
            return -1;
        }

        protected void implReset() {
            currentMode = ASCII;
        }

        protected void implReplaceWith(byte[] newReplacement) {
            /* It's almost impossible to decide which charset they belong
               to. The best thing we can do here is to "guess" based on
               the length of newReplacement.
             */
            if (newReplacement.length == 1) {
                replaceMode = ASCII;
            } else if (newReplacement.length == 2) {
                replaceMode = JISX0208_1983;
            }
        }

        protected CoderResult implFlush(ByteBuffer out) {
            if (currentMode != ASCII) {
                if (out.remaining() < 3)
                    return CoderResult.OVERFLOW;
                out.put((byte)0x1b);
                out.put((byte)0x28);
                out.put((byte)0x42);
                currentMode = ASCII;
            }
            return CoderResult.UNDERFLOW;
        }

        public boolean canEncode(char c) {
            return ((c <= '\u007F') ||
                    (c >= 0xFF61 && c <= 0xFF9F) ||
                    (c == '\u00A5') ||
                    (c == '\u203E') ||
                    enc0208.canEncode(c) ||
                    (enc0212!=null && enc0212.canEncode(c)));
        }

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        private CoderResult encodeArrayLoop(CharBuffer src,
                                            ByteBuffer dst)
        {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    char c = sa[sp];
                    if (c <= '\u007F') {
                        if (currentMode != ASCII) {
                            if (dl - dp < 3)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)0x1b;
                            da[dp++] = (byte)0x28;
                            da[dp++] = (byte)0x42;
                            currentMode = ASCII;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)c;
                    } else if (c >= 0xff61 && c <= 0xff9f && doSBKANA) {
                        //a single byte kana
                        if (currentMode != JISX0201_1976_KANA) {
                            if (dl - dp < 3)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)0x1b;
                            da[dp++] = (byte)0x28;
                            da[dp++] = (byte)0x49;
                            currentMode = JISX0201_1976_KANA;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)(c - 0xff40);
                    } else if (c == '\u00A5' || c == '\u203E') {
                        //backslash or tilde
                        if (currentMode != JISX0201_1976) {
                            if (dl - dp < 3)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)0x1b;
                            da[dp++] = (byte)0x28;
                            da[dp++] = (byte)0x4a;
                            currentMode = JISX0201_1976;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (c == '\u00A5')?(byte)0x5C:(byte)0x7e;
                    } else {
                        int index = enc0208.encodeChar(c);
                        if (index != UNMAPPABLE_ENCODING) {
                            if (currentMode != JISX0208_1983) {
                                if (dl - dp < 3)
                                    return CoderResult.OVERFLOW;
                                da[dp++] = (byte)0x1b;
                                da[dp++] = (byte)0x24;
                                da[dp++] = (byte)0x42;
                                currentMode = JISX0208_1983;
                            }
                            if (dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)(index >> 8);
                            da[dp++] = (byte)(index & 0xff);
                        } else if (enc0212 != null &&
                                   (index = enc0212.encodeChar(c)) != UNMAPPABLE_ENCODING) {
                            if (currentMode != JISX0212_1990) {
                                if (dl - dp < 4)
                                    return CoderResult.OVERFLOW;
                                da[dp++] = (byte)0x1b;
                                da[dp++] = (byte)0x24;
                                da[dp++] = (byte)0x28;
                                da[dp++] = (byte)0x44;
                                currentMode = JISX0212_1990;
                            }
                            if (dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = (byte)(index >> 8);
                            da[dp++] = (byte)(index & 0xff);
                        } else {
                            if (Character.isSurrogate(c) && sgp.parse(c, sa, sp, sl) < 0)
                                return sgp.error();
                            if (unmappableCharacterAction()
                                == CodingErrorAction.REPLACE
                                && currentMode != replaceMode) {
                                if (dl - dp < 3)
                                    return CoderResult.OVERFLOW;
                                if (replaceMode == ASCII) {
                                    da[dp++] = (byte)0x1b;
                                    da[dp++] = (byte)0x28;
                                    da[dp++] = (byte)0x42;
                                } else {
                                    da[dp++] = (byte)0x1b;
                                    da[dp++] = (byte)0x24;
                                    da[dp++] = (byte)0x42;
                                }
                                currentMode = replaceMode;
                            }
                            if (Character.isSurrogate(c))
                                return sgp.unmappableResult();
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    char c = src.get();

                    if (c <= '\u007F') {
                        if (currentMode != ASCII) {
                            if (dst.remaining() < 3)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)0x1b);
                            dst.put((byte)0x28);
                            dst.put((byte)0x42);
                            currentMode = ASCII;
                        }
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)c);
                    } else if (c >= 0xff61 && c <= 0xff9f && doSBKANA) {
                        //Is it a single byte kana?
                        if (currentMode != JISX0201_1976_KANA) {
                            if (dst.remaining() < 3)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)0x1b);
                            dst.put((byte)0x28);
                            dst.put((byte)0x49);
                            currentMode = JISX0201_1976_KANA;
                        }
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)(c - 0xff40));
                    } else if (c == '\u00a5' || c == '\u203E') {
                        if (currentMode != JISX0201_1976) {
                            if (dst.remaining() < 3)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)0x1b);
                            dst.put((byte)0x28);
                            dst.put((byte)0x4a);
                            currentMode = JISX0201_1976;
                        }
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((c == '\u00A5')?(byte)0x5C:(byte)0x7e);
                    } else {
                        int index = enc0208.encodeChar(c);
                        if (index != UNMAPPABLE_ENCODING) {
                            if (currentMode != JISX0208_1983) {
                                if (dst.remaining() < 3)
                                    return CoderResult.OVERFLOW;
                                dst.put((byte)0x1b);
                                dst.put((byte)0x24);
                                dst.put((byte)0x42);
                                currentMode = JISX0208_1983;
                            }
                            if (dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)(index >> 8));
                            dst.put((byte)(index & 0xff));
                        } else if (enc0212 != null &&
                                   (index = enc0212.encodeChar(c)) != UNMAPPABLE_ENCODING) {
                            if (currentMode != JISX0212_1990) {
                                if (dst.remaining() < 4)
                                    return CoderResult.OVERFLOW;
                                dst.put((byte)0x1b);
                                dst.put((byte)0x24);
                                dst.put((byte)0x28);
                                dst.put((byte)0x44);
                                currentMode = JISX0212_1990;
                            }
                            if (dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put((byte)(index >> 8));
                            dst.put((byte)(index & 0xff));
                        } else {
                            if (Character.isSurrogate(c) && sgp.parse(c, src) < 0)
                                return sgp.error();
                            if (unmappableCharacterAction() == CodingErrorAction.REPLACE
                                && currentMode != replaceMode) {
                                if (dst.remaining() < 3)
                                    return CoderResult.OVERFLOW;
                                if (replaceMode == ASCII) {
                                    dst.put((byte)0x1b);
                                    dst.put((byte)0x28);
                                    dst.put((byte)0x42);
                                } else {
                                    dst.put((byte)0x1b);
                                    dst.put((byte)0x24);
                                    dst.put((byte)0x42);
                                }
                                currentMode = replaceMode;
                            }
                            if (Character.isSurrogate(c))
                                return sgp.unmappableResult();
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
              } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src,
                                         ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
