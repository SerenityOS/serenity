/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_STUBROUTINES_PPC_HPP
#define CPU_PPC_STUBROUTINES_PPC_HPP

// This file holds the platform specific parts of the StubRoutines
// definition. See stubRoutines.hpp for a description on how to
// extend it.

static bool returns_to_call_stub(address return_pc) { return return_pc == _call_stub_return_address; }

enum platform_dependent_constants {
  code_size1 = 20000,          // simply increase if too small (assembler will crash if too small)
  code_size2 = 24000           // simply increase if too small (assembler will crash if too small)
};

// CRC32 Intrinsics.
#define CRC32_TABLE_SIZE (4 * 256)
#define REVERSE_CRC32_POLY  0xEDB88320
#define REVERSE_CRC32C_POLY 0x82F63B78
#define INVERSE_REVERSE_CRC32_POLY  0x1aab14226ull
#define INVERSE_REVERSE_CRC32C_POLY 0x105fd79bdull
#define CRC32_UNROLL_FACTOR 2048
#define CRC32_UNROLL_FACTOR2 8

class ppc {
  friend class StubGenerator;

 private:
  static address _nmethod_entry_barrier;

 public:
  static address nmethod_entry_barrier();

  static address generate_crc_constants(juint reverse_poly);
};

#endif // CPU_PPC_STUBROUTINES_PPC_HPP
