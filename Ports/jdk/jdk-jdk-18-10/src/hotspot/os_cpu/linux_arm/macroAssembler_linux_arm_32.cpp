/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "runtime/os.hpp"

void MacroAssembler::breakpoint(AsmCondition cond) {
  if (cond == al) {
    emit_int32(0xe7f001f0);
  } else {
    call(CAST_FROM_FN_PTR(address, os::breakpoint), relocInfo::runtime_call_type, cond);
  }
}

// atomic_cas_bool
//
// Perform an atomic compare and exchange and return bool result
//
// inputs:
//         oldval value to compare to
//         newval value to store if *(base+offset) == oldval
//         base   base address of storage location
//         offset offset added to base to form dest address
// output:
//         Z flag is set in success

void MacroAssembler::atomic_cas_bool(Register oldval, Register newval, Register base, int offset, Register tmpreg) {
  if (VM_Version::supports_ldrex()) {
    Register tmp_reg;
    if (tmpreg == noreg) {
      push(LR);
      tmp_reg = LR;
    } else {
      tmp_reg = tmpreg;
    }
    assert_different_registers(tmp_reg, oldval, newval, base);
    Label loop;
    bind(loop);
    ldrex(tmp_reg, Address(base, offset));
    subs(tmp_reg, tmp_reg, oldval);
    strex(tmp_reg, newval, Address(base, offset), eq);
    cmp(tmp_reg, 1, eq);
    b(loop, eq);
    cmp(tmp_reg, 0);
    if (tmpreg == noreg) {
      pop(tmp_reg);
    }
  } else if (VM_Version::supports_kuser_cmpxchg32()) {
    // On armv5 platforms we must use the Linux kernel helper
    // function for atomic cas operations since ldrex/strex is
    // not supported.
    //
    // This is a special routine at a fixed address 0xffff0fc0 with
    // with these arguments and results
    //
    // input:
    //  r0 = oldval, r1 = newval, r2 = ptr, lr = return adress
    // output:
    //  r0 = 0 carry set on success
    //  r0 != 0 carry clear on failure
    //
    // r3, ip and flags are clobbered
    //

    Label loop;

    push(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR));

    Register tmp_reg = LR; // ignore the argument

    assert_different_registers(tmp_reg, oldval, newval, base);

    // Shuffle registers for kernel call
    if (oldval != R0) {
      if (newval == R0) {
        mov(tmp_reg, newval);
        newval = tmp_reg;
      }
      if (base == R0) {
        mov(tmp_reg, base);
        base = tmp_reg;
      }
      mov(R0, oldval);
    }
    if(newval != R1) {
      if(base == R1) {
        if(newval == R2) {
          mov(tmp_reg, base);
          base = tmp_reg;
        }
        else {
          mov(R2, base);
          base = R2;
        }
      }
      mov(R1, newval);
    }
    if (base != R2)
      mov(R2, base);

    if (offset != 0)
      add(R2, R2, offset);

    mvn(R3, 0xf000);
    mov(LR, PC);
    sub(PC, R3, 0x3f);
    cmp (R0, 0);

    pop(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR));
  } else {
    // Should never run on a platform so old that it does not have kernel helper
    stop("Atomic cmpxchg32 unsupported on this platform");
  }
}

// atomic_cas
//
// Perform an atomic compare and exchange and return previous value
//
// inputs:
//         prev temporary register (destroyed)
//         oldval value to compare to
//         newval value to store if *(base+offset) == oldval
//         base   base address of storage location
//         offset offset added to base to form dest address
// output:
//         returns previous value from *(base+offset) in R0

void MacroAssembler::atomic_cas(Register temp1, Register temp2, Register oldval, Register newval, Register base, int offset) {
  if (temp1 != R0) {
    // try to read the previous value directly in R0
    if (temp2 == R0) {
      // R0 declared free
      temp2 = temp1;
      temp1 = R0;
    } else if ((oldval != R0) && (newval != R0) && (base != R0)) {
      // free, and scratched on return
      temp1 = R0;
    }
  }
  if (VM_Version::supports_ldrex()) {
    Label loop;
    assert_different_registers(temp1, temp2, oldval, newval, base);

    bind(loop);
    ldrex(temp1, Address(base, offset));
    cmp(temp1, oldval);
    strex(temp2, newval, Address(base, offset), eq);
    cmp(temp2, 1, eq);
    b(loop, eq);
    if (temp1 != R0) {
      mov(R0, temp1);
    }
  } else if (VM_Version::supports_kuser_cmpxchg32()) {
    // On armv5 platforms we must use the Linux kernel helper
    // function for atomic cas operations since ldrex/strex is
    // not supported.
    //
    // This is a special routine at a fixed address 0xffff0fc0
    //
    // input:
    //  r0 = oldval, r1 = newval, r2 = ptr, lr = return adress
    // output:
    //  r0 = 0 carry set on success
    //  r0 != 0 carry clear on failure
    //
    // r3, ip and flags are clobbered
    //
    Label done;
    Label loop;

    push(RegisterSet(R1, R4) | RegisterSet(R12) | RegisterSet(LR));

    if ( oldval != R0 || newval != R1 || base != R2 ) {
      push(oldval);
      push(newval);
      push(base);
      pop(R2);
      pop(R1);
      pop(R0);
    }

    if (offset != 0) {
      add(R2, R2, offset);
    }

    mov(R4, R0);
    bind(loop);
    ldr(R0, Address(R2));
    cmp(R0, R4);
    b(done, ne);
    mvn(R12, 0xf000);
    mov(LR, PC);
    sub(PC, R12, 0x3f);
    b(loop, cc);
    mov(R0, R4);
    bind(done);

    pop(RegisterSet(R1, R4) | RegisterSet(R12) | RegisterSet(LR));
  } else {
    // Should never run on a platform so old that it does not have kernel helper
    stop("Atomic cmpxchg32 unsupported on this platform");
  }
}

// atomic_cas64
//
// Perform a 64 bit atomic compare and exchange and return previous value
// as well as returning status in 'result' register
//
// inputs:
//         oldval_lo, oldval_hi value to compare to
//         newval_lo, newval_hi value to store if *(base+offset) == oldval
//         base   base address of storage location
//         offset offset added to base to form dest address
// output:
//         memval_lo, memval_hi, result
//         returns previous value from *(base+offset) in memval_lo/hi
//         returns status in result, 1==success, 0==failure
//         C1 just uses status result
//         VM code uses previous value returned in memval_lo/hi

void MacroAssembler::atomic_cas64(Register memval_lo, Register memval_hi, Register result, Register oldval_lo, Register oldval_hi, Register newval_lo, Register newval_hi, Register base, int offset) {
  if (VM_Version::supports_ldrexd()) {
    Label loop;
    assert_different_registers(memval_lo, memval_hi, result, oldval_lo,
                               oldval_hi, newval_lo, newval_hi, base);
    assert(memval_hi == memval_lo + 1 && memval_lo < R9, "cmpxchg_long: illegal registers");
    assert(oldval_hi == oldval_lo + 1 && oldval_lo < R9, "cmpxchg_long: illegal registers");
    assert(newval_hi == newval_lo + 1 && newval_lo < R9, "cmpxchg_long: illegal registers");
    assert(result != R10, "cmpxchg_long: illegal registers");
    assert(base != R10, "cmpxchg_long: illegal registers");

    mov(result, 0);
    bind(loop);
    ldrexd(memval_lo, Address(base, offset));
    cmp(memval_lo, oldval_lo);
    cmp(memval_hi, oldval_hi, eq);
    strexd(result, newval_lo, Address(base, offset), eq);
    rsbs(result, result, 1, eq);
    b(loop, eq);
  } else if (VM_Version::supports_kuser_cmpxchg64()) {
    // On armv5 platforms we must use the Linux kernel helper
    // function for atomic cas64 operations since ldrexd/strexd is
    // not supported.
    //
    // This is a special routine at a fixed address 0xffff0f60
    //
    // input:
    //  r0 = (long long *)oldval, r1 = (long long *)newval,
    //  r2 = ptr, lr = return adress
    // output:
    //  r0 = 0 carry set on success
    //  r0 != 0 carry clear on failure
    //
    // r3, and flags are clobbered
    //
    Label done;
    Label loop;

    if (result != R12) {
      push(R12);
    }
    push(RegisterSet(R10) | RegisterSet(LR));
    mov(R10, SP);         // Save SP

    bic(SP, SP, StackAlignmentInBytes - 1);  // align stack
    push(RegisterSet(oldval_lo, oldval_hi));
    push(RegisterSet(newval_lo, newval_hi));

    if ((offset != 0) || (base != R12)) {
      add(R12, base, offset);
    }
    push(RegisterSet(R0, R3));
    bind(loop);
    ldrd(memval_lo, Address(R12)); //current
    ldrd(oldval_lo, Address(SP, 24));
    cmp(memval_lo, oldval_lo);
    cmp(memval_hi, oldval_hi, eq);
    pop(RegisterSet(R0, R3), ne);
    mov(result, 0, ne);
    b(done, ne);
    // Setup for kernel call
    mov(R2, R12);
    add(R0, SP, 24);            // R0 == &oldval_lo
    add(R1, SP, 16);            // R1 == &newval_lo
    mvn(R3, 0xf000);            // call kernel helper at 0xffff0f60
    mov(LR, PC);
    sub(PC, R3, 0x9f);
    b(loop, cc);                 // if Carry clear then oldval != current
                                 // try again. Otherwise, return oldval
    // Here on success
    pop(RegisterSet(R0, R3));
    mov(result, 1);
    ldrd(memval_lo, Address(SP, 8));
    bind(done);
    pop(RegisterSet(newval_lo, newval_hi));
    pop(RegisterSet(oldval_lo, oldval_hi));
    mov(SP, R10);                 // restore SP
    pop(RegisterSet(R10) | RegisterSet(LR));
    if (result != R12) {
      pop(R12);
    }
  } else {
    stop("Atomic cmpxchg64 unsupported on this platform");
  }
}
