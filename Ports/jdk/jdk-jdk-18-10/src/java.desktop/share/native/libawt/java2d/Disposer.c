/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_util.h"
#include "Disposer.h"

static jmethodID addRecordMID = NULL;
static jclass dispClass = NULL;

/*
 * Class:     sun_java2d_Disposer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_Disposer_initIDs(JNIEnv *env, jclass disposerClass)
{
    addRecordMID = (*env)->GetStaticMethodID(env, disposerClass, "addRecord",
                                             "(Ljava/lang/Object;JJ)V");
    if (addRecordMID != 0) {
        dispClass = (*env)->NewGlobalRef(env, disposerClass);
    }
}

JNIEXPORT void JNICALL
Disposer_AddRecord(JNIEnv *env, jobject obj, GeneralDisposeFunc disposer, jlong pData) {

    if (dispClass == NULL) {
        /* Needed to initialize the Disposer class as it may be not yet referenced */
        jclass clazz = (*env)->FindClass(env, "sun/java2d/Disposer");
        if ((*env)->ExceptionCheck(env)) {
            // If there's exception pending, we'll just return.
            return;
        }
    }

    (*env)->CallStaticVoidMethod(env, dispClass, addRecordMID,
                                 obj, ptr_to_jlong(disposer), pData);
}

/*
 * Class:     sun_java2d_DefaultDisposerRecord
 * Method:    invokeNativeDispose
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_DefaultDisposerRecord_invokeNativeDispose(JNIEnv *env, jclass dispClass,
                                             jlong disposer, jlong pData)
{
    if (disposer != 0 && pData != 0) {
        GeneralDisposeFunc *disposeMethod = (GeneralDisposeFunc*)(jlong_to_ptr(disposer));
        disposeMethod(env, pData);
    }
}
