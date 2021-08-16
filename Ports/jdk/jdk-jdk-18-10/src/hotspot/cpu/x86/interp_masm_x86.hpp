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

#ifndef CPU_X86_INTERP_MASM_X86_HPP
#define CPU_X86_INTERP_MASM_X86_HPP

#include "asm/macroAssembler.hpp"
#include "interpreter/invocationCounter.hpp"
#include "oops/method.hpp"
#include "runtime/frame.hpp"

// This file specializes the assember with interpreter-specific macros

typedef ByteSize (*OffsetFunction)(uint);

class InterpreterMacroAssembler: public MacroAssembler {
 public:
  // Interpreter specific version of call_VM_base
  virtual void call_VM_leaf_base(address entry_point,
                                 int number_of_arguments);

 protected:

  virtual void call_VM_base(Register oop_result,
                            Register java_thread,
                            Register last_java_sp,
                            address  entry_point,
                            int number_of_arguments,
                            bool check_exceptions);

  // base routine for all dispatches
  void dispatch_base(TosState state, address* table, bool verifyoop = true, bool generate_poll = false);

 public:
  InterpreterMacroAssembler(CodeBuffer* code) : MacroAssembler(code),
    _locals_register(LP64_ONLY(r14) NOT_LP64(rdi)),
    _bcp_register(LP64_ONLY(r13) NOT_LP64(rsi)) {}

  void jump_to_entry(address entry);

 virtual void check_and_handle_popframe(Register java_thread);
 virtual void check_and_handle_earlyret(Register java_thread);

  void load_earlyret_value(TosState state);

  // Interpreter-specific registers
  void save_bcp() {
    movptr(Address(rbp, frame::interpreter_frame_bcp_offset * wordSize), _bcp_register);
  }

  void restore_bcp() {
    movptr(_bcp_register, Address(rbp, frame::interpreter_frame_bcp_offset * wordSize));
  }

  void restore_locals() {
    movptr(_locals_register, Address(rbp, frame::interpreter_frame_locals_offset * wordSize));
  }

  // Helpers for runtime call arguments/results
  void get_method(Register reg) {
    movptr(reg, Address(rbp, frame::interpreter_frame_method_offset * wordSize));
  }

  void get_const(Register reg) {
    get_method(reg);
    movptr(reg, Address(reg, Method::const_offset()));
  }

  void get_constant_pool(Register reg) {
    get_const(reg);
    movptr(reg, Address(reg, ConstMethod::constants_offset()));
  }

  void get_constant_pool_cache(Register reg) {
    get_constant_pool(reg);
    movptr(reg, Address(reg, ConstantPool::cache_offset_in_bytes()));
  }

  void get_cpool_and_tags(Register cpool, Register tags) {
    get_constant_pool(cpool);
    movptr(tags, Address(cpool, ConstantPool::tags_offset_in_bytes()));
  }

  void get_unsigned_2_byte_index_at_bcp(Register reg, int bcp_offset);
  void get_cache_and_index_at_bcp(Register cache,
                                  Register index,
                                  int bcp_offset,
                                  size_t index_size = sizeof(u2));
  void get_cache_and_index_and_bytecode_at_bcp(Register cache,
                                               Register index,
                                               Register bytecode,
                                               int byte_no,
                                               int bcp_offset,
                                               size_t index_size = sizeof(u2));
  void get_cache_entry_pointer_at_bcp(Register cache,
                                      Register tmp,
                                      int bcp_offset,
                                      size_t index_size = sizeof(u2));
  void get_cache_index_at_bcp(Register index,
                              int bcp_offset,
                              size_t index_size = sizeof(u2));

  // load cpool->resolved_references(index);
  void load_resolved_reference_at_index(Register result, Register index, Register tmp = rscratch2);

  // load cpool->resolved_klass_at(index)
  void load_resolved_klass_at_index(Register klass,  // contains the Klass on return
                                    Register cpool,  // the constant pool (corrupted on return)
                                    Register index); // the constant pool index (corrupted on return)

  void load_resolved_method_at_index(int byte_no,
                                     Register method,
                                     Register cache,
                                     Register index);

  NOT_LP64(void f2ieee();)        // truncate ftos to 32bits
  NOT_LP64(void d2ieee();)        // truncate dtos to 64bits

  // Expression stack
  void pop_ptr(Register r = rax);
  void pop_i(Register r = rax);

  // On x86, pushing a ptr or an int is semantically identical, but we
  // maintain a distinction for clarity and for making it easier to change
  // semantics in the future
  void push_ptr(Register r = rax);
  void push_i(Register r = rax);

  // push_i_or_ptr is provided for when explicitly allowing either a ptr or
  // an int might have some advantage, while still documenting the fact that a
  // ptr might be pushed to the stack.
  void push_i_or_ptr(Register r = rax);

  void push_f(XMMRegister r);
  void pop_f(XMMRegister r);
  void pop_d(XMMRegister r);
  void push_d(XMMRegister r);
#ifdef _LP64
  void pop_l(Register r = rax);
  void push_l(Register r = rax);
#else
  void pop_l(Register lo = rax, Register hi = rdx);
  void pop_f();
  void pop_d();

  void push_l(Register lo = rax, Register hi = rdx);
  void push_d();
  void push_f();
#endif // _LP64

  void pop(Register r) { ((MacroAssembler*)this)->pop(r); }
  void push(Register r) { ((MacroAssembler*)this)->push(r); }
  void push(int32_t imm ) { ((MacroAssembler*)this)->push(imm); }

  void pop(TosState state);        // transition vtos -> state
  void push(TosState state);       // transition state -> vtos

  // These are dummies to prevent surprise implicit conversions to Register
  void pop(void* v); // Add unimplemented ambiguous method
  void push(void* v);   // Add unimplemented ambiguous method

  void empty_expression_stack() {
    movptr(rsp, Address(rbp, frame::interpreter_frame_monitor_block_top_offset * wordSize));
    // NULL last_sp until next java call
    movptr(Address(rbp, frame::interpreter_frame_last_sp_offset * wordSize), (int32_t)NULL_WORD);
    NOT_LP64(empty_FPU_stack());
  }

  // Helpers for swap and dup
  void load_ptr(int n, Register val);
  void store_ptr(int n, Register val);

  // Generate a subtype check: branch to ok_is_subtype if sub_klass is
  // a subtype of super_klass.
  void gen_subtype_check( Register sub_klass, Label &ok_is_subtype );

  // Dispatching
  void dispatch_prolog(TosState state, int step = 0);
  void dispatch_epilog(TosState state, int step = 0);
  // dispatch via rbx (assume rbx is loaded already)
  void dispatch_only(TosState state, bool generate_poll = false);
  // dispatch normal table via rbx (assume rbx is loaded already)
  void dispatch_only_normal(TosState state);
  void dispatch_only_noverify(TosState state);
  // load rbx from [_bcp_register + step] and dispatch via rbx
  void dispatch_next(TosState state, int step = 0, bool generate_poll = false);
  // load rbx from [_bcp_register] and dispatch via rbx and table
  void dispatch_via (TosState state, address* table);

  // jump to an invoked target
  void prepare_to_jump_from_interpreted();
  void jump_from_interpreted(Register method, Register temp);

  // narrow int return value
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
  void get_method_counters(Register method, Register mcs, Label& skip);

  // Object locking
  void lock_object  (Register lock_reg);
  void unlock_object(Register lock_reg);

  // Interpreter profiling operations
  void set_method_data_pointer_for_bcp();
  void test_method_data_pointer(Register mdp, Label& zero_continue);
  void verify_method_data_pointer();

  void set_mdp_data_at(Register mdp_in, int constant, Register value);
  void increment_mdp_data_at(Address data, bool decrement = false);
  void increment_mdp_data_at(Register mdp_in, int constant,
                             bool decrement = false);
  void increment_mdp_data_at(Register mdp_in, Register reg, int constant,
                             bool decrement = false);
  void increment_mask_and_jump(Address counter_addr,
                               int increment, Address mask,
                               Register scratch, bool preloaded,
                               Condition cond, Label* where);
  void set_mdp_flag_at(Register mdp_in, int flag_constant);
  void test_mdp_data_at(Register mdp_in, int offset, Register value,
                        Register test_value_out,
                        Label& not_equal_continue);

  void record_klass_in_profile(Register receiver, Register mdp,
                               Register reg2, bool is_virtual_call);
  void record_klass_in_profile_helper(Register receiver, Register mdp,
                                      Register reg2, int start_row,
                                      Label& done, bool is_virtual_call);
  void record_item_in_profile_helper(Register item, Register mdp,
                                     Register reg2, int start_row, Label& done, int total_rows,
                                     OffsetFunction item_offset_fn, OffsetFunction item_count_offset_fn,
                                     int non_profiled_offset);

  void update_mdp_by_offset(Register mdp_in, int offset_of_offset);
  void update_mdp_by_offset(Register mdp_in, Register reg, int offset_of_disp);
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
  void profile_typecheck_failed(Register mdp);
  void profile_switch_default(Register mdp);
  void profile_switch_case(Register index_in_scratch, Register mdp,
                           Register scratch2);

  // Debugging
  // only if +VerifyOops && state == atos
#define interp_verify_oop(reg, state) _interp_verify_oop(reg, state, __FILE__, __LINE__);
  void _interp_verify_oop(Register reg, TosState state, const char* file, int line);
  // only if +VerifyFPU  && (state == ftos || state == dtos)
  void verify_FPU(int stack_depth, TosState state = ftos);

  typedef enum { NotifyJVMTI, SkipNotifyJVMTI } NotifyMethodExitMode;

  // support for jvmti/dtrace
  void notify_method_entry();
  void notify_method_exit(TosState state, NotifyMethodExitMode mode);

 private:

  Register _locals_register; // register that contains the pointer to the locals
  Register _bcp_register; // register that contains the bcp

 public:
  void profile_obj_type(Register obj, const Address& mdo_addr);
  void profile_arguments_type(Register mdp, Register callee, Register tmp, bool is_virtual);
  void profile_return_type(Register mdp, Register ret, Register tmp);
  void profile_parameters_type(Register mdp, Register tmp1, Register tmp2);

};

#endif // CPU_X86_INTERP_MASM_X86_HPP
