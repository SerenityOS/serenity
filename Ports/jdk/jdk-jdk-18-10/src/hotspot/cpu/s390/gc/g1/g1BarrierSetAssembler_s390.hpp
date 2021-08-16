/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

#ifndef CPU_S390_GC_G1_G1BARRIERSETASSEMBLER_S390_HPP
#define CPU_S390_GC_G1_G1BARRIERSETASSEMBLER_S390_HPP

#include "asm/macroAssembler.hpp"
#include "gc/shared/modRefBarrierSetAssembler.hpp"
#include "utilities/macros.hpp"

class LIR_Assembler;
class StubAssembler;
class G1PreBarrierStub;
class G1PostBarrierStub;

class G1BarrierSetAssembler: public ModRefBarrierSetAssembler {
 protected:
  virtual void gen_write_ref_array_pre_barrier(MacroAssembler* masm, DecoratorSet decorators, Register addr, Register count);
  virtual void gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators, Register addr, Register count,
                                                bool do_return);

  void g1_write_barrier_pre(MacroAssembler*    masm, DecoratorSet decorators,
                            const Address*     obj,           // Address of oop or NULL if pre-loaded.
                            Register           Rpre_val,      // Ideally, this is a non-volatile register.
                            Register           Rval,          // Will be preserved.
                            Register           Rtmp1,         // If Rpre_val is volatile, either Rtmp1
                            Register           Rtmp2,         // or Rtmp2 has to be non-volatile.
                            bool               pre_val_needed); // Save Rpre_val across runtime call, caller uses it.

  void g1_write_barrier_post(MacroAssembler* masm, DecoratorSet decorators, Register Rstore_addr, Register Rnew_val,
                             Register Rtmp1, Register Rtmp2, Register Rtmp3);

  virtual void oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                            const Address& dst, Register val, Register tmp1, Register tmp2, Register tmp3);

 public:
#ifdef COMPILER1
  void gen_pre_barrier_stub(LIR_Assembler* ce, G1PreBarrierStub* stub);
  void gen_post_barrier_stub(LIR_Assembler* ce, G1PostBarrierStub* stub);

  void generate_c1_pre_barrier_runtime_stub(StubAssembler* sasm);
  void generate_c1_post_barrier_runtime_stub(StubAssembler* sasm);
#endif

  virtual void load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                       const Address& src, Register dst, Register tmp1, Register tmp2, Label *L_handle_null = NULL);

  virtual void resolve_jobject(MacroAssembler* masm, Register value, Register tmp1, Register tmp2);
};

#endif // CPU_S390_GC_G1_G1BARRIERSETASSEMBLER_S390_HPP
