/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIMETHODBLOCKS_HPP
#define SHARE_CI_CIMETHODBLOCKS_HPP

#include "ci/ciMethod.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/growableArray.hpp"


class ciBlock;

class ciMethodBlocks : public ResourceObj {
private:
  ciMethod *_method;
  Arena *_arena;
  GrowableArray<ciBlock *>  *_blocks;
  ciBlock  **_bci_to_block;
  int _num_blocks;
  int _code_size;

  void do_analysis();
public:
  ciMethodBlocks(Arena *arena, ciMethod *meth);

  ciBlock *block_containing(int bci);
  ciBlock *block(int index)  { return _blocks->at(index); }
  ciBlock *make_block_at(int bci);
  ciBlock *split_block_at(int bci);
  bool is_block_start(int bci);
  int num_blocks()  { return _num_blocks;}
  void clear_processed();

  ciBlock *make_dummy_block(); // a block not associated with a bci

#ifndef PRODUCT
  void dump();
#endif
};

class ciBlock : public ResourceObj {
private:
  int _idx;
  int _start_bci;
  int _limit_bci;
  int _control_bci;
  uint _flags;
  int _ex_start_bci;
  int _ex_limit_bci;
#ifndef PRODUCT
  ciMethod *_method;
#endif
  enum {
    Processed   = (1 << 0),
    Handler     = (1 << 1),
    MayThrow    = (1 << 2),
    DoesJsr     = (1 << 3),
    DoesRet     = (1 << 4),
    RetTarget   = (1 << 5),
    HasHandler  = (1 << 6)
  };


public:
  enum {
    fall_through_bci = -1
  };

  ciBlock(ciMethod *method, int index, int start_bci);
  int start_bci() const         { return _start_bci; }
  int limit_bci() const         { return _limit_bci; }
  int control_bci() const       { return _control_bci; }
  int index() const             { return _idx; }
  void set_start_bci(int bci)   { _start_bci = bci; }
  void set_limit_bci(int bci)   { _limit_bci = bci; }
  void set_control_bci(int bci) { _control_bci = bci;}
  void set_exception_range(int start_bci, int limit_bci);
  int ex_start_bci() const      { return _ex_start_bci; }
  int ex_limit_bci() const      { return _ex_limit_bci; }
  bool contains(int bci) const { return start_bci() <= bci && bci < limit_bci(); }

  // flag handling
  bool  processed() const           { return (_flags & Processed) != 0; }
  bool  is_handler() const          { return (_flags & Handler) != 0; }
  bool  may_throw() const           { return (_flags & MayThrow) != 0; }
  bool  does_jsr() const            { return (_flags & DoesJsr) != 0; }
  bool  does_ret() const            { return (_flags & DoesRet) != 0; }
  bool  has_handler() const         { return (_flags & HasHandler) != 0; }
  bool  is_ret_target() const       { return (_flags & RetTarget) != 0; }
  void  set_processed()             { _flags |= Processed; }
  void  clear_processed()           { _flags &= ~Processed; }
  void  set_handler()               { _flags |= Handler; }
  void  set_may_throw()             { _flags |= MayThrow; }
  void  set_does_jsr()              { _flags |= DoesJsr; }
  void  clear_does_jsr()            { _flags &= ~DoesJsr; }
  void  set_does_ret()              { _flags |= DoesRet; }
  void  clear_does_ret()            { _flags &= ~DoesRet; }
  void  set_is_ret_target()         { _flags |= RetTarget; }
  void  set_has_handler()           { _flags |= HasHandler; }
  void  clear_exception_handler()   { _flags &= ~Handler; _ex_start_bci = -1; _ex_limit_bci = -1; }
#ifndef PRODUCT
  ciMethod *method() const          { return _method; }
  void dump();
  void print_on(outputStream* st) const  PRODUCT_RETURN;
#endif
};

#endif // SHARE_CI_CIMETHODBLOCKS_HPP
