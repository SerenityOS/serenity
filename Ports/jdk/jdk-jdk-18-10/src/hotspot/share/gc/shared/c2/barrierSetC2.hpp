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

#ifndef SHARE_GC_SHARED_C2_BARRIERSETC2_HPP
#define SHARE_GC_SHARED_C2_BARRIERSETC2_HPP

#include "memory/allocation.hpp"
#include "oops/accessDecorators.hpp"
#include "opto/loopnode.hpp"
#include "opto/matcher.hpp"
#include "opto/memnode.hpp"
#include "utilities/globalDefinitions.hpp"

// This means the access is mismatched. This means the value of an access
// is not equivalent to the value pointed to by the address.
const DecoratorSet C2_MISMATCHED             = DECORATOR_LAST << 1;
// The access may not be aligned to its natural size.
const DecoratorSet C2_UNALIGNED              = DECORATOR_LAST << 2;
// The atomic cmpxchg is weak, meaning that spurious false negatives are allowed,
// but never false positives.
const DecoratorSet C2_WEAK_CMPXCHG           = DECORATOR_LAST << 3;
// This denotes that a load has control dependency.
const DecoratorSet C2_CONTROL_DEPENDENT_LOAD = DECORATOR_LAST << 4;
// This denotes that a load that must be pinned, but may float above safepoints.
const DecoratorSet C2_UNKNOWN_CONTROL_LOAD   = DECORATOR_LAST << 5;
// This denotes that the access is produced from the sun.misc.Unsafe intrinsics.
const DecoratorSet C2_UNSAFE_ACCESS          = DECORATOR_LAST << 6;
// This denotes that the access mutates state.
const DecoratorSet C2_WRITE_ACCESS           = DECORATOR_LAST << 7;
// This denotes that the access reads state.
const DecoratorSet C2_READ_ACCESS            = DECORATOR_LAST << 8;
// A nearby allocation?
const DecoratorSet C2_TIGHTLY_COUPLED_ALLOC  = DECORATOR_LAST << 9;
// Loads and stores from an arraycopy being optimized
const DecoratorSet C2_ARRAY_COPY             = DECORATOR_LAST << 10;
// Loads from immutable memory
const DecoratorSet C2_IMMUTABLE_MEMORY       = DECORATOR_LAST << 11;

class Compile;
class ConnectionGraph;
class GraphKit;
class IdealKit;
class Node;
class PhaseGVN;
class PhaseIdealLoop;
class PhaseMacroExpand;
class Type;
class TypePtr;
class Unique_Node_List;

// This class wraps a node and a type.
class C2AccessValue: public StackObj {
protected:
  Node* _node;
  const Type* _type;

public:
  C2AccessValue(Node* node, const Type* type) :
    _node(node),
    _type(type) {}

  Node* node() const        { return _node; }
  const Type* type() const  { return _type; }

  void set_node(Node* node) { _node = node; }
};

// This class wraps a node and a pointer type.
class C2AccessValuePtr: public C2AccessValue {

public:
  C2AccessValuePtr(Node* node, const TypePtr* type) :
    C2AccessValue(node, reinterpret_cast<const Type*>(type)) {}

  const TypePtr* type() const { return reinterpret_cast<const TypePtr*>(_type); }
};

// This class wraps a bunch of context parameters thare are passed around in the
// BarrierSetC2 backend hierarchy, for loads and stores, to reduce boiler plate.
class C2Access: public StackObj {
protected:
  DecoratorSet      _decorators;
  BasicType         _type;
  Node*             _base;
  C2AccessValuePtr& _addr;
  Node*             _raw_access;
  uint8_t           _barrier_data;

  void fixup_decorators();

public:
  C2Access(DecoratorSet decorators,
           BasicType type, Node* base, C2AccessValuePtr& addr) :
    _decorators(decorators),
    _type(type),
    _base(base),
    _addr(addr),
    _raw_access(NULL),
    _barrier_data(0)
  {}

  DecoratorSet decorators() const { return _decorators; }
  Node* base() const              { return _base; }
  C2AccessValuePtr& addr() const  { return _addr; }
  BasicType type() const          { return _type; }
  bool is_oop() const             { return is_reference_type(_type); }
  bool is_raw() const             { return (_decorators & AS_RAW) != 0; }
  Node* raw_access() const        { return _raw_access; }

  uint8_t barrier_data() const        { return _barrier_data; }
  void set_barrier_data(uint8_t data) { _barrier_data = data; }

  void set_raw_access(Node* raw_access) { _raw_access = raw_access; }
  virtual void set_memory() {} // no-op for normal accesses, but not for atomic accesses.

  MemNode::MemOrd mem_node_mo() const;
  bool needs_cpu_membar() const;

  virtual PhaseGVN& gvn() const = 0;
  virtual bool is_parse_access() const { return false; }
  virtual bool is_opt_access() const { return false; }
};

// C2Access for parse time calls to the BarrierSetC2 backend.
class C2ParseAccess: public C2Access {
protected:
  GraphKit*         _kit;

  void* barrier_set_state() const;

public:
  C2ParseAccess(GraphKit* kit, DecoratorSet decorators,
                BasicType type, Node* base, C2AccessValuePtr& addr) :
    C2Access(decorators, type, base, addr),
    _kit(kit) {
    fixup_decorators();
  }

  GraphKit* kit() const           { return _kit; }

  virtual PhaseGVN& gvn() const;
  virtual bool is_parse_access() const { return true; }
};

// This class wraps a bunch of context parameters thare are passed around in the
// BarrierSetC2 backend hierarchy, for atomic accesses, to reduce boiler plate.
class C2AtomicParseAccess: public C2ParseAccess {
  Node* _memory;
  uint  _alias_idx;
  bool  _needs_pinning;

public:
  C2AtomicParseAccess(GraphKit* kit, DecoratorSet decorators, BasicType type,
                 Node* base, C2AccessValuePtr& addr, uint alias_idx) :
    C2ParseAccess(kit, decorators, type, base, addr),
    _memory(NULL),
    _alias_idx(alias_idx),
    _needs_pinning(true) {}

  // Set the memory node based on the current memory slice.
  virtual void set_memory();

  Node* memory() const       { return _memory; }
  uint alias_idx() const     { return _alias_idx; }
  bool needs_pinning() const { return _needs_pinning; }
};

// C2Access for optimization time calls to the BarrierSetC2 backend.
class C2OptAccess: public C2Access {
  PhaseGVN& _gvn;
  MergeMemNode* _mem;
  Node* _ctl;

public:
  C2OptAccess(PhaseGVN& gvn, Node* ctl, MergeMemNode* mem, DecoratorSet decorators,
              BasicType type, Node* base, C2AccessValuePtr& addr) :
    C2Access(decorators, type, base, addr),
    _gvn(gvn), _mem(mem), _ctl(ctl) {
    fixup_decorators();
  }

  MergeMemNode* mem() const { return _mem; }
  Node* ctl() const { return _ctl; }

  virtual PhaseGVN& gvn() const { return _gvn; }
  virtual bool is_opt_access() const { return true; }
};


// This is the top-level class for the backend of the Access API in C2.
// The top-level class is responsible for performing raw accesses. The
// various GC barrier sets inherit from the BarrierSetC2 class to sprinkle
// barriers into the accesses.
class BarrierSetC2: public CHeapObj<mtGC> {
protected:
  virtual void resolve_address(C2Access& access) const;
  virtual Node* store_at_resolved(C2Access& access, C2AccessValue& val) const;
  virtual Node* load_at_resolved(C2Access& access, const Type* val_type) const;

  virtual Node* atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                               Node* new_val, const Type* val_type) const;
  virtual Node* atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                Node* new_val, const Type* value_type) const;
  virtual Node* atomic_xchg_at_resolved(C2AtomicParseAccess& access, Node* new_val, const Type* val_type) const;
  virtual Node* atomic_add_at_resolved(C2AtomicParseAccess& access, Node* new_val, const Type* val_type) const;
  void pin_atomic_op(C2AtomicParseAccess& access) const;

public:
  // This is the entry-point for the backend to perform accesses through the Access API.
  virtual Node* store_at(C2Access& access, C2AccessValue& val) const;
  virtual Node* load_at(C2Access& access, const Type* val_type) const;

  virtual Node* atomic_cmpxchg_val_at(C2AtomicParseAccess& access, Node* expected_val,
                                      Node* new_val, const Type* val_type) const;
  virtual Node* atomic_cmpxchg_bool_at(C2AtomicParseAccess& access, Node* expected_val,
                                       Node* new_val, const Type* val_type) const;
  virtual Node* atomic_xchg_at(C2AtomicParseAccess& access, Node* new_val, const Type* value_type) const;
  virtual Node* atomic_add_at(C2AtomicParseAccess& access, Node* new_val, const Type* value_type) const;

  virtual void clone(GraphKit* kit, Node* src, Node* dst, Node* size, bool is_array) const;

  virtual Node* obj_allocate(PhaseMacroExpand* macro, Node* mem, Node* toobig_false, Node* size_in_bytes,
                             Node*& i_o, Node*& needgc_ctrl,
                             Node*& fast_oop_ctrl, Node*& fast_oop_rawmem,
                             intx prefetch_lines) const;

  virtual Node* ideal_node(PhaseGVN* phase, Node* n, bool can_reshape) const { return NULL; }

  // These are general helper methods used by C2
  enum ArrayCopyPhase {
    Parsing,
    Optimization,
    Expansion
  };

  virtual bool array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type, bool is_clone, bool is_clone_instance, ArrayCopyPhase phase) const { return false; }
  virtual void clone_at_expansion(PhaseMacroExpand* phase, ArrayCopyNode* ac) const;

  // Support for GC barriers emitted during parsing
  virtual bool has_load_barrier_nodes() const { return false; }
  virtual bool is_gc_barrier_node(Node* node) const { return false; }
  virtual Node* step_over_gc_barrier(Node* c) const { return c; }

  // Support for macro expanded GC barriers
  virtual void register_potential_barrier_node(Node* node) const { }
  virtual void unregister_potential_barrier_node(Node* node) const { }
  virtual void eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const { }
  virtual void enqueue_useful_gc_barrier(PhaseIterGVN* igvn, Node* node) const {}
  virtual void eliminate_useless_gc_barriers(Unique_Node_List &useful, Compile* C) const {}

  // Allow barrier sets to have shared state that is preserved across a compilation unit.
  // This could for example comprise macro nodes to be expanded during macro expansion.
  virtual void* create_barrier_state(Arena* comp_arena) const { return NULL; }
  // If the BarrierSetC2 state has barrier nodes in its compilation
  // unit state to be expanded later, then now is the time to do so.
  virtual bool expand_barriers(Compile* C, PhaseIterGVN& igvn) const { return false; }
  virtual bool optimize_loops(PhaseIdealLoop* phase, LoopOptsMode mode, VectorSet& visited, Node_Stack& nstack, Node_List& worklist) const { return false; }
  virtual bool strip_mined_loops_expanded(LoopOptsMode mode) const { return false; }
  virtual bool is_gc_specific_loop_opts_pass(LoopOptsMode mode) const { return false; }

  enum CompilePhase {
    BeforeOptimize,
    BeforeMacroExpand,
    BeforeCodeGen
  };

#ifdef ASSERT
  virtual void verify_gc_barriers(Compile* compile, CompilePhase phase) const {}
#endif

  virtual bool final_graph_reshaping(Compile* compile, Node* n, uint opcode) const { return false; }

  virtual bool escape_add_to_con_graph(ConnectionGraph* conn_graph, PhaseGVN* gvn, Unique_Node_List* delayed_worklist, Node* n, uint opcode) const { return false; }
  virtual bool escape_add_final_edges(ConnectionGraph* conn_graph, PhaseGVN* gvn, Node* n, uint opcode) const { return false; }
  virtual bool escape_has_out_with_unsafe_object(Node* n) const { return false; }

  virtual bool matcher_find_shared_post_visit(Matcher* matcher, Node* n, uint opcode) const { return false; };
  virtual bool matcher_is_store_load_barrier(Node* x, uint xop) const { return false; }

  virtual void late_barrier_analysis() const { }
  virtual int estimate_stub_size() const { return 0; }
  virtual void emit_stubs(CodeBuffer& cb) const { }

  static int arraycopy_payload_base_offset(bool is_array);
};

#endif // SHARE_GC_SHARED_C2_BARRIERSETC2_HPP
