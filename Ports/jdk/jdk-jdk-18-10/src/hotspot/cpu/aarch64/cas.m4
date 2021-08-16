dnl Copyright (c) 2016, 2021, Red Hat Inc. All rights reserved.
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
dnl Process this file with m4 cas.m4 to generate the CAE and wCAS
dnl instructions used in aarch64.ad.
dnl

// BEGIN This section of the file is automatically generated. Do not edit --------------

// Sundry CAS operations.  Note that release is always true,
// regardless of the memory ordering of the CAS.  This is because we
// need the volatile case to be sequentially consistent but there is
// no trailing StoreLoad barrier emitted by C2.  Unfortunately we
// can't check the type of memory ordering here, so we always emit a
// STLXR.

// This section is generated from aarch64_ad_cas.m4


define(`CAS_INSN',
`
// This pattern is generated automatically from cas.m4.
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct compareAndExchange$1$6(iReg$2NoSp res, indirect mem, iReg$2 oldval, iReg$2 newval, rFlagsReg cr) %{
  ifelse($1$6,PAcq,'predicate(needs_acquiring_load_exclusive(n) && (n->as_LoadStore()->barrier_data() == 0));`,
         $1,P,'predicate(n->as_LoadStore()->barrier_data() == 0);`,
         $6,Acq,'predicate(needs_acquiring_load_exclusive(n));`)
  match(Set res (CompareAndExchange$1 mem (Binary oldval newval)));
  ifelse($6,Acq,'ins_cost(VOLATILE_REF_COST);`,'  ins_cost(2 * VOLATILE_REF_COST);`)
  effect(TEMP_DEF res, KILL cr);
  format %{
    "cmpxchg$5`'ifelse($6,Acq,_acq,) $res = $mem, $oldval, $newval\t# ($3, weak) if $mem == $oldval then $mem <-- $newval"
  %}
  ins_encode %{
    __ cmpxchg($mem$$Register, $oldval$$Register, $newval$$Register,
               Assembler::$4, /*acquire*/ ifelse($6,Acq,true,false), /*release*/ true,
               /*weak*/ false, $res$$Register);
  %}
  ins_pipe(pipe_slow);
%}')dnl
define(`CAS_INSN4',
`
// This pattern is generated automatically from cas.m4.
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct compareAndExchange$1$7(iReg$2NoSp res, indirect mem, iReg$2 oldval, iReg$2 newval, rFlagsReg cr) %{
  ifelse($7,Acq,'predicate(needs_acquiring_load_exclusive(n));`)
  match(Set res (CompareAndExchange$1 mem (Binary oldval newval)));
  ifelse($7,Acq,'ins_cost(VOLATILE_REF_COST);`,'  ins_cost(2 * VOLATILE_REF_COST);`)
  effect(TEMP_DEF res, KILL cr);
  format %{
    "cmpxchg$5`'ifelse($7,Acq,_acq,) $res = $mem, $oldval, $newval\t# ($3, weak) if $mem == $oldval then $mem <-- $newval"
  %}
  ins_encode %{
    __ cmpxchg($mem$$Register, $oldval$$Register, $newval$$Register,
               Assembler::$4, /*acquire*/ ifelse($7,Acq,true,false), /*release*/ true,
               /*weak*/ false, $res$$Register);
    __ $6($res$$Register, $res$$Register);
  %}
  ins_pipe(pipe_slow);
%}')dnl
CAS_INSN4(B,I,byte,byte,b,sxtbw)
CAS_INSN4(S,I,short,halfword,s,sxthw)
CAS_INSN(I,I,int,word,w)
CAS_INSN(L,L,long,xword)
CAS_INSN(N,N,narrow oop,word,w)
CAS_INSN(P,P,ptr,xword)
dnl
CAS_INSN4(B,I,byte,byte,b,sxtbw,Acq)
CAS_INSN4(S,I,short,halfword,s,sxthw,Acq)
CAS_INSN(I,I,int,word,w,Acq)
CAS_INSN(L,L,long,xword,,Acq)
CAS_INSN(N,N,narrow oop,word,w,Acq)
CAS_INSN(P,P,ptr,xword,,Acq)
dnl
define(`CAS_INSN2',
`
// This pattern is generated automatically from cas.m4.
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct weakCompareAndSwap$1$6(iRegINoSp res, indirect mem, iReg$2 oldval, iReg$2 newval, rFlagsReg cr) %{
  ifelse($6,Acq,'  predicate(needs_acquiring_load_exclusive(n));`)
  match(Set res (WeakCompareAndSwap$1 mem (Binary oldval newval)));
  ifelse($6,Acq,'ins_cost(VOLATILE_REF_COST);`,'  ins_cost(2 * VOLATILE_REF_COST);`)
  effect(KILL cr);
  format %{
    "cmpxchg$5`'ifelse($6,Acq,_acq,) $res = $mem, $oldval, $newval\t# ($3, weak) if $mem == $oldval then $mem <-- $newval"
    "csetw $res, EQ\t# $res <-- (EQ ? 1 : 0)"
  %}
  ins_encode %{
    __ cmpxchg($mem$$Register, $oldval$$Register, $newval$$Register,
               Assembler::$4, /*acquire*/ ifelse($6,Acq,true,false), /*release*/ true,
               /*weak*/ true, noreg);
    __ csetw($res$$Register, Assembler::EQ);
  %}
  ins_pipe(pipe_slow);
%}')dnl
define(`CAS_INSN3',
`
// This pattern is generated automatically from cas.m4.
// DO NOT EDIT ANYTHING IN THIS SECTION OF THE FILE
instruct weakCompareAndSwap$1$6(iRegINoSp res, indirect mem, iReg$2 oldval, iReg$2 newval, rFlagsReg cr) %{
  ifelse($1$6,PAcq,'predicate(needs_acquiring_load_exclusive(n) && (n->as_LoadStore()->barrier_data() == 0));`,
         $1,P,'predicate(n->as_LoadStore()->barrier_data() == 0);`,
         $6,Acq,'predicate(needs_acquiring_load_exclusive(n));`)
  match(Set res (WeakCompareAndSwap$1 mem (Binary oldval newval)));
  ifelse($6,Acq,'ins_cost(VOLATILE_REF_COST);`,'  ins_cost(2 * VOLATILE_REF_COST);`)
  effect(KILL cr);
  format %{
    "cmpxchg$5`'ifelse($6,Acq,_acq,) $res = $mem, $oldval, $newval\t# ($3, weak) if $mem == $oldval then $mem <-- $newval"
    "csetw $res, EQ\t# $res <-- (EQ ? 1 : 0)"
  %}
  ins_encode %{
    __ cmpxchg($mem$$Register, $oldval$$Register, $newval$$Register,
               Assembler::$4, /*acquire*/ ifelse($6,Acq,true,false), /*release*/ true,
               /*weak*/ true, noreg);
    __ csetw($res$$Register, Assembler::EQ);
  %}
  ins_pipe(pipe_slow);
%}')dnl
CAS_INSN2(B,I,byte,byte,b)
CAS_INSN2(S,I,short,halfword,s)
CAS_INSN3(I,I,int,word,w)
CAS_INSN3(L,L,long,xword)
CAS_INSN3(N,N,narrow oop,word,w)
CAS_INSN3(P,P,ptr,xword)
CAS_INSN2(B,I,byte,byte,b,Acq)
CAS_INSN2(S,I,short,halfword,s,Acq)
CAS_INSN3(I,I,int,word,w,Acq)
CAS_INSN3(L,L,long,xword,,Acq)
CAS_INSN3(N,N,narrow oop,word,w,Acq)
CAS_INSN3(P,P,ptr,xword,,Acq)
dnl

// END This section of the file is automatically generated. Do not edit --------------
