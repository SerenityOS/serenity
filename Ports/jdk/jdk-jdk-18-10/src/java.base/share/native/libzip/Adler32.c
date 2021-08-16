/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Native method support for java.util.zip.Adler32
 */

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include <zlib.h>

#include "java_util_zip_Adler32.h"

JNIEXPORT jint JNICALL
Java_java_util_zip_Adler32_update(JNIEnv *env, jclass cls, jint adler, jint b)
{
    Bytef buf[1];

    buf[0] = (Bytef)b;
    return adler32(adler, buf, 1);
}

JNIEXPORT jint JNICALL
Java_java_util_zip_Adler32_updateBytes(JNIEnv *env, jclass cls, jint adler,
                                       jarray b, jint off, jint len)
{
    Bytef *buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
    if (buf) {
        adler = adler32(adler, buf + off, len);
        (*env)->ReleasePrimitiveArrayCritical(env, b, buf, 0);
    }
    return adler;
}


JNIEXPORT jint JNICALL
Java_java_util_zip_Adler32_updateByteBuffer(JNIEnv *env, jclass cls, jint adler,
                                       jlong address, jint off, jint len)
{
    Bytef *buf = (Bytef *)jlong_to_ptr(address);
    if (buf) {
        adler = adler32(adler, buf + off, len);
    }
    return adler;
}
