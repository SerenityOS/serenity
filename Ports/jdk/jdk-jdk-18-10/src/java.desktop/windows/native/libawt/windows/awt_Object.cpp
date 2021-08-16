/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Object.h"
#include "ObjectList.h"

#ifdef DEBUG
static BOOL reportEvents = FALSE;
#endif


/************************************************************************
 * AwtObject fields
 */

jfieldID AwtObject::pDataID;
jfieldID AwtObject::destroyedID;
jfieldID AwtObject::targetID;
jclass AwtObject::wObjectPeerClass;
jmethodID AwtObject::getPeerForTargetMID;
jfieldID AwtObject::createErrorID;


/************************************************************************
 * AwtObject methods
 */

AwtObject::AwtObject()
{
    theAwtObjectList.Add(this);
    m_peerObject = NULL;
    m_callbacksEnabled = TRUE;
}

AwtObject::~AwtObject()
{
}

void AwtObject::Dispose()
{
    AwtToolkit::GetInstance().PostMessage(WM_AWT_DELETEOBJECT, (WPARAM)this, (LPARAM)0);
}

void AwtObject::_Dispose(jobject self)
{
    TRY_NO_VERIFY;

    CriticalSection::Lock l(AwtToolkit::GetInstance().GetSyncCS());

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject selfGlobalRef = env->NewGlobalRef(self);

    // value 0 of lParam means that we should not attempt to enter the
    // SyncCall critical section, as it was entered someshere earlier
    AwtToolkit::GetInstance().SendMessage(WM_AWT_DISPOSE, (WPARAM)selfGlobalRef, (LPARAM)0);

    CATCH_BAD_ALLOC;
}

void AwtObject::_Dispose(PDATA pData)
{
    TRY_NO_VERIFY;

    CriticalSection::Lock l(AwtToolkit::GetInstance().GetSyncCS());

    AwtToolkit::GetInstance().SendMessage(WM_AWT_DISPOSEPDATA, (WPARAM)pData, (LPARAM)0);

    CATCH_BAD_ALLOC;
}
/*
 * Return the peer associated with some target.  This information is
 * maintained in a hashtable at the java level.
 */
jobject AwtObject::GetPeerForTarget(JNIEnv *env, jobject target)
{
    jobject result =
        env->CallStaticObjectMethod(AwtObject::wObjectPeerClass,
                                    AwtObject::getPeerForTargetMID,
                                    target);

    DASSERT(!safe_ExceptionOccurred(env));
    return result;
}

/* Execute a callback to the associated Java peer. */
void
AwtObject::DoCallback(const char* methodName, const char* methodSig, ...)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    /* don't callback during the create & initialization process */
    if (m_peerObject != NULL && m_callbacksEnabled) {
        va_list args;
        va_start(args, methodSig);
#ifdef DEBUG
        if (reportEvents) {
            jstring targetStr =
                (jstring)JNU_CallMethodByName(env, NULL, GetTarget(env),
                                              "getName",
                                              "()Ljava/lang/String;").l;
            DASSERT(!safe_ExceptionOccurred(env));
            LPCWSTR targetStrW = JNU_GetStringPlatformChars(env, targetStr, NULL);
            printf("Posting %s%s method to %S\n", methodName, methodSig, targetStrW);
            JNU_ReleaseStringPlatformChars(env, targetStr, targetStrW);
        }
#endif
        /* caching would do much good here */
        JNU_CallMethodByNameV(env, NULL, GetPeer(env),
                              methodName, methodSig, args);
        {
            jthrowable exc = safe_ExceptionOccurred(env);
            if (exc) {
                env->DeleteLocalRef(exc);
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
        DASSERT(!safe_ExceptionOccurred(env));
        va_end(args);
    }
}

void AwtObject::SendEvent(jobject event)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

#ifdef DEBUG
    if (reportEvents) {
        jstring eventStr = JNU_ToString(env, event);
        DASSERT(!safe_ExceptionOccurred(env));
        jstring targetStr =
            (jstring)JNU_CallMethodByName(env, NULL, GetTarget(env),"getName",
                                          "()Ljava/lang/String;").l;
        DASSERT(!safe_ExceptionOccurred(env));
        LPCWSTR eventStrW = JNU_GetStringPlatformChars(env, eventStr, NULL);
        LPCWSTR targetStrW = JNU_GetStringPlatformChars(env, targetStr, NULL);
        printf("Posting %S to %S\n", eventStrW, targetStrW);
        JNU_ReleaseStringPlatformChars(env, eventStr, eventStrW);
        JNU_ReleaseStringPlatformChars(env, targetStr, targetStrW);
    }
#endif
    /* Post event to the system EventQueue. */
    JNU_CallMethodByName(env, NULL, GetPeer(env), "postEvent",
                         "(Ljava/awt/AWTEvent;)V", event);
    {
        jthrowable exc = safe_ExceptionOccurred(env);
        if (exc) {
            env->DeleteLocalRef(exc);
            env->ExceptionDescribe();
        }
    }
    DASSERT(!safe_ExceptionOccurred(env));
}

//
// (static)
// Switches to Windows thread via SendMessage and synchronously
// calls AwtObject::WinThreadExecProc with the given command id
// and parameters.
//
// Useful for writing code that needs to be synchronized with
// what's happening on the Windows thread.
//
LRESULT AwtObject::WinThreadExec(
    jobject                             peerObject,
    UINT                                cmdId,
    LPARAM                              param1,
    LPARAM                              param2,
    LPARAM                              param3,
    LPARAM                              param4 )
{
    DASSERT( peerObject != NULL);

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    // since we pass peerObject to another thread we must
    //   make a global ref
    jobject peerObjectGlobalRef = env->NewGlobalRef(peerObject);

    ExecuteArgs         args;
    LRESULT         retVal;

    // setup arguments
    args.cmdId = cmdId;
    args.param1 = param1;
    args.param2 = param2;
    args.param3 = param3;
    args.param4 = param4;

    // call WinThreadExecProc on the toolkit thread
    retVal = AwtToolkit::GetInstance().SendMessage(WM_AWT_EXECUTE_SYNC,
                                                   (WPARAM)peerObjectGlobalRef,
                                                   (LPARAM)&args);
    return retVal;
}

LRESULT AwtObject::WinThreadExecProc(ExecuteArgs * args)
{
    DASSERT(FALSE); // no default handler
    return 0L;
}

/************************************************************************
 * WObjectPeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WObjectPeer_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtObject::wObjectPeerClass = (jclass)env->NewGlobalRef(cls);
    DASSERT(AwtObject::wObjectPeerClass != NULL);
    CHECK_NULL(AwtObject::wObjectPeerClass);

    AwtObject::pDataID = env->GetFieldID(cls, "pData", "J");
    DASSERT(AwtObject::pDataID != NULL);
    CHECK_NULL(AwtObject::pDataID);

    AwtObject::destroyedID = env->GetFieldID(cls, "destroyed", "Z");
    DASSERT(AwtObject::destroyedID != NULL);
    CHECK_NULL(AwtObject::destroyedID);

    AwtObject::targetID = env->GetFieldID(cls, "target",
                                              "Ljava/lang/Object;");
    DASSERT(AwtObject::targetID != NULL);
    CHECK_NULL(AwtObject::targetID);

    AwtObject::getPeerForTargetMID =
        env->GetStaticMethodID(cls, "getPeerForTarget",
                         "(Ljava/lang/Object;)Lsun/awt/windows/WObjectPeer;");
    DASSERT(AwtObject::getPeerForTargetMID != NULL);
    CHECK_NULL(AwtObject::getPeerForTargetMID);

    AwtObject::createErrorID = env->GetFieldID(cls, "createError", "Ljava/lang/Error;");
    DASSERT(AwtObject::createErrorID != NULL);
    CHECK_NULL(AwtObject::createErrorID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
