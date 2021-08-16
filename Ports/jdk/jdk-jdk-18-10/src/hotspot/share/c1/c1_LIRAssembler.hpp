/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_LIRASSEMBLER_HPP
#define SHARE_C1_C1_LIRASSEMBLER_HPP

#include "c1/c1_CodeStubs.hpp"
#include "ci/ciMethodData.hpp"
#include "oops/methodData.hpp"
#include "utilities/macros.hpp"

class Compilation;
class ScopeValue;
class BarrierSet;

class LIR_Assembler: public CompilationResourceObj {
 private:
  C1_MacroAssembler* _masm;
  CodeStubList*      _slow_case_stubs;
  BarrierSet*        _bs;

  Compilation*       _compilation;
  FrameMap*          _frame_map;
  BlockBegin*        _current_block;

  Instruction*       _pending_non_safepoint;
  int                _pending_non_safepoint_offset;

  Label              _unwind_handler_entry;

#ifdef ASSERT
  BlockList          _branch_target_blocks;
  void check_no_unbound_labels();
#endif

  FrameMap* frame_map() const { return _frame_map; }

  void set_current_block(BlockBegin* b) { _current_block = b; }
  BlockBegin* current_block() const { return _current_block; }

  // non-safepoint debug info management
  void flush_debug_info(int before_pc_offset) {
    if (_pending_non_safepoint != NULL) {
      if (_pending_non_safepoint_offset < before_pc_offset)
        record_non_safepoint_debug_info();
      _pending_non_safepoint = NULL;
    }
  }
  void process_debug_info(LIR_Op* op);
  void record_non_safepoint_debug_info();

  // unified bailout support
  void bailout(const char* msg) const { compilation()->bailout(msg); }
  bool bailed_out() const                        { return compilation()->bailed_out(); }

  // code emission patterns and accessors
  void check_codespace();
  bool needs_icache(ciMethod* method) const;

  // returns offset of icache check
  int check_icache();

  bool needs_clinit_barrier_on_entry(ciMethod* method) const;
  void clinit_barrier(ciMethod* method);

  void jobject2reg(jobject o, Register reg);
  void jobject2reg_with_patching(Register reg, CodeEmitInfo* info);

  void metadata2reg(Metadata* o, Register reg);
  void klass2reg_with_patching(Register reg, CodeEmitInfo* info);

  void emit_stubs(CodeStubList* stub_list);

  // addresses
  Address as_Address(LIR_Address* addr);
  Address as_Address_lo(LIR_Address* addr);
  Address as_Address_hi(LIR_Address* addr);

  // debug information
  void add_call_info(int pc_offset, CodeEmitInfo* cinfo);
  void add_debug_info_for_branch(CodeEmitInfo* info);
  void add_debug_info_for_div0(int pc_offset, CodeEmitInfo* cinfo);
  void add_debug_info_for_div0_here(CodeEmitInfo* info);
  ImplicitNullCheckStub* add_debug_info_for_null_check(int pc_offset, CodeEmitInfo* cinfo);
  ImplicitNullCheckStub* add_debug_info_for_null_check_here(CodeEmitInfo* info);

  void breakpoint();
  void push(LIR_Opr opr);
  void pop(LIR_Opr opr);

  // patching
  void append_patching_stub(PatchingStub* stub);
  void patching_epilog(PatchingStub* patch, LIR_PatchCode patch_code, Register obj, CodeEmitInfo* info);

  void comp_op(LIR_Condition condition, LIR_Opr src, LIR_Opr result, LIR_Op2* op);

  PatchingStub::PatchID patching_id(CodeEmitInfo* info);

 public:
  LIR_Assembler(Compilation* c);
  ~LIR_Assembler();
  C1_MacroAssembler* masm() const                { return _masm; }
  Compilation* compilation() const               { return _compilation; }
  ciMethod* method() const                       { return compilation()->method(); }

  CodeOffsets* offsets() const                   { return _compilation->offsets(); }
  int code_offset() const;
  address pc() const;

  int  initial_frame_size_in_bytes() const;
  int  bang_size_in_bytes() const;

  // test for constants which can be encoded directly in instructions
  static bool is_small_constant(LIR_Opr opr);

  static LIR_Opr receiverOpr();
  static LIR_Opr osrBufferPointer();

  // stubs
  void emit_slow_case_stubs();
  void emit_static_call_stub();
  void append_code_stub(CodeStub* op);
  void add_call_info_here(CodeEmitInfo* info)                              { add_call_info(code_offset(), info); }

  // code patterns
  int  emit_exception_handler();
  int  emit_unwind_handler();
  void emit_exception_entries(ExceptionInfoList* info_list);
  int  emit_deopt_handler();

  void emit_code(BlockList* hir);
  void emit_block(BlockBegin* block);
  void emit_lir_list(LIR_List* list);

  // any last minute peephole optimizations are performed here.  In
  // particular sparc uses this for delay slot filling.
  void peephole(LIR_List* list);

  void return_op(LIR_Opr result, C1SafepointPollStub* code_stub);

  // returns offset of poll instruction
  int safepoint_poll(LIR_Opr result, CodeEmitInfo* info);

  void const2reg  (LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info);
  void const2stack(LIR_Opr src, LIR_Opr dest);
  void const2mem  (LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info, bool wide);
  void reg2stack  (LIR_Opr src, LIR_Opr dest, BasicType type, bool pop_fpu_stack);
  void reg2reg    (LIR_Opr src, LIR_Opr dest);
  void reg2mem    (LIR_Opr src, LIR_Opr dest, BasicType type,
                   LIR_PatchCode patch_code, CodeEmitInfo* info,
                   bool pop_fpu_stack, bool wide);
  void stack2reg  (LIR_Opr src, LIR_Opr dest, BasicType type);
  void stack2stack(LIR_Opr src, LIR_Opr dest, BasicType type);
  void mem2reg    (LIR_Opr src, LIR_Opr dest, BasicType type,
                   LIR_PatchCode patch_code,
                   CodeEmitInfo* info, bool wide);

  void shift_op(LIR_Code code, LIR_Opr left, LIR_Opr count, LIR_Opr dest, LIR_Opr tmp);
  void shift_op(LIR_Code code, LIR_Opr left, jint  count, LIR_Opr dest);

  void move_regs(Register from_reg, Register to_reg);
  void swap_reg(Register a, Register b);

  void emit_op0(LIR_Op0* op);
  void emit_op1(LIR_Op1* op);
  void emit_op2(LIR_Op2* op);
  void emit_op3(LIR_Op3* op);
  void emit_opBranch(LIR_OpBranch* op);
  void emit_opLabel(LIR_OpLabel* op);
  void emit_arraycopy(LIR_OpArrayCopy* op);
  void emit_updatecrc32(LIR_OpUpdateCRC32* op);
  void emit_opConvert(LIR_OpConvert* op);
  void emit_alloc_obj(LIR_OpAllocObj* op);
  void emit_alloc_array(LIR_OpAllocArray* op);
  void emit_opTypeCheck(LIR_OpTypeCheck* op);
  void emit_typecheck_helper(LIR_OpTypeCheck *op, Label* success, Label* failure, Label* obj_is_null);
  void emit_compare_and_swap(LIR_OpCompareAndSwap* op);
  void emit_lock(LIR_OpLock* op);
  void emit_call(LIR_OpJavaCall* op);
  void emit_rtcall(LIR_OpRTCall* op);
  void emit_profile_call(LIR_OpProfileCall* op);
  void emit_profile_type(LIR_OpProfileType* op);
  void emit_delay(LIR_OpDelay* op);

  void arith_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest, CodeEmitInfo* info, bool pop_fpu_stack);
  void arithmetic_idiv(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr temp, LIR_Opr result, CodeEmitInfo* info);
  void intrinsic_op(LIR_Code code, LIR_Opr value, LIR_Opr unused, LIR_Opr dest, LIR_Op* op);
#ifdef ASSERT
  void emit_assert(LIR_OpAssert* op);
#endif

  void logic_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest);

  void roundfp_op(LIR_Opr src, LIR_Opr tmp, LIR_Opr dest, bool pop_fpu_stack);
  void move_op(LIR_Opr src, LIR_Opr result, BasicType type,
               LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack, bool wide);
  void volatile_move_op(LIR_Opr src, LIR_Opr result, BasicType type, CodeEmitInfo* info);
  void comp_mem_op(LIR_Opr src, LIR_Opr result, BasicType type, CodeEmitInfo* info);  // info set for null exceptions
  void comp_fl2i(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr result, LIR_Op2* op);
  void cmove(LIR_Condition code, LIR_Opr left, LIR_Opr right, LIR_Opr result, BasicType type);

  void call(        LIR_OpJavaCall* op, relocInfo::relocType rtype);
  void ic_call(     LIR_OpJavaCall* op);
  void vtable_call( LIR_OpJavaCall* op);

  void osr_entry();

  void build_frame();

  void throw_op(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info);
  void unwind_op(LIR_Opr exceptionOop);
  void monitor_address(int monitor_ix, LIR_Opr dst);

  void align_backward_branch_target();
  void align_call(LIR_Code code);

  void negate(LIR_Opr left, LIR_Opr dest, LIR_Opr tmp = LIR_OprFact::illegalOpr);
  void leal(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code = lir_patch_none, CodeEmitInfo* info = NULL);

  void rt_call(LIR_Opr result, address dest, const LIR_OprList* args, LIR_Opr tmp, CodeEmitInfo* info);

  void membar();
  void membar_acquire();
  void membar_release();
  void membar_loadload();
  void membar_storestore();
  void membar_loadstore();
  void membar_storeload();
  void on_spin_wait();
  void get_thread(LIR_Opr result);

  void verify_oop_map(CodeEmitInfo* info);

  void atomic_op(LIR_Code code, LIR_Opr src, LIR_Opr data, LIR_Opr dest, LIR_Opr tmp);

#include CPU_HEADER(c1_LIRAssembler)

 public:

  static int call_stub_size() {
    return _call_stub_size;
  }

  static int exception_handler_size() {
    return _exception_handler_size;
  }

  static int deopt_handler_size() {
    return _deopt_handler_size;
  }
};

#endif // SHARE_C1_C1_LIRASSEMBLER_HPP
