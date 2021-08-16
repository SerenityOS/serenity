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

#ifndef AWT_OBJECT_H
#define AWT_OBJECT_H

#include "awt.h"
#include "awt_Toolkit.h"

#include "java_awt_Event.h"
#include "java_awt_AWTEvent.h"
#include "sun_awt_windows_WObjectPeer.h"

/************************************************************************
 * AwtObject class
 */

class AwtObject {
public:
    class ExecuteArgs {
        public:
            UINT        cmdId;
            LPARAM      param1;
            LPARAM      param2;
            LPARAM      param3;
            LPARAM      param4;
    };


    /* sun.awt.windows.WObjectPeer field and method ids */
    static jfieldID pDataID;
    static jfieldID destroyedID;
    static jfieldID targetID;

    static jmethodID getPeerForTargetMID;
    static jclass wObjectPeerClass;

    static jfieldID createErrorID;

    AwtObject();
    virtual ~AwtObject();

    // Frees all the resources used by this object and then sends a message to TT to delete it.
    // After this method has been called, this object must not be used in any way.
    virtual void Dispose();

    // Static method to be called from JNI methods to dispose AwtObject
    // specified by jobject
    static void _Dispose(jobject self);

    // Static method to be called from JNI methods to dispose AwtObject
    // specified by pData
    static void _Dispose(PDATA pData);

    INLINE CriticalSection& GetLock() { return m_Lock; }

    // Return the associated AWT peer or target object.
    INLINE jobject GetPeer(JNIEnv *env) {
        return m_peerObject;
    }

    INLINE jobject GetTarget(JNIEnv *env) {
        jobject peer = GetPeer(env);
        if (peer != NULL) {
            return env->GetObjectField(peer, AwtObject::targetID);
        } else {
            return NULL;
        }
    }

    INLINE jobject GetTargetAsGlobalRef(JNIEnv *env) {
        jobject localRef = GetTarget(env);
        if (localRef == NULL) {
            return NULL;
        }

        jobject globalRef = env->NewGlobalRef(localRef);
        env->DeleteLocalRef(localRef);
        return globalRef;
    }

    // Return the peer associated with some target
    static jobject GetPeerForTarget(JNIEnv *env, jobject target);

    // Java callback routines
    // Invoke a callback on the java peer object asynchronously
    void DoCallback(const char* methodName, const char* methodSig, ...);

    // Allocate and initialize a new event, and post it to the peer's
    // target object.  No response is expected from the target.
    void SendEvent(jobject event);

    INLINE void EnableCallbacks(BOOL e) { m_callbacksEnabled = e; }

    // Execute any code associated with a command ID -- only classes with
    // DoCommand() defined should associate their instances with cmdIDs.
    virtual void DoCommand(void) {
        DASSERT(FALSE);
    }

    // execute given code on Windows message-pump thread
    static LRESULT WinThreadExec(jobject peerObject, UINT cmdId, LPARAM param1 = 0L, LPARAM param2 = 0L, LPARAM param3 = 0L, LPARAM param4 = 0L);
    // callback function to execute code on Windows message-pump thread
    virtual LRESULT WinThreadExecProc(AwtObject::ExecuteArgs * args);

    // overridden in AwtComponent to return FALSE if any messages
    // are being processed by this component
    virtual BOOL CanBeDeleted() {
        return TRUE;
    }

protected:
    jobject                       m_peerObject;
    BOOL                          m_callbacksEnabled;

private:
    CriticalSection m_Lock;
};

#endif // AWT_OBJECT_H
