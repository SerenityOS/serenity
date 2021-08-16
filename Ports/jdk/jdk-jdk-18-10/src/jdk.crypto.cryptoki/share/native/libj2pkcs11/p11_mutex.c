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

#include "sun_security_pkcs11_wrapper_PKCS11.h"

/* The initArgs that enable the application to do custom mutex-handling */
#ifndef NO_CALLBACKS
jobject jInitArgsObject;
CK_C_INITIALIZE_ARGS_PTR ckpGlobalInitArgs;
#endif /* NO_CALLBACKS */

/* ************************************************************************** */
/* Now come the functions for mutex handling and notification callbacks       */
/* ************************************************************************** */

/*
 * converts the InitArgs object to a CK_C_INITIALIZE_ARGS structure and sets the functions
 * that will call the right Java mutex functions
 *
 * @param env - used to call JNI funktions to get the Java classes, objects, methods and fields
 * @param pInitArgs - the InitArgs object with the Java mutex functions to call
 * @return - the pointer to the CK_C_INITIALIZE_ARGS structure with the functions that will call
 *           the corresponding Java functions
 */
CK_C_INITIALIZE_ARGS_PTR makeCKInitArgsAdapter(JNIEnv *env, jobject jInitArgs)
{
    CK_C_INITIALIZE_ARGS_PTR ckpInitArgs;
    jclass jInitArgsClass;
    jfieldID fieldID;
    jlong jFlags;
    jobject jReserved;
    CK_ULONG ckReservedLength;
#ifndef NO_CALLBACKS
    jobject jMutexHandler;
#endif /* NO_CALLBACKS */

    if(jInitArgs == NULL) {
        return NULL_PTR;
    }

    /* convert the Java InitArgs object to a pointer to a CK_C_INITIALIZE_ARGS structure */
    ckpInitArgs = (CK_C_INITIALIZE_ARGS_PTR) malloc(sizeof(CK_C_INITIALIZE_ARGS));
    if (ckpInitArgs == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL_PTR;
    }
    ckpInitArgs->flags = (CK_FLAGS)0;
    ckpInitArgs->pReserved = (CK_VOID_PTR)NULL;

    /* Set the mutex functions that will call the Java mutex functions, but
     * only set it, if the field is not null.
     */
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    if (jInitArgsClass == NULL) {
        free(ckpInitArgs);
        return NULL;
    }

#ifdef NO_CALLBACKS
    ckpInitArgs->CreateMutex = NULL_PTR;
    ckpInitArgs->DestroyMutex = NULL_PTR;
    ckpInitArgs->LockMutex = NULL_PTR;
    ckpInitArgs->UnlockMutex = NULL_PTR;
#else
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "CreateMutex", "Lsun/security/pkcs11/wrapper/CK_CREATEMUTEX;");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->CreateMutex = (jMutexHandler != NULL) ? &callJCreateMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "DestroyMutex", "Lsun/security/pkcs11/wrapper/CK_DESTROYMUTEX;");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->DestroyMutex = (jMutexHandler != NULL) ? &callJDestroyMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "LockMutex", "Lsun/security/pkcs11/wrapper/CK_LOCKMUTEX;");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->LockMutex = (jMutexHandler != NULL) ? &callJLockMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "UnlockMutex", "Lsun/security/pkcs11/wrapper/CK_UNLOCKMUTEX;");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->UnlockMutex = (jMutexHandler != NULL) ? &callJUnlockMutex : NULL_PTR;

    if ((ckpInitArgs->CreateMutex != NULL_PTR)
            || (ckpInitArgs->DestroyMutex != NULL_PTR)
            || (ckpInitArgs->LockMutex != NULL_PTR)
            || (ckpInitArgs->UnlockMutex != NULL_PTR)) {
        /* we only need to keep a global copy, if we need callbacks */
        /* set the global object jInitArgs so that the right Java mutex functions will be called */
        jInitArgsObject = (*env)->NewGlobalRef(env, jInitArgs);
        ckpGlobalInitArgs = (CK_C_INITIALIZE_ARGS_PTR) malloc(sizeof(CK_C_INITIALIZE_ARGS));
        if (ckpGlobalInitArgs == NULL) {
            free(ckpInitArgs);
            throwOutOfMemoryError(env, 0);
            return NULL_PTR;
        }

        memcpy(ckpGlobalInitArgs, ckpInitArgs, sizeof(CK_C_INITIALIZE_ARGS));
    }
#endif /* NO_CALLBACKS */

    /* convert and set the flags field */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "flags", "J");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jFlags = (*env)->GetLongField(env, jInitArgs, fieldID);
    ckpInitArgs->flags = jLongToCKULong(jFlags);

    /* pReserved should be NULL_PTR in this version */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "pReserved", "Ljava/lang/Object;");
    if (fieldID == NULL) {
        free(ckpInitArgs);
        return NULL;
    }
    jReserved = (*env)->GetObjectField(env, jInitArgs, fieldID);

    /* we try to convert the reserved parameter also */
    ckpInitArgs->pReserved = jObjectToPrimitiveCKObjectPtr(env, jReserved, &ckReservedLength);

    return ckpInitArgs ;
}

#ifndef NO_CALLBACKS

/*
 * is the function that gets called by PKCS#11 to create a mutex and calls the Java
 * CreateMutex function
 *
 * @param env - used to call JNI funktions to get the Java classes, objects, methods and fields
 * @param ppMutex - the new created mutex
 * @return - should return CKR_OK if the mutex creation was ok
 */
CK_RV callJCreateMutex(CK_VOID_PTR_PTR ppMutex)
{
    extern JavaVM *jvm;
    JNIEnv *env;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jCreateMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jCreateMutex;
    jobject jMutex;


    /* Get the currently running Java VM */
    if (jvm == NULL) { return rv ;} /* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
        /* thread detached, so attach it */
        wasAttached = 0;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else if (returnValue == JNI_EVERSION) {
        /* this version of JNI is not supported, so just try to attach */
        /* we assume it was attached to ensure that this thread is not detached
         * afterwards even though it should not
         */
        wasAttached = 1;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else {
        /* attached */
        wasAttached = 1;
    }

    jCreateMutexClass = (*env)->FindClass(env, CLASS_CREATEMUTEX);
    if (jCreateMutexClass == NULL) { return rv; }
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    if (jInitArgsClass == NULL) { return rv; }

    /* get the CreateMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "CreateMutex", "Lsun/security/pkcs11/wrapper/CK_CREATEMUTEX;");
    if (fieldID == NULL) { return rv; }
    jCreateMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jCreateMutex != 0);

    /* call the CK_CREATEMUTEX function of the CreateMutex object */
    /* and get the new Java mutex object */
    methodID = (*env)->GetMethodID(env, jCreateMutexClass, "CK_CREATEMUTEX", "()Ljava/lang/Object;");
    if (methodID == NULL) { return rv; }
    jMutex = (*env)->CallObjectMethod(env, jCreateMutex, methodID);

    /* set a global reference on the Java mutex */
    jMutex = (*env)->NewGlobalRef(env, jMutex);
    /* convert the Java mutex to a CK mutex */
    *ppMutex = jObjectToCKVoidPtr(jMutex);


    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL) {
        /* TBD: clear the pending exception with ExceptionClear? */
        /* The was an exception thrown, now we get the error-code from it */
        pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (pkcs11ExceptionClass == NULL) { return rv; }
        methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
        if (methodID == NULL) { return rv; }

        errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
        rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
        returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv ;
}

/*
 * is the function that gets called by PKCS#11 to destroy a mutex and calls the Java
 * DestroyMutex function
 *
 * @param env - used to call JNI funktions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to destroy
 * @return - should return CKR_OK if the mutex was destroyed
 */
CK_RV callJDestroyMutex(CK_VOID_PTR pMutex)
{
    extern JavaVM *jvm;
    JNIEnv *env;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jDestroyMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jDestroyMutex;
    jobject jMutex;


    /* Get the currently running Java VM */
    if (jvm == NULL) { return rv ; } /* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
        /* thread detached, so attach it */
        wasAttached = 0;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else if (returnValue == JNI_EVERSION) {
        /* this version of JNI is not supported, so just try to attach */
        /* we assume it was attached to ensure that this thread is not detached
         * afterwards even though it should not
         */
        wasAttached = 1;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else {
        /* attached */
        wasAttached = 1;
    }

    jDestroyMutexClass = (*env)->FindClass(env, CLASS_DESTROYMUTEX);
    if (jDestroyMutexClass == NULL) { return rv; }
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    if (jInitArgsClass == NULL) { return rv; }

    /* convert the CK mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the DestroyMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "DestroyMutex", "Lsun/security/pkcs11/wrapper/CK_DESTROYMUTEX;");
    if (fieldID == NULL) { return rv; }
    jDestroyMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jDestroyMutex != 0);

    /* call the CK_DESTROYMUTEX method of the DestroyMutex object */
    methodID = (*env)->GetMethodID(env, jDestroyMutexClass, "CK_DESTROYMUTEX", "(Ljava/lang/Object;)V");
    if (methodID == NULL) { return rv; }
    (*env)->CallVoidMethod(env, jDestroyMutex, methodID, jMutex);

    /* delete the global reference on the Java mutex */
    (*env)->DeleteGlobalRef(env, jMutex);


    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL) {
        /* TBD: clear the pending exception with ExceptionClear? */
        /* The was an exception thrown, now we get the error-code from it */
        pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (pkcs11ExceptionClass == NULL) { return rv; }
        methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
        if (methodID == NULL) { return rv; }
        errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
        rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
        returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv ;
}

/*
 * is the function that gets called by PKCS#11 to lock a mutex and calls the Java
 * LockMutex function
 *
 * @param env - used to call JNI funktions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to lock
 * @return - should return CKR_OK if the mutex was not locked already
 */
CK_RV callJLockMutex(CK_VOID_PTR pMutex)
{
    extern JavaVM *jvm;
    JNIEnv *env;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jLockMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jLockMutex;
    jobject jMutex;


    /* Get the currently running Java VM */
    if (jvm == NULL) { return rv ; } /* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
        /* thread detached, so attach it */
        wasAttached = 0;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else if (returnValue == JNI_EVERSION) {
        /* this version of JNI is not supported, so just try to attach */
        /* we assume it was attached to ensure that this thread is not detached
         * afterwards even though it should not
         */
        wasAttached = 1;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else {
        /* attached */
        wasAttached = 1;
    }

    jLockMutexClass = (*env)->FindClass(env, CLASS_LOCKMUTEX);
    if (jLockMutexClass == NULL) { return rv; }
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    if (jInitArgsClass == NULL) { return rv; }

    /* convert the CK mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the LockMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "LockMutex", "Lsun/security/pkcs11/wrapper/CK_LOCKMUTEX;");
    if (fieldID == NULL) { return rv; }
    jLockMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jLockMutex != 0);

    /* call the CK_LOCKMUTEX method of the LockMutex object */
    methodID = (*env)->GetMethodID(env, jLockMutexClass, "CK_LOCKMUTEX", "(Ljava/lang/Object;)V");
    if (methodID == NULL) { return rv; }
    (*env)->CallVoidMethod(env, jLockMutex, methodID, jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL) {
        /* TBD: clear the pending exception with ExceptionClear? */
        /* The was an exception thrown, now we get the error-code from it */
        pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (pkcs11ExceptionClass == NULL) { return rv; }
        methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
        if (methodID == NULL) { return rv; }
        errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
        rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
        returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv ;
}

/*
 * is the function that gets called by PKCS#11 to unlock a mutex and calls the Java
 * UnlockMutex function
 *
 * @param env - used to call JNI funktions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to unlock
 * @return - should return CKR_OK if the mutex was not unlocked already
 */
CK_RV callJUnlockMutex(CK_VOID_PTR pMutex)
{
    extern JavaVM *jvm;
    JNIEnv *env;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jUnlockMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jUnlockMutex;
    jobject jMutex;


    /* Get the currently running Java VM */
    if (jvm == NULL) { return rv ; } /* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
        /* thread detached, so attach it */
        wasAttached = 0;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else if (returnValue == JNI_EVERSION) {
        /* this version of JNI is not supported, so just try to attach */
        /* we assume it was attached to ensure that this thread is not detached
         * afterwards even though it should not
         */
        wasAttached = 1;
        returnValue = (*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL);
    } else {
        /* attached */
        wasAttached = 1;
    }

    jUnlockMutexClass = (*env)->FindClass(env, CLASS_UNLOCKMUTEX);
    if (jUnlockMutexClass == NULL) { return rv; }
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    if (jInitArgsClass == NULL) { return rv; }

    /* convert the CK-type mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the UnlockMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "UnlockMutex", "Lsun/security/pkcs11/wrapper/CK_UNLOCKMUTEX;");
    if (fieldID == NULL) { return rv; }
    jUnlockMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jUnlockMutex != 0);

    /* call the CK_UNLOCKMUTEX method of the UnLockMutex object */
    methodID = (*env)->GetMethodID(env, jUnlockMutexClass, "CK_UNLOCKMUTEX", "(Ljava/lang/Object;)V");
    if (methodID == NULL) { return rv; }
    (*env)->CallVoidMethod(env, jUnlockMutex, methodID, jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL) {
        /* TBD: clear the pending exception with ExceptionClear? */
        /* The was an exception thrown, now we get the error-code from it */
        pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (pkcs11ExceptionClass == NULL) { return rv; }
        methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
        if (methodID == NULL) { return rv; }
        errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
        rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
        returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv ;
}

#endif /* NO_CALLBACKS */
