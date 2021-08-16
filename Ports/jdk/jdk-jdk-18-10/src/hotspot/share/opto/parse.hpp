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

#ifndef SHARE_OPTO_PARSE_HPP
#define SHARE_OPTO_PARSE_HPP

#include "ci/ciMethodData.hpp"
#include "ci/ciTypeFlow.hpp"
#include "compiler/methodLiveness.hpp"
#include "libadt/vectset.hpp"
#include "oops/generateOopMap.hpp"
#include "opto/graphKit.hpp"
#include "opto/subnode.hpp"

class BytecodeParseHistogram;
class InlineTree;
class Parse;
class SwitchRange;


//------------------------------InlineTree-------------------------------------
class InlineTree : public ResourceObj {
  friend class VMStructs;

  Compile*    C;                  // cache
  JVMState*   _caller_jvms;       // state of caller
  ciMethod*   _method;            // method being called by the caller_jvms
  InlineTree* _caller_tree;
  uint        _count_inline_bcs;  // Accumulated count of inlined bytecodes
  const int   _max_inline_level;  // the maximum inline level for this sub-tree (may be adjusted)

  GrowableArray<InlineTree*> _subtrees;

  bool pass_initial_checks(ciMethod* caller_method, int caller_bci, ciMethod* callee_method);

  void print_impl(outputStream* stj, int indent) const PRODUCT_RETURN;
  const char* _msg;
protected:
  InlineTree(Compile* C,
             const InlineTree* caller_tree,
             ciMethod* callee_method,
             JVMState* caller_jvms,
             int caller_bci,
             int max_inline_level);
  InlineTree *build_inline_tree_for_callee(ciMethod* callee_method,
                                           JVMState* caller_jvms,
                                           int caller_bci);
  bool        try_to_inline(ciMethod* callee_method,
                            ciMethod* caller_method,
                            int caller_bci,
                            JVMState* jvms,
                            ciCallProfile& profile,
                            bool& should_delay);
  bool        should_inline(ciMethod* callee_method,
                            ciMethod* caller_method,
                            int caller_bci,
                            ciCallProfile& profile);
  bool        should_not_inline(ciMethod* callee_method,
                                ciMethod* caller_method,
                                JVMState* jvms);
  bool        is_not_reached(ciMethod* callee_method,
                             ciMethod* caller_method,
                             int caller_bci,
                             ciCallProfile& profile);
  void        print_inlining(ciMethod* callee_method, int caller_bci,
                             ciMethod* caller_method, bool success) const;

  InlineTree* caller_tree()       const { return _caller_tree;  }
  InlineTree* callee_at(int bci, ciMethod* m) const;
  int         inline_level()      const { return stack_depth(); }
  int         stack_depth()       const { return _caller_jvms ? _caller_jvms->depth() : 0; }
  const char* msg()               const { return _msg; }
  void        set_msg(const char* msg)  { _msg = msg; }
public:
  static const char* check_can_parse(ciMethod* callee);

  static InlineTree* build_inline_tree_root();
  static InlineTree* find_subtree_from_root(InlineTree* root, JVMState* jvms, ciMethod* callee);

  // See if it is OK to inline.
  // The receiver is the inline tree for the caller.
  //
  // The result is a temperature indication.  If it is hot or cold,
  // inlining is immediate or undesirable.  Otherwise, the info block
  // returned is newly allocated and may be enqueued.
  //
  // If the method is inlinable, a new inline subtree is created on the fly,
  // and may be accessed by find_subtree_from_root.
  // The call_method is the dest_method for a special or static invocation.
  // The call_method is an optimized virtual method candidate otherwise.
  bool ok_to_inline(ciMethod *call_method, JVMState* caller_jvms, ciCallProfile& profile, bool& should_delay);

  // Information about inlined method
  JVMState*   caller_jvms()       const { return _caller_jvms; }
  ciMethod   *method()            const { return _method; }
  int         caller_bci()        const { return _caller_jvms ? _caller_jvms->bci() : InvocationEntryBci; }
  uint        count_inline_bcs()  const { return _count_inline_bcs; }

#ifndef PRODUCT
private:
  uint        _count_inlines;     // Count of inlined methods
public:
  // Debug information collected during parse
  uint        count_inlines()     const { return _count_inlines; };
#endif
  GrowableArray<InlineTree*> subtrees() { return _subtrees; }

  void print_value_on(outputStream* st) const PRODUCT_RETURN;

  bool        _forced_inline;     // Inlining was forced by CompilerOracle, ciReplay or annotation
  bool        forced_inline()     const { return _forced_inline; }
  // Count number of nodes in this subtree
  int         count() const;
  // Dump inlining replay data to the stream.
  void dump_replay_data(outputStream* out);
};


//-----------------------------------------------------------------------------
//------------------------------Parse------------------------------------------
// Parse bytecodes, build a Graph
class Parse : public GraphKit {
 public:
  // Per-block information needed by the parser:
  class Block {
   private:
    ciTypeFlow::Block* _flow;
    int                _pred_count;     // how many predecessors in CFG?
    int                _preds_parsed;   // how many of these have been parsed?
    uint               _count;          // how many times executed?  Currently only set by _goto's
    bool               _is_parsed;      // has this block been parsed yet?
    bool               _is_handler;     // is this block an exception handler?
    bool               _has_merged_backedge; // does this block have merged backedge?
    SafePointNode*     _start_map;      // all values flowing into this block
    MethodLivenessResult _live_locals;  // lazily initialized liveness bitmap
    bool               _has_predicates; // Were predicates added before parsing of the loop head?

    int                _num_successors; // Includes only normal control flow.
    int                _all_successors; // Include exception paths also.
    Block**            _successors;

   public:

    // Set up the block data structure itself.
    Block(Parse* outer, int rpo);

    // Set up the block's relations to other blocks.
    void init_graph(Parse* outer);

    ciTypeFlow::Block* flow() const        { return _flow; }
    int pred_count() const                 { return _pred_count; }
    int preds_parsed() const               { return _preds_parsed; }
    bool is_parsed() const                 { return _is_parsed; }
    bool is_handler() const                { return _is_handler; }
    void set_count( uint x )               { _count = x; }
    uint count() const                     { return _count; }

    SafePointNode* start_map() const       { assert(is_merged(),"");   return _start_map; }
    void set_start_map(SafePointNode* m)   { assert(!is_merged(), ""); _start_map = m; }

    // True after any predecessor flows control into this block
    bool is_merged() const                 { return _start_map != NULL; }

#ifdef ASSERT
    // True after backedge predecessor flows control into this block
    bool has_merged_backedge() const       { return _has_merged_backedge; }
    void mark_merged_backedge(Block* pred) {
      assert(is_SEL_head(), "should be loop head");
      if (pred != NULL && is_SEL_backedge(pred)) {
        assert(is_parsed(), "block should be parsed before merging backedges");
        _has_merged_backedge = true;
      }
    }
#endif

    // True when all non-exception predecessors have been parsed.
    bool is_ready() const                  { return preds_parsed() == pred_count(); }

    bool has_predicates() const            { return _has_predicates; }
    void set_has_predicates()              { _has_predicates = true; }

    int num_successors() const             { return _num_successors; }
    int all_successors() const             { return _all_successors; }
    Block* successor_at(int i) const {
      assert((uint)i < (uint)all_successors(), "");
      return _successors[i];
    }
    Block* successor_for_bci(int bci);

    int start() const                      { return flow()->start(); }
    int limit() const                      { return flow()->limit(); }
    int rpo() const                        { return flow()->rpo(); }
    int start_sp() const                   { return flow()->stack_size(); }

    bool is_loop_head() const              { return flow()->is_loop_head(); }
    bool is_SEL_head() const               { return flow()->is_single_entry_loop_head(); }
    bool is_SEL_backedge(Block* pred) const{ return is_SEL_head() && pred->rpo() >= rpo(); }
    bool is_invariant_local(uint i) const  {
      const JVMState* jvms = start_map()->jvms();
      if (!jvms->is_loc(i) || flow()->outer()->has_irreducible_entry()) return false;
      return flow()->is_invariant_local(i - jvms->locoff());
    }
    bool can_elide_SEL_phi(uint i) const  { assert(is_SEL_head(),""); return is_invariant_local(i); }

    const Type* peek(int off=0) const      { return stack_type_at(start_sp() - (off+1)); }

    const Type* stack_type_at(int i) const;
    const Type* local_type_at(int i) const;
    static const Type* get_type(ciType* t) { return Type::get_typeflow_type(t); }

    bool has_trap_at(int bci) const        { return flow()->has_trap() && flow()->trap_bci() == bci; }

    // Call this just before parsing a block.
    void mark_parsed() {
      assert(!_is_parsed, "must parse each block exactly once");
      _is_parsed = true;
    }

    // Return the phi/region input index for the "current" pred,
    // and bump the pred number.  For historical reasons these index
    // numbers are handed out in descending order.  The last index is
    // always PhiNode::Input (i.e., 1).  The value returned is known
    // as a "path number" because it distinguishes by which path we are
    // entering the block.
    int next_path_num() {
      assert(preds_parsed() < pred_count(), "too many preds?");
      return pred_count() - _preds_parsed++;
    }

    // Add a previously unaccounted predecessor to this block.
    // This operates by increasing the size of the block's region
    // and all its phi nodes (if any).  The value returned is a
    // path number ("pnum").
    int add_new_path();

    // Initialize me by recording the parser's map.  My own map must be NULL.
    void record_state(Parse* outer);
  };

#ifndef PRODUCT
  // BytecodeParseHistogram collects number of bytecodes parsed, nodes constructed, and transformations.
  class BytecodeParseHistogram : public ResourceObj {
   private:
    enum BPHType {
      BPH_transforms,
      BPH_values
    };
    static bool _initialized;
    static uint _bytecodes_parsed [Bytecodes::number_of_codes];
    static uint _nodes_constructed[Bytecodes::number_of_codes];
    static uint _nodes_transformed[Bytecodes::number_of_codes];
    static uint _new_values       [Bytecodes::number_of_codes];

    Bytecodes::Code _initial_bytecode;
    int             _initial_node_count;
    int             _initial_transforms;
    int             _initial_values;

    Parse     *_parser;
    Compile   *_compiler;

    // Initialization
    static void reset();

    // Return info being collected, select with global flag 'BytecodeParseInfo'
    int current_count(BPHType info_selector);

   public:
    BytecodeParseHistogram(Parse *p, Compile *c);
    static bool initialized();

    // Record info when starting to parse one bytecode
    void set_initial_state( Bytecodes::Code bc );
    // Record results of parsing one bytecode
    void record_change();

    // Profile printing
    static void print(float cutoff = 0.01F); // cutoff in percent
  };

  public:
    // Record work done during parsing
    BytecodeParseHistogram* _parse_histogram;
    void set_parse_histogram(BytecodeParseHistogram *bph) { _parse_histogram = bph; }
    BytecodeParseHistogram* parse_histogram()      { return _parse_histogram; }
#endif

 private:
  friend class Block;

  // Variables which characterize this compilation as a whole:

  JVMState*     _caller;        // JVMS which carries incoming args & state.
  float         _expected_uses; // expected number of calls to this code
  float         _prof_factor;   // discount applied to my profile counts
  int           _depth;         // Inline tree depth, for debug printouts
  const TypeFunc*_tf;           // My kind of function type
  int           _entry_bci;     // the osr bci or InvocationEntryBci

  ciTypeFlow*   _flow;          // Results of previous flow pass.
  Block*        _blocks;        // Array of basic-block structs.
  int           _block_count;   // Number of elements in _blocks.

  GraphKit      _exits;         // Record all normal returns and throws here.
  bool          _wrote_final;   // Did we write a final field?
  bool          _wrote_volatile;     // Did we write a volatile field?
  bool          _wrote_stable;       // Did we write a @Stable field?
  bool          _wrote_fields;       // Did we write any field?
  Node*         _alloc_with_final;   // An allocation node with final field

  // Variables which track Java semantics during bytecode parsing:

  Block*            _block;     // block currently getting parsed
  ciBytecodeStream  _iter;      // stream of this method's bytecodes

  const FastLockNode* _synch_lock; // FastLockNode for synchronized method

#ifndef PRODUCT
  int _max_switch_depth;        // Debugging SwitchRanges.
  int _est_switch_depth;        // Debugging SwitchRanges.
#endif

  bool         _first_return;                  // true if return is the first to be parsed
  bool         _replaced_nodes_for_exceptions; // needs processing of replaced nodes in exception paths?
  uint         _new_idx;                       // any node with _idx above were new during this parsing. Used to trim the replaced nodes list.

 public:
  // Constructor
  Parse(JVMState* caller, ciMethod* parse_method, float expected_uses);

  virtual Parse* is_Parse() const { return (Parse*)this; }

  // Accessors.
  JVMState*     caller()        const { return _caller; }
  float         expected_uses() const { return _expected_uses; }
  float         prof_factor()   const { return _prof_factor; }
  int           depth()         const { return _depth; }
  const TypeFunc* tf()          const { return _tf; }
  //            entry_bci()     -- see osr_bci, etc.

  ciTypeFlow*   flow()          const { return _flow; }
  //            blocks()        -- see rpo_at, start_block, etc.
  int           block_count()   const { return _block_count; }

  GraphKit&     exits()               { return _exits; }
  bool          wrote_final() const   { return _wrote_final; }
  void      set_wrote_final(bool z)   { _wrote_final = z; }
  bool          wrote_volatile() const { return _wrote_volatile; }
  void      set_wrote_volatile(bool z) { _wrote_volatile = z; }
  bool          wrote_stable() const  { return _wrote_stable; }
  void      set_wrote_stable(bool z)  { _wrote_stable = z; }
  bool         wrote_fields() const   { return _wrote_fields; }
  void     set_wrote_fields(bool z)   { _wrote_fields = z; }
  Node*    alloc_with_final() const   { return _alloc_with_final; }
  void set_alloc_with_final(Node* n)  {
    assert((_alloc_with_final == NULL) || (_alloc_with_final == n), "different init objects?");
    _alloc_with_final = n;
  }

  Block*             block()    const { return _block; }
  ciBytecodeStream&  iter()           { return _iter; }
  Bytecodes::Code    bc()       const { return _iter.cur_bc(); }

  void set_block(Block* b)            { _block = b; }

  // Derived accessors:
  bool is_normal_parse() const  { return _entry_bci == InvocationEntryBci; }
  bool is_osr_parse() const     { return _entry_bci != InvocationEntryBci; }
  int osr_bci() const           { assert(is_osr_parse(),""); return _entry_bci; }

  void set_parse_bci(int bci);

  // Must this parse be aborted?
  bool failing()                { return C->failing(); }

  Block* rpo_at(int rpo) {
    assert(0 <= rpo && rpo < _block_count, "oob");
    return &_blocks[rpo];
  }
  Block* start_block() {
    return rpo_at(flow()->start_block()->rpo());
  }
  // Can return NULL if the flow pass did not complete a block.
  Block* successor_for_bci(int bci) {
    return block()->successor_for_bci(bci);
  }

 private:
  // Create a JVMS & map for the initial state of this method.
  SafePointNode* create_entry_map();

  // OSR helpers
  Node *fetch_interpreter_state(int index, BasicType bt, Node *local_addrs, Node *local_addrs_base);
  Node* check_interpreter_type(Node* l, const Type* type, SafePointNode* &bad_type_exit);
  void  load_interpreter_state(Node* osr_buf);

  // Functions for managing basic blocks:
  void init_blocks();
  void load_state_from(Block* b);
  void store_state_to(Block* b) { b->record_state(this); }

  // Parse all the basic blocks.
  void do_all_blocks();

  // Parse the current basic block
  void do_one_block();

  // Raise an error if we get a bad ciTypeFlow CFG.
  void handle_missing_successor(int bci);

  // first actions (before BCI 0)
  void do_method_entry();

  // implementation of monitorenter/monitorexit
  void do_monitor_enter();
  void do_monitor_exit();

  // Eagerly create phie throughout the state, to cope with back edges.
  void ensure_phis_everywhere();

  // Merge the current mapping into the basic block starting at bci
  void merge(          int target_bci);
  // Same as plain merge, except that it allocates a new path number.
  void merge_new_path( int target_bci);
  // Merge the current mapping into an exception handler.
  void merge_exception(int target_bci);
  // Helper: Merge the current mapping into the given basic block
  void merge_common(Block* target, int pnum);
  // Helper functions for merging individual cells.
  PhiNode *ensure_phi(       int idx, bool nocreate = false);
  PhiNode *ensure_memory_phi(int idx, bool nocreate = false);
  // Helper to merge the current memory state into the given basic block
  void merge_memory_edges(MergeMemNode* n, int pnum, bool nophi);

  // Parse this bytecode, and alter the Parsers JVM->Node mapping
  void do_one_bytecode();

  // helper function to generate array store check
  void array_store_check();
  // Helper function to generate array load
  void array_load(BasicType etype);
  // Helper function to generate array store
  void array_store(BasicType etype);
  // Helper function to compute array addressing
  Node* array_addressing(BasicType type, int vals, const Type*& elemtype);

  void clinit_deopt();

  void rtm_deopt();

  // Pass current map to exits
  void return_current(Node* value);

  // Register finalizers on return from Object.<init>
  void call_register_finalizer();

  // Insert a compiler safepoint into the graph
  void add_safepoint();

  // Insert a compiler safepoint into the graph, if there is a back-branch.
  void maybe_add_safepoint(int target_bci) {
    if (target_bci <= bci()) {
      add_safepoint();
    }
  }

  // Note:  Intrinsic generation routines may be found in library_call.cpp.

  // Helper function to setup Ideal Call nodes
  void do_call();

  // Helper function to uncommon-trap or bailout for non-compilable call-sites
  bool can_not_compile_call_site(ciMethod *dest_method, ciInstanceKlass *klass);

  // Helper functions for type checking bytecodes:
  void  do_checkcast();
  void  do_instanceof();

  // Helper functions for shifting & arithmetic
  void modf();
  void modd();
  void l2f();

  // implementation of _get* and _put* bytecodes
  void do_getstatic() { do_field_access(true,  false); }
  void do_getfield () { do_field_access(true,  true); }
  void do_putstatic() { do_field_access(false, false); }
  void do_putfield () { do_field_access(false, true); }

  // common code for making initial checks and forming addresses
  void do_field_access(bool is_get, bool is_field);

  // common code for actually performing the load or store
  void do_get_xxx(Node* obj, ciField* field, bool is_field);
  void do_put_xxx(Node* obj, ciField* field, bool is_field);

  // implementation of object creation bytecodes
  void do_new();
  void do_newarray(BasicType elemtype);
  void do_anewarray();
  void do_multianewarray();
  Node* expand_multianewarray(ciArrayKlass* array_klass, Node* *lengths, int ndimensions, int nargs);

  // implementation of jsr/ret
  void do_jsr();
  void do_ret();

  float   dynamic_branch_prediction(float &cnt, BoolTest::mask btest, Node* test);
  float   branch_prediction(float &cnt, BoolTest::mask btest, int target_bci, Node* test);
  bool    seems_never_taken(float prob) const;
  bool    path_is_suitable_for_uncommon_trap(float prob) const;
  bool    seems_stable_comparison() const;

  void    do_ifnull(BoolTest::mask btest, Node* c);
  void    do_if(BoolTest::mask btest, Node* c);
  int     repush_if_args();
  void    adjust_map_after_if(BoolTest::mask btest, Node* c, float prob,
                              Block* path, Block* other_path);
  void    sharpen_type_after_if(BoolTest::mask btest,
                                Node* con, const Type* tcon,
                                Node* val, const Type* tval);
  void    maybe_add_predicate_after_if(Block* path);
  IfNode* jump_if_fork_int(Node* a, Node* b, BoolTest::mask mask, float prob, float cnt);
  void    jump_if_true_fork(IfNode *ifNode, int dest_bci_if_true, bool unc);
  void    jump_if_false_fork(IfNode *ifNode, int dest_bci_if_false, bool unc);
  void    jump_if_always_fork(int dest_bci_if_true, bool unc);

  friend class SwitchRange;
  void    do_tableswitch();
  void    do_lookupswitch();
  void    jump_switch_ranges(Node* a, SwitchRange* lo, SwitchRange* hi, int depth = 0);
  bool    create_jump_tables(Node* a, SwitchRange* lo, SwitchRange* hi);
  void    linear_search_switch_ranges(Node* key_val, SwitchRange*& lo, SwitchRange*& hi);

  void decrement_age();

  // helper function for call statistics
  void count_compiled_calls(bool at_method_entry, bool is_inline) PRODUCT_RETURN;

  Node_Notes* make_node_notes(Node_Notes* caller_nn);

  // Helper functions for handling normal and abnormal exits.
  void build_exits();

  // Fix up all exceptional control flow exiting a single bytecode.
  void do_exceptions();

  // Fix up all exiting control flow at the end of the parse.
  void do_exits();

  // Add Catch/CatchProjs
  // The call is either a Java call or the VM's rethrow stub
  void catch_call_exceptions(ciExceptionHandlerStream&);

  // Handle all exceptions thrown by the inlined method.
  // Also handles exceptions for individual bytecodes.
  void catch_inline_exceptions(SafePointNode* ex_map);

  // Merge the given map into correct exceptional exit state.
  // Assumes that there is no applicable local handler.
  void throw_to_exit(SafePointNode* ex_map);

  // Use speculative type to optimize CmpP node
  Node* optimize_cmp_with_klass(Node* c);

 public:
#ifndef PRODUCT
  // Handle PrintOpto, etc.
  void show_parse_info();
  void dump_map_adr_mem() const;
  static void print_statistics(); // Print some performance counters
  void dump();
  void dump_bci(int bci);
#endif
};

#endif // SHARE_OPTO_PARSE_HPP
