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

#ifndef SHARE_OOPS_GENERATEOOPMAP_HPP
#define SHARE_OOPS_GENERATEOOPMAP_HPP

#include "interpreter/bytecodeStream.hpp"
#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/signature.hpp"
#include "utilities/bitMap.hpp"

// Forward definition
class GenerateOopMap;
class BasicBlock;
class CellTypeState;
class StackMap;

// These two should be removed. But requires som code to be cleaned up
#define MAXARGSIZE      256      // This should be enough
#define MAX_LOCAL_VARS  65536    // 16-bit entry

typedef void (*jmpFct_t)(GenerateOopMap *c, int bcpDelta, int* data);


//  RetTable
//
// Contains maping between jsr targets and there return addresses. One-to-many mapping
//
class RetTableEntry : public ResourceObj {
 private:
  static int _init_nof_jsrs;                      // Default size of jsrs list
  int _target_bci;                                // Target PC address of jump (bytecode index)
  GrowableArray<intptr_t> * _jsrs;                     // List of return addresses  (bytecode index)
  RetTableEntry *_next;                           // Link to next entry
 public:
   RetTableEntry(int target, RetTableEntry *next);

  // Query
  int target_bci() const                      { return _target_bci; }
  int nof_jsrs() const                        { return _jsrs->length(); }
  int jsrs(int i) const                       { assert(i>=0 && i<nof_jsrs(), "Index out of bounds"); return _jsrs->at(i); }

  // Update entry
  void add_jsr    (int return_bci)            { _jsrs->append(return_bci); }
  void add_delta  (int bci, int delta);
  RetTableEntry * next()  const               { return _next; }
};


class RetTable {
 private:
  RetTableEntry *_first;
  static int _init_nof_entries;

  void add_jsr(int return_bci, int target_bci);   // Adds entry to list
 public:
  RetTable()                                                  { _first = NULL; }
  void compute_ret_table(const methodHandle& method);
  void update_ret_table(int bci, int delta);
  RetTableEntry* find_jsrs_for_target(int targBci);
};

//
// CellTypeState
//
class CellTypeState {
 private:
  unsigned int _state;

  // Masks for separating the BITS and INFO portions of a CellTypeState
  enum { info_mask            = right_n_bits(28),
         bits_mask            = (int)(~info_mask) };

  // These constant are used for manipulating the BITS portion of a
  // CellTypeState
  enum { uninit_bit           = (int)(nth_bit(31)),
         ref_bit              = nth_bit(30),
         val_bit              = nth_bit(29),
         addr_bit             = nth_bit(28),
         live_bits_mask       = (int)(bits_mask & ~uninit_bit) };

  // These constants are used for manipulating the INFO portion of a
  // CellTypeState
  enum { top_info_bit         = nth_bit(27),
         not_bottom_info_bit  = nth_bit(26),
         info_data_mask       = right_n_bits(26),
         info_conflict        = info_mask };

  // Within the INFO data, these values are used to distinguish different
  // kinds of references.
  enum { ref_not_lock_bit     = nth_bit(25),  // 0 if this reference is locked as a monitor
         ref_slot_bit         = nth_bit(24),  // 1 if this reference is a "slot" reference,
                                              // 0 if it is a "line" reference.
         ref_data_mask        = right_n_bits(24) };


  // These values are used to initialize commonly used CellTypeState
  // constants.
  enum { bottom_value         = 0,
         uninit_value         = (int)(uninit_bit | info_conflict),
         ref_value            = ref_bit,
         ref_conflict         = ref_bit | info_conflict,
         val_value            = val_bit | info_conflict,
         addr_value           = addr_bit,
         addr_conflict        = addr_bit | info_conflict };

 public:

  // Since some C++ constructors generate poor code for declarations of the
  // form...
  //
  //   CellTypeState vector[length];
  //
  // ...we avoid making a constructor for this class.  CellTypeState values
  // should be constructed using one of the make_* methods:

  static CellTypeState make_any(int state) {
    CellTypeState s;
    s._state = state;
    // Causes SS10 warning.
    // assert(s.is_valid_state(), "check to see if CellTypeState is valid");
    return s;
  }

  static CellTypeState make_bottom() {
    return make_any(0);
  }

  static CellTypeState make_top() {
    return make_any(AllBits);
  }

  static CellTypeState make_addr(int bci) {
    assert((bci >= 0) && (bci < info_data_mask), "check to see if ret addr is valid");
    return make_any(addr_bit | not_bottom_info_bit | (bci & info_data_mask));
  }

  static CellTypeState make_slot_ref(int slot_num) {
    assert(slot_num >= 0 && slot_num < ref_data_mask, "slot out of range");
    return make_any(ref_bit | not_bottom_info_bit | ref_not_lock_bit | ref_slot_bit |
                    (slot_num & ref_data_mask));
  }

  static CellTypeState make_line_ref(int bci) {
    assert(bci >= 0 && bci < ref_data_mask, "line out of range");
    return make_any(ref_bit | not_bottom_info_bit | ref_not_lock_bit |
                    (bci & ref_data_mask));
  }

  static CellTypeState make_lock_ref(int bci) {
    assert(bci >= 0 && bci < ref_data_mask, "line out of range");
    return make_any(ref_bit | not_bottom_info_bit | (bci & ref_data_mask));
  }

  // Query methods:
  bool is_bottom() const                { return _state == 0; }
  bool is_live() const                  { return ((_state & live_bits_mask) != 0); }
  bool is_valid_state() const {
    // Uninitialized and value cells must contain no data in their info field:
    if ((can_be_uninit() || can_be_value()) && !is_info_top()) {
      return false;
    }
    // The top bit is only set when all info bits are set:
    if (is_info_top() && ((_state & info_mask) != info_mask)) {
      return false;
    }
    // The not_bottom_bit must be set when any other info bit is set:
    if (is_info_bottom() && ((_state & info_mask) != 0)) {
      return false;
    }
    return true;
  }

  bool is_address() const               { return ((_state & bits_mask) == addr_bit); }
  bool is_reference() const             { return ((_state & bits_mask) == ref_bit); }
  bool is_value() const                 { return ((_state & bits_mask) == val_bit); }
  bool is_uninit() const                { return ((_state & bits_mask) == (uint)uninit_bit); }

  bool can_be_address() const           { return ((_state & addr_bit) != 0); }
  bool can_be_reference() const         { return ((_state & ref_bit) != 0); }
  bool can_be_value() const             { return ((_state & val_bit) != 0); }
  bool can_be_uninit() const            { return ((_state & uninit_bit) != 0); }

  bool is_info_bottom() const           { return ((_state & not_bottom_info_bit) == 0); }
  bool is_info_top() const              { return ((_state & top_info_bit) != 0); }
  int  get_info() const {
    assert((!is_info_top() && !is_info_bottom()),
           "check to make sure top/bottom info is not used");
    return (_state & info_data_mask);
  }

  bool is_good_address() const          { return is_address() && !is_info_top(); }
  bool is_lock_reference() const {
    return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == ref_bit);
  }
  bool is_nonlock_reference() const {
    return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == (ref_bit | ref_not_lock_bit));
  }

  bool equal(CellTypeState a) const     { return _state == a._state; }
  bool equal_kind(CellTypeState a) const {
    return (_state & bits_mask) == (a._state & bits_mask);
  }

  char to_char() const;

  // Merge
  CellTypeState merge (CellTypeState cts, int slot) const;

  // Debugging output
  void print(outputStream *os);

  // Default values of common values
  static CellTypeState bottom;
  static CellTypeState uninit;
  static CellTypeState ref;
  static CellTypeState value;
  static CellTypeState refUninit;
  static CellTypeState varUninit;
  static CellTypeState top;
  static CellTypeState addr;
};


//
// BasicBlockStruct
//
class BasicBlock: ResourceObj {
 private:
  bool            _changed;                 // Reached a fixpoint or not
 public:
  enum Constants {
    _dead_basic_block = -2,
    _unreached        = -1                  // Alive but not yet reached by analysis
    // >=0                                  // Alive and has a merged state
  };

  int             _bci;                     // Start of basic block
  int             _end_bci;                 // Bci of last instruction in basicblock
  int             _max_locals;              // Determines split between vars and stack
  int             _max_stack;               // Determines split between stack and monitors
  CellTypeState*  _state;                   // State (vars, stack) at entry.
  int             _stack_top;               // -1 indicates bottom stack value.
  int             _monitor_top;             // -1 indicates bottom monitor stack value.

  CellTypeState* vars()                     { return _state; }
  CellTypeState* stack()                    { return _state + _max_locals; }

  bool changed()                            { return _changed; }
  void set_changed(bool s)                  { _changed = s; }

  bool is_reachable() const                 { return _stack_top >= 0; }  // Analysis has reached this basicblock

  // All basicblocks that are unreachable are going to have a _stack_top == _dead_basic_block.
  // This info. is setup in a pre-parse before the real abstract interpretation starts.
  bool is_dead() const                      { return _stack_top == _dead_basic_block; }
  bool is_alive() const                     { return _stack_top != _dead_basic_block; }
  void mark_as_alive()                      { assert(is_dead(), "must be dead"); _stack_top = _unreached; }
};


//
//  GenerateOopMap
//
// Main class used to compute the pointer-maps in a Method
//
class GenerateOopMap {
 protected:

  // _monitor_top is set to this constant to indicate that a monitor matching
  // problem was encountered prior to this point in control flow.
  enum { bad_monitors = -1 };

  // Main variables
  methodHandle _method;                     // The method we are examine
  RetTable     _rt;                         // Contains the return address mappings
  int          _max_locals;                 // Cached value of no. of locals
  int          _max_stack;                  // Cached value of max. stack depth
  int          _max_monitors;               // Cached value of max. monitor stack depth
  int          _has_exceptions;             // True, if exceptions exist for method
  bool         _got_error;                  // True, if an error occurred during interpretation.
  Handle       _exception;                  // Exception if got_error is true.
  bool         _did_rewriting;              // was bytecodes rewritten
  bool         _did_relocation;             // was relocation neccessary
  bool         _monitor_safe;               // The monitors in this method have been determined
                                            // to be safe.

  // Working Cell type state
  int            _state_len;                // Size of states
  CellTypeState *_state;                    // list of states
  char          *_state_vec_buf;            // Buffer used to print a readable version of a state
  int            _stack_top;
  int            _monitor_top;

  // Timing and statistics
  static elapsedTimer _total_oopmap_time;   // Holds cumulative oopmap generation time
  static uint64_t     _total_byte_count;    // Holds cumulative number of bytes inspected

  // Cell type methods
  void            init_state();
  void            make_context_uninitialized ();
  int             methodsig_to_effect        (Symbol* signature, bool isStatic, CellTypeState* effect);
  bool            merge_local_state_vectors  (CellTypeState* cts, CellTypeState* bbts);
  bool            merge_monitor_state_vectors(CellTypeState* cts, CellTypeState* bbts);
  void            copy_state                 (CellTypeState *dst, CellTypeState *src);
  void            merge_state_into_bb        (BasicBlock *bb);
  static void     merge_state                (GenerateOopMap *gom, int bcidelta, int* data);
  void            set_var                    (int localNo, CellTypeState cts);
  CellTypeState   get_var                    (int localNo);
  CellTypeState   pop                        ();
  void            push                       (CellTypeState cts);
  CellTypeState   monitor_pop                ();
  void            monitor_push               (CellTypeState cts);
  CellTypeState * vars                       ()                                             { return _state; }
  CellTypeState * stack                      ()                                             { return _state+_max_locals; }
  CellTypeState * monitors                   ()                                             { return _state+_max_locals+_max_stack; }

  void            replace_all_CTS_matches    (CellTypeState match,
                                              CellTypeState replace);
  void            print_states               (outputStream *os, CellTypeState *vector, int num);
  void            print_current_state        (outputStream   *os,
                                              BytecodeStream *itr,
                                              bool            detailed);
  void            report_monitor_mismatch    (const char *msg);

  // Basicblock info
  BasicBlock *    _basic_blocks;             // Array of basicblock info
  int             _gc_points;
  int             _bb_count;
  ResourceBitMap  _bb_hdr_bits;

  // Basicblocks methods
  void          initialize_bb               ();
  void          mark_bbheaders_and_count_gc_points();
  bool          is_bb_header                (int bci) const   {
    return _bb_hdr_bits.at(bci);
  }
  int           gc_points                   () const                          { return _gc_points; }
  int           bb_count                    () const                          { return _bb_count; }
  void          set_bbmark_bit              (int bci);
  BasicBlock *  get_basic_block_at          (int bci) const;
  BasicBlock *  get_basic_block_containing  (int bci) const;
  void          interp_bb                   (BasicBlock *bb);
  void          restore_state               (BasicBlock *bb);
  int           next_bb_start_pc            (BasicBlock *bb);
  void          update_basic_blocks         (int bci, int delta, int new_method_size);
  static void   bb_mark_fct                 (GenerateOopMap *c, int deltaBci, int *data);

  // Dead code detection
  void          mark_reachable_code();
  static void   reachable_basicblock        (GenerateOopMap *c, int deltaBci, int *data);

  // Interpretation methods (primary)
  void  do_interpretation                   ();
  void  init_basic_blocks                   ();
  void  setup_method_entry_state            ();
  void  interp_all                          ();

  // Interpretation methods (secondary)
  void  interp1                             (BytecodeStream *itr);
  void  do_exception_edge                   (BytecodeStream *itr);
  void  check_type                          (CellTypeState expected, CellTypeState actual);
  void  ppstore                             (CellTypeState *in,  int loc_no);
  void  ppload                              (CellTypeState *out, int loc_no);
  void  ppush1                              (CellTypeState in);
  void  ppush                               (CellTypeState *in);
  void  ppop1                               (CellTypeState out);
  void  ppop                                (CellTypeState *out);
  void  ppop_any                            (int poplen);
  void  pp                                  (CellTypeState *in, CellTypeState *out);
  void  pp_new_ref                          (CellTypeState *in, int bci);
  void  ppdupswap                           (int poplen, const char *out);
  void  do_ldc                              (int bci);
  void  do_astore                           (int idx);
  void  do_jsr                              (int delta);
  void  do_field                            (int is_get, int is_static, int idx, int bci);
  void  do_method                           (int is_static, int is_interface, int idx, int bci);
  void  do_multianewarray                   (int dims, int bci);
  void  do_monitorenter                     (int bci);
  void  do_monitorexit                      (int bci);
  void  do_return_monitor_check             ();
  void  do_checkcast                        ();
  CellTypeState *signature_to_effect        (const Symbol* sig, int bci, CellTypeState *out);
  int copy_cts                              (CellTypeState *dst, CellTypeState *src);

  // Error handling
  void  error_work                          (const char *format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
  void  report_error                        (const char *format, ...) ATTRIBUTE_PRINTF(2, 3);
  void  verify_error                        (const char *format, ...) ATTRIBUTE_PRINTF(2, 3);
  bool  got_error()                         { return _got_error; }

  // Create result set
  bool  _report_result;
  bool  _report_result_for_send;            // Unfortunatly, stackmaps for sends are special, so we need some extra
  BytecodeStream *_itr_send;                // variables to handle them properly.

  void  report_result                       ();

  // Initvars
  GrowableArray<intptr_t> * _init_vars;

  void  initialize_vars                     ();
  void  add_to_ref_init_set                 (int localNo);

  // Conflicts rewrite logic
  bool      _conflict;                      // True, if a conflict occurred during interpretation
  int       _nof_refval_conflicts;          // No. of conflicts that require rewrites
  int *     _new_var_map;

  void record_refval_conflict               (int varNo);
  void rewrite_refval_conflicts             ();
  void rewrite_refval_conflict              (int from, int to);
  bool rewrite_refval_conflict_inst         (BytecodeStream *i, int from, int to);
  bool rewrite_load_or_store                (BytecodeStream *i, Bytecodes::Code bc, Bytecodes::Code bc0, unsigned int varNo);

  void expand_current_instr                 (int bci, int ilen, int newIlen, u_char inst_buffer[]);
  bool is_astore                            (BytecodeStream *itr, int *index);
  bool is_aload                             (BytecodeStream *itr, int *index);

  // List of bci's where a return address is on top of the stack
  GrowableArray<intptr_t> *_ret_adr_tos;

  bool stack_top_holds_ret_addr             (int bci);
  void compute_ret_adr_at_TOS               ();
  void update_ret_adr_at_TOS                (int bci, int delta);

  int  binsToHold                           (int no)                      { return  ((no+(BitsPerWord-1))/BitsPerWord); }
  char *state_vec_to_string                 (CellTypeState* vec, int len);

  // Helper method. Can be used in subclasses to fx. calculate gc_points. If the current instuction
  // is a control transfer, then calls the jmpFct all possible destinations.
  void  ret_jump_targets_do                 (BytecodeStream *bcs, jmpFct_t jmpFct, int varNo,int *data);
  bool  jump_targets_do                     (BytecodeStream *bcs, jmpFct_t jmpFct, int *data);

  friend class RelocCallback;
 public:
  GenerateOopMap(const methodHandle& method);

  // Compute the map - returns true on success and false on error.
  bool compute_map(Thread* current);
  // Returns the exception related to any error, if the map was computed by a suitable JavaThread.
  Handle exception() { return _exception; }

  void result_for_basicblock(int bci);    // Do a callback on fill_stackmap_for_opcodes for basicblock containing bci

  // Query
  int max_locals() const                           { return _max_locals; }
  Method* method() const                           { return _method(); }
  methodHandle method_as_handle() const            { return _method; }

  bool did_rewriting()                             { return _did_rewriting; }
  bool did_relocation()                            { return _did_relocation; }

  static void print_time();

  // Monitor query
  bool monitor_safe()                              { return _monitor_safe; }

  // Specialization methods. Intended use:
  // - possible_gc_point must return true for every bci for which the stackmaps must be returned
  // - fill_stackmap_prolog is called just before the result is reported. The arguments tells the estimated
  //   number of gc points
  // - fill_stackmap_for_opcodes is called once for each bytecode index in order (0...code_length-1)
  // - fill_stackmap_epilog is called after all results has been reported. Note: Since the algorithm does not report
  //   stackmaps for deadcode, fewer gc_points might have been encounted than assumed during the epilog. It is the
  //   responsibility of the subclass to count the correct number.
  // - fill_init_vars are called once with the result of the init_vars computation
  //
  // All these methods are used during a call to: compute_map. Note: Non of the return results are valid
  // after compute_map returns, since all values are allocated as resource objects.
  //
  // All virtual method must be implemented in subclasses
  virtual bool allow_rewrites             () const                        { return false; }
  virtual bool report_results             () const                        { return true;  }
  virtual bool report_init_vars           () const                        { return true;  }
  virtual bool possible_gc_point          (BytecodeStream *bcs)           { ShouldNotReachHere(); return false; }
  virtual void fill_stackmap_prolog       (int nof_gc_points)             { ShouldNotReachHere(); }
  virtual void fill_stackmap_epilog       ()                              { ShouldNotReachHere(); }
  virtual void fill_stackmap_for_opcodes  (BytecodeStream *bcs,
                                           CellTypeState* vars,
                                           CellTypeState* stack,
                                           int stackTop)                  { ShouldNotReachHere(); }
  virtual void fill_init_vars             (GrowableArray<intptr_t> *init_vars) { ShouldNotReachHere();; }
};

//
// Subclass of the GenerateOopMap Class that just do rewrites of the method, if needed.
// It does not store any oopmaps.
//
class ResolveOopMapConflicts: public GenerateOopMap {
 private:

  bool _must_clear_locals;

  virtual bool report_results() const     { return false; }
  virtual bool report_init_vars() const   { return true;  }
  virtual bool allow_rewrites() const     { return true;  }
  virtual bool possible_gc_point          (BytecodeStream *bcs)           { return false; }
  virtual void fill_stackmap_prolog       (int nof_gc_points)             {}
  virtual void fill_stackmap_epilog       ()                              {}
  virtual void fill_stackmap_for_opcodes  (BytecodeStream *bcs,
                                           CellTypeState* vars,
                                           CellTypeState* stack,
                                           int stack_top)                 {}
  virtual void fill_init_vars             (GrowableArray<intptr_t> *init_vars) { _must_clear_locals = init_vars->length() > 0; }

#ifndef PRODUCT
  // Statistics
  static int _nof_invocations;
  static int _nof_rewrites;
  static int _nof_relocations;
#endif

 public:
  ResolveOopMapConflicts(const methodHandle& method) : GenerateOopMap(method) { _must_clear_locals = false; };

  methodHandle do_potential_rewrite(TRAPS);
  bool must_clear_locals() const { return _must_clear_locals; }
};


//
// Subclass used by the compiler to generate pairing infomation
//
class GeneratePairingInfo: public GenerateOopMap {
 private:

  virtual bool report_results() const     { return false; }
  virtual bool report_init_vars() const   { return false; }
  virtual bool allow_rewrites() const     { return false;  }
  virtual bool possible_gc_point          (BytecodeStream *bcs)           { return false; }
  virtual void fill_stackmap_prolog       (int nof_gc_points)             {}
  virtual void fill_stackmap_epilog       ()                              {}
  virtual void fill_stackmap_for_opcodes  (BytecodeStream *bcs,
                                           CellTypeState* vars,
                                           CellTypeState* stack,
                                           int stack_top)                 {}
  virtual void fill_init_vars             (GrowableArray<intptr_t> *init_vars) {}
 public:
  GeneratePairingInfo(const methodHandle& method) : GenerateOopMap(method)       {};

  // Call compute_map() to generate info.
};

#endif // SHARE_OOPS_GENERATEOOPMAP_HPP
