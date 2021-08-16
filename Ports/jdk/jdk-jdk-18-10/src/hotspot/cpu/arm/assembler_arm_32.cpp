/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "ci/ciEnv.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/hashtable.hpp"
#include "utilities/macros.hpp"

#ifdef COMPILER2
// Convert the raw encoding form into the form expected by the
// constructor for Address.
Address Address::make_raw(int base, int index, int scale, int disp, relocInfo::relocType disp_reloc) {
  RelocationHolder rspec;
  if (disp_reloc != relocInfo::none) {
    rspec = Relocation::spec_simple(disp_reloc);
  }

  Register rindex = as_Register(index);
  if (rindex != PC) {
    assert(disp == 0, "unsupported");
    Address madr(as_Register(base), rindex, lsl, scale);
    madr._rspec = rspec;
    return madr;
  } else {
    assert(scale == 0, "not supported");
    Address madr(as_Register(base), disp);
    madr._rspec = rspec;
    return madr;
  }
}
#endif

void AsmOperand::initialize_rotated_imm(unsigned int imm) {
  for (int shift = 2; shift <= 24; shift += 2) {
    if ((imm & ~(0xff << shift)) == 0) {
      _encoding = 1 << 25 | (32 - shift) << 7 | imm >> shift;
      return;
    }
  }
  assert((imm & 0x0ffffff0) == 0, "too complicated constant: %d (%x)", imm, imm);
  _encoding = 1 << 25 | 4 << 7 | imm >> 28 | imm << 4;
}

bool AsmOperand::is_rotated_imm(unsigned int imm) {
  if ((imm >> 8) == 0) {
    return true;
  }
  for (int shift = 2; shift <= 24; shift += 2) {
    if ((imm & ~(0xff << shift)) == 0) {
      return true;
    }
  }
  if ((imm & 0x0ffffff0) == 0) {
    return true;
  }
  return false;
}
