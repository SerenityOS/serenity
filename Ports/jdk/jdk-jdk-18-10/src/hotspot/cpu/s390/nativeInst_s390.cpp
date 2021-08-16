/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

// Major contributions by JL, LS

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_s390.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/ostream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

#define LUCY_DBG

//-------------------------------------
//  N a t i v e I n s t r u c t i o n
//-------------------------------------

// Define this switch to prevent identity updates.
// In high-concurrency scenarios, it is beneficial to prevent
// identity updates. It has a positive effect on cache line steals.
// and invalidations.
// Test runs of JVM98, JVM2008, and JBB2005 show a very low frequency
// of identity updates. Detection is therefore disabled.
#undef SUPPRESS_IDENTITY_UPDATE

void NativeInstruction::verify() {
  // Make sure code pattern is actually an instruction address.
  // Do not allow:
  //  - NULL
  //  - any address in first page (0x0000 .. 0x0fff)
  //  - odd address (will cause a "specification exception")
  address addr = addr_at(0);
  if ((addr == 0) || (((unsigned long)addr & ~0x0fff) == 0) || ((intptr_t)addr & 1) != 0) {
    tty->print_cr(INTPTR_FORMAT ": bad instruction address", p2i(addr));
    fatal("not an instruction address");
  }
}

// Print location and value (hex representation) of current NativeInstruction
void NativeInstruction::print(const char* msg) const {
  int len = Assembler::instr_len(addr_at(0));
  if (msg == NULL) { // Output line without trailing blanks.
    switch (len) {
      case 2: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x",             p2i(addr_at(0)), len, halfword_at(0));                                 break;
      case 4: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x %4.4x",       p2i(addr_at(0)), len, halfword_at(0), halfword_at(2));                 break;
      case 6: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x %4.4x %4.4x", p2i(addr_at(0)), len, halfword_at(0), halfword_at(2), halfword_at(4)); break;
      default: // Never reached. instr_len() always returns one of the above values. Keep the compiler happy.
        ShouldNotReachHere();
        break;
    }
  } else { // Output line with filler blanks to have msg aligned.
    switch (len) {
      case 2: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x           %s",   p2i(addr_at(0)), len, halfword_at(0), msg);                                 break;
      case 4: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x %4.4x      %s",  p2i(addr_at(0)), len, halfword_at(0), halfword_at(2), msg);                 break;
      case 6: tty->print_cr(INTPTR_FORMAT "(len=%d): %4.4x %4.4x %4.4x %s", p2i(addr_at(0)), len, halfword_at(0), halfword_at(2), halfword_at(4), msg); break;
      default: // Never reached. instr_len() always returns one of the above values. Keep the compiler happy.
        ShouldNotReachHere();
        break;
    }
  }
}
void NativeInstruction::print() const {
  print(NULL);
}

// Hex-Dump of storage around current NativeInstruction. Also try disassembly.
void NativeInstruction::dump(const unsigned int range, const char* msg) const {
  Assembler::dump_code_range(tty, addr_at(0), range, (msg == NULL) ? "":msg);
}

void NativeInstruction::dump(const unsigned int range) const {
  dump(range, NULL);
}

void NativeInstruction::dump() const {
  dump(32, NULL);
}

void NativeInstruction::set_halfword_at(int offset, short i) {
  address addr = addr_at(offset);
#ifndef SUPPRESS_IDENTITY_UPDATE
  *(short*)addr = i;
#else
  if (*(short*)addr != i) {
    *(short*)addr = i;
  }
#endif
  ICache::invalidate_word(addr);
}

void NativeInstruction::set_word_at(int offset, int i) {
  address addr = addr_at(offset);
#ifndef SUPPRESS_IDENTITY_UPDATE
  *(int*)addr = i;
#else
  if (*(int*)addr != i) {
    *(int*)addr = i;
  }
#endif
  ICache::invalidate_word(addr);
}

void NativeInstruction::set_jlong_at(int offset, jlong i) {
  address addr = addr_at(offset);
#ifndef SUPPRESS_IDENTITY_UPDATE
  *(jlong*)addr = i;
#else
  if (*(jlong*)addr != i) {
    *(jlong*)addr = i;
  }
#endif
  // Don't need to invalidate 2 words here, because
  // the flush instruction operates on doublewords.
  ICache::invalidate_word(addr);
}

#undef  SUPPRESS_IDENTITY_UPDATE

//------------------------------------------------------------

int NativeInstruction::illegal_instruction() {
  return 0;
}

bool NativeInstruction::is_illegal() {
  // An instruction with main opcode 0x00 (leftmost byte) is not a valid instruction
  // (and will never be) and causes a SIGILL where the pc points to the next instruction.
  // The caller of this method wants to know if such a situation exists at the current pc.
  //
  // The result of this method is unsharp with respect to the following facts:
  // - Stepping backwards in the instruction stream is not possible on z/Architecture.
  // - z/Architecture instructions are 2, 4, or 6 bytes in length.
  // - The instruction length is coded in the leftmost two bits of the main opcode.
  // - The result is exact if the caller knows by some other means that the
  //   instruction is of length 2.
  //
  // If this method returns false, then the 2-byte instruction at *-2 is not a 0x00 opcode.
  // If this method returns true, then the 2-byte instruction at *-2 is a 0x00 opcode.
  return halfword_at(-2) == illegal_instruction();
}

// We use an illtrap for marking a method as not_entrant or zombie.
bool NativeInstruction::is_sigill_zombie_not_entrant() {
  if (!is_illegal()) return false; // Just a quick path.

  // One-sided error of is_illegal tolerable here
  // (see implementation of is_illegal() for details).

  CodeBlob* cb = CodeCache::find_blob_unsafe(addr_at(0));
  if (cb == NULL || !cb->is_nmethod()) {
    return false;
  }

  nmethod *nm = (nmethod *)cb;
  // This method is not_entrant or zombie if the illtrap instruction
  // is located at the verified entry point.
  // BE AWARE: the current pc (this) points to the instruction after the
  // "illtrap" location.
  address sig_addr = ((address) this) - 2;
  return nm->verified_entry_point() == sig_addr;
}

bool NativeInstruction::is_jump() {
  unsigned long inst;
  Assembler::get_instruction((address)this, &inst);
  return MacroAssembler::is_branch_pcrelative_long(inst);
}

//---------------------------------------------------
//  N a t i v e I l l e g a l I n s t r u c t i o n
//---------------------------------------------------

void NativeIllegalInstruction::insert(address code_pos) {
  NativeIllegalInstruction* nii = (NativeIllegalInstruction*) nativeInstruction_at(code_pos);
  nii->set_halfword_at(0, illegal_instruction());
}

//-----------------------
//  N a t i v e C a l l
//-----------------------

void NativeCall::verify() {
  if (NativeCall::is_call_at(addr_at(0))) return;

  fatal("this is not a `NativeCall' site");
}

address NativeCall::destination() const {
  if (MacroAssembler::is_call_far_pcrelative(instruction_address())) {
    address here = addr_at(MacroAssembler::nop_size());
    return MacroAssembler::get_target_addr_pcrel(here);
  }

  return (address)((NativeMovConstReg *)this)->data();
}

// Similar to replace_mt_safe, but just changes the destination. The
// important thing is that free-running threads are able to execute this
// call instruction at all times. Thus, the displacement field must be
// 4-byte-aligned. We enforce this on z/Architecture by inserting a nop
// instruction in front of 'brasl' when needed.
//
// Used in the runtime linkage of calls; see class CompiledIC.
void NativeCall::set_destination_mt_safe(address dest) {
  if (MacroAssembler::is_call_far_pcrelative(instruction_address())) {
    address iaddr = addr_at(MacroAssembler::nop_size());
    // Ensure that patching is atomic hence mt safe.
    assert(((long)addr_at(MacroAssembler::call_far_pcrelative_size()) & (call_far_pcrelative_displacement_alignment-1)) == 0,
           "constant must be 4-byte aligned");
    set_word_at(MacroAssembler::call_far_pcrelative_size() - 4, Assembler::z_pcrel_off(dest, iaddr));
  } else {
    assert(MacroAssembler::is_load_const_from_toc(instruction_address()), "unsupported instruction");
    nativeMovConstReg_at(instruction_address())->set_data(((intptr_t)dest));
  }
}

//-----------------------------
//  N a t i v e F a r C a l l
//-----------------------------

void NativeFarCall::verify() {
  NativeInstruction::verify();
  if (NativeFarCall::is_far_call_at(addr_at(0))) return;
  fatal("not a NativeFarCall");
}

address NativeFarCall::destination() {
  assert(MacroAssembler::is_call_far_patchable_at((address)this), "unexpected call type");
  address ctable = NULL;
  return MacroAssembler::get_dest_of_call_far_patchable_at((address)this, ctable);
}


// Handles both patterns of patchable far calls.
void NativeFarCall::set_destination(address dest, int toc_offset) {
  address inst_addr = (address)this;

  // Set new destination (implementation of call may change here).
  assert(MacroAssembler::is_call_far_patchable_at(inst_addr), "unexpected call type");

  if (!MacroAssembler::is_call_far_patchable_pcrelative_at(inst_addr)) {
    address ctable = CodeCache::find_blob(inst_addr)->ctable_begin();
    // Need distance of TOC entry from current instruction.
    toc_offset = (ctable + toc_offset) - inst_addr;
    // Call is via constant table entry.
    MacroAssembler::set_dest_of_call_far_patchable_at(inst_addr, dest, toc_offset);
  } else {
    // Here, we have a pc-relative call (brasl).
    // Be aware: dest may have moved in this case, so really patch the displacement,
    // when necessary!
    // This while loop will also consume the nop which always preceeds a call_far_pcrelative.
    // We need to revert this after the loop. Pc-relative calls are always assumed to have a leading nop.
    unsigned int nop_sz    = MacroAssembler::nop_size();
    unsigned int nop_bytes = 0;
    while(MacroAssembler::is_z_nop(inst_addr+nop_bytes)) {
      nop_bytes += nop_sz;
    }
    if (nop_bytes > 0) {
      inst_addr += nop_bytes - nop_sz;
    }

    assert(MacroAssembler::is_call_far_pcrelative(inst_addr), "not a pc-relative call");
    address target = MacroAssembler::get_target_addr_pcrel(inst_addr + nop_sz);
    if (target != dest) {
      NativeCall *call = nativeCall_at(inst_addr);
      call->set_destination_mt_safe(dest);
    }
  }
}

//-------------------------------------
//  N a t i v e M o v C o n s t R e g
//-------------------------------------

// Do not use an assertion here. Let clients decide whether they only
// want this when assertions are enabled.
void NativeMovConstReg::verify() {
  address   loc = addr_at(0);

  // This while loop will also consume the nop which always preceeds a
  // call_far_pcrelative.  We need to revert this after the
  // loop. Pc-relative calls are always assumed to have a leading nop.
  unsigned int nop_sz    = MacroAssembler::nop_size();
  unsigned int nop_bytes = 0;
  while(MacroAssembler::is_z_nop(loc+nop_bytes)) {
    nop_bytes += nop_sz;
  }

  if (nop_bytes > 0) {
    if (MacroAssembler::is_call_far_pcrelative(loc+nop_bytes-nop_sz)) return;
    loc += nop_bytes;
  }

  if (!MacroAssembler::is_load_const_from_toc(loc)            &&    // Load const from TOC.
      !MacroAssembler::is_load_const(loc)                     &&    // Load const inline.
      !MacroAssembler::is_load_narrow_oop(loc)                &&    // Load narrow oop.
      !MacroAssembler::is_load_narrow_klass(loc)              &&    // Load narrow Klass ptr.
      !MacroAssembler::is_compare_immediate_narrow_oop(loc)   &&    // Compare immediate narrow.
      !MacroAssembler::is_compare_immediate_narrow_klass(loc) &&    // Compare immediate narrow.
      !MacroAssembler::is_pcrelative_instruction(loc)) {            // Just to make it run.
    tty->cr();
    tty->print_cr("NativeMovConstReg::verify(): verifying addr %p(0x%x), %d leading nops", loc, *(uint*)loc, nop_bytes/nop_sz);
    tty->cr();
    ((NativeMovConstReg*)loc)->dump(64, "NativeMovConstReg::verify()");
#ifdef LUCY_DBG
    VM_Version::z_SIGSEGV();
#endif
    fatal("this is not a `NativeMovConstReg' site");
  }
}

address NativeMovConstReg::next_instruction_address(int offset) const  {
  address inst_addr = addr_at(offset);

  // Load address (which is a constant) pc-relative.
  if (MacroAssembler::is_load_addr_pcrel(inst_addr))                  { return addr_at(offset+MacroAssembler::load_addr_pcrel_size()); }

  // Load constant from TOC.
  if (MacroAssembler::is_load_const_from_toc(inst_addr))              { return addr_at(offset+MacroAssembler::load_const_from_toc_size()); }

  // Load constant inline.
  if (MacroAssembler::is_load_const(inst_addr))                       { return addr_at(offset+MacroAssembler::load_const_size()); }

  // Load constant narrow inline.
  if (MacroAssembler::is_load_narrow_oop(inst_addr))                  { return addr_at(offset+MacroAssembler::load_narrow_oop_size()); }
  if (MacroAssembler::is_load_narrow_klass(inst_addr))                { return addr_at(offset+MacroAssembler::load_narrow_klass_size()); }

  // Compare constant narrow inline.
  if (MacroAssembler::is_compare_immediate_narrow_oop(inst_addr))     { return addr_at(offset+MacroAssembler::compare_immediate_narrow_oop_size()); }
  if (MacroAssembler::is_compare_immediate_narrow_klass(inst_addr))   { return addr_at(offset+MacroAssembler::compare_immediate_narrow_klass_size()); }

  if (MacroAssembler::is_call_far_patchable_pcrelative_at(inst_addr)) { return addr_at(offset+MacroAssembler::call_far_patchable_size()); }

  if (MacroAssembler::is_pcrelative_instruction(inst_addr))           { return addr_at(offset+Assembler::instr_len(inst_addr)); }

  ((NativeMovConstReg*)inst_addr)->dump(64, "NativeMovConstReg site is not recognized as such");
#ifdef LUCY_DBG
  VM_Version::z_SIGSEGV();
#else
  guarantee(false, "Not a NativeMovConstReg site");
#endif
  return NULL;
}

intptr_t NativeMovConstReg::data() const {
  address loc = addr_at(0);
  if (MacroAssembler::is_load_const(loc)) {
    return MacroAssembler::get_const(loc);
  } else if (MacroAssembler::is_load_narrow_oop(loc)              ||
             MacroAssembler::is_compare_immediate_narrow_oop(loc) ||
             MacroAssembler::is_load_narrow_klass(loc)            ||
             MacroAssembler::is_compare_immediate_narrow_klass(loc)) {
    ((NativeMovConstReg*)loc)->dump(32, "NativeMovConstReg::data(): cannot extract data from narrow ptr (oop or klass)");
#ifdef LUCY_DBG
    VM_Version::z_SIGSEGV();
#else
    ShouldNotReachHere();
#endif
    return *(intptr_t *)NULL;
  } else {
    // Otherwise, assume data resides in TOC. Is asserted in called method.
    return MacroAssembler::get_const_from_toc(loc);
  }
}


// Patch in a new constant.
//
// There are situations where we have multiple (hopefully two at most)
// relocations connected to one instruction. Loading an oop from CP
// using pcrelative addressing would one such example. Here we have an
// oop relocation, modifying the oop itself, and an internal word relocation,
// modifying the relative address.
//
// NativeMovConstReg::set_data is then called once for each relocation. To be
// able to distinguish between the relocations, we use a rather dirty hack:
//
// All calls that deal with an internal word relocation to fix their relative
// address are on a faked, odd instruction address. The instruction can be
// found on the next lower, even address.
//
// All other calls are "normal", i.e. on even addresses.
address NativeMovConstReg::set_data_plain(intptr_t src, CodeBlob *cb) {
  unsigned long x = (unsigned long)src;
  address loc = instruction_address();
  address next_address;

  if (MacroAssembler::is_load_addr_pcrel(loc)) {
    MacroAssembler::patch_target_addr_pcrel(loc, (address)src);
    ICache::invalidate_range(loc, MacroAssembler::load_addr_pcrel_size());
    next_address = next_instruction_address();
  } else if (MacroAssembler::is_load_const_from_toc(loc)) {  // Load constant from TOC.
    MacroAssembler::set_const_in_toc(loc, src, cb);
    next_address = next_instruction_address();
  } else if (MacroAssembler::is_load_const(loc)) {
    // Not mt safe, ok in methods like CodeBuffer::copy_code().
    MacroAssembler::patch_const(loc, x);
    ICache::invalidate_range(loc, MacroAssembler::load_const_size());
    next_address = next_instruction_address();
  }
  // cOops
  else if (MacroAssembler::is_load_narrow_oop(loc)) {
    MacroAssembler::patch_load_narrow_oop(loc, cast_to_oop((void*) x));
    ICache::invalidate_range(loc, MacroAssembler::load_narrow_oop_size());
    next_address = next_instruction_address();
  }
  // compressed klass ptrs
  else if (MacroAssembler::is_load_narrow_klass(loc)) {
    MacroAssembler::patch_load_narrow_klass(loc, (Klass*)x);
    ICache::invalidate_range(loc, MacroAssembler::load_narrow_klass_size());
    next_address = next_instruction_address();
  }
  // cOops
  else if (MacroAssembler::is_compare_immediate_narrow_oop(loc)) {
    MacroAssembler::patch_compare_immediate_narrow_oop(loc, cast_to_oop((void*) x));
    ICache::invalidate_range(loc, MacroAssembler::compare_immediate_narrow_oop_size());
    next_address = next_instruction_address();
  }
  // compressed klass ptrs
  else if (MacroAssembler::is_compare_immediate_narrow_klass(loc)) {
    MacroAssembler::patch_compare_immediate_narrow_klass(loc, (Klass*)x);
    ICache::invalidate_range(loc, MacroAssembler::compare_immediate_narrow_klass_size());
    next_address = next_instruction_address();
  }
  else if (MacroAssembler::is_call_far_patchable_pcrelative_at(loc)) {
    assert(ShortenBranches, "Wait a minute! A pc-relative call w/o ShortenBranches?");
    // This NativeMovConstReg site does not need to be patched. It was
    // patched when it was converted to a call_pcrelative site
    // before. The value of the src argument is not related to the
    // branch target.
    next_address = next_instruction_address();
  }

  else {
    tty->print_cr("WARNING: detected an unrecognized code pattern at loc = %p -> 0x%8.8x %8.8x",
                  loc, *((unsigned int*)loc), *((unsigned int*)(loc+4)));
    next_address = next_instruction_address(); // Failure should be handled in next_instruction_address().
#ifdef LUCY_DBG
    VM_Version::z_SIGSEGV();
#endif
  }

  return next_address;
}

// Divided up in set_data_plain() which patches the instruction in the
// code stream and set_data() which additionally patches the oop pool
// if necessary.
void NativeMovConstReg::set_data(intptr_t data, relocInfo::relocType expected_type) {
  // Also store the value into an oop_Relocation cell, if any.
  CodeBlob *cb = CodeCache::find_blob(instruction_address());
  address next_address = set_data_plain(data, cb);

  // 'RelocIterator' requires an nmethod
  nmethod* nm = cb ? cb->as_nmethod_or_null() : NULL;
  if (nm != NULL) {
    RelocIterator iter(nm, instruction_address(), next_address);
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
    assert(expected_type == relocInfo::none ||
          (expected_type == relocInfo::metadata_type && metadata_addr != NULL) ||
          (expected_type == relocInfo::oop_type && oop_addr != NULL),
          "%s relocation not found", expected_type == relocInfo::oop_type ? "oop" : "metadata");
  }
}

void NativeMovConstReg::set_narrow_oop(intptr_t data) {
  const address start = addr_at(0);
  int           range = 0;
  if (MacroAssembler::is_load_narrow_oop(start)) {
    range = MacroAssembler::patch_load_narrow_oop(start, cast_to_oop <intptr_t> (data));
  } else if (MacroAssembler::is_compare_immediate_narrow_oop(start)) {
    range = MacroAssembler::patch_compare_immediate_narrow_oop(start, cast_to_oop <intptr_t>(data));
  } else {
    fatal("this is not a `NativeMovConstReg::narrow_oop' site");
  }
  ICache::invalidate_range(start, range);
}

// Compressed klass ptrs. patch narrow klass constant.
void NativeMovConstReg::set_narrow_klass(intptr_t data) {
  const address start = addr_at(0);
  int           range = 0;
  if (MacroAssembler::is_load_narrow_klass(start)) {
    range = MacroAssembler::patch_load_narrow_klass(start, (Klass*)data);
  } else if (MacroAssembler::is_compare_immediate_narrow_klass(start)) {
    range = MacroAssembler::patch_compare_immediate_narrow_klass(start, (Klass*)data);
  } else {
    fatal("this is not a `NativeMovConstReg::narrow_klass' site");
  }
  ICache::invalidate_range(start, range);
}

void NativeMovConstReg::set_pcrel_addr(intptr_t newTarget, CompiledMethod *passed_nm /* = NULL */) {
  address next_address;
  address loc = addr_at(0);

  if (MacroAssembler::is_load_addr_pcrel(loc)) {
    address oldTarget = MacroAssembler::get_target_addr_pcrel(loc);
    MacroAssembler::patch_target_addr_pcrel(loc, (address)newTarget);

    ICache::invalidate_range(loc, MacroAssembler::load_addr_pcrel_size());
    next_address = loc + MacroAssembler::load_addr_pcrel_size();
  } else if (MacroAssembler::is_load_const_from_toc_pcrelative(loc) ) {  // Load constant from TOC.
    address oldTarget = MacroAssembler::get_target_addr_pcrel(loc);
    MacroAssembler::patch_target_addr_pcrel(loc, (address)newTarget);

    ICache::invalidate_range(loc, MacroAssembler::load_const_from_toc_size());
    next_address = loc + MacroAssembler::load_const_from_toc_size();
  } else if (MacroAssembler::is_call_far_patchable_pcrelative_at(loc)) {
    assert(ShortenBranches, "Wait a minute! A pc-relative call w/o ShortenBranches?");
    next_address = next_instruction_address();
  } else {
    assert(false, "Not a NativeMovConstReg site for set_pcrel_addr");
    next_address = next_instruction_address(); // Failure should be handled in next_instruction_address().
  }
}

void NativeMovConstReg::set_pcrel_data(intptr_t newData, CompiledMethod *passed_nm /* = NULL */) {
  address  next_address;
  address  loc = addr_at(0);

  if (MacroAssembler::is_load_const_from_toc(loc) ) {  // Load constant from TOC.
    // Offset is +/- 2**32 -> use long.
    long     offset  = MacroAssembler::get_load_const_from_toc_offset(loc);
    address  target  = MacroAssembler::get_target_addr_pcrel(loc);
    intptr_t oldData = *(intptr_t*)target;
    if (oldData != newData) { // Update only if data changes. Prevents cache invalidation.
      *(intptr_t *)(target) = newData;
    }

    // ICache::invalidate_range(target, sizeof(unsigned long));  // No ICache invalidate for CP data.
    next_address = loc + MacroAssembler::load_const_from_toc_size();
  } else if (MacroAssembler::is_call_far_pcrelative(loc)) {
    ((NativeMovConstReg*)loc)->dump(64, "NativeMovConstReg::set_pcrel_data() has a problem: setting data for a pc-relative call?");
#ifdef LUCY_DBG
    VM_Version::z_SIGSEGV();
#else
    assert(false, "Ooooops: setting data for a pc-relative call");
#endif
    next_address = next_instruction_address();
  } else {
    assert(false, "Not a NativeMovConstReg site for set_pcrel_data");
    next_address = next_instruction_address(); // Failure should be handled in next_instruction_address().
  }
}

#ifdef COMPILER1
//--------------------------------
//  N a t i v e M o v R e g M e m
//--------------------------------

void NativeMovRegMem::verify() {
  address l1 = addr_at(0);

  if (!MacroAssembler::is_load_const(l1)) {
    tty->cr();
    tty->print_cr("NativeMovRegMem::verify(): verifying addr " PTR_FORMAT, p2i(l1));
    tty->cr();
    ((NativeMovRegMem*)l1)->dump(64, "NativeMovConstReg::verify()");
    fatal("this is not a `NativeMovRegMem' site");
  }
}
#endif // COMPILER1

//-----------------------
//  N a t i v e J u m p
//-----------------------

void NativeJump::verify() {
  if (NativeJump::is_jump_at(addr_at(0))) return;
  fatal("this is not a `NativeJump' site");
}

// Patch atomically with an illtrap.
void NativeJump::patch_verified_entry(address entry, address verified_entry, address dest) {
  ResourceMark rm;
  int code_size = 2;
  CodeBuffer cb(verified_entry, code_size + 1);
  MacroAssembler* a = new MacroAssembler(&cb);
#ifdef COMPILER2
  assert(dest == SharedRuntime::get_handle_wrong_method_stub(), "expected fixed destination of patch");
#endif
  a->z_illtrap();
  ICache::invalidate_range(verified_entry, code_size);
}

#undef LUCY_DBG

//-------------------------------------
//  N a t i v e G e n e r a l J u m p
//-------------------------------------

#ifndef PRODUCT
void NativeGeneralJump::verify() {
  unsigned long inst;
  Assembler::get_instruction((address)this, &inst);
  assert(MacroAssembler::is_branch_pcrelative_long(inst), "not a general jump instruction");
}
#endif

void NativeGeneralJump::insert_unconditional(address code_pos, address entry) {
  uint64_t instr = BRCL_ZOPC |
                   Assembler::uimm4(Assembler::bcondAlways, 8, 48) |
                   Assembler::simm32(RelAddr::pcrel_off32(entry, code_pos), 16, 48);
  *(uint64_t*) code_pos = (instr << 16); // Must shift into big end, then the brcl will be written to code_pos.
  ICache::invalidate_range(code_pos, instruction_size);
}

void NativeGeneralJump::replace_mt_safe(address instr_addr, address code_buffer) {
  assert(((intptr_t)instr_addr & (BytesPerWord-1)) == 0, "requirement for mt safe patching");
  // Bytes_after_jump cannot change, because we own the Patching_lock.
  assert(Patching_lock->owned_by_self(), "must hold lock to patch instruction");
  intptr_t bytes_after_jump = (*(intptr_t*)instr_addr)  & 0x000000000000ffffL; // 2 bytes after jump.
  intptr_t load_const_bytes = (*(intptr_t*)code_buffer) & 0xffffffffffff0000L;
  *(intptr_t*)instr_addr = load_const_bytes | bytes_after_jump;
  ICache::invalidate_range(instr_addr, 6);
}
