/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_JAVACLASSES_HPP
#define SHARE_CLASSFILE_JAVACLASSES_HPP

#include "classfile/vmClasses.hpp"
#include "oops/oop.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/symbol.hpp"
#include "runtime/os.hpp"
#include "utilities/vmEnums.hpp"

class RecordComponent;

// Interface for manipulating the basic Java classes.

#define BASIC_JAVA_CLASSES_DO_PART1(f) \
  f(java_lang_Class) \
  f(java_lang_String) \
  f(java_lang_ref_Reference) \
  //end

#define BASIC_JAVA_CLASSES_DO_PART2(f) \
  f(java_lang_System) \
  f(java_lang_ClassLoader) \
  f(java_lang_Throwable) \
  f(java_lang_Thread) \
  f(java_lang_ThreadGroup) \
  f(java_lang_InternalError) \
  f(java_lang_AssertionStatusDirectives) \
  f(java_lang_ref_SoftReference) \
  f(java_lang_invoke_MethodHandle) \
  f(java_lang_invoke_DirectMethodHandle) \
  f(java_lang_invoke_MemberName) \
  f(java_lang_invoke_ResolvedMethodName) \
  f(java_lang_invoke_LambdaForm) \
  f(java_lang_invoke_MethodType) \
  f(java_lang_invoke_CallSite) \
  f(java_lang_invoke_ConstantCallSite) \
  f(java_lang_invoke_MethodHandleNatives_CallSiteContext) \
  f(java_security_AccessControlContext) \
  f(java_lang_reflect_AccessibleObject) \
  f(java_lang_reflect_Method) \
  f(java_lang_reflect_Constructor) \
  f(java_lang_reflect_Field) \
  f(java_lang_reflect_RecordComponent) \
  f(reflect_ConstantPool) \
  f(reflect_UnsafeStaticFieldAccessorImpl) \
  f(java_lang_reflect_Parameter) \
  f(java_lang_Module) \
  f(java_lang_StackTraceElement) \
  f(java_lang_StackFrameInfo) \
  f(java_lang_LiveStackFrameInfo) \
  f(java_util_concurrent_locks_AbstractOwnableSynchronizer) \
  f(jdk_internal_invoke_NativeEntryPoint) \
  f(jdk_internal_misc_UnsafeConstants) \
  f(java_lang_boxing_object) \
  f(vector_VectorPayload) \
  //end

#define BASIC_JAVA_CLASSES_DO(f) \
        BASIC_JAVA_CLASSES_DO_PART1(f) \
        BASIC_JAVA_CLASSES_DO_PART2(f)

#define CHECK_INIT(offset)  assert(offset != 0, "should be initialized"); return offset;

// Interface to java.lang.Object objects

class java_lang_Object : AllStatic {
 public:
  static void register_natives(TRAPS);
};

// Interface to java.lang.String objects

// The flags field is a collection of bits representing boolean values used
// internally by the VM.
#define STRING_INJECTED_FIELDS(macro) \
  macro(java_lang_String, flags, byte_signature, false)

class java_lang_String : AllStatic {
 private:
  static int _value_offset;
  static int _hash_offset;
  static int _hashIsZero_offset;
  static int _coder_offset;
  static int _flags_offset;

  static bool _initialized;

  static Handle basic_create(int length, bool byte_arr, TRAPS);

  static inline void set_coder(oop string, jbyte coder);

  // Bitmasks for values in the injected flags field.
  static const uint8_t _deduplication_forbidden_mask = 1 << 0;
  static const uint8_t _deduplication_requested_mask = 1 << 1;

  static int flags_offset() { CHECK_INIT(_flags_offset); }
  // Return the address of the injected flags field.
  static inline uint8_t* flags_addr(oop java_string);
  // Test whether the designated bit of the injected flags field is set.
  static inline bool is_flag_set(oop java_string, uint8_t flag_mask);
  // Atomically test and set the designated bit of the injected flags field,
  // returning true if the bit was already set.
  static bool test_and_set_flag(oop java_string, uint8_t flag_mask);

  static inline unsigned int hash_code_impl(oop java_string, bool update);

 public:

  // Coders
  enum Coder {
    CODER_LATIN1 =  0,
    CODER_UTF16  =  1
  };

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Instance creation
  static Handle create_from_unicode(const jchar* unicode, int len, TRAPS);
  static oop    create_oop_from_unicode(const jchar* unicode, int len, TRAPS);
  static Handle create_from_str(const char* utf8_str, TRAPS);
  static oop    create_oop_from_str(const char* utf8_str, TRAPS);
  static Handle create_from_symbol(Symbol* symbol, TRAPS);
  static Handle create_from_platform_dependent_str(const char* str, TRAPS);

  static void set_compact_strings(bool value);

  static int value_offset() { CHECK_INIT(_value_offset); }
  static int coder_offset() { CHECK_INIT(_coder_offset); }

  static inline void set_value_raw(oop string, typeArrayOop buffer);
  static inline void set_value(oop string, typeArrayOop buffer);

  // Set the deduplication_forbidden flag true.  This flag is sticky; once
  // set it never gets cleared.  This is set when a String is interned in
  // the StringTable, to prevent string deduplication from changing the
  // String's value array.
  static inline void set_deduplication_forbidden(oop java_string);

  // Test and set the deduplication_requested flag.  Returns the old value
  // of the flag.  This flag is sticky; once set it never gets cleared.
  // Some GCs may use this flag when deciding whether to request
  // deduplication of a String, to avoid multiple requests for the same
  // object.
  static inline bool test_and_set_deduplication_requested(oop java_string);

  // Accessors
  static inline typeArrayOop value(oop java_string);
  static inline typeArrayOop value_no_keepalive(oop java_string);
  static inline bool hash_is_set(oop string);
  static inline bool is_latin1(oop java_string);
  static inline bool deduplication_forbidden(oop java_string);
  static inline bool deduplication_requested(oop java_string);
  static inline int length(oop java_string);
  static inline int length(oop java_string, typeArrayOop string_value);
  static int utf8_length(oop java_string);
  static int utf8_length(oop java_string, typeArrayOop string_value);

  // String converters
  static char*  as_utf8_string(oop java_string);
  static char*  as_utf8_string(oop java_string, int& length);
  static char*  as_utf8_string_full(oop java_string, char* buf, int buflen, int& length);
  static char*  as_utf8_string(oop java_string, char* buf, int buflen);
  static char*  as_utf8_string(oop java_string, int start, int len);
  static char*  as_utf8_string(oop java_string, typeArrayOop value, char* buf, int buflen);
  static char*  as_utf8_string(oop java_string, typeArrayOop value, int start, int len, char* buf, int buflen);
  static char*  as_platform_dependent_str(Handle java_string, TRAPS);
  static jchar* as_unicode_string(oop java_string, int& length, TRAPS);
  static jchar* as_unicode_string_or_null(oop java_string, int& length);
  // produce an ascii string with all other values quoted using \u####
  static char*  as_quoted_ascii(oop java_string);

  // Compute the hash value for a java.lang.String object which would
  // contain the characters passed in.
  //
  // As the hash value used by the String object itself, in
  // String.hashCode().  This value is normally calculated in Java code
  // in the String.hashCode method(), but is precomputed for String
  // objects in the shared archive file.
  // hash P(31) from Kernighan & Ritchie
  //
  // For this reason, THIS ALGORITHM MUST MATCH String.hashCode().
  static unsigned int hash_code(const jchar* s, int len) {
    unsigned int h = 0;
    while (len-- > 0) {
      h = 31*h + (unsigned int) *s;
      s++;
    }
    return h;
  }

  static unsigned int hash_code(const jbyte* s, int len) {
    unsigned int h = 0;
    while (len-- > 0) {
      h = 31*h + (((unsigned int) *s) & 0xFF);
      s++;
    }
    return h;
  }

  static unsigned int hash_code(oop java_string);
  static unsigned int hash_code_noupdate(oop java_string);

  static bool equals(oop java_string, const jchar* chars, int len);
  static bool equals(oop str1, oop str2);
  static inline bool value_equals(typeArrayOop str_value1, typeArrayOop str_value2);

  // Conversion between '.' and '/' formats, and allocate a String from the result.
  static Handle externalize_classname(Symbol* java_name, TRAPS);

  // Conversion
  static Symbol* as_symbol(oop java_string);
  static Symbol* as_symbol_or_null(oop java_string);

  // Testers
  static bool is_instance(oop obj);
  static inline bool is_instance_inlined(oop obj);

  // Debugging
  static void print(oop java_string, outputStream* st);
  friend class JavaClasses;
  friend class StringTable;
};


// Interface to java.lang.Class objects

#define CLASS_INJECTED_FIELDS(macro)                                       \
  macro(java_lang_Class, klass,                  intptr_signature,  false) \
  macro(java_lang_Class, array_klass,            intptr_signature,  false) \
  macro(java_lang_Class, oop_size,               int_signature,     false) \
  macro(java_lang_Class, static_oop_field_count, int_signature,     false) \
  macro(java_lang_Class, protection_domain,      object_signature,  false) \
  macro(java_lang_Class, signers,                object_signature,  false) \
  macro(java_lang_Class, source_file,            object_signature,  false) \

class java_lang_Class : AllStatic {
  friend class VMStructs;
  friend class JVMCIVMStructs;

 private:

  // The fake offsets are added by the class loader when java.lang.Class is loaded

  static int _klass_offset;
  static int _array_klass_offset;

  static int _oop_size_offset;
  static int _static_oop_field_count_offset;

  static int _protection_domain_offset;
  static int _init_lock_offset;
  static int _signers_offset;
  static int _class_loader_offset;
  static int _module_offset;
  static int _component_mirror_offset;
  static int _name_offset;
  static int _source_file_offset;
  static int _classData_offset;
  static int _classRedefinedCount_offset;

  static bool _offsets_computed;

  static GrowableArray<Klass*>* _fixup_mirror_list;
  static GrowableArray<Klass*>* _fixup_module_field_list;

  static void set_init_lock(oop java_class, oop init_lock);
  static void set_protection_domain(oop java_class, oop protection_domain);
  static void set_class_loader(oop java_class, oop class_loader);
  static void set_component_mirror(oop java_class, oop comp_mirror);
  static void initialize_mirror_fields(Klass* k, Handle mirror, Handle protection_domain,
                                       Handle classData, TRAPS);
  static void set_mirror_module_field(JavaThread* current, Klass* K, Handle mirror, Handle module);
 public:
  static void allocate_fixup_lists();
  static void compute_offsets();

  // Instance creation
  static void create_mirror(Klass* k, Handle class_loader, Handle module,
                            Handle protection_domain, Handle classData, TRAPS);
  static void fixup_mirror(Klass* k, TRAPS);
  static oop  create_basic_type_mirror(const char* basic_type_name, BasicType type, TRAPS);
  static void update_archived_primitive_mirror_native_pointers(oop archived_mirror) NOT_CDS_JAVA_HEAP_RETURN;
  static void update_archived_mirror_native_pointers(oop archived_mirror) NOT_CDS_JAVA_HEAP_RETURN;

  // Archiving
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  static void archive_basic_type_mirrors() NOT_CDS_JAVA_HEAP_RETURN;
  static oop  archive_mirror(Klass* k) NOT_CDS_JAVA_HEAP_RETURN_(NULL);
  static oop  process_archived_mirror(Klass* k, oop mirror, oop archived_mirror)
                                      NOT_CDS_JAVA_HEAP_RETURN_(NULL);
  static bool restore_archived_mirror(Klass *k, Handle class_loader, Handle module,
                                      Handle protection_domain,
                                      TRAPS) NOT_CDS_JAVA_HEAP_RETURN_(false);

  static void fixup_module_field(Klass* k, Handle module);

  // Conversion
  static Klass* as_Klass(oop java_class);
  static void set_klass(oop java_class, Klass* klass);
  static BasicType as_BasicType(oop java_class, Klass** reference_klass = NULL);
  static Symbol* as_signature(oop java_class, bool intern_if_not_found);
  static void print_signature(oop java_class, outputStream *st);
  static const char* as_external_name(oop java_class);
  // Testing
  static bool is_instance(oop obj);

  static bool is_primitive(oop java_class);
  static BasicType primitive_type(oop java_class);
  static oop primitive_mirror(BasicType t);
  // JVM_NewArray support
  static Klass* array_klass_acquire(oop java_class);
  static void release_set_array_klass(oop java_class, Klass* klass);
  // compiler support for class operations
  static int klass_offset()                { CHECK_INIT(_klass_offset); }
  static int array_klass_offset()          { CHECK_INIT(_array_klass_offset); }
  // Support for classRedefinedCount field
  static int classRedefinedCount(oop the_class_mirror);
  static void set_classRedefinedCount(oop the_class_mirror, int value);

  // Support for embedded per-class oops
  static oop  protection_domain(oop java_class);
  static oop  init_lock(oop java_class);
  static void clear_init_lock(oop java_class) {
    set_init_lock(java_class, NULL);
  }
  static oop  component_mirror(oop java_class);
  static objArrayOop  signers(oop java_class);
  static void set_signers(oop java_class, objArrayOop signers);
  static oop  class_data(oop java_class);
  static void set_class_data(oop java_class, oop classData);

  static int component_mirror_offset() { return _component_mirror_offset; }

  static oop class_loader(oop java_class);
  static void set_module(oop java_class, oop module);
  static oop module(oop java_class);

  static oop name(Handle java_class, TRAPS);

  static oop source_file(oop java_class);
  static void set_source_file(oop java_class, oop source_file);

  static int oop_size(oop java_class);
  static void set_oop_size(HeapWord* java_class, int size);
  static int static_oop_field_count(oop java_class);
  static void set_static_oop_field_count(oop java_class, int size);

  static GrowableArray<Klass*>* fixup_mirror_list() {
    return _fixup_mirror_list;
  }
  static void set_fixup_mirror_list(GrowableArray<Klass*>* v) {
    _fixup_mirror_list = v;
  }

  static GrowableArray<Klass*>* fixup_module_field_list() {
    return _fixup_module_field_list;
  }
  static void set_fixup_module_field_list(GrowableArray<Klass*>* v) {
    _fixup_module_field_list = v;
  }

  // Debugging
  friend class JavaClasses;
};

// Interface to java.lang.Thread objects

class java_lang_Thread : AllStatic {
 private:
  // Note that for this class the layout changed between JDK1.2 and JDK1.3,
  // so we compute the offsets at startup rather than hard-wiring them.
  static int _name_offset;
  static int _group_offset;
  static int _contextClassLoader_offset;
  static int _inheritedAccessControlContext_offset;
  static int _priority_offset;
  static int _eetop_offset;
  static int _interrupted_offset;
  static int _daemon_offset;
  static int _stillborn_offset;
  static int _stackSize_offset;
  static int _tid_offset;
  static int _thread_status_offset;
  static int _park_blocker_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Returns the JavaThread associated with the thread obj
  static JavaThread* thread(oop java_thread);
  // Set JavaThread for instance
  static void set_thread(oop java_thread, JavaThread* thread);
  // Interrupted status
  static bool interrupted(oop java_thread);
  static void set_interrupted(oop java_thread, bool val);
  // Name
  static oop name(oop java_thread);
  static void set_name(oop java_thread, oop name);
  // Priority
  static ThreadPriority priority(oop java_thread);
  static void set_priority(oop java_thread, ThreadPriority priority);
  // Thread group
  static oop  threadGroup(oop java_thread);
  // Stillborn
  static bool is_stillborn(oop java_thread);
  static void set_stillborn(oop java_thread);
  // Alive (NOTE: this is not really a field, but provides the correct
  // definition without doing a Java call)
  static bool is_alive(oop java_thread);
  // Daemon
  static bool is_daemon(oop java_thread);
  static void set_daemon(oop java_thread);
  // Context ClassLoader
  static oop context_class_loader(oop java_thread);
  // Control context
  static oop inherited_access_control_context(oop java_thread);
  // Stack size hint
  static jlong stackSize(oop java_thread);
  // Thread ID
  static jlong thread_id(oop java_thread);

  // Blocker object responsible for thread parking
  static oop park_blocker(oop java_thread);

  // Write thread status info to threadStatus field of java.lang.Thread.
  static void set_thread_status(oop java_thread_oop, JavaThreadStatus status);
  // Read thread status info from threadStatus field of java.lang.Thread.
  static JavaThreadStatus get_thread_status(oop java_thread_oop);

  static const char*  thread_status_name(oop java_thread_oop);

  // Debugging
  friend class JavaClasses;
};

// Interface to java.lang.ThreadGroup objects

class java_lang_ThreadGroup : AllStatic {
 private:
  static int _parent_offset;
  static int _name_offset;
  static int _threads_offset;
  static int _groups_offset;
  static int _maxPriority_offset;
  static int _destroyed_offset;
  static int _daemon_offset;
  static int _nthreads_offset;
  static int _ngroups_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // parent ThreadGroup
  static oop  parent(oop java_thread_group);
  // name
  static const char* name(oop java_thread_group);
  // ("name as oop" accessor is not necessary)
  // Number of threads in group
  static int nthreads(oop java_thread_group);
  // threads
  static objArrayOop threads(oop java_thread_group);
  // Number of threads in group
  static int ngroups(oop java_thread_group);
  // groups
  static objArrayOop groups(oop java_thread_group);
  // maxPriority in group
  static ThreadPriority maxPriority(oop java_thread_group);
  // Destroyed
  static bool is_destroyed(oop java_thread_group);
  // Daemon
  static bool is_daemon(oop java_thread_group);
  // Debugging
  friend class JavaClasses;
};



// Interface to java.lang.Throwable objects

class java_lang_Throwable: AllStatic {
  friend class BacktraceBuilder;
  friend class BacktraceIterator;

 private:
  // Trace constants
  enum {
    trace_methods_offset = 0,
    trace_bcis_offset    = 1,
    trace_mirrors_offset = 2,
    trace_names_offset   = 3,
    trace_next_offset    = 4,
    trace_hidden_offset  = 5,
    trace_size           = 6,
    trace_chunk_size     = 32
  };

  static int _backtrace_offset;
  static int _detailMessage_offset;
  static int _stackTrace_offset;
  static int _depth_offset;
  static int _cause_offset;
  static int _static_unassigned_stacktrace_offset;

  // StackTrace (programmatic access, new since 1.4)
  static void clear_stacktrace(oop throwable);
  // Stacktrace (post JDK 1.7.0 to allow immutability protocol to be followed)
  static void set_stacktrace(oop throwable, oop st_element_array);
  static oop unassigned_stacktrace();

 public:
  // Backtrace
  static oop backtrace(oop throwable);
  static void set_backtrace(oop throwable, oop value);
  static int depth(oop throwable);
  static void set_depth(oop throwable, int value);
  static int get_detailMessage_offset() { CHECK_INIT(_detailMessage_offset); }
  // Message
  static oop message(oop throwable);
  static oop cause(oop throwable);
  static void set_message(oop throwable, oop value);
  static Symbol* detail_message(oop throwable);
  static void print_stack_element(outputStream *st, Method* method, int bci);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocate space for backtrace (created but stack trace not filled in)
  static void allocate_backtrace(Handle throwable, TRAPS);
  // Fill in current stack trace for throwable with preallocated backtrace (no GC)
  static void fill_in_stack_trace_of_preallocated_backtrace(Handle throwable);
  // Fill in current stack trace, can cause GC
  static void fill_in_stack_trace(Handle throwable, const methodHandle& method, TRAPS);
  static void fill_in_stack_trace(Handle throwable, const methodHandle& method = methodHandle());
  // Programmatic access to stack trace
  static void get_stack_trace_elements(Handle throwable, objArrayHandle stack_trace, TRAPS);
  // Printing
  static void print(oop throwable, outputStream* st);
  static void print_stack_trace(Handle throwable, outputStream* st);
  static void java_printStackTrace(Handle throwable, TRAPS);
  // Debugging
  friend class JavaClasses;
  // Gets the method and bci of the top frame (TOS). Returns false if this failed.
  static bool get_top_method_and_bci(oop throwable, Method** method, int* bci);
};


// Interface to java.lang.reflect.AccessibleObject objects

class java_lang_reflect_AccessibleObject: AllStatic {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _override_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Accessors
  static jboolean override(oop reflect);
  static void set_override(oop reflect, jboolean value);

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.reflect.Method objects

class java_lang_reflect_Method : public java_lang_reflect_AccessibleObject {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _clazz_offset;
  static int _name_offset;
  static int _returnType_offset;
  static int _parameterTypes_offset;
  static int _exceptionTypes_offset;
  static int _slot_offset;
  static int _modifiers_offset;
  static int _signature_offset;
  static int _annotations_offset;
  static int _parameter_annotations_offset;
  static int _annotation_default_offset;

  static void compute_offsets();
 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocation
  static Handle create(TRAPS);

  // Accessors
  static oop clazz(oop reflect);
  static void set_clazz(oop reflect, oop value);

  static void set_name(oop method, oop value);

  static oop return_type(oop method);
  static void set_return_type(oop method, oop value);

  static oop parameter_types(oop method);
  static void set_parameter_types(oop method, oop value);

  static int slot(oop reflect);
  static void set_slot(oop reflect, int value);

  static void set_exception_types(oop method, oop value);
  static void set_modifiers(oop method, int value);
  static void set_signature(oop method, oop value);
  static void set_annotations(oop method, oop value);
  static void set_parameter_annotations(oop method, oop value);
  static void set_annotation_default(oop method, oop value);

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.reflect.Constructor objects

class java_lang_reflect_Constructor : public java_lang_reflect_AccessibleObject {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _clazz_offset;
  static int _parameterTypes_offset;
  static int _exceptionTypes_offset;
  static int _slot_offset;
  static int _modifiers_offset;
  static int _signature_offset;
  static int _annotations_offset;
  static int _parameter_annotations_offset;

  static void compute_offsets();
 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocation
  static Handle create(TRAPS);

  // Accessors
  static oop clazz(oop reflect);
  static void set_clazz(oop reflect, oop value);

  static oop parameter_types(oop constructor);
  static void set_parameter_types(oop constructor, oop value);

  static int slot(oop reflect);
  static void set_slot(oop reflect, int value);

  static void set_exception_types(oop constructor, oop value);
  static void set_modifiers(oop constructor, int value);
  static void set_signature(oop constructor, oop value);
  static void set_annotations(oop constructor, oop value);
  static void set_parameter_annotations(oop method, oop value);

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.reflect.Field objects

class java_lang_reflect_Field : public java_lang_reflect_AccessibleObject {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _clazz_offset;
  static int _name_offset;
  static int _type_offset;
  static int _slot_offset;
  static int _modifiers_offset;
  static int _trusted_final_offset;
  static int _signature_offset;
  static int _annotations_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocation
  static Handle create(TRAPS);

  // Accessors
  static oop clazz(oop reflect);
  static void set_clazz(oop reflect, oop value);

  static oop name(oop field);
  static void set_name(oop field, oop value);

  static oop type(oop field);
  static void set_type(oop field, oop value);

  static int slot(oop reflect);
  static void set_slot(oop reflect, int value);

  static int modifiers(oop field);
  static void set_modifiers(oop field, int value);

  static void set_trusted_final(oop field);

  static void set_signature(oop constructor, oop value);
  static void set_annotations(oop constructor, oop value);

  // Debugging
  friend class JavaClasses;
};

class java_lang_reflect_Parameter {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _name_offset;
  static int _modifiers_offset;
  static int _index_offset;
  static int _executable_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocation
  static Handle create(TRAPS);

  // Accessors
  static oop name(oop field);
  static void set_name(oop field, oop value);

  static int index(oop reflect);
  static void set_index(oop reflect, int value);

  static int modifiers(oop reflect);
  static void set_modifiers(oop reflect, int value);

  static oop executable(oop constructor);
  static void set_executable(oop constructor, oop value);

  friend class JavaClasses;
};

#define MODULE_INJECTED_FIELDS(macro)                            \
  macro(java_lang_Module, module_entry, intptr_signature, false)

class java_lang_Module {
  private:
    static int _loader_offset;
    static int _name_offset;
    static int _module_entry_offset;

    static void compute_offsets();

  public:
    static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

    // Allocation
    static Handle create(Handle loader, Handle module_name, TRAPS);

    // Testers
    static bool is_instance(oop obj);

    // Accessors
    static oop loader(oop module);
    static void set_loader(oop module, oop value);

    static oop name(oop module);
    static void set_name(oop module, oop value);

    static ModuleEntry* module_entry(oop module);
    static ModuleEntry* module_entry_raw(oop module);
    static void set_module_entry(oop module, ModuleEntry* module_entry);

  friend class JavaClasses;
};

// Interface to jdk.internal.reflect.ConstantPool objects
class reflect_ConstantPool {
 private:
  // Note that to reduce dependencies on the JDK we compute these
  // offsets at run-time.
  static int _oop_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Allocation
  static Handle create(TRAPS);

  // Accessors
  static void set_cp(oop reflect, ConstantPool* value);
  static int oop_offset() { CHECK_INIT(_oop_offset); }

  static ConstantPool* get_cp(oop reflect);

  // Debugging
  friend class JavaClasses;
};

// Interface to jdk.internal.reflect.UnsafeStaticFieldAccessorImpl objects
class reflect_UnsafeStaticFieldAccessorImpl {
 private:
  static int _base_offset;
  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  static int base_offset() { CHECK_INIT(_base_offset); }

  // Debugging
  friend class JavaClasses;
};

// Interface to java.lang primitive type boxing objects:
//  - java.lang.Boolean
//  - java.lang.Character
//  - java.lang.Float
//  - java.lang.Double
//  - java.lang.Byte
//  - java.lang.Short
//  - java.lang.Integer
//  - java.lang.Long

// This could be separated out into 8 individual classes.

class java_lang_boxing_object: AllStatic {
 private:
  static int _value_offset;
  static int _long_value_offset;

  static void compute_offsets();
  static oop initialize_and_allocate(BasicType type, TRAPS);
 public:
  // Allocation. Returns a boxed value, or NULL for invalid type.
  static oop create(BasicType type, jvalue* value, TRAPS);
  // Accessors. Returns the basic type being boxed, or T_ILLEGAL for invalid oop.
  static BasicType get_value(oop box, jvalue* value);
  static BasicType set_value(oop box, jvalue* value);
  static BasicType basic_type(oop box);
  static bool is_instance(oop box)                 { return basic_type(box) != T_ILLEGAL; }
  static bool is_instance(oop box, BasicType type) { return basic_type(box) == type; }
  static void print(oop box, outputStream* st)     { jvalue value;  print(get_value(box, &value), &value, st); }
  static void print(BasicType type, jvalue* value, outputStream* st);

  static int value_offset(BasicType type) {
    return is_double_word_type(type) ? _long_value_offset : _value_offset;
  }

  static void serialize_offsets(SerializeClosure* f);

  // Debugging
  friend class JavaClasses;
};



// Interface to java.lang.ref.Reference objects

class java_lang_ref_Reference: AllStatic {
  static int _referent_offset;
  static int _queue_offset;
  static int _next_offset;
  static int _discovered_offset;

  static bool _offsets_initialized;

 public:
  // Accessors
  static inline oop weak_referent_no_keepalive(oop ref);
  static inline oop phantom_referent_no_keepalive(oop ref);
  static inline oop unknown_referent_no_keepalive(oop ref);
  static inline void clear_referent(oop ref);
  static inline HeapWord* referent_addr_raw(oop ref);
  static inline oop next(oop ref);
  static inline void set_next(oop ref, oop value);
  static inline void set_next_raw(oop ref, oop value);
  static inline HeapWord* next_addr_raw(oop ref);
  static inline oop discovered(oop ref);
  static inline void set_discovered(oop ref, oop value);
  static inline void set_discovered_raw(oop ref, oop value);
  static inline HeapWord* discovered_addr_raw(oop ref);
  static bool is_referent_field(oop obj, ptrdiff_t offset);
  static inline bool is_final(oop ref);
  static inline bool is_phantom(oop ref);

  static int referent_offset()    { CHECK_INIT(_referent_offset); }
  static int queue_offset()       { CHECK_INIT(_queue_offset); }
  static int next_offset()        { CHECK_INIT(_next_offset); }
  static int discovered_offset()  { CHECK_INIT(_discovered_offset); }

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};


// Interface to java.lang.ref.SoftReference objects

class java_lang_ref_SoftReference: public java_lang_ref_Reference {
  static int _timestamp_offset;
  static int _static_clock_offset;

 public:
  // Accessors
  static jlong timestamp(oop ref);

  // Accessors for statics
  static jlong clock();
  static void set_clock(jlong value);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

// Interface to java.lang.invoke.MethodHandle objects

class java_lang_invoke_MethodHandle: AllStatic {
  friend class JavaClasses;

 private:
  static int _type_offset;               // the MethodType of this MH
  static int _form_offset;               // the LambdaForm of this MH

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Accessors
  static oop            type(oop mh);
  static void       set_type(oop mh, oop mtype);

  static oop            form(oop mh);
  static void       set_form(oop mh, oop lform);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::MethodHandle_klass());
  }
  static bool is_instance(oop obj);

  // Accessors for code generation:
  static int type_offset()             { CHECK_INIT(_type_offset); }
  static int form_offset()             { CHECK_INIT(_form_offset); }
};

// Interface to java.lang.invoke.DirectMethodHandle objects

class java_lang_invoke_DirectMethodHandle: AllStatic {
  friend class JavaClasses;

 private:
  static int _member_offset;               // the MemberName of this DMH

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Accessors
  static oop  member(oop mh);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::DirectMethodHandle_klass());
  }
  static bool is_instance(oop obj);

  // Accessors for code generation:
  static int member_offset()           { CHECK_INIT(_member_offset); }
};

// Interface to java.lang.invoke.LambdaForm objects
// (These are a private interface for managing adapter code generation.)

class java_lang_invoke_LambdaForm: AllStatic {
  friend class JavaClasses;

 private:
  static int _vmentry_offset;  // type is MemberName

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Accessors
  static oop            vmentry(oop lform);

  // Testers
  static bool is_subclass(Klass* klass) {
    return vmClasses::LambdaForm_klass() != NULL &&
      klass->is_subclass_of(vmClasses::LambdaForm_klass());
  }
  static bool is_instance(oop obj);

  // Accessors for code generation:
  static int vmentry_offset()          { CHECK_INIT(_vmentry_offset); }
};

// Interface to java.lang.invoke.NativeEntryPoint objects
// (These are a private interface for managing adapter code generation.)

class jdk_internal_invoke_NativeEntryPoint: AllStatic {
  friend class JavaClasses;

 private:
  static int _shadow_space_offset;
  static int _argMoves_offset;
  static int _returnMoves_offset;
  static int _need_transition_offset;
  static int _method_type_offset;
  static int _name_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Accessors
  static jint       shadow_space(oop entry);
  static oop        argMoves(oop entry);
  static oop        returnMoves(oop entry);
  static jboolean   need_transition(oop entry);
  static oop        method_type(oop entry);
  static oop        name(oop entry);

  // Testers
  static bool is_subclass(Klass* klass) {
    return vmClasses::NativeEntryPoint_klass() != NULL &&
      klass->is_subclass_of(vmClasses::NativeEntryPoint_klass());
  }
  static bool is_instance(oop obj);

  // Accessors for code generation:
  static int shadow_space_offset_in_bytes()    { return _shadow_space_offset;    }
  static int argMoves_offset_in_bytes()        { return _argMoves_offset;        }
  static int returnMoves_offset_in_bytes()     { return _returnMoves_offset;     }
  static int need_transition_offset_in_bytes() { return _need_transition_offset; }
  static int method_type_offset_in_bytes()     { return _method_type_offset;     }
  static int name_offset_in_bytes()            { return _name_offset;            }
};

// Interface to java.lang.invoke.MemberName objects
// (These are a private interface for Java code to query the class hierarchy.)

#define RESOLVEDMETHOD_INJECTED_FIELDS(macro)                                   \
  macro(java_lang_invoke_ResolvedMethodName, vmholder, object_signature, false) \
  macro(java_lang_invoke_ResolvedMethodName, vmtarget, intptr_signature, false)

class java_lang_invoke_ResolvedMethodName : AllStatic {
  friend class JavaClasses;

  static int _vmtarget_offset;
  static int _vmholder_offset;

  static void compute_offsets();
 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  static int vmtarget_offset() { CHECK_INIT(_vmtarget_offset); }

  static Method* vmtarget(oop resolved_method);
  static void set_vmtarget(oop resolved_method, Method* method);

  static void set_vmholder(oop resolved_method, oop holder);

  // find or create resolved member name
  static oop find_resolved_method(const methodHandle& m, TRAPS);

  static bool is_instance(oop resolved_method);
};


#define MEMBERNAME_INJECTED_FIELDS(macro)                               \
  macro(java_lang_invoke_MemberName, vmindex,  intptr_signature, false)


class java_lang_invoke_MemberName: AllStatic {
  friend class JavaClasses;

 private:
  // From java.lang.invoke.MemberName:
  //    private Class<?>   clazz;       // class in which the method is defined
  //    private String     name;        // may be null if not yet materialized
  //    private Object     type;        // may be null if not yet materialized
  //    private int        flags;       // modifier bits; see reflect.Modifier
  //    private ResolvedMethodName method;    // holds VM-specific target value
  //    private intptr_t   vmindex;     // member index within class or interface
  static int _clazz_offset;
  static int _name_offset;
  static int _type_offset;
  static int _flags_offset;
  static int _method_offset;
  static int _vmindex_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  // Accessors
  static oop            clazz(oop mname);
  static void       set_clazz(oop mname, oop clazz);

  static oop            type(oop mname);
  static void       set_type(oop mname, oop type);

  static oop            name(oop mname);
  static void       set_name(oop mname, oop name);

  static int            flags(oop mname);
  static void       set_flags(oop mname, int flags);

  // Link through ResolvedMethodName field to get Method*
  static Method*        vmtarget(oop mname);
  static void       set_method(oop mname, oop method);

  static intptr_t       vmindex(oop mname);
  static void       set_vmindex(oop mname, intptr_t index);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::MemberName_klass());
  }
  static bool is_instance(oop obj);

  static bool is_method(oop obj);

  // Relevant integer codes (keep these in synch. with MethodHandleNatives.Constants):
  enum {
    MN_IS_METHOD             = 0x00010000, // method (not constructor)
    MN_IS_CONSTRUCTOR        = 0x00020000, // constructor
    MN_IS_FIELD              = 0x00040000, // field
    MN_IS_TYPE               = 0x00080000, // nested type
    MN_CALLER_SENSITIVE      = 0x00100000, // @CallerSensitive annotation detected
    MN_TRUSTED_FINAL         = 0x00200000, // trusted final field
    MN_REFERENCE_KIND_SHIFT  = 24, // refKind
    MN_REFERENCE_KIND_MASK   = 0x0F000000 >> MN_REFERENCE_KIND_SHIFT,
    // The SEARCH_* bits are not for MN.flags but for the matchFlags argument of MHN.getMembers:
    MN_SEARCH_SUPERCLASSES   = 0x00100000, // walk super classes
    MN_SEARCH_INTERFACES     = 0x00200000, // walk implemented interfaces
    MN_NESTMATE_CLASS        = 0x00000001,
    MN_HIDDEN_CLASS          = 0x00000002,
    MN_STRONG_LOADER_LINK    = 0x00000004,
    MN_ACCESS_VM_ANNOTATIONS = 0x00000008,
    // Lookup modes
    MN_MODULE_MODE           = 0x00000010,
    MN_UNCONDITIONAL_MODE    = 0x00000020,
    MN_TRUSTED_MODE          = -1
  };

  // Accessors for code generation:
  static int clazz_offset()   { CHECK_INIT(_clazz_offset); }
  static int type_offset()    { CHECK_INIT(_type_offset); }
  static int flags_offset()   { CHECK_INIT(_flags_offset); }
  static int method_offset()  { CHECK_INIT(_method_offset); }
  static int vmindex_offset() { CHECK_INIT(_vmindex_offset); }
};


// Interface to java.lang.invoke.MethodType objects

class java_lang_invoke_MethodType: AllStatic {
  friend class JavaClasses;

 private:
  static int _rtype_offset;
  static int _ptypes_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  // Accessors
  static oop            rtype(oop mt);
  static objArrayOop    ptypes(oop mt);

  static oop            ptype(oop mt, int index);
  static int            ptype_count(oop mt);

  static int            ptype_slot_count(oop mt);  // extra counts for long/double
  static int            rtype_slot_count(oop mt);  // extra counts for long/double

  static Symbol*        as_signature(oop mt, bool intern_if_not_found);
  static void           print_signature(oop mt, outputStream* st);

  static bool is_instance(oop obj);

  static bool equals(oop mt1, oop mt2);

  // Accessors for code generation:
  static int rtype_offset()  { CHECK_INIT(_rtype_offset); }
  static int ptypes_offset() { CHECK_INIT(_ptypes_offset); }
};


// Interface to java.lang.invoke.CallSite objects

class java_lang_invoke_CallSite: AllStatic {
  friend class JavaClasses;

private:
  static int _target_offset;
  static int _context_offset;

  static void compute_offsets();

public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  // Accessors
  static oop              target(          oop site);
  static void         set_target(          oop site, oop target);
  static void         set_target_volatile( oop site, oop target);

  static oop context_no_keepalive(oop site);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::CallSite_klass());
  }
  static bool is_instance(oop obj);

  // Accessors for code generation:
  static int target_offset()  { CHECK_INIT(_target_offset); }
  static int context_offset() { CHECK_INIT(_context_offset); }
};

// Interface to java.lang.invoke.ConstantCallSite objects

class java_lang_invoke_ConstantCallSite: AllStatic {
  friend class JavaClasses;

private:
  static int _is_frozen_offset;

  static void compute_offsets();

public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  // Accessors
  static jboolean is_frozen(oop site);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::ConstantCallSite_klass());
  }
  static bool is_instance(oop obj);
};

// Interface to java.lang.invoke.MethodHandleNatives$CallSiteContext objects

#define CALLSITECONTEXT_INJECTED_FIELDS(macro) \
  macro(java_lang_invoke_MethodHandleNatives_CallSiteContext, vmdependencies, intptr_signature, false) \
  macro(java_lang_invoke_MethodHandleNatives_CallSiteContext, last_cleanup, long_signature, false)

class DependencyContext;

class java_lang_invoke_MethodHandleNatives_CallSiteContext : AllStatic {
  friend class JavaClasses;

private:
  static int _vmdependencies_offset;
  static int _last_cleanup_offset;

  static void compute_offsets();

public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  // Accessors
  static DependencyContext vmdependencies(oop context);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::Context_klass());
  }
  static bool is_instance(oop obj);
};

// Interface to java.security.AccessControlContext objects

class java_security_AccessControlContext: AllStatic {
 private:
  // Note that for this class the layout changed between JDK1.2 and JDK1.3,
  // so we compute the offsets at startup rather than hard-wiring them.
  static int _context_offset;
  static int _privilegedContext_offset;
  static int _isPrivileged_offset;
  static int _isAuthorized_offset;

  static void compute_offsets();
 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  static oop create(objArrayHandle context, bool isPrivileged, Handle privileged_context, TRAPS);

  // Debugging/initialization
  friend class JavaClasses;
};


// Interface to java.lang.ClassLoader objects

#define CLASSLOADER_INJECTED_FIELDS(macro)                            \
  macro(java_lang_ClassLoader, loader_data,  intptr_signature, false)

class java_lang_ClassLoader : AllStatic {
 private:
  static int _loader_data_offset;
  static int _parent_offset;
  static int _parallelCapable_offset;
  static int _name_offset;
  static int _nameAndId_offset;
  static int _unnamedModule_offset;

  static void compute_offsets();

 public:
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  static ClassLoaderData* loader_data_acquire(oop loader);
  static ClassLoaderData* loader_data(oop loader);
  static void release_set_loader_data(oop loader, ClassLoaderData* new_data);

  static oop parent(oop loader);
  static oop name(oop loader);
  static oop nameAndId(oop loader);
  static bool isAncestor(oop loader, oop cl);

  // Support for parallelCapable field
  static bool parallelCapable(oop the_class_mirror);

  static bool is_trusted_loader(oop loader);

  // Return true if this is one of the class loaders associated with
  // the generated bytecodes for reflection.
  static bool is_reflection_class_loader(oop loader);

  // Fix for 4474172
  static oop  non_reflection_class_loader(oop loader);

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::ClassLoader_klass());
  }
  static bool is_instance(oop obj);

  static oop unnamedModule(oop loader);

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.System objects

class java_lang_System : AllStatic {
 private:
  static int _static_in_offset;
  static int _static_out_offset;
  static int _static_err_offset;
  static int _static_security_offset;
  static int _static_allow_security_offset;
  static int _static_never_offset;

 public:
  static int  in_offset() { CHECK_INIT(_static_in_offset); }
  static int out_offset() { CHECK_INIT(_static_out_offset); }
  static int err_offset() { CHECK_INIT(_static_err_offset); }
  static bool allow_security_manager();
  static bool has_security_manager();

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.StackTraceElement objects

class java_lang_StackTraceElement: AllStatic {
 private:
  static int _declaringClassObject_offset;
  static int _classLoaderName_offset;
  static int _moduleName_offset;
  static int _moduleVersion_offset;
  static int _declaringClass_offset;
  static int _methodName_offset;
  static int _fileName_offset;
  static int _lineNumber_offset;

  // Setters
  static void set_classLoaderName(oop element, oop value);
  static void set_moduleName(oop element, oop value);
  static void set_moduleVersion(oop element, oop value);
  static void set_declaringClass(oop element, oop value);
  static void set_methodName(oop element, oop value);
  static void set_fileName(oop element, oop value);
  static void set_lineNumber(oop element, int value);
  static void set_declaringClassObject(oop element, oop value);

  static void decode_file_and_line(Handle java_mirror, InstanceKlass* holder, int version,
                                   const methodHandle& method, int bci,
                                   Symbol*& source, oop& source_file, int& line_number, TRAPS);

 public:
  // Create an instance of StackTraceElement
  static oop create(const methodHandle& method, int bci, TRAPS);

  static void fill_in(Handle element, InstanceKlass* holder, const methodHandle& method,
                      int version, int bci, Symbol* name, TRAPS);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

#if INCLUDE_JVMCI
  static void decode(const methodHandle& method, int bci, Symbol*& fileName, int& lineNumber, TRAPS);
#endif

  // Debugging
  friend class JavaClasses;
};


class Backtrace: AllStatic {
 public:
  // Helper backtrace functions to store bci|version together.
  static int merge_bci_and_version(int bci, int version);
  static int merge_mid_and_cpref(int mid, int cpref);
  static int bci_at(unsigned int merged);
  static int version_at(unsigned int merged);
  static int mid_at(unsigned int merged);
  static int cpref_at(unsigned int merged);
  static int get_line_number(Method* method, int bci);
  static Symbol* get_source_file_name(InstanceKlass* holder, int version);

  // Debugging
  friend class JavaClasses;
};

// Interface to java.lang.StackFrameInfo objects

#define STACKFRAMEINFO_INJECTED_FIELDS(macro)                      \
  macro(java_lang_StackFrameInfo, version, short_signature, false)

class java_lang_StackFrameInfo: AllStatic {
private:
  static int _memberName_offset;
  static int _bci_offset;
  static int _version_offset;

  static Method* get_method(Handle stackFrame, InstanceKlass* holder, TRAPS);

public:
  // Setters
  static void set_method_and_bci(Handle stackFrame, const methodHandle& method, int bci, TRAPS);
  static void set_bci(oop info, int value);

  static void set_version(oop info, short value);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  static void to_stack_trace_element(Handle stackFrame, Handle stack_trace_element, TRAPS);

  // Debugging
  friend class JavaClasses;
};

class java_lang_LiveStackFrameInfo: AllStatic {
 private:
  static int _monitors_offset;
  static int _locals_offset;
  static int _operands_offset;
  static int _mode_offset;

 public:
  static void set_monitors(oop info, oop value);
  static void set_locals(oop info, oop value);
  static void set_operands(oop info, oop value);
  static void set_mode(oop info, int value);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Debugging
  friend class JavaClasses;
};

// Interface to java.lang.reflect.RecordComponent objects

class java_lang_reflect_RecordComponent: AllStatic {
 private:
  static int _clazz_offset;
  static int _name_offset;
  static int _type_offset;
  static int _accessor_offset;
  static int _signature_offset;
  static int _annotations_offset;
  static int _typeAnnotations_offset;

  // Setters
  static void set_clazz(oop element, oop value);
  static void set_name(oop element, oop value);
  static void set_type(oop element, oop value);
  static void set_accessor(oop element, oop value);
  static void set_signature(oop element, oop value);
  static void set_annotations(oop element, oop value);
  static void set_typeAnnotations(oop element, oop value);

 public:
  // Create an instance of RecordComponent
  static oop create(InstanceKlass* holder, RecordComponent* component, TRAPS);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Debugging
  friend class JavaClasses;
};


// Interface to java.lang.AssertionStatusDirectives objects

class java_lang_AssertionStatusDirectives: AllStatic {
 private:
  static int _classes_offset;
  static int _classEnabled_offset;
  static int _packages_offset;
  static int _packageEnabled_offset;
  static int _deflt_offset;

 public:
  // Setters
  static void set_classes(oop obj, oop val);
  static void set_classEnabled(oop obj, oop val);
  static void set_packages(oop obj, oop val);
  static void set_packageEnabled(oop obj, oop val);
  static void set_deflt(oop obj, bool val);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Debugging
  friend class JavaClasses;
};


class java_util_concurrent_locks_AbstractOwnableSynchronizer : AllStatic {
 private:
  static int  _owner_offset;
 public:
  static void compute_offsets();
  static oop  get_owner_threadObj(oop obj);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

 // Interface to jdk.internal.misc.UnsafeConsants

class jdk_internal_misc_UnsafeConstants : AllStatic {
 public:
  static void set_unsafe_constants();
  static void compute_offsets() { }
  static void serialize_offsets(SerializeClosure* f) { }
};

// Interface to jdk.internal.vm.vector.VectorSupport.VectorPayload objects

class vector_VectorPayload : AllStatic {
 private:
  static int _payload_offset;
 public:
  static void set_payload(oop o, oop val);

  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;

  // Testers
  static bool is_subclass(Klass* klass) {
    return klass->is_subclass_of(vmClasses::vector_VectorPayload_klass());
  }
  static bool is_instance(oop obj);
};

class java_lang_Integer : AllStatic {
public:
  static jint value(oop obj);
};

class java_lang_Long : AllStatic {
public:
  static jlong value(oop obj);
};

class java_lang_Character : AllStatic {
public:
  static jchar value(oop obj);
};

class java_lang_Short : AllStatic {
public:
  static jshort value(oop obj);
};

class java_lang_Byte : AllStatic {
public:
  static jbyte value(oop obj);
};

class java_lang_Boolean : AllStatic {
 private:
  static int _static_TRUE_offset;
  static int _static_FALSE_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static oop  get_TRUE(InstanceKlass *k);
  static oop  get_FALSE(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
  static jboolean value(oop obj);
};

class java_lang_Integer_IntegerCache : AllStatic {
 private:
  static int _static_cache_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static objArrayOop  cache(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

class java_lang_Long_LongCache : AllStatic {
 private:
  static int _static_cache_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static objArrayOop  cache(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

class java_lang_Character_CharacterCache : AllStatic {
 private:
  static int _static_cache_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static objArrayOop  cache(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

class java_lang_Short_ShortCache : AllStatic {
 private:
  static int _static_cache_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static objArrayOop  cache(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

class java_lang_Byte_ByteCache : AllStatic {
 private:
  static int _static_cache_offset;
 public:
  static Symbol* symbol();
  static void compute_offsets(InstanceKlass* k);
  static objArrayOop  cache(InstanceKlass *k);
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};


// Interface to java.lang.InternalError objects

#define INTERNALERROR_INJECTED_FIELDS(macro)                      \
  macro(java_lang_InternalError, during_unsafe_access, bool_signature, false)

class java_lang_InternalError : AllStatic {
 private:
  static int _during_unsafe_access_offset;
 public:
  static jboolean during_unsafe_access(oop internal_error);
  static void set_during_unsafe_access(oop internal_error);
  static void compute_offsets();
  static void serialize_offsets(SerializeClosure* f) NOT_CDS_RETURN;
};

// Use to declare fields that need to be injected into Java classes
// for the JVM to use.  The name_index and signature_index are
// declared in vmSymbols.  The may_be_java flag is used to declare
// fields that might already exist in Java but should be injected if
// they don't.  Otherwise the field is unconditionally injected and
// the JVM uses the injected one.  This is to ensure that name
// collisions don't occur.  In general may_be_java should be false
// unless there's a good reason.

class InjectedField {
 public:
  const vmClassID klass_id;
  const vmSymbolID name_index;
  const vmSymbolID signature_index;
  const bool           may_be_java;


  Klass* klass() const      { return vmClasses::klass_at(klass_id); }
  Symbol* name() const      { return lookup_symbol(name_index); }
  Symbol* signature() const { return lookup_symbol(signature_index); }

  int compute_offset();

  // Find the Symbol for this index
  static Symbol* lookup_symbol(vmSymbolID symbol_index) {
    return Symbol::vm_symbol_at(symbol_index);
  }
};

#define DECLARE_INJECTED_FIELD_ENUM(klass, name, signature, may_be_java) \
  klass##_##name##_enum,

#define ALL_INJECTED_FIELDS(macro)          \
  STRING_INJECTED_FIELDS(macro)             \
  CLASS_INJECTED_FIELDS(macro)              \
  CLASSLOADER_INJECTED_FIELDS(macro)        \
  RESOLVEDMETHOD_INJECTED_FIELDS(macro)     \
  MEMBERNAME_INJECTED_FIELDS(macro)         \
  CALLSITECONTEXT_INJECTED_FIELDS(macro)    \
  STACKFRAMEINFO_INJECTED_FIELDS(macro)     \
  MODULE_INJECTED_FIELDS(macro)             \
  INTERNALERROR_INJECTED_FIELDS(macro)


// Interface to hard-coded offset checking

class JavaClasses : AllStatic {
 private:

  static InjectedField _injected_fields[];

  static bool check_offset(const char *klass_name, int offset, const char *field_name, const char* field_sig) PRODUCT_RETURN0;
 public:
  enum InjectedFieldID {
    ALL_INJECTED_FIELDS(DECLARE_INJECTED_FIELD_ENUM)
    MAX_enum
  };

  static int compute_injected_offset(InjectedFieldID id);

  static void compute_offsets();
  static void check_offsets() PRODUCT_RETURN;
  static void serialize_offsets(SerializeClosure* soc) NOT_CDS_RETURN;
  static InjectedField* get_injected(Symbol* class_name, int* field_count);
  static bool is_supported_for_archiving(oop obj) NOT_CDS_JAVA_HEAP_RETURN_(false);
};

#undef DECLARE_INJECTED_FIELD_ENUM

#undef CHECK_INIT
#endif // SHARE_CLASSFILE_JAVACLASSES_HPP
