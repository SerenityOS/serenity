/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The function here is used to get a GDI rasterized LCD glyph and place it
 * into the JDK glyph cache. The benefit is rendering fidelity for the
 * most common cases, with no impact on the 2D rendering pipelines.
 *
 * Requires that the font and graphics are unrotated, and the scale is
 * a simple one, and the font is a TT font registered with windows.
 * Those conditions are established by the calling code.
 *
 * This code
 * - Receives the family name, style, and size of the font
 * and creates a Font object.
 * - Create a surface from which we can get a DC : must be 16 bit or more.
 * Ideally we'd be able to specify the depth of this, but in practice we
 * have to accept it will be the same as the default screen.
 * - Selects the GDI font on to the device
 * - Uses GetGlyphOutline to estimate the bounds.
 * - Creates a DIB on to which to blit the image.
 * - Creates a GlyphInfo structure and copies the GDI glyph and offsets
 * into the glyph which is returned.
 */

#include <stdio.h>
#include <malloc.h>
#include <math.h>
#include <windows.h>
#include <winuser.h>

#include <jni.h>
#include <jni_util.h>
#include <jlong_md.h>
#include <sizecalc.h>
#include <sun_font_FileFontStrike.h>

#include "fontscalerdefs.h"

/* Some of these are also defined in awtmsg.h but I don't want a dependency
 * on that here. They are needed here - and in awtmsg.h - until we
 * move up our build to define WIN32_WINNT >= 0x501 (ie XP), since MS
 * headers will not define them otherwise.
 */
#ifndef SPI_GETFONTSMOOTHINGTYPE
#define SPI_GETFONTSMOOTHINGTYPE        0x200A
#endif //SPI_GETFONTSMOOTHINGTYPE

#ifndef SPI_GETFONTSMOOTHINGCONTRAST
#define SPI_GETFONTSMOOTHINGCONTRAST    0x200C
#endif //SPI_GETFONTSMOOTHINGCONTRAST

#ifndef SPI_GETFONTSMOOTHINGORIENTATION
#define SPI_GETFONTSMOOTHINGORIENTATION    0x2012
#endif //SPI_GETFONTSMOOTHINGORIENTATION

#ifndef FE_FONTSMOOTHINGORIENTATIONBGR
#define FE_FONTSMOOTHINGORIENTATIONBGR 0x0000
#endif //FE_FONTSMOOTHINGORIENTATIONBGR

#ifndef FE_FONTSMOOTHINGORIENTATIONRGB
#define FE_FONTSMOOTHINGORIENTATIONRGB 0x0001
#endif //FE_FONTSMOOTHINGORIENTATIONRGB

#define MIN_GAMMA 100
#define MAX_GAMMA 220
#define LCDLUTCOUNT (MAX_GAMMA-MIN_GAMMA+1)

static unsigned char* igLUTable[LCDLUTCOUNT];

static unsigned char* getIGTable(int gamma) {
    int i, index;
    double ig;
    char *igTable;

    if (gamma < MIN_GAMMA) {
        gamma = MIN_GAMMA;
    } else if (gamma > MAX_GAMMA) {
        gamma = MAX_GAMMA;
    }

    index = gamma - MIN_GAMMA;

    if (igLUTable[index] != NULL) {
        return igLUTable[index];
    }
    igTable = (unsigned char*)malloc(256);
    if (igTable == NULL) {
      return NULL;
    }
    igTable[0] = 0;
    igTable[255] = 255;
    ig = ((double)gamma)/100.0;

    for (i=1;i<255;i++) {
        igTable[i] = (unsigned char)(pow(((double)i)/255.0, ig)*255);
    }
    igLUTable[index] = igTable;
    return igTable;
}


JNIEXPORT jboolean JNICALL
    Java_sun_font_FileFontStrike_initNative(JNIEnv *env, jclass unused) {

    memset(igLUTable, 0,  LCDLUTCOUNT);

    return JNI_TRUE;
}

#ifndef CLEARTYPE_QUALITY
#define CLEARTYPE_QUALITY 5
#endif

#ifndef CLEARTYPE_NATURAL_QUALITY
#define CLEARTYPE_NATURAL_QUALITY 6
#endif

#define FREE_AND_RETURN \
    if (hDesktopDC != 0 && hWnd != 0) { \
       ReleaseDC(hWnd, hDesktopDC); \
    }\
    if (hMemoryDC != 0) { \
        DeleteObject(hMemoryDC); \
    } \
    if (hBitmap != 0) { \
        DeleteObject(hBitmap); \
    } \
    if (tmpBitmap != 0) { \
        DeleteObject(tmpBitmap); \
    } \
    if (dibImage != NULL) { \
        free(dibImage); \
    } \
    if (glyphInfo != NULL) { \
        free(glyphInfo); \
    } \
    return (jlong)0;
/* end define */

JNIEXPORT jlong JNICALL
Java_sun_font_FileFontStrike__1getGlyphImageFromWindows
(JNIEnv *env, jobject unused,
 jstring fontFamily, jint style, jint size, jint glyphCode, jboolean fm,
 jint fontDataSize) {

    GLYPHMETRICS glyphMetrics;
    LOGFONTW lf;
    BITMAPINFO bmi;
    TEXTMETRIC textMetric;
    RECT rect;
    int bytesWidth, dibBytesWidth, extra, imageSize, dibImageSize;
    unsigned char* dibImage = NULL, *rowPtr, *pixelPtr, *dibPixPtr, *dibRowPtr;
    unsigned char r,g,b;
    unsigned char* igTable;
    GlyphInfo* glyphInfo = NULL;
    int nameLen;
    LPWSTR name;
    HFONT oldFont, hFont;
    MAT2 mat2;
    DWORD actualFontDataSize;

    unsigned short width;
    unsigned short height;
    short advanceX;
    short advanceY;
    int topLeftX;
    int topLeftY;
    int err;
    int bmWidth, bmHeight;
    int x, y;
    HBITMAP hBitmap = NULL, hOrigBM;
    HBITMAP tmpBitmap = NULL;
    int gamma, orient;

    HWND hWnd = NULL;
    HDC hDesktopDC = NULL;
    HDC hMemoryDC = NULL;

    hWnd = GetDesktopWindow();
    hDesktopDC = GetWindowDC(hWnd);
    if (hDesktopDC == NULL) {
        return (jlong)0;
    }
    if (GetDeviceCaps(hDesktopDC, BITSPIXEL) < 15) {
        FREE_AND_RETURN;
    }

    hMemoryDC = CreateCompatibleDC(hDesktopDC);
    if (hMemoryDC == NULL || fontFamily == NULL) {
        FREE_AND_RETURN;
    }
    err = SetMapMode(hMemoryDC, MM_TEXT);
    if (err == 0) {
        FREE_AND_RETURN;
    }

    memset(&lf, 0, sizeof(LOGFONTW));
    lf.lfHeight = -size;
    lf.lfWeight = (style & 1) ? FW_BOLD : FW_NORMAL;
    lf.lfItalic = (style & 2) ? 0xff : 0;
    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfQuality = CLEARTYPE_QUALITY;
    lf.lfOutPrecision = OUT_TT_PRECIS;
    lf.lfClipPrecision = CLIP_DEFAULT_PRECIS;
    lf.lfPitchAndFamily = DEFAULT_PITCH;

    nameLen = (*env)->GetStringLength(env, fontFamily);
    name = (LPWSTR)alloca((nameLen+1)*2);
    if (name == NULL) {
       FREE_AND_RETURN;
    }
    (*env)->GetStringRegion(env, fontFamily, 0, nameLen, name);
    name[nameLen] = '\0';

    if (nameLen < (sizeof(lf.lfFaceName) / sizeof(lf.lfFaceName[0]))) {
        wcscpy(lf.lfFaceName, name);
    } else {
        FREE_AND_RETURN;
    }

    hFont = CreateFontIndirectW(&lf);
    if (hFont == NULL) {
        FREE_AND_RETURN;
    }
    oldFont = SelectObject(hMemoryDC, hFont);

    if (fontDataSize > 0) {
        // GDI doesn't allow to select a specific font file for drawing, we can
        // only check that it picks the file we need by validating font size.
        // If it doesn't match, we cannot proceed, as the same glyph code can
        // correspond to a completely different glyph in the selected font.
        actualFontDataSize = GetFontData(hMemoryDC, 0, 0, NULL, 0);
        if (actualFontDataSize != fontDataSize) {
            FREE_AND_RETURN;
        }
    }

    tmpBitmap = CreateCompatibleBitmap(hDesktopDC, 1, 1);
    if (tmpBitmap == NULL) {
        FREE_AND_RETURN;
    }
    hOrigBM = (HBITMAP)SelectObject(hMemoryDC, tmpBitmap);

    memset(&textMetric, 0, sizeof(TEXTMETRIC));
    err = GetTextMetrics(hMemoryDC, &textMetric);
    if (err == 0) {
        FREE_AND_RETURN;
    }
    memset(&glyphMetrics, 0, sizeof(GLYPHMETRICS));
    memset(&mat2, 0, sizeof(MAT2));
    mat2.eM11.value = 1; mat2.eM22.value = 1;
    err = GetGlyphOutline(hMemoryDC, glyphCode,
                          GGO_METRICS|GGO_GLYPH_INDEX,
                          &glyphMetrics,
                          0, NULL, &mat2);
    if (err == GDI_ERROR) {
        /* Probably no such glyph - ie the font wasn't the one we expected. */
        FREE_AND_RETURN;
    }

    width  = (unsigned short)glyphMetrics.gmBlackBoxX;
    height = (unsigned short)glyphMetrics.gmBlackBoxY;

    /* Don't handle "invisible" glyphs in this code */
    if (width <= 0 || height == 0) {
       FREE_AND_RETURN;
    }

    advanceX = glyphMetrics.gmCellIncX;
    advanceY = glyphMetrics.gmCellIncY;
    topLeftX = glyphMetrics.gmptGlyphOrigin.x;
    topLeftY = glyphMetrics.gmptGlyphOrigin.y;

    /* GetGlyphOutline pre-dates cleartype and I'm not sure that it will
     * account for all pixels touched by the rendering. Need to widen,
     * and also adjust by one the x position at which it is rendered.
     * The extra pixels of width are used as follows :
     * One extra pixel at the left and the right will be needed to absorb
     * the pixels that will be touched by filtering by GDI to compensate
     * for colour fringing.
     * However there seem to be some cases where GDI renders two extra
     * pixels to the right, so we add one additional pixel to the right,
     * and in the code that copies this to the image cache we test for
     * the (rare) cases when this is touched, and if its not reduce the
     * stated image width for the blitting loops.
     * For fractional metrics :
     * One extra pixel at each end to account for sub-pixel positioning used
     * when fractional metrics is on in LCD mode.
     * The pixel at the left is needed so the blitting loop can index into
     * that a byte at a time to more accurately position the glyph.
     * The pixel at the right is needed so that when such indexing happens,
     * the blitting still can use the same width.
     * Consequently the width that is specified for the glyph is one less
     * than that of the actual image.
     * Note that in the FM case as a consequence we need to adjust the
     * position at which GDI renders, and the declared width of the glyph
     * See the if (fm) {} cases in the code.
     * For the non-FM case, we not only save 3 bytes per row, but this
     * prevents apparent glyph overlapping which affects the rendering
     * performance of accelerated pipelines since it adds additional
     * read-back requirements.
     */
    width+=3;
    if (fm) {
        width+=1;
    }
    /* DIB scanline must end on a DWORD boundary. We specify 3 bytes per pixel,
     * so must round up as needed to a multiple of 4 bytes.
     */
    dibBytesWidth = bytesWidth = width*3;
    extra = dibBytesWidth % 4;
    if (extra != 0) {
        dibBytesWidth += (4-extra);
    }
    /* The glyph cache image must be a multiple of 3 bytes wide. */
    extra = bytesWidth % 3;
    if (extra != 0) {
        bytesWidth += (3-extra);
    }
    bmWidth = width;
    bmHeight = height;

    /* Must use desktop DC to create a bitmap of that depth */
    hBitmap = CreateCompatibleBitmap(hDesktopDC, bmWidth, bmHeight);
    if (hBitmap == NULL) {
        FREE_AND_RETURN;
    }
    SelectObject(hMemoryDC, hBitmap);

    /* Fill in black */
    rect.left = 0;
    rect.top = 0;
    rect.right = bmWidth;
    rect.bottom = bmHeight;
    FillRect(hMemoryDC, (LPRECT)&rect, GetStockObject(BLACK_BRUSH));

    /* Set text color to white, background to black. */
    SetBkColor(hMemoryDC, RGB(0,0,0));
    SetTextColor(hMemoryDC, RGB(255,255,255));

    /* adjust rendering position */
    x = -topLeftX+1;
    if (fm) {
        x += 1;
    }
    y = topLeftY - textMetric.tmAscent;
    err = ExtTextOutW(hMemoryDC, x, y, ETO_GLYPH_INDEX|ETO_OPAQUE,
                (LPRECT)&rect, (LPCWSTR)&glyphCode, 1, NULL);
    if (err == 0) {
        FREE_AND_RETURN;
    }

    /* Now get the image into a DIB.
     * MS docs for GetDIBits says the compatible bitmap must not be
     * selected into a DC, so restore the original first.
     */
    SelectObject(hMemoryDC, hOrigBM);
    SelectObject(hMemoryDC, oldFont);
    DeleteObject(hFont);

    memset(&bmi, 0, sizeof(BITMAPINFO));
    bmi.bmiHeader.biSize = sizeof(bmi.bmiHeader);
    bmi.bmiHeader.biWidth = width;
    bmi.bmiHeader.biHeight = -height;
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 24;
    bmi.bmiHeader.biCompression = BI_RGB;

    dibImage = SAFE_SIZE_ARRAY_ALLOC(malloc, dibBytesWidth, height);
    if (dibImage == NULL) {
        FREE_AND_RETURN;
    }
    dibImageSize = dibBytesWidth*height;
    memset(dibImage, 0, dibImageSize);

    err = GetDIBits(hMemoryDC, hBitmap, 0, height, dibImage,
                    &bmi, DIB_RGB_COLORS);

    if (err == 0) {        /* GetDIBits failed. */
        FREE_AND_RETURN;
    }

    err = SystemParametersInfo(SPI_GETFONTSMOOTHINGORIENTATION, 0, &orient, 0);
    if (err == 0) {
        FREE_AND_RETURN;
    }
    err = SystemParametersInfo(SPI_GETFONTSMOOTHINGCONTRAST, 0, &gamma, 0);
    if (err == 0) {
        FREE_AND_RETURN;
    }
    igTable = getIGTable(gamma/10);
    if (igTable == NULL) {
        FREE_AND_RETURN;
    }

    /* Now copy glyph image into a GlyphInfo structure and return it.
     * NB the xadvance calculated here may be overwritten by the caller.
     * 1 is subtracted from the bitmap width to get the glyph width, since
     * that extra "1" was added as padding, so the sub-pixel positioning of
     * fractional metrics could index into it.
     */
    glyphInfo = (GlyphInfo*)SAFE_SIZE_STRUCT_ALLOC(malloc, sizeof(GlyphInfo),
            bytesWidth, height);
    if (glyphInfo == NULL) {
        FREE_AND_RETURN;
    }
    imageSize = bytesWidth*height;
    glyphInfo->cellInfo = NULL;
    glyphInfo->rowBytes = bytesWidth;
    glyphInfo->width = width;
    if (fm) {
        glyphInfo->width -= 1; // must subtract 1
    }
    glyphInfo->height = height;
    glyphInfo->advanceX = advanceX;
    glyphInfo->advanceY = advanceY;
    glyphInfo->topLeftX = (float)(topLeftX-1);
    if (fm) {
        glyphInfo->topLeftX -= 1;
    }
    glyphInfo->topLeftY = (float)-topLeftY;
    glyphInfo->image = (unsigned char*)glyphInfo+sizeof(GlyphInfo);
    memset(glyphInfo->image, 0, imageSize);

    /* DIB 24bpp data is always stored in BGR order, but we usually
     * need this in RGB, so we can't just memcpy and need to swap B and R.
     * Also need to apply inverse gamma adjustment here.
     * We re-use the variable "extra" to see if the last pixel is touched
     * at all. If its not we can reduce the glyph image width. This comes
     * into play in some cases where GDI touches more pixels than accounted
     * for by increasing width by two pixels over the B&W image. Whilst
     * the bytes are in the cache, it doesn't affect rendering performance
     * of the hardware pipelines.
     */
    extra = 0;
    if (fm) {
        extra = 1; // always need it.
    }
    dibRowPtr = dibImage;
    rowPtr = glyphInfo->image;
    for (y=0;y<height;y++) {
        pixelPtr = rowPtr;
        dibPixPtr = dibRowPtr;
        for (x=0;x<width;x++) {
            if (orient == FE_FONTSMOOTHINGORIENTATIONRGB) {
                b = *dibPixPtr++;
                g = *dibPixPtr++;
                r = *dibPixPtr++;
            } else {
                r = *dibPixPtr++;
                g = *dibPixPtr++;
                b = *dibPixPtr++;
            }
            *pixelPtr++ = igTable[r];
            *pixelPtr++ = igTable[g];
            *pixelPtr++ = igTable[b];
            if (!fm && (x==(width-1)) && (r|g|b)) {
                extra = 1;
            }
        }
        dibRowPtr += dibBytesWidth;
        rowPtr  += bytesWidth;
    }
    if (!extra) {
        glyphInfo->width -= 1;
    }

    free(dibImage);
    ReleaseDC(hWnd, hDesktopDC);
    DeleteObject(hMemoryDC);
    DeleteObject(hBitmap);
    DeleteObject(tmpBitmap);

    return ptr_to_jlong(glyphInfo);
}
