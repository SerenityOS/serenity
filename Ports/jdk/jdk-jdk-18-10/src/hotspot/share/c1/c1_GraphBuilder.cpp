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
#include "c1/c1_CFGPrinter.hpp"
#include "c1/c1_Canonicalizer.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_GraphBuilder.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "ci/ciCallSite.hpp"
#include "ci/ciField.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciMemberName.hpp"
#include "ci/ciSymbols.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compilerEvent.hpp"
#include "interpreter/bytecode.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/powerOfTwo.hpp"

class BlockListBuilder {
 private:
  Compilation* _compilation;
  IRScope*     _scope;

  BlockList    _blocks;                // internal list of all blocks
  BlockList*   _bci2block;             // mapping from bci to blocks for GraphBuilder

  // fields used by mark_loops
  ResourceBitMap _active;              // for iteration of control flow graph
  ResourceBitMap _visited;             // for iteration of control flow graph
  intArray       _loop_map;            // caches the information if a block is contained in a loop
  int            _next_loop_index;     // next free loop number
  int            _next_block_number;   // for reverse postorder numbering of blocks

  // accessors
  Compilation*  compilation() const              { return _compilation; }
  IRScope*      scope() const                    { return _scope; }
  ciMethod*     method() const                   { return scope()->method(); }
  XHandlers*    xhandlers() const                { return scope()->xhandlers(); }

  // unified bailout support
  void          bailout(const char* msg) const   { compilation()->bailout(msg); }
  bool          bailed_out() const               { return compilation()->bailed_out(); }

  // helper functions
  BlockBegin* make_block_at(int bci, BlockBegin* predecessor);
  void handle_exceptions(BlockBegin* current, int cur_bci);
  void handle_jsr(BlockBegin* current, int sr_bci, int next_bci);
  void store_one(BlockBegin* current, int local);
  void store_two(BlockBegin* current, int local);
  void set_entries(int osr_bci);
  void set_leaders();

  void make_loop_header(BlockBegin* block);
  void mark_loops();
  int  mark_loops(BlockBegin* b, bool in_subroutine);

  // debugging
#ifndef PRODUCT
  void print();
#endif

 public:
  // creation
  BlockListBuilder(Compilation* compilation, IRScope* scope, int osr_bci);

  // accessors for GraphBuilder
  BlockList*    bci2block() const                { return _bci2block; }
};


// Implementation of BlockListBuilder

BlockListBuilder::BlockListBuilder(Compilation* compilation, IRScope* scope, int osr_bci)
 : _compilation(compilation)
 , _scope(scope)
 , _blocks(16)
 , _bci2block(new BlockList(scope->method()->code_size(), NULL))
 , _active()         // size not known yet
 , _visited()        // size not known yet
 , _loop_map() // size not known yet
 , _next_loop_index(0)
 , _next_block_number(0)
{
  set_entries(osr_bci);
  set_leaders();
  CHECK_BAILOUT();

  mark_loops();
  NOT_PRODUCT(if (PrintInitialBlockList) print());

#ifndef PRODUCT
  if (PrintCFGToFile) {
    stringStream title;
    title.print("BlockListBuilder ");
    scope->method()->print_name(&title);
    CFGPrinter::print_cfg(_bci2block, title.as_string(), false, false);
  }
#endif
}


void BlockListBuilder::set_entries(int osr_bci) {
  // generate start blocks
  BlockBegin* std_entry = make_block_at(0, NULL);
  if (scope()->caller() == NULL) {
    std_entry->set(BlockBegin::std_entry_flag);
  }
  if (osr_bci != -1) {
    BlockBegin* osr_entry = make_block_at(osr_bci, NULL);
    osr_entry->set(BlockBegin::osr_entry_flag);
  }

  // generate exception entry blocks
  XHandlers* list = xhandlers();
  const int n = list->length();
  for (int i = 0; i < n; i++) {
    XHandler* h = list->handler_at(i);
    BlockBegin* entry = make_block_at(h->handler_bci(), NULL);
    entry->set(BlockBegin::exception_entry_flag);
    h->set_entry_block(entry);
  }
}


BlockBegin* BlockListBuilder::make_block_at(int cur_bci, BlockBegin* predecessor) {
  assert(method()->bci_block_start().at(cur_bci), "wrong block starts of MethodLivenessAnalyzer");

  BlockBegin* block = _bci2block->at(cur_bci);
  if (block == NULL) {
    block = new BlockBegin(cur_bci);
    block->init_stores_to_locals(method()->max_locals());
    _bci2block->at_put(cur_bci, block);
    _blocks.append(block);

    assert(predecessor == NULL || predecessor->bci() < cur_bci, "targets for backward branches must already exist");
  }

  if (predecessor != NULL) {
    if (block->is_set(BlockBegin::exception_entry_flag)) {
      BAILOUT_("Exception handler can be reached by both normal and exceptional control flow", block);
    }

    predecessor->add_successor(block);
    block->increment_total_preds();
  }

  return block;
}


inline void BlockListBuilder::store_one(BlockBegin* current, int local) {
  current->stores_to_locals().set_bit(local);
}
inline void BlockListBuilder::store_two(BlockBegin* current, int local) {
  store_one(current, local);
  store_one(current, local + 1);
}


void BlockListBuilder::handle_exceptions(BlockBegin* current, int cur_bci) {
  // Draws edges from a block to its exception handlers
  XHandlers* list = xhandlers();
  const int n = list->length();

  for (int i = 0; i < n; i++) {
    XHandler* h = list->handler_at(i);

    if (h->covers(cur_bci)) {
      BlockBegin* entry = h->entry_block();
      assert(entry != NULL && entry == _bci2block->at(h->handler_bci()), "entry must be set");
      assert(entry->is_set(BlockBegin::exception_entry_flag), "flag must be set");

      // add each exception handler only once
      if (!current->is_successor(entry)) {
        current->add_successor(entry);
        entry->increment_total_preds();
      }

      // stop when reaching catchall
      if (h->catch_type() == 0) break;
    }
  }
}

void BlockListBuilder::handle_jsr(BlockBegin* current, int sr_bci, int next_bci) {
  // start a new block after jsr-bytecode and link this block into cfg
  make_block_at(next_bci, current);

  // start a new block at the subroutine entry at mark it with special flag
  BlockBegin* sr_block = make_block_at(sr_bci, current);
  if (!sr_block->is_set(BlockBegin::subroutine_entry_flag)) {
    sr_block->set(BlockBegin::subroutine_entry_flag);
  }
}


void BlockListBuilder::set_leaders() {
  bool has_xhandlers = xhandlers()->has_handlers();
  BlockBegin* current = NULL;

  // The information which bci starts a new block simplifies the analysis
  // Without it, backward branches could jump to a bci where no block was created
  // during bytecode iteration. This would require the creation of a new block at the
  // branch target and a modification of the successor lists.
  const BitMap& bci_block_start = method()->bci_block_start();

  ciBytecodeStream s(method());
  while (s.next() != ciBytecodeStream::EOBC()) {
    int cur_bci = s.cur_bci();

    if (bci_block_start.at(cur_bci)) {
      current = make_block_at(cur_bci, current);
    }
    assert(current != NULL, "must have current block");

    if (has_xhandlers && GraphBuilder::can_trap(method(), s.cur_bc())) {
      handle_exceptions(current, cur_bci);
    }

    switch (s.cur_bc()) {
      // track stores to local variables for selective creation of phi functions
      case Bytecodes::_iinc:     store_one(current, s.get_index()); break;
      case Bytecodes::_istore:   store_one(current, s.get_index()); break;
      case Bytecodes::_lstore:   store_two(current, s.get_index()); break;
      case Bytecodes::_fstore:   store_one(current, s.get_index()); break;
      case Bytecodes::_dstore:   store_two(current, s.get_index()); break;
      case Bytecodes::_astore:   store_one(current, s.get_index()); break;
      case Bytecodes::_istore_0: store_one(current, 0); break;
      case Bytecodes::_istore_1: store_one(current, 1); break;
      case Bytecodes::_istore_2: store_one(current, 2); break;
      case Bytecodes::_istore_3: store_one(current, 3); break;
      case Bytecodes::_lstore_0: store_two(current, 0); break;
      case Bytecodes::_lstore_1: store_two(current, 1); break;
      case Bytecodes::_lstore_2: store_two(current, 2); break;
      case Bytecodes::_lstore_3: store_two(current, 3); break;
      case Bytecodes::_fstore_0: store_one(current, 0); break;
      case Bytecodes::_fstore_1: store_one(current, 1); break;
      case Bytecodes::_fstore_2: store_one(current, 2); break;
      case Bytecodes::_fstore_3: store_one(current, 3); break;
      case Bytecodes::_dstore_0: store_two(current, 0); break;
      case Bytecodes::_dstore_1: store_two(current, 1); break;
      case Bytecodes::_dstore_2: store_two(current, 2); break;
      case Bytecodes::_dstore_3: store_two(current, 3); break;
      case Bytecodes::_astore_0: store_one(current, 0); break;
      case Bytecodes::_astore_1: store_one(current, 1); break;
      case Bytecodes::_astore_2: store_one(current, 2); break;
      case Bytecodes::_astore_3: store_one(current, 3); break;

      // track bytecodes that affect the control flow
      case Bytecodes::_athrow:  // fall through
      case Bytecodes::_ret:     // fall through
      case Bytecodes::_ireturn: // fall through
      case Bytecodes::_lreturn: // fall through
      case Bytecodes::_freturn: // fall through
      case Bytecodes::_dreturn: // fall through
      case Bytecodes::_areturn: // fall through
      case Bytecodes::_return:
        current = NULL;
        break;

      case Bytecodes::_ifeq:      // fall through
      case Bytecodes::_ifne:      // fall through
      case Bytecodes::_iflt:      // fall through
      case Bytecodes::_ifge:      // fall through
      case Bytecodes::_ifgt:      // fall through
      case Bytecodes::_ifle:      // fall through
      case Bytecodes::_if_icmpeq: // fall through
      case Bytecodes::_if_icmpne: // fall through
      case Bytecodes::_if_icmplt: // fall through
      case Bytecodes::_if_icmpge: // fall through
      case Bytecodes::_if_icmpgt: // fall through
      case Bytecodes::_if_icmple: // fall through
      case Bytecodes::_if_acmpeq: // fall through
      case Bytecodes::_if_acmpne: // fall through
      case Bytecodes::_ifnull:    // fall through
      case Bytecodes::_ifnonnull:
        make_block_at(s.next_bci(), current);
        make_block_at(s.get_dest(), current);
        current = NULL;
        break;

      case Bytecodes::_goto:
        make_block_at(s.get_dest(), current);
        current = NULL;
        break;

      case Bytecodes::_goto_w:
        make_block_at(s.get_far_dest(), current);
        current = NULL;
        break;

      case Bytecodes::_jsr:
        handle_jsr(current, s.get_dest(), s.next_bci());
        current = NULL;
        break;

      case Bytecodes::_jsr_w:
        handle_jsr(current, s.get_far_dest(), s.next_bci());
        current = NULL;
        break;

      case Bytecodes::_tableswitch: {
        // set block for each case
        Bytecode_tableswitch sw(&s);
        int l = sw.length();
        for (int i = 0; i < l; i++) {
          make_block_at(cur_bci + sw.dest_offset_at(i), current);
        }
        make_block_at(cur_bci + sw.default_offset(), current);
        current = NULL;
        break;
      }

      case Bytecodes::_lookupswitch: {
        // set block for each case
        Bytecode_lookupswitch sw(&s);
        int l = sw.number_of_pairs();
        for (int i = 0; i < l; i++) {
          make_block_at(cur_bci + sw.pair_at(i).offset(), current);
        }
        make_block_at(cur_bci + sw.default_offset(), current);
        current = NULL;
        break;
      }

      default:
        break;
    }
  }
}


void BlockListBuilder::mark_loops() {
  ResourceMark rm;

  _active.initialize(BlockBegin::number_of_blocks());
  _visited.initialize(BlockBegin::number_of_blocks());
  _loop_map = intArray(BlockBegin::number_of_blocks(), BlockBegin::number_of_blocks(), 0);
  _next_loop_index = 0;
  _next_block_number = _blocks.length();

  // recursively iterate the control flow graph
  mark_loops(_bci2block->at(0), false);
  assert(_next_block_number >= 0, "invalid block numbers");

  // Remove dangling Resource pointers before the ResourceMark goes out-of-scope.
  _active.resize(0);
  _visited.resize(0);
}

void BlockListBuilder::make_loop_header(BlockBegin* block) {
  if (block->is_set(BlockBegin::exception_entry_flag)) {
    // exception edges may look like loops but don't mark them as such
    // since it screws up block ordering.
    return;
  }
  if (!block->is_set(BlockBegin::parser_loop_header_flag)) {
    block->set(BlockBegin::parser_loop_header_flag);

    assert(_loop_map.at(block->block_id()) == 0, "must not be set yet");
    assert(0 <= _next_loop_index && _next_loop_index < BitsPerInt, "_next_loop_index is used as a bit-index in integer");
    _loop_map.at_put(block->block_id(), 1 << _next_loop_index);
    if (_next_loop_index < 31) _next_loop_index++;
  } else {
    // block already marked as loop header
    assert(is_power_of_2((unsigned int)_loop_map.at(block->block_id())), "exactly one bit must be set");
  }
}

int BlockListBuilder::mark_loops(BlockBegin* block, bool in_subroutine) {
  int block_id = block->block_id();

  if (_visited.at(block_id)) {
    if (_active.at(block_id)) {
      // reached block via backward branch
      make_loop_header(block);
    }
    // return cached loop information for this block
    return _loop_map.at(block_id);
  }

  if (block->is_set(BlockBegin::subroutine_entry_flag)) {
    in_subroutine = true;
  }

  // set active and visited bits before successors are processed
  _visited.set_bit(block_id);
  _active.set_bit(block_id);

  intptr_t loop_state = 0;
  for (int i = block->number_of_sux() - 1; i >= 0; i--) {
    // recursively process all successors
    loop_state |= mark_loops(block->sux_at(i), in_subroutine);
  }

  // clear active-bit after all successors are processed
  _active.clear_bit(block_id);

  // reverse-post-order numbering of all blocks
  block->set_depth_first_number(_next_block_number);
  _next_block_number--;

  if (loop_state != 0 || in_subroutine ) {
    // block is contained at least in one loop, so phi functions are necessary
    // phi functions are also necessary for all locals stored in a subroutine
    scope()->requires_phi_function().set_union(block->stores_to_locals());
  }

  if (block->is_set(BlockBegin::parser_loop_header_flag)) {
    int header_loop_state = _loop_map.at(block_id);
    assert(is_power_of_2((unsigned)header_loop_state), "exactly one bit must be set");

    // If the highest bit is set (i.e. when integer value is negative), the method
    // has 32 or more loops. This bit is never cleared because it is used for multiple loops
    if (header_loop_state >= 0) {
      clear_bits(loop_state, header_loop_state);
    }
  }

  // cache and return loop information for this block
  _loop_map.at_put(block_id, loop_state);
  return loop_state;
}


#ifndef PRODUCT

int compare_depth_first(BlockBegin** a, BlockBegin** b) {
  return (*a)->depth_first_number() - (*b)->depth_first_number();
}

void BlockListBuilder::print() {
  tty->print("----- initial block list of BlockListBuilder for method ");
  method()->print_short_name();
  tty->cr();

  // better readability if blocks are sorted in processing order
  _blocks.sort(compare_depth_first);

  for (int i = 0; i < _blocks.length(); i++) {
    BlockBegin* cur = _blocks.at(i);
    tty->print("%4d: B%-4d bci: %-4d  preds: %-4d ", cur->depth_first_number(), cur->block_id(), cur->bci(), cur->total_preds());

    tty->print(cur->is_set(BlockBegin::std_entry_flag)               ? " std" : "    ");
    tty->print(cur->is_set(BlockBegin::osr_entry_flag)               ? " osr" : "    ");
    tty->print(cur->is_set(BlockBegin::exception_entry_flag)         ? " ex" : "   ");
    tty->print(cur->is_set(BlockBegin::subroutine_entry_flag)        ? " sr" : "   ");
    tty->print(cur->is_set(BlockBegin::parser_loop_header_flag)      ? " lh" : "   ");

    if (cur->number_of_sux() > 0) {
      tty->print("    sux: ");
      for (int j = 0; j < cur->number_of_sux(); j++) {
        BlockBegin* sux = cur->sux_at(j);
        tty->print("B%d ", sux->block_id());
      }
    }
    tty->cr();
  }
}

#endif


// A simple growable array of Values indexed by ciFields
class FieldBuffer: public CompilationResourceObj {
 private:
  GrowableArray<Value> _values;

 public:
  FieldBuffer() {}

  void kill() {
    _values.trunc_to(0);
  }

  Value at(ciField* field) {
    assert(field->holder()->is_loaded(), "must be a loaded field");
    int offset = field->offset();
    if (offset < _values.length()) {
      return _values.at(offset);
    } else {
      return NULL;
    }
  }

  void at_put(ciField* field, Value value) {
    assert(field->holder()->is_loaded(), "must be a loaded field");
    int offset = field->offset();
    _values.at_put_grow(offset, value, NULL);
  }

};


// MemoryBuffer is fairly simple model of the current state of memory.
// It partitions memory into several pieces.  The first piece is
// generic memory where little is known about the owner of the memory.
// This is conceptually represented by the tuple <O, F, V> which says
// that the field F of object O has value V.  This is flattened so
// that F is represented by the offset of the field and the parallel
// arrays _objects and _values are used for O and V.  Loads of O.F can
// simply use V.  Newly allocated objects are kept in a separate list
// along with a parallel array for each object which represents the
// current value of its fields.  Stores of the default value to fields
// which have never been stored to before are eliminated since they
// are redundant.  Once newly allocated objects are stored into
// another object or they are passed out of the current compile they
// are treated like generic memory.

class MemoryBuffer: public CompilationResourceObj {
 private:
  FieldBuffer                 _values;
  GrowableArray<Value>        _objects;
  GrowableArray<Value>        _newobjects;
  GrowableArray<FieldBuffer*> _fields;

 public:
  MemoryBuffer() {}

  StoreField* store(StoreField* st) {
    if (!EliminateFieldAccess) {
      return st;
    }

    Value object = st->obj();
    Value value = st->value();
    ciField* field = st->field();
    if (field->holder()->is_loaded()) {
      int offset = field->offset();
      int index = _newobjects.find(object);
      if (index != -1) {
        // newly allocated object with no other stores performed on this field
        FieldBuffer* buf = _fields.at(index);
        if (buf->at(field) == NULL && is_default_value(value)) {
#ifndef PRODUCT
          if (PrintIRDuringConstruction && Verbose) {
            tty->print_cr("Eliminated store for object %d:", index);
            st->print_line();
          }
#endif
          return NULL;
        } else {
          buf->at_put(field, value);
        }
      } else {
        _objects.at_put_grow(offset, object, NULL);
        _values.at_put(field, value);
      }

      store_value(value);
    } else {
      // if we held onto field names we could alias based on names but
      // we don't know what's being stored to so kill it all.
      kill();
    }
    return st;
  }


  // return true if this value correspond to the default value of a field.
  bool is_default_value(Value value) {
    Constant* con = value->as_Constant();
    if (con) {
      switch (con->type()->tag()) {
        case intTag:    return con->type()->as_IntConstant()->value() == 0;
        case longTag:   return con->type()->as_LongConstant()->value() == 0;
        case floatTag:  return jint_cast(con->type()->as_FloatConstant()->value()) == 0;
        case doubleTag: return jlong_cast(con->type()->as_DoubleConstant()->value()) == jlong_cast(0);
        case objectTag: return con->type() == objectNull;
        default:  ShouldNotReachHere();
      }
    }
    return false;
  }


  // return either the actual value of a load or the load itself
  Value load(LoadField* load) {
    if (!EliminateFieldAccess) {
      return load;
    }

    if (strict_fp_requires_explicit_rounding && load->type()->is_float_kind()) {
#ifdef IA32
      if (UseSSE < 2) {
        // can't skip load since value might get rounded as a side effect
        return load;
      }
#else
      Unimplemented();
#endif // IA32
    }

    ciField* field = load->field();
    Value object   = load->obj();
    if (field->holder()->is_loaded() && !field->is_volatile()) {
      int offset = field->offset();
      Value result = NULL;
      int index = _newobjects.find(object);
      if (index != -1) {
        result = _fields.at(index)->at(field);
      } else if (_objects.at_grow(offset, NULL) == object) {
        result = _values.at(field);
      }
      if (result != NULL) {
#ifndef PRODUCT
        if (PrintIRDuringConstruction && Verbose) {
          tty->print_cr("Eliminated load: ");
          load->print_line();
        }
#endif
        assert(result->type()->tag() == load->type()->tag(), "wrong types");
        return result;
      }
    }
    return load;
  }

  // Record this newly allocated object
  void new_instance(NewInstance* object) {
    int index = _newobjects.length();
    _newobjects.append(object);
    if (_fields.at_grow(index, NULL) == NULL) {
      _fields.at_put(index, new FieldBuffer());
    } else {
      _fields.at(index)->kill();
    }
  }

  void store_value(Value value) {
    int index = _newobjects.find(value);
    if (index != -1) {
      // stored a newly allocated object into another object.
      // Assume we've lost track of it as separate slice of memory.
      // We could do better by keeping track of whether individual
      // fields could alias each other.
      _newobjects.remove_at(index);
      // pull out the field info and store it at the end up the list
      // of field info list to be reused later.
      _fields.append(_fields.at(index));
      _fields.remove_at(index);
    }
  }

  void kill() {
    _newobjects.trunc_to(0);
    _objects.trunc_to(0);
    _values.kill();
  }
};


// Implementation of GraphBuilder's ScopeData

GraphBuilder::ScopeData::ScopeData(ScopeData* parent)
  : _parent(parent)
  , _bci2block(NULL)
  , _scope(NULL)
  , _has_handler(false)
  , _stream(NULL)
  , _work_list(NULL)
  , _caller_stack_size(-1)
  , _continuation(NULL)
  , _parsing_jsr(false)
  , _jsr_xhandlers(NULL)
  , _num_returns(0)
  , _cleanup_block(NULL)
  , _cleanup_return_prev(NULL)
  , _cleanup_state(NULL)
  , _ignore_return(false)
{
  if (parent != NULL) {
    _max_inline_size = (intx) ((float) NestedInliningSizeRatio * (float) parent->max_inline_size() / 100.0f);
  } else {
    _max_inline_size = C1MaxInlineSize;
  }
  if (_max_inline_size < C1MaxTrivialSize) {
    _max_inline_size = C1MaxTrivialSize;
  }
}


void GraphBuilder::kill_all() {
  if (UseLocalValueNumbering) {
    vmap()->kill_all();
  }
  _memory->kill();
}


BlockBegin* GraphBuilder::ScopeData::block_at(int bci) {
  if (parsing_jsr()) {
    // It is necessary to clone all blocks associated with a
    // subroutine, including those for exception handlers in the scope
    // of the method containing the jsr (because those exception
    // handlers may contain ret instructions in some cases).
    BlockBegin* block = bci2block()->at(bci);
    if (block != NULL && block == parent()->bci2block()->at(bci)) {
      BlockBegin* new_block = new BlockBegin(block->bci());
      if (PrintInitialBlockList) {
        tty->print_cr("CFG: cloned block %d (bci %d) as block %d for jsr",
                      block->block_id(), block->bci(), new_block->block_id());
      }
      // copy data from cloned blocked
      new_block->set_depth_first_number(block->depth_first_number());
      if (block->is_set(BlockBegin::parser_loop_header_flag)) new_block->set(BlockBegin::parser_loop_header_flag);
      // Preserve certain flags for assertion checking
      if (block->is_set(BlockBegin::subroutine_entry_flag)) new_block->set(BlockBegin::subroutine_entry_flag);
      if (block->is_set(BlockBegin::exception_entry_flag))  new_block->set(BlockBegin::exception_entry_flag);

      // copy was_visited_flag to allow early detection of bailouts
      // if a block that is used in a jsr has already been visited before,
      // it is shared between the normal control flow and a subroutine
      // BlockBegin::try_merge returns false when the flag is set, this leads
      // to a compilation bailout
      if (block->is_set(BlockBegin::was_visited_flag))  new_block->set(BlockBegin::was_visited_flag);

      bci2block()->at_put(bci, new_block);
      block = new_block;
    }
    return block;
  } else {
    return bci2block()->at(bci);
  }
}


XHandlers* GraphBuilder::ScopeData::xhandlers() const {
  if (_jsr_xhandlers == NULL) {
    assert(!parsing_jsr(), "");
    return scope()->xhandlers();
  }
  assert(parsing_jsr(), "");
  return _jsr_xhandlers;
}


void GraphBuilder::ScopeData::set_scope(IRScope* scope) {
  _scope = scope;
  bool parent_has_handler = false;
  if (parent() != NULL) {
    parent_has_handler = parent()->has_handler();
  }
  _has_handler = parent_has_handler || scope->xhandlers()->has_handlers();
}


void GraphBuilder::ScopeData::set_inline_cleanup_info(BlockBegin* block,
                                                      Instruction* return_prev,
                                                      ValueStack* return_state) {
  _cleanup_block       = block;
  _cleanup_return_prev = return_prev;
  _cleanup_state       = return_state;
}


void GraphBuilder::ScopeData::add_to_work_list(BlockBegin* block) {
  if (_work_list == NULL) {
    _work_list = new BlockList();
  }

  if (!block->is_set(BlockBegin::is_on_work_list_flag)) {
    // Do not start parsing the continuation block while in a
    // sub-scope
    if (parsing_jsr()) {
      if (block == jsr_continuation()) {
        return;
      }
    } else {
      if (block == continuation()) {
        return;
      }
    }
    block->set(BlockBegin::is_on_work_list_flag);
    _work_list->push(block);

    sort_top_into_worklist(_work_list, block);
  }
}


void GraphBuilder::sort_top_into_worklist(BlockList* worklist, BlockBegin* top) {
  assert(worklist->top() == top, "");
  // sort block descending into work list
  const int dfn = top->depth_first_number();
  assert(dfn != -1, "unknown depth first number");
  int i = worklist->length()-2;
  while (i >= 0) {
    BlockBegin* b = worklist->at(i);
    if (b->depth_first_number() < dfn) {
      worklist->at_put(i+1, b);
    } else {
      break;
    }
    i --;
  }
  if (i >= -1) worklist->at_put(i + 1, top);
}


BlockBegin* GraphBuilder::ScopeData::remove_from_work_list() {
  if (is_work_list_empty()) {
    return NULL;
  }
  return _work_list->pop();
}


bool GraphBuilder::ScopeData::is_work_list_empty() const {
  return (_work_list == NULL || _work_list->length() == 0);
}


void GraphBuilder::ScopeData::setup_jsr_xhandlers() {
  assert(parsing_jsr(), "");
  // clone all the exception handlers from the scope
  XHandlers* handlers = new XHandlers(scope()->xhandlers());
  const int n = handlers->length();
  for (int i = 0; i < n; i++) {
    // The XHandlers need to be adjusted to dispatch to the cloned
    // handler block instead of the default one but the synthetic
    // unlocker needs to be handled specially.  The synthetic unlocker
    // should be left alone since there can be only one and all code
    // should dispatch to the same one.
    XHandler* h = handlers->handler_at(i);
    assert(h->handler_bci() != SynchronizationEntryBCI, "must be real");
    h->set_entry_block(block_at(h->handler_bci()));
  }
  _jsr_xhandlers = handlers;
}


int GraphBuilder::ScopeData::num_returns() {
  if (parsing_jsr()) {
    return parent()->num_returns();
  }
  return _num_returns;
}


void GraphBuilder::ScopeData::incr_num_returns() {
  if (parsing_jsr()) {
    parent()->incr_num_returns();
  } else {
    ++_num_returns;
  }
}


// Implementation of GraphBuilder

#define INLINE_BAILOUT(msg)        { inline_bailout(msg); return false; }


void GraphBuilder::load_constant() {
  ciConstant con = stream()->get_constant();
  if (con.basic_type() == T_ILLEGAL) {
    // FIXME: an unresolved Dynamic constant can get here,
    // and that should not terminate the whole compilation.
    BAILOUT("could not resolve a constant");
  } else {
    ValueType* t = illegalType;
    ValueStack* patch_state = NULL;
    switch (con.basic_type()) {
      case T_BOOLEAN: t = new IntConstant     (con.as_boolean()); break;
      case T_BYTE   : t = new IntConstant     (con.as_byte   ()); break;
      case T_CHAR   : t = new IntConstant     (con.as_char   ()); break;
      case T_SHORT  : t = new IntConstant     (con.as_short  ()); break;
      case T_INT    : t = new IntConstant     (con.as_int    ()); break;
      case T_LONG   : t = new LongConstant    (con.as_long   ()); break;
      case T_FLOAT  : t = new FloatConstant   (con.as_float  ()); break;
      case T_DOUBLE : t = new DoubleConstant  (con.as_double ()); break;
      case T_ARRAY  : t = new ArrayConstant   (con.as_object ()->as_array   ()); break;
      case T_OBJECT :
       {
        ciObject* obj = con.as_object();
        if (!obj->is_loaded()
            || (PatchALot && obj->klass() != ciEnv::current()->String_klass())) {
          // A Class, MethodType, MethodHandle, or String.
          // Unloaded condy nodes show up as T_ILLEGAL, above.
          patch_state = copy_state_before();
          t = new ObjectConstant(obj);
        } else {
          // Might be a Class, MethodType, MethodHandle, or Dynamic constant
          // result, which might turn out to be an array.
          if (obj->is_null_object())
            t = objectNull;
          else if (obj->is_array())
            t = new ArrayConstant(obj->as_array());
          else
            t = new InstanceConstant(obj->as_instance());
        }
        break;
       }
      default       : ShouldNotReachHere();
    }
    Value x;
    if (patch_state != NULL) {
      x = new Constant(t, patch_state);
    } else {
      x = new Constant(t);
    }
    push(t, append(x));
  }
}


void GraphBuilder::load_local(ValueType* type, int index) {
  Value x = state()->local_at(index);
  assert(x != NULL && !x->type()->is_illegal(), "access of illegal local variable");
  push(type, x);
}


void GraphBuilder::store_local(ValueType* type, int index) {
  Value x = pop(type);
  store_local(state(), x, index);
}


void GraphBuilder::store_local(ValueStack* state, Value x, int index) {
  if (parsing_jsr()) {
    // We need to do additional tracking of the location of the return
    // address for jsrs since we don't handle arbitrary jsr/ret
    // constructs. Here we are figuring out in which circumstances we
    // need to bail out.
    if (x->type()->is_address()) {
      scope_data()->set_jsr_return_address_local(index);

      // Also check parent jsrs (if any) at this time to see whether
      // they are using this local. We don't handle skipping over a
      // ret.
      for (ScopeData* cur_scope_data = scope_data()->parent();
           cur_scope_data != NULL && cur_scope_data->parsing_jsr() && cur_scope_data->scope() == scope();
           cur_scope_data = cur_scope_data->parent()) {
        if (cur_scope_data->jsr_return_address_local() == index) {
          BAILOUT("subroutine overwrites return address from previous subroutine");
        }
      }
    } else if (index == scope_data()->jsr_return_address_local()) {
      scope_data()->set_jsr_return_address_local(-1);
    }
  }

  state->store_local(index, round_fp(x));
}


void GraphBuilder::load_indexed(BasicType type) {
  // In case of in block code motion in range check elimination
  ValueStack* state_before = copy_state_indexed_access();
  compilation()->set_has_access_indexed(true);
  Value index = ipop();
  Value array = apop();
  Value length = NULL;
  if (CSEArrayLength ||
      (array->as_Constant() != NULL) ||
      (array->as_AccessField() && array->as_AccessField()->field()->is_constant()) ||
      (array->as_NewArray() && array->as_NewArray()->length() && array->as_NewArray()->length()->type()->is_constant()) ||
      (array->as_NewMultiArray() && array->as_NewMultiArray()->dims()->at(0)->type()->is_constant())) {
    length = append(new ArrayLength(array, state_before));
  }
  push(as_ValueType(type), append(new LoadIndexed(array, index, length, type, state_before)));
}


void GraphBuilder::store_indexed(BasicType type) {
  // In case of in block code motion in range check elimination
  ValueStack* state_before = copy_state_indexed_access();
  compilation()->set_has_access_indexed(true);
  Value value = pop(as_ValueType(type));
  Value index = ipop();
  Value array = apop();
  Value length = NULL;
  if (CSEArrayLength ||
      (array->as_Constant() != NULL) ||
      (array->as_AccessField() && array->as_AccessField()->field()->is_constant()) ||
      (array->as_NewArray() && array->as_NewArray()->length() && array->as_NewArray()->length()->type()->is_constant()) ||
      (array->as_NewMultiArray() && array->as_NewMultiArray()->dims()->at(0)->type()->is_constant())) {
    length = append(new ArrayLength(array, state_before));
  }
  ciType* array_type = array->declared_type();
  bool check_boolean = false;
  if (array_type != NULL) {
    if (array_type->is_loaded() &&
      array_type->as_array_klass()->element_type()->basic_type() == T_BOOLEAN) {
      assert(type == T_BYTE, "boolean store uses bastore");
      Value mask = append(new Constant(new IntConstant(1)));
      value = append(new LogicOp(Bytecodes::_iand, value, mask));
    }
  } else if (type == T_BYTE) {
    check_boolean = true;
  }
  StoreIndexed* result = new StoreIndexed(array, index, length, type, value, state_before, check_boolean);
  append(result);
  _memory->store_value(value);

  if (type == T_OBJECT && is_profiling()) {
    // Note that we'd collect profile data in this method if we wanted it.
    compilation()->set_would_profile(true);

    if (profile_checkcasts()) {
      result->set_profiled_method(method());
      result->set_profiled_bci(bci());
      result->set_should_profile(true);
    }
  }
}


void GraphBuilder::stack_op(Bytecodes::Code code) {
  switch (code) {
    case Bytecodes::_pop:
      { state()->raw_pop();
      }
      break;
    case Bytecodes::_pop2:
      { state()->raw_pop();
        state()->raw_pop();
      }
      break;
    case Bytecodes::_dup:
      { Value w = state()->raw_pop();
        state()->raw_push(w);
        state()->raw_push(w);
      }
      break;
    case Bytecodes::_dup_x1:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        state()->raw_push(w1);
        state()->raw_push(w2);
        state()->raw_push(w1);
      }
      break;
    case Bytecodes::_dup_x2:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        Value w3 = state()->raw_pop();
        state()->raw_push(w1);
        state()->raw_push(w3);
        state()->raw_push(w2);
        state()->raw_push(w1);
      }
      break;
    case Bytecodes::_dup2:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        state()->raw_push(w2);
        state()->raw_push(w1);
        state()->raw_push(w2);
        state()->raw_push(w1);
      }
      break;
    case Bytecodes::_dup2_x1:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        Value w3 = state()->raw_pop();
        state()->raw_push(w2);
        state()->raw_push(w1);
        state()->raw_push(w3);
        state()->raw_push(w2);
        state()->raw_push(w1);
      }
      break;
    case Bytecodes::_dup2_x2:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        Value w3 = state()->raw_pop();
        Value w4 = state()->raw_pop();
        state()->raw_push(w2);
        state()->raw_push(w1);
        state()->raw_push(w4);
        state()->raw_push(w3);
        state()->raw_push(w2);
        state()->raw_push(w1);
      }
      break;
    case Bytecodes::_swap:
      { Value w1 = state()->raw_pop();
        Value w2 = state()->raw_pop();
        state()->raw_push(w1);
        state()->raw_push(w2);
      }
      break;
    default:
      ShouldNotReachHere();
      break;
  }
}


void GraphBuilder::arithmetic_op(ValueType* type, Bytecodes::Code code, ValueStack* state_before) {
  Value y = pop(type);
  Value x = pop(type);
  Value res = new ArithmeticOp(code, x, y, state_before);
  // Note: currently single-precision floating-point rounding on Intel is handled at the LIRGenerator level
  res = append(res);
  res = round_fp(res);
  push(type, res);
}


void GraphBuilder::negate_op(ValueType* type) {
  push(type, append(new NegateOp(pop(type))));
}


void GraphBuilder::shift_op(ValueType* type, Bytecodes::Code code) {
  Value s = ipop();
  Value x = pop(type);
  // try to simplify
  // Note: This code should go into the canonicalizer as soon as it can
  //       can handle canonicalized forms that contain more than one node.
  if (CanonicalizeNodes && code == Bytecodes::_iushr) {
    // pattern: x >>> s
    IntConstant* s1 = s->type()->as_IntConstant();
    if (s1 != NULL) {
      // pattern: x >>> s1, with s1 constant
      ShiftOp* l = x->as_ShiftOp();
      if (l != NULL && l->op() == Bytecodes::_ishl) {
        // pattern: (a << b) >>> s1
        IntConstant* s0 = l->y()->type()->as_IntConstant();
        if (s0 != NULL) {
          // pattern: (a << s0) >>> s1
          const int s0c = s0->value() & 0x1F; // only the low 5 bits are significant for shifts
          const int s1c = s1->value() & 0x1F; // only the low 5 bits are significant for shifts
          if (s0c == s1c) {
            if (s0c == 0) {
              // pattern: (a << 0) >>> 0 => simplify to: a
              ipush(l->x());
            } else {
              // pattern: (a << s0c) >>> s0c => simplify to: a & m, with m constant
              assert(0 < s0c && s0c < BitsPerInt, "adjust code below to handle corner cases");
              const int m = (1 << (BitsPerInt - s0c)) - 1;
              Value s = append(new Constant(new IntConstant(m)));
              ipush(append(new LogicOp(Bytecodes::_iand, l->x(), s)));
            }
            return;
          }
        }
      }
    }
  }
  // could not simplify
  push(type, append(new ShiftOp(code, x, s)));
}


void GraphBuilder::logic_op(ValueType* type, Bytecodes::Code code) {
  Value y = pop(type);
  Value x = pop(type);
  push(type, append(new LogicOp(code, x, y)));
}


void GraphBuilder::compare_op(ValueType* type, Bytecodes::Code code) {
  ValueStack* state_before = copy_state_before();
  Value y = pop(type);
  Value x = pop(type);
  ipush(append(new CompareOp(code, x, y, state_before)));
}


void GraphBuilder::convert(Bytecodes::Code op, BasicType from, BasicType to) {
  push(as_ValueType(to), append(new Convert(op, pop(as_ValueType(from)), as_ValueType(to))));
}


void GraphBuilder::increment() {
  int index = stream()->get_index();
  int delta = stream()->is_wide() ? (signed short)Bytes::get_Java_u2(stream()->cur_bcp() + 4) : (signed char)(stream()->cur_bcp()[2]);
  load_local(intType, index);
  ipush(append(new Constant(new IntConstant(delta))));
  arithmetic_op(intType, Bytecodes::_iadd);
  store_local(intType, index);
}


void GraphBuilder::_goto(int from_bci, int to_bci) {
  Goto *x = new Goto(block_at(to_bci), to_bci <= from_bci);
  if (is_profiling()) {
    compilation()->set_would_profile(true);
    x->set_profiled_bci(bci());
    if (profile_branches()) {
      x->set_profiled_method(method());
      x->set_should_profile(true);
    }
  }
  append(x);
}


void GraphBuilder::if_node(Value x, If::Condition cond, Value y, ValueStack* state_before) {
  BlockBegin* tsux = block_at(stream()->get_dest());
  BlockBegin* fsux = block_at(stream()->next_bci());
  bool is_bb = tsux->bci() < stream()->cur_bci() || fsux->bci() < stream()->cur_bci();
  // In case of loop invariant code motion or predicate insertion
  // before the body of a loop the state is needed
  Instruction *i = append(new If(x, cond, false, y, tsux, fsux, (is_bb || compilation()->is_optimistic()) ? state_before : NULL, is_bb));

  assert(i->as_Goto() == NULL ||
         (i->as_Goto()->sux_at(0) == tsux  && i->as_Goto()->is_safepoint() == tsux->bci() < stream()->cur_bci()) ||
         (i->as_Goto()->sux_at(0) == fsux  && i->as_Goto()->is_safepoint() == fsux->bci() < stream()->cur_bci()),
         "safepoint state of Goto returned by canonicalizer incorrect");

  if (is_profiling()) {
    If* if_node = i->as_If();
    if (if_node != NULL) {
      // Note that we'd collect profile data in this method if we wanted it.
      compilation()->set_would_profile(true);
      // At level 2 we need the proper bci to count backedges
      if_node->set_profiled_bci(bci());
      if (profile_branches()) {
        // Successors can be rotated by the canonicalizer, check for this case.
        if_node->set_profiled_method(method());
        if_node->set_should_profile(true);
        if (if_node->tsux() == fsux) {
          if_node->set_swapped(true);
        }
      }
      return;
    }

    // Check if this If was reduced to Goto.
    Goto *goto_node = i->as_Goto();
    if (goto_node != NULL) {
      compilation()->set_would_profile(true);
      goto_node->set_profiled_bci(bci());
      if (profile_branches()) {
        goto_node->set_profiled_method(method());
        goto_node->set_should_profile(true);
        // Find out which successor is used.
        if (goto_node->default_sux() == tsux) {
          goto_node->set_direction(Goto::taken);
        } else if (goto_node->default_sux() == fsux) {
          goto_node->set_direction(Goto::not_taken);
        } else {
          ShouldNotReachHere();
        }
      }
      return;
    }
  }
}


void GraphBuilder::if_zero(ValueType* type, If::Condition cond) {
  Value y = append(new Constant(intZero));
  ValueStack* state_before = copy_state_before();
  Value x = ipop();
  if_node(x, cond, y, state_before);
}


void GraphBuilder::if_null(ValueType* type, If::Condition cond) {
  Value y = append(new Constant(objectNull));
  ValueStack* state_before = copy_state_before();
  Value x = apop();
  if_node(x, cond, y, state_before);
}


void GraphBuilder::if_same(ValueType* type, If::Condition cond) {
  ValueStack* state_before = copy_state_before();
  Value y = pop(type);
  Value x = pop(type);
  if_node(x, cond, y, state_before);
}


void GraphBuilder::jsr(int dest) {
  // We only handle well-formed jsrs (those which are "block-structured").
  // If the bytecodes are strange (jumping out of a jsr block) then we
  // might end up trying to re-parse a block containing a jsr which
  // has already been activated. Watch for this case and bail out.
  for (ScopeData* cur_scope_data = scope_data();
       cur_scope_data != NULL && cur_scope_data->parsing_jsr() && cur_scope_data->scope() == scope();
       cur_scope_data = cur_scope_data->parent()) {
    if (cur_scope_data->jsr_entry_bci() == dest) {
      BAILOUT("too-complicated jsr/ret structure");
    }
  }

  push(addressType, append(new Constant(new AddressConstant(next_bci()))));
  if (!try_inline_jsr(dest)) {
    return; // bailed out while parsing and inlining subroutine
  }
}


void GraphBuilder::ret(int local_index) {
  if (!parsing_jsr()) BAILOUT("ret encountered while not parsing subroutine");

  if (local_index != scope_data()->jsr_return_address_local()) {
    BAILOUT("can not handle complicated jsr/ret constructs");
  }

  // Rets simply become (NON-SAFEPOINT) gotos to the jsr continuation
  append(new Goto(scope_data()->jsr_continuation(), false));
}


void GraphBuilder::table_switch() {
  Bytecode_tableswitch sw(stream());
  const int l = sw.length();
  if (CanonicalizeNodes && l == 1 && compilation()->env()->comp_level() != CompLevel_full_profile) {
    // total of 2 successors => use If instead of switch
    // Note: This code should go into the canonicalizer as soon as it can
    //       can handle canonicalized forms that contain more than one node.
    Value key = append(new Constant(new IntConstant(sw.low_key())));
    BlockBegin* tsux = block_at(bci() + sw.dest_offset_at(0));
    BlockBegin* fsux = block_at(bci() + sw.default_offset());
    bool is_bb = tsux->bci() < bci() || fsux->bci() < bci();
    // In case of loop invariant code motion or predicate insertion
    // before the body of a loop the state is needed
    ValueStack* state_before = copy_state_if_bb(is_bb);
    append(new If(ipop(), If::eql, true, key, tsux, fsux, state_before, is_bb));
  } else {
    // collect successors
    BlockList* sux = new BlockList(l + 1, NULL);
    int i;
    bool has_bb = false;
    for (i = 0; i < l; i++) {
      sux->at_put(i, block_at(bci() + sw.dest_offset_at(i)));
      if (sw.dest_offset_at(i) < 0) has_bb = true;
    }
    // add default successor
    if (sw.default_offset() < 0) has_bb = true;
    sux->at_put(i, block_at(bci() + sw.default_offset()));
    // In case of loop invariant code motion or predicate insertion
    // before the body of a loop the state is needed
    ValueStack* state_before = copy_state_if_bb(has_bb);
    Instruction* res = append(new TableSwitch(ipop(), sux, sw.low_key(), state_before, has_bb));
#ifdef ASSERT
    if (res->as_Goto()) {
      for (i = 0; i < l; i++) {
        if (sux->at(i) == res->as_Goto()->sux_at(0)) {
          assert(res->as_Goto()->is_safepoint() == sw.dest_offset_at(i) < 0, "safepoint state of Goto returned by canonicalizer incorrect");
        }
      }
    }
#endif
  }
}


void GraphBuilder::lookup_switch() {
  Bytecode_lookupswitch sw(stream());
  const int l = sw.number_of_pairs();
  if (CanonicalizeNodes && l == 1 && compilation()->env()->comp_level() != CompLevel_full_profile) {
    // total of 2 successors => use If instead of switch
    // Note: This code should go into the canonicalizer as soon as it can
    //       can handle canonicalized forms that contain more than one node.
    // simplify to If
    LookupswitchPair pair = sw.pair_at(0);
    Value key = append(new Constant(new IntConstant(pair.match())));
    BlockBegin* tsux = block_at(bci() + pair.offset());
    BlockBegin* fsux = block_at(bci() + sw.default_offset());
    bool is_bb = tsux->bci() < bci() || fsux->bci() < bci();
    // In case of loop invariant code motion or predicate insertion
    // before the body of a loop the state is needed
    ValueStack* state_before = copy_state_if_bb(is_bb);;
    append(new If(ipop(), If::eql, true, key, tsux, fsux, state_before, is_bb));
  } else {
    // collect successors & keys
    BlockList* sux = new BlockList(l + 1, NULL);
    intArray* keys = new intArray(l, l, 0);
    int i;
    bool has_bb = false;
    for (i = 0; i < l; i++) {
      LookupswitchPair pair = sw.pair_at(i);
      if (pair.offset() < 0) has_bb = true;
      sux->at_put(i, block_at(bci() + pair.offset()));
      keys->at_put(i, pair.match());
    }
    // add default successor
    if (sw.default_offset() < 0) has_bb = true;
    sux->at_put(i, block_at(bci() + sw.default_offset()));
    // In case of loop invariant code motion or predicate insertion
    // before the body of a loop the state is needed
    ValueStack* state_before = copy_state_if_bb(has_bb);
    Instruction* res = append(new LookupSwitch(ipop(), sux, keys, state_before, has_bb));
#ifdef ASSERT
    if (res->as_Goto()) {
      for (i = 0; i < l; i++) {
        if (sux->at(i) == res->as_Goto()->sux_at(0)) {
          assert(res->as_Goto()->is_safepoint() == sw.pair_at(i).offset() < 0, "safepoint state of Goto returned by canonicalizer incorrect");
        }
      }
    }
#endif
  }
}

void GraphBuilder::call_register_finalizer() {
  // If the receiver requires finalization then emit code to perform
  // the registration on return.

  // Gather some type information about the receiver
  Value receiver = state()->local_at(0);
  assert(receiver != NULL, "must have a receiver");
  ciType* declared_type = receiver->declared_type();
  ciType* exact_type = receiver->exact_type();
  if (exact_type == NULL &&
      receiver->as_Local() &&
      receiver->as_Local()->java_index() == 0) {
    ciInstanceKlass* ik = compilation()->method()->holder();
    if (ik->is_final()) {
      exact_type = ik;
    } else if (UseCHA && !(ik->has_subklass() || ik->is_interface())) {
      // test class is leaf class
      compilation()->dependency_recorder()->assert_leaf_type(ik);
      exact_type = ik;
    } else {
      declared_type = ik;
    }
  }

  // see if we know statically that registration isn't required
  bool needs_check = true;
  if (exact_type != NULL) {
    needs_check = exact_type->as_instance_klass()->has_finalizer();
  } else if (declared_type != NULL) {
    ciInstanceKlass* ik = declared_type->as_instance_klass();
    if (!Dependencies::has_finalizable_subclass(ik)) {
      compilation()->dependency_recorder()->assert_has_no_finalizable_subclasses(ik);
      needs_check = false;
    }
  }

  if (needs_check) {
    // Perform the registration of finalizable objects.
    ValueStack* state_before = copy_state_for_exception();
    load_local(objectType, 0);
    append_split(new Intrinsic(voidType, vmIntrinsics::_Object_init,
                               state()->pop_arguments(1),
                               true, state_before, true));
  }
}


void GraphBuilder::method_return(Value x, bool ignore_return) {
  if (RegisterFinalizersAtInit &&
      method()->intrinsic_id() == vmIntrinsics::_Object_init) {
    call_register_finalizer();
  }

  // The conditions for a memory barrier are described in Parse::do_exits().
  bool need_mem_bar = false;
  if (method()->name() == ciSymbols::object_initializer_name() &&
       (scope()->wrote_final() ||
         (AlwaysSafeConstructors && scope()->wrote_fields()) ||
         (support_IRIW_for_not_multiple_copy_atomic_cpu && scope()->wrote_volatile()))) {
    need_mem_bar = true;
  }

  BasicType bt = method()->return_type()->basic_type();
  switch (bt) {
    case T_BYTE:
    {
      Value shift = append(new Constant(new IntConstant(24)));
      x = append(new ShiftOp(Bytecodes::_ishl, x, shift));
      x = append(new ShiftOp(Bytecodes::_ishr, x, shift));
      break;
    }
    case T_SHORT:
    {
      Value shift = append(new Constant(new IntConstant(16)));
      x = append(new ShiftOp(Bytecodes::_ishl, x, shift));
      x = append(new ShiftOp(Bytecodes::_ishr, x, shift));
      break;
    }
    case T_CHAR:
    {
      Value mask = append(new Constant(new IntConstant(0xFFFF)));
      x = append(new LogicOp(Bytecodes::_iand, x, mask));
      break;
    }
    case T_BOOLEAN:
    {
      Value mask = append(new Constant(new IntConstant(1)));
      x = append(new LogicOp(Bytecodes::_iand, x, mask));
      break;
    }
    default:
      break;
  }

  // Check to see whether we are inlining. If so, Return
  // instructions become Gotos to the continuation point.
  if (continuation() != NULL) {

    int invoke_bci = state()->caller_state()->bci();

    if (x != NULL  && !ignore_return) {
      ciMethod* caller = state()->scope()->caller()->method();
      Bytecodes::Code invoke_raw_bc = caller->raw_code_at_bci(invoke_bci);
      if (invoke_raw_bc == Bytecodes::_invokehandle || invoke_raw_bc == Bytecodes::_invokedynamic) {
        ciType* declared_ret_type = caller->get_declared_signature_at_bci(invoke_bci)->return_type();
        if (declared_ret_type->is_klass() && x->exact_type() == NULL &&
            x->declared_type() != declared_ret_type && declared_ret_type != compilation()->env()->Object_klass()) {
          x = append(new TypeCast(declared_ret_type->as_klass(), x, copy_state_before()));
        }
      }
    }

    assert(!method()->is_synchronized() || InlineSynchronizedMethods, "can not inline synchronized methods yet");

    if (compilation()->env()->dtrace_method_probes()) {
      // Report exit from inline methods
      Values* args = new Values(1);
      args->push(append(new Constant(new MethodConstant(method()))));
      append(new RuntimeCall(voidType, "dtrace_method_exit", CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit), args));
    }

    // If the inlined method is synchronized, the monitor must be
    // released before we jump to the continuation block.
    if (method()->is_synchronized()) {
      assert(state()->locks_size() == 1, "receiver must be locked here");
      monitorexit(state()->lock_at(0), SynchronizationEntryBCI);
    }

    if (need_mem_bar) {
      append(new MemBar(lir_membar_storestore));
    }

    // State at end of inlined method is the state of the caller
    // without the method parameters on stack, including the
    // return value, if any, of the inlined method on operand stack.
    set_state(state()->caller_state()->copy_for_parsing());
    if (x != NULL) {
      if (!ignore_return) {
        state()->push(x->type(), x);
      }
      if (profile_return() && x->type()->is_object_kind()) {
        ciMethod* caller = state()->scope()->method();
        profile_return_type(x, method(), caller, invoke_bci);
      }
    }
    Goto* goto_callee = new Goto(continuation(), false);

    // See whether this is the first return; if so, store off some
    // of the state for later examination
    if (num_returns() == 0) {
      set_inline_cleanup_info();
    }

    // The current bci() is in the wrong scope, so use the bci() of
    // the continuation point.
    append_with_bci(goto_callee, scope_data()->continuation()->bci());
    incr_num_returns();
    return;
  }

  state()->truncate_stack(0);
  if (method()->is_synchronized()) {
    // perform the unlocking before exiting the method
    Value receiver;
    if (!method()->is_static()) {
      receiver = _initial_state->local_at(0);
    } else {
      receiver = append(new Constant(new ClassConstant(method()->holder())));
    }
    append_split(new MonitorExit(receiver, state()->unlock()));
  }

  if (need_mem_bar) {
      append(new MemBar(lir_membar_storestore));
  }

  assert(!ignore_return, "Ignoring return value works only for inlining");
  append(new Return(x));
}

Value GraphBuilder::make_constant(ciConstant field_value, ciField* field) {
  if (!field_value.is_valid())  return NULL;

  BasicType field_type = field_value.basic_type();
  ValueType* value = as_ValueType(field_value);

  // Attach dimension info to stable arrays.
  if (FoldStableValues &&
      field->is_stable() && field_type == T_ARRAY && !field_value.is_null_or_zero()) {
    ciArray* array = field_value.as_object()->as_array();
    jint dimension = field->type()->as_array_klass()->dimension();
    value = new StableArrayConstant(array, dimension);
  }

  switch (field_type) {
    case T_ARRAY:
    case T_OBJECT:
      if (field_value.as_object()->should_be_constant()) {
        return new Constant(value);
      }
      return NULL; // Not a constant.
    default:
      return new Constant(value);
  }
}

void GraphBuilder::access_field(Bytecodes::Code code) {
  bool will_link;
  ciField* field = stream()->get_field(will_link);
  ciInstanceKlass* holder = field->holder();
  BasicType field_type = field->type()->basic_type();
  ValueType* type = as_ValueType(field_type);
  // call will_link again to determine if the field is valid.
  const bool needs_patching = !holder->is_loaded() ||
                              !field->will_link(method(), code) ||
                              PatchALot;

  ValueStack* state_before = NULL;
  if (!holder->is_initialized() || needs_patching) {
    // save state before instruction for debug info when
    // deoptimization happens during patching
    state_before = copy_state_before();
  }

  Value obj = NULL;
  if (code == Bytecodes::_getstatic || code == Bytecodes::_putstatic) {
    if (state_before != NULL) {
      // build a patching constant
      obj = new Constant(new InstanceConstant(holder->java_mirror()), state_before);
    } else {
      obj = new Constant(new InstanceConstant(holder->java_mirror()));
    }
  }

  if (field->is_final() && (code == Bytecodes::_putfield)) {
    scope()->set_wrote_final();
  }

  if (code == Bytecodes::_putfield) {
    scope()->set_wrote_fields();
    if (field->is_volatile()) {
      scope()->set_wrote_volatile();
    }
  }

  const int offset = !needs_patching ? field->offset() : -1;
  switch (code) {
    case Bytecodes::_getstatic: {
      // check for compile-time constants, i.e., initialized static final fields
      Value constant = NULL;
      if (field->is_static_constant() && !PatchALot) {
        ciConstant field_value = field->constant_value();
        assert(!field->is_stable() || !field_value.is_null_or_zero(),
               "stable static w/ default value shouldn't be a constant");
        constant = make_constant(field_value, field);
      }
      if (constant != NULL) {
        push(type, append(constant));
      } else {
        if (state_before == NULL) {
          state_before = copy_state_for_exception();
        }
        push(type, append(new LoadField(append(obj), offset, field, true,
                                        state_before, needs_patching)));
      }
      break;
    }
    case Bytecodes::_putstatic: {
      Value val = pop(type);
      if (state_before == NULL) {
        state_before = copy_state_for_exception();
      }
      if (field->type()->basic_type() == T_BOOLEAN) {
        Value mask = append(new Constant(new IntConstant(1)));
        val = append(new LogicOp(Bytecodes::_iand, val, mask));
      }
      append(new StoreField(append(obj), offset, field, val, true, state_before, needs_patching));
      break;
    }
    case Bytecodes::_getfield: {
      // Check for compile-time constants, i.e., trusted final non-static fields.
      Value constant = NULL;
      obj = apop();
      ObjectType* obj_type = obj->type()->as_ObjectType();
      if (field->is_constant() && obj_type->is_constant() && !PatchALot) {
        ciObject* const_oop = obj_type->constant_value();
        if (!const_oop->is_null_object() && const_oop->is_loaded()) {
          ciConstant field_value = field->constant_value_of(const_oop);
          if (field_value.is_valid()) {
            constant = make_constant(field_value, field);
            // For CallSite objects add a dependency for invalidation of the optimization.
            if (field->is_call_site_target()) {
              ciCallSite* call_site = const_oop->as_call_site();
              if (!call_site->is_fully_initialized_constant_call_site()) {
                ciMethodHandle* target = field_value.as_object()->as_method_handle();
                dependency_recorder()->assert_call_site_target_value(call_site, target);
              }
            }
          }
        }
      }
      if (constant != NULL) {
        push(type, append(constant));
      } else {
        if (state_before == NULL) {
          state_before = copy_state_for_exception();
        }
        LoadField* load = new LoadField(obj, offset, field, false, state_before, needs_patching);
        Value replacement = !needs_patching ? _memory->load(load) : load;
        if (replacement != load) {
          assert(replacement->is_linked() || !replacement->can_be_linked(), "should already by linked");
          // Writing an (integer) value to a boolean, byte, char or short field includes an implicit narrowing
          // conversion. Emit an explicit conversion here to get the correct field value after the write.
          BasicType bt = field->type()->basic_type();
          switch (bt) {
          case T_BOOLEAN:
          case T_BYTE:
            replacement = append(new Convert(Bytecodes::_i2b, replacement, as_ValueType(bt)));
            break;
          case T_CHAR:
            replacement = append(new Convert(Bytecodes::_i2c, replacement, as_ValueType(bt)));
            break;
          case T_SHORT:
            replacement = append(new Convert(Bytecodes::_i2s, replacement, as_ValueType(bt)));
            break;
          default:
            break;
          }
          push(type, replacement);
        } else {
          push(type, append(load));
        }
      }
      break;
    }
    case Bytecodes::_putfield: {
      Value val = pop(type);
      obj = apop();
      if (state_before == NULL) {
        state_before = copy_state_for_exception();
      }
      if (field->type()->basic_type() == T_BOOLEAN) {
        Value mask = append(new Constant(new IntConstant(1)));
        val = append(new LogicOp(Bytecodes::_iand, val, mask));
      }
      StoreField* store = new StoreField(obj, offset, field, val, false, state_before, needs_patching);
      if (!needs_patching) store = _memory->store(store);
      if (store != NULL) {
        append(store);
      }
      break;
    }
    default:
      ShouldNotReachHere();
      break;
  }
}


Dependencies* GraphBuilder::dependency_recorder() const {
  assert(DeoptC1, "need debug information");
  return compilation()->dependency_recorder();
}

// How many arguments do we want to profile?
Values* GraphBuilder::args_list_for_profiling(ciMethod* target, int& start, bool may_have_receiver) {
  int n = 0;
  bool has_receiver = may_have_receiver && Bytecodes::has_receiver(method()->java_code_at_bci(bci()));
  start = has_receiver ? 1 : 0;
  if (profile_arguments()) {
    ciProfileData* data = method()->method_data()->bci_to_data(bci());
    if (data != NULL && (data->is_CallTypeData() || data->is_VirtualCallTypeData())) {
      n = data->is_CallTypeData() ? data->as_CallTypeData()->number_of_arguments() : data->as_VirtualCallTypeData()->number_of_arguments();
    }
  }
  // If we are inlining then we need to collect arguments to profile parameters for the target
  if (profile_parameters() && target != NULL) {
    if (target->method_data() != NULL && target->method_data()->parameters_type_data() != NULL) {
      // The receiver is profiled on method entry so it's included in
      // the number of parameters but here we're only interested in
      // actual arguments.
      n = MAX2(n, target->method_data()->parameters_type_data()->number_of_parameters() - start);
    }
  }
  if (n > 0) {
    return new Values(n);
  }
  return NULL;
}

void GraphBuilder::check_args_for_profiling(Values* obj_args, int expected) {
#ifdef ASSERT
  bool ignored_will_link;
  ciSignature* declared_signature = NULL;
  ciMethod* real_target = method()->get_method_at_bci(bci(), ignored_will_link, &declared_signature);
  assert(expected == obj_args->max_length() || real_target->is_method_handle_intrinsic(), "missed on arg?");
#endif
}

// Collect arguments that we want to profile in a list
Values* GraphBuilder::collect_args_for_profiling(Values* args, ciMethod* target, bool may_have_receiver) {
  int start = 0;
  Values* obj_args = args_list_for_profiling(target, start, may_have_receiver);
  if (obj_args == NULL) {
    return NULL;
  }
  int s = obj_args->max_length();
  // if called through method handle invoke, some arguments may have been popped
  for (int i = start, j = 0; j < s && i < args->length(); i++) {
    if (args->at(i)->type()->is_object_kind()) {
      obj_args->push(args->at(i));
      j++;
    }
  }
  check_args_for_profiling(obj_args, s);
  return obj_args;
}


void GraphBuilder::invoke(Bytecodes::Code code) {
  bool will_link;
  ciSignature* declared_signature = NULL;
  ciMethod*             target = stream()->get_method(will_link, &declared_signature);
  ciKlass*              holder = stream()->get_declared_method_holder();
  const Bytecodes::Code bc_raw = stream()->cur_bc_raw();
  assert(declared_signature != NULL, "cannot be null");
  assert(will_link == target->is_loaded(), "");

  ciInstanceKlass* klass = target->holder();
  assert(!target->is_loaded() || klass->is_loaded(), "loaded target must imply loaded klass");

  // check if CHA possible: if so, change the code to invoke_special
  ciInstanceKlass* calling_klass = method()->holder();
  ciInstanceKlass* callee_holder = ciEnv::get_instance_klass_for_declared_method_holder(holder);
  ciInstanceKlass* actual_recv = callee_holder;

  CompileLog* log = compilation()->log();
  if (log != NULL)
      log->elem("call method='%d' instr='%s'",
                log->identify(target),
                Bytecodes::name(code));

  // invoke-special-super
  if (bc_raw == Bytecodes::_invokespecial && !target->is_object_initializer()) {
    ciInstanceKlass* sender_klass = calling_klass;
    if (sender_klass->is_interface()) {
      int index = state()->stack_size() - (target->arg_size_no_receiver() + 1);
      Value receiver = state()->stack_at(index);
      CheckCast* c = new CheckCast(sender_klass, receiver, copy_state_before());
      c->set_invokespecial_receiver_check();
      state()->stack_at_put(index, append_split(c));
    }
  }

  // Some methods are obviously bindable without any type checks so
  // convert them directly to an invokespecial or invokestatic.
  if (target->is_loaded() && !target->is_abstract() && target->can_be_statically_bound()) {
    switch (bc_raw) {
    case Bytecodes::_invokevirtual:
      code = Bytecodes::_invokespecial;
      break;
    case Bytecodes::_invokehandle:
      code = target->is_static() ? Bytecodes::_invokestatic : Bytecodes::_invokespecial;
      break;
    default:
      break;
    }
  } else {
    if (bc_raw == Bytecodes::_invokehandle) {
      assert(!will_link, "should come here only for unlinked call");
      code = Bytecodes::_invokespecial;
    }
  }

  // Push appendix argument (MethodType, CallSite, etc.), if one.
  bool patch_for_appendix = false;
  int patching_appendix_arg = 0;
  if (Bytecodes::has_optional_appendix(bc_raw) && (!will_link || PatchALot)) {
    Value arg = append(new Constant(new ObjectConstant(compilation()->env()->unloaded_ciinstance()), copy_state_before()));
    apush(arg);
    patch_for_appendix = true;
    patching_appendix_arg = (will_link && stream()->has_appendix()) ? 0 : 1;
  } else if (stream()->has_appendix()) {
    ciObject* appendix = stream()->get_appendix();
    Value arg = append(new Constant(new ObjectConstant(appendix)));
    apush(arg);
  }

  ciMethod* cha_monomorphic_target = NULL;
  ciMethod* exact_target = NULL;
  Value better_receiver = NULL;
  if (UseCHA && DeoptC1 && target->is_loaded() &&
      !(// %%% FIXME: Are both of these relevant?
        target->is_method_handle_intrinsic() ||
        target->is_compiled_lambda_form()) &&
      !patch_for_appendix) {
    Value receiver = NULL;
    ciInstanceKlass* receiver_klass = NULL;
    bool type_is_exact = false;
    // try to find a precise receiver type
    if (will_link && !target->is_static()) {
      int index = state()->stack_size() - (target->arg_size_no_receiver() + 1);
      receiver = state()->stack_at(index);
      ciType* type = receiver->exact_type();
      if (type != NULL && type->is_loaded() &&
          type->is_instance_klass() && !type->as_instance_klass()->is_interface()) {
        receiver_klass = (ciInstanceKlass*) type;
        type_is_exact = true;
      }
      if (type == NULL) {
        type = receiver->declared_type();
        if (type != NULL && type->is_loaded() &&
            type->is_instance_klass() && !type->as_instance_klass()->is_interface()) {
          receiver_klass = (ciInstanceKlass*) type;
          if (receiver_klass->is_leaf_type() && !receiver_klass->is_final()) {
            // Insert a dependency on this type since
            // find_monomorphic_target may assume it's already done.
            dependency_recorder()->assert_leaf_type(receiver_klass);
            type_is_exact = true;
          }
        }
      }
    }
    if (receiver_klass != NULL && type_is_exact &&
        receiver_klass->is_loaded() && code != Bytecodes::_invokespecial) {
      // If we have the exact receiver type we can bind directly to
      // the method to call.
      exact_target = target->resolve_invoke(calling_klass, receiver_klass);
      if (exact_target != NULL) {
        target = exact_target;
        code = Bytecodes::_invokespecial;
      }
    }
    if (receiver_klass != NULL &&
        receiver_klass->is_subtype_of(actual_recv) &&
        actual_recv->is_initialized()) {
      actual_recv = receiver_klass;
    }

    if ((code == Bytecodes::_invokevirtual && callee_holder->is_initialized()) ||
        (code == Bytecodes::_invokeinterface && callee_holder->is_initialized() && !actual_recv->is_interface())) {
      // Use CHA on the receiver to select a more precise method.
      cha_monomorphic_target = target->find_monomorphic_target(calling_klass, callee_holder, actual_recv);
    } else if (code == Bytecodes::_invokeinterface && callee_holder->is_loaded() && receiver != NULL) {
      assert(callee_holder->is_interface(), "invokeinterface to non interface?");
      // If there is only one implementor of this interface then we
      // may be able bind this invoke directly to the implementing
      // klass but we need both a dependence on the single interface
      // and on the method we bind to.  Additionally since all we know
      // about the receiver type is the it's supposed to implement the
      // interface we have to insert a check that it's the class we
      // expect.  Interface types are not checked by the verifier so
      // they are roughly equivalent to Object.
      // The number of implementors for declared_interface is less or
      // equal to the number of implementors for target->holder() so
      // if number of implementors of target->holder() == 1 then
      // number of implementors for decl_interface is 0 or 1. If
      // it's 0 then no class implements decl_interface and there's
      // no point in inlining.
      ciInstanceKlass* declared_interface = callee_holder;
      ciInstanceKlass* singleton = declared_interface->unique_implementor();
      if (singleton != NULL) {
        assert(singleton != declared_interface, "not a unique implementor");
        cha_monomorphic_target = target->find_monomorphic_target(calling_klass, declared_interface, singleton);
        if (cha_monomorphic_target != NULL) {
          if (cha_monomorphic_target->holder() != compilation()->env()->Object_klass()) {
            // If CHA is able to bind this invoke then update the class
            // to match that class, otherwise klass will refer to the
            // interface.
            klass = cha_monomorphic_target->holder();
            actual_recv = declared_interface;

            // insert a check it's really the expected class.
            CheckCast* c = new CheckCast(klass, receiver, copy_state_for_exception());
            c->set_incompatible_class_change_check();
            c->set_direct_compare(klass->is_final());
            // pass the result of the checkcast so that the compiler has
            // more accurate type info in the inlinee
            better_receiver = append_split(c);
          } else {
            cha_monomorphic_target = NULL; // subtype check against Object is useless
          }
        }
      }
    }
  }

  if (cha_monomorphic_target != NULL) {
    assert(!target->can_be_statically_bound() || target == cha_monomorphic_target, "");
    assert(!cha_monomorphic_target->is_abstract(), "");
    if (!cha_monomorphic_target->can_be_statically_bound(actual_recv)) {
      // If we inlined because CHA revealed only a single target method,
      // then we are dependent on that target method not getting overridden
      // by dynamic class loading.  Be sure to test the "static" receiver
      // dest_method here, as opposed to the actual receiver, which may
      // falsely lead us to believe that the receiver is final or private.
      dependency_recorder()->assert_unique_concrete_method(actual_recv, cha_monomorphic_target, callee_holder, target);
    }
    code = Bytecodes::_invokespecial;
  }

  // check if we could do inlining
  if (!PatchALot && Inline && target->is_loaded() && callee_holder->is_linked() && !patch_for_appendix) {
    // callee is known => check if we have static binding
    if ((code == Bytecodes::_invokestatic && callee_holder->is_initialized()) || // invokestatic involves an initialization barrier on resolved klass
        code == Bytecodes::_invokespecial ||
        (code == Bytecodes::_invokevirtual && target->is_final_method()) ||
        code == Bytecodes::_invokedynamic) {
      // static binding => check if callee is ok
      ciMethod* inline_target = (cha_monomorphic_target != NULL) ? cha_monomorphic_target : target;
      bool holder_known = (cha_monomorphic_target != NULL) || (exact_target != NULL);
      bool success = try_inline(inline_target, holder_known, false /* ignore_return */, code, better_receiver);

      CHECK_BAILOUT();
      clear_inline_bailout();

      if (success) {
        // Register dependence if JVMTI has either breakpoint
        // setting or hotswapping of methods capabilities since they may
        // cause deoptimization.
        if (compilation()->env()->jvmti_can_hotswap_or_post_breakpoint()) {
          dependency_recorder()->assert_evol_method(inline_target);
        }
        return;
      }
    } else {
      print_inlining(target, "no static binding", /*success*/ false);
    }
  } else {
    print_inlining(target, "not inlineable", /*success*/ false);
  }

  // If we attempted an inline which did not succeed because of a
  // bailout during construction of the callee graph, the entire
  // compilation has to be aborted. This is fairly rare and currently
  // seems to only occur for jasm-generated classes which contain
  // jsr/ret pairs which are not associated with finally clauses and
  // do not have exception handlers in the containing method, and are
  // therefore not caught early enough to abort the inlining without
  // corrupting the graph. (We currently bail out with a non-empty
  // stack at a ret in these situations.)
  CHECK_BAILOUT();

  // inlining not successful => standard invoke
  ValueType* result_type = as_ValueType(declared_signature->return_type());
  ValueStack* state_before = copy_state_exhandling();

  // The bytecode (code) might change in this method so we are checking this very late.
  const bool has_receiver =
    code == Bytecodes::_invokespecial   ||
    code == Bytecodes::_invokevirtual   ||
    code == Bytecodes::_invokeinterface;
  Values* args = state()->pop_arguments(target->arg_size_no_receiver() + patching_appendix_arg);
  Value recv = has_receiver ? apop() : NULL;

  // A null check is required here (when there is a receiver) for any of the following cases
  // - invokespecial, always need a null check.
  // - invokevirtual, when the target is final and loaded. Calls to final targets will become optimized
  //   and require null checking. If the target is loaded a null check is emitted here.
  //   If the target isn't loaded the null check must happen after the call resolution. We achieve that
  //   by using the target methods unverified entry point (see CompiledIC::compute_monomorphic_entry).
  //   (The JVM specification requires that LinkageError must be thrown before a NPE. An unloaded target may
  //   potentially fail, and can't have the null check before the resolution.)
  // - A call that will be profiled. (But we can't add a null check when the target is unloaded, by the same
  //   reason as above, so calls with a receiver to unloaded targets can't be profiled.)
  //
  // Normal invokevirtual will perform the null check during lookup

  bool need_null_check = (code == Bytecodes::_invokespecial) ||
      (target->is_loaded() && (target->is_final_method() || (is_profiling() && profile_calls())));

  if (need_null_check) {
    if (recv != NULL) {
      null_check(recv);
    }

    if (is_profiling()) {
      // Note that we'd collect profile data in this method if we wanted it.
      compilation()->set_would_profile(true);

      if (profile_calls()) {
        assert(cha_monomorphic_target == NULL || exact_target == NULL, "both can not be set");
        ciKlass* target_klass = NULL;
        if (cha_monomorphic_target != NULL) {
          target_klass = cha_monomorphic_target->holder();
        } else if (exact_target != NULL) {
          target_klass = exact_target->holder();
        }
        profile_call(target, recv, target_klass, collect_args_for_profiling(args, NULL, false), false);
      }
    }
  }

  Invoke* result = new Invoke(code, result_type, recv, args, target, state_before);
  // push result
  append_split(result);

  if (result_type != voidType) {
    push(result_type, round_fp(result));
  }
  if (profile_return() && result_type->is_object_kind()) {
    profile_return_type(result, target);
  }
}


void GraphBuilder::new_instance(int klass_index) {
  ValueStack* state_before = copy_state_exhandling();
  bool will_link;
  ciKlass* klass = stream()->get_klass(will_link);
  assert(klass->is_instance_klass(), "must be an instance klass");
  NewInstance* new_instance = new NewInstance(klass->as_instance_klass(), state_before, stream()->is_unresolved_klass());
  _memory->new_instance(new_instance);
  apush(append_split(new_instance));
}


void GraphBuilder::new_type_array() {
  ValueStack* state_before = copy_state_exhandling();
  apush(append_split(new NewTypeArray(ipop(), (BasicType)stream()->get_index(), state_before)));
}


void GraphBuilder::new_object_array() {
  bool will_link;
  ciKlass* klass = stream()->get_klass(will_link);
  ValueStack* state_before = !klass->is_loaded() || PatchALot ? copy_state_before() : copy_state_exhandling();
  NewArray* n = new NewObjectArray(klass, ipop(), state_before);
  apush(append_split(n));
}


bool GraphBuilder::direct_compare(ciKlass* k) {
  if (k->is_loaded() && k->is_instance_klass() && !UseSlowPath) {
    ciInstanceKlass* ik = k->as_instance_klass();
    if (ik->is_final()) {
      return true;
    } else {
      if (DeoptC1 && UseCHA && !(ik->has_subklass() || ik->is_interface())) {
        // test class is leaf class
        dependency_recorder()->assert_leaf_type(ik);
        return true;
      }
    }
  }
  return false;
}


void GraphBuilder::check_cast(int klass_index) {
  bool will_link;
  ciKlass* klass = stream()->get_klass(will_link);
  ValueStack* state_before = !klass->is_loaded() || PatchALot ? copy_state_before() : copy_state_for_exception();
  CheckCast* c = new CheckCast(klass, apop(), state_before);
  apush(append_split(c));
  c->set_direct_compare(direct_compare(klass));

  if (is_profiling()) {
    // Note that we'd collect profile data in this method if we wanted it.
    compilation()->set_would_profile(true);

    if (profile_checkcasts()) {
      c->set_profiled_method(method());
      c->set_profiled_bci(bci());
      c->set_should_profile(true);
    }
  }
}


void GraphBuilder::instance_of(int klass_index) {
  bool will_link;
  ciKlass* klass = stream()->get_klass(will_link);
  ValueStack* state_before = !klass->is_loaded() || PatchALot ? copy_state_before() : copy_state_exhandling();
  InstanceOf* i = new InstanceOf(klass, apop(), state_before);
  ipush(append_split(i));
  i->set_direct_compare(direct_compare(klass));

  if (is_profiling()) {
    // Note that we'd collect profile data in this method if we wanted it.
    compilation()->set_would_profile(true);

    if (profile_checkcasts()) {
      i->set_profiled_method(method());
      i->set_profiled_bci(bci());
      i->set_should_profile(true);
    }
  }
}


void GraphBuilder::monitorenter(Value x, int bci) {
  // save state before locking in case of deoptimization after a NullPointerException
  ValueStack* state_before = copy_state_for_exception_with_bci(bci);
  append_with_bci(new MonitorEnter(x, state()->lock(x), state_before), bci);
  kill_all();
}


void GraphBuilder::monitorexit(Value x, int bci) {
  append_with_bci(new MonitorExit(x, state()->unlock()), bci);
  kill_all();
}


void GraphBuilder::new_multi_array(int dimensions) {
  bool will_link;
  ciKlass* klass = stream()->get_klass(will_link);
  ValueStack* state_before = !klass->is_loaded() || PatchALot ? copy_state_before() : copy_state_exhandling();

  Values* dims = new Values(dimensions, dimensions, NULL);
  // fill in all dimensions
  int i = dimensions;
  while (i-- > 0) dims->at_put(i, ipop());
  // create array
  NewArray* n = new NewMultiArray(klass, dims, state_before);
  apush(append_split(n));
}


void GraphBuilder::throw_op(int bci) {
  // We require that the debug info for a Throw be the "state before"
  // the Throw (i.e., exception oop is still on TOS)
  ValueStack* state_before = copy_state_before_with_bci(bci);
  Throw* t = new Throw(apop(), state_before);
  // operand stack not needed after a throw
  state()->truncate_stack(0);
  append_with_bci(t, bci);
}


Value GraphBuilder::round_fp(Value fp_value) {
  if (strict_fp_requires_explicit_rounding) {
#ifdef IA32
    // no rounding needed if SSE2 is used
    if (UseSSE < 2) {
      // Must currently insert rounding node for doubleword values that
      // are results of expressions (i.e., not loads from memory or
      // constants)
      if (fp_value->type()->tag() == doubleTag &&
          fp_value->as_Constant() == NULL &&
          fp_value->as_Local() == NULL &&       // method parameters need no rounding
          fp_value->as_RoundFP() == NULL) {
        return append(new RoundFP(fp_value));
      }
    }
#else
    Unimplemented();
#endif // IA32
  }
  return fp_value;
}


Instruction* GraphBuilder::append_with_bci(Instruction* instr, int bci) {
  Canonicalizer canon(compilation(), instr, bci);
  Instruction* i1 = canon.canonical();
  if (i1->is_linked() || !i1->can_be_linked()) {
    // Canonicalizer returned an instruction which was already
    // appended so simply return it.
    return i1;
  }

  if (UseLocalValueNumbering) {
    // Lookup the instruction in the ValueMap and add it to the map if
    // it's not found.
    Instruction* i2 = vmap()->find_insert(i1);
    if (i2 != i1) {
      // found an entry in the value map, so just return it.
      assert(i2->is_linked(), "should already be linked");
      return i2;
    }
    ValueNumberingEffects vne(vmap());
    i1->visit(&vne);
  }

  // i1 was not eliminated => append it
  assert(i1->next() == NULL, "shouldn't already be linked");
  _last = _last->set_next(i1, canon.bci());

  if (++_instruction_count >= InstructionCountCutoff && !bailed_out()) {
    // set the bailout state but complete normal processing.  We
    // might do a little more work before noticing the bailout so we
    // want processing to continue normally until it's noticed.
    bailout("Method and/or inlining is too large");
  }

#ifndef PRODUCT
  if (PrintIRDuringConstruction) {
    InstructionPrinter ip;
    ip.print_line(i1);
    if (Verbose) {
      state()->print();
    }
  }
#endif

  // save state after modification of operand stack for StateSplit instructions
  StateSplit* s = i1->as_StateSplit();
  if (s != NULL) {
    if (EliminateFieldAccess) {
      Intrinsic* intrinsic = s->as_Intrinsic();
      if (s->as_Invoke() != NULL || (intrinsic && !intrinsic->preserves_state())) {
        _memory->kill();
      }
    }
    s->set_state(state()->copy(ValueStack::StateAfter, canon.bci()));
  }

  // set up exception handlers for this instruction if necessary
  if (i1->can_trap()) {
    i1->set_exception_handlers(handle_exception(i1));
    assert(i1->exception_state() != NULL || !i1->needs_exception_state() || bailed_out(), "handle_exception must set exception state");
  }
  return i1;
}


Instruction* GraphBuilder::append(Instruction* instr) {
  assert(instr->as_StateSplit() == NULL || instr->as_BlockEnd() != NULL, "wrong append used");
  return append_with_bci(instr, bci());
}


Instruction* GraphBuilder::append_split(StateSplit* instr) {
  return append_with_bci(instr, bci());
}


void GraphBuilder::null_check(Value value) {
  if (value->as_NewArray() != NULL || value->as_NewInstance() != NULL) {
    return;
  } else {
    Constant* con = value->as_Constant();
    if (con) {
      ObjectType* c = con->type()->as_ObjectType();
      if (c && c->is_loaded()) {
        ObjectConstant* oc = c->as_ObjectConstant();
        if (!oc || !oc->value()->is_null_object()) {
          return;
        }
      }
    }
  }
  append(new NullCheck(value, copy_state_for_exception()));
}



XHandlers* GraphBuilder::handle_exception(Instruction* instruction) {
  if (!has_handler() && (!instruction->needs_exception_state() || instruction->exception_state() != NULL)) {
    assert(instruction->exception_state() == NULL
           || instruction->exception_state()->kind() == ValueStack::EmptyExceptionState
           || (instruction->exception_state()->kind() == ValueStack::ExceptionState && _compilation->env()->should_retain_local_variables()),
           "exception_state should be of exception kind");
    return new XHandlers();
  }

  XHandlers*  exception_handlers = new XHandlers();
  ScopeData*  cur_scope_data = scope_data();
  ValueStack* cur_state = instruction->state_before();
  ValueStack* prev_state = NULL;
  int scope_count = 0;

  assert(cur_state != NULL, "state_before must be set");
  do {
    int cur_bci = cur_state->bci();
    assert(cur_scope_data->scope() == cur_state->scope(), "scopes do not match");
    assert(cur_bci == SynchronizationEntryBCI || cur_bci == cur_scope_data->stream()->cur_bci(), "invalid bci");

    // join with all potential exception handlers
    XHandlers* list = cur_scope_data->xhandlers();
    const int n = list->length();
    for (int i = 0; i < n; i++) {
      XHandler* h = list->handler_at(i);
      if (h->covers(cur_bci)) {
        // h is a potential exception handler => join it
        compilation()->set_has_exception_handlers(true);

        BlockBegin* entry = h->entry_block();
        if (entry == block()) {
          // It's acceptable for an exception handler to cover itself
          // but we don't handle that in the parser currently.  It's
          // very rare so we bailout instead of trying to handle it.
          BAILOUT_("exception handler covers itself", exception_handlers);
        }
        assert(entry->bci() == h->handler_bci(), "must match");
        assert(entry->bci() == -1 || entry == cur_scope_data->block_at(entry->bci()), "blocks must correspond");

        // previously this was a BAILOUT, but this is not necessary
        // now because asynchronous exceptions are not handled this way.
        assert(entry->state() == NULL || cur_state->total_locks_size() == entry->state()->total_locks_size(), "locks do not match");

        // xhandler start with an empty expression stack
        if (cur_state->stack_size() != 0) {
          cur_state = cur_state->copy(ValueStack::ExceptionState, cur_state->bci());
        }
        if (instruction->exception_state() == NULL) {
          instruction->set_exception_state(cur_state);
        }

        // Note: Usually this join must work. However, very
        // complicated jsr-ret structures where we don't ret from
        // the subroutine can cause the objects on the monitor
        // stacks to not match because blocks can be parsed twice.
        // The only test case we've seen so far which exhibits this
        // problem is caught by the infinite recursion test in
        // GraphBuilder::jsr() if the join doesn't work.
        if (!entry->try_merge(cur_state)) {
          BAILOUT_("error while joining with exception handler, prob. due to complicated jsr/rets", exception_handlers);
        }

        // add current state for correct handling of phi functions at begin of xhandler
        int phi_operand = entry->add_exception_state(cur_state);

        // add entry to the list of xhandlers of this block
        _block->add_exception_handler(entry);

        // add back-edge from xhandler entry to this block
        if (!entry->is_predecessor(_block)) {
          entry->add_predecessor(_block);
        }

        // clone XHandler because phi_operand and scope_count can not be shared
        XHandler* new_xhandler = new XHandler(h);
        new_xhandler->set_phi_operand(phi_operand);
        new_xhandler->set_scope_count(scope_count);
        exception_handlers->append(new_xhandler);

        // fill in exception handler subgraph lazily
        assert(!entry->is_set(BlockBegin::was_visited_flag), "entry must not be visited yet");
        cur_scope_data->add_to_work_list(entry);

        // stop when reaching catchall
        if (h->catch_type() == 0) {
          return exception_handlers;
        }
      }
    }

    if (exception_handlers->length() == 0) {
      // This scope and all callees do not handle exceptions, so the local
      // variables of this scope are not needed. However, the scope itself is
      // required for a correct exception stack trace -> clear out the locals.
      if (_compilation->env()->should_retain_local_variables()) {
        cur_state = cur_state->copy(ValueStack::ExceptionState, cur_state->bci());
      } else {
        cur_state = cur_state->copy(ValueStack::EmptyExceptionState, cur_state->bci());
      }
      if (prev_state != NULL) {
        prev_state->set_caller_state(cur_state);
      }
      if (instruction->exception_state() == NULL) {
        instruction->set_exception_state(cur_state);
      }
    }

    // Set up iteration for next time.
    // If parsing a jsr, do not grab exception handlers from the
    // parent scopes for this method (already got them, and they
    // needed to be cloned)

    while (cur_scope_data->parsing_jsr()) {
      cur_scope_data = cur_scope_data->parent();
    }

    assert(cur_scope_data->scope() == cur_state->scope(), "scopes do not match");
    assert(cur_state->locks_size() == 0 || cur_state->locks_size() == 1, "unlocking must be done in a catchall exception handler");

    prev_state = cur_state;
    cur_state = cur_state->caller_state();
    cur_scope_data = cur_scope_data->parent();
    scope_count++;
  } while (cur_scope_data != NULL);

  return exception_handlers;
}


// Helper class for simplifying Phis.
class PhiSimplifier : public BlockClosure {
 private:
  bool _has_substitutions;
  Value simplify(Value v);

 public:
  PhiSimplifier(BlockBegin* start) : _has_substitutions(false) {
    start->iterate_preorder(this);
    if (_has_substitutions) {
      SubstitutionResolver sr(start);
    }
  }
  void block_do(BlockBegin* b);
  bool has_substitutions() const { return _has_substitutions; }
};


Value PhiSimplifier::simplify(Value v) {
  Phi* phi = v->as_Phi();

  if (phi == NULL) {
    // no phi function
    return v;
  } else if (v->has_subst()) {
    // already substituted; subst can be phi itself -> simplify
    return simplify(v->subst());
  } else if (phi->is_set(Phi::cannot_simplify)) {
    // already tried to simplify phi before
    return phi;
  } else if (phi->is_set(Phi::visited)) {
    // break cycles in phi functions
    return phi;
  } else if (phi->type()->is_illegal()) {
    // illegal phi functions are ignored anyway
    return phi;

  } else {
    // mark phi function as processed to break cycles in phi functions
    phi->set(Phi::visited);

    // simplify x = [y, x] and x = [y, y] to y
    Value subst = NULL;
    int opd_count = phi->operand_count();
    for (int i = 0; i < opd_count; i++) {
      Value opd = phi->operand_at(i);
      assert(opd != NULL, "Operand must exist!");

      if (opd->type()->is_illegal()) {
        // if one operand is illegal, the entire phi function is illegal
        phi->make_illegal();
        phi->clear(Phi::visited);
        return phi;
      }

      Value new_opd = simplify(opd);
      assert(new_opd != NULL, "Simplified operand must exist!");

      if (new_opd != phi && new_opd != subst) {
        if (subst == NULL) {
          subst = new_opd;
        } else {
          // no simplification possible
          phi->set(Phi::cannot_simplify);
          phi->clear(Phi::visited);
          return phi;
        }
      }
    }

    // sucessfully simplified phi function
    assert(subst != NULL, "illegal phi function");
    _has_substitutions = true;
    phi->clear(Phi::visited);
    phi->set_subst(subst);

#ifndef PRODUCT
    if (PrintPhiFunctions) {
      tty->print_cr("simplified phi function %c%d to %c%d (Block B%d)", phi->type()->tchar(), phi->id(), subst->type()->tchar(), subst->id(), phi->block()->block_id());
    }
#endif

    return subst;
  }
}


void PhiSimplifier::block_do(BlockBegin* b) {
  for_each_phi_fun(b, phi,
    simplify(phi);
  );

#ifdef ASSERT
  for_each_phi_fun(b, phi,
                   assert(phi->operand_count() != 1 || phi->subst() != phi || phi->is_illegal(), "missed trivial simplification");
  );

  ValueStack* state = b->state()->caller_state();
  for_each_state_value(state, value,
    Phi* phi = value->as_Phi();
    assert(phi == NULL || phi->block() != b, "must not have phi function to simplify in caller state");
  );
#endif
}

// This method is called after all blocks are filled with HIR instructions
// It eliminates all Phi functions of the form x = [y, y] and x = [y, x]
void GraphBuilder::eliminate_redundant_phis(BlockBegin* start) {
  PhiSimplifier simplifier(start);
}


void GraphBuilder::connect_to_end(BlockBegin* beg) {
  // setup iteration
  kill_all();
  _block = beg;
  _state = beg->state()->copy_for_parsing();
  _last  = beg;
  iterate_bytecodes_for_block(beg->bci());
}


BlockEnd* GraphBuilder::iterate_bytecodes_for_block(int bci) {
#ifndef PRODUCT
  if (PrintIRDuringConstruction) {
    tty->cr();
    InstructionPrinter ip;
    ip.print_instr(_block); tty->cr();
    ip.print_stack(_block->state()); tty->cr();
    ip.print_inline_level(_block);
    ip.print_head();
    tty->print_cr("locals size: %d stack size: %d", state()->locals_size(), state()->stack_size());
  }
#endif
  _skip_block = false;
  assert(state() != NULL, "ValueStack missing!");
  CompileLog* log = compilation()->log();
  ciBytecodeStream s(method());
  s.reset_to_bci(bci);
  int prev_bci = bci;
  scope_data()->set_stream(&s);
  // iterate
  Bytecodes::Code code = Bytecodes::_illegal;
  bool push_exception = false;

  if (block()->is_set(BlockBegin::exception_entry_flag) && block()->next() == NULL) {
    // first thing in the exception entry block should be the exception object.
    push_exception = true;
  }

  bool ignore_return = scope_data()->ignore_return();

  while (!bailed_out() && last()->as_BlockEnd() == NULL &&
         (code = stream()->next()) != ciBytecodeStream::EOBC() &&
         (block_at(s.cur_bci()) == NULL || block_at(s.cur_bci()) == block())) {
    assert(state()->kind() == ValueStack::Parsing, "invalid state kind");

    if (log != NULL)
      log->set_context("bc code='%d' bci='%d'", (int)code, s.cur_bci());

    // Check for active jsr during OSR compilation
    if (compilation()->is_osr_compile()
        && scope()->is_top_scope()
        && parsing_jsr()
        && s.cur_bci() == compilation()->osr_bci()) {
      bailout("OSR not supported while a jsr is active");
    }

    if (push_exception) {
      apush(append(new ExceptionObject()));
      push_exception = false;
    }

    // handle bytecode
    switch (code) {
      case Bytecodes::_nop            : /* nothing to do */ break;
      case Bytecodes::_aconst_null    : apush(append(new Constant(objectNull            ))); break;
      case Bytecodes::_iconst_m1      : ipush(append(new Constant(new IntConstant   (-1)))); break;
      case Bytecodes::_iconst_0       : ipush(append(new Constant(intZero               ))); break;
      case Bytecodes::_iconst_1       : ipush(append(new Constant(intOne                ))); break;
      case Bytecodes::_iconst_2       : ipush(append(new Constant(new IntConstant   ( 2)))); break;
      case Bytecodes::_iconst_3       : ipush(append(new Constant(new IntConstant   ( 3)))); break;
      case Bytecodes::_iconst_4       : ipush(append(new Constant(new IntConstant   ( 4)))); break;
      case Bytecodes::_iconst_5       : ipush(append(new Constant(new IntConstant   ( 5)))); break;
      case Bytecodes::_lconst_0       : lpush(append(new Constant(new LongConstant  ( 0)))); break;
      case Bytecodes::_lconst_1       : lpush(append(new Constant(new LongConstant  ( 1)))); break;
      case Bytecodes::_fconst_0       : fpush(append(new Constant(new FloatConstant ( 0)))); break;
      case Bytecodes::_fconst_1       : fpush(append(new Constant(new FloatConstant ( 1)))); break;
      case Bytecodes::_fconst_2       : fpush(append(new Constant(new FloatConstant ( 2)))); break;
      case Bytecodes::_dconst_0       : dpush(append(new Constant(new DoubleConstant( 0)))); break;
      case Bytecodes::_dconst_1       : dpush(append(new Constant(new DoubleConstant( 1)))); break;
      case Bytecodes::_bipush         : ipush(append(new Constant(new IntConstant(((signed char*)s.cur_bcp())[1])))); break;
      case Bytecodes::_sipush         : ipush(append(new Constant(new IntConstant((short)Bytes::get_Java_u2(s.cur_bcp()+1))))); break;
      case Bytecodes::_ldc            : // fall through
      case Bytecodes::_ldc_w          : // fall through
      case Bytecodes::_ldc2_w         : load_constant(); break;
      case Bytecodes::_iload          : load_local(intType     , s.get_index()); break;
      case Bytecodes::_lload          : load_local(longType    , s.get_index()); break;
      case Bytecodes::_fload          : load_local(floatType   , s.get_index()); break;
      case Bytecodes::_dload          : load_local(doubleType  , s.get_index()); break;
      case Bytecodes::_aload          : load_local(instanceType, s.get_index()); break;
      case Bytecodes::_iload_0        : load_local(intType   , 0); break;
      case Bytecodes::_iload_1        : load_local(intType   , 1); break;
      case Bytecodes::_iload_2        : load_local(intType   , 2); break;
      case Bytecodes::_iload_3        : load_local(intType   , 3); break;
      case Bytecodes::_lload_0        : load_local(longType  , 0); break;
      case Bytecodes::_lload_1        : load_local(longType  , 1); break;
      case Bytecodes::_lload_2        : load_local(longType  , 2); break;
      case Bytecodes::_lload_3        : load_local(longType  , 3); break;
      case Bytecodes::_fload_0        : load_local(floatType , 0); break;
      case Bytecodes::_fload_1        : load_local(floatType , 1); break;
      case Bytecodes::_fload_2        : load_local(floatType , 2); break;
      case Bytecodes::_fload_3        : load_local(floatType , 3); break;
      case Bytecodes::_dload_0        : load_local(doubleType, 0); break;
      case Bytecodes::_dload_1        : load_local(doubleType, 1); break;
      case Bytecodes::_dload_2        : load_local(doubleType, 2); break;
      case Bytecodes::_dload_3        : load_local(doubleType, 3); break;
      case Bytecodes::_aload_0        : load_local(objectType, 0); break;
      case Bytecodes::_aload_1        : load_local(objectType, 1); break;
      case Bytecodes::_aload_2        : load_local(objectType, 2); break;
      case Bytecodes::_aload_3        : load_local(objectType, 3); break;
      case Bytecodes::_iaload         : load_indexed(T_INT   ); break;
      case Bytecodes::_laload         : load_indexed(T_LONG  ); break;
      case Bytecodes::_faload         : load_indexed(T_FLOAT ); break;
      case Bytecodes::_daload         : load_indexed(T_DOUBLE); break;
      case Bytecodes::_aaload         : load_indexed(T_OBJECT); break;
      case Bytecodes::_baload         : load_indexed(T_BYTE  ); break;
      case Bytecodes::_caload         : load_indexed(T_CHAR  ); break;
      case Bytecodes::_saload         : load_indexed(T_SHORT ); break;
      case Bytecodes::_istore         : store_local(intType   , s.get_index()); break;
      case Bytecodes::_lstore         : store_local(longType  , s.get_index()); break;
      case Bytecodes::_fstore         : store_local(floatType , s.get_index()); break;
      case Bytecodes::_dstore         : store_local(doubleType, s.get_index()); break;
      case Bytecodes::_astore         : store_local(objectType, s.get_index()); break;
      case Bytecodes::_istore_0       : store_local(intType   , 0); break;
      case Bytecodes::_istore_1       : store_local(intType   , 1); break;
      case Bytecodes::_istore_2       : store_local(intType   , 2); break;
      case Bytecodes::_istore_3       : store_local(intType   , 3); break;
      case Bytecodes::_lstore_0       : store_local(longType  , 0); break;
      case Bytecodes::_lstore_1       : store_local(longType  , 1); break;
      case Bytecodes::_lstore_2       : store_local(longType  , 2); break;
      case Bytecodes::_lstore_3       : store_local(longType  , 3); break;
      case Bytecodes::_fstore_0       : store_local(floatType , 0); break;
      case Bytecodes::_fstore_1       : store_local(floatType , 1); break;
      case Bytecodes::_fstore_2       : store_local(floatType , 2); break;
      case Bytecodes::_fstore_3       : store_local(floatType , 3); break;
      case Bytecodes::_dstore_0       : store_local(doubleType, 0); break;
      case Bytecodes::_dstore_1       : store_local(doubleType, 1); break;
      case Bytecodes::_dstore_2       : store_local(doubleType, 2); break;
      case Bytecodes::_dstore_3       : store_local(doubleType, 3); break;
      case Bytecodes::_astore_0       : store_local(objectType, 0); break;
      case Bytecodes::_astore_1       : store_local(objectType, 1); break;
      case Bytecodes::_astore_2       : store_local(objectType, 2); break;
      case Bytecodes::_astore_3       : store_local(objectType, 3); break;
      case Bytecodes::_iastore        : store_indexed(T_INT   ); break;
      case Bytecodes::_lastore        : store_indexed(T_LONG  ); break;
      case Bytecodes::_fastore        : store_indexed(T_FLOAT ); break;
      case Bytecodes::_dastore        : store_indexed(T_DOUBLE); break;
      case Bytecodes::_aastore        : store_indexed(T_OBJECT); break;
      case Bytecodes::_bastore        : store_indexed(T_BYTE  ); break;
      case Bytecodes::_castore        : store_indexed(T_CHAR  ); break;
      case Bytecodes::_sastore        : store_indexed(T_SHORT ); break;
      case Bytecodes::_pop            : // fall through
      case Bytecodes::_pop2           : // fall through
      case Bytecodes::_dup            : // fall through
      case Bytecodes::_dup_x1         : // fall through
      case Bytecodes::_dup_x2         : // fall through
      case Bytecodes::_dup2           : // fall through
      case Bytecodes::_dup2_x1        : // fall through
      case Bytecodes::_dup2_x2        : // fall through
      case Bytecodes::_swap           : stack_op(code); break;
      case Bytecodes::_iadd           : arithmetic_op(intType   , code); break;
      case Bytecodes::_ladd           : arithmetic_op(longType  , code); break;
      case Bytecodes::_fadd           : arithmetic_op(floatType , code); break;
      case Bytecodes::_dadd           : arithmetic_op(doubleType, code); break;
      case Bytecodes::_isub           : arithmetic_op(intType   , code); break;
      case Bytecodes::_lsub           : arithmetic_op(longType  , code); break;
      case Bytecodes::_fsub           : arithmetic_op(floatType , code); break;
      case Bytecodes::_dsub           : arithmetic_op(doubleType, code); break;
      case Bytecodes::_imul           : arithmetic_op(intType   , code); break;
      case Bytecodes::_lmul           : arithmetic_op(longType  , code); break;
      case Bytecodes::_fmul           : arithmetic_op(floatType , code); break;
      case Bytecodes::_dmul           : arithmetic_op(doubleType, code); break;
      case Bytecodes::_idiv           : arithmetic_op(intType   , code, copy_state_for_exception()); break;
      case Bytecodes::_ldiv           : arithmetic_op(longType  , code, copy_state_for_exception()); break;
      case Bytecodes::_fdiv           : arithmetic_op(floatType , code); break;
      case Bytecodes::_ddiv           : arithmetic_op(doubleType, code); break;
      case Bytecodes::_irem           : arithmetic_op(intType   , code, copy_state_for_exception()); break;
      case Bytecodes::_lrem           : arithmetic_op(longType  , code, copy_state_for_exception()); break;
      case Bytecodes::_frem           : arithmetic_op(floatType , code); break;
      case Bytecodes::_drem           : arithmetic_op(doubleType, code); break;
      case Bytecodes::_ineg           : negate_op(intType   ); break;
      case Bytecodes::_lneg           : negate_op(longType  ); break;
      case Bytecodes::_fneg           : negate_op(floatType ); break;
      case Bytecodes::_dneg           : negate_op(doubleType); break;
      case Bytecodes::_ishl           : shift_op(intType , code); break;
      case Bytecodes::_lshl           : shift_op(longType, code); break;
      case Bytecodes::_ishr           : shift_op(intType , code); break;
      case Bytecodes::_lshr           : shift_op(longType, code); break;
      case Bytecodes::_iushr          : shift_op(intType , code); break;
      case Bytecodes::_lushr          : shift_op(longType, code); break;
      case Bytecodes::_iand           : logic_op(intType , code); break;
      case Bytecodes::_land           : logic_op(longType, code); break;
      case Bytecodes::_ior            : logic_op(intType , code); break;
      case Bytecodes::_lor            : logic_op(longType, code); break;
      case Bytecodes::_ixor           : logic_op(intType , code); break;
      case Bytecodes::_lxor           : logic_op(longType, code); break;
      case Bytecodes::_iinc           : increment(); break;
      case Bytecodes::_i2l            : convert(code, T_INT   , T_LONG  ); break;
      case Bytecodes::_i2f            : convert(code, T_INT   , T_FLOAT ); break;
      case Bytecodes::_i2d            : convert(code, T_INT   , T_DOUBLE); break;
      case Bytecodes::_l2i            : convert(code, T_LONG  , T_INT   ); break;
      case Bytecodes::_l2f            : convert(code, T_LONG  , T_FLOAT ); break;
      case Bytecodes::_l2d            : convert(code, T_LONG  , T_DOUBLE); break;
      case Bytecodes::_f2i            : convert(code, T_FLOAT , T_INT   ); break;
      case Bytecodes::_f2l            : convert(code, T_FLOAT , T_LONG  ); break;
      case Bytecodes::_f2d            : convert(code, T_FLOAT , T_DOUBLE); break;
      case Bytecodes::_d2i            : convert(code, T_DOUBLE, T_INT   ); break;
      case Bytecodes::_d2l            : convert(code, T_DOUBLE, T_LONG  ); break;
      case Bytecodes::_d2f            : convert(code, T_DOUBLE, T_FLOAT ); break;
      case Bytecodes::_i2b            : convert(code, T_INT   , T_BYTE  ); break;
      case Bytecodes::_i2c            : convert(code, T_INT   , T_CHAR  ); break;
      case Bytecodes::_i2s            : convert(code, T_INT   , T_SHORT ); break;
      case Bytecodes::_lcmp           : compare_op(longType  , code); break;
      case Bytecodes::_fcmpl          : compare_op(floatType , code); break;
      case Bytecodes::_fcmpg          : compare_op(floatType , code); break;
      case Bytecodes::_dcmpl          : compare_op(doubleType, code); break;
      case Bytecodes::_dcmpg          : compare_op(doubleType, code); break;
      case Bytecodes::_ifeq           : if_zero(intType   , If::eql); break;
      case Bytecodes::_ifne           : if_zero(intType   , If::neq); break;
      case Bytecodes::_iflt           : if_zero(intType   , If::lss); break;
      case Bytecodes::_ifge           : if_zero(intType   , If::geq); break;
      case Bytecodes::_ifgt           : if_zero(intType   , If::gtr); break;
      case Bytecodes::_ifle           : if_zero(intType   , If::leq); break;
      case Bytecodes::_if_icmpeq      : if_same(intType   , If::eql); break;
      case Bytecodes::_if_icmpne      : if_same(intType   , If::neq); break;
      case Bytecodes::_if_icmplt      : if_same(intType   , If::lss); break;
      case Bytecodes::_if_icmpge      : if_same(intType   , If::geq); break;
      case Bytecodes::_if_icmpgt      : if_same(intType   , If::gtr); break;
      case Bytecodes::_if_icmple      : if_same(intType   , If::leq); break;
      case Bytecodes::_if_acmpeq      : if_same(objectType, If::eql); break;
      case Bytecodes::_if_acmpne      : if_same(objectType, If::neq); break;
      case Bytecodes::_goto           : _goto(s.cur_bci(), s.get_dest()); break;
      case Bytecodes::_jsr            : jsr(s.get_dest()); break;
      case Bytecodes::_ret            : ret(s.get_index()); break;
      case Bytecodes::_tableswitch    : table_switch(); break;
      case Bytecodes::_lookupswitch   : lookup_switch(); break;
      case Bytecodes::_ireturn        : method_return(ipop(), ignore_return); break;
      case Bytecodes::_lreturn        : method_return(lpop(), ignore_return); break;
      case Bytecodes::_freturn        : method_return(fpop(), ignore_return); break;
      case Bytecodes::_dreturn        : method_return(dpop(), ignore_return); break;
      case Bytecodes::_areturn        : method_return(apop(), ignore_return); break;
      case Bytecodes::_return         : method_return(NULL  , ignore_return); break;
      case Bytecodes::_getstatic      : // fall through
      case Bytecodes::_putstatic      : // fall through
      case Bytecodes::_getfield       : // fall through
      case Bytecodes::_putfield       : access_field(code); break;
      case Bytecodes::_invokevirtual  : // fall through
      case Bytecodes::_invokespecial  : // fall through
      case Bytecodes::_invokestatic   : // fall through
      case Bytecodes::_invokedynamic  : // fall through
      case Bytecodes::_invokeinterface: invoke(code); break;
      case Bytecodes::_new            : new_instance(s.get_index_u2()); break;
      case Bytecodes::_newarray       : new_type_array(); break;
      case Bytecodes::_anewarray      : new_object_array(); break;
      case Bytecodes::_arraylength    : { ValueStack* state_before = copy_state_for_exception(); ipush(append(new ArrayLength(apop(), state_before))); break; }
      case Bytecodes::_athrow         : throw_op(s.cur_bci()); break;
      case Bytecodes::_checkcast      : check_cast(s.get_index_u2()); break;
      case Bytecodes::_instanceof     : instance_of(s.get_index_u2()); break;
      case Bytecodes::_monitorenter   : monitorenter(apop(), s.cur_bci()); break;
      case Bytecodes::_monitorexit    : monitorexit (apop(), s.cur_bci()); break;
      case Bytecodes::_wide           : ShouldNotReachHere(); break;
      case Bytecodes::_multianewarray : new_multi_array(s.cur_bcp()[3]); break;
      case Bytecodes::_ifnull         : if_null(objectType, If::eql); break;
      case Bytecodes::_ifnonnull      : if_null(objectType, If::neq); break;
      case Bytecodes::_goto_w         : _goto(s.cur_bci(), s.get_far_dest()); break;
      case Bytecodes::_jsr_w          : jsr(s.get_far_dest()); break;
      case Bytecodes::_breakpoint     : BAILOUT_("concurrent setting of breakpoint", NULL);
      default                         : ShouldNotReachHere(); break;
    }

    if (log != NULL)
      log->clear_context(); // skip marker if nothing was printed

    // save current bci to setup Goto at the end
    prev_bci = s.cur_bci();

  }
  CHECK_BAILOUT_(NULL);
  // stop processing of this block (see try_inline_full)
  if (_skip_block) {
    _skip_block = false;
    assert(_last && _last->as_BlockEnd(), "");
    return _last->as_BlockEnd();
  }
  // if there are any, check if last instruction is a BlockEnd instruction
  BlockEnd* end = last()->as_BlockEnd();
  if (end == NULL) {
    // all blocks must end with a BlockEnd instruction => add a Goto
    end = new Goto(block_at(s.cur_bci()), false);
    append(end);
  }
  assert(end == last()->as_BlockEnd(), "inconsistency");

  assert(end->state() != NULL, "state must already be present");
  assert(end->as_Return() == NULL || end->as_Throw() == NULL || end->state()->stack_size() == 0, "stack not needed for return and throw");

  // connect to begin & set state
  // NOTE that inlining may have changed the block we are parsing
  block()->set_end(end);
  // propagate state
  for (int i = end->number_of_sux() - 1; i >= 0; i--) {
    BlockBegin* sux = end->sux_at(i);
    assert(sux->is_predecessor(block()), "predecessor missing");
    // be careful, bailout if bytecodes are strange
    if (!sux->try_merge(end->state())) BAILOUT_("block join failed", NULL);
    scope_data()->add_to_work_list(end->sux_at(i));
  }

  scope_data()->set_stream(NULL);

  // done
  return end;
}


void GraphBuilder::iterate_all_blocks(bool start_in_current_block_for_inlining) {
  do {
    if (start_in_current_block_for_inlining && !bailed_out()) {
      iterate_bytecodes_for_block(0);
      start_in_current_block_for_inlining = false;
    } else {
      BlockBegin* b;
      while ((b = scope_data()->remove_from_work_list()) != NULL) {
        if (!b->is_set(BlockBegin::was_visited_flag)) {
          if (b->is_set(BlockBegin::osr_entry_flag)) {
            // we're about to parse the osr entry block, so make sure
            // we setup the OSR edge leading into this block so that
            // Phis get setup correctly.
            setup_osr_entry_block();
            // this is no longer the osr entry block, so clear it.
            b->clear(BlockBegin::osr_entry_flag);
          }
          b->set(BlockBegin::was_visited_flag);
          connect_to_end(b);
        }
      }
    }
  } while (!bailed_out() && !scope_data()->is_work_list_empty());
}


bool GraphBuilder::_can_trap      [Bytecodes::number_of_java_codes];

void GraphBuilder::initialize() {
  // the following bytecodes are assumed to potentially
  // throw exceptions in compiled code - note that e.g.
  // monitorexit & the return bytecodes do not throw
  // exceptions since monitor pairing proved that they
  // succeed (if monitor pairing succeeded)
  Bytecodes::Code can_trap_list[] =
    { Bytecodes::_ldc
    , Bytecodes::_ldc_w
    , Bytecodes::_ldc2_w
    , Bytecodes::_iaload
    , Bytecodes::_laload
    , Bytecodes::_faload
    , Bytecodes::_daload
    , Bytecodes::_aaload
    , Bytecodes::_baload
    , Bytecodes::_caload
    , Bytecodes::_saload
    , Bytecodes::_iastore
    , Bytecodes::_lastore
    , Bytecodes::_fastore
    , Bytecodes::_dastore
    , Bytecodes::_aastore
    , Bytecodes::_bastore
    , Bytecodes::_castore
    , Bytecodes::_sastore
    , Bytecodes::_idiv
    , Bytecodes::_ldiv
    , Bytecodes::_irem
    , Bytecodes::_lrem
    , Bytecodes::_getstatic
    , Bytecodes::_putstatic
    , Bytecodes::_getfield
    , Bytecodes::_putfield
    , Bytecodes::_invokevirtual
    , Bytecodes::_invokespecial
    , Bytecodes::_invokestatic
    , Bytecodes::_invokedynamic
    , Bytecodes::_invokeinterface
    , Bytecodes::_new
    , Bytecodes::_newarray
    , Bytecodes::_anewarray
    , Bytecodes::_arraylength
    , Bytecodes::_athrow
    , Bytecodes::_checkcast
    , Bytecodes::_instanceof
    , Bytecodes::_monitorenter
    , Bytecodes::_multianewarray
    };

  // inititialize trap tables
  for (int i = 0; i < Bytecodes::number_of_java_codes; i++) {
    _can_trap[i] = false;
  }
  // set standard trap info
  for (uint j = 0; j < ARRAY_SIZE(can_trap_list); j++) {
    _can_trap[can_trap_list[j]] = true;
  }
}


BlockBegin* GraphBuilder::header_block(BlockBegin* entry, BlockBegin::Flag f, ValueStack* state) {
  assert(entry->is_set(f), "entry/flag mismatch");
  // create header block
  BlockBegin* h = new BlockBegin(entry->bci());
  h->set_depth_first_number(0);

  Value l = h;
  BlockEnd* g = new Goto(entry, false);
  l->set_next(g, entry->bci());
  h->set_end(g);
  h->set(f);
  // setup header block end state
  ValueStack* s = state->copy(ValueStack::StateAfter, entry->bci()); // can use copy since stack is empty (=> no phis)
  assert(s->stack_is_empty(), "must have empty stack at entry point");
  g->set_state(s);
  return h;
}



BlockBegin* GraphBuilder::setup_start_block(int osr_bci, BlockBegin* std_entry, BlockBegin* osr_entry, ValueStack* state) {
  BlockBegin* start = new BlockBegin(0);

  // This code eliminates the empty start block at the beginning of
  // each method.  Previously, each method started with the
  // start-block created below, and this block was followed by the
  // header block that was always empty.  This header block is only
  // necesary if std_entry is also a backward branch target because
  // then phi functions may be necessary in the header block.  It's
  // also necessary when profiling so that there's a single block that
  // can increment the the counters.
  // In addition, with range check elimination, we may need a valid block
  // that dominates all the rest to insert range predicates.
  BlockBegin* new_header_block;
  if (std_entry->number_of_preds() > 0 || is_profiling() || RangeCheckElimination) {
    new_header_block = header_block(std_entry, BlockBegin::std_entry_flag, state);
  } else {
    new_header_block = std_entry;
  }

  // setup start block (root for the IR graph)
  Base* base =
    new Base(
      new_header_block,
      osr_entry
    );
  start->set_next(base, 0);
  start->set_end(base);
  // create & setup state for start block
  start->set_state(state->copy(ValueStack::StateAfter, std_entry->bci()));
  base->set_state(state->copy(ValueStack::StateAfter, std_entry->bci()));

  if (base->std_entry()->state() == NULL) {
    // setup states for header blocks
    base->std_entry()->merge(state);
  }

  assert(base->std_entry()->state() != NULL, "");
  return start;
}


void GraphBuilder::setup_osr_entry_block() {
  assert(compilation()->is_osr_compile(), "only for osrs");

  int osr_bci = compilation()->osr_bci();
  ciBytecodeStream s(method());
  s.reset_to_bci(osr_bci);
  s.next();
  scope_data()->set_stream(&s);

  // create a new block to be the osr setup code
  _osr_entry = new BlockBegin(osr_bci);
  _osr_entry->set(BlockBegin::osr_entry_flag);
  _osr_entry->set_depth_first_number(0);
  BlockBegin* target = bci2block()->at(osr_bci);
  assert(target != NULL && target->is_set(BlockBegin::osr_entry_flag), "must be there");
  // the osr entry has no values for locals
  ValueStack* state = target->state()->copy();
  _osr_entry->set_state(state);

  kill_all();
  _block = _osr_entry;
  _state = _osr_entry->state()->copy();
  assert(_state->bci() == osr_bci, "mismatch");
  _last  = _osr_entry;
  Value e = append(new OsrEntry());
  e->set_needs_null_check(false);

  // OSR buffer is
  //
  // locals[nlocals-1..0]
  // monitors[number_of_locks-1..0]
  //
  // locals is a direct copy of the interpreter frame so in the osr buffer
  // so first slot in the local array is the last local from the interpreter
  // and last slot is local[0] (receiver) from the interpreter
  //
  // Similarly with locks. The first lock slot in the osr buffer is the nth lock
  // from the interpreter frame, the nth lock slot in the osr buffer is 0th lock
  // in the interpreter frame (the method lock if a sync method)

  // Initialize monitors in the compiled activation.

  int index;
  Value local;

  // find all the locals that the interpreter thinks contain live oops
  const ResourceBitMap live_oops = method()->live_local_oops_at_bci(osr_bci);

  // compute the offset into the locals so that we can treat the buffer
  // as if the locals were still in the interpreter frame
  int locals_offset = BytesPerWord * (method()->max_locals() - 1);
  for_each_local_value(state, index, local) {
    int offset = locals_offset - (index + local->type()->size() - 1) * BytesPerWord;
    Value get;
    if (local->type()->is_object_kind() && !live_oops.at(index)) {
      // The interpreter thinks this local is dead but the compiler
      // doesn't so pretend that the interpreter passed in null.
      get = append(new Constant(objectNull));
    } else {
      Value off_val = append(new Constant(new IntConstant(offset)));
      get = append(new UnsafeGet(as_BasicType(local->type()), e,
                                 off_val,
                                 false/*is_volatile*/,
                                 true/*is_raw*/));
    }
    _state->store_local(index, get);
  }

  // the storage for the OSR buffer is freed manually in the LIRGenerator.

  assert(state->caller_state() == NULL, "should be top scope");
  state->clear_locals();
  Goto* g = new Goto(target, false);
  append(g);
  _osr_entry->set_end(g);
  target->merge(_osr_entry->end()->state());

  scope_data()->set_stream(NULL);
}


ValueStack* GraphBuilder::state_at_entry() {
  ValueStack* state = new ValueStack(scope(), NULL);

  // Set up locals for receiver
  int idx = 0;
  if (!method()->is_static()) {
    // we should always see the receiver
    state->store_local(idx, new Local(method()->holder(), objectType, idx, true));
    idx = 1;
  }

  // Set up locals for incoming arguments
  ciSignature* sig = method()->signature();
  for (int i = 0; i < sig->count(); i++) {
    ciType* type = sig->type_at(i);
    BasicType basic_type = type->basic_type();
    // don't allow T_ARRAY to propagate into locals types
    if (is_reference_type(basic_type)) basic_type = T_OBJECT;
    ValueType* vt = as_ValueType(basic_type);
    state->store_local(idx, new Local(type, vt, idx, false));
    idx += type->size();
  }

  // lock synchronized method
  if (method()->is_synchronized()) {
    state->lock(NULL);
  }

  return state;
}


GraphBuilder::GraphBuilder(Compilation* compilation, IRScope* scope)
  : _scope_data(NULL)
  , _compilation(compilation)
  , _memory(new MemoryBuffer())
  , _inline_bailout_msg(NULL)
  , _instruction_count(0)
  , _osr_entry(NULL)
{
  int osr_bci = compilation->osr_bci();

  // determine entry points and bci2block mapping
  BlockListBuilder blm(compilation, scope, osr_bci);
  CHECK_BAILOUT();

  BlockList* bci2block = blm.bci2block();
  BlockBegin* start_block = bci2block->at(0);

  push_root_scope(scope, bci2block, start_block);

  // setup state for std entry
  _initial_state = state_at_entry();
  start_block->merge(_initial_state);

  // complete graph
  _vmap        = new ValueMap();
  switch (scope->method()->intrinsic_id()) {
  case vmIntrinsics::_dabs          : // fall through
  case vmIntrinsics::_dsqrt         : // fall through
  case vmIntrinsics::_dsin          : // fall through
  case vmIntrinsics::_dcos          : // fall through
  case vmIntrinsics::_dtan          : // fall through
  case vmIntrinsics::_dlog          : // fall through
  case vmIntrinsics::_dlog10        : // fall through
  case vmIntrinsics::_dexp          : // fall through
  case vmIntrinsics::_dpow          : // fall through
    {
      // Compiles where the root method is an intrinsic need a special
      // compilation environment because the bytecodes for the method
      // shouldn't be parsed during the compilation, only the special
      // Intrinsic node should be emitted.  If this isn't done the the
      // code for the inlined version will be different than the root
      // compiled version which could lead to monotonicity problems on
      // intel.
      if (CheckIntrinsics && !scope->method()->intrinsic_candidate()) {
        BAILOUT("failed to inline intrinsic, method not annotated");
      }

      // Set up a stream so that appending instructions works properly.
      ciBytecodeStream s(scope->method());
      s.reset_to_bci(0);
      scope_data()->set_stream(&s);
      s.next();

      // setup the initial block state
      _block = start_block;
      _state = start_block->state()->copy_for_parsing();
      _last  = start_block;
      load_local(doubleType, 0);
      if (scope->method()->intrinsic_id() == vmIntrinsics::_dpow) {
        load_local(doubleType, 2);
      }

      // Emit the intrinsic node.
      bool result = try_inline_intrinsics(scope->method());
      if (!result) BAILOUT("failed to inline intrinsic");
      method_return(dpop());

      // connect the begin and end blocks and we're all done.
      BlockEnd* end = last()->as_BlockEnd();
      block()->set_end(end);
      break;
    }

  case vmIntrinsics::_Reference_get:
    {
      {
        // With java.lang.ref.reference.get() we must go through the
        // intrinsic - when G1 is enabled - even when get() is the root
        // method of the compile so that, if necessary, the value in
        // the referent field of the reference object gets recorded by
        // the pre-barrier code.
        // Specifically, if G1 is enabled, the value in the referent
        // field is recorded by the G1 SATB pre barrier. This will
        // result in the referent being marked live and the reference
        // object removed from the list of discovered references during
        // reference processing.
        if (CheckIntrinsics && !scope->method()->intrinsic_candidate()) {
          BAILOUT("failed to inline intrinsic, method not annotated");
        }

        // Also we need intrinsic to prevent commoning reads from this field
        // across safepoint since GC can change its value.

        // Set up a stream so that appending instructions works properly.
        ciBytecodeStream s(scope->method());
        s.reset_to_bci(0);
        scope_data()->set_stream(&s);
        s.next();

        // setup the initial block state
        _block = start_block;
        _state = start_block->state()->copy_for_parsing();
        _last  = start_block;
        load_local(objectType, 0);

        // Emit the intrinsic node.
        bool result = try_inline_intrinsics(scope->method());
        if (!result) BAILOUT("failed to inline intrinsic");
        method_return(apop());

        // connect the begin and end blocks and we're all done.
        BlockEnd* end = last()->as_BlockEnd();
        block()->set_end(end);
        break;
      }
      // Otherwise, fall thru
    }

  default:
    scope_data()->add_to_work_list(start_block);
    iterate_all_blocks();
    break;
  }
  CHECK_BAILOUT();

  _start = setup_start_block(osr_bci, start_block, _osr_entry, _initial_state);

  eliminate_redundant_phis(_start);

  NOT_PRODUCT(if (PrintValueNumbering && Verbose) print_stats());
  // for osr compile, bailout if some requirements are not fulfilled
  if (osr_bci != -1) {
    BlockBegin* osr_block = blm.bci2block()->at(osr_bci);
    if (!osr_block->is_set(BlockBegin::was_visited_flag)) {
      BAILOUT("osr entry must have been visited for osr compile");
    }

    // check if osr entry point has empty stack - we cannot handle non-empty stacks at osr entry points
    if (!osr_block->state()->stack_is_empty()) {
      BAILOUT("stack not empty at OSR entry point");
    }
  }
#ifndef PRODUCT
  if (PrintCompilation && Verbose) tty->print_cr("Created %d Instructions", _instruction_count);
#endif
}


ValueStack* GraphBuilder::copy_state_before() {
  return copy_state_before_with_bci(bci());
}

ValueStack* GraphBuilder::copy_state_exhandling() {
  return copy_state_exhandling_with_bci(bci());
}

ValueStack* GraphBuilder::copy_state_for_exception() {
  return copy_state_for_exception_with_bci(bci());
}

ValueStack* GraphBuilder::copy_state_before_with_bci(int bci) {
  return state()->copy(ValueStack::StateBefore, bci);
}

ValueStack* GraphBuilder::copy_state_exhandling_with_bci(int bci) {
  if (!has_handler()) return NULL;
  return state()->copy(ValueStack::StateBefore, bci);
}

ValueStack* GraphBuilder::copy_state_for_exception_with_bci(int bci) {
  ValueStack* s = copy_state_exhandling_with_bci(bci);
  if (s == NULL) {
    if (_compilation->env()->should_retain_local_variables()) {
      s = state()->copy(ValueStack::ExceptionState, bci);
    } else {
      s = state()->copy(ValueStack::EmptyExceptionState, bci);
    }
  }
  return s;
}

int GraphBuilder::recursive_inline_level(ciMethod* cur_callee) const {
  int recur_level = 0;
  for (IRScope* s = scope(); s != NULL; s = s->caller()) {
    if (s->method() == cur_callee) {
      ++recur_level;
    }
  }
  return recur_level;
}


bool GraphBuilder::try_inline(ciMethod* callee, bool holder_known, bool ignore_return, Bytecodes::Code bc, Value receiver) {
  const char* msg = NULL;

  // clear out any existing inline bailout condition
  clear_inline_bailout();

  // exclude methods we don't want to inline
  msg = should_not_inline(callee);
  if (msg != NULL) {
    print_inlining(callee, msg, /*success*/ false);
    return false;
  }

  // method handle invokes
  if (callee->is_method_handle_intrinsic()) {
    if (try_method_handle_inline(callee, ignore_return)) {
      if (callee->has_reserved_stack_access()) {
        compilation()->set_has_reserved_stack_access(true);
      }
      return true;
    }
    return false;
  }

  // handle intrinsics
  if (callee->intrinsic_id() != vmIntrinsics::_none &&
      callee->check_intrinsic_candidate()) {
    if (try_inline_intrinsics(callee, ignore_return)) {
      print_inlining(callee, "intrinsic");
      if (callee->has_reserved_stack_access()) {
        compilation()->set_has_reserved_stack_access(true);
      }
      return true;
    }
    // try normal inlining
  }

  // certain methods cannot be parsed at all
  msg = check_can_parse(callee);
  if (msg != NULL) {
    print_inlining(callee, msg, /*success*/ false);
    return false;
  }

  // If bytecode not set use the current one.
  if (bc == Bytecodes::_illegal) {
    bc = code();
  }
  if (try_inline_full(callee, holder_known, ignore_return, bc, receiver)) {
    if (callee->has_reserved_stack_access()) {
      compilation()->set_has_reserved_stack_access(true);
    }
    return true;
  }

  // Entire compilation could fail during try_inline_full call.
  // In that case printing inlining decision info is useless.
  if (!bailed_out())
    print_inlining(callee, _inline_bailout_msg, /*success*/ false);

  return false;
}


const char* GraphBuilder::check_can_parse(ciMethod* callee) const {
  // Certain methods cannot be parsed at all:
  if ( callee->is_native())            return "native method";
  if ( callee->is_abstract())          return "abstract method";
  if (!callee->can_be_parsed())        return "cannot be parsed";
  return NULL;
}

// negative filter: should callee NOT be inlined?  returns NULL, ok to inline, or rejection msg
const char* GraphBuilder::should_not_inline(ciMethod* callee) const {
  if ( compilation()->directive()->should_not_inline(callee)) return "disallowed by CompileCommand";
  if ( callee->dont_inline())          return "don't inline by annotation";
  return NULL;
}

void GraphBuilder::build_graph_for_intrinsic(ciMethod* callee, bool ignore_return) {
  vmIntrinsics::ID id = callee->intrinsic_id();
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");

  // Some intrinsics need special IR nodes.
  switch(id) {
  case vmIntrinsics::_getReference           : append_unsafe_get(callee, T_OBJECT,  false); return;
  case vmIntrinsics::_getBoolean             : append_unsafe_get(callee, T_BOOLEAN, false); return;
  case vmIntrinsics::_getByte                : append_unsafe_get(callee, T_BYTE,    false); return;
  case vmIntrinsics::_getShort               : append_unsafe_get(callee, T_SHORT,   false); return;
  case vmIntrinsics::_getChar                : append_unsafe_get(callee, T_CHAR,    false); return;
  case vmIntrinsics::_getInt                 : append_unsafe_get(callee, T_INT,     false); return;
  case vmIntrinsics::_getLong                : append_unsafe_get(callee, T_LONG,    false); return;
  case vmIntrinsics::_getFloat               : append_unsafe_get(callee, T_FLOAT,   false); return;
  case vmIntrinsics::_getDouble              : append_unsafe_get(callee, T_DOUBLE,  false); return;
  case vmIntrinsics::_putReference           : append_unsafe_put(callee, T_OBJECT,  false); return;
  case vmIntrinsics::_putBoolean             : append_unsafe_put(callee, T_BOOLEAN, false); return;
  case vmIntrinsics::_putByte                : append_unsafe_put(callee, T_BYTE,    false); return;
  case vmIntrinsics::_putShort               : append_unsafe_put(callee, T_SHORT,   false); return;
  case vmIntrinsics::_putChar                : append_unsafe_put(callee, T_CHAR,    false); return;
  case vmIntrinsics::_putInt                 : append_unsafe_put(callee, T_INT,     false); return;
  case vmIntrinsics::_putLong                : append_unsafe_put(callee, T_LONG,    false); return;
  case vmIntrinsics::_putFloat               : append_unsafe_put(callee, T_FLOAT,   false); return;
  case vmIntrinsics::_putDouble              : append_unsafe_put(callee, T_DOUBLE,  false); return;
  case vmIntrinsics::_getShortUnaligned      : append_unsafe_get(callee, T_SHORT,   false); return;
  case vmIntrinsics::_getCharUnaligned       : append_unsafe_get(callee, T_CHAR,    false); return;
  case vmIntrinsics::_getIntUnaligned        : append_unsafe_get(callee, T_INT,     false); return;
  case vmIntrinsics::_getLongUnaligned       : append_unsafe_get(callee, T_LONG,    false); return;
  case vmIntrinsics::_putShortUnaligned      : append_unsafe_put(callee, T_SHORT,   false); return;
  case vmIntrinsics::_putCharUnaligned       : append_unsafe_put(callee, T_CHAR,    false); return;
  case vmIntrinsics::_putIntUnaligned        : append_unsafe_put(callee, T_INT,     false); return;
  case vmIntrinsics::_putLongUnaligned       : append_unsafe_put(callee, T_LONG,    false); return;
  case vmIntrinsics::_getReferenceVolatile   : append_unsafe_get(callee, T_OBJECT,  true); return;
  case vmIntrinsics::_getBooleanVolatile     : append_unsafe_get(callee, T_BOOLEAN, true); return;
  case vmIntrinsics::_getByteVolatile        : append_unsafe_get(callee, T_BYTE,    true); return;
  case vmIntrinsics::_getShortVolatile       : append_unsafe_get(callee, T_SHORT,   true); return;
  case vmIntrinsics::_getCharVolatile        : append_unsafe_get(callee, T_CHAR,    true); return;
  case vmIntrinsics::_getIntVolatile         : append_unsafe_get(callee, T_INT,     true); return;
  case vmIntrinsics::_getLongVolatile        : append_unsafe_get(callee, T_LONG,    true); return;
  case vmIntrinsics::_getFloatVolatile       : append_unsafe_get(callee, T_FLOAT,   true); return;
  case vmIntrinsics::_getDoubleVolatile      : append_unsafe_get(callee, T_DOUBLE,  true); return;
  case vmIntrinsics::_putReferenceVolatile   : append_unsafe_put(callee, T_OBJECT,  true); return;
  case vmIntrinsics::_putBooleanVolatile     : append_unsafe_put(callee, T_BOOLEAN, true); return;
  case vmIntrinsics::_putByteVolatile        : append_unsafe_put(callee, T_BYTE,    true); return;
  case vmIntrinsics::_putShortVolatile       : append_unsafe_put(callee, T_SHORT,   true); return;
  case vmIntrinsics::_putCharVolatile        : append_unsafe_put(callee, T_CHAR,    true); return;
  case vmIntrinsics::_putIntVolatile         : append_unsafe_put(callee, T_INT,     true); return;
  case vmIntrinsics::_putLongVolatile        : append_unsafe_put(callee, T_LONG,    true); return;
  case vmIntrinsics::_putFloatVolatile       : append_unsafe_put(callee, T_FLOAT,   true); return;
  case vmIntrinsics::_putDoubleVolatile      : append_unsafe_put(callee, T_DOUBLE,  true); return;
  case vmIntrinsics::_compareAndSetLong:
  case vmIntrinsics::_compareAndSetInt:
  case vmIntrinsics::_compareAndSetReference : append_unsafe_CAS(callee); return;
  case vmIntrinsics::_getAndAddInt:
  case vmIntrinsics::_getAndAddLong          : append_unsafe_get_and_set(callee, true); return;
  case vmIntrinsics::_getAndSetInt           :
  case vmIntrinsics::_getAndSetLong          :
  case vmIntrinsics::_getAndSetReference     : append_unsafe_get_and_set(callee, false); return;
  case vmIntrinsics::_getCharStringU         : append_char_access(callee, false); return;
  case vmIntrinsics::_putCharStringU         : append_char_access(callee, true); return;
  default:
    break;
  }

  // create intrinsic node
  const bool has_receiver = !callee->is_static();
  ValueType* result_type = as_ValueType(callee->return_type());
  ValueStack* state_before = copy_state_for_exception();

  Values* args = state()->pop_arguments(callee->arg_size());

  if (is_profiling()) {
    // Don't profile in the special case where the root method
    // is the intrinsic
    if (callee != method()) {
      // Note that we'd collect profile data in this method if we wanted it.
      compilation()->set_would_profile(true);
      if (profile_calls()) {
        Value recv = NULL;
        if (has_receiver) {
          recv = args->at(0);
          null_check(recv);
        }
        profile_call(callee, recv, NULL, collect_args_for_profiling(args, callee, true), true);
      }
    }
  }

  Intrinsic* result = new Intrinsic(result_type, callee->intrinsic_id(),
                                    args, has_receiver, state_before,
                                    vmIntrinsics::preserves_state(id),
                                    vmIntrinsics::can_trap(id));
  // append instruction & push result
  Value value = append_split(result);
  if (result_type != voidType && !ignore_return) {
    push(result_type, value);
  }

  if (callee != method() && profile_return() && result_type->is_object_kind()) {
    profile_return_type(result, callee);
  }
}

bool GraphBuilder::try_inline_intrinsics(ciMethod* callee, bool ignore_return) {
  // For calling is_intrinsic_available we need to transition to
  // the '_thread_in_vm' state because is_intrinsic_available()
  // accesses critical VM-internal data.
  bool is_available = false;
  {
    VM_ENTRY_MARK;
    methodHandle mh(THREAD, callee->get_Method());
    is_available = _compilation->compiler()->is_intrinsic_available(mh, _compilation->directive());
  }

  if (!is_available) {
    if (!InlineNatives) {
      // Return false and also set message that the inlining of
      // intrinsics has been disabled in general.
      INLINE_BAILOUT("intrinsic method inlining disabled");
    } else {
      return false;
    }
  }
  build_graph_for_intrinsic(callee, ignore_return);
  return true;
}


bool GraphBuilder::try_inline_jsr(int jsr_dest_bci) {
  // Introduce a new callee continuation point - all Ret instructions
  // will be replaced with Gotos to this point.
  BlockBegin* cont = block_at(next_bci());
  assert(cont != NULL, "continuation must exist (BlockListBuilder starts a new block after a jsr");

  // Note: can not assign state to continuation yet, as we have to
  // pick up the state from the Ret instructions.

  // Push callee scope
  push_scope_for_jsr(cont, jsr_dest_bci);

  // Temporarily set up bytecode stream so we can append instructions
  // (only using the bci of this stream)
  scope_data()->set_stream(scope_data()->parent()->stream());

  BlockBegin* jsr_start_block = block_at(jsr_dest_bci);
  assert(jsr_start_block != NULL, "jsr start block must exist");
  assert(!jsr_start_block->is_set(BlockBegin::was_visited_flag), "should not have visited jsr yet");
  Goto* goto_sub = new Goto(jsr_start_block, false);
  // Must copy state to avoid wrong sharing when parsing bytecodes
  assert(jsr_start_block->state() == NULL, "should have fresh jsr starting block");
  jsr_start_block->set_state(copy_state_before_with_bci(jsr_dest_bci));
  append(goto_sub);
  _block->set_end(goto_sub);
  _last = _block = jsr_start_block;

  // Clear out bytecode stream
  scope_data()->set_stream(NULL);

  scope_data()->add_to_work_list(jsr_start_block);

  // Ready to resume parsing in subroutine
  iterate_all_blocks();

  // If we bailed out during parsing, return immediately (this is bad news)
  CHECK_BAILOUT_(false);

  // Detect whether the continuation can actually be reached. If not,
  // it has not had state set by the join() operations in
  // iterate_bytecodes_for_block()/ret() and we should not touch the
  // iteration state. The calling activation of
  // iterate_bytecodes_for_block will then complete normally.
  if (cont->state() != NULL) {
    if (!cont->is_set(BlockBegin::was_visited_flag)) {
      // add continuation to work list instead of parsing it immediately
      scope_data()->parent()->add_to_work_list(cont);
    }
  }

  assert(jsr_continuation() == cont, "continuation must not have changed");
  assert(!jsr_continuation()->is_set(BlockBegin::was_visited_flag) ||
         jsr_continuation()->is_set(BlockBegin::parser_loop_header_flag),
         "continuation can only be visited in case of backward branches");
  assert(_last && _last->as_BlockEnd(), "block must have end");

  // continuation is in work list, so end iteration of current block
  _skip_block = true;
  pop_scope_for_jsr();

  return true;
}


// Inline the entry of a synchronized method as a monitor enter and
// register the exception handler which releases the monitor if an
// exception is thrown within the callee. Note that the monitor enter
// cannot throw an exception itself, because the receiver is
// guaranteed to be non-null by the explicit null check at the
// beginning of inlining.
void GraphBuilder::inline_sync_entry(Value lock, BlockBegin* sync_handler) {
  assert(lock != NULL && sync_handler != NULL, "lock or handler missing");

  monitorenter(lock, SynchronizationEntryBCI);
  assert(_last->as_MonitorEnter() != NULL, "monitor enter expected");
  _last->set_needs_null_check(false);

  sync_handler->set(BlockBegin::exception_entry_flag);
  sync_handler->set(BlockBegin::is_on_work_list_flag);

  ciExceptionHandler* desc = new ciExceptionHandler(method()->holder(), 0, method()->code_size(), -1, 0);
  XHandler* h = new XHandler(desc);
  h->set_entry_block(sync_handler);
  scope_data()->xhandlers()->append(h);
  scope_data()->set_has_handler();
}


// If an exception is thrown and not handled within an inlined
// synchronized method, the monitor must be released before the
// exception is rethrown in the outer scope. Generate the appropriate
// instructions here.
void GraphBuilder::fill_sync_handler(Value lock, BlockBegin* sync_handler, bool default_handler) {
  BlockBegin* orig_block = _block;
  ValueStack* orig_state = _state;
  Instruction* orig_last = _last;
  _last = _block = sync_handler;
  _state = sync_handler->state()->copy();

  assert(sync_handler != NULL, "handler missing");
  assert(!sync_handler->is_set(BlockBegin::was_visited_flag), "is visited here");

  assert(lock != NULL || default_handler, "lock or handler missing");

  XHandler* h = scope_data()->xhandlers()->remove_last();
  assert(h->entry_block() == sync_handler, "corrupt list of handlers");

  block()->set(BlockBegin::was_visited_flag);
  Value exception = append_with_bci(new ExceptionObject(), SynchronizationEntryBCI);
  assert(exception->is_pinned(), "must be");

  int bci = SynchronizationEntryBCI;
  if (compilation()->env()->dtrace_method_probes()) {
    // Report exit from inline methods.  We don't have a stream here
    // so pass an explicit bci of SynchronizationEntryBCI.
    Values* args = new Values(1);
    args->push(append_with_bci(new Constant(new MethodConstant(method())), bci));
    append_with_bci(new RuntimeCall(voidType, "dtrace_method_exit", CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit), args), bci);
  }

  if (lock) {
    assert(state()->locks_size() > 0 && state()->lock_at(state()->locks_size() - 1) == lock, "lock is missing");
    if (!lock->is_linked()) {
      lock = append_with_bci(lock, bci);
    }

    // exit the monitor in the context of the synchronized method
    monitorexit(lock, bci);

    // exit the context of the synchronized method
    if (!default_handler) {
      pop_scope();
      bci = _state->caller_state()->bci();
      _state = _state->caller_state()->copy_for_parsing();
    }
  }

  // perform the throw as if at the the call site
  apush(exception);
  throw_op(bci);

  BlockEnd* end = last()->as_BlockEnd();
  block()->set_end(end);

  _block = orig_block;
  _state = orig_state;
  _last = orig_last;
}


bool GraphBuilder::try_inline_full(ciMethod* callee, bool holder_known, bool ignore_return, Bytecodes::Code bc, Value receiver) {
  assert(!callee->is_native(), "callee must not be native");
  if (CompilationPolicy::should_not_inline(compilation()->env(), callee)) {
    INLINE_BAILOUT("inlining prohibited by policy");
  }
  // first perform tests of things it's not possible to inline
  if (callee->has_exception_handlers() &&
      !InlineMethodsWithExceptionHandlers) INLINE_BAILOUT("callee has exception handlers");
  if (callee->is_synchronized() &&
      !InlineSynchronizedMethods         ) INLINE_BAILOUT("callee is synchronized");
  if (!callee->holder()->is_linked())      INLINE_BAILOUT("callee's klass not linked yet");
  if (bc == Bytecodes::_invokestatic &&
      !callee->holder()->is_initialized()) INLINE_BAILOUT("callee's klass not initialized yet");
  if (!callee->has_balanced_monitors())    INLINE_BAILOUT("callee's monitors do not match");

  // Proper inlining of methods with jsrs requires a little more work.
  if (callee->has_jsrs()                 ) INLINE_BAILOUT("jsrs not handled properly by inliner yet");

  if (is_profiling() && !callee->ensure_method_data()) {
    INLINE_BAILOUT("mdo allocation failed");
  }

  const bool is_invokedynamic = (bc == Bytecodes::_invokedynamic);
  const bool has_receiver = (bc != Bytecodes::_invokestatic && !is_invokedynamic);

  const int args_base = state()->stack_size() - callee->arg_size();
  assert(args_base >= 0, "stack underflow during inlining");

  Value recv = NULL;
  if (has_receiver) {
    assert(!callee->is_static(), "callee must not be static");
    assert(callee->arg_size() > 0, "must have at least a receiver");

    recv = state()->stack_at(args_base);
    if (recv->is_null_obj()) {
      INLINE_BAILOUT("receiver is always null");
    }
  }

  // now perform tests that are based on flag settings
  bool inlinee_by_directive = compilation()->directive()->should_inline(callee);
  if (callee->force_inline() || inlinee_by_directive) {
    if (inline_level() > MaxForceInlineLevel                      ) INLINE_BAILOUT("MaxForceInlineLevel");
    if (recursive_inline_level(callee) > C1MaxRecursiveInlineLevel) INLINE_BAILOUT("recursive inlining too deep");

    const char* msg = "";
    if (callee->force_inline())  msg = "force inline by annotation";
    if (inlinee_by_directive)    msg = "force inline by CompileCommand";
    print_inlining(callee, msg);
  } else {
    // use heuristic controls on inlining
    if (inline_level() > C1MaxInlineLevel                       ) INLINE_BAILOUT("inlining too deep");
    int callee_recursive_level = recursive_inline_level(callee);
    if (callee_recursive_level > C1MaxRecursiveInlineLevel      ) INLINE_BAILOUT("recursive inlining too deep");
    if (callee->code_size_for_inlining() > max_inline_size()    ) INLINE_BAILOUT("callee is too large");
    // Additional condition to limit stack usage for non-recursive calls.
    if ((callee_recursive_level == 0) &&
        (callee->max_stack() + callee->max_locals() - callee->size_of_parameters() > C1InlineStackLimit)) {
      INLINE_BAILOUT("callee uses too much stack");
    }

    // don't inline throwable methods unless the inlining tree is rooted in a throwable class
    if (callee->name() == ciSymbols::object_initializer_name() &&
        callee->holder()->is_subclass_of(ciEnv::current()->Throwable_klass())) {
      // Throwable constructor call
      IRScope* top = scope();
      while (top->caller() != NULL) {
        top = top->caller();
      }
      if (!top->method()->holder()->is_subclass_of(ciEnv::current()->Throwable_klass())) {
        INLINE_BAILOUT("don't inline Throwable constructors");
      }
    }

    if (compilation()->env()->num_inlined_bytecodes() > DesiredMethodLimit) {
      INLINE_BAILOUT("total inlining greater than DesiredMethodLimit");
    }
    // printing
    print_inlining(callee, "inline", /*success*/ true);
  }

  assert(bc != Bytecodes::_invokestatic || callee->holder()->is_initialized(), "required");

  // NOTE: Bailouts from this point on, which occur at the
  // GraphBuilder level, do not cause bailout just of the inlining but
  // in fact of the entire compilation.

  BlockBegin* orig_block = block();

  // Insert null check if necessary
  if (has_receiver) {
    // note: null check must happen even if first instruction of callee does
    //       an implicit null check since the callee is in a different scope
    //       and we must make sure exception handling does the right thing
    null_check(recv);
  }

  if (is_profiling()) {
    // Note that we'd collect profile data in this method if we wanted it.
    // this may be redundant here...
    compilation()->set_would_profile(true);

    if (profile_calls()) {
      int start = 0;
      Values* obj_args = args_list_for_profiling(callee, start, has_receiver);
      if (obj_args != NULL) {
        int s = obj_args->max_length();
        // if called through method handle invoke, some arguments may have been popped
        for (int i = args_base+start, j = 0; j < obj_args->max_length() && i < state()->stack_size(); ) {
          Value v = state()->stack_at_inc(i);
          if (v->type()->is_object_kind()) {
            obj_args->push(v);
            j++;
          }
        }
        check_args_for_profiling(obj_args, s);
      }
      profile_call(callee, recv, holder_known ? callee->holder() : NULL, obj_args, true);
    }
  }

  // Introduce a new callee continuation point - if the callee has
  // more than one return instruction or the return does not allow
  // fall-through of control flow, all return instructions of the
  // callee will need to be replaced by Goto's pointing to this
  // continuation point.
  BlockBegin* cont = block_at(next_bci());
  bool continuation_existed = true;
  if (cont == NULL) {
    cont = new BlockBegin(next_bci());
    // low number so that continuation gets parsed as early as possible
    cont->set_depth_first_number(0);
    if (PrintInitialBlockList) {
      tty->print_cr("CFG: created block %d (bci %d) as continuation for inline at bci %d",
                    cont->block_id(), cont->bci(), bci());
    }
    continuation_existed = false;
  }
  // Record number of predecessors of continuation block before
  // inlining, to detect if inlined method has edges to its
  // continuation after inlining.
  int continuation_preds = cont->number_of_preds();

  // Push callee scope
  push_scope(callee, cont);

  // the BlockListBuilder for the callee could have bailed out
  if (bailed_out())
      return false;

  // Temporarily set up bytecode stream so we can append instructions
  // (only using the bci of this stream)
  scope_data()->set_stream(scope_data()->parent()->stream());

  // Pass parameters into callee state: add assignments
  // note: this will also ensure that all arguments are computed before being passed
  ValueStack* callee_state = state();
  ValueStack* caller_state = state()->caller_state();
  for (int i = args_base; i < caller_state->stack_size(); ) {
    const int arg_no = i - args_base;
    Value arg = caller_state->stack_at_inc(i);
    store_local(callee_state, arg, arg_no);
  }

  // Remove args from stack.
  // Note that we preserve locals state in case we can use it later
  // (see use of pop_scope() below)
  caller_state->truncate_stack(args_base);
  assert(callee_state->stack_size() == 0, "callee stack must be empty");

  Value lock = NULL;
  BlockBegin* sync_handler = NULL;

  // Inline the locking of the receiver if the callee is synchronized
  if (callee->is_synchronized()) {
    lock = callee->is_static() ? append(new Constant(new InstanceConstant(callee->holder()->java_mirror())))
                               : state()->local_at(0);
    sync_handler = new BlockBegin(SynchronizationEntryBCI);
    inline_sync_entry(lock, sync_handler);
  }

  if (compilation()->env()->dtrace_method_probes()) {
    Values* args = new Values(1);
    args->push(append(new Constant(new MethodConstant(method()))));
    append(new RuntimeCall(voidType, "dtrace_method_entry", CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_entry), args));
  }

  if (profile_inlined_calls()) {
    profile_invocation(callee, copy_state_before_with_bci(SynchronizationEntryBCI));
  }

  BlockBegin* callee_start_block = block_at(0);
  if (callee_start_block != NULL) {
    assert(callee_start_block->is_set(BlockBegin::parser_loop_header_flag), "must be loop header");
    Goto* goto_callee = new Goto(callee_start_block, false);
    // The state for this goto is in the scope of the callee, so use
    // the entry bci for the callee instead of the call site bci.
    append_with_bci(goto_callee, 0);
    _block->set_end(goto_callee);
    callee_start_block->merge(callee_state);

    _last = _block = callee_start_block;

    scope_data()->add_to_work_list(callee_start_block);
  }

  // Clear out bytecode stream
  scope_data()->set_stream(NULL);
  scope_data()->set_ignore_return(ignore_return);

  CompileLog* log = compilation()->log();
  if (log != NULL) log->head("parse method='%d'", log->identify(callee));

  // Ready to resume parsing in callee (either in the same block we
  // were in before or in the callee's start block)
  iterate_all_blocks(callee_start_block == NULL);

  if (log != NULL) log->done("parse");

  // If we bailed out during parsing, return immediately (this is bad news)
  if (bailed_out())
      return false;

  // iterate_all_blocks theoretically traverses in random order; in
  // practice, we have only traversed the continuation if we are
  // inlining into a subroutine
  assert(continuation_existed ||
         !continuation()->is_set(BlockBegin::was_visited_flag),
         "continuation should not have been parsed yet if we created it");

  // At this point we are almost ready to return and resume parsing of
  // the caller back in the GraphBuilder. The only thing we want to do
  // first is an optimization: during parsing of the callee we
  // generated at least one Goto to the continuation block. If we
  // generated exactly one, and if the inlined method spanned exactly
  // one block (and we didn't have to Goto its entry), then we snip
  // off the Goto to the continuation, allowing control to fall
  // through back into the caller block and effectively performing
  // block merging. This allows load elimination and CSE to take place
  // across multiple callee scopes if they are relatively simple, and
  // is currently essential to making inlining profitable.
  if (num_returns() == 1
      && block() == orig_block
      && block() == inline_cleanup_block()) {
    _last  = inline_cleanup_return_prev();
    _state = inline_cleanup_state();
  } else if (continuation_preds == cont->number_of_preds()) {
    // Inlining caused that the instructions after the invoke in the
    // caller are not reachable any more. So skip filling this block
    // with instructions!
    assert(cont == continuation(), "");
    assert(_last && _last->as_BlockEnd(), "");
    _skip_block = true;
  } else {
    // Resume parsing in continuation block unless it was already parsed.
    // Note that if we don't change _last here, iteration in
    // iterate_bytecodes_for_block will stop when we return.
    if (!continuation()->is_set(BlockBegin::was_visited_flag)) {
      // add continuation to work list instead of parsing it immediately
      assert(_last && _last->as_BlockEnd(), "");
      scope_data()->parent()->add_to_work_list(continuation());
      _skip_block = true;
    }
  }

  // Fill the exception handler for synchronized methods with instructions
  if (callee->is_synchronized() && sync_handler->state() != NULL) {
    fill_sync_handler(lock, sync_handler);
  } else {
    pop_scope();
  }

  compilation()->notice_inlined_method(callee);

  return true;
}


bool GraphBuilder::try_method_handle_inline(ciMethod* callee, bool ignore_return) {
  ValueStack* state_before = copy_state_before();
  vmIntrinsics::ID iid = callee->intrinsic_id();
  switch (iid) {
  case vmIntrinsics::_invokeBasic:
    {
      // get MethodHandle receiver
      const int args_base = state()->stack_size() - callee->arg_size();
      ValueType* type = state()->stack_at(args_base)->type();
      if (type->is_constant()) {
        ciMethod* target = type->as_ObjectType()->constant_value()->as_method_handle()->get_vmtarget();
        // We don't do CHA here so only inline static and statically bindable methods.
        if (target->is_static() || target->can_be_statically_bound()) {
          if (ciMethod::is_consistent_info(callee, target)) {
            Bytecodes::Code bc = target->is_static() ? Bytecodes::_invokestatic : Bytecodes::_invokevirtual;
            ignore_return = ignore_return || (callee->return_type()->is_void() && !target->return_type()->is_void());
            if (try_inline(target, /*holder_known*/ !callee->is_static(), ignore_return, bc)) {
              return true;
            }
          } else {
            print_inlining(target, "signatures mismatch", /*success*/ false);
          }
        } else {
          print_inlining(target, "not static or statically bindable", /*success*/ false);
        }
      } else {
        print_inlining(callee, "receiver not constant", /*success*/ false);
      }
    }
    break;

  case vmIntrinsics::_linkToVirtual:
  case vmIntrinsics::_linkToStatic:
  case vmIntrinsics::_linkToSpecial:
  case vmIntrinsics::_linkToInterface:
    {
      // pop MemberName argument
      const int args_base = state()->stack_size() - callee->arg_size();
      ValueType* type = apop()->type();
      if (type->is_constant()) {
        ciMethod* target = type->as_ObjectType()->constant_value()->as_member_name()->get_vmtarget();
        ignore_return = ignore_return || (callee->return_type()->is_void() && !target->return_type()->is_void());
        // If the target is another method handle invoke, try to recursively get
        // a better target.
        if (target->is_method_handle_intrinsic()) {
          if (try_method_handle_inline(target, ignore_return)) {
            return true;
          }
        } else if (!ciMethod::is_consistent_info(callee, target)) {
          print_inlining(target, "signatures mismatch", /*success*/ false);
        } else {
          ciSignature* signature = target->signature();
          const int receiver_skip = target->is_static() ? 0 : 1;
          // Cast receiver to its type.
          if (!target->is_static()) {
            ciKlass* tk = signature->accessing_klass();
            Value obj = state()->stack_at(args_base);
            if (obj->exact_type() == NULL &&
                obj->declared_type() != tk && tk != compilation()->env()->Object_klass()) {
              TypeCast* c = new TypeCast(tk, obj, state_before);
              append(c);
              state()->stack_at_put(args_base, c);
            }
          }
          // Cast reference arguments to its type.
          for (int i = 0, j = 0; i < signature->count(); i++) {
            ciType* t = signature->type_at(i);
            if (t->is_klass()) {
              ciKlass* tk = t->as_klass();
              Value obj = state()->stack_at(args_base + receiver_skip + j);
              if (obj->exact_type() == NULL &&
                  obj->declared_type() != tk && tk != compilation()->env()->Object_klass()) {
                TypeCast* c = new TypeCast(t, obj, state_before);
                append(c);
                state()->stack_at_put(args_base + receiver_skip + j, c);
              }
            }
            j += t->size();  // long and double take two slots
          }
          // We don't do CHA here so only inline static and statically bindable methods.
          if (target->is_static() || target->can_be_statically_bound()) {
            Bytecodes::Code bc = target->is_static() ? Bytecodes::_invokestatic : Bytecodes::_invokevirtual;
            if (try_inline(target, /*holder_known*/ !callee->is_static(), ignore_return, bc)) {
              return true;
            }
          } else {
            print_inlining(target, "not static or statically bindable", /*success*/ false);
          }
        }
      } else {
        print_inlining(callee, "MemberName not constant", /*success*/ false);
      }
    }
    break;

  case vmIntrinsics::_linkToNative:
    break; // TODO: NYI

  default:
    fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
    break;
  }
  set_state(state_before->copy_for_parsing());
  return false;
}


void GraphBuilder::inline_bailout(const char* msg) {
  assert(msg != NULL, "inline bailout msg must exist");
  _inline_bailout_msg = msg;
}


void GraphBuilder::clear_inline_bailout() {
  _inline_bailout_msg = NULL;
}


void GraphBuilder::push_root_scope(IRScope* scope, BlockList* bci2block, BlockBegin* start) {
  ScopeData* data = new ScopeData(NULL);
  data->set_scope(scope);
  data->set_bci2block(bci2block);
  _scope_data = data;
  _block = start;
}


void GraphBuilder::push_scope(ciMethod* callee, BlockBegin* continuation) {
  IRScope* callee_scope = new IRScope(compilation(), scope(), bci(), callee, -1, false);
  scope()->add_callee(callee_scope);

  BlockListBuilder blb(compilation(), callee_scope, -1);
  CHECK_BAILOUT();

  if (!blb.bci2block()->at(0)->is_set(BlockBegin::parser_loop_header_flag)) {
    // this scope can be inlined directly into the caller so remove
    // the block at bci 0.
    blb.bci2block()->at_put(0, NULL);
  }

  set_state(new ValueStack(callee_scope, state()->copy(ValueStack::CallerState, bci())));

  ScopeData* data = new ScopeData(scope_data());
  data->set_scope(callee_scope);
  data->set_bci2block(blb.bci2block());
  data->set_continuation(continuation);
  _scope_data = data;
}


void GraphBuilder::push_scope_for_jsr(BlockBegin* jsr_continuation, int jsr_dest_bci) {
  ScopeData* data = new ScopeData(scope_data());
  data->set_parsing_jsr();
  data->set_jsr_entry_bci(jsr_dest_bci);
  data->set_jsr_return_address_local(-1);
  // Must clone bci2block list as we will be mutating it in order to
  // properly clone all blocks in jsr region as well as exception
  // handlers containing rets
  BlockList* new_bci2block = new BlockList(bci2block()->length());
  new_bci2block->appendAll(bci2block());
  data->set_bci2block(new_bci2block);
  data->set_scope(scope());
  data->setup_jsr_xhandlers();
  data->set_continuation(continuation());
  data->set_jsr_continuation(jsr_continuation);
  _scope_data = data;
}


void GraphBuilder::pop_scope() {
  int number_of_locks = scope()->number_of_locks();
  _scope_data = scope_data()->parent();
  // accumulate minimum number of monitor slots to be reserved
  scope()->set_min_number_of_locks(number_of_locks);
}


void GraphBuilder::pop_scope_for_jsr() {
  _scope_data = scope_data()->parent();
}

void GraphBuilder::append_unsafe_get(ciMethod* callee, BasicType t, bool is_volatile) {
  Values* args = state()->pop_arguments(callee->arg_size());
  null_check(args->at(0));
  Instruction* offset = args->at(2);
#ifndef _LP64
  offset = append(new Convert(Bytecodes::_l2i, offset, as_ValueType(T_INT)));
#endif
  Instruction* op = append(new UnsafeGet(t, args->at(1), offset, is_volatile));
  push(op->type(), op);
  compilation()->set_has_unsafe_access(true);
}


void GraphBuilder::append_unsafe_put(ciMethod* callee, BasicType t, bool is_volatile) {
  Values* args = state()->pop_arguments(callee->arg_size());
  null_check(args->at(0));
  Instruction* offset = args->at(2);
#ifndef _LP64
  offset = append(new Convert(Bytecodes::_l2i, offset, as_ValueType(T_INT)));
#endif
  Value val = args->at(3);
  if (t == T_BOOLEAN) {
    Value mask = append(new Constant(new IntConstant(1)));
    val = append(new LogicOp(Bytecodes::_iand, val, mask));
  }
  Instruction* op = append(new UnsafePut(t, args->at(1), offset, val, is_volatile));
  compilation()->set_has_unsafe_access(true);
  kill_all();
}

void GraphBuilder::append_unsafe_CAS(ciMethod* callee) {
  ValueStack* state_before = copy_state_for_exception();
  ValueType* result_type = as_ValueType(callee->return_type());
  assert(result_type->is_int(), "int result");
  Values* args = state()->pop_arguments(callee->arg_size());

  // Pop off some args to specially handle, then push back
  Value newval = args->pop();
  Value cmpval = args->pop();
  Value offset = args->pop();
  Value src = args->pop();
  Value unsafe_obj = args->pop();

  // Separately handle the unsafe arg. It is not needed for code
  // generation, but must be null checked
  null_check(unsafe_obj);

#ifndef _LP64
  offset = append(new Convert(Bytecodes::_l2i, offset, as_ValueType(T_INT)));
#endif

  args->push(src);
  args->push(offset);
  args->push(cmpval);
  args->push(newval);

  // An unsafe CAS can alias with other field accesses, but we don't
  // know which ones so mark the state as no preserved.  This will
  // cause CSE to invalidate memory across it.
  bool preserves_state = false;
  Intrinsic* result = new Intrinsic(result_type, callee->intrinsic_id(), args, false, state_before, preserves_state);
  append_split(result);
  push(result_type, result);
  compilation()->set_has_unsafe_access(true);
}

void GraphBuilder::append_char_access(ciMethod* callee, bool is_store) {
  // This intrinsic accesses byte[] array as char[] array. Computing the offsets
  // correctly requires matched array shapes.
  assert (arrayOopDesc::base_offset_in_bytes(T_CHAR) == arrayOopDesc::base_offset_in_bytes(T_BYTE),
          "sanity: byte[] and char[] bases agree");
  assert (type2aelembytes(T_CHAR) == type2aelembytes(T_BYTE)*2,
          "sanity: byte[] and char[] scales agree");

  ValueStack* state_before = copy_state_indexed_access();
  compilation()->set_has_access_indexed(true);
  Values* args = state()->pop_arguments(callee->arg_size());
  Value array = args->at(0);
  Value index = args->at(1);
  if (is_store) {
    Value value = args->at(2);
    Instruction* store = append(new StoreIndexed(array, index, NULL, T_CHAR, value, state_before, false, true));
    store->set_flag(Instruction::NeedsRangeCheckFlag, false);
    _memory->store_value(value);
  } else {
    Instruction* load = append(new LoadIndexed(array, index, NULL, T_CHAR, state_before, true));
    load->set_flag(Instruction::NeedsRangeCheckFlag, false);
    push(load->type(), load);
  }
}

void GraphBuilder::print_inlining(ciMethod* callee, const char* msg, bool success) {
  CompileLog* log = compilation()->log();
  if (log != NULL) {
    assert(msg != NULL, "inlining msg should not be null!");
    if (success) {
      log->inline_success(msg);
    } else {
      log->inline_fail(msg);
    }
  }
  EventCompilerInlining event;
  if (event.should_commit()) {
    CompilerEvent::InlineEvent::post(event, compilation()->env()->task()->compile_id(), method()->get_Method(), callee, success, msg, bci());
  }

  CompileTask::print_inlining_ul(callee, scope()->level(), bci(), msg);

  if (!compilation()->directive()->PrintInliningOption) {
    return;
  }
  CompileTask::print_inlining_tty(callee, scope()->level(), bci(), msg);
  if (success && CIPrintMethodCodes) {
    callee->print_codes();
  }
}

void GraphBuilder::append_unsafe_get_and_set(ciMethod* callee, bool is_add) {
  Values* args = state()->pop_arguments(callee->arg_size());
  BasicType t = callee->return_type()->basic_type();
  null_check(args->at(0));
  Instruction* offset = args->at(2);
#ifndef _LP64
  offset = append(new Convert(Bytecodes::_l2i, offset, as_ValueType(T_INT)));
#endif
  Instruction* op = append(new UnsafeGetAndSet(t, args->at(1), offset, args->at(3), is_add));
  compilation()->set_has_unsafe_access(true);
  kill_all();
  push(op->type(), op);
}

#ifndef PRODUCT
void GraphBuilder::print_stats() {
  vmap()->print();
}
#endif // PRODUCT

void GraphBuilder::profile_call(ciMethod* callee, Value recv, ciKlass* known_holder, Values* obj_args, bool inlined) {
  assert(known_holder == NULL || (known_holder->is_instance_klass() &&
                                  (!known_holder->is_interface() ||
                                   ((ciInstanceKlass*)known_holder)->has_nonstatic_concrete_methods())), "should be non-static concrete method");
  if (known_holder != NULL) {
    if (known_holder->exact_klass() == NULL) {
      known_holder = compilation()->cha_exact_type(known_holder);
    }
  }

  append(new ProfileCall(method(), bci(), callee, recv, known_holder, obj_args, inlined));
}

void GraphBuilder::profile_return_type(Value ret, ciMethod* callee, ciMethod* m, int invoke_bci) {
  assert((m == NULL) == (invoke_bci < 0), "invalid method and invalid bci together");
  if (m == NULL) {
    m = method();
  }
  if (invoke_bci < 0) {
    invoke_bci = bci();
  }
  ciMethodData* md = m->method_data_or_null();
  ciProfileData* data = md->bci_to_data(invoke_bci);
  if (data != NULL && (data->is_CallTypeData() || data->is_VirtualCallTypeData())) {
    bool has_return = data->is_CallTypeData() ? ((ciCallTypeData*)data)->has_return() : ((ciVirtualCallTypeData*)data)->has_return();
    if (has_return) {
      append(new ProfileReturnType(m , invoke_bci, callee, ret));
    }
  }
}

void GraphBuilder::profile_invocation(ciMethod* callee, ValueStack* state) {
  append(new ProfileInvoke(callee, state));
}
