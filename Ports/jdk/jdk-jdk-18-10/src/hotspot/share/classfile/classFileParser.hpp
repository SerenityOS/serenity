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

#ifndef SHARE_CLASSFILE_CLASSFILEPARSER_HPP
#define SHARE_CLASSFILE_CLASSFILEPARSER_HPP

#include "memory/referenceType.hpp"
#include "oops/annotations.hpp"
#include "oops/constantPool.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/typeArrayOop.hpp"
#include "utilities/accessFlags.hpp"

class Annotations;
template <typename T>
class Array;
class ClassFileStream;
class ClassLoaderData;
class ClassLoadInfo;
class ClassInstanceInfo;
class CompressedLineNumberWriteStream;
class ConstMethod;
class FieldInfo;
template <typename T>
class GrowableArray;
class InstanceKlass;
class RecordComponent;
class Symbol;
class TempNewSymbol;
class FieldLayoutBuilder;

// Utility to collect and compact oop maps during layout
class OopMapBlocksBuilder : public ResourceObj {
 public:
  OopMapBlock* _nonstatic_oop_maps;
  unsigned int _nonstatic_oop_map_count;
  unsigned int _max_nonstatic_oop_maps;

  OopMapBlocksBuilder(unsigned int  max_blocks);
  OopMapBlock* last_oop_map() const;
  void initialize_inherited_blocks(OopMapBlock* blocks, unsigned int nof_blocks);
  void add(int offset, int count);
  void copy(OopMapBlock* dst);
  void compact();
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;
};

// Values needed for oopmap and InstanceKlass creation
class FieldLayoutInfo : public ResourceObj {
 public:
  OopMapBlocksBuilder* oop_map_blocks;
  int _instance_size;
  int _nonstatic_field_size;
  int _static_field_size;
  bool  _has_nonstatic_fields;
};

// Parser for for .class files
//
// The bytes describing the class file structure is read from a Stream object

class ClassFileParser {
  friend class FieldLayoutBuilder;
  friend class FieldLayout;

  class ClassAnnotationCollector;
  class FieldAllocationCount;
  class FieldAnnotationCollector;

 public:
  // The ClassFileParser has an associated "publicity" level
  // It is used to control which subsystems (if any)
  // will observe the parsing (logging, events, tracing).
  // Default level is "BROADCAST", which is equivalent to
  // a "public" parsing attempt.
  //
  // "INTERNAL" level should be entirely private to the
  // caller - this allows for internal reuse of ClassFileParser
  //
  enum Publicity {
    INTERNAL,
    BROADCAST
  };

  enum { LegalClass, LegalField, LegalMethod }; // used to verify unqualified names

 private:
  // Potentially unaligned pointer to various 16-bit entries in the class file
  typedef void unsafe_u2;

  const ClassFileStream* _stream; // Actual input stream
  Symbol* _class_name;
  mutable ClassLoaderData* _loader_data;
  const bool _is_hidden;
  const bool _can_access_vm_annotations;
  int _orig_cp_size;

  // Metadata created before the instance klass is created.  Must be deallocated
  // if not transferred to the InstanceKlass upon successful class loading
  // in which case these pointers have been set to NULL.
  const InstanceKlass* _super_klass;
  ConstantPool* _cp;
  Array<u2>* _fields;
  Array<Method*>* _methods;
  Array<u2>* _inner_classes;
  Array<u2>* _nest_members;
  u2 _nest_host;
  Array<u2>* _permitted_subclasses;
  Array<RecordComponent*>* _record_components;
  Array<InstanceKlass*>* _local_interfaces;
  Array<InstanceKlass*>* _transitive_interfaces;
  Annotations* _combined_annotations;
  AnnotationArray* _class_annotations;
  AnnotationArray* _class_type_annotations;
  Array<AnnotationArray*>* _fields_annotations;
  Array<AnnotationArray*>* _fields_type_annotations;
  InstanceKlass* _klass;  // InstanceKlass* once created.
  InstanceKlass* _klass_to_deallocate; // an InstanceKlass* to be destroyed

  ClassAnnotationCollector* _parsed_annotations;
  FieldAllocationCount* _fac;
  FieldLayoutInfo* _field_info;
  const intArray* _method_ordering;
  GrowableArray<Method*>* _all_mirandas;

  enum { fixed_buffer_size = 128 };
  u_char _linenumbertable_buffer[fixed_buffer_size];

  // Size of Java vtable (in words)
  int _vtable_size;
  int _itable_size;

  int _num_miranda_methods;

  ReferenceType _rt;
  Handle _protection_domain;
  AccessFlags _access_flags;

  // for tracing and notifications
  Publicity _pub_level;

  // Used to keep track of whether a constant pool item 19 or 20 is found.  These
  // correspond to CONSTANT_Module and CONSTANT_Package tags and are not allowed
  // in regular class files.  For class file version >= 53, a CFE cannot be thrown
  // immediately when these are seen because a NCDFE must be thrown if the class's
  // access_flags have ACC_MODULE set.  But, the access_flags haven't been looked
  // at yet.  So, the bad constant pool item is cached here.  A value of zero
  // means that no constant pool item 19 or 20 was found.
  short _bad_constant_seen;

  // class attributes parsed before the instance klass is created:
  bool _synthetic_flag;
  int _sde_length;
  const char* _sde_buffer;
  u2 _sourcefile_index;
  u2 _generic_signature_index;

  u2 _major_version;
  u2 _minor_version;
  u2 _this_class_index;
  u2 _super_class_index;
  u2 _itfs_len;
  u2 _java_fields_count;

  bool _need_verify;
  bool _relax_verify;

  bool _has_nonstatic_concrete_methods;
  bool _declares_nonstatic_concrete_methods;
  bool _has_final_method;
  bool _has_contended_fields;

  // precomputed flags
  bool _has_finalizer;
  bool _has_empty_finalizer;
  bool _has_vanilla_constructor;
  int _max_bootstrap_specifier_index;  // detects BSS values

  void parse_stream(const ClassFileStream* const stream, TRAPS);

  void mangle_hidden_class_name(InstanceKlass* const ik);

  void post_process_parsed_stream(const ClassFileStream* const stream,
                                  ConstantPool* cp,
                                  TRAPS);

  void fill_instance_klass(InstanceKlass* ik, bool cf_changed_in_CFLH,
                           const ClassInstanceInfo& cl_inst_info, TRAPS);

  void set_klass(InstanceKlass* instance);

  void set_class_bad_constant_seen(short bad_constant);
  short class_bad_constant_seen() { return  _bad_constant_seen; }
  void set_class_synthetic_flag(bool x)        { _synthetic_flag = x; }
  void set_class_sourcefile_index(u2 x)        { _sourcefile_index = x; }
  void set_class_generic_signature_index(u2 x) { _generic_signature_index = x; }
  void set_class_sde_buffer(const char* x, int len)  { _sde_buffer = x; _sde_length = len; }

  void create_combined_annotations(TRAPS);
  void apply_parsed_class_attributes(InstanceKlass* k);  // update k
  void apply_parsed_class_metadata(InstanceKlass* k, int fields_count);
  void clear_class_metadata();

  // Constant pool parsing
  void parse_constant_pool_entries(const ClassFileStream* const stream,
                                   ConstantPool* cp,
                                   const int length,
                                   TRAPS);

  void parse_constant_pool(const ClassFileStream* const cfs,
                           ConstantPool* const cp,
                           const int length,
                           TRAPS);

  // Interface parsing
  void parse_interfaces(const ClassFileStream* const stream,
                        const int itfs_len,
                        ConstantPool* const cp,
                        bool* has_nonstatic_concrete_methods,
                        TRAPS);

  const InstanceKlass* parse_super_class(ConstantPool* const cp,
                                         const int super_class_index,
                                         const bool need_verify,
                                         TRAPS);

  // Field parsing
  void parse_field_attributes(const ClassFileStream* const cfs,
                              u2 attributes_count,
                              bool is_static,
                              u2 signature_index,
                              u2* const constantvalue_index_addr,
                              bool* const is_synthetic_addr,
                              u2* const generic_signature_index_addr,
                              FieldAnnotationCollector* parsed_annotations,
                              TRAPS);

  void parse_fields(const ClassFileStream* const cfs,
                    bool is_interface,
                    FieldAllocationCount* const fac,
                    ConstantPool* cp,
                    const int cp_size,
                    u2* const java_fields_count_ptr,
                    TRAPS);

  // Method parsing
  Method* parse_method(const ClassFileStream* const cfs,
                       bool is_interface,
                       const ConstantPool* cp,
                       AccessFlags* const promoted_flags,
                       TRAPS);

  void parse_methods(const ClassFileStream* const cfs,
                     bool is_interface,
                     AccessFlags* const promoted_flags,
                     bool* const has_final_method,
                     bool* const declares_nonstatic_concrete_methods,
                     TRAPS);

  const unsafe_u2* parse_exception_table(const ClassFileStream* const stream,
                                         u4 code_length,
                                         u4 exception_table_length,
                                         TRAPS);

  void parse_linenumber_table(u4 code_attribute_length,
                              u4 code_length,
                              CompressedLineNumberWriteStream**const write_stream,
                              TRAPS);

  const unsafe_u2* parse_localvariable_table(const ClassFileStream* const cfs,
                                             u4 code_length,
                                             u2 max_locals,
                                             u4 code_attribute_length,
                                             u2* const localvariable_table_length,
                                             bool isLVTT,
                                             TRAPS);

  const unsafe_u2* parse_checked_exceptions(const ClassFileStream* const cfs,
                                            u2* const checked_exceptions_length,
                                            u4 method_attribute_length,
                                            TRAPS);

  // Classfile attribute parsing
  u2 parse_generic_signature_attribute(const ClassFileStream* const cfs, TRAPS);
  void parse_classfile_sourcefile_attribute(const ClassFileStream* const cfs, TRAPS);
  void parse_classfile_source_debug_extension_attribute(const ClassFileStream* const cfs,
                                                        int length,
                                                        TRAPS);

  // Check for circularity in InnerClasses attribute.
  bool check_inner_classes_circularity(const ConstantPool* cp, int length, TRAPS);

  u2   parse_classfile_inner_classes_attribute(const ClassFileStream* const cfs,
                                               const ConstantPool* cp,
                                               const u1* const inner_classes_attribute_start,
                                               bool parsed_enclosingmethod_attribute,
                                               u2 enclosing_method_class_index,
                                               u2 enclosing_method_method_index,
                                               TRAPS);

  u2 parse_classfile_nest_members_attribute(const ClassFileStream* const cfs,
                                            const u1* const nest_members_attribute_start,
                                            TRAPS);

  u2 parse_classfile_permitted_subclasses_attribute(const ClassFileStream* const cfs,
                                                    const u1* const permitted_subclasses_attribute_start,
                                                    TRAPS);

  u2 parse_classfile_record_attribute(const ClassFileStream* const cfs,
                                      const ConstantPool* cp,
                                      const u1* const record_attribute_start,
                                      TRAPS);

  void parse_classfile_attributes(const ClassFileStream* const cfs,
                                  ConstantPool* cp,
                                  ClassAnnotationCollector* parsed_annotations,
                                  TRAPS);

  void parse_classfile_synthetic_attribute();
  void parse_classfile_signature_attribute(const ClassFileStream* const cfs, TRAPS);
  void parse_classfile_bootstrap_methods_attribute(const ClassFileStream* const cfs,
                                                   ConstantPool* cp,
                                                   u4 attribute_length,
                                                   TRAPS);

  // Annotations handling
  AnnotationArray* assemble_annotations(const u1* const runtime_visible_annotations,
                                        int runtime_visible_annotations_length,
                                        const u1* const runtime_invisible_annotations,
                                        int runtime_invisible_annotations_length,
                                        TRAPS);

  void set_precomputed_flags(InstanceKlass* k);

  // Format checker methods
  void classfile_parse_error(const char* msg, TRAPS) const;
  void classfile_parse_error(const char* msg, int index, TRAPS) const;
  void classfile_parse_error(const char* msg, const char *name, TRAPS) const;
  void classfile_parse_error(const char* msg,
                             int index,
                             const char *name,
                             TRAPS) const;
  void classfile_parse_error(const char* msg,
                             const char* name,
                             const char* signature,
                             TRAPS) const;

  void classfile_icce_error(const char* msg,
                            const Klass* k,
                            TRAPS) const;

  void classfile_ucve_error(const char* msg,
                            const Symbol* class_name,
                            u2 major,
                            u2 minor,
                            TRAPS) const;

  inline void guarantee_property(bool b, const char* msg, TRAPS) const {
    if (!b) { classfile_parse_error(msg, THREAD); return; }
  }

  void report_assert_property_failure(const char* msg, TRAPS) const PRODUCT_RETURN;
  void report_assert_property_failure(const char* msg, int index, TRAPS) const PRODUCT_RETURN;

  inline void assert_property(bool b, const char* msg, TRAPS) const {
#ifdef ASSERT
    if (!b) {
      report_assert_property_failure(msg, THREAD);
    }
#endif
  }

  inline void assert_property(bool b, const char* msg, int index, TRAPS) const {
#ifdef ASSERT
    if (!b) {
      report_assert_property_failure(msg, index, THREAD);
    }
#endif
  }

  inline void check_property(bool property,
                             const char* msg,
                             int index,
                             TRAPS) const {
    if (_need_verify) {
      guarantee_property(property, msg, index, CHECK);
    } else {
      assert_property(property, msg, index, CHECK);
    }
  }

  inline void check_property(bool property, const char* msg, TRAPS) const {
    if (_need_verify) {
      guarantee_property(property, msg, CHECK);
    } else {
      assert_property(property, msg, CHECK);
    }
  }

  inline void guarantee_property(bool b,
                                 const char* msg,
                                 int index,
                                 TRAPS) const {
    if (!b) { classfile_parse_error(msg, index, THREAD); return; }
  }

  inline void guarantee_property(bool b,
                                 const char* msg,
                                 const char *name,
                                 TRAPS) const {
    if (!b) { classfile_parse_error(msg, name, THREAD); return; }
  }

  inline void guarantee_property(bool b,
                                 const char* msg,
                                 int index,
                                 const char *name,
                                 TRAPS) const {
    if (!b) { classfile_parse_error(msg, index, name, THREAD); return; }
  }

  void throwIllegalSignature(const char* type,
                             const Symbol* name,
                             const Symbol* sig,
                             TRAPS) const;

  void verify_constantvalue(const ConstantPool* const cp,
                            int constantvalue_index,
                            int signature_index,
                            TRAPS) const;

  void verify_legal_utf8(const unsigned char* buffer, int length, TRAPS) const;
  void verify_legal_class_name(const Symbol* name, TRAPS) const;
  void verify_legal_field_name(const Symbol* name, TRAPS) const;
  void verify_legal_method_name(const Symbol* name, TRAPS) const;

  void verify_legal_field_signature(const Symbol* fieldname,
                                    const Symbol* signature,
                                    TRAPS) const;
  int  verify_legal_method_signature(const Symbol* methodname,
                                     const Symbol* signature,
                                     TRAPS) const;
  void verify_legal_name_with_signature(const Symbol* name,
                                        const Symbol* signature,
                                        TRAPS) const;

  void verify_class_version(u2 major, u2 minor, Symbol* class_name, TRAPS);

  void verify_legal_class_modifiers(jint flags, TRAPS) const;
  void verify_legal_field_modifiers(jint flags, bool is_interface, TRAPS) const;
  void verify_legal_method_modifiers(jint flags,
                                     bool is_interface,
                                     const Symbol* name,
                                     TRAPS) const;

  void check_super_class_access(const InstanceKlass* this_klass,
                                TRAPS);

  void check_super_interface_access(const InstanceKlass* this_klass,
                                    TRAPS);

  const char* skip_over_field_signature(const char* signature,
                                        bool void_ok,
                                        unsigned int length,
                                        TRAPS) const;

  // Wrapper for constantTag.is_klass_[or_]reference.
  // In older versions of the VM, Klass*s cannot sneak into early phases of
  // constant pool construction, but in later versions they can.
  // %%% Let's phase out the old is_klass_reference.
  bool valid_klass_reference_at(int index) const {
    return _cp->is_within_bounds(index) &&
             _cp->tag_at(index).is_klass_or_reference();
  }

  // Checks that the cpool index is in range and is a utf8
  bool valid_symbol_at(int cpool_index) const {
    return _cp->is_within_bounds(cpool_index) &&
             _cp->tag_at(cpool_index).is_utf8();
  }

  void copy_localvariable_table(const ConstMethod* cm,
                                int lvt_cnt,
                                u2* const localvariable_table_length,
                                const unsafe_u2** const localvariable_table_start,
                                int lvtt_cnt,
                                u2* const localvariable_type_table_length,
                                const unsafe_u2** const localvariable_type_table_start,
                                TRAPS);

  void copy_method_annotations(ConstMethod* cm,
                               const u1* runtime_visible_annotations,
                               int runtime_visible_annotations_length,
                               const u1* runtime_invisible_annotations,
                               int runtime_invisible_annotations_length,
                               const u1* runtime_visible_parameter_annotations,
                               int runtime_visible_parameter_annotations_length,
                               const u1* runtime_invisible_parameter_annotations,
                               int runtime_invisible_parameter_annotations_length,
                               const u1* runtime_visible_type_annotations,
                               int runtime_visible_type_annotations_length,
                               const u1* runtime_invisible_type_annotations,
                               int runtime_invisible_type_annotations_length,
                               const u1* annotation_default,
                               int annotation_default_length,
                               TRAPS);

  void update_class_name(Symbol* new_name);

 public:
  ClassFileParser(ClassFileStream* stream,
                  Symbol* name,
                  ClassLoaderData* loader_data,
                  const ClassLoadInfo* cl_info,
                  Publicity pub_level,
                  TRAPS);

  ~ClassFileParser();

  InstanceKlass* create_instance_klass(bool cf_changed_in_CFLH, const ClassInstanceInfo& cl_inst_info, TRAPS);

  const ClassFileStream* clone_stream() const;

  void set_klass_to_deallocate(InstanceKlass* klass);

  int static_field_size() const;
  int total_oop_map_count() const;
  jint layout_size() const;

  int vtable_size() const { return _vtable_size; }
  int itable_size() const { return _itable_size; }

  u2 this_class_index() const { return _this_class_index; }

  bool is_hidden() const { return _is_hidden; }
  bool is_interface() const { return _access_flags.is_interface(); }

  ClassLoaderData* loader_data() const { return _loader_data; }
  const Symbol* class_name() const { return _class_name; }
  const InstanceKlass* super_klass() const { return _super_klass; }

  ReferenceType reference_type() const { return _rt; }
  AccessFlags access_flags() const { return _access_flags; }

  bool is_internal() const { return INTERNAL == _pub_level; }

  static bool verify_unqualified_name(const char* name, unsigned int length, int type);

#ifdef ASSERT
  static bool is_internal_format(Symbol* class_name);
#endif

};

#endif // SHARE_CLASSFILE_CLASSFILEPARSER_HPP
