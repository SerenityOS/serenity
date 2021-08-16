/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/javaClasses.hpp"
#include "gc/z/c2/zBarrierSetC2.hpp"
#include "gc/z/zBarrierSet.hpp"
#include "gc/z/zBarrierSetAssembler.hpp"
#include "gc/z/zBarrierSetRuntime.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/addnode.hpp"
#include "opto/block.hpp"
#include "opto/compile.hpp"
#include "opto/graphKit.hpp"
#include "opto/machnode.hpp"
#include "opto/macro.hpp"
#include "opto/memnode.hpp"
#include "opto/node.hpp"
#include "opto/output.hpp"
#include "opto/regalloc.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/type.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

class ZBarrierSetC2State : public ResourceObj {
private:
  GrowableArray<ZLoadBarrierStubC2*>* _stubs;
  Node_Array                          _live;

public:
  ZBarrierSetC2State(Arena* arena) :
    _stubs(new (arena) GrowableArray<ZLoadBarrierStubC2*>(arena, 8,  0, NULL)),
    _live(arena) {}

  GrowableArray<ZLoadBarrierStubC2*>* stubs() {
    return _stubs;
  }

  RegMask* live(const Node* node) {
    if (!node->is_Mach()) {
      // Don't need liveness for non-MachNodes
      return NULL;
    }

    const MachNode* const mach = node->as_Mach();
    if (mach->barrier_data() == ZLoadBarrierElided) {
      // Don't need liveness data for nodes without barriers
      return NULL;
    }

    RegMask* live = (RegMask*)_live[node->_idx];
    if (live == NULL) {
      live = new (Compile::current()->comp_arena()->AmallocWords(sizeof(RegMask))) RegMask();
      _live.map(node->_idx, (Node*)live);
    }

    return live;
  }
};

static ZBarrierSetC2State* barrier_set_state() {
  return reinterpret_cast<ZBarrierSetC2State*>(Compile::current()->barrier_set_state());
}

ZLoadBarrierStubC2* ZLoadBarrierStubC2::create(const MachNode* node, Address ref_addr, Register ref, Register tmp, uint8_t barrier_data) {
  ZLoadBarrierStubC2* const stub = new (Compile::current()->comp_arena()) ZLoadBarrierStubC2(node, ref_addr, ref, tmp, barrier_data);
  if (!Compile::current()->output()->in_scratch_emit_size()) {
    barrier_set_state()->stubs()->append(stub);
  }

  return stub;
}

ZLoadBarrierStubC2::ZLoadBarrierStubC2(const MachNode* node, Address ref_addr, Register ref, Register tmp, uint8_t barrier_data) :
    _node(node),
    _ref_addr(ref_addr),
    _ref(ref),
    _tmp(tmp),
    _barrier_data(barrier_data),
    _entry(),
    _continuation() {
  assert_different_registers(ref, ref_addr.base());
  assert_different_registers(ref, ref_addr.index());
}

Address ZLoadBarrierStubC2::ref_addr() const {
  return _ref_addr;
}

Register ZLoadBarrierStubC2::ref() const {
  return _ref;
}

Register ZLoadBarrierStubC2::tmp() const {
  return _tmp;
}

address ZLoadBarrierStubC2::slow_path() const {
  DecoratorSet decorators = DECORATORS_NONE;
  if (_barrier_data & ZLoadBarrierStrong) {
    decorators |= ON_STRONG_OOP_REF;
  }
  if (_barrier_data & ZLoadBarrierWeak) {
    decorators |= ON_WEAK_OOP_REF;
  }
  if (_barrier_data & ZLoadBarrierPhantom) {
    decorators |= ON_PHANTOM_OOP_REF;
  }
  if (_barrier_data & ZLoadBarrierNoKeepalive) {
    decorators |= AS_NO_KEEPALIVE;
  }
  return ZBarrierSetRuntime::load_barrier_on_oop_field_preloaded_addr(decorators);
}

RegMask& ZLoadBarrierStubC2::live() const {
  return *barrier_set_state()->live(_node);
}

Label* ZLoadBarrierStubC2::entry() {
  // The _entry will never be bound when in_scratch_emit_size() is true.
  // However, we still need to return a label that is not bound now, but
  // will eventually be bound. Any lable will do, as it will only act as
  // a placeholder, so we return the _continuation label.
  return Compile::current()->output()->in_scratch_emit_size() ? &_continuation : &_entry;
}

Label* ZLoadBarrierStubC2::continuation() {
  return &_continuation;
}

void* ZBarrierSetC2::create_barrier_state(Arena* comp_arena) const {
  return new (comp_arena) ZBarrierSetC2State(comp_arena);
}

void ZBarrierSetC2::late_barrier_analysis() const {
  analyze_dominating_barriers();
  compute_liveness_at_stubs();
}

void ZBarrierSetC2::emit_stubs(CodeBuffer& cb) const {
  MacroAssembler masm(&cb);
  GrowableArray<ZLoadBarrierStubC2*>* const stubs = barrier_set_state()->stubs();

  for (int i = 0; i < stubs->length(); i++) {
    // Make sure there is enough space in the code buffer
    if (cb.insts()->maybe_expand_to_ensure_remaining(PhaseOutput::MAX_inst_size) && cb.blob() == NULL) {
      ciEnv::current()->record_failure("CodeCache is full");
      return;
    }

    ZBarrierSet::assembler()->generate_c2_load_barrier_stub(&masm, stubs->at(i));
  }

  masm.flush();
}

int ZBarrierSetC2::estimate_stub_size() const {
  Compile* const C = Compile::current();
  BufferBlob* const blob = C->output()->scratch_buffer_blob();
  GrowableArray<ZLoadBarrierStubC2*>* const stubs = barrier_set_state()->stubs();
  int size = 0;

  for (int i = 0; i < stubs->length(); i++) {
    CodeBuffer cb(blob->content_begin(), (address)C->output()->scratch_locs_memory() - blob->content_begin());
    MacroAssembler masm(&cb);
    ZBarrierSet::assembler()->generate_c2_load_barrier_stub(&masm, stubs->at(i));
    size += cb.insts_size();
  }

  return size;
}

static void set_barrier_data(C2Access& access) {
  if (ZBarrierSet::barrier_needed(access.decorators(), access.type())) {
    uint8_t barrier_data = 0;

    if (access.decorators() & ON_PHANTOM_OOP_REF) {
      barrier_data |= ZLoadBarrierPhantom;
    } else if (access.decorators() & ON_WEAK_OOP_REF) {
      barrier_data |= ZLoadBarrierWeak;
    } else {
      barrier_data |= ZLoadBarrierStrong;
    }

    if (access.decorators() & AS_NO_KEEPALIVE) {
      barrier_data |= ZLoadBarrierNoKeepalive;
    }

    access.set_barrier_data(barrier_data);
  }
}

Node* ZBarrierSetC2::load_at_resolved(C2Access& access, const Type* val_type) const {
  set_barrier_data(access);
  return BarrierSetC2::load_at_resolved(access, val_type);
}

Node* ZBarrierSetC2::atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                    Node* new_val, const Type* val_type) const {
  set_barrier_data(access);
  return BarrierSetC2::atomic_cmpxchg_val_at_resolved(access, expected_val, new_val, val_type);
}

Node* ZBarrierSetC2::atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                     Node* new_val, const Type* value_type) const {
  set_barrier_data(access);
  return BarrierSetC2::atomic_cmpxchg_bool_at_resolved(access, expected_val, new_val, value_type);
}

Node* ZBarrierSetC2::atomic_xchg_at_resolved(C2AtomicParseAccess& access, Node* new_val, const Type* val_type) const {
  set_barrier_data(access);
  return BarrierSetC2::atomic_xchg_at_resolved(access, new_val, val_type);
}

bool ZBarrierSetC2::array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type,
                                                    bool is_clone, bool is_clone_instance,
                                                    ArrayCopyPhase phase) const {
  if (phase == ArrayCopyPhase::Parsing) {
    return false;
  }
  if (phase == ArrayCopyPhase::Optimization) {
    return is_clone_instance;
  }
  // else ArrayCopyPhase::Expansion
  return type == T_OBJECT || type == T_ARRAY;
}

// This TypeFunc assumes a 64bit system
static const TypeFunc* clone_type() {
  // Create input type (domain)
  const Type** domain_fields = TypeTuple::fields(4);
  domain_fields[TypeFunc::Parms + 0] = TypeInstPtr::NOTNULL;  // src
  domain_fields[TypeFunc::Parms + 1] = TypeInstPtr::NOTNULL;  // dst
  domain_fields[TypeFunc::Parms + 2] = TypeLong::LONG;        // size lower
  domain_fields[TypeFunc::Parms + 3] = Type::HALF;            // size upper
  const TypeTuple* domain = TypeTuple::make(TypeFunc::Parms + 4, domain_fields);

  // Create result type (range)
  const Type** range_fields = TypeTuple::fields(0);
  const TypeTuple* range = TypeTuple::make(TypeFunc::Parms + 0, range_fields);

  return TypeFunc::make(domain, range);
}

#define XTOP LP64_ONLY(COMMA phase->top())

void ZBarrierSetC2::clone_at_expansion(PhaseMacroExpand* phase, ArrayCopyNode* ac) const {
  Node* const src = ac->in(ArrayCopyNode::Src);
  const TypeAryPtr* ary_ptr = src->get_ptr_type()->isa_aryptr();

  if (ac->is_clone_array() && ary_ptr != NULL) {
    BasicType bt = ary_ptr->elem()->array_element_basic_type();
    if (is_reference_type(bt)) {
      // Clone object array
      bt = T_OBJECT;
    } else {
      // Clone primitive array
      bt = T_LONG;
    }

    Node* ctrl = ac->in(TypeFunc::Control);
    Node* mem = ac->in(TypeFunc::Memory);
    Node* src = ac->in(ArrayCopyNode::Src);
    Node* src_offset = ac->in(ArrayCopyNode::SrcPos);
    Node* dest = ac->in(ArrayCopyNode::Dest);
    Node* dest_offset = ac->in(ArrayCopyNode::DestPos);
    Node* length = ac->in(ArrayCopyNode::Length);

    if (bt == T_OBJECT) {
      // BarrierSetC2::clone sets the offsets via BarrierSetC2::arraycopy_payload_base_offset
      // which 8-byte aligns them to allow for word size copies. Make sure the offsets point
      // to the first element in the array when cloning object arrays. Otherwise, load
      // barriers are applied to parts of the header. Also adjust the length accordingly.
      assert(src_offset == dest_offset, "should be equal");
      jlong offset = src_offset->get_long();
      if (offset != arrayOopDesc::base_offset_in_bytes(T_OBJECT)) {
        assert(!UseCompressedClassPointers, "should only happen without compressed class pointers");
        assert((arrayOopDesc::base_offset_in_bytes(T_OBJECT) - offset) == BytesPerLong, "unexpected offset");
        length = phase->transform_later(new SubLNode(length, phase->longcon(1))); // Size is in longs
        src_offset = phase->longcon(arrayOopDesc::base_offset_in_bytes(T_OBJECT));
        dest_offset = src_offset;
      }
    }
    Node* payload_src = phase->basic_plus_adr(src, src_offset);
    Node* payload_dst = phase->basic_plus_adr(dest, dest_offset);

    const char* copyfunc_name = "arraycopy";
    address     copyfunc_addr = phase->basictype2arraycopy(bt, NULL, NULL, true, copyfunc_name, true);

    const TypePtr* raw_adr_type = TypeRawPtr::BOTTOM;
    const TypeFunc* call_type = OptoRuntime::fast_arraycopy_Type();

    Node* call = phase->make_leaf_call(ctrl, mem, call_type, copyfunc_addr, copyfunc_name, raw_adr_type, payload_src, payload_dst, length XTOP);
    phase->transform_later(call);

    phase->igvn().replace_node(ac, call);
    return;
  }

  // Clone instance
  Node* const ctrl       = ac->in(TypeFunc::Control);
  Node* const mem        = ac->in(TypeFunc::Memory);
  Node* const dst        = ac->in(ArrayCopyNode::Dest);
  Node* const size       = ac->in(ArrayCopyNode::Length);

  assert(size->bottom_type()->is_long(), "Should be long");

  // The native clone we are calling here expects the instance size in words
  // Add header/offset size to payload size to get instance size.
  Node* const base_offset = phase->longcon(arraycopy_payload_base_offset(ac->is_clone_array()) >> LogBytesPerLong);
  Node* const full_size = phase->transform_later(new AddLNode(size, base_offset));

  Node* const call = phase->make_leaf_call(ctrl,
                                           mem,
                                           clone_type(),
                                           ZBarrierSetRuntime::clone_addr(),
                                           "ZBarrierSetRuntime::clone",
                                           TypeRawPtr::BOTTOM,
                                           src,
                                           dst,
                                           full_size,
                                           phase->top());
  phase->transform_later(call);
  phase->igvn().replace_node(ac, call);
}

#undef XTOP

// == Dominating barrier elision ==

static bool block_has_safepoint(const Block* block, uint from, uint to) {
  for (uint i = from; i < to; i++) {
    if (block->get_node(i)->is_MachSafePoint()) {
      // Safepoint found
      return true;
    }
  }

  // Safepoint not found
  return false;
}

static bool block_has_safepoint(const Block* block) {
  return block_has_safepoint(block, 0, block->number_of_nodes());
}

static uint block_index(const Block* block, const Node* node) {
  for (uint j = 0; j < block->number_of_nodes(); ++j) {
    if (block->get_node(j) == node) {
      return j;
    }
  }
  ShouldNotReachHere();
  return 0;
}

void ZBarrierSetC2::analyze_dominating_barriers() const {
  ResourceMark rm;
  Compile* const C = Compile::current();
  PhaseCFG* const cfg = C->cfg();
  Block_List worklist;
  Node_List mem_ops;
  Node_List barrier_loads;

  // Step 1 - Find accesses, and track them in lists
  for (uint i = 0; i < cfg->number_of_blocks(); ++i) {
    const Block* const block = cfg->get_block(i);
    for (uint j = 0; j < block->number_of_nodes(); ++j) {
      const Node* const node = block->get_node(j);
      if (!node->is_Mach()) {
        continue;
      }

      MachNode* const mach = node->as_Mach();
      switch (mach->ideal_Opcode()) {
      case Op_LoadP:
        if ((mach->barrier_data() & ZLoadBarrierStrong) != 0) {
          barrier_loads.push(mach);
        }
        if ((mach->barrier_data() & (ZLoadBarrierStrong | ZLoadBarrierNoKeepalive)) ==
            ZLoadBarrierStrong) {
          mem_ops.push(mach);
        }
        break;
      case Op_CompareAndExchangeP:
      case Op_CompareAndSwapP:
      case Op_GetAndSetP:
        if ((mach->barrier_data() & ZLoadBarrierStrong) != 0) {
          barrier_loads.push(mach);
        }
      case Op_StoreP:
        mem_ops.push(mach);
        break;

      default:
        break;
      }
    }
  }

  // Step 2 - Find dominating accesses for each load
  for (uint i = 0; i < barrier_loads.size(); i++) {
    MachNode* const load = barrier_loads.at(i)->as_Mach();
    const TypePtr* load_adr_type = NULL;
    intptr_t load_offset = 0;
    const Node* const load_obj = load->get_base_and_disp(load_offset, load_adr_type);
    Block* const load_block = cfg->get_block_for_node(load);
    const uint load_index = block_index(load_block, load);

    for (uint j = 0; j < mem_ops.size(); j++) {
      MachNode* mem = mem_ops.at(j)->as_Mach();
      const TypePtr* mem_adr_type = NULL;
      intptr_t mem_offset = 0;
      const Node* mem_obj = mem->get_base_and_disp(mem_offset, mem_adr_type);
      Block* mem_block = cfg->get_block_for_node(mem);
      uint mem_index = block_index(mem_block, mem);

      if (load_obj == NodeSentinel || mem_obj == NodeSentinel ||
          load_obj == NULL || mem_obj == NULL ||
          load_offset < 0 || mem_offset < 0) {
        continue;
      }

      if (mem_obj != load_obj || mem_offset != load_offset) {
        // Not the same addresses, not a candidate
        continue;
      }

      if (load_block == mem_block) {
        // Earlier accesses in the same block
        if (mem_index < load_index && !block_has_safepoint(mem_block, mem_index + 1, load_index)) {
          load->set_barrier_data(ZLoadBarrierElided);
        }
      } else if (mem_block->dominates(load_block)) {
        // Dominating block? Look around for safepoints
        ResourceMark rm;
        Block_List stack;
        VectorSet visited;
        stack.push(load_block);
        bool safepoint_found = block_has_safepoint(load_block);
        while (!safepoint_found && stack.size() > 0) {
          Block* block = stack.pop();
          if (visited.test_set(block->_pre_order)) {
            continue;
          }
          if (block_has_safepoint(block)) {
            safepoint_found = true;
            break;
          }
          if (block == mem_block) {
            continue;
          }

          // Push predecessor blocks
          for (uint p = 1; p < block->num_preds(); ++p) {
            Block* pred = cfg->get_block_for_node(block->pred(p));
            stack.push(pred);
          }
        }

        if (!safepoint_found) {
          load->set_barrier_data(ZLoadBarrierElided);
        }
      }
    }
  }
}

// == Reduced spilling optimization ==

void ZBarrierSetC2::compute_liveness_at_stubs() const {
  ResourceMark rm;
  Compile* const C = Compile::current();
  Arena* const A = Thread::current()->resource_area();
  PhaseCFG* const cfg = C->cfg();
  PhaseRegAlloc* const regalloc = C->regalloc();
  RegMask* const live = NEW_ARENA_ARRAY(A, RegMask, cfg->number_of_blocks() * sizeof(RegMask));
  ZBarrierSetAssembler* const bs = ZBarrierSet::assembler();
  Block_List worklist;

  for (uint i = 0; i < cfg->number_of_blocks(); ++i) {
    new ((void*)(live + i)) RegMask();
    worklist.push(cfg->get_block(i));
  }

  while (worklist.size() > 0) {
    const Block* const block = worklist.pop();
    RegMask& old_live = live[block->_pre_order];
    RegMask new_live;

    // Initialize to union of successors
    for (uint i = 0; i < block->_num_succs; i++) {
      const uint succ_id = block->_succs[i]->_pre_order;
      new_live.OR(live[succ_id]);
    }

    // Walk block backwards, computing liveness
    for (int i = block->number_of_nodes() - 1; i >= 0; --i) {
      const Node* const node = block->get_node(i);

      // Remove def bits
      const OptoReg::Name first = bs->refine_register(node, regalloc->get_reg_first(node));
      const OptoReg::Name second = bs->refine_register(node, regalloc->get_reg_second(node));
      if (first != OptoReg::Bad) {
        new_live.Remove(first);
      }
      if (second != OptoReg::Bad) {
        new_live.Remove(second);
      }

      // Add use bits
      for (uint j = 1; j < node->req(); ++j) {
        const Node* const use = node->in(j);
        const OptoReg::Name first = bs->refine_register(use, regalloc->get_reg_first(use));
        const OptoReg::Name second = bs->refine_register(use, regalloc->get_reg_second(use));
        if (first != OptoReg::Bad) {
          new_live.Insert(first);
        }
        if (second != OptoReg::Bad) {
          new_live.Insert(second);
        }
      }

      // If this node tracks liveness, update it
      RegMask* const regs = barrier_set_state()->live(node);
      if (regs != NULL) {
        regs->OR(new_live);
      }
    }

    // Now at block top, see if we have any changes
    new_live.SUBTRACT(old_live);
    if (new_live.is_NotEmpty()) {
      // Liveness has refined, update and propagate to prior blocks
      old_live.OR(new_live);
      for (uint i = 1; i < block->num_preds(); ++i) {
        Block* const pred = cfg->get_block_for_node(block->pred(i));
        worklist.push(pred);
      }
    }
  }
}
