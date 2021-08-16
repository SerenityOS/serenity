/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
#include "nio.h"
#include "nio_util.h"
#include "sun_nio_ch_FileKey.h"

static jfieldID key_volumeSN;    /* id for FileKey.dwVolumeSerialNumber */
static jfieldID key_indexHigh;   /* id for FileKey.nFileIndexHigh */
static jfieldID key_indexLow;    /* id for FileKey.nFileIndexLow */


JNIEXPORT void JNICALL
Java_sun_nio_ch_FileKey_initIDs(JNIEnv *env, jclass clazz)
{
    CHECK_NULL(key_volumeSN = (*env)->GetFieldID(env, clazz, "dwVolumeSerialNumber", "J"));
    CHECK_NULL(key_indexHigh = (*env)->GetFieldID(env, clazz, "nFileIndexHigh", "J"));
    CHECK_NULL(key_indexLow = (*env)->GetFieldID(env, clazz, "nFileIndexLow", "J"));
}


JNIEXPORT void JNICALL
Java_sun_nio_ch_FileKey_init(JNIEnv *env, jobject this, jobject fdo)
{
    HANDLE fileHandle = (HANDLE)(handleval(env, fdo));
    BOOL result;
    BY_HANDLE_FILE_INFORMATION fileInfo;

    result = GetFileInformationByHandle(fileHandle, &fileInfo);
    if (result) {
        (*env)->SetLongField(env, this, key_volumeSN, fileInfo.dwVolumeSerialNumber);
        (*env)->SetLongField(env, this, key_indexHigh, fileInfo.nFileIndexHigh);
        (*env)->SetLongField(env, this, key_indexLow, fileInfo.nFileIndexLow);
    } else {
        JNU_ThrowIOExceptionWithLastError(env, "GetFileInformationByHandle failed");
    }
}
