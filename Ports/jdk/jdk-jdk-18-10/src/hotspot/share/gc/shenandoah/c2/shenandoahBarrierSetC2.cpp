/*
 * Copyright (c) 2018, 2021, Red Hat, Inc. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shenandoah/shenandoahBarrierSet.hpp"
#include "gc/shenandoah/shenandoahForwarding.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahRuntime.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "gc/shenandoah/c2/shenandoahBarrierSetC2.hpp"
#include "gc/shenandoah/c2/shenandoahSupport.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/escape.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/macro.hpp"
#include "opto/movenode.hpp"
#include "opto/narrowptrnode.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"

ShenandoahBarrierSetC2* ShenandoahBarrierSetC2::bsc2() {
  return reinterpret_cast<ShenandoahBarrierSetC2*>(BarrierSet::barrier_set()->barrier_set_c2());
}

ShenandoahBarrierSetC2State::ShenandoahBarrierSetC2State(Arena* comp_arena)
  : _iu_barriers(new (comp_arena) GrowableArray<ShenandoahIUBarrierNode*>(comp_arena, 8,  0, NULL)),
    _load_reference_barriers(new (comp_arena) GrowableArray<ShenandoahLoadReferenceBarrierNode*>(comp_arena, 8,  0, NULL)) {
}

int ShenandoahBarrierSetC2State::iu_barriers_count() const {
  return _iu_barriers->length();
}

ShenandoahIUBarrierNode* ShenandoahBarrierSetC2State::iu_barrier(int idx) const {
  return _iu_barriers->at(idx);
}

void ShenandoahBarrierSetC2State::add_iu_barrier(ShenandoahIUBarrierNode* n) {
  assert(!_iu_barriers->contains(n), "duplicate entry in barrier list");
  _iu_barriers->append(n);
}

void ShenandoahBarrierSetC2State::remove_iu_barrier(ShenandoahIUBarrierNode* n) {
  _iu_barriers->remove_if_existing(n);
}

int ShenandoahBarrierSetC2State::load_reference_barriers_count() const {
  return _load_reference_barriers->length();
}

ShenandoahLoadReferenceBarrierNode* ShenandoahBarrierSetC2State::load_reference_barrier(int idx) const {
  return _load_reference_barriers->at(idx);
}

void ShenandoahBarrierSetC2State::add_load_reference_barrier(ShenandoahLoadReferenceBarrierNode * n) {
  assert(!_load_reference_barriers->contains(n), "duplicate entry in barrier list");
  _load_reference_barriers->append(n);
}

void ShenandoahBarrierSetC2State::remove_load_reference_barrier(ShenandoahLoadReferenceBarrierNode * n) {
  if (_load_reference_barriers->contains(n)) {
    _load_reference_barriers->remove(n);
  }
}

Node* ShenandoahBarrierSetC2::shenandoah_iu_barrier(GraphKit* kit, Node* obj) const {
  if (ShenandoahIUBarrier) {
    return kit->gvn().transform(new ShenandoahIUBarrierNode(obj));
  }
  return obj;
}

#define __ kit->

bool ShenandoahBarrierSetC2::satb_can_remove_pre_barrier(GraphKit* kit, PhaseTransform* phase, Node* adr,
                                                         BasicType bt, uint adr_idx) const {
  intptr_t offset = 0;
  Node* base = AddPNode::Ideal_base_and_offset(adr, phase, offset);
  AllocateNode* alloc = AllocateNode::Ideal_allocation(base, phase);

  if (offset == Type::OffsetBot) {
    return false; // cannot unalias unless there are precise offsets
  }

  if (alloc == NULL) {
    return false; // No allocation found
  }

  intptr_t size_in_bytes = type2aelembytes(bt);

  Node* mem = __ memory(adr_idx); // start searching here...

  for (int cnt = 0; cnt < 50; cnt++) {

    if (mem->is_Store()) {

      Node* st_adr = mem->in(MemNode::Address);
      intptr_t st_offset = 0;
      Node* st_base = AddPNode::Ideal_base_and_offset(st_adr, phase, st_offset);

      if (st_base == NULL) {
        break; // inscrutable pointer
      }

      // Break we have found a store with same base and offset as ours so break
      if (st_base == base && st_offset == offset) {
        break;
      }

      if (st_offset != offset && st_offset != Type::OffsetBot) {
        const int MAX_STORE = BytesPerLong;
        if (st_offset >= offset + size_in_bytes ||
            st_offset <= offset - MAX_STORE ||
            st_offset <= offset - mem->as_Store()->memory_size()) {
          // Success:  The offsets are provably independent.
          // (You may ask, why not just test st_offset != offset and be done?
          // The answer is that stores of different sizes can co-exist
          // in the same sequence of RawMem effects.  We sometimes initialize
          // a whole 'tile' of array elements with a single jint or jlong.)
          mem = mem->in(MemNode::Memory);
          continue; // advance through independent store memory
        }
      }

      if (st_base != base
          && MemNode::detect_ptr_independence(base, alloc, st_base,
                                              AllocateNode::Ideal_allocation(st_base, phase),
                                              phase)) {
        // Success:  The bases are provably independent.
        mem = mem->in(MemNode::Memory);
        continue; // advance through independent store memory
      }
    } else if (mem->is_Proj() && mem->in(0)->is_Initialize()) {

      InitializeNode* st_init = mem->in(0)->as_Initialize();
      AllocateNode* st_alloc = st_init->allocation();

      // Make sure that we are looking at the same allocation site.
      // The alloc variable is guaranteed to not be null here from earlier check.
      if (alloc == st_alloc) {
        // Check that the initialization is storing NULL so that no previous store
        // has been moved up and directly write a reference
        Node* captured_store = st_init->find_captured_store(offset,
                                                            type2aelembytes(T_OBJECT),
                                                            phase);
        if (captured_store == NULL || captured_store == st_init->zero_memory()) {
          return true;
        }
      }
    }

    // Unless there is an explicit 'continue', we must bail out here,
    // because 'mem' is an inscrutable memory state (e.g., a call).
    break;
  }

  return false;
}

#undef __
#define __ ideal.

void ShenandoahBarrierSetC2::satb_write_barrier_pre(GraphKit* kit,
                                                    bool do_load,
                                                    Node* obj,
                                                    Node* adr,
                                                    uint alias_idx,
                                                    Node* val,
                                                    const TypeOopPtr* val_type,
                                                    Node* pre_val,
                                                    BasicType bt) const {
  // Some sanity checks
  // Note: val is unused in this routine.

  if (do_load) {
    // We need to generate the load of the previous value
    assert(obj != NULL, "must have a base");
    assert(adr != NULL, "where are loading from?");
    assert(pre_val == NULL, "loaded already?");
    assert(val_type != NULL, "need a type");

    if (ReduceInitialCardMarks
        && satb_can_remove_pre_barrier(kit, &kit->gvn(), adr, bt, alias_idx)) {
      return;
    }

  } else {
    // In this case both val_type and alias_idx are unused.
    assert(pre_val != NULL, "must be loaded already");
    // Nothing to be done if pre_val is null.
    if (pre_val->bottom_type() == TypePtr::NULL_PTR) return;
    assert(pre_val->bottom_type()->basic_type() == T_OBJECT, "or we shouldn't be here");
  }
  assert(bt == T_OBJECT, "or we shouldn't be here");

  IdealKit ideal(kit, true);

  Node* tls = __ thread(); // ThreadLocalStorage

  Node* no_base = __ top();
  Node* zero  = __ ConI(0);
  Node* zeroX = __ ConX(0);

  float likely  = PROB_LIKELY(0.999);
  float unlikely  = PROB_UNLIKELY(0.999);

  // Offsets into the thread
  const int index_offset   = in_bytes(ShenandoahThreadLocalData::satb_mark_queue_index_offset());
  const int buffer_offset  = in_bytes(ShenandoahThreadLocalData::satb_mark_queue_buffer_offset());

  // Now the actual pointers into the thread
  Node* buffer_adr  = __ AddP(no_base, tls, __ ConX(buffer_offset));
  Node* index_adr   = __ AddP(no_base, tls, __ ConX(index_offset));

  // Now some of the values
  Node* marking;
  Node* gc_state = __ AddP(no_base, tls, __ ConX(in_bytes(ShenandoahThreadLocalData::gc_state_offset())));
  Node* ld = __ load(__ ctrl(), gc_state, TypeInt::BYTE, T_BYTE, Compile::AliasIdxRaw);
  marking = __ AndI(ld, __ ConI(ShenandoahHeap::MARKING));
  assert(ShenandoahBarrierC2Support::is_gc_state_load(ld), "Should match the shape");

  // if (!marking)
  __ if_then(marking, BoolTest::ne, zero, unlikely); {
    BasicType index_bt = TypeX_X->basic_type();
    assert(sizeof(size_t) == type2aelembytes(index_bt), "Loading Shenandoah SATBMarkQueue::_index with wrong size.");
    Node* index   = __ load(__ ctrl(), index_adr, TypeX_X, index_bt, Compile::AliasIdxRaw);

    if (do_load) {
      // load original value
      // alias_idx correct??
      pre_val = __ load(__ ctrl(), adr, val_type, bt, alias_idx);
    }

    // if (pre_val != NULL)
    __ if_then(pre_val, BoolTest::ne, kit->null()); {
      Node* buffer  = __ load(__ ctrl(), buffer_adr, TypeRawPtr::NOTNULL, T_ADDRESS, Compile::AliasIdxRaw);

      // is the queue for this thread full?
      __ if_then(index, BoolTest::ne, zeroX, likely); {

        // decrement the index
        Node* next_index = kit->gvn().transform(new SubXNode(index, __ ConX(sizeof(intptr_t))));

        // Now get the buffer location we will log the previous value into and store it
        Node *log_addr = __ AddP(no_base, buffer, next_index);
        __ store(__ ctrl(), log_addr, pre_val, T_OBJECT, Compile::AliasIdxRaw, MemNode::unordered);
        // update the index
        __ store(__ ctrl(), index_adr, next_index, index_bt, Compile::AliasIdxRaw, MemNode::unordered);

      } __ else_(); {

        // logging buffer is full, call the runtime
        const TypeFunc *tf = ShenandoahBarrierSetC2::write_ref_field_pre_entry_Type();
        __ make_leaf_call(tf, CAST_FROM_FN_PTR(address, ShenandoahRuntime::write_ref_field_pre_entry), "shenandoah_wb_pre", pre_val, tls);
      } __ end_if();  // (!index)
    } __ end_if();  // (pre_val != NULL)
  } __ end_if();  // (!marking)

  // Final sync IdealKit and GraphKit.
  kit->final_sync(ideal);

  if (ShenandoahSATBBarrier && adr != NULL) {
    Node* c = kit->control();
    Node* call = c->in(1)->in(1)->in(1)->in(0);
    assert(is_shenandoah_wb_pre_call(call), "shenandoah_wb_pre call expected");
    call->add_req(adr);
  }
}

bool ShenandoahBarrierSetC2::is_shenandoah_wb_pre_call(Node* call) {
  return call->is_CallLeaf() &&
         call->as_CallLeaf()->entry_point() == CAST_FROM_FN_PTR(address, ShenandoahRuntime::write_ref_field_pre_entry);
}

bool ShenandoahBarrierSetC2::is_shenandoah_lrb_call(Node* call) {
  if (!call->is_CallLeaf()) {
    return false;
  }

  address entry_point = call->as_CallLeaf()->entry_point();
  return (entry_point == CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong)) ||
         (entry_point == CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong_narrow)) ||
         (entry_point == CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak)) ||
         (entry_point == CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak_narrow)) ||
         (entry_point == CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_phantom));
}

bool ShenandoahBarrierSetC2::is_shenandoah_marking_if(PhaseTransform *phase, Node* n) {
  if (n->Opcode() != Op_If) {
    return false;
  }

  Node* bol = n->in(1);
  assert(bol->is_Bool(), "");
  Node* cmpx = bol->in(1);
  if (bol->as_Bool()->_test._test == BoolTest::ne &&
      cmpx->is_Cmp() && cmpx->in(2) == phase->intcon(0) &&
      is_shenandoah_state_load(cmpx->in(1)->in(1)) &&
      cmpx->in(1)->in(2)->is_Con() &&
      cmpx->in(1)->in(2) == phase->intcon(ShenandoahHeap::MARKING)) {
    return true;
  }

  return false;
}

bool ShenandoahBarrierSetC2::is_shenandoah_state_load(Node* n) {
  if (!n->is_Load()) return false;
  const int state_offset = in_bytes(ShenandoahThreadLocalData::gc_state_offset());
  return n->in(2)->is_AddP() && n->in(2)->in(2)->Opcode() == Op_ThreadLocal
         && n->in(2)->in(3)->is_Con()
         && n->in(2)->in(3)->bottom_type()->is_intptr_t()->get_con() == state_offset;
}

void ShenandoahBarrierSetC2::shenandoah_write_barrier_pre(GraphKit* kit,
                                                          bool do_load,
                                                          Node* obj,
                                                          Node* adr,
                                                          uint alias_idx,
                                                          Node* val,
                                                          const TypeOopPtr* val_type,
                                                          Node* pre_val,
                                                          BasicType bt) const {
  if (ShenandoahSATBBarrier) {
    IdealKit ideal(kit);
    kit->sync_kit(ideal);

    satb_write_barrier_pre(kit, do_load, obj, adr, alias_idx, val, val_type, pre_val, bt);

    ideal.sync_kit(kit);
    kit->final_sync(ideal);
  }
}

// Helper that guards and inserts a pre-barrier.
void ShenandoahBarrierSetC2::insert_pre_barrier(GraphKit* kit, Node* base_oop, Node* offset,
                                                Node* pre_val, bool need_mem_bar) const {
  // We could be accessing the referent field of a reference object. If so, when Shenandoah
  // is enabled, we need to log the value in the referent field in an SATB buffer.
  // This routine performs some compile time filters and generates suitable
  // runtime filters that guard the pre-barrier code.
  // Also add memory barrier for non volatile load from the referent field
  // to prevent commoning of loads across safepoint.

  // Some compile time checks.

  // If offset is a constant, is it java_lang_ref_Reference::_reference_offset?
  const TypeX* otype = offset->find_intptr_t_type();
  if (otype != NULL && otype->is_con() &&
      otype->get_con() != java_lang_ref_Reference::referent_offset()) {
    // Constant offset but not the reference_offset so just return
    return;
  }

  // We only need to generate the runtime guards for instances.
  const TypeOopPtr* btype = base_oop->bottom_type()->isa_oopptr();
  if (btype != NULL) {
    if (btype->isa_aryptr()) {
      // Array type so nothing to do
      return;
    }

    const TypeInstPtr* itype = btype->isa_instptr();
    if (itype != NULL) {
      // Can the klass of base_oop be statically determined to be
      // _not_ a sub-class of Reference and _not_ Object?
      ciKlass* klass = itype->klass();
      if ( klass->is_loaded() &&
          !klass->is_subtype_of(kit->env()->Reference_klass()) &&
          !kit->env()->Object_klass()->is_subtype_of(klass)) {
        return;
      }
    }
  }

  // The compile time filters did not reject base_oop/offset so
  // we need to generate the following runtime filters
  //
  // if (offset == java_lang_ref_Reference::_reference_offset) {
  //   if (instance_of(base, java.lang.ref.Reference)) {
  //     pre_barrier(_, pre_val, ...);
  //   }
  // }

  float likely   = PROB_LIKELY(  0.999);
  float unlikely = PROB_UNLIKELY(0.999);

  IdealKit ideal(kit);

  Node* referent_off = __ ConX(java_lang_ref_Reference::referent_offset());

  __ if_then(offset, BoolTest::eq, referent_off, unlikely); {
      // Update graphKit memory and control from IdealKit.
      kit->sync_kit(ideal);

      Node* ref_klass_con = kit->makecon(TypeKlassPtr::make(kit->env()->Reference_klass()));
      Node* is_instof = kit->gen_instanceof(base_oop, ref_klass_con);

      // Update IdealKit memory and control from graphKit.
      __ sync_kit(kit);

      Node* one = __ ConI(1);
      // is_instof == 0 if base_oop == NULL
      __ if_then(is_instof, BoolTest::eq, one, unlikely); {

        // Update graphKit from IdeakKit.
        kit->sync_kit(ideal);

        // Use the pre-barrier to record the value in the referent field
        satb_write_barrier_pre(kit, false /* do_load */,
                               NULL /* obj */, NULL /* adr */, max_juint /* alias_idx */, NULL /* val */, NULL /* val_type */,
                               pre_val /* pre_val */,
                               T_OBJECT);
        if (need_mem_bar) {
          // Add memory barrier to prevent commoning reads from this field
          // across safepoint since GC can change its value.
          kit->insert_mem_bar(Op_MemBarCPUOrder);
        }
        // Update IdealKit from graphKit.
        __ sync_kit(kit);

      } __ end_if(); // _ref_type != ref_none
  } __ end_if(); // offset == referent_offset

  // Final sync IdealKit and GraphKit.
  kit->final_sync(ideal);
}

#undef __

const TypeFunc* ShenandoahBarrierSetC2::write_ref_field_pre_entry_Type() {
  const Type **fields = TypeTuple::fields(2);
  fields[TypeFunc::Parms+0] = TypeInstPtr::NOTNULL; // original field value
  fields[TypeFunc::Parms+1] = TypeRawPtr::NOTNULL; // thread
  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+2, fields);

  // create result type (range)
  fields = TypeTuple::fields(0);
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+0, fields);

  return TypeFunc::make(domain, range);
}

const TypeFunc* ShenandoahBarrierSetC2::shenandoah_clone_barrier_Type() {
  const Type **fields = TypeTuple::fields(1);
  fields[TypeFunc::Parms+0] = TypeOopPtr::NOTNULL; // src oop
  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+1, fields);

  // create result type (range)
  fields = TypeTuple::fields(0);
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+0, fields);

  return TypeFunc::make(domain, range);
}

const TypeFunc* ShenandoahBarrierSetC2::shenandoah_load_reference_barrier_Type() {
  const Type **fields = TypeTuple::fields(2);
  fields[TypeFunc::Parms+0] = TypeOopPtr::BOTTOM; // original field value
  fields[TypeFunc::Parms+1] = TypeRawPtr::BOTTOM; // original load address

  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+2, fields);

  // create result type (range)
  fields = TypeTuple::fields(1);
  fields[TypeFunc::Parms+0] = TypeOopPtr::BOTTOM;
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+1, fields);

  return TypeFunc::make(domain, range);
}

Node* ShenandoahBarrierSetC2::store_at_resolved(C2Access& access, C2AccessValue& val) const {
  DecoratorSet decorators = access.decorators();

  const TypePtr* adr_type = access.addr().type();
  Node* adr = access.addr().node();

  bool anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool on_heap = (decorators & IN_HEAP) != 0;

  if (!access.is_oop() || (!on_heap && !anonymous)) {
    return BarrierSetC2::store_at_resolved(access, val);
  }

  if (access.is_parse_access()) {
    C2ParseAccess& parse_access = static_cast<C2ParseAccess&>(access);
    GraphKit* kit = parse_access.kit();

    uint adr_idx = kit->C->get_alias_index(adr_type);
    assert(adr_idx != Compile::AliasIdxTop, "use other store_to_memory factory" );
    Node* value = val.node();
    value = shenandoah_iu_barrier(kit, value);
    val.set_node(value);
    shenandoah_write_barrier_pre(kit, true /* do_load */, /*kit->control(),*/ access.base(), adr, adr_idx, val.node(),
                                 static_cast<const TypeOopPtr*>(val.type()), NULL /* pre_val */, access.type());
  } else {
    assert(access.is_opt_access(), "only for optimization passes");
    assert(((decorators & C2_TIGHTLY_COUPLED_ALLOC) != 0 || !ShenandoahSATBBarrier) && (decorators & C2_ARRAY_COPY) != 0, "unexpected caller of this code");
    C2OptAccess& opt_access = static_cast<C2OptAccess&>(access);
    PhaseGVN& gvn =  opt_access.gvn();

    if (ShenandoahIUBarrier) {
      Node* enqueue = gvn.transform(new ShenandoahIUBarrierNode(val.node()));
      val.set_node(enqueue);
    }
  }
  return BarrierSetC2::store_at_resolved(access, val);
}

Node* ShenandoahBarrierSetC2::load_at_resolved(C2Access& access, const Type* val_type) const {
  // 1: non-reference load, no additional barrier is needed
  if (!access.is_oop()) {
    return BarrierSetC2::load_at_resolved(access, val_type);;
  }

  Node* load = BarrierSetC2::load_at_resolved(access, val_type);
  DecoratorSet decorators = access.decorators();
  BasicType type = access.type();

  // 2: apply LRB if needed
  if (ShenandoahBarrierSet::need_load_reference_barrier(decorators, type)) {
    load = new ShenandoahLoadReferenceBarrierNode(NULL, load, decorators);
    if (access.is_parse_access()) {
      load = static_cast<C2ParseAccess &>(access).kit()->gvn().transform(load);
    } else {
      load = static_cast<C2OptAccess &>(access).gvn().transform(load);
    }
  }

  // 3: apply keep-alive barrier for java.lang.ref.Reference if needed
  if (ShenandoahBarrierSet::need_keep_alive_barrier(decorators, type)) {
    Node* top = Compile::current()->top();
    Node* adr = access.addr().node();
    Node* offset = adr->is_AddP() ? adr->in(AddPNode::Offset) : top;
    Node* obj = access.base();

    bool unknown = (decorators & ON_UNKNOWN_OOP_REF) != 0;
    bool on_weak_ref = (decorators & (ON_WEAK_OOP_REF | ON_PHANTOM_OOP_REF)) != 0;
    bool keep_alive = (decorators & AS_NO_KEEPALIVE) == 0;

    // If we are reading the value of the referent field of a Reference
    // object (either by using Unsafe directly or through reflection)
    // then, if SATB is enabled, we need to record the referent in an
    // SATB log buffer using the pre-barrier mechanism.
    // Also we need to add memory barrier to prevent commoning reads
    // from this field across safepoint since GC can change its value.
    if (!on_weak_ref || (unknown && (offset == top || obj == top)) || !keep_alive) {
      return load;
    }

    assert(access.is_parse_access(), "entry not supported at optimization time");
    C2ParseAccess& parse_access = static_cast<C2ParseAccess&>(access);
    GraphKit* kit = parse_access.kit();
    bool mismatched = (decorators & C2_MISMATCHED) != 0;
    bool is_unordered = (decorators & MO_UNORDERED) != 0;
    bool in_native = (decorators & IN_NATIVE) != 0;
    bool need_cpu_mem_bar = !is_unordered || mismatched || in_native;

    if (on_weak_ref) {
      // Use the pre-barrier to record the value in the referent field
      satb_write_barrier_pre(kit, false /* do_load */,
                             NULL /* obj */, NULL /* adr */, max_juint /* alias_idx */, NULL /* val */, NULL /* val_type */,
                             load /* pre_val */, T_OBJECT);
      // Add memory barrier to prevent commoning reads from this field
      // across safepoint since GC can change its value.
      kit->insert_mem_bar(Op_MemBarCPUOrder);
    } else if (unknown) {
      // We do not require a mem bar inside pre_barrier if need_mem_bar
      // is set: the barriers would be emitted by us.
      insert_pre_barrier(kit, obj, offset, load, !need_cpu_mem_bar);
    }
  }

  return load;
}

Node* ShenandoahBarrierSetC2::atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                   Node* new_val, const Type* value_type) const {
  GraphKit* kit = access.kit();
  if (access.is_oop()) {
    new_val = shenandoah_iu_barrier(kit, new_val);
    shenandoah_write_barrier_pre(kit, false /* do_load */,
                                 NULL, NULL, max_juint, NULL, NULL,
                                 expected_val /* pre_val */, T_OBJECT);

    MemNode::MemOrd mo = access.mem_node_mo();
    Node* mem = access.memory();
    Node* adr = access.addr().node();
    const TypePtr* adr_type = access.addr().type();
    Node* load_store = NULL;

#ifdef _LP64
    if (adr->bottom_type()->is_ptr_to_narrowoop()) {
      Node *newval_enc = kit->gvn().transform(new EncodePNode(new_val, new_val->bottom_type()->make_narrowoop()));
      Node *oldval_enc = kit->gvn().transform(new EncodePNode(expected_val, expected_val->bottom_type()->make_narrowoop()));
      if (ShenandoahCASBarrier) {
        load_store = kit->gvn().transform(new ShenandoahCompareAndExchangeNNode(kit->control(), mem, adr, newval_enc, oldval_enc, adr_type, value_type->make_narrowoop(), mo));
      } else {
        load_store = kit->gvn().transform(new CompareAndExchangeNNode(kit->control(), mem, adr, newval_enc, oldval_enc, adr_type, value_type->make_narrowoop(), mo));
      }
    } else
#endif
    {
      if (ShenandoahCASBarrier) {
        load_store = kit->gvn().transform(new ShenandoahCompareAndExchangePNode(kit->control(), mem, adr, new_val, expected_val, adr_type, value_type->is_oopptr(), mo));
      } else {
        load_store = kit->gvn().transform(new CompareAndExchangePNode(kit->control(), mem, adr, new_val, expected_val, adr_type, value_type->is_oopptr(), mo));
      }
    }

    access.set_raw_access(load_store);
    pin_atomic_op(access);

#ifdef _LP64
    if (adr->bottom_type()->is_ptr_to_narrowoop()) {
      load_store = kit->gvn().transform(new DecodeNNode(load_store, load_store->get_ptr_type()));
    }
#endif
    load_store = kit->gvn().transform(new ShenandoahLoadReferenceBarrierNode(NULL, load_store, access.decorators()));
    return load_store;
  }
  return BarrierSetC2::atomic_cmpxchg_val_at_resolved(access, expected_val, new_val, value_type);
}

Node* ShenandoahBarrierSetC2::atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                              Node* new_val, const Type* value_type) const {
  GraphKit* kit = access.kit();
  if (access.is_oop()) {
    new_val = shenandoah_iu_barrier(kit, new_val);
    shenandoah_write_barrier_pre(kit, false /* do_load */,
                                 NULL, NULL, max_juint, NULL, NULL,
                                 expected_val /* pre_val */, T_OBJECT);
    DecoratorSet decorators = access.decorators();
    MemNode::MemOrd mo = access.mem_node_mo();
    Node* mem = access.memory();
    bool is_weak_cas = (decorators & C2_WEAK_CMPXCHG) != 0;
    Node* load_store = NULL;
    Node* adr = access.addr().node();
#ifdef _LP64
    if (adr->bottom_type()->is_ptr_to_narrowoop()) {
      Node *newval_enc = kit->gvn().transform(new EncodePNode(new_val, new_val->bottom_type()->make_narrowoop()));
      Node *oldval_enc = kit->gvn().transform(new EncodePNode(expected_val, expected_val->bottom_type()->make_narrowoop()));
      if (ShenandoahCASBarrier) {
        if (is_weak_cas) {
          load_store = kit->gvn().transform(new ShenandoahWeakCompareAndSwapNNode(kit->control(), mem, adr, newval_enc, oldval_enc, mo));
        } else {
          load_store = kit->gvn().transform(new ShenandoahCompareAndSwapNNode(kit->control(), mem, adr, newval_enc, oldval_enc, mo));
        }
      } else {
        if (is_weak_cas) {
          load_store = kit->gvn().transform(new WeakCompareAndSwapNNode(kit->control(), mem, adr, newval_enc, oldval_enc, mo));
        } else {
          load_store = kit->gvn().transform(new CompareAndSwapNNode(kit->control(), mem, adr, newval_enc, oldval_enc, mo));
        }
      }
    } else
#endif
    {
      if (ShenandoahCASBarrier) {
        if (is_weak_cas) {
          load_store = kit->gvn().transform(new ShenandoahWeakCompareAndSwapPNode(kit->control(), mem, adr, new_val, expected_val, mo));
        } else {
          load_store = kit->gvn().transform(new ShenandoahCompareAndSwapPNode(kit->control(), mem, adr, new_val, expected_val, mo));
        }
      } else {
        if (is_weak_cas) {
          load_store = kit->gvn().transform(new WeakCompareAndSwapPNode(kit->control(), mem, adr, new_val, expected_val, mo));
        } else {
          load_store = kit->gvn().transform(new CompareAndSwapPNode(kit->control(), mem, adr, new_val, expected_val, mo));
        }
      }
    }
    access.set_raw_access(load_store);
    pin_atomic_op(access);
    return load_store;
  }
  return BarrierSetC2::atomic_cmpxchg_bool_at_resolved(access, expected_val, new_val, value_type);
}

Node* ShenandoahBarrierSetC2::atomic_xchg_at_resolved(C2AtomicParseAccess& access, Node* val, const Type* value_type) const {
  GraphKit* kit = access.kit();
  if (access.is_oop()) {
    val = shenandoah_iu_barrier(kit, val);
  }
  Node* result = BarrierSetC2::atomic_xchg_at_resolved(access, val, value_type);
  if (access.is_oop()) {
    result = kit->gvn().transform(new ShenandoahLoadReferenceBarrierNode(NULL, result, access.decorators()));
    shenandoah_write_barrier_pre(kit, false /* do_load */,
                                 NULL, NULL, max_juint, NULL, NULL,
                                 result /* pre_val */, T_OBJECT);
  }
  return result;
}

// Support for GC barriers emitted during parsing
bool ShenandoahBarrierSetC2::is_gc_barrier_node(Node* node) const {
  if (node->Opcode() == Op_ShenandoahLoadReferenceBarrier) return true;
  if (node->Opcode() != Op_CallLeaf && node->Opcode() != Op_CallLeafNoFP) {
    return false;
  }
  CallLeafNode *call = node->as_CallLeaf();
  if (call->_name == NULL) {
    return false;
  }

  return strcmp(call->_name, "shenandoah_clone_barrier") == 0 ||
         strcmp(call->_name, "shenandoah_cas_obj") == 0 ||
         strcmp(call->_name, "shenandoah_wb_pre") == 0;
}

Node* ShenandoahBarrierSetC2::step_over_gc_barrier(Node* c) const {
  if (c == NULL) {
    return c;
  }
  if (c->Opcode() == Op_ShenandoahLoadReferenceBarrier) {
    return c->in(ShenandoahLoadReferenceBarrierNode::ValueIn);
  }
  if (c->Opcode() == Op_ShenandoahIUBarrier) {
    c = c->in(1);
  }
  return c;
}

bool ShenandoahBarrierSetC2::expand_barriers(Compile* C, PhaseIterGVN& igvn) const {
  return !ShenandoahBarrierC2Support::expand(C, igvn);
}

bool ShenandoahBarrierSetC2::optimize_loops(PhaseIdealLoop* phase, LoopOptsMode mode, VectorSet& visited, Node_Stack& nstack, Node_List& worklist) const {
  if (mode == LoopOptsShenandoahExpand) {
    assert(UseShenandoahGC, "only for shenandoah");
    ShenandoahBarrierC2Support::pin_and_expand(phase);
    return true;
  } else if (mode == LoopOptsShenandoahPostExpand) {
    assert(UseShenandoahGC, "only for shenandoah");
    visited.clear();
    ShenandoahBarrierC2Support::optimize_after_expansion(visited, nstack, worklist, phase);
    return true;
  }
  return false;
}

bool ShenandoahBarrierSetC2::array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type, bool is_clone, bool is_clone_instance, ArrayCopyPhase phase) const {
  bool is_oop = is_reference_type(type);
  if (!is_oop) {
    return false;
  }
  if (ShenandoahSATBBarrier && tightly_coupled_alloc) {
    if (phase == Optimization) {
      return false;
    }
    return !is_clone;
  }
  if (phase == Optimization) {
    return !ShenandoahIUBarrier;
  }
  return true;
}

bool ShenandoahBarrierSetC2::clone_needs_barrier(Node* src, PhaseGVN& gvn) {
  const TypeOopPtr* src_type = gvn.type(src)->is_oopptr();
  if (src_type->isa_instptr() != NULL) {
    ciInstanceKlass* ik = src_type->klass()->as_instance_klass();
    if ((src_type->klass_is_exact() || (!ik->is_interface() && !ik->has_subklass())) && !ik->has_injected_fields()) {
      if (ik->has_object_fields()) {
        return true;
      } else {
        if (!src_type->klass_is_exact()) {
          Compile::current()->dependencies()->assert_leaf_type(ik);
        }
      }
    } else {
      return true;
        }
  } else if (src_type->isa_aryptr()) {
    BasicType src_elem  = src_type->klass()->as_array_klass()->element_type()->basic_type();
    if (is_reference_type(src_elem)) {
      return true;
    }
  } else {
    return true;
  }
  return false;
}

void ShenandoahBarrierSetC2::clone_at_expansion(PhaseMacroExpand* phase, ArrayCopyNode* ac) const {
  Node* ctrl = ac->in(TypeFunc::Control);
  Node* mem = ac->in(TypeFunc::Memory);
  Node* src_base = ac->in(ArrayCopyNode::Src);
  Node* src_offset = ac->in(ArrayCopyNode::SrcPos);
  Node* dest_base = ac->in(ArrayCopyNode::Dest);
  Node* dest_offset = ac->in(ArrayCopyNode::DestPos);
  Node* length = ac->in(ArrayCopyNode::Length);

  Node* src = phase->basic_plus_adr(src_base, src_offset);
  Node* dest = phase->basic_plus_adr(dest_base, dest_offset);

  if (ShenandoahCloneBarrier && clone_needs_barrier(src, phase->igvn())) {
    // Check if heap is has forwarded objects. If it does, we need to call into the special
    // routine that would fix up source references before we can continue.

    enum { _heap_stable = 1, _heap_unstable, PATH_LIMIT };
    Node* region = new RegionNode(PATH_LIMIT);
    Node* mem_phi = new PhiNode(region, Type::MEMORY, TypeRawPtr::BOTTOM);

    Node* thread = phase->transform_later(new ThreadLocalNode());
    Node* offset = phase->igvn().MakeConX(in_bytes(ShenandoahThreadLocalData::gc_state_offset()));
    Node* gc_state_addr = phase->transform_later(new AddPNode(phase->C->top(), thread, offset));

    uint gc_state_idx = Compile::AliasIdxRaw;
    const TypePtr* gc_state_adr_type = NULL; // debug-mode-only argument
    debug_only(gc_state_adr_type = phase->C->get_adr_type(gc_state_idx));

    Node* gc_state    = phase->transform_later(new LoadBNode(ctrl, mem, gc_state_addr, gc_state_adr_type, TypeInt::BYTE, MemNode::unordered));
    int flags = ShenandoahHeap::HAS_FORWARDED;
    if (ShenandoahIUBarrier) {
      flags |= ShenandoahHeap::MARKING;
    }
    Node* stable_and  = phase->transform_later(new AndINode(gc_state, phase->igvn().intcon(flags)));
    Node* stable_cmp  = phase->transform_later(new CmpINode(stable_and, phase->igvn().zerocon(T_INT)));
    Node* stable_test = phase->transform_later(new BoolNode(stable_cmp, BoolTest::ne));

    IfNode* stable_iff  = phase->transform_later(new IfNode(ctrl, stable_test, PROB_UNLIKELY(0.999), COUNT_UNKNOWN))->as_If();
    Node* stable_ctrl   = phase->transform_later(new IfFalseNode(stable_iff));
    Node* unstable_ctrl = phase->transform_later(new IfTrueNode(stable_iff));

    // Heap is stable, no need to do anything additional
    region->init_req(_heap_stable, stable_ctrl);
    mem_phi->init_req(_heap_stable, mem);

    // Heap is unstable, call into clone barrier stub
    Node* call = phase->make_leaf_call(unstable_ctrl, mem,
                    ShenandoahBarrierSetC2::shenandoah_clone_barrier_Type(),
                    CAST_FROM_FN_PTR(address, ShenandoahRuntime::shenandoah_clone_barrier),
                    "shenandoah_clone",
                    TypeRawPtr::BOTTOM,
                    src_base);
    call = phase->transform_later(call);

    ctrl = phase->transform_later(new ProjNode(call, TypeFunc::Control));
    mem = phase->transform_later(new ProjNode(call, TypeFunc::Memory));
    region->init_req(_heap_unstable, ctrl);
    mem_phi->init_req(_heap_unstable, mem);

    // Wire up the actual arraycopy stub now
    ctrl = phase->transform_later(region);
    mem = phase->transform_later(mem_phi);

    const char* name = "arraycopy";
    call = phase->make_leaf_call(ctrl, mem,
                                 OptoRuntime::fast_arraycopy_Type(),
                                 phase->basictype2arraycopy(T_LONG, NULL, NULL, true, name, true),
                                 name, TypeRawPtr::BOTTOM,
                                 src, dest, length
                                 LP64_ONLY(COMMA phase->top()));
    call = phase->transform_later(call);

    // Hook up the whole thing into the graph
    phase->igvn().replace_node(ac, call);
  } else {
    BarrierSetC2::clone_at_expansion(phase, ac);
  }
}


// Support for macro expanded GC barriers
void ShenandoahBarrierSetC2::register_potential_barrier_node(Node* node) const {
  if (node->Opcode() == Op_ShenandoahIUBarrier) {
    state()->add_iu_barrier((ShenandoahIUBarrierNode*) node);
  }
  if (node->Opcode() == Op_ShenandoahLoadReferenceBarrier) {
    state()->add_load_reference_barrier((ShenandoahLoadReferenceBarrierNode*) node);
  }
}

void ShenandoahBarrierSetC2::unregister_potential_barrier_node(Node* node) const {
  if (node->Opcode() == Op_ShenandoahIUBarrier) {
    state()->remove_iu_barrier((ShenandoahIUBarrierNode*) node);
  }
  if (node->Opcode() == Op_ShenandoahLoadReferenceBarrier) {
    state()->remove_load_reference_barrier((ShenandoahLoadReferenceBarrierNode*) node);
  }
}

void ShenandoahBarrierSetC2::eliminate_gc_barrier(PhaseMacroExpand* macro, Node* n) const {
  if (is_shenandoah_wb_pre_call(n)) {
    shenandoah_eliminate_wb_pre(n, &macro->igvn());
  }
}

void ShenandoahBarrierSetC2::shenandoah_eliminate_wb_pre(Node* call, PhaseIterGVN* igvn) const {
  assert(UseShenandoahGC && is_shenandoah_wb_pre_call(call), "");
  Node* c = call->as_Call()->proj_out(TypeFunc::Control);
  c = c->unique_ctrl_out();
  assert(c->is_Region() && c->req() == 3, "where's the pre barrier control flow?");
  c = c->unique_ctrl_out();
  assert(c->is_Region() && c->req() == 3, "where's the pre barrier control flow?");
  Node* iff = c->in(1)->is_IfProj() ? c->in(1)->in(0) : c->in(2)->in(0);
  assert(iff->is_If(), "expect test");
  if (!is_shenandoah_marking_if(igvn, iff)) {
    c = c->unique_ctrl_out();
    assert(c->is_Region() && c->req() == 3, "where's the pre barrier control flow?");
    iff = c->in(1)->is_IfProj() ? c->in(1)->in(0) : c->in(2)->in(0);
    assert(is_shenandoah_marking_if(igvn, iff), "expect marking test");
  }
  Node* cmpx = iff->in(1)->in(1);
  igvn->replace_node(cmpx, igvn->makecon(TypeInt::CC_EQ));
  igvn->rehash_node_delayed(call);
  call->del_req(call->req()-1);
}

void ShenandoahBarrierSetC2::enqueue_useful_gc_barrier(PhaseIterGVN* igvn, Node* node) const {
  if (node->Opcode() == Op_AddP && ShenandoahBarrierSetC2::has_only_shenandoah_wb_pre_uses(node)) {
    igvn->add_users_to_worklist(node);
  }
}

void ShenandoahBarrierSetC2::eliminate_useless_gc_barriers(Unique_Node_List &useful, Compile* C) const {
  for (uint i = 0; i < useful.size(); i++) {
    Node* n = useful.at(i);
    if (n->Opcode() == Op_AddP && ShenandoahBarrierSetC2::has_only_shenandoah_wb_pre_uses(n)) {
      for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
        C->record_for_igvn(n->fast_out(i));
      }
    }
  }
  for (int i = state()->iu_barriers_count() - 1; i >= 0; i--) {
    ShenandoahIUBarrierNode* n = state()->iu_barrier(i);
    if (!useful.member(n)) {
      state()->remove_iu_barrier(n);
    }
  }
  for (int i = state()->load_reference_barriers_count() - 1; i >= 0; i--) {
    ShenandoahLoadReferenceBarrierNode* n = state()->load_reference_barrier(i);
    if (!useful.member(n)) {
      state()->remove_load_reference_barrier(n);
    }
  }
}

void* ShenandoahBarrierSetC2::create_barrier_state(Arena* comp_arena) const {
  return new(comp_arena) ShenandoahBarrierSetC2State(comp_arena);
}

ShenandoahBarrierSetC2State* ShenandoahBarrierSetC2::state() const {
  return reinterpret_cast<ShenandoahBarrierSetC2State*>(Compile::current()->barrier_set_state());
}

// If the BarrierSetC2 state has kept macro nodes in its compilation unit state to be
// expanded later, then now is the time to do so.
bool ShenandoahBarrierSetC2::expand_macro_nodes(PhaseMacroExpand* macro) const { return false; }

#ifdef ASSERT
void ShenandoahBarrierSetC2::verify_gc_barriers(Compile* compile, CompilePhase phase) const {
  if (ShenandoahVerifyOptoBarriers && phase == BarrierSetC2::BeforeMacroExpand) {
    ShenandoahBarrierC2Support::verify(Compile::current()->root());
  } else if (phase == BarrierSetC2::BeforeCodeGen) {
    // Verify Shenandoah pre-barriers
    const int marking_offset = in_bytes(ShenandoahThreadLocalData::satb_mark_queue_active_offset());

    Unique_Node_List visited;
    Node_List worklist;
    // We're going to walk control flow backwards starting from the Root
    worklist.push(compile->root());
    while (worklist.size() > 0) {
      Node *x = worklist.pop();
      if (x == NULL || x == compile->top()) continue;
      if (visited.member(x)) {
        continue;
      } else {
        visited.push(x);
      }

      if (x->is_Region()) {
        for (uint i = 1; i < x->req(); i++) {
          worklist.push(x->in(i));
        }
      } else {
        worklist.push(x->in(0));
        // We are looking for the pattern:
        //                            /->ThreadLocal
        // If->Bool->CmpI->LoadB->AddP->ConL(marking_offset)
        //              \->ConI(0)
        // We want to verify that the If and the LoadB have the same control
        // See GraphKit::g1_write_barrier_pre()
        if (x->is_If()) {
          IfNode *iff = x->as_If();
          if (iff->in(1)->is_Bool() && iff->in(1)->in(1)->is_Cmp()) {
            CmpNode *cmp = iff->in(1)->in(1)->as_Cmp();
            if (cmp->Opcode() == Op_CmpI && cmp->in(2)->is_Con() && cmp->in(2)->bottom_type()->is_int()->get_con() == 0
                && cmp->in(1)->is_Load()) {
              LoadNode *load = cmp->in(1)->as_Load();
              if (load->Opcode() == Op_LoadB && load->in(2)->is_AddP() && load->in(2)->in(2)->Opcode() == Op_ThreadLocal
                  && load->in(2)->in(3)->is_Con()
                  && load->in(2)->in(3)->bottom_type()->is_intptr_t()->get_con() == marking_offset) {

                Node *if_ctrl = iff->in(0);
                Node *load_ctrl = load->in(0);

                if (if_ctrl != load_ctrl) {
                  // Skip possible CProj->NeverBranch in infinite loops
                  if ((if_ctrl->is_Proj() && if_ctrl->Opcode() == Op_CProj)
                      && (if_ctrl->in(0)->is_MultiBranch() && if_ctrl->in(0)->Opcode() == Op_NeverBranch)) {
                    if_ctrl = if_ctrl->in(0)->in(0);
                  }
                }
                assert(load_ctrl != NULL && if_ctrl == load_ctrl, "controls must match");
              }
            }
          }
        }
      }
    }
  }
}
#endif

Node* ShenandoahBarrierSetC2::ideal_node(PhaseGVN* phase, Node* n, bool can_reshape) const {
  if (is_shenandoah_wb_pre_call(n)) {
    uint cnt = ShenandoahBarrierSetC2::write_ref_field_pre_entry_Type()->domain()->cnt();
    if (n->req() > cnt) {
      Node* addp = n->in(cnt);
      if (has_only_shenandoah_wb_pre_uses(addp)) {
        n->del_req(cnt);
        if (can_reshape) {
          phase->is_IterGVN()->_worklist.push(addp);
        }
        return n;
      }
    }
  }
  if (n->Opcode() == Op_CmpP) {
    Node* in1 = n->in(1);
    Node* in2 = n->in(2);

    // If one input is NULL, then step over the strong LRB barriers on the other input
    if (in1->bottom_type() == TypePtr::NULL_PTR &&
        !((in2->Opcode() == Op_ShenandoahLoadReferenceBarrier) &&
          !ShenandoahBarrierSet::is_strong_access(((ShenandoahLoadReferenceBarrierNode*)in2)->decorators()))) {
      in2 = step_over_gc_barrier(in2);
    }
    if (in2->bottom_type() == TypePtr::NULL_PTR &&
        !((in1->Opcode() == Op_ShenandoahLoadReferenceBarrier) &&
          !ShenandoahBarrierSet::is_strong_access(((ShenandoahLoadReferenceBarrierNode*)in1)->decorators()))) {
      in1 = step_over_gc_barrier(in1);
    }

    if (in1 != n->in(1)) {
      n->set_req_X(1, in1, phase);
      assert(in2 == n->in(2), "only one change");
      return n;
    }
    if (in2 != n->in(2)) {
      n->set_req_X(2, in2, phase);
      return n;
    }
  } else if (can_reshape &&
             n->Opcode() == Op_If &&
             ShenandoahBarrierC2Support::is_heap_stable_test(n) &&
             n->in(0) != NULL) {
    Node* dom = n->in(0);
    Node* prev_dom = n;
    int op = n->Opcode();
    int dist = 16;
    // Search up the dominator tree for another heap stable test
    while (dom->Opcode() != op    ||  // Not same opcode?
           !ShenandoahBarrierC2Support::is_heap_stable_test(dom) ||  // Not same input 1?
           prev_dom->in(0) != dom) {  // One path of test does not dominate?
      if (dist < 0) return NULL;

      dist--;
      prev_dom = dom;
      dom = IfNode::up_one_dom(dom);
      if (!dom) return NULL;
    }

    // Check that we did not follow a loop back to ourselves
    if (n == dom) {
      return NULL;
    }

    return n->as_If()->dominated_by(prev_dom, phase->is_IterGVN());
  }

  return NULL;
}

bool ShenandoahBarrierSetC2::has_only_shenandoah_wb_pre_uses(Node* n) {
  for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
    Node* u = n->fast_out(i);
    if (!is_shenandoah_wb_pre_call(u)) {
      return false;
    }
  }
  return n->outcnt() > 0;
}

bool ShenandoahBarrierSetC2::final_graph_reshaping(Compile* compile, Node* n, uint opcode) const {
  switch (opcode) {
    case Op_CallLeaf:
    case Op_CallLeafNoFP: {
      assert (n->is_Call(), "");
      CallNode *call = n->as_Call();
      if (ShenandoahBarrierSetC2::is_shenandoah_wb_pre_call(call)) {
        uint cnt = ShenandoahBarrierSetC2::write_ref_field_pre_entry_Type()->domain()->cnt();
        if (call->req() > cnt) {
          assert(call->req() == cnt + 1, "only one extra input");
          Node *addp = call->in(cnt);
          assert(!ShenandoahBarrierSetC2::has_only_shenandoah_wb_pre_uses(addp), "useless address computation?");
          call->del_req(cnt);
        }
      }
      return false;
    }
    case Op_ShenandoahCompareAndSwapP:
    case Op_ShenandoahCompareAndSwapN:
    case Op_ShenandoahWeakCompareAndSwapN:
    case Op_ShenandoahWeakCompareAndSwapP:
    case Op_ShenandoahCompareAndExchangeP:
    case Op_ShenandoahCompareAndExchangeN:
      return true;
    case Op_ShenandoahLoadReferenceBarrier:
      assert(false, "should have been expanded already");
      return true;
    default:
      return false;
  }
}

bool ShenandoahBarrierSetC2::escape_add_to_con_graph(ConnectionGraph* conn_graph, PhaseGVN* gvn, Unique_Node_List* delayed_worklist, Node* n, uint opcode) const {
  switch (opcode) {
    case Op_ShenandoahCompareAndExchangeP:
    case Op_ShenandoahCompareAndExchangeN:
      conn_graph->add_objload_to_connection_graph(n, delayed_worklist);
      // fallthrough
    case Op_ShenandoahWeakCompareAndSwapP:
    case Op_ShenandoahWeakCompareAndSwapN:
    case Op_ShenandoahCompareAndSwapP:
    case Op_ShenandoahCompareAndSwapN:
      conn_graph->add_to_congraph_unsafe_access(n, opcode, delayed_worklist);
      return true;
    case Op_StoreP: {
      Node* adr = n->in(MemNode::Address);
      const Type* adr_type = gvn->type(adr);
      // Pointer stores in Shenandoah barriers looks like unsafe access.
      // Ignore such stores to be able scalar replace non-escaping
      // allocations.
      if (adr_type->isa_rawptr() && adr->is_AddP()) {
        Node* base = conn_graph->get_addp_base(adr);
        if (base->Opcode() == Op_LoadP &&
          base->in(MemNode::Address)->is_AddP()) {
          adr = base->in(MemNode::Address);
          Node* tls = conn_graph->get_addp_base(adr);
          if (tls->Opcode() == Op_ThreadLocal) {
             int offs = (int) gvn->find_intptr_t_con(adr->in(AddPNode::Offset), Type::OffsetBot);
             const int buf_offset = in_bytes(ShenandoahThreadLocalData::satb_mark_queue_buffer_offset());
             if (offs == buf_offset) {
               return true; // Pre barrier previous oop value store.
             }
          }
        }
      }
      return false;
    }
    case Op_ShenandoahIUBarrier:
      conn_graph->add_local_var_and_edge(n, PointsToNode::NoEscape, n->in(1), delayed_worklist);
      break;
    case Op_ShenandoahLoadReferenceBarrier:
      conn_graph->add_local_var_and_edge(n, PointsToNode::NoEscape, n->in(ShenandoahLoadReferenceBarrierNode::ValueIn), delayed_worklist);
      return true;
    default:
      // Nothing
      break;
  }
  return false;
}

bool ShenandoahBarrierSetC2::escape_add_final_edges(ConnectionGraph* conn_graph, PhaseGVN* gvn, Node* n, uint opcode) const {
  switch (opcode) {
    case Op_ShenandoahCompareAndExchangeP:
    case Op_ShenandoahCompareAndExchangeN: {
      Node *adr = n->in(MemNode::Address);
      conn_graph->add_local_var_and_edge(n, PointsToNode::NoEscape, adr, NULL);
      // fallthrough
    }
    case Op_ShenandoahCompareAndSwapP:
    case Op_ShenandoahCompareAndSwapN:
    case Op_ShenandoahWeakCompareAndSwapP:
    case Op_ShenandoahWeakCompareAndSwapN:
      return conn_graph->add_final_edges_unsafe_access(n, opcode);
    case Op_ShenandoahIUBarrier:
      conn_graph->add_local_var_and_edge(n, PointsToNode::NoEscape, n->in(1), NULL);
      return true;
    case Op_ShenandoahLoadReferenceBarrier:
      conn_graph->add_local_var_and_edge(n, PointsToNode::NoEscape, n->in(ShenandoahLoadReferenceBarrierNode::ValueIn), NULL);
      return true;
    default:
      // Nothing
      break;
  }
  return false;
}

bool ShenandoahBarrierSetC2::escape_has_out_with_unsafe_object(Node* n) const {
  return n->has_out_with(Op_ShenandoahCompareAndExchangeP) || n->has_out_with(Op_ShenandoahCompareAndExchangeN) ||
         n->has_out_with(Op_ShenandoahCompareAndSwapP, Op_ShenandoahCompareAndSwapN, Op_ShenandoahWeakCompareAndSwapP, Op_ShenandoahWeakCompareAndSwapN);

}

bool ShenandoahBarrierSetC2::matcher_find_shared_post_visit(Matcher* matcher, Node* n, uint opcode) const {
  switch (opcode) {
    case Op_ShenandoahCompareAndExchangeP:
    case Op_ShenandoahCompareAndExchangeN:
    case Op_ShenandoahWeakCompareAndSwapP:
    case Op_ShenandoahWeakCompareAndSwapN:
    case Op_ShenandoahCompareAndSwapP:
    case Op_ShenandoahCompareAndSwapN: {   // Convert trinary to binary-tree
      Node* newval = n->in(MemNode::ValueIn);
      Node* oldval = n->in(LoadStoreConditionalNode::ExpectedIn);
      Node* pair = new BinaryNode(oldval, newval);
      n->set_req(MemNode::ValueIn,pair);
      n->del_req(LoadStoreConditionalNode::ExpectedIn);
      return true;
    }
    default:
      break;
  }
  return false;
}

bool ShenandoahBarrierSetC2::matcher_is_store_load_barrier(Node* x, uint xop) const {
  return xop == Op_ShenandoahCompareAndExchangeP ||
         xop == Op_ShenandoahCompareAndExchangeN ||
         xop == Op_ShenandoahWeakCompareAndSwapP ||
         xop == Op_ShenandoahWeakCompareAndSwapN ||
         xop == Op_ShenandoahCompareAndSwapN ||
         xop == Op_ShenandoahCompareAndSwapP;
}
