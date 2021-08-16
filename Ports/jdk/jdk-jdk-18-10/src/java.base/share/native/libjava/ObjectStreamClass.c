/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"

#include "java_io_ObjectStreamClass.h"

static jclass noSuchMethodErrCl;

/*
 * Class:     java_io_ObjectStreamClass
 * Method:    initNative
 * Signature: ()V
 *
 * Native code initialization hook.
 */
JNIEXPORT void JNICALL
Java_java_io_ObjectStreamClass_initNative(JNIEnv *env, jclass this)
{
    jclass cl = (*env)->FindClass(env, "java/lang/NoSuchMethodError");
    if (cl == NULL) {           /* exception thrown */
        return;
    }
    noSuchMethodErrCl = (*env)->NewGlobalRef(env, cl);
}

/*
 * Class:     java_io_ObjectStreamClass
 * Method:    hasStaticInitializer
 * Signature: (Ljava/lang/Class;)Z
 *
 * Returns true if the given class defines a <clinit>()V method; returns false
 * otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_java_io_ObjectStreamClass_hasStaticInitializer(JNIEnv *env, jclass this,
                                                    jclass clazz)
{
    jclass superCl = NULL;
    jmethodID superClinitId = NULL;
    jmethodID clinitId =
        (*env)->GetStaticMethodID(env, clazz, "<clinit>", "()V");
    if (clinitId == NULL) {     /* error thrown */
        jthrowable th = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);    /* normal return */
        if (!(*env)->IsInstanceOf(env, th, noSuchMethodErrCl)) {
            (*env)->Throw(env, th);
        }
        return JNI_FALSE;
    }

    /*
     * Check superclass for static initializer as well--if the same method ID
     * is returned, then the static initializer is from a superclass.
     * Empirically, this step appears to be unnecessary in 1.4; however, the
     * JNI spec makes no guarantee that GetStaticMethodID will not return the
     * ID for a superclass initializer.
     */

    if ((superCl = (*env)->GetSuperclass(env, clazz)) == NULL) {
        return JNI_TRUE;
    }
    superClinitId =
        (*env)->GetStaticMethodID(env, superCl, "<clinit>", "()V");
    if (superClinitId == NULL) {        /* error thrown */
        jthrowable th = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);    /* normal return */
        if (!(*env)->IsInstanceOf(env, th, noSuchMethodErrCl)) {
            (*env)->Throw(env, th);
        }
        return JNI_TRUE;
    }

    return (clinitId != superClinitId);
}
