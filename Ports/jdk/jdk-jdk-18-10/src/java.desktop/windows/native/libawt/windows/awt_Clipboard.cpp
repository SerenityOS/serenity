/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Clipboard.h"
#include "awt_DataTransferer.h"
#include "awt_Toolkit.h"
#include <shlobj.h>
#include <sun_awt_windows_WClipboard.h>


/************************************************************************
 * AwtClipboard fields
 */

jmethodID AwtClipboard::lostSelectionOwnershipMID;
jobject AwtClipboard::theCurrentClipboard;

/* This flag is set while we call EmptyClipboard to indicate to
   WM_DESTROYCLIPBOARD handler that we are not losing ownership */
BOOL AwtClipboard::isGettingOwnership = FALSE;

volatile jmethodID AwtClipboard::handleContentsChangedMID;
volatile BOOL AwtClipboard::isClipboardViewerRegistered = FALSE;

#define GALLOCFLG (GMEM_DDESHARE | GMEM_MOVEABLE | GMEM_ZEROINIT)

/************************************************************************
 * AwtClipboard methods
 */

void AwtClipboard::LostOwnership(JNIEnv *env) {
    if (theCurrentClipboard != NULL) {
        env->CallVoidMethod(theCurrentClipboard, lostSelectionOwnershipMID);
        DASSERT(!safe_ExceptionOccurred(env));
    }
}

void AwtClipboard::WmClipboardUpdate(JNIEnv *env) {
    if (theCurrentClipboard != NULL) {
        env->CallVoidMethod(theCurrentClipboard, handleContentsChangedMID);
        DASSERT(!safe_ExceptionOccurred(env));
    }
}

void AwtClipboard::RegisterClipboardViewer(JNIEnv *env, jobject jclipboard) {
    if (isClipboardViewerRegistered) {
        return;
    }

    if (theCurrentClipboard == NULL) {
        theCurrentClipboard = env->NewGlobalRef(jclipboard);
    }

    jclass cls = env->GetObjectClass(jclipboard);
    AwtClipboard::handleContentsChangedMID =
            env->GetMethodID(cls, "handleContentsChanged", "()V");
    DASSERT(AwtClipboard::handleContentsChangedMID != NULL);

    ::AddClipboardFormatListener(AwtToolkit::GetInstance().GetHWnd());
    isClipboardViewerRegistered = TRUE;
}

void AwtClipboard::UnregisterClipboardViewer(JNIEnv *env) {
    TRY;

    if (isClipboardViewerRegistered) {
        ::RemoveClipboardFormatListener(AwtToolkit::GetInstance().GetHWnd());
        isClipboardViewerRegistered = FALSE;
    }

    CATCH_BAD_ALLOC;
}

extern "C" {

void awt_clipboard_uninitialize(JNIEnv *env) {
    AwtClipboard::UnregisterClipboardViewer(env);
    env->DeleteGlobalRef(AwtClipboard::theCurrentClipboard);
    AwtClipboard::theCurrentClipboard = NULL;
}

/************************************************************************
 * WClipboard native methods
 */

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WClipboard_init(JNIEnv *env, jclass cls)
{
    TRY;

    AwtClipboard::lostSelectionOwnershipMID =
        env->GetMethodID(cls, "lostSelectionOwnershipImpl", "()V");
    DASSERT(AwtClipboard::lostSelectionOwnershipMID != NULL);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    openClipboard
 * Signature: (Lsun/awt/windows/WClipboard;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WClipboard_openClipboard(JNIEnv *env, jobject self,
                                              jobject newOwner)
{
    TRY;

    DASSERT(::GetOpenClipboardWindow() != AwtToolkit::GetInstance().GetHWnd());

    if (!::OpenClipboard(AwtToolkit::GetInstance().GetHWnd())) {
        JNU_ThrowByName(env, "java/lang/IllegalStateException",
                        "cannot open system clipboard");
        return;
    }
    if (newOwner != NULL) {
        AwtClipboard::GetOwnership();
        if (AwtClipboard::theCurrentClipboard != NULL) {
            env->DeleteGlobalRef(AwtClipboard::theCurrentClipboard);
        }
        AwtClipboard::theCurrentClipboard = env->NewGlobalRef(newOwner);
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    closeClipboard
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WClipboard_closeClipboard(JNIEnv *env, jobject self)
{
    TRY;

    if (::GetOpenClipboardWindow() == AwtToolkit::GetInstance().GetHWnd()) {
        VERIFY(::CloseClipboard());
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    registerClipboardViewer
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WClipboard_registerClipboardViewer(JNIEnv *env, jobject self)
{
    TRY;

    AwtClipboard::RegisterClipboardViewer(env, self);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    publishClipboardData
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WClipboard_publishClipboardData(JNIEnv *env,
                                                     jobject self,
                                                     jlong format,
                                                     jbyteArray bytes)
{
    TRY;

    DASSERT(::GetOpenClipboardWindow() == AwtToolkit::GetInstance().GetHWnd());

    if (bytes == NULL) {
        return;
    }

    jint nBytes = env->GetArrayLength(bytes);

    if (format == CF_ENHMETAFILE) {
        LPBYTE lpbEmfBuffer =
            (LPBYTE)env->GetPrimitiveArrayCritical(bytes, NULL);
        if (lpbEmfBuffer == NULL) {
            env->PopLocalFrame(NULL);
            throw std::bad_alloc();
        }

        HENHMETAFILE hemf = ::SetEnhMetaFileBits(nBytes, lpbEmfBuffer);

        env->ReleasePrimitiveArrayCritical(bytes, (LPVOID)lpbEmfBuffer,
                                           JNI_ABORT);

        if (hemf != NULL) {
            VERIFY(::SetClipboardData((UINT)format, hemf));
        }
        return;
    } else if (format == CF_METAFILEPICT) {
        LPBYTE lpbMfpBuffer =
            (LPBYTE)env->GetPrimitiveArrayCritical(bytes, NULL);
        if (lpbMfpBuffer == NULL) {
            env->PopLocalFrame(NULL);
            throw std::bad_alloc();
        }

        HMETAFILE hmf = ::SetMetaFileBitsEx(nBytes - sizeof(METAFILEPICT),
                                         lpbMfpBuffer + sizeof(METAFILEPICT));
        if (hmf == NULL) {
            env->ReleasePrimitiveArrayCritical(bytes, (LPVOID)lpbMfpBuffer, JNI_ABORT);
            env->PopLocalFrame(NULL);
            return;
        }

        LPMETAFILEPICT lpMfpOld = (LPMETAFILEPICT)lpbMfpBuffer;

        HMETAFILEPICT hmfp = ::GlobalAlloc(GALLOCFLG, sizeof(METAFILEPICT));
        if (hmfp == NULL) {
            VERIFY(::DeleteMetaFile(hmf));
            env->ReleasePrimitiveArrayCritical(bytes, (LPVOID)lpbMfpBuffer, JNI_ABORT);
            env->PopLocalFrame(NULL);
            throw std::bad_alloc();
        }

        LPMETAFILEPICT lpMfp = (LPMETAFILEPICT)::GlobalLock(hmfp);
        lpMfp->mm = lpMfpOld->mm;
        lpMfp->xExt = lpMfpOld->xExt;
        lpMfp->yExt = lpMfpOld->yExt;
        lpMfp->hMF = hmf;
        ::GlobalUnlock(hmfp);

        env->ReleasePrimitiveArrayCritical(bytes, (LPVOID)lpbMfpBuffer, JNI_ABORT);

        VERIFY(::SetClipboardData((UINT)format, hmfp));

        return;
    }

    // We have to prepend the DROPFILES structure here because WDataTransferer
    // doesn't.
    HGLOBAL hglobal = ::GlobalAlloc(GALLOCFLG, nBytes + ((format == CF_HDROP)
                                                            ? sizeof(DROPFILES)
                                                            : 0));
    if (hglobal == NULL) {
        throw std::bad_alloc();
    }
    char *dataout = (char *)::GlobalLock(hglobal);

    if (format == CF_HDROP) {
        DROPFILES *dropfiles = (DROPFILES *)dataout;
        dropfiles->pFiles = sizeof(DROPFILES);
        dropfiles->fWide = TRUE; // we publish only Unicode
        dataout += sizeof(DROPFILES);
    }

    env->GetByteArrayRegion(bytes, 0, nBytes, (jbyte *)dataout);
    ::GlobalUnlock(hglobal);

    VERIFY(::SetClipboardData((UINT)format, hglobal));

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    getClipboardFormats
 * Signature: ()[J
 */
JNIEXPORT jlongArray JNICALL
Java_sun_awt_windows_WClipboard_getClipboardFormats
    (JNIEnv *env, jobject self)
{
    TRY;

    DASSERT(::GetOpenClipboardWindow() == AwtToolkit::GetInstance().GetHWnd());

    jsize nFormats = ::CountClipboardFormats();
    jlongArray formats = env->NewLongArray(nFormats);
    if (formats == NULL) {
        throw std::bad_alloc();
    }
    if (nFormats == 0) {
        return formats;
    }
    jboolean isCopy;
    jlong *lFormats = env->GetLongArrayElements(formats, &isCopy),
        *saveFormats = lFormats;
    UINT num = 0;

    for (jsize i = 0; i < nFormats; i++, lFormats++) {
        *lFormats = num = ::EnumClipboardFormats(num);
    }

    env->ReleaseLongArrayElements(formats, saveFormats, 0);

    return formats;

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WClipboard
 * Method:    getClipboardData
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_awt_windows_WClipboard_getClipboardData
    (JNIEnv *env, jobject self, jlong format)
{
    TRY;

    DASSERT(::GetOpenClipboardWindow() == AwtToolkit::GetInstance().GetHWnd());

    HANDLE handle = ::GetClipboardData((UINT)format);
    if (handle == NULL) {
        JNU_ThrowIOException(env, "system clipboard data unavailable");
        return NULL;
    }

    jbyteArray bytes = NULL;
    jbyteArray paletteData = NULL;

    switch (format) {
    case CF_ENHMETAFILE:
    case CF_METAFILEPICT: {
        HENHMETAFILE hemf = NULL;

        if (format == CF_METAFILEPICT) {
            HMETAFILEPICT hMetaFilePict = (HMETAFILEPICT)handle;
            LPMETAFILEPICT lpMetaFilePict =
                (LPMETAFILEPICT)::GlobalLock(hMetaFilePict);
            UINT uSize = ::GetMetaFileBitsEx(lpMetaFilePict->hMF, 0, NULL);
            DASSERT(uSize != 0);

            try {
                LPBYTE lpMfBits = (LPBYTE)safe_Malloc(uSize);
                VERIFY(::GetMetaFileBitsEx(lpMetaFilePict->hMF, uSize,
                                           lpMfBits) == uSize);
                hemf = ::SetWinMetaFileBits(uSize, lpMfBits, NULL,
                                            lpMetaFilePict);
                free(lpMfBits);
                if (hemf == NULL) {
                    ::GlobalUnlock(hMetaFilePict);
                    JNU_ThrowIOException(env, "failed to get system clipboard data");
                    return NULL;
                }
            } catch (...) {
                ::GlobalUnlock(hMetaFilePict);
                throw;
            }
            ::GlobalUnlock(hMetaFilePict);
        } else {
            hemf = (HENHMETAFILE)handle;
        }

        UINT uEmfSize = ::GetEnhMetaFileBits(hemf, 0, NULL);
        if (uEmfSize == 0) {
            JNU_ThrowIOException(env, "cannot retrieve metafile bits");
            return NULL;
        }

        bytes = env->NewByteArray(uEmfSize);
        if (bytes == NULL) {
            throw std::bad_alloc();
        }

        LPBYTE lpbEmfBuffer =
            (LPBYTE)env->GetPrimitiveArrayCritical(bytes, NULL);
        if (lpbEmfBuffer == NULL) {
            env->DeleteLocalRef(bytes);
            throw std::bad_alloc();
        }
        VERIFY(::GetEnhMetaFileBits(hemf, uEmfSize, lpbEmfBuffer) == uEmfSize);
        env->ReleasePrimitiveArrayCritical(bytes, lpbEmfBuffer, 0);

        paletteData =
            AwtDataTransferer::GetPaletteBytes(hemf, OBJ_ENHMETAFILE, FALSE);
        break;
    }
    case CF_LOCALE: {
        LCID *lcid = (LCID *)::GlobalLock(handle);
        if (lcid == NULL) {
            JNU_ThrowIOException(env, "invalid LCID");
            return NULL;
        }
        try {
            bytes = AwtDataTransferer::LCIDToTextEncoding(env, *lcid);
        } catch (...) {
            ::GlobalUnlock(handle);
            throw;
        }
        ::GlobalUnlock(handle);
        break;
    }
    default: {
        ::SetLastError(0); // clear error
        // Warning C4244.
        // Cast SIZE_T (__int64 on 64-bit/unsigned int on 32-bit)
        // to jsize (long).
        SIZE_T globalSize = ::GlobalSize(handle);
        jsize size = (globalSize <= INT_MAX) ? (jsize)globalSize : INT_MAX;
        if (::GetLastError() != 0) {
            JNU_ThrowIOException(env, "invalid global memory block handle");
            return NULL;
        }

        bytes = env->NewByteArray(size);
        if (bytes == NULL) {
            throw std::bad_alloc();
        }

        if (size != 0) {
            LPVOID data = ::GlobalLock(handle);
            env->SetByteArrayRegion(bytes, 0, size, (jbyte *)data);
            ::GlobalUnlock(handle);
        }
        break;
    }
    }

    switch (format) {
    case CF_ENHMETAFILE:
    case CF_METAFILEPICT:
    case CF_DIB: {
        if (JNU_IsNull(env, paletteData)) {
            HPALETTE hPalette = (HPALETTE)::GetClipboardData(CF_PALETTE);
            paletteData =
                AwtDataTransferer::GetPaletteBytes(hPalette, OBJ_PAL, TRUE);
        }
        DASSERT(!JNU_IsNull(env, paletteData) &&
                !JNU_IsNull(env, bytes));

        jbyteArray concat =
            (jbyteArray)AwtDataTransferer::ConcatData(env, paletteData, bytes);

        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            env->DeleteLocalRef(bytes);
            env->DeleteLocalRef(paletteData);
            return NULL;
        }

        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(paletteData);
        bytes = concat;
        break;
    }
    }

    return bytes;

    CATCH_BAD_ALLOC_RET(NULL);
}

} /* extern "C" */
