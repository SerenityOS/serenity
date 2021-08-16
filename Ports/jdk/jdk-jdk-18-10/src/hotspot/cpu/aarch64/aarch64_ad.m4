dnl Copyright (c) 2019, 2020, Red Hat Inc. All rights reserved.
dnl DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
dnl
dnl This code is free software; you can redistribute it and/or modify it
dnl under the terms of the GNU General Public License version 2 only, as
dnl published by the Free Software Foundation.
dnl
dnl This code is distributed in the hope that it will be useful, but WITHOUT
dnl ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
dnl FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
dnl version 2 for more details (a copy is included in the LICENSE file that
dnl accompanied this code).
dnl
dnl You should have received a copy of the GNU General Public License version
dnl 2 along with this work; if not, write to the Free Software Foundation,
dnl Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
dnl
dnl Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
dnl or visit www.oracle.com if you need additional information or have any
dnl questions.
dnl
dnl
dnl Process this file with m4 aarch64_ad.m4 to generate instructions used in
dnl aarch64.ad:
dnl 1. the arithmetic
dnl 2. shift patterns
dnl
// BEGIN This section of the file is automatically generated. Do not edit --------------
// This section is generated from aarch64_ad.m4
dnl
define(`ORL2I', `ifelse($1,I,orL2I)')
dnl
define(`BASE_SHIFT_INSN',
`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $2$1_reg_$4_reg(iReg$1NoSp dst,
                         iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2,
                         immI src3) %{
  match(Set dst ($2$1 src1 (ifelse($4, RotateRight, $4, $4$1) src2 src3)));

  ins_cost(1.9 * INSN_COST);
  format %{ "$3  $dst, $src1, $src2, $5 $src3" %}

  ins_encode %{
    __ $3(as_Register($dst$$reg),
              as_Register($src1$$reg),
              as_Register($src2$$reg),
              Assembler::$5,
              $src3$$constant & ifelse($1,I,0x1f,0x3f));
  %}

  ins_pipe(ialu_reg_reg_shift);
%}
')dnl
define(`BASE_INVERTED_INSN',
`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $2$1_reg_not_reg(iReg$1NoSp dst,
                         iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, imm$1_M1 m1) %{
dnl This ifelse is because hotspot reassociates (xor (xor ..)..)
dnl into this canonical form.
  ifelse($2,Xor,
    match(Set dst (Xor$1 m1 (Xor$1 src2 src1)));,
    match(Set dst ($2$1 src1 (Xor$1 src2 m1)));)
  ins_cost(INSN_COST);
  format %{ "$3  $dst, $src1, $src2" %}

  ins_encode %{
    __ $3(as_Register($dst$$reg),
              as_Register($src1$$reg),
              as_Register($src2$$reg),
              Assembler::LSL, 0);
  %}

  ins_pipe(ialu_reg_reg);
%}
')dnl
define(`INVERTED_SHIFT_INSN',
`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
// val ifelse($2, Xor, ^, $2, And, &, |) (-1 ^ (val ifelse($4, RShift, >>, $4, LShift, <<, $4, URShift, >>>, ror) shift)) ==> $3
instruct $2$1_reg_$4_not_reg(iReg$1NoSp dst,
                         iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2,
                         immI src3, imm$1_M1 src4) %{
dnl This ifelse is because hotspot reassociates (xor (xor ..)..)
dnl into this canonical form.
  ifelse($2,Xor,
    match(Set dst ($2$1 src4 (Xor$1(ifelse($4, RotateRight, $4, $4$1) src2 src3) src1)));,
    match(Set dst ($2$1 src1 (Xor$1(ifelse($4, RotateRight, $4, $4$1) src2 src3) src4)));)
  ins_cost(1.9 * INSN_COST);
  format %{ "$3  $dst, $src1, $src2, $5 $src3" %}

  ins_encode %{
    __ $3(as_Register($dst$$reg),
              as_Register($src1$$reg),
              as_Register($src2$$reg),
              Assembler::$5,
              $src3$$constant & ifelse($1,I,0x1f,0x3f));
  %}

  ins_pipe(ialu_reg_reg_shift);
%}
')dnl
define(`NOT_INSN',
`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct reg$1_not_reg(iReg$1NoSp dst,
                         iReg$1`'ORL2I($1) src1, imm$1_M1 m1,
                         rFlagsReg cr) %{
  match(Set dst (Xor$1 src1 m1));
  ins_cost(INSN_COST);
  format %{ "$2  $dst, $src1, zr" %}

  ins_encode %{
    __ $2(as_Register($dst$$reg),
              as_Register($src1$$reg),
              zr,
              Assembler::LSL, 0);
  %}

  ins_pipe(ialu_reg);
%}
')dnl
dnl
define(`BOTH_SHIFT_INSNS',
`BASE_SHIFT_INSN(I, $1, ifelse($2,andr,andw,$2w), $3, $4)
BASE_SHIFT_INSN(L, $1, $2, $3, $4)')dnl
dnl
define(`BOTH_INVERTED_INSNS',
`BASE_INVERTED_INSN(I, $1, $2w, $3, $4)
BASE_INVERTED_INSN(L, $1, $2, $3, $4)')dnl
dnl
define(`BOTH_INVERTED_SHIFT_INSNS',
`INVERTED_SHIFT_INSN(I, $1, $2w, $3, $4)
INVERTED_SHIFT_INSN(L, $1, $2, $3, $4)')dnl
dnl
define(`ALL_SHIFT_KINDS_WITHOUT_ROR',
`BOTH_SHIFT_INSNS($1, $2, URShift, LSR)
BOTH_SHIFT_INSNS($1, $2, RShift, ASR)
BOTH_SHIFT_INSNS($1, $2, LShift, LSL)')dnl
dnl
define(`ALL_SHIFT_KINDS',
`ALL_SHIFT_KINDS_WITHOUT_ROR($1, $2)
BOTH_SHIFT_INSNS($1, $2, RotateRight, ROR)')dnl
dnl
define(`ALL_INVERTED_SHIFT_KINDS',
`BOTH_INVERTED_SHIFT_INSNS($1, $2, URShift, LSR)
BOTH_INVERTED_SHIFT_INSNS($1, $2, RShift, ASR)
BOTH_INVERTED_SHIFT_INSNS($1, $2, RotateRight, ROR)
BOTH_INVERTED_SHIFT_INSNS($1, $2, LShift, LSL)')dnl
dnl
NOT_INSN(L, eon)
NOT_INSN(I, eonw)
BOTH_INVERTED_INSNS(And, bic)
BOTH_INVERTED_INSNS(Or, orn)
BOTH_INVERTED_INSNS(Xor, eon)
ALL_INVERTED_SHIFT_KINDS(And, bic)
ALL_INVERTED_SHIFT_KINDS(Xor, eon)
ALL_INVERTED_SHIFT_KINDS(Or, orn)
ALL_SHIFT_KINDS(And, andr)
ALL_SHIFT_KINDS(Xor, eor)
ALL_SHIFT_KINDS(Or, orr)
ALL_SHIFT_KINDS_WITHOUT_ROR(Add, add)
ALL_SHIFT_KINDS_WITHOUT_ROR(Sub, sub)
dnl
dnl EXTEND mode, rshift_op, src, lshift_count, rshift_count
define(`EXTEND', `($2$1 (LShift$1 $3 $4) $5)') dnl
define(`BFM_INSN',`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE

// Shift Left followed by Shift Right.
// This idiom is used by the compiler for the i2b bytecode etc.
instruct $4$1(iReg$1NoSp dst, iReg$1`'ORL2I($1) src, immI lshift_count, immI rshift_count)
%{
  match(Set dst EXTEND($1, $3, src, lshift_count, rshift_count));
  ins_cost(INSN_COST * 2);
  format %{ "$4  $dst, $src, $rshift_count - $lshift_count, #$2 - $lshift_count" %}
  ins_encode %{
    int lshift = $lshift_count$$constant & $2;
    int rshift = $rshift_count$$constant & $2;
    int s = $2 - lshift;
    int r = (rshift - lshift) & $2;
    __ $4(as_Register($dst$$reg),
            as_Register($src$$reg),
            r, s);
  %}

  ins_pipe(ialu_reg_shift);
%}
')
BFM_INSN(L, 63, RShift, sbfm)
BFM_INSN(I, 31, RShift, sbfmw)
BFM_INSN(L, 63, URShift, ubfm)
BFM_INSN(I, 31, URShift, ubfmw)
dnl
// Bitfield extract with shift & mask
define(`BFX_INSN',
`// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3$1(iReg$1NoSp dst, iReg$1`'ORL2I($1) src, immI rshift, imm$1_bitmask mask)
%{
  match(Set dst (And$1 ($2$1 src rshift) mask));
  // Make sure we are not going to exceed what $3 can do.
  predicate((exact_log2$6(n->in(2)->get_$5() + 1) + (n->in(1)->in(2)->get_int() & $4)) <= ($4 + 1));

  ins_cost(INSN_COST);
  format %{ "$3 $dst, $src, $rshift, $mask" %}
  ins_encode %{
    int rshift = $rshift$$constant & $4;
    intptr_t mask = $mask$$constant;
    int width = exact_log2$6(mask+1);
    __ $3(as_Register($dst$$reg),
            as_Register($src$$reg), rshift, width);
  %}
  ins_pipe(ialu_reg_shift);
%}
')
BFX_INSN(I, URShift, ubfxw, 31, int)
BFX_INSN(L, URShift, ubfx,  63, long, _long)

// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE

// We can use ubfx when extending an And with a mask when we know mask
// is positive.  We know that because immI_bitmask guarantees it.
instruct ubfxIConvI2L(iRegLNoSp dst, iRegIorL2I src, immI rshift, immI_bitmask mask)
%{
  match(Set dst (ConvI2L (AndI (URShiftI src rshift) mask)));
  // Make sure we are not going to exceed what ubfxw can do.
  predicate((exact_log2(n->in(1)->in(2)->get_int() + 1) + (n->in(1)->in(1)->in(2)->get_int() & 31)) <= (31 + 1));

  ins_cost(INSN_COST * 2);
  format %{ "ubfx $dst, $src, $rshift, $mask" %}
  ins_encode %{
    int rshift = $rshift$$constant & 31;
    intptr_t mask = $mask$$constant;
    int width = exact_log2(mask+1);
    __ ubfx(as_Register($dst$$reg),
            as_Register($src$$reg), rshift, width);
  %}
  ins_pipe(ialu_reg_shift);
%}

define(`UBFIZ_INSN', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE

// We can use ubfiz when masking by a positive number and then left shifting the result.
// We know that the mask is positive because imm$1_bitmask guarantees it.
instruct $3$1$8(iReg$2NoSp dst, iReg$1`'ORL2I($1) src, immI lshift, $7 mask)
%{
  ifelse($8,,
    match(Set dst (LShift$1 (And$1 src mask) lshift));,
    match(Set dst ($8 (LShift$1 (And$1 src mask) lshift)));)
  ifelse($8,,
    predicate(($6(n->in(1)->in(2)->get_$5() + 1) + (n->in(2)->get_int() & $4)) <= ($4 + 1));,
    predicate(($6(n->in(1)->in(1)->in(2)->get_$5() + 1) + (n->in(1)->in(2)->get_int() & $4)) <= 31);)

  ins_cost(INSN_COST);
  format %{ "$3 $dst, $src, $lshift, $mask" %}
  ins_encode %{
    int lshift = $lshift$$constant & $4;
    intptr_t mask = $mask$$constant;
    int width = $6(mask+1);
    __ $3(as_Register($dst$$reg),
          as_Register($src$$reg), lshift, width);
  %}
  ins_pipe(ialu_reg_shift);
%}
')
UBFIZ_INSN(I, I, ubfizw, 31, int,  exact_log2,      immI_bitmask)
UBFIZ_INSN(L, L, ubfiz,  63, long, exact_log2_long, immL_bitmask)
UBFIZ_INSN(I, L, ubfizw, 31, int,  exact_log2,      immI_bitmask,           ConvI2L)
UBFIZ_INSN(L, I, ubfiz,  63, long, exact_log2_long, immL_positive_bitmaskI, ConvL2I)

define(`BFX1_INSN', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE

// If there is a convert $1 to $2 block between and And$1 and a LShift$2, we can also match ubfiz
instruct ubfiz$1Conv$3$9(iReg$2NoSp dst, iReg$1`'ORL2I($1) src, immI lshift, $8 mask)
%{
  match(Set dst (LShift$2 (Conv$3 (And$1 src mask)) lshift));
  predicate(($4(n->in(1)->in(1)->in(2)->$5() + 1) + (n->in(2)->get_int() & $6)) <= $7);

  ins_cost(INSN_COST);
  format %{ "ubfiz $dst, $src, $lshift, $mask" %}
  ins_encode %{
    int lshift = $lshift$$constant & $6;
    intptr_t mask = $mask$$constant;
    int width = exact_log2(mask+1);
    __ ubfiz(as_Register($dst$$reg),
             as_Register($src$$reg), lshift, width);
  %}
  ins_pipe(ialu_reg_shift);
%}
')dnl
BFX1_INSN(I, L, I2L, exact_log2,      get_int,  63, (63 + 1), immI_bitmask)
BFX1_INSN(L, I, L2I, exact_log2_long, get_long, 31, 31,       immL_positive_bitmaskI, x)
// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE

// Can skip int2long conversions after AND with small bitmask
instruct ubfizIConvI2LAndI(iRegLNoSp dst, iRegI src, immI_bitmask msk)
%{
  match(Set dst (ConvI2L (AndI src msk)));
  ins_cost(INSN_COST);
  format %{ "ubfiz $dst, $src, 0, exact_log2($msk + 1) " %}
  ins_encode %{
    __ ubfiz(as_Register($dst$$reg), as_Register($src$$reg), 0, exact_log2($msk$$constant + 1));
  %}
  ins_pipe(ialu_reg_shift);
%}


// Rotations dnl
define(`EXTRACT_INSN',`
// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct extr$3$1(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, immI lshift, immI rshift, rFlagsReg cr)
%{
  match(Set dst ($3$1 (LShift$1 src1 lshift) (URShift$1 src2 rshift)));
  predicate(0 == (((n->in(1)->in(2)->get_int() & $2) + (n->in(2)->in(2)->get_int() & $2)) & $2));

  ins_cost(INSN_COST);
  format %{ "extr $dst, $src1, $src2, #$rshift" %}

  ins_encode %{
    __ $4(as_Register($dst$$reg), as_Register($src1$$reg), as_Register($src2$$reg),
            $rshift$$constant & $2);
  %}
  ins_pipe(ialu_reg_reg_extr);
%}
')dnl
EXTRACT_INSN(L, 63, Or, extr)
EXTRACT_INSN(I, 31, Or, extrw)
EXTRACT_INSN(L, 63, Add, extr)
EXTRACT_INSN(I, 31, Add, extrw)
define(ROTATE_INSN, `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $2$1_$3(iReg$1NoSp dst, iReg$1 src, ifelse($3, reg, iReg, imm)I shift)
%{
  match(Set dst (ifelse($2, ror, RotateRight, RotateLeft) src shift));

  ins_cost(INSN_COST);
  format %{ "ifelse($2, ror, ror, rol)    $dst, $src, $shift" %}

  ifelse($2, rol, ins_encode %{
     __ subw(rscratch1, zr, as_Register($shift$$reg));, ins_encode %{)
     __ ifelse($3, imm,
        ifelse($1, I, extrw, extr)(as_Register($dst$$reg), as_Register($src$$reg), as_Register($src$$reg),
               $shift$$constant & ifelse($1, I, 0x1f, 0x3f)),
        ifelse($1, I, rorvw, rorv)(as_Register($dst$$reg), as_Register($src$$reg), ifelse($2, rol, rscratch1, as_Register($shift$$reg))));
  %}
  ins_pipe(ialu_reg_reg_vshift);
%}
')dnl
ROTATE_INSN(I, ror, imm)
ROTATE_INSN(L, ror, imm)
ROTATE_INSN(I, ror, reg)
ROTATE_INSN(L, ror, reg)
ROTATE_INSN(I, rol, reg)
ROTATE_INSN(L, rol, reg)
dnl rol_imm has been transformed to ror_imm during GVN.

// Add/subtract (extended)
dnl ADD_SUB_EXTENDED(mode, size, add node, shift node, insn, shift type, wordsize
define(`ADD_SUB_CONV', `
// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3Ext$1(iReg$2NoSp dst, iReg$2`'ORL2I($2) src1, iReg$1`'ORL2I($1) src2, rFlagsReg cr)
%{
  match(Set dst ($3$2 src1 (ConvI2L src2)));
  ins_cost(INSN_COST);
  format %{ "$4  $dst, $src1, $src2, $5" %}

   ins_encode %{
     __ $4(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$5);
   %}
  ins_pipe(ialu_reg_reg);
%}')dnl
ADD_SUB_CONV(I,L,Add,add,sxtw)
ADD_SUB_CONV(I,L,Sub,sub,sxtw)
dnl
define(`ADD_SUB_EXTENDED', `
// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3Ext$1_$6(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, immI_`'eval($7-$2) lshift, immI_`'eval($7-$2) rshift, rFlagsReg cr)
%{
  match(Set dst ($3$1 src1 EXTEND($1, $4, src2, lshift, rshift)));
  ins_cost(INSN_COST);
  format %{ "$5  $dst, $src1, $src2, $6" %}

   ins_encode %{
     __ $5(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$6);
   %}
  ins_pipe(ialu_reg_reg);
%}')dnl
ADD_SUB_EXTENDED(I,16,Add,RShift,add,sxth,32)
ADD_SUB_EXTENDED(I,8,Add,RShift,add,sxtb,32)
ADD_SUB_EXTENDED(I,8,Add,URShift,add,uxtb,32)
ADD_SUB_EXTENDED(L,16,Add,RShift,add,sxth,64)
ADD_SUB_EXTENDED(L,32,Add,RShift,add,sxtw,64)
ADD_SUB_EXTENDED(L,8,Add,RShift,add,sxtb,64)
ADD_SUB_EXTENDED(L,8,Add,URShift,add,uxtb,64)
dnl
dnl ADD_SUB_ZERO_EXTEND(mode, size, add node, insn, shift type)
define(`ADD_SUB_ZERO_EXTEND', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3Ext$1_$5_and(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, imm$1_$2 mask, rFlagsReg cr)
%{
  match(Set dst ($3$1 src1 (And$1 src2 mask)));
  ins_cost(INSN_COST);
  format %{ "$4  $dst, $src1, $src2, $5" %}

   ins_encode %{
     __ $4(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$5);
   %}
  ins_pipe(ialu_reg_reg);
%}
')
dnl
ADD_SUB_ZERO_EXTEND(I,255,Add,addw,uxtb)
ADD_SUB_ZERO_EXTEND(I,65535,Add,addw,uxth)
ADD_SUB_ZERO_EXTEND(L,255,Add,add,uxtb)
ADD_SUB_ZERO_EXTEND(L,65535,Add,add,uxth)
ADD_SUB_ZERO_EXTEND(L,4294967295,Add,add,uxtw)
dnl
ADD_SUB_ZERO_EXTEND(I,255,Sub,subw,uxtb)
ADD_SUB_ZERO_EXTEND(I,65535,Sub,subw,uxth)
ADD_SUB_ZERO_EXTEND(L,255,Sub,sub,uxtb)
ADD_SUB_ZERO_EXTEND(L,65535,Sub,sub,uxth)
ADD_SUB_ZERO_EXTEND(L,4294967295,Sub,sub,uxtw)
dnl
dnl ADD_SUB_ZERO_EXTEND_SHIFT(mode, size, add node, insn, ext type)
define(`ADD_SUB_EXTENDED_SHIFT', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3Ext$1_$6_shift(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, immIExt lshift2, immI_`'eval($7-$2) lshift1, immI_`'eval($7-$2) rshift1, rFlagsReg cr)
%{
  match(Set dst ($3$1 src1 (LShift$1 EXTEND($1, $4, src2, lshift1, rshift1) lshift2)));
  ins_cost(1.9 * INSN_COST);
  format %{ "$5  $dst, $src1, $src2, $6 #lshift2" %}

   ins_encode %{
     __ $5(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$6, ($lshift2$$constant));
   %}
  ins_pipe(ialu_reg_reg_shift);
%}
')
dnl                   $1 $2 $3   $4   $5   $6  $7
ADD_SUB_EXTENDED_SHIFT(L,8,Add,RShift,add,sxtb,64)
ADD_SUB_EXTENDED_SHIFT(L,16,Add,RShift,add,sxth,64)
ADD_SUB_EXTENDED_SHIFT(L,32,Add,RShift,add,sxtw,64)
dnl
ADD_SUB_EXTENDED_SHIFT(L,8,Sub,RShift,sub,sxtb,64)
ADD_SUB_EXTENDED_SHIFT(L,16,Sub,RShift,sub,sxth,64)
ADD_SUB_EXTENDED_SHIFT(L,32,Sub,RShift,sub,sxtw,64)
dnl
ADD_SUB_EXTENDED_SHIFT(I,8,Add,RShift,addw,sxtb,32)
ADD_SUB_EXTENDED_SHIFT(I,16,Add,RShift,addw,sxth,32)
dnl
ADD_SUB_EXTENDED_SHIFT(I,8,Sub,RShift,subw,sxtb,32)
ADD_SUB_EXTENDED_SHIFT(I,16,Sub,RShift,subw,sxth,32)
dnl
dnl ADD_SUB_CONV_SHIFT(mode, add node, insn, ext type)
define(`ADD_SUB_CONV_SHIFT', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $2ExtI_shift(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iRegIorL2I src2, immIExt lshift, rFlagsReg cr)
%{
  match(Set dst ($2$1 src1 (LShiftL (ConvI2L src2) lshift)));
  ins_cost(1.9 * INSN_COST);
  format %{ "$3  $dst, $src1, $src2, $4 #lshift" %}

   ins_encode %{
     __ $3(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$4, ($lshift$$constant));
   %}
  ins_pipe(ialu_reg_reg_shift);
%}
')dnl
ADD_SUB_CONV_SHIFT(L,Add,add,sxtw)
ADD_SUB_CONV_SHIFT(L,Sub,sub,sxtw)
dnl
dnl ADD_SUB_ZERO_EXTEND(mode, size, add node, insn, ext type)
define(`ADD_SUB_ZERO_EXTEND_SHIFT', `// This pattern is automatically generated from aarch64_ad.m4
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct $3Ext$1_$5_and_shift(iReg$1NoSp dst, iReg$1`'ORL2I($1) src1, iReg$1`'ORL2I($1) src2, imm$1_$2 mask, immIExt lshift, rFlagsReg cr)
%{
  match(Set dst ($3$1 src1 (LShift$1 (And$1 src2 mask) lshift)));
  ins_cost(1.9 * INSN_COST);
  format %{ "$4  $dst, $src1, $src2, $5 #lshift" %}

   ins_encode %{
     __ $4(as_Register($dst$$reg), as_Register($src1$$reg),
            as_Register($src2$$reg), ext::$5, ($lshift$$constant));
   %}
  ins_pipe(ialu_reg_reg_shift);
%}
')dnl
dnl                       $1 $2  $3  $4  $5
ADD_SUB_ZERO_EXTEND_SHIFT(L,255,Add,add,uxtb)
ADD_SUB_ZERO_EXTEND_SHIFT(L,65535,Add,add,uxth)
ADD_SUB_ZERO_EXTEND_SHIFT(L,4294967295,Add,add,uxtw)
dnl
ADD_SUB_ZERO_EXTEND_SHIFT(L,255,Sub,sub,uxtb)
ADD_SUB_ZERO_EXTEND_SHIFT(L,65535,Sub,sub,uxth)
ADD_SUB_ZERO_EXTEND_SHIFT(L,4294967295,Sub,sub,uxtw)
dnl
ADD_SUB_ZERO_EXTEND_SHIFT(I,255,Add,addw,uxtb)
ADD_SUB_ZERO_EXTEND_SHIFT(I,65535,Add,addw,uxth)
dnl
ADD_SUB_ZERO_EXTEND_SHIFT(I,255,Sub,subw,uxtb)
ADD_SUB_ZERO_EXTEND_SHIFT(I,65535,Sub,subw,uxth)
dnl

