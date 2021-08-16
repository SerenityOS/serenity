/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JVMCI_JVMCIENV_HPP
#define SHARE_JVMCI_JVMCIENV_HPP

#include "classfile/javaClasses.hpp"
#include "jvmci/jvmciJavaClasses.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/thread.hpp"

class CompileTask;
class JVMCIObject;
class JVMCIObjectArray;
class JVMCIPrimitiveArray;
class JVMCICompiler;
class JVMCIRuntime;
class nmethodLocker;

#define JVMCI_EXCEPTION_CONTEXT \
  JavaThread* thread = JavaThread::current(); \
  JavaThread* THREAD = thread; // For exception macros.

// Helper to log more context on a JNI exception
#define JVMCI_EXCEPTION_CHECK(env, ...) \
  do { \
    if (env->ExceptionCheck()) { \
      if (env != JavaThread::current()->jni_environment()) { \
        char* sl_path; \
        if (::JVMCI::get_shared_library(sl_path, false) != NULL) { \
          tty->print_cr("In JVMCI shared library (%s):", sl_path); \
        } \
      } \
      tty->print_cr(__VA_ARGS__); \
      return; \
    } \
  } while(0)

// Helper class to ensure that references to Klass* are kept alive for G1
class JVMCIKlassHandle : public StackObj {
 private:
  Klass*     _klass;
  Handle     _holder;
  Thread*    _thread;

  Klass*        klass() const                     { return _klass; }
  Klass*        non_null_klass() const            { assert(_klass != NULL, "resolving NULL _klass"); return _klass; }

 public:
  /* Constructors */
  JVMCIKlassHandle (Thread* thread) : _klass(NULL), _thread(thread) {}
  JVMCIKlassHandle (Thread* thread, Klass* klass);

  JVMCIKlassHandle (const JVMCIKlassHandle &h): _klass(h._klass), _holder(h._holder), _thread(h._thread) {}
  JVMCIKlassHandle& operator=(const JVMCIKlassHandle &s);
  JVMCIKlassHandle& operator=(Klass* klass);

  /* Operators for ease of use */
  Klass*        operator () () const            { return klass(); }
  Klass*        operator -> () const            { return non_null_klass(); }

  bool    operator == (Klass* o) const          { return klass() == o; }
  bool    operator == (const JVMCIKlassHandle& h) const  { return klass() == h.klass(); }

  /* Null checks */
  bool    is_null() const                      { return _klass == NULL; }
  bool    not_null() const                     { return _klass != NULL; }
};

// A class that maintains the state needed for compilations requested
// by the CompileBroker.  It is created in the broker and passed through
// into the code installation step.
class JVMCICompileState : public ResourceObj {
  friend class JVMCIVMStructs;
 private:
  CompileTask*     _task;
  JVMCICompiler*   _compiler;

  // Cache JVMTI state. Defined as bytes so that reading them from Java
  // via Unsafe is well defined (the C++ type for bool is implementation
  // defined and may not be the same as a Java boolean).
  uint64_t _jvmti_redefinition_count;
  jbyte  _jvmti_can_hotswap_or_post_breakpoint;
  jbyte  _jvmti_can_access_local_variables;
  jbyte  _jvmti_can_post_on_exceptions;
  jbyte  _jvmti_can_pop_frame;
  bool   _target_method_is_old;

  // Compilation result values.
  bool             _retryable;
  const char*      _failure_reason;

  // Specifies if _failure_reason is on the C heap. If so, it is allocated
  // with the mtJVMCI NMT flag.
  bool             _failure_reason_on_C_heap;

  // A value indicating compilation activity during the compilation.
  // If successive calls to this method return a different value, then
  // some degree of JVMCI compilation occurred between the calls.
  jint             _compilation_ticks;

 public:
  JVMCICompileState(CompileTask* task, JVMCICompiler* compiler);

  CompileTask* task() { return _task; }

  bool  jvmti_state_changed() const;
  uint64_t jvmti_redefinition_count() const          { return  _jvmti_redefinition_count; }
  bool  jvmti_can_hotswap_or_post_breakpoint() const { return  _jvmti_can_hotswap_or_post_breakpoint != 0; }
  bool  jvmti_can_access_local_variables() const     { return  _jvmti_can_access_local_variables != 0; }
  bool  jvmti_can_post_on_exceptions() const         { return  _jvmti_can_post_on_exceptions != 0; }
  bool  jvmti_can_pop_frame() const                  { return  _jvmti_can_pop_frame != 0; }
  bool  target_method_is_old() const                 { return  _target_method_is_old; }

  const char* failure_reason() { return _failure_reason; }
  bool failure_reason_on_C_heap() { return _failure_reason_on_C_heap; }
  bool retryable() { return _retryable; }

  void set_failure(bool retryable, const char* reason, bool reason_on_C_heap = false) {
    _failure_reason = reason;
    _failure_reason_on_C_heap = reason_on_C_heap;
    _retryable = retryable;
  }

  jint compilation_ticks() const { return _compilation_ticks; }
  void inc_compilation_ticks();
};


// This class is a top level wrapper around interactions between HotSpot
// and the JVMCI Java code.  It supports both a HotSpot heap based
// runtime with HotSpot oop based accessors as well as a shared library
// based runtime that is accessed through JNI. It abstracts away all
// interactions with JVMCI objects so that a single version of the
// HotSpot C++ code can can work with either runtime.
class JVMCIEnv : public ResourceObj {
  friend class JNIAccessMark;

  // Initializes the _env, _mode and _runtime fields.
  void init_env_mode_runtime(JavaThread* thread, JNIEnv* parent_env);

  void init(JavaThread* thread, bool is_hotspot, const char* file, int line);

  JNIEnv*                 _env;  // JNI env for calling into shared library
  bool     _pop_frame_on_close;  // Must pop frame on close?
  bool        _detach_on_close;  // Must detach on close?
  JVMCIRuntime*       _runtime;  // Access to a HotSpotJVMCIRuntime
  bool             _is_hotspot;  // Which heap is the HotSpotJVMCIRuntime in
  bool        _throw_to_caller;  // Propagate an exception raised in this env to the caller?
  const char*            _file;  // The file and ...
  int                    _line;  // ... line where this JNIEnv was created

  // Translates an exception on the HotSpot heap to an exception on
  // the shared library heap. The translation includes the stack and
  // causes of `throwable`. The translated exception is pending in the
  // shared library thread upon returning.
  void translate_hotspot_exception_to_jni_exception(JavaThread* THREAD, const Handle& throwable);

public:
  // Opens a JVMCIEnv scope for a Java to VM call (e.g., via CompilerToVM).
  // An exception occurring within the scope is left pending when the
  // scope closes so that it will be propagated back to Java.
  // The JVMCIEnv destructor translates the exception object for the
  // Java runtime if necessary.
  JVMCIEnv(JavaThread* thread, JNIEnv* env, const char* file, int line);

  // Opens a JVMCIEnv scope for a compilation scheduled by the CompileBroker.
  // An exception occurring within the scope must not be propagated back to
  // the CompileBroker.
  JVMCIEnv(JavaThread* thread, JVMCICompileState* compile_state, const char* file, int line);

  // Opens a JNIEnv scope for a call from within the VM. An exception occurring
  // within the scope must not be propagated back to the caller.
  JVMCIEnv(JavaThread* env, const char* file, int line);

  // Opens a JNIEnv scope for accessing `for_object`. An exception occurring
  // within the scope must not be propagated back to the caller.
  JVMCIEnv(JavaThread* thread, JVMCIObject for_object, const char* file, int line) {
    // A JNI call to access an object in the shared library heap
    // can block or take a long time so do not allow such access
    // on the VM thread.
    assert(for_object.is_hotspot() || !Thread::current()->is_VM_thread(),
        "cannot open JVMCIEnv scope when in the VM thread for accessing a shared library heap object");
    init(thread, for_object.is_hotspot(), file, line);
  }

  // Opens a JNIEnv scope for the HotSpot runtime if `is_hotspot` is true
  // otherwise for the shared library runtime. An exception occurring
  // within the scope must not be propagated back to the caller.
  JVMCIEnv(JavaThread* thread, bool is_hotspot, const char* file, int line) {
    init(thread, is_hotspot, file, line);
  }

  ~JVMCIEnv();

  JVMCIRuntime* runtime() {
    return _runtime;
  }

  // Initializes Services.savedProperties in the shared library by copying
  // the values from the same field in the HotSpot heap.
  void copy_saved_properties();

  jboolean has_pending_exception();
  void clear_pending_exception();

  // Prints an exception and stack trace of a pending exception.
  void describe_pending_exception(bool clear);

  int get_length(JVMCIArray array);

  JVMCIObject get_object_at(JVMCIObjectArray array, int index);
  void put_object_at(JVMCIObjectArray array, int index, JVMCIObject value);

  jboolean get_bool_at(JVMCIPrimitiveArray array, int index);
  void put_bool_at(JVMCIPrimitiveArray array, int index, jboolean value);

  jbyte get_byte_at(JVMCIPrimitiveArray array, int index);
  void put_byte_at(JVMCIPrimitiveArray array, int index, jbyte value);

  jint get_int_at(JVMCIPrimitiveArray array, int index);
  void put_int_at(JVMCIPrimitiveArray array, int index, jint value);

  long get_long_at(JVMCIPrimitiveArray array, int index);
  void put_long_at(JVMCIPrimitiveArray array, int index, jlong value);

  void copy_bytes_to(JVMCIPrimitiveArray src, jbyte* dest, int offset, jsize length);
  void copy_bytes_from(jbyte* src, JVMCIPrimitiveArray dest, int offset, jsize length);

  void copy_longs_from(jlong* src, JVMCIPrimitiveArray dest, int offset, jsize length);

  JVMCIObjectArray initialize_intrinsics(JVMCI_TRAPS);

  jboolean is_boxing_object(BasicType type, JVMCIObject object);

  // Get the primitive value from a Java boxing object.  It's hard error to
  // pass a non-primitive BasicType.
  jvalue get_boxed_value(BasicType type, JVMCIObject object);

  // Return the BasicType of the object if it's a boxing object, otherwise return T_ILLEGAL.
  BasicType get_box_type(JVMCIObject object);

  // Create a boxing object of the appropriate primitive type.
  JVMCIObject create_box(BasicType type, jvalue* value, JVMCI_TRAPS);

  const char* as_utf8_string(JVMCIObject str);

  JVMCIObject create_string(Symbol* str, JVMCI_TRAPS) {
    JVMCIObject s = create_string(str->as_C_string(), JVMCI_CHECK_(JVMCIObject()));
    return s;
  }

  JVMCIObject create_string(const char* str, JVMCI_TRAPS);

  bool equals(JVMCIObject a, JVMCIObject b);

  // Convert into a JNI handle for the appropriate runtime
  jobject get_jobject(JVMCIObject object)                       { assert(object.as_jobject() == NULL || is_hotspot() == object.is_hotspot(), "mismatch"); return object.as_jobject(); }
  jarray get_jarray(JVMCIArray array)                           { assert(array.as_jobject() == NULL || is_hotspot() == array.is_hotspot(), "mismatch"); return array.as_jobject(); }
  jobjectArray get_jobjectArray(JVMCIObjectArray objectArray)   { assert(objectArray.as_jobject() == NULL || is_hotspot() == objectArray.is_hotspot(), "mismatch"); return objectArray.as_jobject(); }
  jbyteArray get_jbyteArray(JVMCIPrimitiveArray primitiveArray) { assert(primitiveArray.as_jobject() == NULL || is_hotspot() == primitiveArray.is_hotspot(), "mismatch"); return primitiveArray.as_jbyteArray(); }

  JVMCIObject         wrap(jobject obj);
  JVMCIObjectArray    wrap(jobjectArray obj)  { return (JVMCIObjectArray)    wrap((jobject) obj); }
  JVMCIPrimitiveArray wrap(jintArray obj)     { return (JVMCIPrimitiveArray) wrap((jobject) obj); }
  JVMCIPrimitiveArray wrap(jbooleanArray obj) { return (JVMCIPrimitiveArray) wrap((jobject) obj); }
  JVMCIPrimitiveArray wrap(jbyteArray obj)    { return (JVMCIPrimitiveArray) wrap((jobject) obj); }
  JVMCIPrimitiveArray wrap(jlongArray obj)    { return (JVMCIPrimitiveArray) wrap((jobject) obj); }

 private:
  JVMCIObject wrap(oop obj)                  { assert(is_hotspot(), "must be"); return wrap(JNIHandles::make_local(obj)); }
  JVMCIObjectArray wrap(objArrayOop obj)     { assert(is_hotspot(), "must be"); return (JVMCIObjectArray) wrap(JNIHandles::make_local(obj)); }
  JVMCIPrimitiveArray wrap(typeArrayOop obj) { assert(is_hotspot(), "must be"); return (JVMCIPrimitiveArray) wrap(JNIHandles::make_local(obj)); }

 public:
  // Compiles a method with the JVMCI compiler.
  // Caller must handle pending exception.
  JVMCIObject call_HotSpotJVMCIRuntime_compileMethod(JVMCIObject runtime, JVMCIObject method, int entry_bci,
                                                     jlong compile_state, int id);

  void call_HotSpotJVMCIRuntime_bootstrapFinished(JVMCIObject runtime, JVMCI_TRAPS);
  void call_HotSpotJVMCIRuntime_shutdown(JVMCIObject runtime);
  JVMCIObject call_HotSpotJVMCIRuntime_runtime(JVMCI_TRAPS);
  JVMCIObject call_JVMCI_getRuntime(JVMCI_TRAPS);
  JVMCIObject call_HotSpotJVMCIRuntime_getCompiler(JVMCIObject runtime, JVMCI_TRAPS);

  JVMCIObject call_HotSpotJVMCIRuntime_callToString(JVMCIObject object, JVMCI_TRAPS);

  JVMCIObject call_JavaConstant_forPrimitive(JVMCIObject kind, jlong value, JVMCI_TRAPS);

  jboolean call_HotSpotJVMCIRuntime_isGCSupported(JVMCIObject runtime, jint gcIdentifier);

  BasicType kindToBasicType(JVMCIObject kind, JVMCI_TRAPS);

#define DO_THROW(name) \
  void throw_##name(const char* msg = NULL);

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

  void fthrow_error(const char* file, int line, const char* format, ...) ATTRIBUTE_PRINTF(4, 5);

  // Given an instance of HotSpotInstalledCode return the corresponding CodeBlob*.  The
  // nmethodLocker is required to keep the CodeBlob alive in the case where it's an nmethod.
  CodeBlob* get_code_blob(JVMCIObject code, nmethodLocker& locker);

  // Given an instance of HotSpotInstalledCode return the corresponding nmethod.  The
  // nmethodLocker is required to keep the nmethod alive.
  nmethod* get_nmethod(JVMCIObject code, nmethodLocker& locker);

  MethodData* asMethodData(jlong metaspaceMethodData) {
    return (MethodData*) (address) metaspaceMethodData;
  }

  const char* klass_name(JVMCIObject object);

  // Unpack an instance of HotSpotResolvedJavaMethodImpl into the original Method*
  Method* asMethod(JVMCIObject jvmci_method);
  Method* asMethod(jobject jvmci_method) { return asMethod(wrap(jvmci_method)); }

  // Unpack an instance of HotSpotResolvedObjectTypeImpl into the original Klass*
  Klass* asKlass(JVMCIObject jvmci_type);
  Klass* asKlass(jobject jvmci_type)  { return asKlass(wrap(jvmci_type)); }

  JVMCIObject get_jvmci_method(const methodHandle& method, JVMCI_TRAPS);

  JVMCIObject get_jvmci_type(const JVMCIKlassHandle& klass, JVMCI_TRAPS);

  // Unpack an instance of HotSpotConstantPool into the original ConstantPool*
  ConstantPool* asConstantPool(JVMCIObject constant_pool);
  ConstantPool* asConstantPool(jobject constant_pool)  { return asConstantPool(wrap(constant_pool)); }

  JVMCIObject get_jvmci_constant_pool(const constantPoolHandle& cp, JVMCI_TRAPS);
  JVMCIObject get_jvmci_primitive_type(BasicType type);

  Handle asConstant(JVMCIObject object, JVMCI_TRAPS);
  JVMCIObject get_object_constant(oop objOop, bool compressed = false, bool dont_register = false);

  JVMCIPrimitiveArray new_booleanArray(int length, JVMCI_TRAPS);
  JVMCIPrimitiveArray new_byteArray(int length, JVMCI_TRAPS);
  JVMCIPrimitiveArray new_intArray(int length, JVMCI_TRAPS);
  JVMCIPrimitiveArray new_longArray(int length, JVMCI_TRAPS);

  JVMCIObjectArray new_byte_array_array(int length, JVMCI_TRAPS);

  JVMCIObject new_StackTraceElement(const methodHandle& method, int bci, JVMCI_TRAPS);
  JVMCIObject new_HotSpotNmethod(const methodHandle& method, const char* name, jboolean isDefault, jlong compileId, JVMCI_TRAPS);
  JVMCIObject new_VMField(JVMCIObject name, JVMCIObject type, jlong offset, jlong address, JVMCIObject value, JVMCI_TRAPS);
  JVMCIObject new_VMFlag(JVMCIObject name, JVMCIObject type, JVMCIObject value, JVMCI_TRAPS);
  JVMCIObject new_VMIntrinsicMethod(JVMCIObject declaringClass, JVMCIObject name, JVMCIObject descriptor, int id, JVMCI_TRAPS);
  JVMCIObject new_HotSpotStackFrameReference(JVMCI_TRAPS);
  JVMCIObject new_JVMCIError(JVMCI_TRAPS);

  jlong make_handle(const Handle& obj);
  oop resolve_handle(jlong objectHandle);

  // These are analagous to the JNI routines
  JVMCIObject make_local(JVMCIObject object);
  void destroy_local(JVMCIObject object);

  // Makes a JNI global handle that is not scoped by the
  // lifetime of a JVMCIRuntime (cf JVMCIRuntime::make_global).
  // These JNI handles are used when translating an object
  // between the HotSpot and JVMCI shared library heap via
  // HotSpotJVMCIRuntime.translate(Object) and
  // HotSpotJVMCIRuntime.unhand(Class<T>, long). Translation
  // can happen in either direction so the referenced object
  // can reside in either heap which is why JVMCIRuntime scoped
  // handles cannot be used (they are specific to HotSpot heap objects).
  JVMCIObject make_global(JVMCIObject object);

  // Destroys a JNI global handle created by JVMCIEnv::make_global.
  void destroy_global(JVMCIObject object);

  // Deoptimizes the nmethod (if any) in the HotSpotNmethod.address
  // field of mirror. The field is subsequently zeroed.
  void invalidate_nmethod_mirror(JVMCIObject mirror, JVMCI_TRAPS);

  void initialize_installed_code(JVMCIObject installed_code, CodeBlob* cb, JVMCI_TRAPS);

 private:
  JVMCICompileState* _compile_state;

 public:

  // Determines if this is for the JVMCI runtime in the HotSpot
  // heap (true) or the shared library heap (false).
  bool is_hotspot() { return _is_hotspot; }

  JVMCICompileState* compile_state() { return _compile_state; }
  void set_compile_state(JVMCICompileState* compile_state) {
    assert(_compile_state == NULL, "set only once");
    _compile_state = compile_state;
  }
  // Generate declarations for the initialize, new, isa, get and set methods for all the types and
  // fields declared in the JVMCI_CLASSES_DO macro.

#define START_CLASS(className, fullClassName)                           \
  void className##_initialize(JVMCI_TRAPS); \
  JVMCIObjectArray new_##className##_array(int length, JVMCI_TRAPS); \
  bool isa_##className(JVMCIObject object);

#define END_CLASS

#define FIELD(className, name, type, accessor)                                                                                                                         \
  type get_ ## className ## _ ## name(JVMCIObject obj); \
  void set_ ## className ## _ ## name(JVMCIObject obj, type x);

#define OOPISH_FIELD(className, name, type, hstype, accessor) \
  FIELD(className, name, type, accessor)

#define STATIC_FIELD(className, name, type) \
  type get_ ## className ## _ ## name(); \
  void set_ ## className ## _ ## name(type x);

#define STATIC_OOPISH_FIELD(className, name, type, hstype) \
  STATIC_FIELD(className, name, type)

#define EMPTY_CAST
#define CHAR_FIELD(className,  name) FIELD(className, name, jchar, char_field)
#define INT_FIELD(className,  name) FIELD(className, name, jint, int_field)
#define BOOLEAN_FIELD(className,  name) FIELD(className, name, jboolean, bool_field)
#define LONG_FIELD(className,  name) FIELD(className, name, jlong, long_field)
#define FLOAT_FIELD(className,  name) FIELD(className, name, jfloat, float_field)
#define OBJECT_FIELD(className,  name, signature) OOPISH_FIELD(className, name, JVMCIObject, oop, obj_field)
#define OBJECTARRAY_FIELD(className,  name, signature) OOPISH_FIELD(className, name, JVMCIObjectArray, objArrayOop, obj_field)
#define PRIMARRAY_FIELD(className,  name, signature) OOPISH_FIELD(className, name, JVMCIPrimitiveArray, typeArrayOop, obj_field)

#define STATIC_INT_FIELD(className, name) STATIC_FIELD(className, name, jint)
#define STATIC_BOOLEAN_FIELD(className, name) STATIC_FIELD(className, name, jboolean)
#define STATIC_OBJECT_FIELD(className, name, signature) STATIC_OOPISH_FIELD(className, name, JVMCIObject, oop)
#define STATIC_OBJECTARRAY_FIELD(className, name, signature) STATIC_OOPISH_FIELD(className, name, JVMCIObjectArray, objArrayOop)
#define METHOD(jniCallType, jniGetMethod, hsCallType, returnType, className, methodName, signatureSymbolName, args)
#define CONSTRUCTOR(className, signature)

  JVMCI_CLASSES_DO(START_CLASS, END_CLASS, CHAR_FIELD, INT_FIELD, BOOLEAN_FIELD, LONG_FIELD, FLOAT_FIELD, OBJECT_FIELD, PRIMARRAY_FIELD, OBJECTARRAY_FIELD, STATIC_OBJECT_FIELD, STATIC_OBJECTARRAY_FIELD, STATIC_INT_FIELD, STATIC_BOOLEAN_FIELD, METHOD, CONSTRUCTOR)

#undef JNI_START_CLASS
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
#undef FIELD
#undef OOPISH_FIELD
#undef STATIC_FIELD
#undef STATIC_OOPISH_FIELD
#undef STATIC_FIELD
#undef STATIC_OBJECT_FIELD
#undef STATIC_OBJECTARRAY_FIELD
#undef STATIC_INT_FIELD
#undef STATIC_BOOLEAN_FIELD
#undef EMPTY_CAST

  // End of JVMCIEnv
};

#endif // SHARE_JVMCI_JVMCIENV_HPP
