/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2011 SAP AG.  All Rights Reserved.
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

/*
 * This source file is the same as libtest-rwx.c and needs to be a separate
 * file so it can be built with "-z noexecstack" by the build process.
 * If any changes are made they probably also need to be made to libtest-rwx.c.
 */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "jni.h"
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_Test_someMethod(JNIEnv *env, jobject mainObject) {
  return 3;
}

#ifdef __cplusplus
}
#endif
