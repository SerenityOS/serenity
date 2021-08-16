/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_ARRAYCOPYNODE_HPP
#define SHARE_OPTO_ARRAYCOPYNODE_HPP

#include "gc/shared/c2/barrierSetC2.hpp"
#include "opto/callnode.hpp"

class GraphKit;

class ArrayCopyNode : public CallNode {
private:

  // What kind of arraycopy variant is this?
  enum {
    None,            // not set yet
    ArrayCopy,       // System.arraycopy()
    CloneInst,       // A clone of instances
    CloneArray,      // A clone of arrays that don't require a barrier
                     // - depends on GC - some need to treat oop arrays separately
    CloneOopArray,   // An oop array clone that requires GC barriers
    CopyOf,          // Arrays.copyOf()
    CopyOfRange      // Arrays.copyOfRange()
  } _kind;

#ifndef PRODUCT
  static const char* _kind_names[CopyOfRange+1];
#endif
  // Is the alloc obtained with
  // AllocateArrayNode::Ideal_array_allocation() tightly coupled
  // (arraycopy follows immediately the allocation)?
  // We cache the result of LibraryCallKit::tightly_coupled_allocation
  // here because it's much easier to find whether there's a tightly
  // couple allocation at parse time than at macro expansion time. At
  // macro expansion time, for every use of the allocation node we
  // would need to figure out whether it happens after the arraycopy (and
  // can be ignored) or between the allocation and the arraycopy. At
  // parse time, it's straightforward because whatever happens after
  // the arraycopy is not parsed yet so doesn't exist when
  // LibraryCallKit::tightly_coupled_allocation() is called.
  bool _alloc_tightly_coupled;
  bool _has_negative_length_guard;

  bool _arguments_validated;

  static const TypeFunc* arraycopy_type() {
    const Type** fields = TypeTuple::fields(ParmLimit - TypeFunc::Parms);
    fields[Src]       = TypeInstPtr::BOTTOM;
    fields[SrcPos]    = TypeInt::INT;
    fields[Dest]      = TypeInstPtr::BOTTOM;
    fields[DestPos]   = TypeInt::INT;
    fields[Length]    = TypeInt::INT;
    fields[SrcLen]    = TypeInt::INT;
    fields[DestLen]   = TypeInt::INT;
    fields[SrcKlass]  = TypeKlassPtr::BOTTOM;
    fields[DestKlass] = TypeKlassPtr::BOTTOM;
    const TypeTuple *domain = TypeTuple::make(ParmLimit, fields);

    // create result type (range)
    fields = TypeTuple::fields(0);

    const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+0, fields);

    return TypeFunc::make(domain, range);
  }

  ArrayCopyNode(Compile* C, bool alloc_tightly_coupled, bool has_negative_length_guard);

  intptr_t get_length_if_constant(PhaseGVN *phase) const;
  int get_count(PhaseGVN *phase) const;
  static const TypePtr* get_address_type(PhaseGVN* phase, const TypePtr* atp, Node* n);

  Node* try_clone_instance(PhaseGVN *phase, bool can_reshape, int count);
  bool prepare_array_copy(PhaseGVN *phase, bool can_reshape,
                          Node*& adr_src, Node*& base_src, Node*& adr_dest, Node*& base_dest,
                          BasicType& copy_type, const Type*& value_type, bool& disjoint_bases);
  void array_copy_test_overlap(PhaseGVN *phase, bool can_reshape,
                               bool disjoint_bases, int count,
                               Node*& forward_ctl, Node*& backward_ctl);
  Node* array_copy_forward(PhaseGVN *phase, bool can_reshape, Node*& ctl,
                           Node* mem,
                           const TypePtr* atp_src, const TypePtr* atp_dest,
                           Node* adr_src, Node* base_src, Node* adr_dest, Node* base_dest,
                           BasicType copy_type, const Type* value_type, int count);
  Node* array_copy_backward(PhaseGVN *phase, bool can_reshape, Node*& ctl,
                            Node* mem,
                            const TypePtr* atp_src, const TypePtr* atp_dest,
                            Node* adr_src, Node* base_src, Node* adr_dest, Node* base_dest,
                            BasicType copy_type, const Type* value_type, int count);
  bool finish_transform(PhaseGVN *phase, bool can_reshape,
                        Node* ctl, Node *mem);
  static bool may_modify_helper(const TypeOopPtr *t_oop, Node* n, PhaseTransform *phase, CallNode*& call);
public:
  static Node* load(BarrierSetC2* bs, PhaseGVN *phase, Node*& ctl, MergeMemNode* mem, Node* addr, const TypePtr* adr_type, const Type *type, BasicType bt);
private:
  void store(BarrierSetC2* bs, PhaseGVN *phase, Node*& ctl, MergeMemNode* mem, Node* addr, const TypePtr* adr_type, Node* val, const Type *type, BasicType bt);

public:

  enum {
    Src   = TypeFunc::Parms,
    SrcPos,
    Dest,
    DestPos,
    Length,
    SrcLen,
    DestLen,
    SrcKlass,
    DestKlass,
    ParmLimit
  };

  // Results from escape analysis for non escaping inputs
  const TypeOopPtr* _src_type;
  const TypeOopPtr* _dest_type;

  static ArrayCopyNode* make(GraphKit* kit, bool may_throw,
                             Node* src, Node* src_offset,
                             Node* dest,  Node* dest_offset,
                             Node* length,
                             bool alloc_tightly_coupled,
                             bool has_negative_length_guard,
                             Node* src_klass = NULL, Node* dest_klass = NULL,
                             Node* src_length = NULL, Node* dest_length = NULL);

  void connect_outputs(GraphKit* kit, bool deoptimize_on_exception = false);

  bool is_arraycopy()             const  { assert(_kind != None, "should bet set"); return _kind == ArrayCopy; }
  bool is_arraycopy_validated()   const  { assert(_kind != None, "should bet set"); return _kind == ArrayCopy && _arguments_validated; }
  bool is_clone_inst()            const  { assert(_kind != None, "should bet set"); return _kind == CloneInst; }
  // is_clone_array - true for all arrays when using GCs that has no barriers
  bool is_clone_array()           const  { assert(_kind != None, "should bet set"); return _kind == CloneArray; }
  // is_clone_oop_array is used when oop arrays need GC barriers
  bool is_clone_oop_array()       const  { assert(_kind != None, "should bet set"); return _kind == CloneOopArray; }
  // is_clonebasic - is true for any type of clone that doesn't need a writebarrier.
  bool is_clonebasic()            const  { assert(_kind != None, "should bet set"); return _kind == CloneInst || _kind == CloneArray; }
  bool is_copyof()                const  { assert(_kind != None, "should bet set"); return _kind == CopyOf; }
  bool is_copyof_validated()      const  { assert(_kind != None, "should bet set"); return _kind == CopyOf && _arguments_validated; }
  bool is_copyofrange()           const  { assert(_kind != None, "should bet set"); return _kind == CopyOfRange; }
  bool is_copyofrange_validated() const  { assert(_kind != None, "should bet set"); return _kind == CopyOfRange && _arguments_validated; }

  void set_arraycopy(bool validated)   { assert(_kind == None, "shouldn't bet set yet"); _kind = ArrayCopy; _arguments_validated = validated; }
  void set_clone_inst()                { assert(_kind == None, "shouldn't bet set yet"); _kind = CloneInst; }
  void set_clone_array()               { assert(_kind == None, "shouldn't bet set yet"); _kind = CloneArray; }
  void set_clone_oop_array()           { assert(_kind == None, "shouldn't bet set yet"); _kind = CloneOopArray; }
  void set_copyof(bool validated)      { assert(_kind == None, "shouldn't bet set yet"); _kind = CopyOf; _arguments_validated = validated; }
  void set_copyofrange(bool validated) { assert(_kind == None, "shouldn't bet set yet"); _kind = CopyOfRange; _arguments_validated = validated; }

  virtual int Opcode() const;
  virtual uint size_of() const; // Size is bigger
  virtual bool guaranteed_safepoint()  { return false; }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);

  virtual bool may_modify(const TypeOopPtr *t_oop, PhaseTransform *phase);

  bool is_alloc_tightly_coupled() const { return _alloc_tightly_coupled; }

  bool has_negative_length_guard() const { return _has_negative_length_guard; }

  static bool may_modify(const TypeOopPtr *t_oop, MemBarNode* mb, PhaseTransform *phase, ArrayCopyNode*& ac);

  static int get_partial_inline_vector_lane_count(BasicType type, int const_len);

  bool modifies(intptr_t offset_lo, intptr_t offset_hi, PhaseTransform* phase, bool must_modify) const;

#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
  virtual void dump_compact_spec(outputStream* st) const;
#endif
};
#endif // SHARE_OPTO_ARRAYCOPYNODE_HPP
