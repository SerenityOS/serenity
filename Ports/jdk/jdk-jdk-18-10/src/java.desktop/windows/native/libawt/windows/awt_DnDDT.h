/*
 * Copyright (c) 1997, 2009, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_DND_DT_H
#define AWT_DND_DT_H

#include <Ole2.h>
#include <shlobj.h>
#include <jni.h>
#include <jni_util.h>

#include "awt_Object.h"
#include "awt_Component.h"
#include "awt_Window.h"

extern "C" void awt_dnd_initialize();

/**
 * AwtDropTarget class: native peer IDropTarget implementation
 */

class AwtDropTarget : virtual public IDropTarget {
    public:
        AwtDropTarget(JNIEnv* env, AwtComponent* component);

        virtual ~AwtDropTarget();

        // IUnknown

        virtual HRESULT __stdcall QueryInterface(REFIID riid, void __RPC_FAR *__RPC_FAR *ppvObject);

        virtual ULONG   __stdcall AddRef(void);
        virtual ULONG   __stdcall Release(void);

        // IDropTarget

        virtual HRESULT __stdcall DragEnter(IDataObject __RPC_FAR *pDataObject, DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect);
        virtual HRESULT __stdcall DragOver(DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect);
        virtual HRESULT __stdcall DragLeave(void);

        virtual HRESULT __stdcall Drop(IDataObject __RPC_FAR *pDataObject, DWORD grfKeyState, POINTL pt, DWORD __RPC_FAR *pdwEffect);

        // AwtDropTarget

        virtual jobject DoGetData(jlong format);

        virtual void DoDropDone(jboolean success, jint action);

        INLINE void Signal() { ::ReleaseMutex(m_mutex); }

        virtual void RegisterTarget(WORD wParam);

        INLINE static void SetCurrentDnDDataObject(IDataObject* pDataObject) {
            DASSERT(sm_pCurrentDnDDataObject != NULL || pDataObject != NULL);
            sm_pCurrentDnDDataObject = pDataObject;
        }

        INLINE static BOOL IsCurrentDnDDataObject(IDataObject* pDataObject) {
            return sm_pCurrentDnDDataObject == pDataObject ? TRUE : FALSE;
        }

        INLINE static BOOL IsLocalDnD() {
            return IsLocalDataObject(sm_pCurrentDnDDataObject);
        }

        static BOOL IsLocalDataObject(IDataObject __RPC_FAR *pDataObject);
    protected:

        INLINE void WaitUntilSignalled(BOOL retain) {
            do {
                // nothing ...
            } while (::WaitForSingleObject(m_mutex, INFINITE) == WAIT_FAILED);

            if (!retain) ::ReleaseMutex(m_mutex);
        }

        virtual jobject GetData(jlong format);

        virtual void DropDone(jboolean success, jint action);

        virtual void DragCleanup(void);

        virtual void LoadCache(IDataObject*);

        virtual void UnloadCache();

        virtual HRESULT ExtractNativeData(jlong fmt, LONG lIndex, STGMEDIUM *pmedium);
        virtual HRESULT SaveIndexToFile(LPCTSTR pFileName, UINT lIndex);
        virtual jobject ConvertNativeData(JNIEnv* env, jlong fmt, STGMEDIUM *pmedium);
        virtual jobject ConvertMemoryMappedData(JNIEnv* env, jlong fmt, STGMEDIUM *pmedium);

    private:
        typedef struct _RegisterTargetRec {
            AwtDropTarget*      dropTarget;
            BOOL                show;
        } RegisterTargetRec, *RegisterTargetPtr;

        static void _RegisterTarget(void* param);

        typedef struct _GetDataRec {
            AwtDropTarget* dropTarget;
            jlong          format;
            jobject*       ret;
        } GetDataRec, *GetDataPtr;

        static void _GetData(void* param);

        typedef struct _DropDoneRec {
            AwtDropTarget* dropTarget;
            jboolean       success;
            jint           action;
        } DropDoneRec, *DropDonePtr;

        static void _DropDone(void* param);

        AwtComponent*         m_component;
        HWND                  m_window;
        jobject               m_target;

        unsigned int          m_refs;

        jobject               m_dtcp;

        WORD                  m_registered; // is drop site registered?

        FORMATETC*            m_formats;
        unsigned int          m_nformats;

        jlongArray            m_cfFormats;

        jboolean              m_dropSuccess;
        jint                  m_dropActions;

        HANDLE                m_mutex;

        // external COM references

        IDataObject              *m_dataObject;
        IDropTargetHelper        *m_pIDropTargetHelper;

        // static members

        static IDataObject       *sm_pCurrentDnDDataObject;

        // method references

        static jobject call_dTCcreate(JNIEnv* env);
        static jint call_dTCenter(JNIEnv* env, jobject self, jobject component,
                                  jint x, jint y, jint dropAction, jint actions,
                                  jlongArray formats, jlong nativeCtxt);
        static void call_dTCexit(JNIEnv* env, jobject self, jobject component,
                                 jlong nativeCtxt);
        static jint call_dTCmotion(JNIEnv* env, jobject self, jobject component,
                                   jint x, jint y, jint dropAction,
                                   jint actions, jlongArray formats,
                                   jlong nativeCtxt);
        static void call_dTCdrop(JNIEnv* env, jobject self, jobject component,
                                 jint x, jint y, jint dropAction, jint actions,
                                 jlongArray formats, jlong nativeCtxt);

        static jobject call_dTCgetfs(JNIEnv* env, jstring fileName,
                                     jlong stgmedium);
        static jobject call_dTCgetis(JNIEnv* env, jlong istream);

        static const unsigned int CACHE_INCR;

        static int __cdecl _compar(const void *, const void *);
};


/**
 * WDTCPIStreamWrapper: cheap wrapper class for incoming IStream drops, maps
 * onto WDropTargetContextPeerIStream class
 */

class WDTCPIStreamWrapper {
    public:
        WDTCPIStreamWrapper(STGMEDIUM* stgmedium);

        virtual ~WDTCPIStreamWrapper();

        static jint DoAvailable(WDTCPIStreamWrapper* istream);
        static jint DoRead(WDTCPIStreamWrapper* istream);
        static jint DoReadBytes(WDTCPIStreamWrapper* istream, jbyteArray buf, jint off, jint len);
        static void DoClose(WDTCPIStreamWrapper* istream);


        virtual jint Available();
        virtual jint Read();
        virtual jint ReadBytes(jbyteArray buf, jint off, jint len);
        virtual void Close();

        INLINE void Signal() { ::ReleaseMutex(m_mutex); }
   protected:

        INLINE void WaitUntilSignalled(BOOL retain) {
            do {
                // nothing ...
            } while (::WaitForSingleObject(m_mutex, INFINITE) == WAIT_FAILED);

            if (!retain) ::ReleaseMutex(m_mutex);
        }

        typedef struct _WDTCPIStreamWrapperRec {
            WDTCPIStreamWrapper* istream;
            jint                 ret;
        } WDTCPIStreamWrapperRec, *WDTCPIStreamWrapperPtr;

        static void _Available(void* param);

        static void _Read     (void* Param);

        typedef struct _WDTCPIStreamWrapperReadBytesRec {
            WDTCPIStreamWrapper* istream;
            jint                 ret;
            jbyteArray           array;
            jint                 off;
            jint                 len;
        } WDTCPIStreamWrapperReadBytesRec, *WDTCPIStreamWrapperReadBytesPtr;

        static void _ReadBytes(void* param);

        static void _Close    (void* param);

    private:
        IStream*        m_istream;
        STGMEDIUM       m_stgmedium;
        STATSTG         m_statstg;
        HANDLE          m_mutex;

        static jclass javaIOExceptionClazz;
};

class AwtInterfaceLocker
{
protected:
    IUnknown *m_pIUnknown;
public:
    AwtInterfaceLocker(IUnknown *pIUnknown)
    : m_pIUnknown( pIUnknown )
    {
        m_pIUnknown->AddRef();
    }
    ~AwtInterfaceLocker()
    {
        m_pIUnknown->Release();
    }
};

#endif /* AWT_DND_DT_H */
