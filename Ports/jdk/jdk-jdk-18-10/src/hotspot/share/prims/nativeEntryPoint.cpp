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
 *
 */

#include "precompiled.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "code/vmreg.hpp"

JNI_LEAF(jlong, NEP_vmStorageToVMReg(JNIEnv* env, jclass _unused, jint type, jint index))
  return VMRegImpl::vmStorageToVMReg(type, index)->value();
JNI_END

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

static JNINativeMethod NEP_methods[] = {
  {CC "vmStorageToVMReg", CC "(II)J", FN_PTR(NEP_vmStorageToVMReg)},
};

JNI_ENTRY(void, JVM_RegisterNativeEntryPointMethods(JNIEnv *env, jclass NEP_class))
  ThreadToNativeFromVM ttnfv(thread);
  int status = env->RegisterNatives(NEP_class, NEP_methods, sizeof(NEP_methods)/sizeof(JNINativeMethod));
  guarantee(status == JNI_OK && !env->ExceptionOccurred(),
            "register jdk.internal.invoke.NativeEntryPoint natives");
JNI_END