/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

// Major contributions by ML, AHa.

#ifndef CPU_S390_FRAME_S390_HPP
#define CPU_S390_FRAME_S390_HPP

#include "runtime/synchronizer.hpp"

  //  C frame layout on ZARCH_64.
  //
  //  In this figure the stack grows upwards, while memory grows
  //  downwards. See "Linux for zSeries: ELF Application Binary Interface Supplement",
  //  IBM Corp. (LINUX-1107-01)
  //
  //  Square brackets denote stack regions possibly larger
  //  than a single 64 bit slot.
  //
  //  STACK:
  //    0       [C_FRAME]               <-- SP after prolog (mod 8 = 0)
  //            [C_FRAME]               <-- SP before prolog
  //            ...
  //            [C_FRAME]
  //
  //  C_FRAME:
  //    0       [ABI_160]
  //
  //  ABI_160:
  //    0       [ABI_16]
  //    16      CARG_1: spill slot for outgoing arg 1. used by next callee.
  //    24      CARG_2: spill slot for outgoing arg 2. used by next callee.
  //    32      CARG_3: spill slot for outgoing arg 3. used by next callee.
  //    40      CARG_4: spill slot for outgoing arg 4. used by next callee.
  //    48      GPR_6:  spill slot for GPR_6. used by next callee.
  //    ...     ...
  //    120     GPR_15:  spill slot for GPR_15. used by next callee.
  //    128     CFARG_1: spill slot for outgoing fp arg 1. used by next callee.
  //    136     CFARG_2: spill slot for outgoing fp arg 2. used by next callee.
  //    144     CFARG_3: spill slot for outgoing fp arg 3. used by next callee.
  //    152     CFARG_4: spill slot for outgoing fp arg 4. used by next callee.
  //    160     [REMAINING CARGS]
  //
  //  ABI_16:
  //    0       callers_sp
  //    8       return_pc

 public:

  // C frame layout

  typedef enum {
     // stack alignment
     alignment_in_bytes = 8,
     // log_2(8*8 bits) = 6.
     log_2_of_alignment_in_bits = 6
  } frame_constants;

  struct z_abi_16 {
    uint64_t callers_sp;
    uint64_t return_pc;
  };

  enum {
    z_abi_16_size = sizeof(z_abi_16)
  };

  #define _z_abi16(_component) \
          (offset_of(frame::z_abi_16, _component))

  // ABI_160:

  // REMARK: This structure should reflect the "minimal" ABI frame
  // layout, but it doesn't. There is an extra field at the end of the
  // structure that marks the area where arguments are passed, when
  // the argument registers "overflow". Thus, sizeof(z_abi_160)
  // doesn't yield the expected (and desired) result. Therefore, as
  // long as we do not provide extra infrastructure, one should use
  // either z_abi_160_size, or _z_abi(remaining_cargs) instead of
  // sizeof(...).
  struct z_abi_160 {
    uint64_t callers_sp;
    uint64_t return_pc;
    uint64_t carg_1;
    uint64_t carg_2;
    uint64_t carg_3;
    uint64_t carg_4;
    uint64_t gpr6;
    uint64_t gpr7;
    uint64_t gpr8;
    uint64_t gpr9;
    uint64_t gpr10;
    uint64_t gpr11;
    uint64_t gpr12;
    uint64_t gpr13;
    uint64_t gpr14;
    uint64_t gpr15;
    uint64_t cfarg_1;
    uint64_t cfarg_2;
    uint64_t cfarg_3;
    uint64_t cfarg_4;
    uint64_t remaining_cargs;
  };

  enum {
    z_abi_160_size = 160
  };

  #define _z_abi(_component) \
          (offset_of(frame::z_abi_160, _component))

  struct z_abi_160_spill : z_abi_160 {
   // Additional spill slots. Use as 'offset_of(z_abi_160_spill, spill[n])'.
    uint64_t spill[0];
    // Aligned to frame::alignment_in_bytes (16).
  };


  // non-volatile GPRs:

  struct z_spill_nonvolatiles {
    uint64_t r6;
    uint64_t r7;
    uint64_t r8;
    uint64_t r9;
    uint64_t r10;
    uint64_t r11;
    uint64_t r12;
    uint64_t r13;
  };

  enum {
    z_spill_nonvolatiles_size = sizeof(z_spill_nonvolatiles)
  };

  #define _z_spill_nonvolatiles_neg(_component) \
          (-frame::z_spill_nonvolatiles_size + offset_of(frame::z_spill_nonvolatiles, _component))

  // Frame layout for the Java template interpreter on z/Architecture.
  //
  // In these figures the stack grows upwards, while memory grows
  // downwards. Square brackets denote regions possibly larger than
  // single 64 bit slots.
  //
  // STACK (no JNI, no compiled code, no library calls, template interpreter is active):
  //
  //   0       [TOP_IJAVA_FRAME]
  //           [PARENT_IJAVA_FRAME]
  //           [PARENT_IJAVA_FRAME]
  //           ...
  //           [PARENT_IJAVA_FRAME]
  //           [ENTRY_FRAME]
  //           [C_FRAME]
  //           ...
  //           [C_FRAME]
  //
  // TOP_IJAVA_FRAME:
  //
  //   0       [TOP_IJAVA_FRAME_ABI]
  //   16      [operand stack]
  //           [monitors]      (optional)
  //           [IJAVA_STATE]
  //           note: Own locals are located in the caller frame.
  //
  // PARENT_IJAVA_FRAME:
  //
  //   0       [PARENT_IJAVA_FRAME_ABI]
  //           [callee's locals w/o arguments]
  //           [outgoing arguments]
  //           [used part of operand stack w/o arguments]
  //           [monitors]      (optional)
  //           [IJAVA_STATE]
  //
  // ENTRY_FRAME:
  //
  //   0       [PARENT_IJAVA_FRAME_ABI]
  //           [callee's locals w/o arguments]
  //           [outgoing arguments]
  //           [ENTRY_FRAME_LOCALS]
  //
  // TOP_IJAVA_FRAME_ABI:
  //
  //   0       [ABI_160]
  //
  //
  // PARENT_IJAVA_FRAME_ABI:
  //
  //   0       [ABI_16]
  //
  // IJAVA_STATE:
  //
  //   0       method
  //   8       locals
  //           monitors               : monitor block top (i.e. lowest address)
  //           cpoolCache
  //           bcp
  //           mdx
  //           esp                    : Points to first slot above operands.
  //           sender_sp              : See comment in z_ijava_state.
  //           top_frame_sp           : Own SP before modification by i2c adapter.
  //           oop_tmp
  //           lresult
  //           fresult
  //
  // EXAMPLE:
  // ---------
  //
  // 3 monitors, 5 operand stack slots max. / 3 allocated
  //
  //    F0      callers_sp               <- Z_SP (callers_sp == Z_fp (own fp))
  //            return_pc
  //            [rest of ABI_160]
  //           /slot 4: free
  //    oper. | slot 3: free             <- Z_esp points to first free slot
  //    stack | slot 2: ref val v2                caches IJAVA_STATE.esp
  //          | slot 1: unused
  //           \slot 0: long val v1
  //           /slot 5                   <- IJAVA_STATE.monitors  = monitor block top
  //          | slot 4
  //  monitors| slot 3
  //          | slot 2
  //          | slot 1
  //           \slot 0
  //            [IJAVA_STATE]            <- monitor block bot (points to first byte in IJAVA_STATE)
  //    F1      [PARENT_IJAVA_FRAME_ABI] <- Z_fp (== *Z_SP, points to slot just below IJAVA_STATE)
  //            [F0's locals]            <- Z_locals, locals[i] := *(Z_locals - i*BytesPerWord)
  //            [F1's operand stack]
  //            [F1's monitors]      (optional)
  //            [IJAVA_STATE]

 public:

  // PARENT_IJAVA_FRAME_ABI

  struct z_parent_ijava_frame_abi : z_abi_16 {
  };

  enum {
    z_parent_ijava_frame_abi_size = sizeof(z_parent_ijava_frame_abi)
  };

  #define _z_parent_ijava_frame_abi(_component) \
          (offset_of(frame::z_parent_ijava_frame_abi, _component))

  // TOP_IJAVA_FRAME_ABI

  struct z_top_ijava_frame_abi : z_abi_160 {
  };

  enum {
    z_top_ijava_frame_abi_size = sizeof(z_top_ijava_frame_abi)
  };

  #define _z_top_ijava_frame_abi(_component) \
          (offset_of(frame::z_top_ijava_frame_abi, _component))

  // IJAVA_STATE

  struct z_ijava_state{
    DEBUG_ONLY(uint64_t magic;) // wrong magic -> wrong state!
    uint64_t method;
    uint64_t mirror;
    uint64_t locals;       // Z_locals
    uint64_t monitors;
    uint64_t cpoolCache;
    uint64_t bcp;          // Z_bcp
    uint64_t mdx;
    uint64_t esp;          // Z_esp
    // Caller's original SP before modification by c2i adapter (if caller is compiled)
    // and before top -> parent frame conversion by the interpreter entry.
    // Note: for i2i calls a correct sender_sp is required, too, because there
    // we cannot use the caller's top_frame_sp as sp when removing the callee
    // frame (caller could be compiled or entry frame). Therefore the sender_sp
    // has to be the interpreted caller's sp as TOP_IJAVA_FRAME. See also
    // AbstractInterpreter::layout_activation() used by deoptimization.
    uint64_t sender_sp;
    // Own SP before modification by i2c adapter and top-2-parent-resize
    // by interpreted callee.
    uint64_t top_frame_sp;
    // Slots only needed for native calls. Maybe better to move elsewhere.
    uint64_t oop_tmp;
    uint64_t lresult;
    uint64_t fresult;
  };

  enum  {
    z_ijava_state_size = sizeof(z_ijava_state)
  };

#ifdef ASSERT
  enum  {
    z_istate_magic_number = 0x900d // ~= good magic
  };
#endif

#define _z_ijava_state_neg(_component) \
         (int) (-frame::z_ijava_state_size + offset_of(frame::z_ijava_state, _component))

  // ENTRY_FRAME

  struct z_entry_frame_locals {
    uint64_t call_wrapper_address;
    uint64_t result_address;
    uint64_t result_type;
    uint64_t arguments_tos_address;
    // Callee saved registers are spilled to caller frame.
    // Caller must have z_abi_160.
  };

  enum {
    z_entry_frame_locals_size = sizeof(z_entry_frame_locals)
  };

  #define _z_entry_frame_locals_neg(_component) \
          (int) (-frame::z_entry_frame_locals_size + offset_of(frame::z_entry_frame_locals, _component))

  //  Frame layout for JIT generated methods
  //
  //  In these figures the stack grows upwards, while memory grows
  //  downwards. Square brackets denote regions possibly larger than single
  //  64 bit slots.
  //
  //  STACK (interpreted Java calls JIT generated Java):
  //
  //          [JIT_FRAME]                                <-- SP (mod 16 = 0)
  //          [TOP_IJAVA_FRAME]
  //         ...
  //
  //
  //  JIT_FRAME (is a C frame according to z/Architecture ABI):
  //
  //          [out_preserve]
  //          [out_args]
  //          [spills]
  //          [monitor] (optional)
  //       ...
  //          [monitor] (optional)
  //          [in_preserve] added / removed by prolog / epilog

 public:

   struct z_top_jit_abi_32 {
     uint64_t callers_sp;
     uint64_t return_pc;
     uint64_t toc;
     uint64_t tmp;
   };

  #define _z_top_jit_abi(_component) \
          (offset_of(frame::z_top_jit_abi_32, _component))

  struct jit_monitor {
        uint64_t monitor[1];
  };

  struct jit_in_preserve {
    // Used to provide a z/Architecture ABI on top of a jit frame.
    // nothing to add here!
  };

  struct jit_out_preserve : z_top_jit_abi_32 {
    // Nothing to add here!
  };

  enum {
    z_jit_out_preserve_size = sizeof(jit_out_preserve)
  };

  typedef enum {
     jit_monitor_size_in_4_byte_units = sizeof(jit_monitor) / 4,

     // Stack alignment requirement. Log_2 of alignment size in bits.
     // log_2(16*8 bits) = 7.
     jit_log_2_of_stack_alignment_in_bits = 7,

     jit_out_preserve_size_in_4_byte_units = sizeof(jit_out_preserve) / 4,

     jit_in_preserve_size_in_4_byte_units = sizeof(jit_in_preserve) / 4
  } jit_frame_constants;


  // C2I adapter frames:
  //
  // STACK (interpreted called from compiled, on entry to frame manager):
  //
  //       [TOP_C2I_FRAME]
  //       [JIT_FRAME]
  //       ...
  //
  //
  // STACK (interpreted called from compiled, after interpreter has been pushed):
  //
  //       [TOP_IJAVA_FRAME]
  //       [PARENT_C2I_FRAME]
  //       [JIT_FRAME]
  //       ...
  //
  //
  // TOP_C2I_FRAME:
  //
  //       [TOP_IJAVA_FRAME_ABI]
  //       [outgoing Java arguments]
  //       alignment (optional)
  //
  //
  // PARENT_C2I_FRAME:
  //
  //       [PARENT_IJAVA_FRAME_ABI]
  //       alignment (optional)
  //       [callee's locals w/o arguments]
  //       [outgoing Java arguments]
  //       alignment (optional)

 private:

  //  STACK:
  //            ...
  //            [THIS_FRAME]             <-- this._sp (stack pointer for this frame)
  //            [CALLER_FRAME]           <-- this.fp() (_sp of caller's frame)
  //            ...
  //

  // NOTE: Stack pointer is now held in the base class, so remove it from here.

  // Needed by deoptimization.
  intptr_t* _unextended_sp;

  // Frame pointer for this frame.
  intptr_t* _fp;

 public:

  // Interface for all frames:

  // Accessors

  inline intptr_t* fp() const { return _fp; }

 private:

  inline void find_codeblob_and_set_pc_and_deopt_state(address pc);

 // Constructors

 public:
  inline frame(intptr_t* sp);
  // To be used, if sp was not extended to match callee's calling convention.
  inline frame(intptr_t* sp, address pc);
  inline frame(intptr_t* sp, address pc, intptr_t* unextended_sp);

  // Access frame via stack pointer.
  inline intptr_t* sp_addr_at(int index) const  { return &sp()[index]; }
  inline intptr_t  sp_at(     int index) const  { return *sp_addr_at(index); }

  // Access ABIs.
  inline z_abi_16*  own_abi()     const { return (z_abi_16*) sp(); }
  inline z_abi_160* callers_abi() const { return (z_abi_160*) fp(); }

 private:

  intptr_t* compiled_sender_sp(CodeBlob* cb) const;
  address*  compiled_sender_pc_addr(CodeBlob* cb) const;

  address* sender_pc_addr(void) const;

 public:

  // Additional interface for interpreter frames:
  static int interpreter_frame_interpreterstate_size_in_bytes();
  static int interpreter_frame_monitor_size_in_bytes();


  // template interpreter state
  inline z_ijava_state* ijava_state_unchecked() const;

 private:

  inline z_ijava_state* ijava_state() const;

  // Where z_ijava_state.monitors is saved.
  inline BasicObjectLock**  interpreter_frame_monitors_addr() const;
  // Where z_ijava_state.esp is saved.
  inline intptr_t** interpreter_frame_esp_addr() const;

 public:
  inline intptr_t* interpreter_frame_top_frame_sp();
  inline void interpreter_frame_set_tos_address(intptr_t* x);
  inline void interpreter_frame_set_top_frame_sp(intptr_t* top_frame_sp);
  inline void interpreter_frame_set_sender_sp(intptr_t* sender_sp);
#ifdef ASSERT
  inline void interpreter_frame_set_magic();
#endif

  // monitors:

  // Next two functions read and write z_ijava_state.monitors.
 private:
  inline BasicObjectLock* interpreter_frame_monitors() const;
  inline void interpreter_frame_set_monitors(BasicObjectLock* monitors);

 public:

  // Additional interface for entry frames:
  inline z_entry_frame_locals* entry_frame_locals() const {
    return (z_entry_frame_locals*) (((address) fp()) - z_entry_frame_locals_size);
  }

 public:

  // Get caller pc from stack slot of gpr14.
  address native_sender_pc() const;
  // Get caller pc from stack slot of gpr10.
  address callstub_sender_pc() const;

  // Dump all frames starting at a given C stack pointer.
  // max_frames: Limit number of traced frames.
  //             <= 0 --> full trace
  //             > 0  --> trace the #max_frames topmost frames
  static void back_trace(outputStream* st, intptr_t* start_sp, intptr_t* top_pc,
                         unsigned long flags, int max_frames = 0);

  enum {
    // This enum value specifies the offset from the pc remembered by
    // call instructions to the location where control returns to
    // after a normal return. Most architectures remember the return
    // location directly, i.e. the offset is zero. This is the case
    // for z/Architecture, too.
    //
    // Normal return address is the instruction following the branch.
    pc_return_offset =  0,
  };

  static jint interpreter_frame_expression_stack_direction() { return -1; }

#endif // CPU_S390_FRAME_S390_HPP
