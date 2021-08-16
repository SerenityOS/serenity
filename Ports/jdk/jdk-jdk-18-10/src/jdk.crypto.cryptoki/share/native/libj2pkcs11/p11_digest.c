/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 */

/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

#include "pkcs11wrapper.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "jlong.h"

#include "sun_security_pkcs11_wrapper_PKCS11.h"

#ifdef P11_ENABLE_C_DIGESTINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DigestInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1DigestInit
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_DigestInit)(ckSessionHandle, ckpMechanism);

    freeCKMechanismPtr(ckpMechanism);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_DIGEST
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Digest
 * Signature: (J[BII[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jDigest          CK_BYTE_PTR pDigest
 *                                      CK_ULONG_PTR pulDigestLen
 */
JNIEXPORT jint JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1DigestSingle
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism,
     jbyteArray jIn, jint jInOfs, jint jInLen, jbyteArray jDigest,
     jint jDigestOfs, jint jDigestLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE BUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE_PTR bufP = BUF;
    CK_BYTE DIGESTBUF[MAX_DIGEST_LEN];
    CK_ULONG ckDigestLength = 0;
    CK_MECHANISM_PTR ckpMechanism = NULL;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) { return 0; }

    rv = (*ckpFunctions->C_DigestInit)(ckSessionHandle, ckpMechanism);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { goto cleanup; }

    if (jInLen > MAX_STACK_BUFFER_LEN) {
        /* always use single part op, even for large data */
        bufP = (CK_BYTE_PTR) malloc((size_t)jInLen);
        if (bufP == NULL) {
            throwOutOfMemoryError(env, 0);
            goto cleanup;
        }
    }

    (*env)->GetByteArrayRegion(env, jIn, jInOfs, jInLen, (jbyte *)bufP);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    ckDigestLength = min(MAX_DIGEST_LEN, jDigestLen);

    rv = (*ckpFunctions->C_Digest)(ckSessionHandle, bufP, jInLen, DIGESTBUF, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        (*env)->SetByteArrayRegion(env, jDigest, jDigestOfs, ckDigestLength, (jbyte *)DIGESTBUF);
    }
cleanup:
    freeCKMechanismPtr(ckpMechanism);
    if (bufP != BUF) { free(bufP); }

    return ckDigestLength;
}
#endif

#ifdef P11_ENABLE_C_DIGESTUPDATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DigestUpdate
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1DigestUpdate
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jlong directIn, jbyteArray jIn,
     jint jInOfs, jint jInLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE_PTR bufP;
    CK_BYTE BUF[MAX_STACK_BUFFER_LEN];
    jsize bufLen;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directIn != 0) {
        rv = (*ckpFunctions->C_DigestUpdate)(ckSessionHandle, (CK_BYTE_PTR)jlong_to_ptr(directIn), jInLen);
        ckAssertReturnValueOK(env, rv);
        return;
    }

    if (jInLen <= MAX_STACK_BUFFER_LEN) {
        bufLen = MAX_STACK_BUFFER_LEN;
        bufP = BUF;
    } else {
        bufLen = min(MAX_HEAP_BUFFER_LEN, jInLen);
        bufP = (CK_BYTE_PTR) malloc((size_t)bufLen);
        if (bufP == NULL) {
            throwOutOfMemoryError(env, 0);
            return;
        }
    }

    while (jInLen > 0) {
        jsize chunkLen = min(bufLen, jInLen);
        (*env)->GetByteArrayRegion(env, jIn, jInOfs, chunkLen, (jbyte *)bufP);
        if ((*env)->ExceptionCheck(env)) {
            if (bufP != BUF) { free(bufP); }
            return;
        }
        rv = (*ckpFunctions->C_DigestUpdate)(ckSessionHandle, bufP, chunkLen);
        if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) {
            if (bufP != BUF) { free(bufP); }
            return;
        }
        jInOfs += chunkLen;
        jInLen -= chunkLen;
    }

    if (bufP != BUF) {
        free(bufP);
    }
}
#endif

#ifdef P11_ENABLE_C_DIGESTKEY
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DigestKey
 * Signature: (JJ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1DigestKey
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_ULONG ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_DigestKey)(ckSessionHandle, ckKeyHandle);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_DIGESTFINAL
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DigestFinal
 * Signature: (J[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jDigest          CK_BYTE_PTR pDigest
 *                                      CK_ULONG_PTR pulDigestLen
 */
JNIEXPORT jint JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1DigestFinal
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jDigest,
     jint jDigestOfs, jint jDigestLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE BUF[MAX_DIGEST_LEN];
    CK_ULONG ckDigestLength = min(MAX_DIGEST_LEN, jDigestLen);

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    rv = (*ckpFunctions->C_DigestFinal)(ckSessionHandle, BUF, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        (*env)->SetByteArrayRegion(env, jDigest, jDigestOfs, ckDigestLength, (jbyte *)BUF);
    }
    return ckDigestLength;
}
#endif

#ifdef P11_ENABLE_C_SEEDRANDOM
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SeedRandom
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSeed            CK_BYTE_PTR pSeed
 *                                      CK_ULONG ulSeedLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SeedRandom
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jSeed)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpSeed = NULL_PTR;
    CK_ULONG ckSeedLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jByteArrayToCKByteArray(env, jSeed, &ckpSeed, &ckSeedLength);
    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_SeedRandom)(ckSessionHandle, ckpSeed, ckSeedLength);

    free(ckpSeed);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_GENERATERANDOM
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GenerateRandom
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jRandomData      CK_BYTE_PTR pRandomData
 *                                      CK_ULONG ulRandomDataLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1GenerateRandom
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jRandomData)
{
    CK_SESSION_HANDLE ckSessionHandle;
    jbyte *jRandomBuffer;
    jlong jRandomBufferLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    jRandomBufferLength = (*env)->GetArrayLength(env, jRandomData);
    jRandomBuffer = (*env)->GetByteArrayElements(env, jRandomData, NULL);
    if (jRandomBuffer == NULL) { return; }

    rv = (*ckpFunctions->C_GenerateRandom)(ckSessionHandle,
                                         (CK_BYTE_PTR) jRandomBuffer,
                                         jLongToCKULong(jRandomBufferLength));

    /* copy back generated bytes */
    (*env)->ReleaseByteArrayElements(env, jRandomData, jRandomBuffer, 0);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif
