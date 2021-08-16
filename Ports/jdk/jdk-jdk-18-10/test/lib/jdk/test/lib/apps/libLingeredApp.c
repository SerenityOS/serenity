/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

// Borrowed from hotspot vmError.cpp.
// Returns an address which is guaranteed to generate a SIGSEGV on read,
// which is not NULL and contains bits in every word
void* get_segfault_address() {
  return (void*)
#ifdef _LP64
    0xABC0000000000ABCULL;
#else
    0x00000ABC;
#endif
}

JNIEXPORT jint JNICALL
Java_jdk_test_lib_apps_LingeredApp_crash(JNIEnv *env, jclass clss)
{
    return *(jint *)get_segfault_address();
}
