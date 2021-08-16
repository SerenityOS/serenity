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

#ifndef SHARE_OPTO_MATCHER_HPP
#define SHARE_OPTO_MATCHER_HPP

#include "libadt/vectset.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compressedOops.hpp"
#include "opto/node.hpp"
#include "opto/phaseX.hpp"
#include "opto/regmask.hpp"
#include "runtime/vm_version.hpp"

class Compile;
class Node;
class MachNode;
class MachTypeNode;
class MachOper;

//---------------------------Matcher-------------------------------------------
class Matcher : public PhaseTransform {
  friend class VMStructs;

public:

  // Machine-dependent definitions
#include CPU_HEADER(matcher)

  // State and MStack class used in xform() and find_shared() iterative methods.
  enum Node_State { Pre_Visit,  // node has to be pre-visited
                    Visit,  // visit node
                    Post_Visit,  // post-visit node
                    Alt_Post_Visit   // alternative post-visit path
  };

  class MStack: public Node_Stack {
  public:
    MStack(int size) : Node_Stack(size) { }

    void push(Node *n, Node_State ns) {
      Node_Stack::push(n, (uint)ns);
    }
    void push(Node *n, Node_State ns, Node *parent, int indx) {
      ++_inode_top;
      if ((_inode_top + 1) >= _inode_max) grow();
      _inode_top->node = parent;
      _inode_top->indx = (uint)indx;
      ++_inode_top;
      _inode_top->node = n;
      _inode_top->indx = (uint)ns;
    }
    Node *parent() {
      pop();
      return node();
    }
    Node_State state() const {
      return (Node_State)index();
    }
    void set_state(Node_State ns) {
      set_index((uint)ns);
    }
  };

private:
  // Private arena of State objects
  ResourceArea _states_arena;

  VectorSet   _visited;         // Visit bits

  // Used to control the Label pass
  VectorSet   _shared;          // Shared Ideal Node
  VectorSet   _dontcare;        // Nothing the matcher cares about

  // Private methods which perform the actual matching and reduction
  // Walks the label tree, generating machine nodes
  MachNode *ReduceInst( State *s, int rule, Node *&mem);
  void ReduceInst_Chain_Rule( State *s, int rule, Node *&mem, MachNode *mach);
  uint ReduceInst_Interior(State *s, int rule, Node *&mem, MachNode *mach, uint num_opnds);
  void ReduceOper( State *s, int newrule, Node *&mem, MachNode *mach );

  // If this node already matched using "rule", return the MachNode for it.
  MachNode* find_shared_node(Node* n, uint rule);

  // Convert a dense opcode number to an expanded rule number
  const int *_reduceOp;
  const int *_leftOp;
  const int *_rightOp;

  // Map dense opcode number to info on when rule is swallowed constant.
  const bool *_swallowed;

  // Map dense rule number to determine if this is an instruction chain rule
  const uint _begin_inst_chain_rule;
  const uint _end_inst_chain_rule;

  // We want to clone constants and possible CmpI-variants.
  // If we do not clone CmpI, then we can have many instances of
  // condition codes alive at once.  This is OK on some chips and
  // bad on others.  Hence the machine-dependent table lookup.
  const char *_must_clone;

  // Find shared Nodes, or Nodes that otherwise are Matcher roots
  void find_shared( Node *n );
  bool find_shared_visit(MStack& mstack, Node* n, uint opcode, bool& mem_op, int& mem_addr_idx);
  void find_shared_post_visit(Node* n, uint opcode);

  bool is_vshift_con_pattern(Node* n, Node* m);

  // Debug and profile information for nodes in old space:
  GrowableArray<Node_Notes*>* _old_node_note_array;

  // Node labeling iterator for instruction selection
  Node* Label_Root(const Node* n, State* svec, Node* control, Node*& mem);

  Node *transform( Node *dummy );

  Node_List _projection_list;        // For Machine nodes killing many values

  Node_Array _shared_nodes;

#ifndef PRODUCT
  Node_Array _old2new_map;    // Map roots of ideal-trees to machine-roots
  Node_Array _new2old_map;    // Maps machine nodes back to ideal
  VectorSet _reused;          // Ideal IGV identifiers reused by machine nodes
#endif // !PRODUCT

  // Accessors for the inherited field PhaseTransform::_nodes:
  void   grow_new_node_array(uint idx_limit) {
    _nodes.map(idx_limit-1, NULL);
  }
  bool    has_new_node(const Node* n) const {
    return _nodes.at(n->_idx) != NULL;
  }
  Node*       new_node(const Node* n) const {
    assert(has_new_node(n), "set before get");
    return _nodes.at(n->_idx);
  }
  void    set_new_node(const Node* n, Node *nn) {
    assert(!has_new_node(n), "set only once");
    _nodes.map(n->_idx, nn);
  }

#ifdef ASSERT
  // Make sure only new nodes are reachable from this node
  void verify_new_nodes_only(Node* root);

  Node* _mem_node;   // Ideal memory node consumed by mach node
#endif

  // Mach node for ConP #NULL
  MachNode* _mach_null;

  void handle_precedence_edges(Node* n, MachNode *mach);

public:
  int LabelRootDepth;
  // Convert ideal machine register to a register mask for spill-loads
  static const RegMask *idealreg2regmask[];
  RegMask *idealreg2spillmask  [_last_machine_leaf];
  RegMask *idealreg2debugmask  [_last_machine_leaf];
  RegMask *idealreg2mhdebugmask[_last_machine_leaf];
  void init_spill_mask( Node *ret );
  // Convert machine register number to register mask
  static uint mreg2regmask_max;
  static RegMask mreg2regmask[];
  static RegMask STACK_ONLY_mask;
  static RegMask caller_save_regmask;
  static RegMask caller_save_regmask_exclude_soe;
  static RegMask mh_caller_save_regmask;
  static RegMask mh_caller_save_regmask_exclude_soe;

  MachNode* mach_null() const { return _mach_null; }

  bool    is_shared( Node *n ) { return _shared.test(n->_idx) != 0; }
  void   set_shared( Node *n ) {  _shared.set(n->_idx); }
  bool   is_visited( Node *n ) { return _visited.test(n->_idx) != 0; }
  void  set_visited( Node *n ) { _visited.set(n->_idx); }
  bool  is_dontcare( Node *n ) { return _dontcare.test(n->_idx) != 0; }
  void set_dontcare( Node *n ) {  _dontcare.set(n->_idx); }

  // Mode bit to tell DFA and expand rules whether we are running after
  // (or during) register selection.  Usually, the matcher runs before,
  // but it will also get called to generate post-allocation spill code.
  // In this situation, it is a deadly error to attempt to allocate more
  // temporary registers.
  bool _allocation_started;

  // Machine register names
  static const char *regName[];
  // Machine register encodings
  static const unsigned char _regEncode[];
  // Machine Node names
  const char **_ruleName;
  // Rules that are cheaper to rematerialize than to spill
  static const uint _begin_rematerialize;
  static const uint _end_rematerialize;

  // An array of chars, from 0 to _last_Mach_Reg.
  // No Save       = 'N' (for register windows)
  // Save on Entry = 'E'
  // Save on Call  = 'C'
  // Always Save   = 'A' (same as SOE + SOC)
  const char *_register_save_policy;
  const char *_c_reg_save_policy;
  // Convert a machine register to a machine register type, so-as to
  // properly match spill code.
  const int *_register_save_type;
  // Maps from machine register to boolean; true if machine register can
  // be holding a call argument in some signature.
  static bool can_be_java_arg( int reg );
  // Maps from machine register to boolean; true if machine register holds
  // a spillable argument.
  static bool is_spillable_arg( int reg );
  // Number of integer live ranges that constitute high register pressure
  static uint int_pressure_limit();
  // Number of float live ranges that constitute high register pressure
  static uint float_pressure_limit();

  // List of IfFalse or IfTrue Nodes that indicate a taken null test.
  // List is valid in the post-matching space.
  Node_List _null_check_tests;
  void collect_null_checks( Node *proj, Node *orig_proj );
  void validate_null_checks( );

  Matcher();

  // Get a projection node at position pos
  Node* get_projection(uint pos) {
    return _projection_list[pos];
  }

  // Push a projection node onto the projection list
  void push_projection(Node* node) {
    _projection_list.push(node);
  }

  Node* pop_projection() {
    return _projection_list.pop();
  }

  // Number of nodes in the projection list
  uint number_of_projections() const {
    return _projection_list.size();
  }

  // Select instructions for entire method
  void match();

  // Helper for match
  OptoReg::Name warp_incoming_stk_arg( VMReg reg );

  // Transform, then walk.  Does implicit DCE while walking.
  // Name changed from "transform" to avoid it being virtual.
  Node *xform( Node *old_space_node, int Nodes );

  // Match a single Ideal Node - turn it into a 1-Node tree; Label & Reduce.
  MachNode *match_tree( const Node *n );
  MachNode *match_sfpt( SafePointNode *sfpt );
  // Helper for match_sfpt
  OptoReg::Name warp_outgoing_stk_arg( VMReg reg, OptoReg::Name begin_out_arg_area, OptoReg::Name &out_arg_limit_per_call );

  // Initialize first stack mask and related masks.
  void init_first_stack_mask();

  // If we should save-on-entry this register
  bool is_save_on_entry( int reg );

  // Fixup the save-on-entry registers
  void Fixup_Save_On_Entry( );

  // --- Frame handling ---

  // Register number of the stack slot corresponding to the incoming SP.
  // Per the Big Picture in the AD file, it is:
  //   SharedInfo::stack0 + locks + in_preserve_stack_slots + pad2.
  OptoReg::Name _old_SP;

  // Register number of the stack slot corresponding to the highest incoming
  // argument on the stack.  Per the Big Picture in the AD file, it is:
  //   _old_SP + out_preserve_stack_slots + incoming argument size.
  OptoReg::Name _in_arg_limit;

  // Register number of the stack slot corresponding to the new SP.
  // Per the Big Picture in the AD file, it is:
  //   _in_arg_limit + pad0
  OptoReg::Name _new_SP;

  // Register number of the stack slot corresponding to the highest outgoing
  // argument on the stack.  Per the Big Picture in the AD file, it is:
  //   _new_SP + max outgoing arguments of all calls
  OptoReg::Name _out_arg_limit;

  OptoRegPair *_parm_regs;        // Array of machine registers per argument
  RegMask *_calling_convention_mask; // Array of RegMasks per argument

  // Does matcher have a match rule for this ideal node?
  static const bool has_match_rule(int opcode);
  static const bool _hasMatchRule[_last_opcode];

  // Does matcher have a match rule for this ideal node and is the
  // predicate (if there is one) true?
  // NOTE: If this function is used more commonly in the future, ADLC
  // should generate this one.
  static const bool match_rule_supported(int opcode);

  // identify extra cases that we might want to provide match rules for
  // e.g. Op_ vector nodes and other intrinsics while guarding with vlen
  static const bool match_rule_supported_vector(int opcode, int vlen, BasicType bt);

  static const RegMask* predicate_reg_mask(void);
  static const TypeVect* predicate_reg_type(const Type* elemTy, int length);

  // Vector width in bytes
  static const int vector_width_in_bytes(BasicType bt);

  // Limits on vector size (number of elements).
  static const int max_vector_size(const BasicType bt);
  static const int min_vector_size(const BasicType bt);
  static const bool vector_size_supported(const BasicType bt, int size) {
    return (Matcher::max_vector_size(bt) >= size &&
            Matcher::min_vector_size(bt) <= size);
  }

  // Actual max scalable vector register length.
  static const int scalable_vector_reg_size(const BasicType bt);

  // Vector ideal reg
  static const uint vector_ideal_reg(int len);

  // Vector length
  static uint vector_length(const Node* n);
  static uint vector_length(const MachNode* use, const MachOper* opnd);

  // Vector length in bytes
  static uint vector_length_in_bytes(const Node* n);
  static uint vector_length_in_bytes(const MachNode* use, const MachOper* opnd);

  // Vector element basic type
  static BasicType vector_element_basic_type(const Node* n);
  static BasicType vector_element_basic_type(const MachNode* use, const MachOper* opnd);

  // These calls are all generated by the ADLC

  // Java-Java calling convention
  // (what you use when Java calls Java)

  // Alignment of stack in bytes, standard Intel word alignment is 4.
  // Sparc probably wants at least double-word (8).
  static uint stack_alignment_in_bytes();
  // Alignment of stack, measured in stack slots.
  // The size of stack slots is defined by VMRegImpl::stack_slot_size.
  static uint stack_alignment_in_slots() {
    return stack_alignment_in_bytes() / (VMRegImpl::stack_slot_size);
  }

  // Convert a sig into a calling convention register layout
  // and find interesting things about it.
  static OptoReg::Name  find_receiver();
  // Return address register.  On Intel it is a stack-slot.  On PowerPC
  // it is the Link register.  On Sparc it is r31?
  virtual OptoReg::Name return_addr() const;
  RegMask              _return_addr_mask;
  // Return value register.  On Intel it is EAX.
  static OptoRegPair   return_value(uint ideal_reg);
  static OptoRegPair c_return_value(uint ideal_reg);
  RegMask                     _return_value_mask;
  // Inline Cache Register
  static OptoReg::Name  inline_cache_reg();
  static int            inline_cache_reg_encode();

  // Register for DIVI projection of divmodI
  static RegMask divI_proj_mask();
  // Register for MODI projection of divmodI
  static RegMask modI_proj_mask();

  // Register for DIVL projection of divmodL
  static RegMask divL_proj_mask();
  // Register for MODL projection of divmodL
  static RegMask modL_proj_mask();

  // Use hardware DIV instruction when it is faster than
  // a code which use multiply for division by constant.
  static bool use_asm_for_ldiv_by_con( jlong divisor );

  static const RegMask method_handle_invoke_SP_save_mask();

  // Java-Interpreter calling convention
  // (what you use when calling between compiled-Java and Interpreted-Java

  // Number of callee-save + always-save registers
  // Ignores frame pointer and "special" registers
  static int  number_of_saved_registers();

  // The Method-klass-holder may be passed in the inline_cache_reg
  // and then expanded into the inline_cache_reg and a method_ptr register

  // Interpreter's Frame Pointer Register
  static OptoReg::Name  interpreter_frame_pointer_reg();

  // Java-Native calling convention
  // (what you use when intercalling between Java and C++ code)

  // Frame pointer. The frame pointer is kept at the base of the stack
  // and so is probably the stack pointer for most machines.  On Intel
  // it is ESP.  On the PowerPC it is R1.  On Sparc it is SP.
  OptoReg::Name  c_frame_pointer() const;
  static RegMask c_frame_ptr_mask;

  // Java-Native vector calling convention
  static const bool supports_vector_calling_convention();
  static OptoRegPair vector_return_value(uint ideal_reg);

  // Is this branch offset small enough to be addressed by a short branch?
  bool is_short_branch_offset(int rule, int br_size, int offset);

  // Should the input 'm' of node 'n' be cloned during matching?
  // Reports back whether the node was cloned or not.
  bool    clone_node(Node* n, Node* m, Matcher::MStack& mstack);
  bool pd_clone_node(Node* n, Node* m, Matcher::MStack& mstack);

  // Should the Matcher clone shifts on addressing modes, expecting them to
  // be subsumed into complex addressing expressions or compute them into
  // registers?  True for Intel but false for most RISCs
  bool pd_clone_address_expressions(AddPNode* m, MStack& mstack, VectorSet& address_visited);
  // Clone base + offset address expression
  bool clone_base_plus_offset_address(AddPNode* m, MStack& mstack, VectorSet& address_visited);

  // Generate implicit null check for narrow oops if it can fold
  // into address expression (x64).
  //
  // [R12 + narrow_oop_reg<<3 + offset] // fold into address expression
  // NullCheck narrow_oop_reg
  //
  // When narrow oops can't fold into address expression (Sparc) and
  // base is not null use decode_not_null and normal implicit null check.
  // Note, decode_not_null node can be used here since it is referenced
  // only on non null path but it requires special handling, see
  // collect_null_checks():
  //
  // decode_not_null narrow_oop_reg, oop_reg // 'shift' and 'add base'
  // [oop_reg + offset]
  // NullCheck oop_reg
  //
  // With Zero base and when narrow oops can not fold into address
  // expression use normal implicit null check since only shift
  // is needed to decode narrow oop.
  //
  // decode narrow_oop_reg, oop_reg // only 'shift'
  // [oop_reg + offset]
  // NullCheck oop_reg
  //
  static bool gen_narrow_oop_implicit_null_checks();

 private:
  void do_postselect_cleanup();

  void specialize_generic_vector_operands();
  void specialize_mach_node(MachNode* m);
  void specialize_temp_node(MachTempNode* tmp, MachNode* use, uint idx);
  MachOper* specialize_vector_operand(MachNode* m, uint opnd_idx);

  static MachOper* pd_specialize_generic_vector_operand(MachOper* generic_opnd, uint ideal_reg, bool is_temp);
  static bool is_reg2reg_move(MachNode* m);
  static bool is_generic_vector(MachOper* opnd);

  const RegMask* regmask_for_ideal_register(uint ideal_reg, Node* ret);

  // Graph verification code
  DEBUG_ONLY( bool verify_after_postselect_cleanup(); )

 public:
  // This routine is run whenever a graph fails to match.
  // If it returns, the compiler should bailout to interpreter without error.
  // In non-product mode, SoftMatchFailure is false to detect non-canonical
  // graphs.  Print a message and exit.
  static void soft_match_failure() {
    if( SoftMatchFailure ) return;
    else { fatal("SoftMatchFailure is not allowed except in product"); }
  }

  // Check for a following volatile memory barrier without an
  // intervening load and thus we don't need a barrier here.  We
  // retain the Node to act as a compiler ordering barrier.
  static bool post_store_load_barrier(const Node* mb);

  // Does n lead to an uncommon trap that can cause deoptimization?
  static bool branches_to_uncommon_trap(const Node *n);

#ifndef PRODUCT
  // Record mach-to-Ideal mapping, reusing the Ideal IGV identifier if possible.
  void record_new2old(Node* newn, Node* old);

  void dump_old2new_map();      // machine-independent to machine-dependent

  Node* find_old_node(Node* new_node) {
    return _new2old_map[new_node->_idx];
  }
#endif // !PRODUCT
};

#endif // SHARE_OPTO_MATCHER_HPP
