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

/* The list of notify callback handles that are currently active and waiting
 * for callbacks from their sessions.
 */
#ifndef NO_CALLBACKS
NotifyListNode *notifyListHead = NULL;
jobject notifyListLock = NULL;
#endif /* NO_CALLBACKS */

#ifdef P11_ENABLE_C_OPENSESSION
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_OpenSession
 * Signature: (JJLjava/lang/Object;Lsun/security/pkcs11/wrapper/CK_NOTIFY;)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jlong jFlags                CK_FLAGS flags
 * @param   jobject jApplication        CK_VOID_PTR pApplication
 * @param   jobject jNotify             CK_NOTIFY Notify
 * @return  jlong jSessionHandle        CK_SESSION_HANDLE_PTR phSession
 */
JNIEXPORT jlong JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1OpenSession
    (JNIEnv *env, jobject obj, jlong jSlotID, jlong jFlags, jobject jApplication, jobject jNotify)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_SLOT_ID ckSlotID;
    CK_FLAGS ckFlags;
    CK_VOID_PTR ckpApplication;
    CK_NOTIFY ckNotify;
    jlong jSessionHandle;
    CK_RV rv;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation = NULL;
#endif /* NO_CALLBACKS */

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return 0L; }

    ckSlotID = jLongToCKULong(jSlotID);
    ckFlags = jLongToCKULong(jFlags);

#ifndef NO_CALLBACKS
    if (jNotify != NULL) {
        notifyEncapsulation = (NotifyEncapsulation *) malloc(sizeof(NotifyEncapsulation));
        if (notifyEncapsulation == NULL) {
            throwOutOfMemoryError(env, 0);
            return 0L;
        }
        notifyEncapsulation->jApplicationData = (jApplication != NULL)
                ? (*env)->NewGlobalRef(env, jApplication)
                : NULL;
        notifyEncapsulation->jNotifyObject = (*env)->NewGlobalRef(env, jNotify);
        ckpApplication = notifyEncapsulation;
        ckNotify = (CK_NOTIFY) &notifyCallback;
    } else {
        ckpApplication = NULL_PTR;
        ckNotify = NULL_PTR;
    }
#else
        ckpApplication = NULL_PTR;
        ckNotify = NULL_PTR;
#endif /* NO_CALLBACKS */

    TRACE0("DEBUG: C_OpenSession");
    TRACE1(", slotID=%lu", ckSlotID);
    TRACE1(", flags=%lu", (unsigned long) ckFlags);
    TRACE0(" ... ");

    rv = (*ckpFunctions->C_OpenSession)(ckSlotID, ckFlags, ckpApplication, ckNotify, &ckSessionHandle);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) {
#ifndef NO_CALLBACKS
        if (notifyEncapsulation != NULL) {
            if (notifyEncapsulation->jApplicationData != NULL) {
                (*env)->DeleteGlobalRef(env, jApplication);
            }
            (*env)->DeleteGlobalRef(env, jNotify);
            free(notifyEncapsulation);
        }
#endif /* NO_CALLBACKS */
        return 0L;
    }

    TRACE0("got session");
    TRACE1(", SessionHandle=%lu", (unsigned long) ckSessionHandle);
    TRACE0(" ... ");

    jSessionHandle = ckULongToJLong(ckSessionHandle);

#ifndef NO_CALLBACKS
    if (notifyEncapsulation != NULL) {
        /* store the notifyEncapsulation to enable later cleanup */
        putNotifyEntry(env, ckSessionHandle, notifyEncapsulation);
    }
#endif /* NO_CALLBACKS */

    TRACE0("FINISHED\n");

    return jSessionHandle ;
}
#endif

#ifdef P11_ENABLE_C_CLOSESESSION
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_CloseSession
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1CloseSession
    (JNIEnv *env, jobject obj, jlong jSessionHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation;
    jobject jApplicationData;
#endif /* NO_CALLBACKS */

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    rv = (*ckpFunctions->C_CloseSession)(ckSessionHandle);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }

#ifndef NO_CALLBACKS
    notifyEncapsulation = removeNotifyEntry(env, ckSessionHandle);

    if (notifyEncapsulation != NULL) {
        /* there was a notify object used with this session, now dump the
         * encapsulation object
         */
        (*env)->DeleteGlobalRef(env, notifyEncapsulation->jNotifyObject);
        jApplicationData = notifyEncapsulation->jApplicationData;
        if (jApplicationData != NULL) {
            (*env)->DeleteGlobalRef(env, jApplicationData);
        }
        free(notifyEncapsulation);
    }
#endif /* NO_CALLBACKS */

}
#endif

#ifdef P11_ENABLE_C_CLOSEALLSESSIONS
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_CloseAllSessions
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1CloseAllSessions
    (JNIEnv *env, jobject obj, jlong jSlotID)
{
    CK_SLOT_ID ckSlotID;
    CK_RV rv;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation;
    jobject jApplicationData;
#endif /* NO_CALLBACKS */

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_CloseAllSessions)(ckSlotID);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }

#ifndef NO_CALLBACKS
    /* Remove all notify callback helper objects. */
    while ((notifyEncapsulation = removeFirstNotifyEntry(env)) != NULL) {
        /* there was a notify object used with this session, now dump the
         * encapsulation object
         */
        (*env)->DeleteGlobalRef(env, notifyEncapsulation->jNotifyObject);
        jApplicationData = notifyEncapsulation->jApplicationData;
        if (jApplicationData != NULL) {
            (*env)->DeleteGlobalRef(env, jApplicationData);
        }
        free(notifyEncapsulation);
    }
#endif /* NO_CALLBACKS */
}
#endif

#ifdef P11_ENABLE_C_GETSESSIONINFO
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetSessionInfo
 * Signature: (J)Lsun/security/pkcs11/wrapper/CK_SESSION_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jobject jSessionInfo        CK_SESSION_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetSessionInfo
    (JNIEnv *env, jobject obj, jlong jSessionHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_SESSION_INFO ckSessionInfo;
    jobject jSessionInfo=NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    rv = (*ckpFunctions->C_GetSessionInfo)(ckSessionHandle, &ckSessionInfo);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jSessionInfo = ckSessionInfoPtrToJSessionInfo(env, &ckSessionInfo);
    }
    return jSessionInfo ;
}
#endif

#ifdef P11_ENABLE_C_GETOPERATIONSTATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_GetOperationState
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jState           CK_BYTE_PTR pOperationState
 *                                      CK_ULONG_PTR pulOperationStateLen
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1GetOperationState
    (JNIEnv *env, jobject obj, jlong jSessionHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpState;
    CK_ULONG ckStateLength;
    jbyteArray jState = NULL;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return NULL; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    rv = (*ckpFunctions->C_GetOperationState)(ckSessionHandle, NULL_PTR, &ckStateLength);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return NULL ; }

    ckpState = (CK_BYTE_PTR) malloc(ckStateLength);
    if (ckpState == NULL) {
        throwOutOfMemoryError(env, 0);
        return NULL;
    }

    rv = (*ckpFunctions->C_GetOperationState)(ckSessionHandle, ckpState, &ckStateLength);
    if (ckAssertReturnValueOK(env, rv) == CK_ASSERT_OK) {
        jState = ckByteArrayToJByteArray(env, ckpState, ckStateLength);
    }
    free(ckpState);

    return jState ;
}
#endif

#ifdef P11_ENABLE_C_SETOPERATIONSTATE
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_SetOperationState
 * Signature: (J[BJJ)V
 * Parametermapping:                        *PKCS11*
 * @param   jlong jSessionHandle            CK_SESSION_HANDLE hSession
 * @param   jbyteArray jOperationState      CK_BYTE_PTR pOperationState
 *                                          CK_ULONG ulOperationStateLen
 * @param   jlong jEncryptionKeyHandle      CK_OBJECT_HANDLE hEncryptionKey
 * @param   jlong jAuthenticationKeyHandle  CK_OBJECT_HANDLE hAuthenticationKey
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1SetOperationState
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jbyteArray jOperationState, jlong jEncryptionKeyHandle, jlong jAuthenticationKeyHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpState = NULL_PTR;
    CK_ULONG ckStateLength;
    CK_OBJECT_HANDLE ckEncryptionKeyHandle;
    CK_OBJECT_HANDLE ckAuthenticationKeyHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    jByteArrayToCKByteArray(env, jOperationState, &ckpState, &ckStateLength);
    if ((*env)->ExceptionCheck(env)) { return; }

    ckEncryptionKeyHandle = jLongToCKULong(jEncryptionKeyHandle);
    ckAuthenticationKeyHandle = jLongToCKULong(jAuthenticationKeyHandle);

    rv = (*ckpFunctions->C_SetOperationState)(ckSessionHandle, ckpState, ckStateLength, ckEncryptionKeyHandle, ckAuthenticationKeyHandle);

    free(ckpState);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_LOGIN
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Login
 * Signature: (JJ[C)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jUserType             CK_USER_TYPE userType
 * @param   jcharArray jPin             CK_CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1Login
    (JNIEnv *env, jobject obj, jlong jSessionHandle, jlong jUserType, jcharArray jPin)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_USER_TYPE ckUserType;
    CK_CHAR_PTR ckpPinArray = NULL_PTR;
    CK_ULONG ckPinLength;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckUserType = jLongToCKULong(jUserType);
    jCharArrayToCKCharArray(env, jPin, &ckpPinArray, &ckPinLength);
    if ((*env)->ExceptionCheck(env)) { return; }

    rv = (*ckpFunctions->C_Login)(ckSessionHandle, ckUserType, ckpPinArray, ckPinLength);

    free(ckpPinArray);

    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

#ifdef P11_ENABLE_C_LOGOUT
/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    C_Logout
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_C_1Logout
    (JNIEnv *env, jobject obj, jlong jSessionHandle)
{
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;

    CK_FUNCTION_LIST_PTR ckpFunctions = getFunctionList(env, obj);
    if (ckpFunctions == NULL) { return; }

    ckSessionHandle = jLongToCKULong(jSessionHandle);

    rv = (*ckpFunctions->C_Logout)(ckSessionHandle);
    if (ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}
#endif

/* ************************************************************************** */
/* Functions for keeping track of notify callbacks                            */
/* ************************************************************************** */

#ifndef NO_CALLBACKS

/*
 * Add the given notify encapsulation object to the list of active notify
 * objects.
 * If notifyEncapsulation is NULL, this function does nothing.
 */
void putNotifyEntry(JNIEnv *env, CK_SESSION_HANDLE hSession, NotifyEncapsulation *notifyEncapsulation) {
    NotifyListNode *currentNode, *newNode;

    if (notifyEncapsulation == NULL) {
        return;
    }

    newNode = (NotifyListNode *) malloc(sizeof(NotifyListNode));
    if (newNode == NULL) {
        throwOutOfMemoryError(env, 0);
        return;
    }
    newNode->hSession = hSession;
    newNode->notifyEncapsulation = notifyEncapsulation;
    newNode->next = NULL;

    (*env)->MonitorEnter(env, notifyListLock); /* synchronize access to list */

    if (notifyListHead == NULL) {
        /* this is the first entry */
        notifyListHead = newNode;
    } else {
        /* go to the last entry; i.e. the first node which's 'next' is NULL.
         */
        currentNode = notifyListHead;
        while (currentNode->next != NULL) {
            currentNode = currentNode->next;
        }
        currentNode->next = newNode;
    }

    (*env)->MonitorExit(env, notifyListLock); /* synchronize access to list */
}

/*
 * Removes the active notifyEncapsulation object used with the given session and
 * returns it. If there is no notifyEncapsulation active for this session, this
 * function returns NULL.
 */
NotifyEncapsulation * removeNotifyEntry(JNIEnv *env, CK_SESSION_HANDLE hSession) {
    NotifyEncapsulation *notifyEncapsulation;
    NotifyListNode *currentNode, *previousNode;

    (*env)->MonitorEnter(env, notifyListLock); /* synchronize access to list */

    if (notifyListHead == NULL) {
        /* this is the first entry */
        notifyEncapsulation = NULL;
    } else {
        /* Find the node with the wanted session handle. Also stop, when we reach
         * the last entry; i.e. the first node which's 'next' is NULL.
         */
        currentNode = notifyListHead;
        previousNode = NULL;

        while ((currentNode->hSession != hSession) && (currentNode->next != NULL)) {
            previousNode = currentNode;
            currentNode = currentNode->next;
        }

        if (currentNode->hSession == hSession) {
            /* We found a entry for the wanted session, now remove it. */
            if (previousNode == NULL) {
                /* it's the first node */
                notifyListHead = currentNode->next;
            } else {
                previousNode->next = currentNode->next;
            }
            notifyEncapsulation = currentNode->notifyEncapsulation;
            free(currentNode);
        } else {
            /* We did not find a entry for this session */
            notifyEncapsulation = NULL;
        }
    }

    (*env)->MonitorExit(env, notifyListLock); /* synchronize access to list */

    return notifyEncapsulation ;
}

/*

 * Removes the first notifyEncapsulation object. If there is no notifyEncapsulation,
 * this function returns NULL.
 */
NotifyEncapsulation * removeFirstNotifyEntry(JNIEnv *env) {
    NotifyEncapsulation *notifyEncapsulation;
    NotifyListNode *currentNode;

    (*env)->MonitorEnter(env, notifyListLock); /* synchronize access to list */

    if (notifyListHead == NULL) {
        /* this is the first entry */
        notifyEncapsulation = NULL;
    } else {
        /* Remove the first entry. */
        currentNode = notifyListHead;
        notifyListHead = notifyListHead->next;
        notifyEncapsulation = currentNode->notifyEncapsulation;
        free(currentNode);
    }

    (*env)->MonitorExit(env, notifyListLock); /* synchronize access to list */

    return notifyEncapsulation ;
}

#endif /* NO_CALLBACKS */

#ifndef NO_CALLBACKS

/*
 * The function handling notify callbacks. It casts the pApplication parameter
 * back to a NotifyEncapsulation structure and retrieves the Notify object and
 * the application data from it.
 *
 * @param hSession The session, this callback is comming from.
 * @param event The type of event that occurred.
 * @param pApplication The application data as passed in upon OpenSession. In
                       this wrapper we always pass in a NotifyEncapsulation
                       object, which holds necessary information for delegating
                       the callback to the Java VM.
 * @return
 */
CK_RV notifyCallback(
    CK_SESSION_HANDLE hSession,     /* the session's handle */
    CK_NOTIFICATION   event,
    CK_VOID_PTR       pApplication  /* passed to C_OpenSession */
)
{
    NotifyEncapsulation *notifyEncapsulation;
    extern JavaVM *jvm;
    JNIEnv *env;
    jint returnValue;
    jlong jSessionHandle;
    jlong jEvent;
    jclass ckNotifyClass;
    jmethodID jmethod;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;

    if (pApplication == NULL) { return rv ; } /* This should not occur in this wrapper. */

    notifyEncapsulation = (NotifyEncapsulation *) pApplication;

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

    jSessionHandle = ckULongToJLong(hSession);
    jEvent = ckULongToJLong(event);

    ckNotifyClass = (*env)->FindClass(env, CLASS_NOTIFY);
    if (ckNotifyClass == NULL) { return rv; }
    jmethod = (*env)->GetMethodID(env, ckNotifyClass, "CK_NOTIFY", "(JJLjava/lang/Object;)V");
    if (jmethod == NULL) { return rv; }

    (*env)->CallVoidMethod(env, notifyEncapsulation->jNotifyObject, jmethod,
                         jSessionHandle, jEvent, notifyEncapsulation->jApplicationData);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL) {
        /* TBD: clear the pending exception with ExceptionClear? */
        /* The was an exception thrown, now we get the error-code from it */
        pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
        if (pkcs11ExceptionClass == NULL) { return rv; }

        jmethod = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
        if (jmethod == NULL) { return rv; }

        errorCode = (*env)->CallLongMethod(env, pkcs11Exception, jmethod);
        rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
        returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv ;
}

#endif /* NO_CALLBACKS */
