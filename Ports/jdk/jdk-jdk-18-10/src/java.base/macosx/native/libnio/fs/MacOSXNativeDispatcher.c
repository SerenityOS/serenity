/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>

#include <CoreFoundation/CoreFoundation.h>

JNIEXPORT jcharArray JNICALL
Java_sun_nio_fs_MacOSXNativeDispatcher_normalizepath(JNIEnv* env, jclass this,
                                                     jcharArray path,
                                                     jint form)
{
    jcharArray result = NULL;
    char *chars;
    CFMutableStringRef csref = CFStringCreateMutable(NULL, 0);
    if (csref == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
        return NULL;
    }
    chars = (char*)(*env)->GetPrimitiveArrayCritical(env, path, 0);
    if (chars != NULL) {
        char chars_buf[(PATH_MAX + 1) * 2];     // utf16 + zero padding
        jsize len = (*env)->GetArrayLength(env, path);
        CFStringAppendCharacters(csref, (const UniChar*)chars, len);
        (*env)->ReleasePrimitiveArrayCritical(env, path, chars, 0);
        CFStringNormalize(csref, form);
        len = CFStringGetLength(csref);
        if (len < PATH_MAX) {
            if (CFStringGetCString(csref, chars_buf, sizeof(chars_buf), kCFStringEncodingUTF16)) {
                result = (*env)->NewCharArray(env, len);
                if (result != NULL) {
                    (*env)->SetCharArrayRegion(env, result, 0, len, (jchar*)&chars_buf);
                }
            }
        } else {
            int ulen = (len + 1) * 2;
            chars = malloc(ulen);
            if (chars == NULL) {
                JNU_ThrowOutOfMemoryError(env, "native heap");
            } else {
                if (CFStringGetCString(csref, chars, ulen, kCFStringEncodingUTF16)) {
                    result = (*env)->NewCharArray(env, len);
                    if (result != NULL) {
                        (*env)->SetCharArrayRegion(env, result, 0, len, (jchar*)chars);
                    }
                }
                free(chars);
            }
        }
    }
    CFRelease(csref);
    return result;
}
