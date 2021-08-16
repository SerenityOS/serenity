/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_CPCACHE_HPP
#define SHARE_OOPS_CPCACHE_HPP

#include "interpreter/bytecodes.hpp"
#include "memory/allocation.hpp"
#include "oops/array.hpp"
#include "oops/oopHandle.hpp"
#include "runtime/handles.hpp"
#include "utilities/align.hpp"
#include "utilities/constantTag.hpp"
#include "utilities/growableArray.hpp"

// The ConstantPoolCache is not a cache! It is the resolution table that the
// interpreter uses to avoid going into the runtime and a way to access resolved
// values.

// A ConstantPoolCacheEntry describes an individual entry of the constant
// pool cache. There's 2 principal kinds of entries: field entries for in-
// stance & static field access, and method entries for invokes. Some of
// the entry layout is shared and looks as follows:
//
// bit number |31                0|
// bit length |-8--|-8--|---16----|
// --------------------------------
// _indices   [ b2 | b1 |  index  ]  index = constant_pool_index
// _f1        [  entry specific   ]  metadata ptr (method or klass)
// _f2        [  entry specific   ]  vtable or res_ref index, or vfinal method ptr
// _flags     [tos|0|F=1|0|0|0|f|v|0 |0000|field_index] (for field entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |1     |-3-|----16-----]
// _flags     [tos|0|F=0|S|A|I|f|0|vf|indy_rf|000|00000|psize] (for method entries)
// bit length [ 4 |1| 1 |1|1|1|1|1|1 |-4--|--8--|--8--]

// --------------------------------
//
// with:
// index  = original constant pool index
// b1     = bytecode 1
// b2     = bytecode 2
// psize  = parameters size (method entries only)
// field_index = index into field information in holder InstanceKlass
//          The index max is 0xffff (max number of fields in constant pool)
//          and is multiplied by (InstanceKlass::next_offset) when accessing.
// tos    = TosState
// F      = the entry is for a field (or F=0 for a method)
// A      = call site has an appendix argument (loaded from resolved references)
// I      = interface call is forced virtual (must use a vtable index or vfinal)
// f      = field or method is final
// v      = field is volatile
// vf     = virtual but final (method entries only: is_vfinal())
// indy_rf = call site specifier method resolution failed
//
// The flags after TosState have the following interpretation:
// bit 27: 0 for fields, 1 for methods
// f  flag true if field is marked final
// v  flag true if field is volatile (only for fields)
// f2 flag true if f2 contains an oop (e.g., virtual final method)
// fv flag true if invokeinterface used for method in class Object
//
// The flags 31, 30, 29, 28 together build a 4 bit number 0 to 16 with the
// following mapping to the TosState states:
//
// btos: 0
// ztos: 1
// ctos: 2
// stos: 3
// itos: 4
// ltos: 5
// ftos: 6
// dtos: 7
// atos: 8
// vtos: 9
//
// Entry specific: field entries:
// _indices = get (b1 section) and put (b2 section) bytecodes, original constant pool index
// _f1      = field holder (as a java.lang.Class, not a Klass*)
// _f2      = field offset in bytes
// _flags   = field type information, original FieldInfo index in field holder
//            (field_index section)
//
// Entry specific: method entries:
// _indices = invoke code for f1 (b1 section), invoke code for f2 (b2 section),
//            original constant pool index
// _f1      = Method* for non-virtual calls, unused by virtual calls.
//            for interface calls, which are essentially virtual but need a klass,
//            contains Klass* for the corresponding interface.
//            for invokedynamic and invokehandle, f1 contains the adapter method which
//            manages the actual call. The appendix is stored in the ConstantPool
//            resolved_references array.
//            (upcoming metadata changes will move the appendix to a separate array)
// _f2      = vtable/itable index (or final Method*) for virtual calls only,
//            unused by non-virtual.  The is_vfinal flag indicates this is a
//            method pointer for a final method, not an index.
// _flags   = has local signature (MHs and indy),
//            virtual final bit (vfinal),
//            parameter size (psize section)
//
// Note: invokevirtual & invokespecial bytecodes can share the same constant
//       pool entry and thus the same constant pool cache entry. All invoke
//       bytecodes but invokevirtual use only _f1 and the corresponding b1
//       bytecode, while invokevirtual uses only _f2 and the corresponding
//       b2 bytecode.  The value of _flags is shared for both types of entries.
//
// The fields are volatile so that they are stored in the order written in the
// source code.  The _indices field with the bytecode must be written last.

class CallInfo;

class ConstantPoolCacheEntry {
  friend class VMStructs;
  friend class constantPoolCacheKlass;
  friend class ConstantPool;
  friend class InterpreterRuntime;

 private:
  volatile intx     _indices;  // constant pool index & rewrite bytecodes
  Metadata* volatile   _f1;       // entry specific metadata field
  volatile intx        _f2;       // entry specific int/metadata field
  volatile intx     _flags;    // flags


  void set_bytecode_1(Bytecodes::Code code);
  void set_bytecode_2(Bytecodes::Code code);
  void set_f1(Metadata* f1) {
    Metadata* existing_f1 = _f1; // read once
    assert(existing_f1 == NULL || existing_f1 == f1, "illegal field change");
    _f1 = f1;
  }
  void release_set_f1(Metadata* f1);
  void set_f2(intx f2) {
    intx existing_f2 = _f2; // read once
    assert(existing_f2 == 0 || existing_f2 == f2, "illegal field change");
    _f2 = f2;
  }
  void set_f2_as_vfinal_method(Method* f2) {
    assert(is_vfinal(), "flags must be set");
    set_f2((intx)f2);
  }
  int make_flags(TosState state, int option_bits, int field_index_or_method_params);
  void set_flags(intx flags)                     { _flags = flags; }
  void set_field_flags(TosState field_type, int option_bits, int field_index) {
    assert((field_index & field_index_mask) == field_index, "field_index in range");
    set_flags(make_flags(field_type, option_bits | (1 << is_field_entry_shift), field_index));
  }
  void set_method_flags(TosState return_type, int option_bits, int method_params) {
    assert((method_params & parameter_size_mask) == method_params, "method_params in range");
    set_flags(make_flags(return_type, option_bits, method_params));
  }

 public:
  // specific bit definitions for the flags field:
  // (Note: the interpreter must use these definitions to access the CP cache.)
  enum {
    // high order bits are the TosState corresponding to field type or method return type
    tos_state_bits             = 4,
    tos_state_mask             = right_n_bits(tos_state_bits),
    tos_state_shift            = BitsPerInt - tos_state_bits,  // see verify_tos_state_shift below
    // misc. option bits; can be any bit position in [16..27]
    is_field_entry_shift       = 26,  // (F) is it a field or a method?
    has_local_signature_shift  = 25,  // (S) does the call site have a per-site signature (sig-poly methods)?
    has_appendix_shift         = 24,  // (A) does the call site have an appendix argument?
    is_forced_virtual_shift    = 23,  // (I) is the interface reference forced to virtual mode?
    is_final_shift             = 22,  // (f) is the field or method final?
    is_volatile_shift          = 21,  // (v) is the field volatile?
    is_vfinal_shift            = 20,  // (vf) did the call resolve to a final method?
    indy_resolution_failed_shift= 19, // (indy_rf) did call site specifier resolution fail ?
    // low order bits give field index (for FieldInfo) or method parameter size:
    field_index_bits           = 16,
    field_index_mask           = right_n_bits(field_index_bits),
    parameter_size_bits        = 8,  // subset of field_index_mask, range is 0..255
    parameter_size_mask        = right_n_bits(parameter_size_bits),
    option_bits_mask           = ~(((~0u) << tos_state_shift) | (field_index_mask | parameter_size_mask))
  };

  // specific bit definitions for the indices field:
  enum {
    cp_index_bits              = 2*BitsPerByte,
    cp_index_mask              = right_n_bits(cp_index_bits),
    bytecode_1_shift           = cp_index_bits,
    bytecode_1_mask            = right_n_bits(BitsPerByte), // == (u1)0xFF
    bytecode_2_shift           = cp_index_bits + BitsPerByte,
    bytecode_2_mask            = right_n_bits(BitsPerByte)  // == (u1)0xFF
  };


  // Initialization
  void initialize_entry(int original_index);     // initialize primary entry
  void initialize_resolved_reference_index(int ref_index) {
    assert(_f2 == 0, "set once");  // note: ref_index might be zero also
    _f2 = ref_index;
  }

  void set_field(                                // sets entry to resolved field state
    Bytecodes::Code get_code,                    // the bytecode used for reading the field
    Bytecodes::Code put_code,                    // the bytecode used for writing the field
    Klass*          field_holder,                // the object/klass holding the field
    int             orig_field_index,            // the original field index in the field holder
    int             field_offset,                // the field offset in words in the field holder
    TosState        field_type,                  // the (machine) field type
    bool            is_final,                    // the field is final
    bool            is_volatile                  // the field is volatile
  );

 private:
  void set_direct_or_vtable_call(
    Bytecodes::Code invoke_code,                 // the bytecode used for invoking the method
    const methodHandle& method,                  // the method/prototype if any (NULL, otherwise)
    int             vtable_index,                // the vtable index if any, else negative
    bool            sender_is_interface
  );

 public:
  void set_direct_call(                          // sets entry to exact concrete method entry
    Bytecodes::Code invoke_code,                 // the bytecode used for invoking the method
    const methodHandle& method,                  // the method to call
    bool            sender_is_interface
  );

  void set_vtable_call(                          // sets entry to vtable index
    Bytecodes::Code invoke_code,                 // the bytecode used for invoking the method
    const methodHandle& method,                  // resolved method which declares the vtable index
    int             vtable_index                 // the vtable index
  );

  void set_itable_call(
    Bytecodes::Code invoke_code,                 // the bytecode used; must be invokeinterface
    Klass* referenced_klass,                     // the referenced klass in the InterfaceMethodref
    const methodHandle& method,                  // the resolved interface method
    int itable_index                             // index into itable for the method
  );

  void set_method_handle(
    const constantPoolHandle& cpool,             // holding constant pool (required for locking)
    const CallInfo &call_info                    // Call link information
  );

  void set_dynamic_call(
    const constantPoolHandle& cpool,             // holding constant pool (required for locking)
    const CallInfo &call_info                    // Call link information
  );

  // Common code for invokedynamic and MH invocations.

  // The "appendix" is an optional call-site-specific parameter which is
  // pushed by the JVM at the end of the argument list.  This argument may
  // be a MethodType for the MH.invokes and a CallSite for an invokedynamic
  // instruction.  However, its exact type and use depends on the Java upcall,
  // which simply returns a compiled LambdaForm along with any reference
  // that LambdaForm needs to complete the call.  If the upcall returns a
  // null appendix, the argument is not passed at all.
  //
  // The appendix is *not* represented in the signature of the symbolic
  // reference for the call site, but (if present) it *is* represented in
  // the Method* bound to the site.  This means that static and dynamic
  // resolution logic needs to make slightly different assessments about the
  // number and types of arguments.
  void set_method_handle_common(
    const constantPoolHandle& cpool,                    // holding constant pool (required for locking)
    Bytecodes::Code invoke_code,                 // _invokehandle or _invokedynamic
    const CallInfo &call_info                    // Call link information
  );

  // Return TRUE if resolution failed and this thread got to record the failure
  // status.  Return FALSE if another thread succeeded or failed in resolving
  // the method and recorded the success or failure before this thread had a
  // chance to record its failure.
  bool save_and_throw_indy_exc(const constantPoolHandle& cpool, int cpool_index,
                               int index, constantTag tag, TRAPS);

  // invokedynamic and invokehandle call sites have an "appendix" item in the
  // resolved references array.
  Method*      method_if_resolved(const constantPoolHandle& cpool);
  oop        appendix_if_resolved(const constantPoolHandle& cpool);

  void set_parameter_size(int value);

  // Which bytecode number (1 or 2) in the index field is valid for this bytecode?
  // Returns -1 if neither is valid.
  static int bytecode_number(Bytecodes::Code code) {
    switch (code) {
      case Bytecodes::_getstatic       :    // fall through
      case Bytecodes::_getfield        :    // fall through
      case Bytecodes::_invokespecial   :    // fall through
      case Bytecodes::_invokestatic    :    // fall through
      case Bytecodes::_invokehandle    :    // fall through
      case Bytecodes::_invokedynamic   :    // fall through
      case Bytecodes::_invokeinterface : return 1;
      case Bytecodes::_putstatic       :    // fall through
      case Bytecodes::_putfield        :    // fall through
      case Bytecodes::_invokevirtual   : return 2;
      default                          : break;
    }
    return -1;
  }

  // Has this bytecode been resolved? Only valid for invokes and get/put field/static.
  bool is_resolved(Bytecodes::Code code) const;

  // Accessors
  int indices() const                            { return _indices; }
  int indices_ord() const;
  int constant_pool_index() const                { return (indices() & cp_index_mask); }
  Bytecodes::Code bytecode_1() const;
  Bytecodes::Code bytecode_2() const;
  Metadata* f1_ord() const;
  Method*   f1_as_method() const;
  Klass*    f1_as_klass() const;
  // Use the accessor f1() to acquire _f1's value. This is needed for
  // example in BytecodeInterpreter::run(), where is_f1_null() is
  // called to check if an invokedynamic call is resolved. This load
  // of _f1 must be ordered with the loads performed by
  // cache->main_entry_index().
  bool      is_f1_null() const;  // classifies a CPC entry as unbound
  int       f2_as_index() const                  { assert(!is_vfinal(), ""); return (int) _f2; }
  Method*   f2_as_vfinal_method() const          { assert(is_vfinal(), ""); return (Method*)_f2; }
  Method*   f2_as_interface_method() const;
  intx flags_ord() const;
  int  field_index() const                       { assert(is_field_entry(),  ""); return (_flags & field_index_mask); }
  int  parameter_size() const                    { assert(is_method_entry(), ""); return (_flags & parameter_size_mask); }
  bool is_volatile() const                       { return (_flags & (1 << is_volatile_shift))       != 0; }
  bool is_final() const                          { return (_flags & (1 << is_final_shift))          != 0; }
  bool is_forced_virtual() const                 { return (_flags & (1 << is_forced_virtual_shift)) != 0; }
  bool is_vfinal() const                         { return (_flags & (1 << is_vfinal_shift))         != 0; }
  bool indy_resolution_failed() const;
  bool has_appendix() const;
  bool has_local_signature() const;
  bool is_method_entry() const                   { return (_flags & (1 << is_field_entry_shift))    == 0; }
  bool is_field_entry() const                    { return (_flags & (1 << is_field_entry_shift))    != 0; }
  bool is_long() const                           { return flag_state() == ltos; }
  bool is_double() const                         { return flag_state() == dtos; }
  TosState flag_state() const                    { assert((uint)number_of_states <= (uint)tos_state_mask+1, "");
                                                   return (TosState)((_flags >> tos_state_shift) & tos_state_mask); }
  void set_indy_resolution_failed();

  // Code generation support
  static WordSize size()                         {
    return in_WordSize(align_up((int)sizeof(ConstantPoolCacheEntry), wordSize) / wordSize);
  }
  static ByteSize size_in_bytes()                { return in_ByteSize(sizeof(ConstantPoolCacheEntry)); }
  static ByteSize indices_offset()               { return byte_offset_of(ConstantPoolCacheEntry, _indices); }
  static ByteSize f1_offset()                    { return byte_offset_of(ConstantPoolCacheEntry, _f1); }
  static ByteSize f2_offset()                    { return byte_offset_of(ConstantPoolCacheEntry, _f2); }
  static ByteSize flags_offset()                 { return byte_offset_of(ConstantPoolCacheEntry, _flags); }

#if INCLUDE_JVMTI
  // RedefineClasses() API support:
  // If this ConstantPoolCacheEntry refers to old_method then update it
  // to refer to new_method.
  // trace_name_printed is set to true if the current call has
  // printed the klass name so that other routines in the adjust_*
  // group don't print the klass name.
  void adjust_method_entry(Method* old_method, Method* new_method,
         bool* trace_name_printed);
  bool check_no_old_or_obsolete_entries();
  Method* get_interesting_method_entry();
#endif // INCLUDE_JVMTI

  // Debugging & Printing
  void print (outputStream* st, int index) const;
  void verify(outputStream* st) const;

  static void verify_tos_state_shift() {
    // When shifting flags as a 32-bit int, make sure we don't need an extra mask for tos_state:
    assert((((u4)-1 >> tos_state_shift) & ~tos_state_mask) == 0, "no need for tos_state mask");
  }

  void verify_just_initialized(bool f2_used);
  void reinitialize(bool f2_used);
};


// A constant pool cache is a runtime data structure set aside to a constant pool. The cache
// holds interpreter runtime information for all field access and invoke bytecodes. The cache
// is created and initialized before a class is actively used (i.e., initialized), the indivi-
// dual cache entries are filled at resolution (i.e., "link") time (see also: rewriter.*).

class ConstantPoolCache: public MetaspaceObj {
  friend class VMStructs;
  friend class MetadataFactory;
 private:
  // If you add a new field that points to any metaspace object, you
  // must add this field to ConstantPoolCache::metaspace_pointers_do().
  int             _length;
  ConstantPool*   _constant_pool;          // the corresponding constant pool

  // The following fields need to be modified at runtime, so they cannot be
  // stored in the ConstantPool, which is read-only.
  // Array of resolved objects from the constant pool and map from resolved
  // object index to original constant pool index
  OopHandle            _resolved_references;
  Array<u2>*           _reference_map;
  // The narrowOop pointer to the archived resolved_references. Set at CDS dump
  // time when caching java heap object is supported.
  CDS_JAVA_HEAP_ONLY(int _archived_references_index;)

  // Sizing
  debug_only(friend class ClassVerifier;)

  // Constructor
  ConstantPoolCache(int length,
                    const intStack& inverse_index_map,
                    const intStack& invokedynamic_inverse_index_map,
                    const intStack& invokedynamic_references_map);

  // Initialization
  void initialize(const intArray& inverse_index_map,
                  const intArray& invokedynamic_inverse_index_map,
                  const intArray& invokedynamic_references_map);
 public:
  static ConstantPoolCache* allocate(ClassLoaderData* loader_data,
                                     const intStack& cp_cache_map,
                                     const intStack& invokedynamic_cp_cache_map,
                                     const intStack& invokedynamic_references_map, TRAPS);

  int length() const                      { return _length; }
  void metaspace_pointers_do(MetaspaceClosure* it);
  MetaspaceObj::Type type() const         { return ConstantPoolCacheType; }

  oop  archived_references() NOT_CDS_JAVA_HEAP_RETURN_(NULL);
  void set_archived_references(oop o) NOT_CDS_JAVA_HEAP_RETURN;
  void clear_archived_references() NOT_CDS_JAVA_HEAP_RETURN;

  inline oop resolved_references();
  void set_resolved_references(OopHandle s) { _resolved_references = s; }
  Array<u2>* reference_map() const        { return _reference_map; }
  void set_reference_map(Array<u2>* o)    { _reference_map = o; }

  // Assembly code support
  static int resolved_references_offset_in_bytes() { return offset_of(ConstantPoolCache, _resolved_references); }

  // CDS support
  void remove_unshareable_info();
  void verify_just_initialized();
 private:
  void walk_entries_for_initialization(bool check_only);
  void set_length(int length)                    { _length = length; }

  static int header_size()                       { return sizeof(ConstantPoolCache) / wordSize; }
  static int size(int length)                    { return align_metadata_size(header_size() + length * in_words(ConstantPoolCacheEntry::size())); }
 public:
  int size() const                               { return size(length()); }
 private:

  // Helpers
  ConstantPool**        constant_pool_addr()     { return &_constant_pool; }
  ConstantPoolCacheEntry* base() const           { return (ConstantPoolCacheEntry*)((address)this + in_bytes(base_offset())); }

  friend class constantPoolCacheKlass;
  friend class ConstantPoolCacheEntry;

 public:
  // Accessors
  void set_constant_pool(ConstantPool* pool)   { _constant_pool = pool; }
  ConstantPool* constant_pool() const          { return _constant_pool; }
  // Fetches the entry at the given index.
  // In either case the index must not be encoded or byte-swapped in any way.
  ConstantPoolCacheEntry* entry_at(int i) const {
    assert(0 <= i && i < length(), "index out of bounds");
    return base() + i;
  }

  // Code generation
  static ByteSize base_offset()                  { return in_ByteSize(sizeof(ConstantPoolCache)); }
  static ByteSize entry_offset(int raw_index) {
    int index = raw_index;
    return (base_offset() + ConstantPoolCacheEntry::size_in_bytes() * index);
  }

#if INCLUDE_JVMTI
  // RedefineClasses() API support:
  // If any entry of this ConstantPoolCache points to any of
  // old_methods, replace it with the corresponding new_method.
  // trace_name_printed is set to true if the current call has
  // printed the klass name so that other routines in the adjust_*
  // group don't print the klass name.
  void adjust_method_entries(bool* trace_name_printed);
  bool check_no_old_or_obsolete_entries();
  void dump_cache();
#endif // INCLUDE_JVMTI

  // RedefineClasses support
  DEBUG_ONLY(bool on_stack() { return false; })
  void deallocate_contents(ClassLoaderData* data);
  bool is_klass() const { return false; }

  // Printing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;

  const char* internal_name() const { return "{constant pool cache}"; }

  // Verify
  void verify_on(outputStream* st);
};

#endif // SHARE_OOPS_CPCACHE_HPP
