/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_LINEARSCAN_HPP
#define SHARE_C1_C1_LINEARSCAN_HPP

#include "c1/c1_FpuStackSim.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_LIR.hpp"
#include "c1/c1_LIRGenerator.hpp"
#include "compiler/oopMap.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"

class FpuStackAllocator;
class IRScopeDebugInfo;
class Interval;
class IntervalWalker;
class LIRGenerator;
class LinearScan;
class MoveResolver;
class Range;

typedef GrowableArray<Interval*> IntervalArray;
typedef GrowableArray<Interval*> IntervalList;
typedef GrowableArray<IntervalList*> IntervalsList;
typedef GrowableArray<ScopeValue*> ScopeValueArray;
typedef GrowableArray<LIR_OpList*> LIR_OpListStack;

enum IntervalUseKind {
  // priority of use kinds must be ascending
  noUse = 0,
  loopEndMarker = 1,
  shouldHaveRegister = 2,
  mustHaveRegister = 3,

  firstValidKind = 1,
  lastValidKind = 3
};

enum IntervalKind {
  fixedKind = 0,  // interval pre-colored by LIR_Generator
  anyKind   = 1,  // no register/memory allocated by LIR_Generator
  nofKinds,
  firstKind = fixedKind
};


// during linear scan an interval is in one of four states in
enum IntervalState {
  unhandledState = 0, // unhandled state (not processed yet)
  activeState   = 1,  // life and is in a physical register
  inactiveState = 2,  // in a life time hole and is in a physical register
  handledState  = 3,  // spilled or not life again
  invalidState = -1
};


enum IntervalSpillState {
  noDefinitionFound,  // starting state of calculation: no definition found yet
  oneDefinitionFound, // one definition has already been found.
                      // Note: two consecutive definitions are treated as one (e.g. consecutive move and add because of two-operand LIR form)
                      // the position of this definition is stored in _definition_pos
  oneMoveInserted,    // one spill move has already been inserted.
  storeAtDefinition,  // the interval should be stored immediately after its definition because otherwise
                      // there would be multiple redundant stores
  startInMemory,      // the interval starts in memory (e.g. method parameter), so a store is never necessary
  noOptimization      // the interval has more then one definition (e.g. resulting from phi moves), so stores to memory are not optimized
};


#define for_each_interval_kind(kind) \
  for (IntervalKind kind = firstKind; kind < nofKinds; kind = (IntervalKind)(kind + 1))

#define for_each_visitor_mode(mode) \
  for (LIR_OpVisitState::OprMode mode = LIR_OpVisitState::firstMode; mode < LIR_OpVisitState::numModes; mode = (LIR_OpVisitState::OprMode)(mode + 1))


class LinearScan : public CompilationResourceObj {
  // declare classes used by LinearScan as friends because they
  // need a wide variety of functions declared here
  //
  // Only the small interface to the rest of the compiler is public
  friend class Interval;
  friend class IntervalWalker;
  friend class LinearScanWalker;
  friend class FpuStackAllocator;
  friend class MoveResolver;
  friend class LinearScanStatistic;
  friend class LinearScanTimers;
  friend class RegisterVerifier;

 public:
  enum {
    any_reg = -1,
    nof_cpu_regs = pd_nof_cpu_regs_linearscan,
    nof_fpu_regs = pd_nof_fpu_regs_linearscan,
    nof_xmm_regs = pd_nof_xmm_regs_linearscan,
    nof_regs = nof_cpu_regs + nof_fpu_regs + nof_xmm_regs
  };

 private:
  Compilation*              _compilation;
  IR*                       _ir;
  LIRGenerator*             _gen;
  FrameMap*                 _frame_map;

  BlockList                 _cached_blocks;     // cached list with all blocks in linear-scan order (only correct if original list keeps unchanged)
  int                       _num_virtual_regs;  // number of virtual registers (without new registers introduced because of splitting intervals)
  bool                      _has_fpu_registers; // true if this method uses any floating point registers (and so fpu stack allocation is necessary)
  int                       _num_calls;         // total number of calls in this method
  int                       _max_spills;        // number of stack slots used for intervals allocated to memory
  int                       _unused_spill_slot; // unused spill slot for a single-word value because of alignment of a double-word value

  IntervalList              _intervals;         // mapping from register number to interval
  IntervalList*             _new_intervals_from_allocation; // list with all intervals created during allocation when an existing interval is split
  IntervalArray*            _sorted_intervals;  // intervals sorted by Interval::from()
  bool                      _needs_full_resort; // set to true if an Interval::from() is changed and _sorted_intervals must be resorted

  LIR_OpArray               _lir_ops;           // mapping from LIR_Op id to LIR_Op node
  BlockBeginArray           _block_of_op;       // mapping from LIR_Op id to the BlockBegin containing this instruction
  ResourceBitMap            _has_info;          // bit set for each LIR_Op id that has a CodeEmitInfo
  ResourceBitMap            _has_call;          // bit set for each LIR_Op id that destroys all caller save registers
  BitMap2D                  _interval_in_loop;  // bit set for each virtual register that is contained in each loop

  // cached debug info to prevent multiple creation of same object
  // TODO: cached scope values for registers could be static
  ScopeValueArray           _scope_value_cache;

  static ConstantOopWriteValue* _oop_null_scope_value;
  static ConstantIntValue*    _int_m1_scope_value;
  static ConstantIntValue*    _int_0_scope_value;
  static ConstantIntValue*    _int_1_scope_value;
  static ConstantIntValue*    _int_2_scope_value;

  // accessors
  IR*           ir() const                       { return _ir; }
  Compilation*  compilation() const              { return _compilation; }
  LIRGenerator* gen() const                      { return _gen; }
  FrameMap*     frame_map() const                { return _frame_map; }

  // unified bailout support
  void          bailout(const char* msg) const   { compilation()->bailout(msg); }
  bool          bailed_out() const               { return compilation()->bailed_out(); }

  // access to block list (sorted in linear scan order)
  int           block_count() const              { assert(_cached_blocks.length() == ir()->linear_scan_order()->length(), "invalid cached block list"); return _cached_blocks.length(); }
  BlockBegin*   block_at(int idx) const          { assert(_cached_blocks.at(idx) == ir()->linear_scan_order()->at(idx), "invalid cached block list");   return _cached_blocks.at(idx); }

  int           num_virtual_regs() const         { return _num_virtual_regs; }
  // size of live_in and live_out sets of BasicBlocks (BitMap needs rounded size for iteration)
  int           live_set_size() const            { return align_up(_num_virtual_regs, BitsPerWord); }
  bool          has_fpu_registers() const        { return _has_fpu_registers; }
  int           num_loops() const                { return ir()->num_loops(); }
  bool          is_interval_in_loop(int interval, int loop) const { return _interval_in_loop.at(interval, loop); }

  // handling of fpu stack allocation (platform dependent, needed for debug information generation)
#ifdef IA32
  FpuStackAllocator* _fpu_stack_allocator;
  bool use_fpu_stack_allocation() const          { return UseSSE < 2 && has_fpu_registers(); }
#else
  bool use_fpu_stack_allocation() const          { return false; }
#endif


  // access to interval list
  int           interval_count() const           { return _intervals.length(); }
  Interval*     interval_at(int reg_num) const   { return _intervals.at(reg_num); }

  // access to LIR_Ops and Blocks indexed by op_id
  int          max_lir_op_id() const                { assert(_lir_ops.length() > 0, "no operations"); return (_lir_ops.length() - 1) << 1; }
  LIR_Op*      lir_op_with_id(int op_id) const      { assert(op_id >= 0 && op_id <= max_lir_op_id() && op_id % 2 == 0, "op_id out of range or not even"); return _lir_ops.at(op_id >> 1); }
  BlockBegin*  block_of_op_with_id(int op_id) const { assert(_block_of_op.length() > 0 && op_id >= 0 && op_id <= max_lir_op_id() + 1, "op_id out of range"); return _block_of_op.at(op_id >> 1); }

  bool is_block_begin(int op_id)                    { return op_id == 0 || block_of_op_with_id(op_id) != block_of_op_with_id(op_id - 1); }

  bool has_call(int op_id)                          { assert(op_id % 2 == 0, "must be even"); return _has_call.at(op_id >> 1); }
  bool has_info(int op_id)                          { assert(op_id % 2 == 0, "must be even"); return _has_info.at(op_id >> 1); }


  // functions for converting LIR-Operands to register numbers
  static bool is_valid_reg_num(int reg_num)         { return reg_num >= 0; }
  static int  reg_num(LIR_Opr opr);
  static int  reg_numHi(LIR_Opr opr);

  // functions for classification of intervals
  static bool is_precolored_interval(const Interval* i);
  static bool is_virtual_interval(const Interval* i);

  static bool is_precolored_cpu_interval(const Interval* i);
  static bool is_virtual_cpu_interval(const Interval* i);
  static bool is_precolored_fpu_interval(const Interval* i);
  static bool is_virtual_fpu_interval(const Interval* i);

  static bool is_in_fpu_register(const Interval* i);
  static bool is_oop_interval(const Interval* i);


  // General helper functions
  int         allocate_spill_slot(bool double_word);
  void        assign_spill_slot(Interval* it);
  void        propagate_spill_slots();

  Interval*   create_interval(int reg_num);
  void        append_interval(Interval* it);
  void        copy_register_flags(Interval* from, Interval* to);

  // platform dependent functions
  static bool is_processed_reg_num(int reg_num);
  static int  num_physical_regs(BasicType type);
  static bool requires_adjacent_regs(BasicType type);
  static bool is_caller_save(int assigned_reg);

  // spill move optimization: eliminate moves from register to stack if
  // stack slot is known to be correct
  void        change_spill_definition_pos(Interval* interval, int def_pos);
  void        change_spill_state(Interval* interval, int spill_pos);
  static bool must_store_at_definition(const Interval* i);
  void        eliminate_spill_moves();

  // Phase 1: number all instructions in all blocks
  void number_instructions();

  // Phase 2: compute local live sets separately for each block
  // (sets live_gen and live_kill for each block)
  //
  // helper methods used by compute_local_live_sets()
  void set_live_gen_kill(Value value, LIR_Op* op, BitMap& live_gen, BitMap& live_kill);

  void compute_local_live_sets();

  // Phase 3: perform a backward dataflow analysis to compute global live sets
  // (sets live_in and live_out for each block)
  void compute_global_live_sets();


  // Phase 4: build intervals
  // (fills the list _intervals)
  //
  // helper methods used by build_intervals()
  void add_use (Value value, int from, int to, IntervalUseKind use_kind);

  void add_def (LIR_Opr opr, int def_pos,      IntervalUseKind use_kind);
  void add_use (LIR_Opr opr, int from, int to, IntervalUseKind use_kind);
  void add_temp(LIR_Opr opr, int temp_pos,     IntervalUseKind use_kind);

  void add_def (int reg_num, int def_pos,      IntervalUseKind use_kind, BasicType type);
  void add_use (int reg_num, int from, int to, IntervalUseKind use_kind, BasicType type);
  void add_temp(int reg_num, int temp_pos,     IntervalUseKind use_kind, BasicType type);

  // Add platform dependent kills for particular LIR ops.  Can be used
  // to add platform dependent behaviour for some operations.
  void pd_add_temps(LIR_Op* op);

  IntervalUseKind use_kind_of_output_operand(LIR_Op* op, LIR_Opr opr);
  IntervalUseKind use_kind_of_input_operand(LIR_Op* op, LIR_Opr opr);
  void handle_method_arguments(LIR_Op* op);
  void handle_doubleword_moves(LIR_Op* op);
  void add_register_hints(LIR_Op* op);

  void build_intervals();


  // Phase 5: actual register allocation
  // (Uses LinearScanWalker)
  //
  // helper functions for building a sorted list of intervals
  NOT_PRODUCT(bool is_sorted(IntervalArray* intervals);)
  static int interval_cmp(Interval** a, Interval** b);
  void add_to_list(Interval** first, Interval** prev, Interval* interval);
  void create_unhandled_lists(Interval** list1, Interval** list2, bool (is_list1)(const Interval* i), bool (is_list2)(const Interval* i));

  void sort_intervals_before_allocation();
  void sort_intervals_after_allocation();
  void allocate_registers();


  // Phase 6: resolve data flow
  // (insert moves at edges between blocks if intervals have been split)
  //
  // helper functions for resolve_data_flow()
  Interval* split_child_at_op_id(Interval* interval, int op_id, LIR_OpVisitState::OprMode mode);
  Interval* interval_at_block_begin(BlockBegin* block, int reg_num);
  Interval* interval_at_block_end(BlockBegin* block, int reg_num);
  Interval* interval_at_op_id(int reg_num, int op_id);
  void resolve_collect_mappings(BlockBegin* from_block, BlockBegin* to_block, MoveResolver &move_resolver);
  void resolve_find_insert_pos(BlockBegin* from_block, BlockBegin* to_block, MoveResolver &move_resolver);
  void resolve_data_flow();

  void resolve_exception_entry(BlockBegin* block, int reg_num, MoveResolver &move_resolver);
  void resolve_exception_entry(BlockBegin* block, MoveResolver &move_resolver);
  void resolve_exception_edge(XHandler* handler, int throwing_op_id, int reg_num, Phi* phi, MoveResolver &move_resolver);
  void resolve_exception_edge(XHandler* handler, int throwing_op_id, MoveResolver &move_resolver);
  void resolve_exception_handlers();

  // Phase 7: assign register numbers back to LIR
  // (includes computation of debug information and oop maps)
  //
  // helper functions for assign_reg_num()
  VMReg vm_reg_for_interval(Interval* interval);
  VMReg vm_reg_for_operand(LIR_Opr opr);

  static LIR_Opr operand_for_interval(Interval* interval);
  static LIR_Opr calc_operand_for_interval(const Interval* interval);
  LIR_Opr       canonical_spill_opr(Interval* interval);

  LIR_Opr color_lir_opr(LIR_Opr opr, int id, LIR_OpVisitState::OprMode);

  // methods used for oop map computation
  IntervalWalker* init_compute_oop_maps();
  OopMap*         compute_oop_map(IntervalWalker* iw, LIR_Op* op, CodeEmitInfo* info, bool is_call_site);
  void            compute_oop_map(IntervalWalker* iw, const LIR_OpVisitState &visitor, LIR_Op* op);

  // methods used for debug information computation
  void init_compute_debug_info();

  MonitorValue*  location_for_monitor_index(int monitor_index);
  LocationValue* location_for_name(int name, Location::Type loc_type);
  void set_oop(OopMap* map, VMReg name) {
    if (map->legal_vm_reg_name(name)) {
      map->set_oop(name);
    } else {
      bailout("illegal oopMap register name");
    }
  }

  int append_scope_value_for_constant(LIR_Opr opr, GrowableArray<ScopeValue*>* scope_values);
  int append_scope_value_for_operand(LIR_Opr opr, GrowableArray<ScopeValue*>* scope_values);
  int append_scope_value(int op_id, Value value, GrowableArray<ScopeValue*>* scope_values);

  IRScopeDebugInfo* compute_debug_info_for_scope(int op_id, IRScope* cur_scope, ValueStack* cur_state, ValueStack* innermost_state);
  void compute_debug_info(CodeEmitInfo* info, int op_id);

  void assign_reg_num(LIR_OpList* instructions, IntervalWalker* iw);
  void assign_reg_num();


  // Phase 8: fpu stack allocation
  // (Used only on x86 when fpu operands are present)
  void allocate_fpu_stack();


  // helper functions for printing state
#ifndef PRODUCT
  static void print_bitmap(BitMap& bitmap);
  void        print_intervals(const char* label);
  void        print_lir(int level, const char* label, bool hir_valid = true);
  static void print_reg_num(int reg_num) { print_reg_num(tty, reg_num); }
  static void print_reg_num(outputStream* out, int reg_num);
  static LIR_Opr get_operand(int reg_num);
#endif

#ifdef ASSERT
  // verification functions for allocation
  // (check that all intervals have a correct register and that no registers are overwritten)
  void verify();
  void verify_intervals();
  void verify_no_oops_in_fixed_intervals();
  void verify_constants();
  void verify_registers();
#endif

 public:
  // creation
  LinearScan(IR* ir, LIRGenerator* gen, FrameMap* frame_map);

  // main entry function: perform linear scan register allocation
  void             do_linear_scan();

  // accessors used by Compilation
  int         max_spills()  const { return _max_spills; }
  int         num_calls() const   { assert(_num_calls >= 0, "not set"); return _num_calls; }

#ifndef PRODUCT
  // entry functions for printing
  static void print_statistics();
  static void print_timers(double total);

  // Used for debugging
  Interval* find_interval_at(int reg_num) const;
#endif
};


// Helper class for ordering moves that are inserted at the same position in the LIR
// When moves between registers are inserted, it is important that the moves are
// ordered such that no register is overwritten. So moves from register to stack
// are processed prior to moves from stack to register. When moves have circular
// dependencies, a temporary stack slot is used to break the circle.
// The same logic is used in the LinearScanWalker and in LinearScan during resolve_data_flow
// and therefore factored out in a separate class
class MoveResolver: public StackObj {
 private:
  LinearScan*      _allocator;

  LIR_List*        _insert_list;
  int              _insert_idx;
  LIR_InsertionBuffer _insertion_buffer; // buffer where moves are inserted

  IntervalList     _mapping_from;
  LIR_OprList      _mapping_from_opr;
  IntervalList     _mapping_to;
  bool             _multiple_reads_allowed;
  int              _register_blocked[LinearScan::nof_regs];

  int  register_blocked(int reg)                    { assert(reg >= 0 && reg < LinearScan::nof_regs, "out of bounds"); return _register_blocked[reg]; }
  void set_register_blocked(int reg, int direction) { assert(reg >= 0 && reg < LinearScan::nof_regs, "out of bounds"); assert(direction == 1 || direction == -1, "out of bounds"); _register_blocked[reg] += direction; }

  void block_registers(Interval* it);
  void unblock_registers(Interval* it);
  bool save_to_process_move(Interval* from, Interval* to);

  void create_insertion_buffer(LIR_List* list);
  void append_insertion_buffer();
  void insert_move(Interval* from_interval, Interval* to_interval);
  void insert_move(LIR_Opr from_opr, Interval* to_interval);
  LIR_Opr get_virtual_register(Interval* interval);

  DEBUG_ONLY(void verify_before_resolve();)
  void resolve_mappings();
 public:
  MoveResolver(LinearScan* allocator);

  DEBUG_ONLY(void check_empty();)
  void set_multiple_reads_allowed() { _multiple_reads_allowed = true; }
  void set_insert_position(LIR_List* insert_list, int insert_idx);
  void move_insert_position(LIR_List* insert_list, int insert_idx);
  void add_mapping(Interval* from, Interval* to);
  void add_mapping(LIR_Opr from, Interval* to);
  void resolve_and_append_moves();

  LinearScan* allocator()   { return _allocator; }
  bool has_mappings()       { return _mapping_from.length() > 0; }
};


class Range : public CompilationResourceObj {
  friend class Interval;

 private:
  static Range*    _end;       // sentinel (from == to == max_jint)

  int              _from;      // from (inclusive)
  int              _to;        // to (exclusive)
  Range*           _next;      // linear list of Ranges

  // used only by class Interval, so hide them
  bool             intersects(Range* r) const    { return intersects_at(r) != -1; }
  int              intersects_at(Range* r) const;

 public:
  Range(int from, int to, Range* next);

  static void      initialize(Arena* arena);
  static Range*    end()                         { return _end; }

  int              from() const                  { return _from; }
  int              to()   const                  { return _to; }
  Range*           next() const                  { return _next; }
  void             set_from(int from)            { _from = from; }
  void             set_to(int to)                { _to = to; }
  void             set_next(Range* next)         { _next = next; }

  // for testing
  void             print(outputStream* out = tty) const PRODUCT_RETURN;
};


// Interval is an ordered list of disjoint ranges.

// For pre-colored double word LIR_Oprs, one interval is created for
// the low word register and one is created for the hi word register.
// On Intel for FPU double registers only one interval is created.  At
// all times assigned_reg contains the reg. number of the physical
// register.

// For LIR_Opr in virtual registers a single interval can represent
// single and double word values.  When a physical register is
// assigned to the interval, assigned_reg contains the
// phys. reg. number and for double word values assigned_regHi the
// phys. reg. number of the hi word if there is any.  For spilled
// intervals assigned_reg contains the stack index.  assigned_regHi is
// always -1.

class Interval : public CompilationResourceObj {
 private:
  static Interval* _end;          // sentinel (interval with only range Range::end())

  int              _reg_num;
  BasicType        _type;         // valid only for virtual registers
  Range*           _first;        // sorted list of Ranges
  intStack         _use_pos_and_kinds; // sorted list of use-positions and their according use-kinds

  Range*           _current;      // interval iteration: the current Range
  Interval*        _next;         // interval iteration: sorted list of Intervals (ends with sentinel)
  IntervalState    _state;        // interval iteration: to which set belongs this interval


  int              _assigned_reg;
  int              _assigned_regHi;

  int              _cached_to;    // cached value: to of last range (-1: not cached)
  LIR_Opr          _cached_opr;
  VMReg            _cached_vm_reg;

  Interval*        _split_parent;           // the original interval where this interval is derived from
  IntervalList*    _split_children;         // list of all intervals that are split off from this interval (only available for split parents)
  Interval*        _current_split_child;    // the current split child that has been active or inactive last (always stored in split parents)

  int              _canonical_spill_slot;   // the stack slot where all split parts of this interval are spilled to (always stored in split parents)
  bool             _insert_move_when_activated; // true if move is inserted between _current_split_child and this interval when interval gets active the first time
  IntervalSpillState _spill_state;          // for spill move optimization
  int              _spill_definition_pos;   // position where the interval is defined (if defined only once)
  Interval*        _register_hint;          // this interval should be in the same register as the hint interval

  int              calc_to();
  Interval*        new_split_child();
 public:
  Interval(int reg_num);

  static void      initialize(Arena* arena);
  static Interval* end()                         { return _end; }

  // accessors
  int              reg_num() const               { return _reg_num; }
  void             set_reg_num(int r)            { assert(_reg_num == -1, "cannot change reg_num"); _reg_num = r; }
  BasicType        type() const                  { assert(_reg_num == -1 || _reg_num >= LIR_OprDesc::vreg_base, "cannot access type for fixed interval"); return _type; }
  void             set_type(BasicType type)      { assert(_reg_num < LIR_OprDesc::vreg_base || _type == T_ILLEGAL || _type == type, "overwriting existing type"); _type = type; }

  Range*           first() const                 { return _first; }
  int              from() const                  { return _first->from(); }
  int              to()                          { if (_cached_to == -1) _cached_to = calc_to(); assert(_cached_to == calc_to(), "invalid cached value"); return _cached_to; }

#ifndef PRODUCT
  int              num_use_positions() const     { return _use_pos_and_kinds.length() / 2; }
#endif

  Interval*        next() const                  { return _next; }
  Interval**       next_addr()                   { return &_next; }
  void             set_next(Interval* next)      { _next = next; }

  int              assigned_reg() const          { return _assigned_reg; }
  int              assigned_regHi() const        { return _assigned_regHi; }
  void             assign_reg(int reg)           { _assigned_reg = reg; _assigned_regHi = LinearScan::any_reg; }
  void             assign_reg(int reg,int regHi) { _assigned_reg = reg; _assigned_regHi = regHi; }

  Interval*        register_hint(bool search_split_child = true) const; // calculation needed
  void             set_register_hint(Interval* i) { _register_hint = i; }

  int              state() const                 { return _state; }
  void             set_state(IntervalState s)    { _state = s; }

  // access to split parent and split children
  bool             is_split_parent() const       { return _split_parent == this; }
  bool             is_split_child() const        { return _split_parent != this; }
  Interval*        split_parent() const          { assert(_split_parent->is_split_parent(), "must be"); return _split_parent; }
  Interval*        split_child_at_op_id(int op_id, LIR_OpVisitState::OprMode mode);
  Interval*        split_child_before_op_id(int op_id);
  DEBUG_ONLY(void  check_split_children();)

  // information stored in split parent, but available for all children
  int              canonical_spill_slot() const            { return split_parent()->_canonical_spill_slot; }
  void             set_canonical_spill_slot(int slot)      { assert(split_parent()->_canonical_spill_slot == -1, "overwriting existing value"); split_parent()->_canonical_spill_slot = slot; }
  Interval*        current_split_child() const             { return split_parent()->_current_split_child; }
  void             make_current_split_child()              { split_parent()->_current_split_child = this; }

  bool             insert_move_when_activated() const      { return _insert_move_when_activated; }
  void             set_insert_move_when_activated(bool b)  { _insert_move_when_activated = b; }

  // for spill optimization
  IntervalSpillState spill_state() const         { return split_parent()->_spill_state; }
  int              spill_definition_pos() const  { return split_parent()->_spill_definition_pos; }
  void             set_spill_state(IntervalSpillState state) {  assert(state >= spill_state(), "state cannot decrease"); split_parent()->_spill_state = state; }
  void             set_spill_definition_pos(int pos) { assert(spill_definition_pos() == -1, "cannot set the position twice"); split_parent()->_spill_definition_pos = pos; }
  // returns true if this interval has a shadow copy on the stack that is always correct
  bool             always_in_memory() const      { return split_parent()->_spill_state == storeAtDefinition || split_parent()->_spill_state == startInMemory; }

  // caching of values that take time to compute and are used multiple times
  LIR_Opr          cached_opr() const            { return _cached_opr; }
  VMReg            cached_vm_reg() const         { return _cached_vm_reg; }
  void             set_cached_opr(LIR_Opr opr)   { _cached_opr = opr; }
  void             set_cached_vm_reg(VMReg reg)  { _cached_vm_reg = reg; }

  // access to use positions
  int    first_usage(IntervalUseKind min_use_kind) const;           // id of the first operation requiring this interval in a register
  int    next_usage(IntervalUseKind min_use_kind, int from) const;  // id of next usage seen from the given position
  int    next_usage_exact(IntervalUseKind exact_use_kind, int from) const;
  int    previous_usage(IntervalUseKind min_use_kind, int from) const;

  // manipulating intervals
  void   add_use_pos(int pos, IntervalUseKind use_kind);
  void   add_range(int from, int to);
  Interval* split(int split_pos);
  Interval* split_from_start(int split_pos);
  void remove_first_use_pos()                    { _use_pos_and_kinds.trunc_to(_use_pos_and_kinds.length() - 2); }

  // test intersection
  bool   covers(int op_id, LIR_OpVisitState::OprMode mode) const;
  bool   has_hole_between(int from, int to);
  bool   intersects(Interval* i) const           { return _first->intersects(i->_first); }
  bool   intersects_any_children_of(Interval* i) const;
  int    intersects_at(Interval* i) const        { return _first->intersects_at(i->_first); }

  // range iteration
  void   rewind_range()                          { _current = _first; }
  void   next_range()                            { assert(this != _end, "not allowed on sentinel"); _current = _current->next(); }
  int    current_from() const                    { return _current->from(); }
  int    current_to() const                      { return _current->to(); }
  bool   current_at_end() const                  { return _current == Range::end(); }
  bool   current_intersects(Interval* it)        { return _current->intersects(it->_current); };
  int    current_intersects_at(Interval* it)     { return _current->intersects_at(it->_current); };

  // printing
#ifndef PRODUCT
  void print() const { print_on(tty); }
  void print_on(outputStream* out) const {
    print_on(out, false);
  }
  // Special version for compatibility with C1 Visualizer.
  void print_on(outputStream* out, bool is_cfg_printer) const;

  // Used for debugging
  void print_parent() const;
  void print_children() const;
#endif

};


class IntervalWalker : public CompilationResourceObj {
 protected:
  Compilation*     _compilation;
  LinearScan*      _allocator;

  Interval*        _unhandled_first[nofKinds];  // sorted list of intervals, not life before the current position
  Interval*        _active_first   [nofKinds];  // sorted list of intervals, life at the current position
  Interval*        _inactive_first [nofKinds];  // sorted list of intervals, intervals in a life time hole at the current position

  Interval*        _current;                     // the current interval coming from unhandled list
  int              _current_position;            // the current position (intercept point through the intervals)
  IntervalKind     _current_kind;                // and whether it is fixed_kind or any_kind.


  Compilation*     compilation() const               { return _compilation; }
  LinearScan*      allocator() const                 { return _allocator; }

  // unified bailout support
  void             bailout(const char* msg) const    { compilation()->bailout(msg); }
  bool             bailed_out() const                { return compilation()->bailed_out(); }

  void check_bounds(IntervalKind kind) { assert(kind >= fixedKind && kind <= anyKind, "invalid interval_kind"); }

  Interval** unhandled_first_addr(IntervalKind kind) { check_bounds(kind); return &_unhandled_first[kind]; }
  Interval** active_first_addr(IntervalKind kind)    { check_bounds(kind); return &_active_first[kind]; }
  Interval** inactive_first_addr(IntervalKind kind)  { check_bounds(kind); return &_inactive_first[kind]; }

  void append_sorted(Interval** first, Interval* interval);
  void append_to_unhandled(Interval** list, Interval* interval);

  bool remove_from_list(Interval** list, Interval* i);
  void remove_from_list(Interval* i);

  void next_interval();
  Interval*        current() const               { return _current; }
  IntervalKind     current_kind() const          { return _current_kind; }

  void walk_to(IntervalState state, int from);

  // activate_current() is called when an unhandled interval becomes active (in current(), current_kind()).
  // Return false if current() should not be moved the the active interval list.
  // It is safe to append current to any interval list but the unhandled list.
  virtual bool activate_current() { return true; }

  // This method is called whenever an interval moves from one interval list to another to print some
  // information about it and its state change if TraceLinearScanLevel is set appropriately.
  DEBUG_ONLY(void interval_moved(Interval* interval, IntervalKind kind, IntervalState from, IntervalState to);)

 public:
  IntervalWalker(LinearScan* allocator, Interval* unhandled_fixed_first, Interval* unhandled_any_first);

  Interval* unhandled_first(IntervalKind kind)   { check_bounds(kind); return _unhandled_first[kind]; }
  Interval* active_first(IntervalKind kind)      { check_bounds(kind); return _active_first[kind]; }
  Interval* inactive_first(IntervalKind kind)    { check_bounds(kind); return _inactive_first[kind]; }

  // active contains the intervals that are live after the lir_op
  void walk_to(int lir_op_id);
  // active contains the intervals that are live before the lir_op
  void walk_before(int lir_op_id)  { walk_to(lir_op_id-1); }
  // walk through all intervals
  void walk()                      { walk_to(max_jint); }

  int current_position()           { return _current_position; }
};


// The actual linear scan register allocator
class LinearScanWalker : public IntervalWalker {
  enum {
    any_reg = LinearScan::any_reg
  };

 private:
  int              _first_reg;       // the reg. number of the first phys. register
  int              _last_reg;        // the reg. nmber of the last phys. register
  int              _num_phys_regs;   // required by current interval
  bool             _adjacent_regs;   // have lo/hi words of phys. regs be adjacent

  int              _use_pos[LinearScan::nof_regs];
  int              _block_pos[LinearScan::nof_regs];
  IntervalList*    _spill_intervals[LinearScan::nof_regs];

  MoveResolver     _move_resolver;   // for ordering spill moves

  // accessors mapped to same functions in class LinearScan
  int         block_count() const      { return allocator()->block_count(); }
  BlockBegin* block_at(int idx) const  { return allocator()->block_at(idx); }
  BlockBegin* block_of_op_with_id(int op_id) const { return allocator()->block_of_op_with_id(op_id); }

  void init_use_lists(bool only_process_use_pos);
  void exclude_from_use(int reg);
  void exclude_from_use(Interval* i);
  void set_use_pos(int reg, Interval* i, int use_pos, bool only_process_use_pos);
  void set_use_pos(Interval* i, int use_pos, bool only_process_use_pos);
  void set_block_pos(int reg, Interval* i, int block_pos);
  void set_block_pos(Interval* i, int block_pos);

  void free_exclude_active_fixed();
  void free_exclude_active_any();
  void free_collect_inactive_fixed(Interval* cur);
  void free_collect_inactive_any(Interval* cur);
  void spill_exclude_active_fixed();
  void spill_block_inactive_fixed(Interval* cur);
  void spill_collect_active_any();
  void spill_collect_inactive_any(Interval* cur);

  void insert_move(int op_id, Interval* src_it, Interval* dst_it);
  int  find_optimal_split_pos(BlockBegin* min_block, BlockBegin* max_block, int max_split_pos);
  int  find_optimal_split_pos(Interval* it, int min_split_pos, int max_split_pos, bool do_loop_optimization);
  void split_before_usage(Interval* it, int min_split_pos, int max_split_pos);
  void split_for_spilling(Interval* it);
  void split_stack_interval(Interval* it);
  void split_when_partial_register_available(Interval* it, int register_available_until);
  void split_and_spill_interval(Interval* it);

  int  find_free_reg(int reg_needed_until, int interval_to, int hint_reg, int ignore_reg, bool* need_split);
  int  find_free_double_reg(int reg_needed_until, int interval_to, int hint_reg, bool* need_split);
  bool alloc_free_reg(Interval* cur);

  int  find_locked_reg(int reg_needed_until, int interval_to, int ignore_reg, bool* need_split);
  int  find_locked_double_reg(int reg_needed_until, int interval_to, bool* need_split);
  void split_and_spill_intersecting_intervals(int reg, int regHi);
  void alloc_locked_reg(Interval* cur);

  bool no_allocation_possible(Interval* cur);
  void init_vars_for_alloc(Interval* cur);
  bool pd_init_regs_for_alloc(Interval* cur);

  void combine_spilled_intervals(Interval* cur);
  bool is_move(LIR_Op* op, Interval* from, Interval* to);

  bool activate_current();

 public:
  LinearScanWalker(LinearScan* allocator, Interval* unhandled_fixed_first, Interval* unhandled_any_first);

  // must be called when all intervals are allocated
  void             finish_allocation()           { _move_resolver.resolve_and_append_moves(); }
};



/*
When a block has more than one predecessor, and all predecessors end with
the same sequence of move-instructions, than this moves can be placed once
at the beginning of the block instead of multiple times in the predecessors.

Similarly, when a block has more than one successor, then equal sequences of
moves at the beginning of the successors can be placed once at the end of
the block. But because the moves must be inserted before all branch
instructions, this works only when there is exactly one conditional branch
at the end of the block (because the moves must be inserted before all
branches, but after all compares).

This optimization affects all kind of moves (reg->reg, reg->stack and
stack->reg). Because this optimization works best when a block contains only
few moves, it has a huge impact on the number of blocks that are totally
empty.
*/
class EdgeMoveOptimizer : public StackObj {
 private:
  // the class maintains a list with all lir-instruction-list of the
  // successors (predecessors) and the current index into the lir-lists
  LIR_OpListStack _edge_instructions;
  intStack        _edge_instructions_idx;

  void init_instructions();
  void append_instructions(LIR_OpList* instructions, int instructions_idx);
  LIR_Op* instruction_at(int edge);
  void remove_cur_instruction(int edge, bool decrement_index);

  bool operations_different(LIR_Op* op1, LIR_Op* op2);

  void optimize_moves_at_block_end(BlockBegin* cur);
  void optimize_moves_at_block_begin(BlockBegin* cur);

  EdgeMoveOptimizer();

 public:
  static void optimize(BlockList* code);
};



class ControlFlowOptimizer : public StackObj {
 private:
  BlockList _original_preds;

  enum {
    ShortLoopSize = 5
  };
  void reorder_short_loop(BlockList* code, BlockBegin* header_block, int header_idx);
  void reorder_short_loops(BlockList* code);

  bool can_delete_block(BlockBegin* cur);
  void substitute_branch_target(BlockBegin* cur, BlockBegin* target_from, BlockBegin* target_to);
  void delete_empty_blocks(BlockList* code);

  void delete_unnecessary_jumps(BlockList* code);
  void delete_jumps_to_return(BlockList* code);

  DEBUG_ONLY(void verify(BlockList* code);)

  ControlFlowOptimizer();
 public:
  static void optimize(BlockList* code);
};


#ifndef PRODUCT

// Helper class for collecting statistics of LinearScan
class LinearScanStatistic : public StackObj {
 public:
  enum Counter {
    // general counters
    counter_method,
    counter_fpu_method,
    counter_loop_method,
    counter_exception_method,
    counter_loop,
    counter_block,
    counter_loop_block,
    counter_exception_block,
    counter_interval,
    counter_fixed_interval,
    counter_range,
    counter_fixed_range,
    counter_use_pos,
    counter_fixed_use_pos,
    counter_spill_slots,
    blank_line_1,

    // counter for classes of lir instructions
    counter_instruction,
    counter_label,
    counter_entry,
    counter_return,
    counter_call,
    counter_move,
    counter_cmp,
    counter_cond_branch,
    counter_uncond_branch,
    counter_stub_branch,
    counter_alu,
    counter_alloc,
    counter_sync,
    counter_throw,
    counter_unwind,
    counter_typecheck,
    counter_fpu_stack,
    counter_misc_inst,
    counter_other_inst,
    blank_line_2,

    // counter for different types of moves
    counter_move_total,
    counter_move_reg_reg,
    counter_move_reg_stack,
    counter_move_stack_reg,
    counter_move_stack_stack,
    counter_move_reg_mem,
    counter_move_mem_reg,
    counter_move_const_any,

    number_of_counters,
    invalid_counter = -1
  };

 private:
  int _counters_sum[number_of_counters];
  int _counters_max[number_of_counters];

  void inc_counter(Counter idx, int value = 1) { _counters_sum[idx] += value; }

  const char* counter_name(int counter_idx);
  Counter base_counter(int counter_idx);

  void sum_up(LinearScanStatistic &method_statistic);
  void collect(LinearScan* allocator);

 public:
  LinearScanStatistic();
  void print(const char* title);
  static void compute(LinearScan* allocator, LinearScanStatistic &global_statistic);
};


// Helper class for collecting compilation time of LinearScan
class LinearScanTimers : public StackObj {
 public:
  enum Timer {
    timer_do_nothing,
    timer_number_instructions,
    timer_compute_local_live_sets,
    timer_compute_global_live_sets,
    timer_build_intervals,
    timer_sort_intervals_before,
    timer_allocate_registers,
    timer_resolve_data_flow,
    timer_sort_intervals_after,
    timer_eliminate_spill_moves,
    timer_assign_reg_num,
    timer_allocate_fpu_stack,
    timer_optimize_lir,

    number_of_timers
  };

 private:
  elapsedTimer _timers[number_of_timers];
  const char*  timer_name(int idx);

 public:
  LinearScanTimers();

  void begin_method();                     // called for each method when register allocation starts
  void end_method(LinearScan* allocator);  // called for each method when register allocation completed
  void print(double total_time);           // called before termination of VM to print global summary

  elapsedTimer* timer(int idx) { return &(_timers[idx]); }
};


#endif // ifndef PRODUCT

// Pick up platform-dependent implementation details
#include CPU_HEADER(c1_LinearScan)

#endif // SHARE_C1_C1_LINEARSCAN_HPP
