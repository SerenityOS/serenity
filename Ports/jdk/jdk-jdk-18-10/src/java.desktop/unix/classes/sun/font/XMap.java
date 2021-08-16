/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Locale;
import java.nio.charset.*;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

class XMap {

    private static HashMap<String, XMap> xMappers = new HashMap<>();

    /* ConvertedGlyphs has unicode code points as indexes and values
     * are platform-encoded multi-bytes chars packed into java chars.
     * These platform-encoded characters are equated to glyph ids, although
     * that's not strictly true, as X11 only supports using chars.
     * The assumption carried over from the native implementation that
     * a char is big enough to hold an X11 glyph id (ie platform char).
     */
    char[] convertedGlyphs;

    static synchronized XMap getXMapper(String encoding) {
        XMap mapper = xMappers.get(encoding);
        if (mapper == null) {
            mapper = getXMapperInternal(encoding);
            xMappers.put(encoding, mapper);
        }
        return mapper;
    }

    static final int SINGLE_BYTE = 1;
    static final int DOUBLE_BYTE = 2;

    private static XMap getXMapperInternal(String encoding) {

        String jclass = null;
        int nBytes = SINGLE_BYTE;
        int maxU = 0xffff;
        int minU = 0;
        boolean addAscii = false;
        boolean lowPartOnly = false;
        if (encoding.equals("dingbats")) {
            jclass = "sun.font.X11Dingbats";
            minU = 0x2701;
            maxU = 0x27be;
        } else if (encoding.equals("symbol")){
            jclass = "sun.awt.Symbol";
            minU = 0x0391;
            maxU = 0x22ef;
        } else if (encoding.equals("iso8859-1")) {
            maxU = 0xff;
        } else if (encoding.equals("iso8859-2")) {
            jclass = "ISO8859_2";
        } else if (encoding.equals("jisx0208.1983-0")) {
            jclass = "JIS0208";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals("jisx0201.1976-0")) {
            jclass = "JIS0201";
            // this is mapping the latin supplement range 128->255 which
            // doesn't exist in JIS0201. This needs examination.
            // it was also overwriting a couple of the mappings of
            // 7E and A5 which in JIS201 are different chars than in
            // Latin 1. I have revised AddAscii to not overwrite chars
            // which are already converted.
            addAscii = true;
            lowPartOnly = true;
        } else if (encoding.equals("jisx0212.1990-0")) {
            jclass = "JIS0212";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals("iso8859-4")) {
            jclass = "ISO8859_4";
        } else if (encoding.equals("iso8859-5")) {
            jclass = "ISO8859_5";
        } else if (encoding.equals("koi8-r")) {
            jclass = "KOI8_R";
        } else if (encoding.equals("ansi-1251")) {
            jclass = "windows-1251";
        } else if (encoding.equals("iso8859-6")) {
            jclass = "ISO8859_6";
        } else if (encoding.equals("iso8859-7")) {
            jclass = "ISO8859_7";
        } else if (encoding.equals("iso8859-8")) {
            jclass = "ISO8859_8";
        } else if (encoding.equals("iso8859-9")) {
            jclass = "ISO8859_9";
        } else if (encoding.equals("iso8859-13")) {
            jclass = "ISO8859_13";
        } else if (encoding.equals("iso8859-15")) {
            jclass = "ISO8859_15";
        } else if (encoding.equals("ksc5601.1987-0")) {
            jclass ="sun.font.X11KSC5601";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals( "ksc5601.1992-3")) {
            jclass ="sun.font.X11Johab";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals( "ksc5601.1987-1")) {
            jclass ="EUC_KR";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals( "cns11643-1")) {
            jclass = "sun.font.X11CNS11643P1";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals("cns11643-2")) {
            jclass = "sun.font.X11CNS11643P2";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals("cns11643-3")) {
            jclass = "sun.font.X11CNS11643P3";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.equals("gb2312.1980-0")) {
            jclass = "sun.font.X11GB2312";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.indexOf("big5") >= 0) {
            jclass = "Big5";
            nBytes = DOUBLE_BYTE;
            addAscii = true;
        } else if (encoding.equals("tis620.2533-0")) {
            jclass = "TIS620";
        } else if (encoding.equals("gbk-0")) {
            jclass = "sun.font.X11GBK";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.indexOf("sun.unicode-0") >= 0) {
            jclass = "sun.font.X11SunUnicode_0";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.indexOf("gb18030.2000-1") >= 0) {
            jclass = "sun.font.X11GB18030_1";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.indexOf( "gb18030.2000-0") >= 0) {
            jclass = "sun.font.X11GB18030_0";
            nBytes = DOUBLE_BYTE;
        } else if (encoding.indexOf("hkscs") >= 0) {
            jclass = "MS950_HKSCS_XP";
            nBytes = DOUBLE_BYTE;
        }
        return new XMap(jclass, minU, maxU, nBytes, addAscii, lowPartOnly);
    }

    private static final char SURR_MIN = '\uD800';
    private static final char SURR_MAX = '\uDFFF';

    private XMap(String className, int minU, int maxU, int nBytes,
                 boolean addAscii, boolean lowPartOnly) {

        CharsetEncoder enc = null;
        if (className != null) {
            try {
                if (className.startsWith("sun.awt")) {
                    enc = ((Charset)Class.forName(className).getDeclaredConstructor().
                                                  newInstance()).newEncoder();
                } else {
                    enc = Charset.forName(className).newEncoder();
                }
            } catch (Exception x) {x.printStackTrace();}
        }
        if (enc == null) {
            convertedGlyphs = new char[256];
            for (int i=0; i<256; i++) {
                convertedGlyphs[i] = (char)i;
            }
            return;
        } else {
            /* chars is set to the unicode values to convert,
             * bytes is where the X11 character codes will be output.
             * Finally we pack the byte pairs into chars.
             */
            int count = maxU - minU + 1;
            byte[] bytes = new byte[count*nBytes];
            char[] chars  = new char[count];
            for (int i=0; i<count; i++) {
                chars[i] = (char)(minU+i);
            }
            int startCharIndex = 0;
            /* For multi-byte encodings, single byte chars should be skipped */
            if (nBytes > SINGLE_BYTE && minU < 256) {
                startCharIndex = 256-minU;
            }
            byte[] rbytes = new byte[nBytes];
            try {
                int cbLen = 0;
                int bbLen = 0;
                // Since we don't support surrogates in any X11 encoding, skip
                // the surrogate area, otherwise the sequence of "Oxdbff0xdc00"
                // will accidently cause the surrogate-aware nio charset to treat
                // them as a legal pair and then undesirablly skip 2 "chars"
                // for one "unmappable character"
                if (startCharIndex < SURR_MIN && startCharIndex + count >SURR_MAX) {
                    cbLen = SURR_MIN - startCharIndex;
                    bbLen = cbLen * nBytes;
                    enc.onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(rbytes)
                        .encode(CharBuffer.wrap(chars, startCharIndex, cbLen),
                                ByteBuffer.wrap(bytes, startCharIndex * nBytes, bbLen),
                                true);
                    startCharIndex = SURR_MAX + 1;
                }
                cbLen = count - startCharIndex;
                bbLen = cbLen * nBytes;
                enc.onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .replaceWith(rbytes)
                    .encode(CharBuffer.wrap(chars, startCharIndex, cbLen),
                            ByteBuffer.wrap(bytes, startCharIndex * nBytes, bbLen),
                            true);
            } catch (Exception e) { e.printStackTrace();}

            convertedGlyphs = new char[65536];
            for (int i=0; i<count; i++) {
                if (nBytes == 1) {
                    convertedGlyphs[i+minU] = (char)(bytes[i]&0xff);
                } else {
                    convertedGlyphs[i+minU] =
                        (char)(((bytes[i*2]&0xff) << 8) + (bytes[i*2+1]&0xff));
                }
            }
        }

        int max = (lowPartOnly) ? 128 : 256;
        if (addAscii && convertedGlyphs.length >= 256) {
            for (int i=0;i<max;i++) {
                if (convertedGlyphs[i] == 0) {
                    convertedGlyphs[i] = (char)i;
                }
            }
        }
    }
}
