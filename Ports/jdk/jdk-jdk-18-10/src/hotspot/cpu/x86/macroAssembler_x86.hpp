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

#ifndef CPU_X86_MACROASSEMBLER_X86_HPP
#define CPU_X86_MACROASSEMBLER_X86_HPP

#include "asm/assembler.hpp"
#include "code/vmreg.inline.hpp"
#include "compiler/oopMap.hpp"
#include "utilities/macros.hpp"
#include "runtime/rtmLocking.hpp"
#include "runtime/vm_version.hpp"

// MacroAssembler extends Assembler by frequently used macros.
//
// Instructions for which a 'better' code sequence exists depending
// on arguments should also go in here.

class MacroAssembler: public Assembler {
  friend class LIR_Assembler;
  friend class Runtime1;      // as_Address()

 public:
  // Support for VM calls
  //
  // This is the base routine called by the different versions of call_VM_leaf. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).

  virtual void call_VM_leaf_base(
    address entry_point,               // the entry point
    int     number_of_arguments        // the number of arguments to pop after the call
  );

 protected:
  // This is the base routine called by the different versions of call_VM. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  //
  // If no java_thread register is specified (noreg) than rdi will be used instead. call_VM_base
  // returns the register which contains the thread upon return. If a thread register has been
  // specified, the return value will correspond to that register. If no last_java_sp is specified
  // (noreg) than rsp will be used instead.
  virtual void call_VM_base(           // returns the register containing the thread upon return
    Register oop_result,               // where an oop-result ends up if any; use noreg otherwise
    Register java_thread,              // the thread if computed before     ; use noreg otherwise
    Register last_java_sp,             // to set up last_Java_frame in stubs; use noreg otherwise
    address  entry_point,              // the entry point
    int      number_of_arguments,      // the number of arguments (w/o thread) to pop after the call
    bool     check_exceptions          // whether to check for pending exceptions after return
  );

  void call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions = true);

  // helpers for FPU flag access
  // tmp is a temporary register, if none is available use noreg
  void save_rax   (Register tmp);
  void restore_rax(Register tmp);

 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

 // These routines should emit JVMTI PopFrame and ForceEarlyReturn handling code.
 // The implementation is only non-empty for the InterpreterMacroAssembler,
 // as only the interpreter handles PopFrame and ForceEarlyReturn requests.
 virtual void check_and_handle_popframe(Register java_thread);
 virtual void check_and_handle_earlyret(Register java_thread);

  Address as_Address(AddressLiteral adr);
  Address as_Address(ArrayAddress adr);

  // Support for NULL-checks
  //
  // Generates code that causes a NULL OS exception if the content of reg is NULL.
  // If the accessed location is M[reg + offset] and the offset is known, provide the
  // offset. No explicit code generation is needed if the offset is within a certain
  // range (0 <= offset <= page_size).

  void null_check(Register reg, int offset = -1);
  static bool needs_explicit_null_check(intptr_t offset);
  static bool uses_implicit_null_check(void* address);

  // Required platform-specific helpers for Label::patch_instructions.
  // They _shadow_ the declarations in AbstractAssembler, which are undefined.
  void pd_patch_instruction(address branch, address target, const char* file, int line) {
    unsigned char op = branch[0];
    assert(op == 0xE8 /* call */ ||
        op == 0xE9 /* jmp */ ||
        op == 0xEB /* short jmp */ ||
        (op & 0xF0) == 0x70 /* short jcc */ ||
        op == 0x0F && (branch[1] & 0xF0) == 0x80 /* jcc */ ||
        op == 0xC7 && branch[1] == 0xF8 /* xbegin */,
        "Invalid opcode at patch point");

    if (op == 0xEB || (op & 0xF0) == 0x70) {
      // short offset operators (jmp and jcc)
      char* disp = (char*) &branch[1];
      int imm8 = target - (address) &disp[1];
      guarantee(this->is8bit(imm8), "Short forward jump exceeds 8-bit offset at %s:%d",
                file == NULL ? "<NULL>" : file, line);
      *disp = imm8;
    } else {
      int* disp = (int*) &branch[(op == 0x0F || op == 0xC7)? 2: 1];
      int imm32 = target - (address) &disp[1];
      *disp = imm32;
    }
  }

  // The following 4 methods return the offset of the appropriate move instruction

  // Support for fast byte/short loading with zero extension (depending on particular CPU)
  int load_unsigned_byte(Register dst, Address src);
  int load_unsigned_short(Register dst, Address src);

  // Support for fast byte/short loading with sign extension (depending on particular CPU)
  int load_signed_byte(Register dst, Address src);
  int load_signed_short(Register dst, Address src);

  // Support for sign-extension (hi:lo = extend_sign(lo))
  void extend_sign(Register hi, Register lo);

  // Load and store values by size and signed-ness
  void load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed, Register dst2 = noreg);
  void store_sized_value(Address dst, Register src, size_t size_in_bytes, Register src2 = noreg);

  // Support for inc/dec with optimal instruction selection depending on value

  void increment(Register reg, int value = 1) { LP64_ONLY(incrementq(reg, value)) NOT_LP64(incrementl(reg, value)) ; }
  void decrement(Register reg, int value = 1) { LP64_ONLY(decrementq(reg, value)) NOT_LP64(decrementl(reg, value)) ; }

  void decrementl(Address dst, int value = 1);
  void decrementl(Register reg, int value = 1);

  void decrementq(Register reg, int value = 1);
  void decrementq(Address dst, int value = 1);

  void incrementl(Address dst, int value = 1);
  void incrementl(Register reg, int value = 1);

  void incrementq(Register reg, int value = 1);
  void incrementq(Address dst, int value = 1);

  // Support optimal SSE move instructions.
  void movflt(XMMRegister dst, XMMRegister src) {
    if (dst-> encoding() == src->encoding()) return;
    if (UseXmmRegToRegMoveAll) { movaps(dst, src); return; }
    else                       { movss (dst, src); return; }
  }
  void movflt(XMMRegister dst, Address src) { movss(dst, src); }
  void movflt(XMMRegister dst, AddressLiteral src);
  void movflt(Address dst, XMMRegister src) { movss(dst, src); }

  // Move with zero extension
  void movfltz(XMMRegister dst, XMMRegister src) { movss(dst, src); }

  void movdbl(XMMRegister dst, XMMRegister src) {
    if (dst-> encoding() == src->encoding()) return;
    if (UseXmmRegToRegMoveAll) { movapd(dst, src); return; }
    else                       { movsd (dst, src); return; }
  }

  void movdbl(XMMRegister dst, AddressLiteral src);

  void movdbl(XMMRegister dst, Address src) {
    if (UseXmmLoadAndClearUpper) { movsd (dst, src); return; }
    else                         { movlpd(dst, src); return; }
  }
  void movdbl(Address dst, XMMRegister src) { movsd(dst, src); }

  void incrementl(AddressLiteral dst);
  void incrementl(ArrayAddress dst);

  void incrementq(AddressLiteral dst);

  // Alignment
  void align(int modulus);
  void align(int modulus, int target);

  // A 5 byte nop that is safe for patching (see patch_verified_entry)
  void fat_nop();

  // Stack frame creation/removal
  void enter();
  void leave();

  // Support for getting the JavaThread pointer (i.e.; a reference to thread-local information)
  // The pointer will be loaded into the thread register.
  void get_thread(Register thread);

#ifdef _LP64
  // Support for argument shuffling

  void move32_64(VMRegPair src, VMRegPair dst);
  void long_move(VMRegPair src, VMRegPair dst);
  void float_move(VMRegPair src, VMRegPair dst);
  void double_move(VMRegPair src, VMRegPair dst);
  void move_ptr(VMRegPair src, VMRegPair dst);
  void object_move(OopMap* map,
                   int oop_handle_offset,
                   int framesize_in_slots,
                   VMRegPair src,
                   VMRegPair dst,
                   bool is_receiver,
                   int* receiver_offset);
#endif // _LP64

  // Support for VM calls
  //
  // It is imperative that all calls into the VM are handled via the call_VM macros.
  // They make sure that the stack linkage is setup correctly. call_VM's correspond
  // to ENTRY/ENTRY_X entry points while call_VM_leaf's correspond to LEAF entry points.


  void call_VM(Register oop_result,
               address entry_point,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  // Overloadings with last_Java_sp
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               int number_of_arguments = 0,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, bool
               check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  void get_vm_result  (Register oop_result, Register thread);
  void get_vm_result_2(Register metadata_result, Register thread);

  // These always tightly bind to MacroAssembler::call_VM_base
  // bypassing the virtual implementation
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, int number_of_arguments = 0, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4, bool check_exceptions = true);

  void call_VM_leaf0(address entry_point);
  void call_VM_leaf(address entry_point,
                    int number_of_arguments = 0);
  void call_VM_leaf(address entry_point,
                    Register arg_1);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2, Register arg_3);

  // These always tightly bind to MacroAssembler::call_VM_leaf_base
  // bypassing the virtual implementation
  void super_call_VM_leaf(address entry_point);
  void super_call_VM_leaf(address entry_point, Register arg_1);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4);

  // last Java Frame (fills frame anchor)
  void set_last_Java_frame(Register thread,
                           Register last_java_sp,
                           Register last_java_fp,
                           address last_java_pc);

  // thread in the default location (r15_thread on 64bit)
  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           address last_java_pc);

  void reset_last_Java_frame(Register thread, bool clear_fp);

  // thread in the default location (r15_thread on 64bit)
  void reset_last_Java_frame(bool clear_fp);

  // jobjects
  void clear_jweak_tag(Register possibly_jweak);
  void resolve_jobject(Register value, Register thread, Register tmp);

  // C 'boolean' to Java boolean: x == 0 ? 0 : 1
  void c2bool(Register x);

  // C++ bool manipulation

  void movbool(Register dst, Address src);
  void movbool(Address dst, bool boolconst);
  void movbool(Address dst, Register src);
  void testbool(Register dst);

  void resolve_oop_handle(Register result, Register tmp = rscratch2);
  void resolve_weak_handle(Register result, Register tmp);
  void load_mirror(Register mirror, Register method, Register tmp = rscratch2);
  void load_method_holder_cld(Register rresult, Register rmethod);

  void load_method_holder(Register holder, Register method);

  // oop manipulations
  void load_klass(Register dst, Register src, Register tmp);
  void store_klass(Register dst, Register src, Register tmp);

  void access_load_at(BasicType type, DecoratorSet decorators, Register dst, Address src,
                      Register tmp1, Register thread_tmp);
  void access_store_at(BasicType type, DecoratorSet decorators, Address dst, Register src,
                       Register tmp1, Register tmp2);

  void load_heap_oop(Register dst, Address src, Register tmp1 = noreg,
                     Register thread_tmp = noreg, DecoratorSet decorators = 0);
  void load_heap_oop_not_null(Register dst, Address src, Register tmp1 = noreg,
                              Register thread_tmp = noreg, DecoratorSet decorators = 0);
  void store_heap_oop(Address dst, Register src, Register tmp1 = noreg,
                      Register tmp2 = noreg, DecoratorSet decorators = 0);

  // Used for storing NULL. All other oop constants should be
  // stored using routines that take a jobject.
  void store_heap_oop_null(Address dst);

#ifdef _LP64
  void store_klass_gap(Register dst, Register src);

  // This dummy is to prevent a call to store_heap_oop from
  // converting a zero (like NULL) into a Register by giving
  // the compiler two choices it can't resolve

  void store_heap_oop(Address dst, void* dummy);

  void encode_heap_oop(Register r);
  void decode_heap_oop(Register r);
  void encode_heap_oop_not_null(Register r);
  void decode_heap_oop_not_null(Register r);
  void encode_heap_oop_not_null(Register dst, Register src);
  void decode_heap_oop_not_null(Register dst, Register src);

  void set_narrow_oop(Register dst, jobject obj);
  void set_narrow_oop(Address dst, jobject obj);
  void cmp_narrow_oop(Register dst, jobject obj);
  void cmp_narrow_oop(Address dst, jobject obj);

  void encode_klass_not_null(Register r, Register tmp);
  void decode_klass_not_null(Register r, Register tmp);
  void encode_and_move_klass_not_null(Register dst, Register src);
  void decode_and_move_klass_not_null(Register dst, Register src);
  void set_narrow_klass(Register dst, Klass* k);
  void set_narrow_klass(Address dst, Klass* k);
  void cmp_narrow_klass(Register dst, Klass* k);
  void cmp_narrow_klass(Address dst, Klass* k);

  // if heap base register is used - reinit it with the correct value
  void reinit_heapbase();

  DEBUG_ONLY(void verify_heapbase(const char* msg);)

#endif // _LP64

  // Int division/remainder for Java
  // (as idivl, but checks for special case as described in JVM spec.)
  // returns idivl instruction offset for implicit exception handling
  int corrected_idivl(Register reg);

  // Long division/remainder for Java
  // (as idivq, but checks for special case as described in JVM spec.)
  // returns idivq instruction offset for implicit exception handling
  int corrected_idivq(Register reg);

  void int3();

  // Long operation macros for a 32bit cpu
  // Long negation for Java
  void lneg(Register hi, Register lo);

  // Long multiplication for Java
  // (destroys contents of eax, ebx, ecx and edx)
  void lmul(int x_rsp_offset, int y_rsp_offset); // rdx:rax = x * y

  // Long shifts for Java
  // (semantics as described in JVM spec.)
  void lshl(Register hi, Register lo);                               // hi:lo << (rcx & 0x3f)
  void lshr(Register hi, Register lo, bool sign_extension = false);  // hi:lo >> (rcx & 0x3f)

  // Long compare for Java
  // (semantics as described in JVM spec.)
  void lcmp2int(Register x_hi, Register x_lo, Register y_hi, Register y_lo); // x_hi = lcmp(x, y)


  // misc

  // Sign extension
  void sign_extend_short(Register reg);
  void sign_extend_byte(Register reg);

  // Division by power of 2, rounding towards 0
  void division_with_shift(Register reg, int shift_value);

#ifndef _LP64
  // Compares the top-most stack entries on the FPU stack and sets the eflags as follows:
  //
  // CF (corresponds to C0) if x < y
  // PF (corresponds to C2) if unordered
  // ZF (corresponds to C3) if x = y
  //
  // The arguments are in reversed order on the stack (i.e., top of stack is first argument).
  // tmp is a temporary register, if none is available use noreg (only matters for non-P6 code)
  void fcmp(Register tmp);
  // Variant of the above which allows y to be further down the stack
  // and which only pops x and y if specified. If pop_right is
  // specified then pop_left must also be specified.
  void fcmp(Register tmp, int index, bool pop_left, bool pop_right);

  // Floating-point comparison for Java
  // Compares the top-most stack entries on the FPU stack and stores the result in dst.
  // The arguments are in reversed order on the stack (i.e., top of stack is first argument).
  // (semantics as described in JVM spec.)
  void fcmp2int(Register dst, bool unordered_is_less);
  // Variant of the above which allows y to be further down the stack
  // and which only pops x and y if specified. If pop_right is
  // specified then pop_left must also be specified.
  void fcmp2int(Register dst, bool unordered_is_less, int index, bool pop_left, bool pop_right);

  // Floating-point remainder for Java (ST0 = ST0 fremr ST1, ST1 is empty afterwards)
  // tmp is a temporary register, if none is available use noreg
  void fremr(Register tmp);

  // only if +VerifyFPU
  void verify_FPU(int stack_depth, const char* s = "illegal FPU state");
#endif // !LP64

  // dst = c = a * b + c
  void fmad(XMMRegister dst, XMMRegister a, XMMRegister b, XMMRegister c);
  void fmaf(XMMRegister dst, XMMRegister a, XMMRegister b, XMMRegister c);

  void vfmad(XMMRegister dst, XMMRegister a, XMMRegister b, XMMRegister c, int vector_len);
  void vfmaf(XMMRegister dst, XMMRegister a, XMMRegister b, XMMRegister c, int vector_len);
  void vfmad(XMMRegister dst, XMMRegister a, Address b, XMMRegister c, int vector_len);
  void vfmaf(XMMRegister dst, XMMRegister a, Address b, XMMRegister c, int vector_len);


  // same as fcmp2int, but using SSE2
  void cmpss2int(XMMRegister opr1, XMMRegister opr2, Register dst, bool unordered_is_less);
  void cmpsd2int(XMMRegister opr1, XMMRegister opr2, Register dst, bool unordered_is_less);

  // branch to L if FPU flag C2 is set/not set
  // tmp is a temporary register, if none is available use noreg
  void jC2 (Register tmp, Label& L);
  void jnC2(Register tmp, Label& L);

  // Load float value from 'address'. If UseSSE >= 1, the value is loaded into
  // register xmm0. Otherwise, the value is loaded onto the FPU stack.
  void load_float(Address src);

  // Store float value to 'address'. If UseSSE >= 1, the value is stored
  // from register xmm0. Otherwise, the value is stored from the FPU stack.
  void store_float(Address dst);

  // Load double value from 'address'. If UseSSE >= 2, the value is loaded into
  // register xmm0. Otherwise, the value is loaded onto the FPU stack.
  void load_double(Address src);

  // Store double value to 'address'. If UseSSE >= 2, the value is stored
  // from register xmm0. Otherwise, the value is stored from the FPU stack.
  void store_double(Address dst);

#ifndef _LP64
  // Pop ST (ffree & fincstp combined)
  void fpop();

  void empty_FPU_stack();
#endif // !_LP64

  void push_IU_state();
  void pop_IU_state();

  void push_FPU_state();
  void pop_FPU_state();

  void push_CPU_state();
  void pop_CPU_state();

  // Round up to a power of two
  void round_to(Register reg, int modulus);

  // Callee saved registers handling
  void push_callee_saved_registers();
  void pop_callee_saved_registers();

  // allocation
  void eden_allocate(
    Register thread,                   // Current thread
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void tlab_allocate(
    Register thread,                   // Current thread
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void zero_memory(Register address, Register length_in_bytes, int offset_in_bytes, Register temp);

  // interface method calling
  void lookup_interface_method(Register recv_klass,
                               Register intf_klass,
                               RegisterOrConstant itable_index,
                               Register method_result,
                               Register scan_temp,
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
  // No registers are killed, except temp_reg.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     Label* L_slow_path,
                RegisterOrConstant super_check_offset = RegisterOrConstant(-1));

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // The temp_reg and temp2_reg can be noreg, if no temps are available.
  // Updates the sub's secondary super cache as necessary.
  // If set_cond_codes, condition codes will be Z on success, NZ on failure.
  void check_klass_subtype_slow_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Register temp2_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     bool set_cond_codes = false);

  // Simplified, combined version, good for typical uses.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp_reg,
                           Label& L_success);

  void clinit_barrier(Register klass,
                      Register thread,
                      Label* L_fast_path = NULL,
                      Label* L_slow_path = NULL);

  // method handles (JSR 292)
  Address argument_address(RegisterOrConstant arg_slot, int extra_slot_offset = 0);

  // Debugging

  // only if +VerifyOops
  void _verify_oop(Register reg, const char* s, const char* file, int line);
  void _verify_oop_addr(Address addr, const char* s, const char* file, int line);

  void _verify_oop_checked(Register reg, const char* s, const char* file, int line) {
    if (VerifyOops) {
      _verify_oop(reg, s, file, line);
    }
  }
  void _verify_oop_addr_checked(Address reg, const char* s, const char* file, int line) {
    if (VerifyOops) {
      _verify_oop_addr(reg, s, file, line);
    }
  }

  // TODO: verify method and klass metadata (compare against vptr?)
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line){}

#define verify_oop(reg) _verify_oop_checked(reg, "broken oop " #reg, __FILE__, __LINE__)
#define verify_oop_msg(reg, msg) _verify_oop_checked(reg, "broken oop " #reg ", " #msg, __FILE__, __LINE__)
#define verify_oop_addr(addr) _verify_oop_addr_checked(addr, "broken oop addr " #addr, __FILE__, __LINE__)
#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

  // Verify or restore cpu control state after JNI call
  void restore_cpu_control_state_after_jni();

  // prints msg, dumps registers and stops execution
  void stop(const char* msg);

  // prints msg and continues
  void warn(const char* msg);

  // dumps registers and other state
  void print_state();

  static void debug32(int rdi, int rsi, int rbp, int rsp, int rbx, int rdx, int rcx, int rax, int eip, char* msg);
  static void debug64(char* msg, int64_t pc, int64_t regs[]);
  static void print_state32(int rdi, int rsi, int rbp, int rsp, int rbx, int rdx, int rcx, int rax, int eip);
  static void print_state64(int64_t pc, int64_t regs[]);

  void os_breakpoint();

  void untested()                                { stop("untested"); }

  void unimplemented(const char* what = "");

  void should_not_reach_here()                   { stop("should not reach here"); }

  void print_CPU_state();

  // Stack overflow checking
  void bang_stack_with_offset(int offset) {
    // stack grows down, caller passes positive offset
    assert(offset > 0, "must bang with negative offset");
    movl(Address(rsp, (-offset)), rax);
  }

  // Writes to stack successive pages until offset reached to check for
  // stack overflow + shadow pages.  Also, clobbers tmp
  void bang_stack_size(Register size, Register tmp);

  // Check for reserved stack access in method being exited (for JIT)
  void reserved_stack_check();

  void safepoint_poll(Label& slow_path, Register thread_reg, bool at_return, bool in_nmethod);

  void verify_tlab();

  Condition negate_condition(Condition cond);

  // Instructions that use AddressLiteral operands. These instruction can handle 32bit/64bit
  // operands. In general the names are modified to avoid hiding the instruction in Assembler
  // so that we don't need to implement all the varieties in the Assembler with trivial wrappers
  // here in MacroAssembler. The major exception to this rule is call

  // Arithmetics


  void addptr(Address dst, int32_t src) { LP64_ONLY(addq(dst, src)) NOT_LP64(addl(dst, src)) ; }
  void addptr(Address dst, Register src);

  void addptr(Register dst, Address src) { LP64_ONLY(addq(dst, src)) NOT_LP64(addl(dst, src)); }
  void addptr(Register dst, int32_t src);
  void addptr(Register dst, Register src);
  void addptr(Register dst, RegisterOrConstant src) {
    if (src.is_constant()) addptr(dst, (int) src.as_constant());
    else                   addptr(dst,       src.as_register());
  }

  void andptr(Register dst, int32_t src);
  void andptr(Register src1, Register src2) { LP64_ONLY(andq(src1, src2)) NOT_LP64(andl(src1, src2)) ; }

  void cmp8(AddressLiteral src1, int imm);

  // renamed to drag out the casting of address to int32_t/intptr_t
  void cmp32(Register src1, int32_t imm);

  void cmp32(AddressLiteral src1, int32_t imm);
  // compare reg - mem, or reg - &mem
  void cmp32(Register src1, AddressLiteral src2);

  void cmp32(Register src1, Address src2);

#ifndef _LP64
  void cmpklass(Address dst, Metadata* obj);
  void cmpklass(Register dst, Metadata* obj);
  void cmpoop(Address dst, jobject obj);
#endif // _LP64

  void cmpoop(Register src1, Register src2);
  void cmpoop(Register src1, Address src2);
  void cmpoop(Register dst, jobject obj);

  // NOTE src2 must be the lval. This is NOT an mem-mem compare
  void cmpptr(Address src1, AddressLiteral src2);

  void cmpptr(Register src1, AddressLiteral src2);

  void cmpptr(Register src1, Register src2) { LP64_ONLY(cmpq(src1, src2)) NOT_LP64(cmpl(src1, src2)) ; }
  void cmpptr(Register src1, Address src2) { LP64_ONLY(cmpq(src1, src2)) NOT_LP64(cmpl(src1, src2)) ; }
  // void cmpptr(Address src1, Register src2) { LP64_ONLY(cmpq(src1, src2)) NOT_LP64(cmpl(src1, src2)) ; }

  void cmpptr(Register src1, int32_t src2) { LP64_ONLY(cmpq(src1, src2)) NOT_LP64(cmpl(src1, src2)) ; }
  void cmpptr(Address src1, int32_t src2) { LP64_ONLY(cmpq(src1, src2)) NOT_LP64(cmpl(src1, src2)) ; }

  // cmp64 to avoild hiding cmpq
  void cmp64(Register src1, AddressLiteral src);

  void cmpxchgptr(Register reg, Address adr);

  void locked_cmpxchgptr(Register reg, AddressLiteral adr);


  void imulptr(Register dst, Register src) { LP64_ONLY(imulq(dst, src)) NOT_LP64(imull(dst, src)); }
  void imulptr(Register dst, Register src, int imm32) { LP64_ONLY(imulq(dst, src, imm32)) NOT_LP64(imull(dst, src, imm32)); }


  void negptr(Register dst) { LP64_ONLY(negq(dst)) NOT_LP64(negl(dst)); }

  void notptr(Register dst) { LP64_ONLY(notq(dst)) NOT_LP64(notl(dst)); }

  void shlptr(Register dst, int32_t shift);
  void shlptr(Register dst) { LP64_ONLY(shlq(dst)) NOT_LP64(shll(dst)); }

  void shrptr(Register dst, int32_t shift);
  void shrptr(Register dst) { LP64_ONLY(shrq(dst)) NOT_LP64(shrl(dst)); }

  void sarptr(Register dst) { LP64_ONLY(sarq(dst)) NOT_LP64(sarl(dst)); }
  void sarptr(Register dst, int32_t src) { LP64_ONLY(sarq(dst, src)) NOT_LP64(sarl(dst, src)); }

  void subptr(Address dst, int32_t src) { LP64_ONLY(subq(dst, src)) NOT_LP64(subl(dst, src)); }

  void subptr(Register dst, Address src) { LP64_ONLY(subq(dst, src)) NOT_LP64(subl(dst, src)); }
  void subptr(Register dst, int32_t src);
  // Force generation of a 4 byte immediate value even if it fits into 8bit
  void subptr_imm32(Register dst, int32_t src);
  void subptr(Register dst, Register src);
  void subptr(Register dst, RegisterOrConstant src) {
    if (src.is_constant()) subptr(dst, (int) src.as_constant());
    else                   subptr(dst,       src.as_register());
  }

  void sbbptr(Address dst, int32_t src) { LP64_ONLY(sbbq(dst, src)) NOT_LP64(sbbl(dst, src)); }
  void sbbptr(Register dst, int32_t src) { LP64_ONLY(sbbq(dst, src)) NOT_LP64(sbbl(dst, src)); }

  void xchgptr(Register src1, Register src2) { LP64_ONLY(xchgq(src1, src2)) NOT_LP64(xchgl(src1, src2)) ; }
  void xchgptr(Register src1, Address src2) { LP64_ONLY(xchgq(src1, src2)) NOT_LP64(xchgl(src1, src2)) ; }

  void xaddptr(Address src1, Register src2) { LP64_ONLY(xaddq(src1, src2)) NOT_LP64(xaddl(src1, src2)) ; }



  // Helper functions for statistics gathering.
  // Conditionally (atomically, on MPs) increments passed counter address, preserving condition codes.
  void cond_inc32(Condition cond, AddressLiteral counter_addr);
  // Unconditional atomic increment.
  void atomic_incl(Address counter_addr);
  void atomic_incl(AddressLiteral counter_addr, Register scr = rscratch1);
#ifdef _LP64
  void atomic_incq(Address counter_addr);
  void atomic_incq(AddressLiteral counter_addr, Register scr = rscratch1);
#endif
  void atomic_incptr(AddressLiteral counter_addr, Register scr = rscratch1) { LP64_ONLY(atomic_incq(counter_addr, scr)) NOT_LP64(atomic_incl(counter_addr, scr)) ; }
  void atomic_incptr(Address counter_addr) { LP64_ONLY(atomic_incq(counter_addr)) NOT_LP64(atomic_incl(counter_addr)) ; }

  void lea(Register dst, AddressLiteral adr);
  void lea(Address dst, AddressLiteral adr);
  void lea(Register dst, Address adr) { Assembler::lea(dst, adr); }

  void leal32(Register dst, Address src) { leal(dst, src); }

  // Import other testl() methods from the parent class or else
  // they will be hidden by the following overriding declaration.
  using Assembler::testl;
  void testl(Register dst, AddressLiteral src);

  void orptr(Register dst, Address src) { LP64_ONLY(orq(dst, src)) NOT_LP64(orl(dst, src)); }
  void orptr(Register dst, Register src) { LP64_ONLY(orq(dst, src)) NOT_LP64(orl(dst, src)); }
  void orptr(Register dst, int32_t src) { LP64_ONLY(orq(dst, src)) NOT_LP64(orl(dst, src)); }
  void orptr(Address dst, int32_t imm32) { LP64_ONLY(orq(dst, imm32)) NOT_LP64(orl(dst, imm32)); }

  void testptr(Register src, int32_t imm32) {  LP64_ONLY(testq(src, imm32)) NOT_LP64(testl(src, imm32)); }
  void testptr(Register src1, Address src2) { LP64_ONLY(testq(src1, src2)) NOT_LP64(testl(src1, src2)); }
  void testptr(Register src1, Register src2);

  void xorptr(Register dst, Register src) { LP64_ONLY(xorq(dst, src)) NOT_LP64(xorl(dst, src)); }
  void xorptr(Register dst, Address src) { LP64_ONLY(xorq(dst, src)) NOT_LP64(xorl(dst, src)); }

  // Calls

  void call(Label& L, relocInfo::relocType rtype);
  void call(Register entry);
  void call(Address addr) { Assembler::call(addr); }

  // NOTE: this call transfers to the effective address of entry NOT
  // the address contained by entry. This is because this is more natural
  // for jumps/calls.
  void call(AddressLiteral entry);

  // Emit the CompiledIC call idiom
  void ic_call(address entry, jint method_index = 0);

  // Jumps

  // NOTE: these jumps tranfer to the effective address of dst NOT
  // the address contained by dst. This is because this is more natural
  // for jumps/calls.
  void jump(AddressLiteral dst);
  void jump_cc(Condition cc, AddressLiteral dst);

  // 32bit can do a case table jump in one instruction but we no longer allow the base
  // to be installed in the Address class. This jump will tranfers to the address
  // contained in the location described by entry (not the address of entry)
  void jump(ArrayAddress entry);

  // Floating

  void andpd(XMMRegister dst, Address src) { Assembler::andpd(dst, src); }
  void andpd(XMMRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);
  void andpd(XMMRegister dst, XMMRegister src) { Assembler::andpd(dst, src); }

  void andps(XMMRegister dst, XMMRegister src) { Assembler::andps(dst, src); }
  void andps(XMMRegister dst, Address src) { Assembler::andps(dst, src); }
  void andps(XMMRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);

  void comiss(XMMRegister dst, XMMRegister src) { Assembler::comiss(dst, src); }
  void comiss(XMMRegister dst, Address src) { Assembler::comiss(dst, src); }
  void comiss(XMMRegister dst, AddressLiteral src);

  void comisd(XMMRegister dst, XMMRegister src) { Assembler::comisd(dst, src); }
  void comisd(XMMRegister dst, Address src) { Assembler::comisd(dst, src); }
  void comisd(XMMRegister dst, AddressLiteral src);

#ifndef _LP64
  void fadd_s(Address src)        { Assembler::fadd_s(src); }
  void fadd_s(AddressLiteral src) { Assembler::fadd_s(as_Address(src)); }

  void fldcw(Address src) { Assembler::fldcw(src); }
  void fldcw(AddressLiteral src);

  void fld_s(int index)   { Assembler::fld_s(index); }
  void fld_s(Address src) { Assembler::fld_s(src); }
  void fld_s(AddressLiteral src);

  void fld_d(Address src) { Assembler::fld_d(src); }
  void fld_d(AddressLiteral src);

  void fmul_s(Address src)        { Assembler::fmul_s(src); }
  void fmul_s(AddressLiteral src) { Assembler::fmul_s(as_Address(src)); }
#endif // _LP64

  void fld_x(Address src) { Assembler::fld_x(src); }
  void fld_x(AddressLiteral src);

  void ldmxcsr(Address src) { Assembler::ldmxcsr(src); }
  void ldmxcsr(AddressLiteral src);

#ifdef _LP64
 private:
  void sha256_AVX2_one_round_compute(
    Register  reg_old_h,
    Register  reg_a,
    Register  reg_b,
    Register  reg_c,
    Register  reg_d,
    Register  reg_e,
    Register  reg_f,
    Register  reg_g,
    Register  reg_h,
    int iter);
  void sha256_AVX2_four_rounds_compute_first(int start);
  void sha256_AVX2_four_rounds_compute_last(int start);
  void sha256_AVX2_one_round_and_sched(
        XMMRegister xmm_0,     /* == ymm4 on 0, 1, 2, 3 iterations, then rotate 4 registers left on 4, 8, 12 iterations */
        XMMRegister xmm_1,     /* ymm5 */  /* full cycle is 16 iterations */
        XMMRegister xmm_2,     /* ymm6 */
        XMMRegister xmm_3,     /* ymm7 */
        Register    reg_a,      /* == eax on 0 iteration, then rotate 8 register right on each next iteration */
        Register    reg_b,      /* ebx */    /* full cycle is 8 iterations */
        Register    reg_c,      /* edi */
        Register    reg_d,      /* esi */
        Register    reg_e,      /* r8d */
        Register    reg_f,      /* r9d */
        Register    reg_g,      /* r10d */
        Register    reg_h,      /* r11d */
        int iter);

  void addm(int disp, Register r1, Register r2);
  void gfmul(XMMRegister tmp0, XMMRegister t);
  void schoolbookAAD(int i, Register subkeyH, XMMRegister data, XMMRegister tmp0,
                     XMMRegister tmp1, XMMRegister tmp2, XMMRegister tmp3);
  void generateHtbl_one_block(Register htbl);
  void generateHtbl_eight_blocks(Register htbl);
 public:
  void sha256_AVX2(XMMRegister msg, XMMRegister state0, XMMRegister state1, XMMRegister msgtmp0,
                   XMMRegister msgtmp1, XMMRegister msgtmp2, XMMRegister msgtmp3, XMMRegister msgtmp4,
                   Register buf, Register state, Register ofs, Register limit, Register rsp,
                   bool multi_block, XMMRegister shuf_mask);
  void avx_ghash(Register state, Register htbl, Register data, Register blocks);
#endif

#ifdef _LP64
 private:
  void sha512_AVX2_one_round_compute(Register old_h, Register a, Register b, Register c, Register d,
                                     Register e, Register f, Register g, Register h, int iteration);

  void sha512_AVX2_one_round_and_schedule(XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                                          Register a, Register b, Register c, Register d, Register e, Register f,
                                          Register g, Register h, int iteration);

  void addmq(int disp, Register r1, Register r2);
 public:
  void sha512_AVX2(XMMRegister msg, XMMRegister state0, XMMRegister state1, XMMRegister msgtmp0,
                   XMMRegister msgtmp1, XMMRegister msgtmp2, XMMRegister msgtmp3, XMMRegister msgtmp4,
                   Register buf, Register state, Register ofs, Register limit, Register rsp, bool multi_block,
                   XMMRegister shuf_mask);
private:
  void roundEnc(XMMRegister key, int rnum);
  void lastroundEnc(XMMRegister key, int rnum);
  void roundDec(XMMRegister key, int rnum);
  void lastroundDec(XMMRegister key, int rnum);
  void ev_load_key(XMMRegister xmmdst, Register key, int offset, XMMRegister xmm_shuf_mask);

public:
  void aesecb_encrypt(Register source_addr, Register dest_addr, Register key, Register len);
  void aesecb_decrypt(Register source_addr, Register dest_addr, Register key, Register len);
  void aesctr_encrypt(Register src_addr, Register dest_addr, Register key, Register counter,
                      Register len_reg, Register used, Register used_addr, Register saved_encCounter_start);

#endif

  void fast_md5(Register buf, Address state, Address ofs, Address limit,
                bool multi_block);

  void fast_sha1(XMMRegister abcd, XMMRegister e0, XMMRegister e1, XMMRegister msg0,
                 XMMRegister msg1, XMMRegister msg2, XMMRegister msg3, XMMRegister shuf_mask,
                 Register buf, Register state, Register ofs, Register limit, Register rsp,
                 bool multi_block);

#ifdef _LP64
  void fast_sha256(XMMRegister msg, XMMRegister state0, XMMRegister state1, XMMRegister msgtmp0,
                   XMMRegister msgtmp1, XMMRegister msgtmp2, XMMRegister msgtmp3, XMMRegister msgtmp4,
                   Register buf, Register state, Register ofs, Register limit, Register rsp,
                   bool multi_block, XMMRegister shuf_mask);
#else
  void fast_sha256(XMMRegister msg, XMMRegister state0, XMMRegister state1, XMMRegister msgtmp0,
                   XMMRegister msgtmp1, XMMRegister msgtmp2, XMMRegister msgtmp3, XMMRegister msgtmp4,
                   Register buf, Register state, Register ofs, Register limit, Register rsp,
                   bool multi_block);
#endif

  void fast_exp(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp);

#ifdef _LP64
  void fast_log(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp1, Register tmp2);

  void fast_log10(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                  XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                  Register rax, Register rcx, Register rdx, Register r11);

  void fast_pow(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4,
                XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register rax, Register rcx,
                Register rdx, Register tmp1, Register tmp2, Register tmp3, Register tmp4);

  void fast_sin(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rbx, Register rcx, Register rdx, Register tmp1, Register tmp2,
                Register tmp3, Register tmp4);

  void fast_cos(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp1,
                Register tmp2, Register tmp3, Register tmp4);
  void fast_tan(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp1,
                Register tmp2, Register tmp3, Register tmp4);
#else
  void fast_log(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp1);

  void fast_log10(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp);

  void fast_pow(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3, XMMRegister xmm4,
                XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7, Register rax, Register rcx,
                Register rdx, Register tmp);

  void fast_sin(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rbx, Register rdx);

  void fast_cos(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp);

  void libm_sincos_huge(XMMRegister xmm0, XMMRegister xmm1, Register eax, Register ecx,
                        Register edx, Register ebx, Register esi, Register edi,
                        Register ebp, Register esp);

  void libm_reduce_pi04l(Register eax, Register ecx, Register edx, Register ebx,
                         Register esi, Register edi, Register ebp, Register esp);

  void libm_tancot_huge(XMMRegister xmm0, XMMRegister xmm1, Register eax, Register ecx,
                        Register edx, Register ebx, Register esi, Register edi,
                        Register ebp, Register esp);

  void fast_tan(XMMRegister xmm0, XMMRegister xmm1, XMMRegister xmm2, XMMRegister xmm3,
                XMMRegister xmm4, XMMRegister xmm5, XMMRegister xmm6, XMMRegister xmm7,
                Register rax, Register rcx, Register rdx, Register tmp);
#endif

private:

  // these are private because users should be doing movflt/movdbl

  void movss(XMMRegister dst, XMMRegister src) { Assembler::movss(dst, src); }
  void movss(Address dst, XMMRegister src)     { Assembler::movss(dst, src); }
  void movss(XMMRegister dst, Address src)     { Assembler::movss(dst, src); }
  void movss(XMMRegister dst, AddressLiteral src);

  void movlpd(XMMRegister dst, Address src)    {Assembler::movlpd(dst, src); }
  void movlpd(XMMRegister dst, AddressLiteral src);

public:

  void addsd(XMMRegister dst, XMMRegister src)    { Assembler::addsd(dst, src); }
  void addsd(XMMRegister dst, Address src)        { Assembler::addsd(dst, src); }
  void addsd(XMMRegister dst, AddressLiteral src);

  void addss(XMMRegister dst, XMMRegister src)    { Assembler::addss(dst, src); }
  void addss(XMMRegister dst, Address src)        { Assembler::addss(dst, src); }
  void addss(XMMRegister dst, AddressLiteral src);

  void addpd(XMMRegister dst, XMMRegister src)    { Assembler::addpd(dst, src); }
  void addpd(XMMRegister dst, Address src)        { Assembler::addpd(dst, src); }
  void addpd(XMMRegister dst, AddressLiteral src);

  void divsd(XMMRegister dst, XMMRegister src)    { Assembler::divsd(dst, src); }
  void divsd(XMMRegister dst, Address src)        { Assembler::divsd(dst, src); }
  void divsd(XMMRegister dst, AddressLiteral src);

  void divss(XMMRegister dst, XMMRegister src)    { Assembler::divss(dst, src); }
  void divss(XMMRegister dst, Address src)        { Assembler::divss(dst, src); }
  void divss(XMMRegister dst, AddressLiteral src);

  // Move Unaligned Double Quadword
  void movdqu(Address     dst, XMMRegister src);
  void movdqu(XMMRegister dst, Address src);
  void movdqu(XMMRegister dst, XMMRegister src);
  void movdqu(XMMRegister dst, AddressLiteral src, Register scratchReg = rscratch1);

  void kmovwl(KRegister dst, Register src) { Assembler::kmovwl(dst, src); }
  void kmovwl(Register dst, KRegister src) { Assembler::kmovwl(dst, src); }
  void kmovwl(KRegister dst, Address src) { Assembler::kmovwl(dst, src); }
  void kmovwl(KRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);
  void kmovwl(Address dst,  KRegister src) { Assembler::kmovwl(dst, src); }
  void kmovwl(KRegister dst, KRegister src) { Assembler::kmovwl(dst, src); }

  void kmovql(KRegister dst, KRegister src) { Assembler::kmovql(dst, src); }
  void kmovql(KRegister dst, Register src) { Assembler::kmovql(dst, src); }
  void kmovql(Register dst, KRegister src) { Assembler::kmovql(dst, src); }
  void kmovql(KRegister dst, Address src) { Assembler::kmovql(dst, src); }
  void kmovql(Address  dst, KRegister src) { Assembler::kmovql(dst, src); }
  void kmovql(KRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);

  // Safe move operation, lowers down to 16bit moves for targets supporting
  // AVX512F feature and 64bit moves for targets supporting AVX512BW feature.
  void kmov(Address  dst, KRegister src);
  void kmov(KRegister dst, Address src);
  void kmov(KRegister dst, KRegister src);
  void kmov(Register dst, KRegister src);
  void kmov(KRegister dst, Register src);

  // AVX Unaligned forms
  void vmovdqu(Address     dst, XMMRegister src);
  void vmovdqu(XMMRegister dst, Address src);
  void vmovdqu(XMMRegister dst, XMMRegister src);
  void vmovdqu(XMMRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);

  // AVX512 Unaligned
  void evmovdqu(BasicType type, KRegister kmask, Address dst, XMMRegister src, int vector_len);
  void evmovdqu(BasicType type, KRegister kmask, XMMRegister dst, Address src, int vector_len);

  void evmovdqub(Address dst, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdqub(dst, src, merge, vector_len); }
  void evmovdqub(XMMRegister dst, Address src, bool merge, int vector_len) { Assembler::evmovdqub(dst, src, merge, vector_len); }
  void evmovdqub(XMMRegister dst, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdqub(dst, src, merge, vector_len); }
  void evmovdqub(XMMRegister dst, KRegister mask, Address src, bool merge, int vector_len) { Assembler::evmovdqub(dst, mask, src, merge, vector_len); }
  void evmovdqub(Address dst, KRegister mask, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdqub(dst, mask, src, merge, vector_len); }
  void evmovdqub(XMMRegister dst, KRegister mask, AddressLiteral src, bool merge, int vector_len, Register scratch_reg);

  void evmovdquw(Address dst, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdquw(dst, src, merge, vector_len); }
  void evmovdquw(Address dst, KRegister mask, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdquw(dst, mask, src, merge, vector_len); }
  void evmovdquw(XMMRegister dst, Address src, bool merge, int vector_len) { Assembler::evmovdquw(dst, src, merge, vector_len); }
  void evmovdquw(XMMRegister dst, KRegister mask, Address src, bool merge, int vector_len) { Assembler::evmovdquw(dst, mask, src, merge, vector_len); }
  void evmovdquw(XMMRegister dst, KRegister mask, AddressLiteral src, bool merge, int vector_len, Register scratch_reg);

  void evmovdqul(Address dst, XMMRegister src, int vector_len) { Assembler::evmovdqul(dst, src, vector_len); }
  void evmovdqul(XMMRegister dst, Address src, int vector_len) { Assembler::evmovdqul(dst, src, vector_len); }
  void evmovdqul(XMMRegister dst, XMMRegister src, int vector_len) {
     if (dst->encoding() == src->encoding()) return;
     Assembler::evmovdqul(dst, src, vector_len);
  }
  void evmovdqul(Address dst, KRegister mask, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdqul(dst, mask, src, merge, vector_len); }
  void evmovdqul(XMMRegister dst, KRegister mask, Address src, bool merge, int vector_len) { Assembler::evmovdqul(dst, mask, src, merge, vector_len); }
  void evmovdqul(XMMRegister dst, KRegister mask, XMMRegister src, bool merge, int vector_len) {
    if (dst->encoding() == src->encoding() && mask == k0) return;
    Assembler::evmovdqul(dst, mask, src, merge, vector_len);
   }
  void evmovdqul(XMMRegister dst, KRegister mask, AddressLiteral src, bool merge, int vector_len, Register scratch_reg);

  void evmovdquq(XMMRegister dst, Address src, int vector_len) { Assembler::evmovdquq(dst, src, vector_len); }
  void evmovdquq(Address dst, XMMRegister src, int vector_len) { Assembler::evmovdquq(dst, src, vector_len); }
  void evmovdquq(XMMRegister dst, AddressLiteral src, int vector_len, Register rscratch);
  void evmovdquq(XMMRegister dst, XMMRegister src, int vector_len) {
    if (dst->encoding() == src->encoding()) return;
    Assembler::evmovdquq(dst, src, vector_len);
  }
  void evmovdquq(Address dst, KRegister mask, XMMRegister src, bool merge, int vector_len) { Assembler::evmovdquq(dst, mask, src, merge, vector_len); }
  void evmovdquq(XMMRegister dst, KRegister mask, Address src, bool merge, int vector_len) { Assembler::evmovdquq(dst, mask, src, merge, vector_len); }
  void evmovdquq(XMMRegister dst, KRegister mask, XMMRegister src, bool merge, int vector_len) {
    if (dst->encoding() == src->encoding() && mask == k0) return;
    Assembler::evmovdquq(dst, mask, src, merge, vector_len);
  }
  void evmovdquq(XMMRegister dst, KRegister mask, AddressLiteral src, bool merge, int vector_len, Register scratch_reg);

  // Move Aligned Double Quadword
  void movdqa(XMMRegister dst, Address src)       { Assembler::movdqa(dst, src); }
  void movdqa(XMMRegister dst, XMMRegister src)   { Assembler::movdqa(dst, src); }
  void movdqa(XMMRegister dst, AddressLiteral src);

  void movsd(XMMRegister dst, XMMRegister src) { Assembler::movsd(dst, src); }
  void movsd(Address dst, XMMRegister src)     { Assembler::movsd(dst, src); }
  void movsd(XMMRegister dst, Address src)     { Assembler::movsd(dst, src); }
  void movsd(XMMRegister dst, AddressLiteral src);

  void mulpd(XMMRegister dst, XMMRegister src)    { Assembler::mulpd(dst, src); }
  void mulpd(XMMRegister dst, Address src)        { Assembler::mulpd(dst, src); }
  void mulpd(XMMRegister dst, AddressLiteral src);

  void mulsd(XMMRegister dst, XMMRegister src)    { Assembler::mulsd(dst, src); }
  void mulsd(XMMRegister dst, Address src)        { Assembler::mulsd(dst, src); }
  void mulsd(XMMRegister dst, AddressLiteral src);

  void mulss(XMMRegister dst, XMMRegister src)    { Assembler::mulss(dst, src); }
  void mulss(XMMRegister dst, Address src)        { Assembler::mulss(dst, src); }
  void mulss(XMMRegister dst, AddressLiteral src);

  // Carry-Less Multiplication Quadword
  void pclmulldq(XMMRegister dst, XMMRegister src) {
    // 0x00 - multiply lower 64 bits [0:63]
    Assembler::pclmulqdq(dst, src, 0x00);
  }
  void pclmulhdq(XMMRegister dst, XMMRegister src) {
    // 0x11 - multiply upper 64 bits [64:127]
    Assembler::pclmulqdq(dst, src, 0x11);
  }

  void pcmpeqb(XMMRegister dst, XMMRegister src);
  void pcmpeqw(XMMRegister dst, XMMRegister src);

  void pcmpestri(XMMRegister dst, Address src, int imm8);
  void pcmpestri(XMMRegister dst, XMMRegister src, int imm8);

  void pmovzxbw(XMMRegister dst, XMMRegister src);
  void pmovzxbw(XMMRegister dst, Address src);

  void pmovmskb(Register dst, XMMRegister src);

  void ptest(XMMRegister dst, XMMRegister src);

  void sqrtsd(XMMRegister dst, XMMRegister src)    { Assembler::sqrtsd(dst, src); }
  void sqrtsd(XMMRegister dst, Address src)        { Assembler::sqrtsd(dst, src); }
  void sqrtsd(XMMRegister dst, AddressLiteral src);

  void roundsd(XMMRegister dst, XMMRegister src, int32_t rmode)    { Assembler::roundsd(dst, src, rmode); }
  void roundsd(XMMRegister dst, Address src, int32_t rmode)        { Assembler::roundsd(dst, src, rmode); }
  void roundsd(XMMRegister dst, AddressLiteral src, int32_t rmode, Register scratch_reg);

  void sqrtss(XMMRegister dst, XMMRegister src)    { Assembler::sqrtss(dst, src); }
  void sqrtss(XMMRegister dst, Address src)        { Assembler::sqrtss(dst, src); }
  void sqrtss(XMMRegister dst, AddressLiteral src);

  void subsd(XMMRegister dst, XMMRegister src)    { Assembler::subsd(dst, src); }
  void subsd(XMMRegister dst, Address src)        { Assembler::subsd(dst, src); }
  void subsd(XMMRegister dst, AddressLiteral src);

  void subss(XMMRegister dst, XMMRegister src)    { Assembler::subss(dst, src); }
  void subss(XMMRegister dst, Address src)        { Assembler::subss(dst, src); }
  void subss(XMMRegister dst, AddressLiteral src);

  void ucomiss(XMMRegister dst, XMMRegister src) { Assembler::ucomiss(dst, src); }
  void ucomiss(XMMRegister dst, Address src)     { Assembler::ucomiss(dst, src); }
  void ucomiss(XMMRegister dst, AddressLiteral src);

  void ucomisd(XMMRegister dst, XMMRegister src) { Assembler::ucomisd(dst, src); }
  void ucomisd(XMMRegister dst, Address src)     { Assembler::ucomisd(dst, src); }
  void ucomisd(XMMRegister dst, AddressLiteral src);

  // Bitwise Logical XOR of Packed Double-Precision Floating-Point Values
  void xorpd(XMMRegister dst, XMMRegister src);
  void xorpd(XMMRegister dst, Address src)     { Assembler::xorpd(dst, src); }
  void xorpd(XMMRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);

  // Bitwise Logical XOR of Packed Single-Precision Floating-Point Values
  void xorps(XMMRegister dst, XMMRegister src);
  void xorps(XMMRegister dst, Address src)     { Assembler::xorps(dst, src); }
  void xorps(XMMRegister dst, AddressLiteral src, Register scratch_reg = rscratch1);

  // Shuffle Bytes
  void pshufb(XMMRegister dst, XMMRegister src) { Assembler::pshufb(dst, src); }
  void pshufb(XMMRegister dst, Address src)     { Assembler::pshufb(dst, src); }
  void pshufb(XMMRegister dst, AddressLiteral src);
  // AVX 3-operands instructions

  void vaddsd(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vaddsd(dst, nds, src); }
  void vaddsd(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vaddsd(dst, nds, src); }
  void vaddsd(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vaddss(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vaddss(dst, nds, src); }
  void vaddss(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vaddss(dst, nds, src); }
  void vaddss(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vabsss(XMMRegister dst, XMMRegister nds, XMMRegister src, AddressLiteral negate_field, int vector_len);
  void vabssd(XMMRegister dst, XMMRegister nds, XMMRegister src, AddressLiteral negate_field, int vector_len);

  void vpaddb(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void vpaddb(XMMRegister dst, XMMRegister nds, Address src, int vector_len);
  void vpaddb(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register rscratch);

  void vpaddw(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void vpaddw(XMMRegister dst, XMMRegister nds, Address src, int vector_len);

  void vpaddd(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vpaddd(dst, nds, src, vector_len); }
  void vpaddd(XMMRegister dst, XMMRegister nds, Address src, int vector_len) { Assembler::vpaddd(dst, nds, src, vector_len); }
  void vpaddd(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register rscratch);

  void vpand(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vpand(dst, nds, src, vector_len); }
  void vpand(XMMRegister dst, XMMRegister nds, Address src, int vector_len) { Assembler::vpand(dst, nds, src, vector_len); }
  void vpand(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  void vpbroadcastw(XMMRegister dst, XMMRegister src, int vector_len);
  void vpbroadcastw(XMMRegister dst, Address src, int vector_len) { Assembler::vpbroadcastw(dst, src, vector_len); }

  void vpcmpeqb(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);

  void vpcmpeqw(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void evpcmpeqd(KRegister kdst, KRegister mask, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg);

  // Vector compares
  void evpcmpd(KRegister kdst, KRegister mask, XMMRegister nds, XMMRegister src,
               int comparison, bool is_signed, int vector_len) { Assembler::evpcmpd(kdst, mask, nds, src, comparison, is_signed, vector_len); }
  void evpcmpd(KRegister kdst, KRegister mask, XMMRegister nds, AddressLiteral src,
               int comparison, bool is_signed, int vector_len, Register scratch_reg);
  void evpcmpq(KRegister kdst, KRegister mask, XMMRegister nds, XMMRegister src,
               int comparison, bool is_signed, int vector_len) { Assembler::evpcmpq(kdst, mask, nds, src, comparison, is_signed, vector_len); }
  void evpcmpq(KRegister kdst, KRegister mask, XMMRegister nds, AddressLiteral src,
               int comparison, bool is_signed, int vector_len, Register scratch_reg);
  void evpcmpb(KRegister kdst, KRegister mask, XMMRegister nds, XMMRegister src,
               int comparison, bool is_signed, int vector_len) { Assembler::evpcmpb(kdst, mask, nds, src, comparison, is_signed, vector_len); }
  void evpcmpb(KRegister kdst, KRegister mask, XMMRegister nds, AddressLiteral src,
               int comparison, bool is_signed, int vector_len, Register scratch_reg);
  void evpcmpw(KRegister kdst, KRegister mask, XMMRegister nds, XMMRegister src,
               int comparison, bool is_signed, int vector_len) { Assembler::evpcmpw(kdst, mask, nds, src, comparison, is_signed, vector_len); }
  void evpcmpw(KRegister kdst, KRegister mask, XMMRegister nds, AddressLiteral src,
               int comparison, bool is_signed, int vector_len, Register scratch_reg);


  // Emit comparison instruction for the specified comparison predicate.
  void vpcmpCCW(XMMRegister dst, XMMRegister nds, XMMRegister src, ComparisonPredicate cond, Width width, int vector_len, Register scratch_reg);
  void vpcmpCC(XMMRegister dst, XMMRegister nds, XMMRegister src, int cond_encoding, Width width, int vector_len);

  void vpmovzxbw(XMMRegister dst, Address src, int vector_len);
  void vpmovzxbw(XMMRegister dst, XMMRegister src, int vector_len) { Assembler::vpmovzxbw(dst, src, vector_len); }

  void vpmovmskb(Register dst, XMMRegister src, int vector_len = Assembler::AVX_256bit);

  void vpmullw(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void vpmullw(XMMRegister dst, XMMRegister nds, Address src, int vector_len);
  void vpmulld(XMMRegister dst, XMMRegister nds, Address src, int vector_len) {
    Assembler::vpmulld(dst, nds, src, vector_len);
  };
  void vpmulld(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) {
    Assembler::vpmulld(dst, nds, src, vector_len);
  }
  void vpmulld(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg);

  void vpsubb(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void vpsubb(XMMRegister dst, XMMRegister nds, Address src, int vector_len);

  void vpsubw(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len);
  void vpsubw(XMMRegister dst, XMMRegister nds, Address src, int vector_len);

  void vpsraw(XMMRegister dst, XMMRegister nds, XMMRegister shift, int vector_len);
  void vpsraw(XMMRegister dst, XMMRegister nds, int shift, int vector_len);

  void evpsraq(XMMRegister dst, XMMRegister nds, XMMRegister shift, int vector_len);
  void evpsraq(XMMRegister dst, XMMRegister nds, int shift, int vector_len);

  void vpsrlw(XMMRegister dst, XMMRegister nds, XMMRegister shift, int vector_len);
  void vpsrlw(XMMRegister dst, XMMRegister nds, int shift, int vector_len);

  void vpsllw(XMMRegister dst, XMMRegister nds, XMMRegister shift, int vector_len);
  void vpsllw(XMMRegister dst, XMMRegister nds, int shift, int vector_len);

  void vptest(XMMRegister dst, XMMRegister src);
  void vptest(XMMRegister dst, XMMRegister src, int vector_len) { Assembler::vptest(dst, src, vector_len); }

  void punpcklbw(XMMRegister dst, XMMRegister src);
  void punpcklbw(XMMRegister dst, Address src) { Assembler::punpcklbw(dst, src); }

  void pshufd(XMMRegister dst, Address src, int mode);
  void pshufd(XMMRegister dst, XMMRegister src, int mode) { Assembler::pshufd(dst, src, mode); }

  void pshuflw(XMMRegister dst, XMMRegister src, int mode);
  void pshuflw(XMMRegister dst, Address src, int mode) { Assembler::pshuflw(dst, src, mode); }

  void vandpd(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vandpd(dst, nds, src, vector_len); }
  void vandpd(XMMRegister dst, XMMRegister nds, Address src, int vector_len)     { Assembler::vandpd(dst, nds, src, vector_len); }
  void vandpd(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  void vandps(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vandps(dst, nds, src, vector_len); }
  void vandps(XMMRegister dst, XMMRegister nds, Address src, int vector_len)     { Assembler::vandps(dst, nds, src, vector_len); }
  void vandps(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  void evpord(XMMRegister dst, KRegister mask, XMMRegister nds, AddressLiteral src, bool merge, int vector_len, Register scratch_reg);

  void vdivsd(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vdivsd(dst, nds, src); }
  void vdivsd(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vdivsd(dst, nds, src); }
  void vdivsd(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vdivss(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vdivss(dst, nds, src); }
  void vdivss(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vdivss(dst, nds, src); }
  void vdivss(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vmulsd(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vmulsd(dst, nds, src); }
  void vmulsd(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vmulsd(dst, nds, src); }
  void vmulsd(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vmulss(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vmulss(dst, nds, src); }
  void vmulss(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vmulss(dst, nds, src); }
  void vmulss(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vsubsd(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vsubsd(dst, nds, src); }
  void vsubsd(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vsubsd(dst, nds, src); }
  void vsubsd(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vsubss(XMMRegister dst, XMMRegister nds, XMMRegister src) { Assembler::vsubss(dst, nds, src); }
  void vsubss(XMMRegister dst, XMMRegister nds, Address src)     { Assembler::vsubss(dst, nds, src); }
  void vsubss(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  void vnegatess(XMMRegister dst, XMMRegister nds, AddressLiteral src);
  void vnegatesd(XMMRegister dst, XMMRegister nds, AddressLiteral src);

  // AVX Vector instructions

  void vxorpd(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vxorpd(dst, nds, src, vector_len); }
  void vxorpd(XMMRegister dst, XMMRegister nds, Address src, int vector_len) { Assembler::vxorpd(dst, nds, src, vector_len); }
  void vxorpd(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  void vxorps(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vxorps(dst, nds, src, vector_len); }
  void vxorps(XMMRegister dst, XMMRegister nds, Address src, int vector_len) { Assembler::vxorps(dst, nds, src, vector_len); }
  void vxorps(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  void vpxor(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) {
    if (UseAVX > 1 || (vector_len < 1)) // vpxor 256 bit is available only in AVX2
      Assembler::vpxor(dst, nds, src, vector_len);
    else
      Assembler::vxorpd(dst, nds, src, vector_len);
  }
  void vpxor(XMMRegister dst, XMMRegister nds, Address src, int vector_len) {
    if (UseAVX > 1 || (vector_len < 1)) // vpxor 256 bit is available only in AVX2
      Assembler::vpxor(dst, nds, src, vector_len);
    else
      Assembler::vxorpd(dst, nds, src, vector_len);
  }
  void vpxor(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg = rscratch1);

  // Simple version for AVX2 256bit vectors
  void vpxor(XMMRegister dst, XMMRegister src) { Assembler::vpxor(dst, dst, src, true); }
  void vpxor(XMMRegister dst, Address src) { Assembler::vpxor(dst, dst, src, true); }

  void vpermd(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) { Assembler::vpermd(dst, nds, src, vector_len); }
  void vpermd(XMMRegister dst, XMMRegister nds, AddressLiteral src, int vector_len, Register scratch_reg);

  void vinserti128(XMMRegister dst, XMMRegister nds, XMMRegister src, uint8_t imm8) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinserti32x4(dst, nds, src, imm8);
    } else if (UseAVX > 1) {
      // vinserti128 is available only in AVX2
      Assembler::vinserti128(dst, nds, src, imm8);
    } else {
      Assembler::vinsertf128(dst, nds, src, imm8);
    }
  }

  void vinserti128(XMMRegister dst, XMMRegister nds, Address src, uint8_t imm8) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinserti32x4(dst, nds, src, imm8);
    } else if (UseAVX > 1) {
      // vinserti128 is available only in AVX2
      Assembler::vinserti128(dst, nds, src, imm8);
    } else {
      Assembler::vinsertf128(dst, nds, src, imm8);
    }
  }

  void vextracti128(XMMRegister dst, XMMRegister src, uint8_t imm8) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextracti32x4(dst, src, imm8);
    } else if (UseAVX > 1) {
      // vextracti128 is available only in AVX2
      Assembler::vextracti128(dst, src, imm8);
    } else {
      Assembler::vextractf128(dst, src, imm8);
    }
  }

  void vextracti128(Address dst, XMMRegister src, uint8_t imm8) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextracti32x4(dst, src, imm8);
    } else if (UseAVX > 1) {
      // vextracti128 is available only in AVX2
      Assembler::vextracti128(dst, src, imm8);
    } else {
      Assembler::vextractf128(dst, src, imm8);
    }
  }

  // 128bit copy to/from high 128 bits of 256bit (YMM) vector registers
  void vinserti128_high(XMMRegister dst, XMMRegister src) {
    vinserti128(dst, dst, src, 1);
  }
  void vinserti128_high(XMMRegister dst, Address src) {
    vinserti128(dst, dst, src, 1);
  }
  void vextracti128_high(XMMRegister dst, XMMRegister src) {
    vextracti128(dst, src, 1);
  }
  void vextracti128_high(Address dst, XMMRegister src) {
    vextracti128(dst, src, 1);
  }

  void vinsertf128_high(XMMRegister dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinsertf32x4(dst, dst, src, 1);
    } else {
      Assembler::vinsertf128(dst, dst, src, 1);
    }
  }

  void vinsertf128_high(XMMRegister dst, Address src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinsertf32x4(dst, dst, src, 1);
    } else {
      Assembler::vinsertf128(dst, dst, src, 1);
    }
  }

  void vextractf128_high(XMMRegister dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextractf32x4(dst, src, 1);
    } else {
      Assembler::vextractf128(dst, src, 1);
    }
  }

  void vextractf128_high(Address dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextractf32x4(dst, src, 1);
    } else {
      Assembler::vextractf128(dst, src, 1);
    }
  }

  // 256bit copy to/from high 256 bits of 512bit (ZMM) vector registers
  void vinserti64x4_high(XMMRegister dst, XMMRegister src) {
    Assembler::vinserti64x4(dst, dst, src, 1);
  }
  void vinsertf64x4_high(XMMRegister dst, XMMRegister src) {
    Assembler::vinsertf64x4(dst, dst, src, 1);
  }
  void vextracti64x4_high(XMMRegister dst, XMMRegister src) {
    Assembler::vextracti64x4(dst, src, 1);
  }
  void vextractf64x4_high(XMMRegister dst, XMMRegister src) {
    Assembler::vextractf64x4(dst, src, 1);
  }
  void vextractf64x4_high(Address dst, XMMRegister src) {
    Assembler::vextractf64x4(dst, src, 1);
  }
  void vinsertf64x4_high(XMMRegister dst, Address src) {
    Assembler::vinsertf64x4(dst, dst, src, 1);
  }

  // 128bit copy to/from low 128 bits of 256bit (YMM) vector registers
  void vinserti128_low(XMMRegister dst, XMMRegister src) {
    vinserti128(dst, dst, src, 0);
  }
  void vinserti128_low(XMMRegister dst, Address src) {
    vinserti128(dst, dst, src, 0);
  }
  void vextracti128_low(XMMRegister dst, XMMRegister src) {
    vextracti128(dst, src, 0);
  }
  void vextracti128_low(Address dst, XMMRegister src) {
    vextracti128(dst, src, 0);
  }

  void vinsertf128_low(XMMRegister dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinsertf32x4(dst, dst, src, 0);
    } else {
      Assembler::vinsertf128(dst, dst, src, 0);
    }
  }

  void vinsertf128_low(XMMRegister dst, Address src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vinsertf32x4(dst, dst, src, 0);
    } else {
      Assembler::vinsertf128(dst, dst, src, 0);
    }
  }

  void vextractf128_low(XMMRegister dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextractf32x4(dst, src, 0);
    } else {
      Assembler::vextractf128(dst, src, 0);
    }
  }

  void vextractf128_low(Address dst, XMMRegister src) {
    if (UseAVX > 2 && VM_Version::supports_avx512novl()) {
      Assembler::vextractf32x4(dst, src, 0);
    } else {
      Assembler::vextractf128(dst, src, 0);
    }
  }

  // 256bit copy to/from low 256 bits of 512bit (ZMM) vector registers
  void vinserti64x4_low(XMMRegister dst, XMMRegister src) {
    Assembler::vinserti64x4(dst, dst, src, 0);
  }
  void vinsertf64x4_low(XMMRegister dst, XMMRegister src) {
    Assembler::vinsertf64x4(dst, dst, src, 0);
  }
  void vextracti64x4_low(XMMRegister dst, XMMRegister src) {
    Assembler::vextracti64x4(dst, src, 0);
  }
  void vextractf64x4_low(XMMRegister dst, XMMRegister src) {
    Assembler::vextractf64x4(dst, src, 0);
  }
  void vextractf64x4_low(Address dst, XMMRegister src) {
    Assembler::vextractf64x4(dst, src, 0);
  }
  void vinsertf64x4_low(XMMRegister dst, Address src) {
    Assembler::vinsertf64x4(dst, dst, src, 0);
  }

  // Carry-Less Multiplication Quadword
  void vpclmulldq(XMMRegister dst, XMMRegister nds, XMMRegister src) {
    // 0x00 - multiply lower 64 bits [0:63]
    Assembler::vpclmulqdq(dst, nds, src, 0x00);
  }
  void vpclmulhdq(XMMRegister dst, XMMRegister nds, XMMRegister src) {
    // 0x11 - multiply upper 64 bits [64:127]
    Assembler::vpclmulqdq(dst, nds, src, 0x11);
  }
  void vpclmullqhqdq(XMMRegister dst, XMMRegister nds, XMMRegister src) {
    // 0x10 - multiply nds[0:63] and src[64:127]
    Assembler::vpclmulqdq(dst, nds, src, 0x10);
  }
  void vpclmulhqlqdq(XMMRegister dst, XMMRegister nds, XMMRegister src) {
    //0x01 - multiply nds[64:127] and src[0:63]
    Assembler::vpclmulqdq(dst, nds, src, 0x01);
  }

  void evpclmulldq(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) {
    // 0x00 - multiply lower 64 bits [0:63]
    Assembler::evpclmulqdq(dst, nds, src, 0x00, vector_len);
  }
  void evpclmulhdq(XMMRegister dst, XMMRegister nds, XMMRegister src, int vector_len) {
    // 0x11 - multiply upper 64 bits [64:127]
    Assembler::evpclmulqdq(dst, nds, src, 0x11, vector_len);
  }

  // Data

  void cmov32( Condition cc, Register dst, Address  src);
  void cmov32( Condition cc, Register dst, Register src);

  void cmov(   Condition cc, Register dst, Register src) { cmovptr(cc, dst, src); }

  void cmovptr(Condition cc, Register dst, Address  src) { LP64_ONLY(cmovq(cc, dst, src)) NOT_LP64(cmov32(cc, dst, src)); }
  void cmovptr(Condition cc, Register dst, Register src) { LP64_ONLY(cmovq(cc, dst, src)) NOT_LP64(cmov32(cc, dst, src)); }

  void movoop(Register dst, jobject obj);
  void movoop(Address dst, jobject obj);

  void mov_metadata(Register dst, Metadata* obj);
  void mov_metadata(Address dst, Metadata* obj);

  void movptr(ArrayAddress dst, Register src);
  // can this do an lea?
  void movptr(Register dst, ArrayAddress src);

  void movptr(Register dst, Address src);

#ifdef _LP64
  void movptr(Register dst, AddressLiteral src, Register scratch=rscratch1);
#else
  void movptr(Register dst, AddressLiteral src, Register scratch=noreg); // Scratch reg is ignored in 32-bit
#endif

  void movptr(Register dst, intptr_t src);
  void movptr(Register dst, Register src);
  void movptr(Address dst, intptr_t src);

  void movptr(Address dst, Register src);

  void movptr(Register dst, RegisterOrConstant src) {
    if (src.is_constant()) movptr(dst, src.as_constant());
    else                   movptr(dst, src.as_register());
  }

#ifdef _LP64
  // Generally the next two are only used for moving NULL
  // Although there are situations in initializing the mark word where
  // they could be used. They are dangerous.

  // They only exist on LP64 so that int32_t and intptr_t are not the same
  // and we have ambiguous declarations.

  void movptr(Address dst, int32_t imm32);
  void movptr(Register dst, int32_t imm32);
#endif // _LP64

  // to avoid hiding movl
  void mov32(AddressLiteral dst, Register src);
  void mov32(Register dst, AddressLiteral src);

  // to avoid hiding movb
  void movbyte(ArrayAddress dst, int src);

  // Import other mov() methods from the parent class or else
  // they will be hidden by the following overriding declaration.
  using Assembler::movdl;
  using Assembler::movq;
  void movdl(XMMRegister dst, AddressLiteral src);
  void movq(XMMRegister dst, AddressLiteral src);

  // Can push value or effective address
  void pushptr(AddressLiteral src);

  void pushptr(Address src) { LP64_ONLY(pushq(src)) NOT_LP64(pushl(src)); }
  void popptr(Address src) { LP64_ONLY(popq(src)) NOT_LP64(popl(src)); }

  void pushoop(jobject obj);
  void pushklass(Metadata* obj);

  // sign extend as need a l to ptr sized element
  void movl2ptr(Register dst, Address src) { LP64_ONLY(movslq(dst, src)) NOT_LP64(movl(dst, src)); }
  void movl2ptr(Register dst, Register src) { LP64_ONLY(movslq(dst, src)) NOT_LP64(if (dst != src) movl(dst, src)); }


 public:
  // C2 compiled method's prolog code.
  void verified_entry(int framesize, int stack_bang_size, bool fp_mode_24b, bool is_stub);

  // clear memory of size 'cnt' qwords, starting at 'base';
  // if 'is_large' is set, do not try to produce short loop
  void clear_mem(Register base, Register cnt, Register rtmp, XMMRegister xtmp, bool is_large, KRegister mask=knoreg);

  // clear memory initialization sequence for constant size;
  void clear_mem(Register base, int cnt, Register rtmp, XMMRegister xtmp, KRegister mask=knoreg);

  // clear memory of size 'cnt' qwords, starting at 'base' using XMM/YMM registers
  void xmm_clear_mem(Register base, Register cnt, Register rtmp, XMMRegister xtmp, KRegister mask=knoreg);

  // Fill primitive arrays
  void generate_fill(BasicType t, bool aligned,
                     Register to, Register value, Register count,
                     Register rtmp, XMMRegister xtmp);

  void encode_iso_array(Register src, Register dst, Register len,
                        XMMRegister tmp1, XMMRegister tmp2, XMMRegister tmp3,
                        XMMRegister tmp4, Register tmp5, Register result);

#ifdef _LP64
  void add2_with_carry(Register dest_hi, Register dest_lo, Register src1, Register src2);
  void multiply_64_x_64_loop(Register x, Register xstart, Register x_xstart,
                             Register y, Register y_idx, Register z,
                             Register carry, Register product,
                             Register idx, Register kdx);
  void multiply_add_128_x_128(Register x_xstart, Register y, Register z,
                              Register yz_idx, Register idx,
                              Register carry, Register product, int offset);
  void multiply_128_x_128_bmi2_loop(Register y, Register z,
                                    Register carry, Register carry2,
                                    Register idx, Register jdx,
                                    Register yz_idx1, Register yz_idx2,
                                    Register tmp, Register tmp3, Register tmp4);
  void multiply_128_x_128_loop(Register x_xstart, Register y, Register z,
                               Register yz_idx, Register idx, Register jdx,
                               Register carry, Register product,
                               Register carry2);
  void multiply_to_len(Register x, Register xlen, Register y, Register ylen, Register z, Register zlen,
                       Register tmp1, Register tmp2, Register tmp3, Register tmp4, Register tmp5);
  void square_rshift(Register x, Register len, Register z, Register tmp1, Register tmp3,
                     Register tmp4, Register tmp5, Register rdxReg, Register raxReg);
  void multiply_add_64_bmi2(Register sum, Register op1, Register op2, Register carry,
                            Register tmp2);
  void multiply_add_64(Register sum, Register op1, Register op2, Register carry,
                       Register rdxReg, Register raxReg);
  void add_one_64(Register z, Register zlen, Register carry, Register tmp1);
  void lshift_by_1(Register x, Register len, Register z, Register zlen, Register tmp1, Register tmp2,
                       Register tmp3, Register tmp4);
  void square_to_len(Register x, Register len, Register z, Register zlen, Register tmp1, Register tmp2,
                     Register tmp3, Register tmp4, Register tmp5, Register rdxReg, Register raxReg);

  void mul_add_128_x_32_loop(Register out, Register in, Register offset, Register len, Register tmp1,
               Register tmp2, Register tmp3, Register tmp4, Register tmp5, Register rdxReg,
               Register raxReg);
  void mul_add(Register out, Register in, Register offset, Register len, Register k, Register tmp1,
               Register tmp2, Register tmp3, Register tmp4, Register tmp5, Register rdxReg,
               Register raxReg);
  void vectorized_mismatch(Register obja, Register objb, Register length, Register log2_array_indxscale,
                           Register result, Register tmp1, Register tmp2,
                           XMMRegister vec1, XMMRegister vec2, XMMRegister vec3);
#endif

  // CRC32 code for java.util.zip.CRC32::updateBytes() intrinsic.
  void update_byte_crc32(Register crc, Register val, Register table);
  void kernel_crc32(Register crc, Register buf, Register len, Register table, Register tmp);


#ifdef _LP64
  void kernel_crc32_avx512(Register crc, Register buf, Register len, Register table, Register tmp1, Register tmp2);
  void kernel_crc32_avx512_256B(Register crc, Register buf, Register len, Register key, Register pos,
                                Register tmp1, Register tmp2, Label& L_barrett, Label& L_16B_reduction_loop,
                                Label& L_get_last_two_xmms, Label& L_128_done, Label& L_cleanup);
  void updateBytesAdler32(Register adler32, Register buf, Register length, XMMRegister shuf0, XMMRegister shuf1, ExternalAddress scale);
#endif // _LP64

  // CRC32C code for java.util.zip.CRC32C::updateBytes() intrinsic
  // Note on a naming convention:
  // Prefix w = register only used on a Westmere+ architecture
  // Prefix n = register only used on a Nehalem architecture
#ifdef _LP64
  void crc32c_ipl_alg4(Register in_out, uint32_t n,
                       Register tmp1, Register tmp2, Register tmp3);
#else
  void crc32c_ipl_alg4(Register in_out, uint32_t n,
                       Register tmp1, Register tmp2, Register tmp3,
                       XMMRegister xtmp1, XMMRegister xtmp2);
#endif
  void crc32c_pclmulqdq(XMMRegister w_xtmp1,
                        Register in_out,
                        uint32_t const_or_pre_comp_const_index, bool is_pclmulqdq_supported,
                        XMMRegister w_xtmp2,
                        Register tmp1,
                        Register n_tmp2, Register n_tmp3);
  void crc32c_rec_alt2(uint32_t const_or_pre_comp_const_index_u1, uint32_t const_or_pre_comp_const_index_u2, bool is_pclmulqdq_supported, Register in_out, Register in1, Register in2,
                       XMMRegister w_xtmp1, XMMRegister w_xtmp2, XMMRegister w_xtmp3,
                       Register tmp1, Register tmp2,
                       Register n_tmp3);
  void crc32c_proc_chunk(uint32_t size, uint32_t const_or_pre_comp_const_index_u1, uint32_t const_or_pre_comp_const_index_u2, bool is_pclmulqdq_supported,
                         Register in_out1, Register in_out2, Register in_out3,
                         Register tmp1, Register tmp2, Register tmp3,
                         XMMRegister w_xtmp1, XMMRegister w_xtmp2, XMMRegister w_xtmp3,
                         Register tmp4, Register tmp5,
                         Register n_tmp6);
  void crc32c_ipl_alg2_alt2(Register in_out, Register in1, Register in2,
                            Register tmp1, Register tmp2, Register tmp3,
                            Register tmp4, Register tmp5, Register tmp6,
                            XMMRegister w_xtmp1, XMMRegister w_xtmp2, XMMRegister w_xtmp3,
                            bool is_pclmulqdq_supported);
  // Fold 128-bit data chunk
  void fold_128bit_crc32(XMMRegister xcrc, XMMRegister xK, XMMRegister xtmp, Register buf, int offset);
  void fold_128bit_crc32(XMMRegister xcrc, XMMRegister xK, XMMRegister xtmp, XMMRegister xbuf);
#ifdef _LP64
  // Fold 512-bit data chunk
  void fold512bit_crc32_avx512(XMMRegister xcrc, XMMRegister xK, XMMRegister xtmp, Register buf, Register pos, int offset);
#endif // _LP64
  // Fold 8-bit data
  void fold_8bit_crc32(Register crc, Register table, Register tmp);
  void fold_8bit_crc32(XMMRegister crc, Register table, XMMRegister xtmp, Register tmp);

  // Compress char[] array to byte[].
  void char_array_compress(Register src, Register dst, Register len,
                           XMMRegister tmp1, XMMRegister tmp2, XMMRegister tmp3,
                           XMMRegister tmp4, Register tmp5, Register result,
                           KRegister mask1 = knoreg, KRegister mask2 = knoreg);

  // Inflate byte[] array to char[].
  void byte_array_inflate(Register src, Register dst, Register len,
                          XMMRegister tmp1, Register tmp2, KRegister mask = knoreg);

  void fill64_masked_avx(uint shift, Register dst, int disp,
                         XMMRegister xmm, KRegister mask, Register length,
                         Register temp, bool use64byteVector = false);

  void fill32_masked_avx(uint shift, Register dst, int disp,
                         XMMRegister xmm, KRegister mask, Register length,
                         Register temp);

  void fill32_avx(Register dst, int disp, XMMRegister xmm);

  void fill64_avx(Register dst, int dis, XMMRegister xmm, bool use64byteVector = false);

#ifdef _LP64
  void convert_f2i(Register dst, XMMRegister src);
  void convert_d2i(Register dst, XMMRegister src);
  void convert_f2l(Register dst, XMMRegister src);
  void convert_d2l(Register dst, XMMRegister src);

  void cache_wb(Address line);
  void cache_wbsync(bool is_pre);

#if COMPILER2_OR_JVMCI
  void arraycopy_avx3_special_cases(XMMRegister xmm, KRegister mask, Register from,
                                    Register to, Register count, int shift,
                                    Register index, Register temp,
                                    bool use64byteVector, Label& L_entry, Label& L_exit);

  void arraycopy_avx3_special_cases_conjoint(XMMRegister xmm, KRegister mask, Register from,
                                             Register to, Register start_index, Register end_index,
                                             Register count, int shift, Register temp,
                                             bool use64byteVector, Label& L_entry, Label& L_exit);

  void copy64_masked_avx(Register dst, Register src, XMMRegister xmm,
                         KRegister mask, Register length, Register index,
                         Register temp, int shift = Address::times_1, int offset = 0,
                         bool use64byteVector = false);

  void copy32_masked_avx(Register dst, Register src, XMMRegister xmm,
                         KRegister mask, Register length, Register index,
                         Register temp, int shift = Address::times_1, int offset = 0);

  void copy32_avx(Register dst, Register src, Register index, XMMRegister xmm,
                  int shift = Address::times_1, int offset = 0);

  void copy64_avx(Register dst, Register src, Register index, XMMRegister xmm,
                  bool conjoint, int shift = Address::times_1, int offset = 0,
                  bool use64byteVector = false);
#endif // COMPILER2_OR_JVMCI

#endif // _LP64

  void vallones(XMMRegister dst, int vector_len);
};

/**
 * class SkipIfEqual:
 *
 * Instantiating this class will result in assembly code being output that will
 * jump around any code emitted between the creation of the instance and it's
 * automatic destruction at the end of a scope block, depending on the value of
 * the flag passed to the constructor, which will be checked at run-time.
 */
class SkipIfEqual {
 private:
  MacroAssembler* _masm;
  Label _label;

 public:
   SkipIfEqual(MacroAssembler*, const bool* flag_addr, bool value);
   ~SkipIfEqual();
};

#endif // CPU_X86_MACROASSEMBLER_X86_HPP
