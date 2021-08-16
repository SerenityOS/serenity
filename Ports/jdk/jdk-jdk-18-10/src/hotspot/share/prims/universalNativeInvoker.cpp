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

#include "precompiled.hpp"
#include "prims/universalNativeInvoker.hpp"
#include "runtime/interfaceSupport.inline.hpp"

ProgrammableInvoker::Generator::Generator(CodeBuffer* code, const ABIDescriptor* abi, const BufferLayout* layout)
  : StubCodeGenerator(code),
    _abi(abi),
    _layout(layout) {}

void ProgrammableInvoker::invoke_native(Stub stub, address buff, JavaThread* thread) {
  ThreadToNativeFromVM ttnfvm(thread);
  // We need WXExec because we are about to call a generated stub. Like in VM
  // entries, the thread state should be changed while we are still in WXWrite.
  // See JDK-8265292.
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, thread));
  stub(buff);
}

JNI_ENTRY(void, PI_invokeNative(JNIEnv* env, jclass _unused, jlong adapter_stub, jlong buff))
  assert(thread->thread_state() == _thread_in_vm, "thread state is: %d", thread->thread_state());
  ProgrammableInvoker::Stub stub = (ProgrammableInvoker::Stub) adapter_stub;
  address c = (address) buff;
  ProgrammableInvoker::invoke_native(stub, c, thread);
JNI_END

JNI_ENTRY(jlong, PI_generateAdapter(JNIEnv* env, jclass _unused, jobject abi, jobject layout))
  return (jlong) ProgrammableInvoker::generate_adapter(abi, layout);
JNI_END

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)
#define FOREIGN_ABI "Ljdk/internal/foreign/abi"

static JNINativeMethod PI_methods[] = {
  {CC "invokeNative",    CC "(JJ)V",                                                             FN_PTR(PI_invokeNative)   },
  {CC "generateAdapter", CC "(" FOREIGN_ABI "/ABIDescriptor;" FOREIGN_ABI "/BufferLayout;" ")J", FN_PTR(PI_generateAdapter)}
};

JNI_ENTRY(void, JVM_RegisterProgrammableInvokerMethods(JNIEnv *env, jclass PI_class))
  ThreadToNativeFromVM ttnfv(thread);
  int status = env->RegisterNatives(PI_class, PI_methods, sizeof(PI_methods)/sizeof(JNINativeMethod));
  guarantee(status == JNI_OK && !env->ExceptionOccurred(),
            "register jdk.internal.foreign.abi.programmable.ProgrammableInvoker natives");
JNI_END
