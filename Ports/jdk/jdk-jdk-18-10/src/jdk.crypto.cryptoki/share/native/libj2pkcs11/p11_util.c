/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* declare file private functions */

ModuleData * getModuleEntry(JNIEnv *env, jobject pkcs11Implementation);
int isModulePresent(JNIEnv *env, jobject pkcs11Implementation);
void removeAllModuleEntries(JNIEnv *env);


/* ************************************************************************** */
/* Functions for keeping track of currently active and loaded modules         */
/* ************************************************************************** */


/*
 * Create a new object for locking.
 */
jobject createLockObject(JNIEnv *env) {
    jclass jObjectClass;
    jobject jLockObject;
    jmethodID jConstructor;

    jObjectClass = (*env)->FindClass(env, "java/lang/Object");
    if (jObjectClass == NULL) { return NULL; }
    jConstructor = (*env)->GetMethodID(env, jObjectClass, "<init>", "()V");
    if (jConstructor == NULL) { return NULL; }
    jLockObject = (*env)->NewObject(env, jObjectClass, jConstructor);
    if (jLockObject == NULL) { return NULL; }
    jLockObject = (*env)->NewGlobalRef(env, jLockObject);

    return jLockObject ;
}

/*
 * Create a new object for locking.
 */
void destroyLockObject(JNIEnv *env, jobject jLockObject) {
    if (jLockObject != NULL) {
        (*env)->DeleteGlobalRef(env, jLockObject);
    }
}

/*
 * Add the given pkcs11Implementation object to the list of present modules.
 * Attach the given data to the entry. If the given pkcs11Implementation is
 * already in the list, just override its old module data with the new one.
 * None of the arguments can be NULL. If one of the arguments is NULL, this
 * function does nothing.
 */
void putModuleEntry(JNIEnv *env, jobject pkcs11Implementation, ModuleData *moduleData) {
    if (pkcs11Implementation == NULL_PTR) {
        return ;
    }
    if (moduleData == NULL) {
        return ;
    }
    (*env)->SetLongField(env, pkcs11Implementation, pNativeDataID, ptr_to_jlong(moduleData));
}


/*
 * Get the module data of the entry for the given pkcs11Implementation. Returns
 * NULL, if the pkcs11Implementation is not in the list.
 */
ModuleData * getModuleEntry(JNIEnv *env, jobject pkcs11Implementation) {
    jlong jData;
    if (pkcs11Implementation == NULL) {
        return NULL;
    }
    jData = (*env)->GetLongField(env, pkcs11Implementation, pNativeDataID);
    return (ModuleData*)jlong_to_ptr(jData);
}

CK_FUNCTION_LIST_PTR getFunctionList(JNIEnv *env, jobject pkcs11Implementation) {
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    moduleData = getModuleEntry(env, pkcs11Implementation);
    if (moduleData == NULL) {
        throwDisconnectedRuntimeException(env);
        return NULL;
    }
    ckpFunctions = moduleData->ckFunctionListPtr;
    return ckpFunctions;
}


/*
 * Returns 1, if the given pkcs11Implementation is in the list.
 * 0, otherwise.
 */
int isModulePresent(JNIEnv *env, jobject pkcs11Implementation) {
    int present;

    ModuleData *moduleData = getModuleEntry(env, pkcs11Implementation);

    present = (moduleData != NULL) ? 1 : 0;

    return present ;
}


/*
 * Removes the entry for the given pkcs11Implementation from the list. Returns
 * the module's data, after the node was removed. If this function returns NULL
 * the pkcs11Implementation was not in the list.
 */
ModuleData * removeModuleEntry(JNIEnv *env, jobject pkcs11Implementation) {
    ModuleData *moduleData = getModuleEntry(env, pkcs11Implementation);
    if (moduleData == NULL) {
        return NULL;
    }
    (*env)->SetLongField(env, pkcs11Implementation, pNativeDataID, 0);
    return moduleData;
}

/*
 * Removes all present entries from the list of modules and frees all
 * associated resources. This function is used for clean-up.
 */
void removeAllModuleEntries(JNIEnv *env) {
    /* XXX empty */
}

/* ************************************************************************** */
/* Below there follow the helper functions to support conversions between     */
/* Java and Cryptoki types                                                    */
/* ************************************************************************** */

/*
 * function to convert a PKCS#11 return value into a PKCS#11Exception
 *
 * This function generates a PKCS#11Exception with the returnValue as the
 * errorcode. If the returnValue is not CKR_OK. The function returns 0, if the
 * returnValue is CKR_OK. Otherwise, it returns the returnValue as a jLong.
 *
 * @param env - used to call JNI functions and to get the Exception class
 * @param returnValue - of the PKCS#11 function
 */
jlong ckAssertReturnValueOK(JNIEnv *env, CK_RV returnValue) {
    return ckAssertReturnValueOK2(env, returnValue, NULL);
}

/*
 * function to convert a PKCS#11 return value and additional message into a
 * PKCS#11Exception
 *
 * This function generates a PKCS#11Exception with the returnValue as the
 * errorcode. If the returnValue is not CKR_OK. The function returns 0, if the
 * returnValue is CKR_OK. Otherwise, it returns the returnValue as a jLong.
 *
 * @param env - used to call JNI functions and to get the Exception class
 * @param returnValue - of the PKCS#11 function
 * @param msg - additional message for the generated PKCS11Exception
 */
jlong ckAssertReturnValueOK2(JNIEnv *env, CK_RV returnValue, const char* msg) {
    jclass jPKCS11ExceptionClass;
    jmethodID jConstructor;
    jthrowable jPKCS11Exception;
    jlong jErrorCode = 0L;
    jstring jMsg = NULL;

    if (returnValue != CKR_OK) {
        jErrorCode = ckULongToJLong(returnValue);
        jPKCS11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (jPKCS11ExceptionClass != NULL) {
            jConstructor = (*env)->GetMethodID(env, jPKCS11ExceptionClass,
                    "<init>", "(JLjava/lang/String;)V");
            if (jConstructor != NULL) {
                if (msg != NULL) {
                    jMsg = (*env)->NewStringUTF(env, msg);
                }
                jPKCS11Exception = (jthrowable) (*env)->NewObject(env,
                        jPKCS11ExceptionClass, jConstructor, jErrorCode, jMsg);
                if (jPKCS11Exception != NULL) {
                    (*env)->Throw(env, jPKCS11Exception);
                }
            }
        }
        (*env)->DeleteLocalRef(env, jPKCS11ExceptionClass);
    }
    return jErrorCode;
}


/*
 * Throws a Java Exception by name
 */
void throwByName(JNIEnv *env, const char *name, const char *msg)
{
    jclass cls = (*env)->FindClass(env, name);

    if (cls != 0) /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
}

/*
 * Throws java.lang.OutOfMemoryError
 */
void throwOutOfMemoryError(JNIEnv *env, const char *msg)
{
    throwByName(env, "java/lang/OutOfMemoryError", msg);
}

/*
 * Throws java.lang.NullPointerException
 */
void throwNullPointerException(JNIEnv *env, const char *msg)
{
    throwByName(env, "java/lang/NullPointerException", msg);
}

/*
 * Throws java.io.IOException
 */
void throwIOException(JNIEnv *env, const char *msg)
{
    throwByName(env, "java/io/IOException", msg);
}

/*
 * This function simply throws a PKCS#11RuntimeException with the given
 * string as its message.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 * @param jmessage The message string of the Exception object.
 */
void throwPKCS11RuntimeException(JNIEnv *env, const char *message)
{
    throwByName(env, CLASS_PKCS11RUNTIMEEXCEPTION, message);
}

/*
 * This function simply throws a PKCS#11RuntimeException. The message says that
 * the object is not connected to the module.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 */
void throwDisconnectedRuntimeException(JNIEnv *env)
{
    throwPKCS11RuntimeException(env, "This object is not connected to a module.");
}

/* This function frees the specified CK_ATTRIBUTE array.
 *
 * @param attrPtr pointer to the to-be-freed CK_ATTRIBUTE array.
 * @param len the length of the array
 */
void freeCKAttributeArray(CK_ATTRIBUTE_PTR attrPtr, int len) {
    if (attrPtr != NULL) {
        int i;
        for (i=0; i<len; i++) {
            if (attrPtr[i].pValue != NULL_PTR) {
                free(attrPtr[i].pValue);
            }
        }
        free(attrPtr);
    }
}

/* This function frees the specified CK_MECHANISM_PTR pointer and its
 * pParameter including mechanism-specific memory allocations.
 *
 * @param mechPtr pointer to the to-be-freed CK_MECHANISM structure.
 */
void freeCKMechanismPtr(CK_MECHANISM_PTR mechPtr) {
     void *tmp;
     CK_SSL3_MASTER_KEY_DERIVE_PARAMS *sslMkdTmp;
     CK_SSL3_KEY_MAT_PARAMS* sslKmTmp;
     CK_TLS12_MASTER_KEY_DERIVE_PARAMS *tlsMkdTmp;
     CK_TLS12_KEY_MAT_PARAMS* tlsKmTmp;

     if (mechPtr != NULL) {
         TRACE2("DEBUG freeCKMechanismPtr: free pMech %p (mech 0x%lX)\n",
                 mechPtr,  mechPtr->mechanism);
         if (mechPtr->pParameter != NULL) {
             tmp = mechPtr->pParameter;
             switch (mechPtr->mechanism) {
                 case CKM_AES_GCM:
                     if (mechPtr->ulParameterLen == sizeof(CK_GCM_PARAMS_NO_IVBITS)) {
                         TRACE0("[ GCM_PARAMS w/o ulIvBits ]\n");
                         free(((CK_GCM_PARAMS_NO_IVBITS*)tmp)->pIv);
                         free(((CK_GCM_PARAMS_NO_IVBITS*)tmp)->pAAD);
                     } else if (mechPtr->ulParameterLen == sizeof(CK_GCM_PARAMS)) {
                         TRACE0("[ GCM_PARAMS ]\n");
                         free(((CK_GCM_PARAMS*)tmp)->pIv);
                         free(((CK_GCM_PARAMS*)tmp)->pAAD);
                     }
                     break;
                 case CKM_AES_CCM:
                     TRACE0("[ CK_CCM_PARAMS ]\n");
                     free(((CK_CCM_PARAMS*)tmp)->pNonce);
                     free(((CK_CCM_PARAMS*)tmp)->pAAD);
                     break;
                 case CKM_CHACHA20_POLY1305:
                     TRACE0("[ CK_SALSA20_CHACHA20_POLY1305_PARAMS ]\n");
                     free(((CK_SALSA20_CHACHA20_POLY1305_PARAMS*)tmp)->pNonce);
                     free(((CK_SALSA20_CHACHA20_POLY1305_PARAMS*)tmp)->pAAD);
                     break;
                 case CKM_TLS_PRF:
                 case CKM_NSS_TLS_PRF_GENERAL:
                     TRACE0("[ CK_TLS_PRF_PARAMS ]\n");
                     free(((CK_TLS_PRF_PARAMS*)tmp)->pSeed);
                     free(((CK_TLS_PRF_PARAMS*)tmp)->pLabel);
                     free(((CK_TLS_PRF_PARAMS*)tmp)->pulOutputLen);
                     free(((CK_TLS_PRF_PARAMS*)tmp)->pOutput);
                     break;
                 case CKM_SSL3_MASTER_KEY_DERIVE:
                 case CKM_TLS_MASTER_KEY_DERIVE:
                 case CKM_SSL3_MASTER_KEY_DERIVE_DH:
                 case CKM_TLS_MASTER_KEY_DERIVE_DH:
                     sslMkdTmp = tmp;
                     TRACE0("[ CK_SSL3_MASTER_KEY_DERIVE_PARAMS ]\n");
                     free(sslMkdTmp->RandomInfo.pClientRandom);
                     free(sslMkdTmp->RandomInfo.pServerRandom);
                     free(sslMkdTmp->pVersion);
                     break;
                 case CKM_SSL3_KEY_AND_MAC_DERIVE:
                 case CKM_TLS_KEY_AND_MAC_DERIVE:
                     sslKmTmp = tmp;
                     TRACE0("[ CK_SSL3_KEY_MAT_PARAMS ]\n");
                     free(sslKmTmp->RandomInfo.pClientRandom);
                     free(sslKmTmp->RandomInfo.pServerRandom);
                     if (sslKmTmp->pReturnedKeyMaterial != NULL) {
                         free(sslKmTmp->pReturnedKeyMaterial->pIVClient);
                         free(sslKmTmp->pReturnedKeyMaterial->pIVServer);
                         free(sslKmTmp->pReturnedKeyMaterial);
                     }
                     break;
                 case CKM_TLS12_MASTER_KEY_DERIVE:
                 case CKM_TLS12_MASTER_KEY_DERIVE_DH:
                     tlsMkdTmp = tmp;
                     TRACE0("[ CK_TLS12_MASTER_KEY_DERIVE_PARAMS ]\n");
                     free(tlsMkdTmp->RandomInfo.pClientRandom);
                     free(tlsMkdTmp->RandomInfo.pServerRandom);
                     free(tlsMkdTmp->pVersion);
                     break;
                 case CKM_TLS12_KEY_AND_MAC_DERIVE:
                     tlsKmTmp = tmp;
                     TRACE0("[ CK_TLS12_KEY_MAT_PARAMS ]\n");
                     free(tlsKmTmp->RandomInfo.pClientRandom);
                     free(tlsKmTmp->RandomInfo.pServerRandom);
                     if (tlsKmTmp->pReturnedKeyMaterial != NULL) {
                         free(tlsKmTmp->pReturnedKeyMaterial->pIVClient);
                         free(tlsKmTmp->pReturnedKeyMaterial->pIVServer);
                         free(tlsKmTmp->pReturnedKeyMaterial);
                     }
                     break;
                 case CKM_ECDH1_DERIVE:
                 case CKM_ECDH1_COFACTOR_DERIVE:
                     TRACE0("[ CK_ECDH1_DERIVE_PARAMS ]\n");
                     free(((CK_ECDH1_DERIVE_PARAMS *)tmp)->pSharedData);
                     free(((CK_ECDH1_DERIVE_PARAMS *)tmp)->pPublicData);
                     break;
                 case CKM_TLS_MAC:
                 case CKM_AES_CTR:
                 case CKM_RSA_PKCS_PSS:
                 case CKM_CAMELLIA_CTR:
                     // params do not contain pointers
                     break;
                 default:
                     // currently unsupported mechs by SunPKCS11 provider
                     // CKM_RSA_PKCS_OAEP, CKM_ECMQV_DERIVE,
                     // CKM_X9_42_*, CKM_KEA_DERIVE, CKM_RC2_*, CKM_RC5_*,
                     // CKM_SKIPJACK_*, CKM_KEY_WRAP_SET_OAEP, CKM_PKCS5_PBKD2,
                     // PBE mechs, WTLS mechs, CMS mechs,
                     // CKM_EXTRACT_KEY_FROM_KEY, CKM_OTP, CKM_KIP,
                     // CKM_DSA_PARAMETER_GEN?, CKM_GOSTR3410_*
                     // CK_any_CBC_ENCRYPT_DATA?
                     TRACE0("ERROR: UNSUPPORTED CK_MECHANISM\n");
                     break;
             }
             TRACE1("\t=> freed param %p\n", tmp);
             free(tmp);
         } else {
             TRACE0("\t=> param NULL\n");
         }
         free(mechPtr);
         TRACE0("FINISHED\n");
     }
}

/* This function replaces the CK_GCM_PARAMS_NO_IVBITS structure associated
 * with the specified CK_MECHANISM structure with CK_GCM_PARAMS
 * structure.
 *
 * @param mechPtr pointer to the CK_MECHANISM structure containing
 * the to-be-converted CK_GCM_PARAMS_NO_IVBITS structure.
 * @return pointer to the CK_MECHANISM structure containing the
 * converted CK_GCM_PARAMS structure or NULL if no conversion took place.
 */
CK_MECHANISM_PTR updateGCMParams(JNIEnv *env, CK_MECHANISM_PTR mechPtr) {
    CK_GCM_PARAMS* pGcmParams2 = NULL;
    CK_GCM_PARAMS_NO_IVBITS* pParams = NULL;
    if ((mechPtr->mechanism == CKM_AES_GCM) &&
            (mechPtr->pParameter != NULL_PTR) &&
            (mechPtr->ulParameterLen == sizeof(CK_GCM_PARAMS_NO_IVBITS))) {
        pGcmParams2 = calloc(1, sizeof(CK_GCM_PARAMS));
        if (pGcmParams2 == NULL) {
            throwOutOfMemoryError(env, 0);
            return NULL;
        }
        pParams = (CK_GCM_PARAMS_NO_IVBITS*) mechPtr->pParameter;
        pGcmParams2->pIv = pParams->pIv;
        pGcmParams2->ulIvLen = pParams->ulIvLen;
        pGcmParams2->ulIvBits = (pGcmParams2->ulIvLen << 3);
        pGcmParams2->pAAD = pParams->pAAD;
        pGcmParams2->ulAADLen = pParams->ulAADLen;
        pGcmParams2->ulTagBits = pParams->ulTagBits;
        TRACE1("DEBUG updateGCMParams: pMech %p\n", mechPtr);
        TRACE2("\t=> GCM param w/o ulIvBits %p => GCM param %p\n", pParams,
                pGcmParams2);
        free(pParams);
        mechPtr->pParameter = pGcmParams2;
        mechPtr->ulParameterLen = sizeof(CK_GCM_PARAMS);
        return mechPtr;
    } else {
        TRACE0("DEBUG updateGCMParams: no conversion done\n");
    }
    return NULL;
}

/*
 * the following functions convert Java arrays to PKCS#11 array pointers and
 * their array length and vice versa
 *
 * void j<Type>ArrayToCK<Type>Array(JNIEnv *env,
 *                                  const j<Type>Array jArray,
 *                                  CK_<Type>_PTR *ckpArray,
 *                                  CK_ULONG_PTR ckLength);
 *
 * j<Type>Array ck<Type>ArrayToJ<Type>Array(JNIEnv *env,
 *                                          const CK_<Type>_PTR ckpArray,
 *                                          CK_ULONG ckLength);
 *
 * PKCS#11 arrays consist always of a pointer to the beginning of the array and
 * the array length whereas Java arrays carry their array length.
 *
 * The Functions to convert a Java array to a PKCS#11 array are void functions.
 * Their arguments are the Java array object to convert, the reference to the
 * array pointer, where the new PKCS#11 array should be stored and the reference
 * to the array length where the PKCS#11 array length should be stored. These two
 * references must not be NULL_PTR.
 *
 * The functions first obtain the array length of the Java array and then allocate
 * the memory for the PKCS#11 array and set the array length. Then each element
 * gets converted depending on their type. After use the allocated memory of the
 * PKCS#11 array has to be explicitly freed.
 *
 * The Functions to convert a PKCS#11 array to a Java array get the PKCS#11 array
 * pointer and the array length and they return the new Java array object. The
 * Java array does not need to get freed after use.
 */

/*
 * converts a jbooleanArray to a CK_BBOOL array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_BBOOL array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jBooleanArrayToCKBBoolArray(JNIEnv *env, const jbooleanArray jArray, CK_BBOOL **ckpArray, CK_ULONG_PTR ckpLength)
{
    jboolean* jpTemp;
    CK_ULONG i;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    *ckpLength = (*env)->GetArrayLength(env, jArray);
    jpTemp = (jboolean*) calloc(*ckpLength, sizeof(jboolean));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    (*env)->GetBooleanArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
    if ((*env)->ExceptionCheck(env)) {
        free(jpTemp);
        return;
    }

    *ckpArray = (CK_BBOOL*) calloc (*ckpLength, sizeof(CK_BBOOL));
    if (*ckpArray == NULL) {
        free(jpTemp);
        throwOutOfMemoryError(env, 0);
        return;
    }
    for (i=0; i<(*ckpLength); i++) {
        (*ckpArray)[i] = jBooleanToCKBBool(jpTemp[i]);
    }
    free(jpTemp);
}

/*
 * converts a jbyteArray to a CK_BYTE array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_BYTE array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jByteArrayToCKByteArray(JNIEnv *env, const jbyteArray jArray, CK_BYTE_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    jbyte* jpTemp;
    CK_ULONG i;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    *ckpLength = (*env)->GetArrayLength(env, jArray);
    jpTemp = (jbyte*) calloc(*ckpLength, sizeof(jbyte));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    (*env)->GetByteArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
    if ((*env)->ExceptionCheck(env)) {
        free(jpTemp);
        return;
    }

    /* if CK_BYTE is the same size as jbyte, we save an additional copy */
    if (sizeof(CK_BYTE) == sizeof(jbyte)) {
        *ckpArray = (CK_BYTE_PTR) jpTemp;
    } else {
        *ckpArray = (CK_BYTE_PTR) calloc (*ckpLength, sizeof(CK_BYTE));
        if (*ckpArray == NULL) {
            free(jpTemp);
            throwOutOfMemoryError(env, 0);
            return;
        }
        for (i=0; i<(*ckpLength); i++) {
            (*ckpArray)[i] = jByteToCKByte(jpTemp[i]);
        }
        free(jpTemp);
    }
}

/*
 * converts a jlongArray to a CK_ULONG array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_ULONG array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jLongArrayToCKULongArray(JNIEnv *env, const jlongArray jArray, CK_ULONG_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    jlong* jTemp;
    CK_ULONG i;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    *ckpLength = (*env)->GetArrayLength(env, jArray);
    jTemp = (jlong*) calloc(*ckpLength, sizeof(jlong));
    if (jTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    (*env)->GetLongArrayRegion(env, jArray, 0, *ckpLength, jTemp);
    if ((*env)->ExceptionCheck(env)) {
        free(jTemp);
        return;
    }

    *ckpArray = (CK_ULONG_PTR) calloc(*ckpLength, sizeof(CK_ULONG));
    if (*ckpArray == NULL) {
        free(jTemp);
        throwOutOfMemoryError(env, 0);
        return;
    }
    for (i=0; i<(*ckpLength); i++) {
        (*ckpArray)[i] = jLongToCKULong(jTemp[i]);
    }
    free(jTemp);
}

/*
 * converts a jcharArray to a CK_CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jCharArrayToCKCharArray(JNIEnv *env, const jcharArray jArray, CK_CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    jchar* jpTemp;
    CK_ULONG i;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    *ckpLength = (*env)->GetArrayLength(env, jArray);
    jpTemp = (jchar*) calloc(*ckpLength, sizeof(jchar));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    (*env)->GetCharArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
    if ((*env)->ExceptionCheck(env)) {
        free(jpTemp);
        return;
    }

    *ckpArray = (CK_CHAR_PTR) calloc (*ckpLength, sizeof(CK_CHAR));
    if (*ckpArray == NULL) {
        free(jpTemp);
        throwOutOfMemoryError(env, 0);
        return;
    }
    for (i=0; i<(*ckpLength); i++) {
        (*ckpArray)[i] = jCharToCKChar(jpTemp[i]);
    }
    free(jpTemp);
}

/*
 * converts a jcharArray to a CK_UTF8CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_UTF8CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jCharArrayToCKUTF8CharArray(JNIEnv *env, const jcharArray jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    jchar* jTemp;
    CK_ULONG i;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    *ckpLength = (*env)->GetArrayLength(env, jArray);
    jTemp = (jchar*) calloc(*ckpLength, sizeof(jchar));
    if (jTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    (*env)->GetCharArrayRegion(env, jArray, 0, *ckpLength, jTemp);
    if ((*env)->ExceptionCheck(env)) {
        free(jTemp);
        return;
    }

    *ckpArray = (CK_UTF8CHAR_PTR) calloc(*ckpLength, sizeof(CK_UTF8CHAR));
    if (*ckpArray == NULL) {
        free(jTemp);
        throwOutOfMemoryError(env, 0);
        return;
    }
    for (i=0; i<(*ckpLength); i++) {
        (*ckpArray)[i] = jCharToCKUTF8Char(jTemp[i]);
    }
    free(jTemp);
}

/*
 * converts a jstring to a CK_CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jStringToCKUTF8CharArray(JNIEnv *env, const jstring jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    const char* pCharArray;
    jboolean isCopy;

    if(jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }

    pCharArray = (*env)->GetStringUTFChars(env, jArray, &isCopy);
    if (pCharArray == NULL) { return; }

    *ckpLength = (CK_ULONG) strlen(pCharArray);
    *ckpArray = (CK_UTF8CHAR_PTR) calloc(*ckpLength + 1, sizeof(CK_UTF8CHAR));
    if (*ckpArray == NULL) {
        (*env)->ReleaseStringUTFChars(env, (jstring) jArray, pCharArray);
        throwOutOfMemoryError(env, 0);
        return;
    }
    strcpy((char*)*ckpArray, pCharArray);
    (*env)->ReleaseStringUTFChars(env, (jstring) jArray, pCharArray);
}

/*
 * converts a jobjectArray with Java Attributes to a CK_ATTRIBUTE array. The allocated memory
 * has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array informtaion
 * @param jArray - the Java Attribute array (template) to convert
 * @param ckpArray - the reference, where the pointer to the new CK_ATTRIBUTE array will be
 *                   stored
 * @param ckpLength - the reference, where the array length will be stored
 */
void jAttributeArrayToCKAttributeArray(JNIEnv *env, jobjectArray jArray, CK_ATTRIBUTE_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
    CK_ULONG i;
    jlong jLength;
    jobject jAttribute;

    TRACE0("\nDEBUG: jAttributeArrayToCKAttributeArray");
    if (jArray == NULL) {
        *ckpArray = NULL_PTR;
        *ckpLength = 0L;
        return;
    }
    jLength = (*env)->GetArrayLength(env, jArray);
    *ckpLength = jLongToCKULong(jLength);
    *ckpArray = (CK_ATTRIBUTE_PTR) calloc(*ckpLength, sizeof(CK_ATTRIBUTE));
    if (*ckpArray == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    TRACE1(", converting %lld attributes", (long long int) jLength);
    for (i=0; i<(*ckpLength); i++) {
        TRACE1(", getting %lu. attribute", i);
        jAttribute = (*env)->GetObjectArrayElement(env, jArray, i);
        if ((*env)->ExceptionCheck(env)) {
            freeCKAttributeArray(*ckpArray, i);
            return;
        }
        TRACE1(", jAttribute , converting %lu. attribute", i);
        (*ckpArray)[i] = jAttributeToCKAttribute(env, jAttribute);
        if ((*env)->ExceptionCheck(env)) {
            freeCKAttributeArray(*ckpArray, i);
            return;
        }
    }
    TRACE0("FINISHED\n");
}

/*
 * converts a CK_BYTE array and its length to a jbyteArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_BYTE array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java byte array or NULL if error occurred
 */
jbyteArray ckByteArrayToJByteArray(JNIEnv *env, const CK_BYTE_PTR ckpArray, CK_ULONG ckLength)
{
    CK_ULONG i;
    jbyte* jpTemp;
    jbyteArray jArray;

    /* if CK_BYTE is the same size as jbyte, we save an additional copy */
    if (sizeof(CK_BYTE) == sizeof(jbyte)) {
        jpTemp = (jbyte*) ckpArray;
    } else {
        jpTemp = (jbyte*) calloc(ckLength, sizeof(jbyte));
        if (jpTemp == NULL) {
            throwOutOfMemoryError(env, 0);
            return NULL;
        }
        for (i=0; i<ckLength; i++) {
            jpTemp[i] = ckByteToJByte(ckpArray[i]);
        }
    }

    jArray = (*env)->NewByteArray(env, ckULongToJSize(ckLength));
    if (jArray != NULL) {
        (*env)->SetByteArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
    }

    if (sizeof(CK_BYTE) != sizeof(jbyte)) { free(jpTemp); }

    return jArray ;
}

/*
 * converts a CK_ULONG array and its length to a jlongArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_ULONG array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java long array
 */
jlongArray ckULongArrayToJLongArray(JNIEnv *env, const CK_ULONG_PTR ckpArray, CK_ULONG ckLength)
{
    CK_ULONG i;
    jlong* jpTemp;
    jlongArray jArray;

    jpTemp = (jlong*) calloc(ckLength, sizeof(jlong));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    for (i=0; i<ckLength; i++) {
        jpTemp[i] = ckLongToJLong(ckpArray[i]);
    }
    jArray = (*env)->NewLongArray(env, ckULongToJSize(ckLength));
    if (jArray != NULL) {
        (*env)->SetLongArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
    }
    free(jpTemp);

    return jArray ;
}

/*
 * converts a CK_CHAR array and its length to a jcharArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_CHAR array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java char array
 */
jcharArray ckCharArrayToJCharArray(JNIEnv *env, const CK_CHAR_PTR ckpArray, CK_ULONG ckLength)
{
    CK_ULONG i;
    jchar* jpTemp;
    jcharArray jArray;

    jpTemp = (jchar*) calloc(ckLength, sizeof(jchar));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    for (i=0; i<ckLength; i++) {
        jpTemp[i] = ckCharToJChar(ckpArray[i]);
    }
    jArray = (*env)->NewCharArray(env, ckULongToJSize(ckLength));
    if (jArray != NULL) {
        (*env)->SetCharArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
    }
    free(jpTemp);

    return jArray ;
}

/*
 * converts a CK_UTF8CHAR array and its length to a jcharArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_UTF8CHAR array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java char array
 */
jcharArray ckUTF8CharArrayToJCharArray(JNIEnv *env, const CK_UTF8CHAR_PTR ckpArray, CK_ULONG ckLength)
{
    CK_ULONG i;
    jchar* jpTemp;
    jcharArray jArray;

    jpTemp = (jchar*) calloc(ckLength, sizeof(jchar));
    if (jpTemp == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    for (i=0; i<ckLength; i++) {
        jpTemp[i] = ckUTF8CharToJChar(ckpArray[i]);
    }
    jArray = (*env)->NewCharArray(env, ckULongToJSize(ckLength));
    if (jArray != NULL) {
        (*env)->SetCharArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
    }
    free(jpTemp);

    return jArray ;
}

/*
 * the following functions convert Java objects to PKCS#11 pointers and the
 * length in bytes and vice versa
 *
 * CK_<Type>_PTR j<Object>ToCK<Type>Ptr(JNIEnv *env, jobject jObject);
 *
 * jobject ck<Type>PtrToJ<Object>(JNIEnv *env, const CK_<Type>_PTR ckpValue);
 *
 * The functions that convert a Java object to a PKCS#11 pointer first allocate
 * the memory for the PKCS#11 pointer. Then they set each element corresponding
 * to the fields in the Java object to convert. After use the allocated memory of
 * the PKCS#11 pointer has to be explicitly freed.
 *
 * The functions to convert a PKCS#11 pointer to a Java object create a new Java
 * object first and than they set all fields in the object depending on the values
 * of the type or structure where the PKCS#11 pointer points to.
 */

/*
 * converts a CK_BBOOL pointer to a Java boolean Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpValue - the pointer to the CK_BBOOL value
 * @return - the new Java boolean object with the boolean value
 */
jobject ckBBoolPtrToJBooleanObject(JNIEnv *env, const CK_BBOOL *ckpValue)
{
    jclass jValueObjectClass;
    jmethodID jConstructor;
    jobject jValueObject;
    jboolean jValue;

    jValueObjectClass = (*env)->FindClass(env, "java/lang/Boolean");
    if (jValueObjectClass == NULL) { return NULL; }
    jConstructor = (*env)->GetMethodID(env, jValueObjectClass, "<init>", "(Z)V");
    if (jConstructor == NULL) { return NULL; }
    jValue = ckBBoolToJBoolean(*ckpValue);
    jValueObject = (*env)->NewObject(env, jValueObjectClass, jConstructor, jValue);

    return jValueObject ;
}

/*
 * converts a CK_ULONG pointer to a Java long Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpValue - the pointer to the CK_ULONG value
 * @return - the new Java long object with the long value
 */
jobject ckULongPtrToJLongObject(JNIEnv *env, const CK_ULONG_PTR ckpValue)
{
    jclass jValueObjectClass;
    jmethodID jConstructor;
    jobject jValueObject;
    jlong jValue;

    jValueObjectClass = (*env)->FindClass(env, "java/lang/Long");
    if (jValueObjectClass == NULL) { return NULL; }
    jConstructor = (*env)->GetMethodID(env, jValueObjectClass, "<init>", "(J)V");
    if (jConstructor == NULL) { return NULL; }
    jValue = ckULongToJLong(*ckpValue);
    jValueObject = (*env)->NewObject(env, jValueObjectClass, jConstructor, jValue);

    return jValueObject ;
}

/*
 * converts a Java boolean object into a pointer to a CK_BBOOL value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Boolean" object to convert
 * @return - the pointer to the new CK_BBOOL value
 */
CK_BBOOL* jBooleanObjectToCKBBoolPtr(JNIEnv *env, jobject jObject)
{
    jclass jObjectClass;
    jmethodID jValueMethod;
    jboolean jValue;
    CK_BBOOL *ckpValue;

    jObjectClass = (*env)->FindClass(env, "java/lang/Boolean");
    if (jObjectClass == NULL) { return NULL; }
    jValueMethod = (*env)->GetMethodID(env, jObjectClass, "booleanValue", "()Z");
    if (jValueMethod == NULL) { return NULL; }
    jValue = (*env)->CallBooleanMethod(env, jObject, jValueMethod);
    ckpValue = (CK_BBOOL *) malloc(sizeof(CK_BBOOL));
    if (ckpValue == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    *ckpValue = jBooleanToCKBBool(jValue);

    return ckpValue ;
}

/*
 * converts a Java byte object into a pointer to a CK_BYTE value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Byte" object to convert
 * @return - the pointer to the new CK_BYTE value
 */
CK_BYTE_PTR jByteObjectToCKBytePtr(JNIEnv *env, jobject jObject)
{
    jclass jObjectClass;
    jmethodID jValueMethod;
    jbyte jValue;
    CK_BYTE_PTR ckpValue;

    jObjectClass = (*env)->FindClass(env, "java/lang/Byte");
    if (jObjectClass == NULL) { return NULL; }
    jValueMethod = (*env)->GetMethodID(env, jObjectClass, "byteValue", "()B");
    if (jValueMethod == NULL) { return NULL; }
    jValue = (*env)->CallByteMethod(env, jObject, jValueMethod);
    ckpValue = (CK_BYTE_PTR) malloc(sizeof(CK_BYTE));
    if (ckpValue == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    *ckpValue = jByteToCKByte(jValue);
    return ckpValue ;
}

/*
 * converts a Java integer object into a pointer to a CK_ULONG value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Integer" object to convert
 * @return - the pointer to the new CK_ULONG value
 */
CK_ULONG* jIntegerObjectToCKULongPtr(JNIEnv *env, jobject jObject)
{
    jclass jObjectClass;
    jmethodID jValueMethod;
    jint jValue;
    CK_ULONG *ckpValue;

    jObjectClass = (*env)->FindClass(env, "java/lang/Integer");
    if (jObjectClass == NULL) { return NULL; }
    jValueMethod = (*env)->GetMethodID(env, jObjectClass, "intValue", "()I");
    if (jValueMethod == NULL) { return NULL; }
    jValue = (*env)->CallIntMethod(env, jObject, jValueMethod);
    ckpValue = (CK_ULONG *) malloc(sizeof(CK_ULONG));
    if (ckpValue == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    *ckpValue = jLongToCKLong(jValue);
    return ckpValue ;
}

/*
 * converts a Java long object into a pointer to a CK_ULONG value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Long" object to convert
 * @return - the pointer to the new CK_ULONG value
 */
CK_ULONG* jLongObjectToCKULongPtr(JNIEnv *env, jobject jObject)
{
    jclass jObjectClass;
    jmethodID jValueMethod;
    jlong jValue;
    CK_ULONG *ckpValue;

    jObjectClass = (*env)->FindClass(env, "java/lang/Long");
    if (jObjectClass == NULL) { return NULL; }
    jValueMethod = (*env)->GetMethodID(env, jObjectClass, "longValue", "()J");
    if (jValueMethod == NULL) { return NULL; }
    jValue = (*env)->CallLongMethod(env, jObject, jValueMethod);
    ckpValue = (CK_ULONG *) malloc(sizeof(CK_ULONG));
    if (ckpValue == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    *ckpValue = jLongToCKULong(jValue);

    return ckpValue ;
}

/*
 * converts a Java char object into a pointer to a CK_CHAR value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Char" object to convert
 * @return - the pointer to the new CK_CHAR value
 */
CK_CHAR_PTR jCharObjectToCKCharPtr(JNIEnv *env, jobject jObject)
{
    jclass jObjectClass;
    jmethodID jValueMethod;
    jchar jValue;
    CK_CHAR_PTR ckpValue;

    jObjectClass = (*env)->FindClass(env, "java/lang/Char");
    if (jObjectClass == NULL) { return NULL; }
    jValueMethod = (*env)->GetMethodID(env, jObjectClass, "charValue", "()C");
    if (jValueMethod == NULL) { return NULL; }
    jValue = (*env)->CallCharMethod(env, jObject, jValueMethod);
    ckpValue = (CK_CHAR_PTR) malloc(sizeof(CK_CHAR));
    if (ckpValue == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    *ckpValue = jCharToCKChar(jValue);

    return ckpValue ;
}

/*
 * converts a Java object into a pointer to CK-type or a CK-structure with the length in Bytes.
 * The memory of the returned pointer MUST BE FREED BY CALLER!
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jObject - the Java object to convert
 * @param ckpLength - pointer to the length (bytes) of the newly-allocated CK-value or CK-structure
 * @return ckpObject - pointer to the newly-allocated CK-value or CK-structure
 */
CK_VOID_PTR jObjectToPrimitiveCKObjectPtr(JNIEnv *env, jobject jObject, CK_ULONG *ckpLength)
{
    jclass jLongClass, jBooleanClass, jByteArrayClass, jCharArrayClass;
    jclass jByteClass, jDateClass, jCharacterClass, jIntegerClass;
    jclass jBooleanArrayClass, jIntArrayClass, jLongArrayClass;
    jclass jStringClass;
    jclass jObjectClass, jClassClass;
    CK_VOID_PTR ckpObject;
    jmethodID jMethod;
    jobject jClassObject;
    jstring jClassNameString;
    char *classNameString, *exceptionMsgPrefix, *exceptionMsg;

    TRACE0("\nDEBUG: jObjectToPrimitiveCKObjectPtr");
    if (jObject == NULL) {
        *ckpLength = 0;
        return NULL;
    }

    jLongClass = (*env)->FindClass(env, "java/lang/Long");
    if (jLongClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jLongClass)) {
        ckpObject = jLongObjectToCKULongPtr(env, jObject);
        *ckpLength = sizeof(CK_ULONG);
        TRACE1("<converted long value %lu>", *((CK_ULONG *) ckpObject));
        return ckpObject;
    }

    jBooleanClass = (*env)->FindClass(env, "java/lang/Boolean");
    if (jBooleanClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jBooleanClass)) {
        ckpObject = jBooleanObjectToCKBBoolPtr(env, jObject);
        *ckpLength = sizeof(CK_BBOOL);
        TRACE0(" <converted boolean value ");
        TRACE0((*((CK_BBOOL *) ckpObject) == TRUE) ? "TRUE>" : "FALSE>");
        return ckpObject;
    }

    jByteArrayClass = (*env)->FindClass(env, "[B");
    if (jByteArrayClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jByteArrayClass)) {
        jByteArrayToCKByteArray(env, jObject, (CK_BYTE_PTR*) &ckpObject, ckpLength);
        return ckpObject;
    }

    jCharArrayClass = (*env)->FindClass(env, "[C");
    if (jCharArrayClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jCharArrayClass)) {
        jCharArrayToCKUTF8CharArray(env, jObject, (CK_UTF8CHAR_PTR*) &ckpObject, ckpLength);
        return ckpObject;
    }

    jByteClass = (*env)->FindClass(env, "java/lang/Byte");
    if (jByteClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jByteClass)) {
        ckpObject = jByteObjectToCKBytePtr(env, jObject);
        *ckpLength = sizeof(CK_BYTE);
        TRACE1("<converted byte value %X>", *((CK_BYTE *) ckpObject));
        return ckpObject;
    }

    jDateClass = (*env)->FindClass(env, CLASS_DATE);
    if (jDateClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jDateClass)) {
        ckpObject = jDateObjectToCKDatePtr(env, jObject);
        *ckpLength = sizeof(CK_DATE);
        TRACE3("<converted date value %.4s-%.2s-%.2s>", ((CK_DATE *) ckpObject)->year,
                ((CK_DATE *) ckpObject)->month, ((CK_DATE *) ckpObject)->day);
        return ckpObject;
    }

    jCharacterClass = (*env)->FindClass(env, "java/lang/Character");
    if (jCharacterClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jCharacterClass)) {
        ckpObject = jCharObjectToCKCharPtr(env, jObject);
        *ckpLength = sizeof(CK_UTF8CHAR);
        TRACE1("<converted char value %c>", *((CK_CHAR *) ckpObject));
        return ckpObject;
    }

    jIntegerClass = (*env)->FindClass(env, "java/lang/Integer");
    if (jIntegerClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jIntegerClass)) {
        ckpObject = jIntegerObjectToCKULongPtr(env, jObject);
        *ckpLength = sizeof(CK_ULONG);
        TRACE1("<converted integer value %lu>", *((CK_ULONG *) ckpObject));
        return ckpObject;
    }

    jBooleanArrayClass = (*env)->FindClass(env, "[Z");
    if (jBooleanArrayClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jBooleanArrayClass)) {
        jBooleanArrayToCKBBoolArray(env, jObject, (CK_BBOOL**) &ckpObject, ckpLength);
        return ckpObject;
    }

    jIntArrayClass = (*env)->FindClass(env, "[I");
    if (jIntArrayClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jIntArrayClass)) {
        jLongArrayToCKULongArray(env, jObject, (CK_ULONG_PTR*) &ckpObject, ckpLength);
        return ckpObject;
    }

    jLongArrayClass = (*env)->FindClass(env, "[J");
    if (jLongArrayClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jLongArrayClass)) {
        jLongArrayToCKULongArray(env, jObject, (CK_ULONG_PTR*) &ckpObject, ckpLength);
        return ckpObject;
    }

    jStringClass = (*env)->FindClass(env, "java/lang/String");
    if (jStringClass == NULL) { return NULL; }
    if ((*env)->IsInstanceOf(env, jObject, jStringClass)) {
        jStringToCKUTF8CharArray(env, jObject, (CK_UTF8CHAR_PTR*) &ckpObject, ckpLength);
        return ckpObject;
    }

    /* type of jObject unknown, throw PKCS11RuntimeException */
    jObjectClass = (*env)->FindClass(env, "java/lang/Object");
    if (jObjectClass == NULL) { return NULL; }
    jMethod = (*env)->GetMethodID(env, jObjectClass, "getClass", "()Ljava/lang/Class;");
    if (jMethod == NULL) { return NULL; }
    jClassObject = (*env)->CallObjectMethod(env, jObject, jMethod);
    assert(jClassObject != 0);
    jClassClass = (*env)->FindClass(env, "java/lang/Class");
    if (jClassClass == NULL) { return NULL; }
    jMethod = (*env)->GetMethodID(env, jClassClass, "getName", "()Ljava/lang/String;");
    if (jMethod == NULL) { return NULL; }
    jClassNameString = (jstring)
        (*env)->CallObjectMethod(env, jClassObject, jMethod);
    assert(jClassNameString != 0);
    classNameString = (char*)
        (*env)->GetStringUTFChars(env, jClassNameString, NULL);
    if (classNameString == NULL) { return NULL; }
    exceptionMsgPrefix = "Java object of this class cannot be converted to native PKCS#11 type: ";
    exceptionMsg = (char *)
        malloc(strlen(exceptionMsgPrefix) + strlen(classNameString) + 1);
    if (exceptionMsg == NULL) {
        (*env)->ReleaseStringUTFChars(env, jClassNameString, classNameString);
        throwOutOfMemoryError(env, 0);
        return NULL;
    }
    strcpy(exceptionMsg, exceptionMsgPrefix);
    strcat(exceptionMsg, classNameString);
    (*env)->ReleaseStringUTFChars(env, jClassNameString, classNameString);
    throwPKCS11RuntimeException(env, exceptionMsg);
    free(exceptionMsg);
    *ckpLength = 0;

    TRACE0("FINISHED\n");
    return NULL;
}

#ifdef P11_MEMORYDEBUG

#undef malloc
#undef calloc
#undef free

void *p11malloc(size_t c, char *file, int line) {
    void *p = malloc(c);
    fprintf(stdout, "malloc\t%08lX\t%lX\t%s:%d\n", ptr_to_jlong(p), c, file, line);
    fflush(stdout);
    return p;
}

void *p11calloc(size_t c, size_t s, char *file, int line) {
    void *p = calloc(c, s);
    fprintf(stdout, "calloc\t%08lX\t%lX\t%lX\t%s:%d\n", ptr_to_jlong(p), c, s, file, line);
    fflush(stdout);
    return p;
}

void p11free(void *p, char *file, int line) {
    fprintf(stdout, "free\t%08lX\t\t%s:%d\n", ptr_to_jlong(p), file, line);
    fflush(stdout);
    free(p);
}

#endif

// prints a message to stdout if debug output is enabled
void printDebug(const char *format, ...) {
    if (debug == JNI_TRUE) {
        va_list args;
        fprintf(stdout, "sunpkcs11: ");
        va_start(args, format);
        vfprintf(stdout, format, args);
        va_end(args);
        fflush(stdout);
    }
}

