/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#pragma push_macro("bad_alloc")
//"bad_alloc" would be introduced in STL as "std::zbad_alloc" and discarded by linker
//by this action we avoid the conflict with AWT implementation of "bad_alloc"
//we need <new> inclusion for STL "new" oprators set.
#define bad_alloc zbad_alloc
#include <new>

#if defined(_DEBUG) || defined(DEBUG)
extern void * operator new(size_t size, const char * filename, int linenumber);
void * operator new(size_t size) {return operator new(size, "stl", 1);}
#endif
#include <map>

#pragma pop_macro("bad_alloc")
//"bad_alloc" is undefined from here

#include <awt.h>
#include <shlobj.h>

#include "jlong.h"
#include "awt_DataTransferer.h"
#include "awt_DnDDS.h"
#include "awt_DnDDT.h"
#include "awt_Cursor.h"
#include "awt_Toolkit.h"
#include "awt_Component.h"

#include "java_awt_event_InputEvent.h"
#include "java_awt_dnd_DnDConstants.h"
#include "sun_awt_windows_WDragSourceContextPeer.h"

#include "awt_ole.h"
#include "awt_DCHolder.h"

bool operator < (const FORMATETC &fr, const FORMATETC &fl) {
    return memcmp(&fr, &fl, sizeof(FORMATETC)) < 0;
}

typedef std::map<FORMATETC, STGMEDIUM> CDataMap;

#define GALLOCFLG (GMEM_DDESHARE | GMEM_MOVEABLE | GMEM_ZEROINIT)
#define JAVA_BUTTON_MASK (java_awt_event_InputEvent_BUTTON1_DOWN_MASK | \
                          java_awt_event_InputEvent_BUTTON2_DOWN_MASK | \
                          java_awt_event_InputEvent_BUTTON3_DOWN_MASK)

extern "C" {
DWORD __cdecl convertActionsToDROPEFFECT(jint actions);
jint  __cdecl convertDROPEFFECTToActions(DWORD effects);
}

class PictureDragHelper
{
private:
    static CDataMap st;
    static IDragSourceHelper *pHelper;
public:
    static HRESULT Create(
        JNIEnv* env,
        jintArray imageData,
        int imageWidth,
        int imageHeight,
        int anchorX,
        int anchorY,
        IDataObject *pIDataObject)
    {
        if (NULL == imageData) {
            return S_FALSE;
        }
        OLE_TRY
        OLE_HRT( CoCreateInstance(
            CLSID_DragDropHelper,
            NULL,
            CLSCTX_ALL,
            IID_IDragSourceHelper,
            (LPVOID*)&pHelper))

        jintArray ia = imageData;
        jsize iPointCoint = env->GetArrayLength(ia);

        DCHolder ph;
        ph.Create(NULL, imageWidth, imageHeight, TRUE);
        env->GetIntArrayRegion(ia, 0, iPointCoint, (jint*)ph.m_pPoints);

        SHDRAGIMAGE sdi;
        sdi.sizeDragImage.cx = imageWidth;
        sdi.sizeDragImage.cy = imageHeight;
        sdi.ptOffset.x = anchorX;
        sdi.ptOffset.y = anchorY;
        sdi.crColorKey = 0xFFFFFFFF;
        sdi.hbmpDragImage = ph;

        // this call assures that the bitmap will be dragged around
        OLE_HR = pHelper->InitializeFromBitmap(
            &sdi,
            pIDataObject
        );
        // in case of an error we need to destroy the image, else the helper object takes ownership
        if (FAILED(OLE_HR)) {
            DeleteObject(sdi.hbmpDragImage);
        }
        OLE_CATCH
        OLE_RETURN_HR
    }

    static void Destroy()
    {
        if (NULL!=pHelper) {
            CleanFormatMap();
            pHelper->Release();
            pHelper = NULL;
        }
    }

    static void CleanFormatMap()
    {
        for (CDataMap::iterator i = st.begin(); st.end() != i; i = st.erase(i)) {
            ::ReleaseStgMedium(&i->second);
        }
    }
    static void SetData(const FORMATETC &format, const STGMEDIUM &medium)
    {
        CDataMap::iterator i = st.find(format);
        if (st.end() != i) {
            ::ReleaseStgMedium(&i->second);
            i->second = medium;
        } else {
            st[format] = medium;
        }
    }
    static const FORMATETC *FindFormat(const FORMATETC &format)
    {
        static FORMATETC fm = {0};
        CDataMap::iterator i = st.find(format);
        if (st.end() != i) {
            return &i->first;
        }
        for (i = st.begin(); st.end() != i; ++i) {
            if (i->first.cfFormat==format.cfFormat) {
                return &i->first;
            }
        }
        return NULL;
    }
    static STGMEDIUM *FindData(const FORMATETC &format)
    {
        CDataMap::iterator i = st.find(format);
        if (st.end() != i) {
            return &i->second;
        }
        for (i = st.begin(); st.end() != i; ++i) {
            const FORMATETC &f = i->first;
            if (f.cfFormat==format.cfFormat && (f.tymed == (f.tymed & format.tymed))) {
                return &i->second;
            }
        }
        return NULL;
    }
};


CDataMap PictureDragHelper::st;
IDragSourceHelper *PictureDragHelper::pHelper = NULL;

extern const CLIPFORMAT CF_PERFORMEDDROPEFFECT = ::RegisterClipboardFormat(CFSTR_PERFORMEDDROPEFFECT);
extern const CLIPFORMAT CF_FILEGROUPDESCRIPTORW = ::RegisterClipboardFormat(CFSTR_FILEDESCRIPTORW);
extern const CLIPFORMAT CF_FILEGROUPDESCRIPTORA = ::RegisterClipboardFormat(CFSTR_FILEDESCRIPTORA);
extern const CLIPFORMAT CF_FILECONTENTS = ::RegisterClipboardFormat(CFSTR_FILECONTENTS);

typedef struct {
    AwtDragSource* dragSource;
    jobject        cursor;
    jintArray      imageData;
    jint           imageWidth;
    jint           imageHeight;
    jint           x;
    jint           y;
} StartDragRec;

/**
 * StartDrag
 */

void AwtDragSource::StartDrag(
    AwtDragSource* self,
    jobject cursor,
    jintArray imageData,
    jint imageWidth,
    jint imageHeight,
    jint x,
    jint y)
{
    StartDragRec* sdrp = new StartDragRec;
    sdrp->dragSource = self;
    sdrp->imageData = imageData;
    sdrp->cursor = cursor;
    sdrp->imageWidth = imageWidth;
    sdrp->imageHeight = imageHeight;
    sdrp->x = x;
    sdrp->y = y;

    AwtToolkit::GetInstance().WaitForSingleObject(self->m_mutex);

    AwtToolkit::GetInstance().InvokeFunctionLater((void (*)(void *))&AwtDragSource::_DoDragDrop, (void *)sdrp);

    self->WaitUntilSignalled(FALSE);
}

/**
 * DoDragDrop - called from message pump thread
 */

void AwtDragSource::_DoDragDrop(void* param) {
    StartDragRec*  sdrp         = (StartDragRec*)param;
    AwtDragSource* dragSource   = sdrp->dragSource;
    DWORD          effects      = DROPEFFECT_NONE;
    JNIEnv*        env          = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject        peer         = env->NewLocalRef(dragSource->GetPeer());

    if (sdrp->imageData) {
        PictureDragHelper::Create(
            env,
            sdrp->imageData,
            sdrp->imageWidth,
            sdrp->imageHeight,
            sdrp->x,
            sdrp->y,
            (IDataObject*)dragSource);
        env->DeleteGlobalRef(sdrp->imageData);
    }
    dragSource->SetCursor(sdrp->cursor);
    env->DeleteGlobalRef(sdrp->cursor);
    delete sdrp;

    HRESULT        res;

    // StartDrag has caused dragSource->m_mutex to be held by our thread now

    AwtDropTarget::SetCurrentDnDDataObject(dragSource);

    ::GetCursorPos(&dragSource->m_dragPoint);

    dragSource->Signal();

    AwtToolkit &toolkit = AwtToolkit::GetInstance();
    toolkit.isInDoDragDropLoop = TRUE;
    res = ::DoDragDrop(dragSource,
                       dragSource,
                       convertActionsToDROPEFFECT(dragSource->m_actions),
                       &effects
          );
    toolkit.isInDoDragDropLoop = FALSE;

    if (effects == DROPEFFECT_NONE && dragSource->m_dwPerformedDropEffect != DROPEFFECT_NONE) {
        effects = dragSource->m_dwPerformedDropEffect;
    }
    dragSource->m_dwPerformedDropEffect = DROPEFFECT_NONE;

    call_dSCddfinished(env, peer, res == DRAGDROP_S_DROP && effects != DROPEFFECT_NONE,
                       convertDROPEFFECTToActions(effects),
                       dragSource->m_dragPoint);

    env->DeleteLocalRef(peer);

    DASSERT(AwtDropTarget::IsCurrentDnDDataObject(dragSource));
    AwtDropTarget::SetCurrentDnDDataObject(NULL);

    PictureDragHelper::Destroy();
    dragSource->Release();
}

/**
 * constructor
 */

AwtDragSource::AwtDragSource(JNIEnv* env, jobject peer, jobject component,
                             jobject transferable, jobject trigger,
                             jint actions, jlongArray formats,
                             jobject formatMap) {
    m_peer      = env->NewGlobalRef(peer);

    m_refs      = 1;

    m_actions   = actions;

    m_ntypes    = 0;

    m_initmods  = 0;
    m_lastmods  = 0;

    m_droptarget   = NULL;
    m_enterpending = TRUE;

    m_cursor     = NULL;

    m_mutex      = ::CreateMutex(NULL, FALSE, NULL);

    m_component     = env->NewGlobalRef(component);
    m_transferable  = env->NewGlobalRef(transferable);
    m_formatMap     = env->NewGlobalRef(formatMap);

    m_dragPoint.x = 0;
    m_dragPoint.y = 0;

    m_fNC         = TRUE;
    m_dropPoint.x = 0;
    m_dropPoint.y = 0;

    m_dwPerformedDropEffect = DROPEFFECT_NONE;
    m_bRestoreNodropCustomCursor = FALSE;

    LoadCache(formats);
}

/**
 * destructor
 */

AwtDragSource::~AwtDragSource() {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // fix for 6212440: on application shutdown, this object's
    // destruction might be suppressed due to dangling COM references.
    // On destruction, VM might be shut down already, so we should make
    // a null check on env.
    if (env) {
        env->DeleteGlobalRef(m_peer);
        env->DeleteGlobalRef(m_component);
        env->DeleteGlobalRef(m_transferable);
        env->DeleteGlobalRef(m_formatMap);
    }

    ::CloseHandle(m_mutex);

    UnloadCache();
}

/**
 * _compar
 *
 * compare format's then tymed's .... only one tymed bit may be set
 * at any time in a FORMATETC in the cache.
 */

int AwtDragSource::_compar(const void* first, const void* second) {
    FORMATETC *fp = (FORMATETC *)first;
    FORMATETC *sp = (FORMATETC *)second;
    int      r  = fp->cfFormat - sp->cfFormat;

    return r != 0 ? r : fp->tymed - sp->tymed;
}

/**
 * LoadCache
 */

void AwtDragSource::LoadCache(jlongArray formats) {
    JNIEnv*      env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    unsigned int items = 0;
    unsigned int i = 0;
    unsigned int idx = 0;

    if (m_types != (FORMATETC *)NULL) {
        UnloadCache();
    }

    items = env->GetArrayLength(formats);

    if (items == 0) {
        return;
    }

    jlong *lFormats = env->GetLongArrayElements(formats, 0),
        *saveFormats = lFormats;
    if (lFormats == NULL) {
        m_ntypes = 0;
        return;
    }

    for (i = 0, m_ntypes = 0; i < items; i++, lFormats++) {
        // Warning C4244.
        // Cast from jlong to CLIPFORMAT (WORD).
        CLIPFORMAT fmt = (CLIPFORMAT)*lFormats;
        switch (fmt) {
        case CF_ENHMETAFILE:
            m_ntypes++;    // Only TYMED_ENHMF
            break;
        case CF_METAFILEPICT:
            m_ntypes++;    // Only TYMED_MFPICT
            break;
        case CF_HDROP:
            m_ntypes++;    // Only TYMED_HGLOBAL
            break;
        default:
            m_ntypes += 2; // TYMED_HGLOBAL and TYMED_ISTREAM
            break;
        }
    }

    try {
        m_types = (FORMATETC *)safe_Calloc(sizeof(FORMATETC), m_ntypes);
    } catch (std::bad_alloc&) {
        m_ntypes = 0;
        env->ReleaseLongArrayElements(formats, saveFormats, 0);
        throw;
    }

    lFormats = saveFormats;

    for (i = 0, idx = 0; i < items; i++, lFormats++) {
        // Warning C4244.
        // Cast from jlong to CLIPFORMAT (WORD).
        CLIPFORMAT fmt = (CLIPFORMAT)*lFormats;

        m_types[idx].cfFormat = fmt;
        m_types[idx].dwAspect = DVASPECT_CONTENT;
        m_types[idx].lindex   = -1;

        switch (fmt) {
        default:
            m_types[idx].tymed = TYMED_ISTREAM;
            idx++;

            // now make a copy, but with a TYMED of HGLOBAL
            m_types[idx] = m_types[idx-1];
            m_types[idx].tymed = TYMED_HGLOBAL;
            idx++;
            break;
        case CF_HDROP:
            m_types[idx].tymed = TYMED_HGLOBAL;
            idx++;
            break;
        case CF_ENHMETAFILE:
            m_types[idx].tymed = TYMED_ENHMF;
            idx++;
            break;
        case CF_METAFILEPICT:
            m_types[idx].tymed = TYMED_MFPICT;
            idx++;
            break;
        }
    }
    DASSERT(idx == m_ntypes);

    env->ReleaseLongArrayElements(formats, saveFormats, 0);

    // sort them in ascending order of format
    qsort((void *)m_types, (size_t)m_ntypes, (size_t)sizeof(FORMATETC),
          _compar);
}

/**
 * UnloadCache
 */

void AwtDragSource::UnloadCache() {
    if (m_ntypes == 0) {
        return;
    }

    free((void*)m_types);
    m_ntypes = 0;
    m_types  = (FORMATETC *)NULL;
}

/**
 * ChangeCursor
 */
HRESULT AwtDragSource::ChangeCursor()
{
    if (m_cursor != NULL) {
        ::SetCursor(m_cursor->GetHCursor());
        return S_OK;
    }
    return DRAGDROP_S_USEDEFAULTCURSORS;
}

/**
 * SetCursor
 */
void AwtDragSource::SetCursor(jobject cursor) {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (JNU_IsNull(env, cursor)) {
        m_cursor = NULL;
        return;
    }

    jlong pData = env->GetLongField(cursor, AwtCursor::pDataID);
    // Warning C4312.
    // Cast jlong (__int64) to pointer.
    m_cursor = (AwtCursor*)pData;

    if (m_cursor == NULL) {
        m_cursor = AwtCursor::CreateSystemCursor(cursor);
    }
}

/**
 * MatchFormatEtc
 */

HRESULT __stdcall
AwtDragSource::MatchFormatEtc(FORMATETC __RPC_FAR *pFormatEtcIn,
                              FORMATETC *cacheEnt) {
    TRY;

    const FORMATETC *pFormat = PictureDragHelper::FindFormat(*pFormatEtcIn);
    if (NULL != pFormat) {
        if (NULL != cacheEnt) {
            *cacheEnt = *pFormat;
        }
        return S_OK;
    }

    if ((pFormatEtcIn->tymed & (TYMED_HGLOBAL | TYMED_ISTREAM | TYMED_ENHMF |
                                TYMED_MFPICT)) == 0) {
        return DV_E_TYMED;
    } else if (pFormatEtcIn->lindex != -1) {
        return DV_E_LINDEX;
    } else if (pFormatEtcIn->dwAspect != DVASPECT_CONTENT) {
        return DV_E_DVASPECT;
    }

    FORMATETC tmp = *pFormatEtcIn;

    static const DWORD supportedTymeds[] =
        { TYMED_ISTREAM, TYMED_HGLOBAL, TYMED_ENHMF, TYMED_MFPICT };
    static const int nSupportedTymeds = 4;

    for (int i = 0; i < nSupportedTymeds; i++) {
        /*
         * Fix for BugTraq Id 4426805.
         * Match only if the tymed is supported by the requester.
         */
        if ((pFormatEtcIn->tymed & supportedTymeds[i]) == 0) {
            continue;
        }

        tmp.tymed = supportedTymeds[i];
        pFormat = (const FORMATETC *)bsearch((const void *)&tmp,
                                             (const void *)m_types,
                                             (size_t)      m_ntypes,
                                             (size_t)      sizeof(FORMATETC),
                                                           _compar
                                             );
        if (NULL != pFormat) {
            if (cacheEnt != (FORMATETC *)NULL) {
                *cacheEnt = *pFormat;
            }
            return S_OK;
        }
    }

    return DV_E_FORMATETC;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * QueryInterface
 */

HRESULT __stdcall AwtDragSource::QueryInterface(REFIID riid, void __RPC_FAR *__RPC_FAR *ppvObject) {
    TRY;

    if (riid == IID_IUnknown) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IUnknown*)(IDropSource*)this;
        AddRef();
        return S_OK;
    } else if (riid == IID_IDropSource) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IDropSource*)this;
        AddRef();
        return S_OK;
    } else if (riid == IID_IDataObject) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IDataObject*)this;
        AddRef();
        return S_OK;
    } else {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)NULL;
        return E_NOINTERFACE;
    }

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * AddRef
 */

ULONG __stdcall AwtDragSource::AddRef() {
    return (ULONG)++m_refs;
}

/**
 * Release
 */

ULONG __stdcall AwtDragSource::Release() {
    int refs;

    if ((refs = --m_refs) == 0) delete this;

    return (ULONG)refs;
}

/**
 * QueryContinueDrag
 */

HRESULT __stdcall  AwtDragSource::QueryContinueDrag(BOOL fEscapeKeyPressed, DWORD grfKeyState) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    JNIEnv* env       = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (fEscapeKeyPressed)
        return DRAGDROP_S_CANCEL;

    jint modifiers = AwtComponent::GetJavaModifiers();

    POINT dragPoint;

    ::GetCursorPos(&dragPoint);

    if ( (dragPoint.x != m_dragPoint.x || dragPoint.y != m_dragPoint.y) &&
         m_lastmods == modifiers) {//cannot move before cursor change
        call_dSCmouseMoved(env, m_peer, m_actions, modifiers, dragPoint);
        JNU_CHECK_EXCEPTION_RETURN(env, E_UNEXPECTED);
        m_dragPoint = dragPoint;
    }

    if ((modifiers & JAVA_BUTTON_MASK) == 0) {
        return DRAGDROP_S_DROP;
    } else if (m_initmods == 0) {
        m_initmods = modifiers;
    } else if ((modifiers & JAVA_BUTTON_MASK) != (m_initmods & JAVA_BUTTON_MASK)) {
        return DRAGDROP_S_CANCEL;
    } else if (m_lastmods != modifiers) {
        call_dSCchanged(env, m_peer, m_actions, modifiers, dragPoint);
        m_bRestoreNodropCustomCursor = TRUE;
    }

    m_lastmods = modifiers;

    //CR 6480706 - MS Bug on hold
    HCURSOR hNeedCursor;
    if (
        m_bRestoreNodropCustomCursor &&
        m_cursor != NULL &&
        (hNeedCursor = m_cursor->GetHCursor()) != ::GetCursor() )
    {
        ChangeCursor();
        m_bRestoreNodropCustomCursor = FALSE;
    }
    return S_OK;

   CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * GiveFeedback
 */

HRESULT __stdcall  AwtDragSource::GiveFeedback(DWORD dwEffect) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    JNIEnv* env       = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jint    modifiers = 0;
    SHORT   mods = 0;

    m_actions = convertDROPEFFECTToActions(dwEffect);

    if (::GetKeyState(VK_LBUTTON) & 0xff00) {
        mods |= MK_LBUTTON;
    } else if (::GetKeyState(VK_MBUTTON) & 0xff00) {
        mods |= MK_MBUTTON;
    } else if (::GetKeyState(VK_RBUTTON) & 0xff00) {
        mods |= MK_RBUTTON;
    }

    if (::GetKeyState(VK_SHIFT)   & 0xff00)
        mods |= MK_SHIFT;
    if (::GetKeyState(VK_CONTROL) & 0xff00)
        mods |= MK_CONTROL;
    if (::GetKeyState(VK_MENU) & 0xff00)
        mods |= MK_ALT;

    modifiers = AwtComponent::GetJavaModifiers();

    POINT curs;

    ::GetCursorPos(&curs);

    m_droptarget = ::WindowFromPoint(curs);

    int invalid = (dwEffect == DROPEFFECT_NONE);

    if (invalid) {
        // Don't call dragExit if dragEnter and dragOver haven't been called.
        if (!m_enterpending) {
            call_dSCexit(env, m_peer, curs);
        }
        m_droptarget = (HWND)NULL;
        m_enterpending = TRUE;
    } else if (m_droptarget != NULL) {
        (*(m_enterpending ? call_dSCenter : call_dSCmotion))
            (env, m_peer, m_actions, modifiers, curs);

        m_enterpending = FALSE;
    }

    if (m_droptarget != NULL) {
        RECT  rect;
        POINT client = curs;
        VERIFY(::ScreenToClient(m_droptarget, &client));
        VERIFY(::GetClientRect(m_droptarget, &rect));
        if (::PtInRect(&rect, client)) {
            m_fNC = FALSE;
            m_dropPoint = client;
        } else {
            m_fNC = TRUE;
            m_dropPoint = curs;
        }
    } else {
        m_fNC = TRUE;
        m_dropPoint.x = 0;
        m_dropPoint.y = 0;
    }

    m_bRestoreNodropCustomCursor = (dwEffect == DROPEFFECT_NONE);

    return ChangeCursor();

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}


/**
 * GetData
 */

HRESULT __stdcall AwtDragSource::GetData(FORMATETC __RPC_FAR *pFormatEtc,
                                         STGMEDIUM __RPC_FAR *pmedium) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;
    STGMEDIUM *pPicMedia = PictureDragHelper::FindData(*pFormatEtc);
    if (NULL != pPicMedia) {
        *pmedium = *pPicMedia;
        //return  outside, so AddRef the instance of pstm or hGlobal!
        if (pmedium->tymed == TYMED_ISTREAM) {
            pmedium->pstm->AddRef();
            pmedium->pUnkForRelease = (IUnknown *)NULL;
        } else if (pmedium->tymed == TYMED_HGLOBAL) {
            AddRef();
            pmedium->pUnkForRelease = (IDropSource *)this;
        }
        return S_OK;
    }

    HRESULT res = GetProcessId(pFormatEtc, pmedium);
    if (res == S_OK) {
        return res;
    }

    FORMATETC matchedFormatEtc;
    res = MatchFormatEtc(pFormatEtc, &matchedFormatEtc);
    if (res != S_OK) {
        return res;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->PushLocalFrame(2) < 0) {
        return E_OUTOFMEMORY;
    }

    jbyteArray bytes =
        AwtDataTransferer::ConvertData(env, m_component, m_transferable,
                                       (jlong)matchedFormatEtc.cfFormat,
                                       m_formatMap);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->PopLocalFrame(NULL);
        return E_UNEXPECTED;
    }
    if (bytes == NULL) {
        env->PopLocalFrame(NULL);
        return E_UNEXPECTED;
    }

    jint nBytes = env->GetArrayLength(bytes);

    if ((matchedFormatEtc.tymed & TYMED_ISTREAM) != 0) {
        ADSIStreamProxy *istream = new ADSIStreamProxy(this, bytes, nBytes);

        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            env->PopLocalFrame(NULL);
            return E_UNEXPECTED;
        }

        pmedium->tymed = TYMED_ISTREAM;
        pmedium->pstm = istream;
        pmedium->pUnkForRelease = (IUnknown *)NULL;

        env->PopLocalFrame(NULL);
        return S_OK;
    } else if ((matchedFormatEtc.tymed & TYMED_HGLOBAL) != 0) {
        HGLOBAL copy = ::GlobalAlloc(GALLOCFLG, nBytes +
                                     ((matchedFormatEtc.cfFormat == CF_HDROP)
                                          ? sizeof(DROPFILES)
                                          : 0));
        if (copy == NULL) {
            env->PopLocalFrame(NULL);
            throw std::bad_alloc();
        }

        char *dataout = (char *)::GlobalLock(copy);

        if (matchedFormatEtc.cfFormat == CF_HDROP) {
            DROPFILES *dropfiles = (DROPFILES *)dataout;
            dropfiles->pFiles = sizeof(DROPFILES);
            dropfiles->pt.x = m_dropPoint.x;
            dropfiles->pt.y = m_dropPoint.y;
            dropfiles->fNC = m_fNC;
            dropfiles->fWide = TRUE; // we publish only Unicode
            dataout += sizeof(DROPFILES);
        }

        env->GetByteArrayRegion(bytes, 0, nBytes, (jbyte *)dataout);
        ::GlobalUnlock(copy);

        pmedium->tymed = TYMED_HGLOBAL;
        pmedium->hGlobal = copy;
        pmedium->pUnkForRelease = (IUnknown *)NULL;

        env->PopLocalFrame(NULL);
        return S_OK;
    } else if ((matchedFormatEtc.tymed & TYMED_ENHMF) != 0) {
        LPBYTE lpbEmfBuffer =
            (LPBYTE)env->GetPrimitiveArrayCritical(bytes, NULL);
        if (lpbEmfBuffer == NULL) {
            env->PopLocalFrame(NULL);
            throw std::bad_alloc();
        }

        HENHMETAFILE hemf = ::SetEnhMetaFileBits(nBytes, lpbEmfBuffer);

        env->ReleasePrimitiveArrayCritical(bytes, (LPVOID)lpbEmfBuffer, JNI_ABORT);

        if (hemf == NULL) {
            env->PopLocalFrame(NULL);
            return E_UNEXPECTED;
        }

        pmedium->tymed = TYMED_ENHMF;
        pmedium->hEnhMetaFile = hemf;
        pmedium->pUnkForRelease = (IUnknown *)NULL;

        env->PopLocalFrame(NULL);
        return S_OK;
    } else if ((matchedFormatEtc.tymed & TYMED_MFPICT) != 0) {
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
            return E_UNEXPECTED;
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

        pmedium->tymed = TYMED_MFPICT;
        pmedium->hMetaFilePict = hmfp;
        pmedium->pUnkForRelease = (IUnknown *)NULL;

        env->PopLocalFrame(NULL);
        return S_OK;
    }

    env->PopLocalFrame(NULL);
    return DV_E_TYMED;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * GetDataHere
 */

HRESULT __stdcall AwtDragSource::GetDataHere(FORMATETC __RPC_FAR *pFormatEtc,
                                             STGMEDIUM __RPC_FAR *pmedium) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    if (pmedium->pUnkForRelease != (IUnknown *)NULL) {
        return E_INVALIDARG;
    }

    HRESULT res = GetProcessId(pFormatEtc, pmedium);
    if (res == S_OK) {
        return res;
    }

    FORMATETC matchedFormatEtc;
    res = MatchFormatEtc(pFormatEtc, &matchedFormatEtc);
    if (res != S_OK) {
        return res;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->PushLocalFrame(2) < 0) {
        return E_OUTOFMEMORY;
    }

    jbyteArray bytes =
        AwtDataTransferer::ConvertData(env, m_component, m_transferable,
                                       (jlong)matchedFormatEtc.cfFormat,
                                       m_formatMap);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->PopLocalFrame(NULL);
        return E_UNEXPECTED;
    }
    if (bytes == NULL) {
        env->PopLocalFrame(NULL);
        return E_UNEXPECTED;
    }

    jint nBytes = env->GetArrayLength(bytes);

    // NOTE: TYMED_ENHMF and TYMED_MFPICT are not valid for GetDataHere().
    if ((matchedFormatEtc.tymed & TYMED_ISTREAM) != 0) {
        jboolean isCopy;
        jbyte *bBytes = env->GetByteArrayElements(bytes, &isCopy);
        if (bBytes == NULL) {
            env->PopLocalFrame(NULL);
            return E_UNEXPECTED;
        }

        ULONG act;
        HRESULT res = pmedium->pstm->Write((const void *)bBytes, (ULONG)nBytes,
                                           &act);

        env->ReleaseByteArrayElements(bytes, bBytes, JNI_ABORT);

        env->PopLocalFrame(NULL);
        return S_OK;
    } else if ((matchedFormatEtc.tymed & TYMED_HGLOBAL) != 0) {
        ::SetLastError(0); // clear error
        // Warning C4244.
        SIZE_T mBytes = ::GlobalSize(pmedium->hGlobal);
        if (::GetLastError() != 0) {
            env->PopLocalFrame(NULL);
            return E_UNEXPECTED;
        }

        if (nBytes + ((matchedFormatEtc.cfFormat == CF_HDROP)
                        ? sizeof(DROPFILES) : 0) > mBytes) {
            env->PopLocalFrame(NULL);
            return STG_E_MEDIUMFULL;
        }

        char *dataout = (char *)::GlobalLock(pmedium->hGlobal);

        if (matchedFormatEtc.cfFormat == CF_HDROP) {
            DROPFILES *dropfiles = (DROPFILES *)dataout;
            dropfiles->pFiles = sizeof(DROPFILES);
            dropfiles->pt.x = m_dropPoint.x;
            dropfiles->pt.y = m_dropPoint.y;
            dropfiles->fNC = m_fNC;
            dropfiles->fWide = TRUE; // good guess!
            dataout += sizeof(DROPFILES);
        }

        env->GetByteArrayRegion(bytes, 0, nBytes, (jbyte *)dataout);
        ::GlobalUnlock(pmedium->hGlobal);

        env->PopLocalFrame(NULL);
        return S_OK;
    }

    env->PopLocalFrame(NULL);
    return DV_E_TYMED;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * QueryGetData
 */

HRESULT __stdcall  AwtDragSource::QueryGetData(FORMATETC __RPC_FAR *pFormatEtc) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    return MatchFormatEtc(pFormatEtc, (FORMATETC *)NULL);

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}


/**
 * GetCanonicalFormatEtc
 */

HRESULT __stdcall  AwtDragSource::GetCanonicalFormatEtc(FORMATETC __RPC_FAR *pFormatEtcIn, FORMATETC __RPC_FAR *pFormatEtcOut) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    HRESULT   res = MatchFormatEtc(pFormatEtcIn, (FORMATETC *)NULL);

    if (res != S_OK) return res;

    *pFormatEtcOut = *pFormatEtcIn;

    pFormatEtcOut->ptd = NULL;

    return DATA_S_SAMEFORMATETC;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * SetData
 */

HRESULT __stdcall AwtDragSource::SetData(FORMATETC __RPC_FAR *pFormatEtc, STGMEDIUM __RPC_FAR *pmedium, BOOL fRelease) {
    AwtToolkit::GetInstance().eventNumber++;
    if (pFormatEtc->cfFormat == CF_PERFORMEDDROPEFFECT && pmedium->tymed == TYMED_HGLOBAL) {
        m_dwPerformedDropEffect = *(DWORD*)::GlobalLock(pmedium->hGlobal);
        ::GlobalUnlock(pmedium->hGlobal);
        if (fRelease) {
            ::ReleaseStgMedium(pmedium);
        }
        return S_OK;
    }

    if (fRelease) {
        //we are copying pmedium as a structure for further use, so no any release!
        PictureDragHelper::SetData(*pFormatEtc, *pmedium);
        return S_OK;
    }
    return E_UNEXPECTED;
}

/**
 * EnumFormatEtc
 */

HRESULT __stdcall  AwtDragSource::EnumFormatEtc(DWORD dwDirection, IEnumFORMATETC *__RPC_FAR *ppenumFormatEtc) {
    AwtToolkit::GetInstance().eventNumber++;
    TRY;

    *ppenumFormatEtc = new ADSIEnumFormatEtc(this);
    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * DAdvise
 */

HRESULT __stdcall  AwtDragSource::DAdvise(FORMATETC __RPC_FAR *pFormatEtc, DWORD advf, IAdviseSink __RPC_FAR *pAdvSink, DWORD __RPC_FAR *pdwConnection) {
    AwtToolkit::GetInstance().eventNumber++;
    return E_NOTIMPL;
}

/**
 * DUnadvise
 */

HRESULT __stdcall  AwtDragSource::DUnadvise(DWORD dwConnection) {
    AwtToolkit::GetInstance().eventNumber++;
    return OLE_E_ADVISENOTSUPPORTED;
}

/**
 * EnumAdvise
 */

HRESULT __stdcall  AwtDragSource::EnumDAdvise(IEnumSTATDATA __RPC_FAR *__RPC_FAR *ppenumAdvise) {
    AwtToolkit::GetInstance().eventNumber++;
    return OLE_E_ADVISENOTSUPPORTED;
}

const UINT AwtDragSource::PROCESS_ID_FORMAT =
    ::RegisterClipboardFormat(TEXT("_SUNW_JAVA_AWT_PROCESS_ID"));

HRESULT __stdcall AwtDragSource::GetProcessId(FORMATETC __RPC_FAR *pFormatEtc, STGMEDIUM __RPC_FAR *pmedium) {
    AwtToolkit::GetInstance().eventNumber++;
    if ((pFormatEtc->tymed & TYMED_HGLOBAL) == 0) {
        return DV_E_TYMED;
    } else if (pFormatEtc->lindex != -1) {
        return DV_E_LINDEX;
    } else if (pFormatEtc->dwAspect != DVASPECT_CONTENT) {
        return DV_E_DVASPECT;
    } else if (pFormatEtc->cfFormat != PROCESS_ID_FORMAT) {
        return DV_E_FORMATETC;
    }

    DWORD id = ::CoGetCurrentProcess();

    HGLOBAL copy = ::GlobalAlloc(GALLOCFLG, sizeof(id));

    if (copy == NULL) {
        throw std::bad_alloc();
    }

    char *dataout = (char *)::GlobalLock(copy);

    memcpy(dataout, &id, sizeof(id));
    ::GlobalUnlock(copy);

    pmedium->tymed = TYMED_HGLOBAL;
    pmedium->hGlobal = copy;
    pmedium->pUnkForRelease = (IUnknown *)NULL;

    return S_OK;
}

static void ScaleDownAbs(POINT &pt) {
    HMONITOR monitor = MonitorFromPoint(pt, MONITOR_DEFAULTTOPRIMARY);
    int screen = AwtWin32GraphicsDevice::GetScreenFromHMONITOR(monitor);
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(screen);
    if (device) {
        pt.x = device->ScaleDownAbsX(pt.x);
        pt.y = device->ScaleDownAbsY(pt.y);
    }
}

DECLARE_JAVA_CLASS(dSCClazz, "sun/awt/windows/WDragSourceContextPeer")

void
AwtDragSource::call_dSCenter(JNIEnv* env, jobject self, jint targetActions,
                             jint modifiers, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCenter, dSCClazz, "dragEnter", "(IIII)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCenter, targetActions, modifiers, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void
AwtDragSource::call_dSCmotion(JNIEnv* env, jobject self, jint targetActions,
                              jint modifiers, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCmotion, dSCClazz, "dragMotion", "(IIII)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCmotion, targetActions, modifiers, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void
AwtDragSource::call_dSCchanged(JNIEnv* env, jobject self, jint targetActions,
                               jint modifiers, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCchanged, dSCClazz, "operationChanged",
                             "(IIII)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCchanged, targetActions, modifiers, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void
AwtDragSource::call_dSCexit(JNIEnv* env, jobject self, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCexit, dSCClazz, "dragExit", "(II)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCexit, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void
AwtDragSource::call_dSCddfinished(JNIEnv* env, jobject self, jboolean success,
                                  jint operations, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCddfinished, dSCClazz, "dragDropFinished", "(ZIII)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCddfinished, success, operations, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void
AwtDragSource::call_dSCmouseMoved(JNIEnv* env, jobject self, jint targetActions,
                                  jint modifiers, POINT pt) {
    ScaleDownAbs(pt);
    DECLARE_VOID_JAVA_METHOD(dSCmouseMoved, dSCClazz, "dragMouseMoved",
                             "(IIII)V");
    DASSERT(!JNU_IsNull(env, self));
    env->CallVoidMethod(self, dSCmouseMoved, targetActions, modifiers, pt.x, pt.y);
    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

DECLARE_JAVA_CLASS(awtIEClazz, "java/awt/event/InputEvent")

/**
 * Constructor
 */

AwtDragSource::ADSIEnumFormatEtc::ADSIEnumFormatEtc(AwtDragSource* parent) {
    m_parent = parent;
    m_idx    = 0;

    m_refs   = 0;

    m_parent->AddRef();

    AddRef();
}

/**
 * Destructor
 */

AwtDragSource::ADSIEnumFormatEtc::~ADSIEnumFormatEtc() {
    m_parent->Release();
}

/**
 * QueryInterface
 */

HRESULT __stdcall  AwtDragSource::ADSIEnumFormatEtc::QueryInterface(REFIID riid, void __RPC_FAR *__RPC_FAR *ppvObject) {
    TRY;

    if (riid == IID_IUnknown) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IUnknown*)this;
        AddRef();
        return S_OK;
    } else if (riid == IID_IEnumFORMATETC) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IEnumFORMATETC*)this;
        AddRef();
        return S_OK;
    } else {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)NULL;
        return E_NOINTERFACE;
    }

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * AddRef
 */

ULONG __stdcall  AwtDragSource::ADSIEnumFormatEtc::AddRef(void) {
    return (ULONG)++m_refs;
}

/**
 * Release
 */

ULONG __stdcall  AwtDragSource::ADSIEnumFormatEtc::Release(void) {
    int refs;

    if ((refs = --m_refs) == 0) delete this;

    return (ULONG)refs;
}

/**
 * Next
 */

HRESULT _stdcall AwtDragSource::ADSIEnumFormatEtc::Next(ULONG celt, FORMATETC __RPC_FAR *rgelt, ULONG __RPC_FAR *pceltFetched) {
    TRY;

    unsigned int len = m_parent->getNTypes();
    unsigned int i;

    for (i = 0; i < celt && m_idx < len; i++, m_idx++) {
        FORMATETC fetc = m_parent->getType(m_idx);
        rgelt[i] = fetc;
    }

    if (pceltFetched != NULL) *pceltFetched = i;

    return i == celt ? S_OK : S_FALSE;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Skip
 */

HRESULT __stdcall  AwtDragSource::ADSIEnumFormatEtc::Skip(ULONG celt) {
    TRY;

    unsigned int len = m_parent->getNTypes();
    unsigned int tmp = m_idx + celt;

    if (tmp < len) {
        m_idx = tmp;

        return S_OK;
    } else {
        m_idx = len;

        return S_FALSE;
    }

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Reset
 */

HRESULT __stdcall  AwtDragSource::ADSIEnumFormatEtc::Reset(void) {
    m_idx = 0;

    return S_OK;
}

/**
 * Clone
 */

HRESULT __stdcall  AwtDragSource::ADSIEnumFormatEtc::Clone(IEnumFORMATETC  __RPC_FAR *__RPC_FAR *ppenum) {
    TRY;

    *ppenum = new ADSIEnumFormatEtc(m_parent);
    (*ppenum)->Skip(m_idx);
    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * constructor
 */

AwtDragSource::ADSIStreamProxy::ADSIStreamProxy(AwtDragSource* parent, jbyteArray buffer, jint blen) {
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    m_parent = parent;

    m_buffer = (signed char *)safe_Calloc(sizeof(signed char), m_blen = blen);

    env->GetByteArrayRegion(buffer, 0, blen, m_buffer);

    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) return;

    m_off     = 0;

    m_cloneof = (ADSIStreamProxy*)NULL;

    m_refs    = 0;

    FILETIME now;

    ::CoFileTimeNow(&now);

    m_statstg.pwcsName          = (LPWSTR)NULL;
    m_statstg.type              = STGTY_STREAM;
    m_statstg.cbSize.HighPart   = 0;
    m_statstg.cbSize.LowPart    = m_blen;
    m_statstg.mtime             = now;
    m_statstg.ctime             = now;
    m_statstg.atime             = now;
    m_statstg.grfMode           = STGM_READ;
    m_statstg.grfLocksSupported = FALSE;
    m_statstg.clsid             = CLSID_NULL;
    m_statstg.grfStateBits      = 0;
    m_statstg.reserved          = 0;

    m_parent->AddRef();

    AddRef();
}

/**
 * constructor (clone)
 */

AwtDragSource::ADSIStreamProxy::ADSIStreamProxy(ADSIStreamProxy* cloneof) {
    m_cloneof = cloneof;

    m_parent  = cloneof->m_parent;

    m_buffer  = cloneof->m_buffer;
    m_blen    = cloneof->m_blen;
    m_off     = cloneof->m_off;

    m_statstg = cloneof->m_statstg;

    m_refs    = 0;

    m_parent->AddRef();
    m_cloneof->AddRef();
}

/**
 * destructor
 */

AwtDragSource::ADSIStreamProxy::~ADSIStreamProxy() {
    if (m_cloneof == (ADSIStreamProxy*)NULL)
        free((void *)m_buffer);
    else {
        m_cloneof->Release();
    }

    m_parent->Release();
}

/**
 * QueryInterface
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::QueryInterface(REFIID riid, void __RPC_FAR *__RPC_FAR *ppvObject) {
    TRY;

    if (riid == IID_IUnknown) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IUnknown*)this;
        AddRef();
        return S_OK;
    } else if (riid == IID_IStream) {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)(IStream*)this;
        AddRef();
        return S_OK;
    } else {
        *ppvObject = (void __RPC_FAR *__RPC_FAR)NULL;
        return E_NOINTERFACE;
    }

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * AddRef
 */

ULONG __stdcall  AwtDragSource::ADSIStreamProxy::AddRef(void) {
    return (ULONG)++m_refs;
}

/**
 * Release
 */

ULONG __stdcall  AwtDragSource::ADSIStreamProxy::Release(void) {
    int refs;

    if ((refs = --m_refs) == 0) delete this;

    return (ULONG)refs;
}

/**
 * Read
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Read(void __RPC_FAR *pv, ULONG cb, ULONG __RPC_FAR *pcbRead) {
    TRY;

    unsigned int rem  = m_blen - m_off;
    int          read = cb > rem ? rem : cb;

    if (read > 0) memmove(pv, (void *)(m_buffer + m_off), read);

    m_off += read;

    if (pcbRead != (ULONG __RPC_FAR *)NULL) {
        *pcbRead = read;
    }

    FILETIME now; ::CoFileTimeNow(&now); m_statstg.atime = now;

    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Write
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Write(const void __RPC_FAR *pv, ULONG cb, ULONG __RPC_FAR *pcbWritten) {
    TRY;

    if (pcbWritten != (ULONG __RPC_FAR *)NULL) {
        *pcbWritten = 0;
    }

    FILETIME now; ::CoFileTimeNow(&now); m_statstg.atime = now;

    return STG_E_CANTSAVE; // don't support writing

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Seek
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Seek(LARGE_INTEGER dlibMove, DWORD dwOrigin, ULARGE_INTEGER __RPC_FAR *plibNewPosition) {
    TRY;

    if (dlibMove.HighPart != 0) return STG_E_INVALIDPOINTER;

    if (plibNewPosition != (ULARGE_INTEGER __RPC_FAR *)NULL) {
        plibNewPosition->HighPart = 0;
        plibNewPosition->LowPart  = 0;
    }

    switch (dwOrigin) {
        case STREAM_SEEK_SET: {
            if (dlibMove.HighPart != 0 || dlibMove.LowPart >= m_blen) return STG_E_INVALIDPOINTER;

            m_off = dlibMove.LowPart;
        }
        break;

        case STREAM_SEEK_CUR:
        case STREAM_SEEK_END: {
            if (dlibMove.HighPart > 0) return STG_E_INVALIDPOINTER;

            long newoff = (dwOrigin == STREAM_SEEK_END ? m_blen : m_off) + dlibMove.LowPart;

            if (newoff < 0 || newoff >= (long)m_blen)
                return STG_E_INVALIDPOINTER;
            else
                m_off = newoff;
        }
        break;

        default: return STG_E_INVALIDFUNCTION;
    }

    if (plibNewPosition != (ULARGE_INTEGER __RPC_FAR *)NULL)
        plibNewPosition->LowPart = m_off;

    FILETIME now; ::CoFileTimeNow(&now); m_statstg.atime = now;

    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * SetSize
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::SetSize(ULARGE_INTEGER libNewSize) {
    return STG_E_INVALIDFUNCTION;
}

/**
 * CopyTo
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::CopyTo(IStream __RPC_FAR *pstm, ULARGE_INTEGER cb, ULARGE_INTEGER __RPC_FAR *pcbRead, ULARGE_INTEGER __RPC_FAR *pcbWritten) {
    TRY;

    ULONG written = 0;

    pcbWritten->HighPart = (ULONG)0;
    pcbWritten->LowPart  = (ULONG)0;

    pcbRead->HighPart     = (ULONG)0;

    unsigned int rem     = m_blen - m_off;
    int          ovrflow = cb.LowPart >= rem;


    if (cb.HighPart != 0) return STG_E_INVALIDPOINTER;

    ULONG nbytes = pcbRead->LowPart = (ULONG)(ovrflow ? rem : cb.LowPart);

    HRESULT res = pstm->Write((const void *)(m_buffer + m_off), nbytes, &written);

    pcbWritten->LowPart = written;

    FILETIME now; ::CoFileTimeNow(&now); m_statstg.atime = now;

    return res;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Commit
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Commit(DWORD grfCommitFlags) {
    return S_OK;
}

/**
 * Revert
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Revert() {
    return S_OK;
}

/**
 * LockRegion
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::LockRegion(ULARGE_INTEGER libOffset, ULARGE_INTEGER cb, DWORD dwLockType) {
    return STG_E_INVALIDFUNCTION;
}

/**
 * UnlockRegion
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::UnlockRegion(ULARGE_INTEGER libOffset, ULARGE_INTEGER cb, DWORD dwLockType) {
    return STG_E_INVALIDFUNCTION;
}

/**
 * Stat
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Stat(STATSTG __RPC_FAR *pstatstg, DWORD grfStatFlag) {
    TRY;

    *pstatstg = m_statstg;

    FILETIME now; ::CoFileTimeNow(&now); m_statstg.atime = now;

    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/**
 * Clone
 */

HRESULT __stdcall  AwtDragSource::ADSIStreamProxy::Clone(IStream __RPC_FAR *__RPC_FAR *ppstm) {
    TRY;

    *ppstm = new ADSIStreamProxy(this);
    return S_OK;

    CATCH_BAD_ALLOC_RET(E_OUTOFMEMORY);
}

/*****************************************************************************/

extern "C" {

/**
 * setNativeCursor
 */

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDragSourceContextPeer_setNativeCursor(JNIEnv* env,
                                                            jobject self,
                                                            jlong nativeCtxt,
                                                            jobject cursor,
                                                            jint type) {
    TRY;

    AwtDragSource* ds = (AwtDragSource*)nativeCtxt;
    if (ds != NULL) {
        ds->SetCursor(cursor);
    }

    CATCH_BAD_ALLOC;
}

/**
 * createDragSource
 */

JNIEXPORT jlong JNICALL
Java_sun_awt_windows_WDragSourceContextPeer_createDragSource(
    JNIEnv* env, jobject self, jobject component, jobject transferable,
    jobject trigger, jint actions,
    jlongArray formats, jobject formatMap)
{
    TRY;

    if (!AwtDropTarget::IsCurrentDnDDataObject(NULL)) {
        JNU_ThrowByName(env, "java/awt/dnd/InvalidDnDOperationException",
                        "Drag and drop is in progress");
        return (jlong)NULL;
    }

    AwtDragSource* ds = new AwtDragSource(env, self, component,
                                          transferable, trigger, actions,
                                          formats, formatMap);

    DASSERT(AwtDropTarget::IsLocalDataObject(ds));

    return (jlong)ds;

    CATCH_BAD_ALLOC_RET(0);
}

/**
 * doDragDrop
 */

JNIEXPORT void JNICALL Java_sun_awt_windows_WDragSourceContextPeer_doDragDrop(
    JNIEnv* env,
    jobject self,
    jlong nativeCtxt,
    jobject cursor,
    jintArray imageData,
    jint imageWidth, jint imageHeight,
    jint x, jint y)
{
    TRY;

    cursor = env->NewGlobalRef(cursor);
    if (NULL != imageData) {
        imageData = (jintArray)env->NewGlobalRef(imageData);
    }

    AwtDragSource::StartDrag(
        (AwtDragSource*)nativeCtxt,
        cursor,
        imageData,
        imageWidth, imageHeight,
        x, y);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
