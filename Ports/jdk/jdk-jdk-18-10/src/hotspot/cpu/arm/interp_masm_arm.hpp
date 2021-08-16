/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_INTERP_MASM_ARM_HPP
#define CPU_ARM_INTERP_MASM_ARM_HPP

#include "asm/macroAssembler.hpp"
#include "interpreter/invocationCounter.hpp"
#include "oops/method.hpp"
#include "runtime/frame.hpp"
#include "prims/jvmtiExport.hpp"

// This file specializes the assember with interpreter-specific macros


class InterpreterMacroAssembler: public MacroAssembler {

 public:

  // allow JvmtiExport checks to be extended
  bool can_force_early_return()       { return JvmtiExport::can_force_early_return(); }
  bool can_post_interpreter_events()  { return JvmtiExport::can_post_interpreter_events(); }
  bool can_pop_frame()                { return JvmtiExport::can_pop_frame(); }
  bool can_post_breakpoint()          { return JvmtiExport::can_post_breakpoint(); }
  bool can_post_field_access()        { return JvmtiExport::can_post_field_access(); }
  bool can_post_field_modification()  { return JvmtiExport::can_post_field_modification(); }
  // flags controlled by JVMTI settings
  bool rewrite_frequent_pairs()       { return RewriteFrequentPairs; }

 protected:

  // Template interpreter specific version of call_VM_helper
  virtual void call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions);

  // base routine for all dispatches
  typedef enum { DispatchDefault, DispatchNormal } DispatchTableMode;
  void dispatch_base(TosState state, DispatchTableMode table_mode, bool verifyoop = true, bool generate_poll = false);

 public:
  InterpreterMacroAssembler(CodeBuffer* code);

  virtual void check_and_handle_popframe();
  virtual void check_and_handle_earlyret();

  // Interpreter-specific registers

  inline void check_stack_top() {}
  inline void check_stack_top_on_expansion() {}
  inline void check_extended_sp(Register tmp) {}
  inline void check_no_cached_stack_top(Register tmp) {}


  void save_bcp()                                          { str(Rbcp, Address(FP, frame::interpreter_frame_bcp_offset * wordSize)); }
  void restore_bcp()                                       { ldr(Rbcp, Address(FP, frame::interpreter_frame_bcp_offset * wordSize)); }
  void restore_locals()                                    { ldr(Rlocals, Address(FP, frame::interpreter_frame_locals_offset * wordSize)); }
  void restore_method()                                    { ldr(Rmethod, Address(FP, frame::interpreter_frame_method_offset * wordSize)); }
  void restore_dispatch();


  // Helpers for runtime call arguments/results
  void get_const(Register reg)                             { ldr(reg, Address(Rmethod, Method::const_offset())); }
  void get_constant_pool(Register reg)                     { get_const(reg); ldr(reg, Address(reg, ConstMethod::constants_offset())); }
  void get_constant_pool_cache(Register reg)               { get_constant_pool(reg); ldr(reg, Address(reg, ConstantPool::cache_offset_in_bytes())); }
  void get_cpool_and_tags(Register cpool, Register tags)   { get_constant_pool(cpool); ldr(tags, Address(cpool, ConstantPool::tags_offset_in_bytes())); }

  // Sets reg. Blows Rtemp.
  void get_unsigned_2_byte_index_at_bcp(Register reg, int bcp_offset);

  // Sets index. Blows reg_tmp.
  void get_index_at_bcp(Register index, int bcp_offset, Register reg_tmp, size_t index_size = sizeof(u2));
  // Sets cache, index.
  void get_cache_and_index_at_bcp(Register cache, Register index, int bcp_offset, size_t index_size = sizeof(u2));
  void get_cache_and_index_and_bytecode_at_bcp(Register cache, Register index, Register bytecode, int byte_no, int bcp_offset, size_t index_size = sizeof(u2));
  // Sets cache. Blows reg_tmp.
  void get_cache_entry_pointer_at_bcp(Register cache, Register reg_tmp, int bcp_offset, size_t index_size = sizeof(u2));

  // Load object from cpool->resolved_references(*bcp+1)
  void load_resolved_reference_at_index(Register result, Register tmp);

  // load cpool->resolved_klass_at(index); Rtemp is corrupted upon return
  void load_resolved_klass_at_offset(Register Rcpool, Register Rindex, Register Rklass);

  void pop_ptr(Register r);
  void pop_i(Register r = R0_tos);
  void pop_l(Register lo = R0_tos_lo, Register hi = R1_tos_hi);
  void pop_f(FloatRegister fd);
  void pop_d(FloatRegister fd);

  void push_ptr(Register r);
  void push_i(Register r = R0_tos);
  void push_l(Register lo = R0_tos_lo, Register hi = R1_tos_hi);
  void push_f();
  void push_d();

  // Transition vtos -> state. Blows R0, R1. Sets TOS cached value.
  void pop(TosState state);
  // Transition state -> vtos. Blows Rtemp.
  void push(TosState state);

  // The following methods are overridden to allow overloaded calls to
  //   MacroAssembler::push/pop(Register)
  //   MacroAssembler::push/pop(RegisterSet)
  //   InterpreterMacroAssembler::push/pop(TosState)
  void push(Register rd, AsmCondition cond = al)         { MacroAssembler::push(rd, cond);      }
  void pop(Register rd, AsmCondition cond = al)          { MacroAssembler::pop(rd, cond);       }

  void push(RegisterSet reg_set, AsmCondition cond = al) { MacroAssembler::push(reg_set, cond); }
  void pop(RegisterSet reg_set, AsmCondition cond = al)  { MacroAssembler::pop(reg_set, cond);  }

  // Converts return value in R0/R1 (interpreter calling conventions) to TOS cached value.
  void convert_retval_to_tos(TosState state);
  // Converts TOS cached value to return value in R0/R1 (according to interpreter calling conventions).
  void convert_tos_to_retval(TosState state);

  // JVMTI ForceEarlyReturn support
  void load_earlyret_value(TosState state);

  void jump_to_entry(address entry);

  // Blows Rtemp.
  void empty_expression_stack() {
      ldr(Rstack_top, Address(FP, frame::interpreter_frame_monitor_block_top_offset * wordSize));
      check_stack_top();
      // NULL last_sp until next java call
      str(zero_register(Rtemp), Address(FP, frame::interpreter_frame_last_sp_offset * wordSize));
  }

  // Helpers for swap and dup
  void load_ptr(int n, Register val);
  void store_ptr(int n, Register val);

  // Generate a subtype check: branch to not_subtype if sub_klass is
  // not a subtype of super_klass.
  // Profiling code for the subtype check failure (profile_typecheck_failed)
  // should be explicitly generated by the caller in the not_subtype case.
  // Blows Rtemp, tmp1, tmp2.
  void gen_subtype_check(Register Rsub_klass, Register Rsuper_klass,
                         Label &not_subtype, Register tmp1, Register tmp2);

  // Dispatching
  void dispatch_prolog(TosState state, int step = 0);
  void dispatch_epilog(TosState state, int step = 0);
  void dispatch_only(TosState state, bool generate_poll = false);  // dispatch by R3_bytecode
  void dispatch_only_normal(TosState state);                       // dispatch normal table by R3_bytecode
  void dispatch_only_noverify(TosState state);
  void dispatch_next(TosState state, int step = 0, bool generate_poll = false); // load R3_bytecode from [Rbcp + step] and dispatch by R3_bytecode

  // jump to an invoked target
  void prepare_to_jump_from_interpreted();
  void jump_from_interpreted(Register method);

  void narrow(Register result);

  // Returning from interpreted functions
  //
  // Removes the current activation (incl. unlocking of monitors)
  // and sets up the return address.  This code is also used for
  // exception unwindwing. In that case, we do not want to throw
  // IllegalMonitorStateExceptions, since that might get us into an
  // infinite rethrow exception loop.
  // Additionally this code is used for popFrame and earlyReturn.
  // In popFrame case we want to skip throwing an exception,
  // installing an exception, and notifying jvmdi.
  // In earlyReturn case we only want to skip throwing an exception
  // and installing an exception.
  void remove_activation(TosState state, Register ret_addr,
                         bool throw_monitor_exception = true,
                         bool install_monitor_exception = true,
                         bool notify_jvmdi = true);

  // At certain points in the method invocation the monitor of
  // synchronized methods hasn't been entered yet.
  // To correctly handle exceptions at these points, we set the thread local
  // variable _do_not_unlock_if_synchronized to true. The remove_activation will
  // check this flag.
  void set_do_not_unlock_if_synchronized(bool flag, Register tmp);

  // Debugging
  void interp_verify_oop(Register reg, TosState state, const char* file, int line);    // only if +VerifyOops && state == atos

  void verify_FPU(int stack_depth, TosState state = ftos) {
    // No VFP state verification is required for ARM
  }

  // Object locking
  void lock_object  (Register lock_reg);
  void unlock_object(Register lock_reg);

  // Interpreter profiling operations
  void set_method_data_pointer_for_bcp(); // Blows R0-R3/R0-R18, Rtemp, LR
  void test_method_data_pointer(Register mdp, Label& zero_continue);
  void verify_method_data_pointer();

  void set_mdp_data_at(Register mdp_in, int offset, Register value);

  // Increments mdp data. Sets bumped_count register to adjusted counter.
  void increment_mdp_data_at(Address data, Register bumped_count, bool decrement = false);
  // Increments mdp data. Sets bumped_count register to adjusted counter.
  void increment_mdp_data_at(Register mdp_in, int offset, Register bumped_count, bool decrement = false);
  void increment_mask_and_jump(Address counter_addr,
                               int increment, Address mask_addr,
                               Register scratch, Register scratch2,
                               AsmCondition cond, Label* where);
  void set_mdp_flag_at(Register mdp_in, int flag_constant);

  void test_mdp_data_at(Register mdp_in, int offset, Register value,
                        Register test_value_out,
                        Label& not_equal_continue);

  void record_klass_in_profile(Register receiver, Register mdp,
                               Register reg_tmp, bool is_virtual_call);
  void record_klass_in_profile_helper(Register receiver, Register mdp,
                                      Register reg_tmp,
                                      int start_row, Label& done, bool is_virtual_call);

  void update_mdp_by_offset(Register mdp_in, int offset_of_offset, Register reg_tmp);
  void update_mdp_by_offset(Register mdp_in, Register reg_offset, Register reg_tmp);
  void update_mdp_by_constant(Register mdp_in, int constant);
  void update_mdp_for_ret(Register return_bci);                   // Blows R0-R3/R0-R18, Rtemp, LR

  void profile_taken_branch(Register mdp, Register bumped_count); // Sets mdp, bumped_count registers, blows Rtemp.
  void profile_not_taken_branch(Register mdp);                    // Sets mdp, blows Rtemp.

  void profile_call(Register mdp);                                // Sets mdp, blows Rtemp.
  void profile_final_call(Register mdp);                          // Sets mdp, blows Rtemp.
  void profile_virtual_call(Register mdp, Register receiver,      // Sets mdp, blows Rtemp.
                            bool receiver_can_be_null = false);
  void profile_ret(Register mdp, Register return_bci);            // Sets mdp, blows R0-R3/R0-R18, Rtemp, LR
  void profile_null_seen(Register mdp);                           // Sets mdp.
  void profile_typecheck(Register mdp, Register klass);           // Sets mdp, blows Rtemp.

  void profile_typecheck_failed(Register mdp);                    // Sets mdp, blows Rtemp.
  void profile_switch_default(Register mdp);                      // Sets mdp, blows Rtemp.

  // Sets mdp. Blows reg_tmp1, reg_tmp2. Index could be the same as reg_tmp2.
  void profile_switch_case(Register mdp, Register index, Register reg_tmp1, Register reg_tmp2);

  void byteswap_u32(Register r, Register rtmp1, Register rtmp2);

  void inc_global_counter(address address_of_counter, int offset_in_bytes, Register tmp1, Register tmp2, bool avoid_overflow);

  typedef enum { NotifyJVMTI, SkipNotifyJVMTI } NotifyMethodExitMode;

  // support for jvmti
  void notify_method_entry();
  void notify_method_exit(TosState state, NotifyMethodExitMode mode,
                          bool native = false, Register result_lo = noreg, Register result_hi = noreg, FloatRegister result_fp = fnoreg);

  void trace_state(const char* msg) PRODUCT_RETURN;

void get_method_counters(Register method,
                         Register Rcounters,
                         Label& skip,
                         bool saveRegs = false,
                         Register reg1 = noreg,
                         Register reg2 = noreg,
                         Register reg3 = noreg);
};

#endif // CPU_ARM_INTERP_MASM_ARM_HPP
