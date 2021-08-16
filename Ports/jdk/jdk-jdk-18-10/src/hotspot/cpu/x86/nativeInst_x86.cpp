/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "code/compiledIC.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_x86.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/ostream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

void NativeInstruction::wrote(int offset) {
  ICache::invalidate_word(addr_at(offset));
}

#ifdef ASSERT
void NativeLoadGot::report_and_fail() const {
  tty->print_cr("Addr: " INTPTR_FORMAT " Code: %x %x %x", p2i(instruction_address()),
                  (has_rex ? ubyte_at(0) : 0), ubyte_at(rex_size), ubyte_at(rex_size + 1));
  fatal("not a indirect rip mov to rbx");
}

void NativeLoadGot::verify() const {
  if (has_rex) {
    int rex = ubyte_at(0);
    if (rex != rex_prefix && rex != rex_b_prefix) {
      report_and_fail();
    }
  }

  int inst = ubyte_at(rex_size);
  if (inst != instruction_code) {
    report_and_fail();
  }
  int modrm = ubyte_at(rex_size + 1);
  if (modrm != modrm_rbx_code && modrm != modrm_rax_code) {
    report_and_fail();
  }
}
#endif

intptr_t NativeLoadGot::data() const {
  return *(intptr_t *) got_address();
}

address NativePltCall::destination() const {
  NativeGotJump* jump = nativeGotJump_at(plt_jump());
  return jump->destination();
}

address NativePltCall::plt_entry() const {
  return return_address() + displacement();
}

address NativePltCall::plt_jump() const {
  address entry = plt_entry();
  // Virtual PLT code has move instruction first
  if (((NativeGotJump*)entry)->is_GotJump()) {
    return entry;
  } else {
    return nativeLoadGot_at(entry)->next_instruction_address();
  }
}

address NativePltCall::plt_load_got() const {
  address entry = plt_entry();
  if (!((NativeGotJump*)entry)->is_GotJump()) {
    // Virtual PLT code has move instruction first
    return entry;
  } else {
    // Static PLT code has move instruction second (from c2i stub)
    return nativeGotJump_at(entry)->next_instruction_address();
  }
}

address NativePltCall::plt_c2i_stub() const {
  address entry = plt_load_got();
  // This method should be called only for static calls which has C2I stub.
  NativeLoadGot* load = nativeLoadGot_at(entry);
  return entry;
}

address NativePltCall::plt_resolve_call() const {
  NativeGotJump* jump = nativeGotJump_at(plt_jump());
  address entry = jump->next_instruction_address();
  if (((NativeGotJump*)entry)->is_GotJump()) {
    return entry;
  } else {
    // c2i stub 2 instructions
    entry = nativeLoadGot_at(entry)->next_instruction_address();
    return nativeGotJump_at(entry)->next_instruction_address();
  }
}

void NativePltCall::reset_to_plt_resolve_call() {
  set_destination_mt_safe(plt_resolve_call());
}

void NativePltCall::set_destination_mt_safe(address dest) {
  // rewriting the value in the GOT, it should always be aligned
  NativeGotJump* jump = nativeGotJump_at(plt_jump());
  address* got = (address *) jump->got_address();
  *got = dest;
}

void NativePltCall::set_stub_to_clean() {
  NativeLoadGot* method_loader = nativeLoadGot_at(plt_c2i_stub());
  NativeGotJump* jump          = nativeGotJump_at(method_loader->next_instruction_address());
  method_loader->set_data(0);
  jump->set_jump_destination((address)-1);
}

void NativePltCall::verify() const {
  // Make sure code pattern is actually a call rip+off32 instruction.
  int inst = ubyte_at(0);
  if (inst != instruction_code) {
    tty->print_cr("Addr: " INTPTR_FORMAT " Code: 0x%x", p2i(instruction_address()),
                                                        inst);
    fatal("not a call rip+off32");
  }
}

address NativeGotJump::destination() const {
  address *got_entry = (address *) got_address();
  return *got_entry;
}

#ifdef ASSERT
void NativeGotJump::report_and_fail() const {
  tty->print_cr("Addr: " INTPTR_FORMAT " Code: %x %x %x", p2i(instruction_address()),
                 (has_rex() ? ubyte_at(0) : 0), ubyte_at(rex_size()), ubyte_at(rex_size() + 1));
  fatal("not a indirect rip jump");
}

void NativeGotJump::verify() const {
  if (has_rex()) {
    int rex = ubyte_at(0);
    if (rex != rex_prefix) {
      report_and_fail();
    }
  }
  int inst = ubyte_at(rex_size());
  if (inst != instruction_code) {
    report_and_fail();
  }
  int modrm = ubyte_at(rex_size() + 1);
  if (modrm != modrm_code) {
    report_and_fail();
  }
}
#endif

void NativeCall::verify() {
  // Make sure code pattern is actually a call imm32 instruction.
  int inst = ubyte_at(0);
  if (inst != instruction_code) {
    tty->print_cr("Addr: " INTPTR_FORMAT " Code: 0x%x", p2i(instruction_address()),
                                                        inst);
    fatal("not a call disp32");
  }
}

address NativeCall::destination() const {
  // Getting the destination of a call isn't safe because that call can
  // be getting patched while you're calling this.  There's only special
  // places where this can be called but not automatically verifiable by
  // checking which locks are held.  The solution is true atomic patching
  // on x86, nyi.
  return return_address() + displacement();
}

void NativeCall::print() {
  tty->print_cr(PTR_FORMAT ": call " PTR_FORMAT,
                p2i(instruction_address()), p2i(destination()));
}

// Inserts a native call instruction at a given pc
void NativeCall::insert(address code_pos, address entry) {
  intptr_t disp = (intptr_t)entry - ((intptr_t)code_pos + 1 + 4);
#ifdef AMD64
  guarantee(disp == (intptr_t)(jint)disp, "must be 32-bit offset");
#endif // AMD64
  *code_pos = instruction_code;
  *((int32_t *)(code_pos+1)) = (int32_t) disp;
  ICache::invalidate_range(code_pos, instruction_size);
}

// MT-safe patching of a call instruction.
// First patches first word of instruction to two jmp's that jmps to them
// selfs (spinlock). Then patches the last byte, and then atomicly replaces
// the jmp's with the first 4 byte of the new instruction.
void NativeCall::replace_mt_safe(address instr_addr, address code_buffer) {
  assert(Patching_lock->is_locked() ||
         SafepointSynchronize::is_at_safepoint(), "concurrent code patching");
  assert (instr_addr != NULL, "illegal address for code patching");

  NativeCall* n_call =  nativeCall_at (instr_addr); // checking that it is a call
  guarantee((intptr_t)instr_addr % BytesPerWord == 0, "must be aligned");

  // First patch dummy jmp in place
  unsigned char patch[4];
  assert(sizeof(patch)==sizeof(jint), "sanity check");
  patch[0] = 0xEB;       // jmp rel8
  patch[1] = 0xFE;       // jmp to self
  patch[2] = 0xEB;
  patch[3] = 0xFE;

  // First patch dummy jmp in place
  *(jint*)instr_addr = *(jint *)patch;

  // Invalidate.  Opteron requires a flush after every write.
  n_call->wrote(0);

  // Patch 4th byte
  instr_addr[4] = code_buffer[4];

  n_call->wrote(4);

  // Patch bytes 0-3
  *(jint*)instr_addr = *(jint *)code_buffer;

  n_call->wrote(0);

#ifdef ASSERT
   // verify patching
   for ( int i = 0; i < instruction_size; i++) {
     address ptr = (address)((intptr_t)code_buffer + i);
     int a_byte = (*ptr) & 0xFF;
     assert(*((address)((intptr_t)instr_addr + i)) == a_byte, "mt safe patching failed");
   }
#endif

}


// Similar to replace_mt_safe, but just changes the destination.  The
// important thing is that free-running threads are able to execute this
// call instruction at all times.  If the displacement field is aligned
// we can simply rely on atomicity of 32-bit writes to make sure other threads
// will see no intermediate states.  Otherwise, the first two bytes of the
// call are guaranteed to be aligned, and can be atomically patched to a
// self-loop to guard the instruction while we change the other bytes.

// We cannot rely on locks here, since the free-running threads must run at
// full speed.
//
// Used in the runtime linkage of calls; see class CompiledIC.
// (Cf. 4506997 and 4479829, where threads witnessed garbage displacements.)
void NativeCall::set_destination_mt_safe(address dest) {
  debug_only(verify());
  // Make sure patching code is locked.  No two threads can patch at the same
  // time but one may be executing this code.
  assert(Patching_lock->is_locked() || SafepointSynchronize::is_at_safepoint() ||
         CompiledICLocker::is_safe(instruction_address()), "concurrent code patching");
  // Both C1 and C2 should now be generating code which aligns the patched address
  // to be within a single cache line.
  bool is_aligned = ((uintptr_t)displacement_address() + 0) / cache_line_size ==
                    ((uintptr_t)displacement_address() + 3) / cache_line_size;

  guarantee(is_aligned, "destination must be aligned");

  // The destination lies within a single cache line.
  set_destination(dest);
}


void NativeMovConstReg::verify() {
#ifdef AMD64
  // make sure code pattern is actually a mov reg64, imm64 instruction
  if ((ubyte_at(0) != Assembler::REX_W && ubyte_at(0) != Assembler::REX_WB) ||
      (ubyte_at(1) & (0xff ^ register_mask)) != 0xB8) {
    print();
    fatal("not a REX.W[B] mov reg64, imm64");
  }
#else
  // make sure code pattern is actually a mov reg, imm32 instruction
  u_char test_byte = *(u_char*)instruction_address();
  u_char test_byte_2 = test_byte & ( 0xff ^ register_mask);
  if (test_byte_2 != instruction_code) fatal("not a mov reg, imm32");
#endif // AMD64
}


void NativeMovConstReg::print() {
  tty->print_cr(PTR_FORMAT ": mov reg, " INTPTR_FORMAT,
                p2i(instruction_address()), data());
}

//-------------------------------------------------------------------

int NativeMovRegMem::instruction_start() const {
  int off = 0;
  u_char instr_0 = ubyte_at(off);

  // See comment in Assembler::locate_operand() about VEX prefixes.
  if (instr_0 == instruction_VEX_prefix_2bytes) {
    assert((UseAVX > 0), "shouldn't have VEX prefix");
    NOT_LP64(assert((0xC0 & ubyte_at(1)) == 0xC0, "shouldn't have LDS and LES instructions"));
    return 2;
  }
  if (instr_0 == instruction_VEX_prefix_3bytes) {
    assert((UseAVX > 0), "shouldn't have VEX prefix");
    NOT_LP64(assert((0xC0 & ubyte_at(1)) == 0xC0, "shouldn't have LDS and LES instructions"));
    return 3;
  }
  if (instr_0 == instruction_EVEX_prefix_4bytes) {
    assert(VM_Version::supports_evex(), "shouldn't have EVEX prefix");
    return 4;
  }

  // First check to see if we have a (prefixed or not) xor
  if (instr_0 >= instruction_prefix_wide_lo && // 0x40
      instr_0 <= instruction_prefix_wide_hi) { // 0x4f
    off++;
    instr_0 = ubyte_at(off);
  }

  if (instr_0 == instruction_code_xor) {
    off += 2;
    instr_0 = ubyte_at(off);
  }

  // Now look for the real instruction and the many prefix/size specifiers.

  if (instr_0 == instruction_operandsize_prefix ) {  // 0x66
    off++; // Not SSE instructions
    instr_0 = ubyte_at(off);
  }

  if ( instr_0 == instruction_code_xmm_ss_prefix || // 0xf3
       instr_0 == instruction_code_xmm_sd_prefix) { // 0xf2
    off++;
    instr_0 = ubyte_at(off);
  }

  if ( instr_0 >= instruction_prefix_wide_lo && // 0x40
       instr_0 <= instruction_prefix_wide_hi) { // 0x4f
    off++;
    instr_0 = ubyte_at(off);
  }


  if (instr_0 == instruction_extended_prefix ) {  // 0x0f
    off++;
  }

  return off;
}

int NativeMovRegMem::patch_offset() const {
  int off = data_offset + instruction_start();
  u_char mod_rm = *(u_char*)(instruction_address() + 1);
  // nnnn(r12|rsp) isn't coded as simple mod/rm since that is
  // the encoding to use an SIB byte. Which will have the nnnn
  // field off by one byte
  if ((mod_rm & 7) == 0x4) {
    off++;
  }
  return off;
}

void NativeMovRegMem::verify() {
  // make sure code pattern is actually a mov [reg+offset], reg instruction
  u_char test_byte = *(u_char*)instruction_address();
  switch (test_byte) {
    case instruction_code_reg2memb:  // 0x88 movb a, r
    case instruction_code_reg2mem:   // 0x89 movl a, r (can be movq in 64bit)
    case instruction_code_mem2regb:  // 0x8a movb r, a
    case instruction_code_mem2reg:   // 0x8b movl r, a (can be movq in 64bit)
      break;

    case instruction_code_mem2reg_movslq: // 0x63 movsql r, a
    case instruction_code_mem2reg_movzxb: // 0xb6 movzbl r, a (movzxb)
    case instruction_code_mem2reg_movzxw: // 0xb7 movzwl r, a (movzxw)
    case instruction_code_mem2reg_movsxb: // 0xbe movsbl r, a (movsxb)
    case instruction_code_mem2reg_movsxw: // 0xbf  movswl r, a (movsxw)
      break;

    case instruction_code_float_s:   // 0xd9 fld_s a
    case instruction_code_float_d:   // 0xdd fld_d a
    case instruction_code_xmm_load:  // 0x10 movsd xmm, a
    case instruction_code_xmm_store: // 0x11 movsd a, xmm
    case instruction_code_xmm_lpd:   // 0x12 movlpd xmm, a
      break;

    case instruction_code_lea:       // 0x8d lea r, a
      break;

    default:
          fatal ("not a mov [reg+offs], reg instruction");
  }
}


void NativeMovRegMem::print() {
  tty->print_cr(PTR_FORMAT ": mov reg, [reg + %x]", p2i(instruction_address()), offset());
}

//-------------------------------------------------------------------

void NativeLoadAddress::verify() {
  // make sure code pattern is actually a mov [reg+offset], reg instruction
  u_char test_byte = *(u_char*)instruction_address();
#ifdef _LP64
  if ( (test_byte == instruction_prefix_wide ||
        test_byte == instruction_prefix_wide_extended) ) {
    test_byte = *(u_char*)(instruction_address() + 1);
  }
#endif // _LP64
  if ( ! ((test_byte == lea_instruction_code)
          LP64_ONLY(|| (test_byte == mov64_instruction_code) ))) {
    fatal ("not a lea reg, [reg+offs] instruction");
  }
}


void NativeLoadAddress::print() {
  tty->print_cr(PTR_FORMAT ": lea [reg + %x], reg", p2i(instruction_address()), offset());
}

//--------------------------------------------------------------------------------

void NativeJump::verify() {
  if (*(u_char*)instruction_address() != instruction_code) {
    // far jump
    NativeMovConstReg* mov = nativeMovConstReg_at(instruction_address());
    NativeInstruction* jmp = nativeInstruction_at(mov->next_instruction_address());
    if (!jmp->is_jump_reg()) {
      fatal("not a jump instruction");
    }
  }
}


void NativeJump::insert(address code_pos, address entry) {
  intptr_t disp = (intptr_t)entry - ((intptr_t)code_pos + 1 + 4);
#ifdef AMD64
  guarantee(disp == (intptr_t)(int32_t)disp, "must be 32-bit offset");
#endif // AMD64

  *code_pos = instruction_code;
  *((int32_t*)(code_pos + 1)) = (int32_t)disp;

  ICache::invalidate_range(code_pos, instruction_size);
}

void NativeJump::check_verified_entry_alignment(address entry, address verified_entry) {
  // Patching to not_entrant can happen while activations of the method are
  // in use. The patching in that instance must happen only when certain
  // alignment restrictions are true. These guarantees check those
  // conditions.
#ifdef AMD64
  const int linesize = 64;
#else
  const int linesize = 32;
#endif // AMD64

  // Must be wordSize aligned
  guarantee(((uintptr_t) verified_entry & (wordSize -1)) == 0,
            "illegal address for code patching 2");
  // First 5 bytes must be within the same cache line - 4827828
  guarantee((uintptr_t) verified_entry / linesize ==
            ((uintptr_t) verified_entry + 4) / linesize,
            "illegal address for code patching 3");
}


// MT safe inserting of a jump over an unknown instruction sequence (used by nmethod::makeZombie)
// The problem: jmp <dest> is a 5-byte instruction. Atomical write can be only with 4 bytes.
// First patches the first word atomically to be a jump to itself.
// Then patches the last byte  and then atomically patches the first word (4-bytes),
// thus inserting the desired jump
// This code is mt-safe with the following conditions: entry point is 4 byte aligned,
// entry point is in same cache line as unverified entry point, and the instruction being
// patched is >= 5 byte (size of patch).
//
// In C2 the 5+ byte sized instruction is enforced by code in MachPrologNode::emit.
// In C1 the restriction is enforced by CodeEmitter::method_entry
// In JVMCI, the restriction is enforced by HotSpotFrameContext.enter(...)
//
void NativeJump::patch_verified_entry(address entry, address verified_entry, address dest) {
  // complete jump instruction (to be inserted) is in code_buffer;
  unsigned char code_buffer[5];
  code_buffer[0] = instruction_code;
  intptr_t disp = (intptr_t)dest - ((intptr_t)verified_entry + 1 + 4);
#ifdef AMD64
  guarantee(disp == (intptr_t)(int32_t)disp, "must be 32-bit offset");
#endif // AMD64
  *(int32_t*)(code_buffer + 1) = (int32_t)disp;

  check_verified_entry_alignment(entry, verified_entry);

  // Can't call nativeJump_at() because it's asserts jump exists
  NativeJump* n_jump = (NativeJump*) verified_entry;

  //First patch dummy jmp in place

  unsigned char patch[4];
  assert(sizeof(patch)==sizeof(int32_t), "sanity check");
  patch[0] = 0xEB;       // jmp rel8
  patch[1] = 0xFE;       // jmp to self
  patch[2] = 0xEB;
  patch[3] = 0xFE;

  // First patch dummy jmp in place
  *(int32_t*)verified_entry = *(int32_t *)patch;

  n_jump->wrote(0);

  // Patch 5th byte (from jump instruction)
  verified_entry[4] = code_buffer[4];

  n_jump->wrote(4);

  // Patch bytes 0-3 (from jump instruction)
  *(int32_t*)verified_entry = *(int32_t *)code_buffer;
  // Invalidate.  Opteron requires a flush after every write.
  n_jump->wrote(0);

}

address NativeFarJump::jump_destination() const          {
  NativeMovConstReg* mov = nativeMovConstReg_at(addr_at(0));
  return (address)mov->data();
}

void NativeFarJump::verify() {
  if (is_far_jump()) {
    NativeMovConstReg* mov = nativeMovConstReg_at(addr_at(0));
    NativeInstruction* jmp = nativeInstruction_at(mov->next_instruction_address());
    if (jmp->is_jump_reg()) return;
  }
  fatal("not a jump instruction");
}

void NativePopReg::insert(address code_pos, Register reg) {
  assert(reg->encoding() < 8, "no space for REX");
  assert(NativePopReg::instruction_size == sizeof(char), "right address unit for update");
  *code_pos = (u_char)(instruction_code | reg->encoding());
  ICache::invalidate_range(code_pos, instruction_size);
}


void NativeIllegalInstruction::insert(address code_pos) {
  assert(NativeIllegalInstruction::instruction_size == sizeof(short), "right address unit for update");
  *(short *)code_pos = instruction_code;
  ICache::invalidate_range(code_pos, instruction_size);
}

void NativeGeneralJump::verify() {
  assert(((NativeInstruction *)this)->is_jump() ||
         ((NativeInstruction *)this)->is_cond_jump(), "not a general jump instruction");
}


void NativeGeneralJump::insert_unconditional(address code_pos, address entry) {
  intptr_t disp = (intptr_t)entry - ((intptr_t)code_pos + 1 + 4);
#ifdef AMD64
  guarantee(disp == (intptr_t)(int32_t)disp, "must be 32-bit offset");
#endif // AMD64

  *code_pos = unconditional_long_jump;
  *((int32_t *)(code_pos+1)) = (int32_t) disp;
  ICache::invalidate_range(code_pos, instruction_size);
}


// MT-safe patching of a long jump instruction.
// First patches first word of instruction to two jmp's that jmps to them
// selfs (spinlock). Then patches the last byte, and then atomicly replaces
// the jmp's with the first 4 byte of the new instruction.
void NativeGeneralJump::replace_mt_safe(address instr_addr, address code_buffer) {
   assert (instr_addr != NULL, "illegal address for code patching (4)");
   NativeGeneralJump* n_jump =  nativeGeneralJump_at (instr_addr); // checking that it is a jump

   // Temporary code
   unsigned char patch[4];
   assert(sizeof(patch)==sizeof(int32_t), "sanity check");
   patch[0] = 0xEB;       // jmp rel8
   patch[1] = 0xFE;       // jmp to self
   patch[2] = 0xEB;
   patch[3] = 0xFE;

   // First patch dummy jmp in place
   *(int32_t*)instr_addr = *(int32_t *)patch;
    n_jump->wrote(0);

   // Patch 4th byte
   instr_addr[4] = code_buffer[4];

    n_jump->wrote(4);

   // Patch bytes 0-3
   *(jint*)instr_addr = *(jint *)code_buffer;

    n_jump->wrote(0);

#ifdef ASSERT
   // verify patching
   for ( int i = 0; i < instruction_size; i++) {
     address ptr = (address)((intptr_t)code_buffer + i);
     int a_byte = (*ptr) & 0xFF;
     assert(*((address)((intptr_t)instr_addr + i)) == a_byte, "mt safe patching failed");
   }
#endif

}



address NativeGeneralJump::jump_destination() const {
  int op_code = ubyte_at(0);
  bool is_rel32off = (op_code == 0xE9 || op_code == 0x0F);
  int  offset  = (op_code == 0x0F)  ? 2 : 1;
  int  length  = offset + ((is_rel32off) ? 4 : 1);

  if (is_rel32off)
    return addr_at(0) + length + int_at(offset);
  else
    return addr_at(0) + length + sbyte_at(offset);
}
