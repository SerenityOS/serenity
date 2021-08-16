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
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/linkResolver.hpp"
#include "jvmci/jniAccessMark.inline.hpp"
#include "jvmci/jvmciJavaClasses.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/java.hpp"

// ------------------------------------------------------------------

oop HotSpotJVMCI::resolve(JVMCIObject obj) {
  return JNIHandles::resolve(obj.as_jobject());
}

arrayOop HotSpotJVMCI::resolve(JVMCIArray obj) {
  return (arrayOop) JNIHandles::resolve(obj.as_jobject());
}

objArrayOop HotSpotJVMCI::resolve(JVMCIObjectArray obj) {
  return (objArrayOop) JNIHandles::resolve(obj.as_jobject());
}

typeArrayOop HotSpotJVMCI::resolve(JVMCIPrimitiveArray obj) {
  return (typeArrayOop) JNIHandles::resolve(obj.as_jobject());
}

JVMCIObject HotSpotJVMCI::wrap(oop obj) {
  assert(Thread::current()->is_Java_thread(), "must be");
  return JVMCIObject(JNIHandles::make_local(obj), true);
}

/**
 * Computes the field offset of a static or instance field.
 * It looks up the name and signature symbols without creating new ones;
 * all the symbols of these classes need to be already loaded.
 */
void HotSpotJVMCI::compute_offset(int &dest_offset, Klass* klass, const char* name, const char* signature, bool static_field, TRAPS) {
  InstanceKlass* ik = InstanceKlass::cast(klass);
  Symbol* name_symbol = SymbolTable::probe(name, (int)strlen(name));
  Symbol* signature_symbol = SymbolTable::probe(signature, (int)strlen(signature));
  if (name_symbol == NULL || signature_symbol == NULL) {
#ifndef PRODUCT
    ik->print_on(tty);
#endif
    fatal("symbol with name %s and signature %s was not found in symbol table (klass=%s)", name, signature, klass->name()->as_C_string());
  }

  fieldDescriptor fd;
  if (!ik->find_field(name_symbol, signature_symbol, &fd)) {
    ResourceMark rm;
    fatal("Could not find field %s.%s with signature %s", ik->external_name(), name, signature);
  }
  guarantee(fd.is_static() == static_field, "static/instance mismatch");
  dest_offset = fd.offset();
  assert(dest_offset != 0, "must be valid offset");
  if (static_field) {
    // Must ensure classes for static fields are initialized as the
    // accessor itself does not include a class initialization check.
    ik->initialize(CHECK);
  }
  JVMCI_event_2("   field offset for %s %s.%s = %d", signature, ik->external_name(), name, dest_offset);
}

#ifndef PRODUCT
static void check_resolve_method(const char* call_type, Klass* resolved_klass, Symbol* method_name, Symbol* method_signature, TRAPS) {
  Method* method = NULL;
  LinkInfo link_info(resolved_klass, method_name, method_signature, NULL, LinkInfo::AccessCheck::skip, LinkInfo::LoaderConstraintCheck::skip);
  if (strcmp(call_type, "call_static") == 0) {
    method = LinkResolver::resolve_static_call_or_null(link_info);
  } else if (strcmp(call_type, "call_virtual") == 0) {
    method = LinkResolver::resolve_virtual_call_or_null(resolved_klass, link_info);
  } else if (strcmp(call_type, "call_special") == 0) {
    method = LinkResolver::resolve_special_call_or_null(link_info);
  } else {
    fatal("Unknown or unsupported call type: %s", call_type);
  }
  if (method == NULL) {
    fatal("Could not resolve %s.%s%s", resolved_klass->external_name(), method_name->as_C_string(), method_signature->as_C_string());
  }
}
#endif

jclass JNIJVMCI::_box_classes[T_CONFLICT+1];
jclass JNIJVMCI::_byte_array;
jfieldID JNIJVMCI::_box_fields[T_CONFLICT+1];
jmethodID JNIJVMCI::_box_constructors[T_CONFLICT+1];
jmethodID JNIJVMCI::_Class_getName_method;

jmethodID JNIJVMCI::_HotSpotResolvedJavaMethodImpl_fromMetaspace_method;
jmethodID JNIJVMCI::_HotSpotConstantPool_fromMetaspace_method;
jmethodID JNIJVMCI::_HotSpotResolvedObjectTypeImpl_fromMetaspace_method;
jmethodID JNIJVMCI::_HotSpotResolvedPrimitiveType_fromMetaspace_method;

#define START_CLASS(className, fullClassName)                          { \
  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::fullClassName(), true, CHECK); \
  className::_klass = InstanceKlass::cast(k);                                     \
  JVMCI_event_2(" klass for %s = " PTR_FORMAT, k->external_name(), p2i(k));       \
  className::_klass->initialize(CHECK);

#define END_CLASS }

#define FIELD(className, name, signature, static_field) compute_offset(className::_##name##_offset, className::_klass, #name, signature, static_field, CHECK);
#define CHAR_FIELD(className, name) FIELD(className, name, "C", false)
#define INT_FIELD(className, name) FIELD(className, name, "I", false)
#define BOOLEAN_FIELD(className, name) FIELD(className, name, "Z", false)
#define LONG_FIELD(className, name) FIELD(className, name, "J", false)
#define FLOAT_FIELD(className, name) FIELD(className, name, "F", false)
#define OBJECT_FIELD(className, name, signature) FIELD(className, name, signature, false)
#define STATIC_OBJECT_FIELD(className, name, signature) FIELD(className, name, signature, true)
#define STATIC_INT_FIELD(className, name) FIELD(className, name, "I", true)
#define STATIC_BOOLEAN_FIELD(className, name) FIELD(className, name, "Z", true)
#ifdef PRODUCT
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)
#else
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args) \
  check_resolve_method(#hsCallType, k, vmSymbols::methodName##_name(), vmSymbols::signatureSymbolName(), CHECK);
#define CONSTRUCTOR(className, signature) { \
  TempNewSymbol sig = SymbolTable::new_symbol(signature); \
  check_resolve_method("call_special", k, vmSymbols::object_initializer_name(), sig, CHECK); \
  }
#endif
/**
 * Computes and initializes the offsets used by HotSpotJVMCI.
 */
void HotSpotJVMCI::compute_offsets(TRAPS) {
  JVMCI_CLASSES_DO(START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, OBJECT_FIELD, OBJECT_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECT_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)
}

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
#undef STATIC_FIELD
#undef STATIC_OBJECT_FIELD
#undef STATIC_OBJECTARRAY_FIELD
#undef STATIC_INT_FIELD
#undef STATIC_BOOLEAN_FIELD
#undef EMPTY_CAST

// ------------------------------------------------------------------

#define START_CLASS(className, fullClassName)                                           \
  void HotSpotJVMCI::className::initialize(JVMCI_TRAPS) {                               \
    JavaThread* THREAD = JavaThread::current(); /* For exception macros. */             \
    className::klass()->initialize(CHECK);                                              \
  }                                                                                     \
  bool HotSpotJVMCI::className::is_instance(JVMCIEnv* env, JVMCIObject object) {        \
    return resolve(object)->is_a(className::klass());                                   \
  }                                                                                     \
  void HotSpotJVMCI::className::check(oop obj, const char* field_name, int offset) {    \
    assert(obj != NULL, "NULL field access of %s.%s", #className, field_name); \
    assert(obj->is_a(className::klass()), "wrong class, " #className " expected, found %s", obj->klass()->external_name()); \
    assert(offset != 0, "must be valid offset");                                        \
  }                                                                                     \
  InstanceKlass* HotSpotJVMCI::className::_klass = NULL;

#define END_CLASS

#define FIELD(className, name, type, accessor, cast)                     \
  type HotSpotJVMCI::className::name(JVMCIEnv* env, oop obj)               { className::check(obj, #name, className::_##name##_offset); return cast obj->accessor(className::_##name##_offset); } \
  void HotSpotJVMCI::className::set_##name(JVMCIEnv* env, oop obj, type x) { className::check(obj, #name, className::_##name##_offset); obj->accessor##_put(className::_##name##_offset, x); }

#define EMPTY_CAST
#define CHAR_FIELD(className, name) FIELD(className, name, jchar, char_field, EMPTY_CAST)
#define INT_FIELD(className, name) FIELD(className, name, jint, int_field, EMPTY_CAST)
#define BOOLEAN_FIELD(className, name) FIELD(className, name, jboolean, bool_field, EMPTY_CAST)
#define LONG_FIELD(className, name) FIELD(className, name, jlong, long_field, EMPTY_CAST)
#define FLOAT_FIELD(className, name) FIELD(className, name, jfloat, float_field, EMPTY_CAST)

#define OBJECT_FIELD(className, name, signature) FIELD(className, name, oop, obj_field, EMPTY_CAST)
#define OBJECTARRAY_FIELD(className, name, signature) FIELD(className, name, objArrayOop, obj_field, (objArrayOop))
#define PRIMARRAY_FIELD(className, name, signature) FIELD(className, name, typeArrayOop, obj_field, (typeArrayOop))
#define STATIC_OBJECT_FIELD(className, name, signature) STATIC_OOPISH_FIELD(className, name, oop)
#define STATIC_OBJECTARRAY_FIELD(className, name, signature) STATIC_OOPISH_FIELD(className, name, objArrayOop)
#define STATIC_OOPISH_FIELD(className, name, type)                                                                        \
    type HotSpotJVMCI::className::name(JVMCIEnv* env) {                                                                   \
      assert(className::klass() != NULL && className::klass()->is_linked(), "Class not yet linked: " #className);         \
      InstanceKlass* ik = className::klass();                                                                             \
      oop base = ik->static_field_base_raw();                                                                             \
      oop result = HeapAccess<>::oop_load_at(base, className::_##name##_offset);                                          \
      return type(result);                                                                                                \
    }                                                                                                                     \
    void HotSpotJVMCI::className::set_##name(JVMCIEnv* env, type x) {                                                     \
      assert(className::klass() != NULL && className::klass()->is_linked(), "Class not yet linked: " #className);         \
      assert(className::klass() != NULL, "Class not yet loaded: " #className);                                            \
      InstanceKlass* ik = className::klass();                                                                             \
      oop base = ik->static_field_base_raw();                                                                             \
      HeapAccess<>::oop_store_at(base, className::_##name##_offset, x);                                                   \
    }
#define STATIC_PRIMITIVE_FIELD(className, name, jtypename)                                                                \
    jtypename HotSpotJVMCI::className::get_##name(JVMCIEnv* env) {                                                        \
      assert(className::klass() != NULL && className::klass()->is_linked(), "Class not yet linked: " #className);         \
      InstanceKlass* ik = className::klass();                                                                             \
      oop base = ik->static_field_base_raw();                                                                             \
      return HeapAccess<>::load_at(base, className::_##name##_offset);                                                    \
    }                                                                                                                     \
    void HotSpotJVMCI::className::set_##name(JVMCIEnv* env, jtypename x) {                                                \
      assert(className::klass() != NULL && className::klass()->is_linked(), "Class not yet linked: " #className);         \
      InstanceKlass* ik = className::klass();                                                                             \
      oop base = ik->static_field_base_raw();                                                                             \
      HeapAccess<>::store_at(base, _##name##_offset, x);                                                                  \
    }

#define STATIC_INT_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jint)
#define STATIC_BOOLEAN_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jboolean)
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)

/**
 * Generates the method and field definitions for the classes in HotSpotJVMCI. For example:
 *
 * void HotSpotJVMCI::Architecture::initialize(JVMCIEnv* env) { ... }
 * bool HotSpotJVMCI::Architecture::is_instance(JVMCIEnv* env, JVMCIObject object) { ... }
 * void HotSpotJVMCI::Architecture::check(oop obj, const char* field_name, int offset) { ... }
 *  oop HotSpotJVMCI::Architecture::wordKind(JVMCIEnv* env, oop obj) { ... }
 * void HotSpotJVMCI::Architecture::set_wordKind(JVMCIEnv* env, oop obj, oop x) { ... }
 *
 * InstanceKlass *HotSpotJVMCI::Architecture::_klass = NULL;
 */
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
#undef STATIC_PRIMITIVE_FIELD
#undef EMPTY_CAST

/**
 * Initializes the JNI id of a field. As per the JNI specification,
 * this ensures the declaring class is initialized.
 */
void JNIJVMCI::initialize_field_id(JNIEnv* env, jfieldID &fieldid, jclass clazz, const char* class_name, const char* name, const char* signature, bool static_field) {
  if (JVMCILibDumpJNIConfig != NULL) {
    fileStream* st = JVMCIGlobals::get_jni_config_file();
    st->print_cr("field %s %s %s", class_name, name, signature);
    return;
  }
  if (env->ExceptionCheck()) {
    return;
  }
  jfieldID current = fieldid;
  if (static_field) {
    // Class initialization barrier
    fieldid = env->GetStaticFieldID(clazz, name, signature);
  } else {
    // Class initialization barrier
    fieldid = env->GetFieldID(clazz, name, signature);
  }
  JVMCI_event_2("   jfieldID for %s %s.%s = " PTR_FORMAT, signature, class_name, name, p2i(fieldid));

  if (env->ExceptionCheck()) {
    env->ExceptionDescribe();
    env->ExceptionClear();
    ResourceMark rm;
    fatal("Could not find field %s.%s with signature %s", class_name, name, signature);
  }
}

#define START_CLASS(className, fullClassName) {                                             \
  current_class_name = vmSymbols::fullClassName()->as_C_string();                           \
  if (JVMCILibDumpJNIConfig != NULL) {                                                      \
    fileStream* st = JVMCIGlobals::get_jni_config_file();                                   \
    st->print_cr("class %s", current_class_name);                                           \
  } else {                                                                                  \
    jclass k = env->FindClass(current_class_name);                                          \
    JVMCI_EXCEPTION_CHECK(env, "FindClass(%s)", current_class_name);                        \
    assert(k != NULL, #fullClassName " not initialized");                                   \
    k = (jclass) env->NewGlobalRef(k);                                                      \
    JVMCI_event_2(" jclass for %s = " PTR_FORMAT, current_class_name, p2i(k));              \
    className::_class = k;                                                                  \
  }

#define END_CLASS current_class_name = NULL; }

#define FIELD(className, name, signature, static_field) initialize_field_id(env, className::_##name##_field_id, className::_class, current_class_name, #name, signature, static_field);
#define CHAR_FIELD(className, name) FIELD(className, name, "C", false)
#define INT_FIELD(className, name) FIELD(className, name, "I", false)
#define BOOLEAN_FIELD(className, name) FIELD(className, name, "Z", false)
#define LONG_FIELD(className, name) FIELD(className, name, "J", false)
#define FLOAT_FIELD(className, name) FIELD(className, name, "F", false)
#define OBJECT_FIELD(className, name, signature) FIELD(className, name, signature, false)
#define STATIC_OBJECT_FIELD(className, name, signature) FIELD(className, name, signature, true)
#define STATIC_INT_FIELD(className, name) FIELD(className, name, "I", true)
#define STATIC_BOOLEAN_FIELD(className, name) FIELD(className, name, "Z", true)

#define GET_JNI_METHOD(jniGetMethod, dst, clazz, methodName, signature)                        \
    if (JVMCILibDumpJNIConfig != NULL) {                                                       \
      fileStream* st = JVMCIGlobals::get_jni_config_file();                                    \
      st->print_cr("method %s %s %s", current_class_name, methodName, signature);              \
    } else {                                                                                   \
      jmethodID current = dst;                                                                 \
      dst = env->jniGetMethod(clazz, methodName, signature);                                   \
      JVMCI_EXCEPTION_CHECK(env, #jniGetMethod "(%s.%s%s)",                                    \
                  current_class_name, methodName, signature);                                  \
      assert(dst != NULL, "uninitialized");                                                    \
      JVMCI_event_2("   jmethodID for %s.%s%s = " PTR_FORMAT,                                  \
                  current_class_name, methodName, signature, p2i(dst));                        \
    }

#define GET_JNI_CONSTRUCTOR(clazz, signature) \
  GET_JNI_METHOD(GetMethodID, JNIJVMCI::clazz::_constructor, clazz::_class, "<init>", signature) \

#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args) \
     GET_JNI_METHOD(jniGetMethod,                                        \
                    className::_##methodName##_method,                   \
                    className::clazz(),                                  \
                    vmSymbols::methodName##_name()->as_C_string(),       \
                    vmSymbols::signatureSymbolName()->as_C_string())

#define CONSTRUCTOR(className, signature) \
  GET_JNI_CONSTRUCTOR(className, signature)

extern "C" {
  void     JNICALL JVM_RegisterJVMCINatives(JNIEnv *env, jclass compilerToVMClass);
  jobject  JNICALL JVM_GetJVMCIRuntime(JNIEnv *env, jclass c);
}

// Dumps symbols for public <init>() and <init>(String) methods of
// non-abstract Throwable subtypes known by the VM. This is to
// support the use of reflection in jdk.vm.ci.hotspot.TranslatedException.create().
class ThrowableInitDumper : public SymbolClosure {
 private:
  fileStream* _st;
 public:
  ThrowableInitDumper(fileStream* st)     { _st = st; }
  void do_symbol(Symbol** p) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    Symbol* name = *p;
    if (name == NULL) {
      return;
    }
    Klass* k = SystemDictionary::resolve_or_null(name, CHECK_EXIT);
    if (k != NULL && k->is_instance_klass()) {
      InstanceKlass* iklass = InstanceKlass::cast(k);
      if (iklass->is_subclass_of(vmClasses::Throwable_klass()) && iklass->is_public() && !iklass->is_abstract()) {
        const char* class_name = NULL;
        Array<Method*>* methods = iklass->methods();
        for (int i = 0; i < methods->length(); i++) {
          Method* m = methods->at(i);
          if (m->name() == vmSymbols::object_initializer_name() &&
              m->is_public() &&
              (m->signature() == vmSymbols::void_method_signature() || m->signature() == vmSymbols::string_void_signature())) {
            if (class_name == NULL) {
              class_name = name->as_C_string();
              _st->print_cr("class %s", class_name);
            }
            _st->print_cr("method %s %s %s", class_name, m->name()->as_C_string(), m->signature()->as_C_string());
          }
        }
      }
    }
  }
};

#define IN_CLASS(fullClassName) current_class_name = vmSymbols::fullClassName()->as_C_string()
/**
 * Initializes the JNI method and field ids used in JNIJVMCI.
 */
void JNIJVMCI::initialize_ids(JNIEnv* env) {
  ResourceMark rm;
  const char* current_class_name = NULL;
  JVMCI_CLASSES_DO(START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, OBJECT_FIELD, OBJECT_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECT_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)

  IN_CLASS(java_lang_Class);
  GET_JNI_METHOD(GetMethodID, _Class_getName_method, Class::_class, "getName", "()Ljava/lang/String;");

  IN_CLASS(jdk_vm_ci_hotspot_HotSpotResolvedPrimitiveType);
  GET_JNI_METHOD(GetStaticMethodID, _HotSpotResolvedPrimitiveType_fromMetaspace_method, HotSpotResolvedPrimitiveType::_class,
                                                                                          vmSymbols::fromMetaspace_name()->as_C_string(),
                                                                                          vmSymbols::primitive_fromMetaspace_signature()->as_C_string());
  IN_CLASS(jdk_vm_ci_hotspot_HotSpotResolvedObjectTypeImpl);
  GET_JNI_METHOD(GetStaticMethodID, _HotSpotResolvedObjectTypeImpl_fromMetaspace_method, HotSpotResolvedObjectTypeImpl::_class,
                                                                                           vmSymbols::fromMetaspace_name()->as_C_string(),
                                                                                           vmSymbols::klass_fromMetaspace_signature()->as_C_string());
  IN_CLASS(jdk_vm_ci_hotspot_HotSpotConstantPool);
  GET_JNI_METHOD(GetStaticMethodID, _HotSpotConstantPool_fromMetaspace_method, HotSpotConstantPool::_class,
                                                                                  vmSymbols::fromMetaspace_name()->as_C_string(),
                                                                                  vmSymbols::constantPool_fromMetaspace_signature()->as_C_string());
  IN_CLASS(jdk_vm_ci_hotspot_HotSpotResolvedJavaMethodImpl);
  GET_JNI_METHOD(GetStaticMethodID, _HotSpotResolvedJavaMethodImpl_fromMetaspace_method, HotSpotResolvedJavaMethodImpl::_class,
                                                                                           vmSymbols::fromMetaspace_name()->as_C_string(),
                                                                                           vmSymbols::method_fromMetaspace_signature()->as_C_string());

#define BOX_CLASSES(generate)     \
  generate(Boolean, T_BOOLEAN, Z) \
  generate(Byte, T_BYTE, B)       \
  generate(Character, T_CHAR, C)  \
  generate(Short, T_SHORT, S)     \
  generate(Integer, T_INT, I)     \
  generate(Long, T_LONG, J)       \
  generate(Float, T_FLOAT, F)     \
  generate(Double, T_DOUBLE, D)   \

#define DO_BOX_CLASS(klass, basicType, type) \
  current_class_name = "java/lang/" #klass;                                                                       \
  if (JVMCILibDumpJNIConfig == NULL) {                                                                            \
    _box_classes[basicType] = env->FindClass("java/lang/" #klass);                                                \
    JVMCI_EXCEPTION_CHECK(env, "FindClass(%s)", #klass);                                                          \
    _box_classes[basicType] = (jclass) env->NewGlobalRef(_box_classes[basicType]);                                \
    assert(_box_classes[basicType] != NULL, "uninitialized");                                                     \
    _box_fields[basicType] = env->GetFieldID(_box_classes[basicType], "value", #type);                            \
    JVMCI_EXCEPTION_CHECK(env, "GetFieldID(%s, value, %s)", #klass, #type);                                       \
    GET_JNI_METHOD(GetMethodID, _box_constructors[basicType], _box_classes[basicType], "<init>", "(" #type ")V"); \
  } else {                                                                                                        \
    fileStream* st = JVMCIGlobals::get_jni_config_file();                                                         \
    st->print_cr("field %s value %s", current_class_name, #type);                                                 \
    st->print_cr("method %s <init> (%s)V", current_class_name, #type);                                            \
  }

  BOX_CLASSES(DO_BOX_CLASS);

  if (JVMCILibDumpJNIConfig == NULL) {
    _byte_array = env->FindClass("[B");
    JVMCI_EXCEPTION_CHECK(env, "FindClass([B)");
    _byte_array = (jclass) env->NewGlobalRef(_byte_array);
    assert(_byte_array != NULL, "uninitialized");
  } else {
    fileStream* st = JVMCIGlobals::get_jni_config_file();
    st->print_cr("class [B");
  }

#define DUMP_ALL_NATIVE_METHODS(class_symbol) do {                                                                  \
  current_class_name = class_symbol->as_C_string();                                                                 \
  Klass* k = SystemDictionary::resolve_or_fail(class_symbol, true, CHECK_EXIT);                                     \
  InstanceKlass* iklass = InstanceKlass::cast(k);                                                                   \
  Array<Method*>* methods = iklass->methods();                                                                      \
  for (int i = 0; i < methods->length(); i++) {                                                                     \
    Method* m = methods->at(i);                                                                                     \
    if (m->is_native()) {                                                                                           \
      st->print_cr("method %s %s %s", current_class_name, m->name()->as_C_string(), m->signature()->as_C_string()); \
    }                                                                                                               \
  }                                                                                                                 \
} while(0)

  if (JVMCILibDumpJNIConfig != NULL) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    fileStream* st = JVMCIGlobals::get_jni_config_file();

    DUMP_ALL_NATIVE_METHODS(vmSymbols::jdk_vm_ci_hotspot_CompilerToVM());
    ThrowableInitDumper dumper(st);
    vmSymbols::symbols_do(&dumper);

    st->flush();
    tty->print_cr("Dumped JVMCI shared library JNI configuration to %s", JVMCILibDumpJNIConfig);
    vm_exit(0);
  }

#undef DUMP_ALL_NATIVE_METHODS
#undef DO_BOX_CLASS
#undef BOX_CLASSES
#undef IN_CLASS

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &(f))
}

static void register_natives_for_class(JNIEnv* env, jclass clazz, const char* name, const JNINativeMethod *methods, jint nMethods) {
  if (clazz == NULL) {
    clazz = env->FindClass(name);
    if (env->ExceptionCheck()) {
      env->ExceptionDescribe();
      fatal("Could not find class %s", name);
    }
  }
  env->RegisterNatives(clazz, methods, nMethods);
  if (env->ExceptionCheck()) {
    env->ExceptionDescribe();
    fatal("Failure registering natives for %s", name);
  }
}

void JNIJVMCI::register_natives(JNIEnv* env) {
  if (env != JavaThread::current()->jni_environment()) {
    JNINativeMethod CompilerToVM_nmethods[] = {{ CC"registerNatives", CC"()V", FN_PTR(JVM_RegisterJVMCINatives) }};
    JNINativeMethod JVMCI_nmethods[] = {{ CC"initializeRuntime",   CC"()Ljdk/vm/ci/runtime/JVMCIRuntime;", FN_PTR(JVM_GetJVMCIRuntime) }};

    register_natives_for_class(env, NULL, "jdk/vm/ci/hotspot/CompilerToVM", CompilerToVM_nmethods, 1);
    register_natives_for_class(env, JVMCI::clazz(), "jdk/vm/ci/runtime/JVMCI", JVMCI_nmethods, 1);
  }
}

#undef METHOD
#undef CONSTRUCTOR
#undef FIELD2

#define EMPTY0
#define EMPTY1(x)
#define EMPTY2(x,y)
#define FIELD3(className, name, sig) FIELD2(className, name)
#define FIELD2(className, name) \
  jfieldID JNIJVMCI::className::_##name##_field_id = 0; \
  int HotSpotJVMCI::className::_##name##_offset = 0;
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)

// Generates the definitions of static fields used by the accessors. For example:
//  jfieldID JNIJVMCI::Architecture::_wordKind_field_id = 0;
//  jfieldID HotSpotJVMCI::Architecture::_wordKind_offset = 0;
JVMCI_CLASSES_DO(EMPTY2, EMPTY0, FIELD2, FIELD2, FIELD2, FIELD2, FIELD2, FIELD3, FIELD3, FIELD3, FIELD3, FIELD3, FIELD2, FIELD2, METHOD, CONSTRUCTOR)

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
#undef STATIC_FIELD
#undef STATIC_OBJECT_FIELD
#undef STATIC_OBJECTARRAY_FIELD
#undef STATIC_INT_FIELD
#undef STATIC_BOOLEAN_FIELD
#undef EMPTY_CAST


#define START_CLASS(className, fullClassName)                                                                                     \
  void JNIJVMCI::className::initialize(JVMCI_TRAPS) {                                                                             \
    /* should already be initialized */                                                                                           \
  }                                                                                                                               \
  bool JNIJVMCI::className::is_instance(JVMCIEnv* jvmciEnv, JVMCIObject object) {                                                 \
    JNIAccessMark jni(jvmciEnv);                                                                                                  \
    return jni()->IsInstanceOf(object.as_jobject(), className::clazz()) != 0;                                                     \
  }                                                                                                                               \
  void JNIJVMCI::className::check(JVMCIEnv* jvmciEnv, JVMCIObject obj, const char* field_name, jfieldID offset) {                 \
    assert(obj.is_non_null(), "NULL field access of %s.%s", #className, field_name);                                     \
    assert(jvmciEnv->isa_##className(obj), "wrong class, " #className " expected, found %s", jvmciEnv->klass_name(obj)); \
    assert(offset != 0, "must be valid offset");                                                                                  \
  }                                                                                                                               \
  jclass JNIJVMCI::className::_class = NULL;

#define END_CLASS

#define FIELD(className, name, type, accessor, cast)                                                                \
  type JNIJVMCI::className::get_##name(JVMCIEnv* jvmciEnv, JVMCIObject obj) {                                       \
   className::check(jvmciEnv, obj, #name, className::_##name##_field_id);                                           \
   JNIAccessMark jni(jvmciEnv);                               \
   return cast jni()->Get##accessor##Field(resolve_handle(obj), className::_##name##_field_id); \
  }                                                                                                                 \
  void JNIJVMCI::className::set_##name(JVMCIEnv* jvmciEnv, JVMCIObject obj, type x) {                               \
    className::check(jvmciEnv, obj, #name, className::_##name##_field_id);                                          \
    JNIAccessMark jni(jvmciEnv); \
    jni()->Set##accessor##Field(resolve_handle(obj), className::_##name##_field_id, x);         \
  } \

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

#define OOPISH_FIELD(className, name, type, accessor, cast)                                             \
  type JNIJVMCI::className::get_##name(JVMCIEnv* jvmciEnv, JVMCIObject obj) {                           \
    className::check(jvmciEnv, obj, #name, className::_##name##_field_id);                              \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    return cast wrap(jni()->Get##accessor##Field(resolve_handle(obj), className::_##name##_field_id));  \
  }                                                                                                     \
  void JNIJVMCI::className::set_##name(JVMCIEnv* jvmciEnv, JVMCIObject obj, type x) {                   \
    className::check(jvmciEnv, obj, #name, className::_##name##_field_id);                              \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    jni()->Set##accessor##Field(resolve_handle(obj), className::_##name##_field_id, resolve_handle(x)); \
  }

#define STATIC_OOPISH_FIELD(className, name, type, accessor, cast)                                      \
  type JNIJVMCI::className::get_##name(JVMCIEnv* jvmciEnv) {                                            \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    return cast wrap(jni()->GetStatic##accessor##Field(className::clazz(), className::_##name##_field_id));  \
  }                                                                                                     \
  void JNIJVMCI::className::set_##name(JVMCIEnv* jvmciEnv, type x) {                                    \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    jni()->SetStatic##accessor##Field(className::clazz(), className::_##name##_field_id, resolve_handle(x)); \
  }

#define STATIC_PRIMITIVE_FIELD(className, name, type, accessor, cast)                                   \
  type JNIJVMCI::className::get_##name(JVMCIEnv* jvmciEnv) {                                            \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    return cast jni()->GetStatic##accessor##Field(className::clazz(), className::_##name##_field_id);   \
  }                                                                                                     \
  void JNIJVMCI::className::set_##name(JVMCIEnv* jvmciEnv, type x) {                                    \
    JNIAccessMark jni(jvmciEnv);                                                                        \
    jni()->SetStatic##accessor##Field(className::clazz(), className::_##name##_field_id, x);            \
  }

#define STATIC_INT_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jint, Int, EMPTY_CAST)
#define STATIC_BOOLEAN_FIELD(className, name) STATIC_PRIMITIVE_FIELD(className, name, jboolean, Boolean, EMPTY_CAST)
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args) \
  jmethodID JNIJVMCI::className::_##methodName##_method;

#define CONSTRUCTOR(className, signature) \
  jmethodID JNIJVMCI::className::_constructor;

/**
 * Generates the method definitions for the classes in HotSpotJVMCI.
 */
JVMCI_CLASSES_DO(START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, PRIMARRAY_FIELD, OBJECTARRAY_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECTARRAY_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)

#undef METHOD
#undef CONSTRUCTOR
#undef START_CLASS
#undef END_CLASS
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
#undef STATIC_PRIMITIVE_FIELD
#undef OOPISH_FIELD
#undef EMPTY_CAST
