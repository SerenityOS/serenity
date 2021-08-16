/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "classfile/modules.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/jni/jfrJavaCall.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceOop.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/semaphore.inline.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "utilities/growableArray.hpp"
#include "classfile/vmSymbols.hpp"

#ifdef ASSERT
void JfrJavaSupport::check_java_thread_in_vm(JavaThread* t) {
  assert(t != NULL, "invariant");
  assert(t->thread_state() == _thread_in_vm, "invariant");
}

void JfrJavaSupport::check_java_thread_in_native(JavaThread* t) {
  assert(t != NULL, "invariant");
  assert(t->thread_state() == _thread_in_native, "invariant");
}

static void check_new_unstarted_java_thread(JavaThread* t) {
  assert(t != NULL, "invariant");
  assert(t->thread_state() == _thread_new, "invariant");
}
#endif

/*
 *  Handles and references
 */
jobject JfrJavaSupport::local_jni_handle(const oop obj, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  return t->active_handles()->allocate_handle(obj);
}

jobject JfrJavaSupport::local_jni_handle(const jobject handle, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  const oop obj = JNIHandles::resolve(handle);
  return obj == NULL ? NULL : local_jni_handle(obj, t);
}

void JfrJavaSupport::destroy_local_jni_handle(jobject handle) {
  JNIHandles::destroy_local(handle);
}

jobject JfrJavaSupport::global_jni_handle(const oop obj, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  HandleMark hm(t);
  return JNIHandles::make_global(Handle(t, obj));
}

jobject JfrJavaSupport::global_jni_handle(const jobject handle, JavaThread* t) {
  const oop obj = JNIHandles::resolve(handle);
  return obj == NULL ? NULL : global_jni_handle(obj, t);
}

void JfrJavaSupport::destroy_global_jni_handle(jobject handle) {
  JNIHandles::destroy_global(handle);
}

jweak JfrJavaSupport::global_weak_jni_handle(const oop obj, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  HandleMark hm(t);
  return JNIHandles::make_weak_global(Handle(t, obj));
}

jweak JfrJavaSupport::global_weak_jni_handle(const jobject handle, JavaThread* t) {
  const oop obj = JNIHandles::resolve(handle);
  return obj == NULL ? NULL : global_weak_jni_handle(obj, t);
}

void JfrJavaSupport::destroy_global_weak_jni_handle(jweak handle) {
  JNIHandles::destroy_weak_global(handle);
}

oop JfrJavaSupport::resolve_non_null(jobject obj) {
  return JNIHandles::resolve_non_null(obj);
}

/*
 *  Method invocation
 */
void JfrJavaSupport::call_static(JfrJavaArguments* args, TRAPS) {
  JfrJavaCall::call_static(args, THREAD);
}

void JfrJavaSupport::call_special(JfrJavaArguments* args, TRAPS) {
  JfrJavaCall::call_special(args, THREAD);
}

void JfrJavaSupport::call_virtual(JfrJavaArguments* args, TRAPS) {
  JfrJavaCall::call_virtual(args, THREAD);
}

void JfrJavaSupport::notify_all(jobject object, TRAPS) {
  assert(object != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  HandleMark hm(THREAD);
  Handle h_obj(THREAD, resolve_non_null(object));
  assert(h_obj.not_null(), "invariant");
  ObjectSynchronizer::jni_enter(h_obj, THREAD);
  ObjectSynchronizer::notifyall(h_obj, THREAD);
  ObjectSynchronizer::jni_exit(h_obj(), THREAD);
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
}

/*
 *  Object construction
 */
static void object_construction(JfrJavaArguments* args, JavaValue* result, InstanceKlass* klass, TRAPS) {
  assert(args != NULL, "invariant");
  assert(result != NULL, "invariant");
  assert(klass != NULL, "invariant");
  assert(klass->is_initialized(), "invariant");

  HandleMark hm(THREAD);
  instanceOop obj = klass->allocate_instance(CHECK);
  instanceHandle h_obj(THREAD, obj);
  assert(h_obj.not_null(), "invariant");
  args->set_receiver(h_obj);
  result->set_type(T_VOID); // constructor result type
  JfrJavaSupport::call_special(args, CHECK);
  result->set_type(T_OBJECT); // set back to original result type
  result->set_oop(h_obj());
}

static void array_construction(JfrJavaArguments* args, JavaValue* result, InstanceKlass* klass, int array_length, TRAPS) {
  assert(args != NULL, "invariant");
  assert(result != NULL, "invariant");
  assert(klass != NULL, "invariant");
  assert(klass->is_initialized(), "invariant");

  Klass* const ak = klass->array_klass(THREAD);
  ObjArrayKlass::cast(ak)->initialize(THREAD);
  HandleMark hm(THREAD);
  objArrayOop arr = ObjArrayKlass::cast(ak)->allocate(array_length, CHECK);
  result->set_oop(arr);
}

static void create_object(JfrJavaArguments* args, JavaValue* result, TRAPS) {
  assert(args != NULL, "invariant");
  assert(result != NULL, "invariant");
  assert(result->get_type() == T_OBJECT, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));

  InstanceKlass* const klass = static_cast<InstanceKlass*>(args->klass());
  klass->initialize(CHECK);

  const int array_length = args->array_length();

  if (array_length >= 0) {
    array_construction(args, result, klass, array_length, CHECK);
  } else {
    object_construction(args, result, klass, THREAD);
  }
}

static void handle_result(JavaValue* result, bool global_ref, JavaThread* t) {
  assert(result != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(t));
  const oop result_oop = result->get_oop();
  if (result_oop == NULL) {
    return;
  }
  result->set_jobject(global_ref ?
                      JfrJavaSupport::global_jni_handle(result_oop, t) :
                      JfrJavaSupport::local_jni_handle(result_oop, t));
}

void JfrJavaSupport::new_object(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  create_object(args, args->result(), THREAD);
}

void JfrJavaSupport::new_object_local_ref(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue* const result = args->result();
  assert(result != NULL, "invariant");
  create_object(args, result, CHECK);
  handle_result(result, false, THREAD);
}

void JfrJavaSupport::new_object_global_ref(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue* const result = args->result();
  assert(result != NULL, "invariant");
  create_object(args, result, CHECK);
  handle_result(result, true, THREAD);
}

jstring JfrJavaSupport::new_string(const char* c_str, TRAPS) {
  assert(c_str != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  const oop result = java_lang_String::create_oop_from_str(c_str, THREAD);
  return (jstring)local_jni_handle(result, THREAD);
}

jobjectArray JfrJavaSupport::new_string_array(int length, TRAPS) {
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue result(T_OBJECT);
  JfrJavaArguments args(&result, "java/lang/String", "<init>", "()V", CHECK_NULL);
  args.set_array_length(length);
  new_object_local_ref(&args, THREAD);
  return (jobjectArray)args.result()->get_jobject();
}

jobject JfrJavaSupport::new_java_lang_Boolean(bool value, TRAPS) {
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue result(T_OBJECT);
  JfrJavaArguments args(&result, "java/lang/Boolean", "<init>", "(Z)V", CHECK_NULL);
  args.push_int(value ? (jint)JNI_TRUE : (jint)JNI_FALSE);
  new_object_local_ref(&args, THREAD);
  return args.result()->get_jobject();
}

jobject JfrJavaSupport::new_java_lang_Integer(jint value, TRAPS) {
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue result(T_OBJECT);
  JfrJavaArguments args(&result, "java/lang/Integer", "<init>", "(I)V", CHECK_NULL);
  args.push_int(value);
  new_object_local_ref(&args, THREAD);
  return args.result()->get_jobject();
}

jobject JfrJavaSupport::new_java_lang_Long(jlong value, TRAPS) {
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));
  JavaValue result(T_OBJECT);
  JfrJavaArguments args(&result, "java/lang/Long", "<init>", "(J)V", CHECK_NULL);
  args.push_long(value);
  new_object_local_ref(&args, THREAD);
  return args.result()->get_jobject();
}

void JfrJavaSupport::set_array_element(jobjectArray arr, jobject element, int index, JavaThread* t) {
  assert(arr != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(t));
  HandleMark hm(t);
  objArrayHandle a(t, (objArrayOop)resolve_non_null(arr));
  a->obj_at_put(index, resolve_non_null(element));
}

/*
 *  Field access
 */
static void write_int_field(const Handle& h_oop, fieldDescriptor* fd, jint value) {
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  h_oop->int_field_put(fd->offset(), value);
}

static void write_float_field(const Handle& h_oop, fieldDescriptor* fd, jfloat value) {
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  h_oop->float_field_put(fd->offset(), value);
}

static void write_double_field(const Handle& h_oop, fieldDescriptor* fd, jdouble value) {
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  h_oop->double_field_put(fd->offset(), value);
}

static void write_long_field(const Handle& h_oop, fieldDescriptor* fd, jlong value) {
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  h_oop->long_field_put(fd->offset(), value);
}

static void write_oop_field(const Handle& h_oop, fieldDescriptor* fd, const oop value) {
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  h_oop->obj_field_put(fd->offset(), value);
}

static void write_specialized_field(JfrJavaArguments* args, const Handle& h_oop, fieldDescriptor* fd, bool static_field) {
  assert(args != NULL, "invariant");
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  assert(fd->offset() > 0, "invariant");
  assert(args->length() >= 1, "invariant");

  // attempt must set a real value
  assert(args->param(1).get_type() != T_VOID, "invariant");

  switch(fd->field_type()) {
    case T_BOOLEAN:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
      write_int_field(h_oop, fd, args->param(1).get_jint());
      break;
    case T_FLOAT:
      write_float_field(h_oop, fd, args->param(1).get_jfloat());
      break;
    case T_DOUBLE:
      write_double_field(h_oop, fd, args->param(1).get_jdouble());
      break;
    case T_LONG:
      write_long_field(h_oop, fd, args->param(1).get_jlong());
      break;
    case T_OBJECT:
      write_oop_field(h_oop, fd, args->param(1).get_oop());
      break;
    case T_ADDRESS:
      write_oop_field(h_oop, fd, JfrJavaSupport::resolve_non_null(args->param(1).get_jobject()));
      break;
    default:
      ShouldNotReachHere();
  }
}

static void read_specialized_field(JavaValue* result, const Handle& h_oop, fieldDescriptor* fd) {
  assert(result != NULL, "invariant");
  assert(h_oop.not_null(), "invariant");
  assert(fd != NULL, "invariant");
  assert(fd->offset() > 0, "invariant");

  switch(fd->field_type()) {
    case T_BOOLEAN:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
      result->set_jint(h_oop->int_field(fd->offset()));
      break;
    case T_FLOAT:
      result->set_jfloat(h_oop->float_field(fd->offset()));
      break;
    case T_DOUBLE:
      result->set_jdouble(h_oop->double_field(fd->offset()));
      break;
    case T_LONG:
      result->set_jlong(h_oop->long_field(fd->offset()));
      break;
    case T_OBJECT:
      result->set_oop(h_oop->obj_field(fd->offset()));
      break;
    default:
      ShouldNotReachHere();
  }
}

static bool find_field(InstanceKlass* ik,
                       Symbol* name_symbol,
                       Symbol* signature_symbol,
                       fieldDescriptor* fd,
                       bool is_static = false,
                       bool allow_super = false) {
  if (allow_super || is_static) {
    return ik->find_field(name_symbol, signature_symbol, is_static, fd) != NULL;
  }
  return ik->find_local_field(name_symbol, signature_symbol, fd);
}

static void lookup_field(JfrJavaArguments* args, InstanceKlass* klass, fieldDescriptor* fd, bool static_field) {
  assert(args != NULL, "invariant");
  assert(klass != NULL, "invariant");
  assert(klass->is_initialized(), "invariant");
  assert(fd != NULL, "invariant");
  find_field(klass, args->name(), args->signature(), fd, static_field, true);
}

static void read_field(JfrJavaArguments* args, JavaValue* result, TRAPS) {
  assert(args != NULL, "invariant");
  assert(result != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));

  InstanceKlass* const klass = static_cast<InstanceKlass*>(args->klass());
  klass->initialize(CHECK);
  const bool static_field = !args->has_receiver();
  fieldDescriptor fd;
  lookup_field(args, klass, &fd, static_field);
  assert(fd.offset() > 0, "invariant");

  HandleMark hm(THREAD);
  Handle h_oop(static_field ? Handle(THREAD, klass->java_mirror()) : Handle(THREAD, args->receiver()));
  read_specialized_field(result, h_oop, &fd);
}

static void write_field(JfrJavaArguments* args, JavaValue* result, TRAPS) {
  assert(args != NULL, "invariant");
  assert(result != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));

  InstanceKlass* const klass = static_cast<InstanceKlass*>(args->klass());
  klass->initialize(CHECK);

  const bool static_field = !args->has_receiver();
  fieldDescriptor fd;
  lookup_field(args, klass, &fd, static_field);
  assert(fd.offset() > 0, "invariant");

  HandleMark hm(THREAD);
  Handle h_oop(static_field ? Handle(THREAD, klass->java_mirror()) : Handle(THREAD, args->receiver()));
  write_specialized_field(args, h_oop, &fd, static_field);
}

void JfrJavaSupport::set_field(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  write_field(args, args->result(), THREAD);
}

void JfrJavaSupport::get_field(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  read_field(args, args->result(), THREAD);
}

void JfrJavaSupport::get_field_local_ref(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));

  JavaValue* const result = args->result();
  assert(result != NULL, "invariant");
  assert(result->get_type() == T_OBJECT, "invariant");

  read_field(args, result, CHECK);
  const oop obj = result->get_oop();

  if (obj != NULL) {
    result->set_jobject(local_jni_handle(obj, THREAD));
  }
}

void JfrJavaSupport::get_field_global_ref(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(check_java_thread_in_vm(THREAD));

  JavaValue* const result = args->result();
  assert(result != NULL, "invariant");
  assert(result->get_type() == T_OBJECT, "invariant");
  read_field(args, result, CHECK);
  const oop obj = result->get_oop();
  if (obj != NULL) {
    result->set_jobject(global_jni_handle(obj, THREAD));
  }
}

/*
 *  Misc
 */
Klass* JfrJavaSupport::klass(const jobject handle) {
  const oop obj = resolve_non_null(handle);
  assert(obj != NULL, "invariant");
  return obj->klass();
}

static char* allocate_string(bool c_heap, int length, JavaThread* jt) {
  return c_heap ? NEW_C_HEAP_ARRAY(char, length, mtTracing) :
                  NEW_RESOURCE_ARRAY_IN_THREAD(jt, char, length);
}

const char* JfrJavaSupport::c_str(oop string, JavaThread* t, bool c_heap /* false */) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  char* str = NULL;
  const typeArrayOop value = java_lang_String::value(string);
  if (value != NULL) {
    const int length = java_lang_String::utf8_length(string, value);
    str = allocate_string(c_heap, length + 1, t);
    if (str == NULL) {
      JfrJavaSupport::throw_out_of_memory_error("Unable to allocate native memory", t);
      return NULL;
    }
    java_lang_String::as_utf8_string(string, value, str, length + 1);
  }
  return str;
}

const char* JfrJavaSupport::c_str(jstring string, JavaThread* t, bool c_heap /* false */) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  return string != NULL ? c_str(resolve_non_null(string), t, c_heap) : NULL;
}

/*
 *  Exceptions and errors
 */
static void create_and_throw(Symbol* name, const char* message, TRAPS) {
  assert(name != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  assert(!HAS_PENDING_EXCEPTION, "invariant");
  THROW_MSG(name, message);
}

void JfrJavaSupport::throw_illegal_state_exception(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_IllegalStateException(), message, THREAD);
}

void JfrJavaSupport::throw_internal_error(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_InternalError(), message, THREAD);
}

void JfrJavaSupport::throw_illegal_argument_exception(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_IllegalArgumentException(), message, THREAD);
}

void JfrJavaSupport::throw_out_of_memory_error(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_OutOfMemoryError(), message, THREAD);
}

void JfrJavaSupport::throw_class_format_error(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_ClassFormatError(), message, THREAD);
}

void JfrJavaSupport::throw_runtime_exception(const char* message, TRAPS) {
  create_and_throw(vmSymbols::java_lang_RuntimeException(), message, THREAD);
}

void JfrJavaSupport::abort(jstring errorMsg, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));

  ResourceMark rm(t);
  const char* const error_msg = c_str(errorMsg, t);
  if (error_msg != NULL) {
    log_error(jfr, system)("%s",error_msg);
  }
  log_error(jfr, system)("%s", "An irrecoverable error in Jfr. Shutting down VM...");
  vm_abort();
}

JfrJavaSupport::CAUSE JfrJavaSupport::_cause = JfrJavaSupport::VM_ERROR;
void JfrJavaSupport::set_cause(jthrowable throwable, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));

  HandleMark hm(t);
  Handle ex(t, JNIHandles::resolve_external_guard(throwable));

  if (ex.is_null()) {
    return;
  }

  if (ex->is_a(vmClasses::OutOfMemoryError_klass())) {
    _cause = OUT_OF_MEMORY;
    return;
  }
  if (ex->is_a(vmClasses::StackOverflowError_klass())) {
    _cause = STACK_OVERFLOW;
    return;
  }
  if (ex->is_a(vmClasses::Error_klass())) {
    _cause = VM_ERROR;
    return;
  }
  if (ex->is_a(vmClasses::RuntimeException_klass())) {
    _cause = RUNTIME_EXCEPTION;
    return;
  }
  if (ex->is_a(vmClasses::Exception_klass())) {
    _cause = UNKNOWN;
    return;
  }
}

void JfrJavaSupport::uncaught_exception(jthrowable throwable, JavaThread* t) {
  DEBUG_ONLY(check_java_thread_in_vm(t));
  assert(throwable != NULL, "invariant");
  set_cause(throwable, t);
}

JfrJavaSupport::CAUSE JfrJavaSupport::cause() {
  return _cause;
}

const char* const JDK_JFR_MODULE_NAME = "jdk.jfr";
const char* const JDK_JFR_PACKAGE_NAME = "jdk/jfr";

static bool is_jdk_jfr_module_in_readability_graph() {
  // take one of the packages in the module to be located and query for its definition.
  TempNewSymbol pkg_sym = SymbolTable::new_symbol(JDK_JFR_PACKAGE_NAME);
  return Modules::is_package_defined(pkg_sym, Handle());
}

static void print_module_resolution_error(outputStream* stream) {
  assert(stream != NULL, "invariant");
  stream->print_cr("Module %s not found.", JDK_JFR_MODULE_NAME);
  stream->print_cr("Flight Recorder can not be enabled.");
}

bool JfrJavaSupport::is_jdk_jfr_module_available() {
  return is_jdk_jfr_module_in_readability_graph();
}

bool JfrJavaSupport::is_jdk_jfr_module_available(outputStream* stream, TRAPS) {
  if (!JfrJavaSupport::is_jdk_jfr_module_available()) {
    if (stream != NULL) {
      print_module_resolution_error(stream);
    }
    return false;
  }
  return true;
}

class ThreadExclusionListAccess : public StackObj {
 private:
  static Semaphore _mutex_semaphore;
 public:
  ThreadExclusionListAccess() { _mutex_semaphore.wait(); }
  ~ThreadExclusionListAccess() { _mutex_semaphore.signal(); }
};

Semaphore ThreadExclusionListAccess::_mutex_semaphore(1);
static GrowableArray<jweak>* exclusion_list = NULL;

static bool equals(const jweak excluded_thread, Handle target_thread) {
  return JfrJavaSupport::resolve_non_null(excluded_thread) == target_thread();
}

static int find_exclusion_thread_idx(Handle thread) {
  if (exclusion_list != NULL) {
    for (int i = 0; i < exclusion_list->length(); ++i) {
      if (equals(exclusion_list->at(i), thread)) {
        return i;
      }
    }
  }
  return -1;
}

static Handle as_handle(jobject thread) {
  return Handle(Thread::current(), JfrJavaSupport::resolve_non_null(thread));
}

static bool thread_is_not_excluded(Handle thread) {
  return -1 == find_exclusion_thread_idx(thread);
}

static bool thread_is_not_excluded(jobject thread) {
  return thread_is_not_excluded(as_handle(thread));
}

static bool is_thread_excluded(jobject thread) {
  return !thread_is_not_excluded(thread);
}

#ifdef ASSERT
static bool is_thread_excluded(Handle thread) {
  return !thread_is_not_excluded(thread);
}
#endif // ASSERT

static int add_thread_to_exclusion_list(jobject thread) {
  ThreadExclusionListAccess lock;
  if (exclusion_list == NULL) {
    exclusion_list = new (ResourceObj::C_HEAP, mtTracing) GrowableArray<jweak>(10, mtTracing);
  }
  assert(exclusion_list != NULL, "invariant");
  assert(thread_is_not_excluded(thread), "invariant");
  jweak ref = JfrJavaSupport::global_weak_jni_handle(thread, JavaThread::current());
  const int idx = exclusion_list->append(ref);
  assert(is_thread_excluded(thread), "invariant");
  return idx;
}

static void remove_thread_from_exclusion_list(Handle thread) {
  assert(exclusion_list != NULL, "invariant");
  assert(is_thread_excluded(thread), "invariant");
  assert(exclusion_list != NULL, "invariant");
  const int idx = find_exclusion_thread_idx(thread);
  assert(idx >= 0, "invariant");
  assert(idx < exclusion_list->length(), "invariant");
  JfrJavaSupport::destroy_global_weak_jni_handle(exclusion_list->at(idx));
  exclusion_list->delete_at(idx);
  assert(thread_is_not_excluded(thread), "invariant");
  if (0 == exclusion_list->length()) {
    delete exclusion_list;
    exclusion_list = NULL;
  }
}

static void remove_thread_from_exclusion_list(jobject thread) {
  ThreadExclusionListAccess lock;
  remove_thread_from_exclusion_list(as_handle(thread));
}

// includes removal
static bool check_exclusion_state_on_thread_start(JavaThread* jt) {
  Handle h_obj(jt, jt->threadObj());
  ThreadExclusionListAccess lock;
  if (thread_is_not_excluded(h_obj)) {
    return false;
  }
  remove_thread_from_exclusion_list(h_obj);
  return true;
}

static JavaThread* get_native(jobject thread) {
  ThreadsListHandle tlh;
  JavaThread* native_thread = NULL;
  (void)tlh.cv_internal_thread_to_JavaThread(thread, &native_thread, NULL);
  return native_thread;
}

jlong JfrJavaSupport::jfr_thread_id(jobject thread) {
  JavaThread* native_thread = get_native(thread);
  return native_thread != NULL ? JFR_THREAD_ID(native_thread) : 0;
}

void JfrJavaSupport::exclude(jobject thread) {
  JavaThread* native_thread = get_native(thread);
  if (native_thread != NULL) {
    JfrThreadLocal::exclude(native_thread);
  } else {
    // not started yet, track the thread oop
    add_thread_to_exclusion_list(thread);
  }
}

void JfrJavaSupport::include(jobject thread) {
  JavaThread* native_thread = get_native(thread);
  if (native_thread != NULL) {
    JfrThreadLocal::include(native_thread);
  } else {
    // not started yet, untrack the thread oop
    remove_thread_from_exclusion_list(thread);
  }
}

bool JfrJavaSupport::is_excluded(jobject thread) {
  JavaThread* native_thread = get_native(thread);
  return native_thread != NULL ? native_thread->jfr_thread_local()->is_excluded() : is_thread_excluded(thread);
}

static const Klass* get_handler_field_descriptor(const Handle& h_mirror, fieldDescriptor* descriptor, TRAPS) {
  assert(h_mirror.not_null(), "invariant");
  assert(descriptor != NULL, "invariant");
  Klass* const k = java_lang_Class::as_Klass(h_mirror());
  assert(k->is_instance_klass(), "invariant");
  InstanceKlass* const ik = InstanceKlass::cast(k);
  if (ik->is_not_initialized()) {
    ik->initialize(CHECK_NULL);
  }
  assert(ik->is_being_initialized() || ik->is_initialized(), "invariant");
  const Klass* const typed_field_holder = ik->find_field(vmSymbols::eventHandler_name(),
                                                         vmSymbols::jdk_jfr_internal_handlers_EventHandler_signature(),
                                                         true,
                                                         descriptor);
  return typed_field_holder != NULL ? typed_field_holder : ik->find_field(vmSymbols::eventHandler_name(),
                                                                          vmSymbols::object_signature(), // untyped
                                                                          true,
                                                                          descriptor);
}

jobject JfrJavaSupport::get_handler(jobject clazz, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  HandleMark hm(THREAD);
  const Handle h_mirror(Handle(THREAD, JNIHandles::resolve(clazz)));
  assert(h_mirror.not_null(), "invariant");
  fieldDescriptor handler_field_descriptor;
  const Klass* const field_holder = get_handler_field_descriptor(h_mirror, &handler_field_descriptor, THREAD);
  if (field_holder == NULL) {
    // The only reason should be that klass initialization failed.
    return NULL;
  }
  assert(java_lang_Class::as_Klass(h_mirror()) == field_holder, "invariant");
  oop handler_oop = h_mirror->obj_field(handler_field_descriptor.offset());
  return handler_oop != NULL ? JfrJavaSupport::local_jni_handle(handler_oop, THREAD) : NULL;
}

bool JfrJavaSupport::set_handler(jobject clazz, jobject handler, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  HandleMark hm(THREAD);
  const Handle h_mirror(Handle(THREAD, JNIHandles::resolve(clazz)));
  assert(h_mirror.not_null(), "invariant");
  fieldDescriptor handler_field_descriptor;
  const Klass* const field_holder = get_handler_field_descriptor(h_mirror, &handler_field_descriptor, THREAD);
  if (field_holder == NULL) {
    // The only reason should be that klass initialization failed.
    return false;
  }
  assert(java_lang_Class::as_Klass(h_mirror()) == field_holder, "invariant");
  const oop handler_oop = JNIHandles::resolve(handler);
  assert(handler_oop != NULL, "invariant");
  h_mirror->obj_field_put(handler_field_descriptor.offset(), handler_oop);
  return true;
}

void JfrJavaSupport::on_thread_start(Thread* t) {
  assert(t != NULL, "invariant");
  assert(Thread::current() == t, "invariant");
  if (!t->is_Java_thread()) {
    return;
  }
  DEBUG_ONLY(check_new_unstarted_java_thread(JavaThread::cast(t));)
  HandleMark hm(t);
  if (check_exclusion_state_on_thread_start(JavaThread::cast(t))) {
    JfrThreadLocal::exclude(t);
  }
}
