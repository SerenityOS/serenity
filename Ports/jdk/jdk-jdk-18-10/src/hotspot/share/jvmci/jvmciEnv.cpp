/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm_io.h"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "code/codeCache.hpp"
#include "compiler/compilerOracle.hpp"
#include "compiler/compileTask.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "jvmci/jniAccessMark.inline.hpp"
#include "jvmci/jvmciCompiler.hpp"
#include "jvmci/jvmciRuntime.hpp"

JVMCICompileState::JVMCICompileState(CompileTask* task, JVMCICompiler* compiler):
  _task(task),
  _compiler(compiler),
  _retryable(true),
  _failure_reason(NULL),
  _failure_reason_on_C_heap(false) {
  // Get Jvmti capabilities under lock to get consistent values.
  MutexLocker mu(JvmtiThreadState_lock);
  _jvmti_redefinition_count             = JvmtiExport::redefinition_count();
  _jvmti_can_hotswap_or_post_breakpoint = JvmtiExport::can_hotswap_or_post_breakpoint() ? 1 : 0;
  _jvmti_can_access_local_variables     = JvmtiExport::can_access_local_variables() ? 1 : 0;
  _jvmti_can_post_on_exceptions         = JvmtiExport::can_post_on_exceptions() ? 1 : 0;
  _jvmti_can_pop_frame                  = JvmtiExport::can_pop_frame() ? 1 : 0;
  _target_method_is_old                 = _task != NULL && _task->method()->is_old();
  if (task->is_blocking()) {
    task->set_blocking_jvmci_compile_state(this);
  }
}

// Update global JVMCI compilation ticks after 512 thread-local JVMCI compilation ticks.
// This mitigates the overhead of the atomic operation used for the global update.
#define THREAD_TICKS_PER_GLOBAL_TICKS (2 << 9)
#define THREAD_TICKS_PER_GLOBAL_TICKS_MASK (THREAD_TICKS_PER_GLOBAL_TICKS - 1)

void JVMCICompileState::inc_compilation_ticks() {
  if ((++_compilation_ticks & THREAD_TICKS_PER_GLOBAL_TICKS_MASK) == 0) {
    _compiler->inc_global_compilation_ticks();
  }
}

bool JVMCICompileState::jvmti_state_changed() const {
  // Some classes were redefined
  if (jvmti_redefinition_count() != JvmtiExport::redefinition_count()) {
    return true;
  }
  if (!jvmti_can_access_local_variables() &&
      JvmtiExport::can_access_local_variables()) {
    return true;
  }
  if (!jvmti_can_hotswap_or_post_breakpoint() &&
      JvmtiExport::can_hotswap_or_post_breakpoint()) {
    return true;
  }
  if (!jvmti_can_post_on_exceptions() &&
      JvmtiExport::can_post_on_exceptions()) {
    return true;
  }
  if (!jvmti_can_pop_frame() &&
      JvmtiExport::can_pop_frame()) {
    return true;
  }
  return false;
}

void JVMCIEnv::copy_saved_properties() {
  assert(!is_hotspot(), "can only copy saved properties from HotSpot to native image");

  JavaThread* THREAD = JavaThread::current(); // For exception macros.

  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_vm_ci_services_Services(), Handle(), Handle(), true, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    JVMCIRuntime::fatal_exception(NULL, "Error initializing jdk.vm.ci.services.Services");
  }
  InstanceKlass* ik = InstanceKlass::cast(k);
  if (ik->should_be_initialized()) {
    ik->initialize(THREAD);
    if (HAS_PENDING_EXCEPTION) {
      JVMCIRuntime::fatal_exception(NULL, "Error initializing jdk.vm.ci.services.Services");
    }
  }

  // Get the serialized saved properties from HotSpot
  TempNewSymbol serializeSavedProperties = SymbolTable::new_symbol("serializeSavedProperties");
  JavaValue result(T_OBJECT);
  JavaCallArguments args;
  JavaCalls::call_static(&result, ik, serializeSavedProperties, vmSymbols::serializePropertiesToByteArray_signature(), &args, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    JVMCIRuntime::fatal_exception(NULL, "Error calling jdk.vm.ci.services.Services.serializeSavedProperties");
  }
  oop res = result.get_oop();
  assert(res->is_typeArray(), "must be");
  assert(TypeArrayKlass::cast(res->klass())->element_type() == T_BYTE, "must be");
  typeArrayOop ba = typeArrayOop(res);
  int serialized_properties_len = ba->length();

  // Copy serialized saved properties from HotSpot object into native buffer
  jbyte* serialized_properties = NEW_RESOURCE_ARRAY(jbyte, serialized_properties_len);
  memcpy(serialized_properties, ba->byte_at_addr(0), serialized_properties_len);

  // Copy native buffer into shared library object
  JVMCIPrimitiveArray buf = new_byteArray(serialized_properties_len, this);
  if (has_pending_exception()) {
    describe_pending_exception(true);
    fatal("Error in copy_saved_properties");
  }
  copy_bytes_from(serialized_properties, buf, 0, serialized_properties_len);
  if (has_pending_exception()) {
    describe_pending_exception(true);
    fatal("Error in copy_saved_properties");
  }

  // Initialize saved properties in shared library
  jclass servicesClass = JNIJVMCI::Services::clazz();
  jmethodID initializeSavedProperties = JNIJVMCI::Services::initializeSavedProperties_method();
  JNIAccessMark jni(this, THREAD);
  jni()->CallStaticVoidMethod(servicesClass, initializeSavedProperties, buf.as_jobject());
  if (jni()->ExceptionCheck()) {
    jni()->ExceptionDescribe();
    fatal("Error calling jdk.vm.ci.services.Services.initializeSavedProperties");
  }
}

void JVMCIEnv::init_env_mode_runtime(JavaThread* thread, JNIEnv* parent_env) {
  assert(thread != NULL, "npe");
  _env = NULL;
  _pop_frame_on_close = false;
  _detach_on_close = false;
  if (!UseJVMCINativeLibrary) {
    // In HotSpot mode, JNI isn't used at all.
    _runtime = JVMCI::java_runtime();
    _is_hotspot = true;
    return;
  }

  if (parent_env != NULL) {
    // If the parent JNI environment is non-null then figure out whether it
    // is a HotSpot or shared library JNIEnv and set the state appropriately.
    _is_hotspot = thread->jni_environment() == parent_env;
    if (_is_hotspot) {
      // Select the Java runtime
      _runtime = JVMCI::java_runtime();
      return;
    }
    _runtime = JVMCI::compiler_runtime();
    assert(_runtime != NULL, "npe");
    _env = parent_env;
    return;
  }

  // Running in JVMCI shared library mode so ensure the shared library
  // is loaded and initialized and get a shared library JNIEnv
  _is_hotspot = false;

  _runtime = JVMCI::compiler_runtime();
  _env = _runtime->init_shared_library_javavm();

  if (_env != NULL) {
    // Creating the JVMCI shared library VM also attaches the current thread
    _detach_on_close = true;
  } else {
    _runtime->GetEnv(thread, (void**)&parent_env, JNI_VERSION_1_2);
    if (parent_env != NULL) {
      // Even though there's a parent JNI env, there's no guarantee
      // it was opened by a JVMCIEnv scope and thus may not have
      // pushed a local JNI frame. As such, we use a new JNI local
      // frame in this scope to ensure local JNI refs are collected
      // in a timely manner after leaving this scope.
      _env = parent_env;
    } else {
      ResourceMark rm; // Thread name is resource allocated
      JavaVMAttachArgs attach_args;
      attach_args.version = JNI_VERSION_1_2;
      attach_args.name = const_cast<char*>(thread->name());
      attach_args.group = NULL;
      if (_runtime->AttachCurrentThread(thread, (void**) &_env, &attach_args) != JNI_OK) {
        fatal("Error attaching current thread (%s) to JVMCI shared library JNI interface", attach_args.name);
      }
      _detach_on_close = true;
    }
  }

  assert(_env != NULL, "missing env");
  assert(_throw_to_caller == false, "must be");

  JNIAccessMark jni(this, thread);
  jint result = _env->PushLocalFrame(32);
  if (result != JNI_OK) {
    char message[256];
    jio_snprintf(message, 256, "Uncaught exception pushing local frame for JVMCIEnv scope entered at %s:%d", _file, _line);
    JVMCIRuntime::fatal_exception(this, message);
  }
  _pop_frame_on_close = true;
}

JVMCIEnv::JVMCIEnv(JavaThread* thread, JVMCICompileState* compile_state, const char* file, int line):
    _throw_to_caller(false), _file(file), _line(line), _compile_state(compile_state) {
  init_env_mode_runtime(thread, NULL);
}

JVMCIEnv::JVMCIEnv(JavaThread* thread, const char* file, int line):
    _throw_to_caller(false), _file(file), _line(line), _compile_state(NULL) {
  init_env_mode_runtime(thread, NULL);
}

JVMCIEnv::JVMCIEnv(JavaThread* thread, JNIEnv* parent_env, const char* file, int line):
    _throw_to_caller(true), _file(file), _line(line), _compile_state(NULL) {
  init_env_mode_runtime(thread, parent_env);
  assert(_env == NULL || parent_env == _env, "mismatched JNIEnvironment");
}

void JVMCIEnv::init(JavaThread* thread, bool is_hotspot, const char* file, int line) {
  _compile_state = NULL;
  _throw_to_caller = false;
  _file = file;
  _line = line;
  if (is_hotspot) {
    _env = NULL;
    _pop_frame_on_close = false;
    _detach_on_close = false;
    _is_hotspot = true;
    _runtime = JVMCI::java_runtime();
  } else {
    init_env_mode_runtime(thread, NULL);
  }
}

// Prints a pending exception (if any) and its stack trace.
void JVMCIEnv::describe_pending_exception(bool clear) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (!is_hotspot()) {
    JNIAccessMark jni(this, THREAD);
    if (jni()->ExceptionCheck()) {
      jthrowable ex = !clear ? jni()->ExceptionOccurred() : NULL;
      jni()->ExceptionDescribe();
      if (ex != NULL) {
        jni()->Throw(ex);
      }
    }
  } else {
    if (HAS_PENDING_EXCEPTION) {
      JVMCIRuntime::describe_pending_hotspot_exception(THREAD, clear);
    }
  }
}

void JVMCIEnv::translate_hotspot_exception_to_jni_exception(JavaThread* THREAD, const Handle& throwable) {
  assert(!is_hotspot(), "must_be");
  // Resolve HotSpotJVMCIRuntime class explicitly as HotSpotJVMCI::compute_offsets
  // may not have been called.
  Klass* runtimeKlass = SystemDictionary::resolve_or_fail(vmSymbols::jdk_vm_ci_hotspot_HotSpotJVMCIRuntime(), true, CHECK);
  JavaCallArguments jargs;
  jargs.push_oop(throwable);
  JavaValue result(T_OBJECT);
  JavaCalls::call_static(&result,
                          runtimeKlass,
                          vmSymbols::encodeThrowable_name(),
                          vmSymbols::encodeThrowable_signature(), &jargs, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    JVMCIRuntime::fatal_exception(this, "HotSpotJVMCIRuntime.encodeThrowable should not throw an exception");
  }

  oop encoded_throwable_string = result.get_oop();

  ResourceMark rm;
  const char* encoded_throwable_chars = java_lang_String::as_utf8_string(encoded_throwable_string);

  JNIAccessMark jni(this, THREAD);
  jobject jni_encoded_throwable_string = jni()->NewStringUTF(encoded_throwable_chars);
  jthrowable jni_throwable = (jthrowable) jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotJVMCIRuntime::clazz(),
                                JNIJVMCI::HotSpotJVMCIRuntime::decodeThrowable_method(),
                                jni_encoded_throwable_string);
  jni()->Throw(jni_throwable);
}

JVMCIEnv::~JVMCIEnv() {
  if (_throw_to_caller) {
    if (is_hotspot()) {
      // Nothing to do
    } else {
      Thread* thread = Thread::current();
      if (thread->is_Java_thread()) {
        JavaThread* THREAD = JavaThread::cast(thread); // For exception macros.
        if (HAS_PENDING_EXCEPTION) {
          Handle throwable = Handle(THREAD, PENDING_EXCEPTION);
          CLEAR_PENDING_EXCEPTION;
          translate_hotspot_exception_to_jni_exception(THREAD, throwable);
        }
      }
    }
  } else {
    if (_pop_frame_on_close) {
      // Pop the JNI local frame that was pushed when entering this JVMCIEnv scope.
      JNIAccessMark jni(this);
      jni()->PopLocalFrame(NULL);
    }

    if (has_pending_exception()) {
      char message[256];
      jio_snprintf(message, 256, "Uncaught exception exiting JVMCIEnv scope entered at %s:%d", _file, _line);
      JVMCIRuntime::fatal_exception(this, message);
    }

    if (_detach_on_close) {
      _runtime->DetachCurrentThread(JavaThread::current());
    }
  }
}

jboolean JVMCIEnv::has_pending_exception() {
  if (is_hotspot()) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    return HAS_PENDING_EXCEPTION;
  } else {
    JNIAccessMark jni(this);
    return jni()->ExceptionCheck();
  }
}

void JVMCIEnv::clear_pending_exception() {
  if (is_hotspot()) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    CLEAR_PENDING_EXCEPTION;
  } else {
    JNIAccessMark jni(this);
    jni()->ExceptionClear();
  }
}

int JVMCIEnv::get_length(JVMCIArray array) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(array)->length();
  } else {
    JNIAccessMark jni(this);
    return jni()->GetArrayLength(get_jarray(array));
  }
}

JVMCIObject JVMCIEnv::get_object_at(JVMCIObjectArray array, int index) {
  if (is_hotspot()) {
    oop result = HotSpotJVMCI::resolve(array)->obj_at(index);
    return wrap(result);
  } else {
    JNIAccessMark jni(this);
    jobject result = jni()->GetObjectArrayElement(get_jobjectArray(array), index);
    return wrap(result);
  }
}

void JVMCIEnv::put_object_at(JVMCIObjectArray array, int index, JVMCIObject value) {
  if (is_hotspot()) {
    HotSpotJVMCI::resolve(array)->obj_at_put(index, HotSpotJVMCI::resolve(value));
  } else {
    JNIAccessMark jni(this);
    jni()->SetObjectArrayElement(get_jobjectArray(array), index, get_jobject(value));
  }
}

jboolean JVMCIEnv::get_bool_at(JVMCIPrimitiveArray array, int index) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(array)->bool_at(index);
  } else {
    JNIAccessMark jni(this);
    jboolean result;
    jni()->GetBooleanArrayRegion(array.as_jbooleanArray(), index, 1, &result);
    return result;
  }
}
void JVMCIEnv::put_bool_at(JVMCIPrimitiveArray array, int index, jboolean value) {
  if (is_hotspot()) {
    HotSpotJVMCI::resolve(array)->bool_at_put(index, value);
  } else {
    JNIAccessMark jni(this);
    jni()->SetBooleanArrayRegion(array.as_jbooleanArray(), index, 1, &value);
  }
}

jbyte JVMCIEnv::get_byte_at(JVMCIPrimitiveArray array, int index) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(array)->byte_at(index);
  } else {
    JNIAccessMark jni(this);
    jbyte result;
    jni()->GetByteArrayRegion(array.as_jbyteArray(), index, 1, &result);
    return result;
  }
}
void JVMCIEnv::put_byte_at(JVMCIPrimitiveArray array, int index, jbyte value) {
  if (is_hotspot()) {
    HotSpotJVMCI::resolve(array)->byte_at_put(index, value);
  } else {
    JNIAccessMark jni(this);
    jni()->SetByteArrayRegion(array.as_jbyteArray(), index, 1, &value);
  }
}

jint JVMCIEnv::get_int_at(JVMCIPrimitiveArray array, int index) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(array)->int_at(index);
  } else {
    JNIAccessMark jni(this);
    jint result;
    jni()->GetIntArrayRegion(array.as_jintArray(), index, 1, &result);
    return result;
  }
}
void JVMCIEnv::put_int_at(JVMCIPrimitiveArray array, int index, jint value) {
  if (is_hotspot()) {
    HotSpotJVMCI::resolve(array)->int_at_put(index, value);
  } else {
    JNIAccessMark jni(this);
    jni()->SetIntArrayRegion(array.as_jintArray(), index, 1, &value);
  }
}

long JVMCIEnv::get_long_at(JVMCIPrimitiveArray array, int index) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(array)->long_at(index);
  } else {
    JNIAccessMark jni(this);
    jlong result;
    jni()->GetLongArrayRegion(array.as_jlongArray(), index, 1, &result);
    return result;
  }
}
void JVMCIEnv::put_long_at(JVMCIPrimitiveArray array, int index, jlong value) {
  if (is_hotspot()) {
    HotSpotJVMCI::resolve(array)->long_at_put(index, value);
  } else {
    JNIAccessMark jni(this);
    jni()->SetLongArrayRegion(array.as_jlongArray(), index, 1, &value);
  }
}

void JVMCIEnv::copy_bytes_to(JVMCIPrimitiveArray src, jbyte* dest, int offset, jsize length) {
  if (length == 0) {
    return;
  }
  if (is_hotspot()) {
    memcpy(dest, HotSpotJVMCI::resolve(src)->byte_at_addr(offset), length);
  } else {
    JNIAccessMark jni(this);
    jni()->GetByteArrayRegion(src.as_jbyteArray(), offset, length, dest);
  }
}
void JVMCIEnv::copy_bytes_from(jbyte* src, JVMCIPrimitiveArray dest, int offset, jsize length) {
  if (length == 0) {
    return;
  }
  if (is_hotspot()) {
    memcpy(HotSpotJVMCI::resolve(dest)->byte_at_addr(offset), src, length);
  } else {
    JNIAccessMark jni(this);
    jni()->SetByteArrayRegion(dest.as_jbyteArray(), offset, length, src);
  }
}

void JVMCIEnv::copy_longs_from(jlong* src, JVMCIPrimitiveArray dest, int offset, jsize length) {
  if (length == 0) {
    return;
  }
  if (is_hotspot()) {
    memcpy(HotSpotJVMCI::resolve(dest)->long_at_addr(offset), src, length * sizeof(jlong));
  } else {
    JNIAccessMark jni(this);
    jni()->SetLongArrayRegion(dest.as_jlongArray(), offset, length, src);
  }
}

jboolean JVMCIEnv::is_boxing_object(BasicType type, JVMCIObject object) {
  if (is_hotspot()) {
    return java_lang_boxing_object::is_instance(HotSpotJVMCI::resolve(object), type);
  } else {
    JNIAccessMark jni(this);
    return jni()->IsInstanceOf(get_jobject(object), JNIJVMCI::box_class(type));
  }
}

// Get the primitive value from a Java boxing object.  It's hard error to
// pass a non-primitive BasicType.
jvalue JVMCIEnv::get_boxed_value(BasicType type, JVMCIObject object) {
  jvalue result;
  if (is_hotspot()) {
    if (java_lang_boxing_object::get_value(HotSpotJVMCI::resolve(object), &result) == T_ILLEGAL) {
      ShouldNotReachHere();
    }
  } else {
    JNIAccessMark jni(this);
    jfieldID field = JNIJVMCI::box_field(type);
    switch (type) {
      case T_BOOLEAN: result.z = jni()->GetBooleanField(get_jobject(object), field); break;
      case T_BYTE:    result.b = jni()->GetByteField(get_jobject(object), field); break;
      case T_SHORT:   result.s = jni()->GetShortField(get_jobject(object), field); break;
      case T_CHAR:    result.c = jni()->GetCharField(get_jobject(object), field); break;
      case T_INT:     result.i = jni()->GetIntField(get_jobject(object), field); break;
      case T_LONG:    result.j = jni()->GetLongField(get_jobject(object), field); break;
      case T_FLOAT:   result.f = jni()->GetFloatField(get_jobject(object), field); break;
      case T_DOUBLE:  result.d = jni()->GetDoubleField(get_jobject(object), field); break;
      default:
        ShouldNotReachHere();
    }
  }
  return result;
}

// Return the BasicType of the object if it's a boxing object, otherwise return T_ILLEGAL.
BasicType JVMCIEnv::get_box_type(JVMCIObject object) {
  if (is_hotspot()) {
    return java_lang_boxing_object::basic_type(HotSpotJVMCI::resolve(object));
  } else {
    JNIAccessMark jni(this);
    jclass clazz = jni()->GetObjectClass(get_jobject(object));
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_BOOLEAN))) return T_BOOLEAN;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_BYTE))) return T_BYTE;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_SHORT))) return T_SHORT;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_CHAR))) return T_CHAR;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_INT))) return T_INT;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_LONG))) return T_LONG;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_FLOAT))) return T_FLOAT;
    if (jni()->IsSameObject(clazz, JNIJVMCI::box_class(T_DOUBLE))) return T_DOUBLE;
    return T_ILLEGAL;
  }
}

// Create a boxing object of the appropriate primitive type.
JVMCIObject JVMCIEnv::create_box(BasicType type, jvalue* value, JVMCI_TRAPS) {
  switch (type) {
    case T_BOOLEAN:
    case T_BYTE:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
    case T_LONG:
    case T_FLOAT:
    case T_DOUBLE:
      break;
    default:
      JVMCI_THROW_MSG_(IllegalArgumentException, "Only boxes for primitive values can be created", JVMCIObject());
  }
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    oop box = java_lang_boxing_object::create(type, value, CHECK_(JVMCIObject()));
    return HotSpotJVMCI::wrap(box);
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject box = jni()->NewObjectA(JNIJVMCI::box_class(type), JNIJVMCI::box_constructor(type), value);
    assert(box != NULL, "");
    return wrap(box);
  }
}

const char* JVMCIEnv::as_utf8_string(JVMCIObject str) {
  if (is_hotspot()) {
    return java_lang_String::as_utf8_string(HotSpotJVMCI::resolve(str));
  } else {
    JNIAccessMark jni(this);
    int length = jni()->GetStringLength(str.as_jstring());
    int utf8_length = jni()->GetStringUTFLength(str.as_jstring());
    char* result = NEW_RESOURCE_ARRAY(char, utf8_length + 1);
    jni()->GetStringUTFRegion(str.as_jstring(), 0, length, result);
    return result;
  }
}

#define DO_THROW(name)                             \
void JVMCIEnv::throw_##name(const char* msg) {     \
  if (is_hotspot()) {                              \
    JavaThread* THREAD = JavaThread::current();    \
    THROW_MSG(HotSpotJVMCI::name::symbol(), msg);  \
  } else {                                         \
    JNIAccessMark jni(this);                       \
    jni()->ThrowNew(JNIJVMCI::name::clazz(), msg); \
  }                                                \
}

DO_THROW(InternalError)
DO_THROW(ArrayIndexOutOfBoundsException)
DO_THROW(IllegalStateException)
DO_THROW(NullPointerException)
DO_THROW(IllegalArgumentException)
DO_THROW(InvalidInstalledCodeException)
DO_THROW(UnsatisfiedLinkError)
DO_THROW(UnsupportedOperationException)
DO_THROW(ClassNotFoundException)

#undef DO_THROW

void JVMCIEnv::fthrow_error(const char* file, int line, const char* format, ...) {
  const int max_msg_size = 1024;
  va_list ap;
  va_start(ap, format);
  char msg[max_msg_size];
  vsnprintf(msg, max_msg_size, format, ap);
  msg[max_msg_size-1] = '\0';
  va_end(ap);
  JavaThread* THREAD = JavaThread::current();
  if (is_hotspot()) {
    Handle h_loader = Handle();
    Handle h_protection_domain = Handle();
    Exceptions::_throw_msg(THREAD, file, line, vmSymbols::jdk_vm_ci_common_JVMCIError(), msg, h_loader, h_protection_domain);
  } else {
    JNIAccessMark jni(this, THREAD);
    jni()->ThrowNew(JNIJVMCI::JVMCIError::clazz(), msg);
  }
}

jboolean JVMCIEnv::call_HotSpotJVMCIRuntime_isGCSupported (JVMCIObject runtime, jint gcIdentifier) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(runtime)));
    jargs.push_int(gcIdentifier);
    JavaValue result(T_BOOLEAN);
    JavaCalls::call_special(&result,
                            HotSpotJVMCI::HotSpotJVMCIRuntime::klass(),
                            vmSymbols::isGCSupported_name(),
                            vmSymbols::int_bool_signature(), &jargs, CHECK_0);
    return result.get_jboolean();
  } else {
    JNIAccessMark jni(this, THREAD);
    jboolean result = jni()->CallNonvirtualBooleanMethod(runtime.as_jobject(),
                                                     JNIJVMCI::HotSpotJVMCIRuntime::clazz(),
                                                     JNIJVMCI::HotSpotJVMCIRuntime::isGCSupported_method(),
                                                     gcIdentifier);
    if (jni()->ExceptionCheck()) {
      return false;
    }
    return result;
  }
}

JVMCIObject JVMCIEnv::call_HotSpotJVMCIRuntime_compileMethod (JVMCIObject runtime, JVMCIObject method, int entry_bci,
                                                              jlong compile_state, int id) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(runtime)));
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(method)));
    jargs.push_int(entry_bci);
    jargs.push_long(compile_state);
    jargs.push_int(id);
    JavaValue result(T_OBJECT);
    JavaCalls::call_special(&result,
                            HotSpotJVMCI::HotSpotJVMCIRuntime::klass(),
                            vmSymbols::compileMethod_name(),
                            vmSymbols::compileMethod_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->CallNonvirtualObjectMethod(runtime.as_jobject(),
                                                     JNIJVMCI::HotSpotJVMCIRuntime::clazz(),
                                                     JNIJVMCI::HotSpotJVMCIRuntime::compileMethod_method(),
                                                     method.as_jobject(), entry_bci, compile_state, id);
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}

void JVMCIEnv::call_HotSpotJVMCIRuntime_bootstrapFinished (JVMCIObject runtime, JVMCIEnv* JVMCIENV) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(runtime)));
    JavaValue result(T_VOID);
    JavaCalls::call_special(&result, HotSpotJVMCI::HotSpotJVMCIRuntime::klass(), vmSymbols::bootstrapFinished_name(), vmSymbols::void_method_signature(), &jargs, CHECK);
  } else {
    JNIAccessMark jni(this, THREAD);
    jni()->CallNonvirtualVoidMethod(runtime.as_jobject(), JNIJVMCI::HotSpotJVMCIRuntime::clazz(), JNIJVMCI::HotSpotJVMCIRuntime::bootstrapFinished_method());

  }
}

void JVMCIEnv::call_HotSpotJVMCIRuntime_shutdown (JVMCIObject runtime) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  HandleMark hm(THREAD);
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(runtime)));
    JavaValue result(T_VOID);
    JavaCalls::call_special(&result, HotSpotJVMCI::HotSpotJVMCIRuntime::klass(), vmSymbols::shutdown_name(), vmSymbols::void_method_signature(), &jargs, THREAD);
  } else {
    JNIAccessMark jni(this, THREAD);
    jni()->CallNonvirtualVoidMethod(runtime.as_jobject(), JNIJVMCI::HotSpotJVMCIRuntime::clazz(), JNIJVMCI::HotSpotJVMCIRuntime::shutdown_method());
  }
  if (has_pending_exception()) {
    // This should never happen as HotSpotJVMCIRuntime.shutdown() should
    // handle all exceptions.
    describe_pending_exception(true);
  }
}

JVMCIObject JVMCIEnv::call_HotSpotJVMCIRuntime_runtime (JVMCIEnv* JVMCIENV) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    JavaValue result(T_OBJECT);
    JavaCalls::call_static(&result, HotSpotJVMCI::HotSpotJVMCIRuntime::klass(), vmSymbols::runtime_name(), vmSymbols::runtime_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotJVMCIRuntime::clazz(), JNIJVMCI::HotSpotJVMCIRuntime::runtime_method());
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::call_JVMCI_getRuntime (JVMCIEnv* JVMCIENV) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    JavaValue result(T_OBJECT);
    JavaCalls::call_static(&result, HotSpotJVMCI::JVMCI::klass(), vmSymbols::getRuntime_name(), vmSymbols::getRuntime_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->CallStaticObjectMethod(JNIJVMCI::JVMCI::clazz(), JNIJVMCI::JVMCI::getRuntime_method());
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::call_HotSpotJVMCIRuntime_getCompiler (JVMCIObject runtime, JVMCIEnv* JVMCIENV) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(runtime)));
    JavaValue result(T_OBJECT);
    JavaCalls::call_virtual(&result, HotSpotJVMCI::HotSpotJVMCIRuntime::klass(), vmSymbols::getCompiler_name(), vmSymbols::getCompiler_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->CallObjectMethod(runtime.as_jobject(), JNIJVMCI::HotSpotJVMCIRuntime::getCompiler_method());
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}


JVMCIObject JVMCIEnv::call_HotSpotJVMCIRuntime_callToString(JVMCIObject object, JVMCIEnv* JVMCIENV) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(object)));
    JavaValue result(T_OBJECT);
    JavaCalls::call_static(&result,
                           HotSpotJVMCI::HotSpotJVMCIRuntime::klass(),
                           vmSymbols::callToString_name(),
                           vmSymbols::callToString_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = (jstring) jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotJVMCIRuntime::clazz(),
                                                     JNIJVMCI::HotSpotJVMCIRuntime::callToString_method(),
                                                     object.as_jobject());
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}


JVMCIObject JVMCIEnv::call_JavaConstant_forPrimitive(JVMCIObject kind, jlong value, JVMCI_TRAPS) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaCallArguments jargs;
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(kind)));
    jargs.push_long(value);
    JavaValue result(T_OBJECT);
    JavaCalls::call_static(&result,
                           HotSpotJVMCI::JavaConstant::klass(),
                           vmSymbols::forPrimitive_name(),
                           vmSymbols::forPrimitive_signature(), &jargs, CHECK_(JVMCIObject()));
    return wrap(result.get_oop());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = (jstring) jni()->CallStaticObjectMethod(JNIJVMCI::JavaConstant::clazz(),
                                                             JNIJVMCI::JavaConstant::forPrimitive_method(),
                                                             kind.as_jobject(), value);
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::get_jvmci_primitive_type(BasicType type) {
  JVMCIObjectArray primitives = get_HotSpotResolvedPrimitiveType_primitives();
  JVMCIObject result = get_object_at(primitives, type);
  return result;
}

JVMCIObject JVMCIEnv::new_StackTraceElement(const methodHandle& method, int bci, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  Symbol* file_name_sym;
  int line_number;
  java_lang_StackTraceElement::decode(method, bci, file_name_sym, line_number, CHECK_(JVMCIObject()));

  Symbol* method_name_sym = method->name();
  InstanceKlass* holder = method->method_holder();
  const char* declaring_class_str = holder->external_name();

  if (is_hotspot()) {
    HotSpotJVMCI::StackTraceElement::klass()->initialize(CHECK_(JVMCIObject()));
    oop objOop = HotSpotJVMCI::StackTraceElement::klass()->allocate_instance(CHECK_(JVMCIObject()));
    Handle obj = Handle(THREAD, objOop);

    oop declaring_class = StringTable::intern((char*) declaring_class_str, CHECK_(JVMCIObject()));
    HotSpotJVMCI::StackTraceElement::set_declaringClass(this, obj(), declaring_class);

    oop method_name = StringTable::intern(method_name_sym, CHECK_(JVMCIObject()));
    HotSpotJVMCI::StackTraceElement::set_methodName(this, obj(), method_name);

    if (file_name_sym != NULL) {
      oop file_name = StringTable::intern(file_name_sym, CHECK_(JVMCIObject()));
      HotSpotJVMCI::StackTraceElement::set_fileName(this, obj(), file_name);
    }
    HotSpotJVMCI::StackTraceElement::set_lineNumber(this, obj(), line_number);
    return wrap(obj());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject declaring_class = jni()->NewStringUTF(declaring_class_str);
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    jobject method_name = jni()->NewStringUTF(method_name_sym->as_C_string());
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    jobject file_name = NULL;
    if (file_name_sym != NULL) {
      file_name = jni()->NewStringUTF(file_name_sym->as_C_string());
      if (jni()->ExceptionCheck()) {
        return JVMCIObject();
      }
    }

    jobject result = jni()->NewObject(JNIJVMCI::StackTraceElement::clazz(),
                                      JNIJVMCI::StackTraceElement::constructor(),
                                      declaring_class, method_name, file_name, line_number);
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::new_HotSpotNmethod(const methodHandle& method, const char* name, jboolean isDefault, jlong compileId, JVMCI_TRAPS) {
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.

  JVMCIObject methodObject = get_jvmci_method(method, JVMCI_CHECK_(JVMCIObject()));

  if (is_hotspot()) {
    InstanceKlass* ik = InstanceKlass::cast(HotSpotJVMCI::HotSpotNmethod::klass());
    if (ik->should_be_initialized()) {
      ik->initialize(CHECK_(JVMCIObject()));
    }
    oop obj = ik->allocate_instance(CHECK_(JVMCIObject()));
    Handle obj_h(THREAD, obj);
    Handle nameStr = java_lang_String::create_from_str(name, CHECK_(JVMCIObject()));

    // Call constructor
    JavaCallArguments jargs;
    jargs.push_oop(obj_h);
    jargs.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(methodObject)));
    jargs.push_oop(nameStr);
    jargs.push_int(isDefault);
    jargs.push_long(compileId);
    JavaValue result(T_VOID);
    JavaCalls::call_special(&result, ik,
                            vmSymbols::object_initializer_name(),
                            vmSymbols::method_string_bool_long_signature(),
                            &jargs, CHECK_(JVMCIObject()));
    return wrap(obj_h());
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject nameStr = name == NULL ? NULL : jni()->NewStringUTF(name);
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }

    jobject result = jni()->NewObject(JNIJVMCI::HotSpotNmethod::clazz(),
                                      JNIJVMCI::HotSpotNmethod::constructor(),
                                      methodObject.as_jobject(), nameStr, isDefault);
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::make_local(JVMCIObject object) {
  if (object.is_null()) {
    return JVMCIObject();
  }
  if (is_hotspot()) {
    return wrap(JNIHandles::make_local(HotSpotJVMCI::resolve(object)));
  } else {
    JNIAccessMark jni(this);
    return wrap(jni()->NewLocalRef(object.as_jobject()));
  }
}

JVMCIObject JVMCIEnv::make_global(JVMCIObject object) {
  if (object.is_null()) {
    return JVMCIObject();
  }
  if (is_hotspot()) {
    return wrap(JNIHandles::make_global(Handle(Thread::current(), HotSpotJVMCI::resolve(object))));
  } else {
    JNIAccessMark jni(this);
    return wrap(jni()->NewGlobalRef(object.as_jobject()));
  }
}

void JVMCIEnv::destroy_local(JVMCIObject object) {
  if (is_hotspot()) {
    JNIHandles::destroy_local(object.as_jobject());
  } else {
    JNIAccessMark jni(this);
    jni()->DeleteLocalRef(object.as_jobject());
  }
}

void JVMCIEnv::destroy_global(JVMCIObject object) {
  if (is_hotspot()) {
    JNIHandles::destroy_global(object.as_jobject());
  } else {
    JNIAccessMark jni(this);
    jni()->DeleteGlobalRef(object.as_jobject());
  }
}

const char* JVMCIEnv::klass_name(JVMCIObject object) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(object)->klass()->signature_name();
  } else {
    JVMCIObject name;
    {
      JNIAccessMark jni(this);
      jclass jcl = jni()->GetObjectClass(object.as_jobject());
      jobject result = jni()->CallObjectMethod(jcl, JNIJVMCI::Class_getName_method());
      name = JVMCIObject::create(result, is_hotspot());
    }
    return as_utf8_string(name);
  }
}

JVMCIObject JVMCIEnv::get_jvmci_method(const methodHandle& method, JVMCI_TRAPS) {
  JVMCIObject method_object;
  if (method() == NULL) {
    return method_object;
  }

  CompilerOracle::tag_blackhole_if_possible(method);

  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  jmetadata handle = _runtime->allocate_handle(method);
  jboolean exception = false;
  if (is_hotspot()) {
    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_long((jlong) handle);
    JavaCalls::call_static(&result, HotSpotJVMCI::HotSpotResolvedJavaMethodImpl::klass(),
                           vmSymbols::fromMetaspace_name(),
                           vmSymbols::method_fromMetaspace_signature(), &args, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      exception = true;
    } else {
      method_object = wrap(result.get_oop());
    }
  } else {
    JNIAccessMark jni(this, THREAD);
    method_object = JNIJVMCI::wrap(jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotResolvedJavaMethodImpl::clazz(),
                                                                  JNIJVMCI::HotSpotResolvedJavaMethodImpl_fromMetaspace_method(),
                                                                  (jlong) handle));
    exception = jni()->ExceptionCheck();
  }

  if (exception) {
    _runtime->release_handle(handle);
    return JVMCIObject();
  }

  assert(asMethod(method_object) == method(), "must be");
  if (get_HotSpotResolvedJavaMethodImpl_metadataHandle(method_object) != (jlong) handle) {
    _runtime->release_handle(handle);
  }
  assert(!method_object.is_null(), "must be");
  return method_object;
}

JVMCIObject JVMCIEnv::get_jvmci_type(const JVMCIKlassHandle& klass, JVMCI_TRAPS) {
  JVMCIObject type;
  if (klass.is_null()) {
    return type;
  }

  jlong pointer = (jlong) klass();
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  JVMCIObject signature = create_string(klass->signature_name(), JVMCI_CHECK_(JVMCIObject()));
  jboolean exception = false;
  if (is_hotspot()) {
    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_long(pointer);
    args.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(signature)));
    JavaCalls::call_static(&result,
                           HotSpotJVMCI::HotSpotResolvedObjectTypeImpl::klass(),
                           vmSymbols::fromMetaspace_name(),
                           vmSymbols::klass_fromMetaspace_signature(), &args, THREAD);

    if (HAS_PENDING_EXCEPTION) {
      exception = true;
    } else {
      type = wrap(result.get_oop());
    }
  } else {
    JNIAccessMark jni(this, THREAD);

    HandleMark hm(THREAD);
    type = JNIJVMCI::wrap(jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotResolvedObjectTypeImpl::clazz(),
                                                        JNIJVMCI::HotSpotResolvedObjectTypeImpl_fromMetaspace_method(),
                                                        pointer, signature.as_jstring()));
    exception = jni()->ExceptionCheck();
  }
  if (exception) {
    return JVMCIObject();
  }

  assert(type.is_non_null(), "must have result");
  return type;
}

JVMCIObject JVMCIEnv::get_jvmci_constant_pool(const constantPoolHandle& cp, JVMCI_TRAPS) {
  JVMCIObject cp_object;
  jmetadata handle = _runtime->allocate_handle(cp);
  jboolean exception = false;
  JavaThread* THREAD = JVMCI::compilation_tick(JavaThread::current()); // For exception macros.
  if (is_hotspot()) {
    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_long((jlong) handle);
    JavaCalls::call_static(&result,
                           HotSpotJVMCI::HotSpotConstantPool::klass(),
                           vmSymbols::fromMetaspace_name(),
                           vmSymbols::constantPool_fromMetaspace_signature(), &args, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      exception = true;
    } else {
      cp_object = wrap(result.get_oop());
    }
  } else {
    JNIAccessMark jni(this, THREAD);
    cp_object = JNIJVMCI::wrap(jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotConstantPool::clazz(),
                                                             JNIJVMCI::HotSpotConstantPool_fromMetaspace_method(),
                                                             (jlong) handle));
    exception = jni()->ExceptionCheck();
  }

  if (exception) {
    _runtime->release_handle(handle);
    return JVMCIObject();
  }

  assert(!cp_object.is_null(), "must be");
  // Constant pools aren't cached so this is always a newly created object using the handle
  assert(get_HotSpotConstantPool_metadataHandle(cp_object) == (jlong) handle, "must use same handle");
  return cp_object;
}

JVMCIPrimitiveArray JVMCIEnv::new_booleanArray(int length, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    typeArrayOop result = oopFactory::new_boolArray(length, CHECK_(JVMCIObject()));
    return wrap(result);
  } else {
    JNIAccessMark jni(this, THREAD);
    jbooleanArray result = jni()->NewBooleanArray(length);
    return wrap(result);
  }
}

JVMCIPrimitiveArray JVMCIEnv::new_byteArray(int length, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    typeArrayOop result = oopFactory::new_byteArray(length, CHECK_(JVMCIObject()));
    return wrap(result);
  } else {
    JNIAccessMark jni(this, THREAD);
    jbyteArray result = jni()->NewByteArray(length);
    return wrap(result);
  }
}

JVMCIObjectArray JVMCIEnv::new_byte_array_array(int length, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    Klass* byteArrayArrayKlass = TypeArrayKlass::cast(Universe::byteArrayKlassObj  ())->array_klass(CHECK_(JVMCIObject()));
    objArrayOop result = ObjArrayKlass::cast(byteArrayArrayKlass) ->allocate(length, CHECK_(JVMCIObject()));
    return wrap(result);
  } else {
    JNIAccessMark jni(this, THREAD);
    jobjectArray result = jni()->NewObjectArray(length, JNIJVMCI::byte_array(), NULL);
    return wrap(result);
  }
}

JVMCIPrimitiveArray JVMCIEnv::new_intArray(int length, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    typeArrayOop result = oopFactory::new_intArray(length, CHECK_(JVMCIObject()));
    return wrap(result);
  } else {
    JNIAccessMark jni(this, THREAD);
    jintArray result = jni()->NewIntArray(length);
    return wrap(result);
  }
}

JVMCIPrimitiveArray JVMCIEnv::new_longArray(int length, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    typeArrayOop result = oopFactory::new_longArray(length, CHECK_(JVMCIObject()));
    return wrap(result);
  } else {
    JNIAccessMark jni(this, THREAD);
    jlongArray result = jni()->NewLongArray(length);
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::new_VMField(JVMCIObject name, JVMCIObject type, jlong offset, jlong address, JVMCIObject value, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    HotSpotJVMCI::VMField::klass()->initialize(CHECK_(JVMCIObject()));
    oop obj = HotSpotJVMCI::VMField::klass()->allocate_instance(CHECK_(JVMCIObject()));
    HotSpotJVMCI::VMField::set_name(this, obj, HotSpotJVMCI::resolve(name));
    HotSpotJVMCI::VMField::set_type(this, obj, HotSpotJVMCI::resolve(type));
    HotSpotJVMCI::VMField::set_offset(this, obj, offset);
    HotSpotJVMCI::VMField::set_address(this, obj, address);
    HotSpotJVMCI::VMField::set_value(this, obj, HotSpotJVMCI::resolve(value));
    return wrap(obj);
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->NewObject(JNIJVMCI::VMField::clazz(),
                                    JNIJVMCI::VMField::constructor(),
                                    get_jobject(name), get_jobject(type), offset, address, get_jobject(value));
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::new_VMFlag(JVMCIObject name, JVMCIObject type, JVMCIObject value, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    HotSpotJVMCI::VMFlag::klass()->initialize(CHECK_(JVMCIObject()));
    oop obj = HotSpotJVMCI::VMFlag::klass()->allocate_instance(CHECK_(JVMCIObject()));
    HotSpotJVMCI::VMFlag::set_name(this, obj, HotSpotJVMCI::resolve(name));
    HotSpotJVMCI::VMFlag::set_type(this, obj, HotSpotJVMCI::resolve(type));
    HotSpotJVMCI::VMFlag::set_value(this, obj, HotSpotJVMCI::resolve(value));
    return wrap(obj);
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->NewObject(JNIJVMCI::VMFlag::clazz(),
                                    JNIJVMCI::VMFlag::constructor(),
                                    get_jobject(name), get_jobject(type), get_jobject(value));
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::new_VMIntrinsicMethod(JVMCIObject declaringClass, JVMCIObject name, JVMCIObject descriptor, int id, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    HotSpotJVMCI::VMIntrinsicMethod::klass()->initialize(CHECK_(JVMCIObject()));
    oop obj = HotSpotJVMCI::VMIntrinsicMethod::klass()->allocate_instance(CHECK_(JVMCIObject()));
    HotSpotJVMCI::VMIntrinsicMethod::set_declaringClass(this, obj, HotSpotJVMCI::resolve(declaringClass));
    HotSpotJVMCI::VMIntrinsicMethod::set_name(this, obj, HotSpotJVMCI::resolve(name));
    HotSpotJVMCI::VMIntrinsicMethod::set_descriptor(this, obj, HotSpotJVMCI::resolve(descriptor));
    HotSpotJVMCI::VMIntrinsicMethod::set_id(this, obj, id);
    return wrap(obj);
  } else {
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->NewObject(JNIJVMCI::VMIntrinsicMethod::clazz(),
                                    JNIJVMCI::VMIntrinsicMethod::constructor(),
                                    get_jobject(declaringClass), get_jobject(name), get_jobject(descriptor), id);
    return wrap(result);
  }
}

JVMCIObject JVMCIEnv::new_HotSpotStackFrameReference(JVMCI_TRAPS) {
  if (is_hotspot()) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    HotSpotJVMCI::HotSpotStackFrameReference::klass()->initialize(CHECK_(JVMCIObject()));
    oop obj = HotSpotJVMCI::HotSpotStackFrameReference::klass()->allocate_instance(CHECK_(JVMCIObject()));
    return wrap(obj);
  } else {
    ShouldNotReachHere();
    return JVMCIObject();
  }
}
JVMCIObject JVMCIEnv::new_JVMCIError(JVMCI_TRAPS) {
  if (is_hotspot()) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    HotSpotJVMCI::JVMCIError::klass()->initialize(CHECK_(JVMCIObject()));
    oop obj = HotSpotJVMCI::JVMCIError::klass()->allocate_instance(CHECK_(JVMCIObject()));
    return wrap(obj);
  } else {
    ShouldNotReachHere();
    return JVMCIObject();
  }
}


JVMCIObject JVMCIEnv::get_object_constant(oop objOop, bool compressed, bool dont_register) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  Handle obj = Handle(THREAD, objOop);
  if (obj.is_null()) {
    return JVMCIObject();
  }
  if (is_hotspot()) {
    HotSpotJVMCI::DirectHotSpotObjectConstantImpl::klass()->initialize(CHECK_(JVMCIObject()));
    oop constant = HotSpotJVMCI::DirectHotSpotObjectConstantImpl::klass()->allocate_instance(CHECK_(JVMCIObject()));
    HotSpotJVMCI::DirectHotSpotObjectConstantImpl::set_object(this, constant, obj());
    HotSpotJVMCI::HotSpotObjectConstantImpl::set_compressed(this, constant, compressed);
    return wrap(constant);
  } else {
    jlong handle = make_handle(obj);
    JNIAccessMark jni(this, THREAD);
    jobject result = jni()->NewObject(JNIJVMCI::IndirectHotSpotObjectConstantImpl::clazz(),
                                      JNIJVMCI::IndirectHotSpotObjectConstantImpl::constructor(),
                                      handle, compressed, dont_register);
    return wrap(result);
  }
}


Handle JVMCIEnv::asConstant(JVMCIObject constant, JVMCI_TRAPS) {
  if (constant.is_null()) {
    return Handle();
  }
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    assert(HotSpotJVMCI::DirectHotSpotObjectConstantImpl::is_instance(this, constant), "wrong type");
    oop obj = HotSpotJVMCI::DirectHotSpotObjectConstantImpl::object(this, HotSpotJVMCI::resolve(constant));
    return Handle(THREAD, obj);
  } else if (isa_IndirectHotSpotObjectConstantImpl(constant)) {
    jlong object_handle = get_IndirectHotSpotObjectConstantImpl_objectHandle(constant);
    if (object_handle == 0L) {
      JVMCI_THROW_MSG_(NullPointerException, "Foreign object reference has been cleared", Handle());
    }
    oop result = resolve_handle(object_handle);
    if (result == NULL) {
      JVMCI_THROW_MSG_(InternalError, "Constant was unexpectedly NULL", Handle());
    }
    return Handle(THREAD, result);
  } else {
    JVMCI_THROW_MSG_(IllegalArgumentException, "DirectHotSpotObjectConstantImpl shouldn't reach JVMCI in SVM mode", Handle());
  }
}

JVMCIObject JVMCIEnv::wrap(jobject object) {
  return JVMCIObject::create(object, is_hotspot());
}

jlong JVMCIEnv::make_handle(const Handle& obj) {
  assert(!obj.is_null(), "should only create handle for non-NULL oops");
  jobject handle = _runtime->make_global(obj);
  return (jlong) handle;
}

oop JVMCIEnv::resolve_handle(jlong objectHandle) {
  assert(objectHandle != 0, "should be a valid handle");
  oop obj = *((oopDesc**)objectHandle);
  if (obj != NULL) {
    oopDesc::verify(obj);
  }
  return obj;
}

JVMCIObject JVMCIEnv::create_string(const char* str, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  if (is_hotspot()) {
    Handle result = java_lang_String::create_from_str(str, CHECK_(JVMCIObject()));
    return HotSpotJVMCI::wrap(result());
  } else {
    jobject result;
    jboolean exception = false;
    {
      JNIAccessMark jni(this, THREAD);
      result = jni()->NewStringUTF(str);
      exception = jni()->ExceptionCheck();
    }
    return wrap(result);
  }
}

bool JVMCIEnv::equals(JVMCIObject a, JVMCIObject b) {
  if (is_hotspot()) {
    return HotSpotJVMCI::resolve(a) == HotSpotJVMCI::resolve(b);
  } else {
    JNIAccessMark jni(this);
    return jni()->IsSameObject(a.as_jobject(), b.as_jobject()) != 0;
  }
}

BasicType JVMCIEnv::kindToBasicType(JVMCIObject kind, JVMCI_TRAPS) {
  if (kind.is_null()) {
    JVMCI_THROW_(NullPointerException, T_ILLEGAL);
  }
  jchar ch = get_JavaKind_typeChar(kind);
  switch(ch) {
    case 'Z': return T_BOOLEAN;
    case 'B': return T_BYTE;
    case 'S': return T_SHORT;
    case 'C': return T_CHAR;
    case 'I': return T_INT;
    case 'F': return T_FLOAT;
    case 'J': return T_LONG;
    case 'D': return T_DOUBLE;
    case 'A': return T_OBJECT;
    case '-': return T_ILLEGAL;
    default:
      JVMCI_ERROR_(T_ILLEGAL, "unexpected Kind: %c", ch);
  }
}

void JVMCIEnv::initialize_installed_code(JVMCIObject installed_code, CodeBlob* cb, JVMCI_TRAPS) {
  // Ensure that all updates to the InstalledCode fields are consistent.
  if (get_InstalledCode_address(installed_code) != 0) {
    JVMCI_THROW_MSG(InternalError, "InstalledCode instance already in use");
  }
  if (!isa_HotSpotInstalledCode(installed_code)) {
    JVMCI_THROW_MSG(InternalError, "InstalledCode instance must be a subclass of HotSpotInstalledCode");
  }

  // Ignore the version which can stay at 0
  if (cb->is_nmethod()) {
    nmethod* nm = cb->as_nmethod_or_null();
    if (!nm->is_alive()) {
      JVMCI_THROW_MSG(InternalError, "nmethod has been reclaimed");
    }
    if (nm->is_in_use()) {
      set_InstalledCode_entryPoint(installed_code, (jlong) nm->verified_entry_point());
    }
  } else {
    set_InstalledCode_entryPoint(installed_code, (jlong) cb->code_begin());
  }
  set_InstalledCode_address(installed_code, (jlong) cb);
  set_HotSpotInstalledCode_size(installed_code, cb->size());
  set_HotSpotInstalledCode_codeStart(installed_code, (jlong) cb->code_begin());
  set_HotSpotInstalledCode_codeSize(installed_code, cb->code_size());
}


void JVMCIEnv::invalidate_nmethod_mirror(JVMCIObject mirror, JVMCI_TRAPS) {
  if (mirror.is_null()) {
    JVMCI_THROW(NullPointerException);
  }

  nmethodLocker locker;
  nmethod* nm = JVMCIENV->get_nmethod(mirror, locker);
  if (nm == NULL) {
    // Nothing to do
    return;
  }

  Thread* current = Thread::current();
  if (!mirror.is_hotspot() && !current->is_Java_thread()) {
    // Calling back into native might cause the execution to block, so only allow this when calling
    // from a JavaThread, which is the normal case anyway.
    JVMCI_THROW_MSG(IllegalArgumentException,
                    "Cannot invalidate HotSpotNmethod object in shared library VM heap from non-JavaThread");
  }

  nmethodLocker nml(nm);
  if (nm->is_alive()) {
    // Invalidating the HotSpotNmethod means we want the nmethod to be deoptimized.
    Deoptimization::deoptimize_all_marked(nm);
  }

  // A HotSpotNmethod instance can only reference a single nmethod
  // during its lifetime so simply clear it here.
  set_InstalledCode_address(mirror, 0);
}

Klass* JVMCIEnv::asKlass(JVMCIObject obj) {
  return (Klass*) get_HotSpotResolvedObjectTypeImpl_metadataPointer(obj);
}

Method* JVMCIEnv::asMethod(JVMCIObject obj) {
  Method** metadataHandle = (Method**) get_HotSpotResolvedJavaMethodImpl_metadataHandle(obj);
  return *metadataHandle;
}

ConstantPool* JVMCIEnv::asConstantPool(JVMCIObject obj) {
  ConstantPool** metadataHandle = (ConstantPool**) get_HotSpotConstantPool_metadataHandle(obj);
  return *metadataHandle;
}

CodeBlob* JVMCIEnv::get_code_blob(JVMCIObject obj, nmethodLocker& locker) {
  address code = (address) get_InstalledCode_address(obj);
  if (code == NULL) {
    return NULL;
  }
  if (isa_HotSpotNmethod(obj)) {
    nmethod* nm = NULL;
    {
      // Lookup the CodeBlob while holding the CodeCache_lock to ensure the nmethod can't be freed
      // by nmethod::flush while we're interrogating it.
      MutexLocker cm_lock(CodeCache_lock, Mutex::_no_safepoint_check_flag);
      CodeBlob* cb = CodeCache::find_blob_unsafe(code);
      if (cb == (CodeBlob*) code) {
        nmethod* the_nm = cb->as_nmethod_or_null();
        if (the_nm != NULL && the_nm->is_alive()) {
          // Lock the nmethod to stop any further transitions by the sweeper.  It's still possible
          // for this code to execute in the middle of the sweeping of the nmethod but that will be
          // handled below.
          locker.set_code(nm, true);
          nm = the_nm;
        }
      }
    }

    if (nm != NULL) {
      // We found the nmethod but it could be in the process of being freed.  Check the state of the
      // nmethod while holding the CompiledMethod_lock.  This ensures that any transitions by other
      // threads have seen the is_locked_by_vm() update above.
      MutexLocker cm_lock(CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
      if (!nm->is_alive()) {
        //  It was alive when we looked it up but it's no longer alive so release it.
        locker.set_code(NULL);
        nm = NULL;
      }
    }

    jlong compile_id_snapshot = get_HotSpotNmethod_compileIdSnapshot(obj);
    if (compile_id_snapshot != 0L) {
      // Found a live nmethod with the same address, make sure it's the same nmethod
      if (nm == (nmethod*) code && nm->compile_id() == compile_id_snapshot && nm->is_alive()) {
        if (nm->is_not_entrant()) {
          // Zero the entry point so that the nmethod
          // cannot be invoked by the mirror but can
          // still be deoptimized.
          set_InstalledCode_entryPoint(obj, 0);
        }
        return nm;
      }
      // The HotSpotNmethod no longer refers to a valid nmethod so clear the state
      locker.set_code(NULL);
      nm = NULL;
    }

    if (nm == NULL) {
      // The HotSpotNmethod was pointing at some nmethod but the nmethod is no longer valid, so
      // clear the InstalledCode fields of this HotSpotNmethod so that it no longer refers to a
      // nmethod in the code cache.
      set_InstalledCode_address(obj, 0);
      set_InstalledCode_entryPoint(obj, 0);
    }
    return nm;
  }

  CodeBlob* cb = (CodeBlob*) code;
  assert(!cb->is_nmethod(), "unexpected nmethod");
  return cb;
}

nmethod* JVMCIEnv::get_nmethod(JVMCIObject obj, nmethodLocker& locker) {
  CodeBlob* cb = get_code_blob(obj, locker);
  if (cb != NULL) {
    return cb->as_nmethod_or_null();
  }
  return NULL;
}

// Generate implementations for the initialize, new, isa, get and set methods for all the types and
// fields declared in the JVMCI_CLASSES_DO macro.

#define START_CLASS(className, fullClassName)                                                                        \
  void JVMCIEnv::className##_initialize(JVMCI_TRAPS) {                                                               \
    if (is_hotspot()) {                                                                                              \
      HotSpotJVMCI::className::initialize(JVMCI_CHECK);                                                              \
    } else {                                                                                                         \
      JNIJVMCI::className::initialize(JVMCI_CHECK);                                                                  \
    }                                                                                                                \
  }                                                                                                                  \
  JVMCIObjectArray JVMCIEnv::new_##className##_array(int length, JVMCI_TRAPS) {                                      \
    if (is_hotspot()) {                                                                                              \
      JavaThread* THREAD = JavaThread::current(); /* For exception macros. */ \
      objArrayOop array = oopFactory::new_objArray(HotSpotJVMCI::className::klass(), length, CHECK_(JVMCIObject())); \
      return (JVMCIObjectArray) wrap(array);                                                                         \
    } else {                                                                                                         \
      JNIAccessMark jni(this);                                                                                       \
      jobjectArray result = jni()->NewObjectArray(length, JNIJVMCI::className::clazz(), NULL);                       \
      return wrap(result);                                                                                           \
    }                                                                                                                \
  }                                                                                                                  \
  bool JVMCIEnv::isa_##className(JVMCIObject object) {                                                               \
    if (is_hotspot()) {                                                                                              \
      return HotSpotJVMCI::className::is_instance(this, object);                                                     \
    } else {                                                                                                         \
      return JNIJVMCI::className::is_instance(this, object);                                                         \
    }                                                                                                                \
  }

#define END_CLASS

#define FIELD(className, name, type, accessor, cast)                 \
  type JVMCIEnv::get_##className##_##name(JVMCIObject obj) {         \
    if (is_hotspot()) {                                              \
      return HotSpotJVMCI::className::get_##name(this, obj);         \
    } else {                                                         \
      return JNIJVMCI::className::get_##name(this, obj);             \
    }                                                                \
  }                                                                  \
  void JVMCIEnv::set_##className##_##name(JVMCIObject obj, type x) { \
    if (is_hotspot()) {                                              \
      HotSpotJVMCI::className::set_##name(this, obj, x);             \
    } else {                                                         \
      JNIJVMCI::className::set_##name(this, obj, x);                 \
    }                                                                \
  }

#define EMPTY_CAST
#define CHAR_FIELD(className, name)                    FIELD(className, name, jchar, Char, EMPTY_CAST)
#define INT_FIELD(className, name)                     FIELD(className, name, jint, Int, EMPTY_CAST)
#define BOOLEAN_FIELD(className, name)                 FIELD(className, name, jboolean, Boolean, EMPTY_CAST)
#define LONG_FIELD(className, name)                    FIELD(className, name, jlong, Long, EMPTY_CAST)
#define FLOAT_FIELD(className, name)                   FIELD(className, name, jfloat, Float, EMPTY_CAST)

#define OBJECT_FIELD(className, name, signature)              OOPISH_FIELD(className, name, JVMCIObject, Object, EMPTY_CAST)
#define OBJECTARRAY_FIELD(className, name, signature)         OOPISH_FIELD(className, name, JVMCIObjectArray, Object, (JVMCIObjectArray))
#define PRIMARRAY_FIELD(className, name, signature)           OOPISH_FIELD(className, name, JVMCIPrimitiveArray, Object, (JVMCIPrimitiveArray))

#define STATIC_OBJECT_FIELD(className, name, signature)       STATIC_OOPISH_FIELD(className, name, JVMCIObject, Object, (JVMCIObject))
#define STATIC_OBJECTARRAY_FIELD(className, name, signature)  STATIC_OOPISH_FIELD(className, name, JVMCIObjectArray, Object, (JVMCIObjectArray))

#define OOPISH_FIELD(className, name, type, accessor, cast)           \
  type JVMCIEnv::get_##className##_##name(JVMCIObject obj) {          \
    if (is_hotspot()) {                                               \
      return HotSpotJVMCI::className::get_##name(this, obj);          \
    } else {                                                          \
      return JNIJVMCI::className::get_##name(this, obj);              \
    }                                                                 \
  }                                                                   \
  void JVMCIEnv::set_##className##_##name(JVMCIObject obj, type x) {  \
    if (is_hotspot()) {                                               \
      HotSpotJVMCI::className::set_##name(this, obj, x);              \
    } else {                                                          \
      JNIJVMCI::className::set_##name(this, obj, x);                  \
    }                                                                 \
  }

#define STATIC_OOPISH_FIELD(className, name, type, accessor, cast)    \
  type JVMCIEnv::get_##className##_##name() {                         \
    if (is_hotspot()) {                                               \
      return HotSpotJVMCI::className::get_##name(this);               \
    } else {                                                          \
      return JNIJVMCI::className::get_##name(this);                   \
    }                                                                 \
  }                                                                   \
  void JVMCIEnv::set_##className##_##name(type x) {                   \
    if (is_hotspot()) {                                               \
      HotSpotJVMCI::className::set_##name(this, x);                   \
    } else {                                                          \
      JNIJVMCI::className::set_##name(this, x);                       \
    }                                                                 \
  }

#define STATIC_PRIMITIVE_FIELD(className, name, type, accessor, cast) \
  type JVMCIEnv::get_##className##_##name() {                         \
    if (is_hotspot()) {                                               \
      return HotSpotJVMCI::className::get_##name(this);               \
    } else {                                                          \
      return JNIJVMCI::className::get_##name(this);                   \
    }                                                                 \
  }                                                                   \
  void JVMCIEnv::set_##className##_##name(type x) {                   \
    if (is_hotspot()) {                                               \
      HotSpotJVMCI::className::set_##name(this, x);                   \
    } else {                                                          \
      JNIJVMCI::className::set_##name(this, x);                       \
    }                                                                 \
  }
#define STATIC_INT_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jint, Int, EMPTY_CAST)
#define STATIC_BOOLEAN_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jboolean, Boolean, EMPTY_CAST)
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)

JVMCI_CLASSES_DO(START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, PRIMARRAY_FIELD, OBJECTARRAY_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECTARRAY_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)

#undef START_CLASS
#undef END_CLASS
#undef METHOD
#undef CONSTRUCTOR
#undef FIELD
#undef CHAR_FIELD
#undef INT_FIELD
#undef BOOLEAN_FIELD
#undef LONG_FIELD
#undef FLOAT_FIELD
#undef OBJECT_FIELD
#undef PRIMARRAY_FIELD
#undef OBJECTARRAY_FIELD
#undef STATIC_OOPISH_FIELD
#undef STATIC_OBJECT_FIELD
#undef STATIC_OBJECTARRAY_FIELD
#undef STATIC_INT_FIELD
#undef STATIC_BOOLEAN_FIELD
#undef EMPTY_CAST
