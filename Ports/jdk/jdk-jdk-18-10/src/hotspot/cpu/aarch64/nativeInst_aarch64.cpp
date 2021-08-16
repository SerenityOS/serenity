/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_aarch64.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/ostream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

void NativeCall::verify() {
  assert(NativeCall::is_call_at((address)this), "unexpected code at call site");
}

void NativeInstruction::wrote(int offset) {
  ICache::invalidate_word(addr_at(offset));
}

void NativeLoadGot::report_and_fail() const {
  tty->print_cr("Addr: " INTPTR_FORMAT, p2i(instruction_address()));
  fatal("not a indirect rip mov to rbx");
}

void NativeLoadGot::verify() const {
  assert(is_adrp_at((address)this), "must be adrp");
}

address NativeLoadGot::got_address() const {
  return MacroAssembler::target_addr_for_insn((address)this);
}

intptr_t NativeLoadGot::data() const {
  return *(intptr_t *) got_address();
}

address NativePltCall::destination() const {
  NativeGotJump* jump = nativeGotJump_at(plt_jump());
  return *(address*)MacroAssembler::target_addr_for_insn((address)jump);
}

address NativePltCall::plt_entry() const {
  return MacroAssembler::target_addr_for_insn((address)this);
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
  assert(NativeCall::is_call_at((address)this), "unexpected code at call site");
}

address NativeGotJump::got_address() const {
  return MacroAssembler::target_addr_for_insn((address)this);
}

address NativeGotJump::destination() const {
  address *got_entry = (address *) got_address();
  return *got_entry;
}

bool NativeGotJump::is_GotJump() const {
  NativeInstruction *insn =
    nativeInstruction_at(addr_at(3 * NativeInstruction::instruction_size));
  return insn->encoding() == 0xd61f0200; // br x16
}

void NativeGotJump::verify() const {
  assert(is_adrp_at((address)this), "must be adrp");
}

address NativeCall::destination() const {
  address addr = (address)this;
  address destination = instruction_address() + displacement();

  // Do we use a trampoline stub for this call?
  CodeBlob* cb = CodeCache::find_blob_unsafe(addr);   // Else we get assertion if nmethod is zombie.
  assert(cb && cb->is_nmethod(), "sanity");
  nmethod *nm = (nmethod *)cb;
  if (nm->stub_contains(destination) && is_NativeCallTrampolineStub_at(destination)) {
    // Yes we do, so get the destination from the trampoline stub.
    const address trampoline_stub_addr = destination;
    destination = nativeCallTrampolineStub_at(trampoline_stub_addr)->destination();
  }

  return destination;
}

// Similar to replace_mt_safe, but just changes the destination. The
// important thing is that free-running threads are able to execute this
// call instruction at all times.
//
// Used in the runtime linkage of calls; see class CompiledIC.
//
// Add parameter assert_lock to switch off assertion
// during code generation, where no patching lock is needed.
void NativeCall::set_destination_mt_safe(address dest, bool assert_lock) {
  assert(!assert_lock ||
         (Patching_lock->is_locked() || SafepointSynchronize::is_at_safepoint()) ||
         CompiledICLocker::is_safe(addr_at(0)),
         "concurrent code patching");

  ResourceMark rm;
  int code_size = NativeInstruction::instruction_size;
  address addr_call = addr_at(0);
  bool reachable = Assembler::reachable_from_branch_at(addr_call, dest);
  assert(NativeCall::is_call_at(addr_call), "unexpected code at call site");

  // Patch the constant in the call's trampoline stub.
  address trampoline_stub_addr = get_trampoline();
  if (trampoline_stub_addr != NULL) {
    assert (! is_NativeCallTrampolineStub_at(dest), "chained trampolines");
    nativeCallTrampolineStub_at(trampoline_stub_addr)->set_destination(dest);
  }

  // Patch the call.
  if (reachable) {
    set_destination(dest);
  } else {
    assert (trampoline_stub_addr != NULL, "we need a trampoline");
    set_destination(trampoline_stub_addr);
  }

  ICache::invalidate_range(addr_call, instruction_size);
}

address NativeCall::get_trampoline() {
  address call_addr = addr_at(0);

  CodeBlob *code = CodeCache::find_blob(call_addr);
  assert(code != NULL, "Could not find the containing code blob");

  address bl_destination
    = MacroAssembler::pd_call_destination(call_addr);
  if (code->contains(bl_destination) &&
      is_NativeCallTrampolineStub_at(bl_destination))
    return bl_destination;

  if (code->is_nmethod()) {
    return trampoline_stub_Relocation::get_trampoline_for(call_addr, (nmethod*)code);
  }

  return NULL;
}

// Inserts a native call instruction at a given pc
void NativeCall::insert(address code_pos, address entry) { Unimplemented(); }

//-------------------------------------------------------------------

void NativeMovConstReg::verify() {
  if (! (nativeInstruction_at(instruction_address())->is_movz() ||
        is_adrp_at(instruction_address()) ||
        is_ldr_literal_at(instruction_address())) ) {
    fatal("should be MOVZ or ADRP or LDR (literal)");
  }
}


intptr_t NativeMovConstReg::data() const {
  // das(uint64_t(instruction_address()),2);
  address addr = MacroAssembler::target_addr_for_insn(instruction_address());
  if (maybe_cpool_ref(instruction_address())) {
    return *(intptr_t*)addr;
  } else {
    return (intptr_t)addr;
  }
}

void NativeMovConstReg::set_data(intptr_t x) {
  if (maybe_cpool_ref(instruction_address())) {
    address addr = MacroAssembler::target_addr_for_insn(instruction_address());
    *(intptr_t*)addr = x;
  } else {
    // Store x into the instruction stream.
    MacroAssembler::pd_patch_instruction(instruction_address(), (address)x);
    ICache::invalidate_range(instruction_address(), instruction_size);
  }

  // Find and replace the oop/metadata corresponding to this
  // instruction in oops section.
  CodeBlob* cb = CodeCache::find_blob(instruction_address());
  nmethod* nm = cb->as_nmethod_or_null();
  if (nm != NULL) {
    RelocIterator iter(nm, instruction_address(), next_instruction_address());
    while (iter.next()) {
      if (iter.type() == relocInfo::oop_type) {
        oop* oop_addr = iter.oop_reloc()->oop_addr();
        *oop_addr = cast_to_oop(x);
        break;
      } else if (iter.type() == relocInfo::metadata_type) {
        Metadata** metadata_addr = iter.metadata_reloc()->metadata_addr();
        *metadata_addr = (Metadata*)x;
        break;
      }
    }
  }
}

void NativeMovConstReg::print() {
  tty->print_cr(PTR_FORMAT ": mov reg, " INTPTR_FORMAT,
                p2i(instruction_address()), data());
}

//-------------------------------------------------------------------

int NativeMovRegMem::offset() const  {
  address pc = instruction_address();
  unsigned insn = *(unsigned*)pc;
  if (Instruction_aarch64::extract(insn, 28, 24) == 0b10000) {
    address addr = MacroAssembler::target_addr_for_insn(pc);
    return *addr;
  } else {
    return (int)(intptr_t)MacroAssembler::target_addr_for_insn(instruction_address());
  }
}

void NativeMovRegMem::set_offset(int x) {
  address pc = instruction_address();
  unsigned insn = *(unsigned*)pc;
  if (maybe_cpool_ref(pc)) {
    address addr = MacroAssembler::target_addr_for_insn(pc);
    *(int64_t*)addr = x;
  } else {
    MacroAssembler::pd_patch_instruction(pc, (address)intptr_t(x));
    ICache::invalidate_range(instruction_address(), instruction_size);
  }
}

void NativeMovRegMem::verify() {
#ifdef ASSERT
  address dest = MacroAssembler::target_addr_for_insn(instruction_address());
#endif
}

//--------------------------------------------------------------------------------

void NativeJump::verify() { ; }


void NativeJump::check_verified_entry_alignment(address entry, address verified_entry) {
}


address NativeJump::jump_destination() const          {
  address dest = MacroAssembler::target_addr_for_insn(instruction_address());

  // We use jump to self as the unresolved address which the inline
  // cache code (and relocs) know about
  // As a special case we also use sequence movptr(r,0); br(r);
  // i.e. jump to 0 when we need leave space for a wide immediate
  // load

  // return -1 if jump to self or to 0
  if ((dest == (address)this) || dest == 0) {
    dest = (address) -1;
  }
  return dest;
}

void NativeJump::set_jump_destination(address dest) {
  // We use jump to self as the unresolved address which the inline
  // cache code (and relocs) know about
  if (dest == (address) -1)
    dest = instruction_address();

  MacroAssembler::pd_patch_instruction(instruction_address(), dest);
  ICache::invalidate_range(instruction_address(), instruction_size);
};

//-------------------------------------------------------------------

address NativeGeneralJump::jump_destination() const {
  NativeMovConstReg* move = nativeMovConstReg_at(instruction_address());
  address dest = (address) move->data();

  // We use jump to self as the unresolved address which the inline
  // cache code (and relocs) know about
  // As a special case we also use jump to 0 when first generating
  // a general jump

  // return -1 if jump to self or to 0
  if ((dest == (address)this) || dest == 0) {
    dest = (address) -1;
  }
  return dest;
}

void NativeGeneralJump::set_jump_destination(address dest) {
  NativeMovConstReg* move = nativeMovConstReg_at(instruction_address());

  // We use jump to self as the unresolved address which the inline
  // cache code (and relocs) know about
  if (dest == (address) -1) {
    dest = instruction_address();
  }

  move->set_data((uintptr_t) dest);
};

//-------------------------------------------------------------------

bool NativeInstruction::is_safepoint_poll() {
  // a safepoint_poll is implemented in two steps as either
  //
  // adrp(reg, polling_page);
  // ldr(zr, [reg, #offset]);
  //
  // or
  //
  // mov(reg, polling_page);
  // ldr(zr, [reg, #offset]);
  //
  // or
  //
  // ldr(reg, [rthread, #offset]);
  // ldr(zr, [reg, #offset]);
  //
  // however, we cannot rely on the polling page address load always
  // directly preceding the read from the page. C1 does that but C2
  // has to do the load and read as two independent instruction
  // generation steps. that's because with a single macro sequence the
  // generic C2 code can only add the oop map before the mov/adrp and
  // the trap handler expects an oop map to be associated with the
  // load. with the load scheuled as a prior step the oop map goes
  // where it is needed.
  //
  // so all we can do here is check that marked instruction is a load
  // word to zr
  return is_ldrw_to_zr(address(this));
}

bool NativeInstruction::is_adrp_at(address instr) {
  unsigned insn = *(unsigned*)instr;
  return (Instruction_aarch64::extract(insn, 31, 24) & 0b10011111) == 0b10010000;
}

bool NativeInstruction::is_ldr_literal_at(address instr) {
  unsigned insn = *(unsigned*)instr;
  return (Instruction_aarch64::extract(insn, 29, 24) & 0b011011) == 0b00011000;
}

bool NativeInstruction::is_ldrw_to_zr(address instr) {
  unsigned insn = *(unsigned*)instr;
  return (Instruction_aarch64::extract(insn, 31, 22) == 0b1011100101 &&
          Instruction_aarch64::extract(insn, 4, 0) == 0b11111);
}

bool NativeInstruction::is_general_jump() {
  if (is_movz()) {
    NativeInstruction* inst1 = nativeInstruction_at(addr_at(instruction_size * 1));
    if (inst1->is_movk()) {
      NativeInstruction* inst2 = nativeInstruction_at(addr_at(instruction_size * 2));
      if (inst2->is_movk()) {
        NativeInstruction* inst3 = nativeInstruction_at(addr_at(instruction_size * 3));
        if (inst3->is_blr()) {
          return true;
        }
      }
    }
  }
  return false;
}

bool NativeInstruction::is_movz() {
  return Instruction_aarch64::extract(int_at(0), 30, 23) == 0b10100101;
}

bool NativeInstruction::is_movk() {
  return Instruction_aarch64::extract(int_at(0), 30, 23) == 0b11100101;
}

bool NativeInstruction::is_sigill_zombie_not_entrant() {
  return uint_at(0) == 0xd4bbd5a1; // dcps1 #0xdead
}

void NativeIllegalInstruction::insert(address code_pos) {
  *(juint*)code_pos = 0xd4bbd5a1; // dcps1 #0xdead
}

bool NativeInstruction::is_stop() {
  return uint_at(0) == 0xd4bbd5c1; // dcps1 #0xdeae
}

//-------------------------------------------------------------------

// MT-safe inserting of a jump over a jump or a nop (used by
// nmethod::make_not_entrant_or_zombie)

void NativeJump::patch_verified_entry(address entry, address verified_entry, address dest) {

  assert(dest == SharedRuntime::get_handle_wrong_method_stub(), "expected fixed destination of patch");
  assert(nativeInstruction_at(verified_entry)->is_jump_or_nop()
         || nativeInstruction_at(verified_entry)->is_sigill_zombie_not_entrant(),
         "Aarch64 cannot replace non-jump with jump");

  // Patch this nmethod atomically.
  if (Assembler::reachable_from_branch_at(verified_entry, dest)) {
    ptrdiff_t disp = dest - verified_entry;
    guarantee(disp < 1 << 27 && disp > - (1 << 27), "branch overflow");

    unsigned int insn = (0b000101 << 26) | ((disp >> 2) & 0x3ffffff);
    *(unsigned int*)verified_entry = insn;
  } else {
    // We use an illegal instruction for marking a method as
    // not_entrant or zombie.
    NativeIllegalInstruction::insert(verified_entry);
  }

  ICache::invalidate_range(verified_entry, instruction_size);
}

void NativeGeneralJump::verify() {  }

void NativeGeneralJump::insert_unconditional(address code_pos, address entry) {
  NativeGeneralJump* n_jump = (NativeGeneralJump*)code_pos;

  CodeBuffer cb(code_pos, instruction_size);
  MacroAssembler a(&cb);

  a.movptr(rscratch1, (uintptr_t)entry);
  a.br(rscratch1);

  ICache::invalidate_range(code_pos, instruction_size);
}

// MT-safe patching of a long jump instruction.
void NativeGeneralJump::replace_mt_safe(address instr_addr, address code_buffer) {
  ShouldNotCallThis();
}

address NativeCallTrampolineStub::destination(nmethod *nm) const {
  return ptr_at(data_offset);
}

void NativeCallTrampolineStub::set_destination(address new_destination) {
  set_ptr_at(data_offset, new_destination);
  OrderAccess::release();
}

// Generate a trampoline for a branch to dest.  If there's no need for a
// trampoline, simply patch the call directly to dest.
address NativeCall::trampoline_jump(CodeBuffer &cbuf, address dest) {
  MacroAssembler a(&cbuf);
  address stub = NULL;

  if (a.far_branches()
      && ! is_NativeCallTrampolineStub_at(instruction_address() + displacement())) {
    stub = a.emit_trampoline_stub(instruction_address() - cbuf.insts()->start(), dest);
  }

  if (stub == NULL) {
    // If we generated no stub, patch this call directly to dest.
    // This will happen if we don't need far branches or if there
    // already was a trampoline.
    set_destination(dest);
  }

  return stub;
}
