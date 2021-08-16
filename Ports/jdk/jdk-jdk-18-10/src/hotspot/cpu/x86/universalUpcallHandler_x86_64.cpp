/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "code/codeBlob.hpp"
#include "code/codeBlob.hpp"
#include "code/vmreg.inline.hpp"
#include "compiler/disassembler.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "prims/universalUpcallHandler.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"

#define __ _masm->

// 1. Create buffer according to layout
// 2. Load registers & stack args into buffer
// 3. Call upcall helper with upcall handler instance & buffer pointer (C++ ABI)
// 4. Load return value from buffer into foreign ABI registers
// 5. Return
address ProgrammableUpcallHandler::generate_upcall_stub(jobject rec, jobject jabi, jobject jlayout) {
  ResourceMark rm;
  const ABIDescriptor abi = ForeignGlobals::parse_abi_descriptor(jabi);
  const BufferLayout layout = ForeignGlobals::parse_buffer_layout(jlayout);

  CodeBuffer buffer("upcall_stub", 1024, upcall_stub_size);

  MacroAssembler* _masm = new MacroAssembler(&buffer);
  int stack_alignment_C = 16; // bytes
  int register_size = sizeof(uintptr_t);
  int buffer_alignment = xmm_reg_size;

  // stub code
  __ enter();

  // save pointer to JNI receiver handle into constant segment
  Address rec_adr = __ as_Address(InternalAddress(__ address_constant((address)rec)));

  __ subptr(rsp, (int) align_up(layout.buffer_size, buffer_alignment));

  Register used[] = { c_rarg0, c_rarg1, rax, rbx, rdi, rsi, r12, r13, r14, r15 };
  GrowableArray<Register> preserved;
  // TODO need to preserve anything killed by the upcall that is non-volatile, needs XMM regs as well, probably
  for (size_t i = 0; i < sizeof(used)/sizeof(Register); i++) {
    Register reg = used[i];
    if (!abi.is_volatile_reg(reg)) {
      preserved.push(reg);
    }
  }

  int preserved_size = align_up(preserved.length() * register_size, stack_alignment_C); // includes register alignment
  int buffer_offset = preserved_size; // offset from rsp

  __ subptr(rsp, preserved_size);
  for (int i = 0; i < preserved.length(); i++) {
    __ movptr(Address(rsp, i * register_size), preserved.at(i));
  }

  for (int i = 0; i < abi._integer_argument_registers.length(); i++) {
    size_t offs = buffer_offset + layout.arguments_integer + i * sizeof(uintptr_t);
    __ movptr(Address(rsp, (int)offs), abi._integer_argument_registers.at(i));
  }

  for (int i = 0; i < abi._vector_argument_registers.length(); i++) {
    XMMRegister reg = abi._vector_argument_registers.at(i);
    size_t offs = buffer_offset + layout.arguments_vector + i * xmm_reg_size;
    __ movdqu(Address(rsp, (int)offs), reg);
  }

  // Capture prev stack pointer (stack arguments base)
#ifndef _WIN64
  __ lea(rax, Address(rbp, 16)); // skip frame+return address
#else
  __ lea(rax, Address(rbp, 16 + 32)); // also skip shadow space
#endif
  __ movptr(Address(rsp, buffer_offset + (int) layout.stack_args), rax);
#ifndef PRODUCT
  __ movptr(Address(rsp, buffer_offset + (int) layout.stack_args_bytes), -1); // unknown
#endif

  // Call upcall helper

  __ movptr(c_rarg0, rec_adr);
  __ lea(c_rarg1, Address(rsp, buffer_offset));

#ifdef _WIN64
  __ block_comment("allocate shadow space for argument register spill");
  __ subptr(rsp, 32);
#endif

  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, ProgrammableUpcallHandler::attach_thread_and_do_upcall)));

#ifdef _WIN64
  __ block_comment("pop shadow space");
  __ addptr(rsp, 32);
#endif

  for (int i = 0; i < abi._integer_return_registers.length(); i++) {
    size_t offs = buffer_offset + layout.returns_integer + i * sizeof(uintptr_t);
    __ movptr(abi._integer_return_registers.at(i), Address(rsp, (int)offs));
  }

  for (int i = 0; i < abi._vector_return_registers.length(); i++) {
    XMMRegister reg = abi._vector_return_registers.at(i);
    size_t offs = buffer_offset + layout.returns_vector + i * xmm_reg_size;
    __ movdqu(reg, Address(rsp, (int)offs));
  }

  for (size_t i = abi._X87_return_registers_noof; i > 0 ; i--) {
      ssize_t offs = buffer_offset + layout.returns_x87 + (i - 1) * (sizeof(long double));
      __ fld_x (Address(rsp, (int)offs));
  }

  // Restore preserved registers
  for (int i = 0; i < preserved.length(); i++) {
    __ movptr(preserved.at(i), Address(rsp, i * register_size));
  }

  __ leave();
  __ ret(0);

  _masm->flush();

  BufferBlob* blob = BufferBlob::create("upcall_stub", &buffer);

  return blob->code_begin();
}

struct ArgMove {
  BasicType bt;
  VMRegPair from;
  VMRegPair to;

  bool is_identity() const {
      return from.first() == to.first() && from.second() == to.second();
  }
};

static GrowableArray<ArgMove> compute_argument_shuffle(Method* entry, int& out_arg_size_bytes, const CallRegs& conv, BasicType& ret_type) {
  assert(entry->is_static(), "");

  // Fill in the signature array, for the calling-convention call.
  const int total_out_args = entry->size_of_parameters();
  assert(total_out_args > 0, "receiver arg ");

  BasicType* out_sig_bt = NEW_RESOURCE_ARRAY(BasicType, total_out_args);
  VMRegPair* out_regs = NEW_RESOURCE_ARRAY(VMRegPair, total_out_args);

  {
    int i = 0;
    SignatureStream ss(entry->signature());
    for (; !ss.at_return_type(); ss.next()) {
      out_sig_bt[i++] = ss.type();  // Collect remaining bits of signature
      if (ss.type() == T_LONG || ss.type() == T_DOUBLE)
        out_sig_bt[i++] = T_VOID;   // Longs & doubles take 2 Java slots
    }
    assert(i == total_out_args, "");
    ret_type = ss.type();
  }

  int out_arg_slots = SharedRuntime::java_calling_convention(out_sig_bt, out_regs, total_out_args);

  const int total_in_args = total_out_args - 1; // skip receiver
  BasicType* in_sig_bt  = NEW_RESOURCE_ARRAY(BasicType, total_in_args);
  VMRegPair* in_regs    = NEW_RESOURCE_ARRAY(VMRegPair, total_in_args);

  for (int i = 0; i < total_in_args ; i++ ) {
    in_sig_bt[i] = out_sig_bt[i+1]; // skip receiver
  }

  // Now figure out where the args must be stored and how much stack space they require.
  conv.calling_convention(in_sig_bt, in_regs, total_in_args);

  GrowableArray<int> arg_order(2 * total_in_args);

  VMRegPair tmp_vmreg;
  tmp_vmreg.set2(rbx->as_VMReg());

  // Compute a valid move order, using tmp_vmreg to break any cycles
  SharedRuntime::compute_move_order(in_sig_bt,
                                    total_in_args, in_regs,
                                    total_out_args, out_regs,
                                    arg_order,
                                    tmp_vmreg);

  GrowableArray<ArgMove> arg_order_vmreg(total_in_args); // conservative

#ifdef ASSERT
  bool reg_destroyed[RegisterImpl::number_of_registers];
  bool freg_destroyed[XMMRegisterImpl::number_of_registers];
  for ( int r = 0 ; r < RegisterImpl::number_of_registers ; r++ ) {
    reg_destroyed[r] = false;
  }
  for ( int f = 0 ; f < XMMRegisterImpl::number_of_registers ; f++ ) {
    freg_destroyed[f] = false;
  }
#endif // ASSERT

  for (int i = 0; i < arg_order.length(); i += 2) {
    int in_arg  = arg_order.at(i);
    int out_arg = arg_order.at(i + 1);

    assert(in_arg != -1 || out_arg != -1, "");
    BasicType arg_bt = (in_arg != -1 ? in_sig_bt[in_arg] : out_sig_bt[out_arg]);
    switch (arg_bt) {
      case T_BOOLEAN:
      case T_BYTE:
      case T_SHORT:
      case T_CHAR:
      case T_INT:
      case T_FLOAT:
        break; // process

      case T_LONG:
      case T_DOUBLE:
        assert(in_arg  == -1 || (in_arg  + 1 < total_in_args  &&  in_sig_bt[in_arg  + 1] == T_VOID), "bad arg list: %d", in_arg);
        assert(out_arg == -1 || (out_arg + 1 < total_out_args && out_sig_bt[out_arg + 1] == T_VOID), "bad arg list: %d", out_arg);
        break; // process

      case T_VOID:
        continue; // skip

      default:
        fatal("found in upcall args: %s", type2name(arg_bt));
    }

    ArgMove move;
    move.bt   = arg_bt;
    move.from = (in_arg != -1 ? in_regs[in_arg] : tmp_vmreg);
    move.to   = (out_arg != -1 ? out_regs[out_arg] : tmp_vmreg);

    if(move.is_identity()) {
      continue; // useless move
    }

#ifdef ASSERT
    if (in_arg != -1) {
      if (in_regs[in_arg].first()->is_Register()) {
        assert(!reg_destroyed[in_regs[in_arg].first()->as_Register()->encoding()], "destroyed reg!");
      } else if (in_regs[in_arg].first()->is_XMMRegister()) {
        assert(!freg_destroyed[in_regs[in_arg].first()->as_XMMRegister()->encoding()], "destroyed reg!");
      }
    }
    if (out_arg != -1) {
      if (out_regs[out_arg].first()->is_Register()) {
        reg_destroyed[out_regs[out_arg].first()->as_Register()->encoding()] = true;
      } else if (out_regs[out_arg].first()->is_XMMRegister()) {
        freg_destroyed[out_regs[out_arg].first()->as_XMMRegister()->encoding()] = true;
      }
    }
#endif /* ASSERT */

    arg_order_vmreg.push(move);
  }

  int stack_slots = SharedRuntime::out_preserve_stack_slots() + out_arg_slots;
  out_arg_size_bytes = align_up(stack_slots * VMRegImpl::stack_slot_size, StackAlignmentInBytes);

  return arg_order_vmreg;
}

static const char* null_safe_string(const char* str) {
  return str == nullptr ? "NULL" : str;
}

#ifdef ASSERT
static void print_arg_moves(const GrowableArray<ArgMove>& arg_moves, Method* entry) {
  LogTarget(Trace, foreign) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print_cr("Argument shuffle for %s {", entry->name_and_sig_as_C_string());
    for (int i = 0; i < arg_moves.length(); i++) {
      ArgMove arg_mv = arg_moves.at(i);
      BasicType arg_bt     = arg_mv.bt;
      VMRegPair from_vmreg = arg_mv.from;
      VMRegPair to_vmreg   = arg_mv.to;

      ls.print("Move a %s from (", null_safe_string(type2name(arg_bt)));
      from_vmreg.first()->print_on(&ls);
      ls.print(",");
      from_vmreg.second()->print_on(&ls);
      ls.print(") to ");
      to_vmreg.first()->print_on(&ls);
      ls.print(",");
      to_vmreg.second()->print_on(&ls);
      ls.print_cr(")");
    }
    ls.print_cr("}");
  }
}
#endif

static void save_native_arguments(MacroAssembler* _masm, const CallRegs& conv, int arg_save_area_offset) {
  __ block_comment("{ save_native_args ");
  int store_offset = arg_save_area_offset;
  for (int i = 0; i < conv._args_length; i++) {
    VMReg reg = conv._arg_regs[i];
    if (reg->is_Register()) {
      __ movptr(Address(rsp, store_offset), reg->as_Register());
      store_offset += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      __ movdqu(Address(rsp, store_offset), reg->as_XMMRegister());
      store_offset += 16;
    }
    // do nothing for stack
  }
  __ block_comment("} save_native_args ");
}

static void restore_native_arguments(MacroAssembler* _masm, const CallRegs& conv, int arg_save_area_offset) {
  __ block_comment("{ restore_native_args ");
  int load_offset = arg_save_area_offset;
  for (int i = 0; i < conv._args_length; i++) {
    VMReg reg = conv._arg_regs[i];
    if (reg->is_Register()) {
      __ movptr(reg->as_Register(), Address(rsp, load_offset));
      load_offset += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      __ movdqu(reg->as_XMMRegister(), Address(rsp, load_offset));
      load_offset += 16;
    }
    // do nothing for stack
  }
  __ block_comment("} restore_native_args ");
}

static bool is_valid_XMM(XMMRegister reg) {
  return reg->is_valid() && (UseAVX >= 3 || (reg->encoding() < 16)); // why is this not covered by is_valid()?
}

// for callee saved regs, according to the caller's ABI
static int compute_reg_save_area_size(const ABIDescriptor& abi) {
  int size = 0;
  for (Register reg = as_Register(0); reg->is_valid(); reg = reg->successor()) {
    if (reg == rbp || reg == rsp) continue; // saved/restored by prologue/epilogue
    if (!abi.is_volatile_reg(reg)) {
      size += 8; // bytes
    }
  }

  for (XMMRegister reg = as_XMMRegister(0); is_valid_XMM(reg); reg = reg->successor()) {
    if (!abi.is_volatile_reg(reg)) {
      if (UseAVX >= 3) {
        size += 64; // bytes
      } else if (UseAVX >= 1) {
        size += 32;
      } else {
        size += 16;
      }
    }
  }

#ifndef _WIN64
  // for mxcsr
  size += 8;
#endif

  return size;
}

static int compute_arg_save_area_size(const CallRegs& conv) {
  int result_size = 0;
  for (int i = 0; i < conv._args_length; i++) {
    VMReg reg = conv._arg_regs[i];
    if (reg->is_Register()) {
      result_size += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      result_size += 16;
    }
    // do nothing for stack
  }
  return result_size;
}

static int compute_res_save_area_size(const CallRegs& conv) {
  int result_size = 0;
  for (int i = 0; i < conv._rets_length; i++) {
    VMReg reg = conv._ret_regs[i];
    if (reg->is_Register()) {
      result_size += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      result_size += 16;
    } else {
      ShouldNotReachHere(); // unhandled type
    }
  }
  return result_size;
}

static void save_java_result(MacroAssembler* _masm, const CallRegs& conv, int res_save_area_offset) {
  int offset = res_save_area_offset;
  __ block_comment("{ save java result ");
  for (int i = 0; i < conv._rets_length; i++) {
    VMReg reg = conv._ret_regs[i];
    if (reg->is_Register()) {
      __ movptr(Address(rsp, offset), reg->as_Register());
      offset += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      __ movdqu(Address(rsp, offset), reg->as_XMMRegister());
      offset += 16;
    } else {
      ShouldNotReachHere(); // unhandled type
    }
  }
  __ block_comment("} save java result ");
}

static void restore_java_result(MacroAssembler* _masm, const CallRegs& conv, int res_save_area_offset) {
  int offset = res_save_area_offset;
  __ block_comment("{ restore java result ");
  for (int i = 0; i < conv._rets_length; i++) {
    VMReg reg = conv._ret_regs[i];
    if (reg->is_Register()) {
      __ movptr(reg->as_Register(), Address(rsp, offset));
      offset += 8;
    } else if (reg->is_XMMRegister()) {
      // Java API doesn't support vector args
      __ movdqu(reg->as_XMMRegister(), Address(rsp, offset));
      offset += 16;
    } else {
      ShouldNotReachHere(); // unhandled type
    }
  }
  __ block_comment("} restore java result ");
}

constexpr int MXCSR_MASK = 0xFFC0;  // Mask out any pending exceptions

static void preserve_callee_saved_registers(MacroAssembler* _masm, const ABIDescriptor& abi, int reg_save_area_offset) {
  // 1. iterate all registers in the architecture
  //     - check if they are volatile or not for the given abi
  //     - if NOT, we need to save it here
  // 2. save mxcsr on non-windows platforms

  int offset = reg_save_area_offset;

  __ block_comment("{ preserve_callee_saved_regs ");
  for (Register reg = as_Register(0); reg->is_valid(); reg = reg->successor()) {
    if (reg == rbp || reg == rsp) continue; // saved/restored by prologue/epilogue
    if (!abi.is_volatile_reg(reg)) {
      __ movptr(Address(rsp, offset), reg);
      offset += 8;
    }
  }

  for (XMMRegister reg = as_XMMRegister(0); is_valid_XMM(reg); reg = reg->successor()) {
    if (!abi.is_volatile_reg(reg)) {
      if (UseAVX >= 3) {
        __ evmovdqul(Address(rsp, offset), reg, Assembler::AVX_512bit);
        offset += 64;
      } else if (UseAVX >= 1) {
        __ vmovdqu(Address(rsp, offset), reg);
        offset += 32;
      } else {
        __ movdqu(Address(rsp, offset), reg);
        offset += 16;
      }
    }
  }

#ifndef _WIN64
  {
    const Address mxcsr_save(rsp, offset);
    Label skip_ldmx;
    __ stmxcsr(mxcsr_save);
    __ movl(rax, mxcsr_save);
    __ andl(rax, MXCSR_MASK);    // Only check control and mask bits
    ExternalAddress mxcsr_std(StubRoutines::x86::addr_mxcsr_std());
    __ cmp32(rax, mxcsr_std);
    __ jcc(Assembler::equal, skip_ldmx);
    __ ldmxcsr(mxcsr_std);
    __ bind(skip_ldmx);
  }
#endif

  __ block_comment("} preserve_callee_saved_regs ");
}

static void restore_callee_saved_registers(MacroAssembler* _masm, const ABIDescriptor& abi, int reg_save_area_offset) {
  // 1. iterate all registers in the architecture
  //     - check if they are volatile or not for the given abi
  //     - if NOT, we need to restore it here
  // 2. restore mxcsr on non-windows platforms

  int offset = reg_save_area_offset;

  __ block_comment("{ restore_callee_saved_regs ");
  for (Register reg = as_Register(0); reg->is_valid(); reg = reg->successor()) {
    if (reg == rbp || reg == rsp) continue; // saved/restored by prologue/epilogue
    if (!abi.is_volatile_reg(reg)) {
      __ movptr(reg, Address(rsp, offset));
      offset += 8;
    }
  }

  for (XMMRegister reg = as_XMMRegister(0); is_valid_XMM(reg); reg = reg->successor()) {
    if (!abi.is_volatile_reg(reg)) {
      if (UseAVX >= 3) {
        __ evmovdqul(reg, Address(rsp, offset), Assembler::AVX_512bit);
        offset += 64;
      } else if (UseAVX >= 1) {
        __ vmovdqu(reg, Address(rsp, offset));
        offset += 32;
      } else {
        __ movdqu(reg, Address(rsp, offset));
        offset += 16;
      }
    }
  }

#ifndef _WIN64
  const Address mxcsr_save(rsp, offset);
  __ ldmxcsr(mxcsr_save);
#endif

  __ block_comment("} restore_callee_saved_regs ");
}

static void shuffle_arguments(MacroAssembler* _masm, const GrowableArray<ArgMove>& arg_moves) {
  for (int i = 0; i < arg_moves.length(); i++) {
    ArgMove arg_mv = arg_moves.at(i);
    BasicType arg_bt     = arg_mv.bt;
    VMRegPair from_vmreg = arg_mv.from;
    VMRegPair to_vmreg   = arg_mv.to;

    assert(
      !((from_vmreg.first()->is_Register() && to_vmreg.first()->is_XMMRegister())
      || (from_vmreg.first()->is_XMMRegister() && to_vmreg.first()->is_Register())),
       "move between gp and fp reg not supported");

    __ block_comment(err_msg("bt=%s", null_safe_string(type2name(arg_bt))));
    switch (arg_bt) {
      case T_BOOLEAN:
      case T_BYTE:
      case T_SHORT:
      case T_CHAR:
      case T_INT:
       __ move32_64(from_vmreg, to_vmreg);
       break;

      case T_FLOAT:
        __ float_move(from_vmreg, to_vmreg);
        break;

      case T_DOUBLE:
        __ double_move(from_vmreg, to_vmreg);
        break;

      case T_LONG :
        __ long_move(from_vmreg, to_vmreg);
        break;

      default:
        fatal("found in upcall args: %s", type2name(arg_bt));
    }
  }
}

// Register is a class, but it would be assigned numerical value.
// "0" is assigned for rax and for xmm0. Thus we need to ignore -Wnonnull.
PRAGMA_DIAG_PUSH
PRAGMA_NONNULL_IGNORED
address ProgrammableUpcallHandler::generate_optimized_upcall_stub(jobject receiver, Method* entry, jobject jabi, jobject jconv) {
  ResourceMark rm;
  const ABIDescriptor abi = ForeignGlobals::parse_abi_descriptor(jabi);
  const CallRegs conv = ForeignGlobals::parse_call_regs(jconv);
  assert(conv._rets_length <= 1, "no multi reg returns");
  CodeBuffer buffer("upcall_stub_linkToNative", /* code_size = */ 2048, /* locs_size = */ 1024);

  int register_size = sizeof(uintptr_t);
  int buffer_alignment = xmm_reg_size;

  int out_arg_area = -1;
  BasicType ret_type;
  GrowableArray<ArgMove> arg_moves = compute_argument_shuffle(entry, out_arg_area, conv, ret_type);
  assert(out_arg_area != -1, "Should have been set");
  DEBUG_ONLY(print_arg_moves(arg_moves, entry);)

  // out_arg_area (for stack arguments) doubles as shadow space for native calls.
  // make sure it is big enough.
  if (out_arg_area < frame::arg_reg_save_area_bytes) {
    out_arg_area = frame::arg_reg_save_area_bytes;
  }

  int reg_save_area_size = compute_reg_save_area_size(abi);
  int arg_save_area_size = compute_arg_save_area_size(conv);
  int res_save_area_size = compute_res_save_area_size(conv);
  // To spill receiver during deopt
  int deopt_spill_size = 1 * BytesPerWord;

  int shuffle_area_offset    = 0;
  int deopt_spill_offset     = shuffle_area_offset    + out_arg_area;
  int res_save_area_offset   = deopt_spill_offset     + deopt_spill_size;
  int arg_save_area_offset   = res_save_area_offset   + res_save_area_size;
  int reg_save_area_offset   = arg_save_area_offset   + arg_save_area_size;
  int frame_data_offset      = reg_save_area_offset   + reg_save_area_size;
  int frame_bottom_offset    = frame_data_offset      + sizeof(OptimizedEntryBlob::FrameData);

  int frame_size = frame_bottom_offset;
  frame_size = align_up(frame_size, StackAlignmentInBytes);

  // Ok The space we have allocated will look like:
  //
  //
  // FP-> |                     |
  //      |---------------------| = frame_bottom_offset = frame_size
  //      |                     |
  //      | FrameData           |
  //      |---------------------| = frame_data_offset
  //      |                     |
  //      | reg_save_area       |
  //      |---------------------| = reg_save_are_offset
  //      |                     |
  //      | arg_save_area       |
  //      |---------------------| = arg_save_are_offset
  //      |                     |
  //      | res_save_area       |
  //      |---------------------| = res_save_are_offset
  //      |                     |
  //      | deopt_spill         |
  //      |---------------------| = deopt_spill_offset
  //      |                     |
  // SP-> | out_arg_area        |   needs to be at end for shadow space
  //
  //

  //////////////////////////////////////////////////////////////////////////////

  MacroAssembler* _masm = new MacroAssembler(&buffer);
  address start = __ pc();
  __ enter(); // set up frame
  if ((abi._stack_alignment_bytes % 16) != 0) {
    // stack alignment of caller is not a multiple of 16
    __ andptr(rsp, -StackAlignmentInBytes); // align stack
  }
  // allocate frame (frame_size is also aligned, so stack is still aligned)
  __ subptr(rsp, frame_size);

  // we have to always spill args since we need to do a call to get the thread
  // (and maybe attach it).
  save_native_arguments(_masm, conv, arg_save_area_offset);

  preserve_callee_saved_registers(_masm, abi, reg_save_area_offset);

  __ block_comment("{ on_entry");
  __ vzeroupper();
  __ lea(c_rarg0, Address(rsp, frame_data_offset));
  // stack already aligned
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, ProgrammableUpcallHandler::on_entry)));
  __ movptr(r15_thread, rax);
  __ reinit_heapbase();
  __ block_comment("} on_entry");

  __ block_comment("{ argument shuffle");
  // TODO merge these somehow
  restore_native_arguments(_masm, conv, arg_save_area_offset);
  shuffle_arguments(_masm, arg_moves);
  __ block_comment("} argument shuffle");

  __ block_comment("{ receiver ");
  __ movptr(rscratch1, (intptr_t)receiver);
  __ resolve_jobject(rscratch1, r15_thread, rscratch2);
  __ movptr(j_rarg0, rscratch1);
  __ block_comment("} receiver ");

  __ mov_metadata(rbx, entry);
  __ movptr(Address(r15_thread, JavaThread::callee_target_offset()), rbx); // just in case callee is deoptimized

  __ call(Address(rbx, Method::from_compiled_offset()));

  save_java_result(_masm, conv, res_save_area_offset);

  __ block_comment("{ on_exit");
  __ vzeroupper();
  __ lea(c_rarg0, Address(rsp, frame_data_offset));
  // stack already aligned
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, ProgrammableUpcallHandler::on_exit)));
  __ reinit_heapbase();
  __ block_comment("} on_exit");

  restore_callee_saved_registers(_masm, abi, reg_save_area_offset);

  restore_java_result(_masm, conv, res_save_area_offset);

  // return value shuffle
#ifdef ASSERT
  if (conv._rets_length == 1) { // 0 or 1
    VMReg j_expected_result_reg;
    switch (ret_type) {
      case T_BOOLEAN:
      case T_BYTE:
      case T_SHORT:
      case T_CHAR:
      case T_INT:
      case T_LONG:
       j_expected_result_reg = rax->as_VMReg();
       break;
      case T_FLOAT:
      case T_DOUBLE:
        j_expected_result_reg = xmm0->as_VMReg();
        break;
      default:
        fatal("unexpected return type: %s", type2name(ret_type));
    }
    // No need to move for now, since CallArranger can pick a return type
    // that goes in the same reg for both CCs. But, at least assert they are the same
    assert(conv._ret_regs[0] == j_expected_result_reg,
     "unexpected result register: %s != %s", conv._ret_regs[0]->name(), j_expected_result_reg->name());
  }
#endif

  __ leave();
  __ ret(0);

  //////////////////////////////////////////////////////////////////////////////

  __ block_comment("{ exception handler");

  intptr_t exception_handler_offset = __ pc() - start;

  // TODO: this is always the same, can we bypass and call handle_uncaught_exception directly?

  // native caller has no idea how to handle exceptions
  // we just crash here. Up to callee to catch exceptions.
  __ verify_oop(rax);
  __ vzeroupper();
  __ mov(c_rarg0, rax);
  __ andptr(rsp, -StackAlignmentInBytes); // align stack as required by ABI
  __ subptr(rsp, frame::arg_reg_save_area_bytes); // windows (not really needed)
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, ProgrammableUpcallHandler::handle_uncaught_exception)));
  __ should_not_reach_here();

  __ block_comment("} exception handler");

  _masm->flush();


#ifndef PRODUCT
  stringStream ss;
  ss.print("optimized_upcall_stub_%s", entry->signature()->as_C_string());
  const char* name = _masm->code_string(ss.as_string());
#else // PRODUCT
  const char* name = "optimized_upcall_stub";
#endif // PRODUCT

  OptimizedEntryBlob* blob = OptimizedEntryBlob::create(name, &buffer, exception_handler_offset, receiver, in_ByteSize(frame_data_offset));

  if (TraceOptimizedUpcallStubs) {
    blob->print_on(tty);
    Disassembler::decode(blob, tty);
  }

  return blob->code_begin();
}
PRAGMA_DIAG_POP

bool ProgrammableUpcallHandler::supports_optimized_upcalls() {
  return true;
}
