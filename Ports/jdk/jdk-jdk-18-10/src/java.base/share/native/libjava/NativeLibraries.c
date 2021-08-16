/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <assert.h>

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "jdk_internal_loader_NativeLibraries.h"
#include <string.h>

typedef jint (JNICALL *JNI_OnLoad_t)(JavaVM *, void *);
typedef void (JNICALL *JNI_OnUnload_t)(JavaVM *, void *);

static jfieldID handleID;
static jfieldID jniVersionID;
static void *procHandle;


static jboolean initIDs(JNIEnv *env)
{
    if (handleID == 0) {
        jclass this =
            (*env)->FindClass(env, "jdk/internal/loader/NativeLibraries$NativeLibraryImpl");
        if (this == 0)
            return JNI_FALSE;
        handleID = (*env)->GetFieldID(env, this, "handle", "J");
        if (handleID == 0)
            return JNI_FALSE;
        jniVersionID = (*env)->GetFieldID(env, this, "jniVersion", "I");
        if (jniVersionID == 0)
            return JNI_FALSE;
        procHandle = getProcessHandle();
    }
    return JNI_TRUE;
}


/*
 * Support for finding JNI_On(Un)Load_<lib_name> if it exists.
 * If cname == NULL then just find normal JNI_On(Un)Load entry point
 */
static void *findJniFunction(JNIEnv *env, void *handle,
                                    const char *cname, jboolean isLoad) {
    const char *onLoadSymbols[] = JNI_ONLOAD_SYMBOLS;
    const char *onUnloadSymbols[] = JNI_ONUNLOAD_SYMBOLS;
    const char **syms;
    int symsLen;
    void *entryName = NULL;
    char *jniFunctionName;
    int i;
    size_t len;

    // Check for JNI_On(Un)Load<_libname> function
    if (isLoad) {
        syms = onLoadSymbols;
        symsLen = sizeof(onLoadSymbols) / sizeof(char *);
    } else {
        syms = onUnloadSymbols;
        symsLen = sizeof(onUnloadSymbols) / sizeof(char *);
    }
    for (i = 0; i < symsLen; i++) {
        // cname + sym + '_' + '\0'
        if ((len = (cname != NULL ? strlen(cname) : 0) + strlen(syms[i]) + 2) >
            FILENAME_MAX) {
            goto done;
        }
        jniFunctionName = malloc(len);
        if (jniFunctionName == NULL) {
            JNU_ThrowOutOfMemoryError(env, NULL);
            goto done;
        }
        buildJniFunctionName(syms[i], cname, jniFunctionName);
        entryName = JVM_FindLibraryEntry(handle, jniFunctionName);
        free(jniFunctionName);
        if(entryName) {
            break;
        }
    }

 done:
    return entryName;
}

/*
 * Class:     jdk_internal_loader_NativeLibraries
 * Method:    load
 * Signature: (Ljava/lang/String;ZZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_jdk_internal_loader_NativeLibraries_load
  (JNIEnv *env, jobject this, jobject lib, jstring name, jboolean isBuiltin, jboolean isJNI)
{
    const char *cname;
    jint jniVersion;
    jthrowable cause;
    void * handle;
    jboolean loaded = JNI_FALSE;

    if (!initIDs(env))
        return JNI_FALSE;

    cname = JNU_GetStringPlatformChars(env, name, 0);
    if (cname == 0)
        return JNI_FALSE;
    handle = isBuiltin ? procHandle : JVM_LoadLibrary(cname);
    if (isJNI) {
        if (handle) {
            JNI_OnLoad_t JNI_OnLoad;
            JNI_OnLoad = (JNI_OnLoad_t)findJniFunction(env, handle,
                                                       isBuiltin ? cname : NULL,
                                                       JNI_TRUE);
            if (JNI_OnLoad) {
                JavaVM *jvm;
                (*env)->GetJavaVM(env, &jvm);
                jniVersion = (*JNI_OnLoad)(jvm, NULL);
            } else {
                jniVersion = 0x00010001;
            }

            cause = (*env)->ExceptionOccurred(env);
            if (cause) {
                (*env)->ExceptionClear(env);
                (*env)->Throw(env, cause);
                if (!isBuiltin) {
                    JVM_UnloadLibrary(handle);
                }
                goto done;
            }

            if (!JVM_IsSupportedJNIVersion(jniVersion) ||
                (isBuiltin && jniVersion < JNI_VERSION_1_8)) {
                char msg[256];
                jio_snprintf(msg, sizeof(msg),
                             "unsupported JNI version 0x%08X required by %s",
                             jniVersion, cname);
                JNU_ThrowByName(env, "java/lang/UnsatisfiedLinkError", msg);
                if (!isBuiltin) {
                    JVM_UnloadLibrary(handle);
                }
                goto done;
            }
            (*env)->SetIntField(env, lib, jniVersionID, jniVersion);
        } else {
            cause = (*env)->ExceptionOccurred(env);
            if (cause) {
                (*env)->ExceptionClear(env);
                (*env)->SetLongField(env, lib, handleID, (jlong)0);
                (*env)->Throw(env, cause);
            }
            goto done;
        }
    }
    (*env)->SetLongField(env, lib, handleID, ptr_to_jlong(handle));
    loaded = JNI_TRUE;

 done:
    JNU_ReleaseStringPlatformChars(env, name, cname);
    return loaded;
}

/*
 * Class:     jdk_internal_loader_NativeLibraries
 * Method:    unload
 * Signature: (Ljava/lang/String;ZZJ)V
 */
JNIEXPORT void JNICALL
Java_jdk_internal_loader_NativeLibraries_unload
(JNIEnv *env, jclass cls, jstring name, jboolean isBuiltin, jboolean isJNI, jlong address)
{
    const char *onUnloadSymbols[] = JNI_ONUNLOAD_SYMBOLS;
    void *handle;
    JNI_OnUnload_t JNI_OnUnload;
    const char *cname;

    if (!initIDs(env))
        return;
    cname = JNU_GetStringPlatformChars(env, name, 0);
    if (cname == NULL) {
        return;
    }
    handle = jlong_to_ptr(address);
    if (isJNI) {
        JNI_OnUnload = (JNI_OnUnload_t )findJniFunction(env, handle,
                                                        isBuiltin ? cname : NULL,
                                                        JNI_FALSE);
        if (JNI_OnUnload) {
            JavaVM *jvm;
            (*env)->GetJavaVM(env, &jvm);
            (*JNI_OnUnload)(jvm, NULL);
        }
    }
    if (!isBuiltin) {
        JVM_UnloadLibrary(handle);
    }
    JNU_ReleaseStringPlatformChars(env, name, cname);
}


/*
 * Class:     jdk_internal_loader_NativeLibraries
 * Method:    findEntry0
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_jdk_internal_loader_NativeLibraries_findEntry0
  (JNIEnv *env, jobject this, jobject lib, jstring name)
{
    jlong handle;
    const char *cname;
    jlong res;

    if (!initIDs(env))
        return jlong_zero;

    handle = (*env)->GetLongField(env, lib, handleID);
    cname = (*env)->GetStringUTFChars(env, name, 0);
    if (cname == 0)
        return jlong_zero;
    res = ptr_to_jlong(JVM_FindLibraryEntry(jlong_to_ptr(handle), cname));
    (*env)->ReleaseStringUTFChars(env, name, cname);
    return res;
}

/*
 * Class:     jdk_internal_loader_NativeLibraries
 * Method:    findBuiltinLib
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_jdk_internal_loader_NativeLibraries_findBuiltinLib
  (JNIEnv *env, jclass cls, jstring name)
{
    const char *cname;
    char *libName;
    size_t prefixLen = strlen(JNI_LIB_PREFIX);
    size_t suffixLen = strlen(JNI_LIB_SUFFIX);
    size_t len;
    jstring lib;
    void *ret;
    const char *onLoadSymbols[] = JNI_ONLOAD_SYMBOLS;

    if (name == NULL) {
        JNU_ThrowInternalError(env, "NULL filename for native library");
        return NULL;
    }
    procHandle = getProcessHandle();
    cname = JNU_GetStringPlatformChars(env, name, 0);
    if (cname == NULL) {
        return NULL;
    }
    // Copy name Skipping PREFIX
    len = strlen(cname);
    if (len <= (prefixLen+suffixLen)) {
        JNU_ReleaseStringPlatformChars(env, name, cname);
        return NULL;
    }
    libName = malloc(len + 1); //+1 for null if prefix+suffix == 0
    if (libName == NULL) {
        JNU_ReleaseStringPlatformChars(env, name, cname);
        JNU_ThrowOutOfMemoryError(env, NULL);
        return NULL;
    }
    if (len > prefixLen) {
        strcpy(libName, cname+prefixLen);
    }
    JNU_ReleaseStringPlatformChars(env, name, cname);

    // Strip SUFFIX
    libName[strlen(libName)-suffixLen] = '\0';

    // Check for JNI_OnLoad_libname function
    ret = findJniFunction(env, procHandle, libName, JNI_TRUE);
    if (ret != NULL) {
        lib = JNU_NewStringPlatform(env, libName);
        free(libName);
        return lib;
    }
    free(libName);
    return NULL;
}
