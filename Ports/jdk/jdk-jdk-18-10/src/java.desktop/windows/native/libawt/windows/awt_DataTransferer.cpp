/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "awt_DataTransferer.h"
#include "awt_DnDDT.h"
#include "awt_TextComponent.h"
#include <shlobj.h>
#include <shellapi.h>
#include <sun_awt_windows_WDataTransferer.h>

#include "locale_str.h"

#define GALLOCFLG (GMEM_DDESHARE | GMEM_MOVEABLE | GMEM_ZEROINIT)
#define WIN_TO_JAVA_PIXEL(r, g, b) (0xFF000000 | (r) << 16 | (g) << 8  | (b) << 0)

DECLARE_JAVA_CLASS(dataTransfererClazz, "sun/awt/datatransfer/DataTransferer");

jobject
AwtDataTransferer::GetDataTransferer(JNIEnv* env) {
    DECLARE_STATIC_OBJECT_JAVA_METHOD(getInstanceMethodID, dataTransfererClazz,
                                      "getInstance",
                                      "()Lsun/awt/datatransfer/DataTransferer;");
    return env->CallStaticObjectMethod(clazz, getInstanceMethodID);
}

jbyteArray
AwtDataTransferer::ConvertData(JNIEnv* env, jobject source, jobject contents,
                               jlong format, jobject formatMap) {
    jobject transferer = GetDataTransferer(env);

    if (!JNU_IsNull(env, transferer)) {
        jbyteArray ret = NULL;
        DECLARE_OBJECT_JAVA_METHOD(convertDataMethodID, dataTransfererClazz,
                                   "convertData",
                                   "(Ljava/lang/Object;Ljava/awt/datatransfer/Transferable;JLjava/util/Map;Z)[B");

        ret = (jbyteArray)env->CallObjectMethod(transferer, convertDataMethodID,
                                                source, contents, format,
                                                formatMap, AwtToolkit::IsMainThread());

        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        env->DeleteLocalRef(transferer);

        return ret;
    } else {
        return NULL;
    }
}

jobject
AwtDataTransferer::ConcatData(JNIEnv* env, jobject obj1, jobject obj2) {
    jobject transferer = GetDataTransferer(env);

    if (!JNU_IsNull(env, transferer)) {
        jobject ret = NULL;
        DECLARE_OBJECT_JAVA_METHOD(concatDataMethodID, dataTransfererClazz,
                                   "concatData",
                                   "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        ret = env->CallObjectMethod(transferer, concatDataMethodID, obj1, obj2);

        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        env->DeleteLocalRef(transferer);

        return ret;
    } else {
        return NULL;
    }
}

/**
 * This routine retrieves palette entries from enhanced metafile or
 * a logical color palette, builds appropriate LOGPALETTE structure,
 * writes it into a created Java byte array and returns a local
 * reference to the array.
 * This routine is used for image data transfer.
 *
 * @param hGdiObj - a handle to the GDI object to retrieve palette entries from,
 *        it can be a handle to either a logical color palette (OBJ_PAL type)
 *        or an enhanced metafile (OBJ_ENHMETAFILE). If it is neither of these
 *        types the routine fails(see bFailSafe).
 * @param dwGdiObjType - a type of the passed GDI object. It should be specified
 *        if the type of the passed GDI object is known to the caller. Otherwise
 *        pass 0.
 * @param bFailSafe - if FALSE, the routine will return NULL in case of failure,
 *        otherwise it will return an array with empty LOGPALETTE structure
 *        in case of failure.
 * @return a local reference to Java byte array which contains LOGPALETTE
 *        structure which defines a logical color palette or a palette of
 *        an enhanced metafile.
 */
jbyteArray
AwtDataTransferer::GetPaletteBytes(HGDIOBJ hGdiObj, DWORD dwGdiObjType,
                                   BOOL bFailSafe) {

    if (hGdiObj == NULL) {
        dwGdiObjType = 0;
    } else if (dwGdiObjType == 0) {
        dwGdiObjType = ::GetObjectType(hGdiObj);
    } else {
        DASSERT(::GetObjectType(hGdiObj) == dwGdiObjType);
    }

    if (!bFailSafe && dwGdiObjType == 0) {
        return NULL;
    }

    UINT nEntries = 0;

    switch (dwGdiObjType) {
    case OBJ_PAL:
        nEntries =
            ::GetPaletteEntries((HPALETTE)hGdiObj, 0, 0, NULL);
        break;
    case OBJ_ENHMETAFILE:
        nEntries =
            ::GetEnhMetaFilePaletteEntries((HENHMETAFILE)hGdiObj, 0, NULL);
        break;
    }

    if (!bFailSafe && (nEntries == 0 || nEntries == GDI_ERROR)) {
        return NULL;
    }

    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jsize size = sizeof(LOGPALETTE) + nEntries * sizeof(PALETTEENTRY);

    jbyteArray paletteBytes = env->NewByteArray(size);
    if (JNU_IsNull(env, paletteBytes)) {
        throw std::bad_alloc();
    }

    LOGPALETTE* pLogPalette =
        (LOGPALETTE*)env->GetPrimitiveArrayCritical(paletteBytes, NULL);
    PALETTEENTRY* pPalEntries = (PALETTEENTRY*)pLogPalette->palPalEntry;

    pLogPalette->palVersion = 0x300;
    pLogPalette->palNumEntries = nEntries;

    switch (dwGdiObjType) {
    case OBJ_PAL:
        VERIFY(::GetPaletteEntries((HPALETTE)hGdiObj, 0, nEntries,
                                   pPalEntries) == nEntries);
        break;
    case OBJ_ENHMETAFILE:
        VERIFY(::GetEnhMetaFilePaletteEntries((HENHMETAFILE)hGdiObj, nEntries,
                                              pPalEntries) == nEntries);
        break;
    }

    env->ReleasePrimitiveArrayCritical(paletteBytes, pLogPalette, 0);

    return paletteBytes;
}

jbyteArray
AwtDataTransferer::LCIDToTextEncoding(JNIEnv *env, LCID lcid) {
    LANGID langID = LANGIDFROMLCID(lcid);
    const char *encoding = getEncodingFromLangID(langID);

    // Warning C4244.
    // Cast SIZE_T (__int64 on 64-bit/unsigned int on 32-bit)
    // to jsize (long).
    // We assume that the encoding name length cannot exceed INT_MAX.
    jsize length = (jsize)strlen(encoding);

    jbyteArray retval = env->NewByteArray(length);
    if (retval == NULL) {
        throw std::bad_alloc();
    }
    env->SetByteArrayRegion(retval, 0, length, (jbyte *)encoding);
    free((void *)encoding);
    return retval;
}

static VOID CALLBACK
IdleFunc() {
    /*
     * Fix for 4485987 and 4669873.
     * If IdleFunc is a noop, the secondary message pump occasionally occupies
     * all processor time and causes drag freezes. GetQueueStatus is needed to
     * mark all messages that are currently in the queue as old, otherwise
     * WaitMessage will return immediatelly as we selectively get messages from
     * the queue.
     */
    ::WaitMessage();
    ::GetQueueStatus(QS_ALLINPUT);
}

static BOOL CALLBACK
PeekMessageFunc(MSG& msg) {
    return ::PeekMessage(&msg, NULL, WM_QUIT, WM_QUIT, PM_REMOVE) ||
           ::PeekMessage(&msg, NULL, WM_AWT_INVOKE_METHOD, WM_AWT_INVOKE_METHOD, PM_REMOVE) ||
           ::PeekMessage(&msg, NULL, WM_PAINT, WM_PAINT, PM_REMOVE);
}

void
AwtDataTransferer::SecondaryMessageLoop() {
    DASSERT(AwtToolkit::MainThread() == ::GetCurrentThreadId());

    AwtToolkit::GetInstance().MessageLoop(IdleFunc,
                                          PeekMessageFunc);
}

extern "C" {

/*
 * Class:     sun_awt_datatransfer_DataTransferer
 * Method:    draqQueryFile
 * Signature: ([B)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_sun_awt_windows_WDataTransferer_dragQueryFile
    (JNIEnv *env, jobject obj, jbyteArray bytes)
{
    TRY;

    /*
     * Fix for the BugTraq ID 4327064 - inter-jvm DnD crashes the droping jvm.
     * On Win9X DragQueryFile() doesn't accept a pointer to the local help as the first
     * argument, so we should dump the bits into global memory.
     */
    UINT size = env->GetArrayLength(bytes);
    HGLOBAL hglobal = NULL;
    jbyte *bBytes = NULL;
    HDROP hdrop = NULL;
    LPTSTR buffer = NULL;

    hglobal = ::GlobalAlloc(GALLOCFLG, size);

    if (hglobal == NULL) {
        throw std::bad_alloc();
    }

    try {

        bBytes = (jbyte*)::GlobalLock(hglobal);
        env->GetByteArrayRegion(bytes, 0, size, bBytes);

        hdrop = (HDROP)bBytes;

        UINT nFilenames = ::DragQueryFile(hdrop, 0xFFFFFFFF, NULL, 0);

        jclass str_clazz = env->FindClass("java/lang/String");
        DASSERT(str_clazz != NULL);
        if (str_clazz == NULL) {
           throw std::bad_alloc();
        }
        jobjectArray filenames = env->NewObjectArray(nFilenames, str_clazz,
                                                     NULL);
        if (filenames == NULL) {
            throw std::bad_alloc();
        }

        UINT bufsize = 512; // in characters, not in bytes
        buffer = (LPTSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, bufsize, sizeof(TCHAR));

        for (UINT i = 0; i < nFilenames; i++) {
            UINT size = ::DragQueryFile(hdrop, i, NULL, 0);
            if (size > bufsize) {
                bufsize = size;
                buffer = (LPTSTR)SAFE_SIZE_ARRAY_REALLOC(safe_Realloc, buffer, bufsize, sizeof(TCHAR));
            }
            ::DragQueryFile(hdrop, i, buffer, bufsize);

            jstring name = JNU_NewStringPlatform(env, buffer);
            if (name == NULL) {
                throw std::bad_alloc();
            }

            env->SetObjectArrayElement(filenames, i, name);
        }

        free(buffer);
        ::GlobalUnlock(hglobal);
        ::GlobalFree(hglobal);
        return filenames;

    } catch (std::bad_alloc&) {
        free(buffer);
        ::GlobalUnlock(hglobal);
        ::GlobalFree(hglobal);
        throw;
    }

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WDataTransferer
 * Method:    platformImageBytesToImageData
 * Signature: ([BI)[I
 */
JNIEXPORT jintArray JNICALL
Java_sun_awt_windows_WDataTransferer_platformImageBytesToImageData(
    JNIEnv *env, jobject self, jbyteArray bytes, jlong format) {

    TRY;

    HDC hdc = NULL;

    LOGPALETTE* pLogPalette = NULL;
    WORD uPaletteEntries = 0;
    SIZE_T uOffset = 0;
    HPALETTE hPalette = NULL;
    HPALETTE hOldPalette = NULL;

    BITMAPINFO* pSrcBmi = NULL;
    BITMAPINFOHEADER* pSrcBmih = NULL;
    LPVOID pSrcBits = NULL;
    BITMAPINFO* pDstBmi = NULL;
    BITMAPINFOHEADER* pDstBmih = NULL;
    LPVOID pDstBits = NULL;

    LPBYTE lpEnhMetaFileBits = NULL;
    HENHMETAFILE hEnhMetaFile = NULL;

    HBITMAP hDibSection = NULL;
    HBITMAP hOldBitmap = NULL;
    jintArray buffer = NULL;
    LONG width = 0;
    LONG height = 0;
    int numPixels = 0;

    if (JNU_IsNull(env, bytes)) {
        return NULL;
    }

    jsize size = env->GetArrayLength(bytes);
    if (size == 0) {
        return NULL;
    }

    jbyte* bBytes = (jbyte*)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, size, sizeof(jbyte));

    try {

        env->GetByteArrayRegion(bytes, 0, size, bBytes);

        pLogPalette = (LOGPALETTE*)bBytes;
        uPaletteEntries = pLogPalette->palNumEntries;
        uOffset = sizeof(LOGPALETTE) + uPaletteEntries * sizeof(PALETTEENTRY);
        DASSERT(uOffset < (SIZE_T)size);

        if (uPaletteEntries == 0) {
            pLogPalette = NULL;
        }

        hdc = ::CreateCompatibleDC(NULL);
        if (hdc == NULL) {
            free(bBytes);
            return NULL;
        }

        switch (format) {
        case CF_DIB:

            pSrcBmi = (BITMAPINFO*)((LPSTR)bBytes + uOffset);
            pSrcBmih = &pSrcBmi->bmiHeader;

            width = pSrcBmih->biWidth;
            height = abs(pSrcBmih->biHeight);

            {
                DWORD nColorEntries = 0;

                switch (pSrcBmih->biBitCount) {
                case  0: nColorEntries = 0; break;
                case  1: nColorEntries = 2; break;
                case  4:
                case  8:
                    nColorEntries = (pSrcBmih->biClrUsed != 0) ?
                        pSrcBmih->biClrUsed : (1 << pSrcBmih->biBitCount);
                    break;
                case 16:
                case 24:
                case 32:
                    nColorEntries = pSrcBmih->biClrUsed;
                    // If biBitCount is 16 or 32 and biCompression is
                    // BI_BITFIELDS the color table will be prefixed with
                    // three DWORD color masks.
                    if (pSrcBmih->biCompression == BI_BITFIELDS &&
                        (pSrcBmih->biBitCount == 16 ||
                         pSrcBmih->biBitCount == 32)) {
                        nColorEntries += 3;
                    }
                    break;
                default:
                    // The header is probably corrupted.
                    // Fail immediatelly to avoid memory access violation.
                    free(bBytes);
                    ::DeleteDC(hdc);
                    return NULL;
                }

                pSrcBits = (LPSTR)pSrcBmi + pSrcBmih->biSize
                    + nColorEntries * sizeof(RGBQUAD);
            }
            break;
        case CF_ENHMETAFILE:
        case CF_METAFILEPICT:
            lpEnhMetaFileBits = (BYTE*)bBytes + uOffset;
            // Warning C4244. size is jsize, uOffset is SIZE_T.
            // We assert that size > uOffset, so it is safe to cast to jsize.
            hEnhMetaFile = ::SetEnhMetaFileBits(size - (jsize)uOffset,
                                                lpEnhMetaFileBits);
            DASSERT(hEnhMetaFile != NULL);

            {
                UINT uHeaderSize =
                    ::GetEnhMetaFileHeader(hEnhMetaFile, 0, NULL);
                DASSERT(uHeaderSize != 0);
                ENHMETAHEADER* lpemh = (ENHMETAHEADER*)safe_Malloc(uHeaderSize);
                VERIFY(::GetEnhMetaFileHeader(hEnhMetaFile, uHeaderSize,
                                              lpemh) == uHeaderSize);
                LPRECTL lpFrame = &lpemh->rclFrame;
                POINT p = { abs(lpFrame->right - lpFrame->left),
                            abs(lpFrame->bottom - lpFrame->top) };
                VERIFY(::SaveDC(hdc));
                VERIFY(::SetMapMode(hdc, MM_HIMETRIC));
                VERIFY(::LPtoDP(hdc, &p, 1));
                VERIFY(::RestoreDC(hdc, -1));
                width = p.x;
                height = -p.y;

                free(lpemh);
            }
            break;
        default:
            DASSERT(FALSE); // Other formats are not supported yet.
            free(bBytes);
            ::DeleteDC(hdc);
            return NULL;
        }

        // JNI doesn't allow to store more than INT_MAX in a single array.
        // We report conversion failure in this case.
        if (width * height > INT_MAX) {
            free(bBytes);
            ::DeleteDC(hdc);
            return NULL;
        }

        numPixels = width * height;

        if (pLogPalette != NULL) {
            hPalette = ::CreatePalette(pLogPalette);
            if (hPalette == NULL) {
                free(bBytes);
                ::DeleteDC(hdc);
                return NULL;
            }
            hOldPalette = ::SelectPalette(hdc, hPalette, FALSE);
            ::RealizePalette(hdc);
        }

        // allocate memory for BITMAPINFO
        pDstBmi = (BITMAPINFO *)safe_Calloc(1, sizeof(BITMAPINFO));
        pDstBmih = &pDstBmi->bmiHeader;

        static const int BITS_PER_PIXEL = 32;

        // prepare BITMAPINFO for a 32-bit RGB bitmap
        pDstBmih->biSize = sizeof(BITMAPINFOHEADER);
        pDstBmih->biWidth = width;
        pDstBmih->biHeight = -height; // negative height means a top-down DIB
        pDstBmih->biPlanes = 1;
        pDstBmih->biBitCount = BITS_PER_PIXEL;
        pDstBmih->biCompression = BI_RGB;
        // NOTE: MSDN says that biSizeImage may be set to 0 for BI_RGB bitmaps,
        // but this causes CreateDIBSection to allocate zero-size memory block
        // for DIB data. It works okay when biSizeImage is explicitly specified.
        pDstBmih->biSizeImage = width * height * (BITS_PER_PIXEL >> 3);

        hDibSection = ::CreateDIBSection(hdc, (BITMAPINFO*)pDstBmi,
                                         DIB_RGB_COLORS, &pDstBits,
                                         NULL, 0);

        if (hDibSection == NULL) {
            free(pDstBmi); pDstBmi = NULL;
            if (hPalette != NULL) {
                VERIFY(::SelectPalette(hdc, hOldPalette, FALSE) != NULL);
                hOldPalette = NULL;
                VERIFY(::DeleteObject(hPalette)); hPalette = NULL;
            }
            VERIFY(::DeleteDC(hdc)); hdc = NULL;
            free(bBytes); bBytes = NULL;

            JNU_ThrowIOException(env, "failed to get drop data");
            return NULL;
        }

        hOldBitmap = (HBITMAP)::SelectObject(hdc, hDibSection);
        DASSERT(hOldBitmap != NULL);

        switch (format) {
        case CF_DIB:
            VERIFY(::StretchDIBits(hdc,
                                   0, 0, width, height,
                                   0, 0, width, height,
                                   pSrcBits, pSrcBmi,
                                   DIB_RGB_COLORS, SRCCOPY) != GDI_ERROR);
            break;
        case CF_ENHMETAFILE:
        case CF_METAFILEPICT: {
            RECT rect = { 0, 0, width, height };

            VERIFY(::PlayEnhMetaFile(hdc, hEnhMetaFile, &rect));
            VERIFY(::DeleteEnhMetaFile(hEnhMetaFile)); hEnhMetaFile = NULL;
            break;
        }
        default:
            // Other formats are not supported yet.
            DASSERT(FALSE);
            break;
        }

        // convert Win32 pixel format (BGRX) to Java format (ARGB)
        DASSERT(sizeof(jint) == sizeof(RGBQUAD));
        RGBQUAD* prgbq = (RGBQUAD*)pDstBits;
        for(int nPixel = 0; nPixel < numPixels; nPixel++, prgbq++) {
            jint jpixel = WIN_TO_JAVA_PIXEL(prgbq->rgbRed,
                                            prgbq->rgbGreen,
                                            prgbq->rgbBlue);
            // stuff the 32-bit pixel back into the 32-bit RGBQUAD
            *prgbq = *((RGBQUAD*)(&jpixel));
        }

        buffer = env->NewIntArray(numPixels + 2);
        if (buffer == NULL) {
            throw std::bad_alloc();
        }

        // copy pixels into Java array
        env->SetIntArrayRegion(buffer, 0, numPixels, (jint*)pDstBits);

        // copy dimensions into Java array
        env->SetIntArrayRegion(buffer, numPixels, 1, (jint*)&width);
        env->SetIntArrayRegion(buffer, numPixels + 1, 1, (jint*)&height);

        VERIFY(::SelectObject(hdc, hOldBitmap) != NULL); hOldBitmap = NULL;
        VERIFY(::DeleteObject(hDibSection)); hDibSection = NULL;
        free(pDstBmi); pDstBmi = NULL;
        if (hPalette != NULL) {
            VERIFY(::SelectPalette(hdc, hOldPalette, FALSE) != NULL);
            hOldPalette = NULL;
            VERIFY(::DeleteObject(hPalette)); hPalette = NULL;
        }
        VERIFY(::DeleteDC(hdc)); hdc = NULL;
        free(bBytes); bBytes = NULL;
    } catch (...) {
        if (hdc != NULL && hOldBitmap != NULL) {
            VERIFY(::SelectObject(hdc, hOldBitmap) != NULL); hOldBitmap = NULL;
        }
        if (hDibSection != NULL) {
            VERIFY(::DeleteObject(hDibSection)); hDibSection = NULL;
        }
        if (pDstBmi != NULL) {
            free(pDstBmi); pDstBmi = NULL;
        }
        if (hPalette != NULL) {
            if (hdc != NULL) {
                VERIFY(::SelectPalette(hdc, hOldPalette, FALSE) != NULL);
                hOldPalette = NULL;
            }
            VERIFY(::DeleteObject(hPalette)); hPalette = NULL;
        }
        if (hdc != NULL) {
            VERIFY(::DeleteDC(hdc)); hdc = NULL;
        }
        if (hEnhMetaFile != NULL) {
            VERIFY(::DeleteEnhMetaFile(hEnhMetaFile)); hEnhMetaFile = NULL;
        }
        if (bBytes != NULL) {
            free(bBytes); bBytes = NULL;
        }
        throw;
    }

    return buffer;

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WDataTransferer
 * Method:    imageDataToPlatformImageBytes
 * Signature: ([BIII)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_awt_windows_WDataTransferer_imageDataToPlatformImageBytes(JNIEnv *env,
                                               jobject self, jbyteArray imageData,
                                               jint width, jint height,
                                               jlong format) {

    TRY;

    if (JNU_IsNull(env, imageData)) {
        return NULL;
    }

    UINT size = env->GetArrayLength(imageData);
    if (size == 0) {
        return NULL;
    }

    // In the passed imageData array all lines are padded with zeroes except for
    // the last one, so we have to add one pad size here.
    int mod = (width * 3) % 4;
    int pad = mod > 0 ? 4 - mod : 0;
    int nBytes = sizeof(BITMAPINFO) + size + pad;
    BITMAPINFO* pinfo = (BITMAPINFO*)safe_Calloc(1, nBytes);

    static const int BITS_PER_PIXEL = 24;

    // prepare BITMAPINFO for a 24-bit BGR bitmap
    pinfo->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    pinfo->bmiHeader.biWidth = width;
    pinfo->bmiHeader.biHeight = height; // positive height means a bottom-up DIB
    pinfo->bmiHeader.biPlanes = 1;
    pinfo->bmiHeader.biBitCount = BITS_PER_PIXEL;
    pinfo->bmiHeader.biCompression = BI_RGB;
    // NOTE: MSDN says that biSizeImage may be set to 0 for BI_RGB bitmaps,
    // but some programs (e.g. Imaging for Windows NT by Wang Laboratories)
    // don't handle such DIBs correctly, so we specify the size explicitly.
    pinfo->bmiHeader.biSizeImage = size + pad;

    jbyte *array = (jbyte*)((LPSTR)pinfo + sizeof(BITMAPINFOHEADER));
    env->GetByteArrayRegion(imageData, 0, size, array);
    HRESULT hr = S_OK;

    jbyteArray bytes = NULL;
    switch (format) {
    case CF_DIB:
        bytes = env->NewByteArray(nBytes);
        if( NULL == bytes ) {
            hr = E_OUTOFMEMORY;
        } else {
            env->SetByteArrayRegion(bytes, 0, nBytes, (jbyte*)pinfo);
        }
        break;
    case CF_ENHMETAFILE:
    {
        HDC hdc = ::GetDC(NULL);
        if( NULL == hdc) {
            hr = HRESULT_FROM_WIN32(::GetLastError());
        } else {
            POINT p = { width, height };
            //We are trying to support context-independent metafile.
            //To implement it we have to select correct MM_HIMETRIC map mode.
            VERIFY(::SetMapMode(hdc, MM_HIMETRIC));
            VERIFY(::DPtoLP(hdc, &p, 1));
            //In accordance with CreateEnhMetaFile documentation the rectangle have to
            //be normal (left <= right, top <= bottom)
            RECT r = { min(0, p.x), min(0, p.y), max(0, p.x), max(0, p.y) };
            //Due to inversed row order in source bitmap the destination
            //height have to be negative.
            HDC hemfdc = ::CreateEnhMetaFile(NULL, NULL, &r, NULL);
            if( NULL == hemfdc) {
                hr = HRESULT_FROM_WIN32(::GetLastError());
            } else {
                int iMFHeight = r.bottom - r.top;
                int iMFWidth = r.right - r.left;
                VERIFY(::SetMapMode(hemfdc, MM_HIMETRIC));
                if( GDI_ERROR == ::StretchDIBits(hemfdc,
                    0, iMFHeight, iMFWidth, -iMFHeight,
                    0, 0, width, height,
                    (LPVOID)array, pinfo,
                    DIB_RGB_COLORS, SRCCOPY))
                {
                    hr = HRESULT_FROM_WIN32(::GetLastError());
                }
                HENHMETAFILE hemf = ::CloseEnhMetaFile(hemfdc);
                if( NULL == hemf) {
                    hr = HRESULT_FROM_WIN32(::GetLastError());
                } else {
                    if(SUCCEEDED(hr)){
                        UINT uEmfSize = ::GetEnhMetaFileBits(hemf, 0, NULL);
                        if( 0 == uEmfSize) {
                            hr = HRESULT_FROM_WIN32(::GetLastError());
                        } else {
                            LPBYTE lpbEmfBuffer = NULL;
                            try {
                                lpbEmfBuffer = (LPBYTE)safe_Malloc(uEmfSize);
                                VERIFY(::GetEnhMetaFileBits(hemf, uEmfSize,
                                                            lpbEmfBuffer) == uEmfSize);
                                bytes = env->NewByteArray(uEmfSize);
                                if(NULL == bytes) {
                                    hr = E_OUTOFMEMORY;
                                } else {
                                    env->SetByteArrayRegion(bytes, 0, uEmfSize, (jbyte*)lpbEmfBuffer);
                                }
                            } catch (std::bad_alloc &) {
                                hr = E_OUTOFMEMORY;
                            }
                            free(lpbEmfBuffer);
                        }
                    }
                    VERIFY(::DeleteEnhMetaFile(hemf));
                }
            }
            VERIFY(::ReleaseDC(NULL, hdc));
        }
        break;
    }
    case CF_METAFILEPICT:
    {
        HDC hdc = ::GetDC(NULL);
        if( NULL == hdc) {
            hr = HRESULT_FROM_WIN32(::GetLastError());
        } else {
            POINT p = { width, height };
            VERIFY(::SetMapMode(hdc, MM_HIMETRIC));
            VERIFY(::DPtoLP(hdc, &p, 1));
            RECT r = { min(0, p.x), min(0, p.y), max(0, p.x), max(0, p.y) };
            HDC hmfdc = ::CreateMetaFile(NULL);
            if( NULL == hmfdc) {
                hr = HRESULT_FROM_WIN32(::GetLastError());
            } else {
                VERIFY(::SetMapMode(hmfdc, MM_HIMETRIC));
                int iMFHeight = r.bottom - r.top;
                int iMFWidth = r.right - r.left;
                //The destination Y coordinate (3d parameter in StretchDIBits call) is different for
                //CF_ENHMETAFILE and CF_METAFILEPICT formats due to applying MM_ANISOTROPIC map mode
                //at very last moment. MM_ANISOTROPIC map mode changes the Y-axis direction and can be
                //selected just for metafile header.
                if( GDI_ERROR == ::StretchDIBits(hmfdc,
                    0, 0, iMFWidth, -iMFHeight,
                    0, 0, width, height,
                    (LPVOID)array, pinfo,
                    DIB_RGB_COLORS, SRCCOPY))
                {
                    hr = HRESULT_FROM_WIN32(::GetLastError());
                }
                HMETAFILE hmf = ::CloseMetaFile(hmfdc);
                if( NULL == hmf) {
                    hr = HRESULT_FROM_WIN32(::GetLastError());
                } else {
                    if(SUCCEEDED(hr)){
                        UINT uMfSize = ::GetMetaFileBitsEx(hmf, 0, NULL);
                        if( 0 == uMfSize) {
                            hr = HRESULT_FROM_WIN32(::GetLastError());
                        } else {
                            LPBYTE lpbMfBuffer = NULL;
                            try {
                                lpbMfBuffer = (LPBYTE)SAFE_SIZE_STRUCT_ALLOC(safe_Malloc,
                                        sizeof(METAFILEPICT), uMfSize, 1);
                                const UINT uMfSizeWithHead = uMfSize + sizeof(METAFILEPICT);
                                VERIFY(::GetMetaFileBitsEx(hmf, uMfSize,
                                                            lpbMfBuffer + sizeof(METAFILEPICT)) == uMfSize);
                                bytes = env->NewByteArray(uMfSizeWithHead);
                                if(NULL == bytes) {
                                    hr = E_OUTOFMEMORY;
                                } else {
                                    LPMETAFILEPICT lpMfp = (LPMETAFILEPICT)lpbMfBuffer;
                                    lpMfp->mm = MM_ANISOTROPIC; // should use MM_ANISOTROPIC exactly (MSDN)
                                    lpMfp->xExt = iMFWidth;
                                    lpMfp->yExt = iMFHeight;
                                    env->SetByteArrayRegion(bytes, 0, uMfSizeWithHead, (jbyte*)lpbMfBuffer);
                                }
                            } catch (std::bad_alloc &) {
                                hr = E_OUTOFMEMORY;
                            }
                            free(lpbMfBuffer);
                        }
                    }
                    VERIFY(::DeleteMetaFile(hmf));
                }
            }
            VERIFY(::ReleaseDC(NULL, hdc));
        }
        break;
    }
    default:
        DASSERT(FALSE); // Other formats are not supported yet.
        hr = E_NOTIMPL;
        break;
    }
    free(pinfo);
    if(FAILED(hr)){
        if(E_OUTOFMEMORY == hr)
            throw std::bad_alloc();
        return NULL;
    }
    return bytes;
    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WDataTransferer
 * Method:    registerClipboardFormat
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_windows_WDataTransferer_registerClipboardFormat(JNIEnv *env,
                                                             jclass cls,
                                                             jstring str)
{
    TRY;

    LPCTSTR cStr = JNU_GetStringPlatformChars(env, str, NULL);
    CHECK_NULL_RETURN(cStr, 0);
    jlong value = ::RegisterClipboardFormat(cStr);
    JNU_ReleaseStringPlatformChars(env, str, cStr);

    return value;

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WDataTransferer
 * Method:    getClipboardFormatName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_sun_awt_windows_WDataTransferer_getClipboardFormatName(JNIEnv *env,
                                                            jclass cls,
                                                            jlong format)
{
    TRY;

    LPTSTR buf = new TCHAR[512]; // perhaps a bad idea to limit ourselves to 512
    VERIFY(::GetClipboardFormatName((UINT)format, buf, 512));
    jstring name = JNU_NewStringPlatform(env, buf);
    delete [] buf;
    if (name == NULL) {
        throw std::bad_alloc();
    }
    return name;

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WToolkitThreadBlockedHandler
 * Method:    startSecondaryEventLoop
 * Signature: ()V;
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkitThreadBlockedHandler_startSecondaryEventLoop(JNIEnv *env, jclass)
{
    TRY;

    AwtDataTransferer::SecondaryMessageLoop();

    CATCH_BAD_ALLOC;
}

}
