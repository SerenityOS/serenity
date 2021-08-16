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

public final class Script {

    public static final int INVALID_CODE = -1;
    public static final int COMMON       =  0;  /* Zyyy */
    public static final int INHERITED    =  1;  /* Qaai */

    public static final int ARABIC       =  2;  /* Arab */
    public static final int ARMENIAN     =  3;  /* Armn */
    public static final int BENGALI      =  4;  /* Beng */
    public static final int BOPOMOFO     =  5;  /* Bopo */
    public static final int CHEROKEE     =  6;  /* Cher */
    public static final int COPTIC       =  7;  /* Qaac */
    public static final int CYRILLIC     =  8;  /* Cyrl (Cyrs) */
    public static final int DESERET      =  9;  /* Dsrt */
    public static final int DEVANAGARI   = 10;  /* Deva */
    public static final int ETHIOPIC     = 11;  /* Ethi */
    public static final int GEORGIAN     = 12;  /* Geor (Geon; Geoa) */
    public static final int GOTHIC       = 13;  /* Goth */
    public static final int GREEK        = 14;  /* Grek */
    public static final int GUJARATI     = 15;  /* Gujr */
    public static final int GURMUKHI     = 16;  /* Guru */
    public static final int HAN          = 17;  /* Hani */
    public static final int HANGUL       = 18;  /* Hang */
    public static final int HEBREW       = 19;  /* Hebr */
    public static final int HIRAGANA     = 20;  /* Hira */
    public static final int KANNADA      = 21;  /* Knda */
    public static final int KATAKANA     = 22;  /* Kana */
    public static final int KHMER        = 23;  /* Khmr */
    public static final int LAO          = 24;  /* Laoo */
    public static final int LATIN        = 25;  /* Latn (Latf; Latg) */
    public static final int MALAYALAM    = 26;  /* Mlym */
    public static final int MONGOLIAN    = 27;  /* Mong */
    public static final int MYANMAR      = 28;  /* Mymr */
    public static final int OGHAM        = 29;  /* Ogam */
    public static final int OLD_ITALIC   = 30;  /* Ital */
    public static final int ORIYA        = 31;  /* Orya */
    public static final int RUNIC        = 32;  /* Runr */
    public static final int SINHALA      = 33;  /* Sinh */
    public static final int SYRIAC       = 34;  /* Syrc (Syrj; Syrn; Syre) */
    public static final int TAMIL        = 35;  /* Taml */
    public static final int TELUGU       = 36;  /* Telu */
    public static final int THAANA       = 37;  /* Thaa */
    public static final int THAI         = 38;  /* Thai */
    public static final int TIBETAN      = 39;  /* Tibt */
    public static final int CANADIAN_ABORIGINAL = 40;  /* Cans */
    public static final int UCAS         = CANADIAN_ABORIGINAL;  /* Cans */
    public static final int YI           = 41;  /* Yiii */
    public static final int TAGALOG      = 42;  /* Tglg */
    public static final int HANUNOO      = 43;  /* Hano */
    public static final int BUHID        = 44;  /* Buhd */
    public static final int TAGBANWA     = 45;  /* Tagb */
    public static final int CODE_LIMIT   = 46;
}
