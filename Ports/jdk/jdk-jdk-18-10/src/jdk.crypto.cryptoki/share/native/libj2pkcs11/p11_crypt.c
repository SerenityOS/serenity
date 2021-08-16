/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * ===========================================================================
 */

#include "pkcs11wrapper.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "sun_security_pkcs11_wrapper_PKCS11.h"

#ifdef P11_ENABLE_C_ENCRYPTINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_EncryptInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1EncryptInit
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_MECHANISM_PTR ckpTemp;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckKeyHandle = jLongToCKULong(jKeyHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    TRACE1("DEBUG C_EncryptInit: created pMech = %p\n",
            ckpMechanism);

    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_EncryptInit)(ckSessionHandle, ckpMechanism,
                                        ckKeyHandle);

    if (ckpMechanism->mechanism == CKM_AES_GCM) {
        if (rv == CKR_ARGUMENTS_BAD || rv == CKR_MECHANISM_PARAM_INVALID) {
            // retry with CKM_GCM_PARAMS structure in pkcs11t.h
            TRACE0("DEBUG C_EncryptInit: retry with CK_GCM_PARAMS\n");
            ckpTemp = updateGCMParams(env, ckpMechanism);
            if (ckpTemp != NULL) { // only re-call if conversion succeeds
                ckpMechanism = ckpTemp;
                rv = (*ckpFunctions->C_EncryptInit)(ckSessionHandle, ckpMechanism,
                        ckKeyHandle);
            }
        }
    }

    TRACE1("DEBUG C_EncryptInit: freed pMech = %p\n", ckpMechanism);
    freeCKMechanismPtr(ckpMechanism);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }

    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_ENCRYPT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Encrypt
 * Signature: (JJ[BIIJ[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong directIn              CK_BYTE_PTR pData
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @param   jlong directOut             CK_BYTE_PTR pEncryptedData
 * @return  jint encryptedDataLen       CK_BYTE_PTR pEncryptedData
 *                                      CK_ULONG_PTR pulEncryptedDataLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1Encrypt
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;

    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckEncryptedLen = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directIn != 0) {
      inBufP = (CK_BYTE_PTR) jlong_to_ptr(directIn);
    } else {
      inBufP = (*env)->GetPrimitiveArrayCritical(env, jIn, NULL);
      if (inBufP == NULL) { return 0; }
    }

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) {
          goto cleanup;
      }
    }

    ckEncryptedLen = jOutLen;

    rv = (*ckpFunctions->C_Encrypt)(ckSessionHandle,
                                    (CK_BYTE_PTR)(inBufP + jInOfs), jInLen,
                                    (CK_BYTE_PTR)(outBufP + jOutOfs),
                                    &ckEncryptedLen);

    ckAssertReturnValueOK(env, rv);

cleanup:
    if (directIn == 0 && inBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jIn, inBufP, JNI_ABORT);
    }
    if (directOut == 0 && outBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);
    }
    return ckEncryptedLen;
}
#endif

#ifdef P11_ENABLE_C_ENCRYPTUPDATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_EncryptUpdate
 * Signature: (J[BII[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 * @return  jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG_PTR pulEncryptedPartLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1EncryptUpdate
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;

    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckEncryptedPartLen = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directIn != 0) {
      inBufP = (CK_BYTE_PTR) jlong_to_ptr(directIn);
    } else {
      inBufP = (*env)->GetPrimitiveArrayCritical(env, jIn, NULL);
      if (inBufP == NULL) { return 0; }
    }

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) {
          goto cleanup;
      }
    }

    ckEncryptedPartLen = jOutLen;

    rv = (*ckpFunctions->C_EncryptUpdate)(ckSessionHandle,
                                          (CK_BYTE_PTR)(inBufP + jInOfs), jInLen,
                                          (CK_BYTE_PTR)(outBufP + jOutOfs),
                                          &ckEncryptedPartLen);

    ckAssertReturnValueOK(env, rv);

cleanup:
    if (directIn == 0 && inBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jIn, inBufP, JNI_ABORT);
    }
    if (directOut == 0 && outBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);
    }
    return ckEncryptedPartLen;
}
#endif

#ifdef P11_ENABLE_C_ENCRYPTFINAL
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_EncryptFinal
 * Signature: (J[BII)I
 * Parametermapping:                        *PKCS11*
 * @param   jlong jSessionHandle            CK_SESSION_HANDLE hSession
 * @return  jbyteArray jLastEncryptedPart   CK_BYTE_PTR pLastEncryptedDataPart
 *                                          CK_ULONG_PTR pulLastEncryptedDataPartLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1EncryptFinal
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckLastEncryptedPartLen;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) { return 0; }
    }

    ckLastEncryptedPartLen = jOutLen;

    rv = (*ckpFunctions->C_EncryptFinal)(ckSessionHandle,
                                         (CK_BYTE_PTR)(outBufP + jOutOfs),
                                         &ckLastEncryptedPartLen);

    if (directOut == 0) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);
    }

    ckAssertReturnValueOK(env, rv);

    return ckLastEncryptedPartLen;
}
#endif

#ifdef P11_ENABLE_C_DECRYPTINIT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DecryptInit
 * Signature: (JLsun/security/pkcs11/wrapper/CK_MECHANISM;J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1DecryptInit
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jobject jMechanism, jlong jKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM_PTR ckpMechanism = NULL;
    CK_MECHANISM_PTR ckpTemp;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckKeyHandle = jLongToCKULong(jKeyHandle);
    ckpMechanism = jMechanismToCKMechanismPtr(env, jMechanism);
    TRACE1("DEBUG C_DecryptInit: created pMech = %p\n",
            ckpMechanism);

    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_DecryptInit)(ckSessionHandle, ckpMechanism,
                                        ckKeyHandle);

    if (ckpMechanism->mechanism == CKM_AES_GCM) {
        if (rv == CKR_ARGUMENTS_BAD || rv == CKR_MECHANISM_PARAM_INVALID) {
            // retry with CKM_GCM_PARAMS structure in pkcs11t.h
            TRACE0("DEBUG C_DecryptInit: retry with CK_GCM_PARAMS\n");
            ckpTemp = updateGCMParams(env, ckpMechanism);
            if (ckpTemp != NULL) { // only re-call if conversion succeeds
                ckpMechanism = ckpTemp;
                rv = (*ckpFunctions->C_DecryptInit)(ckSessionHandle, ckpMechanism,
                        ckKeyHandle);
            }
        }
    }

    TRACE1("DEBUG C_DecryptInit: freed pMech = %p\n", ckpMechanism);
    freeCKMechanismPtr(ckpMechanism);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }

    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_DECRYPT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Decrypt
 * Signature: (JJ[BIIJ[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedData   CK_BYTE_PTR pEncryptedData
 *                                      CK_ULONG ulEncryptedDataLen
 * @return  jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG_PTR pulDataLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1Decrypt
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;

    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckOutLen = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directIn != 0) {
      inBufP = (CK_BYTE_PTR) jlong_to_ptr(directIn);
    } else {
      inBufP = (*env)->GetPrimitiveArrayCritical(env, jIn, NULL);
      if (inBufP == NULL) { return 0; }
    }

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) {
          goto cleanup;
      }
    }
    ckOutLen = jOutLen;

    rv = (*ckpFunctions->C_Decrypt)(ckSessionHandle,
                                    (CK_BYTE_PTR)(inBufP + jInOfs), jInLen,
                                    (CK_BYTE_PTR)(outBufP + jOutOfs),
                                    &ckOutLen);

    ckAssertReturnValueOK(env, rv);

cleanup:
    if (directIn == 0 && inBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jIn, inBufP, JNI_ABORT);
    }
    if (directOut == 0 && outBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);
    }
    return ckOutLen;
}
#endif

#ifdef P11_ENABLE_C_DECRYPTUPDATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DecryptUpdate
 * Signature: (J[BII[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG ulEncryptedPartLen
 * @return  jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG_PTR pulPartLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1DecryptUpdate
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directIn, jbyteArray jIn, jint jInOfs, jint jInLen,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;

    CK_BYTE_PTR inBufP;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckDecryptedPartLen = 0;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directIn != 0) {
      inBufP = (CK_BYTE_PTR) jlong_to_ptr(directIn);
    } else {
      inBufP = (*env)->GetPrimitiveArrayCritical(env, jIn, NULL);
      if (inBufP == NULL) { return 0; }
    }

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) {
          goto cleanup;
      }
    }

    ckDecryptedPartLen = jOutLen;
    rv = (*ckpFunctions->C_DecryptUpdate)(ckSessionHandle,
                                          (CK_BYTE_PTR)(inBufP + jInOfs), jInLen,
                                          (CK_BYTE_PTR)(outBufP + jOutOfs),
                                          &ckDecryptedPartLen);
    ckAssertReturnValueOK(env, rv);

cleanup:
    if (directIn == 0 && inBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jIn, inBufP, JNI_ABORT);
    }
    if (directOut == 0 && outBufP != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);
    }
    return ckDecryptedPartLen;
}

#endif

#ifdef P11_ENABLE_C_DECRYPTFINAL
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_DecryptFinal
 * Signature: (J[BII)I
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jLastPart        CK_BYTE_PTR pLastPart
 *                                      CK_ULONG_PTR pulLastPartLen
 */
JNIEXPORT jint JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1DecryptFinal
(JNIEnv *env, jobject obj, jlong jSessionHandle,
 jlong directOut, jbyteArray jOut, jint jOutOfs, jint jOutLen)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    CK_BYTE_PTR outBufP;
    CK_ULONG ckLastPartLen;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    if (directOut != 0) {
      outBufP = (CK_BYTE_PTR) jlong_to_ptr(directOut);
    } else {
      outBufP = (*env)->GetPrimitiveArrayCritical(env, jOut, NULL);
      if (outBufP == NULL) { return 0; }
    }

    ckLastPartLen = jOutLen;

    rv = (*ckpFunctions->C_DecryptFinal)(ckSessionHandle,
                                         (CK_BYTE_PTR)(outBufP + jOutOfs),
                                         &ckLastPartLen);

    if (directOut == 0) {
        (*env)->ReleasePrimitiveArrayCritical(env, jOut, outBufP, 0);

    }

    ckAssertReturnValueOK(env, rv);

    return ckLastPartLen;
}
#endif
