/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2017 SAP SE. All rights reserved.
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

#ifndef CPU_S390_INTERP_MASM_S390_HPP
#define CPU_S390_INTERP_MASM_S390_HPP

#include "asm/macroAssembler.hpp"
#include "interpreter/invocationCounter.hpp"

// This file specializes the assember with interpreter-specific macros.

class InterpreterMacroAssembler: public MacroAssembler {

 protected:
  // Interpreter specific version of call_VM_base().
  virtual void call_VM_leaf_base(address entry_point);
  virtual void call_VM_leaf_base(address entry_point, bool allow_relocation);

  virtual void call_VM_base(Register oop_result,
                            Register last_java_sp,
                            address  entry_point,
                            bool check_exceptions);
  virtual void call_VM_base(Register oop_result,
                            Register last_java_sp,
                            address  entry_point,
                            bool allow_relocation,
                            bool check_exceptions);

  // Base routine for all dispatches.
  void dispatch_base(TosState state, address* table, bool generate_poll = false);

 public:
  InterpreterMacroAssembler(CodeBuffer* c)
    : MacroAssembler(c) {}

  virtual void check_and_handle_popframe(Register java_thread);
  virtual void check_and_handle_earlyret(Register java_thread);

  void jump_to_entry(address entry, Register Rscratch);

  virtual void load_earlyret_value(TosState state);

  static const Address l_tmp;
  static const Address d_tmp;

  // Handy address generation macros.
#define thread_(field_name) Address(Z_thread, JavaThread::field_name ## _offset())
#define method_(field_name) Address(Z_method, Method::field_name ## _offset())
#define method2_(Rmethod, field_name) Address(Rmethod, Method::field_name ## _offset())

  // Helper routine for frame allocation/deallocation.
  // Compute the delta by which the caller's SP has to
  // be adjusted to accomodate for the non-argument locals.
  void compute_extra_locals_size_in_bytes(Register args_size, Register locals_size, Register delta);

  // dispatch routines
  void dispatch_prolog(TosState state, int step = 0);
  void dispatch_epilog(TosState state, int step = 0);
  void dispatch_only(TosState state, bool generate_poll = false);
  // Dispatch normal table via Z_bytecode (assume Z_bytecode is loaded already).
  void dispatch_only_normal(TosState state);
  void dispatch_normal(TosState state);
  void dispatch_next(TosState state, int step = 0, bool generate_poll = false);
  void dispatch_next_noverify_oop(TosState state, int step = 0);
  void dispatch_via(TosState state, address* table);

  void narrow(Register result, Register ret_type);

  // Jump to an invoked target.
  void prepare_to_jump_from_interpreted(Register method);
  void jump_from_interpreted(Register method, Register temp);

  // Removes the current activation (incl. unlocking of monitors).
  // Additionally this code is used for earlyReturn in which case we
  // want to skip throwing an exception and installing an exception.
  void remove_activation(TosState state,
                         Register return_pc,
                         bool throw_monitor_exception = true,
                         bool install_monitor_exception = true,
                         bool notify_jvmti = true);

 public:
  // Super call_VM calls - correspond to MacroAssembler::call_VM(_leaf) calls.
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void super_call_VM(Register thread_cache, Register oop_result, Register last_java_sp,
                     address entry_point, Register arg_1, Register arg_2, bool check_exception = true);

  // Generate a subtype check: branch to ok_is_subtype if sub_klass is
  // a subtype of super_klass. Blows registers tmp1, tmp2 and tmp3.
  void gen_subtype_check(Register sub_klass, Register super_klass, Register tmp1, Register tmp2, Label &ok_is_subtype);

  void get_cache_and_index_at_bcp(Register cache, Register cpe_offset, int bcp_offset, size_t index_size = sizeof(u2));
  void get_cache_and_index_and_bytecode_at_bcp(Register cache, Register cpe_offset, Register bytecode,
                                               int byte_no, int bcp_offset, size_t index_size = sizeof(u2));
  void get_cache_entry_pointer_at_bcp(Register cache, Register tmp, int bcp_offset, size_t index_size = sizeof(u2));
  void get_cache_index_at_bcp(Register index, int bcp_offset, size_t index_size = sizeof(u2));
  void load_resolved_reference_at_index(Register result, Register index);
  // load cpool->resolved_klass_at(index)
  void load_resolved_klass_at_offset(Register cpool, Register offset, Register iklass);

  void load_resolved_method_at_index(int byte_no, Register cache, Register cpe_offset, Register method);

  // Pop topmost element from stack. It just disappears. Useful if
  // consumed previously by access via stackTop().
  void popx(int len);
  void pop_i()   { popx(1); }
  void pop_ptr() { popx(1); }
  void pop_l()   { popx(2); }
  void pop_f()   { popx(1); }
  void pop_d()   { popx(2); }
  // Get Address object of stack top. No checks. No pop.
  // Purpose: provide address of stack operand to exploit reg-mem operations.
  // Avoid RISC-like mem2reg - reg-reg-op sequence.
  Address stackTop();

  // Helpers for expression stack.
  void pop_i(     Register r);
  void pop_ptr(   Register r);
  void pop_l(     Register r);
  void pop_f(FloatRegister f);
  void pop_d(FloatRegister f);

  void push_i(     Register r = Z_tos);
  void push_ptr(   Register r = Z_tos);
  void push_l(     Register r = Z_tos);
  void push_f(FloatRegister f = Z_ftos);
  void push_d(FloatRegister f = Z_ftos);

  // Helpers for swap and dup.
  void load_ptr(int n, Register val);
  void store_ptr(int n, Register val);

  void pop (TosState state);           // transition vtos -> state
  void push(TosState state);           // transition state -> vtos
  void empty_expression_stack(void);

#ifdef ASSERT
  void verify_sp(Register Rsp, Register Rtemp);
  void verify_esp(Register Resp, Register Rtemp); // Verify that Resp points to a word in the operand stack.
#endif // ASSERT

 public:
  void if_cmp(Condition cc, bool ptr_compare);

  // Accessors to the template interpreter state.

  void asm_assert_ijava_state_magic(Register tmp) PRODUCT_RETURN;

  void save_bcp();

  void restore_bcp();

  void save_esp();

  void restore_esp();

  void get_monitors(Register reg);

  void save_monitors(Register reg);

  void get_mdp(Register mdp);

  void save_mdp(Register mdp);

  // Values that are only read (besides initialization).
  void restore_locals();

  void get_method(Register reg);

  // Load values from bytecode stream:

  enum signedOrNot { Signed, Unsigned };
  enum setCCOrNot  { set_CC,  dont_set_CC };

  void get_2_byte_integer_at_bcp(Register    Rdst,
                                 int         bcp_offset,
                                 signedOrNot is_signed  );

  void get_4_byte_integer_at_bcp(Register   Rdst,
                                 int        bcp_offset,
                                 setCCOrNot should_set_CC = dont_set_CC);

  // common code

  void field_offset_at(int n, Register tmp, Register dest, Register base);
  int  field_offset_at(Register object, address bcp, int offset);
  void fast_iaaccess(int n, address bcp);
  void fast_iaputfield(address bcp, bool do_store_check);

  void index_check(Register array, Register index, int index_shift, Register tmp, Register res);
  void index_check_without_pop(Register array, Register index, int index_shift, Register tmp, Register res);

  void get_constant_pool(Register Rdst);
  void get_constant_pool_cache(Register Rdst);
  void get_cpool_and_tags(Register Rcpool, Register Rtags);
  void is_a(Label& L);


  // --------------------------------------------------

  void unlock_if_synchronized_method(TosState state, bool throw_monitor_exception = true, bool install_monitor_exception = true);

  void add_monitor_to_stack(bool stack_is_empty,
                            Register Rtemp,
                            Register Rtemp2,
                            Register Rtemp3);

  void access_local_int(Register index, Register dst);
  void access_local_ptr(Register index, Register dst);
  void access_local_long(Register index, Register dst);
  void access_local_float(Register index, FloatRegister dst);
  void access_local_double(Register index, FloatRegister dst);
#ifdef ASSERT
  void check_for_regarea_stomp(Register Rindex, int offset, Register Rlimit, Register Rscratch, Register Rscratch1);
#endif // ASSERT
  void store_local_int(Register index, Register src);
  void store_local_ptr(Register index, Register src);
  void store_local_long(Register index, Register src);
  void store_local_float(Register index, FloatRegister src);
  void store_local_double(Register index, FloatRegister src);


  Address first_local_in_stack();
  static int top_most_monitor_byte_offset(); // Offset in bytes to top of monitor block.
  Address top_most_monitor();
  void compute_stack_base(Register Rdest);

  enum LoadOrStore { load, store };
  void static_iload_or_store(int which_local, LoadOrStore direction, Register Rtmp);
  void static_aload_or_store(int which_local, LoadOrStore direction, Register Rtmp);
  void static_dload_or_store(int which_local, LoadOrStore direction);

  void static_iinc(          int which_local, jint increment, Register Rtmp, Register Rtmp2);

  void get_method_counters(Register Rmethod, Register Rcounters, Label& skip);
  void increment_invocation_counter(Register Rcounters, Register RctrSum);
  void increment_backedge_counter(Register Rcounters, Register RctrSum);
  void test_backedge_count_for_osr(Register backedge_count, Register branch_bcp, Register Rtmp);

  void record_static_call_in_profile(Register Rentry, Register Rtmp);
  void record_receiver_call_in_profile(Register Rklass, Register Rentry, Register Rtmp);

  // Object locking
  void lock_object  (Register lock_reg, Register obj_reg);
  void unlock_object(Register lock_reg, Register obj_reg=noreg);

  // Interpreter profiling operations
  void set_method_data_pointer_for_bcp();
  void test_method_data_pointer(Register mdp, Label& zero_continue);
  void verify_method_data_pointer();

  void set_mdp_data_at(Register mdp_in, int constant, Register value);
  void increment_mdp_data_at(Register mdp_in, int constant,
                             Register tmp = Z_R1_scratch, bool decrement = false);
  void increment_mask_and_jump(Address counter_addr,
                               int increment, Address mask,
                               Register scratch, bool preloaded,
                               branch_condition cond, Label* where);
  void set_mdp_flag_at(Register mdp_in, int flag_constant);
  void test_mdp_data_at(Register mdp_in, int offset, Register value,
                        Register test_value_out,
                        Label& not_equal_continue);

  void record_klass_in_profile(Register receiver, Register mdp,
                               Register reg2, bool is_virtual_call);
  void record_klass_in_profile_helper(Register receiver, Register mdp,
                                      Register reg2, int start_row,
                                      Label& done, bool is_virtual_call);

  void update_mdp_by_offset(Register mdp_in, int offset_of_offset);
  void update_mdp_by_offset(Register mdp_in, Register dataidx, int offset_of_disp);
  void update_mdp_by_constant(Register mdp_in, int constant);
  void update_mdp_for_ret(Register return_bci);

  void profile_taken_branch(Register mdp, Register bumped_count);
  void profile_not_taken_branch(Register mdp);
  void profile_call(Register mdp);
  void profile_final_call(Register mdp);
  void profile_virtual_call(Register receiver, Register mdp,
                            Register scratch2,
                            bool receiver_can_be_null = false);
  void profile_ret(Register return_bci, Register mdp);
  void profile_null_seen(Register mdp);
  void profile_typecheck(Register mdp, Register klass, Register scratch);
  void profile_typecheck_failed(Register mdp, Register tmp);
  void profile_switch_default(Register mdp);
  void profile_switch_case(Register index_in_scratch, Register mdp,
                           Register scratch1, Register scratch2);

  void profile_obj_type(Register obj, Address mdo_addr, Register klass, bool cmp_done = false);
  void profile_arguments_type(Register mdp, Register callee, Register tmp, bool is_virtual);
  void profile_return_type(Register mdp, Register ret, Register tmp);
  void profile_parameters_type(Register mdp, Register tmp1, Register tmp2);

  // Debugging
  void verify_oop(Register reg, TosState state = atos);    // Only if +VerifyOops && state == atos.
  void verify_oop_or_return_address(Register reg, Register rtmp); // for astore
  void verify_FPU(int stack_depth, TosState state = ftos);

  // JVMTI helpers
  void skip_if_jvmti_mode(Label &Lskip, Register Rscratch = Z_R0);

  // support for JVMTI/Dtrace
  typedef enum { NotifyJVMTI, SkipNotifyJVMTI } NotifyMethodExitMode;
  void notify_method_entry();
  void notify_method_exit(bool native_method, TosState state, NotifyMethodExitMode mode);

  // Pop the topmost TOP_IJAVA_FRAME and set it's sender_sp as new Z_SP.
  // The return pc is loaded into the Register return_pc.
  void pop_interpreter_frame(Register return_pc, Register tmp1, Register tmp2);
};

#endif // CPU_S390_INTERP_MASM_S390_HPP
