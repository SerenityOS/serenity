/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2021, Red Hat Inc. All rights reserved.
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

#include <sys/types.h>

#include "precompiled.hpp"
#include "jvm.h"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "compiler/compileTask.hpp"
#include "compiler/disassembler.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "nativeInst_aarch64.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "utilities/powerOfTwo.hpp"
#ifdef COMPILER1
#include "c1/c1_LIRAssembler.hpp"
#endif
#ifdef COMPILER2
#include "oops/oop.hpp"
#include "opto/compile.hpp"
#include "opto/node.hpp"
#include "opto/output.hpp"
#endif

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) block_comment(str)
#endif
#define STOP(str) stop(str);
#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

// Patch any kind of instruction; there may be several instructions.
// Return the total length (in bytes) of the instructions.
int MacroAssembler::pd_patch_instruction_size(address branch, address target) {
  int instructions = 1;
  assert((uint64_t)target < (1ull << 48), "48-bit overflow in address constant");
  intptr_t offset = (target - branch) >> 2;
  unsigned insn = *(unsigned*)branch;
  if ((Instruction_aarch64::extract(insn, 29, 24) & 0b111011) == 0b011000) {
    // Load register (literal)
    Instruction_aarch64::spatch(branch, 23, 5, offset);
  } else if (Instruction_aarch64::extract(insn, 30, 26) == 0b00101) {
    // Unconditional branch (immediate)
    Instruction_aarch64::spatch(branch, 25, 0, offset);
  } else if (Instruction_aarch64::extract(insn, 31, 25) == 0b0101010) {
    // Conditional branch (immediate)
    Instruction_aarch64::spatch(branch, 23, 5, offset);
  } else if (Instruction_aarch64::extract(insn, 30, 25) == 0b011010) {
    // Compare & branch (immediate)
    Instruction_aarch64::spatch(branch, 23, 5, offset);
  } else if (Instruction_aarch64::extract(insn, 30, 25) == 0b011011) {
    // Test & branch (immediate)
    Instruction_aarch64::spatch(branch, 18, 5, offset);
  } else if (Instruction_aarch64::extract(insn, 28, 24) == 0b10000) {
    // PC-rel. addressing
    offset = target-branch;
    int shift = Instruction_aarch64::extract(insn, 31, 31);
    if (shift) {
      uint64_t dest = (uint64_t)target;
      uint64_t pc_page = (uint64_t)branch >> 12;
      uint64_t adr_page = (uint64_t)target >> 12;
      unsigned offset_lo = dest & 0xfff;
      offset = adr_page - pc_page;

      // We handle 4 types of PC relative addressing
      //   1 - adrp    Rx, target_page
      //       ldr/str Ry, [Rx, #offset_in_page]
      //   2 - adrp    Rx, target_page
      //       add     Ry, Rx, #offset_in_page
      //   3 - adrp    Rx, target_page (page aligned reloc, offset == 0)
      //       movk    Rx, #imm16<<32
      //   4 - adrp    Rx, target_page (page aligned reloc, offset == 0)
      // In the first 3 cases we must check that Rx is the same in the adrp and the
      // subsequent ldr/str, add or movk instruction. Otherwise we could accidentally end
      // up treating a type 4 relocation as a type 1, 2 or 3 just because it happened
      // to be followed by a random unrelated ldr/str, add or movk instruction.
      //
      unsigned insn2 = ((unsigned*)branch)[1];
      if (Instruction_aarch64::extract(insn2, 29, 24) == 0b111001 &&
                Instruction_aarch64::extract(insn, 4, 0) ==
                        Instruction_aarch64::extract(insn2, 9, 5)) {
        // Load/store register (unsigned immediate)
        unsigned size = Instruction_aarch64::extract(insn2, 31, 30);
        Instruction_aarch64::patch(branch + sizeof (unsigned),
                                    21, 10, offset_lo >> size);
        guarantee(((dest >> size) << size) == dest, "misaligned target");
        instructions = 2;
      } else if (Instruction_aarch64::extract(insn2, 31, 22) == 0b1001000100 &&
                Instruction_aarch64::extract(insn, 4, 0) ==
                        Instruction_aarch64::extract(insn2, 4, 0)) {
        // add (immediate)
        Instruction_aarch64::patch(branch + sizeof (unsigned),
                                   21, 10, offset_lo);
        instructions = 2;
      } else if (Instruction_aarch64::extract(insn2, 31, 21) == 0b11110010110 &&
                   Instruction_aarch64::extract(insn, 4, 0) ==
                     Instruction_aarch64::extract(insn2, 4, 0)) {
        // movk #imm16<<32
        Instruction_aarch64::patch(branch + 4, 20, 5, (uint64_t)target >> 32);
        uintptr_t dest = ((uintptr_t)target & 0xffffffffULL) | ((uintptr_t)branch & 0xffff00000000ULL);
        uintptr_t pc_page = (uintptr_t)branch >> 12;
        uintptr_t adr_page = (uintptr_t)dest >> 12;
        offset = adr_page - pc_page;
        instructions = 2;
      }
    }
    int offset_lo = offset & 3;
    offset >>= 2;
    Instruction_aarch64::spatch(branch, 23, 5, offset);
    Instruction_aarch64::patch(branch, 30, 29, offset_lo);
  } else if (Instruction_aarch64::extract(insn, 31, 21) == 0b11010010100) {
    uint64_t dest = (uint64_t)target;
    // Move wide constant
    assert(nativeInstruction_at(branch+4)->is_movk(), "wrong insns in patch");
    assert(nativeInstruction_at(branch+8)->is_movk(), "wrong insns in patch");
    Instruction_aarch64::patch(branch, 20, 5, dest & 0xffff);
    Instruction_aarch64::patch(branch+4, 20, 5, (dest >>= 16) & 0xffff);
    Instruction_aarch64::patch(branch+8, 20, 5, (dest >>= 16) & 0xffff);
    assert(target_addr_for_insn(branch) == target, "should be");
    instructions = 3;
  } else if (Instruction_aarch64::extract(insn, 31, 22) == 0b1011100101 &&
             Instruction_aarch64::extract(insn, 4, 0) == 0b11111) {
    // nothing to do
    assert(target == 0, "did not expect to relocate target for polling page load");
  } else {
    ShouldNotReachHere();
  }
  return instructions * NativeInstruction::instruction_size;
}

int MacroAssembler::patch_oop(address insn_addr, address o) {
  int instructions;
  unsigned insn = *(unsigned*)insn_addr;
  assert(nativeInstruction_at(insn_addr+4)->is_movk(), "wrong insns in patch");

  // OOPs are either narrow (32 bits) or wide (48 bits).  We encode
  // narrow OOPs by setting the upper 16 bits in the first
  // instruction.
  if (Instruction_aarch64::extract(insn, 31, 21) == 0b11010010101) {
    // Move narrow OOP
    uint32_t n = CompressedOops::narrow_oop_value(cast_to_oop(o));
    Instruction_aarch64::patch(insn_addr, 20, 5, n >> 16);
    Instruction_aarch64::patch(insn_addr+4, 20, 5, n & 0xffff);
    instructions = 2;
  } else {
    // Move wide OOP
    assert(nativeInstruction_at(insn_addr+8)->is_movk(), "wrong insns in patch");
    uintptr_t dest = (uintptr_t)o;
    Instruction_aarch64::patch(insn_addr, 20, 5, dest & 0xffff);
    Instruction_aarch64::patch(insn_addr+4, 20, 5, (dest >>= 16) & 0xffff);
    Instruction_aarch64::patch(insn_addr+8, 20, 5, (dest >>= 16) & 0xffff);
    instructions = 3;
  }
  return instructions * NativeInstruction::instruction_size;
}

int MacroAssembler::patch_narrow_klass(address insn_addr, narrowKlass n) {
  // Metatdata pointers are either narrow (32 bits) or wide (48 bits).
  // We encode narrow ones by setting the upper 16 bits in the first
  // instruction.
  NativeInstruction *insn = nativeInstruction_at(insn_addr);
  assert(Instruction_aarch64::extract(insn->encoding(), 31, 21) == 0b11010010101 &&
         nativeInstruction_at(insn_addr+4)->is_movk(), "wrong insns in patch");

  Instruction_aarch64::patch(insn_addr, 20, 5, n >> 16);
  Instruction_aarch64::patch(insn_addr+4, 20, 5, n & 0xffff);
  return 2 * NativeInstruction::instruction_size;
}

address MacroAssembler::target_addr_for_insn(address insn_addr, unsigned insn) {
  intptr_t offset = 0;
  if ((Instruction_aarch64::extract(insn, 29, 24) & 0b011011) == 0b00011000) {
    // Load register (literal)
    offset = Instruction_aarch64::sextract(insn, 23, 5);
    return address(((uint64_t)insn_addr + (offset << 2)));
  } else if (Instruction_aarch64::extract(insn, 30, 26) == 0b00101) {
    // Unconditional branch (immediate)
    offset = Instruction_aarch64::sextract(insn, 25, 0);
  } else if (Instruction_aarch64::extract(insn, 31, 25) == 0b0101010) {
    // Conditional branch (immediate)
    offset = Instruction_aarch64::sextract(insn, 23, 5);
  } else if (Instruction_aarch64::extract(insn, 30, 25) == 0b011010) {
    // Compare & branch (immediate)
    offset = Instruction_aarch64::sextract(insn, 23, 5);
   } else if (Instruction_aarch64::extract(insn, 30, 25) == 0b011011) {
    // Test & branch (immediate)
    offset = Instruction_aarch64::sextract(insn, 18, 5);
  } else if (Instruction_aarch64::extract(insn, 28, 24) == 0b10000) {
    // PC-rel. addressing
    offset = Instruction_aarch64::extract(insn, 30, 29);
    offset |= Instruction_aarch64::sextract(insn, 23, 5) << 2;
    int shift = Instruction_aarch64::extract(insn, 31, 31) ? 12 : 0;
    if (shift) {
      offset <<= shift;
      uint64_t target_page = ((uint64_t)insn_addr) + offset;
      target_page &= ((uint64_t)-1) << shift;
      // Return the target address for the following sequences
      //   1 - adrp    Rx, target_page
      //       ldr/str Ry, [Rx, #offset_in_page]
      //   2 - adrp    Rx, target_page
      //       add     Ry, Rx, #offset_in_page
      //   3 - adrp    Rx, target_page (page aligned reloc, offset == 0)
      //       movk    Rx, #imm12<<32
      //   4 - adrp    Rx, target_page (page aligned reloc, offset == 0)
      //
      // In the first two cases  we check that the register is the same and
      // return the target_page + the offset within the page.
      // Otherwise we assume it is a page aligned relocation and return
      // the target page only.
      //
      unsigned insn2 = ((unsigned*)insn_addr)[1];
      if (Instruction_aarch64::extract(insn2, 29, 24) == 0b111001 &&
                Instruction_aarch64::extract(insn, 4, 0) ==
                        Instruction_aarch64::extract(insn2, 9, 5)) {
        // Load/store register (unsigned immediate)
        unsigned int byte_offset = Instruction_aarch64::extract(insn2, 21, 10);
        unsigned int size = Instruction_aarch64::extract(insn2, 31, 30);
        return address(target_page + (byte_offset << size));
      } else if (Instruction_aarch64::extract(insn2, 31, 22) == 0b1001000100 &&
                Instruction_aarch64::extract(insn, 4, 0) ==
                        Instruction_aarch64::extract(insn2, 4, 0)) {
        // add (immediate)
        unsigned int byte_offset = Instruction_aarch64::extract(insn2, 21, 10);
        return address(target_page + byte_offset);
      } else {
        if (Instruction_aarch64::extract(insn2, 31, 21) == 0b11110010110  &&
               Instruction_aarch64::extract(insn, 4, 0) ==
                 Instruction_aarch64::extract(insn2, 4, 0)) {
          target_page = (target_page & 0xffffffff) |
                         ((uint64_t)Instruction_aarch64::extract(insn2, 20, 5) << 32);
        }
        return (address)target_page;
      }
    } else {
      ShouldNotReachHere();
    }
  } else if (Instruction_aarch64::extract(insn, 31, 23) == 0b110100101) {
    uint32_t *insns = (uint32_t *)insn_addr;
    // Move wide constant: movz, movk, movk.  See movptr().
    assert(nativeInstruction_at(insns+1)->is_movk(), "wrong insns in patch");
    assert(nativeInstruction_at(insns+2)->is_movk(), "wrong insns in patch");
    return address(uint64_t(Instruction_aarch64::extract(insns[0], 20, 5))
                   + (uint64_t(Instruction_aarch64::extract(insns[1], 20, 5)) << 16)
                   + (uint64_t(Instruction_aarch64::extract(insns[2], 20, 5)) << 32));
  } else if (Instruction_aarch64::extract(insn, 31, 22) == 0b1011100101 &&
             Instruction_aarch64::extract(insn, 4, 0) == 0b11111) {
    return 0;
  } else {
    ShouldNotReachHere();
  }
  return address(((uint64_t)insn_addr + (offset << 2)));
}

void MacroAssembler::safepoint_poll(Label& slow_path, bool at_return, bool acquire, bool in_nmethod) {
  if (acquire) {
    lea(rscratch1, Address(rthread, JavaThread::polling_word_offset()));
    ldar(rscratch1, rscratch1);
  } else {
    ldr(rscratch1, Address(rthread, JavaThread::polling_word_offset()));
  }
  if (at_return) {
    // Note that when in_nmethod is set, the stack pointer is incremented before the poll. Therefore,
    // we may safely use the sp instead to perform the stack watermark check.
    cmp(in_nmethod ? sp : rfp, rscratch1);
    br(Assembler::HI, slow_path);
  } else {
    tbnz(rscratch1, log2i_exact(SafepointMechanism::poll_bit()), slow_path);
  }
}

void MacroAssembler::reset_last_Java_frame(bool clear_fp) {
  // we must set sp to zero to clear frame
  str(zr, Address(rthread, JavaThread::last_Java_sp_offset()));

  // must clear fp, so that compiled frames are not confused; it is
  // possible that we need it only for debugging
  if (clear_fp) {
    str(zr, Address(rthread, JavaThread::last_Java_fp_offset()));
  }

  // Always clear the pc because it could have been set by make_walkable()
  str(zr, Address(rthread, JavaThread::last_Java_pc_offset()));
}

// Calls to C land
//
// When entering C land, the rfp, & resp of the last Java frame have to be recorded
// in the (thread-local) JavaThread object. When leaving C land, the last Java fp
// has to be reset to 0. This is required to allow proper stack traversal.
void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         Register last_java_pc,
                                         Register scratch) {

  if (last_java_pc->is_valid()) {
      str(last_java_pc, Address(rthread,
                                JavaThread::frame_anchor_offset()
                                + JavaFrameAnchor::last_Java_pc_offset()));
    }

  // determine last_java_sp register
  if (last_java_sp == sp) {
    mov(scratch, sp);
    last_java_sp = scratch;
  } else if (!last_java_sp->is_valid()) {
    last_java_sp = esp;
  }

  str(last_java_sp, Address(rthread, JavaThread::last_Java_sp_offset()));

  // last_java_fp is optional
  if (last_java_fp->is_valid()) {
    str(last_java_fp, Address(rthread, JavaThread::last_Java_fp_offset()));
  }
}

void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         address  last_java_pc,
                                         Register scratch) {
  assert(last_java_pc != NULL, "must provide a valid PC");

  adr(scratch, last_java_pc);
  str(scratch, Address(rthread,
                       JavaThread::frame_anchor_offset()
                       + JavaFrameAnchor::last_Java_pc_offset()));

  set_last_Java_frame(last_java_sp, last_java_fp, noreg, scratch);
}

void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         Label &L,
                                         Register scratch) {
  if (L.is_bound()) {
    set_last_Java_frame(last_java_sp, last_java_fp, target(L), scratch);
  } else {
    InstructionMark im(this);
    L.add_patch_at(code(), locator());
    set_last_Java_frame(last_java_sp, last_java_fp, pc() /* Patched later */, scratch);
  }
}

void MacroAssembler::far_call(Address entry, CodeBuffer *cbuf, Register tmp) {
  assert(ReservedCodeCacheSize < 4*G, "branch out of range");
  assert(CodeCache::find_blob(entry.target()) != NULL,
         "destination of far call not found in code cache");
  if (far_branches()) {
    uint64_t offset;
    // We can use ADRP here because we know that the total size of
    // the code cache cannot exceed 2Gb.
    adrp(tmp, entry, offset);
    add(tmp, tmp, offset);
    if (cbuf) cbuf->set_insts_mark();
    blr(tmp);
  } else {
    if (cbuf) cbuf->set_insts_mark();
    bl(entry);
  }
}

void MacroAssembler::far_jump(Address entry, CodeBuffer *cbuf, Register tmp) {
  assert(ReservedCodeCacheSize < 4*G, "branch out of range");
  assert(CodeCache::find_blob(entry.target()) != NULL,
         "destination of far call not found in code cache");
  if (far_branches()) {
    uint64_t offset;
    // We can use ADRP here because we know that the total size of
    // the code cache cannot exceed 2Gb.
    adrp(tmp, entry, offset);
    add(tmp, tmp, offset);
    if (cbuf) cbuf->set_insts_mark();
    br(tmp);
  } else {
    if (cbuf) cbuf->set_insts_mark();
    b(entry);
  }
}

void MacroAssembler::reserved_stack_check() {
    // testing if reserved zone needs to be enabled
    Label no_reserved_zone_enabling;

    ldr(rscratch1, Address(rthread, JavaThread::reserved_stack_activation_offset()));
    cmp(sp, rscratch1);
    br(Assembler::LO, no_reserved_zone_enabling);

    enter();   // LR and FP are live.
    lea(rscratch1, CAST_FROM_FN_PTR(address, SharedRuntime::enable_stack_reserved_zone));
    mov(c_rarg0, rthread);
    blr(rscratch1);
    leave();

    // We have already removed our own frame.
    // throw_delayed_StackOverflowError will think that it's been
    // called by our caller.
    lea(rscratch1, RuntimeAddress(StubRoutines::throw_delayed_StackOverflowError_entry()));
    br(rscratch1);
    should_not_reach_here();

    bind(no_reserved_zone_enabling);
}

static void pass_arg0(MacroAssembler* masm, Register arg) {
  if (c_rarg0 != arg ) {
    masm->mov(c_rarg0, arg);
  }
}

static void pass_arg1(MacroAssembler* masm, Register arg) {
  if (c_rarg1 != arg ) {
    masm->mov(c_rarg1, arg);
  }
}

static void pass_arg2(MacroAssembler* masm, Register arg) {
  if (c_rarg2 != arg ) {
    masm->mov(c_rarg2, arg);
  }
}

static void pass_arg3(MacroAssembler* masm, Register arg) {
  if (c_rarg3 != arg ) {
    masm->mov(c_rarg3, arg);
  }
}

void MacroAssembler::call_VM_base(Register oop_result,
                                  Register java_thread,
                                  Register last_java_sp,
                                  address  entry_point,
                                  int      number_of_arguments,
                                  bool     check_exceptions) {
   // determine java_thread register
  if (!java_thread->is_valid()) {
    java_thread = rthread;
  }

  // determine last_java_sp register
  if (!last_java_sp->is_valid()) {
    last_java_sp = esp;
  }

  // debugging support
  assert(number_of_arguments >= 0   , "cannot have negative number of arguments");
  assert(java_thread == rthread, "unexpected register");
#ifdef ASSERT
  // TraceBytecodes does not use r12 but saves it over the call, so don't verify
  // if ((UseCompressedOops || UseCompressedClassPointers) && !TraceBytecodes) verify_heapbase("call_VM_base: heap base corrupted?");
#endif // ASSERT

  assert(java_thread != oop_result  , "cannot use the same register for java_thread & oop_result");
  assert(java_thread != last_java_sp, "cannot use the same register for java_thread & last_java_sp");

  // push java thread (becomes first argument of C function)

  mov(c_rarg0, java_thread);

  // set last Java frame before call
  assert(last_java_sp != rfp, "can't use rfp");

  Label l;
  set_last_Java_frame(last_java_sp, rfp, l, rscratch1);

  // do the call, remove parameters
  MacroAssembler::call_VM_leaf_base(entry_point, number_of_arguments, &l);

  // lr could be poisoned with PAC signature during throw_pending_exception
  // if it was tail-call optimized by compiler, since lr is not callee-saved
  // reload it with proper value
  adr(lr, l);

  // reset last Java frame
  // Only interpreter should have to clear fp
  reset_last_Java_frame(true);

   // C++ interp handles this in the interpreter
  check_and_handle_popframe(java_thread);
  check_and_handle_earlyret(java_thread);

  if (check_exceptions) {
    // check for pending exceptions (java_thread is set upon return)
    ldr(rscratch1, Address(java_thread, in_bytes(Thread::pending_exception_offset())));
    Label ok;
    cbz(rscratch1, ok);
    lea(rscratch1, RuntimeAddress(StubRoutines::forward_exception_entry()));
    br(rscratch1);
    bind(ok);
  }

  // get oop result if there is one and reset the value in the thread
  if (oop_result->is_valid()) {
    get_vm_result(oop_result, java_thread);
  }
}

void MacroAssembler::call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions) {
  call_VM_base(oop_result, noreg, noreg, entry_point, number_of_arguments, check_exceptions);
}

// Maybe emit a call via a trampoline.  If the code cache is small
// trampolines won't be emitted.

address MacroAssembler::trampoline_call(Address entry, CodeBuffer* cbuf) {
  assert(JavaThread::current()->is_Compiler_thread(), "just checking");
  assert(entry.rspec().type() == relocInfo::runtime_call_type
         || entry.rspec().type() == relocInfo::opt_virtual_call_type
         || entry.rspec().type() == relocInfo::static_call_type
         || entry.rspec().type() == relocInfo::virtual_call_type, "wrong reloc type");

  // We need a trampoline if branches are far.
  if (far_branches()) {
    bool in_scratch_emit_size = false;
#ifdef COMPILER2
    // We don't want to emit a trampoline if C2 is generating dummy
    // code during its branch shortening phase.
    CompileTask* task = ciEnv::current()->task();
    in_scratch_emit_size =
      (task != NULL && is_c2_compile(task->comp_level()) &&
       Compile::current()->output()->in_scratch_emit_size());
#endif
    if (!in_scratch_emit_size) {
      address stub = emit_trampoline_stub(offset(), entry.target());
      if (stub == NULL) {
        postcond(pc() == badAddress);
        return NULL; // CodeCache is full
      }
    }
  }

  if (cbuf) cbuf->set_insts_mark();
  relocate(entry.rspec());
  if (!far_branches()) {
    bl(entry.target());
  } else {
    bl(pc());
  }
  // just need to return a non-null address
  postcond(pc() != badAddress);
  return pc();
}


// Emit a trampoline stub for a call to a target which is too far away.
//
// code sequences:
//
// call-site:
//   branch-and-link to <destination> or <trampoline stub>
//
// Related trampoline stub for this call site in the stub section:
//   load the call target from the constant pool
//   branch (LR still points to the call site above)

address MacroAssembler::emit_trampoline_stub(int insts_call_instruction_offset,
                                             address dest) {
  // Max stub size: alignment nop, TrampolineStub.
  address stub = start_a_stub(NativeInstruction::instruction_size
                   + NativeCallTrampolineStub::instruction_size);
  if (stub == NULL) {
    return NULL;  // CodeBuffer::expand failed
  }

  // Create a trampoline stub relocation which relates this trampoline stub
  // with the call instruction at insts_call_instruction_offset in the
  // instructions code-section.
  align(wordSize);
  relocate(trampoline_stub_Relocation::spec(code()->insts()->start()
                                            + insts_call_instruction_offset));
  const int stub_start_offset = offset();

  // Now, create the trampoline stub's code:
  // - load the call
  // - call
  Label target;
  ldr(rscratch1, target);
  br(rscratch1);
  bind(target);
  assert(offset() - stub_start_offset == NativeCallTrampolineStub::data_offset,
         "should be");
  emit_int64((int64_t)dest);

  const address stub_start_addr = addr_at(stub_start_offset);

  assert(is_NativeCallTrampolineStub_at(stub_start_addr), "doesn't look like a trampoline");

  end_a_stub();
  return stub_start_addr;
}

void MacroAssembler::emit_static_call_stub() {
  // CompiledDirectStaticCall::set_to_interpreted knows the
  // exact layout of this stub.

  isb();
  mov_metadata(rmethod, (Metadata*)NULL);

  // Jump to the entry point of the i2c stub.
  movptr(rscratch1, 0);
  br(rscratch1);
}

void MacroAssembler::c2bool(Register x) {
  // implements x == 0 ? 0 : 1
  // note: must only look at least-significant byte of x
  //       since C-style booleans are stored in one byte
  //       only! (was bug)
  tst(x, 0xff);
  cset(x, Assembler::NE);
}

address MacroAssembler::ic_call(address entry, jint method_index) {
  RelocationHolder rh = virtual_call_Relocation::spec(pc(), method_index);
  // address const_ptr = long_constant((jlong)Universe::non_oop_word());
  // uintptr_t offset;
  // ldr_constant(rscratch2, const_ptr);
  movptr(rscratch2, (uintptr_t)Universe::non_oop_word());
  return trampoline_call(Address(entry, rh));
}

// Implementation of call_VM versions

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             bool check_exceptions) {
  call_VM_helper(oop_result, entry_point, 0, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             bool check_exceptions) {
  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 1, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 2, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             Register arg_3,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);

  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);

  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 3, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             int number_of_arguments,
                             bool check_exceptions) {
  call_VM_base(oop_result, rthread, last_java_sp, entry_point, number_of_arguments, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             bool check_exceptions) {
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 1, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             bool check_exceptions) {

  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 2, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             Register arg_3,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 3, check_exceptions);
}


void MacroAssembler::get_vm_result(Register oop_result, Register java_thread) {
  ldr(oop_result, Address(java_thread, JavaThread::vm_result_offset()));
  str(zr, Address(java_thread, JavaThread::vm_result_offset()));
  verify_oop(oop_result, "broken oop in call_VM_base");
}

void MacroAssembler::get_vm_result_2(Register metadata_result, Register java_thread) {
  ldr(metadata_result, Address(java_thread, JavaThread::vm_result_2_offset()));
  str(zr, Address(java_thread, JavaThread::vm_result_2_offset()));
}

void MacroAssembler::align(int modulus) {
  while (offset() % modulus != 0) nop();
}

// these are no-ops overridden by InterpreterMacroAssembler

void MacroAssembler::check_and_handle_earlyret(Register java_thread) { }

void MacroAssembler::check_and_handle_popframe(Register java_thread) { }

// Look up the method for a megamorphic invokeinterface call.
// The target method is determined by <intf_klass, itable_index>.
// The receiver klass is in recv_klass.
// On success, the result will be in method_result, and execution falls through.
// On failure, execution transfers to the given label.
void MacroAssembler::lookup_interface_method(Register recv_klass,
                                             Register intf_klass,
                                             RegisterOrConstant itable_index,
                                             Register method_result,
                                             Register scan_temp,
                                             Label& L_no_such_interface,
                         bool return_method) {
  assert_different_registers(recv_klass, intf_klass, scan_temp);
  assert_different_registers(method_result, intf_klass, scan_temp);
  assert(recv_klass != method_result || !return_method,
     "recv_klass can be destroyed when method isn't needed");
  assert(itable_index.is_constant() || itable_index.as_register() == method_result,
         "caller must use same register for non-constant itable index as for method");

  // Compute start of first itableOffsetEntry (which is at the end of the vtable)
  int vtable_base = in_bytes(Klass::vtable_start_offset());
  int itentry_off = itableMethodEntry::method_offset_in_bytes();
  int scan_step   = itableOffsetEntry::size() * wordSize;
  int vte_size    = vtableEntry::size_in_bytes();
  assert(vte_size == wordSize, "else adjust times_vte_scale");

  ldrw(scan_temp, Address(recv_klass, Klass::vtable_length_offset()));

  // %%% Could store the aligned, prescaled offset in the klassoop.
  // lea(scan_temp, Address(recv_klass, scan_temp, times_vte_scale, vtable_base));
  lea(scan_temp, Address(recv_klass, scan_temp, Address::lsl(3)));
  add(scan_temp, scan_temp, vtable_base);

  if (return_method) {
    // Adjust recv_klass by scaled itable_index, so we can free itable_index.
    assert(itableMethodEntry::size() * wordSize == wordSize, "adjust the scaling in the code below");
    // lea(recv_klass, Address(recv_klass, itable_index, Address::times_ptr, itentry_off));
    lea(recv_klass, Address(recv_klass, itable_index, Address::lsl(3)));
    if (itentry_off)
      add(recv_klass, recv_klass, itentry_off);
  }

  // for (scan = klass->itable(); scan->interface() != NULL; scan += scan_step) {
  //   if (scan->interface() == intf) {
  //     result = (klass + scan->offset() + itable_index);
  //   }
  // }
  Label search, found_method;

  ldr(method_result, Address(scan_temp, itableOffsetEntry::interface_offset_in_bytes()));
  cmp(intf_klass, method_result);
  br(Assembler::EQ, found_method);
  bind(search);
  // Check that the previous entry is non-null.  A null entry means that
  // the receiver class doesn't implement the interface, and wasn't the
  // same as when the caller was compiled.
  cbz(method_result, L_no_such_interface);
  if (itableOffsetEntry::interface_offset_in_bytes() != 0) {
    add(scan_temp, scan_temp, scan_step);
    ldr(method_result, Address(scan_temp, itableOffsetEntry::interface_offset_in_bytes()));
  } else {
    ldr(method_result, Address(pre(scan_temp, scan_step)));
  }
  cmp(intf_klass, method_result);
  br(Assembler::NE, search);

  bind(found_method);

  // Got a hit.
  if (return_method) {
    ldrw(scan_temp, Address(scan_temp, itableOffsetEntry::offset_offset_in_bytes()));
    ldr(method_result, Address(recv_klass, scan_temp, Address::uxtw(0)));
  }
}

// virtual method calling
void MacroAssembler::lookup_virtual_method(Register recv_klass,
                                           RegisterOrConstant vtable_index,
                                           Register method_result) {
  const int base = in_bytes(Klass::vtable_start_offset());
  assert(vtableEntry::size() * wordSize == 8,
         "adjust the scaling in the code below");
  int vtable_offset_in_bytes = base + vtableEntry::method_offset_in_bytes();

  if (vtable_index.is_register()) {
    lea(method_result, Address(recv_klass,
                               vtable_index.as_register(),
                               Address::lsl(LogBytesPerWord)));
    ldr(method_result, Address(method_result, vtable_offset_in_bytes));
  } else {
    vtable_offset_in_bytes += vtable_index.as_constant() * wordSize;
    ldr(method_result,
        form_address(rscratch1, recv_klass, vtable_offset_in_bytes, 0));
  }
}

void MacroAssembler::check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp_reg,
                           Label& L_success) {
  Label L_failure;
  check_klass_subtype_fast_path(sub_klass, super_klass, temp_reg,        &L_success, &L_failure, NULL);
  check_klass_subtype_slow_path(sub_klass, super_klass, temp_reg, noreg, &L_success, NULL);
  bind(L_failure);
}


void MacroAssembler::check_klass_subtype_fast_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   Label* L_slow_path,
                                        RegisterOrConstant super_check_offset) {
  assert_different_registers(sub_klass, super_klass, temp_reg);
  bool must_load_sco = (super_check_offset.constant_or_zero() == -1);
  if (super_check_offset.is_register()) {
    assert_different_registers(sub_klass, super_klass,
                               super_check_offset.as_register());
  } else if (must_load_sco) {
    assert(temp_reg != noreg, "supply either a temp or a register offset");
  }

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  if (L_slow_path == NULL) { L_slow_path = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1, "at most one NULL in the batch");

  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  int sco_offset = in_bytes(Klass::super_check_offset_offset());
  Address super_check_offset_addr(super_klass, sco_offset);

  // Hacked jmp, which may only be used just before L_fallthrough.
#define final_jmp(label)                                                \
  if (&(label) == &L_fallthrough) { /*do nothing*/ }                    \
  else                            b(label)                /*omit semi*/

  // If the pointers are equal, we are done (e.g., String[] elements).
  // This self-check enables sharing of secondary supertype arrays among
  // non-primary types such as array-of-interface.  Otherwise, each such
  // type would need its own customized SSA.
  // We move this check to the front of the fast path because many
  // type checks are in fact trivially successful in this manner,
  // so we get a nicely predicted branch right at the start of the check.
  cmp(sub_klass, super_klass);
  br(Assembler::EQ, *L_success);

  // Check the supertype display:
  if (must_load_sco) {
    ldrw(temp_reg, super_check_offset_addr);
    super_check_offset = RegisterOrConstant(temp_reg);
  }
  Address super_check_addr(sub_klass, super_check_offset);
  ldr(rscratch1, super_check_addr);
  cmp(super_klass, rscratch1); // load displayed supertype

  // This check has worked decisively for primary supers.
  // Secondary supers are sought in the super_cache ('super_cache_addr').
  // (Secondary supers are interfaces and very deeply nested subtypes.)
  // This works in the same check above because of a tricky aliasing
  // between the super_cache and the primary super display elements.
  // (The 'super_check_addr' can address either, as the case requires.)
  // Note that the cache is updated below if it does not help us find
  // what we need immediately.
  // So if it was a primary super, we can just fail immediately.
  // Otherwise, it's the slow path for us (no success at this point).

  if (super_check_offset.is_register()) {
    br(Assembler::EQ, *L_success);
    subs(zr, super_check_offset.as_register(), sc_offset);
    if (L_failure == &L_fallthrough) {
      br(Assembler::EQ, *L_slow_path);
    } else {
      br(Assembler::NE, *L_failure);
      final_jmp(*L_slow_path);
    }
  } else if (super_check_offset.as_constant() == sc_offset) {
    // Need a slow path; fast failure is impossible.
    if (L_slow_path == &L_fallthrough) {
      br(Assembler::EQ, *L_success);
    } else {
      br(Assembler::NE, *L_slow_path);
      final_jmp(*L_success);
    }
  } else {
    // No slow path; it's a fast decision.
    if (L_failure == &L_fallthrough) {
      br(Assembler::EQ, *L_success);
    } else {
      br(Assembler::NE, *L_failure);
      final_jmp(*L_success);
    }
  }

  bind(L_fallthrough);

#undef final_jmp
}

// These two are taken from x86, but they look generally useful

// scans count pointer sized words at [addr] for occurence of value,
// generic
void MacroAssembler::repne_scan(Register addr, Register value, Register count,
                                Register scratch) {
  Label Lloop, Lexit;
  cbz(count, Lexit);
  bind(Lloop);
  ldr(scratch, post(addr, wordSize));
  cmp(value, scratch);
  br(EQ, Lexit);
  sub(count, count, 1);
  cbnz(count, Lloop);
  bind(Lexit);
}

// scans count 4 byte words at [addr] for occurence of value,
// generic
void MacroAssembler::repne_scanw(Register addr, Register value, Register count,
                                Register scratch) {
  Label Lloop, Lexit;
  cbz(count, Lexit);
  bind(Lloop);
  ldrw(scratch, post(addr, wordSize));
  cmpw(value, scratch);
  br(EQ, Lexit);
  sub(count, count, 1);
  cbnz(count, Lloop);
  bind(Lexit);
}

void MacroAssembler::check_klass_subtype_slow_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Register temp2_reg,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   bool set_cond_codes) {
  assert_different_registers(sub_klass, super_klass, temp_reg);
  if (temp2_reg != noreg)
    assert_different_registers(sub_klass, super_klass, temp_reg, temp2_reg, rscratch1);
#define IS_A_TEMP(reg) ((reg) == temp_reg || (reg) == temp2_reg)

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1, "at most one NULL in the batch");

  // a couple of useful fields in sub_klass:
  int ss_offset = in_bytes(Klass::secondary_supers_offset());
  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  Address secondary_supers_addr(sub_klass, ss_offset);
  Address super_cache_addr(     sub_klass, sc_offset);

  BLOCK_COMMENT("check_klass_subtype_slow_path");

  // Do a linear scan of the secondary super-klass chain.
  // This code is rarely used, so simplicity is a virtue here.
  // The repne_scan instruction uses fixed registers, which we must spill.
  // Don't worry too much about pre-existing connections with the input regs.

  assert(sub_klass != r0, "killed reg"); // killed by mov(r0, super)
  assert(sub_klass != r2, "killed reg"); // killed by lea(r2, &pst_counter)

  RegSet pushed_registers;
  if (!IS_A_TEMP(r2))    pushed_registers += r2;
  if (!IS_A_TEMP(r5))    pushed_registers += r5;

  if (super_klass != r0 || UseCompressedOops) {
    if (!IS_A_TEMP(r0))   pushed_registers += r0;
  }

  push(pushed_registers, sp);

  // Get super_klass value into r0 (even if it was in r5 or r2).
  if (super_klass != r0) {
    mov(r0, super_klass);
  }

#ifndef PRODUCT
  mov(rscratch2, (address)&SharedRuntime::_partial_subtype_ctr);
  Address pst_counter_addr(rscratch2);
  ldr(rscratch1, pst_counter_addr);
  add(rscratch1, rscratch1, 1);
  str(rscratch1, pst_counter_addr);
#endif //PRODUCT

  // We will consult the secondary-super array.
  ldr(r5, secondary_supers_addr);
  // Load the array length.
  ldrw(r2, Address(r5, Array<Klass*>::length_offset_in_bytes()));
  // Skip to start of data.
  add(r5, r5, Array<Klass*>::base_offset_in_bytes());

  cmp(sp, zr); // Clear Z flag; SP is never zero
  // Scan R2 words at [R5] for an occurrence of R0.
  // Set NZ/Z based on last compare.
  repne_scan(r5, r0, r2, rscratch1);

  // Unspill the temp. registers:
  pop(pushed_registers, sp);

  br(Assembler::NE, *L_failure);

  // Success.  Cache the super we found and proceed in triumph.
  str(super_klass, super_cache_addr);

  if (L_success != &L_fallthrough) {
    b(*L_success);
  }

#undef IS_A_TEMP

  bind(L_fallthrough);
}

void MacroAssembler::clinit_barrier(Register klass, Register scratch, Label* L_fast_path, Label* L_slow_path) {
  assert(L_fast_path != NULL || L_slow_path != NULL, "at least one is required");
  assert_different_registers(klass, rthread, scratch);

  Label L_fallthrough, L_tmp;
  if (L_fast_path == NULL) {
    L_fast_path = &L_fallthrough;
  } else if (L_slow_path == NULL) {
    L_slow_path = &L_fallthrough;
  }
  // Fast path check: class is fully initialized
  ldrb(scratch, Address(klass, InstanceKlass::init_state_offset()));
  subs(zr, scratch, InstanceKlass::fully_initialized);
  br(Assembler::EQ, *L_fast_path);

  // Fast path check: current thread is initializer thread
  ldr(scratch, Address(klass, InstanceKlass::init_thread_offset()));
  cmp(rthread, scratch);

  if (L_slow_path == &L_fallthrough) {
    br(Assembler::EQ, *L_fast_path);
    bind(*L_slow_path);
  } else if (L_fast_path == &L_fallthrough) {
    br(Assembler::NE, *L_slow_path);
    bind(*L_fast_path);
  } else {
    Unimplemented();
  }
}

void MacroAssembler::verify_oop(Register reg, const char* s) {
  if (!VerifyOops) return;

  // Pass register number to verify_oop_subroutine
  const char* b = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("verify_oop: %s: %s", reg->name(), s);
    b = code_string(ss.as_string());
  }
  BLOCK_COMMENT("verify_oop {");

  stp(r0, rscratch1, Address(pre(sp, -2 * wordSize)));
  stp(rscratch2, lr, Address(pre(sp, -2 * wordSize)));

  mov(r0, reg);
  movptr(rscratch1, (uintptr_t)(address)b);

  // call indirectly to solve generation ordering problem
  lea(rscratch2, ExternalAddress(StubRoutines::verify_oop_subroutine_entry_address()));
  ldr(rscratch2, Address(rscratch2));
  blr(rscratch2);

  ldp(rscratch2, lr, Address(post(sp, 2 * wordSize)));
  ldp(r0, rscratch1, Address(post(sp, 2 * wordSize)));

  BLOCK_COMMENT("} verify_oop");
}

void MacroAssembler::verify_oop_addr(Address addr, const char* s) {
  if (!VerifyOops) return;

  const char* b = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("verify_oop_addr: %s", s);
    b = code_string(ss.as_string());
  }
  BLOCK_COMMENT("verify_oop_addr {");

  stp(r0, rscratch1, Address(pre(sp, -2 * wordSize)));
  stp(rscratch2, lr, Address(pre(sp, -2 * wordSize)));

  // addr may contain sp so we will have to adjust it based on the
  // pushes that we just did.
  if (addr.uses(sp)) {
    lea(r0, addr);
    ldr(r0, Address(r0, 4 * wordSize));
  } else {
    ldr(r0, addr);
  }
  movptr(rscratch1, (uintptr_t)(address)b);

  // call indirectly to solve generation ordering problem
  lea(rscratch2, ExternalAddress(StubRoutines::verify_oop_subroutine_entry_address()));
  ldr(rscratch2, Address(rscratch2));
  blr(rscratch2);

  ldp(rscratch2, lr, Address(post(sp, 2 * wordSize)));
  ldp(r0, rscratch1, Address(post(sp, 2 * wordSize)));

  BLOCK_COMMENT("} verify_oop_addr");
}

Address MacroAssembler::argument_address(RegisterOrConstant arg_slot,
                                         int extra_slot_offset) {
  // cf. TemplateTable::prepare_invoke(), if (load_receiver).
  int stackElementSize = Interpreter::stackElementSize;
  int offset = Interpreter::expr_offset_in_bytes(extra_slot_offset+0);
#ifdef ASSERT
  int offset1 = Interpreter::expr_offset_in_bytes(extra_slot_offset+1);
  assert(offset1 - offset == stackElementSize, "correct arithmetic");
#endif
  if (arg_slot.is_constant()) {
    return Address(esp, arg_slot.as_constant() * stackElementSize
                   + offset);
  } else {
    add(rscratch1, esp, arg_slot.as_register(),
        ext::uxtx, exact_log2(stackElementSize));
    return Address(rscratch1, offset);
  }
}

void MacroAssembler::call_VM_leaf_base(address entry_point,
                                       int number_of_arguments,
                                       Label *retaddr) {
  Label E, L;

  stp(rscratch1, rmethod, Address(pre(sp, -2 * wordSize)));

  mov(rscratch1, entry_point);
  blr(rscratch1);
  if (retaddr)
    bind(*retaddr);

  ldp(rscratch1, rmethod, Address(post(sp, 2 * wordSize)));
}

void MacroAssembler::call_VM_leaf(address entry_point, int number_of_arguments) {
  call_VM_leaf_base(entry_point, number_of_arguments);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0) {
  pass_arg0(this, arg_0);
  call_VM_leaf_base(entry_point, 1);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0, Register arg_1) {
  pass_arg0(this, arg_0);
  pass_arg1(this, arg_1);
  call_VM_leaf_base(entry_point, 2);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0,
                                  Register arg_1, Register arg_2) {
  pass_arg0(this, arg_0);
  pass_arg1(this, arg_1);
  pass_arg2(this, arg_2);
  call_VM_leaf_base(entry_point, 3);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0) {
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 1);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1) {

  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 2);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1, Register arg_2) {
  assert(arg_0 != c_rarg2, "smashed arg");
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 3);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1, Register arg_2, Register arg_3) {
  assert(arg_0 != c_rarg3, "smashed arg");
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);
  assert(arg_0 != c_rarg2, "smashed arg");
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 4);
}

void MacroAssembler::null_check(Register reg, int offset) {
  if (needs_explicit_null_check(offset)) {
    // provoke OS NULL exception if reg = NULL by
    // accessing M[reg] w/o changing any registers
    // NOTE: this is plenty to provoke a segv
    ldr(zr, Address(reg));
  } else {
    // nothing to do, (later) access of M[reg + offset]
    // will provoke OS NULL exception if reg = NULL
  }
}

// MacroAssembler protected routines needed to implement
// public methods

void MacroAssembler::mov(Register r, Address dest) {
  code_section()->relocate(pc(), dest.rspec());
  uint64_t imm64 = (uint64_t)dest.target();
  movptr(r, imm64);
}

// Move a constant pointer into r.  In AArch64 mode the virtual
// address space is 48 bits in size, so we only need three
// instructions to create a patchable instruction sequence that can
// reach anywhere.
void MacroAssembler::movptr(Register r, uintptr_t imm64) {
#ifndef PRODUCT
  {
    char buffer[64];
    snprintf(buffer, sizeof(buffer), "0x%" PRIX64, (uint64_t)imm64);
    block_comment(buffer);
  }
#endif
  assert(imm64 < (1ull << 48), "48-bit overflow in address constant");
  movz(r, imm64 & 0xffff);
  imm64 >>= 16;
  movk(r, imm64 & 0xffff, 16);
  imm64 >>= 16;
  movk(r, imm64 & 0xffff, 32);
}

// Macro to mov replicated immediate to vector register.
//  Vd will get the following values for different arrangements in T
//   imm32 == hex 000000gh  T8B:  Vd = ghghghghghghghgh
//   imm32 == hex 000000gh  T16B: Vd = ghghghghghghghghghghghghghghghgh
//   imm32 == hex 0000efgh  T4H:  Vd = efghefghefghefgh
//   imm32 == hex 0000efgh  T8H:  Vd = efghefghefghefghefghefghefghefgh
//   imm32 == hex abcdefgh  T2S:  Vd = abcdefghabcdefgh
//   imm32 == hex abcdefgh  T4S:  Vd = abcdefghabcdefghabcdefghabcdefgh
//   T1D/T2D: invalid
void MacroAssembler::mov(FloatRegister Vd, SIMD_Arrangement T, uint32_t imm32) {
  assert(T != T1D && T != T2D, "invalid arrangement");
  if (T == T8B || T == T16B) {
    assert((imm32 & ~0xff) == 0, "extraneous bits in unsigned imm32 (T8B/T16B)");
    movi(Vd, T, imm32 & 0xff, 0);
    return;
  }
  uint32_t nimm32 = ~imm32;
  if (T == T4H || T == T8H) {
    assert((imm32  & ~0xffff) == 0, "extraneous bits in unsigned imm32 (T4H/T8H)");
    imm32 &= 0xffff;
    nimm32 &= 0xffff;
  }
  uint32_t x = imm32;
  int movi_cnt = 0;
  int movn_cnt = 0;
  while (x) { if (x & 0xff) movi_cnt++; x >>= 8; }
  x = nimm32;
  while (x) { if (x & 0xff) movn_cnt++; x >>= 8; }
  if (movn_cnt < movi_cnt) imm32 = nimm32;
  unsigned lsl = 0;
  while (imm32 && (imm32 & 0xff) == 0) { lsl += 8; imm32 >>= 8; }
  if (movn_cnt < movi_cnt)
    mvni(Vd, T, imm32 & 0xff, lsl);
  else
    movi(Vd, T, imm32 & 0xff, lsl);
  imm32 >>= 8; lsl += 8;
  while (imm32) {
    while ((imm32 & 0xff) == 0) { lsl += 8; imm32 >>= 8; }
    if (movn_cnt < movi_cnt)
      bici(Vd, T, imm32 & 0xff, lsl);
    else
      orri(Vd, T, imm32 & 0xff, lsl);
    lsl += 8; imm32 >>= 8;
  }
}

void MacroAssembler::mov_immediate64(Register dst, uint64_t imm64)
{
#ifndef PRODUCT
  {
    char buffer[64];
    snprintf(buffer, sizeof(buffer), "0x%" PRIX64, imm64);
    block_comment(buffer);
  }
#endif
  if (operand_valid_for_logical_immediate(false, imm64)) {
    orr(dst, zr, imm64);
  } else {
    // we can use a combination of MOVZ or MOVN with
    // MOVK to build up the constant
    uint64_t imm_h[4];
    int zero_count = 0;
    int neg_count = 0;
    int i;
    for (i = 0; i < 4; i++) {
      imm_h[i] = ((imm64 >> (i * 16)) & 0xffffL);
      if (imm_h[i] == 0) {
        zero_count++;
      } else if (imm_h[i] == 0xffffL) {
        neg_count++;
      }
    }
    if (zero_count == 4) {
      // one MOVZ will do
      movz(dst, 0);
    } else if (neg_count == 4) {
      // one MOVN will do
      movn(dst, 0);
    } else if (zero_count == 3) {
      for (i = 0; i < 4; i++) {
        if (imm_h[i] != 0L) {
          movz(dst, (uint32_t)imm_h[i], (i << 4));
          break;
        }
      }
    } else if (neg_count == 3) {
      // one MOVN will do
      for (int i = 0; i < 4; i++) {
        if (imm_h[i] != 0xffffL) {
          movn(dst, (uint32_t)imm_h[i] ^ 0xffffL, (i << 4));
          break;
        }
      }
    } else if (zero_count == 2) {
      // one MOVZ and one MOVK will do
      for (i = 0; i < 3; i++) {
        if (imm_h[i] != 0L) {
          movz(dst, (uint32_t)imm_h[i], (i << 4));
          i++;
          break;
        }
      }
      for (;i < 4; i++) {
        if (imm_h[i] != 0L) {
          movk(dst, (uint32_t)imm_h[i], (i << 4));
        }
      }
    } else if (neg_count == 2) {
      // one MOVN and one MOVK will do
      for (i = 0; i < 4; i++) {
        if (imm_h[i] != 0xffffL) {
          movn(dst, (uint32_t)imm_h[i] ^ 0xffffL, (i << 4));
          i++;
          break;
        }
      }
      for (;i < 4; i++) {
        if (imm_h[i] != 0xffffL) {
          movk(dst, (uint32_t)imm_h[i], (i << 4));
        }
      }
    } else if (zero_count == 1) {
      // one MOVZ and two MOVKs will do
      for (i = 0; i < 4; i++) {
        if (imm_h[i] != 0L) {
          movz(dst, (uint32_t)imm_h[i], (i << 4));
          i++;
          break;
        }
      }
      for (;i < 4; i++) {
        if (imm_h[i] != 0x0L) {
          movk(dst, (uint32_t)imm_h[i], (i << 4));
        }
      }
    } else if (neg_count == 1) {
      // one MOVN and two MOVKs will do
      for (i = 0; i < 4; i++) {
        if (imm_h[i] != 0xffffL) {
          movn(dst, (uint32_t)imm_h[i] ^ 0xffffL, (i << 4));
          i++;
          break;
        }
      }
      for (;i < 4; i++) {
        if (imm_h[i] != 0xffffL) {
          movk(dst, (uint32_t)imm_h[i], (i << 4));
        }
      }
    } else {
      // use a MOVZ and 3 MOVKs (makes it easier to debug)
      movz(dst, (uint32_t)imm_h[0], 0);
      for (i = 1; i < 4; i++) {
        movk(dst, (uint32_t)imm_h[i], (i << 4));
      }
    }
  }
}

void MacroAssembler::mov_immediate32(Register dst, uint32_t imm32)
{
#ifndef PRODUCT
    {
      char buffer[64];
      snprintf(buffer, sizeof(buffer), "0x%" PRIX32, imm32);
      block_comment(buffer);
    }
#endif
  if (operand_valid_for_logical_immediate(true, imm32)) {
    orrw(dst, zr, imm32);
  } else {
    // we can use MOVZ, MOVN or two calls to MOVK to build up the
    // constant
    uint32_t imm_h[2];
    imm_h[0] = imm32 & 0xffff;
    imm_h[1] = ((imm32 >> 16) & 0xffff);
    if (imm_h[0] == 0) {
      movzw(dst, imm_h[1], 16);
    } else if (imm_h[0] == 0xffff) {
      movnw(dst, imm_h[1] ^ 0xffff, 16);
    } else if (imm_h[1] == 0) {
      movzw(dst, imm_h[0], 0);
    } else if (imm_h[1] == 0xffff) {
      movnw(dst, imm_h[0] ^ 0xffff, 0);
    } else {
      // use a MOVZ and MOVK (makes it easier to debug)
      movzw(dst, imm_h[0], 0);
      movkw(dst, imm_h[1], 16);
    }
  }
}

// Form an address from base + offset in Rd.  Rd may or may
// not actually be used: you must use the Address that is returned.
// It is up to you to ensure that the shift provided matches the size
// of your data.
Address MacroAssembler::form_address(Register Rd, Register base, int64_t byte_offset, int shift) {
  if (Address::offset_ok_for_immed(byte_offset, shift))
    // It fits; no need for any heroics
    return Address(base, byte_offset);

  // Don't do anything clever with negative or misaligned offsets
  unsigned mask = (1 << shift) - 1;
  if (byte_offset < 0 || byte_offset & mask) {
    mov(Rd, byte_offset);
    add(Rd, base, Rd);
    return Address(Rd);
  }

  // See if we can do this with two 12-bit offsets
  {
    uint64_t word_offset = byte_offset >> shift;
    uint64_t masked_offset = word_offset & 0xfff000;
    if (Address::offset_ok_for_immed(word_offset - masked_offset, 0)
        && Assembler::operand_valid_for_add_sub_immediate(masked_offset << shift)) {
      add(Rd, base, masked_offset << shift);
      word_offset -= masked_offset;
      return Address(Rd, word_offset << shift);
    }
  }

  // Do it the hard way
  mov(Rd, byte_offset);
  add(Rd, base, Rd);
  return Address(Rd);
}

void MacroAssembler::atomic_incw(Register counter_addr, Register tmp, Register tmp2) {
  if (UseLSE) {
    mov(tmp, 1);
    ldadd(Assembler::word, tmp, zr, counter_addr);
    return;
  }
  Label retry_load;
  if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))
    prfm(Address(counter_addr), PSTL1STRM);
  bind(retry_load);
  // flush and load exclusive from the memory location
  ldxrw(tmp, counter_addr);
  addw(tmp, tmp, 1);
  // if we store+flush with no intervening write tmp wil be zero
  stxrw(tmp2, tmp, counter_addr);
  cbnzw(tmp2, retry_load);
}


int MacroAssembler::corrected_idivl(Register result, Register ra, Register rb,
                                    bool want_remainder, Register scratch)
{
  // Full implementation of Java idiv and irem.  The function
  // returns the (pc) offset of the div instruction - may be needed
  // for implicit exceptions.
  //
  // constraint : ra/rb =/= scratch
  //         normal case
  //
  // input : ra: dividend
  //         rb: divisor
  //
  // result: either
  //         quotient  (= ra idiv rb)
  //         remainder (= ra irem rb)

  assert(ra != scratch && rb != scratch, "reg cannot be scratch");

  int idivl_offset = offset();
  if (! want_remainder) {
    sdivw(result, ra, rb);
  } else {
    sdivw(scratch, ra, rb);
    Assembler::msubw(result, scratch, rb, ra);
  }

  return idivl_offset;
}

int MacroAssembler::corrected_idivq(Register result, Register ra, Register rb,
                                    bool want_remainder, Register scratch)
{
  // Full implementation of Java ldiv and lrem.  The function
  // returns the (pc) offset of the div instruction - may be needed
  // for implicit exceptions.
  //
  // constraint : ra/rb =/= scratch
  //         normal case
  //
  // input : ra: dividend
  //         rb: divisor
  //
  // result: either
  //         quotient  (= ra idiv rb)
  //         remainder (= ra irem rb)

  assert(ra != scratch && rb != scratch, "reg cannot be scratch");

  int idivq_offset = offset();
  if (! want_remainder) {
    sdiv(result, ra, rb);
  } else {
    sdiv(scratch, ra, rb);
    Assembler::msub(result, scratch, rb, ra);
  }

  return idivq_offset;
}

void MacroAssembler::membar(Membar_mask_bits order_constraint) {
  address prev = pc() - NativeMembar::instruction_size;
  address last = code()->last_insn();
  if (last != NULL && nativeInstruction_at(last)->is_Membar() && prev == last) {
    NativeMembar *bar = NativeMembar_at(prev);
    // We are merging two memory barrier instructions.  On AArch64 we
    // can do this simply by ORing them together.
    bar->set_kind(bar->get_kind() | order_constraint);
    BLOCK_COMMENT("merged membar");
  } else {
    code()->set_last_insn(pc());
    dmb(Assembler::barrier(order_constraint));
  }
}

bool MacroAssembler::try_merge_ldst(Register rt, const Address &adr, size_t size_in_bytes, bool is_store) {
  if (ldst_can_merge(rt, adr, size_in_bytes, is_store)) {
    merge_ldst(rt, adr, size_in_bytes, is_store);
    code()->clear_last_insn();
    return true;
  } else {
    assert(size_in_bytes == 8 || size_in_bytes == 4, "only 8 bytes or 4 bytes load/store is supported.");
    const uint64_t mask = size_in_bytes - 1;
    if (adr.getMode() == Address::base_plus_offset &&
        (adr.offset() & mask) == 0) { // only supports base_plus_offset.
      code()->set_last_insn(pc());
    }
    return false;
  }
}

void MacroAssembler::ldr(Register Rx, const Address &adr) {
  // We always try to merge two adjacent loads into one ldp.
  if (!try_merge_ldst(Rx, adr, 8, false)) {
    Assembler::ldr(Rx, adr);
  }
}

void MacroAssembler::ldrw(Register Rw, const Address &adr) {
  // We always try to merge two adjacent loads into one ldp.
  if (!try_merge_ldst(Rw, adr, 4, false)) {
    Assembler::ldrw(Rw, adr);
  }
}

void MacroAssembler::str(Register Rx, const Address &adr) {
  // We always try to merge two adjacent stores into one stp.
  if (!try_merge_ldst(Rx, adr, 8, true)) {
    Assembler::str(Rx, adr);
  }
}

void MacroAssembler::strw(Register Rw, const Address &adr) {
  // We always try to merge two adjacent stores into one stp.
  if (!try_merge_ldst(Rw, adr, 4, true)) {
    Assembler::strw(Rw, adr);
  }
}

// MacroAssembler routines found actually to be needed

void MacroAssembler::push(Register src)
{
  str(src, Address(pre(esp, -1 * wordSize)));
}

void MacroAssembler::pop(Register dst)
{
  ldr(dst, Address(post(esp, 1 * wordSize)));
}

// Note: load_unsigned_short used to be called load_unsigned_word.
int MacroAssembler::load_unsigned_short(Register dst, Address src) {
  int off = offset();
  ldrh(dst, src);
  return off;
}

int MacroAssembler::load_unsigned_byte(Register dst, Address src) {
  int off = offset();
  ldrb(dst, src);
  return off;
}

int MacroAssembler::load_signed_short(Register dst, Address src) {
  int off = offset();
  ldrsh(dst, src);
  return off;
}

int MacroAssembler::load_signed_byte(Register dst, Address src) {
  int off = offset();
  ldrsb(dst, src);
  return off;
}

int MacroAssembler::load_signed_short32(Register dst, Address src) {
  int off = offset();
  ldrshw(dst, src);
  return off;
}

int MacroAssembler::load_signed_byte32(Register dst, Address src) {
  int off = offset();
  ldrsbw(dst, src);
  return off;
}

void MacroAssembler::load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed, Register dst2) {
  switch (size_in_bytes) {
  case  8:  ldr(dst, src); break;
  case  4:  ldrw(dst, src); break;
  case  2:  is_signed ? load_signed_short(dst, src) : load_unsigned_short(dst, src); break;
  case  1:  is_signed ? load_signed_byte( dst, src) : load_unsigned_byte( dst, src); break;
  default:  ShouldNotReachHere();
  }
}

void MacroAssembler::store_sized_value(Address dst, Register src, size_t size_in_bytes, Register src2) {
  switch (size_in_bytes) {
  case  8:  str(src, dst); break;
  case  4:  strw(src, dst); break;
  case  2:  strh(src, dst); break;
  case  1:  strb(src, dst); break;
  default:  ShouldNotReachHere();
  }
}

void MacroAssembler::decrementw(Register reg, int value)
{
  if (value < 0)  { incrementw(reg, -value);      return; }
  if (value == 0) {                               return; }
  if (value < (1 << 12)) { subw(reg, reg, value); return; }
  /* else */ {
    guarantee(reg != rscratch2, "invalid dst for register decrement");
    movw(rscratch2, (unsigned)value);
    subw(reg, reg, rscratch2);
  }
}

void MacroAssembler::decrement(Register reg, int value)
{
  if (value < 0)  { increment(reg, -value);      return; }
  if (value == 0) {                              return; }
  if (value < (1 << 12)) { sub(reg, reg, value); return; }
  /* else */ {
    assert(reg != rscratch2, "invalid dst for register decrement");
    mov(rscratch2, (uint64_t)value);
    sub(reg, reg, rscratch2);
  }
}

void MacroAssembler::decrementw(Address dst, int value)
{
  assert(!dst.uses(rscratch1), "invalid dst for address decrement");
  if (dst.getMode() == Address::literal) {
    assert(abs(value) < (1 << 12), "invalid value and address mode combination");
    lea(rscratch2, dst);
    dst = Address(rscratch2);
  }
  ldrw(rscratch1, dst);
  decrementw(rscratch1, value);
  strw(rscratch1, dst);
}

void MacroAssembler::decrement(Address dst, int value)
{
  assert(!dst.uses(rscratch1), "invalid address for decrement");
  if (dst.getMode() == Address::literal) {
    assert(abs(value) < (1 << 12), "invalid value and address mode combination");
    lea(rscratch2, dst);
    dst = Address(rscratch2);
  }
  ldr(rscratch1, dst);
  decrement(rscratch1, value);
  str(rscratch1, dst);
}

void MacroAssembler::incrementw(Register reg, int value)
{
  if (value < 0)  { decrementw(reg, -value);      return; }
  if (value == 0) {                               return; }
  if (value < (1 << 12)) { addw(reg, reg, value); return; }
  /* else */ {
    assert(reg != rscratch2, "invalid dst for register increment");
    movw(rscratch2, (unsigned)value);
    addw(reg, reg, rscratch2);
  }
}

void MacroAssembler::increment(Register reg, int value)
{
  if (value < 0)  { decrement(reg, -value);      return; }
  if (value == 0) {                              return; }
  if (value < (1 << 12)) { add(reg, reg, value); return; }
  /* else */ {
    assert(reg != rscratch2, "invalid dst for register increment");
    movw(rscratch2, (unsigned)value);
    add(reg, reg, rscratch2);
  }
}

void MacroAssembler::incrementw(Address dst, int value)
{
  assert(!dst.uses(rscratch1), "invalid dst for address increment");
  if (dst.getMode() == Address::literal) {
    assert(abs(value) < (1 << 12), "invalid value and address mode combination");
    lea(rscratch2, dst);
    dst = Address(rscratch2);
  }
  ldrw(rscratch1, dst);
  incrementw(rscratch1, value);
  strw(rscratch1, dst);
}

void MacroAssembler::increment(Address dst, int value)
{
  assert(!dst.uses(rscratch1), "invalid dst for address increment");
  if (dst.getMode() == Address::literal) {
    assert(abs(value) < (1 << 12), "invalid value and address mode combination");
    lea(rscratch2, dst);
    dst = Address(rscratch2);
  }
  ldr(rscratch1, dst);
  increment(rscratch1, value);
  str(rscratch1, dst);
}


void MacroAssembler::pusha() {
  push(0x7fffffff, sp);
}

void MacroAssembler::popa() {
  pop(0x7fffffff, sp);
}

// Push lots of registers in the bit set supplied.  Don't push sp.
// Return the number of words pushed
int MacroAssembler::push(unsigned int bitset, Register stack) {
  int words_pushed = 0;

  // Scan bitset to accumulate register pairs
  unsigned char regs[32];
  int count = 0;
  for (int reg = 0; reg <= 30; reg++) {
    if (1 & bitset)
      regs[count++] = reg;
    bitset >>= 1;
  }
  regs[count++] = zr->encoding_nocheck();
  count &= ~1;  // Only push an even nuber of regs

  if (count) {
    stp(as_Register(regs[0]), as_Register(regs[1]),
       Address(pre(stack, -count * wordSize)));
    words_pushed += 2;
  }
  for (int i = 2; i < count; i += 2) {
    stp(as_Register(regs[i]), as_Register(regs[i+1]),
       Address(stack, i * wordSize));
    words_pushed += 2;
  }

  assert(words_pushed == count, "oops, pushed != count");

  return count;
}

int MacroAssembler::pop(unsigned int bitset, Register stack) {
  int words_pushed = 0;

  // Scan bitset to accumulate register pairs
  unsigned char regs[32];
  int count = 0;
  for (int reg = 0; reg <= 30; reg++) {
    if (1 & bitset)
      regs[count++] = reg;
    bitset >>= 1;
  }
  regs[count++] = zr->encoding_nocheck();
  count &= ~1;

  for (int i = 2; i < count; i += 2) {
    ldp(as_Register(regs[i]), as_Register(regs[i+1]),
       Address(stack, i * wordSize));
    words_pushed += 2;
  }
  if (count) {
    ldp(as_Register(regs[0]), as_Register(regs[1]),
       Address(post(stack, count * wordSize)));
    words_pushed += 2;
  }

  assert(words_pushed == count, "oops, pushed != count");

  return count;
}

// Push lots of registers in the bit set supplied.  Don't push sp.
// Return the number of dwords pushed
int MacroAssembler::push_fp(unsigned int bitset, Register stack) {
  int words_pushed = 0;
  bool use_sve = false;
  int sve_vector_size_in_bytes = 0;

#ifdef COMPILER2
  use_sve = Matcher::supports_scalable_vector();
  sve_vector_size_in_bytes = Matcher::scalable_vector_reg_size(T_BYTE);
#endif

  // Scan bitset to accumulate register pairs
  unsigned char regs[32];
  int count = 0;
  for (int reg = 0; reg <= 31; reg++) {
    if (1 & bitset)
      regs[count++] = reg;
    bitset >>= 1;
  }

  if (count == 0) {
    return 0;
  }

  // SVE
  if (use_sve && sve_vector_size_in_bytes > 16) {
    sub(stack, stack, sve_vector_size_in_bytes * count);
    for (int i = 0; i < count; i++) {
      sve_str(as_FloatRegister(regs[i]), Address(stack, i));
    }
    return count * sve_vector_size_in_bytes / 8;
  }

  // NEON
  if (count == 1) {
    strq(as_FloatRegister(regs[0]), Address(pre(stack, -wordSize * 2)));
    return 2;
  }

  bool odd = (count & 1) == 1;
  int push_slots = count + (odd ? 1 : 0);

  // Always pushing full 128 bit registers.
  stpq(as_FloatRegister(regs[0]), as_FloatRegister(regs[1]), Address(pre(stack, -push_slots * wordSize * 2)));
  words_pushed += 2;

  for (int i = 2; i + 1 < count; i += 2) {
    stpq(as_FloatRegister(regs[i]), as_FloatRegister(regs[i+1]), Address(stack, i * wordSize * 2));
    words_pushed += 2;
  }

  if (odd) {
    strq(as_FloatRegister(regs[count - 1]), Address(stack, (count - 1) * wordSize * 2));
    words_pushed++;
  }

  assert(words_pushed == count, "oops, pushed(%d) != count(%d)", words_pushed, count);
  return count * 2;
}

// Return the number of dwords poped
int MacroAssembler::pop_fp(unsigned int bitset, Register stack) {
  int words_pushed = 0;
  bool use_sve = false;
  int sve_vector_size_in_bytes = 0;

#ifdef COMPILER2
  use_sve = Matcher::supports_scalable_vector();
  sve_vector_size_in_bytes = Matcher::scalable_vector_reg_size(T_BYTE);
#endif
  // Scan bitset to accumulate register pairs
  unsigned char regs[32];
  int count = 0;
  for (int reg = 0; reg <= 31; reg++) {
    if (1 & bitset)
      regs[count++] = reg;
    bitset >>= 1;
  }

  if (count == 0) {
    return 0;
  }

  // SVE
  if (use_sve && sve_vector_size_in_bytes > 16) {
    for (int i = count - 1; i >= 0; i--) {
      sve_ldr(as_FloatRegister(regs[i]), Address(stack, i));
    }
    add(stack, stack, sve_vector_size_in_bytes * count);
    return count * sve_vector_size_in_bytes / 8;
  }

  // NEON
  if (count == 1) {
    ldrq(as_FloatRegister(regs[0]), Address(post(stack, wordSize * 2)));
    return 2;
  }

  bool odd = (count & 1) == 1;
  int push_slots = count + (odd ? 1 : 0);

  if (odd) {
    ldrq(as_FloatRegister(regs[count - 1]), Address(stack, (count - 1) * wordSize * 2));
    words_pushed++;
  }

  for (int i = 2; i + 1 < count; i += 2) {
    ldpq(as_FloatRegister(regs[i]), as_FloatRegister(regs[i+1]), Address(stack, i * wordSize * 2));
    words_pushed += 2;
  }

  ldpq(as_FloatRegister(regs[0]), as_FloatRegister(regs[1]), Address(post(stack, push_slots * wordSize * 2)));
  words_pushed += 2;

  assert(words_pushed == count, "oops, pushed(%d) != count(%d)", words_pushed, count);

  return count * 2;
}

#ifdef ASSERT
void MacroAssembler::verify_heapbase(const char* msg) {
#if 0
  assert (UseCompressedOops || UseCompressedClassPointers, "should be compressed");
  assert (Universe::heap() != NULL, "java heap should be initialized");
  if (!UseCompressedOops || Universe::ptr_base() == NULL) {
    // rheapbase is allocated as general register
    return;
  }
  if (CheckCompressedOops) {
    Label ok;
    push(1 << rscratch1->encoding(), sp); // cmpptr trashes rscratch1
    cmpptr(rheapbase, ExternalAddress((address)CompressedOops::ptrs_base_addr()));
    br(Assembler::EQ, ok);
    stop(msg);
    bind(ok);
    pop(1 << rscratch1->encoding(), sp);
  }
#endif
}
#endif

void MacroAssembler::resolve_jobject(Register value, Register thread, Register tmp) {
  Label done, not_weak;
  cbz(value, done);           // Use NULL as-is.

  STATIC_ASSERT(JNIHandles::weak_tag_mask == 1u);
  tbz(r0, 0, not_weak);    // Test for jweak tag.

  // Resolve jweak.
  access_load_at(T_OBJECT, IN_NATIVE | ON_PHANTOM_OOP_REF, value,
                 Address(value, -JNIHandles::weak_tag_value), tmp, thread);
  verify_oop(value);
  b(done);

  bind(not_weak);
  // Resolve (untagged) jobject.
  access_load_at(T_OBJECT, IN_NATIVE, value, Address(value, 0), tmp, thread);
  verify_oop(value);
  bind(done);
}

void MacroAssembler::stop(const char* msg) {
  BLOCK_COMMENT(msg);
  dcps1(0xdeae);
  emit_int64((uintptr_t)msg);
}

void MacroAssembler::unimplemented(const char* what) {
  const char* buf = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("unimplemented: %s", what);
    buf = code_string(ss.as_string());
  }
  stop(buf);
}

// If a constant does not fit in an immediate field, generate some
// number of MOV instructions and then perform the operation.
void MacroAssembler::wrap_add_sub_imm_insn(Register Rd, Register Rn, unsigned imm,
                                           add_sub_imm_insn insn1,
                                           add_sub_reg_insn insn2) {
  assert(Rd != zr, "Rd = zr and not setting flags?");
  if (operand_valid_for_add_sub_immediate((int)imm)) {
    (this->*insn1)(Rd, Rn, imm);
  } else {
    if (uabs(imm) < (1 << 24)) {
       (this->*insn1)(Rd, Rn, imm & -(1 << 12));
       (this->*insn1)(Rd, Rd, imm & ((1 << 12)-1));
    } else {
       assert_different_registers(Rd, Rn);
       mov(Rd, (uint64_t)imm);
       (this->*insn2)(Rd, Rn, Rd, LSL, 0);
    }
  }
}

// Seperate vsn which sets the flags. Optimisations are more restricted
// because we must set the flags correctly.
void MacroAssembler::wrap_adds_subs_imm_insn(Register Rd, Register Rn, unsigned imm,
                                           add_sub_imm_insn insn1,
                                           add_sub_reg_insn insn2) {
  if (operand_valid_for_add_sub_immediate((int)imm)) {
    (this->*insn1)(Rd, Rn, imm);
  } else {
    assert_different_registers(Rd, Rn);
    assert(Rd != zr, "overflow in immediate operand");
    mov(Rd, (uint64_t)imm);
    (this->*insn2)(Rd, Rn, Rd, LSL, 0);
  }
}


void MacroAssembler::add(Register Rd, Register Rn, RegisterOrConstant increment) {
  if (increment.is_register()) {
    add(Rd, Rn, increment.as_register());
  } else {
    add(Rd, Rn, increment.as_constant());
  }
}

void MacroAssembler::addw(Register Rd, Register Rn, RegisterOrConstant increment) {
  if (increment.is_register()) {
    addw(Rd, Rn, increment.as_register());
  } else {
    addw(Rd, Rn, increment.as_constant());
  }
}

void MacroAssembler::sub(Register Rd, Register Rn, RegisterOrConstant decrement) {
  if (decrement.is_register()) {
    sub(Rd, Rn, decrement.as_register());
  } else {
    sub(Rd, Rn, decrement.as_constant());
  }
}

void MacroAssembler::subw(Register Rd, Register Rn, RegisterOrConstant decrement) {
  if (decrement.is_register()) {
    subw(Rd, Rn, decrement.as_register());
  } else {
    subw(Rd, Rn, decrement.as_constant());
  }
}

void MacroAssembler::reinit_heapbase()
{
  if (UseCompressedOops) {
    if (Universe::is_fully_initialized()) {
      mov(rheapbase, CompressedOops::ptrs_base());
    } else {
      lea(rheapbase, ExternalAddress((address)CompressedOops::ptrs_base_addr()));
      ldr(rheapbase, Address(rheapbase));
    }
  }
}

// this simulates the behaviour of the x86 cmpxchg instruction using a
// load linked/store conditional pair. we use the acquire/release
// versions of these instructions so that we flush pending writes as
// per Java semantics.

// n.b the x86 version assumes the old value to be compared against is
// in rax and updates rax with the value located in memory if the
// cmpxchg fails. we supply a register for the old value explicitly

// the aarch64 load linked/store conditional instructions do not
// accept an offset. so, unlike x86, we must provide a plain register
// to identify the memory word to be compared/exchanged rather than a
// register+offset Address.

void MacroAssembler::cmpxchgptr(Register oldv, Register newv, Register addr, Register tmp,
                                Label &succeed, Label *fail) {
  // oldv holds comparison value
  // newv holds value to write in exchange
  // addr identifies memory word to compare against/update
  if (UseLSE) {
    mov(tmp, oldv);
    casal(Assembler::xword, oldv, newv, addr);
    cmp(tmp, oldv);
    br(Assembler::EQ, succeed);
    membar(AnyAny);
  } else {
    Label retry_load, nope;
    if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))
      prfm(Address(addr), PSTL1STRM);
    bind(retry_load);
    // flush and load exclusive from the memory location
    // and fail if it is not what we expect
    ldaxr(tmp, addr);
    cmp(tmp, oldv);
    br(Assembler::NE, nope);
    // if we store+flush with no intervening write tmp wil be zero
    stlxr(tmp, newv, addr);
    cbzw(tmp, succeed);
    // retry so we only ever return after a load fails to compare
    // ensures we don't return a stale value after a failed write.
    b(retry_load);
    // if the memory word differs we return it in oldv and signal a fail
    bind(nope);
    membar(AnyAny);
    mov(oldv, tmp);
  }
  if (fail)
    b(*fail);
}

void MacroAssembler::cmpxchg_obj_header(Register oldv, Register newv, Register obj, Register tmp,
                                        Label &succeed, Label *fail) {
  assert(oopDesc::mark_offset_in_bytes() == 0, "assumption");
  cmpxchgptr(oldv, newv, obj, tmp, succeed, fail);
}

void MacroAssembler::cmpxchgw(Register oldv, Register newv, Register addr, Register tmp,
                                Label &succeed, Label *fail) {
  // oldv holds comparison value
  // newv holds value to write in exchange
  // addr identifies memory word to compare against/update
  // tmp returns 0/1 for success/failure
  if (UseLSE) {
    mov(tmp, oldv);
    casal(Assembler::word, oldv, newv, addr);
    cmp(tmp, oldv);
    br(Assembler::EQ, succeed);
    membar(AnyAny);
  } else {
    Label retry_load, nope;
    if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))
      prfm(Address(addr), PSTL1STRM);
    bind(retry_load);
    // flush and load exclusive from the memory location
    // and fail if it is not what we expect
    ldaxrw(tmp, addr);
    cmp(tmp, oldv);
    br(Assembler::NE, nope);
    // if we store+flush with no intervening write tmp wil be zero
    stlxrw(tmp, newv, addr);
    cbzw(tmp, succeed);
    // retry so we only ever return after a load fails to compare
    // ensures we don't return a stale value after a failed write.
    b(retry_load);
    // if the memory word differs we return it in oldv and signal a fail
    bind(nope);
    membar(AnyAny);
    mov(oldv, tmp);
  }
  if (fail)
    b(*fail);
}

// A generic CAS; success or failure is in the EQ flag.  A weak CAS
// doesn't retry and may fail spuriously.  If the oldval is wanted,
// Pass a register for the result, otherwise pass noreg.

// Clobbers rscratch1
void MacroAssembler::cmpxchg(Register addr, Register expected,
                             Register new_val,
                             enum operand_size size,
                             bool acquire, bool release,
                             bool weak,
                             Register result) {
  if (result == noreg)  result = rscratch1;
  BLOCK_COMMENT("cmpxchg {");
  if (UseLSE) {
    mov(result, expected);
    lse_cas(result, new_val, addr, size, acquire, release, /*not_pair*/ true);
    compare_eq(result, expected, size);
  } else {
    Label retry_load, done;
    if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))
      prfm(Address(addr), PSTL1STRM);
    bind(retry_load);
    load_exclusive(result, addr, size, acquire);
    compare_eq(result, expected, size);
    br(Assembler::NE, done);
    store_exclusive(rscratch1, new_val, addr, size, release);
    if (weak) {
      cmpw(rscratch1, 0u);  // If the store fails, return NE to our caller.
    } else {
      cbnzw(rscratch1, retry_load);
    }
    bind(done);
  }
  BLOCK_COMMENT("} cmpxchg");
}

// A generic comparison. Only compares for equality, clobbers rscratch1.
void MacroAssembler::compare_eq(Register rm, Register rn, enum operand_size size) {
  if (size == xword) {
    cmp(rm, rn);
  } else if (size == word) {
    cmpw(rm, rn);
  } else if (size == halfword) {
    eorw(rscratch1, rm, rn);
    ands(zr, rscratch1, 0xffff);
  } else if (size == byte) {
    eorw(rscratch1, rm, rn);
    ands(zr, rscratch1, 0xff);
  } else {
    ShouldNotReachHere();
  }
}


static bool different(Register a, RegisterOrConstant b, Register c) {
  if (b.is_constant())
    return a != c;
  else
    return a != b.as_register() && a != c && b.as_register() != c;
}

#define ATOMIC_OP(NAME, LDXR, OP, IOP, AOP, STXR, sz)                   \
void MacroAssembler::atomic_##NAME(Register prev, RegisterOrConstant incr, Register addr) { \
  if (UseLSE) {                                                         \
    prev = prev->is_valid() ? prev : zr;                                \
    if (incr.is_register()) {                                           \
      AOP(sz, incr.as_register(), prev, addr);                          \
    } else {                                                            \
      mov(rscratch2, incr.as_constant());                               \
      AOP(sz, rscratch2, prev, addr);                                   \
    }                                                                   \
    return;                                                             \
  }                                                                     \
  Register result = rscratch2;                                          \
  if (prev->is_valid())                                                 \
    result = different(prev, incr, addr) ? prev : rscratch2;            \
                                                                        \
  Label retry_load;                                                     \
  if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))         \
    prfm(Address(addr), PSTL1STRM);                                     \
  bind(retry_load);                                                     \
  LDXR(result, addr);                                                   \
  OP(rscratch1, result, incr);                                          \
  STXR(rscratch2, rscratch1, addr);                                     \
  cbnzw(rscratch2, retry_load);                                         \
  if (prev->is_valid() && prev != result) {                             \
    IOP(prev, rscratch1, incr);                                         \
  }                                                                     \
}

ATOMIC_OP(add, ldxr, add, sub, ldadd, stxr, Assembler::xword)
ATOMIC_OP(addw, ldxrw, addw, subw, ldadd, stxrw, Assembler::word)
ATOMIC_OP(addal, ldaxr, add, sub, ldaddal, stlxr, Assembler::xword)
ATOMIC_OP(addalw, ldaxrw, addw, subw, ldaddal, stlxrw, Assembler::word)

#undef ATOMIC_OP

#define ATOMIC_XCHG(OP, AOP, LDXR, STXR, sz)                            \
void MacroAssembler::atomic_##OP(Register prev, Register newv, Register addr) { \
  if (UseLSE) {                                                         \
    prev = prev->is_valid() ? prev : zr;                                \
    AOP(sz, newv, prev, addr);                                          \
    return;                                                             \
  }                                                                     \
  Register result = rscratch2;                                          \
  if (prev->is_valid())                                                 \
    result = different(prev, newv, addr) ? prev : rscratch2;            \
                                                                        \
  Label retry_load;                                                     \
  if ((VM_Version::features() & VM_Version::CPU_STXR_PREFETCH))         \
    prfm(Address(addr), PSTL1STRM);                                     \
  bind(retry_load);                                                     \
  LDXR(result, addr);                                                   \
  STXR(rscratch1, newv, addr);                                          \
  cbnzw(rscratch1, retry_load);                                         \
  if (prev->is_valid() && prev != result)                               \
    mov(prev, result);                                                  \
}

ATOMIC_XCHG(xchg, swp, ldxr, stxr, Assembler::xword)
ATOMIC_XCHG(xchgw, swp, ldxrw, stxrw, Assembler::word)
ATOMIC_XCHG(xchgl, swpl, ldxr, stlxr, Assembler::xword)
ATOMIC_XCHG(xchglw, swpl, ldxrw, stlxrw, Assembler::word)
ATOMIC_XCHG(xchgal, swpal, ldaxr, stlxr, Assembler::xword)
ATOMIC_XCHG(xchgalw, swpal, ldaxrw, stlxrw, Assembler::word)

#undef ATOMIC_XCHG

#ifndef PRODUCT
extern "C" void findpc(intptr_t x);
#endif

void MacroAssembler::debug64(char* msg, int64_t pc, int64_t regs[])
{
  // In order to get locks to work, we need to fake a in_VM state
  if (ShowMessageBoxOnError ) {
    JavaThread* thread = JavaThread::current();
    JavaThreadState saved_state = thread->thread_state();
    thread->set_thread_state(_thread_in_vm);
#ifndef PRODUCT
    if (CountBytecodes || TraceBytecodes || StopInterpreterAt) {
      ttyLocker ttyl;
      BytecodeCounter::print();
    }
#endif
    if (os::message_box(msg, "Execution stopped, print registers?")) {
      ttyLocker ttyl;
      tty->print_cr(" pc = 0x%016" PRIx64, pc);
#ifndef PRODUCT
      tty->cr();
      findpc(pc);
      tty->cr();
#endif
      tty->print_cr(" r0 = 0x%016" PRIx64, regs[0]);
      tty->print_cr(" r1 = 0x%016" PRIx64, regs[1]);
      tty->print_cr(" r2 = 0x%016" PRIx64, regs[2]);
      tty->print_cr(" r3 = 0x%016" PRIx64, regs[3]);
      tty->print_cr(" r4 = 0x%016" PRIx64, regs[4]);
      tty->print_cr(" r5 = 0x%016" PRIx64, regs[5]);
      tty->print_cr(" r6 = 0x%016" PRIx64, regs[6]);
      tty->print_cr(" r7 = 0x%016" PRIx64, regs[7]);
      tty->print_cr(" r8 = 0x%016" PRIx64, regs[8]);
      tty->print_cr(" r9 = 0x%016" PRIx64, regs[9]);
      tty->print_cr("r10 = 0x%016" PRIx64, regs[10]);
      tty->print_cr("r11 = 0x%016" PRIx64, regs[11]);
      tty->print_cr("r12 = 0x%016" PRIx64, regs[12]);
      tty->print_cr("r13 = 0x%016" PRIx64, regs[13]);
      tty->print_cr("r14 = 0x%016" PRIx64, regs[14]);
      tty->print_cr("r15 = 0x%016" PRIx64, regs[15]);
      tty->print_cr("r16 = 0x%016" PRIx64, regs[16]);
      tty->print_cr("r17 = 0x%016" PRIx64, regs[17]);
      tty->print_cr("r18 = 0x%016" PRIx64, regs[18]);
      tty->print_cr("r19 = 0x%016" PRIx64, regs[19]);
      tty->print_cr("r20 = 0x%016" PRIx64, regs[20]);
      tty->print_cr("r21 = 0x%016" PRIx64, regs[21]);
      tty->print_cr("r22 = 0x%016" PRIx64, regs[22]);
      tty->print_cr("r23 = 0x%016" PRIx64, regs[23]);
      tty->print_cr("r24 = 0x%016" PRIx64, regs[24]);
      tty->print_cr("r25 = 0x%016" PRIx64, regs[25]);
      tty->print_cr("r26 = 0x%016" PRIx64, regs[26]);
      tty->print_cr("r27 = 0x%016" PRIx64, regs[27]);
      tty->print_cr("r28 = 0x%016" PRIx64, regs[28]);
      tty->print_cr("r30 = 0x%016" PRIx64, regs[30]);
      tty->print_cr("r31 = 0x%016" PRIx64, regs[31]);
      BREAKPOINT;
    }
  }
  fatal("DEBUG MESSAGE: %s", msg);
}

RegSet MacroAssembler::call_clobbered_registers() {
  RegSet regs = RegSet::range(r0, r17) - RegSet::of(rscratch1, rscratch2);
#ifndef R18_RESERVED
  regs += r18_tls;
#endif
  return regs;
}

void MacroAssembler::push_call_clobbered_registers_except(RegSet exclude) {
  int step = 4 * wordSize;
  push(call_clobbered_registers() - exclude, sp);
  sub(sp, sp, step);
  mov(rscratch1, -step);
  // Push v0-v7, v16-v31.
  for (int i = 31; i>= 4; i -= 4) {
    if (i <= v7->encoding() || i >= v16->encoding())
      st1(as_FloatRegister(i-3), as_FloatRegister(i-2), as_FloatRegister(i-1),
          as_FloatRegister(i), T1D, Address(post(sp, rscratch1)));
  }
  st1(as_FloatRegister(0), as_FloatRegister(1), as_FloatRegister(2),
      as_FloatRegister(3), T1D, Address(sp));
}

void MacroAssembler::pop_call_clobbered_registers_except(RegSet exclude) {
  for (int i = 0; i < 32; i += 4) {
    if (i <= v7->encoding() || i >= v16->encoding())
      ld1(as_FloatRegister(i), as_FloatRegister(i+1), as_FloatRegister(i+2),
          as_FloatRegister(i+3), T1D, Address(post(sp, 4 * wordSize)));
  }

  reinitialize_ptrue();

  pop(call_clobbered_registers() - exclude, sp);
}

void MacroAssembler::push_CPU_state(bool save_vectors, bool use_sve,
                                    int sve_vector_size_in_bytes) {
  push(0x3fffffff, sp);         // integer registers except lr & sp
  if (save_vectors && use_sve && sve_vector_size_in_bytes > 16) {
    sub(sp, sp, sve_vector_size_in_bytes * FloatRegisterImpl::number_of_registers);
    for (int i = 0; i < FloatRegisterImpl::number_of_registers; i++) {
      sve_str(as_FloatRegister(i), Address(sp, i));
    }
  } else {
    int step = (save_vectors ? 8 : 4) * wordSize;
    mov(rscratch1, -step);
    sub(sp, sp, step);
    for (int i = 28; i >= 4; i -= 4) {
      st1(as_FloatRegister(i), as_FloatRegister(i+1), as_FloatRegister(i+2),
          as_FloatRegister(i+3), save_vectors ? T2D : T1D, Address(post(sp, rscratch1)));
    }
    st1(v0, v1, v2, v3, save_vectors ? T2D : T1D, sp);
  }
}

void MacroAssembler::pop_CPU_state(bool restore_vectors, bool use_sve,
                                   int sve_vector_size_in_bytes) {
  if (restore_vectors && use_sve && sve_vector_size_in_bytes > 16) {
    for (int i = FloatRegisterImpl::number_of_registers - 1; i >= 0; i--) {
      sve_ldr(as_FloatRegister(i), Address(sp, i));
    }
    add(sp, sp, sve_vector_size_in_bytes * FloatRegisterImpl::number_of_registers);
  } else {
    int step = (restore_vectors ? 8 : 4) * wordSize;
    for (int i = 0; i <= 28; i += 4)
      ld1(as_FloatRegister(i), as_FloatRegister(i+1), as_FloatRegister(i+2),
          as_FloatRegister(i+3), restore_vectors ? T2D : T1D, Address(post(sp, step)));
  }

  if (restore_vectors) {
    reinitialize_ptrue();
  }

  pop(0x3fffffff, sp);         // integer registers except lr & sp
}

/**
 * Helpers for multiply_to_len().
 */
void MacroAssembler::add2_with_carry(Register final_dest_hi, Register dest_hi, Register dest_lo,
                                     Register src1, Register src2) {
  adds(dest_lo, dest_lo, src1);
  adc(dest_hi, dest_hi, zr);
  adds(dest_lo, dest_lo, src2);
  adc(final_dest_hi, dest_hi, zr);
}

// Generate an address from (r + r1 extend offset).  "size" is the
// size of the operand.  The result may be in rscratch2.
Address MacroAssembler::offsetted_address(Register r, Register r1,
                                          Address::extend ext, int offset, int size) {
  if (offset || (ext.shift() % size != 0)) {
    lea(rscratch2, Address(r, r1, ext));
    return Address(rscratch2, offset);
  } else {
    return Address(r, r1, ext);
  }
}

Address MacroAssembler::spill_address(int size, int offset, Register tmp)
{
  assert(offset >= 0, "spill to negative address?");
  // Offset reachable ?
  //   Not aligned - 9 bits signed offset
  //   Aligned - 12 bits unsigned offset shifted
  Register base = sp;
  if ((offset & (size-1)) && offset >= (1<<8)) {
    add(tmp, base, offset & ((1<<12)-1));
    base = tmp;
    offset &= -1u<<12;
  }

  if (offset >= (1<<12) * size) {
    add(tmp, base, offset & (((1<<12)-1)<<12));
    base = tmp;
    offset &= ~(((1<<12)-1)<<12);
  }

  return Address(base, offset);
}

Address MacroAssembler::sve_spill_address(int sve_reg_size_in_bytes, int offset, Register tmp) {
  assert(offset >= 0, "spill to negative address?");

  Register base = sp;

  // An immediate offset in the range 0 to 255 which is multiplied
  // by the current vector or predicate register size in bytes.
  if (offset % sve_reg_size_in_bytes == 0 && offset < ((1<<8)*sve_reg_size_in_bytes)) {
    return Address(base, offset / sve_reg_size_in_bytes);
  }

  add(tmp, base, offset);
  return Address(tmp);
}

// Checks whether offset is aligned.
// Returns true if it is, else false.
bool MacroAssembler::merge_alignment_check(Register base,
                                           size_t size,
                                           int64_t cur_offset,
                                           int64_t prev_offset) const {
  if (AvoidUnalignedAccesses) {
    if (base == sp) {
      // Checks whether low offset if aligned to pair of registers.
      int64_t pair_mask = size * 2 - 1;
      int64_t offset = prev_offset > cur_offset ? cur_offset : prev_offset;
      return (offset & pair_mask) == 0;
    } else { // If base is not sp, we can't guarantee the access is aligned.
      return false;
    }
  } else {
    int64_t mask = size - 1;
    // Load/store pair instruction only supports element size aligned offset.
    return (cur_offset & mask) == 0 && (prev_offset & mask) == 0;
  }
}

// Checks whether current and previous loads/stores can be merged.
// Returns true if it can be merged, else false.
bool MacroAssembler::ldst_can_merge(Register rt,
                                    const Address &adr,
                                    size_t cur_size_in_bytes,
                                    bool is_store) const {
  address prev = pc() - NativeInstruction::instruction_size;
  address last = code()->last_insn();

  if (last == NULL || !nativeInstruction_at(last)->is_Imm_LdSt()) {
    return false;
  }

  if (adr.getMode() != Address::base_plus_offset || prev != last) {
    return false;
  }

  NativeLdSt* prev_ldst = NativeLdSt_at(prev);
  size_t prev_size_in_bytes = prev_ldst->size_in_bytes();

  assert(prev_size_in_bytes == 4 || prev_size_in_bytes == 8, "only supports 64/32bit merging.");
  assert(cur_size_in_bytes == 4 || cur_size_in_bytes == 8, "only supports 64/32bit merging.");

  if (cur_size_in_bytes != prev_size_in_bytes || is_store != prev_ldst->is_store()) {
    return false;
  }

  int64_t max_offset = 63 * prev_size_in_bytes;
  int64_t min_offset = -64 * prev_size_in_bytes;

  assert(prev_ldst->is_not_pre_post_index(), "pre-index or post-index is not supported to be merged.");

  // Only same base can be merged.
  if (adr.base() != prev_ldst->base()) {
    return false;
  }

  int64_t cur_offset = adr.offset();
  int64_t prev_offset = prev_ldst->offset();
  size_t diff = abs(cur_offset - prev_offset);
  if (diff != prev_size_in_bytes) {
    return false;
  }

  // Following cases can not be merged:
  // ldr x2, [x2, #8]
  // ldr x3, [x2, #16]
  // or:
  // ldr x2, [x3, #8]
  // ldr x2, [x3, #16]
  // If t1 and t2 is the same in "ldp t1, t2, [xn, #imm]", we'll get SIGILL.
  if (!is_store && (adr.base() == prev_ldst->target() || rt == prev_ldst->target())) {
    return false;
  }

  int64_t low_offset = prev_offset > cur_offset ? cur_offset : prev_offset;
  // Offset range must be in ldp/stp instruction's range.
  if (low_offset > max_offset || low_offset < min_offset) {
    return false;
  }

  if (merge_alignment_check(adr.base(), prev_size_in_bytes, cur_offset, prev_offset)) {
    return true;
  }

  return false;
}

// Merge current load/store with previous load/store into ldp/stp.
void MacroAssembler::merge_ldst(Register rt,
                                const Address &adr,
                                size_t cur_size_in_bytes,
                                bool is_store) {

  assert(ldst_can_merge(rt, adr, cur_size_in_bytes, is_store) == true, "cur and prev must be able to be merged.");

  Register rt_low, rt_high;
  address prev = pc() - NativeInstruction::instruction_size;
  NativeLdSt* prev_ldst = NativeLdSt_at(prev);

  int64_t offset;

  if (adr.offset() < prev_ldst->offset()) {
    offset = adr.offset();
    rt_low = rt;
    rt_high = prev_ldst->target();
  } else {
    offset = prev_ldst->offset();
    rt_low = prev_ldst->target();
    rt_high = rt;
  }

  Address adr_p = Address(prev_ldst->base(), offset);
  // Overwrite previous generated binary.
  code_section()->set_end(prev);

  const size_t sz = prev_ldst->size_in_bytes();
  assert(sz == 8 || sz == 4, "only supports 64/32bit merging.");
  if (!is_store) {
    BLOCK_COMMENT("merged ldr pair");
    if (sz == 8) {
      ldp(rt_low, rt_high, adr_p);
    } else {
      ldpw(rt_low, rt_high, adr_p);
    }
  } else {
    BLOCK_COMMENT("merged str pair");
    if (sz == 8) {
      stp(rt_low, rt_high, adr_p);
    } else {
      stpw(rt_low, rt_high, adr_p);
    }
  }
}

/**
 * Multiply 64 bit by 64 bit first loop.
 */
void MacroAssembler::multiply_64_x_64_loop(Register x, Register xstart, Register x_xstart,
                                           Register y, Register y_idx, Register z,
                                           Register carry, Register product,
                                           Register idx, Register kdx) {
  //
  //  jlong carry, x[], y[], z[];
  //  for (int idx=ystart, kdx=ystart+1+xstart; idx >= 0; idx-, kdx--) {
  //    huge_128 product = y[idx] * x[xstart] + carry;
  //    z[kdx] = (jlong)product;
  //    carry  = (jlong)(product >>> 64);
  //  }
  //  z[xstart] = carry;
  //

  Label L_first_loop, L_first_loop_exit;
  Label L_one_x, L_one_y, L_multiply;

  subsw(xstart, xstart, 1);
  br(Assembler::MI, L_one_x);

  lea(rscratch1, Address(x, xstart, Address::lsl(LogBytesPerInt)));
  ldr(x_xstart, Address(rscratch1));
  ror(x_xstart, x_xstart, 32); // convert big-endian to little-endian

  bind(L_first_loop);
  subsw(idx, idx, 1);
  br(Assembler::MI, L_first_loop_exit);
  subsw(idx, idx, 1);
  br(Assembler::MI, L_one_y);
  lea(rscratch1, Address(y, idx, Address::uxtw(LogBytesPerInt)));
  ldr(y_idx, Address(rscratch1));
  ror(y_idx, y_idx, 32); // convert big-endian to little-endian
  bind(L_multiply);

  // AArch64 has a multiply-accumulate instruction that we can't use
  // here because it has no way to process carries, so we have to use
  // separate add and adc instructions.  Bah.
  umulh(rscratch1, x_xstart, y_idx); // x_xstart * y_idx -> rscratch1:product
  mul(product, x_xstart, y_idx);
  adds(product, product, carry);
  adc(carry, rscratch1, zr);   // x_xstart * y_idx + carry -> carry:product

  subw(kdx, kdx, 2);
  ror(product, product, 32); // back to big-endian
  str(product, offsetted_address(z, kdx, Address::uxtw(LogBytesPerInt), 0, BytesPerLong));

  b(L_first_loop);

  bind(L_one_y);
  ldrw(y_idx, Address(y,  0));
  b(L_multiply);

  bind(L_one_x);
  ldrw(x_xstart, Address(x,  0));
  b(L_first_loop);

  bind(L_first_loop_exit);
}

/**
 * Multiply 128 bit by 128. Unrolled inner loop.
 *
 */
void MacroAssembler::multiply_128_x_128_loop(Register y, Register z,
                                             Register carry, Register carry2,
                                             Register idx, Register jdx,
                                             Register yz_idx1, Register yz_idx2,
                                             Register tmp, Register tmp3, Register tmp4,
                                             Register tmp6, Register product_hi) {

  //   jlong carry, x[], y[], z[];
  //   int kdx = ystart+1;
  //   for (int idx=ystart-2; idx >= 0; idx -= 2) { // Third loop
  //     huge_128 tmp3 = (y[idx+1] * product_hi) + z[kdx+idx+1] + carry;
  //     jlong carry2  = (jlong)(tmp3 >>> 64);
  //     huge_128 tmp4 = (y[idx]   * product_hi) + z[kdx+idx] + carry2;
  //     carry  = (jlong)(tmp4 >>> 64);
  //     z[kdx+idx+1] = (jlong)tmp3;
  //     z[kdx+idx] = (jlong)tmp4;
  //   }
  //   idx += 2;
  //   if (idx > 0) {
  //     yz_idx1 = (y[idx] * product_hi) + z[kdx+idx] + carry;
  //     z[kdx+idx] = (jlong)yz_idx1;
  //     carry  = (jlong)(yz_idx1 >>> 64);
  //   }
  //

  Label L_third_loop, L_third_loop_exit, L_post_third_loop_done;

  lsrw(jdx, idx, 2);

  bind(L_third_loop);

  subsw(jdx, jdx, 1);
  br(Assembler::MI, L_third_loop_exit);
  subw(idx, idx, 4);

  lea(rscratch1, Address(y, idx, Address::uxtw(LogBytesPerInt)));

  ldp(yz_idx2, yz_idx1, Address(rscratch1, 0));

  lea(tmp6, Address(z, idx, Address::uxtw(LogBytesPerInt)));

  ror(yz_idx1, yz_idx1, 32); // convert big-endian to little-endian
  ror(yz_idx2, yz_idx2, 32);

  ldp(rscratch2, rscratch1, Address(tmp6, 0));

  mul(tmp3, product_hi, yz_idx1);  //  yz_idx1 * product_hi -> tmp4:tmp3
  umulh(tmp4, product_hi, yz_idx1);

  ror(rscratch1, rscratch1, 32); // convert big-endian to little-endian
  ror(rscratch2, rscratch2, 32);

  mul(tmp, product_hi, yz_idx2);   //  yz_idx2 * product_hi -> carry2:tmp
  umulh(carry2, product_hi, yz_idx2);

  // propagate sum of both multiplications into carry:tmp4:tmp3
  adds(tmp3, tmp3, carry);
  adc(tmp4, tmp4, zr);
  adds(tmp3, tmp3, rscratch1);
  adcs(tmp4, tmp4, tmp);
  adc(carry, carry2, zr);
  adds(tmp4, tmp4, rscratch2);
  adc(carry, carry, zr);

  ror(tmp3, tmp3, 32); // convert little-endian to big-endian
  ror(tmp4, tmp4, 32);
  stp(tmp4, tmp3, Address(tmp6, 0));

  b(L_third_loop);
  bind (L_third_loop_exit);

  andw (idx, idx, 0x3);
  cbz(idx, L_post_third_loop_done);

  Label L_check_1;
  subsw(idx, idx, 2);
  br(Assembler::MI, L_check_1);

  lea(rscratch1, Address(y, idx, Address::uxtw(LogBytesPerInt)));
  ldr(yz_idx1, Address(rscratch1, 0));
  ror(yz_idx1, yz_idx1, 32);
  mul(tmp3, product_hi, yz_idx1);  //  yz_idx1 * product_hi -> tmp4:tmp3
  umulh(tmp4, product_hi, yz_idx1);
  lea(rscratch1, Address(z, idx, Address::uxtw(LogBytesPerInt)));
  ldr(yz_idx2, Address(rscratch1, 0));
  ror(yz_idx2, yz_idx2, 32);

  add2_with_carry(carry, tmp4, tmp3, carry, yz_idx2);

  ror(tmp3, tmp3, 32);
  str(tmp3, Address(rscratch1, 0));

  bind (L_check_1);

  andw (idx, idx, 0x1);
  subsw(idx, idx, 1);
  br(Assembler::MI, L_post_third_loop_done);
  ldrw(tmp4, Address(y, idx, Address::uxtw(LogBytesPerInt)));
  mul(tmp3, tmp4, product_hi);  //  tmp4 * product_hi -> carry2:tmp3
  umulh(carry2, tmp4, product_hi);
  ldrw(tmp4, Address(z, idx, Address::uxtw(LogBytesPerInt)));

  add2_with_carry(carry2, tmp3, tmp4, carry);

  strw(tmp3, Address(z, idx, Address::uxtw(LogBytesPerInt)));
  extr(carry, carry2, tmp3, 32);

  bind(L_post_third_loop_done);
}

/**
 * Code for BigInteger::multiplyToLen() instrinsic.
 *
 * r0: x
 * r1: xlen
 * r2: y
 * r3: ylen
 * r4:  z
 * r5: zlen
 * r10: tmp1
 * r11: tmp2
 * r12: tmp3
 * r13: tmp4
 * r14: tmp5
 * r15: tmp6
 * r16: tmp7
 *
 */
void MacroAssembler::multiply_to_len(Register x, Register xlen, Register y, Register ylen,
                                     Register z, Register zlen,
                                     Register tmp1, Register tmp2, Register tmp3, Register tmp4,
                                     Register tmp5, Register tmp6, Register product_hi) {

  assert_different_registers(x, xlen, y, ylen, z, zlen, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6);

  const Register idx = tmp1;
  const Register kdx = tmp2;
  const Register xstart = tmp3;

  const Register y_idx = tmp4;
  const Register carry = tmp5;
  const Register product  = xlen;
  const Register x_xstart = zlen;  // reuse register

  // First Loop.
  //
  //  final static long LONG_MASK = 0xffffffffL;
  //  int xstart = xlen - 1;
  //  int ystart = ylen - 1;
  //  long carry = 0;
  //  for (int idx=ystart, kdx=ystart+1+xstart; idx >= 0; idx-, kdx--) {
  //    long product = (y[idx] & LONG_MASK) * (x[xstart] & LONG_MASK) + carry;
  //    z[kdx] = (int)product;
  //    carry = product >>> 32;
  //  }
  //  z[xstart] = (int)carry;
  //

  movw(idx, ylen);      // idx = ylen;
  movw(kdx, zlen);      // kdx = xlen+ylen;
  mov(carry, zr);       // carry = 0;

  Label L_done;

  movw(xstart, xlen);
  subsw(xstart, xstart, 1);
  br(Assembler::MI, L_done);

  multiply_64_x_64_loop(x, xstart, x_xstart, y, y_idx, z, carry, product, idx, kdx);

  Label L_second_loop;
  cbzw(kdx, L_second_loop);

  Label L_carry;
  subw(kdx, kdx, 1);
  cbzw(kdx, L_carry);

  strw(carry, Address(z, kdx, Address::uxtw(LogBytesPerInt)));
  lsr(carry, carry, 32);
  subw(kdx, kdx, 1);

  bind(L_carry);
  strw(carry, Address(z, kdx, Address::uxtw(LogBytesPerInt)));

  // Second and third (nested) loops.
  //
  // for (int i = xstart-1; i >= 0; i--) { // Second loop
  //   carry = 0;
  //   for (int jdx=ystart, k=ystart+1+i; jdx >= 0; jdx--, k--) { // Third loop
  //     long product = (y[jdx] & LONG_MASK) * (x[i] & LONG_MASK) +
  //                    (z[k] & LONG_MASK) + carry;
  //     z[k] = (int)product;
  //     carry = product >>> 32;
  //   }
  //   z[i] = (int)carry;
  // }
  //
  // i = xlen, j = tmp1, k = tmp2, carry = tmp5, x[i] = product_hi

  const Register jdx = tmp1;

  bind(L_second_loop);
  mov(carry, zr);                // carry = 0;
  movw(jdx, ylen);               // j = ystart+1

  subsw(xstart, xstart, 1);      // i = xstart-1;
  br(Assembler::MI, L_done);

  str(z, Address(pre(sp, -4 * wordSize)));

  Label L_last_x;
  lea(z, offsetted_address(z, xstart, Address::uxtw(LogBytesPerInt), 4, BytesPerInt)); // z = z + k - j
  subsw(xstart, xstart, 1);       // i = xstart-1;
  br(Assembler::MI, L_last_x);

  lea(rscratch1, Address(x, xstart, Address::uxtw(LogBytesPerInt)));
  ldr(product_hi, Address(rscratch1));
  ror(product_hi, product_hi, 32);  // convert big-endian to little-endian

  Label L_third_loop_prologue;
  bind(L_third_loop_prologue);

  str(ylen, Address(sp, wordSize));
  stp(x, xstart, Address(sp, 2 * wordSize));
  multiply_128_x_128_loop(y, z, carry, x, jdx, ylen, product,
                          tmp2, x_xstart, tmp3, tmp4, tmp6, product_hi);
  ldp(z, ylen, Address(post(sp, 2 * wordSize)));
  ldp(x, xlen, Address(post(sp, 2 * wordSize)));   // copy old xstart -> xlen

  addw(tmp3, xlen, 1);
  strw(carry, Address(z, tmp3, Address::uxtw(LogBytesPerInt)));
  subsw(tmp3, tmp3, 1);
  br(Assembler::MI, L_done);

  lsr(carry, carry, 32);
  strw(carry, Address(z, tmp3, Address::uxtw(LogBytesPerInt)));
  b(L_second_loop);

  // Next infrequent code is moved outside loops.
  bind(L_last_x);
  ldrw(product_hi, Address(x,  0));
  b(L_third_loop_prologue);

  bind(L_done);
}

// Code for BigInteger::mulAdd instrinsic
// out     = r0
// in      = r1
// offset  = r2  (already out.length-offset)
// len     = r3
// k       = r4
//
// pseudo code from java implementation:
// carry = 0;
// offset = out.length-offset - 1;
// for (int j=len-1; j >= 0; j--) {
//     product = (in[j] & LONG_MASK) * kLong + (out[offset] & LONG_MASK) + carry;
//     out[offset--] = (int)product;
//     carry = product >>> 32;
// }
// return (int)carry;
void MacroAssembler::mul_add(Register out, Register in, Register offset,
      Register len, Register k) {
    Label LOOP, END;
    // pre-loop
    cmp(len, zr); // cmp, not cbz/cbnz: to use condition twice => less branches
    csel(out, zr, out, Assembler::EQ);
    br(Assembler::EQ, END);
    add(in, in, len, LSL, 2); // in[j+1] address
    add(offset, out, offset, LSL, 2); // out[offset + 1] address
    mov(out, zr); // used to keep carry now
    BIND(LOOP);
    ldrw(rscratch1, Address(pre(in, -4)));
    madd(rscratch1, rscratch1, k, out);
    ldrw(rscratch2, Address(pre(offset, -4)));
    add(rscratch1, rscratch1, rscratch2);
    strw(rscratch1, Address(offset));
    lsr(out, rscratch1, 32);
    subs(len, len, 1);
    br(Assembler::NE, LOOP);
    BIND(END);
}

/**
 * Emits code to update CRC-32 with a byte value according to constants in table
 *
 * @param [in,out]crc   Register containing the crc.
 * @param [in]val       Register containing the byte to fold into the CRC.
 * @param [in]table     Register containing the table of crc constants.
 *
 * uint32_t crc;
 * val = crc_table[(val ^ crc) & 0xFF];
 * crc = val ^ (crc >> 8);
 *
 */
void MacroAssembler::update_byte_crc32(Register crc, Register val, Register table) {
  eor(val, val, crc);
  andr(val, val, 0xff);
  ldrw(val, Address(table, val, Address::lsl(2)));
  eor(crc, val, crc, Assembler::LSR, 8);
}

/**
 * Emits code to update CRC-32 with a 32-bit value according to tables 0 to 3
 *
 * @param [in,out]crc   Register containing the crc.
 * @param [in]v         Register containing the 32-bit to fold into the CRC.
 * @param [in]table0    Register containing table 0 of crc constants.
 * @param [in]table1    Register containing table 1 of crc constants.
 * @param [in]table2    Register containing table 2 of crc constants.
 * @param [in]table3    Register containing table 3 of crc constants.
 *
 * uint32_t crc;
 *   v = crc ^ v
 *   crc = table3[v&0xff]^table2[(v>>8)&0xff]^table1[(v>>16)&0xff]^table0[v>>24]
 *
 */
void MacroAssembler::update_word_crc32(Register crc, Register v, Register tmp,
        Register table0, Register table1, Register table2, Register table3,
        bool upper) {
  eor(v, crc, v, upper ? LSR:LSL, upper ? 32:0);
  uxtb(tmp, v);
  ldrw(crc, Address(table3, tmp, Address::lsl(2)));
  ubfx(tmp, v, 8, 8);
  ldrw(tmp, Address(table2, tmp, Address::lsl(2)));
  eor(crc, crc, tmp);
  ubfx(tmp, v, 16, 8);
  ldrw(tmp, Address(table1, tmp, Address::lsl(2)));
  eor(crc, crc, tmp);
  ubfx(tmp, v, 24, 8);
  ldrw(tmp, Address(table0, tmp, Address::lsl(2)));
  eor(crc, crc, tmp);
}

void MacroAssembler::kernel_crc32_using_crc32(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3) {
    Label CRC_by64_loop, CRC_by4_loop, CRC_by1_loop, CRC_less64, CRC_by64_pre, CRC_by32_loop, CRC_less32, L_exit;
    assert_different_registers(crc, buf, len, tmp0, tmp1, tmp2, tmp3);

    mvnw(crc, crc);

    subs(len, len, 128);
    br(Assembler::GE, CRC_by64_pre);
  BIND(CRC_less64);
    adds(len, len, 128-32);
    br(Assembler::GE, CRC_by32_loop);
  BIND(CRC_less32);
    adds(len, len, 32-4);
    br(Assembler::GE, CRC_by4_loop);
    adds(len, len, 4);
    br(Assembler::GT, CRC_by1_loop);
    b(L_exit);

  BIND(CRC_by32_loop);
    ldp(tmp0, tmp1, Address(post(buf, 16)));
    subs(len, len, 32);
    crc32x(crc, crc, tmp0);
    ldr(tmp2, Address(post(buf, 8)));
    crc32x(crc, crc, tmp1);
    ldr(tmp3, Address(post(buf, 8)));
    crc32x(crc, crc, tmp2);
    crc32x(crc, crc, tmp3);
    br(Assembler::GE, CRC_by32_loop);
    cmn(len, 32);
    br(Assembler::NE, CRC_less32);
    b(L_exit);

  BIND(CRC_by4_loop);
    ldrw(tmp0, Address(post(buf, 4)));
    subs(len, len, 4);
    crc32w(crc, crc, tmp0);
    br(Assembler::GE, CRC_by4_loop);
    adds(len, len, 4);
    br(Assembler::LE, L_exit);
  BIND(CRC_by1_loop);
    ldrb(tmp0, Address(post(buf, 1)));
    subs(len, len, 1);
    crc32b(crc, crc, tmp0);
    br(Assembler::GT, CRC_by1_loop);
    b(L_exit);

  BIND(CRC_by64_pre);
    sub(buf, buf, 8);
    ldp(tmp0, tmp1, Address(buf, 8));
    crc32x(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 24));
    crc32x(crc, crc, tmp1);
    ldr(tmp3, Address(buf, 32));
    crc32x(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 40));
    crc32x(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 48));
    crc32x(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 56));
    crc32x(crc, crc, tmp1);
    ldr(tmp3, Address(pre(buf, 64)));

    b(CRC_by64_loop);

    align(CodeEntryAlignment);
  BIND(CRC_by64_loop);
    subs(len, len, 64);
    crc32x(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 8));
    crc32x(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 16));
    crc32x(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 24));
    crc32x(crc, crc, tmp1);
    ldr(tmp3, Address(buf, 32));
    crc32x(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 40));
    crc32x(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 48));
    crc32x(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 56));
    crc32x(crc, crc, tmp1);
    ldr(tmp3, Address(pre(buf, 64)));
    br(Assembler::GE, CRC_by64_loop);

    // post-loop
    crc32x(crc, crc, tmp2);
    crc32x(crc, crc, tmp3);

    sub(len, len, 64);
    add(buf, buf, 8);
    cmn(len, 128);
    br(Assembler::NE, CRC_less64);
  BIND(L_exit);
    mvnw(crc, crc);
}

/**
 * @param crc   register containing existing CRC (32-bit)
 * @param buf   register pointing to input byte buffer (byte*)
 * @param len   register containing number of bytes
 * @param table register that will contain address of CRC table
 * @param tmp   scratch register
 */
void MacroAssembler::kernel_crc32(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3) {
  Label L_by16, L_by16_loop, L_by4, L_by4_loop, L_by1, L_by1_loop, L_exit;
  uint64_t offset;

  if (UseCRC32) {
      kernel_crc32_using_crc32(crc, buf, len, table0, table1, table2, table3);
      return;
  }

    mvnw(crc, crc);

    adrp(table0, ExternalAddress(StubRoutines::crc_table_addr()), offset);
    if (offset) add(table0, table0, offset);
    add(table1, table0, 1*256*sizeof(juint));
    add(table2, table0, 2*256*sizeof(juint));
    add(table3, table0, 3*256*sizeof(juint));

  if (UseNeon) {
      cmp(len, (u1)64);
      br(Assembler::LT, L_by16);
      eor(v16, T16B, v16, v16);

    Label L_fold;

      add(tmp, table0, 4*256*sizeof(juint)); // Point at the Neon constants

      ld1(v0, v1, T2D, post(buf, 32));
      ld1r(v4, T2D, post(tmp, 8));
      ld1r(v5, T2D, post(tmp, 8));
      ld1r(v6, T2D, post(tmp, 8));
      ld1r(v7, T2D, post(tmp, 8));
      mov(v16, T4S, 0, crc);

      eor(v0, T16B, v0, v16);
      sub(len, len, 64);

    BIND(L_fold);
      pmull(v22, T8H, v0, v5, T8B);
      pmull(v20, T8H, v0, v7, T8B);
      pmull(v23, T8H, v0, v4, T8B);
      pmull(v21, T8H, v0, v6, T8B);

      pmull2(v18, T8H, v0, v5, T16B);
      pmull2(v16, T8H, v0, v7, T16B);
      pmull2(v19, T8H, v0, v4, T16B);
      pmull2(v17, T8H, v0, v6, T16B);

      uzp1(v24, T8H, v20, v22);
      uzp2(v25, T8H, v20, v22);
      eor(v20, T16B, v24, v25);

      uzp1(v26, T8H, v16, v18);
      uzp2(v27, T8H, v16, v18);
      eor(v16, T16B, v26, v27);

      ushll2(v22, T4S, v20, T8H, 8);
      ushll(v20, T4S, v20, T4H, 8);

      ushll2(v18, T4S, v16, T8H, 8);
      ushll(v16, T4S, v16, T4H, 8);

      eor(v22, T16B, v23, v22);
      eor(v18, T16B, v19, v18);
      eor(v20, T16B, v21, v20);
      eor(v16, T16B, v17, v16);

      uzp1(v17, T2D, v16, v20);
      uzp2(v21, T2D, v16, v20);
      eor(v17, T16B, v17, v21);

      ushll2(v20, T2D, v17, T4S, 16);
      ushll(v16, T2D, v17, T2S, 16);

      eor(v20, T16B, v20, v22);
      eor(v16, T16B, v16, v18);

      uzp1(v17, T2D, v20, v16);
      uzp2(v21, T2D, v20, v16);
      eor(v28, T16B, v17, v21);

      pmull(v22, T8H, v1, v5, T8B);
      pmull(v20, T8H, v1, v7, T8B);
      pmull(v23, T8H, v1, v4, T8B);
      pmull(v21, T8H, v1, v6, T8B);

      pmull2(v18, T8H, v1, v5, T16B);
      pmull2(v16, T8H, v1, v7, T16B);
      pmull2(v19, T8H, v1, v4, T16B);
      pmull2(v17, T8H, v1, v6, T16B);

      ld1(v0, v1, T2D, post(buf, 32));

      uzp1(v24, T8H, v20, v22);
      uzp2(v25, T8H, v20, v22);
      eor(v20, T16B, v24, v25);

      uzp1(v26, T8H, v16, v18);
      uzp2(v27, T8H, v16, v18);
      eor(v16, T16B, v26, v27);

      ushll2(v22, T4S, v20, T8H, 8);
      ushll(v20, T4S, v20, T4H, 8);

      ushll2(v18, T4S, v16, T8H, 8);
      ushll(v16, T4S, v16, T4H, 8);

      eor(v22, T16B, v23, v22);
      eor(v18, T16B, v19, v18);
      eor(v20, T16B, v21, v20);
      eor(v16, T16B, v17, v16);

      uzp1(v17, T2D, v16, v20);
      uzp2(v21, T2D, v16, v20);
      eor(v16, T16B, v17, v21);

      ushll2(v20, T2D, v16, T4S, 16);
      ushll(v16, T2D, v16, T2S, 16);

      eor(v20, T16B, v22, v20);
      eor(v16, T16B, v16, v18);

      uzp1(v17, T2D, v20, v16);
      uzp2(v21, T2D, v20, v16);
      eor(v20, T16B, v17, v21);

      shl(v16, T2D, v28, 1);
      shl(v17, T2D, v20, 1);

      eor(v0, T16B, v0, v16);
      eor(v1, T16B, v1, v17);

      subs(len, len, 32);
      br(Assembler::GE, L_fold);

      mov(crc, 0);
      mov(tmp, v0, T1D, 0);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, false);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, true);
      mov(tmp, v0, T1D, 1);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, false);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, true);
      mov(tmp, v1, T1D, 0);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, false);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, true);
      mov(tmp, v1, T1D, 1);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, false);
      update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, true);

      add(len, len, 32);
  }

  BIND(L_by16);
    subs(len, len, 16);
    br(Assembler::GE, L_by16_loop);
    adds(len, len, 16-4);
    br(Assembler::GE, L_by4_loop);
    adds(len, len, 4);
    br(Assembler::GT, L_by1_loop);
    b(L_exit);

  BIND(L_by4_loop);
    ldrw(tmp, Address(post(buf, 4)));
    update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3);
    subs(len, len, 4);
    br(Assembler::GE, L_by4_loop);
    adds(len, len, 4);
    br(Assembler::LE, L_exit);
  BIND(L_by1_loop);
    subs(len, len, 1);
    ldrb(tmp, Address(post(buf, 1)));
    update_byte_crc32(crc, tmp, table0);
    br(Assembler::GT, L_by1_loop);
    b(L_exit);

    align(CodeEntryAlignment);
  BIND(L_by16_loop);
    subs(len, len, 16);
    ldp(tmp, tmp3, Address(post(buf, 16)));
    update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, false);
    update_word_crc32(crc, tmp, tmp2, table0, table1, table2, table3, true);
    update_word_crc32(crc, tmp3, tmp2, table0, table1, table2, table3, false);
    update_word_crc32(crc, tmp3, tmp2, table0, table1, table2, table3, true);
    br(Assembler::GE, L_by16_loop);
    adds(len, len, 16-4);
    br(Assembler::GE, L_by4_loop);
    adds(len, len, 4);
    br(Assembler::GT, L_by1_loop);
  BIND(L_exit);
    mvnw(crc, crc);
}

void MacroAssembler::kernel_crc32c_using_crc32c(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3) {
    Label CRC_by64_loop, CRC_by4_loop, CRC_by1_loop, CRC_less64, CRC_by64_pre, CRC_by32_loop, CRC_less32, L_exit;
    assert_different_registers(crc, buf, len, tmp0, tmp1, tmp2, tmp3);

    subs(len, len, 128);
    br(Assembler::GE, CRC_by64_pre);
  BIND(CRC_less64);
    adds(len, len, 128-32);
    br(Assembler::GE, CRC_by32_loop);
  BIND(CRC_less32);
    adds(len, len, 32-4);
    br(Assembler::GE, CRC_by4_loop);
    adds(len, len, 4);
    br(Assembler::GT, CRC_by1_loop);
    b(L_exit);

  BIND(CRC_by32_loop);
    ldp(tmp0, tmp1, Address(post(buf, 16)));
    subs(len, len, 32);
    crc32cx(crc, crc, tmp0);
    ldr(tmp2, Address(post(buf, 8)));
    crc32cx(crc, crc, tmp1);
    ldr(tmp3, Address(post(buf, 8)));
    crc32cx(crc, crc, tmp2);
    crc32cx(crc, crc, tmp3);
    br(Assembler::GE, CRC_by32_loop);
    cmn(len, 32);
    br(Assembler::NE, CRC_less32);
    b(L_exit);

  BIND(CRC_by4_loop);
    ldrw(tmp0, Address(post(buf, 4)));
    subs(len, len, 4);
    crc32cw(crc, crc, tmp0);
    br(Assembler::GE, CRC_by4_loop);
    adds(len, len, 4);
    br(Assembler::LE, L_exit);
  BIND(CRC_by1_loop);
    ldrb(tmp0, Address(post(buf, 1)));
    subs(len, len, 1);
    crc32cb(crc, crc, tmp0);
    br(Assembler::GT, CRC_by1_loop);
    b(L_exit);

  BIND(CRC_by64_pre);
    sub(buf, buf, 8);
    ldp(tmp0, tmp1, Address(buf, 8));
    crc32cx(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 24));
    crc32cx(crc, crc, tmp1);
    ldr(tmp3, Address(buf, 32));
    crc32cx(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 40));
    crc32cx(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 48));
    crc32cx(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 56));
    crc32cx(crc, crc, tmp1);
    ldr(tmp3, Address(pre(buf, 64)));

    b(CRC_by64_loop);

    align(CodeEntryAlignment);
  BIND(CRC_by64_loop);
    subs(len, len, 64);
    crc32cx(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 8));
    crc32cx(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 16));
    crc32cx(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 24));
    crc32cx(crc, crc, tmp1);
    ldr(tmp3, Address(buf, 32));
    crc32cx(crc, crc, tmp2);
    ldr(tmp0, Address(buf, 40));
    crc32cx(crc, crc, tmp3);
    ldr(tmp1, Address(buf, 48));
    crc32cx(crc, crc, tmp0);
    ldr(tmp2, Address(buf, 56));
    crc32cx(crc, crc, tmp1);
    ldr(tmp3, Address(pre(buf, 64)));
    br(Assembler::GE, CRC_by64_loop);

    // post-loop
    crc32cx(crc, crc, tmp2);
    crc32cx(crc, crc, tmp3);

    sub(len, len, 64);
    add(buf, buf, 8);
    cmn(len, 128);
    br(Assembler::NE, CRC_less64);
  BIND(L_exit);
}

/**
 * @param crc   register containing existing CRC (32-bit)
 * @param buf   register pointing to input byte buffer (byte*)
 * @param len   register containing number of bytes
 * @param table register that will contain address of CRC table
 * @param tmp   scratch register
 */
void MacroAssembler::kernel_crc32c(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3) {
  kernel_crc32c_using_crc32c(crc, buf, len, table0, table1, table2, table3);
}


SkipIfEqual::SkipIfEqual(
    MacroAssembler* masm, const bool* flag_addr, bool value) {
  _masm = masm;
  uint64_t offset;
  _masm->adrp(rscratch1, ExternalAddress((address)flag_addr), offset);
  _masm->ldrb(rscratch1, Address(rscratch1, offset));
  _masm->cbzw(rscratch1, _label);
}

SkipIfEqual::~SkipIfEqual() {
  _masm->bind(_label);
}

void MacroAssembler::addptr(const Address &dst, int32_t src) {
  Address adr;
  switch(dst.getMode()) {
  case Address::base_plus_offset:
    // This is the expected mode, although we allow all the other
    // forms below.
    adr = form_address(rscratch2, dst.base(), dst.offset(), LogBytesPerWord);
    break;
  default:
    lea(rscratch2, dst);
    adr = Address(rscratch2);
    break;
  }
  ldr(rscratch1, adr);
  add(rscratch1, rscratch1, src);
  str(rscratch1, adr);
}

void MacroAssembler::cmpptr(Register src1, Address src2) {
  uint64_t offset;
  adrp(rscratch1, src2, offset);
  ldr(rscratch1, Address(rscratch1, offset));
  cmp(src1, rscratch1);
}

void MacroAssembler::cmpoop(Register obj1, Register obj2) {
  cmp(obj1, obj2);
}

void MacroAssembler::load_method_holder_cld(Register rresult, Register rmethod) {
  load_method_holder(rresult, rmethod);
  ldr(rresult, Address(rresult, InstanceKlass::class_loader_data_offset()));
}

void MacroAssembler::load_method_holder(Register holder, Register method) {
  ldr(holder, Address(method, Method::const_offset()));                      // ConstMethod*
  ldr(holder, Address(holder, ConstMethod::constants_offset()));             // ConstantPool*
  ldr(holder, Address(holder, ConstantPool::pool_holder_offset_in_bytes())); // InstanceKlass*
}

void MacroAssembler::load_klass(Register dst, Register src) {
  if (UseCompressedClassPointers) {
    ldrw(dst, Address(src, oopDesc::klass_offset_in_bytes()));
    decode_klass_not_null(dst);
  } else {
    ldr(dst, Address(src, oopDesc::klass_offset_in_bytes()));
  }
}

// ((OopHandle)result).resolve();
void MacroAssembler::resolve_oop_handle(Register result, Register tmp) {
  // OopHandle::resolve is an indirection.
  access_load_at(T_OBJECT, IN_NATIVE, result, Address(result, 0), tmp, noreg);
}

// ((WeakHandle)result).resolve();
void MacroAssembler::resolve_weak_handle(Register rresult, Register rtmp) {
  assert_different_registers(rresult, rtmp);
  Label resolved;

  // A null weak handle resolves to null.
  cbz(rresult, resolved);

  // Only 64 bit platforms support GCs that require a tmp register
  // Only IN_HEAP loads require a thread_tmp register
  // WeakHandle::resolve is an indirection like jweak.
  access_load_at(T_OBJECT, IN_NATIVE | ON_PHANTOM_OOP_REF,
                 rresult, Address(rresult), rtmp, /*tmp_thread*/noreg);
  bind(resolved);
}

void MacroAssembler::load_mirror(Register dst, Register method, Register tmp) {
  const int mirror_offset = in_bytes(Klass::java_mirror_offset());
  ldr(dst, Address(rmethod, Method::const_offset()));
  ldr(dst, Address(dst, ConstMethod::constants_offset()));
  ldr(dst, Address(dst, ConstantPool::pool_holder_offset_in_bytes()));
  ldr(dst, Address(dst, mirror_offset));
  resolve_oop_handle(dst, tmp);
}

void MacroAssembler::cmp_klass(Register oop, Register trial_klass, Register tmp) {
  if (UseCompressedClassPointers) {
    ldrw(tmp, Address(oop, oopDesc::klass_offset_in_bytes()));
    if (CompressedKlassPointers::base() == NULL) {
      cmp(trial_klass, tmp, LSL, CompressedKlassPointers::shift());
      return;
    } else if (((uint64_t)CompressedKlassPointers::base() & 0xffffffff) == 0
               && CompressedKlassPointers::shift() == 0) {
      // Only the bottom 32 bits matter
      cmpw(trial_klass, tmp);
      return;
    }
    decode_klass_not_null(tmp);
  } else {
    ldr(tmp, Address(oop, oopDesc::klass_offset_in_bytes()));
  }
  cmp(trial_klass, tmp);
}

void MacroAssembler::store_klass(Register dst, Register src) {
  // FIXME: Should this be a store release?  concurrent gcs assumes
  // klass length is valid if klass field is not null.
  if (UseCompressedClassPointers) {
    encode_klass_not_null(src);
    strw(src, Address(dst, oopDesc::klass_offset_in_bytes()));
  } else {
    str(src, Address(dst, oopDesc::klass_offset_in_bytes()));
  }
}

void MacroAssembler::store_klass_gap(Register dst, Register src) {
  if (UseCompressedClassPointers) {
    // Store to klass gap in destination
    strw(src, Address(dst, oopDesc::klass_gap_offset_in_bytes()));
  }
}

// Algorithm must match CompressedOops::encode.
void MacroAssembler::encode_heap_oop(Register d, Register s) {
#ifdef ASSERT
  verify_heapbase("MacroAssembler::encode_heap_oop: heap base corrupted?");
#endif
  verify_oop(s, "broken oop in encode_heap_oop");
  if (CompressedOops::base() == NULL) {
    if (CompressedOops::shift() != 0) {
      assert (LogMinObjAlignmentInBytes == CompressedOops::shift(), "decode alg wrong");
      lsr(d, s, LogMinObjAlignmentInBytes);
    } else {
      mov(d, s);
    }
  } else {
    subs(d, s, rheapbase);
    csel(d, d, zr, Assembler::HS);
    lsr(d, d, LogMinObjAlignmentInBytes);

    /*  Old algorithm: is this any worse?
    Label nonnull;
    cbnz(r, nonnull);
    sub(r, r, rheapbase);
    bind(nonnull);
    lsr(r, r, LogMinObjAlignmentInBytes);
    */
  }
}

void MacroAssembler::encode_heap_oop_not_null(Register r) {
#ifdef ASSERT
  verify_heapbase("MacroAssembler::encode_heap_oop_not_null: heap base corrupted?");
  if (CheckCompressedOops) {
    Label ok;
    cbnz(r, ok);
    stop("null oop passed to encode_heap_oop_not_null");
    bind(ok);
  }
#endif
  verify_oop(r, "broken oop in encode_heap_oop_not_null");
  if (CompressedOops::base() != NULL) {
    sub(r, r, rheapbase);
  }
  if (CompressedOops::shift() != 0) {
    assert (LogMinObjAlignmentInBytes == CompressedOops::shift(), "decode alg wrong");
    lsr(r, r, LogMinObjAlignmentInBytes);
  }
}

void MacroAssembler::encode_heap_oop_not_null(Register dst, Register src) {
#ifdef ASSERT
  verify_heapbase("MacroAssembler::encode_heap_oop_not_null2: heap base corrupted?");
  if (CheckCompressedOops) {
    Label ok;
    cbnz(src, ok);
    stop("null oop passed to encode_heap_oop_not_null2");
    bind(ok);
  }
#endif
  verify_oop(src, "broken oop in encode_heap_oop_not_null2");

  Register data = src;
  if (CompressedOops::base() != NULL) {
    sub(dst, src, rheapbase);
    data = dst;
  }
  if (CompressedOops::shift() != 0) {
    assert (LogMinObjAlignmentInBytes == CompressedOops::shift(), "decode alg wrong");
    lsr(dst, data, LogMinObjAlignmentInBytes);
    data = dst;
  }
  if (data == src)
    mov(dst, src);
}

void  MacroAssembler::decode_heap_oop(Register d, Register s) {
#ifdef ASSERT
  verify_heapbase("MacroAssembler::decode_heap_oop: heap base corrupted?");
#endif
  if (CompressedOops::base() == NULL) {
    if (CompressedOops::shift() != 0 || d != s) {
      lsl(d, s, CompressedOops::shift());
    }
  } else {
    Label done;
    if (d != s)
      mov(d, s);
    cbz(s, done);
    add(d, rheapbase, s, Assembler::LSL, LogMinObjAlignmentInBytes);
    bind(done);
  }
  verify_oop(d, "broken oop in decode_heap_oop");
}

void  MacroAssembler::decode_heap_oop_not_null(Register r) {
  assert (UseCompressedOops, "should only be used for compressed headers");
  assert (Universe::heap() != NULL, "java heap should be initialized");
  // Cannot assert, unverified entry point counts instructions (see .ad file)
  // vtableStubs also counts instructions in pd_code_size_limit.
  // Also do not verify_oop as this is called by verify_oop.
  if (CompressedOops::shift() != 0) {
    assert(LogMinObjAlignmentInBytes == CompressedOops::shift(), "decode alg wrong");
    if (CompressedOops::base() != NULL) {
      add(r, rheapbase, r, Assembler::LSL, LogMinObjAlignmentInBytes);
    } else {
      add(r, zr, r, Assembler::LSL, LogMinObjAlignmentInBytes);
    }
  } else {
    assert (CompressedOops::base() == NULL, "sanity");
  }
}

void  MacroAssembler::decode_heap_oop_not_null(Register dst, Register src) {
  assert (UseCompressedOops, "should only be used for compressed headers");
  assert (Universe::heap() != NULL, "java heap should be initialized");
  // Cannot assert, unverified entry point counts instructions (see .ad file)
  // vtableStubs also counts instructions in pd_code_size_limit.
  // Also do not verify_oop as this is called by verify_oop.
  if (CompressedOops::shift() != 0) {
    assert(LogMinObjAlignmentInBytes == CompressedOops::shift(), "decode alg wrong");
    if (CompressedOops::base() != NULL) {
      add(dst, rheapbase, src, Assembler::LSL, LogMinObjAlignmentInBytes);
    } else {
      add(dst, zr, src, Assembler::LSL, LogMinObjAlignmentInBytes);
    }
  } else {
    assert (CompressedOops::base() == NULL, "sanity");
    if (dst != src) {
      mov(dst, src);
    }
  }
}

MacroAssembler::KlassDecodeMode MacroAssembler::_klass_decode_mode(KlassDecodeNone);

MacroAssembler::KlassDecodeMode MacroAssembler::klass_decode_mode() {
  assert(UseCompressedClassPointers, "not using compressed class pointers");
  assert(Metaspace::initialized(), "metaspace not initialized yet");

  if (_klass_decode_mode != KlassDecodeNone) {
    return _klass_decode_mode;
  }

  assert(LogKlassAlignmentInBytes == CompressedKlassPointers::shift()
         || 0 == CompressedKlassPointers::shift(), "decode alg wrong");

  if (CompressedKlassPointers::base() == NULL) {
    return (_klass_decode_mode = KlassDecodeZero);
  }

  if (operand_valid_for_logical_immediate(
        /*is32*/false, (uint64_t)CompressedKlassPointers::base())) {
    const uint64_t range_mask =
      (1ULL << log2i(CompressedKlassPointers::range())) - 1;
    if (((uint64_t)CompressedKlassPointers::base() & range_mask) == 0) {
      return (_klass_decode_mode = KlassDecodeXor);
    }
  }

  const uint64_t shifted_base =
    (uint64_t)CompressedKlassPointers::base() >> CompressedKlassPointers::shift();
  guarantee((shifted_base & 0xffff0000ffffffff) == 0,
            "compressed class base bad alignment");

  return (_klass_decode_mode = KlassDecodeMovk);
}

void MacroAssembler::encode_klass_not_null(Register dst, Register src) {
  switch (klass_decode_mode()) {
  case KlassDecodeZero:
    if (CompressedKlassPointers::shift() != 0) {
      lsr(dst, src, LogKlassAlignmentInBytes);
    } else {
      if (dst != src) mov(dst, src);
    }
    break;

  case KlassDecodeXor:
    if (CompressedKlassPointers::shift() != 0) {
      eor(dst, src, (uint64_t)CompressedKlassPointers::base());
      lsr(dst, dst, LogKlassAlignmentInBytes);
    } else {
      eor(dst, src, (uint64_t)CompressedKlassPointers::base());
    }
    break;

  case KlassDecodeMovk:
    if (CompressedKlassPointers::shift() != 0) {
      ubfx(dst, src, LogKlassAlignmentInBytes, 32);
    } else {
      movw(dst, src);
    }
    break;

  case KlassDecodeNone:
    ShouldNotReachHere();
    break;
  }
}

void MacroAssembler::encode_klass_not_null(Register r) {
  encode_klass_not_null(r, r);
}

void  MacroAssembler::decode_klass_not_null(Register dst, Register src) {
  assert (UseCompressedClassPointers, "should only be used for compressed headers");

  switch (klass_decode_mode()) {
  case KlassDecodeZero:
    if (CompressedKlassPointers::shift() != 0) {
      lsl(dst, src, LogKlassAlignmentInBytes);
    } else {
      if (dst != src) mov(dst, src);
    }
    break;

  case KlassDecodeXor:
    if (CompressedKlassPointers::shift() != 0) {
      lsl(dst, src, LogKlassAlignmentInBytes);
      eor(dst, dst, (uint64_t)CompressedKlassPointers::base());
    } else {
      eor(dst, src, (uint64_t)CompressedKlassPointers::base());
    }
    break;

  case KlassDecodeMovk: {
    const uint64_t shifted_base =
      (uint64_t)CompressedKlassPointers::base() >> CompressedKlassPointers::shift();

    if (dst != src) movw(dst, src);
    movk(dst, shifted_base >> 32, 32);

    if (CompressedKlassPointers::shift() != 0) {
      lsl(dst, dst, LogKlassAlignmentInBytes);
    }

    break;
  }

  case KlassDecodeNone:
    ShouldNotReachHere();
    break;
  }
}

void  MacroAssembler::decode_klass_not_null(Register r) {
  decode_klass_not_null(r, r);
}

void  MacroAssembler::set_narrow_oop(Register dst, jobject obj) {
#ifdef ASSERT
  {
    ThreadInVMfromUnknown tiv;
    assert (UseCompressedOops, "should only be used for compressed oops");
    assert (Universe::heap() != NULL, "java heap should be initialized");
    assert (oop_recorder() != NULL, "this assembler needs an OopRecorder");
    assert(Universe::heap()->is_in(JNIHandles::resolve(obj)), "should be real oop");
  }
#endif
  int oop_index = oop_recorder()->find_index(obj);
  InstructionMark im(this);
  RelocationHolder rspec = oop_Relocation::spec(oop_index);
  code_section()->relocate(inst_mark(), rspec);
  movz(dst, 0xDEAD, 16);
  movk(dst, 0xBEEF);
}

void  MacroAssembler::set_narrow_klass(Register dst, Klass* k) {
  assert (UseCompressedClassPointers, "should only be used for compressed headers");
  assert (oop_recorder() != NULL, "this assembler needs an OopRecorder");
  int index = oop_recorder()->find_index(k);
  assert(! Universe::heap()->is_in(k), "should not be an oop");

  InstructionMark im(this);
  RelocationHolder rspec = metadata_Relocation::spec(index);
  code_section()->relocate(inst_mark(), rspec);
  narrowKlass nk = CompressedKlassPointers::encode(k);
  movz(dst, (nk >> 16), 16);
  movk(dst, nk & 0xffff);
}

void MacroAssembler::access_load_at(BasicType type, DecoratorSet decorators,
                                    Register dst, Address src,
                                    Register tmp1, Register thread_tmp) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::load_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  } else {
    bs->load_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  }
}

void MacroAssembler::access_store_at(BasicType type, DecoratorSet decorators,
                                     Address dst, Register src,
                                     Register tmp1, Register thread_tmp) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::store_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  } else {
    bs->store_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  }
}

void MacroAssembler::load_heap_oop(Register dst, Address src, Register tmp1,
                                   Register thread_tmp, DecoratorSet decorators) {
  access_load_at(T_OBJECT, IN_HEAP | decorators, dst, src, tmp1, thread_tmp);
}

void MacroAssembler::load_heap_oop_not_null(Register dst, Address src, Register tmp1,
                                            Register thread_tmp, DecoratorSet decorators) {
  access_load_at(T_OBJECT, IN_HEAP | IS_NOT_NULL | decorators, dst, src, tmp1, thread_tmp);
}

void MacroAssembler::store_heap_oop(Address dst, Register src, Register tmp1,
                                    Register thread_tmp, DecoratorSet decorators) {
  access_store_at(T_OBJECT, IN_HEAP | decorators, dst, src, tmp1, thread_tmp);
}

// Used for storing NULLs.
void MacroAssembler::store_heap_oop_null(Address dst) {
  access_store_at(T_OBJECT, IN_HEAP, dst, noreg, noreg, noreg);
}

Address MacroAssembler::allocate_metadata_address(Metadata* obj) {
  assert(oop_recorder() != NULL, "this assembler needs a Recorder");
  int index = oop_recorder()->allocate_metadata_index(obj);
  RelocationHolder rspec = metadata_Relocation::spec(index);
  return Address((address)obj, rspec);
}

// Move an oop into a register.  immediate is true if we want
// immediate instructions and nmethod entry barriers are not enabled.
// i.e. we are not going to patch this instruction while the code is being
// executed by another thread.
void MacroAssembler::movoop(Register dst, jobject obj, bool immediate) {
  int oop_index;
  if (obj == NULL) {
    oop_index = oop_recorder()->allocate_oop_index(obj);
  } else {
#ifdef ASSERT
    {
      ThreadInVMfromUnknown tiv;
      assert(Universe::heap()->is_in(JNIHandles::resolve(obj)), "should be real oop");
    }
#endif
    oop_index = oop_recorder()->find_index(obj);
  }
  RelocationHolder rspec = oop_Relocation::spec(oop_index);

  // nmethod entry barrier necessitate using the constant pool. They have to be
  // ordered with respected to oop accesses.
  // Using immediate literals would necessitate ISBs.
  if (BarrierSet::barrier_set()->barrier_set_nmethod() != NULL || !immediate) {
    address dummy = address(uintptr_t(pc()) & -wordSize); // A nearby aligned address
    ldr_constant(dst, Address(dummy, rspec));
  } else
    mov(dst, Address((address)obj, rspec));

}

// Move a metadata address into a register.
void MacroAssembler::mov_metadata(Register dst, Metadata* obj) {
  int oop_index;
  if (obj == NULL) {
    oop_index = oop_recorder()->allocate_metadata_index(obj);
  } else {
    oop_index = oop_recorder()->find_index(obj);
  }
  RelocationHolder rspec = metadata_Relocation::spec(oop_index);
  mov(dst, Address((address)obj, rspec));
}

Address MacroAssembler::constant_oop_address(jobject obj) {
#ifdef ASSERT
  {
    ThreadInVMfromUnknown tiv;
    assert(oop_recorder() != NULL, "this assembler needs an OopRecorder");
    assert(Universe::heap()->is_in(JNIHandles::resolve(obj)), "not an oop");
  }
#endif
  int oop_index = oop_recorder()->find_index(obj);
  return Address((address)obj, oop_Relocation::spec(oop_index));
}

// Defines obj, preserves var_size_in_bytes, okay for t2 == var_size_in_bytes.
void MacroAssembler::tlab_allocate(Register obj,
                                   Register var_size_in_bytes,
                                   int con_size_in_bytes,
                                   Register t1,
                                   Register t2,
                                   Label& slow_case) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->tlab_allocate(this, obj, var_size_in_bytes, con_size_in_bytes, t1, t2, slow_case);
}

// Defines obj, preserves var_size_in_bytes
void MacroAssembler::eden_allocate(Register obj,
                                   Register var_size_in_bytes,
                                   int con_size_in_bytes,
                                   Register t1,
                                   Label& slow_case) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->eden_allocate(this, obj, var_size_in_bytes, con_size_in_bytes, t1, slow_case);
}

void MacroAssembler::verify_tlab() {
#ifdef ASSERT
  if (UseTLAB && VerifyOops) {
    Label next, ok;

    stp(rscratch2, rscratch1, Address(pre(sp, -16)));

    ldr(rscratch2, Address(rthread, in_bytes(JavaThread::tlab_top_offset())));
    ldr(rscratch1, Address(rthread, in_bytes(JavaThread::tlab_start_offset())));
    cmp(rscratch2, rscratch1);
    br(Assembler::HS, next);
    STOP("assert(top >= start)");
    should_not_reach_here();

    bind(next);
    ldr(rscratch2, Address(rthread, in_bytes(JavaThread::tlab_end_offset())));
    ldr(rscratch1, Address(rthread, in_bytes(JavaThread::tlab_top_offset())));
    cmp(rscratch2, rscratch1);
    br(Assembler::HS, ok);
    STOP("assert(top <= end)");
    should_not_reach_here();

    bind(ok);
    ldp(rscratch2, rscratch1, Address(post(sp, 16)));
  }
#endif
}

// Writes to stack successive pages until offset reached to check for
// stack overflow + shadow pages.  This clobbers tmp.
void MacroAssembler::bang_stack_size(Register size, Register tmp) {
  assert_different_registers(tmp, size, rscratch1);
  mov(tmp, sp);
  // Bang stack for total size given plus shadow page size.
  // Bang one page at a time because large size can bang beyond yellow and
  // red zones.
  Label loop;
  mov(rscratch1, os::vm_page_size());
  bind(loop);
  lea(tmp, Address(tmp, -os::vm_page_size()));
  subsw(size, size, rscratch1);
  str(size, Address(tmp));
  br(Assembler::GT, loop);

  // Bang down shadow pages too.
  // At this point, (tmp-0) is the last address touched, so don't
  // touch it again.  (It was touched as (tmp-pagesize) but then tmp
  // was post-decremented.)  Skip this address by starting at i=1, and
  // touch a few more pages below.  N.B.  It is important to touch all
  // the way down to and including i=StackShadowPages.
  for (int i = 0; i < (int)(StackOverflow::stack_shadow_zone_size() / os::vm_page_size()) - 1; i++) {
    // this could be any sized move but this is can be a debugging crumb
    // so the bigger the better.
    lea(tmp, Address(tmp, -os::vm_page_size()));
    str(size, Address(tmp));
  }
}

// Move the address of the polling page into dest.
void MacroAssembler::get_polling_page(Register dest, relocInfo::relocType rtype) {
  ldr(dest, Address(rthread, JavaThread::polling_page_offset()));
}

// Read the polling page.  The address of the polling page must
// already be in r.
address MacroAssembler::read_polling_page(Register r, relocInfo::relocType rtype) {
  address mark;
  {
    InstructionMark im(this);
    code_section()->relocate(inst_mark(), rtype);
    ldrw(zr, Address(r, 0));
    mark = inst_mark();
  }
  verify_cross_modify_fence_not_required();
  return mark;
}

void MacroAssembler::adrp(Register reg1, const Address &dest, uint64_t &byte_offset) {
  relocInfo::relocType rtype = dest.rspec().reloc()->type();
  uint64_t low_page = (uint64_t)CodeCache::low_bound() >> 12;
  uint64_t high_page = (uint64_t)(CodeCache::high_bound()-1) >> 12;
  uint64_t dest_page = (uint64_t)dest.target() >> 12;
  int64_t offset_low = dest_page - low_page;
  int64_t offset_high = dest_page - high_page;

  assert(is_valid_AArch64_address(dest.target()), "bad address");
  assert(dest.getMode() == Address::literal, "ADRP must be applied to a literal address");

  InstructionMark im(this);
  code_section()->relocate(inst_mark(), dest.rspec());
  // 8143067: Ensure that the adrp can reach the dest from anywhere within
  // the code cache so that if it is relocated we know it will still reach
  if (offset_high >= -(1<<20) && offset_low < (1<<20)) {
    _adrp(reg1, dest.target());
  } else {
    uint64_t target = (uint64_t)dest.target();
    uint64_t adrp_target
      = (target & 0xffffffffULL) | ((uint64_t)pc() & 0xffff00000000ULL);

    _adrp(reg1, (address)adrp_target);
    movk(reg1, target >> 32, 32);
  }
  byte_offset = (uint64_t)dest.target() & 0xfff;
}

void MacroAssembler::load_byte_map_base(Register reg) {
  CardTable::CardValue* byte_map_base =
    ((CardTableBarrierSet*)(BarrierSet::barrier_set()))->card_table()->byte_map_base();

  // Strictly speaking the byte_map_base isn't an address at all, and it might
  // even be negative. It is thus materialised as a constant.
  mov(reg, (uint64_t)byte_map_base);
}

void MacroAssembler::build_frame(int framesize) {
  assert(framesize >= 2 * wordSize, "framesize must include space for FP/LR");
  assert(framesize % (2*wordSize) == 0, "must preserve 2*wordSize alignment");
  if (framesize < ((1 << 9) + 2 * wordSize)) {
    sub(sp, sp, framesize);
    stp(rfp, lr, Address(sp, framesize - 2 * wordSize));
    if (PreserveFramePointer) add(rfp, sp, framesize - 2 * wordSize);
  } else {
    stp(rfp, lr, Address(pre(sp, -2 * wordSize)));
    if (PreserveFramePointer) mov(rfp, sp);
    if (framesize < ((1 << 12) + 2 * wordSize))
      sub(sp, sp, framesize - 2 * wordSize);
    else {
      mov(rscratch1, framesize - 2 * wordSize);
      sub(sp, sp, rscratch1);
    }
  }
  verify_cross_modify_fence_not_required();
}

void MacroAssembler::remove_frame(int framesize) {
  assert(framesize >= 2 * wordSize, "framesize must include space for FP/LR");
  assert(framesize % (2*wordSize) == 0, "must preserve 2*wordSize alignment");
  if (framesize < ((1 << 9) + 2 * wordSize)) {
    ldp(rfp, lr, Address(sp, framesize - 2 * wordSize));
    add(sp, sp, framesize);
  } else {
    if (framesize < ((1 << 12) + 2 * wordSize))
      add(sp, sp, framesize - 2 * wordSize);
    else {
      mov(rscratch1, framesize - 2 * wordSize);
      add(sp, sp, rscratch1);
    }
    ldp(rfp, lr, Address(post(sp, 2 * wordSize)));
  }
}


// This method checks if provided byte array contains byte with highest bit set.
address MacroAssembler::has_negatives(Register ary1, Register len, Register result) {
    // Simple and most common case of aligned small array which is not at the
    // end of memory page is placed here. All other cases are in stub.
    Label LOOP, END, STUB, STUB_LONG, SET_RESULT, DONE;
    const uint64_t UPPER_BIT_MASK=0x8080808080808080;
    assert_different_registers(ary1, len, result);

    cmpw(len, 0);
    br(LE, SET_RESULT);
    cmpw(len, 4 * wordSize);
    br(GE, STUB_LONG); // size > 32 then go to stub

    int shift = 64 - exact_log2(os::vm_page_size());
    lsl(rscratch1, ary1, shift);
    mov(rscratch2, (size_t)(4 * wordSize) << shift);
    adds(rscratch2, rscratch1, rscratch2);  // At end of page?
    br(CS, STUB); // at the end of page then go to stub
    subs(len, len, wordSize);
    br(LT, END);

  BIND(LOOP);
    ldr(rscratch1, Address(post(ary1, wordSize)));
    tst(rscratch1, UPPER_BIT_MASK);
    br(NE, SET_RESULT);
    subs(len, len, wordSize);
    br(GE, LOOP);
    cmpw(len, -wordSize);
    br(EQ, SET_RESULT);

  BIND(END);
    ldr(result, Address(ary1));
    sub(len, zr, len, LSL, 3); // LSL 3 is to get bits from bytes
    lslv(result, result, len);
    tst(result, UPPER_BIT_MASK);
    b(SET_RESULT);

  BIND(STUB);
    RuntimeAddress has_neg = RuntimeAddress(StubRoutines::aarch64::has_negatives());
    assert(has_neg.target() != NULL, "has_negatives stub has not been generated");
    address tpc1 = trampoline_call(has_neg);
    if (tpc1 == NULL) {
      DEBUG_ONLY(reset_labels(STUB_LONG, SET_RESULT, DONE));
      postcond(pc() == badAddress);
      return NULL;
    }
    b(DONE);

  BIND(STUB_LONG);
    RuntimeAddress has_neg_long = RuntimeAddress(StubRoutines::aarch64::has_negatives_long());
    assert(has_neg_long.target() != NULL, "has_negatives stub has not been generated");
    address tpc2 = trampoline_call(has_neg_long);
    if (tpc2 == NULL) {
      DEBUG_ONLY(reset_labels(SET_RESULT, DONE));
      postcond(pc() == badAddress);
      return NULL;
    }
    b(DONE);

  BIND(SET_RESULT);
    cset(result, NE); // set true or false

  BIND(DONE);
  postcond(pc() != badAddress);
  return pc();
}

address MacroAssembler::arrays_equals(Register a1, Register a2, Register tmp3,
                                      Register tmp4, Register tmp5, Register result,
                                      Register cnt1, int elem_size) {
  Label DONE, SAME;
  Register tmp1 = rscratch1;
  Register tmp2 = rscratch2;
  Register cnt2 = tmp2;  // cnt2 only used in array length compare
  int elem_per_word = wordSize/elem_size;
  int log_elem_size = exact_log2(elem_size);
  int length_offset = arrayOopDesc::length_offset_in_bytes();
  int base_offset
    = arrayOopDesc::base_offset_in_bytes(elem_size == 2 ? T_CHAR : T_BYTE);
  int stubBytesThreshold = 3 * 64 + (UseSIMDForArrayEquals ? 0 : 16);

  assert(elem_size == 1 || elem_size == 2, "must be char or byte");
  assert_different_registers(a1, a2, result, cnt1, rscratch1, rscratch2);

#ifndef PRODUCT
  {
    const char kind = (elem_size == 2) ? 'U' : 'L';
    char comment[64];
    snprintf(comment, sizeof comment, "array_equals%c{", kind);
    BLOCK_COMMENT(comment);
  }
#endif

  // if (a1 == a2)
  //     return true;
  cmpoop(a1, a2); // May have read barriers for a1 and a2.
  br(EQ, SAME);

  if (UseSimpleArrayEquals) {
    Label NEXT_WORD, SHORT, TAIL03, TAIL01, A_MIGHT_BE_NULL, A_IS_NOT_NULL;
    // if (a1 == null || a2 == null)
    //     return false;
    // a1 & a2 == 0 means (some-pointer is null) or
    // (very-rare-or-even-probably-impossible-pointer-values)
    // so, we can save one branch in most cases
    tst(a1, a2);
    mov(result, false);
    br(EQ, A_MIGHT_BE_NULL);
    // if (a1.length != a2.length)
    //      return false;
    bind(A_IS_NOT_NULL);
    ldrw(cnt1, Address(a1, length_offset));
    ldrw(cnt2, Address(a2, length_offset));
    eorw(tmp5, cnt1, cnt2);
    cbnzw(tmp5, DONE);
    lea(a1, Address(a1, base_offset));
    lea(a2, Address(a2, base_offset));
    // Check for short strings, i.e. smaller than wordSize.
    subs(cnt1, cnt1, elem_per_word);
    br(Assembler::LT, SHORT);
    // Main 8 byte comparison loop.
    bind(NEXT_WORD); {
      ldr(tmp1, Address(post(a1, wordSize)));
      ldr(tmp2, Address(post(a2, wordSize)));
      subs(cnt1, cnt1, elem_per_word);
      eor(tmp5, tmp1, tmp2);
      cbnz(tmp5, DONE);
    } br(GT, NEXT_WORD);
    // Last longword.  In the case where length == 4 we compare the
    // same longword twice, but that's still faster than another
    // conditional branch.
    // cnt1 could be 0, -1, -2, -3, -4 for chars; -4 only happens when
    // length == 4.
    if (log_elem_size > 0)
      lsl(cnt1, cnt1, log_elem_size);
    ldr(tmp3, Address(a1, cnt1));
    ldr(tmp4, Address(a2, cnt1));
    eor(tmp5, tmp3, tmp4);
    cbnz(tmp5, DONE);
    b(SAME);
    bind(A_MIGHT_BE_NULL);
    // in case both a1 and a2 are not-null, proceed with loads
    cbz(a1, DONE);
    cbz(a2, DONE);
    b(A_IS_NOT_NULL);
    bind(SHORT);

    tbz(cnt1, 2 - log_elem_size, TAIL03); // 0-7 bytes left.
    {
      ldrw(tmp1, Address(post(a1, 4)));
      ldrw(tmp2, Address(post(a2, 4)));
      eorw(tmp5, tmp1, tmp2);
      cbnzw(tmp5, DONE);
    }
    bind(TAIL03);
    tbz(cnt1, 1 - log_elem_size, TAIL01); // 0-3 bytes left.
    {
      ldrh(tmp3, Address(post(a1, 2)));
      ldrh(tmp4, Address(post(a2, 2)));
      eorw(tmp5, tmp3, tmp4);
      cbnzw(tmp5, DONE);
    }
    bind(TAIL01);
    if (elem_size == 1) { // Only needed when comparing byte arrays.
      tbz(cnt1, 0, SAME); // 0-1 bytes left.
      {
        ldrb(tmp1, a1);
        ldrb(tmp2, a2);
        eorw(tmp5, tmp1, tmp2);
        cbnzw(tmp5, DONE);
      }
    }
  } else {
    Label NEXT_DWORD, SHORT, TAIL, TAIL2, STUB,
        CSET_EQ, LAST_CHECK;
    mov(result, false);
    cbz(a1, DONE);
    ldrw(cnt1, Address(a1, length_offset));
    cbz(a2, DONE);
    ldrw(cnt2, Address(a2, length_offset));
    // on most CPUs a2 is still "locked"(surprisingly) in ldrw and it's
    // faster to perform another branch before comparing a1 and a2
    cmp(cnt1, (u1)elem_per_word);
    br(LE, SHORT); // short or same
    ldr(tmp3, Address(pre(a1, base_offset)));
    subs(zr, cnt1, stubBytesThreshold);
    br(GE, STUB);
    ldr(tmp4, Address(pre(a2, base_offset)));
    sub(tmp5, zr, cnt1, LSL, 3 + log_elem_size);
    cmp(cnt2, cnt1);
    br(NE, DONE);

    // Main 16 byte comparison loop with 2 exits
    bind(NEXT_DWORD); {
      ldr(tmp1, Address(pre(a1, wordSize)));
      ldr(tmp2, Address(pre(a2, wordSize)));
      subs(cnt1, cnt1, 2 * elem_per_word);
      br(LE, TAIL);
      eor(tmp4, tmp3, tmp4);
      cbnz(tmp4, DONE);
      ldr(tmp3, Address(pre(a1, wordSize)));
      ldr(tmp4, Address(pre(a2, wordSize)));
      cmp(cnt1, (u1)elem_per_word);
      br(LE, TAIL2);
      cmp(tmp1, tmp2);
    } br(EQ, NEXT_DWORD);
    b(DONE);

    bind(TAIL);
    eor(tmp4, tmp3, tmp4);
    eor(tmp2, tmp1, tmp2);
    lslv(tmp2, tmp2, tmp5);
    orr(tmp5, tmp4, tmp2);
    cmp(tmp5, zr);
    b(CSET_EQ);

    bind(TAIL2);
    eor(tmp2, tmp1, tmp2);
    cbnz(tmp2, DONE);
    b(LAST_CHECK);

    bind(STUB);
    ldr(tmp4, Address(pre(a2, base_offset)));
    cmp(cnt2, cnt1);
    br(NE, DONE);
    if (elem_size == 2) { // convert to byte counter
      lsl(cnt1, cnt1, 1);
    }
    eor(tmp5, tmp3, tmp4);
    cbnz(tmp5, DONE);
    RuntimeAddress stub = RuntimeAddress(StubRoutines::aarch64::large_array_equals());
    assert(stub.target() != NULL, "array_equals_long stub has not been generated");
    address tpc = trampoline_call(stub);
    if (tpc == NULL) {
      DEBUG_ONLY(reset_labels(SHORT, LAST_CHECK, CSET_EQ, SAME, DONE));
      postcond(pc() == badAddress);
      return NULL;
    }
    b(DONE);

    // (a1 != null && a2 == null) || (a1 != null && a2 != null && a1 == a2)
    // so, if a2 == null => return false(0), else return true, so we can return a2
    mov(result, a2);
    b(DONE);
    bind(SHORT);
    cmp(cnt2, cnt1);
    br(NE, DONE);
    cbz(cnt1, SAME);
    sub(tmp5, zr, cnt1, LSL, 3 + log_elem_size);
    ldr(tmp3, Address(a1, base_offset));
    ldr(tmp4, Address(a2, base_offset));
    bind(LAST_CHECK);
    eor(tmp4, tmp3, tmp4);
    lslv(tmp5, tmp4, tmp5);
    cmp(tmp5, zr);
    bind(CSET_EQ);
    cset(result, EQ);
    b(DONE);
  }

  bind(SAME);
  mov(result, true);
  // That's it.
  bind(DONE);

  BLOCK_COMMENT("} array_equals");
  postcond(pc() != badAddress);
  return pc();
}

// Compare Strings

// For Strings we're passed the address of the first characters in a1
// and a2 and the length in cnt1.
// elem_size is the element size in bytes: either 1 or 2.
// There are two implementations.  For arrays >= 8 bytes, all
// comparisons (including the final one, which may overlap) are
// performed 8 bytes at a time.  For strings < 8 bytes, we compare a
// halfword, then a short, and then a byte.

void MacroAssembler::string_equals(Register a1, Register a2,
                                   Register result, Register cnt1, int elem_size)
{
  Label SAME, DONE, SHORT, NEXT_WORD;
  Register tmp1 = rscratch1;
  Register tmp2 = rscratch2;
  Register cnt2 = tmp2;  // cnt2 only used in array length compare

  assert(elem_size == 1 || elem_size == 2, "must be 2 or 1 byte");
  assert_different_registers(a1, a2, result, cnt1, rscratch1, rscratch2);

#ifndef PRODUCT
  {
    const char kind = (elem_size == 2) ? 'U' : 'L';
    char comment[64];
    snprintf(comment, sizeof comment, "{string_equals%c", kind);
    BLOCK_COMMENT(comment);
  }
#endif

  mov(result, false);

  // Check for short strings, i.e. smaller than wordSize.
  subs(cnt1, cnt1, wordSize);
  br(Assembler::LT, SHORT);
  // Main 8 byte comparison loop.
  bind(NEXT_WORD); {
    ldr(tmp1, Address(post(a1, wordSize)));
    ldr(tmp2, Address(post(a2, wordSize)));
    subs(cnt1, cnt1, wordSize);
    eor(tmp1, tmp1, tmp2);
    cbnz(tmp1, DONE);
  } br(GT, NEXT_WORD);
  // Last longword.  In the case where length == 4 we compare the
  // same longword twice, but that's still faster than another
  // conditional branch.
  // cnt1 could be 0, -1, -2, -3, -4 for chars; -4 only happens when
  // length == 4.
  ldr(tmp1, Address(a1, cnt1));
  ldr(tmp2, Address(a2, cnt1));
  eor(tmp2, tmp1, tmp2);
  cbnz(tmp2, DONE);
  b(SAME);

  bind(SHORT);
  Label TAIL03, TAIL01;

  tbz(cnt1, 2, TAIL03); // 0-7 bytes left.
  {
    ldrw(tmp1, Address(post(a1, 4)));
    ldrw(tmp2, Address(post(a2, 4)));
    eorw(tmp1, tmp1, tmp2);
    cbnzw(tmp1, DONE);
  }
  bind(TAIL03);
  tbz(cnt1, 1, TAIL01); // 0-3 bytes left.
  {
    ldrh(tmp1, Address(post(a1, 2)));
    ldrh(tmp2, Address(post(a2, 2)));
    eorw(tmp1, tmp1, tmp2);
    cbnzw(tmp1, DONE);
  }
  bind(TAIL01);
  if (elem_size == 1) { // Only needed when comparing 1-byte elements
    tbz(cnt1, 0, SAME); // 0-1 bytes left.
    {
      ldrb(tmp1, a1);
      ldrb(tmp2, a2);
      eorw(tmp1, tmp1, tmp2);
      cbnzw(tmp1, DONE);
    }
  }
  // Arrays are equal.
  bind(SAME);
  mov(result, true);

  // That's it.
  bind(DONE);
  BLOCK_COMMENT("} string_equals");
}


// The size of the blocks erased by the zero_blocks stub.  We must
// handle anything smaller than this ourselves in zero_words().
const int MacroAssembler::zero_words_block_size = 8;

// zero_words() is used by C2 ClearArray patterns and by
// C1_MacroAssembler.  It is as small as possible, handling small word
// counts locally and delegating anything larger to the zero_blocks
// stub.  It is expanded many times in compiled code, so it is
// important to keep it short.

// ptr:   Address of a buffer to be zeroed.
// cnt:   Count in HeapWords.
//
// ptr, cnt, rscratch1, and rscratch2 are clobbered.
address MacroAssembler::zero_words(Register ptr, Register cnt)
{
  assert(is_power_of_2(zero_words_block_size), "adjust this");

  BLOCK_COMMENT("zero_words {");
  assert(ptr == r10 && cnt == r11, "mismatch in register usage");
  RuntimeAddress zero_blocks = RuntimeAddress(StubRoutines::aarch64::zero_blocks());
  assert(zero_blocks.target() != NULL, "zero_blocks stub has not been generated");

  subs(rscratch1, cnt, zero_words_block_size);
  Label around;
  br(LO, around);
  {
    RuntimeAddress zero_blocks = RuntimeAddress(StubRoutines::aarch64::zero_blocks());
    assert(zero_blocks.target() != NULL, "zero_blocks stub has not been generated");
    // Make sure this is a C2 compilation. C1 allocates space only for
    // trampoline stubs generated by Call LIR ops, and in any case it
    // makes sense for a C1 compilation task to proceed as quickly as
    // possible.
    CompileTask* task;
    if (StubRoutines::aarch64::complete()
        && Thread::current()->is_Compiler_thread()
        && (task = ciEnv::current()->task())
        && is_c2_compile(task->comp_level())) {
      address tpc = trampoline_call(zero_blocks);
      if (tpc == NULL) {
        DEBUG_ONLY(reset_labels(around));
        assert(false, "failed to allocate space for trampoline");
        return NULL;
      }
    } else {
      far_call(zero_blocks);
    }
  }
  bind(around);

  // We have a few words left to do. zero_blocks has adjusted r10 and r11
  // for us.
  for (int i = zero_words_block_size >> 1; i > 1; i >>= 1) {
    Label l;
    tbz(cnt, exact_log2(i), l);
    for (int j = 0; j < i; j += 2) {
      stp(zr, zr, post(ptr, 2 * BytesPerWord));
    }
    bind(l);
  }
  {
    Label l;
    tbz(cnt, 0, l);
    str(zr, Address(ptr));
    bind(l);
  }

  BLOCK_COMMENT("} zero_words");
  return pc();
}

// base:         Address of a buffer to be zeroed, 8 bytes aligned.
// cnt:          Immediate count in HeapWords.
//
// r10, r11, rscratch1, and rscratch2 are clobbered.
void MacroAssembler::zero_words(Register base, uint64_t cnt)
{
  guarantee(zero_words_block_size < BlockZeroingLowLimit,
            "increase BlockZeroingLowLimit");
  if (cnt <= (uint64_t)BlockZeroingLowLimit / BytesPerWord) {
#ifndef PRODUCT
    {
      char buf[64];
      snprintf(buf, sizeof buf, "zero_words (count = %" PRIu64 ") {", cnt);
      BLOCK_COMMENT(buf);
    }
#endif
    if (cnt >= 16) {
      uint64_t loops = cnt/16;
      if (loops > 1) {
        mov(rscratch2, loops - 1);
      }
      {
        Label loop;
        bind(loop);
        for (int i = 0; i < 16; i += 2) {
          stp(zr, zr, Address(base, i * BytesPerWord));
        }
        add(base, base, 16 * BytesPerWord);
        if (loops > 1) {
          subs(rscratch2, rscratch2, 1);
          br(GE, loop);
        }
      }
    }
    cnt %= 16;
    int i = cnt & 1;  // store any odd word to start
    if (i) str(zr, Address(base));
    for (; i < (int)cnt; i += 2) {
      stp(zr, zr, Address(base, i * wordSize));
    }
    BLOCK_COMMENT("} zero_words");
  } else {
    mov(r10, base); mov(r11, cnt);
    zero_words(r10, r11);
  }
}

// Zero blocks of memory by using DC ZVA.
//
// Aligns the base address first sufficently for DC ZVA, then uses
// DC ZVA repeatedly for every full block.  cnt is the size to be
// zeroed in HeapWords.  Returns the count of words left to be zeroed
// in cnt.
//
// NOTE: This is intended to be used in the zero_blocks() stub.  If
// you want to use it elsewhere, note that cnt must be >= 2*zva_length.
void MacroAssembler::zero_dcache_blocks(Register base, Register cnt) {
  Register tmp = rscratch1;
  Register tmp2 = rscratch2;
  int zva_length = VM_Version::zva_length();
  Label initial_table_end, loop_zva;
  Label fini;

  // Base must be 16 byte aligned. If not just return and let caller handle it
  tst(base, 0x0f);
  br(Assembler::NE, fini);
  // Align base with ZVA length.
  neg(tmp, base);
  andr(tmp, tmp, zva_length - 1);

  // tmp: the number of bytes to be filled to align the base with ZVA length.
  add(base, base, tmp);
  sub(cnt, cnt, tmp, Assembler::ASR, 3);
  adr(tmp2, initial_table_end);
  sub(tmp2, tmp2, tmp, Assembler::LSR, 2);
  br(tmp2);

  for (int i = -zva_length + 16; i < 0; i += 16)
    stp(zr, zr, Address(base, i));
  bind(initial_table_end);

  sub(cnt, cnt, zva_length >> 3);
  bind(loop_zva);
  dc(Assembler::ZVA, base);
  subs(cnt, cnt, zva_length >> 3);
  add(base, base, zva_length);
  br(Assembler::GE, loop_zva);
  add(cnt, cnt, zva_length >> 3); // count not zeroed by DC ZVA
  bind(fini);
}

// base:   Address of a buffer to be filled, 8 bytes aligned.
// cnt:    Count in 8-byte unit.
// value:  Value to be filled with.
// base will point to the end of the buffer after filling.
void MacroAssembler::fill_words(Register base, Register cnt, Register value)
{
//  Algorithm:
//
//    scratch1 = cnt & 7;
//    cnt -= scratch1;
//    p += scratch1;
//    switch (scratch1) {
//      do {
//        cnt -= 8;
//          p[-8] = v;
//        case 7:
//          p[-7] = v;
//        case 6:
//          p[-6] = v;
//          // ...
//        case 1:
//          p[-1] = v;
//        case 0:
//          p += 8;
//      } while (cnt);
//    }

  assert_different_registers(base, cnt, value, rscratch1, rscratch2);

  Label fini, skip, entry, loop;
  const int unroll = 8; // Number of stp instructions we'll unroll

  cbz(cnt, fini);
  tbz(base, 3, skip);
  str(value, Address(post(base, 8)));
  sub(cnt, cnt, 1);
  bind(skip);

  andr(rscratch1, cnt, (unroll-1) * 2);
  sub(cnt, cnt, rscratch1);
  add(base, base, rscratch1, Assembler::LSL, 3);
  adr(rscratch2, entry);
  sub(rscratch2, rscratch2, rscratch1, Assembler::LSL, 1);
  br(rscratch2);

  bind(loop);
  add(base, base, unroll * 16);
  for (int i = -unroll; i < 0; i++)
    stp(value, value, Address(base, i * 16));
  bind(entry);
  subs(cnt, cnt, unroll * 2);
  br(Assembler::GE, loop);

  tbz(cnt, 0, fini);
  str(value, Address(post(base, 8)));
  bind(fini);
}

// Intrinsic for sun/nio/cs/ISO_8859_1$Encoder.implEncodeISOArray and
// java/lang/StringUTF16.compress.
void MacroAssembler::encode_iso_array(Register src, Register dst,
                      Register len, Register result,
                      FloatRegister Vtmp1, FloatRegister Vtmp2,
                      FloatRegister Vtmp3, FloatRegister Vtmp4)
{
    Label DONE, SET_RESULT, NEXT_32, NEXT_32_PRFM, LOOP_8, NEXT_8, LOOP_1, NEXT_1,
        NEXT_32_START, NEXT_32_PRFM_START;
    Register tmp1 = rscratch1, tmp2 = rscratch2;

      mov(result, len); // Save initial len

      cmp(len, (u1)8); // handle shortest strings first
      br(LT, LOOP_1);
      cmp(len, (u1)32);
      br(LT, NEXT_8);
      // The following code uses the SIMD 'uzp1' and 'uzp2' instructions
      // to convert chars to bytes
      if (SoftwarePrefetchHintDistance >= 0) {
        ld1(Vtmp1, Vtmp2, Vtmp3, Vtmp4, T8H, src);
        subs(tmp2, len, SoftwarePrefetchHintDistance/2 + 16);
        br(LE, NEXT_32_START);
        b(NEXT_32_PRFM_START);
        BIND(NEXT_32_PRFM);
          ld1(Vtmp1, Vtmp2, Vtmp3, Vtmp4, T8H, src);
        BIND(NEXT_32_PRFM_START);
          prfm(Address(src, SoftwarePrefetchHintDistance));
          orr(v4, T16B, Vtmp1, Vtmp2);
          orr(v5, T16B, Vtmp3, Vtmp4);
          uzp1(Vtmp1, T16B, Vtmp1, Vtmp2);
          uzp1(Vtmp3, T16B, Vtmp3, Vtmp4);
          uzp2(v5, T16B, v4, v5); // high bytes
          umov(tmp2, v5, D, 1);
          fmovd(tmp1, v5);
          orr(tmp1, tmp1, tmp2);
          cbnz(tmp1, LOOP_8);
          stpq(Vtmp1, Vtmp3, dst);
          sub(len, len, 32);
          add(dst, dst, 32);
          add(src, src, 64);
          subs(tmp2, len, SoftwarePrefetchHintDistance/2 + 16);
          br(GE, NEXT_32_PRFM);
          cmp(len, (u1)32);
          br(LT, LOOP_8);
        BIND(NEXT_32);
          ld1(Vtmp1, Vtmp2, Vtmp3, Vtmp4, T8H, src);
        BIND(NEXT_32_START);
      } else {
        BIND(NEXT_32);
          ld1(Vtmp1, Vtmp2, Vtmp3, Vtmp4, T8H, src);
      }
      prfm(Address(src, SoftwarePrefetchHintDistance));
      uzp1(v4, T16B, Vtmp1, Vtmp2);
      uzp1(v5, T16B, Vtmp3, Vtmp4);
      orr(Vtmp1, T16B, Vtmp1, Vtmp2);
      orr(Vtmp3, T16B, Vtmp3, Vtmp4);
      uzp2(Vtmp1, T16B, Vtmp1, Vtmp3); // high bytes
      umov(tmp2, Vtmp1, D, 1);
      fmovd(tmp1, Vtmp1);
      orr(tmp1, tmp1, tmp2);
      cbnz(tmp1, LOOP_8);
      stpq(v4, v5, dst);
      sub(len, len, 32);
      add(dst, dst, 32);
      add(src, src, 64);
      cmp(len, (u1)32);
      br(GE, NEXT_32);
      cbz(len, DONE);

    BIND(LOOP_8);
      cmp(len, (u1)8);
      br(LT, LOOP_1);
    BIND(NEXT_8);
      ld1(Vtmp1, T8H, src);
      uzp1(Vtmp2, T16B, Vtmp1, Vtmp1); // low bytes
      uzp2(Vtmp3, T16B, Vtmp1, Vtmp1); // high bytes
      fmovd(tmp1, Vtmp3);
      cbnz(tmp1, NEXT_1);
      strd(Vtmp2, dst);

      sub(len, len, 8);
      add(dst, dst, 8);
      add(src, src, 16);
      cmp(len, (u1)8);
      br(GE, NEXT_8);

    BIND(LOOP_1);

    cbz(len, DONE);
    BIND(NEXT_1);
      ldrh(tmp1, Address(post(src, 2)));
      tst(tmp1, 0xff00);
      br(NE, SET_RESULT);
      strb(tmp1, Address(post(dst, 1)));
      subs(len, len, 1);
      br(GT, NEXT_1);

    BIND(SET_RESULT);
      sub(result, result, len); // Return index where we stopped
                                // Return len == 0 if we processed all
                                // characters
    BIND(DONE);
}


// Inflate byte[] array to char[].
address MacroAssembler::byte_array_inflate(Register src, Register dst, Register len,
                                           FloatRegister vtmp1, FloatRegister vtmp2,
                                           FloatRegister vtmp3, Register tmp4) {
  Label big, done, after_init, to_stub;

  assert_different_registers(src, dst, len, tmp4, rscratch1);

  fmovd(vtmp1, 0.0);
  lsrw(tmp4, len, 3);
  bind(after_init);
  cbnzw(tmp4, big);
  // Short string: less than 8 bytes.
  {
    Label loop, tiny;

    cmpw(len, 4);
    br(LT, tiny);
    // Use SIMD to do 4 bytes.
    ldrs(vtmp2, post(src, 4));
    zip1(vtmp3, T8B, vtmp2, vtmp1);
    subw(len, len, 4);
    strd(vtmp3, post(dst, 8));

    cbzw(len, done);

    // Do the remaining bytes by steam.
    bind(loop);
    ldrb(tmp4, post(src, 1));
    strh(tmp4, post(dst, 2));
    subw(len, len, 1);

    bind(tiny);
    cbnz(len, loop);

    b(done);
  }

  if (SoftwarePrefetchHintDistance >= 0) {
    bind(to_stub);
      RuntimeAddress stub = RuntimeAddress(StubRoutines::aarch64::large_byte_array_inflate());
      assert(stub.target() != NULL, "large_byte_array_inflate stub has not been generated");
      address tpc = trampoline_call(stub);
      if (tpc == NULL) {
        DEBUG_ONLY(reset_labels(big, done));
        postcond(pc() == badAddress);
        return NULL;
      }
      b(after_init);
  }

  // Unpack the bytes 8 at a time.
  bind(big);
  {
    Label loop, around, loop_last, loop_start;

    if (SoftwarePrefetchHintDistance >= 0) {
      const int large_loop_threshold = (64 + 16)/8;
      ldrd(vtmp2, post(src, 8));
      andw(len, len, 7);
      cmp(tmp4, (u1)large_loop_threshold);
      br(GE, to_stub);
      b(loop_start);

      bind(loop);
      ldrd(vtmp2, post(src, 8));
      bind(loop_start);
      subs(tmp4, tmp4, 1);
      br(EQ, loop_last);
      zip1(vtmp2, T16B, vtmp2, vtmp1);
      ldrd(vtmp3, post(src, 8));
      st1(vtmp2, T8H, post(dst, 16));
      subs(tmp4, tmp4, 1);
      zip1(vtmp3, T16B, vtmp3, vtmp1);
      st1(vtmp3, T8H, post(dst, 16));
      br(NE, loop);
      b(around);
      bind(loop_last);
      zip1(vtmp2, T16B, vtmp2, vtmp1);
      st1(vtmp2, T8H, post(dst, 16));
      bind(around);
      cbz(len, done);
    } else {
      andw(len, len, 7);
      bind(loop);
      ldrd(vtmp2, post(src, 8));
      sub(tmp4, tmp4, 1);
      zip1(vtmp3, T16B, vtmp2, vtmp1);
      st1(vtmp3, T8H, post(dst, 16));
      cbnz(tmp4, loop);
    }
  }

  // Do the tail of up to 8 bytes.
  add(src, src, len);
  ldrd(vtmp3, Address(src, -8));
  add(dst, dst, len, ext::uxtw, 1);
  zip1(vtmp3, T16B, vtmp3, vtmp1);
  strq(vtmp3, Address(dst, -16));

  bind(done);
  postcond(pc() != badAddress);
  return pc();
}

// Compress char[] array to byte[].
void MacroAssembler::char_array_compress(Register src, Register dst, Register len,
                                         FloatRegister tmp1Reg, FloatRegister tmp2Reg,
                                         FloatRegister tmp3Reg, FloatRegister tmp4Reg,
                                         Register result) {
  encode_iso_array(src, dst, len, result,
                   tmp1Reg, tmp2Reg, tmp3Reg, tmp4Reg);
  cmp(len, zr);
  csel(result, result, zr, EQ);
}

// get_thread() can be called anywhere inside generated code so we
// need to save whatever non-callee save context might get clobbered
// by the call to JavaThread::aarch64_get_thread_helper() or, indeed,
// the call setup code.
//
// On Linux, aarch64_get_thread_helper() clobbers only r0, r1, and flags.
// On other systems, the helper is a usual C function.
//
void MacroAssembler::get_thread(Register dst) {
  RegSet saved_regs =
    LINUX_ONLY(RegSet::range(r0, r1)  + lr - dst)
    NOT_LINUX (RegSet::range(r0, r17) + lr - dst);

  push(saved_regs, sp);

  mov(lr, CAST_FROM_FN_PTR(address, JavaThread::aarch64_get_thread_helper));
  blr(lr);
  if (dst != c_rarg0) {
    mov(dst, c_rarg0);
  }

  pop(saved_regs, sp);
}

void MacroAssembler::cache_wb(Address line) {
  assert(line.getMode() == Address::base_plus_offset, "mode should be base_plus_offset");
  assert(line.index() == noreg, "index should be noreg");
  assert(line.offset() == 0, "offset should be 0");
  // would like to assert this
  // assert(line._ext.shift == 0, "shift should be zero");
  if (VM_Version::features() & VM_Version::CPU_DCPOP) {
    // writeback using clear virtual address to point of persistence
    dc(Assembler::CVAP, line.base());
  } else {
    // no need to generate anything as Unsafe.writebackMemory should
    // never invoke this stub
  }
}

void MacroAssembler::cache_wbsync(bool is_pre) {
  // we only need a barrier post sync
  if (!is_pre) {
    membar(Assembler::AnyAny);
  }
}

void MacroAssembler::verify_sve_vector_length() {
  // Make sure that native code does not change SVE vector length.
  if (!UseSVE) return;
  Label verify_ok;
  movw(rscratch1, zr);
  sve_inc(rscratch1, B);
  subsw(zr, rscratch1, VM_Version::get_initial_sve_vector_length());
  br(EQ, verify_ok);
  stop("Error: SVE vector length has changed since jvm startup");
  bind(verify_ok);
}

void MacroAssembler::verify_ptrue() {
  Label verify_ok;
  if (!UseSVE) {
    return;
  }
  sve_cntp(rscratch1, B, ptrue, ptrue); // get true elements count.
  sve_dec(rscratch1, B);
  cbz(rscratch1, verify_ok);
  stop("Error: the preserved predicate register (p7) elements are not all true");
  bind(verify_ok);
}

void MacroAssembler::safepoint_isb() {
  isb();
#ifndef PRODUCT
  if (VerifyCrossModifyFence) {
    // Clear the thread state.
    strb(zr, Address(rthread, in_bytes(JavaThread::requires_cross_modify_fence_offset())));
  }
#endif
}

#ifndef PRODUCT
void MacroAssembler::verify_cross_modify_fence_not_required() {
  if (VerifyCrossModifyFence) {
    // Check if thread needs a cross modify fence.
    ldrb(rscratch1, Address(rthread, in_bytes(JavaThread::requires_cross_modify_fence_offset())));
    Label fence_not_required;
    cbz(rscratch1, fence_not_required);
    // If it does then fail.
    lea(rscratch1, CAST_FROM_FN_PTR(address, JavaThread::verify_cross_modify_fence_failure));
    mov(c_rarg0, rthread);
    blr(rscratch1);
    bind(fence_not_required);
  }
}
#endif
