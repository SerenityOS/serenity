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
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "compiler/compilationPolicy.hpp"
#include "memory/resourceArea.hpp"
#include "prims/universalUpcallHandler.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"

#define FOREIGN_ABI "jdk/internal/foreign/abi/"

extern struct JavaVM_ main_vm;

void ProgrammableUpcallHandler::upcall_helper(JavaThread* thread, jobject rec, address buff) {
  JavaThread* THREAD = thread; // For exception macros.
  ThreadInVMfromNative tiv(THREAD);
  const UpcallMethod& upcall_method = instance().upcall_method;

  ResourceMark rm(THREAD);
  JavaValue result(T_VOID);
  JavaCallArguments args(2); // long = 2 slots

  args.push_jobject(rec);
  args.push_long((jlong) buff);

  JavaCalls::call_static(&result, upcall_method.klass, upcall_method.name, upcall_method.sig, &args, CATCH);
}

JavaThread* ProgrammableUpcallHandler::maybe_attach_and_get_thread(bool* should_detach) {
  JavaThread* thread = JavaThread::current_or_null();
  if (thread == nullptr) {
    JavaVM_ *vm = (JavaVM *)(&main_vm);
    JNIEnv* p_env = nullptr; // unused
    jint result = vm->functions->AttachCurrentThread(vm, (void**) &p_env, nullptr);
    guarantee(result == JNI_OK, "Could not attach thread for upcall. JNI error code: %d", result);
    *should_detach = true;
    thread = JavaThread::current();
    assert(!thread->has_last_Java_frame(), "newly-attached thread not expected to have last Java frame");
  } else {
    *should_detach = false;
  }
  return thread;
}

void ProgrammableUpcallHandler::detach_current_thread() {
  JavaVM_ *vm = (JavaVM *)(&main_vm);
  vm->functions->DetachCurrentThread(vm);
}

// modelled after JavaCallWrapper::JavaCallWrapper
JavaThread* ProgrammableUpcallHandler::on_entry(OptimizedEntryBlob::FrameData* context) {
  JavaThread* thread = maybe_attach_and_get_thread(&context->should_detach);
  context->thread = thread;

  assert(thread->can_call_java(), "must be able to call Java");

  // Allocate handle block for Java code. This must be done before we change thread_state to _thread_in_Java,
  // since it can potentially block.
  context->new_handles = JNIHandleBlock::allocate_block(thread);

  // After this, we are officially in Java Code. This needs to be done before we change any of the thread local
  // info, since we cannot find oops before the new information is set up completely.
  ThreadStateTransition::transition_from_native(thread, _thread_in_Java);

  // Make sure that we handle asynchronous stops and suspends _before_ we clear all thread state
  // in OptimizedEntryBlob::FrameData. This way, we can decide if we need to do any pd actions
  // to prepare for stop/suspend (cache sp, or other state).
  bool clear_pending_exception = true;
  if (thread->has_special_runtime_exit_condition()) {
    thread->handle_special_runtime_exit_condition();
    if (thread->has_pending_exception()) {
      clear_pending_exception = false;
    }
  }

  context->old_handles = thread->active_handles();

  // For the profiler, the last_Java_frame information in thread must always be in
  // legal state. We have no last Java frame if last_Java_sp == NULL so
  // the valid transition is to clear _last_Java_sp and then reset the rest of
  // the (platform specific) state.

  context->jfa.copy(thread->frame_anchor());
  thread->frame_anchor()->clear();

  debug_only(thread->inc_java_call_counter());
  thread->set_active_handles(context->new_handles);     // install new handle block and reset Java frame linkage

  // clear any pending exception in thread (native calls start with no exception pending)
  if (clear_pending_exception) {
    thread->clear_pending_exception();
  }

  MACOS_AARCH64_ONLY(thread->enable_wx(WXExec));

  return thread;
}

// modelled after JavaCallWrapper::~JavaCallWrapper
void ProgrammableUpcallHandler::on_exit(OptimizedEntryBlob::FrameData* context) {
  JavaThread* thread = context->thread;
  assert(thread == JavaThread::current(), "must still be the same thread");

  MACOS_AARCH64_ONLY(thread->enable_wx(WXWrite));

  // restore previous handle block
  thread->set_active_handles(context->old_handles);

  thread->frame_anchor()->zap();

  debug_only(thread->dec_java_call_counter());

  // Old thread-local info. has been restored. We are now back in native code.
  ThreadStateTransition::transition_from_java(thread, _thread_in_native);

  thread->frame_anchor()->copy(&context->jfa);

  // Release handles after we are marked as being in native code again, since this
  // operation might block
  JNIHandleBlock::release_block(context->new_handles, thread);

  assert(!thread->has_pending_exception(), "Upcall can not throw an exception");

  if (context->should_detach) {
    detach_current_thread();
  }
}

void ProgrammableUpcallHandler::attach_thread_and_do_upcall(jobject rec, address buff) {
  bool should_detach = false;
  JavaThread* thread = maybe_attach_and_get_thread(&should_detach);

  {
    MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXWrite, thread));
    upcall_helper(thread, rec, buff);
  }

  if (should_detach) {
    detach_current_thread();
  }
}

const ProgrammableUpcallHandler& ProgrammableUpcallHandler::instance() {
  static ProgrammableUpcallHandler handler;
  return handler;
}

ProgrammableUpcallHandler::ProgrammableUpcallHandler() {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  ResourceMark rm(THREAD);
  Symbol* sym = SymbolTable::new_symbol(FOREIGN_ABI "ProgrammableUpcallHandler");
  Klass* k = SystemDictionary::resolve_or_null(sym, Handle(), Handle(), CATCH);
  k->initialize(CATCH);

  upcall_method.klass = k;
  upcall_method.name = SymbolTable::new_symbol("invoke");
  upcall_method.sig = SymbolTable::new_symbol("(Ljava/lang/invoke/MethodHandle;J)V");

  assert(upcall_method.klass->lookup_method(upcall_method.name, upcall_method.sig) != nullptr,
    "Could not find upcall method: %s.%s%s", upcall_method.klass->external_name(),
    upcall_method.name->as_C_string(), upcall_method.sig->as_C_string());
}

void ProgrammableUpcallHandler::handle_uncaught_exception(oop exception) {
  // Based on CATCH macro
  tty->print_cr("Uncaught exception:");
  exception->print();
  ShouldNotReachHere();
}

JVM_ENTRY(jlong, PUH_AllocateUpcallStub(JNIEnv *env, jclass unused, jobject rec, jobject abi, jobject buffer_layout))
  Handle receiver(THREAD, JNIHandles::resolve(rec));
  jobject global_rec = JNIHandles::make_global(receiver);
  return (jlong) ProgrammableUpcallHandler::generate_upcall_stub(global_rec, abi, buffer_layout);
JNI_END

JVM_ENTRY(jlong, PUH_AllocateOptimizedUpcallStub(JNIEnv *env, jclass unused, jobject mh, jobject abi, jobject conv))
  Handle mh_h(THREAD, JNIHandles::resolve(mh));
  jobject mh_j = JNIHandles::make_global(mh_h);

  oop lform = java_lang_invoke_MethodHandle::form(mh_h());
  oop vmentry = java_lang_invoke_LambdaForm::vmentry(lform);
  Method* entry = java_lang_invoke_MemberName::vmtarget(vmentry);
  const methodHandle mh_entry(THREAD, entry);

  assert(entry->method_holder()->is_initialized(), "no clinit barrier");
  CompilationPolicy::compile_if_required(mh_entry, CHECK_0);

  return (jlong) ProgrammableUpcallHandler::generate_optimized_upcall_stub(mh_j, entry, abi, conv);
JVM_END

JVM_ENTRY(jboolean, PUH_SupportsOptimizedUpcalls(JNIEnv *env, jclass unused))
  return (jboolean) ProgrammableUpcallHandler::supports_optimized_upcalls();
JVM_END

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

static JNINativeMethod PUH_methods[] = {
  {CC "allocateUpcallStub", CC "(" "Ljava/lang/invoke/MethodHandle;" "L" FOREIGN_ABI "ABIDescriptor;" "L" FOREIGN_ABI "BufferLayout;" ")J", FN_PTR(PUH_AllocateUpcallStub)},
  {CC "allocateOptimizedUpcallStub", CC "(" "Ljava/lang/invoke/MethodHandle;" "L" FOREIGN_ABI "ABIDescriptor;" "L" FOREIGN_ABI "ProgrammableUpcallHandler$CallRegs;" ")J", FN_PTR(PUH_AllocateOptimizedUpcallStub)},
  {CC "supportsOptimizedUpcalls", CC "()Z", FN_PTR(PUH_SupportsOptimizedUpcalls)},
};

/**
 * This one function is exported, used by NativeLookup.
 */
JNI_ENTRY(void, JVM_RegisterProgrammableUpcallHandlerMethods(JNIEnv *env, jclass PUH_class))
  ThreadToNativeFromVM ttnfv(thread);
  int status = env->RegisterNatives(PUH_class, PUH_methods, sizeof(PUH_methods)/sizeof(JNINativeMethod));
  guarantee(status == JNI_OK && !env->ExceptionOccurred(),
            "register jdk.internal.foreign.abi.ProgrammableUpcallHandler natives");
JNI_END
