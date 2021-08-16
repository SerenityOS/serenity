/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "stdhdrs.h"
#include "windows.h"
#include <windowsx.h>
#include <zmouse.h>

#include "GraphicsPrimitiveMgr.h"

#include "awt.h"
#include "awt_BitmapUtil.h"

// Platform-dependent RECT_[EQ | SET | INC_HEIGHT] macros
#include "utility/rect.h"

HBITMAP BitmapUtil::CreateTransparencyMaskFromARGB(int width, int height, int* imageData)
{
    //Scan lines should be aligned to word boundary
    if (!IS_SAFE_SIZE_ADD(width, 15)) return NULL;
    char* buf = SAFE_SIZE_NEW_ARRAY2(char, (width + 15) / 16 * 2, height);
    if (buf == NULL) return NULL;
    int* srcPos = imageData;
    char* bufPos = buf;
    int tmp = 0;
    int cbit = 0x80;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            //cbit is shifted right for every pixel
            //next byte is stored when cbit is zero
            if ((cbit & 0xFF) == 0x00) {
                *bufPos = tmp;
                bufPos++;
                tmp = 0;
                cbit = 0x80;
            }
            unsigned char alpha = (*srcPos >> 0x18) & 0xFF;
            if (alpha == 0x00) {
                tmp |= cbit;
            }
            cbit >>= 1;
            srcPos++;
        }
        //save last word at the end of scan line even if it's incomplete
        *bufPos = tmp;
        bufPos++;
        tmp = 0;
        cbit = 0x80;
        //add word-padding byte if necessary
        if (((bufPos - buf) & 0x01) == 0x01) {
            *bufPos = 0;
            bufPos++;
        }
    }
    HBITMAP bmp = CreateBitmap(width, height, 1, 1, buf);
    delete[] buf;

    return bmp;
}

//BITMAPINFO extended with
typedef struct tagBITMAPINFOEX  {
    BITMAPINFOHEADER bmiHeader;
    DWORD            dwMasks[256];
}   BITMAPINFOEX, *LPBITMAPINFOEX;

/*
 * Creates 32-bit ARGB bitmap from specified RAW data.
 * This function may not work on OS prior to Win95.
 * See MSDN articles for CreateDIBitmap, BITMAPINFOHEADER,
 * BITMAPV4HEADER, BITMAPV5HEADER for additional info.
 */
HBITMAP BitmapUtil::CreateV4BitmapFromARGB(int width, int height, int* imageData)
{
    BITMAPINFOEX    bitmapInfo;
    HDC             hDC;
    char            *bitmapData;
    HBITMAP         hTempBitmap;
    HBITMAP         hBitmap;

    hDC = ::GetDC(::GetDesktopWindow());
    if (!hDC) {
        return NULL;
    }

    memset(&bitmapInfo, 0, sizeof(BITMAPINFOEX));
    bitmapInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bitmapInfo.bmiHeader.biWidth = width;
    bitmapInfo.bmiHeader.biHeight = -height;
    bitmapInfo.bmiHeader.biPlanes = 1;
    bitmapInfo.bmiHeader.biBitCount = 32;
    bitmapInfo.bmiHeader.biCompression = BI_RGB;

    hTempBitmap = ::CreateDIBSection(hDC, (BITMAPINFO*)&(bitmapInfo),
                                    DIB_RGB_COLORS,
                                    (void**)&(bitmapData),
                                    NULL, 0);

    if (!bitmapData) {
        ReleaseDC(::GetDesktopWindow(), hDC);
        return NULL;
    }

    int* src = imageData;
    char* dest = bitmapData;
    for (int i = 0; i < height; i++ ) {
        for (int j = 0; j < width; j++ ) {
            unsigned char alpha = (*src >> 0x18) & 0xFF;
            if (alpha == 0) {
                dest[3] = dest[2] = dest[1] = dest[0] = 0;
            } else {
                dest[3] = alpha;
                dest[2] = (*src >> 0x10) & 0xFF;
                dest[1] = (*src >> 0x08) & 0xFF;
                dest[0] = *src & 0xFF;
            }
            src++;
            dest += 4;
        }
    }

    hBitmap = CreateDIBitmap(hDC,
                             (BITMAPINFOHEADER*)&bitmapInfo,
                             CBM_INIT,
                             (void *)bitmapData,
                             (BITMAPINFO*)&bitmapInfo,
                             DIB_RGB_COLORS);

    ::DeleteObject(hTempBitmap);
    ::ReleaseDC(::GetDesktopWindow(), hDC);
    ::GdiFlush();
    return hBitmap;
}

/*
 * Creates 32-bit premultiplied ARGB bitmap from specified ARGBPre data.
 * This function may not work on OS prior to Win95.
 * See MSDN articles for CreateDIBitmap, BITMAPINFOHEADER,
 * BITMAPV4HEADER, BITMAPV5HEADER for additional info.
 */
HBITMAP BitmapUtil::CreateBitmapFromARGBPre(int width, int height,
                                            int srcStride,
                                            int* imageData)
{
    BITMAPINFOHEADER bmi;
    void *bitmapBits = NULL;

    ZeroMemory(&bmi, sizeof(bmi));
    bmi.biSize = sizeof(bmi);
    bmi.biWidth = width;
    bmi.biHeight = -height;
    bmi.biPlanes = 1;
    bmi.biBitCount = 32;
    bmi.biCompression = BI_RGB;

    HBITMAP hBitmap =
        ::CreateDIBSection(NULL, (BITMAPINFO *) & bmi, DIB_RGB_COLORS,
                           &bitmapBits, NULL, 0);

    if (!bitmapBits) {
        return NULL;
    }

    int dstStride = width * 4;

    if (srcStride == dstStride) {
        memcpy(bitmapBits, (void*)imageData, srcStride * height);
    } else if (height > 0) {
        void *pSrcPixels = (void*)imageData;
        void *pDstPixels = bitmapBits;
        do {
            memcpy(pDstPixels, pSrcPixels, dstStride);
            pSrcPixels = PtrAddBytes(pSrcPixels, srcStride);
            pDstPixels = PtrAddBytes(pDstPixels, dstStride);
        } while (--height > 0);
    }

    return hBitmap;
}

extern "C" {

/**
 * This method is called from the WGL pipeline when it needs to create a bitmap
 * needed to update the layered window.
 */
HBITMAP BitmapUtil_CreateBitmapFromARGBPre(int width, int height,
                                           int srcStride,
                                           int* imageData)
{
    return BitmapUtil::CreateBitmapFromARGBPre(width, height,
                                               srcStride, imageData);

}

}  /* extern "C" */


/**
 * Transforms the given bitmap into an HRGN representing the transparency
 * of the bitmap. The bitmap MUST BE 32bpp. Alpha value == 0 is considered
 * transparent, alpha > 0 - opaque.
 */
HRGN BitmapUtil::BitmapToRgn(HBITMAP hBitmap)
{
    HDC hdc = ::CreateCompatibleDC(NULL);
    ::SelectObject(hdc, hBitmap);

    BITMAPINFOEX bi;
    ::ZeroMemory(&bi, sizeof(bi));

    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);

    BOOL r = ::GetDIBits(hdc, hBitmap, 0, 0, NULL,
            reinterpret_cast<BITMAPINFO*>(&bi), DIB_RGB_COLORS);

    if (!r || bi.bmiHeader.biBitCount != 32)
    {
        ::DeleteDC(hdc);
        return NULL;
    }

    UINT width = bi.bmiHeader.biWidth;
    UINT height = abs(bi.bmiHeader.biHeight);

    BYTE * buf = (BYTE*)safe_Malloc(bi.bmiHeader.biSizeImage);
    if (!buf) {
        ::DeleteDC(hdc);
        return NULL;
    }
    bi.bmiHeader.biHeight = -(INT)height;
    ::GetDIBits(hdc, hBitmap, 0, height, buf,
            reinterpret_cast<BITMAPINFO*>(&bi), DIB_RGB_COLORS);

    /* reserving memory for the worst case */
    if (!IS_SAFE_SIZE_MUL(width / 2 + 1, height)) {
        ::DeleteDC(hdc);
        free(buf);
        return NULL;
    }
    RGNDATA * pRgnData = (RGNDATA *) SAFE_SIZE_STRUCT_ALLOC(safe_Malloc,
            sizeof(RGNDATAHEADER),
            sizeof(RECT), (width / 2 + 1) * height);
    if (!pRgnData) {
        ::DeleteDC(hdc);
        free(buf);
        return NULL;
    }
    RGNDATAHEADER * pRgnHdr = (RGNDATAHEADER *) pRgnData;
    pRgnHdr->dwSize = sizeof(RGNDATAHEADER);
    pRgnHdr->iType = RDH_RECTANGLES;
    pRgnHdr->nRgnSize = 0;
    pRgnHdr->rcBound.top = 0;
    pRgnHdr->rcBound.left = 0;
    pRgnHdr->rcBound.bottom = height;
    pRgnHdr->rcBound.right = width;

    pRgnHdr->nCount = BitmapToYXBandedRectangles(32, width, height, buf,
            (RECT_T *) (((BYTE *) pRgnData) + sizeof(RGNDATAHEADER)));

    HRGN rgn = ::ExtCreateRegion(NULL,
            sizeof(RGNDATAHEADER) + sizeof(RECT_T) * pRgnHdr->nCount,
            pRgnData);

    free(pRgnData);
    ::DeleteDC(hdc);
    free(buf);

    return rgn;
}

/**
 * Makes a copy of the given bitmap. Blends every pixel of the source
 * with the given blendColor and alpha. If alpha == 0, the function
 * simply makes a plain copy of the source without any blending.
 */
HBITMAP BitmapUtil::BlendCopy(HBITMAP hSrcBitmap, COLORREF blendColor,
        BYTE alpha)
{
    HDC hdc = ::CreateCompatibleDC(NULL);
    HBITMAP oldBitmap = (HBITMAP)::SelectObject(hdc, hSrcBitmap);

    BITMAPINFOEX bi;
    ::ZeroMemory(&bi, sizeof(bi));

    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);

    BOOL r = ::GetDIBits(hdc, hSrcBitmap, 0, 0, NULL,
            reinterpret_cast<BITMAPINFO*>(&bi), DIB_RGB_COLORS);

    if (!r || bi.bmiHeader.biBitCount != 32)
    {
        ::DeleteDC(hdc);
        return NULL;
    }

    UINT width = bi.bmiHeader.biWidth;
    UINT height = abs(bi.bmiHeader.biHeight);

    BYTE * buf = (BYTE*)safe_Malloc(bi.bmiHeader.biSizeImage);
    if (!buf) {
        ::DeleteDC(hdc);
        return NULL;
    }
    bi.bmiHeader.biHeight = -(INT)height;
    ::GetDIBits(hdc, hSrcBitmap, 0, height, buf,
            reinterpret_cast<BITMAPINFO*>(&bi), DIB_RGB_COLORS);

    UINT widthBytes = width * bi.bmiHeader.biBitCount / 8;
    UINT alignedWidth = (((widthBytes - 1) / 4) + 1) * 4;
    UINT i, j;

    for (j = 0; j < height; j++) {
        BYTE *pSrc = (BYTE *) buf + j * alignedWidth;
        for (i = 0; i < width; i++, pSrc += 4) {
            // Note: if the current alpha is zero, the other three color
            // components may (theoretically) contain some uninitialized
            // data. The developer does not expect to display them,
            // hence we handle this situation differently.
            if (pSrc[3] == 0) {
                pSrc[0] = GetBValue(blendColor) * alpha / 255;
                pSrc[1] = GetGValue(blendColor) * alpha / 255;
                pSrc[2] = GetRValue(blendColor) * alpha / 255;
                pSrc[3] = alpha;
            } else {
                pSrc[0] = (GetBValue(blendColor) * alpha / 255) +
                    (pSrc[0] * (255 - alpha) / 255);
                pSrc[1] = (GetGValue(blendColor) * alpha / 255) +
                    (pSrc[1] * (255 - alpha) / 255);
                pSrc[2] = (GetRValue(blendColor) * alpha / 255) +
                    (pSrc[2] * (255 - alpha) / 255);
                pSrc[3] = (alpha * alpha / 255) +
                    (pSrc[3] * (255 - alpha) / 255);
            }
        }
    }

    HBITMAP hDstBitmap = ::CreateDIBitmap(hdc,
            reinterpret_cast<BITMAPINFOHEADER*>(&bi),
            CBM_INIT,
            buf,
            reinterpret_cast<BITMAPINFO*>(&bi),
            DIB_RGB_COLORS
            );

    ::SelectObject(hdc, oldBitmap);
    ::DeleteDC(hdc);
    free(buf);

    return hDstBitmap;
}

/**
 * Creates a 32 bit ARGB bitmap. Returns the bitmap handle. The *bitmapBits
 * contains the pointer to the bitmap data or NULL if an error occurred.
 */
HBITMAP BitmapUtil::CreateARGBBitmap(int width, int height, void ** bitmapBitsPtr)
{
    BITMAPINFOHEADER bmi;

    ::ZeroMemory(&bmi, sizeof(bmi));
    bmi.biSize = sizeof(BITMAPINFOHEADER);
    bmi.biWidth = width;
    bmi.biHeight = -height;
    bmi.biPlanes = 1;
    bmi.biBitCount = 32;
    bmi.biCompression = BI_RGB;

    return ::CreateDIBSection(NULL, (BITMAPINFO *) & bmi, DIB_RGB_COLORS,
                bitmapBitsPtr, NULL, 0);
}
