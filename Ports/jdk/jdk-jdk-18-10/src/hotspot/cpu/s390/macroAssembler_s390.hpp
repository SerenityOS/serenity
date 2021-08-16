/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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

#ifndef CPU_S390_MACROASSEMBLER_S390_HPP
#define CPU_S390_MACROASSEMBLER_S390_HPP

#include "asm/assembler.hpp"
#include "oops/accessDecorators.hpp"

#define MODERN_IFUN(name)  ((void (MacroAssembler::*)(Register, int64_t, Register, Register))&MacroAssembler::name)
#define CLASSIC_IFUN(name) ((void (MacroAssembler::*)(Register, int64_t, Register, Register))&MacroAssembler::name)
#define MODERN_FFUN(name)  ((void (MacroAssembler::*)(FloatRegister, int64_t, Register, Register))&MacroAssembler::name)
#define CLASSIC_FFUN(name) ((void (MacroAssembler::*)(FloatRegister, int64_t, Register, Register))&MacroAssembler::name)

class MacroAssembler: public Assembler {
 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

  //
  // Optimized instruction emitters
  //

  // Move register if destination register and target register are different.
  void lr_if_needed(Register rd, Register rs);
  void lgr_if_needed(Register rd, Register rs);
  void llgfr_if_needed(Register rd, Register rs);
  void ldr_if_needed(FloatRegister rd, FloatRegister rs);

  void move_reg_if_needed(Register dest, BasicType dest_type, Register src, BasicType src_type);
  void move_freg_if_needed(FloatRegister dest, BasicType dest_type, FloatRegister src, BasicType src_type);

  void freg2mem_opt(FloatRegister reg,
                    int64_t       disp,
                    Register      index,
                    Register      base,
                    void (MacroAssembler::*modern) (FloatRegister, int64_t, Register, Register),
                    void (MacroAssembler::*classic)(FloatRegister, int64_t, Register, Register),
                    Register      scratch = Z_R0);
  void freg2mem_opt(FloatRegister reg,
                    const Address &a, bool is_double = true);

  void mem2freg_opt(FloatRegister reg,
                    int64_t       disp,
                    Register      index,
                    Register      base,
                    void (MacroAssembler::*modern) (FloatRegister, int64_t, Register, Register),
                    void (MacroAssembler::*classic)(FloatRegister, int64_t, Register, Register),
                    Register      scratch = Z_R0);
  void mem2freg_opt(FloatRegister reg,
                    const Address &a, bool is_double = true);

  void reg2mem_opt(Register reg,
                   int64_t  disp,
                   Register index,
                   Register base,
                   void (MacroAssembler::*modern) (Register, int64_t, Register, Register),
                   void (MacroAssembler::*classic)(Register, int64_t, Register, Register),
                   Register scratch = Z_R0);
  // returns offset of the store instruction
  int reg2mem_opt(Register reg, const Address &a, bool is_double = true);

  void mem2reg_opt(Register reg,
                   int64_t  disp,
                   Register index,
                   Register base,
                   void (MacroAssembler::*modern) (Register, int64_t, Register, Register),
                   void (MacroAssembler::*classic)(Register, int64_t, Register, Register));
  void mem2reg_opt(Register reg, const Address &a, bool is_double = true);
  void mem2reg_signed_opt(Register reg, const Address &a);

  // AND immediate and set condition code, works for 64 bit immediates/operation as well.
   void and_imm(Register r, long mask, Register tmp = Z_R0, bool wide = false);

  // 1's complement, 32bit or 64bit. Optimized to exploit distinct operands facility.
  // Note: The condition code is neither preserved nor correctly set by this code!!!
  // Note: (wide == false) does not protect the high order half of the target register
  // from alternation. It only serves as optimization hint for 32-bit results.
  void not_(Register r1, Register r2 = noreg, bool wide = false);  // r1 = ~r2

  // Expanded support of all "rotate_then_<logicalOP>" instructions.
  //
  // Generalize and centralize rotate_then_<logicalOP> emitter.
  // Functional description. For details, see Principles of Operation, Chapter 7, "Rotate Then Insert..."
  //  - Bits  in a register are numbered left (most significant) to right (least significant), i.e. [0..63].
  //  - Bytes in a register are numbered left (most significant) to right (least significant), i.e. [0..7].
  //  - Register src is rotated to the left by (nRotate&0x3f) positions.
  //  - Negative values for nRotate result in a rotation to the right by abs(nRotate) positions.
  //  - The bits in positions [lBitPos..rBitPos] of the _ROTATED_ src operand take part in the
  //    logical operation performed on the contents (in those positions) of the dst operand.
  //  - The logical operation that is performed on the dst operand is one of
  //     o insert the selected bits (replacing the original contents of those bit positions)
  //     o and the selected bits with the corresponding bits of the dst operand
  //     o or  the selected bits with the corresponding bits of the dst operand
  //     o xor the selected bits with the corresponding bits of the dst operand
  //  - For clear_dst == true, the destination register is cleared before the bits are inserted.
  //    For clear_dst == false, only the bit positions that get data inserted from src
  //    are changed. All other bit positions remain unchanged.
  //  - For test_only == true,  the result of the logicalOP is only used to set the condition code, dst remains unchanged.
  //    For test_only == false, the result of the logicalOP replaces the selected bits of dst.
  //  - src32bit and dst32bit indicate the respective register is used as 32bit value only.
  //    Knowledge can simplify code generation.
  //
  // Here is an important performance note, valid for all <logicalOP>s except "insert":
  //   Due to the too complex nature of the operation, it cannot be done in a single cycle.
  //   Timing constraints require the instructions to be cracked into two micro-ops, taking
  //   one or two cycles each to execute. In some cases, an additional pipeline bubble might get added.
  //   Macroscopically, that makes up for a three- or four-cycle instruction where you would
  //   expect just a single cycle.
  //   It is thus not beneficial from a performance point of view to exploit those instructions.
  //   Other reasons (code compactness, register pressure, ...) might outweigh this penalty.
  //
  unsigned long create_mask(int lBitPos, int rBitPos);
  void rotate_then_mask(Register dst, Register src, int lBitPos, int rBitPos,
                        int nRotate, bool src32bit, bool dst32bit, bool oneBits);
  void rotate_then_insert(Register dst, Register src, int lBitPos, int rBitPos, int nRotate,
                          bool clear_dst);
  void rotate_then_and(Register dst, Register src, int lBitPos, int rBitPos, int nRotate,
                       bool test_only);
  void rotate_then_or(Register dst, Register src, int lBitPos, int rBitPos, int nRotate,
                      bool test_onlyt);
  void rotate_then_xor(Register dst, Register src, int lBitPos, int rBitPos, int nRotate,
                       bool test_only);

  void add64(Register r1, RegisterOrConstant inc);

  // Helper function to multiply the 64bit contents of a register by a 16bit constant.
  // The optimization tries to avoid the mghi instruction, since it uses the FPU for
  // calculation and is thus rather slow.
  //
  // There is no handling for special cases, e.g. cval==0 or cval==1.
  //
  // Returns len of generated code block.
  unsigned int mul_reg64_const16(Register rval, Register work, int cval);

  // Generic operation r1 := r2 + imm.
  void add2reg(Register r1, int64_t imm, Register r2 = noreg);
  // Generic operation r := b + x + d.
  void add2reg_with_index(Register r, int64_t d, Register x, Register b = noreg);

  // Add2mem* methods for direct memory increment.
  void add2mem_32(const Address &a, int64_t imm, Register tmp);
  void add2mem_64(const Address &a, int64_t imm, Register tmp);

  // *((int8_t*)(dst)) |= imm8
  inline void or2mem_8(Address& dst, int64_t imm8);

  // Load values by size and signedness.
  void load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed);
  void store_sized_value(Register src, Address dst, size_t size_in_bytes);

  // Load values with large offsets to base address.
 private:
  int  split_largeoffset(int64_t si20_offset, Register tmp, bool fixed_codelen, bool accumulate);
 public:
  void load_long_largeoffset(Register t, int64_t si20, Register a, Register tmp);
  void load_float_largeoffset(FloatRegister t, int64_t si20, Register a, Register tmp);
  void load_double_largeoffset(FloatRegister t, int64_t si20, Register a, Register tmp);

 private:
  long toc_distance();
 public:
  void load_toc(Register Rtoc);
  void load_long_pcrelative(Register Rdst, address dataLocation);
  static int load_long_pcrelative_size() { return 6; }
  void load_addr_pcrelative(Register Rdst, address dataLocation);
  static int load_addr_pcrel_size() { return 6; } // Just a LARL.

  // Load a value from memory and test (set CC).
  void load_and_test_byte    (Register dst, const Address &a);
  void load_and_test_short   (Register dst, const Address &a);
  void load_and_test_int     (Register dst, const Address &a);
  void load_and_test_int2long(Register dst, const Address &a);
  void load_and_test_long    (Register dst, const Address &a);

  // Test a bit in memory. Result is reflected in CC.
  void testbit(const Address &a, unsigned int bit);
  // Test a bit in a register. Result is reflected in CC.
  void testbit(Register r, unsigned int bitPos);

  void prefetch_read(Address a);
  void prefetch_update(Address a);

  // Clear a register, i.e. load const zero into reg. Return len (in bytes) of
  // generated instruction(s).
  //   whole_reg: Clear 64 bits if true, 32 bits otherwise.
  //   set_cc: Use instruction that sets the condition code, if true.
  int clear_reg(Register r, bool whole_reg = true, bool set_cc = true);

#ifdef ASSERT
  int preset_reg(Register r, unsigned long pattern, int pattern_len);
#endif

  // Clear (store zeros) a small piece of memory.
  // CAUTION: Do not use this for atomic memory clearing. Use store_const() instead.
  //   addr: Address descriptor of memory to clear.
  //         Index register will not be used!
  //   size: Number of bytes to clear.
  void clear_mem(const Address& addr, unsigned size);

  // Move immediate values to memory. Currently supports 32 and 64 bit stores,
  // but may be extended to 16 bit store operation, if needed.
  // For details, see implementation in *.cpp file.
         int store_const(const Address &dest, long imm,
                         unsigned int lm, unsigned int lc,
                         Register scratch = Z_R0);
  inline int store_const(const Address &dest, long imm,
                         Register scratch = Z_R0, bool is_long = true);

  // Move/initialize arbitrarily large memory area. No check for destructive overlap.
  // Being interruptible, these instructions need a retry-loop.
  void move_long_ext(Register dst, Register src, unsigned int pad);

  void compare_long_ext(Register left, Register right, unsigned int pad);
  void compare_long_uni(Register left, Register right, unsigned int pad);

  void search_string(Register end, Register start);
  void search_string_uni(Register end, Register start);

  // Translate instructions
  // Being interruptible, these instructions need a retry-loop.
  void translate_oo(Register dst, Register src, uint mask);
  void translate_ot(Register dst, Register src, uint mask);
  void translate_to(Register dst, Register src, uint mask);
  void translate_tt(Register dst, Register src, uint mask);

  // Crypto instructions.
  // Being interruptible, these instructions need a retry-loop.
  void cksm(Register crcBuff, Register srcBuff);
  void km( Register dstBuff, Register srcBuff);
  void kmc(Register dstBuff, Register srcBuff);
  void kimd(Register srcBuff);
  void klmd(Register srcBuff);
  void kmac(Register srcBuff);

  // nop padding
  void align(int modulus);
  void align_address(int modulus);

  //
  // Constants, loading constants, TOC support
  //

  // Load generic address: d <- base(a) + index(a) + disp(a).
  inline void load_address(Register d, const Address &a);
  // Load absolute address (and try to optimize).
  void load_absolute_address(Register d, address addr);

  // Address of Z_ARG1 and argument_offset.
  // If temp_reg == arg_slot, arg_slot will be overwritten.
  Address argument_address(RegisterOrConstant arg_slot,
                           Register temp_reg = noreg,
                           int64_t extra_slot_offset = 0);

  // Load a narrow ptr constant (oop or klass ptr).
  void load_narrow_oop( Register t, narrowOop a);
  void load_narrow_klass(Register t, Klass* k);

  static bool is_load_const_32to64(address pos);
  static bool is_load_narrow_oop(address pos)   { return is_load_const_32to64(pos); }
  static bool is_load_narrow_klass(address pos) { return is_load_const_32to64(pos); }

  static int  load_const_32to64_size()          { return 6; }
  static bool load_narrow_oop_size()            { return load_const_32to64_size(); }
  static bool load_narrow_klass_size()          { return load_const_32to64_size(); }

  static int  patch_load_const_32to64(address pos, int64_t a);
  static int  patch_load_narrow_oop(address pos, oop o);
  static int  patch_load_narrow_klass(address pos, Klass* k);

  // cOops. CLFI exploit.
  void compare_immediate_narrow_oop(Register oop1, narrowOop oop2);
  void compare_immediate_narrow_klass(Register op1, Klass* op2);
  static bool is_compare_immediate32(address pos);
  static bool is_compare_immediate_narrow_oop(address pos);
  static bool is_compare_immediate_narrow_klass(address pos);
  static int  compare_immediate_narrow_size()       { return 6; }
  static int  compare_immediate_narrow_oop_size()   { return compare_immediate_narrow_size(); }
  static int  compare_immediate_narrow_klass_size() { return compare_immediate_narrow_size(); }
  static int  patch_compare_immediate_32(address pos, int64_t a);
  static int  patch_compare_immediate_narrow_oop(address pos, oop o);
  static int  patch_compare_immediate_narrow_klass(address pos, Klass* k);

  // Load a 32bit constant into a 64bit register.
  void load_const_32to64(Register t, int64_t x, bool sign_extend=true);
  // Load a 64 bit constant.
         void load_const(Register t, long a);
  inline void load_const(Register t, void* a);
  inline void load_const(Register t, Label& L);
  inline void load_const(Register t, const AddressLiteral& a);
  // Get the 64 bit constant from a `load_const' sequence.
  static long get_const(address load_const);
  // Patch the 64 bit constant of a `load_const' sequence. This is a low level
  // procedure. It neither flushes the instruction cache nor is it atomic.
  static void patch_const(address load_const, long x);
  static int load_const_size() { return 12; }

  // Turn a char into boolean. NOTE: destroys r.
  void c2bool(Register r, Register t = Z_R0);

  // Optimized version of load_const for constants that do not need to be
  // loaded by a sequence of instructions of fixed length and that do not
  // need to be patched.
  int load_const_optimized_rtn_len(Register t, long x, bool emit);
  inline void load_const_optimized(Register t, long x);
  inline void load_const_optimized(Register t, void* a);
  inline void load_const_optimized(Register t, Label& L);
  inline void load_const_optimized(Register t, const AddressLiteral& a);

 public:

  //----------------------------------------------------------
  //            oops in code             -------------
  //  including compressed oops support  -------------
  //----------------------------------------------------------

  // Metadata in code that we have to keep track of.
  AddressLiteral allocate_metadata_address(Metadata* obj); // allocate_index
  AddressLiteral constant_metadata_address(Metadata* obj); // find_index

  // allocate_index
  AddressLiteral allocate_oop_address(jobject obj);
  // find_index
  AddressLiteral constant_oop_address(jobject obj);
  // Uses allocate_oop_address.
  inline void set_oop         (jobject obj, Register d);
  // Uses constant_oop_address.
  inline void set_oop_constant(jobject obj, Register d);
  // Uses constant_metadata_address.
  inline bool set_metadata_constant(Metadata* md, Register d);

  //
  // branch, jump
  //

  // Use one generic function for all branch patches.
  static unsigned long patched_branch(address dest_pos, unsigned long inst, address inst_pos);

  void pd_patch_instruction(address branch, address target, const char* file, int line);

  // Extract relative address from "relative" instructions.
  static long get_pcrel_offset(unsigned long inst);
  static long get_pcrel_offset(address pc);
  static address get_target_addr_pcrel(address pc);

  static inline bool is_call_pcrelative_short(unsigned long inst);
  static inline bool is_call_pcrelative_long(unsigned long inst);
  static inline bool is_branch_pcrelative_short(unsigned long inst);
  static inline bool is_branch_pcrelative_long(unsigned long inst);
  static inline bool is_compareandbranch_pcrelative_short(unsigned long inst);
  static inline bool is_branchoncount_pcrelative_short(unsigned long inst);
  static inline bool is_branchonindex32_pcrelative_short(unsigned long inst);
  static inline bool is_branchonindex64_pcrelative_short(unsigned long inst);
  static inline bool is_branchonindex_pcrelative_short(unsigned long inst);
  static inline bool is_branch_pcrelative16(unsigned long inst);
  static inline bool is_branch_pcrelative32(unsigned long inst);
  static inline bool is_branch_pcrelative(unsigned long inst);
  static inline bool is_load_pcrelative_long(unsigned long inst);
  static inline bool is_misc_pcrelative_long(unsigned long inst);
  static inline bool is_pcrelative_short(unsigned long inst);
  static inline bool is_pcrelative_long(unsigned long inst);
  // PCrelative TOC access. Variants with address argument.
  static inline bool is_load_pcrelative_long(address iLoc);
  static inline bool is_pcrelative_short(address iLoc);
  static inline bool is_pcrelative_long(address iLoc);

  static inline bool is_pcrelative_instruction(address iloc);
  static inline bool is_load_addr_pcrel(address a);

  static void patch_target_addr_pcrel(address pc, address con);
  static void patch_addr_pcrel(address pc, address con) {
    patch_target_addr_pcrel(pc, con); // Just delegate. This is only for nativeInst_s390.cpp.
  }

  //---------------------------------------------------------
  //  Some macros for more comfortable assembler programming.
  //---------------------------------------------------------

  // NOTE: pass NearLabel T to signal that the branch target T will be bound to a near address.

  void compare32_and_branch(Register r1, RegisterOrConstant x2, branch_condition cond, Label& target);
  void compareU32_and_branch(Register r1, RegisterOrConstant x2, branch_condition cond, Label& target);
  void compare64_and_branch(Register r1, RegisterOrConstant x2, branch_condition cond, Label& target);
  void compareU64_and_branch(Register r1, RegisterOrConstant x2, branch_condition cond, Label& target);

  void branch_optimized(Assembler::branch_condition cond, address branch_target);
  void branch_optimized(Assembler::branch_condition cond, Label&  branch_target);
  void compare_and_branch_optimized(Register r1,
                                    Register r2,
                                    Assembler::branch_condition cond,
                                    address  branch_addr,
                                    bool     len64,
                                    bool     has_sign);
  void compare_and_branch_optimized(Register r1,
                                    jlong    x2,
                                    Assembler::branch_condition cond,
                                    Label&   branch_target,
                                    bool     len64,
                                    bool     has_sign);
  void compare_and_branch_optimized(Register r1,
                                    Register r2,
                                    Assembler::branch_condition cond,
                                    Label&   branch_target,
                                    bool     len64,
                                    bool     has_sign);

  //
  // Support for frame handling
  //
  // Specify the register that should be stored as the return pc in the
  // current frame (default is R14).
  inline void save_return_pc(Register pc = Z_R14);
  inline void restore_return_pc();

  // Get current PC.
  address get_PC(Register result);

  // Get current PC + offset. Offset given in bytes, must be even!
  address get_PC(Register result, int64_t offset);

  // Get size of instruction at pc (which must point to valid code).
  void instr_size(Register size, Register pc);

  // Accessing, and in particular modifying, a stack location is only safe if
  // the stack pointer (Z_SP) is set such that the accessed stack location is
  // in the reserved range.
  //
  // From a performance point of view, it is desirable not to change the SP
  // first and then immediately use it to access the freshly reserved space.
  // That opens a small gap, though. If, just after storing some value (the
  // frame pointer) into the to-be-reserved space, an interrupt is caught,
  // the handler might use the space beyond Z_SP for it's own purpose.
  // If that happens, the stored value might get altered.

  // Resize current frame either relatively wrt to current SP or absolute.
  void resize_frame_sub(Register offset, Register fp, bool load_fp=true);
  void resize_frame_abs_with_offset(Register newSP, Register fp, int offset, bool load_fp);
  void resize_frame_absolute(Register addr, Register fp, bool load_fp);
  void resize_frame(RegisterOrConstant offset, Register fp, bool load_fp=true);

  // Push a frame of size bytes, if copy_sp is false, old_sp must already
  // contain a copy of Z_SP.
  void push_frame(Register bytes, Register old_sp, bool copy_sp = true, bool bytes_with_inverted_sign = false);

  // Push a frame of size `bytes'. no abi space provided.
  // Don't rely on register locking, instead pass a scratch register
  // (Z_R0 by default).
  // CAUTION! passing registers >= Z_R2 may produce bad results on
  // old CPUs!
  unsigned int push_frame(unsigned int bytes, Register scratch = Z_R0);

  // Push a frame of size `bytes' with abi160 on top.
  unsigned int push_frame_abi160(unsigned int bytes);

  // Pop current C frame.
  void pop_frame();
  // Pop current C frame and restore return PC register (Z_R14).
  void pop_frame_restore_retPC(int frame_size_in_bytes);

  //
  // Calls
  //

 private:
  address _last_calls_return_pc;

 public:
  // Support for VM calls. This is the base routine called by the
  // different versions of call_VM_leaf. The interpreter may customize
  // this version by overriding it for its purposes (e.g., to
  // save/restore additional registers when doing a VM call).
  void call_VM_leaf_base(address entry_point);
  void call_VM_leaf_base(address entry_point, bool allow_relocation);

  // It is imperative that all calls into the VM are handled via the
  // call_VM macros. They make sure that the stack linkage is setup
  // correctly. Call_VM's correspond to ENTRY/ENTRY_X entry points
  // while call_VM_leaf's correspond to LEAF entry points.
  //
  // This is the base routine called by the different versions of
  // call_VM. The interpreter may customize this version by overriding
  // it for its purposes (e.g., to save/restore additional registers
  // when doing a VM call).

  // If no last_java_sp is specified (noreg) then SP will be used instead.

  virtual void call_VM_base(
    Register        oop_result,        // Where an oop-result ends up if any; use noreg otherwise.
    Register        last_java_sp,      // To set up last_Java_frame in stubs; use noreg otherwise.
    address         entry_point,       // The entry point.
    bool            check_exception);  // Flag which indicates if exception should be checked.
  virtual void call_VM_base(
    Register        oop_result,       // Where an oop-result ends up if any; use noreg otherwise.
    Register        last_java_sp,     // To set up last_Java_frame in stubs; use noreg otherwise.
    address         entry_point,      // The entry point.
    bool            allow_relocation, // Flag to request generation of relocatable code.
    bool            check_exception); // Flag which indicates if exception should be checked.

  // Call into the VM.
  // Passes the thread pointer (in Z_ARG1) as a prepended argument.
  // Makes sure oop return values are visible to the GC.
  void call_VM(Register oop_result, address entry_point, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2,
               Register arg_3, bool check_exceptions = true);

  void call_VM_static(Register oop_result, address entry_point, bool check_exceptions = true);
  void call_VM_static(Register oop_result, address entry_point, Register arg_1, Register arg_2,
                      Register arg_3, bool check_exceptions = true);

  // Overloaded with last_java_sp.
  void call_VM(Register oop_result, Register last_java_sp, address entry_point, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point,
               Register arg_1, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point,
               Register arg_1, Register arg_2, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point,
               Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);

  void call_VM_leaf(address entry_point);
  void call_VM_leaf(address entry_point, Register arg_1);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);

  // Really static VM leaf call (never patched).
  void call_VM_leaf_static(address entry_point);
  void call_VM_leaf_static(address entry_point, Register arg_1);
  void call_VM_leaf_static(address entry_point, Register arg_1, Register arg_2);
  void call_VM_leaf_static(address entry_point, Register arg_1, Register arg_2, Register arg_3);

  // Call a C function via its function entry. Updates and returns _last_calls_return_pc.
  inline address call(Register function_entry);
  inline address call_c(Register function_entry);
         address call_c(address function_entry);
  // Variant for really static (non-relocatable) calls which are never patched.
         address call_c_static(address function_entry);
  // TOC or pc-relative call + emits a runtime_call relocation.
         address call_c_opt(address function_entry);

  inline address call_stub(Register function_entry);
  inline address call_stub(address  function_entry);

  // Get the pc where the last call will return to. Returns _last_calls_return_pc.
  inline address last_calls_return_pc();

 private:
  static bool is_call_far_patchable_variant0_at(address instruction_addr); // Dynamic TOC: load target addr from CP and call.
  static bool is_call_far_patchable_variant2_at(address instruction_addr); // PC-relative call, prefixed with NOPs.


 public:
  bool           call_far_patchable(address target, int64_t toc_offset);
  static bool    is_call_far_patchable_at(address inst_start);             // All supported forms of patchable calls.
  static bool    is_call_far_patchable_pcrelative_at(address inst_start);  // Pc-relative call with leading nops.
  static bool    is_call_far_pcrelative(address instruction_addr);         // Pure far pc-relative call, with one leading size adjustment nop.
  static void    set_dest_of_call_far_patchable_at(address inst_start, address target, int64_t toc_offset);
  static address get_dest_of_call_far_patchable_at(address inst_start, address toc_start);

  void align_call_far_patchable(address pc);

  // PCrelative TOC access.

  // This value is independent of code position - constant for the lifetime of the VM.
  static int call_far_patchable_size() {
    return load_const_from_toc_size() + call_byregister_size();
  }

  static int call_far_patchable_ret_addr_offset() { return call_far_patchable_size(); }

  static bool call_far_patchable_requires_alignment_nop(address pc) {
    int size = call_far_patchable_size();
    return ((intptr_t)(pc + size) & 0x03L) != 0;
  }

  // END OF PCrelative TOC access.

  static int jump_byregister_size()          { return 2; }
  static int jump_pcrelative_size()          { return 4; }
  static int jump_far_pcrelative_size()      { return 6; }
  static int call_byregister_size()          { return 2; }
  static int call_pcrelative_size()          { return 4; }
  static int call_far_pcrelative_size()      { return 2 + 6; } // Prepend each BRASL with a nop.
  static int call_far_pcrelative_size_raw()  { return 6; }     // Prepend each BRASL with a nop.

  //
  // Java utilities
  //

  // These routines should emit JVMTI PopFrame and ForceEarlyReturn handling code.
  // The implementation is only non-empty for the InterpreterMacroAssembler,
  // as only the interpreter handles PopFrame and ForceEarlyReturn requests.
  virtual void check_and_handle_popframe(Register java_thread);
  virtual void check_and_handle_earlyret(Register java_thread);

  // Polling page support.
  enum poll_mask {
    mask_stackbang = 0xde, // 222 (dec)
    mask_safepoint = 0x6f, // 111 (dec)
    mask_profiling = 0xba  // 186 (dec)
  };

  // Read from the polling page.
  void load_from_polling_page(Register polling_page_address, int64_t offset = 0);

  // Check if given instruction is a read from the polling page
  // as emitted by load_from_polling_page.
  static bool is_load_from_polling_page(address instr_loc);
  // Extract poll address from instruction and ucontext.
  static address get_poll_address(address instr_loc, void* ucontext);
  // Extract poll register from instruction.
  static uint get_poll_register(address instr_loc);

  // Check if safepoint requested and if so branch
  void safepoint_poll(Label& slow_path, Register temp_reg);

  // Stack overflow checking
  void bang_stack_with_offset(int offset);

  // Check for reserved stack access in method being exited. If the reserved
  // stack area was accessed, protect it again and throw StackOverflowError.
  // Uses Z_R1.
  void reserved_stack_check(Register return_pc);

  // Atomics
  // -- none?

  void tlab_allocate(Register obj,                // Result: pointer to object after successful allocation
                     Register var_size_in_bytes,  // Object size in bytes if unknown at compile time; invalid otherwise.
                     int      con_size_in_bytes,  // Object size in bytes if   known at compile time.
                     Register t1,                 // temp register
                     Label&   slow_case);         // Continuation point if fast allocation fails.

  // Emitter for interface method lookup.
  //   input: recv_klass, intf_klass, itable_index
  //   output: method_result
  //   kills: itable_index, temp1_reg, Z_R0, Z_R1
  void lookup_interface_method(Register           recv_klass,
                               Register           intf_klass,
                               RegisterOrConstant itable_index,
                               Register           method_result,
                               Register           temp1_reg,
                               Label&             no_such_interface,
                               bool               return_method = true);

  // virtual method calling
  void lookup_virtual_method(Register             recv_klass,
                             RegisterOrConstant   vtable_index,
                             Register             method_result);

  // Factor out code to call ic_miss_handler.
  unsigned int call_ic_miss_handler(Label& ICM, int trapMarker, int requiredSize, Register scratch);
  void nmethod_UEP(Label& ic_miss);

  // Emitters for "partial subtype" checks.

  // Test sub_klass against super_klass, with fast and slow paths.

  // The fast path produces a tri-state answer: yes / no / maybe-slow.
  // One of the three labels can be NULL, meaning take the fall-through.
  // If super_check_offset is -1, the value is loaded up from super_klass.
  // No registers are killed, except temp_reg and temp2_reg.
  // If super_check_offset is not -1, temp1_reg is not used and can be noreg.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp1_reg,
                                     Label*   L_success,
                                     Label*   L_failure,
                                     Label*   L_slow_path,
                                     RegisterOrConstant super_check_offset = RegisterOrConstant(-1));

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // The temp_reg can be noreg, if no temps are available.
  // It can also be sub_klass or super_klass, meaning it's OK to kill that one.
  // Updates the sub's secondary super cache as necessary.
  void check_klass_subtype_slow_path(Register Rsubklass,
                                     Register Rsuperklas,
                                     Register Rarray_ptr, // tmp
                                     Register Rlength,    // tmp
                                     Label* L_success,
                                     Label* L_failure);

  // Simplified, combined version, good for typical uses.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp1_reg,
                           Register temp2_reg,
                           Label&   L_success);

  void clinit_barrier(Register klass,
                      Register thread,
                      Label* L_fast_path = NULL,
                      Label* L_slow_path = NULL);

  // Increment a counter at counter_address when the eq condition code is set.
  // Kills registers tmp1_reg and tmp2_reg and preserves the condition code.
  void increment_counter_eq(address counter_address, Register tmp1_reg, Register tmp2_reg);

  void compiler_fast_lock_object(Register oop, Register box, Register temp1, Register temp2);
  void compiler_fast_unlock_object(Register oop, Register box, Register temp1, Register temp2);

  void resolve_jobject(Register value, Register tmp1, Register tmp2);

  // Support for last Java frame (but use call_VM instead where possible).
 private:
  void set_last_Java_frame(Register last_Java_sp, Register last_Java_pc, bool allow_relocation);
  void reset_last_Java_frame(bool allow_relocation);
  void set_top_ijava_frame_at_SP_as_last_Java_frame(Register sp, Register tmp1, bool allow_relocation);
 public:
  inline void set_last_Java_frame(Register last_java_sp, Register last_Java_pc);
  inline void set_last_Java_frame_static(Register last_java_sp, Register last_Java_pc);
  inline void reset_last_Java_frame(void);
  inline void reset_last_Java_frame_static(void);
  inline void set_top_ijava_frame_at_SP_as_last_Java_frame(Register sp, Register tmp1);
  inline void set_top_ijava_frame_at_SP_as_last_Java_frame_static(Register sp, Register tmp1);

  void set_thread_state(JavaThreadState new_state);

  // Read vm result from thread.
  void get_vm_result  (Register oop_result);
  void get_vm_result_2(Register result);

  // Vm result is currently getting hijacked to for oop preservation.
  void set_vm_result(Register oop_result);

  // Support for NULL-checks
  //
  // Generates code that causes a NULL OS exception if the content of reg is NULL.
  // If the accessed location is M[reg + offset] and the offset is known, provide the
  // offset. No explicit code generation is needed if the offset is within a certain
  // range (0 <= offset <= page_size).
  //
  // %%%%%% Currently not done for z/Architecture

  void null_check(Register reg, Register tmp = Z_R0, int64_t offset = -1);
  static bool needs_explicit_null_check(intptr_t offset);  // Implemented in shared file ?!
  static bool uses_implicit_null_check(void* address);

  // Klass oop manipulations if compressed.
  void encode_klass_not_null(Register dst, Register src = noreg);
  void decode_klass_not_null(Register dst, Register src);
  void decode_klass_not_null(Register dst);
  void load_klass(Register klass, Address mem);
  void load_klass(Register klass, Register src_oop);
  void store_klass(Register klass, Register dst_oop, Register ck = noreg); // Klass will get compressed if ck not provided.
  void store_klass_gap(Register s, Register dst_oop);

  // This function calculates the size of the code generated by
  //   decode_klass_not_null(register dst)
  // when (Universe::heap() != NULL). Hence, if the instructions
  // it generates change, then this method needs to be updated.
  static int instr_size_for_decode_klass_not_null();

  void encode_heap_oop(Register oop);
  void encode_heap_oop_not_null(Register oop);

  static int get_oop_base_pow2_offset(uint64_t oop_base);
  int  get_oop_base(Register Rbase, uint64_t oop_base);
  int  get_oop_base_complement(Register Rbase, uint64_t oop_base);
  void compare_heap_oop(Register Rop1, Address mem, bool maybeNULL);
  void compare_klass_ptr(Register Rop1, int64_t disp, Register Rbase, bool maybeNULL);

  // Access heap oop, handle encoding and GC barriers.
 private:
  void access_store_at(BasicType type, DecoratorSet decorators,
                       const Address& addr, Register val,
                       Register tmp1, Register tmp2, Register tmp3);
  void access_load_at(BasicType type, DecoratorSet decorators,
                      const Address& addr, Register dst,
                      Register tmp1, Register tmp2, Label *is_null = NULL);

 public:
  // tmp1 and tmp2 are used with decorators ON_PHANTOM_OOP_REF or ON_WEAK_OOP_REF.
  void load_heap_oop(Register dest, const Address &a,
                     Register tmp1, Register tmp2,
                     DecoratorSet decorators = 0, Label *is_null = NULL);
  void store_heap_oop(Register Roop, const Address &a,
                      Register tmp1, Register tmp2, Register tmp3,
                      DecoratorSet decorators = 0);

  void oop_encoder(Register Rdst, Register Rsrc, bool maybeNULL,
                   Register Rbase = Z_R1, int pow2_offset = -1, bool only32bitValid = false);
  void oop_decoder(Register Rdst, Register Rsrc, bool maybeNULL,
                   Register Rbase = Z_R1, int pow2_offset = -1);

  void resolve_oop_handle(Register result);
  void load_mirror_from_const_method(Register mirror, Register const_method);
  void load_method_holder(Register holder, Register method);

  //--------------------------
  //---  Operations on arrays.
  //--------------------------
  unsigned int Clear_Array(Register cnt_arg, Register base_pointer_arg, Register odd_tmp_reg);
  unsigned int Clear_Array_Const(long cnt, Register base);
  unsigned int Clear_Array_Const_Big(long cnt, Register base_pointer_arg, Register odd_tmp_reg);
  unsigned int CopyRawMemory_AlignedDisjoint(Register src_reg, Register dst_reg,
                                             Register cnt_reg,
                                             Register tmp1_reg, Register tmp2_reg);


  // Emit an oop const to the constant pool and set a relocation info
  // with address current_pc. Return the TOC offset of the constant.
  int store_const_in_toc(AddressLiteral& val);
  int store_oop_in_toc(AddressLiteral& oop);
  // Emit an oop const to the constant pool via store_oop_in_toc, or
  // emit a scalar const to the constant pool via store_const_in_toc,
  // and load the constant into register dst.
  bool load_const_from_toc(Register dst, AddressLiteral& a, Register Rtoc = noreg);
  // Get CPU version dependent size of load_const sequence.
  // The returned value is valid only for code sequences
  // generated by load_const, not load_const_optimized.
  static int load_const_from_toc_size() {
    return load_long_pcrelative_size();
  }
  bool load_oop_from_toc(Register dst, AddressLiteral& a, Register Rtoc = noreg);
  static intptr_t get_const_from_toc(address pc);
  static void     set_const_in_toc(address pc, unsigned long new_data, CodeBlob *cb);

  // Dynamic TOC.
  static bool is_load_const(address a);
  static bool is_load_const_from_toc_pcrelative(address a);
  static bool is_load_const_from_toc(address a) { return is_load_const_from_toc_pcrelative(a); }

  // PCrelative TOC access.
  static bool is_call_byregister(address a) { return is_z_basr(*(short*)a); }
  static bool is_load_const_from_toc_call(address a);
  static bool is_load_const_call(address a);
  static int load_const_call_size() { return load_const_size() + call_byregister_size(); }
  static int load_const_from_toc_call_size() { return load_const_from_toc_size() + call_byregister_size(); }
  // Offset is +/- 2**32 -> use long.
  static long get_load_const_from_toc_offset(address a);

  // Bit operations for single register operands.
  inline void lshift(Register r, int places, bool doubl = true);   // <<
  inline void rshift(Register r, int places, bool doubl = true);   // >>

  //
  // Debugging
  //

  // Assert on CC (condition code in CPU state).
  void asm_assert(bool check_equal, const char* msg, int id) PRODUCT_RETURN;
  void asm_assert_low(const char *msg, int id) PRODUCT_RETURN;
  void asm_assert_high(const char *msg, int id) PRODUCT_RETURN;
  void asm_assert_eq(const char* msg, int id) { asm_assert(true, msg, id); }
  void asm_assert_ne(const char* msg, int id) { asm_assert(false, msg, id); }

  void asm_assert_static(bool check_equal, const char* msg, int id) PRODUCT_RETURN;

 private:
  // Emit assertions.
  void asm_assert_mems_zero(bool check_equal, bool allow_relocation, int size, int64_t mem_offset,
                            Register mem_base, const char* msg, int id) PRODUCT_RETURN;

 public:
  inline void asm_assert_mem4_is_zero(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(true,  true, 4, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem8_is_zero(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(true,  true, 8, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem4_isnot_zero(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(false, true, 4, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem8_isnot_zero(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(false, true, 8, mem_offset, mem_base, msg, id);
  }

  inline void asm_assert_mem4_is_zero_static(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(true,  false, 4, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem8_is_zero_static(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(true,  false, 8, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem4_isnot_zero_static(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(false, false, 4, mem_offset, mem_base, msg, id);
  }
  inline void asm_assert_mem8_isnot_zero_static(int64_t mem_offset, Register mem_base, const char* msg, int id) {
    asm_assert_mems_zero(false, false, 8, mem_offset, mem_base, msg, id);
  }
  void asm_assert_frame_size(Register expected_size, Register tmp, const char* msg, int id) PRODUCT_RETURN;

  // Verify Z_thread contents.
  void verify_thread();

  // Save and restore functions: Exclude Z_R0.
  void save_volatile_regs(   Register dst, int offset, bool include_fp, bool include_flags);
  void restore_volatile_regs(Register src, int offset, bool include_fp, bool include_flags);

  // Only if +VerifyOops.
  // Kills Z_R0.
  void verify_oop(Register reg, const char* s = "broken oop");
  // Kills Z_R0, condition code.
  void verify_oop_addr(Address addr, const char* msg = "contains broken oop");

  // TODO: verify_method and klass metadata (compare against vptr?).
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line) {}

#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

 private:
  // Generate printout in stop().
  static const char* stop_types[];
  enum {
    stop_stop               = 0,
    stop_untested           = 1,
    stop_unimplemented      = 2,
    stop_shouldnotreachhere = 3,
    stop_end                = 4
  };
  // Prints msg and stops execution.
  void    stop(int type, const char* msg, int id = 0);
  address stop_chain(address reentry, int type, const char* msg, int id, bool allow_relocation); // Non-relocateable code only!!
  void    stop_static(int type, const char* msg, int id);                                        // Non-relocateable code only!!

 public:

  // Prints msg and stops.
  address stop_chain(      address reentry, const char* msg = "", int id = 0) { return stop_chain(reentry, stop_stop, msg, id, true); }
  address stop_chain_static(address reentry, const char* msg = "", int id = 0) { return stop_chain(reentry, stop_stop, msg, id, false); }
  void stop_static  (const char* msg = "", int id = 0) { stop_static(stop_stop,   msg, id); }
  void stop         (const char* msg = "", int id = 0) { stop(stop_stop,          msg, id); }
  void untested     (const char* msg = "", int id = 0) { stop(stop_untested,      msg, id); }
  void unimplemented(const char* msg = "", int id = 0) { stop(stop_unimplemented, msg, id); }
  void should_not_reach_here(const char* msg = "", int id = -1) { stop(stop_shouldnotreachhere, msg, id); }

  // Factor out part of stop into subroutine to save space.
  void stop_subroutine();

  // Prints msg, but don't stop.
  void warn(const char* msg);

  //-----------------------------
  //---  basic block tracing code
  //-----------------------------
  void trace_basic_block(uint i);
  void init_basic_block_trace();
  // Number of bytes a basic block gets larger due to the tracing code macro (worst case).
  // Currently, worst case is 48 bytes. 64 puts us securely on the safe side.
  static int basic_blck_trace_blk_size_incr() { return 64; }

  // Write pattern 0x0101010101010101 in region [low-before, high+after].
  // Low and high may be the same registers. Before and after are
  // the numbers of 8-byte words.
  void zap_from_to(Register low, Register high, Register tmp1 = Z_R0, Register tmp2 = Z_R1,
                   int before = 0, int after = 0) PRODUCT_RETURN;

  // Emitters for CRC32 calculation.
  // A note on invertCRC:
  //   Unfortunately, internal representation of crc differs between CRC32 and CRC32C.
  //   CRC32 holds it's current crc value in the externally visible representation.
  //   CRC32C holds it's current crc value in internal format, ready for updating.
  //   Thus, the crc value must be bit-flipped before updating it in the CRC32 case.
  //   In the CRC32C case, it must be bit-flipped when it is given to the outside world (getValue()).
  //   The bool invertCRC parameter indicates whether bit-flipping is required before updates.
 private:
  void fold_byte_crc32(Register crc, Register table, Register val, Register tmp);
  void fold_8bit_crc32(Register crc, Register table, Register tmp);
  void update_byte_crc32( Register crc, Register val, Register table);
  void update_byteLoop_crc32(Register crc, Register buf, Register len, Register table,
                             Register data);
  void update_1word_crc32(Register crc, Register buf, Register table, int bufDisp, int bufInc,
                          Register t0,  Register t1,  Register t2,  Register t3);
 public:
  void kernel_crc32_singleByteReg(Register crc, Register val, Register table,
                                  bool invertCRC);
  void kernel_crc32_singleByte(Register crc, Register buf, Register len, Register table, Register tmp,
                               bool invertCRC);
  void kernel_crc32_1byte(Register crc, Register buf, Register len, Register table,
                          Register t0,  Register t1,  Register t2,  Register t3,
                          bool invertCRC);
  void kernel_crc32_1word(Register crc, Register buf, Register len, Register table,
                          Register t0,  Register t1,  Register t2,  Register t3,
                          bool invertCRC);

  // Emitters for BigInteger.multiplyToLen intrinsic
  // note: length of result array (zlen) is passed on the stack
 private:
  void add2_with_carry(Register dest_hi, Register dest_lo,
                       Register src1, Register src2);
  void multiply_64_x_64_loop(Register x, Register xstart,
                             Register x_xstart,
                             Register y, Register y_idx, Register z,
                             Register carry, Register product,
                             Register idx, Register kdx);
  void multiply_add_128_x_128(Register x_xstart, Register y, Register z,
                              Register yz_idx, Register idx,
                              Register carry, Register product, int offset);
  void multiply_128_x_128_loop(Register x_xstart,
                               Register y, Register z,
                               Register yz_idx, Register idx,
                               Register jdx,
                               Register carry, Register product,
                               Register carry2);
 public:
  void multiply_to_len(Register x, Register xlen,
                       Register y, Register ylen,
                       Register z,
                       Register tmp1, Register tmp2,
                       Register tmp3, Register tmp4, Register tmp5);
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
  SkipIfEqual(MacroAssembler*, const bool* flag_addr, bool value, Register _rscratch);
  ~SkipIfEqual();
};

#ifdef ASSERT
// Return false (e.g. important for our impl. of virtual calls).
inline bool AbstractAssembler::pd_check_instruction_mark() { return false; }
#endif

#endif // CPU_S390_MACROASSEMBLER_S390_HPP
