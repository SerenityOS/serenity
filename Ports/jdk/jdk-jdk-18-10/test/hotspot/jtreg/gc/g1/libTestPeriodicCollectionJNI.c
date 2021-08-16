/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Native support for TestPeriodicCollectionJNI test.
 */

#include "jni.h"

#ifdef WINDOWS
#include <windows.h>
#else
#include <unistd.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

static volatile int release_critical = 0;

JNIEXPORT jboolean JNICALL
Java_gc_g1_TestPeriodicCollectionJNI_blockInNative(JNIEnv* env, jobject obj, jintArray dummy) {
    void* native_array = (*env)->GetPrimitiveArrayCritical(env, dummy, 0);

    if (native_array == NULL) {
        return JNI_FALSE;
    }

    while (!release_critical) {
#ifdef WINDOWS
        Sleep(1);
#else
        usleep(1000);
#endif
    }

    (*env)->ReleasePrimitiveArrayCritical(env, dummy, native_array, 0);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_gc_g1_TestPeriodicCollectionJNI_unblock(JNIEnv *env, jobject obj)
{
    release_critical = 1;
}

#ifdef __cplusplus
}
#endif
