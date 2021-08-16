/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/c2/g1BarrierSetC2.hpp"
#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1BarrierSetRuntime.hpp"
#include "gc/g1/g1CardTable.hpp"
#include "gc/g1/g1ThreadLocalData.hpp"
#include "gc/g1/heapRegion.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/compile.hpp"
#include "opto/escape.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/macro.hpp"
#include "opto/rootnode.hpp"
#include "opto/type.hpp"
#include "utilities/macros.hpp"

const TypeFunc *G1BarrierSetC2::write_ref_field_pre_entry_Type() {
  const Type **fields = TypeTuple::fields(2);
  fields[TypeFunc::Parms+0] = TypeInstPtr::NOTNULL; // original field value
  fields[TypeFunc::Parms+1] = TypeRawPtr::NOTNULL; // thread
  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+2, fields);

  // create result type (range)
  fields = TypeTuple::fields(0);
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+0, fields);

  return TypeFunc::make(domain, range);
}

const TypeFunc *G1BarrierSetC2::write_ref_field_post_entry_Type() {
  const Type **fields = TypeTuple::fields(2);
  fields[TypeFunc::Parms+0] = TypeRawPtr::NOTNULL;  // Card addr
  fields[TypeFunc::Parms+1] = TypeRawPtr::NOTNULL;  // thread
  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+2, fields);

  // create result type (range)
  fields = TypeTuple::fields(0);
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms, fields);

  return TypeFunc::make(domain, range);
}

#define __ ideal.
/*
 * Determine if the G1 pre-barrier can be removed. The pre-barrier is
 * required by SATB to make sure all objects live at the start of the
 * marking are kept alive, all reference updates need to any previous
 * reference stored before writing.
 *
 * If the previous value is NULL there is no need to save the old value.
 * References that are NULL are filtered during runtime by the barrier
 * code to avoid unnecessary queuing.
 *
 * However in the case of newly allocated objects it might be possible to
 * prove that the reference about to be overwritten is NULL during compile
 * time and avoid adding the barrier code completely.
 *
 * The compiler needs to determine that the object in which a field is about
 * to be written is newly allocated, and that no prior store to the same field
 * has happened since the allocation.
 *
 * Returns true if the pre-barrier can be removed
 */
bool G1BarrierSetC2::g1_can_remove_pre_barrier(GraphKit* kit,
                                               PhaseTransform* phase,
                                               Node* adr,
                                               BasicType bt,
                                               uint adr_idx) const {
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

  Node* mem = kit->memory(adr_idx); // start searching here...

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

// G1 pre/post barriers
void G1BarrierSetC2::pre_barrier(GraphKit* kit,
                                 bool do_load,
                                 Node* ctl,
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

    if (use_ReduceInitialCardMarks()
        && g1_can_remove_pre_barrier(kit, &kit->gvn(), adr, bt, alias_idx)) {
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

  BasicType active_type = in_bytes(SATBMarkQueue::byte_width_of_active()) == 4 ? T_INT : T_BYTE;
  assert(in_bytes(SATBMarkQueue::byte_width_of_active()) == 4 || in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "flag width");

  // Offsets into the thread
  const int marking_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
  const int index_offset   = in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset());
  const int buffer_offset  = in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset());

  // Now the actual pointers into the thread
  Node* marking_adr = __ AddP(no_base, tls, __ ConX(marking_offset));
  Node* buffer_adr  = __ AddP(no_base, tls, __ ConX(buffer_offset));
  Node* index_adr   = __ AddP(no_base, tls, __ ConX(index_offset));

  // Now some of the values
  Node* marking = __ load(__ ctrl(), marking_adr, TypeInt::INT, active_type, Compile::AliasIdxRaw);

  // if (!marking)
  __ if_then(marking, BoolTest::ne, zero, unlikely); {
    BasicType index_bt = TypeX_X->basic_type();
    assert(sizeof(size_t) == type2aelembytes(index_bt), "Loading G1 SATBMarkQueue::_index with wrong size.");
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
        const TypeFunc *tf = write_ref_field_pre_entry_Type();
        __ make_leaf_call(tf, CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_pre_entry), "write_ref_field_pre_entry", pre_val, tls);
      } __ end_if();  // (!index)
    } __ end_if();  // (pre_val != NULL)
  } __ end_if();  // (!marking)

  // Final sync IdealKit and GraphKit.
  kit->final_sync(ideal);
}

/*
 * G1 similar to any GC with a Young Generation requires a way to keep track of
 * references from Old Generation to Young Generation to make sure all live
 * objects are found. G1 also requires to keep track of object references
 * between different regions to enable evacuation of old regions, which is done
 * as part of mixed collections. References are tracked in remembered sets and
 * is continuously updated as reference are written to with the help of the
 * post-barrier.
 *
 * To reduce the number of updates to the remembered set the post-barrier
 * filters updates to fields in objects located in the Young Generation,
 * the same region as the reference, when the NULL is being written or
 * if the card is already marked as dirty by an earlier write.
 *
 * Under certain circumstances it is possible to avoid generating the
 * post-barrier completely if it is possible during compile time to prove
 * the object is newly allocated and that no safepoint exists between the
 * allocation and the store.
 *
 * In the case of slow allocation the allocation code must handle the barrier
 * as part of the allocation in the case the allocated object is not located
 * in the nursery; this would happen for humongous objects.
 *
 * Returns true if the post barrier can be removed
 */
bool G1BarrierSetC2::g1_can_remove_post_barrier(GraphKit* kit,
                                                PhaseTransform* phase, Node* store,
                                                Node* adr) const {
  intptr_t      offset = 0;
  Node*         base   = AddPNode::Ideal_base_and_offset(adr, phase, offset);
  AllocateNode* alloc  = AllocateNode::Ideal_allocation(base, phase);

  if (offset == Type::OffsetBot) {
    return false; // cannot unalias unless there are precise offsets
  }

  if (alloc == NULL) {
     return false; // No allocation found
  }

  // Start search from Store node
  Node* mem = store->in(MemNode::Control);
  if (mem->is_Proj() && mem->in(0)->is_Initialize()) {

    InitializeNode* st_init = mem->in(0)->as_Initialize();
    AllocateNode*  st_alloc = st_init->allocation();

    // Make sure we are looking at the same allocation
    if (alloc == st_alloc) {
      return true;
    }
  }

  return false;
}

//
// Update the card table and add card address to the queue
//
void G1BarrierSetC2::g1_mark_card(GraphKit* kit,
                                  IdealKit& ideal,
                                  Node* card_adr,
                                  Node* oop_store,
                                  uint oop_alias_idx,
                                  Node* index,
                                  Node* index_adr,
                                  Node* buffer,
                                  const TypeFunc* tf) const {
  Node* zero  = __ ConI(0);
  Node* zeroX = __ ConX(0);
  Node* no_base = __ top();
  BasicType card_bt = T_BYTE;
  // Smash zero into card. MUST BE ORDERED WRT TO STORE
  __ storeCM(__ ctrl(), card_adr, zero, oop_store, oop_alias_idx, card_bt, Compile::AliasIdxRaw);

  //  Now do the queue work
  __ if_then(index, BoolTest::ne, zeroX); {

    Node* next_index = kit->gvn().transform(new SubXNode(index, __ ConX(sizeof(intptr_t))));
    Node* log_addr = __ AddP(no_base, buffer, next_index);

    // Order, see storeCM.
    __ store(__ ctrl(), log_addr, card_adr, T_ADDRESS, Compile::AliasIdxRaw, MemNode::unordered);
    __ store(__ ctrl(), index_adr, next_index, TypeX_X->basic_type(), Compile::AliasIdxRaw, MemNode::unordered);

  } __ else_(); {
    __ make_leaf_call(tf, CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_post_entry), "write_ref_field_post_entry", card_adr, __ thread());
  } __ end_if();

}

void G1BarrierSetC2::post_barrier(GraphKit* kit,
                                  Node* ctl,
                                  Node* oop_store,
                                  Node* obj,
                                  Node* adr,
                                  uint alias_idx,
                                  Node* val,
                                  BasicType bt,
                                  bool use_precise) const {
  // If we are writing a NULL then we need no post barrier

  if (val != NULL && val->is_Con() && val->bottom_type() == TypePtr::NULL_PTR) {
    // Must be NULL
    const Type* t = val->bottom_type();
    assert(t == Type::TOP || t == TypePtr::NULL_PTR, "must be NULL");
    // No post barrier if writing NULLx
    return;
  }

  if (use_ReduceInitialCardMarks() && obj == kit->just_allocated_object(kit->control())) {
    // We can skip marks on a freshly-allocated object in Eden.
    // Keep this code in sync with new_deferred_store_barrier() in runtime.cpp.
    // That routine informs GC to take appropriate compensating steps,
    // upon a slow-path allocation, so as to make this card-mark
    // elision safe.
    return;
  }

  if (use_ReduceInitialCardMarks()
      && g1_can_remove_post_barrier(kit, &kit->gvn(), oop_store, adr)) {
    return;
  }

  if (!use_precise) {
    // All card marks for a (non-array) instance are in one place:
    adr = obj;
  }
  // (Else it's an array (or unknown), and we want more precise card marks.)
  assert(adr != NULL, "");

  IdealKit ideal(kit, true);

  Node* tls = __ thread(); // ThreadLocalStorage

  Node* no_base = __ top();
  float likely = PROB_LIKELY_MAG(3);
  float unlikely = PROB_UNLIKELY_MAG(3);
  Node* young_card = __ ConI((jint)G1CardTable::g1_young_card_val());
  Node* dirty_card = __ ConI((jint)G1CardTable::dirty_card_val());
  Node* zeroX = __ ConX(0);

  const TypeFunc *tf = write_ref_field_post_entry_Type();

  // Offsets into the thread
  const int index_offset  = in_bytes(G1ThreadLocalData::dirty_card_queue_index_offset());
  const int buffer_offset = in_bytes(G1ThreadLocalData::dirty_card_queue_buffer_offset());

  // Pointers into the thread

  Node* buffer_adr = __ AddP(no_base, tls, __ ConX(buffer_offset));
  Node* index_adr =  __ AddP(no_base, tls, __ ConX(index_offset));

  // Now some values
  // Use ctrl to avoid hoisting these values past a safepoint, which could
  // potentially reset these fields in the JavaThread.
  Node* index  = __ load(__ ctrl(), index_adr, TypeX_X, TypeX_X->basic_type(), Compile::AliasIdxRaw);
  Node* buffer = __ load(__ ctrl(), buffer_adr, TypeRawPtr::NOTNULL, T_ADDRESS, Compile::AliasIdxRaw);

  // Convert the store obj pointer to an int prior to doing math on it
  // Must use ctrl to prevent "integerized oop" existing across safepoint
  Node* cast =  __ CastPX(__ ctrl(), adr);

  // Divide pointer by card size
  Node* card_offset = __ URShiftX( cast, __ ConI(CardTable::card_shift) );

  // Combine card table base and card offset
  Node* card_adr = __ AddP(no_base, byte_map_base_node(kit), card_offset );

  // If we know the value being stored does it cross regions?

  if (val != NULL) {
    // Does the store cause us to cross regions?

    // Should be able to do an unsigned compare of region_size instead of
    // and extra shift. Do we have an unsigned compare??
    // Node* region_size = __ ConI(1 << HeapRegion::LogOfHRGrainBytes);
    Node* xor_res =  __ URShiftX ( __ XorX( cast,  __ CastPX(__ ctrl(), val)), __ ConI(HeapRegion::LogOfHRGrainBytes));

    // if (xor_res == 0) same region so skip
    __ if_then(xor_res, BoolTest::ne, zeroX, likely); {

      // No barrier if we are storing a NULL
      __ if_then(val, BoolTest::ne, kit->null(), likely); {

        // Ok must mark the card if not already dirty

        // load the original value of the card
        Node* card_val = __ load(__ ctrl(), card_adr, TypeInt::INT, T_BYTE, Compile::AliasIdxRaw);

        __ if_then(card_val, BoolTest::ne, young_card, unlikely); {
          kit->sync_kit(ideal);
          kit->insert_mem_bar(Op_MemBarVolatile, oop_store);
          __ sync_kit(kit);

          Node* card_val_reload = __ load(__ ctrl(), card_adr, TypeInt::INT, T_BYTE, Compile::AliasIdxRaw);
          __ if_then(card_val_reload, BoolTest::ne, dirty_card); {
            g1_mark_card(kit, ideal, card_adr, oop_store, alias_idx, index, index_adr, buffer, tf);
          } __ end_if();
        } __ end_if();
      } __ end_if();
    } __ end_if();
  } else {
    // The Object.clone() intrinsic uses this path if !ReduceInitialCardMarks.
    // We don't need a barrier here if the destination is a newly allocated object
    // in Eden. Otherwise, GC verification breaks because we assume that cards in Eden
    // are set to 'g1_young_gen' (see G1CardTable::verify_g1_young_region()).
    assert(!use_ReduceInitialCardMarks(), "can only happen with card marking");
    Node* card_val = __ load(__ ctrl(), card_adr, TypeInt::INT, T_BYTE, Compile::AliasIdxRaw);
    __ if_then(card_val, BoolTest::ne, young_card); {
      g1_mark_card(kit, ideal, card_adr, oop_store, alias_idx, index, index_adr, buffer, tf);
    } __ end_if();
  }

  // Final sync IdealKit and GraphKit.
  kit->final_sync(ideal);
}

// Helper that guards and inserts a pre-barrier.
void G1BarrierSetC2::insert_pre_barrier(GraphKit* kit, Node* base_oop, Node* offset,
                                        Node* pre_val, bool need_mem_bar) const {
  // We could be accessing the referent field of a reference object. If so, when G1
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
        pre_barrier(kit, false /* do_load */,
                    __ ctrl(),
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

Node* G1BarrierSetC2::load_at_resolved(C2Access& access, const Type* val_type) const {
  DecoratorSet decorators = access.decorators();
  Node* adr = access.addr().node();
  Node* obj = access.base();

  bool anonymous = (decorators & C2_UNSAFE_ACCESS) != 0;
  bool mismatched = (decorators & C2_MISMATCHED) != 0;
  bool unknown = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool on_weak = (decorators & ON_WEAK_OOP_REF) != 0;
  bool on_phantom = (decorators & ON_PHANTOM_OOP_REF) != 0;
  bool is_unordered = (decorators & MO_UNORDERED) != 0;
  bool no_keepalive = (decorators & AS_NO_KEEPALIVE) != 0;
  bool is_mixed = !in_heap && !in_native;
  bool need_cpu_mem_bar = !is_unordered || mismatched || is_mixed;

  Node* top = Compile::current()->top();
  Node* offset = adr->is_AddP() ? adr->in(AddPNode::Offset) : top;
  Node* load = CardTableBarrierSetC2::load_at_resolved(access, val_type);

  // If we are reading the value of the referent field of a Reference
  // object (either by using Unsafe directly or through reflection)
  // then, if G1 is enabled, we need to record the referent in an
  // SATB log buffer using the pre-barrier mechanism.
  // Also we need to add memory barrier to prevent commoning reads
  // from this field across safepoint since GC can change its value.
  bool need_read_barrier = (((on_weak || on_phantom) && !no_keepalive) ||
                            (in_heap && unknown && offset != top && obj != top));

  if (!access.is_oop() || !need_read_barrier) {
    return load;
  }

  assert(access.is_parse_access(), "entry not supported at optimization time");
  C2ParseAccess& parse_access = static_cast<C2ParseAccess&>(access);
  GraphKit* kit = parse_access.kit();

  if (on_weak || on_phantom) {
    // Use the pre-barrier to record the value in the referent field
    pre_barrier(kit, false /* do_load */,
                kit->control(),
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

  return load;
}

bool G1BarrierSetC2::is_gc_barrier_node(Node* node) const {
  if (CardTableBarrierSetC2::is_gc_barrier_node(node)) {
    return true;
  }
  if (node->Opcode() != Op_CallLeaf) {
    return false;
  }
  CallLeafNode *call = node->as_CallLeaf();
  if (call->_name == NULL) {
    return false;
  }

  return strcmp(call->_name, "write_ref_field_pre_entry") == 0 || strcmp(call->_name, "write_ref_field_post_entry") == 0;
}

void G1BarrierSetC2::eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const {
  assert(node->Opcode() == Op_CastP2X, "ConvP2XNode required");
  assert(node->outcnt() <= 2, "expects 1 or 2 users: Xor and URShift nodes");
  // It could be only one user, URShift node, in Object.clone() intrinsic
  // but the new allocation is passed to arraycopy stub and it could not
  // be scalar replaced. So we don't check the case.

  // An other case of only one user (Xor) is when the value check for NULL
  // in G1 post barrier is folded after CCP so the code which used URShift
  // is removed.

  // Take Region node before eliminating post barrier since it also
  // eliminates CastP2X node when it has only one user.
  Node* this_region = node->in(0);
  assert(this_region != NULL, "");

  // Remove G1 post barrier.

  // Search for CastP2X->Xor->URShift->Cmp path which
  // checks if the store done to a different from the value's region.
  // And replace Cmp with #0 (false) to collapse G1 post barrier.
  Node* xorx = node->find_out_with(Op_XorX);
  if (xorx != NULL) {
    Node* shift = xorx->unique_out();
    Node* cmpx = shift->unique_out();
    assert(cmpx->is_Cmp() && cmpx->unique_out()->is_Bool() &&
    cmpx->unique_out()->as_Bool()->_test._test == BoolTest::ne,
    "missing region check in G1 post barrier");
    macro->replace_node(cmpx, macro->makecon(TypeInt::CC_EQ));

    // Remove G1 pre barrier.

    // Search "if (marking != 0)" check and set it to "false".
    // There is no G1 pre barrier if previous stored value is NULL
    // (for example, after initialization).
    if (this_region->is_Region() && this_region->req() == 3) {
      int ind = 1;
      if (!this_region->in(ind)->is_IfFalse()) {
        ind = 2;
      }
      if (this_region->in(ind)->is_IfFalse() &&
          this_region->in(ind)->in(0)->Opcode() == Op_If) {
        Node* bol = this_region->in(ind)->in(0)->in(1);
        assert(bol->is_Bool(), "");
        cmpx = bol->in(1);
        if (bol->as_Bool()->_test._test == BoolTest::ne &&
            cmpx->is_Cmp() && cmpx->in(2) == macro->intcon(0) &&
            cmpx->in(1)->is_Load()) {
          Node* adr = cmpx->in(1)->as_Load()->in(MemNode::Address);
          const int marking_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
          if (adr->is_AddP() && adr->in(AddPNode::Base) == macro->top() &&
              adr->in(AddPNode::Address)->Opcode() == Op_ThreadLocal &&
              adr->in(AddPNode::Offset) == macro->MakeConX(marking_offset)) {
            macro->replace_node(cmpx, macro->makecon(TypeInt::CC_EQ));
          }
        }
      }
    }
  } else {
    assert(!use_ReduceInitialCardMarks(), "can only happen with card marking");
    // This is a G1 post barrier emitted by the Object.clone() intrinsic.
    // Search for the CastP2X->URShiftX->AddP->LoadB->Cmp path which checks if the card
    // is marked as young_gen and replace the Cmp with 0 (false) to collapse the barrier.
    Node* shift = node->find_out_with(Op_URShiftX);
    assert(shift != NULL, "missing G1 post barrier");
    Node* addp = shift->unique_out();
    Node* load = addp->find_out_with(Op_LoadB);
    assert(load != NULL, "missing G1 post barrier");
    Node* cmpx = load->unique_out();
    assert(cmpx->is_Cmp() && cmpx->unique_out()->is_Bool() &&
           cmpx->unique_out()->as_Bool()->_test._test == BoolTest::ne,
           "missing card value check in G1 post barrier");
    macro->replace_node(cmpx, macro->makecon(TypeInt::CC_EQ));
    // There is no G1 pre barrier in this case
  }
  // Now CastP2X can be removed since it is used only on dead path
  // which currently still alive until igvn optimize it.
  assert(node->outcnt() == 0 || node->unique_out()->Opcode() == Op_URShiftX, "");
  macro->replace_node(node, macro->top());
}

Node* G1BarrierSetC2::step_over_gc_barrier(Node* c) const {
  if (!use_ReduceInitialCardMarks() &&
      c != NULL && c->is_Region() && c->req() == 3) {
    for (uint i = 1; i < c->req(); i++) {
      if (c->in(i) != NULL && c->in(i)->is_Region() &&
          c->in(i)->req() == 3) {
        Node* r = c->in(i);
        for (uint j = 1; j < r->req(); j++) {
          if (r->in(j) != NULL && r->in(j)->is_Proj() &&
              r->in(j)->in(0) != NULL &&
              r->in(j)->in(0)->Opcode() == Op_CallLeaf &&
              r->in(j)->in(0)->as_Call()->entry_point() == CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_post_entry)) {
            Node* call = r->in(j)->in(0);
            c = c->in(i == 1 ? 2 : 1);
            if (c != NULL && c->Opcode() != Op_Parm) {
              c = c->in(0);
              if (c != NULL) {
                c = c->in(0);
                assert(call->in(0) == NULL ||
                       call->in(0)->in(0) == NULL ||
                       call->in(0)->in(0)->in(0) == NULL ||
                       call->in(0)->in(0)->in(0)->in(0) == NULL ||
                       call->in(0)->in(0)->in(0)->in(0)->in(0) == NULL ||
                       c == call->in(0)->in(0)->in(0)->in(0)->in(0), "bad barrier shape");
                return c;
              }
            }
          }
        }
      }
    }
  }
  return c;
}

#ifdef ASSERT
void G1BarrierSetC2::verify_gc_barriers(Compile* compile, CompilePhase phase) const {
  if (phase != BarrierSetC2::BeforeCodeGen) {
    return;
  }
  // Verify G1 pre-barriers
  const int marking_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());

  Unique_Node_List visited;
  Node_List worklist;
  // We're going to walk control flow backwards starting from the Root
  worklist.push(compile->root());
  while (worklist.size() > 0) {
    Node* x = worklist.pop();
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
            LoadNode* load = cmp->in(1)->as_Load();
            if (load->Opcode() == Op_LoadB && load->in(2)->is_AddP() && load->in(2)->in(2)->Opcode() == Op_ThreadLocal
                && load->in(2)->in(3)->is_Con()
                && load->in(2)->in(3)->bottom_type()->is_intptr_t()->get_con() == marking_offset) {

              Node* if_ctrl = iff->in(0);
              Node* load_ctrl = load->in(0);

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
#endif

bool G1BarrierSetC2::escape_add_to_con_graph(ConnectionGraph* conn_graph, PhaseGVN* gvn, Unique_Node_List* delayed_worklist, Node* n, uint opcode) const {
  if (opcode == Op_StoreP) {
    Node* adr = n->in(MemNode::Address);
    const Type* adr_type = gvn->type(adr);
    // Pointer stores in G1 barriers looks like unsafe access.
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
          const int buf_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset());
          if (offs == buf_offset) {
            return true; // G1 pre barrier previous oop value store.
          }
          if (offs == in_bytes(G1ThreadLocalData::dirty_card_queue_buffer_offset())) {
            return true; // G1 post barrier card address store.
          }
        }
      }
    }
  }
  return false;
}
