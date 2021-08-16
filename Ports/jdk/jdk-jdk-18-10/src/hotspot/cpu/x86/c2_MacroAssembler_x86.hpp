/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_C2_MACROASSEMBLER_X86_HPP
#define CPU_X86_C2_MACROASSEMBLER_X86_HPP

// C2_MacroAssembler contains high-level macros for C2

public:
  Assembler::AvxVectorLen vector_length_encoding(int vlen_in_bytes);

  // special instructions for EVEX
  void setvectmask(Register dst, Register src, KRegister mask);
  void restorevectmask(KRegister mask);

  // Code used by cmpFastLock and cmpFastUnlock mach instructions in .ad file.
  // See full desription in macroAssembler_x86.cpp.
  void fast_lock(Register obj, Register box, Register tmp,
                 Register scr, Register cx1, Register cx2,
                 RTMLockingCounters* rtm_counters,
                 RTMLockingCounters* stack_rtm_counters,
                 Metadata* method_data,
                 bool use_rtm, bool profile_rtm);
  void fast_unlock(Register obj, Register box, Register tmp, bool use_rtm);

#if INCLUDE_RTM_OPT
  void rtm_counters_update(Register abort_status, Register rtm_counters);
  void branch_on_random_using_rdtsc(Register tmp, Register scr, int count, Label& brLabel);
  void rtm_abort_ratio_calculation(Register tmp, Register rtm_counters_reg,
                                   RTMLockingCounters* rtm_counters,
                                   Metadata* method_data);
  void rtm_profiling(Register abort_status_Reg, Register rtm_counters_Reg,
                     RTMLockingCounters* rtm_counters, Metadata* method_data, bool profile_rtm);
  void rtm_retry_lock_on_abort(Register retry_count, Register abort_status, Label& retryLabel);
  void rtm_retry_lock_on_busy(Register retry_count, Register box, Register tmp, Register scr, Label& retryLabel);
  void rtm_stack_locking(Register obj, Register tmp, Register scr,
                         Register retry_on_abort_count,
                         RTMLockingCounters* stack_rtm_counters,
                         Metadata* method_data, bool profile_rtm,
                         Label& DONE_LABEL, Label& IsInflated);
  void rtm_inflated_locking(Register obj, Register box, Register tmp,
                            Register scr, Register retry_on_busy_count,
                            Register retry_on_abort_count,
                            RTMLockingCounters* rtm_counters,
                            Metadata* method_data, bool profile_rtm,
                            Label& DONE_LABEL);
#endif

  // Generic instructions support for use in .ad files C2 code generation
  void vabsnegd(int opcode, XMMRegister dst, XMMRegister src, Register scr);
  void vabsnegd(int opcode, XMMRegister dst, XMMRegister src, int vector_len, Register scr);
  void vabsnegf(int opcode, XMMRegister dst, XMMRegister src, Register scr);
  void vabsnegf(int opcode, XMMRegister dst, XMMRegister src, int vector_len, Register scr);

  void pminmax(int opcode, BasicType elem_bt, XMMRegister dst, XMMRegister src,
               XMMRegister tmp = xnoreg);
  void vpminmax(int opcode, BasicType elem_bt,
                XMMRegister dst, XMMRegister src1, XMMRegister src2,
                int vlen_enc);

  void vminmax_fp(int opcode, BasicType elem_bt,
                  XMMRegister dst, XMMRegister a, XMMRegister b,
                  XMMRegister tmp, XMMRegister atmp, XMMRegister btmp,
                  int vlen_enc);
  void evminmax_fp(int opcode, BasicType elem_bt,
                   XMMRegister dst, XMMRegister a, XMMRegister b,
                   KRegister ktmp, XMMRegister atmp, XMMRegister btmp,
                   int vlen_enc);

  void signum_fp(int opcode, XMMRegister dst,
                 XMMRegister zero, XMMRegister one,
                 Register scratch);

  void vextendbw(bool sign, XMMRegister dst, XMMRegister src, int vector_len);
  void vextendbw(bool sign, XMMRegister dst, XMMRegister src);
  void vextendbd(bool sign, XMMRegister dst, XMMRegister src, int vector_len);
  void vextendwd(bool sign, XMMRegister dst, XMMRegister src, int vector_len);

  void vshiftd(int opcode, XMMRegister dst, XMMRegister shift);
  void vshiftd_imm(int opcode, XMMRegister dst, int shift);
  void vshiftd(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc);
  void vshiftd_imm(int opcode, XMMRegister dst, XMMRegister nds, int shift, int vector_len);
  void vshiftw(int opcode, XMMRegister dst, XMMRegister shift);
  void vshiftw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc);
  void vshiftq(int opcode, XMMRegister dst, XMMRegister shift);
  void vshiftq_imm(int opcode, XMMRegister dst, int shift);
  void vshiftq(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc);
  void vshiftq_imm(int opcode, XMMRegister dst, XMMRegister nds, int shift, int vector_len);

  void vprotate_imm(int opcode, BasicType etype, XMMRegister dst, XMMRegister src, int shift, int vector_len);
  void vprotate_var(int opcode, BasicType etype, XMMRegister dst, XMMRegister src, XMMRegister shift, int vector_len);

  void varshiftd(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc);
  void varshiftw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc);
  void varshiftq(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc, XMMRegister vtmp = xnoreg);
  void varshiftbw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vector_len, XMMRegister vtmp, Register scratch);
  void evarshiftb(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vector_len, XMMRegister vtmp, Register scratch);

  void insert(BasicType typ, XMMRegister dst, Register val, int idx);
  void vinsert(BasicType typ, XMMRegister dst, XMMRegister src, Register val, int idx);
  void vgather(BasicType typ, XMMRegister dst, Register base, XMMRegister idx, XMMRegister mask, int vector_len);
  void evgather(BasicType typ, XMMRegister dst, KRegister mask, Register base, XMMRegister idx, int vector_len);
  void evscatter(BasicType typ, Register base, XMMRegister idx, KRegister mask, XMMRegister src, int vector_len);

  void evmovdqu(BasicType type, KRegister kmask, XMMRegister dst, Address src, int vector_len);
  void evmovdqu(BasicType type, KRegister kmask, Address dst, XMMRegister src, int vector_len);

  // extract
  void extract(BasicType typ, Register dst, XMMRegister src, int idx);
  XMMRegister get_lane(BasicType typ, XMMRegister dst, XMMRegister src, int elemindex);
  void get_elem(BasicType typ, Register dst, XMMRegister src, int elemindex);
  void get_elem(BasicType typ, XMMRegister dst, XMMRegister src, int elemindex, Register tmp = noreg, XMMRegister vtmp = xnoreg);

  // vector test
  void vectortest(int bt, int vlen, XMMRegister src1, XMMRegister src2,
                  XMMRegister vtmp1 = xnoreg, XMMRegister vtmp2 = xnoreg, KRegister mask = knoreg);

  // blend
  void evpcmp(BasicType typ, KRegister kdmask, KRegister ksmask, XMMRegister src1, AddressLiteral adr, int comparison, int vector_len, Register scratch = rscratch1);
  void evpcmp(BasicType typ, KRegister kdmask, KRegister ksmask, XMMRegister src1, XMMRegister src2, int comparison, int vector_len);
  void evpblend(BasicType typ, XMMRegister dst, KRegister kmask, XMMRegister src1, XMMRegister src2, bool merge, int vector_len);

  void load_vector_mask(XMMRegister dst, XMMRegister src, int vlen_in_bytes, BasicType elem_bt, bool is_legacy);
  void load_iota_indices(XMMRegister dst, Register scratch, int vlen_in_bytes);

  // vector compare
  void vpcmpu(BasicType typ, XMMRegister dst, XMMRegister src1, XMMRegister src2, ComparisonPredicate comparison, int vlen_in_bytes,
              XMMRegister vtmp1, XMMRegister vtmp2, Register scratch);
  void vpcmpu32(BasicType typ, XMMRegister dst, XMMRegister src1, XMMRegister src2, ComparisonPredicate comparison, int vlen_in_bytes,
                XMMRegister vtmp1, XMMRegister vtmp2, XMMRegister vtmp3, Register scratch);

  // Reductions for vectors of bytes, shorts, ints, longs, floats, and doubles.

  // dst = src1  reduce(op, src2) using vtmp as temps
  void reduceI(int opcode, int vlen, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
#ifdef _LP64
  void reduceL(int opcode, int vlen, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void genmask(KRegister dst, Register len, Register temp);
#endif // _LP64

  // dst = reduce(op, src2) using vtmp as temps
  void reduce_fp(int opcode, int vlen,
                 XMMRegister dst, XMMRegister src,
                 XMMRegister vtmp1, XMMRegister vtmp2 = xnoreg);
  void reduceB(int opcode, int vlen, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void mulreduceB(int opcode, int vlen, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduceS(int opcode, int vlen, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduceFloatMinMax(int opcode, int vlen, bool is_dst_valid,
                         XMMRegister dst, XMMRegister src,
                         XMMRegister tmp, XMMRegister atmp, XMMRegister btmp, XMMRegister xmm_0, XMMRegister xmm_1 = xnoreg);
  void reduceDoubleMinMax(int opcode, int vlen, bool is_dst_valid,
                          XMMRegister dst, XMMRegister src,
                          XMMRegister tmp, XMMRegister atmp, XMMRegister btmp, XMMRegister xmm_0, XMMRegister xmm_1 = xnoreg);
 private:
  void reduceF(int opcode, int vlen, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduceD(int opcode, int vlen, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);

  // Int Reduction
  void reduce2I (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce4I (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce8I (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce16I(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);

  // Byte Reduction
  void reduce8B (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce16B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce32B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce64B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void mulreduce8B (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void mulreduce16B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void mulreduce32B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void mulreduce64B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);

  // Short Reduction
  void reduce4S (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce8S (int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce16S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce32S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);

  // Long Reduction
#ifdef _LP64
  void reduce2L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce4L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce8L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2);
#endif // _LP64

  // Float Reduction
  void reduce2F (int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp);
  void reduce4F (int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp);
  void reduce8F (int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce16F(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);

  // Double Reduction
  void reduce2D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp);
  void reduce4D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);
  void reduce8D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2);

  // Base reduction instruction
  void reduce_operation_128(BasicType typ, int opcode, XMMRegister dst, XMMRegister src);
  void reduce_operation_256(BasicType typ, int opcode, XMMRegister dst, XMMRegister src1, XMMRegister src2);

 public:
#ifdef _LP64
  void vector_mask_operation(int opc, Register dst, XMMRegister mask, XMMRegister xtmp, Register tmp,
                             KRegister ktmp, int masklen, int vec_enc);

  void vector_mask_operation(int opc, Register dst, XMMRegister mask, XMMRegister xtmp, XMMRegister xtmp1,
                             Register tmp, int masklen, int vec_enc);
#endif
  void string_indexof_char(Register str1, Register cnt1, Register ch, Register result,
                           XMMRegister vec1, XMMRegister vec2, XMMRegister vec3, Register tmp);

  void stringL_indexof_char(Register str1, Register cnt1, Register ch, Register result,
                           XMMRegister vec1, XMMRegister vec2, XMMRegister vec3, Register tmp);

  // IndexOf strings.
  // Small strings are loaded through stack if they cross page boundary.
  void string_indexof(Register str1, Register str2,
                      Register cnt1, Register cnt2,
                      int int_cnt2,  Register result,
                      XMMRegister vec, Register tmp,
                      int ae);

  // IndexOf for constant substrings with size >= 8 elements
  // which don't need to be loaded through stack.
  void string_indexofC8(Register str1, Register str2,
                      Register cnt1, Register cnt2,
                      int int_cnt2,  Register result,
                      XMMRegister vec, Register tmp,
                      int ae);

    // Smallest code: we don't need to load through stack,
    // check string tail.

  // helper function for string_compare
  void load_next_elements(Register elem1, Register elem2, Register str1, Register str2,
                          Address::ScaleFactor scale, Address::ScaleFactor scale1,
                          Address::ScaleFactor scale2, Register index, int ae);
  // Compare strings.
  void string_compare(Register str1, Register str2,
                      Register cnt1, Register cnt2, Register result,
                      XMMRegister vec1, int ae, KRegister mask = knoreg);

  // Search for Non-ASCII character (Negative byte value) in a byte array,
  // return true if it has any and false otherwise.
  void has_negatives(Register ary1, Register len,
                     Register result, Register tmp1,
                     XMMRegister vec1, XMMRegister vec2, KRegister mask1 = knoreg, KRegister mask2 = knoreg);

  // Compare char[] or byte[] arrays.
  void arrays_equals(bool is_array_equ, Register ary1, Register ary2,
                     Register limit, Register result, Register chr,
                     XMMRegister vec1, XMMRegister vec2, bool is_char, KRegister mask = knoreg);

#endif // CPU_X86_C2_MACROASSEMBLER_X86_HPP
