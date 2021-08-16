/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Test if a file exists in the file system cache
 */
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/mman.h>

#include "jni.h"

/*
 * Throws an exception with the given class name and detail message
 */
static void ThrowException(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
}

/*
 * Class:     DirectIO
 * Method:    isFileInCache0
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean Java_DirectIOTest_isFileInCache0(JNIEnv *env,
                                                jclass cls,
                                                jint file_size,
                                                jstring file_path) {
    void *f_mmap;
#ifdef __linux__
    unsigned char *f_seg;
#else
    char *f_seg;
#endif

#ifdef __APPLE__
    size_t mask = MINCORE_INCORE;
#else
    size_t mask = 0x1;
#endif

    size_t page_size = getpagesize();
    size_t index = (file_size + page_size - 1) /page_size;
    jboolean result = JNI_FALSE;

    const char* path = (*env)->GetStringUTFChars(env, file_path, JNI_FALSE);

    int fd = open(path, O_RDWR);

    (*env)->ReleaseStringUTFChars(env, file_path, path);

    f_mmap = mmap(0, file_size, PROT_NONE, MAP_SHARED, fd, 0);
    if (f_mmap == MAP_FAILED) {
        close(fd);
        ThrowException(env, "java/io/IOException",
            "test of whether file exists in cache failed");
    }
    f_seg = malloc(index);
    if (f_seg != NULL) {
        if(mincore(f_mmap, file_size, f_seg) == 0) {
            size_t i;
            for (i = 0; i < index; i++) {
                if (f_seg[i] & mask) {
                    result = JNI_TRUE;
                    break;
                }
            }
        }
        free(f_seg);
    } else {
        ThrowException(env, "java/io/IOException",
            "test of whether file exists in cache failed");
    }
    close(fd);
    munmap(f_mmap, file_size);
    return result;
}
