/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"
#include "java_nio_MappedMemoryUtils.h"
#include <stdlib.h>

JNIEXPORT jboolean JNICALL
Java_java_nio_MappedMemoryUtils_isLoaded0(JNIEnv *env, jobject obj, jlong address,
                                         jlong len, jlong numPages)
{
    jboolean loaded = JNI_FALSE;
    /* Information not available?
    MEMORY_BASIC_INFORMATION info;
    void *a = (void *) jlong_to_ptr(address);
    int result = VirtualQuery(a, &info, (DWORD)len);
    */
    return loaded;
}

JNIEXPORT void JNICALL
Java_java_nio_MappedMemoryUtils_load0(JNIEnv *env, jobject obj, jlong address,
                                     jlong len)
{
    // no madvise available
}

JNIEXPORT void JNICALL
Java_java_nio_MappedMemoryUtils_unload0(JNIEnv *env, jobject obj, jlong address,
                                     jlong len)
{
    // no madvise available
}

JNIEXPORT void JNICALL
Java_java_nio_MappedMemoryUtils_force0(JNIEnv *env, jobject obj, jobject fdo,
                                      jlong address, jlong len)
{
    void *a = (void *) jlong_to_ptr(address);
    BOOL result;
    int retry;

    /*
     * FlushViewOfFile can fail with ERROR_LOCK_VIOLATION if the memory
     * system is writing dirty pages to disk. As there is no way to
     * synchronize the flushing then we retry a limited number of times.
     */
    retry = 0;
    do {
        result = FlushViewOfFile(a, (DWORD)len);
        if ((result != 0) || (GetLastError() != ERROR_LOCK_VIOLATION))
            break;
        retry++;
    } while (retry < 5);

    /**
     * FlushViewOfFile only initiates the writing of dirty pages to disk
     * so we have to call FlushFileBuffers to and ensure they are written.
     */
    if (result != 0) {
        // by right, the jfieldID initialization should be in a static
        // initializer but we do it here instead to avoiding needing to
        // load nio.dll during startup.
        static jfieldID handle_fdID;
        HANDLE h;
        if (handle_fdID == NULL) {
            jclass clazz = (*env)->FindClass(env, "java/io/FileDescriptor");
            CHECK_NULL(clazz); //exception thrown
            handle_fdID = (*env)->GetFieldID(env, clazz, "handle", "J");
            CHECK_NULL(handle_fdID);
        }
        h = jlong_to_ptr((*env)->GetLongField(env, fdo, handle_fdID));
        result = FlushFileBuffers(h);
        if (result == 0 && GetLastError() == ERROR_ACCESS_DENIED) {
            // read-only mapping
            result = 1;
        }
    }

    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Flush failed");
    }
}
