/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _X11FONTSCALER_H_
#define _X11FONTSCALER_H_

#include "gdefs.h"

#ifndef HEADLESS
#include <X11/Xlib.h>
#endif

#define SHIFTFACTOR 16
#define NO_POINTSIZE -1.0

#ifdef HEADLESS

typedef struct {
    unsigned char byte1;
    unsigned char byte2;
} AWTChar2b;

#define Success 1

#else /* !HEADLESS */

extern Display *awt_display;
typedef XChar2b AWTChar2b;

#endif /* !HEADLESS */

typedef void *AWTChar;
typedef void *AWTFont;

typedef struct NativeScalerContext {
    AWTFont xFont;
    int minGlyph;
    int maxGlyph;
    int numGlyphs;
    int defaultGlyph;
    int ptSize;
    double scale;
} NativeScalerContext;


/*
 * Important note : All AWTxxx functions are defined in font.h.
 * These were added to remove the dependency of certain files on X11.
 * These functions are used to perform X11 operations and should
 * be "stubbed out" in environments that do not support X11.
 */
JNIEXPORT int JNICALL AWTCountFonts(char* xlfd);
JNIEXPORT void JNICALL AWTLoadFont(char* name, AWTFont* pReturn);
JNIEXPORT void JNICALL AWTFreeFont(AWTFont font);
JNIEXPORT unsigned JNICALL AWTFontMinByte1(AWTFont font);
JNIEXPORT unsigned JNICALL AWTFontMaxByte1(AWTFont font);
JNIEXPORT unsigned JNICALL AWTFontMinCharOrByte2(AWTFont font);
JNIEXPORT unsigned JNICALL AWTFontMaxCharOrByte2(AWTFont font);
JNIEXPORT unsigned JNICALL AWTFontDefaultChar(AWTFont font);
/* Do not call AWTFreeChar() after AWTFontPerChar() or AWTFontMaxBounds() */
JNIEXPORT AWTChar JNICALL AWTFontPerChar(AWTFont font, int index);
JNIEXPORT AWTChar JNICALL AWTFontMaxBounds(AWTFont font);
JNIEXPORT int JNICALL AWTFontAscent(AWTFont font);
JNIEXPORT int JNICALL AWTFontDescent(AWTFont font);
/* Call AWTFreeChar() on overall after calling AWTFontQueryTextExtents16() */
JNIEXPORT void JNICALL AWTFontTextExtents16(AWTFont font, AWTChar2b* xChar,
                                            AWTChar* overall);
JNIEXPORT void JNICALL AWTFreeChar(AWTChar xChar);
JNIEXPORT jlong JNICALL AWTFontGenerateImage(AWTFont xFont, AWTChar2b* xChar);
JNIEXPORT short JNICALL AWTCharAdvance(AWTChar xChar);
JNIEXPORT short JNICALL AWTCharLBearing(AWTChar xChar);
JNIEXPORT short JNICALL AWTCharRBearing(AWTChar xChar);
JNIEXPORT short JNICALL AWTCharAscent(AWTChar xChar);
JNIEXPORT short JNICALL AWTCharDescent(AWTChar xChar);

#endif
