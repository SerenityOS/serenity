/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeCache.hpp"
#include "code/nativeInst.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/thread.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

class NativeNMethodCmpBarrier: public NativeInstruction {
public:
#ifdef _LP64
  enum Intel_specific_constants {
    instruction_code        = 0x81,
    instruction_size        = 8,
    imm_offset              = 4,
    instruction_rex_prefix  = Assembler::REX | Assembler::REX_B,
    instruction_modrm       = 0x7f  // [r15 + offset]
  };
#else
  enum Intel_specific_constants {
    instruction_code        = 0x81,
    instruction_size        = 7,
    imm_offset              = 2,
    instruction_modrm       = 0x3f  // [rdi]
  };
#endif

  address instruction_address() const { return addr_at(0); }
  address immediate_address() const { return addr_at(imm_offset); }

  jint get_immedate() const { return int_at(imm_offset); }
  void set_immediate(jint imm) { set_int_at(imm_offset, imm); }
  void verify() const;
};

#ifdef _LP64
void NativeNMethodCmpBarrier::verify() const {
  if (((uintptr_t) instruction_address()) & 0x7) {
    fatal("Not properly aligned");
  }

  int prefix = ubyte_at(0);
  if (prefix != instruction_rex_prefix) {
    tty->print_cr("Addr: " INTPTR_FORMAT " Prefix: 0x%x", p2i(instruction_address()),
        prefix);
    fatal("not a cmp barrier");
  }

  int inst = ubyte_at(1);
  if (inst != instruction_code) {
    tty->print_cr("Addr: " INTPTR_FORMAT " Code: 0x%x", p2i(instruction_address()),
        inst);
    fatal("not a cmp barrier");
  }

  int modrm = ubyte_at(2);
  if (modrm != instruction_modrm) {
    tty->print_cr("Addr: " INTPTR_FORMAT " mod/rm: 0x%x", p2i(instruction_address()),
        modrm);
    fatal("not a cmp barrier");
  }
}
#else
void NativeNMethodCmpBarrier::verify() const {
  if (((uintptr_t) instruction_address()) & 0x3) {
    fatal("Not properly aligned");
  }

  int inst = ubyte_at(0);
  if (inst != instruction_code) {
    tty->print_cr("Addr: " INTPTR_FORMAT " Code: 0x%x", p2i(instruction_address()),
        inst);
    fatal("not a cmp barrier");
  }

  int modrm = ubyte_at(1);
  if (modrm != instruction_modrm) {
    tty->print_cr("Addr: " INTPTR_FORMAT " mod/rm: 0x%x", p2i(instruction_address()),
        modrm);
    fatal("not a cmp barrier");
  }
}
#endif // _LP64

void BarrierSetNMethod::deoptimize(nmethod* nm, address* return_address_ptr) {
  /*
   * [ callers frame          ]
   * [ callers return address ] <- callers rsp
   * [ callers rbp            ] <- callers rbp
   * [ callers frame slots    ]
   * [ return_address         ] <- return_address_ptr
   * [ cookie ]                 <- used to write the new rsp (callers rsp)
   * [ stub rbp ]
   * [ stub stuff             ]
   */

  address* stub_rbp = return_address_ptr - 2;
  address* callers_rsp = return_address_ptr + nm->frame_size(); /* points to callers return_address now */
  address* callers_rbp = callers_rsp - 1; // 1 to move to the callers return address, 1 more to move to the rbp
  address* cookie = return_address_ptr - 1;

  LogTarget(Trace, nmethod, barrier) out;
  if (out.is_enabled()) {
    JavaThread* jth = JavaThread::current();
    ResourceMark mark;
    log_trace(nmethod, barrier)("deoptimize(nmethod: %p, return_addr: %p, osr: %d, thread: %p(%s), making rsp: %p) -> %p",
                               nm, (address *) return_address_ptr, nm->is_osr_method(), jth,
                               jth->name(), callers_rsp, nm->verified_entry_point());
  }

  assert(nm->frame_size() >= 3, "invariant");
  assert(*cookie == (address) -1, "invariant");

  // Preserve caller rbp.
  *stub_rbp = *callers_rbp;

  // At the cookie address put the callers rsp.
  *cookie = (address) callers_rsp; // should point to the return address

  // In the slot that used to be the callers rbp we put the address that our stub needs to jump to at the end.
  // Overwriting the caller rbp should be okay since our stub rbp has the same value.
  address* jmp_addr_ptr = callers_rbp;
  *jmp_addr_ptr = SharedRuntime::get_handle_wrong_method_stub();
}

// This is the offset of the entry barrier from where the frame is completed.
// If any code changes between the end of the verified entry where the entry
// barrier resides, and the completion of the frame, then
// NativeNMethodCmpBarrier::verify() will immediately complain when it does
// not find the expected native instruction at this offset, which needs updating.
// Note that this offset is invariant of PreserveFramePointer.
static const int entry_barrier_offset = LP64_ONLY(-19) NOT_LP64(-18);

static NativeNMethodCmpBarrier* native_nmethod_barrier(nmethod* nm) {
  address barrier_address = nm->code_begin() + nm->frame_complete_offset() + entry_barrier_offset;
  NativeNMethodCmpBarrier* barrier = reinterpret_cast<NativeNMethodCmpBarrier*>(barrier_address);
  debug_only(barrier->verify());
  return barrier;
}

void BarrierSetNMethod::disarm(nmethod* nm) {
  if (!supports_entry_barrier(nm)) {
    return;
  }

  NativeNMethodCmpBarrier* cmp = native_nmethod_barrier(nm);
  cmp->set_immediate(disarmed_value());
}

bool BarrierSetNMethod::is_armed(nmethod* nm) {
  if (!supports_entry_barrier(nm)) {
    return false;
  }

  NativeNMethodCmpBarrier* cmp = native_nmethod_barrier(nm);
  return (disarmed_value() != cmp->get_immedate());
}
