/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "code/scopeDesc.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compilerEvent.hpp"
#include "compiler/disassembler.hpp"
#include "compiler/oopMap.hpp"
#include "interpreter/linkResolver.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "jfr/jfrEvents.hpp"
#include "jvmci/jvmciCompilerToVM.hpp"
#include "jvmci/jvmciCodeInstaller.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "logging/log.hpp"
#include "logging/logTag.hpp"
#include "memory/oopFactory.hpp"
#include "memory/universe.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/method.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "prims/nativeLookup.hpp"
#include "runtime/atomic.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/reflectionUtils.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/vframe_hp.hpp"
#include "runtime/vframe.inline.hpp"

JVMCIKlassHandle::JVMCIKlassHandle(Thread* thread, Klass* klass) {
  _thread = thread;
  _klass = klass;
  if (klass != NULL) {
    _holder = Handle(_thread, klass->klass_holder());
  }
}

JVMCIKlassHandle& JVMCIKlassHandle::operator=(Klass* klass) {
  _klass = klass;
  if (klass != NULL) {
    _holder = Handle(_thread, klass->klass_holder());
  }
  return *this;
}

static void requireInHotSpot(const char* caller, JVMCI_TRAPS) {
  if (!JVMCIENV->is_hotspot()) {
    JVMCI_THROW_MSG(IllegalStateException, err_msg("Cannot call %s from JVMCI shared library", caller));
  }
}

void JNIHandleMark::push_jni_handle_block(JavaThread* thread) {
  if (thread != NULL) {
    // Allocate a new block for JNI handles.
    // Inlined code from jni_PushLocalFrame()
    JNIHandleBlock* java_handles = thread->active_handles();
    JNIHandleBlock* compile_handles = JNIHandleBlock::allocate_block(thread);
    assert(compile_handles != NULL && java_handles != NULL, "should not be NULL");
    compile_handles->set_pop_frame_link(java_handles);
    thread->set_active_handles(compile_handles);
  }
}

void JNIHandleMark::pop_jni_handle_block(JavaThread* thread) {
  if (thread != NULL) {
    // Release our JNI handle block
    JNIHandleBlock* compile_handles = thread->active_handles();
    JNIHandleBlock* java_handles = compile_handles->pop_frame_link();
    thread->set_active_handles(java_handles);
    compile_handles->set_pop_frame_link(NULL);
    JNIHandleBlock::release_block(compile_handles, thread); // may block
  }
}

class JVMCITraceMark : public StackObj {
  const char* _msg;
 public:
  JVMCITraceMark(const char* msg) {
    _msg = msg;
    JVMCI_event_2("Enter %s", _msg);
  }
  ~JVMCITraceMark() {
    JVMCI_event_2(" Exit %s", _msg);
  }
};


Handle JavaArgumentUnboxer::next_arg(BasicType expectedType) {
  assert(_index < _args->length(), "out of bounds");
  oop arg=((objArrayOop) (_args))->obj_at(_index++);
  assert(expectedType == T_OBJECT || java_lang_boxing_object::is_instance(arg, expectedType), "arg type mismatch");
  return Handle(Thread::current(), arg);
}

// Bring the JVMCI compiler thread into the VM state.
#define JVMCI_VM_ENTRY_MARK                                       \
  MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));       \
  ThreadInVMfromNative __tiv(thread);                             \
  HandleMarkCleaner __hm(thread);                                 \
  JavaThread* THREAD = thread;                                        \
  debug_only(VMNativeEntryWrapper __vew;)

// Native method block that transitions current thread to '_thread_in_vm'.
#define C2V_BLOCK(result_type, name, signature)      \
  JVMCI_VM_ENTRY_MARK;                               \
  ResourceMark rm;                                   \
  JNI_JVMCIENV(JVMCI::compilation_tick(thread), env);

static JavaThread* get_current_thread(bool allow_null=true) {
  Thread* thread = Thread::current_or_null_safe();
  if (thread == NULL) {
    assert(allow_null, "npe");
    return NULL;
  }
  return JavaThread::cast(thread);
}

// Entry to native method implementation that transitions
// current thread to '_thread_in_vm'.
#define C2V_VMENTRY(result_type, name, signature)        \
  JNIEXPORT result_type JNICALL c2v_ ## name signature { \
  JavaThread* thread = get_current_thread();             \
  if (thread == NULL) {                                  \
    env->ThrowNew(JNIJVMCI::InternalError::clazz(),      \
        err_msg("Cannot call into HotSpot from JVMCI shared library without attaching current thread")); \
    return;                                              \
  }                                                      \
  JVMCITraceMark jtm("CompilerToVM::" #name);            \
  C2V_BLOCK(result_type, name, signature)

#define C2V_VMENTRY_(result_type, name, signature, result) \
  JNIEXPORT result_type JNICALL c2v_ ## name signature { \
  JavaThread* thread = get_current_thread();             \
  if (thread == NULL) {                                  \
    env->ThrowNew(JNIJVMCI::InternalError::clazz(),      \
        err_msg("Cannot call into HotSpot from JVMCI shared library without attaching current thread")); \
    return result;                                       \
  }                                                      \
  JVMCITraceMark jtm("CompilerToVM::" #name);            \
  C2V_BLOCK(result_type, name, signature)

#define C2V_VMENTRY_NULL(result_type, name, signature) C2V_VMENTRY_(result_type, name, signature, NULL)
#define C2V_VMENTRY_0(result_type, name, signature) C2V_VMENTRY_(result_type, name, signature, 0)

// Entry to native method implementation that does not transition
// current thread to '_thread_in_vm'.
#define C2V_VMENTRY_PREFIX(result_type, name, signature) \
  JNIEXPORT result_type JNICALL c2v_ ## name signature { \
  JavaThread* thread = get_current_thread();

#define C2V_END }

#define JNI_THROW(caller, name, msg) do {                                         \
    jint __throw_res = env->ThrowNew(JNIJVMCI::name::clazz(), msg);               \
    if (__throw_res != JNI_OK) {                                                  \
      tty->print_cr("Throwing " #name " in " caller " returned %d", __throw_res); \
    }                                                                             \
    return;                                                                       \
  } while (0);

#define JNI_THROW_(caller, name, msg, result) do {                                \
    jint __throw_res = env->ThrowNew(JNIJVMCI::name::clazz(), msg);               \
    if (__throw_res != JNI_OK) {                                                  \
      tty->print_cr("Throwing " #name " in " caller " returned %d", __throw_res); \
    }                                                                             \
    return result;                                                                \
  } while (0)

jobjectArray readConfiguration0(JNIEnv *env, JVMCI_TRAPS);

C2V_VMENTRY_NULL(jobjectArray, readConfiguration, (JNIEnv* env))
  jobjectArray config = readConfiguration0(env, JVMCI_CHECK_NULL);
  return config;
}

C2V_VMENTRY_NULL(jobject, getFlagValue, (JNIEnv* env, jobject c2vm, jobject name_handle))
#define RETURN_BOXED_LONG(value) jvalue p; p.j = (jlong) (value); JVMCIObject box = JVMCIENV->create_box(T_LONG, &p, JVMCI_CHECK_NULL); return box.as_jobject();
#define RETURN_BOXED_DOUBLE(value) jvalue p; p.d = (jdouble) (value); JVMCIObject box = JVMCIENV->create_box(T_DOUBLE, &p, JVMCI_CHECK_NULL); return box.as_jobject();
  JVMCIObject name = JVMCIENV->wrap(name_handle);
  if (name.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }
  const char* cstring = JVMCIENV->as_utf8_string(name);
  const JVMFlag* flag = JVMFlag::find_declared_flag(cstring);
  if (flag == NULL) {
    return c2vm;
  }
  if (flag->is_bool()) {
    jvalue prim;
    prim.z = flag->get_bool();
    JVMCIObject box = JVMCIENV->create_box(T_BOOLEAN, &prim, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobject(box);
  } else if (flag->is_ccstr()) {
    JVMCIObject value = JVMCIENV->create_string(flag->get_ccstr(), JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobject(value);
  } else if (flag->is_intx()) {
    RETURN_BOXED_LONG(flag->get_intx());
  } else if (flag->is_int()) {
    RETURN_BOXED_LONG(flag->get_int());
  } else if (flag->is_uint()) {
    RETURN_BOXED_LONG(flag->get_uint());
  } else if (flag->is_uint64_t()) {
    RETURN_BOXED_LONG(flag->get_uint64_t());
  } else if (flag->is_size_t()) {
    RETURN_BOXED_LONG(flag->get_size_t());
  } else if (flag->is_uintx()) {
    RETURN_BOXED_LONG(flag->get_uintx());
  } else if (flag->is_double()) {
    RETURN_BOXED_DOUBLE(flag->get_double());
  } else {
    JVMCI_ERROR_NULL("VM flag %s has unsupported type %s", flag->name(), flag->type_string());
  }
#undef RETURN_BOXED_LONG
#undef RETURN_BOXED_DOUBLE
C2V_END

C2V_VMENTRY_NULL(jbyteArray, getBytecode, (JNIEnv* env, jobject, jobject jvmci_method))
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));

  int code_size = method->code_size();
  jbyte* reconstituted_code = NEW_RESOURCE_ARRAY(jbyte, code_size);

  guarantee(method->method_holder()->is_rewritten(), "Method's holder should be rewritten");
  // iterate over all bytecodes and replace non-Java bytecodes

  for (BytecodeStream s(method); s.next() != Bytecodes::_illegal; ) {
    Bytecodes::Code code = s.code();
    Bytecodes::Code raw_code = s.raw_code();
    int bci = s.bci();
    int len = s.instruction_size();

    // Restore original byte code.
    reconstituted_code[bci] =  (jbyte) (s.is_wide()? Bytecodes::_wide : code);
    if (len > 1) {
      memcpy(reconstituted_code + (bci + 1), s.bcp()+1, len-1);
    }

    if (len > 1) {
      // Restore the big-endian constant pool indexes.
      // Cf. Rewriter::scan_method
      switch (code) {
        case Bytecodes::_getstatic:
        case Bytecodes::_putstatic:
        case Bytecodes::_getfield:
        case Bytecodes::_putfield:
        case Bytecodes::_invokevirtual:
        case Bytecodes::_invokespecial:
        case Bytecodes::_invokestatic:
        case Bytecodes::_invokeinterface:
        case Bytecodes::_invokehandle: {
          int cp_index = Bytes::get_native_u2((address) reconstituted_code + (bci + 1));
          Bytes::put_Java_u2((address) reconstituted_code + (bci + 1), (u2) cp_index);
          break;
        }

        case Bytecodes::_invokedynamic: {
          int cp_index = Bytes::get_native_u4((address) reconstituted_code + (bci + 1));
          Bytes::put_Java_u4((address) reconstituted_code + (bci + 1), (u4) cp_index);
          break;
        }

        default:
          break;
      }

      // Not all ldc byte code are rewritten.
      switch (raw_code) {
        case Bytecodes::_fast_aldc: {
          int cpc_index = reconstituted_code[bci + 1] & 0xff;
          int cp_index = method->constants()->object_to_cp_index(cpc_index);
          assert(cp_index < method->constants()->length(), "sanity check");
          reconstituted_code[bci + 1] = (jbyte) cp_index;
          break;
        }

        case Bytecodes::_fast_aldc_w: {
          int cpc_index = Bytes::get_native_u2((address) reconstituted_code + (bci + 1));
          int cp_index = method->constants()->object_to_cp_index(cpc_index);
          assert(cp_index < method->constants()->length(), "sanity check");
          Bytes::put_Java_u2((address) reconstituted_code + (bci + 1), (u2) cp_index);
          break;
        }

        default:
          break;
      }
    }
  }

  JVMCIPrimitiveArray result = JVMCIENV->new_byteArray(code_size, JVMCI_CHECK_NULL);
  JVMCIENV->copy_bytes_from(reconstituted_code, result, 0, code_size);
  return JVMCIENV->get_jbyteArray(result);
C2V_END

C2V_VMENTRY_0(jint, getExceptionTableLength, (JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  return method->exception_table_length();
C2V_END

C2V_VMENTRY_0(jlong, getExceptionTableStart, (JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  if (method->exception_table_length() == 0) {
    return 0L;
  }
  return (jlong) (address) method->exception_table_start();
C2V_END

C2V_VMENTRY_NULL(jobject, asResolvedJavaMethod, (JNIEnv* env, jobject, jobject executable_handle))
  requireInHotSpot("asResolvedJavaMethod", JVMCI_CHECK_NULL);
  oop executable = JNIHandles::resolve(executable_handle);
  oop mirror = NULL;
  int slot = 0;

  if (executable->klass() == vmClasses::reflect_Constructor_klass()) {
    mirror = java_lang_reflect_Constructor::clazz(executable);
    slot = java_lang_reflect_Constructor::slot(executable);
  } else {
    assert(executable->klass() == vmClasses::reflect_Method_klass(), "wrong type");
    mirror = java_lang_reflect_Method::clazz(executable);
    slot = java_lang_reflect_Method::slot(executable);
  }
  Klass* holder = java_lang_Class::as_Klass(mirror);
  methodHandle method (THREAD, InstanceKlass::cast(holder)->method_with_idnum(slot));
  JVMCIObject result = JVMCIENV->get_jvmci_method(method, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
}

C2V_VMENTRY_NULL(jobject, getResolvedJavaMethod, (JNIEnv* env, jobject, jobject base, jlong offset))
  Method* method = NULL;
  JVMCIObject base_object = JVMCIENV->wrap(base);
  if (base_object.is_null()) {
    method = *((Method**)(offset));
  } else if (JVMCIENV->isa_HotSpotObjectConstantImpl(base_object)) {
    Handle obj = JVMCIENV->asConstant(base_object, JVMCI_CHECK_NULL);
    if (obj->is_a(vmClasses::ResolvedMethodName_klass())) {
      method = (Method*) (intptr_t) obj->long_field(offset);
    } else {
      JVMCI_THROW_MSG_NULL(IllegalArgumentException, err_msg("Unexpected type: %s", obj->klass()->external_name()));
    }
  } else if (JVMCIENV->isa_HotSpotResolvedJavaMethodImpl(base_object)) {
    method = JVMCIENV->asMethod(base_object);
  }
  if (method == NULL) {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException, err_msg("Unexpected type: %s", JVMCIENV->klass_name(base_object)));
  }
  assert (method->is_method(), "invalid read");
  JVMCIObject result = JVMCIENV->get_jvmci_method(methodHandle(THREAD, method), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
}

C2V_VMENTRY_NULL(jobject, getConstantPool, (JNIEnv* env, jobject, jobject object_handle))
  ConstantPool* cp = NULL;
  JVMCIObject object = JVMCIENV->wrap(object_handle);
  if (object.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }
  if (JVMCIENV->isa_HotSpotResolvedJavaMethodImpl(object)) {
    cp = JVMCIENV->asMethod(object)->constMethod()->constants();
  } else if (JVMCIENV->isa_HotSpotResolvedObjectTypeImpl(object)) {
    cp = InstanceKlass::cast(JVMCIENV->asKlass(object))->constants();
  } else {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException,
                err_msg("Unexpected type: %s", JVMCIENV->klass_name(object)));
  }
  assert(cp != NULL, "npe");

  JVMCIObject result = JVMCIENV->get_jvmci_constant_pool(constantPoolHandle(THREAD, cp), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
}

C2V_VMENTRY_NULL(jobject, getResolvedJavaType0, (JNIEnv* env, jobject, jobject base, jlong offset, jboolean compressed))
  JVMCIKlassHandle klass(THREAD);
  JVMCIObject base_object = JVMCIENV->wrap(base);
  jlong base_address = 0;
  if (base_object.is_non_null() && offset == oopDesc::klass_offset_in_bytes()) {
    // klass = JVMCIENV->unhandle(base_object)->klass();
    if (JVMCIENV->isa_HotSpotObjectConstantImpl(base_object)) {
      Handle base_oop = JVMCIENV->asConstant(base_object, JVMCI_CHECK_NULL);
      klass = base_oop->klass();
    } else {
      assert(false, "What types are we actually expecting here?");
    }
  } else if (!compressed) {
    if (base_object.is_non_null()) {
      if (JVMCIENV->isa_HotSpotResolvedJavaMethodImpl(base_object)) {
        base_address = (intptr_t) JVMCIENV->asMethod(base_object);
      } else if (JVMCIENV->isa_HotSpotConstantPool(base_object)) {
        base_address = (intptr_t) JVMCIENV->asConstantPool(base_object);
      } else if (JVMCIENV->isa_HotSpotResolvedObjectTypeImpl(base_object)) {
        base_address = (intptr_t) JVMCIENV->asKlass(base_object);
      } else if (JVMCIENV->isa_HotSpotObjectConstantImpl(base_object)) {
        Handle base_oop = JVMCIENV->asConstant(base_object, JVMCI_CHECK_NULL);
        if (base_oop->is_a(vmClasses::Class_klass())) {
          base_address = cast_from_oop<jlong>(base_oop());
        }
      }
      if (base_address == 0) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException,
                    err_msg("Unexpected arguments: %s " JLONG_FORMAT " %s", JVMCIENV->klass_name(base_object), offset, compressed ? "true" : "false"));
      }
    }
    klass = *((Klass**) (intptr_t) (base_address + offset));
  } else {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException,
                err_msg("Unexpected arguments: %s " JLONG_FORMAT " %s",
                        base_object.is_non_null() ? JVMCIENV->klass_name(base_object) : "null",
                        offset, compressed ? "true" : "false"));
  }
  assert (klass == NULL || klass->is_klass(), "invalid read");
  JVMCIObject result = JVMCIENV->get_jvmci_type(klass, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
}

C2V_VMENTRY_NULL(jobject, findUniqueConcreteMethod, (JNIEnv* env, jobject, jobject jvmci_type, jobject jvmci_method))
  methodHandle method (THREAD, JVMCIENV->asMethod(jvmci_method));
  InstanceKlass* holder = InstanceKlass::cast(JVMCIENV->asKlass(jvmci_type));
  if (holder->is_interface()) {
    JVMCI_THROW_MSG_NULL(InternalError, err_msg("Interface %s should be handled in Java code", holder->external_name()));
  }
  if (method->can_be_statically_bound()) {
    JVMCI_THROW_MSG_NULL(InternalError, err_msg("Effectively static method %s.%s should be handled in Java code", method->method_holder()->external_name(), method->external_name()));
  }

  methodHandle ucm;
  {
    MutexLocker locker(Compile_lock);
    ucm = methodHandle(THREAD, Dependencies::find_unique_concrete_method(holder, method()));
  }
  JVMCIObject result = JVMCIENV->get_jvmci_method(ucm, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobject, getImplementor, (JNIEnv* env, jobject, jobject jvmci_type))
  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (!klass->is_interface()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(),
        err_msg("Expected interface type, got %s", klass->external_name()));
  }
  InstanceKlass* iklass = InstanceKlass::cast(klass);
  JVMCIKlassHandle handle(THREAD);
  {
    // Need Compile_lock around implementor()
    MutexLocker locker(Compile_lock);
    handle = iklass->implementor();
  }
  JVMCIObject implementor = JVMCIENV->get_jvmci_type(handle, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(implementor);
C2V_END

C2V_VMENTRY_0(jboolean, methodIsIgnoredBySecurityStackWalk,(JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  return method->is_ignored_by_security_stack_walk();
C2V_END

C2V_VMENTRY_0(jboolean, isCompilable,(JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  // Skip redefined methods
  if (method->is_old()) {
    return false;
  }
  return !method->is_not_compilable(CompLevel_full_optimization);
C2V_END

C2V_VMENTRY_0(jboolean, hasNeverInlineDirective,(JNIEnv* env, jobject, jobject jvmci_method))
  methodHandle method (THREAD, JVMCIENV->asMethod(jvmci_method));
  return !Inline || CompilerOracle::should_not_inline(method) || method->dont_inline();
C2V_END

C2V_VMENTRY_0(jboolean, shouldInlineMethod,(JNIEnv* env, jobject, jobject jvmci_method))
  methodHandle method (THREAD, JVMCIENV->asMethod(jvmci_method));
  return CompilerOracle::should_inline(method) || method->force_inline();
C2V_END

C2V_VMENTRY_NULL(jobject, lookupType, (JNIEnv* env, jobject, jstring jname, jclass accessing_class, jboolean resolve))
  JVMCIObject name = JVMCIENV->wrap(jname);
  const char* str = JVMCIENV->as_utf8_string(name);
  TempNewSymbol class_name = SymbolTable::new_symbol(str);

  if (class_name->utf8_length() <= 1) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Primitive type %s should be handled in Java code", class_name->as_C_string()));
  }

  JVMCIKlassHandle resolved_klass(THREAD);
  Klass* accessing_klass = NULL;
  Handle class_loader;
  Handle protection_domain;
  if (accessing_class != NULL) {
    accessing_klass = JVMCIENV->asKlass(accessing_class);
    class_loader = Handle(THREAD, accessing_klass->class_loader());
    protection_domain = Handle(THREAD, accessing_klass->protection_domain());
  } else {
    // Use the System class loader
    class_loader = Handle(THREAD, SystemDictionary::java_system_loader());
    JVMCIENV->runtime()->initialize(JVMCIENV);
  }

  if (resolve) {
    resolved_klass = SystemDictionary::resolve_or_null(class_name, class_loader, protection_domain, CHECK_NULL);
    if (resolved_klass == NULL) {
      JVMCI_THROW_MSG_NULL(ClassNotFoundException, str);
    }
  } else {
    if (Signature::has_envelope(class_name)) {
      // This is a name from a signature.  Strip off the trimmings.
      // Call recursive to keep scope of strippedsym.
      TempNewSymbol strippedsym = Signature::strip_envelope(class_name);
      resolved_klass = SystemDictionary::find_instance_klass(strippedsym,
                                                             class_loader,
                                                             protection_domain);
    } else if (Signature::is_array(class_name)) {
      SignatureStream ss(class_name, false);
      int ndim = ss.skip_array_prefix();
      if (ss.type() == T_OBJECT) {
        Symbol* strippedsym = ss.as_symbol();
        resolved_klass = SystemDictionary::find_instance_klass(strippedsym,
                                                               class_loader,
                                                               protection_domain);
        if (!resolved_klass.is_null()) {
          resolved_klass = resolved_klass->array_klass(ndim, CHECK_NULL);
        }
      } else {
        resolved_klass = TypeArrayKlass::cast(Universe::typeArrayKlassObj(ss.type()))->array_klass(ndim, CHECK_NULL);
      }
    } else {
      resolved_klass = SystemDictionary::find_instance_klass(class_name,
                                                             class_loader,
                                                             protection_domain);
    }
  }
  JVMCIObject result = JVMCIENV->get_jvmci_type(resolved_klass, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobject, getArrayType, (JNIEnv* env, jobject, jobject jvmci_type))
  if (jvmci_type == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }

  JVMCIObject jvmci_type_object = JVMCIENV->wrap(jvmci_type);
  JVMCIKlassHandle array_klass(THREAD);
  if (JVMCIENV->isa_HotSpotResolvedPrimitiveType(jvmci_type_object)) {
    BasicType type = JVMCIENV->kindToBasicType(JVMCIENV->get_HotSpotResolvedPrimitiveType_kind(jvmci_type_object), JVMCI_CHECK_0);
    if (type == T_VOID) {
      return NULL;
    }
    array_klass = Universe::typeArrayKlassObj(type);
    if (array_klass == NULL) {
      JVMCI_THROW_MSG_NULL(InternalError, err_msg("No array klass for primitive type %s", type2name(type)));
    }
  } else {
    Klass* klass = JVMCIENV->asKlass(jvmci_type);
    if (klass == NULL) {
      JVMCI_THROW_0(NullPointerException);
    }
    array_klass = klass->array_klass(CHECK_NULL);
  }
  JVMCIObject result = JVMCIENV->get_jvmci_type(array_klass, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobject, lookupClass, (JNIEnv* env, jobject, jclass mirror))
  requireInHotSpot("lookupClass", JVMCI_CHECK_NULL);
  if (mirror == NULL) {
    return NULL;
  }
  JVMCIKlassHandle klass(THREAD);
  klass = java_lang_Class::as_Klass(JNIHandles::resolve(mirror));
  if (klass == NULL) {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException, "Primitive classes are unsupported");
  }
  JVMCIObject result = JVMCIENV->get_jvmci_type(klass, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
}

C2V_VMENTRY_NULL(jobject, resolvePossiblyCachedConstantInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  oop obj = cp->resolve_possibly_cached_constant_at(index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (tag.is_dynamic_constant() || tag.is_dynamic_constant_in_error()) {
    if (obj == Universe::the_null_sentinel()) {
      return JVMCIENV->get_jobject(JVMCIENV->get_JavaConstant_NULL_POINTER());
    }
    BasicType bt = Signature::basic_type(cp->uncached_signature_ref_at(index));
    if (!is_reference_type(bt)) {
      if (!is_java_primitive(bt)) {
        return JVMCIENV->get_jobject(JVMCIENV->get_JavaConstant_ILLEGAL());
      }

      // Convert standard box (e.g. java.lang.Integer) to JVMCI box (e.g. jdk.vm.ci.meta.PrimitiveConstant)
      jvalue value;
      jlong raw_value;
      JVMCIObject kind;
      BasicType bt2 = java_lang_boxing_object::get_value(obj, &value);
      assert(bt2 == bt, "");
      switch (bt2) {
        case T_LONG:    kind = JVMCIENV->get_JavaKind_Long();    raw_value = value.j; break;
        case T_DOUBLE:  kind = JVMCIENV->get_JavaKind_Double();  raw_value = value.j; break;
        case T_FLOAT:   kind = JVMCIENV->get_JavaKind_Float();   raw_value = value.i; break;
        case T_INT:     kind = JVMCIENV->get_JavaKind_Int();     raw_value = value.i; break;
        case T_SHORT:   kind = JVMCIENV->get_JavaKind_Short();   raw_value = value.s; break;
        case T_BYTE:    kind = JVMCIENV->get_JavaKind_Byte();    raw_value = value.b; break;
        case T_CHAR:    kind = JVMCIENV->get_JavaKind_Char();    raw_value = value.c; break;
        case T_BOOLEAN: kind = JVMCIENV->get_JavaKind_Boolean(); raw_value = value.z; break;
        default:        return JVMCIENV->get_jobject(JVMCIENV->get_JavaConstant_ILLEGAL());
      }

      JVMCIObject result = JVMCIENV->call_JavaConstant_forPrimitive(kind, raw_value, JVMCI_CHECK_NULL);
      return JVMCIENV->get_jobject(result);
    }
  }
  return JVMCIENV->get_jobject(JVMCIENV->get_object_constant(obj));
C2V_END

C2V_VMENTRY_0(jint, lookupNameAndTypeRefIndexInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  return cp->name_and_type_ref_index_at(index);
C2V_END

C2V_VMENTRY_NULL(jobject, lookupNameInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint which))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  JVMCIObject sym = JVMCIENV->create_string(cp->name_ref_at(which), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(sym);
C2V_END

C2V_VMENTRY_NULL(jobject, lookupSignatureInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint which))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  JVMCIObject sym = JVMCIENV->create_string(cp->signature_ref_at(which), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(sym);
C2V_END

C2V_VMENTRY_0(jint, lookupKlassRefIndexInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  return cp->klass_ref_index_at(index);
C2V_END

C2V_VMENTRY_NULL(jobject, resolveTypeInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  Klass* klass = cp->klass_at(index, CHECK_NULL);
  JVMCIKlassHandle resolved_klass(THREAD, klass);
  if (resolved_klass->is_instance_klass()) {
    InstanceKlass::cast(resolved_klass())->link_class(CHECK_NULL);
    if (!InstanceKlass::cast(resolved_klass())->is_linked()) {
      // link_class() should not return here if there is an issue.
      JVMCI_THROW_MSG_NULL(InternalError, err_msg("Class %s must be linked", resolved_klass()->external_name()));
    }
  }
  JVMCIObject klassObject = JVMCIENV->get_jvmci_type(resolved_klass, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(klassObject);
C2V_END

C2V_VMENTRY_NULL(jobject, lookupKlassInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index, jbyte opcode))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  Klass* loading_klass = cp->pool_holder();
  bool is_accessible = false;
  JVMCIKlassHandle klass(THREAD, JVMCIRuntime::get_klass_by_index(cp, index, is_accessible, loading_klass));
  Symbol* symbol = NULL;
  if (klass.is_null()) {
    constantTag tag = cp->tag_at(index);
    if (tag.is_klass()) {
      // The klass has been inserted into the constant pool
      // very recently.
      klass = cp->resolved_klass_at(index);
    } else if (tag.is_symbol()) {
      symbol = cp->symbol_at(index);
    } else {
      assert(cp->tag_at(index).is_unresolved_klass(), "wrong tag");
      symbol = cp->klass_name_at(index);
    }
  }
  JVMCIObject result;
  if (!klass.is_null()) {
    result = JVMCIENV->get_jvmci_type(klass, JVMCI_CHECK_NULL);
  } else {
    result = JVMCIENV->create_string(symbol, JVMCI_CHECK_NULL);
  }
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobject, lookupAppendixInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  oop appendix_oop = ConstantPool::appendix_at_if_loaded(cp, index);
  return JVMCIENV->get_jobject(JVMCIENV->get_object_constant(appendix_oop));
C2V_END

C2V_VMENTRY_NULL(jobject, lookupMethodInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index, jbyte opcode))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  InstanceKlass* pool_holder = cp->pool_holder();
  Bytecodes::Code bc = (Bytecodes::Code) (((int) opcode) & 0xFF);
  methodHandle method(THREAD, JVMCIRuntime::get_method_by_index(cp, index, bc, pool_holder));
  JVMCIObject result = JVMCIENV->get_jvmci_method(method, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_0(jint, constantPoolRemapInstructionOperandFromCache, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  return cp->remap_instruction_operand_from_cache(index);
C2V_END

C2V_VMENTRY_NULL(jobject, resolveFieldInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index, jobject jvmci_method, jbyte opcode, jintArray info_handle))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  Bytecodes::Code code = (Bytecodes::Code)(((int) opcode) & 0xFF);
  fieldDescriptor fd;
  methodHandle mh(THREAD, (jvmci_method != NULL) ? JVMCIENV->asMethod(jvmci_method) : NULL);
  LinkInfo link_info(cp, index, mh, CHECK_NULL);
  LinkResolver::resolve_field(fd, link_info, Bytecodes::java_code(code), false, CHECK_NULL);
  JVMCIPrimitiveArray info = JVMCIENV->wrap(info_handle);
  if (info.is_null() || JVMCIENV->get_length(info) != 3) {
    JVMCI_ERROR_NULL("info must not be null and have a length of 3");
  }
  JVMCIENV->put_int_at(info, 0, fd.access_flags().as_int());
  JVMCIENV->put_int_at(info, 1, fd.offset());
  JVMCIENV->put_int_at(info, 2, fd.index());
  JVMCIKlassHandle handle(THREAD, fd.field_holder());
  JVMCIObject field_holder = JVMCIENV->get_jvmci_type(handle, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(field_holder);
C2V_END

C2V_VMENTRY_0(jint, getVtableIndexForInterfaceMethod, (JNIEnv* env, jobject, jobject jvmci_type, jobject jvmci_method))
  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  InstanceKlass* holder = method->method_holder();
  if (klass->is_interface()) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Interface %s should be handled in Java code", klass->external_name()));
  }
  if (!holder->is_interface()) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Method %s is not held by an interface, this case should be handled in Java code", method->name_and_sig_as_C_string()));
  }
  if (!klass->is_instance_klass()) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Class %s must be instance klass", klass->external_name()));
  }
  if (!InstanceKlass::cast(klass)->is_linked()) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Class %s must be linked", klass->external_name()));
  }
  if (!klass->is_subtype_of(holder)) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Class %s does not implement interface %s", klass->external_name(), holder->external_name()));
  }
  return LinkResolver::vtable_index_of_interface_method(klass, method);
C2V_END

C2V_VMENTRY_NULL(jobject, resolveMethod, (JNIEnv* env, jobject, jobject receiver_jvmci_type, jobject jvmci_method, jobject caller_jvmci_type))
  Klass* recv_klass = JVMCIENV->asKlass(receiver_jvmci_type);
  Klass* caller_klass = JVMCIENV->asKlass(caller_jvmci_type);
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));

  Klass* resolved     = method->method_holder();
  Symbol* h_name      = method->name();
  Symbol* h_signature = method->signature();

  if (MethodHandles::is_signature_polymorphic_method(method())) {
      // Signature polymorphic methods are already resolved, JVMCI just returns NULL in this case.
      return NULL;
  }

  if (method->name() == vmSymbols::clone_name() &&
      resolved == vmClasses::Object_klass() &&
      recv_klass->is_array_klass()) {
    // Resolution of the clone method on arrays always returns Object.clone even though that method
    // has protected access.  There's some trickery in the access checking to make this all work out
    // so it's necessary to pass in the array class as the resolved class to properly trigger this.
    // Otherwise it's impossible to resolve the array clone methods through JVMCI.  See
    // LinkResolver::check_method_accessability for the matching logic.
    resolved = recv_klass;
  }

  LinkInfo link_info(resolved, h_name, h_signature, caller_klass);
  Method* m = NULL;
  // Only do exact lookup if receiver klass has been linked.  Otherwise,
  // the vtable has not been setup, and the LinkResolver will fail.
  if (recv_klass->is_array_klass() ||
      (InstanceKlass::cast(recv_klass)->is_linked() && !recv_klass->is_interface())) {
    if (resolved->is_interface()) {
      m = LinkResolver::resolve_interface_call_or_null(recv_klass, link_info);
    } else {
      m = LinkResolver::resolve_virtual_call_or_null(recv_klass, link_info);
    }
  }

  if (m == NULL) {
    // Return NULL if there was a problem with lookup (uninitialized class, etc.)
    return NULL;
  }

  JVMCIObject result = JVMCIENV->get_jvmci_method(methodHandle(THREAD, m), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_0(jboolean, hasFinalizableSubclass,(JNIEnv* env, jobject, jobject jvmci_type))
  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  assert(klass != NULL, "method must not be called for primitive types");
  if (!klass->is_instance_klass()) {
    return false;
  }
  InstanceKlass* iklass = InstanceKlass::cast(klass);
  return Dependencies::find_finalizable_subclass(iklass) != NULL;
C2V_END

C2V_VMENTRY_NULL(jobject, getClassInitializer, (JNIEnv* env, jobject, jobject jvmci_type))
  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (!klass->is_instance_klass()) {
    return NULL;
  }
  InstanceKlass* iklass = InstanceKlass::cast(klass);
  methodHandle clinit(THREAD, iklass->class_initializer());
  JVMCIObject result = JVMCIENV->get_jvmci_method(clinit, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_0(jlong, getMaxCallTargetOffset, (JNIEnv* env, jobject, jlong addr))
  address target_addr = (address) addr;
  if (target_addr != 0x0) {
    int64_t off_low = (int64_t)target_addr - ((int64_t)CodeCache::low_bound() + sizeof(int));
    int64_t off_high = (int64_t)target_addr - ((int64_t)CodeCache::high_bound() + sizeof(int));
    return MAX2(ABS(off_low), ABS(off_high));
  }
  return -1;
C2V_END

C2V_VMENTRY(void, setNotInlinableOrCompilable,(JNIEnv* env, jobject,  jobject jvmci_method))
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  method->set_not_c1_compilable();
  method->set_not_c2_compilable();
  method->set_dont_inline(true);
C2V_END

C2V_VMENTRY_0(jint, installCode, (JNIEnv *env, jobject, jobject target, jobject compiled_code,
            jobject installed_code, jlong failed_speculations_address, jbyteArray speculations_obj))
  HandleMark hm(THREAD);
  JNIHandleMark jni_hm(thread);

  JVMCIObject target_handle = JVMCIENV->wrap(target);
  JVMCIObject compiled_code_handle = JVMCIENV->wrap(compiled_code);
  CodeBlob* cb = NULL;
  JVMCIObject installed_code_handle = JVMCIENV->wrap(installed_code);
  JVMCIPrimitiveArray speculations_handle = JVMCIENV->wrap(speculations_obj);

  int speculations_len = JVMCIENV->get_length(speculations_handle);
  char* speculations = NEW_RESOURCE_ARRAY(char, speculations_len);
  JVMCIENV->copy_bytes_to(speculations_handle, (jbyte*) speculations, 0, speculations_len);

  JVMCICompiler* compiler = JVMCICompiler::instance(true, CHECK_JNI_ERR);

  TraceTime install_time("installCode", JVMCICompiler::codeInstallTimer(!thread->is_Compiler_thread()));

  nmethodLocker nmethod_handle;
  CodeInstaller installer(JVMCIENV);
  JVMCI::CodeInstallResult result = installer.install(compiler,
      target_handle,
      compiled_code_handle,
      cb,
      nmethod_handle,
      installed_code_handle,
      (FailedSpeculation**)(address) failed_speculations_address,
      speculations,
      speculations_len,
      JVMCI_CHECK_0);

  if (PrintCodeCacheOnCompilation) {
    stringStream s;
    // Dump code cache into a buffer before locking the tty,
    {
      MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      CodeCache::print_summary(&s, false);
    }
    ttyLocker ttyl;
    tty->print_raw_cr(s.as_string());
  }

  if (result != JVMCI::ok) {
    assert(cb == NULL, "should be");
  } else {
    if (installed_code_handle.is_non_null()) {
      if (cb->is_nmethod()) {
        assert(JVMCIENV->isa_HotSpotNmethod(installed_code_handle), "wrong type");
        // Clear the link to an old nmethod first
        JVMCIObject nmethod_mirror = installed_code_handle;
        JVMCIENV->invalidate_nmethod_mirror(nmethod_mirror, JVMCI_CHECK_0);
      } else {
        assert(JVMCIENV->isa_InstalledCode(installed_code_handle), "wrong type");
      }
      // Initialize the link to the new code blob
      JVMCIENV->initialize_installed_code(installed_code_handle, cb, JVMCI_CHECK_0);
    }
  }
  return result;
C2V_END

C2V_VMENTRY_0(jint, getMetadata, (JNIEnv *env, jobject, jobject target, jobject compiled_code, jobject metadata))
  JVMCI_THROW_MSG_0(InternalError, "unimplemented");
C2V_END

C2V_VMENTRY(void, resetCompilationStatistics, (JNIEnv* env, jobject))
  JVMCICompiler* compiler = JVMCICompiler::instance(true, CHECK);
  CompilerStatistics* stats = compiler->stats();
  stats->_standard.reset();
  stats->_osr.reset();
C2V_END

C2V_VMENTRY_NULL(jobject, disassembleCodeBlob, (JNIEnv* env, jobject, jobject installedCode))
  HandleMark hm(THREAD);

  if (installedCode == NULL) {
    JVMCI_THROW_MSG_NULL(NullPointerException, "installedCode is null");
  }

  JVMCIObject installedCodeObject = JVMCIENV->wrap(installedCode);
  nmethodLocker locker;
  CodeBlob* cb = JVMCIENV->get_code_blob(installedCodeObject, locker);
  if (cb == NULL) {
    return NULL;
  }

  // We don't want the stringStream buffer to resize during disassembly as it
  // uses scoped resource memory. If a nested function called during disassembly uses
  // a ResourceMark and the buffer expands within the scope of the mark,
  // the buffer becomes garbage when that scope is exited. Experience shows that
  // the disassembled code is typically about 10x the code size so a fixed buffer
  // sized to 20x code size plus a fixed amount for header info should be sufficient.
  int bufferSize = cb->code_size() * 20 + 1024;
  char* buffer = NEW_RESOURCE_ARRAY(char, bufferSize);
  stringStream st(buffer, bufferSize);
  if (cb->is_nmethod()) {
    nmethod* nm = (nmethod*) cb;
    if (!nm->is_alive()) {
      return NULL;
    }
  }
  Disassembler::decode(cb, &st);
  if (st.size() <= 0) {
    return NULL;
  }

  JVMCIObject result = JVMCIENV->create_string(st.as_string(), JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobject, getStackTraceElement, (JNIEnv* env, jobject, jobject jvmci_method, int bci))
  HandleMark hm(THREAD);

  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  JVMCIObject element = JVMCIENV->new_StackTraceElement(method, bci, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(element);
C2V_END

C2V_VMENTRY_NULL(jobject, executeHotSpotNmethod, (JNIEnv* env, jobject, jobject args, jobject hs_nmethod))
  // The incoming arguments array would have to contain JavaConstants instead of regular objects
  // and the return value would have to be wrapped as a JavaConstant.
  requireInHotSpot("executeHotSpotNmethod", JVMCI_CHECK_NULL);

  HandleMark hm(THREAD);

  JVMCIObject nmethod_mirror = JVMCIENV->wrap(hs_nmethod);
  nmethodLocker locker;
  nmethod* nm = JVMCIENV->get_nmethod(nmethod_mirror, locker);
  if (nm == NULL || !nm->is_in_use()) {
    JVMCI_THROW_NULL(InvalidInstalledCodeException);
  }
  methodHandle mh(THREAD, nm->method());
  Symbol* signature = mh->signature();
  JavaCallArguments jca(mh->size_of_parameters());

  JavaArgumentUnboxer jap(signature, &jca, (arrayOop) JNIHandles::resolve(args), mh->is_static());
  JavaValue result(jap.return_type());
  jca.set_alternative_target(Handle(THREAD, JNIHandles::resolve(nmethod_mirror.as_jobject())));
  JavaCalls::call(&result, mh, &jca, CHECK_NULL);

  if (jap.return_type() == T_VOID) {
    return NULL;
  } else if (is_reference_type(jap.return_type())) {
    return JNIHandles::make_local(THREAD, result.get_oop());
  } else {
    jvalue *value = (jvalue *) result.get_value_addr();
    // Narrow the value down if required (Important on big endian machines)
    switch (jap.return_type()) {
      case T_BOOLEAN:
       value->z = (jboolean) value->i;
       break;
      case T_BYTE:
       value->b = (jbyte) value->i;
       break;
      case T_CHAR:
       value->c = (jchar) value->i;
       break;
      case T_SHORT:
       value->s = (jshort) value->i;
       break;
      default:
        break;
    }
    JVMCIObject o = JVMCIENV->create_box(jap.return_type(), value, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobject(o);
  }
C2V_END

C2V_VMENTRY_NULL(jlongArray, getLineNumberTable, (JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  if (!method->has_linenumber_table()) {
    return NULL;
  }
  u2 num_entries = 0;
  CompressedLineNumberReadStream streamForSize(method->compressed_linenumber_table());
  while (streamForSize.read_pair()) {
    num_entries++;
  }

  CompressedLineNumberReadStream stream(method->compressed_linenumber_table());
  JVMCIPrimitiveArray result = JVMCIENV->new_longArray(2 * num_entries, JVMCI_CHECK_NULL);

  int i = 0;
  jlong value;
  while (stream.read_pair()) {
    value = ((long) stream.bci());
    JVMCIENV->put_long_at(result, i, value);
    value = ((long) stream.line());
    JVMCIENV->put_long_at(result, i + 1, value);
    i += 2;
  }

  return (jlongArray) JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_0(jlong, getLocalVariableTableStart, (JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  if (!method->has_localvariable_table()) {
    return 0;
  }
  return (jlong) (address) method->localvariable_table_start();
C2V_END

C2V_VMENTRY_0(jint, getLocalVariableTableLength, (JNIEnv* env, jobject, jobject jvmci_method))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  return method->localvariable_table_length();
C2V_END

C2V_VMENTRY(void, reprofile, (JNIEnv* env, jobject, jobject jvmci_method))
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  MethodCounters* mcs = method->method_counters();
  if (mcs != NULL) {
    mcs->clear_counters();
  }
  NOT_PRODUCT(method->set_compiled_invocation_count(0));

  CompiledMethod* code = method->code();
  if (code != NULL) {
    code->make_not_entrant();
  }

  MethodData* method_data = method->method_data();
  if (method_data == NULL) {
    ClassLoaderData* loader_data = method->method_holder()->class_loader_data();
    method_data = MethodData::allocate(loader_data, method, CHECK);
    method->set_method_data(method_data);
  } else {
    method_data->initialize();
  }
C2V_END


C2V_VMENTRY(void, invalidateHotSpotNmethod, (JNIEnv* env, jobject, jobject hs_nmethod))
  JVMCIObject nmethod_mirror = JVMCIENV->wrap(hs_nmethod);
  JVMCIENV->invalidate_nmethod_mirror(nmethod_mirror, JVMCI_CHECK);
C2V_END

C2V_VMENTRY_NULL(jlongArray, collectCounters, (JNIEnv* env, jobject))
  // Returns a zero length array if counters aren't enabled
  JVMCIPrimitiveArray array = JVMCIENV->new_longArray(JVMCICounterSize, JVMCI_CHECK_NULL);
  if (JVMCICounterSize > 0) {
    jlong* temp_array = NEW_RESOURCE_ARRAY(jlong, JVMCICounterSize);
    JavaThread::collect_counters(temp_array, JVMCICounterSize);
    JVMCIENV->copy_longs_from(temp_array, array, 0, JVMCICounterSize);
  }
  return (jlongArray) JVMCIENV->get_jobject(array);
C2V_END

C2V_VMENTRY_0(jint, getCountersSize, (JNIEnv* env, jobject))
  return (jint) JVMCICounterSize;
C2V_END

C2V_VMENTRY_0(jboolean, setCountersSize, (JNIEnv* env, jobject, jint new_size))
  return JavaThread::resize_all_jvmci_counters(new_size);
C2V_END

C2V_VMENTRY_0(jint, allocateCompileId, (JNIEnv* env, jobject, jobject jvmci_method, int entry_bci))
  HandleMark hm(THREAD);
  if (jvmci_method == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  if (entry_bci >= method->code_size() || entry_bci < -1) {
    JVMCI_THROW_MSG_0(IllegalArgumentException, err_msg("Unexpected bci %d", entry_bci));
  }
  return CompileBroker::assign_compile_id_unlocked(THREAD, method, entry_bci);
C2V_END


C2V_VMENTRY_0(jboolean, isMature, (JNIEnv* env, jobject, jlong metaspace_method_data))
  MethodData* mdo = JVMCIENV->asMethodData(metaspace_method_data);
  return mdo != NULL && mdo->is_mature();
C2V_END

C2V_VMENTRY_0(jboolean, hasCompiledCodeForOSR, (JNIEnv* env, jobject, jobject jvmci_method, int entry_bci, int comp_level))
  Method* method = JVMCIENV->asMethod(jvmci_method);
  return method->lookup_osr_nmethod_for(entry_bci, comp_level, true) != NULL;
C2V_END

C2V_VMENTRY_NULL(jobject, getSymbol, (JNIEnv* env, jobject, jlong symbol))
  JVMCIObject sym = JVMCIENV->create_string((Symbol*)(address)symbol, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(sym);
C2V_END

/*
 * Used by matches() to convert a ResolvedJavaMethod[] to an array of Method*.
 */
GrowableArray<Method*>* init_resolved_methods(jobjectArray methods, JVMCIEnv* JVMCIENV) {
  objArrayOop methods_oop = (objArrayOop) JNIHandles::resolve(methods);
  GrowableArray<Method*>* resolved_methods = new GrowableArray<Method*>(methods_oop->length());
  for (int i = 0; i < methods_oop->length(); i++) {
    oop resolved = methods_oop->obj_at(i);
    Method* resolved_method = NULL;
    if (resolved->klass() == HotSpotJVMCI::HotSpotResolvedJavaMethodImpl::klass()) {
      resolved_method = HotSpotJVMCI::asMethod(JVMCIENV, resolved);
    }
    resolved_methods->append(resolved_method);
  }
  return resolved_methods;
}

/*
 * Used by c2v_iterateFrames to check if `method` matches one of the ResolvedJavaMethods in the `methods` array.
 * The ResolvedJavaMethod[] array is converted to a Method* array that is then cached in the resolved_methods_ref in/out parameter.
 * In case of a match, the matching ResolvedJavaMethod is returned in matched_jvmci_method_ref.
 */
bool matches(jobjectArray methods, Method* method, GrowableArray<Method*>** resolved_methods_ref, Handle* matched_jvmci_method_ref, Thread* THREAD, JVMCIEnv* JVMCIENV) {
  GrowableArray<Method*>* resolved_methods = *resolved_methods_ref;
  if (resolved_methods == NULL) {
    resolved_methods = init_resolved_methods(methods, JVMCIENV);
    *resolved_methods_ref = resolved_methods;
  }
  assert(method != NULL, "method should not be NULL");
  assert(resolved_methods->length() == ((objArrayOop) JNIHandles::resolve(methods))->length(), "arrays must have the same length");
  for (int i = 0; i < resolved_methods->length(); i++) {
    Method* m = resolved_methods->at(i);
    if (m == method) {
      *matched_jvmci_method_ref = Handle(THREAD, ((objArrayOop) JNIHandles::resolve(methods))->obj_at(i));
      return true;
    }
  }
  return false;
}

/*
 * Resolves an interface call to a concrete method handle.
 */
methodHandle resolve_interface_call(Klass* spec_klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS) {
  CallInfo callinfo;
  Handle receiver = args->receiver();
  Klass* recvrKlass = receiver.is_null() ? (Klass*)NULL : receiver->klass();
  LinkInfo link_info(spec_klass, name, signature);
  LinkResolver::resolve_interface_call(
          callinfo, receiver, recvrKlass, link_info, true, CHECK_(methodHandle()));
  methodHandle method(THREAD, callinfo.selected_method());
  assert(method.not_null(), "should have thrown exception");
  return method;
}

/*
 * Used by c2v_iterateFrames to make a new vframeStream at the given compiled frame id (stack pointer) and vframe id.
 */
void resync_vframestream_to_compiled_frame(vframeStream& vfst, intptr_t* stack_pointer, int vframe_id, JavaThread* thread, TRAPS) {
  vfst = vframeStream(thread);
  while (vfst.frame_id() != stack_pointer && !vfst.at_end()) {
    vfst.next();
  }
  if (vfst.frame_id() != stack_pointer) {
    THROW_MSG(vmSymbols::java_lang_IllegalStateException(), "stack frame not found after deopt")
  }
  if (vfst.is_interpreted_frame()) {
    THROW_MSG(vmSymbols::java_lang_IllegalStateException(), "compiled stack frame expected")
  }
  while (vfst.vframe_id() != vframe_id) {
    if (vfst.at_end()) {
      THROW_MSG(vmSymbols::java_lang_IllegalStateException(), "vframe not found after deopt")
    }
    vfst.next();
    assert(!vfst.is_interpreted_frame(), "Wrong frame type");
  }
}

/*
 * Used by c2v_iterateFrames. Returns an array of any unallocated scope objects or NULL if none.
 */
GrowableArray<ScopeValue*>* get_unallocated_objects_or_null(GrowableArray<ScopeValue*>* scope_objects) {
  GrowableArray<ScopeValue*>* unallocated = NULL;
  for (int i = 0; i < scope_objects->length(); i++) {
    ObjectValue* sv = (ObjectValue*) scope_objects->at(i);
    if (sv->value().is_null()) {
      if (unallocated == NULL) {
        unallocated = new GrowableArray<ScopeValue*>(scope_objects->length());
      }
      unallocated->append(sv);
    }
  }
  return unallocated;
}

C2V_VMENTRY_NULL(jobject, iterateFrames, (JNIEnv* env, jobject compilerToVM, jobjectArray initial_methods, jobjectArray match_methods, jint initialSkip, jobject visitor_handle))

  if (!thread->has_last_Java_frame()) {
    return NULL;
  }
  Handle visitor(THREAD, JNIHandles::resolve_non_null(visitor_handle));

  requireInHotSpot("iterateFrames", JVMCI_CHECK_NULL);

  HotSpotJVMCI::HotSpotStackFrameReference::klass()->initialize(CHECK_NULL);

  vframeStream vfst(thread);
  jobjectArray methods = initial_methods;
  methodHandle visitor_method;
  GrowableArray<Method*>* resolved_methods = NULL;

  while (!vfst.at_end()) { // frame loop
    bool realloc_called = false;
    intptr_t* frame_id = vfst.frame_id();

    // Previous compiledVFrame of this frame; use with at_scope() to reuse scope object pool.
    compiledVFrame* prev_cvf = NULL;

    for (; !vfst.at_end() && vfst.frame_id() == frame_id; vfst.next()) { // vframe loop
      int frame_number = 0;
      Method *method = vfst.method();
      int bci = vfst.bci();

      Handle matched_jvmci_method;
      if (methods == NULL || matches(methods, method, &resolved_methods, &matched_jvmci_method, THREAD, JVMCIENV)) {
        if (initialSkip > 0) {
          initialSkip--;
          continue;
        }
        javaVFrame* vf;
        if (prev_cvf != NULL && prev_cvf->frame_pointer()->id() == frame_id) {
          assert(prev_cvf->is_compiled_frame(), "expected compiled Java frame");
          vf = prev_cvf->at_scope(vfst.decode_offset(), vfst.vframe_id());
        } else {
          vf = vfst.asJavaVFrame();
        }

        StackValueCollection* locals = NULL;
        typeArrayHandle localIsVirtual_h;
        if (vf->is_compiled_frame()) {
          // compiled method frame
          compiledVFrame* cvf = compiledVFrame::cast(vf);

          ScopeDesc* scope = cvf->scope();
          // native wrappers do not have a scope
          if (scope != NULL && scope->objects() != NULL) {
            prev_cvf = cvf;

            GrowableArray<ScopeValue*>* objects = NULL;
            if (!realloc_called) {
              objects = scope->objects();
            } else {
              // some object might already have been re-allocated, only reallocate the non-allocated ones
              objects = get_unallocated_objects_or_null(scope->objects());
            }

            if (objects != NULL) {
              RegisterMap reg_map(vf->register_map());
              bool realloc_failures = Deoptimization::realloc_objects(thread, vf->frame_pointer(), &reg_map, objects, CHECK_NULL);
              Deoptimization::reassign_fields(vf->frame_pointer(), &reg_map, objects, realloc_failures, false);
              realloc_called = true;
            }

            GrowableArray<ScopeValue*>* local_values = scope->locals();
            for (int i = 0; i < local_values->length(); i++) {
              ScopeValue* value = local_values->at(i);
              if (value->is_object()) {
                if (localIsVirtual_h.is_null()) {
                  typeArrayOop array_oop = oopFactory::new_boolArray(local_values->length(), CHECK_NULL);
                  localIsVirtual_h = typeArrayHandle(THREAD, array_oop);
                }
                localIsVirtual_h->bool_at_put(i, true);
              }
            }
          }

          locals = cvf->locals();
          frame_number = cvf->vframe_id();
        } else {
          // interpreted method frame
          interpretedVFrame* ivf = interpretedVFrame::cast(vf);

          locals = ivf->locals();
        }
        assert(bci == vf->bci(), "wrong bci");
        assert(method == vf->method(), "wrong method");

        Handle frame_reference = HotSpotJVMCI::HotSpotStackFrameReference::klass()->allocate_instance_handle(CHECK_NULL);
        HotSpotJVMCI::HotSpotStackFrameReference::set_bci(JVMCIENV, frame_reference(), bci);
        if (matched_jvmci_method.is_null()) {
          methodHandle mh(THREAD, method);
          JVMCIObject jvmci_method = JVMCIENV->get_jvmci_method(mh, JVMCI_CHECK_NULL);
          matched_jvmci_method = Handle(THREAD, JNIHandles::resolve(jvmci_method.as_jobject()));
        }
        HotSpotJVMCI::HotSpotStackFrameReference::set_method(JVMCIENV, frame_reference(), matched_jvmci_method());
        HotSpotJVMCI::HotSpotStackFrameReference::set_localIsVirtual(JVMCIENV, frame_reference(), localIsVirtual_h());

        HotSpotJVMCI::HotSpotStackFrameReference::set_compilerToVM(JVMCIENV, frame_reference(), JNIHandles::resolve(compilerToVM));
        HotSpotJVMCI::HotSpotStackFrameReference::set_stackPointer(JVMCIENV, frame_reference(), (jlong) frame_id);
        HotSpotJVMCI::HotSpotStackFrameReference::set_frameNumber(JVMCIENV, frame_reference(), frame_number);

        // initialize the locals array
        objArrayOop array_oop = oopFactory::new_objectArray(locals->size(), CHECK_NULL);
        objArrayHandle array(THREAD, array_oop);
        for (int i = 0; i < locals->size(); i++) {
          StackValue* var = locals->at(i);
          if (var->type() == T_OBJECT) {
            array->obj_at_put(i, locals->at(i)->get_obj()());
          }
        }
        HotSpotJVMCI::HotSpotStackFrameReference::set_locals(JVMCIENV, frame_reference(), array());
        HotSpotJVMCI::HotSpotStackFrameReference::set_objectsMaterialized(JVMCIENV, frame_reference(), JNI_FALSE);

        JavaValue result(T_OBJECT);
        JavaCallArguments args(visitor);
        if (visitor_method.is_null()) {
          visitor_method = resolve_interface_call(HotSpotJVMCI::InspectedFrameVisitor::klass(), vmSymbols::visitFrame_name(), vmSymbols::visitFrame_signature(), &args, CHECK_NULL);
        }

        args.push_oop(frame_reference);
        JavaCalls::call(&result, visitor_method, &args, CHECK_NULL);
        if (result.get_oop() != NULL) {
          return JNIHandles::make_local(thread, result.get_oop());
        }
        if (methods == initial_methods) {
          methods = match_methods;
          if (resolved_methods != NULL && JNIHandles::resolve(match_methods) != JNIHandles::resolve(initial_methods)) {
            resolved_methods = NULL;
          }
        }
        assert(initialSkip == 0, "There should be no match before initialSkip == 0");
        if (HotSpotJVMCI::HotSpotStackFrameReference::objectsMaterialized(JVMCIENV, frame_reference()) == JNI_TRUE) {
          // the frame has been deoptimized, we need to re-synchronize the frame and vframe
          prev_cvf = NULL;
          intptr_t* stack_pointer = (intptr_t*) HotSpotJVMCI::HotSpotStackFrameReference::stackPointer(JVMCIENV, frame_reference());
          resync_vframestream_to_compiled_frame(vfst, stack_pointer, frame_number, thread, CHECK_NULL);
        }
      }
    } // end of vframe loop
  } // end of frame loop

  // the end was reached without finding a matching method
  return NULL;
C2V_END

C2V_VMENTRY(void, resolveInvokeDynamicInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  CallInfo callInfo;
  LinkResolver::resolve_invoke(callInfo, Handle(), cp, index, Bytecodes::_invokedynamic, CHECK);
  ConstantPoolCacheEntry* cp_cache_entry = cp->invokedynamic_cp_cache_entry_at(index);
  cp_cache_entry->set_dynamic_call(cp, callInfo);
C2V_END

C2V_VMENTRY(void, resolveInvokeHandleInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  Klass* holder = cp->klass_ref_at(index, CHECK);
  Symbol* name = cp->name_ref_at(index);
  if (MethodHandles::is_signature_polymorphic_name(holder, name)) {
    CallInfo callInfo;
    LinkResolver::resolve_invoke(callInfo, Handle(), cp, index, Bytecodes::_invokehandle, CHECK);
    ConstantPoolCacheEntry* cp_cache_entry = cp->cache()->entry_at(cp->decode_cpcache_index(index));
    cp_cache_entry->set_method_handle(cp, callInfo);
  }
C2V_END

C2V_VMENTRY_0(jint, isResolvedInvokeHandleInPool, (JNIEnv* env, jobject, jobject jvmci_constant_pool, jint index))
  constantPoolHandle cp(THREAD, JVMCIENV->asConstantPool(jvmci_constant_pool));
  ConstantPoolCacheEntry* cp_cache_entry = cp->cache()->entry_at(cp->decode_cpcache_index(index));
  if (cp_cache_entry->is_resolved(Bytecodes::_invokehandle)) {
    // MethodHandle.invoke* --> LambdaForm?
    ResourceMark rm;

    LinkInfo link_info(cp, index, CATCH);

    Klass* resolved_klass = link_info.resolved_klass();

    Symbol* name_sym = cp->name_ref_at(index);

    vmassert(MethodHandles::is_method_handle_invoke_name(resolved_klass, name_sym), "!");
    vmassert(MethodHandles::is_signature_polymorphic_name(resolved_klass, name_sym), "!");

    methodHandle adapter_method(THREAD, cp_cache_entry->f1_as_method());

    methodHandle resolved_method(adapter_method);

    // Can we treat it as a regular invokevirtual?
    if (resolved_method->method_holder() == resolved_klass && resolved_method->name() == name_sym) {
      vmassert(!resolved_method->is_static(),"!");
      vmassert(MethodHandles::is_signature_polymorphic_method(resolved_method()),"!");
      vmassert(!MethodHandles::is_signature_polymorphic_static(resolved_method->intrinsic_id()), "!");
      vmassert(cp_cache_entry->appendix_if_resolved(cp) == NULL, "!");

      methodHandle m(THREAD, LinkResolver::linktime_resolve_virtual_method_or_null(link_info));
      vmassert(m == resolved_method, "!!");
      return -1;
    }

    return Bytecodes::_invokevirtual;
  }
  if (cp_cache_entry->is_resolved(Bytecodes::_invokedynamic)) {
    return Bytecodes::_invokedynamic;
  }
  return -1;
C2V_END


C2V_VMENTRY_NULL(jobject, getSignaturePolymorphicHolders, (JNIEnv* env, jobject))
  JVMCIObjectArray holders = JVMCIENV->new_String_array(2, JVMCI_CHECK_NULL);
  JVMCIObject mh = JVMCIENV->create_string("Ljava/lang/invoke/MethodHandle;", JVMCI_CHECK_NULL);
  JVMCIObject vh = JVMCIENV->create_string("Ljava/lang/invoke/VarHandle;", JVMCI_CHECK_NULL);
  JVMCIENV->put_object_at(holders, 0, mh);
  JVMCIENV->put_object_at(holders, 1, vh);
  return JVMCIENV->get_jobject(holders);
C2V_END

C2V_VMENTRY_0(jboolean, shouldDebugNonSafepoints, (JNIEnv* env, jobject))
  //see compute_recording_non_safepoints in debugInfroRec.cpp
  if (JvmtiExport::should_post_compiled_method_load() && FLAG_IS_DEFAULT(DebugNonSafepoints)) {
    return true;
  }
  return DebugNonSafepoints;
C2V_END

// public native void materializeVirtualObjects(HotSpotStackFrameReference stackFrame, boolean invalidate);
C2V_VMENTRY(void, materializeVirtualObjects, (JNIEnv* env, jobject, jobject _hs_frame, bool invalidate))
  JVMCIObject hs_frame = JVMCIENV->wrap(_hs_frame);
  if (hs_frame.is_null()) {
    JVMCI_THROW_MSG(NullPointerException, "stack frame is null");
  }

  requireInHotSpot("materializeVirtualObjects", JVMCI_CHECK);

  JVMCIENV->HotSpotStackFrameReference_initialize(JVMCI_CHECK);

  // look for the given stack frame
  StackFrameStream fst(thread, false /* update */, true /* process_frames */);
  intptr_t* stack_pointer = (intptr_t*) JVMCIENV->get_HotSpotStackFrameReference_stackPointer(hs_frame);
  while (fst.current()->id() != stack_pointer && !fst.is_done()) {
    fst.next();
  }
  if (fst.current()->id() != stack_pointer) {
    JVMCI_THROW_MSG(IllegalStateException, "stack frame not found");
  }

  if (invalidate) {
    if (!fst.current()->is_compiled_frame()) {
      JVMCI_THROW_MSG(IllegalStateException, "compiled stack frame expected");
    }
    assert(fst.current()->cb()->is_nmethod(), "nmethod expected");
    ((nmethod*) fst.current()->cb())->make_not_entrant();
  }
  Deoptimization::deoptimize(thread, *fst.current(), Deoptimization::Reason_none);
  // look for the frame again as it has been updated by deopt (pc, deopt state...)
  StackFrameStream fstAfterDeopt(thread, true /* update */, true /* process_frames */);
  while (fstAfterDeopt.current()->id() != stack_pointer && !fstAfterDeopt.is_done()) {
    fstAfterDeopt.next();
  }
  if (fstAfterDeopt.current()->id() != stack_pointer) {
    JVMCI_THROW_MSG(IllegalStateException, "stack frame not found after deopt");
  }

  vframe* vf = vframe::new_vframe(fstAfterDeopt.current(), fstAfterDeopt.register_map(), thread);
  if (!vf->is_compiled_frame()) {
    JVMCI_THROW_MSG(IllegalStateException, "compiled stack frame expected");
  }

  GrowableArray<compiledVFrame*>* virtualFrames = new GrowableArray<compiledVFrame*>(10);
  while (true) {
    assert(vf->is_compiled_frame(), "Wrong frame type");
    virtualFrames->push(compiledVFrame::cast(vf));
    if (vf->is_top()) {
      break;
    }
    vf = vf->sender();
  }

  int last_frame_number = JVMCIENV->get_HotSpotStackFrameReference_frameNumber(hs_frame);
  if (last_frame_number >= virtualFrames->length()) {
    JVMCI_THROW_MSG(IllegalStateException, "invalid frame number");
  }

  // Reallocate the non-escaping objects and restore their fields.
  assert (virtualFrames->at(last_frame_number)->scope() != NULL,"invalid scope");
  GrowableArray<ScopeValue*>* objects = virtualFrames->at(last_frame_number)->scope()->objects();

  if (objects == NULL) {
    // no objects to materialize
    return;
  }

  bool realloc_failures = Deoptimization::realloc_objects(thread, fstAfterDeopt.current(), fstAfterDeopt.register_map(), objects, CHECK);
  Deoptimization::reassign_fields(fstAfterDeopt.current(), fstAfterDeopt.register_map(), objects, realloc_failures, false);

  for (int frame_index = 0; frame_index < virtualFrames->length(); frame_index++) {
    compiledVFrame* cvf = virtualFrames->at(frame_index);

    GrowableArray<ScopeValue*>* scopeLocals = cvf->scope()->locals();
    StackValueCollection* locals = cvf->locals();
    if (locals != NULL) {
      for (int i2 = 0; i2 < locals->size(); i2++) {
        StackValue* var = locals->at(i2);
        if (var->type() == T_OBJECT && scopeLocals->at(i2)->is_object()) {
          jvalue val;
          val.l = cast_from_oop<jobject>(locals->at(i2)->get_obj()());
          cvf->update_local(T_OBJECT, i2, val);
        }
      }
    }

    GrowableArray<ScopeValue*>* scopeExpressions = cvf->scope()->expressions();
    StackValueCollection* expressions = cvf->expressions();
    if (expressions != NULL) {
      for (int i2 = 0; i2 < expressions->size(); i2++) {
        StackValue* var = expressions->at(i2);
        if (var->type() == T_OBJECT && scopeExpressions->at(i2)->is_object()) {
          jvalue val;
          val.l = cast_from_oop<jobject>(expressions->at(i2)->get_obj()());
          cvf->update_stack(T_OBJECT, i2, val);
        }
      }
    }

    GrowableArray<MonitorValue*>* scopeMonitors = cvf->scope()->monitors();
    GrowableArray<MonitorInfo*>* monitors = cvf->monitors();
    if (monitors != NULL) {
      for (int i2 = 0; i2 < monitors->length(); i2++) {
        cvf->update_monitor(i2, monitors->at(i2));
      }
    }
  }

  // all locals are materialized by now
  JVMCIENV->set_HotSpotStackFrameReference_localIsVirtual(hs_frame, NULL);
  // update the locals array
  JVMCIObjectArray array = JVMCIENV->get_HotSpotStackFrameReference_locals(hs_frame);
  StackValueCollection* locals = virtualFrames->at(last_frame_number)->locals();
  for (int i = 0; i < locals->size(); i++) {
    StackValue* var = locals->at(i);
    if (var->type() == T_OBJECT) {
      JVMCIENV->put_object_at(array, i, HotSpotJVMCI::wrap(locals->at(i)->get_obj()()));
    }
  }
  HotSpotJVMCI::HotSpotStackFrameReference::set_objectsMaterialized(JVMCIENV, hs_frame, JNI_TRUE);
C2V_END

// Use of tty does not require the current thread to be attached to the VM
// so no need for a full C2V_VMENTRY transition.
C2V_VMENTRY_PREFIX(void, writeDebugOutput, (JNIEnv* env, jobject, jlong buffer, jint length, bool flush))
  if (length <= 8) {
    tty->write((char*) &buffer, length);
  } else {
    tty->write((char*) buffer, length);
  }
  if (flush) {
    tty->flush();
  }
C2V_END

// Use of tty does not require the current thread to be attached to the VM
// so no need for a full C2V_VMENTRY transition.
C2V_VMENTRY_PREFIX(void, flushDebugOutput, (JNIEnv* env, jobject))
  tty->flush();
C2V_END

C2V_VMENTRY_0(jint, methodDataProfileDataSize, (JNIEnv* env, jobject, jlong metaspace_method_data, jint position))
  MethodData* mdo = JVMCIENV->asMethodData(metaspace_method_data);
  ProfileData* profile_data = mdo->data_at(position);
  if (mdo->is_valid(profile_data)) {
    return profile_data->size_in_bytes();
  }
  DataLayout* data    = mdo->extra_data_base();
  DataLayout* end   = mdo->extra_data_limit();
  for (;; data = mdo->next_extra(data)) {
    assert(data < end, "moved past end of extra data");
    profile_data = data->data_in();
    if (mdo->dp_to_di(profile_data->dp()) == position) {
      return profile_data->size_in_bytes();
    }
  }
  JVMCI_THROW_MSG_0(IllegalArgumentException, err_msg("Invalid profile data position %d", position));
C2V_END

C2V_VMENTRY_0(jlong, getFingerprint, (JNIEnv* env, jobject, jlong metaspace_klass))
  JVMCI_THROW_MSG_0(InternalError, "unimplemented");
C2V_END

C2V_VMENTRY_NULL(jobject, getInterfaces, (JNIEnv* env, jobject, jobject jvmci_type))
  if (jvmci_type == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }

  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (klass == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  if (!klass->is_instance_klass()) {
    JVMCI_THROW_MSG_0(InternalError, err_msg("Class %s must be instance klass", klass->external_name()));
  }
  InstanceKlass* iklass = InstanceKlass::cast(klass);

  // Regular instance klass, fill in all local interfaces
  int size = iklass->local_interfaces()->length();
  JVMCIObjectArray interfaces = JVMCIENV->new_HotSpotResolvedObjectTypeImpl_array(size, JVMCI_CHECK_NULL);
  for (int index = 0; index < size; index++) {
    JVMCIKlassHandle klass(THREAD);
    Klass* k = iklass->local_interfaces()->at(index);
    klass = k;
    JVMCIObject type = JVMCIENV->get_jvmci_type(klass, JVMCI_CHECK_NULL);
    JVMCIENV->put_object_at(interfaces, index, type);
  }
  return JVMCIENV->get_jobject(interfaces);
C2V_END

C2V_VMENTRY_NULL(jobject, getComponentType, (JNIEnv* env, jobject, jobject jvmci_type))
  if (jvmci_type == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }

  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  oop mirror = klass->java_mirror();
  if (java_lang_Class::is_primitive(mirror) ||
      !java_lang_Class::as_Klass(mirror)->is_array_klass()) {
    return NULL;
  }

  oop component_mirror = java_lang_Class::component_mirror(mirror);
  if (component_mirror == NULL) {
    return NULL;
  }
  Klass* component_klass = java_lang_Class::as_Klass(component_mirror);
  if (component_klass != NULL) {
    JVMCIKlassHandle klass_handle(THREAD);
    klass_handle = component_klass;
    JVMCIObject result = JVMCIENV->get_jvmci_type(klass_handle, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobject(result);
  }
  BasicType type = java_lang_Class::primitive_type(component_mirror);
  JVMCIObject result = JVMCIENV->get_jvmci_primitive_type(type);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY(void, ensureInitialized, (JNIEnv* env, jobject, jobject jvmci_type))
  if (jvmci_type == NULL) {
    JVMCI_THROW(NullPointerException);
  }

  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (klass != NULL && klass->should_be_initialized()) {
    InstanceKlass* k = InstanceKlass::cast(klass);
    k->initialize(CHECK);
  }
C2V_END

C2V_VMENTRY(void, ensureLinked, (JNIEnv* env, jobject, jobject jvmci_type))
  if (jvmci_type == NULL) {
    JVMCI_THROW(NullPointerException);
  }

  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (klass != NULL && klass->is_instance_klass()) {
    InstanceKlass* k = InstanceKlass::cast(klass);
    k->link_class(CHECK);
  }
C2V_END

C2V_VMENTRY_0(jint, interpreterFrameSize, (JNIEnv* env, jobject, jobject bytecode_frame_handle))
  if (bytecode_frame_handle == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }

  JVMCIObject top_bytecode_frame = JVMCIENV->wrap(bytecode_frame_handle);
  JVMCIObject bytecode_frame = top_bytecode_frame;
  int size = 0;
  int callee_parameters = 0;
  int callee_locals = 0;
  Method* method = JVMCIENV->asMethod(JVMCIENV->get_BytecodePosition_method(bytecode_frame));
  int extra_args = method->max_stack() - JVMCIENV->get_BytecodeFrame_numStack(bytecode_frame);

  while (bytecode_frame.is_non_null()) {
    int locks = JVMCIENV->get_BytecodeFrame_numLocks(bytecode_frame);
    int temps = JVMCIENV->get_BytecodeFrame_numStack(bytecode_frame);
    bool is_top_frame = (JVMCIENV->equals(bytecode_frame, top_bytecode_frame));
    Method* method = JVMCIENV->asMethod(JVMCIENV->get_BytecodePosition_method(bytecode_frame));

    int frame_size = BytesPerWord * Interpreter::size_activation(method->max_stack(),
                                                                 temps + callee_parameters,
                                                                 extra_args,
                                                                 locks,
                                                                 callee_parameters,
                                                                 callee_locals,
                                                                 is_top_frame);
    size += frame_size;

    callee_parameters = method->size_of_parameters();
    callee_locals = method->max_locals();
    extra_args = 0;
    bytecode_frame = JVMCIENV->get_BytecodePosition_caller(bytecode_frame);
  }
  return size + Deoptimization::last_frame_adjust(0, callee_locals) * BytesPerWord;
C2V_END

C2V_VMENTRY(void, compileToBytecode, (JNIEnv* env, jobject, jobject lambda_form_handle))
  Handle lambda_form = JVMCIENV->asConstant(JVMCIENV->wrap(lambda_form_handle), JVMCI_CHECK);
  if (lambda_form->is_a(vmClasses::LambdaForm_klass())) {
    TempNewSymbol compileToBytecode = SymbolTable::new_symbol("compileToBytecode");
    JavaValue result(T_VOID);
    JavaCalls::call_special(&result, lambda_form, vmClasses::LambdaForm_klass(), compileToBytecode, vmSymbols::void_method_signature(), CHECK);
  } else {
    JVMCI_THROW_MSG(IllegalArgumentException,
                    err_msg("Unexpected type: %s", lambda_form->klass()->external_name()))
  }
C2V_END

C2V_VMENTRY_0(jint, getIdentityHashCode, (JNIEnv* env, jobject, jobject object))
  Handle obj = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_0);
  return obj->identity_hash();
C2V_END

C2V_VMENTRY_0(jboolean, isInternedString, (JNIEnv* env, jobject, jobject object))
  Handle str = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_0);
  if (!java_lang_String::is_instance(str())) {
    return false;
  }
  int len;
  jchar* name = java_lang_String::as_unicode_string(str(), len, CHECK_false);
  return (StringTable::lookup(name, len) != NULL);
C2V_END


C2V_VMENTRY_NULL(jobject, unboxPrimitive, (JNIEnv* env, jobject, jobject object))
  if (object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle box = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_NULL);
  BasicType type = java_lang_boxing_object::basic_type(box());
  jvalue result;
  if (java_lang_boxing_object::get_value(box(), &result) == T_ILLEGAL) {
    return NULL;
  }
  JVMCIObject boxResult = JVMCIENV->create_box(type, &result, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(boxResult);
C2V_END

C2V_VMENTRY_NULL(jobject, boxPrimitive, (JNIEnv* env, jobject, jobject object))
  if (object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  JVMCIObject box = JVMCIENV->wrap(object);
  BasicType type = JVMCIENV->get_box_type(box);
  if (type == T_ILLEGAL) {
    return NULL;
  }
  jvalue value = JVMCIENV->get_boxed_value(type, box);
  JavaValue box_result(T_OBJECT);
  JavaCallArguments jargs;
  Klass* box_klass = NULL;
  Symbol* box_signature = NULL;
#define BOX_CASE(bt, v, argtype, name)           \
  case bt: \
    jargs.push_##argtype(value.v); \
    box_klass = vmClasses::name##_klass(); \
    box_signature = vmSymbols::name##_valueOf_signature(); \
    break

  switch (type) {
    BOX_CASE(T_BOOLEAN, z, int, Boolean);
    BOX_CASE(T_BYTE, b, int, Byte);
    BOX_CASE(T_CHAR, c, int, Character);
    BOX_CASE(T_SHORT, s, int, Short);
    BOX_CASE(T_INT, i, int, Integer);
    BOX_CASE(T_LONG, j, long, Long);
    BOX_CASE(T_FLOAT, f, float, Float);
    BOX_CASE(T_DOUBLE, d, double, Double);
    default:
      ShouldNotReachHere();
  }
#undef BOX_CASE

  JavaCalls::call_static(&box_result,
                         box_klass,
                         vmSymbols::valueOf_name(),
                         box_signature, &jargs, CHECK_NULL);
  oop hotspot_box = box_result.get_oop();
  JVMCIObject result = JVMCIENV->get_object_constant(hotspot_box, false);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_NULL(jobjectArray, getDeclaredConstructors, (JNIEnv* env, jobject, jobject holder))
  if (holder == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Klass* klass = JVMCIENV->asKlass(holder);
  if (!klass->is_instance_klass()) {
    JVMCIObjectArray methods = JVMCIENV->new_ResolvedJavaMethod_array(0, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobjectArray(methods);
  }

  InstanceKlass* iklass = InstanceKlass::cast(klass);
  // Ensure class is linked
  iklass->link_class(CHECK_NULL);

  GrowableArray<Method*> constructors_array;
  for (int i = 0; i < iklass->methods()->length(); i++) {
    Method* m = iklass->methods()->at(i);
    if (m->is_initializer() && !m->is_static()) {
      constructors_array.append(m);
    }
  }
  JVMCIObjectArray methods = JVMCIENV->new_ResolvedJavaMethod_array(constructors_array.length(), JVMCI_CHECK_NULL);
  for (int i = 0; i < constructors_array.length(); i++) {
    methodHandle ctor(THREAD, constructors_array.at(i));
    JVMCIObject method = JVMCIENV->get_jvmci_method(ctor, JVMCI_CHECK_NULL);
    JVMCIENV->put_object_at(methods, i, method);
  }
  return JVMCIENV->get_jobjectArray(methods);
C2V_END

C2V_VMENTRY_NULL(jobjectArray, getDeclaredMethods, (JNIEnv* env, jobject, jobject holder))
  if (holder == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Klass* klass = JVMCIENV->asKlass(holder);
  if (!klass->is_instance_klass()) {
    JVMCIObjectArray methods = JVMCIENV->new_ResolvedJavaMethod_array(0, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobjectArray(methods);
  }

  InstanceKlass* iklass = InstanceKlass::cast(klass);
  // Ensure class is linked
  iklass->link_class(CHECK_NULL);

  GrowableArray<Method*> methods_array;
  for (int i = 0; i < iklass->methods()->length(); i++) {
    Method* m = iklass->methods()->at(i);
    if (!m->is_initializer() && !m->is_overpass()) {
      methods_array.append(m);
    }
  }
  JVMCIObjectArray methods = JVMCIENV->new_ResolvedJavaMethod_array(methods_array.length(), JVMCI_CHECK_NULL);
  for (int i = 0; i < methods_array.length(); i++) {
    methodHandle mh(THREAD, methods_array.at(i));
    JVMCIObject method = JVMCIENV->get_jvmci_method(mh, JVMCI_CHECK_NULL);
    JVMCIENV->put_object_at(methods, i, method);
  }
  return JVMCIENV->get_jobjectArray(methods);
C2V_END

C2V_VMENTRY_NULL(jobject, readFieldValue, (JNIEnv* env, jobject, jobject object, jobject expected_type, long displacement, jboolean is_volatile, jobject kind_object))
  if (object == NULL || kind_object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }

  JVMCIObject kind = JVMCIENV->wrap(kind_object);
  BasicType basic_type = JVMCIENV->kindToBasicType(kind, JVMCI_CHECK_NULL);

  InstanceKlass* holder = NULL;
  if (expected_type != NULL) {
    holder = InstanceKlass::cast(JVMCIENV->asKlass(JVMCIENV->wrap(expected_type)));
  }

  bool is_static = false;
  Handle obj;
  JVMCIObject base = JVMCIENV->wrap(object);
  if (JVMCIENV->isa_HotSpotObjectConstantImpl(base)) {
    obj = JVMCIENV->asConstant(base, JVMCI_CHECK_NULL);
    // asConstant will throw an NPE if a constant contains NULL

    if (holder != NULL && !obj->is_a(holder)) {
      // Not a subtype of field holder
      return NULL;
    }
    is_static = false;
    if (holder == NULL && java_lang_Class::is_instance(obj()) && displacement >= InstanceMirrorKlass::offset_of_static_fields()) {
      is_static = true;
    }
  } else if (JVMCIENV->isa_HotSpotResolvedObjectTypeImpl(base)) {
    is_static = true;
    Klass* klass = JVMCIENV->asKlass(base);
    if (holder != NULL && holder != klass) {
      return NULL;
    }
    obj = Handle(THREAD, klass->java_mirror());
  } else {
    // The Java code is expected to guard against this path
    ShouldNotReachHere();
  }

  if (displacement < 0 || ((long) displacement + type2aelembytes(basic_type) > HeapWordSize * obj->size())) {
    // Reading outside of the object bounds
    JVMCI_THROW_MSG_NULL(IllegalArgumentException, "reading outside object bounds");
  }

  // Perform basic sanity checks on the read.  Primitive reads are permitted to read outside the
  // bounds of their fields but object reads must map exactly onto the underlying oop slot.
  if (basic_type == T_OBJECT) {
    if (obj->is_objArray()) {
      if (displacement < arrayOopDesc::base_offset_in_bytes(T_OBJECT)) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, "reading from array header");
      }
      if (displacement + heapOopSize > arrayOopDesc::base_offset_in_bytes(T_OBJECT) + arrayOop(obj())->length() * heapOopSize) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, "reading after last array element");
      }
      if (((displacement - arrayOopDesc::base_offset_in_bytes(T_OBJECT)) % heapOopSize) != 0) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, "misaligned object read from array");
      }
    } else if (obj->is_instance()) {
      InstanceKlass* klass = InstanceKlass::cast(is_static ? java_lang_Class::as_Klass(obj()) : obj->klass());
      fieldDescriptor fd;
      if (!klass->find_field_from_offset(displacement, is_static, &fd)) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, err_msg("Can't find field at displacement %d in object of type %s", (int) displacement, klass->external_name()));
      }
      if (fd.field_type() != T_OBJECT && fd.field_type() != T_ARRAY) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, err_msg("Field at displacement %d in object of type %s is %s but expected %s", (int) displacement,
                                                               klass->external_name(), type2name(fd.field_type()), type2name(basic_type)));
      }
    } else if (obj->is_typeArray()) {
      JVMCI_THROW_MSG_NULL(IllegalArgumentException, "Can't read objects from primitive array");
    } else {
      ShouldNotReachHere();
    }
  } else {
    if (obj->is_objArray()) {
      JVMCI_THROW_MSG_NULL(IllegalArgumentException, "Reading primitive from object array");
    } else if (obj->is_typeArray()) {
      if (displacement < arrayOopDesc::base_offset_in_bytes(ArrayKlass::cast(obj->klass())->element_type())) {
        JVMCI_THROW_MSG_NULL(IllegalArgumentException, "reading from array header");
      }
    }
  }

  jlong value = 0;
  switch (basic_type) {
    case T_BOOLEAN: value = is_volatile ? obj->bool_field_acquire(displacement)   : obj->bool_field(displacement);  break;
    case T_BYTE:    value = is_volatile ? obj->byte_field_acquire(displacement)   : obj->byte_field(displacement);  break;
    case T_SHORT:   value = is_volatile ? obj->short_field_acquire(displacement)  : obj->short_field(displacement); break;
    case T_CHAR:    value = is_volatile ? obj->char_field_acquire(displacement)   : obj->char_field(displacement);  break;
    case T_FLOAT:
    case T_INT:     value = is_volatile ? obj->int_field_acquire(displacement)    : obj->int_field(displacement);   break;
    case T_DOUBLE:
    case T_LONG:    value = is_volatile ? obj->long_field_acquire(displacement)   : obj->long_field(displacement);  break;

    case T_OBJECT: {
      if (displacement == java_lang_Class::component_mirror_offset() && java_lang_Class::is_instance(obj()) &&
          (java_lang_Class::as_Klass(obj()) == NULL || !java_lang_Class::as_Klass(obj())->is_array_klass())) {
        // Class.componentType for non-array classes can transiently contain an int[] that's
        // used for locking so always return null to mimic Class.getComponentType()
        return JVMCIENV->get_jobject(JVMCIENV->get_JavaConstant_NULL_POINTER());
      }

      oop value = is_volatile ? obj->obj_field_acquire(displacement) : obj->obj_field(displacement);
      if (value == NULL) {
        return JVMCIENV->get_jobject(JVMCIENV->get_JavaConstant_NULL_POINTER());
      } else {
        if (value != NULL && !oopDesc::is_oop(value)) {
          // Throw an exception to improve debuggability.  This check isn't totally reliable because
          // is_oop doesn't try to be completety safe but for most invalid values it provides a good
          // enough answer.  It possible to crash in the is_oop call but that just means the crash happens
          // closer to where things went wrong.
          JVMCI_THROW_MSG_NULL(InternalError, err_msg("Read bad oop " INTPTR_FORMAT " at offset " JLONG_FORMAT " in object " INTPTR_FORMAT " of type %s",
                                                      p2i(value), displacement, p2i(obj()), obj->klass()->external_name()));
        }

        JVMCIObject result = JVMCIENV->get_object_constant(value);
        return JVMCIENV->get_jobject(result);
      }
    }

    default:
      ShouldNotReachHere();
  }
  JVMCIObject result = JVMCIENV->call_JavaConstant_forPrimitive(kind, value, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END

C2V_VMENTRY_0(jboolean, isInstance, (JNIEnv* env, jobject, jobject holder, jobject object))
  if (object == NULL || holder == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle obj = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_0);
  Klass* klass = JVMCIENV->asKlass(JVMCIENV->wrap(holder));
  return obj->is_a(klass);
C2V_END

C2V_VMENTRY_0(jboolean, isAssignableFrom, (JNIEnv* env, jobject, jobject holder, jobject otherHolder))
  if (holder == NULL || otherHolder == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Klass* klass = JVMCIENV->asKlass(JVMCIENV->wrap(holder));
  Klass* otherKlass = JVMCIENV->asKlass(JVMCIENV->wrap(otherHolder));
  return otherKlass->is_subtype_of(klass);
C2V_END

C2V_VMENTRY_0(jboolean, isTrustedForIntrinsics, (JNIEnv* env, jobject, jobject holder))
  if (holder == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  InstanceKlass* ik = InstanceKlass::cast(JVMCIENV->asKlass(JVMCIENV->wrap(holder)));
  if (ik->class_loader_data()->is_boot_class_loader_data() || ik->class_loader_data()->is_platform_class_loader_data()) {
    return true;
  }
  return false;
C2V_END

C2V_VMENTRY_NULL(jobject, asJavaType, (JNIEnv* env, jobject, jobject object))
  if (object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle obj = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_NULL);
  if (java_lang_Class::is_instance(obj())) {
    if (java_lang_Class::is_primitive(obj())) {
      JVMCIObject type = JVMCIENV->get_jvmci_primitive_type(java_lang_Class::primitive_type(obj()));
      return JVMCIENV->get_jobject(type);
    }
    Klass* klass = java_lang_Class::as_Klass(obj());
    JVMCIKlassHandle klass_handle(THREAD);
    klass_handle = klass;
    JVMCIObject type = JVMCIENV->get_jvmci_type(klass_handle, JVMCI_CHECK_NULL);
    return JVMCIENV->get_jobject(type);
  }
  return NULL;
C2V_END


C2V_VMENTRY_NULL(jobject, asString, (JNIEnv* env, jobject, jobject object))
  if (object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle obj = JVMCIENV->asConstant(JVMCIENV->wrap(object), JVMCI_CHECK_NULL);
  const char* str = java_lang_String::as_utf8_string(obj());
  JVMCIObject result = JVMCIENV->create_string(str, JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(result);
C2V_END


C2V_VMENTRY_0(jboolean, equals, (JNIEnv* env, jobject, jobject x, jlong xHandle, jobject y, jlong yHandle))
  if (x == NULL || y == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  return JVMCIENV->resolve_handle(xHandle) == JVMCIENV->resolve_handle(yHandle);
C2V_END

C2V_VMENTRY_NULL(jobject, getJavaMirror, (JNIEnv* env, jobject, jobject object))
  if (object == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  JVMCIObject base_object = JVMCIENV->wrap(object);
  Handle mirror;
  if (JVMCIENV->isa_HotSpotResolvedObjectTypeImpl(base_object)) {
    mirror = Handle(THREAD, JVMCIENV->asKlass(base_object)->java_mirror());
  } else if (JVMCIENV->isa_HotSpotResolvedPrimitiveType(base_object)) {
    mirror = JVMCIENV->asConstant(JVMCIENV->get_HotSpotResolvedPrimitiveType_mirror(base_object), JVMCI_CHECK_NULL);
  } else {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException,
                         err_msg("Unexpected type: %s", JVMCIENV->klass_name(base_object)));
 }
  JVMCIObject result = JVMCIENV->get_object_constant(mirror());
  return JVMCIENV->get_jobject(result);
C2V_END


C2V_VMENTRY_0(jint, getArrayLength, (JNIEnv* env, jobject, jobject x))
  if (x == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle xobj = JVMCIENV->asConstant(JVMCIENV->wrap(x), JVMCI_CHECK_0);
  if (xobj->klass()->is_array_klass()) {
    return arrayOop(xobj())->length();
  }
  return -1;
 C2V_END


C2V_VMENTRY_NULL(jobject, readArrayElement, (JNIEnv* env, jobject, jobject x, int index))
  if (x == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Handle xobj = JVMCIENV->asConstant(JVMCIENV->wrap(x), JVMCI_CHECK_NULL);
  if (xobj->klass()->is_array_klass()) {
    arrayOop array = arrayOop(xobj());
    BasicType element_type = ArrayKlass::cast(array->klass())->element_type();
    if (index < 0 || index >= array->length()) {
      return NULL;
    }
    JVMCIObject result;

    if (element_type == T_OBJECT) {
      result = JVMCIENV->get_object_constant(objArrayOop(xobj())->obj_at(index));
      if (result.is_null()) {
        result = JVMCIENV->get_JavaConstant_NULL_POINTER();
      }
    } else {
      jvalue value;
      switch (element_type) {
        case T_DOUBLE:        value.d = typeArrayOop(xobj())->double_at(index);        break;
        case T_FLOAT:         value.f = typeArrayOop(xobj())->float_at(index);         break;
        case T_LONG:          value.j = typeArrayOop(xobj())->long_at(index);          break;
        case T_INT:           value.i = typeArrayOop(xobj())->int_at(index);            break;
        case T_SHORT:         value.s = typeArrayOop(xobj())->short_at(index);          break;
        case T_CHAR:          value.c = typeArrayOop(xobj())->char_at(index);           break;
        case T_BYTE:          value.b = typeArrayOop(xobj())->byte_at(index);           break;
        case T_BOOLEAN:       value.z = typeArrayOop(xobj())->byte_at(index) & 1;       break;
        default:              ShouldNotReachHere();
      }
      result = JVMCIENV->create_box(element_type, &value, JVMCI_CHECK_NULL);
    }
    assert(!result.is_null(), "must have a value");
    return JVMCIENV->get_jobject(result);
  }
  return NULL;;
C2V_END


C2V_VMENTRY_0(jint, arrayBaseOffset, (JNIEnv* env, jobject, jobject kind))
  if (kind == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  BasicType type = JVMCIENV->kindToBasicType(JVMCIENV->wrap(kind), JVMCI_CHECK_0);
  return arrayOopDesc::header_size(type) * HeapWordSize;
C2V_END

C2V_VMENTRY_0(jint, arrayIndexScale, (JNIEnv* env, jobject, jobject kind))
  if (kind == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  BasicType type = JVMCIENV->kindToBasicType(JVMCIENV->wrap(kind), JVMCI_CHECK_0);
  return type2aelembytes(type);
C2V_END

C2V_VMENTRY(void, deleteGlobalHandle, (JNIEnv* env, jobject, jlong h))
  jobject handle = (jobject)(address)h;
  if (handle != NULL) {
    JVMCIENV->runtime()->destroy_global(handle);
  }
}

static void requireJVMCINativeLibrary(JVMCI_TRAPS) {
  if (!UseJVMCINativeLibrary) {
    JVMCI_THROW_MSG(UnsupportedOperationException, "JVMCI shared library is not enabled (requires -XX:+UseJVMCINativeLibrary)");
  }
}

C2V_VMENTRY_NULL(jlongArray, registerNativeMethods, (JNIEnv* env, jobject, jclass mirror))
  requireJVMCINativeLibrary(JVMCI_CHECK_NULL);
  requireInHotSpot("registerNativeMethods", JVMCI_CHECK_NULL);
  char* sl_path;
  void* sl_handle;
  JVMCIRuntime* runtime = JVMCI::compiler_runtime();
  {
    // Ensure the JVMCI shared library runtime is initialized.
    JVMCIEnv __peer_jvmci_env__(thread, false, __FILE__, __LINE__);
    JVMCIEnv* peerEnv = &__peer_jvmci_env__;
    HandleMark hm(THREAD);
    JVMCIObject receiver = runtime->get_HotSpotJVMCIRuntime(peerEnv);
    if (peerEnv->has_pending_exception()) {
      peerEnv->describe_pending_exception(true);
    }
    sl_handle = JVMCI::get_shared_library(sl_path, false);
    if (sl_handle == NULL) {
      JVMCI_THROW_MSG_0(InternalError, err_msg("Error initializing JVMCI runtime %d", runtime->id()));
    }
  }

  if (mirror == NULL) {
    JVMCI_THROW_0(NullPointerException);
  }
  Klass* klass = java_lang_Class::as_Klass(JNIHandles::resolve(mirror));
  if (klass == NULL || !klass->is_instance_klass()) {
    JVMCI_THROW_MSG_0(IllegalArgumentException, "clazz is for primitive type");
  }

  InstanceKlass* iklass = InstanceKlass::cast(klass);
  for (int i = 0; i < iklass->methods()->length(); i++) {
    methodHandle method(THREAD, iklass->methods()->at(i));
    if (method->is_native()) {

      // Compute argument size
      int args_size = 1                             // JNIEnv
                    + (method->is_static() ? 1 : 0) // class for static methods
                    + method->size_of_parameters(); // actual parameters

      // 1) Try JNI short style
      stringStream st;
      char* pure_name = NativeLookup::pure_jni_name(method);
      guarantee(pure_name != NULL, "Illegal native method name encountered");
      os::print_jni_name_prefix_on(&st, args_size);
      st.print_raw(pure_name);
      os::print_jni_name_suffix_on(&st, args_size);
      char* jni_name = st.as_string();

      address entry = (address) os::dll_lookup(sl_handle, jni_name);
      if (entry == NULL) {
        // 2) Try JNI long style
        st.reset();
        char* long_name = NativeLookup::long_jni_name(method);
        guarantee(long_name != NULL, "Illegal native method name encountered");
        os::print_jni_name_prefix_on(&st, args_size);
        st.print_raw(pure_name);
        st.print_raw(long_name);
        os::print_jni_name_suffix_on(&st, args_size);
        char* jni_long_name = st.as_string();
        entry = (address) os::dll_lookup(sl_handle, jni_long_name);
        if (entry == NULL) {
          JVMCI_THROW_MSG_0(UnsatisfiedLinkError, err_msg("%s [neither %s nor %s exist in %s]",
              method->name_and_sig_as_C_string(),
              jni_name, jni_long_name, sl_path));
        }
      }

      if (method->has_native_function() && entry != method->native_function()) {
        JVMCI_THROW_MSG_0(UnsatisfiedLinkError, err_msg("%s [cannot re-link from " PTR_FORMAT " to " PTR_FORMAT "]",
            method->name_and_sig_as_C_string(), p2i(method->native_function()), p2i(entry)));
      }
      method->set_native_function(entry, Method::native_bind_event_is_interesting);
      log_debug(jni, resolve)("[Dynamic-linking native method %s.%s ... JNI] @ " PTR_FORMAT,
                              method->method_holder()->external_name(),
                              method->name()->as_C_string(),
                              p2i((void*) entry));
    }
  }

  typeArrayOop info_oop = oopFactory::new_longArray(4, CHECK_0);
  jlongArray info = (jlongArray) JNIHandles::make_local(THREAD, info_oop);
  runtime->init_JavaVM_info(info, JVMCI_CHECK_0);
  return info;
}

C2V_VMENTRY_PREFIX(jboolean, isCurrentThreadAttached, (JNIEnv* env, jobject c2vm))
  if (thread == NULL) {
    // Called from unattached JVMCI shared library thread
    return false;
  }
  JVMCITraceMark jtm("isCurrentThreadAttached");
  if (thread->jni_environment() == env) {
    C2V_BLOCK(jboolean, isCurrentThreadAttached, (JNIEnv* env, jobject))
    requireJVMCINativeLibrary(JVMCI_CHECK_0);
    JVMCIRuntime* runtime = JVMCI::compiler_runtime();
    if (runtime == NULL || !runtime->has_shared_library_javavm()) {
      JVMCI_THROW_MSG_0(IllegalStateException, "Require JVMCI shared library JavaVM to be initialized in isCurrentThreadAttached");
    }
    JNIEnv* peerEnv;
    return runtime->GetEnv(thread, (void**) &peerEnv, JNI_VERSION_1_2) == JNI_OK;
  }
  return true;
C2V_END

C2V_VMENTRY_PREFIX(jlong, getCurrentJavaThread, (JNIEnv* env, jobject c2vm))
  if (thread == NULL) {
    // Called from unattached JVMCI shared library thread
    return 0L;
  }
  JVMCITraceMark jtm("getCurrentJavaThread");
  return (jlong) p2i(thread);
C2V_END

C2V_VMENTRY_PREFIX(jboolean, attachCurrentThread, (JNIEnv* env, jobject c2vm, jbyteArray name, jboolean as_daemon))
  if (thread == NULL) {
    // Called from unattached JVMCI shared library thread
    guarantee(name != NULL, "libjvmci caller must pass non-null name");

    extern struct JavaVM_ main_vm;
    JNIEnv* hotspotEnv;

    int name_len = env->GetArrayLength(name);
    char name_buf[64]; // Cannot use Resource heap as it requires a current thread
    int to_copy = MIN2(name_len, (int) sizeof(name_buf) - 1);
    env->GetByteArrayRegion(name, 0, to_copy, (jbyte*) name_buf);
    name_buf[to_copy] = '\0';
    JavaVMAttachArgs attach_args;
    attach_args.version = JNI_VERSION_1_2;
    attach_args.name = name_buf;
    attach_args.group = NULL;
    jint res = as_daemon ? main_vm.AttachCurrentThreadAsDaemon((void**) &hotspotEnv, &attach_args) :
                           main_vm.AttachCurrentThread((void**) &hotspotEnv, &attach_args);
    if (res != JNI_OK) {
      JNI_THROW_("attachCurrentThread", InternalError, err_msg("Trying to attach thread returned %d", res), false);
    }
    return true;
  }
  JVMCITraceMark jtm("attachCurrentThread");
  if (thread->jni_environment() == env) {
    // Called from HotSpot
    C2V_BLOCK(jboolean, attachCurrentThread, (JNIEnv* env, jobject, jboolean))
    requireJVMCINativeLibrary(JVMCI_CHECK_0);
    JVMCIRuntime* runtime = JVMCI::compiler_runtime();
    if (runtime == NULL || !runtime->has_shared_library_javavm()) {
        JVMCI_THROW_MSG_0(IllegalStateException, "Require JVMCI shared library JavaVM to be initialized in attachCurrentThread");
    }

    JavaVMAttachArgs attach_args;
    attach_args.version = JNI_VERSION_1_2;
    attach_args.name = const_cast<char*>(thread->name());
    attach_args.group = NULL;
    JNIEnv* peerJNIEnv;
    if (runtime->GetEnv(thread, (void**) &peerJNIEnv, JNI_VERSION_1_2) == JNI_OK) {
      return false;
    }
    jint res = as_daemon ? runtime->AttachCurrentThreadAsDaemon(thread, (void**) &peerJNIEnv, &attach_args) :
                           runtime->AttachCurrentThread(thread, (void**) &peerJNIEnv, &attach_args);

    if (res == JNI_OK) {
      guarantee(peerJNIEnv != NULL, "must be");
      JVMCI_event_1("attached to JavaVM for JVMCI runtime %d", runtime->id());
      return true;
    }
    JVMCI_THROW_MSG_0(InternalError, err_msg("Error %d while attaching %s", res, attach_args.name));
  }
  // Called from JVMCI shared library
  return false;
C2V_END

C2V_VMENTRY_PREFIX(void, detachCurrentThread, (JNIEnv* env, jobject c2vm))
  if (thread == NULL) {
    // Called from unattached JVMCI shared library thread
    JNI_THROW("detachCurrentThread", IllegalStateException, "Cannot detach non-attached thread");
  }
  JVMCITraceMark jtm("detachCurrentThread");
  if (thread->jni_environment() == env) {
    // Called from HotSpot
    C2V_BLOCK(void, detachCurrentThread, (JNIEnv* env, jobject))
    requireJVMCINativeLibrary(JVMCI_CHECK);
    requireInHotSpot("detachCurrentThread", JVMCI_CHECK);
    JVMCIRuntime* runtime = JVMCI::compiler_runtime();
    if (runtime == NULL || !runtime->has_shared_library_javavm()) {
      JVMCI_THROW_MSG(IllegalStateException, "Require JVMCI shared library JavaVM to be initialized in detachCurrentThread");
    }
    JNIEnv* peerJNIEnv;
    if (runtime->GetEnv(thread, (void**) &peerJNIEnv, JNI_VERSION_1_2) != JNI_OK) {
      JVMCI_THROW_MSG(IllegalStateException, err_msg("Cannot detach non-attached thread: %s", thread->name()));
    }
    jint res = runtime->DetachCurrentThread(thread);
    if (res != JNI_OK) {
      JVMCI_THROW_MSG(InternalError, err_msg("Error %d while attaching %s", res, thread->name()));
    }
  } else {
    // Called from attached JVMCI shared library thread
    extern struct JavaVM_ main_vm;
    jint res = main_vm.DetachCurrentThread();
    if (res != JNI_OK) {
      JNI_THROW("detachCurrentThread", InternalError, "Cannot detach non-attached thread");
    }
  }
C2V_END

C2V_VMENTRY_0(jlong, translate, (JNIEnv* env, jobject, jobject obj_handle))
  requireJVMCINativeLibrary(JVMCI_CHECK_0);
  if (obj_handle == NULL) {
    return 0L;
  }
  JVMCIEnv __peer_jvmci_env__(thread, !JVMCIENV->is_hotspot(), __FILE__, __LINE__);
  JVMCIEnv* peerEnv = &__peer_jvmci_env__;
  JVMCIEnv* thisEnv = JVMCIENV;

  JVMCIObject obj = thisEnv->wrap(obj_handle);
  JVMCIObject result;
  if (thisEnv->isa_HotSpotResolvedJavaMethodImpl(obj)) {
    methodHandle method(THREAD, thisEnv->asMethod(obj));
    result = peerEnv->get_jvmci_method(method, JVMCI_CHECK_0);
  } else if (thisEnv->isa_HotSpotResolvedObjectTypeImpl(obj)) {
    Klass* klass = thisEnv->asKlass(obj);
    JVMCIKlassHandle klass_handle(THREAD);
    klass_handle = klass;
    result = peerEnv->get_jvmci_type(klass_handle, JVMCI_CHECK_0);
  } else if (thisEnv->isa_HotSpotResolvedPrimitiveType(obj)) {
    BasicType type = JVMCIENV->kindToBasicType(JVMCIENV->get_HotSpotResolvedPrimitiveType_kind(obj), JVMCI_CHECK_0);
    result = peerEnv->get_jvmci_primitive_type(type);
  } else if (thisEnv->isa_IndirectHotSpotObjectConstantImpl(obj) ||
             thisEnv->isa_DirectHotSpotObjectConstantImpl(obj)) {
    Handle constant = thisEnv->asConstant(obj, JVMCI_CHECK_0);
    result = peerEnv->get_object_constant(constant());
  } else if (thisEnv->isa_HotSpotNmethod(obj)) {
    nmethodLocker locker;
    nmethod* nm = JVMCIENV->get_nmethod(obj, locker);
    if (nm != NULL) {
      JVMCINMethodData* data = nm->jvmci_nmethod_data();
      if (data != NULL) {
        if (peerEnv->is_hotspot()) {
          // Only the mirror in the HotSpot heap is accessible
          // through JVMCINMethodData
          oop nmethod_mirror = data->get_nmethod_mirror(nm, /* phantom_ref */ true);
          if (nmethod_mirror != NULL) {
            result = HotSpotJVMCI::wrap(nmethod_mirror);
          }
        }
      }
    }
    if (result.is_null()) {
      JVMCIObject methodObject = thisEnv->get_HotSpotNmethod_method(obj);
      methodHandle mh(THREAD, thisEnv->asMethod(methodObject));
      jboolean isDefault = thisEnv->get_HotSpotNmethod_isDefault(obj);
      jlong compileIdSnapshot = thisEnv->get_HotSpotNmethod_compileIdSnapshot(obj);
      JVMCIObject name_string = thisEnv->get_InstalledCode_name(obj);
      const char* cstring = name_string.is_null() ? NULL : thisEnv->as_utf8_string(name_string);
      // Create a new HotSpotNmethod instance in the peer runtime
      result = peerEnv->new_HotSpotNmethod(mh, cstring, isDefault, compileIdSnapshot, JVMCI_CHECK_0);
      if (nm == NULL) {
        // nmethod must have been unloaded
      } else {
        // Link the new HotSpotNmethod to the nmethod
        peerEnv->initialize_installed_code(result, nm, JVMCI_CHECK_0);
        // Only HotSpotNmethod instances in the HotSpot heap are tracked directly by the runtime.
        if (peerEnv->is_hotspot()) {
          JVMCINMethodData* data = nm->jvmci_nmethod_data();
          if (data == NULL) {
            JVMCI_THROW_MSG_0(IllegalArgumentException, "Cannot set HotSpotNmethod mirror for default nmethod");
          }
          if (data->get_nmethod_mirror(nm, /* phantom_ref */ false) != NULL) {
            JVMCI_THROW_MSG_0(IllegalArgumentException, "Cannot overwrite existing HotSpotNmethod mirror for nmethod");
          }
          oop nmethod_mirror = HotSpotJVMCI::resolve(result);
          data->set_nmethod_mirror(nm, nmethod_mirror);
        }
      }
    }
  } else {
    JVMCI_THROW_MSG_0(IllegalArgumentException,
                err_msg("Cannot translate object of type: %s", thisEnv->klass_name(obj)));
  }
  return (jlong) peerEnv->make_global(result).as_jobject();
}

C2V_VMENTRY_NULL(jobject, unhand, (JNIEnv* env, jobject, jlong obj_handle))
  requireJVMCINativeLibrary(JVMCI_CHECK_NULL);
  if (obj_handle == 0L) {
    return NULL;
  }
  jobject global_handle = (jobject) obj_handle;
  JVMCIObject global_handle_obj = JVMCIENV->wrap((jobject) obj_handle);
  jobject result = JVMCIENV->make_local(global_handle_obj).as_jobject();

  JVMCIENV->destroy_global(global_handle_obj);
  return result;
}

C2V_VMENTRY(void, updateHotSpotNmethod, (JNIEnv* env, jobject, jobject code_handle))
  JVMCIObject code = JVMCIENV->wrap(code_handle);
  // Execute this operation for the side effect of updating the InstalledCode state
  nmethodLocker locker;
  JVMCIENV->get_nmethod(code, locker);
}

C2V_VMENTRY_NULL(jbyteArray, getCode, (JNIEnv* env, jobject, jobject code_handle))
  JVMCIObject code = JVMCIENV->wrap(code_handle);
  nmethodLocker locker;
  CodeBlob* cb = JVMCIENV->get_code_blob(code, locker);
  if (cb == NULL) {
    return NULL;
  }
  int code_size = cb->code_size();
  JVMCIPrimitiveArray result = JVMCIENV->new_byteArray(code_size, JVMCI_CHECK_NULL);
  JVMCIENV->copy_bytes_from((jbyte*) cb->code_begin(), result, 0, code_size);
  return JVMCIENV->get_jbyteArray(result);
}

C2V_VMENTRY_NULL(jobject, asReflectionExecutable, (JNIEnv* env, jobject, jobject jvmci_method))
  requireInHotSpot("asReflectionExecutable", JVMCI_CHECK_NULL);
  methodHandle m(THREAD, JVMCIENV->asMethod(jvmci_method));
  oop executable;
  if (m->is_initializer()) {
    if (m->is_static_initializer()) {
      JVMCI_THROW_MSG_NULL(IllegalArgumentException,
          "Cannot create java.lang.reflect.Method for class initializer");
    }
    executable = Reflection::new_constructor(m, CHECK_NULL);
  } else {
    executable = Reflection::new_method(m, false, CHECK_NULL);
  }
  return JNIHandles::make_local(THREAD, executable);
}

C2V_VMENTRY_NULL(jobject, asReflectionField, (JNIEnv* env, jobject, jobject jvmci_type, jint index))
  requireInHotSpot("asReflectionField", JVMCI_CHECK_NULL);
  Klass* klass = JVMCIENV->asKlass(jvmci_type);
  if (!klass->is_instance_klass()) {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException,
        err_msg("Expected non-primitive type, got %s", klass->external_name()));
  }
  InstanceKlass* iklass = InstanceKlass::cast(klass);
  Array<u2>* fields = iklass->fields();
  if (index < 0 ||index > fields->length()) {
    JVMCI_THROW_MSG_NULL(IllegalArgumentException,
        err_msg("Field index %d out of bounds for %s", index, klass->external_name()));
  }
  fieldDescriptor fd(iklass, index);
  oop reflected = Reflection::new_field(&fd, CHECK_NULL);
  return JNIHandles::make_local(THREAD, reflected);
}

C2V_VMENTRY_NULL(jobjectArray, getFailedSpeculations, (JNIEnv* env, jobject, jlong failed_speculations_address, jobjectArray current))
  FailedSpeculation* head = *((FailedSpeculation**)(address) failed_speculations_address);
  int result_length = 0;
  for (FailedSpeculation* fs = head; fs != NULL; fs = fs->next()) {
    result_length++;
  }
  int current_length = 0;
  JVMCIObjectArray current_array = NULL;
  if (current != NULL) {
    current_array = JVMCIENV->wrap(current);
    current_length = JVMCIENV->get_length(current_array);
    if (current_length == result_length) {
      // No new failures
      return current;
    }
  }
  JVMCIObjectArray result = JVMCIENV->new_byte_array_array(result_length, JVMCI_CHECK_NULL);
  int result_index = 0;
  for (FailedSpeculation* fs = head; result_index < result_length; fs = fs->next()) {
    assert(fs != NULL, "npe");
    JVMCIPrimitiveArray entry;
    if (result_index < current_length) {
      entry = (JVMCIPrimitiveArray) JVMCIENV->get_object_at(current_array, result_index);
    } else {
      entry = JVMCIENV->new_byteArray(fs->data_len(), JVMCI_CHECK_NULL);
      JVMCIENV->copy_bytes_from((jbyte*) fs->data(), entry, 0, fs->data_len());
    }
    JVMCIENV->put_object_at(result, result_index++, entry);
  }
  return JVMCIENV->get_jobjectArray(result);
}

C2V_VMENTRY_0(jlong, getFailedSpeculationsAddress, (JNIEnv* env, jobject, jobject jvmci_method))
  methodHandle method(THREAD, JVMCIENV->asMethod(jvmci_method));
  MethodData* method_data = method->method_data();
  if (method_data == NULL) {
    ClassLoaderData* loader_data = method->method_holder()->class_loader_data();
    method_data = MethodData::allocate(loader_data, method, CHECK_0);
    method->set_method_data(method_data);
  }
  return (jlong) method_data->get_failed_speculations_address();
}

C2V_VMENTRY(void, releaseFailedSpeculations, (JNIEnv* env, jobject, jlong failed_speculations_address))
  FailedSpeculation::free_failed_speculations((FailedSpeculation**)(address) failed_speculations_address);
}

C2V_VMENTRY_0(jboolean, addFailedSpeculation, (JNIEnv* env, jobject, jlong failed_speculations_address, jbyteArray speculation_obj))
  JVMCIPrimitiveArray speculation_handle = JVMCIENV->wrap(speculation_obj);
  int speculation_len = JVMCIENV->get_length(speculation_handle);
  char* speculation = NEW_RESOURCE_ARRAY(char, speculation_len);
  JVMCIENV->copy_bytes_to(speculation_handle, (jbyte*) speculation, 0, speculation_len);
  return FailedSpeculation::add_failed_speculation(NULL, (FailedSpeculation**)(address) failed_speculations_address, (address) speculation, speculation_len);
}

C2V_VMENTRY(void, callSystemExit, (JNIEnv* env, jobject, jint status))
  JavaValue result(T_VOID);
  JavaCallArguments jargs(1);
  jargs.push_int(status);
  JavaCalls::call_static(&result,
                       vmClasses::System_klass(),
                       vmSymbols::exit_method_name(),
                       vmSymbols::int_void_signature(),
                       &jargs,
                       CHECK);
}

C2V_VMENTRY_0(jlong, ticksNow, (JNIEnv* env, jobject))
  return CompilerEvent::ticksNow();
}

C2V_VMENTRY_0(jint, registerCompilerPhase, (JNIEnv* env, jobject, jstring jphase_name))
#if INCLUDE_JFR
  JVMCIObject phase_name = JVMCIENV->wrap(jphase_name);
  const char *name = JVMCIENV->as_utf8_string(phase_name);
  return CompilerEvent::PhaseEvent::get_phase_id(name, true, true, true);
#else
  return -1;
#endif // !INCLUDE_JFR
}

C2V_VMENTRY(void, notifyCompilerPhaseEvent, (JNIEnv* env, jobject, jlong startTime, jint phase, jint compileId, jint level))
  EventCompilerPhase event;
  if (event.should_commit()) {
    CompilerEvent::PhaseEvent::post(event, startTime, phase, compileId, level);
  }
}

C2V_VMENTRY(void, notifyCompilerInliningEvent, (JNIEnv* env, jobject, jint compileId, jobject caller, jobject callee, jboolean succeeded, jstring jmessage, jint bci))
  EventCompilerInlining event;
  if (event.should_commit()) {
    Method* caller_method = JVMCIENV->asMethod(caller);
    Method* callee_method = JVMCIENV->asMethod(callee);
    JVMCIObject message = JVMCIENV->wrap(jmessage);
    CompilerEvent::InlineEvent::post(event, compileId, caller_method, callee_method, succeeded, JVMCIENV->as_utf8_string(message), bci);
  }
}

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &(c2v_ ## f))

#define STRING                  "Ljava/lang/String;"
#define OBJECT                  "Ljava/lang/Object;"
#define CLASS                   "Ljava/lang/Class;"
#define OBJECTCONSTANT          "Ljdk/vm/ci/hotspot/HotSpotObjectConstantImpl;"
#define HANDLECONSTANT          "Ljdk/vm/ci/hotspot/IndirectHotSpotObjectConstantImpl;"
#define EXECUTABLE              "Ljava/lang/reflect/Executable;"
#define STACK_TRACE_ELEMENT     "Ljava/lang/StackTraceElement;"
#define INSTALLED_CODE          "Ljdk/vm/ci/code/InstalledCode;"
#define TARGET_DESCRIPTION      "Ljdk/vm/ci/code/TargetDescription;"
#define BYTECODE_FRAME          "Ljdk/vm/ci/code/BytecodeFrame;"
#define JAVACONSTANT            "Ljdk/vm/ci/meta/JavaConstant;"
#define INSPECTED_FRAME_VISITOR "Ljdk/vm/ci/code/stack/InspectedFrameVisitor;"
#define RESOLVED_METHOD         "Ljdk/vm/ci/meta/ResolvedJavaMethod;"
#define HS_RESOLVED_METHOD      "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaMethodImpl;"
#define HS_RESOLVED_KLASS       "Ljdk/vm/ci/hotspot/HotSpotResolvedObjectTypeImpl;"
#define HS_RESOLVED_TYPE        "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaType;"
#define HS_RESOLVED_FIELD       "Ljdk/vm/ci/hotspot/HotSpotResolvedJavaField;"
#define HS_INSTALLED_CODE       "Ljdk/vm/ci/hotspot/HotSpotInstalledCode;"
#define HS_NMETHOD              "Ljdk/vm/ci/hotspot/HotSpotNmethod;"
#define HS_CONSTANT_POOL        "Ljdk/vm/ci/hotspot/HotSpotConstantPool;"
#define HS_COMPILED_CODE        "Ljdk/vm/ci/hotspot/HotSpotCompiledCode;"
#define HS_CONFIG               "Ljdk/vm/ci/hotspot/HotSpotVMConfig;"
#define HS_METADATA             "Ljdk/vm/ci/hotspot/HotSpotMetaData;"
#define HS_STACK_FRAME_REF      "Ljdk/vm/ci/hotspot/HotSpotStackFrameReference;"
#define HS_SPECULATION_LOG      "Ljdk/vm/ci/hotspot/HotSpotSpeculationLog;"
#define METASPACE_OBJECT        "Ljdk/vm/ci/hotspot/MetaspaceObject;"
#define REFLECTION_EXECUTABLE   "Ljava/lang/reflect/Executable;"
#define REFLECTION_FIELD        "Ljava/lang/reflect/Field;"
#define METASPACE_METHOD_DATA   "J"

JNINativeMethod CompilerToVM::methods[] = {
  {CC "getBytecode",                                  CC "(" HS_RESOLVED_METHOD ")[B",                                                      FN_PTR(getBytecode)},
  {CC "getExceptionTableStart",                       CC "(" HS_RESOLVED_METHOD ")J",                                                       FN_PTR(getExceptionTableStart)},
  {CC "getExceptionTableLength",                      CC "(" HS_RESOLVED_METHOD ")I",                                                       FN_PTR(getExceptionTableLength)},
  {CC "findUniqueConcreteMethod",                     CC "(" HS_RESOLVED_KLASS HS_RESOLVED_METHOD ")" HS_RESOLVED_METHOD,                   FN_PTR(findUniqueConcreteMethod)},
  {CC "getImplementor",                               CC "(" HS_RESOLVED_KLASS ")" HS_RESOLVED_KLASS,                                       FN_PTR(getImplementor)},
  {CC "getStackTraceElement",                         CC "(" HS_RESOLVED_METHOD "I)" STACK_TRACE_ELEMENT,                                   FN_PTR(getStackTraceElement)},
  {CC "methodIsIgnoredBySecurityStackWalk",           CC "(" HS_RESOLVED_METHOD ")Z",                                                       FN_PTR(methodIsIgnoredBySecurityStackWalk)},
  {CC "setNotInlinableOrCompilable",                  CC "(" HS_RESOLVED_METHOD ")V",                                                       FN_PTR(setNotInlinableOrCompilable)},
  {CC "isCompilable",                                 CC "(" HS_RESOLVED_METHOD ")Z",                                                       FN_PTR(isCompilable)},
  {CC "hasNeverInlineDirective",                      CC "(" HS_RESOLVED_METHOD ")Z",                                                       FN_PTR(hasNeverInlineDirective)},
  {CC "shouldInlineMethod",                           CC "(" HS_RESOLVED_METHOD ")Z",                                                       FN_PTR(shouldInlineMethod)},
  {CC "lookupType",                                   CC "(" STRING HS_RESOLVED_KLASS "Z)" HS_RESOLVED_TYPE,                                FN_PTR(lookupType)},
  {CC "getArrayType",                                 CC "(" HS_RESOLVED_TYPE ")" HS_RESOLVED_KLASS,                                        FN_PTR(getArrayType)},
  {CC "lookupClass",                                  CC "(" CLASS ")" HS_RESOLVED_TYPE,                                                    FN_PTR(lookupClass)},
  {CC "lookupNameInPool",                             CC "(" HS_CONSTANT_POOL "I)" STRING,                                                  FN_PTR(lookupNameInPool)},
  {CC "lookupNameAndTypeRefIndexInPool",              CC "(" HS_CONSTANT_POOL "I)I",                                                        FN_PTR(lookupNameAndTypeRefIndexInPool)},
  {CC "lookupSignatureInPool",                        CC "(" HS_CONSTANT_POOL "I)" STRING,                                                  FN_PTR(lookupSignatureInPool)},
  {CC "lookupKlassRefIndexInPool",                    CC "(" HS_CONSTANT_POOL "I)I",                                                        FN_PTR(lookupKlassRefIndexInPool)},
  {CC "lookupKlassInPool",                            CC "(" HS_CONSTANT_POOL "I)Ljava/lang/Object;",                                       FN_PTR(lookupKlassInPool)},
  {CC "lookupAppendixInPool",                         CC "(" HS_CONSTANT_POOL "I)" OBJECTCONSTANT,                                          FN_PTR(lookupAppendixInPool)},
  {CC "lookupMethodInPool",                           CC "(" HS_CONSTANT_POOL "IB)" HS_RESOLVED_METHOD,                                     FN_PTR(lookupMethodInPool)},
  {CC "constantPoolRemapInstructionOperandFromCache", CC "(" HS_CONSTANT_POOL "I)I",                                                        FN_PTR(constantPoolRemapInstructionOperandFromCache)},
  {CC "resolvePossiblyCachedConstantInPool",          CC "(" HS_CONSTANT_POOL "I)" JAVACONSTANT,                                            FN_PTR(resolvePossiblyCachedConstantInPool)},
  {CC "resolveTypeInPool",                            CC "(" HS_CONSTANT_POOL "I)" HS_RESOLVED_KLASS,                                       FN_PTR(resolveTypeInPool)},
  {CC "resolveFieldInPool",                           CC "(" HS_CONSTANT_POOL "I" HS_RESOLVED_METHOD "B[I)" HS_RESOLVED_KLASS,              FN_PTR(resolveFieldInPool)},
  {CC "resolveInvokeDynamicInPool",                   CC "(" HS_CONSTANT_POOL "I)V",                                                        FN_PTR(resolveInvokeDynamicInPool)},
  {CC "resolveInvokeHandleInPool",                    CC "(" HS_CONSTANT_POOL "I)V",                                                        FN_PTR(resolveInvokeHandleInPool)},
  {CC "isResolvedInvokeHandleInPool",                 CC "(" HS_CONSTANT_POOL "I)I",                                                        FN_PTR(isResolvedInvokeHandleInPool)},
  {CC "resolveMethod",                                CC "(" HS_RESOLVED_KLASS HS_RESOLVED_METHOD HS_RESOLVED_KLASS ")" HS_RESOLVED_METHOD, FN_PTR(resolveMethod)},
  {CC "getSignaturePolymorphicHolders",               CC "()[" STRING,                                                                      FN_PTR(getSignaturePolymorphicHolders)},
  {CC "getVtableIndexForInterfaceMethod",             CC "(" HS_RESOLVED_KLASS HS_RESOLVED_METHOD ")I",                                     FN_PTR(getVtableIndexForInterfaceMethod)},
  {CC "getClassInitializer",                          CC "(" HS_RESOLVED_KLASS ")" HS_RESOLVED_METHOD,                                      FN_PTR(getClassInitializer)},
  {CC "hasFinalizableSubclass",                       CC "(" HS_RESOLVED_KLASS ")Z",                                                        FN_PTR(hasFinalizableSubclass)},
  {CC "getMaxCallTargetOffset",                       CC "(J)J",                                                                            FN_PTR(getMaxCallTargetOffset)},
  {CC "asResolvedJavaMethod",                         CC "(" EXECUTABLE ")" HS_RESOLVED_METHOD,                                             FN_PTR(asResolvedJavaMethod)},
  {CC "getResolvedJavaMethod",                        CC "(" OBJECTCONSTANT "J)" HS_RESOLVED_METHOD,                                        FN_PTR(getResolvedJavaMethod)},
  {CC "getConstantPool",                              CC "(" METASPACE_OBJECT ")" HS_CONSTANT_POOL,                                         FN_PTR(getConstantPool)},
  {CC "getResolvedJavaType0",                         CC "(Ljava/lang/Object;JZ)" HS_RESOLVED_KLASS,                                        FN_PTR(getResolvedJavaType0)},
  {CC "readConfiguration",                            CC "()[" OBJECT,                                                                      FN_PTR(readConfiguration)},
  {CC "installCode",                                  CC "(" TARGET_DESCRIPTION HS_COMPILED_CODE INSTALLED_CODE "J[B)I",                    FN_PTR(installCode)},
  {CC "getMetadata",                                  CC "(" TARGET_DESCRIPTION HS_COMPILED_CODE HS_METADATA ")I",                          FN_PTR(getMetadata)},
  {CC "resetCompilationStatistics",                   CC "()V",                                                                             FN_PTR(resetCompilationStatistics)},
  {CC "disassembleCodeBlob",                          CC "(" INSTALLED_CODE ")" STRING,                                                     FN_PTR(disassembleCodeBlob)},
  {CC "executeHotSpotNmethod",                        CC "([" OBJECT HS_NMETHOD ")" OBJECT,                                                 FN_PTR(executeHotSpotNmethod)},
  {CC "getLineNumberTable",                           CC "(" HS_RESOLVED_METHOD ")[J",                                                      FN_PTR(getLineNumberTable)},
  {CC "getLocalVariableTableStart",                   CC "(" HS_RESOLVED_METHOD ")J",                                                       FN_PTR(getLocalVariableTableStart)},
  {CC "getLocalVariableTableLength",                  CC "(" HS_RESOLVED_METHOD ")I",                                                       FN_PTR(getLocalVariableTableLength)},
  {CC "reprofile",                                    CC "(" HS_RESOLVED_METHOD ")V",                                                       FN_PTR(reprofile)},
  {CC "invalidateHotSpotNmethod",                     CC "(" HS_NMETHOD ")V",                                                               FN_PTR(invalidateHotSpotNmethod)},
  {CC "collectCounters",                              CC "()[J",                                                                            FN_PTR(collectCounters)},
  {CC "getCountersSize",                              CC "()I",                                                                             FN_PTR(getCountersSize)},
  {CC "setCountersSize",                              CC "(I)Z",                                                                            FN_PTR(setCountersSize)},
  {CC "allocateCompileId",                            CC "(" HS_RESOLVED_METHOD "I)I",                                                      FN_PTR(allocateCompileId)},
  {CC "isMature",                                     CC "(" METASPACE_METHOD_DATA ")Z",                                                    FN_PTR(isMature)},
  {CC "hasCompiledCodeForOSR",                        CC "(" HS_RESOLVED_METHOD "II)Z",                                                     FN_PTR(hasCompiledCodeForOSR)},
  {CC "getSymbol",                                    CC "(J)" STRING,                                                                      FN_PTR(getSymbol)},
  {CC "iterateFrames",                                CC "([" RESOLVED_METHOD "[" RESOLVED_METHOD "I" INSPECTED_FRAME_VISITOR ")" OBJECT,   FN_PTR(iterateFrames)},
  {CC "materializeVirtualObjects",                    CC "(" HS_STACK_FRAME_REF "Z)V",                                                      FN_PTR(materializeVirtualObjects)},
  {CC "shouldDebugNonSafepoints",                     CC "()Z",                                                                             FN_PTR(shouldDebugNonSafepoints)},
  {CC "writeDebugOutput",                             CC "(JIZ)V",                                                                          FN_PTR(writeDebugOutput)},
  {CC "flushDebugOutput",                             CC "()V",                                                                             FN_PTR(flushDebugOutput)},
  {CC "methodDataProfileDataSize",                    CC "(JI)I",                                                                           FN_PTR(methodDataProfileDataSize)},
  {CC "getFingerprint",                               CC "(J)J",                                                                            FN_PTR(getFingerprint)},
  {CC "interpreterFrameSize",                         CC "(" BYTECODE_FRAME ")I",                                                           FN_PTR(interpreterFrameSize)},
  {CC "compileToBytecode",                            CC "(" OBJECTCONSTANT ")V",                                                           FN_PTR(compileToBytecode)},
  {CC "getFlagValue",                                 CC "(" STRING ")" OBJECT,                                                             FN_PTR(getFlagValue)},
  {CC "getInterfaces",                                CC "(" HS_RESOLVED_KLASS ")[" HS_RESOLVED_KLASS,                                      FN_PTR(getInterfaces)},
  {CC "getComponentType",                             CC "(" HS_RESOLVED_KLASS ")" HS_RESOLVED_TYPE,                                        FN_PTR(getComponentType)},
  {CC "ensureInitialized",                            CC "(" HS_RESOLVED_KLASS ")V",                                                        FN_PTR(ensureInitialized)},
  {CC "ensureLinked",                                 CC "(" HS_RESOLVED_KLASS ")V",                                                        FN_PTR(ensureLinked)},
  {CC "getIdentityHashCode",                          CC "(" OBJECTCONSTANT ")I",                                                           FN_PTR(getIdentityHashCode)},
  {CC "isInternedString",                             CC "(" OBJECTCONSTANT ")Z",                                                           FN_PTR(isInternedString)},
  {CC "unboxPrimitive",                               CC "(" OBJECTCONSTANT ")" OBJECT,                                                     FN_PTR(unboxPrimitive)},
  {CC "boxPrimitive",                                 CC "(" OBJECT ")" OBJECTCONSTANT,                                                     FN_PTR(boxPrimitive)},
  {CC "getDeclaredConstructors",                      CC "(" HS_RESOLVED_KLASS ")[" RESOLVED_METHOD,                                        FN_PTR(getDeclaredConstructors)},
  {CC "getDeclaredMethods",                           CC "(" HS_RESOLVED_KLASS ")[" RESOLVED_METHOD,                                        FN_PTR(getDeclaredMethods)},
  {CC "readFieldValue",                               CC "(" HS_RESOLVED_KLASS HS_RESOLVED_KLASS "JZLjdk/vm/ci/meta/JavaKind;)" JAVACONSTANT, FN_PTR(readFieldValue)},
  {CC "readFieldValue",                               CC "(" OBJECTCONSTANT HS_RESOLVED_KLASS "JZLjdk/vm/ci/meta/JavaKind;)" JAVACONSTANT,  FN_PTR(readFieldValue)},
  {CC "isInstance",                                   CC "(" HS_RESOLVED_KLASS OBJECTCONSTANT ")Z",                                         FN_PTR(isInstance)},
  {CC "isAssignableFrom",                             CC "(" HS_RESOLVED_KLASS HS_RESOLVED_KLASS ")Z",                                      FN_PTR(isAssignableFrom)},
  {CC "isTrustedForIntrinsics",                       CC "(" HS_RESOLVED_KLASS ")Z",                                                        FN_PTR(isTrustedForIntrinsics)},
  {CC "asJavaType",                                   CC "(" OBJECTCONSTANT ")" HS_RESOLVED_TYPE,                                           FN_PTR(asJavaType)},
  {CC "asString",                                     CC "(" OBJECTCONSTANT ")" STRING,                                                     FN_PTR(asString)},
  {CC "equals",                                       CC "(" OBJECTCONSTANT "J" OBJECTCONSTANT "J)Z",                                       FN_PTR(equals)},
  {CC "getJavaMirror",                                CC "(" HS_RESOLVED_TYPE ")" OBJECTCONSTANT,                                           FN_PTR(getJavaMirror)},
  {CC "getArrayLength",                               CC "(" OBJECTCONSTANT ")I",                                                           FN_PTR(getArrayLength)},
  {CC "readArrayElement",                             CC "(" OBJECTCONSTANT "I)Ljava/lang/Object;",                                         FN_PTR(readArrayElement)},
  {CC "arrayBaseOffset",                              CC "(Ljdk/vm/ci/meta/JavaKind;)I",                                                    FN_PTR(arrayBaseOffset)},
  {CC "arrayIndexScale",                              CC "(Ljdk/vm/ci/meta/JavaKind;)I",                                                    FN_PTR(arrayIndexScale)},
  {CC "deleteGlobalHandle",                           CC "(J)V",                                                                            FN_PTR(deleteGlobalHandle)},
  {CC "registerNativeMethods",                        CC "(" CLASS ")[J",                                                                   FN_PTR(registerNativeMethods)},
  {CC "isCurrentThreadAttached",                      CC "()Z",                                                                             FN_PTR(isCurrentThreadAttached)},
  {CC "getCurrentJavaThread",                         CC "()J",                                                                             FN_PTR(getCurrentJavaThread)},
  {CC "attachCurrentThread",                          CC "([BZ)Z",                                                                          FN_PTR(attachCurrentThread)},
  {CC "detachCurrentThread",                          CC "()V",                                                                             FN_PTR(detachCurrentThread)},
  {CC "translate",                                    CC "(" OBJECT ")J",                                                                   FN_PTR(translate)},
  {CC "unhand",                                       CC "(J)" OBJECT,                                                                      FN_PTR(unhand)},
  {CC "updateHotSpotNmethod",                         CC "(" HS_NMETHOD ")V",                                                               FN_PTR(updateHotSpotNmethod)},
  {CC "getCode",                                      CC "(" HS_INSTALLED_CODE ")[B",                                                       FN_PTR(getCode)},
  {CC "asReflectionExecutable",                       CC "(" HS_RESOLVED_METHOD ")" REFLECTION_EXECUTABLE,                                  FN_PTR(asReflectionExecutable)},
  {CC "asReflectionField",                            CC "(" HS_RESOLVED_KLASS "I)" REFLECTION_FIELD,                                       FN_PTR(asReflectionField)},
  {CC "getFailedSpeculations",                        CC "(J[[B)[[B",                                                                       FN_PTR(getFailedSpeculations)},
  {CC "getFailedSpeculationsAddress",                 CC "(" HS_RESOLVED_METHOD ")J",                                                       FN_PTR(getFailedSpeculationsAddress)},
  {CC "releaseFailedSpeculations",                    CC "(J)V",                                                                            FN_PTR(releaseFailedSpeculations)},
  {CC "addFailedSpeculation",                         CC "(J[B)Z",                                                                          FN_PTR(addFailedSpeculation)},
  {CC "callSystemExit",                               CC "(I)V",                                                                            FN_PTR(callSystemExit)},
  {CC "ticksNow",                                     CC "()J",                                                                             FN_PTR(ticksNow)},
  {CC "registerCompilerPhase",                        CC "(" STRING ")I",                                                                   FN_PTR(registerCompilerPhase)},
  {CC "notifyCompilerPhaseEvent",                     CC "(JIII)V",                                                                         FN_PTR(notifyCompilerPhaseEvent)},
  {CC "notifyCompilerInliningEvent",                  CC "(I" HS_RESOLVED_METHOD HS_RESOLVED_METHOD "ZLjava/lang/String;I)V",               FN_PTR(notifyCompilerInliningEvent)},
};

int CompilerToVM::methods_count() {
  return sizeof(methods) / sizeof(JNINativeMethod);
}
