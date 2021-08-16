/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_EXCEPTIONHANDLERTABLE_HPP
#define SHARE_CODE_EXCEPTIONHANDLERTABLE_HPP

#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "utilities/align.hpp"

// A HandlerTableEntry describes an individual entry of a subtable
// of ExceptionHandlerTable. An entry consists of a pair(bci, pco),
// where bci is the exception handler bci, and pco is the pc offset
// relative to the nmethod code start for the compiled exception
// handler corresponding to the (interpreted) exception handler
// starting at bci.
//
// The first HandlerTableEntry of each subtable holds the length
// and catch_pco for the subtable (the length is the number of
// subtable entries w/o header).

class HandlerTableEntry {
 private:
  int _bci;
  int _pco;
  int _scope_depth;

 public:
  HandlerTableEntry(int bci, int pco, int scope_depth) {
    assert( 0 <= pco, "pco must be positive");
    assert( 0 <= scope_depth, "scope_depth must be positive");
    _bci = bci;
    _pco = pco;
    _scope_depth = scope_depth;
  }

  int len() const { return _bci; } // for entry at subtable begin
  int bci() const { return _bci; }
  int pco() const { return _pco; }
  int scope_depth() const { return _scope_depth; }
};


// An ExceptionHandlerTable is an abstraction over a list of subtables
// of exception handlers for CatchNodes. Each subtable has a one-entry
// header holding length and catch_pco of the subtable, followed
// by 'length' entries for each exception handler that can be reached
// from the corresponding CatchNode. The catch_pco is the pc offset of
// the CatchNode in the corresponding nmethod. Empty subtables are dis-
// carded.
//
// Structure of the table:
//
// table    = { subtable }.
// subtable = header entry { entry }.
// header   = a pair (number of subtable entries, catch pc offset, [unused])
// entry    = a pair (handler bci, handler pc offset, scope depth)
//
// An ExceptionHandlerTable can be created from scratch, in which case
// it is possible to add subtables. It can also be created from an
// nmethod (for lookup purposes) in which case the table cannot be
// modified.

class nmethod;
class ExceptionHandlerTable {
 private:
  HandlerTableEntry* _table;    // the table
  int                _length;   // the current length of the table
  int                _size;     // the number of allocated entries
  ReallocMark        _nesting;  // assertion check for reallocations

 public:
  // add the entry & grow the table if needed
  void add_entry(HandlerTableEntry entry);
  HandlerTableEntry* subtable_for(int catch_pco) const;

  // (compile-time) construction within compiler
  ExceptionHandlerTable(int initial_size = 8);

  // (run-time) construction from nmethod
  ExceptionHandlerTable(const CompiledMethod* nm);

  // (compile-time) add entries
  void add_subtable(
    int                 catch_pco, // the pc offset for the CatchNode
    GrowableArray<intptr_t>* handler_bcis, // the exception handler entry point bcis
    GrowableArray<intptr_t>* scope_depths_from_top_scope,
                                           // if representing exception handlers in multiple
                                           // inlined scopes, indicates which scope relative to
                                           // the youngest/innermost one in which we are performing
                                           // the lookup; zero (or null GrowableArray) indicates
                                           // innermost scope
    GrowableArray<intptr_t>* handler_pcos  // pc offsets for the compiled handlers
  );

  // nmethod support
  int  size_in_bytes() const { return align_up(_length * (int)sizeof(HandlerTableEntry), oopSize); }
  void copy_to(CompiledMethod* nm);
  void copy_bytes_to(address addr);

  // lookup
  HandlerTableEntry* entry_for(int catch_pco, int handler_bci, int scope_depth) const;

  // debugging
  void print_subtable(HandlerTableEntry* t, address base = NULL) const;
  void print(address base = NULL) const;
  void print_subtable_for(int catch_pco) const;
};


// ----------------------------------------------------------------------------
// Implicit null exception tables.  Maps an exception PC offset to a
// continuation PC offset.  During construction it's a variable sized
// array with a max size and current length.  When stored inside an
// nmethod a zero length table takes no space.  This is detected by
// nul_chk_table_size() == 0.  Otherwise the table has a length word
// followed by pairs of <excp-offset, const-offset>.

// Use 32-bit representation for offsets
typedef  uint              implicit_null_entry;

class ImplicitExceptionTable {
  uint _size;
  uint _len;
  implicit_null_entry *_data;
  implicit_null_entry *adr( uint idx ) const { return &_data[2*idx]; }
  ReallocMark          _nesting;  // assertion check for reallocations

public:
  ImplicitExceptionTable( ) :  _size(0), _len(0), _data(0) { }
  // (run-time) construction from nmethod
  ImplicitExceptionTable( const CompiledMethod *nm );

  void set_size( uint size );
  void append( uint exec_off, uint cont_off );

#if INCLUDE_JVMCI
  void add_deoptimize(uint exec_off) {
    // Use the same offset as a marker value for deoptimization
    append(exec_off, exec_off);
  }
#endif

  // Returns the offset to continue execution at.  If the returned
  // value equals exec_off then the dispatch is expected to be a
  // deoptimization instead.
  uint continuation_offset( uint exec_off ) const;

  uint len() const { return _len; }

  uint get_exec_offset(uint i) { assert(i < _len, "oob"); return *adr(i); }
  uint get_cont_offset(uint i) { assert(i < _len, "oob"); return *(adr(i) + 1); }

  int size_in_bytes() const { return len() == 0 ? 0 : ((2 * len() + 1) * sizeof(implicit_null_entry)); }

  void copy_to(nmethod* nm);
  void copy_bytes_to(address addr, int size);
  void print(address base) const;
  void verify(nmethod *nm) const;
};

#endif // SHARE_CODE_EXCEPTIONHANDLERTABLE_HPP
