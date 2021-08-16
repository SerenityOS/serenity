/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Native method support for java.util.zip.Inflater
 */

#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include "jlong.h"
#include "jni.h"
#include "jvm.h"
#include "jni_util.h"
#include <zlib.h>
#include "java_util_zip_Inflater.h"

#define ThrowDataFormatException(env, msg) \
        JNU_ThrowByName(env, "java/util/zip/DataFormatException", msg)

static jfieldID inputConsumedID;
static jfieldID outputConsumedID;

JNIEXPORT void JNICALL
Java_java_util_zip_Inflater_initIDs(JNIEnv *env, jclass cls)
{
    inputConsumedID = (*env)->GetFieldID(env, cls, "inputConsumed", "I");
    outputConsumedID = (*env)->GetFieldID(env, cls, "outputConsumed", "I");
    CHECK_NULL(inputConsumedID);
    CHECK_NULL(outputConsumedID);
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Inflater_init(JNIEnv *env, jclass cls, jboolean nowrap)
{
    z_stream *strm = calloc(1, sizeof(z_stream));

    if (strm == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return jlong_zero;
    } else {
        const char *msg;
        int ret = inflateInit2(strm, nowrap ? -MAX_WBITS : MAX_WBITS);
        switch (ret) {
          case Z_OK:
            return ptr_to_jlong(strm);
          case Z_MEM_ERROR:
            free(strm);
            JNU_ThrowOutOfMemoryError(env, 0);
            return jlong_zero;
          default:
            msg = ((strm->msg != NULL) ? strm->msg :
                   (ret == Z_VERSION_ERROR) ?
                   "zlib returned Z_VERSION_ERROR: "
                   "compile time and runtime zlib implementations differ" :
                   (ret == Z_STREAM_ERROR) ?
                   "inflateInit2 returned Z_STREAM_ERROR" :
                   "unknown error initializing zlib library");
            free(strm);
            JNU_ThrowInternalError(env, msg);
            return jlong_zero;
        }
    }
}

static void checkSetDictionaryResult(JNIEnv *env, jlong addr, int res)
{
    switch (res) {
    case Z_OK:
        break;
    case Z_STREAM_ERROR:
    case Z_DATA_ERROR:
        JNU_ThrowIllegalArgumentException(env, ((z_stream *)jlong_to_ptr(addr))->msg);
        break;
    default:
        JNU_ThrowInternalError(env, ((z_stream *)jlong_to_ptr(addr))->msg);
        break;
    }
}

JNIEXPORT void JNICALL
Java_java_util_zip_Inflater_setDictionary(JNIEnv *env, jclass cls, jlong addr,
                                          jbyteArray b, jint off, jint len)
{
    jint res;
    Bytef *buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
    if (buf == NULL) /* out of memory */
        return;
    res = inflateSetDictionary(jlong_to_ptr(addr), buf + off, len);
    (*env)->ReleasePrimitiveArrayCritical(env, b, buf, 0);
    checkSetDictionaryResult(env, addr, res);
}

JNIEXPORT void JNICALL
Java_java_util_zip_Inflater_setDictionaryBuffer(JNIEnv *env, jclass cls, jlong addr,
                                          jlong bufferAddr, jint len)
{
    jint res;
    Bytef *buf = jlong_to_ptr(bufferAddr);
    res = inflateSetDictionary(jlong_to_ptr(addr), buf, len);
    checkSetDictionaryResult(env, addr, res);
}

static jint doInflate(jlong addr,
                       jbyte *input, jint inputLen,
                       jbyte *output, jint outputLen)
{
    jint ret;
    z_stream *strm = jlong_to_ptr(addr);

    strm->next_in  = (Bytef *) input;
    strm->next_out = (Bytef *) output;
    strm->avail_in  = inputLen;
    strm->avail_out = outputLen;

    ret = inflate(strm, Z_PARTIAL_FLUSH);
    return ret;
}

static jlong checkInflateStatus(JNIEnv *env, jobject this, jlong addr,
                        jint inputLen, jint outputLen, jint ret )
{
    z_stream *strm = jlong_to_ptr(addr);
    jint inputUsed = 0, outputUsed = 0;
    int finished = 0;
    int needDict = 0;

    switch (ret) {
    case Z_STREAM_END:
        finished = 1;
        /* fall through */
    case Z_OK:
        inputUsed = inputLen - strm->avail_in;
        outputUsed = outputLen - strm->avail_out;
        break;
    case Z_NEED_DICT:
        needDict = 1;
        /* Might have consumed some input here! */
        inputUsed = inputLen - strm->avail_in;
        /* zlib is unclear about whether output may be produced */
        outputUsed = outputLen - strm->avail_out;
        break;
    case Z_BUF_ERROR:
        break;
    case Z_DATA_ERROR:
        inputUsed = inputLen - strm->avail_in;
        (*env)->SetIntField(env, this, inputConsumedID, inputUsed);
        outputUsed = outputLen - strm->avail_out;
        (*env)->SetIntField(env, this, outputConsumedID, outputUsed);
        ThrowDataFormatException(env, strm->msg);
        break;
    case Z_MEM_ERROR:
        JNU_ThrowOutOfMemoryError(env, 0);
        break;
    default:
        JNU_ThrowInternalError(env, strm->msg);
        break;
    }
    return ((jlong)inputUsed) | (((jlong)outputUsed) << 31) | (((jlong)finished) << 62) | (((jlong)needDict) << 63);
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Inflater_inflateBytesBytes(JNIEnv *env, jobject this, jlong addr,
                                         jbyteArray inputArray, jint inputOff, jint inputLen,
                                         jbyteArray outputArray, jint outputOff, jint outputLen)
{
    jbyte *input = (*env)->GetPrimitiveArrayCritical(env, inputArray, 0);
    jbyte *output;
    jint ret;
    jlong retVal;

    if (input == NULL) {
        if (inputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }
    output = (*env)->GetPrimitiveArrayCritical(env, outputArray, 0);
    if (output == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, inputArray, input, 0);
        if (outputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }

    ret = doInflate(addr, input + inputOff, inputLen, output + outputOff,
                    outputLen);

    (*env)->ReleasePrimitiveArrayCritical(env, outputArray, output, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, inputArray, input, 0);

    retVal = checkInflateStatus(env, this, addr, inputLen, outputLen, ret );
    return retVal;
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Inflater_inflateBytesBuffer(JNIEnv *env, jobject this, jlong addr,
                                         jbyteArray inputArray, jint inputOff, jint inputLen,
                                         jlong outputBuffer, jint outputLen)
{
    jbyte *input = (*env)->GetPrimitiveArrayCritical(env, inputArray, 0);
    jbyte *output;
    jint ret;
    jlong retVal;

    if (input == NULL) {
        if (inputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }
    output = jlong_to_ptr(outputBuffer);

    ret = doInflate(addr, input + inputOff, inputLen, output, outputLen);

    (*env)->ReleasePrimitiveArrayCritical(env, inputArray, input, 0);
    retVal = checkInflateStatus(env, this, addr, inputLen, outputLen, ret );

    return retVal;
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Inflater_inflateBufferBytes(JNIEnv *env, jobject this, jlong addr,
                                         jlong inputBuffer, jint inputLen,
                                         jbyteArray outputArray, jint outputOff, jint outputLen)
{
    jbyte *input = jlong_to_ptr(inputBuffer);
    jbyte *output = (*env)->GetPrimitiveArrayCritical(env, outputArray, 0);
    jint ret;
    jlong retVal;

    if (output == NULL) {
        if (outputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }

    ret = doInflate(addr, input, inputLen, output  + outputOff, outputLen);

    (*env)->ReleasePrimitiveArrayCritical(env, outputArray, output, 0);
    retVal = checkInflateStatus(env, this, addr, inputLen, outputLen, ret );

    return retVal;
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Inflater_inflateBufferBuffer(JNIEnv *env, jobject this, jlong addr,
                                         jlong inputBuffer, jint inputLen,
                                         jlong outputBuffer, jint outputLen)
{
    jbyte *input = jlong_to_ptr(inputBuffer);
    jbyte *output = jlong_to_ptr(outputBuffer);
    jint ret;
    jlong retVal;

    ret = doInflate(addr, input, inputLen, output, outputLen);
    retVal = checkInflateStatus(env, this, addr, inputLen, outputLen, ret);
    return retVal;
}

JNIEXPORT jint JNICALL
Java_java_util_zip_Inflater_getAdler(JNIEnv *env, jclass cls, jlong addr)
{
    return ((z_stream *)jlong_to_ptr(addr))->adler;
}

JNIEXPORT void JNICALL
Java_java_util_zip_Inflater_reset(JNIEnv *env, jclass cls, jlong addr)
{
    if (inflateReset(jlong_to_ptr(addr)) != Z_OK) {
        JNU_ThrowInternalError(env, 0);
    }
}

JNIEXPORT void JNICALL
Java_java_util_zip_Inflater_end(JNIEnv *env, jclass cls, jlong addr)
{
    if (inflateEnd(jlong_to_ptr(addr)) == Z_STREAM_ERROR) {
        JNU_ThrowInternalError(env, 0);
    } else {
        free(jlong_to_ptr(addr));
    }
}
