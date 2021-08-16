/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciMethodBlocks.hpp"
#include "ci/ciStreams.hpp"
#include "interpreter/bytecode.hpp"
#include "utilities/copy.hpp"

// ciMethodBlocks



ciBlock *ciMethodBlocks::block_containing(int bci) {
  ciBlock *blk = _bci_to_block[bci];
  return blk;
}

bool ciMethodBlocks::is_block_start(int bci) {
  assert(bci >=0 && bci < _code_size, "valid bytecode range");
  ciBlock *b = _bci_to_block[bci];
  assert(b != NULL, "must have block for bytecode");
  return b->start_bci() == bci;
}

// ------------------------------------------------------------------
// ciMethodBlocks::split_block_at
//
// Split the block spanning bci into two separate ranges.  The former
// block becomes the second half and a new range is created for the
// first half.  Returns the range beginning at bci.
ciBlock *ciMethodBlocks::split_block_at(int bci) {
  ciBlock *former_block = block_containing(bci);
  ciBlock *new_block = new(_arena) ciBlock(_method, _num_blocks++, former_block->start_bci());
  _blocks->append(new_block);
  assert(former_block != NULL, "must not be NULL");
  new_block->set_limit_bci(bci);
  former_block->set_start_bci(bci);
  for (int pos=bci-1; pos >= 0; pos--) {
    ciBlock *current_block = block_containing(pos);
    if (current_block == former_block) {
      // Replace it.
      _bci_to_block[pos] = new_block;
    } else if (current_block == NULL) {
      // Non-bytecode start.  Skip.
      continue;
    } else {
      // We are done with our backwards walk
      break;
    }
  }
  // Move an exception handler information if needed.
  if (former_block->is_handler()) {
    int ex_start = former_block->ex_start_bci();
    int ex_end = former_block->ex_limit_bci();
    new_block->set_exception_range(ex_start, ex_end);
    // Clear information in former_block.
    former_block->clear_exception_handler();
  }
  return former_block;
}

ciBlock *ciMethodBlocks::make_block_at(int bci) {
  ciBlock *cb = block_containing(bci);
  if (cb == NULL ) {
    // This is our first time visiting this bytecode.  Create
    // a fresh block and assign it this starting point.
    ciBlock *nb = new(_arena) ciBlock(_method, _num_blocks++, bci);
    _blocks->append(nb);
     _bci_to_block[bci] = nb;
    return nb;
  } else if (cb->start_bci() == bci) {
    // The block begins at bci.  Simply return it.
    return cb;
  } else {
    // We have already created a block containing bci but
    // not starting at bci.  This existing block needs to
    // be split into two.
    return split_block_at(bci);
  }
}

ciBlock *ciMethodBlocks::make_dummy_block() {
  ciBlock *dum = new(_arena) ciBlock(_method, -1, 0);
  return dum;
}

void ciMethodBlocks::do_analysis() {
  ciBytecodeStream s(_method);
  ciBlock *cur_block = block_containing(0);
  int limit_bci = _method->code_size();

  while (s.next() != ciBytecodeStream::EOBC()) {
    int bci = s.cur_bci();
    // Determine if a new block has been made at the current bci.  If
    // this block differs from our current range, switch to the new
    // one and end the old one.
    assert(cur_block != NULL, "must always have a current block");
    ciBlock *new_block = block_containing(bci);
    if (new_block == NULL || new_block == cur_block) {
      // We have not marked this bci as the start of a new block.
      // Keep interpreting the current_range.
      _bci_to_block[bci] = cur_block;
    } else {
      cur_block->set_limit_bci(bci);
      cur_block = new_block;
    }

    switch (s.cur_bc()) {
      case Bytecodes::_ifeq        :
      case Bytecodes::_ifne        :
      case Bytecodes::_iflt        :
      case Bytecodes::_ifge        :
      case Bytecodes::_ifgt        :
      case Bytecodes::_ifle        :
      case Bytecodes::_if_icmpeq   :
      case Bytecodes::_if_icmpne   :
      case Bytecodes::_if_icmplt   :
      case Bytecodes::_if_icmpge   :
      case Bytecodes::_if_icmpgt   :
      case Bytecodes::_if_icmple   :
      case Bytecodes::_if_acmpeq   :
      case Bytecodes::_if_acmpne   :
      case Bytecodes::_ifnull      :
      case Bytecodes::_ifnonnull   :
      {
        cur_block->set_control_bci(bci);
        ciBlock *fall_through = make_block_at(s.next_bci());
        int dest_bci = s.get_dest();
        ciBlock *dest = make_block_at(dest_bci);
        break;
      }

      case Bytecodes::_goto        :
      {
        cur_block->set_control_bci(bci);
        if (s.next_bci() < limit_bci) {
          (void) make_block_at(s.next_bci());
        }
        int dest_bci = s.get_dest();
        ciBlock *dest = make_block_at(dest_bci);
        break;
      }

      case Bytecodes::_jsr         :
      {
        cur_block->set_control_bci(bci);
        ciBlock *ret = make_block_at(s.next_bci());
        int dest_bci = s.get_dest();
        ciBlock *dest = make_block_at(dest_bci);
        break;
      }

      case Bytecodes::_tableswitch :
        {
          cur_block->set_control_bci(bci);
          Bytecode_tableswitch sw(&s);
          int len = sw.length();
          ciBlock *dest;
          int dest_bci;
          for (int i = 0; i < len; i++) {
            dest_bci = s.cur_bci() + sw.dest_offset_at(i);
            dest = make_block_at(dest_bci);
          }
          dest_bci = s.cur_bci() + sw.default_offset();
          make_block_at(dest_bci);
          if (s.next_bci() < limit_bci) {
            dest = make_block_at(s.next_bci());
          }
        }
        break;

      case Bytecodes::_lookupswitch:
        {
          cur_block->set_control_bci(bci);
          Bytecode_lookupswitch sw(&s);
          int len = sw.number_of_pairs();
          ciBlock *dest;
          int dest_bci;
          for (int i = 0; i < len; i++) {
            dest_bci = s.cur_bci() + sw.pair_at(i).offset();
            dest = make_block_at(dest_bci);
          }
          dest_bci = s.cur_bci() + sw.default_offset();
          dest = make_block_at(dest_bci);
          if (s.next_bci() < limit_bci) {
            dest = make_block_at(s.next_bci());
          }
        }
        break;

      case Bytecodes::_goto_w      :
      {
        cur_block->set_control_bci(bci);
        if (s.next_bci() < limit_bci) {
          (void) make_block_at(s.next_bci());
        }
        int dest_bci = s.get_far_dest();
        ciBlock *dest = make_block_at(dest_bci);
        break;
      }

      case Bytecodes::_jsr_w       :
      {
        cur_block->set_control_bci(bci);
        ciBlock *ret = make_block_at(s.next_bci());
        int dest_bci = s.get_far_dest();
        ciBlock *dest = make_block_at(dest_bci);
        break;
      }

      case Bytecodes::_athrow      :
        cur_block->set_may_throw();
        // fall-through
      case Bytecodes::_ret         :
      case Bytecodes::_ireturn     :
      case Bytecodes::_lreturn     :
      case Bytecodes::_freturn     :
      case Bytecodes::_dreturn     :
      case Bytecodes::_areturn     :
      case Bytecodes::_return      :
        cur_block->set_control_bci(bci);
        if (s.next_bci() < limit_bci) {
          (void) make_block_at(s.next_bci());
        }
        break;

      default:
        break;
    }
  }
  //  End the last block
  cur_block->set_limit_bci(limit_bci);
}

ciMethodBlocks::ciMethodBlocks(Arena *arena, ciMethod *meth): _method(meth),
                          _arena(arena), _num_blocks(0), _code_size(meth->code_size()) {
  int block_estimate = _code_size / 8;

  _blocks =  new(_arena) GrowableArray<ciBlock *>(_arena, block_estimate, 0, NULL);
  int b2bsize = _code_size * sizeof(ciBlock **);
  _bci_to_block = (ciBlock **) arena->Amalloc(b2bsize);
  Copy::zero_to_words((HeapWord*) _bci_to_block, b2bsize / sizeof(HeapWord));

  // create initial block covering the entire method
  ciBlock *b = new(arena) ciBlock(_method, _num_blocks++, 0);
  _blocks->append(b);
  _bci_to_block[0] = b;

  // create blocks for exception handlers
  if (meth->has_exception_handlers()) {
    for(ciExceptionHandlerStream str(meth); !str.is_done(); str.next()) {
      ciExceptionHandler* handler = str.handler();
      ciBlock *eb = make_block_at(handler->handler_bci());
      //
      // Several exception handlers can have the same handler_bci:
      //
      //  try {
      //    if (a.foo(b) < 0) {
      //      return a.error();
      //    }
      //    return CoderResult.UNDERFLOW;
      //  } finally {
      //      a.position(b);
      //  }
      //
      //  The try block above is divided into 2 exception blocks
      //  separated by 'areturn' bci.
      //
      int ex_start = handler->start();
      int ex_end = handler->limit();
      // ensure a block at the start of exception range and start of following code
      (void) make_block_at(ex_start);
      if (ex_end < _code_size)
        (void) make_block_at(ex_end);

      if (eb->is_handler()) {
        // Extend old handler exception range to cover additional range.
        int old_ex_start = eb->ex_start_bci();
        int old_ex_end   = eb->ex_limit_bci();
        if (ex_start > old_ex_start)
          ex_start = old_ex_start;
        if (ex_end < old_ex_end)
          ex_end = old_ex_end;
        eb->clear_exception_handler(); // Reset exception information
      }
      eb->set_exception_range(ex_start, ex_end);
    }
  }

  // scan the bytecodes and identify blocks
  do_analysis();

  // mark blocks that have exception handlers
  if (meth->has_exception_handlers()) {
    for(ciExceptionHandlerStream str(meth); !str.is_done(); str.next()) {
      ciExceptionHandler* handler = str.handler();
      int ex_start = handler->start();
      int ex_end = handler->limit();

      int bci = ex_start;
      while (bci < ex_end) {
        ciBlock *b = block_containing(bci);
        b->set_has_handler();
        bci = b->limit_bci();
      }
    }
  }
}

void ciMethodBlocks::clear_processed() {
  for (int i = 0; i < _blocks->length(); i++)
    _blocks->at(i)->clear_processed();
}

#ifndef PRODUCT
void ciMethodBlocks::dump() {
  tty->print("---- blocks for method: ");
  _method->print();
  tty->cr();
  for (int i = 0; i < _blocks->length(); i++) {
    tty->print("  B%d: ", i); _blocks->at(i)->dump();
  }
}
#endif

ciBlock::ciBlock(ciMethod *method, int index, int start_bci) :
                         _idx(index), _start_bci(start_bci), _limit_bci(-1), _control_bci(fall_through_bci),
                         _flags(0), _ex_start_bci(-1), _ex_limit_bci(-1)
#ifndef PRODUCT
                         , _method(method)
#endif
{
}

void ciBlock::set_exception_range(int start_bci, int limit_bci)  {
   assert(limit_bci >= start_bci, "valid range");
   assert(!is_handler() && _ex_start_bci == -1 && _ex_limit_bci == -1, "must not be handler");
   _ex_start_bci = start_bci;
   _ex_limit_bci = limit_bci;
   set_handler();
}

#ifndef PRODUCT
static const char *flagnames[] = {
  "Processed",
  "Handler",
  "MayThrow",
  "Jsr",
  "Ret",
  "RetTarget",
  "HasHandler",
};

void ciBlock::dump() {
  tty->print(" [%d .. %d), {", _start_bci, _limit_bci);
  for (int i = 0; i < 7; i++) {
    if ((_flags & (1 << i)) != 0) {
      tty->print(" %s", flagnames[i]);
    }
  }
  tty->print(" ]");
  if (is_handler())
    tty->print(" handles(%d..%d)", _ex_start_bci, _ex_limit_bci);
  tty->cr();
}

// ------------------------------------------------------------------
// ciBlock::print_on
void ciBlock::print_on(outputStream* st) const {
  st->print_cr("--------------------------------------------------------");
  st->print   ("ciBlock [%d - %d) control : ", start_bci(), limit_bci());
  if (control_bci() == fall_through_bci) {
    st->print_cr("%d:fall through", limit_bci());
  } else {
    st->print_cr("%d:%s", control_bci(),
        Bytecodes::name(method()->java_code_at_bci(control_bci())));
  }

  if (Verbose || WizardMode) {
    method()->print_codes_on(start_bci(), limit_bci(), st);
  }
}
#endif
