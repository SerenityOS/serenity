/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <shlwapi.h>
#include <shellapi.h>
#include <memory.h>

#include "awt_DataTransferer.h"
#include "java_awt_dnd_DnDConstants.h"
#include "sun_awt_windows_WDropTargetContextPeer.h"
#include "awt_Container.h"
#include "awt_ole.h"
#include "awt_Toolkit.h"
#include "awt_DnDDT.h"
#include "awt_DnDDS.h"


// forwards

extern "C" {
    DWORD __cdecl convertActionsToDROPEFFECT(jint actions);
    jint  __cdecl convertDROPEFFECTToActions(DWORD effects);
    DWORD __cdecl mapModsToDROPEFFECT(DWORD, DWORD);
} // extern "C"


IDataObject* AwtDropTarget::sm_pCurrentDnDDataObject = (IDataObject*)NULL;

/**
 * constructor
 */

AwtDropTarget::AwtDropTarget(JNIEnv* env, AwtComponent* component) {

    m_component     = component;
    m_window        = component->GetHWnd();
    m_refs          = 1U;
    m_target        = env->NewGlobalRef(component->GetTarget(env));
    m_registered    = 0;
    m_dataObject    = NULL;
    m_formats       = NULL;
    m_nformats      = 0;
    m_dtcp          = NULL;
    m_cfFormats     = NULL;
    m_mutex         = ::CreateMutex(NULL, FALSE, NULL);
    m_pIDropTargetHelper = NULL;
}

/**
 * destructor
 */

AwtDropTarget::~AwtDropTarget() {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // fix for 6212440: on application shutdown, this object's
    // destruction might be suppressed due to dangling COM references.
    // On destruction, VM might be shut down already, so we should make
    // a null check on env.
    if (env) {
        env->DeleteGlobalRef(m_target);
        env->DeleteGlobalRef(m_dtcp);
    }

    ::CloseHandle(m_mutex);

    UnloadCache();
}

/**
 * QueryInterface
 */

HRESULT __stdcall AwtDropTarget::QueryInterface(REFIID riid, void __RPC_FAR *__RPC_FAR *ppvObject) {
    if ( IID_IUnknown == riid ||
         IID_IDropTarget == riid )
    {
        *ppvObject = static_cast<IDropTarget*>(this);
        AddRef();
        return S_OK;
    }
    *ppvObject = NULL;
    return E_NOINTERFACE;
}

/**
 * AddRef
 */

ULONG __stdcall AwtDropTarget::AddRef() {
    return (ULONG)++m_refs;
}

/**
 * Release
 */

ULONG __stdcall AwtDropTarget::Release() {
    int refs;

    if ((refs = --m_refs) == 0) delete this;

    return (ULONG)refs;
}

static void ScaleDown(POINT &cp, HWND m_window) {
    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(m_window);
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice* device = devices->GetDevice(screen);
    if (device) {
        cp.x = device->ScaleDownX(cp.x);
        cp.y = device->ScaleDownY(cp.y);
    }
}

/**
 * DragEnter
 */

HRESULT __stdcall AwtDropTarget::DragEnter(IDataObject __RPC_FAR *pDataObj, DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect) {
    TRY;
    AwtToolkit::GetInstance().isInDoDragDropLoop = TRUE;
    if (NULL != m_pIDropTargetHelper) {
        m_pIDropTargetHelper->DragEnter(
            m_window,
            pDataObj,
            (LPPOINT)&pt,
            *pdwEffect);
    }

    AwtInterfaceLocker _lk(this);

    JNIEnv*    env       = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    HRESULT    ret       = S_OK;
    DWORD      retEffect = DROPEFFECT_NONE;
    jobject    dtcp = NULL;

    if ( (!IsLocalDnD() && !IsCurrentDnDDataObject(NULL)) ||
        (IsLocalDnD()  && !IsLocalDataObject(pDataObj)))
    {
        *pdwEffect = retEffect;
        AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
        return ret;
    }

    dtcp = call_dTCcreate(env);
    if (dtcp) {
        env->DeleteGlobalRef(m_dtcp);
        m_dtcp = env->NewGlobalRef(dtcp);
        env->DeleteLocalRef(dtcp);
    }

    if (JNU_IsNull(env, m_dtcp) || !JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
        return ret;
    }

    LoadCache(pDataObj);

    {
        POINT cp;
        RECT  wr;

        ::GetWindowRect(m_window, &wr);

        cp.x = pt.x - wr.left;
        cp.y = pt.y - wr.top;
        ScaleDown(cp, m_window);

        jint actions = call_dTCenter(env, m_dtcp, m_target,
                                     (jint)cp.x, (jint)cp.y,
                                     ::convertDROPEFFECTToActions(mapModsToDROPEFFECT(*pdwEffect, grfKeyState)),
                                     ::convertDROPEFFECTToActions(*pdwEffect),
                                     m_cfFormats, (jlong)this);

        try {
            if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                actions = java_awt_dnd_DnDConstants_ACTION_NONE;
                AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
            }
        } catch (std::bad_alloc&) {
            retEffect = ::convertActionsToDROPEFFECT(actions);
            *pdwEffect = retEffect;
            AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
            throw;
        }

        retEffect = ::convertActionsToDROPEFFECT(actions);
    }

    *pdwEffect = retEffect;

    return ret;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * DragOver
 */

HRESULT __stdcall AwtDropTarget::DragOver(DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect) {
    TRY;
    if (NULL != m_pIDropTargetHelper) {
        m_pIDropTargetHelper->DragOver(
            (LPPOINT)&pt,
            *pdwEffect
        );
    }

    AwtInterfaceLocker _lk(this);

    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    HRESULT ret = S_OK;
    POINT   cp;
    RECT    wr;
    jint    actions;

    if ( (!IsLocalDnD() && !IsCurrentDnDDataObject(m_dataObject)) ||
        (IsLocalDnD()  && !IsLocalDataObject(m_dataObject)))
    {
        *pdwEffect = DROPEFFECT_NONE;
        return ret;
    }

    ::GetWindowRect(m_window, &wr);

    cp.x = pt.x - wr.left;
    cp.y = pt.y - wr.top;
    ScaleDown(cp, m_window);

    actions = call_dTCmotion(env, m_dtcp, m_target,(jint)cp.x, (jint)cp.y,
                             ::convertDROPEFFECTToActions(mapModsToDROPEFFECT(*pdwEffect, grfKeyState)),
                             ::convertDROPEFFECTToActions(*pdwEffect),
                             m_cfFormats, (jlong)this);

    try {
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            actions = java_awt_dnd_DnDConstants_ACTION_NONE;
        }
    } catch (std::bad_alloc&) {
        *pdwEffect = ::convertActionsToDROPEFFECT(actions);
        throw;
    }

    *pdwEffect = ::convertActionsToDROPEFFECT(actions);

    return ret;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * DragLeave
 */

HRESULT __stdcall AwtDropTarget::DragLeave() {
    TRY_NO_VERIFY;
    if (NULL != m_pIDropTargetHelper) {
        m_pIDropTargetHelper->DragLeave();
    }

    AwtInterfaceLocker _lk(this);

    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    HRESULT ret = S_OK;

    if ( (!IsLocalDnD() && !IsCurrentDnDDataObject(m_dataObject)) ||
        (IsLocalDnD()  && !IsLocalDataObject(m_dataObject)))
    {
        DragCleanup();
        return ret;
    }

    call_dTCexit(env, m_dtcp, m_target, (jlong)this);

    try {
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    } catch (std::bad_alloc&) {
        DragCleanup();
        throw;
    }

    DragCleanup();

    return ret;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Drop
 */

HRESULT __stdcall AwtDropTarget::Drop(IDataObject __RPC_FAR *pDataObj, DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect) {
    TRY;
    if (NULL != m_pIDropTargetHelper) {
        m_pIDropTargetHelper->Drop(
            pDataObj,
            (LPPOINT)&pt,
            *pdwEffect
        );
    }
    AwtInterfaceLocker _lk(this);

    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    HRESULT ret = S_OK;
    POINT   cp;
    RECT    wr;

    if ( (!IsLocalDnD() && !IsCurrentDnDDataObject(pDataObj)) ||
        (IsLocalDnD()  && !IsLocalDataObject(pDataObj)))
    {
        *pdwEffect = DROPEFFECT_NONE;
        DragCleanup();
        return ret;
    }

    LoadCache(pDataObj);

    ::GetWindowRect(m_window, &wr);

    cp.x = pt.x - wr.left;
    cp.y = pt.y - wr.top;
    ScaleDown(cp, m_window);

    m_dropActions = java_awt_dnd_DnDConstants_ACTION_NONE;

    call_dTCdrop(env, m_dtcp, m_target, (jint)cp.x, (jint)cp.y,
                 ::convertDROPEFFECTToActions(mapModsToDROPEFFECT(*pdwEffect, grfKeyState)),
                 ::convertDROPEFFECTToActions(*pdwEffect),
                 m_cfFormats, (jlong)this);

    try {
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            ret = E_FAIL;
        }
    } catch (std::bad_alloc&) {
        AwtToolkit::GetInstance().MessageLoop(AwtToolkit::SecondaryIdleFunc,
                                              AwtToolkit::CommonPeekMessageFunc);
        *pdwEffect = ::convertActionsToDROPEFFECT(m_dropActions);
        DragCleanup();
        throw;
    }

    /*
     * Fix for 4623377.
     * Dispatch all messages in the nested message loop running while the drop is
     * processed. This ensures that the modal dialog shown during drop receives
     * all events and so it is able to close. This way the app won't deadlock.
     */
    AwtToolkit::GetInstance().MessageLoop(AwtToolkit::SecondaryIdleFunc,
                                          AwtToolkit::CommonPeekMessageFunc);

    ret = (m_dropSuccess == JNI_TRUE) ? S_OK : E_FAIL;
    *pdwEffect = ::convertActionsToDROPEFFECT(m_dropActions);

    DragCleanup();

    return ret;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * DoDropDone
 */

void AwtDropTarget::DoDropDone(jboolean success, jint action) {
    DropDoneRec ddr = { this, success, action };

    AwtToolkit::GetInstance().InvokeFunction(_DropDone, &ddr);
}

/**
 * _DropDone
 */

void AwtDropTarget::_DropDone(void* param) {
    DropDonePtr ddrp = (DropDonePtr)param;

    (ddrp->dropTarget)->DropDone(ddrp->success, ddrp->action);
}

/**
 * DropDone
 */

void AwtDropTarget::DropDone(jboolean success, jint action) {
    m_dropSuccess = success;
    m_dropActions = action;
    AwtToolkit::GetInstance().QuitMessageLoop(AwtToolkit::EXIT_ENCLOSING_LOOP);
    AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
}

/**
 * DoRegisterTarget
 */

void AwtDropTarget::_RegisterTarget(void* param) {
    RegisterTargetPtr rtrp = (RegisterTargetPtr)param;

    rtrp->dropTarget->RegisterTarget(rtrp->show);
}

/**
 * RegisterTarget
 */

void AwtDropTarget::RegisterTarget(WORD show) {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    HRESULT res;

    if (!AwtToolkit::IsMainThread()) {
        RegisterTargetRec rtr = { this, show };

        AwtToolkit::GetInstance().InvokeFunction(_RegisterTarget, &rtr);

        return;
    }

    // if we are'nt yet visible, defer until the parent is!

    if (show) {
        OLE_TRY
        OLE_HRT(CoCreateInstance(
            CLSID_DragDropHelper,
            NULL,
            CLSCTX_ALL,
            IID_IDropTargetHelper,
            (LPVOID*)&m_pIDropTargetHelper
        ))
        OLE_HRT(::RegisterDragDrop(m_window, (IDropTarget*)this))
        OLE_CATCH
        res = OLE_HR;
    } else {
        res = ::RevokeDragDrop(m_window);
        if (NULL != m_pIDropTargetHelper) {
            m_pIDropTargetHelper->Release();
        }
    }

    if (res == S_OK) m_registered = show;
}

/**
 * DoGetData
 */

jobject AwtDropTarget::DoGetData(jlong format) {
    jobject    ret = (jobject)NULL;
    GetDataRec gdr = { this, format, &ret };

    AwtToolkit::GetInstance().WaitForSingleObject(m_mutex);

    AwtToolkit::GetInstance().InvokeFunctionLater(_GetData, &gdr);

    WaitUntilSignalled(FALSE);

    return ret;
}

/**
 * _GetData
 */

void AwtDropTarget::_GetData(void* param) {
    GetDataPtr gdrp = (GetDataPtr)param;

    *(gdrp->ret) = gdrp->dropTarget->GetData(gdrp->format);

    gdrp->dropTarget->Signal();
}


/**
 * GetData
 *
 * Returns the data object being transferred.
 */

HRESULT AwtDropTarget::ExtractNativeData(
    jlong fmt,
    LONG lIndex,
    STGMEDIUM *pmedium)
{
    FORMATETC format = { (unsigned short)fmt };
    HRESULT hr = E_INVALIDARG;

    static const DWORD supportedTymeds[] = {
        TYMED_ISTREAM,
        TYMED_ENHMF,
        TYMED_GDI,
        TYMED_MFPICT,
        TYMED_FILE,
        TYMED_HGLOBAL
    };

    for (int i = 0; i < sizeof(supportedTymeds)/sizeof(supportedTymeds[0]); ++i) {
        // Only TYMED_HGLOBAL is supported for CF_LOCALE.
        if (fmt == CF_LOCALE && supportedTymeds[i] != TYMED_HGLOBAL) {
            continue;
        }

        format.tymed = supportedTymeds[i];
        FORMATETC *cpp = (FORMATETC *)bsearch(
            (const void *)&format,
            (const void *)m_formats,
            (size_t)m_nformats,
            (size_t)sizeof(FORMATETC),
            _compar);

        if (NULL == cpp) {
            continue;
        }

        format = *cpp;
        format.lindex = lIndex;

        hr = m_dataObject->GetData(&format, pmedium);
        if (SUCCEEDED(hr)) {
            return hr;
        }
    }
    return hr;
}

HRESULT CheckRetValue(
    JNIEnv* env,
    jobject ret)
{
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        return E_UNEXPECTED;
    } else if (JNU_IsNull(env, ret)) {
        return E_INVALIDARG;
    }
    return S_OK;
}

jobject AwtDropTarget::ConvertNativeData(JNIEnv* env, jlong fmt, STGMEDIUM *pmedium) /*throw std::bad_alloc */
{
    jobject ret = NULL;
    jbyteArray paletteDataLocal = NULL;
    HRESULT hr = S_OK;
    switch (pmedium->tymed) {
        case TYMED_HGLOBAL: {
            if (fmt == CF_LOCALE) {
                LCID *lcid = (LCID *)::GlobalLock(pmedium->hGlobal);
                if (NULL == lcid) {
                    hr = E_INVALIDARG;
                } else {
                    try{
                        ret = AwtDataTransferer::LCIDToTextEncoding(env, *lcid);
                        hr = CheckRetValue(env, ret);
                    } catch (std::bad_alloc&) {
                        hr = E_OUTOFMEMORY;
                    }
                    ::GlobalUnlock(pmedium->hGlobal);
                }
            } else {
                ::SetLastError(0); // clear error
                // Warning C4244.
                // Cast SIZE_T (__int64 on 64-bit/unsigned int on 32-bit)
                // to jsize (long).
                SIZE_T globalSize = ::GlobalSize(pmedium->hGlobal);
                jsize size = (globalSize <= INT_MAX) ? (jsize)globalSize : INT_MAX;
                if (size == 0 && ::GetLastError() != 0) {
                    hr = E_INVALIDARG;
                } else {
                    jbyteArray bytes = env->NewByteArray(size);
                    if (NULL == bytes) {
                        hr = E_OUTOFMEMORY;
                    } else {
                        LPVOID data = ::GlobalLock(pmedium->hGlobal);
                        if (NULL == data) {
                            hr = E_INVALIDARG;
                        } else {
                            env->SetByteArrayRegion(bytes, 0, size, (jbyte *)data);
                            ret = bytes;
                            //bytes is not null here => no CheckRetValue call
                            ::GlobalUnlock(pmedium->hGlobal);
                        }
                    }
                }
            }
            break;
        }
        case TYMED_FILE: {
            jobject local = JNU_NewStringPlatform(
                env,
                pmedium->lpszFileName);
            if (env->ExceptionCheck()) {
                hr = E_OUTOFMEMORY;
                break;
            }
            jstring fileName = (jstring)env->NewGlobalRef(local);
            env->DeleteLocalRef(local);

            STGMEDIUM *stgm = NULL;
            try {
                //on success stgm would be deallocated by JAVA call freeStgMedium
                stgm = (STGMEDIUM *)safe_Malloc(sizeof(STGMEDIUM));
                memcpy(stgm, pmedium, sizeof(STGMEDIUM));
                // Warning C4311.
                // Cast pointer to jlong (__int64).
                ret = call_dTCgetfs(env, fileName, (jlong)stgm);
                hr = CheckRetValue(env, ret);
            } catch (std::bad_alloc&) {
                hr = E_OUTOFMEMORY;
            }
            if (FAILED(hr)) {
                //free just on error
                env->DeleteGlobalRef(fileName);
                free(stgm);
            }
            break;
        }
        case TYMED_ISTREAM: {
            WDTCPIStreamWrapper* istream = NULL;
            try {
                istream = new WDTCPIStreamWrapper(pmedium);
                // Warning C4311.
                // Cast pointer to jlong (__int64).
                ret = call_dTCgetis(env, (jlong)istream);
                hr = CheckRetValue(env, ret);
            } catch (std::bad_alloc&) {
                hr = E_OUTOFMEMORY;
            }
            if (FAILED(hr) && NULL!=istream) {
                //free just on error
                istream->Close();
            }
            break;
        }
        case TYMED_GDI:
            // Currently support only CF_PALETTE for TYMED_GDI.
            if (CF_PALETTE == fmt) {
                ret = AwtDataTransferer::GetPaletteBytes(
                    pmedium->hBitmap,
                    0,
                    TRUE);
                hr = CheckRetValue(env, ret);
            }
            break;
        case TYMED_MFPICT:
        case TYMED_ENHMF: {
            HENHMETAFILE hEnhMetaFile = NULL;
            if (pmedium->tymed == TYMED_MFPICT ) {
                //let's create ENHMF from MFPICT to simplify treatment
                LPMETAFILEPICT lpMetaFilePict =
                    (LPMETAFILEPICT)::GlobalLock(pmedium->hMetaFilePict);
                if (NULL == lpMetaFilePict) {
                    hr = E_INVALIDARG;
                } else {
                    UINT uSize = ::GetMetaFileBitsEx(lpMetaFilePict->hMF, 0, NULL);
                    if (0 == uSize) {
                        hr = E_INVALIDARG;
                    } else {
                        try{
                            LPBYTE lpMfBits = (LPBYTE)safe_Malloc(uSize);
                            VERIFY(::GetMetaFileBitsEx(
                                lpMetaFilePict->hMF,
                                uSize,
                                lpMfBits) == uSize);
                            hEnhMetaFile = ::SetWinMetaFileBits(
                                uSize,
                                lpMfBits,
                                NULL,
                                lpMetaFilePict);
                            free(lpMfBits);
                        } catch (std::bad_alloc&) {
                            hr = E_OUTOFMEMORY;
                        }
                    }
                    ::GlobalUnlock(pmedium->hMetaFilePict);
                }
            } else {
                hEnhMetaFile = pmedium->hEnhMetaFile;
            }

            if (NULL == hEnhMetaFile) {
                hr = E_INVALIDARG;
            } else {
                try {
                    paletteDataLocal = AwtDataTransferer::GetPaletteBytes(
                        hEnhMetaFile,
                        OBJ_ENHMETAFILE,
                        FALSE);
                    //paletteDataLocal can be NULL here - it is not a error!

                    UINT uEmfSize = ::GetEnhMetaFileBits(hEnhMetaFile, 0, NULL);
                    DASSERT(uEmfSize != 0);

                    LPBYTE lpEmfBits = (LPBYTE)safe_Malloc(uEmfSize);
                    //no chance to throw exception before catch => no more try-blocks
                    //and no leaks on lpEmfBits

                    VERIFY(::GetEnhMetaFileBits(
                        hEnhMetaFile,
                        uEmfSize,
                        lpEmfBits) == uEmfSize);

                    jbyteArray bytes = env->NewByteArray(uEmfSize);
                    if (NULL == bytes) {
                        hr = E_OUTOFMEMORY;
                    } else {
                        env->SetByteArrayRegion(bytes, 0, uEmfSize, (jbyte*)lpEmfBits);
                        ret = bytes;
                        //bytes is not null here => no CheckRetValue call
                    }
                    free(lpEmfBits);
                } catch (std::bad_alloc&) {
                    hr = E_OUTOFMEMORY;
                }
                if (pmedium->tymed == TYMED_MFPICT) {
                    //because we create it manually
                    ::DeleteEnhMetaFile(hEnhMetaFile);
                }
            }
            break;
        }
        case TYMED_ISTORAGE:
        default:
            hr = E_NOTIMPL;
            break;
    }

    if (FAILED(hr)) {
        //clear exception garbage for hr = E_UNEXPECTED
        ret  = NULL;
    } else {
        switch (fmt) {
        case CF_METAFILEPICT:
        case CF_ENHMETAFILE:
            // If we failed to retrieve palette entries from metafile,
            // fall through and try CF_PALETTE format.
        case CF_DIB: {
            if (JNU_IsNull(env, paletteDataLocal)) {
                jobject paletteData = GetData(CF_PALETTE);

                if (JNU_IsNull(env, paletteData)) {
                    paletteDataLocal =
                        AwtDataTransferer::GetPaletteBytes(NULL, 0, TRUE);
                } else {
                    // GetData() returns a global ref.
                    // We want to deal with local ref.
                    paletteDataLocal = (jbyteArray)env->NewLocalRef(paletteData);
                    env->DeleteGlobalRef(paletteData);
                }
            }
            DASSERT(!JNU_IsNull(env, paletteDataLocal) &&
                    !JNU_IsNull(env, ret));

            jobject concat = AwtDataTransferer::ConcatData(env, paletteDataLocal, ret);
            env->DeleteLocalRef(ret);
            ret = concat;
            hr = CheckRetValue(env, ret);
            break;
        }
        }
    }

    if (!JNU_IsNull(env, paletteDataLocal) ) {
        env->DeleteLocalRef(paletteDataLocal);
    }
    jobject global = NULL;
    if (SUCCEEDED(hr)) {
        global = env->NewGlobalRef(ret);
        env->DeleteLocalRef(ret);
    } else if (E_UNEXPECTED == hr) {
        //internal Java non-GPF exception
        env->ExceptionDescribe();
        env->ExceptionClear();
    } else if (E_OUTOFMEMORY == hr) {
        throw std::bad_alloc();
    } //NULL returns for all other cases
    return global;
}

HRESULT AwtDropTarget::SaveIndexToFile(LPCTSTR pFileName, UINT lIndex)
{
    OLE_TRY
    STGMEDIUM stgmedium;
    OLE_HRT( ExtractNativeData(CF_FILECONTENTS, lIndex, &stgmedium) );
    OLE_NEXT_TRY
        IStreamPtr spSrc;
        if (TYMED_HGLOBAL == stgmedium.tymed) {
            OLE_HRT( CreateStreamOnHGlobal(
                stgmedium.hGlobal,
                FALSE,
                &spSrc
            ));
        } else if(TYMED_ISTREAM == stgmedium.tymed) {
            spSrc = stgmedium.pstm;
        }
        if (NULL == spSrc) {
            OLE_HRT(E_INVALIDARG);
        }
        IStreamPtr spDst;
        OLE_HRT(SHCreateStreamOnFile(
            pFileName,
            STGM_WRITE | STGM_CREATE,
            &spDst
        ));
        STATSTG si = {0};
        OLE_HRT( spSrc->Stat(&si, STATFLAG_NONAME ) );
        OLE_HRT( spSrc->CopyTo(spDst, si.cbSize, NULL, NULL) );
    OLE_CATCH
    ::ReleaseStgMedium(&stgmedium);
    OLE_CATCH
    OLE_RETURN_HR;
}


HRESULT GetTempPathWithSlash(JNIEnv *env, _bstr_t &bsTempPath) /*throws _com_error*/
{
    static _bstr_t _bsPath;

    OLE_TRY
    if (0 == _bsPath.length()) {
        BOOL bSafeEmergency = TRUE;
        TCHAR szPath[MAX_PATH*2];
        JLClass systemCls(env, env->FindClass("java/lang/System"));
        if (systemCls) {
            jmethodID idGetProperty = env->GetStaticMethodID(
                    systemCls,
                    "getProperty",
                    "(Ljava/lang/String;)Ljava/lang/String;");
            if (0 != idGetProperty) {
                static TCHAR param[] = _T("java.io.tmpdir");
                JLString tempdir(env, JNU_NewStringPlatform(env, param));
                if (tempdir) {
                    JLString jsTempPath(env, (jstring)env->CallStaticObjectMethod(
                        systemCls,
                        idGetProperty,
                        (jstring)tempdir
                    ));
                    if (jsTempPath) {
                        _bsPath = (LPCWSTR)JavaStringBuffer(env, jsTempPath);
                        OLE_HRT(SHGetFolderPath(
                            NULL,
                            CSIDL_WINDOWS,
                            NULL,
                            0,
                            szPath));
                        _tcscat(szPath, _T("\\"));
                        //Dead environment block leads to fact that windows folder becomes temporary path.
                        //For example while jtreg execution %TEMP%, %TMP% and etc. aren't defined.
                        bSafeEmergency = ( 0 == _tcsicmp(_bsPath, szPath) );
                    }
                }
            }
        }
        if (bSafeEmergency) {
            OLE_HRT(SHGetFolderPath(
                NULL,
                CSIDL_INTERNET_CACHE|CSIDL_FLAG_CREATE,
                NULL,
                0,
                szPath));
            _tcscat(szPath, _T("\\"));
            _bsPath = szPath;
        }
    }
    OLE_CATCH
    bsTempPath = _bsPath;
    OLE_RETURN_HR
}

jobject AwtDropTarget::ConvertMemoryMappedData(JNIEnv* env, jlong fmt, STGMEDIUM *pmedium) /*throw std::bad_alloc */
{
    jobject retObj = NULL;
    OLE_TRY
    if (TYMED_HGLOBAL != pmedium->tymed) {
        OLE_HRT(E_INVALIDARG);
    }
    FILEGROUPDESCRIPTORA *pfgdHead = (FILEGROUPDESCRIPTORA *)::GlobalLock(pmedium->hGlobal);
    if (NULL == pfgdHead) {
        OLE_HRT(E_INVALIDARG);
    }
    OLE_NEXT_TRY
        if (0 == pfgdHead->cItems) {
            OLE_HRT(E_INVALIDARG);
        }
        IStreamPtr spFileNames;
        OLE_HRT( CreateStreamOnHGlobal(
            NULL,
            TRUE,
            &spFileNames
        ));

        _bstr_t sbTempDir;
        OLE_HRT( GetTempPathWithSlash(env, sbTempDir) );
        FILEDESCRIPTORA *pfgdA = pfgdHead->fgd;
        FILEDESCRIPTORW *pfgdW = (FILEDESCRIPTORW *)pfgdA;
        for (UINT i = 0; i < pfgdHead->cItems; ++i) {
            _bstr_t stFullName(sbTempDir);
            if(CF_FILEGROUPDESCRIPTORA == fmt) {
                stFullName += pfgdA->cFileName; //as CHAR
                ++pfgdA;
            } else {
                stFullName += pfgdW->cFileName; //as WCHAR
                ++pfgdW;
            }
            OLE_HRT(SaveIndexToFile(
                stFullName,
                i));
            //write to stream with zero terminator
            OLE_HRT( spFileNames->Write((LPCTSTR)stFullName, (stFullName.length() + 1)*sizeof(TCHAR), NULL) );
        }
        OLE_HRT( spFileNames->Write(_T(""), sizeof(TCHAR), NULL) );
        STATSTG st;
        OLE_HRT( spFileNames->Stat(&st, STATFLAG_NONAME) );

        //empty lists was forbidden: pfgdHead->cItems > 0
        jbyteArray bytes = env->NewByteArray(st.cbSize.LowPart);
        if (NULL == bytes) {
            OLE_HRT(E_OUTOFMEMORY);
        } else {
            HGLOBAL glob;
            OLE_HRT(GetHGlobalFromStream(spFileNames, &glob));
            jbyte *pFileListWithDoubleZeroTerminator = (jbyte *)::GlobalLock(glob);
            env->SetByteArrayRegion(bytes, 0, st.cbSize.LowPart, pFileListWithDoubleZeroTerminator);
            ::GlobalUnlock(pFileListWithDoubleZeroTerminator);
            retObj = bytes;
        }
        //std::bad_alloc could happen in JStringBuffer
        //no leaks due to wrapper
    OLE_CATCH_BAD_ALLOC
    ::GlobalUnlock(pmedium->hGlobal);
    OLE_CATCH
    jobject global = NULL;
    if (SUCCEEDED(OLE_HR)) {
        global = env->NewGlobalRef(retObj);
        env->DeleteLocalRef(retObj);
    } else if (E_OUTOFMEMORY == OLE_HR) {
        throw std::bad_alloc();
    }
    return global;
}

jobject AwtDropTarget::GetData(jlong fmt)
{
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return (jobject)NULL;
    }
    jobject ret = NULL;
    OLE_TRY
    STGMEDIUM stgmedium;
    OLE_HRT( ExtractNativeData(fmt, -1, &stgmedium) );
    OLE_NEXT_TRY
        if (CF_FILEGROUPDESCRIPTORA == fmt ||
            CF_FILEGROUPDESCRIPTORW == fmt)
        {
            ret = ConvertMemoryMappedData(env, fmt, &stgmedium);
        } else {
            ret = ConvertNativeData(env, fmt, &stgmedium);
        }
    OLE_CATCH_BAD_ALLOC
    ::ReleaseStgMedium(&stgmedium);
    OLE_CATCH
    if (E_OUTOFMEMORY == OLE_HR) {
        throw std::bad_alloc();
    }
    return ret;
}

/**
 *
 */

int __cdecl AwtDropTarget::_compar(const void* first, const void* second) {
    FORMATETC *fp = (FORMATETC *)first;
    FORMATETC *sp = (FORMATETC *)second;

    if (fp->cfFormat == sp->cfFormat) {
        return fp->tymed - sp->tymed;
    }

    return fp->cfFormat - sp->cfFormat;
}

const unsigned int AwtDropTarget::CACHE_INCR = 16;

void AwtDropTarget::LoadCache(IDataObject* pDataObj) {
    JNIEnv*      env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    unsigned int cnt = 0;
    HRESULT      res;
    IEnumFORMATETC* pEnumFormatEtc = NULL;

    if (m_dataObject != (IDataObject*)NULL) UnloadCache();

    if (!IsLocalDnD()) {
        SetCurrentDnDDataObject(pDataObj);
    }

    (m_dataObject = pDataObj)->AddRef();

    res = m_dataObject->EnumFormatEtc(DATADIR_GET, &pEnumFormatEtc);

    if (res == S_OK) {
    for (;;) {

        FORMATETC tmp;
        ULONG     actual = 1;

            res = pEnumFormatEtc->Next((ULONG)1, &tmp, &actual);
            if (res == S_FALSE)
                break;

        if (!(tmp.cfFormat  >= 1                &&
              tmp.ptd       == NULL             &&
                (tmp.lindex == -1 || CF_FILECONTENTS==tmp.cfFormat) &&
              tmp.dwAspect  == DVASPECT_CONTENT &&
                ( tmp.tymed == TYMED_HGLOBAL ||
               tmp.tymed    == TYMED_FILE       ||
               tmp.tymed    == TYMED_ISTREAM    ||
               tmp.tymed    == TYMED_GDI        ||
               tmp.tymed    == TYMED_MFPICT     ||
               tmp.tymed    == TYMED_ENHMF
              ) // but not ISTORAGE
             )
            )
                continue;

        if (m_dataObject->QueryGetData(&tmp) != S_OK) continue;

        if (m_nformats % CACHE_INCR == 0) {
            m_formats = (FORMATETC *)SAFE_SIZE_ARRAY_REALLOC(safe_Realloc, m_formats,
                                                  CACHE_INCR + m_nformats,
                                                  sizeof(FORMATETC));
        }

        memcpy(m_formats + m_nformats, &tmp, sizeof(FORMATETC));

        m_nformats++;
    }

        // We are responsible for releasing the enumerator.
        pEnumFormatEtc->Release();
    }

    if (m_nformats > 0) {
        qsort((void*)m_formats, m_nformats, sizeof(FORMATETC),
              AwtDropTarget::_compar);
    }

    if (m_cfFormats != NULL) {
        env->DeleteGlobalRef(m_cfFormats);
    }
    jlongArray l_cfFormats = env->NewLongArray(m_nformats);
    if (l_cfFormats == NULL) {
        throw std::bad_alloc();
    }
    m_cfFormats = (jlongArray)env->NewGlobalRef(l_cfFormats);
    env->DeleteLocalRef(l_cfFormats);

    jboolean isCopy;
    jlong *lcfFormats = env->GetLongArrayElements(m_cfFormats, &isCopy),
        *saveFormats = lcfFormats;

    for (unsigned int i = 0; i < m_nformats; i++, lcfFormats++) {
        *lcfFormats = m_formats[i].cfFormat;
    }

    env->ReleaseLongArrayElements(m_cfFormats, saveFormats, 0);
}

/**
 * UnloadCache
 */

void AwtDropTarget::UnloadCache() {
    if (m_dataObject == (IDataObject*)NULL) return;

    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    free((void*)m_formats);
    m_formats  = (FORMATETC *)NULL;
    m_nformats = 0;

    // fix for 6212440: on application shutdown, this object's
    // destruction might be suppressed due to dangling COM references.
    // This method is called from the destructor.
    // On destruction, VM might be shut down already, so we should make
    // a null check on env.
    if (env) {
        env->DeleteGlobalRef(m_cfFormats);
    }
    m_cfFormats = NULL;

    if (!IsLocalDnD()) {
        DASSERT(IsCurrentDnDDataObject(m_dataObject));
        SetCurrentDnDDataObject(NULL);
    }

    m_dataObject->Release();
    m_dataObject = (IDataObject*)NULL;
}

/**
 * DragCleanup
 */

void AwtDropTarget::DragCleanup(void) {
    UnloadCache();
    AwtToolkit::GetInstance().isInDoDragDropLoop = FALSE;
}

BOOL AwtDropTarget::IsLocalDataObject(IDataObject __RPC_FAR *pDataObject) {
    BOOL local = FALSE;

    if (pDataObject != NULL) {
        FORMATETC format;
        STGMEDIUM stgmedium;

        format.cfFormat = AwtDragSource::PROCESS_ID_FORMAT;
        format.ptd      = NULL;
        format.dwAspect = DVASPECT_CONTENT;
        format.lindex   = -1;
        format.tymed    = TYMED_HGLOBAL;

        if (pDataObject->GetData(&format, &stgmedium) == S_OK) {
            ::SetLastError(0); // clear error
            // Warning C4244.
            SIZE_T size = ::GlobalSize(stgmedium.hGlobal);
            if (size < sizeof(DWORD) || ::GetLastError() != 0) {
                ::SetLastError(0); // clear error
            } else {

                DWORD id = ::CoGetCurrentProcess();

                LPVOID data = ::GlobalLock(stgmedium.hGlobal);
                if (memcmp(data, &id, sizeof(id)) == 0) {
                    local = TRUE;
                }
                ::GlobalUnlock(stgmedium.hGlobal);
            }
            ::ReleaseStgMedium(&stgmedium);
        }
    }

    return local;
}

DECLARE_JAVA_CLASS(dTCClazz, "sun/awt/windows/WDropTargetContextPeer")

jobject
AwtDropTarget::call_dTCcreate(JNIEnv* env) {
    DECLARE_STATIC_OBJECT_JAVA_METHOD(dTCcreate, dTCClazz,
                                      "getWDropTargetContextPeer",
                                      "()Lsun/awt/windows/WDropTargetContextPeer;");
    return env->CallStaticObjectMethod(clazz, dTCcreate);
}

jint
AwtDropTarget::call_dTCenter(JNIEnv* env, jobject self, jobject component, jint x, jint y,
              jint dropAction, jint actions, jlongArray formats,
              jlong nativeCtxt) {
    DECLARE_JINT_JAVA_METHOD(dTCenter, dTCClazz, "handleEnterMessage",
                            "(Ljava/awt/Component;IIII[JJ)I");
    DASSERT(!JNU_IsNull(env, self));
    return env->CallIntMethod(self, dTCenter, component, x, y, dropAction,
                              actions, formats, nativeCtxt);
}

void
AwtDropTarget::call_dTCexit(JNIEnv* env, jobject self, jobject component, jlong nativeCtxt) {
    DECLARE_VOID_JAVA_METHOD(dTCexit, dTCClazz, "handleExitMessage",
                            "(Ljava/awt/Component;J)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dTCexit, component, nativeCtxt);
}

jint
AwtDropTarget::call_dTCmotion(JNIEnv* env, jobject self, jobject component, jint x, jint y,
               jint dropAction, jint actions, jlongArray formats,
               jlong nativeCtxt) {
    DECLARE_JINT_JAVA_METHOD(dTCmotion, dTCClazz, "handleMotionMessage",
                            "(Ljava/awt/Component;IIII[JJ)I");
    DASSERT(!JNU_IsNull(env, self));
    return env->CallIntMethod(self, dTCmotion, component, x, y,
                                 dropAction, actions, formats, nativeCtxt);
}

void
AwtDropTarget::call_dTCdrop(JNIEnv* env, jobject self, jobject component, jint x, jint y,
             jint dropAction, jint actions, jlongArray formats,
             jlong nativeCtxt) {
    DECLARE_VOID_JAVA_METHOD(dTCdrop, dTCClazz, "handleDropMessage",
                            "(Ljava/awt/Component;IIII[JJ)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dTCdrop, component, x, y,
                           dropAction, actions, formats, nativeCtxt);
}

jobject
AwtDropTarget::call_dTCgetfs(JNIEnv* env, jstring fileName, jlong stgmedium) {
    DECLARE_STATIC_OBJECT_JAVA_METHOD(dTCgetfs, dTCClazz, "getFileStream",
                                      "(Ljava/lang/String;J)Ljava/io/FileInputStream;");
    return env->CallStaticObjectMethod(clazz, dTCgetfs, fileName, stgmedium);
}

jobject
AwtDropTarget::call_dTCgetis(JNIEnv* env, jlong istream) {
    DECLARE_STATIC_OBJECT_JAVA_METHOD(dTCgetis, dTCClazz, "getIStream",
                                      "(J)Ljava/lang/Object;");
    return env->CallStaticObjectMethod(clazz, dTCgetis, istream);
}

/*****************************************************************************/

/**
 * construct a wrapper
 */

WDTCPIStreamWrapper::WDTCPIStreamWrapper(STGMEDIUM* stgmedium) {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    m_stgmedium = *stgmedium;
    m_istream   = stgmedium->pstm;
    m_istream->AddRef();
    m_mutex     = ::CreateMutex(NULL, FALSE, NULL);
}

/**
 * destroy a wrapper
 */

WDTCPIStreamWrapper::~WDTCPIStreamWrapper() {
    ::CloseHandle(m_mutex);
    m_istream->Release();
    ::ReleaseStgMedium(&m_stgmedium);
}

/**
 * return available data
 */

jint WDTCPIStreamWrapper::DoAvailable(WDTCPIStreamWrapper* istream) {
    WDTCPIStreamWrapperRec iswr = { istream, 0 };

    AwtToolkit::GetInstance().WaitForSingleObject(istream->m_mutex);

    AwtToolkit::GetInstance().InvokeFunctionLater( _Available, &iswr);

    istream->WaitUntilSignalled(FALSE);

    return iswr.ret;
}

/**
 * return available data
 */

void WDTCPIStreamWrapper::_Available(void *param) {
    WDTCPIStreamWrapperPtr iswrp = (WDTCPIStreamWrapperPtr)param;

    iswrp->ret = (iswrp->istream)->Available();

    iswrp->istream->Signal();
}

/**
 * return available data
 */

jint WDTCPIStreamWrapper::Available() {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (m_istream->Stat(&m_statstg, STATFLAG_NONAME) != S_OK) {
        JNU_ThrowIOException(env, "IStream::Stat() failed");
        return 0;
    }

    if (m_statstg.cbSize.QuadPart > 0x7ffffffL) {
        JNU_ThrowIOException(env, "IStream::Stat() cbSize > 0x7ffffff");
        return 0;
    }

    return (jint)m_statstg.cbSize.LowPart;
}

/**
 * read 1 byte
 */

jint WDTCPIStreamWrapper::DoRead(WDTCPIStreamWrapper* istream) {
    WDTCPIStreamWrapperRec iswr = { istream, 0 };

    AwtToolkit::GetInstance().WaitForSingleObject(istream->m_mutex);

    AwtToolkit::GetInstance().InvokeFunctionLater(_Read, &iswr);

    istream->WaitUntilSignalled(FALSE);

    return iswr.ret;
}

/**
 * read 1 byte
 */

void WDTCPIStreamWrapper::_Read(void* param) {
    WDTCPIStreamWrapperPtr iswrp = (WDTCPIStreamWrapperPtr)param;

    iswrp->ret = (iswrp->istream)->Read();

    iswrp->istream->Signal();
}

/**
 * read 1 byte
 */

jint WDTCPIStreamWrapper::Read() {
    JNIEnv* env    = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jint    b      = 0;
    ULONG   actual = 0;
    HRESULT res;

    switch (res = m_istream->Read((void *)&b, (ULONG)1, &actual)) {
        case S_FALSE:
            return (jint)-1;

        case S_OK:
            return (jint)(actual == 0 ? -1 : b);

        default:
            JNU_ThrowIOException(env, "IStream::Read failed");
    }
    return (jint)-1;
}

/**
 * read Buffer
 */

jint WDTCPIStreamWrapper::DoReadBytes(WDTCPIStreamWrapper* istream, jbyteArray array, jint off, jint len) {
    WDTCPIStreamWrapperReadBytesRec iswrbr = { istream, 0, array, off, len };

    AwtToolkit::GetInstance().WaitForSingleObject(istream->m_mutex);

    AwtToolkit::GetInstance().InvokeFunctionLater(_ReadBytes, &iswrbr);

    istream->WaitUntilSignalled(FALSE);

    return iswrbr.ret;
}

/**
 * read buffer
 */

void WDTCPIStreamWrapper::_ReadBytes(void*  param) {
    WDTCPIStreamWrapperReadBytesPtr iswrbrp =
        (WDTCPIStreamWrapperReadBytesPtr)param;

    iswrbrp->ret = (iswrbrp->istream)->ReadBytes(iswrbrp->array,
                                                 iswrbrp->off,
                                                 iswrbrp->len);
    iswrbrp->istream->Signal();
}

/**
 * read buffer
 */

jint WDTCPIStreamWrapper::ReadBytes(jbyteArray buf, jint off, jint len) {
    JNIEnv*  env     = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jboolean isCopy  = JNI_FALSE;
    ULONG    actual  = 0;
    jbyte*   local   = env->GetByteArrayElements(buf, &isCopy);
    HRESULT  res;
    CHECK_NULL_RETURN(local, (jint)-1);

    switch (res = m_istream->Read((void *)(local + off), (ULONG)len, &actual)) {
        case S_FALSE:
        case S_OK: {
            int eof = (actual == 0);

            env->ReleaseByteArrayElements(buf, local, !eof ? 0 : JNI_ABORT);
            return (jint)(!eof ? actual : -1);
        }

        default:
            env->ReleaseByteArrayElements(buf, local, JNI_ABORT);
            JNU_ThrowIOException(env, "IStream::Read failed");
    }

    return (jint)-1;
}

/**
 * close
 */

void WDTCPIStreamWrapper::DoClose(WDTCPIStreamWrapper* istream) {
    AwtToolkit::GetInstance().InvokeFunctionLater(_Close, istream);
}

/**
 * close
 */

void WDTCPIStreamWrapper::_Close(void* param) {
    ((WDTCPIStreamWrapper*)param)->Close();
}

/**
 * close
 */

void WDTCPIStreamWrapper::Close() {
    delete this;
}

/*****************************************************************************/

extern "C" {

/**
 * awt_dnd_initialize: initial DnD system
 */

void awt_dnd_initialize() {
    ::OleInitialize((LPVOID)NULL);
}

/**
 * awt_dnd_uninitialize: deactivate DnD system
 */

void awt_dnd_uninitialize() {
    ::OleUninitialize();
}

/**
 * convertActionsToDROPEFFECT
 */

DWORD convertActionsToDROPEFFECT(jint actions) {
    DWORD effects = DROPEFFECT_NONE;

    if (actions & java_awt_dnd_DnDConstants_ACTION_LINK) effects |= DROPEFFECT_LINK;
    if (actions & java_awt_dnd_DnDConstants_ACTION_MOVE) effects |= DROPEFFECT_MOVE;
    if (actions & java_awt_dnd_DnDConstants_ACTION_COPY) effects |= DROPEFFECT_COPY;
    return effects;
}

/**
 * convertDROPEFFECTToAction
 */

jint convertDROPEFFECTToActions(DWORD effects) {
    jint actions = java_awt_dnd_DnDConstants_ACTION_NONE;

    if (effects & DROPEFFECT_LINK) actions |= java_awt_dnd_DnDConstants_ACTION_LINK;
    if (effects & DROPEFFECT_MOVE) actions |= java_awt_dnd_DnDConstants_ACTION_MOVE;
    if (effects & DROPEFFECT_COPY) actions |= java_awt_dnd_DnDConstants_ACTION_COPY;

    return actions;
}

/**
 * map keyboard modifiers to a DROPEFFECT
 */

DWORD mapModsToDROPEFFECT(DWORD effects, DWORD mods) {
    DWORD ret = DROPEFFECT_NONE;

    /*
     * Fix for 4285634.
     * Calculate the drop action to match Motif DnD behavior.
     * If the user selects an operation (by pressing a modifier key),
     * return the selected operation or DROPEFFECT_NONE if the selected
     * operation is not supported by the drag source.
     * If the user doesn't select an operation search the set of operations
     * supported by the drag source for DROPEFFECT_MOVE, then for
     * DROPEFFECT_COPY, then for DROPEFFECT_LINK and return the first operation
     * found.
     */
    switch (mods & (MK_CONTROL | MK_SHIFT)) {
        case MK_CONTROL:
            ret = DROPEFFECT_COPY;
        break;

        case MK_CONTROL | MK_SHIFT:
            ret = DROPEFFECT_LINK;
        break;

        case MK_SHIFT:
            ret = DROPEFFECT_MOVE;
        break;

        default:
            if (effects & DROPEFFECT_MOVE) {
                ret = DROPEFFECT_MOVE;
            } else if (effects & DROPEFFECT_COPY) {
                ret = DROPEFFECT_COPY;
            } else if (effects & DROPEFFECT_LINK) {
                ret = DROPEFFECT_LINK;
            }
            break;
    }

    return ret & effects;
}

/**
 * downcall to fetch data ... gets scheduled on message thread
 */

JNIEXPORT jobject JNICALL Java_sun_awt_windows_WDropTargetContextPeer_getData(JNIEnv* env, jobject self, jlong dropTarget, jlong format) {
    TRY;

    AwtDropTarget* pDropTarget = (AwtDropTarget*)dropTarget;

    DASSERT(!::IsBadReadPtr(pDropTarget, sizeof(AwtDropTarget)));
    return pDropTarget->DoGetData(format);

    CATCH_BAD_ALLOC_RET(NULL);
}

/**
 * downcall to signal drop done ... gets scheduled on message thread
 */

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDropTargetContextPeer_dropDone(JNIEnv* env, jobject self,
                             jlong dropTarget, jboolean success, jint actions) {
    TRY_NO_HANG;

    AwtDropTarget* pDropTarget = (AwtDropTarget*)dropTarget;

    DASSERT(!::IsBadReadPtr(pDropTarget, sizeof(AwtDropTarget)));
    pDropTarget->DoDropDone(success, actions);

    CATCH_BAD_ALLOC;
}

/**
 * downcall to free up storage medium for FileStream
 */

JNIEXPORT void JNICALL Java_sun_awt_windows_WDropTargetContextPeerFileStream_freeStgMedium(JNIEnv* env, jobject self, jlong stgmedium) {
    TRY;

    ::ReleaseStgMedium((STGMEDIUM*)stgmedium);

    free((void*)stgmedium);

    CATCH_BAD_ALLOC;
}

/**
 *
 */

JNIEXPORT jint JNICALL Java_sun_awt_windows_WDropTargetContextPeerIStream_Available(JNIEnv* env, jobject self, jlong istream) {
    TRY;

    return WDTCPIStreamWrapper::DoAvailable((WDTCPIStreamWrapper*)istream);

    CATCH_BAD_ALLOC_RET(0);
}

/**
 *
 */

JNIEXPORT jint JNICALL Java_sun_awt_windows_WDropTargetContextPeerIStream_Read(JNIEnv* env, jobject self, jlong istream) {
    TRY;

    return WDTCPIStreamWrapper::DoRead((WDTCPIStreamWrapper*)istream);

    CATCH_BAD_ALLOC_RET(0);
}

/**
 *
 */

JNIEXPORT jint JNICALL Java_sun_awt_windows_WDropTargetContextPeerIStream_ReadBytes(JNIEnv* env, jobject self, jlong istream, jbyteArray buf, jint off, jint len) {
    TRY;

    return WDTCPIStreamWrapper::DoReadBytes((WDTCPIStreamWrapper*)istream, buf, off, len);

    CATCH_BAD_ALLOC_RET(0);
}

/**
 *
 */

JNIEXPORT void JNICALL Java_sun_awt_windows_WDropTargetContextPeerIStream_Close(JNIEnv* env, jobject self, jlong istream) {
    TRY_NO_VERIFY;

    WDTCPIStreamWrapper::DoClose((WDTCPIStreamWrapper*)istream);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
