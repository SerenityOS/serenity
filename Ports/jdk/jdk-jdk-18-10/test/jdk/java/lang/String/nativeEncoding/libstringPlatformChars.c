/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>

#include "jni.h"
#include "jni_util.h"

JNIEXPORT jbyteArray JNICALL
Java_StringPlatformChars_getBytes(JNIEnv *env, jclass unused, jstring value)
{
    const char* str;
    int len;
    jbyteArray bytes = NULL;

    str = JNU_GetStringPlatformChars(env, value, NULL);
    if (str == NULL) {
        return NULL;
    }
    len = (int)strlen(str);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes != 0) {
        jclass strClazz = JNU_ClassString(env);
        if (strClazz == NULL) {
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *)str);

        return bytes;
    }
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_StringPlatformChars_newString(JNIEnv *env, jclass unused, jbyteArray bytes)
{
    char* str;
    int len = (*env)->GetArrayLength(env, bytes);
    int i;
    jbyte* jbytes;

    str = (char*)malloc(len + 1);
    jbytes = (*env)->GetPrimitiveArrayCritical(env, bytes, NULL);
    if (jbytes == NULL) {
        return NULL;
    }
    for (i = 0; i < len; i++) {
        str[i] = (char)jbytes[i];
    }
    str[len] = '\0';
    (*env)->ReleasePrimitiveArrayCritical(env, bytes, (void*)jbytes, 0);

    return JNU_NewStringPlatform(env, str);
}

