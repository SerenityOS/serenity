/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>

// checked malloc to trap OOM conditions
static void* c_malloc(JNIEnv* env, size_t size) {
  void* ret = malloc(size);
  if (ret == NULL)
    env->FatalError("malloc failed");
  return ret;
}

// Asserts every exception as fatal one
#define CE {\
    if (env->ExceptionOccurred())\
    {\
        puts("Unexpected JNI exception. TEST FAIL.");\
        env->ExceptionDescribe();\
        env->ExceptionClear();\
        env->FatalError("Unexpected JNI Exception. TEST FAIL.");\
    }\
}

// Checks return code for JNI calls that don't raise exceptions
// and generate fatal error
#define CHECK(jniCall) do { \
  if ((jniCall) != 0) { \
    env->FatalError("Error invoking JNI method: " #jniCall); \
  } \
} while (0)
