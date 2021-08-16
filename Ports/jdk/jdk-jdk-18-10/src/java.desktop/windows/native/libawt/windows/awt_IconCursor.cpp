/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_IconCursor.h"

/* common code used by awt_Frame.cpp and awt_Cursor.cpp */

HBITMAP create_BMP(HWND hW,int* imageData,int nSS, int nW, int nH)
{
    Bitmapheader    bmhHeader;
    HDC             hDC;
    char            *ptrImageData;
    HBITMAP         hbmpBitmap;
    HBITMAP         hBitmap;

    int             nNumChannels    = 3;

    if (!hW) {
        hW = ::GetDesktopWindow();
    }
    hDC = ::GetDC(hW);
    if (!hDC) {
        return NULL;
    }

    memset(&bmhHeader, 0, sizeof(Bitmapheader));
    bmhHeader.bmiHeader.biSize              = sizeof(BITMAPINFOHEADER);
    bmhHeader.bmiHeader.biWidth             = nW;
    bmhHeader.bmiHeader.biHeight            = -nH;
    bmhHeader.bmiHeader.biPlanes            = 1;

    bmhHeader.bmiHeader.biBitCount          = 24;
    bmhHeader.bmiHeader.biCompression       = BI_RGB;

    hbmpBitmap = ::CreateDIBSection(hDC, (BITMAPINFO*)&(bmhHeader),
                                    DIB_RGB_COLORS,
                                    (void**)&(ptrImageData),
                                    NULL, 0);
    int  *srcPtr = imageData;
    char *dstPtr = ptrImageData;
    if (!dstPtr) {
        ReleaseDC(hW, hDC);
        return NULL;
    }
    for (int nOutern = 0; nOutern < nH; nOutern++ ) {
        for (int nInner = 0; nInner < nSS; nInner++ ) {
            dstPtr[2] = (*srcPtr >> 0x10) & 0xFF;
            dstPtr[1] = (*srcPtr >> 0x08) & 0xFF;
            dstPtr[0] = *srcPtr & 0xFF;

            srcPtr++;
            dstPtr += nNumChannels;
        }
    }

    // convert it into DDB to make CustomCursor work on WIN95
    hBitmap = CreateDIBitmap(hDC,
                             (BITMAPINFOHEADER*)&bmhHeader,
                             CBM_INIT,
                             (void *)ptrImageData,
                             (BITMAPINFO*)&bmhHeader,
                             DIB_RGB_COLORS);

    ::DeleteObject(hbmpBitmap);
    ::ReleaseDC(hW, hDC);
    ::GdiFlush();
    return hBitmap;
}

void destroy_BMP(HBITMAP hBMP)
{
    if (hBMP) {
        ::DeleteObject(hBMP);
    }
}
