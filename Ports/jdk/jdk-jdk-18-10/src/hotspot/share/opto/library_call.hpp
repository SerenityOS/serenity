/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "ci/ciMethod.hpp"
#include "classfile/javaClasses.hpp"
#include "opto/callGenerator.hpp"
#include "opto/graphKit.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/intrinsicnode.hpp"
#include "opto/movenode.hpp"

class LibraryIntrinsic : public InlineCallGenerator {
  // Extend the set of intrinsics known to the runtime:
 public:
 private:
  bool             _is_virtual;
  bool             _does_virtual_dispatch;
  int8_t           _predicates_count;  // Intrinsic is predicated by several conditions
  int8_t           _last_predicate; // Last generated predicate
  vmIntrinsics::ID _intrinsic_id;

 public:
  LibraryIntrinsic(ciMethod* m, bool is_virtual, int predicates_count, bool does_virtual_dispatch, vmIntrinsics::ID id)
    : InlineCallGenerator(m),
      _is_virtual(is_virtual),
      _does_virtual_dispatch(does_virtual_dispatch),
      _predicates_count((int8_t)predicates_count),
      _last_predicate((int8_t)-1),
      _intrinsic_id(id)
  {
  }
  virtual bool is_intrinsic() const { return true; }
  virtual bool is_virtual()   const { return _is_virtual; }
  virtual bool is_predicated() const { return _predicates_count > 0; }
  virtual int  predicates_count() const { return _predicates_count; }
  virtual bool does_virtual_dispatch()   const { return _does_virtual_dispatch; }
  virtual JVMState* generate(JVMState* jvms);
  virtual Node* generate_predicate(JVMState* jvms, int predicate);
  vmIntrinsics::ID intrinsic_id() const { return _intrinsic_id; }
};


// Local helper class for LibraryIntrinsic:
class LibraryCallKit : public GraphKit {
 private:
  LibraryIntrinsic* _intrinsic;     // the library intrinsic being called
  Node*             _result;        // the result node, if any
  int               _reexecute_sp;  // the stack pointer when bytecode needs to be reexecuted

  const TypeOopPtr* sharpen_unsafe_type(Compile::AliasType* alias_type, const TypePtr *adr_type);

 public:
  LibraryCallKit(JVMState* jvms, LibraryIntrinsic* intrinsic)
    : GraphKit(jvms),
      _intrinsic(intrinsic),
      _result(NULL)
  {
    // Check if this is a root compile.  In that case we don't have a caller.
    if (!jvms->has_method()) {
      _reexecute_sp = sp();
    } else {
      // Find out how many arguments the interpreter needs when deoptimizing
      // and save the stack pointer value so it can used by uncommon_trap.
      // We find the argument count by looking at the declared signature.
      bool ignored_will_link;
      ciSignature* declared_signature = NULL;
      ciMethod* ignored_callee = caller()->get_method_at_bci(bci(), ignored_will_link, &declared_signature);
      const int nargs = declared_signature->arg_size_for_bc(caller()->java_code_at_bci(bci()));
      _reexecute_sp = sp() + nargs;  // "push" arguments back on stack
    }
  }

  virtual LibraryCallKit* is_LibraryCallKit() const { return (LibraryCallKit*)this; }

  ciMethod*         caller()    const    { return jvms()->method(); }
  int               bci()       const    { return jvms()->bci(); }
  LibraryIntrinsic* intrinsic() const    { return _intrinsic; }
  vmIntrinsics::ID  intrinsic_id() const { return _intrinsic->intrinsic_id(); }
  ciMethod*         callee()    const    { return _intrinsic->method(); }

  bool  try_to_inline(int predicate);
  Node* try_to_predicate(int predicate);

  void push_result() {
    // Push the result onto the stack.
    if (!stopped() && result() != NULL) {
      BasicType bt = result()->bottom_type()->basic_type();
      push_node(bt, result());
    }
  }

 private:
  void fatal_unexpected_iid(vmIntrinsics::ID iid) {
    fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
  }

  void  set_result(Node* n) { assert(_result == NULL, "only set once"); _result = n; }
  void  set_result(RegionNode* region, PhiNode* value);
  Node*     result() { return _result; }

  virtual int reexecute_sp() { return _reexecute_sp; }

  // Helper functions to inline natives
  Node* generate_guard(Node* test, RegionNode* region, float true_prob);
  Node* generate_slow_guard(Node* test, RegionNode* region);
  Node* generate_fair_guard(Node* test, RegionNode* region);
  Node* generate_negative_guard(Node* index, RegionNode* region,
                                // resulting CastII of index:
                                Node* *pos_index = NULL);
  Node* generate_limit_guard(Node* offset, Node* subseq_length,
                             Node* array_length,
                             RegionNode* region);
  void  generate_string_range_check(Node* array, Node* offset,
                                    Node* length, bool char_count);
  Node* generate_current_thread(Node* &tls_output);
  Node* load_mirror_from_klass(Node* klass);
  Node* load_klass_from_mirror_common(Node* mirror, bool never_see_null,
                                      RegionNode* region, int null_path,
                                      int offset);
  Node* load_klass_from_mirror(Node* mirror, bool never_see_null,
                               RegionNode* region, int null_path) {
    int offset = java_lang_Class::klass_offset();
    return load_klass_from_mirror_common(mirror, never_see_null,
                                         region, null_path,
                                         offset);
  }
  Node* load_array_klass_from_mirror(Node* mirror, bool never_see_null,
                                     RegionNode* region, int null_path) {
    int offset = java_lang_Class::array_klass_offset();
    return load_klass_from_mirror_common(mirror, never_see_null,
                                         region, null_path,
                                         offset);
  }
  Node* generate_access_flags_guard(Node* kls,
                                    int modifier_mask, int modifier_bits,
                                    RegionNode* region);
  Node* generate_interface_guard(Node* kls, RegionNode* region);
  Node* generate_hidden_class_guard(Node* kls, RegionNode* region);
  Node* generate_array_guard(Node* kls, RegionNode* region) {
    return generate_array_guard_common(kls, region, false, false);
  }
  Node* generate_non_array_guard(Node* kls, RegionNode* region) {
    return generate_array_guard_common(kls, region, false, true);
  }
  Node* generate_objArray_guard(Node* kls, RegionNode* region) {
    return generate_array_guard_common(kls, region, true, false);
  }
  Node* generate_non_objArray_guard(Node* kls, RegionNode* region) {
    return generate_array_guard_common(kls, region, true, true);
  }
  Node* generate_array_guard_common(Node* kls, RegionNode* region,
                                    bool obj_array, bool not_array);
  Node* generate_virtual_guard(Node* obj_klass, RegionNode* slow_region);
  CallJavaNode* generate_method_call(vmIntrinsics::ID method_id,
                                     bool is_virtual = false, bool is_static = false);
  CallJavaNode* generate_method_call_static(vmIntrinsics::ID method_id) {
    return generate_method_call(method_id, false, true);
  }
  CallJavaNode* generate_method_call_virtual(vmIntrinsics::ID method_id) {
    return generate_method_call(method_id, true, false);
  }
  Node* load_field_from_object(Node* fromObj, const char* fieldName, const char* fieldTypeString, DecoratorSet decorators, bool is_static, ciInstanceKlass* fromKls);
  Node* field_address_from_object(Node* fromObj, const char* fieldName, const char* fieldTypeString, bool is_exact, bool is_static, ciInstanceKlass* fromKls);

  Node* make_string_method_node(int opcode, Node* str1_start, Node* cnt1, Node* str2_start, Node* cnt2, StrIntrinsicNode::ArgEnc ae);
  bool inline_string_compareTo(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOf(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOfI(StrIntrinsicNode::ArgEnc ae);
  Node* make_indexOf_node(Node* src_start, Node* src_count, Node* tgt_start, Node* tgt_count,
                          RegionNode* region, Node* phi, StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOfChar(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_equals(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_toBytesU();
  bool inline_string_getCharsU();
  bool inline_string_copy(bool compress);
  bool inline_string_char_access(bool is_store);
  Node* round_double_node(Node* n);
  bool runtime_math(const TypeFunc* call_type, address funcAddr, const char* funcName);
  bool inline_math_native(vmIntrinsics::ID id);
  bool inline_math(vmIntrinsics::ID id);
  bool inline_double_math(vmIntrinsics::ID id);
  bool inline_math_pow();
  template <typename OverflowOp>
  bool inline_math_overflow(Node* arg1, Node* arg2);
  void inline_math_mathExact(Node* math, Node* test);
  bool inline_math_addExactI(bool is_increment);
  bool inline_math_addExactL(bool is_increment);
  bool inline_math_multiplyExactI();
  bool inline_math_multiplyExactL();
  bool inline_math_multiplyHigh();
  bool inline_math_negateExactI();
  bool inline_math_negateExactL();
  bool inline_math_subtractExactI(bool is_decrement);
  bool inline_math_subtractExactL(bool is_decrement);
  bool inline_min_max(vmIntrinsics::ID id);
  bool inline_notify(vmIntrinsics::ID id);
  Node* generate_min_max(vmIntrinsics::ID id, Node* x, Node* y);
  // This returns Type::AnyPtr, RawPtr, or OopPtr.
  int classify_unsafe_addr(Node* &base, Node* &offset, BasicType type);
  Node* make_unsafe_address(Node*& base, Node* offset, BasicType type = T_ILLEGAL, bool can_cast = false);

  typedef enum { Relaxed, Opaque, Volatile, Acquire, Release } AccessKind;
  DecoratorSet mo_decorator_for_access_kind(AccessKind kind);
  bool inline_unsafe_access(bool is_store, BasicType type, AccessKind kind, bool is_unaligned);
  static bool klass_needs_init_guard(Node* kls);
  bool inline_unsafe_allocate();
  bool inline_unsafe_newArray(bool uninitialized);
  bool inline_unsafe_writeback0();
  bool inline_unsafe_writebackSync0(bool is_pre);
  bool inline_unsafe_copyMemory();
  bool inline_native_currentThread();

  bool inline_native_time_funcs(address method, const char* funcName);
#ifdef JFR_HAVE_INTRINSICS
  bool inline_native_classID();
  bool inline_native_getEventWriter();
#endif
  bool inline_native_Class_query(vmIntrinsics::ID id);
  bool inline_native_subtype_check();
  bool inline_native_getLength();
  bool inline_array_copyOf(bool is_copyOfRange);
  bool inline_array_equals(StrIntrinsicNode::ArgEnc ae);
  bool inline_preconditions_checkIndex(BasicType bt);
  void copy_to_clone(Node* obj, Node* alloc_obj, Node* obj_size, bool is_array);
  bool inline_native_clone(bool is_virtual);
  bool inline_native_Reflection_getCallerClass();
  // Helper function for inlining native object hash method
  bool inline_native_hashcode(bool is_virtual, bool is_static);
  bool inline_native_getClass();

  // Helper functions for inlining arraycopy
  bool inline_arraycopy();
  AllocateArrayNode* tightly_coupled_allocation(Node* ptr);
  JVMState* arraycopy_restore_alloc_state(AllocateArrayNode* alloc, int& saved_reexecute_sp);
  void arraycopy_move_allocation_here(AllocateArrayNode* alloc, Node* dest, JVMState* saved_jvms, int saved_reexecute_sp,
                                      uint new_idx);

  typedef enum { LS_get_add, LS_get_set, LS_cmp_swap, LS_cmp_swap_weak, LS_cmp_exchange } LoadStoreKind;
  bool inline_unsafe_load_store(BasicType type,  LoadStoreKind kind, AccessKind access_kind);
  bool inline_unsafe_fence(vmIntrinsics::ID id);
  bool inline_onspinwait();
  bool inline_fp_conversions(vmIntrinsics::ID id);
  bool inline_number_methods(vmIntrinsics::ID id);
  bool inline_reference_get();
  bool inline_reference_refersTo0(bool is_phantom);
  bool inline_Class_cast();
  bool inline_aescrypt_Block(vmIntrinsics::ID id);
  bool inline_cipherBlockChaining_AESCrypt(vmIntrinsics::ID id);
  bool inline_electronicCodeBook_AESCrypt(vmIntrinsics::ID id);
  bool inline_counterMode_AESCrypt(vmIntrinsics::ID id);
  Node* inline_cipherBlockChaining_AESCrypt_predicate(bool decrypting);
  Node* inline_electronicCodeBook_AESCrypt_predicate(bool decrypting);
  Node* inline_counterMode_AESCrypt_predicate();
  Node* get_key_start_from_aescrypt_object(Node* aescrypt_object);
  bool inline_ghash_processBlocks();
  bool inline_base64_encodeBlock();
  bool inline_base64_decodeBlock();
  bool inline_digestBase_implCompress(vmIntrinsics::ID id);
  bool inline_digestBase_implCompressMB(int predicate);
  bool inline_digestBase_implCompressMB(Node* digestBaseObj, ciInstanceKlass* instklass,
                                        const char* state_type, address stubAddr, const char *stubName,
                                        Node* src_start, Node* ofs, Node* limit);
  Node* get_state_from_digest_object(Node *digestBase_object, const char* state_type);
  Node* get_digest_length_from_digest_object(Node *digestBase_object);
  Node* inline_digestBase_implCompressMB_predicate(int predicate);
  bool inline_encodeISOArray();
  bool inline_updateCRC32();
  bool inline_updateBytesCRC32();
  bool inline_updateByteBufferCRC32();
  Node* get_table_from_crc32c_class(ciInstanceKlass *crc32c_class);
  bool inline_updateBytesCRC32C();
  bool inline_updateDirectByteBufferCRC32C();
  bool inline_updateBytesAdler32();
  bool inline_updateByteBufferAdler32();
  bool inline_multiplyToLen();
  bool inline_hasNegatives();
  bool inline_squareToLen();
  bool inline_mulAdd();
  bool inline_montgomeryMultiply();
  bool inline_montgomerySquare();
  bool inline_bigIntegerShift(bool isRightShift);
  bool inline_vectorizedMismatch();
  bool inline_fma(vmIntrinsics::ID id);
  bool inline_character_compare(vmIntrinsics::ID id);
  bool inline_fp_min_max(vmIntrinsics::ID id);

  bool inline_profileBoolean();
  bool inline_isCompileConstant();

  // Vector API support
  bool inline_vector_nary_operation(int n);
  bool inline_vector_broadcast_coerced();
  bool inline_vector_shuffle_to_vector();
  bool inline_vector_shuffle_iota();
  bool inline_vector_mask_operation();
  bool inline_vector_mem_operation(bool is_store);
  bool inline_vector_gather_scatter(bool is_scatter);
  bool inline_vector_reduction();
  bool inline_vector_test();
  bool inline_vector_blend();
  bool inline_vector_rearrange();
  bool inline_vector_compare();
  bool inline_vector_broadcast_int();
  bool inline_vector_convert();
  bool inline_vector_extract();
  bool inline_vector_insert();
  Node* gen_call_to_svml(int vector_api_op_id, BasicType bt, int num_elem, Node* opd1, Node* opd2);

  enum VectorMaskUseType {
    VecMaskUseLoad,
    VecMaskUseStore,
    VecMaskUseAll,
    VecMaskNotUsed
  };

  bool arch_supports_vector(int op, int num_elem, BasicType type, VectorMaskUseType mask_use_type, bool has_scalar_args = false);

  void clear_upper_avx() {
#ifdef X86
    if (UseAVX >= 2) {
      C->set_clear_upper_avx(true);
    }
#endif
  }

  bool inline_getObjectSize();

  bool inline_blackhole();
};

