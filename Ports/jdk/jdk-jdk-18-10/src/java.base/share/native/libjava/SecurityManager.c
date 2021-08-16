/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"

#include "java_lang_SecurityManager.h"
#include "java_lang_ClassLoader.h"

/*
 * Make sure a security manager instance is initialized.
 * TRUE means OK, FALSE means not.
 */
static jboolean
check(JNIEnv *env, jobject this)
{
    static jfieldID initField = 0;
    jboolean initialized = JNI_FALSE;

    if (initField == 0) {
        jclass clazz = (*env)->FindClass(env, "java/lang/SecurityManager");
        if (clazz == 0) {
            (*env)->ExceptionClear(env);
            return JNI_FALSE;
        }
        initField = (*env)->GetFieldID(env, clazz, "initialized", "Z");
        if (initField == 0) {
            (*env)->ExceptionClear(env);
            return JNI_FALSE;
        }
    }
    initialized = (*env)->GetBooleanField(env, this, initField);

    if (initialized == JNI_TRUE) {
        return JNI_TRUE;
    } else {
        jclass securityException =
            (*env)->FindClass(env, "java/lang/SecurityException");
        if (securityException != 0) {
            (*env)->ThrowNew(env, securityException,
                             "security manager not initialized.");
        }
        return JNI_FALSE;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_java_lang_SecurityManager_getClassContext(JNIEnv *env, jobject this)
{
    if (!check(env, this)) {
        return NULL;            /* exception */
    }

    return JVM_GetClassContext(env);
}
