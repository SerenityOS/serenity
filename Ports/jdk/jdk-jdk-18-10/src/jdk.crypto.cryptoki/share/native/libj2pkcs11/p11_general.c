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

/* declare file private functions */

void prefetchFields(JNIEnv *env, jclass thisClass);
jobject ckInfoPtrToJInfo(JNIEnv *env, const CK_INFO_PTR ckpInfo);
jobject ckSlotInfoPtrToJSlotInfo(JNIEnv *env, const CK_SLOT_INFO_PTR ckpSlotInfo);
jobject ckTokenInfoPtrToJTokenInfo(JNIEnv *env, const CK_TOKEN_INFO_PTR ckpTokenInfo);
jobject ckMechanismInfoPtrToJMechanismInfo(JNIEnv *env, const CK_MECHANISM_INFO_PTR ckpMechanismInfo);

/* define variables */

jfieldID pNativeDataID;
jfieldID mech_mechanismID;
jfieldID mech_pParameterID;
jfieldID mech_pHandleID;

jclass jByteArrayClass;
jclass jLongClass;

JavaVM* jvm = NULL;

jboolean debug = 0;

JNIEXPORT jint JNICALL DEF_JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    return JNI_VERSION_1_4;
}

/* ************************************************************************** */
/* The native implementation of the methods of the PKCS11Implementation class */
/* ************************************************************************** */

/*
 * This method is used to do free the memory allocated for CK_MECHANISM structure.
 *
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    freeMechanism
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_freeMechanism
(JNIEnv *env, jclass thisClass, jlong ckpMechanism) {
    if (ckpMechanism != 0L) {
        freeCKMechanismPtr(jlong_to_ptr(ckpMechanism));
        TRACE1("DEBUG PKCS11_freeMechanism: free pMech = %lld\n", (long long int) ckpMechanism);
    }
    return 0L;
}

/*
 * This method is used to do static initialization. This method is static and
 * synchronized. Summary: use this method like a static initialization block.
 *
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    initializeLibrary
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_initializeLibrary
(JNIEnv *env, jclass thisClass, jboolean enableDebug)
{
#ifndef NO_CALLBACKS
    if (notifyListLock == NULL) {
        notifyListLock = createLockObject(env);
    }
#endif

    prefetchFields(env, thisClass);
    debug = enableDebug;
}

jclass fetchClass(JNIEnv *env, const char *name) {
    jclass tmpClass = (*env)->FindClass(env, name);
    if (tmpClass == NULL) { return NULL; }
    return (*env)->NewGlobalRef(env, tmpClass);
}

void prefetchFields(JNIEnv *env, jclass thisClass) {
    jclass tmpClass;

    /* PKCS11 - pNativeData */
    pNativeDataID = (*env)->GetFieldID(env, thisClass, "pNativeData", "J");
    if (pNativeDataID == NULL) { return; }

    /* CK_MECHANISM - mechanism, pParameter, pHandle */
    tmpClass = (*env)->FindClass(env, CLASS_MECHANISM);
    if (tmpClass == NULL) { return; }
    mech_mechanismID = (*env)->GetFieldID(env, tmpClass, "mechanism", "J");
    if (mech_mechanismID == NULL) { return; }
    mech_pParameterID = (*env)->GetFieldID(env, tmpClass, "pParameter",
                                           "Ljava/lang/Object;");
    if (mech_pParameterID == NULL) { return; }
    mech_pHandleID = (*env)->GetFieldID(env, tmpClass, "pHandle", "J");
    if (mech_pHandleID == NULL) { return; }

    /* java classes for primitive types - byte[], long */
    jByteArrayClass = fetchClass(env, "[B");
    if (jByteArrayClass == NULL) { return; }
    jLongClass = fetchClass(env, "java/lang/Long");
}

/* This method is designed to do a clean-up. It releases all global resources
 * of this library. By now, this function is not called. Calling from
 * JNI_OnUnload would be an option, but some VMs do not support JNI_OnUnload.
 *
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    finalizeLibrary
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_finalizeLibrary
(JNIEnv *env, jclass thisClass)
{
/* XXX
    * remove all left lists and release the resources and the lock
     * objects that synchroniz access to these lists.
     *
    removeAllModuleEntries(env);
    if (moduleListHead == NULL) { * check, if we removed the last active module *
        * remove also the moduleListLock, it is no longer used *
        if (moduleListLock != NULL) {
            destroyLockObject(env, moduleListLock);
            moduleListLock = NULL;
        }
#ifndef NO_CALLBACKS
        * remove all left notify callback entries *
        while (removeFirstNotifyEntry(env));
        * remove also the notifyListLock, it is no longer used *
        if (notifyListLock != NULL) {
            destroyLockObject(env, notifyListLock);
            notifyListLock = NULL;
        }
        if (jInitArgsObject != NULL) {
            (*env)->DeleteGlobalRef(env, jInitArgsObject);
        }
        if (ckpGlobalInitArgs != NULL_PTR) {
            free(ckpGlobalInitArgs);
        }
#endif * NO_CALLBACKS *
    }
*/
}

#ifdef P11_ENABLE_C_INITIALIZE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Initialize
 * Signature: (Ljava/lang/Object;)V
 * Parametermapping:                    *PKCS11*
 * @param   jobject jInitArgs           CK_VOID_PTR pInitArgs
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1Initialize
(JNIEnv *env, jobject obj, jobject jInitArgs)
{
    /*
     * Initalize Cryptoki
     */
    CK_C_INITIALIZE_ARGS_PTR ckpInitArgs;
    CK_RV rv;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0("DEBUG: initializing module... ");

    ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) {
        TRACE0("failed getting module entry");
        return;
    }

    ckpInitArgs = (jInitArgs != NULL)
                ? makeCKInitArgsAdapter(env, jInitArgs)
                : NULL_PTR;

    rv = (*ckpFunctions->C_Initialize)(ckpInitArgs);

    free(ckpInitArgs);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) {
      TRACE1("DEBUG: C_Initialize had a bad return value %lu \n", (unsigned long) rv);
      return;
    }

    TRACE0("FINISHED\n");
}
#endif

#ifdef P11_ENABLE_C_FINALIZE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Finalize
 * Signature: (Ljava/lang/Object;)V
 * Parametermapping:                    *PKCS11*
 * @param   jobject jReserved           CK_VOID_PTR pReserved
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1Finalize
(JNIEnv *env, jobject obj, jobject jReserved)
{
    /*
     * Finalize Cryptoki
     */
    CK_VOID_PTR ckpReserved;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckpReserved = jObjectToCKVoidPtr(jReserved);

    rv = (*ckpFunctions->C_Finalize)(ckpReserved);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_GETINFO
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetInfo
 * Signature: ()Lsun/security/pkcs11/wrapper/CK_INFO;
 * Parametermapping:                    *PKCS11*
 * @return  jobject jInfoObject         CK_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetInfo
(JNIEnv *env, jobject obj)
{
    CK_INFO ckLibInfo;
    jobject jInfoObject=NULL;
    CK_RV rv;
    CK_FUNCTION_LIST_PTR ckpFunctions;
    memset(&ckLibInfo, 0, sizeof(CK_INFO));

    ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    rv = (*ckpFunctions->C_GetInfo)(&ckLibInfo);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jInfoObject = ckInfoPtrToJInfo(env, &ckLibInfo);
    }
    return jInfoObject ;
}

/*
 * converts a pointer to a CK_INFO structure into a Java CK_INFO Object.
 *
 * @param env - used to call JNI funktions to create the new Java object
 * @param ckpInfo - the pointer to the CK_INFO structure
 * @return - the new Java CK_INFO object
 */
jobject ckInfoPtrToJInfo(JNIEnv *env, const CK_INFO_PTR ckpInfo)
{
    jclass jInfoClass;
    jmethodID jCtrId;
    jobject jInfoObject;
    jobject jCryptokiVer;
    jcharArray jVendor;
    jlong jFlags;
    jcharArray jLibraryDesc;
    jobject jLibraryVer;

    /* load CK_INFO class */
    jInfoClass = (*env)->FindClass(env, CLASS_INFO);
    if (jInfoClass == NULL) { return NULL; };

    /* load CK_INFO constructor */
    jCtrId = (*env)->GetMethodID
      (env, jInfoClass, "<init>",
       "(Lsun/security/pkcs11/wrapper/CK_VERSION;[CJ[CLsun/security/pkcs11/wrapper/CK_VERSION;)V");
    if (jCtrId == NULL) { return NULL; }

    /* prep all fields */
    jCryptokiVer = ckVersionPtrToJVersion(env, &(ckpInfo->cryptokiVersion));
    if (jCryptokiVer == NULL) { return NULL; }
    jVendor =
      ckUTF8CharArrayToJCharArray(env, &(ckpInfo->manufacturerID[0]), 32);
    if (jVendor == NULL) { return NULL; }
    jFlags = ckULongToJLong(ckpInfo->flags);
    jLibraryDesc =
      ckUTF8CharArrayToJCharArray(env, &(ckpInfo->libraryDescription[0]), 32);
    if (jLibraryDesc == NULL) { return NULL; }
    jLibraryVer = ckVersionPtrToJVersion(env, &(ckpInfo->libraryVersion));
    if (jLibraryVer == NULL) { return NULL; }

    /* create new CK_INFO object */
    jInfoObject = (*env)->NewObject(env, jInfoClass, jCtrId, jCryptokiVer,
                                    jVendor, jFlags, jLibraryDesc, jLibraryVer);
    if (jInfoObject == NULL) { return NULL; }

    /* free local references */
    (*env)->DeleteLocalRef(env, jInfoClass);
    (*env)->DeleteLocalRef(env, jCryptokiVer);
    (*env)->DeleteLocalRef(env, jVendor);
    (*env)->DeleteLocalRef(env, jLibraryDesc);
    (*env)->DeleteLocalRef(env, jLibraryVer);

    return jInfoObject ;
}
#endif

#ifdef P11_ENABLE_C_GETSLOTLIST
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetSlotList
 * Signature: (Z)[J
 * Parametermapping:                    *PKCS11*
 * @param   jboolean jTokenPresent      CK_BBOOL tokenPresent
 * @return  jlongArray jSlotList        CK_SLOT_ID_PTR pSlotList
 *                                      CK_ULONG_PTR pulCount
 */
JNIEXPORT jlongArray JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetSlotList
(JNIEnv *env, jobject obj, jboolean jTokenPresent)
{
    CK_ULONG ckTokenNumber;
    CK_SLOT_ID_PTR ckpSlotList;
    CK_BBOOL ckTokenPresent;
    jlongArray jSlotList = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckTokenPresent = jBooleanToCKBBool(jTokenPresent);

    rv = (*ckpFunctions->C_GetSlotList)(ckTokenPresent, NULL_PTR,
                                        &ckTokenNumber);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return NULL ; }

    ckpSlotList = (CK_SLOT_ID_PTR) malloc(ckTokenNumber * sizeof(CK_SLOT_ID));
    if (ckpSlotList == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }

    rv = (*ckpFunctions->C_GetSlotList)(ckTokenPresent, ckpSlotList,
                                        &ckTokenNumber);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSlotList = ckULongArrayToJLongArray(env, ckpSlotList, ckTokenNumber);
    }
    free(ckpSlotList);

    return jSlotList ;
}
#endif

#ifdef P11_ENABLE_C_GETSLOTINFO
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetSlotInfo
 * Signature: (J)Lsun/security/pkcs11/wrapper/CK_SLOT_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jobject jSlotInfoObject     CK_SLOT_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetSlotInfo
(JNIEnv *env, jobject obj, jlong jSlotID)
{
    CK_SLOT_ID ckSlotID;
    CK_SLOT_INFO ckSlotInfo;
    jobject jSlotInfoObject=NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetSlotInfo)(ckSlotID, &ckSlotInfo);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSlotInfoObject = ckSlotInfoPtrToJSlotInfo(env, &ckSlotInfo);
    }
    return jSlotInfoObject;
}

/*
 * converts a pointer to a CK_SLOT_INFO structure into a Java CK_SLOT_INFO
 * Object.
 *
 * @param env - used to call JNI funktions to create the new Java object
 * @param ckpSlotInfo - the pointer to the CK_SLOT_INFO structure
 * @return - the new Java CK_SLOT_INFO object
 */
jobject
ckSlotInfoPtrToJSlotInfo
(JNIEnv *env, const CK_SLOT_INFO_PTR ckpSlotInfo)
{
    jclass jSlotInfoClass;
    jmethodID jCtrId;
    jobject jSlotInfoObject;
    jcharArray jSlotDesc;
    jcharArray jVendor;
    jlong jFlags;
    jobject jHardwareVer;
    jobject jFirmwareVer;

    /* load CK_SLOT_INFO class */
    jSlotInfoClass = (*env)->FindClass(env, CLASS_SLOT_INFO);
    if (jSlotInfoClass == NULL) { return NULL; };

    /* load CK_SLOT_INFO constructor */
    jCtrId = (*env)->GetMethodID
      (env, jSlotInfoClass, "<init>",
       "([C[CJLsun/security/pkcs11/wrapper/CK_VERSION;Lsun/security/pkcs11/wrapper/CK_VERSION;)V");
    if (jCtrId == NULL) { return NULL; }

    /* prep all fields */
    jSlotDesc =
      ckUTF8CharArrayToJCharArray(env, &(ckpSlotInfo->slotDescription[0]), 64);
    if (jSlotDesc == NULL) { return NULL; }
    jVendor =
      ckUTF8CharArrayToJCharArray(env, &(ckpSlotInfo->manufacturerID[0]), 32);
    if (jVendor == NULL) { return NULL; }
    jFlags = ckULongToJLong(ckpSlotInfo->flags);
    jHardwareVer = ckVersionPtrToJVersion(env, &(ckpSlotInfo->hardwareVersion));
    if (jHardwareVer == NULL) { return NULL; }
    jFirmwareVer = ckVersionPtrToJVersion(env, &(ckpSlotInfo->firmwareVersion));
    if (jFirmwareVer == NULL) { return NULL; }

    /* create new CK_SLOT_INFO object */
    jSlotInfoObject = (*env)->NewObject
      (env, jSlotInfoClass, jCtrId, jSlotDesc, jVendor, jFlags,
       jHardwareVer, jFirmwareVer);
    if (jSlotInfoObject == NULL) { return NULL; }

    /* free local references */
    (*env)->DeleteLocalRef(env, jSlotInfoClass);
    (*env)->DeleteLocalRef(env, jSlotDesc);
    (*env)->DeleteLocalRef(env, jVendor);
    (*env)->DeleteLocalRef(env, jHardwareVer);
    (*env)->DeleteLocalRef(env, jFirmwareVer);

    return jSlotInfoObject ;
}

#endif

#ifdef P11_ENABLE_C_GETTOKENINFO
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetTokenInfo
 * Signature: (J)Lsun/security/pkcs11/wrapper/CK_TOKEN_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jobject jInfoTokenObject    CK_TOKEN_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetTokenInfo
(JNIEnv *env, jobject obj, jlong jSlotID)
{
    CK_SLOT_ID ckSlotID;
    CK_TOKEN_INFO ckTokenInfo;
    jobject jInfoTokenObject = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetTokenInfo)(ckSlotID, &ckTokenInfo);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jInfoTokenObject = ckTokenInfoPtrToJTokenInfo(env, &ckTokenInfo);
    }
    return jInfoTokenObject ;
}

/*
 * converts a pointer to a CK_TOKEN_INFO structure into a Java CK_TOKEN_INFO
 * Object.
 *
 * @param env - used to call JNI funktions to create the new Java object
 * @param ckpTokenInfo - the pointer to the CK_TOKEN_INFO structure
 * @return - the new Java CK_TOKEN_INFO object
 */
jobject
ckTokenInfoPtrToJTokenInfo
(JNIEnv *env, const CK_TOKEN_INFO_PTR ckpTokenInfo)
{
    jclass jTokenInfoClass;
    jmethodID jCtrId;
    jobject jTokenInfoObject;
    jcharArray jLabel;
    jcharArray jVendor;
    jcharArray jModel;
    jcharArray jSerialNo;
    jlong jFlags;
    jlong jMaxSnCnt;
    jlong jSnCnt;
    jlong jMaxRwSnCnt;
    jlong jRwSnCnt;
    jlong jMaxPinLen;
    jlong jMinPinLen;
    jlong jTotalPubMem;
    jlong jFreePubMem;
    jlong jTotalPrivMem;
    jlong jFreePrivMem;
    jobject jHardwareVer;
    jobject jFirmwareVer;
    jcharArray jUtcTime;

    /* load CK_TOKEN_INFO class */
    jTokenInfoClass = (*env)->FindClass(env, CLASS_TOKEN_INFO);
    if (jTokenInfoClass == NULL)  { return NULL; };

    /* load CK_TOKEN_INFO constructor */
    jCtrId = (*env)->GetMethodID
      (env, jTokenInfoClass, "<init>",
       "([C[C[C[CJJJJJJJJJJJLsun/security/pkcs11/wrapper/CK_VERSION;Lsun/security/pkcs11/wrapper/CK_VERSION;[C)V");
    if (jCtrId == NULL)  { return NULL; };

    /* prep all fields */
    jLabel = ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->label[0]), 32);
    if (jLabel == NULL)  { return NULL; };
    jVendor =
      ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->manufacturerID[0]), 32);
    if (jVendor == NULL)  { return NULL; };
    jModel = ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->model[0]), 16);
    if (jModel == NULL)  { return NULL; };
    jSerialNo =
      ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->serialNumber[0]), 16);
    if (jSerialNo == NULL)  { return NULL; };
    jFlags = ckULongToJLong(ckpTokenInfo->flags);
    jMaxSnCnt = ckULongSpecialToJLong(ckpTokenInfo->ulMaxSessionCount);
    jSnCnt = ckULongSpecialToJLong(ckpTokenInfo->ulSessionCount);
    jMaxRwSnCnt = ckULongSpecialToJLong(ckpTokenInfo->ulMaxRwSessionCount);
    jRwSnCnt = ckULongSpecialToJLong(ckpTokenInfo->ulRwSessionCount);
    jMaxPinLen = ckULongToJLong(ckpTokenInfo->ulMaxPinLen);
    jMinPinLen = ckULongToJLong(ckpTokenInfo->ulMinPinLen);
    jTotalPubMem = ckULongSpecialToJLong(ckpTokenInfo->ulTotalPublicMemory);
    jFreePubMem = ckULongSpecialToJLong(ckpTokenInfo->ulFreePublicMemory);
    jTotalPrivMem = ckULongSpecialToJLong(ckpTokenInfo->ulTotalPrivateMemory);
    jFreePrivMem = ckULongSpecialToJLong(ckpTokenInfo->ulFreePrivateMemory);
    jHardwareVer =
      ckVersionPtrToJVersion(env, &(ckpTokenInfo->hardwareVersion));
    if (jHardwareVer == NULL) { return NULL; }
    jFirmwareVer =
      ckVersionPtrToJVersion(env, &(ckpTokenInfo->firmwareVersion));
    if (jFirmwareVer == NULL) { return NULL; }
    jUtcTime =
      ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->utcTime[0]), 16);
    if (jUtcTime == NULL) { return NULL; }

    /* create new CK_TOKEN_INFO object */
    jTokenInfoObject =
      (*env)->NewObject(env, jTokenInfoClass, jCtrId, jLabel, jVendor, jModel,
                        jSerialNo, jFlags,
                        jMaxSnCnt, jSnCnt, jMaxRwSnCnt, jRwSnCnt,
                        jMaxPinLen, jMinPinLen,
                        jTotalPubMem, jFreePubMem, jTotalPrivMem, jFreePrivMem,
                        jHardwareVer, jFirmwareVer, jUtcTime);
    if (jTokenInfoObject == NULL) { return NULL; }

    /* free local references */
    (*env)->DeleteLocalRef(env, jTokenInfoClass);
    (*env)->DeleteLocalRef(env, jLabel);
    (*env)->DeleteLocalRef(env, jVendor);
    (*env)->DeleteLocalRef(env, jModel);
    (*env)->DeleteLocalRef(env, jSerialNo);
    (*env)->DeleteLocalRef(env, jHardwareVer);
    (*env)->DeleteLocalRef(env, jFirmwareVer);

    return jTokenInfoObject ;
}
#endif

#ifdef P11_ENABLE_C_WAITFORSLOTEVENT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_WaitForSlotEvent
 * Signature: (JLjava/lang/Object;)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jFlags                CK_FLAGS flags
 * @param   jobject jReserved           CK_VOID_PTR pReserved
 * @return  jlong jSlotID               CK_SLOT_ID_PTR pSlot
 */
JNIEXPORT jlong JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1WaitForSlotEvent
(JNIEnv *env, jobject obj, jlong jFlags, jobject jReserved)
{
    CK_FLAGS ckFlags;
    CK_SLOT_ID ckSlotID;
    jlong jSlotID = 0L;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0L; }

    ckFlags = jLongToCKULong(jFlags);

    rv = (*ckpFunctions->C_WaitForSlotEvent)(ckFlags, &ckSlotID, NULL_PTR);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSlotID = ckULongToJLong(ckSlotID);
    }

    return jSlotID ;
}
#endif

#ifdef P11_ENABLE_C_GETMECHANISMLIST
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetMechanismList
 * Signature: (J)[J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jlongArray jMechanismList   CK_MECHANISM_TYPE_PTR pMechanismList
 *                                      CK_ULONG_PTR pulCount
 */
JNIEXPORT jlongArray JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetMechanismList
(JNIEnv *env, jobject obj, jlong jSlotID)
{
    CK_SLOT_ID ckSlotID;
    CK_ULONG ckMechanismNumber;
    CK_MECHANISM_TYPE_PTR ckpMechanismList;
    jlongArray jMechanismList = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetMechanismList)(ckSlotID, NULL_PTR,
                                             &ckMechanismNumber);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return NULL ; }

    ckpMechanismList = (CK_MECHANISM_TYPE_PTR)
      malloc(ckMechanismNumber * sizeof(CK_MECHANISM_TYPE));
    if (ckpMechanismList == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }

    rv = (*ckpFunctions->C_GetMechanismList)(ckSlotID, ckpMechanismList,
                                             &ckMechanismNumber);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jMechanismList = ckULongArrayToJLongArray(env, ckpMechanismList,
                                                  ckMechanismNumber);
    }
    free(ckpMechanismList);

    return jMechanismList ;
}
#endif

#ifdef P11_ENABLE_C_GETMECHANISMINFO
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetMechanismInfo
 * Signature: (JJ)Lsun/security/pkcs11/wrapper/CK_MECHANISM_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jlong jType                 CK_MECHANISM_TYPE type
 * @return  jobject jMechanismInfo      CK_MECHANISM_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetMechanismInfo
(JNIEnv *env, jobject obj, jlong jSlotID, jlong jType)
{
    CK_SLOT_ID ckSlotID;
    CK_MECHANISM_TYPE ckMechanismType;
    CK_MECHANISM_INFO ckMechanismInfo;
    jobject jMechanismInfo = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSlotID = jLongToCKULong(jSlotID);
    ckMechanismType = jLongToCKULong(jType);

    rv = (*ckpFunctions->C_GetMechanismInfo)(ckSlotID, ckMechanismType,
                                             &ckMechanismInfo);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jMechanismInfo = ckMechanismInfoPtrToJMechanismInfo(env, &ckMechanismInfo);
    }
    return jMechanismInfo ;
}

/*
 * converts a pointer to a CK_MECHANISM_INFO structure into a Java
 * CK_MECHANISM_INFO Object.
 *
 * @param env - used to call JNI funktions to create the new Java object
 * @param ckpMechanismInfo - the pointer to the CK_MECHANISM_INFO structure
 * @return - the new Java CK_MECHANISM_INFO object
 */
jobject
ckMechanismInfoPtrToJMechanismInfo
(JNIEnv *env, const CK_MECHANISM_INFO_PTR ckpMechanismInfo)
{

    jclass jMechanismInfoClass;
    jmethodID jCtrId;
    jobject jMechanismInfoObject;
    jlong jMinKeySize;
    jlong jMaxKeySize;
    jlong jFlags;

    /* load CK_MECHANISM_INFO class */
    jMechanismInfoClass = (*env)->FindClass(env, CLASS_MECHANISM_INFO);
    if (jMechanismInfoClass == NULL) { return NULL; };

    /* load CK_MECHANISM_INFO constructor */
    jCtrId = (*env)->GetMethodID(env, jMechanismInfoClass, "<init>", "(JJJ)V");
    if (jCtrId == NULL) { return NULL; };

    /* prep all fields */
    jMinKeySize = ckULongToJLong(ckpMechanismInfo->ulMinKeySize);
    jMaxKeySize = ckULongToJLong(ckpMechanismInfo->ulMaxKeySize);
    jFlags = ckULongToJLong(ckpMechanismInfo->flags);

    /* create new CK_MECHANISM_INFO object */
    jMechanismInfoObject = (*env)->NewObject(env, jMechanismInfoClass, jCtrId,
                                             jMinKeySize, jMaxKeySize, jFlags);
    if (jMechanismInfoObject == NULL) { return NULL; };

    /* free local references */
    (*env)->DeleteLocalRef(env, jMechanismInfoClass);

    return jMechanismInfoObject ;
}
#endif

#ifdef P11_ENABLE_C_INITTOKEN
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_InitToken
 * Signature: (J[C[C)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jcharArray jPin             CK_CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 * @param   jcharArray jLabel           CK_UTF8CHAR_PTR pLabel
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1InitToken
(JNIEnv *env, jobject obj, jlong jSlotID, jcharArray jPin, jcharArray jLabel)
{
    CK_SLOT_ID ckSlotID;
    CK_CHAR_PTR ckpPin = NULL_PTR;
    CK_UTF8CHAR_PTR ckpLabel = NULL_PTR;
    CK_ULONG ckPinLength;
    CK_ULONG ckLabelLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSlotID = jLongToCKULong(jSlotID);
    jCharArrayToCKCharArray(env, jPin, &ckpPin, &ckPinLength);
    if ((*env)->ExceptionCheck(env)) { return; }
    /* ckLabelLength <= 32 !!! */
    jCharArrayToCKUTF8CharArray(env, jLabel, &ckpLabel, &ckLabelLength);
    if ((*env)->ExceptionCheck(env)) {
        free(ckpPin);
        return;
    }

    rv = (*ckpFunctions->C_InitToken)(ckSlotID, ckpPin, ckPinLength, ckpLabel);
    TRACE1("InitToken return code: %d", rv);

    free(ckpPin);
    free(ckpLabel);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_INITPIN
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_InitPIN
 * Signature: (J[C)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE
 * @param   jcharArray jPin             CK_CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1InitPIN
(JNIEnv *env, jobject obj, jlong jSessionHandle, jcharArray jPin)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_CHAR_PTR ckpPin = NULL_PTR;
    CK_ULONG ckPinLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jCharArrayToCKCharArray(env, jPin, &ckpPin, &ckPinLength);
    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_InitPIN)(ckSessionHandle, ckpPin, ckPinLength);

    free(ckpPin);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_SETPIN
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SetPIN
 * Signature: (J[C[C)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jcharArray jOldPin          CK_CHAR_PTR pOldPin
 *                                      CK_ULONG ulOldLen
 * @param   jcharArray jNewPin          CK_CHAR_PTR pNewPin
 *                                      CK_ULONG ulNewLen
 */
JNIEXPORT void JNICALL
Java_sun_security_pkcs11_wrapper_PKCS11_C_1SetPIN
(JNIEnv *env, jobject obj, jlong jSessionHandle, jcharArray jOldPin,
jcharArray jNewPin)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_CHAR_PTR ckpOldPin = NULL_PTR;
    CK_CHAR_PTR ckpNewPin = NULL_PTR;
    CK_ULONG ckOldPinLength;
    CK_ULONG ckNewPinLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jCharArrayToCKCharArray(env, jOldPin, &ckpOldPin, &ckOldPinLength);
    if ((*env)->ExceptionCheck(env)) { return; }
    jCharArrayToCKCharArray(env, jNewPin, &ckpNewPin, &ckNewPinLength);
    if ((*env)->ExceptionCheck(env)) {
        free(ckpOldPin);
        return;
    }

    rv = (*ckpFunctions->C_SetPIN)(ckSessionHandle, ckpOldPin, ckOldPinLength,
                                   ckpNewPin, ckNewPinLength);

    free(ckpOldPin);
    free(ckpNewPin);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif
