/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

// #define SECMOD_DEBUG

#include "j2secmod.h"
#include "jni_util.h"


JNIEXPORT jboolean JNICALL Java_sun_security_pkcs11_Secmod_nssVersionCheck
  (JNIEnv *env, jclass thisClass, jlong jHandle, jstring jVersion)
{
    int res = 0;
    FPTR_VersionCheck versionCheck;
    const char *requiredVersion;

    versionCheck = (FPTR_VersionCheck)findFunction(env, jHandle,
        "NSS_VersionCheck");
    if (versionCheck == NULL) {
        return JNI_FALSE;
    }

    requiredVersion = (*env)->GetStringUTFChars(env, jVersion, NULL);
    if (requiredVersion == NULL)  {
        return JNI_FALSE;
    }

    res = versionCheck(requiredVersion);
    dprintf2("-version >=%s: %d\n", requiredVersion, res);
    (*env)->ReleaseStringUTFChars(env, jVersion, requiredVersion);

    return (res == 0) ? JNI_FALSE : JNI_TRUE;
}

/*
 * Initializes NSS.
 * The NSS_INIT_OPTIMIZESPACE flag is supplied by the caller.
 * The NSS_Init* functions are mapped to the NSS_Initialize function.
 */
JNIEXPORT jboolean JNICALL Java_sun_security_pkcs11_Secmod_nssInitialize
  (JNIEnv *env, jclass thisClass, jstring jFunctionName, jlong jHandle, jstring jConfigDir, jboolean jNssOptimizeSpace)
{
    int res = 0;
    FPTR_Initialize initialize =
        (FPTR_Initialize)findFunction(env, jHandle, "NSS_Initialize");
    #ifdef SECMOD_DEBUG
    FPTR_GetError getError =
        (FPTR_GetError)findFunction(env, jHandle, "PORT_GetError");
    #endif // SECMOD_DEBUG
    unsigned int flags = 0x00;
    const char *configDir = NULL;
    const char *functionName = NULL;
    const char *configFile = NULL;

    /* If we cannot initialize, exit now */
    if (initialize == NULL) {
        res = 1;
        goto cleanup;
    }

    functionName = (*env)->GetStringUTFChars(env, jFunctionName, NULL);
    if (functionName == NULL) {
        res = 1;
        goto cleanup;
    }

    if (jConfigDir != NULL) {
        configDir = (*env)->GetStringUTFChars(env, jConfigDir, NULL);
        if (!configDir) {
            res = 1;
            goto cleanup;
        }
    }

    if (jNssOptimizeSpace == JNI_TRUE) {
        flags = 0x20; // NSS_INIT_OPTIMIZESPACE flag
    }

    configFile = "secmod.db";
    if (configDir != NULL && strncmp("sql:", configDir, 4U) == 0) {
        configFile = "pkcs11.txt";
    }

    /*
     * If the NSS_Init function is requested then call NSS_Initialize to
     * open the Cert, Key and Security Module databases, read only.
     */
    if (strcmp("NSS_Init", functionName) == 0) {
        flags = flags | 0x01; // NSS_INIT_READONLY flag
        res = initialize(configDir, "", "", configFile, flags);

    /*
     * If the NSS_InitReadWrite function is requested then call
     * NSS_Initialize to open the Cert, Key and Security Module databases,
     * read/write.
     */
    } else if (strcmp("NSS_InitReadWrite", functionName) == 0) {
        res = initialize(configDir, "", "", configFile, flags);

    /*
     * If the NSS_NoDB_Init function is requested then call
     * NSS_Initialize without creating Cert, Key or Security Module
     * databases.
     */
    } else if (strcmp("NSS_NoDB_Init", functionName) == 0) {
        flags = flags | 0x02  // NSS_INIT_NOCERTDB flag
                      | 0x04  // NSS_INIT_NOMODDB flag
                      | 0x08  // NSS_INIT_FORCEOPEN flag
                      | 0x10; // NSS_INIT_NOROOTINIT flag
        res = initialize("", "", "", "", flags);

    } else {
        res = 2;
    }

cleanup:
    if (functionName != NULL) {
        (*env)->ReleaseStringUTFChars(env, jFunctionName, functionName);
    }
    if (configDir != NULL) {
        (*env)->ReleaseStringUTFChars(env, jConfigDir, configDir);
    }
    dprintf1("-res: %d\n", res);
    #ifdef SECMOD_DEBUG
    if (res == -1) {
        if (getError != NULL) {
            dprintf1("-NSS error: %d\n", getError());
        }
    }
    #endif // SECMOD_DEBUG

    return (res == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_sun_security_pkcs11_Secmod_nssGetModuleList
  (JNIEnv *env, jclass thisClass, jlong jHandle, jstring jLibDir)
{
    FPTR_GetDBModuleList getModuleList =
        (FPTR_GetDBModuleList)findFunction(env, jHandle, "SECMOD_GetDefaultModuleList");

    SECMODModuleList *list;
    SECMODModule *module;
    jclass jListClass, jModuleClass;
    jobject jList, jModule;
    jmethodID jListConstructor, jAdd, jModuleConstructor;
    jstring jCommonName, jDllName;
    jint i, jSlotID;

    if (getModuleList == NULL) {
        dprintf("-getmodulelist function not found\n");
        return NULL;
    }
    list = getModuleList();
    if (list == NULL) {
        dprintf("-module list is null\n");
        return NULL;
    }

    jListClass = (*env)->FindClass(env, "java/util/ArrayList");
    if (jListClass == NULL) {
        return NULL;
    }
    jListConstructor = (*env)->GetMethodID(env, jListClass, "<init>", "()V");
    if (jListConstructor == NULL) {
        return NULL;
    }
    jAdd = (*env)->GetMethodID(env, jListClass, "add", "(Ljava/lang/Object;)Z");
    if (jAdd == NULL) {
        return NULL;
    }
    jList = (*env)->NewObject(env, jListClass, jListConstructor);
    if (jList == NULL) {
        return NULL;
    }
    jModuleClass = (*env)->FindClass(env, "sun/security/pkcs11/Secmod$Module");
    if (jModuleClass == NULL) {
        return NULL;
    }
    jModuleConstructor = (*env)->GetMethodID(env, jModuleClass, "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V");
    if (jModuleConstructor == NULL) {
        return NULL;
    }

    while (list != NULL) {
        module = list->module;
        // assert module != null
        dprintf1("-commonname: %s\n", module->commonName);
        dprintf1("-dllname: %s\n", (module->dllName != NULL) ? module->dllName : "NULL");
        dprintf1("-slots: %d\n", module->slotCount);
        dprintf1("-loaded: %d\n", module->loaded);
        dprintf1("-internal: %d\n", module->internal);
        dprintf1("-fips: %d\n", module->isFIPS);
        jCommonName = (*env)->NewStringUTF(env, module->commonName);
        if (jCommonName == NULL) {
            return NULL;
        }
        if (module->dllName == NULL) {
            jDllName = NULL;
        } else {
            jDllName = (*env)->NewStringUTF(env, module->dllName);
            if (jDllName == NULL) {
                return NULL;
            }
        }
        for (i = 0; i < module->slotCount; i++ ) {
            jSlotID = module->slots[i]->slotID;
            if (jDllName == NULL && jSlotID != NETSCAPE_SLOT_ID &&
                    jSlotID != PRIVATE_KEY_SLOT_ID && jSlotID != FIPS_SLOT_ID) {
                // Ignore unknown slot IDs in the NSS Internal Module. See JDK-8265462.
                continue;
            }
            jModule = (*env)->NewObject(env, jModuleClass, jModuleConstructor,
                    jLibDir, jDllName, jCommonName, i, jSlotID);
            if (jModule == NULL) {
                return NULL;
            }
            (*env)->CallVoidMethod(env, jList, jAdd, jModule);
            if ((*env)->ExceptionCheck(env)) {
                return NULL;
            }
        }
        list = list->next;
    }
    dprintf("-ok\n");

    return jList;
}
