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

#ifdef P11_ENABLE_C_SIGNINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SignInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SignInit
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    TRACE0("DEBUG: C_SignInit\n");

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) { return; }

    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_SignInit)(ckSessionHandle, ckpMechanism, ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK ||
            (ckpMechanism->pParameter == NULL)) {
        freeCKMechanismPtr(ckpMechanism);
    } else {
        (*env)->SetLongField(env, jMechanism, mech_pHandleID, ptr_to_jlong(ckpMechanism));
        TRACE1("DEBUG C_SignInit: stored pMech = 0x%lX\n", ptr_to_jlong(ckpMechanism));
    }
    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_SIGN
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Sign
 * Signature: (J[BI)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1Sign
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jData)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR;
    CK_ULONG ckDataLength;
    CK_BYTE_PTR bufP;
    CK_ULONG ckSignatureLength;
    CK_BYTE BUF[MAX_STACK_BUFFER_LEN];
    jbyteArray jSignature = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    TRACE0("DEBUG: C_Sign\n");

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }

    TRACE1("DEBUG C_Sign: data length = %lu\n", ckDataLength);

    // unknown signature length
    bufP = BUF;
    ckSignatureLength = MAX_STACK_BUFFER_LEN;

    rv = (*ckpFunctions->C_Sign)(ckSessionHandle, ckpData, ckDataLength,
        bufP, &ckSignatureLength);

    TRACE1("DEBUG C_Sign: ret rv=0x%lX\n", rv);

    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSignature = ckByteArrayToJByteArray(env, bufP, ckSignatureLength);
        TRACE1("DEBUG C_Sign: signature length = %lu\n", ckSignatureLength);
    }

    free(ckpData);
    if (bufP != BUF) { free(bufP); }

    TRACE0("FINISHED\n");
    return jSignature;
}
#endif

#ifdef P11_ENABLE_C_SIGNUPDATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SignUpdate
 * Signature: (J[BII)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SignUpdate
  (JNIEnv *env, jobject obj, jlong jSessionHandle, jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen)
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
        rv = (*ckpFunctions->C_SignUpdate)(ckSessionHandle, (CK_BYTE_PTR) jlong_to_ptr(directIn), jInLen);
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
            goto cleanup;
        }
        rv = (*ckpFunctions->C_SignUpdate)(ckSessionHandle, bufP, chunkLen);
        if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) {
            goto cleanup;
        }
        jInOfs += chunkLen;
        jInLen -= chunkLen;
    }

cleanup:
    if (bufP != BUF) { free(bufP); }

    return;
}
#endif

#ifdef P11_ENABLE_C_SIGNFINAL
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SignFinal
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SignFinal
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jint jExpectedLength)
{
    CK_SESSION_HANDLE ckSessionHandle;
    jbyteArray jSignature = NULL;
    CK_RV rv;
    CK_BYTE BUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE_PTR bufP = BUF;
    CK_ULONG ckSignatureLength = MAX_STACK_BUFFER_LEN;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if ((jExpectedLength > 0) && ((CK_ULONG)jExpectedLength < ckSignatureLength)) {
        ckSignatureLength = jExpectedLength;
    }

    rv = (*ckpFunctions->C_SignFinal)(ckSessionHandle, bufP, &ckSignatureLength);
    if (rv == CKR_BUFFER_TOO_SMALL) {
        bufP = (CK_BYTE_PTR) malloc(ckSignatureLength);
        if (bufP == NULL) {
            throwOutOfMemoryError(env, 0);
            return NULL;
        }
        rv = (*ckpFunctions->C_SignFinal)(ckSessionHandle, bufP, &ckSignatureLength);
    }
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSignature = ckByteArrayToJByteArray(env, bufP, ckSignatureLength);
    }

    if (bufP != BUF) { free(bufP); }

    return jSignature;
}
#endif

#ifdef P11_ENABLE_C_SIGNRECOVERINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SignRecoverInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SignRecoverInit
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    TRACE0("DEBUG: C_SignRecoverInit\n");

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) { return; }

    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_SignRecoverInit)(ckSessionHandle, ckpMechanism, ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK ||
            (ckpMechanism->pParameter == NULL)) {
        freeCKMechanismPtr(ckpMechanism);
    } else {
        (*env)->SetLongField(env, jMechanism, mech_pHandleID, ptr_to_jlong(ckpMechanism));
        TRACE1("DEBUG C_SignRecoverInit, stored pMech = 0x%lX\n", ptr_to_jlong(ckpMechanism));
    }
    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_SIGNRECOVER
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SignRecover
 * Signature: (J[BII[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jint JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SignRecover
  (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jIn, jint jInOfs, jint jInLen, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE INBUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE OUTBUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP = OUTBUF;
    CK_ULONG ckSignatureLength = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (jInLen <= MAX_STACK_BUFFER_LEN) {
        inBufP = INBUF;
        ckSignatureLength = MAX_STACK_BUFFER_LEN;
    } else {
        inBufP = (CK_BYTE_PTR) malloc((size_t)jInLen);
        if (inBufP == NULL) {
            throwOutOfMemoryError(env, 0);
            return 0;
        }
        ckSignatureLength = jInLen;
    }

    (*env)->GetByteArrayRegion(env, jIn, jInOfs, jInLen, (jbyte *)inBufP);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    rv = (*ckpFunctions->C_SignRecover)(ckSessionHandle, inBufP, jInLen, outBufP, &ckSignatureLength);
    /* re-alloc larger buffer if it fits into our Java buffer */
    if ((rv == CKR_BUFFER_TOO_SMALL) && (ckSignatureLength <= jIntToCKULong(jOutLen))) {
        outBufP = (CK_BYTE_PTR) malloc(ckSignatureLength);
        if (outBufP == NULL) {
            throwOutOfMemoryError(env, 0);
            goto cleanup;
        }
        rv = (*ckpFunctions->C_SignRecover)(ckSessionHandle, inBufP, jInLen, outBufP, &ckSignatureLength);
    }
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        (*env)->SetByteArrayRegion(env, jOut, jOutOfs, ckSignatureLength, (jbyte *)outBufP);
    }
cleanup:
    if (inBufP != INBUF) { free(inBufP); }
    if (outBufP != OUTBUF) { free(outBufP); }

    return ckSignatureLength;
}
#endif

#ifdef P11_ENABLE_C_VERIFYINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_VerifyInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1VerifyInit
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    TRACE0("DEBUG: C_VerifyInit\n");

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_VerifyInit)(ckSessionHandle, ckpMechanism, ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK ||
            (ckpMechanism->pParameter == NULL)) {
        freeCKMechanismPtr(ckpMechanism);
    } else {
        (*env)->SetLongField(env, jMechanism, mech_pHandleID, ptr_to_jlong(ckpMechanism));
        TRACE1("DEBUG C_VerifyInit: stored pMech = 0x%lX\n", ptr_to_jlong(ckpMechanism));
    }
    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_VERIFY
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Verify
 * Signature: (J[B[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1Verify
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jData, jbyteArray jSignature)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR;
    CK_BYTE_PTR ckpSignature = NULL_PTR;
    CK_ULONG ckDataLength;
    CK_ULONG ckSignatureLength;
    CK_RV rv = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength);
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    jByteArrayToCKByteArray(env, jSignature, &ckpSignature, &ckSignatureLength);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    /* verify the signature */
    rv = (*ckpFunctions->C_Verify)(ckSessionHandle, ckpData, ckDataLength, ckpSignature, ckSignatureLength);

cleanup:
    free(ckpData);
    free(ckpSignature);

    ckAssertReturnValueOK(env, rv);
}
#endif

#ifdef P11_ENABLE_C_VERIFYUPDATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_VerifyUpdate
 * Signature: (J[BII)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1VerifyUpdate
  (JNIEnv *env, jobject obj, jlong jSessionHandle, jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen)
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
        rv = (*ckpFunctions->C_VerifyUpdate)(ckSessionHandle, (CK_BYTE_PTR)jlong_to_ptr(directIn), jInLen);
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
            goto cleanup;
        }
    }

    while (jInLen > 0) {
        jsize chunkLen = min(bufLen, jInLen);
        (*env)->GetByteArrayRegion(env, jIn, jInOfs, chunkLen, (jbyte *)bufP);
        if ((*env)->ExceptionCheck(env)) {
            goto cleanup;
        }

        rv = (*ckpFunctions->C_VerifyUpdate)(ckSessionHandle, bufP, chunkLen);
        if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) {
            goto cleanup;
        }
        jInOfs += chunkLen;
        jInLen -= chunkLen;
    }

cleanup:
    if (bufP != BUF) { free(bufP); }
}
#endif

#ifdef P11_ENABLE_C_VERIFYFINAL
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_VerifyFinal
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG ulSignatureLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1VerifyFinal
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jSignature)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpSignature = NULL_PTR;
    CK_ULONG ckSignatureLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jByteArrayToCKByteArray(env, jSignature, &ckpSignature, &ckSignatureLength);
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    /* verify the signature */
    rv = (*ckpFunctions->C_VerifyFinal)(ckSessionHandle, ckpSignature, ckSignatureLength);

    free(ckpSignature);

    ckAssertReturnValueOK(env, rv);
}
#endif

#ifdef P11_ENABLE_C_VERIFYRECOVERINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_VerifyRecoverInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1VerifyRecoverInit
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    TRACE0("DEBUG: C_VerifyRecoverInit\n");

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    if ((*env)->ExceptionCheck(env)) { return; }

    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_VerifyRecoverInit)(ckSessionHandle, ckpMechanism, ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK ||
            (ckpMechanism->pParameter == NULL)) {
        freeCKMechanismPtr(ckpMechanism);
    } else {
        (*env)->SetLongField(env, jMechanism, mech_pHandleID, ptr_to_jlong(ckpMechanism));
        TRACE1("DEBUG C_VerifyRecoverInit: stored pMech = 0x%lX\n", ptr_to_jlong(ckpMechanism));
    }
    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_VERIFYRECOVER
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_VerifyRecover
 * Signature: (J[BII[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG ulSignatureLen
 * @return  jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG_PTR pulDataLen
 */
JNIEXPORT jint JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1VerifyRecover
  (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jIn, jint jInOfs, jint jInLen, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE INBUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE OUTBUF[MAX_STACK_BUFFER_LEN];
    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP = OUTBUF;
    CK_ULONG ckDataLength = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (jInLen <= MAX_STACK_BUFFER_LEN) {
        inBufP = INBUF;
        ckDataLength = MAX_STACK_BUFFER_LEN;
    } else {
        inBufP = (CK_BYTE_PTR) malloc((size_t)jInLen);
        if (inBufP == NULL) {
            throwOutOfMemoryError(env, 0);
            return 0;
        }
        ckDataLength = jInLen;
    }

    (*env)->GetByteArrayRegion(env, jIn, jInOfs, jInLen, (jbyte *)inBufP);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    rv = (*ckpFunctions->C_VerifyRecover)(ckSessionHandle, inBufP, jInLen, outBufP, &ckDataLength);

    /* re-alloc larger buffer if it fits into our Java buffer */
    if ((rv == CKR_BUFFER_TOO_SMALL) && (ckDataLength <= jIntToCKULong(jOutLen))) {
        outBufP = (CK_BYTE_PTR) malloc(ckDataLength);
        if (outBufP == NULL) {
            throwOutOfMemoryError(env, 0);
            goto cleanup;
        }
        rv = (*ckpFunctions->C_VerifyRecover)(ckSessionHandle, inBufP, jInLen, outBufP, &ckDataLength);
    }
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        (*env)->SetByteArrayRegion(env, jOut, jOutOfs, ckDataLength, (jbyte *)outBufP);
    }

cleanup:
    if (inBufP != INBUF) { free(inBufP); }
    if (outBufP != OUTBUF) { free(outBufP); }

    return ckDataLength;
}
#endif
