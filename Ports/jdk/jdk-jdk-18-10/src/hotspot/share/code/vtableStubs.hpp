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

#ifndef SHARE_CODE_VTABLESTUBS_HPP
#define SHARE_CODE_VTABLESTUBS_HPP

#include "asm/macroAssembler.hpp"
#include "code/vmreg.hpp"
#include "memory/allocation.hpp"

// A VtableStub holds an individual code stub for a pair (vtable index, #args) for either itables or vtables
// There's a one-to-one relationship between a VtableStub and such a pair.

// A word on VtableStub sizing:
//   Such a vtable/itable stub consists of the instance data
//   and an immediately following CodeBuffer.
//   Unfortunately, the required space for the code buffer varies, depending on
//   the setting of compile time macros (PRODUCT, ASSERT, ...) and of command line
//   parameters. Actual data may have an influence on the size as well.
//
//   A simple approximation for the VtableStub size would be to just take a value
//   "large enough" for all circumstances - a worst case estimate.
//   As there can exist many stubs - and they never go away - we certainly don't
//   want to waste more code cache space than absolutely necessary.
//
//   We need a different approach which, as far as possible, should be independent
//   from or adaptive to code size variations. These variations may be caused by
//   changed compile time or run time switches as well as by changed emitter code.
//
//   Here is the idea:
//   For the first stub we generate, we allocate a "large enough" code buffer.
//   Once all instructions are emitted, we know the actual size of the stub.
//   Remembering that size allows us to allocate a tightly matching code buffer
//   for all subsequent stubs. That covers all "static variance", i.e. all variance
//   that is due to compile time macros, command line parameters, machine capabilities,
//   and other influences which are immutable for the life span of the vm.
//
//   Life isn't always that easy. Code size may depend on actual data, "load constant"
//   being an example for that. All code segments with such "dynamic variance" require
//   additional care. We need to know or estimate the worst case code size for each
//   such segment. With that knowledge, we can maintain a "slop counter" in the
//   platform-specific stub emitters. It accumulates the difference between worst-case
//   and actual code size. When the stub is fully generated, the actual stub size is
//   adjusted (increased) by the slop counter value.
//
//   As a result, we allocate all but the first code buffers with the same, tightly matching size.
//

// VtableStubs creates the code stubs for compiled calls through vtables.
// There is one stub per (vtable index, args_size) pair, and the stubs are
// never deallocated. They don't need to be GCed because they contain no oops.
class VtableStub;

class VtableStubs : AllStatic {
 public:                                         // N must be public (some compilers need this for _table)
  enum {
    N    = 256,                                  // size of stub table; must be power of two
    mask = N - 1
  };

 private:
  friend class VtableStub;
  static VtableStub* _table[N];                  // table of existing stubs
  static int         _number_of_vtable_stubs;    // number of stubs created so far (for statistics)
  static int         _vtab_stub_size;            // current size estimate for vtable stub (quasi-constant)
  static int         _itab_stub_size;            // current size estimate for itable stub (quasi-constant)

  static VtableStub* create_vtable_stub(int vtable_index);
  static VtableStub* create_itable_stub(int vtable_index);
  static VtableStub* lookup            (bool is_vtable_stub, int vtable_index);
  static void        enter             (bool is_vtable_stub, int vtable_index, VtableStub* s);
  static inline uint hash              (bool is_vtable_stub, int vtable_index);
  static address     find_stub         (bool is_vtable_stub, int vtable_index);
  static void        bookkeeping(MacroAssembler* masm, outputStream* out, VtableStub* s,
                                 address npe_addr, address ame_addr,   bool is_vtable_stub,
                                 int     index,    int     slop_bytes, int  index_dependent_slop);
  static int         code_size_limit(bool is_vtable_stub);
  static void        check_and_set_size_limit(bool is_vtable_stub,
                                              int   code_size,
                                              int   padding);

 public:
  static address     find_vtable_stub(int vtable_index) { return find_stub(true,  vtable_index); }
  static address     find_itable_stub(int itable_index) { return find_stub(false, itable_index); }

  static VtableStub* entry_point(address pc);                        // vtable stub entry point for a pc
  static bool        contains(address pc);                           // is pc within any stub?
  static VtableStub* stub_containing(address pc);                    // stub containing pc or NULL
  static int         number_of_vtable_stubs() { return _number_of_vtable_stubs; }
  static void        initialize();
  static void        vtable_stub_do(void f(VtableStub*));            // iterates over all vtable stubs
};


class VtableStub {
 private:
  friend class VtableStubs;

  static address _chunk;             // For allocation
  static address _chunk_end;         // For allocation
  static VMReg   _receiver_location; // Where to find receiver

  VtableStub*    _next;              // Pointer to next entry in hash table
  const short    _index;             // vtable index
  short          _ame_offset;        // Where an AbstractMethodError might occur
  short          _npe_offset;        // Where a NullPointerException might occur
  bool           _is_vtable_stub;    // True if vtable stub, false, is itable stub
  /* code follows here */            // The vtableStub code

  void* operator new(size_t size, int code_size) throw();

  VtableStub(bool is_vtable_stub, int index)
        : _next(NULL), _index(index), _ame_offset(-1), _npe_offset(-1),
          _is_vtable_stub(is_vtable_stub) {}
  VtableStub* next() const                       { return _next; }
  int index() const                              { return _index; }
  static VMReg receiver_location()               { return _receiver_location; }
  void set_next(VtableStub* n)                   { _next = n; }

 public:
  address code_begin() const                     { return (address)(this + 1); }
  address code_end() const                       { return code_begin() + VtableStubs::code_size_limit(_is_vtable_stub); }
  address entry_point() const                    { return code_begin(); }
  static int entry_offset()                      { return sizeof(class VtableStub); }

  bool matches(bool is_vtable_stub, int index) const {
    return _index == index && _is_vtable_stub == is_vtable_stub;
  }
  bool contains(address pc) const                { return code_begin() <= pc && pc < code_end(); }

 private:
  void set_exception_points(address npe_addr, address ame_addr) {
    _npe_offset = npe_addr - code_begin();
    _ame_offset = ame_addr - code_begin();
    assert(is_abstract_method_error(ame_addr),   "offset must be correct");
    assert(is_null_pointer_exception(npe_addr),  "offset must be correct");
    assert(!is_abstract_method_error(npe_addr),  "offset must be correct");
    assert(!is_null_pointer_exception(ame_addr), "offset must be correct");
  }

  // platform-dependent routines
  static int  pd_code_alignment();
  // CNC: Removed because vtable stubs are now made with an ideal graph
  // static bool pd_disregard_arg_size();

  static void align_chunk() {
    uintptr_t off = (uintptr_t)( _chunk + sizeof(VtableStub) ) % pd_code_alignment();
    if (off != 0)  _chunk += pd_code_alignment() - off;
  }

 public:
  // Query
  bool is_itable_stub()                          { return !_is_vtable_stub; }
  bool is_vtable_stub()                          { return  _is_vtable_stub; }
  bool is_abstract_method_error(address epc)     { return epc == code_begin()+_ame_offset; }
  bool is_null_pointer_exception(address epc)    { return epc == code_begin()+_npe_offset; }

  void print_on(outputStream* st) const;
  void print() const;

};

#endif // SHARE_CODE_VTABLESTUBS_HPP
