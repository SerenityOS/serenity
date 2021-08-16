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

#include "precompiled.hpp"
#include "classfile/vmSymbols.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constantPool.hpp"
#include "oops/generateOopMap.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "runtime/relocator.hpp"
#include "runtime/timerTrace.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/ostream.hpp"

//
//
// Compute stack layouts for each instruction in method.
//
//  Problems:
//  - What to do about jsr with different types of local vars?
//  Need maps that are conditional on jsr path?
//  - Jsr and exceptions should be done more efficiently (the retAddr stuff)
//
//  Alternative:
//  - Could extend verifier to provide this information.
//    For: one fewer abstract interpreter to maintain. Against: the verifier
//    solves a bigger problem so slower (undesirable to force verification of
//    everything?).
//
//  Algorithm:
//    Partition bytecodes into basic blocks
//    For each basic block: store entry state (vars, stack). For instructions
//    inside basic blocks we do not store any state (instead we recompute it
//    from state produced by previous instruction).
//
//    Perform abstract interpretation of bytecodes over this lattice:
//
//                _--'#'--_
//               /  /  \   \
//             /   /     \   \
//            /    |     |     \
//          'r'   'v'   'p'   ' '
//           \     |     |     /
//            \    \     /    /
//              \   \   /    /
//                -- '@' --
//
//    '#'  top, result of conflict merge
//    'r'  reference type
//    'v'  value type
//    'p'  pc type for jsr/ret
//    ' '  uninitialized; never occurs on operand stack in Java
//    '@'  bottom/unexecuted; initial state each bytecode.
//
//    Basic block headers are the only merge points. We use this iteration to
//    compute the information:
//
//    find basic blocks;
//    initialize them with uninitialized state;
//    initialize first BB according to method signature;
//    mark first BB changed
//    while (some BB is changed) do {
//      perform abstract interpration of all bytecodes in BB;
//      merge exit state of BB into entry state of all successor BBs,
//      noting if any of these change;
//    }
//
//  One additional complication is necessary. The jsr instruction pushes
//  a return PC on the stack (a 'p' type in the abstract interpretation).
//  To be able to process "ret" bytecodes, we keep track of these return
//  PC's in a 'retAddrs' structure in abstract interpreter context (when
//  processing a "ret" bytecodes, it is not sufficient to know that it gets
//  an argument of the right type 'p'; we need to know which address it
//  returns to).
//
// (Note this comment is borrowed form the original author of the algorithm)

// ComputeCallStack
//
// Specialization of SignatureIterator - compute the effects of a call
//
class ComputeCallStack : public SignatureIterator {
  CellTypeState *_effect;
  int _idx;

  void setup();
  void set(CellTypeState state)         { _effect[_idx++] = state; }
  int  length()                         { return _idx; };

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type, bool for_return = false) {
    if (for_return && type == T_VOID) {
      set(CellTypeState::bottom);
    } else if (is_reference_type(type)) {
      set(CellTypeState::ref);
    } else {
      assert(is_java_primitive(type), "");
      set(CellTypeState::value);
      if (is_double_word_type(type)) {
        set(CellTypeState::value);
      }
    }
  }

 public:
  ComputeCallStack(Symbol* signature) : SignatureIterator(signature) {};

  // Compute methods
  int compute_for_parameters(bool is_static, CellTypeState *effect) {
    _idx    = 0;
    _effect = effect;

    if (!is_static)
      effect[_idx++] = CellTypeState::ref;

    do_parameters_on(this);

    return length();
  };

  int compute_for_returntype(CellTypeState *effect) {
    _idx    = 0;
    _effect = effect;
    do_type(return_type(), true);
    set(CellTypeState::bottom);  // Always terminate with a bottom state, so ppush works

    return length();
  }
};

//=========================================================================================
// ComputeEntryStack
//
// Specialization of SignatureIterator - in order to set up first stack frame
//
class ComputeEntryStack : public SignatureIterator {
  CellTypeState *_effect;
  int _idx;

  void setup();
  void set(CellTypeState state)         { _effect[_idx++] = state; }
  int  length()                         { return _idx; };

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type, bool for_return = false) {
    if (for_return && type == T_VOID) {
      set(CellTypeState::bottom);
    } else if (is_reference_type(type)) {
      set(CellTypeState::make_slot_ref(_idx));
    } else {
      assert(is_java_primitive(type), "");
      set(CellTypeState::value);
      if (is_double_word_type(type)) {
        set(CellTypeState::value);
      }
    }
  }

 public:
  ComputeEntryStack(Symbol* signature) : SignatureIterator(signature) {};

  // Compute methods
  int compute_for_parameters(bool is_static, CellTypeState *effect) {
    _idx    = 0;
    _effect = effect;

    if (!is_static)
      effect[_idx++] = CellTypeState::make_slot_ref(0);

    do_parameters_on(this);

    return length();
  };

  int compute_for_returntype(CellTypeState *effect) {
    _idx    = 0;
    _effect = effect;
    do_type(return_type(), true);
    set(CellTypeState::bottom);  // Always terminate with a bottom state, so ppush works

    return length();
  }
};

//=====================================================================================
//
// Implementation of RetTable/RetTableEntry
//
// Contains function to itereate through all bytecodes
// and find all return entry points
//
int RetTable::_init_nof_entries = 10;
int RetTableEntry::_init_nof_jsrs = 5;

RetTableEntry::RetTableEntry(int target, RetTableEntry *next) {
  _target_bci = target;
  _jsrs = new GrowableArray<intptr_t>(_init_nof_jsrs);
  _next = next;
}

void RetTableEntry::add_delta(int bci, int delta) {
  if (_target_bci > bci) _target_bci += delta;

  for (int k = 0; k < _jsrs->length(); k++) {
    int jsr = _jsrs->at(k);
    if (jsr > bci) _jsrs->at_put(k, jsr+delta);
  }
}

void RetTable::compute_ret_table(const methodHandle& method) {
  BytecodeStream i(method);
  Bytecodes::Code bytecode;

  while( (bytecode = i.next()) >= 0) {
    switch (bytecode) {
      case Bytecodes::_jsr:
        add_jsr(i.next_bci(), i.dest());
        break;
      case Bytecodes::_jsr_w:
        add_jsr(i.next_bci(), i.dest_w());
        break;
      default:
        break;
    }
  }
}

void RetTable::add_jsr(int return_bci, int target_bci) {
  RetTableEntry* entry = _first;

  // Scan table for entry
  for (;entry && entry->target_bci() != target_bci; entry = entry->next());

  if (!entry) {
    // Allocate new entry and put in list
    entry = new RetTableEntry(target_bci, _first);
    _first = entry;
  }

  // Now "entry" is set.  Make sure that the entry is initialized
  // and has room for the new jsr.
  entry->add_jsr(return_bci);
}

RetTableEntry* RetTable::find_jsrs_for_target(int targBci) {
  RetTableEntry *cur = _first;

  while(cur) {
    assert(cur->target_bci() != -1, "sanity check");
    if (cur->target_bci() == targBci)  return cur;
    cur = cur->next();
  }
  ShouldNotReachHere();
  return NULL;
}

// The instruction at bci is changing size by "delta".  Update the return map.
void RetTable::update_ret_table(int bci, int delta) {
  RetTableEntry *cur = _first;
  while(cur) {
    cur->add_delta(bci, delta);
    cur = cur->next();
  }
}

//
// Celltype state
//

CellTypeState CellTypeState::bottom      = CellTypeState::make_bottom();
CellTypeState CellTypeState::uninit      = CellTypeState::make_any(uninit_value);
CellTypeState CellTypeState::ref         = CellTypeState::make_any(ref_conflict);
CellTypeState CellTypeState::value       = CellTypeState::make_any(val_value);
CellTypeState CellTypeState::refUninit   = CellTypeState::make_any(ref_conflict | uninit_value);
CellTypeState CellTypeState::top         = CellTypeState::make_top();
CellTypeState CellTypeState::addr        = CellTypeState::make_any(addr_conflict);

// Commonly used constants
static CellTypeState epsilonCTS[1] = { CellTypeState::bottom };
static CellTypeState   refCTS   = CellTypeState::ref;
static CellTypeState   valCTS   = CellTypeState::value;
static CellTypeState    vCTS[2] = { CellTypeState::value, CellTypeState::bottom };
static CellTypeState    rCTS[2] = { CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState   rrCTS[3] = { CellTypeState::ref,   CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState   vrCTS[3] = { CellTypeState::value, CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState   vvCTS[3] = { CellTypeState::value, CellTypeState::value, CellTypeState::bottom };
static CellTypeState  rvrCTS[4] = { CellTypeState::ref,   CellTypeState::value, CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState  vvrCTS[4] = { CellTypeState::value, CellTypeState::value, CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState  vvvCTS[4] = { CellTypeState::value, CellTypeState::value, CellTypeState::value, CellTypeState::bottom };
static CellTypeState vvvrCTS[5] = { CellTypeState::value, CellTypeState::value, CellTypeState::value, CellTypeState::ref,   CellTypeState::bottom };
static CellTypeState vvvvCTS[5] = { CellTypeState::value, CellTypeState::value, CellTypeState::value, CellTypeState::value, CellTypeState::bottom };

char CellTypeState::to_char() const {
  if (can_be_reference()) {
    if (can_be_value() || can_be_address())
      return '#';    // Conflict that needs to be rewritten
    else
      return 'r';
  } else if (can_be_value())
    return 'v';
  else if (can_be_address())
    return 'p';
  else if (can_be_uninit())
    return ' ';
  else
    return '@';
}


// Print a detailed CellTypeState.  Indicate all bits that are set.  If
// the CellTypeState represents an address or a reference, print the
// value of the additional information.
void CellTypeState::print(outputStream *os) {
  if (can_be_address()) {
    os->print("(p");
  } else {
    os->print("( ");
  }
  if (can_be_reference()) {
    os->print("r");
  } else {
    os->print(" ");
  }
  if (can_be_value()) {
    os->print("v");
  } else {
    os->print(" ");
  }
  if (can_be_uninit()) {
    os->print("u|");
  } else {
    os->print(" |");
  }
  if (is_info_top()) {
    os->print("Top)");
  } else if (is_info_bottom()) {
    os->print("Bot)");
  } else {
    if (is_reference()) {
      int info = get_info();
      int data = info & ~(ref_not_lock_bit | ref_slot_bit);
      if (info & ref_not_lock_bit) {
        // Not a monitor lock reference.
        if (info & ref_slot_bit) {
          // slot
          os->print("slot%d)", data);
        } else {
          // line
          os->print("line%d)", data);
        }
      } else {
        // lock
        os->print("lock%d)", data);
      }
    } else {
      os->print("%d)", get_info());
    }
  }
}

//
// Basicblock handling methods
//

void GenerateOopMap::initialize_bb() {
  _gc_points = 0;
  _bb_count  = 0;
  _bb_hdr_bits.reinitialize(method()->code_size());
}

void GenerateOopMap::bb_mark_fct(GenerateOopMap *c, int bci, int *data) {
  assert(bci>= 0 && bci < c->method()->code_size(), "index out of bounds");
  if (c->is_bb_header(bci))
     return;

  if (TraceNewOopMapGeneration) {
     tty->print_cr("Basicblock#%d begins at: %d", c->_bb_count, bci);
  }
  c->set_bbmark_bit(bci);
  c->_bb_count++;
}


void GenerateOopMap::mark_bbheaders_and_count_gc_points() {
  initialize_bb();

  bool fellThrough = false;  // False to get first BB marked.

  // First mark all exception handlers as start of a basic-block
  ExceptionTable excps(method());
  for(int i = 0; i < excps.length(); i ++) {
    bb_mark_fct(this, excps.handler_pc(i), NULL);
  }

  // Then iterate through the code
  BytecodeStream bcs(_method);
  Bytecodes::Code bytecode;

  while( (bytecode = bcs.next()) >= 0) {
    int bci = bcs.bci();

    if (!fellThrough)
        bb_mark_fct(this, bci, NULL);

    fellThrough = jump_targets_do(&bcs, &GenerateOopMap::bb_mark_fct, NULL);

     /* We will also mark successors of jsr's as basic block headers. */
    switch (bytecode) {
      case Bytecodes::_jsr:
        assert(!fellThrough, "should not happen");
        bb_mark_fct(this, bci + Bytecodes::length_for(bytecode), NULL);
        break;
      case Bytecodes::_jsr_w:
        assert(!fellThrough, "should not happen");
        bb_mark_fct(this, bci + Bytecodes::length_for(bytecode), NULL);
        break;
      default:
        break;
    }

    if (possible_gc_point(&bcs))
      _gc_points++;
  }
}

void GenerateOopMap::set_bbmark_bit(int bci) {
  _bb_hdr_bits.at_put(bci, true);
}

void GenerateOopMap::reachable_basicblock(GenerateOopMap *c, int bci, int *data) {
  assert(bci>= 0 && bci < c->method()->code_size(), "index out of bounds");
  BasicBlock* bb = c->get_basic_block_at(bci);
  if (bb->is_dead()) {
    bb->mark_as_alive();
    *data = 1; // Mark basicblock as changed
  }
}


void GenerateOopMap::mark_reachable_code() {
  int change = 1; // int to get function pointers to work

  // Mark entry basic block as alive and all exception handlers
  _basic_blocks[0].mark_as_alive();
  ExceptionTable excps(method());
  for(int i = 0; i < excps.length(); i++) {
    BasicBlock *bb = get_basic_block_at(excps.handler_pc(i));
    // If block is not already alive (due to multiple exception handlers to same bb), then
    // make it alive
    if (bb->is_dead()) bb->mark_as_alive();
  }

  BytecodeStream bcs(_method);

  // Iterate through all basic blocks until we reach a fixpoint
  while (change) {
    change = 0;

    for (int i = 0; i < _bb_count; i++) {
      BasicBlock *bb = &_basic_blocks[i];
      if (bb->is_alive()) {
        // Position bytecodestream at last bytecode in basicblock
        bcs.set_start(bb->_end_bci);
        bcs.next();
        Bytecodes::Code bytecode = bcs.code();
        int bci = bcs.bci();
        assert(bci == bb->_end_bci, "wrong bci");

        bool fell_through = jump_targets_do(&bcs, &GenerateOopMap::reachable_basicblock, &change);

        // We will also mark successors of jsr's as alive.
        switch (bytecode) {
          case Bytecodes::_jsr:
          case Bytecodes::_jsr_w:
            assert(!fell_through, "should not happen");
            reachable_basicblock(this, bci + Bytecodes::length_for(bytecode), &change);
            break;
          default:
            break;
        }
        if (fell_through) {
          // Mark successor as alive
          if (bb[1].is_dead()) {
            bb[1].mark_as_alive();
            change = 1;
          }
        }
      }
    }
  }
}

/* If the current instruction in "c" has no effect on control flow,
   returns "true".  Otherwise, calls "jmpFct" one or more times, with
   "c", an appropriate "pcDelta", and "data" as arguments, then
   returns "false".  There is one exception: if the current
   instruction is a "ret", returns "false" without calling "jmpFct".
   Arrangements for tracking the control flow of a "ret" must be made
   externally. */
bool GenerateOopMap::jump_targets_do(BytecodeStream *bcs, jmpFct_t jmpFct, int *data) {
  int bci = bcs->bci();

  switch (bcs->code()) {
    case Bytecodes::_ifeq:
    case Bytecodes::_ifne:
    case Bytecodes::_iflt:
    case Bytecodes::_ifge:
    case Bytecodes::_ifgt:
    case Bytecodes::_ifle:
    case Bytecodes::_if_icmpeq:
    case Bytecodes::_if_icmpne:
    case Bytecodes::_if_icmplt:
    case Bytecodes::_if_icmpge:
    case Bytecodes::_if_icmpgt:
    case Bytecodes::_if_icmple:
    case Bytecodes::_if_acmpeq:
    case Bytecodes::_if_acmpne:
    case Bytecodes::_ifnull:
    case Bytecodes::_ifnonnull:
      (*jmpFct)(this, bcs->dest(), data);
      (*jmpFct)(this, bci + 3, data);
      break;

    case Bytecodes::_goto:
      (*jmpFct)(this, bcs->dest(), data);
      break;
    case Bytecodes::_goto_w:
      (*jmpFct)(this, bcs->dest_w(), data);
      break;
    case Bytecodes::_tableswitch:
      { Bytecode_tableswitch tableswitch(method(), bcs->bcp());
        int len = tableswitch.length();

        (*jmpFct)(this, bci + tableswitch.default_offset(), data); /* Default. jump address */
        while (--len >= 0) {
          (*jmpFct)(this, bci + tableswitch.dest_offset_at(len), data);
        }
        break;
      }

    case Bytecodes::_lookupswitch:
      { Bytecode_lookupswitch lookupswitch(method(), bcs->bcp());
        int npairs = lookupswitch.number_of_pairs();
        (*jmpFct)(this, bci + lookupswitch.default_offset(), data); /* Default. */
        while(--npairs >= 0) {
          LookupswitchPair pair = lookupswitch.pair_at(npairs);
          (*jmpFct)(this, bci + pair.offset(), data);
        }
        break;
      }
    case Bytecodes::_jsr:
      assert(bcs->is_wide()==false, "sanity check");
      (*jmpFct)(this, bcs->dest(), data);



      break;
    case Bytecodes::_jsr_w:
      (*jmpFct)(this, bcs->dest_w(), data);
      break;
    case Bytecodes::_wide:
      ShouldNotReachHere();
      return true;
      break;
    case Bytecodes::_athrow:
    case Bytecodes::_ireturn:
    case Bytecodes::_lreturn:
    case Bytecodes::_freturn:
    case Bytecodes::_dreturn:
    case Bytecodes::_areturn:
    case Bytecodes::_return:
    case Bytecodes::_ret:
      break;
    default:
      return true;
  }
  return false;
}

/* Requires "pc" to be the head of a basic block; returns that basic
   block. */
BasicBlock *GenerateOopMap::get_basic_block_at(int bci) const {
  BasicBlock* bb = get_basic_block_containing(bci);
  assert(bb->_bci == bci, "should have found BB");
  return bb;
}

// Requires "pc" to be the start of an instruction; returns the basic
//   block containing that instruction. */
BasicBlock  *GenerateOopMap::get_basic_block_containing(int bci) const {
  BasicBlock *bbs = _basic_blocks;
  int lo = 0, hi = _bb_count - 1;

  while (lo <= hi) {
    int m = (lo + hi) / 2;
    int mbci = bbs[m]._bci;
    int nbci;

    if ( m == _bb_count-1) {
      assert( bci >= mbci && bci < method()->code_size(), "sanity check failed");
      return bbs+m;
    } else {
      nbci = bbs[m+1]._bci;
    }

    if ( mbci <= bci && bci < nbci) {
      return bbs+m;
    } else if (mbci < bci) {
      lo = m + 1;
    } else {
      assert(mbci > bci, "sanity check");
      hi = m - 1;
    }
  }

  fatal("should have found BB");
  return NULL;
}

void GenerateOopMap::restore_state(BasicBlock *bb)
{
  memcpy(_state, bb->_state, _state_len*sizeof(CellTypeState));
  _stack_top = bb->_stack_top;
  _monitor_top = bb->_monitor_top;
}

int GenerateOopMap::next_bb_start_pc(BasicBlock *bb) {
 int bbNum = bb - _basic_blocks + 1;
 if (bbNum == _bb_count)
    return method()->code_size();

 return _basic_blocks[bbNum]._bci;
}

//
// CellType handling methods
//

// Allocate memory and throw LinkageError if failure.
#define ALLOC_RESOURCE_ARRAY(var, type, count) \
  var = NEW_RESOURCE_ARRAY_RETURN_NULL(type, count);              \
  if (var == NULL) {                                              \
    report_error("Cannot reserve enough memory to analyze this method"); \
    return;                                                       \
  }


void GenerateOopMap::init_state() {
  _state_len     = _max_locals + _max_stack + _max_monitors;
  ALLOC_RESOURCE_ARRAY(_state, CellTypeState, _state_len);
  memset(_state, 0, _state_len * sizeof(CellTypeState));
  int count = MAX3(_max_locals, _max_stack, _max_monitors) + 1/*for null terminator char */;
  ALLOC_RESOURCE_ARRAY(_state_vec_buf, char, count);
}

void GenerateOopMap::make_context_uninitialized() {
  CellTypeState* vs = vars();

  for (int i = 0; i < _max_locals; i++)
      vs[i] = CellTypeState::uninit;

  _stack_top = 0;
  _monitor_top = 0;
}

int GenerateOopMap::methodsig_to_effect(Symbol* signature, bool is_static, CellTypeState* effect) {
  ComputeEntryStack ces(signature);
  return ces.compute_for_parameters(is_static, effect);
}

// Return result of merging cts1 and cts2.
CellTypeState CellTypeState::merge(CellTypeState cts, int slot) const {
  CellTypeState result;

  assert(!is_bottom() && !cts.is_bottom(),
         "merge of bottom values is handled elsewhere");

  result._state = _state | cts._state;

  // If the top bit is set, we don't need to do any more work.
  if (!result.is_info_top()) {
    assert((result.can_be_address() || result.can_be_reference()),
           "only addresses and references have non-top info");

    if (!equal(cts)) {
      // The two values being merged are different.  Raise to top.
      if (result.is_reference()) {
        result = CellTypeState::make_slot_ref(slot);
      } else {
        result._state |= info_conflict;
      }
    }
  }
  assert(result.is_valid_state(), "checking that CTS merge maintains legal state");

  return result;
}

// Merge the variable state for locals and stack from cts into bbts.
bool GenerateOopMap::merge_local_state_vectors(CellTypeState* cts,
                                               CellTypeState* bbts) {
  int i;
  int len = _max_locals + _stack_top;
  bool change = false;

  for (i = len - 1; i >= 0; i--) {
    CellTypeState v = cts[i].merge(bbts[i], i);
    change = change || !v.equal(bbts[i]);
    bbts[i] = v;
  }

  return change;
}

// Merge the monitor stack state from cts into bbts.
bool GenerateOopMap::merge_monitor_state_vectors(CellTypeState* cts,
                                                 CellTypeState* bbts) {
  bool change = false;
  if (_max_monitors > 0 && _monitor_top != bad_monitors) {
    // If there are no monitors in the program, or there has been
    // a monitor matching error before this point in the program,
    // then we do not merge in the monitor state.

    int base = _max_locals + _max_stack;
    int len = base + _monitor_top;
    for (int i = len - 1; i >= base; i--) {
      CellTypeState v = cts[i].merge(bbts[i], i);

      // Can we prove that, when there has been a change, it will already
      // have been detected at this point?  That would make this equal
      // check here unnecessary.
      change = change || !v.equal(bbts[i]);
      bbts[i] = v;
    }
  }

  return change;
}

void GenerateOopMap::copy_state(CellTypeState *dst, CellTypeState *src) {
  int len = _max_locals + _stack_top;
  for (int i = 0; i < len; i++) {
    if (src[i].is_nonlock_reference()) {
      dst[i] = CellTypeState::make_slot_ref(i);
    } else {
      dst[i] = src[i];
    }
  }
  if (_max_monitors > 0 && _monitor_top != bad_monitors) {
    int base = _max_locals + _max_stack;
    len = base + _monitor_top;
    for (int i = base; i < len; i++) {
      dst[i] = src[i];
    }
  }
}


// Merge the states for the current block and the next.  As long as a
// block is reachable the locals and stack must be merged.  If the
// stack heights don't match then this is a verification error and
// it's impossible to interpret the code.  Simultaneously monitor
// states are being check to see if they nest statically.  If monitor
// depths match up then their states are merged.  Otherwise the
// mismatch is simply recorded and interpretation continues since
// monitor matching is purely informational and doesn't say anything
// about the correctness of the code.
void GenerateOopMap::merge_state_into_bb(BasicBlock *bb) {
  guarantee(bb != NULL, "null basicblock");
  assert(bb->is_alive(), "merging state into a dead basicblock");

  if (_stack_top == bb->_stack_top) {
    // always merge local state even if monitors don't match.
    if (merge_local_state_vectors(_state, bb->_state)) {
      bb->set_changed(true);
    }
    if (_monitor_top == bb->_monitor_top) {
      // monitors still match so continue merging monitor states.
      if (merge_monitor_state_vectors(_state, bb->_state)) {
        bb->set_changed(true);
      }
    } else {
      if (log_is_enabled(Info, monitormismatch)) {
        report_monitor_mismatch("monitor stack height merge conflict");
      }
      // When the monitor stacks are not matched, we set _monitor_top to
      // bad_monitors.  This signals that, from here on, the monitor stack cannot
      // be trusted.  In particular, monitorexit bytecodes may throw
      // exceptions.  We mark this block as changed so that the change
      // propagates properly.
      bb->_monitor_top = bad_monitors;
      bb->set_changed(true);
      _monitor_safe = false;
    }
  } else if (!bb->is_reachable()) {
    // First time we look at this  BB
    copy_state(bb->_state, _state);
    bb->_stack_top = _stack_top;
    bb->_monitor_top = _monitor_top;
    bb->set_changed(true);
  } else {
    verify_error("stack height conflict: %d vs. %d",  _stack_top, bb->_stack_top);
  }
}

void GenerateOopMap::merge_state(GenerateOopMap *gom, int bci, int* data) {
   gom->merge_state_into_bb(gom->get_basic_block_at(bci));
}

void GenerateOopMap::set_var(int localNo, CellTypeState cts) {
  assert(cts.is_reference() || cts.is_value() || cts.is_address(),
         "wrong celltypestate");
  if (localNo < 0 || localNo > _max_locals) {
    verify_error("variable write error: r%d", localNo);
    return;
  }
  vars()[localNo] = cts;
}

CellTypeState GenerateOopMap::get_var(int localNo) {
  assert(localNo < _max_locals + _nof_refval_conflicts, "variable read error");
  if (localNo < 0 || localNo > _max_locals) {
    verify_error("variable read error: r%d", localNo);
    return valCTS; // just to pick something;
  }
  return vars()[localNo];
}

CellTypeState GenerateOopMap::pop() {
  if ( _stack_top <= 0) {
    verify_error("stack underflow");
    return valCTS; // just to pick something
  }
  return  stack()[--_stack_top];
}

void GenerateOopMap::push(CellTypeState cts) {
  if ( _stack_top >= _max_stack) {
    verify_error("stack overflow");
    return;
  }
  stack()[_stack_top++] = cts;
}

CellTypeState GenerateOopMap::monitor_pop() {
  assert(_monitor_top != bad_monitors, "monitor_pop called on error monitor stack");
  if (_monitor_top == 0) {
    // We have detected a pop of an empty monitor stack.
    _monitor_safe = false;
     _monitor_top = bad_monitors;

    if (log_is_enabled(Info, monitormismatch)) {
      report_monitor_mismatch("monitor stack underflow");
    }
    return CellTypeState::ref; // just to keep the analysis going.
  }
  return  monitors()[--_monitor_top];
}

void GenerateOopMap::monitor_push(CellTypeState cts) {
  assert(_monitor_top != bad_monitors, "monitor_push called on error monitor stack");
  if (_monitor_top >= _max_monitors) {
    // Some monitorenter is being executed more than once.
    // This means that the monitor stack cannot be simulated.
    _monitor_safe = false;
    _monitor_top = bad_monitors;

    if (log_is_enabled(Info, monitormismatch)) {
      report_monitor_mismatch("monitor stack overflow");
    }
    return;
  }
  monitors()[_monitor_top++] = cts;
}

//
// Interpretation handling methods
//

void GenerateOopMap::do_interpretation()
{
  // "i" is just for debugging, so we can detect cases where this loop is
  // iterated more than once.
  int i = 0;
  do {
#ifndef PRODUCT
    if (TraceNewOopMapGeneration) {
      tty->print("\n\nIteration #%d of do_interpretation loop, method:\n", i);
      method()->print_name(tty);
      tty->print("\n\n");
    }
#endif
    _conflict = false;
    _monitor_safe = true;
    // init_state is now called from init_basic_blocks.  The length of a
    // state vector cannot be determined until we have made a pass through
    // the bytecodes counting the possible monitor entries.
    if (!_got_error) init_basic_blocks();
    if (!_got_error) setup_method_entry_state();
    if (!_got_error) interp_all();
    if (!_got_error) rewrite_refval_conflicts();
    i++;
  } while (_conflict && !_got_error);
}

void GenerateOopMap::init_basic_blocks() {
  // Note: Could consider reserving only the needed space for each BB's state
  // (entry stack may not be of maximal height for every basic block).
  // But cumbersome since we don't know the stack heights yet.  (Nor the
  // monitor stack heights...)

  ALLOC_RESOURCE_ARRAY(_basic_blocks, BasicBlock, _bb_count);

  // Make a pass through the bytecodes.  Count the number of monitorenters.
  // This can be used an upper bound on the monitor stack depth in programs
  // which obey stack discipline with their monitor usage.  Initialize the
  // known information about basic blocks.
  BytecodeStream j(_method);
  Bytecodes::Code bytecode;

  int bbNo = 0;
  int monitor_count = 0;
  int prev_bci = -1;
  while( (bytecode = j.next()) >= 0) {
    if (j.code() == Bytecodes::_monitorenter) {
      monitor_count++;
    }

    int bci = j.bci();
    if (is_bb_header(bci)) {
      // Initialize the basicblock structure
      BasicBlock *bb   = _basic_blocks + bbNo;
      bb->_bci         = bci;
      bb->_max_locals  = _max_locals;
      bb->_max_stack   = _max_stack;
      bb->set_changed(false);
      bb->_stack_top   = BasicBlock::_dead_basic_block; // Initialize all basicblocks are dead.
      bb->_monitor_top = bad_monitors;

      if (bbNo > 0) {
        _basic_blocks[bbNo - 1]._end_bci = prev_bci;
      }

      bbNo++;
    }
    // Remember prevous bci.
    prev_bci = bci;
  }
  // Set
  _basic_blocks[bbNo-1]._end_bci = prev_bci;


  // Check that the correct number of basicblocks was found
  if (bbNo !=_bb_count) {
    if (bbNo < _bb_count) {
      verify_error("jump into the middle of instruction?");
      return;
    } else {
      verify_error("extra basic blocks - should not happen?");
      return;
    }
  }

  _max_monitors = monitor_count;

  // Now that we have a bound on the depth of the monitor stack, we can
  // initialize the CellTypeState-related information.
  init_state();

  // We allocate space for all state-vectors for all basicblocks in one huge
  // chunk.  Then in the next part of the code, we set a pointer in each
  // _basic_block that points to each piece.

  // The product of bbNo and _state_len can get large if there are lots of
  // basic blocks and stack/locals/monitors.  Need to check to make sure
  // we don't overflow the capacity of a pointer.
  if ((unsigned)bbNo > UINTPTR_MAX / sizeof(CellTypeState) / _state_len) {
    report_error("The amount of memory required to analyze this method "
                 "exceeds addressable range");
    return;
  }

  CellTypeState *basicBlockState;
  ALLOC_RESOURCE_ARRAY(basicBlockState, CellTypeState, bbNo * _state_len);
  memset(basicBlockState, 0, bbNo * _state_len * sizeof(CellTypeState));

  // Make a pass over the basicblocks and assign their state vectors.
  for (int blockNum=0; blockNum < bbNo; blockNum++) {
    BasicBlock *bb = _basic_blocks + blockNum;
    bb->_state = basicBlockState + blockNum * _state_len;

#ifdef ASSERT
    if (blockNum + 1 < bbNo) {
      address bcp = _method->bcp_from(bb->_end_bci);
      int bc_len = Bytecodes::java_length_at(_method(), bcp);
      assert(bb->_end_bci + bc_len == bb[1]._bci, "unmatched bci info in basicblock");
    }
#endif
  }
#ifdef ASSERT
  { BasicBlock *bb = &_basic_blocks[bbNo-1];
    address bcp = _method->bcp_from(bb->_end_bci);
    int bc_len = Bytecodes::java_length_at(_method(), bcp);
    assert(bb->_end_bci + bc_len == _method->code_size(), "wrong end bci");
  }
#endif

  // Mark all alive blocks
  mark_reachable_code();
}

void GenerateOopMap::setup_method_entry_state() {

    // Initialize all locals to 'uninit' and set stack-height to 0
    make_context_uninitialized();

    // Initialize CellState type of arguments
    methodsig_to_effect(method()->signature(), method()->is_static(), vars());

    // If some references must be pre-assigned to null, then set that up
    initialize_vars();

    // This is the start state
    merge_state_into_bb(&_basic_blocks[0]);

    assert(_basic_blocks[0].changed(), "we are not getting off the ground");
}

// The instruction at bci is changing size by "delta".  Update the basic blocks.
void GenerateOopMap::update_basic_blocks(int bci, int delta,
                                         int new_method_size) {
  assert(new_method_size >= method()->code_size() + delta,
         "new method size is too small");

  _bb_hdr_bits.reinitialize(new_method_size);

  for(int k = 0; k < _bb_count; k++) {
    if (_basic_blocks[k]._bci > bci) {
      _basic_blocks[k]._bci     += delta;
      _basic_blocks[k]._end_bci += delta;
    }
    _bb_hdr_bits.at_put(_basic_blocks[k]._bci, true);
  }
}

//
// Initvars handling
//

void GenerateOopMap::initialize_vars() {
  for (int k = 0; k < _init_vars->length(); k++)
    _state[_init_vars->at(k)] = CellTypeState::make_slot_ref(k);
}

void GenerateOopMap::add_to_ref_init_set(int localNo) {

  if (TraceNewOopMapGeneration)
    tty->print_cr("Added init vars: %d", localNo);

  // Is it already in the set?
  if (_init_vars->contains(localNo) )
    return;

   _init_vars->append(localNo);
}

//
// Interpreration code
//

void GenerateOopMap::interp_all() {
  bool change = true;

  while (change && !_got_error) {
    change = false;
    for (int i = 0; i < _bb_count && !_got_error; i++) {
      BasicBlock *bb = &_basic_blocks[i];
      if (bb->changed()) {
         if (_got_error) return;
         change = true;
         bb->set_changed(false);
         interp_bb(bb);
      }
    }
  }
}

void GenerateOopMap::interp_bb(BasicBlock *bb) {

  // We do not want to do anything in case the basic-block has not been initialized. This
  // will happen in the case where there is dead-code hang around in a method.
  assert(bb->is_reachable(), "should be reachable or deadcode exist");
  restore_state(bb);

  BytecodeStream itr(_method);

  // Set iterator interval to be the current basicblock
  int lim_bci = next_bb_start_pc(bb);
  itr.set_interval(bb->_bci, lim_bci);
  assert(lim_bci != bb->_bci, "must be at least one instruction in a basicblock");
  itr.next(); // read first instruction

  // Iterates through all bytecodes except the last in a basic block.
  // We handle the last one special, since there is controlflow change.
  while(itr.next_bci() < lim_bci && !_got_error) {
    if (_has_exceptions || _monitor_top != 0) {
      // We do not need to interpret the results of exceptional
      // continuation from this instruction when the method has no
      // exception handlers and the monitor stack is currently
      // empty.
      do_exception_edge(&itr);
    }
    interp1(&itr);
    itr.next();
  }

  // Handle last instruction.
  if (!_got_error) {
    assert(itr.next_bci() == lim_bci, "must point to end");
    if (_has_exceptions || _monitor_top != 0) {
      do_exception_edge(&itr);
    }
    interp1(&itr);

    bool fall_through = jump_targets_do(&itr, GenerateOopMap::merge_state, NULL);
    if (_got_error)  return;

    if (itr.code() == Bytecodes::_ret) {
      assert(!fall_through, "cannot be set if ret instruction");
      // Automatically handles 'wide' ret indicies
      ret_jump_targets_do(&itr, GenerateOopMap::merge_state, itr.get_index(), NULL);
    } else if (fall_through) {
     // Hit end of BB, but the instr. was a fall-through instruction,
     // so perform transition as if the BB ended in a "jump".
     if (lim_bci != bb[1]._bci) {
       verify_error("bytecodes fell through last instruction");
       return;
     }
     merge_state_into_bb(bb + 1);
    }
  }
}

void GenerateOopMap::do_exception_edge(BytecodeStream* itr) {
  // Only check exception edge, if bytecode can trap
  if (!Bytecodes::can_trap(itr->code())) return;
  switch (itr->code()) {
    case Bytecodes::_aload_0:
      // These bytecodes can trap for rewriting.  We need to assume that
      // they do not throw exceptions to make the monitor analysis work.
      return;

    case Bytecodes::_ireturn:
    case Bytecodes::_lreturn:
    case Bytecodes::_freturn:
    case Bytecodes::_dreturn:
    case Bytecodes::_areturn:
    case Bytecodes::_return:
      // If the monitor stack height is not zero when we leave the method,
      // then we are either exiting with a non-empty stack or we have
      // found monitor trouble earlier in our analysis.  In either case,
      // assume an exception could be taken here.
      if (_monitor_top == 0) {
        return;
      }
      break;

    case Bytecodes::_monitorexit:
      // If the monitor stack height is bad_monitors, then we have detected a
      // monitor matching problem earlier in the analysis.  If the
      // monitor stack height is 0, we are about to pop a monitor
      // off of an empty stack.  In either case, the bytecode
      // could throw an exception.
      if (_monitor_top != bad_monitors && _monitor_top != 0) {
        return;
      }
      break;

    default:
      break;
  }

  if (_has_exceptions) {
    int bci = itr->bci();
    ExceptionTable exct(method());
    for(int i = 0; i< exct.length(); i++) {
      int start_pc   = exct.start_pc(i);
      int end_pc     = exct.end_pc(i);
      int handler_pc = exct.handler_pc(i);
      int catch_type = exct.catch_type_index(i);

      if (start_pc <= bci && bci < end_pc) {
        BasicBlock *excBB = get_basic_block_at(handler_pc);
        guarantee(excBB != NULL, "no basic block for exception");
        CellTypeState *excStk = excBB->stack();
        CellTypeState *cOpStck = stack();
        CellTypeState cOpStck_0 = cOpStck[0];
        int cOpStackTop = _stack_top;

        // Exception stacks are always the same.
        assert(method()->max_stack() > 0, "sanity check");

        // We remembered the size and first element of "cOpStck"
        // above; now we temporarily set them to the appropriate
        // values for an exception handler. */
        cOpStck[0] = CellTypeState::make_slot_ref(_max_locals);
        _stack_top = 1;

        merge_state_into_bb(excBB);

        // Now undo the temporary change.
        cOpStck[0] = cOpStck_0;
        _stack_top = cOpStackTop;

        // If this is a "catch all" handler, then we do not need to
        // consider any additional handlers.
        if (catch_type == 0) {
          return;
        }
      }
    }
  }

  // It is possible that none of the exception handlers would have caught
  // the exception.  In this case, we will exit the method.  We must
  // ensure that the monitor stack is empty in this case.
  if (_monitor_top == 0) {
    return;
  }

  // We pessimistically assume that this exception can escape the
  // method. (It is possible that it will always be caught, but
  // we don't care to analyse the types of the catch clauses.)

  // We don't set _monitor_top to bad_monitors because there are no successors
  // to this exceptional exit.

  if (log_is_enabled(Info, monitormismatch) && _monitor_safe) {
    // We check _monitor_safe so that we only report the first mismatched
    // exceptional exit.
    report_monitor_mismatch("non-empty monitor stack at exceptional exit");
  }
  _monitor_safe = false;

}

void GenerateOopMap::report_monitor_mismatch(const char *msg) {
  ResourceMark rm;
  LogStream ls(Log(monitormismatch)::info());
  ls.print("Monitor mismatch in method ");
  method()->print_short_name(&ls);
  ls.print_cr(": %s", msg);
}

void GenerateOopMap::print_states(outputStream *os,
                                  CellTypeState* vec, int num) {
  for (int i = 0; i < num; i++) {
    vec[i].print(tty);
  }
}

// Print the state values at the current bytecode.
void GenerateOopMap::print_current_state(outputStream   *os,
                                         BytecodeStream *currentBC,
                                         bool            detailed) {
  if (detailed) {
    os->print("     %4d vars     = ", currentBC->bci());
    print_states(os, vars(), _max_locals);
    os->print("    %s", Bytecodes::name(currentBC->code()));
  } else {
    os->print("    %4d  vars = '%s' ", currentBC->bci(),  state_vec_to_string(vars(), _max_locals));
    os->print("     stack = '%s' ", state_vec_to_string(stack(), _stack_top));
    if (_monitor_top != bad_monitors) {
      os->print("  monitors = '%s'  \t%s", state_vec_to_string(monitors(), _monitor_top), Bytecodes::name(currentBC->code()));
    } else {
      os->print("  [bad monitor stack]");
    }
  }

  switch(currentBC->code()) {
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
    case Bytecodes::_invokedynamic:
    case Bytecodes::_invokeinterface: {
      int idx = currentBC->has_index_u4() ? currentBC->get_index_u4() : currentBC->get_index_u2_cpcache();
      ConstantPool* cp      = method()->constants();
      int nameAndTypeIdx    = cp->name_and_type_ref_index_at(idx);
      int signatureIdx      = cp->signature_ref_index_at(nameAndTypeIdx);
      Symbol* signature     = cp->symbol_at(signatureIdx);
      os->print("%s", signature->as_C_string());
    }
    default:
      break;
  }

  if (detailed) {
    os->cr();
    os->print("          stack    = ");
    print_states(os, stack(), _stack_top);
    os->cr();
    if (_monitor_top != bad_monitors) {
      os->print("          monitors = ");
      print_states(os, monitors(), _monitor_top);
    } else {
      os->print("          [bad monitor stack]");
    }
  }

  os->cr();
}

// Sets the current state to be the state after executing the
// current instruction, starting in the current state.
void GenerateOopMap::interp1(BytecodeStream *itr) {
  if (TraceNewOopMapGeneration) {
    print_current_state(tty, itr, TraceNewOopMapGenerationDetailed);
  }

  // Should we report the results? Result is reported *before* the instruction at the current bci is executed.
  // However, not for calls. For calls we do not want to include the arguments, so we postpone the reporting until
  // they have been popped (in method ppl).
  if (_report_result == true) {
    switch(itr->code()) {
      case Bytecodes::_invokevirtual:
      case Bytecodes::_invokespecial:
      case Bytecodes::_invokestatic:
      case Bytecodes::_invokedynamic:
      case Bytecodes::_invokeinterface:
        _itr_send = itr;
        _report_result_for_send = true;
        break;
      default:
       fill_stackmap_for_opcodes(itr, vars(), stack(), _stack_top);
       break;
    }
  }

  // abstract interpretation of current opcode
  switch(itr->code()) {
    case Bytecodes::_nop:                                           break;
    case Bytecodes::_goto:                                          break;
    case Bytecodes::_goto_w:                                        break;
    case Bytecodes::_iinc:                                          break;
    case Bytecodes::_return:            do_return_monitor_check();
                                        break;

    case Bytecodes::_aconst_null:
    case Bytecodes::_new:               ppush1(CellTypeState::make_line_ref(itr->bci()));
                                        break;

    case Bytecodes::_iconst_m1:
    case Bytecodes::_iconst_0:
    case Bytecodes::_iconst_1:
    case Bytecodes::_iconst_2:
    case Bytecodes::_iconst_3:
    case Bytecodes::_iconst_4:
    case Bytecodes::_iconst_5:
    case Bytecodes::_fconst_0:
    case Bytecodes::_fconst_1:
    case Bytecodes::_fconst_2:
    case Bytecodes::_bipush:
    case Bytecodes::_sipush:            ppush1(valCTS);             break;

    case Bytecodes::_lconst_0:
    case Bytecodes::_lconst_1:
    case Bytecodes::_dconst_0:
    case Bytecodes::_dconst_1:          ppush(vvCTS);               break;

    case Bytecodes::_ldc2_w:            ppush(vvCTS);               break;

    case Bytecodes::_ldc:               // fall through:
    case Bytecodes::_ldc_w:             do_ldc(itr->bci());         break;

    case Bytecodes::_iload:
    case Bytecodes::_fload:             ppload(vCTS, itr->get_index()); break;

    case Bytecodes::_lload:
    case Bytecodes::_dload:             ppload(vvCTS,itr->get_index()); break;

    case Bytecodes::_aload:             ppload(rCTS, itr->get_index()); break;

    case Bytecodes::_iload_0:
    case Bytecodes::_fload_0:           ppload(vCTS, 0);            break;
    case Bytecodes::_iload_1:
    case Bytecodes::_fload_1:           ppload(vCTS, 1);            break;
    case Bytecodes::_iload_2:
    case Bytecodes::_fload_2:           ppload(vCTS, 2);            break;
    case Bytecodes::_iload_3:
    case Bytecodes::_fload_3:           ppload(vCTS, 3);            break;

    case Bytecodes::_lload_0:
    case Bytecodes::_dload_0:           ppload(vvCTS, 0);           break;
    case Bytecodes::_lload_1:
    case Bytecodes::_dload_1:           ppload(vvCTS, 1);           break;
    case Bytecodes::_lload_2:
    case Bytecodes::_dload_2:           ppload(vvCTS, 2);           break;
    case Bytecodes::_lload_3:
    case Bytecodes::_dload_3:           ppload(vvCTS, 3);           break;

    case Bytecodes::_aload_0:           ppload(rCTS, 0);            break;
    case Bytecodes::_aload_1:           ppload(rCTS, 1);            break;
    case Bytecodes::_aload_2:           ppload(rCTS, 2);            break;
    case Bytecodes::_aload_3:           ppload(rCTS, 3);            break;

    case Bytecodes::_iaload:
    case Bytecodes::_faload:
    case Bytecodes::_baload:
    case Bytecodes::_caload:
    case Bytecodes::_saload:            pp(vrCTS, vCTS); break;

    case Bytecodes::_laload:            pp(vrCTS, vvCTS);  break;
    case Bytecodes::_daload:            pp(vrCTS, vvCTS); break;

    case Bytecodes::_aaload:            pp_new_ref(vrCTS, itr->bci()); break;

    case Bytecodes::_istore:
    case Bytecodes::_fstore:            ppstore(vCTS, itr->get_index()); break;

    case Bytecodes::_lstore:
    case Bytecodes::_dstore:            ppstore(vvCTS, itr->get_index()); break;

    case Bytecodes::_astore:            do_astore(itr->get_index());     break;

    case Bytecodes::_istore_0:
    case Bytecodes::_fstore_0:          ppstore(vCTS, 0);           break;
    case Bytecodes::_istore_1:
    case Bytecodes::_fstore_1:          ppstore(vCTS, 1);           break;
    case Bytecodes::_istore_2:
    case Bytecodes::_fstore_2:          ppstore(vCTS, 2);           break;
    case Bytecodes::_istore_3:
    case Bytecodes::_fstore_3:          ppstore(vCTS, 3);           break;

    case Bytecodes::_lstore_0:
    case Bytecodes::_dstore_0:          ppstore(vvCTS, 0);          break;
    case Bytecodes::_lstore_1:
    case Bytecodes::_dstore_1:          ppstore(vvCTS, 1);          break;
    case Bytecodes::_lstore_2:
    case Bytecodes::_dstore_2:          ppstore(vvCTS, 2);          break;
    case Bytecodes::_lstore_3:
    case Bytecodes::_dstore_3:          ppstore(vvCTS, 3);          break;

    case Bytecodes::_astore_0:          do_astore(0);               break;
    case Bytecodes::_astore_1:          do_astore(1);               break;
    case Bytecodes::_astore_2:          do_astore(2);               break;
    case Bytecodes::_astore_3:          do_astore(3);               break;

    case Bytecodes::_iastore:
    case Bytecodes::_fastore:
    case Bytecodes::_bastore:
    case Bytecodes::_castore:
    case Bytecodes::_sastore:           ppop(vvrCTS);               break;
    case Bytecodes::_lastore:
    case Bytecodes::_dastore:           ppop(vvvrCTS);              break;
    case Bytecodes::_aastore:           ppop(rvrCTS);               break;

    case Bytecodes::_pop:               ppop_any(1);                break;
    case Bytecodes::_pop2:              ppop_any(2);                break;

    case Bytecodes::_dup:               ppdupswap(1, "11");         break;
    case Bytecodes::_dup_x1:            ppdupswap(2, "121");        break;
    case Bytecodes::_dup_x2:            ppdupswap(3, "1321");       break;
    case Bytecodes::_dup2:              ppdupswap(2, "2121");       break;
    case Bytecodes::_dup2_x1:           ppdupswap(3, "21321");      break;
    case Bytecodes::_dup2_x2:           ppdupswap(4, "214321");     break;
    case Bytecodes::_swap:              ppdupswap(2, "12");         break;

    case Bytecodes::_iadd:
    case Bytecodes::_fadd:
    case Bytecodes::_isub:
    case Bytecodes::_fsub:
    case Bytecodes::_imul:
    case Bytecodes::_fmul:
    case Bytecodes::_idiv:
    case Bytecodes::_fdiv:
    case Bytecodes::_irem:
    case Bytecodes::_frem:
    case Bytecodes::_ishl:
    case Bytecodes::_ishr:
    case Bytecodes::_iushr:
    case Bytecodes::_iand:
    case Bytecodes::_ior:
    case Bytecodes::_ixor:
    case Bytecodes::_l2f:
    case Bytecodes::_l2i:
    case Bytecodes::_d2f:
    case Bytecodes::_d2i:
    case Bytecodes::_fcmpl:
    case Bytecodes::_fcmpg:             pp(vvCTS, vCTS); break;

    case Bytecodes::_ladd:
    case Bytecodes::_dadd:
    case Bytecodes::_lsub:
    case Bytecodes::_dsub:
    case Bytecodes::_lmul:
    case Bytecodes::_dmul:
    case Bytecodes::_ldiv:
    case Bytecodes::_ddiv:
    case Bytecodes::_lrem:
    case Bytecodes::_drem:
    case Bytecodes::_land:
    case Bytecodes::_lor:
    case Bytecodes::_lxor:              pp(vvvvCTS, vvCTS); break;

    case Bytecodes::_ineg:
    case Bytecodes::_fneg:
    case Bytecodes::_i2f:
    case Bytecodes::_f2i:
    case Bytecodes::_i2c:
    case Bytecodes::_i2s:
    case Bytecodes::_i2b:               pp(vCTS, vCTS); break;

    case Bytecodes::_lneg:
    case Bytecodes::_dneg:
    case Bytecodes::_l2d:
    case Bytecodes::_d2l:               pp(vvCTS, vvCTS); break;

    case Bytecodes::_lshl:
    case Bytecodes::_lshr:
    case Bytecodes::_lushr:             pp(vvvCTS, vvCTS); break;

    case Bytecodes::_i2l:
    case Bytecodes::_i2d:
    case Bytecodes::_f2l:
    case Bytecodes::_f2d:               pp(vCTS, vvCTS); break;

    case Bytecodes::_lcmp:              pp(vvvvCTS, vCTS); break;
    case Bytecodes::_dcmpl:
    case Bytecodes::_dcmpg:             pp(vvvvCTS, vCTS); break;

    case Bytecodes::_ifeq:
    case Bytecodes::_ifne:
    case Bytecodes::_iflt:
    case Bytecodes::_ifge:
    case Bytecodes::_ifgt:
    case Bytecodes::_ifle:
    case Bytecodes::_tableswitch:       ppop1(valCTS);
                                        break;
    case Bytecodes::_ireturn:
    case Bytecodes::_freturn:           do_return_monitor_check();
                                        ppop1(valCTS);
                                        break;
    case Bytecodes::_if_icmpeq:
    case Bytecodes::_if_icmpne:
    case Bytecodes::_if_icmplt:
    case Bytecodes::_if_icmpge:
    case Bytecodes::_if_icmpgt:
    case Bytecodes::_if_icmple:         ppop(vvCTS);
                                        break;

    case Bytecodes::_lreturn:           do_return_monitor_check();
                                        ppop(vvCTS);
                                        break;

    case Bytecodes::_dreturn:           do_return_monitor_check();
                                        ppop(vvCTS);
                                        break;

    case Bytecodes::_if_acmpeq:
    case Bytecodes::_if_acmpne:         ppop(rrCTS);                 break;

    case Bytecodes::_jsr:               do_jsr(itr->dest());         break;
    case Bytecodes::_jsr_w:             do_jsr(itr->dest_w());       break;

    case Bytecodes::_getstatic:         do_field(true,  true,  itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_putstatic:         do_field(false, true,  itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_getfield:          do_field(true,  false, itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_putfield:          do_field(false, false, itr->get_index_u2_cpcache(), itr->bci()); break;

    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:     do_method(false, false, itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_invokestatic:      do_method(true,  false, itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_invokedynamic:     do_method(true,  false, itr->get_index_u4(),         itr->bci()); break;
    case Bytecodes::_invokeinterface:   do_method(false, true,  itr->get_index_u2_cpcache(), itr->bci()); break;
    case Bytecodes::_newarray:
    case Bytecodes::_anewarray:         pp_new_ref(vCTS, itr->bci()); break;
    case Bytecodes::_checkcast:         do_checkcast(); break;
    case Bytecodes::_arraylength:
    case Bytecodes::_instanceof:        pp(rCTS, vCTS); break;
    case Bytecodes::_monitorenter:      do_monitorenter(itr->bci()); break;
    case Bytecodes::_monitorexit:       do_monitorexit(itr->bci()); break;

    case Bytecodes::_athrow:            // handled by do_exception_edge() BUT ...
                                        // vlh(apple): do_exception_edge() does not get
                                        // called if method has no exception handlers
                                        if ((!_has_exceptions) && (_monitor_top > 0)) {
                                          _monitor_safe = false;
                                        }
                                        break;

    case Bytecodes::_areturn:           do_return_monitor_check();
                                        ppop1(refCTS);
                                        break;
    case Bytecodes::_ifnull:
    case Bytecodes::_ifnonnull:         ppop1(refCTS); break;
    case Bytecodes::_multianewarray:    do_multianewarray(*(itr->bcp()+3), itr->bci()); break;

    case Bytecodes::_wide:              fatal("Iterator should skip this bytecode"); break;
    case Bytecodes::_ret:                                           break;

    // Java opcodes
    case Bytecodes::_lookupswitch:      ppop1(valCTS);             break;

    default:
         tty->print("unexpected opcode: %d\n", itr->code());
         ShouldNotReachHere();
    break;
  }
}

void GenerateOopMap::check_type(CellTypeState expected, CellTypeState actual) {
  if (!expected.equal_kind(actual)) {
    verify_error("wrong type on stack (found: %c expected: %c)", actual.to_char(), expected.to_char());
  }
}

void GenerateOopMap::ppstore(CellTypeState *in, int loc_no) {
  while(!(*in).is_bottom()) {
    CellTypeState expected =*in++;
    CellTypeState actual   = pop();
    check_type(expected, actual);
    assert(loc_no >= 0, "sanity check");
    set_var(loc_no++, actual);
  }
}

void GenerateOopMap::ppload(CellTypeState *out, int loc_no) {
  while(!(*out).is_bottom()) {
    CellTypeState out1 = *out++;
    CellTypeState vcts = get_var(loc_no);
    assert(out1.can_be_reference() || out1.can_be_value(),
           "can only load refs. and values.");
    if (out1.is_reference()) {
      assert(loc_no>=0, "sanity check");
      if (!vcts.is_reference()) {
        // We were asked to push a reference, but the type of the
        // variable can be something else
        _conflict = true;
        if (vcts.can_be_uninit()) {
          // It is a ref-uninit conflict (at least). If there are other
          // problems, we'll get them in the next round
          add_to_ref_init_set(loc_no);
          vcts = out1;
        } else {
          // It wasn't a ref-uninit conflict. So must be a
          // ref-val or ref-pc conflict. Split the variable.
          record_refval_conflict(loc_no);
          vcts = out1;
        }
        push(out1); // recover...
      } else {
        push(vcts); // preserve reference.
      }
      // Otherwise it is a conflict, but one that verification would
      // have caught if illegal. In particular, it can't be a topCTS
      // resulting from mergeing two difference pcCTS's since the verifier
      // would have rejected any use of such a merge.
    } else {
      push(out1); // handle val/init conflict
    }
    loc_no++;
  }
}

void GenerateOopMap::ppdupswap(int poplen, const char *out) {
  CellTypeState actual[5];
  assert(poplen < 5, "this must be less than length of actual vector");

  // Pop all arguments.
  for (int i = 0; i < poplen; i++) {
    actual[i] = pop();
  }
  // Field _state is uninitialized when calling push.
  for (int i = poplen; i < 5; i++) {
    actual[i] = CellTypeState::uninit;
  }

  // put them back
  char push_ch = *out++;
  while (push_ch != '\0') {
    int idx = push_ch - '1';
    assert(idx >= 0 && idx < poplen, "wrong arguments");
    push(actual[idx]);
    push_ch = *out++;
  }
}

void GenerateOopMap::ppop1(CellTypeState out) {
  CellTypeState actual = pop();
  check_type(out, actual);
}

void GenerateOopMap::ppop(CellTypeState *out) {
  while (!(*out).is_bottom()) {
    ppop1(*out++);
  }
}

void GenerateOopMap::ppush1(CellTypeState in) {
  assert(in.is_reference() | in.is_value(), "sanity check");
  push(in);
}

void GenerateOopMap::ppush(CellTypeState *in) {
  while (!(*in).is_bottom()) {
    ppush1(*in++);
  }
}

void GenerateOopMap::pp(CellTypeState *in, CellTypeState *out) {
  ppop(in);
  ppush(out);
}

void GenerateOopMap::pp_new_ref(CellTypeState *in, int bci) {
  ppop(in);
  ppush1(CellTypeState::make_line_ref(bci));
}

void GenerateOopMap::ppop_any(int poplen) {
  if (_stack_top >= poplen) {
    _stack_top -= poplen;
  } else {
    verify_error("stack underflow");
  }
}

// Replace all occurences of the state 'match' with the state 'replace'
// in our current state vector.
void GenerateOopMap::replace_all_CTS_matches(CellTypeState match,
                                             CellTypeState replace) {
  int i;
  int len = _max_locals + _stack_top;
  bool change = false;

  for (i = len - 1; i >= 0; i--) {
    if (match.equal(_state[i])) {
      _state[i] = replace;
    }
  }

  if (_monitor_top > 0) {
    int base = _max_locals + _max_stack;
    len = base + _monitor_top;
    for (i = len - 1; i >= base; i--) {
      if (match.equal(_state[i])) {
        _state[i] = replace;
      }
    }
  }
}

void GenerateOopMap::do_checkcast() {
  CellTypeState actual = pop();
  check_type(refCTS, actual);
  push(actual);
}

void GenerateOopMap::do_monitorenter(int bci) {
  CellTypeState actual = pop();
  if (_monitor_top == bad_monitors) {
    return;
  }

  // Bail out when we get repeated locks on an identical monitor.  This case
  // isn't too hard to handle and can be made to work if supporting nested
  // redundant synchronized statements becomes a priority.
  //
  // See also "Note" in do_monitorexit(), below.
  if (actual.is_lock_reference()) {
    _monitor_top = bad_monitors;
    _monitor_safe = false;

    if (log_is_enabled(Info, monitormismatch)) {
      report_monitor_mismatch("nested redundant lock -- bailout...");
    }
    return;
  }

  CellTypeState lock = CellTypeState::make_lock_ref(bci);
  check_type(refCTS, actual);
  if (!actual.is_info_top()) {
    replace_all_CTS_matches(actual, lock);
    monitor_push(lock);
  }
}

void GenerateOopMap::do_monitorexit(int bci) {
  CellTypeState actual = pop();
  if (_monitor_top == bad_monitors) {
    return;
  }
  check_type(refCTS, actual);
  CellTypeState expected = monitor_pop();
  if (!actual.is_lock_reference() || !expected.equal(actual)) {
    // The monitor we are exiting is not verifiably the one
    // on the top of our monitor stack.  This causes a monitor
    // mismatch.
    _monitor_top = bad_monitors;
    _monitor_safe = false;

    // We need to mark this basic block as changed so that
    // this monitorexit will be visited again.  We need to
    // do this to ensure that we have accounted for the
    // possibility that this bytecode will throw an
    // exception.
    BasicBlock* bb = get_basic_block_containing(bci);
    guarantee(bb != NULL, "no basic block for bci");
    bb->set_changed(true);
    bb->_monitor_top = bad_monitors;

    if (log_is_enabled(Info, monitormismatch)) {
      report_monitor_mismatch("improper monitor pair");
    }
  } else {
    // This code is a fix for the case where we have repeated
    // locking of the same object in straightline code.  We clear
    // out the lock when it is popped from the monitor stack
    // and replace it with an unobtrusive reference value that can
    // be locked again.
    //
    // Note: when generateOopMap is fixed to properly handle repeated,
    //       nested, redundant locks on the same object, then this
    //       fix will need to be removed at that time.
    replace_all_CTS_matches(actual, CellTypeState::make_line_ref(bci));
  }
}

void GenerateOopMap::do_return_monitor_check() {
  if (_monitor_top > 0) {
    // The monitor stack must be empty when we leave the method
    // for the monitors to be properly matched.
    _monitor_safe = false;

    // Since there are no successors to the *return bytecode, it
    // isn't necessary to set _monitor_top to bad_monitors.

    if (log_is_enabled(Info, monitormismatch)) {
      report_monitor_mismatch("non-empty monitor stack at return");
    }
  }
}

void GenerateOopMap::do_jsr(int targ_bci) {
  push(CellTypeState::make_addr(targ_bci));
}



void GenerateOopMap::do_ldc(int bci) {
  Bytecode_loadconstant ldc(methodHandle(Thread::current(), method()), bci);
  ConstantPool* cp  = method()->constants();
  constantTag tag = cp->tag_at(ldc.pool_index()); // idx is index in resolved_references
  BasicType       bt  = ldc.result_type();
#ifdef ASSERT
  BasicType   tag_bt = (tag.is_dynamic_constant() || tag.is_dynamic_constant_in_error()) ? bt : tag.basic_type();
  assert(bt == tag_bt, "same result");
#endif
  CellTypeState   cts;
  if (is_reference_type(bt)) {  // could be T_ARRAY with condy
    assert(!tag.is_string_index() && !tag.is_klass_index(), "Unexpected index tag");
    cts = CellTypeState::make_line_ref(bci);
  } else {
    cts = valCTS;
  }
  ppush1(cts);
}

void GenerateOopMap::do_multianewarray(int dims, int bci) {
  assert(dims >= 1, "sanity check");
  for(int i = dims -1; i >=0; i--) {
    ppop1(valCTS);
  }
  ppush1(CellTypeState::make_line_ref(bci));
}

void GenerateOopMap::do_astore(int idx) {
  CellTypeState r_or_p = pop();
  if (!r_or_p.is_address() && !r_or_p.is_reference()) {
    // We actually expected ref or pc, but we only report that we expected a ref. It does not
    // really matter (at least for now)
    verify_error("wrong type on stack (found: %c, expected: {pr})", r_or_p.to_char());
    return;
  }
  set_var(idx, r_or_p);
}

// Copies bottom/zero terminated CTS string from "src" into "dst".
//   Does NOT terminate with a bottom. Returns the number of cells copied.
int GenerateOopMap::copy_cts(CellTypeState *dst, CellTypeState *src) {
  int idx = 0;
  while (!src[idx].is_bottom()) {
    dst[idx] = src[idx];
    idx++;
  }
  return idx;
}

void GenerateOopMap::do_field(int is_get, int is_static, int idx, int bci) {
  // Dig up signature for field in constant pool
  ConstantPool* cp     = method()->constants();
  int nameAndTypeIdx     = cp->name_and_type_ref_index_at(idx);
  int signatureIdx       = cp->signature_ref_index_at(nameAndTypeIdx);
  Symbol* signature      = cp->symbol_at(signatureIdx);

  CellTypeState temp[4];
  CellTypeState *eff  = signature_to_effect(signature, bci, temp);

  CellTypeState in[4];
  CellTypeState *out;
  int i =  0;

  if (is_get) {
    out = eff;
  } else {
    out = epsilonCTS;
    i   = copy_cts(in, eff);
  }
  if (!is_static) in[i++] = CellTypeState::ref;
  in[i] = CellTypeState::bottom;
  assert(i<=3, "sanity check");
  pp(in, out);
}

void GenerateOopMap::do_method(int is_static, int is_interface, int idx, int bci) {
 // Dig up signature for field in constant pool
  ConstantPool* cp  = _method->constants();
  Symbol* signature   = cp->signature_ref_at(idx);

  // Parse method signature
  CellTypeState out[4];
  CellTypeState in[MAXARGSIZE+1];   // Includes result
  ComputeCallStack cse(signature);

  // Compute return type
  int res_length=  cse.compute_for_returntype(out);

  // Temporary hack.
  if (out[0].equal(CellTypeState::ref) && out[1].equal(CellTypeState::bottom)) {
    out[0] = CellTypeState::make_line_ref(bci);
  }

  assert(res_length<=4, "max value should be vv");

  // Compute arguments
  int arg_length = cse.compute_for_parameters(is_static != 0, in);
  assert(arg_length<=MAXARGSIZE, "too many locals");

  // Pop arguments
  for (int i = arg_length - 1; i >= 0; i--) ppop1(in[i]);// Do args in reverse order.

  // Report results
  if (_report_result_for_send == true) {
     fill_stackmap_for_opcodes(_itr_send, vars(), stack(), _stack_top);
     _report_result_for_send = false;
  }

  // Push return address
  ppush(out);
}

// This is used to parse the signature for fields, since they are very simple...
CellTypeState *GenerateOopMap::signature_to_effect(const Symbol* sig, int bci, CellTypeState *out) {
  // Object and array
  BasicType bt = Signature::basic_type(sig);
  if (is_reference_type(bt)) {
    out[0] = CellTypeState::make_line_ref(bci);
    out[1] = CellTypeState::bottom;
    return out;
  }
  if (is_double_word_type(bt)) return vvCTS; // Long and Double
  if (bt == T_VOID) return epsilonCTS;       // Void
  return vCTS;                               // Otherwise
}

uint64_t GenerateOopMap::_total_byte_count = 0;
elapsedTimer GenerateOopMap::_total_oopmap_time;

// This function assumes "bcs" is at a "ret" instruction and that the vars
// state is valid for that instruction. Furthermore, the ret instruction
// must be the last instruction in "bb" (we store information about the
// "ret" in "bb").
void GenerateOopMap::ret_jump_targets_do(BytecodeStream *bcs, jmpFct_t jmpFct, int varNo, int *data) {
  CellTypeState ra = vars()[varNo];
  if (!ra.is_good_address()) {
    verify_error("ret returns from two jsr subroutines?");
    return;
  }
  int target = ra.get_info();

  RetTableEntry* rtEnt = _rt.find_jsrs_for_target(target);
  int bci = bcs->bci();
  for (int i = 0; i < rtEnt->nof_jsrs(); i++) {
    int target_bci = rtEnt->jsrs(i);
    // Make sure a jrtRet does not set the changed bit for dead basicblock.
    BasicBlock* jsr_bb    = get_basic_block_containing(target_bci - 1);
    debug_only(BasicBlock* target_bb = &jsr_bb[1];)
    assert(target_bb  == get_basic_block_at(target_bci), "wrong calc. of successor basicblock");
    bool alive = jsr_bb->is_alive();
    if (TraceNewOopMapGeneration) {
      tty->print("pc = %d, ret -> %d alive: %s\n", bci, target_bci, alive ? "true" : "false");
    }
    if (alive) jmpFct(this, target_bci, data);
  }
}

//
// Debug method
//
char* GenerateOopMap::state_vec_to_string(CellTypeState* vec, int len) {
#ifdef ASSERT
  int checklen = MAX3(_max_locals, _max_stack, _max_monitors) + 1;
  assert(len < checklen, "state_vec_buf overflow");
#endif
  for (int i = 0; i < len; i++) _state_vec_buf[i] = vec[i].to_char();
  _state_vec_buf[len] = 0;
  return _state_vec_buf;
}

void GenerateOopMap::print_time() {
  tty->print_cr ("Accumulated oopmap times:");
  tty->print_cr ("---------------------------");
  tty->print_cr ("  Total : %3.3f sec.", GenerateOopMap::_total_oopmap_time.seconds());
  tty->print_cr ("  (%3.0f bytecodes per sec) ",
  GenerateOopMap::_total_byte_count / GenerateOopMap::_total_oopmap_time.seconds());
}

//
//  ============ Main Entry Point ===========
//
GenerateOopMap::GenerateOopMap(const methodHandle& method) {
  // We have to initialize all variables here, that can be queried directly
  _method = method;
  _max_locals=0;
  _init_vars = NULL;

#ifndef PRODUCT
  // If we are doing a detailed trace, include the regular trace information.
  if (TraceNewOopMapGenerationDetailed) {
    TraceNewOopMapGeneration = true;
  }
#endif
}

bool GenerateOopMap::compute_map(Thread* current) {
#ifndef PRODUCT
  if (TimeOopMap2) {
    method()->print_short_name(tty);
    tty->print("  ");
  }
  if (TimeOopMap) {
    _total_byte_count += method()->code_size();
  }
#endif
  TraceTime t_single("oopmap time", TimeOopMap2);
  TraceTime t_all(NULL, &_total_oopmap_time, TimeOopMap);

  // Initialize values
  _got_error      = false;
  _conflict       = false;
  _max_locals     = method()->max_locals();
  _max_stack      = method()->max_stack();
  _has_exceptions = (method()->has_exception_handler());
  _nof_refval_conflicts = 0;
  _init_vars      = new GrowableArray<intptr_t>(5);  // There are seldom more than 5 init_vars
  _report_result  = false;
  _report_result_for_send = false;
  _new_var_map    = NULL;
  _ret_adr_tos    = new GrowableArray<intptr_t>(5);  // 5 seems like a good number;
  _did_rewriting  = false;
  _did_relocation = false;

  if (TraceNewOopMapGeneration) {
    tty->print("Method name: %s\n", method()->name()->as_C_string());
    if (Verbose) {
      _method->print_codes();
      tty->print_cr("Exception table:");
      ExceptionTable excps(method());
      for(int i = 0; i < excps.length(); i ++) {
        tty->print_cr("[%d - %d] -> %d",
                      excps.start_pc(i), excps.end_pc(i), excps.handler_pc(i));
      }
    }
  }

  // if no code - do nothing
  // compiler needs info
  if (method()->code_size() == 0 || _max_locals + method()->max_stack() == 0) {
    fill_stackmap_prolog(0);
    fill_stackmap_epilog();
    return true;
  }
  // Step 1: Compute all jump targets and their return value
  if (!_got_error)
    _rt.compute_ret_table(_method);

  // Step 2: Find all basic blocks and count GC points
  if (!_got_error)
    mark_bbheaders_and_count_gc_points();

  // Step 3: Calculate stack maps
  if (!_got_error)
    do_interpretation();

  // Step 4:Return results
  if (!_got_error && report_results())
     report_result();

  return !_got_error;
}

// Error handling methods
//
// If we compute from a suitable JavaThread then we create an exception for the GenerateOopMap
// calling code to retrieve (via exception()) and throw if desired (in most cases errors are ignored).
// Otherwise it is considered a fatal error to hit malformed bytecode.
void GenerateOopMap::error_work(const char *format, va_list ap) {
  _got_error = true;
  char msg_buffer[512];
  os::vsnprintf(msg_buffer, sizeof(msg_buffer), format, ap);
  // Append method name
  char msg_buffer2[512];
  os::snprintf(msg_buffer2, sizeof(msg_buffer2), "%s in method %s", msg_buffer, method()->name()->as_C_string());
  Thread* current = Thread::current();
  if (current->can_call_java()) {
    _exception = Exceptions::new_exception(JavaThread::cast(current),
                                           vmSymbols::java_lang_LinkageError(),
                                           msg_buffer2);
  } else {
    fatal("%s", msg_buffer2);
  }
}

void GenerateOopMap::report_error(const char *format, ...) {
  va_list ap;
  va_start(ap, format);
  error_work(format, ap);
}

void GenerateOopMap::verify_error(const char *format, ...) {
  // We do not distinguish between different types of errors for verification
  // errors.  Let the verifier give a better message.
  report_error("Illegal class file encountered. Try running with -Xverify:all");
}

//
// Report result opcodes
//
void GenerateOopMap::report_result() {

  if (TraceNewOopMapGeneration) tty->print_cr("Report result pass");

  // We now want to report the result of the parse
  _report_result = true;

  // Prolog code
  fill_stackmap_prolog(_gc_points);

   // Mark everything changed, then do one interpretation pass.
  for (int i = 0; i<_bb_count; i++) {
    if (_basic_blocks[i].is_reachable()) {
      _basic_blocks[i].set_changed(true);
      interp_bb(&_basic_blocks[i]);
    }
  }

  // Note: Since we are skipping dead-code when we are reporting results, then
  // the no. of encountered gc-points might be fewer than the previously number
  // we have counted. (dead-code is a pain - it should be removed before we get here)
  fill_stackmap_epilog();

  // Report initvars
  fill_init_vars(_init_vars);

  _report_result = false;
}

void GenerateOopMap::result_for_basicblock(int bci) {
 if (TraceNewOopMapGeneration) tty->print_cr("Report result pass for basicblock");

  // We now want to report the result of the parse
  _report_result = true;

  // Find basicblock and report results
  BasicBlock* bb = get_basic_block_containing(bci);
  guarantee(bb != NULL, "no basic block for bci");
  assert(bb->is_reachable(), "getting result from unreachable basicblock");
  bb->set_changed(true);
  interp_bb(bb);
}

//
// Conflict handling code
//

void GenerateOopMap::record_refval_conflict(int varNo) {
  assert(varNo>=0 && varNo< _max_locals, "index out of range");

  if (TraceOopMapRewrites) {
     tty->print("### Conflict detected (local no: %d)\n", varNo);
  }

  if (!_new_var_map) {
    _new_var_map = NEW_RESOURCE_ARRAY(int, _max_locals);
    for (int k = 0; k < _max_locals; k++)  _new_var_map[k] = k;
  }

  if ( _new_var_map[varNo] == varNo) {
    // Check if max. number of locals has been reached
    if (_max_locals + _nof_refval_conflicts >= MAX_LOCAL_VARS) {
      report_error("Rewriting exceeded local variable limit");
      return;
    }
    _new_var_map[varNo] = _max_locals + _nof_refval_conflicts;
    _nof_refval_conflicts++;
  }
}

void GenerateOopMap::rewrite_refval_conflicts()
{
  // We can get here two ways: Either a rewrite conflict was detected, or
  // an uninitialize reference was detected. In the second case, we do not
  // do any rewriting, we just want to recompute the reference set with the
  // new information

  int nof_conflicts = 0;              // Used for debugging only

  if ( _nof_refval_conflicts == 0 )
     return;

  // Check if rewrites are allowed in this parse.
  if (!allow_rewrites()) {
    fatal("Rewriting method not allowed at this stage");
  }


  // Tracing flag
  _did_rewriting = true;

  if (TraceOopMapRewrites) {
    tty->print_cr("ref/value conflict for method %s - bytecodes are getting rewritten", method()->name()->as_C_string());
    method()->print();
    method()->print_codes();
  }

  assert(_new_var_map!=NULL, "nothing to rewrite");
  assert(_conflict==true, "We should not be here");

  compute_ret_adr_at_TOS();
  if (!_got_error) {
    for (int k = 0; k < _max_locals && !_got_error; k++) {
      if (_new_var_map[k] != k) {
        if (TraceOopMapRewrites) {
          tty->print_cr("Rewriting: %d -> %d", k, _new_var_map[k]);
        }
        rewrite_refval_conflict(k, _new_var_map[k]);
        if (_got_error) return;
        nof_conflicts++;
      }
    }
  }

  assert(nof_conflicts == _nof_refval_conflicts, "sanity check");

  // Adjust the number of locals
  method()->set_max_locals(_max_locals+_nof_refval_conflicts);
  _max_locals += _nof_refval_conflicts;

  // That was that...
  _new_var_map = NULL;
  _nof_refval_conflicts = 0;
}

void GenerateOopMap::rewrite_refval_conflict(int from, int to) {
  bool startOver;
  do {
    // Make sure that the BytecodeStream is constructed in the loop, since
    // during rewriting a new method is going to be used, and the next time
    // around we want to use that.
    BytecodeStream bcs(_method);
    startOver = false;

    while( !startOver && !_got_error &&
           // test bcs in case method changed and it became invalid
           bcs.next() >=0) {
      startOver = rewrite_refval_conflict_inst(&bcs, from, to);
    }
  } while (startOver && !_got_error);
}

/* If the current instruction is one that uses local variable "from"
   in a ref way, change it to use "to". There's a subtle reason why we
   renumber the ref uses and not the non-ref uses: non-ref uses may be
   2 slots wide (double, long) which would necessitate keeping track of
   whether we should add one or two variables to the method. If the change
   affected the width of some instruction, returns "TRUE"; otherwise, returns "FALSE".
   Another reason for moving ref's value is for solving (addr, ref) conflicts, which
   both uses aload/astore methods.
*/
bool GenerateOopMap::rewrite_refval_conflict_inst(BytecodeStream *itr, int from, int to) {
  Bytecodes::Code bc = itr->code();
  int index;
  int bci = itr->bci();

  if (is_aload(itr, &index) && index == from) {
    if (TraceOopMapRewrites) {
      tty->print_cr("Rewriting aload at bci: %d", bci);
    }
    return rewrite_load_or_store(itr, Bytecodes::_aload, Bytecodes::_aload_0, to);
  }

  if (is_astore(itr, &index) && index == from) {
    if (!stack_top_holds_ret_addr(bci)) {
      if (TraceOopMapRewrites) {
        tty->print_cr("Rewriting astore at bci: %d", bci);
      }
      return rewrite_load_or_store(itr, Bytecodes::_astore, Bytecodes::_astore_0, to);
    } else {
      if (TraceOopMapRewrites) {
        tty->print_cr("Supress rewriting of astore at bci: %d", bci);
      }
    }
  }

  return false;
}

// The argument to this method is:
// bc : Current bytecode
// bcN : either _aload or _astore
// bc0 : either _aload_0 or _astore_0
bool GenerateOopMap::rewrite_load_or_store(BytecodeStream *bcs, Bytecodes::Code bcN, Bytecodes::Code bc0, unsigned int varNo) {
  assert(bcN == Bytecodes::_astore   || bcN == Bytecodes::_aload,   "wrong argument (bcN)");
  assert(bc0 == Bytecodes::_astore_0 || bc0 == Bytecodes::_aload_0, "wrong argument (bc0)");
  int ilen = Bytecodes::length_at(_method(), bcs->bcp());
  int newIlen;

  if (ilen == 4) {
    // Original instruction was wide; keep it wide for simplicity
    newIlen = 4;
  } else if (varNo < 4)
     newIlen = 1;
  else if (varNo >= 256)
     newIlen = 4;
  else
     newIlen = 2;

  // If we need to relocate in order to patch the byte, we
  // do the patching in a temp. buffer, that is passed to the reloc.
  // The patching of the bytecode stream is then done by the Relocator.
  // This is neccesary, since relocating the instruction at a certain bci, might
  // also relocate that instruction, e.g., if a _goto before it gets widen to a _goto_w.
  // Hence, we do not know which bci to patch after relocation.

  assert(newIlen <= 4, "sanity check");
  u_char inst_buffer[4]; // Max. instruction size is 4.
  address bcp;

  if (newIlen != ilen) {
    // Relocation needed do patching in temp. buffer
    bcp = (address)inst_buffer;
  } else {
    bcp = _method->bcp_from(bcs->bci());
  }

  // Patch either directly in Method* or in temp. buffer
  if (newIlen == 1) {
    assert(varNo < 4, "varNo too large");
    *bcp = bc0 + varNo;
  } else if (newIlen == 2) {
    assert(varNo < 256, "2-byte index needed!");
    *(bcp + 0) = bcN;
    *(bcp + 1) = varNo;
  } else {
    assert(newIlen == 4, "Wrong instruction length");
    *(bcp + 0) = Bytecodes::_wide;
    *(bcp + 1) = bcN;
    Bytes::put_Java_u2(bcp+2, varNo);
  }

  if (newIlen != ilen) {
    expand_current_instr(bcs->bci(), ilen, newIlen, inst_buffer);
  }


  return (newIlen != ilen);
}

class RelocCallback : public RelocatorListener {
 private:
  GenerateOopMap* _gom;
 public:
   RelocCallback(GenerateOopMap* gom) { _gom = gom; };

  // Callback method
  virtual void relocated(int bci, int delta, int new_code_length) {
    _gom->update_basic_blocks  (bci, delta, new_code_length);
    _gom->update_ret_adr_at_TOS(bci, delta);
    _gom->_rt.update_ret_table (bci, delta);
  }
};

// Returns true if expanding was succesful. Otherwise, reports an error and
// returns false.
void GenerateOopMap::expand_current_instr(int bci, int ilen, int newIlen, u_char inst_buffer[]) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  RelocCallback rcb(this);
  Relocator rc(_method, &rcb);
  methodHandle m= rc.insert_space_at(bci, newIlen, inst_buffer, THREAD);
  if (m.is_null() || HAS_PENDING_EXCEPTION) {
    report_error("could not rewrite method - exception occurred or bytecode buffer overflow");
    return;
  }

  // Relocator returns a new method.
  _did_relocation = true;
  _method = m;
}


bool GenerateOopMap::is_astore(BytecodeStream *itr, int *index) {
  Bytecodes::Code bc = itr->code();
  switch(bc) {
    case Bytecodes::_astore_0:
    case Bytecodes::_astore_1:
    case Bytecodes::_astore_2:
    case Bytecodes::_astore_3:
      *index = bc - Bytecodes::_astore_0;
      return true;
    case Bytecodes::_astore:
      *index = itr->get_index();
      return true;
    default:
      return false;
  }
}

bool GenerateOopMap::is_aload(BytecodeStream *itr, int *index) {
  Bytecodes::Code bc = itr->code();
  switch(bc) {
    case Bytecodes::_aload_0:
    case Bytecodes::_aload_1:
    case Bytecodes::_aload_2:
    case Bytecodes::_aload_3:
      *index = bc - Bytecodes::_aload_0;
      return true;

    case Bytecodes::_aload:
      *index = itr->get_index();
      return true;

    default:
      return false;
  }
}


// Return true iff the top of the operand stack holds a return address at
// the current instruction
bool GenerateOopMap::stack_top_holds_ret_addr(int bci) {
  for(int i = 0; i < _ret_adr_tos->length(); i++) {
    if (_ret_adr_tos->at(i) == bci)
      return true;
  }

  return false;
}

void GenerateOopMap::compute_ret_adr_at_TOS() {
  assert(_ret_adr_tos != NULL, "must be initialized");
  _ret_adr_tos->clear();

  for (int i = 0; i < bb_count(); i++) {
    BasicBlock* bb = &_basic_blocks[i];

    // Make sure to only check basicblocks that are reachable
    if (bb->is_reachable()) {

      // For each Basic block we check all instructions
      BytecodeStream bcs(_method);
      bcs.set_interval(bb->_bci, next_bb_start_pc(bb));

      restore_state(bb);

      while (bcs.next()>=0 && !_got_error) {
        // TDT: should this be is_good_address() ?
        if (_stack_top > 0 && stack()[_stack_top-1].is_address()) {
          _ret_adr_tos->append(bcs.bci());
          if (TraceNewOopMapGeneration) {
            tty->print_cr("Ret_adr TOS at bci: %d", bcs.bci());
          }
        }
        interp1(&bcs);
      }
    }
  }
}

void GenerateOopMap::update_ret_adr_at_TOS(int bci, int delta) {
  for(int i = 0; i < _ret_adr_tos->length(); i++) {
    int v = _ret_adr_tos->at(i);
    if (v > bci)  _ret_adr_tos->at_put(i, v + delta);
  }
}

// ===================================================================

#ifndef PRODUCT
int ResolveOopMapConflicts::_nof_invocations  = 0;
int ResolveOopMapConflicts::_nof_rewrites     = 0;
int ResolveOopMapConflicts::_nof_relocations  = 0;
#endif

methodHandle ResolveOopMapConflicts::do_potential_rewrite(TRAPS) {
  if (!compute_map(THREAD)) {
    THROW_HANDLE_(exception(), methodHandle());
  }

#ifndef PRODUCT
  // Tracking and statistics
  if (PrintRewrites) {
    _nof_invocations++;
    if (did_rewriting()) {
      _nof_rewrites++;
      if (did_relocation()) _nof_relocations++;
      tty->print("Method was rewritten %s: ", (did_relocation()) ? "and relocated" : "");
      method()->print_value(); tty->cr();
      tty->print_cr("Cand.: %d rewrts: %d (%d%%) reloc.: %d (%d%%)",
          _nof_invocations,
          _nof_rewrites,    (_nof_rewrites    * 100) / _nof_invocations,
          _nof_relocations, (_nof_relocations * 100) / _nof_invocations);
    }
  }
#endif
  return methodHandle(THREAD, method());
}
