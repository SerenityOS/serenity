dnl Copyright (c) 2014, 2019, Red Hat Inc. All rights reserved.
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
dnl Process this file with m4 ad_encode.m4 to generate the load/store
dnl patterns used in aarch64.ad.
dnl
define(choose, `loadStore($1, &MacroAssembler::$3, $2, $4,
               $5, $6, $7, $8, $9);dnl

  %}')dnl
define(access, `
    $3Register $1_reg = as_$3Register($$1$$reg);
    $4choose(C2_MacroAssembler(&cbuf), $1_reg,$2,$mem->opcode(),
        as_Register($mem$$base),$mem$$index,$mem$$scale,$mem$$disp,$5)')dnl
define(load,`
  // This encoding class is generated automatically from ad_encode.m4.
  // DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
  enc_class aarch64_enc_$2($1 dst, memory$5 mem) %{dnl
access(dst,$2,$3,$4,$5)')dnl
load(iRegI,ldrsbw,,,1)
load(iRegI,ldrsb,,,1)
load(iRegI,ldrb,,,1)
load(iRegL,ldrb,,,1)
load(iRegI,ldrshw,,,2)
load(iRegI,ldrsh,,,2)
load(iRegI,ldrh,,,2)
load(iRegL,ldrh,,,2)
load(iRegI,ldrw,,,4)
load(iRegL,ldrw,,,4)
load(iRegL,ldrsw,,,4)
load(iRegL,ldr,,,8)
load(vRegF,ldrs,Float,,4)
load(vRegD,ldrd,Float,,8)
define(STORE,`
  // This encoding class is generated automatically from ad_encode.m4.
  // DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
  enc_class aarch64_enc_$2($1 src, memory$5 mem) %{dnl
access(src,$2,$3,$4,$5)')dnl
define(STORE0,`
  // This encoding class is generated automatically from ad_encode.m4.
  // DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
  enc_class aarch64_enc_$2`'0(memory$4 mem) %{
    C2_MacroAssembler _masm(&cbuf);
    choose(_masm,zr,$2,$mem->opcode(),
        as_$3Register($mem$$base),$mem$$index,$mem$$scale,$mem$$disp,$4)')dnl
STORE(iRegI,strb,,,1)
STORE0(iRegI,strb,,1)
STORE(iRegI,strh,,,2)
STORE0(iRegI,strh,,2)
STORE(iRegI,strw,,,4)
STORE0(iRegI,strw,,4)
STORE(iRegL,str,,
`// we sometimes get asked to store the stack pointer into the
    // current thread -- we cannot do that directly on AArch64
    if (src_reg == r31_sp) {
      C2_MacroAssembler _masm(&cbuf);
      assert(as_Register($mem$$base) == rthread, "unexpected store for sp");
      __ mov(rscratch2, sp);
      src_reg = rscratch2;
    }
    ',8)
STORE0(iRegL,str,,8)
STORE(vRegF,strs,Float,,4)
STORE(vRegD,strd,Float,,8)

  // This encoding class is generated automatically from ad_encode.m4.
  // DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
  enc_class aarch64_enc_strb0_ordered(memory4 mem) %{
      C2_MacroAssembler _masm(&cbuf);
      __ membar(Assembler::StoreStore);
      loadStore(_masm, &MacroAssembler::strb, zr, $mem->opcode(),
               as_Register($mem$$base), $mem$$index, $mem$$scale, $mem$$disp, 1);
  %}

