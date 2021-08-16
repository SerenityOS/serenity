/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_C1_MACROASSEMBLER_PPC_HPP
#define CPU_PPC_C1_MACROASSEMBLER_PPC_HPP

  void pd_init() { /* nothing to do */ }

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
  void initialize_body(Register base, Register index);
  void initialize_body(Register obj, Register tmp1, Register tmp2, int obj_size_in_bytes, int hdr_size_in_bytes);

  // locking/unlocking
  void lock_object  (Register Rmark, Register Roop, Register Rbox, Register Rscratch, Label& slow_case);
  void unlock_object(Register Rmark, Register Roop, Register Rbox,                    Label& slow_case);

  void initialize_object(
    Register obj,                      // result: pointer to object after successful allocation
    Register klass,                    // object klass
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2                        // temp register
  );

  // Allocation of fixed-size objects
  // (Can also be used to allocate fixed-size arrays, by setting
  // hdr_size correctly and storing the array length afterwards.)
  void allocate_object(
    Register obj,                      // result: pointer to object after successful allocation
    Register t1,                       // temp register
    Register t2,                       // temp register
    Register t3,                       // temp register
    int      hdr_size,                 // object header size in words
    int      obj_size,                 // object size in words
    Register klass,                    // object klass
    Label&   slow_case                 // continuation point if fast allocation fails
  );

  enum {
    max_array_allocation_length = 0x40000000 // ppc friendly value, requires lis only
  };

  // Allocation of arrays
  void allocate_array(
    Register obj,                      // result: pointer to array after successful allocation
    Register len,                      // array length
    Register t1,                       // temp register
    Register t2,                       // temp register
    Register t3,                       // temp register
    int      hdr_size,                 // object header size in words
    int      elt_size,                 // element size in bytes
    Register klass,                    // object klass
    Label&   slow_case                 // continuation point if fast allocation fails
  );

  void null_check(Register r, Label *Lnull = NULL);

  address call_c_with_frame_resize(address dest, int frame_resize);

#endif // CPU_PPC_C1_MACROASSEMBLER_PPC_HPP
