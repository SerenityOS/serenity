/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_CFGNODE_HPP
#define SHARE_OPTO_CFGNODE_HPP

#include "opto/multnode.hpp"
#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/type.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

class Matcher;
class Node;
class   RegionNode;
class   TypeNode;
class     PhiNode;
class   GotoNode;
class   MultiNode;
class     MultiBranchNode;
class       IfNode;
class       PCTableNode;
class         JumpNode;
class         CatchNode;
class       NeverBranchNode;
class   ProjNode;
class     CProjNode;
class       IfTrueNode;
class       IfFalseNode;
class       CatchProjNode;
class     JProjNode;
class       JumpProjNode;
class     SCMemProjNode;
class PhaseIdealLoop;

//------------------------------RegionNode-------------------------------------
// The class of RegionNodes, which can be mapped to basic blocks in the
// program.  Their inputs point to Control sources.  PhiNodes (described
// below) have an input point to a RegionNode.  Merged data inputs to PhiNodes
// correspond 1-to-1 with RegionNode inputs.  The zero input of a PhiNode is
// the RegionNode, and the zero input of the RegionNode is itself.
class RegionNode : public Node {
private:
  bool _is_unreachable_region;

  bool is_possible_unsafe_loop(const PhaseGVN* phase) const;
  bool is_unreachable_from_root(const PhaseGVN* phase) const;
public:
  // Node layout (parallels PhiNode):
  enum { Region,                // Generally points to self.
         Control                // Control arcs are [1..len)
  };

  RegionNode(uint required) : Node(required), _is_unreachable_region(false) {
    init_class_id(Class_Region);
    init_req(0, this);
  }

  Node* is_copy() const {
    const Node* r = _in[Region];
    if (r == NULL)
      return nonnull_req();
    return NULL;  // not a copy!
  }
  PhiNode* has_phi() const;        // returns an arbitrary phi user, or NULL
  PhiNode* has_unique_phi() const; // returns the unique phi user, or NULL
  // Is this region node unreachable from root?
  bool is_unreachable_region(const PhaseGVN* phase);
  virtual int Opcode() const;
  virtual uint size_of() const { return sizeof(*this); }
  virtual bool pinned() const { return (const Node*)in(0) == this; }
  virtual bool is_CFG() const { return true; }
  virtual uint hash() const { return NO_HASH; } // CFG nodes do not hash
  virtual bool depends_only_on_test() const { return false; }
  virtual const Type* bottom_type() const { return Type::CONTROL; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual const RegMask &out_RegMask() const;
  bool try_clean_mem_phi(PhaseGVN* phase);
  bool optimize_trichotomy(PhaseIterGVN* igvn);
};

//------------------------------JProjNode--------------------------------------
// jump projection for node that produces multiple control-flow paths
class JProjNode : public ProjNode {
 public:
  JProjNode( Node* ctrl, uint idx ) : ProjNode(ctrl,idx) {}
  virtual int Opcode() const;
  virtual bool  is_CFG() const { return true; }
  virtual uint  hash() const { return NO_HASH; }  // CFG nodes do not hash
  virtual const Node* is_block_proj() const { return in(0); }
  virtual const RegMask& out_RegMask() const;
  virtual uint  ideal_reg() const { return 0; }
};

//------------------------------PhiNode----------------------------------------
// PhiNodes merge values from different Control paths.  Slot 0 points to the
// controlling RegionNode.  Other slots map 1-for-1 with incoming control flow
// paths to the RegionNode.
class PhiNode : public TypeNode {
  friend class PhaseRenumberLive;

  const TypePtr* const _adr_type; // non-null only for Type::MEMORY nodes.
  // The following fields are only used for data PhiNodes to indicate
  // that the PhiNode represents the value of a known instance field.
        int _inst_mem_id; // Instance memory id (node index of the memory Phi)
        int _inst_id;     // Instance id of the memory slice.
  const int _inst_index;  // Alias index of the instance memory slice.
  // Array elements references have the same alias_idx but different offset.
  const int _inst_offset; // Offset of the instance memory slice.
  // Size is bigger to hold the _adr_type field.
  virtual uint hash() const;    // Check the type
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const { return sizeof(*this); }

  // Determine if CMoveNode::is_cmove_id can be used at this join point.
  Node* is_cmove_id(PhaseTransform* phase, int true_path);
  bool wait_for_region_igvn(PhaseGVN* phase);
  bool is_data_loop(RegionNode* r, Node* uin, const PhaseGVN* phase);

public:
  // Node layout (parallels RegionNode):
  enum { Region,                // Control input is the Phi's region.
         Input                  // Input values are [1..len)
  };

  PhiNode( Node *r, const Type *t, const TypePtr* at = NULL,
           const int imid = -1,
           const int iid = TypeOopPtr::InstanceTop,
           const int iidx = Compile::AliasIdxTop,
           const int ioffs = Type::OffsetTop )
    : TypeNode(t,r->req()),
      _adr_type(at),
      _inst_mem_id(imid),
      _inst_id(iid),
      _inst_index(iidx),
      _inst_offset(ioffs)
  {
    init_class_id(Class_Phi);
    init_req(0, r);
    verify_adr_type();
  }
  // create a new phi with in edges matching r and set (initially) to x
  static PhiNode* make( Node* r, Node* x );
  // extra type arguments override the new phi's bottom_type and adr_type
  static PhiNode* make( Node* r, Node* x, const Type *t, const TypePtr* at = NULL );
  // create a new phi with narrowed memory type
  PhiNode* slice_memory(const TypePtr* adr_type) const;
  PhiNode* split_out_instance(const TypePtr* at, PhaseIterGVN *igvn) const;
  // like make(r, x), but does not initialize the in edges to x
  static PhiNode* make_blank( Node* r, Node* x );

  // Accessors
  RegionNode* region() const { Node* r = in(Region); assert(!r || r->is_Region(), ""); return (RegionNode*)r; }

  bool is_tripcount(BasicType bt) const;

  // Determine a unique non-trivial input, if any.
  // Ignore casts if it helps.  Return NULL on failure.
  Node* unique_input(PhaseTransform *phase, bool uncast);
  Node* unique_input(PhaseTransform *phase) {
    Node* uin = unique_input(phase, false);
    if (uin == NULL) {
      uin = unique_input(phase, true);
    }
    return uin;
  }

  // Check for a simple dead loop.
  enum LoopSafety { Safe = 0, Unsafe, UnsafeLoop };
  LoopSafety simple_data_loop_check(Node *in) const;
  // Is it unsafe data loop? It becomes a dead loop if this phi node removed.
  bool is_unsafe_data_reference(Node *in) const;
  int  is_diamond_phi(bool check_control_only = false) const;
  virtual int Opcode() const;
  virtual bool pinned() const { return in(0) != 0; }
  virtual const TypePtr *adr_type() const { verify_adr_type(true); return _adr_type; }

  void  set_inst_mem_id(int inst_mem_id) { _inst_mem_id = inst_mem_id; }
  const int inst_mem_id() const { return _inst_mem_id; }
  const int inst_id()     const { return _inst_id; }
  const int inst_index()  const { return _inst_index; }
  const int inst_offset() const { return _inst_offset; }
  bool is_same_inst_field(const Type* tp, int mem_id, int id, int index, int offset) {
    return type()->basic_type() == tp->basic_type() &&
           inst_mem_id() == mem_id &&
           inst_id()     == id     &&
           inst_index()  == index  &&
           inst_offset() == offset &&
           type()->higher_equal(tp);
  }

  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const RegMask &out_RegMask() const;
  virtual const RegMask &in_RegMask(uint) const;
#ifndef PRODUCT
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
  virtual void dump_spec(outputStream *st) const;
#endif
#ifdef ASSERT
  void verify_adr_type(VectorSet& visited, const TypePtr* at) const;
  void verify_adr_type(bool recursive = false) const;
#else //ASSERT
  void verify_adr_type(bool recursive = false) const {}
#endif //ASSERT
};

//------------------------------GotoNode---------------------------------------
// GotoNodes perform direct branches.
class GotoNode : public Node {
public:
  GotoNode( Node *control ) : Node(control) {}
  virtual int Opcode() const;
  virtual bool pinned() const { return true; }
  virtual bool  is_CFG() const { return true; }
  virtual uint hash() const { return NO_HASH; }  // CFG nodes do not hash
  virtual const Node *is_block_proj() const { return this; }
  virtual bool depends_only_on_test() const { return false; }
  virtual const Type *bottom_type() const { return Type::CONTROL; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const RegMask &out_RegMask() const;

#ifndef PRODUCT
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

//------------------------------CProjNode--------------------------------------
// control projection for node that produces multiple control-flow paths
class CProjNode : public ProjNode {
public:
  CProjNode( Node *ctrl, uint idx ) : ProjNode(ctrl,idx) {}
  virtual int Opcode() const;
  virtual bool  is_CFG() const { return true; }
  virtual uint hash() const { return NO_HASH; }  // CFG nodes do not hash
  virtual const Node *is_block_proj() const { return in(0); }
  virtual const RegMask &out_RegMask() const;
  virtual uint ideal_reg() const { return 0; }
};

//---------------------------MultiBranchNode-----------------------------------
// This class defines a MultiBranchNode, a MultiNode which yields multiple
// control values. These are distinguished from other types of MultiNodes
// which yield multiple values, but control is always and only projection #0.
class MultiBranchNode : public MultiNode {
public:
  MultiBranchNode( uint required ) : MultiNode(required) {
    init_class_id(Class_MultiBranch);
  }
  // returns required number of users to be well formed.
  virtual int required_outcnt() const = 0;
};

//------------------------------IfNode-----------------------------------------
// Output selected Control, based on a boolean test
class IfNode : public MultiBranchNode {
  // Size is bigger to hold the probability field.  However, _prob does not
  // change the semantics so it does not appear in the hash & cmp functions.
  virtual uint size_of() const { return sizeof(*this); }

private:
  // Helper methods for fold_compares
  bool cmpi_folds(PhaseIterGVN* igvn, bool fold_ne = false);
  bool is_ctrl_folds(Node* ctrl, PhaseIterGVN* igvn);
  bool has_shared_region(ProjNode* proj, ProjNode*& success, ProjNode*& fail);
  bool has_only_uncommon_traps(ProjNode* proj, ProjNode*& success, ProjNode*& fail, PhaseIterGVN* igvn);
  Node* merge_uncommon_traps(ProjNode* proj, ProjNode* success, ProjNode* fail, PhaseIterGVN* igvn);
  static void improve_address_types(Node* l, Node* r, ProjNode* fail, PhaseIterGVN* igvn);
  bool is_cmp_with_loadrange(ProjNode* proj);
  bool is_null_check(ProjNode* proj, PhaseIterGVN* igvn);
  bool is_side_effect_free_test(ProjNode* proj, PhaseIterGVN* igvn);
  void reroute_side_effect_free_unc(ProjNode* proj, ProjNode* dom_proj, PhaseIterGVN* igvn);
  ProjNode* uncommon_trap_proj(CallStaticJavaNode*& call) const;
  bool fold_compares_helper(ProjNode* proj, ProjNode* success, ProjNode* fail, PhaseIterGVN* igvn);
  static bool is_dominator_unc(CallStaticJavaNode* dom_unc, CallStaticJavaNode* unc);

protected:
  ProjNode* range_check_trap_proj(int& flip, Node*& l, Node*& r);
  Node* Ideal_common(PhaseGVN *phase, bool can_reshape);
  Node* search_identical(int dist);

  Node* simple_subsuming(PhaseIterGVN* igvn);

public:

  // Degrees of branch prediction probability by order of magnitude:
  // PROB_UNLIKELY_1e(N) is a 1 in 1eN chance.
  // PROB_LIKELY_1e(N) is a 1 - PROB_UNLIKELY_1e(N)
#define PROB_UNLIKELY_MAG(N)    (1e- ## N ## f)
#define PROB_LIKELY_MAG(N)      (1.0f-PROB_UNLIKELY_MAG(N))

  // Maximum and minimum branch prediction probabilties
  // 1 in 1,000,000 (magnitude 6)
  //
  // Although PROB_NEVER == PROB_MIN and PROB_ALWAYS == PROB_MAX
  // they are used to distinguish different situations:
  //
  // The name PROB_MAX (PROB_MIN) is for probabilities which correspond to
  // very likely (unlikely) but with a concrete possibility of a rare
  // contrary case.  These constants would be used for pinning
  // measurements, and as measures for assertions that have high
  // confidence, but some evidence of occasional failure.
  //
  // The name PROB_ALWAYS (PROB_NEVER) is to stand for situations for which
  // there is no evidence at all that the contrary case has ever occurred.

#define PROB_NEVER              PROB_UNLIKELY_MAG(6)
#define PROB_ALWAYS             PROB_LIKELY_MAG(6)

#define PROB_MIN                PROB_UNLIKELY_MAG(6)
#define PROB_MAX                PROB_LIKELY_MAG(6)

  // Static branch prediction probabilities
  // 1 in 10 (magnitude 1)
#define PROB_STATIC_INFREQUENT  PROB_UNLIKELY_MAG(1)
#define PROB_STATIC_FREQUENT    PROB_LIKELY_MAG(1)

  // Fair probability 50/50
#define PROB_FAIR               (0.5f)

  // Unknown probability sentinel
#define PROB_UNKNOWN            (-1.0f)

  // Probability "constructors", to distinguish as a probability any manifest
  // constant without a names
#define PROB_LIKELY(x)          ((float) (x))
#define PROB_UNLIKELY(x)        (1.0f - (float)(x))

  // Other probabilities in use, but without a unique name, are documented
  // here for lack of a better place:
  //
  // 1 in 1000 probabilities (magnitude 3):
  //     threshold for converting to conditional move
  //     likelihood of null check failure if a null HAS been seen before
  //     likelihood of slow path taken in library calls
  //
  // 1 in 10,000 probabilities (magnitude 4):
  //     threshold for making an uncommon trap probability more extreme
  //     threshold for for making a null check implicit
  //     likelihood of needing a gc if eden top moves during an allocation
  //     likelihood of a predicted call failure
  //
  // 1 in 100,000 probabilities (magnitude 5):
  //     threshold for ignoring counts when estimating path frequency
  //     likelihood of FP clipping failure
  //     likelihood of catching an exception from a try block
  //     likelihood of null check failure if a null has NOT been seen before
  //
  // Magic manifest probabilities such as 0.83, 0.7, ... can be found in
  // gen_subtype_check() and catch_inline_exceptions().

  float _prob;                  // Probability of true path being taken.
  float _fcnt;                  // Frequency counter
  IfNode( Node *control, Node *b, float p, float fcnt )
    : MultiBranchNode(2), _prob(p), _fcnt(fcnt) {
    init_class_id(Class_If);
    init_req(0,control);
    init_req(1,b);
  }
  virtual int Opcode() const;
  virtual bool pinned() const { return true; }
  virtual const Type *bottom_type() const { return TypeTuple::IFBOTH; }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual int required_outcnt() const { return 2; }
  virtual const RegMask &out_RegMask() const;
  Node* fold_compares(PhaseIterGVN* phase);
  static Node* up_one_dom(Node* curr, bool linear_only = false);
  Node* dominated_by(Node* prev_dom, PhaseIterGVN* igvn);

  // Takes the type of val and filters it through the test represented
  // by if_proj and returns a more refined type if one is produced.
  // Returns NULL is it couldn't improve the type.
  static const TypeInt* filtered_int_type(PhaseGVN* phase, Node* val, Node* if_proj);

#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
  virtual void related(GrowableArray <Node *> *in_rel, GrowableArray <Node *> *out_rel, bool compact) const;
#endif
};

class RangeCheckNode : public IfNode {
private:
  int is_range_check(Node* &range, Node* &index, jint &offset);

public:
  RangeCheckNode(Node* control, Node *b, float p, float fcnt)
    : IfNode(control, b, p, fcnt) {
    init_class_id(Class_RangeCheck);
  }

  virtual int Opcode() const;
  virtual Node* Ideal(PhaseGVN *phase, bool can_reshape);
};

class IfProjNode : public CProjNode {
public:
  IfProjNode(IfNode *ifnode, uint idx) : CProjNode(ifnode,idx) {}
  virtual Node* Identity(PhaseGVN* phase);

protected:
  // Type of If input when this branch is always taken
  virtual bool always_taken(const TypeTuple* t) const = 0;

#ifndef PRODUCT
public:
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

class IfTrueNode : public IfProjNode {
public:
  IfTrueNode( IfNode *ifnode ) : IfProjNode(ifnode,1) {
    init_class_id(Class_IfTrue);
  }
  virtual int Opcode() const;

protected:
  virtual bool always_taken(const TypeTuple* t) const { return t == TypeTuple::IFTRUE; }
};

class IfFalseNode : public IfProjNode {
public:
  IfFalseNode( IfNode *ifnode ) : IfProjNode(ifnode,0) {
    init_class_id(Class_IfFalse);
  }
  virtual int Opcode() const;

protected:
  virtual bool always_taken(const TypeTuple* t) const { return t == TypeTuple::IFFALSE; }
};


//------------------------------PCTableNode------------------------------------
// Build an indirect branch table.  Given a control and a table index,
// control is passed to the Projection matching the table index.  Used to
// implement switch statements and exception-handling capabilities.
// Undefined behavior if passed-in index is not inside the table.
class PCTableNode : public MultiBranchNode {
  virtual uint hash() const;    // Target count; table size
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const { return sizeof(*this); }

public:
  const uint _size;             // Number of targets

  PCTableNode( Node *ctrl, Node *idx, uint size ) : MultiBranchNode(2), _size(size) {
    init_class_id(Class_PCTable);
    init_req(0, ctrl);
    init_req(1, idx);
  }
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type *bottom_type() const;
  virtual bool pinned() const { return true; }
  virtual int required_outcnt() const { return _size; }
};

//------------------------------JumpNode---------------------------------------
// Indirect branch.  Uses PCTable above to implement a switch statement.
// It emits as a table load and local branch.
class JumpNode : public PCTableNode {
  virtual uint size_of() const { return sizeof(*this); }
public:
  float* _probs; // probability of each projection
  float _fcnt;   // total number of times this Jump was executed
  JumpNode( Node* control, Node* switch_val, uint size, float* probs, float cnt)
    : PCTableNode(control, switch_val, size),
      _probs(probs), _fcnt(cnt) {
    init_class_id(Class_Jump);
  }
  virtual int   Opcode() const;
  virtual const RegMask& out_RegMask() const;
  virtual const Node* is_block_proj() const { return this; }
#ifndef PRODUCT
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

class JumpProjNode : public JProjNode {
  virtual uint hash() const;
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const { return sizeof(*this); }

 private:
  const int  _dest_bci;
  const uint _proj_no;
  const int  _switch_val;
 public:
  JumpProjNode(Node* jumpnode, uint proj_no, int dest_bci, int switch_val)
    : JProjNode(jumpnode, proj_no), _dest_bci(dest_bci), _proj_no(proj_no), _switch_val(switch_val) {
    init_class_id(Class_JumpProj);
  }

  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::CONTROL; }
  int  dest_bci()    const { return _dest_bci; }
  int  switch_val()  const { return _switch_val; }
  uint proj_no()     const { return _proj_no; }
#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
  virtual void dump_compact_spec(outputStream *st) const;
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};

//------------------------------CatchNode--------------------------------------
// Helper node to fork exceptions.  "Catch" catches any exceptions thrown by
// a just-prior call.  Looks like a PCTableNode but emits no code - just the
// table.  The table lookup and branch is implemented by RethrowNode.
class CatchNode : public PCTableNode {
public:
  CatchNode( Node *ctrl, Node *idx, uint size ) : PCTableNode(ctrl,idx,size){
    init_class_id(Class_Catch);
  }
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
};

// CatchProjNode controls which exception handler is targetted after a call.
// It is passed in the bci of the target handler, or no_handler_bci in case
// the projection doesn't lead to an exception handler.
class CatchProjNode : public CProjNode {
  virtual uint hash() const;
  virtual bool cmp( const Node &n ) const;
  virtual uint size_of() const { return sizeof(*this); }

private:
  const int _handler_bci;

public:
  enum {
    fall_through_index =  0,      // the fall through projection index
    catch_all_index    =  1,      // the projection index for catch-alls
    no_handler_bci     = -1       // the bci for fall through or catch-all projs
  };

  CatchProjNode(Node* catchnode, uint proj_no, int handler_bci)
    : CProjNode(catchnode, proj_no), _handler_bci(handler_bci) {
    init_class_id(Class_CatchProj);
    assert(proj_no != fall_through_index || handler_bci < 0, "fall through case must have bci < 0");
  }

  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type *bottom_type() const { return Type::CONTROL; }
  int  handler_bci() const        { return _handler_bci; }
  bool is_handler_proj() const    { return _handler_bci >= 0; }
#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
#endif
};


//---------------------------------CreateExNode--------------------------------
// Helper node to create the exception coming back from a call
class CreateExNode : public TypeNode {
public:
  CreateExNode(const Type* t, Node* control, Node* i_o) : TypeNode(t, 2) {
    init_req(0, control);
    init_req(1, i_o);
  }
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual bool pinned() const { return true; }
  uint match_edge(uint idx) const { return 0; }
  virtual uint ideal_reg() const { return Op_RegP; }
};

//------------------------------NeverBranchNode-------------------------------
// The never-taken branch.  Used to give the appearance of exiting infinite
// loops to those algorithms that like all paths to be reachable.  Encodes
// empty.
class NeverBranchNode : public MultiBranchNode {
public:
  NeverBranchNode( Node *ctrl ) : MultiBranchNode(1) { init_req(0,ctrl); }
  virtual int Opcode() const;
  virtual bool pinned() const { return true; };
  virtual const Type *bottom_type() const { return TypeTuple::IFBOTH; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual int required_outcnt() const { return 2; }
  virtual void emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const { }
  virtual uint size(PhaseRegAlloc *ra_) const { return 0; }
#ifndef PRODUCT
  virtual void format( PhaseRegAlloc *, outputStream *st ) const;
#endif
};

#endif // SHARE_OPTO_CFGNODE_HPP
