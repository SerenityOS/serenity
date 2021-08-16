/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
 */

#include "precompiled.hpp"

#if defined(AARCH64) && !defined(ZERO)

#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "compiler/disassembler.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"

#define __ _masm.

static void asm_check(const unsigned int *insns, const unsigned int *insns1, size_t len) {
  bool ok = true;
  for (unsigned int i = 0; i < len; i++) {
    if (insns[i] != insns1[i]) {
      ResourceMark rm;
      stringStream ss;
      ss.print_cr("Ours:");
      Disassembler::decode((address)&insns1[i], (address)&insns1[i+1], &ss);
      ss.print_cr("Theirs:");
      Disassembler::decode((address)&insns[i], (address)&insns[i+1], &ss);

      EXPECT_EQ(insns[i], insns1[i]) << ss.as_string();
    }
  }
}

TEST_VM(AssemblerAArch64, validate) {
  // Smoke test for assembler
  BufferBlob* b = BufferBlob::create("aarch64Test", 500000);
  CodeBuffer code(b);
  Assembler _masm(&code);
  address entry = __ pc();

  // python aarch64-asmtest.py | expand > asmtest.out.h
#include "asmtest.out.h"

  asm_check((unsigned int *)entry, insns, sizeof insns / sizeof insns[0]);

  {
    address PC = __ pc();
    __ ld1(v0, __ T16B, Address(r16));      // No offset
    __ ld1(v0, __ T8H, __ post(r16, 16));   // Post-index
    __ ld2(v0, v1, __ T8H, __ post(r24, 16 * 2));   // Post-index
    __ ld1(v0, __ T16B, __ post(r16, r17)); // Register post-index
    static const unsigned int vector_insns[] = {
       0x4c407200, // ld1   {v0.16b}, [x16]
       0x4cdf7600, // ld1   {v0.8h}, [x16], #16
       0x4cdf8700, // ld2   {v0.8h, v1.8h}, [x24], #32
       0x4cd17200, // ld1   {v0.16b}, [x16], x17
      };
    asm_check((unsigned int *)PC, vector_insns,
              sizeof vector_insns / sizeof vector_insns[0]);
  }

  BufferBlob::free(b);
}

#endif  // AARCH64
