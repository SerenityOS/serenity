/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <signal.h>
#include <stdlib.h>

#include <jni.h>
#include <jvm.h>
#include <jni_util.h>
#include <jlong.h>
#include "jdk_internal_misc_Signal.h"

JNIEXPORT jint JNICALL
Java_jdk_internal_misc_Signal_findSignal0(JNIEnv *env, jclass cls, jstring name)
{
    jint res;
    const char *cname;
    if (name == NULL) {
        JNU_ThrowNullPointerException(env, "name");
        return 0;
    }
    cname = (*env)->GetStringUTFChars(env, name, 0);
    if (cname == NULL) {
        /* out of memory thrown */
        return 0;
    }
    res = JVM_FindSignal(cname);
    (*env)->ReleaseStringUTFChars(env, name, cname);
    return res;
}

JNIEXPORT jlong JNICALL
Java_jdk_internal_misc_Signal_handle0(JNIEnv *env, jclass cls, jint sig, jlong handler)
{
    return ptr_to_jlong(JVM_RegisterSignal(sig, jlong_to_ptr(handler)));
}

JNIEXPORT void JNICALL
Java_jdk_internal_misc_Signal_raise0(JNIEnv *env, jclass cls, jint sig)
{
    JVM_RaiseSignal(sig);
}
