/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * Ideally there would be no native fonts used, and this class would be
 * unneeded and removed. Presently it is still needed until such time
 * as font configuration files (or the implementation equivalent) can have
 * all references to fonts that are not handled via Java 2D removed.
 * Currently there are two cases where this class is needed, both on
 * Unix, primarily Solaris, but useful on Linux too if fonts have moved.
 * 1. Some legacy F3 fonts are still referenced so that AWT "X/Motif"
 * can get dingbats and symbols from them. This can be dispensed with when
 * either AWT is based on 2D, or when the X font path is known to always
 * contain a Type1 or TrueType font that can be used in font configuration
 * files to replace the F3 fonts.
 * 2. When location of font files by 2D fails, because of some system
 * configuration problem, it is desirable to have a fall back to some
 * functionality that lessens the immediate impact on users. Being able
 * to perform limited operations by using bitmaps from X11 helps here.
 */

public class NativeFont extends PhysicalFont {

    String encoding;

    private int numGlyphs = -1;
    boolean isBitmapDelegate;
    PhysicalFont delegateFont;

    /**
     * Verifies native font is accessible.
     * @throws FontFormatException if the font can't be located.
     */
    public NativeFont(String platName, boolean bitmapDelegate)
        throws FontFormatException {
        super(platName, null);

        /* This is set true if this is an instance of a NativeFont
         * created by some other font, to get native bitmaps.
         * The delegating font will call this font only for "basic"
         * cases - ie non-rotated, uniform scale, monochrome bitmaps.
         * If this is false, then this instance may need to itself
         * delegate to another font for non-basic cases. Since
         * NativeFonts are used in that way only for symbol and dingbats
         * we know its safe to delegate these to the JRE's default
         * physical font (Lucida Sans Regular).
         */
        isBitmapDelegate = bitmapDelegate;

        if (GraphicsEnvironment.isHeadless()) {
            throw new FontFormatException("Native font in headless toolkit");
        }
        fontRank = Font2D.NATIVE_RANK;
        initNames();
        if (getNumGlyphs() == 0) {
          throw new FontFormatException("Couldn't locate font" + platName);
        }
    }

    private void initNames() throws FontFormatException {
        /* Valid XLFD has exactly 14 "-" chars.
         * First run over the string to verify have at least this many
         * At the same time record the locations of the hyphens
         * so we can just pick the right substring later on
         */
        int[] hPos = new int[14];
        int hyphenCnt = 1;
        int pos = 1;

        String xlfd = platName.toLowerCase(Locale.ENGLISH);
        if (xlfd.startsWith("-")) {
            while (pos != -1 && hyphenCnt < 14) {
                pos = xlfd.indexOf('-', pos);
                if (pos != -1) {
                    hPos[hyphenCnt++] = pos;
                    pos++;
                }
            }
        }

        if (hyphenCnt == 14 && pos != -1) {

            /* Capitalise words in the Family name */
            String tmpFamily = xlfd.substring(hPos[1]+1, hPos[2]);
            StringBuilder sBuffer = new StringBuilder(tmpFamily);
            char ch = Character.toUpperCase(sBuffer.charAt(0));
            sBuffer.replace(0, 1, String.valueOf(ch));
            for (int i=1;i<sBuffer.length()-1; i++) {
                if (sBuffer.charAt(i) == ' ') {
                    ch = Character.toUpperCase(sBuffer.charAt(i+1));
                    sBuffer.replace(i+1, i+2, String.valueOf(ch));
                }
            }
            familyName = sBuffer.toString();

            String tmpWeight = xlfd.substring(hPos[2]+1, hPos[3]);
            String tmpSlant = xlfd.substring(hPos[3]+1, hPos[4]);

            String styleStr = null;

            if (tmpWeight.indexOf("bold") >= 0 ||
                tmpWeight.indexOf("demi") >= 0) {
                style |= Font.BOLD;
                styleStr = "Bold";
            }

            if (tmpSlant.equals("i") ||
                tmpSlant.indexOf("italic") >= 0) {
                style |= Font.ITALIC;

                if (styleStr == null) {
                    styleStr = "Italic";
                } else {
                    styleStr = styleStr + " Italic";
                }
            }
            else if (tmpSlant.equals("o") ||
                tmpSlant.indexOf("oblique") >= 0) {
                style |= Font.ITALIC;
                if (styleStr == null) {
                    styleStr = "Oblique";
                } else {
                    styleStr = styleStr + " Oblique";
                }
            }

            if (styleStr == null) {
                fullName = familyName;
            } else {
                fullName = familyName + " " + styleStr;
            }

            encoding = xlfd.substring(hPos[12]+1);
            if (encoding.startsWith("-")) {
                encoding = xlfd.substring(hPos[13]+1);
            }
            if (encoding.indexOf("fontspecific") >= 0) {
                if (tmpFamily.indexOf("dingbats") >= 0) {
                    encoding = "dingbats";
                } else if (tmpFamily.indexOf("symbol") >= 0) {
                    encoding = "symbol";
                } else {
                    encoding = "iso8859-1";
                }
            }
        } else {
            throw new FontFormatException("Bad native name " + platName);
//             familyName = "Unknown";
//             fullName = "Unknown";
//             style = Font.PLAIN;
//             encoding = "iso8859-1";
        }
    }

    /* Wildcard all the size fields in the XLFD and retrieve a list of
     * XLFD's that match.
     * We only look for scaleable fonts, so we can just replace the 0's
     * with *'s and see what we get back
     * No matches means even the scaleable version wasn't found. This is
     * means the X font path isn't set up for this font at all.
     * One match means only the scaleable version we started with was found
     * -monotype-arial-bold-i-normal--0-0-0-0-p-0-iso8859-1
     * Two matches apparently means as well as the above, a scaleable
     * specified for 72 dpi is found, not that there are bitmaps : eg
     * -monotype-arial-bold-i-normal--0-0-72-72-p-0-iso8859-1
     * So require at least 3 matches (no need to parse) to determine that
     * there are external bitmaps.
     */
    static boolean hasExternalBitmaps(String platName) {
        /* Turn -monotype-arial-bold-i-normal--0-0-0-0-p-0-iso8859-1
         * into -monotype-arial-bold-i-normal--*-*-*-*-p-*-iso8859-1
         * by replacing all -0- substrings with -*-
         */
        StringBuilder sb = new StringBuilder(platName);
        int pos = sb.indexOf("-0-");
        while (pos >=0) {
            sb.replace(pos+1, pos+2, "*");
            pos = sb.indexOf("-0-", pos);
        };
        String xlfd = sb.toString();
        return haveBitmapFonts(xlfd.getBytes(UTF_8));
    }

    public static boolean fontExists(String xlfd) {
        return fontExists(xlfd.getBytes(UTF_8));
    }

    private static native boolean haveBitmapFonts(byte[] xlfd);
    private static native boolean fontExists(byte[] xlfd);

    public CharToGlyphMapper getMapper() {
        if (mapper == null) {
            if (isBitmapDelegate) {
                /* we are a delegate */
                mapper = new NativeGlyphMapper(this);
            } else {
                /* we need to delegate */
                SunFontManager fm = SunFontManager.getInstance();
                delegateFont = fm.getDefaultPhysicalFont();
                mapper = delegateFont.getMapper();
            }
        }
        return mapper;
    }

    FontStrike createStrike(FontStrikeDesc desc) {
        if (isBitmapDelegate) {
            return new NativeStrike(this, desc);
        } else {
            if (delegateFont == null) {
                SunFontManager fm = SunFontManager.getInstance();
                delegateFont = fm.getDefaultPhysicalFont();
            }
            /* If no FileFont's are found, delegate font may be
             * a NativeFont, so we need to avoid recursing here.
             */
            if (delegateFont instanceof NativeFont) {
                return new NativeStrike((NativeFont)delegateFont, desc);
            }
            FontStrike delegate = delegateFont.createStrike(desc);
            return new DelegateStrike(this, desc, delegate);
        }
    }

    public Rectangle2D getMaxCharBounds(FontRenderContext frc) {
            return null;
    }

    native StrikeMetrics getFontMetrics(long pScalerContext);

    native float getGlyphAdvance(long pContext, int glyphCode);

    Rectangle2D.Float getGlyphOutlineBounds(long pScalerContext,
                                            int glyphCode) {
        return new Rectangle2D.Float(0f, 0f, 0f, 0f);
    }

    public GeneralPath getGlyphOutline(long pScalerContext,
                                       int glyphCode,
                                       float x,
                                       float y) {
        return null;
    }

    native long getGlyphImage(long pScalerContext, int glyphCode);

    native long getGlyphImageNoDefault(long pScalerContext, int glyphCode);

    void getGlyphMetrics(long pScalerContext, int glyphCode,
                        Point2D.Float metrics) {
        throw new RuntimeException("this should be called on the strike");
    }

    public  GeneralPath getGlyphVectorOutline(long pScalerContext,
                                              int[] glyphs, int numGlyphs,
                                              float x,  float y) {
        return null;
    }

    private native int countGlyphs(byte[] platformNameBytes, int ptSize);

    public int getNumGlyphs() {
        if (numGlyphs == -1) {
            byte[] bytes = getPlatformNameBytes(8);
            numGlyphs = countGlyphs(bytes, 8);
        }
        return numGlyphs;
    }

    PhysicalFont getDelegateFont() {
        if (delegateFont == null) {
            SunFontManager fm = SunFontManager.getInstance();
            delegateFont = fm.getDefaultPhysicalFont();
        }
        return delegateFont;
    }

    /* Specify that the dpi is 72x72, as this corresponds to JDK's
     * default user space. These are the 10th and 11th fields in the XLFD.
     * ptSize in XLFD is in 10th's of a point so multiply by 10,
     * Replace the 9th field in the XLFD (ie after the 8th hyphen)
     * with this pt size (this corresponds to the field that's "%d" in the
     * font configuration files). Wild card the other numeric fields.
     * ie to request 12 pt Times New Roman italic font, use an XLFD like :
     * -monotype-times new roman-regular-i---*-120-72-72-p-*-iso8859-1
     */
    @SuppressWarnings("cast")
    byte[] getPlatformNameBytes(int ptSize) {
        int[] hPos = new int[14];
        int hyphenCnt = 1;
        int pos = 1;

        while (pos != -1 && hyphenCnt < 14) {
            pos = platName.indexOf('-', pos);
            if (pos != -1) {
                hPos[hyphenCnt++] = pos;
                    pos++;
            }
        }
        String sizeStr = Integer.toString((int)Math.abs(ptSize)*10);
        StringBuilder sb = new StringBuilder(platName);
        /* work backwards so as to not invalidate the positions. */
        sb.replace(hPos[11]+1, hPos[12], "*");

        sb.replace(hPos[9]+1, hPos[10], "72");

        sb.replace(hPos[8]+1, hPos[9], "72");

        /* replace the 3 lines above with the next 3 lines to get the 1.4.2
         * behaviour
         */
//      sb.replace(hPos[11]+1, hPos[12], "0");
//      sb.replace(hPos[9]+1, hPos[10], "0");
//      sb.replace(hPos[8]+1, hPos[9], "0");

        sb.replace(hPos[7]+1, hPos[8], sizeStr);

        sb.replace(hPos[6]+1, hPos[7], "*");

        /* replace the 1 line above with the next line to get the 1.4.2
         * behaviour
         */
//      sb.replace(hPos[6]+1, hPos[7], "0");

        /* comment out this block to the 1.4.2 behaviour */
        if (hPos[0] == 0 && hPos[1] == 1) {
            /* null foundry name : some linux font configuration files have
             * symbol font entries like this and its just plain wrong.
             * Replace with a wild card. (Although those fonts should be
             * located via disk access rather than X11).
             */
           sb.replace(hPos[0]+1, hPos[1], "*");
        }

        String xlfd = sb.toString();
        return xlfd.getBytes(UTF_8);
    }

    public String toString() {
        return " ** Native Font: Family="+familyName+ " Name="+fullName+
            " style="+style+" nativeName="+platName;
    }
}
