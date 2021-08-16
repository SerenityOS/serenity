/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "splashscreen_impl.h"
#include "splashscreen_gfx.h"

#include <gif_lib.h>

#include "sizecalc.h"

#define GIF_TRANSPARENT     0x01
#define GIF_USER_INPUT      0x02
#define GIF_DISPOSE_MASK    0x07
#define GIF_DISPOSE_SHIFT   2

#define GIF_NOT_TRANSPARENT -1

#define GIF_DISPOSE_NONE    0   // No disposal specified. The decoder is
                                // not required to take any action.
#define GIF_DISPOSE_LEAVE   1   // Do not dispose. The graphic is to be left
                                // in place.
#define GIF_DISPOSE_BACKGND 2   // Restore to background color. The area used by the
                                // graphic must be restored to the background color.

#define GIF_DISPOSE_RESTORE 3   // Restore to previous. The decoder is required to
                                // restore the area overwritten by the graphic with
                                // what was there prior to rendering the graphic.

static const char szNetscape20ext[11] = "NETSCAPE2.0";

#define NSEXT_LOOP      0x01    // Loop Count field code

// convert libungif samples to our ones
#define MAKE_QUAD_GIF(c,a) MAKE_QUAD((c).Red, (c).Green, (c).Blue, (unsigned)(a))

/* stdio FILE* and memory input functions for libungif */
int
SplashStreamGifInputFunc(GifFileType * gif, GifByteType * buf, int n)
{
    SplashStream* io = (SplashStream*)gif->UserData;
    int rc = io->read(io, buf, n);
    return rc;
}

/* These macro help to ensure that we only take part of frame that fits into
   logical screen. */

/* Ensure that p belongs to [pmin, pmax) interval. Returns fixed point (if fix is needed) */
#define FIX_POINT(p, pmin, pmax) ( ((p) < (pmin)) ? (pmin) : (((p) > (pmax)) ? (pmax) : (p)))
/* Ensures that line starting at point p does not exceed boundary pmax.
   Returns fixed length (if fix is needed) */
#define FIX_LENGTH(p, len, pmax) ( ((p) + (len)) > (pmax) ? ((pmax) - (p)) : (len))

int
SplashDecodeGif(Splash * splash, GifFileType * gif)
{
    int stride;
    int bufferSize;
    byte_t *pBitmapBits, *pOldBitmapBits;
    int i, j;
    int imageIndex;
    int cx, cy, cw, ch; /* clamped coordinates */
    const int interlacedOffset[] = { 0, 4, 2, 1, 0 };   /* The way Interlaced image should. */
    const int interlacedJumps[] = { 8, 8, 4, 2, 1 };    /* be read - offsets and jumps... */

    if (DGifSlurp(gif) == GIF_ERROR) {
        return 0;
    }

    SplashCleanup(splash);

    if (!SAFE_TO_ALLOC(gif->SWidth, splash->imageFormat.depthBytes)) {
        return 0;
    }
    stride = gif->SWidth * splash->imageFormat.depthBytes;
    if (splash->byteAlignment > 1)
        stride =
            (stride + splash->byteAlignment - 1) & ~(splash->byteAlignment - 1);

    if (!SAFE_TO_ALLOC(gif->SHeight, stride)) {
        return 0;
    }

    if (!SAFE_TO_ALLOC(gif->ImageCount, sizeof(SplashImage*))) {
        return 0;
    }
    bufferSize = stride * gif->SHeight;
    pBitmapBits = (byte_t *) malloc(bufferSize);
    if (!pBitmapBits) {
        return 0;
    }
    pOldBitmapBits = (byte_t *) malloc(bufferSize);
    if (!pOldBitmapBits) {
        free(pBitmapBits);
        return 0;
    }
    memset(pBitmapBits, 0, bufferSize);

    splash->width = gif->SWidth;
    splash->height = gif->SHeight;
    splash->frameCount = gif->ImageCount;
    splash->frames = (SplashImage *)
        SAFE_SIZE_ARRAY_ALLOC(malloc, sizeof(SplashImage), gif->ImageCount);
    if (!splash->frames) {
      free(pBitmapBits);
      free(pOldBitmapBits);
      return 0;
    }
    memset(splash->frames, 0, sizeof(SplashImage) * gif->ImageCount);
    splash->loopCount = 1;

    for (imageIndex = 0; imageIndex < gif->ImageCount; imageIndex++) {
        SavedImage *image = &(gif->SavedImages[imageIndex]);
        GifImageDesc *desc = &(image->ImageDesc);
        ColorMapObject *colorMap =
            desc->ColorMap ? desc->ColorMap : gif->SColorMap;

        int transparentColor = -1;
        int frameDelay = 100;
        int disposeMethod = GIF_DISPOSE_RESTORE;
        int colorCount = 0;
        rgbquad_t colorMapBuf[SPLASH_COLOR_MAP_SIZE];

        cx = FIX_POINT(desc->Left, 0, gif->SWidth);
        cy = FIX_POINT(desc->Top, 0, gif->SHeight);
        cw = FIX_LENGTH(desc->Left, desc->Width, gif->SWidth);
        ch = FIX_LENGTH(desc->Top, desc->Height, gif->SHeight);

        if (colorMap) {
            if (colorMap->ColorCount <= SPLASH_COLOR_MAP_SIZE) {
                colorCount = colorMap->ColorCount;
            } else  {
                colorCount = SPLASH_COLOR_MAP_SIZE;
            }
        }

        /* the code below is loosely based around gif extension processing from win32 libungif sample */

        for (i = 0; i < image->ExtensionBlockCount; i++) {
            byte_t *pExtension = (byte_t *) image->ExtensionBlocks[i].Bytes;
            unsigned size = image->ExtensionBlocks[i].ByteCount;

            switch (image->ExtensionBlocks[i].Function) {
            case GRAPHICS_EXT_FUNC_CODE:
                {
                    int flag = pExtension[0];

                    frameDelay = (((int)pExtension[2]) << 8) | pExtension[1];
                    if (frameDelay < 10)
                        frameDelay = 10;
                    if (flag & GIF_TRANSPARENT) {
                        transparentColor = pExtension[3];
                    } else {
                        transparentColor = GIF_NOT_TRANSPARENT;
                    }
                    disposeMethod =
                        (flag >> GIF_DISPOSE_SHIFT) & GIF_DISPOSE_MASK;
                    break;
                }
            case APPLICATION_EXT_FUNC_CODE:
                {
                    if (size == sizeof(szNetscape20ext)
                        && memcmp(pExtension, szNetscape20ext, size) == 0) {
                        int iSubCode;

                        if (++i >= image->ExtensionBlockCount)
                            break;
                        pExtension = (byte_t *) image->ExtensionBlocks[i].Bytes;
                        if (image->ExtensionBlocks[i].ByteCount != 3)
                            break;
                        iSubCode = pExtension[0] & 0x07;
                        if (iSubCode == NSEXT_LOOP) {
                            splash->loopCount =
                                (pExtension[1] | (((int)pExtension[2]) << 8)) - 1;
                        }
                    }
                    break;
                }
            default:
                break;
            }
        }

        if (colorMap) {
            for (i = 0; i < colorCount; i++) {
                colorMapBuf[i] = MAKE_QUAD_GIF(colorMap->Colors[i], 0xff);
            }
        }
        {

            byte_t *pSrc = image->RasterBits;
            ImageFormat srcFormat;
            ImageRect srcRect, dstRect;
            int pass = 4, npass = 5;

#if GIFLIB_MAJOR < 5
            /* Interlaced gif support is broken in giflib < 5
               so we need to work around this */
            if (desc->Interlace) {
                pass = 0;
                npass = 4;
            }
#endif

            srcFormat.colorMap = colorMapBuf;
            srcFormat.depthBytes = 1;
            srcFormat.byteOrder = BYTE_ORDER_NATIVE;
            srcFormat.transparentColor = transparentColor;
            srcFormat.fixedBits = QUAD_ALPHA_MASK;      // fixed 100% alpha
            srcFormat.premultiplied = 0;

            for (; pass < npass; ++pass) {
                int jump = interlacedJumps[pass];
                int ofs = interlacedOffset[pass];
                /* Number of source lines for current pass */
                int numPassLines = (desc->Height + jump - ofs - 1) / jump;
                /* Number of lines that fits to dest buffer */
                int numLines = (ch + jump - ofs - 1) / jump;

                initRect(&srcRect, 0, 0, desc->Width, numLines, 1,
                    desc->Width, pSrc, &srcFormat);

                if (numLines > 0) {
                    initRect(&dstRect, cx, cy + ofs, cw,
                             numLines , jump, stride, pBitmapBits, &splash->imageFormat);

                    pSrc += convertRect(&srcRect, &dstRect, CVT_ALPHATEST);
                }
                // skip extra source data
                pSrc += (numPassLines - numLines) * srcRect.stride;
            }
        }

        // now dispose of the previous frame correctly

        splash->frames[imageIndex].bitmapBits =
            (rgbquad_t *) malloc(bufferSize); // bufferSize is safe (checked above)
        if (!splash->frames[imageIndex].bitmapBits) {
            free(pBitmapBits);
            free(pOldBitmapBits);
            /* Assuming that callee will take care of splash frames we have already allocated */
            return 0;
        }
        memcpy(splash->frames[imageIndex].bitmapBits, pBitmapBits, bufferSize);

        SplashInitFrameShape(splash, imageIndex);

        splash->frames[imageIndex].delay = frameDelay * 10;     // 100ths of second to milliseconds
        switch (disposeMethod) {
        case GIF_DISPOSE_LEAVE:
            memcpy(pOldBitmapBits, pBitmapBits, bufferSize);
            break;
        case GIF_DISPOSE_NONE:
            break;
        case GIF_DISPOSE_BACKGND:
            {
                ImageRect dstRect;
                rgbquad_t fillColor = 0;                        // 0 is transparent

                if (transparentColor < 0) {
                    fillColor= MAKE_QUAD_GIF(
                        colorMap->Colors[gif->SBackGroundColor], 0xff);
                }
                initRect(&dstRect,
                         cx, cy, cw, ch,
                         1, stride,
                         pBitmapBits, &splash->imageFormat);
                fillRect(fillColor, &dstRect);
            }
            break;
        case GIF_DISPOSE_RESTORE:
            {
                int lineSize = cw * splash->imageFormat.depthBytes;
                if (lineSize > 0) {
                    int lineOffset = cx * splash->imageFormat.depthBytes;
                    int lineIndex = cy * stride + lineOffset;
                    for (j=0; j<ch; j++) {
                        memcpy(pBitmapBits + lineIndex, pOldBitmapBits + lineIndex,
                               lineSize);
                        lineIndex += stride;
                    }
                }
            }
            break;
        }
    }

    free(pBitmapBits);
    free(pOldBitmapBits);

#if GIFLIB_MAJOR > 5 || (GIFLIB_MAJOR == 5 && GIFLIB_MINOR >= 1)
    if (DGifCloseFile(gif, NULL) == GIF_ERROR) {
        return 0;
    }
#else
    DGifCloseFile(gif);
#endif

    return 1;
}

int
SplashDecodeGifStream(Splash * splash, SplashStream * stream)
{
#if GIFLIB_MAJOR >= 5
    GifFileType *gif = DGifOpen((void *) stream, SplashStreamGifInputFunc, NULL);
#else
    GifFileType *gif = DGifOpen((void *) stream, SplashStreamGifInputFunc);
#endif

    if (!gif)
        return 0;
    return SplashDecodeGif(splash, gif);
}
