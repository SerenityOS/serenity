/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2020 SAP SE. All rights reserved.
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
#include "code/compiledIC.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_ppc.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.hpp"
#include "runtime/handles.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/ostream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

// We use an illtrap for marking a method as not_entrant or zombie
// Work around a C++ compiler bug which changes 'this'
bool NativeInstruction::is_sigill_zombie_not_entrant_at(address addr) {
  if (*(int*)addr != 0 /*illtrap*/) return false;
  CodeBlob* cb = CodeCache::find_blob_unsafe(addr);
  if (cb == NULL || !cb->is_nmethod()) return false;
  nmethod *nm = (nmethod *)cb;
  // This method is not_entrant or zombie iff the illtrap instruction is
  // located at the verified entry point.
  return nm->verified_entry_point() == addr;
}

#ifdef ASSERT
void NativeInstruction::verify() {
  // Make sure code pattern is actually an instruction address.
  address addr = addr_at(0);
  if (addr == 0 || ((intptr_t)addr & 3) != 0) {
    fatal("not an instruction address");
  }
}
#endif // ASSERT

// Extract call destination from a NativeCall. The call might use a trampoline stub.
address NativeCall::destination() const {
  address addr = (address)this;
  address destination = Assembler::bxx_destination(addr);

  // Do we use a trampoline stub for this call?
  // Trampoline stubs are located behind the main code.
  if (destination > addr) {
    // Filter out recursive method invocation (call to verified/unverified entry point).
    CodeBlob* cb = CodeCache::find_blob_unsafe(addr);   // Else we get assertion if nmethod is zombie.
    assert(cb && cb->is_nmethod(), "sanity");
    nmethod *nm = (nmethod *)cb;
    if (nm->stub_contains(destination) && is_NativeCallTrampolineStub_at(destination)) {
      // Yes we do, so get the destination from the trampoline stub.
      const address trampoline_stub_addr = destination;
      destination = NativeCallTrampolineStub_at(trampoline_stub_addr)->destination(nm);
    }
  }

  return destination;
}

// Similar to replace_mt_safe, but just changes the destination. The
// important thing is that free-running threads are able to execute this
// call instruction at all times. Thus, the displacement field must be
// instruction-word-aligned.
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
  int code_size = 1 * BytesPerInstWord;
  address addr_call = addr_at(0);
  assert(MacroAssembler::is_bl(*(int*)addr_call), "unexpected code at call-site");

  CodeBuffer cb(addr_call, code_size + 1);
  MacroAssembler* a = new MacroAssembler(&cb);

  // Patch the call.
  if (!ReoptimizeCallSequences || !a->is_within_range_of_b(dest, addr_call)) {
    address trampoline_stub_addr = get_trampoline();

    // We did not find a trampoline stub because the current codeblob
    // does not provide this information. The branch will be patched
    // later during a final fixup, when all necessary information is
    // available.
    if (trampoline_stub_addr == 0)
      return;

    // Patch the constant in the call's trampoline stub.
    NativeCallTrampolineStub_at(trampoline_stub_addr)->set_destination(dest);
    dest = trampoline_stub_addr;
  }

  OrderAccess::release();
  a->bl(dest);

  ICache::ppc64_flush_icache_bytes(addr_call, code_size);
}

address NativeCall::get_trampoline() {
  address call_addr = addr_at(0);

  CodeBlob *code = CodeCache::find_blob(call_addr);
  assert(code != NULL, "Could not find the containing code blob");

  // There are no relocations available when the code gets relocated
  // because of CodeBuffer expansion.
  if (code->relocation_size() == 0)
    return NULL;

  address bl_destination = Assembler::bxx_destination(call_addr);
  if (code->contains(bl_destination) &&
      is_NativeCallTrampolineStub_at(bl_destination))
    return bl_destination;

  // If the codeBlob is not a nmethod, this is because we get here from the
  // CodeBlob constructor, which is called within the nmethod constructor.
  return trampoline_stub_Relocation::get_trampoline_for(call_addr, (nmethod*)code);
}

#ifdef ASSERT
void NativeCall::verify() {
  address addr = addr_at(0);

  if (!NativeCall::is_call_at(addr)) {
    tty->print_cr("not a NativeCall at " PTR_FORMAT, p2i(addr));
    // TODO: PPC port: Disassembler::decode(addr - 20, addr + 20, tty);
    fatal("not a NativeCall at " PTR_FORMAT, p2i(addr));
  }
}
#endif // ASSERT

#ifdef ASSERT
void NativeFarCall::verify() {
  address addr = addr_at(0);

  NativeInstruction::verify();
  if (!NativeFarCall::is_far_call_at(addr)) {
    tty->print_cr("not a NativeFarCall at " PTR_FORMAT, p2i(addr));
    // TODO: PPC port: Disassembler::decode(addr, 20, 20, tty);
    fatal("not a NativeFarCall at " PTR_FORMAT, p2i(addr));
  }
}
#endif // ASSERT

address NativeMovConstReg::next_instruction_address() const {
#ifdef ASSERT
  CodeBlob* nm = CodeCache::find_blob(instruction_address());
  assert(nm != NULL, "Could not find code blob");
  assert(!MacroAssembler::is_set_narrow_oop(addr_at(0), nm->content_begin()), "Should not patch narrow oop here");
#endif

  if (MacroAssembler::is_load_const_from_method_toc_at(addr_at(0))) {
    return addr_at(load_const_from_method_toc_instruction_size);
  } else {
    return addr_at(load_const_instruction_size);
  }
}

intptr_t NativeMovConstReg::data() const {
  address   addr = addr_at(0);

  if (MacroAssembler::is_load_const_at(addr)) {
    return MacroAssembler::get_const(addr);
  }

  CodeBlob* cb = CodeCache::find_blob_unsafe(addr);
  assert(cb != NULL, "Could not find code blob");
  if (MacroAssembler::is_set_narrow_oop(addr, cb->content_begin())) {
    narrowOop no = MacroAssembler::get_narrow_oop(addr, cb->content_begin());
    // We can reach here during GC with 'no' pointing to new object location
    // while 'heap()->is_in' still reports false (e.g. with SerialGC).
    // Therefore we use raw decoding.
    if (CompressedOops::is_null(no)) return 0;
    return cast_from_oop<intptr_t>(CompressedOops::decode_raw(no));
  } else {
    assert(MacroAssembler::is_load_const_from_method_toc_at(addr), "must be load_const_from_pool");

    address ctable = cb->content_begin();
    int offset = MacroAssembler::get_offset_of_load_const_from_method_toc_at(addr);
    return *(intptr_t *)(ctable + offset);
  }
}

address NativeMovConstReg::set_data_plain(intptr_t data, CodeBlob *cb) {
  address addr         = instruction_address();
  address next_address = NULL;
  if (!cb) cb = CodeCache::find_blob(addr);

  if (cb != NULL && MacroAssembler::is_load_const_from_method_toc_at(addr)) {
    // A load from the method's TOC (ctable).
    assert(cb->is_nmethod(), "must be nmethod");
    const address ctable = cb->content_begin();
    const int toc_offset = MacroAssembler::get_offset_of_load_const_from_method_toc_at(addr);
    *(intptr_t *)(ctable + toc_offset) = data;
    next_address = addr + BytesPerInstWord;
  } else if (cb != NULL &&
             MacroAssembler::is_calculate_address_from_global_toc_at(addr, cb->content_begin())) {
    // A calculation relative to the global TOC.
    if (MacroAssembler::get_address_of_calculate_address_from_global_toc_at(addr, cb->content_begin()) !=
        (address)data) {
      const address inst2_addr = addr;
      const address inst1_addr =
        MacroAssembler::patch_calculate_address_from_global_toc_at(inst2_addr, cb->content_begin(),
                                                                   (address)data);
      assert(inst1_addr != NULL && inst1_addr < inst2_addr, "first instruction must be found");
      const int range = inst2_addr - inst1_addr + BytesPerInstWord;
      ICache::ppc64_flush_icache_bytes(inst1_addr, range);
    }
    next_address = addr + 1 * BytesPerInstWord;
  } else if (MacroAssembler::is_load_const_at(addr)) {
    // A normal 5 instruction load_const code sequence.
    if (MacroAssembler::get_const(addr) != (long)data) {
      // This is not mt safe, ok in methods like CodeBuffer::copy_code().
      MacroAssembler::patch_const(addr, (long)data);
      ICache::ppc64_flush_icache_bytes(addr, load_const_instruction_size);
    }
    next_address = addr + 5 * BytesPerInstWord;
  } else if (MacroAssembler::is_bl(* (int*) addr)) {
    // A single branch-and-link instruction.
    ResourceMark rm;
    const int code_size = 1 * BytesPerInstWord;
    CodeBuffer cb(addr, code_size + 1);
    MacroAssembler* a = new MacroAssembler(&cb);
    a->bl((address) data);
    ICache::ppc64_flush_icache_bytes(addr, code_size);
    next_address = addr + code_size;
  } else {
    ShouldNotReachHere();
  }

  return next_address;
}

void NativeMovConstReg::set_data(intptr_t data) {
  // Store the value into the instruction stream.
  CodeBlob *cb = CodeCache::find_blob(instruction_address());
  address next_address = set_data_plain(data, cb);

  // Also store the value into an oop_Relocation cell, if any.
  if (cb && cb->is_nmethod()) {
    RelocIterator iter((nmethod *) cb, instruction_address(), next_address);
    oop* oop_addr = NULL;
    Metadata** metadata_addr = NULL;
    while (iter.next()) {
      if (iter.type() == relocInfo::oop_type) {
        oop_Relocation *r = iter.oop_reloc();
        if (oop_addr == NULL) {
          oop_addr = r->oop_addr();
          *oop_addr = cast_to_oop(data);
        } else {
          assert(oop_addr == r->oop_addr(), "must be only one set-oop here");
        }
      }
      if (iter.type() == relocInfo::metadata_type) {
        metadata_Relocation *r = iter.metadata_reloc();
        if (metadata_addr == NULL) {
          metadata_addr = r->metadata_addr();
          *metadata_addr = (Metadata*)data;
        } else {
          assert(metadata_addr == r->metadata_addr(), "must be only one set-metadata here");
        }
      }
    }
  }
}

void NativeMovConstReg::set_narrow_oop(narrowOop data, CodeBlob *code /* = NULL */) {
  address   inst2_addr = addr_at(0);
  CodeBlob* cb = (code) ? code : CodeCache::find_blob(instruction_address());
  assert(cb != NULL, "Could not find code blob");
  if (MacroAssembler::get_narrow_oop(inst2_addr, cb->content_begin()) == data) {
    return;
  }
  const address inst1_addr =
    MacroAssembler::patch_set_narrow_oop(inst2_addr, cb->content_begin(), data);
  assert(inst1_addr != NULL && inst1_addr < inst2_addr, "first instruction must be found");
  const int range = inst2_addr - inst1_addr + BytesPerInstWord;
  ICache::ppc64_flush_icache_bytes(inst1_addr, range);
}

// Do not use an assertion here. Let clients decide whether they only
// want this when assertions are enabled.
#ifdef ASSERT
void NativeMovConstReg::verify() {
  address   addr = addr_at(0);
  if (! MacroAssembler::is_load_const_at(addr) &&
      ! MacroAssembler::is_load_const_from_method_toc_at(addr)) {
    CodeBlob* cb = CodeCache::find_blob_unsafe(addr);   // find_nmethod() asserts if nmethod is zombie.
    if (! (cb != NULL && MacroAssembler::is_calculate_address_from_global_toc_at(addr, cb->content_begin())) &&
        ! (cb != NULL && MacroAssembler::is_set_narrow_oop(addr, cb->content_begin())) &&
        ! MacroAssembler::is_bl(*((int*) addr))) {
      tty->print_cr("not a NativeMovConstReg at " PTR_FORMAT, p2i(addr));
      // TODO: PPC port: Disassembler::decode(addr, 20, 20, tty);
      fatal("not a NativeMovConstReg at " PTR_FORMAT, p2i(addr));
    }
  }
}
#endif // ASSERT

void NativeJump::patch_verified_entry(address entry, address verified_entry, address dest) {
  ResourceMark rm;
  int code_size = 1 * BytesPerInstWord;
  CodeBuffer cb(verified_entry, code_size + 1);
  MacroAssembler* a = new MacroAssembler(&cb);
#ifdef COMPILER2
  assert(dest == SharedRuntime::get_handle_wrong_method_stub(), "expected fixed destination of patch");
#endif
  // Patch this nmethod atomically. Always use illtrap/trap in debug build.
  if (DEBUG_ONLY(false &&) a->is_within_range_of_b(dest, a->pc())) {
    a->b(dest);
  } else {
    // The signal handler will continue at dest=OptoRuntime::handle_wrong_method_stub().
    // We use an illtrap for marking a method as not_entrant or zombie.
    a->illtrap();
  }
  ICache::ppc64_flush_icache_bytes(verified_entry, code_size);
}

#ifdef ASSERT
void NativeJump::verify() {
  address addr = addr_at(0);

  NativeInstruction::verify();
  if (!NativeJump::is_jump_at(addr)) {
    tty->print_cr("not a NativeJump at " PTR_FORMAT, p2i(addr));
    // TODO: PPC port: Disassembler::decode(addr, 20, 20, tty);
    fatal("not a NativeJump at " PTR_FORMAT, p2i(addr));
  }
}
#endif // ASSERT


void NativeGeneralJump::insert_unconditional(address code_pos, address entry) {
  CodeBuffer cb(code_pos, BytesPerInstWord + 1);
  MacroAssembler a(&cb);
  a.b(entry);
  ICache::ppc64_flush_icache_bytes(code_pos, NativeGeneralJump::instruction_size);
}

// MT-safe patching of a jmp instruction.
void NativeGeneralJump::replace_mt_safe(address instr_addr, address code_buffer) {
  // Bytes beyond offset NativeGeneralJump::instruction_size are copied by caller.

  // Finally patch out the jump.
  volatile juint *jump_addr = (volatile juint*)instr_addr;
  // Release not needed because caller uses invalidate_range after copying the remaining bytes.
  //Atomic::release_store(jump_addr, *((juint*)code_buffer));
  *jump_addr = *((juint*)code_buffer); // atomically store code over branch instruction
  ICache::ppc64_flush_icache_bytes(instr_addr, NativeGeneralJump::instruction_size);
}


//-------------------------------------------------------------------

// Call trampoline stubs.
//
// Layout and instructions of a call trampoline stub:
//    0:  load the TOC (part 1)
//    4:  load the TOC (part 2)
//    8:  load the call target from the constant pool (part 1)
//  [12:  load the call target from the constant pool (part 2, optional)]
//   ..:  branch via CTR
//

address NativeCallTrampolineStub::encoded_destination_addr() const {
  address instruction_addr = addr_at(0 * BytesPerInstWord);
  if (!MacroAssembler::is_ld_largeoffset(instruction_addr)) {
    instruction_addr = addr_at(2 * BytesPerInstWord);
    assert(MacroAssembler::is_ld_largeoffset(instruction_addr),
           "must be a ld with large offset (from the constant pool)");
  }
  return instruction_addr;
}

address NativeCallTrampolineStub::destination(nmethod *nm) const {
  CodeBlob* cb = nm ? nm : CodeCache::find_blob_unsafe(addr_at(0));
  assert(cb != NULL, "Could not find code blob");
  address ctable = cb->content_begin();

  return *(address*)(ctable + destination_toc_offset());
}

int NativeCallTrampolineStub::destination_toc_offset() const {
  return MacroAssembler::get_ld_largeoffset_offset(encoded_destination_addr());
}

void NativeCallTrampolineStub::set_destination(address new_destination) {
  CodeBlob* cb = CodeCache::find_blob(addr_at(0));
  assert(cb != NULL, "Could not find code blob");
  address ctable = cb->content_begin();

  *(address*)(ctable + destination_toc_offset()) = new_destination;
}
