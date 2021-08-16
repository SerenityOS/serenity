/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "scriptMapping.h"
/*
 * Java level-code has a script code indexes that correspond to
 * the indexes used by the ICU layout library. In order to call
 * harfbuzz we must map these to the equivalent harfbuzz codes.
 * Some of these happen to be the same but not many.
 */

hb_script_t ICU_to_Harfbuzz_ScriptCode[] = {

    HB_SCRIPT_COMMON,           /* 0 */
    HB_SCRIPT_INHERITED,        /* 1 */
    HB_SCRIPT_ARABIC,           /* 2 */
    HB_SCRIPT_ARMENIAN,         /* 3 */
    HB_SCRIPT_BENGALI,          /* 4 */
    HB_SCRIPT_BOPOMOFO,         /* 5 */
    HB_SCRIPT_CHEROKEE,         /* 6 */
    HB_SCRIPT_COPTIC,           /* 7 */
    HB_SCRIPT_CYRILLIC,         /* 8 */
    HB_SCRIPT_DESERET,          /* 9 */
    HB_SCRIPT_DEVANAGARI,       /* 10 */
    HB_SCRIPT_ETHIOPIC,         /* 11 */
    HB_SCRIPT_GEORGIAN,         /* 12 */
    HB_SCRIPT_GOTHIC,           /* 13 */
    HB_SCRIPT_GREEK,            /* 14 */
    HB_SCRIPT_GUJARATI,         /* 15 */
    HB_SCRIPT_GURMUKHI,         /* 16 */
    HB_SCRIPT_HAN,              /* 17 */
    HB_SCRIPT_HANGUL,           /* 18 */
    HB_SCRIPT_HEBREW,           /* 19 */
    HB_SCRIPT_HIRAGANA,         /* 20 */
    HB_SCRIPT_KANNADA,          /* 21 */
    HB_SCRIPT_KATAKANA,         /* 22 */
    HB_SCRIPT_KHMER,            /* 23 */
    HB_SCRIPT_LAO,              /* 24 */
    HB_SCRIPT_LATIN,            /* 25 */
    HB_SCRIPT_MALAYALAM,        /* 26 */
    HB_SCRIPT_MONGOLIAN,        /* 27 */
    HB_SCRIPT_MYANMAR,          /* 28 */
    HB_SCRIPT_OGHAM,            /* 29 */
    HB_SCRIPT_OLD_ITALIC,       /* 30 */
    HB_SCRIPT_ORIYA,            /* 31 */
    HB_SCRIPT_RUNIC,            /* 32 */
    HB_SCRIPT_SINHALA,          /* 33 */
    HB_SCRIPT_SYRIAC,           /* 34 */
    HB_SCRIPT_TAMIL,            /* 35 */
    HB_SCRIPT_TELUGU,           /* 36 */
    HB_SCRIPT_THAANA,           /* 37 */
    HB_SCRIPT_THAI,             /* 38 */
    HB_SCRIPT_TIBETAN,          /* 39 */
    HB_SCRIPT_CANADIAN_SYLLABICS,   /* 40 */
    HB_SCRIPT_YI,               /* 41 */
    HB_SCRIPT_TAGALOG,          /* 42 */
    HB_SCRIPT_HANUNOO,          /* 43 */
    HB_SCRIPT_BUHID,            /* 44 */
    HB_SCRIPT_TAGBANWA,         /* 45 */

};

int MAX_ICU_SCRIPTCODE = 45;

hb_script_t getHBScriptCode(int code) {
    if ((code < 0) || (code > MAX_ICU_SCRIPTCODE)) {
        return HB_SCRIPT_INVALID;
    }
    return ICU_to_Harfbuzz_ScriptCode[code];
}
