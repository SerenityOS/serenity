/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <windows.h>
#include <rpc.h>
#include <winsock.h>
#include <lm.h>

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>
#include <tchar.h>
#include <fcntl.h>

#include "jni_util.h"

#define SECURITY_WIN32
#include "sspi.h"

static void endSequence (PCredHandle credHand, PCtxtHandle ctxHandle, JNIEnv *env, jobject status);

static jfieldID ntlm_ctxHandleID;
static jfieldID ntlm_crdHandleID;
static jfieldID status_seqCompleteID;

static HINSTANCE lib = NULL;

JNIEXPORT void JNICALL Java_sun_net_www_protocol_http_ntlm_NTLMAuthSequence_initFirst
(JNIEnv *env, jclass authseq_clazz, jclass status_clazz)
{
    ntlm_ctxHandleID = (*env)->GetFieldID(env, authseq_clazz, "ctxHandle", "J");
    CHECK_NULL(ntlm_ctxHandleID);
    ntlm_crdHandleID = (*env)->GetFieldID(env, authseq_clazz, "crdHandle", "J");
    CHECK_NULL(ntlm_crdHandleID);
    status_seqCompleteID = (*env)->GetFieldID(env, status_clazz, "sequenceComplete", "Z");
}

/*
 * Class:     sun_net_www_protocol_http_NTLMAuthSequence
 * Method:    getCredentialsHandle
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J
 */

JNIEXPORT jlong JNICALL Java_sun_net_www_protocol_http_ntlm_NTLMAuthSequence_getCredentialsHandle
(JNIEnv *env, jobject this, jstring user, jstring domain, jstring password)
{
    SEC_WINNT_AUTH_IDENTITY   AuthId;
    SEC_WINNT_AUTH_IDENTITY * pAuthId;
    const CHAR        *pUser = 0;
    const CHAR        *pDomain = 0;
    const CHAR        *pPassword = 0;
    CredHandle      *pCred;
    TimeStamp            ltime;
    jboolean         isCopy;
    SECURITY_STATUS      ss;

    if (user != 0) {
        pUser = JNU_GetStringPlatformChars(env, user, &isCopy);
        if (pUser == NULL)
            return 0;  // pending Exception
    }
    if (domain != 0) {
        pDomain = JNU_GetStringPlatformChars(env, domain, &isCopy);
        if (pDomain == NULL) {
            if (pUser != NULL)
                JNU_ReleaseStringPlatformChars(env, user, pUser);
            return 0;  // pending Exception
        }
    }
    if (password != 0) {
        pPassword = JNU_GetStringPlatformChars(env, password, &isCopy);
        if (pPassword == NULL) {
            if(pUser != NULL)
                JNU_ReleaseStringPlatformChars(env, user, pUser);
            if(pDomain != NULL)
                JNU_ReleaseStringPlatformChars(env, domain, pDomain);
            return 0;  // pending Exception
        }
    }
    pCred = (CredHandle *)malloc(sizeof (CredHandle));
    if (pCred == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
        if (pUser != NULL)
            JNU_ReleaseStringPlatformChars(env, user, pUser);
        if (pPassword != NULL)
            JNU_ReleaseStringPlatformChars(env, password, pPassword);
        if (pDomain != NULL)
            JNU_ReleaseStringPlatformChars(env, domain, pDomain);
        return NULL;
    }

    if ( ((pUser != NULL) || (pPassword != NULL)) || (pDomain != NULL)) {
        pAuthId = &AuthId;

        memset( &AuthId, 0, sizeof( AuthId ));

        if ( pUser != NULL ) {
            AuthId.User       = (unsigned char *) pUser;
            AuthId.UserLength = (unsigned long) strlen( pUser );
        }

        if ( pPassword != NULL ) {
            AuthId.Password       = (unsigned char *) pPassword;
            AuthId.PasswordLength = (unsigned long) strlen( pPassword );
        }

        if ( pDomain != NULL ) {
            AuthId.Domain       = (unsigned char *) pDomain;
            AuthId.DomainLength = (unsigned long) strlen( pDomain );
        }

        AuthId.Flags = SEC_WINNT_AUTH_IDENTITY_ANSI;
    } else {
        pAuthId = NULL;
    }

    ss = AcquireCredentialsHandleA(
        NULL, "NTLM", SECPKG_CRED_OUTBOUND,
        NULL, pAuthId, NULL, NULL,
        pCred, &ltime
        );

    /* Release resources held by JNU_GetStringPlatformChars */
    if (pUser != NULL)
        JNU_ReleaseStringPlatformChars(env, user, pUser);
    if (pPassword != NULL)
        JNU_ReleaseStringPlatformChars(env, password, pPassword);
    if (pDomain != NULL)
        JNU_ReleaseStringPlatformChars(env, domain, pDomain);

    if (ss == 0) {
        return (jlong) pCred;
    } else {
        return 0;
    }
}


/*
 * Class:     sun_net_www_protocol_http_ntlm_NTLMAuthSequence
 * Method:    getNextToken
 * Signature: (J[BLsun/net/www/protocol/http/ntlm/NTLMAuthSequence/Status;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_net_www_protocol_http_ntlm_NTLMAuthSequence_getNextToken
(JNIEnv *env, jobject this, jlong crdHandle, jbyteArray lastToken, jobject status)
{

    VOID        *pInput = 0;
    DWORD            inputLen;
    CHAR         buffOut[1024];
    jboolean         isCopy;
    SECURITY_STATUS      ss;
    SecBufferDesc        OutBuffDesc;
    SecBuffer            OutSecBuff;
    SecBufferDesc        InBuffDesc;
    SecBuffer            InSecBuff;
    ULONG                ContextAttributes;
    CredHandle      *pCred = (CredHandle *)crdHandle;
    CtxtHandle      *pCtx;
    CtxtHandle      *newContext;
    TimeStamp            ltime;
    jbyteArray       result;


    pCtx = (CtxtHandle *) (*env)->GetLongField (env, this, ntlm_ctxHandleID);
    if (pCtx == 0) { /* first call */
        newContext = (CtxtHandle *)malloc(sizeof(CtxtHandle));
        if (newContext != NULL) {
            (*env)->SetLongField (env, this, ntlm_ctxHandleID, (jlong)newContext);
        } else {
            JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
            return NULL;
        }
    } else {
        newContext = pCtx;
    }

    OutBuffDesc.ulVersion = 0;
    OutBuffDesc.cBuffers  = 1;
    OutBuffDesc.pBuffers  = &OutSecBuff;

    OutSecBuff.cbBuffer   = 1024;
    OutSecBuff.BufferType = SECBUFFER_TOKEN;
    OutSecBuff.pvBuffer   = buffOut;

    /*
     *  Prepare our Input buffer - Note the server is expecting the client's
     *  negotiation packet on the first call
     */

    if (lastToken != 0)
    {
        pInput = (VOID *)(*env)->GetByteArrayElements(env, lastToken, &isCopy);
        CHECK_NULL_RETURN(pInput, NULL);
        inputLen = (*env)->GetArrayLength(env, lastToken);

        InBuffDesc.ulVersion = 0;
        InBuffDesc.cBuffers  = 1;
        InBuffDesc.pBuffers  = &InSecBuff;

        InSecBuff.cbBuffer       = inputLen;
        InSecBuff.BufferType = SECBUFFER_TOKEN;
        InSecBuff.pvBuffer       = pInput;
    }

    /*
     *  will return success when its done but we still
     *  need to send the out buffer if there are bytes to send
     */

    ss = InitializeSecurityContextA(
        pCred, pCtx, NULL, 0, 0, SECURITY_NATIVE_DREP,
        lastToken ? &InBuffDesc : NULL, 0, newContext, &OutBuffDesc,
        &ContextAttributes, &ltime
    );

    if (pInput != 0) {
        (*env)->ReleaseByteArrayElements(env, lastToken, pInput, JNI_ABORT);
    }

    if (ss < 0) {
        endSequence (pCred, pCtx, env, status);
        return 0;
    }

    if ((ss == SEC_I_COMPLETE_NEEDED) || (ss == SEC_I_COMPLETE_AND_CONTINUE) ) {
        ss = CompleteAuthToken( pCtx, &OutBuffDesc );

        if (ss < 0) {
            endSequence (pCred, pCtx, env, status);
            return 0;
        }
    }

    if ( OutSecBuff.cbBuffer > 0 ) {
        jbyteArray ret = (*env)->NewByteArray(env, OutSecBuff.cbBuffer);
        if (ret != NULL) {
            (*env)->SetByteArrayRegion(env, ret, 0, OutSecBuff.cbBuffer,
                    OutSecBuff.pvBuffer);
        }
        if (lastToken != 0) // 2nd stage
            endSequence (pCred, pCtx, env, status);
        result = ret;
    }

    if ((ss != SEC_I_CONTINUE_NEEDED) && (ss == SEC_I_COMPLETE_AND_CONTINUE)) {
        endSequence (pCred, pCtx, env, status);
    }

    return result;
}

static void endSequence (PCredHandle credHand, PCtxtHandle ctxHandle, JNIEnv *env, jobject status) {
    if (credHand != 0) {
        FreeCredentialsHandle(credHand);
        free(credHand);
    }

    if (ctxHandle != 0) {
        DeleteSecurityContext(ctxHandle);
        free(ctxHandle);
    }

    /* Sequence is complete so set flag */
    (*env)->SetBooleanField(env, status, status_seqCompleteID, JNI_TRUE);
}
