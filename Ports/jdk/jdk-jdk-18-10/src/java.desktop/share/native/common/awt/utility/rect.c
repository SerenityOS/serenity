/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

#include "utility/rect.h"

#if defined(__cplusplus)
extern "C" {
#endif

/**
 * bitsPerPixel must be 32 for now.
 * outBuf must be large enough to conatin all the rectangles.
 */
int BitmapToYXBandedRectangles(int bitsPerPixel, int width, int height, unsigned char * buf, RECT_T * outBuf)
{
    //XXX: we might want to reuse the code in the splashscreen library,
    // though we'd have to deal with the ALPHA_THRESHOLD and different
    // image formats in this case.
    int widthBytes = width * bitsPerPixel / 8;
    int alignedWidth = (((widthBytes - 1) / 4) + 1) * 4;

    RECT_T * out = outBuf;

    RECT_T *pPrevLine = NULL, *pFirst = out, *pThis = pFirst;
    int i, j, i0;
    int length;

    for (j = 0; j < height; j++) {
        /* generate data for a scanline */

        unsigned char *pSrc = (unsigned char *) buf + j * alignedWidth;
        RECT_T *pLine = pThis;

        i = 0;

        do {
            // pSrc[0,1,2] == B,G,R; pSrc[3] == Alpha
            while (i < width && !pSrc[3]) {
                pSrc += 4;
                ++i;
            }
            if (i >= width)
                break;
            i0 = i;
            while (i < width && pSrc[3]) {
                pSrc += 4;
                ++i;
            }
            RECT_SET(*pThis, i0, j, i - i0, 1);
            ++pThis;
        } while (i < width);

        /*  check if the previous scanline is exactly the same, merge if so
            (this is the only optimization we can use for YXBanded rectangles,
            and win32 supports YXBanded only */

        length = pThis - pLine;
        if (pPrevLine && pLine - pPrevLine == length) {
            for (i = 0; i < length && RECT_EQ_X(pPrevLine[i], pLine[i]); ++i) {
            }
            if (i == pLine - pPrevLine) {
                // do merge
                for (i = 0; i < length; i++) {
                    RECT_INC_HEIGHT(pPrevLine[i]);
                }
                pThis = pLine;
                continue;
            }
        }
        /* or else use the generated scanline */

        pPrevLine = pLine;
    }

    return pThis - pFirst;
}

#if defined(__cplusplus)
}
#endif
