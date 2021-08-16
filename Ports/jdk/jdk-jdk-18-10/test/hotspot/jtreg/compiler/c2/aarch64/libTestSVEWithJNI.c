/*
* Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
* Copyright (c) 2020, Arm Limited. All rights reserved.
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
*
*/

#if defined(__aarch64__) && defined (__linux__)

#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/prctl.h>
#include <unistd.h>

#ifndef PR_SVE_GET_VL
// For old toolchains which do not have SVE related macros defined.
#define PR_SVE_SET_VL   50
#define PR_SVE_GET_VL   51
#endif

int get_current_thread_vl() {
  return prctl(PR_SVE_GET_VL);
}

int set_current_thread_vl(unsigned long arg) {
  return prctl(PR_SVE_SET_VL, arg);
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_compiler_c2_aarch64_TestSVEWithJNI_setVectorLength
(JNIEnv * env, jclass clz, jint length) {
  return set_current_thread_vl(length);
}

JNIEXPORT jint JNICALL Java_compiler_c2_aarch64_TestSVEWithJNI_getVectorLength
(JNIEnv *env, jclass clz) {
  return get_current_thread_vl();
}


#ifdef __cplusplus
}
#endif

#endif
