/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

// Major contributions by AHa, JL, LS

#ifndef CPU_S390_NATIVEINST_S390_HPP
#define CPU_S390_NATIVEINST_S390_HPP

#include "asm/macroAssembler.hpp"
#include "runtime/icache.hpp"
#include "runtime/os.hpp"

class NativeCall;
class NativeFarCall;
class NativeMovConstReg;
class NativeJump;
#ifndef COMPILER2
class NativeGeneralJump;
class NativeMovRegMem;
#endif
class NativeInstruction;

NativeCall* nativeCall_before(address return_address);
NativeCall* nativeCall_at(address instr);
NativeFarCall* nativeFarCall_before(address return_address);
NativeFarCall* nativeFarCall_at(address instr);
NativeMovConstReg* nativeMovConstReg_at(address address);
NativeMovConstReg* nativeMovConstReg_before(address address);
NativeJump* nativeJump_at(address address);
#ifndef COMPILER2
NativeMovRegMem* nativeMovRegMem_at (address address);
NativeGeneralJump* nativeGeneralJump_at(address address);
#endif
NativeInstruction* nativeInstruction_at(address address);

// We have interface for the following instructions:
// - NativeInstruction
//   - NativeCall
//   - NativeFarCall
//   - NativeMovConstReg
//   - NativeMovRegMem
//   - NativeJump
//   - NativeGeneralJump
//   - NativeIllegalInstruction
// The base class for different kinds of native instruction abstractions.
// Provides the primitive operations to manipulate code relative to this.

//-------------------------------------
//  N a t i v e I n s t r u c t i o n
//-------------------------------------

class NativeInstruction {
  friend class Relocation;

 public:

  enum z_specific_constants {
    nop_instruction_size = 2
  };

  bool is_illegal();

  // Bcrl is currently the only accepted instruction here.
  bool is_jump();

  // We use an illtrap for marking a method as not_entrant or zombie.
  bool is_sigill_zombie_not_entrant();

  bool is_safepoint_poll() {
    // Is the current instruction a POTENTIAL read access to the polling page?
    // The instruction's current arguments are not checked!
    return MacroAssembler::is_load_from_polling_page(addr_at(0));
  }

  address get_poll_address(void *ucontext) {
    // Extract poll address from instruction and ucontext.
    return MacroAssembler::get_poll_address(addr_at(0), ucontext);
  }

  uint get_poll_register() {
    // Extract poll register from instruction.
    return MacroAssembler::get_poll_register(addr_at(0));
  }

 public:

  // The output of __ breakpoint_trap().
  static int illegal_instruction();

  // The address of the currently processed instruction.
  address instruction_address() const { return addr_at(0); }

 protected:
  address addr_at(int offset) const { return address(this) + offset; }

  // z/Architecture terminology
  //   halfword   = 2 bytes
  //   word       = 4 bytes
  //   doubleword = 8 bytes
  unsigned short halfword_at(int offset) const { return *(unsigned short*)addr_at(offset); }
  int  word_at(int offset)               const { return *(jint*)addr_at(offset); }
  long long_at(int offset)               const { return *(jlong*)addr_at(offset); }
  void set_halfword_at(int offset, short i); // Deals with I-cache.
  void set_word_at(int offset, int i);       // Deals with I-cache.
  void set_jlong_at(int offset, jlong i);    // Deals with I-cache.
  void set_addr_at(int offset, address x);   // Deals with I-cache.

  void print() const;
  void print(const char* msg) const;
  void dump() const;
  void dump(const unsigned int range) const;
  void dump(const unsigned int range, const char* msg) const;

 public:

  void verify();

  friend NativeInstruction* nativeInstruction_at(address address) {
    NativeInstruction* inst = (NativeInstruction*)address;
    #ifdef ASSERT
      inst->verify();
    #endif
    return inst;
  }
};

//---------------------------------------------------
//  N a t i v e I l l e g a l I n s t r u c t i o n
//---------------------------------------------------

class NativeIllegalInstruction: public NativeInstruction {
 public:
  enum z_specific_constants {
    instruction_size = 2
  };

  // Insert illegal opcode at specific address.
  static void insert(address code_pos);
};

//-----------------------
//  N a t i v e C a l l
//-----------------------

// The NativeCall is an abstraction for accessing/manipulating call
// instructions. It is used to manipulate inline caches, primitive &
// dll calls, etc.

// A native call, as defined by this abstraction layer, consists of
// all instructions required to set up for and actually make the call.
//
// On z/Architecture, there exist three different forms of native calls:
// 1) Call with pc-relative address, 1 instruction
//    The location of the target function is encoded as relative address
//    in the call instruction. The short form (BRAS) allows for a
//    16-bit signed relative address (in 2-byte units). The long form
//    (BRASL) allows for a 32-bit signed relative address (in 2-byte units).
// 2) Call with immediate address, 3 or 5 instructions.
//    The location of the target function is given by an immediate
//    constant which is loaded into a (scratch) register. Depending on
//    the hardware capabilities, this takes 2 or 4 instructions.
//    The call itself is then a "call by register"(BASR) instruction.
// 3) Call with address from constant pool, 2(3) instructions (with dynamic TOC)
//    The location of the target function is stored in the constant pool
//    during compilation. From there it is loaded into a (scratch) register.
//    The call itself is then a "call by register"(BASR) instruction.
//
// When initially generating a call, the compiler uses form 2) (not
// patchable, target address constant, e.g. runtime calls) or 3) (patchable,
// target address might eventually get relocated). Later in the process,
// a call could be transformed into form 1) (also patchable) during ShortenBranches.
//
// If a call is/has to be patchable, the instruction sequence generated for it
// has to be constant in length. Excessive space, created e.g. by ShortenBranches,
// is allocated to lower addresses and filled with nops. That is necessary to
// keep the return address constant, no matter what form the call has.
// Methods dealing with such calls have "patchable" as part of their name.

class NativeCall: public NativeInstruction {
 public:

  static int get_IC_pos_in_java_to_interp_stub() {
    return 0;
  }

  enum z_specific_constants {
    instruction_size                           = 18, // Used in shared code for calls with reloc_info:
                                                     // value correct if !has_long_displacement_fast().
    call_far_pcrelative_displacement_offset    =  4, // Includes 2 bytes for the nop.
    call_far_pcrelative_displacement_alignment =  4
  };


  // Maximum size (in bytes) of a call to an absolute address.
  // Used when emitting call to deopt handler blob, which is a
  // "load_const_call". The code pattern is:
  //   tmpReg := load_const(address);   (* depends on CPU ArchLvl, but is otherwise constant *)
  //   call(tmpReg);                    (* basr, 2 bytes *)
  static unsigned int max_instruction_size() {
    return MacroAssembler::load_const_size() + MacroAssembler::call_byregister_size();
  }

  // address instruction_address() const { return addr_at(0); }

  // For the ordering of the checks see note at nativeCall_before.
  address next_instruction_address() const  {
    address iaddr = instruction_address();

    if (MacroAssembler::is_load_const_call(iaddr)) {
      // Form 2): load_const, BASR
      return addr_at(MacroAssembler::load_const_call_size());
    }

    if (MacroAssembler::is_load_const_from_toc_call(iaddr)) {
      // Form 3): load_const_from_toc (LARL+LG/LGRL), BASR.
      return addr_at(MacroAssembler::load_const_from_toc_call_size());
    }

    if (MacroAssembler::is_call_far_pcrelative(iaddr)) {
      // Form 1): NOP, BRASL
      // The BRASL (Branch Relative And Save Long) is patched into the space created
      // by the load_const_from_toc_call sequence (typically (LARL-LG)/LGRL - BASR.
      // The BRASL must be positioned such that it's end is FW (4-byte) aligned (for atomic patching).
      // It is achieved by aligning the end of the entire sequence on a 4byte boundary, by inserting
      // a nop, if required, at the very beginning of the instruction sequence. The nop needs to
      // be accounted for when calculating the next instruction address. The alignment takes place
      // already when generating the original instruction sequence. The alignment requirement
      // makes the size depend on location.
      // The return address of the call must always be at the end of the instruction sequence.
      // Inserting the extra alignment nop (or anything else) at the end is not an option.
      // The patched-in brasl instruction is prepended with a nop to make it easier to
      // distinguish from a load_const_from_toc_call sequence.
      return addr_at(MacroAssembler::call_far_pcrelative_size());
    }

    ((NativeCall*)iaddr)->print();
    guarantee(false, "Not a NativeCall site");
    return NULL;
  }

  address return_address() const {
    return next_instruction_address();
  }

  address destination() const;

  void set_destination_mt_safe(address dest);

  void verify_alignment() {} // Yet another real do nothing guy :)
  void verify();

  // Creation.
  friend NativeCall* nativeCall_at(address instr) {
    NativeCall* call;

    // Make sure not to return garbage.
    if (NativeCall::is_call_at(instr)) {
      call = (NativeCall*)instr;
    } else {
      call = (NativeCall*)instr;
      call->print();
      guarantee(false, "Not a NativeCall site");
    }

#ifdef ASSERT
    call->verify();
#endif
    return call;
  }

  // This is a very tricky function to implement. It involves stepping
  // backwards in the instruction stream. On architectures with variable
  // instruction length, this is a risky endeavor. From the return address,
  // you do not know how far to step back to be at a location (your starting
  // point) that will eventually bring you back to the return address.
  // Furthermore, it may happen that there are multiple starting points.
  //
  // With only a few possible (allowed) code patterns, the risk is lower but
  // does not diminish completely. Experience shows that there are code patterns
  // which look like a load_const_from_toc_call @(return address-8), but in
  // fact are a call_far_pcrelative @(return address-6). The other way around
  // is possible as well, but was not knowingly observed so far.
  //
  // The unpredictability is caused by the pc-relative address field in both
  // the call_far_pcrelative (BASR) and the load_const_from_toc (LGRL)
  // instructions. This field can contain an arbitrary bit pattern.
  //
  // Here is a real-world example:
  // Mnemonics: <not a valid sequence>   LGRL r10,<addr> BASR r14,r10
  // Hex code:  eb01 9008 007a c498 ffff c4a8 c0e5 ffc1 0dea
  // Mnemonics: AGSI <mem>,I8  LGRL r9,<addr> BRASL r14,<addr>  correct
  //
  // If you first check for a load_const_from_toc_call @(-8), you will find
  // a false positive. In this example, it is obviously false, because the
  // preceding bytes do not form a valid instruction pattern. If you first
  // check for call_far_pcrelative @(-6), you get a true positive - in this
  // case.
  //
  // The following remedy has been implemented/enforced:
  // 1) Everywhere, the permissible code patterns are checked in the same
  //    sequence: Form 2) - Form 3) - Form 1).
  // 2) The call_far_pcrelative, which would ideally be just one BRASL
  //    instruction, is always prepended with a NOP. This measure avoids
  //    ambiguities with load_const_from_toc_call.
  friend NativeCall* nativeCall_before(address return_address) {
    NativeCall *call = NULL;

    // Make sure not to return garbage
    address instp = return_address - MacroAssembler::load_const_call_size();
    if (MacroAssembler::is_load_const_call(instp)) {                 // Form 2)
      call = (NativeCall*)(instp);                                   // load_const + basr
    } else {
      instp = return_address - MacroAssembler::load_const_from_toc_call_size();
      if (MacroAssembler::is_load_const_from_toc_call(instp)) {      // Form 3)
        call = (NativeCall*)(instp);                                 // load_const_from_toc + basr
      } else {
        instp = return_address - MacroAssembler::call_far_pcrelative_size();
        if (MacroAssembler::is_call_far_pcrelative(instp)) {         // Form 1)
          call = (NativeCall*)(instp);                               // brasl (or nop + brasl)
        } else {
          call = (NativeCall*)(instp);
          call->print();
          guarantee(false, "Not a NativeCall site");
        }
      }
    }

#ifdef ASSERT
    call->verify();
#endif
    return call;
  }

  // Ordering of checks 2) 3) 1) is relevant!
  static bool is_call_at(address a) {
    // Check plain instruction sequence. Do not care about filler or alignment nops.
    bool b = MacroAssembler::is_load_const_call(a) ||           // load_const + basr
             MacroAssembler::is_load_const_from_toc_call(a) ||  // load_const_from_toc + basr
             MacroAssembler::is_call_far_pcrelative(a);         // nop + brasl
    return b;
  }

  // Ordering of checks 2) 3) 1) is relevant!
  static bool is_call_before(address a) {
    // check plain instruction sequence. Do not care about filler or alignment nops.
    bool b = MacroAssembler::is_load_const_call(         a - MacroAssembler::load_const_call_size()) ||           // load_const + basr
             MacroAssembler::is_load_const_from_toc_call(a - MacroAssembler::load_const_from_toc_call_size()) ||  // load_const_from_toc + basr
             MacroAssembler::is_call_far_pcrelative(     a - MacroAssembler::call_far_pcrelative_size());         // nop+brasl
    return b;
  }

  static bool is_call_to(address instr, address target) {
    // Check whether there is a `NativeCall' at the address `instr'
    // calling to the address `target'.
    return is_call_at(instr) && target == ((NativeCall *)instr)->destination();
  }

  bool is_pcrelative() {
    return MacroAssembler::is_call_far_pcrelative((address)this);
  }
};

//-----------------------------
//  N a t i v e F a r C a l l
//-----------------------------

// The NativeFarCall is an abstraction for accessing/manipulating native
// call-anywhere instructions.
// Used to call native methods which may be loaded anywhere in the address
// space, possibly out of reach of a call instruction.

// Refer to NativeCall for a description of the supported call forms.

class NativeFarCall: public NativeInstruction {

 public:
  // We use MacroAssembler::call_far_patchable() for implementing a
  // call-anywhere instruction.

  static int instruction_size()      { return MacroAssembler::call_far_patchable_size(); }
  static int return_address_offset() { return MacroAssembler::call_far_patchable_ret_addr_offset(); }

  // address instruction_address() const { return addr_at(0); }

  address next_instruction_address() const {
    return addr_at(instruction_size());
  }

  address return_address() const {
    return addr_at(return_address_offset());
  }

  // Returns the NativeFarCall's destination.
  address destination();

  // Sets the NativeCall's destination, not necessarily mt-safe.
  // Used when relocating code.
  void set_destination(address dest, int toc_offset);

  // Checks whether instr points at a NativeFarCall instruction.
  static bool is_far_call_at(address instr) {
    // Use compound inspection function which, in addition to instruction sequence,
    // also checks for expected nops and for instruction alignment.
    return MacroAssembler::is_call_far_patchable_at(instr);
  }

  // Does the NativeFarCall implementation use a pc-relative encoding
  // of the call destination?
  // Used when relocating code.
  bool is_pcrelative() {
    address iaddr = (address)this;
    assert(is_far_call_at(iaddr), "unexpected call type");
    return MacroAssembler::is_call_far_patchable_pcrelative_at(iaddr);
  }

  void verify();

  // Instantiates a NativeFarCall object starting at the given instruction
  // address and returns the NativeFarCall object.
  inline friend NativeFarCall* nativeFarCall_at(address instr) {
    NativeFarCall* call = (NativeFarCall*)instr;
#ifdef ASSERT
    call->verify();
#endif
    return call;
  }
};


//-------------------------------------
//  N a t i v e M o v C o n s t R e g
//-------------------------------------

// An interface for accessing/manipulating native set_oop imm, reg instructions.
// (Used to manipulate inlined data references, etc.)

// A native move of a constant into a register, as defined by this abstraction layer,
// deals with instruction sequences that load "quasi constant" oops into registers
// for addressing. For multiple causes, those "quasi constant" oops eventually need
// to be changed (i.e. patched). The reason is quite simple: objects might get moved
// around in storage. Pc-relative oop addresses have to be patched also if the
// reference location is moved. That happens when executable code is relocated.

class NativeMovConstReg: public NativeInstruction {
 public:

  enum z_specific_constants {
    instruction_size = 10 // Used in shared code for calls with reloc_info.
  };

  // address instruction_address() const { return addr_at(0); }

  // The current instruction might be located at an offset.
  address next_instruction_address(int offset = 0) const;

  // (The [set_]data accessor respects oop_type relocs also.)
  intptr_t data() const;

  // Patch data in code stream.
  address set_data_plain(intptr_t x, CodeBlob *code);
  // Patch data in code stream and oop pool if necessary.
  void set_data(intptr_t x, relocInfo::relocType expected_type = relocInfo::none);

  // Patch narrow oop constant in code stream.
  void set_narrow_oop(intptr_t data);
  void set_narrow_klass(intptr_t data);
  void set_pcrel_addr(intptr_t addr, CompiledMethod *nm = NULL);
  void set_pcrel_data(intptr_t data, CompiledMethod *nm = NULL);

  void verify();

  // Creation.
  friend NativeMovConstReg* nativeMovConstReg_at(address address) {
    NativeMovConstReg* test = (NativeMovConstReg*)address;
    #ifdef ASSERT
      test->verify();
    #endif
    return test;
  }
};


#ifdef COMPILER1
//---------------------------------
//  N a t i v e M o v R e g M e m
//---------------------------------

// Interface to manipulate a code sequence that performs a memory access (load/store).
// The code is the patchable version of memory accesses generated by
// LIR_Assembler::reg2mem() and LIR_Assembler::mem2reg().
//
// Loading the offset for the mem access is target of the manipulation.
//
// The instruction sequence looks like this:
//   iihf        %r1,$bits1              ; load offset for mem access
//   iilf        %r1,$bits2
//   [compress oop]                      ; optional, store only
//   load/store  %r2,0(%r1,%r2)          ; memory access

class NativeMovRegMem;
inline NativeMovRegMem* nativeMovRegMem_at (address address);
class NativeMovRegMem: public NativeInstruction {
 public:
  enum z_specific_constants {
    instruction_size = 12 // load_const used with access_field_id
  };

  int num_bytes_to_end_of_patch() const { return instruction_size; }

  intptr_t offset() const {
    return nativeMovConstReg_at(addr_at(0))->data();
  }
  void set_offset(intptr_t x) {
    nativeMovConstReg_at(addr_at(0))->set_data(x);
  }
  void add_offset_in_bytes(intptr_t radd_offset) {
    set_offset(offset() + radd_offset);
  }
  void verify();

 private:
  friend inline NativeMovRegMem* nativeMovRegMem_at(address address) {
    NativeMovRegMem* test = (NativeMovRegMem*)address;
    #ifdef ASSERT
      test->verify();
    #endif
    return test;
  }
};
#endif // COMPILER1


//-----------------------
//  N a t i v e J u m p
//-----------------------


// An interface for accessing/manipulating native jumps
class NativeJump: public NativeInstruction {
 public:
  enum z_constants {
    instruction_size = 2 // Size of z_illtrap().
  };

  // Maximum size (in bytes) of a jump to an absolute address.
  // Used when emitting branch to an exception handler which is a "load_const_optimized_branch".
  // Thus, a pessimistic estimate is obtained when using load_const.
  // code pattern is:
  //   tmpReg := load_const(address);   (* varying size *)
  //   jumpTo(tmpReg);                  (* bcr, 2 bytes *)
  //
  static unsigned int max_instruction_size() {
    return MacroAssembler::load_const_size() + MacroAssembler::jump_byregister_size();
  }


//  address instruction_address() const { return addr_at(0); }

  address jump_destination() const {
    return (address)nativeMovConstReg_at(instruction_address())->data();
  }

  void set_jump_destination(address dest) {
    nativeMovConstReg_at(instruction_address())->set_data(((intptr_t)dest));
  }

  // Creation
  friend NativeJump* nativeJump_at(address address) {
    NativeJump* jump = (NativeJump*)address;
    #ifdef ASSERT
      jump->verify();
    #endif
    return jump;
  }

  static bool is_jump_at(address a) {
    int off = 0;
    bool b = (MacroAssembler::is_load_const_from_toc(a+off) &&
              Assembler::is_z_br(*(short*)(a+off + MacroAssembler::load_const_from_toc_size())));
    b = b || (MacroAssembler::is_load_const(a+off) &&
              Assembler::is_z_br(*(short*)(a+off + MacroAssembler::load_const_size())));
    return b;
  }

  void verify();

  // Insertion of native jump instruction.
  static void insert(address code_pos, address entry);

  // MT-safe insertion of native jump at verified method entry.
  static void check_verified_entry_alignment(address entry, address verified_entry) { }

  static void patch_verified_entry(address entry, address verified_entry, address dest);
};

//-------------------------------------
//  N a t i v e G e n e r a l J u m p
//-------------------------------------

// Despite the name, handles only simple branches.
// On ZARCH_64 BRCL only.
class NativeGeneralJump;
inline NativeGeneralJump* nativeGeneralJump_at(address address);
class NativeGeneralJump: public NativeInstruction {
 public:
  enum ZARCH_specific_constants {
    instruction_size = 6
  };

  address instruction_address() const { return addr_at(0); }
  address jump_destination()    const { return addr_at(0) + MacroAssembler::get_pcrel_offset(addr_at(0)); }

  // Creation
  friend inline NativeGeneralJump* nativeGeneralJump_at(address addr) {
    NativeGeneralJump* jump = (NativeGeneralJump*)(addr);
#ifdef ASSERT
    jump->verify();
#endif
    return jump;
  }

  // Insertion of native general jump instruction.
  static void insert_unconditional(address code_pos, address entry);

  void set_jump_destination(address dest) {
    Unimplemented();
    // set_word_at(MacroAssembler::call_far_pcrelative_size()-4, Assembler::z_pcrel_off(dest, addr_at(0)));
  }

  static void replace_mt_safe(address instr_addr, address code_buffer);

  void verify() PRODUCT_RETURN;
};

#endif // CPU_S390_NATIVEINST_S390_HPP
