/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "jvm.h"
#include "jdk_internal_misc_CDS.h"

JNIEXPORT void JNICALL
Java_jdk_internal_misc_CDS_initializeFromArchive(JNIEnv *env, jclass ignore,
                                                jclass c) {
    JVM_InitializeFromArchive(env, c);
}

JNIEXPORT void JNICALL
Java_jdk_internal_misc_CDS_defineArchivedModules(JNIEnv *env, jclass ignore,
                                                jobject platform_loader,
                                                jobject system_loader) {
    JVM_DefineArchivedModules(env, platform_loader, system_loader);
}

JNIEXPORT jlong JNICALL
Java_jdk_internal_misc_CDS_getRandomSeedForDumping(JNIEnv *env, jclass ignore) {
    return JVM_GetRandomSeedForDumping();
}

JNIEXPORT jboolean JNICALL
Java_jdk_internal_misc_CDS_isDumpingArchive0(JNIEnv *env, jclass jcls) {
    return JVM_IsCDSDumpingEnabled(env);
}

JNIEXPORT jboolean JNICALL
Java_jdk_internal_misc_CDS_isSharingEnabled0(JNIEnv *env, jclass jcls) {
    return JVM_IsSharingEnabled(env);
}

JNIEXPORT jboolean JNICALL
Java_jdk_internal_misc_CDS_isDumpingClassList0(JNIEnv *env, jclass jcls) {
    return JVM_IsDumpingClassList(env);
}

JNIEXPORT void JNICALL
Java_jdk_internal_misc_CDS_logLambdaFormInvoker(JNIEnv *env, jclass jcls, jstring line) {
    JVM_LogLambdaFormInvoker(env, line);
}

JNIEXPORT void JNICALL
Java_jdk_internal_misc_CDS_dumpClassList(JNIEnv *env, jclass jcls, jstring fileName) {
    JVM_DumpClassListToFile(env, fileName);
}

JNIEXPORT void JNICALL
Java_jdk_internal_misc_CDS_dumpDynamicArchive(JNIEnv *env, jclass jcls, jstring archiveName) {
    JVM_DumpDynamicArchive(env, archiveName);
}
