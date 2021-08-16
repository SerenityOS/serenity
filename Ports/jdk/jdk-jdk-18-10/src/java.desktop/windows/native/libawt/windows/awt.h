/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _AWT_H_
#define _AWT_H_

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif

#ifndef _WIN32_IE
#define _WIN32_IE 0x0600
#endif

//#ifndef NTDDI_VERSION
//#define NTDDI_VERSION NTDDI_LONGHORN
//#endif

#include "stdhdrs.h"
#include "alloc.h"
#include "awt_Debug.h"

extern COLORREF DesktopColor2RGB(int colorIndex);

class AwtObject;
typedef AwtObject* PDATA;

#define JNI_IS_TRUE(obj) ((obj) ? JNI_TRUE : JNI_FALSE)

#define JNI_CHECK_NULL_GOTO(obj, msg, where) {                            \
    if (obj == NULL) {                                                    \
        env->ExceptionClear();                                            \
        JNU_ThrowNullPointerException(env, msg);                          \
        goto where;                                                       \
    }                                                                     \
}

#define JNI_CHECK_NULL_RETURN(obj, msg) {                                 \
    if (obj == NULL) {                                                    \
        env->ExceptionClear();                                            \
        JNU_ThrowNullPointerException(env, msg);                          \
        return;                                                           \
    }                                                                     \
}

#define JNI_CHECK_PEER_CREATION_RETURN(peer) {                            \
    if (peer == NULL ) {                                                  \
        return;                                                           \
    }                                                                     \
    pData = JNI_GET_PDATA(peer);                                          \
    if (pData == NULL) {                                                  \
        return;                                                           \
    }                                                                     \
}

#define JNI_CHECK_NULL_RETURN_NULL(obj, msg) {                            \
    if (obj == NULL) {                                                    \
        env->ExceptionClear();                                            \
        JNU_ThrowNullPointerException(env, msg);                          \
        return 0;                                                         \
    }                                                                     \
}

#define JNI_CHECK_NULL_RETURN_VAL(obj, msg, val) {                        \
    if (obj == NULL) {                                                    \
        env->ExceptionClear();                                            \
        JNU_ThrowNullPointerException(env, msg);                          \
        return val;                                                       \
    }                                                                     \
}

/**
 * This macros must be used under SyncCall or on the Toolkit thread.
 */
#define JNI_CHECK_PEER_GOTO(peer, where) {                                \
    JNI_CHECK_NULL_GOTO(peer, "peer", where);                             \
    pData = JNI_GET_PDATA(peer);                                          \
    if (pData == NULL) {                                                  \
        THROW_NULL_PDATA_IF_NOT_DESTROYED(peer);                          \
        goto where;                                                       \
    }                                                                     \
}

/**
 * This macros must be used under SyncCall or on the Toolkit thread.
 */
#define JNI_CHECK_PEER_RETURN(peer) {                                     \
    JNI_CHECK_NULL_RETURN(peer, "peer");                                  \
    pData = JNI_GET_PDATA(peer);                                          \
    if (pData == NULL) {                                                  \
        THROW_NULL_PDATA_IF_NOT_DESTROYED(peer);                          \
        return;                                                           \
    }                                                                     \
}

/**
 * This macros must be used under SyncCall or on the Toolkit thread.
 */
#define JNI_CHECK_PEER_RETURN_NULL(peer) {                                \
    JNI_CHECK_NULL_RETURN_NULL(peer, "peer");                             \
    pData = JNI_GET_PDATA(peer);                                          \
    if (pData == NULL) {                                                  \
        THROW_NULL_PDATA_IF_NOT_DESTROYED(peer);                          \
        return 0;                                                         \
    }                                                                     \
}

/**
 * This macros must be used under SyncCall or on the Toolkit thread.
 */
#define JNI_CHECK_PEER_RETURN_VAL(peer, val) {                            \
    JNI_CHECK_NULL_RETURN_VAL(peer, "peer", val);                         \
    pData = JNI_GET_PDATA(peer);                                          \
    if (pData == NULL) {                                                  \
        THROW_NULL_PDATA_IF_NOT_DESTROYED(peer);                          \
        return val;                                                       \
    }                                                                     \
}

#define THROW_NULL_PDATA_IF_NOT_DESTROYED(peer) {                         \
    jboolean destroyed = JNI_GET_DESTROYED(peer);                         \
    if (destroyed != JNI_TRUE) {                                          \
        env->ExceptionClear();                                            \
        JNU_ThrowNullPointerException(env, "null pData");                 \
    }                                                                     \
}

#define JNI_GET_PDATA(peer) (PDATA) env->GetLongField(peer, AwtObject::pDataID)
#define JNI_GET_DESTROYED(peer) env->GetBooleanField(peer, AwtObject::destroyedID)

#define JNI_SET_PDATA(peer, data) env->SetLongField(peer,                  \
                                                    AwtObject::pDataID,    \
                                                    (jlong)data)
#define JNI_SET_DESTROYED(peer) env->SetBooleanField(peer,                   \
                                                     AwtObject::destroyedID, \
                                                     JNI_TRUE)
/*  /NEW JNI */

/*
 * IS_WIN64 returns TRUE on 64-bit Itanium
 */
#if defined (_WIN64)
    #define IS_WIN64 TRUE
#else
    #define IS_WIN64 FALSE
#endif

/*
 * IS_WIN2000 returns TRUE on 2000, XP and Vista
 * IS_WINXP returns TRUE on XP and Vista
 * IS_WINVISTA returns TRUE on Vista
 */
#define IS_WIN2000 (LOBYTE(LOWORD(::GetVersion())) >= 5)
#define IS_WINXP ((IS_WIN2000 && HIBYTE(LOWORD(::GetVersion())) >= 1) || LOBYTE(LOWORD(::GetVersion())) > 5)
#define IS_WINVISTA (LOBYTE(LOWORD(::GetVersion())) >= 6)
#define IS_WIN8 (                                                              \
    (IS_WINVISTA && (HIBYTE(LOWORD(::GetVersion())) >= 2)) ||                  \
    (LOBYTE(LOWORD(::GetVersion())) > 6))

#define IS_WINVER_ATLEAST(maj, min) \
                   ((maj) < LOBYTE(LOWORD(::GetVersion())) || \
                      (maj) == LOBYTE(LOWORD(::GetVersion())) && \
                      (min) <= HIBYTE(LOWORD(::GetVersion())))

/*
 * macros to crack a LPARAM into two ints -- used for signed coordinates,
 * such as with mouse messages.
 */
#define LO_INT(l)           ((int)(short)(l))
#define HI_INT(l)           ((int)(short)(((DWORD)(l) >> 16) & 0xFFFF))

extern JavaVM *jvm;

// Platform encoding is Unicode (UTF-16), re-define JNU_ functions
// to proper JNI functions.
#define JNU_NewStringPlatform(env, x) env->NewString(reinterpret_cast<const jchar*>(x), static_cast<jsize>(_tcslen(x)))
#define JNU_GetStringPlatformChars(env, x, y) reinterpret_cast<LPCWSTR>(env->GetStringChars(x, y))
#define JNU_ReleaseStringPlatformChars(env, x, y) env->ReleaseStringChars(x, reinterpret_cast<const jchar*>(y))

/*
 * Itanium symbols needed for 64-bit compilation.
 * These are defined in winuser.h in the August 2001 MSDN update.
 */
#ifndef GCLP_HBRBACKGROUND
    #ifdef _WIN64
        #error Macros for GetClassLongPtr, etc. are for 32-bit windows only
    #endif /* !_WIN64 */
    #define GetClassLongPtr GetClassLong
    #define SetClassLongPtr SetClassLong
    #define GCLP_HBRBACKGROUND GCL_HBRBACKGROUND
    #define GCLP_HCURSOR GCL_HCURSOR
    #define GCLP_HICON GCL_HICON
    #define GCLP_HICONSM GCL_HICONSM
    #define GCLP_HMODULE GCL_HMODULE
    #define GCLP_MENUNAME GCL_MENUNAME
    #define GCLP_WNDPROC GCL_WNDPROC
    #define GetWindowLongPtr GetWindowLong
    #define SetWindowLongPtr SetWindowLong
    #define GWLP_WNDPROC GWL_WNDPROC
    #define GWLP_HINSTANCE GWL_HINSTANCE
    #define GWLP_HWNDPARENT GWL_HWNDPARENT
    #define GWLP_ID GWL_ID
    #define GWLP_USERDATA GWL_USERDATA
    #define DWLP_DLGPROC DWL_DLGPROC
    #define DWLP_MSGRESULT DWL_MSGRESULT
    #define DWLP_USER DWL_USER
#endif /* !GCLP_HBRBACKGROUND */

/*
 * macros for saving and restoring FPU control word
 * NOTE: float.h must be defined if using these macros
 */
#define SAVE_CONTROLWORD  \
  unsigned int fpu_cw = _control87(0, 0);

#define RESTORE_CONTROLWORD  \
  if (_control87(0, 0) != fpu_cw) {  \
    _control87(fpu_cw, 0xffffffff);  \
  }

/*
 * checks if the current thread is/isn't the toolkit thread
 */
#if defined(DEBUG)
#define CHECK_IS_TOOLKIT_THREAD() \
  if (GetCurrentThreadId() != AwtToolkit::MainThread())  \
  { JNU_ThrowInternalError(env,"Operation is not permitted on non-toolkit thread!\n"); }
#define CHECK_ISNOT_TOOLKIT_THREAD()  \
  if (GetCurrentThreadId() == AwtToolkit::MainThread())  \
  { JNU_ThrowInternalError(env,"Operation is not permitted on toolkit thread!\n"); }
#else
#define CHECK_IS_TOOLKIT_THREAD()
#define CHECK_ISNOT_TOOLKIT_THREAD()
#endif


struct EnvHolder
{
    JavaVM *m_pVM;
    JNIEnv *m_env;
    bool    m_isOwner;
    EnvHolder(
        JavaVM *pVM,
        LPCSTR name = "COM holder",
        jint ver = JNI_VERSION_1_2)
    : m_pVM(pVM),
      m_env((JNIEnv *)JNU_GetEnv(pVM, ver)),
      m_isOwner(false)
    {
        if (NULL == m_env) {
            JavaVMAttachArgs attachArgs;
            attachArgs.version  = ver;
            attachArgs.name     = const_cast<char *>(name);
            attachArgs.group    = NULL;
            jint status = m_pVM->AttachCurrentThread(
                (void**)&m_env,
                &attachArgs);
            m_isOwner = (NULL!=m_env);
        }
    }
    ~EnvHolder() {
        if (m_isOwner) {
            m_pVM->DetachCurrentThread();
        }
    }
    operator bool()  const { return NULL!=m_env; }
    bool operator !()  const { return NULL==m_env; }
    operator JNIEnv*() const { return m_env; }
    JNIEnv* operator ->() const { return m_env; }
};

template <class T>
class JLocalRef {
    JNIEnv* m_env;
    T m_localJRef;

public:
    JLocalRef(JNIEnv* env, T localJRef = NULL)
    : m_env(env),
    m_localJRef(localJRef)
    {}
    T Detach() {
        T ret = m_localJRef;
        m_localJRef = NULL;
        return ret;
    }
    void Attach(T newValue) {
        if (m_localJRef) {
            m_env->DeleteLocalRef((jobject)m_localJRef);
        }
        m_localJRef = newValue;
    }

    operator T() { return m_localJRef; }
    operator bool() { return NULL!=m_localJRef; }
    bool operator !() { return NULL==m_localJRef; }

    ~JLocalRef() {
        if (m_localJRef) {
            m_env->DeleteLocalRef((jobject)m_localJRef);
        }
    }
};

typedef JLocalRef<jobject> JLObject;
typedef JLocalRef<jstring> JLString;
typedef JLocalRef<jclass>  JLClass;

/*
 * Class to encapsulate the extraction of the java string contents
 * into a buffer and the cleanup of the buffer
 */
class JavaStringBuffer
{
protected:
    LPWSTR m_pStr;
    jsize  m_dwSize;
    LPCWSTR getNonEmptyString() {
        return (NULL==m_pStr)
                ? L""
                : m_pStr;
    }

public:
    JavaStringBuffer(jsize cbTCharCount) {
        m_dwSize = cbTCharCount;
        m_pStr = (0 == m_dwSize)
            ? NULL
            : (LPWSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, (m_dwSize+1), sizeof(WCHAR) );
    }

    JavaStringBuffer(JNIEnv *env, jstring text) {
        m_dwSize = (NULL == text)
            ? 0
            : env->GetStringLength(text);
        if (0 == m_dwSize) {
            m_pStr = NULL;
        } else {
            m_pStr = (LPWSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, (m_dwSize+1), sizeof(WCHAR) );
            env->GetStringRegion(text, 0, m_dwSize, reinterpret_cast<jchar *>(m_pStr));
            m_pStr[m_dwSize] = 0;
        }
    }


    ~JavaStringBuffer() {
        free(m_pStr);
    }

    void Resize(jsize cbTCharCount) {
        m_dwSize = cbTCharCount;
        //It is ok to have non-null terminated string here.
        //The function is used only for space reservation in staff buffer for
        //followed data copying process. And that is the reason why we ignore
        //the special case m_dwSize==0 here.
        m_pStr = (LPWSTR)SAFE_SIZE_ARRAY_REALLOC(safe_Realloc, m_pStr, m_dwSize+1, sizeof(WCHAR) );
    }
    //we are in UNICODE now, so LPWSTR:=:LPTSTR
    operator LPCWSTR() { return getNonEmptyString(); }
    operator LPARAM() { return (LPARAM)getNonEmptyString(); }
    const void *GetData() { return (const void *)getNonEmptyString(); }
    jsize  GetSize() { return m_dwSize; }
};


#endif  /* _AWT_H_ */
