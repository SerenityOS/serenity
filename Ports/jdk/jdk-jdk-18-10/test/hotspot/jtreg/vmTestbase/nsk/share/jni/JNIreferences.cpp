/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
#include "jni.h"
#include <stdlib.h>
#include "nsk_tools.h"

extern "C" {


static jobject* globalReferences = NULL;
static jweak* weakReferences = NULL;

JNIEXPORT jint JNICALL
Java_nsk_share_ReferringObject_createJNIGlobalReferenceNative(JNIEnv *env,
        jobject thisObject, jobject object, jint maxJNIGlobalReferences)
{
        jint i;
        jint result = -1;

        if (globalReferences == NULL)
        {
                globalReferences = (jobject*)malloc(sizeof(jobject) * maxJNIGlobalReferences);

                if (globalReferences == NULL)
                {
                        NSK_COMPLAIN0("malloc return NULL\n");
                        return -1;
                }

                for (i = 0; i < maxJNIGlobalReferences; i++)
                {
                        globalReferences[i] = NULL;
                }
        }

        for (i = 0; i < maxJNIGlobalReferences; i++)
        {
                jobject reference = globalReferences[i];

                if (reference == NULL)
                {
                        reference = env->NewGlobalRef(object);

                        if (reference == NULL)
                        {
                                NSK_COMPLAIN0("NewGlobalRef return NULL\n");

                                env->ThrowNew(
                                    env->FindClass("nsk/share/TestJNIError"),
                                    "NewGlobalRef return NULL");
                        }

                        globalReferences[i] = reference;

                        result = i;

                        break;
                }
        }

        return result;
}

JNIEXPORT void JNICALL
Java_nsk_share_ReferringObject_deleteJNIGlobalReferenceNative(JNIEnv *env,
        jobject thisObject, jint index)
{
        jobject reference = globalReferences[index];

        if (reference == NULL)
        {
                NSK_COMPLAIN1("globalReferences[%d] = NULL, possible wrong index is passed\n", index);

                env->ThrowNew(
                    env->FindClass("nsk/share/TestBug"),
                    "Requested globalReferences[] element is NULL, possible wrong index is passed");
        }

        env->DeleteGlobalRef(reference);

        globalReferences[index] = NULL;
}


JNIEXPORT void JNICALL
Java_nsk_share_ReferringObject_createJNILocalReferenceNative(JNIEnv *env,
        jobject thisObject, jobject object, jobject createWicket, jobject deleteWicket)
{
        jobject reference = env->NewLocalRef(object);
        jclass klass;

        if (reference == NULL)
        {
                NSK_COMPLAIN0("NewLocalRef return NULL\n");

                env->ThrowNew(
                    env->FindClass("nsk/share/TestJNIError"),
                    "NewLocalRef return NULL");
        }

        klass = env->GetObjectClass(createWicket);

        // notify another thread that JNI local reference has been created
        env->CallVoidMethod(createWicket,
                            env->GetMethodID(klass, "unlock", "()V"));

        // wait till JNI local reference can be released (it will heppen then we will leave the method)
        env->CallVoidMethod(deleteWicket,
                            env->GetMethodID(klass, "waitFor", "()V"));
}

JNIEXPORT jint JNICALL
Java_nsk_share_ReferringObject_createJNIWeakReferenceNative(JNIEnv *env,
        jobject thisObject, jobject object, jint maxJNIWeakReferences)
{
        jint i;
        jint result = -1;

        if (weakReferences == NULL)
        {
                weakReferences = (jweak*)malloc(sizeof(jweak) * maxJNIWeakReferences);

                if (weakReferences == NULL)
                {
                        NSK_COMPLAIN0("malloc return NULL\n");

                        return -1;
                }

                for (i = 0; i < maxJNIWeakReferences; i++)
                {
                        weakReferences[i] = NULL;
                }
        }

        for (i = 0; i < maxJNIWeakReferences; i++)
        {
                jobject reference = weakReferences[i];

                if (reference == NULL)
                {
                        reference = env->NewWeakGlobalRef(object);

                        if (reference == NULL)
                        {
                                NSK_COMPLAIN0("NewWeakGlobalRef return NULL\n");

                                env->ThrowNew(
                                    env->FindClass("nsk/share/TestJNIError"),
                                    "NewWeakGlobalRef return NULL");
                        }

                        weakReferences[i] = reference;

                        result = i;

                        break;
                }
        }

        return result;
}

JNIEXPORT void JNICALL
Java_nsk_share_ReferringObject_deleteJNIWeakReferenceNative(JNIEnv *env,
        jobject thisObject, jint index)
{
        jweak reference = weakReferences[index];

        if (reference == NULL)
        {
                NSK_COMPLAIN1("weakReferences[%d] = NULL, possible wrong index is passed\n", index);

                env->ThrowNew(
                    env->FindClass("nsk/share/TestBug"),
                    "Requested weakReferences[] element is NULL, possible wrong index is passed");
        }

        if (env->IsSameObject(reference, NULL) == JNI_TRUE)
        {
                NSK_COMPLAIN0("TEST BUG: Weak reference was collected\n");

                env->ThrowNew(
                    env->FindClass("nsk/share/TestBug"),
                    "TEST BUG: Weak reference was collected");
        }

        env->DeleteWeakGlobalRef(reference);

        weakReferences[index] = NULL;
}

}
