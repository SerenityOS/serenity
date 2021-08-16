/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_WHITEBOX_HPP
#define SHARE_PRIMS_WHITEBOX_HPP

#include "jni.h"

#include "utilities/exceptions.hpp"
#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/symbol.hpp"

#define WB_METHOD_DECLARE(result_type) extern "C" result_type JNICALL

// Unconditionally clear pedantic pending JNI checks
class ClearPendingJniExcCheck : public StackObj {
private:
  JavaThread* _thread;
public:
  ClearPendingJniExcCheck(JNIEnv* env) : _thread(JavaThread::thread_from_jni_environment(env)) {}
  ~ClearPendingJniExcCheck() {
    _thread->clear_pending_jni_exception_check();
  }
};

class CodeBlob;
class CodeHeap;
class JavaThread;

class WhiteBox : public AllStatic {
 private:
  static bool _used;
 public:
  static volatile bool compilation_locked;
  static bool used()     { return _used; }
  static void set_used() { _used = true; }
  static int offset_for_field(const char* field_name, oop object,
    Symbol* signature_symbol);
  static const char* lookup_jstring(const char* field_name, oop object);
  static bool lookup_bool(const char* field_name, oop object);
  static int get_blob_type(const CodeBlob* code);
  static CodeHeap* get_code_heap(int blob_type);
  static CodeBlob* allocate_code_blob(int size, int blob_type);
  static int array_bytes_to_length(size_t bytes);
  static void register_methods(JNIEnv* env, jclass wbclass, JavaThread* thread,
    JNINativeMethod* method_array, int method_count);
  static void register_extended(JNIEnv* env, jclass wbclass, JavaThread* thread);
  static bool compile_method(Method* method, int comp_level, int bci, JavaThread* THREAD);
#ifdef LINUX
  static bool validate_cgroup(const char* proc_cgroups, const char* proc_self_cgroup, const char* proc_self_mountinfo, u1* cg_flags);
#endif
};

#endif // SHARE_PRIMS_WHITEBOX_HPP
