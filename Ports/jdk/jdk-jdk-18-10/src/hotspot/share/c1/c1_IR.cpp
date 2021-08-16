/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_Compilation.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_GraphBuilder.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_Optimizer.hpp"
#include "compiler/oopMap.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/bitMap.inline.hpp"


// Implementation of XHandlers
//
// Note: This code could eventually go away if we are
//       just using the ciExceptionHandlerStream.

XHandlers::XHandlers(ciMethod* method) : _list(method->exception_table_length()) {
  ciExceptionHandlerStream s(method);
  while (!s.is_done()) {
    _list.append(new XHandler(s.handler()));
    s.next();
  }
  assert(s.count() == method->exception_table_length(), "exception table lengths inconsistent");
}

// deep copy of all XHandler contained in list
XHandlers::XHandlers(XHandlers* other) :
  _list(other->length())
{
  for (int i = 0; i < other->length(); i++) {
    _list.append(new XHandler(other->handler_at(i)));
  }
}

// Returns whether a particular exception type can be caught.  Also
// returns true if klass is unloaded or any exception handler
// classes are unloaded.  type_is_exact indicates whether the throw
// is known to be exactly that class or it might throw a subtype.
bool XHandlers::could_catch(ciInstanceKlass* klass, bool type_is_exact) const {
  // the type is unknown so be conservative
  if (!klass->is_loaded()) {
    return true;
  }

  for (int i = 0; i < length(); i++) {
    XHandler* handler = handler_at(i);
    if (handler->is_catch_all()) {
      // catch of ANY
      return true;
    }
    ciInstanceKlass* handler_klass = handler->catch_klass();
    // if it's unknown it might be catchable
    if (!handler_klass->is_loaded()) {
      return true;
    }
    // if the throw type is definitely a subtype of the catch type
    // then it can be caught.
    if (klass->is_subtype_of(handler_klass)) {
      return true;
    }
    if (!type_is_exact) {
      // If the type isn't exactly known then it can also be caught by
      // catch statements where the inexact type is a subtype of the
      // catch type.
      // given: foo extends bar extends Exception
      // throw bar can be caught by catch foo, catch bar, and catch
      // Exception, however it can't be caught by any handlers without
      // bar in its type hierarchy.
      if (handler_klass->is_subtype_of(klass)) {
        return true;
      }
    }
  }

  return false;
}


bool XHandlers::equals(XHandlers* others) const {
  if (others == NULL) return false;
  if (length() != others->length()) return false;

  for (int i = 0; i < length(); i++) {
    if (!handler_at(i)->equals(others->handler_at(i))) return false;
  }
  return true;
}

bool XHandler::equals(XHandler* other) const {
  assert(entry_pco() != -1 && other->entry_pco() != -1, "must have entry_pco");

  if (entry_pco() != other->entry_pco()) return false;
  if (scope_count() != other->scope_count()) return false;
  if (_desc != other->_desc) return false;

  assert(entry_block() == other->entry_block(), "entry_block must be equal when entry_pco is equal");
  return true;
}


// Implementation of IRScope
BlockBegin* IRScope::build_graph(Compilation* compilation, int osr_bci) {
  GraphBuilder gm(compilation, this);
  NOT_PRODUCT(if (PrintValueNumbering && Verbose) gm.print_stats());
  if (compilation->bailed_out()) return NULL;
  return gm.start();
}


IRScope::IRScope(Compilation* compilation, IRScope* caller, int caller_bci, ciMethod* method, int osr_bci, bool create_graph)
: _compilation(compilation)
, _callees(2)
, _requires_phi_function(method->max_locals())
{
  _caller             = caller;
  _level              = caller == NULL ?  0 : caller->level() + 1;
  _method             = method;
  _xhandlers          = new XHandlers(method);
  _number_of_locks    = 0;
  _monitor_pairing_ok = method->has_balanced_monitors();
  _wrote_final        = false;
  _wrote_fields       = false;
  _wrote_volatile     = false;
  _start              = NULL;

  if (osr_bci != -1) {
    // selective creation of phi functions is not possibel in osr-methods
    _requires_phi_function.set_range(0, method->max_locals());
  }

  assert(method->holder()->is_loaded() , "method holder must be loaded");

  // build graph if monitor pairing is ok
  if (create_graph && monitor_pairing_ok()) _start = build_graph(compilation, osr_bci);
}


int IRScope::max_stack() const {
  int my_max = method()->max_stack();
  int callee_max = 0;
  for (int i = 0; i < number_of_callees(); i++) {
    callee_max = MAX2(callee_max, callee_no(i)->max_stack());
  }
  return my_max + callee_max;
}


bool IRScopeDebugInfo::should_reexecute() {
  ciMethod* cur_method = scope()->method();
  int       cur_bci    = bci();
  if (cur_method != NULL && cur_bci != SynchronizationEntryBCI) {
    Bytecodes::Code code = cur_method->java_code_at_bci(cur_bci);
    return Interpreter::bytecode_should_reexecute(code);
  } else
    return false;
}


// Implementation of CodeEmitInfo

// Stack must be NON-null
CodeEmitInfo::CodeEmitInfo(ValueStack* stack, XHandlers* exception_handlers, bool deoptimize_on_exception)
  : _scope_debug_info(NULL)
  , _scope(stack->scope())
  , _exception_handlers(exception_handlers)
  , _oop_map(NULL)
  , _stack(stack)
  , _is_method_handle_invoke(false)
  , _deoptimize_on_exception(deoptimize_on_exception) {
  assert(_stack != NULL, "must be non null");
}


CodeEmitInfo::CodeEmitInfo(CodeEmitInfo* info, ValueStack* stack)
  : _scope_debug_info(NULL)
  , _scope(info->_scope)
  , _exception_handlers(NULL)
  , _oop_map(NULL)
  , _stack(stack == NULL ? info->_stack : stack)
  , _is_method_handle_invoke(info->_is_method_handle_invoke)
  , _deoptimize_on_exception(info->_deoptimize_on_exception) {

  // deep copy of exception handlers
  if (info->_exception_handlers != NULL) {
    _exception_handlers = new XHandlers(info->_exception_handlers);
  }
}


void CodeEmitInfo::record_debug_info(DebugInformationRecorder* recorder, int pc_offset) {
  // record the safepoint before recording the debug info for enclosing scopes
  recorder->add_safepoint(pc_offset, _oop_map->deep_copy());
  _scope_debug_info->record_debug_info(recorder, pc_offset, true/*topmost*/, _is_method_handle_invoke);
  recorder->end_safepoint(pc_offset);
}


void CodeEmitInfo::add_register_oop(LIR_Opr opr) {
  assert(_oop_map != NULL, "oop map must already exist");
  assert(opr->is_single_cpu(), "should not call otherwise");

  VMReg name = frame_map()->regname(opr);
  _oop_map->set_oop(name);
}

// Mirror the stack size calculation in the deopt code
// How much stack space would we need at this point in the program in
// case of deoptimization?
int CodeEmitInfo::interpreter_frame_size() const {
  ValueStack* state = _stack;
  int size = 0;
  int callee_parameters = 0;
  int callee_locals = 0;
  int extra_args = state->scope()->method()->max_stack() - state->stack_size();

  while (state != NULL) {
    int locks = state->locks_size();
    int temps = state->stack_size();
    bool is_top_frame = (state == _stack);
    ciMethod* method = state->scope()->method();

    int frame_size = BytesPerWord * Interpreter::size_activation(method->max_stack(),
                                                                 temps + callee_parameters,
                                                                 extra_args,
                                                                 locks,
                                                                 callee_parameters,
                                                                 callee_locals,
                                                                 is_top_frame);
    size += frame_size;

    callee_parameters = method->size_of_parameters();
    callee_locals = method->max_locals();
    extra_args = 0;
    state = state->caller_state();
  }
  return size + Deoptimization::last_frame_adjust(0, callee_locals) * BytesPerWord;
}

// Implementation of IR

IR::IR(Compilation* compilation, ciMethod* method, int osr_bci) :
  _num_loops(0) {
  // setup IR fields
  _compilation = compilation;
  _top_scope   = new IRScope(compilation, NULL, -1, method, osr_bci, true);
  _code        = NULL;
}


void IR::optimize_blocks() {
  Optimizer opt(this);
  if (!compilation()->profile_branches()) {
    if (DoCEE) {
      opt.eliminate_conditional_expressions();
#ifndef PRODUCT
      if (PrintCFG || PrintCFG1) { tty->print_cr("CFG after CEE"); print(true); }
      if (PrintIR  || PrintIR1 ) { tty->print_cr("IR after CEE"); print(false); }
#endif
    }
    if (EliminateBlocks) {
      opt.eliminate_blocks();
#ifndef PRODUCT
      if (PrintCFG || PrintCFG1) { tty->print_cr("CFG after block elimination"); print(true); }
      if (PrintIR  || PrintIR1 ) { tty->print_cr("IR after block elimination"); print(false); }
#endif
    }
  }
}

void IR::eliminate_null_checks() {
  Optimizer opt(this);
  if (EliminateNullChecks) {
    opt.eliminate_null_checks();
#ifndef PRODUCT
    if (PrintCFG || PrintCFG1) { tty->print_cr("CFG after null check elimination"); print(true); }
    if (PrintIR  || PrintIR1 ) { tty->print_cr("IR after null check elimination"); print(false); }
#endif
  }
}


static int sort_pairs(BlockPair** a, BlockPair** b) {
  if ((*a)->from() == (*b)->from()) {
    return (*a)->to()->block_id() - (*b)->to()->block_id();
  } else {
    return (*a)->from()->block_id() - (*b)->from()->block_id();
  }
}


class CriticalEdgeFinder: public BlockClosure {
  BlockPairList blocks;
  IR*       _ir;

 public:
  CriticalEdgeFinder(IR* ir): _ir(ir) {}
  void block_do(BlockBegin* bb) {
    BlockEnd* be = bb->end();
    int nos = be->number_of_sux();
    if (nos >= 2) {
      for (int i = 0; i < nos; i++) {
        BlockBegin* sux = be->sux_at(i);
        if (sux->number_of_preds() >= 2) {
          blocks.append(new BlockPair(bb, sux));
        }
      }
    }
  }

  void split_edges() {
    BlockPair* last_pair = NULL;
    blocks.sort(sort_pairs);
    for (int i = 0; i < blocks.length(); i++) {
      BlockPair* pair = blocks.at(i);
      if (last_pair != NULL && pair->is_same(last_pair)) continue;
      BlockBegin* from = pair->from();
      BlockBegin* to = pair->to();
      BlockBegin* split = from->insert_block_between(to);
#ifndef PRODUCT
      if ((PrintIR || PrintIR1) && Verbose) {
        tty->print_cr("Split critical edge B%d -> B%d (new block B%d)",
                      from->block_id(), to->block_id(), split->block_id());
      }
#endif
      last_pair = pair;
    }
  }
};

void IR::split_critical_edges() {
  CriticalEdgeFinder cef(this);

  iterate_preorder(&cef);
  cef.split_edges();
}


class UseCountComputer: public ValueVisitor, BlockClosure {
 private:
  void visit(Value* n) {
    // Local instructions and Phis for expression stack values at the
    // start of basic blocks are not added to the instruction list
    if (!(*n)->is_linked() && (*n)->can_be_linked()) {
      assert(false, "a node was not appended to the graph");
      Compilation::current()->bailout("a node was not appended to the graph");
    }
    // use n's input if not visited before
    if (!(*n)->is_pinned() && !(*n)->has_uses()) {
      // note: a) if the instruction is pinned, it will be handled by compute_use_count
      //       b) if the instruction has uses, it was touched before
      //       => in both cases we don't need to update n's values
      uses_do(n);
    }
    // use n
    (*n)->_use_count++;
  }

  Values* worklist;
  int depth;
  enum {
    max_recurse_depth = 20
  };

  void uses_do(Value* n) {
    depth++;
    if (depth > max_recurse_depth) {
      // don't allow the traversal to recurse too deeply
      worklist->push(*n);
    } else {
      (*n)->input_values_do(this);
      // special handling for some instructions
      if ((*n)->as_BlockEnd() != NULL) {
        // note on BlockEnd:
        //   must 'use' the stack only if the method doesn't
        //   terminate, however, in those cases stack is empty
        (*n)->state_values_do(this);
      }
    }
    depth--;
  }

  void block_do(BlockBegin* b) {
    depth = 0;
    // process all pinned nodes as the roots of expression trees
    for (Instruction* n = b; n != NULL; n = n->next()) {
      if (n->is_pinned()) uses_do(&n);
    }
    assert(depth == 0, "should have counted back down");

    // now process any unpinned nodes which recursed too deeply
    while (worklist->length() > 0) {
      Value t = worklist->pop();
      if (!t->is_pinned()) {
        // compute the use count
        uses_do(&t);

        // pin the instruction so that LIRGenerator doesn't recurse
        // too deeply during it's evaluation.
        t->pin();
      }
    }
    assert(depth == 0, "should have counted back down");
  }

  UseCountComputer() {
    worklist = new Values();
    depth = 0;
  }

 public:
  static void compute(BlockList* blocks) {
    UseCountComputer ucc;
    blocks->iterate_backward(&ucc);
  }
};


// helper macro for short definition of trace-output inside code
#ifdef ASSERT
  #define TRACE_LINEAR_SCAN(level, code)       \
    if (TraceLinearScanLevel >= level) {       \
      code;                                    \
    }
#else
  #define TRACE_LINEAR_SCAN(level, code)
#endif

class ComputeLinearScanOrder : public StackObj {
 private:
  int        _max_block_id;        // the highest block_id of a block
  int        _num_blocks;          // total number of blocks (smaller than _max_block_id)
  int        _num_loops;           // total number of loops
  bool       _iterative_dominators;// method requires iterative computation of dominatiors

  BlockList* _linear_scan_order;   // the resulting list of blocks in correct order

  ResourceBitMap _visited_blocks;   // used for recursive processing of blocks
  ResourceBitMap _active_blocks;    // used for recursive processing of blocks
  ResourceBitMap _dominator_blocks; // temproary BitMap used for computation of dominator
  intArray       _forward_branches; // number of incoming forward branches for each block
  BlockList      _loop_end_blocks;  // list of all loop end blocks collected during count_edges
  BitMap2D       _loop_map;         // two-dimensional bit set: a bit is set if a block is contained in a loop
  BlockList      _work_list;        // temporary list (used in mark_loops and compute_order)
  BlockList      _loop_headers;

  Compilation* _compilation;

  // accessors for _visited_blocks and _active_blocks
  void init_visited()                     { _active_blocks.clear(); _visited_blocks.clear(); }
  bool is_visited(BlockBegin* b) const    { return _visited_blocks.at(b->block_id()); }
  bool is_active(BlockBegin* b) const     { return _active_blocks.at(b->block_id()); }
  void set_visited(BlockBegin* b)         { assert(!is_visited(b), "already set"); _visited_blocks.set_bit(b->block_id()); }
  void set_active(BlockBegin* b)          { assert(!is_active(b), "already set");  _active_blocks.set_bit(b->block_id()); }
  void clear_active(BlockBegin* b)        { assert(is_active(b), "not already");   _active_blocks.clear_bit(b->block_id()); }

  // accessors for _forward_branches
  void inc_forward_branches(BlockBegin* b) { _forward_branches.at_put(b->block_id(), _forward_branches.at(b->block_id()) + 1); }
  int  dec_forward_branches(BlockBegin* b) { _forward_branches.at_put(b->block_id(), _forward_branches.at(b->block_id()) - 1); return _forward_branches.at(b->block_id()); }

  // accessors for _loop_map
  bool is_block_in_loop   (int loop_idx, BlockBegin* b) const { return _loop_map.at(loop_idx, b->block_id()); }
  void set_block_in_loop  (int loop_idx, BlockBegin* b)       { _loop_map.set_bit(loop_idx, b->block_id()); }
  void clear_block_in_loop(int loop_idx, int block_id)        { _loop_map.clear_bit(loop_idx, block_id); }

  // count edges between blocks
  void count_edges(BlockBegin* cur, BlockBegin* parent);

  // loop detection
  void mark_loops();
  void clear_non_natural_loops(BlockBegin* start_block);
  void assign_loop_depth(BlockBegin* start_block);

  // computation of final block order
  BlockBegin* common_dominator(BlockBegin* a, BlockBegin* b);
  void compute_dominator(BlockBegin* cur, BlockBegin* parent);
  void compute_dominator_impl(BlockBegin* cur, BlockBegin* parent);
  int  compute_weight(BlockBegin* cur);
  bool ready_for_processing(BlockBegin* cur);
  void sort_into_work_list(BlockBegin* b);
  void append_block(BlockBegin* cur);
  void compute_order(BlockBegin* start_block);

  // fixup of dominators for non-natural loops
  bool compute_dominators_iter();
  void compute_dominators();

  // debug functions
  DEBUG_ONLY(void print_blocks();)
  DEBUG_ONLY(void verify();)

  Compilation* compilation() const { return _compilation; }
 public:
  ComputeLinearScanOrder(Compilation* c, BlockBegin* start_block);

  // accessors for final result
  BlockList* linear_scan_order() const    { return _linear_scan_order; }
  int        num_loops() const            { return _num_loops; }
};


ComputeLinearScanOrder::ComputeLinearScanOrder(Compilation* c, BlockBegin* start_block) :
  _max_block_id(BlockBegin::number_of_blocks()),
  _num_blocks(0),
  _num_loops(0),
  _iterative_dominators(false),
  _linear_scan_order(NULL), // initialized later with correct size
  _visited_blocks(_max_block_id),
  _active_blocks(_max_block_id),
  _dominator_blocks(_max_block_id),
  _forward_branches(_max_block_id, _max_block_id, 0),
  _loop_end_blocks(8),
  _loop_map(0),             // initialized later with correct size
  _work_list(8),
  _compilation(c)
{
  TRACE_LINEAR_SCAN(2, tty->print_cr("***** computing linear-scan block order"));

  count_edges(start_block, NULL);

  if (compilation()->is_profiling()) {
    ciMethod *method = compilation()->method();
    if (!method->is_accessor()) {
      ciMethodData* md = method->method_data_or_null();
      assert(md != NULL, "Sanity");
      md->set_compilation_stats(_num_loops, _num_blocks);
    }
  }

  if (_num_loops > 0) {
    mark_loops();
    clear_non_natural_loops(start_block);
    assign_loop_depth(start_block);
  }

  compute_order(start_block);
  compute_dominators();

  DEBUG_ONLY(print_blocks());
  DEBUG_ONLY(verify());
}


// Traverse the CFG:
// * count total number of blocks
// * count all incoming edges and backward incoming edges
// * number loop header blocks
// * create a list with all loop end blocks
void ComputeLinearScanOrder::count_edges(BlockBegin* cur, BlockBegin* parent) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("Enter count_edges for block B%d coming from B%d", cur->block_id(), parent != NULL ? parent->block_id() : -1));
  assert(cur->dominator() == NULL, "dominator already initialized");

  if (is_active(cur)) {
    TRACE_LINEAR_SCAN(3, tty->print_cr("backward branch"));
    assert(is_visited(cur), "block must be visisted when block is active");
    assert(parent != NULL, "must have parent");

    cur->set(BlockBegin::backward_branch_target_flag);

    // When a loop header is also the start of an exception handler, then the backward branch is
    // an exception edge. Because such edges are usually critical edges which cannot be split, the
    // loop must be excluded here from processing.
    if (cur->is_set(BlockBegin::exception_entry_flag)) {
      // Make sure that dominators are correct in this weird situation
      _iterative_dominators = true;
      return;
    }

    cur->set(BlockBegin::linear_scan_loop_header_flag);
    parent->set(BlockBegin::linear_scan_loop_end_flag);

    assert(parent->number_of_sux() == 1 && parent->sux_at(0) == cur,
           "loop end blocks must have one successor (critical edges are split)");

    _loop_end_blocks.append(parent);
    return;
  }

  // increment number of incoming forward branches
  inc_forward_branches(cur);

  if (is_visited(cur)) {
    TRACE_LINEAR_SCAN(3, tty->print_cr("block already visited"));
    return;
  }

  _num_blocks++;
  set_visited(cur);
  set_active(cur);

  // recursive call for all successors
  int i;
  for (i = cur->number_of_sux() - 1; i >= 0; i--) {
    count_edges(cur->sux_at(i), cur);
  }
  for (i = cur->number_of_exception_handlers() - 1; i >= 0; i--) {
    count_edges(cur->exception_handler_at(i), cur);
  }

  clear_active(cur);

  // Each loop has a unique number.
  // When multiple loops are nested, assign_loop_depth assumes that the
  // innermost loop has the lowest number. This is guaranteed by setting
  // the loop number after the recursive calls for the successors above
  // have returned.
  if (cur->is_set(BlockBegin::linear_scan_loop_header_flag)) {
    assert(cur->loop_index() == -1, "cannot set loop-index twice");
    TRACE_LINEAR_SCAN(3, tty->print_cr("Block B%d is loop header of loop %d", cur->block_id(), _num_loops));

    cur->set_loop_index(_num_loops);
    _loop_headers.append(cur);
    _num_loops++;
  }

  TRACE_LINEAR_SCAN(3, tty->print_cr("Finished count_edges for block B%d", cur->block_id()));
}


void ComputeLinearScanOrder::mark_loops() {
  TRACE_LINEAR_SCAN(3, tty->print_cr("----- marking loops"));

  _loop_map = BitMap2D(_num_loops, _max_block_id);

  for (int i = _loop_end_blocks.length() - 1; i >= 0; i--) {
    BlockBegin* loop_end   = _loop_end_blocks.at(i);
    BlockBegin* loop_start = loop_end->sux_at(0);
    int         loop_idx   = loop_start->loop_index();

    TRACE_LINEAR_SCAN(3, tty->print_cr("Processing loop from B%d to B%d (loop %d):", loop_start->block_id(), loop_end->block_id(), loop_idx));
    assert(loop_end->is_set(BlockBegin::linear_scan_loop_end_flag), "loop end flag must be set");
    assert(loop_end->number_of_sux() == 1, "incorrect number of successors");
    assert(loop_start->is_set(BlockBegin::linear_scan_loop_header_flag), "loop header flag must be set");
    assert(loop_idx >= 0 && loop_idx < _num_loops, "loop index not set");
    assert(_work_list.is_empty(), "work list must be empty before processing");

    // add the end-block of the loop to the working list
    _work_list.push(loop_end);
    set_block_in_loop(loop_idx, loop_end);
    do {
      BlockBegin* cur = _work_list.pop();

      TRACE_LINEAR_SCAN(3, tty->print_cr("    processing B%d", cur->block_id()));
      assert(is_block_in_loop(loop_idx, cur), "bit in loop map must be set when block is in work list");

      // recursive processing of all predecessors ends when start block of loop is reached
      if (cur != loop_start && !cur->is_set(BlockBegin::osr_entry_flag)) {
        for (int j = cur->number_of_preds() - 1; j >= 0; j--) {
          BlockBegin* pred = cur->pred_at(j);

          if (!is_block_in_loop(loop_idx, pred) /*&& !pred->is_set(BlockBeginosr_entry_flag)*/) {
            // this predecessor has not been processed yet, so add it to work list
            TRACE_LINEAR_SCAN(3, tty->print_cr("    pushing B%d", pred->block_id()));
            _work_list.push(pred);
            set_block_in_loop(loop_idx, pred);
          }
        }
      }
    } while (!_work_list.is_empty());
  }
}


// check for non-natural loops (loops where the loop header does not dominate
// all other loop blocks = loops with mulitple entries).
// such loops are ignored
void ComputeLinearScanOrder::clear_non_natural_loops(BlockBegin* start_block) {
  for (int i = _num_loops - 1; i >= 0; i--) {
    if (is_block_in_loop(i, start_block)) {
      // loop i contains the entry block of the method
      // -> this is not a natural loop, so ignore it
      TRACE_LINEAR_SCAN(2, tty->print_cr("Loop %d is non-natural, so it is ignored", i));

      BlockBegin *loop_header = _loop_headers.at(i);
      assert(loop_header->is_set(BlockBegin::linear_scan_loop_header_flag), "Must be loop header");

      for (int j = 0; j < loop_header->number_of_preds(); j++) {
        BlockBegin *pred = loop_header->pred_at(j);
        pred->clear(BlockBegin::linear_scan_loop_end_flag);
      }

      loop_header->clear(BlockBegin::linear_scan_loop_header_flag);

      for (int block_id = _max_block_id - 1; block_id >= 0; block_id--) {
        clear_block_in_loop(i, block_id);
      }
      _iterative_dominators = true;
    }
  }
}

void ComputeLinearScanOrder::assign_loop_depth(BlockBegin* start_block) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("----- computing loop-depth and weight"));
  init_visited();

  assert(_work_list.is_empty(), "work list must be empty before processing");
  _work_list.append(start_block);

  do {
    BlockBegin* cur = _work_list.pop();

    if (!is_visited(cur)) {
      set_visited(cur);
      TRACE_LINEAR_SCAN(4, tty->print_cr("Computing loop depth for block B%d", cur->block_id()));

      // compute loop-depth and loop-index for the block
      assert(cur->loop_depth() == 0, "cannot set loop-depth twice");
      int i;
      int loop_depth = 0;
      int min_loop_idx = -1;
      for (i = _num_loops - 1; i >= 0; i--) {
        if (is_block_in_loop(i, cur)) {
          loop_depth++;
          min_loop_idx = i;
        }
      }
      cur->set_loop_depth(loop_depth);
      cur->set_loop_index(min_loop_idx);

      // append all unvisited successors to work list
      for (i = cur->number_of_sux() - 1; i >= 0; i--) {
        _work_list.append(cur->sux_at(i));
      }
      for (i = cur->number_of_exception_handlers() - 1; i >= 0; i--) {
        _work_list.append(cur->exception_handler_at(i));
      }
    }
  } while (!_work_list.is_empty());
}


BlockBegin* ComputeLinearScanOrder::common_dominator(BlockBegin* a, BlockBegin* b) {
  assert(a != NULL && b != NULL, "must have input blocks");

  _dominator_blocks.clear();
  while (a != NULL) {
    _dominator_blocks.set_bit(a->block_id());
    assert(a->dominator() != NULL || a == _linear_scan_order->at(0), "dominator must be initialized");
    a = a->dominator();
  }
  while (b != NULL && !_dominator_blocks.at(b->block_id())) {
    assert(b->dominator() != NULL || b == _linear_scan_order->at(0), "dominator must be initialized");
    b = b->dominator();
  }

  assert(b != NULL, "could not find dominator");
  return b;
}

void ComputeLinearScanOrder::compute_dominator(BlockBegin* cur, BlockBegin* parent) {
  init_visited();
  compute_dominator_impl(cur, parent);
}

void ComputeLinearScanOrder::compute_dominator_impl(BlockBegin* cur, BlockBegin* parent) {
  // Mark as visited to avoid recursive calls with same parent
  set_visited(cur);

  if (cur->dominator() == NULL) {
    TRACE_LINEAR_SCAN(4, tty->print_cr("DOM: initializing dominator of B%d to B%d", cur->block_id(), parent->block_id()));
    cur->set_dominator(parent);

  } else if (!(cur->is_set(BlockBegin::linear_scan_loop_header_flag) && parent->is_set(BlockBegin::linear_scan_loop_end_flag))) {
    TRACE_LINEAR_SCAN(4, tty->print_cr("DOM: computing dominator of B%d: common dominator of B%d and B%d is B%d", cur->block_id(), parent->block_id(), cur->dominator()->block_id(), common_dominator(cur->dominator(), parent)->block_id()));
    // Does not hold for exception blocks
    assert(cur->number_of_preds() > 1 || cur->is_set(BlockBegin::exception_entry_flag), "");
    cur->set_dominator(common_dominator(cur->dominator(), parent));
  }

  // Additional edge to xhandler of all our successors
  // range check elimination needs that the state at the end of a
  // block be valid in every block it dominates so cur must dominate
  // the exception handlers of its successors.
  int num_cur_xhandler = cur->number_of_exception_handlers();
  for (int j = 0; j < num_cur_xhandler; j++) {
    BlockBegin* xhandler = cur->exception_handler_at(j);
    if (!is_visited(xhandler)) {
      compute_dominator_impl(xhandler, parent);
    }
  }
}


int ComputeLinearScanOrder::compute_weight(BlockBegin* cur) {
  BlockBegin* single_sux = NULL;
  if (cur->number_of_sux() == 1) {
    single_sux = cur->sux_at(0);
  }

  // limit loop-depth to 15 bit (only for security reason, it will never be so big)
  int weight = (cur->loop_depth() & 0x7FFF) << 16;

  // general macro for short definition of weight flags
  // the first instance of INC_WEIGHT_IF has the highest priority
  int cur_bit = 15;
  #define INC_WEIGHT_IF(condition) if ((condition)) { weight |= (1 << cur_bit); } cur_bit--;

  // this is necessery for the (very rare) case that two successing blocks have
  // the same loop depth, but a different loop index (can happen for endless loops
  // with exception handlers)
  INC_WEIGHT_IF(!cur->is_set(BlockBegin::linear_scan_loop_header_flag));

  // loop end blocks (blocks that end with a backward branch) are added
  // after all other blocks of the loop.
  INC_WEIGHT_IF(!cur->is_set(BlockBegin::linear_scan_loop_end_flag));

  // critical edge split blocks are prefered because than they have a bigger
  // proability to be completely empty
  INC_WEIGHT_IF(cur->is_set(BlockBegin::critical_edge_split_flag));

  // exceptions should not be thrown in normal control flow, so these blocks
  // are added as late as possible
  INC_WEIGHT_IF(cur->end()->as_Throw() == NULL  && (single_sux == NULL || single_sux->end()->as_Throw()  == NULL));
  INC_WEIGHT_IF(cur->end()->as_Return() == NULL && (single_sux == NULL || single_sux->end()->as_Return() == NULL));

  // exceptions handlers are added as late as possible
  INC_WEIGHT_IF(!cur->is_set(BlockBegin::exception_entry_flag));

  // guarantee that weight is > 0
  weight |= 1;

  #undef INC_WEIGHT_IF
  assert(cur_bit >= 0, "too many flags");
  assert(weight > 0, "weight cannot become negative");

  return weight;
}

bool ComputeLinearScanOrder::ready_for_processing(BlockBegin* cur) {
  // Discount the edge just traveled.
  // When the number drops to zero, all forward branches were processed
  if (dec_forward_branches(cur) != 0) {
    return false;
  }

  assert(_linear_scan_order->find(cur) == -1, "block already processed (block can be ready only once)");
  assert(_work_list.find(cur) == -1, "block already in work-list (block can be ready only once)");
  return true;
}

void ComputeLinearScanOrder::sort_into_work_list(BlockBegin* cur) {
  assert(_work_list.find(cur) == -1, "block already in work list");

  int cur_weight = compute_weight(cur);

  // the linear_scan_number is used to cache the weight of a block
  cur->set_linear_scan_number(cur_weight);

#ifndef PRODUCT
  if (StressLinearScan) {
    _work_list.insert_before(0, cur);
    return;
  }
#endif

  _work_list.append(NULL); // provide space for new element

  int insert_idx = _work_list.length() - 1;
  while (insert_idx > 0 && _work_list.at(insert_idx - 1)->linear_scan_number() > cur_weight) {
    _work_list.at_put(insert_idx, _work_list.at(insert_idx - 1));
    insert_idx--;
  }
  _work_list.at_put(insert_idx, cur);

  TRACE_LINEAR_SCAN(3, tty->print_cr("Sorted B%d into worklist. new worklist:", cur->block_id()));
  TRACE_LINEAR_SCAN(3, for (int i = 0; i < _work_list.length(); i++) tty->print_cr("%8d B%2d  weight:%6x", i, _work_list.at(i)->block_id(), _work_list.at(i)->linear_scan_number()));

#ifdef ASSERT
  for (int i = 0; i < _work_list.length(); i++) {
    assert(_work_list.at(i)->linear_scan_number() > 0, "weight not set");
    assert(i == 0 || _work_list.at(i - 1)->linear_scan_number() <= _work_list.at(i)->linear_scan_number(), "incorrect order in worklist");
  }
#endif
}

void ComputeLinearScanOrder::append_block(BlockBegin* cur) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("appending block B%d (weight 0x%6x) to linear-scan order", cur->block_id(), cur->linear_scan_number()));
  assert(_linear_scan_order->find(cur) == -1, "cannot add the same block twice");

  // currently, the linear scan order and code emit order are equal.
  // therefore the linear_scan_number and the weight of a block must also
  // be equal.
  cur->set_linear_scan_number(_linear_scan_order->length());
  _linear_scan_order->append(cur);
}

void ComputeLinearScanOrder::compute_order(BlockBegin* start_block) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("----- computing final block order"));

  // the start block is always the first block in the linear scan order
  _linear_scan_order = new BlockList(_num_blocks);
  append_block(start_block);

  assert(start_block->end()->as_Base() != NULL, "start block must end with Base-instruction");
  BlockBegin* std_entry = ((Base*)start_block->end())->std_entry();
  BlockBegin* osr_entry = ((Base*)start_block->end())->osr_entry();

  BlockBegin* sux_of_osr_entry = NULL;
  if (osr_entry != NULL) {
    // special handling for osr entry:
    // ignore the edge between the osr entry and its successor for processing
    // the osr entry block is added manually below
    assert(osr_entry->number_of_sux() == 1, "osr entry must have exactly one successor");
    assert(osr_entry->sux_at(0)->number_of_preds() >= 2, "sucessor of osr entry must have two predecessors (otherwise it is not present in normal control flow");

    sux_of_osr_entry = osr_entry->sux_at(0);
    dec_forward_branches(sux_of_osr_entry);

    compute_dominator(osr_entry, start_block);
    _iterative_dominators = true;
  }
  compute_dominator(std_entry, start_block);

  // start processing with standard entry block
  assert(_work_list.is_empty(), "list must be empty before processing");

  if (ready_for_processing(std_entry)) {
    sort_into_work_list(std_entry);
  } else {
    assert(false, "the std_entry must be ready for processing (otherwise, the method has no start block)");
  }

  do {
    BlockBegin* cur = _work_list.pop();

    if (cur == sux_of_osr_entry) {
      // the osr entry block is ignored in normal processing, it is never added to the
      // work list. Instead, it is added as late as possible manually here.
      append_block(osr_entry);
      compute_dominator(cur, osr_entry);
    }
    append_block(cur);

    int i;
    int num_sux = cur->number_of_sux();
    // changed loop order to get "intuitive" order of if- and else-blocks
    for (i = 0; i < num_sux; i++) {
      BlockBegin* sux = cur->sux_at(i);
      compute_dominator(sux, cur);
      if (ready_for_processing(sux)) {
        sort_into_work_list(sux);
      }
    }
    num_sux = cur->number_of_exception_handlers();
    for (i = 0; i < num_sux; i++) {
      BlockBegin* sux = cur->exception_handler_at(i);
      if (ready_for_processing(sux)) {
        sort_into_work_list(sux);
      }
    }
  } while (_work_list.length() > 0);
}


bool ComputeLinearScanOrder::compute_dominators_iter() {
  bool changed = false;
  int num_blocks = _linear_scan_order->length();

  assert(_linear_scan_order->at(0)->dominator() == NULL, "must not have dominator");
  assert(_linear_scan_order->at(0)->number_of_preds() == 0, "must not have predecessors");
  for (int i = 1; i < num_blocks; i++) {
    BlockBegin* block = _linear_scan_order->at(i);

    BlockBegin* dominator = block->pred_at(0);
    int num_preds = block->number_of_preds();

    TRACE_LINEAR_SCAN(4, tty->print_cr("DOM: Processing B%d", block->block_id()));

    for (int j = 0; j < num_preds; j++) {

      BlockBegin *pred = block->pred_at(j);
      TRACE_LINEAR_SCAN(4, tty->print_cr("   DOM: Subrocessing B%d", pred->block_id()));

      if (block->is_set(BlockBegin::exception_entry_flag)) {
        dominator = common_dominator(dominator, pred);
        int num_pred_preds = pred->number_of_preds();
        for (int k = 0; k < num_pred_preds; k++) {
          dominator = common_dominator(dominator, pred->pred_at(k));
        }
      } else {
        dominator = common_dominator(dominator, pred);
      }
    }

    if (dominator != block->dominator()) {
      TRACE_LINEAR_SCAN(4, tty->print_cr("DOM: updating dominator of B%d from B%d to B%d", block->block_id(), block->dominator()->block_id(), dominator->block_id()));

      block->set_dominator(dominator);
      changed = true;
    }
  }
  return changed;
}

void ComputeLinearScanOrder::compute_dominators() {
  TRACE_LINEAR_SCAN(3, tty->print_cr("----- computing dominators (iterative computation reqired: %d)", _iterative_dominators));

  // iterative computation of dominators is only required for methods with non-natural loops
  // and OSR-methods. For all other methods, the dominators computed when generating the
  // linear scan block order are correct.
  if (_iterative_dominators) {
    do {
      TRACE_LINEAR_SCAN(1, tty->print_cr("DOM: next iteration of fix-point calculation"));
    } while (compute_dominators_iter());
  }

  // check that dominators are correct
  assert(!compute_dominators_iter(), "fix point not reached");

  // Add Blocks to dominates-Array
  int num_blocks = _linear_scan_order->length();
  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = _linear_scan_order->at(i);

    BlockBegin *dom = block->dominator();
    if (dom) {
      assert(dom->dominator_depth() != -1, "Dominator must have been visited before");
      dom->dominates()->append(block);
      block->set_dominator_depth(dom->dominator_depth() + 1);
    } else {
      block->set_dominator_depth(0);
    }
  }
}


#ifdef ASSERT
void ComputeLinearScanOrder::print_blocks() {
  if (TraceLinearScanLevel >= 2) {
    tty->print_cr("----- loop information:");
    for (int block_idx = 0; block_idx < _linear_scan_order->length(); block_idx++) {
      BlockBegin* cur = _linear_scan_order->at(block_idx);

      tty->print("%4d: B%2d: ", cur->linear_scan_number(), cur->block_id());
      for (int loop_idx = 0; loop_idx < _num_loops; loop_idx++) {
        tty->print ("%d ", is_block_in_loop(loop_idx, cur));
      }
      tty->print_cr(" -> loop_index: %2d, loop_depth: %2d", cur->loop_index(), cur->loop_depth());
    }
  }

  if (TraceLinearScanLevel >= 1) {
    tty->print_cr("----- linear-scan block order:");
    for (int block_idx = 0; block_idx < _linear_scan_order->length(); block_idx++) {
      BlockBegin* cur = _linear_scan_order->at(block_idx);
      tty->print("%4d: B%2d    loop: %2d  depth: %2d", cur->linear_scan_number(), cur->block_id(), cur->loop_index(), cur->loop_depth());

      tty->print(cur->is_set(BlockBegin::exception_entry_flag)         ? " ex" : "   ");
      tty->print(cur->is_set(BlockBegin::critical_edge_split_flag)     ? " ce" : "   ");
      tty->print(cur->is_set(BlockBegin::linear_scan_loop_header_flag) ? " lh" : "   ");
      tty->print(cur->is_set(BlockBegin::linear_scan_loop_end_flag)    ? " le" : "   ");

      if (cur->dominator() != NULL) {
        tty->print("    dom: B%d ", cur->dominator()->block_id());
      } else {
        tty->print("    dom: NULL ");
      }

      if (cur->number_of_preds() > 0) {
        tty->print("    preds: ");
        for (int j = 0; j < cur->number_of_preds(); j++) {
          BlockBegin* pred = cur->pred_at(j);
          tty->print("B%d ", pred->block_id());
        }
      }
      if (cur->number_of_sux() > 0) {
        tty->print("    sux: ");
        for (int j = 0; j < cur->number_of_sux(); j++) {
          BlockBegin* sux = cur->sux_at(j);
          tty->print("B%d ", sux->block_id());
        }
      }
      if (cur->number_of_exception_handlers() > 0) {
        tty->print("    ex: ");
        for (int j = 0; j < cur->number_of_exception_handlers(); j++) {
          BlockBegin* ex = cur->exception_handler_at(j);
          tty->print("B%d ", ex->block_id());
        }
      }
      tty->cr();
    }
  }
}

void ComputeLinearScanOrder::verify() {
  assert(_linear_scan_order->length() == _num_blocks, "wrong number of blocks in list");

  if (StressLinearScan) {
    // blocks are scrambled when StressLinearScan is used
    return;
  }

  // check that all successors of a block have a higher linear-scan-number
  // and that all predecessors of a block have a lower linear-scan-number
  // (only backward branches of loops are ignored)
  int i;
  for (i = 0; i < _linear_scan_order->length(); i++) {
    BlockBegin* cur = _linear_scan_order->at(i);

    assert(cur->linear_scan_number() == i, "incorrect linear_scan_number");
    assert(cur->linear_scan_number() >= 0 && cur->linear_scan_number() == _linear_scan_order->find(cur), "incorrect linear_scan_number");

    int j;
    for (j = cur->number_of_sux() - 1; j >= 0; j--) {
      BlockBegin* sux = cur->sux_at(j);

      assert(sux->linear_scan_number() >= 0 && sux->linear_scan_number() == _linear_scan_order->find(sux), "incorrect linear_scan_number");
      if (!sux->is_set(BlockBegin::backward_branch_target_flag)) {
        assert(cur->linear_scan_number() < sux->linear_scan_number(), "invalid order");
      }
      if (cur->loop_depth() == sux->loop_depth()) {
        assert(cur->loop_index() == sux->loop_index() || sux->is_set(BlockBegin::linear_scan_loop_header_flag), "successing blocks with same loop depth must have same loop index");
      }
    }

    for (j = cur->number_of_preds() - 1; j >= 0; j--) {
      BlockBegin* pred = cur->pred_at(j);

      assert(pred->linear_scan_number() >= 0 && pred->linear_scan_number() == _linear_scan_order->find(pred), "incorrect linear_scan_number");
      if (!cur->is_set(BlockBegin::backward_branch_target_flag)) {
        assert(cur->linear_scan_number() > pred->linear_scan_number(), "invalid order");
      }
      if (cur->loop_depth() == pred->loop_depth()) {
        assert(cur->loop_index() == pred->loop_index() || cur->is_set(BlockBegin::linear_scan_loop_header_flag), "successing blocks with same loop depth must have same loop index");
      }

      assert(cur->dominator()->linear_scan_number() <= cur->pred_at(j)->linear_scan_number(), "dominator must be before predecessors");
    }

    // check dominator
    if (i == 0) {
      assert(cur->dominator() == NULL, "first block has no dominator");
    } else {
      assert(cur->dominator() != NULL, "all but first block must have dominator");
    }
    // Assertion does not hold for exception handlers
    assert(cur->number_of_preds() != 1 || cur->dominator() == cur->pred_at(0) || cur->is_set(BlockBegin::exception_entry_flag), "Single predecessor must also be dominator");
  }

  // check that all loops are continuous
  for (int loop_idx = 0; loop_idx < _num_loops; loop_idx++) {
    int block_idx = 0;
    assert(!is_block_in_loop(loop_idx, _linear_scan_order->at(block_idx)), "the first block must not be present in any loop");

    // skip blocks before the loop
    while (block_idx < _num_blocks && !is_block_in_loop(loop_idx, _linear_scan_order->at(block_idx))) {
      block_idx++;
    }
    // skip blocks of loop
    while (block_idx < _num_blocks && is_block_in_loop(loop_idx, _linear_scan_order->at(block_idx))) {
      block_idx++;
    }
    // after the first non-loop block, there must not be another loop-block
    while (block_idx < _num_blocks) {
      assert(!is_block_in_loop(loop_idx, _linear_scan_order->at(block_idx)), "loop not continuous in linear-scan order");
      block_idx++;
    }
  }
}
#endif // ASSERT


void IR::compute_code() {
  assert(is_valid(), "IR must be valid");

  ComputeLinearScanOrder compute_order(compilation(), start());
  _num_loops = compute_order.num_loops();
  _code = compute_order.linear_scan_order();
}


void IR::compute_use_counts() {
  // make sure all values coming out of this block get evaluated.
  int num_blocks = _code->length();
  for (int i = 0; i < num_blocks; i++) {
    _code->at(i)->end()->state()->pin_stack_for_linear_scan();
  }

  // compute use counts
  UseCountComputer::compute(_code);
}


void IR::iterate_preorder(BlockClosure* closure) {
  assert(is_valid(), "IR must be valid");
  start()->iterate_preorder(closure);
}


void IR::iterate_postorder(BlockClosure* closure) {
  assert(is_valid(), "IR must be valid");
  start()->iterate_postorder(closure);
}

void IR::iterate_linear_scan_order(BlockClosure* closure) {
  linear_scan_order()->iterate_forward(closure);
}


#ifndef PRODUCT
class BlockPrinter: public BlockClosure {
 private:
  InstructionPrinter* _ip;
  bool                _cfg_only;
  bool                _live_only;

 public:
  BlockPrinter(InstructionPrinter* ip, bool cfg_only, bool live_only = false) {
    _ip       = ip;
    _cfg_only = cfg_only;
    _live_only = live_only;
  }

  virtual void block_do(BlockBegin* block) {
    if (_cfg_only) {
      _ip->print_instr(block); tty->cr();
    } else {
      block->print_block(*_ip, _live_only);
    }
  }
};


void IR::print(BlockBegin* start, bool cfg_only, bool live_only) {
  ttyLocker ttyl;
  InstructionPrinter ip(!cfg_only);
  BlockPrinter bp(&ip, cfg_only, live_only);
  start->iterate_preorder(&bp);
  tty->cr();
}

void IR::print(bool cfg_only, bool live_only) {
  if (is_valid()) {
    print(start(), cfg_only, live_only);
  } else {
    tty->print_cr("invalid IR");
  }
}


typedef GrowableArray<BlockList*> BlockListList;

class PredecessorValidator : public BlockClosure {
 private:
  BlockListList* _predecessors;
  BlockList*     _blocks;

  static int cmp(BlockBegin** a, BlockBegin** b) {
    return (*a)->block_id() - (*b)->block_id();
  }

 public:
  PredecessorValidator(IR* hir) {
    ResourceMark rm;
    _predecessors = new BlockListList(BlockBegin::number_of_blocks(), BlockBegin::number_of_blocks(), NULL);
    _blocks = new BlockList();

    int i;
    hir->start()->iterate_preorder(this);
    if (hir->code() != NULL) {
      assert(hir->code()->length() == _blocks->length(), "must match");
      for (i = 0; i < _blocks->length(); i++) {
        assert(hir->code()->contains(_blocks->at(i)), "should be in both lists");
      }
    }

    for (i = 0; i < _blocks->length(); i++) {
      BlockBegin* block = _blocks->at(i);
      BlockList* preds = _predecessors->at(block->block_id());
      if (preds == NULL) {
        assert(block->number_of_preds() == 0, "should be the same");
        continue;
      }

      // clone the pred list so we can mutate it
      BlockList* pred_copy = new BlockList();
      int j;
      for (j = 0; j < block->number_of_preds(); j++) {
        pred_copy->append(block->pred_at(j));
      }
      // sort them in the same order
      preds->sort(cmp);
      pred_copy->sort(cmp);
      int length = MIN2(preds->length(), block->number_of_preds());
      for (j = 0; j < block->number_of_preds(); j++) {
        assert(preds->at(j) == pred_copy->at(j), "must match");
      }

      assert(preds->length() == block->number_of_preds(), "should be the same");
    }
  }

  virtual void block_do(BlockBegin* block) {
    _blocks->append(block);
    BlockEnd* be = block->end();
    int n = be->number_of_sux();
    int i;
    for (i = 0; i < n; i++) {
      BlockBegin* sux = be->sux_at(i);
      assert(!sux->is_set(BlockBegin::exception_entry_flag), "must not be xhandler");

      BlockList* preds = _predecessors->at_grow(sux->block_id(), NULL);
      if (preds == NULL) {
        preds = new BlockList();
        _predecessors->at_put(sux->block_id(), preds);
      }
      preds->append(block);
    }

    n = block->number_of_exception_handlers();
    for (i = 0; i < n; i++) {
      BlockBegin* sux = block->exception_handler_at(i);
      assert(sux->is_set(BlockBegin::exception_entry_flag), "must be xhandler");

      BlockList* preds = _predecessors->at_grow(sux->block_id(), NULL);
      if (preds == NULL) {
        preds = new BlockList();
        _predecessors->at_put(sux->block_id(), preds);
      }
      preds->append(block);
    }
  }
};

class VerifyBlockBeginField : public BlockClosure {

public:

  virtual void block_do(BlockBegin *block) {
    for ( Instruction *cur = block; cur != NULL; cur = cur->next()) {
      assert(cur->block() == block, "Block begin is not correct");
    }
  }
};

void IR::verify() {
#ifdef ASSERT
  PredecessorValidator pv(this);
  VerifyBlockBeginField verifier;
  this->iterate_postorder(&verifier);
#endif
}

#endif // PRODUCT

void SubstitutionResolver::visit(Value* v) {
  Value v0 = *v;
  if (v0) {
    Value vs = v0->subst();
    if (vs != v0) {
      *v = v0->subst();
    }
  }
}

#ifdef ASSERT
class SubstitutionChecker: public ValueVisitor {
  void visit(Value* v) {
    Value v0 = *v;
    if (v0) {
      Value vs = v0->subst();
      assert(vs == v0, "missed substitution");
    }
  }
};
#endif


void SubstitutionResolver::block_do(BlockBegin* block) {
  Instruction* last = NULL;
  for (Instruction* n = block; n != NULL;) {
    n->values_do(this);
    // need to remove this instruction from the instruction stream
    if (n->subst() != n) {
      guarantee(last != NULL, "must have last");
      last->set_next(n->next());
    } else {
      last = n;
    }
    n = last->next();
  }

#ifdef ASSERT
  SubstitutionChecker check_substitute;
  if (block->state()) block->state()->values_do(&check_substitute);
  block->block_values_do(&check_substitute);
  if (block->end() && block->end()->state()) block->end()->state()->values_do(&check_substitute);
#endif
}
