/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* disable asserts in product mode */
#ifndef DEBUG
  #ifndef NDEBUG
    #define NDEBUG
  #endif
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <winscard.h>

// #define J2PCSC_DEBUG

#ifdef J2PCSC_DEBUG
#define dprintf(s) printf(s)
#define dprintf1(s, p1) printf(s, p1)
#define dprintf2(s, p1, p2) printf(s, p1, p2)
#define dprintf3(s, p1, p2, p3) printf(s, p1, p2, p3)
#else
#define dprintf(s)
#define dprintf1(s, p1)
#define dprintf2(s, p1, p2)
#define dprintf3(s, p1, p2, p3)
#endif

#include "sun_security_smartcardio_PCSC.h"

#include "pcsc_md.h"

#include "jni_util.h"

#define MAX_STACK_BUFFER_SIZE 8192

// make the buffers larger than what should be necessary, just in case
#define ATR_BUFFER_SIZE 128
#define READERNAME_BUFFER_SIZE 128
#define RECEIVE_BUFFER_SIZE MAX_STACK_BUFFER_SIZE

#define J2PCSC_EXCEPTION_NAME "sun/security/smartcardio/PCSCException"

void throwOutOfMemoryError(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");

    if (cls != NULL) /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);

}

void throwPCSCException(JNIEnv* env, LONG code) {
    jclass pcscClass;
    jmethodID constructor;
    jthrowable pcscException;

    pcscClass = (*env)->FindClass(env, J2PCSC_EXCEPTION_NAME);
    if (pcscClass == NULL) {
        return;
    }
    constructor = (*env)->GetMethodID(env, pcscClass, "<init>", "(I)V");
    if (constructor == NULL) {
        return;
    }
    pcscException = (jthrowable) (*env)->NewObject(env, pcscClass,
        constructor, (jint)code);
    if (pcscException != NULL) {
        (*env)->Throw(env, pcscException);
    }
}

jboolean handleRV(JNIEnv* env, LONG code) {
    if (code == SCARD_S_SUCCESS) {
        return JNI_FALSE;
    } else {
        throwPCSCException(env, code);
        return JNI_TRUE;
    }
}

JNIEXPORT jint JNICALL DEF_JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_4;
}

JNIEXPORT jlong JNICALL Java_sun_security_smartcardio_PCSC_SCardEstablishContext
    (JNIEnv *env, jclass thisClass, jint dwScope)
{
    SCARDCONTEXT context = 0;
    LONG rv;
    dprintf("-establishContext\n");
    rv = CALL_SCardEstablishContext(dwScope, NULL, NULL, &context);
    if (handleRV(env, rv)) {
        return 0;
    }
    // note: SCARDCONTEXT is typedef'd as long, so this works
    return (jlong)context;
}

/**
 * Convert a multi string to a java string array,
 */
jobjectArray pcsc_multi2jstring(JNIEnv *env, char *spec) {
    jobjectArray result;
    jclass stringClass;
    char *cp, **tab = NULL;
    jstring js;
    int cnt = 0;

    cp = spec;
    while (*cp != 0) {
        cp += (strlen(cp) + 1);
        ++cnt;
    }

    tab = (char **)malloc(cnt * sizeof(char *));
    if (tab == NULL) {
        throwOutOfMemoryError(env, NULL);
        return NULL;
    }

    cnt = 0;
    cp = spec;
    while (*cp != 0) {
        tab[cnt++] = cp;
        cp += (strlen(cp) + 1);
    }

    stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL) {
        free(tab);
        return NULL;
    }

    result = (*env)->NewObjectArray(env, cnt, stringClass, NULL);
    if (result != NULL) {
        while (cnt-- > 0) {
            js = (*env)->NewStringUTF(env, tab[cnt]);
            if ((*env)->ExceptionCheck(env)) {
                free(tab);
                return NULL;
            }
            (*env)->SetObjectArrayElement(env, result, cnt, js);
            if ((*env)->ExceptionCheck(env)) {
                free(tab);
                return NULL;
            }
            (*env)->DeleteLocalRef(env, js);
        }
    }
    free(tab);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_sun_security_smartcardio_PCSC_SCardListReaders
    (JNIEnv *env, jclass thisClass, jlong jContext)
{
    SCARDCONTEXT context = (SCARDCONTEXT)jContext;
    LONG rv;
    LPSTR mszReaders = NULL;
    DWORD size = 0;
    jobjectArray result;

    dprintf1("-context: %x\n", context);
    rv = CALL_SCardListReaders(context, NULL, NULL, &size);
    if (handleRV(env, rv)) {
        return NULL;
    }
    dprintf1("-size: %d\n", size);

    if (size != 0) {
        mszReaders = malloc(size);
        if (mszReaders == NULL) {
            throwOutOfMemoryError(env, NULL);
            return NULL;
        }

        rv = CALL_SCardListReaders(context, NULL, mszReaders, &size);
        if (handleRV(env, rv)) {
            free(mszReaders);
            return NULL;
        }
        dprintf1("-String: %s\n", mszReaders);
    } else {
      return NULL;
    }

    result = pcsc_multi2jstring(env, mszReaders);
    free(mszReaders);
    return result;
}

JNIEXPORT jlong JNICALL Java_sun_security_smartcardio_PCSC_SCardConnect
    (JNIEnv *env, jclass thisClass, jlong jContext, jstring jReaderName,
    jint jShareMode, jint jPreferredProtocols)
{
    SCARDCONTEXT context = (SCARDCONTEXT)jContext;
    LONG rv;
    LPCSTR readerName;
    SCARDHANDLE card = 0;
    DWORD proto = 0;

    readerName = (*env)->GetStringUTFChars(env, jReaderName, NULL);
    if (readerName == NULL) {
        return 0;
    }
    rv = CALL_SCardConnect(context, readerName, jShareMode, jPreferredProtocols, &card, &proto);
    (*env)->ReleaseStringUTFChars(env, jReaderName, readerName);
    dprintf1("-cardhandle: %x\n", card);
    dprintf1("-protocol: %d\n", proto);
    if (handleRV(env, rv)) {
        return 0;
    }

    return (jlong)card;
}

JNIEXPORT jbyteArray JNICALL Java_sun_security_smartcardio_PCSC_SCardTransmit
    (JNIEnv *env, jclass thisClass, jlong jCard, jint protocol,
    jbyteArray jBuf, jint jOfs, jint jLen)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;
    SCARD_IO_REQUEST sendPci;
    unsigned char *sbuf;
    unsigned char rbuf[RECEIVE_BUFFER_SIZE];
    DWORD rlen = RECEIVE_BUFFER_SIZE;
    int ofs = (int)jOfs;
    int len = (int)jLen;
    jbyteArray jOut;

    sendPci.dwProtocol = protocol;
    sendPci.cbPciLength = sizeof(SCARD_IO_REQUEST);

    sbuf = (unsigned char *) ((*env)->GetByteArrayElements(env, jBuf, NULL));
    if (sbuf == NULL) {
        return NULL;
    }
    rv = CALL_SCardTransmit(card, &sendPci, sbuf + ofs, len, NULL, rbuf, &rlen);
    (*env)->ReleaseByteArrayElements(env, jBuf, (jbyte *)sbuf, JNI_ABORT);

    if (handleRV(env, rv)) {
        return NULL;
    }

    jOut = (*env)->NewByteArray(env, rlen);
    if (jOut != NULL) {
        (*env)->SetByteArrayRegion(env, jOut, 0, rlen, (jbyte *)rbuf);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
    }
    return jOut;
}

JNIEXPORT jbyteArray JNICALL Java_sun_security_smartcardio_PCSC_SCardStatus
    (JNIEnv *env, jclass thisClass, jlong jCard, jbyteArray jStatus)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;
    char readerName[READERNAME_BUFFER_SIZE];
    DWORD readerLen = READERNAME_BUFFER_SIZE;
    unsigned char atr[ATR_BUFFER_SIZE];
    DWORD atrLen = ATR_BUFFER_SIZE;
    DWORD state = 0;
    DWORD protocol = 0;
    jbyteArray jArray;
    jbyte status[2];

    rv = CALL_SCardStatus(card, readerName, &readerLen, &state, &protocol, atr, &atrLen);
    if (handleRV(env, rv)) {
        return NULL;
    }
    dprintf1("-reader: %s\n", readerName);
    dprintf1("-status: %d\n", state);
    dprintf1("-protocol: %d\n", protocol);

    jArray = (*env)->NewByteArray(env, atrLen);
    if (jArray == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, jArray, 0, atrLen, (jbyte *)atr);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    status[0] = (jbyte) state;
    status[1] = (jbyte) protocol;
    (*env)->SetByteArrayRegion(env, jStatus, 0, 2, status);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return jArray;
}

JNIEXPORT void JNICALL Java_sun_security_smartcardio_PCSC_SCardDisconnect
    (JNIEnv *env, jclass thisClass, jlong jCard, jint jDisposition)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;

    rv = CALL_SCardDisconnect(card, jDisposition);
    dprintf1("-disconnect: 0x%X\n", rv);
    handleRV(env, rv);
    return;
}

JNIEXPORT jintArray JNICALL Java_sun_security_smartcardio_PCSC_SCardGetStatusChange
    (JNIEnv *env, jclass thisClass, jlong jContext, jlong jTimeout,
    jintArray jCurrentState, jobjectArray jReaderNames)
{
    SCARDCONTEXT context = (SCARDCONTEXT)jContext;
    LONG rv;
    int readers = (*env)->GetArrayLength(env, jReaderNames);
    SCARD_READERSTATE *readerState;
    int i;
    jintArray jEventState = NULL;
    int *currentState = NULL;
    const char *readerName;

    readerState = calloc(readers, sizeof(SCARD_READERSTATE));
    if (readerState == NULL && readers > 0) {
        throwOutOfMemoryError(env, NULL);
        return NULL;
    }

    currentState = (*env)->GetIntArrayElements(env, jCurrentState, NULL);
    if (currentState == NULL) {
        free(readerState);
        return NULL;
    }

    for (i = 0; i < readers; i++) {
        readerState[i].szReader = NULL;
    }

    for (i = 0; i < readers; i++) {
        jobject jReaderName = (*env)->GetObjectArrayElement(env, jReaderNames, i);
        if ((*env)->ExceptionCheck(env)) {
            goto cleanup;
        }
        readerName = (*env)->GetStringUTFChars(env, jReaderName, NULL);
        if (readerName == NULL) {
            goto cleanup;
        }
        readerState[i].szReader = strdup(readerName);
        (*env)->ReleaseStringUTFChars(env, jReaderName, readerName);
        if (readerState[i].szReader == NULL) {
            throwOutOfMemoryError(env, NULL);
            goto cleanup;
        }
        readerState[i].pvUserData = NULL;
        readerState[i].dwCurrentState = currentState[i];
        readerState[i].dwEventState = SCARD_STATE_UNAWARE;
        readerState[i].cbAtr = 0;
        (*env)->DeleteLocalRef(env, jReaderName);
    }

    if (readers > 0) {
        rv = CALL_SCardGetStatusChange(context, (DWORD)jTimeout, readerState, readers);
        if (handleRV(env, rv)) {
            goto cleanup;
        }
    }

    jEventState = (*env)->NewIntArray(env, readers);
    if (jEventState == NULL) {
        goto cleanup;
    }
    for (i = 0; i < readers; i++) {
        jint eventStateTmp;
        dprintf3("-reader status %s: 0x%X, 0x%X\n", readerState[i].szReader,
            readerState[i].dwCurrentState, readerState[i].dwEventState);
        eventStateTmp = (jint)readerState[i].dwEventState;
        (*env)->SetIntArrayRegion(env, jEventState, i, 1, &eventStateTmp);
        if ((*env)->ExceptionCheck(env)) {
            jEventState = NULL;
            goto cleanup;
        }
    }
cleanup:
    (*env)->ReleaseIntArrayElements(env, jCurrentState, currentState, JNI_ABORT);
    for (i = 0; i < readers; i++) {
        free((char *)readerState[i].szReader);
    }
    free(readerState);
    return jEventState;
}

JNIEXPORT void JNICALL Java_sun_security_smartcardio_PCSC_SCardBeginTransaction
    (JNIEnv *env, jclass thisClass, jlong jCard)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;

    rv = CALL_SCardBeginTransaction(card);
    dprintf1("-beginTransaction: 0x%X\n", rv);
    handleRV(env, rv);
    return;
}

JNIEXPORT void JNICALL Java_sun_security_smartcardio_PCSC_SCardEndTransaction
    (JNIEnv *env, jclass thisClass, jlong jCard, jint jDisposition)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;

    rv = CALL_SCardEndTransaction(card, jDisposition);
    dprintf1("-endTransaction: 0x%X\n", rv);
    handleRV(env, rv);
    return;
}

JNIEXPORT jbyteArray JNICALL Java_sun_security_smartcardio_PCSC_SCardControl
    (JNIEnv *env, jclass thisClass, jlong jCard, jint jControlCode, jbyteArray jSendBuffer)
{
    SCARDHANDLE card = (SCARDHANDLE)jCard;
    LONG rv;
    jbyte* sendBuffer;
    jint sendBufferLength = (*env)->GetArrayLength(env, jSendBuffer);
    jbyte receiveBuffer[MAX_STACK_BUFFER_SIZE];
    jint receiveBufferLength = MAX_STACK_BUFFER_SIZE;
    ULONG returnedLength = 0;
    jbyteArray jReceiveBuffer;

    sendBuffer = (*env)->GetByteArrayElements(env, jSendBuffer, NULL);
    if (sendBuffer == NULL) {
        return NULL;
    }

#ifdef J2PCSC_DEBUG
{
    int k;
    printf("-control: 0x%X\n", jControlCode);
    printf("-send: ");
    for (k = 0; k < sendBufferLength; k++) {
        printf("%02x ", sendBuffer[k]);
    }
    printf("\n");
}
#endif

    rv = CALL_SCardControl(card, jControlCode, sendBuffer, sendBufferLength,
        receiveBuffer, receiveBufferLength, &returnedLength);

    (*env)->ReleaseByteArrayElements(env, jSendBuffer, sendBuffer, JNI_ABORT);
    if (handleRV(env, rv)) {
        return NULL;
    }

#ifdef J2PCSC_DEBUG
{
    int k;
    printf("-recv:  ");
    for (k = 0; k < returnedLength; k++) {
        printf("%02x ", receiveBuffer[k]);
    }
    printf("\n");
}
#endif

    jReceiveBuffer = (*env)->NewByteArray(env, returnedLength);
    if (jReceiveBuffer == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, jReceiveBuffer, 0, returnedLength, receiveBuffer);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return jReceiveBuffer;
}
