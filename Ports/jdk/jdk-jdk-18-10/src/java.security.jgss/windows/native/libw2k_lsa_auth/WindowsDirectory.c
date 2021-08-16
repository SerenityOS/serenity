/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

#define UNICODE
#include <jni.h>
#include <windows.h>
#include <stdlib.h>
#include "sun_security_krb5_Config.h"

/*
 * Class:     sun_security_krb5_Config
 * Method:    getWindowsDirectory
 * Signature: (Z)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_security_krb5_Config_getWindowsDirectory(
        JNIEnv* env, jclass configClass, jboolean isSystem) {
    TCHAR lpPath[MAX_PATH+1];
    UINT len;
    if (isSystem) {
        len = GetSystemWindowsDirectory(lpPath, MAX_PATH);
    } else {
        len = GetWindowsDirectory(lpPath, MAX_PATH);
    }
    if (len) {
        return (*env)->NewString(env, lpPath, len);
    } else {
        return NULL;
    }
}
