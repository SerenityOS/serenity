/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * pkcs11wrapper.c
 * 18.05.2001
 *
 * This module contains the native functions of the Java to PKCS#11 interface
 * which are platform dependent. This includes loading a dynamic link libary,
 * retrieving the function list and unloading the dynamic link library.
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 */

#include "pkcs11wrapper.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <windows.h>

#include <jni.h>

#include "sun_security_pkcs11_wrapper_PKCS11.h"

/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    connect
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_connect
    (JNIEnv *env, jobject obj, jstring jPkcs11ModulePath,
        jstring jGetFunctionList)
{
    HINSTANCE hModule;
    CK_C_GetFunctionList C_GetFunctionList;
    CK_RV rv = CK_ASSERT_OK;
    ModuleData *moduleData;
    jobject globalPKCS11ImplementationReference;
    LPVOID lpMsgBuf = NULL;
    char *exceptionMessage = NULL;
    const char *getFunctionListStr;

    const char *libraryNameStr = (*env)->GetStringUTFChars(env,
            jPkcs11ModulePath, 0);
    TRACE1("DEBUG: connect to PKCS#11 module: %s ... ", libraryNameStr);


  /*
   * Load the PKCS #11 DLL
   */
    hModule = LoadLibrary(libraryNameStr);
    if (hModule == NULL) {
        FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL,
            GetLastError(),
            0, /* Default language */
            (LPTSTR) &lpMsgBuf,
            0,
            NULL
        );
        exceptionMessage = (char *) malloc(sizeof(char) *
                (strlen((LPTSTR) lpMsgBuf) + strlen(libraryNameStr) + 1));
        if (exceptionMessage == NULL) {
            throwOutOfMemoryError(env, 0);
            goto cleanup;
        }
        strcpy(exceptionMessage, (LPTSTR) lpMsgBuf);
        strcat(exceptionMessage, libraryNameStr);
        throwIOException(env, (LPTSTR) exceptionMessage);
        goto cleanup;
    }

    /*
     * Get function pointer to C_GetFunctionList
     */
    getFunctionListStr = (*env)->GetStringUTFChars(env, jGetFunctionList, 0);
    C_GetFunctionList = (CK_C_GetFunctionList) GetProcAddress(hModule,
            getFunctionListStr);
    (*env)->ReleaseStringUTFChars(env, jGetFunctionList, getFunctionListStr);
    if (C_GetFunctionList == NULL) {
        FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL,
            GetLastError(),
            0, /* Default language */
            (LPTSTR) &lpMsgBuf,
            0,
            NULL
        );
        throwIOException(env, (LPTSTR) lpMsgBuf);
        goto cleanup;
    }

    /*
     * Get function pointers to all PKCS #11 functions
     */
    moduleData = (ModuleData *) malloc(sizeof(ModuleData));
    if (moduleData == NULL) {
        throwOutOfMemoryError(env, 0);
        goto cleanup;
    }
    moduleData->hModule = hModule;
    moduleData->applicationMutexHandler = NULL;
    rv = (C_GetFunctionList)(&(moduleData->ckFunctionListPtr));
    globalPKCS11ImplementationReference = (*env)->NewGlobalRef(env, obj);
    putModuleEntry(env, globalPKCS11ImplementationReference, moduleData);

    TRACE0("FINISHED\n");

cleanup:
    /* Free up allocated buffers we no longer need */
    if (lpMsgBuf != NULL) {
        LocalFree( lpMsgBuf );
    }
    if (libraryNameStr != NULL) {
        (*env)->ReleaseStringUTFChars(env, jPkcs11ModulePath, libraryNameStr);
    }
    if (exceptionMessage != NULL) {
        free(exceptionMessage);
    }

    if(ckAssertReturnValueOK(env, rv) != CK_ASSERT_OK) { return; }
}

/*
 * Class:     sun_security_pkcs11_wrapper_PKCS11
 * Method:    disconnect
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_security_pkcs11_wrapper_PKCS11_disconnect
    (JNIEnv *env, jobject obj)
{
    ModuleData *moduleData;
    TRACE0("DEBUG: disconnecting module...");
    moduleData = removeModuleEntry(env, obj);

    if (moduleData != NULL) {
        FreeLibrary(moduleData->hModule);
    }

    free(moduleData);
    TRACE0("FINISHED\n");
}
