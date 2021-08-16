/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "code/codeCache.hpp"
#include "runtime/vmOperations.hpp"

JVM_ENTRY(static jboolean, UH_FreeUpcallStub0(JNIEnv *env, jobject _unused, jlong addr))
  //acquire code cache lock
  MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
  //find code blob
  CodeBlob* cb = CodeCache::find_blob((char*)addr);
  if (cb == NULL) {
    return false;
  }
  //free global JNI handle
  jobject handle = NULL;
  if (cb->is_optimized_entry_blob()) {
    handle = ((OptimizedEntryBlob*)cb)->receiver();
  } else {
    jobject* handle_ptr = (jobject*)(void*)cb->content_begin();
    handle = *handle_ptr;
  }
  JNIHandles::destroy_global(handle);
  //free code blob
  CodeCache::free(cb);
  return true;
JVM_END

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)
#define LANG "Ljava/lang/"

// These are the native methods on jdk.internal.foreign.NativeInvoker.
static JNINativeMethod UH_methods[] = {
  {CC "freeUpcallStub0",     CC "(J)Z",                FN_PTR(UH_FreeUpcallStub0)}
};

/**
 * This one function is exported, used by NativeLookup.
 */
JVM_LEAF(void, JVM_RegisterUpcallHandlerMethods(JNIEnv *env, jclass UH_class))
  int status = env->RegisterNatives(UH_class, UH_methods, sizeof(UH_methods)/sizeof(JNINativeMethod));
  guarantee(status == JNI_OK && !env->ExceptionOccurred(),
            "register jdk.internal.foreign.abi.UpcallStubs natives");
JVM_END

