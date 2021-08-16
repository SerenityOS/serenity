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
#include "asm/codeBuffer.hpp"
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/icache.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"


// Implementation of AbstractAssembler
//
// The AbstractAssembler is generating code into a CodeBuffer. To make code generation faster,
// the assembler keeps a copy of the code buffers boundaries & modifies them when
// emitting bytes rather than using the code buffers accessor functions all the time.
// The code buffer is updated via set_code_end(...) after emitting a whole instruction.

AbstractAssembler::AbstractAssembler(CodeBuffer* code) {
  if (code == NULL)  return;
  CodeSection* cs = code->insts();
  cs->clear_mark();   // new assembler kills old mark
  if (cs->start() == NULL)  {
    vm_exit_out_of_memory(0, OOM_MMAP_ERROR, "CodeCache: no room for %s", code->name());
  }
  _code_section = cs;
  _oop_recorder= code->oop_recorder();
  DEBUG_ONLY( _short_branch_delta = 0; )
}

void AbstractAssembler::set_code_section(CodeSection* cs) {
  assert(cs->outer() == code_section()->outer(), "sanity");
  assert(cs->is_allocated(), "need to pre-allocate this section");
  cs->clear_mark();  // new assembly into this section kills old mark
  _code_section = cs;
}

// Inform CodeBuffer that incoming code and relocation will be for stubs
address AbstractAssembler::start_a_stub(int required_space) {
  CodeBuffer*  cb = code();
  CodeSection* cs = cb->stubs();
  assert(_code_section == cb->insts(), "not in insts?");
  if (cs->maybe_expand_to_ensure_remaining(required_space)
      && cb->blob() == NULL) {
    return NULL;
  }
  set_code_section(cs);
  return pc();
}

// Inform CodeBuffer that incoming code and relocation will be code
// Should not be called if start_a_stub() returned NULL
void AbstractAssembler::end_a_stub() {
  assert(_code_section == code()->stubs(), "not in stubs?");
  set_code_section(code()->insts());
}

// Inform CodeBuffer that incoming code and relocation will be for stubs
address AbstractAssembler::start_a_const(int required_space, int required_align) {
  CodeBuffer*  cb = code();
  CodeSection* cs = cb->consts();
  assert(_code_section == cb->insts() || _code_section == cb->stubs(), "not in insts/stubs?");
  address end = cs->end();
  int pad = -(intptr_t)end & (required_align-1);
  if (cs->maybe_expand_to_ensure_remaining(pad + required_space)) {
    if (cb->blob() == NULL)  return NULL;
    end = cs->end();  // refresh pointer
  }
  if (pad > 0) {
    while (--pad >= 0) { *end++ = 0; }
    cs->set_end(end);
  }
  set_code_section(cs);
  return end;
}

// Inform CodeBuffer that incoming code and relocation will be code
// in section cs (insts or stubs).
void AbstractAssembler::end_a_const(CodeSection* cs) {
  assert(_code_section == code()->consts(), "not in consts?");
  set_code_section(cs);
}

void AbstractAssembler::flush() {
  ICache::invalidate_range(addr_at(0), offset());
}

void AbstractAssembler::bind(Label& L) {
  if (L.is_bound()) {
    // Assembler can bind a label more than once to the same place.
    guarantee(L.loc() == locator(), "attempt to redefine label");
    return;
  }
  L.bind_loc(locator());
  L.patch_instructions((MacroAssembler*)this);
}

void AbstractAssembler::generate_stack_overflow_check(int frame_size_in_bytes) {
  // Each code entry causes one stack bang n pages down the stack where n
  // is configurable by StackShadowPages.  The setting depends on the maximum
  // depth of VM call stack or native before going back into java code,
  // since only java code can raise a stack overflow exception using the
  // stack banging mechanism.  The VM and native code does not detect stack
  // overflow.
  // The code in JavaCalls::call() checks that there is at least n pages
  // available, so all entry code needs to do is bang once for the end of
  // this shadow zone.
  // The entry code may need to bang additional pages if the framesize
  // is greater than a page.

  const int page_size = os::vm_page_size();
  int bang_end = (int)StackOverflow::stack_shadow_zone_size();

  // This is how far the previous frame's stack banging extended.
  const int bang_end_safe = bang_end;

  if (frame_size_in_bytes > page_size) {
    bang_end += frame_size_in_bytes;
  }

  int bang_offset = bang_end_safe;
  while (bang_offset <= bang_end) {
    // Need at least one stack bang at end of shadow zone.
    bang_stack_with_offset(bang_offset);
    bang_offset += page_size;
  }
}

void Label::add_patch_at(CodeBuffer* cb, int branch_loc, const char* file, int line) {
  assert(_loc == -1, "Label is unbound");
  // Don't add patch locations during scratch emit.
  if (cb->insts()->scratch_emit()) { return; }
  if (_patch_index < PatchCacheSize) {
    _patches[_patch_index] = branch_loc;
#ifdef ASSERT
    _lines[_patch_index] = line;
    _files[_patch_index] = file;
#endif
  } else {
    if (_patch_overflow == NULL) {
      _patch_overflow = cb->create_patch_overflow();
    }
    _patch_overflow->push(branch_loc);
  }
  ++_patch_index;
}

void Label::patch_instructions(MacroAssembler* masm) {
  assert(is_bound(), "Label is bound");
  CodeBuffer* cb = masm->code();
  int target_sect = CodeBuffer::locator_sect(loc());
  address target = cb->locator_address(loc());
  while (_patch_index > 0) {
    --_patch_index;
    int branch_loc;
    int line = 0;
    const char* file = NULL;
    if (_patch_index >= PatchCacheSize) {
      branch_loc = _patch_overflow->pop();
    } else {
      branch_loc = _patches[_patch_index];
#ifdef ASSERT
      line = _lines[_patch_index];
      file = _files[_patch_index];
#endif
    }
    int branch_sect = CodeBuffer::locator_sect(branch_loc);
    address branch = cb->locator_address(branch_loc);
    if (branch_sect == CodeBuffer::SECT_CONSTS) {
      // The thing to patch is a constant word.
      *(address*)branch = target;
      continue;
    }

    // Push the target offset into the branch instruction.
    masm->pd_patch_instruction(branch, target, file, line);
  }
}

void AbstractAssembler::block_comment(const char* comment) {
  if (sect() == CodeBuffer::SECT_INSTS) {
    code_section()->outer()->block_comment(offset(), comment);
  }
}

const char* AbstractAssembler::code_string(const char* str) {
  if (sect() == CodeBuffer::SECT_INSTS || sect() == CodeBuffer::SECT_STUBS) {
    return code_section()->outer()->code_string(str);
  }
  return NULL;
}

bool MacroAssembler::uses_implicit_null_check(void* address) {
  // Exception handler checks the nmethod's implicit null checks table
  // only when this method returns false.
  uintptr_t addr = reinterpret_cast<uintptr_t>(address);
  uintptr_t page_size = (uintptr_t)os::vm_page_size();
#ifdef _LP64
  if (UseCompressedOops && CompressedOops::base() != NULL) {
    // A SEGV can legitimately happen in C2 code at address
    // (heap_base + offset) if  Matcher::narrow_oop_use_complex_address
    // is configured to allow narrow oops field loads to be implicitly
    // null checked
    uintptr_t start = (uintptr_t)CompressedOops::base();
    uintptr_t end = start + page_size;
    if (addr >= start && addr < end) {
      return true;
    }
  }
#endif
  return addr < page_size;
}

bool MacroAssembler::needs_explicit_null_check(intptr_t offset) {
  // The offset -1 is used (hardcoded) in a number of places in C1 and MacroAssembler
  // to indicate an unknown offset. For example, TemplateTable::pop_and_check_object(Register r)
  // calls MacroAssembler::null_check(Register reg, int offset = -1) which gets here
  // with -1. Another example is GraphBuilder::access_field(...) which uses -1 as placeholder
  // for offsets to be patched in later. The -1 there means the offset is not yet known
  // and may lie outside of the zero-trapping page, and thus we need to ensure we're forcing
  // an explicit null check for -1.

  // Check if offset is outside of [0, os::vm_page_size()]
  return offset < 0 || offset >= os::vm_page_size();
}
