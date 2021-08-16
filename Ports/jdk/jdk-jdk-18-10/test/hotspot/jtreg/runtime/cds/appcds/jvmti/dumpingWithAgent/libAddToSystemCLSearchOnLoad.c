/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
#include <string.h>
#include <jvmti.h>

#ifdef __cplusplus
extern "C" {
#endif

    static jvmtiEnv *jvmti = NULL;

    JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
        int err = (*jvm)->GetEnv(jvm, (void**) &jvmti, JVMTI_VERSION_9);
        if (err != JNI_OK) {
            return JNI_ERR;
        }
        err = (*jvmti)->AddToSystemClassLoaderSearch(jvmti, (const char*)options);
        if (err != JVMTI_ERROR_NONE) {
            return JNI_ERR;
        }
        return JNI_OK;
    }

#ifdef __cplusplus
}
#endif
