/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_MACROASSEMBLER_PPC_HPP
#define CPU_PPC_MACROASSEMBLER_PPC_HPP

#include "asm/assembler.hpp"
#include "oops/accessDecorators.hpp"
#include "runtime/rtmLocking.hpp"
#include "utilities/macros.hpp"

// MacroAssembler extends Assembler by a few frequently used macros.

class ciTypeArray;

class MacroAssembler: public Assembler {
 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

  // Indicates whether and, if so, which registers must be preserved when calling runtime code.
  enum PreservationLevel {
    PRESERVATION_NONE,
    PRESERVATION_FRAME_LR,
    PRESERVATION_FRAME_LR_GP_REGS,
    PRESERVATION_FRAME_LR_GP_FP_REGS
  };

  //
  // Optimized instruction emitters
  //

  inline static int largeoffset_si16_si16_hi(int si31) { return (si31 + (1<<15)) >> 16; }
  inline static int largeoffset_si16_si16_lo(int si31) { return si31 - (((si31 + (1<<15)) >> 16) << 16); }

  // load d = *[a+si31]
  // Emits several instructions if the offset is not encodable in one instruction.
  void ld_largeoffset_unchecked(Register d, int si31, Register a, int emit_filler_nop);
  void ld_largeoffset          (Register d, int si31, Register a, int emit_filler_nop);
  inline static bool is_ld_largeoffset(address a);
  inline static int get_ld_largeoffset_offset(address a);

  inline void round_to(Register r, int modulus);

  // Load/store with type given by parameter.
  void load_sized_value( Register dst, RegisterOrConstant offs, Register base, size_t size_in_bytes, bool is_signed);
  void store_sized_value(Register dst, RegisterOrConstant offs, Register base, size_t size_in_bytes);

  // Move register if destination register and target register are different
  inline void mr_if_needed(Register rd, Register rs);
  inline void fmr_if_needed(FloatRegister rd, FloatRegister rs);
  // This is dedicated for emitting scheduled mach nodes. For better
  // readability of the ad file I put it here.
  // Endgroups are not needed if
  //  - the scheduler is off
  //  - the scheduler found that there is a natural group end, in that
  //    case it reduced the size of the instruction used in the test
  //    yielding 'needed'.
  inline void endgroup_if_needed(bool needed);

  // Memory barriers.
  inline void membar(int bits);
  inline void release();
  inline void acquire();
  inline void fence();

  // nop padding
  void align(int modulus, int max = 252, int rem = 0);

  // Align prefix opcode to make sure it's not on the last word of a
  // 64-byte block.
  //
  // Note: do not call align_prefix() in a .ad file (e.g. ppc.ad).  Instead
  // add ins_alignment(2) to the instruct definition and implement the
  // compute_padding() method of the instruct node to use
  // compute_prefix_padding().  See loadConI32Node::compute_padding() in
  // ppc.ad for an example.
  void align_prefix();

  //
  // Constants, loading constants, TOC support
  //

  // Address of the global TOC.
  inline static address global_toc();
  // Offset of given address to the global TOC.
  inline static int offset_to_global_toc(const address addr);

  // Address of TOC of the current method.
  inline address method_toc();
  // Offset of given address to TOC of the current method.
  inline int offset_to_method_toc(const address addr);

  // Global TOC.
  void calculate_address_from_global_toc(Register dst, address addr,
                                         bool hi16 = true, bool lo16 = true,
                                         bool add_relocation = true, bool emit_dummy_addr = false);
  inline void calculate_address_from_global_toc_hi16only(Register dst, address addr) {
    calculate_address_from_global_toc(dst, addr, true, false);
  };
  inline void calculate_address_from_global_toc_lo16only(Register dst, address addr) {
    calculate_address_from_global_toc(dst, addr, false, true);
  };

  inline static bool is_calculate_address_from_global_toc_at(address a, address bound);
  // Returns address of first instruction in sequence.
  static address patch_calculate_address_from_global_toc_at(address a, address bound, address addr);
  static address get_address_of_calculate_address_from_global_toc_at(address a, address addr);

#ifdef _LP64
  // Patch narrow oop constant.
  inline static bool is_set_narrow_oop(address a, address bound);
  // Returns address of first instruction in sequence.
  static address patch_set_narrow_oop(address a, address bound, narrowOop data);
  static narrowOop get_narrow_oop(address a, address bound);
#endif

  inline static bool is_load_const_at(address a);

  // Emits an oop const to the constant pool, loads the constant, and
  // sets a relocation info with address current_pc.
  // Returns true if successful.
  bool load_const_from_method_toc(Register dst, AddressLiteral& a, Register toc, bool fixed_size = false);

  static bool is_load_const_from_method_toc_at(address a);
  static int get_offset_of_load_const_from_method_toc_at(address a);

  // Get the 64 bit constant from a `load_const' sequence.
  static long get_const(address load_const);

  // Patch the 64 bit constant of a `load_const' sequence. This is a
  // low level procedure. It neither flushes the instruction cache nor
  // is it atomic.
  static void patch_const(address load_const, long x);

  // Metadata in code that we have to keep track of.
  AddressLiteral allocate_metadata_address(Metadata* obj); // allocate_index
  AddressLiteral constant_metadata_address(Metadata* obj); // find_index
  // Oops used directly in compiled code are stored in the constant pool,
  // and loaded from there.
  // Allocate new entry for oop in constant pool. Generate relocation.
  AddressLiteral allocate_oop_address(jobject obj);
  // Find oop obj in constant pool. Return relocation with it's index.
  AddressLiteral constant_oop_address(jobject obj);

  // Find oop in constant pool and emit instructions to load it.
  // Uses constant_oop_address.
  inline void set_oop_constant(jobject obj, Register d);
  // Same as load_address.
  inline void set_oop         (AddressLiteral obj_addr, Register d);

  //
  // branch, jump
  //
  // set dst to -1, 0, +1 as follows: if CCR0bi is "greater than", dst is set to 1,
  // if CCR0bi is "equal", dst is set to 0, otherwise it's set to -1.
  void inline set_cmp3(Register dst);
  // set dst to (treat_unordered_like_less ? -1 : +1)
  void inline set_cmpu3(Register dst, bool treat_unordered_like_less);

  inline void pd_patch_instruction(address branch, address target, const char* file, int line);
  NOT_PRODUCT(static void pd_print_patched_instruction(address branch);)

  // Conditional far branch for destinations encodable in 24+2 bits.
  // Same interface as bc, e.g. no inverse boint-field.
  enum {
    bc_far_optimize_not         = 0,
    bc_far_optimize_on_relocate = 1
  };
  // optimize: flag for telling the conditional far branch to optimize
  //           itself when relocated.
  void bc_far(int boint, int biint, Label& dest, int optimize);
  void bc_far_optimized(int boint, int biint, Label& dest); // 1 or 2 instructions
  // Relocation of conditional far branches.
  static bool    is_bc_far_at(address instruction_addr);
  static address get_dest_of_bc_far_at(address instruction_addr);
  static void    set_dest_of_bc_far_at(address instruction_addr, address dest);
 private:
  static bool inline is_bc_far_variant1_at(address instruction_addr);
  static bool inline is_bc_far_variant2_at(address instruction_addr);
  static bool inline is_bc_far_variant3_at(address instruction_addr);
 public:

  // Convenience bc_far versions.
  inline void blt_far(ConditionRegister crx, Label& L, int optimize);
  inline void bgt_far(ConditionRegister crx, Label& L, int optimize);
  inline void beq_far(ConditionRegister crx, Label& L, int optimize);
  inline void bso_far(ConditionRegister crx, Label& L, int optimize);
  inline void bge_far(ConditionRegister crx, Label& L, int optimize);
  inline void ble_far(ConditionRegister crx, Label& L, int optimize);
  inline void bne_far(ConditionRegister crx, Label& L, int optimize);
  inline void bns_far(ConditionRegister crx, Label& L, int optimize);

  // Emit, identify and patch a NOT mt-safe patchable 64 bit absolute call/jump.
 private:
  enum {
    bxx64_patchable_instruction_count = (2/*load_codecache_const*/ + 3/*5load_const*/ + 1/*mtctr*/ + 1/*bctrl*/),
    bxx64_patchable_size              = bxx64_patchable_instruction_count * BytesPerInstWord,
    bxx64_patchable_ret_addr_offset   = bxx64_patchable_size
  };
  void bxx64_patchable(address target, relocInfo::relocType rt, bool link);
  static bool is_bxx64_patchable_at(            address instruction_addr, bool link);
  // Does the instruction use a pc-relative encoding of the destination?
  static bool is_bxx64_patchable_pcrelative_at( address instruction_addr, bool link);
  static bool is_bxx64_patchable_variant1_at(   address instruction_addr, bool link);
  // Load destination relative to global toc.
  static bool is_bxx64_patchable_variant1b_at(  address instruction_addr, bool link);
  static bool is_bxx64_patchable_variant2_at(   address instruction_addr, bool link);
  static void set_dest_of_bxx64_patchable_at(   address instruction_addr, address target, bool link);
  static address get_dest_of_bxx64_patchable_at(address instruction_addr, bool link);

 public:
  // call
  enum {
    bl64_patchable_instruction_count = bxx64_patchable_instruction_count,
    bl64_patchable_size              = bxx64_patchable_size,
    bl64_patchable_ret_addr_offset   = bxx64_patchable_ret_addr_offset
  };
  inline void bl64_patchable(address target, relocInfo::relocType rt) {
    bxx64_patchable(target, rt, /*link=*/true);
  }
  inline static bool is_bl64_patchable_at(address instruction_addr) {
    return is_bxx64_patchable_at(instruction_addr, /*link=*/true);
  }
  inline static bool is_bl64_patchable_pcrelative_at(address instruction_addr) {
    return is_bxx64_patchable_pcrelative_at(instruction_addr, /*link=*/true);
  }
  inline static void set_dest_of_bl64_patchable_at(address instruction_addr, address target) {
    set_dest_of_bxx64_patchable_at(instruction_addr, target, /*link=*/true);
  }
  inline static address get_dest_of_bl64_patchable_at(address instruction_addr) {
    return get_dest_of_bxx64_patchable_at(instruction_addr, /*link=*/true);
  }
  // jump
  enum {
    b64_patchable_instruction_count = bxx64_patchable_instruction_count,
    b64_patchable_size              = bxx64_patchable_size,
  };
  inline void b64_patchable(address target, relocInfo::relocType rt) {
    bxx64_patchable(target, rt, /*link=*/false);
  }
  inline static bool is_b64_patchable_at(address instruction_addr) {
    return is_bxx64_patchable_at(instruction_addr, /*link=*/false);
  }
  inline static bool is_b64_patchable_pcrelative_at(address instruction_addr) {
    return is_bxx64_patchable_pcrelative_at(instruction_addr, /*link=*/false);
  }
  inline static void set_dest_of_b64_patchable_at(address instruction_addr, address target) {
    set_dest_of_bxx64_patchable_at(instruction_addr, target, /*link=*/false);
  }
  inline static address get_dest_of_b64_patchable_at(address instruction_addr) {
    return get_dest_of_bxx64_patchable_at(instruction_addr, /*link=*/false);
  }

  //
  // Support for frame handling
  //

  // some ABI-related functions

  // Clobbers all volatile, (non-floating-point) general-purpose registers for debugging purposes.
  // This is especially useful for making calls to the JRT in places in which this hasn't been done before;
  // e.g. with the introduction of LRBs (load reference barriers) for concurrent garbage collection.
  void clobber_volatile_gprs(Register excluded_register = noreg);
  void clobber_carg_stack_slots(Register tmp);

  void save_nonvolatile_gprs(   Register dst_base, int offset);
  void restore_nonvolatile_gprs(Register src_base, int offset);

  enum {
    num_volatile_gp_regs = 11,
    num_volatile_fp_regs = 14,
    num_volatile_regs = num_volatile_gp_regs + num_volatile_fp_regs
  };

  void save_volatile_gprs(   Register dst_base, int offset,
                             bool include_fp_regs = true, bool include_R3_RET_reg = true);
  void restore_volatile_gprs(Register src_base, int offset,
                             bool include_fp_regs = true, bool include_R3_RET_reg = true);
  void save_LR_CR(   Register tmp);     // tmp contains LR on return.
  void restore_LR_CR(Register tmp);

  // Get current PC using bl-next-instruction trick.
  address get_PC_trash_LR(Register result);

  // Resize current frame either relatively wrt to current SP or absolute.
  void resize_frame(Register offset, Register tmp);
  void resize_frame(int      offset, Register tmp);
  void resize_frame_absolute(Register addr, Register tmp1, Register tmp2);

  // Push a frame of size bytes.
  void push_frame(Register bytes, Register tmp);

  // Push a frame of size `bytes'. No abi space provided.
  void push_frame(unsigned int bytes, Register tmp);

  // Push a frame of size `bytes' plus abi_reg_args on top.
  void push_frame_reg_args(unsigned int bytes, Register tmp);

  // Setup up a new C frame with a spill area for non-volatile GPRs and additional
  // space for local variables
  void push_frame_reg_args_nonvolatiles(unsigned int bytes, Register tmp);

  // pop current C frame
  void pop_frame();

  //
  // Calls
  //

 private:
  address _last_calls_return_pc;

#if defined(ABI_ELFv2)
  // Generic version of a call to C function.
  // Updates and returns _last_calls_return_pc.
  address branch_to(Register function_entry, bool and_link);
#else
  // Generic version of a call to C function via a function descriptor
  // with variable support for C calling conventions (TOC, ENV, etc.).
  // updates and returns _last_calls_return_pc.
  address branch_to(Register function_descriptor, bool and_link, bool save_toc_before_call,
                    bool restore_toc_after_call, bool load_toc_of_callee, bool load_env_of_callee);
#endif

 public:

  // Get the pc where the last call will return to. returns _last_calls_return_pc.
  inline address last_calls_return_pc();

#if defined(ABI_ELFv2)
  // Call a C function via a function descriptor and use full C
  // calling conventions. Updates and returns _last_calls_return_pc.
  address call_c(Register function_entry);
  // For tail calls: only branch, don't link, so callee returns to caller of this function.
  address call_c_and_return_to_caller(Register function_entry);
  address call_c(address function_entry, relocInfo::relocType rt);
#else
  // Call a C function via a function descriptor and use full C
  // calling conventions. Updates and returns _last_calls_return_pc.
  address call_c(Register function_descriptor);
  // For tail calls: only branch, don't link, so callee returns to caller of this function.
  address call_c_and_return_to_caller(Register function_descriptor);
  address call_c(const FunctionDescriptor* function_descriptor, relocInfo::relocType rt);
  address call_c_using_toc(const FunctionDescriptor* function_descriptor, relocInfo::relocType rt,
                           Register toc);
#endif

 protected:

  // It is imperative that all calls into the VM are handled via the
  // call_VM macros. They make sure that the stack linkage is setup
  // correctly. call_VM's correspond to ENTRY/ENTRY_X entry points
  // while call_VM_leaf's correspond to LEAF entry points.
  //
  // This is the base routine called by the different versions of
  // call_VM. The interpreter may customize this version by overriding
  // it for its purposes (e.g., to save/restore additional registers
  // when doing a VM call).
  //
  // If no last_java_sp is specified (noreg) then SP will be used instead.
  virtual void call_VM_base(
     // where an oop-result ends up if any; use noreg otherwise
    Register        oop_result,
    // to set up last_Java_frame in stubs; use noreg otherwise
    Register        last_java_sp,
    // the entry point
    address         entry_point,
    // flag which indicates if exception should be checked
    bool            check_exception = true
  );

  // Support for VM calls. This is the base routine called by the
  // different versions of call_VM_leaf. The interpreter may customize
  // this version by overriding it for its purposes (e.g., to
  // save/restore additional registers when doing a VM call).
  void call_VM_leaf_base(address entry_point);

 public:
  // Call into the VM.
  // Passes the thread pointer (in R3_ARG1) as a prepended argument.
  // Makes sure oop return values are visible to the GC.
  void call_VM(Register oop_result, address entry_point, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, Register arg3, bool check_exceptions = true);
  void call_VM_leaf(address entry_point);
  void call_VM_leaf(address entry_point, Register arg_1);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);

  // Call a stub function via a function descriptor, but don't save
  // TOC before call, don't setup TOC and ENV for call, and don't
  // restore TOC after call. Updates and returns _last_calls_return_pc.
  inline address call_stub(Register function_entry);
  inline void call_stub_and_return_to(Register function_entry, Register return_pc);

  //
  // Java utilities
  //

  // Read from the polling page, its address is already in a register.
  inline void load_from_polling_page(Register polling_page_address, int offset = 0);
  // Check whether instruction is a read access to the polling page
  // which was emitted by load_from_polling_page(..).
  static bool is_load_from_polling_page(int instruction, void* ucontext/*may be NULL*/,
                                        address* polling_address_ptr = NULL);

  // Support for NULL-checks
  //
  // Generates code that causes a NULL OS exception if the content of reg is NULL.
  // If the accessed location is M[reg + offset] and the offset is known, provide the
  // offset. No explicit code generation is needed if the offset is within a certain
  // range (0 <= offset <= page_size).

  // Stack overflow checking
  void bang_stack_with_offset(int offset);

  // If instruction is a stack bang of the form ld, stdu, or
  // stdux, return the banged address. Otherwise, return 0.
  static address get_stack_bang_address(int instruction, void* ucontext);

  // Check for reserved stack access in method being exited. If the reserved
  // stack area was accessed, protect it again and throw StackOverflowError.
  void reserved_stack_check(Register return_pc);

  // Atomics
  // CmpxchgX sets condition register to cmpX(current, compare).
  // (flag == ne) => (dest_current_value != compare_value), (!swapped)
  // (flag == eq) => (dest_current_value == compare_value), ( swapped)
  static inline bool cmpxchgx_hint_acquire_lock()  { return true; }
  // The stxcx will probably not be succeeded by a releasing store.
  static inline bool cmpxchgx_hint_release_lock()  { return false; }
  static inline bool cmpxchgx_hint_atomic_update() { return false; }

  // Cmpxchg semantics
  enum {
    MemBarNone = 0,
    MemBarRel  = 1,
    MemBarAcq  = 2,
    MemBarFenceAfter = 4 // use powers of 2
  };
 private:
  // Helper functions for word/sub-word atomics.
  void atomic_get_and_modify_generic(Register dest_current_value, Register exchange_value,
                                     Register addr_base, Register tmp1, Register tmp2, Register tmp3,
                                     bool cmpxchgx_hint, bool is_add, int size);
  void cmpxchg_loop_body(ConditionRegister flag, Register dest_current_value,
                         Register compare_value, Register exchange_value,
                         Register addr_base, Register tmp1, Register tmp2,
                         Label &retry, Label &failed, bool cmpxchgx_hint, int size);
  void cmpxchg_generic(ConditionRegister flag,
                       Register dest_current_value, Register compare_value, Register exchange_value, Register addr_base,
                       Register tmp1, Register tmp2,
                       int semantics, bool cmpxchgx_hint, Register int_flag_success, bool contention_hint, bool weak, int size);
 public:
  // Temps and addr_base are killed if processor does not support Power 8 instructions.
  // Result will be sign extended.
  void getandsetb(Register dest_current_value, Register exchange_value, Register addr_base,
                  Register tmp1, Register tmp2, Register tmp3, bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, exchange_value, addr_base, tmp1, tmp2, tmp3, cmpxchgx_hint, false, 1);
  }
  // Temps and addr_base are killed if processor does not support Power 8 instructions.
  // Result will be sign extended.
  void getandseth(Register dest_current_value, Register exchange_value, Register addr_base,
                  Register tmp1, Register tmp2, Register tmp3, bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, exchange_value, addr_base, tmp1, tmp2, tmp3, cmpxchgx_hint, false, 2);
  }
  void getandsetw(Register dest_current_value, Register exchange_value, Register addr_base,
                  bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, exchange_value, addr_base, noreg, noreg, noreg, cmpxchgx_hint, false, 4);
  }
  void getandsetd(Register dest_current_value, Register exchange_value, Register addr_base,
                  bool cmpxchgx_hint);
  // tmp2/3 and addr_base are killed if processor does not support Power 8 instructions (tmp1 is always needed).
  // Result will be sign extended.
  void getandaddb(Register dest_current_value, Register inc_value, Register addr_base,
                  Register tmp1, Register tmp2, Register tmp3, bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, inc_value, addr_base, tmp1, tmp2, tmp3, cmpxchgx_hint, true, 1);
  }
  // tmp2/3 and addr_base are killed if processor does not support Power 8 instructions (tmp1 is always needed).
  // Result will be sign extended.
  void getandaddh(Register dest_current_value, Register inc_value, Register addr_base,
                  Register tmp1, Register tmp2, Register tmp3, bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, inc_value, addr_base, tmp1, tmp2, tmp3, cmpxchgx_hint, true, 2);
  }
  void getandaddw(Register dest_current_value, Register inc_value, Register addr_base,
                  Register tmp1, bool cmpxchgx_hint) {
    atomic_get_and_modify_generic(dest_current_value, inc_value, addr_base, tmp1, noreg, noreg, cmpxchgx_hint, true, 4);
  }
  void getandaddd(Register dest_current_value, Register exchange_value, Register addr_base,
                  Register tmp, bool cmpxchgx_hint);
  // Temps, addr_base and exchange_value are killed if processor does not support Power 8 instructions.
  // compare_value must be at least 32 bit sign extended. Result will be sign extended.
  void cmpxchgb(ConditionRegister flag,
                Register dest_current_value, Register compare_value, Register exchange_value, Register addr_base,
                Register tmp1, Register tmp2, int semantics, bool cmpxchgx_hint = false,
                Register int_flag_success = noreg, bool contention_hint = false, bool weak = false) {
    cmpxchg_generic(flag, dest_current_value, compare_value, exchange_value, addr_base, tmp1, tmp2,
                    semantics, cmpxchgx_hint, int_flag_success, contention_hint, weak, 1);
  }
  // Temps, addr_base and exchange_value are killed if processor does not support Power 8 instructions.
  // compare_value must be at least 32 bit sign extended. Result will be sign extended.
  void cmpxchgh(ConditionRegister flag,
                Register dest_current_value, Register compare_value, Register exchange_value, Register addr_base,
                Register tmp1, Register tmp2, int semantics, bool cmpxchgx_hint = false,
                Register int_flag_success = noreg, bool contention_hint = false, bool weak = false) {
    cmpxchg_generic(flag, dest_current_value, compare_value, exchange_value, addr_base, tmp1, tmp2,
                    semantics, cmpxchgx_hint, int_flag_success, contention_hint, weak, 2);
  }
  void cmpxchgw(ConditionRegister flag,
                Register dest_current_value, Register compare_value, Register exchange_value, Register addr_base,
                int semantics, bool cmpxchgx_hint = false,
                Register int_flag_success = noreg, bool contention_hint = false, bool weak = false) {
    cmpxchg_generic(flag, dest_current_value, compare_value, exchange_value, addr_base, noreg, noreg,
                    semantics, cmpxchgx_hint, int_flag_success, contention_hint, weak, 4);
  }
  void cmpxchgd(ConditionRegister flag,
                Register dest_current_value, RegisterOrConstant compare_value, Register exchange_value,
                Register addr_base, int semantics, bool cmpxchgx_hint = false,
                Register int_flag_success = noreg, Label* failed = NULL, bool contention_hint = false, bool weak = false);

  // interface method calling
  void lookup_interface_method(Register recv_klass,
                               Register intf_klass,
                               RegisterOrConstant itable_index,
                               Register method_result,
                               Register temp_reg, Register temp2_reg,
                               Label& no_such_interface,
                               bool return_method = true);

  // virtual method calling
  void lookup_virtual_method(Register recv_klass,
                             RegisterOrConstant vtable_index,
                             Register method_result);

  // Test sub_klass against super_klass, with fast and slow paths.

  // The fast path produces a tri-state answer: yes / no / maybe-slow.
  // One of the three labels can be NULL, meaning take the fall-through.
  // If super_check_offset is -1, the value is loaded up from super_klass.
  // No registers are killed, except temp_reg and temp2_reg.
  // If super_check_offset is not -1, temp2_reg is not used and can be noreg.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp1_reg,
                                     Register temp2_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     Label* L_slow_path = NULL, // default fall through
                                     RegisterOrConstant super_check_offset = RegisterOrConstant(-1));

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // The temp_reg can be noreg, if no temps are available.
  // It can also be sub_klass or super_klass, meaning it's OK to kill that one.
  // Updates the sub's secondary super cache as necessary.
  void check_klass_subtype_slow_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp1_reg,
                                     Register temp2_reg,
                                     Label* L_success = NULL,
                                     Register result_reg = noreg);

  // Simplified, combined version, good for typical uses.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp1_reg,
                           Register temp2_reg,
                           Label& L_success);

  void clinit_barrier(Register klass,
                      Register thread,
                      Label* L_fast_path = NULL,
                      Label* L_slow_path = NULL);

  // Method handle support (JSR 292).
  RegisterOrConstant argument_offset(RegisterOrConstant arg_slot, Register temp_reg, int extra_slot_offset = 0);

  // allocation (for C1)
  void eden_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void tlab_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void incr_allocated_bytes(RegisterOrConstant size_in_bytes, Register t1, Register t2);

  enum { trampoline_stub_size = 6 * 4 };
  address emit_trampoline_stub(int destination_toc_offset, int insts_call_instruction_offset, Register Rtoc = noreg);

  void atomic_inc_ptr(Register addr, Register result, int simm16 = 1);
  void atomic_ori_int(Register addr, Register result, int uimm16);

#if INCLUDE_RTM_OPT
  void rtm_counters_update(Register abort_status, Register rtm_counters);
  void branch_on_random_using_tb(Register tmp, int count, Label& brLabel);
  void rtm_abort_ratio_calculation(Register rtm_counters_reg, RTMLockingCounters* rtm_counters,
                                   Metadata* method_data);
  void rtm_profiling(Register abort_status_Reg, Register temp_Reg,
                     RTMLockingCounters* rtm_counters, Metadata* method_data, bool profile_rtm);
  void rtm_retry_lock_on_abort(Register retry_count, Register abort_status,
                               Label& retryLabel, Label* checkRetry = NULL);
  void rtm_retry_lock_on_busy(Register retry_count, Register owner_addr, Label& retryLabel);
  void rtm_stack_locking(ConditionRegister flag, Register obj, Register mark_word, Register tmp,
                         Register retry_on_abort_count,
                         RTMLockingCounters* stack_rtm_counters,
                         Metadata* method_data, bool profile_rtm,
                         Label& DONE_LABEL, Label& IsInflated);
  void rtm_inflated_locking(ConditionRegister flag, Register obj, Register mark_word, Register box,
                            Register retry_on_busy_count, Register retry_on_abort_count,
                            RTMLockingCounters* rtm_counters,
                            Metadata* method_data, bool profile_rtm,
                            Label& DONE_LABEL);
#endif

  void compiler_fast_lock_object(ConditionRegister flag, Register oop, Register box,
                                 Register tmp1, Register tmp2, Register tmp3,
                                 RTMLockingCounters* rtm_counters = NULL,
                                 RTMLockingCounters* stack_rtm_counters = NULL,
                                 Metadata* method_data = NULL,
                                 bool use_rtm = false, bool profile_rtm = false);

  void compiler_fast_unlock_object(ConditionRegister flag, Register oop, Register box,
                                   Register tmp1, Register tmp2, Register tmp3,
                                   bool use_rtm = false);

  // Check if safepoint requested and if so branch
  void safepoint_poll(Label& slow_path, Register temp, bool at_return, bool in_nmethod);

  void resolve_jobject(Register value, Register tmp1, Register tmp2,
                       MacroAssembler::PreservationLevel preservation_level);

  // Support for managing the JavaThread pointer (i.e.; the reference to
  // thread-local information).

  // Support for last Java frame (but use call_VM instead where possible):
  // access R16_thread->last_Java_sp.
  void set_last_Java_frame(Register last_java_sp, Register last_Java_pc);
  void reset_last_Java_frame(void);
  void set_top_ijava_frame_at_SP_as_last_Java_frame(Register sp, Register tmp1);

  // Read vm result from thread: oop_result = R16_thread->result;
  void get_vm_result  (Register oop_result);
  void get_vm_result_2(Register metadata_result);

  static bool needs_explicit_null_check(intptr_t offset);
  static bool uses_implicit_null_check(void* address);

  // Trap-instruction-based checks.
  // Range checks can be distinguished from zero checks as they check 32 bit,
  // zero checks all 64 bits (tw, td).
  inline void trap_null_check(Register a, trap_to_bits cmp = traptoEqual);
  static bool is_trap_null_check(int x) {
    return is_tdi(x, traptoEqual,               -1/*any reg*/, 0) ||
           is_tdi(x, traptoGreaterThanUnsigned, -1/*any reg*/, 0);
  }

  inline void trap_ic_miss_check(Register a, Register b);
  static bool is_trap_ic_miss_check(int x) {
    return is_td(x, traptoGreaterThanUnsigned | traptoLessThanUnsigned, -1/*any reg*/, -1/*any reg*/);
  }

  // Implicit or explicit null check, jumps to static address exception_entry.
  inline void null_check_throw(Register a, int offset, Register temp_reg, address exception_entry);
  inline void null_check(Register a, int offset, Label *Lis_null); // implicit only if Lis_null not provided

  // Access heap oop, handle encoding and GC barriers.
  // Some GC barriers call C so use needs_frame = true if an extra frame is needed at the current call site.
 private:
  inline void access_store_at(BasicType type, DecoratorSet decorators,
                              Register base, RegisterOrConstant ind_or_offs, Register val,
                              Register tmp1, Register tmp2, Register tmp3,
                              MacroAssembler::PreservationLevel preservation_level);
  inline void access_load_at(BasicType type, DecoratorSet decorators,
                             Register base, RegisterOrConstant ind_or_offs, Register dst,
                             Register tmp1, Register tmp2,
                             MacroAssembler::PreservationLevel preservation_level, Label *L_handle_null = NULL);

 public:
  // Specify tmp1 for better code in certain compressed oops cases. Specify Label to bail out on null oop.
  // tmp1, tmp2 and needs_frame are used with decorators ON_PHANTOM_OOP_REF or ON_WEAK_OOP_REF.
  inline void load_heap_oop(Register d, RegisterOrConstant offs, Register s1,
                            Register tmp1, Register tmp2,
                            MacroAssembler::PreservationLevel preservation_level,
                            DecoratorSet decorators = 0, Label *L_handle_null = NULL);

  inline void store_heap_oop(Register d, RegisterOrConstant offs, Register s1,
                             Register tmp1, Register tmp2, Register tmp3,
                             MacroAssembler::PreservationLevel preservation_level, DecoratorSet decorators = 0);

  // Encode/decode heap oop. Oop may not be null, else en/decoding goes wrong.
  // src == d allowed.
  inline Register encode_heap_oop_not_null(Register d, Register src = noreg);
  inline Register decode_heap_oop_not_null(Register d, Register src = noreg);

  // Null allowed.
  inline Register encode_heap_oop(Register d, Register src); // Prefer null check in GC barrier!
  inline void decode_heap_oop(Register d);

  // Load/Store klass oop from klass field. Compress.
  void load_klass(Register dst, Register src);
  void store_klass(Register dst_oop, Register klass, Register tmp = R0);
  void store_klass_gap(Register dst_oop, Register val = noreg); // Will store 0 if val not specified.

  void resolve_oop_handle(Register result, Register tmp1, Register tmp2,
                          MacroAssembler::PreservationLevel preservation_level);
  void resolve_weak_handle(Register result, Register tmp1, Register tmp2,
                           MacroAssembler::PreservationLevel preservation_level);
  void load_method_holder(Register holder, Register method);

  static int instr_size_for_decode_klass_not_null();
  void decode_klass_not_null(Register dst, Register src = noreg);
  Register encode_klass_not_null(Register dst, Register src = noreg);

  // SIGTRAP-based range checks for arrays.
  inline void trap_range_check_l(Register a, Register b);
  inline void trap_range_check_l(Register a, int si16);
  static bool is_trap_range_check_l(int x) {
    return (is_tw (x, traptoLessThanUnsigned, -1/*any reg*/, -1/*any reg*/) ||
            is_twi(x, traptoLessThanUnsigned, -1/*any reg*/)                  );
  }
  inline void trap_range_check_le(Register a, int si16);
  static bool is_trap_range_check_le(int x) {
    return is_twi(x, traptoEqual | traptoLessThanUnsigned, -1/*any reg*/);
  }
  inline void trap_range_check_g(Register a, int si16);
  static bool is_trap_range_check_g(int x) {
    return is_twi(x, traptoGreaterThanUnsigned, -1/*any reg*/);
  }
  inline void trap_range_check_ge(Register a, Register b);
  inline void trap_range_check_ge(Register a, int si16);
  static bool is_trap_range_check_ge(int x) {
    return (is_tw (x, traptoEqual | traptoGreaterThanUnsigned, -1/*any reg*/, -1/*any reg*/) ||
            is_twi(x, traptoEqual | traptoGreaterThanUnsigned, -1/*any reg*/)                  );
  }
  static bool is_trap_range_check(int x) {
    return is_trap_range_check_l(x) || is_trap_range_check_le(x) ||
           is_trap_range_check_g(x) || is_trap_range_check_ge(x);
  }

  void clear_memory_unrolled(Register base_ptr, int cnt_dwords, Register tmp = R0, int offset = 0);
  void clear_memory_constlen(Register base_ptr, int cnt_dwords, Register tmp = R0);
  void clear_memory_doubleword(Register base_ptr, Register cnt_dwords, Register tmp = R0, long const_cnt = -1);

  // Emitters for BigInteger.multiplyToLen intrinsic.
  inline void multiply64(Register dest_hi, Register dest_lo,
                         Register x, Register y);
  void add2_with_carry(Register dest_hi, Register dest_lo,
                       Register src1, Register src2);
  void multiply_64_x_64_loop(Register x, Register xstart, Register x_xstart,
                             Register y, Register y_idx, Register z,
                             Register carry, Register product_high, Register product,
                             Register idx, Register kdx, Register tmp);
  void multiply_add_128_x_128(Register x_xstart, Register y, Register z,
                              Register yz_idx, Register idx, Register carry,
                              Register product_high, Register product, Register tmp,
                              int offset);
  void multiply_128_x_128_loop(Register x_xstart,
                               Register y, Register z,
                               Register yz_idx, Register idx, Register carry,
                               Register product_high, Register product,
                               Register carry2, Register tmp);
  void muladd(Register out, Register in, Register offset, Register len, Register k,
              Register tmp1, Register tmp2, Register carry);
  void multiply_to_len(Register x, Register xlen,
                       Register y, Register ylen,
                       Register z, Register zlen,
                       Register tmp1, Register tmp2, Register tmp3, Register tmp4, Register tmp5,
                       Register tmp6, Register tmp7, Register tmp8, Register tmp9, Register tmp10,
                       Register tmp11, Register tmp12, Register tmp13);

  // Emitters for CRC32 calculation.
  // A note on invertCRC:
  //   Unfortunately, internal representation of crc differs between CRC32 and CRC32C.
  //   CRC32 holds it's current crc value in the externally visible representation.
  //   CRC32C holds it's current crc value in internal format, ready for updating.
  //   Thus, the crc value must be bit-flipped before updating it in the CRC32 case.
  //   In the CRC32C case, it must be bit-flipped when it is given to the outside world (getValue()).
  //   The bool invertCRC parameter indicates whether bit-flipping is required before updates.
  void load_reverse_32(Register dst, Register src);
  int  crc32_table_columns(Register table, Register tc0, Register tc1, Register tc2, Register tc3);
  void fold_byte_crc32(Register crc, Register val, Register table, Register tmp);
  void update_byte_crc32(Register crc, Register val, Register table);
  void update_byteLoop_crc32(Register crc, Register buf, Register len, Register table,
                             Register data, bool loopAlignment);
  void update_1word_crc32(Register crc, Register buf, Register table, int bufDisp, int bufInc,
                          Register t0,  Register t1,  Register t2,  Register t3,
                          Register tc0, Register tc1, Register tc2, Register tc3);
  void kernel_crc32_1word(Register crc, Register buf, Register len, Register table,
                          Register t0,  Register t1,  Register t2,  Register t3,
                          Register tc0, Register tc1, Register tc2, Register tc3,
                          bool invertCRC);
  void kernel_crc32_vpmsum(Register crc, Register buf, Register len, Register constants,
                           Register t0, Register t1, Register t2, Register t3, Register t4,
                           Register t5, Register t6, bool invertCRC);
  void kernel_crc32_vpmsum_aligned(Register crc, Register buf, Register len, Register constants,
                                   Register t0, Register t1, Register t2, Register t3, Register t4,
                                   Register t5, Register t6);
  // Version which internally decides what to use.
  void crc32(Register crc, Register buf, Register len, Register t0, Register t1, Register t2,
             Register t3, Register t4, Register t5, Register t6, Register t7, bool is_crc32c);

  void kernel_crc32_singleByteReg(Register crc, Register val, Register table,
                                  bool invertCRC);

  // SHA-2 auxiliary functions and public interfaces
 private:
  void sha256_deque(const VectorRegister src,
      const VectorRegister dst1, const VectorRegister dst2, const VectorRegister dst3);
  void sha256_load_h_vec(const VectorRegister a, const VectorRegister e, const Register hptr);
  void sha256_round(const VectorRegister* hs, const int total_hs, int& h_cnt, const VectorRegister kpw);
  void sha256_load_w_plus_k_vec(const Register buf_in, const VectorRegister* ws,
      const int total_ws, const Register k, const VectorRegister* kpws,
      const int total_kpws);
  void sha256_calc_4w(const VectorRegister w0, const VectorRegister w1,
      const VectorRegister w2, const VectorRegister w3, const VectorRegister kpw0,
      const VectorRegister kpw1, const VectorRegister kpw2, const VectorRegister kpw3,
      const Register j, const Register k);
  void sha256_update_sha_state(const VectorRegister a, const VectorRegister b,
      const VectorRegister c, const VectorRegister d, const VectorRegister e,
      const VectorRegister f, const VectorRegister g, const VectorRegister h,
      const Register hptr);

  void sha512_load_w_vec(const Register buf_in, const VectorRegister* ws, const int total_ws);
  void sha512_update_sha_state(const Register state, const VectorRegister* hs, const int total_hs);
  void sha512_round(const VectorRegister* hs, const int total_hs, int& h_cnt, const VectorRegister kpw);
  void sha512_load_h_vec(const Register state, const VectorRegister* hs, const int total_hs);
  void sha512_calc_2w(const VectorRegister w0, const VectorRegister w1,
      const VectorRegister w2, const VectorRegister w3,
      const VectorRegister w4, const VectorRegister w5,
      const VectorRegister w6, const VectorRegister w7,
      const VectorRegister kpw0, const VectorRegister kpw1, const Register j,
      const VectorRegister vRb, const Register k);

 public:
  void sha256(bool multi_block);
  void sha512(bool multi_block);

  void cache_wb(Address line);
  void cache_wbsync(bool is_presync);

  //
  // Debugging
  //

  // assert on cr0
  void asm_assert(bool check_equal, const char* msg);
  void asm_assert_eq(const char* msg) { asm_assert(true, msg); }
  void asm_assert_ne(const char* msg) { asm_assert(false, msg); }

 private:
  void asm_assert_mems_zero(bool check_equal, int size, int mem_offset, Register mem_base,
                            const char* msg);

 public:

  void asm_assert_mem8_is_zero(int mem_offset, Register mem_base, const char* msg) {
    asm_assert_mems_zero(true,  8, mem_offset, mem_base, msg);
  }
  void asm_assert_mem8_isnot_zero(int mem_offset, Register mem_base, const char* msg) {
    asm_assert_mems_zero(false, 8, mem_offset, mem_base, msg);
  }

  // Verify R16_thread contents.
  void verify_thread();

  // Calls verify_oop. If UseCompressedOops is on, decodes the oop.
  // Preserves reg.
  void verify_coop(Register reg, const char*);
  // Emit code to verify that reg contains a valid oop if +VerifyOops is set.
  void verify_oop(Register reg, const char* s = "broken oop");
  void verify_oop_addr(RegisterOrConstant offs, Register base, const char* s = "contains broken oop");

  // TODO: verify method and klass metadata (compare against vptr?)
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line) {}

  // Convenience method returning function entry. For the ELFv1 case
  // creates function descriptor at the current address and returs
  // the pointer to it. For the ELFv2 case returns the current address.
  inline address function_entry();

#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

 private:
  void stop(int type, const char* msg);

 public:
  enum {
    stop_stop               = 0,
    stop_untested           = 1,
    stop_unimplemented      = 2,
    stop_shouldnotreachhere = 3,
    stop_msg_present        = -0x8000
  };

  // Prints msg, dumps registers and stops execution.
  void stop                 (const char* msg = NULL) { stop(stop_stop,               msg); }
  void untested             (const char* msg = NULL) { stop(stop_untested,           msg); }
  void unimplemented        (const char* msg = NULL) { stop(stop_unimplemented,      msg); }
  void should_not_reach_here(const char* msg = NULL) { stop(stop_shouldnotreachhere, msg); }

  void zap_from_to(Register low, int before, Register high, int after, Register val, Register addr) PRODUCT_RETURN;
};

// class SkipIfEqualZero:
//
// Instantiating this class will result in assembly code being output that will
// jump around any code emitted between the creation of the instance and it's
// automatic destruction at the end of a scope block, depending on the value of
// the flag passed to the constructor, which will be checked at run-time.
class SkipIfEqualZero : public StackObj {
 private:
  MacroAssembler* _masm;
  Label _label;

 public:
   // 'Temp' is a temp register that this object can use (and trash).
   explicit SkipIfEqualZero(MacroAssembler*, Register temp, const bool* flag_addr);
   static void skip_to_label_if_equal_zero(MacroAssembler*, Register temp,
                                           const bool* flag_addr, Label& label);
   ~SkipIfEqualZero();
};

#endif // CPU_PPC_MACROASSEMBLER_PPC_HPP
