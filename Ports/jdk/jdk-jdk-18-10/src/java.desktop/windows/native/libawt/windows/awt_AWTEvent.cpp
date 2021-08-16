/*
 * Copyright (c) 1998, 2002, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_AWTEvent.h"
#include "awt_Component.h"
#include <java_awt_AWTEvent.h>

/************************************************************************
 * AwtAWTEvent fields
 */

jfieldID AwtAWTEvent::bdataID;
jfieldID AwtAWTEvent::idID;
jfieldID AwtAWTEvent::consumedID;

/************************************************************************
 * AwtAWTEvent static methods
 */

void AwtAWTEvent::saveMSG(JNIEnv *env, MSG *pMsg, jobject jevent)
{
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    jbyteArray bdata = env->NewByteArray(sizeof(MSG));
    if(bdata == 0) {
        throw std::bad_alloc();
    }
    env->SetByteArrayRegion(bdata, 0, sizeof(MSG), (jbyte *)pMsg);
    DASSERT(AwtAWTEvent::bdataID);
    env->SetObjectField(jevent, AwtAWTEvent::bdataID,  bdata);
    env->DeleteLocalRef(bdata);
}

/************************************************************************
 * AwtEvent native methods
 */

extern "C" {

/*
 * Class:     java_awt_AWTEvent
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_AWTEvent_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtAWTEvent::bdataID = env->GetFieldID(cls, "bdata", "[B");
    DASSERT(AwtAWTEvent::bdataID != NULL);
    CHECK_NULL(AwtAWTEvent::bdataID);

    AwtAWTEvent::idID = env->GetFieldID(cls, "id", "I");
    DASSERT(AwtAWTEvent::idID != NULL);
    CHECK_NULL(AwtAWTEvent::idID);

    AwtAWTEvent::consumedID = env->GetFieldID(cls, "consumed", "Z");
    DASSERT(AwtAWTEvent::consumedID != NULL);
    CHECK_NULL(AwtAWTEvent::consumedID);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     java_awt_AWTEvent
 * Method:    nativeSetSource
 * Signature: (Ljava/awt/peer/ComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_java_awt_AWTEvent_nativeSetSource
    (JNIEnv *env, jobject self, jobject newSource)
{
    TRY;

    JNI_CHECK_NULL_RETURN(self, "null AWTEvent");

    MSG *pMsg;

    jbyteArray bdata = (jbyteArray)
        env->GetObjectField(self, AwtAWTEvent::bdataID);
    if (bdata != NULL) {
        jboolean dummy;
        PDATA pData;
        JNI_CHECK_PEER_RETURN(newSource);
        AwtComponent *p = (AwtComponent *)pData;
        HWND hwnd = p->GetHWnd();

        pMsg = (MSG *)env->GetPrimitiveArrayCritical(bdata, &dummy);
        if (pMsg == NULL) {
            throw std::bad_alloc();
        }
        pMsg->hwnd = hwnd;
        env->ReleasePrimitiveArrayCritical(bdata, (void *)pMsg, 0);
    }

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
