/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Native method support for java.util.zip.Deflater
 */

#include <stdio.h>
#include <stdlib.h>
#include "jlong.h"
#include "jni.h"
#include "jni_util.h"
#include <zlib.h>

#include "java_util_zip_Deflater.h"

#define DEF_MEM_LEVEL 8

JNIEXPORT jlong JNICALL
Java_java_util_zip_Deflater_init(JNIEnv *env, jclass cls, jint level,
                                 jint strategy, jboolean nowrap)
{
    z_stream *strm = calloc(1, sizeof(z_stream));

    if (strm == 0) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return jlong_zero;
    } else {
        const char *msg;
        int ret = deflateInit2(strm, level, Z_DEFLATED,
                               nowrap ? -MAX_WBITS : MAX_WBITS,
                               DEF_MEM_LEVEL, strategy);
        switch (ret) {
          case Z_OK:
            return ptr_to_jlong(strm);
          case Z_MEM_ERROR:
            free(strm);
            JNU_ThrowOutOfMemoryError(env, 0);
            return jlong_zero;
          case Z_STREAM_ERROR:
            free(strm);
            JNU_ThrowIllegalArgumentException(env, 0);
            return jlong_zero;
          default:
            msg = ((strm->msg != NULL) ? strm->msg :
                   (ret == Z_VERSION_ERROR) ?
                   "zlib returned Z_VERSION_ERROR: "
                   "compile time and runtime zlib implementations differ" :
                   "unknown error initializing zlib library");
            free(strm);
            JNU_ThrowInternalError(env, msg);
            return jlong_zero;
        }
    }
}

static void throwInternalErrorHelper(JNIEnv *env, z_stream *strm, const char *fixmsg) {
    const char *msg = NULL;
    msg = (strm->msg != NULL) ? strm->msg : fixmsg;
    JNU_ThrowInternalError(env, msg);
}

static void checkSetDictionaryResult(JNIEnv *env, jlong addr, jint res)
{
    z_stream *strm = (z_stream *) jlong_to_ptr(addr);
    switch (res) {
    case Z_OK:
        break;
    case Z_STREAM_ERROR:
        JNU_ThrowIllegalArgumentException(env, 0);
        break;
    default:
        throwInternalErrorHelper(env, strm, "unknown error in checkSetDictionaryResult");
        break;
    }
}

JNIEXPORT void JNICALL
Java_java_util_zip_Deflater_setDictionary(JNIEnv *env, jclass cls, jlong addr,
                                          jbyteArray b, jint off, jint len)
{
    int res;
    Bytef *buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
    if (buf == NULL) /* out of memory */
        return;
    res = deflateSetDictionary(jlong_to_ptr(addr), buf + off, len);
    (*env)->ReleasePrimitiveArrayCritical(env, b, buf, 0);
    checkSetDictionaryResult(env, addr, res);
}

JNIEXPORT void JNICALL
Java_java_util_zip_Deflater_setDictionaryBuffer(JNIEnv *env, jclass cls, jlong addr,
                                          jlong bufferAddr, jint len)
{
    int res;
    Bytef *buf = jlong_to_ptr(bufferAddr);
    res = deflateSetDictionary(jlong_to_ptr(addr), buf, len);
    checkSetDictionaryResult(env, addr, res);
}

static jint doDeflate(JNIEnv *env, jlong addr,
                       jbyte *input, jint inputLen,
                       jbyte *output, jint outputLen,
                       jint flush, jint params)
{
    z_stream *strm = jlong_to_ptr(addr);
    int setParams = params & 1;
    int res;

    strm->next_in  = (Bytef *) input;
    strm->next_out = (Bytef *) output;
    strm->avail_in  = inputLen;
    strm->avail_out = outputLen;

    if (setParams) {
        int strategy = (params >> 1) & 3;
        int level = params >> 3;
        res = deflateParams(strm, level, strategy);
    } else {
        res = deflate(strm, flush);
    }
    return res;
}

static jlong checkDeflateStatus(JNIEnv *env, jlong addr,
                        jint inputLen,
                        jint outputLen,
                        jint params, int res)
{
    z_stream *strm = jlong_to_ptr(addr);
    jint inputUsed = 0, outputUsed = 0;
    int finished = 0;
    int setParams = params & 1;

    if (setParams) {
        switch (res) {
        case Z_OK:
            setParams = 0;
            /* fall through */
        case Z_BUF_ERROR:
            inputUsed = inputLen - strm->avail_in;
            outputUsed = outputLen - strm->avail_out;
            break;
        default:
            throwInternalErrorHelper(env, strm, "unknown error in checkDeflateStatus, setParams case");
            return 0;
        }
    } else {
        switch (res) {
        case Z_STREAM_END:
            finished = 1;
            /* fall through */
        case Z_OK:
        case Z_BUF_ERROR:
            inputUsed = inputLen - strm->avail_in;
            outputUsed = outputLen - strm->avail_out;
            break;
        default:
            throwInternalErrorHelper(env, strm, "unknown error in checkDeflateStatus");
            return 0;
        }
    }
    return ((jlong)inputUsed) | (((jlong)outputUsed) << 31) | (((jlong)finished) << 62) | (((jlong)setParams) << 63);
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Deflater_deflateBytesBytes(JNIEnv *env, jobject this, jlong addr,
                                         jbyteArray inputArray, jint inputOff, jint inputLen,
                                         jbyteArray outputArray, jint outputOff, jint outputLen,
                                         jint flush, jint params)
{
    jbyte *input = (*env)->GetPrimitiveArrayCritical(env, inputArray, 0);
    jbyte *output;
    jlong retVal;
    jint res;

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

     res = doDeflate(env, addr, input + inputOff, inputLen,output + outputOff,
                     outputLen, flush, params);

    (*env)->ReleasePrimitiveArrayCritical(env, outputArray, output, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, inputArray, input, 0);

    retVal = checkDeflateStatus(env, addr, inputLen, outputLen, params, res);
    return retVal;
}


JNIEXPORT jlong JNICALL
Java_java_util_zip_Deflater_deflateBytesBuffer(JNIEnv *env, jobject this, jlong addr,
                                         jbyteArray inputArray, jint inputOff, jint inputLen,
                                         jlong outputBuffer, jint outputLen,
                                         jint flush, jint params)
{
    jbyte *input = (*env)->GetPrimitiveArrayCritical(env, inputArray, 0);
    jbyte *output;
    jlong retVal;
    jint res;
    if (input == NULL) {
        if (inputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }
    output = jlong_to_ptr(outputBuffer);

    res = doDeflate(env, addr, input + inputOff, inputLen, output, outputLen,
                    flush, params);

    (*env)->ReleasePrimitiveArrayCritical(env, inputArray, input, 0);

    retVal = checkDeflateStatus(env, addr, inputLen, outputLen, params, res);
    return retVal;
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Deflater_deflateBufferBytes(JNIEnv *env, jobject this, jlong addr,
                                         jlong inputBuffer, jint inputLen,
                                         jbyteArray outputArray, jint outputOff, jint outputLen,
                                         jint flush, jint params)
{
    jbyte *input = jlong_to_ptr(inputBuffer);
    jbyte *output = (*env)->GetPrimitiveArrayCritical(env, outputArray, 0);
    jlong retVal;
    jint res;
    if (output == NULL) {
        if (outputLen != 0 && (*env)->ExceptionOccurred(env) == NULL)
            JNU_ThrowOutOfMemoryError(env, 0);
        return 0L;
    }

    res = doDeflate(env, addr, input, inputLen, output + outputOff, outputLen,
                    flush, params);
    (*env)->ReleasePrimitiveArrayCritical(env, outputArray, output, 0);

    retVal = checkDeflateStatus(env, addr, inputLen, outputLen, params, res);
    return retVal;
}

JNIEXPORT jlong JNICALL
Java_java_util_zip_Deflater_deflateBufferBuffer(JNIEnv *env, jobject this, jlong addr,
                                         jlong inputBuffer, jint inputLen,
                                         jlong outputBuffer, jint outputLen,
                                         jint flush, jint params)
{
    jbyte *input = jlong_to_ptr(inputBuffer);
    jbyte *output = jlong_to_ptr(outputBuffer);
    jlong retVal;
    jint res;

    res = doDeflate(env, addr, input, inputLen, output, outputLen, flush, params);
    retVal = checkDeflateStatus(env, addr, inputLen, outputLen, params, res);
    return retVal;
}

JNIEXPORT jint JNICALL
Java_java_util_zip_Deflater_getAdler(JNIEnv *env, jclass cls, jlong addr)
{
    return ((z_stream *)jlong_to_ptr(addr))->adler;
}

JNIEXPORT void JNICALL
Java_java_util_zip_Deflater_reset(JNIEnv *env, jclass cls, jlong addr)
{
    if (deflateReset((z_stream *)jlong_to_ptr(addr)) != Z_OK) {
        JNU_ThrowInternalError(env, "deflateReset failed");
    }
}

JNIEXPORT void JNICALL
Java_java_util_zip_Deflater_end(JNIEnv *env, jclass cls, jlong addr)
{
    if (deflateEnd((z_stream *)jlong_to_ptr(addr)) == Z_STREAM_ERROR) {
        JNU_ThrowInternalError(env, "deflateEnd failed");
    } else {
        free((z_stream *)jlong_to_ptr(addr));
    }
}
