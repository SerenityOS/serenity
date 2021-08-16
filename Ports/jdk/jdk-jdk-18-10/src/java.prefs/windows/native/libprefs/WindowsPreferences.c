/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <windows.h>
#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "java_util_prefs_WindowsPreferences.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

JNIEXPORT jlongArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegOpenKey(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray lpSubKey, jint securityMask) {
    char* str = (*env)->GetByteArrayElements(env, lpSubKey, NULL);
    CHECK_NULL_RETURN(str, NULL);

    HKEY handle;
    int errorCode = RegOpenKeyEx((HKEY) hKey, str, 0, securityMask, &handle);
    (*env)->ReleaseByteArrayElements(env, lpSubKey, str, 0);

    __declspec(align(8)) jlong tmp[2];
    tmp[0] = (jlong) handle;
    tmp[1] = errorCode;
    jlongArray result = (*env)->NewLongArray(env, 2);
    if (result != NULL) {
        (*env)->SetLongArrayRegion(env, result, 0, 2, tmp);
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegCloseKey(JNIEnv* env,
    jclass this_class, jlong hKey) {
    return (jint) RegCloseKey((HKEY) hKey);
};

JNIEXPORT jlongArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegCreateKeyEx(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray lpSubKey) {
    char* str = (*env)->GetByteArrayElements(env, lpSubKey, NULL);
    CHECK_NULL_RETURN(str, NULL);

    HKEY handle;
    DWORD lpdwDisposition;
    int errorCode = RegCreateKeyEx((HKEY) hKey, str, 0, NULL,
        REG_OPTION_NON_VOLATILE, KEY_READ,
        NULL, &handle, &lpdwDisposition);
    (*env)->ReleaseByteArrayElements(env, lpSubKey, str, 0);

    __declspec(align(8)) jlong tmp[3];
    tmp[0] = (jlong) handle;
    tmp[1] = errorCode;
    tmp[2] = lpdwDisposition;
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result != NULL) {
        (*env)->SetLongArrayRegion(env, result, 0, 3, tmp);
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegDeleteKey(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray lpSubKey) {
    char* str = (*env)->GetByteArrayElements(env, lpSubKey, NULL);
    CHECK_NULL_RETURN(str, -1);

    int result = RegDeleteKey((HKEY) hKey, str);
    (*env)->ReleaseByteArrayElements(env, lpSubKey, str, 0);
    return result;

};

JNIEXPORT jint JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegFlushKey(JNIEnv* env,
    jclass this_class, jlong hKey) {
    return RegFlushKey((HKEY) hKey);
}

JNIEXPORT jbyteArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegQueryValueEx(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray valueName) {
    char* valueNameStr = (*env)->GetByteArrayElements(env, valueName, NULL);
    CHECK_NULL_RETURN(valueNameStr, NULL);

    DWORD valueType;
    DWORD valueSize;
    if (RegQueryValueEx((HKEY) hKey, valueNameStr, NULL, &valueType, NULL,
        &valueSize) != ERROR_SUCCESS) {
        (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
        return NULL;
    }

    char* buffer = (char*) malloc(valueSize);
    if (buffer != NULL) {
        if (RegQueryValueEx((HKEY) hKey, valueNameStr, NULL, &valueType, buffer,
            &valueSize) != ERROR_SUCCESS) {
            free(buffer);
            (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
            return NULL;
        }
    } else {
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
        (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
        return NULL;
    }

    jbyteArray result;
    if (valueType == REG_SZ) {
        result = (*env)->NewByteArray(env, valueSize);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, valueSize, buffer);
        }
    } else {
        result = NULL;
    }
    free(buffer);
    (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegSetValueEx(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray valueName, jbyteArray data) {
    if ((valueName == NULL) || (data == NULL)) {
        return -1;
    }
    int size = (*env)->GetArrayLength(env, data);
    char* dataStr = (*env)->GetByteArrayElements(env, data, NULL);
    CHECK_NULL_RETURN(dataStr, -1);

    char* valueNameStr = (*env)->GetByteArrayElements(env, valueName, NULL);
    int error_code = -1;
    if (valueNameStr != NULL) {
        error_code = RegSetValueEx((HKEY) hKey, valueNameStr, 0,
            REG_SZ, dataStr, size);
        (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
    }
    (*env)->ReleaseByteArrayElements(env, data, dataStr, 0);
    return error_code;
}

JNIEXPORT jint JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegDeleteValue(JNIEnv* env,
    jclass this_class, jlong hKey, jbyteArray valueName) {
    if (valueName == NULL) {
        return -1;
    }
    char* valueNameStr = (*env)->GetByteArrayElements(env, valueName, NULL);
    CHECK_NULL_RETURN(valueNameStr, -1);

    int error_code = RegDeleteValue((HKEY) hKey, valueNameStr);
    (*env)->ReleaseByteArrayElements(env, valueName, valueNameStr, 0);
    return error_code;
}

JNIEXPORT jlongArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegQueryInfoKey(JNIEnv* env,
    jclass this_class, jlong hKey) {
    int subKeysNumber;
    int maxSubKeyLength;
    int valuesNumber;
    int maxValueNameLength;
    int errorCode = RegQueryInfoKey((HKEY) hKey, NULL, NULL, NULL,
        &subKeysNumber, &maxSubKeyLength, NULL,
        &valuesNumber, &maxValueNameLength,
        NULL, NULL, NULL);

    __declspec(align(8)) jlong tmp[5];
    tmp[0] = subKeysNumber;
    tmp[1] = errorCode;
    tmp[2] = valuesNumber;
    tmp[3] = maxSubKeyLength;
    tmp[4] = maxValueNameLength;
    jintArray result = (*env)->NewLongArray(env, 5);
    if (result != NULL) {
        (*env)->SetLongArrayRegion(env, result, 0, 5, tmp);
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegEnumKeyEx(JNIEnv* env,
    jclass this_class, jlong hKey, jint subKeyIndex, jint maxKeyLength) {
    int size = maxKeyLength;
    char* buffer = (char*) malloc(maxKeyLength);
    if (buffer == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
        return NULL;
    }
    if (RegEnumKeyEx((HKEY) hKey, subKeyIndex, buffer, &size, NULL, NULL,
        NULL, NULL) != ERROR_SUCCESS) {
        free(buffer);
        return NULL;
    }

    jbyteArray result = (*env)->NewByteArray(env, size + 1);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, size + 1, buffer);
    }
    free(buffer);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_java_util_prefs_WindowsPreferences_WindowsRegEnumValue(JNIEnv* env,
    jclass this_class, jlong hKey, jint valueIndex, jint maxValueNameLength) {
    int size = maxValueNameLength;
    char* buffer = (char*) malloc(maxValueNameLength);
    if (buffer == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
        return NULL;
    }

    int error_code = RegEnumValue((HKEY) hKey, valueIndex, buffer,
        &size, NULL, NULL, NULL, NULL);
    if (error_code != ERROR_SUCCESS) {
        free(buffer);
        return NULL;
    }
    jbyteArray result = (*env)->NewByteArray(env, size + 1);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, size + 1, buffer);
    }
    free(buffer);
    return result;
}


#ifdef __cplusplus
}
#endif
