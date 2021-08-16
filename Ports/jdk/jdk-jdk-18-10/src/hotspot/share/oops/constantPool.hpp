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

#ifndef SHARE_OOPS_CONSTANTPOOL_HPP
#define SHARE_OOPS_CONSTANTPOOL_HPP

#include "memory/allocation.hpp"
#include "oops/arrayOop.hpp"
#include "oops/cpCache.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oopHandle.hpp"
#include "oops/symbol.hpp"
#include "oops/typeArrayOop.hpp"
#include "runtime/handles.hpp"
#include "runtime/thread.hpp"
#include "utilities/align.hpp"
#include "utilities/bytes.hpp"
#include "utilities/constantTag.hpp"

// A ConstantPool is an array containing class constants as described in the
// class file.
//
// Most of the constant pool entries are written during class parsing, which
// is safe.  For klass types, the constant pool entry is
// modified when the entry is resolved.  If a klass constant pool
// entry is read without a lock, only the resolved state guarantees that
// the entry in the constant pool is a klass object and not a Symbol*.

class SymbolHashMap;

class CPSlot {
 friend class ConstantPool;
  intptr_t _ptr;
  enum TagBits  {_pseudo_bit = 1};
 public:

  CPSlot(intptr_t ptr): _ptr(ptr) {}
  CPSlot(Symbol* ptr, int tag_bits = 0): _ptr((intptr_t)ptr | tag_bits) {}

  intptr_t value()   { return _ptr; }

  Symbol* get_symbol() {
    return (Symbol*)(_ptr & ~_pseudo_bit);
  }
};

// This represents a JVM_CONSTANT_Class, JVM_CONSTANT_UnresolvedClass, or
// JVM_CONSTANT_UnresolvedClassInError slot in the constant pool.
class CPKlassSlot {
  // cp->symbol_at(_name_index) gives the name of the class.
  int _name_index;

  // cp->_resolved_klasses->at(_resolved_klass_index) gives the Klass* for the class.
  int _resolved_klass_index;
public:
  enum {
    // This is used during constant pool merging where the resolved klass index is
    // not yet known, and will be computed at a later stage (during a call to
    // initialize_unresolved_klasses()).
    _temp_resolved_klass_index = 0xffff
  };
  CPKlassSlot(int n, int rk) {
    _name_index = n;
    _resolved_klass_index = rk;
  }
  int name_index() const {
    return _name_index;
  }
  int resolved_klass_index() const {
    assert(_resolved_klass_index != _temp_resolved_klass_index, "constant pool merging was incomplete");
    return _resolved_klass_index;
  }
};

class ConstantPool : public Metadata {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class BytecodeInterpreter;  // Directly extracts a klass in the pool for fast instanceof/checkcast
  friend class Universe;             // For null constructor
 private:
  // If you add a new field that points to any metaspace object, you
  // must add this field to ConstantPool::metaspace_pointers_do().
  Array<u1>*           _tags;        // the tag array describing the constant pool's contents
  ConstantPoolCache*   _cache;       // the cache holding interpreter runtime information
  InstanceKlass*       _pool_holder; // the corresponding class
  Array<u2>*           _operands;    // for variable-sized (InvokeDynamic) nodes, usually empty

  // Consider using an array of compressed klass pointers to
  // save space on 64-bit platforms.
  Array<Klass*>*       _resolved_klasses;

  u2              _major_version;        // major version number of class file
  u2              _minor_version;        // minor version number of class file

  // Constant pool index to the utf8 entry of the Generic signature,
  // or 0 if none.
  u2              _generic_signature_index;
  // Constant pool index to the utf8 entry for the name of source file
  // containing this klass, 0 if not specified.
  u2              _source_file_name_index;

  enum {
    _has_preresolution    = 1,       // Flags
    _on_stack             = 2,
    _is_shared            = 4,
    _has_dynamic_constant = 8
  };

  u2              _flags;  // old fashioned bit twiddling

  int             _length; // number of elements in the array

  union {
    // set for CDS to restore resolved references
    int                _resolved_reference_length;
    // keeps version number for redefined classes (used in backtrace)
    int                _version;
  } _saved;

  void set_tags(Array<u1>* tags)               { _tags = tags; }
  void tag_at_put(int which, jbyte t)          { tags()->at_put(which, t); }
  void release_tag_at_put(int which, jbyte t)  { tags()->release_at_put(which, t); }

  u1* tag_addr_at(int which) const             { return tags()->adr_at(which); }

  void set_operands(Array<u2>* operands)       { _operands = operands; }

  u2 flags() const                             { return _flags; }
  void set_flags(u2 f)                         { _flags = f; }

 private:
  intptr_t* base() const { return (intptr_t*) (((char*) this) + sizeof(ConstantPool)); }

  CPSlot slot_at(int which) const;

  void slot_at_put(int which, CPSlot s) const {
    assert(is_within_bounds(which), "index out of bounds");
    assert(s.value() != 0, "Caught something");
    *(intptr_t*)&base()[which] = s.value();
  }
  intptr_t* obj_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (intptr_t*) &base()[which];
  }

  jint* int_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (jint*) &base()[which];
  }

  jlong* long_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (jlong*) &base()[which];
  }

  jfloat* float_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (jfloat*) &base()[which];
  }

  jdouble* double_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (jdouble*) &base()[which];
  }

  ConstantPool(Array<u1>* tags);
  ConstantPool() { assert(DumpSharedSpaces || UseSharedSpaces, "only for CDS"); }
 public:
  static ConstantPool* allocate(ClassLoaderData* loader_data, int length, TRAPS);

  virtual bool is_constantPool() const      { return true; }

  Array<u1>* tags() const                   { return _tags; }
  Array<u2>* operands() const               { return _operands; }

  bool has_preresolution() const            { return (_flags & _has_preresolution) != 0; }
  void set_has_preresolution() {
    assert(!is_shared(), "should never be called on shared ConstantPools");
    _flags |= _has_preresolution;
  }

  // minor and major version numbers of class file
  u2 major_version() const                 { return _major_version; }
  void set_major_version(u2 major_version) { _major_version = major_version; }
  u2 minor_version() const                 { return _minor_version; }
  void set_minor_version(u2 minor_version) { _minor_version = minor_version; }

  // generics support
  Symbol* generic_signature() const {
    return (_generic_signature_index == 0) ?
      (Symbol*)NULL : symbol_at(_generic_signature_index);
  }
  u2 generic_signature_index() const                   { return _generic_signature_index; }
  void set_generic_signature_index(u2 sig_index)       { _generic_signature_index = sig_index; }

  // source file name
  Symbol* source_file_name() const {
    return (_source_file_name_index == 0) ?
      (Symbol*)NULL : symbol_at(_source_file_name_index);
  }
  u2 source_file_name_index() const                    { return _source_file_name_index; }
  void set_source_file_name_index(u2 sourcefile_index) { _source_file_name_index = sourcefile_index; }

  void copy_fields(const ConstantPool* orig);

  // Redefine classes support.  If a method refering to this constant pool
  // is on the executing stack, or as a handle in vm code, this constant pool
  // can't be removed from the set of previous versions saved in the instance
  // class.
  bool on_stack() const                      { return (_flags &_on_stack) != 0; }
  void set_on_stack(const bool value);

  // Faster than MetaspaceObj::is_shared() - used by set_on_stack()
  bool is_shared() const                     { return (_flags & _is_shared) != 0; }

  bool has_dynamic_constant() const       { return (_flags & _has_dynamic_constant) != 0; }
  void set_has_dynamic_constant()         { _flags |= _has_dynamic_constant; }

  // Klass holding pool
  InstanceKlass* pool_holder() const      { return _pool_holder; }
  void set_pool_holder(InstanceKlass* k)  { _pool_holder = k; }
  InstanceKlass** pool_holder_addr()      { return &_pool_holder; }

  // Interpreter runtime support
  ConstantPoolCache* cache() const        { return _cache; }
  void set_cache(ConstantPoolCache* cache){ _cache = cache; }

  virtual void metaspace_pointers_do(MetaspaceClosure* iter);
  virtual MetaspaceObj::Type type() const { return ConstantPoolType; }

  // Create object cache in the constant pool
  void initialize_resolved_references(ClassLoaderData* loader_data,
                                      const intStack& reference_map,
                                      int constant_pool_map_length,
                                      TRAPS);

  // resolved strings, methodHandles and callsite objects from the constant pool
  objArrayOop resolved_references()  const;
  objArrayOop resolved_references_or_null()  const;
  // mapping resolved object array indexes to cp indexes and back.
  int object_to_cp_index(int index)         { return reference_map()->at(index); }
  int cp_to_object_index(int index);

  void set_resolved_klasses(Array<Klass*>* rk)  { _resolved_klasses = rk; }
  Array<Klass*>* resolved_klasses() const       { return _resolved_klasses; }
  void allocate_resolved_klasses(ClassLoaderData* loader_data, int num_klasses, TRAPS);
  void initialize_unresolved_klasses(ClassLoaderData* loader_data, TRAPS);

  // Invokedynamic indexes.
  // They must look completely different from normal indexes.
  // The main reason is that byte swapping is sometimes done on normal indexes.
  // Finally, it is helpful for debugging to tell the two apart.
  static bool is_invokedynamic_index(int i) { return (i < 0); }
  static int  decode_invokedynamic_index(int i) { assert(is_invokedynamic_index(i),  ""); return ~i; }
  static int  encode_invokedynamic_index(int i) { assert(!is_invokedynamic_index(i), ""); return ~i; }


  // The invokedynamic points at a CP cache entry.  This entry points back
  // at the original CP entry (CONSTANT_InvokeDynamic) and also (via f2) at an entry
  // in the resolved_references array (which provides the appendix argument).
  int invokedynamic_cp_cache_index(int indy_index) const {
    assert(is_invokedynamic_index(indy_index), "should be a invokedynamic index");
    int cache_index = decode_invokedynamic_index(indy_index);
    return cache_index;
  }
  ConstantPoolCacheEntry* invokedynamic_cp_cache_entry_at(int indy_index) const {
    // decode index that invokedynamic points to.
    int cp_cache_index = invokedynamic_cp_cache_index(indy_index);
    return cache()->entry_at(cp_cache_index);
  }
  // Given the per-instruction index of an indy instruction, report the
  // main constant pool entry for its bootstrap specifier.
  // From there, uncached_name/signature_ref_at will get the name/type.
  int invokedynamic_bootstrap_ref_index_at(int indy_index) const {
    return invokedynamic_cp_cache_entry_at(indy_index)->constant_pool_index();
  }

  // Assembly code support
  static int tags_offset_in_bytes()         { return offset_of(ConstantPool, _tags); }
  static int cache_offset_in_bytes()        { return offset_of(ConstantPool, _cache); }
  static int pool_holder_offset_in_bytes()  { return offset_of(ConstantPool, _pool_holder); }
  static int resolved_klasses_offset_in_bytes()    { return offset_of(ConstantPool, _resolved_klasses); }

  // Storing constants

  // For temporary use while constructing constant pool
  void klass_index_at_put(int which, int name_index) {
    tag_at_put(which, JVM_CONSTANT_ClassIndex);
    *int_at_addr(which) = name_index;
  }

  // Hidden class support:
  void klass_at_put(int class_index, Klass* k);

  void unresolved_klass_at_put(int which, int name_index, int resolved_klass_index) {
    release_tag_at_put(which, JVM_CONSTANT_UnresolvedClass);

    assert((name_index & 0xffff0000) == 0, "must be");
    assert((resolved_klass_index & 0xffff0000) == 0, "must be");
    *int_at_addr(which) =
      build_int_from_shorts((jushort)resolved_klass_index, (jushort)name_index);
  }

  void method_handle_index_at_put(int which, int ref_kind, int ref_index) {
    tag_at_put(which, JVM_CONSTANT_MethodHandle);
    *int_at_addr(which) = ((jint) ref_index<<16) | ref_kind;
  }

  void method_type_index_at_put(int which, int ref_index) {
    tag_at_put(which, JVM_CONSTANT_MethodType);
    *int_at_addr(which) = ref_index;
  }

  void dynamic_constant_at_put(int which, int bsms_attribute_index, int name_and_type_index) {
    tag_at_put(which, JVM_CONSTANT_Dynamic);
    *int_at_addr(which) = ((jint) name_and_type_index<<16) | bsms_attribute_index;
  }

  void invoke_dynamic_at_put(int which, int bsms_attribute_index, int name_and_type_index) {
    tag_at_put(which, JVM_CONSTANT_InvokeDynamic);
    *int_at_addr(which) = ((jint) name_and_type_index<<16) | bsms_attribute_index;
  }

  void unresolved_string_at_put(int which, Symbol* s) {
    release_tag_at_put(which, JVM_CONSTANT_String);
    slot_at_put(which, CPSlot(s));
  }

  void int_at_put(int which, jint i) {
    tag_at_put(which, JVM_CONSTANT_Integer);
    *int_at_addr(which) = i;
  }

  void long_at_put(int which, jlong l) {
    tag_at_put(which, JVM_CONSTANT_Long);
    // *long_at_addr(which) = l;
    Bytes::put_native_u8((address)long_at_addr(which), *((u8*) &l));
  }

  void float_at_put(int which, jfloat f) {
    tag_at_put(which, JVM_CONSTANT_Float);
    *float_at_addr(which) = f;
  }

  void double_at_put(int which, jdouble d) {
    tag_at_put(which, JVM_CONSTANT_Double);
    // *double_at_addr(which) = d;
    // u8 temp = *(u8*) &d;
    Bytes::put_native_u8((address) double_at_addr(which), *((u8*) &d));
  }

  Symbol** symbol_at_addr(int which) const {
    assert(is_within_bounds(which), "index out of bounds");
    return (Symbol**) &base()[which];
  }

  void symbol_at_put(int which, Symbol* s) {
    assert(s->refcount() != 0, "should have nonzero refcount");
    tag_at_put(which, JVM_CONSTANT_Utf8);
    *symbol_at_addr(which) = s;
  }

  void string_at_put(int which, int obj_index, oop str);

  // For temporary use while constructing constant pool
  void string_index_at_put(int which, int string_index) {
    tag_at_put(which, JVM_CONSTANT_StringIndex);
    *int_at_addr(which) = string_index;
  }

  void field_at_put(int which, int class_index, int name_and_type_index) {
    tag_at_put(which, JVM_CONSTANT_Fieldref);
    *int_at_addr(which) = ((jint) name_and_type_index<<16) | class_index;
  }

  void method_at_put(int which, int class_index, int name_and_type_index) {
    tag_at_put(which, JVM_CONSTANT_Methodref);
    *int_at_addr(which) = ((jint) name_and_type_index<<16) | class_index;
  }

  void interface_method_at_put(int which, int class_index, int name_and_type_index) {
    tag_at_put(which, JVM_CONSTANT_InterfaceMethodref);
    *int_at_addr(which) = ((jint) name_and_type_index<<16) | class_index;  // Not so nice
  }

  void name_and_type_at_put(int which, int name_index, int signature_index) {
    tag_at_put(which, JVM_CONSTANT_NameAndType);
    *int_at_addr(which) = ((jint) signature_index<<16) | name_index;  // Not so nice
  }

  // Tag query

  constantTag tag_at(int which) const { return (constantTag)tags()->at_acquire(which); }

  // Fetching constants

  Klass* klass_at(int which, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return klass_at_impl(h_this, which, THREAD);
  }

  CPKlassSlot klass_slot_at(int which) const {
    assert(tag_at(which).is_unresolved_klass() || tag_at(which).is_klass(),
           "Corrupted constant pool");
    int value = *int_at_addr(which);
    int name_index = extract_high_short_from_int(value);
    int resolved_klass_index = extract_low_short_from_int(value);
    return CPKlassSlot(name_index, resolved_klass_index);
  }

  Symbol* klass_name_at(int which) const;  // Returns the name, w/o resolving.
  int klass_name_index_at(int which) const {
    return klass_slot_at(which).name_index();
  }

  Klass* resolved_klass_at(int which) const;  // Used by Compiler

  // RedefineClasses() API support:
  Symbol* klass_at_noresolve(int which) { return klass_name_at(which); }
  void temp_unresolved_klass_at_put(int which, int name_index) {
    // Used only during constant pool merging for class redefinition. The resolved klass index
    // will be initialized later by a call to initialize_unresolved_klasses().
    unresolved_klass_at_put(which, name_index, CPKlassSlot::_temp_resolved_klass_index);
  }

  jint int_at(int which) {
    assert(tag_at(which).is_int(), "Corrupted constant pool");
    return *int_at_addr(which);
  }

  jlong long_at(int which) {
    assert(tag_at(which).is_long(), "Corrupted constant pool");
    // return *long_at_addr(which);
    u8 tmp = Bytes::get_native_u8((address)&base()[which]);
    return *((jlong*)&tmp);
  }

  jfloat float_at(int which) {
    assert(tag_at(which).is_float(), "Corrupted constant pool");
    return *float_at_addr(which);
  }

  jdouble double_at(int which) {
    assert(tag_at(which).is_double(), "Corrupted constant pool");
    u8 tmp = Bytes::get_native_u8((address)&base()[which]);
    return *((jdouble*)&tmp);
  }

  Symbol* symbol_at(int which) const {
    assert(tag_at(which).is_utf8(), "Corrupted constant pool");
    return *symbol_at_addr(which);
  }

  oop string_at(int which, int obj_index, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return string_at_impl(h_this, which, obj_index, THREAD);
  }
  oop string_at(int which, TRAPS) {
    int obj_index = cp_to_object_index(which);
    return string_at(which, obj_index, THREAD);
  }

  // Version that can be used before string oop array is created.
  oop uncached_string_at(int which, TRAPS);

  // only called when we are sure a string entry is already resolved (via an
  // earlier string_at call.
  oop resolved_string_at(int which) {
    assert(tag_at(which).is_string(), "Corrupted constant pool");
    // Must do an acquire here in case another thread resolved the klass
    // behind our back, lest we later load stale values thru the oop.
    // we might want a volatile_obj_at in ObjArrayKlass.
    int obj_index = cp_to_object_index(which);
    return resolved_references()->obj_at(obj_index);
  }

  Symbol* unresolved_string_at(int which) {
    assert(tag_at(which).is_string(), "Corrupted constant pool");
    Symbol* sym = slot_at(which).get_symbol();
    return sym;
  }

  // Returns an UTF8 for a CONSTANT_String entry at a given index.
  // UTF8 char* representation was chosen to avoid conversion of
  // java_lang_Strings at resolved entries into Symbol*s
  // or vice versa.
  char* string_at_noresolve(int which);

  jint name_and_type_at(int which) {
    assert(tag_at(which).is_name_and_type(), "Corrupted constant pool");
    return *int_at_addr(which);
  }

  int method_handle_ref_kind_at(int which) {
    assert(tag_at(which).is_method_handle() ||
           tag_at(which).is_method_handle_in_error(), "Corrupted constant pool");
    return extract_low_short_from_int(*int_at_addr(which));  // mask out unwanted ref_index bits
  }
  int method_handle_index_at(int which) {
    assert(tag_at(which).is_method_handle() ||
           tag_at(which).is_method_handle_in_error(), "Corrupted constant pool");
    return extract_high_short_from_int(*int_at_addr(which));  // shift out unwanted ref_kind bits
  }
  int method_type_index_at(int which) {
    assert(tag_at(which).is_method_type() ||
           tag_at(which).is_method_type_in_error(), "Corrupted constant pool");
    return *int_at_addr(which);
  }

  // Derived queries:
  Symbol* method_handle_name_ref_at(int which) {
    int member = method_handle_index_at(which);
    return impl_name_ref_at(member, true);
  }
  Symbol* method_handle_signature_ref_at(int which) {
    int member = method_handle_index_at(which);
    return impl_signature_ref_at(member, true);
  }
  int method_handle_klass_index_at(int which) {
    int member = method_handle_index_at(which);
    return impl_klass_ref_index_at(member, true);
  }
  Symbol* method_type_signature_at(int which) {
    int sym = method_type_index_at(which);
    return symbol_at(sym);
  }

  int bootstrap_name_and_type_ref_index_at(int which) {
    assert(tag_at(which).has_bootstrap(), "Corrupted constant pool");
    return extract_high_short_from_int(*int_at_addr(which));
  }
  int bootstrap_methods_attribute_index(int which) {
    assert(tag_at(which).has_bootstrap(), "Corrupted constant pool");
    return extract_low_short_from_int(*int_at_addr(which));
  }
  int bootstrap_operand_base(int which) {
    int bsms_attribute_index = bootstrap_methods_attribute_index(which);
    return operand_offset_at(operands(), bsms_attribute_index);
  }
  // The first part of the operands array consists of an index into the second part.
  // Extract a 32-bit index value from the first part.
  static int operand_offset_at(Array<u2>* operands, int bsms_attribute_index) {
    int n = (bsms_attribute_index * 2);
    assert(n >= 0 && n+2 <= operands->length(), "oob");
    // The first 32-bit index points to the beginning of the second part
    // of the operands array.  Make sure this index is in the first part.
    DEBUG_ONLY(int second_part = build_int_from_shorts(operands->at(0),
                                                       operands->at(1)));
    assert(second_part == 0 || n+2 <= second_part, "oob (2)");
    int offset = build_int_from_shorts(operands->at(n+0),
                                       operands->at(n+1));
    // The offset itself must point into the second part of the array.
    assert(offset == 0 || offset >= second_part && offset <= operands->length(), "oob (3)");
    return offset;
  }
  static void operand_offset_at_put(Array<u2>* operands, int bsms_attribute_index, int offset) {
    int n = bsms_attribute_index * 2;
    assert(n >= 0 && n+2 <= operands->length(), "oob");
    operands->at_put(n+0, extract_low_short_from_int(offset));
    operands->at_put(n+1, extract_high_short_from_int(offset));
  }
  static int operand_array_length(Array<u2>* operands) {
    if (operands == NULL || operands->length() == 0)  return 0;
    int second_part = operand_offset_at(operands, 0);
    return (second_part / 2);
  }

#ifdef ASSERT
  // operand tuples fit together exactly, end to end
  static int operand_limit_at(Array<u2>* operands, int bsms_attribute_index) {
    int nextidx = bsms_attribute_index + 1;
    if (nextidx == operand_array_length(operands))
      return operands->length();
    else
      return operand_offset_at(operands, nextidx);
  }
  int bootstrap_operand_limit(int which) {
    int bsms_attribute_index = bootstrap_methods_attribute_index(which);
    return operand_limit_at(operands(), bsms_attribute_index);
  }
#endif //ASSERT

  // Layout of InvokeDynamic and Dynamic bootstrap method specifier
  // data in second part of operands array.  This encodes one record in
  // the BootstrapMethods attribute.  The whole specifier also includes
  // the name and type information from the main constant pool entry.
  enum {
         _indy_bsm_offset  = 0,  // CONSTANT_MethodHandle bsm
         _indy_argc_offset = 1,  // u2 argc
         _indy_argv_offset = 2   // u2 argv[argc]
  };

  // These functions are used in RedefineClasses for CP merge

  int operand_offset_at(int bsms_attribute_index) {
    assert(0 <= bsms_attribute_index &&
           bsms_attribute_index < operand_array_length(operands()),
           "Corrupted CP operands");
    return operand_offset_at(operands(), bsms_attribute_index);
  }
  int operand_bootstrap_method_ref_index_at(int bsms_attribute_index) {
    int offset = operand_offset_at(bsms_attribute_index);
    return operands()->at(offset + _indy_bsm_offset);
  }
  int operand_argument_count_at(int bsms_attribute_index) {
    int offset = operand_offset_at(bsms_attribute_index);
    int argc = operands()->at(offset + _indy_argc_offset);
    return argc;
  }
  int operand_argument_index_at(int bsms_attribute_index, int j) {
    int offset = operand_offset_at(bsms_attribute_index);
    return operands()->at(offset + _indy_argv_offset + j);
  }
  int operand_next_offset_at(int bsms_attribute_index) {
    int offset = operand_offset_at(bsms_attribute_index) + _indy_argv_offset
                   + operand_argument_count_at(bsms_attribute_index);
    return offset;
  }
  // Compare a bootstrap specifier data in the operands arrays
  bool compare_operand_to(int bsms_attribute_index1, const constantPoolHandle& cp2,
                          int bsms_attribute_index2);
  // Find a bootstrap specifier data in the operands array
  int find_matching_operand(int bsms_attribute_index, const constantPoolHandle& search_cp,
                            int operands_cur_len);
  // Resize the operands array with delta_len and delta_size
  void resize_operands(int delta_len, int delta_size, TRAPS);
  // Extend the operands array with the length and size of the ext_cp operands
  void extend_operands(const constantPoolHandle& ext_cp, TRAPS);
  // Shrink the operands array to a smaller array with new_len length
  void shrink_operands(int new_len, TRAPS);

  int bootstrap_method_ref_index_at(int which) {
    assert(tag_at(which).has_bootstrap(), "Corrupted constant pool");
    int op_base = bootstrap_operand_base(which);
    return operands()->at(op_base + _indy_bsm_offset);
  }
  int bootstrap_argument_count_at(int which) {
    assert(tag_at(which).has_bootstrap(), "Corrupted constant pool");
    int op_base = bootstrap_operand_base(which);
    int argc = operands()->at(op_base + _indy_argc_offset);
    DEBUG_ONLY(int end_offset = op_base + _indy_argv_offset + argc;
               int next_offset = bootstrap_operand_limit(which));
    assert(end_offset == next_offset, "matched ending");
    return argc;
  }
  int bootstrap_argument_index_at(int which, int j) {
    int op_base = bootstrap_operand_base(which);
    DEBUG_ONLY(int argc = operands()->at(op_base + _indy_argc_offset));
    assert((uint)j < (uint)argc, "oob");
    return operands()->at(op_base + _indy_argv_offset + j);
  }

  // The following methods (name/signature/klass_ref_at, klass_ref_at_noresolve,
  // name_and_type_ref_index_at) all expect to be passed indices obtained
  // directly from the bytecode.
  // If the indices are meant to refer to fields or methods, they are
  // actually rewritten constant pool cache indices.
  // The routine remap_instruction_operand_from_cache manages the adjustment
  // of these values back to constant pool indices.

  // There are also "uncached" versions which do not adjust the operand index; see below.

  // FIXME: Consider renaming these with a prefix "cached_" to make the distinction clear.
  // In a few cases (the verifier) there are uses before a cpcache has been built,
  // which are handled by a dynamic check in remap_instruction_operand_from_cache.
  // FIXME: Remove the dynamic check, and adjust all callers to specify the correct mode.

  // Lookup for entries consisting of (klass_index, name_and_type index)
  Klass* klass_ref_at(int which, TRAPS);
  Symbol* klass_ref_at_noresolve(int which);
  Symbol* name_ref_at(int which)                { return impl_name_ref_at(which, false); }
  Symbol* signature_ref_at(int which)           { return impl_signature_ref_at(which, false); }

  int klass_ref_index_at(int which)               { return impl_klass_ref_index_at(which, false); }
  int name_and_type_ref_index_at(int which)       { return impl_name_and_type_ref_index_at(which, false); }

  int remap_instruction_operand_from_cache(int operand);  // operand must be biased by CPCACHE_INDEX_TAG

  constantTag tag_ref_at(int cp_cache_index)      { return impl_tag_ref_at(cp_cache_index, false); }

  // Lookup for entries consisting of (name_index, signature_index)
  int name_ref_index_at(int which_nt);            // ==  low-order jshort of name_and_type_at(which_nt)
  int signature_ref_index_at(int which_nt);       // == high-order jshort of name_and_type_at(which_nt)

  BasicType basic_type_for_signature_at(int which) const;

  // Resolve string constants (to prevent allocation during compilation)
  void resolve_string_constants(TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    resolve_string_constants_impl(h_this, CHECK);
  }

  // CDS support
  void archive_resolved_references() NOT_CDS_JAVA_HEAP_RETURN;
  void add_dumped_interned_strings() NOT_CDS_JAVA_HEAP_RETURN;
  void resolve_class_constants(TRAPS) NOT_CDS_JAVA_HEAP_RETURN;
  void remove_unshareable_info();
  void restore_unshareable_info(TRAPS);
  // The ConstantPool vtable is restored by this call when the ConstantPool is
  // in the shared archive.  See patch_klass_vtables() in metaspaceShared.cpp for
  // all the gory details.  SA, dtrace and pstack helpers distinguish metadata
  // by their vtable.
  void restore_vtable() { guarantee(is_constantPool(), "vtable restored by this call"); }

 private:
  enum { _no_index_sentinel = -1, _possible_index_sentinel = -2 };
 public:

  // Get the tag for a constant, which may involve a constant dynamic
  constantTag constant_tag_at(int which);
  // Get the basic type for a constant, which may involve a constant dynamic
  BasicType basic_type_for_constant_at(int which);

  // Resolve late bound constants.
  oop resolve_constant_at(int index, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return resolve_constant_at_impl(h_this, index, _no_index_sentinel, NULL, THREAD);
  }

  oop resolve_cached_constant_at(int cache_index, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return resolve_constant_at_impl(h_this, _no_index_sentinel, cache_index, NULL, THREAD);
  }

  oop resolve_possibly_cached_constant_at(int pool_index, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return resolve_constant_at_impl(h_this, pool_index, _possible_index_sentinel, NULL, THREAD);
  }

  oop find_cached_constant_at(int pool_index, bool& found_it, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    return resolve_constant_at_impl(h_this, pool_index, _possible_index_sentinel, &found_it, THREAD);
  }

  void copy_bootstrap_arguments_at(int index,
                                   int start_arg, int end_arg,
                                   objArrayHandle info, int pos,
                                   bool must_resolve, Handle if_not_available, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    copy_bootstrap_arguments_at_impl(h_this, index, start_arg, end_arg,
                                     info, pos, must_resolve, if_not_available, THREAD);
  }

  // Klass name matches name at offset
  bool klass_name_at_matches(const InstanceKlass* k, int which);

  // Sizing
  int length() const                   { return _length; }
  void set_length(int length)          { _length = length; }

  // Tells whether index is within bounds.
  bool is_within_bounds(int index) const {
    return 0 <= index && index < length();
  }

  // Sizing (in words)
  static int header_size()             {
    return align_up((int)sizeof(ConstantPool), wordSize) / wordSize;
  }
  static int size(int length)          { return align_metadata_size(header_size() + length); }
  int size() const                     { return size(length()); }

  // ConstantPools should be stored in the read-only region of CDS archive.
  static bool is_read_only_by_default() { return true; }

  friend class ClassFileParser;
  friend class SystemDictionary;

  // Used by CDS. These classes need to access the private ConstantPool() constructor.
  template <class T> friend class CppVtableTesterA;
  template <class T> friend class CppVtableTesterB;
  template <class T> friend class CppVtableCloner;

  // Used by compiler to prevent classloading.
  static Method*          method_at_if_loaded      (const constantPoolHandle& this_cp, int which);
  static bool       has_appendix_at_if_loaded      (const constantPoolHandle& this_cp, int which);
  static oop            appendix_at_if_loaded      (const constantPoolHandle& this_cp, int which);
  static bool has_local_signature_at_if_loaded     (const constantPoolHandle& this_cp, int which);
  static Klass*            klass_at_if_loaded      (const constantPoolHandle& this_cp, int which);

  // Routines currently used for annotations (only called by jvm.cpp) but which might be used in the
  // future by other Java code. These take constant pool indices rather than
  // constant pool cache indices as do the peer methods above.
  Symbol* uncached_klass_ref_at_noresolve(int which);
  Symbol* uncached_name_ref_at(int which)                 { return impl_name_ref_at(which, true); }
  Symbol* uncached_signature_ref_at(int which)            { return impl_signature_ref_at(which, true); }
  int       uncached_klass_ref_index_at(int which)          { return impl_klass_ref_index_at(which, true); }
  int       uncached_name_and_type_ref_index_at(int which)  { return impl_name_and_type_ref_index_at(which, true); }

  // Sharing
  int pre_resolve_shared_klasses(TRAPS);

  // Debugging
  const char* printable_name_at(int which) PRODUCT_RETURN0;

#ifdef ASSERT
  enum { CPCACHE_INDEX_TAG = 0x10000 };  // helps keep CP cache indices distinct from CP indices
#else
  enum { CPCACHE_INDEX_TAG = 0 };        // in product mode, this zero value is a no-op
#endif //ASSERT

  static int decode_cpcache_index(int raw_index, bool invokedynamic_ok = false) {
    if (invokedynamic_ok && is_invokedynamic_index(raw_index))
      return decode_invokedynamic_index(raw_index);
    else
      return raw_index - CPCACHE_INDEX_TAG;
  }

 private:

  void set_resolved_references(OopHandle s) { _cache->set_resolved_references(s); }
  Array<u2>* reference_map() const        {  return (_cache == NULL) ? NULL :  _cache->reference_map(); }
  void set_reference_map(Array<u2>* o)    { _cache->set_reference_map(o); }

  Symbol* impl_name_ref_at(int which, bool uncached);
  Symbol* impl_signature_ref_at(int which, bool uncached);

  int       impl_klass_ref_index_at(int which, bool uncached);
  int       impl_name_and_type_ref_index_at(int which, bool uncached);
  constantTag impl_tag_ref_at(int which, bool uncached);

  // Used while constructing constant pool (only by ClassFileParser)
  jint klass_index_at(int which) {
    assert(tag_at(which).is_klass_index(), "Corrupted constant pool");
    return *int_at_addr(which);
  }

  jint string_index_at(int which) {
    assert(tag_at(which).is_string_index(), "Corrupted constant pool");
    return *int_at_addr(which);
  }

  // Performs the LinkResolver checks
  static void verify_constant_pool_resolve(const constantPoolHandle& this_cp, Klass* klass, TRAPS);

  // Implementation of methods that needs an exposed 'this' pointer, in order to
  // handle GC while executing the method
  static Klass* klass_at_impl(const constantPoolHandle& this_cp, int which, TRAPS);
  static oop string_at_impl(const constantPoolHandle& this_cp, int which, int obj_index, TRAPS);

  static void trace_class_resolution(const constantPoolHandle& this_cp, Klass* k);

  // Resolve string constants (to prevent allocation during compilation)
  static void resolve_string_constants_impl(const constantPoolHandle& this_cp, TRAPS);

  static oop resolve_constant_at_impl(const constantPoolHandle& this_cp, int index, int cache_index,
                                      bool* status_return, TRAPS);
  static void copy_bootstrap_arguments_at_impl(const constantPoolHandle& this_cp, int index,
                                               int start_arg, int end_arg,
                                               objArrayHandle info, int pos,
                                               bool must_resolve, Handle if_not_available, TRAPS);

  // Exception handling
  static void save_and_throw_exception(const constantPoolHandle& this_cp, int which, constantTag tag, TRAPS);

 public:
  // Exception handling
  static void throw_resolution_error(const constantPoolHandle& this_cp, int which, TRAPS);

  // Merging ConstantPool* support:
  bool compare_entry_to(int index1, const constantPoolHandle& cp2, int index2);
  void copy_cp_to(int start_i, int end_i, const constantPoolHandle& to_cp, int to_i, TRAPS) {
    constantPoolHandle h_this(THREAD, this);
    copy_cp_to_impl(h_this, start_i, end_i, to_cp, to_i, THREAD);
  }
  static void copy_cp_to_impl(const constantPoolHandle& from_cp, int start_i, int end_i, const constantPoolHandle& to_cp, int to_i, TRAPS);
  static void copy_entry_to(const constantPoolHandle& from_cp, int from_i, const constantPoolHandle& to_cp, int to_i);
  static void copy_operands(const constantPoolHandle& from_cp, const constantPoolHandle& to_cp, TRAPS);
  int  find_matching_entry(int pattern_i, const constantPoolHandle& search_cp);
  int  version() const                    { return _saved._version; }
  void set_version(int version)           { _saved._version = version; }
  void increment_and_save_version(int version) {
    _saved._version = version >= 0 ? (version + 1) : version;  // keep overflow
  }

  void set_resolved_reference_length(int length) { _saved._resolved_reference_length = length; }
  int  resolved_reference_length() const  { return _saved._resolved_reference_length; }

  // Decrease ref counts of symbols that are in the constant pool
  // when the holder class is unloaded
  void unreference_symbols();

  // Deallocate constant pool for RedefineClasses
  void deallocate_contents(ClassLoaderData* loader_data);
  void release_C_heap_structures();

  // JVMTI accesss - GetConstantPool, RetransformClasses, ...
  friend class JvmtiConstantPoolReconstituter;

 private:
  jint cpool_entry_size(jint idx);
  jint hash_entries_to(SymbolHashMap *symmap, SymbolHashMap *classmap);

  // Copy cpool bytes into byte array.
  // Returns:
  //  int > 0, count of the raw cpool bytes that have been copied
  //        0, OutOfMemory error
  //       -1, Internal error
  int  copy_cpool_bytes(int cpool_size,
                        SymbolHashMap* tbl,
                        unsigned char *bytes);

 public:
  // Verify
  void verify_on(outputStream* st);

  // Printing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;
  void print_entry_on(int index, outputStream* st);

  const char* internal_name() const { return "{constant pool}"; }
};

class SymbolHashMapEntry : public CHeapObj<mtSymbol> {
 private:
  SymbolHashMapEntry* _next;   // Next element in the linked list for this bucket
  Symbol*             _symbol; // 1-st part of the mapping: symbol => value
  unsigned int        _hash;   // 32-bit hash for item
  u2                  _value;  // 2-nd part of the mapping: symbol => value

 public:
  unsigned   int hash() const             { return _hash;   }
  void       set_hash(unsigned int hash)  { _hash = hash;   }

  SymbolHashMapEntry* next() const        { return _next;   }
  void set_next(SymbolHashMapEntry* next) { _next = next;   }

  Symbol*    symbol() const               { return _symbol; }
  void       set_symbol(Symbol* sym)      { _symbol = sym;  }

  u2         value() const                {  return _value; }
  void       set_value(u2 value)          { _value = value; }

  SymbolHashMapEntry(unsigned int hash, Symbol* symbol, u2 value)
    : _next(NULL), _symbol(symbol), _hash(hash), _value(value) {}

}; // End SymbolHashMapEntry class


class SymbolHashMapBucket : public CHeapObj<mtSymbol> {

private:
  SymbolHashMapEntry*    _entry;

public:
  SymbolHashMapEntry* entry() const         {  return _entry; }
  void set_entry(SymbolHashMapEntry* entry) { _entry = entry; }
  void clear()                              { _entry = NULL;  }

}; // End SymbolHashMapBucket class


class SymbolHashMap: public CHeapObj<mtSymbol> {

 private:
  // Default number of entries in the table
  enum SymbolHashMap_Constants {
    _Def_HashMap_Size = 256
  };

  int                   _table_size;
  SymbolHashMapBucket*  _buckets;

  void initialize_table(int table_size);

 public:

  int table_size() const        { return _table_size; }

  SymbolHashMap()               { initialize_table(_Def_HashMap_Size); }
  SymbolHashMap(int table_size) { initialize_table(table_size); }

  // hash P(31) from Kernighan & Ritchie
  static unsigned int compute_hash(const char* str, int len) {
    unsigned int hash = 0;
    while (len-- > 0) {
      hash = 31*hash + (unsigned) *str;
      str++;
    }
    return hash;
  }

  SymbolHashMapEntry* bucket(int i) {
    return _buckets[i].entry();
  }

  void add_entry(Symbol* sym, u2 value);
  SymbolHashMapEntry* find_entry(Symbol* sym);

  u2 symbol_to_value(Symbol* sym) {
    SymbolHashMapEntry *entry = find_entry(sym);
    return (entry == NULL) ? 0 : entry->value();
  }

  ~SymbolHashMap();
}; // End SymbolHashMap class

#endif // SHARE_OOPS_CONSTANTPOOL_HPP
