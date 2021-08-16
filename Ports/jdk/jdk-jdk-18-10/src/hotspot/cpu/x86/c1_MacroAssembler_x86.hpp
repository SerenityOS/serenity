/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_C1_MACROASSEMBLER_X86_HPP
#define CPU_X86_C1_MACROASSEMBLER_X86_HPP

// C1_MacroAssembler contains high-level macros for C1

 private:
  int _rsp_offset;    // track rsp changes
  // initialization
  void pd_init() { _rsp_offset = 0; }

 public:
  void try_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );

  void initialize_header(Register obj, Register klass, Register len, Register t1, Register t2);
  void initialize_body(Register obj, Register len_in_bytes, int hdr_size_in_bytes, Register t1);

  // locking
  // hdr     : must be rax, contents destroyed
  // obj     : must point to the object to lock, contents preserved
  // disp_hdr: must point to the displaced header location, contents preserved
  // returns code offset at which to add null check debug information
  int lock_object  (Register swap, Register obj, Register disp_hdr, Label& slow_case);

  // unlocking
  // hdr     : contents destroyed
  // obj     : must point to the object to lock, contents preserved
  // disp_hdr: must be eax & must point to the displaced header location, contents destroyed
  void unlock_object(Register swap, Register obj, Register lock, Label& slow_case);

  void initialize_object(
    Register obj,                      // result: pointer to object after successful allocation
    Register klass,                    // object klass
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    bool     is_tlab_allocated         // the object was allocated in a TLAB; relevant for the implementation of ZeroTLAB
  );

  // allocation of fixed-size objects
  // (can also be used to allocate fixed-size arrays, by setting
  // hdr_size correctly and storing the array length afterwards)
  // obj        : must be rax, will contain pointer to allocated object
  // t1, t2     : scratch registers - contents destroyed
  // header_size: size of object header in words
  // object_size: total size of object in words
  // slow_case  : exit to slow case implementation if fast allocation fails
  void allocate_object(Register obj, Register t1, Register t2, int header_size, int object_size, Register klass, Label& slow_case);

  enum {
    max_array_allocation_length = 0x00FFFFFF
  };

  // allocation of arrays
  // obj        : must be rax, will contain pointer to allocated object
  // len        : array length in number of elements
  // t          : scratch register - contents destroyed
  // header_size: size of object header in words
  // f          : element scale factor
  // slow_case  : exit to slow case implementation if fast allocation fails
  void allocate_array(Register obj, Register len, Register t, Register t2, int header_size, Address::ScaleFactor f, Register klass, Label& slow_case);

  int  rsp_offset() const { return _rsp_offset; }
  void set_rsp_offset(int n) { _rsp_offset = n; }

  // Note: NEVER push values directly, but only through following push_xxx functions;
  //       This helps us to track the rsp changes compared to the entry rsp (->_rsp_offset)

  void push_jint (jint i)     { _rsp_offset++; push(i); }
  void push_oop  (jobject o)  { _rsp_offset++; pushoop(o); }
  // Seems to always be in wordSize
  void push_addr (Address a)  { _rsp_offset++; pushptr(a); }
  void push_reg  (Register r) { _rsp_offset++; push(r); }
  void pop_reg   (Register r) { _rsp_offset--; pop(r); assert(_rsp_offset >= 0, "stack offset underflow"); }

  void dec_stack (int nof_words) {
    _rsp_offset -= nof_words;
    assert(_rsp_offset >= 0, "stack offset underflow");
    addptr(rsp, wordSize * nof_words);
  }

  void dec_stack_after_call (int nof_words) {
    _rsp_offset -= nof_words;
    assert(_rsp_offset >= 0, "stack offset underflow");
  }

  void invalidate_registers(bool inv_rax, bool inv_rbx, bool inv_rcx, bool inv_rdx, bool inv_rsi, bool inv_rdi) PRODUCT_RETURN;

  // This platform only uses signal-based null checks. The Label is not needed.
  void null_check(Register r, Label *Lnull = NULL) { MacroAssembler::null_check(r); }

  void load_parameter(int offset_in_words, Register reg);

  void save_live_registers_no_oop_map(bool save_fpu_registers);
  void restore_live_registers_except_rax(bool restore_fpu_registers);
  void restore_live_registers(bool restore_fpu_registers);

#endif // CPU_X86_C1_MACROASSEMBLER_X86_HPP
