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

#ifndef CPU_X86_NATIVEINST_X86_HPP
#define CPU_X86_NATIVEINST_X86_HPP

#include "asm/assembler.hpp"
#include "runtime/icache.hpp"
#include "runtime/safepointMechanism.hpp"

// We have interfaces for the following instructions:
// - NativeInstruction
// - - NativeCall
// - - NativeMovConstReg
// - - NativeMovConstRegPatching
// - - NativeMovRegMem
// - - NativeMovRegMemPatching
// - - NativeJump
// - - NativeFarJump
// - - NativeIllegalOpCode
// - - NativeGeneralJump
// - - NativeReturn
// - - NativeReturnX (return with argument)
// - - NativePushConst
// - - NativeTstRegMem

// The base class for different kinds of native instruction abstractions.
// Provides the primitive operations to manipulate code relative to this.

class NativeInstruction {
  friend class Relocation;

 public:
  enum Intel_specific_constants {
    nop_instruction_code        = 0x90,
    nop_instruction_size        =    1
  };

  bool is_nop()                        { return ubyte_at(0) == nop_instruction_code; }
  inline bool is_call();
  inline bool is_call_reg();
  inline bool is_illegal();
  inline bool is_return();
  inline bool is_jump();
  inline bool is_jump_reg();
  inline bool is_far_jump();
  inline bool is_cond_jump();
  inline bool is_safepoint_poll();
  inline bool is_mov_literal64();

 protected:
  address addr_at(int offset) const    { return address(this) + offset; }

  s_char sbyte_at(int offset) const    { return *(s_char*) addr_at(offset); }
  u_char ubyte_at(int offset) const    { return *(u_char*) addr_at(offset); }

  jint int_at(int offset) const         { return *(jint*) addr_at(offset); }

  intptr_t ptr_at(int offset) const    { return *(intptr_t*) addr_at(offset); }

  oop  oop_at (int offset) const       { return *(oop*) addr_at(offset); }


  void set_char_at(int offset, char c)        { *addr_at(offset) = (u_char)c; wrote(offset); }
  void set_int_at(int offset, jint  i)        { *(jint*)addr_at(offset) = i;  wrote(offset); }
  void set_ptr_at (int offset, intptr_t  ptr) { *(intptr_t*) addr_at(offset) = ptr;  wrote(offset); }
  void set_oop_at (int offset, oop  o)        { *(oop*) addr_at(offset) = o;  wrote(offset); }

  // This doesn't really do anything on Intel, but it is the place where
  // cache invalidation belongs, generically:
  void wrote(int offset);

 public:

  inline friend NativeInstruction* nativeInstruction_at(address address);
};

inline NativeInstruction* nativeInstruction_at(address address) {
  NativeInstruction* inst = (NativeInstruction*)address;
#ifdef ASSERT
  //inst->verify();
#endif
  return inst;
}

class NativePltCall: public NativeInstruction {
public:
  enum Intel_specific_constants {
    instruction_code           = 0xE8,
    instruction_size           =    5,
    instruction_offset         =    0,
    displacement_offset        =    1,
    return_address_offset      =    5
  };
  address instruction_address() const { return addr_at(instruction_offset); }
  address next_instruction_address() const { return addr_at(return_address_offset); }
  address displacement_address() const { return addr_at(displacement_offset); }
  int displacement() const { return (jint) int_at(displacement_offset); }
  address return_address() const { return addr_at(return_address_offset); }
  address destination() const;
  address plt_entry() const;
  address plt_jump() const;
  address plt_load_got() const;
  address plt_resolve_call() const;
  address plt_c2i_stub() const;
  void set_stub_to_clean();

  void  reset_to_plt_resolve_call();
  void  set_destination_mt_safe(address dest);

  void verify() const;
};

inline NativePltCall* nativePltCall_at(address address) {
  NativePltCall* call = (NativePltCall*) address;
#ifdef ASSERT
  call->verify();
#endif
  return call;
}

inline NativePltCall* nativePltCall_before(address addr) {
  address at = addr - NativePltCall::instruction_size;
  return nativePltCall_at(at);
}

class NativeCall;
inline NativeCall* nativeCall_at(address address);
// The NativeCall is an abstraction for accessing/manipulating native call imm32/rel32off
// instructions (used to manipulate inline caches, primitive & dll calls, etc.).

class NativeCall: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xE8,
    instruction_size            =    5,
    instruction_offset          =    0,
    displacement_offset         =    1,
    return_address_offset       =    5
  };

  enum { cache_line_size = BytesPerWord };  // conservative estimate!

  address instruction_address() const       { return addr_at(instruction_offset); }
  address next_instruction_address() const  { return addr_at(return_address_offset); }
  int   displacement() const                { return (jint) int_at(displacement_offset); }
  address displacement_address() const      { return addr_at(displacement_offset); }
  address return_address() const            { return addr_at(return_address_offset); }
  address destination() const;
  void  set_destination(address dest)       {
#ifdef AMD64
    intptr_t disp = dest - return_address();
    guarantee(disp == (intptr_t)(jint)disp, "must be 32-bit offset");
#endif // AMD64
    set_int_at(displacement_offset, dest - return_address());
  }
  void  set_destination_mt_safe(address dest);

  void  verify_alignment() { assert((intptr_t)addr_at(displacement_offset) % BytesPerInt == 0, "must be aligned"); }
  void  verify();
  void  print();

  // Creation
  inline friend NativeCall* nativeCall_at(address address);
  inline friend NativeCall* nativeCall_before(address return_address);

  static bool is_call_at(address instr) {
    return ((*instr) & 0xFF) == NativeCall::instruction_code;
  }

  static bool is_call_before(address return_address) {
    return is_call_at(return_address - NativeCall::return_address_offset);
  }

  static bool is_call_to(address instr, address target) {
    return nativeInstruction_at(instr)->is_call() &&
      nativeCall_at(instr)->destination() == target;
  }

  // MT-safe patching of a call instruction.
  static void insert(address code_pos, address entry);

  static void replace_mt_safe(address instr_addr, address code_buffer);
};

inline NativeCall* nativeCall_at(address address) {
  NativeCall* call = (NativeCall*)(address - NativeCall::instruction_offset);
#ifdef ASSERT
  call->verify();
#endif
  return call;
}

inline NativeCall* nativeCall_before(address return_address) {
  NativeCall* call = (NativeCall*)(return_address - NativeCall::return_address_offset);
#ifdef ASSERT
  call->verify();
#endif
  return call;
}

class NativeCallReg: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xFF,
    instruction_offset          =    0,
    return_address_offset_norex =    2,
    return_address_offset_rex   =    3
  };

  int next_instruction_offset() const  {
    if (ubyte_at(0) == NativeCallReg::instruction_code) {
      return return_address_offset_norex;
    } else {
      return return_address_offset_rex;
    }
  }
};

// An interface for accessing/manipulating native mov reg, imm32 instructions.
// (used to manipulate inlined 32bit data dll calls, etc.)
class NativeMovConstReg: public NativeInstruction {
#ifdef AMD64
  static const bool has_rex = true;
  static const int rex_size = 1;
#else
  static const bool has_rex = false;
  static const int rex_size = 0;
#endif // AMD64
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xB8,
    instruction_size            =    1 + rex_size + wordSize,
    instruction_offset          =    0,
    data_offset                 =    1 + rex_size,
    next_instruction_offset     =    instruction_size,
    register_mask               = 0x07
  };

  address instruction_address() const       { return addr_at(instruction_offset); }
  address next_instruction_address() const  { return addr_at(next_instruction_offset); }
  intptr_t data() const                     { return ptr_at(data_offset); }
  void  set_data(intptr_t x)                { set_ptr_at(data_offset, x); }

  void  verify();
  void  print();

  // Creation
  inline friend NativeMovConstReg* nativeMovConstReg_at(address address);
  inline friend NativeMovConstReg* nativeMovConstReg_before(address address);
};

inline NativeMovConstReg* nativeMovConstReg_at(address address) {
  NativeMovConstReg* test = (NativeMovConstReg*)(address - NativeMovConstReg::instruction_offset);
#ifdef ASSERT
  test->verify();
#endif
  return test;
}

inline NativeMovConstReg* nativeMovConstReg_before(address address) {
  NativeMovConstReg* test = (NativeMovConstReg*)(address - NativeMovConstReg::instruction_size - NativeMovConstReg::instruction_offset);
#ifdef ASSERT
  test->verify();
#endif
  return test;
}

class NativeMovConstRegPatching: public NativeMovConstReg {
 private:
    friend NativeMovConstRegPatching* nativeMovConstRegPatching_at(address address) {
    NativeMovConstRegPatching* test = (NativeMovConstRegPatching*)(address - instruction_offset);
    #ifdef ASSERT
      test->verify();
    #endif
    return test;
  }
};

// An interface for accessing/manipulating native moves of the form:
//      mov[b/w/l/q] [reg + offset], reg   (instruction_code_reg2mem)
//      mov[b/w/l/q] reg, [reg+offset]     (instruction_code_mem2reg
//      mov[s/z]x[w/b/q] [reg + offset], reg
//      fld_s  [reg+offset]
//      fld_d  [reg+offset]
//      fstp_s [reg + offset]
//      fstp_d [reg + offset]
//      mov_literal64  scratch,<pointer> ; mov[b/w/l/q] 0(scratch),reg | mov[b/w/l/q] reg,0(scratch)
//
// Warning: These routines must be able to handle any instruction sequences
// that are generated as a result of the load/store byte,word,long
// macros.  For example: The load_unsigned_byte instruction generates
// an xor reg,reg inst prior to generating the movb instruction.  This
// class must skip the xor instruction.

class NativeMovRegMem: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_prefix_wide_lo          = Assembler::REX,
    instruction_prefix_wide_hi          = Assembler::REX_WRXB,
    instruction_code_xor                = 0x33,
    instruction_extended_prefix         = 0x0F,
    instruction_code_mem2reg_movslq     = 0x63,
    instruction_code_mem2reg_movzxb     = 0xB6,
    instruction_code_mem2reg_movsxb     = 0xBE,
    instruction_code_mem2reg_movzxw     = 0xB7,
    instruction_code_mem2reg_movsxw     = 0xBF,
    instruction_operandsize_prefix      = 0x66,
    instruction_code_reg2mem            = 0x89,
    instruction_code_mem2reg            = 0x8b,
    instruction_code_reg2memb           = 0x88,
    instruction_code_mem2regb           = 0x8a,
    instruction_code_float_s            = 0xd9,
    instruction_code_float_d            = 0xdd,
    instruction_code_long_volatile      = 0xdf,
    instruction_code_xmm_ss_prefix      = 0xf3,
    instruction_code_xmm_sd_prefix      = 0xf2,
    instruction_code_xmm_code           = 0x0f,
    instruction_code_xmm_load           = 0x10,
    instruction_code_xmm_store          = 0x11,
    instruction_code_xmm_lpd            = 0x12,

    instruction_code_lea                = 0x8d,

    instruction_VEX_prefix_2bytes       = Assembler::VEX_2bytes,
    instruction_VEX_prefix_3bytes       = Assembler::VEX_3bytes,
    instruction_EVEX_prefix_4bytes      = Assembler::EVEX_4bytes,

    instruction_offset                  = 0,
    data_offset                         = 2,
    next_instruction_offset             = 4
  };

  // helper
  int instruction_start() const;

  address instruction_address() const {
    return addr_at(instruction_start());
  }

  int num_bytes_to_end_of_patch() const {
    return patch_offset() + sizeof(jint);
  }

  int offset() const {
    return int_at(patch_offset());
  }

  void set_offset(int x) {
    set_int_at(patch_offset(), x);
  }

  void add_offset_in_bytes(int add_offset) {
    int patch_off = patch_offset();
    set_int_at(patch_off, int_at(patch_off) + add_offset);
  }

  void verify();
  void print ();

 private:
  int patch_offset() const;
  inline friend NativeMovRegMem* nativeMovRegMem_at (address address);
};

inline NativeMovRegMem* nativeMovRegMem_at (address address) {
  NativeMovRegMem* test = (NativeMovRegMem*)(address - NativeMovRegMem::instruction_offset);
#ifdef ASSERT
  test->verify();
#endif
  return test;
}


// An interface for accessing/manipulating native leal instruction of form:
//        leal reg, [reg + offset]

class NativeLoadAddress: public NativeMovRegMem {
#ifdef AMD64
  static const bool has_rex = true;
  static const int rex_size = 1;
#else
  static const bool has_rex = false;
  static const int rex_size = 0;
#endif // AMD64
 public:
  enum Intel_specific_constants {
    instruction_prefix_wide             = Assembler::REX_W,
    instruction_prefix_wide_extended    = Assembler::REX_WB,
    lea_instruction_code                = 0x8D,
    mov64_instruction_code              = 0xB8
  };

  void verify();
  void print ();

 private:
  friend NativeLoadAddress* nativeLoadAddress_at (address address) {
    NativeLoadAddress* test = (NativeLoadAddress*)(address - instruction_offset);
    #ifdef ASSERT
      test->verify();
    #endif
    return test;
  }
};

// destination is rbx or rax
// mov rbx, [rip + offset]
class NativeLoadGot: public NativeInstruction {
#ifdef AMD64
  static const bool has_rex = true;
  static const int rex_size = 1;
#else
  static const bool has_rex = false;
  static const int rex_size = 0;
#endif

  enum Intel_specific_constants {
    rex_prefix = 0x48,
    rex_b_prefix = 0x49,
    instruction_code = 0x8b,
    modrm_rbx_code = 0x1d,
    modrm_rax_code = 0x05,
    instruction_length = 6 + rex_size,
    offset_offset = 2 + rex_size
  };

  int rip_offset() const { return int_at(offset_offset); }
  address return_address() const { return addr_at(instruction_length); }
  address got_address() const { return return_address() + rip_offset(); }

#ifdef ASSERT
  void report_and_fail() const;
  address instruction_address() const { return addr_at(0); }
#endif

public:
  address next_instruction_address() const { return return_address(); }
  intptr_t data() const;
  void set_data(intptr_t data) {
    intptr_t *addr = (intptr_t *) got_address();
    *addr = data;
  }

  DEBUG_ONLY( void verify() const );
};

inline NativeLoadGot* nativeLoadGot_at(address addr) {
  NativeLoadGot* load = (NativeLoadGot*) addr;
#ifdef ASSERT
  load->verify();
#endif
  return load;
}

// jump rel32off

class NativeJump: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xe9,
    instruction_size            =    5,
    instruction_offset          =    0,
    data_offset                 =    1,
    next_instruction_offset     =    5
  };

  address instruction_address() const       { return addr_at(instruction_offset); }
  address next_instruction_address() const  { return addr_at(next_instruction_offset); }
  address jump_destination() const          {
     address dest = (int_at(data_offset)+next_instruction_address());
     // 32bit used to encode unresolved jmp as jmp -1
     // 64bit can't produce this so it used jump to self.
     // Now 32bit and 64bit use jump to self as the unresolved address
     // which the inline cache code (and relocs) know about

     // return -1 if jump to self
    dest = (dest == (address) this) ? (address) -1 : dest;
    return dest;
  }

  void  set_jump_destination(address dest)  {
    intptr_t val = dest - next_instruction_address();
    if (dest == (address) -1) {
      val = -5; // jump to self
    }
#ifdef AMD64
    assert((labs(val)  & 0xFFFFFFFF00000000) == 0 || dest == (address)-1, "must be 32bit offset or -1");
#endif // AMD64
    set_int_at(data_offset, (jint)val);
  }

  // Creation
  inline friend NativeJump* nativeJump_at(address address);

  void verify();

  // Insertion of native jump instruction
  static void insert(address code_pos, address entry);
  // MT-safe insertion of native jump at verified method entry
  static void check_verified_entry_alignment(address entry, address verified_entry);
  static void patch_verified_entry(address entry, address verified_entry, address dest);
};

inline NativeJump* nativeJump_at(address address) {
  NativeJump* jump = (NativeJump*)(address - NativeJump::instruction_offset);
#ifdef ASSERT
  jump->verify();
#endif
  return jump;
}

// far jump reg
class NativeFarJump: public NativeInstruction {
 public:
  address jump_destination() const;

  // Creation
  inline friend NativeFarJump* nativeFarJump_at(address address);

  void verify();

};

inline NativeFarJump* nativeFarJump_at(address address) {
  NativeFarJump* jump = (NativeFarJump*)(address);
#ifdef ASSERT
  jump->verify();
#endif
  return jump;
}

// Handles all kinds of jump on Intel. Long/far, conditional/unconditional
class NativeGeneralJump: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    // Constants does not apply, since the lengths and offsets depends on the actual jump
    // used
    // Instruction codes:
    //   Unconditional jumps: 0xE9    (rel32off), 0xEB (rel8off)
    //   Conditional jumps:   0x0F8x  (rel32off), 0x7x (rel8off)
    unconditional_long_jump  = 0xe9,
    unconditional_short_jump = 0xeb,
    instruction_size = 5
  };

  address instruction_address() const       { return addr_at(0); }
  address jump_destination()    const;

  // Creation
  inline friend NativeGeneralJump* nativeGeneralJump_at(address address);

  // Insertion of native general jump instruction
  static void insert_unconditional(address code_pos, address entry);
  static void replace_mt_safe(address instr_addr, address code_buffer);

  void verify();
};

inline NativeGeneralJump* nativeGeneralJump_at(address address) {
  NativeGeneralJump* jump = (NativeGeneralJump*)(address);
  debug_only(jump->verify();)
  return jump;
}

class NativeGotJump: public NativeInstruction {
  enum Intel_specific_constants {
    rex_prefix = 0x41,
    instruction_code = 0xff,
    modrm_code = 0x25,
    instruction_size = 6,
    rip_offset = 2
  };

  bool has_rex() const { return ubyte_at(0) == rex_prefix; }
  int rex_size() const { return has_rex() ? 1 : 0; }

  address return_address() const { return addr_at(instruction_size + rex_size()); }
  int got_offset() const { return (jint) int_at(rip_offset + rex_size()); }

#ifdef ASSERT
  void report_and_fail() const;
  address instruction_address() const { return addr_at(0); }
#endif

public:
  address got_address() const { return return_address() + got_offset(); }
  address next_instruction_address() const { return return_address(); }
  bool is_GotJump() const { return ubyte_at(rex_size()) == instruction_code; }

  address destination() const;
  void set_jump_destination(address dest)  {
    address *got_entry = (address *) got_address();
    *got_entry = dest;
  }

  DEBUG_ONLY( void verify() const; )
};

inline NativeGotJump* nativeGotJump_at(address addr) {
  NativeGotJump* jump = (NativeGotJump*)(addr);
  debug_only(jump->verify());
  return jump;
}

class NativePopReg : public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0x58,
    instruction_size            =    1,
    instruction_offset          =    0,
    data_offset                 =    1,
    next_instruction_offset     =    1
  };

  // Insert a pop instruction
  static void insert(address code_pos, Register reg);
};


class NativeIllegalInstruction: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0x0B0F,    // Real byte order is: 0x0F, 0x0B
    instruction_size            =    2,
    instruction_offset          =    0,
    next_instruction_offset     =    2
  };

  // Insert illegal opcode as specific address
  static void insert(address code_pos);
};

// return instruction that does not pop values of the stack
class NativeReturn: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xC3,
    instruction_size            =    1,
    instruction_offset          =    0,
    next_instruction_offset     =    1
  };
};

// return instruction that does pop values of the stack
class NativeReturnX: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_code            = 0xC2,
    instruction_size            =    2,
    instruction_offset          =    0,
    next_instruction_offset     =    2
  };
};

// Simple test vs memory
class NativeTstRegMem: public NativeInstruction {
 public:
  enum Intel_specific_constants {
    instruction_rex_prefix_mask = 0xF0,
    instruction_rex_prefix      = Assembler::REX,
    instruction_rex_b_prefix    = Assembler::REX_B,
    instruction_code_memXregl   = 0x85,
    modrm_mask                  = 0x38, // select reg from the ModRM byte
    modrm_reg                   = 0x00  // rax
  };
};

inline bool NativeInstruction::is_illegal()      { return (short)int_at(0) == (short)NativeIllegalInstruction::instruction_code; }
inline bool NativeInstruction::is_call()         { return ubyte_at(0) == NativeCall::instruction_code; }
inline bool NativeInstruction::is_call_reg()     { return ubyte_at(0) == NativeCallReg::instruction_code ||
                                                          (ubyte_at(1) == NativeCallReg::instruction_code &&
                                                           (ubyte_at(0) == Assembler::REX || ubyte_at(0) == Assembler::REX_B)); }
inline bool NativeInstruction::is_return()       { return ubyte_at(0) == NativeReturn::instruction_code ||
                                                          ubyte_at(0) == NativeReturnX::instruction_code; }
inline bool NativeInstruction::is_jump()         { return ubyte_at(0) == NativeJump::instruction_code ||
                                                          ubyte_at(0) == 0xEB; /* short jump */ }
inline bool NativeInstruction::is_jump_reg()     {
  int pos = 0;
  if (ubyte_at(0) == Assembler::REX_B) pos = 1;
  return ubyte_at(pos) == 0xFF && (ubyte_at(pos + 1) & 0xF0) == 0xE0;
}
inline bool NativeInstruction::is_far_jump()     { return is_mov_literal64(); }
inline bool NativeInstruction::is_cond_jump()    { return (int_at(0) & 0xF0FF) == 0x800F /* long jump */ ||
                                                          (ubyte_at(0) & 0xF0) == 0x70;  /* short jump */ }
inline bool NativeInstruction::is_safepoint_poll() {
#ifdef AMD64
  const bool has_rex_prefix = ubyte_at(0) == NativeTstRegMem::instruction_rex_b_prefix;
  const int test_offset = has_rex_prefix ? 1 : 0;
#else
  const int test_offset = 0;
#endif
  const bool is_test_opcode = ubyte_at(test_offset) == NativeTstRegMem::instruction_code_memXregl;
  const bool is_rax_target = (ubyte_at(test_offset + 1) & NativeTstRegMem::modrm_mask) == NativeTstRegMem::modrm_reg;
  return is_test_opcode && is_rax_target;
}

inline bool NativeInstruction::is_mov_literal64() {
#ifdef AMD64
  return ((ubyte_at(0) == Assembler::REX_W || ubyte_at(0) == Assembler::REX_WB) &&
          (ubyte_at(1) & (0xff ^ NativeMovConstReg::register_mask)) == 0xB8);
#else
  return false;
#endif // AMD64
}

#endif // CPU_X86_NATIVEINST_X86_HPP
