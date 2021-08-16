/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_util.h"
#include "jdk_util.h"
#include <urlmon.h>

typedef HRESULT (WINAPI *CoInternetCreateSecurityManagerType)
        (IServiceProvider*,IInternetSecurityManager**,DWORD);

static CoInternetCreateSecurityManagerType fn_CoInternetCreateSecurityManager;

JNIEXPORT jboolean JNICALL
Java_sun_net_www_protocol_http_ntlm_NTLMAuthentication_isTrustedSiteAvailable
  (JNIEnv *env, jclass clazz)
{
    HMODULE libUrlmon = JDK_LoadSystemLibrary("urlmon.dll");
    if (libUrlmon != NULL) {
        fn_CoInternetCreateSecurityManager = (CoInternetCreateSecurityManagerType)
                GetProcAddress(libUrlmon, "CoInternetCreateSecurityManager");
        if (fn_CoInternetCreateSecurityManager != NULL) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_sun_net_www_protocol_http_ntlm_NTLMAuthentication_isTrustedSite0
  (JNIEnv *env, jclass clazz, jstring url)
{
    HRESULT hr;
    DWORD dwZone;
    DWORD  pPolicy = 0;
    IInternetSecurityManager *spSecurityManager;
    jboolean ret;

    if (fn_CoInternetCreateSecurityManager == NULL)
        return JNI_FALSE;

    // Create IInternetSecurityManager
    hr = fn_CoInternetCreateSecurityManager(NULL, &spSecurityManager, (DWORD)0);
    if (FAILED(hr)) {
        return JNI_FALSE;
    }

    const LPCWSTR bstrURL = (LPCWSTR)((*env)->GetStringChars(env, url, NULL));
    if (bstrURL == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        spSecurityManager->lpVtbl->Release(spSecurityManager);
        return JNI_FALSE;
    }

    // Determines the policy for the URLACTION_CREDENTIALS_USE action and display
    // a user interface, if the policy indicates that the user should be queried
    hr = spSecurityManager->lpVtbl->ProcessUrlAction(
        spSecurityManager,
        bstrURL,
        URLACTION_CREDENTIALS_USE,
        (LPBYTE)&pPolicy,
        sizeof(DWORD), 0, 0, 0, 0);

    if (FAILED(hr)) {
        ret = JNI_FALSE;
        goto cleanupAndReturn;
    }

    // If these two User Authentication Logon options is selected
    // Anonymous logon
    // Prompt for user name and password
    if (pPolicy == URLPOLICY_CREDENTIALS_ANONYMOUS_ONLY ||
        pPolicy == URLPOLICY_CREDENTIALS_MUST_PROMPT_USER) {
        ret = JNI_FALSE;
        goto cleanupAndReturn;
    }

    // Option "Automatic logon with current user name and password" is selected
    if (pPolicy == URLPOLICY_CREDENTIALS_SILENT_LOGON_OK) {
        ret = JNI_TRUE;
        goto cleanupAndReturn;
    }

    // Option "Automatic logon only in intranet zone" is selected
    if (pPolicy == URLPOLICY_CREDENTIALS_CONDITIONAL_PROMPT) {

        // Gets the zone index from the specified URL
        hr = spSecurityManager->lpVtbl->MapUrlToZone(
                spSecurityManager, bstrURL, &dwZone, 0);
        if (FAILED(hr)) {
            ret = JNI_FALSE;
            goto cleanupAndReturn;
        }

        // Check if the URL is in Local or Intranet zone
        if (dwZone == URLZONE_INTRANET || dwZone == URLZONE_LOCAL_MACHINE) {
            ret = JNI_TRUE;
            goto cleanupAndReturn;
        }
    }
    ret = JNI_FALSE;

cleanupAndReturn:
    (*env)->ReleaseStringChars(env, url, bstrURL);
    spSecurityManager->lpVtbl->Release(spSecurityManager);
    return ret;
}
