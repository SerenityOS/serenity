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

#ifndef SHARE_OPTO_CALLNODE_HPP
#define SHARE_OPTO_CALLNODE_HPP

#include "opto/connode.hpp"
#include "opto/mulnode.hpp"
#include "opto/multnode.hpp"
#include "opto/opcodes.hpp"
#include "opto/phaseX.hpp"
#include "opto/replacednodes.hpp"
#include "opto/type.hpp"
#include "utilities/growableArray.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

class NamedCounter;
class MultiNode;
class  SafePointNode;
class   CallNode;
class     CallJavaNode;
class       CallStaticJavaNode;
class       CallDynamicJavaNode;
class     CallRuntimeNode;
class       CallLeafNode;
class         CallLeafNoFPNode;
class         CallLeafVectorNode;
class     CallNativeNode;
class     AllocateNode;
class       AllocateArrayNode;
class     AbstractLockNode;
class       LockNode;
class       UnlockNode;
class FastLockNode;

//------------------------------StartNode--------------------------------------
// The method start node
class StartNode : public MultiNode {
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger
public:
  const TypeTuple *_domain;
  StartNode( Node *root, const TypeTuple *domain ) : MultiNode(2), _domain(domain) {
    init_class_id(Class_Start);
    init_req(0,this);
    init_req(1,root);
  }
  virtual int Opcode() const;
  virtual bool pinned() const { return true; };
  virtual const Type *bottom_type() const;
  virtual const TypePtr *adr_type() const { return TypePtr::BOTTOM; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual void  calling_convention( BasicType* sig_bt, VMRegPair *parm_reg, uint length ) const;
  virtual const RegMask &in_RegMask(uint) const;
  virtual Node *match( const ProjNode *proj, const Matcher *m );
  virtual uint ideal_reg() const { return 0; }
#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
  virtual void  dump_compact_spec(outputStream *st) const;
#endif
};

//------------------------------StartOSRNode-----------------------------------
// The method start node for on stack replacement code
class StartOSRNode : public StartNode {
public:
  StartOSRNode( Node *root, const TypeTuple *domain ) : StartNode(root, domain) {}
  virtual int   Opcode() const;
  static  const TypeTuple *osr_domain();
};


//------------------------------ParmNode---------------------------------------
// Incoming parameters
class ParmNode : public ProjNode {
  static const char * const names[TypeFunc::Parms+1];
public:
  ParmNode( StartNode *src, uint con ) : ProjNode(src,con) {
    init_class_id(Class_Parm);
  }
  virtual int Opcode() const;
  virtual bool  is_CFG() const { return (_con == TypeFunc::Control); }
  virtual uint ideal_reg() const;
#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
  virtual void dump_compact_spec(outputStream *st) const;
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};


//------------------------------ReturnNode-------------------------------------
// Return from subroutine node
class ReturnNode : public Node {
public:
  ReturnNode( uint edges, Node *cntrl, Node *i_o, Node *memory, Node *retadr, Node *frameptr );
  virtual int Opcode() const;
  virtual bool  is_CFG() const { return true; }
  virtual uint hash() const { return NO_HASH; }  // CFG nodes do not hash
  virtual bool depends_only_on_test() const { return false; }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual uint ideal_reg() const { return NotAMachineReg; }
  virtual uint match_edge(uint idx) const;
#ifndef PRODUCT
  virtual void dump_req(outputStream *st = tty) const;
#endif
};


//------------------------------RethrowNode------------------------------------
// Rethrow of exception at call site.  Ends a procedure before rethrowing;
// ends the current basic block like a ReturnNode.  Restores registers and
// unwinds stack.  Rethrow happens in the caller's method.
class RethrowNode : public Node {
 public:
  RethrowNode( Node *cntrl, Node *i_o, Node *memory, Node *frameptr, Node *ret_adr, Node *exception );
  virtual int Opcode() const;
  virtual bool  is_CFG() const { return true; }
  virtual uint hash() const { return NO_HASH; }  // CFG nodes do not hash
  virtual bool depends_only_on_test() const { return false; }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual uint match_edge(uint idx) const;
  virtual uint ideal_reg() const { return NotAMachineReg; }
#ifndef PRODUCT
  virtual void dump_req(outputStream *st = tty) const;
#endif
};


//------------------------------TailCallNode-----------------------------------
// Pop stack frame and jump indirect
class TailCallNode : public ReturnNode {
public:
  TailCallNode( Node *cntrl, Node *i_o, Node *memory, Node *frameptr, Node *retadr, Node *target, Node *moop )
    : ReturnNode( TypeFunc::Parms+2, cntrl, i_o, memory, frameptr, retadr ) {
    init_req(TypeFunc::Parms, target);
    init_req(TypeFunc::Parms+1, moop);
  }

  virtual int Opcode() const;
  virtual uint match_edge(uint idx) const;
};

//------------------------------TailJumpNode-----------------------------------
// Pop stack frame and jump indirect
class TailJumpNode : public ReturnNode {
public:
  TailJumpNode( Node *cntrl, Node *i_o, Node *memory, Node *frameptr, Node *target, Node *ex_oop)
    : ReturnNode(TypeFunc::Parms+2, cntrl, i_o, memory, frameptr, Compile::current()->top()) {
    init_req(TypeFunc::Parms, target);
    init_req(TypeFunc::Parms+1, ex_oop);
  }

  virtual int Opcode() const;
  virtual uint match_edge(uint idx) const;
};

//-------------------------------JVMState-------------------------------------
// A linked list of JVMState nodes captures the whole interpreter state,
// plus GC roots, for all active calls at some call site in this compilation
// unit.  (If there is no inlining, then the list has exactly one link.)
// This provides a way to map the optimized program back into the interpreter,
// or to let the GC mark the stack.
class JVMState : public ResourceObj {
  friend class VMStructs;
public:
  typedef enum {
    Reexecute_Undefined = -1, // not defined -- will be translated into false later
    Reexecute_False     =  0, // false       -- do not reexecute
    Reexecute_True      =  1  // true        -- reexecute the bytecode
  } ReexecuteState; //Reexecute State

private:
  JVMState*         _caller;    // List pointer for forming scope chains
  uint              _depth;     // One more than caller depth, or one.
  uint              _locoff;    // Offset to locals in input edge mapping
  uint              _stkoff;    // Offset to stack in input edge mapping
  uint              _monoff;    // Offset to monitors in input edge mapping
  uint              _scloff;    // Offset to fields of scalar objs in input edge mapping
  uint              _endoff;    // Offset to end of input edge mapping
  uint              _sp;        // Jave Expression Stack Pointer for this state
  int               _bci;       // Byte Code Index of this JVM point
  ReexecuteState    _reexecute; // Whether this bytecode need to be re-executed
  ciMethod*         _method;    // Method Pointer
  SafePointNode*    _map;       // Map node associated with this scope
public:
  friend class Compile;
  friend class PreserveReexecuteState;

  // Because JVMState objects live over the entire lifetime of the
  // Compile object, they are allocated into the comp_arena, which
  // does not get resource marked or reset during the compile process
  void *operator new( size_t x, Compile* C ) throw() { return C->comp_arena()->Amalloc(x); }
  void operator delete( void * ) { } // fast deallocation

  // Create a new JVMState, ready for abstract interpretation.
  JVMState(ciMethod* method, JVMState* caller);
  JVMState(int stack_size);  // root state; has a null method

  // Access functions for the JVM
  // ... --|--- loc ---|--- stk ---|--- arg ---|--- mon ---|--- scl ---|
  //       \ locoff    \ stkoff    \ argoff    \ monoff    \ scloff    \ endoff
  uint              locoff() const { return _locoff; }
  uint              stkoff() const { return _stkoff; }
  uint              argoff() const { return _stkoff + _sp; }
  uint              monoff() const { return _monoff; }
  uint              scloff() const { return _scloff; }
  uint              endoff() const { return _endoff; }
  uint              oopoff() const { return debug_end(); }

  int            loc_size() const { return stkoff() - locoff(); }
  int            stk_size() const { return monoff() - stkoff(); }
  int            mon_size() const { return scloff() - monoff(); }
  int            scl_size() const { return endoff() - scloff(); }

  bool        is_loc(uint i) const { return locoff() <= i && i < stkoff(); }
  bool        is_stk(uint i) const { return stkoff() <= i && i < monoff(); }
  bool        is_mon(uint i) const { return monoff() <= i && i < scloff(); }
  bool        is_scl(uint i) const { return scloff() <= i && i < endoff(); }

  uint                      sp() const { return _sp; }
  int                      bci() const { return _bci; }
  bool        should_reexecute() const { return _reexecute==Reexecute_True; }
  bool  is_reexecute_undefined() const { return _reexecute==Reexecute_Undefined; }
  bool              has_method() const { return _method != NULL; }
  ciMethod*             method() const { assert(has_method(), ""); return _method; }
  JVMState*             caller() const { return _caller; }
  SafePointNode*           map() const { return _map; }
  uint                   depth() const { return _depth; }
  uint             debug_start() const; // returns locoff of root caller
  uint               debug_end() const; // returns endoff of self
  uint              debug_size() const {
    return loc_size() + sp() + mon_size() + scl_size();
  }
  uint        debug_depth()  const; // returns sum of debug_size values at all depths

  // Returns the JVM state at the desired depth (1 == root).
  JVMState* of_depth(int d) const;

  // Tells if two JVM states have the same call chain (depth, methods, & bcis).
  bool same_calls_as(const JVMState* that) const;

  // Monitors (monitors are stored as (boxNode, objNode) pairs
  enum { logMonitorEdges = 1 };
  int  nof_monitors()              const { return mon_size() >> logMonitorEdges; }
  int  monitor_depth()             const { return nof_monitors() + (caller() ? caller()->monitor_depth() : 0); }
  int  monitor_box_offset(int idx) const { return monoff() + (idx << logMonitorEdges) + 0; }
  int  monitor_obj_offset(int idx) const { return monoff() + (idx << logMonitorEdges) + 1; }
  bool is_monitor_box(uint off)    const {
    assert(is_mon(off), "should be called only for monitor edge");
    return (0 == bitfield(off - monoff(), 0, logMonitorEdges));
  }
  bool is_monitor_use(uint off)    const { return (is_mon(off)
                                                   && is_monitor_box(off))
                                             || (caller() && caller()->is_monitor_use(off)); }

  // Initialization functions for the JVM
  void              set_locoff(uint off) { _locoff = off; }
  void              set_stkoff(uint off) { _stkoff = off; }
  void              set_monoff(uint off) { _monoff = off; }
  void              set_scloff(uint off) { _scloff = off; }
  void              set_endoff(uint off) { _endoff = off; }
  void              set_offsets(uint off) {
    _locoff = _stkoff = _monoff = _scloff = _endoff = off;
  }
  void              set_map(SafePointNode* map) { _map = map; }
  void              bind_map(SafePointNode* map); // set_map() and set_jvms() for the SafePointNode
  void              set_sp(uint sp) { _sp = sp; }
                    // _reexecute is initialized to "undefined" for a new bci
  void              set_bci(int bci) {if(_bci != bci)_reexecute=Reexecute_Undefined; _bci = bci; }
  void              set_should_reexecute(bool reexec) {_reexecute = reexec ? Reexecute_True : Reexecute_False;}

  // Miscellaneous utility functions
  JVMState* clone_deep(Compile* C) const;    // recursively clones caller chain
  JVMState* clone_shallow(Compile* C) const; // retains uncloned caller
  void      set_map_deep(SafePointNode *map);// reset map for all callers
  void      adapt_position(int delta);       // Adapt offsets in in-array after adding an edge.
  int       interpreter_frame_size() const;

#ifndef PRODUCT
  void      print_method_with_lineno(outputStream* st, bool show_name) const;
  void      format(PhaseRegAlloc *regalloc, const Node *n, outputStream* st) const;
  void      dump_spec(outputStream *st) const;
  void      dump_on(outputStream* st) const;
  void      dump() const {
    dump_on(tty);
  }
#endif
};

//------------------------------SafePointNode----------------------------------
// A SafePointNode is a subclass of a MultiNode for convenience (and
// potential code sharing) only - conceptually it is independent of
// the Node semantics.
class SafePointNode : public MultiNode {
  friend JVMState;
  friend class GraphKit;
  friend class VMStructs;

  virtual bool           cmp( const Node &n ) const;
  virtual uint           size_of() const;       // Size is bigger

protected:
  JVMState* const _jvms;      // Pointer to list of JVM State objects
  // Many calls take *all* of memory as input,
  // but some produce a limited subset of that memory as output.
  // The adr_type reports the call's behavior as a store, not a load.
  const TypePtr*  _adr_type;  // What type of memory does this node produce?
  ReplacedNodes   _replaced_nodes; // During parsing: list of pair of nodes from calls to GraphKit::replace_in_map()
  bool            _has_ea_local_in_scope; // NoEscape or ArgEscape objects in JVM States

  void set_jvms(JVMState* s) {
  assert(s != nullptr, "assign NULL value to _jvms");
    *(JVMState**)&_jvms = s;  // override const attribute in the accessor
  }
public:
  SafePointNode(uint edges, JVMState* jvms,
                // A plain safepoint advertises no memory effects (NULL):
                const TypePtr* adr_type = NULL)
    : MultiNode( edges ),
      _jvms(jvms),
      _adr_type(adr_type),
      _has_ea_local_in_scope(false)
  {
    init_class_id(Class_SafePoint);
  }

  JVMState* jvms() const { return _jvms; }
  virtual bool needs_deep_clone_jvms(Compile* C) { return false; }
  void clone_jvms(Compile* C) {
    if (jvms() != NULL) {
      if (needs_deep_clone_jvms(C)) {
        set_jvms(jvms()->clone_deep(C));
        jvms()->set_map_deep(this);
      } else {
        jvms()->clone_shallow(C)->bind_map(this);
      }
    }
  }

 private:
  void verify_input(JVMState* jvms, uint idx) const {
    assert(verify_jvms(jvms), "jvms must match");
    Node* n = in(idx);
    assert((!n->bottom_type()->isa_long() && !n->bottom_type()->isa_double()) ||
           in(idx + 1)->is_top(), "2nd half of long/double");
  }

 public:
  // Functionality from old debug nodes which has changed
  Node *local(JVMState* jvms, uint idx) const {
    verify_input(jvms, jvms->locoff() + idx);
    return in(jvms->locoff() + idx);
  }
  Node *stack(JVMState* jvms, uint idx) const {
    verify_input(jvms, jvms->stkoff() + idx);
    return in(jvms->stkoff() + idx);
  }
  Node *argument(JVMState* jvms, uint idx) const {
    verify_input(jvms, jvms->argoff() + idx);
    return in(jvms->argoff() + idx);
  }
  Node *monitor_box(JVMState* jvms, uint idx) const {
    assert(verify_jvms(jvms), "jvms must match");
    return in(jvms->monitor_box_offset(idx));
  }
  Node *monitor_obj(JVMState* jvms, uint idx) const {
    assert(verify_jvms(jvms), "jvms must match");
    return in(jvms->monitor_obj_offset(idx));
  }

  void  set_local(JVMState* jvms, uint idx, Node *c);

  void  set_stack(JVMState* jvms, uint idx, Node *c) {
    assert(verify_jvms(jvms), "jvms must match");
    set_req(jvms->stkoff() + idx, c);
  }
  void  set_argument(JVMState* jvms, uint idx, Node *c) {
    assert(verify_jvms(jvms), "jvms must match");
    set_req(jvms->argoff() + idx, c);
  }
  void ensure_stack(JVMState* jvms, uint stk_size) {
    assert(verify_jvms(jvms), "jvms must match");
    int grow_by = (int)stk_size - (int)jvms->stk_size();
    if (grow_by > 0)  grow_stack(jvms, grow_by);
  }
  void grow_stack(JVMState* jvms, uint grow_by);
  // Handle monitor stack
  void push_monitor( const FastLockNode *lock );
  void pop_monitor ();
  Node *peek_monitor_box() const;
  Node *peek_monitor_obj() const;

  // Access functions for the JVM
  Node *control  () const { return in(TypeFunc::Control  ); }
  Node *i_o      () const { return in(TypeFunc::I_O      ); }
  Node *memory   () const { return in(TypeFunc::Memory   ); }
  Node *returnadr() const { return in(TypeFunc::ReturnAdr); }
  Node *frameptr () const { return in(TypeFunc::FramePtr ); }

  void set_control  ( Node *c ) { set_req(TypeFunc::Control,c); }
  void set_i_o      ( Node *c ) { set_req(TypeFunc::I_O    ,c); }
  void set_memory   ( Node *c ) { set_req(TypeFunc::Memory ,c); }

  MergeMemNode* merged_memory() const {
    return in(TypeFunc::Memory)->as_MergeMem();
  }

  // The parser marks useless maps as dead when it's done with them:
  bool is_killed() { return in(TypeFunc::Control) == NULL; }

  // Exception states bubbling out of subgraphs such as inlined calls
  // are recorded here.  (There might be more than one, hence the "next".)
  // This feature is used only for safepoints which serve as "maps"
  // for JVM states during parsing, intrinsic expansion, etc.
  SafePointNode*         next_exception() const;
  void               set_next_exception(SafePointNode* n);
  bool                   has_exceptions() const { return next_exception() != NULL; }

  // Helper methods to operate on replaced nodes
  ReplacedNodes replaced_nodes() const {
    return _replaced_nodes;
  }

  void set_replaced_nodes(ReplacedNodes replaced_nodes) {
    _replaced_nodes = replaced_nodes;
  }

  void clone_replaced_nodes() {
    _replaced_nodes.clone();
  }
  void record_replaced_node(Node* initial, Node* improved) {
    _replaced_nodes.record(initial, improved);
  }
  void transfer_replaced_nodes_from(SafePointNode* sfpt, uint idx = 0) {
    _replaced_nodes.transfer_from(sfpt->_replaced_nodes, idx);
  }
  void delete_replaced_nodes() {
    _replaced_nodes.reset();
  }
  void apply_replaced_nodes(uint idx) {
    _replaced_nodes.apply(this, idx);
  }
  void merge_replaced_nodes_with(SafePointNode* sfpt) {
    _replaced_nodes.merge_with(sfpt->_replaced_nodes);
  }
  bool has_replaced_nodes() const {
    return !_replaced_nodes.is_empty();
  }
  void set_has_ea_local_in_scope(bool b) {
    _has_ea_local_in_scope = b;
  }
  bool has_ea_local_in_scope() const {
    return _has_ea_local_in_scope;
  }

  void disconnect_from_root(PhaseIterGVN *igvn);

  // Standard Node stuff
  virtual int            Opcode() const;
  virtual bool           pinned() const { return true; }
  virtual const Type*    Value(PhaseGVN* phase) const;
  virtual const Type*    bottom_type() const { return Type::CONTROL; }
  virtual const TypePtr* adr_type() const { return _adr_type; }
  void set_adr_type(const TypePtr* adr_type) { _adr_type = adr_type; }
  virtual Node          *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual Node*          Identity(PhaseGVN* phase);
  virtual uint           ideal_reg() const { return 0; }
  virtual const RegMask &in_RegMask(uint) const;
  virtual const RegMask &out_RegMask() const;
  virtual uint           match_edge(uint idx) const;

#ifndef PRODUCT
  virtual void           dump_spec(outputStream *st) const;
  virtual void           related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

//------------------------------SafePointScalarObjectNode----------------------
// A SafePointScalarObjectNode represents the state of a scalarized object
// at a safepoint.

class SafePointScalarObjectNode: public TypeNode {
  uint _first_index; // First input edge relative index of a SafePoint node where
                     // states of the scalarized object fields are collected.
                     // It is relative to the last (youngest) jvms->_scloff.
  uint _n_fields;    // Number of non-static fields of the scalarized object.
  bool _is_auto_box; // True if the scalarized object is an auto box.
  DEBUG_ONLY(Node* _alloc;)

  virtual uint hash() const ; // { return NO_HASH; }
  virtual bool cmp( const Node &n ) const;

  uint first_index() const { return _first_index; }

public:
  SafePointScalarObjectNode(const TypeOopPtr* tp,
#ifdef ASSERT
                            Node* alloc,
#endif
                            uint first_index, uint n_fields, bool is_auto_box = false);
  virtual int Opcode() const;
  virtual uint           ideal_reg() const;
  virtual const RegMask &in_RegMask(uint) const;
  virtual const RegMask &out_RegMask() const;
  virtual uint           match_edge(uint idx) const;

  uint first_index(JVMState* jvms) const {
    assert(jvms != NULL, "missed JVMS");
    return jvms->scloff() + _first_index;
  }
  uint n_fields()    const { return _n_fields; }

  bool is_auto_box() const { return _is_auto_box; }
#ifdef ASSERT
  Node* alloc() const { return _alloc; }
#endif

  virtual uint size_of() const { return sizeof(*this); }

  // Assumes that "this" is an argument to a safepoint node "s", and that
  // "new_call" is being created to correspond to "s".  But the difference
  // between the start index of the jvmstates of "new_call" and "s" is
  // "jvms_adj".  Produce and return a SafePointScalarObjectNode that
  // corresponds appropriately to "this" in "new_call".  Assumes that
  // "sosn_map" is a map, specific to the translation of "s" to "new_call",
  // mapping old SafePointScalarObjectNodes to new, to avoid multiple copies.
  SafePointScalarObjectNode* clone(Dict* sosn_map, bool& new_node) const;

#ifndef PRODUCT
  virtual void              dump_spec(outputStream *st) const;
#endif
};


// Simple container for the outgoing projections of a call.  Useful
// for serious surgery on calls.
class CallProjections : public StackObj {
public:
  Node* fallthrough_proj;
  Node* fallthrough_catchproj;
  Node* fallthrough_memproj;
  Node* fallthrough_ioproj;
  Node* catchall_catchproj;
  Node* catchall_memproj;
  Node* catchall_ioproj;
  Node* resproj;
  Node* exobj;
};

class CallGenerator;

//------------------------------CallNode---------------------------------------
// Call nodes now subsume the function of debug nodes at callsites, so they
// contain the functionality of a full scope chain of debug nodes.
class CallNode : public SafePointNode {
  friend class VMStructs;

protected:
  bool may_modify_arraycopy_helper(const TypeOopPtr* dest_t, const TypeOopPtr* t_oop, PhaseTransform* phase);

public:
  const TypeFunc* _tf;          // Function type
  address         _entry_point; // Address of method being called
  float           _cnt;         // Estimate of number of times called
  CallGenerator*  _generator;   // corresponding CallGenerator for some late inline calls
  const char*     _name;        // Printable name, if _method is NULL

  CallNode(const TypeFunc* tf, address addr, const TypePtr* adr_type, JVMState* jvms = nullptr)
    : SafePointNode(tf->domain()->cnt(), jvms, adr_type),
      _tf(tf),
      _entry_point(addr),
      _cnt(COUNT_UNKNOWN),
      _generator(NULL),
      _name(NULL)
  {
    init_class_id(Class_Call);
  }

  const TypeFunc* tf()         const { return _tf; }
  const address  entry_point() const { return _entry_point; }
  const float    cnt()         const { return _cnt; }
  CallGenerator* generator()   const { return _generator; }

  void set_tf(const TypeFunc* tf)       { _tf = tf; }
  void set_entry_point(address p)       { _entry_point = p; }
  void set_cnt(float c)                 { _cnt = c; }
  void set_generator(CallGenerator* cg) { _generator = cg; }

  virtual const Type* bottom_type() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase) { return this; }
  virtual bool        cmp(const Node &n) const;
  virtual uint        size_of() const = 0;
  virtual void        calling_convention(BasicType* sig_bt, VMRegPair* parm_regs, uint argcnt) const;
  virtual Node*       match(const ProjNode* proj, const Matcher* m);
  virtual uint        ideal_reg() const { return NotAMachineReg; }
  // Are we guaranteed that this node is a safepoint?  Not true for leaf calls and
  // for some macro nodes whose expansion does not have a safepoint on the fast path.
  virtual bool        guaranteed_safepoint()  { return true; }
  // For macro nodes, the JVMState gets modified during expansion. If calls
  // use MachConstantBase, it gets modified during matching. So when cloning
  // the node the JVMState must be deep cloned. Default is to shallow clone.
  virtual bool needs_deep_clone_jvms(Compile* C) { return C->needs_deep_clone_jvms(); }

  // Returns true if the call may modify n
  virtual bool        may_modify(const TypeOopPtr* t_oop, PhaseTransform* phase);
  // Does this node have a use of n other than in debug information?
  bool                has_non_debug_use(Node* n);
  // Returns the unique CheckCastPP of a call
  // or result projection is there are several CheckCastPP
  // or returns NULL if there is no one.
  Node* result_cast();
  // Does this node returns pointer?
  bool returns_pointer() const {
    const TypeTuple* r = tf()->range();
    return (r->cnt() > TypeFunc::Parms &&
            r->field_at(TypeFunc::Parms)->isa_ptr());
  }

  // Collect all the interesting edges from a call for use in
  // replacing the call by something else.  Used by macro expansion
  // and the late inlining support.
  void extract_projections(CallProjections* projs, bool separate_io_proj, bool do_asserts = true);

  virtual uint match_edge(uint idx) const;

  bool is_call_to_arraycopystub() const;

  virtual void copy_call_debug_info(PhaseIterGVN* phase, SafePointNode* sfpt) {}

#ifndef PRODUCT
  virtual void        dump_req(outputStream* st = tty) const;
  virtual void        dump_spec(outputStream* st) const;
#endif
};


//------------------------------CallJavaNode-----------------------------------
// Make a static or dynamic subroutine call node using Java calling
// convention.  (The "Java" calling convention is the compiler's calling
// convention, as opposed to the interpreter's or that of native C.)
class CallJavaNode : public CallNode {
  friend class VMStructs;
protected:
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger

  bool    _optimized_virtual;
  bool    _method_handle_invoke;
  bool    _override_symbolic_info; // Override symbolic call site info from bytecode
  ciMethod* _method;               // Method being direct called
  bool    _arg_escape;             // ArgEscape in parameter list
public:
  CallJavaNode(const TypeFunc* tf , address addr, ciMethod* method)
    : CallNode(tf, addr, TypePtr::BOTTOM),
      _optimized_virtual(false),
      _method_handle_invoke(false),
      _override_symbolic_info(false),
      _method(method),
      _arg_escape(false)
  {
    init_class_id(Class_CallJava);
  }

  virtual int   Opcode() const;
  ciMethod* method() const                 { return _method; }
  void  set_method(ciMethod *m)            { _method = m; }
  void  set_optimized_virtual(bool f)      { _optimized_virtual = f; }
  bool  is_optimized_virtual() const       { return _optimized_virtual; }
  void  set_method_handle_invoke(bool f)   { _method_handle_invoke = f; }
  bool  is_method_handle_invoke() const    { return _method_handle_invoke; }
  void  set_override_symbolic_info(bool f) { _override_symbolic_info = f; }
  bool  override_symbolic_info() const     { return _override_symbolic_info; }
  void  set_arg_escape(bool f)             { _arg_escape = f; }
  bool  arg_escape() const                 { return _arg_escape; }
  void copy_call_debug_info(PhaseIterGVN* phase, SafePointNode *sfpt);

  DEBUG_ONLY( bool validate_symbolic_info() const; )

#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
  virtual void  dump_compact_spec(outputStream *st) const;
#endif
};

//------------------------------CallStaticJavaNode-----------------------------
// Make a direct subroutine call using Java calling convention (for static
// calls and optimized virtual calls, plus calls to wrappers for run-time
// routines); generates static stub.
class CallStaticJavaNode : public CallJavaNode {
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger
public:
  CallStaticJavaNode(Compile* C, const TypeFunc* tf, address addr, ciMethod* method)
    : CallJavaNode(tf, addr, method) {
    init_class_id(Class_CallStaticJava);
    if (C->eliminate_boxing() && (method != NULL) && method->is_boxing_method()) {
      init_flags(Flag_is_macro);
      C->add_macro_node(this);
    }
  }
  CallStaticJavaNode(const TypeFunc* tf, address addr, const char* name, const TypePtr* adr_type)
    : CallJavaNode(tf, addr, NULL) {
    init_class_id(Class_CallStaticJava);
    // This node calls a runtime stub, which often has narrow memory effects.
    _adr_type = adr_type;
    _name = name;
  }

  // If this is an uncommon trap, return the request code, else zero.
  int uncommon_trap_request() const;
  static int extract_uncommon_trap_request(const Node* call);

  bool is_boxing_method() const {
    return is_macro() && (method() != NULL) && method()->is_boxing_method();
  }
  // Late inlining modifies the JVMState, so we need to deep clone it
  // when the call node is cloned (because it is macro node).
  virtual bool needs_deep_clone_jvms(Compile* C) {
    return is_boxing_method() || CallNode::needs_deep_clone_jvms(C);
  }

  virtual int         Opcode() const;
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);

#ifndef PRODUCT
  virtual void        dump_spec(outputStream *st) const;
  virtual void        dump_compact_spec(outputStream *st) const;
#endif
};

//------------------------------CallDynamicJavaNode----------------------------
// Make a dispatched call using Java calling convention.
class CallDynamicJavaNode : public CallJavaNode {
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger
public:
  CallDynamicJavaNode(const TypeFunc* tf , address addr, ciMethod* method, int vtable_index)
    : CallJavaNode(tf,addr,method), _vtable_index(vtable_index) {
    init_class_id(Class_CallDynamicJava);
  }

  // Late inlining modifies the JVMState, so we need to deep clone it
  // when the call node is cloned.
  virtual bool needs_deep_clone_jvms(Compile* C) {
    return IncrementalInlineVirtual || CallNode::needs_deep_clone_jvms(C);
  }

  int _vtable_index;
  virtual int   Opcode() const;
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
#endif
};

//------------------------------CallRuntimeNode--------------------------------
// Make a direct subroutine call node into compiled C++ code.
class CallRuntimeNode : public CallNode {
protected:
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger
public:
  CallRuntimeNode(const TypeFunc* tf, address addr, const char* name,
                  const TypePtr* adr_type, JVMState* jvms = nullptr)
    : CallNode(tf, addr, adr_type, jvms)
  {
    init_class_id(Class_CallRuntime);
    _name = name;
  }

  virtual int   Opcode() const;
  virtual void  calling_convention( BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt ) const;

#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
#endif
};

//------------------------------CallLeafNode-----------------------------------
// Make a direct subroutine call node into compiled C++ code, without
// safepoints
class CallLeafNode : public CallRuntimeNode {
public:
  CallLeafNode(const TypeFunc* tf, address addr, const char* name,
               const TypePtr* adr_type)
    : CallRuntimeNode(tf, addr, name, adr_type)
  {
    init_class_id(Class_CallLeaf);
  }
  virtual int   Opcode() const;
  virtual bool        guaranteed_safepoint()  { return false; }
#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
#endif
};

//------------------------------CallNativeNode-----------------------------------
// Make a direct call into a foreign function with an arbitrary ABI
// safepoints
class CallNativeNode : public CallNode {
  friend class MachCallNativeNode;
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const;
  static void print_regs(const GrowableArray<VMReg>& regs, outputStream* st);
public:
  GrowableArray<VMReg> _arg_regs;
  GrowableArray<VMReg> _ret_regs;
  const int _shadow_space_bytes;
  const bool _need_transition;

  CallNativeNode(const TypeFunc* tf, address addr, const char* name,
                 const TypePtr* adr_type,
                 const GrowableArray<VMReg>& arg_regs,
                 const GrowableArray<VMReg>& ret_regs,
                 int shadow_space_bytes,
                 bool need_transition)
    : CallNode(tf, addr, adr_type), _arg_regs(arg_regs),
      _ret_regs(ret_regs), _shadow_space_bytes(shadow_space_bytes),
      _need_transition(need_transition)
  {
    init_class_id(Class_CallNative);
    _name = name;
  }
  virtual int   Opcode() const;
  virtual bool  guaranteed_safepoint()  { return _need_transition; }
  virtual Node* match(const ProjNode *proj, const Matcher *m);
  virtual void  calling_convention( BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt ) const;
#ifndef PRODUCT
  virtual void  dump_spec(outputStream *st) const;
#endif
};

//------------------------------CallLeafNoFPNode-------------------------------
// CallLeafNode, not using floating point or using it in the same manner as
// the generated code
class CallLeafNoFPNode : public CallLeafNode {
public:
  CallLeafNoFPNode(const TypeFunc* tf, address addr, const char* name,
                   const TypePtr* adr_type)
    : CallLeafNode(tf, addr, name, adr_type)
  {
    init_class_id(Class_CallLeafNoFP);
  }
  virtual int   Opcode() const;
};

//------------------------------CallLeafVectorNode-------------------------------
// CallLeafNode but calling with vector calling convention instead.
class CallLeafVectorNode : public CallLeafNode {
private:
  uint _num_bits;
protected:
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const; // Size is bigger
public:
  CallLeafVectorNode(const TypeFunc* tf, address addr, const char* name,
                   const TypePtr* adr_type, uint num_bits)
    : CallLeafNode(tf, addr, name, adr_type), _num_bits(num_bits)
  {
  }
  virtual int   Opcode() const;
  virtual void  calling_convention( BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt ) const;
};


//------------------------------Allocate---------------------------------------
// High-level memory allocation
//
//  AllocateNode and AllocateArrayNode are subclasses of CallNode because they will
//  get expanded into a code sequence containing a call.  Unlike other CallNodes,
//  they have 2 memory projections and 2 i_o projections (which are distinguished by
//  the _is_io_use flag in the projection.)  This is needed when expanding the node in
//  order to differentiate the uses of the projection on the normal control path from
//  those on the exception return path.
//
class AllocateNode : public CallNode {
public:
  enum {
    // Output:
    RawAddress  = TypeFunc::Parms,    // the newly-allocated raw address
    // Inputs:
    AllocSize   = TypeFunc::Parms,    // size (in bytes) of the new object
    KlassNode,                        // type (maybe dynamic) of the obj.
    InitialTest,                      // slow-path test (may be constant)
    ALength,                          // array length (or TOP if none)
    ParmLimit
  };

  static const TypeFunc* alloc_type(const Type* t) {
    const Type** fields = TypeTuple::fields(ParmLimit - TypeFunc::Parms);
    fields[AllocSize]   = TypeInt::POS;
    fields[KlassNode]   = TypeInstPtr::NOTNULL;
    fields[InitialTest] = TypeInt::BOOL;
    fields[ALength]     = t;  // length (can be a bad length)

    const TypeTuple *domain = TypeTuple::make(ParmLimit, fields);

    // create result type (range)
    fields = TypeTuple::fields(1);
    fields[TypeFunc::Parms+0] = TypeRawPtr::NOTNULL; // Returned oop

    const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+1, fields);

    return TypeFunc::make(domain, range);
  }

  // Result of Escape Analysis
  bool _is_scalar_replaceable;
  bool _is_non_escaping;
  // True when MemBar for new is redundant with MemBar at initialzer exit
  bool _is_allocation_MemBar_redundant;

  virtual uint size_of() const; // Size is bigger
  AllocateNode(Compile* C, const TypeFunc *atype, Node *ctrl, Node *mem, Node *abio,
               Node *size, Node *klass_node, Node *initial_test);
  // Expansion modifies the JVMState, so we need to deep clone it
  virtual bool needs_deep_clone_jvms(Compile* C) { return true; }
  virtual int Opcode() const;
  virtual uint ideal_reg() const { return Op_RegP; }
  virtual bool        guaranteed_safepoint()  { return false; }

  // allocations do not modify their arguments
  virtual bool        may_modify(const TypeOopPtr *t_oop, PhaseTransform *phase) { return false;}

  // Pattern-match a possible usage of AllocateNode.
  // Return null if no allocation is recognized.
  // The operand is the pointer produced by the (possible) allocation.
  // It must be a projection of the Allocate or its subsequent CastPP.
  // (Note:  This function is defined in file graphKit.cpp, near
  // GraphKit::new_instance/new_array, whose output it recognizes.)
  // The 'ptr' may not have an offset unless the 'offset' argument is given.
  static AllocateNode* Ideal_allocation(Node* ptr, PhaseTransform* phase);

  // Fancy version which uses AddPNode::Ideal_base_and_offset to strip
  // an offset, which is reported back to the caller.
  // (Note:  AllocateNode::Ideal_allocation is defined in graphKit.cpp.)
  static AllocateNode* Ideal_allocation(Node* ptr, PhaseTransform* phase,
                                        intptr_t& offset);

  // Dig the klass operand out of a (possible) allocation site.
  static Node* Ideal_klass(Node* ptr, PhaseTransform* phase) {
    AllocateNode* allo = Ideal_allocation(ptr, phase);
    return (allo == NULL) ? NULL : allo->in(KlassNode);
  }

  // Conservatively small estimate of offset of first non-header byte.
  int minimum_header_size() {
    return is_AllocateArray() ? arrayOopDesc::base_offset_in_bytes(T_BYTE) :
                                instanceOopDesc::base_offset_in_bytes();
  }

  // Return the corresponding initialization barrier (or null if none).
  // Walks out edges to find it...
  // (Note: Both InitializeNode::allocation and AllocateNode::initialization
  // are defined in graphKit.cpp, which sets up the bidirectional relation.)
  InitializeNode* initialization();

  // Convenience for initialization->maybe_set_complete(phase)
  bool maybe_set_complete(PhaseGVN* phase);

  // Return true if allocation doesn't escape thread, its escape state
  // needs be noEscape or ArgEscape. InitializeNode._does_not_escape
  // is true when its allocation's escape state is noEscape or
  // ArgEscape. In case allocation's InitializeNode is NULL, check
  // AlllocateNode._is_non_escaping flag.
  // AlllocateNode._is_non_escaping is true when its escape state is
  // noEscape.
  bool does_not_escape_thread() {
    InitializeNode* init = NULL;
    return _is_non_escaping || (((init = initialization()) != NULL) && init->does_not_escape());
  }

  // If object doesn't escape in <.init> method and there is memory barrier
  // inserted at exit of its <.init>, memory barrier for new is not necessary.
  // Inovke this method when MemBar at exit of initializer and post-dominate
  // allocation node.
  void compute_MemBar_redundancy(ciMethod* initializer);
  bool is_allocation_MemBar_redundant() { return _is_allocation_MemBar_redundant; }

  Node* make_ideal_mark(PhaseGVN *phase, Node* obj, Node* control, Node* mem);
};

//------------------------------AllocateArray---------------------------------
//
// High-level array allocation
//
class AllocateArrayNode : public AllocateNode {
public:
  AllocateArrayNode(Compile* C, const TypeFunc *atype, Node *ctrl, Node *mem, Node *abio,
                    Node* size, Node* klass_node, Node* initial_test,
                    Node* count_val
                    )
    : AllocateNode(C, atype, ctrl, mem, abio, size, klass_node,
                   initial_test)
  {
    init_class_id(Class_AllocateArray);
    set_req(AllocateNode::ALength,        count_val);
  }
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);

  // Dig the length operand out of a array allocation site.
  Node* Ideal_length() {
    return in(AllocateNode::ALength);
  }

  // Dig the length operand out of a array allocation site and narrow the
  // type with a CastII, if necesssary
  Node* make_ideal_length(const TypeOopPtr* ary_type, PhaseTransform *phase, bool can_create = true);

  // Pattern-match a possible usage of AllocateArrayNode.
  // Return null if no allocation is recognized.
  static AllocateArrayNode* Ideal_array_allocation(Node* ptr, PhaseTransform* phase) {
    AllocateNode* allo = Ideal_allocation(ptr, phase);
    return (allo == NULL || !allo->is_AllocateArray())
           ? NULL : allo->as_AllocateArray();
  }
};

//------------------------------AbstractLockNode-----------------------------------
class AbstractLockNode: public CallNode {
private:
  enum {
    Regular = 0,  // Normal lock
    NonEscObj,    // Lock is used for non escaping object
    Coarsened,    // Lock was coarsened
    Nested        // Nested lock
  } _kind;

  static const char* _kind_names[Nested+1];

#ifndef PRODUCT
  NamedCounter* _counter;
#endif

protected:
  // helper functions for lock elimination
  //

  bool find_matching_unlock(const Node* ctrl, LockNode* lock,
                            GrowableArray<AbstractLockNode*> &lock_ops);
  bool find_lock_and_unlock_through_if(Node* node, LockNode* lock,
                                       GrowableArray<AbstractLockNode*> &lock_ops);
  bool find_unlocks_for_region(const RegionNode* region, LockNode* lock,
                               GrowableArray<AbstractLockNode*> &lock_ops);
  LockNode *find_matching_lock(UnlockNode* unlock);

  // Update the counter to indicate that this lock was eliminated.
  void set_eliminated_lock_counter() PRODUCT_RETURN;

public:
  AbstractLockNode(const TypeFunc *tf)
    : CallNode(tf, NULL, TypeRawPtr::BOTTOM),
      _kind(Regular)
  {
#ifndef PRODUCT
    _counter = NULL;
#endif
  }
  virtual int Opcode() const = 0;
  Node *   obj_node() const       {return in(TypeFunc::Parms + 0); }
  Node *   box_node() const       {return in(TypeFunc::Parms + 1); }
  Node *   fastlock_node() const  {return in(TypeFunc::Parms + 2); }
  void     set_box_node(Node* box) { set_req(TypeFunc::Parms + 1, box); }

  const Type *sub(const Type *t1, const Type *t2) const { return TypeInt::CC;}

  virtual uint size_of() const { return sizeof(*this); }

  bool is_eliminated()  const { return (_kind != Regular); }
  bool is_non_esc_obj() const { return (_kind == NonEscObj); }
  bool is_coarsened()   const { return (_kind == Coarsened); }
  bool is_nested()      const { return (_kind == Nested); }

  const char * kind_as_string() const;
  void log_lock_optimization(Compile* c, const char * tag, Node* bad_lock = NULL) const;

  void set_non_esc_obj() { _kind = NonEscObj; set_eliminated_lock_counter(); }
  void set_coarsened()   { _kind = Coarsened; set_eliminated_lock_counter(); }
  void set_nested()      { _kind = Nested; set_eliminated_lock_counter(); }

  // locking does not modify its arguments
  virtual bool may_modify(const TypeOopPtr *t_oop, PhaseTransform *phase){ return false;}

#ifndef PRODUCT
  void create_lock_counter(JVMState* s);
  NamedCounter* counter() const { return _counter; }
  virtual void dump_spec(outputStream* st) const;
  virtual void dump_compact_spec(outputStream* st) const;
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

//------------------------------Lock---------------------------------------
// High-level lock operation
//
// This is a subclass of CallNode because it is a macro node which gets expanded
// into a code sequence containing a call.  This node takes 3 "parameters":
//    0  -  object to lock
//    1 -   a BoxLockNode
//    2 -   a FastLockNode
//
class LockNode : public AbstractLockNode {
public:

  static const TypeFunc *lock_type() {
    // create input type (domain)
    const Type **fields = TypeTuple::fields(3);
    fields[TypeFunc::Parms+0] = TypeInstPtr::NOTNULL;  // Object to be Locked
    fields[TypeFunc::Parms+1] = TypeRawPtr::BOTTOM;    // Address of stack location for lock
    fields[TypeFunc::Parms+2] = TypeInt::BOOL;         // FastLock
    const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms+3,fields);

    // create result type (range)
    fields = TypeTuple::fields(0);

    const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+0,fields);

    return TypeFunc::make(domain,range);
  }

  virtual int Opcode() const;
  virtual uint size_of() const; // Size is bigger
  LockNode(Compile* C, const TypeFunc *tf) : AbstractLockNode( tf ) {
    init_class_id(Class_Lock);
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }
  virtual bool        guaranteed_safepoint()  { return false; }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  // Expansion modifies the JVMState, so we need to deep clone it
  virtual bool needs_deep_clone_jvms(Compile* C) { return true; }

  bool is_nested_lock_region(); // Is this Lock nested?
  bool is_nested_lock_region(Compile * c); // Why isn't this Lock nested?
};

//------------------------------Unlock---------------------------------------
// High-level unlock operation
class UnlockNode : public AbstractLockNode {
private:
#ifdef ASSERT
  JVMState* const _dbg_jvms;      // Pointer to list of JVM State objects
#endif
public:
  virtual int Opcode() const;
  virtual uint size_of() const; // Size is bigger
  UnlockNode(Compile* C, const TypeFunc *tf) : AbstractLockNode( tf )
#ifdef ASSERT
    , _dbg_jvms(NULL)
#endif
  {
    init_class_id(Class_Unlock);
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  // unlock is never a safepoint
  virtual bool        guaranteed_safepoint()  { return false; }
#ifdef ASSERT
  void set_dbg_jvms(JVMState* s) {
    *(JVMState**)&_dbg_jvms = s;  // override const attribute in the accessor
  }
  JVMState* dbg_jvms() const { return _dbg_jvms; }
#else
  JVMState* dbg_jvms() const { return NULL; }
#endif
};
#endif // SHARE_OPTO_CALLNODE_HPP
