/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include <sun_java2d_windows_GDIBlitLoops.h>
#include "gdefs.h"
#include "Trace.h"
#include "GDIWindowSurfaceData.h"

static RGBQUAD *byteGrayPalette = NULL;

extern "C" {

typedef struct tagBitmapheader  {
    BITMAPINFOHEADER bmiHeader;
    union {
        DWORD           dwMasks[3];
        RGBQUAD         palette[256];
    } colors;
} BmiType;

/*
 * Some GDI functions functions will fail if they operate on memory which spans
 * virtual allocations as used by modern garbage collectors (ie ZGC).
 * So if the call to SetDIBitsToDevice fails, we will re-try it on malloced
 * memory rather than the pinned Java heap memory.
 * Once Microsoft fix the GDI bug, the small performance penalty of this retry
 * will be gone.
 */
static void retryingSetDIBitsToDevice(
    HDC              hdc,
    int              xDest,
    int              yDest,
    DWORD            w,
    DWORD            h,
    int              xSrc,
    int              ySrc,
    UINT             StartScan,
    UINT             cLines,
    const VOID       *lpvBits,
    BITMAPINFO       *lpbmi,
    UINT             ColorUse) {

#ifdef DEBUG_PERF
    LARGE_INTEGER    ts1, ts2;
    QueryPerformanceCounter(&ts1);
#endif

    int ret =
        SetDIBitsToDevice(hdc, xDest, yDest, w, h,
                          xSrc, ySrc, StartScan, cLines, lpvBits,
                          lpbmi, ColorUse);

    if (ret != 0 || h == 0) {
#ifdef DEBUG_PERF
         QueryPerformanceCounter(&ts2);
         printf("success time: %zd\n", (ts2.QuadPart-ts1.QuadPart));
#endif
        return;
    }

    size_t size = lpbmi->bmiHeader.biSizeImage;
    void* imageData = NULL;
    try {
        imageData = safe_Malloc(size);
    } catch (std::bad_alloc&) {
    }
    if (imageData == NULL) {
        return;
    }
    memcpy(imageData, lpvBits, size); // this is the most expensive part.
    SetDIBitsToDevice(hdc, xDest, yDest, w, h,
                      xSrc, ySrc, StartScan, cLines, imageData,
                      lpbmi, ColorUse);
    free(imageData);

#ifdef DEBUG_PERF
    QueryPerformanceCounter(&ts2);
    printf("with retry time: %zd\n", (ts2.QuadPart-ts1.QuadPart));
#endif

};

/*
 * Class:     sun_java2d_windows_GDIBlitLoops
 * Method:    nativeBlit
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/SurfaceData;IIIIIIZ)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIBlitLoops_nativeBlit
    (JNIEnv *env, jobject joSelf,
     jobject srcData, jobject dstData,
     jobject clip,
     jint srcx, jint srcy,
     jint dstx, jint dsty,
     jint width, jint height,
     jint rmask, jint gmask, jint bmask,
     jboolean needLut)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIBlitLoops_nativeBlit");

    SurfaceDataRasInfo srcInfo;
    SurfaceDataOps *srcOps = SurfaceData_GetOps(env, srcData);
    GDIWinSDOps *dstOps = GDIWindowSurfaceData_GetOps(env, dstData);
    jint lockFlags;

    srcInfo.bounds.x1 = srcx;
    srcInfo.bounds.y1 = srcy;
    srcInfo.bounds.x2 = srcx + width;
    srcInfo.bounds.y2 = srcy + height;
    if (needLut) {
        lockFlags = (SD_LOCK_READ | SD_LOCK_LUT);
    } else {
        lockFlags = SD_LOCK_READ;
    }
    // This method is used among other things for on-screen copyArea, in which
    // case the source and destination surfaces are the same. It is important
    // to first lock the source and then get the hDC for the destination
    // surface because the same per-thread hDC will be used for both
    // and we need to have the correct clip set to the hDC
    // used with the SetDIBitsToDevice call.
    if (srcOps->Lock(env, srcOps, &srcInfo, lockFlags) != SD_SUCCESS) {
        return;
    }

    SurfaceDataBounds dstBounds = {dstx, dsty, dstx + width, dsty + height};
    // Intersect the source and dest rects. Note that the source blit bounds
    // will be adjusted to the surfaces's bounds if needed.
    SurfaceData_IntersectBlitBounds(&(srcInfo.bounds), &dstBounds,
                                    dstx - srcx, dsty - srcy);

    srcx = srcInfo.bounds.x1;
    srcy = srcInfo.bounds.y1;
    dstx = dstBounds.x1;
    dsty = dstBounds.y1;
    width = srcInfo.bounds.x2 - srcInfo.bounds.x1;
    height = srcInfo.bounds.y2 - srcInfo.bounds.y1;

    if (width > 0 && height > 0)
    {
        BmiType bmi;
        // REMIND: A performance tweak here would be to make some of this
        // data static.  For example, we could have one structure that is
        // always used for ByteGray copies and we only change dynamic data
        // in the structure with every new copy.  Also, we could store
        // structures with Ops or with the Java objects so that surfaces
        // could retain their own DIB info and we would not need to
        // recreate it every time.

        // GetRasInfo implicitly calls GetPrimitiveArrayCritical
        // and since GetDC uses JNI it needs to be called first.
        HDC hDC = dstOps->GetDC(env, dstOps, 0, NULL, clip, NULL, 0);
        if (hDC == NULL) {
            SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
            return;
        }
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        if (srcInfo.rasBase == NULL) {
            dstOps->ReleaseDC(env, dstOps, hDC);
            SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
            return;
        }
        void *rasBase = ((char *)srcInfo.rasBase) + srcInfo.scanStride * srcy +
                        srcInfo.pixelStride * srcx;

        // If scanlines are DWORD-aligned (scanStride is a multiple of 4),
        // then we can do the work much faster.  This is due to a constraint
        // in the way DIBs are structured and parsed by GDI
        jboolean fastBlt = ((srcInfo.scanStride & 0x03) == 0);
        bmi.bmiHeader.biSize = sizeof(bmi.bmiHeader);
        bmi.bmiHeader.biWidth = srcInfo.scanStride/srcInfo.pixelStride;
        // fastBlt copies whole image in one call; else copy line-by-line
        LONG dwHeight = srcInfo.bounds.y2 - srcInfo.bounds.y1;
        bmi.bmiHeader.biHeight = (fastBlt) ? -dwHeight : -1;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = (WORD)srcInfo.pixelStride * 8;
        // 1,3,4 byte use BI_RGB, 2 byte use BI_BITFIELD...
        // 4 byte _can_ use BI_BITFIELD, but this seems to cause a performance
        // penalty.  Since we only ever have one format (xrgb) for 32-bit
        // images that enter this function, just use BI_RGB.
        // Could do BI_RGB for 2-byte 555 format, but no perceived
        // performance benefit.
        bmi.bmiHeader.biCompression = (srcInfo.pixelStride != 2)
                ? BI_RGB : BI_BITFIELDS;
        bmi.bmiHeader.biSizeImage = (bmi.bmiHeader.biWidth * dwHeight *
                                     srcInfo.pixelStride);
        bmi.bmiHeader.biXPelsPerMeter = 0;
        bmi.bmiHeader.biYPelsPerMeter = 0;
        bmi.bmiHeader.biClrUsed = 0;
        bmi.bmiHeader.biClrImportant = 0;
        if (srcInfo.pixelStride == 1) {
            // Copy palette info into bitmap for 8-bit image
            if (needLut) {
                memcpy(bmi.colors.palette, srcInfo.lutBase, srcInfo.lutSize * sizeof(RGBQUAD));
                if (srcInfo.lutSize != 256) {
                    bmi.bmiHeader.biClrUsed = srcInfo.lutSize;
                }
            } else {
                // If no LUT needed, must be ByteGray src.  If we have not
                // yet created the byteGrayPalette, create it now and copy
                // it into our temporary bmi structure.
                // REMIND: byteGrayPalette is a leak since we do not have
                // a mechanism to free it up.  This should be fine, since it
                // is only 256 bytes for any process and only gets malloc'd
                // when using ByteGray surfaces.  Eventually, we should use
                // the new Disposer mechanism to delete this native memory.
                if (byteGrayPalette == NULL) {
                    // assert (256 * sizeof(RGBQUAD)) <= SIZE_MAX
                    byteGrayPalette = (RGBQUAD *)safe_Malloc(256 * sizeof(RGBQUAD));
                    for (int i = 0; i < 256; ++i) {
                        byteGrayPalette[i].rgbRed = i;
                        byteGrayPalette[i].rgbGreen = i;
                        byteGrayPalette[i].rgbBlue = i;
                    }
                }
                memcpy(bmi.colors.palette, byteGrayPalette, 256 * sizeof(RGBQUAD));
            }
        } else if (srcInfo.pixelStride == 2) {
            // For 16-bit case, init the masks for the pixel depth
            bmi.colors.dwMasks[0] = rmask;
            bmi.colors.dwMasks[1] = gmask;
            bmi.colors.dwMasks[2] = bmask;
        }

        if (fastBlt) {
            // Window could go away at any time, leaving bits on the screen
            // from this GDI call, so make sure window still exists
            if (::IsWindowVisible(dstOps->window)) {
                // Could also call StretchDIBits.  Testing showed slight
                // performance advantage of SetDIBits instead, so since we
                // have no need of scaling, might as well use SetDIBits.
                retryingSetDIBitsToDevice(hDC, dstx, dsty, width, height,
                    0, 0, 0, height, rasBase,
                    (BITMAPINFO*)&bmi, DIB_RGB_COLORS);
            }
        } else {
            // Source scanlines not DWORD-aligned - copy each scanline individually
            for (int i = 0; i < height; i += 1) {
                if (::IsWindowVisible(dstOps->window)) {
                    retryingSetDIBitsToDevice(hDC, dstx, dsty+i, width, 1,
                        0, 0, 0, 1, rasBase,
                        (BITMAPINFO*)&bmi, DIB_RGB_COLORS);
                    rasBase = (void*)((char*)rasBase + srcInfo.scanStride);
                } else {
                    break;
                }
            }
        }
        dstOps->ReleaseDC(env, dstOps, hDC);
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);

    return;
}

} // extern "C"
