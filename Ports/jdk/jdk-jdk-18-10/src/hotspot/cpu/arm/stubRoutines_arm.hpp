/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_STUBROUTINES_ARM_HPP
#define CPU_ARM_STUBROUTINES_ARM_HPP

// This file holds the platform specific parts of the StubRoutines
// definition. See stubRoutines.hpp for a description on how to
// extend it.

enum platform_dependent_constants {
  code_size1 =  9000,           // simply increase if too small (assembler will crash if too small)
  code_size2 = 22000            // simply increase if too small (assembler will crash if too small)
};

class Arm {
 friend class StubGenerator;
 friend class VMStructs;

 private:

  static address _idiv_irem_entry;
  static address _partial_subtype_check;

 public:

  static address idiv_irem_entry() { return _idiv_irem_entry; }
  static address partial_subtype_check() { return _partial_subtype_check; }
};

  static bool returns_to_call_stub(address return_pc) {
    return return_pc == _call_stub_return_address;
  }

  static address _atomic_load_long_entry;
  static address _atomic_store_long_entry;

  static address atomic_load_long_entry()                  { return _atomic_load_long_entry; }
  static address atomic_store_long_entry()                 { return _atomic_store_long_entry; }


#endif // CPU_ARM_STUBROUTINES_ARM_HPP
