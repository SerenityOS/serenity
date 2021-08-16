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
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "gc/shared/c2/cardTableBarrierSetC2.hpp"
#include "gc/shared/gc_globals.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/graphKit.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

ArrayCopyNode::ArrayCopyNode(Compile* C, bool alloc_tightly_coupled, bool has_negative_length_guard)
  : CallNode(arraycopy_type(), NULL, TypePtr::BOTTOM),
    _kind(None),
    _alloc_tightly_coupled(alloc_tightly_coupled),
    _has_negative_length_guard(has_negative_length_guard),
    _arguments_validated(false),
    _src_type(TypeOopPtr::BOTTOM),
    _dest_type(TypeOopPtr::BOTTOM) {
  init_class_id(Class_ArrayCopy);
  init_flags(Flag_is_macro);
  C->add_macro_node(this);
}

uint ArrayCopyNode::size_of() const { return sizeof(*this); }

ArrayCopyNode* ArrayCopyNode::make(GraphKit* kit, bool may_throw,
                                   Node* src, Node* src_offset,
                                   Node* dest, Node* dest_offset,
                                   Node* length,
                                   bool alloc_tightly_coupled,
                                   bool has_negative_length_guard,
                                   Node* src_klass, Node* dest_klass,
                                   Node* src_length, Node* dest_length) {

  ArrayCopyNode* ac = new ArrayCopyNode(kit->C, alloc_tightly_coupled, has_negative_length_guard);
  kit->set_predefined_input_for_runtime_call(ac);

  ac->init_req(ArrayCopyNode::Src, src);
  ac->init_req(ArrayCopyNode::SrcPos, src_offset);
  ac->init_req(ArrayCopyNode::Dest, dest);
  ac->init_req(ArrayCopyNode::DestPos, dest_offset);
  ac->init_req(ArrayCopyNode::Length, length);
  ac->init_req(ArrayCopyNode::SrcLen, src_length);
  ac->init_req(ArrayCopyNode::DestLen, dest_length);
  ac->init_req(ArrayCopyNode::SrcKlass, src_klass);
  ac->init_req(ArrayCopyNode::DestKlass, dest_klass);

  if (may_throw) {
    ac->set_req(TypeFunc::I_O , kit->i_o());
    kit->add_safepoint_edges(ac, false);
  }

  return ac;
}

void ArrayCopyNode::connect_outputs(GraphKit* kit, bool deoptimize_on_exception) {
  kit->set_all_memory_call(this, true);
  kit->set_control(kit->gvn().transform(new ProjNode(this,TypeFunc::Control)));
  kit->set_i_o(kit->gvn().transform(new ProjNode(this, TypeFunc::I_O)));
  kit->make_slow_call_ex(this, kit->env()->Throwable_klass(), true, deoptimize_on_exception);
  kit->set_all_memory_call(this);
}

#ifndef PRODUCT
const char* ArrayCopyNode::_kind_names[] = {"arraycopy", "arraycopy, validated arguments", "clone", "oop array clone", "CopyOf", "CopyOfRange"};

void ArrayCopyNode::dump_spec(outputStream *st) const {
  CallNode::dump_spec(st);
  st->print(" (%s%s)", _kind_names[_kind], _alloc_tightly_coupled ? ", tightly coupled allocation" : "");
}

void ArrayCopyNode::dump_compact_spec(outputStream* st) const {
  st->print("%s%s", _kind_names[_kind], _alloc_tightly_coupled ? ",tight" : "");
}
#endif

intptr_t ArrayCopyNode::get_length_if_constant(PhaseGVN *phase) const {
  // check that length is constant
  Node* length = in(ArrayCopyNode::Length);
  const Type* length_type = phase->type(length);

  if (length_type == Type::TOP) {
    return -1;
  }

  assert(is_clonebasic() || is_arraycopy() || is_copyof() || is_copyofrange(), "unexpected array copy type");

  return is_clonebasic() ? length->find_intptr_t_con(-1) : length->find_int_con(-1);
}

int ArrayCopyNode::get_count(PhaseGVN *phase) const {
  Node* src = in(ArrayCopyNode::Src);
  const Type* src_type = phase->type(src);

  if (is_clonebasic()) {
    if (src_type->isa_instptr()) {
      const TypeInstPtr* inst_src = src_type->is_instptr();
      ciInstanceKlass* ik = inst_src->klass()->as_instance_klass();
      // ciInstanceKlass::nof_nonstatic_fields() doesn't take injected
      // fields into account. They are rare anyway so easier to simply
      // skip instances with injected fields.
      if ((!inst_src->klass_is_exact() && (ik->is_interface() || ik->has_subklass())) || ik->has_injected_fields()) {
        return -1;
      }
      int nb_fields = ik->nof_nonstatic_fields();
      return nb_fields;
    } else {
      const TypeAryPtr* ary_src = src_type->isa_aryptr();
      assert (ary_src != NULL, "not an array or instance?");
      // clone passes a length as a rounded number of longs. If we're
      // cloning an array we'll do it element by element. If the
      // length input to ArrayCopyNode is constant, length of input
      // array must be too.

      assert((get_length_if_constant(phase) == -1) != ary_src->size()->is_con() ||
             phase->is_IterGVN() || phase->C->inlining_incrementally() || StressReflectiveCode, "inconsistent");

      if (ary_src->size()->is_con()) {
        return ary_src->size()->get_con();
      }
      return -1;
    }
  }

  return get_length_if_constant(phase);
}

Node* ArrayCopyNode::load(BarrierSetC2* bs, PhaseGVN *phase, Node*& ctl, MergeMemNode* mem, Node* adr, const TypePtr* adr_type, const Type *type, BasicType bt) {
  DecoratorSet decorators = C2_READ_ACCESS | C2_CONTROL_DEPENDENT_LOAD | IN_HEAP | C2_ARRAY_COPY;
  C2AccessValuePtr addr(adr, adr_type);
  C2OptAccess access(*phase, ctl, mem, decorators, bt, adr->in(AddPNode::Base), addr);
  Node* res = bs->load_at(access, type);
  ctl = access.ctl();
  return res;
}

void ArrayCopyNode::store(BarrierSetC2* bs, PhaseGVN *phase, Node*& ctl, MergeMemNode* mem, Node* adr, const TypePtr* adr_type, Node* val, const Type *type, BasicType bt) {
  DecoratorSet decorators = C2_WRITE_ACCESS | IN_HEAP | C2_ARRAY_COPY;
  if (is_alloc_tightly_coupled()) {
    decorators |= C2_TIGHTLY_COUPLED_ALLOC;
  }
  C2AccessValuePtr addr(adr, adr_type);
  C2AccessValue value(val, type);
  C2OptAccess access(*phase, ctl, mem, decorators, bt, adr->in(AddPNode::Base), addr);
  bs->store_at(access, value);
  ctl = access.ctl();
}


Node* ArrayCopyNode::try_clone_instance(PhaseGVN *phase, bool can_reshape, int count) {
  if (!is_clonebasic()) {
    return NULL;
  }

  Node* base_src = in(ArrayCopyNode::Src);
  Node* base_dest = in(ArrayCopyNode::Dest);
  Node* ctl = in(TypeFunc::Control);
  Node* in_mem = in(TypeFunc::Memory);

  const Type* src_type = phase->type(base_src);
  const TypeInstPtr* inst_src = src_type->isa_instptr();
  if (inst_src == NULL) {
    return NULL;
  }

  MergeMemNode* mem = phase->transform(MergeMemNode::make(in_mem))->as_MergeMem();
  PhaseIterGVN* igvn = phase->is_IterGVN();
  if (igvn != NULL) {
    igvn->_worklist.push(mem);
  }

  if (!inst_src->klass_is_exact()) {
    ciInstanceKlass* ik = inst_src->klass()->as_instance_klass();
    assert(!ik->is_interface(), "inconsistent klass hierarchy");
    if (ik->has_subklass()) {
      // Concurrent class loading.
      // Fail fast and return NodeSentinel to indicate that the transform failed.
      return NodeSentinel;
    } else {
      phase->C->dependencies()->assert_leaf_type(ik);
    }
  }

  ciInstanceKlass* ik = inst_src->klass()->as_instance_klass();
  assert(ik->nof_nonstatic_fields() <= ArrayCopyLoadStoreMaxElem, "too many fields");

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  for (int i = 0; i < count; i++) {
    ciField* field = ik->nonstatic_field_at(i);
    const TypePtr* adr_type = phase->C->alias_type(field)->adr_type();
    Node* off = phase->MakeConX(field->offset());
    Node* next_src = phase->transform(new AddPNode(base_src,base_src,off));
    Node* next_dest = phase->transform(new AddPNode(base_dest,base_dest,off));
    BasicType bt = field->layout_type();

    const Type *type;
    if (bt == T_OBJECT) {
      if (!field->type()->is_loaded()) {
        type = TypeInstPtr::BOTTOM;
      } else {
        ciType* field_klass = field->type();
        type = TypeOopPtr::make_from_klass(field_klass->as_klass());
      }
    } else {
      type = Type::get_const_basic_type(bt);
    }

    Node* v = load(bs, phase, ctl, mem, next_src, adr_type, type, bt);
    store(bs, phase, ctl, mem, next_dest, adr_type, v, type, bt);
  }

  if (!finish_transform(phase, can_reshape, ctl, mem)) {
    // Return NodeSentinel to indicate that the transform failed
    return NodeSentinel;
  }

  return mem;
}

bool ArrayCopyNode::prepare_array_copy(PhaseGVN *phase, bool can_reshape,
                                       Node*& adr_src,
                                       Node*& base_src,
                                       Node*& adr_dest,
                                       Node*& base_dest,
                                       BasicType& copy_type,
                                       const Type*& value_type,
                                       bool& disjoint_bases) {
  base_src = in(ArrayCopyNode::Src);
  base_dest = in(ArrayCopyNode::Dest);
  const Type* src_type = phase->type(base_src);
  const TypeAryPtr* ary_src = src_type->isa_aryptr();

  Node* src_offset = in(ArrayCopyNode::SrcPos);
  Node* dest_offset = in(ArrayCopyNode::DestPos);

  if (is_arraycopy() || is_copyofrange() || is_copyof()) {
    const Type* dest_type = phase->type(base_dest);
    const TypeAryPtr* ary_dest = dest_type->isa_aryptr();

    // newly allocated object is guaranteed to not overlap with source object
    disjoint_bases = is_alloc_tightly_coupled();

    if (ary_src  == NULL || ary_src->klass()  == NULL ||
        ary_dest == NULL || ary_dest->klass() == NULL) {
      // We don't know if arguments are arrays
      return false;
    }

    BasicType src_elem  = ary_src->klass()->as_array_klass()->element_type()->basic_type();
    BasicType dest_elem = ary_dest->klass()->as_array_klass()->element_type()->basic_type();
    if (is_reference_type(src_elem))   src_elem  = T_OBJECT;
    if (is_reference_type(dest_elem))  dest_elem = T_OBJECT;

    if (src_elem != dest_elem || dest_elem == T_VOID) {
      // We don't know if arguments are arrays of the same type
      return false;
    }

    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    if (bs->array_copy_requires_gc_barriers(is_alloc_tightly_coupled(), dest_elem, false, false, BarrierSetC2::Optimization)) {
      // It's an object array copy but we can't emit the card marking
      // that is needed
      return false;
    }

    value_type = ary_src->elem();

    uint shift  = exact_log2(type2aelembytes(dest_elem));
    uint header = arrayOopDesc::base_offset_in_bytes(dest_elem);

    src_offset = Compile::conv_I2X_index(phase, src_offset, ary_src->size());
    dest_offset = Compile::conv_I2X_index(phase, dest_offset, ary_dest->size());
    if (src_offset->is_top() || dest_offset->is_top()) {
      // Offset is out of bounds (the ArrayCopyNode will be removed)
      return false;
    }

    Node* src_scale  = phase->transform(new LShiftXNode(src_offset, phase->intcon(shift)));
    Node* dest_scale = phase->transform(new LShiftXNode(dest_offset, phase->intcon(shift)));

    adr_src          = phase->transform(new AddPNode(base_src, base_src, src_scale));
    adr_dest         = phase->transform(new AddPNode(base_dest, base_dest, dest_scale));

    adr_src          = phase->transform(new AddPNode(base_src, adr_src, phase->MakeConX(header)));
    adr_dest         = phase->transform(new AddPNode(base_dest, adr_dest, phase->MakeConX(header)));

    copy_type = dest_elem;
  } else {
    assert(ary_src != NULL, "should be a clone");
    assert(is_clonebasic(), "should be");

    disjoint_bases = true;

    adr_src  = phase->transform(new AddPNode(base_src, base_src, src_offset));
    adr_dest = phase->transform(new AddPNode(base_dest, base_dest, dest_offset));

    BasicType elem = ary_src->klass()->as_array_klass()->element_type()->basic_type();
    if (is_reference_type(elem)) {
      elem = T_OBJECT;
    }

    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    if (bs->array_copy_requires_gc_barriers(true, elem, true, is_clone_inst(), BarrierSetC2::Optimization)) {
      return false;
    }

    // The address is offseted to an aligned address where a raw copy would start.
    // If the clone copy is decomposed into load-stores - the address is adjusted to
    // point at where the array starts.
    const Type* toff = phase->type(src_offset);
    int offset = toff->isa_long() ? (int) toff->is_long()->get_con() : (int) toff->is_int()->get_con();
    int diff = arrayOopDesc::base_offset_in_bytes(elem) - offset;
    assert(diff >= 0, "clone should not start after 1st array element");
    if (diff > 0) {
      adr_src = phase->transform(new AddPNode(base_src, adr_src, phase->MakeConX(diff)));
      adr_dest = phase->transform(new AddPNode(base_dest, adr_dest, phase->MakeConX(diff)));
    }
    copy_type = elem;
    value_type = ary_src->elem();
  }
  return true;
}

const TypePtr* ArrayCopyNode::get_address_type(PhaseGVN* phase, const TypePtr* atp, Node* n) {
  if (atp == TypeOopPtr::BOTTOM) {
    atp = phase->type(n)->isa_ptr();
  }
  // adjust atp to be the correct array element address type
  return atp->add_offset(Type::OffsetBot);
}

void ArrayCopyNode::array_copy_test_overlap(PhaseGVN *phase, bool can_reshape, bool disjoint_bases, int count, Node*& forward_ctl, Node*& backward_ctl) {
  Node* ctl = in(TypeFunc::Control);
  if (!disjoint_bases && count > 1) {
    Node* src_offset = in(ArrayCopyNode::SrcPos);
    Node* dest_offset = in(ArrayCopyNode::DestPos);
    assert(src_offset != NULL && dest_offset != NULL, "should be");
    Node* cmp = phase->transform(new CmpINode(src_offset, dest_offset));
    Node *bol = phase->transform(new BoolNode(cmp, BoolTest::lt));
    IfNode *iff = new IfNode(ctl, bol, PROB_FAIR, COUNT_UNKNOWN);

    phase->transform(iff);

    forward_ctl = phase->transform(new IfFalseNode(iff));
    backward_ctl = phase->transform(new IfTrueNode(iff));
  } else {
    forward_ctl = ctl;
  }
}

Node* ArrayCopyNode::array_copy_forward(PhaseGVN *phase,
                                        bool can_reshape,
                                        Node*& forward_ctl,
                                        Node* mem,
                                        const TypePtr* atp_src,
                                        const TypePtr* atp_dest,
                                        Node* adr_src,
                                        Node* base_src,
                                        Node* adr_dest,
                                        Node* base_dest,
                                        BasicType copy_type,
                                        const Type* value_type,
                                        int count) {
  if (!forward_ctl->is_top()) {
    // copy forward
    MergeMemNode* mm = MergeMemNode::make(mem);

    if (count > 0) {
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      Node* v = load(bs, phase, forward_ctl, mm, adr_src, atp_src, value_type, copy_type);
      store(bs, phase, forward_ctl, mm, adr_dest, atp_dest, v, value_type, copy_type);
      for (int i = 1; i < count; i++) {
        Node* off  = phase->MakeConX(type2aelembytes(copy_type) * i);
        Node* next_src = phase->transform(new AddPNode(base_src,adr_src,off));
        Node* next_dest = phase->transform(new AddPNode(base_dest,adr_dest,off));
        v = load(bs, phase, forward_ctl, mm, next_src, atp_src, value_type, copy_type);
        store(bs, phase, forward_ctl, mm, next_dest, atp_dest, v, value_type, copy_type);
      }
    } else if (can_reshape) {
      PhaseIterGVN* igvn = phase->is_IterGVN();
      igvn->_worklist.push(adr_src);
      igvn->_worklist.push(adr_dest);
    }
    return mm;
  }
  return phase->C->top();
}

Node* ArrayCopyNode::array_copy_backward(PhaseGVN *phase,
                                         bool can_reshape,
                                         Node*& backward_ctl,
                                         Node* mem,
                                         const TypePtr* atp_src,
                                         const TypePtr* atp_dest,
                                         Node* adr_src,
                                         Node* base_src,
                                         Node* adr_dest,
                                         Node* base_dest,
                                         BasicType copy_type,
                                         const Type* value_type,
                                         int count) {
  if (!backward_ctl->is_top()) {
    // copy backward
    MergeMemNode* mm = MergeMemNode::make(mem);

    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    assert(copy_type != T_OBJECT || !bs->array_copy_requires_gc_barriers(false, T_OBJECT, false, false, BarrierSetC2::Optimization), "only tightly coupled allocations for object arrays");

    if (count > 0) {
      for (int i = count-1; i >= 1; i--) {
        Node* off  = phase->MakeConX(type2aelembytes(copy_type) * i);
        Node* next_src = phase->transform(new AddPNode(base_src,adr_src,off));
        Node* next_dest = phase->transform(new AddPNode(base_dest,adr_dest,off));
        Node* v = load(bs, phase, backward_ctl, mm, next_src, atp_src, value_type, copy_type);
        store(bs, phase, backward_ctl, mm, next_dest, atp_dest, v, value_type, copy_type);
      }
      Node* v = load(bs, phase, backward_ctl, mm, adr_src, atp_src, value_type, copy_type);
      store(bs, phase, backward_ctl, mm, adr_dest, atp_dest, v, value_type, copy_type);
    } else if (can_reshape) {
      PhaseIterGVN* igvn = phase->is_IterGVN();
      igvn->_worklist.push(adr_src);
      igvn->_worklist.push(adr_dest);
    }
    return phase->transform(mm);
  }
  return phase->C->top();
}

bool ArrayCopyNode::finish_transform(PhaseGVN *phase, bool can_reshape,
                                     Node* ctl, Node *mem) {
  if (can_reshape) {
    PhaseIterGVN* igvn = phase->is_IterGVN();
    igvn->set_delay_transform(false);
    if (is_clonebasic()) {
      Node* out_mem = proj_out(TypeFunc::Memory);

      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      if (out_mem->outcnt() != 1 || !out_mem->raw_out(0)->is_MergeMem() ||
          out_mem->raw_out(0)->outcnt() != 1 || !out_mem->raw_out(0)->raw_out(0)->is_MemBar()) {
        assert(bs->array_copy_requires_gc_barriers(true, T_OBJECT, true, is_clone_inst(), BarrierSetC2::Optimization), "can only happen with card marking");
        return false;
      }

      igvn->replace_node(out_mem->raw_out(0), mem);

      Node* out_ctl = proj_out(TypeFunc::Control);
      igvn->replace_node(out_ctl, ctl);
    } else {
      // replace fallthrough projections of the ArrayCopyNode by the
      // new memory, control and the input IO.
      CallProjections callprojs;
      extract_projections(&callprojs, true, false);

      if (callprojs.fallthrough_ioproj != NULL) {
        igvn->replace_node(callprojs.fallthrough_ioproj, in(TypeFunc::I_O));
      }
      if (callprojs.fallthrough_memproj != NULL) {
        igvn->replace_node(callprojs.fallthrough_memproj, mem);
      }
      if (callprojs.fallthrough_catchproj != NULL) {
        igvn->replace_node(callprojs.fallthrough_catchproj, ctl);
      }

      // The ArrayCopyNode is not disconnected. It still has the
      // projections for the exception case. Replace current
      // ArrayCopyNode with a dummy new one with a top() control so
      // that this part of the graph stays consistent but is
      // eventually removed.

      set_req(0, phase->C->top());
      remove_dead_region(phase, can_reshape);
    }
  } else {
    if (in(TypeFunc::Control) != ctl) {
      // we can't return new memory and control from Ideal at parse time
      assert(!is_clonebasic() || UseShenandoahGC, "added control for clone?");
      phase->record_for_igvn(this);
      return false;
    }
  }
  return true;
}


Node *ArrayCopyNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if (remove_dead_region(phase, can_reshape))  return this;

  if (StressArrayCopyMacroNode && !can_reshape) {
    phase->record_for_igvn(this);
    return NULL;
  }

  // See if it's a small array copy and we can inline it as
  // loads/stores
  // Here we can only do:
  // - arraycopy if all arguments were validated before and we don't
  // need card marking
  // - clone for which we don't need to do card marking

  if (!is_clonebasic() && !is_arraycopy_validated() &&
      !is_copyofrange_validated() && !is_copyof_validated()) {
    return NULL;
  }

  assert(in(TypeFunc::Control) != NULL &&
         in(TypeFunc::Memory) != NULL &&
         in(ArrayCopyNode::Src) != NULL &&
         in(ArrayCopyNode::Dest) != NULL &&
         in(ArrayCopyNode::Length) != NULL &&
         in(ArrayCopyNode::SrcPos) != NULL &&
         in(ArrayCopyNode::DestPos) != NULL, "broken inputs");

  if (in(TypeFunc::Control)->is_top() ||
      in(TypeFunc::Memory)->is_top() ||
      phase->type(in(ArrayCopyNode::Src)) == Type::TOP ||
      phase->type(in(ArrayCopyNode::Dest)) == Type::TOP ||
      (in(ArrayCopyNode::SrcPos) != NULL && in(ArrayCopyNode::SrcPos)->is_top()) ||
      (in(ArrayCopyNode::DestPos) != NULL && in(ArrayCopyNode::DestPos)->is_top())) {
    return NULL;
  }

  int count = get_count(phase);

  if (count < 0 || count > ArrayCopyLoadStoreMaxElem) {
    return NULL;
  }

  Node* mem = try_clone_instance(phase, can_reshape, count);
  if (mem != NULL) {
    return (mem == NodeSentinel) ? NULL : mem;
  }

  Node* adr_src = NULL;
  Node* base_src = NULL;
  Node* adr_dest = NULL;
  Node* base_dest = NULL;
  BasicType copy_type = T_ILLEGAL;
  const Type* value_type = NULL;
  bool disjoint_bases = false;

  if (!prepare_array_copy(phase, can_reshape,
                          adr_src, base_src, adr_dest, base_dest,
                          copy_type, value_type, disjoint_bases)) {
    return NULL;
  }

  Node* src = in(ArrayCopyNode::Src);
  Node* dest = in(ArrayCopyNode::Dest);
  const TypePtr* atp_src = get_address_type(phase, _src_type, src);
  const TypePtr* atp_dest = get_address_type(phase, _dest_type, dest);
  Node* in_mem = in(TypeFunc::Memory);

  if (can_reshape) {
    assert(!phase->is_IterGVN()->delay_transform(), "cannot delay transforms");
    phase->is_IterGVN()->set_delay_transform(true);
  }

  Node* backward_ctl = phase->C->top();
  Node* forward_ctl = phase->C->top();
  array_copy_test_overlap(phase, can_reshape, disjoint_bases, count, forward_ctl, backward_ctl);

  Node* forward_mem = array_copy_forward(phase, can_reshape, forward_ctl,
                                         in_mem,
                                         atp_src, atp_dest,
                                         adr_src, base_src, adr_dest, base_dest,
                                         copy_type, value_type, count);

  Node* backward_mem = array_copy_backward(phase, can_reshape, backward_ctl,
                                           in_mem,
                                           atp_src, atp_dest,
                                           adr_src, base_src, adr_dest, base_dest,
                                           copy_type, value_type, count);

  Node* ctl = NULL;
  if (!forward_ctl->is_top() && !backward_ctl->is_top()) {
    ctl = new RegionNode(3);
    ctl->init_req(1, forward_ctl);
    ctl->init_req(2, backward_ctl);
    ctl = phase->transform(ctl);
    MergeMemNode* forward_mm = forward_mem->as_MergeMem();
    MergeMemNode* backward_mm = backward_mem->as_MergeMem();
    for (MergeMemStream mms(forward_mm, backward_mm); mms.next_non_empty2(); ) {
      if (mms.memory() != mms.memory2()) {
        Node* phi = new PhiNode(ctl, Type::MEMORY, phase->C->get_adr_type(mms.alias_idx()));
        phi->init_req(1, mms.memory());
        phi->init_req(2, mms.memory2());
        phi = phase->transform(phi);
        mms.set_memory(phi);
      }
    }
    mem = forward_mem;
  } else if (!forward_ctl->is_top()) {
    ctl = forward_ctl;
    mem = forward_mem;
  } else {
    assert(!backward_ctl->is_top(), "no copy?");
    ctl = backward_ctl;
    mem = backward_mem;
  }

  if (can_reshape) {
    assert(phase->is_IterGVN()->delay_transform(), "should be delaying transforms");
    phase->is_IterGVN()->set_delay_transform(false);
  }

  if (!finish_transform(phase, can_reshape, ctl, mem)) {
    return NULL;
  }

  return mem;
}

bool ArrayCopyNode::may_modify(const TypeOopPtr *t_oop, PhaseTransform *phase) {
  Node* dest = in(ArrayCopyNode::Dest);
  if (dest->is_top()) {
    return false;
  }
  const TypeOopPtr* dest_t = phase->type(dest)->is_oopptr();
  assert(!dest_t->is_known_instance() || _dest_type->is_known_instance(), "result of EA not recorded");
  assert(in(ArrayCopyNode::Src)->is_top() || !phase->type(in(ArrayCopyNode::Src))->is_oopptr()->is_known_instance() ||
         _src_type->is_known_instance(), "result of EA not recorded");

  if (_dest_type != TypeOopPtr::BOTTOM || t_oop->is_known_instance()) {
    assert(_dest_type == TypeOopPtr::BOTTOM || _dest_type->is_known_instance(), "result of EA is known instance");
    return t_oop->instance_id() == _dest_type->instance_id();
  }

  return CallNode::may_modify_arraycopy_helper(dest_t, t_oop, phase);
}

bool ArrayCopyNode::may_modify_helper(const TypeOopPtr *t_oop, Node* n, PhaseTransform *phase, CallNode*& call) {
  if (n != NULL &&
      n->is_Call() &&
      n->as_Call()->may_modify(t_oop, phase) &&
      (n->as_Call()->is_ArrayCopy() || n->as_Call()->is_call_to_arraycopystub())) {
    call = n->as_Call();
    return true;
  }
  return false;
}

bool ArrayCopyNode::may_modify(const TypeOopPtr *t_oop, MemBarNode* mb, PhaseTransform *phase, ArrayCopyNode*& ac) {

  Node* c = mb->in(0);

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  // step over g1 gc barrier if we're at e.g. a clone with ReduceInitialCardMarks off
  c = bs->step_over_gc_barrier(c);

  CallNode* call = NULL;
  guarantee(c != NULL, "step_over_gc_barrier failed, there must be something to step to.");
  if (c->is_Region()) {
    for (uint i = 1; i < c->req(); i++) {
      if (c->in(i) != NULL) {
        Node* n = c->in(i)->in(0);
        if (may_modify_helper(t_oop, n, phase, call)) {
          ac = call->isa_ArrayCopy();
          assert(c == mb->in(0), "only for clone");
          return true;
        }
      }
    }
  } else if (may_modify_helper(t_oop, c->in(0), phase, call)) {
    ac = call->isa_ArrayCopy();
#ifdef ASSERT
    bool use_ReduceInitialCardMarks = BarrierSet::barrier_set()->is_a(BarrierSet::CardTableBarrierSet) &&
      static_cast<CardTableBarrierSetC2*>(bs)->use_ReduceInitialCardMarks();
    assert(c == mb->in(0) || (ac != NULL && ac->is_clonebasic() && !use_ReduceInitialCardMarks), "only for clone");
#endif
    return true;
  } else if (mb->trailing_partial_array_copy()) {
    return true;
  }

  return false;
}

// Does this array copy modify offsets between offset_lo and offset_hi
// in the destination array
// if must_modify is false, return true if the copy could write
// between offset_lo and offset_hi
// if must_modify is true, return true if the copy is guaranteed to
// write between offset_lo and offset_hi
bool ArrayCopyNode::modifies(intptr_t offset_lo, intptr_t offset_hi, PhaseTransform* phase, bool must_modify) const {
  assert(_kind == ArrayCopy || _kind == CopyOf || _kind == CopyOfRange, "only for real array copies");

  Node* dest = in(Dest);
  Node* dest_pos = in(DestPos);
  Node* len = in(Length);

  const TypeInt *dest_pos_t = phase->type(dest_pos)->isa_int();
  const TypeInt *len_t = phase->type(len)->isa_int();
  const TypeAryPtr* ary_t = phase->type(dest)->isa_aryptr();

  if (dest_pos_t == NULL || len_t == NULL || ary_t == NULL) {
    return !must_modify;
  }

  BasicType ary_elem = ary_t->klass()->as_array_klass()->element_type()->basic_type();
  uint header = arrayOopDesc::base_offset_in_bytes(ary_elem);
  uint elemsize = type2aelembytes(ary_elem);

  jlong dest_pos_plus_len_lo = (((jlong)dest_pos_t->_lo) + len_t->_lo) * elemsize + header;
  jlong dest_pos_plus_len_hi = (((jlong)dest_pos_t->_hi) + len_t->_hi) * elemsize + header;
  jlong dest_pos_lo = ((jlong)dest_pos_t->_lo) * elemsize + header;
  jlong dest_pos_hi = ((jlong)dest_pos_t->_hi) * elemsize + header;

  if (must_modify) {
    if (offset_lo >= dest_pos_hi && offset_hi < dest_pos_plus_len_lo) {
      return true;
    }
  } else {
    if (offset_hi >= dest_pos_lo && offset_lo < dest_pos_plus_len_hi) {
      return true;
    }
  }
  return false;
}

// As an optimization, choose optimum vector size for copy length known at compile time.
int ArrayCopyNode::get_partial_inline_vector_lane_count(BasicType type, int const_len) {
  int lane_count = ArrayOperationPartialInlineSize/type2aelembytes(type);
  if (const_len > 0) {
    int size_in_bytes = const_len * type2aelembytes(type);
    if (size_in_bytes <= 16)
      lane_count = 16/type2aelembytes(type);
    else if (size_in_bytes > 16 && size_in_bytes <= 32)
      lane_count = 32/type2aelembytes(type);
  }
  return lane_count;
}
