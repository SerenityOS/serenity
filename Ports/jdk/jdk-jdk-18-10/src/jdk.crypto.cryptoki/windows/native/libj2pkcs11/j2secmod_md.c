/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jni_util.h>

#include "j2secmod.h"

extern void throwNullPointerException(JNIEnv *env, const char *message);
extern void throwIOException(JNIEnv *env, const char *message);

void *findFunction(JNIEnv *env, jlong jHandle, const char *functionName) {
    HINSTANCE hModule = (HINSTANCE)jHandle;
    void *fAddress = GetProcAddress(hModule, functionName);
    if (fAddress == NULL) {
        char errorMessage[256];
        _snprintf(errorMessage, sizeof(errorMessage), "Symbol not found: %s", functionName);
        throwNullPointerException(env, errorMessage);
        return NULL;
    }
    return fAddress;
}

JNIEXPORT jlong JNICALL Java_sun_security_pkcs11_Secmod_nssGetLibraryHandle
  (JNIEnv *env, jclass thisClass, jstring jLibName)
{
    const char *libName = (*env)->GetStringUTFChars(env, jLibName, NULL);
    HMODULE hModule = GetModuleHandle(libName);
    dprintf2("-handle for %s: %d\n", libName, hModule);
    (*env)->ReleaseStringUTFChars(env, jLibName, libName);
    return (jlong)hModule;
}

JNIEXPORT jlong JNICALL Java_sun_security_pkcs11_Secmod_nssLoadLibrary
  (JNIEnv *env, jclass thisClass, jstring jName)
{
    HINSTANCE hModule;
    LPVOID lpMsgBuf;

    const char *libName = (*env)->GetStringUTFChars(env, jName, NULL);
    dprintf1("-lib %s\n", libName);

    hModule = LoadLibrary(libName);
    (*env)->ReleaseStringUTFChars(env, jName, libName);

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
        dprintf1("-error: %s\n", lpMsgBuf);
        throwIOException(env, (char*)lpMsgBuf);
        LocalFree(lpMsgBuf);
        return 0;
    }
    dprintf2("-handle: %d (0X%X)\n", hModule, hModule);
    return (jlong)hModule;
}
