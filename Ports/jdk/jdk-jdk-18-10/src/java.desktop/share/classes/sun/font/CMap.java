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

package sun.font;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.nio.charset.*;

/*
 * A tt font has a CMAP table which is in turn made up of sub-tables which
 * describe the char to glyph mapping in (possibly) multiple ways.
 * CMAP subtables are described by 3 values.
 * 1. Platform ID (eg 3=Microsoft, which is the id we look for in JDK)
 * 2. Encoding (eg 0=symbol, 1=unicode)
 * 3. TrueType subtable format (how the char->glyph mapping for the encoding
 * is stored in the subtable). See the TrueType spec. Format 4 is required
 * by MS in fonts for windows. Its uses segmented mapping to delta values.
 * Most typically we see are (3,1,4) :
 * CMAP Platform ID=3 is what we use.
 * Encodings that are used in practice by JDK on Solaris are
 *  symbol (3,0)
 *  unicode (3,1)
 *  GBK (3,5) (note that solaris zh fonts report 3,4 but are really 3,5)
 * The format for almost all subtables is 4. However the solaris (3,5)
 * encodings are typically in format 2.
 */
abstract class CMap {

//     static char WingDings_b2c[] = {
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0x2702, 0x2701, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0x2706, 0x2709, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x2707, 0x270d,
//         0xfffd, 0x270c, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0x2708, 0xfffd, 0xfffd, 0x2744, 0xfffd, 0x271e, 0xfffd,
//         0x2720, 0x2721, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0x2751, 0x2752, 0xfffd, 0xfffd, 0x2756, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0x2740, 0x273f, 0x275d, 0x275e, 0xfffd,
//         0xfffd, 0x2780, 0x2781, 0x2782, 0x2783, 0x2784, 0x2785, 0x2786,
//         0x2787, 0x2788, 0x2789, 0xfffd, 0x278a, 0x278b, 0x278c, 0x278d,
//         0x278e, 0x278f, 0x2790, 0x2791, 0x2792, 0x2793, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x274d, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x2736, 0x2734, 0xfffd, 0x2735,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x272a, 0x2730, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x27a5, 0xfffd, 0x27a6, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x27a2, 0xfffd, 0xfffd, 0xfffd, 0x27b3, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x27a1, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x27a9, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0x2717, 0x2713, 0xfffd, 0xfffd, 0xfffd,
//    };

//     static char Symbols_b2c[] = {
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0x2200, 0xfffd, 0x2203, 0xfffd, 0xfffd, 0x220d,
//         0xfffd, 0xfffd, 0x2217, 0xfffd, 0xfffd, 0x2212, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x2245, 0x0391, 0x0392, 0x03a7, 0x0394, 0x0395, 0x03a6, 0x0393,
//         0x0397, 0x0399, 0x03d1, 0x039a, 0x039b, 0x039c, 0x039d, 0x039f,
//         0x03a0, 0x0398, 0x03a1, 0x03a3, 0x03a4, 0x03a5, 0x03c2, 0x03a9,
//         0x039e, 0x03a8, 0x0396, 0xfffd, 0x2234, 0xfffd, 0x22a5, 0xfffd,
//         0xfffd, 0x03b1, 0x03b2, 0x03c7, 0x03b4, 0x03b5, 0x03c6, 0x03b3,
//         0x03b7, 0x03b9, 0x03d5, 0x03ba, 0x03bb, 0x03bc, 0x03bd, 0x03bf,
//         0x03c0, 0x03b8, 0x03c1, 0x03c3, 0x03c4, 0x03c5, 0x03d6, 0x03c9,
//         0x03be, 0x03c8, 0x03b6, 0xfffd, 0xfffd, 0xfffd, 0x223c, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0x03d2, 0xfffd, 0x2264, 0x2215, 0x221e, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x2218, 0xfffd, 0xfffd, 0x2265, 0xfffd, 0x221d, 0xfffd, 0x2219,
//         0xfffd, 0x2260, 0x2261, 0x2248, 0x22ef, 0x2223, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x2297, 0x2295, 0x2205, 0x2229,
//         0x222a, 0x2283, 0x2287, 0x2284, 0x2282, 0x2286, 0x2208, 0x2209,
//         0xfffd, 0x2207, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x221a, 0x22c5,
//         0xfffd, 0x2227, 0x2228, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0x22c4, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0x2211, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0x222b, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//         0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd, 0xfffd,
//     };

    static final short ShiftJISEncoding = 2;
    static final short GBKEncoding      = 3;
    static final short Big5Encoding     = 4;
    static final short WansungEncoding  = 5;
    static final short JohabEncoding    = 6;
    static final short MSUnicodeSurrogateEncoding = 10;

    static final char noSuchChar = (char)0xfffd;
    static final int SHORTMASK = 0x0000ffff;
    static final int INTMASK   = 0x7fffffff;

    static final char[][] converterMaps = new char[7][];

    /*
     * Unicode->other encoding translation array. A pre-computed look up
     * which can be shared across all fonts using that encoding.
     * Using this saves running character coverters repeatedly.
     */
    char[] xlat;
    UVS uvs = null;

    static CMap initialize(TrueTypeFont font) {

        CMap cmap = null;

        int offset, platformID, encodingID=-1;

        int three0=0, three1=0, three2=0, three3=0, three4=0, three5=0,
            three6=0, three10=0;
        int zero5 = 0; // for Unicode Variation Sequences
        boolean threeStar = false;

        ByteBuffer cmapBuffer = font.getTableBuffer(TrueTypeFont.cmapTag);
        int cmapTableOffset = font.getTableSize(TrueTypeFont.cmapTag);
        short numberSubTables = cmapBuffer.getShort(2);

        /* locate the offsets of all 3,*  (ie Microsoft platform) encodings */
        for (int i=0; i<numberSubTables; i++) {
            cmapBuffer.position(i * 8 + 4);
            platformID = cmapBuffer.getShort();
            if (platformID == 3) {
                threeStar = true;
                encodingID = cmapBuffer.getShort();
                offset     = cmapBuffer.getInt();
                switch (encodingID) {
                case 0:  three0  = offset; break; // MS Symbol encoding
                case 1:  three1  = offset; break; // MS Unicode cmap
                case 2:  three2  = offset; break; // ShiftJIS cmap.
                case 3:  three3  = offset; break; // GBK cmap
                case 4:  three4  = offset; break; // Big 5 cmap
                case 5:  three5  = offset; break; // Wansung
                case 6:  three6  = offset; break; // Johab
                case 10: three10 = offset; break; // MS Unicode surrogates
                }
            } else if (platformID == 0) {
                encodingID = cmapBuffer.getShort();
                offset     = cmapBuffer.getInt();
                if (encodingID == 5) {
                    zero5 = offset;
                }
            }
        }

        /* This defines the preference order for cmap subtables */
        if (threeStar) {
            if (three10 != 0) {
                cmap = createCMap(cmapBuffer, three10, null);
            }
            else if  (three0 != 0) {
                /* The special case treatment of these fonts leads to
                 * anomalies where a user can view "wingdings" and "wingdings2"
                 * and the latter shows all its code points in the unicode
                 * private use area at 0xF000->0XF0FF and the former shows
                 * a scattered subset of its glyphs that are known mappings to
                 * unicode code points.
                 * The primary purpose of these mappings was to facilitate
                 * display of symbol chars etc in composite fonts, however
                 * this is not needed as all these code points are covered
                 * by some other platform symbol font.
                 * Commenting this out reduces the role of these two files
                 * (assuming that they continue to be used in font.properties)
                 * to just one of contributing to the overall composite
                 * font metrics, and also AWT can still access the fonts.
                 * Clients which explicitly accessed these fonts as names
                 * "Symbol" and "Wingdings" (ie as physical fonts) and
                 * expected to see a scattering of these characters will
                 * see them now as missing. How much of a problem is this?
                 * Perhaps we could still support this mapping just for
                 * "Symbol.ttf" but I suspect some users would prefer it
                 * to be mapped in to the Latin range as that is how
                 * the "symbol" font is used in native apps.
                 */
//              String name = font.platName.toLowerCase(Locale.ENGLISH);
//              if (name.endsWith("symbol.ttf")) {
//                  cmap = createSymbolCMap(cmapBuffer, three0, Symbols_b2c);
//              } else if (name.endsWith("wingding.ttf")) {
//                  cmap = createSymbolCMap(cmapBuffer, three0, WingDings_b2c);
//              } else {
                    cmap = createCMap(cmapBuffer, three0, null);
//              }
            }
            else if (three1 != 0) {
                cmap = createCMap(cmapBuffer, three1, null);
            }
            else if (three2 != 0) {
                cmap = createCMap(cmapBuffer, three2,
                                  getConverterMap(ShiftJISEncoding));
            }
            else if (three3 != 0) {
                cmap = createCMap(cmapBuffer, three3,
                                  getConverterMap(GBKEncoding));
            }
            else if (three4 != 0) {
                cmap = createCMap(cmapBuffer, three4,
                                  getConverterMap(Big5Encoding));
            }
            else if (three5 != 0) {
                cmap = createCMap(cmapBuffer, three5,
                                  getConverterMap(WansungEncoding));
            }
            else if (three6 != 0) {
                cmap = createCMap(cmapBuffer, three6,
                                  getConverterMap(JohabEncoding));
            }
        } else {
            /* No 3,* subtable was found. Just use whatever is the first
             * table listed. Not very useful but maybe better than
             * rejecting the font entirely?
             */
            cmap = createCMap(cmapBuffer, cmapBuffer.getInt(8), null);
        }
        // For Unicode Variation Sequences
        if (cmap != null && zero5 != 0) {
            cmap.createUVS(cmapBuffer, zero5);
        }
        return cmap;
    }

    /* speed up the converting by setting the range for double
     * byte characters;
     */
    static char[] getConverter(short encodingID) {
        int dBegin = 0x8000;
        int dEnd   = 0xffff;
        String encoding;

        switch (encodingID) {
        case ShiftJISEncoding:
            dBegin = 0x8140;
            dEnd   = 0xfcfc;
            encoding = "SJIS";
            break;
        case GBKEncoding:
            dBegin = 0x8140;
            dEnd   = 0xfea0;
            encoding = "GBK";
            break;
        case Big5Encoding:
            dBegin = 0xa140;
            dEnd   = 0xfefe;
            encoding = "Big5";
            break;
        case WansungEncoding:
            dBegin = 0xa1a1;
            dEnd   = 0xfede;
            encoding = "EUC_KR";
            break;
        case JohabEncoding:
            dBegin = 0x8141;
            dEnd   = 0xfdfe;
            encoding = "Johab";
            break;
        default:
            return null;
        }

        try {
            char[] convertedChars = new char[65536];
            for (int i=0; i<65536; i++) {
                convertedChars[i] = noSuchChar;
            }

            byte[] inputBytes = new byte[(dEnd-dBegin+1)*2];
            char[] outputChars = new char[(dEnd-dBegin+1)];

            int j = 0;
            int firstByte;
            if (encodingID == ShiftJISEncoding) {
                for (int i = dBegin; i <= dEnd; i++) {
                    firstByte = (i >> 8 & 0xff);
                    if (firstByte >= 0xa1 && firstByte <= 0xdf) {
                        //sjis halfwidth katakana
                        inputBytes[j++] = (byte)0xff;
                        inputBytes[j++] = (byte)0xff;
                    } else {
                        inputBytes[j++] = (byte)firstByte;
                        inputBytes[j++] = (byte)(i & 0xff);
                    }
                }
            } else {
                for (int i = dBegin; i <= dEnd; i++) {
                    inputBytes[j++] = (byte)(i>>8 & 0xff);
                    inputBytes[j++] = (byte)(i & 0xff);
                }
            }

            Charset.forName(encoding).newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith("\u0000")
            .decode(ByteBuffer.wrap(inputBytes, 0, inputBytes.length),
                    CharBuffer.wrap(outputChars, 0, outputChars.length),
                    true);

            // ensure single byte ascii
            for (int i = 0x20; i <= 0x7e; i++) {
                convertedChars[i] = (char)i;
            }

            //sjis halfwidth katakana
            if (encodingID == ShiftJISEncoding) {
                for (int i = 0xa1; i <= 0xdf; i++) {
                    convertedChars[i] = (char)(i - 0xa1 + 0xff61);
                }
            }

            /* It would save heap space (approx 60Kbytes for each of these
             * converters) if stored only valid ranges (ie returned
             * outputChars directly. But this is tricky since want to
             * include the ASCII range too.
             */
//          System.err.println("oc.len="+outputChars.length);
//          System.err.println("cc.len="+convertedChars.length);
//          System.err.println("dbegin="+dBegin);
            System.arraycopy(outputChars, 0, convertedChars, dBegin,
                             outputChars.length);

            //return convertedChars;
            /* invert this map as now want it to map from Unicode
             * to other encoding.
             */
            char [] invertedChars = new char[65536];
            for (int i=0;i<65536;i++) {
                if (convertedChars[i] != noSuchChar) {
                    invertedChars[convertedChars[i]] = (char)i;
                }
            }
            return invertedChars;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * The returned array maps to unicode from some other 2 byte encoding
     * eg for a 2byte index which represents a SJIS char, the indexed
     * value is the corresponding unicode char.
     */
    static char[] getConverterMap(short encodingID) {
        if (converterMaps[encodingID] == null) {
           converterMaps[encodingID] = getConverter(encodingID);
        }
        return converterMaps[encodingID];
    }


    static CMap createCMap(ByteBuffer buffer, int offset, char[] xlat) {
        /* First do a sanity check that this cmap subtable is contained
         * within the cmap table.
         */
        int subtableFormat = buffer.getChar(offset);
        long subtableLength;
        if (subtableFormat < 8) {
            subtableLength = buffer.getChar(offset+2);
        } else {
            subtableLength = buffer.getInt(offset+4) & INTMASK;
        }
        if (FontUtilities.isLogging() && offset + subtableLength > buffer.capacity()) {
            FontUtilities.logWarning("Cmap subtable overflows buffer.");
        }
        switch (subtableFormat) {
        case 0:  return new CMapFormat0(buffer, offset);
        case 2:  return new CMapFormat2(buffer, offset, xlat);
        case 4:  return new CMapFormat4(buffer, offset, xlat);
        case 6:  return new CMapFormat6(buffer, offset, xlat);
        case 8:  return new CMapFormat8(buffer, offset, xlat);
        case 10: return new CMapFormat10(buffer, offset, xlat);
        case 12: return new CMapFormat12(buffer, offset, xlat);
        default: throw new RuntimeException("Cmap format unimplemented: " +
                                            (int)buffer.getChar(offset));
        }
    }

    private void createUVS(ByteBuffer buffer, int offset) {
        int subtableFormat = buffer.getChar(offset);
        if (subtableFormat == 14) {
            long subtableLength = buffer.getInt(offset + 2) & INTMASK;
            if (FontUtilities.isLogging() && offset + subtableLength > buffer.capacity()) {
                FontUtilities.logWarning("Cmap UVS subtable overflows buffer.");
            }
            try {
                this.uvs = new UVS(buffer, offset);
            } catch (Throwable t) {
            }
        }
        return;
    }

/*
    final char charVal(byte[] cmap, int index) {
        return (char)(((0xff & cmap[index]) << 8)+(0xff & cmap[index+1]));
    }

    final short shortVal(byte[] cmap, int index) {
        return (short)(((0xff & cmap[index]) << 8)+(0xff & cmap[index+1]));
    }
*/
    abstract char getGlyph(int charCode);

    /* Format 4 Header is
     * ushort format (off=0)
     * ushort length (off=2)
     * ushort language (off=4)
     * ushort segCountX2 (off=6)
     * ushort searchRange (off=8)
     * ushort entrySelector (off=10)
     * ushort rangeShift (off=12)
     * ushort endCount[segCount] (off=14)
     * ushort reservedPad
     * ushort startCount[segCount]
     * short idDelta[segCount]
     * idRangeOFfset[segCount]
     * ushort glyphIdArray[]
     */
    static class CMapFormat4 extends CMap {
        int segCount;
        int entrySelector;
        int rangeShift;
        char[] endCount;
        char[] startCount;
        short[] idDelta;
        char[] idRangeOffset;
        char[] glyphIds;

        CMapFormat4(ByteBuffer bbuffer, int offset, char[] xlat) {

            this.xlat = xlat;

            bbuffer.position(offset);
            CharBuffer buffer = bbuffer.asCharBuffer();
            buffer.get(); // skip, we already know format=4
            int subtableLength = buffer.get();
            /* Try to recover from some bad fonts which specify a subtable
             * length that would overflow the byte buffer holding the whole
             * cmap table. If this isn't a recoverable situation an exception
             * may be thrown which is caught higher up the call stack.
             * Whilst this may seem lenient, in practice, unless the "bad"
             * subtable we are using is the last one in the cmap table we
             * would have no way of knowing about this problem anyway.
             */
            if (offset+subtableLength > bbuffer.capacity()) {
                subtableLength = bbuffer.capacity() - offset;
            }
            buffer.get(); // skip language
            segCount = buffer.get()/2;
            int searchRange = buffer.get();
            entrySelector = buffer.get();
            rangeShift    = buffer.get()/2;
            startCount = new char[segCount];
            endCount = new char[segCount];
            idDelta = new short[segCount];
            idRangeOffset = new char[segCount];

            for (int i=0; i<segCount; i++) {
                endCount[i] = buffer.get();
            }
            buffer.get(); // 2 bytes for reserved pad
            for (int i=0; i<segCount; i++) {
                startCount[i] = buffer.get();
            }

            for (int i=0; i<segCount; i++) {
                idDelta[i] = (short)buffer.get();
            }

            for (int i=0; i<segCount; i++) {
                char ctmp = buffer.get();
                idRangeOffset[i] = (char)((ctmp>>1)&0xffff);
            }
            /* Can calculate the number of glyph IDs by subtracting
             * "pos" from the length of the cmap
             */
            int pos = (segCount*8+16)/2;
            buffer.position(pos);
            int numGlyphIds = (subtableLength/2 - pos);
            glyphIds = new char[numGlyphIds];
            for (int i=0;i<numGlyphIds;i++) {
                glyphIds[i] = buffer.get();
            }
/*
            System.err.println("segcount="+segCount);
            System.err.println("entrySelector="+entrySelector);
            System.err.println("rangeShift="+rangeShift);
            for (int j=0;j<segCount;j++) {
              System.err.println("j="+j+ " sc="+(int)(startCount[j]&0xffff)+
                                 " ec="+(int)(endCount[j]&0xffff)+
                                 " delta="+idDelta[j] +
                                 " ro="+(int)idRangeOffset[j]);
            }

            //System.err.println("numglyphs="+glyphIds.length);
            for (int i=0;i<numGlyphIds;i++) {
                  System.err.println("gid["+i+"]="+(int)glyphIds[i]);
            }
*/
        }

        char getGlyph(int charCode) {

            final int origCharCode = charCode;
            int index = 0;
            char glyphCode = 0;

            int controlGlyph = getControlCodeGlyph(charCode, true);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }

            /* presence of translation array indicates that this
             * cmap is in some other (non-unicode encoding).
             * In order to look-up a char->glyph mapping we need to
             * translate the unicode code point to the encoding of
             * the cmap.
             * REMIND: VALID CHARCODES??
             */
            if (xlat != null) {
                charCode = xlat[charCode];
            }

            /*
             * Citation from the TrueType (and OpenType) spec:
             *   The segments are sorted in order of increasing endCode
             *   values, and the segment values are specified in four parallel
             *   arrays. You search for the first endCode that is greater than
             *   or equal to the character code you want to map. If the
             *   corresponding startCode is less than or equal to the
             *   character code, then you use the corresponding idDelta and
             *   idRangeOffset to map the character code to a glyph index
             *   (otherwise, the missingGlyph is returned).
             */

            /*
             * CMAP format4 defines several fields for optimized search of
             * the segment list (entrySelector, searchRange, rangeShift).
             * However, benefits are neglible and some fonts have incorrect
             * data - so we use straightforward binary search (see bug 6247425)
             */
            int left = 0, right = startCount.length;
            index = startCount.length >> 1;
            while (left < right) {
                if (endCount[index] < charCode) {
                    left = index + 1;
                } else {
                    right = index;
                }
                index = (left + right) >> 1;
            }

            if (charCode >= startCount[index] && charCode <= endCount[index]) {
                int rangeOffset = idRangeOffset[index];

                if (rangeOffset == 0) {
                    glyphCode = (char)(charCode + idDelta[index]);
                } else {
                    /* Calculate an index into the glyphIds array */

/*
                    System.err.println("rangeoffset="+rangeOffset+
                                       " charCode=" + charCode +
                                       " scnt["+index+"]="+(int)startCount[index] +
                                       " segCnt="+segCount);
*/

                    int glyphIDIndex = rangeOffset - segCount + index
                                         + (charCode - startCount[index]);
                    glyphCode = glyphIds[glyphIDIndex];
                    if (glyphCode != 0) {
                        glyphCode = (char)(glyphCode + idDelta[index]);
                    }
                }
            }
            if (glyphCode == 0) {
              glyphCode = getFormatCharGlyph(origCharCode);
            }
            return glyphCode;
        }
    }

    // Format 0: Byte Encoding table
    static class CMapFormat0 extends CMap {
        byte [] cmap;

        CMapFormat0(ByteBuffer buffer, int offset) {

            /* skip 6 bytes of format, length, and version */
            int len = buffer.getChar(offset+2);
            cmap = new byte[len-6];
            buffer.position(offset+6);
            buffer.get(cmap);
        }

        char getGlyph(int charCode) {
            if (charCode < 256) {
                if (charCode < 0x0010) {
                    switch (charCode) {
                    case 0x0009:
                    case 0x000a:
                    case 0x000d: return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
                    }
                }
                return (char)(0xff & cmap[charCode]);
            } else {
                return 0;
            }
        }
    }

//     static CMap createSymbolCMap(ByteBuffer buffer, int offset, char[] syms) {

//      CMap cmap = createCMap(buffer, offset, null);
//      if (cmap == null) {
//          return null;
//      } else {
//          return new CMapFormatSymbol(cmap, syms);
//      }
//     }

//     static class CMapFormatSymbol extends CMap {

//      CMap cmap;
//      static final int NUM_BUCKETS = 128;
//      Bucket[] buckets = new Bucket[NUM_BUCKETS];

//      class Bucket {
//          char unicode;
//          char glyph;
//          Bucket next;

//          Bucket(char u, char g) {
//              unicode = u;
//              glyph = g;
//          }
//      }

//      CMapFormatSymbol(CMap cmap, char[] syms) {

//          this.cmap = cmap;

//          for (int i=0;i<syms.length;i++) {
//              char unicode = syms[i];
//              if (unicode != noSuchChar) {
//                  char glyph = cmap.getGlyph(i + 0xf000);
//                  int hash = unicode % NUM_BUCKETS;
//                  Bucket bucket = new Bucket(unicode, glyph);
//                  if (buckets[hash] == null) {
//                      buckets[hash] = bucket;
//                  } else {
//                      Bucket b = buckets[hash];
//                      while (b.next != null) {
//                          b = b.next;
//                      }
//                      b.next = bucket;
//                  }
//              }
//          }
//      }

//      char getGlyph(int unicode) {
//          if (unicode >= 0x1000) {
//              return 0;
//          }
//          else if (unicode >=0xf000 && unicode < 0xf100) {
//              return cmap.getGlyph(unicode);
//          } else {
//              Bucket b = buckets[unicode % NUM_BUCKETS];
//              while (b != null) {
//                  if (b.unicode == unicode) {
//                      return b.glyph;
//                  } else {
//                      b = b.next;
//                  }
//              }
//              return 0;
//          }
//      }
//     }

    // Format 2: High-byte mapping through table
    static class CMapFormat2 extends CMap {

        char[] subHeaderKey = new char[256];
         /* Store subheaders in individual arrays
          * A SubHeader entry theortically looks like {
          *   char firstCode;
          *   char entryCount;
          *   short idDelta;
          *   char idRangeOffset;
          * }
          */
        char[] firstCodeArray;
        char[] entryCountArray;
        short[] idDeltaArray;
        char[] idRangeOffSetArray;

        char[] glyphIndexArray;

        CMapFormat2(ByteBuffer buffer, int offset, char[] xlat) {

            this.xlat = xlat;

            int tableLen = buffer.getChar(offset+2);
            buffer.position(offset+6);
            CharBuffer cBuffer = buffer.asCharBuffer();
            char maxSubHeader = 0;
            for (int i=0;i<256;i++) {
                subHeaderKey[i] = cBuffer.get();
                if (subHeaderKey[i] > maxSubHeader) {
                    maxSubHeader = subHeaderKey[i];
                }
            }
            /* The value of the subHeaderKey is 8 * the subHeader index,
             * so the number of subHeaders can be obtained by dividing
             * this value bv 8 and adding 1.
             */
            int numSubHeaders = (maxSubHeader >> 3) +1;
            firstCodeArray = new char[numSubHeaders];
            entryCountArray = new char[numSubHeaders];
            idDeltaArray  = new short[numSubHeaders];
            idRangeOffSetArray  = new char[numSubHeaders];
            for (int i=0; i<numSubHeaders; i++) {
                firstCodeArray[i] = cBuffer.get();
                entryCountArray[i] = cBuffer.get();
                idDeltaArray[i] = (short)cBuffer.get();
                idRangeOffSetArray[i] = cBuffer.get();
//              System.out.println("sh["+i+"]:fc="+(int)firstCodeArray[i]+
//                                 " ec="+(int)entryCountArray[i]+
//                                 " delta="+(int)idDeltaArray[i]+
//                                 " offset="+(int)idRangeOffSetArray[i]);
            }

            int glyphIndexArrSize = (tableLen-518-numSubHeaders*8)/2;
            glyphIndexArray = new char[glyphIndexArrSize];
            for (int i=0; i<glyphIndexArrSize;i++) {
                glyphIndexArray[i] = cBuffer.get();
            }
        }

        char getGlyph(int charCode) {
            final int origCharCode = charCode;
            int controlGlyph = getControlCodeGlyph(charCode, true);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }

            if (xlat != null) {
                charCode = xlat[charCode];
            }

            char highByte = (char)(charCode >> 8);
            char lowByte = (char)(charCode & 0xff);
            int key = subHeaderKey[highByte]>>3; // index into subHeaders
            char mapMe;

            if (key != 0) {
                mapMe = lowByte;
            } else {
                mapMe = highByte;
                if (mapMe == 0) {
                    mapMe = lowByte;
                }
            }

//          System.err.println("charCode="+Integer.toHexString(charCode)+
//                             " key="+key+ " mapMe="+Integer.toHexString(mapMe));
            char firstCode = firstCodeArray[key];
            if (mapMe < firstCode) {
                return 0;
            } else {
                mapMe -= firstCode;
            }

            if (mapMe < entryCountArray[key]) {
                /* "address" arithmetic is needed to calculate the offset
                 * into glyphIndexArray. "idRangeOffSetArray[key]" specifies
                 * the number of bytes from that location in the table where
                 * the subarray of glyphIndexes starting at "firstCode" begins.
                 * Each entry in the subHeader table is 8 bytes, and the
                 * idRangeOffSetArray field is at offset 6 in the entry.
                 * The glyphIndexArray immediately follows the subHeaders.
                 * So if there are "N" entries then the number of bytes to the
                 * start of glyphIndexArray is (N-key)*8-6.
                 * Subtract this from the idRangeOffSetArray value to get
                 * the number of bytes into glyphIndexArray and divide by 2 to
                 * get the (char) array index.
                 */
                int glyphArrayOffset = ((idRangeOffSetArray.length-key)*8)-6;
                int glyphSubArrayStart =
                        (idRangeOffSetArray[key] - glyphArrayOffset)/2;
                char glyphCode = glyphIndexArray[glyphSubArrayStart+mapMe];
                if (glyphCode != 0) {
                    glyphCode += idDeltaArray[key]; //idDelta
                    return glyphCode;
                }
            }
            return getFormatCharGlyph(origCharCode);
        }
    }

    // Format 6: Trimmed table mapping
    static class CMapFormat6 extends CMap {

        char firstCode;
        char entryCount;
        char[] glyphIdArray;

        CMapFormat6(ByteBuffer bbuffer, int offset, char[] xlat) {

             bbuffer.position(offset+6);
             CharBuffer buffer = bbuffer.asCharBuffer();
             firstCode = buffer.get();
             entryCount = buffer.get();
             glyphIdArray = new char[entryCount];
             for (int i=0; i< entryCount; i++) {
                 glyphIdArray[i] = buffer.get();
             }
         }

         char getGlyph(int charCode) {
            final int origCharCode = charCode;
            int controlGlyph = getControlCodeGlyph(charCode, true);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }

             if (xlat != null) {
                 charCode = xlat[charCode];
             }

             charCode -= firstCode;
             if (charCode < 0 || charCode >= entryCount) {
                  return getFormatCharGlyph(origCharCode);
             } else {
                  return glyphIdArray[charCode];
             }
         }
    }

    // Format 8: mixed 16-bit and 32-bit coverage
    // Seems unlikely this code will ever get tested as we look for
    // MS platform Cmaps and MS states (in the Opentype spec on their website)
    // that MS doesn't support this format
    static class CMapFormat8 extends CMap {
         byte[] is32 = new byte[8192];
         int nGroups;
         int[] startCharCode;
         int[] endCharCode;
         int[] startGlyphID;

         CMapFormat8(ByteBuffer bbuffer, int offset, char[] xlat) {

             bbuffer.position(12);
             bbuffer.get(is32);
             nGroups = bbuffer.getInt() & INTMASK;
             // A map group record is three uint32's making for 12 bytes total
             if (bbuffer.remaining() < (12 * (long)nGroups)) {
                 throw new RuntimeException("Format 8 table exceeded");
             }
             startCharCode = new int[nGroups];
             endCharCode   = new int[nGroups];
             startGlyphID  = new int[nGroups];
         }

        char getGlyph(int charCode) {
            if (xlat != null) {
                throw new RuntimeException("xlat array for cmap fmt=8");
            }
            return 0;
        }

    }


    // Format 4-byte 10: Trimmed table mapping
    // Seems unlikely this code will ever get tested as we look for
    // MS platform Cmaps and MS states (in the Opentype spec on their website)
    // that MS doesn't support this format
    static class CMapFormat10 extends CMap {

         long firstCode;
         int entryCount;
         char[] glyphIdArray;

         CMapFormat10(ByteBuffer bbuffer, int offset, char[] xlat) {

             bbuffer.position(offset+12);
             firstCode = bbuffer.getInt() & INTMASK;
             entryCount = bbuffer.getInt() & INTMASK;
             // each glyph is a uint16, so 2 bytes per value.
             if (bbuffer.remaining() < (2 * (long)entryCount)) {
                 throw new RuntimeException("Format 10 table exceeded");
             }
             CharBuffer buffer = bbuffer.asCharBuffer();
             glyphIdArray = new char[entryCount];
             for (int i=0; i< entryCount; i++) {
                 glyphIdArray[i] = buffer.get();
             }
         }

         char getGlyph(int charCode) {

             if (xlat != null) {
                 throw new RuntimeException("xlat array for cmap fmt=10");
             }

             int code = (int)(charCode - firstCode);
             if (code < 0 || code >= entryCount) {
                 return 0;
             } else {
                 return glyphIdArray[code];
             }
         }
    }

    // Format 12: Segmented coverage for UCS-4 (fonts supporting
    // surrogate pairs)
    static class CMapFormat12 extends CMap {

        int numGroups;
        int highBit =0;
        int power;
        int extra;
        long[] startCharCode;
        long[] endCharCode;
        int[] startGlyphID;

        CMapFormat12(ByteBuffer buffer, int offset, char[] xlat) {
            if (xlat != null) {
                throw new RuntimeException("xlat array for cmap fmt=12");
            }

            buffer.position(offset+12);
            numGroups = buffer.getInt() & INTMASK;
            // A map group record is three uint32's making for 12 bytes total
            if (buffer.remaining() < (12 * (long)numGroups)) {
                throw new RuntimeException("Format 12 table exceeded");
            }
            startCharCode = new long[numGroups];
            endCharCode = new long[numGroups];
            startGlyphID = new int[numGroups];
            buffer = buffer.slice();
            IntBuffer ibuffer = buffer.asIntBuffer();
            for (int i=0; i<numGroups; i++) {
                startCharCode[i] = ibuffer.get() & INTMASK;
                endCharCode[i] = ibuffer.get() & INTMASK;
                startGlyphID[i] = ibuffer.get() & INTMASK;
            }

            /* Finds the high bit by binary searching through the bits */
            int value = numGroups;

            if (value >= 1 << 16) {
                value >>= 16;
                highBit += 16;
            }

            if (value >= 1 << 8) {
                value >>= 8;
                highBit += 8;
            }

            if (value >= 1 << 4) {
                value >>= 4;
                highBit += 4;
            }

            if (value >= 1 << 2) {
                value >>= 2;
                highBit += 2;
            }

            if (value >= 1 << 1) {
                value >>= 1;
                highBit += 1;
            }

            power = 1 << highBit;
            extra = numGroups - power;
        }

        char getGlyph(int charCode) {
            final int origCharCode = charCode;
            int controlGlyph = getControlCodeGlyph(charCode, false);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }
            int probe = power;
            int range = 0;

            if (startCharCode[extra] <= charCode) {
                range = extra;
            }

            while (probe > 1) {
                probe >>= 1;

                if (startCharCode[range+probe] <= charCode) {
                    range += probe;
                }
            }

            if (startCharCode[range] <= charCode &&
                  endCharCode[range] >= charCode) {
                return (char)
                    (startGlyphID[range] + (charCode - startCharCode[range]));
            }

            return getFormatCharGlyph(origCharCode);
        }

    }

    /* Used to substitute for bad Cmaps. */
    static class NullCMapClass extends CMap {

        char getGlyph(int charCode) {
            return 0;
        }
    }

    public static final NullCMapClass theNullCmap = new NullCMapClass();

    final int getControlCodeGlyph(int charCode, boolean noSurrogates) {
        if (charCode < 0x0010) {
            switch (charCode) {
            case 0x0009:
            case 0x000a:
            case 0x000d: return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
            }
         } else if (noSurrogates && charCode >= 0xFFFF) {
            return 0;
        }
        return -1;
    }

    final char getFormatCharGlyph(int charCode) {
        if (charCode >= 0x200c) {
            if ((charCode <= 0x200f) ||
                (charCode >= 0x2028 && charCode <= 0x202e) ||
                (charCode >= 0x206a && charCode <= 0x206f)) {
                return (char)CharToGlyphMapper.INVISIBLE_GLYPH_ID;
            }
        }
        return 0;
    }

    static class UVS {
        int numSelectors;
        int[] selector;

        //for Non-Default UVS Table
        int[] numUVSMapping;
        int[][] unicodeValue;
        char[][] glyphID;

        UVS(ByteBuffer buffer, int offset) {
            buffer.position(offset+6);
            numSelectors = buffer.getInt() & INTMASK;
            // A variation selector record is one 3 byte int + two int32's
            // making for 11 bytes per record.
            if (buffer.remaining() < (11 * (long)numSelectors)) {
                throw new RuntimeException("Variations exceed buffer");
            }
            selector = new int[numSelectors];
            numUVSMapping = new int[numSelectors];
            unicodeValue = new int[numSelectors][];
            glyphID = new char[numSelectors][];

            for (int i = 0; i < numSelectors; i++) {
                buffer.position(offset + 10 + i * 11);
                selector[i] = (buffer.get() & 0xff) << 16; //UINT24
                selector[i] += (buffer.get() & 0xff) << 8;
                selector[i] += buffer.get() & 0xff;

                //skip Default UVS Table

                //for Non-Default UVS Table
                int tableOffset = buffer.getInt(offset + 10 + i * 11 + 7);
                if (tableOffset == 0) {
                    numUVSMapping[i] = 0;
                } else if (tableOffset > 0) {
                    buffer.position(offset+tableOffset);
                    numUVSMapping[i] = buffer.getInt() & INTMASK;
                    // a UVS mapping record is one 3 byte int + uint16
                    // making for 5 bytes per record.
                    if (buffer.remaining() < (5 * (long)numUVSMapping[i])) {
                        throw new RuntimeException("Variations exceed buffer");
                    }
                    unicodeValue[i] = new int[numUVSMapping[i]];
                    glyphID[i] = new char[numUVSMapping[i]];

                    for (int j = 0; j < numUVSMapping[i]; j++) {
                        int temp = (buffer.get() & 0xff) << 16; //UINT24
                        temp += (buffer.get() & 0xff) << 8;
                        temp += buffer.get() & 0xff;
                        unicodeValue[i][j] = temp;
                        glyphID[i][j] = buffer.getChar();
                    }
                }
            }
        }

        static final int VS_NOGLYPH = 0;
        private int getGlyph(int charCode, int variationSelector) {
            int targetSelector = -1;
            for (int i = 0; i < numSelectors; i++) {
                if (selector[i] == variationSelector) {
                    targetSelector = i;
                    break;
                }
            }
            if (targetSelector == -1) {
                return VS_NOGLYPH;
            }
            if (numUVSMapping[targetSelector] > 0) {
                int index = java.util.Arrays.binarySearch(
                                unicodeValue[targetSelector], charCode);
                if (index >= 0) {
                    return glyphID[targetSelector][index];
                }
            }
            return VS_NOGLYPH;
        }
    }

    char getVariationGlyph(int charCode, int variationSelector) {
        char glyph = 0;
        if (uvs == null) {
            glyph = getGlyph(charCode);
        } else {
            int result = uvs.getGlyph(charCode, variationSelector);
            if (result > 0) {
                glyph = (char)(result & 0xFFFF);
            } else {
                glyph = getGlyph(charCode);
            }
        }
        return glyph;
    }
}
