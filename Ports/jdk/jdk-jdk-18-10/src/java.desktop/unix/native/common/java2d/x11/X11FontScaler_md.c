/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <ctype.h>
#include <sys/utsname.h>

#include <jni.h>
#include <jni_util.h>
#include "fontscalerdefs.h"
#include "X11FontScaler.h"

#ifndef HEADLESS

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <awt.h>

static GC pixmapGC = 0;
static Pixmap pixmap = 0;
static Atom psAtom = 0;
static Atom fullNameAtom = 0;
static int pixmapWidth = 0;
static int pixmapHeight = 0;

#define FONT_AWT_LOCK() \
env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2); \
AWT_LOCK();

int CreatePixmapAndGC (int width, int height)
{
    /* REMIND: use the actual screen, not the default screen */
    Window awt_defaultRoot =
        RootWindow(awt_display, DefaultScreen(awt_display));

    if (width < 100) {
      width = 100;
    }
    if (height < 100) {
      height = 100;
    }
    pixmapHeight = height;
    pixmapWidth = width;
    if (pixmap != 0) {
      XFreePixmap (awt_display, pixmap);
    }
    if (pixmapGC != NULL) {
      XFreeGC (awt_display, pixmapGC);
    }
    pixmap = XCreatePixmap (awt_display, awt_defaultRoot, pixmapWidth,
                          pixmapHeight, 1);
    if (pixmap == 0) {
      return BadAlloc;
    }
    pixmapGC = XCreateGC (awt_display, pixmap, 0, 0);
    if (pixmapGC == NULL) {
      return BadAlloc;
    }
    XFillRectangle (awt_display, pixmap, pixmapGC, 0, 0, pixmapWidth,
                  pixmapHeight);
    XSetForeground (awt_display, pixmapGC, 1);
    return Success;
}

#ifdef DUMP_IMAGES

static void dumpXImage(XImage *ximage)
{
    int height = ximage->height;
    int width = ximage->width;
    int row;
    int column;

    fprintf(stderr, "-------------------------------------------\n");
    for (row = 0; row < height; ++row) {
      for (column = 0; column < width; ++column) {
          int pixel = ximage->f.get_pixel(ximage, column, row);
          fprintf(stderr, (pixel == 0) ? "  " : "XX");
      }
      fprintf(stderr, "\n");
    }
    fprintf(stderr, "-------------------------------------------\n");
}

#endif

#endif /* !HEADLESS */

JNIEXPORT int JNICALL AWTCountFonts(char* xlfd) {
#ifdef HEADLESS
    return 0;
#else
    char **names;
    int count;
    JNIEnv *env;
    FONT_AWT_LOCK();
    names = XListFonts(awt_display, xlfd, 3, &count);
    XFreeFontNames(names);
    AWT_UNLOCK();
    return count;
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL AWTLoadFont(char* name, AWTFont *pReturn) {
    JNIEnv *env;
    *pReturn = NULL;
#ifndef HEADLESS
    FONT_AWT_LOCK();
    *pReturn = (AWTFont)XLoadQueryFont(awt_display, name);
    AWT_UNLOCK();
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL AWTFreeFont(AWTFont font) {
#ifndef HEADLESS
    JNIEnv *env;
    FONT_AWT_LOCK();
    XFreeFont(awt_display, (XFontStruct *)font);
    AWT_UNLOCK();
#endif /* !HEADLESS */
}

JNIEXPORT unsigned JNICALL AWTFontMinByte1(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->min_byte1;
#endif /* !HEADLESS */
}

JNIEXPORT unsigned JNICALL AWTFontMaxByte1(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->max_byte1;
#endif /* !HEADLESS */
}

JNIEXPORT unsigned JNICALL AWTFontMinCharOrByte2(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->min_char_or_byte2;
#endif /* !HEADLESS */
}

JNIEXPORT unsigned JNICALL AWTFontMaxCharOrByte2(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->max_char_or_byte2;
#endif /* !HEADLESS */
}

JNIEXPORT unsigned JNICALL AWTFontDefaultChar(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->default_char;
#endif /* !HEADLESS */
}

JNIEXPORT AWTChar JNICALL AWTFontPerChar(AWTFont font, int index) {
#ifdef HEADLESS
    return NULL;
#else
    XFontStruct *fXFont = (XFontStruct *)font;
    XCharStruct *perChar = fXFont->per_char;
    if (perChar == NULL) {
        return NULL;
    }
    return (AWTChar)&(perChar[index]);
#endif /* !HEADLESS */
}

JNIEXPORT AWTChar JNICALL AWTFontMaxBounds(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return (AWTChar)&((XFontStruct *)font)->max_bounds;
#endif /* !HEADLESS */
}


JNIEXPORT int JNICALL AWTFontAscent(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->ascent;
#endif /* !HEADLESS */
}


JNIEXPORT int JNICALL AWTFontDescent(AWTFont font) {
#ifdef HEADLESS
    return 0;
#else
    return ((XFontStruct *)font)->descent;
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL AWTFontTextExtents16(AWTFont font,
                                            AWTChar2b* xChar,
                                            AWTChar* overall) {
#ifndef HEADLESS
    JNIEnv *env;
    int ascent, descent, direction;
    XFontStruct* xFont = (XFontStruct*)font;
    XCharStruct* newChar = (XCharStruct*)malloc(sizeof(XCharStruct));
    *overall = (AWTChar)newChar;
    /* There is a claim from the pre 1.5 source base that the info in the
     * XFontStruct is flaky for 16 byte chars. This seems plausible as
     * for info to be valid, that struct would need a large number of
     * XCharStructs. But there's nothing in the X APIs which warns you of
     * this. If it really is flaky you must question why there's an
     * XTextExtents16 API call. Try XTextExtents16 for now and if it fails
     * go back to XQueryTextExtents16 in this function.
     * Indeed the metrics from the Solaris 9 JA font
     * -ricoh-gothic-medium-r-normal--*-140-72-72-m-*-jisx0208.1983-0
     * do appear different so revert to the query api
     */
    FONT_AWT_LOCK();
    XQueryTextExtents16(awt_display,xFont->fid, xChar, 1,
                        &direction, &ascent, &descent, newChar);
/* XTextExtents16(xFont, xChar, 1, &direction, &ascent, &descent, newChar);  */
    AWT_UNLOCK();
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL AWTFreeChar(AWTChar xChar) {
#ifndef HEADLESS
    free(xChar);
#endif /* !HEADLESS */
}

JNIEXPORT jlong JNICALL AWTFontGenerateImage(AWTFont pFont, AWTChar2b* xChar) {

#ifndef HEADLESS

    int width, height, direction, ascent, descent;
    GlyphInfo *glyphInfo;
    XFontStruct* xFont = (XFontStruct*)pFont;
    XCharStruct xcs;
    XImage *ximage;
    int h, i, j, nbytes;
    unsigned char *srcRow, *dstRow, *dstByte;
    int wholeByteCount, remainingBitsCount;
    unsigned int imageSize;
    JNIEnv *env;


    FONT_AWT_LOCK();
/*     XTextExtents16(xFont, xChar, 1, &direction, &ascent, &descent, &xcs); */
    XQueryTextExtents16(awt_display,xFont->fid, xChar, 1,
                        &direction, &ascent, &descent, &xcs);
    width = xcs.rbearing - xcs.lbearing;
    height = xcs.ascent+xcs.descent;
    imageSize = width*height;
    glyphInfo = (GlyphInfo*)malloc(sizeof(GlyphInfo)+imageSize);
    if (glyphInfo == NULL) {
        AWT_UNLOCK();
        return (jlong)(uintptr_t)NULL;
    }
    glyphInfo->cellInfo = NULL;
    glyphInfo->width = width;
    glyphInfo->height = height;
    glyphInfo->topLeftX = xcs.lbearing;
    glyphInfo->topLeftY = -xcs.ascent;
    glyphInfo->advanceX = xcs.width;
    glyphInfo->advanceY = 0;

    if (imageSize == 0) {
        glyphInfo->image = NULL;
        AWT_UNLOCK();
        return (jlong)(uintptr_t)glyphInfo;
    } else {
        glyphInfo->image = (unsigned char*)glyphInfo+sizeof(GlyphInfo);
    }

    if ((pixmap == 0) || (width > pixmapWidth) || (height > pixmapHeight)) {
        if (CreatePixmapAndGC(width, height) != Success) {
            glyphInfo->image = NULL;
            AWT_UNLOCK();
            return (jlong)(uintptr_t)glyphInfo;
        }
    }

    XSetFont(awt_display, pixmapGC, xFont->fid);
    XSetForeground(awt_display, pixmapGC, 0);
    XFillRectangle(awt_display, pixmap, pixmapGC, 0, 0,
                   pixmapWidth, pixmapHeight);
    XSetForeground(awt_display, pixmapGC, 1);
    XDrawString16(awt_display, pixmap, pixmapGC,
                  -xcs.lbearing, xcs.ascent, xChar, 1);
    ximage = XGetImage(awt_display, pixmap, 0, 0, width, height,
                       AllPlanes, XYPixmap);

    if (ximage == NULL) {
        glyphInfo->image = NULL;
        AWT_UNLOCK();
        return (jlong)(uintptr_t)glyphInfo;
    }

#ifdef DUMP_IMAGES
    dumpXImage(ximage);
#endif

    nbytes =  ximage->bytes_per_line;
    srcRow = (unsigned char*)ximage->data;
    dstRow = (unsigned char*)glyphInfo->image;
    wholeByteCount = width >> 3;
    remainingBitsCount = width & 7;

    for (h=0; h<height; h++) {
        const UInt8* src8 = srcRow;
        UInt8 *dstByte = dstRow;
        UInt32 srcValue;

        srcRow += nbytes;
        dstRow += width;

        for (i = 0; i < wholeByteCount; i++) {
            srcValue = *src8++;
            for (j = 0; j < 8; j++) {
                if (ximage->bitmap_bit_order == LSBFirst) {
                    *dstByte++ = (srcValue & 0x01) ? 0xFF : 0;
                    srcValue >>= 1;
                } else {                /* MSBFirst */
                    *dstByte++ = (srcValue & 0x80) ? 0xFF : 0;
                    srcValue <<= 1;
                }
            }
        }
        if (remainingBitsCount) {
            srcValue = *src8;
            for (j = 0; j < remainingBitsCount; j++) {
                if (ximage->bitmap_bit_order == LSBFirst) {
                    *dstByte++ = (srcValue & 0x01) ? 0xFF : 0;
                    srcValue >>= 1;
                } else {                /* MSBFirst */
                    *dstByte++ = (srcValue & 0x80) ? 0xFF : 0;
                    srcValue <<= 1;
                }
            }
        }
    }

    XDestroyImage (ximage);
    AWT_UNLOCK();
    return (jlong)(uintptr_t)glyphInfo;
#else
    return (jlong)0;
#endif /* !HEADLESS */
}

JNIEXPORT short JNICALL AWTCharAdvance(AWTChar xChar) {
#ifdef HEADLESS
    return 0;
#else
    return ((XCharStruct *)xChar)->width;
#endif /* !HEADLESS */
}

JNIEXPORT short JNICALL AWTCharLBearing(AWTChar xChar) {
#ifdef HEADLESS
    return 0;
#else
    return ((XCharStruct *)xChar)->lbearing;
#endif /* !HEADLESS */
}

JNIEXPORT short JNICALL AWTCharRBearing(AWTChar xChar) {
#ifdef HEADLESS
    return 0;
#else
    return ((XCharStruct *)xChar)->rbearing;
#endif /* !HEADLESS */
}

JNIEXPORT short JNICALL AWTCharAscent(AWTChar xChar) {
#ifdef HEADLESS
    return 0;
#else
    return ((XCharStruct *)xChar)->ascent;
#endif /* !HEADLESS */
}

JNIEXPORT short JNICALL AWTCharDescent(AWTChar xChar) {
#ifdef HEADLESS
    return 0;
#else
    return ((XCharStruct *)xChar)->descent;
#endif /* !HEADLESS */
}
