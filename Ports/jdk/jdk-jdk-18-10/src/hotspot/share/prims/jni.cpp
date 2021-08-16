/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012 Red Hat, Inc.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
#include "jni.h"
#include "jvm.h"
#include "ci/ciReplay.hpp"
#include "classfile/altHashing.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/javaThreadStatus.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/modules.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compiler_globals.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcLocker.inline.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "interpreter/linkResolver.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/arrayOop.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/instanceOop.hpp"
#include "oops/klass.inline.hpp"
#include "oops/markWord.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "oops/typeArrayKlass.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jniCheck.hpp"
#include "prims/jniExport.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "runtime/atomic.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jfieldIDWorkaround.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/reflection.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "services/memTracker.hpp"
#include "services/runtimeService.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmError.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmciCompiler.hpp"
#endif

static jint CurrentVersion = JNI_VERSION_10;

#if defined(_WIN32) && !defined(USE_VECTORED_EXCEPTION_HANDLING)
extern LONG WINAPI topLevelExceptionFilter(_EXCEPTION_POINTERS* );
#endif

// The DT_RETURN_MARK macros create a scoped object to fire the dtrace
// '-return' probe regardless of the return path is taken out of the function.
// Methods that have multiple return paths use this to avoid having to
// instrument each return path.  Methods that use CHECK or THROW must use this
// since those macros can cause an immedate uninstrumented return.
//
// In order to get the return value, a reference to the variable containing
// the return value must be passed to the contructor of the object, and
// the return value must be set before return (since the mark object has
// a reference to it).
//
// Example:
// DT_RETURN_MARK_DECL(SomeFunc, int);
// JNI_ENTRY(int, SomeFunc, ...)
//   int return_value = 0;
//   DT_RETURN_MARK(SomeFunc, int, (const int&)return_value);
//   foo(CHECK_0)
//   return_value = 5;
//   return return_value;
// JNI_END
#define DT_RETURN_MARK_DECL(name, type, probe)                             \
  DTRACE_ONLY(                                                             \
    class DTraceReturnProbeMark_##name {                                   \
     public:                                                               \
      const type& _ret_ref;                                                \
      DTraceReturnProbeMark_##name(const type& v) : _ret_ref(v) {}         \
      ~DTraceReturnProbeMark_##name() {                                    \
        probe;                                                             \
      }                                                                    \
    }                                                                      \
  )
// Void functions are simpler since there's no return value
#define DT_VOID_RETURN_MARK_DECL(name, probe)                              \
  DTRACE_ONLY(                                                             \
    class DTraceReturnProbeMark_##name {                                   \
     public:                                                               \
      ~DTraceReturnProbeMark_##name() {                                    \
        probe;                                                             \
      }                                                                    \
    }                                                                      \
  )

// Place these macros in the function to mark the return.  Non-void
// functions need the type and address of the return value.
#define DT_RETURN_MARK(name, type, ref) \
  DTRACE_ONLY( DTraceReturnProbeMark_##name dtrace_return_mark(ref) )
#define DT_VOID_RETURN_MARK(name) \
  DTRACE_ONLY( DTraceReturnProbeMark_##name dtrace_return_mark )


// Use these to select distinct code for floating-point vs. non-floating point
// situations.  Used from within common macros where we need slightly
// different behavior for Float/Double
#define FP_SELECT_Boolean(intcode, fpcode) intcode
#define FP_SELECT_Byte(intcode, fpcode)    intcode
#define FP_SELECT_Char(intcode, fpcode)    intcode
#define FP_SELECT_Short(intcode, fpcode)   intcode
#define FP_SELECT_Object(intcode, fpcode)  intcode
#define FP_SELECT_Int(intcode, fpcode)     intcode
#define FP_SELECT_Long(intcode, fpcode)    intcode
#define FP_SELECT_Float(intcode, fpcode)   fpcode
#define FP_SELECT_Double(intcode, fpcode)  fpcode
#define FP_SELECT(TypeName, intcode, fpcode) \
  FP_SELECT_##TypeName(intcode, fpcode)

// Choose DT_RETURN_MARK macros  based on the type: float/double -> void
// (dtrace doesn't do FP yet)
#define DT_RETURN_MARK_DECL_FOR(TypeName, name, type, probe)    \
  FP_SELECT(TypeName, \
    DT_RETURN_MARK_DECL(name, type, probe), DT_VOID_RETURN_MARK_DECL(name, probe) )
#define DT_RETURN_MARK_FOR(TypeName, name, type, ref) \
  FP_SELECT(TypeName, \
    DT_RETURN_MARK(name, type, ref), DT_VOID_RETURN_MARK(name) )


// out-of-line helpers for class jfieldIDWorkaround:

bool jfieldIDWorkaround::is_valid_jfieldID(Klass* k, jfieldID id) {
  if (jfieldIDWorkaround::is_instance_jfieldID(k, id)) {
    uintptr_t as_uint = (uintptr_t) id;
    intptr_t offset = raw_instance_offset(id);
    if (is_checked_jfieldID(id)) {
      if (!klass_hash_ok(k, id)) {
        return false;
      }
    }
    return InstanceKlass::cast(k)->contains_field_offset(offset);
  } else {
    JNIid* result = (JNIid*) id;
#ifdef ASSERT
    return result != NULL && result->is_static_field_id();
#else
    return result != NULL;
#endif
  }
}


intptr_t jfieldIDWorkaround::encode_klass_hash(Klass* k, intptr_t offset) {
  if (offset <= small_offset_mask) {
    Klass* field_klass = k;
    Klass* super_klass = field_klass->super();
    // With compressed oops the most super class with nonstatic fields would
    // be the owner of fields embedded in the header.
    while (InstanceKlass::cast(super_klass)->has_nonstatic_fields() &&
           InstanceKlass::cast(super_klass)->contains_field_offset(offset)) {
      field_klass = super_klass;   // super contains the field also
      super_klass = field_klass->super();
    }
    debug_only(NoSafepointVerifier nosafepoint;)
    uintptr_t klass_hash = field_klass->identity_hash();
    return ((klass_hash & klass_mask) << klass_shift) | checked_mask_in_place;
  } else {
#if 0
    #ifndef PRODUCT
    {
      ResourceMark rm;
      warning("VerifyJNIFields: long offset %d in %s", offset, k->external_name());
    }
    #endif
#endif
    return 0;
  }
}

bool jfieldIDWorkaround::klass_hash_ok(Klass* k, jfieldID id) {
  uintptr_t as_uint = (uintptr_t) id;
  intptr_t klass_hash = (as_uint >> klass_shift) & klass_mask;
  do {
    debug_only(NoSafepointVerifier nosafepoint;)
    // Could use a non-blocking query for identity_hash here...
    if ((k->identity_hash() & klass_mask) == klass_hash)
      return true;
    k = k->super();
  } while (k != NULL);
  return false;
}

void jfieldIDWorkaround::verify_instance_jfieldID(Klass* k, jfieldID id) {
  guarantee(jfieldIDWorkaround::is_instance_jfieldID(k, id), "must be an instance field" );
  uintptr_t as_uint = (uintptr_t) id;
  intptr_t offset = raw_instance_offset(id);
  if (VerifyJNIFields) {
    if (is_checked_jfieldID(id)) {
      guarantee(klass_hash_ok(k, id),
    "Bug in native code: jfieldID class must match object");
    } else {
#if 0
      #ifndef PRODUCT
      if (Verbose) {
  ResourceMark rm;
  warning("VerifyJNIFields: unverified offset %d for %s", offset, k->external_name());
      }
      #endif
#endif
    }
  }
  guarantee(InstanceKlass::cast(k)->contains_field_offset(offset),
      "Bug in native code: jfieldID offset must address interior of object");
}

// Implementation of JNI entries

DT_RETURN_MARK_DECL(DefineClass, jclass
                    , HOTSPOT_JNI_DEFINECLASS_RETURN(_ret_ref));

JNI_ENTRY(jclass, jni_DefineClass(JNIEnv *env, const char *name, jobject loaderRef,
                                  const jbyte *buf, jsize bufLen))
  HOTSPOT_JNI_DEFINECLASS_ENTRY(
    env, (char*) name, loaderRef, (char*) buf, bufLen);

  jclass cls = NULL;
  DT_RETURN_MARK(DefineClass, jclass, (const jclass&)cls);

  // Class resolution will get the class name from the .class stream if the name is null.
  TempNewSymbol class_name = name == NULL ? NULL :
    SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_NoClassDefFoundError(),
                                        CHECK_NULL);

  ResourceMark rm(THREAD);
  ClassFileStream st((u1*)buf, bufLen, NULL, ClassFileStream::verify);
  Handle class_loader (THREAD, JNIHandles::resolve(loaderRef));
  Handle protection_domain;
  ClassLoadInfo cl_info(protection_domain);
  Klass* k = SystemDictionary::resolve_from_stream(&st, class_name,
                                                   class_loader,
                                                   cl_info,
                                                   CHECK_NULL);

  if (log_is_enabled(Debug, class, resolve)) {
    trace_class_resolution(k);
  }

  cls = (jclass)JNIHandles::make_local(THREAD, k->java_mirror());
  return cls;
JNI_END



DT_RETURN_MARK_DECL(FindClass, jclass
                    , HOTSPOT_JNI_FINDCLASS_RETURN(_ret_ref));

JNI_ENTRY(jclass, jni_FindClass(JNIEnv *env, const char *name))
  HOTSPOT_JNI_FINDCLASS_ENTRY(env, (char *)name);

  jclass result = NULL;
  DT_RETURN_MARK(FindClass, jclass, (const jclass&)result);

  // This should be ClassNotFoundException imo.
  TempNewSymbol class_name =
    SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_NoClassDefFoundError(),
                                        CHECK_NULL);

  //%note jni_3
  Handle protection_domain;
  // Find calling class
  Klass* k = thread->security_get_caller_class(0);
  // default to the system loader when no context
  Handle loader(THREAD, SystemDictionary::java_system_loader());
  if (k != NULL) {
    // Special handling to make sure JNI_OnLoad and JNI_OnUnload are executed
    // in the correct class context.
    if (k->class_loader() == NULL &&
        k->name() == vmSymbols::jdk_internal_loader_NativeLibraries()) {
      JavaValue result(T_OBJECT);
      JavaCalls::call_static(&result, k,
                             vmSymbols::getFromClass_name(),
                             vmSymbols::void_class_signature(),
                             CHECK_NULL);
      // When invoked from JNI_OnLoad, NativeLibraries::getFromClass returns
      // a non-NULL Class object.  When invoked from JNI_OnUnload,
      // it will return NULL to indicate no context.
      oop mirror = result.get_oop();
      if (mirror != NULL) {
        Klass* fromClass = java_lang_Class::as_Klass(mirror);
        loader = Handle(THREAD, fromClass->class_loader());
        protection_domain = Handle(THREAD, fromClass->protection_domain());
      }
    } else {
      loader = Handle(THREAD, k->class_loader());
    }
  }

  result = find_class_from_class_loader(env, class_name, true, loader,
                                        protection_domain, true, thread);

  if (log_is_enabled(Debug, class, resolve) && result != NULL) {
    trace_class_resolution(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(result)));
  }

  return result;
JNI_END

DT_RETURN_MARK_DECL(FromReflectedMethod, jmethodID
                    , HOTSPOT_JNI_FROMREFLECTEDMETHOD_RETURN((uintptr_t)_ret_ref));

JNI_ENTRY(jmethodID, jni_FromReflectedMethod(JNIEnv *env, jobject method))
  HOTSPOT_JNI_FROMREFLECTEDMETHOD_ENTRY(env, method);

  jmethodID ret = NULL;
  DT_RETURN_MARK(FromReflectedMethod, jmethodID, (const jmethodID&)ret);

  // method is a handle to a java.lang.reflect.Method object
  oop reflected  = JNIHandles::resolve_non_null(method);
  oop mirror     = NULL;
  int slot       = 0;

  if (reflected->klass() == vmClasses::reflect_Constructor_klass()) {
    mirror = java_lang_reflect_Constructor::clazz(reflected);
    slot   = java_lang_reflect_Constructor::slot(reflected);
  } else {
    assert(reflected->klass() == vmClasses::reflect_Method_klass(), "wrong type");
    mirror = java_lang_reflect_Method::clazz(reflected);
    slot   = java_lang_reflect_Method::slot(reflected);
  }
  Klass* k1 = java_lang_Class::as_Klass(mirror);

  // Make sure class is initialized before handing id's out to methods
  k1->initialize(CHECK_NULL);
  Method* m = InstanceKlass::cast(k1)->method_with_idnum(slot);
  ret = m==NULL? NULL : m->jmethod_id();  // return NULL if reflected method deleted
  return ret;
JNI_END

DT_RETURN_MARK_DECL(FromReflectedField, jfieldID
                    , HOTSPOT_JNI_FROMREFLECTEDFIELD_RETURN((uintptr_t)_ret_ref));

JNI_ENTRY(jfieldID, jni_FromReflectedField(JNIEnv *env, jobject field))
  HOTSPOT_JNI_FROMREFLECTEDFIELD_ENTRY(env, field);

  jfieldID ret = NULL;
  DT_RETURN_MARK(FromReflectedField, jfieldID, (const jfieldID&)ret);

  // field is a handle to a java.lang.reflect.Field object
  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  Klass* k1       = java_lang_Class::as_Klass(mirror);
  int slot        = java_lang_reflect_Field::slot(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  // Make sure class is initialized before handing id's out to fields
  k1->initialize(CHECK_NULL);

  // First check if this is a static field
  if (modifiers & JVM_ACC_STATIC) {
    intptr_t offset = InstanceKlass::cast(k1)->field_offset( slot );
    JNIid* id = InstanceKlass::cast(k1)->jni_id_for(offset);
    assert(id != NULL, "corrupt Field object");
    debug_only(id->set_is_static_field_id();)
    // A jfieldID for a static field is a JNIid specifying the field holder and the offset within the Klass*
    ret = jfieldIDWorkaround::to_static_jfieldID(id);
    return ret;
  }

  // The slot is the index of the field description in the field-array
  // The jfieldID is the offset of the field within the object
  // It may also have hash bits for k, if VerifyJNIFields is turned on.
  intptr_t offset = InstanceKlass::cast(k1)->field_offset( slot );
  assert(InstanceKlass::cast(k1)->contains_field_offset(offset), "stay within object");
  ret = jfieldIDWorkaround::to_instance_jfieldID(k1, offset);
  return ret;
JNI_END


DT_RETURN_MARK_DECL(ToReflectedMethod, jobject
                    , HOTSPOT_JNI_TOREFLECTEDMETHOD_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_ToReflectedMethod(JNIEnv *env, jclass cls, jmethodID method_id, jboolean isStatic))
  HOTSPOT_JNI_TOREFLECTEDMETHOD_ENTRY(env, cls, (uintptr_t) method_id, isStatic);

  jobject ret = NULL;
  DT_RETURN_MARK(ToReflectedMethod, jobject, (const jobject&)ret);

  methodHandle m (THREAD, Method::resolve_jmethod_id(method_id));
  assert(m->is_static() == (isStatic != 0), "jni_ToReflectedMethod access flags doesn't match");
  oop reflection_method;
  if (m->is_initializer()) {
    reflection_method = Reflection::new_constructor(m, CHECK_NULL);
  } else {
    reflection_method = Reflection::new_method(m, false, CHECK_NULL);
  }
  ret = JNIHandles::make_local(THREAD, reflection_method);
  return ret;
JNI_END

DT_RETURN_MARK_DECL(GetSuperclass, jclass
                    , HOTSPOT_JNI_GETSUPERCLASS_RETURN(_ret_ref));

JNI_ENTRY(jclass, jni_GetSuperclass(JNIEnv *env, jclass sub))
  HOTSPOT_JNI_GETSUPERCLASS_ENTRY(env, sub);

  jclass obj = NULL;
  DT_RETURN_MARK(GetSuperclass, jclass, (const jclass&)obj);

  oop mirror = JNIHandles::resolve_non_null(sub);
  // primitive classes return NULL
  if (java_lang_Class::is_primitive(mirror)) return NULL;

  // Rules of Class.getSuperClass as implemented by KLass::java_super:
  // arrays return Object
  // interfaces return NULL
  // proper classes return Klass::super()
  Klass* k = java_lang_Class::as_Klass(mirror);
  if (k->is_interface()) return NULL;

  // return mirror for superclass
  Klass* super = k->java_super();
  // super2 is the value computed by the compiler's getSuperClass intrinsic:
  debug_only(Klass* super2 = ( k->is_array_klass()
                                 ? vmClasses::Object_klass()
                                 : k->super() ) );
  assert(super == super2,
         "java_super computation depends on interface, array, other super");
  obj = (super == NULL) ? NULL : (jclass) JNIHandles::make_local(THREAD, super->java_mirror());
  return obj;
JNI_END

JNI_ENTRY_NO_PRESERVE(jboolean, jni_IsAssignableFrom(JNIEnv *env, jclass sub, jclass super))
  HOTSPOT_JNI_ISASSIGNABLEFROM_ENTRY(env, sub, super);

  oop sub_mirror   = JNIHandles::resolve_non_null(sub);
  oop super_mirror = JNIHandles::resolve_non_null(super);
  if (java_lang_Class::is_primitive(sub_mirror) ||
      java_lang_Class::is_primitive(super_mirror)) {
    jboolean ret = (sub_mirror == super_mirror);

    HOTSPOT_JNI_ISASSIGNABLEFROM_RETURN(ret);
    return ret;
  }
  Klass* sub_klass   = java_lang_Class::as_Klass(sub_mirror);
  Klass* super_klass = java_lang_Class::as_Klass(super_mirror);
  assert(sub_klass != NULL && super_klass != NULL, "invalid arguments to jni_IsAssignableFrom");
  jboolean ret = sub_klass->is_subtype_of(super_klass) ?
                   JNI_TRUE : JNI_FALSE;

  HOTSPOT_JNI_ISASSIGNABLEFROM_RETURN(ret);
  return ret;
JNI_END


DT_RETURN_MARK_DECL(Throw, jint
                    , HOTSPOT_JNI_THROW_RETURN(_ret_ref));

JNI_ENTRY(jint, jni_Throw(JNIEnv *env, jthrowable obj))
  HOTSPOT_JNI_THROW_ENTRY(env, obj);

  jint ret = JNI_OK;
  DT_RETURN_MARK(Throw, jint, (const jint&)ret);

  THROW_OOP_(JNIHandles::resolve(obj), JNI_OK);
  ShouldNotReachHere();
  return 0;  // Mute compiler.
JNI_END


DT_RETURN_MARK_DECL(ThrowNew, jint
                    , HOTSPOT_JNI_THROWNEW_RETURN(_ret_ref));

JNI_ENTRY(jint, jni_ThrowNew(JNIEnv *env, jclass clazz, const char *message))
  HOTSPOT_JNI_THROWNEW_ENTRY(env, clazz, (char *) message);

  jint ret = JNI_OK;
  DT_RETURN_MARK(ThrowNew, jint, (const jint&)ret);

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz)));
  Symbol*  name = k->name();
  Handle class_loader (THREAD,  k->class_loader());
  Handle protection_domain (THREAD, k->protection_domain());
  THROW_MSG_LOADER_(name, (char *)message, class_loader, protection_domain, JNI_OK);
  ShouldNotReachHere();
  return 0;  // Mute compiler.
JNI_END


// JNI functions only transform a pending async exception to a synchronous
// exception in ExceptionOccurred and ExceptionCheck calls, since
// delivering an async exception in other places won't change the native
// code's control flow and would be harmful when native code further calls
// JNI functions with a pending exception. Async exception is also checked
// during the call, so ExceptionOccurred/ExceptionCheck won't return
// false but deliver the async exception at the very end during
// state transition.

static void jni_check_async_exceptions(JavaThread *thread) {
  assert(thread == Thread::current(), "must be itself");
  thread->check_and_handle_async_exceptions();
}

JNI_ENTRY_NO_PRESERVE(jthrowable, jni_ExceptionOccurred(JNIEnv *env))
  HOTSPOT_JNI_EXCEPTIONOCCURRED_ENTRY(env);

  jni_check_async_exceptions(thread);
  oop exception = thread->pending_exception();
  jthrowable ret = (jthrowable) JNIHandles::make_local(THREAD, exception);

  HOTSPOT_JNI_EXCEPTIONOCCURRED_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY_NO_PRESERVE(void, jni_ExceptionDescribe(JNIEnv *env))
  HOTSPOT_JNI_EXCEPTIONDESCRIBE_ENTRY(env);

  if (thread->has_pending_exception()) {
    Handle ex(thread, thread->pending_exception());
    thread->clear_pending_exception();
    if (ex->is_a(vmClasses::ThreadDeath_klass())) {
      // Don't print anything if we are being killed.
    } else {
      jio_fprintf(defaultStream::error_stream(), "Exception ");
      if (thread != NULL && thread->threadObj() != NULL) {
        ResourceMark rm(THREAD);
        jio_fprintf(defaultStream::error_stream(),
        "in thread \"%s\" ", thread->name());
      }
      if (ex->is_a(vmClasses::Throwable_klass())) {
        JavaValue result(T_VOID);
        JavaCalls::call_virtual(&result,
                                ex,
                                vmClasses::Throwable_klass(),
                                vmSymbols::printStackTrace_name(),
                                vmSymbols::void_method_signature(),
                                THREAD);
        // If an exception is thrown in the call it gets thrown away. Not much
        // we can do with it. The native code that calls this, does not check
        // for the exception - hence, it might still be in the thread when DestroyVM gets
        // called, potentially causing a few asserts to trigger - since no pending exception
        // is expected.
        CLEAR_PENDING_EXCEPTION;
      } else {
        ResourceMark rm(THREAD);
        jio_fprintf(defaultStream::error_stream(),
        ". Uncaught exception of type %s.",
        ex->klass()->external_name());
      }
    }
  }

  HOTSPOT_JNI_EXCEPTIONDESCRIBE_RETURN();
JNI_END


JNI_ENTRY_NO_PRESERVE(void, jni_ExceptionClear(JNIEnv *env))
  HOTSPOT_JNI_EXCEPTIONCLEAR_ENTRY(env);

  // The jni code might be using this API to clear java thrown exception.
  // So just mark jvmti thread exception state as exception caught.
  JvmtiThreadState *state = JavaThread::current()->jvmti_thread_state();
  if (state != NULL && state->is_exception_detected()) {
    state->set_exception_caught();
  }
  thread->clear_pending_exception();

  HOTSPOT_JNI_EXCEPTIONCLEAR_RETURN();
JNI_END


JNI_ENTRY(void, jni_FatalError(JNIEnv *env, const char *msg))
  HOTSPOT_JNI_FATALERROR_ENTRY(env, (char *) msg);

  tty->print_cr("FATAL ERROR in native method: %s", msg);
  thread->print_stack();
  os::abort(); // Dump core and abort
JNI_END


JNI_ENTRY(jint, jni_PushLocalFrame(JNIEnv *env, jint capacity))
  HOTSPOT_JNI_PUSHLOCALFRAME_ENTRY(env, capacity);

  //%note jni_11
  if (capacity < 0 ||
      ((MaxJNILocalCapacity > 0) && (capacity > MaxJNILocalCapacity))) {
    HOTSPOT_JNI_PUSHLOCALFRAME_RETURN((uint32_t)JNI_ERR);
    return JNI_ERR;
  }
  JNIHandleBlock* old_handles = thread->active_handles();
  JNIHandleBlock* new_handles = JNIHandleBlock::allocate_block(thread);
  assert(new_handles != NULL, "should not be NULL");
  new_handles->set_pop_frame_link(old_handles);
  thread->set_active_handles(new_handles);
  jint ret = JNI_OK;
  HOTSPOT_JNI_PUSHLOCALFRAME_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY(jobject, jni_PopLocalFrame(JNIEnv *env, jobject result))
  HOTSPOT_JNI_POPLOCALFRAME_ENTRY(env, result);

  //%note jni_11
  Handle result_handle(thread, JNIHandles::resolve(result));
  JNIHandleBlock* old_handles = thread->active_handles();
  JNIHandleBlock* new_handles = old_handles->pop_frame_link();
  if (new_handles != NULL) {
    // As a sanity check we only release the handle blocks if the pop_frame_link is not NULL.
    // This way code will still work if PopLocalFrame is called without a corresponding
    // PushLocalFrame call. Note that we set the pop_frame_link to NULL explicitly, otherwise
    // the release_block call will release the blocks.
    thread->set_active_handles(new_handles);
    old_handles->set_pop_frame_link(NULL);              // clear link we won't release new_handles below
    JNIHandleBlock::release_block(old_handles, thread); // may block
    result = JNIHandles::make_local(thread, result_handle());
  }
  HOTSPOT_JNI_POPLOCALFRAME_RETURN(result);
  return result;
JNI_END


JNI_ENTRY(jobject, jni_NewGlobalRef(JNIEnv *env, jobject ref))
  HOTSPOT_JNI_NEWGLOBALREF_ENTRY(env, ref);

  Handle ref_handle(thread, JNIHandles::resolve(ref));
  jobject ret = JNIHandles::make_global(ref_handle, AllocFailStrategy::RETURN_NULL);

  HOTSPOT_JNI_NEWGLOBALREF_RETURN(ret);
  return ret;
JNI_END

// Must be JNI_ENTRY (with HandleMark)
JNI_ENTRY_NO_PRESERVE(void, jni_DeleteGlobalRef(JNIEnv *env, jobject ref))
  HOTSPOT_JNI_DELETEGLOBALREF_ENTRY(env, ref);

  JNIHandles::destroy_global(ref);

  HOTSPOT_JNI_DELETEGLOBALREF_RETURN();
JNI_END

JNI_ENTRY_NO_PRESERVE(void, jni_DeleteLocalRef(JNIEnv *env, jobject obj))
  HOTSPOT_JNI_DELETELOCALREF_ENTRY(env, obj);

  JNIHandles::destroy_local(obj);

  HOTSPOT_JNI_DELETELOCALREF_RETURN();
JNI_END

JNI_ENTRY_NO_PRESERVE(jboolean, jni_IsSameObject(JNIEnv *env, jobject r1, jobject r2))
  HOTSPOT_JNI_ISSAMEOBJECT_ENTRY(env, r1, r2);

  jboolean ret = JNIHandles::is_same_object(r1, r2) ? JNI_TRUE : JNI_FALSE;

  HOTSPOT_JNI_ISSAMEOBJECT_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY(jobject, jni_NewLocalRef(JNIEnv *env, jobject ref))
  HOTSPOT_JNI_NEWLOCALREF_ENTRY(env, ref);

  jobject ret = JNIHandles::make_local(THREAD, JNIHandles::resolve(ref),
                                       AllocFailStrategy::RETURN_NULL);

  HOTSPOT_JNI_NEWLOCALREF_RETURN(ret);
  return ret;
JNI_END

JNI_LEAF(jint, jni_EnsureLocalCapacity(JNIEnv *env, jint capacity))
  HOTSPOT_JNI_ENSURELOCALCAPACITY_ENTRY(env, capacity);

  jint ret;
  if (capacity >= 0 &&
      ((MaxJNILocalCapacity <= 0) || (capacity <= MaxJNILocalCapacity))) {
    ret = JNI_OK;
  } else {
    ret = JNI_ERR;
  }

  HOTSPOT_JNI_ENSURELOCALCAPACITY_RETURN(ret);
  return ret;
JNI_END

// Return the Handle Type
JNI_LEAF(jobjectRefType, jni_GetObjectRefType(JNIEnv *env, jobject obj))
  HOTSPOT_JNI_GETOBJECTREFTYPE_ENTRY(env, obj);

  jobjectRefType ret = JNIInvalidRefType;
  if (obj != NULL) {
    ret = JNIHandles::handle_type(thread, obj);
  }

  HOTSPOT_JNI_GETOBJECTREFTYPE_RETURN((void *) ret);
  return ret;
JNI_END


class JNI_ArgumentPusher : public SignatureIterator {
 protected:
  JavaCallArguments*  _arguments;

  void push_int(jint x)         { _arguments->push_int(x); }
  void push_long(jlong x)       { _arguments->push_long(x); }
  void push_float(jfloat x)     { _arguments->push_float(x); }
  void push_double(jdouble x)   { _arguments->push_double(x); }
  void push_object(jobject x)   { _arguments->push_jobject(x); }

  void push_boolean(jboolean b) {
    // Normalize boolean arguments from native code by converting 1-255 to JNI_TRUE and
    // 0 to JNI_FALSE.  Boolean return values from native are normalized the same in
    // TemplateInterpreterGenerator::generate_result_handler_for and
    // SharedRuntime::generate_native_wrapper.
    push_int(b == 0 ? JNI_FALSE : JNI_TRUE);
  }

  JNI_ArgumentPusher(Method* method)
    : SignatureIterator(method->signature(),
                        Fingerprinter(methodHandle(Thread::current(), method)).fingerprint())
  {
    _arguments = NULL;
  }

 public:
  virtual void push_arguments_on(JavaCallArguments* arguments) = 0;
};


class JNI_ArgumentPusherVaArg : public JNI_ArgumentPusher {
  va_list _ap;

  void set_ap(va_list rap) {
    va_copy(_ap, rap);
  }

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    switch (type) {
    // these are coerced to int when using va_arg
    case T_BYTE:
    case T_CHAR:
    case T_SHORT:
    case T_INT:         push_int(va_arg(_ap, jint)); break;
    case T_BOOLEAN:     push_boolean((jboolean) va_arg(_ap, jint)); break;

    // each of these paths is exercised by the various jck Call[Static,Nonvirtual,][Void,Int,..]Method[A,V,] tests

    case T_LONG:        push_long(va_arg(_ap, jlong)); break;
    // float is coerced to double w/ va_arg
    case T_FLOAT:       push_float((jfloat) va_arg(_ap, jdouble)); break;
    case T_DOUBLE:      push_double(va_arg(_ap, jdouble)); break;

    case T_ARRAY:
    case T_OBJECT:      push_object(va_arg(_ap, jobject)); break;
    default:            ShouldNotReachHere();
    }
  }

 public:
  JNI_ArgumentPusherVaArg(jmethodID method_id, va_list rap)
      : JNI_ArgumentPusher(Method::resolve_jmethod_id(method_id)) {
    set_ap(rap);
  }

  ~JNI_ArgumentPusherVaArg() {
    va_end(_ap);
  }

  virtual void push_arguments_on(JavaCallArguments* arguments) {
    _arguments = arguments;
    do_parameters_on(this);
  }
};


class JNI_ArgumentPusherArray : public JNI_ArgumentPusher {
 protected:
  const jvalue *_ap;

  inline void set_ap(const jvalue *rap) { _ap = rap; }

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    switch (type) {
    case T_CHAR:        push_int((_ap++)->c); break;
    case T_SHORT:       push_int((_ap++)->s); break;
    case T_BYTE:        push_int((_ap++)->b); break;
    case T_INT:         push_int((_ap++)->i); break;
    case T_BOOLEAN:     push_boolean((_ap++)->z); break;
    case T_LONG:        push_long((_ap++)->j); break;
    case T_FLOAT:       push_float((_ap++)->f); break;
    case T_DOUBLE:      push_double((_ap++)->d); break;
    case T_ARRAY:
    case T_OBJECT:      push_object((_ap++)->l); break;
    default:            ShouldNotReachHere();
    }
  }

 public:
  JNI_ArgumentPusherArray(jmethodID method_id, const jvalue *rap)
      : JNI_ArgumentPusher(Method::resolve_jmethod_id(method_id)) {
    set_ap(rap);
  }

  virtual void push_arguments_on(JavaCallArguments* arguments) {
    _arguments = arguments;
    do_parameters_on(this);
  }
};


enum JNICallType {
  JNI_STATIC,
  JNI_VIRTUAL,
  JNI_NONVIRTUAL
};



static void jni_invoke_static(JNIEnv *env, JavaValue* result, jobject receiver, JNICallType call_type, jmethodID method_id, JNI_ArgumentPusher *args, TRAPS) {
  methodHandle method(THREAD, Method::resolve_jmethod_id(method_id));

  // Create object to hold arguments for the JavaCall, and associate it with
  // the jni parser
  ResourceMark rm(THREAD);
  int number_of_parameters = method->size_of_parameters();
  JavaCallArguments java_args(number_of_parameters);

  assert(method->is_static(), "method should be static");

  // Fill out JavaCallArguments object
  args->push_arguments_on(&java_args);
  // Initialize result type
  result->set_type(args->return_type());

  // Invoke the method. Result is returned as oop.
  JavaCalls::call(result, method, &java_args, CHECK);

  // Convert result
  if (is_reference_type(result->get_type())) {
    result->set_jobject(JNIHandles::make_local(THREAD, result->get_oop()));
  }
}


static void jni_invoke_nonstatic(JNIEnv *env, JavaValue* result, jobject receiver, JNICallType call_type, jmethodID method_id, JNI_ArgumentPusher *args, TRAPS) {
  oop recv = JNIHandles::resolve(receiver);
  if (recv == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  Handle h_recv(THREAD, recv);

  int number_of_parameters;
  Method* selected_method;
  {
    Method* m = Method::resolve_jmethod_id(method_id);
    number_of_parameters = m->size_of_parameters();
    InstanceKlass* holder = m->method_holder();
    if (call_type != JNI_VIRTUAL) {
        selected_method = m;
    } else if (!m->has_itable_index()) {
      // non-interface call -- for that little speed boost, don't handlize
      debug_only(NoSafepointVerifier nosafepoint;)
      // jni_GetMethodID makes sure class is linked and initialized
      // so m should have a valid vtable index.
      assert(m->valid_vtable_index(), "no valid vtable index");
      int vtbl_index = m->vtable_index();
      if (vtbl_index != Method::nonvirtual_vtable_index) {
        selected_method = h_recv->klass()->method_at_vtable(vtbl_index);
      } else {
        // final method
        selected_method = m;
      }
    } else {
      // interface call
      int itbl_index = m->itable_index();
      Klass* k = h_recv->klass();
      selected_method = InstanceKlass::cast(k)->method_at_itable(holder, itbl_index, CHECK);
    }
  }

  methodHandle method(THREAD, selected_method);

  // Create object to hold arguments for the JavaCall, and associate it with
  // the jni parser
  ResourceMark rm(THREAD);
  JavaCallArguments java_args(number_of_parameters);

  // handle arguments
  assert(!method->is_static(), "method %s should not be static", method->name_and_sig_as_C_string());
  java_args.push_oop(h_recv); // Push jobject handle

  // Fill out JavaCallArguments object
  args->push_arguments_on(&java_args);
  // Initialize result type
  result->set_type(args->return_type());

  // Invoke the method. Result is returned as oop.
  JavaCalls::call(result, method, &java_args, CHECK);

  // Convert result
  if (is_reference_type(result->get_type())) {
    result->set_jobject(JNIHandles::make_local(THREAD, result->get_oop()));
  }
}

DT_RETURN_MARK_DECL(AllocObject, jobject
                    , HOTSPOT_JNI_ALLOCOBJECT_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_AllocObject(JNIEnv *env, jclass clazz))
  HOTSPOT_JNI_ALLOCOBJECT_ENTRY(env, clazz);

  jobject ret = NULL;
  DT_RETURN_MARK(AllocObject, jobject, (const jobject&)ret);

  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(clazz), CHECK_NULL);
  ret = JNIHandles::make_local(THREAD, i);
  return ret;
JNI_END

DT_RETURN_MARK_DECL(NewObjectA, jobject
                    , HOTSPOT_JNI_NEWOBJECTA_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_NewObjectA(JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args))
  HOTSPOT_JNI_NEWOBJECTA_ENTRY(env, clazz, (uintptr_t) methodID);

  jobject obj = NULL;
  DT_RETURN_MARK(NewObjectA, jobject, (const jobject&)obj);

  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(clazz), CHECK_NULL);
  obj = JNIHandles::make_local(THREAD, i);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherArray ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_NULL);
  return obj;
JNI_END


DT_RETURN_MARK_DECL(NewObjectV, jobject
                    , HOTSPOT_JNI_NEWOBJECTV_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_NewObjectV(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args))
  HOTSPOT_JNI_NEWOBJECTV_ENTRY(env, clazz, (uintptr_t) methodID);

  jobject obj = NULL;
  DT_RETURN_MARK(NewObjectV, jobject, (const jobject&)obj);

  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(clazz), CHECK_NULL);
  obj = JNIHandles::make_local(THREAD, i);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_NULL);
  return obj;
JNI_END


DT_RETURN_MARK_DECL(NewObject, jobject
                    , HOTSPOT_JNI_NEWOBJECT_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_NewObject(JNIEnv *env, jclass clazz, jmethodID methodID, ...))
  HOTSPOT_JNI_NEWOBJECT_ENTRY(env, clazz, (uintptr_t) methodID);

  jobject obj = NULL;
  DT_RETURN_MARK(NewObject, jobject, (const jobject&)obj);

  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(clazz), CHECK_NULL);
  obj = JNIHandles::make_local(THREAD, i);
  va_list args;
  va_start(args, methodID);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_NULL);
  va_end(args);
  return obj;
JNI_END


JNI_ENTRY(jclass, jni_GetObjectClass(JNIEnv *env, jobject obj))
  HOTSPOT_JNI_GETOBJECTCLASS_ENTRY(env, obj);

  Klass* k = JNIHandles::resolve_non_null(obj)->klass();
  jclass ret =
    (jclass) JNIHandles::make_local(THREAD, k->java_mirror());

  HOTSPOT_JNI_GETOBJECTCLASS_RETURN(ret);
  return ret;
JNI_END

JNI_ENTRY_NO_PRESERVE(jboolean, jni_IsInstanceOf(JNIEnv *env, jobject obj, jclass clazz))
  HOTSPOT_JNI_ISINSTANCEOF_ENTRY(env, obj, clazz);

  jboolean ret = JNI_TRUE;
  if (obj != NULL) {
    ret = JNI_FALSE;
    Klass* k = java_lang_Class::as_Klass(
      JNIHandles::resolve_non_null(clazz));
    if (k != NULL) {
      ret = JNIHandles::resolve_non_null(obj)->is_a(k) ? JNI_TRUE : JNI_FALSE;
    }
  }

  HOTSPOT_JNI_ISINSTANCEOF_RETURN(ret);
  return ret;
JNI_END


static jmethodID get_method_id(JNIEnv *env, jclass clazz, const char *name_str,
                               const char *sig, bool is_static, TRAPS) {
  // %%%% This code should probably just call into a method in the LinkResolver
  //
  // The class should have been loaded (we have an instance of the class
  // passed in) so the method and signature should already be in the symbol
  // table.  If they're not there, the method doesn't exist.
  const char *name_to_probe = (name_str == NULL)
                        ? vmSymbols::object_initializer_name()->as_C_string()
                        : name_str;
  TempNewSymbol name = SymbolTable::probe(name_to_probe, (int)strlen(name_to_probe));
  TempNewSymbol signature = SymbolTable::probe(sig, (int)strlen(sig));

  if (name == NULL || signature == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_NoSuchMethodError(), name_str);
  }

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* klass = java_lang_Class::as_Klass(mirror);

  // Throw a NoSuchMethodError exception if we have an instance of a
  // primitive java.lang.Class
  if (java_lang_Class::is_primitive(mirror)) {
    ResourceMark rm(THREAD);
    THROW_MSG_0(vmSymbols::java_lang_NoSuchMethodError(), err_msg("%s%s.%s%s", is_static ? "static " : "", klass->signature_name(), name_str, sig));
  }

  // Make sure class is linked and initialized before handing id's out to
  // Method*s.
  klass->initialize(CHECK_NULL);

  Method* m;
  if (name == vmSymbols::object_initializer_name() ||
      name == vmSymbols::class_initializer_name()) {
    // Never search superclasses for constructors
    if (klass->is_instance_klass()) {
      m = InstanceKlass::cast(klass)->find_method(name, signature);
    } else {
      m = NULL;
    }
  } else {
    m = klass->lookup_method(name, signature);
    if (m == NULL &&  klass->is_instance_klass()) {
      m = InstanceKlass::cast(klass)->lookup_method_in_ordered_interfaces(name, signature);
    }
  }
  if (m == NULL || (m->is_static() != is_static)) {
    ResourceMark rm(THREAD);
    THROW_MSG_0(vmSymbols::java_lang_NoSuchMethodError(), err_msg("%s%s.%s%s", is_static ? "static " : "", klass->signature_name(), name_str, sig));
  }
  return m->jmethod_id();
}


JNI_ENTRY(jmethodID, jni_GetMethodID(JNIEnv *env, jclass clazz,
          const char *name, const char *sig))
  HOTSPOT_JNI_GETMETHODID_ENTRY(env, clazz, (char *) name, (char *) sig);
  jmethodID ret = get_method_id(env, clazz, name, sig, false, thread);
  HOTSPOT_JNI_GETMETHODID_RETURN((uintptr_t) ret);
  return ret;
JNI_END


JNI_ENTRY(jmethodID, jni_GetStaticMethodID(JNIEnv *env, jclass clazz,
          const char *name, const char *sig))
  HOTSPOT_JNI_GETSTATICMETHODID_ENTRY(env, (char *) clazz, (char *) name, (char *)sig);
  jmethodID ret = get_method_id(env, clazz, name, sig, true, thread);
  HOTSPOT_JNI_GETSTATICMETHODID_RETURN((uintptr_t) ret);
  return ret;
JNI_END



//
// Calling Methods
//


#define DEFINE_CALLMETHOD(ResultType, Result, Tag \
                          , EntryProbe, ReturnProbe)    \
\
  DT_RETURN_MARK_DECL_FOR(Result, Call##Result##Method, ResultType \
                          , ReturnProbe);                          \
\
JNI_ENTRY(ResultType, \
          jni_Call##Result##Method(JNIEnv *env, jobject obj, jmethodID methodID, ...)) \
\
  EntryProbe; \
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, Call##Result##Method, ResultType, \
                     (const ResultType&)ret);\
\
  va_list args; \
  va_start(args, methodID); \
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK_0); \
  va_end(args); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLMETHOD(jboolean, Boolean, T_BOOLEAN
                  , HOTSPOT_JNI_CALLBOOLEANMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBOOLEANMETHOD_RETURN(_ret_ref))
DEFINE_CALLMETHOD(jbyte,    Byte,    T_BYTE
                  , HOTSPOT_JNI_CALLBYTEMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBYTEMETHOD_RETURN(_ret_ref))
DEFINE_CALLMETHOD(jchar,    Char,    T_CHAR
                  , HOTSPOT_JNI_CALLCHARMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLCHARMETHOD_RETURN(_ret_ref))
DEFINE_CALLMETHOD(jshort,   Short,   T_SHORT
                  , HOTSPOT_JNI_CALLSHORTMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLSHORTMETHOD_RETURN(_ret_ref))

DEFINE_CALLMETHOD(jobject,  Object,  T_OBJECT
                  , HOTSPOT_JNI_CALLOBJECTMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLOBJECTMETHOD_RETURN(_ret_ref))
DEFINE_CALLMETHOD(jint,     Int,     T_INT,
                  HOTSPOT_JNI_CALLINTMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLINTMETHOD_RETURN(_ret_ref))
DEFINE_CALLMETHOD(jlong,    Long,    T_LONG
                  , HOTSPOT_JNI_CALLLONGMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLLONGMETHOD_RETURN(_ret_ref))
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLMETHOD(jfloat,   Float,   T_FLOAT
                  , HOTSPOT_JNI_CALLFLOATMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLFLOATMETHOD_RETURN())
DEFINE_CALLMETHOD(jdouble,  Double,  T_DOUBLE
                  , HOTSPOT_JNI_CALLDOUBLEMETHOD_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLDOUBLEMETHOD_RETURN())

#define DEFINE_CALLMETHODV(ResultType, Result, Tag \
                          , EntryProbe, ReturnProbe)    \
\
  DT_RETURN_MARK_DECL_FOR(Result, Call##Result##MethodV, ResultType \
                          , ReturnProbe);                          \
\
JNI_ENTRY(ResultType, \
          jni_Call##Result##MethodV(JNIEnv *env, jobject obj, jmethodID methodID, va_list args)) \
\
  EntryProbe;\
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, Call##Result##MethodV, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK_0); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLMETHODV(jboolean, Boolean, T_BOOLEAN
                  , HOTSPOT_JNI_CALLBOOLEANMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBOOLEANMETHODV_RETURN(_ret_ref))
DEFINE_CALLMETHODV(jbyte,    Byte,    T_BYTE
                  , HOTSPOT_JNI_CALLBYTEMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBYTEMETHODV_RETURN(_ret_ref))
DEFINE_CALLMETHODV(jchar,    Char,    T_CHAR
                  , HOTSPOT_JNI_CALLCHARMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLCHARMETHODV_RETURN(_ret_ref))
DEFINE_CALLMETHODV(jshort,   Short,   T_SHORT
                  , HOTSPOT_JNI_CALLSHORTMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLSHORTMETHODV_RETURN(_ret_ref))

DEFINE_CALLMETHODV(jobject,  Object,  T_OBJECT
                  , HOTSPOT_JNI_CALLOBJECTMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLOBJECTMETHODV_RETURN(_ret_ref))
DEFINE_CALLMETHODV(jint,     Int,     T_INT,
                  HOTSPOT_JNI_CALLINTMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLINTMETHODV_RETURN(_ret_ref))
DEFINE_CALLMETHODV(jlong,    Long,    T_LONG
                  , HOTSPOT_JNI_CALLLONGMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLLONGMETHODV_RETURN(_ret_ref))
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLMETHODV(jfloat,   Float,   T_FLOAT
                  , HOTSPOT_JNI_CALLFLOATMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLFLOATMETHODV_RETURN())
DEFINE_CALLMETHODV(jdouble,  Double,  T_DOUBLE
                  , HOTSPOT_JNI_CALLDOUBLEMETHODV_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLDOUBLEMETHODV_RETURN())

#define DEFINE_CALLMETHODA(ResultType, Result, Tag \
                          , EntryProbe, ReturnProbe)    \
\
  DT_RETURN_MARK_DECL_FOR(Result, Call##Result##MethodA, ResultType \
                          , ReturnProbe);                          \
\
JNI_ENTRY(ResultType, \
          jni_Call##Result##MethodA(JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args)) \
  EntryProbe; \
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, Call##Result##MethodA, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherArray ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK_0); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLMETHODA(jboolean, Boolean, T_BOOLEAN
                  , HOTSPOT_JNI_CALLBOOLEANMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBOOLEANMETHODA_RETURN(_ret_ref))
DEFINE_CALLMETHODA(jbyte,    Byte,    T_BYTE
                  , HOTSPOT_JNI_CALLBYTEMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLBYTEMETHODA_RETURN(_ret_ref))
DEFINE_CALLMETHODA(jchar,    Char,    T_CHAR
                  , HOTSPOT_JNI_CALLCHARMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLCHARMETHODA_RETURN(_ret_ref))
DEFINE_CALLMETHODA(jshort,   Short,   T_SHORT
                  , HOTSPOT_JNI_CALLSHORTMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLSHORTMETHODA_RETURN(_ret_ref))

DEFINE_CALLMETHODA(jobject,  Object,  T_OBJECT
                  , HOTSPOT_JNI_CALLOBJECTMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLOBJECTMETHODA_RETURN(_ret_ref))
DEFINE_CALLMETHODA(jint,     Int,     T_INT,
                  HOTSPOT_JNI_CALLINTMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLINTMETHODA_RETURN(_ret_ref))
DEFINE_CALLMETHODA(jlong,    Long,    T_LONG
                  , HOTSPOT_JNI_CALLLONGMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLLONGMETHODA_RETURN(_ret_ref))
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLMETHODA(jfloat,   Float,   T_FLOAT
                  , HOTSPOT_JNI_CALLFLOATMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLFLOATMETHODA_RETURN())
DEFINE_CALLMETHODA(jdouble,  Double,  T_DOUBLE
                  , HOTSPOT_JNI_CALLDOUBLEMETHODA_ENTRY(env, obj, (uintptr_t)methodID),
                  HOTSPOT_JNI_CALLDOUBLEMETHODA_RETURN())

DT_VOID_RETURN_MARK_DECL(CallVoidMethod, HOTSPOT_JNI_CALLVOIDMETHOD_RETURN());
DT_VOID_RETURN_MARK_DECL(CallVoidMethodV, HOTSPOT_JNI_CALLVOIDMETHODV_RETURN());
DT_VOID_RETURN_MARK_DECL(CallVoidMethodA, HOTSPOT_JNI_CALLVOIDMETHODA_RETURN());


JNI_ENTRY(void, jni_CallVoidMethod(JNIEnv *env, jobject obj, jmethodID methodID, ...))
  HOTSPOT_JNI_CALLVOIDMETHOD_ENTRY(env, obj, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallVoidMethod);

  va_list args;
  va_start(args, methodID);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK);
  va_end(args);
JNI_END


JNI_ENTRY(void, jni_CallVoidMethodV(JNIEnv *env, jobject obj, jmethodID methodID, va_list args))
  HOTSPOT_JNI_CALLVOIDMETHODV_ENTRY(env, obj, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallVoidMethodV);

  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK);
JNI_END


JNI_ENTRY(void, jni_CallVoidMethodA(JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args))
  HOTSPOT_JNI_CALLVOIDMETHODA_ENTRY(env, obj, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallVoidMethodA);

  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherArray ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_VIRTUAL, methodID, &ap, CHECK);
JNI_END



#define DEFINE_CALLNONVIRTUALMETHOD(ResultType, Result, Tag \
                                    , EntryProbe, ReturnProbe)      \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallNonvirtual##Result##Method, ResultType \
                          , ReturnProbe);\
\
JNI_ENTRY(ResultType, \
          jni_CallNonvirtual##Result##Method(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, ...)) \
\
  EntryProbe;\
  ResultType ret;\
  DT_RETURN_MARK_FOR(Result, CallNonvirtual##Result##Method, ResultType, \
                     (const ResultType&)ret);\
\
  va_list args; \
  va_start(args, methodID); \
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_0); \
  va_end(args); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLNONVIRTUALMETHOD(jboolean, Boolean, T_BOOLEAN
                            , HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jbyte,    Byte,    T_BYTE
                            , HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jchar,    Char,    T_CHAR
                            , HOTSPOT_JNI_CALLNONVIRTUALCHARMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALCHARMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jshort,   Short,   T_SHORT
                            , HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHOD_RETURN(_ret_ref))

DEFINE_CALLNONVIRTUALMETHOD(jobject,  Object,  T_OBJECT
                            , HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jint,     Int,     T_INT
                            , HOTSPOT_JNI_CALLNONVIRTUALINTMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALINTMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jlong,    Long,    T_LONG
                            , HOTSPOT_JNI_CALLNONVIRTUALLONGMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
// Float and double probes don't return value because dtrace doesn't currently support it
                            HOTSPOT_JNI_CALLNONVIRTUALLONGMETHOD_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHOD(jfloat,   Float,   T_FLOAT
                            , HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHOD_RETURN())
DEFINE_CALLNONVIRTUALMETHOD(jdouble,  Double,  T_DOUBLE
                            , HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHOD_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHOD_RETURN())

#define DEFINE_CALLNONVIRTUALMETHODV(ResultType, Result, Tag \
                                    , EntryProbe, ReturnProbe)      \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallNonvirtual##Result##MethodV, ResultType \
                          , ReturnProbe);\
\
JNI_ENTRY(ResultType, \
          jni_CallNonvirtual##Result##MethodV(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, va_list args)) \
\
  EntryProbe;\
  ResultType ret;\
  DT_RETURN_MARK_FOR(Result, CallNonvirtual##Result##MethodV, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_0); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLNONVIRTUALMETHODV(jboolean, Boolean, T_BOOLEAN
                            , HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jbyte,    Byte,    T_BYTE
                            , HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jchar,    Char,    T_CHAR
                            , HOTSPOT_JNI_CALLNONVIRTUALCHARMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALCHARMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jshort,   Short,   T_SHORT
                            , HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHODV_RETURN(_ret_ref))

DEFINE_CALLNONVIRTUALMETHODV(jobject,  Object,  T_OBJECT
                            , HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jint,     Int,     T_INT
                            , HOTSPOT_JNI_CALLNONVIRTUALINTMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALINTMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jlong,    Long,    T_LONG
                            , HOTSPOT_JNI_CALLNONVIRTUALLONGMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
// Float and double probes don't return value because dtrace doesn't currently support it
                            HOTSPOT_JNI_CALLNONVIRTUALLONGMETHODV_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODV(jfloat,   Float,   T_FLOAT
                            , HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHODV_RETURN())
DEFINE_CALLNONVIRTUALMETHODV(jdouble,  Double,  T_DOUBLE
                            , HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHODV_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHODV_RETURN())

#define DEFINE_CALLNONVIRTUALMETHODA(ResultType, Result, Tag \
                                    , EntryProbe, ReturnProbe)      \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallNonvirtual##Result##MethodA, ResultType \
                          , ReturnProbe);\
\
JNI_ENTRY(ResultType, \
          jni_CallNonvirtual##Result##MethodA(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, const jvalue *args)) \
\
  EntryProbe;\
  ResultType ret;\
  DT_RETURN_MARK_FOR(Result, CallNonvirtual##Result##MethodA, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherArray ap(methodID, args); \
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK_0); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLNONVIRTUALMETHODA(jboolean, Boolean, T_BOOLEAN
                            , HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBOOLEANMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jbyte,    Byte,    T_BYTE
                            , HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALBYTEMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jchar,    Char,    T_CHAR
                            , HOTSPOT_JNI_CALLNONVIRTUALCHARMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALCHARMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jshort,   Short,   T_SHORT
                            , HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALSHORTMETHODA_RETURN(_ret_ref))

DEFINE_CALLNONVIRTUALMETHODA(jobject,  Object,  T_OBJECT
                            , HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALOBJECTMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jint,     Int,     T_INT
                            , HOTSPOT_JNI_CALLNONVIRTUALINTMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALINTMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jlong,    Long,    T_LONG
                            , HOTSPOT_JNI_CALLNONVIRTUALLONGMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
// Float and double probes don't return value because dtrace doesn't currently support it
                            HOTSPOT_JNI_CALLNONVIRTUALLONGMETHODA_RETURN(_ret_ref))
DEFINE_CALLNONVIRTUALMETHODA(jfloat,   Float,   T_FLOAT
                            , HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALFLOATMETHODA_RETURN())
DEFINE_CALLNONVIRTUALMETHODA(jdouble,  Double,  T_DOUBLE
                            , HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHODA_ENTRY(env, obj, cls, (uintptr_t)methodID),
                            HOTSPOT_JNI_CALLNONVIRTUALDOUBLEMETHODA_RETURN())

DT_VOID_RETURN_MARK_DECL(CallNonvirtualVoidMethod
                         , HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHOD_RETURN());
DT_VOID_RETURN_MARK_DECL(CallNonvirtualVoidMethodV
                         , HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHODV_RETURN());
DT_VOID_RETURN_MARK_DECL(CallNonvirtualVoidMethodA
                         , HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHODA_RETURN());

JNI_ENTRY(void, jni_CallNonvirtualVoidMethod(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, ...))
  HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHOD_ENTRY(env, obj, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallNonvirtualVoidMethod);

  va_list args;
  va_start(args, methodID);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK);
  va_end(args);
JNI_END


JNI_ENTRY(void, jni_CallNonvirtualVoidMethodV(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, va_list args))
  HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHODV_ENTRY(
               env, obj, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallNonvirtualVoidMethodV);

  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK);
JNI_END


JNI_ENTRY(void, jni_CallNonvirtualVoidMethodA(JNIEnv *env, jobject obj, jclass cls, jmethodID methodID, const jvalue *args))
  HOTSPOT_JNI_CALLNONVIRTUALVOIDMETHODA_ENTRY(
                env, obj, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallNonvirtualVoidMethodA);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherArray ap(methodID, args);
  jni_invoke_nonstatic(env, &jvalue, obj, JNI_NONVIRTUAL, methodID, &ap, CHECK);
JNI_END



#define DEFINE_CALLSTATICMETHOD(ResultType, Result, Tag \
                                , EntryProbe, ResultProbe) \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallStatic##Result##Method, ResultType \
                          , ResultProbe);                               \
\
JNI_ENTRY(ResultType, \
          jni_CallStatic##Result##Method(JNIEnv *env, jclass cls, jmethodID methodID, ...)) \
\
  EntryProbe; \
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, CallStatic##Result##Method, ResultType, \
                     (const ResultType&)ret);\
\
  va_list args; \
  va_start(args, methodID); \
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK_0); \
  va_end(args); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLSTATICMETHOD(jboolean, Boolean, T_BOOLEAN
                        , HOTSPOT_JNI_CALLSTATICBOOLEANMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBOOLEANMETHOD_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHOD(jbyte,    Byte,    T_BYTE
                        , HOTSPOT_JNI_CALLSTATICBYTEMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBYTEMETHOD_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHOD(jchar,    Char,    T_CHAR
                        , HOTSPOT_JNI_CALLSTATICCHARMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICCHARMETHOD_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHOD(jshort,   Short,   T_SHORT
                        , HOTSPOT_JNI_CALLSTATICSHORTMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICSHORTMETHOD_RETURN(_ret_ref));

DEFINE_CALLSTATICMETHOD(jobject,  Object,  T_OBJECT
                        , HOTSPOT_JNI_CALLSTATICOBJECTMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICOBJECTMETHOD_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHOD(jint,     Int,     T_INT
                        , HOTSPOT_JNI_CALLSTATICINTMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICINTMETHOD_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHOD(jlong,    Long,    T_LONG
                        , HOTSPOT_JNI_CALLSTATICLONGMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICLONGMETHOD_RETURN(_ret_ref));
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLSTATICMETHOD(jfloat,   Float,   T_FLOAT
                        , HOTSPOT_JNI_CALLSTATICFLOATMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICFLOATMETHOD_RETURN());
DEFINE_CALLSTATICMETHOD(jdouble,  Double,  T_DOUBLE
                        , HOTSPOT_JNI_CALLSTATICDOUBLEMETHOD_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICDOUBLEMETHOD_RETURN());

#define DEFINE_CALLSTATICMETHODV(ResultType, Result, Tag \
                                , EntryProbe, ResultProbe) \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallStatic##Result##MethodV, ResultType \
                          , ResultProbe);                               \
\
JNI_ENTRY(ResultType, \
          jni_CallStatic##Result##MethodV(JNIEnv *env, jclass cls, jmethodID methodID, va_list args)) \
\
  EntryProbe; \
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, CallStatic##Result##MethodV, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherVaArg ap(methodID, args); \
  /* Make sure class is initialized before trying to invoke its method */ \
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls)); \
  k->initialize(CHECK_0); \
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK_0); \
  va_end(args); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLSTATICMETHODV(jboolean, Boolean, T_BOOLEAN
                        , HOTSPOT_JNI_CALLSTATICBOOLEANMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBOOLEANMETHODV_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODV(jbyte,    Byte,    T_BYTE
                        , HOTSPOT_JNI_CALLSTATICBYTEMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBYTEMETHODV_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODV(jchar,    Char,    T_CHAR
                        , HOTSPOT_JNI_CALLSTATICCHARMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICCHARMETHODV_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODV(jshort,   Short,   T_SHORT
                        , HOTSPOT_JNI_CALLSTATICSHORTMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICSHORTMETHODV_RETURN(_ret_ref));

DEFINE_CALLSTATICMETHODV(jobject,  Object,  T_OBJECT
                        , HOTSPOT_JNI_CALLSTATICOBJECTMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICOBJECTMETHODV_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODV(jint,     Int,     T_INT
                        , HOTSPOT_JNI_CALLSTATICINTMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICINTMETHODV_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODV(jlong,    Long,    T_LONG
                        , HOTSPOT_JNI_CALLSTATICLONGMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICLONGMETHODV_RETURN(_ret_ref));
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLSTATICMETHODV(jfloat,   Float,   T_FLOAT
                        , HOTSPOT_JNI_CALLSTATICFLOATMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICFLOATMETHODV_RETURN());
DEFINE_CALLSTATICMETHODV(jdouble,  Double,  T_DOUBLE
                        , HOTSPOT_JNI_CALLSTATICDOUBLEMETHODV_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICDOUBLEMETHODV_RETURN());

#define DEFINE_CALLSTATICMETHODA(ResultType, Result, Tag \
                                , EntryProbe, ResultProbe) \
\
  DT_RETURN_MARK_DECL_FOR(Result, CallStatic##Result##MethodA, ResultType \
                          , ResultProbe);                               \
\
JNI_ENTRY(ResultType, \
          jni_CallStatic##Result##MethodA(JNIEnv *env, jclass cls, jmethodID methodID, const jvalue *args)) \
\
  EntryProbe; \
  ResultType ret = 0;\
  DT_RETURN_MARK_FOR(Result, CallStatic##Result##MethodA, ResultType, \
                     (const ResultType&)ret);\
\
  JavaValue jvalue(Tag); \
  JNI_ArgumentPusherArray ap(methodID, args); \
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK_0); \
  ret = jvalue.get_##ResultType(); \
  return ret;\
JNI_END

// the runtime type of subword integral basic types is integer
DEFINE_CALLSTATICMETHODA(jboolean, Boolean, T_BOOLEAN
                        , HOTSPOT_JNI_CALLSTATICBOOLEANMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBOOLEANMETHODA_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODA(jbyte,    Byte,    T_BYTE
                        , HOTSPOT_JNI_CALLSTATICBYTEMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICBYTEMETHODA_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODA(jchar,    Char,    T_CHAR
                        , HOTSPOT_JNI_CALLSTATICCHARMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICCHARMETHODA_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODA(jshort,   Short,   T_SHORT
                        , HOTSPOT_JNI_CALLSTATICSHORTMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICSHORTMETHODA_RETURN(_ret_ref));

DEFINE_CALLSTATICMETHODA(jobject,  Object,  T_OBJECT
                        , HOTSPOT_JNI_CALLSTATICOBJECTMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICOBJECTMETHODA_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODA(jint,     Int,     T_INT
                        , HOTSPOT_JNI_CALLSTATICINTMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICINTMETHODA_RETURN(_ret_ref));
DEFINE_CALLSTATICMETHODA(jlong,    Long,    T_LONG
                        , HOTSPOT_JNI_CALLSTATICLONGMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICLONGMETHODA_RETURN(_ret_ref));
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_CALLSTATICMETHODA(jfloat,   Float,   T_FLOAT
                        , HOTSPOT_JNI_CALLSTATICFLOATMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICFLOATMETHODA_RETURN());
DEFINE_CALLSTATICMETHODA(jdouble,  Double,  T_DOUBLE
                        , HOTSPOT_JNI_CALLSTATICDOUBLEMETHODA_ENTRY(env, cls, (uintptr_t)methodID),
                        HOTSPOT_JNI_CALLSTATICDOUBLEMETHODA_RETURN());

DT_VOID_RETURN_MARK_DECL(CallStaticVoidMethod
                         , HOTSPOT_JNI_CALLSTATICVOIDMETHOD_RETURN());
DT_VOID_RETURN_MARK_DECL(CallStaticVoidMethodV
                         , HOTSPOT_JNI_CALLSTATICVOIDMETHODV_RETURN());
DT_VOID_RETURN_MARK_DECL(CallStaticVoidMethodA
                         , HOTSPOT_JNI_CALLSTATICVOIDMETHODA_RETURN());

JNI_ENTRY(void, jni_CallStaticVoidMethod(JNIEnv *env, jclass cls, jmethodID methodID, ...))
  HOTSPOT_JNI_CALLSTATICVOIDMETHOD_ENTRY(env, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallStaticVoidMethod);

  va_list args;
  va_start(args, methodID);
  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK);
  va_end(args);
JNI_END


JNI_ENTRY(void, jni_CallStaticVoidMethodV(JNIEnv *env, jclass cls, jmethodID methodID, va_list args))
  HOTSPOT_JNI_CALLSTATICVOIDMETHODV_ENTRY(env, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallStaticVoidMethodV);

  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherVaArg ap(methodID, args);
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK);
JNI_END


JNI_ENTRY(void, jni_CallStaticVoidMethodA(JNIEnv *env, jclass cls, jmethodID methodID, const jvalue *args))
  HOTSPOT_JNI_CALLSTATICVOIDMETHODA_ENTRY(env, cls, (uintptr_t) methodID);
  DT_VOID_RETURN_MARK(CallStaticVoidMethodA);

  JavaValue jvalue(T_VOID);
  JNI_ArgumentPusherArray ap(methodID, args);
  jni_invoke_static(env, &jvalue, NULL, JNI_STATIC, methodID, &ap, CHECK);
JNI_END


//
// Accessing Fields
//


DT_RETURN_MARK_DECL(GetFieldID, jfieldID
                    , HOTSPOT_JNI_GETFIELDID_RETURN((uintptr_t)_ret_ref));

JNI_ENTRY(jfieldID, jni_GetFieldID(JNIEnv *env, jclass clazz,
          const char *name, const char *sig))
  HOTSPOT_JNI_GETFIELDID_ENTRY(env, clazz, (char *) name, (char *) sig);
  jfieldID ret = 0;
  DT_RETURN_MARK(GetFieldID, jfieldID, (const jfieldID&)ret);

  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz));

  // The class should have been loaded (we have an instance of the class
  // passed in) so the field and signature should already be in the symbol
  // table.  If they're not there, the field doesn't exist.
  TempNewSymbol fieldname = SymbolTable::probe(name, (int)strlen(name));
  TempNewSymbol signame = SymbolTable::probe(sig, (int)strlen(sig));
  if (fieldname == NULL || signame == NULL) {
    ResourceMark rm;
    THROW_MSG_0(vmSymbols::java_lang_NoSuchFieldError(), err_msg("%s.%s %s", k->external_name(), name, sig));
  }

  // Make sure class is initialized before handing id's out to fields
  k->initialize(CHECK_NULL);

  fieldDescriptor fd;
  if (!k->is_instance_klass() ||
      !InstanceKlass::cast(k)->find_field(fieldname, signame, false, &fd)) {
    ResourceMark rm;
    THROW_MSG_0(vmSymbols::java_lang_NoSuchFieldError(), err_msg("%s.%s %s", k->external_name(), name, sig));
  }

  // A jfieldID for a non-static field is simply the offset of the field within the instanceOop
  // It may also have hash bits for k, if VerifyJNIFields is turned on.
  ret = jfieldIDWorkaround::to_instance_jfieldID(k, fd.offset());
  return ret;
JNI_END


JNI_ENTRY(jobject, jni_GetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID))
  HOTSPOT_JNI_GETOBJECTFIELD_ENTRY(env, obj, (uintptr_t) fieldID);
  oop o = JNIHandles::resolve_non_null(obj);
  Klass* k = o->klass();
  int offset = jfieldIDWorkaround::from_instance_jfieldID(k, fieldID);
  // Keep JVMTI addition small and only check enabled flag here.
  // jni_GetField_probe() assumes that is okay to create handles.
  if (JvmtiExport::should_post_field_access()) {
    o = JvmtiExport::jni_GetField_probe(thread, obj, o, k, fieldID, false);
  }
  oop loaded_obj = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_load_at(o, offset);
  jobject ret = JNIHandles::make_local(THREAD, loaded_obj);
  HOTSPOT_JNI_GETOBJECTFIELD_RETURN(ret);
  return ret;
JNI_END



#define DEFINE_GETFIELD(Return,Fieldname,Result \
  , EntryProbe, ReturnProbe) \
\
  DT_RETURN_MARK_DECL_FOR(Result, Get##Result##Field, Return \
  , ReturnProbe); \
\
JNI_ENTRY_NO_PRESERVE(Return, jni_Get##Result##Field(JNIEnv *env, jobject obj, jfieldID fieldID)) \
\
  EntryProbe; \
  Return ret = 0;\
  DT_RETURN_MARK_FOR(Result, Get##Result##Field, Return, (const Return&)ret);\
\
  oop o = JNIHandles::resolve_non_null(obj); \
  Klass* k = o->klass(); \
  int offset = jfieldIDWorkaround::from_instance_jfieldID(k, fieldID);  \
  /* Keep JVMTI addition small and only check enabled flag here.       */ \
  if (JvmtiExport::should_post_field_access()) { \
    o = JvmtiExport::jni_GetField_probe(thread, obj, o, k, fieldID, false); \
  } \
  ret = o->Fieldname##_field(offset); \
  return ret; \
JNI_END

DEFINE_GETFIELD(jboolean, bool,   Boolean
                , HOTSPOT_JNI_GETBOOLEANFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETBOOLEANFIELD_RETURN(_ret_ref))
DEFINE_GETFIELD(jbyte,    byte,   Byte
                , HOTSPOT_JNI_GETBYTEFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETBYTEFIELD_RETURN(_ret_ref))
DEFINE_GETFIELD(jchar,    char,   Char
                , HOTSPOT_JNI_GETCHARFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETCHARFIELD_RETURN(_ret_ref))
DEFINE_GETFIELD(jshort,   short,  Short
                , HOTSPOT_JNI_GETSHORTFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETSHORTFIELD_RETURN(_ret_ref))
DEFINE_GETFIELD(jint,     int,    Int
                , HOTSPOT_JNI_GETINTFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETINTFIELD_RETURN(_ret_ref))
DEFINE_GETFIELD(jlong,    long,   Long
                , HOTSPOT_JNI_GETLONGFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETLONGFIELD_RETURN(_ret_ref))
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_GETFIELD(jfloat,   float,  Float
                , HOTSPOT_JNI_GETFLOATFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETFLOATFIELD_RETURN())
DEFINE_GETFIELD(jdouble,  double, Double
                , HOTSPOT_JNI_GETDOUBLEFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_GETDOUBLEFIELD_RETURN())

address jni_GetBooleanField_addr() {
  return (address)jni_GetBooleanField;
}
address jni_GetByteField_addr() {
  return (address)jni_GetByteField;
}
address jni_GetCharField_addr() {
  return (address)jni_GetCharField;
}
address jni_GetShortField_addr() {
  return (address)jni_GetShortField;
}
address jni_GetIntField_addr() {
  return (address)jni_GetIntField;
}
address jni_GetLongField_addr() {
  return (address)jni_GetLongField;
}
address jni_GetFloatField_addr() {
  return (address)jni_GetFloatField;
}
address jni_GetDoubleField_addr() {
  return (address)jni_GetDoubleField;
}

JNI_ENTRY_NO_PRESERVE(void, jni_SetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID, jobject value))
  HOTSPOT_JNI_SETOBJECTFIELD_ENTRY(env, obj, (uintptr_t) fieldID, value);
  oop o = JNIHandles::resolve_non_null(obj);
  Klass* k = o->klass();
  int offset = jfieldIDWorkaround::from_instance_jfieldID(k, fieldID);
  // Keep JVMTI addition small and only check enabled flag here.
  if (JvmtiExport::should_post_field_modification()) {
    jvalue field_value;
    field_value.l = value;
    o = JvmtiExport::jni_SetField_probe(thread, obj, o, k, fieldID, false, JVM_SIGNATURE_CLASS, (jvalue *)&field_value);
  }
  HeapAccess<ON_UNKNOWN_OOP_REF>::oop_store_at(o, offset, JNIHandles::resolve(value));
  HOTSPOT_JNI_SETOBJECTFIELD_RETURN();
JNI_END


#define DEFINE_SETFIELD(Argument,Fieldname,Result,SigType,unionType \
                        , EntryProbe, ReturnProbe) \
\
JNI_ENTRY_NO_PRESERVE(void, jni_Set##Result##Field(JNIEnv *env, jobject obj, jfieldID fieldID, Argument value)) \
\
  EntryProbe; \
\
  oop o = JNIHandles::resolve_non_null(obj); \
  Klass* k = o->klass(); \
  int offset = jfieldIDWorkaround::from_instance_jfieldID(k, fieldID);  \
  /* Keep JVMTI addition small and only check enabled flag here.       */ \
  if (JvmtiExport::should_post_field_modification()) { \
    jvalue field_value; \
    field_value.unionType = value; \
    o = JvmtiExport::jni_SetField_probe(thread, obj, o, k, fieldID, false, SigType, (jvalue *)&field_value); \
  } \
  if (SigType == JVM_SIGNATURE_BOOLEAN) { value = ((jboolean)value) & 1; } \
  o->Fieldname##_field_put(offset, value); \
  ReturnProbe; \
JNI_END

DEFINE_SETFIELD(jboolean, bool,   Boolean, JVM_SIGNATURE_BOOLEAN, z
                , HOTSPOT_JNI_SETBOOLEANFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETBOOLEANFIELD_RETURN())
DEFINE_SETFIELD(jbyte,    byte,   Byte,    JVM_SIGNATURE_BYTE, b
                , HOTSPOT_JNI_SETBYTEFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETBYTEFIELD_RETURN())
DEFINE_SETFIELD(jchar,    char,   Char,    JVM_SIGNATURE_CHAR, c
                , HOTSPOT_JNI_SETCHARFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETCHARFIELD_RETURN())
DEFINE_SETFIELD(jshort,   short,  Short,   JVM_SIGNATURE_SHORT, s
                , HOTSPOT_JNI_SETSHORTFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETSHORTFIELD_RETURN())
DEFINE_SETFIELD(jint,     int,    Int,     JVM_SIGNATURE_INT, i
                , HOTSPOT_JNI_SETINTFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETINTFIELD_RETURN())
DEFINE_SETFIELD(jlong,    long,   Long,    JVM_SIGNATURE_LONG, j
                , HOTSPOT_JNI_SETLONGFIELD_ENTRY(env, obj, (uintptr_t)fieldID, value),
                HOTSPOT_JNI_SETLONGFIELD_RETURN())
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_SETFIELD(jfloat,   float,  Float,   JVM_SIGNATURE_FLOAT, f
                , HOTSPOT_JNI_SETFLOATFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_SETFLOATFIELD_RETURN())
DEFINE_SETFIELD(jdouble,  double, Double,  JVM_SIGNATURE_DOUBLE, d
                , HOTSPOT_JNI_SETDOUBLEFIELD_ENTRY(env, obj, (uintptr_t)fieldID),
                HOTSPOT_JNI_SETDOUBLEFIELD_RETURN())

DT_RETURN_MARK_DECL(ToReflectedField, jobject
                    , HOTSPOT_JNI_TOREFLECTEDFIELD_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_ToReflectedField(JNIEnv *env, jclass cls, jfieldID fieldID, jboolean isStatic))
  HOTSPOT_JNI_TOREFLECTEDFIELD_ENTRY(env, cls, (uintptr_t) fieldID, isStatic);
  jobject ret = NULL;
  DT_RETURN_MARK(ToReflectedField, jobject, (const jobject&)ret);

  fieldDescriptor fd;
  bool found = false;
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));

  assert(jfieldIDWorkaround::is_static_jfieldID(fieldID) == (isStatic != 0), "invalid fieldID");

  if (isStatic) {
    // Static field. The fieldID a JNIid specifying the field holder and the offset within the Klass*.
    JNIid* id = jfieldIDWorkaround::from_static_jfieldID(fieldID);
    assert(id->is_static_field_id(), "invalid static field id");
    found = id->find_local_field(&fd);
  } else {
    // Non-static field. The fieldID is really the offset of the field within the instanceOop.
    int offset = jfieldIDWorkaround::from_instance_jfieldID(k, fieldID);
    found = InstanceKlass::cast(k)->find_field_from_offset(offset, false, &fd);
  }
  assert(found, "bad fieldID passed into jni_ToReflectedField");
  oop reflected = Reflection::new_field(&fd, CHECK_NULL);
  ret = JNIHandles::make_local(THREAD, reflected);
  return ret;
JNI_END


//
// Accessing Static Fields
//
DT_RETURN_MARK_DECL(GetStaticFieldID, jfieldID
                    , HOTSPOT_JNI_GETSTATICFIELDID_RETURN((uintptr_t)_ret_ref));

JNI_ENTRY(jfieldID, jni_GetStaticFieldID(JNIEnv *env, jclass clazz,
          const char *name, const char *sig))
  HOTSPOT_JNI_GETSTATICFIELDID_ENTRY(env, clazz, (char *) name, (char *) sig);
  jfieldID ret = NULL;
  DT_RETURN_MARK(GetStaticFieldID, jfieldID, (const jfieldID&)ret);

  // The class should have been loaded (we have an instance of the class
  // passed in) so the field and signature should already be in the symbol
  // table.  If they're not there, the field doesn't exist.
  TempNewSymbol fieldname = SymbolTable::probe(name, (int)strlen(name));
  TempNewSymbol signame = SymbolTable::probe(sig, (int)strlen(sig));
  if (fieldname == NULL || signame == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_NoSuchFieldError(), (char*) name);
  }
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz));
  // Make sure class is initialized before handing id's out to static fields
  k->initialize(CHECK_NULL);

  fieldDescriptor fd;
  if (!k->is_instance_klass() ||
      !InstanceKlass::cast(k)->find_field(fieldname, signame, true, &fd)) {
    THROW_MSG_0(vmSymbols::java_lang_NoSuchFieldError(), (char*) name);
  }

  // A jfieldID for a static field is a JNIid specifying the field holder and the offset within the Klass*
  JNIid* id = fd.field_holder()->jni_id_for(fd.offset());
  debug_only(id->set_is_static_field_id();)

  debug_only(id->verify(fd.field_holder()));

  ret = jfieldIDWorkaround::to_static_jfieldID(id);
  return ret;
JNI_END


JNI_ENTRY(jobject, jni_GetStaticObjectField(JNIEnv *env, jclass clazz, jfieldID fieldID))
  HOTSPOT_JNI_GETSTATICOBJECTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID);
#if INCLUDE_JNI_CHECK
  DEBUG_ONLY(Klass* param_k = jniCheck::validate_class(thread, clazz);)
#endif // INCLUDE_JNI_CHECK
  JNIid* id = jfieldIDWorkaround::from_static_jfieldID(fieldID);
  assert(id->is_static_field_id(), "invalid static field id");
  // Keep JVMTI addition small and only check enabled flag here.
  // jni_GetField_probe() assumes that is okay to create handles.
  if (JvmtiExport::should_post_field_access()) {
    JvmtiExport::jni_GetField_probe(thread, NULL, NULL, id->holder(), fieldID, true);
  }
  jobject ret = JNIHandles::make_local(THREAD, id->holder()->java_mirror()->obj_field(id->offset()));
  HOTSPOT_JNI_GETSTATICOBJECTFIELD_RETURN(ret);
  return ret;
JNI_END


#define DEFINE_GETSTATICFIELD(Return,Fieldname,Result \
                              , EntryProbe, ReturnProbe) \
\
  DT_RETURN_MARK_DECL_FOR(Result, GetStatic##Result##Field, Return \
                          , ReturnProbe);                                          \
\
JNI_ENTRY(Return, jni_GetStatic##Result##Field(JNIEnv *env, jclass clazz, jfieldID fieldID)) \
  EntryProbe; \
  Return ret = 0;\
  DT_RETURN_MARK_FOR(Result, GetStatic##Result##Field, Return, \
                     (const Return&)ret);\
  JNIid* id = jfieldIDWorkaround::from_static_jfieldID(fieldID); \
  assert(id->is_static_field_id(), "invalid static field id"); \
  /* Keep JVMTI addition small and only check enabled flag here. */ \
  /* jni_GetField_probe() assumes that is okay to create handles. */ \
  if (JvmtiExport::should_post_field_access()) { \
    JvmtiExport::jni_GetField_probe(thread, NULL, NULL, id->holder(), fieldID, true); \
  } \
  ret = id->holder()->java_mirror()-> Fieldname##_field (id->offset()); \
  return ret;\
JNI_END

DEFINE_GETSTATICFIELD(jboolean, bool,   Boolean
                      , HOTSPOT_JNI_GETSTATICBOOLEANFIELD_ENTRY(env, clazz, (uintptr_t) fieldID), HOTSPOT_JNI_GETSTATICBOOLEANFIELD_RETURN(_ret_ref))
DEFINE_GETSTATICFIELD(jbyte,    byte,   Byte
                      , HOTSPOT_JNI_GETSTATICBYTEFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),    HOTSPOT_JNI_GETSTATICBYTEFIELD_RETURN(_ret_ref)   )
DEFINE_GETSTATICFIELD(jchar,    char,   Char
                      , HOTSPOT_JNI_GETSTATICCHARFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),    HOTSPOT_JNI_GETSTATICCHARFIELD_RETURN(_ret_ref)   )
DEFINE_GETSTATICFIELD(jshort,   short,  Short
                      , HOTSPOT_JNI_GETSTATICSHORTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),   HOTSPOT_JNI_GETSTATICSHORTFIELD_RETURN(_ret_ref)  )
DEFINE_GETSTATICFIELD(jint,     int,    Int
                      , HOTSPOT_JNI_GETSTATICINTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),     HOTSPOT_JNI_GETSTATICINTFIELD_RETURN(_ret_ref)    )
DEFINE_GETSTATICFIELD(jlong,    long,   Long
                      , HOTSPOT_JNI_GETSTATICLONGFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),    HOTSPOT_JNI_GETSTATICLONGFIELD_RETURN(_ret_ref)   )
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_GETSTATICFIELD(jfloat,   float,  Float
                      , HOTSPOT_JNI_GETSTATICFLOATFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),   HOTSPOT_JNI_GETSTATICFLOATFIELD_RETURN()          )
DEFINE_GETSTATICFIELD(jdouble,  double, Double
                      , HOTSPOT_JNI_GETSTATICDOUBLEFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),  HOTSPOT_JNI_GETSTATICDOUBLEFIELD_RETURN()         )

JNI_ENTRY(void, jni_SetStaticObjectField(JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value))
 HOTSPOT_JNI_SETSTATICOBJECTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value);
  JNIid* id = jfieldIDWorkaround::from_static_jfieldID(fieldID);
  assert(id->is_static_field_id(), "invalid static field id");
  // Keep JVMTI addition small and only check enabled flag here.
  // jni_SetField_probe() assumes that is okay to create handles.
  if (JvmtiExport::should_post_field_modification()) {
    jvalue field_value;
    field_value.l = value;
    JvmtiExport::jni_SetField_probe(thread, NULL, NULL, id->holder(), fieldID, true, JVM_SIGNATURE_CLASS, (jvalue *)&field_value);
  }
  id->holder()->java_mirror()->obj_field_put(id->offset(), JNIHandles::resolve(value));
  HOTSPOT_JNI_SETSTATICOBJECTFIELD_RETURN();
JNI_END



#define DEFINE_SETSTATICFIELD(Argument,Fieldname,Result,SigType,unionType \
                              , EntryProbe, ReturnProbe) \
\
JNI_ENTRY(void, jni_SetStatic##Result##Field(JNIEnv *env, jclass clazz, jfieldID fieldID, Argument value)) \
  EntryProbe; \
\
  JNIid* id = jfieldIDWorkaround::from_static_jfieldID(fieldID); \
  assert(id->is_static_field_id(), "invalid static field id"); \
  /* Keep JVMTI addition small and only check enabled flag here. */ \
  /* jni_SetField_probe() assumes that is okay to create handles. */ \
  if (JvmtiExport::should_post_field_modification()) { \
    jvalue field_value; \
    field_value.unionType = value; \
    JvmtiExport::jni_SetField_probe(thread, NULL, NULL, id->holder(), fieldID, true, SigType, (jvalue *)&field_value); \
  } \
  if (SigType == JVM_SIGNATURE_BOOLEAN) { value = ((jboolean)value) & 1; } \
  id->holder()->java_mirror()-> Fieldname##_field_put (id->offset(), value); \
  ReturnProbe;\
JNI_END

DEFINE_SETSTATICFIELD(jboolean, bool,   Boolean, JVM_SIGNATURE_BOOLEAN, z
                      , HOTSPOT_JNI_SETSTATICBOOLEANFIELD_ENTRY(env, clazz, (uintptr_t)fieldID, value),
                      HOTSPOT_JNI_SETSTATICBOOLEANFIELD_RETURN())
DEFINE_SETSTATICFIELD(jbyte,    byte,   Byte,    JVM_SIGNATURE_BYTE, b
                      , HOTSPOT_JNI_SETSTATICBYTEFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value),
                      HOTSPOT_JNI_SETSTATICBYTEFIELD_RETURN())
DEFINE_SETSTATICFIELD(jchar,    char,   Char,    JVM_SIGNATURE_CHAR, c
                      , HOTSPOT_JNI_SETSTATICCHARFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value),
                      HOTSPOT_JNI_SETSTATICCHARFIELD_RETURN())
DEFINE_SETSTATICFIELD(jshort,   short,  Short,   JVM_SIGNATURE_SHORT, s
                      , HOTSPOT_JNI_SETSTATICSHORTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value),
                      HOTSPOT_JNI_SETSTATICSHORTFIELD_RETURN())
DEFINE_SETSTATICFIELD(jint,     int,    Int,     JVM_SIGNATURE_INT, i
                      , HOTSPOT_JNI_SETSTATICINTFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value),
                      HOTSPOT_JNI_SETSTATICINTFIELD_RETURN())
DEFINE_SETSTATICFIELD(jlong,    long,   Long,    JVM_SIGNATURE_LONG, j
                      , HOTSPOT_JNI_SETSTATICLONGFIELD_ENTRY(env, clazz, (uintptr_t) fieldID, value),
                      HOTSPOT_JNI_SETSTATICLONGFIELD_RETURN())
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_SETSTATICFIELD(jfloat,   float,  Float,   JVM_SIGNATURE_FLOAT, f
                      , HOTSPOT_JNI_SETSTATICFLOATFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),
                      HOTSPOT_JNI_SETSTATICFLOATFIELD_RETURN())
DEFINE_SETSTATICFIELD(jdouble,  double, Double,  JVM_SIGNATURE_DOUBLE, d
                      , HOTSPOT_JNI_SETSTATICDOUBLEFIELD_ENTRY(env, clazz, (uintptr_t) fieldID),
                      HOTSPOT_JNI_SETSTATICDOUBLEFIELD_RETURN())

//
// String Operations
//

// Unicode Interface

DT_RETURN_MARK_DECL(NewString, jstring
                    , HOTSPOT_JNI_NEWSTRING_RETURN(_ret_ref));

JNI_ENTRY(jstring, jni_NewString(JNIEnv *env, const jchar *unicodeChars, jsize len))
 HOTSPOT_JNI_NEWSTRING_ENTRY(env, (uint16_t *) unicodeChars, len);
  jstring ret = NULL;
  DT_RETURN_MARK(NewString, jstring, (const jstring&)ret);
  oop string=java_lang_String::create_oop_from_unicode((jchar*) unicodeChars, len, CHECK_NULL);
  ret = (jstring) JNIHandles::make_local(THREAD, string);
  return ret;
JNI_END


JNI_ENTRY_NO_PRESERVE(jsize, jni_GetStringLength(JNIEnv *env, jstring string))
  HOTSPOT_JNI_GETSTRINGLENGTH_ENTRY(env, string);
  jsize ret = 0;
  oop s = JNIHandles::resolve_non_null(string);
  ret = java_lang_String::length(s);
 HOTSPOT_JNI_GETSTRINGLENGTH_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY_NO_PRESERVE(const jchar*, jni_GetStringChars(
  JNIEnv *env, jstring string, jboolean *isCopy))
 HOTSPOT_JNI_GETSTRINGCHARS_ENTRY(env, string, (uintptr_t *) isCopy);
  jchar* buf = NULL;
  oop s = JNIHandles::resolve_non_null(string);
  typeArrayOop s_value = java_lang_String::value(s);
  if (s_value != NULL) {
    int s_len = java_lang_String::length(s, s_value);
    bool is_latin1 = java_lang_String::is_latin1(s);
    buf = NEW_C_HEAP_ARRAY_RETURN_NULL(jchar, s_len + 1, mtInternal);  // add one for zero termination
    /* JNI Specification states return NULL on OOM */
    if (buf != NULL) {
      if (s_len > 0) {
        if (!is_latin1) {
          ArrayAccess<>::arraycopy_to_native(s_value, (size_t) typeArrayOopDesc::element_offset<jchar>(0),
                                             buf, s_len);
        } else {
          for (int i = 0; i < s_len; i++) {
            buf[i] = ((jchar) s_value->byte_at(i)) & 0xff;
          }
        }
      }
      buf[s_len] = 0;
      //%note jni_5
      if (isCopy != NULL) {
        *isCopy = JNI_TRUE;
      }
    }
  }
  HOTSPOT_JNI_GETSTRINGCHARS_RETURN(buf);
  return buf;
JNI_END


JNI_ENTRY_NO_PRESERVE(void, jni_ReleaseStringChars(JNIEnv *env, jstring str, const jchar *chars))
  HOTSPOT_JNI_RELEASESTRINGCHARS_ENTRY(env, str, (uint16_t *) chars);
  //%note jni_6
  if (chars != NULL) {
    // Since String objects are supposed to be immutable, don't copy any
    // new data back.  A bad user will have to go after the char array.
    FreeHeap((void*) chars);
  }
  HOTSPOT_JNI_RELEASESTRINGCHARS_RETURN();
JNI_END


// UTF Interface

DT_RETURN_MARK_DECL(NewStringUTF, jstring
                    , HOTSPOT_JNI_NEWSTRINGUTF_RETURN(_ret_ref));

JNI_ENTRY(jstring, jni_NewStringUTF(JNIEnv *env, const char *bytes))
  HOTSPOT_JNI_NEWSTRINGUTF_ENTRY(env, (char *) bytes);
  jstring ret;
  DT_RETURN_MARK(NewStringUTF, jstring, (const jstring&)ret);

  oop result = java_lang_String::create_oop_from_str((char*) bytes, CHECK_NULL);
  ret = (jstring) JNIHandles::make_local(THREAD, result);
  return ret;
JNI_END


JNI_ENTRY(jsize, jni_GetStringUTFLength(JNIEnv *env, jstring string))
 HOTSPOT_JNI_GETSTRINGUTFLENGTH_ENTRY(env, string);
  oop java_string = JNIHandles::resolve_non_null(string);
  jsize ret = java_lang_String::utf8_length(java_string);
  HOTSPOT_JNI_GETSTRINGUTFLENGTH_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY(const char*, jni_GetStringUTFChars(JNIEnv *env, jstring string, jboolean *isCopy))
 HOTSPOT_JNI_GETSTRINGUTFCHARS_ENTRY(env, string, (uintptr_t *) isCopy);
  char* result = NULL;
  oop java_string = JNIHandles::resolve_non_null(string);
  typeArrayOop s_value = java_lang_String::value(java_string);
  if (s_value != NULL) {
    size_t length = java_lang_String::utf8_length(java_string, s_value);
    /* JNI Specification states return NULL on OOM */
    result = AllocateHeap(length + 1, mtInternal, 0, AllocFailStrategy::RETURN_NULL);
    if (result != NULL) {
      java_lang_String::as_utf8_string(java_string, s_value, result, (int) length + 1);
      if (isCopy != NULL) {
        *isCopy = JNI_TRUE;
      }
    }
  }
 HOTSPOT_JNI_GETSTRINGUTFCHARS_RETURN(result);
  return result;
JNI_END


JNI_LEAF(void, jni_ReleaseStringUTFChars(JNIEnv *env, jstring str, const char *chars))
 HOTSPOT_JNI_RELEASESTRINGUTFCHARS_ENTRY(env, str, (char *) chars);
  if (chars != NULL) {
    FreeHeap((char*) chars);
  }
HOTSPOT_JNI_RELEASESTRINGUTFCHARS_RETURN();
JNI_END


JNI_ENTRY_NO_PRESERVE(jsize, jni_GetArrayLength(JNIEnv *env, jarray array))
 HOTSPOT_JNI_GETARRAYLENGTH_ENTRY(env, array);
  arrayOop a = arrayOop(JNIHandles::resolve_non_null(array));
  assert(a->is_array(), "must be array");
  jsize ret = a->length();
 HOTSPOT_JNI_GETARRAYLENGTH_RETURN(ret);
  return ret;
JNI_END


//
// Object Array Operations
//

DT_RETURN_MARK_DECL(NewObjectArray, jobjectArray
                    , HOTSPOT_JNI_NEWOBJECTARRAY_RETURN(_ret_ref));

JNI_ENTRY(jobjectArray, jni_NewObjectArray(JNIEnv *env, jsize length, jclass elementClass, jobject initialElement))
 HOTSPOT_JNI_NEWOBJECTARRAY_ENTRY(env, length, elementClass, initialElement);
  jobjectArray ret = NULL;
  DT_RETURN_MARK(NewObjectArray, jobjectArray, (const jobjectArray&)ret);
  Klass* ek = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(elementClass));
  Klass* ak = ek->array_klass(CHECK_NULL);
  ObjArrayKlass::cast(ak)->initialize(CHECK_NULL);
  objArrayOop result = ObjArrayKlass::cast(ak)->allocate(length, CHECK_NULL);
  oop initial_value = JNIHandles::resolve(initialElement);
  if (initial_value != NULL) {  // array already initialized with NULL
    for (int index = 0; index < length; index++) {
      result->obj_at_put(index, initial_value);
    }
  }
  ret = (jobjectArray) JNIHandles::make_local(THREAD, result);
  return ret;
JNI_END

DT_RETURN_MARK_DECL(GetObjectArrayElement, jobject
                    , HOTSPOT_JNI_GETOBJECTARRAYELEMENT_RETURN(_ret_ref));

JNI_ENTRY(jobject, jni_GetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index))
 HOTSPOT_JNI_GETOBJECTARRAYELEMENT_ENTRY(env, array, index);
  jobject ret = NULL;
  DT_RETURN_MARK(GetObjectArrayElement, jobject, (const jobject&)ret);
  objArrayOop a = objArrayOop(JNIHandles::resolve_non_null(array));
  if (a->is_within_bounds(index)) {
    ret = JNIHandles::make_local(THREAD, a->obj_at(index));
    return ret;
  } else {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Index %d out of bounds for length %d", index, a->length());
    THROW_MSG_0(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
JNI_END

DT_VOID_RETURN_MARK_DECL(SetObjectArrayElement
                         , HOTSPOT_JNI_SETOBJECTARRAYELEMENT_RETURN());

JNI_ENTRY(void, jni_SetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index, jobject value))
 HOTSPOT_JNI_SETOBJECTARRAYELEMENT_ENTRY(env, array, index, value);
  DT_VOID_RETURN_MARK(SetObjectArrayElement);

  objArrayOop a = objArrayOop(JNIHandles::resolve_non_null(array));
  oop v = JNIHandles::resolve(value);
  if (a->is_within_bounds(index)) {
    if (v == NULL || v->is_a(ObjArrayKlass::cast(a->klass())->element_klass())) {
      a->obj_at_put(index, v);
    } else {
      ResourceMark rm(THREAD);
      stringStream ss;
      Klass *bottom_kl = ObjArrayKlass::cast(a->klass())->bottom_klass();
      ss.print("type mismatch: can not store %s to %s[%d]",
               v->klass()->external_name(),
               bottom_kl->is_typeArray_klass() ? type2name_tab[ArrayKlass::cast(bottom_kl)->element_type()] : bottom_kl->external_name(),
               index);
      for (int dims = ArrayKlass::cast(a->klass())->dimension(); dims > 1; --dims) {
        ss.print("[]");
      }
      THROW_MSG(vmSymbols::java_lang_ArrayStoreException(), ss.as_string());
    }
  } else {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Index %d out of bounds for length %d", index, a->length());
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
JNI_END



#define DEFINE_NEWSCALARARRAY(Return,Allocator,Result \
                              ,EntryProbe,ReturnProbe)  \
\
  DT_RETURN_MARK_DECL(New##Result##Array, Return \
                      , ReturnProbe); \
\
JNI_ENTRY(Return, \
          jni_New##Result##Array(JNIEnv *env, jsize len)) \
  EntryProbe; \
  Return ret = NULL;\
  DT_RETURN_MARK(New##Result##Array, Return, (const Return&)ret);\
\
  oop obj= oopFactory::Allocator(len, CHECK_NULL); \
  ret = (Return) JNIHandles::make_local(THREAD, obj); \
  return ret;\
JNI_END

DEFINE_NEWSCALARARRAY(jbooleanArray, new_boolArray,   Boolean,
                      HOTSPOT_JNI_NEWBOOLEANARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWBOOLEANARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jbyteArray,    new_byteArray,   Byte,
                      HOTSPOT_JNI_NEWBYTEARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWBYTEARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jshortArray,   new_shortArray,  Short,
                      HOTSPOT_JNI_NEWSHORTARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWSHORTARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jcharArray,    new_charArray,   Char,
                      HOTSPOT_JNI_NEWCHARARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWCHARARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jintArray,     new_intArray,    Int,
                      HOTSPOT_JNI_NEWINTARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWINTARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jlongArray,    new_longArray,   Long,
                      HOTSPOT_JNI_NEWLONGARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWLONGARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jfloatArray,   new_floatArray,  Float,
                      HOTSPOT_JNI_NEWFLOATARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWFLOATARRAY_RETURN(_ret_ref))
DEFINE_NEWSCALARARRAY(jdoubleArray,  new_doubleArray, Double,
                      HOTSPOT_JNI_NEWDOUBLEARRAY_ENTRY(env, len),
                      HOTSPOT_JNI_NEWDOUBLEARRAY_RETURN(_ret_ref))

// Return an address which will fault if the caller writes to it.

static char* get_bad_address() {
  static char* bad_address = NULL;
  if (bad_address == NULL) {
    size_t size = os::vm_allocation_granularity();
    bad_address = os::reserve_memory(size);
    if (bad_address != NULL) {
      os::protect_memory(bad_address, size, os::MEM_PROT_READ,
                         /*is_committed*/false);
      MemTracker::record_virtual_memory_type((void*)bad_address, mtInternal);
    }
  }
  return bad_address;
}



#define DEFINE_GETSCALARARRAYELEMENTS(ElementTag,ElementType,Result, Tag \
                                      , EntryProbe, ReturnProbe) \
\
JNI_ENTRY_NO_PRESERVE(ElementType*, \
          jni_Get##Result##ArrayElements(JNIEnv *env, ElementType##Array array, jboolean *isCopy)) \
  EntryProbe; \
  /* allocate an chunk of memory in c land */ \
  typeArrayOop a = typeArrayOop(JNIHandles::resolve_non_null(array)); \
  ElementType* result; \
  int len = a->length(); \
  if (len == 0) { \
    if (isCopy != NULL) { \
      *isCopy = JNI_FALSE; \
    } \
    /* Empty array: legal but useless, can't return NULL. \
     * Return a pointer to something useless. \
     * Avoid asserts in typeArrayOop. */ \
    result = (ElementType*)get_bad_address(); \
  } else { \
    /* JNI Specification states return NULL on OOM */                    \
    result = NEW_C_HEAP_ARRAY_RETURN_NULL(ElementType, len, mtInternal); \
    if (result != NULL) {                                                \
      /* copy the array to the c chunk */                                \
      ArrayAccess<>::arraycopy_to_native(a, typeArrayOopDesc::element_offset<ElementType>(0), \
                                         result, len);                   \
      if (isCopy) {                                                      \
        *isCopy = JNI_TRUE;                                              \
      }                                                                  \
    }                                                                    \
  } \
  ReturnProbe; \
  return result; \
JNI_END

DEFINE_GETSCALARARRAYELEMENTS(T_BOOLEAN, jboolean, Boolean, bool
                              , HOTSPOT_JNI_GETBOOLEANARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETBOOLEANARRAYELEMENTS_RETURN((uintptr_t*)result))
DEFINE_GETSCALARARRAYELEMENTS(T_BYTE,    jbyte,    Byte,    byte
                              , HOTSPOT_JNI_GETBYTEARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETBYTEARRAYELEMENTS_RETURN((char*)result))
DEFINE_GETSCALARARRAYELEMENTS(T_SHORT,   jshort,   Short,   short
                              , HOTSPOT_JNI_GETSHORTARRAYELEMENTS_ENTRY(env, (uint16_t*) array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETSHORTARRAYELEMENTS_RETURN((uint16_t*)result))
DEFINE_GETSCALARARRAYELEMENTS(T_CHAR,    jchar,    Char,    char
                              , HOTSPOT_JNI_GETCHARARRAYELEMENTS_ENTRY(env, (uint16_t*) array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETCHARARRAYELEMENTS_RETURN(result))
DEFINE_GETSCALARARRAYELEMENTS(T_INT,     jint,     Int,     int
                              , HOTSPOT_JNI_GETINTARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETINTARRAYELEMENTS_RETURN((uint32_t*)result))
DEFINE_GETSCALARARRAYELEMENTS(T_LONG,    jlong,    Long,    long
                              , HOTSPOT_JNI_GETLONGARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETLONGARRAYELEMENTS_RETURN(((uintptr_t*)result)))
// Float and double probes don't return value because dtrace doesn't currently support it
DEFINE_GETSCALARARRAYELEMENTS(T_FLOAT,   jfloat,   Float,   float
                              , HOTSPOT_JNI_GETFLOATARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETFLOATARRAYELEMENTS_RETURN(result))
DEFINE_GETSCALARARRAYELEMENTS(T_DOUBLE,  jdouble,  Double,  double
                              , HOTSPOT_JNI_GETDOUBLEARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) isCopy),
                              HOTSPOT_JNI_GETDOUBLEARRAYELEMENTS_RETURN(result))


#define DEFINE_RELEASESCALARARRAYELEMENTS(ElementTag,ElementType,Result,Tag \
                                          , EntryProbe, ReturnProbe);\
\
JNI_ENTRY_NO_PRESERVE(void, \
          jni_Release##Result##ArrayElements(JNIEnv *env, ElementType##Array array, \
                                             ElementType *buf, jint mode)) \
  EntryProbe; \
  typeArrayOop a = typeArrayOop(JNIHandles::resolve_non_null(array)); \
  int len = a->length(); \
  if (len != 0) {   /* Empty array:  nothing to free or copy. */  \
    if ((mode == 0) || (mode == JNI_COMMIT)) { \
      ArrayAccess<>::arraycopy_from_native(buf, a, typeArrayOopDesc::element_offset<ElementType>(0), len); \
    } \
    if ((mode == 0) || (mode == JNI_ABORT)) { \
      FreeHeap(buf); \
    } \
  } \
  ReturnProbe; \
JNI_END

DEFINE_RELEASESCALARARRAYELEMENTS(T_BOOLEAN, jboolean, Boolean, bool
                                  , HOTSPOT_JNI_RELEASEBOOLEANARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) buf, mode),
                                  HOTSPOT_JNI_RELEASEBOOLEANARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_BYTE,    jbyte,    Byte,    byte
                                  , HOTSPOT_JNI_RELEASEBYTEARRAYELEMENTS_ENTRY(env, array, (char *) buf, mode),
                                  HOTSPOT_JNI_RELEASEBYTEARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_SHORT,   jshort,   Short,   short
                                  ,  HOTSPOT_JNI_RELEASESHORTARRAYELEMENTS_ENTRY(env, array, (uint16_t *) buf, mode),
                                  HOTSPOT_JNI_RELEASESHORTARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_CHAR,    jchar,    Char,    char
                                  ,  HOTSPOT_JNI_RELEASECHARARRAYELEMENTS_ENTRY(env, array, (uint16_t *) buf, mode),
                                  HOTSPOT_JNI_RELEASECHARARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_INT,     jint,     Int,     int
                                  , HOTSPOT_JNI_RELEASEINTARRAYELEMENTS_ENTRY(env, array, (uint32_t *) buf, mode),
                                  HOTSPOT_JNI_RELEASEINTARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_LONG,    jlong,    Long,    long
                                  , HOTSPOT_JNI_RELEASELONGARRAYELEMENTS_ENTRY(env, array, (uintptr_t *) buf, mode),
                                  HOTSPOT_JNI_RELEASELONGARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_FLOAT,   jfloat,   Float,   float
                                  , HOTSPOT_JNI_RELEASEFLOATARRAYELEMENTS_ENTRY(env, array, (float *) buf, mode),
                                  HOTSPOT_JNI_RELEASEFLOATARRAYELEMENTS_RETURN())
DEFINE_RELEASESCALARARRAYELEMENTS(T_DOUBLE,  jdouble,  Double,  double
                                  , HOTSPOT_JNI_RELEASEDOUBLEARRAYELEMENTS_ENTRY(env, array, (double *) buf, mode),
                                  HOTSPOT_JNI_RELEASEDOUBLEARRAYELEMENTS_RETURN())

static void check_bounds(jsize start, jsize copy_len, jsize array_len, TRAPS) {
  ResourceMark rm(THREAD);
  if (copy_len < 0) {
    stringStream ss;
    ss.print("Length %d is negative", copy_len);
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  } else if (start < 0 || (start > array_len - copy_len)) {
    stringStream ss;
    ss.print("Array region %d.." INT64_FORMAT " out of bounds for length %d",
             start, (int64_t)start+(int64_t)copy_len, array_len);
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
}

#define DEFINE_GETSCALARARRAYREGION(ElementTag,ElementType,Result, Tag \
                                    , EntryProbe, ReturnProbe); \
  DT_VOID_RETURN_MARK_DECL(Get##Result##ArrayRegion \
                           , ReturnProbe); \
\
JNI_ENTRY(void, \
jni_Get##Result##ArrayRegion(JNIEnv *env, ElementType##Array array, jsize start, \
             jsize len, ElementType *buf)) \
  EntryProbe; \
  DT_VOID_RETURN_MARK(Get##Result##ArrayRegion); \
  typeArrayOop src = typeArrayOop(JNIHandles::resolve_non_null(array)); \
  check_bounds(start, len, src->length(), CHECK); \
  if (len > 0) {    \
    ArrayAccess<>::arraycopy_to_native(src, typeArrayOopDesc::element_offset<ElementType>(start), buf, len); \
  } \
JNI_END

DEFINE_GETSCALARARRAYREGION(T_BOOLEAN, jboolean,Boolean, bool
                            , HOTSPOT_JNI_GETBOOLEANARRAYREGION_ENTRY(env, array, start, len, (uintptr_t *) buf),
                            HOTSPOT_JNI_GETBOOLEANARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_BYTE,    jbyte,   Byte,    byte
                            ,  HOTSPOT_JNI_GETBYTEARRAYREGION_ENTRY(env, array, start, len, (char *) buf),
                            HOTSPOT_JNI_GETBYTEARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_SHORT,   jshort,  Short,   short
                            , HOTSPOT_JNI_GETSHORTARRAYREGION_ENTRY(env, array, start, len, (uint16_t *) buf),
                            HOTSPOT_JNI_GETSHORTARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_CHAR,    jchar,   Char,    char
                            ,  HOTSPOT_JNI_GETCHARARRAYREGION_ENTRY(env, array, start, len, (uint16_t*) buf),
                            HOTSPOT_JNI_GETCHARARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_INT,     jint,    Int,     int
                            , HOTSPOT_JNI_GETINTARRAYREGION_ENTRY(env, array, start, len, (uint32_t*) buf),
                            HOTSPOT_JNI_GETINTARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_LONG,    jlong,   Long,    long
                            ,  HOTSPOT_JNI_GETLONGARRAYREGION_ENTRY(env, array, start, len, (uintptr_t *) buf),
                            HOTSPOT_JNI_GETLONGARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_FLOAT,   jfloat,  Float,   float
                            , HOTSPOT_JNI_GETFLOATARRAYREGION_ENTRY(env, array, start, len, (float *) buf),
                            HOTSPOT_JNI_GETFLOATARRAYREGION_RETURN());
DEFINE_GETSCALARARRAYREGION(T_DOUBLE,  jdouble, Double,  double
                            , HOTSPOT_JNI_GETDOUBLEARRAYREGION_ENTRY(env, array, start, len, (double *) buf),
                            HOTSPOT_JNI_GETDOUBLEARRAYREGION_RETURN());


#define DEFINE_SETSCALARARRAYREGION(ElementTag,ElementType,Result, Tag \
                                    , EntryProbe, ReturnProbe); \
  DT_VOID_RETURN_MARK_DECL(Set##Result##ArrayRegion \
                           ,ReturnProbe);           \
\
JNI_ENTRY(void, \
jni_Set##Result##ArrayRegion(JNIEnv *env, ElementType##Array array, jsize start, \
             jsize len, const ElementType *buf)) \
  EntryProbe; \
  DT_VOID_RETURN_MARK(Set##Result##ArrayRegion); \
  typeArrayOop dst = typeArrayOop(JNIHandles::resolve_non_null(array)); \
  check_bounds(start, len, dst->length(), CHECK); \
  if (len > 0) { \
    ArrayAccess<>::arraycopy_from_native(buf, dst, typeArrayOopDesc::element_offset<ElementType>(start), len); \
  } \
JNI_END

DEFINE_SETSCALARARRAYREGION(T_BOOLEAN, jboolean, Boolean, bool
                            , HOTSPOT_JNI_SETBOOLEANARRAYREGION_ENTRY(env, array, start, len, (uintptr_t *)buf),
                            HOTSPOT_JNI_SETBOOLEANARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_BYTE,    jbyte,    Byte,    byte
                            , HOTSPOT_JNI_SETBYTEARRAYREGION_ENTRY(env, array, start, len, (char *) buf),
                            HOTSPOT_JNI_SETBYTEARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_SHORT,   jshort,   Short,   short
                            , HOTSPOT_JNI_SETSHORTARRAYREGION_ENTRY(env, array, start, len, (uint16_t *) buf),
                            HOTSPOT_JNI_SETSHORTARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_CHAR,    jchar,    Char,    char
                            , HOTSPOT_JNI_SETCHARARRAYREGION_ENTRY(env, array, start, len, (uint16_t *) buf),
                            HOTSPOT_JNI_SETCHARARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_INT,     jint,     Int,     int
                            , HOTSPOT_JNI_SETINTARRAYREGION_ENTRY(env, array, start, len, (uint32_t *) buf),
                            HOTSPOT_JNI_SETINTARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_LONG,    jlong,    Long,    long
                            , HOTSPOT_JNI_SETLONGARRAYREGION_ENTRY(env, array, start, len, (uintptr_t *) buf),
                            HOTSPOT_JNI_SETLONGARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_FLOAT,   jfloat,   Float,   float
                            , HOTSPOT_JNI_SETFLOATARRAYREGION_ENTRY(env, array, start, len, (float *) buf),
                            HOTSPOT_JNI_SETFLOATARRAYREGION_RETURN())
DEFINE_SETSCALARARRAYREGION(T_DOUBLE,  jdouble,  Double,  double
                            , HOTSPOT_JNI_SETDOUBLEARRAYREGION_ENTRY(env, array, start, len, (double *) buf),
                            HOTSPOT_JNI_SETDOUBLEARRAYREGION_RETURN())


DT_RETURN_MARK_DECL(RegisterNatives, jint
                    , HOTSPOT_JNI_REGISTERNATIVES_RETURN(_ret_ref));

JNI_ENTRY(jint, jni_RegisterNatives(JNIEnv *env, jclass clazz,
                                    const JNINativeMethod *methods,
                                    jint nMethods))
  HOTSPOT_JNI_REGISTERNATIVES_ENTRY(env, clazz, (void *) methods, nMethods);
  jint ret = 0;
  DT_RETURN_MARK(RegisterNatives, jint, (const jint&)ret);

  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz));

  // There are no restrictions on native code registering native methods,
  // which allows agents to redefine the bindings to native methods, however
  // we issue a warning if any code running outside of the boot/platform
  // loader is rebinding any native methods in classes loaded by the
  // boot/platform loader that are in named modules. That will catch changes
  // to platform classes while excluding classes added to the bootclasspath.
  bool do_warning = false;

  // Only instanceKlasses can have native methods
  if (k->is_instance_klass()) {
    oop cl = k->class_loader();
    InstanceKlass* ik = InstanceKlass::cast(k);
    // Check for a platform class
    if ((cl ==  NULL || SystemDictionary::is_platform_class_loader(cl)) &&
        ik->module()->is_named()) {
      Klass* caller = thread->security_get_caller_class(1);
      // If no caller class, or caller class has a different loader, then
      // issue a warning below.
      do_warning = (caller == NULL) || caller->class_loader() != cl;
    }
  }


  for (int index = 0; index < nMethods; index++) {
    const char* meth_name = methods[index].name;
    const char* meth_sig = methods[index].signature;
    int meth_name_len = (int)strlen(meth_name);

    // The class should have been loaded (we have an instance of the class
    // passed in) so the method and signature should already be in the symbol
    // table.  If they're not there, the method doesn't exist.
    TempNewSymbol  name = SymbolTable::probe(meth_name, meth_name_len);
    TempNewSymbol  signature = SymbolTable::probe(meth_sig, (int)strlen(meth_sig));

    if (name == NULL || signature == NULL) {
      ResourceMark rm(THREAD);
      stringStream st;
      st.print("Method %s.%s%s not found", k->external_name(), meth_name, meth_sig);
      // Must return negative value on failure
      THROW_MSG_(vmSymbols::java_lang_NoSuchMethodError(), st.as_string(), -1);
    }

    if (do_warning) {
      ResourceMark rm(THREAD);
      log_warning(jni, resolve)("Re-registering of platform native method: %s.%s%s "
              "from code in a different classloader", k->external_name(), meth_name, meth_sig);
    }

    bool res = Method::register_native(k, name, signature,
                                       (address) methods[index].fnPtr, THREAD);
    if (!res) {
      ret = -1;
      break;
    }
  }
  return ret;
JNI_END


JNI_ENTRY(jint, jni_UnregisterNatives(JNIEnv *env, jclass clazz))
 HOTSPOT_JNI_UNREGISTERNATIVES_ENTRY(env, clazz);
  Klass* k   = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz));
  //%note jni_2
  if (k->is_instance_klass()) {
    for (int index = 0; index < InstanceKlass::cast(k)->methods()->length(); index++) {
      Method* m = InstanceKlass::cast(k)->methods()->at(index);
      if (m->is_native()) {
        m->clear_native_function();
        m->set_signature_handler(NULL);
      }
    }
  }
 HOTSPOT_JNI_UNREGISTERNATIVES_RETURN(0);
  return 0;
JNI_END

//
// Monitor functions
//

DT_RETURN_MARK_DECL(MonitorEnter, jint
                    , HOTSPOT_JNI_MONITORENTER_RETURN(_ret_ref));

JNI_ENTRY(jint, jni_MonitorEnter(JNIEnv *env, jobject jobj))
 HOTSPOT_JNI_MONITORENTER_ENTRY(env, jobj);
  jint ret = JNI_ERR;
  DT_RETURN_MARK(MonitorEnter, jint, (const jint&)ret);

  // If the object is null, we can't do anything with it
  if (jobj == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), JNI_ERR);
  }

  Handle obj(thread, JNIHandles::resolve_non_null(jobj));
  ObjectSynchronizer::jni_enter(obj, thread);
  ret = JNI_OK;
  return ret;
JNI_END

DT_RETURN_MARK_DECL(MonitorExit, jint
                    , HOTSPOT_JNI_MONITOREXIT_RETURN(_ret_ref));

JNI_ENTRY(jint, jni_MonitorExit(JNIEnv *env, jobject jobj))
 HOTSPOT_JNI_MONITOREXIT_ENTRY(env, jobj);
  jint ret = JNI_ERR;
  DT_RETURN_MARK(MonitorExit, jint, (const jint&)ret);

  // Don't do anything with a null object
  if (jobj == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), JNI_ERR);
  }

  Handle obj(THREAD, JNIHandles::resolve_non_null(jobj));
  ObjectSynchronizer::jni_exit(obj(), CHECK_(JNI_ERR));

  ret = JNI_OK;
  return ret;
JNI_END

//
// Extensions
//

DT_VOID_RETURN_MARK_DECL(GetStringRegion
                         , HOTSPOT_JNI_GETSTRINGREGION_RETURN());

JNI_ENTRY(void, jni_GetStringRegion(JNIEnv *env, jstring string, jsize start, jsize len, jchar *buf))
 HOTSPOT_JNI_GETSTRINGREGION_ENTRY(env, string, start, len, buf);
  DT_VOID_RETURN_MARK(GetStringRegion);
  oop s = JNIHandles::resolve_non_null(string);
  typeArrayOop s_value = java_lang_String::value(s);
  int s_len = java_lang_String::length(s, s_value);
  if (start < 0 || len < 0 || start > s_len - len) {
    THROW(vmSymbols::java_lang_StringIndexOutOfBoundsException());
  } else {
    if (len > 0) {
      bool is_latin1 = java_lang_String::is_latin1(s);
      if (!is_latin1) {
        ArrayAccess<>::arraycopy_to_native(s_value, typeArrayOopDesc::element_offset<jchar>(start),
                                           buf, len);
      } else {
        for (int i = 0; i < len; i++) {
          buf[i] = ((jchar) s_value->byte_at(i + start)) & 0xff;
        }
      }
    }
  }
JNI_END

DT_VOID_RETURN_MARK_DECL(GetStringUTFRegion
                         , HOTSPOT_JNI_GETSTRINGUTFREGION_RETURN());

JNI_ENTRY(void, jni_GetStringUTFRegion(JNIEnv *env, jstring string, jsize start, jsize len, char *buf))
 HOTSPOT_JNI_GETSTRINGUTFREGION_ENTRY(env, string, start, len, buf);
  DT_VOID_RETURN_MARK(GetStringUTFRegion);
  oop s = JNIHandles::resolve_non_null(string);
  typeArrayOop s_value = java_lang_String::value(s);
  int s_len = java_lang_String::length(s, s_value);
  if (start < 0 || len < 0 || start > s_len - len) {
    THROW(vmSymbols::java_lang_StringIndexOutOfBoundsException());
  } else {
    //%note jni_7
    if (len > 0) {
      // Assume the buffer is large enough as the JNI spec. does not require user error checking
      java_lang_String::as_utf8_string(s, s_value, start, len, buf, INT_MAX);
      // as_utf8_string null-terminates the result string
    } else {
      // JDK null-terminates the buffer even in len is zero
      if (buf != NULL) {
        buf[0] = 0;
      }
    }
  }
JNI_END

static oop lock_gc_or_pin_object(JavaThread* thread, jobject obj) {
  if (Universe::heap()->supports_object_pinning()) {
    const oop o = JNIHandles::resolve_non_null(obj);
    return Universe::heap()->pin_object(thread, o);
  } else {
    GCLocker::lock_critical(thread);
    return JNIHandles::resolve_non_null(obj);
  }
}

static void unlock_gc_or_unpin_object(JavaThread* thread, jobject obj) {
  if (Universe::heap()->supports_object_pinning()) {
    const oop o = JNIHandles::resolve_non_null(obj);
    return Universe::heap()->unpin_object(thread, o);
  } else {
    GCLocker::unlock_critical(thread);
  }
}

JNI_ENTRY(void*, jni_GetPrimitiveArrayCritical(JNIEnv *env, jarray array, jboolean *isCopy))
 HOTSPOT_JNI_GETPRIMITIVEARRAYCRITICAL_ENTRY(env, array, (uintptr_t *) isCopy);
  if (isCopy != NULL) {
    *isCopy = JNI_FALSE;
  }
  oop a = lock_gc_or_pin_object(thread, array);
  assert(a->is_typeArray(), "Primitive array only");
  BasicType type = TypeArrayKlass::cast(a->klass())->element_type();
  void* ret = arrayOop(a)->base(type);
 HOTSPOT_JNI_GETPRIMITIVEARRAYCRITICAL_RETURN(ret);
  return ret;
JNI_END


JNI_ENTRY(void, jni_ReleasePrimitiveArrayCritical(JNIEnv *env, jarray array, void *carray, jint mode))
  HOTSPOT_JNI_RELEASEPRIMITIVEARRAYCRITICAL_ENTRY(env, array, carray, mode);
  unlock_gc_or_unpin_object(thread, array);
HOTSPOT_JNI_RELEASEPRIMITIVEARRAYCRITICAL_RETURN();
JNI_END


static typeArrayOop lock_gc_or_pin_string_value(JavaThread* thread, oop str) {
  if (Universe::heap()->supports_object_pinning()) {
    // Forbid deduplication before obtaining the value array, to prevent
    // deduplication from replacing the value array while setting up or in
    // the critical section.  That would lead to the release operation
    // unpinning the wrong object.
    if (StringDedup::is_enabled()) {
      NoSafepointVerifier nsv;
      StringDedup::forbid_deduplication(str);
    }
    typeArrayOop s_value = java_lang_String::value(str);
    return (typeArrayOop) Universe::heap()->pin_object(thread, s_value);
  } else {
    Handle h(thread, str);      // Handlize across potential safepoint.
    GCLocker::lock_critical(thread);
    return java_lang_String::value(h());
  }
}

static void unlock_gc_or_unpin_string_value(JavaThread* thread, oop str) {
  if (Universe::heap()->supports_object_pinning()) {
    typeArrayOop s_value = java_lang_String::value(str);
    Universe::heap()->unpin_object(thread, s_value);
  } else {
    GCLocker::unlock_critical(thread);
  }
}

JNI_ENTRY(const jchar*, jni_GetStringCritical(JNIEnv *env, jstring string, jboolean *isCopy))
  HOTSPOT_JNI_GETSTRINGCRITICAL_ENTRY(env, string, (uintptr_t *) isCopy);
  oop s = JNIHandles::resolve_non_null(string);
  jchar* ret;
  if (!java_lang_String::is_latin1(s)) {
    typeArrayOop s_value = lock_gc_or_pin_string_value(thread, s);
    ret = (jchar*) s_value->base(T_CHAR);
    if (isCopy != NULL) *isCopy = JNI_FALSE;
  } else {
    // Inflate latin1 encoded string to UTF16
    typeArrayOop s_value = java_lang_String::value(s);
    int s_len = java_lang_String::length(s, s_value);
    ret = NEW_C_HEAP_ARRAY_RETURN_NULL(jchar, s_len + 1, mtInternal);  // add one for zero termination
    /* JNI Specification states return NULL on OOM */
    if (ret != NULL) {
      for (int i = 0; i < s_len; i++) {
        ret[i] = ((jchar) s_value->byte_at(i)) & 0xff;
      }
      ret[s_len] = 0;
    }
    if (isCopy != NULL) *isCopy = JNI_TRUE;
  }
 HOTSPOT_JNI_GETSTRINGCRITICAL_RETURN((uint16_t *) ret);
  return ret;
JNI_END


JNI_ENTRY(void, jni_ReleaseStringCritical(JNIEnv *env, jstring str, const jchar *chars))
  HOTSPOT_JNI_RELEASESTRINGCRITICAL_ENTRY(env, str, (uint16_t *) chars);
  oop s = JNIHandles::resolve_non_null(str);
  bool is_latin1 = java_lang_String::is_latin1(s);
  if (is_latin1) {
    // For latin1 string, free jchar array allocated by earlier call to GetStringCritical.
    // This assumes that ReleaseStringCritical bookends GetStringCritical.
    FREE_C_HEAP_ARRAY(jchar, chars);
  } else {
    // For non-latin1 string, drop the associated gc-locker/pin.
    unlock_gc_or_unpin_string_value(thread, s);
  }
HOTSPOT_JNI_RELEASESTRINGCRITICAL_RETURN();
JNI_END


JNI_ENTRY(jweak, jni_NewWeakGlobalRef(JNIEnv *env, jobject ref))
  HOTSPOT_JNI_NEWWEAKGLOBALREF_ENTRY(env, ref);
  Handle ref_handle(thread, JNIHandles::resolve(ref));
  jweak ret = JNIHandles::make_weak_global(ref_handle, AllocFailStrategy::RETURN_NULL);
  if (ret == NULL) {
    THROW_OOP_(Universe::out_of_memory_error_c_heap(), NULL);
  }
  HOTSPOT_JNI_NEWWEAKGLOBALREF_RETURN(ret);
  return ret;
JNI_END

// Must be JNI_ENTRY (with HandleMark)
JNI_ENTRY(void, jni_DeleteWeakGlobalRef(JNIEnv *env, jweak ref))
  HOTSPOT_JNI_DELETEWEAKGLOBALREF_ENTRY(env, ref);
  JNIHandles::destroy_weak_global(ref);
  HOTSPOT_JNI_DELETEWEAKGLOBALREF_RETURN();
JNI_END


JNI_ENTRY_NO_PRESERVE(jboolean, jni_ExceptionCheck(JNIEnv *env))
 HOTSPOT_JNI_EXCEPTIONCHECK_ENTRY(env);
  jni_check_async_exceptions(thread);
  jboolean ret = (thread->has_pending_exception()) ? JNI_TRUE : JNI_FALSE;
 HOTSPOT_JNI_EXCEPTIONCHECK_RETURN(ret);
  return ret;
JNI_END


// Initialization state for three routines below relating to
// java.nio.DirectBuffers
static          int directBufferSupportInitializeStarted = 0;
static volatile int directBufferSupportInitializeEnded   = 0;
static volatile int directBufferSupportInitializeFailed  = 0;
static jclass    bufferClass                 = NULL;
static jclass    directBufferClass           = NULL;
static jclass    directByteBufferClass       = NULL;
static jmethodID directByteBufferConstructor = NULL;
static jfieldID  directBufferAddressField    = NULL;
static jfieldID  bufferCapacityField         = NULL;

static jclass lookupOne(JNIEnv* env, const char* name, TRAPS) {
  Handle loader;            // null (bootstrap) loader
  Handle protection_domain; // null protection domain

  TempNewSymbol sym = SymbolTable::new_symbol(name);
  jclass result =  find_class_from_class_loader(env, sym, true, loader, protection_domain, true, CHECK_NULL);

  if (log_is_enabled(Debug, class, resolve) && result != NULL) {
    trace_class_resolution(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(result)));
  }
  return result;
}

// These lookups are done with the NULL (bootstrap) ClassLoader to
// circumvent any security checks that would be done by jni_FindClass.
JNI_ENTRY(bool, lookupDirectBufferClasses(JNIEnv* env))
{
  if ((bufferClass           = lookupOne(env, "java/nio/Buffer", thread))           == NULL) { return false; }
  if ((directBufferClass     = lookupOne(env, "sun/nio/ch/DirectBuffer", thread))   == NULL) { return false; }
  if ((directByteBufferClass = lookupOne(env, "java/nio/DirectByteBuffer", thread)) == NULL) { return false; }
  return true;
}
JNI_END


static bool initializeDirectBufferSupport(JNIEnv* env, JavaThread* thread) {
  if (directBufferSupportInitializeFailed) {
    return false;
  }

  if (Atomic::cmpxchg(&directBufferSupportInitializeStarted, 0, 1) == 0) {
    if (!lookupDirectBufferClasses(env)) {
      directBufferSupportInitializeFailed = 1;
      return false;
    }

    // Make global references for these
    bufferClass           = (jclass) env->NewGlobalRef(bufferClass);
    directBufferClass     = (jclass) env->NewGlobalRef(directBufferClass);
    directByteBufferClass = (jclass) env->NewGlobalRef(directByteBufferClass);

    // Global refs will be NULL if out-of-memory (no exception is pending)
    if (bufferClass == NULL || directBufferClass == NULL || directByteBufferClass == NULL) {
      directBufferSupportInitializeFailed = 1;
      return false;
    }

    // Get needed field and method IDs
    directByteBufferConstructor = env->GetMethodID(directByteBufferClass, "<init>", "(JI)V");
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
      directBufferSupportInitializeFailed = 1;
      return false;
    }
    directBufferAddressField    = env->GetFieldID(bufferClass, "address", "J");
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
      directBufferSupportInitializeFailed = 1;
      return false;
    }
    bufferCapacityField         = env->GetFieldID(bufferClass, "capacity", "I");
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
      directBufferSupportInitializeFailed = 1;
      return false;
    }

    if ((directByteBufferConstructor == NULL) ||
        (directBufferAddressField    == NULL) ||
        (bufferCapacityField         == NULL)) {
      directBufferSupportInitializeFailed = 1;
      return false;
    }

    directBufferSupportInitializeEnded = 1;
  } else {
    while (!directBufferSupportInitializeEnded && !directBufferSupportInitializeFailed) {
      os::naked_yield();
    }
  }

  return !directBufferSupportInitializeFailed;
}

extern "C" jobject JNICALL jni_NewDirectByteBuffer(JNIEnv *env, void* address, jlong capacity)
{
  // thread_from_jni_environment() will block if VM is gone.
  JavaThread* thread = JavaThread::thread_from_jni_environment(env);

 HOTSPOT_JNI_NEWDIRECTBYTEBUFFER_ENTRY(env, address, capacity);

  if (!directBufferSupportInitializeEnded) {
    if (!initializeDirectBufferSupport(env, thread)) {
      HOTSPOT_JNI_NEWDIRECTBYTEBUFFER_RETURN(NULL);
      return NULL;
    }
  }

  // Being paranoid about accidental sign extension on address
  jlong addr = (jlong) ((uintptr_t) address);
  // NOTE that package-private DirectByteBuffer constructor currently
  // takes int capacity
  jint  cap  = (jint)  capacity;
  jobject ret = env->NewObject(directByteBufferClass, directByteBufferConstructor, addr, cap);
  HOTSPOT_JNI_NEWDIRECTBYTEBUFFER_RETURN(ret);
  return ret;
}

DT_RETURN_MARK_DECL(GetDirectBufferAddress, void*
                    , HOTSPOT_JNI_GETDIRECTBUFFERADDRESS_RETURN((void*) _ret_ref));

extern "C" void* JNICALL jni_GetDirectBufferAddress(JNIEnv *env, jobject buf)
{
  // thread_from_jni_environment() will block if VM is gone.
  JavaThread* thread = JavaThread::thread_from_jni_environment(env);

  HOTSPOT_JNI_GETDIRECTBUFFERADDRESS_ENTRY(env, buf);
  void* ret = NULL;
  DT_RETURN_MARK(GetDirectBufferAddress, void*, (const void*&)ret);

  if (!directBufferSupportInitializeEnded) {
    if (!initializeDirectBufferSupport(env, thread)) {
      return 0;
    }
  }

  if ((buf != NULL) && (!env->IsInstanceOf(buf, directBufferClass))) {
    return 0;
  }

  ret = (void*)(intptr_t)env->GetLongField(buf, directBufferAddressField);
  return ret;
}

DT_RETURN_MARK_DECL(GetDirectBufferCapacity, jlong
                    , HOTSPOT_JNI_GETDIRECTBUFFERCAPACITY_RETURN(_ret_ref));

extern "C" jlong JNICALL jni_GetDirectBufferCapacity(JNIEnv *env, jobject buf)
{
  // thread_from_jni_environment() will block if VM is gone.
  JavaThread* thread = JavaThread::thread_from_jni_environment(env);

  HOTSPOT_JNI_GETDIRECTBUFFERCAPACITY_ENTRY(env, buf);
  jlong ret = -1;
  DT_RETURN_MARK(GetDirectBufferCapacity, jlong, (const jlong&)ret);

  if (!directBufferSupportInitializeEnded) {
    if (!initializeDirectBufferSupport(env, thread)) {
      ret = 0;
      return ret;
    }
  }

  if (buf == NULL) {
    return -1;
  }

  if (!env->IsInstanceOf(buf, directBufferClass)) {
    return -1;
  }

  // NOTE that capacity is currently an int in the implementation
  ret = env->GetIntField(buf, bufferCapacityField);
  return ret;
}


JNI_LEAF(jint, jni_GetVersion(JNIEnv *env))
  HOTSPOT_JNI_GETVERSION_ENTRY(env);
  HOTSPOT_JNI_GETVERSION_RETURN(CurrentVersion);
  return CurrentVersion;
JNI_END

extern struct JavaVM_ main_vm;

JNI_LEAF(jint, jni_GetJavaVM(JNIEnv *env, JavaVM **vm))
  HOTSPOT_JNI_GETJAVAVM_ENTRY(env, (void **) vm);
  *vm  = (JavaVM *)(&main_vm);
  HOTSPOT_JNI_GETJAVAVM_RETURN(JNI_OK);
  return JNI_OK;
JNI_END


JNI_ENTRY(jobject, jni_GetModule(JNIEnv* env, jclass clazz))
  return Modules::get_module(clazz, THREAD);
JNI_END


// Structure containing all jni functions
struct JNINativeInterface_ jni_NativeInterface = {
    NULL,
    NULL,
    NULL,

    NULL,

    jni_GetVersion,

    jni_DefineClass,
    jni_FindClass,

    jni_FromReflectedMethod,
    jni_FromReflectedField,

    jni_ToReflectedMethod,

    jni_GetSuperclass,
    jni_IsAssignableFrom,

    jni_ToReflectedField,

    jni_Throw,
    jni_ThrowNew,
    jni_ExceptionOccurred,
    jni_ExceptionDescribe,
    jni_ExceptionClear,
    jni_FatalError,

    jni_PushLocalFrame,
    jni_PopLocalFrame,

    jni_NewGlobalRef,
    jni_DeleteGlobalRef,
    jni_DeleteLocalRef,
    jni_IsSameObject,

    jni_NewLocalRef,
    jni_EnsureLocalCapacity,

    jni_AllocObject,
    jni_NewObject,
    jni_NewObjectV,
    jni_NewObjectA,

    jni_GetObjectClass,
    jni_IsInstanceOf,

    jni_GetMethodID,

    jni_CallObjectMethod,
    jni_CallObjectMethodV,
    jni_CallObjectMethodA,
    jni_CallBooleanMethod,
    jni_CallBooleanMethodV,
    jni_CallBooleanMethodA,
    jni_CallByteMethod,
    jni_CallByteMethodV,
    jni_CallByteMethodA,
    jni_CallCharMethod,
    jni_CallCharMethodV,
    jni_CallCharMethodA,
    jni_CallShortMethod,
    jni_CallShortMethodV,
    jni_CallShortMethodA,
    jni_CallIntMethod,
    jni_CallIntMethodV,
    jni_CallIntMethodA,
    jni_CallLongMethod,
    jni_CallLongMethodV,
    jni_CallLongMethodA,
    jni_CallFloatMethod,
    jni_CallFloatMethodV,
    jni_CallFloatMethodA,
    jni_CallDoubleMethod,
    jni_CallDoubleMethodV,
    jni_CallDoubleMethodA,
    jni_CallVoidMethod,
    jni_CallVoidMethodV,
    jni_CallVoidMethodA,

    jni_CallNonvirtualObjectMethod,
    jni_CallNonvirtualObjectMethodV,
    jni_CallNonvirtualObjectMethodA,
    jni_CallNonvirtualBooleanMethod,
    jni_CallNonvirtualBooleanMethodV,
    jni_CallNonvirtualBooleanMethodA,
    jni_CallNonvirtualByteMethod,
    jni_CallNonvirtualByteMethodV,
    jni_CallNonvirtualByteMethodA,
    jni_CallNonvirtualCharMethod,
    jni_CallNonvirtualCharMethodV,
    jni_CallNonvirtualCharMethodA,
    jni_CallNonvirtualShortMethod,
    jni_CallNonvirtualShortMethodV,
    jni_CallNonvirtualShortMethodA,
    jni_CallNonvirtualIntMethod,
    jni_CallNonvirtualIntMethodV,
    jni_CallNonvirtualIntMethodA,
    jni_CallNonvirtualLongMethod,
    jni_CallNonvirtualLongMethodV,
    jni_CallNonvirtualLongMethodA,
    jni_CallNonvirtualFloatMethod,
    jni_CallNonvirtualFloatMethodV,
    jni_CallNonvirtualFloatMethodA,
    jni_CallNonvirtualDoubleMethod,
    jni_CallNonvirtualDoubleMethodV,
    jni_CallNonvirtualDoubleMethodA,
    jni_CallNonvirtualVoidMethod,
    jni_CallNonvirtualVoidMethodV,
    jni_CallNonvirtualVoidMethodA,

    jni_GetFieldID,

    jni_GetObjectField,
    jni_GetBooleanField,
    jni_GetByteField,
    jni_GetCharField,
    jni_GetShortField,
    jni_GetIntField,
    jni_GetLongField,
    jni_GetFloatField,
    jni_GetDoubleField,

    jni_SetObjectField,
    jni_SetBooleanField,
    jni_SetByteField,
    jni_SetCharField,
    jni_SetShortField,
    jni_SetIntField,
    jni_SetLongField,
    jni_SetFloatField,
    jni_SetDoubleField,

    jni_GetStaticMethodID,

    jni_CallStaticObjectMethod,
    jni_CallStaticObjectMethodV,
    jni_CallStaticObjectMethodA,
    jni_CallStaticBooleanMethod,
    jni_CallStaticBooleanMethodV,
    jni_CallStaticBooleanMethodA,
    jni_CallStaticByteMethod,
    jni_CallStaticByteMethodV,
    jni_CallStaticByteMethodA,
    jni_CallStaticCharMethod,
    jni_CallStaticCharMethodV,
    jni_CallStaticCharMethodA,
    jni_CallStaticShortMethod,
    jni_CallStaticShortMethodV,
    jni_CallStaticShortMethodA,
    jni_CallStaticIntMethod,
    jni_CallStaticIntMethodV,
    jni_CallStaticIntMethodA,
    jni_CallStaticLongMethod,
    jni_CallStaticLongMethodV,
    jni_CallStaticLongMethodA,
    jni_CallStaticFloatMethod,
    jni_CallStaticFloatMethodV,
    jni_CallStaticFloatMethodA,
    jni_CallStaticDoubleMethod,
    jni_CallStaticDoubleMethodV,
    jni_CallStaticDoubleMethodA,
    jni_CallStaticVoidMethod,
    jni_CallStaticVoidMethodV,
    jni_CallStaticVoidMethodA,

    jni_GetStaticFieldID,

    jni_GetStaticObjectField,
    jni_GetStaticBooleanField,
    jni_GetStaticByteField,
    jni_GetStaticCharField,
    jni_GetStaticShortField,
    jni_GetStaticIntField,
    jni_GetStaticLongField,
    jni_GetStaticFloatField,
    jni_GetStaticDoubleField,

    jni_SetStaticObjectField,
    jni_SetStaticBooleanField,
    jni_SetStaticByteField,
    jni_SetStaticCharField,
    jni_SetStaticShortField,
    jni_SetStaticIntField,
    jni_SetStaticLongField,
    jni_SetStaticFloatField,
    jni_SetStaticDoubleField,

    jni_NewString,
    jni_GetStringLength,
    jni_GetStringChars,
    jni_ReleaseStringChars,

    jni_NewStringUTF,
    jni_GetStringUTFLength,
    jni_GetStringUTFChars,
    jni_ReleaseStringUTFChars,

    jni_GetArrayLength,

    jni_NewObjectArray,
    jni_GetObjectArrayElement,
    jni_SetObjectArrayElement,

    jni_NewBooleanArray,
    jni_NewByteArray,
    jni_NewCharArray,
    jni_NewShortArray,
    jni_NewIntArray,
    jni_NewLongArray,
    jni_NewFloatArray,
    jni_NewDoubleArray,

    jni_GetBooleanArrayElements,
    jni_GetByteArrayElements,
    jni_GetCharArrayElements,
    jni_GetShortArrayElements,
    jni_GetIntArrayElements,
    jni_GetLongArrayElements,
    jni_GetFloatArrayElements,
    jni_GetDoubleArrayElements,

    jni_ReleaseBooleanArrayElements,
    jni_ReleaseByteArrayElements,
    jni_ReleaseCharArrayElements,
    jni_ReleaseShortArrayElements,
    jni_ReleaseIntArrayElements,
    jni_ReleaseLongArrayElements,
    jni_ReleaseFloatArrayElements,
    jni_ReleaseDoubleArrayElements,

    jni_GetBooleanArrayRegion,
    jni_GetByteArrayRegion,
    jni_GetCharArrayRegion,
    jni_GetShortArrayRegion,
    jni_GetIntArrayRegion,
    jni_GetLongArrayRegion,
    jni_GetFloatArrayRegion,
    jni_GetDoubleArrayRegion,

    jni_SetBooleanArrayRegion,
    jni_SetByteArrayRegion,
    jni_SetCharArrayRegion,
    jni_SetShortArrayRegion,
    jni_SetIntArrayRegion,
    jni_SetLongArrayRegion,
    jni_SetFloatArrayRegion,
    jni_SetDoubleArrayRegion,

    jni_RegisterNatives,
    jni_UnregisterNatives,

    jni_MonitorEnter,
    jni_MonitorExit,

    jni_GetJavaVM,

    jni_GetStringRegion,
    jni_GetStringUTFRegion,

    jni_GetPrimitiveArrayCritical,
    jni_ReleasePrimitiveArrayCritical,

    jni_GetStringCritical,
    jni_ReleaseStringCritical,

    jni_NewWeakGlobalRef,
    jni_DeleteWeakGlobalRef,

    jni_ExceptionCheck,

    jni_NewDirectByteBuffer,
    jni_GetDirectBufferAddress,
    jni_GetDirectBufferCapacity,

    // New 1_6 features

    jni_GetObjectRefType,

    // Module features

    jni_GetModule
};


// For jvmti use to modify jni function table.
// Java threads in native contiues to run until it is transitioned
// to VM at safepoint. Before the transition or before it is blocked
// for safepoint it may access jni function table. VM could crash if
// any java thread access the jni function table in the middle of memcpy.
// To avoid this each function pointers are copied automically.
void copy_jni_function_table(const struct JNINativeInterface_ *new_jni_NativeInterface) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  intptr_t *a = (intptr_t *) jni_functions();
  intptr_t *b = (intptr_t *) new_jni_NativeInterface;
  for (uint i=0; i <  sizeof(struct JNINativeInterface_)/sizeof(void *); i++) {
    Atomic::store(a++, *b++);
  }
}

void quicken_jni_functions() {
  // Replace Get<Primitive>Field with fast versions
  if (UseFastJNIAccessors && !VerifyJNIFields && !CheckJNICalls) {
    address func;
    func = JNI_FastGetField::generate_fast_get_boolean_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetBooleanField = (GetBooleanField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_byte_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetByteField = (GetByteField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_char_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetCharField = (GetCharField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_short_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetShortField = (GetShortField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_int_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetIntField = (GetIntField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_long_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetLongField = (GetLongField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_float_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetFloatField = (GetFloatField_t)func;
    }
    func = JNI_FastGetField::generate_fast_get_double_field();
    if (func != (address)-1) {
      jni_NativeInterface.GetDoubleField = (GetDoubleField_t)func;
    }
  }
}

// Returns the function structure
struct JNINativeInterface_* jni_functions() {
#if INCLUDE_JNI_CHECK
  if (CheckJNICalls) return jni_functions_check();
#endif // INCLUDE_JNI_CHECK
  return &jni_NativeInterface;
}

// Returns the function structure
struct JNINativeInterface_* jni_functions_nocheck() {
  return &jni_NativeInterface;
}

static void post_thread_start_event(const JavaThread* jt) {
  assert(jt != NULL, "invariant");
  EventThreadStart event;
  if (event.should_commit()) {
    event.set_thread(JFR_THREAD_ID(jt));
    event.set_parentThread((traceid)0);
#if INCLUDE_JFR
    if (EventThreadStart::is_stacktrace_enabled()) {
      jt->jfr_thread_local()->set_cached_stack_trace_id((traceid)0);
      event.commit();
      jt->jfr_thread_local()->clear_cached_stack_trace();
    } else
#endif
    {
      event.commit();
    }
  }
}

// Invocation API


// Forward declaration
extern const struct JNIInvokeInterface_ jni_InvokeInterface;

// Global invocation API vars
volatile int vm_created = 0;
// Indicate whether it is safe to recreate VM. Recreation is only
// possible after a failed initial creation attempt in some cases.
volatile int safe_to_recreate_vm = 1;
struct JavaVM_ main_vm = {&jni_InvokeInterface};


#define JAVASTACKSIZE (400 * 1024)    /* Default size of a thread java stack */
enum { VERIFY_NONE, VERIFY_REMOTE, VERIFY_ALL };

DT_RETURN_MARK_DECL(GetDefaultJavaVMInitArgs, jint
                    , HOTSPOT_JNI_GETDEFAULTJAVAVMINITARGS_RETURN(_ret_ref));

_JNI_IMPORT_OR_EXPORT_ jint JNICALL JNI_GetDefaultJavaVMInitArgs(void *args_) {
  HOTSPOT_JNI_GETDEFAULTJAVAVMINITARGS_ENTRY(args_);
  JDK1_1InitArgs *args = (JDK1_1InitArgs *)args_;
  jint ret = JNI_ERR;
  DT_RETURN_MARK(GetDefaultJavaVMInitArgs, jint, (const jint&)ret);

  if (Threads::is_supported_jni_version(args->version)) {
    ret = JNI_OK;
  }
  // 1.1 style no longer supported in hotspot.
  // According the JNI spec, we should update args->version on return.
  // We also use the structure to communicate with launcher about default
  // stack size.
  if (args->version == JNI_VERSION_1_1) {
    args->version = JNI_VERSION_1_2;
    // javaStackSize is int in arguments structure
    assert(jlong(ThreadStackSize) * K < INT_MAX, "integer overflow");
    args->javaStackSize = (jint)(ThreadStackSize * K);
  }
  return ret;
}

DT_RETURN_MARK_DECL(CreateJavaVM, jint
                    , HOTSPOT_JNI_CREATEJAVAVM_RETURN(_ret_ref));

static jint JNI_CreateJavaVM_inner(JavaVM **vm, void **penv, void *args) {
  HOTSPOT_JNI_CREATEJAVAVM_ENTRY((void **) vm, penv, args);

  jint result = JNI_ERR;
  DT_RETURN_MARK(CreateJavaVM, jint, (const jint&)result);

  // We're about to use Atomic::xchg for synchronization.  Some Zero
  // platforms use the GCC builtin __sync_lock_test_and_set for this,
  // but __sync_lock_test_and_set is not guaranteed to do what we want
  // on all architectures.  So we check it works before relying on it.
#if defined(ZERO) && defined(ASSERT)
  {
    jint a = 0xcafebabe;
    jint b = Atomic::xchg(&a, (jint) 0xdeadbeef);
    void *c = &a;
    void *d = Atomic::xchg(&c, &b);
    assert(a == (jint) 0xdeadbeef && b == (jint) 0xcafebabe, "Atomic::xchg() works");
    assert(c == &b && d == &a, "Atomic::xchg() works");
  }
#endif // ZERO && ASSERT

  // At the moment it's only possible to have one Java VM,
  // since some of the runtime state is in global variables.

  // We cannot use our mutex locks here, since they only work on
  // Threads. We do an atomic compare and exchange to ensure only
  // one thread can call this method at a time

  // We use Atomic::xchg rather than Atomic::add/dec since on some platforms
  // the add/dec implementations are dependent on whether we are running
  // on a multiprocessor Atomic::xchg does not have this problem.
  if (Atomic::xchg(&vm_created, 1) == 1) {
    return JNI_EEXIST;   // already created, or create attempt in progress
  }

  // If a previous creation attempt failed but can be retried safely,
  // then safe_to_recreate_vm will have been reset to 1 after being
  // cleared here. If a previous creation attempt succeeded and we then
  // destroyed that VM, we will be prevented from trying to recreate
  // the VM in the same process, as the value will still be 0.
  if (Atomic::xchg(&safe_to_recreate_vm, 0) == 0) {
    return JNI_ERR;
  }

  assert(vm_created == 1, "vm_created is true during the creation");

  /**
   * Certain errors during initialization are recoverable and do not
   * prevent this method from being called again at a later time
   * (perhaps with different arguments).  However, at a certain
   * point during initialization if an error occurs we cannot allow
   * this function to be called again (or it will crash).  In those
   * situations, the 'canTryAgain' flag is set to false, which atomically
   * sets safe_to_recreate_vm to 1, such that any new call to
   * JNI_CreateJavaVM will immediately fail using the above logic.
   */
  bool can_try_again = true;

  result = Threads::create_vm((JavaVMInitArgs*) args, &can_try_again);
  if (result == JNI_OK) {
    JavaThread *thread = JavaThread::current();
    assert(!thread->has_pending_exception(), "should have returned not OK");
    /* thread is thread_in_vm here */
    *vm = (JavaVM *)(&main_vm);
    *(JNIEnv**)penv = thread->jni_environment();

#if INCLUDE_JVMCI
    if (EnableJVMCI) {
      if (UseJVMCICompiler) {
        // JVMCI is initialized on a CompilerThread
        if (BootstrapJVMCI) {
          JavaThread* THREAD = thread; // For exception macros.
          JVMCICompiler* compiler = JVMCICompiler::instance(true, CATCH);
          compiler->bootstrap(THREAD);
          if (HAS_PENDING_EXCEPTION) {
            HandleMark hm(THREAD);
            vm_exit_during_initialization(Handle(THREAD, PENDING_EXCEPTION));
          }
        }
      }
    }
#endif

    // Notify JVMTI
    if (JvmtiExport::should_post_thread_life()) {
       JvmtiExport::post_thread_start(thread);
    }

    post_thread_start_event(thread);

#ifndef PRODUCT
    if (ReplayCompiles) ciReplay::replay(thread);
#endif

#ifdef ASSERT
    // Some platforms (like Win*) need a wrapper around these test
    // functions in order to properly handle error conditions.
    if (ErrorHandlerTest != 0) {
      VMError::controlled_crash(ErrorHandlerTest);
    }
#endif

    // Since this is not a JVM_ENTRY we have to set the thread state manually before leaving.
    ThreadStateTransition::transition(thread, _thread_in_vm, _thread_in_native);
    MACOS_AARCH64_ONLY(thread->enable_wx(WXExec));
  } else {
    // If create_vm exits because of a pending exception, exit with that
    // exception.  In the future when we figure out how to reclaim memory,
    // we may be able to exit with JNI_ERR and allow the calling application
    // to continue.
    if (Universe::is_fully_initialized()) {
      // otherwise no pending exception possible - VM will already have aborted
      JavaThread* THREAD = JavaThread::current(); // For exception macros.
      if (HAS_PENDING_EXCEPTION) {
        HandleMark hm(THREAD);
        vm_exit_during_initialization(Handle(THREAD, PENDING_EXCEPTION));
      }
    }

    if (can_try_again) {
      // reset safe_to_recreate_vm to 1 so that retrial would be possible
      safe_to_recreate_vm = 1;
    }

    // Creation failed. We must reset vm_created
    *vm = 0;
    *(JNIEnv**)penv = 0;
    // reset vm_created last to avoid race condition. Use OrderAccess to
    // control both compiler and architectural-based reordering.
    Atomic::release_store(&vm_created, 0);
  }

  // Flush stdout and stderr before exit.
  fflush(stdout);
  fflush(stderr);

  return result;

}

_JNI_IMPORT_OR_EXPORT_ jint JNICALL JNI_CreateJavaVM(JavaVM **vm, void **penv, void *args) {
  jint result = JNI_ERR;
  // On Windows, let CreateJavaVM run with SEH protection
#if defined(_WIN32) && !defined(USE_VECTORED_EXCEPTION_HANDLING)
  __try {
#endif
    result = JNI_CreateJavaVM_inner(vm, penv, args);
#if defined(_WIN32) && !defined(USE_VECTORED_EXCEPTION_HANDLING)
  } __except(topLevelExceptionFilter((_EXCEPTION_POINTERS*)_exception_info())) {
    // Nothing to do.
  }
#endif
  return result;
}

_JNI_IMPORT_OR_EXPORT_ jint JNICALL JNI_GetCreatedJavaVMs(JavaVM **vm_buf, jsize bufLen, jsize *numVMs) {
  HOTSPOT_JNI_GETCREATEDJAVAVMS_ENTRY((void **) vm_buf, bufLen, (uintptr_t *) numVMs);

  if (vm_created == 1) {
    if (numVMs != NULL) *numVMs = 1;
    if (bufLen > 0)     *vm_buf = (JavaVM *)(&main_vm);
  } else {
    if (numVMs != NULL) *numVMs = 0;
  }
  HOTSPOT_JNI_GETCREATEDJAVAVMS_RETURN(JNI_OK);
  return JNI_OK;
}

extern "C" {

DT_RETURN_MARK_DECL(DestroyJavaVM, jint
                    , HOTSPOT_JNI_DESTROYJAVAVM_RETURN(_ret_ref));

static jint JNICALL jni_DestroyJavaVM_inner(JavaVM *vm) {
  HOTSPOT_JNI_DESTROYJAVAVM_ENTRY(vm);
  jint res = JNI_ERR;
  DT_RETURN_MARK(DestroyJavaVM, jint, (const jint&)res);

  if (vm_created == 0) {
    res = JNI_ERR;
    return res;
  }

  JNIEnv *env;
  JavaVMAttachArgs destroyargs;
  destroyargs.version = CurrentVersion;
  destroyargs.name = (char *)"DestroyJavaVM";
  destroyargs.group = NULL;
  res = vm->AttachCurrentThread((void **)&env, (void *)&destroyargs);
  if (res != JNI_OK) {
    return res;
  }

  // Since this is not a JVM_ENTRY we have to set the thread state manually before entering.
  JavaThread* thread = JavaThread::current();

  // We are going to VM, change W^X state to the expected one.
  MACOS_AARCH64_ONLY(WXMode oldmode = thread->enable_wx(WXWrite));

  ThreadStateTransition::transition_from_native(thread, _thread_in_vm);
  Threads::destroy_vm();
  // Don't bother restoring thread state, VM is gone.
  vm_created = 0;
  return JNI_OK;
}

jint JNICALL jni_DestroyJavaVM(JavaVM *vm) {
  jint result = JNI_ERR;
  // On Windows, we need SEH protection
#if defined(_WIN32) && !defined(USE_VECTORED_EXCEPTION_HANDLING)
  __try {
#endif
    result = jni_DestroyJavaVM_inner(vm);
#if defined(_WIN32) && !defined(USE_VECTORED_EXCEPTION_HANDLING)
  } __except(topLevelExceptionFilter((_EXCEPTION_POINTERS*)_exception_info())) {
    // Nothing to do.
  }
#endif
  return result;
}

static jint attach_current_thread(JavaVM *vm, void **penv, void *_args, bool daemon) {
  JavaVMAttachArgs *args = (JavaVMAttachArgs *) _args;

  // Check below commented out from JDK1.2fcs as well
  /*
  if (args && (args->version != JNI_VERSION_1_1 || args->version != JNI_VERSION_1_2)) {
    return JNI_EVERSION;
  }
  */

  Thread* t = Thread::current_or_null();
  if (t != NULL) {
    // If executing from an atexit hook we may be in the VMThread.
    if (t->is_Java_thread()) {
      // If the thread has been attached this operation is a no-op
      *(JNIEnv**)penv = JavaThread::cast(t)->jni_environment();
      return JNI_OK;
    } else {
      return JNI_ERR;
    }
  }

  // Create a thread and mark it as attaching so it will be skipped by the
  // ThreadsListEnumerator - see CR 6404306
  JavaThread* thread = new JavaThread(true);

  // Set correct safepoint info. The thread is going to call into Java when
  // initializing the Java level thread object. Hence, the correct state must
  // be set in order for the Safepoint code to deal with it correctly.
  thread->set_thread_state(_thread_in_vm);
  thread->record_stack_base_and_size();
  thread->register_thread_stack_with_NMT();
  thread->initialize_thread_current();
  MACOS_AARCH64_ONLY(thread->init_wx());

  if (!os::create_attached_thread(thread)) {
    thread->smr_delete();
    return JNI_ERR;
  }
  // Enable stack overflow checks
  thread->stack_overflow_state()->create_stack_guard_pages();

  thread->initialize_tlab();

  thread->cache_global_variables();

  // This thread will not do a safepoint check, since it has
  // not been added to the Thread list yet.
  { MutexLocker ml(Threads_lock);
    // This must be inside this lock in order to get FullGCALot to work properly, i.e., to
    // avoid this thread trying to do a GC before it is added to the thread-list
    thread->set_active_handles(JNIHandleBlock::allocate_block());
    Threads::add(thread, daemon);
  }
  // Create thread group and name info from attach arguments
  oop group = NULL;
  char* thread_name = NULL;
  if (args != NULL && Threads::is_supported_jni_version(args->version)) {
    group = JNIHandles::resolve(args->group);
    thread_name = args->name; // may be NULL
  }
  if (group == NULL) group = Universe::main_thread_group();

  // Create Java level thread object and attach it to this thread
  bool attach_failed = false;
  {
    EXCEPTION_MARK;
    HandleMark hm(THREAD);
    Handle thread_group(THREAD, group);
    thread->allocate_threadObj(thread_group, thread_name, daemon, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
      // cleanup outside the handle mark.
      attach_failed = true;
    }
  }

  if (attach_failed) {
    // Added missing cleanup
    thread->cleanup_failed_attach_current_thread(daemon);
    return JNI_ERR;
  }

  // mark the thread as no longer attaching
  // this uses a fence to push the change through so we don't have
  // to regrab the threads_lock
  thread->set_done_attaching_via_jni();

  // Set java thread status.
  java_lang_Thread::set_thread_status(thread->threadObj(),
              JavaThreadStatus::RUNNABLE);

  // Notify the debugger
  if (JvmtiExport::should_post_thread_life()) {
    JvmtiExport::post_thread_start(thread);
  }

  post_thread_start_event(thread);

  *(JNIEnv**)penv = thread->jni_environment();

  // Now leaving the VM, so change thread_state. This is normally automatically taken care
  // of in the JVM_ENTRY. But in this situation we have to do it manually. Notice, that by
  // using ThreadStateTransition::transition, we do a callback to the safepoint code if
  // needed.

  ThreadStateTransition::transition(thread, _thread_in_vm, _thread_in_native);
  MACOS_AARCH64_ONLY(thread->enable_wx(WXExec));

  // Perform any platform dependent FPU setup
  os::setup_fpu();

  return JNI_OK;
}


jint JNICALL jni_AttachCurrentThread(JavaVM *vm, void **penv, void *_args) {
  HOTSPOT_JNI_ATTACHCURRENTTHREAD_ENTRY(vm, penv, _args);
  if (vm_created == 0) {
    HOTSPOT_JNI_ATTACHCURRENTTHREAD_RETURN((uint32_t) JNI_ERR);
    return JNI_ERR;
  }

  jint ret = attach_current_thread(vm, penv, _args, false);
  HOTSPOT_JNI_ATTACHCURRENTTHREAD_RETURN(ret);
  return ret;
}


jint JNICALL jni_DetachCurrentThread(JavaVM *vm)  {
  HOTSPOT_JNI_DETACHCURRENTTHREAD_ENTRY(vm);
  if (vm_created == 0) {
    HOTSPOT_JNI_DETACHCURRENTTHREAD_RETURN(JNI_ERR);
    return JNI_ERR;
  }

  Thread* current = Thread::current_or_null();

  // If the thread has already been detached the operation is a no-op
  if (current == NULL) {
    HOTSPOT_JNI_DETACHCURRENTTHREAD_RETURN(JNI_OK);
    return JNI_OK;
  }

  // If executing from an atexit hook we may be in the VMThread.
  if (!current->is_Java_thread()) {
    HOTSPOT_JNI_DETACHCURRENTTHREAD_RETURN((uint32_t) JNI_ERR);
    return JNI_ERR;
  }

  VM_Exit::block_if_vm_exited();

  JavaThread* thread = JavaThread::cast(current);
  if (thread->has_last_Java_frame()) {
    HOTSPOT_JNI_DETACHCURRENTTHREAD_RETURN((uint32_t) JNI_ERR);
    // Can't detach a thread that's running java, that can't work.
    return JNI_ERR;
  }

  // We are going to VM, change W^X state to the expected one.
  MACOS_AARCH64_ONLY(thread->enable_wx(WXWrite));

  // Safepoint support. Have to do call-back to safepoint code, if in the
  // middle of a safepoint operation
  ThreadStateTransition::transition_from_native(thread, _thread_in_vm);

  // XXX: Note that JavaThread::exit() call below removes the guards on the
  // stack pages set up via enable_stack_{red,yellow}_zone() calls
  // above in jni_AttachCurrentThread. Unfortunately, while the setting
  // of the guards is visible in jni_AttachCurrentThread above,
  // the removal of the guards is buried below in JavaThread::exit()
  // here. The abstraction should be more symmetrically either exposed
  // or hidden (e.g. it could probably be hidden in the same
  // (platform-dependent) methods where we do alternate stack
  // maintenance work?)
  thread->exit(false, JavaThread::jni_detach);
  thread->smr_delete();

  // Go to the execute mode, the initial state of the thread on creation.
  // Use os interface as the thread is not a JavaThread anymore.
  MACOS_AARCH64_ONLY(os::current_thread_enable_wx(WXExec));

  HOTSPOT_JNI_DETACHCURRENTTHREAD_RETURN(JNI_OK);
  return JNI_OK;
}

DT_RETURN_MARK_DECL(GetEnv, jint
                    , HOTSPOT_JNI_GETENV_RETURN(_ret_ref));

jint JNICALL jni_GetEnv(JavaVM *vm, void **penv, jint version) {
  HOTSPOT_JNI_GETENV_ENTRY(vm, penv, version);
  jint ret = JNI_ERR;
  DT_RETURN_MARK(GetEnv, jint, (const jint&)ret);

  if (vm_created == 0) {
    *penv = NULL;
    ret = JNI_EDETACHED;
    return ret;
  }

  if (JniExportedInterface::GetExportedInterface(vm, penv, version, &ret)) {
    return ret;
  }

#ifndef JVMPI_VERSION_1
// need these in order to be polite about older agents
#define JVMPI_VERSION_1   ((jint)0x10000001)
#define JVMPI_VERSION_1_1 ((jint)0x10000002)
#define JVMPI_VERSION_1_2 ((jint)0x10000003)
#endif // !JVMPI_VERSION_1

  Thread* thread = Thread::current_or_null();
  if (thread != NULL && thread->is_Java_thread()) {
    if (Threads::is_supported_jni_version_including_1_1(version)) {
      *(JNIEnv**)penv = JavaThread::cast(thread)->jni_environment();
      ret = JNI_OK;
      return ret;

    } else if (version == JVMPI_VERSION_1 ||
               version == JVMPI_VERSION_1_1 ||
               version == JVMPI_VERSION_1_2) {
      tty->print_cr("ERROR: JVMPI, an experimental interface, is no longer supported.");
      tty->print_cr("Please use the supported interface: the JVM Tool Interface (JVM TI).");
      ret = JNI_EVERSION;
      return ret;
    } else if (JvmtiExport::is_jvmdi_version(version)) {
      tty->print_cr("FATAL ERROR: JVMDI is no longer supported.");
      tty->print_cr("Please use the supported interface: the JVM Tool Interface (JVM TI).");
      ret = JNI_EVERSION;
      return ret;
    } else {
      *penv = NULL;
      ret = JNI_EVERSION;
      return ret;
    }
  } else {
    *penv = NULL;
    ret = JNI_EDETACHED;
    return ret;
  }
}


jint JNICALL jni_AttachCurrentThreadAsDaemon(JavaVM *vm, void **penv, void *_args) {
  HOTSPOT_JNI_ATTACHCURRENTTHREADASDAEMON_ENTRY(vm, penv, _args);
  if (vm_created == 0) {
  HOTSPOT_JNI_ATTACHCURRENTTHREADASDAEMON_RETURN((uint32_t) JNI_ERR);
    return JNI_ERR;
  }

  jint ret = attach_current_thread(vm, penv, _args, true);
  HOTSPOT_JNI_ATTACHCURRENTTHREADASDAEMON_RETURN(ret);
  return ret;
}


} // End extern "C"

const struct JNIInvokeInterface_ jni_InvokeInterface = {
    NULL,
    NULL,
    NULL,

    jni_DestroyJavaVM,
    jni_AttachCurrentThread,
    jni_DetachCurrentThread,
    jni_GetEnv,
    jni_AttachCurrentThreadAsDaemon
};
